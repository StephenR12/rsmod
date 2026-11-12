package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType

@Execution(ExecutionMode.SAME_THREAD)
class SpiderDropsTest {
    @Test
    fun GameTestState.`loaded Spider identities have exact production contracts`() =
        runSpiderTest { deps ->
            assertEquals(setOf(3_017, 3_019), deps.registry.registeredTypeIds())

            with(deps.spider) {
                assertEquals(3_019, id)
                assertEquals("Spider", name)
                assertEquals("Attack", op[1])
                assertEquals(1, size)
                assertEquals(listOf(1, 1, 1, 2, 1, 1), combatStats())
                assertEquals(35, respawnRate)
                assertNull(huntMode)
                assertEquals(
                    constants.elemental_weakness_fire,
                    param(params.elemental_weakness_type),
                )
                assertEquals(50, param(params.elemental_weakness_percent))
            }
            with(deps.giantSpider) {
                assertEquals(3_017, id)
                assertEquals("Giant spider", name)
                assertEquals("Attack", op[1])
                assertEquals(1, size)
                assertEquals(listOf(1, 1, 1, 5, 1, 1), combatStats())
                assertEquals(30, respawnRate)
                assertEquals(huntmodes.constant_melee.id, huntMode)
                assertEquals(5, huntRange)
                assertEquals(
                    constants.elemental_weakness_fire,
                    param(params.elemental_weakness_type),
                )
                assertEquals(50, param(params.elemental_weakness_percent))
            }
        }

    @Test
    fun GameTestState.`both loaded identities have only an independent one in 128 beginner clue`() =
        runSpiderTest { deps ->
            deps.types.forEach { type ->
                random.next = SpiderDrops.BEGINNER_CLUE_DENOMINATOR - 1
                assertEquals(emptyList<Int>(), drops(deps, type).map { it.obj.id })

                random.next = 0
                val rewards = drops(deps, type)
                assertEquals(listOf(objs.clue_scroll_beginner.id), rewards.map { it.obj.id })
                assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, rewards.single().duration)
            }

            random.next = 0
            val npc = spawnNpc(player.coords, deps.spider)
            player.observerUUID = player.uuid
            npc.heroPoints(player, 1)
            deps.death.spawnDrops(npc)
            assertEquals(
                listOf(objs.clue_scroll_beginner.id),
                deps.objRepo.findAll(npc.coords).map { it.type }.toList(),
            )
        }

    @Test
    fun GameTestState.`Spider clues use shared ownership box and capacity policy`() =
        runSpiderTest { deps ->
            player.inv[0] = InvObj(objs.clue_scroll_beginner)
            random.next = 42
            var result = table(deps, deps.spider).resolve(context(deps, deps.spider))
            assertEquals(emptyList<Int>(), result.drops.map { it.obj.id })
            assertEquals(42, random.next)
            assertEquals(42, deps.random.of(SpiderDrops.BEGINNER_CLUE_DENOMINATOR))

            player.clearInv()
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            random.next = 0
            result = table(deps, deps.giantSpider).resolve(context(deps, deps.giantSpider))
            assertEquals(listOf(objs.scroll_box_beginner.id), result.drops.map { it.obj.id })
            assertEquals(ClueInventory.SCROLL_BOX_GROUND_DURATION, result.drops.single().duration)

            player.inv[0] = InvObj(objs.scroll_box_beginner, 2)
            random.next = 0
            result = table(deps, deps.giantSpider).resolve(context(deps, deps.giantSpider))
            assertEquals(emptyList<Int>(), result.drops.map { it.obj.id })
            assertEquals(
                listOf(
                    "You have a sneaking suspicion that you would have received a " +
                        "beginner scroll box."
                ),
                result.messages,
            )
        }

    private fun GameTestState.runSpiderTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(SpiderDrops::class),
            testBody = body,
        )

    private fun GameTestScope.context(deps: Dependencies, type: UnpackedNpcType): NpcDropContext =
        NpcDropContext(spawnNpc(player.coords, type), player, player.coords, deps.random)

    private fun GameTestScope.table(deps: Dependencies, type: UnpackedNpcType) =
        checkNotNull(deps.registry[type])

    private fun GameTestScope.drops(deps: Dependencies, type: UnpackedNpcType) =
        table(deps, type).roll(context(deps, type))

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val death: NpcDeath,
        val objRepo: ObjRepository,
        val npcTypes: NpcTypeList,
        val random: GameRandom,
    ) {
        val spider: UnpackedNpcType
            get() = npcTypes[npcs.spider]

        val giantSpider: UnpackedNpcType
            get() = npcTypes[npcs.giant_spider]

        val types: List<UnpackedNpcType>
            get() = listOf(spider, giantSpider)
    }
}
