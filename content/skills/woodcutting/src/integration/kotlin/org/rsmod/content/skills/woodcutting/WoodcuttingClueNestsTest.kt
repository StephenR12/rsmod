package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueInventoryState
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.back
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.woodcutting.WoodcuttingClueNests.Companion.clueNestBaseChance
import org.rsmod.content.skills.woodcutting.WoodcuttingClueNests.Companion.denominator
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingObjs
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLevelReq
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLogs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class WoodcuttingClueNestsTest {
    @Test
    fun GameTestState.`cache 233 clue nests variables and every tree base chance are pinned`() =
        runWoodcuttingTest { deps ->
            val tiers =
                listOf(
                    NestDefinition(WoodcuttingObjs.clueNestBeginner, 23_127, false),
                    NestDefinition(WoodcuttingObjs.clueNestEasy, 19_712, true),
                    NestDefinition(WoodcuttingObjs.clueNestMedium, 19_714, true),
                    NestDefinition(WoodcuttingObjs.clueNestHard, 19_716, true),
                    NestDefinition(WoodcuttingObjs.clueNestElite, 19_718, true),
                )
            for (definition in tiers) {
                val type = deps.objs[definition.type]
                assertEquals(definition.id, type.id)
                assertEquals(definition.members, type.members)
                assertEquals("Open", type.iop[0])
            }
            assertEquals(28_630, WoodcuttingObjs.twitchersGloves.id)
            assertEquals(24_361, objs.scroll_box_beginner.id)
            assertEquals(24_362, objs.scroll_box_easy.id)
            assertEquals(24_363, objs.scroll_box_medium.id)
            assertEquals(24_364, objs.scroll_box_hard.id)
            assertEquals(24_365, objs.scroll_box_elite.id)
            assertEquals(24_366, objs.scroll_box_master.id)
            assertEquals(5_075, objs.empty_bird_nest.id)
            assertEquals(9_365, varbits.vesseled_clues_disabled.id)
            assertEquals(8_063, varbits.x_marks_the_spot.id)
            assertEquals(16_565, varbits.scroll_case_beginner_minor.id)
            assertEquals(16_566, varbits.scroll_case_beginner_major.id)
            assertEquals(16_595, varbits.scroll_case_mimic.id)
            assertEquals(
                listOf(ClueInventory.X_MARKS_COMPLETE),
                deps.dbRows.types.getValue(X_MARKS_DB_ROW).data[19],
            )
            assertEquals(12_863, varbits.ca_tier_status_easy.id)
            assertEquals(12_864, varbits.ca_tier_status_medium.id)
            assertEquals(12_865, varbits.ca_tier_status_hard.id)
            assertEquals(12_866, varbits.ca_tier_status_elite.id)
            assertEquals(23..23, deps.varbits[varbits.vesseled_clues_disabled].bits)
            assertEquals(0..1, deps.varbits[varbits.ca_tier_status_easy].bits)
            assertEquals(2..3, deps.varbits[varbits.ca_tier_status_medium].bits)
            assertEquals(4..5, deps.varbits[varbits.ca_tier_status_hard].bits)
            assertEquals(6..7, deps.varbits[varbits.ca_tier_status_elite].bits)

            val bases =
                listOf(
                    objs.logs to 317_647,
                    objs.achey_tree_logs to 317_647,
                    objs.charcoal to 317_647,
                    objs.oak_logs to 361_146,
                    objs.willow_logs to 289_286,
                    objs.teak_logs to 264_336,
                    objs.juniper_logs to 360_000,
                    objs.maple_logs to 221_918,
                    objs.bark to 214_367,
                    objs.mahogany_logs to 220_623,
                    objs.arctic_pine_logs to 145_758,
                    objs.yew_logs to 145_013,
                    objs.magic_logs to 72_321,
                    objs.redwood_logs to 72_321,
                )
            for ((product, expected) in bases) {
                val tree = findLocType(content.tree) { it.treeLogs == product }
                assertEquals(expected, tree.clueNestBaseChance, deps.objs[product].name)
            }
        }

    @Test
    fun GameTestState.`X Marks replaces a clue nest with a scroll box and empty nest`() =
        runWoodcuttingTest { deps ->
            val yew = findLocType(content.tree) { it.treeLogs == objs.yew_logs }
            val origin = CoordGrid(0, 50, 50, 32, 32)
            player.teleport(origin)
            player.members = false
            player.observerUUID = 17L
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)

            random.next = 0 // Beginner clue success.
            random.then = 4 // Shared scroll-box and empty-nest placement.
            var drops = emptyList<WoodcuttingClueTier>()
            player.withProtectedAccess { drops = deps.clueNests.tryDrops(this, yew) }

            assertEquals(listOf(WoodcuttingClueTier.Beginner), drops)
            val floor =
                (0 until WoodcuttingBirdNests.ADJACENT_TILE_COUNT)
                    .asSequence()
                    .map { WoodcuttingBirdNests.adjacentDropCoords(origin, it) }
                    .distinct()
                    .flatMap(deps.objRepo::findAll)
                    .toList()
            assertEquals(
                setOf(objs.scroll_box_beginner.id, objs.empty_bird_nest.id),
                floor.map { it.type }.toSet(),
            )
            assertTrue(floor.all { it.isPrivate && it.isOriginalOwner(player) })
            assertFalse(floor.any { it.type == WoodcuttingObjs.clueNestBeginner.id })
            assertMessageSent("<col=ff0000>A bird's nest falls out of the tree.</col>")

            advance(Constants.lootdrop_duration)
            val afterNestExpiry =
                (0 until WoodcuttingBirdNests.ADJACENT_TILE_COUNT)
                    .asSequence()
                    .map { WoodcuttingBirdNests.adjacentDropCoords(origin, it) }
                    .distinct()
                    .flatMap(deps.objRepo::findAll)
                    .toList()
            assertEquals(listOf(objs.scroll_box_beginner.id), afterNestExpiry.map { it.type })

            advance(ClueInventory.SCROLL_BOX_GROUND_DURATION - Constants.lootdrop_duration)
            assertTrue(
                (0 until WoodcuttingBirdNests.ADJACENT_TILE_COUNT)
                    .asSequence()
                    .map { WoodcuttingBirdNests.adjacentDropCoords(origin, it) }
                    .distinct()
                    .flatMap(deps.objRepo::findAll)
                    .none()
            )
        }

    @Test
    fun GameTestState.`scroll box cap rolls then reports exact blocked drop message`() =
        runWoodcuttingTest { deps ->
            val yew = findLocType(content.tree) { it.treeLogs == objs.yew_logs }
            player.teleport(CoordGrid(0, 50, 50, 32, 32))
            player.members = false
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            player.inv[0] = InvObj(objs.scroll_box_beginner, 2)

            lateinit var state: ClueInventoryState
            player.withProtectedAccess {
                state = deps.clueInventory.state(player, inv, bank, ClueTier.Beginner)
            }
            assertEquals(2, state.capacity)
            assertEquals(2, state.owned)
            assertFalse(state.canReceive)

            random.next = 0 // Successful roll is blocked by the cap.
            random.then = 42 // Placement must not be consumed.
            var drops = emptyList<WoodcuttingClueTier>()
            player.withProtectedAccess { drops = deps.clueNests.tryDrops(this, yew) }

            assertTrue(drops.isEmpty())
            assertEquals(42, random.next)
            assertMessageSent(
                "You have a sneaking suspicion that you would have received a beginner scroll box."
            )
            assertTrue(deps.objRepo.findAll(ZoneKey.from(player.coords)).none())

            player.setVarBit(varbits.scroll_case_beginner_minor, 1)
            player.setVarBit(varbits.scroll_case_beginner_major, 1)
            player.setVarBit(varbits.scroll_case_mimic, 1)
            player.withProtectedAccess {
                state = deps.clueInventory.state(player, inv, bank, ClueTier.Beginner)
            }
            assertEquals(ClueInventory.MAX_SCROLL_BOX_CAPACITY, state.capacity)
            assertTrue(state.canReceive)
        }

    @Test
    fun GameTestState.`clue ownership counts clues and containers but not keys or caskets`() =
        runWoodcuttingTest { deps ->
            val mediumClue = deps.objs.types.getValue(2_837)
            val mediumKey = deps.objs.types.getValue(2_838)
            val mediumCasket = deps.objs.types.getValue(2_828)
            assertEquals("Clue scroll (medium)", mediumClue.name)
            assertEquals("Key (medium)", mediumKey.name)
            assertEquals("Casket (medium)", mediumCasket.name)

            player.inv[0] = InvObj(mediumKey)
            player.inv[1] = InvObj(mediumCasket)
            lateinit var state: ClueInventoryState
            player.withProtectedAccess {
                state = deps.clueInventory.state(player, inv, bank, ClueTier.Medium)
            }
            assertEquals(0, state.owned)

            player.withProtectedAccess { bank[0] = InvObj(mediumClue) }
            player.withProtectedAccess {
                state = deps.clueInventory.state(player, inv, bank, ClueTier.Medium)
            }
            assertEquals(1, state.owned)
            assertFalse(state.canReceive)
        }

    @Test
    fun GameTestState
        .`formula uses visible level combat tier and wilderness ring in exact order`() =
        runWoodcuttingTest { deps ->
            val yew = findLocType(content.tree) { it.treeLogs == objs.yew_logs }
            player.stats[stats.woodcutting] = 60

            assertEquals(
                181,
                denominator(145_013, 60, 2, combatTierComplete = false, wildernessRing = false),
            )
            assertEquals(1_540, deps.clueNests.denominator(player, yew, WoodcuttingClueTier.Easy))

            player.setVarBit(varbits.ca_tier_status_easy, WoodcuttingClueNests.COMPLETE_STATUS)
            assertEquals(1_463, deps.clueNests.denominator(player, yew, WoodcuttingClueTier.Easy))

            player.setVarBit(varbits.inside_wilderness, 1)
            player.worn[Wearpos.Ring.slot] = InvObj(WoodcuttingObjs.ringOfWealthI3)
            assertEquals(731, deps.clueNests.denominator(player, yew, WoodcuttingClueTier.Easy))

            player.back = InvObj(objs.woodcutting_skillcape)
            assertEquals(
                731,
                deps.clueNests.denominator(player, yew, WoodcuttingClueTier.Easy),
                "Woodcutting cape must not affect clue nests.",
            )
        }

    @Test
    fun GameTestState
        .`eligible tiers roll independently while toggle membership and ownership skip`() =
        runWoodcuttingTest { deps ->
            val yew = findLocType(content.tree) { it.treeLogs == objs.yew_logs }
            val origin = CoordGrid(0, 50, 50, 32, 32)
            player.teleport(origin)
            player.members = true
            player.stats[stats.woodcutting] = 60
            player.observerUUID = 7L
            player.withProtectedAccess { bank[0] = InvObj(WoodcuttingObjs.clueNestEasy) }

            random.next = 0 // Beginner success.
            random.then = 0 // Beginner placement.
            random.then = 1 // Medium miss; easy is skipped because it is owned.
            random.then = 0 // Hard success.
            random.then = 8 // Hard placement.
            random.then = 1 // Elite miss.
            var drops = emptyList<WoodcuttingClueTier>()
            player.withProtectedAccess { drops = deps.clueNests.tryDrops(this, yew) }

            assertEquals(listOf(WoodcuttingClueTier.Beginner, WoodcuttingClueTier.Hard), drops)
            val floor =
                (0 until WoodcuttingBirdNests.ADJACENT_TILE_COUNT)
                    .asSequence()
                    .map { WoodcuttingBirdNests.adjacentDropCoords(origin, it) }
                    .distinct()
                    .flatMap(deps.objRepo::findAll)
                    .toList()
            assertEquals(
                setOf(WoodcuttingObjs.clueNestBeginner.id, WoodcuttingObjs.clueNestHard.id),
                floor.map { it.type }.toSet(),
            )
            assertTrue(floor.all { it.isPrivate && it.isOriginalOwner(player) })

            random.then = 42
            player.setVarBit(varbits.vesseled_clues_disabled, 1)
            var disabled = emptyList<WoodcuttingClueTier>()
            player.withProtectedAccess { disabled = deps.clueNests.tryDrops(this, yew) }
            assertTrue(disabled.isEmpty())
            assertEquals(42, random.next, "The Arthur opt-out must not consume clue rolls.")

            player.setVarBit(varbits.vesseled_clues_disabled, 0)
            player.members = false
            player.withProtectedAccess { bank[1] = InvObj(WoodcuttingObjs.clueNestBeginner) }
            var ineligible = emptyList<WoodcuttingClueTier>()
            player.withProtectedAccess { ineligible = deps.clueNests.tryDrops(this, yew) }
            assertTrue(ineligible.isEmpty())
            assertEquals(42, random.next, "F2P and owned tiers must be skipped before randomness.")
        }

    @Test
    fun GameTestState.`beginner clue nest is tertiary to a successful free to play log`() =
        runWoodcuttingTest { deps ->
            val type =
                findLocType(content.tree) { it.treeLevelReq == 1 && it.treeLogs == objs.logs }
            val tree = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(tree.coords.translateX(-1))
            player.members = false
            player.observerUUID = 9L
            player.righthand = InvObj(objs.bronze_axe)
            player.stats[stats.woodcutting] = type.treeLevelReq
            var cutLogs = 0
            eventBus.subscribeUnbound(Woodcutting.CutLogs::class.java) { cutLogs++ }

            player.opLoc1(tree)
            advance(3)
            random.next = 0 // Successful log roll.
            random.then = 0 // Keep the single-cut tree standing.
            random.then = 0 // Beginner clue success.
            random.then = 0 // Clue placement.
            advance()

            assertTrue(player.inv.contains(objs.logs))
            assertEquals(250, player.statMap.getFineXP(stats.woodcutting))
            assertEquals(1, cutLogs)
            assertMessageSent("You get some logs.")
            assertMessageSent("<col=ff0000>A bird's nest falls out of the tree.</col>")
            val nest =
                deps.objRepo.findAll(ZoneKey.from(player.coords)).single {
                    it.type == WoodcuttingObjs.clueNestBeginner.id
                }
            assertTrue(nest.isPrivate)
            assertFalse(player.inv.contains(WoodcuttingObjs.clueNestBeginner))
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
    constructor(
        val clueNests: WoodcuttingClueNests,
        val clueInventory: ClueInventory,
        val objRepo: ObjRepository,
        val objs: ObjTypeList,
        val varbits: VarBitTypeList,
        val dbRows: DbRowTypeList,
    )

    private data class NestDefinition(
        val type: org.rsmod.game.type.obj.ObjType,
        val id: Int,
        val members: Boolean,
    )

    private companion object {
        const val X_MARKS_DB_ROW = 162
    }
}
