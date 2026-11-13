package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ui.ifOpenSide
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class StarterHouseTest {
    @Test
    fun GameTestState.`the garden statue can be installed and reclaimed from its native hotspot`()=
        runInjectedGameTest(StarterHouse::class,null,StarterHouseScript::class){house->
            player.members=true;player.inv[0]=InvObj(objs.coins,1000)
            player.withProtectedAccess{house.purchase(this)}
            val spot=locTypes.getValue(15364);val statue=locTypes.getValue(57014)
            placeMapLoc(CoordGrid(1862,5704),spot)
            val portal=placeMapLoc(CoordGrid(2953,3224),locTypes.values.single{it.internalName=="poh_rimmington_portal"})
            player.teleport(portal.coords.translateX(1));player.opLoc3(portal);advance(3)
            val target=player.coords.translate(2,-2)
            val anchor=BoundLocInfo(requireNotNull(findLoc(target,spot)),spot)
            player.teleport(target.translateX(-1))
            player.inv[0]=InvObj(objs.hammer);player.inv[1]=InvObj(objTypes.values.single{it.internalName=="poh_saw"});player.inv[2]=InvObj(objTypes.getValue(31027))
            player.opLoc5(anchor);advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,3));advance(7)
            assertNotNull(findLoc(target,statue));assertEquals(0,player.inv.count(objTypes.getValue(31027)))
            player.opLoc5(BoundLocInfo(requireNotNull(findLoc(target,statue)),statue));advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton,1));advance(3)
            assertNotNull(findLoc(target,spot));assertEquals(1,player.inv.count(objTypes.getValue(31027)))
        }

    @Test
    fun GameTestState.`purchase enter build leave and re-enter an owned house`() =
        runInjectedGameTest(StarterHouse::class, null, StarterHouseScript::class) { house ->
            player.members = true
            player.inv[0] = InvObj(objs.coins, 999)
            player.withProtectedAccess { house.purchase(this) }
            assertEquals(0, player.vars[varbits.poh_house_location])
            player.inv[0] = InvObj(objs.coins, 1000)
            player.withProtectedAccess { house.purchase(this) }
            assertEquals(1, player.vars[varbits.poh_house_location])
            assertEquals(0, player.inv.count(objTypes[objs.coins]))
            fun loc(name: String) = locTypes.values.single { it.internalName == name }
            val chairSpace = loc("poh_parlour_1")
            val chair = loc("poh_chair1")
            val indoorPortal = loc("poh_exit_portal")
            // Seed the same template placements the normal map loader reads from l29_89.
            placeMapLoc(CoordGrid(1858, 5756), chairSpace)
            placeMapLoc(CoordGrid(1859, 5707), loc("poh_crude_garden_1"))
            val outdoorPos = CoordGrid(2953, 3224)
            val outdoor = placeMapLoc(outdoorPos, loc("poh_rimmington_portal"))
            player.teleport(outdoorPos.translateX(1))
            player.opLoc3(outdoor)
            advance(3)
            assertEquals(1, player.vars[varbits.poh_building_mode])
            val entry = player.coords
            val chairPos = entry.translate(-2, 10)
            val hotspot = requireNotNull(findLoc(chairPos, chairSpace))
            player.teleport(hotspot.coords.translateX(1))
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objTypes.values.single { it.internalName == "poh_saw" })
            player.inv[2] = InvObj(objs.plank)
            player.inv[3] = InvObj(objs.plank)
            player.inv[4] = InvObj(objs.bronze_nails, 2)
            val xp = player.statMap.getFineXP(stats.construction)
            player.opLoc5(BoundLocInfo(hotspot, chairSpace))
            advance(2)
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.menu_list, subcomponent = 0)
            )
            advance(5)
            assertNotNull(findLoc(chairPos, chair))
            assertEquals(580, player.statMap.getFineXP(stats.construction) - xp)
            val exit = requireNotNull(findLoc(entry, indoorPortal))
            player.teleport(exit.coords.translateZ(-1))
            player.opLoc1(BoundLocInfo(exit, indoorPortal))
            advance(3)
            assertEquals(0, player.vars[varbits.poh_building_mode])
            assertEquals(CoordGrid(2954, 3224), player.coords)
            player.opLoc2(outdoor)
            advance(3)
            assertNotNull(findLoc(player.coords.translate(-2, 10), chair))
        }

    data class LayoutDependencies
    @Inject
    constructor(
        val house: StarterHouse,
        val rooms: HouseRoomRepository,
        val buttons: org.rsmod.game.type.comp.ComponentTypeList,
    )

    @Test
    fun GameTestState
        .`purchase a kitchen through its mapped doorway and use the furnished larder`() =
        runInjectedGameTest(
            LayoutDependencies::class,
            null,
            StarterHouseScript::class,
            HouseFixtures::class,
        ) { deps ->
            player.members = true
            player.setBaseLevel(stats.construction, 10)
            player.setCurrentLevel(stats.construction, 10)
            fun loc(name: String) = locTypes.values.single { it.internalName == name }
            fun obj(name: String) = objTypes.values.single { it.internalName == name }
            fun menu(index: Int) {
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, index))
            }
            val east = loc("poh_hotspot_doorl_rimmington")
            val cupboard = loc("poh_kitchen_5")
            placeMapLoc(CoordGrid(1863, 5707), east, LocShape.WallStraight, LocAngle.East)
            placeMapLoc(
                CoordGrid(1878, 5752),
                cupboard,
                LocShape.CentrepieceStraight,
                LocAngle.East,
            )
            val outside = placeMapLoc(CoordGrid(2953, 3224), loc("poh_rimmington_portal"))
            player.inv[0] = InvObj(objs.coins, 6000)
            player.withProtectedAccess { deps.house.purchase(this) }
            player.teleport(outside.coords.translateX(1))
            player.opLoc3(outside)
            advance(3)
            val door = requireNotNull(findLoc(player.coords.translate(3, 1), east))
            player.teleport(door.coords.translateX(-1))
            player.opLoc5(BoundLocInfo(door, east))
            advance(3)
            menu(2)
            menu(0)
            advance(3)
            assertEquals(0, player.inv.count(objTypes[objs.coins]))
            val cell = HouseRoom.ENTRANCE_CELL + 1
            assertEquals(HouseRoomType.Kitchen, deps.rooms.snapshot(player)[cell]?.type)
            val location = player.coords.translate(10, -2)
            val space = requireNotNull(findLoc(location, cupboard))
            player.teleport(location.translateZ(1))
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(obj("poh_saw"))
            player.inv[2] = InvObj(objs.steel_nails, 8)
            for (slot in 3..10) player.inv[slot] = InvObj(objs.plank)
            player.opLoc5(BoundLocInfo(space, cupboard))
            advance(3)
            menu(0)
            advance(5)
            val larder = loc("poh_larder_1")
            val built = requireNotNull(findLoc(location, larder))
            player.opLoc1(BoundLocInfo(built, larder))
            advance(2)
            menu(1)
            assertEquals(1, player.inv.count(obj("bucket_milk")))
            assertEquals(1, deps.rooms.snapshot(player)[cell]!!.variant(5))
            advance(2)
            player.ifClose()
            player.ifOpenSide(interfaces.poh_options, eventBus)
            player.ifButton(deps.buttons.values.single { it.internalName == "poh_options:viewer" })
            advance(1)
            menu(0) // Kitchen (z=2), then parlour (z=3).
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 2))
            advance(1)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance(2)
            assertNull(deps.rooms.snapshot(player)[cell])
            assertEquals(2, deps.rooms.snapshot(player).size)
        }

    @Test
    fun GameTestState.`plant each fairy ring with cache materials and resolve DIQ from outside`() =
        runInjectedGameTest(LayoutDependencies::class, null, StarterHouseScript::class) { deps ->
            player.members = true
            player.setBaseLevel(stats.construction, 99)
            player.setCurrentLevel(stats.construction, 99)
            player.setCurrentLevel(stats.farming, 83)
            player.inv[0] = InvObj(objs.coins, 1000)
            player.withProtectedAccess { deps.house.purchase(this) }
            fun loc(name: String) = locTypes.values.single { it.internalName == name }
            fun obj(name: String) = objTypes.values.single { it.internalName == name }
            fun diq(): CoordGrid? {
                val event = org.rsmod.api.player.events.HouseFairyRingDestination(player)
                eventBus.publish(event)
                return event.destination
            }
            assertNull(diq())
            val hotspot = loc("poh_superior_garden_hotspot_treering")
            placeMapLoc(CoordGrid(1899, 5699), hotspot, LocShape.CentrepieceStraight, LocAngle.East)
            val layout = deps.rooms.layout(player)
            val cell = HouseRoom.ENTRANCE_CELL + 1
            for (variant in 2..3) {
                deps.rooms.replace(
                    player,
                    layout +
                        (cell to HouseRoom(cell, HouseRoomType.SuperiorGarden, rotation = variant)),
                )
                val entrance = requireNotNull(deps.house.prepare(player, true))
                player.teleport(entrance)
                advance(2)
                val space = requireNotNull(findLoc(entrance.translate(7, 1), hotspot))
                player.teleport(space.coords.translateX(-1))
                for (i in 0..9) player.inv[i] = InvObj(obj("bittercap_mushroom"))
                player.inv[10] = InvObj(obj("poh_fairy_enchantment"))
                player.inv[11] = InvObj(obj("watering_can_1"))
                if (variant == 3) player.inv[12] = InvObj(obj("plantpot_spirit_tree_sapling"))
                val construction = player.statMap.getFineXP(stats.construction)
                val farming = player.statMap.getFineXP(stats.farming)
                player.opLoc5(BoundLocInfo(space, hotspot))
                advance(3)
                player.resumeActiveCoroutine(
                    ResumePauseButtonInput(components.menu_list, variant - 1)
                )
                advance(8)
                assertEquals(variant, deps.rooms.snapshot(player)[cell]?.variant(1))
                assertEquals(
                    if (variant == 2) 5350 else 8850,
                    player.statMap.getFineXP(stats.construction) - construction,
                )
                assertEquals(
                    if (variant == 2) 0 else 8850,
                    player.statMap.getFineXP(stats.farming) - farming,
                )
                assertEquals(1, player.inv.count(obj("watering_can_0")))
                player.withProtectedAccess { deps.house.leave(this) }
                val destination = requireNotNull(diq())
                val trees =
                    org.rsmod.api.player.events.CultivatedSpiritTrees(player, reserveHouse = true)
                eventBus.publish(trees)
                assertEquals(variant == 3, trees.hasHouseTree)
                assertEquals(
                    if (variant == 3) destination.translateZ(-1) else null,
                    trees.houseDestination,
                )
                val ring = loc(if (variant == 2) "poh_fairy_ring" else "poh_spirit_ring")
                assertNotNull(findLoc(destination, ring))
                player.teleport(destination)
                advance(2)
                player.withProtectedAccess { deps.house.leave(this) }
                for (i in 0 until player.inv.size) player.inv[i] = null
            }
        }

    @Test
    fun GameTestState
        .`pay the Seers agent for Fancy Stone and enter the corresponding cache template`() =
        runInjectedGameTest(LayoutDependencies::class, null, StarterHouseScript::class) { deps ->
            player.members = true
            player.setBaseLevel(stats.construction, 50)
            player.setCurrentLevel(stats.construction, 50)
            player.inv[0] = InvObj(objs.coins, 26000)
            player.withProtectedAccess { deps.house.purchase(this) }
            val tile = CoordGrid(2737, 3502)
            val agent =
                spawnNpc(tile, npcTypes.values.single { it.internalName == "poh_estate_agent" })
            player.teleport(tile.translateX(1))
            player.opNpc1(agent)
            advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 2))
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 5))
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance(2)
            val task = org.rsmod.api.player.diary.DiaryTask.DecorateSeersFancyStone
            assertEquals(
                true,
                task.region.tasks(task.difficulty)[task.number - 1].isComplete(player),
            )
            assertEquals(0, player.inv.count(objTypes[objs.coins]))
            val door = locTypes.values.single { it.internalName == "poh_hotspot_doorl_yanille" }
            placeMapLoc(CoordGrid(1927, 5707, 1), door, LocShape.WallStraight, LocAngle.East)
            val entry = requireNotNull(deps.house.prepare(player, true))
            assertNotNull(findLoc(entry.translate(3, 1), door))
        }
}
