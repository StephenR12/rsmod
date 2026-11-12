package org.rsmod.content.bosses.corporealbeast

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.npc.NpcTypeList

@Execution(ExecutionMode.SAME_THREAD)
class CorporealBeastTest {
    @Test
    fun GameTestState.`corporeal beast spawns permanently with its OSRS combat stats`() =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(CorporealBeastScript::class)) {
            deps ->
            val boss =
                deps.npcs.findAll(CorporealBeastMap.bossRoom).single {
                    it.id == CorporealBeastNpcs.boss.id
                }
            assertEquals(CorporealBeastStats.HITPOINTS, boss.baseHitpointsLvl)
            assertEquals(CorporealBeastStats.ATTACK, boss.attackLvl)
            assertEquals(CorporealBeastStats.STRENGTH, boss.strengthLvl)
            assertEquals(CorporealBeastStats.DEFENCE, boss.defenceLvl)
            assertEquals(CorporealBeastStats.MAGIC, boss.magicLvl)
            assertTrue(boss.respawns)
        }

    @Test
    fun GameTestState.`dark core has twenty five hitpoints and heals corporeal beast`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val boss = spawnNpc(player.coords, deps.types[CorporealBeastNpcs.boss])
            boss.hitpoints = 500
            boss.baseHitpointsLvl = CorporealBeastStats.HITPOINTS

            val core = deps.controller.summonDarkCore(boss)
            deps.controller.healFromCore(core, CorporealBeastController.DARK_CORE_DRAIN)

            assertEquals(CorporealBeastController.DARK_CORE_HITPOINTS, core.baseHitpointsLvl)
            assertEquals(505, boss.hitpoints)
        }

    @Test
    fun GameTestState.`corporeal beast signature drop table is registered`() =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(CorporealBeastDrops::class)) {
            deps ->
            random.next = 100
            val table = deps.drops[CorporealBeastNpcs.boss]
            assertNotNull(table)
            val boss = spawnNpc(player.coords, deps.types[CorporealBeastNpcs.boss])
            val rolled =
                checkNotNull(table).roll(NpcDropContext(boss, player, player.coords, deps.random))
            assertTrue(rolled.isNotEmpty())
            assertEquals(1_365, CorporealBeastDropRates.COMMON_SIGIL)
            assertEquals(4_095, CorporealBeastDropRates.ELYSIAN_SIGIL)
            assertEquals(5_000, CorporealBeastDropRates.PET)
        }

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val random: GameRandom,
        val controller: CorporealBeastController,
    )
}
