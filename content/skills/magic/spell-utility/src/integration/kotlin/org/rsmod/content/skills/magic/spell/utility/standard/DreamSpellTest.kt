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
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.PlayerActionEvent
import org.rsmod.api.player.stat.stat
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
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.seq.SeqTypeList

@Execution(ExecutionMode.SAME_THREAD)
class DreamSpellTest {
    @Test
    fun GameTestState.`cache 238 pins Dream state and presentation contracts`() =
        runDreamTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_dream))

            assertEquals(11148, spell.obj.id)
            assertEquals(79, spell.levelReq)
            assertEquals(82.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 2, objs.cosmic_rune to 1, objs.body_rune to 5),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_dream.id ushr 16)
            assertEquals(130, components.magic_spellbook_dream.id and 0xFFFF)
            assertEquals(
                listOf("Cast"),
                deps.components[components.magic_spellbook_dream].op.toList(),
            )
            assertEquals(6296, seqs.dream_player_dream_spell_ready.id)
            assertEquals(7627, seqs.dream_spell_start.id)
            assertEquals(7628, seqs.dream_spell_end.id)
            assertEquals(6302, seqs.dream_zeds.id)
            assertEquals(1056, spotanims.dream_zeds_spotanim.id)
            assertTrue(deps.seqs[seqs.dream_spell_start].tickDuration > 0)
            assertTrue(deps.seqs[seqs.dream_spell_end].tickDuration > 0)
        }

    @Test
    fun GameTestState
        .`successful cast consumes exact runes awards xp and enters start animation`() =
        runDreamTest { deps ->
            prepareCaster(deps)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.dream.cast(this) }

            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.cosmic_rune))
            assertEquals(0, player.count(objs.body_rune))
            assertEquals(820, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(seqs.dream_spell_start.id, player.pendingSequence.id)
            assertTrue(deps.states.isActive(player))
            assertEquals(1, player.queueList.count(dream_queues.channel))
            assertTrue(client.filterIsInstance<SynthSound>().isEmpty())
            assertTrue(player.pendingSpotanims.isEmpty())
        }

    @Test
    fun GameTestState.`independent timer heals exactly three times per one hundred ticks`() =
        runDreamTest { deps ->
            prepareCaster(deps, currentHitpoints = 90)
            player.withProtectedAccess { deps.dream.cast(this) }

            advance(33)
            assertEquals(90, player.stat(stats.hitpoints))
            advance()
            assertEquals(91, player.stat(stats.hitpoints))
            advance(32)
            assertEquals(91, player.stat(stats.hitpoints))
            advance()
            assertEquals(92, player.stat(stats.hitpoints))
            advance(32)
            assertEquals(92, player.stat(stats.hitpoints))
            advance()
            assertEquals(93, player.stat(stats.hitpoints))
            assertTrue(deps.states.isActive(player))
        }

    @Test
    fun GameTestState.`sleep presentation transitions from cache start and wakes at full health`() =
        runDreamTest { deps ->
            prepareCaster(deps, baseHitpoints = 92, currentHitpoints = 90)
            player.withProtectedAccess { deps.dream.cast(this) }
            val startTicks = deps.seqs[seqs.dream_spell_start].tickDuration

            advance(startTicks)
            assertTrue(deps.states.isActive(player))
            advance(67 - startTicks)

            assertEquals(92, player.stat(stats.hitpoints))
            assertFalse(deps.states.isActive(player))
            assertEquals(0, player.queueList.count(dream_queues.channel))
        }

    @Test
    fun GameTestState.`new action or hitpoint damage interrupts before another heal`() =
        runDreamTest { deps ->
            prepareCaster(deps, casts = 2)
            player.withProtectedAccess { deps.dream.cast(this) }
            deps.events.publish(PlayerActionEvent.Interrupt(player))
            assertFalse(deps.states.isActive(player))
            assertEquals(0, player.queueList.count(dream_queues.channel))
            assertEquals(seqs.dream_spell_end.id, player.pendingSequence.id)

            player.withProtectedAccess { deps.dream.cast(this) }
            advance(10)
            player.setCurrentLevel(stats.hitpoints, 89)
            advance()
            assertFalse(deps.states.isActive(player))
            assertEquals(89, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState.`chat continues Dream while active direct recast is an atomic no-op`() =
        runDreamTest { deps ->
            prepareCaster(deps, casts = 2)
            val magicXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.dream.cast(this) }
            player.say("Still dreaming")
            player.withProtectedAccess { deps.dream.cast(this) }

            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(820, player.statMap.getFineXP(stats.magic) - magicXp)
            advance(34)
            assertEquals(91, player.stat(stats.hitpoints))
            assertTrue(deps.states.isActive(player))
        }

    @Test
    fun GameTestState.`members quests spellbook Magic full health and rune failures are atomic`() =
        runDreamTest { deps ->
            prepareCaster(deps)

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
            player.stats[stats.magic] = 78
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.setCurrentLevel(stats.hitpoints, 99)
            reject(deps)
            player.setCurrentLevel(stats.hitpoints, 90)
            player.inv[2] = null
            reject(deps, "You do not have enough Body Runes to cast this spell.")

            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(1, player.count(objs.cosmic_rune))
            assertFalse(deps.states.isActive(player))
        }

    @Test
    fun GameTestState.`Fountain overlay and session cleanup use shared policies`() =
        runDreamTest { deps ->
            prepareCaster(deps, casts = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val magicXp = player.statMap.getFineXP(stats.magic)

            deps.events.publish(
                IfOverlayButton(
                    player,
                    components.magic_spellbook_dream,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )

            assertTrue(deps.states.isActive(player))
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
            deps.events.publish(SessionStateEvent.Logout(player))
            assertFalse(deps.states.isActive(player))
            assertEquals(0, player.queueList.count(dream_queues.channel))
            deps.events.publish(SessionStateEvent.Login(player))
            assertFalse(deps.states.isActive(player))
        }

    private fun GameTestScope.prepareCaster(
        deps: Dependencies,
        casts: Int = 1,
        baseHitpoints: Int = 99,
        currentHitpoints: Int = 90,
    ) {
        deps.dream.clear(player)
        player.members = true
        player.stats[stats.magic] = 99
        player.setBaseLevel(stats.hitpoints, baseHitpoints)
        player.setCurrentLevel(stats.hitpoints, currentHitpoints)
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.dream_mentor, 28)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.in_lms_game, 0)
        player.clearInv()
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 2)
            player.inv[1] = InvObj(objs.cosmic_rune, casts)
            player.inv[2] = InvObj(objs.body_rune, casts * 5)
        }
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        val magicXp = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess {
            deps.dream.cast(this)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
        assertFalse(deps.states.isActive(player))
        assertEquals(0, player.queueList.count(dream_queues.channel))
    }

    private fun GameTestState.runDreamTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            DreamSpellScript::class,
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
            bind(DreamStateRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val dream: DreamSpell,
        val states: DreamStateRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val seqs: SeqTypeList,
        val events: EventBus,
    )
}
