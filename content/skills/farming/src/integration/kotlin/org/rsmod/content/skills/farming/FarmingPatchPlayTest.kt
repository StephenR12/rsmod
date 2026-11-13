package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class FarmingPatchPlayTest {
    data class Dependencies
    @Inject
    constructor(
        val repository: FarmingPatchRepository,
        val codec: FarmingPatchCodec,
        val crops: FarmingCropCatalogue,
    )

    @Test
    fun GameTestState.`rake sow grow and harvest a real herb patch`() =
        runInjectedGameTest(Dependencies::class, null, FarmingScript::class) { deps ->
            player.members = true
            player.setCurrentLevel(stats.farming, 9)
            val guam = objTypes.values.single { it.internalName == "guam_seed" }
            val base = locTypes.values.single { it.internalName == "farming_herb_patch_1" }
            val target = placeMapLoc(CoordGrid(3058, 3311), base)
            player.teleport(target.coords.translateX(-1))
            player.inv[0] = InvObj(objTypes.values.single { it.internalName == "rake" })
            repeat(3) {
                player.opLoc1(target)
                advance(3)
            }
            val state =
                requireNotNull(deps.repository.find(player, deps.codec.patchKey(target, base)))
            assertEquals(0, state.weeds)
            player.inv[5] = InvObj(guam)
            player.inv[6] = InvObj(objTypes.values.single { it.internalName == "dibber" })
            player.withProtectedAccess {
                check(
                    eventBus.publish(this, LocUDefaultEvents.OpType(target, target, base, guam, 5))
                )
            }
            assertEquals("guam", state.cropKey)
            val crop = requireNotNull(deps.crops.forSeed(guam.id))
            assertEquals(
                "herb_guam_leaf_seed",
                deps.codec.visualType(base, state, crop)?.internalName,
            )
            // Move the growth clock forward without allowing a random disease to obscure the
            // harvest.
            state.protected = true
            deps.repository.advance(
                player,
                state,
                state.nextGrowthAt + crop.growthMinutes * crop.growthCycles * 60_000L,
            )
            assertEquals("Pick", deps.codec.visualType(base, state, crop)?.op?.get(0))
            deps.codec.sync(player, base, state, crop)
            player.opLoc1(target)
            advance(3)
            assertEquals(1, player.inv.count(objTypes[requireNotNull(crop.produce)]))
        }

    @Test
    fun GameTestState.`gardener quotes the patch price and takes mixed noted payment atomically`() =
        runInjectedGameTest(Dependencies::class, null, FarmingScript::class) { deps ->
            player.members = true
            val base = locTypes.values.single { it.internalName == "farming_tree_patch_1" }
            val tree = placeMapLoc(CoordGrid(2934, 3439), base)
            val npc =
                spawnNpc(
                    tree.coords.translateX(3),
                    npcTypes.values.single { it.internalName == "farming_gardener_tree_1" },
                )
            val state =
                deps.repository.getOrCreate(
                    player,
                    deps.codec.patchKey(tree, base),
                    FarmingPatchType.Tree,
                )
            state.weeds = 0
            state.nextGrowthAt = System.currentTimeMillis() + 1_000_000L
            state.cropKey = "yew"
            val spines = objTypes[requireNotNull(deps.crops["yew"]!!.protection).item]
            val certified = objTypes.cert(spines)
            player.teleport(npc.coords.translateZ(-1))
            player.inv[0] = InvObj(spines)
            player.inv[1] = InvObj(certified, 8)
            fun pay() {
                player.opNpc3(npc)
                advance(2)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
                advance(1)
            }
            pay()
            assertEquals(false, state.protected)
            assertEquals(8, player.inv.count(certified))
            player.inv[1] = InvObj(certified, 9)
            pay()
            assertEquals(true, state.protected)
            assertEquals(0, player.inv.count(spines))
            assertEquals(0, player.inv.count(certified))
        }

    @Test
    fun GameTestState
        .`vineyard requires saltpetre and supplies blessed grapes after checking health`() =
        runInjectedGameTest(Dependencies::class, null, FarmingScript::class) { deps ->
            player.members = true
            player.setCurrentLevel(stats.farming, 36)
            fun obj(name: String) = objTypes.values.single { it.internalName == name }
            val base = locTypes.values.single { it.internalName == "grapevine_patch_clickzone_01" }
            val target =
                placeMapLoc(
                    CoordGrid(1808, 3562),
                    base,
                    org.rsmod.game.loc.LocShape[22],
                    org.rsmod.game.loc.LocAngle.East,
                )
            player.teleport(target.coords.translateX(-1))
            val seed = obj("grape_seed")
            player.inv[0] = InvObj(seed)
            player.inv[1] = InvObj(obj("dibber"))
            player.inv[2] = InvObj(obj("gardening_trowel"))
            player.inv[3] = InvObj(obj("hosidius_saltpetre"))
            fun use(slot: Int) {
                val item = objTypes[player.inv[slot]!!]
                player.withProtectedAccess {
                    eventBus.publish(
                        this,
                        LocUDefaultEvents.OpType(target, target, base, item, slot),
                    )
                }
                advance(2)
            }
            use(0)
            assertEquals(1, player.inv.count(seed))
            use(3)
            use(0)
            val state = deps.repository.find(player, deps.codec.patchKey(target, base))!!
            assertEquals("grape", state.cropKey)
            deps.repository.advance(player, state, state.nextGrowthAt + 35 * 60_000L)
            val crop = deps.crops["grape"]!!
            assertEquals(
                "grapevine_clickzone_xp",
                deps.codec.visualType(base, state, crop)?.internalName,
            )
            deps.codec.sync(player, base, state, crop)
            player.opLoc1(target)
            advance(3)
            assertEquals(true, state.healthChecked)
            player.inv[0] = InvObj(obj("grape_blessing"))
            player.opLoc1(target)
            advance(2)
            assertEquals(1, player.inv.count(obj("zamorak_grapes")))
            assertEquals(0, player.inv.count(obj("grape_blessing")))
        }

    @Test
    fun GameTestState
        .`spirit tree network includes only health-checked mature crops and forgets a cleared plot`() =
        runInjectedGameTest(Dependencies::class, null, FarmingScript::class) { deps ->
            player.members = true
            val anchors =
                listOf(
                    CoordGrid(3059, 3257),
                    CoordGrid(2612, 3857),
                    CoordGrid(2801, 3202),
                    CoordGrid(1692, 3541),
                    CoordGrid(1252, 3749),
                )
            fun destinations(): Set<CoordGrid> {
                val query = org.rsmod.api.player.events.CultivatedSpiritTrees(player)
                eventBus.publish(query)
                return query.plots
            }
            val base = locTypes.values.single { it.internalName == "farming_spirit_tree_patch_3" }
            val target = placeMapLoc(anchors[2], base)
            val state =
                deps.repository.getOrCreate(
                    player,
                    deps.codec.patchKey(target, base),
                    FarmingPatchType.SpiritTree,
                )
            state.weeds = 0
            state.cropKey = "spirit"
            state.status = FarmingCropStatus.Harvestable
            val crop = requireNotNull(deps.crops["spirit"])
            deps.codec.sync(player, base, state, crop)
            assertEquals(emptySet<CoordGrid>(), destinations())
            player.teleport(target.coords.translateX(-1))
            player.opLoc1(target)
            advance(3)
            assertEquals(setOf(anchors[2]), destinations())
            assertEquals(
                "spirit_tree_fullygrown",
                deps.codec.visualType(base, state, crop)?.internalName,
            )
            player.inv[0] = InvObj(objTypes.values.single { it.internalName == "spade" })
            player.opLoc5(target)
            advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance(2)
            assertEquals(emptySet<CoordGrid>(), destinations())
        }
}
