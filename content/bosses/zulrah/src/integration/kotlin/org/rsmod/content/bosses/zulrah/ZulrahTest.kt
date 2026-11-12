package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.magic.spell.utility.standard.LunarPoisonCure
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript

@Execution(ExecutionMode.SAME_THREAD)
class ZulrahTest {
    @Test
    fun `all rotations begin ranged and contain melee magic and jad phases`() {
        assertEquals(4, ZulrahRotations.all.size)
        ZulrahRotations.all.forEach { rotation ->
            assertEquals(ZulrahForm.Ranged, rotation.first().form)
            assertTrue(rotation.any { it.form == ZulrahForm.Melee })
            assertTrue(rotation.any { it.form == ZulrahForm.Magic })
            assertEquals(1, rotation.count { it.jad })
            assertTrue(rotation.all { it.attacks > 0 })
        }
    }

    @Test
    fun GameTestState.`private encounter spawns a 500 hitpoint owner-bound boss`() =
        runZulrahTest { deps ->
            player.members = true
            player.withProtectedAccess { deps.controller.enter(this) }

            val session = requireNotNull(deps.controller.sessionFor(player))
            val boss = requireNotNull(session.boss)
            assertTrue(player.coords != ZulrahController.OUTSIDE)
            assertEquals(player, deps.controller.ownerFor(boss))
            assertEquals(ZulrahController.HITPOINTS, boss.hitpoints)
            assertEquals(ZulrahController.HITPOINTS, boss.baseHitpointsLvl)
            assertEquals(session.phase.form.npc.id, boss.id)
            assertFalse(session.completed)
        }

    @Test
    fun GameTestState.`phase attack budget queues one deterministic transition`() =
        runZulrahTest { deps ->
            player.members = true
            player.withProtectedAccess { deps.controller.enter(this) }
            val session = requireNotNull(deps.controller.sessionFor(player))
            val boss = requireNotNull(session.boss)

            repeat(session.phase.attacks) { deps.controller.consumeBossAttack(boss) }

            assertTrue(session.transitioning)
            assertEquals(0, session.attacksRemaining)
            assertTrue(org.rsmod.api.config.refs.queues.generic_queue1 in boss.queueList)
        }

    @Test
    fun GameTestState.`every form shares guaranteed scales and two main rolls`() =
        runZulrahTest(scripts = arrayOf(ZulrahDrops::class)) { deps ->
            ZulrahNpcs.bosses.forEach { bossType ->
                random.next = 100
                val npc = spawnNpc(player.coords, deps.npcTypes[bossType])
                val drops =
                    requireNotNull(deps.dropTables[bossType])
                        .roll(NpcDropContext(npc, player, player.coords, deps.random))
                assertEquals(ZulrahObjs.scales.id, drops.first().obj.id)
                assertTrue(drops.first().obj.count in 100..299)
                assertTrue(drops.size >= 3)
            }
        }

    @Test
    fun `drop subtables retain their exact weights`() {
        assertEquals(
            ZulrahDropTable.COMMON_WEIGHT,
            ZulrahDropTable.commonEntries.sumOf { it.weight },
        )
        assertEquals(ZulrahDropTable.SHARK_WEIGHT, ZulrahDropTable.sharkEntries.sumOf { it.weight })
        assertEquals(
            ZulrahDropTable.MUTAGEN_WEIGHT,
            ZulrahDropTable.mutagenEntries.sumOf { it.weight },
        )
        assertEquals(ZulrahDropTable.RARE_WEIGHT, ZulrahDropTable.rareEntries.sumOf { it.weight })
    }

    @Test
    fun GameTestState.`mutagens and pet are reachable at their exact nested rolls`() =
        runZulrahTest(scripts = arrayOf(ZulrahDrops::class)) { deps ->
            val npc = spawnNpc(player.coords, deps.npcTypes[ZulrahNpcs.ranged])
            val table = requireNotNull(deps.dropTables[ZulrahNpcs.ranged])

            random.next = 100
            random.then = 4
            random.then = 68
            random.then = 5_244
            random.then = 0
            random.then = 1
            random.then = 1
            val mutagenDrops = table.roll(NpcDropContext(npc, player, player.coords, deps.random))
            assertTrue(mutagenDrops.any { it.obj.id == ZulrahObjs.tanzaniteMutagen.id })

            random.next = 100
            random.then = 4
            random.then = 68
            random.then = 5_254
            random.then = 0
            random.then = 1
            random.then = 1
            val secondMutagenDrops =
                table.roll(NpcDropContext(npc, player, player.coords, deps.random))
            assertTrue(secondMutagenDrops.any { it.obj.id == ZulrahObjs.magmaMutagen.id })

            random.next = 100
            random.then = 0
            random.then = 0
            random.then = 1
            random.then = 0
            val petDrops = table.roll(NpcDropContext(npc, player, player.coords, deps.random))
            assertTrue(petDrops.any { it.obj.id == ZulrahObjs.petSnakeling.id })
        }

    @Test
    fun GameTestState.`successful Zulrah damage applies venom but a zero does not`() =
        runZulrahTest { deps ->
            assertEquals(0, player.vars[varps.poison])
            assertFalse(deps.combat.envenom(player, damage = 0))
            assertEquals(0, player.vars[varps.poison])

            assertTrue(deps.combat.envenom(player, damage = 1))
            assertEquals(LunarPoisonCure.VENOM_THRESHOLD, player.vars[varps.poison])
        }

    private fun GameTestState.runZulrahTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>> = emptyArray(),
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private data class Dependencies
    @Inject
    constructor(
        val controller: ZulrahController,
        val combat: ZulrahCombat,
        val npcTypes: NpcTypeList,
        val dropTables: NpcDropTableRegistry,
        val random: GameRandom,
    )
}
