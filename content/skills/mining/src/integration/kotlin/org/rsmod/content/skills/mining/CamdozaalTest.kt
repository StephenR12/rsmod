package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.mining.camdozaal.*
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

class CamdozaalTest {
    @Test
    fun GameTestState.freePlayerMinesAndCrushesBarroniteAndBuysAPermanentMiningCharm() =
        runGameTest(Mining::class, CalcifiedDeposits::class, CamdozaalForge::class) {
            player.members = false
            player.stats[stats.mining] = 50
            player.stats[stats.smithing] = 14
            player.righthand = InvObj(MiningObjs.runePickaxe)
            val rock = placeMapLoc(CoordGrid(2924, 5822), locTypes.getValue(41547))
            player.telejump(rock.coords.translateX(-1))
            player.opLoc1(rock)
            advance(7)
            assertEquals(0, player.count(MiningObjs.barroniteShards))
            player.setVarBit(cacheTypes.varbits.getValue(12063), 120)
            player.opLoc1(rock)
            advance()
            random.next = 128
            random.then = 0
            random.then = 0
            random.then = 180
            advance(4)
            assertEquals(1, player.count(MiningObjs.barroniteDeposit))
            player.moveGameClick(player.coords.translate(-1, 0))
            advance()
            val crusher = placeMapLoc(CoordGrid(2956, 5807), locTypes.getValue(41551))
            player.telejump(crusher.coords.translateX(-1))
            player.inv[7] = InvObj(objs.hammer)
            val smith = player.statMap.getFineXP(stats.smithing)
            random.next = 50000
            random.then = 1
            random.then = 7
            player.opLoc1(crusher)
            advance(5)
            assertEquals(7, player.count(MiningObjs.barroniteShards))
            assertEquals(300, player.statMap.getFineXP(stats.smithing) - smith)
            player.setVarBit(cacheTypes.varbits.getValue(12068), 2)
            player.setVarp(cacheTypes.varps.getValue(2953), 2493)
            val npc = spawnNpc(player.coords.translate(0, 1), npcTypes.getValue(10684))
            player.opNpc3(npc)
            advance(3)
            assertModalOpen(cacheTypes.interfaces.getValue(685))
            player.ifButton(cacheTypes.components.getValue((685 shl 16) or 8), op = IfButtonOp.Op1)
            advance()
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(12102)])
            assertEquals(0, player.vars[cacheTypes.varps.getValue(2953)])
            assertEquals(0, player.count(MiningObjs.barroniteShards))
        }

    @Test
    fun GameTestState.ritualFishingUsesTheNetKnifeAndAltarOnAFreeWorld() =
        runGameTest(CamdozaalFishing::class) {
            player.members = false
            player.setVarBit(cacheTypes.varbits.getValue(12063), 120)
            player.stats[stats.fishing] = 7
            player.stats[stats.cooking] = 42
            player.inv[0] = InvObj(objs.small_fishing_net)
            player.inv[1] = InvObj(objs.knife)
            val spot = spawnNpc(CoordGrid(2928, 5777), npcTypes.getValue(10686))
            player.telejump(spot.coords.translateX(1))
            player.opNpc1(spot)
            advance(2)
            random.next = 0
            random.then = 0
            random.then = 75000
            advance(5)
            assertEquals(1, player.count(objTypes.getValue(25652)))
            val table = placeMapLoc(CoordGrid(2935, 5774), locTypes.getValue(41545))
            player.telejump(table.coords.translateX(-1))
            player.opLoc1(table)
            advance(5)
            assertEquals(1, player.count(objTypes.getValue(25654)))
            assertEquals(0, player.count(objTypes.getValue(25652)))
            val altar = placeMapLoc(CoordGrid(2937, 5770), locTypes.getValue(41546))
            player.telejump(altar.coords.translateX(-1))
            val prayer = player.statMap.getFineXP(stats.prayer)
            player.opLoc1(altar)
            advance(4)
            assertEquals(0, player.count(objTypes.getValue(25654)))
            assertEquals(40, player.statMap.getFineXP(stats.prayer) - prayer)
        }

    @Test
    fun GameTestState.vaultConvertsOnlyThisRunsBoxesOnEscapeAndForfeitsThemOnTimeout() =
        runGameTest(CamdozaalVault::class) {
            player.setVarBit(cacheTypes.varbits.getValue(12063), 120)
            player.setVarp(cacheTypes.varps.getValue(2953), 1500)
            val door =
                placeMapLoc(CoordGrid(2969, 5824), locTypes.getValue(41537), angle = LocAngle.East)
            val box = placeMapLoc(CoordGrid(2955, 5855), locTypes.getValue(41389))
            player.telejump(door.coords.translate(1, -1))
            player.inv[0] = InvObj(objTypes.getValue(25647))
            fun enter() {
                player.opLoc1(door)
                advance(3)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
                advance(2)
            }
            fun take() {
                val bit = cacheTypes.varbits.getValue(12088)
                assertTrue(player.vars[bit] == 1) { "Ornate lockbox visibility at entry" }
                player.telejump(box.coords.translate(0, -1))
                assertTrue(locExists(box))
                player.opLoc1(box)
                advance(5)
                assertTrue(player.activityBlocksTeleport) { "Round still active at the lockbox" }
                assertTrue(player.vars[bit] == 0) {
                    "Take operation, player at ${player.coords}, interaction ${player.interaction}"
                }
            }
            enter()
            assertTrue(player.activityBlocksTeleport)
            take()
            assertEquals(1, player.count(objTypes.getValue(25650)))
            player.telejump(door.coords.translate(1, 1))
            player.opLoc1(door)
            advance(3)
            assertEquals(1, player.count(objTypes.getValue(25651)))
            assertEquals(0, player.count(objTypes.getValue(25650)))
            assertFalse(player.activityBlocksTeleport)
            enter()
            take()
            advance(101)
            assertEquals(0, player.count(objTypes.getValue(25650)))
            assertFalse(player.activityBlocksTeleport)
            assertEquals(1, player.count(objTypes.getValue(25647)))
            assertEquals(1, player.count(objTypes.getValue(25651)))
            assertEquals(0, player.vars[cacheTypes.varps.getValue(2953)])
        }
}
