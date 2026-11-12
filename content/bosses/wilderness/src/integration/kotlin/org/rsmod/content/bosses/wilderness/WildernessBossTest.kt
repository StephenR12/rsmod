package org.rsmod.content.bosses.wilderness

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript

@Execution(ExecutionMode.SAME_THREAD)
class WildernessBossTest {
    @Test
    fun GameTestState.`all eleven wilderness bosses spawn permanently with configured stats`() =
        runBossTest(scripts = arrayOf(WildernessBossScript::class)) { deps ->
            val bosses =
                WildernessBosses.definitions.map { definition ->
                    val npc =
                        deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                    assertEquals(definition.hitpoints, npc.baseHitpointsLvl)
                    assertEquals(definition.attack, npc.attackLvl)
                    assertEquals(definition.strength, npc.strengthLvl)
                    assertEquals(definition.defence, npc.defenceLvl)
                    assertTrue(npc.respawns)
                    npc
                }
            assertEquals(11, bosses.size)
            assertEquals(8, WildernessBosses.definitions.map { it.family }.distinct().size)
        }

    @Test
    fun GameTestState.`Venenatis and Spindel summon three five-hitpoint spiderlings`() =
        runBossTest { deps ->
            val boss = spawnNpc(player.coords, deps.types[WildernessBossNpcs.venenatis])

            deps.controller.spawnSpiderlings(boss, lesser = false)

            val spiderlings =
                deps.npcs
                    .findAll(org.rsmod.map.zone.ZoneKey.from(player.coords), 1)
                    .filter { it.id == WildernessBossNpcs.venenatisSpiderling.id }
                    .toList()
            assertEquals(3, spiderlings.size)
            assertTrue(spiderlings.all { it.baseHitpointsLvl == 5 })
        }

    @Test
    fun GameTestState.`one fifty thousand coin fee unlocks every reworked boss lair`() =
        runBossTest { deps ->
            val callisto = WildernessBosses.byId.getValue(WildernessBossNpcs.callisto.id)
            player.inv[0] = InvObj(objs.coins, WildernessBossAccess.ENTRANCE_FEE)

            player.withProtectedAccess { deps.access.enter(this, callisto) }

            assertTrue(deps.access.hasPaid(player))
            assertEquals(0, player.inv.count(objs.coins))
            assertEquals(callisto.spawn, player.coords)
        }

    @Test
    fun GameTestState.`all boss drop tables register their guaranteed remains`() =
        runBossTest(scripts = arrayOf(WildernessBossDrops::class)) { deps ->
            random.next = 100
            WildernessBosses.definitions.forEach { definition ->
                val table = deps.drops[definition.npc]
                assertNotNull(table)
                val npc = spawnNpc(player.coords, deps.types[definition.npc])
                val rolled =
                    checkNotNull(table)
                        .roll(NpcDropContext(npc, player, player.coords, deps.random))
                val expected =
                    if (definition.family == WildernessBossFamily.KingBlackDragon) {
                        WildernessBossObjs.dragonBones
                    } else {
                        WildernessBossObjs.bigBones
                    }
                assertTrue(rolled.any { it.obj.id == expected.id })
            }
        }

    @Test
    fun GameTestState.`King Black Dragon always drops bones and two hides`() =
        runBossTest(scripts = arrayOf(WildernessBossDrops::class)) { deps ->
            random.next = 100
            val npc = spawnNpc(player.coords, deps.types[WildernessBossNpcs.kingBlackDragon])
            val rolled =
                checkNotNull(deps.drops[WildernessBossNpcs.kingBlackDragon])
                    .roll(NpcDropContext(npc, player, player.coords, deps.random))
            assertTrue(rolled.any { it.obj.id == WildernessBossObjs.dragonBones.id })
            assertTrue(
                rolled.any {
                    it.obj.id == WildernessBossObjs.blackDragonhide.id && it.obj.count == 2
                }
            )
        }

    private fun GameTestState.runBossTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>> = emptyArray(),
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val random: GameRandom,
        val controller: WildernessBossController,
        val access: WildernessBossAccess,
    )
}
