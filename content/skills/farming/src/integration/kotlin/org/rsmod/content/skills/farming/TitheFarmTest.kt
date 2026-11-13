package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.map.CoordGrid

class TitheFarmTest {
    data class Deps @Inject constructor(val regions: RegionRegistry, val bits: VarBitTypeList)

    @Test
    fun GameTestState
        .`Logavano goes from table to private farm through three waterings to harvest and deposit`() =
        runInjectedGameTest(Deps::class, null, TitheFarm::class) { deps ->
            player.members = true
            fun loc(name: String) = locTypes.values.single { it.internalName == name }
            fun item(name: String) = objTypes.values.single { it.internalName == name }
            val table = placeMapLoc(CoordGrid(1802, 3503), loc("tithe_plant_seed_table"))
            val door =
                placeMapLoc(
                    CoordGrid(1805, 3501),
                    loc("hosidius_tithe_farm_door"),
                    LocShape[0],
                    LocAngle[0],
                )
            val empty = loc("hosidius_tithe_empty")
            val source = CoordGrid(1810, 3503)
            val patch = placeMapLoc(source, empty)
            val sack = loc("tithe_sack_of_fruit_empty")
            val sackSource = CoordGrid(1809, 3502)
            placeMapLoc(sackSource, sack)
            val seed = item("hosidius_tithe_seed_c")
            val fruit = item("hosidius_tithe_fruit_c")
            player.teleport(CoordGrid(1803, 3502))
            player.opLoc1(table)
            advance(5)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 2))
            assertEquals(0, player.count(seed))
            player.setCurrentLevel(stats.farming, 74)
            player.opLoc1(table)
            advance(5)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 2))
            player.resumeActiveCoroutine(ResumePCountDialogInput(2))
            assertEquals(2, player.count(seed))
            val slot = player.inv.mapSlots { _, it -> it?.id == seed.id }.single()
            player.inv[5] = InvObj(item("dibber"))
            player.inv[6] = InvObj(item("watering_can_8"))
            fun plant(target: BoundLocInfo) {
                player.withProtectedAccess {
                    check(eventBus.publish(this, LocUEvents.Op(target, target, empty, seed, slot)))
                }
            }
            plant(patch)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.PlantTitheLogavano))
            assertEquals(2, player.count(seed))
            player.teleport(CoordGrid(1804, 3501))
            player.opLoc1(door)
            advance(5)
            val region = requireNotNull(deps.regions[player.coords])
            assertEquals(region.normal[CoordGrid(1806, 3501)], player.coords)
            val at = region.normal[source]
            fun bind(name: String, tile: CoordGrid = at): BoundLocInfo {
                val type = loc(name)
                return BoundLocInfo(requireNotNull(findLoc(tile, type)) { name }, type)
            }
            player.teleport(at.translateX(-1))
            plant(bind("hosidius_tithe_empty"))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.PlantTitheLogavano))
            assertEquals(1, player.count(seed))
            assertNotNull(findLoc(source, empty))
            assertNull(findLoc(source, loc("hosidius_tithe_c_1_dry")))
            for (stage in 1..3) {
                player.opLoc2(bind("hosidius_tithe_c_${stage}_dry"))
                advance(4)
                bind("hosidius_tithe_c_${stage}_wet")
                advance(96)
            }
            player.opLoc1(bind("hosidius_tithe_c_4"))
            advance(4)
            assertEquals(1, player.count(fruit))
            assertEquals(1, player.count(item("watering_can_5")))
            assertEquals(230, player.statMap.getFineXP(stats.farming))
            val privateSack = bind("tithe_sack_of_fruit_empty", region.normal[sackSource])
            player.teleport(privateSack.coords.translateX(-1))
            player.opLoc1(privateSack)
            advance(4)
            assertEquals(0, player.count(fruit))
            assertEquals(1, player.vars[deps.bits[4900]!!])
            assertEquals(2530, player.statMap.getFineXP(stats.farming))
            player.inv[8] = InvObj(fruit, 99)
            player.opLoc1(privateSack)
            advance(4)
            assertEquals(35, player.vars[deps.bits[4893]!!])
            assertEquals(0, player.vars[deps.bits[4900]!!])
            assertEquals(347530, player.statMap.getFineXP(stats.farming))
            // An unwatered seed blights and can be cleared for replanting.
            player.teleport(at.translateX(-1))
            plant(bind("hosidius_tithe_empty"))
            advance(102)
            player.opLoc1(bind("hosidius_tithe_c_1_dead"))
            advance(4)
            bind("hosidius_tithe_empty")
            val privateDoor = bind("hosidius_tithe_farm_door", region.normal[door.coords])
            player.teleport(privateDoor.coords.translateX(1))
            player.opLoc1(privateDoor)
            advance(4)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance(4)
            assertEquals(CoordGrid(1804, 3501), player.coords)
            assertEquals(35, player.vars[deps.bits[4893]!!])
            assertEquals(0, player.count(item("hosidius_tithe_fertiliser")))
        }
}
