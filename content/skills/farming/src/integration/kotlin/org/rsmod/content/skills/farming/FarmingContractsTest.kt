package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

class FarmingContractsTest {
    data class Deps
    @Inject
    constructor(
        val patches: FarmingPatchRepository,
        val codec: FarmingPatchCodec,
        val vars: VarpTypeList,
        val bits: VarBitTypeList,
    )

    @Test
    fun GameTestState
        .`Jane rewards a completed Guild flower contract with a pack that can be emptied`() =
        runInjectedGameTest(
            Deps::class,
            null,
            FarmingContracts::class,
            FarmingScript::class,
            SeedPacks::class,
        ) { deps ->
            val at = CoordGrid(1249, 3727)
            val jane =
                spawnNpc(at, npcTypes.values.single { it.internalName == "farming_guild_master" })
            player.teleport(at.translateX(-1))
            player.members = true
            player.setCurrentLevel(stats.farming, 45)
            fun ask() {
                player.opNpc1(jane)
                advance(4)
            }
            ask()
            random.next = 1
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
            assertEquals(2, player.vars[deps.vars[65639]!!])
            val base = locTypes.values.single { it.internalName == "farming_flower_patch_6" }
            val patch = placeMapLoc(CoordGrid(1260, 3725), base)
            val crop =
                deps.patches.getOrCreate(
                    player,
                    deps.codec.patchKey(patch, base),
                    FarmingPatchType.Flower,
                )
            crop.weeds = 0
            crop.cropKey = "marigold"
            crop.status = FarmingCropStatus.Harvestable
            crop.stage = 4
            crop.harvestLives = 1
            player.teleport(patch.coords.translateX(-1))
            player.opLoc1(patch)
            advance(5)
            assertEquals(-2, player.vars[deps.vars[65639]!!])
            player.teleport(jane.coords.translateX(-1))
            ask()
            val pack = objTypes.values.single { it.internalName == "seedbox" }
            assertEquals(1, player.count(pack))
            assertEquals(1, player.vars[deps.vars[65641]!!])
            val slot = player.inv.mapSlots { _, it -> it?.id == pack.id }.single()
            val before = player.inv.objs.filterNotNull().sumOf { it.count }
            player.withProtectedAccess { opHeld1(slot) }
            val remainder = player.inv.getValue(slot).vars
            assertEquals(
                5,
                ((remainder ushr 3) and 15) +
                    ((remainder ushr 7) and 15) +
                    ((remainder ushr 11) and 15),
            )
            player.withProtectedAccess { opHeld3(slot) }
            assertEquals(0, player.count(pack))
            assertTrue(player.inv.objs.filterNotNull().sumOf { it.count } > before)
        }

    @Test
    fun GameTestState
        .`barbarian planting advances only on success and taught pot smashing excludes spirit trees`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class) { deps ->
            fun obj(name: String) = objTypes.values.single { it.internalName == name }
            player.members = true
            player.setCurrentLevel(stats.farming, 85)
            val base = locTypes.values.single { it.internalName == "farming_herb_patch_1" }
            val patch = placeMapLoc(CoordGrid(3058, 3311), base)
            val state =
                deps.patches.getOrCreate(
                    player,
                    deps.codec.patchKey(patch, base),
                    FarmingPatchType.Herb,
                )
            state.weeds = 0
            player.teleport(patch.coords.translateX(-1))
            val guam = obj("guam_seed")
            player.inv[2] = InvObj(guam, 2)
            fun sow() {
                player.withProtectedAccess {
                    check(
                        eventBus.publish(
                            this,
                            LocUDefaultEvents.OpType(patch, patch, base, guam, 2),
                        )
                    )
                }
            }
            sow()
            assertEquals(2, player.count(guam))
            player.setVarBit(deps.bits[9609]!!, 1)
            random.next = 7
            sow()
            assertEquals(1, player.count(guam))
            assertNull(state.cropKey)
            random.next = 0
            sow()
            assertEquals("guam", state.cropKey)
            assertEquals(2, player.vars[deps.bits[9609]!!])
            val tree = locTypes.values.single { it.internalName == "farming_tree_patch_1" }
            val plot = placeMapLoc(CoordGrid(2934, 3439), tree)
            deps.patches
                .getOrCreate(player, deps.codec.patchKey(plot, tree), FarmingPatchType.Tree)
                .weeds = 0
            player.teleport(plot.coords.translateX(-1))
            val oak = obj("plantpot_oak_sapling")
            player.inv[3] = InvObj(oak)
            player.inv[4] = InvObj(obj("spade"))
            player.setVarBit(deps.bits[9610]!!, 1)
            player.setVarBit(deps.bits[9614]!!, 1)
            player.withProtectedAccess {
                check(eventBus.publish(this, LocUDefaultEvents.OpType(plot, plot, tree, oak, 3)))
            }
            assertEquals(0, player.count(obj("plantpot_empty")))
            assertEquals(2, player.vars[deps.bits[9610]!!])
        }
}
