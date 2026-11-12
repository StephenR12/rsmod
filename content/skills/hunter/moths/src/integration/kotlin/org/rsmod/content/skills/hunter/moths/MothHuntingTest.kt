package org.rsmod.content.skills.hunter.moths

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class MothHuntingTest {
    @Test
    fun GameTestState.`cache 233 moth identities operations rates and scripts are pinned`() =
        runMothTest { deps ->
            assertEquals(12770, MothNpcs.sunlight.id)
            assertEquals(12771, MothNpcs.moonlight.id)
            assertEquals(12772, MothNpcs.moonlightLowWander.id)
            assertEquals(12773, MothNpcs.moonlightNoClick.id)
            assertEquals(10010, MothObjs.butterflyNet.id)
            assertEquals(11259, MothObjs.magicButterflyNet.id)
            assertEquals(10012, MothObjs.emptyJar.id)
            assertEquals(28890, MothObjs.sunlightJar.id)
            assertEquals(28893, MothObjs.moonlightJar.id)
            assertEquals(5209, MothSeqs.catch.id)
            assertEquals(5213, MothSeqs.openJar.id)
            assertEquals(5214, MothSeqs.butterflyOut.id)

            assertEquals(
                listOf("Catch", null, null, null, null),
                deps.npcs[MothNpcs.sunlight].op.toList(),
            )
            assertEquals(
                listOf("Catch", null, null, null, null),
                deps.npcs[MothNpcs.moonlight].op.toList(),
            )
            assertEquals(
                listOf("Catch", null, null, null, null),
                deps.npcs[MothNpcs.moonlightLowWander].op.toList(),
            )
            assertEquals(
                listOf(null, null, null, null, null),
                deps.npcs[MothNpcs.moonlightNoClick].op.toList(),
            )
            assertEquals(100, deps.npcs[MothNpcs.sunlight].respawnRate)
            assertEquals(100, deps.npcs[MothNpcs.moonlight].respawnRate)
            assertEquals(100, deps.npcs[MothNpcs.moonlightLowWander].respawnRate)
            assertEquals("Release", deps.objs[MothObjs.sunlightJar].iop[3])
            assertEquals("Release", deps.objs[MothObjs.moonlightJar].iop[3])
            assertEquals(Wearpos.RightHand.slot, deps.objs[MothObjs.butterflyNet].wearpos1)
            assertEquals(Wearpos.RightHand.slot, deps.objs[MothObjs.magicButterflyNet].wearpos1)

            assertEquals(listOf(65, 75), MothSpeciesList.all.map { it.level })
            assertEquals(listOf(75, 85), MothSpeciesList.all.map { it.barehandedLevel })
            assertEquals(listOf(74.0, 84.0), MothSpeciesList.all.map { it.xp })
            assertEquals(listOf(20, 0), MothSpeciesList.all.map { it.regularRate.low })
            assertEquals(listOf(296, 276), MothSpeciesList.all.map { it.regularRate.high })
            assertEquals(listOf(40, 20), MothSpeciesList.all.map { it.enhancedRate.low })
            assertEquals(listOf(316, 286), MothSpeciesList.all.map { it.enhancedRate.high })

            for (npc in
                listOf(MothNpcs.sunlight, MothNpcs.moonlight, MothNpcs.moonlightLowWander)) {
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, npc.id.toLong()))
            }
            assertFalse(
                deps.events.contains(
                    NpcEvents.Op1::class.java,
                    MothNpcs.moonlightNoClick.id.toLong(),
                )
            )
            assertTrue(
                deps.events.contains(
                    HeldObjEvents.Op4::class.java,
                    MothObjs.sunlightJar.id.toLong(),
                )
            )
            assertTrue(
                deps.events.contains(
                    HeldObjEvents.Op4::class.java,
                    MothObjs.moonlightJar.id.toLong(),
                )
            )
            assertEquals(MothSeqs.catch.id, deps.seqs[MothSeqs.catch].id)
            assertEquals(MothSeqs.openJar.id, deps.seqs[MothSeqs.openJar].id)
            assertEquals(MothSeqs.butterflyOut.id, deps.seqs[MothSeqs.butterflyOut].id)
        }

    @Test
    fun GameTestState.`membership visible level and right hand select exact catch method`() =
        runMothTest { deps ->
            preparePlayer(level = 99)
            player.members = false
            assertNull(deps.hunting.method(player, MothSpeciesList.sunlight))

            player.members = true
            player.setCurrentLevel(stats.hunter, 64)
            assertNull(deps.hunting.method(player, MothSpeciesList.sunlight))
            player.setCurrentLevel(stats.hunter, 65)
            assertNull(deps.hunting.method(player, MothSpeciesList.sunlight))

            player.worn[Wearpos.RightHand.slot] = InvObj(MothObjs.butterflyNet)
            assertEquals(
                MothCatchMethod.RegularNet,
                deps.hunting.method(player, MothSpeciesList.sunlight),
            )
            player.worn[Wearpos.RightHand.slot] = InvObj(MothObjs.magicButterflyNet)
            assertEquals(
                MothCatchMethod.MagicNet,
                deps.hunting.method(player, MothSpeciesList.sunlight),
            )
            player.worn[Wearpos.RightHand.slot] = null
            player.setCurrentLevel(stats.hunter, 75)
            assertEquals(
                MothCatchMethod.Barehanded,
                deps.hunting.method(player, MothSpeciesList.sunlight),
            )
            assertNull(deps.hunting.method(player, MothSpeciesList.moonlight))
            player.setCurrentLevel(stats.hunter, 85)
            assertEquals(
                MothCatchMethod.Barehanded,
                deps.hunting.method(player, MothSpeciesList.moonlight),
            )
        }

    @Test
    fun GameTestState.`net catch atomically replaces a jar in a full inventory`() =
        runMothTest { deps ->
            preparePlayer(level = 99)
            player.fillInv()
            player.inv[0] = InvObj(MothObjs.emptyJar)
            player.worn[Wearpos.RightHand.slot] = InvObj(MothObjs.butterflyNet)
            val startXp = player.statMap.getFineXP(stats.hunter)
            val npc = spawnNpc(player.coords.translate(1, 0), deps.npcs[MothNpcs.sunlight])
            random.next = 0

            player.withProtectedAccess { deps.hunting.catch(this, npc) }
            advance(MothHunting.CATCH_TICKS)

            assertEquals(0, player.inv.count(MothObjs.emptyJar))
            assertEquals(1, player.inv.count(MothObjs.sunlightJar))
            assertEquals(740, player.statMap.getFineXP(stats.hunter) - startXp)
            assertFalse(npc.isValidTarget())
        }

    @Test
    fun GameTestState.`direct sunlight catch restores stats hitpoints and run but not prayer`() =
        runMothTest { deps ->
            preparePlayer(level = 99)
            player.worn[Wearpos.RightHand.slot] = InvObj(MothObjs.magicButterflyNet)
            player.setBaseLevel(stats.attack, 80)
            player.setCurrentLevel(stats.attack, 10)
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 50)
            player.setBaseLevel(stats.prayer, 99)
            player.setCurrentLevel(stats.prayer, 20)
            player.runEnergy = 1_234
            val npc = spawnNpc(player.coords.translate(1, 0), deps.npcs[MothNpcs.sunlight])
            random.next = 0

            player.withProtectedAccess { deps.hunting.catch(this, npc) }
            advance(MothHunting.CATCH_TICKS)

            assertEquals(32, player.statMap.getCurrentLevel(stats.attack).toInt())
            assertEquals(58, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(20, player.statMap.getCurrentLevel(stats.prayer).toInt())
            // The action grants 500 energy; the ordinary game-cycle processor regenerates 15.
            assertEquals(1_749, player.runEnergy)
            assertEquals(0, player.inv.count(MothObjs.sunlightJar))
        }

    @Test
    fun GameTestState.`sunlight effect floors base percentage and caps every documented stat`() =
        runMothTest { _ ->
            preparePlayer(level = 99)
            for (stat in MothEffects.sunlightRestoredStats) {
                player.setBaseLevel(stat, 80)
                player.setCurrentLevel(stat, 70)
            }

            player.withProtectedAccess { MothEffects.apply(this, MothEffect.Sunlight, false) }

            for (stat in MothEffects.sunlightRestoredStats) {
                assertEquals(80, player.statMap.getCurrentLevel(stat).toInt(), stat.toString())
            }
        }

    @Test
    fun GameTestState.`barehanded moonlight catch ignores empty jar and restores flat prayer`() =
        runMothTest { deps ->
            preparePlayer(level = 85)
            player.inv[0] = InvObj(MothObjs.emptyJar)
            player.setBaseLevel(stats.prayer, 99)
            player.setCurrentLevel(stats.prayer, 50)
            val npc =
                spawnNpc(player.coords.translate(1, 0), deps.npcs[MothNpcs.moonlightLowWander])
            random.next = 0

            player.withProtectedAccess { deps.hunting.catch(this, npc) }
            advance(MothHunting.CATCH_TICKS)

            assertEquals(1, player.inv.count(MothObjs.emptyJar))
            assertEquals(0, player.inv.count(MothObjs.moonlightJar))
            assertEquals(72, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertFalse(npc.isValidTarget())
        }

    @Test
    fun GameTestState.`failed catch preserves npc inventory experience and effects`() =
        runMothTest { deps ->
            preparePlayer(level = 65)
            player.inv[0] = InvObj(MothObjs.emptyJar)
            player.worn[Wearpos.RightHand.slot] = InvObj(MothObjs.butterflyNet)
            player.setBaseLevel(stats.attack, 80)
            player.setCurrentLevel(stats.attack, 10)
            player.runEnergy = 1_234
            val before = player.inv.toList()
            val startXp = player.statMap.getFineXP(stats.hunter)
            val npc = spawnNpc(player.coords.translate(1, 0), deps.npcs[MothNpcs.sunlight])
            random.next = 99

            player.withProtectedAccess { deps.hunting.catch(this, npc) }
            advance(MothHunting.CATCH_TICKS)

            assertEquals(before, player.inv.toList())
            assertEquals(startXp, player.statMap.getFineXP(stats.hunter))
            assertEquals(10, player.statMap.getCurrentLevel(stats.attack).toInt())
            // A failed action receives only ordinary game-cycle regeneration.
            assertEquals(1_249, player.runEnergy)
            assertTrue(npc.isValidTarget())
        }

    @Test
    fun GameTestState.`release atomically returns jar and never grants direct catch run energy`() =
        runMothTest { deps ->
            preparePlayer(level = 99)
            player.fillInv()
            player.inv[0] = InvObj(MothObjs.sunlightJar)
            player.setBaseLevel(stats.attack, 80)
            player.setCurrentLevel(stats.attack, 10)
            player.runEnergy = 1_234

            player.withProtectedAccess { deps.hunting.release(this, 0) }

            assertEquals(0, player.inv.count(MothObjs.sunlightJar))
            assertEquals(1, player.inv.count(MothObjs.emptyJar))
            assertEquals(32, player.statMap.getCurrentLevel(stats.attack).toInt())
            assertEquals(1_234, player.runEnergy)

            player.inv[0] = InvObj(MothObjs.moonlightJar)
            player.setBaseLevel(stats.prayer, 99)
            player.setCurrentLevel(stats.prayer, 90)
            player.withProtectedAccess { deps.hunting.release(this, 0) }
            assertEquals(0, player.inv.count(MothObjs.moonlightJar))
            assertEquals(1, player.inv.count(MothObjs.emptyJar))
            assertEquals(99, player.statMap.getCurrentLevel(stats.prayer).toInt())
        }

    @Test
    fun GameTestState.`successful catch uses cache respawn rate and returns npc at spawn`() =
        runMothTest { deps ->
            preparePlayer(level = 99)
            player.worn[Wearpos.RightHand.slot] = InvObj(MothObjs.butterflyNet)
            val spawn = player.coords.translate(1, 0)
            val npc = spawnNpc(spawn, deps.npcs[MothNpcs.moonlight]).apply { respawns = true }
            random.next = 0

            player.withProtectedAccess { deps.hunting.catch(this, npc) }
            advance(MothHunting.CATCH_TICKS)

            assertFalse(npc.isValidTarget())
            // Despawn schedules 100 cycles from the action; one cycle has elapsed on assertion.
            assertEquals(99, npc.lifecycleRespawnCycle - mapClock.cycle)
            advanceUntil(predicate = { npc.isValidTarget() })
            assertEquals(spawn, npc.coords)
        }

    private fun GameTestScope.preparePlayer(level: Int) {
        val origin = CoordGrid(3200, 3200)
        allocZoneCollision(origin)
        player.placeAt(origin)
        player.observerUUID = 1L
        player.members = true
        player.setBaseLevel(stats.hunter, level.coerceAtMost(99))
        player.setCurrentLevel(stats.hunter, level)
    }

    private fun GameTestState.runMothTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, MothScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            bind(MothHunting::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val hunting: MothHunting,
        val events: EventBus,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val seqs: SeqTypeList,
        val npcRepo: NpcRepository,
    )
}
