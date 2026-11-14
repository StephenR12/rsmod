package org.rsmod.content.skills.magic.bryophytasstaff

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.spells.runes.combo.ComboRuneRepository
import org.rsmod.api.spells.runes.combo.ComboRuneScript
import org.rsmod.api.spells.runes.compact.CompactRuneRepository
import org.rsmod.api.spells.runes.compact.CompactRuneScript
import org.rsmod.api.spells.runes.fake.FakeRuneRepository
import org.rsmod.api.spells.runes.fake.FakeRuneScript
import org.rsmod.api.spells.runes.staves.StaffSubstituteRepository
import org.rsmod.api.spells.runes.staves.StaffSubstituteScript
import org.rsmod.api.spells.runes.subs.RuneSubstituteRepository
import org.rsmod.api.spells.runes.subs.RuneSubstituteScript
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneRepository
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneScript
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpLifetime

@Execution(ExecutionMode.SAME_THREAD)
class BryophytasStaffTest {
    @Test
    fun GameTestState
        .`withdrawal transforms the empty staff and consumes only the missing nature runes`() =
        runStaffTest { deps ->
            player.withProtectedAccess { vars[varbits.charges_bryophytas_staff_quantity] = 8 }
            player.inv[4] = InvObj(objs.bryophytas_staff_uncharged)
            var available = 3
            fun withdraw() {
                player.withProtectedAccess {
                    assertTrue(
                        deps.events.publish(
                            this,
                            BankedObjEvents.Autocharge(4, inv.getValue(4)) { type, requested ->
                                assertEquals(objs.nature_rune.id, type.id)
                                val used = minOf(available, requested)
                                available -= used
                                used
                            },
                        )
                    )
                }
            }
            withdraw()
            assertEquals(InvObj(objs.bryophytas_staff, vars = 3), player.inv[4])
            available = 9
            withdraw()
            assertEquals(8, player.inv[4]?.vars)
            assertEquals(4, available)
            player.withProtectedAccess { vars[varbits.charges_bryophytas_staff_quantity] = 2 }
            withdraw()
            assertEquals(8, player.inv[4]?.vars)
            assertEquals(4, available)
        }

    @Test
    fun GameTestState.`cache 238 staff variants and interaction dispatch are pinned`() =
        runStaffTest { deps ->
            assertEquals(22368, objs.bryophytas_staff_uncharged.id)
            assertEquals(22370, objs.bryophytas_staff.id)
            val bankAutochargeQuantity = deps.varbits[varbits.charges_bryophytas_staff_quantity]
            assertEquals(16139, bankAutochargeQuantity.id)
            assertEquals(4677, bankAutochargeQuantity.baseVar.id)
            assertEquals(20..30, bankAutochargeQuantity.bits)
            assertEquals(VarpLifetime.Perm, bankAutochargeQuantity.baseVar.scope)
            assertEquals(
                listOf(null, "Wield", "Check", null, null),
                deps.objs[objs.bryophytas_staff_uncharged].iop.toList(),
            )
            assertEquals(
                listOf(null, "Wield", "Check", "Uncharge", null),
                deps.objs[objs.bryophytas_staff].iop.toList(),
            )
            assertEquals(
                objs.bryophytas_staff,
                deps.objs[objs.bryophytas_staff_uncharged].paramOrNull(params.charged_variant),
            )
            assertEquals(
                objs.bryophytas_staff_uncharged,
                deps.objs[objs.bryophytas_staff].paramOrNull(params.uncharged_variant),
            )
            assertEquals(
                4_340,
                deps.objs[objs.bryophytas_staff].paramOrNull(params.bank_autocharge)?.id,
            )
            assertTrue(
                deps.events.contains(
                    BankedObjEvents.ConfigureCharges::class.java,
                    objs.bryophytas_staff.id.toLong(),
                )
            )
            for (staff in listOf(objs.bryophytas_staff_uncharged, objs.bryophytas_staff)) {
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(staff.id, objs.nature_rune.id),
                    )
                )
                assertTrue(deps.events.contains(HeldObjEvents.Op3::class.java, staff.id.toLong()))
                assertTrue(deps.events.contains(WornObjEvents.Op2::class.java, staff.id.toLong()))
            }
            assertTrue(
                deps.events.contains(
                    HeldObjEvents.Op4::class.java,
                    objs.bryophytas_staff.id.toLong(),
                )
            )
        }

    @Test
    fun GameTestState.`bank charge preference is permanent and capped at one thousand`() =
        runStaffTest { deps ->
            player.withProtectedAccess { deps.lifecycle.configureCharges(this) }
            player.resumeCountDialog(1_500)
            advance()
            assertEquals(1_000, player.vars[varbits.charges_bryophytas_staff_quantity])

            player.withProtectedAccess { deps.lifecycle.configureCharges(this) }
            player.resumeCountDialog(0)
            advance()
            assertEquals(0, player.vars[varbits.charges_bryophytas_staff_quantity])
        }

    @Test
    fun GameTestState
        .`loading transforms the staff caps at one thousand and supports full inventory`() =
        runStaffTest { deps ->
            fillInventory()
            player.inv[3] = InvObj(objs.bryophytas_staff_uncharged)
            player.inv[17] = InvObj(objs.nature_rune, 1_200)

            player.withProtectedAccess { deps.lifecycle.charge(this, staffSlot = 3, runeSlot = 17) }

            assertTrue(player.inv[3].isType(objs.bryophytas_staff))
            assertEquals(1_000, deps.lifecycle.charges(player.inv[3]))
            assertEquals(200, player.inv[17]?.count)
            client.clear()
            player.withProtectedAccess { deps.lifecycle.charge(this, staffSlot = 3, runeSlot = 17) }
            assertEquals(200, player.inv[17]?.count)
            assertMessageSent(BryophytasStaffLifecycle.FULL_MESSAGE)
        }

    @Test
    fun GameTestState.`check reports item-local charges and uncharge destroys every stored rune`() =
        runStaffTest { deps ->
            player.inv[2] = InvObj(objs.bryophytas_staff_uncharged)
            player.inv[9] = InvObj(objs.nature_rune, 73)
            player.withProtectedAccess { deps.lifecycle.charge(this, staffSlot = 2, runeSlot = 9) }
            client.clear()

            player.withProtectedAccess { deps.lifecycle.check(this, player.inv[2]) }
            assertMessageSent("Bryophyta's staff has 73 charges remaining.")
            client.clear()
            player.withProtectedAccess { deps.lifecycle.uncharge(this, staffSlot = 2) }

            assertTrue(player.inv[2].isType(objs.bryophytas_staff_uncharged))
            assertEquals(0, deps.lifecycle.charges(player.inv[2]))
            assertEquals(0, player.count(objs.nature_rune))
        }

    @Test
    fun GameTestState
        .`casts consume staff before loose runes and depletion changes the worn variant`() =
        runStaffTest { deps ->
            player.worn[Wearpos.RightHand.slot] = chargedStaff(2)
            player.inv[4] = InvObj(objs.nature_rune, 5)
            random.then = 1

            val result = deps.runes.delReqs(player, natureSpell(3))

            assertEquals(MagicRuneManager.CastResult.Success.Consumed(false), result)
            assertTrue(player.worn[Wearpos.RightHand.slot].isType(objs.bryophytas_staff_uncharged))
            assertEquals(4, player.count(objs.nature_rune))
        }

    @Test
    fun GameTestState.`one in fifteen proc saves exactly one charge for a multi-rune spell`() =
        runStaffTest { deps ->
            player.worn[Wearpos.RightHand.slot] = chargedStaff(5)
            random.then = 0

            val result = deps.runes.delReqs(player, natureSpell(3))

            assertEquals(MagicRuneManager.CastResult.Success.Consumed(false), result)
            assertEquals(3, deps.lifecycle.charges(player.worn[Wearpos.RightHand.slot]))
            assertMessageSent("Your staff saved you a nature rune.")
        }

    @Test
    fun GameTestState.`failed casts do not consume charges or roll the conservation chance`() =
        runStaffTest { deps ->
            player.worn[Wearpos.RightHand.slot] = chargedStaff(2)
            random.then = 0

            assertEquals(
                MagicRuneManager.CastResult.Failure.MissingObjRequirements,
                deps.runes.delReqs(player, natureSpell(3)),
            )
            assertEquals(2, deps.lifecycle.charges(player.worn[Wearpos.RightHand.slot]))

            player.inv[0] = InvObj(objs.nature_rune)
            assertEquals(
                MagicRuneManager.CastResult.Success.Consumed(false),
                deps.runes.delReqs(player, natureSpell(3)),
            )
            assertEquals(1, deps.lifecycle.charges(player.worn[Wearpos.RightHand.slot]))
            assertMessageSent("Your staff saved you a nature rune.")
        }

    private fun GameTestScope.fillInventory() {
        for (slot in player.inv.indices) {
            player.inv[slot] = InvObj(objs.coins)
        }
    }

    private fun chargedStaff(charges: Int): InvObj = InvObj(objs.bryophytas_staff, vars = charges)

    private fun natureSpell(natureRunes: Int): MagicSpell =
        MagicSpell(
            obj = objs.spell_high_alchemy,
            name = "Test nature spell",
            component = components.magic_spellbook_high_alchemy,
            spellbook = Spellbook.Standard,
            type = MagicSpellType.Utility,
            maxHit = 0,
            levelReq = 1,
            castXp = 0.0,
            objReqs = listOf(MagicSpell.ObjRequirement(objs.nature_rune, natureRunes, null)),
        )

    private fun GameTestState.runStaffTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            BryophytasStaffScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val lifecycle: BryophytasStaffLifecycle,
        val runes: MagicRuneManager,
        val events: EventBus,
        val objs: ObjTypeList,
        val varbits: VarBitTypeList,
    )
}
