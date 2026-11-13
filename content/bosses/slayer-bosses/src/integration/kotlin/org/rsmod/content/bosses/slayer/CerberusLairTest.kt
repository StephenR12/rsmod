package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.config.refs.*
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class CerberusLairTest {
    @Test
    fun GameTestState
        .`the Key Master lobby is freely accessible and three rooms enforce Slayer assignments`() =
        runGameTest(CerberusLair::class) {
            player.members = true
            fun click(id: Int, tile: CoordGrid) {
                val loc = placeMapLoc(tile, locTypes.getValue(id))
                player.placeAt(tile.translate(0, -1))
                player.opLoc1(loc)
                advance(3)
            }
            for (destination in
                listOf(
                    CoordGrid(1310, 1238),
                    CoordGrid(2873, 9847),
                    CoordGrid(1240, 1228),
                    CoordGrid(1304, 1292),
                    CoordGrid(1368, 1228),
                )) allocZoneCollision(destination)
            val cave = CoordGrid(2874, 9846)
            click(26567, cave)
            assertEquals(CoordGrid(1310, 1238), player.coords)
            val west = CoordGrid(1291, 1254)
            val winch = placeMapLoc(west, locTypes.getValue(23104))
            fun turn() {
                player.placeAt(west.translate(0, -1))
                player.opLoc1(winch)
                advance()
            }
            player.stats[stats.slayer] = 91
            turn()
            assertEquals(west.translate(0, -1), player.coords)
            player.withProtectedAccess {
                vars[varps.slayer_count] = 12
                vars[varps.slayer_target] = 31
            }
            player.setCurrentLevel(stats.slayer, 90)
            turn()
            assertEquals(west.translate(0, -1), player.coords)
            player.setCurrentLevel(stats.slayer, 91)
            player.setVarBit(cacheTypes.varbits.getValue(4067), 8)
            player.withProtectedAccess { vars[cacheTypes.varps.getValue(2096)] = 1 }
            turn()
            assertEquals(west.translate(0, -1), player.coords)
            player.withProtectedAccess { vars[cacheTypes.varps.getValue(2096)] = 6 }
            turn()
            assertEquals(CoordGrid(1240, 1228), player.coords)
            click(21772, CoordGrid(1239, 1225))
            assertEquals(west.translate(0, -1), player.coords)
            click(26566, CoordGrid(1310, 1236))
            assertEquals(CoordGrid(2873, 9847), player.coords)
        }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource("1307,1269,1303,1289,1", "1328,1254,1367,1225,2")
    fun `assigned Cerberus slayers can turn the native winch and return through its gate`(
        x: Int,
        z: Int,
        gx: Int,
        gz: Int,
        angle: Int,
        game: GameTestState,
    ) =
        game.runGameTest(CerberusLair::class) {
            player.members = true
            player.stats[stats.slayer] = 91
            player.setVarp(varps.slayer_count, 12)
            player.setVarp(varps.slayer_target, 98)
            player.setVarBit(game.cacheTypes.varbits.getValue(4723), 20)
            val near = if (angle == 1) CoordGrid(x + 2, z) else CoordGrid(x, z - 1)
            val winch =
                placeMapLoc(
                    CoordGrid(x, z),
                    locTypes.getValue(23104),
                    angle = org.rsmod.game.loc.LocAngle[angle],
                )
            val gate = placeMapLoc(CoordGrid(gx, gz), locTypes.getValue(21772))
            val inside = gate.coords.translate(1, 3)
            allocZoneCollision(inside)
            player.placeAt(near)
            assertTrue(npcTypes.getValue(5862).isAssignedSlayerTarget(player, inside))
            player.opLoc1(winch)
            advance(5)
            assertEquals(inside, player.coords)
            player.placeAt(gate.coords.translate(0, -1))
            player.opLoc1(gate)
            advance(5)
            assertEquals(near, player.coords)
        }

    @Test
    fun GameTestState
        .`key master scrolls respect activity and magic restrictions before consumption`() =
        runGameTest(CerberusLair::class) {
            player.members = true
            val scroll = objTypes.getValue(13249)
            player.inv[0] = InvObj(scroll, 2)
            player.activityBlocksTeleport = true
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(2, player.count(scroll))
            player.activityBlocksTeleport = false
            player.setVarBit(varbits.teleblock_cycles, 101)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(2, player.count(scroll))
            player.setVarBit(varbits.teleblock_cycles, 0)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(CoordGrid(1310, 1250), player.coords)
            assertEquals(1, player.count(scroll))
        }

    @Test
    fun GameTestState.`the Jailer and Velrak supply both keys for the deep dungeon route`() =
        runInjectedGameTest(Drops::class, null, TaverleyDungeon::class) { d ->
            player.members = true
            val jailer = spawnNpc(CoordGrid(2933, 9693), npcTypes.getValue(300))
            val result =
                requireNotNull(d.tables[jailer.type])
                    .roll(NpcDropContext(jailer, player, jailer.coords, d.random))
            assertEquals(listOf(objs.bones.id, 1591), result.map { it.obj.id })
            assertEquals(100, result.last().duration)
            val door = placeMapLoc(CoordGrid(2931, 9690), locTypes.getValue(2631))
            player.placeAt(door.coords)
            player.opLoc1(door)
            advance()
            assertEquals(door.coords, player.coords)
            player.inv[0] = result.last().obj
            player.opLoc1(door)
            advance()
            assertEquals(CoordGrid(2931, 9689), player.coords)
            val velrak = spawnNpc(CoordGrid(2931, 9688), npcTypes.getValue(4925))
            player.opNpc1(velrak)
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            assertEquals(1, player.count(objTypes.getValue(1590)))
            val gate = placeMapLoc(CoordGrid(2924, 9803), locTypes.getValue(2623))
            player.placeAt(gate.coords)
            player.opLoc1(gate)
            advance()
            assertEquals(CoordGrid(2923, 9803), player.coords)
        }

    @Test
    fun GameTestState
        .`Cerberus chooses one main branch including seven scrolls and one of four crystals`() =
        runInjectedGameTest(Drops::class, null, SlayerBossDrops::class) { d ->
            val boss = spawnNpc(CoordGrid(1238, 1250), npcTypes.getValue(5862))
            val table = requireNotNull(d.tables[boss.type])
            val ctx = NpcDropContext(boss, player, boss.coords, d.random)
            random.next = 124
            random.then = 1
            random.then = 1
            val scroll = table.roll(ctx)
            assertEquals(listOf(25778, 13249), scroll.map { it.obj.id })
            assertEquals(7, scroll.last().obj.count)
            random.next = 126
            random.then = 2
            random.then = 1
            random.then = 1
            assertEquals(listOf(25778, 13227), table.roll(ctx).map { it.obj.id })
        }

    class Drops @Inject constructor(val tables: NpcDropTableRegistry, val random: GameRandom)
}
