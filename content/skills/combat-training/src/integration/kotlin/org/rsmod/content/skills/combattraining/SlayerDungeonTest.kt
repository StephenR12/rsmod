package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.openrs2.buffer.use
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.map.GameMapDecoder
import org.rsmod.api.cache.map.npc.MapNpcListDecoder
import org.rsmod.api.cache.util.toInlineBuf
import org.rsmod.api.combat.commons.npc.inKonarArea
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.combat.commons.npc.requiresSlayerAssignment
import org.rsmod.api.combat.commons.npc.slayerFamily
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.plugin.NpcDeathScript
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.feet
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.NpcList
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey

@Execution(ExecutionMode.SAME_THREAD)
class SlayerDungeonTest {
    private class World
    @Inject
    constructor(
        val npcs: NpcList,
        val repo: NpcRepository,
        val drops: NpcDropTableRegistry,
        val rng: GameRandom,
        @org.rsmod.api.random.CoreRandom val deathRandom: GameRandom,
    )

    @Test
    fun GameTestState.`packed diary bosses can dispatch ranged attacks at their configured distances`() =
        runGameTest {
            val expected = mapOf(
                239 to 10, 494 to 10, 963 to 5, 2042 to 20, 2043 to 20, 2044 to 20,
                2265 to 5, 2266 to 5, 2267 to 1, 5862 to 14, 6610 to 18, 6611 to 18,
                6615 to 1, 6619 to 10, 7286 to 10, 7527 to 1, 7533 to 7, 7566 to 8,
                7573 to 8, 7604 to 10, 7605 to 10, 7606 to 10,
            )
            for ((id, range) in expected) {
                assertEquals(range, npcTypes.getValue(id).attackRange, "NPC $id")
            }
        }

    @Test
    fun GameTestState.`packed map places every ordinary family and separates their task rooms`() =
        runInjectedGameTest(
            World::class,
            null,
            SlayerDungeonMonsters::class,
            CombatDiaryKills::class,
            NpcHitScript::class,
            NpcDeathScript::class,
        ) { world ->
            val squares =
                listOf(
                    47 to 149,
                    48 to 149,
                    19 to 159,
                    19 to 158,
                    20 to 159,
                    20 to 160,
                    56 to 159,
                    56 to 160,
                    21 to 66,
                    23 to 66,
                    23 to 67,
                )
            Cache.open(Path.of(".data/cache/game")).use { cache ->
                for ((x, z) in squares) {
                    val name = "n${x}_$z"
                    val group =
                        cache.read(Js5Archives.MAPS, name, 0).use {
                            MapNpcListDecoder.decode(it.toInlineBuf())
                        }
                    GameMapDecoder.putNpcs(world.repo, npcTypes, MapSquareKey(x, z), group)
                }
            }
            advance(2)
            val counts =
                mapOf(
                    465 to 6,
                    466 to 6,
                    467 to 5,
                    468 to 5,
                    7792 to 8,
                    7793 to 8,
                    7794 to 8,
                    7795 to 5,
                    8609 to 17,
                    8610 to 16,
                    12490 to 17,
                ) + (12491..12498).associateWith { if (it < 12495) 5 else 4 }
            val live = world.npcs.filter { it.id in counts }
            assertEquals(counts, live.groupingBy { it.id }.eachCount())
            assertEquals(6, npcTypes.getValue(8610).attackRange)
            assertEquals(8, npcTypes.getValue(8609).attackRange)
            assertEquals(
                14,
                live.count { it.id in 465..468 && it.requiresSlayerAssignment(player) },
            )
            assertEquals(
                15,
                live.count { it.id in 7792..7795 && it.requiresSlayerAssignment(player) },
            )
            assertEquals(7, live.count { it.id == 8610 && it.requiresSlayerAssignment(player) })
            assertEquals(8, live.count { it.id == 8609 && it.requiresSlayerAssignment(player) })
            assertTrue(
                live.all {
                    inKonarArea(
                        when (it.id) {
                            in 465..468 -> 33
                            in 7792..7795 -> 32
                            8609,
                            8610 -> 3
                            else -> 49
                        },
                        it.spawnCoords,
                    )
                }
            )
            (world.deathRandom as org.rsmod.api.testing.random.FixedRandom).set(1)
            player.members = true
            player.observerUUID = player.uuid
            for ((id, diary) in
                listOf(
                    465 to DiaryTask.AsgarnianSkeletalWyvern,
                    8610 to DiaryTask.KaruulmWyrm,
                    8609 to DiaryTask.KaruulmHydra,
                )) {
                val n = live.first { it.id == id && !it.requiresSlayerAssignment(player) }
                player.placeAt(n.coords.translate(-1, 0))
                assertFalse(player.hasCompletedDiaryTask(diary))
                n.queueHit(player, 1, HitType.Melee, 400)
                advance(9)
                assertTrue(player.hasCompletedDiaryTask(diary))
            }
            val wyvern = live.first { it.id == 7795 && it.requiresSlayerAssignment(player) }
            assertEquals(82, wyvern.visType.param(params.slayer_levelrequire))
            assertEquals(300, wyvern.hitpoints)
            assertFalse(wyvern.isAssignedSlayerTarget(player))
            player.setVarp(varps.slayer_target, wyvern.visType.slayerFamily)
            player.setVarp(varps.slayer_count, 10)
            assertTrue(wyvern.isAssignedSlayerTarget(player))
        }

    @Test
    fun GameTestState.`native collision connects ordinary dungeon arrivals to monster chambers`() =
        runGameTest {
            val squares =
                listOf(
                    46 to 149,
                    47 to 149,
                    48 to 149,
                    19 to 158,
                    19 to 159,
                    20 to 159,
                    20 to 160,
                    56 to 159,
                    56 to 160,
                    21 to 66,
                    23 to 66,
                    23 to 67,
                    43 to 66,
                    40 to 69,
                )
            val terrain = org.rsmod.routefinder.collision.CollisionFlagMap()
            val routes = org.rsmod.api.route.RouteFactory(terrain)
            Cache.open(Path.of(".data/cache/game")).use { cache ->
                val builder = org.rsmod.api.cache.map.GameMapBuilder()
                for ((x, z) in squares) {
                    val square = MapSquareKey(x, z)
                    val group = "${x}_$z"
                    val map =
                        cache.read(Js5Archives.MAPS, "m$group", 0).use {
                            org.rsmod.api.cache.map.tile.MapTileDecoder.decode(it.toInlineBuf())
                        }
                    val key =
                        org.openrs2.crypto.SymmetricKey.fromIntArray(
                            xteaRepository.getValue(square)
                        )
                    val locs =
                        cache.read(Js5Archives.MAPS, "l$group", 0, key).use {
                            org.rsmod.api.cache.map.loc.MapLocListDecoder.decode(it.toInlineBuf())
                        }
                    GameMapDecoder.putMaps(terrain, square, map)
                    GameMapDecoder.putLocs(builder, terrain, locTypes, square, map, locs)
                }
            }
            val failures = mutableListOf<String>()
            fun path(from: CoordGrid, to: CoordGrid) {
                player.avatar.coords = from
                val route = routes.create(player.avatar, to)
                if (!route.success || route.alternative)
                    failures += "$from->$to; alternative=${route.lastOrNull()}"
            }
            path(CoordGrid(3007, 9550), CoordGrid(3056, 9563))
            path(CoordGrid(3056, 9555), CoordGrid(3025, 9543))
            path(CoordGrid(3060, 9559), CoordGrid(3071, 9581))
            path(CoordGrid(1311, 10187), CoordGrid(1304, 10205))
            path(CoordGrid(1301, 10205), CoordGrid(1268, 10191))
            path(CoordGrid(1311, 10217), CoordGrid(1311, 10246))
            path(CoordGrid(3604, 10230), CoordGrid(3599, 10186))
            path(CoordGrid(3596, 10290), CoordGrid(3603, 10291))
            path(CoordGrid(3605, 10287), CoordGrid(3612, 10274))
            path(CoordGrid(1473, 4238), CoordGrid(1476, 4237))
            path(CoordGrid(1479, 4240, 1), CoordGrid(1487, 4238, 1))
            path(CoordGrid(1488, 4238, 1), CoordGrid(1491, 4238, 1))
            path(CoordGrid(1492, 4238, 1), CoordGrid(1493, 4239, 1))
            path(CoordGrid(2596, 4435), CoordGrid(2608, 4450))
            path(CoordGrid(2789, 4242), CoordGrid(2789, 4257))
            assertEquals(emptyList<String>(), failures)
        }

    @Test
    fun GameTestState
        .`completed quest access restores the lost chime and reopens all four dungeons`() =
        runInjectedGameTest(World::class, null, SlayerDungeonAccess::class) {
            player.members = true
            fun click(id: Int, x: Int, z: Int, level: Int = 0) {
                val c = CoordGrid(x, z, level)
                val type = locTypes.getValue(id)
                val loc = placeMapLoc(c, type)
                player.withProtectedAccess { eventBus.publish(this, LocEvents.Op1(loc, loc, type)) }
            }
            click(1738, 3008, 3150)
            assertEquals(CoordGrid(3007, 9550), player.coords)
            click(10596, 3055, 9561)
            assertEquals(CoordGrid(3056, 9555), player.coords)
            click(8729, 3060, 9556)
            assertEquals(CoordGrid(3060, 9559), player.coords)
            click(8729, 3060, 9556)
            assertEquals(CoordGrid(3060, 9554), player.coords)
            val fossil = cacheTypes.varbits.getValue(5795)
            player.setVarBit(fossil, 49)
            click(30869, 3745, 3777)
            assertEquals(CoordGrid(3060, 9554), player.coords)
            player.setVarBit(fossil, 50)
            click(30869, 3745, 3777)
            assertEquals(CoordGrid(3604, 10230), player.coords)
            click(30878, 3603, 10232)
            assertEquals(CoordGrid(3746, 3780), player.coords)
            click(30842, 3677, 3853)
            assertEquals(CoordGrid(3596, 10290), player.coords)
            click(34513, 1310, 3807)
            assertEquals(CoordGrid(1311, 10187), player.coords)
            player.feet = InvObj(objTypes.getValue(23037))
            click(34544, 1311, 10215)
            assertEquals(CoordGrid(1311, 10217), player.coords)
            click(34514, 1311, 10185)
            assertEquals(CoordGrid(1311, 3806), player.coords)
            val path = cacheTypes.varbits.getValue(15288)
            player.setVarBit(path, 49)
            click(49868, 2322, 3100)
            assertEquals(CoordGrid(1311, 3806), player.coords)
            player.setVarBit(path, 50)
            click(5250, 2533, 3155)
            click(49620, 2608, 4451)
            click(49612, 2789, 4258)
            assertTrue(objTypes.getValue(28575) in player.inv)
            click(49611, 2793, 4256)
            assertTrue(objTypes.getValue(28577) in player.inv)
            click(49868, 2322, 3100)
            assertEquals(CoordGrid(1473, 4238), player.coords)
            click(49754, 1472, 4236)
            assertEquals(CoordGrid(2321, 3100), player.coords)
        }

    @Test
    fun GameTestState
        .`both warped families require the crystal chime and can drop an uncharged sceptre`() =
        runInjectedGameTest(
            World::class,
            null,
            SlayerDungeonMonsters::class,
            NpcHitScript::class,
        ) { world ->
            player.members = true
            player.observerUUID = player.uuid
            for (id in listOf(12490, 12491)) {
                val n = spawnNpc(CoordGrid(1484, 4237, 1), npcTypes.getValue(id))
                val hp = n.hitpoints
                player.inv[0] = null
                n.queueHit(player, 1, HitType.Melee, 10)
                advance(2)
                assertEquals(hp, n.hitpoints)
                player.inv[0] = InvObj(objTypes.getValue(28577))
                n.queueHit(player, 1, HitType.Melee, 10)
                advance(2)
                assertEquals(hp - 10, n.hitpoints)
                random.next = 0
                random.then = 0
                val loot =
                    world.drops[n.type]!!.roll(NpcDropContext(n, player, n.coords, world.rng)).map {
                        it.obj.id
                    }
                assertTrue(28583 in loot)
                assertTrue((if (id == 12490) 532 else 526) in loot)
            }
        }
}
