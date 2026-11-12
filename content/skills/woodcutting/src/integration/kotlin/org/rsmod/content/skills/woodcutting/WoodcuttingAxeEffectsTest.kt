package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingParams
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axePriority
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeSoteRequired
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingReq
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.cutSuccessRates
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.findAxe
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLevelReq
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLogs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class WoodcuttingAxeEffectsTest {
    @Test
    fun GameTestState.`cache 233 special axe identities access and priorities are pinned`() =
        runWoodcuttingTest { deps ->
            val definitions =
                listOf(
                    AxeDefinition(objs.dragon_axe, 6739, 61, 8, false),
                    AxeDefinition(objs.dragon_axe_or, 25378, 61, 8, false),
                    AxeDefinition(objs.dragon_axe_or_reloaded, 30352, 61, 8, false),
                    AxeDefinition(objs.third_age_axe, 20011, 61, 8, false),
                    AxeDefinition(objs.infernal_axe, 13241, 61, 8, false),
                    AxeDefinition(objs.infernal_axe_uncharged, 13242, 61, 8, false),
                    AxeDefinition(objs.infernal_axe_or, 25066, 61, 8, false),
                    AxeDefinition(objs.infernal_axe_or_uncharged, 25371, 61, 8, false),
                    AxeDefinition(objs.infernal_axe_or_reloaded, 30347, 61, 8, false),
                    AxeDefinition(objs.infernal_axe_or_reloaded_uncharged, 30348, 61, 8, false),
                    AxeDefinition(objs.crystal_axe, 23673, 71, 9, true),
                    AxeDefinition(objs.crystal_axe_inactive, 23675, 71, 8, true),
                )
            for (definition in definitions) {
                val type = deps.objs[definition.type]
                assertEquals(definition.id, type.id)
                assertEquals(definition.level, type.axeWoodcuttingReq, type.name)
                assertEquals(definition.priority, type.axePriority, type.name)
                assertEquals(definition.soteRequired, type.axeSoteRequired, type.name)
                assertTrue(type.members, type.name)
                assertEquals(content.woodcutting_axe.id, type.contentGroup, type.name)
            }

            val trees =
                cacheTypes.locs.values.filter {
                    it.isContentType(content.tree) &&
                        it.paramMap?.contains(WoodcuttingParams.success_rates) == true
                }
            assertTrue(trees.isNotEmpty())
            for (tree in trees) {
                val dragon = cutSuccessRates(tree, InvObj(objs.dragon_axe), cacheTypes.enums)
                val inactive =
                    cutSuccessRates(tree, InvObj(objs.crystal_axe_inactive), cacheTypes.enums)
                val active = cutSuccessRates(tree, InvObj(objs.crystal_axe), cacheTypes.enums)
                assertEquals(dragon, inactive, tree.name)
                assertTrue(active.first >= inactive.first, tree.name)
                assertTrue(active.second >= inactive.second, tree.name)
            }
        }

    @Test
    fun GameTestState.`best usable axe respects F2P quest and real efficiency order`() =
        runWoodcuttingTest { deps ->
            player.clearAllInvs()
            player.stats[stats.woodcutting] = 99
            player.inv[0] = InvObj(objs.bronze_axe)
            player.inv[1] = InvObj(objs.iron_axe)
            player.inv[2] = InvObj(objs.rune_axe)
            player.righthand = InvObj(objs.dragon_axe)

            player.members = false
            assertEquals(objs.rune_axe.id, findAxe(player, deps.objs)?.id)

            player.members = true
            player.righthand = InvObj(objs.crystal_axe)
            player.setVarBit(varbits.song_of_the_elves, 193)
            assertEquals(objs.rune_axe.id, findAxe(player, deps.objs)?.id)

            player.setVarBit(varbits.song_of_the_elves, 194)
            assertEquals(objs.crystal_axe.id, findAxe(player, deps.objs)?.id)

            player.righthand = null
            player.inv[0] = InvObj(objs.crystal_axe_inactive)
            player.inv[1] = InvObj(objs.crystal_axe)
            assertEquals(objs.crystal_axe.id, findAxe(player, deps.objs)?.id)
        }

    @Test
    fun GameTestState.`infernal variants burn eligible logs and preserve exact charge states`() =
        runWoodcuttingTest { deps ->
            assertEquals(0..12, varobjs.infernal_tool_charges.bits)
            assertEquals(5_000, InfernalAxeState.fromCharged(InvObj(objs.infernal_axe)).charges)

            for ((charged, depleted) in WoodcuttingAxeEffects.infernalDepletedTypes) {
                prepare()
                val axe = InvObj(charged, vars = InfernalAxeState(1).pack())
                player.inv[0] = axe
                random.next = 0
                val result = apply(deps, axe, objs.redwood_logs)

                assertTrue(result.logConsumed)
                assertEquals(depleted.id, player.inv[0]?.id)
                assertEquals(1_750, player.statMap.getFineXP(stats.firemaking))
            }

            prepare()
            val axe = InvObj(objs.infernal_axe, vars = InfernalAxeState(2).pack())
            player.inv[0] = axe
            equipPyromancer()
            random.next = 0
            val result = apply(deps, axe, objs.logs)
            assertTrue(result.logConsumed)
            assertEquals(1, InfernalAxeState.fromCharged(player.inv[0]!!).charges)
            assertEquals(205, player.statMap.getFineXP(stats.firemaking))

            prepare()
            val uncharged = InvObj(objs.infernal_axe_uncharged)
            player.inv[0] = uncharged
            val inactiveResult = apply(deps, uncharged, objs.logs)
            assertFalse(inactiveResult.logConsumed)
            assertEquals(uncharged, player.inv[0])
            assertEquals(0, player.statMap.getFineXP(stats.firemaking))
        }

    @Test
    fun GameTestState.`crystal charges and both independent saving effects are exact`() =
        runWoodcuttingTest { deps ->
            assertEquals(0..14, varobjs.crystal_tool_charges.bits)
            assertEquals(10_000, CrystalAxeState.fromCharged(InvObj(objs.crystal_axe)).charges)

            prepare()
            var axe = InvObj(objs.crystal_axe, vars = CrystalAxeState(2).pack())
            player.inv[0] = axe
            apply(deps, axe, objs.logs)
            assertEquals(1, CrystalAxeState.fromCharged(player.inv[0]!!).charges)

            axe = player.inv[0]!!
            apply(deps, axe, objs.logs)
            assertEquals(objs.crystal_axe_inactive.id, player.inv[0]?.id)

            val signets =
                listOf(objs.elven_signet, objs.celestial_signet, objs.celestial_signet_charged)
            for (signet in signets) {
                prepare()
                axe = InvObj(objs.crystal_axe, vars = CrystalAxeState(2).pack())
                player.inv[0] = axe
                player.worn[Wearpos.Ring.slot] = InvObj(signet)
                random.next = 0
                apply(deps, axe, objs.logs)
                assertEquals(2, CrystalAxeState.fromCharged(player.inv[0]!!).charges)
            }

            prepare()
            axe = InvObj(objs.crystal_axe, vars = CrystalAxeState(2).pack())
            player.inv[0] = axe
            player.worn[Wearpos.Ring.slot] = InvObj(objs.elven_signet)
            player.worn[Wearpos.Quiver.slot] = InvObj(objs.ghommals_lucky_penny)
            random.next = 1
            random.then = 0
            apply(deps, axe, objs.logs)
            assertEquals(2, CrystalAxeState.fromCharged(player.inv[0]!!).charges)

            prepare()
            axe = InvObj(objs.infernal_axe, vars = InfernalAxeState(2).pack())
            player.inv[0] = axe
            player.worn[Wearpos.Quiver.slot] = InvObj(objs.ghommals_lucky_penny)
            random.next = 0
            random.then = 0
            apply(deps, axe, objs.logs)
            assertEquals(2, InfernalAxeState.fromCharged(player.inv[0]!!).charges)
        }

    @Test
    fun GameTestState.`successful infernal tree cycle replaces the log with Firemaking xp`() =
        runWoodcuttingTest { deps ->
            prepare()
            val type = findLocType(content.tree) { it.treeLevelReq == 1 }
            val tree = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(tree.coords.translateX(-1))
            player.righthand = InvObj(objs.infernal_axe, vars = InfernalAxeState(2).pack())
            player.opLoc1(tree)
            advance(3)

            random.next = 0
            random.then = 0
            random.then = 255
            random.then = 0
            advance()

            assertFalse(player.inv.contains(type.treeLogs))
            assertEquals(200, player.statMap.getFineXP(stats.firemaking))
            assertEquals(1, InfernalAxeState.fromCharged(player.righthand!!).charges)
        }

    private fun GameTestScope.prepare() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.woodcutting] = 99
        player.stats[stats.firemaking] = 1
        player.statMap.setFineXP(stats.woodcutting, 0)
        player.statMap.setFineXP(stats.firemaking, 0)
        player.setVarBit(varbits.song_of_the_elves, 194)
        player.setVarBit(varbits.vesseled_clues_disabled, 1)
        client.clear()
    }

    private fun GameTestScope.equipPyromancer() {
        player.worn[Wearpos.Hat.slot] = InvObj(objs.pyromancer_hood)
        player.worn[Wearpos.Torso.slot] = InvObj(objs.pyromancer_garb)
        player.worn[Wearpos.Legs.slot] = InvObj(objs.pyromancer_robe)
        player.worn[Wearpos.Feet.slot] = InvObj(objs.pyromancer_boots)
    }

    private fun GameTestScope.apply(
        deps: Dependencies,
        axe: InvObj,
        product: ObjType,
    ): WoodcuttingAxeEffects.Result {
        lateinit var result: WoodcuttingAxeEffects.Result
        player.withProtectedAccess { result = deps.effects.apply(this, axe, product) }
        return result
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
    constructor(val effects: WoodcuttingAxeEffects, val objs: ObjTypeList)

    private data class AxeDefinition(
        val type: ObjType,
        val id: Int,
        val level: Int,
        val priority: Int,
        val soteRequired: Boolean,
    )
}
