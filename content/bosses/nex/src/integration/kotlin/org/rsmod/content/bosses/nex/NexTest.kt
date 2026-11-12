package org.rsmod.content.bosses.nex

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.PlayerDeath
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript

@Execution(ExecutionMode.SAME_THREAD)
class NexTest {
    @Test
    fun GameTestState.`nex and the four ancient mages spawn permanently`() =
        runNexTest(scripts = arrayOf(NexScript::class)) { deps ->
            val nex = deps.npcs.findAll(NexArena.bossSpawn).single { it.id == NexNpcs.boss.id }
            assertEquals(3_400, nex.baseHitpointsLvl)
            assertEquals(
                listOf(315, 200, 260, 230, 350),
                listOf(nex.attackLvl, nex.strengthLvl, nex.defenceLvl, nex.magicLvl, nex.rangedLvl),
            )
            assertTrue(nex.respawns)

            val mages =
                NexArena.mageSpawns.map { (type, coords) ->
                    deps.npcs.findAll(coords).single { it.id == type.id }
                }
            assertEquals(4, mages.size)
            assertTrue(mages.all { it.baseHitpointsLvl == 500 && it.respawns })
            assertTrue(mages.none(deps.controller::isMageActive))

            val followers =
                NexAccessMap.prisonFollowers.map { (type, coords) ->
                    deps.npcs.findAll(coords).single { it.id == type.id }
                }
            assertEquals(6, followers.size)
            assertTrue(followers.all { it.baseHitpointsLvl == 120 && it.respawns })
        }

    @Test
    fun GameTestState.`frozen key permanently unlocks the level seventy door`() =
        runNexTest { deps ->
            player.setBaseLevel(stats.agility, 70)
            player.setBaseLevel(stats.ranged, 70)
            player.setBaseLevel(stats.strength, 70)
            player.setCurrentLevel(stats.hitpoints, 70)
            player.setVarBit(
                varbits.desert_treasure_main,
                NexAccessController.DESERT_TREASURE_COMPLETE,
            )
            player.inv[0] = InvObj(NexObjs.frozenKey)

            player.withProtectedAccess { deps.access.enterFrozenDoor(this) }

            assertEquals(NexAccessMap.ancientPrison, player.coords)
            assertEquals(0, player.inv.count(NexObjs.frozenKey))
            player.setBaseLevel(stats.agility, 1)
            player.withProtectedAccess {
                deps.access.leaveFrozenDoor(this)
                deps.access.enterFrozenDoor(this)
            }
            assertEquals(NexAccessMap.ancientPrison, player.coords)
        }

    @Test
    fun GameTestState.`four God Wars generals award pieces that assemble into a frozen key`() =
        runNexTest { deps ->
            val generals =
                listOf(
                    NexGodWarsNpcs.kree,
                    NexGodWarsNpcs.graardor,
                    NexGodWarsNpcs.kril,
                    NexGodWarsNpcs.zilyana,
                )
            generals.forEach { type ->
                val general = spawnNpc(player.coords, deps.types[type])
                deps.access.onGodWarsKill(player, general)
            }

            assertEquals(4, player.inv.filterNotNull().size)
            player.withProtectedAccess { deps.access.assembleFrozenKey(this) }

            assertEquals(1, player.inv.count(NexObjs.frozenKey))
            assertEquals(1, player.inv.filterNotNull().size)
        }

    @Test
    fun GameTestState.`forty ancient essence is consumed at the prison door`() =
        runNexTest { deps ->
            val warrior = spawnNpc(player.coords, deps.types[NexNpcs.prisonWarrior])
            repeat(20) { deps.access.addAncientEssence(player, warrior) }
            assertEquals(40, deps.access.ancientEssence(player))

            player.withProtectedAccess { deps.access.enterSafeRoom(this) }

            assertEquals(NexAccessMap.safeRoom, player.coords)
            assertEquals(0, deps.access.ancientEssence(player))
        }

    @Test
    fun GameTestState
        .`unsafe Nex death stores risk and chest reclaims it for one hundred thousand`() =
        runNexTest { deps ->
            player.username = "nex-death-test"
            val carried =
                listOf(
                    NexObjs.armadylKeyPiece,
                    NexObjs.bandosKeyPiece,
                    NexObjs.zamorakKeyPiece,
                    NexObjs.saradominKeyPiece,
                )
            carried.forEachIndexed { slot, obj -> player.inv[slot] = InvObj(obj) }

            deps.deathStorage.markAtRisk(player)
            deps.deathStorage.onDeath(player)

            assertTrue(deps.deathStorage.hasStoredItems(player))
            assertEquals(1, deps.deathStorage.storedItemCount(player))
            player.inv[10] = InvObj(NexObjs.coins, NexDeathStorage.RECLAIM_FEE)
            player.withProtectedAccess { deps.deathStorage.reclaim(this) }

            assertFalse(deps.deathStorage.hasStoredItems(player))
            assertEquals(0, player.inv.count(NexObjs.coins))
            assertTrue(carried.all { player.inv.count(it) == 1 })
        }

    @Test
    fun GameTestState.`standard death completion captures Nex risk after protected access ends`() =
        runNexTest(scripts = arrayOf(NexScript::class)) { deps ->
            player.username = "nex-standard-death-test"
            listOf(
                    NexObjs.armadylKeyPiece,
                    NexObjs.bandosKeyPiece,
                    NexObjs.zamorakKeyPiece,
                    NexObjs.saradominKeyPiece,
                )
                .forEachIndexed { slot, obj -> player.inv[slot] = InvObj(obj) }
            deps.deathStorage.markAtRisk(player)

            player.withProtectedAccess { deps.playerDeath.death(this) }
            advance(7)

            assertTrue(deps.deathStorage.hasStoredItems(player))
            assertEquals(1, deps.deathStorage.storedItemCount(player))
        }

    @Test
    fun GameTestState.`mage gates advance all five phases and Zaros heals five hundred`() =
        runNexTest(scripts = arrayOf(NexScript::class)) { deps ->
            val nex = deps.npcs.findAll(NexArena.bossSpawn).single { it.id == NexNpcs.boss.id }
            val state = deps.controller.state(nex)
            val expected = listOf(NexPhase.Shadow, NexPhase.Blood, NexPhase.Ice, NexPhase.Zaros)
            val mages = listOf(NexNpcs.fumus, NexNpcs.umbra, NexNpcs.cruor, NexNpcs.glacies)
            mages.zip(expected).forEach { (type, phase) ->
                val mage =
                    deps.npcs.findAll(NexArena.mageSpawns.getValue(type)).single {
                        it.id == type.id
                    }
                state.waitingForMage = type
                assertTrue(deps.controller.isMageActive(mage))
                if (phase == NexPhase.Zaros) {
                    nex.hitpoints = NexPhase.Ice.threshold
                }
                deps.controller.onMageKilled(mage)
                assertEquals(phase, state.phase)
            }
            assertEquals(NexPhase.Ice.threshold + NexController.ZAROS_HEAL, nex.hitpoints)
        }

    @Test
    fun `final phase rotates soul split deflect melee and no curse`() {
        val state = NexState(phase = NexPhase.Zaros)
        state.attacks = 0
        assertEquals(NexCurse.SoulSplit, state.curse)
        state.attacks = 4
        assertEquals(NexCurse.DeflectMelee, state.curse)
        state.attacks = 8
        assertEquals(NexCurse.None, state.curse)
    }

    @Test
    fun GameTestState.`drop table registers guaranteed shards and exact unique weights`() =
        runNexTest(scripts = arrayOf(NexDrops::class)) { deps ->
            assertEquals(12, NexDropTable.uniqueEntries.sumOf { it.weight })
            assertEquals(listOf(2, 2, 2, 2, 3, 1), NexDropTable.uniqueEntries.map { it.weight })
            assertEquals(43, NexDropTable.UNIQUE_DENOMINATOR)
            assertEquals(500, NexDropTable.PET_DENOMINATOR)

            random.next = 100
            val npc = spawnNpc(player.coords, deps.types[NexNpcs.boss])
            val table = checkNotNull(deps.drops[NexNpcs.boss])
            val rolled =
                table.roll(NpcDropContext(npc, player, player.coords, deps.random)).map {
                    it.obj.id
                }
            assertTrue(NexObjs.bigBones.id in rolled)
            assertTrue(NexObjs.nihilShard.id in rolled)
            assertFalse(NexObjs.nexling.id in rolled)
        }

    private fun GameTestState.runNexTest(
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
        val controller: NexController,
        val access: NexAccessController,
        val deathStorage: NexDeathStorage,
        val playerDeath: PlayerDeath,
    )
}
