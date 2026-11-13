package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.map.CoordGrid

class DorgeshKaanCourseTest {
    data class Deps @Inject constructor(val bits: VarBitTypeList)

    @Test
    fun GameTestState
        .`Turgall requires an outbound route and intact return before paying both grapple legs`() =
        runInjectedGameTest(Deps::class, null, DorgeshKaanCourse::class) { deps ->
            fun tile(x: Int, z: Int) = CoordGrid(x, z, 3)
            fun item(name: String) = objTypes.values.single { it.internalName == name }
            val engineerTile = CoordGrid(2741, 5241, 2)
            allocZoneCollision(engineerTile)
            val engineer =
                spawnNpc(
                    engineerTile,
                    npcTypes.values.single { it.internalName == "dorgesh_male_engineer" },
                )
            val pylon = locTypes.values.single { it.internalName == "dorgesh_pylon_grapple_base" }
            val anchors = listOf(tile(2710, 5242), tile(2704, 5224), tile(2703, 5212))
            val near = listOf(tile(2719, 5242), tile(2704, 5233), tile(2703, 5221))
            val far = listOf(tile(2709, 5242), tile(2704, 5223), tile(2703, 5211))
            val pylons =
                anchors.mapIndexed { i, at ->
                    placeMapLoc(at, pylon, LocShape[10], LocAngle[if (i == 0) 3 else 2])
                }
            fun cross(index: Int, outbound: Boolean) {
                player.teleport(if (outbound) near[index] else far[index])
                player.opLoc1(pylons[index])
                advance(12)
            }
            player.members = true
            player.observerUUID = 1L
            for (skill in listOf(stats.agility, stats.strength, stats.ranged)) {
                player.setBaseLevel(skill, 70)
                player.setCurrentLevel(skill, 70)
            }
            player.setVarBit(deps.bits[2258]!!, 13)
            player.inv[10] = InvObj(item("wint_torch"))
            player.teleport(engineer.coords.translateX(1))
            player.opNpc1(engineer)
            advance(8)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            val spanner = item("dorgesh_spanner")
            assertEquals(1, player.count(spanner))
            val partIndex = player.vars[deps.bits[3454]!!] - 1
            val part = item("dorgesh_powerstation_" + listOf("battery", "fuse", "meter")[partIndex])
            val consoleType =
                locTypes.values.single { it.internalName == "dorgesh_old_generator_console" }
            val console = placeMapLoc(CoordGrid(2702, 5190, 2), consoleType)
            fun useSpanner() {
                player.teleport(console.coords.translateZ(1))
                val slot = player.inv.mapSlots { _, obj -> obj?.id == spanner.id }.single()
                player.withProtectedAccess {
                    check(
                        eventBus.publish(
                            this,
                            LocUEvents.Op(console, console, consoleType, spanner, slot),
                        )
                    )
                }
            }
            useSpanner()
            assertEquals(0, player.count(part))
            cross(0, true)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.GrappleDorgeshPylon))
            player.righthand = InvObj(item("xbows_crossbow_bronze"))
            player.quiver = InvObj(objs.mith_grapple, 20)
            val startRanged = player.statMap.getFineXP(stats.ranged)
            val startStrength = player.statMap.getFineXP(stats.strength)
            for (i in near.indices) {
                cross(i, true)
                assertEquals(far[i], player.coords)
            }
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.GrappleDorgeshPylon))
            useSpanner()
            assertEquals(1, player.count(part))
            fun deliver() {
                player.teleport(engineer.coords.translateX(1))
                player.opNpc1(engineer)
                advance(4)
            }
            deliver()
            assertEquals(1, player.count(part))
            for (i in far.indices.reversed()) {
                cross(i, false)
                assertEquals(near[i], player.coords)
            }
            deliver()
            assertEquals(0, player.count(part))
            assertEquals(0, player.vars[deps.bits[3454]!!])
            assertEquals(startRanged + 12500, player.statMap.getFineXP(stats.ranged))
            assertEquals(startStrength + 1080, player.statMap.getFineXP(stats.strength))
        }
}
