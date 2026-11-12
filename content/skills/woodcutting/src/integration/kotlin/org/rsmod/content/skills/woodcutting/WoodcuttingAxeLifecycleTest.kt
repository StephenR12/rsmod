package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpLifetime

@Execution(ExecutionMode.SAME_THREAD)
class WoodcuttingAxeLifecycleTest {
    @Test
    fun GameTestState.`cache 233 pins axe lifecycle operations and dispatch`() =
        runWoodcuttingTest { deps ->
            assertEquals(36552, CrystalSingingLocs.bowl.id)
            assertEquals(
                listOf("Sing-crystal", null, null, null, null),
                locTypes[CrystalSingingLocs.bowl].op.toList(),
            )
            assertEquals(listOf(9134, 9136), CrystalSingingNpcs.assistants.map { it.id })
            for (singer in CrystalSingingNpcs.assistants) {
                assertEquals(
                    listOf("Talk-to", null, "Sing", null, null),
                    npcTypes[singer].op.toList(),
                )
                assertTrue(deps.events.contains(NpcEvents.Op3::class.java, singer.id))
            }
            assertEquals(8459, seqs.prif_crystal_singing.id)
            assertTrue(deps.events.contains(LocEvents.Op1::class.java, CrystalSingingLocs.bowl.id))
            assertEquals(
                listOf(null, "Wield", "Check", null, null),
                deps.objs[objs.infernal_axe].iop.toList(),
            )
            assertEquals(
                listOf(null, "Wield", null, null, null),
                deps.objs[objs.infernal_axe_uncharged].iop.toList(),
            )
            for (style in deps.infernal.ornamentStyles) {
                val charged = style.plain == objs.infernal_axe
                for (ornamented in listOf(style.trailblazer, style.reloaded)) {
                    val type = deps.objs[ornamented]
                    assertEquals(
                        listOf(
                            "Wield",
                            if (charged) "Check" else null,
                            "Revert",
                            "Change-Style",
                            null,
                        ),
                        type.iop.toList(),
                    )
                    assertEquals(if (charged) "Check" else "", type.paramOrNull(params.wear_op1))
                    assertEquals("Change-Style", type.paramOrNull(params.wear_op2))
                    assertTrue(
                        deps.events.contains(WornObjEvents.Op3::class.java, ornamented.id.toLong())
                    )
                }
            }
            assertEquals(
                listOf(null, "Wield", "Check", "Revert", null),
                deps.objs[objs.crystal_axe].iop.toList(),
            )
            assertEquals(
                listOf(null, "Wield", null, "Revert", null),
                deps.objs[objs.crystal_axe_inactive].iop.toList(),
            )
            assertEquals("Check", deps.objs[objs.crystal_axe].paramOrNull(params.wear_op1))
            assertEquals("", deps.objs[objs.crystal_axe_inactive].paramOrNull(params.wear_op1))
            assertEquals(4_338, deps.objs[objs.crystal_axe].paramOrNull(params.bank_autocharge)?.id)
            val bankAutochargeQuantity = deps.varbits[varbits.charges_crystal_tools_quantity]
            assertEquals(16_148, bankAutochargeQuantity.id)
            assertEquals(4_681, bankAutochargeQuantity.baseVar.id)
            assertEquals(0..15, bankAutochargeQuantity.bits)
            assertEquals(VarpLifetime.Perm, bankAutochargeQuantity.baseVar.scope)
            assertTrue(
                deps.events.contains(
                    BankedObjEvents.ConfigureCharges::class.java,
                    objs.crystal_axe.id,
                )
            )

            val heldUsePairs =
                listOf(objs.dragon_axe to objs.smouldering_stone) +
                    deps.infernal.rechargePairs.flatMap { recharge ->
                        listOf(
                            recharge.depleted to objs.smouldering_stone,
                            recharge.depleted to objs.dragon_axe,
                        )
                    } +
                    deps.infernal.ornamentPairs.map {
                        it.plain to objs.trailblazer_tool_ornament_kit
                    } +
                    listOf(
                        objs.crystal_axe to objs.crystal_shard,
                        objs.crystal_axe_inactive to objs.crystal_shard,
                    )
            for ((first, second) in heldUsePairs) {
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
    fun GameTestState
        .`singing bowl creates every available axe in three ticks with boosted levels`() =
        runWoodcuttingTest { deps ->
            prepare(crafting = 75, smithing = 75)
            player.stats.setCurrentLevel(stats.crafting, 76)
            player.stats.setCurrentLevel(stats.smithing, 76)
            player.inv[0] = InvObj(objs.dragon_axe)
            player.inv[1] = InvObj(objs.dragon_axe)
            player.inv[2] = InvObj(objs.crystal_tool_seed)
            player.inv[3] = InvObj(objs.crystal_tool_seed)
            player.inv[4] = InvObj(objs.crystal_shard, 250)

            player.withProtectedAccess { deps.singing.create(this, assisted = false) }
            advance(2)
            assertEquals(0, player.count(objs.crystal_axe))
            advance()

            assertEquals(2, player.count(objs.crystal_axe))
            assertEquals(10, player.count(objs.crystal_shard))
            assertEquals(0, player.count(objs.dragon_axe))
            assertEquals(0, player.count(objs.crystal_tool_seed))
            assertEquals(120_000, player.statMap.getFineXP(stats.crafting))
            assertEquals(120_000, player.statMap.getFineXP(stats.smithing))
            for (axe in player.inv.filterNotNull { it.id == objs.crystal_axe.id }) {
                assertEquals(10_000, CrystalAxeState.fromCharged(axe).charges)
            }
        }

    @Test
    fun GameTestState.`bowl opens the native crystal axe product chooser`() =
        runWoodcuttingTest { deps ->
            prepare()
            player.withProtectedAccess { deps.singing.selectAtBowl(this) }

            assertTrue(
                client.filterIsInstance<IfOpenSub>().any {
                    it.interfaceId == interfaces.skillmulti.id
                }
            )
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    1,
                    objs.crystal_axe.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    1,
                    "What would you like to sing?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().single {
                    it.interfaceId == interfaces.skillmulti.id && it.componentId == 14
                }
            assertEquals(1, events.start)
            assertEquals(1, events.end)
        }

    @Test
    fun GameTestState.`singing bowl gates membership quest skills materials and stale actions`() =
        runWoodcuttingTest { deps ->
            prepare()
            player.members = false
            add(objs.dragon_axe)
            add(objs.crystal_tool_seed)
            player.inv[2] = InvObj(objs.crystal_shard, 120)
            player.withProtectedAccess { deps.singing.create(this, assisted = false) }
            assertEquals(0, player.count(objs.crystal_axe))

            prepare()
            player.setVarBit(varbits.song_of_the_elves, 193)
            add(objs.dragon_axe)
            add(objs.crystal_tool_seed)
            player.inv[2] = InvObj(objs.crystal_shard, 120)
            player.withProtectedAccess { deps.singing.create(this, assisted = false) }
            assertEquals(0, player.count(objs.crystal_axe))

            prepare(crafting = 75)
            add(objs.dragon_axe)
            add(objs.crystal_tool_seed)
            player.inv[2] = InvObj(objs.crystal_shard, 120)
            player.withProtectedAccess { deps.singing.create(this, assisted = false) }
            assertEquals(0, player.count(objs.crystal_axe))

            prepare(smithing = 75)
            add(objs.dragon_axe)
            add(objs.crystal_tool_seed)
            player.inv[2] = InvObj(objs.crystal_shard, 120)
            player.withProtectedAccess { deps.singing.create(this, assisted = false) }
            assertEquals(0, player.count(objs.crystal_axe))

            prepare()
            add(objs.dragon_axe)
            add(objs.crystal_tool_seed)
            player.inv[2] = InvObj(objs.crystal_shard, 119)
            player.withProtectedAccess { deps.singing.create(this, assisted = false) }
            assertEquals(0, player.count(objs.crystal_axe))

            prepare()
            add(objs.dragon_axe)
            add(objs.crystal_tool_seed)
            player.inv[2] = InvObj(objs.crystal_shard, 120)
            player.withProtectedAccess { deps.singing.create(this, assisted = false) }
            player.inv[2] = InvObj(objs.crystal_shard, 119)
            advance(3)
            assertEquals(1, player.count(objs.dragon_axe))
            assertEquals(1, player.count(objs.crystal_tool_seed))
            assertEquals(119, player.count(objs.crystal_shard))
            assertEquals(0, player.count(objs.crystal_axe))
            assertEquals(0, player.statMap.getFineXP(stats.crafting))
            assertEquals(0, player.statMap.getFineXP(stats.smithing))
        }

    @Test
    fun GameTestState.`learned singers charge the sixty shard premium without skills or xp`() =
        runWoodcuttingTest { deps ->
            prepare(crafting = 1, smithing = 1)
            add(objs.dragon_axe)
            add(objs.crystal_tool_seed)
            player.inv[2] = InvObj(objs.crystal_shard, 180)

            player.withProtectedAccess { deps.singing.create(this, assisted = true) }
            advance(3)

            assertEquals(1, player.count(objs.crystal_axe))
            assertEquals(0, player.count(objs.crystal_shard))
            assertEquals(
                10_000,
                CrystalAxeState.fromCharged(player.inv.filterNotNull().single()).charges,
            )
            assertEquals(0, player.statMap.getFineXP(stats.crafting))
            assertEquals(0, player.statMap.getFineXP(stats.smithing))
        }

    @Test
    fun GameTestState.`infernal creation takes three ticks and uses unboosted levels`() =
        runWoodcuttingTest { deps ->
            repeat(2) { reverse ->
                prepare()
                player.inv[3] = InvObj(objs.dragon_axe)
                player.inv[8] = InvObj(objs.smouldering_stone)
                fillExcept(3, 8)
                interactUse(deps, objs.dragon_axe, 3, objs.smouldering_stone, 8, reverse != 0)
                advance(2)
                assertEquals(objs.dragon_axe.id, player.inv[3]?.id)
                advance()
                assertEquals(objs.infernal_axe.id, player.inv[3]?.id)
                assertEquals(null, player.inv[8])
                assertEquals(5_000, InfernalAxeState.fromCharged(player.inv[3]!!).charges)
                assertEquals(2_000, player.statMap.getFineXP(stats.woodcutting))
                assertEquals(3_500, player.statMap.getFineXP(stats.firemaking))
            }

            prepare(woodcutting = 60)
            player.stats.setCurrentLevel(stats.woodcutting, 61)
            add(objs.dragon_axe)
            add(objs.smouldering_stone)
            createDirect(deps)
            assertEquals(1, player.count(objs.dragon_axe))

            prepare(firemaking = 84)
            player.stats.setCurrentLevel(stats.firemaking, 85)
            add(objs.dragon_axe)
            add(objs.smouldering_stone)
            createDirect(deps)
            assertEquals(1, player.count(objs.dragon_axe))

            prepare()
            add(objs.dragon_axe)
            add(objs.smouldering_stone)
            createDirect(deps)
            val stoneSlot = player.inv.indexOfFirst { it?.id == objs.smouldering_stone.id }
            player.inv[stoneSlot] = InvObj(objs.coins)
            advance(3)
            assertEquals(1, player.count(objs.dragon_axe))
            assertEquals(0, player.count(objs.infernal_axe))
            assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
            assertEquals(0, player.statMap.getFineXP(stats.firemaking))
        }

    @Test
    fun GameTestState.`every infernal style recharges with either material and awards both xp`() =
        runWoodcuttingTest { deps ->
            for (recharge in deps.infernal.rechargePairs) {
                for (material in listOf(objs.smouldering_stone, objs.dragon_axe)) {
                    repeat(2) { reverse ->
                        prepare(woodcutting = 1, firemaking = 1)
                        player.inv[3] = InvObj(recharge.depleted)
                        player.inv[8] = InvObj(material)
                        fillExcept(3, 8)
                        interactUse(deps, recharge.depleted, 3, material, 8, reverse != 0)
                        assertEquals(recharge.charged.id, player.inv[3]?.id)
                        assertEquals(null, player.inv[8])
                        assertEquals(5_000, InfernalAxeState.fromCharged(player.inv[3]!!).charges)
                        assertEquals(2_000, player.statMap.getFineXP(stats.woodcutting))
                        assertEquals(3_500, player.statMap.getFineXP(stats.firemaking))
                    }
                }
            }

            prepare()
            player.members = false
            player.inv[3] = InvObj(objs.infernal_axe_uncharged)
            player.inv[8] = InvObj(objs.smouldering_stone)
            interactUse(deps, objs.infernal_axe_uncharged, 3, objs.smouldering_stone, 8, false)
            assertEquals(objs.infernal_axe_uncharged.id, player.inv[3]?.id)
            assertEquals(objs.smouldering_stone.id, player.inv[8]?.id)
            assertMessageSent("You need to be on a members' world to recharge an infernal axe.")
        }

    @Test
    fun GameTestState.`infernal checks ornaments and style changes preserve charge state`() =
        runWoodcuttingTest { deps ->
            for (style in deps.infernal.ornamentStyles) {
                val vars =
                    if (style.plain == objs.infernal_axe) InfernalAxeState(2_499).pack() else 0
                prepare()
                player.inv[3] = InvObj(style.plain, vars = vars)
                player.inv[8] = InvObj(objs.trailblazer_tool_ornament_kit)
                interactUse(deps, style.plain, 3, objs.trailblazer_tool_ornament_kit, 8, false)
                assertEquals(style.trailblazer.id, player.inv[3]?.id)
                assertEquals(vars, player.inv[3]?.vars)

                if (style.plain == objs.infernal_axe) {
                    player.withProtectedAccess {
                        deps.held.interact(this, player.inv, 3, HeldOp.Op2)
                    }
                    assertMessageSent("Your infernal axe is 49% charged.")
                }

                player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op4) }
                assertEquals(style.reloaded.id, player.inv[3]?.id)
                assertEquals(vars, player.inv[3]?.vars)

                player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op1) }
                assertEquals(style.reloaded.id, player.worn[Wearpos.RightHand.slot]?.id)
                player.withProtectedAccess {
                    deps.worn.interact(this, player.worn, Wearpos.RightHand.slot, IfButtonOp.Op3)
                }
                assertEquals(style.trailblazer.id, player.worn[Wearpos.RightHand.slot]?.id)

                player.inv[0] = player.worn[Wearpos.RightHand.slot]
                player.worn[Wearpos.RightHand.slot] = null
                val axeSlot = player.inv.indexOfFirst { it?.id == style.trailblazer.id }
                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, axeSlot, HeldOp.Op3)
                }
                assertEquals(style.plain.id, player.inv[axeSlot]?.id)
                assertEquals(vars, player.inv[axeSlot]?.vars)
                assertEquals(1, player.count(objs.trailblazer_tool_ornament_kit))
            }

            prepare()
            player.inv[3] = InvObj(objs.infernal_axe, vars = InfernalAxeState(5_000).pack())
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            assertMessageSent("Your infernal axe is 100% charged.")
        }

    @Test
    fun GameTestState.`crystal shards charge active and inactive axes to the cap`() =
        runWoodcuttingTest { deps ->
            prepare()
            player.inv[3] = InvObj(objs.crystal_axe, vars = CrystalAxeState(19_850).pack())
            player.inv[4] = InvObj(objs.crystal_shard, 3)
            interactUse(deps, objs.crystal_axe, 3, objs.crystal_shard, 4, false)
            player.resumeCountDialog(10)
            advance()
            assertEquals(1, player.count(objs.crystal_shard))
            assertEquals(20_000, CrystalAxeState.fromCharged(player.inv[3]!!).charges)

            prepare()
            player.inv[3] = InvObj(objs.crystal_shard, 3)
            player.inv[4] = InvObj(objs.crystal_axe_inactive)
            interactUse(deps, objs.crystal_shard, 3, objs.crystal_axe_inactive, 4, false)
            player.resumeCountDialog(2)
            advance()
            assertEquals(1, player.count(objs.crystal_shard))
            assertEquals(objs.crystal_axe.id, player.inv[4]?.id)
            assertEquals(200, CrystalAxeState.fromCharged(player.inv[4]!!).charges)

            prepare()
            player.inv[3] = InvObj(objs.crystal_axe, vars = CrystalAxeState(20_000).pack())
            player.inv[4] = InvObj(objs.crystal_shard, 2)
            interactUse(deps, objs.crystal_axe, 3, objs.crystal_shard, 4, false)
            assertEquals(2, player.count(objs.crystal_shard))
            assertMessageSent("The crystal axe is already fully charged.")
        }

    @Test
    fun GameTestState.`crystal bank charge preference is permanent capped and members only`() =
        runWoodcuttingTest { deps ->
            prepare()
            player.withProtectedAccess { deps.crystal.configureCharges(this) }
            player.resumeCountDialog(25_000)
            advance()
            assertEquals(20_000, player.vars[varbits.charges_crystal_tools_quantity])

            player.members = false
            client.clear()
            player.withProtectedAccess { deps.crystal.configureCharges(this) }
            assertEquals(20_000, player.vars[varbits.charges_crystal_tools_quantity])
            assertMessageSent("You need to be on a members' world to configure a crystal axe.")
        }

    @Test
    fun GameTestState.`crystal check and revert cover active inactive and full inventory`() =
        runWoodcuttingTest { deps ->
            prepare()
            player.inv[3] = InvObj(objs.crystal_axe, vars = CrystalAxeState(12_345).pack())
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            assertMessageSent("The crystal axe has 12345 charges remaining.")

            for (axe in listOf(objs.crystal_axe, objs.crystal_axe_inactive)) {
                prepare()
                player.inv[3] = InvObj(axe, vars = 12_345)
                player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op4) }
                assertEquals(objs.dragon_axe.id, player.inv[3]?.id)
                assertEquals(1, player.count(objs.crystal_tool_seed))
            }

            prepare()
            fillExcept()
            player.inv[3] = InvObj(objs.crystal_axe, vars = CrystalAxeState(321).pack())
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op4) }
            assertEquals(objs.crystal_axe.id, player.inv[3]?.id)
            assertEquals(321, CrystalAxeState.fromCharged(player.inv[3]!!).charges)
            assertEquals(0, player.count(objs.crystal_tool_seed))
            assertMessageSent("You do not have enough inventory space to revert the crystal axe.")
        }

    private fun GameTestScope.prepare(
        woodcutting: Int = 61,
        firemaking: Int = 85,
        crafting: Int = 76,
        smithing: Int = 76,
    ) {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.attack] = 60
        player.stats[stats.woodcutting] = woodcutting
        player.stats[stats.firemaking] = firemaking
        player.stats[stats.crafting] = crafting
        player.stats[stats.smithing] = smithing
        player.setVarBit(varbits.song_of_the_elves, CrystalAxeSinging.SONG_OF_THE_ELVES_COMPLETE)
        player.statMap.setFineXP(stats.woodcutting, 0)
        player.statMap.setFineXP(stats.firemaking, 0)
        player.statMap.setFineXP(stats.crafting, 0)
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

    private fun GameTestScope.interactUse(
        deps: Dependencies,
        first: ObjType,
        firstSlot: Int,
        second: ObjType,
        secondSlot: Int,
        reverse: Boolean,
    ) {
        player.withProtectedAccess {
            if (reverse) {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[second],
                    secondSlot,
                    deps.objs[first],
                    firstSlot,
                )
            } else {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[first],
                    firstSlot,
                    deps.objs[second],
                    secondSlot,
                )
            }
        }
    }

    private fun GameTestScope.createDirect(deps: Dependencies) {
        val dragonSlot = player.inv.indexOfFirst { it?.id == objs.dragon_axe.id }
        val stoneSlot = player.inv.indexOfFirst { it?.id == objs.smouldering_stone.id }
        player.withProtectedAccess { deps.infernal.create(this, dragonSlot, stoneSlot) }
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
        val infernal: InfernalAxeLifecycle,
        val crystal: CrystalAxeLifecycle,
        val singing: CrystalAxeSinging,
        val events: EventBus,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
        val worn: WornInteractions,
        val objs: ObjTypeList,
        val varbits: VarBitTypeList,
    )
}
