package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.*
import org.rsmod.api.player.CostumeRoomInventory
import org.rsmod.api.player.hasStoredChompyHat
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.inv.InvScope
import org.rsmod.map.CoordGrid

class CostumeRoomTest {
    data class Deps @Inject constructor(val house: StarterHouse, val rooms: HouseRoomRepository)

    @Test
    fun GameTestState
        .`oak armour case accepts all eighteen native hats and keeps actual counts across house visits`() =
        runInjectedGameTest(Deps::class, null, StarterHouseScript::class, CostumeRoom::class) { d ->
            player.members = true
            player.setVarBit(varbits.poh_house_location, 1)
            player.setCurrentLevel(stats.construction, 46)
            val hotspot = locTypes.getValue(18815)
            placeMapLoc(CoordGrid(1906, 5711), hotspot, LocShape[10], LocAngle[1])
            val cell = HouseRoom.ENTRANCE_CELL + 1
            d.rooms.replace(
                player,
                d.rooms.layout(player) + (cell to HouseRoom(cell, HouseRoomType.CostumeRoom)),
            )
            val entry = checkNotNull(d.house.prepare(player, true))
            val tile = entry.translate(6, 5)
            player.teleport(tile.translateZ(-1))
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objTypes.values.single { it.internalName == "poh_saw" })
            repeat(3) { player.inv[2 + it] = InvObj(objs.oak_plank) }
            player.opLoc5(BoundLocInfo(checkNotNull(findLoc(tile, hotspot)), hotspot))
            advance(3)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
            advance(6)
            assertEquals(1, d.rooms.layout(player).getValue(cell).variant(6))
            assertEquals(0, player.count(objs.oak_plank))
            assertEquals(1800, player.statMap.getFineXP(stats.construction))
            val case = locTypes.getValue(18778)
            player.opLoc1(BoundLocInfo(checkNotNull(findLoc(tile, case)), case))
            advance(3)
            for (i in 0..17) player.inv[2 + i] = InvObj(objTypes.getValue(2978 + i))
            player.inv[20] = InvObj(objTypes.getValue(2978))
            player.ifButton(
                cacheTypes.components.values.single {
                    it.internalName == "poh_costumes:depositall"
                },
                op = IfButtonOp.Op1,
            )
            advance(2)
            val store = player.invMap.getValue(CostumeRoomInventory.storage)
            assertEquals(InvScope.Perm, store.type.scope)
            assertEquals(2, store.count(objTypes.getValue(2978)))
            assertTrue((0..17).all(player::hasStoredChompyHat))
            assertEquals(1, player.count(objs.hammer))
            player.setVarBit(cacheTypes.varbits.getValue(1777), 2)
            val handle = cacheTypes.components.values.single { it.internalName == "poh_costumes:items" }
            val lastHat = objTypes.getValue(2995)
            val slot = store.indexOfFirst { it?.id == lastHat.id }
            store[slot] = null
            player.ifButton(handle, comsub=15, obj=2978, op=IfButtonOp.Op1);advance(2)
            assertEquals(0, player.count(objTypes.getValue(2978)))
            store[slot] = InvObj(lastHat)
            player.ifButton(handle, comsub=15, obj=2978, op=IfButtonOp.Op1);advance(2)
            assertEquals(1, player.count(lastHat))
            assertFalse(player.hasStoredChompyHat(17))
            player.setVarBit(cacheTypes.varbits.getValue(1777), 0)
            player.ifButton(cacheTypes.components.values.single { it.internalName == "poh_costumes:depositall" },op=IfButtonOp.Op1);advance(2)
            player.ifClose()
            advance(2)
            val next = checkNotNull(d.house.prepare(player, false))
            val at = next.translate(6, 5)
            player.teleport(at.translateZ(-1))
            player.opLoc1(BoundLocInfo(checkNotNull(findLoc(at, case)), case))
            advance(3)
            player.ifButton(
                cacheTypes.components.values.single { it.internalName == "poh_costumes:items" },
                comsub = 15,
                obj = 2995,
                op = IfButtonOp.Op1,
            )
            advance(2)
            assertEquals(1, player.count(objTypes.getValue(2995)))
            assertFalse(player.hasStoredChompyHat(17))
        }
}
