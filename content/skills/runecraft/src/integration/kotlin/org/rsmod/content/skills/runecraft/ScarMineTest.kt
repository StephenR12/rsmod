package org.rsmod.content.skills.runecraft

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class ScarMineTest {
    @Test
    fun GameTestState
        .`the catalytic guardian and stepping stones lead through the native quest gated passage`() =
        runGameTest(ScarMine::class) {
            player.members = true
            val scar = cacheTypes.varbits.getValue(15128)
            val guardian = spawnNpc(CoordGrid(3611, 9472), npcTypes.getValue(12417))
            player.teleport(CoordGrid(3610, 9472))
            player.setVarBit(cacheTypes.varbits.getValue(15130), 2)
            player.setVarBit(scar, 38)
            player.opNpc3(guardian)
            advance(3)
            assertEquals(CoordGrid(2013, 6432), player.coords)
            val first =
                placeMapLoc(
                    CoordGrid(2031, 6430),
                    locTypes.getValue(49209),
                    LocShape.GroundDecor,
                    LocAngle.South,
                )
            player.teleport(CoordGrid(2029, 6430))
            player.opLoc1(first)
            advance(3)
            assertEquals(CoordGrid(2035, 6430), player.coords)
            val last =
                placeMapLoc(CoordGrid(2041, 6427), locTypes.getValue(49923), LocShape.GroundDecor)
            player.teleport(CoordGrid(2041, 6429))
            player.setVarBit(scar, 37)
            player.opLoc1(last)
            advance(3)
            assertEquals(CoordGrid(2041, 6429), player.coords)
            player.setVarBit(scar, 38)
            player.opLoc1(last)
            advance(3)
            assertEquals(CoordGrid(2041, 6425), player.coords)
            val passage =
                placeMapLoc(CoordGrid(2040, 6423), locTypes.getValue(49926), angle = LocAngle.North)
            player.opLoc1(passage)
            advance(4)
            assertEquals(CoordGrid(1952, 6349), player.coords)
            player.opLoc1(
                placeMapLoc(CoordGrid(1952, 6348), locTypes.getValue(49922), angle = LocAngle.North)
            )
            advance(3)
            assertEquals(CoordGrid(2041, 6424), player.coords)
        }

    @Test
    fun GameTestState.`mining a deposit consumes one celestial charge and the rock replenishes`() =
        runGameTest(ScarMine::class) {
            val at = CoordGrid(1939, 6367)
            val rock = placeMapLoc(at, locTypes.getValue(49912))
            val empty = locTypes.getValue(49915)
            player.teleport(at.translate(1, 0))
            player.members = true
            player.setVarBit(cacheTypes.varbits.getValue(15128), 38)
            val chunk = objTypes.getValue(28591)
            player.inv[0] = InvObj(objs.bronze_pickaxe)
            player.setBaseLevel(stats.mining, 63)
            player.setCurrentLevel(stats.mining, 63)
            player.opLoc1(rock)
            advance(8)
            assertEquals(0, player.count(chunk))
            player.worn[12] = InvObj(objTypes.getValue(25541), 1, 1)
            player.setBaseLevel(stats.mining, 64)
            player.setCurrentLevel(stats.mining, 64)
            random.next = 0
            random.then = 0
            random.then = 0
            random.then = 0
            player.opLoc1(rock)
            advance(12)
            assertEquals(2, player.count(chunk))
            assertEquals(25539, player.worn[12]?.id)
            assertTrue(locExists(at, empty))
            advance(26)
            assertTrue(locExists(at, locTypes.getValue(49912)))
        }

    @Test
    fun GameTestState
        .`the Ventriculus stores five units per chunk and charges only for delivered extracts`() =
        runGameTest(ScarMine::class) {
            player.members = true
            player.setVarBit(cacheTypes.varbits.getValue(15128), 38)
            val stored = cacheTypes.varbits.getValue(15318)
            val coins = cacheTypes.varps.getValue(4002)
            val chunk = objTypes.getValue(28591)
            val opening =
                placeMapLoc(CoordGrid(1951, 6368), locTypes.getValue(49918), angle = LocAngle.South)
            player.teleport(CoordGrid(1951, 6367))
            repeat(3) { player.inv[it] = InvObj(chunk) }
            player.opLoc1(opening)
            advance(3)
            assertEquals(15, player.vars[stored])
            assertEquals(0, player.count(chunk))
            val clean =
                placeMapLoc(CoordGrid(1949, 6368), locTypes.getValue(49920), angle = LocAngle.South)
            player.teleport(CoordGrid(1949, 6367))
            player.opLoc1(clean)
            advance(2)
            player.resumeCountDialog(3)
            advance()
            assertEquals(9, player.vars[stored])
            assertEquals(
                3,
                player.count(objTypes.getValue(objTypes[RunecraftObjs.pureEssence].certlink)),
            )
            val coffer = placeMapLoc(CoordGrid(1952, 6362), locTypes.getValue(49921))
            player.teleport(CoordGrid(1952, 6363))
            player.inv[1] = InvObj(objs.coins, 12500)
            player.opLoc1(coffer)
            advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
            player.resumeCountDialog(12500)
            advance()
            assertEquals(12500, player.vars[coins])
            assertEquals(0, player.count(objs.coins))
            val extract =
                placeMapLoc(CoordGrid(1953, 6368), locTypes.getValue(49919), angle = LocAngle.South)
            player.teleport(CoordGrid(1953, 6367))
            fun buy() {
                player.opLoc1(extract)
                advance(2)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 1))
                player.resumeCountDialog(2)
                advance()
            }
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.beer)
            buy()
            assertEquals(12500, player.vars[coins])
            assertEquals(9, player.vars[stored])
            player.inv[1] = null
            buy()
            assertEquals(2, player.count(RunecraftObjs.twistedExtract))
            assertEquals(500, player.vars[coins])
            assertEquals(7, player.vars[stored])
        }
}
