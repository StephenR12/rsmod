package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class StandardFishingTest {
    @Test
    fun GameTestState.walkingAwayCancelsFishingBeforeTheNextCatchRoll() = runFishingTest { d ->
        prepare(level = 99)
        player.inv[0] = InvObj(objs.small_fishing_net)
        val npc = spawnSpot(d)
        val origin = player.coords
        player.opNpc1(npc)
        advance()
        player.moveGameClick(origin.translate(-1, 0))
        advance(2)
        assertTrue(player.coords != origin)
        advance(8)
        assertEquals(0, player.count(objs.raw_shrimps) + player.count(objs.raw_anchovies))
    }

    @Test
    fun GameTestState.`Al Kharid anchovies award the local diary task on a successful catch`() =
        runFishingTest { deps ->
            prepare(level = 15)
            player.placeAt(CoordGrid(3266, 3148))
            add(objs.small_fishing_net)
            random.next = 0
            val npc =
                spawnNpc(CoordGrid(3267, 3148), deps.npcs[SmallNetBaitFishingNpcs.saltfish_51_49])
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            advance(5)
            assertEquals(1, player.count(objs.raw_anchovies))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchAlKharidAnchovies))
        }

    @Test
    fun GameTestState.`cache 233 pins fish sack barrel identities and operations`() =
        runFishingTest { deps ->
            assertEquals(22838, objs.fish_sack.id)
            assertEquals(25585, objs.fish_sack_barrel_closed.id)
            assertEquals(25587, objs.fish_sack_barrel_open.id)
            assertEquals(
                listOf(null, "Wear", null, null, null),
                deps.objs[objs.fish_sack].iop.toList(),
            )
            assertEquals(
                listOf("Fill", "Wear", "Configure", "Open", "Destroy"),
                deps.objs[objs.fish_sack_barrel_closed].iop.toList(),
            )
            assertEquals(
                listOf("Fill", "Wear", "Configure", "Close", "Destroy"),
                deps.objs[objs.fish_sack_barrel_open].iop.toList(),
            )
            for (barrel in listOf(objs.fish_sack_barrel_closed, objs.fish_sack_barrel_open)) {
                assertEquals(Wearpos.Back.slot, deps.objs[barrel].wearpos1)
                assertEquals("Check", deps.objs[barrel].paramOrNull(params.wear_op1))
                assertTrue(deps.events.contains(WornObjEvents.Op2::class.java, barrel.id.toLong()))
                assertTrue(deps.events.contains(WornObjEvents.Op3::class.java, barrel.id.toLong()))
            }
            assertEquals(
                "Open",
                deps.objs[objs.fish_sack_barrel_closed].paramOrNull(params.wear_op2),
            )
            assertEquals(
                "Close",
                deps.objs[objs.fish_sack_barrel_open].paramOrNull(params.wear_op2),
            )
        }

    @Test
    fun GameTestState.`cache 233 pins crystal harpoon lifecycle operations`() =
        runFishingTest { deps ->
            assertEquals(
                listOf(null, "Wield", "Check", "Revert", null),
                deps.objs[objs.crystal_harpoon].iop.toList(),
            )
            assertEquals(
                listOf(null, "Wield", null, "Revert", null),
                deps.objs[objs.crystal_harpoon_inactive].iop.toList(),
            )
            assertEquals("Check", deps.objs[objs.crystal_harpoon].paramOrNull(params.wear_op1))
            assertEquals("", deps.objs[objs.crystal_harpoon_inactive].paramOrNull(params.wear_op1))
            assertEquals(
                4_338,
                deps.objs[objs.crystal_harpoon].paramOrNull(params.bank_autocharge)?.id,
            )
            val bankAutochargeQuantity = deps.varbits[varbits.charges_crystal_tools_quantity]
            assertEquals(16_148, bankAutochargeQuantity.id)
            assertEquals(4_681, bankAutochargeQuantity.baseVar.id)
            assertEquals(0..15, bankAutochargeQuantity.bits)
            assertEquals(VarpLifetime.Perm, bankAutochargeQuantity.baseVar.scope)
            assertEquals(23953, objs.crystal_tool_seed.id)
            for (harpoon in listOf(objs.crystal_harpoon, objs.crystal_harpoon_inactive)) {
                assertTrue(
                    deps.events.contains(
                        BankedObjEvents.ConfigureCharges::class.java,
                        harpoon.id.toLong(),
                    )
                )
            }
            assertTrue(
                deps.events.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.crystal_harpoon.id, objs.crystal_shard.id),
                )
            )
            assertTrue(
                deps.events.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.crystal_harpoon_inactive.id, objs.crystal_shard.id),
                )
            )
        }

    @Test
    fun GameTestState.`crystal harpoon bank preference is permanent capped and members only`() =
        runFishingTest { deps ->
            player.members = true
            player.withProtectedAccess { deps.crystal.configureCharges(this) }
            player.resumeCountDialog(25_000)
            advance()
            assertEquals(20_000, player.vars[varbits.charges_crystal_tools_quantity])

            player.members = false
            client.clear()
            player.withProtectedAccess { deps.crystal.configureCharges(this) }
            assertEquals(20_000, player.vars[varbits.charges_crystal_tools_quantity])
            assertMessageSent("You need to be on a members' world to configure a crystal harpoon.")
        }

    @Test
    fun GameTestState.`cache 233 pins fishing charge saving equipment identities`() =
        runFishingTest { deps ->
            assertEquals(23943, objs.elven_signet.id)
            assertEquals(25543, objs.celestial_signet.id)
            assertEquals(25545, objs.celestial_signet_charged.id)
            assertEquals(27544, objs.ghommals_lucky_penny.id)
            assertEquals(Wearpos.Ring.slot, deps.objs[objs.elven_signet].wearpos1)
            assertEquals(Wearpos.Ring.slot, deps.objs[objs.celestial_signet].wearpos1)
            assertEquals(Wearpos.Ring.slot, deps.objs[objs.celestial_signet_charged].wearpos1)
            assertEquals(Wearpos.Quiver.slot, deps.objs[objs.ghommals_lucky_penny].wearpos1)
        }

    @Test
    fun GameTestState.`crystal harpoon check reports carried and worn charge state`() =
        runFishingTest { deps ->
            prepare(level = 0)
            player.inv[3] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(12_345).pack())
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            assertMessageSent("The crystal harpoon has 12345 charges remaining.")
            assertEquals(12_345, CrystalHarpoonState.fromCharged(player.inv[3]!!).charges)

            prepare(level = 0)
            player.worn[Wearpos.RightHand.slot] = InvObj(objs.crystal_harpoon)
            player.withProtectedAccess {
                deps.worn.interact(this, player.worn, Wearpos.RightHand.slot, IfButtonOp.Op2)
            }
            assertMessageSent("The crystal harpoon has 10000 charges remaining.")
            assertEquals(objs.crystal_harpoon.id, player.worn[Wearpos.RightHand.slot]?.id)

            prepare(level = 99)
            player.members = false
            player.inv[3] = InvObj(objs.crystal_harpoon)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            assertMessageSent("You need to be on a members' world to check a crystal harpoon.")
        }

    @Test
    fun GameTestState.`crystal harpoon revert returns dragon harpoon and tool seed atomically`() =
        runFishingTest { deps ->
            for (harpoon in listOf(objs.crystal_harpoon, objs.crystal_harpoon_inactive)) {
                prepare(level = 0)
                player.inv[3] = InvObj(harpoon, vars = 12_345)
                player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op4) }
                assertEquals(objs.dragon_harpoon.id, player.inv[3]?.id)
                assertEquals(1, player.count(objs.crystal_tool_seed))
                assertEquals(0, player.count(harpoon))
            }

            prepare(level = 99)
            fillInventory()
            player.inv[3] = InvObj(objs.crystal_harpoon, vars = 12_345)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op4) }
            assertEquals(objs.crystal_harpoon.id, player.inv[3]?.id)
            assertEquals(12_345, player.inv[3]?.vars)
            assertEquals(0, player.count(objs.dragon_harpoon))
            assertEquals(0, player.count(objs.crystal_tool_seed))
            assertMessageSent(
                "You do not have enough inventory space to revert the crystal harpoon."
            )

            prepare(level = 99)
            player.members = false
            player.inv[3] = InvObj(objs.crystal_harpoon_inactive)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op4) }
            assertEquals(objs.crystal_harpoon_inactive.id, player.inv[3]?.id)
            assertMessageSent("You need to be on a members' world to revert a crystal harpoon.")
        }

    @Test
    fun GameTestState
        .`crystal shards charge active and inactive harpoons through quantity input`() =
        runFishingTest { deps ->
            prepare(level = 0)
            player.inv[3] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(19_850).pack())
            player.inv[4] = InvObj(objs.crystal_shard, 3)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.crystal_harpoon],
                    3,
                    deps.objs[objs.crystal_shard],
                    4,
                )
            }
            player.resumeCountDialog(10)
            advance()
            assertEquals(1, player.count(objs.crystal_shard))
            assertEquals(20_000, CrystalHarpoonState.fromCharged(player.inv[3]!!).charges)

            prepare(level = 0)
            player.inv[3] = InvObj(objs.crystal_shard, 3)
            player.inv[4] = InvObj(objs.crystal_harpoon_inactive)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.crystal_shard],
                    3,
                    deps.objs[objs.crystal_harpoon_inactive],
                    4,
                )
            }
            player.resumeCountDialog(2)
            advance()
            assertEquals(1, player.count(objs.crystal_shard))
            assertEquals(objs.crystal_harpoon.id, player.inv[4]?.id)
            assertEquals(200, CrystalHarpoonState.fromCharged(player.inv[4]!!).charges)
        }

    @Test
    fun GameTestState.`crystal shard charging rejects free full and stale inputs safely`() =
        runFishingTest { deps ->
            prepare(level = 99)
            player.members = false
            player.inv[3] = InvObj(objs.crystal_harpoon_inactive)
            player.inv[4] = InvObj(objs.crystal_shard, 2)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.crystal_harpoon_inactive],
                    3,
                    deps.objs[objs.crystal_shard],
                    4,
                )
            }
            assertEquals(objs.crystal_harpoon_inactive.id, player.inv[3]?.id)
            assertEquals(2, player.count(objs.crystal_shard))
            assertMessageSent("You need to be on a members' world to charge a crystal harpoon.")

            prepare(level = 99)
            player.inv[3] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(20_000).pack())
            player.inv[4] = InvObj(objs.crystal_shard, 2)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.crystal_harpoon],
                    3,
                    deps.objs[objs.crystal_shard],
                    4,
                )
            }
            assertEquals(2, player.count(objs.crystal_shard))
            assertMessageSent("The crystal harpoon is already fully charged.")

            prepare(level = 99)
            fillInventory()
            player.inv[3] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(1_000).pack())
            player.inv[4] = InvObj(objs.crystal_shard, 2)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.crystal_harpoon],
                    3,
                    deps.objs[objs.crystal_shard],
                    4,
                )
            }
            player.inv[3] = InvObj(objs.crystal_harpoon_inactive)
            player.resumeCountDialog(2)
            advance()
            assertEquals(objs.crystal_harpoon_inactive.id, player.inv[3]?.id)
            assertEquals(2, player.count(objs.crystal_shard))
        }

    @Test
    fun GameTestState.`cache 233 pins charged infernal check operations`() =
        runFishingTest { deps ->
            val charged = deps.crafting.rechargePairs.map { it.charged }
            assertEquals(
                listOf(
                    listOf(null, "Wield", "Check", null, null),
                    listOf("Wield", "Check", "Revert", "Change-Style", null),
                    listOf("Wield", "Check", "Revert", "Change-Style", null),
                ),
                charged.map { deps.objs[it].iop.toList() },
            )
            charged.forEach { obj ->
                assertEquals("Check", deps.objs[obj].paramOrNull(params.wear_op1))
            }
        }

    @Test
    fun GameTestState.`charged infernal check reports carried and worn state for every style`() =
        runFishingTest { deps ->
            for (charged in deps.crafting.rechargePairs.map { it.charged }) {
                prepare(level = 0)
                player.inv[3] = InvObj(charged, vars = InfernalHarpoonState(123).pack())
                val operation = if (charged == objs.infernal_harpoon) HeldOp.Op3 else HeldOp.Op2
                player.withProtectedAccess { deps.held.interact(this, player.inv, 3, operation) }
                assertMessageSent("The infernal harpoon has 123 charges remaining.")
                assertEquals(charged.id, player.inv[3]?.id)
                assertEquals(123, InfernalHarpoonState.fromCharged(player.inv[3]!!).charges)

                prepare(level = 0)
                player.worn[Wearpos.RightHand.slot] = InvObj(charged)
                player.withProtectedAccess {
                    deps.worn.interact(this, player.worn, Wearpos.RightHand.slot, IfButtonOp.Op2)
                }
                assertMessageSent("The infernal harpoon has 5000 charges remaining.")
                assertEquals(charged.id, player.worn[Wearpos.RightHand.slot]?.id)
            }

            prepare(level = 99)
            player.members = false
            player.inv[3] = InvObj(objs.infernal_harpoon)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            assertMessageSent("You need to be on a members' world to check an infernal harpoon.")
        }

    @Test
    fun GameTestState.`cache shifted ornamented wield operation equips every style`() =
        runFishingTest { deps ->
            val ornamented =
                deps.crafting.ornamentStyles.flatMap { listOf(it.trailblazer, it.reloaded) }
            for (harpoon in ornamented) {
                prepare(level = 0)
                player.inv[3] = InvObj(harpoon, vars = 246)
                player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op1) }
                assertEquals(null, player.inv[3])
                assertEquals(harpoon.id, player.worn[Wearpos.RightHand.slot]?.id)
                assertEquals(246, player.worn[Wearpos.RightHand.slot]?.vars)
            }

            prepare(level = 99)
            player.members = false
            player.inv[3] = InvObj(objs.infernal_harpoon_or)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op1) }
            assertEquals(objs.infernal_harpoon_or.id, player.inv[3]?.id)
            assertEquals(null, player.worn[Wearpos.RightHand.slot])
            assertMessageSent("You need to be on a members' world to wield an infernal harpoon.")
        }

    @Test
    fun GameTestState.`cache 233 pins infernal ornament inventory and worn operations`() =
        runFishingTest { deps ->
            val charged = listOf(objs.infernal_harpoon_or, objs.infernal_harpoon_or_reloaded)
            val depleted =
                listOf(
                    objs.infernal_harpoon_or_uncharged,
                    objs.infernal_harpoon_or_reloaded_uncharged,
                )
            charged.forEach { obj ->
                val type = deps.objs[obj]
                assertEquals(
                    listOf("Wield", "Check", "Revert", "Change-Style", null),
                    type.iop.toList(),
                )
                assertEquals("Check", type.paramOrNull(params.wear_op1))
                assertEquals("Change-Style", type.paramOrNull(params.wear_op2))
            }
            depleted.forEach { obj ->
                val type = deps.objs[obj]
                assertEquals(
                    listOf("Wield", null, "Revert", "Change-Style", null),
                    type.iop.toList(),
                )
                assertEquals("", type.paramOrNull(params.wear_op1))
                assertEquals("Change-Style", type.paramOrNull(params.wear_op2))
            }
            assertEquals(
                listOf(Triple(21031, 25059, 30342), Triple(21033, 25367, 30343)),
                deps.crafting.ornamentStyles.map {
                    Triple(it.plain.id, it.trailblazer.id, it.reloaded.id)
                },
            )
        }

    @Test
    fun GameTestState.`revert returns the plain harpoon and kit from every ornament style`() =
        runFishingTest { deps ->
            for (style in deps.crafting.ornamentStyles) {
                for (ornamented in listOf(style.trailblazer, style.reloaded)) {
                    prepare(level = 0)
                    player.stats[stats.cooking] = 0
                    player.inv[3] = InvObj(ornamented, vars = 123)

                    player.withProtectedAccess {
                        deps.held.interact(this, player.inv, 3, HeldOp.Op3)
                    }

                    assertEquals(style.plain.id, player.inv[3]?.id)
                    assertEquals(123, player.inv[3]?.vars)
                    assertEquals(1, player.count(objs.trailblazer_tool_ornament_kit))
                    assertEquals(0, player.statMap.getFineXP(stats.fishing))
                    assertEquals(0, player.statMap.getFineXP(stats.cooking))
                }
            }
        }

    @Test
    fun GameTestState.`revert is members only and atomic when inventory is full`() =
        runFishingTest { deps ->
            val style = deps.crafting.ornamentStyles.first()
            prepare(level = 99)
            player.members = false
            player.inv[3] = InvObj(style.trailblazer, vars = 321)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            assertEquals(style.trailblazer.id, player.inv[3]?.id)
            assertEquals(0, player.count(objs.trailblazer_tool_ornament_kit))
            assertMessageSent("You need to be on a members' world to revert an infernal harpoon.")

            prepare(level = 99)
            fillInventory()
            player.inv[3] = InvObj(style.reloaded, vars = 321)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            assertEquals(style.reloaded.id, player.inv[3]?.id)
            assertEquals(321, player.inv[3]?.vars)
            assertEquals(0, player.count(style.plain))
            assertEquals(0, player.count(objs.trailblazer_tool_ornament_kit))
            assertMessageSent("You do not have enough inventory space to remove the ornament kit.")
        }

    @Test
    fun GameTestState.`change style works both ways in a full inventory without losing state`() =
        runFishingTest { deps ->
            for (style in deps.crafting.ornamentStyles) {
                for ((source, target) in
                    listOf(
                        style.trailblazer to style.reloaded,
                        style.reloaded to style.trailblazer,
                    )) {
                    prepare(level = 0)
                    player.stats[stats.cooking] = 0
                    fillInventory()
                    player.inv[3] = InvObj(source, vars = 456)

                    player.withProtectedAccess {
                        deps.held.interact(this, player.inv, 3, HeldOp.Op4)
                    }

                    assertEquals(target.id, player.inv[3]?.id)
                    assertEquals(456, player.inv[3]?.vars)
                    assertEquals(0, player.statMap.getFineXP(stats.fishing))
                    assertEquals(0, player.statMap.getFineXP(stats.cooking))
                }
            }

            val style = deps.crafting.ornamentStyles.first()
            prepare(level = 99)
            player.members = false
            player.inv[3] = InvObj(style.trailblazer, vars = 789)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op4) }
            assertEquals(style.trailblazer.id, player.inv[3]?.id)
            assertEquals(789, player.inv[3]?.vars)
            assertMessageSent(
                "You need to be on a members' world to change an infernal harpoon's style."
            )
        }

    @Test
    fun GameTestState.`worn change style works both ways without losing state`() =
        runFishingTest { deps ->
            for (style in deps.crafting.ornamentStyles) {
                for ((source, target) in
                    listOf(
                        style.trailblazer to style.reloaded,
                        style.reloaded to style.trailblazer,
                    )) {
                    prepare(level = 0)
                    player.worn[Wearpos.RightHand.slot] = InvObj(source, vars = 987)

                    player.withProtectedAccess {
                        deps.worn.interact(
                            this,
                            player.worn,
                            Wearpos.RightHand.slot,
                            IfButtonOp.Op3,
                        )
                    }

                    assertEquals(target.id, player.worn[Wearpos.RightHand.slot]?.id)
                    assertEquals(987, player.worn[Wearpos.RightHand.slot]?.vars)
                }
            }
        }

    @Test
    fun GameTestState.`cache 233 infernal harpoon creation identities and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(13233, objs.smouldering_stone.id)
            assertEquals(25090, objs.trailblazer_tool_ornament_kit.id)
            assertEquals(21028, objs.dragon_harpoon.id)
            assertEquals(21031, objs.infernal_harpoon.id)
            assertEquals(75, InfernalHarpoonCrafting.FISHING_LEVEL)
            assertEquals(85, InfernalHarpoonCrafting.COOKING_LEVEL)
            assertEquals(200.0, InfernalHarpoonCrafting.FISHING_XP)
            assertEquals(350.0, InfernalHarpoonCrafting.COOKING_XP)
            assertTrue(
                deps.events.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.dragon_harpoon.id, objs.smouldering_stone.id),
                )
            )
            assertEquals(
                listOf(21033 to 21031, 25367 to 25059, 30343 to 30342),
                deps.crafting.rechargePairs.map { it.depleted.id to it.charged.id },
            )
            deps.crafting.rechargePairs.forEach { recharge ->
                listOf(objs.smouldering_stone, objs.dragon_harpoon).forEach { material ->
                    assertTrue(
                        deps.events.contains(
                            HeldUEvents.Type::class.java,
                            EventBus.composeLongKey(recharge.depleted.id, material.id),
                        )
                    )
                }
            }
            assertEquals(
                listOf(21031 to 25059, 21033 to 25367),
                deps.crafting.ornamentPairs.map { it.plain.id to it.ornamented.id },
            )
            deps.crafting.ornamentPairs.forEach { ornament ->
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(
                            ornament.plain.id,
                            objs.trailblazer_tool_ornament_kit.id,
                        ),
                    )
                )
            }
        }

    @Test
    fun GameTestState
        .`dragon harpoon and smouldering stone create a full infernal in either order`() =
        runFishingTest { deps ->
            repeat(2) { reverse ->
                prepare(level = 75)
                player.stats[stats.cooking] = 85
                player.statMap.setFineXP(stats.cooking, 0)
                player.inv[3] = InvObj(objs.dragon_harpoon)
                player.inv[8] = InvObj(objs.smouldering_stone)
                for (slot in player.inv.indices) {
                    if (slot != 3 && slot != 8) {
                        player.inv[slot] = InvObj(objs.coins)
                    }
                }

                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.dragon_harpoon],
                            3,
                            deps.objs[objs.smouldering_stone],
                            8,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.smouldering_stone],
                            8,
                            deps.objs[objs.dragon_harpoon],
                            3,
                        )
                    }
                }

                assertEquals(objs.infernal_harpoon.id, player.inv[3]?.id, "reverse=$reverse")
                assertEquals(null, player.inv[8], "reverse=$reverse")
                assertEquals(
                    InfernalHarpoonState.MAX_CHARGES,
                    InfernalHarpoonState.fromCharged(player.inv[3]!!).charges,
                    "reverse=$reverse",
                )
                assertEquals(2000, player.statMap.getFineXP(stats.fishing), "reverse=$reverse")
                assertEquals(3500, player.statMap.getFineXP(stats.cooking), "reverse=$reverse")
            }
        }

    @Test
    fun GameTestState.`infernal creation uses unboosted levels and preserves invalid inputs`() =
        runFishingTest { deps ->
            prepare(level = 75)
            player.members = false
            player.stats[stats.cooking] = 85
            player.statMap.setFineXP(stats.cooking, 0)
            add(objs.dragon_harpoon)
            add(objs.smouldering_stone)
            val membersDragonSlot = player.inv.indexOfFirst { it?.id == objs.dragon_harpoon.id }
            val membersStoneSlot = player.inv.indexOfFirst { it?.id == objs.smouldering_stone.id }
            player.withProtectedAccess {
                deps.crafting.create(this, membersDragonSlot, membersStoneSlot)
            }
            assertEquals(1, player.count(objs.dragon_harpoon))
            assertEquals(1, player.count(objs.smouldering_stone))

            prepare(level = 74)
            player.stats[stats.cooking] = 85
            player.statMap.setFineXP(stats.cooking, 0)
            add(objs.dragon_harpoon)
            add(objs.smouldering_stone)
            val dragonSlot = player.inv.indexOfFirst { it?.id == objs.dragon_harpoon.id }
            val stoneSlot = player.inv.indexOfFirst { it?.id == objs.smouldering_stone.id }
            player.stats.setCurrentLevel(stats.fishing, 75)
            player.withProtectedAccess { deps.crafting.create(this, dragonSlot, stoneSlot) }
            assertEquals(1, player.count(objs.dragon_harpoon))
            assertEquals(1, player.count(objs.smouldering_stone))

            prepare(level = 75)
            player.stats[stats.cooking] = 84
            player.statMap.setFineXP(stats.cooking, 0)
            add(objs.dragon_harpoon)
            add(objs.smouldering_stone)
            val secondDragonSlot = player.inv.indexOfFirst { it?.id == objs.dragon_harpoon.id }
            val secondStoneSlot = player.inv.indexOfFirst { it?.id == objs.smouldering_stone.id }
            player.stats.setCurrentLevel(stats.cooking, 85)
            player.withProtectedAccess {
                deps.crafting.create(this, secondDragonSlot, secondStoneSlot)
            }
            assertEquals(1, player.count(objs.dragon_harpoon))
            assertEquals(1, player.count(objs.smouldering_stone))

            prepare(level = 75)
            player.stats[stats.cooking] = 85
            player.statMap.setFineXP(stats.cooking, 0)
            add(objs.dragon_harpoon)
            add(objs.smouldering_stone)
            val thirdDragonSlot = player.inv.indexOfFirst { it?.id == objs.dragon_harpoon.id }
            val thirdStoneSlot = player.inv.indexOfFirst { it?.id == objs.smouldering_stone.id }
            player.inv[thirdStoneSlot] = InvObj(objs.coins)
            player.withProtectedAccess {
                deps.crafting.create(this, thirdDragonSlot, thirdStoneSlot)
            }
            assertEquals(1, player.count(objs.dragon_harpoon))
            assertEquals(0, player.count(objs.infernal_harpoon))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState
        .`every depleted infernal style recharges with either material in either order`() =
        runFishingTest { deps ->
            for (recharge in deps.crafting.rechargePairs) {
                for (material in listOf(objs.smouldering_stone, objs.dragon_harpoon)) {
                    repeat(2) { reverse ->
                        prepare(level = 0)
                        player.stats[stats.cooking] = 0
                        player.inv[3] = InvObj(recharge.depleted)
                        player.inv[8] = InvObj(material)
                        for (slot in player.inv.indices) {
                            if (slot != 3 && slot != 8) {
                                player.inv[slot] = InvObj(objs.coins)
                            }
                        }

                        player.withProtectedAccess {
                            if (reverse == 0) {
                                deps.heldU.interact(
                                    this,
                                    player.inv,
                                    deps.objs[recharge.depleted],
                                    3,
                                    deps.objs[material],
                                    8,
                                )
                            } else {
                                deps.heldU.interact(
                                    this,
                                    player.inv,
                                    deps.objs[material],
                                    8,
                                    deps.objs[recharge.depleted],
                                    3,
                                )
                            }
                        }

                        assertEquals(recharge.charged.id, player.inv[3]?.id)
                        assertEquals(null, player.inv[8])
                        assertEquals(
                            InfernalHarpoonState.MAX_CHARGES,
                            InfernalHarpoonState.fromCharged(player.inv[3]!!).charges,
                        )
                        assertEquals(0, player.statMap.getFineXP(stats.fishing))
                        assertEquals(0, player.statMap.getFineXP(stats.cooking))
                    }
                }
            }
        }

    @Test
    fun GameTestState.`infernal recharge is members only and preserves stale inputs`() =
        runFishingTest { deps ->
            val recharge = deps.crafting.rechargePairs.first()
            prepare(level = 99)
            player.members = false
            add(recharge.depleted)
            add(objs.smouldering_stone)
            val depletedSlot = player.inv.indexOfFirst { it?.id == recharge.depleted.id }
            val stoneSlot = player.inv.indexOfFirst { it?.id == objs.smouldering_stone.id }
            player.withProtectedAccess {
                deps.crafting.recharge(
                    this,
                    depletedSlot,
                    stoneSlot,
                    recharge,
                    objs.smouldering_stone,
                )
            }
            assertEquals(1, player.count(recharge.depleted))
            assertEquals(1, player.count(objs.smouldering_stone))
            assertMessageSent("You need to be on a members' world to recharge an infernal harpoon.")

            player.members = true
            player.inv[stoneSlot] = InvObj(objs.coins)
            player.withProtectedAccess {
                deps.crafting.recharge(
                    this,
                    depletedSlot,
                    stoneSlot,
                    recharge,
                    objs.smouldering_stone,
                )
            }
            assertEquals(1, player.count(recharge.depleted))
            assertEquals(0, player.count(recharge.charged))
        }

    @Test
    fun GameTestState.`trailblazer kit ornaments charged and depleted infernal harpoons`() =
        runFishingTest { deps ->
            for (ornament in deps.crafting.ornamentPairs) {
                repeat(2) { reverse ->
                    prepare(level = 0)
                    player.stats[stats.cooking] = 0
                    val charged = ornament.plain == objs.infernal_harpoon
                    val vars = if (charged) InfernalHarpoonState(123).pack() else 0
                    player.inv[3] = InvObj(ornament.plain, vars = vars)
                    player.inv[8] = InvObj(objs.trailblazer_tool_ornament_kit)
                    for (slot in player.inv.indices) {
                        if (slot != 3 && slot != 8) {
                            player.inv[slot] = InvObj(objs.coins)
                        }
                    }

                    player.withProtectedAccess {
                        if (reverse == 0) {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[ornament.plain],
                                3,
                                deps.objs[objs.trailblazer_tool_ornament_kit],
                                8,
                            )
                        } else {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[objs.trailblazer_tool_ornament_kit],
                                8,
                                deps.objs[ornament.plain],
                                3,
                            )
                        }
                    }

                    assertEquals(ornament.ornamented.id, player.inv[3]?.id)
                    assertEquals(null, player.inv[8])
                    assertEquals(vars, player.inv[3]?.vars)
                    if (charged) {
                        assertEquals(123, InfernalHarpoonState.fromCharged(player.inv[3]!!).charges)
                    }
                    assertEquals(0, player.statMap.getFineXP(stats.fishing))
                    assertEquals(0, player.statMap.getFineXP(stats.cooking))
                }
            }
        }

    @Test
    fun GameTestState.`infernal ornamenting is members only and preserves stale inputs`() =
        runFishingTest { deps ->
            val ornament = deps.crafting.ornamentPairs.first()
            prepare(level = 99)
            player.members = false
            add(ornament.plain)
            add(objs.trailblazer_tool_ornament_kit)
            val harpoonSlot = player.inv.indexOfFirst { it?.id == ornament.plain.id }
            val kitSlot =
                player.inv.indexOfFirst { it?.id == objs.trailblazer_tool_ornament_kit.id }
            player.withProtectedAccess {
                deps.crafting.applyOrnament(this, harpoonSlot, kitSlot, ornament)
            }
            assertEquals(1, player.count(ornament.plain))
            assertEquals(1, player.count(objs.trailblazer_tool_ornament_kit))
            assertMessageSent("You need to be on a members' world to ornament an infernal harpoon.")

            player.members = true
            player.inv[kitSlot] = InvObj(objs.coins)
            player.withProtectedAccess {
                deps.crafting.applyOrnament(this, harpoonSlot, kitSlot, ornament)
            }
            assertEquals(1, player.count(ornament.plain))
            assertEquals(0, player.count(ornament.ornamented))
        }

    @Test
    fun GameTestState.`cache 233 spots objects animations and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(
                listOf(
                    1514,
                    1517,
                    1518,
                    1521,
                    1523,
                    1524,
                    1525,
                    1528,
                    1530,
                    1544,
                    3913,
                    7155,
                    7459,
                    7462,
                    7467,
                    7469,
                    7947,
                    14038,
                    14040,
                    14041,
                    14524,
                ),
                SmallNetBaitFishingNpcs.all.map { it.id },
            )
            for (spot in SmallNetBaitFishingNpcs.all) {
                val type = deps.npcs[spot]
                assertTrue(type.op[0] == "Small Net" || type.op[0] == "Net", "spot=${spot.id}")
                assertEquals(null, type.op[1], "spot=${spot.id}")
                assertEquals("Bait", type.op[2], "spot=${spot.id}")
                assertEquals("I can see fish swimming in the water.", type.desc)
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, spot.id))
                assertTrue(deps.events.contains(NpcEvents.Op3::class.java, spot.id))
            }
            assertEquals(303, objs.small_fishing_net.id)
            assertEquals(307, objs.fishing_rod.id)
            assertEquals(313, objs.fishing_bait.id)
            assertEquals(317, objs.raw_shrimps.id)
            assertEquals(321, objs.raw_anchovies.id)
            assertEquals(327, objs.raw_sardine.id)
            assertEquals(345, objs.raw_herring.id)
            assertEquals(621, seqs.human_smallnet.id)
            assertEquals(622, seqs.human_fishing_casting.id)
            assertEquals(8188, seqs.human_fishing_casting_pearl.id)
            assertEquals(8189, seqs.human_fishing_casting_pearl_fly.id)
            assertNotNull(deps.objs[objs.small_fishing_net])
            assertNotNull(deps.seqs[seqs.human_smallnet])
            assertNotNull(deps.seqs[seqs.human_fishing_casting])
        }

    @Test
    fun GameTestState.`cache 233 lure bait spots objects animations and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(
                listOf(
                    394,
                    1506,
                    1507,
                    1508,
                    1509,
                    1512,
                    1513,
                    1515,
                    1516,
                    1526,
                    1527,
                    1529,
                    3417,
                    3418,
                    7463,
                    7464,
                    7468,
                    8524,
                    12774,
                    14036,
                    14521,
                    14522,
                    14525,
                    14526,
                    14527,
                    14528,
                ),
                LureBaitFishingNpcs.all.map { it.id },
            )
            for (spot in LureBaitFishingNpcs.all) {
                val type = deps.npcs[spot]
                assertEquals("Lure", type.op[0], "spot=${spot.id}")
                assertEquals(null, type.op[1], "spot=${spot.id}")
                assertEquals("Bait", type.op[2], "spot=${spot.id}")
                assertEquals("hidden", type.op[3], "spot=${spot.id}")
                assertEquals("hidden", type.op[4], "spot=${spot.id}")
                assertEquals("I can see fish swimming in the water.", type.desc, "spot=${spot.id}")
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, spot.id))
                assertTrue(deps.events.contains(NpcEvents.Op3::class.java, spot.id))
            }
            assertEquals(309, objs.fly_fishing_rod.id)
            assertEquals(314, objs.feather.id)
            assertEquals(331, objs.raw_salmon.id)
            assertEquals(335, objs.raw_trout.id)
            assertEquals(349, objs.raw_pike.id)
            assertEquals(10087, objs.stripy_feather.id)
            assertEquals(10138, objs.raw_rainbow_fish.id)
            assertEquals(22844, objs.pearl_fly_fishing_rod.id)
            assertNotNull(deps.objs[objs.fly_fishing_rod])
            assertNotNull(deps.objs[objs.pearl_fly_fishing_rod])
            assertNotNull(deps.seqs[seqs.human_fishing_casting_pearl_fly])
        }

    @Test
    fun GameTestState.`cache 233 cage spots lobster objects animation and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(
                listOf(
                    1510,
                    1519,
                    1522,
                    2146,
                    3657,
                    3914,
                    7199,
                    7460,
                    7465,
                    7470,
                    7946,
                    9173,
                    9174,
                    10515,
                    12777,
                    14039,
                ),
                CageHarpoonFishingNpcs.all.map { it.id },
            )
            for (spot in CageHarpoonFishingNpcs.all) {
                val type = deps.npcs[spot]
                assertEquals("Cage", type.op[0], "spot=${spot.id}")
                assertEquals(null, type.op[1], "spot=${spot.id}")
                assertEquals("Harpoon", type.op[2], "spot=${spot.id}")
                assertEquals("I can see fish swimming in the water.", type.desc, "spot=${spot.id}")
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, spot.id))
            }
            assertEquals(
                listOf(5821, 10514, 12776),
                CageHarpoonFishingNpcs.nonCageHarpoon.map { it.id },
            )
            for ((spot, expectedOp) in
                CageHarpoonFishingNpcs.nonCageHarpoon.zip(listOf("Net", "Net", "Big Net"))) {
                val type = deps.npcs[spot]
                assertEquals(expectedOp, type.op[0], "spot=${spot.id}")
                assertEquals(null, type.op[1], "spot=${spot.id}")
                assertEquals("Harpoon", type.op[2], "spot=${spot.id}")
                assertTrue(!deps.events.contains(NpcEvents.Op1::class.java, spot.id))
            }
            assertEquals(
                listOf(
                    1510,
                    1519,
                    1522,
                    2146,
                    3657,
                    3914,
                    5821,
                    7199,
                    7460,
                    7465,
                    7470,
                    7946,
                    9173,
                    9174,
                    10515,
                    12776,
                    12777,
                    14039,
                ),
                CageHarpoonFishingNpcs.tunaSwordfish.map { it.id },
            )
            for (spot in CageHarpoonFishingNpcs.tunaSwordfish) {
                assertEquals("Harpoon", deps.npcs[spot].op[2], "spot=${spot.id}")
                assertTrue(deps.events.contains(NpcEvents.Op3::class.java, spot.id))
            }
            assertTrue(!deps.events.contains(NpcEvents.Op3::class.java, 10514))
            assertEquals(301, objs.lobster_pot.id)
            assertEquals(377, objs.raw_lobster.id)
            assertEquals(619, seqs.human_lobster.id)
            assertEquals(311, objs.harpoon.id)
            assertEquals(359, objs.raw_tuna.id)
            assertEquals(371, objs.raw_swordfish.id)
            assertEquals(7991, objs.big_swordfish.id)
            assertEquals(10129, objs.barb_tail_harpoon.id)
            assertEquals(618, seqs.human_harpoon.id)
            assertEquals(5108, seqs.human_harpoon_barbed.id)
            assertNotNull(deps.objs[objs.lobster_pot])
            assertNotNull(deps.objs[objs.raw_lobster])
            assertNotNull(deps.seqs[seqs.human_lobster])
            assertNotNull(deps.objs[objs.harpoon])
            assertNotNull(deps.objs[objs.raw_tuna])
            assertNotNull(deps.objs[objs.raw_swordfish])
            assertNotNull(deps.objs[objs.big_swordfish])
            assertNotNull(deps.objs[objs.barb_tail_harpoon])
            assertNotNull(deps.seqs[seqs.human_harpoon])
            assertNotNull(deps.seqs[seqs.human_harpoon_barbed])
        }

    @Test
    fun GameTestState.`cache 233 big net harpoon spots shark objects and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(
                listOf(
                    1511,
                    1520,
                    3419,
                    3915,
                    4476,
                    4477,
                    5233,
                    5234,
                    5820,
                    7200,
                    7461,
                    7466,
                    8525,
                    8526,
                    8527,
                    9171,
                    9172,
                    12775,
                    14037,
                    14523,
                ),
                BigNetHarpoonFishingNpcs.all.map { it.id },
            )
            for (spot in BigNetHarpoonFishingNpcs.all) {
                val type = deps.npcs[spot]
                if (spot.id == 5820) {
                    assertEquals("Cage", type.op[0], "spot=${spot.id}")
                    assertTrue(!deps.events.contains(NpcEvents.Op1::class.java, spot.id))
                } else {
                    assertTrue(type.op[0] == "Big Net" || type.op[0] == "Net", "spot=${spot.id}")
                    assertTrue(
                        deps.events.contains(NpcEvents.Op1::class.java, spot.id),
                        "spot=${spot.id}",
                    )
                }
                assertEquals(null, type.op[1], "spot=${spot.id}")
                assertEquals("Harpoon", type.op[2], "spot=${spot.id}")
                assertEquals("I can see fish swimming in the water.", type.desc, "spot=${spot.id}")
                assertTrue(deps.events.contains(NpcEvents.Op3::class.java, spot.id))
            }
            assertEquals(
                BigNetHarpoonFishingNpcs.all.filterNot { it.id == 5820 },
                BigNetHarpoonFishingNpcs.bigNet,
            )
            assertEquals(listOf(1510, 1511), FishingGuildFishingNpcs.all.map { it.id })
            assertEquals(305, objs.big_fishing_net.id)
            assertEquals(341, objs.raw_cod.id)
            assertEquals(353, objs.raw_mackerel.id)
            assertEquals(363, objs.raw_bass.id)
            assertEquals(383, objs.raw_shark.id)
            assertEquals(401, objs.seaweed.id)
            assertEquals(405, objs.casket.id)
            assertEquals(407, objs.oyster.id)
            assertEquals(1059, objs.leather_gloves.id)
            assertEquals(1061, objs.leather_boots.id)
            assertEquals(7989, objs.big_bass.id)
            assertEquals(7993, objs.big_shark.id)
            assertEquals(620, seqs.human_bignet.id)
            assertNotNull(deps.objs[objs.big_fishing_net])
            assertNotNull(deps.objs[objs.raw_cod])
            assertNotNull(deps.objs[objs.raw_mackerel])
            assertNotNull(deps.objs[objs.raw_bass])
            assertNotNull(deps.objs[objs.raw_shark])
            assertNotNull(deps.objs[objs.seaweed])
            assertNotNull(deps.objs[objs.casket])
            assertNotNull(deps.objs[objs.oyster])
            assertNotNull(deps.objs[objs.leather_gloves])
            assertNotNull(deps.objs[objs.leather_boots])
            assertNotNull(deps.objs[objs.big_bass])
            assertNotNull(deps.objs[objs.big_shark])
            assertNotNull(deps.seqs[seqs.human_bignet])
        }

    @Test
    fun GameTestState
        .`cache 233 specialist harpoon variants animations levels and rates are pinned`() =
        runFishingTest { deps ->
            assertEquals(21028, objs.dragon_harpoon.id)
            assertEquals(25373, objs.dragon_harpoon_or.id)
            assertEquals(30349, objs.dragon_harpoon_or_reloaded.id)
            assertEquals(21031, objs.infernal_harpoon.id)
            assertEquals(21033, objs.infernal_harpoon_uncharged.id)
            assertEquals(25059, objs.infernal_harpoon_or.id)
            assertEquals(25367, objs.infernal_harpoon_or_uncharged.id)
            assertEquals(30342, objs.infernal_harpoon_or_reloaded.id)
            assertEquals(30343, objs.infernal_harpoon_or_reloaded_uncharged.id)
            assertEquals(23762, objs.crystal_harpoon.id)
            assertEquals(23764, objs.crystal_harpoon_inactive.id)
            assertEquals(7401, seqs.human_harpoon_dragon.id)
            assertEquals(7402, seqs.human_harpoon_infernal.id)
            assertEquals(8336, seqs.human_harpoon_crystal.id)
            assertEquals(8784, seqs.human_harpoon_trailblazer.id)
            assertEquals(88, seqs.human_harpoon_dragon_or.id)
            assertEquals(11867, seqs.human_harpoon_trailblazer_reloaded.id)
            assertEquals(11868, seqs.human_harpoon_dragon_or_reloaded.id)
            assertEquals(
                listOf(
                    ToolContract(23762, 8336, true, true, 71, 1.35),
                    ToolContract(21031, 7402, true, true, 75, 1.2),
                    ToolContract(25059, 8784, true, true, 75, 1.2),
                    ToolContract(30342, 11867, true, true, 75, 1.2),
                    ToolContract(21028, 7401, true, true, 61, 1.2),
                    ToolContract(25373, 88, true, true, 61, 1.2),
                    ToolContract(30349, 11868, true, true, 61, 1.2),
                    ToolContract(21033, 7402, true, true, 75, 1.2),
                    ToolContract(25367, 8784, true, true, 75, 1.2),
                    ToolContract(30343, 11867, true, true, 75, 1.2),
                    ToolContract(23764, 7401, true, true, 71, 1.2),
                    ToolContract(10129, 5108, true, true, 33, 1.0),
                    ToolContract(311, 618, false, false, 35, 1.0),
                ),
                StandardFishingMethods.harpoonTools.map(::toolContract),
            )
            assertEquals(38.0 / 256.0, adjustFishingSuccessRate(32.0 / 256.0, 1.2))
            assertEquals(43.0 / 256.0, adjustFishingSuccessRate(32.0 / 256.0, 1.35))
            val shark = StandardFishingMethods.shark.catches.single()
            assertEquals(32.0 / 256.0, fishingSuccessRate(shark, 76, invisibleBoost = 0))
            assertEquals(35.0 / 256.0, fishingSuccessRate(shark, 76, invisibleBoost = 7))
            assertEquals(
                42.0 / 256.0,
                adjustFishingSuccessRate(
                    fishingSuccessRate(shark, 76, invisibleBoost = 7),
                    multiplier = 1.2,
                ),
            )
            StandardFishingMethods.harpoonTools.forEach {
                assertNotNull(deps.objs[it.type])
                assertNotNull(deps.seqs[it.animation])
            }
            assertEquals(0..12, varobjs.infernal_tool_charges.bits)
            assertEquals(5_000, InfernalHarpoonState.MAX_CHARGES)
            assertEquals(
                listOf(21033, 25367, 30343),
                StandardFishingMethods.harpoonTools.mapNotNull { it.infernalDepletedType?.id },
            )
            assertEquals(
                5_000,
                InfernalHarpoonState.fromCharged(InvObj(objs.infernal_harpoon)).charges,
            )
            assertEquals(
                listOf(70.0, 50.0),
                StandardFishingMethods.harpoon.catches.map { it.infernalCookingXp },
            )
            assertEquals(105.0, StandardFishingMethods.shark.catches.single().infernalCookingXp)
            assertEquals(0..14, varobjs.crystal_tool_charges.bits)
            assertEquals(10_000, CrystalHarpoonState.INITIAL_CHARGES)
            assertEquals(20_000, CrystalHarpoonState.MAX_CHARGES)
            assertEquals(
                listOf(23764),
                StandardFishingMethods.harpoonTools.mapNotNull { it.crystalDepletedType?.id },
            )
            assertEquals(
                10_000,
                CrystalHarpoonState.fromCharged(InvObj(objs.crystal_harpoon)).charges,
            )
        }

    @Test
    fun GameTestState.`catch tables preserve levels xp rates order and messages`() =
        runFishingTest {
            assertEquals(
                listOf(
                    CatchContract(321, 15, 40.0, 24, 128, "You catch some raw anchovies."),
                    CatchContract(317, 1, 10.0, 48, 256, "You catch some raw shrimps."),
                ),
                StandardFishingMethods.smallNet.catches.map(::contract),
            )
            assertEquals(
                listOf(
                    CatchContract(345, 10, 30.0, 24, 128, "You catch a raw herring."),
                    CatchContract(327, 5, 20.0, 32, 192, "You catch a raw sardine."),
                ),
                StandardFishingMethods.bait.catches.map(::contract),
            )
            assertEquals(
                listOf(
                    CatchContract(331, 30, 70.0, 16, 96, "You catch a raw salmon"),
                    CatchContract(335, 20, 50.0, 32, 192, "You catch a raw trout"),
                ),
                StandardFishingMethods.fly.catches.map(::contract),
            )
            assertEquals(listOf(303), StandardFishingMethods.smallNet.tools.map { it.type.id })
            assertEquals(listOf(307, 22846), StandardFishingMethods.bait.tools.map { it.type.id })
            assertEquals(listOf(false, true), StandardFishingMethods.bait.tools.map { it.wearable })
            assertEquals(listOf(309, 22844), StandardFishingMethods.fly.tools.map { it.type.id })
            assertEquals(listOf(false, true), StandardFishingMethods.fly.tools.map { it.wearable })
            assertEquals(
                listOf(CatchContract(349, 25, 60.0, 16, 96, "You catch a pike.")),
                StandardFishingMethods.pike.catches.map(::contract),
            )
            assertEquals(5, StandardFishingMethods.pike.extraRollDelayChance)
            assertEquals(
                listOf(CatchContract(10138, 38, 80.0, 8, 64, "You catch a rainbow fish.")),
                StandardFishingMethods.rainbow.catches.map(::contract),
            )
            assertTrue(StandardFishingMethods.rainbow.membersOnly)
            assertEquals(
                listOf(CatchContract(377, 40, 90.0, 6, 95, "You catch a lobster.")),
                StandardFishingMethods.lobster.catches.map(::contract),
            )
            assertEquals(listOf(301), StandardFishingMethods.lobster.tools.map { it.type.id })
            assertEquals(
                listOf(
                    CatchContract(371, 50, 100.0, 4, 48, "You catch a raw swordfish."),
                    CatchContract(359, 35, 80.0, 8, 64, "You catch a raw tuna."),
                ),
                StandardFishingMethods.harpoon.catches.map(::contract),
            )
            assertEquals(StandardFishingMethods.harpoonTools, StandardFishingMethods.harpoon.tools)
            val bigSwordfish = StandardFishingMethods.harpoon.catches.first().rareReplacement
            assertNotNull(bigSwordfish)
            assertEquals(7991, bigSwordfish?.output?.id)
            assertEquals(2500, bigSwordfish?.chance)
            assertEquals("You catch an enormous swordfish!", bigSwordfish?.message)
            assertEquals(true, bigSwordfish?.membersOnly)
            assertEquals(
                listOf(CatchContract(383, 76, 110.0, 3, 40, "You catch a shark!")),
                StandardFishingMethods.shark.catches.map(::contract),
            )
            assertTrue(StandardFishingMethods.shark.membersOnly)
            assertEquals(StandardFishingMethods.harpoonTools, StandardFishingMethods.shark.tools)
            val bigShark = StandardFishingMethods.shark.catches.single().rareReplacement
            assertNotNull(bigShark)
            assertEquals(7993, bigShark?.output?.id)
            assertEquals(5000, bigShark?.chance)
            assertEquals("You catch an enormous shark!", bigShark?.message)
            assertEquals(true, bigShark?.membersOnly)
            assertEquals(
                listOf(
                    BigNetContract(1061, 16, 1.0, 10, 10, false, 0),
                    BigNetContract(401, 16, 1.0, 10, 10, false, 0),
                    BigNetContract(1059, 16, 1.0, 10, 10, false, 0),
                    BigNetContract(353, 16, 20.0, 5, 65, false, 0),
                    BigNetContract(353, 16, 20.0, 10, 10, false, 0),
                    BigNetContract(407, 16, 10.0, 3, 7, false, 0),
                    BigNetContract(405, 16, 10.0, 1, 2, false, 1),
                    BigNetContract(341, 23, 45.0, 4, 55, false, 0),
                    BigNetContract(363, 46, 100.0, 3, 40, true, 0),
                ),
                StandardFishingMethods.bigNetRolls.map(::bigNetContract),
            )
            assertEquals(
                listOf(11111, 11109, 11107, 11105, 11970, 11968),
                StandardFishingMethods.chargedSkillsNecklaces.map { it.id },
            )
            val bigBass = StandardFishingMethods.bigNetRolls.last().rareReplacement
            assertNotNull(bigBass)
            assertEquals(7989, bigBass?.output?.id)
            assertEquals(1000, bigBass?.chance)
            assertEquals("You catch an enormous bass!", bigBass?.message)
            assertEquals(true, bigBass?.membersOnly)
        }

    @Test
    fun GameTestState.`cache 233 Radas blessing identities and extra catch rates are pinned`() =
        runFishingTest { deps ->
            assertEquals(22941, objs.diary_radas_blessing_1.id)
            assertEquals(22943, objs.diary_radas_blessing_2.id)
            assertEquals(22945, objs.diary_radas_blessing_3.id)
            assertEquals(22947, objs.diary_radas_blessing_4.id)
            assertEquals(
                listOf(22947 to 8, 22945 to 6, 22943 to 4, 22941 to 2),
                StandardFishingMethods.radasBlessings.map { it.type.id to it.extraCatchPercent },
            )
            StandardFishingMethods.radasBlessings.forEach { assertNotNull(deps.objs[it.type]) }
            assertEquals(25588, objs.spirit_flakes.id)
            assertNotNull(deps.objs[objs.spirit_flakes])
        }

    @Test
    fun GameTestState.`worn Radas blessing doubles an ordinary catch without extra xp`() =
        runFishingTest { deps ->
            prepare(level = 1)
            add(objs.small_fishing_net)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 7
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            advance(5)

            assertEquals(2, player.count(objs.raw_shrimps))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))

            prepare(level = 1)
            add(objs.small_fishing_net)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 8
            val thresholdNpc = spawnSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, thresholdNpc, StandardFishingMethods.smallNet)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_shrimps))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`carried Radas blessing does not activate its catch bonus`() =
        runFishingTest { deps ->
            prepare(level = 1)
            add(objs.small_fishing_net)
            add(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_shrimps))
            assertEquals(1, player.count(objs.diary_radas_blessing_4))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`spirit flake doubles an ordinary bait catch without extra xp or bait`() =
        runFishingTest { deps ->
            prepare(level = 5)
            add(objs.fishing_rod)
            add(objs.fishing_bait, 2)
            add(objs.spirit_flakes, 2)
            random.next = 0
            random.then = 49
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.bait) }
            advance(5)

            assertEquals(2, player.count(objs.raw_sardine))
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(200, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`spirit flake is consumed when its extra catch roll fails`() =
        runFishingTest { deps ->
            prepare(level = 1)
            add(objs.small_fishing_net)
            add(objs.spirit_flakes, 2)
            random.next = 0
            random.then = 50
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_shrimps))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`spirit flakes and Radas blessing add to a fifty eight percent chance`() =
        runFishingTest { deps ->
            prepare(level = 1)
            add(objs.small_fishing_net)
            add(objs.spirit_flakes, 2)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 57
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            advance(5)

            assertEquals(2, player.count(objs.raw_shrimps))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`combined spirit flake and Radas chance excludes roll fifty eight`() =
        runFishingTest { deps ->
            prepare(level = 1)
            add(objs.small_fishing_net)
            add(objs.spirit_flakes, 2)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 58
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_shrimps))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`spirit flake duplicates the selected big fish replacement`() =
        runFishingTest { deps ->
            prepare(level = 50)
            add(objs.harpoon)
            add(objs.spirit_flakes, 2)
            random.next = 0
            queueRandom(0, 49)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(2, player.count(objs.big_swordfish))
            assertEquals(0, player.count(objs.raw_swordfish))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(1000, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`Radas blessing duplicates the selected big fish replacement`() =
        runFishingTest { deps ->
            prepare(level = 50)
            add(objs.harpoon)
            player.worn[0] = InvObj(objs.diary_radas_blessing_1)
            random.next = 0
            queueRandom(0, 1)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(2, player.count(objs.big_swordfish))
            assertEquals(0, player.count(objs.raw_swordfish))
            assertEquals(1000, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`Radas bonus preserves a base catch when only one slot is available`() =
        runFishingTest { deps ->
            prepare(level = 5)
            fillInventory()
            player.inv[0] = InvObj(objs.fishing_rod)
            player.inv[1] = InvObj(objs.fishing_bait)
            player.inv[2] = InvObj(objs.spirit_flakes, 2)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.bait) }
            advance(5)

            assertEquals(1, player.count(objs.raw_sardine))
            assertEquals(0, player.count(objs.fishing_bait))
            assertEquals(2, player.count(objs.spirit_flakes))
            assertEquals(200, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`Radas bonus catch consumes two crystal harpoon charges`() =
        runFishingTest { deps ->
            prepare(level = 71)
            player.inv[0] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(4).pack())
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            queueRandom(0, 1, 7)
            val chargedNpc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, chargedNpc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(2, player.count(objs.raw_swordfish))
            assertEquals(0, player.count(objs.big_swordfish))
            val harpoon = player.inv.first { it?.id == objs.crystal_harpoon.id }
            assertEquals(2, CrystalHarpoonState.fromCharged(harpoon!!).charges)
            assertEquals(1000, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`spirit flake extra fish has an independent infernal cooking roll`() =
        runFishingTest { deps ->
            prepare(level = 75)
            player.statMap.setFineXP(stats.cooking, 0)
            player.inv[0] = InvObj(objs.infernal_harpoon, vars = InfernalHarpoonState(5).pack())
            add(objs.spirit_flakes, 2)
            queueRandom(0, 1, 49, 0, 1)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_swordfish))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(1000, player.statMap.getFineXP(stats.fishing))
            assertEquals(700, player.statMap.getFineXP(stats.cooking))
            val harpoon = player.inv.first { it?.id == objs.infernal_harpoon.id }
            assertEquals(4, InfernalHarpoonState.fromCharged(harpoon!!).charges)
        }

    @Test
    fun GameTestState.`small net starts immediately but first successful roll is tick five`() =
        runFishingTest { deps ->
            prepare(level = 1)
            add(objs.small_fishing_net)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            assertEquals(seqs.human_smallnet.id, player.pendingSequence.id)
            advance(4)
            assertEquals(0, player.count(objs.raw_shrimps))
            advance()

            assertEquals(1, player.count(objs.raw_shrimps))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch some raw shrimps.")
        }

    @Test
    fun GameTestState.`eligible catches cascade from anchovy to shrimp`() = runFishingTest { deps ->
        prepare(level = 15)
        add(objs.small_fishing_net)
        random.next = 100
        random.then = 0
        val npc = spawnSpot(deps)

        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.smallNet) }
        advance(5)

        assertEquals(0, player.count(objs.raw_anchovies))
        assertEquals(1, player.count(objs.raw_shrimps))
        assertEquals(100, player.statMap.getFineXP(stats.fishing))
    }

    @Test
    fun GameTestState.`bait success consumes exactly one bait and awards the selected fish`() =
        runFishingTest { deps ->
            prepare(level = 10)
            add(objs.fishing_rod)
            add(objs.fishing_bait, 2)
            random.next = 0
            val npc = spawnSpot(deps)

            player.opNpc3(npc)
            advance(6)

            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1, player.count(objs.raw_herring))
            assertEquals(300, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch a raw herring.")
        }

    @Test
    fun GameTestState.`failed rolls consume no bait and repeat every five ticks`() =
        runFishingTest { deps ->
            prepare(level = 5)
            add(objs.fishing_rod)
            add(objs.fishing_bait, 2)
            random.next = 100
            random.then = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.bait) }
            advance(5)
            assertEquals(2, player.count(objs.fishing_bait))
            assertEquals(0, player.count(objs.raw_sardine))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))

            advance(5)
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1, player.count(objs.raw_sardine))
            assertEquals(200, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`worn pearl fishing rod is accepted`() = runFishingTest { deps ->
        prepare(level = 5)
        player.worn[0] = InvObj(objs.pearl_fishing_rod)
        add(objs.fishing_bait)
        random.next = 0
        val npc = spawnSpot(deps)

        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.bait) }
        assertEquals(seqs.human_fishing_casting_pearl.id, player.pendingSequence.id)
        advance(5)

        assertEquals(0, player.count(objs.fishing_bait))
        assertEquals(1, player.count(objs.raw_sardine))
        assertEquals(200, player.statMap.getFineXP(stats.fishing))
    }

    @Test
    fun GameTestState.`fly fishing cascades from salmon to trout and consumes one feather`() =
        runFishingTest { deps ->
            prepare(level = 30)
            add(objs.fly_fishing_rod)
            add(objs.feather, 2)
            random.next = 100
            random.then = 0
            val npc = spawnFreshSpot(deps)

            player.opNpc1(npc)
            advance(6)

            assertEquals(0, player.count(objs.raw_salmon))
            assertEquals(1, player.count(objs.raw_trout))
            assertEquals(1, player.count(objs.feather))
            assertEquals(500, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch a raw trout")
        }

    @Test
    fun GameTestState.`fly fishing catches salmon first and preserves its no-period message`() =
        runFishingTest { deps ->
            prepare(level = 30)
            add(objs.fly_fishing_rod)
            add(objs.feather)
            random.next = 0
            val npc = spawnFreshSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.fly) }
            advance(5)

            assertEquals(1, player.count(objs.raw_salmon))
            assertEquals(0, player.count(objs.feather))
            assertEquals(700, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch a raw salmon")
        }

    @Test
    fun GameTestState.`worn pearl fly rod uses its cache animation`() = runFishingTest { deps ->
        prepare(level = 20)
        player.worn[0] = InvObj(objs.pearl_fly_fishing_rod)
        add(objs.feather)
        random.next = 0
        val npc = spawnFreshSpot(deps)

        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.fly) }
        assertEquals(seqs.human_fishing_casting_pearl_fly.id, player.pendingSequence.id)
        advance(5)

        assertEquals(1, player.count(objs.raw_trout))
        assertEquals(0, player.count(objs.feather))
        assertEquals(500, player.statMap.getFineXP(stats.fishing))
    }

    @Test
    fun GameTestState.`pike has a one in five six-tick interval and consumes bait on success`() =
        runFishingTest { deps ->
            prepare(level = 25)
            add(objs.fishing_rod)
            add(objs.fishing_bait)
            random.next = 0
            random.then = 0
            val npc = spawnFreshSpot(deps)

            player.opNpc3(npc)
            advance(6)
            assertEquals(0, player.count(objs.raw_pike))
            assertEquals(1, player.count(objs.fishing_bait))
            advance()

            assertEquals(1, player.count(objs.raw_pike))
            assertEquals(0, player.count(objs.fishing_bait))
            assertEquals(600, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`stripy feathers select members-only rainbow fishing`() =
        runFishingTest { deps ->
            prepare(level = 38)
            add(objs.fly_fishing_rod)
            add(objs.stripy_feather)
            random.next = 0
            val npc = spawnFreshSpot(deps)

            player.opNpc1(npc)
            advance(6)

            assertEquals(1, player.count(objs.raw_rainbow_fish))
            assertEquals(0, player.count(objs.stripy_feather))
            assertEquals(800, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`rainbow fishing is rejected on free worlds without mutation`() =
        runFishingTest { deps ->
            prepare(level = 38)
            player.members = false
            add(objs.fly_fishing_rod)
            add(objs.stripy_feather)
            val npc = spawnFreshSpot(deps)

            player.withProtectedAccess { deps.fishing.lure(this, npc) }

            assertEquals(0, player.count(objs.raw_rainbow_fish))
            assertEquals(1, player.count(objs.stripy_feather))
            assertMessageSent("You need to be on a members' world to fish here.")
        }

    @Test
    fun GameTestState.`cage fishing routes through option one on a five-tick roll`() =
        runFishingTest { deps ->
            prepare(level = 40)
            add(objs.lobster_pot)
            random.next = 0
            val npc = spawnCageSpot(deps)

            player.opNpc1(npc)
            advance(5)
            assertEquals(0, player.count(objs.raw_lobster))
            advance()

            assertEquals(1, player.count(objs.raw_lobster))
            assertEquals(900, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch a lobster.")
        }

    @Test
    fun GameTestState.`cage fishing revalidates level tool distance and free capacity`() =
        runFishingTest { deps ->
            prepare(level = 39)
            val npc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lobster)
            }
            assertMessageSent("You need a Fishing level of 40 to fish here.")

            prepare(level = 40)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lobster)
            }
            assertMessageSent("You do not have the required equipment to fish here.")

            prepare(level = 40)
            fillInventory()
            player.inv[0] = InvObj(objs.lobster_pot)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lobster)
            }
            assertEquals(0, player.count(objs.raw_lobster))
            assertMessageSent("You do not have enough inventory space.")

            prepare(level = 40)
            add(objs.lobster_pot)
            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lobster)
            }
            player.teleport(player.coords.translate(10, 0))
            advance(5)
            assertEquals(0, player.count(objs.raw_lobster))
        }

    @Test
    fun GameTestState.`cache 233 pins barbarian hand fishing state and animation family`() =
        runFishingTest {
            assertEquals(3757, varbits.brut_fishing_s.id)
            assertEquals(6703, seqs.brut_player_hand_fishing_start.id)
            assertEquals(6704, seqs.brut_player_hand_fishing_ready.id)
            assertEquals(6705, seqs.brut_player_hand_fishing_end_shark_2.id)
            assertEquals(6706, seqs.brut_player_hand_fishing_end_shark_1.id)
            assertEquals(6707, seqs.brut_player_hand_fishing_end_swordfish_1.id)
            assertEquals(6708, seqs.brut_player_hand_fishing_end_swordfish_2.id)
            assertEquals(6709, seqs.brut_player_hand_fishing_end_blank.id)
            assertEquals(6710, seqs.brut_player_hand_fishing_end_tuna_1.id)
            assertEquals(6711, seqs.brut_player_hand_fishing_end_tuna_2.id)
        }

    @Test
    fun GameTestState.`barehanded tuna needs training boosted fishing and base strength`() =
        runFishingTest { deps ->
            prepare(level = 55)
            setStrength(level = 35)
            val npc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertMessageSent("You do not have the required equipment to fish here.")

            player.withProtectedAccess { vars[varbits.brut_fishing_s] = 1 }
            player.members = false
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertMessageSent("You do not have the required equipment to fish here.")

            player.members = true
            setStrength(level = 34, currentLevel = 99)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertMessageSent("You do not have the required equipment to fish here.")

            setStrength(level = 35)
            player.stats[stats.fishing] = 50
            player.stats.setCurrentLevel(stats.fishing, 55)
            player.statMap.setFineXP(stats.fishing, 0)
            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertEquals(seqs.brut_player_hand_fishing_end_blank.id, player.pendingSequence.id)
            advance(5)

            assertEquals(1, player.count(objs.raw_tuna))
            assertEquals(800, player.statMap.getFineXP(stats.fishing))
            assertEquals(80, player.statMap.getFineXP(stats.strength))
        }

    @Test
    fun GameTestState.`barehanded cascade applies per fish strength gates and xp`() =
        runFishingTest { deps ->
            prepare(level = 70)
            setStrength(level = 49)
            player.withProtectedAccess { vars[varbits.brut_fishing_s] = 3 }
            random.next = 0
            val tunaNpc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, tunaNpc, StandardFishingMethods.harpoon)
            }
            advance(5)
            assertEquals(1, player.count(objs.raw_tuna))
            assertEquals(80, player.statMap.getFineXP(stats.strength))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 70)
            setStrength(level = 50)
            player.withProtectedAccess { vars[varbits.brut_fishing_s] = 3 }
            queueRandom(0, 1)
            val swordfishNpc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, swordfishNpc, StandardFishingMethods.harpoon)
            }
            advance(5)
            assertEquals(1, player.count(objs.raw_swordfish))
            assertEquals(100, player.statMap.getFineXP(stats.strength))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 96)
            setStrength(level = 76)
            player.withProtectedAccess { vars[varbits.brut_fishing_s] = 3 }
            queueRandom(0, 1)
            val sharkNpc = spawnBigNetHarpoonSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, sharkNpc, StandardFishingMethods.shark)
            }
            advance(5)
            assertEquals(1, player.count(objs.raw_shark))
            assertEquals(110, player.statMap.getFineXP(stats.strength))
        }

    @Test
    fun GameTestState.`a harpoon takes priority and first barehanded catch advances training`() =
        runFishingTest { deps ->
            prepare(level = 55)
            setStrength(level = 35)
            player.withProtectedAccess { vars[varbits.brut_fishing_s] = 3 }
            add(objs.harpoon)
            random.next = 0
            val npc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertEquals(seqs.human_harpoon.id, player.pendingSequence.id)
            advance(5)

            assertEquals(1, player.count(objs.raw_swordfish))
            assertEquals(0, player.statMap.getFineXP(stats.strength))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 55)
            setStrength(level = 35)
            player.withProtectedAccess { vars[varbits.brut_fishing_s] = 1 }
            random.next = 0
            val trainingNpc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, trainingNpc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(2, player.vars[varbits.brut_fishing_s])
            assertMessageSent("You catch a raw tuna.")
            assertEquals(80, player.statMap.getFineXP(stats.strength))
        }

    @Test
    fun GameTestState.`cache 233 pins barbarian rod fishing spots tools bait and catches`() =
        runFishingTest { deps ->
            assertEquals(listOf(1542, 7323), BarbarianFishingNpcs.all.map { it.id })
            for (spot in BarbarianFishingNpcs.all) {
                val type = deps.npcs[spot]
                assertEquals("Use-rod", type.op[0], "spot=${spot.id}")
                assertEquals("I can see fish swimming in the water.", type.desc, "spot=${spot.id}")
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, spot.id))
            }
            assertEquals(3758, varbits.brut_fishing_r.id)
            assertEquals(11323, objs.barbarian_rod.id)
            assertEquals(22842, objs.pearl_barbarian_rod.id)
            assertEquals(11328, objs.leaping_trout.id)
            assertEquals(11330, objs.leaping_salmon.id)
            assertEquals(11332, objs.leaping_sturgeon.id)
            assertEquals(11334, objs.fish_offcuts.id)
            assertEquals(11324, objs.roe.id)
            assertEquals(11326, objs.caviar.id)
            assertEquals(9349, seqs.human_fishing_casting_barbarian.id)
            assertEquals(9350, seqs.human_fishing_onspot_barbarian.id)
            assertEquals(8190, seqs.human_fishing_casting_pearl_barbarian.id)
            assertEquals(8193, seqs.human_fishing_onspot_pearl_barbarian.id)
            assertEquals(
                listOf(11323, 22842),
                StandardFishingMethods.barbarianRod.tools.map { it.type.id },
            )
            assertEquals(
                listOf(9350, 8190),
                StandardFishingMethods.barbarianRod.tools.map { it.animation.id },
            )
            assertEquals(
                listOf(11334, 313, 314, 11324, 11326),
                StandardFishingMethods.barbarianRod.baits.map { it.id },
            )
            assertEquals(
                listOf(
                    CatchContract(11332, 70, 80.0, 8, 64, "You catch a leaping sturgeon."),
                    CatchContract(11330, 58, 70.0, 16, 96, "You catch a leaping salmon."),
                    CatchContract(11328, 48, 50.0, 32, 192, "You catch a leaping trout."),
                ),
                StandardFishingMethods.barbarianRod.catches.map(::contract),
            )
            assertEquals(
                listOf(45, 30, 15),
                StandardFishingMethods.barbarianRod.catches.map { it.baseStrengthLevel },
            )
            assertEquals(
                listOf(45, 30, 15),
                StandardFishingMethods.barbarianRod.catches.map { it.baseAgilityLevel },
            )
            assertEquals(
                listOf(7.0, 6.0, 5.0),
                StandardFishingMethods.barbarianRod.catches.map { it.strengthXp },
            )
            assertEquals(
                listOf(7.0, 6.0, 5.0),
                StandardFishingMethods.barbarianRod.catches.map { it.agilityXp },
            )
            assertTrue(StandardFishingMethods.barbarianRod.membersOnly)
            assertTrue(StandardFishingMethods.barbarianRod.requiresBarbarianRodTraining)
            assertNotNull(deps.objs[objs.barbarian_rod])
            assertNotNull(deps.objs[objs.pearl_barbarian_rod])
            assertNotNull(deps.seqs[seqs.human_fishing_onspot_barbarian])
            assertNotNull(deps.seqs[seqs.human_fishing_casting_pearl_barbarian])
        }

    @Test
    fun GameTestState
        .`barbarian rod fishing requires training membership and base support levels`() =
        runFishingTest { deps ->
            prepare(level = 48)
            setStrength(level = 15)
            setAgility(level = 15)
            add(objs.barbarian_rod)
            add(objs.fishing_bait)
            val npc = spawnBarbarianSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.barbarianRod)
            }
            assertMessageSent("You must begin Barbarian Fishing training before you can fish here.")

            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 1 }
            player.members = false
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.barbarianRod)
            }
            assertMessageSent("You need to be on a members' world to fish here.")

            prepare(level = 48)
            setStrength(level = 14, currentLevel = 99)
            setAgility(level = 15)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 1 }
            add(objs.barbarian_rod)
            add(objs.fishing_bait)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.barbarianRod)
            }
            assertMessageSent("You need a Strength level of 15 to fish here.")

            prepare(level = 48)
            setStrength(level = 15)
            setAgility(level = 14, currentLevel = 99)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 1 }
            add(objs.barbarian_rod)
            add(objs.fishing_bait)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.barbarianRod)
            }
            assertMessageSent("You need an Agility level of 15 to fish here.")

            prepare(level = 43)
            player.stats.setCurrentLevel(stats.fishing, 48)
            setStrength(level = 15)
            setAgility(level = 15)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 3 }
            add(objs.barbarian_rod)
            add(objs.fishing_bait)
            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.barbarianRod)
            }
            assertEquals(seqs.human_fishing_onspot_barbarian.id, player.pendingSequence.id)
            advance(5)
            assertEquals(1, player.count(objs.leaping_trout))
        }

    @Test
    fun GameTestState.`barbarian rod cascade applies per fish support gates and exact xp`() =
        runFishingTest { deps ->
            prepare(level = 58)
            setStrength(level = 30)
            setAgility(level = 30)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 3 }
            add(objs.barbarian_rod)
            add(objs.feather)
            random.next = 0
            val salmonNpc = spawnBarbarianSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, salmonNpc, StandardFishingMethods.barbarianRod)
            }
            advance(5)
            assertEquals(1, player.count(objs.leaping_salmon))
            assertEquals(700, player.statMap.getFineXP(stats.fishing))
            assertEquals(60, player.statMap.getFineXP(stats.strength))
            assertEquals(60, player.statMap.getFineXP(stats.agility))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 70)
            setStrength(level = 44)
            setAgility(level = 44)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 3 }
            add(objs.barbarian_rod)
            add(objs.fishing_bait)
            random.next = 0
            val gatedNpc = spawnBarbarianSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, gatedNpc, StandardFishingMethods.barbarianRod)
            }
            advance(5)
            assertEquals(0, player.count(objs.leaping_sturgeon))
            assertEquals(1, player.count(objs.leaping_salmon))
            assertEquals(60, player.statMap.getFineXP(stats.strength))
            assertEquals(60, player.statMap.getFineXP(stats.agility))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 70)
            setStrength(level = 45)
            setAgility(level = 45)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 3 }
            add(objs.barbarian_rod)
            add(objs.fish_offcuts)
            random.next = 0
            val sturgeonNpc = spawnBarbarianSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, sturgeonNpc, StandardFishingMethods.barbarianRod)
            }
            advance(5)
            assertEquals(1, player.count(objs.leaping_sturgeon))
            assertEquals(800, player.statMap.getFineXP(stats.fishing))
            assertEquals(70, player.statMap.getFineXP(stats.strength))
            assertEquals(70, player.statMap.getFineXP(stats.agility))
        }

    @Test
    fun GameTestState.`barbarian fishing consumes bait by priority and atomically frees a slot`() =
        runFishingTest { deps ->
            prepare(level = 48)
            setStrength(level = 15)
            setAgility(level = 15)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 3 }
            add(objs.barbarian_rod)
            add(objs.fish_offcuts, 2)
            add(objs.fishing_bait, 2)
            add(objs.feather, 2)
            add(objs.roe, 2)
            add(objs.caviar, 2)
            random.next = 0
            val priorityNpc = spawnBarbarianSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, priorityNpc, StandardFishingMethods.barbarianRod)
            }
            advance(5)
            assertEquals(1, player.count(objs.fish_offcuts))
            assertEquals(2, player.count(objs.fishing_bait))
            assertEquals(2, player.count(objs.feather))
            assertEquals(2, player.count(objs.roe))
            assertEquals(2, player.count(objs.caviar))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 48)
            setStrength(level = 15)
            setAgility(level = 15)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 3 }
            fillInventory()
            player.inv[0] = InvObj(objs.barbarian_rod)
            player.inv[1] = InvObj(objs.caviar)
            random.next = 0
            val fullNpc = spawnBarbarianSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, fullNpc, StandardFishingMethods.barbarianRod)
            }
            advance(5)
            assertEquals(0, player.count(objs.caviar))
            assertEquals(1, player.count(objs.leaping_trout))
        }

    @Test
    fun GameTestState.`pearl barbarian rod and first catch advance the training state`() =
        runFishingTest { deps ->
            prepare(level = 48)
            setStrength(level = 15)
            setAgility(level = 15)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 1 }
            player.worn[Wearpos.RightHand.slot] = InvObj(objs.pearl_barbarian_rod)
            add(objs.roe)
            random.next = 0
            val npc = spawnBarbarianSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.barbarianRod)
            }
            assertEquals(seqs.human_fishing_casting_pearl_barbarian.id, player.pendingSequence.id)
            advance(5)

            assertEquals(2, player.vars[varbits.brut_fishing_r])
            assertEquals(1, player.count(objs.leaping_trout))
            assertEquals(0, player.count(objs.roe))
            assertEquals(500, player.statMap.getFineXP(stats.fishing))
            assertEquals(50, player.statMap.getFineXP(stats.strength))
            assertEquals(50, player.statMap.getFineXP(stats.agility))
            assertMessageSent("You catch a leaping trout.")
        }

    @Test
    fun GameTestState.`barbarian bonus fish uses one bait and awards support xp once`() =
        runFishingTest { deps ->
            prepare(level = 48)
            setStrength(level = 15)
            setAgility(level = 15)
            player.withProtectedAccess { vars[varbits.brut_fishing_r] = 3 }
            add(objs.barbarian_rod)
            add(objs.fish_offcuts, 2)
            add(objs.spirit_flakes, 2)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 57
            val npc = spawnBarbarianSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.barbarianRod)
            }
            advance(5)

            assertEquals(2, player.count(objs.leaping_trout))
            assertEquals(1, player.count(objs.fish_offcuts))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(500, player.statMap.getFineXP(stats.fishing))
            assertEquals(50, player.statMap.getFineXP(stats.strength))
            assertEquals(50, player.statMap.getFineXP(stats.agility))
        }

    @Test
    fun GameTestState.`cache 233 pins leaping fish gutting recipes sequence and dispatch`() =
        runFishingTest { deps ->
            assertEquals(6702, seqs.brut_human_knifeuse.id)
            assertEquals(104, fishing_queues.leapingFishGutting.id)
            assertEquals(
                listOf(
                    LeapingFishGuttingRecipe(objs.leaping_trout, objs.roe, 10.0, 150, 99, 1, 2),
                    LeapingFishGuttingRecipe(objs.leaping_salmon, objs.roe, 10.0, 80, 80, 3, 4),
                    LeapingFishGuttingRecipe(objs.leaping_sturgeon, objs.caviar, 15.0, 80, 80, 5, 6),
                ),
                LeapingFishGuttingRecipes.all,
            )
            for (recipe in LeapingFishGuttingRecipes.all) {
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(objs.knife.id, recipe.input.id),
                    )
                )
            }
            assertNotNull(deps.seqs[seqs.brut_human_knifeuse])
        }

    @Test
    fun GameTestState.`leaping fish gut success and offcut endpoints are exact`() =
        runFishingTest { deps ->
            val cases =
                listOf(
                    Triple(LeapingFishGuttingRecipes.trout, 99, objs.roe),
                    Triple(LeapingFishGuttingRecipes.salmon, 80, objs.roe),
                    Triple(LeapingFishGuttingRecipes.sturgeon, 80, objs.caviar),
                )
            for ((recipe, level, output) in cases) {
                prepare(level = 0)
                player.stats[stats.cooking] = level
                player.statMap.setFineXP(stats.cooking, 0)
                player.inv[3] = InvObj(objs.knife)
                player.inv[4] = InvObj(recipe.input)
                random.next = recipe.maximumNumerator - 1
                random.then = recipe.offcutNumerator - 1

                player.withProtectedAccess {
                    deps.heldU.interact(
                        this,
                        player.inv,
                        deps.objs[objs.knife],
                        3,
                        deps.objs[recipe.input],
                        4,
                    )
                }

                assertEquals(0, player.count(recipe.input))
                assertEquals(1, player.count(output))
                assertEquals(1, player.count(objs.fish_offcuts))
                assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.cooking))
                assertEquals(seqs.brut_human_knifeuse.id, player.pendingSequence.id)
            }

            assertTrue(guttingRollSucceeds(LeapingFishGuttingRecipes.trout, 1, 0))
            assertTrue(!guttingRollSucceeds(LeapingFishGuttingRecipes.trout, 1, 1))
            assertTrue(guttingRollSucceeds(LeapingFishGuttingRecipes.trout, 99, 98))
            assertTrue(!guttingRollSucceeds(LeapingFishGuttingRecipes.trout, 99, 99))
            assertTrue(guttingRollSucceeds(LeapingFishGuttingRecipes.salmon, 80, 79))
            assertTrue(guttingRollSucceeds(LeapingFishGuttingRecipes.sturgeon, 99, 79))
        }

    @Test
    fun GameTestState.`failed gutting destroys the fish without output or cooking xp`() =
        runFishingTest { deps ->
            prepare(level = 0)
            player.stats[stats.cooking] = 1
            player.statMap.setFineXP(stats.cooking, 0)
            player.inv[3] = InvObj(objs.knife)
            player.inv[4] = InvObj(objs.leaping_salmon)
            random.next = 1

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.knife],
                    3,
                    deps.objs[objs.leaping_salmon],
                    4,
                )
            }

            assertEquals(0, player.count(objs.leaping_salmon))
            assertEquals(0, player.count(objs.roe))
            assertEquals(0, player.count(objs.fish_offcuts))
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState.`leaping fish auto gut one same type every three ticks`() =
        runFishingTest { deps ->
            prepare(level = 0)
            player.stats[stats.cooking] = 99
            player.statMap.setFineXP(stats.cooking, 0)
            player.inv[3] = InvObj(objs.knife)
            add(objs.leaping_trout, 3)
            add(objs.leaping_salmon)
            random.next = 0
            queueRandom(1, 0, 1, 0, 1)
            val fishSlot = player.inv.indexOfFirst { it?.id == objs.leaping_trout.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.knife],
                    3,
                    deps.objs[objs.leaping_trout],
                    fishSlot,
                )
            }
            assertEquals(2, player.count(objs.leaping_trout))
            advance(3)
            assertEquals(1, player.count(objs.leaping_trout))
            advance(3)

            assertEquals(0, player.count(objs.leaping_trout))
            assertEquals(1, player.count(objs.leaping_salmon))
            assertEquals(3, player.count(objs.roe))
            assertEquals(300, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState.`leaping gutting revalidates membership knife and bounded capacity`() =
        runFishingTest { deps ->
            prepare(level = 0)
            player.members = false
            player.inv[3] = InvObj(objs.knife)
            player.inv[4] = InvObj(objs.leaping_trout)
            player.withProtectedAccess { deps.gutting.start(this, firstSlot = 3, secondSlot = 4) }
            assertEquals(1, player.count(objs.leaping_trout))
            assertMessageSent("You need to be on a members' world to gut this fish.")

            prepare(level = 0)
            player.stats[stats.cooking] = 99
            player.statMap.setFineXP(stats.cooking, 0)
            player.inv[3] = InvObj(objs.knife)
            add(objs.leaping_trout, 2)
            random.next = 0
            random.then = 1
            val fishSlot = player.inv.indexOfFirst { it?.id == objs.leaping_trout.id }
            player.withProtectedAccess {
                deps.gutting.start(this, firstSlot = 3, secondSlot = fishSlot)
            }
            player.inv[3] = null
            advance(3)
            assertEquals(1, player.count(objs.leaping_trout))
            assertEquals(100, player.statMap.getFineXP(stats.cooking))

            prepare(level = 0)
            player.stats[stats.cooking] = 99
            player.statMap.setFineXP(stats.cooking, 0)
            fillInventory()
            player.inv[0] = InvObj(objs.knife)
            player.inv[1] = InvObj(objs.leaping_trout)
            random.next = 0
            random.then = 0
            player.withProtectedAccess { deps.gutting.start(this, firstSlot = 0, secondSlot = 1) }
            assertEquals(1, player.count(objs.roe))
            assertEquals(0, player.count(objs.fish_offcuts))
            assertEquals(100, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState.`harpoon option three catches tuna at level thirty five`() =
        runFishingTest { deps ->
            prepare(level = 35)
            add(objs.harpoon)
            random.next = 0
            val npc = spawnCageSpot(deps)

            player.opNpc3(npc)
            advance(6)

            assertEquals(1, player.count(objs.raw_tuna))
            assertEquals(0, player.count(objs.raw_swordfish))
            assertEquals(800, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch a raw tuna.")
        }

    @Test
    fun GameTestState.`harpoon cascade rolls swordfish before tuna`() = runFishingTest { deps ->
        prepare(level = 50)
        add(objs.harpoon)
        random.next = 100
        random.then = 0
        val npc = spawnCageSpot(deps)

        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.harpoon) }
        advance(5)

        assertEquals(0, player.count(objs.raw_swordfish))
        assertEquals(1, player.count(objs.raw_tuna))
        assertEquals(800, player.statMap.getFineXP(stats.fishing))
    }

    @Test
    fun GameTestState.`successful swordfish rolls the members big fish replacement`() =
        runFishingTest { deps ->
            prepare(level = 50)
            add(objs.harpoon)
            random.next = 0
            random.then = 0
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(0, player.count(objs.raw_swordfish))
            assertEquals(1, player.count(objs.big_swordfish))
            assertEquals(1000, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch an enormous swordfish!")

            prepare(level = 50)
            player.members = false
            add(objs.harpoon)
            random.next = 0
            random.then = 0
            val freeNpc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, freeNpc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_swordfish))
            assertEquals(0, player.count(objs.big_swordfish))
            assertMessageSent("You catch a raw swordfish.")
        }

    @Test
    fun GameTestState.`worn barb tail harpoon has its own members animation`() =
        runFishingTest { deps ->
            prepare(level = 50)
            player.worn[0] = InvObj(objs.barb_tail_harpoon)
            random.next = 0
            random.then = 1
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertEquals(seqs.human_harpoon_barbed.id, player.pendingSequence.id)
            advance(5)
            assertEquals(1, player.count(objs.raw_swordfish))
        }

    @Test
    fun GameTestState.`free worlds reject the members barb tail harpoon`() =
        runFishingTest { deps ->
            prepare(level = 35)
            player.members = false
            player.worn[0] = InvObj(objs.barb_tail_harpoon)
            val npc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertMessageSent("You do not have the required equipment to fish here.")
        }

    @Test
    fun GameTestState.`dragon harpoon applies its twenty percent catch rate`() =
        runFishingTest { deps ->
            prepare(level = 76)
            add(objs.dragon_harpoon)
            queueRandom(14, 1)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            assertEquals(seqs.human_harpoon_dragon.id, player.pendingSequence.id)
            advance(5)

            assertEquals(1, player.count(objs.raw_shark))
            assertEquals(1100, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`crystal harpoon has thirty five percent rate and tool priority`() =
        runFishingTest { deps ->
            prepare(level = 76)
            add(objs.dragon_harpoon)
            player.worn[0] = InvObj(objs.crystal_harpoon)
            queueRandom(16, 1)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            assertEquals(seqs.human_harpoon_crystal.id, player.pendingSequence.id)
            advance(5)

            assertEquals(1, player.count(objs.raw_shark))
            assertEquals(1100, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`inactive crystal harpoon retains dragon rate and animation`() =
        runFishingTest { deps ->
            prepare(level = 76)
            player.worn[0] = InvObj(objs.crystal_harpoon_inactive)
            queueRandom(14, 1)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            assertEquals(seqs.human_harpoon_dragon.id, player.pendingSequence.id)
            advance(5)

            assertEquals(1, player.count(objs.raw_shark))
        }

    @Test
    fun GameTestState.`infernal harpoon auto cooks tuna for half cooking xp and one charge`() =
        runFishingTest { deps ->
            prepare(level = 75)
            player.statMap.setFineXP(stats.cooking, 0)
            add(objs.infernal_harpoon)
            queueRandom(100, 0, 0)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(0, player.count(objs.raw_tuna))
            assertEquals(800, player.statMap.getFineXP(stats.fishing))
            assertEquals(500, player.statMap.getFineXP(stats.cooking))
            val harpoon = player.inv.first { it?.id == objs.infernal_harpoon.id }
            assertEquals(4_999, InfernalHarpoonState.fromCharged(harpoon!!).charges)
        }

    @Test
    fun GameTestState.`failed infernal auto cook keeps the fish and all charges`() =
        runFishingTest { deps ->
            prepare(level = 75)
            player.statMap.setFineXP(stats.cooking, 0)
            add(objs.infernal_harpoon)
            queueRandom(100, 0, 1)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_tuna))
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
            val harpoon = player.inv.first { it?.id == objs.infernal_harpoon.id }
            assertEquals(5_000, InfernalHarpoonState.fromCharged(harpoon!!).charges)
        }

    @Test
    fun GameTestState.`last worn infernal harpoon charge changes to its depleted style`() =
        runFishingTest { deps ->
            prepare(level = 75)
            player.statMap.setFineXP(stats.cooking, 0)
            player.worn[0] =
                InvObj(objs.infernal_harpoon_or_reloaded, vars = InfernalHarpoonState(1).pack())
            queueRandom(100, 0, 0)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(objs.infernal_harpoon_or_reloaded_uncharged.id, player.worn[0]?.id)
            assertEquals(500, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState.`big swordfish is kept and does not consume an infernal charge`() =
        runFishingTest { deps ->
            prepare(level = 75)
            player.statMap.setFineXP(stats.cooking, 0)
            player.inv[0] = InvObj(objs.infernal_harpoon, vars = InfernalHarpoonState(1).pack())
            queueRandom(0, 0)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(1, player.count(objs.big_swordfish))
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
            val harpoon = player.inv.first { it?.id == objs.infernal_harpoon.id }
            assertEquals(1, InfernalHarpoonState.fromCharged(harpoon!!).charges)
        }

    @Test
    fun GameTestState.`carried crystal harpoon consumes one charge for a caught fish`() =
        runFishingTest { deps ->
            prepare(level = 71)
            add(objs.crystal_harpoon)
            queueRandom(0, 1)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_swordfish))
            val harpoon = player.inv.first { it?.id == objs.crystal_harpoon.id }
            assertEquals(9_999, CrystalHarpoonState.fromCharged(harpoon!!).charges)
        }

    @Test
    fun GameTestState.`elven and celestial signets each save crystal harpoon charges`() =
        runFishingTest { deps ->
            val signets =
                listOf(objs.elven_signet, objs.celestial_signet, objs.celestial_signet_charged)
            for (signet in signets) {
                prepare(level = 71)
                player.inv[0] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(2).pack())
                player.worn[Wearpos.Ring.slot] = InvObj(signet)
                queueRandom(0, 1, 0)
                val npc = spawnCageSpot(deps)

                player.withProtectedAccess {
                    deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
                }
                advance(5)

                assertEquals(1, player.count(objs.raw_swordfish))
                val harpoon = player.inv.first { it?.id == objs.crystal_harpoon.id }
                assertEquals(2, CrystalHarpoonState.fromCharged(harpoon!!).charges)
                player.teleport(player.coords.translate(10, 0))
                advance(5)
            }

            prepare(level = 71)
            player.inv[0] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(2).pack())
            player.worn[Wearpos.Ring.slot] = InvObj(objs.elven_signet)
            queueRandom(0, 1, 1)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            val harpoon = player.inv.first { it?.id == objs.crystal_harpoon.id }
            assertEquals(1, CrystalHarpoonState.fromCharged(harpoon!!).charges)
        }

    @Test
    fun GameTestState.`lucky penny saves crystal and infernal harpoon charges`() =
        runFishingTest { deps ->
            prepare(level = 71)
            player.inv[0] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(2).pack())
            player.worn[Wearpos.Quiver.slot] = InvObj(objs.ghommals_lucky_penny)
            queueRandom(0, 1, 0)
            val crystalNpc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, crystalNpc, StandardFishingMethods.harpoon)
            }
            advance(5)

            val crystalHarpoon = player.inv.first { it?.id == objs.crystal_harpoon.id }
            assertEquals(2, CrystalHarpoonState.fromCharged(crystalHarpoon!!).charges)
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 75)
            player.statMap.setFineXP(stats.cooking, 0)
            player.inv[0] = InvObj(objs.infernal_harpoon, vars = InfernalHarpoonState(2).pack())
            player.worn[Wearpos.Quiver.slot] = InvObj(objs.ghommals_lucky_penny)
            queueRandom(100, 0, 0, 0)
            val infernalNpc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, infernalNpc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(0, player.count(objs.raw_tuna))
            assertEquals(500, player.statMap.getFineXP(stats.cooking))
            val infernalHarpoon = player.inv.first { it?.id == objs.infernal_harpoon.id }
            assertEquals(2, InfernalHarpoonState.fromCharged(infernalHarpoon!!).charges)
        }

    @Test
    fun GameTestState.`signet and lucky penny roll independently for every multiplied fish`() =
        runFishingTest { deps ->
            prepare(level = 71)
            player.inv[0] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(3).pack())
            add(objs.spirit_flakes, 2)
            player.worn[Wearpos.Ring.slot] = InvObj(objs.elven_signet)
            player.worn[Wearpos.Quiver.slot] = InvObj(objs.ghommals_lucky_penny)
            queueRandom(0, 1, 0, 0, 1, 1, 0)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(2, player.count(objs.raw_swordfish))
            assertEquals(1, player.count(objs.spirit_flakes))
            val harpoon = player.inv.first { it?.id == objs.crystal_harpoon.id }
            assertEquals(3, CrystalHarpoonState.fromCharged(harpoon!!).charges)
        }

    @Test
    fun GameTestState.`failed crystal harpoon attempt consumes no charge`() =
        runFishingTest { deps ->
            prepare(level = 76)
            add(objs.crystal_harpoon)
            random.next = 100
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            advance(5)

            assertEquals(0, player.count(objs.raw_shark))
            val harpoon = player.inv.first { it?.id == objs.crystal_harpoon.id }
            assertEquals(10_000, CrystalHarpoonState.fromCharged(harpoon!!).charges)
        }

    @Test
    fun GameTestState.`last worn crystal harpoon charge changes it to inactive`() =
        runFishingTest { deps ->
            prepare(level = 71)
            player.worn[0] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(1).pack())
            queueRandom(0, 1)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_swordfish))
            assertEquals(objs.crystal_harpoon_inactive.id, player.worn[0]?.id)
        }

    @Test
    fun GameTestState.`big swordfish consumes one crystal harpoon charge`() =
        runFishingTest { deps ->
            prepare(level = 71)
            player.inv[0] = InvObj(objs.crystal_harpoon, vars = CrystalHarpoonState(2).pack())
            queueRandom(0, 0)
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(1, player.count(objs.big_swordfish))
            val harpoon = player.inv.first { it?.id == objs.crystal_harpoon.id }
            assertEquals(1, CrystalHarpoonState.fromCharged(harpoon!!).charges)
        }

    @Test
    fun GameTestState.`fishing guild boost applies to the cage harpoon spot`() =
        runFishingTest { deps ->
            prepare(level = 35)
            add(objs.harpoon)
            random.next = 11
            val npc = spawnCageSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_tuna))
            assertEquals(800, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`fishing guild boost improves chance but does not unlock shark`() =
        runFishingTest { deps ->
            prepare(level = 69)
            add(objs.harpoon)
            val npc = spawnBigNetHarpoonSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            assertMessageSent("You need a Fishing level of 76 to fish here.")

            prepare(level = 76)
            add(objs.harpoon)
            queueRandom(13, 1)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_shark))
            assertEquals(1100, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`specialist level and selected tool are revalidated on result tick`() =
        runFishingTest { deps ->
            prepare(level = 60)
            add(objs.dragon_harpoon)
            val npc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertMessageSent("You do not have the required equipment to fish here.")

            player.stats.setCurrentLevel(stats.fishing, 61)
            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertEquals(seqs.human_harpoon_dragon.id, player.pendingSequence.id)
            player.inv[player.inv.indexOfFirst { it?.id == objs.dragon_harpoon.id }] =
                InvObj(objs.harpoon)
            advance(5)

            assertEquals(0, player.count(objs.raw_tuna))
            assertEquals(0, player.count(objs.raw_swordfish))
        }

    @Test
    fun GameTestState.`harpoon revalidates level tool and free capacity`() =
        runFishingTest { deps ->
            prepare(level = 34)
            add(objs.harpoon)
            val npc = spawnCageSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertMessageSent("You need a Fishing level of 35 to fish here.")

            prepare(level = 35)
            fillInventory()
            player.inv[0] = InvObj(objs.harpoon)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            assertMessageSent("You do not have enough inventory space.")

            prepare(level = 35)
            add(objs.harpoon)
            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.harpoon)
            }
            player.inv[player.inv.indexOfFirst { it?.id == objs.harpoon.id }] = null
            advance(5)
            assertEquals(0, player.count(objs.raw_tuna))
        }

    @Test
    fun GameTestState.`big net harpoon option three catches a shark at five ticks`() =
        runFishingTest { deps ->
            prepare(level = 76)
            add(objs.harpoon)
            random.next = 0
            random.then = 1
            val npc = spawnBigNetHarpoonSpot(deps)

            player.opNpc3(npc)
            advance(6)

            assertEquals(1, player.count(objs.raw_shark))
            assertEquals(0, player.count(objs.big_shark))
            assertEquals(1100, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch a shark!")
        }

    @Test
    fun GameTestState.`successful shark rolls the members big fish replacement`() =
        runFishingTest { deps ->
            prepare(level = 76)
            add(objs.harpoon)
            random.next = 0
            random.then = 0
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            advance(5)

            assertEquals(0, player.count(objs.raw_shark))
            assertEquals(1, player.count(objs.big_shark))
            assertEquals(1100, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch an enormous shark!")
        }

    @Test
    fun GameTestState.`shark fishing requires members and accepts a worn barb tail harpoon`() =
        runFishingTest { deps ->
            prepare(level = 76)
            player.members = false
            add(objs.harpoon)
            val npc = spawnBigNetHarpoonSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            assertMessageSent("You need to be on a members' world to fish here.")

            prepare(level = 76)
            player.worn[0] = InvObj(objs.barb_tail_harpoon)
            random.next = 0
            random.then = 1
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            assertEquals(seqs.human_harpoon_barbed.id, player.pendingSequence.id)
            advance(5)
            assertEquals(1, player.count(objs.raw_shark))
        }

    @Test
    fun GameTestState.`shark fishing revalidates level tool and free capacity`() =
        runFishingTest { deps ->
            prepare(level = 75)
            add(objs.harpoon)
            val npc = spawnBigNetHarpoonSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            assertMessageSent("You need a Fishing level of 76 to fish here.")

            prepare(level = 76)
            fillInventory()
            player.inv[0] = InvObj(objs.harpoon)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            assertMessageSent("You do not have enough inventory space.")

            prepare(level = 76)
            add(objs.harpoon)
            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.shark)
            }
            player.inv[player.inv.indexOfFirst { it?.id == objs.harpoon.id }] = null
            advance(5)
            assertEquals(0, player.count(objs.raw_shark))
        }

    @Test
    fun GameTestState.`big net option one performs all nine independent rolls in source order`() =
        runFishingTest { deps ->
            prepare(level = 99)
            add(objs.big_fishing_net)
            queueRandom(0, 0, 0, 0, 0, 0, 0, 0, 0, 1)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            assertEquals(seqs.human_bignet.id, player.pendingSequence.id)
            advance(5)
            assertEquals(1, player.count(objs.leather_boots))
            assertEquals(1, player.count(objs.seaweed))
            assertEquals(1, player.count(objs.leather_gloves))
            assertEquals(2, player.count(objs.raw_mackerel))
            assertEquals(1, player.count(objs.oyster))
            assertEquals(1, player.count(objs.casket))
            assertEquals(1, player.count(objs.raw_cod))
            assertEquals(1, player.count(objs.raw_bass))
            assertEquals(0, player.count(objs.big_bass))
            assertEquals(2080, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch some leather boots.")
            assertMessageSent("You catch some seaweed.")
            assertMessageSent("You catch some leather gloves.")
            assertMessageSent("You catch a raw mackerel.")
            assertMessageSent("You catch an oyster.")
            assertMessageSent("You catch a casket.")
            assertMessageSent("You catch a raw cod.")
            assertMessageSent("You catch a raw bass.")
        }

    @Test
    fun GameTestState.`big net keeps both mackerel rolls independent`() = runFishingTest { deps ->
        prepare(level = 16)
        add(objs.big_fishing_net)
        queueRandom(100, 100, 100, 100, 0, 100, 100)
        val npc = spawnBigNetHarpoonSpot(deps)

        player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
        advance(5)

        assertEquals(1, player.count(objs.raw_mackerel))
        assertEquals(200, player.statMap.getFineXP(stats.fishing))
        assertMessageSent("You catch a raw mackerel.")
    }

    @Test
    fun GameTestState.`spirit flake doubles a big net non-fish catch for no extra xp`() =
        runFishingTest { deps ->
            prepare(level = 16)
            add(objs.big_fishing_net)
            add(objs.spirit_flakes, 2)
            queueRandom(0, 100, 100, 100, 100, 100, 100, 49)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)

            assertEquals(2, player.count(objs.leather_boots))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(10, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch some leather boots.")
        }

    @Test
    fun GameTestState.`big net consumes a spirit flake when its multiplier roll fails`() =
        runFishingTest { deps ->
            prepare(level = 16)
            add(objs.big_fishing_net)
            add(objs.spirit_flakes, 2)
            queueRandom(0, 100, 100, 100, 100, 100, 100, 50)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)

            assertEquals(1, player.count(objs.leather_boots))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(10, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`big net reserves one spirit flake for every accepted primary catch`() =
        runFishingTest { deps ->
            prepare(level = 16)
            add(objs.big_fishing_net)
            add(objs.spirit_flakes, 2)
            queueRandom(0, 0, 100, 100, 100, 100, 100, 50, 50)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)

            assertEquals(1, player.count(objs.leather_boots))
            assertEquals(1, player.count(objs.seaweed))
            assertEquals(0, player.count(objs.spirit_flakes))
            assertEquals(20, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`Radas blessing doubles a big net non-fish catch`() = runFishingTest { deps ->
        prepare(level = 16)
        add(objs.big_fishing_net)
        player.worn[0] = InvObj(objs.diary_radas_blessing_4)
        queueRandom(0, 100, 100, 100, 100, 100, 100, 7)
        val npc = spawnBigNetHarpoonSpot(deps)

        player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
        advance(5)

        assertEquals(2, player.count(objs.leather_boots))
        assertEquals(10, player.statMap.getFineXP(stats.fishing))
    }

    @Test
    fun GameTestState.`big net multiplier fills capacity before later primary rolls`() =
        runFishingTest { deps ->
            prepare(level = 16)
            fillInventory()
            player.inv[0] = InvObj(objs.big_fishing_net)
            player.inv[1] = InvObj(objs.spirit_flakes, 2)
            player.inv[2] = null
            player.inv[3] = null
            queueRandom(0, 0, 100, 100, 100, 100, 100, 49)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)

            assertEquals(2, player.count(objs.leather_boots))
            assertEquals(0, player.count(objs.seaweed))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(10, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`fishing guild boost applies to big net rolls without unlocking products`() =
        runFishingTest { deps ->
            prepare(level = 16)
            add(objs.big_fishing_net)
            queueRandom(6, 6, 6, 6, 6, 6, 6)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)

            assertEquals(1, player.count(objs.raw_mackerel))
            assertEquals(0, player.count(objs.raw_cod))
            assertEquals(0, player.count(objs.raw_bass))
            assertEquals(200, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`worn charged skills necklace adds one casket chance point`() =
        runFishingTest { deps ->
            prepare(level = 16)
            add(objs.big_fishing_net)
            queueRandom(100, 100, 100, 100, 100, 100, 1)
            val npc = spawnBigNetHarpoonSpot(deps)
            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)
            assertEquals(0, player.count(objs.casket))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 16)
            add(objs.big_fishing_net)
            player.worn[0] = InvObj(objs.skills_necklace_6)
            queueRandom(100, 100, 100, 100, 100, 100, 1)
            val chargedNpc = spawnBigNetHarpoonSpot(deps)
            player.withProtectedAccess { deps.fishing.bigNet(this, chargedNpc) }
            advance(5)

            assertEquals(1, player.count(objs.casket))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`bass unlock uses base level and successful bass can become big`() =
        runFishingTest { deps ->
            prepare(level = 45)
            player.stats.setCurrentLevel(stats.fishing, 46)
            add(objs.big_fishing_net)
            queueRandom(100, 100, 100, 100, 100, 100, 100, 100)
            val npc = spawnBigNetHarpoonSpot(deps)
            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)
            assertEquals(0, player.count(objs.raw_bass))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 46)
            add(objs.big_fishing_net)
            queueRandom(100, 100, 100, 100, 100, 100, 100, 100, 0, 0)
            val eligibleNpc = spawnBigNetHarpoonSpot(deps)
            player.withProtectedAccess { deps.fishing.bigNet(this, eligibleNpc) }
            advance(5)

            assertEquals(0, player.count(objs.raw_bass))
            assertEquals(1, player.count(objs.big_bass))
            assertEquals(1000, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch an enormous bass!")
        }

    @Test
    fun GameTestState.`spirit flake duplicates a big bass replacement`() = runFishingTest { deps ->
        prepare(level = 46)
        add(objs.big_fishing_net)
        add(objs.spirit_flakes, 2)
        queueRandom(100, 100, 100, 100, 100, 100, 100, 100, 0, 0, 49)
        val npc = spawnBigNetHarpoonSpot(deps)

        player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
        advance(5)

        assertEquals(2, player.count(objs.big_bass))
        assertEquals(0, player.count(objs.raw_bass))
        assertEquals(1, player.count(objs.spirit_flakes))
        assertEquals(1000, player.statMap.getFineXP(stats.fishing))
        assertMessageSent("You catch an enormous bass!")
    }

    @Test
    fun GameTestState.`big net accepts rolled outputs only up to remaining capacity`() =
        runFishingTest { deps ->
            prepare(level = 99)
            fillInventory()
            player.inv[0] = InvObj(objs.big_fishing_net)
            player.inv[1] = null
            player.inv[2] = InvObj(objs.spirit_flakes, 2)
            queueRandom(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)

            assertEquals(1, player.count(objs.leather_boots))
            assertEquals(0, player.count(objs.seaweed))
            assertEquals(0, player.count(objs.raw_mackerel))
            assertEquals(2, player.count(objs.spirit_flakes))
            assertEquals(10, player.statMap.getFineXP(stats.fishing))
            assertEquals(65535, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`big net revalidates membership level tool distance and free capacity`() =
        runFishingTest { deps ->
            val npc = spawnBigNetHarpoonSpot(deps)
            prepare(level = 16)
            player.members = false
            add(objs.big_fishing_net)
            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            assertMessageSent("You need to be on a members' world to fish here.")

            prepare(level = 15)
            add(objs.big_fishing_net)
            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            assertMessageSent("You need a Fishing level of 16 to fish here.")

            prepare(level = 16)
            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            assertMessageSent("You do not have the required equipment to fish here.")

            prepare(level = 16)
            fillInventory()
            player.inv[0] = InvObj(objs.big_fishing_net)
            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            assertMessageSent("You do not have enough inventory space.")

            prepare(level = 16)
            add(objs.big_fishing_net)
            queueRandom(0, 0, 0, 0, 0, 0, 0)
            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            player.inv[player.inv.indexOfFirst { it?.id == objs.big_fishing_net.id }] = null
            advance(5)
            assertEquals(0, player.count(objs.leather_boots))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`open barrel accepts eligible big net fish from a full inventory`() =
        runFishingTest { deps ->
            prepare(level = 16)
            fillInventory()
            player.inv[0] = InvObj(objs.big_fishing_net)
            player.inv[1] = InvObj(objs.fish_barrel_open)
            queueRandom(0, 100, 100, 0, 100, 100, 100)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)

            assertEquals(0, player.count(objs.leather_boots))
            assertEquals(1, deps.barrel.count(player, objs.raw_mackerel))
            assertEquals(0, player.count(objs.raw_mackerel))
            assertEquals(200, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch a raw mackerel.")
        }

    @Test
    fun GameTestState.`big net doubled fish splits across the final barrel slot`() =
        runFishingTest { deps ->
            prepare(level = 16)
            add(objs.big_fishing_net)
            add(objs.fish_barrel_open)
            add(objs.spirit_flakes)
            assertEquals(27, deps.barrel.storeCaught(player, objs.raw_tuna, 27))
            queueRandom(100, 100, 100, 0, 100, 100, 100, 49)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)

            assertEquals(28, deps.barrel.count(player))
            assertEquals(1, deps.barrel.count(player, objs.raw_mackerel))
            assertEquals(1, player.count(objs.raw_mackerel))
            assertEquals(0, player.count(objs.spirit_flakes))
            assertEquals(200, player.statMap.getFineXP(stats.fishing))
            assertMessageSent(FishBarrelStorage.BARREL_FULL_MESSAGE)
        }

    @Test
    fun GameTestState.`big bass remains inventory only when a fish barrel is open`() =
        runFishingTest { deps ->
            prepare(level = 46)
            fillInventory()
            player.inv[0] = InvObj(objs.big_fishing_net)
            player.inv[1] = InvObj(objs.fish_barrel_open)
            player.inv[2] = null
            queueRandom(100, 100, 100, 100, 100, 100, 100, 100, 0, 0)
            val npc = spawnBigNetHarpoonSpot(deps)

            player.withProtectedAccess { deps.fishing.bigNet(this, npc) }
            advance(5)

            assertEquals(1, player.count(objs.big_bass))
            assertEquals(0, player.count(objs.raw_bass))
            assertEquals(0, deps.barrel.count(player))
            assertEquals(1000, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch an enormous bass!")
        }

    @Test
    fun GameTestState.`cache 233 pins fish barrel identities operations capacity and fish set`() =
        runFishingTest { deps ->
            assertEquals(25582, objs.fish_barrel_closed.id)
            assertEquals(25584, objs.fish_barrel_open.id)
            assertEquals(
                listOf("Fill", "Check", "Open", null, "Destroy"),
                deps.objs[objs.fish_barrel_closed].iop.toList(),
            )
            assertEquals(
                listOf("Fill", "Check", "Close", null, "Destroy"),
                deps.objs[objs.fish_barrel_open].iop.toList(),
            )
            val storageType = deps.invs[invs.fish_barrel_storage]
            assertEquals(28, storageType.size)
            assertEquals(InvStackType.Never, storageType.stack)
            assertEquals(
                listOf(
                    317,
                    327,
                    345,
                    321,
                    353,
                    335,
                    341,
                    349,
                    3379,
                    331,
                    359,
                    10138,
                    5001,
                    377,
                    363,
                    11328,
                    371,
                    2148,
                    11330,
                    7944,
                    3142,
                    11332,
                    383,
                    21293,
                    13439,
                    11934,
                    13339,
                    22826,
                    22829,
                    22832,
                    22835,
                    395,
                    389,
                ),
                FishBarrelStorage.accepted.map { it.id },
            )
        }

    @Test
    fun GameTestState.`fish sack and barrel assemble in either state without losing contents`() =
        runFishingTest { deps ->
            val contracts =
                listOf(
                    Triple(objs.fish_barrel_closed, objs.fish_sack_barrel_closed, false),
                    Triple(objs.fish_barrel_open, objs.fish_sack_barrel_open, true),
                )
            for ((barrel, combined, storesFish) in contracts) {
                prepare(level = 1)
                player.inv[3] = InvObj(barrel)
                player.inv[4] = InvObj(objs.fish_sack)
                if (storesFish) {
                    assertEquals(2, deps.barrel.storeCaught(player, objs.raw_tuna, 2))
                }
                player.withProtectedAccess {
                    deps.heldU.interact(
                        this,
                        player.inv,
                        deps.objs[objs.fish_sack],
                        4,
                        deps.objs[barrel],
                        3,
                    )
                }

                assertEquals(combined.id, player.inv[3]?.id)
                assertEquals(null, player.inv[4])
                assertEquals(0, player.count(barrel))
                assertEquals(0, player.count(objs.fish_sack))
                assertEquals(if (storesFish) 2 else 0, deps.barrel.count(player))
            }

            prepare(level = 1)
            player.members = false
            player.inv[3] = InvObj(objs.fish_barrel_closed)
            player.inv[4] = InvObj(objs.fish_sack)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.fish_barrel_closed],
                    3,
                    deps.objs[objs.fish_sack],
                    4,
                )
            }
            assertEquals(objs.fish_barrel_closed.id, player.inv[3]?.id)
            assertEquals(objs.fish_sack.id, player.inv[4]?.id)
            assertMessageSent("You need to be on a members' world to create a fish sack barrel.")
        }

    @Test
    fun GameTestState.`equipped open fish sack barrel stores catches and toggles in place`() =
        runFishingTest { deps ->
            prepare(level = 1)
            fillInventory()
            player.inv[0] = InvObj(objs.small_fishing_net)
            player.worn[Wearpos.Back.slot] = InvObj(objs.fish_sack_barrel_open)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            advance(5)

            assertEquals(1, deps.barrel.count(player, objs.raw_shrimps))
            assertEquals(0, player.count(objs.raw_shrimps))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
            player.teleport(player.coords.translate(10, 0))
            advance(5)
            player.withProtectedAccess {
                deps.worn.interact(this, player.worn, Wearpos.Back.slot, IfButtonOp.Op3)
            }
            assertEquals(objs.fish_sack_barrel_closed.id, player.worn[Wearpos.Back.slot]?.id)
            assertEquals(1, deps.barrel.count(player))
        }

    @Test
    fun GameTestState.`configure dismantles an open sack barrel and preserves storage`() =
        runFishingTest { deps ->
            prepare(level = 1)
            player.inv[3] = InvObj(objs.fish_sack_barrel_open)
            assertEquals(2, deps.barrel.storeCaught(player, objs.raw_tuna, 2))

            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            confirmChoice()

            assertEquals(objs.fish_barrel_open.id, player.inv[3]?.id)
            assertEquals(1, player.count(objs.fish_sack))
            assertEquals(2, deps.barrel.count(player, objs.raw_tuna))
        }

    @Test
    fun GameTestState.`configure dismantles a closed sack barrel and preserves storage`() =
        runFishingTest { deps ->
            prepare(level = 1)
            player.inv[3] = InvObj(objs.fish_sack_barrel_open)
            assertEquals(2, deps.barrel.storeCaught(player, objs.raw_tuna, 2))
            player.inv[3] = InvObj(objs.fish_sack_barrel_closed)

            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            confirmChoice()

            assertEquals(objs.fish_barrel_closed.id, player.inv[3]?.id)
            assertEquals(1, player.count(objs.fish_sack))
            assertEquals(2, deps.barrel.count(player, objs.raw_tuna))
        }

    @Test
    fun GameTestState.`fish sack barrel dismantle capacity and destroy are safe`() =
        runFishingTest { deps ->
            prepare(level = 1)
            fillInventory()
            player.inv[3] = InvObj(objs.fish_sack_barrel_closed)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op3) }
            confirmChoice()
            assertEquals(objs.fish_sack_barrel_closed.id, player.inv[3]?.id)
            assertEquals(0, player.count(objs.fish_sack))
            assertMessageSent(
                "You do not have enough inventory space to dismantle the fish sack barrel."
            )

            prepare(level = 1)
            add(objs.fish_sack_barrel_open)
            assertEquals(2, deps.barrel.storeCaught(player, objs.raw_tuna, 2))
            val slot = player.inv.indexOfFirst { it?.id == objs.fish_sack_barrel_open.id }
            val barrel = requireNotNull(player.inv[slot])
            assertTrue(
                deps.events.publish(
                    HeldDropEvents.Destroy(
                        player,
                        slot,
                        barrel,
                        deps.objs[objs.fish_sack_barrel_open],
                    )
                )
            )
            assertEquals(0, deps.barrel.count(player))
        }

    @Test
    fun GameTestState.`fish barrel fill toggle and membership gates preserve contents`() =
        runFishingTest { deps ->
            prepare(level = 1)
            add(objs.fish_barrel_closed)
            add(objs.raw_shrimps, 2)
            add(objs.raw_tuna)
            add(objs.coins)
            val slot = player.inv.indexOfFirst { it?.id == objs.fish_barrel_closed.id }

            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
            assertEquals(3, deps.barrel.count(player))
            assertEquals(2, deps.barrel.count(player, objs.raw_shrimps))
            assertEquals(1, deps.barrel.count(player, objs.raw_tuna))
            assertEquals(0, player.count(objs.raw_shrimps))
            assertEquals(1, player.count(objs.coins))

            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op3) }
            assertEquals(objs.fish_barrel_open.id, player.inv[slot]?.id)
            assertEquals(3, deps.barrel.count(player))
            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op3) }
            assertEquals(objs.fish_barrel_closed.id, player.inv[slot]?.id)

            player.members = false
            add(objs.raw_sardine)
            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
            assertEquals(3, deps.barrel.count(player))
            assertEquals(1, player.count(objs.raw_sardine))
            assertMessageSent("You need to be on a members' world to use a fish barrel.")
        }

    @Test
    fun GameTestState.`fish barrel bankside empty and destroy clear permanent storage`() =
        runFishingTest { deps ->
            prepare(level = 1)
            add(objs.fish_barrel_open)
            assertEquals(4, deps.barrel.storeCaught(player, objs.raw_lobster, 4))
            val slot = player.inv.indexOfFirst { it?.id == objs.fish_barrel_open.id }

            assertTrue(
                deps.events.publish(
                    HeldBanksideEvents.Type(player, slot, deps.objs[objs.fish_barrel_open])
                )
            )
            val bank = player.invMap.getOrPut(deps.invs[invs.bank])
            assertEquals(4, bank.count(deps.objs[objs.raw_lobster]))
            assertEquals(0, deps.barrel.count(player))

            assertEquals(2, deps.barrel.storeCaught(player, objs.raw_tuna, 2))
            val barrel = requireNotNull(player.inv[slot])
            assertTrue(
                deps.events.publish(
                    HeldDropEvents.Destroy(player, slot, barrel, deps.objs[objs.fish_barrel_open])
                )
            )
            assertEquals(0, deps.barrel.count(player))
        }

    @Test
    fun GameTestState.`open barrel accepts ordinary catches from a full inventory`() =
        runFishingTest { deps ->
            prepare(level = 1)
            fillInventory()
            player.inv[0] = InvObj(objs.small_fishing_net)
            player.inv[1] = InvObj(objs.fish_barrel_open)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            advance(5)

            assertEquals(1, deps.barrel.count(player, objs.raw_shrimps))
            assertEquals(0, player.count(objs.raw_shrimps))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 1)
            fillInventory()
            player.inv[0] = InvObj(objs.small_fishing_net)
            player.inv[1] = InvObj(objs.fish_barrel_closed)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            assertEquals(0, deps.barrel.count(player))
            assertMessageSent("You do not have enough inventory space.")
        }

    @Test
    fun GameTestState.`barrel splits a doubled catch and reports reaching capacity`() =
        runFishingTest { deps ->
            prepare(level = 1)
            add(objs.small_fishing_net)
            add(objs.fish_barrel_open)
            add(objs.spirit_flakes)
            assertEquals(27, deps.barrel.storeCaught(player, objs.raw_tuna, 27))
            random.next = 0
            random.then = 49
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.smallNet)
            }
            advance(5)

            assertEquals(28, deps.barrel.count(player))
            assertEquals(1, deps.barrel.count(player, objs.raw_shrimps))
            assertEquals(1, player.count(objs.raw_shrimps))
            assertEquals(0, player.count(objs.spirit_flakes))
            assertEquals(100, player.statMap.getFineXP(stats.fishing))
            assertMessageSent(FishBarrelStorage.BARREL_FULL_MESSAGE)
        }

    @Test
    fun GameTestState.`full inventory bait exchange is atomic`() = runFishingTest { deps ->
        prepare(level = 5)
        fillInventory()
        player.inv[0] = InvObj(objs.fishing_rod)
        player.inv[1] = InvObj(objs.fishing_bait, 1)
        random.next = 0
        val npc = spawnSpot(deps)
        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.bait) }
        advance(5)
        assertEquals(0, player.count(objs.fishing_bait))
        assertEquals(1, player.count(objs.raw_sardine))

        prepare(level = 5)
        fillInventory()
        player.inv[0] = InvObj(objs.fishing_rod)
        player.inv[1] = InvObj(objs.fishing_bait, 2)
        random.next = 0
        val secondNpc = spawnSpot(deps)
        player.withProtectedAccess {
            deps.fishing.fish(this, secondNpc, StandardFishingMethods.bait)
        }
        advance(5)
        assertEquals(2, player.count(objs.fishing_bait))
        assertEquals(0, player.count(objs.raw_sardine))
        assertEquals(0, player.statMap.getFineXP(stats.fishing))
    }

    @Test
    fun GameTestState.`equipment level and distance are revalidated`() = runFishingTest { deps ->
        prepare(level = 0)
        val npc = spawnSpot(deps)
        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.smallNet) }
        assertMessageSent("You need a Fishing level of 1 to fish here.")

        prepare(level = 1)
        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.smallNet) }
        assertMessageSent("You do not have the required equipment to fish here.")

        prepare(level = 1)
        add(objs.small_fishing_net)
        random.next = 0
        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.smallNet) }
        player.teleport(player.coords.translate(10, 0))
        advance(5)
        assertEquals(0, player.count(objs.raw_shrimps))
        assertEquals(0, player.statMap.getFineXP(stats.fishing))
    }

    private fun GameTestScope.prepare(level: Int) {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fishing] = level
        player.statMap.setFineXP(stats.fishing, 0)
        client.clear()
    }

    private fun GameTestScope.setStrength(level: Int, currentLevel: Int = level) {
        player.stats[stats.strength] = level
        player.stats.setCurrentLevel(stats.strength, currentLevel)
        player.statMap.setFineXP(stats.strength, 0)
    }

    private fun GameTestScope.setAgility(level: Int, currentLevel: Int = level) {
        player.stats[stats.agility] = level
        player.stats.setCurrentLevel(stats.agility, currentLevel)
        player.statMap.setFineXP(stats.agility, 0)
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        val type = objTypes[obj]
        if (type.stackable) {
            val slot = player.inv.indexOfFirst { it == null }
            player.inv[slot] = InvObj(obj, count)
        } else {
            repeat(count) {
                val slot = player.inv.indexOfFirst { it == null }
                player.inv[slot] = InvObj(obj)
            }
        }
    }

    private fun GameTestScope.fillInventory() {
        for (slot in player.inv.indices) {
            player.inv[slot] = InvObj(objs.coins)
        }
    }

    private fun GameTestScope.queueRandom(vararg values: Int) {
        values.forEach { random.then = it }
    }

    private fun GameTestScope.confirmChoice() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
        )
    }

    private fun GameTestScope.spawnSpot(deps: Dependencies): Npc =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[SmallNetBaitFishingNpcs.saltfish_50_49])

    private fun GameTestScope.spawnFreshSpot(deps: Dependencies): Npc =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[LureBaitFishingNpcs.freshfish_26_57])

    private fun GameTestScope.spawnCageSpot(deps: Dependencies): Npc =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[CageHarpoonFishingNpcs.rarefish_40_53])

    private fun GameTestScope.spawnBigNetHarpoonSpot(deps: Dependencies): Npc =
        spawnNpc(
            player.coords.translate(1, 0),
            deps.npcs[BigNetHarpoonFishingNpcs.memberfish_40_53],
        )

    private fun GameTestScope.spawnBarbarianSpot(deps: Dependencies): Npc =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[BarbarianFishingNpcs.ottosGrotto])

    private fun GameTestState.runFishingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            FishingScript::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val fishing: StandardFishing,
        val crystal: CrystalHarpoonLifecycle,
        val barrel: FishBarrelStorage,
        val gutting: LeapingFishGutting,
        val crafting: InfernalHarpoonCrafting,
        val events: EventBus,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
        val worn: WornInteractions,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val seqs: SeqTypeList,
        val invs: InvTypeList,
        val varbits: VarBitTypeList,
    )

    private data class CatchContract(
        val output: Int,
        val level: Int,
        val xp: Double,
        val low: Int,
        val high: Int,
        val message: String,
    )

    private data class BigNetContract(
        val output: Int,
        val level: Int,
        val xp: Double,
        val low: Int,
        val high: Int,
        val baseLevelRequired: Boolean,
        val casketChanceBonus: Int,
    )

    private data class ToolContract(
        val type: Int,
        val animation: Int,
        val wearable: Boolean,
        val membersOnly: Boolean,
        val level: Int,
        val catchRateMultiplier: Double,
    )

    private fun contract(catch: FishingCatch) =
        CatchContract(catch.output.id, catch.level, catch.xp, catch.low, catch.high, catch.message)

    private fun bigNetContract(roll: BigNetFishingRoll) =
        BigNetContract(
            roll.output.id,
            roll.level,
            roll.xp,
            roll.low,
            roll.high,
            roll.baseLevelRequired,
            roll.casketChanceBonus,
        )

    private fun toolContract(tool: FishingTool) =
        ToolContract(
            tool.type.id,
            tool.animation.id,
            tool.wearable,
            tool.membersOnly,
            tool.level,
            tool.catchRateMultiplier,
        )
}
