package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isSuccess
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
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
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SpellbookSwapSpellTest {
    @Test
    fun GameTestState.`cache contracts define Spellbook Swap and its client effects`() =
        runSwapTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_spellbook_swap))
            assertEquals(11150, spell.obj.id)
            assertEquals(96, spell.levelReq)
            assertEquals(130.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 3, objs.cosmic_rune to 2, objs.law_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(144, components.magic_spellbook_spellbook_swap.id and 0xFFFF)
            assertEquals(6299, seqs.dream_player_spellbook_swap.id)
            assertEquals(1062, spotanims.dream_spellbook_spotanim.id)
            assertEquals(3617, varbits.lunar_spellbook_change.id)
            assertEquals(4823, varbits.spellbook_swap_book.id)
            assertEquals(9631, varbits.arceuus_spellbook_unlocked.id)
        }

    @Test
    fun GameTestState.`successful Standard selection consumes exact cost and applies effects`() =
        runSwapTest { deps ->
            prepareCaster()
            val startXp = player.statMap.getFineXP(stats.magic)

            beginCast(deps, selection = 0)

            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.cosmic_rune))
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(1300, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook])
            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook_swap_book])
            assertEquals(1, player.vars[varbits.lunar_spellbook_change])
            assertTrue(deps.states.isActive(player))
            assertTrue(player.isDelayed)
            assertEquals(seqs.dream_player_spellbook_swap.id, player.pendingSequence.id)
            assertEquals(
                spotanims.dream_spellbook_spotanim.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(3613, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`Ancient choice requires Desert Treasure access`() = runSwapTest { deps ->
        prepareCaster()
        player.setVarBit(varbits.desert_treasure_main, 15)
        beginCast(deps, selection = 1)
        assertEquals(Spellbook.Ancients.varValue, player.vars[varbits.spellbook])
    }

    @Test
    fun GameTestState.`Arceuus choice requires prior Arceuus spellbook access`() =
        runSwapTest { deps ->
            prepareCaster()
            player.setVarBit(varbits.arceuus_spellbook_unlocked, 1)
            beginCast(deps, selection = 1)
            assertEquals(Spellbook.Arceuus.varValue, player.vars[varbits.spellbook])
        }

    @Test
    fun GameTestState.`cancel selection is atomic`() = runSwapTest { deps ->
        prepareCaster()
        val xp = player.statMap.getFineXP(stats.magic)

        beginCast(deps, selection = 1)

        assertEquals(3, player.count(objs.astral_rune))
        assertEquals(2, player.count(objs.cosmic_rune))
        assertEquals(1, player.count(objs.law_rune))
        assertEquals(xp, player.statMap.getFineXP(stats.magic))
        assertEquals(Spellbook.Lunars.varValue, player.vars[varbits.spellbook])
        assertFalse(deps.states.isActive(player))
    }

    @Test
    fun GameTestState.`members quests spellbook Magic and rune gates are atomic`() =
        runSwapTest { deps ->
            prepareCaster()

            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.dream_mentor, 27)
            reject(deps, "You need to complete Dream Mentor to cast this spell.")
            player.setVarBit(varbits.dream_mentor, 28)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 95
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.clearInv()
            reject(deps, "You do not have enough Astral Runes to cast this spell.")
        }

    @Test
    fun GameTestState.`active swap lasts through tick 199 and reverts on tick 200`() =
        runSwapTest { deps ->
            prepareCaster()
            beginCast(deps, selection = 0)

            advance(199)
            assertTrue(deps.states.isActive(player))
            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook])

            advance()
            assertFalse(deps.states.isActive(player))
            assertEquals(Spellbook.Lunars.varValue, player.vars[varbits.spellbook])
            assertEquals(0, player.vars[varbits.lunar_spellbook_change])
        }

    @Test
    fun GameTestState.`next successful spell consumes swap but a failed cast retains it`() =
        runSwapTest { deps ->
            prepareCaster(casts = 1, extraStandardRunes = false)
            beginCast(deps, selection = 0)
            advance(4)
            val highAlchemy = requireNotNull(deps.spells.getObjSpell(objs.spell_high_alchemy))

            assertTrue(deps.runes.attemptCast(player, highAlchemy).isFailure())
            assertTrue(deps.states.isActive(player))
            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook])

            player.inv[0] = InvObj(objs.nature_rune)
            player.inv[1] = InvObj(objs.fire_rune, 5)
            assertTrue(deps.runes.attemptCast(player, highAlchemy).isSuccess())
            assertFalse(deps.states.isActive(player))
            assertEquals(Spellbook.Lunars.varValue, player.vars[varbits.spellbook])
        }

    @Test
    fun GameTestState.`LMS entry cancels an active swap on the state timer`() =
        runSwapTest { deps ->
            prepareCaster()
            beginCast(deps, selection = 0)
            player.setVarBit(varbits.in_lms_game, 1)

            advance()

            assertFalse(deps.states.isActive(player))
            assertEquals(Spellbook.Lunars.varValue, player.vars[varbits.spellbook])
        }

    @Test
    fun GameTestState.`login preserves known active state and repairs orphaned client state`() =
        runSwapTest { deps ->
            prepareCaster()
            beginCast(deps, selection = 0)
            deps.events.publish(SessionStateEvent.Login(player))
            assertTrue(deps.states.isActive(player))
            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook])

            deps.states.revert(player)
            player.setVarBit(varbits.spellbook, Spellbook.Ancients.varValue)
            player.setVarBit(varbits.lunar_spellbook_change, 1)
            deps.events.publish(SessionStateEvent.Login(player))
            assertEquals(Spellbook.Lunars.varValue, player.vars[varbits.spellbook])
            assertEquals(0, player.vars[varbits.lunar_spellbook_change])
        }

    @Test
    fun GameTestState.`Fountain cast grants no xp and consumes no runes`() = runSwapTest { deps ->
        prepareCaster(casts = 0)
        player.setVarBit(varbits.fountain_of_rune_active, 1)
        val xp = player.statMap.getFineXP(stats.magic)

        beginCast(deps, selection = 0)

        assertEquals(xp, player.statMap.getFineXP(stats.magic))
        assertTrue(deps.states.isActive(player))
    }

    @Test
    fun GameTestState.`moving cast preserves its route and blocks energy drain for four ticks`() =
        runSwapTest { deps ->
            prepareCaster()
            player.routeDestination.add(CoordGrid(3201, 3200, 0))

            beginCast(deps, selection = 0)

            assertFalse(player.routeDestination.isEmpty())
            assertEquals(player.currentMapClock + 4, player.runEnergyDrainBlockedUntil)
        }

    private fun GameTestScope.prepareCaster(casts: Int = 1, extraStandardRunes: Boolean = false) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.dream_mentor, 28)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.in_lms_game, 0)
        player.clearInv()
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 3)
            player.inv[1] = InvObj(objs.cosmic_rune, casts * 2)
            player.inv[2] = InvObj(objs.law_rune, casts)
        }
        if (extraStandardRunes) {
            player.inv[3] = InvObj(objs.nature_rune)
            player.inv[4] = InvObj(objs.fire_rune, 5)
        }
    }

    private fun GameTestScope.beginCast(deps: Dependencies, selection: Int) {
        player.withProtectedAccess { deps.swap.cast(this) }
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.menu_list, subcomponent = selection)
        )
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        val runes = player.count(objs.astral_rune)
        val xp = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess { deps.swap.cast(this) }
        assertEquals(runes, player.count(objs.astral_rune))
        assertEquals(xp, player.statMap.getFineXP(stats.magic))
        assertFalse(deps.states.isActive(player))
        if (message != null) {
            assertMessageSent(message)
        }
        client.clear()
    }

    private fun GameTestState.runSwapTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            SpellbookSwapSpellScript::class,
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
            bind(SpellbookSwapStateRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val swap: SpellbookSwapSpell,
        val states: SpellbookSwapStateRepository,
        val spells: MagicSpellRegistry,
        val runes: MagicRuneManager,
        val events: EventBus,
    )
}
