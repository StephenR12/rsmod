package org.rsmod.api.game.process.npc.hunt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.game.type.util.ParamMapBuilder
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
class GoadingPotionHuntTest {
    @Test
    fun GameTestState.`active goading attracts passive attackable npc inside nine by nine`() =
        runGameTest(GoadingAttackScript::class) {
            advance(9)
            val subject = registerPlayer(player.coords.translateX(100))
            subject.setVarBit(varbits.goading_potion_timer, 100)
            subject.setBaseLevel(stats.slayer, 99)
            subject.setCurrentLevel(stats.slayer, 99)
            allocZoneCollision(subject.coords)
            spawnNpc(subject.coords.translateX(4), passiveMonster)

            advance(6)

            assertMessageSent(GoadingAttackScript.ATTACK_MESSAGE, subject)
        }

    @Test
    fun GameTestState.`goading excludes targets outside radius`() =
        runGameTest(GoadingAttackScript::class) {
            advance(9)
            val subject = registerPlayer(player.coords.translateX(200))
            subject.setVarBit(varbits.goading_potion_timer, 100)
            subject.setBaseLevel(stats.slayer, 99)
            subject.setCurrentLevel(stats.slayer, 99)
            allocZoneCollision(subject.coords)
            spawnNpc(subject.coords.translateX(5), passiveMonster)

            advance(6)

            assertMessageNotSent(GoadingAttackScript.ATTACK_MESSAGE, subject)
        }

    @Test
    fun GameTestState.`goading excludes monsters above Slayer level until requirement is met`() =
        runGameTest(GoadingAttackScript::class) {
            advance(9)
            val subject = registerPlayer(player.coords.translateX(300))
            subject.setVarBit(varbits.goading_potion_timer, 100)
            subject.setBaseLevel(stats.slayer, 49)
            subject.setCurrentLevel(stats.slayer, 49)
            allocZoneCollision(subject.coords)
            val npc = spawnNpc(subject.coords.translateZ(4), slayerMonster)

            advance(6)
            assertMessageNotSent(GoadingAttackScript.ATTACK_MESSAGE, subject)

            subject.setBaseLevel(stats.slayer, 50)
            subject.setCurrentLevel(stats.slayer, 50)
            assertEquals(50, subject.statMap.getCurrentLevel(stats.slayer).toInt() and 0xFF)
            assertEquals(100, subject.vars[varbits.goading_potion_timer])
            assertEquals(10, npc.hitpoints)
            advance(6)
            assertMessageSent(GoadingAttackScript.ATTACK_MESSAGE, subject)
        }

    @Test
    fun GameTestState.`inactive goading timer does not attract monsters`() =
        runGameTest(GoadingAttackScript::class) {
            advance(9)
            val subject = registerPlayer(player.coords.translateX(400))
            subject.setBaseLevel(stats.slayer, 99)
            subject.setCurrentLevel(stats.slayer, 99)
            allocZoneCollision(subject.coords)
            spawnNpc(subject.coords.translateX(4), passiveMonster)

            advance(6)

            assertMessageNotSent(GoadingAttackScript.ATTACK_MESSAGE, subject)
        }

    @Test
    fun GameTestState.`goading does not steal a monster already interacting with another player`() =
        runGameTest(GoadingAttackScript::class) {
            advance(9)
            val subject = registerPlayer(player.coords.translateX(500))
            subject.setVarBit(varbits.goading_potion_timer, 100)
            subject.setBaseLevel(stats.slayer, 99)
            subject.setCurrentLevel(stats.slayer, 99)
            val other = registerPlayer(subject.coords.translateX(2))
            allocZoneCollision(subject.coords)
            val npc = spawnNpc(subject.coords.translateX(4), passiveMonster)
            npc.opPlayer2(other)

            advance(6)

            assertMessageNotSent(GoadingAttackScript.ATTACK_MESSAGE, subject)
            assertMessageSent(GoadingAttackScript.ATTACK_MESSAGE, other)
        }

    @Test
    fun GameTestState.`cache documented nibblers and Jad healers are immune to goading`() =
        runGameTest(GoadingAttackScript::class) {
            advance(9)
            val subject = registerPlayer(player.coords.translateX(600))
            subject.setVarBit(varbits.goading_potion_timer, 100)
            subject.setBaseLevel(stats.slayer, 99)
            subject.setCurrentLevel(stats.slayer, 99)
            allocZoneCollision(subject.coords)
            val immuneIds =
                listOf(
                    npcs.fight_caves_jad_healer.id,
                    npcs.inferno_nibbler.id,
                    npcs.inferno_jad_healer.id,
                    npcs.inferno_jad_healer_finalwave.id,
                    npcs.jad_challenge_healer.id,
                )
            immuneIds.forEachIndexed { index, id ->
                spawnNpc(
                    subject.coords.translate(index - 2, if (index % 2 == 0) 1 else -1),
                    npcTypeFactory.create(id) {
                        name = "Immune monster"
                        op[1] = "Attack"
                        size = 1
                        hitpoints = 10
                    },
                )
            }

            advance(6)

            assertMessageNotSent(GoadingAttackScript.ATTACK_MESSAGE, subject)
            assertEquals(listOf(3128, 7691, 7701, 7705, 10624), immuneIds)
        }

    private class GoadingAttackScript : PluginScript() {
        override fun ScriptContext.startup() {
            onAiOpPlayer2(passiveMonster) { it.target.mes(ATTACK_MESSAGE) }
            onAiOpPlayer2(slayerMonster) { it.target.mes(ATTACK_MESSAGE) }
        }

        companion object {
            const val ATTACK_MESSAGE = "The monster was goaded."
        }
    }

    private companion object {
        val passiveMonster =
            npcTypeFactory.create(id = 60_000) {
                name = "Passive monster"
                op[1] = "Attack"
                size = 1
                hitpoints = 10
            }

        val slayerMonster =
            npcTypeFactory.create(id = 60_001) {
                name = "Slayer monster"
                op[1] = "Attack"
                size = 1
                hitpoints = 10
                paramMap =
                    ParamMapBuilder().apply { this[params.slayer_levelrequire] = 50 }.toParamMap()
            }
    }
}
