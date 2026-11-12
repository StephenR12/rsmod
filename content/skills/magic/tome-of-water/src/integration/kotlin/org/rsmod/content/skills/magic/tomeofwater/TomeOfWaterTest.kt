package org.rsmod.content.skills.magic.tomeofwater

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.commons.magic.TomeOfWater
import org.rsmod.api.combat.formulas.attributes.CombatSpellAttributes
import org.rsmod.api.combat.formulas.attributes.collector.CombatMagicAttributeCollector
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.usedTomeOfWater
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.random.GameRandom
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
class TomeOfWaterTest {
    @Test
    fun GameTestState.`cache 233 tome variants and core interaction dispatch are pinned`() =
        runTomeTest { deps ->
            assertEquals(25_574, objs.tome_of_water.id)
            assertEquals(25_576, objs.tome_of_water_uncharged.id)
            assertEquals(25_578, objs.soaked_page.id)
            assertEquals(0..14, varobjs.tome_of_water_charges.bits)

            val bankAutochargeQuantity = deps.varbits[varbits.charges_tome_of_water_quantity]
            assertEquals(16_152, bankAutochargeQuantity.id)
            assertEquals(4_682, bankAutochargeQuantity.baseVar.id)
            assertEquals(VarpLifetime.Perm, bankAutochargeQuantity.baseVar.scope)

            assertEquals(
                listOf(null, "Wield", null, "Pages", null),
                deps.objs[objs.tome_of_water_uncharged].iop.toList(),
            )
            assertEquals(
                listOf(null, "Wield", "Check", "Pages", null),
                deps.objs[objs.tome_of_water].iop.toList(),
            )
            assertEquals(
                objs.tome_of_water,
                deps.objs[objs.tome_of_water_uncharged].paramOrNull(params.charged_variant),
            )
            assertEquals(
                objs.tome_of_water_uncharged,
                deps.objs[objs.tome_of_water].paramOrNull(params.uncharged_variant),
            )

            for (tome in listOf(objs.tome_of_water_uncharged, objs.tome_of_water)) {
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(tome.id, objs.soaked_page.id),
                    )
                )
            }
            assertTrue(
                deps.events.contains(HeldObjEvents.Op3::class.java, objs.tome_of_water.id.toLong())
            )
            assertTrue(
                deps.events.contains(WornObjEvents.Op1::class.java, objs.tome_of_water.id.toLong())
            )
        }

    @Test
    fun GameTestState.`pages add twenty charges transform the tome and cap at twenty thousand`() =
        runTomeTest { deps ->
            fillInventory()
            player.inv[3] = InvObj(objs.tome_of_water_uncharged)
            player.inv[17] = InvObj(objs.soaked_page, 1_100)

            player.withProtectedAccess { deps.lifecycle.charge(this, tomeSlot = 3, pageSlot = 17) }

            assertTrue(player.inv[3].isType(objs.tome_of_water))
            assertEquals(TomeOfWater.MAX_CHARGES, deps.lifecycle.charges(player.inv[3]))
            assertEquals(100, player.inv[17]?.count)
            client.clear()
            player.withProtectedAccess { deps.lifecycle.charge(this, tomeSlot = 3, pageSlot = 17) }
            assertEquals(100, player.inv[17]?.count)
            assertMessageSent(TomeOfWaterLifecycle.FULL_MESSAGE)
        }

    @Test
    fun GameTestState.`a final partial page fills only the remaining charge capacity`() =
        runTomeTest { deps ->
            player.inv[2] = chargedTome(19_990)
            player.inv[8] = InvObj(objs.soaked_page, 2)

            player.withProtectedAccess { deps.lifecycle.charge(this, tomeSlot = 2, pageSlot = 8) }

            assertEquals(20_000, deps.lifecycle.charges(player.inv[2]))
            assertEquals(1, player.inv[8]?.count)
        }

    @Test
    fun GameTestState.`charged tome supplies utility water runes without spending a charge`() =
        runTomeTest { deps ->
            player.worn[Wearpos.LeftHand.slot] = chargedTome(1)

            val result =
                deps.runes.attemptCast(
                    player,
                    spell(Spellbook.Standard, MagicSpellType.Utility, 12),
                )

            assertEquals(MagicRuneManager.CastResult.Success.AllUnlimited, result)
            assertEquals(1, deps.lifecycle.charges(player.worn[Wearpos.LeftHand.slot]))
        }

    @Test
    fun GameTestState.`standard combat cast consumes one charge and preserves loose water runes`() =
        runTomeTest { deps ->
            player.worn[Wearpos.LeftHand.slot] = chargedTome(2)
            player.inv[4] = InvObj(objs.water_rune, 100)

            val result =
                deps.runes.attemptCast(player, spell(Spellbook.Standard, MagicSpellType.Combat, 12))

            assertTrue(result.usedTomeOfWater())
            assertEquals(1, deps.lifecycle.charges(player.worn[Wearpos.LeftHand.slot]))
            assertEquals(100, player.inv[4]?.count)
        }

    @Test
    fun GameTestState
        .`last combat charge still marks the cast and changes the worn tome to empty`() =
        runTomeTest { deps ->
            player.worn[Wearpos.LeftHand.slot] = chargedTome(1)

            val result =
                deps.runes.attemptCast(player, spell(Spellbook.Standard, MagicSpellType.Combat, 3))

            assertEquals(MagicRuneManager.CastResult.Success.TomeOfWater(false), result)
            assertTrue(player.worn[Wearpos.LeftHand.slot].isType(objs.tome_of_water_uncharged))
            assertEquals(0, deps.lifecycle.charges(player.worn[Wearpos.LeftHand.slot]))
        }

    @Test
    fun GameTestState.`last charge cast state retains the water tome combat attribute`() =
        runTomeTest { deps ->
            player.worn[Wearpos.LeftHand.slot] = chargedTome(1)
            val result =
                deps.runes.attemptCast(player, spell(Spellbook.Standard, MagicSpellType.Combat, 3))

            val attributes =
                deps.attributes.spellCollect(
                    player = player,
                    spell = objs.spell_water_strike,
                    spellbook = Spellbook.Standard,
                    usedSunfireRune = false,
                    usedTomeOfWater = result.usedTomeOfWater(),
                    random = deps.random,
                )

            assertTrue(CombatSpellAttributes.WaterTome in attributes)
            assertTrue(CombatSpellAttributes.WaterSpell in attributes)
            assertTrue(player.worn[Wearpos.LeftHand.slot].isType(objs.tome_of_water_uncharged))
        }

    @Test
    fun GameTestState.`zero-charge variant gives neither unlimited water nor tome cast state`() =
        runTomeTest { deps ->
            player.worn[Wearpos.LeftHand.slot] = chargedTome(0)
            val combat = spell(Spellbook.Standard, MagicSpellType.Combat, 3)

            assertFalse(deps.runes.canCastSpell(player, combat))
            player.inv[6] = InvObj(objs.water_rune, 3)
            val result = deps.runes.attemptCast(player, combat)

            assertFalse(result.usedTomeOfWater())
            assertEquals(0, deps.lifecycle.charges(player.worn[Wearpos.LeftHand.slot]))
            assertEquals(0, player.count(objs.water_rune))
        }

    @Test
    fun GameTestState.`other spellbooks receive water runes without consuming tome charges`() =
        runTomeTest { deps ->
            player.worn[Wearpos.LeftHand.slot] = chargedTome(1)

            val result =
                deps.runes.attemptCast(
                    player,
                    spell(Spellbook.Ancients, MagicSpellType.Combat, 6),
                    Spellbook.Ancients,
                )

            assertEquals(MagicRuneManager.CastResult.Success.AllUnlimited, result)
            assertEquals(1, deps.lifecycle.charges(player.worn[Wearpos.LeftHand.slot]))
        }

    @Test
    fun GameTestState.`failed standard combat cast does not spend a tome charge`() =
        runTomeTest { deps ->
            player.worn[Wearpos.LeftHand.slot] = chargedTome(5)
            val spell =
                spell(Spellbook.Standard, MagicSpellType.Combat, 3)
                    .copy(
                        objReqs =
                            listOf(
                                MagicSpell.ObjRequirement(objs.water_rune, 3, null),
                                MagicSpell.ObjRequirement(objs.earth_rune, 2, null),
                            )
                    )

            val result = deps.runes.attemptCast(player, spell)

            assertTrue(result is MagicRuneManager.CastResult.Failure.MissingObjRequirements)
            assertEquals(5, deps.lifecycle.charges(player.worn[Wearpos.LeftHand.slot]))
        }

    private fun GameTestScope.fillInventory() {
        for (slot in player.inv.indices) {
            player.inv[slot] = InvObj(objs.coins)
        }
    }

    private fun chargedTome(charges: Int): InvObj = InvObj(objs.tome_of_water, vars = charges)

    private fun spell(book: Spellbook, type: MagicSpellType, waterRunes: Int): MagicSpell =
        MagicSpell(
            obj = objs.spell_water_strike,
            name = "Tome test spell",
            component = components.magic_spellbook_high_alchemy,
            spellbook = book,
            type = type,
            maxHit = 2,
            levelReq = 1,
            castXp = 0.0,
            objReqs = listOf(MagicSpell.ObjRequirement(objs.water_rune, waterRunes, null)),
        )

    private fun GameTestState.runTomeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            TomeOfWaterScript::class,
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
        val lifecycle: TomeOfWaterLifecycle,
        val runes: MagicRuneManager,
        val events: EventBus,
        val objs: ObjTypeList,
        val varbits: VarBitTypeList,
        val attributes: CombatMagicAttributeCollector,
        val random: GameRandom,
    )
}
