package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.*
import org.rsmod.api.death.*
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.diary.*
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.combattraining.CombatDiaryKills
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RegionalSlayerDungeonsTest {
    @Test
    fun GameTestState
        .`Pirate Pete rejects unstarted quest then sails both ways without changing quest progress`() =
        game { d ->
            player.members = true
            player.placeAt(CoordGrid(3679, 3535))
            val pete = spawnNpc(CoordGrid(3680, 3535), npcTypes[RegionalDungeonNpcs.pete])
            player.opNpc1(pete)
            advance(4)
            assertEquals(CoordGrid(3679, 3535), player.coords)
            player.setVarp(RegionalDungeonVars.rum, 19)
            allocZoneCollision(CoordGrid(2156, 5111))
            player.opNpc1(pete)
            advance(4)
            assertEquals(CoordGrid(2156, 5111), player.coords)
            val back = spawnNpc(CoordGrid(2155, 5111), npcTypes[RegionalDungeonNpcs.islandPete])
            player.opNpc1(back)
            advance(4)
            assertEquals(CoordGrid(3680, 3534), player.coords)
            assertEquals(19, player.vars[RegionalDungeonVars.rum])
        }

    @Test
    fun GameTestState
        .`native brewery stair and ladder route reaches and leaves fever spider basement`() =
        game { d ->
            player.members = true
            val stair =
                placeMapLoc(
                    CoordGrid(2151, 5109),
                    locTypes[RegionalDungeonLocs.bottom],
                    angle = org.rsmod.game.loc.LocAngle.South,
                )
            player.placeAt(CoordGrid(2156, 5111))
            allocZoneCollision(CoordGrid(2152, 5107, 1))
            player.opLoc1(stair)
            advance(20)
            assertEquals(1, player.level)
            val ladder =
                placeMapLoc(
                    CoordGrid(2139, 5105, 1),
                    locTypes[RegionalDungeonLocs.ladderTop],
                    angle = org.rsmod.game.loc.LocAngle.South,
                )
            allocZoneCollision(CoordGrid(2139, 5104))
            player.opLoc1(ladder)
            advance(20)
            assertEquals(CoordGrid(2139, 5104), player.coords)
            val up =
                placeMapLoc(
                    CoordGrid(2139, 5105),
                    locTypes[RegionalDungeonLocs.ladder],
                    angle = org.rsmod.game.loc.LocAngle.South,
                )
            player.opLoc1(up)
            advance(3)
            assertEquals(CoordGrid(2139, 5104, 1), player.coords)
        }

    @Test
    fun GameTestState.`repeatable terror dog chamber requires both native quest end states`() =
        game { d ->
            player.members = true
            player.placeAt(CoordGrid(3185, 4601))
            allocZoneCollision(CoordGrid(3149, 4644))
            player.withProtectedAccess { assertFalse(d.routes.enterTarn(this)) }
            player.setVarp(RegionalDungeonVars.mine, 11)
            player.withProtectedAccess { assertFalse(d.routes.enterTarn(this)) }
            player.setVarBit(RegionalDungeonBits.tarn, 3)
            val door = placeMapLoc(CoordGrid(3185, 4602), locTypes[RegionalDungeonLocs.tarnDoor])
            player.opLoc1(door)
            advance(3)
            assertEquals(CoordGrid(3149, 4644), player.coords)
            val exit = placeMapLoc(CoordGrid(3149, 4643), locTypes[RegionalDungeonLocs.tarnExit])
            player.opLoc1(exit)
            advance(3)
            assertEquals(CoordGrid(3185, 4601), player.coords)
        }

    @Test
    fun GameTestState
        .`Slayer ring uses acquired native charges and quest gate before Tarn teleport`() =
        game { d ->
            player.members = true
            player.inv[3] = InvObj(RegionalDungeonItems.rings[0])
            val start = player.coords
            player.withProtectedAccess { assertFalse(d.routes.ringTeleport(this, inv, 3, 3)) }
            advance(6)
            assertEquals(start, player.coords)
            assertEquals(RegionalDungeonItems.rings[0].id, player.inv[3]?.id)
            player.setVarp(RegionalDungeonVars.mine, 11)
            allocZoneCollision(CoordGrid(3185, 4601))
            player.withProtectedAccess { d.routes.ringTeleport(this, inv, 3, 3) }
            advance(6)
            assertEquals(CoordGrid(3185, 4601), player.coords)
            assertEquals(RegionalDungeonItems.gem.id, player.inv[3]?.id)
        }

    @Test
    fun GameTestState
        .`all four Molch dwellings enter native level one and temple hole returns to pyramid`() =
        game { d ->
            player.members = true
            for (i in 0..3) {
                player.placeAt(CoordGrid(2000, 2000))
                val dest =
                    listOf(
                        CoordGrid(1292, 10062, 1),
                        CoordGrid(1314, 10066, 1),
                        CoordGrid(1330, 10072, 1),
                        CoordGrid(1312, 10090, 1),
                    )[i]
                allocZoneCollision(dest)
                player.withProtectedAccess { assertTrue(d.routes.enterMolch(this, i)) }
                assertEquals(dest, player.coords)
            }
            val exit =
                placeMapLoc(CoordGrid(1292, 10077, 1), locTypes[RegionalDungeonLocs.templeExit])
            player.placeAt(CoordGrid(1291, 10077, 1))
            allocZoneCollision(CoordGrid(1305, 3666))
            player.opLoc1(exit)
            advance(3)
            assertEquals(CoordGrid(1305, 3666), player.coords)
        }

    @Test
    fun GameTestState.`Molch barriers cross the actual axis and escalate bounded passage damage`() =
        game { d ->
            player.members = true
            player.stats[stats.hitpoints] = 99
            val c = CoordGrid(1297, 10096, 1)
            allocZoneCollision(c)
            for (i in 1..6) {
                player.placeAt(c.translate(-1, 0))
                val hp = player.stats[stats.hitpoints]
                player.withProtectedAccess { assertTrue(d.routes.passBarrier(this, c)) }
                assertEquals(c.translate(2, 0), player.coords)
                assertEquals(
                    hp - if (i >= 6) 20 else if (i >= 3) 10 else 1,
                    player.stats[stats.hitpoints],
                )
            }
            assertEquals(2, player.vars[RegionalDungeonBits.barriers[1]])
        }

    @Test
    fun GameTestState
        .`actual credited spider dog and level one temple shaman deaths advance their diary tasks`() =
        game { d ->
            player.members = true
            player.observerUUID = player.uuid
            for ((id, c, task) in
                listOf(
                    Triple(626, CoordGrid(2142, 5097), DiaryTask.BraindeathFeverSpider),
                    Triple(6474, CoordGrid(3140, 4647), DiaryTask.TerrorDog),
                    Triple(8565, CoordGrid(1290, 10098, 1), DiaryTask.TempleShaman),
                )) {
                player.placeAt(c.translate(-1, 0))
                val npc = spawnNpc(c, npcTypes.getValue(id))
                npc.queueHit(player, 1, HitType.Melee, npc.hitpoints)
                advance()
                assertTrue(player.hasCompletedDiaryTask(task), "$id at $c")
            }
        }

    @Test
    fun GameTestState.`same temple shaman on wiki display floor does not receive task credit`() =
        game { d ->
            player.members = true
            player.observerUUID = player.uuid
            player.placeAt(CoordGrid(1289, 10098))
            val npc = spawnNpc(CoordGrid(1290, 10098), npcTypes[RegionalDungeonNpcs.shaman])
            npc.queueHit(player, 1, HitType.Melee, npc.hitpoints)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.TempleShaman))
        }

    @Test
    fun GameTestState
        .`native repeatable populations have live combat stats and ordinary source backed loot`() =
        game { d ->
            assertEquals(40, npcTypes[RegionalDungeonNpcs.fever].hitpoints)
            assertEquals(42, npcTypes[RegionalDungeonNpcs.fever].param(params.slayer_levelrequire))
            assertEquals(82, npcTypes[RegionalDungeonNpcs.dogs[0]].hitpoints)
            assertEquals(87, npcTypes[RegionalDungeonNpcs.dogs[1]].hitpoints)
            assertEquals(150, npcTypes[RegionalDungeonNpcs.shaman].hitpoints)
            val spider =
                spawnNpc(player.coords.translate(1, 0), npcTypes[RegionalDungeonNpcs.fever])
            random.next = 0
            val loot =
                d.drops[spider.visType]!!.roll(
                    NpcDropContext(spider, player, spider.coords, d.random)
                )
            assertEquals(1357, loot.single().obj.id)
            val dog = spawnNpc(player.coords.translate(3, 0), npcTypes[RegionalDungeonNpcs.dogs[0]])
            random.next = 0
            val dogLoot =
                d.drops[dog.visType]!!.roll(NpcDropContext(dog, player, dog.coords, d.random))
            assertTrue(dogLoot.any { it.obj.id == 526 })
        }

    @Test
    fun GameTestState.`transport landings occupy walkable native terrain and scenery`() =
        game { d ->
            val landings =
                listOf(
                    CoordGrid(2156, 5111),
                    CoordGrid(2152, 5107, 1),
                    CoordGrid(2139, 5104),
                    CoordGrid(2139, 5104, 1),
                    CoordGrid(3149, 4644),
                    CoordGrid(3185, 4601),
                    CoordGrid(1292, 10062, 1),
                    CoordGrid(1314, 10066, 1),
                    CoordGrid(1330, 10072, 1),
                    CoordGrid(1312, 10090, 1),
                    CoordGrid(1305, 3666),
                )
            val blocked =
                landings.filter { c ->
                    allocZoneCollision(c)
                    d.collision.isWalkBlocked(c)
                }
            assertTrue(
                blocked.isEmpty(),
                blocked.joinToString("\n") { c ->
                    "$c blocked; alternatives=" +
                        (-2..2)
                            .flatMap { x -> (-2..2).map { z -> c.translate(x, z) } }
                            .filter { !d.collision.isWalkBlocked(it) }
                },
            )
        }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            null,
            RegionalSlayerDungeons::class,
            NpcHitScript::class,
            CombatDiaryKills::class,
            testBody = body,
        )

    data class Deps
    @Inject
    constructor(
        val routes: RegionalSlayerDungeons,
        val drops: NpcDropTableRegistry,
        val random: GameRandom,
        val collision: org.rsmod.routefinder.collision.CollisionFlagMap,
    )
}
