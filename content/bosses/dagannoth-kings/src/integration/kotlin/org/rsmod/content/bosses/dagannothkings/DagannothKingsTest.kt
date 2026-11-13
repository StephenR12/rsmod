package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.npc.engagePlayer
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.map.CoordGrid
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript

@Execution(ExecutionMode.SAME_THREAD)
class DagannothKingsTest {
    @Test
    fun GameTestState.`all three kings spawn with the complete combat triangle`() =
        runKingsTest(scripts = arrayOf(DagannothKingsScript::class)) { deps ->
            val kings =
                DagannothKings.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(3, kings.size)
            assertTrue(kings.all { it.baseHitpointsLvl == 255 && it.respawns })
            assertEquals(3, DagannothKings.definitions.map { it.style }.distinct().size)
        }

    @Test
    fun GameTestState.`Prime attacks at five tiles while Rex must close to melee`() =
        runInjectedGameTest(
            Dependencies::class,
            WeaponTestModule,
            DagannothKingsScript::class,
            org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript::class,
        ) { deps ->
            val tile = CoordGrid(3200, 3200, 0)
            allocZoneCollision(tile)
            allocZoneCollision(tile.translateX(8))
            val prime = spawnNpc(tile, deps.types[DagannothKingNpcs.prime])
            player.teleport(tile.translateX(prime.size + 4))
            advance(1)
            prime.engagePlayer(player, deps.interactions)
            assertEquals(NpcMode.ApPlayer2, prime.mode)
            advance(1)
            assertTrue(prime.actionDelay > prime.currentMapClock)
            assertEquals(tile, prime.coords)

            val rex = spawnNpc(tile.translateZ(3), deps.types[DagannothKingNpcs.rex])
            rex.engagePlayer(player, deps.interactions)
            assertEquals(NpcMode.OpPlayer2, rex.mode)
        }

    @Test
    fun GameTestState.`all kings always drop dagannoth bones`() =
        runKingsTest(scripts = arrayOf(DagannothKingsDrops::class)) { deps ->
            random.next = 100
            DagannothKings.definitions.forEach { definition ->
                val npc = spawnNpc(player.coords, deps.types[definition.npc])
                val drops =
                    checkNotNull(deps.drops[definition.npc])
                        .roll(NpcDropContext(npc, player, player.coords, deps.random))
                assertTrue(drops.any { it.obj.id == DagannothKingObjs.bones.id })
            }
        }

    private fun GameTestState.runKingsTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>> = emptyArray(),
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private object WeaponTestModule : com.google.inject.AbstractModule() {
        override fun configure() {
            bind(org.rsmod.api.combat.weapon.styles.AttackStyles::class.java)
                .`in`(com.google.inject.Scopes.SINGLETON)
        }
    }

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val interactions: AiPlayerInteractions,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val random: GameRandom,
    )
}
