package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

class ColossalWyrmTest {
    @Test
    fun GameTestState
        .`Worm Tongue unlocks both branching paths and each native zipline pays its own lap`() =
        runGameTest(ColossalWyrm::class,org.rsmod.api.shops.OmniShopButtons::class) {
            player.members = true
            player.observerUUID = 1
            fun tile(x: Int, z: Int, l: Int = 0) = CoordGrid(x, z, l)
            for (l in 0..2) for (x in 1622..1658 step 4) for (z in
                2905..2936 step 4) allocZoneCollision(tile(x, z, l))
            val guide = spawnNpc(tile(1639, 2922), npcTypes.getValue(14100))
            player.teleport(tile(1641, 2922))
            player.opNpc1(guide)
            advance(3)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance(2)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(11354)])
            val obstacles =
                listOf(
                    listOf(55178, 1652, 2931, 0, 0, 2, 1653, 2931),
                    listOf(55180, 1655, 2925, 1, 22, 1, 1655, 2926),
                    listOf(55184, 1646, 2910, 1, 22, 2, 1647, 2910),
                    listOf(55186, 1631, 2910, 1, 10, 3, 1632, 2910),
                    listOf(55190, 1626, 2932, 1, 0, 0, 1627, 2932),
                    listOf(55191, 1648, 2909, 1, 0, 3, 1648, 2910),
                    listOf(55192, 1646, 2907, 2, 10, 1, 1647, 2907),
                    listOf(55194, 1633, 2908, 2, 22, 2, 1634, 2908),
                    listOf(55179, 1626, 2933, 2, 10, 3, 1625, 2933),
                )
            val locs =
                obstacles.map { v ->
                    placeMapLoc(
                        tile(v[1], v[2], v[3]),
                        locTypes.getValue(v[0]),
                        LocShape[v[4]],
                        LocAngle[v[5]],
                    )
                }
            fun cross(i: Int) {
                val v = obstacles[i]
                player.teleport(tile(v[6], v[7], v[3]))
                player.opLoc1(locs[i])
                advance(40)
            }
            val early = player.statMap.getFineXP(stats.agility)
            cross(0)
            assertEquals(early, player.statMap.getFineXP(stats.agility))
            player.setBaseLevel(stats.agility, 50)
            player.setCurrentLevel(stats.agility, 50)
            val basic = player.statMap.getFineXP(stats.agility)
            cross(0)
            cross(1)
            cross(5)
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(11293)])
            cross(2)
            cross(3)
            cross(4)
            cross(8)
            assertEquals(tile(1645, 2933), player.coords)
            assertEquals(5041, player.statMap.getFineXP(stats.agility) - basic)
            player.setBaseLevel(stats.agility, 62)
            player.setCurrentLevel(stats.agility, 62)
            val advanced = player.statMap.getFineXP(stats.agility)
            cross(0)
            cross(1)
            cross(5)
            cross(6)
            cross(7)
            cross(8)
            assertEquals(7496, player.statMap.getFineXP(stats.agility) - advanced)
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65780)])
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65781)])
            player.teleport(tile(1641, 2922))
            player.opNpc3(guide)
            advance(3)
            val list =
                cacheTypes.components.values.single { it.internalName == "omnishop_main:list" }
            val termite = objTypes.getValue(30038)
            player.inv[0] = InvObj(termite, 650)
            fun buy(item: Int) {
                player.ifButton(list, comsub = item * 12, obj = -1, op = IfButtonOp.Op2)
                advance(1)
            }
            buy(2)
            assertEquals(650, player.count(termite))
            buy(1)
            assertEquals(1, player.count(objTypes.getValue(30040)))
            assertEquals(610, player.count(termite))
        }
}
