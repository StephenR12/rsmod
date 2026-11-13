package org.rsmod.content.skills.woodcutting

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class CrystalGatheringToolSingingTest {
    @Test
    fun GameTestState.pickaxeConsumesExactInputsAwardsBothSkillsAndStartsWithTenThousandCharges() =
        singTool(false)

    @Test
    fun GameTestState.harpoonConsumesExactInputsAwardsBothSkillsAndStartsWithTenThousandCharges() =
        singTool(true)

    private fun GameTestState.singTool(harpoon: Boolean) = game { d ->
        prepare()
        val recipe = if (harpoon) d.harpoon else d.pickaxe
        materials(recipe, 120)
        val craft = player.statMap.getFineXP(stats.crafting)
        val smith = player.statMap.getFineXP(stats.smithing)
        player.withProtectedAccess { recipe.sing(this, false) }
        assertEquals(0, player.count(recipe.item))
        advance(3)
        assertEquals(1, player.count(recipe.item))
        assertEquals(0, player.count(recipe.dragon))
        assertEquals(0, player.count(objs.crystal_tool_seed))
        assertEquals(0, player.count(objs.crystal_shard))
        val result = player.inv.firstOrNull { it?.id == recipe.item.id }
        assertEquals(10000, CrystalAxeState.fromCharged(requireNotNull(result)).charges)
        assertEquals(60000, player.statMap.getFineXP(stats.crafting) - craft)
        assertEquals(60000, player.statMap.getFineXP(stats.smithing) - smith)
    }

    @Test
    fun GameTestState.assistedToolSingingUsesSixtyExtraShardsWithoutSkillsOrXp() = game { d ->
        prepare()
        player.stats[stats.crafting] = 1
        player.stats[stats.smithing] = 1
        materials(d.harpoon, 180)
        val craft = player.statMap.getFineXP(stats.crafting)
        val smith = player.statMap.getFineXP(stats.smithing)
        player.withProtectedAccess { d.harpoon.sing(this, true) }
        advance(3)
        assertEquals(1, player.count(objs.crystal_harpoon))
        assertEquals(0, player.count(objs.crystal_shard))
        assertEquals(craft, player.statMap.getFineXP(stats.crafting))
        assertEquals(smith, player.statMap.getFineXP(stats.smithing))
    }

    @Test
    fun GameTestState.singingRejectsMissingQuestNonmembersUnboostedSkillsAndCosmeticPickaxe() =
        game { d ->
            prepare()
            materials(d.pickaxe, 120)
            player.members = false
            player.withProtectedAccess { d.pickaxe.sing(this, false) }
            advance(3)
            assertEquals(0, player.count(d.pickaxe.item))
            player.members = true
            player.setVarBit(varbits.song_of_the_elves, 193)
            player.withProtectedAccess { d.pickaxe.sing(this, false) }
            advance(3)
            assertEquals(0, player.count(d.pickaxe.item))
            player.setVarBit(varbits.song_of_the_elves, 194)
            player.setCurrentLevel(stats.crafting, 75)
            player.withProtectedAccess { d.pickaxe.sing(this, false) }
            advance(3)
            assertEquals(0, player.count(d.pickaxe.item))
            player.setCurrentLevel(stats.crafting, 76)
            player.inv[0] = InvObj(objs.dragon_pickaxe_upgraded)
            player.withProtectedAccess { d.pickaxe.sing(this, false) }
            advance(3)
            assertEquals(0, player.count(d.pickaxe.item))
            assertEquals(120, player.count(objs.crystal_shard))
        }

    @Test
    fun GameTestState
        .visibleCraftingAndSmithingBoostsAllowSingingAndStaleMaterialsCancelAtomically() =
        game { d ->
            prepare()
            player.stats[stats.crafting] = 75
            player.stats[stats.smithing] = 75
            player.setCurrentLevel(stats.crafting, 76)
            player.setCurrentLevel(stats.smithing, 76)
            materials(d.pickaxe, 120)
            player.withProtectedAccess { d.pickaxe.sing(this, false) }
            advance(3)
            assertEquals(1, player.count(d.pickaxe.item))
            player.clearInv()
            materials(d.harpoon, 120)
            player.withProtectedAccess { d.harpoon.sing(this, false) }
            player.inv[1] = null
            advance(3)
            assertEquals(0, player.count(d.harpoon.item))
            assertEquals(1, player.count(d.harpoon.dragon))
            assertEquals(120, player.count(objs.crystal_shard))
        }

    @Test
    fun GameTestState.sharedBowlMenuContainsBothToolProductsWithAxeFirst() = game { d ->
        prepare()
        player.withProtectedAccess { d.singing.selectAtBowl(this) }
        val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
        assertEquals(objs.crystal_axe.id, setup.values[2])
        assertTrue(setup.values.contains(objs.crystal_pickaxe.id))
        assertTrue(setup.values.contains(objs.crystal_harpoon.id))
    }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.crafting] = 76
        player.stats[stats.smithing] = 76
        player.setVarBit(varbits.song_of_the_elves, 194)
    }

    private fun GameTestScope.materials(recipe: CrystalGatheringToolSinging, shards: Int) {
        player.inv[0] = InvObj(recipe.dragon)
        player.inv[1] = InvObj(objs.crystal_tool_seed)
        player.inv[2] = InvObj(objs.crystal_shard, shards)
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            CrystalGatheringSingingTestModule,
            Woodcutting::class,
            testBody = body,
        )

    data class Deps
    @Inject
    constructor(
        val pickaxe: CrystalPickaxeSinging,
        val harpoon: CrystalHarpoonSinging,
        val singing: CrystalAxeSinging,
    )
}

internal object CrystalGatheringSingingTestModule : AbstractModule() {
    override fun configure() {
        bind(CrystalAxeSinging::class.java)
        val recipes = Multibinder.newSetBinder(binder(), CrystalSingingRecipe::class.java)
        recipes.addBinding().to(CrystalPickaxeSinging::class.java)
        recipes.addBinding().to(CrystalHarpoonSinging::class.java)
    }
}
