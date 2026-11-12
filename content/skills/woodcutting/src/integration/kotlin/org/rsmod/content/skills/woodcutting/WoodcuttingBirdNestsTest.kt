package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.back
import org.rsmod.api.player.front
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.woodcutting.WoodcuttingBirdNests.Companion.adjacentDropCoords
import org.rsmod.content.skills.woodcutting.WoodcuttingBirdNests.Companion.chanceDenominator
import org.rsmod.content.skills.woodcutting.WoodcuttingBirdNests.Companion.selectNest
import org.rsmod.content.skills.woodcutting.WoodcuttingBirdNests.Companion.tableSize
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingObjs
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLevelReq
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLogs
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class WoodcuttingBirdNestsTest {
    @Test
    fun GameTestState.`successful members log roll can become a private bird nest`() =
        runWoodcuttingTest { deps ->
            val type = findLocType(content.tree) { it.treeLogs == objs.yew_logs }
            val tree = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(tree.coords.translateX(-1))
            player.members = true
            player.setVarBit(varbits.vesseled_clues_disabled, 1)
            player.observerUUID = 1L
            player.back = InvObj(objs.woodcutting_skillcape)
            player.righthand = InvObj(objs.bronze_axe)
            player.stats[stats.woodcutting] = type.treeLevelReq
            val initialXp = player.statMap.getFineXP(stats.woodcutting)
            var cutLogEvents = 0
            eventBus.subscribeUnbound(Woodcutting.CutLogs::class.java) { cutLogEvents++ }

            player.opLoc1(tree)
            advance(3)
            random.next = 0 // Successful log roll.
            random.then = 0 // Successful 1/230 cape nest roll.
            random.then = 34 // Last ring-nest slot.
            random.then = 1 // Duplicated west placement entry.
            advance()

            assertMessageSent("<col=ff0000>A bird's nest falls out of the tree.</col>")
            assertMessageNotSent("You get some yew logs.")
            assertDoesNotContain(player.inv, objs.yew_logs)
            assertEquals(initialXp, player.statMap.getFineXP(stats.woodcutting))
            assertEquals(0, cutLogEvents)
            val nest =
                deps.objRepo.findAll(ZoneKey.from(player.coords)).single {
                    it.type == WoodcuttingObjs.birdNestRing.id
                }
            assertEquals(player.coords.translateX(-1), nest.coords)
            assertTrue(nest.isPrivate)
            assertTrue(nest.isOriginalOwner(player))

            advance(98)
            assertTrue(nest.isPrivate)
            advance()
            assertTrue(nest.isPublic)
            advance(99)
            assertTrue(
                deps.objRepo.findAll(ZoneKey.from(player.coords)).any { it === nest },
                "Nest should remain for the full two-minute loot duration.",
            )
            advance()
            assertFalse(deps.objRepo.findAll(ZoneKey.from(player.coords)).any { it === nest })
        }

    @Test
    fun GameTestState.`free worlds and redwoods cannot roll regular nests`() =
        runWoodcuttingTest { deps ->
            player.members = false
            random.next = 41
            player.withProtectedAccess { assertFalse(deps.birdNests.tryDrop(this, objs.yew_logs)) }
            assertEquals(41, random.next, "Free-world rejection must not consume a nest roll.")

            player.members = true
            player.back = InvObj(objs.woodcutting_skillcape)
            player.front = InvObj(WoodcuttingObjs.strungRabbitFoot)
            player.withProtectedAccess {
                assertFalse(deps.birdNests.tryDrop(this, objs.redwood_logs))
            }
            assertEquals(41, random.next, "Redwood rejection must not consume a nest roll.")
            assertNoMessageSent()
        }

    @Test
    fun GameTestState.`nest odds table and adjacent placement match the sourced contract`() =
        runWoodcuttingTest { deps ->
            assertEquals(256, chanceDenominator(hasWoodcuttingCape = false))
            assertEquals(230, chanceDenominator(hasWoodcuttingCape = true))
            assertEquals(100, tableSize(hasStrungRabbitFoot = false))
            assertEquals(95, tableSize(hasStrungRabbitFoot = true))

            assertEquals(256, deps.birdNests.chanceDenominator(player))
            player.back = InvObj(objs.woodcutting_skillcape)
            assertEquals(230, deps.birdNests.chanceDenominator(player))
            player.back = InvObj(objs.woodcutting_skillcape_t)
            assertEquals(230, deps.birdNests.chanceDenominator(player))
            player.back = InvObj(objs.max_cape_worn)
            assertEquals(230, deps.birdNests.chanceDenominator(player))
            player.back = null
            player.inv[0] = InvObj(objs.woodcutting_skillcape)
            assertEquals(256, deps.birdNests.chanceDenominator(player))

            assertEquals(100, deps.birdNests.tableSize(player))
            player.front = InvObj(WoodcuttingObjs.strungRabbitFoot)
            assertEquals(95, deps.birdNests.tableSize(player))
            player.front = null
            player.inv[1] = InvObj(WoodcuttingObjs.strungRabbitFoot)
            assertEquals(100, deps.birdNests.tableSize(player))

            assertEquals(WoodcuttingObjs.birdNestEggRed, selectNest(0))
            assertEquals(WoodcuttingObjs.birdNestEggBlue, selectNest(1))
            assertEquals(WoodcuttingObjs.birdNestEggGreen, selectNest(2))
            assertEquals(WoodcuttingObjs.birdNestRing, selectNest(3))
            assertEquals(WoodcuttingObjs.birdNestRing, selectNest(34))
            assertEquals(WoodcuttingObjs.birdNestSeeds, selectNest(35))
            assertEquals(WoodcuttingObjs.birdNestSeeds, selectNest(94))
            assertEquals(WoodcuttingObjs.birdNestSeeds, selectNest(99))
            assertThrows(IllegalArgumentException::class.java) { selectNest(100) }

            val origin = CoordGrid(0, 50, 50, 32, 32)
            val placements = (0..8).map { adjacentDropCoords(origin, it) }
            assertTrue(
                placements.all {
                    it.level == origin.level &&
                        kotlin.math.max(
                            kotlin.math.abs(it.x - origin.x),
                            kotlin.math.abs(it.z - origin.z),
                        ) == 1
                }
            )
            assertEquals(2, placements.count { it == origin.translateX(-1) })
            assertThrows(IllegalArgumentException::class.java) { adjacentDropCoords(origin, 9) }
        }

    private fun GameTestState.runWoodcuttingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            Woodcutting::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(val birdNests: WoodcuttingBirdNests, val objRepo: ObjRepository)
}
