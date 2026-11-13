package org.rsmod.content.skills.woodcutting.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.LumbridgeSkillStepVars
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLevelReq
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLogs
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeRespawnTime
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeRespawnTimeHigh
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeStump
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class WoodcuttingScriptTest {
    @Test
    fun GameTestState.`local oak harvest records only the first ordered Lumbridge task step`() =
        runGameTest(Woodcutting::class) {
            player.members = true
            player.setVarBit(varbits.vesseled_clues_disabled, 1)
            player.righthand = InvObj(objs.rune_axe)
            player.stats[stats.woodcutting] = 99
            val type =
                findLocType(content.tree) {
                    it.treeLogs.id == objs.oak_logs.id && it.hasParam(params.despawn_time)
                }
            val tree = placeMapLoc(CoordGrid(3205, 3245), type)
            player.teleport(tree.coords.translateX(-1))
            player.opLoc1(tree)
            advance(3)
            random.next = 0
            random.then = 255
            advance()
            assertEquals(1, player.vars[LumbridgeSkillStepVars.oakCut])
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopAndBurnLumbridgeOak))
        }

    @Test
    fun GameTestState.`Draynor willow task requires a successful local willow harvest`() =
        runGameTest(Woodcutting::class) {
            player.members = true
            player.setVarBit(varbits.vesseled_clues_disabled, 1)
            player.righthand = InvObj(objs.rune_axe)
            player.stats[stats.woodcutting] = 99
            val type = findLocType(content.tree) { it.treeLogs.id == objs.willow_logs.id }
            val remote = placeMapLoc(CoordGrid(3205, 3205), type)
            player.teleport(remote.coords.translateX(-1))
            player.opLoc1(remote)
            advance(3)
            random.next = 0
            random.then = 255
            advance()
            val task = DiaryTask.LUMBRIDGE_DRAYNOR_MEDIUM_CHOP_DRAYNOR_WILLOW
            assertFalse(player.hasCompletedDiaryTask(task))

            val local = placeMapLoc(CoordGrid(3087, 3230), type)
            player.teleport(local.coords.translateX(-1))
            player.actionDelay = -1
            player.opLoc1(local)
            advance(3)
            assertFalse(player.hasCompletedDiaryTask(task))
            random.next = 0
            random.then = 255
            advance()
            assertTrue(player.hasCompletedDiaryTask(task))
        }

    @Test
    fun GameTestState.`validate axe requirement`() =
        runGameTest(Woodcutting::class) {
            val type = findLocType(content.tree) { it.treeLevelReq == 1 }
            val tree = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(tree.coords.translateX(-1))
            player.clearInv()

            player.righthand = InvObj(objs.rune_axe)
            player.stats[stats.woodcutting] = 1
            player.opLoc1(tree)
            advance(ticks = 2)
            assertMessagesSent(
                "You need an axe to chop down this tree.",
                "You do not have an axe which you have the woodcutting level to use.",
            )

            player.actionDelay = -1 // Reset action delay set by initial axe check.
            player.stats[stats.woodcutting] = 100
            player.opLoc1(tree)
            advance(ticks = 2)
            assertMessageSent("You swing your axe at the tree.")
        }

    @Test
    fun GameTestState.`validate tree requirement`() =
        runGameTest(Woodcutting::class) {
            val type = findLocType(content.tree) { it.treeLevelReq > 1 }
            val tree = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(tree.coords.translateX(-1))
            player.clearInv()

            player.righthand = InvObj(objs.bronze_axe)
            player.stats[stats.woodcutting] = type.treeLevelReq - 1
            player.opLoc1(tree)
            advance(ticks = 1)
            assertMessageSent(
                "You need a Woodcutting level of ${type.treeLevelReq} to chop down this tree."
            )

            player.stats[stats.woodcutting] = type.treeLevelReq
            player.opLoc1(tree)
            advance(ticks = 2)
            assertMessageSent("You swing your axe at the tree.")
        }

    @Test
    fun GameTestState.`cut down regular tree`() =
        runGameTest(Woodcutting::class) {
            val type = findLocType(content.tree) { it.treeLevelReq == 1 }
            val tree = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            val logs = type.treeLogs
            player.teleport(tree.coords.translateX(-1))
            player.members = true
            player.setVarBit(varbits.vesseled_clues_disabled, 1)

            player.righthand = InvObj(objs.bronze_axe)
            player.stats[stats.woodcutting] = type.treeLevelReq
            player.opLoc1(tree)
            advance(ticks = 2)
            assertMessageSent("You swing your axe at the tree.")
            assertMessageNotSent("You get some logs.")
            assertDoesNotContain(player.inv, logs)

            advance(ticks = 1)
            assertMessageNotSent("You swing your axe at the tree.")
            assertMessageNotSent("You get some logs.")
            assertDoesNotContain(player.inv, logs)

            random.next = 0 // Set random roll to guarantee log success rate.
            random.then = 0 // Set random roll to avoid tree turning into stump.
            random.then = 255 // Set bird-nest roll to miss.
            advance(ticks = 1)
            assertMessageNotSent("You swing your axe at the tree.")
            assertMessageSent("You get some logs.")
            assertContains(player.inv, logs)
            assertExists(tree)

            player.clearInv()

            advance(ticks = 1)
            assertMessageNotSent("You swing your axe at the tree.")
            assertMessageNotSent("You get some logs.")
            assertDoesNotContain(player.inv, logs)

            advance(ticks = 1)
            assertMessageNotSent("You swing your axe at the tree.")
            assertMessageNotSent("You get some logs.")
            assertDoesNotContain(player.inv, logs)

            advance(ticks = 1)
            assertMessageNotSent("You swing your axe at the tree.")
            assertMessageNotSent("You get some logs.")
            assertDoesNotContain(player.inv, logs)

            random.next = 0 // Set random roll to guarantee log success rate.
            random.then = 256 // Set random roll to guarantee tree turning into stump.
            random.then = 255 // Set bird-nest roll to miss.
            advance(ticks = 1)
            assertMessageNotSent("You swing your axe at the tree.")
            assertMessageSent("You get some logs.")
            assertContains(player.inv, logs)
            assertDoesNotExist(tree)
            assertExists(tree.coords, type.treeStump)
            advance(ticks = type.treeRespawnTimeHigh)
            assertDoesNotExist(tree.coords, type.treeStump)
            assertExists(tree)
        }

    @Test
    fun GameTestState.`cut down timed-despawn tree`() =
        runGameTest(Woodcutting::class) {
            val type = findLocType(content.tree) { it.hasParam(params.despawn_time) }
            val tree = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            val logs = type.treeLogs
            player.teleport(tree.coords.translateX(-1))
            player.members = true
            player.setVarBit(varbits.vesseled_clues_disabled, 1)

            player.righthand = InvObj(objs.bronze_axe)
            player.stats[stats.woodcutting] = type.treeLevelReq
            player.opLoc1(tree)
            advance(ticks = 3)

            random.next = 0 // Set random roll to guarantee log success rate.
            random.then = 255 // Set bird-nest roll to miss.
            advance(ticks = 1)
            assertContains(player.inv, logs)
            val controller = conRepo.findExact(tree.coords)
            assertNotNull(controller)

            // Fast-forward until despawn controller has 1 tick duration left.
            advance(ticks = controller.durationStart - 1)
            assertExists(tree)
            assertDoesNotExist(tree.coords, type.treeStump)
            assertEquals(controller, conRepo.findExact(tree.coords))

            random.next = 0 // Set random roll to guarantee log success rate.
            random.then = 255 // Set bird-nest roll to miss.
            player.actionDelay = mapClock.cycle // Set player action delay to force log cut.
            advance(ticks = 1)
            assertExists(tree.coords, type.treeStump)
            assertDoesNotExist(tree)

            advance(ticks = type.treeRespawnTime - 2)
            assertExists(tree.coords, type.treeStump)
            assertDoesNotExist(tree)

            advance(ticks = 1)
            assertExists(tree)
            assertDoesNotExist(tree.coords, type.treeStump)
        }

    @Test
    fun GameTestState.`spam click tree to cut logs`() =
        runGameTest(Woodcutting::class) {
            val type = findLocType(content.tree) { it.hasParam(params.skill_productitem) }
            val tree = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            val logs = type.treeLogs
            val logName = objTypes[type.treeLogs].name.lowercase()
            player.teleport(tree.coords.translateX(-1))
            player.clearInv()

            player.righthand = InvObj(objs.bronze_axe)
            player.stats[stats.woodcutting] = type.treeLevelReq
            player.opLoc1(tree)
            advance(ticks = 1) // Initial attempt where player action delay is set.

            player.opLoc1(tree)
            advance(ticks = 1)
            assertMessageSent("You swing your axe at the tree.")
            assertMessageNotSent("You get some $logName.")
            assertDoesNotContain(player.inv, logs)

            player.opLoc1(tree)
            advance(ticks = 1)
            assertMessageSent("You swing your axe at the tree.")
            assertMessageNotSent("You get some $logName.")
            assertDoesNotContain(player.inv, logs)

            player.opLoc1(tree)
            advance(ticks = 1)
            assertMessageSent("You swing your axe at the tree.")
            assertMessageSent("You get some $logName.")
            assertContains(player.inv, logs)
        }
}
