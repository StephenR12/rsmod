package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class CrystalPickaxeTest {
    @Test
    fun GameTestState.selectedWornAndCarriedToolsConsumeChargesAndDeactivate() = game { d ->
        prepare()
        assertEquals(
            10000,
            CrystalPickaxeState.fromCharged(InvObj(MiningObjs.crystalPickaxe)).charges,
        )
        val tool = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(2).pack())
        player.inv[0] = tool
        player.withProtectedAccess { assertTrue(d.lifecycle.consumeCharge(this, tool)) }
        assertEquals(1, CrystalPickaxeState.fromCharged(player.inv[0]!!).charges)
        player.withProtectedAccess { assertTrue(d.lifecycle.consumeCharge(this, player.inv[0]!!)) }
        assertEquals(MiningObjs.crystalPickaxeInactive.id, player.inv[0]?.id)
        player.inv[0] = null
        player.righthand = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(1).pack())
        player.withProtectedAccess {
            assertTrue(d.lifecycle.consumeCharge(this, player.righthand!!))
        }
        assertEquals(MiningObjs.crystalPickaxeInactive.id, player.righthand?.id)
    }

    @Test
    fun GameTestState.allSignetsAndIndependentPennyCanSaveCharges() = game { d ->
        prepare()
        for (signet in
            listOf(objs.elven_signet, objs.celestial_signet, objs.celestial_signet_charged)) {
            val tool = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(2).pack())
            player.inv[0] = tool
            player.worn[12] = InvObj(signet)
            random.next = 0
            player.withProtectedAccess { assertTrue(d.lifecycle.consumeCharge(this, tool)) }
            assertEquals(2, CrystalPickaxeState.fromCharged(player.inv[0]!!).charges)
        }
        val tool = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(2).pack())
        player.inv[0] = tool
        player.worn[12] = InvObj(objs.elven_signet)
        player.worn[13] = InvObj(objs.ghommals_lucky_penny)
        random.next = 1
        random.then = 0
        player.withProtectedAccess { d.lifecycle.consumeCharge(this, tool) }
        assertEquals(2, CrystalPickaxeState.fromCharged(player.inv[0]!!).charges)
        random.next = 1
        random.then = 1
        player.withProtectedAccess { d.lifecycle.consumeCharge(this, tool) }
        assertEquals(1, CrystalPickaxeState.fromCharged(player.inv[0]!!).charges)
    }

    @Test
    fun GameTestState.otherOrMissingToolsConsumeNothing() = game { d ->
        prepare()
        val selected = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(2).pack())
        player.inv[0] = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(1).pack())
        player.withProtectedAccess {
            assertFalse(d.lifecycle.consumeCharge(this, selected))
            assertFalse(d.lifecycle.consumeCharge(this, InvObj(MiningObjs.runePickaxe)))
        }
        assertEquals(1, CrystalPickaxeState.fromCharged(player.inv[0]!!).charges)
    }

    @Test
    fun GameTestState.ordinaryOreConsumesFinalChargeAndInactiveToolStillMines() = game { d ->
        prepare()
        val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.iron1)
        player.telejump(rock.coords.translateX(-1))
        player.righthand = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(1).pack())
        random.next = 1
        player.opLoc1(rock)
        advance()
        random.next = 128
        random.then = 0
        random.then = 1
        advance(4)
        assertEquals(1, player.count(objs.iron_ore))
        assertEquals(MiningObjs.crystalPickaxeInactive.id, player.righthand?.id)
        assertEquals(
            MiningObjs.crystalPickaxeInactive.id,
            Mining.findPickaxe(player, d.objects)?.id,
        )
    }

    @Test
    fun GameTestState.failedRollAndFullInventoryPreserveCharge() = game { d ->
        prepare()
        val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.iron1)
        player.telejump(rock.coords.translateX(-1))
        player.righthand = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(2).pack())
        random.next = 1
        player.opLoc1(rock)
        advance()
        random.next = 128
        random.then = 255
        random.then = 1
        advance(3)
        player.moveGameClick(player.coords.translate(-1, 0))
        advance()
        assertEquals(0, player.count(objs.iron_ore))
        assertEquals(2, CrystalPickaxeState.fromCharged(player.righthand!!).charges)
        for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.knife)
        player.opLoc1(rock)
        advance(6)
        assertEquals(2, CrystalPickaxeState.fromCharged(player.righthand!!).charges)
    }

    @Test
    fun GameTestState.nativeWornCheckAndRemoveUseDistinctOperations() = game { d ->
        prepare()
        player.righthand = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(123).pack())
        assertEquals("Check", d.objects[MiningObjs.crystalPickaxe].param(params.wear_op1))
        player.withProtectedAccess { d.worn.interact(this, player.worn, 3, IfButtonOp.Op2) }
        assertMessageSent("The crystal pickaxe has 123 charges remaining.")
        assertEquals(MiningObjs.crystalPickaxe.id, player.righthand?.id)
        player.withProtectedAccess { d.worn.interact(this, player.worn, 3, IfButtonOp.Op1) }
        assertNull(player.righthand)
        assertEquals(1, player.count(MiningObjs.crystalPickaxe))
    }

    @Test
    fun GameTestState.shardsAddOneHundredChargesAndClampToTwentyThousand() = game { d ->
        prepare()
        player.inv[0] = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(19950).pack())
        player.inv[1] = InvObj(objs.crystal_shard, 2)
        player.withProtectedAccess { d.lifecycle.charge(this, 0, 1) }
        player.resumeCountDialog(1)
        advance()
        assertEquals(20000, CrystalPickaxeState.fromCharged(player.inv[0]!!).charges)
        assertEquals(1, player.count(objs.crystal_shard))
        player.withProtectedAccess { d.lifecycle.charge(this, 0, 1) }
        assertEquals(1, player.count(objs.crystal_shard))
    }

    @Test
    fun GameTestState.revertReturnsMaterialsAtomicallyAndRejectsFullBag() = game { d ->
        prepare()
        val tool = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(123).pack())
        player.inv[0] = tool
        for (slot in 1..27) player.inv[slot] = InvObj(objs.knife)
        player.withProtectedAccess { d.lifecycle.revert(this, 0, MiningObjs.crystalPickaxe) }
        assertEquals(tool, player.inv[0])
        player.inv[27] = null
        player.withProtectedAccess { d.lifecycle.revert(this, 0, MiningObjs.crystalPickaxe) }
        assertEquals(1, player.count(MiningObjs.dragonPickaxe))
        assertEquals(1, player.count(objs.crystal_tool_seed))
    }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.mining] = 85
        VarPlayerIntMapSetter.set(player, varbits.song_of_the_elves, 194)
    }

    private fun GameTestState.game(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, null, Mining::class, testBody = body)

    data class Dependencies
    @Inject
    constructor(
        val lifecycle: CrystalPickaxeLifecycle,
        val objects: ObjTypeList,
        val worn: WornInteractions,
    )
}
