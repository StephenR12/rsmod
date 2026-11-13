package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.NpcList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

class HouseActivitiesTest {
    data class Deps
    @Inject
    constructor(
        val house: StarterHouse,
        val rooms: HouseRoomRepository,
        val npcs: NpcList,
        val bits: VarBitTypeList,
        val vars: VarpTypeList,
    )

    @Test
    fun GameTestState
        .`building the oak lectern consumes its plank and leaves the tablet loc in the Study`() =
        runInjectedGameTest(Deps::class, null, StarterHouseScript::class) { deps ->
            player.members = true
            player.setVarBit(varbits.poh_house_location, 1)
            player.setCurrentLevel(stats.construction, 40)
            val hot = locTypes.values.single { it.internalName == "poh_study_1" }
            placeMapLoc(CoordGrid(1890, 5738), hot, LocShape[10], LocAngle[2])
            val cell = HouseRoom.ENTRANCE_CELL + 1
            deps.rooms.replace(
                player,
                deps.rooms.layout(player) + (cell to HouseRoom(cell, HouseRoomType.Study)),
            )
            val entry = requireNotNull(deps.house.prepare(player, true))
            val at = entry.translate(6, 0)
            player.teleport(at.translateZ(1))
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objTypes.values.single { it.internalName == "poh_saw" })
            player.inv[2] = InvObj(objs.oak_plank)
            player.opLoc5(BoundLocInfo(requireNotNull(findLoc(at, hot)), hot))
            advance(3)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
            advance(6)
            assertNotNull(
                findLoc(at, locTypes.values.single { it.internalName == "poh_lectern_1" })
            )
            assertEquals(0, player.count(objs.oak_plank))
            assertEquals(600, player.statMap.getFineXP(stats.construction))
            assertEquals(1, deps.rooms.snapshot(player).getValue(cell).variant(1))
        }

    @Test
    fun GameTestState
        .`Brimstail replaces a missing crystal seed and Askeladden's rock operation follows the trials`() =
        runInjectedGameTest(Deps::class, null, HouseQuestSupplies::class) { deps ->
            player.members = true
            val seed = objTypes.values.single { it.internalName == "crystal_seed_old_small" }
            val rock = objTypes.values.single { it.internalName == "vt_useless_rock" }
            val at = CoordGrid(2408, 9812)
            val gnome =
                spawnNpc(at, npcTypes.values.single { it.internalName == "gnome_brimstail_1op" })
            player.teleport(at.translateX(1))
            player.setVarBit(deps.bits[2497]!!, 60)
            fun recover() {
                player.opNpc1(gnome)
                advance(3)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            }
            recover()
            recover()
            assertEquals(1, player.count(seed))
            val town = CoordGrid(2659, 3662)
            val child =
                spawnNpc(
                    town,
                    npcTypes.values.single { it.internalName == "viking_askelladen_norockop" },
                )
            player.teleport(town.translateX(1))
            fun ask() {
                player.opNpc1(child)
                advance(3)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            }
            ask()
            assertEquals(0, player.count(rock))
            player.setVarp(deps.vars[347]!!, 1)
            ask()
            assertEquals(1, player.count(rock))
        }

    @Test
    fun GameTestState
        .`Renitee charges for the Saradomin crest and sells the painting used in the Quest Hall`() =
        runInjectedGameTest(Deps::class, null, Heraldry::class, StarterHouseScript::class) { deps ->
            player.members = true
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.construction, 65)
            val pos = CoordGrid(2982, 3341, 1)
            val herald =
                spawnNpc(pos, npcTypes.values.single { it.internalName == "poh_herald_of_falador" })
            player.teleport(pos.translateX(1))
            player.inv[0] = InvObj(objs.coins, 7000)
            fun talk() {
                player.opNpc1(herald)
                advance(3)
            }
            fun option(number: Int) {
                player.resumeActiveCoroutine(
                    ResumePauseButtonInput(components.chatmenu_pbutton, number)
                )
            }
            fun item(index: Int) {
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, index))
            }
            talk()
            option(1)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.DiscoverFaladorCrest))
            option(1)
            item(12)
            option(1)
            advance(2)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.AdoptSaradominCrest))
            assertEquals(2000, player.count(objs.coins))
            player.setVarp(deps.vars[402]!!, 6)
            talk()
            option(2)
            option(1)
            advance(2)
            val painting =
                objTypes.values.single { it.internalName == "poh_unframed_painting_istafar" }
            assertEquals(1, player.count(painting))
            assertEquals(0, player.count(objs.coins))
            player.setVarBit(varbits.poh_house_location, 1)
            val hot = locTypes.values.single { it.internalName == "poh_hall2_3" }
            placeMapLoc(CoordGrid(1902, 5751), hot, LocShape[5], LocAngle[1])
            val cell = HouseRoom.ENTRANCE_CELL + 1
            deps.rooms.replace(
                player,
                deps.rooms.layout(player) + (cell to HouseRoom(cell, HouseRoomType.QuestHall)),
            )
            val entry = requireNotNull(deps.house.prepare(player, true))
            val at = entry.translate(10, 5)
            player.teleport(at.translateZ(-1))
            player.inv[3] = InvObj(objs.hammer)
            player.inv[4] = InvObj(objTypes.values.single { it.internalName == "poh_saw" })
            for (slot in 5..7) player.inv[slot] = InvObj(objs.mahogany_plank)
            player.opLoc5(BoundLocInfo(requireNotNull(findLoc(at, hot)), hot))
            advance(3)
            item(4)
            advance(6)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.BuildIsafdarPainting))
            assertEquals(0, player.count(painting))
            assertEquals(4640, player.statMap.getFineXP(stats.construction))
        }

    @Test
    fun GameTestState.`focusing a portal charges actual runes and entering it awards Kharyrll`() =
        runInjectedGameTest(Deps::class, null, HouseActivities::class, StarterHouseScript::class) {
            deps ->
            player.members = true
            player.setVarBit(varbits.poh_house_location, 1)
            player.setCurrentLevel(stats.magic, 66)
            fun loc(name: String) = locTypes.values.single { it.internalName == name }
            val frame = loc("poh_teleroom_1")
            val focus = loc("poh_teleroom_7")
            placeMapLoc(CoordGrid(1864, 5731), frame, LocShape[10], LocAngle[1])
            placeMapLoc(CoordGrid(1867, 5731), focus, LocShape[10], LocAngle[1])
            val cell = HouseRoom.ENTRANCE_CELL + 1
            val room = HouseRoom(cell, HouseRoomType.PortalChamber).furnish(1, 1).furnish(7, 1)
            deps.rooms.replace(player, deps.rooms.layout(player) + (cell to room))
            val entry = requireNotNull(deps.house.prepare(player, false))
            val at = entry.translate(7, 1)
            val focusType = loc("poh_teleport_centrepiece")
            val target = BoundLocInfo(requireNotNull(findLoc(at, focusType)), focusType)
            player.teleport(at.translateZ(-1))
            player.inv[0] = InvObj(objs.blood_rune, 100)
            player.inv[1] = InvObj(objs.law_rune, 199)
            fun choose() {
                player.opLoc1(target)
                advance(3)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 4))
            }
            choose()
            assertEquals(100, player.count(objs.blood_rune))
            assertEquals(0, deps.rooms.snapshot(player).getValue(cell).portal(1))
            player.setVarBit(varbits.desert_treasure_main, 15)
            choose()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            assertEquals(100, player.count(objs.blood_rune))
            player.inv[1] = InvObj(objs.law_rune, 200)
            choose()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance(2)
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(0, player.count(objs.blood_rune))
            assertEquals(5, deps.rooms.snapshot(player).getValue(cell).portal(1))
            val portal = loc("poh_portal_teak_kharyrll")
            val gate = BoundLocInfo(requireNotNull(findLoc(entry.translate(4, 1), portal)), portal)
            player.teleport(gate.coords.translateX(1))
            player.opLoc1(gate)
            advance(5)
            assertEquals(CoordGrid(3493, 3474), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterKharyrllHousePortal))
        }

    @Test
    fun GameTestState
        .`a dropped pet rock survives rebuilding the menagerie and grants the task on pickup`() =
        runInjectedGameTest(Deps::class, null, HouseActivities::class, StarterHouseScript::class) {
            deps ->
            player.members = true
            player.setVarBit(varbits.poh_house_location, 1)
            val cell = HouseRoom.ENTRANCE_CELL + 1
            deps.rooms.replace(
                player,
                deps.rooms.layout(player) + (cell to HouseRoom(cell, HouseRoomType.Menagerie)),
            )
            val entry = requireNotNull(deps.house.prepare(player, false))
            val tile = entry.translate(8, 0)
            player.teleport(tile)
            val rock = objTypes.values.single { it.internalName == "vt_useless_rock" }
            player.inv[0] = InvObj(rock)
            player.withProtectedAccess { opHeld5(0) }
            advance(3)
            assertEquals(0, player.count(rock))
            assertNotEquals(0L, deps.rooms.snapshot(player).getValue(cell).petRocks)
            player.withProtectedAccess { deps.house.leave(this) }
            val again = requireNotNull(deps.house.prepare(player, false))
            player.teleport(again.translate(8, 1))
            val pet = deps.npcs.single { it.type.internalName == "poh_rock" }
            player.opNpc3(pet)
            advance(5)
            assertEquals(1, player.count(rock))
            assertEquals(0L, deps.rooms.snapshot(player).getValue(cell).petRocks)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.RetrieveMenageriePetRock))
        }

    @Test
    fun GameTestState
        .`mounting the KQ head consumes the stuffed trophy and talking completes the task`() =
        runInjectedGameTest(Deps::class, null, HouseActivities::class, StarterHouseScript::class) {
            deps ->
            player.members = true
            player.setVarBit(varbits.poh_house_location, 1)
            fun loc(name: String) = locTypes.values.single { it.internalName == name }
            val hot = loc("poh_hall1_2")
            placeMapLoc(CoordGrid(1870, 5751), hot, LocShape[10], LocAngle[0])
            val cell = HouseRoom.ENTRANCE_CELL + 1
            deps.rooms.replace(
                player,
                deps.rooms.layout(player) +
                    (cell to HouseRoom(cell, HouseRoomType.SkillHall).furnish(2, 3)),
            )
            val entry = requireNotNull(deps.house.prepare(player, true))
            val at = entry.translate(10, 5)
            player.teleport(at.translateZ(-1))
            val blank = loc("poh_trophy_head_blank_gilded")
            val target = BoundLocInfo(requireNotNull(findLoc(at, blank)), blank)
            val head =
                objTypes.values.single { it.internalName == "poh_pitydrop_kalphitequeen_stuffed" }
            player.inv[0] = InvObj(head)
            player.withProtectedAccess {
                check(eventBus.publish(this, LocUEvents.Op(target, target, blank, head, 0)))
            }
            assertEquals(0, player.count(head))
            assertEquals(2230, player.statMap.getFineXP(stats.construction))
            assertEquals(2000, player.statMap.getFineXP(stats.ranged))
            val mounted = loc("poh_trophy_kalphitequeen_gilded")
            player.opLoc1(BoundLocInfo(requireNotNull(findLoc(at, mounted)), mounted))
            advance(5)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SpeakToMountedKq))
        }

    @Test
    fun GameTestState
        .`the singing bowl restores a seed to a charged saw only after Eyes of Glouphrie`() =
        runInjectedGameTest(Deps::class, null, CrystalSaw::class) { deps ->
            player.members = true
            val seed = objTypes.values.single { it.internalName == "crystal_seed_old_small" }
            val saw = objTypes.values.single { it.internalName == "eyeglo_crystal_saw" }
            val bowl = locTypes.values.single { it.internalName == "eyeglo_singing_bowl" }
            val at = CoordGrid(2388, 9813)
            val target = placeMapLoc(at, bowl)
            player.teleport(at.translateX(1))
            player.inv[0] = InvObj(seed)
            player.opLoc3(target)
            advance(9)
            assertEquals(1, player.count(seed))
            player.setVarBit(deps.bits[2497]!!, 60)
            player.opLoc3(target)
            advance(9)
            assertEquals(0, player.count(seed))
            assertEquals(1, player.count(saw))
            assertEquals(28, player.vars[deps.bits[2514]!!])
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SingCrystalSaw))
        }
}
