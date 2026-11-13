package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class WesleyCrushingTest {
    @Test
    fun GameTestState.`all seventeen documented raw ingredients cost fifty coins with exact outputs and no xp`() =
        runWesleyTest { deps ->
            player.members = true
            assertEquals(7369, WesleyNpcs.wesley.id)
            assertEquals(17, WesleyRecipes.all.size)
            for (recipe in WesleyRecipes.all) {
                player.clearInv()
                player.inv[0] = InvObj(objs.coins, 50)
                player.inv[1] = InvObj(recipe.input)
                val xp = player.statMap.getFineXP(stats.herblore)
                var count = 0
                player.withProtectedAccess { count = deps.crushing.crush(this) }
                assertEquals(1, count)
                assertEquals(recipe.outputCount, player.count(recipe.output))
                assertEquals(0, player.count(recipe.input))
                assertEquals(0, player.count(objs.coins))
                assertEquals(xp, player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`bulk noted input remains noted and charges per input rather than output`() =
        runWesleyTest { deps ->
            player.members = true
            val input = deps.objs.getValue(deps.objs[objs.unicorn_horn].certlink)
            val output = deps.objs.getValue(deps.objs[objs.unicorn_horn_dust].certlink)
            player.inv[0] = InvObj(input, 100)
            player.inv[1] = InvObj(objs.coins, 6000)
            player.withProtectedAccess { deps.crushing.crush(this) }
            assertEquals(0, player.count(input))
            assertEquals(100, player.count(output))
            assertEquals(1000, player.count(objs.coins))

            player.clearInv()
            player.inv[0] = InvObj(objs.lava_scale)
            player.inv[1] = InvObj(objs.coins, 50)
            player.withProtectedAccess { deps.crushing.crush(this) }
            assertEquals(3, player.count(objs.lava_scale_shard))
            assertEquals(0, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`affordability failure rolls back and full inventory accepts stackable output`() =
        runWesleyTest { deps ->
            player.members = true
            player.inv[0] = InvObj(objs.unicorn_horn)
            player.inv[1] = InvObj(objs.coins, 49)
            val before = player.inv.toList()
            player.withProtectedAccess { deps.crushing.crush(this) }
            assertEquals(before, player.inv.toList())
            player.fillInv()
            player.inv[0] = InvObj(objs.lava_scale)
            player.inv[1] = InvObj(objs.coins, 100)
            player.withProtectedAccess { deps.crushing.crush(this) }
            assertEquals(0, player.count(objs.lava_scale))
            assertEquals(3, player.count(objs.lava_scale_shard))
            assertEquals(50, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`native Talk-to offers an explicit quote and decline preserves ingredients`() =
        runWesleyTest { deps ->
            player.members = true
            player.placeAt(CoordGrid(3200, 3200))
            player.inv[0] = InvObj(objs.unicorn_horn)
            player.inv[1] = InvObj(objs.coins, 50)
            val npc = spawnNpc(player.coords.translateX(1), deps.npcs[WesleyNpcs.wesley])
            player.opNpc1(npc)
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 2))
            assertEquals(1, player.count(objs.unicorn_horn))
            assertEquals(50, player.count(objs.coins))
        }

    private fun GameTestState.runWesleyTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(WesleyScript::class), testBody = body)

    data class Dependencies @Inject constructor(val crushing: WesleyCrushing, val objs: ObjTypeList, val npcs: NpcTypeList)
}
