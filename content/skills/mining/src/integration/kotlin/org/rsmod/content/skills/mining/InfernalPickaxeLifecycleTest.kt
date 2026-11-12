package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos

@Execution(ExecutionMode.SAME_THREAD)
class InfernalPickaxeLifecycleTest {
    @Test
    fun GameTestState.`cache 233 pins infernal pickaxe identities operations and dispatch`() =
        runMiningTest { deps ->
            assertEquals(13243, MiningObjs.infernalPickaxe.id)
            assertEquals(13244, MiningObjs.infernalPickaxeUncharged.id)
            assertEquals(25063, MiningObjs.infernalPickaxeTrailblazer.id)
            assertEquals(25369, MiningObjs.infernalPickaxeTrailblazerUncharged.id)
            assertEquals(30345, MiningObjs.infernalPickaxeTrailblazerReloaded.id)
            assertEquals(30346, MiningObjs.infernalPickaxeTrailblazerReloadedUncharged.id)
            assertEquals(
                listOf(null, "Wield", "Check", null, null),
                deps.objs[MiningObjs.infernalPickaxe].iop.toList(),
            )
            assertEquals(
                listOf(null, "Wield", null, null, null),
                deps.objs[MiningObjs.infernalPickaxeUncharged].iop.toList(),
            )
            for (style in deps.lifecycle.ornamentStyles) {
                val charged = style.plain == MiningObjs.infernalPickaxe
                for (ornamented in listOf(style.trailblazer, style.reloaded)) {
                    assertEquals(
                        listOf(
                            "Wield",
                            if (charged) "Check" else null,
                            "Revert",
                            "Change-Style",
                            null,
                        ),
                        deps.objs[ornamented].iop.toList(),
                    )
                    assertEquals(
                        if (charged) "Check" else "",
                        deps.objs[ornamented].paramOrNull(params.wear_op1),
                    )
                    assertEquals("Change-Style", deps.objs[ornamented].paramOrNull(params.wear_op2))
                    assertTrue(
                        deps.events.contains(WornObjEvents.Op3::class.java, ornamented.id.toLong())
                    )
                }
            }
            val itemPairs =
                listOf(MiningObjs.dragonPickaxe to objs.smouldering_stone) +
                    deps.lifecycle.cosmeticDragonPickaxes.map { it to objs.smouldering_stone } +
                    deps.lifecycle.ornamentPairs.map {
                        it.plain to objs.trailblazer_tool_ornament_kit
                    }
            for ((first, second) in itemPairs) {
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(first.id, second.id),
                    ),
                    "pair=${first.id},${second.id}",
                )
            }
        }

    @Test
    fun GameTestState.`standard dragon pickaxe creates a full infernal after three ticks`() =
        runMiningTest { deps ->
            repeat(2) { reverse ->
                prepare()
                player.inv[3] = InvObj(MiningObjs.dragonPickaxe)
                player.inv[8] = InvObj(objs.smouldering_stone)
                fillExcept(3, 8)

                player.withProtectedAccess {
                    val dragon = deps.objs[MiningObjs.dragonPickaxe]
                    val stone = deps.objs[objs.smouldering_stone]
                    if (reverse == 0) {
                        deps.heldU.interact(this, player.inv, dragon, 3, stone, 8)
                    } else {
                        deps.heldU.interact(this, player.inv, stone, 8, dragon, 3)
                    }
                }
                advance(2)
                assertEquals(MiningObjs.dragonPickaxe.id, player.inv[3]?.id)
                assertEquals(objs.smouldering_stone.id, player.inv[8]?.id)
                advance()

                assertEquals(MiningObjs.infernalPickaxe.id, player.inv[3]?.id)
                assertEquals(null, player.inv[8])
                assertEquals(
                    InfernalPickaxeState.MAX_CHARGES,
                    InfernalPickaxeState.fromCharged(player.inv[3]!!).charges,
                )
                assertEquals(2_000, player.statMap.getFineXP(stats.mining))
                assertEquals(3_500, player.statMap.getFineXP(stats.smithing))
            }
        }

    @Test
    fun GameTestState.`infernal creation uses base levels and revalidates both inputs`() =
        runMiningTest { deps ->
            prepare(mining = 60)
            player.stats.setCurrentLevel(stats.mining, 61)
            add(MiningObjs.dragonPickaxe)
            add(objs.smouldering_stone)
            createDirect(deps)
            assertEquals(0, player.count(MiningObjs.infernalPickaxe))

            prepare(smithing = 84)
            player.stats.setCurrentLevel(stats.smithing, 85)
            add(MiningObjs.dragonPickaxe)
            add(objs.smouldering_stone)
            createDirect(deps)
            assertEquals(0, player.count(MiningObjs.infernalPickaxe))

            prepare()
            add(MiningObjs.dragonPickaxe)
            add(objs.smouldering_stone)
            createDirect(deps)
            val stoneSlot = player.inv.indexOfFirst { it?.id == objs.smouldering_stone.id }
            player.inv[stoneSlot] = InvObj(objs.coins)
            advance(3)
            assertEquals(1, player.count(MiningObjs.dragonPickaxe))
            assertEquals(0, player.count(MiningObjs.infernalPickaxe))
            assertEquals(0, player.statMap.getFineXP(stats.mining))
            assertEquals(0, player.statMap.getFineXP(stats.smithing))
        }

    @Test
    fun GameTestState.`every cosmetic dragon pickaxe receives the historical rejection`() =
        runMiningTest { deps ->
            for (cosmetic in deps.lifecycle.cosmeticDragonPickaxes) {
                prepare()
                player.inv[3] = InvObj(cosmetic)
                player.inv[8] = InvObj(objs.smouldering_stone)
                player.withProtectedAccess {
                    deps.heldU.interact(
                        this,
                        player.inv,
                        deps.objs[cosmetic],
                        3,
                        deps.objs[objs.smouldering_stone],
                        8,
                    )
                }
                assertMessageSent(InfernalPickaxeLifecycle.COSMETIC_CONVERSION_REJECTION)
                assertEquals(cosmetic.id, player.inv[3]?.id)
                assertEquals(objs.smouldering_stone.id, player.inv[8]?.id)
            }
        }

    @Test
    fun GameTestState.`ornament application and removal preserve charge state atomically`() =
        runMiningTest { deps ->
            for (ornament in deps.lifecycle.ornamentPairs) {
                prepare()
                val vars =
                    if (ornament.plain == MiningObjs.infernalPickaxe) {
                        InfernalPickaxeState(123).pack()
                    } else {
                        0
                    }
                player.inv[3] = InvObj(ornament.plain, vars = vars)
                player.inv[8] = InvObj(objs.trailblazer_tool_ornament_kit)
                fillExcept(3, 8)
                player.withProtectedAccess {
                    deps.heldU.interact(
                        this,
                        player.inv,
                        deps.objs[ornament.plain],
                        3,
                        deps.objs[objs.trailblazer_tool_ornament_kit],
                        8,
                    )
                }
                assertEquals(ornament.ornamented.id, player.inv[3]?.id)
                assertEquals(vars, player.inv[3]?.vars)
                assertEquals(null, player.inv[8])

                player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
                assertEquals(ornament.plain.id, player.inv[3]?.id)
                assertEquals(vars, player.inv[3]?.vars)
                assertEquals(1, player.count(objs.trailblazer_tool_ornament_kit))
            }

            prepare()
            fillExcept()
            player.inv[3] =
                InvObj(
                    MiningObjs.infernalPickaxeTrailblazer,
                    vars = InfernalPickaxeState(77).pack(),
                )
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            assertEquals(MiningObjs.infernalPickaxeTrailblazer.id, player.inv[3]?.id)
            assertEquals(0, player.count(objs.trailblazer_tool_ornament_kit))
        }

    @Test
    fun GameTestState
        .`trailblazer styles switch in inventory and while worn without losing charges`() =
        runMiningTest { deps ->
            for (style in deps.lifecycle.ornamentStyles) {
                val vars =
                    if (style.plain == MiningObjs.infernalPickaxe) {
                        InfernalPickaxeState(246).pack()
                    } else {
                        0
                    }
                prepare()
                player.inv[3] = InvObj(style.trailblazer, vars = vars)
                player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op4) }
                assertEquals(style.reloaded.id, player.inv[3]?.id)
                assertEquals(vars, player.inv[3]?.vars)

                player.stats[stats.attack] = 60
                player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op1) }
                assertEquals(style.reloaded.id, player.worn[Wearpos.RightHand.slot]?.id)
                assertEquals(vars, player.worn[Wearpos.RightHand.slot]?.vars)
                player.withProtectedAccess {
                    deps.worn.interact(this, player.worn, Wearpos.RightHand.slot, IfButtonOp.Op3)
                }
                assertEquals(style.trailblazer.id, player.worn[Wearpos.RightHand.slot]?.id)
                assertEquals(vars, player.worn[Wearpos.RightHand.slot]?.vars)
            }
        }

    @Test
    fun GameTestState.`charge state packs exact counts and becomes depleted at zero`() =
        runMiningTest {
            val charged = InvObj(MiningObjs.infernalPickaxe, vars = InfernalPickaxeState(2).pack())
            assertEquals(2, InfernalPickaxeState.fromCharged(charged).charges)
            val one =
                InfernalPickaxeState.fromCharged(charged)
                    .consume(charged, MiningObjs.infernalPickaxeUncharged)
            assertEquals(MiningObjs.infernalPickaxe.id, one.id)
            assertEquals(1, InfernalPickaxeState.fromCharged(one).charges)
            val empty =
                InfernalPickaxeState.fromCharged(one)
                    .consume(one, MiningObjs.infernalPickaxeUncharged)
            assertEquals(MiningObjs.infernalPickaxeUncharged.id, empty.id)
            assertEquals(0, empty.vars)
            assertEquals(
                InfernalPickaxeState.MAX_CHARGES,
                InfernalPickaxeState.fromCharged(InvObj(MiningObjs.infernalPickaxe)).charges,
            )
        }

    private fun GameTestScope.prepare(mining: Int = 61, smithing: Int = 85) {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.mining] = mining
        player.stats[stats.smithing] = smithing
        player.statMap.setFineXP(stats.mining, 0)
        player.statMap.setFineXP(stats.smithing, 0)
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType) {
        player.inv[player.inv.indexOfFirst { it == null }] = InvObj(obj)
    }

    private fun GameTestScope.fillExcept(vararg empty: Int) {
        for (slot in player.inv.indices) {
            if (slot !in empty && player.inv[slot] == null) {
                player.inv[slot] = InvObj(objs.coins)
            }
        }
    }

    private fun GameTestScope.createDirect(deps: Dependencies) {
        val dragonSlot = player.inv.indexOfFirst { it?.id == MiningObjs.dragonPickaxe.id }
        val stoneSlot = player.inv.indexOfFirst { it?.id == objs.smouldering_stone.id }
        player.withProtectedAccess { deps.lifecycle.create(this, dragonSlot, stoneSlot) }
    }

    private fun GameTestState.runMiningTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, childModule = null, Mining::class, testBody = body)

    data class Dependencies
    @Inject
    constructor(
        val lifecycle: InfernalPickaxeLifecycle,
        val events: EventBus,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
        val worn: WornInteractions,
        val objs: ObjTypeList,
    )
}
