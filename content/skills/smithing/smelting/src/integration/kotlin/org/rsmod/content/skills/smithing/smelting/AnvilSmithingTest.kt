package org.rsmod.content.skills.smithing.smelting

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.feet
import org.rsmod.api.player.hands
import org.rsmod.api.player.legs
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class AnvilSmithingTest {
    @Test
    fun GameTestState.`native Make Five button sets batch size and stops at the requested count`() =
        runAnvilTest { deps ->
            prepare()
            val anvil = placeMapLoc(CoordGrid(3200, 3200), AnvilLocs.ordinary.first())
            player.placeAt(anvil.coords.translateX(-1))
            player.inv[0] = InvObj(objs.hammer)
            repeat(6) { player.inv[it + 1] = InvObj(objs.bronze_bar) }
            player.withProtectedAccess { deps.smithing.open(this, anvil, objs.bronze_bar) }
            player.ifButton(AnvilComponents.button4)
            advance()
            player.ifButton(AnvilComponents.button9)
            advance(26)
            val dagger = deps.recipes.find(1, AnvilComponents.button9)!!
            assertEquals(5, player.count(dagger.output))
            assertEquals(1, player.count(objs.bronze_bar))
        }

    @Test
    fun GameTestState
        .`rusted anvil rejects higher metals while ordinary bronze smithing remains available`() =
        runAnvilTest { deps ->
            prepare()
            val anvil = placeMapLoc(CoordGrid(3200, 3200), AnvilLocs.rusted)
            player.placeAt(anvil.coords.translateX(-1))
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objs.iron_bar)
            val iron = deps.recipes.find(2, AnvilComponents.button9)!!
            player.withProtectedAccess { deps.smithing.smith(this, anvil, iron, 1) }
            assertMessageSent("You can only smith bronze items on this rusted anvil.")
            assertEquals(1, player.count(objs.iron_bar))
            player.inv[1] = InvObj(objs.bronze_bar)
            val bronze = deps.recipes.find(1, AnvilComponents.button9)!!
            player.withProtectedAccess { deps.smithing.smith(this, anvil, bronze, 1) }
            advance(5)
            assertEquals(1, player.count(bronze.output))
        }

    @Test
    fun GameTestState.`all six metal catalogs follow cache quantity bar and level enums`() =
        runAnvilTest { deps ->
            assertEquals(156, deps.recipes.all.size)
            assertEquals(312, AnvilInterfaces.smithing.id)
            assertEquals(3216, AnvilVars.barType.id)
            assertEquals(
                setOf(2031, 2097, 4306, 6150, 22725, 28563, 31623, 39242, 39620, 40725, 51501),
                (AnvilLocs.ordinary + AnvilLocs.doric + AnvilLocs.rusted).map { it.id }.toSet(),
            )
            for (recipe in deps.recipes.all) {
                assertEquals(deps.enums.getValue(844).primitiveMap[recipe.output.id], recipe.count)
                assertEquals(deps.enums.getValue(845).primitiveMap[recipe.output.id], recipe.bars)
                assertEquals(
                    (deps.enums.getValue(846).primitiveMap.getValue(recipe.output.id) as Int)
                        .coerceAtLeast(1),
                    recipe.level,
                )
                assertEquals(recipe.metal * 12.5 * recipe.bars, recipe.xp)
            }
        }

    @Test
    fun GameTestState
        .`every recipe consumes exact bars produces exact quantity and awards exact xp after five ticks`() =
        runAnvilTest { deps ->
            prepare()
            val anvil = placeMapLoc(CoordGrid(3200, 3200), AnvilLocs.ordinary.first())
            player.placeAt(anvil.coords.translateX(-1))
            for (recipe in deps.recipes.all) {
                player.clearInv()
                player.inv[0] = InvObj(objs.hammer)
                repeat(recipe.bars) { player.inv[it + 1] = InvObj(recipe.bar) }
                val xp = player.statMap.getFineXP(stats.smithing)
                player.withProtectedAccess { deps.smithing.smith(this, anvil, recipe, 1) }
                advance(4)
                assertEquals(0, player.count(recipe.output))
                advance()
                assertEquals(
                    recipe.count,
                    player.count(recipe.output),
                    deps.objs[recipe.output].name,
                )
                assertEquals(0, player.count(recipe.bar))
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.smithing) - xp,
                )
            }
        }

    @Test
    fun GameTestState.`native anvil operation and smith button dispatch one atomic recipe`() =
        runAnvilTest { deps ->
            prepare()
            val anvil = placeMapLoc(CoordGrid(3200, 3200), AnvilLocs.ordinary.first())
            player.placeAt(anvil.coords.translateX(-1))
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objs.bronze_bar)
            player.opLoc1(anvil)
            advance()
            assertEquals(1, player.vars[AnvilVars.barType])
            player.ifButton(AnvilComponents.button9, op = IfButtonOp.Op1)
            advance(6)
            val dagger = deps.recipes.find(1, AnvilComponents.button9)!!
            assertEquals(1, player.count(dagger.output))
        }

    @Test
    fun GameTestState.`tool or level loss and movement during the delay preserve all bars`() =
        runAnvilTest { deps ->
            prepare()
            val anvil = placeMapLoc(CoordGrid(3200, 3200), AnvilLocs.ordinary.first())
            player.placeAt(anvil.coords.translateX(-1))
            val recipe = deps.recipes.find(6, AnvilComponents.button22)!!
            player.inv[0] = InvObj(objs.hammer)
            repeat(5) { player.inv[it + 1] = InvObj(recipe.bar) }
            player.withProtectedAccess { deps.smithing.smith(this, anvil, recipe, 1) }
            player.inv[0] = null
            advance(5)
            assertEquals(5, player.count(recipe.bar))
            player.inv[0] = InvObj(objs.hammer)
            player.withProtectedAccess { deps.smithing.smith(this, anvil, recipe, 1) }
            player.setCurrentLevel(stats.smithing, 98)
            advance(5)
            assertEquals(5, player.count(recipe.bar))
            player.setCurrentLevel(stats.smithing, 99)
            player.withProtectedAccess { deps.smithing.smith(this, anvil, recipe, 1) }
            player.placeAt(anvil.coords.translateX(5))
            advance(5)
            assertEquals(5, player.count(recipe.bar))
        }

    @Test
    fun GameTestState
        .`full inventory replacement succeeds and full uniform makes production four ticks`() =
        runAnvilTest { deps ->
            prepare()
            player.righthand = InvObj(objs.imcando_hammer)
            player.torso = InvObj(deps.objs.getValue(27023))
            player.legs = InvObj(deps.objs.getValue(27025))
            player.feet = InvObj(deps.objs.getValue(27027))
            player.hands = InvObj(deps.objs.getValue(27031))
            assertEquals(4, deps.smithing.actionTicks(player))
            val anvil = placeMapLoc(CoordGrid(3200, 3200), AnvilLocs.ordinary.first())
            player.placeAt(anvil.coords.translateX(-1))
            val recipe = deps.recipes.find(1, AnvilComponents.button23)!!
            player.fillInv()
            player.inv[0] = InvObj(recipe.bar)
            player.withProtectedAccess { deps.smithing.smith(this, anvil, recipe, 1) }
            advance(4)
            assertEquals(15, player.count(recipe.output))
            assertEquals(0, player.count(recipe.bar))
        }

    @Test
    fun GameTestState
        .`members and quest boundaries reject protected products before consuming bars`() =
        runAnvilTest { deps ->
            prepare()
            val anvil = placeMapLoc(CoordGrid(3200, 3200), AnvilLocs.ordinary.first())
            player.placeAt(anvil.coords.translateX(-1))
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objs.bronze_bar)
            val dart = deps.recipes.find(1, AnvilComponents.button29)!!
            player.setVarp(AnvilQuestVars.touristTrap, 0)
            player.withProtectedAccess { deps.smithing.smith(this, anvil, dart, 1) }
            assertMessageSent("You need to complete The Tourist Trap to smith dart tips.")
            assertEquals(1, player.count(objs.bronze_bar))
            player.members = false
            player.withProtectedAccess { deps.smithing.smith(this, anvil, dart, 1) }
            assertMessageSent("You need to be on a members' world to smith this item.")
            assertEquals(1, player.count(objs.bronze_bar))
        }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.smithing] = 99
        player.setVarp(AnvilQuestVars.touristTrap, 16)
        player.setVarp(AnvilQuestVars.deathPlateau, 80)
        player.setVarp(AnvilQuestVars.doric, 100)
    }

    private fun GameTestState.runAnvilTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            AnvilSmithingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(SmeltingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val smithing: AnvilSmithing,
        val recipes: AnvilRecipes,
        val enums: EnumTypeList,
        val objs: ObjTypeList,
    )
}
