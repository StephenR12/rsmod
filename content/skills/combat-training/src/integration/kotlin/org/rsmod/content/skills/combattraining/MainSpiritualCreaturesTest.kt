package org.rsmod.content.skills.combattraining

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrs2.buffer.use
import org.openrs2.cache.Cache
import org.rsmod.map.square.MapSquareKey
import org.rsmod.api.cache.map.MapArchive
import org.rsmod.api.cache.map.MapFile
import org.rsmod.api.cache.map.npc.MapNpcDefinition
import org.rsmod.api.cache.map.npc.MapNpcListDecoder
import org.rsmod.api.cache.util.toInlineBuf
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.random.FixedRandom
import org.rsmod.map.CoordGrid

class MainSpiritualCreaturesTest {
    @Test
    fun GameTestState
        .`native main God Wars map records include each of the twelve variants on their dungeon floors`() =
        runGameTest {
            val counts = mutableMapOf<Int, Int>()
            val planes = mutableSetOf<Int>()
            val expected =
                mapOf(
                    2210 to 20,
                    2211 to 12,
                    2212 to 12,
                    2242 to 13,
                    2243 to 10,
                    2244 to 13,
                    3159 to 11,
                    3160 to 1,
                    3161 to 11,
                    3166 to 3,
                    3167 to 4,
                    3168 to 4,
                )
            Cache.open(Path.of(".data/cache/game")).use { cache ->
                for (mx in 44..45) for (mz in 81..83) {
                    val group = MapArchive(cache).read(MapSquareKey(mx, mz), MapFile.Npcs) ?: continue
                    group.use {
                        for (packed in MapNpcListDecoder.decode(it.toInlineBuf()).packedSpawns) {
                            val npc = MapNpcDefinition(packed)
                            if (npc.id !in expected) continue
                            counts[npc.id] = (counts[npc.id] ?: 0) + 1
                            planes += npc.level
                        }
                    }
                }
            }
            assertEquals(expected, counts)
            assertEquals(setOf(0, 1, 2), planes)
        }

    @Test
    fun GameTestState.`the same mage credits only the appropriate regional diary on each kill`() =
        runGameTest(CombatDiaryKills::class) {
            player.members = true
            val mage = npcTypes.getValue(2212)
            val main = spawnNpc(CoordGrid(2916, 5288, 1), mage)
            val wild = spawnNpc(CoordGrid(3027, 10124), mage)
            eventBus.publish(NpcKillEvents.Credited(player, main))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.GodWarsMage))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.WildGodWarsMage))
            eventBus.publish(NpcKillEvents.Credited(player, wild))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.WildGodWarsMage))
        }

    @Test
    fun GameTestState
        .`mages protect correctly at range respect attack speed and award dragon boots on one main slot`() =
        runInjectedGameTest(NpcDropTableRegistry::class, null, MainSpiritualCreatures::class) {
            registry ->
            player.members = true
            val mage = npcTypes.getValue(2212)
            val npc = spawnNpc(CoordGrid(2916, 5288, 1), mage)
            player.teleport(npc.coords.translateX(3))
            player.setVarBit(varbits.protect_from_magic, 1)
            val hp = player.hitpoints
            npc.apPlayer2(player)
            advance(3)
            assertTrue(npc.actionDelay >= mapClock.cycle)
            assertEquals(hp, player.hitpoints)
            assertEquals(4, mage.param(params.attackrate))
            assertEquals(850, mage.param(params.slayer_experience))
            assertEquals(83, mage.param(params.slayer_levelrequire))
            val table = requireNotNull(registry[mage])
            val awards = table.roll(NpcDropContext(npc, player, npc.coords, FixedRandom(123)))
            assertEquals(listOf(11840), awards.map { it.obj.id })
            assertSame(player, awards.single().receiver)
        }
}
