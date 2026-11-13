package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

class DiaryEntranceTest {
    data class Vars @Inject constructor(val vars: VarpTypeList, val bits: VarBitTypeList)

    @Test
    fun GameTestState
        .`regional shortcuts and Yanille lock consume the native actions and check direction`() =
        runInjectedGameTest(Vars::class, null, RegionalDungeonEntrances::class) { defs ->
            player.members = true
            fun objectAt(name: String, x: Int, z: Int, shape: Int, angle: Int) =
                placeMapLoc(
                    CoordGrid(x, z),
                    locTypes.values.single { it.internalName == name },
                    LocShape[shape],
                    LocAngle[angle],
                )
            val fence = objectAt("lumbridge_sc_fencejump", 3240, 3335, 0, 3)
            player.teleport(CoordGrid(3240, 3334))
            player.setCurrentLevel(stats.agility, 12)
            player.opLoc1(fence)
            advance(6)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.JumpVarrockSouthFence))
            player.setCurrentLevel(stats.agility, 46)
            player.opLoc1(fence)
            advance(6)
            assertEquals(CoordGrid(3240, 3335), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.JumpVarrockSouthFence))
            val crevice = objectAt("dwarf_mines_sc_wall_crack", 3029, 9806, 0, 0)
            player.teleport(CoordGrid(3028, 9806))
            player.opLoc1(crevice)
            advance(8)
            assertEquals(CoordGrid(3035, 9806), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SqueezeDwarvenMineCrevice))
            val wall = objectAt("fairy_sc_juttingwall", 2400, 4403, 10, 0)
            player.setVarp(defs.vars[147]!!, 6)
            player.teleport(CoordGrid(2400, 4402))
            player.opLoc1(wall)
            advance(9)
            assertEquals(CoordGrid(2400, 4404), player.coords)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.SqueezeCosmicWall))
            player.opLoc1(wall)
            advance(9)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SqueezeCosmicWall))
            val gate = objectAt("wcguild_gatel", 1657, 3505, 0, 2)
            player.teleport(CoordGrid(1658, 3505))
            player.setCurrentLevel(stats.woodcutting, 59)
            player.opLoc1(gate)
            advance(6)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EnterWoodcuttingGuild))
            player.setCurrentLevel(stats.woodcutting, 60)
            player.opLoc1(gate)
            advance(6)
            assertEquals(CoordGrid(1657, 3505), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterWoodcuttingGuild))
            val cellar = objectAt("stairs_cellar", 2603, 3078, 10, 1)
            objectAt("stairs_from_cellar", 2603, 9478, 10, 1)
            val door = objectAt("toollock5", 2601, 9482, 0, 3)
            player.teleport(CoordGrid(2602, 3079))
            player.opLoc1(cellar)
            advance(9)
            assertEquals(CoordGrid(2602, 9479), player.coords)
            player.setCurrentLevel(stats.thieving, 82)
            player.inv[5] = InvObj(objTypes.values.single { it.internalName == "lockpick" })
            player.teleport(CoordGrid(2601, 9481))
            random.next = 7
            player.opLoc2(door)
            advance(6)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.PickYanilleDungeonDoor))
            random.next = 0
            player.opLoc2(door)
            advance(8)
            assertEquals(CoordGrid(2601, 9482), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.PickYanilleDungeonDoor))
        }

    @Test
    fun GameTestState
        .`the monkey minder admits only a Karamjan monkey and Bobawu laps unlock a backpack`() =
        runInjectedGameTest(Vars::class, null, ApeAtollCourse::class, MonkeyRewards::class) { defs
            ->
            fun obj(name: String) = objTypes.values.single { it.internalName == name }
            player.members = true
            val outside = CoordGrid(2603, 3275)
            val cage = CoordGrid(2604, 3276)
            val minder =
                spawnNpc(outside, npcTypes.values.single { it.internalName == "mm_monkey_minder" })
            player.teleport(outside.translateZ(-1))
            player.setVarp(defs.vars[365]!!, 4)
            val ninja = obj("mm_monkey_greegree_for_small_ninja_monkey")
            val karamja = obj("mm_monkey_greegree_for_normal_monkey")
            player.inv[0] = InvObj(ninja)
            player.inv[1] = InvObj(karamja)
            player.withProtectedAccess { opHeld2(0) }
            advance(3)
            player.opNpc1(minder)
            advance(7)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EnterArdougneMonkeyCage))
            player.withProtectedAccess { opHeld2(1) }
            advance(3)
            player.opNpc1(minder)
            advance(7)
            assertEquals(cage, player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterArdougneMonkeyCage))
            player.withProtectedAccess { wornUnequip(3) }
            advance(3)
            player.opNpc1(minder)
            advance(6)
            assertEquals(outside, player.coords)
            val crate =
                placeMapLoc(
                    CoordGrid(2030, 5609),
                    locTypes.values.single { it.internalName == "mm2_crashsite_monkeycrate" },
                )
            player.setVarBit(defs.bits[5027]!!, 195)
            player.teleport(crate.coords.translateX(-1))
            player.opLoc1(crate)
            advance(4)
            val plain = obj("mm2_monkey")
            assertEquals(1, player.count(plain))
            val slot = player.inv.mapSlots { _, it -> it?.id == plain.id }.single()
            fun transform() {
                player.clearPendingAction()
                player.withProtectedAccess { opHeld3(slot) }
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 1))
            }
            transform()
            assertEquals(1, player.count(plain))
            player.setVarp(defs.vars[65636]!!, 100)
            transform()
            assertEquals(1, player.count(obj("mm2_monkey_karamja")))
        }
}
