package org.rsmod.content.bosses.world

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList

@Execution(ExecutionMode.SAME_THREAD)
class WorldBossTest {
    @Test
    fun GameTestState
        .`Sarachnis spawns two pairs after damaging hits and removes the survivors on death`() =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(WorldBossScript::class, NpcHitScript::class),
        ) { deps ->
            val room =
                WorldBosses.definitions.single { it.family == WorldBossFamily.Sarachnis }.spawn
            val boss = deps.npcs.findAll(room).single { it.id == WorldBossNpcs.sarachnis.id }
            player.placeAt(room.translate(0, 3))
            val state = deps.controller.state(boss)
            boss.queueHit(player, 1, HitType.Melee, 140)
            advance(2)
            assertEquals(2, state.spawns.size)
            assertEquals(setOf(8714, 8715), state.spawns.map { it.id }.toSet())
            boss.queueHit(player, 1, HitType.Melee, 135)
            advance(2)
            assertEquals(4, state.spawns.size)
            assertTrue(state.spawns.all { it.hitpoints == 30 })
            eventBus.publish(NpcKillEvents.Credited(player, boss))
            advance()
            assertTrue(state.spawns.none { it.isSlotAssigned })
        }

    @Test
    fun GameTestState
        .`Sarachnis pre-roll and ordinary tables are exclusive and seed quantities scale`() =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(WorldBossScript::class, WorldBossDrops::class),
        ) { deps ->
            val room =
                WorldBosses.definitions.single { it.family == WorldBossFamily.Sarachnis }.spawn
            val boss = deps.npcs.findAll(room).single { it.id == WorldBossNpcs.sarachnis.id }
            val table = requireNotNull(deps.drops[boss.type])
            fun roll(vararg draws: Int): org.rsmod.game.inv.InvObj {
                val sequence = SequenceRandom()
                for (draw in draws) sequence.next = draw
                repeat(5) { sequence.next = 1 }
                return table.roll(NpcDropContext(boss, player, room, sequence)).single().obj
            }
            assertEquals(1149, roll(0).id)
            assertEquals(WorldBossObjs.sarachnisCudgel.id, roll(2).id)
            assertEquals(org.rsmod.game.inv.InvObj(objTypes.getValue(565), 91), roll(3, 8, 91))
            assertEquals(org.rsmod.game.inv.InvObj(objTypes.getValue(5296), 3), roll(383, 87, 0))
        }

    @Test
    fun GameTestState.`four uncovered world bosses now spawn with registered drops`() =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(WorldBossScript::class, WorldBossDrops::class),
        ) { deps ->
            val bosses =
                WorldBosses.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(4, bosses.size)
            assertTrue(bosses.all { it.respawns })
            assertTrue(WorldBosses.definitions.all { deps.drops[it.npc] != null })
        }

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val controller: WorldBossController,
    )
}
