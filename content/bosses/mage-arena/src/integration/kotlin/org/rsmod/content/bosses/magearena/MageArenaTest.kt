package org.rsmod.content.bosses.magearena

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class MageArenaTest {
    @Test
    fun GameTestState.`mage arena one progresses through all five Kolodion forms`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val kolodion = spawnNpc(player.coords, deps.types[MageArenaNpcs.kolodionHuman])
            val state = deps.controller.resetKolodion(kolodion)

            MageArenaBosses.kolodionForms.forEachIndexed { phase, form ->
                state.kolodionPhase = phase
                deps.controller.configureKolodion(kolodion, phase)
                assertEquals(form.npc.id, kolodion.visType.id)
                assertEquals(form.hitpoints, kolodion.hitpoints)
            }
        }

    @Test
    fun GameTestState.`enchanted symbol summons the next mage arena two follower`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            player.withProtectedAccess {
                vars[varbits.inside_wilderness] = 1
                deps.controller.summonNextFollower(this)
            }

            val followers =
                deps.npcs
                    .findAll(ZoneKey.from(player.coords), 1)
                    .filter { it.id in MageArenaBosses.followerIds }
                    .toList()
            assertEquals(1, followers.size)
            assertEquals(MageArenaNpcs.zachariah.id, followers.single().id)
            assertEquals(MageArenaController.FOLLOWER_HITPOINTS, followers.single().hitpoints)
        }

    @Test
    fun GameTestState.`Derwen energy heals five hitpoints while alive`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val derwen = spawnNpc(player.coords, deps.types[MageArenaNpcs.derwen])
            derwen.hitpoints = 200
            derwen.baseHitpointsLvl = MageArenaController.FOLLOWER_HITPOINTS

            deps.controller.summonDerwenEnergy(derwen)
            val energy =
                deps.npcs.findAll(ZoneKey.from(player.coords), 1).single {
                    it.id == MageArenaNpcs.derwenEnergy.id
                }
            deps.controller.healDerwen(energy)

            assertEquals(MageArenaController.DERWEN_ENERGY_HITPOINTS, energy.hitpoints)
            assertEquals(205, derwen.hitpoints)
        }

    @Test
    fun GameTestState.`mage arena two followers always drop their quest artefacts`() =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(MageArenaDrops::class)) { deps ->
            val expected =
                mapOf(
                    MageArenaNpcs.zachariah to MageArenaObjs.justiciarsHand,
                    MageArenaNpcs.derwen to MageArenaObjs.entsRoots,
                    MageArenaNpcs.porazdir to MageArenaObjs.demonsHeart,
                )
            expected.forEach { (type, obj) ->
                val npc = spawnNpc(player.coords, deps.types[type])
                val drops =
                    checkNotNull(deps.drops[type])
                        .roll(NpcDropContext(npc, player, player.coords, deps.random))
                assertTrue(drops.any { it.obj.id == obj.id })
            }
        }

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val random: org.rsmod.api.random.GameRandom,
        val controller: MageArenaController,
    )
}
