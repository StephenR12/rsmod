package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcChampionScrollDropRolls
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropRollResult
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.FreshDropOrigin
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType

@Execution(ExecutionMode.SAME_THREAD)
class ImpDropsTest {
    @Test
    fun GameTestState.`three loaded Imps have exact cache and dated override contract`() =
        runImpTest { deps ->
            val type = deps.type
            assertEquals(5_007, type.id)
            assertEquals("Imp", type.name)
            assertEquals("Attack", type.op[1])
            assertEquals(1, type.size)
            assertEquals(listOf(1, 1, 1, 8, 1, 1), type.combatStats())
            assertEquals(50, type.respawnRate)
            assertEquals(
                constants.elemental_weakness_water,
                type.param(params.elemental_weakness_type),
            )
            assertEquals(10, type.param(params.elemental_weakness_percent))
            assertEquals(setOf(type.id), deps.registry.registeredTypeIds())
            assertEquals(101, varps.quest_points.id)
            assertEquals(1_457, varbits.champions_defeated_imp.id)
            assertEquals(13_453, objs.ensouled_imp_head_initial.id)
            assertEquals(13_454, objs.ensouled_imp_head.id)
            assertEquals(6_803, objs.imp_champion_scroll.id)
        }

    @Test
    fun GameTestState.`free world main table has every exact 128 weight boundary`() =
        runImpTest { deps ->
            player.members = false
            val cases =
                listOf(
                    MainCase(0, objs.black_bead.id),
                    MainCase(5, objs.red_bead.id),
                    MainCase(10, objs.white_bead.id),
                    MainCase(15, objs.yellow_bead.id),
                    MainCase(20, objs.bronze_bolts.id),
                    MainCase(28, objs.blue_wizard_hat.id),
                    MainCase(36, objs.egg.id),
                    MainCase(41, objs.raw_chicken.id),
                    MainCase(46, objs.burnt_bread.id),
                    MainCase(50, objs.burnt_meat.id),
                    MainCase(54, objs.cabbage.id),
                    MainCase(56, objs.bread_dough.id),
                    MainCase(58, objs.bread.id),
                    MainCase(59, objs.cooked_meat.id),
                    MainCase(60, objs.hammer.id),
                    MainCase(68, objs.tinderbox.id),
                    MainCase(73, objs.shears.id),
                    MainCase(77, objs.bucket_empty.id),
                    MainCase(81, objs.bucket_water.id),
                    MainCase(83, objs.jug_empty.id),
                    MainCase(85, objs.jug_water.id),
                    MainCase(87, objs.pot_empty.id),
                    MainCase(89, objs.pot_of_flour.id),
                    MainCase(91, objs.ball_of_wool.id),
                    MainCase(99, objs.mind_talisman.id),
                    MainCase(106, objs.ashes.id),
                    MainCase(112, objs.clay.id),
                    MainCase(116, objs.cadava_berries.id),
                    MainCase(120, objs.grain.id),
                    MainCase(123, objs.chefs_hat.id),
                    MainCase(125, objs.flyer.id),
                    MainCase(127, objs.apothecary_potion.id),
                )

            cases.forEach { case ->
                val sequence = SequenceRandom().apply { next = case.roll }
                val result = deps.resolve(context(deps, sequence))
                assertEquals(
                    listOf(objs.fiendish_ashes.id, case.objId),
                    result.drops.map { it.obj.id },
                    "main roll ${case.roll}",
                )
                assertTrue(result.drops.all { it.obj.count == 1 }, "main roll ${case.roll}")
                assertTrue(result.messages.isEmpty(), "main roll ${case.roll}")
            }
        }

    @Test
    fun GameTestState.`members head is independent and preserves exact fresh origin`() =
        runImpTest { deps ->
            player.members = true
            player.setVarp(varps.quest_points, 0)
            val sequence = SequenceRandom()
            sequence.next = 127
            sequence.then = 0

            val result = deps.resolve(context(deps, sequence))

            assertEquals(
                listOf(
                    objs.fiendish_ashes.id,
                    objs.apothecary_potion.id,
                    objs.ensouled_imp_head_initial.id,
                ),
                result.drops.map { it.obj.id },
            )
            assertEquals(player.coords, FreshDropOrigin.unpack(result.drops.last().obj.vars))
            assertNull(FreshDropOrigin.unpack(result.drops.first().obj.vars))
        }

    @Test
    fun GameTestState.`champion scroll requires membership and 32 quest points`() =
        runImpTest { deps ->
            player.members = true
            player.setVarp(varps.quest_points, 31)
            val ineligible = SequenceRandom()
            ineligible.next = 127
            ineligible.then = ImpDrops.ENSOULED_HEAD_DENOMINATOR - 1
            assertEquals(
                listOf(objs.fiendish_ashes.id, objs.apothecary_potion.id),
                deps.resolve(context(deps, ineligible)).drops.map { it.obj.id },
            )

            player.setVarp(varps.quest_points, 32)
            val eligible = SequenceRandom()
            eligible.next = 127
            eligible.then = ImpDrops.ENSOULED_HEAD_DENOMINATOR - 1
            eligible.then = 0
            val result = deps.resolve(context(deps, eligible))
            assertEquals(
                listOf(
                    objs.fiendish_ashes.id,
                    objs.apothecary_potion.id,
                    objs.imp_champion_scroll.id,
                ),
                result.drops.map { it.obj.id },
            )
            assertEquals(listOf(NpcChampionScrollDropRolls.DROP_MESSAGE), result.messages)
        }

    @Test
    fun GameTestState.`scroll duplicates remain possible until the Imp Champion is defeated`() =
        runImpTest { deps ->
            player.members = true
            player.setVarp(varps.quest_points, 32)
            player.inv[0] = InvObj(objs.imp_champion_scroll)

            val beforeCompletion = championRollSequence()
            val repeat = deps.resolve(context(deps, beforeCompletion))
            assertEquals(objs.imp_champion_scroll.id, repeat.drops.last().obj.id)
            assertEquals(listOf(NpcChampionScrollDropRolls.DROP_MESSAGE), repeat.messages)

            player.setVarBit(varbits.champions_defeated_imp, 1)
            val afterCompletion = championRollSequence()
            val suppressed = deps.resolve(context(deps, afterCompletion))
            assertEquals(
                listOf(objs.fiendish_ashes.id, objs.apothecary_potion.id),
                suppressed.drops.map { it.obj.id },
            )
            assertEquals(listOf(NpcChampionScrollDropRolls.DUPLICATE_MESSAGE), suppressed.messages)
        }

    private fun championRollSequence(): SequenceRandom =
        SequenceRandom().apply {
            next = 127
            then = ImpDrops.ENSOULED_HEAD_DENOMINATOR - 1
            then = 0
        }

    private fun GameTestScope.context(
        deps: Dependencies,
        random: GameRandom = deps.random,
    ): NpcDropContext {
        val npc = spawnNpc(player.coords, deps.type)
        return NpcDropContext(npc, player, npc.coords, random)
    }

    private fun GameTestState.runImpTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(ImpDrops::class),
            testBody = body,
        )

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class MainCase(val roll: Int, val objId: Int)

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val npcTypes: NpcTypeList,
        val random: GameRandom,
    ) {
        val type: UnpackedNpcType
            get() = npcTypes[npcs.imp]

        fun resolve(context: NpcDropContext): NpcDropRollResult =
            checkNotNull(registry[type]).resolve(context)
    }
}
