package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.MagicSpellRegistry
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
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.type.varbit.VarBitTypeList

@Execution(ExecutionMode.SAME_THREAD)
class TeleportToTargetSpellTest {
    @Test
    fun GameTestState.`cache contract defines the spell scroll and Bounty Hunter varbits`() =
        runTargetTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_teleport_to_target))
            assertEquals(85, spell.levelReq)
            assertEquals(45.0, spell.castXp)
            assertNull(spell.spellbook)
            assertEquals(MagicSpellType.Teleport, spell.type)
            assertEquals(
                mapOf(objs.law_rune to 1, objs.chaos_rune to 1, objs.death_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(76, spell.component.id and 0xFFFF)
            assertEquals(17152, objs.spell_teleport_to_target.id)

            val scroll = deps.objTypes[objs.bounty_teleport_scroll]
            assertEquals(12846, scroll.id)
            assertEquals("Target teleport scroll", scroll.name)
            assertTrue(scroll.members)
            assertEquals("Read", scroll.iop[0])
            assertEquals(30_000, scroll.cost)
            assertTrue(scroll.tradeable)

            assertVarBit(deps, varbits.bounty_target_teleport, 2010, 1140, 31..31)
            assertVarBit(deps, varbits.bounty_target_limit, 6503, 1140, 30..30)
            assertVarBit(deps, varbits.bounty_teleport_warning, 236, 1429, 10..10)
            assertVarBit(deps, varbits.bounty_teleport_quota, 5604, 1429, 6..9)
        }

    @Test
    fun GameTestState.`reading the scroll confirms consumes and permanently unlocks the spell`() =
        runTargetTest { deps ->
            prepareCaster()
            player.inv[0] = InvObj(objs.bounty_teleport_scroll)

            player.withProtectedAccess {
                with(deps.script) { readTargetTeleportScroll(player.inv, slot = 0) }
            }
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.messagebox_pbutton, subcomponent = -1)
            )
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
            )

            assertEquals(1, player.vars[varbits.bounty_target_teleport])
            assertEquals(0, player.count(objs.bounty_teleport_scroll))
            assertMessageSent("You have learnt the 'Teleport to Target' spell.")

            player.inv[0] = InvObj(objs.bounty_teleport_scroll)
            player.withProtectedAccess {
                with(deps.script) { readTargetTeleportScroll(player.inv, slot = 0) }
            }
            assertEquals(1, player.count(objs.bounty_teleport_scroll))
        }

    @Test
    fun GameTestState.`declining the scroll keeps it and leaves the spell locked`() =
        runTargetTest { deps ->
            prepareCaster()
            player.inv[0] = InvObj(objs.bounty_teleport_scroll)

            player.withProtectedAccess {
                with(deps.script) { readTargetTeleportScroll(player.inv, slot = 0) }
            }
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.messagebox_pbutton, subcomponent = -1)
            )
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 2)
            )

            assertEquals(0, player.vars[varbits.bounty_target_teleport])
            assertEquals(1, player.count(objs.bounty_teleport_scroll))
        }

    @Test
    fun GameTestState.`lobby cast consumes exact costs and arrives within twelve squares`() =
        runTargetTest { deps ->
            val target = prepareCast(deps)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(0, runeCount(deps))
            assertEquals(450, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.teleport_casting.id, player.pendingSequence.id)
            assertEquals(
                spotanims.teleport_casting.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            advance(3)
            assertTrue(player.coords.chebyshevDistance(target.coords) > 12)
            advance()
            assertTrue(player.coords.chebyshevDistance(target.coords) <= 12)
            assertEquals(target.coords.level, player.coords.level)
            assertEquals(1, deps.targets.assignment(player)?.teleportsUsed)
            assertEquals(1, player.vars[varbits.bounty_teleport_quota])
            assertEquals(0, player.vars[varbits.bounty_target_limit])
        }

    @Test
    fun GameTestState.`five casts exhaust the per-target quota and the sixth is atomic`() =
        runTargetTest { deps ->
            prepareCast(deps, runeMultiplier = 6)

            repeat(5) {
                player.withProtectedAccess { deps.spell.cast(this) }
                advance(4)
            }
            assertEquals(5, deps.targets.assignment(player)?.teleportsUsed)
            assertEquals(5, player.vars[varbits.bounty_teleport_quota])
            assertEquals(1, player.vars[varbits.bounty_target_limit])
            val runesBefore = runeCount(deps)
            val xpBefore = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess {
                deps.spell.cast(this)
                assertMessageSent("You have already teleported to this target five times.")
            }

            assertEquals(runesBefore, runeCount(deps))
            assertEquals(xpBefore, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`crater casting is blocked until twenty ticks after combat`() =
        runTargetTest { deps ->
            prepareCast(deps, casterLocation = BountyHunterLocation.Crater, runeMultiplier = 2)
            player.setVarp(varps.lastcombat, player.currentMapClock - 19)
            val runesBefore = runeCount(deps)

            player.withProtectedAccess {
                deps.spell.cast(this)
                assertMessageSent("You cannot use this spell until 12 seconds after combat.")
            }
            assertEquals(runesBefore, runeCount(deps))

            player.setVarp(varps.lastcombat, player.currentMapClock - 20)
            player.withProtectedAccess { deps.spell.cast(this) }
            assertTrue(runeCount(deps) < runesBefore)
            advance(4)
            assertEquals(1, deps.targets.assignment(player)?.teleportsUsed)
        }

    @Test
    fun GameTestState
        .`membership unlock location target and target-location failures are atomic`() =
        runTargetTest { deps ->
            val target = prepareCast(deps, runeMultiplier = 1)
            val runesBefore = runeCount(deps)
            val xpBefore = player.statMap.getFineXP(stats.magic)

            player.members = false
            assertRejected(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.bounty_target_teleport, 0)
            assertRejected(deps, "You have not unlocked this spell.")
            player.setVarBit(varbits.bounty_target_teleport, 1)
            deps.targets.clear(player)
            assertRejected(deps, "You can only cast this spell in Bounty Hunter.")
            deps.targets.setLocation(player, BountyHunterLocation.Lobby)
            assertRejected(deps, "You do not have a Bounty Hunter target.")
            deps.targets.assign(player, target)
            deps.targets.setLocation(target, BountyHunterLocation.Lobby)
            assertRejected(deps, "Your Bounty Hunter target is not inside the crater.")

            assertEquals(runesBefore, runeCount(deps))
            assertEquals(xpBefore, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`target replacement during the four-tick cast prevents stale teleport`() =
        runTargetTest { deps ->
            val first = prepareCast(deps)
            val second = registerPlayer(first.coords.translate(1, 0))
            second.setCurrentLevel(stats.hitpoints, 99)
            deps.targets.setLocation(second, BountyHunterLocation.Crater)
            val origin = player.coords

            player.withProtectedAccess { deps.spell.cast(this) }
            advance(2)
            deps.targets.assign(player, second)
            advance(2)

            assertEquals(origin, player.coords)
            assertMessageSent("Your Bounty Hunter target is no longer available.")
            assertEquals(0, deps.targets.assignment(player)?.teleportsUsed)
        }

    @Test
    fun GameTestState.`spellbook dispatch and logout cleanup use the shared target contract`() =
        runTargetTest { deps ->
            val target = prepareCast(deps)

            deps.events.publish(
                IfOverlayButton(
                    player,
                    components.magic_spellbook_bounty_target,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )
            advance(4)
            assertEquals(1, deps.targets.assignment(player)?.teleportsUsed)

            deps.events.publish(SessionStateEvent.Logout(target))
            assertNull(deps.targets.assignment(player))
            assertEquals(0, player.vars[varbits.bounty_teleport_quota])
            assertEquals(0, player.vars[varbits.bounty_target_limit])

            deps.targets.setLocation(target, BountyHunterLocation.Crater)
            deps.targets.assign(player, target)

            deps.events.publish(SessionStateEvent.Logout(player))
            assertNull(deps.targets.assignment(player))
            assertNull(deps.targets.location(player))
        }

    private fun GameTestScope.prepareCaster() {
        player.members = true
        player.stats[stats.magic] = 99
        player.clearInv()
        player.setVarBit(varbits.bounty_target_teleport, 0)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarp(varps.lastcombat, -100)
        player.setVarp(varps.lastcombat_pvp, -100)
    }

    private fun GameTestScope.prepareCast(
        deps: Dependencies,
        casterLocation: BountyHunterLocation = BountyHunterLocation.Lobby,
        runeMultiplier: Int = 1,
    ): Player {
        prepareCaster()
        player.setVarBit(varbits.bounty_target_teleport, 1)
        val target = registerPlayer(player.coords.translate(20, 0))
        target.setCurrentLevel(stats.hitpoints, 99)
        deps.targets.setLocation(player, casterLocation)
        deps.targets.setLocation(target, BountyHunterLocation.Crater)
        deps.targets.assign(player, target)
        addRunes(deps, runeMultiplier)
        return target
    }

    private fun GameTestScope.addRunes(deps: Dependencies, multiplier: Int) {
        val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_teleport_to_target))
        spell.objReqs.forEachIndexed { slot, req ->
            player.inv[slot] = InvObj(req.obj, req.count * multiplier)
        }
    }

    private fun GameTestScope.runeCount(deps: Dependencies): Int {
        val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_teleport_to_target))
        return spell.objReqs.sumOf { player.count(it.obj) }
    }

    private fun GameTestScope.assertRejected(deps: Dependencies, message: String) {
        val runesBefore = runeCount(deps)
        val xpBefore = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess {
            deps.spell.cast(this)
            assertMessageSent(message)
        }
        assertEquals(runesBefore, runeCount(deps))
        assertEquals(xpBefore, player.statMap.getFineXP(stats.magic))
    }

    private fun assertVarBit(
        deps: Dependencies,
        type: VarBitType,
        id: Int,
        varp: Int,
        bits: IntRange,
    ) {
        val unpacked = deps.varbitTypes[type]
        assertEquals(id, unpacked.id)
        assertEquals(varp, unpacked.baseVar.id)
        assertEquals(bits, unpacked.bits)
    }

    private fun GameTestState.runTargetTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            TeleportToTargetSpellScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
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
        val spell: TeleportToTargetSpell,
        val script: TeleportToTargetSpellScript,
        val targets: BountyTargetRepository,
        val spells: MagicSpellRegistry,
        val objTypes: ObjTypeList,
        val varbitTypes: VarBitTypeList,
        val events: EventBus,
    )
}
