package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class InfernalPickaxeEffectsTest {
    @Test
    fun GameTestState.eachInfernalStyleConsumesTheExactSelectedCarriedToolAndDepletesOnLastProc() =
        game { d ->
            player.members = true
            for (pair in d.lifecycle.rechargePairs) {
                val tool = InvObj(pair.charged, vars = InfernalPickaxeState(1).pack())
                player.inv[5] = tool
                player.inv[6] = InvObj(pair.charged)
                val before = player.statMap.getFineXP(stats.smithing)
                random.next = 0
                player.withProtectedAccess {
                    assertTrue(d.lifecycle.combust(this, tool, objs.iron_ore, 15))
                }
                assertEquals(pair.depleted.id, player.inv[5]?.id)
                assertEquals(0, player.inv[6]?.vars)
                assertEquals(62, player.statMap.getFineXP(stats.smithing) - before)
            }
        }

    @Test
    fun GameTestState.wornToolLosesChargeOnlyWhenAResourceIsConsumed() = game { d ->
        player.members = true
        val tool = InvObj(MiningObjs.infernalPickaxe, vars = InfernalPickaxeState(2).pack())
        player.righthand = tool
        random.next = 1
        player.withProtectedAccess {
            assertFalse(d.lifecycle.combust(this, tool, objs.gold_ore, 40))
        }
        assertEquals(tool, player.righthand)
        random.next = 0
        player.withProtectedAccess {
            assertTrue(d.lifecycle.combust(this, tool, objs.gold_ore, 40))
        }
        assertEquals(1, InfernalPickaxeState.fromCharged(requireNotNull(player.righthand)).charges)
    }

    @Test
    fun GameTestState.missingNonmemberAndDepletedToolsCannotConsumeOrGrantXp() = game { d ->
        val tool = InvObj(MiningObjs.infernalPickaxe)
        val before = player.statMap.getFineXP(stats.smithing)
        player.members = true
        player.withProtectedAccess {
            assertFalse(d.lifecycle.combust(this, tool, objs.iron_ore, 15))
        }
        player.righthand = tool
        player.members = false
        player.withProtectedAccess {
            assertFalse(d.lifecycle.combust(this, tool, objs.iron_ore, 15))
        }
        player.members = true
        player.righthand = InvObj(MiningObjs.infernalPickaxeUncharged)
        player.withProtectedAccess {
            assertFalse(
                d.lifecycle.combust(this, requireNotNull(player.righthand), objs.iron_ore, 15)
            )
        }
        assertEquals(before, player.statMap.getFineXP(stats.smithing))
    }

    @Test
    fun GameTestState.gemsEssenceDenseEssenceAndAmethystAreExcluded() = game { d ->
        player.members = true
        val tool = InvObj(MiningObjs.infernalPickaxe)
        player.righthand = tool
        val before = player.statMap.getFineXP(stats.smithing)
        for (product in
            listOf(
                objs.uncut_opal,
                objs.uncut_sapphire,
                MiningObjs.runeEssence,
                MiningObjs.pureEssence,
                MiningObjs.denseEssenceBlock,
                objs.amethyst,
            )) {
            random.next = 0
            player.withProtectedAccess { assertFalse(d.lifecycle.combust(this, tool, product, 99)) }
            assertEquals(tool, player.righthand)
        }
        assertEquals(before, player.statMap.getFineXP(stats.smithing))
    }

    @Test
    fun GameTestState.luckyPennyCanSaveInfernalChargeWhileCrystalSignetCannot() = game { d ->
        player.members = true
        val tool = InvObj(MiningObjs.infernalPickaxe, vars = InfernalPickaxeState(1).pack())
        player.righthand = tool
        player.worn[Wearpos.Quiver.slot] = InvObj(objs.ghommals_lucky_penny)
        random.next = 0
        player.withProtectedAccess {
            assertTrue(d.lifecycle.combust(this, tool, objs.iron_ore, 15))
        }
        assertEquals(tool, player.righthand)
        player.worn[Wearpos.Quiver.slot] = null
        player.worn[Wearpos.Ring.slot] = InvObj(objs.elven_signet)
        random.next = 0
        player.withProtectedAccess {
            assertTrue(d.lifecycle.combust(this, tool, objs.iron_ore, 15))
        }
        assertEquals(MiningObjs.infernalPickaxeUncharged.id, player.righthand?.id)
    }

    @Test
    fun GameTestState.allStylesRechargeWithEitherNormalMaterialAndAwardCreationXp() = game { d ->
        player.members = true
        for (pair in d.lifecycle.rechargePairs) for (material in
            listOf(objs.smouldering_stone, MiningObjs.dragonPickaxe)) {
            player.inv[3] = InvObj(pair.depleted)
            player.inv[7] = InvObj(material)
            val mining = player.statMap.getFineXP(stats.mining)
            val smithing = player.statMap.getFineXP(stats.smithing)
            player.withProtectedAccess { d.lifecycle.recharge(this, 3, 7, pair, material) }
            assertEquals(pair.charged.id, player.inv[3]?.id)
            assertNull(player.inv[7])
            assertEquals(
                5000,
                InfernalPickaxeState.fromCharged(requireNotNull(player.inv[3])).charges,
            )
            assertEquals(2000, player.statMap.getFineXP(stats.mining) - mining)
            assertEquals(3500, player.statMap.getFineXP(stats.smithing) - smithing)
        }
    }

    @Test
    fun GameTestState.rechargeRejectsNonmembersCosmeticMaterialsAndAlreadyChargedTools() =
        game { d ->
            val pair = d.lifecycle.rechargePairs.first()
            player.members = false
            player.inv[0] = InvObj(pair.depleted)
            player.inv[1] = InvObj(objs.smouldering_stone)
            player.withProtectedAccess {
                d.lifecycle.recharge(this, 0, 1, pair, objs.smouldering_stone)
            }
            assertEquals(pair.depleted.id, player.inv[0]?.id)
            assertNotNull(player.inv[1])
            player.members = true
            player.inv[1] = InvObj(MiningObjs.dragonPickaxeUpgraded)
            player.withProtectedAccess {
                d.lifecycle.recharge(this, 0, 1, pair, MiningObjs.dragonPickaxeUpgraded)
            }
            assertEquals(pair.depleted.id, player.inv[0]?.id)
            player.inv[0] = InvObj(pair.charged)
            player.inv[1] = InvObj(objs.smouldering_stone)
            player.withProtectedAccess {
                d.lifecycle.recharge(this, 0, 1, pair, objs.smouldering_stone)
            }
            assertNotNull(player.inv[1])
        }

    @Test
    fun GameTestState.ordinaryIronMiningProcAwardsMiningAndSmithingWithoutProducingABarOrOre() =
        game { d ->
            player.members = true
            player.stats[stats.mining] = 99
            player.righthand =
                InvObj(MiningObjs.infernalPickaxe, vars = InfernalPickaxeState(1).pack())
            val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.iron1)
            player.telejump(rock.coords.translateX(-1))
            val mining = player.statMap.getFineXP(stats.mining)
            val smithing = player.statMap.getFineXP(stats.smithing)
            random.next = 128
            player.opLoc1(rock)
            advance()
            random.next = 128
            random.then = 0
            random.then = 1
            random.then = 0
            random.then = 0
            advance(8)
            assertEquals(0, player.count(objs.iron_ore))
            assertEquals(0, player.count(objs.iron_bar))
            assertEquals(350, player.statMap.getFineXP(stats.mining) - mining)
            assertEquals(62, player.statMap.getFineXP(stats.smithing) - smithing)
            assertEquals(MiningObjs.infernalPickaxeUncharged.id, player.righthand?.id)
        }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, null, Mining::class, testBody = body)

    data class Deps @Inject constructor(val lifecycle: InfernalPickaxeLifecycle)
}
