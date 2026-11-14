package org.rsmod.content.skills.woodcutting

import it.unimi.dsi.fastutil.shorts.ShortArrayList
import jakarta.inject.Inject
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrs2.buffer.use
import org.openrs2.cache.Cache
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.cache.map.MapArchive
import org.rsmod.api.cache.map.MapFile
import org.rsmod.api.cache.map.GameMapDecoder
import org.rsmod.api.cache.map.area.MapAreaDecoder
import org.rsmod.api.cache.util.toInlineBuf
import org.rsmod.api.config.refs.BaseStats
import org.rsmod.api.config.refs.areas
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.api.type.builders.map.area.MapAreaCollector
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.game.area.AreaIndex
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionLocOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey

class WoodcuttingLevelBoostsTest {
    @OptIn(InternalApi::class)
    @Test
    fun GameTestState.`dated guild polygon supplies the invisible seven level boost`() =
        runBoostTest { deps ->
            WoodcuttingMapAreas.cleanup()
            try {
                val generatedIndex = AreaIndex()
                MapAreaCollector().loadAndCollect(listOf(WoodcuttingMapAreas)).forEach {
                    (square, definition) ->
                    GameMapDecoder.putAreas(generatedIndex, square, definition)
                }
                // Game tests bind an empty AreaIndex, so decode the production cache groups here.
                val cachedIndex = AreaIndex()
                Cache.open(Path.of(".data/cache/game")).use { cache ->
                    listOf(MapSquareKey(24, 54), MapSquareKey(25, 54)).forEach { square ->
                        val group = "a${square.x}_${square.z}"
                        val definition =
                            MapArchive(cache).read(square, MapFile.Areas)?.use {
                                MapAreaDecoder.decode(it.toInlineBuf())
                            } ?: error("Missing cached area map $group")
                        GameMapDecoder.putAreas(cachedIndex, square, definition)
                    }
                }
                val areaChecker = AreaChecker(cachedIndex, deps.regionRegistry)
                val invisibleLevels = deps.invisibleLevels(areaChecker)
                val inside =
                    listOf(
                        CoordGrid(1591, 3480, 0),
                        CoordGrid(1578, 3488, 0),
                        CoordGrid(1647, 3508, 0),
                        CoordGrid(1625, 3518, 2),
                    )
                for (coords in inside) {
                    player.placeAt(coords)
                    assertEquals(
                        true,
                        generatedIndex.containsWoodcuttingGuild(coords),
                        "expected generated Woodcutting Guild area at $coords",
                    )
                    assertEquals(
                        true,
                        cachedIndex.containsWoodcuttingGuild(coords),
                        "expected cached Woodcutting Guild area at $coords",
                    )
                    assertEquals(
                        true,
                        areaChecker.inArea(areas.woodcutting_guild, coords),
                        "expected Woodcutting Guild area at $coords",
                    )
                    assertEquals(
                        7,
                        invisibleLevels.get(player, stats.woodcutting),
                        "expected +7 Woodcutting at $coords",
                    )
                }

                val outside =
                    listOf(
                        CoordGrid(1662, 3505, 0),
                        CoordGrid(1570, 3464, 0),
                        CoordGrid(1603, 3508, 0),
                        CoordGrid(1635, 3518, 0),
                        CoordGrid(1590, 3471, 0),
                        CoordGrid(1591, 9888, 0),
                    )
                for (coords in outside) {
                    player.placeAt(coords)
                    assertEquals(
                        false,
                        generatedIndex.containsWoodcuttingGuild(coords),
                        "expected generated area outside Woodcutting Guild at $coords",
                    )
                    assertEquals(
                        false,
                        areaChecker.inArea(areas.woodcutting_guild, coords),
                        "expected outside Woodcutting Guild at $coords",
                    )
                    assertEquals(
                        0,
                        invisibleLevels.get(player, stats.woodcutting),
                        "expected no Woodcutting boost at $coords",
                    )
                }
            } finally {
                WoodcuttingMapAreas.cleanup()
            }
        }

    @OptIn(InternalApi::class)
    @Test
    fun GameTestState.`active static tree groups supply one to ten levels outside the guild`() =
        runBoostTest { deps ->
            val invisibleLevels =
                deps.invisibleLevels(AreaChecker(AreaIndex(), deps.regionRegistry))
            val origin = player.coords
            val oak =
                placeMapLoc(
                    origin.translateX(1),
                    deps.locTypes.getValue(WoodcuttingLevelBoostTestLocs.oak.id),
                )
            val otherOak =
                placeMapLoc(
                    origin.translateZ(1),
                    deps.locTypes.getValue(WoodcuttingLevelBoostTestLocs.oak.id),
                )
            val subject =
                registerPlayer(origin).apply {
                    members = false
                    setCurrentLevel(BaseStats.hitpoints, 10)
                }
            advance()
            subject.chop(oak)

            assertEquals(
                true,
                deps.locTypes[oak].hasParam(params.despawn_time),
                "eligible oak should carry a Forestry despawn timer",
            )
            assertEquals(true, subject.isValidTarget(), "subject should be a valid target")
            assertEquals(
                true,
                deps.boundValidator.touches(subject.avatar, oak),
                "subject should touch the oak",
            )
            assertEquals(
                true,
                deps.playerRepo.findAll(ZoneKey.from(oak.coords), zoneRadius = 1).any {
                    it === subject
                },
                "subject should be registered near the oak",
            )
            assertEquals(1, invisibleLevels.get(subject, stats.woodcutting))

            val stopped = registerPlayer(origin)
            val wrongTree = registerPlayer(origin)
            val travelling = registerPlayer(origin.translate(4, 4))
            val wrongOp = registerPlayer(origin)
            val dead = registerPlayer(origin)
            val loggingOut = registerPlayer(origin)
            val hidden = registerPlayer(origin)
            listOf(stopped, wrongTree, travelling, wrongOp, loggingOut, hidden).forEach {
                it.setCurrentLevel(BaseStats.hitpoints, 10)
            }
            advance()
            subject.chop(oak)
            wrongTree.chop(otherOak)
            travelling.chop(oak)
            wrongOp.chop(oak, InteractionOp.Op1)
            dead.apply {
                setCurrentLevel(BaseStats.hitpoints, 0)
                chop(oak)
            }
            loggingOut.apply {
                pendingLogout = true
                chop(oak)
            }
            hidden.apply {
                this.hidden = true
                chop(oak)
            }
            assertEquals(
                1,
                invisibleLevels.get(subject, stats.woodcutting),
                "only active players touching the same tree should count",
            )

            val activeGroup =
                List(12) {
                    registerPlayer(origin).apply { setCurrentLevel(BaseStats.hitpoints, 10) }
                }
            advance()
            subject.chop(oak)
            activeGroup.forEach { it.chop(oak) }
            assertEquals(
                10,
                invisibleLevels.get(subject, stats.woodcutting),
                "the group boost should cap at ten",
            )

            val normalTree =
                placeMapLoc(
                    origin.translateX(-1),
                    deps.locTypes.getValue(WoodcuttingLevelBoostTestLocs.normal.id),
                )
            subject.chop(normalTree)
            activeGroup.first().chop(normalTree)
            assertEquals(
                0,
                invisibleLevels.get(subject, stats.woodcutting),
                "normal trees should not receive a group boost",
            )
        }

    private fun GameTestState.runBoostTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            Woodcutting::class,
            testBody = body,
        )

    class Dependencies
    @Inject
    constructor(
        val regionRegistry: RegionRegistry,
        val locTypes: LocTypeList,
        val playerRepo: PlayerRepository,
        val boundValidator: BoundValidator,
    ) {
        fun invisibleLevels(areaChecker: AreaChecker): InvisibleLevels =
            InvisibleLevels(
                setOf(WoodcuttingLevelBoosts(areaChecker, locTypes, playerRepo, boundValidator))
            )
    }

    private fun Player.chop(tree: BoundLocInfo, op: InteractionOp = InteractionOp.Op3) {
        interaction =
            InteractionLocOp(op = op, target = tree, hasOpTrigger = true, hasApTrigger = false)
    }

    private fun AreaIndex.containsWoodcuttingGuild(coords: CoordGrid): Boolean {
        val matches = ShortArrayList()
        putAreas(coords, matches)
        return matches.contains(areas.woodcutting_guild.id.toShort())
    }
}

object WoodcuttingLevelBoostTestLocs : LocReferences() {
    val oak = find("oaktree")
    val normal = find("tree")
}
