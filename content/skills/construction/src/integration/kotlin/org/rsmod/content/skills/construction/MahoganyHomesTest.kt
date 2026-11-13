package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.minigames.MahoganyContract
import org.rsmod.api.player.minigames.hasRossContract
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class MahoganyHomesTest {
    @Test fun GameTestState.`plank sack bank action fills from the inventory and empties into the open bank`()=
        runGameTest(ContractRewards::class){
            player.members=true
            val sack=objTypes.getValue(24882)
            player.inv[0]=InvObj(sack)
            for(slot in 1..3)player.inv[slot]=InvObj(objs.plank)
            player.inv[4]=InvObj(objs.oak_plank)
            val bank=player.invMap.getOrPut(cacheTypes.invs[org.rsmod.api.config.refs.invs.bank])
            player.withProtectedAccess{ifOpenMainModal(org.rsmod.api.config.refs.interfaces.bank_main)}
            assertTrue(player.isAccessProtected)
            fun use() = eventBus.publish(org.rsmod.api.player.events.interact.HeldBanksideEvents.Type(player,0,sack))
            assertTrue(use())
            assertEquals(0,player.count(objs.plank))
            assertEquals(0,bank.count(objTypes[objs.plank]))
            assertTrue(use())
            assertEquals(3,bank.count(objTypes[objs.plank]))
            assertEquals(1,bank.count(objTypes[objs.oak_plank]))
            assertEquals(0,player.vars[cacheTypes.varps.getValue(65688)])
        }
    @Test
    fun GameTestState.`Amy assigns a real layout and only the requesting homeowner pays once`() =
        runGameTest(MahoganyHomes::class, ContractRewards::class) {
            player.members = true
            val office = CoordGrid(2989, 3364)
            player.teleport(office.translateZ(1))
            allocZoneCollision(office)
            val amy = spawnNpc(office, npcTypes.getValue(10428))
            val xp = player.statMap.getFineXP(stats.construction)
            player.opNpc1(amy)
            advance(2)
            random.next = 6
            random.then = 0
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
            advance(2)
            assertTrue(player.hasRossContract())
            val rossTile = CoordGrid(2614, 3317)
            val ross = spawnNpc(rossTile, npcTypes.getValue(10420))
            player.teleport(rossTile.translateX(1))
            advance(2)
            player.opNpc1(ross)
            advance(2)
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65687)])
            val sack = objTypes.getValue(24882)
            player.inv[0] = InvObj(sack)
            for (slot in 1..8) player.inv[slot] = InvObj(objs.plank)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(8, player.vars[cacheTypes.varps.getValue(65688)])
            player.inv[1] = InvObj(objTypes.values.single { it.internalName == "poh_saw" })
            player.inv[2] = InvObj(objs.hammer)
            val tiles =
                listOf(
                    CoordGrid(2617, 3317),
                    CoordGrid(2611, 3318),
                    CoordGrid(2611, 3315),
                    CoordGrid(2613, 3317, 1),
                    CoordGrid(2615, 3316, 1),
                )
            for ((index, pos) in tiles.withIndex()) {
                val base = locTypes.getValue(40164 + index)
                allocZoneCollision(pos)
                val loc = placeMapLoc(pos, base)
                val approach =
                    listOf(
                        pos.translateX(-1),
                        pos.translateX(1),
                        pos.translateX(1),
                        pos.translateZ(-1),
                        pos.translateX(1),
                    )[index]
                player.teleport(approach)
                advance(2)
                val before = player.vars[cacheTypes.varps.getValue(65689)]
                if (index == 0) {
                    player.opLoc3(loc)
                    advance(6)
                    assertEquals(before, player.vars[cacheTypes.varps.getValue(65689)])
                    player.inv[3] = InvObj(objs.steel_bar)
                }
                if (index == 0 || index == 4) {
                    player.opLoc3(loc)
                    advance(6)
                    assertEquals(
                        2,
                        (player.vars[cacheTypes.varps.getValue(65689)] ushr (index * 4)) and 15,
                        "repair at $pos",
                    )
                } else {
                    player.opLoc2(loc)
                    advance(5)
                    assertEquals(
                        4,
                        (player.vars[cacheTypes.varps.getValue(65689)] ushr (index * 4)) and 15,
                    )
                    player.opLoc4(loc)
                    advance(6)
                }
            }
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65688)])
            assertEquals(xp + 4050, player.statMap.getFineXP(stats.construction))
            player.runEnergy = 1
            player.teleport(rossTile.translateX(1))
            advance(2)
            player.opNpc1(ross)
            advance(3)
            assertEquals(0, player.vars[MahoganyContract.client])
            assertEquals(2, player.vars[cacheTypes.varps.getValue(65687)])
            assertEquals(xp + 9050, player.statMap.getFineXP(stats.construction))
            assertEquals(10000, player.runEnergy)
            player.opNpc1(ross)
            advance(2)
            assertEquals(2, player.vars[cacheTypes.varps.getValue(65687)])
        }

    @Test
    fun GameTestState.`reward purchase prevents an underpayment and opens the awarded crate`() =
        runGameTest(MahoganyHomes::class, ContractRewards::class) {
            player.members = true
            val tile = CoordGrid(2989, 3364)
            val amy = spawnNpc(tile, npcTypes.getValue(10428))
            player.teleport(tile.translateZ(1))
            player.setVarBit(cacheTypes.varbits.getValue(10562), 1)
            fun buy() {
                player.opNpc5(amy)
                advance(3)
                player.ifButton(
                    cacheTypes.components.getValue((673 shl 16) or 6),
                    comsub = 5,
                    op = IfButtonOp.Op1,
                )
                advance(2)
            }
            player.setVarp(cacheTypes.varps.getValue(65687), 24)
            buy()
            assertEquals(0, player.inv.count(objTypes.getValue(24884)))
            player.setVarp(cacheTypes.varps.getValue(65687), 25)
            buy()
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65687)])
            assertEquals(1, player.inv.count(objTypes.getValue(24884)))
            player.ifClose()
            advance(2)
            random.next = 0
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(9, player.inv.count(objTypes.getValue(8791)))
        }
}
