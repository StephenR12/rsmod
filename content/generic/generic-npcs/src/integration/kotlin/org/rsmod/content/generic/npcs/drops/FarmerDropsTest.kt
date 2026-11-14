package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.clue.Cache233EasyClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.NpcDrop
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType

@Execution(ExecutionMode.SAME_THREAD)
class FarmerDropsTest {
    @Test
    fun GameTestState.`five loaded Farmers have exact cache and wiki combat contracts`() =
        runFarmerTest { deps ->
            assertEquals(FARMER_IDS, deps.registry.registeredTypeIds())
            deps.farmerTypes.forEach { type ->
                assertEquals("Attack", type.op[1])
                assertEquals("Pickpocket", type.op[2])
                assertEquals("Farmer", type.name)
                assertEquals(1, type.size)
                assertEquals(listOf(3, 4, 8, 12, 1, 1), type.combatStats())
                assertEquals(25, type.respawnRate)
                assertEquals(6, type.param(params.attackrate))
                assertEquals(categories.attacktype_stab, type.param(params.npc_attack_type))
                assertEquals(5, type.param(params.attack_stab))
                assertEquals(6, type.param(params.melee_strength))
                assertNull(type.huntMode)
                assertNull(type.paramOrNull(params.elemental_weakness_type))
                assertSame(deps.table, deps.registry[type])
            }
        }

    @Test
    fun GameTestState.`members main table has every exact 128 weight boundary`() =
        runFarmerTest { deps ->
            player.members = true
            val cases =
                listOf(
                    MainCase(0, objs.earth_rune.id, 4),
                    MainCase(2, objs.fire_rune.id, 6),
                    MainCase(4, objs.mind_rune.id, 9),
                    MainCase(6, objs.chaos_rune.id, 2),
                    MainCase(7, objs.grimy_guam_leaf.id, nestedRoll = 0),
                    MainCase(18, objs.potato_seed.id, nestedRoll = 0, quantityRoll = 1),
                    MainCase(45, objs.coins.id, 3),
                    MainCase(83, objs.coins.id, 25),
                    MainCase(84, null),
                    MainCase(112, objs.fishing_bait.id),
                    MainCase(117, objs.rake.id),
                    MainCase(120, objs.earth_talisman.id),
                    MainCase(122, objs.gardening_boots.id),
                    MainCase(124, objs.seed_dibber.id),
                    MainCase(126, objs.secateurs.id),
                    MainCase(127, objs.watering_can_8.id),
                )

            cases.forEach { case ->
                val result = deps.table.roll(context(deps, misses(case)))
                assertMain(case, result)
            }
        }

    @Test
    fun GameTestState.`allotment seed subtable has every exact 128 weight boundary and quantity`() =
        runFarmerTest { deps ->
            player.members = true
            val cases =
                listOf(
                    SeedCase(0, objs.potato_seed.id, 4),
                    SeedCase(64, objs.onion_seed.id, 3),
                    SeedCase(96, objs.cabbage_seed.id, 3),
                    SeedCase(112, objs.tomato_seed.id, 2),
                    SeedCase(120, objs.sweetcorn_seed.id, 2),
                    SeedCase(124, objs.strawberry_seed.id, 1, quantityRoll = null),
                    SeedCase(126, objs.watermelon_seed.id, 1, quantityRoll = null),
                    SeedCase(127, objs.snape_grass_seed.id, 3),
                )

            cases.forEach { case ->
                val random =
                    SequenceRandom().apply {
                        next = 18
                        then = case.roll
                        case.quantityRoll?.let { then = it }
                        then = FarmerDrops.BEGINNER_CLUE_DENOMINATOR - 1
                        then = FarmerDrops.EASY_CLUE_DENOMINATOR - 1
                    }
                val result = deps.table.roll(context(deps, random))
                assertEquals(listOf(objs.bones.id, case.objId), result.map { it.obj.id })
                assertEquals(case.count, result.last().obj.count, "seed roll ${case.roll}")
            }
        }

    @Test
    fun GameTestState.`free table has every exact 128 weight boundary`() = runFarmerTest { deps ->
        player.members = false
        val cases =
            listOf(
                MainCase(0, objs.bronze_med_helm.id),
                MainCase(2, objs.iron_dagger.id),
                MainCase(3, objs.bronze_arrow.id, 7),
                MainCase(6, objs.earth_rune.id, 4),
                MainCase(8, objs.fire_rune.id, 6),
                MainCase(10, objs.mind_rune.id, 9),
                MainCase(12, objs.chaos_rune.id, 2),
                MainCase(13, objs.coins.id, 3),
                MainCase(51, objs.coins.id, 10),
                MainCase(74, objs.coins.id, 5),
                MainCase(83, objs.coins.id, 15),
                MainCase(87, objs.coins.id, 25),
                MainCase(88, null),
                MainCase(118, objs.fishing_bait.id),
                MainCase(123, objs.copper_ore.id),
                MainCase(125, objs.earth_talisman.id),
                MainCase(127, objs.cabbage.id),
            )

        cases.forEach { case ->
            val random =
                SequenceRandom().apply {
                    next = case.roll
                    then = FarmerDrops.BEGINNER_CLUE_DENOMINATOR - 1
                }
            val result = deps.table.roll(context(deps, random))
            assertMain(case, result)
        }
    }

    @Test
    fun GameTestState.`beginner and Easy clues are independent ordered and membership exact`() =
        runFarmerTest { deps ->
            player.members = true
            val members =
                SequenceRandom().apply {
                    next = 127
                    then = 0
                    then = 0
                    then = Cache233EasyClueSteps.SIZE - 1
                }
            val memberDrops = deps.table.roll(context(deps, members))
            assertEquals(
                listOf(objs.bones.id, objs.watering_can_8.id, objs.clue_scroll_beginner.id, 31_268),
                memberDrops.map { it.obj.id },
            )
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, memberDrops[2].duration)
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, memberDrops[3].duration)

            player.members = false
            val free =
                SequenceRandom().apply {
                    next = 127
                    then = 0
                }
            val freeDrops = deps.table.roll(context(deps, free))
            assertEquals(
                listOf(objs.bones.id, objs.cabbage.id, objs.clue_scroll_beginner.id),
                freeDrops.map { it.obj.id },
            )
        }

    private fun assertMain(case: MainCase, drops: List<NpcDrop>) {
        assertEquals(
            listOfNotNull(objs.bones.id, case.objId),
            drops.map { it.obj.id },
            "main roll ${case.roll}",
        )
        if (case.objId != null) {
            assertEquals(case.count, drops.last().obj.count, "main roll ${case.roll}")
        }
    }

    private fun misses(case: MainCase): SequenceRandom =
        SequenceRandom().apply {
            next = case.roll
            case.nestedRoll?.let { then = it }
            case.quantityRoll?.let { then = it }
            then = FarmerDrops.BEGINNER_CLUE_DENOMINATOR - 1
            then = FarmerDrops.EASY_CLUE_DENOMINATOR - 1
        }

    private fun GameTestScope.context(deps: Dependencies, random: GameRandom): NpcDropContext =
        NpcDropContext(
            npc = spawnNpc(player.coords, deps.farmerTypes.first()),
            hero = player,
            coords = player.coords,
            random = random,
        )

    private fun GameTestState.runFarmerTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(FarmerDrops::class),
            testBody = body,
        )

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class MainCase(
        val roll: Int,
        val objId: Int? = null,
        val count: Int = 1,
        val nestedRoll: Int? = null,
        val quantityRoll: Int? = null,
    )

    private data class SeedCase(
        val roll: Int,
        val objId: Int,
        val count: Int,
        val quantityRoll: Int? = count,
    )

    private data class Dependencies
    @Inject
    constructor(val registry: NpcDropTableRegistry, val npcTypes: NpcTypeList) {
        val farmerTypes: List<UnpackedNpcType>
            get() = FarmerDrops.farmerTypes.map { npcTypes[it] }

        val table: NpcDropTable
            get() = checkNotNull(registry[farmerTypes.first()])
    }

    private companion object {
        val FARMER_IDS: Set<Int> = setOf(3_114, 3_244, 11_919, 11_920, 11_921)
    }
}
