package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropRollResult
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcGemDropObjs
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos

@Execution(ExecutionMode.SAME_THREAD)
class FerociousSpiritDropsTest {
    @Test
    fun GameTestState.`cache 233 spirit and every local drop object identity are pinned`() =
        runSpiritTest { deps ->
            assertEquals(5_566, deps.type.id)
            assertEquals("Ferocious barbarian spirit", deps.type.name)
            assertEquals(setOf(5_566), deps.registry.registeredTypeIds())
            assertEquals(139, varps.legends_quest.id)
            assertEquals(75, FerociousSpiritDrops.LEGENDS_QUEST_COMPLETE)
            assertEquals(128, FerociousSpiritDrops.MAIN_ROLL_WEIGHT)
            assertEquals(
                listOf(1_428, 1_430, 11_373, 1_432, 5_295, 2_354, 2_360, 2_362, 2_364, 533, 1_462),
                listOf(
                        FerociousSpiritObjs.mithrilMace,
                        FerociousSpiritObjs.adamantMace,
                        FerociousSpiritObjs.mithrilHasta,
                        FerociousSpiritObjs.runeMace,
                        FerociousSpiritObjs.ranarrSeed,
                        FerociousSpiritObjs.notedSteelBar,
                        FerociousSpiritObjs.notedMithrilBar,
                        FerociousSpiritObjs.notedAdamantiteBar,
                        FerociousSpiritObjs.notedRuniteBar,
                        FerociousSpiritObjs.notedBigBones,
                        FerociousSpiritObjs.natureTalisman,
                    )
                    .map { it.id },
            )
            assertEquals(
                listOf(830, 987, 985, 1_247, 2_366, 1_249),
                listOf(
                        NpcGemDropObjs.runeJavelin,
                        NpcGemDropObjs.loopHalfOfKey,
                        NpcGemDropObjs.toothHalfOfKey,
                        NpcGemDropObjs.runeSpear,
                        NpcGemDropObjs.shieldLeftHalf,
                        NpcGemDropObjs.dragonSpear,
                    )
                    .map { it.id },
            )
            assertEquals(
                listOf(
                    2_572,
                    11_988,
                    11_986,
                    11_984,
                    11_982,
                    11_980,
                    12_785,
                    20_790,
                    20_789,
                    20_788,
                    20_787,
                    20_786,
                ),
                FerociousSpiritObjs.ringsOfWealth.map { it.id },
            )
            listOf(
                    FerociousSpiritObjs.notedSteelBar,
                    FerociousSpiritObjs.notedMithrilBar,
                    FerociousSpiritObjs.notedAdamantiteBar,
                    FerociousSpiritObjs.notedRuniteBar,
                    FerociousSpiritObjs.notedBigBones,
                )
                .forEach {
                    assertTrue(deps.objTypes[it].isCert, "noted obj ${it.id}")
                    assertTrue(deps.objTypes[it].isStackable, "stackable noted obj ${it.id}")
                }
        }

    @Test
    fun GameTestState.`all 128 main-table outcomes preserve exact branch weights and quantities`() =
        runSpiritTest { deps ->
            player.setVarp(varps.legends_quest, 0)
            for (roll in 0 until FerociousSpiritDrops.MAIN_ROLL_WEIGHT) {
                val random =
                    SequenceRandom().apply {
                        next = roll
                        then = 5
                        then = 5
                    }
                val drops = deps.resolve(context(deps, random)).drops.map { it.obj }
                assertEquals(expectedMain(roll), drops.map { it.id to it.count }, "main roll $roll")
            }
        }

    @Test
    fun GameTestState.`ordinary gem and mega-rare tables have exact boundaries`() =
        runSpiritTest { deps ->
            player.setVarp(varps.legends_quest, FerociousSpiritDrops.LEGENDS_QUEST_COMPLETE)
            val common =
                listOf(
                    GemCase(0, null),
                    GemCase(62, null),
                    GemCase(63, objs.uncut_sapphire.id),
                    GemCase(94, objs.uncut_sapphire.id),
                    GemCase(95, objs.uncut_emerald.id),
                    GemCase(110, objs.uncut_emerald.id),
                    GemCase(111, objs.uncut_ruby.id),
                    GemCase(118, objs.uncut_ruby.id),
                    GemCase(119, FerociousSpiritObjs.natureTalisman.id),
                    GemCase(121, FerociousSpiritObjs.natureTalisman.id),
                    GemCase(122, objs.uncut_diamond.id),
                    GemCase(123, objs.uncut_diamond.id),
                    GemCase(124, NpcGemDropObjs.runeJavelin.id, 5),
                    GemCase(125, NpcGemDropObjs.loopHalfOfKey.id),
                    GemCase(126, NpcGemDropObjs.toothHalfOfKey.id),
                )
            common.forEach { case ->
                assertGem(deps, case.gemRoll, megaRoll = 0, case.objId, case.count)
            }
            assertGem(deps, 127, 0, null)
            assertGem(deps, 127, 112, null)
            assertGem(deps, 127, 113, NpcGemDropObjs.runeSpear.id)
            assertGem(deps, 127, 120, NpcGemDropObjs.runeSpear.id)
            assertGem(deps, 127, 121, NpcGemDropObjs.shieldLeftHalf.id)
            assertGem(deps, 127, 124, NpcGemDropObjs.shieldLeftHalf.id)
            assertGem(deps, 127, 125, NpcGemDropObjs.dragonSpear.id)
            assertGem(deps, 127, 127, NpcGemDropObjs.dragonSpear.id)
        }

    @Test
    fun GameTestState.`ring of wealth removes both nothing entries`() = runSpiritTest { deps ->
        player.setVarp(varps.legends_quest, FerociousSpiritDrops.LEGENDS_QUEST_COMPLETE)
        player.worn[Wearpos.Ring.slot] = InvObj(FerociousSpiritObjs.ringOfWealthI3)

        assertGem(deps, 0, 0, objs.uncut_sapphire.id)
        assertGem(deps, 31, 0, objs.uncut_sapphire.id)
        assertGem(deps, 32, 0, objs.uncut_emerald.id)
        assertGem(deps, 47, 0, objs.uncut_emerald.id)
        assertGem(deps, 48, 0, objs.uncut_ruby.id)
        assertGem(deps, 55, 0, objs.uncut_ruby.id)
        assertGem(deps, 56, 0, FerociousSpiritObjs.natureTalisman.id)
        assertGem(deps, 58, 0, FerociousSpiritObjs.natureTalisman.id)
        assertGem(deps, 59, 0, objs.uncut_diamond.id)
        assertGem(deps, 60, 0, objs.uncut_diamond.id)
        assertGem(deps, 61, 0, NpcGemDropObjs.runeJavelin.id, 5)
        assertGem(deps, 62, 0, NpcGemDropObjs.loopHalfOfKey.id)
        assertGem(deps, 63, 0, NpcGemDropObjs.toothHalfOfKey.id)
        assertGem(deps, 64, 0, NpcGemDropObjs.runeSpear.id)
        assertGem(deps, 64, 7, NpcGemDropObjs.runeSpear.id)
        assertGem(deps, 64, 8, NpcGemDropObjs.shieldLeftHalf.id)
        assertGem(deps, 64, 11, NpcGemDropObjs.shieldLeftHalf.id)
        assertGem(deps, 64, 12, NpcGemDropObjs.dragonSpear.id)
        assertGem(deps, 64, 14, NpcGemDropObjs.dragonSpear.id)
    }

    @Test
    fun GameTestState.`pre-Legends mega slot becomes the map talisman`() = runSpiritTest { deps ->
        player.setVarp(varps.legends_quest, FerociousSpiritDrops.LEGENDS_QUEST_COMPLETE - 1)
        assertGem(deps, 127, 0, FerociousSpiritObjs.natureTalisman.id)

        player.worn[Wearpos.Ring.slot] = InvObj(FerociousSpiritObjs.ringOfWealth5)
        assertGem(deps, 64, 0, FerociousSpiritObjs.natureTalisman.id)
    }

    private fun expectedMain(roll: Int): List<Pair<Int, Int>> =
        when (roll) {
            in 0..4 -> one(FerociousSpiritObjs.mithrilMace.id)
            in 5..8 -> one(FerociousSpiritObjs.adamantMace.id)
            in 9..10 -> one(FerociousSpiritObjs.mithrilHasta.id)
            11 -> one(FerociousSpiritObjs.runeMace.id)
            in 12..19 -> one(objs.death_rune.id, 5)
            in 20..23 -> one(objs.adamant_throwing_knife.id, 20)
            in 24..27 -> one(objs.adamant_dart_p.id, 20)
            in 28..31 -> one(objs.rune_arrow.id, 10)
            in 32..35 -> one(objs.runite_bolts.id, 10)
            in 36..45 -> listOf(objs.grimy_guam_leaf.id to 1, objs.grimy_guam_leaf.id to 1)
            in 46..50 -> one(FerociousSpiritObjs.ranarrSeed.id)
            in 51..66 -> one(objs.prayer_mix_1.id)
            in 67..74 -> one(objs.prayer_mix_2.id)
            in 75..78 -> one(objs.antifire_mix_1.id)
            in 79..82 -> one(objs.antifire_mix_2.id)
            in 83..85 -> one(objs.superattack_mix_2.id)
            in 86..88 -> one(objs.super_defence_mix_2.id)
            in 89..90 -> one(objs.super_strength_mix_1.id)
            in 91..92 -> one(objs.super_strength_mix_2.id)
            in 93..94 -> one(objs.superattack_mix_1.id)
            in 95..96 -> one(objs.super_defence_mix_1.id)
            97 -> one(objs.antipoison_supermix_2.id)
            98 -> one(objs.fishing_mix_2.id)
            in 99..100 -> one(objs.yew_logs.id, 3)
            in 101..102 -> one(FerociousSpiritObjs.notedSteelBar.id, 2)
            in 103..104 -> one(FerociousSpiritObjs.notedMithrilBar.id, 2)
            105 -> one(FerociousSpiritObjs.notedAdamantiteBar.id)
            106 -> one(FerociousSpiritObjs.notedRuniteBar.id)
            in 107..113 -> one(FerociousSpiritObjs.notedBigBones.id, 10)
            in 114..119 -> one(objs.shark.id, 3)
            in 120..127 -> emptyList()
            else -> error("Outside main table: $roll")
        }

    private fun one(id: Int, count: Int = 1): List<Pair<Int, Int>> = listOf(id to count)

    private fun GameTestScope.assertGem(
        deps: Dependencies,
        gemRoll: Int,
        megaRoll: Int,
        objId: Int?,
        count: Int = 1,
    ) {
        val random =
            SequenceRandom().apply {
                next = 120
                then = gemRoll
                then = megaRoll
            }
        val drops = deps.resolve(context(deps, random)).drops.map { it.obj }
        assertEquals(
            if (objId == null) emptyList() else listOf(objId to count),
            drops.map { it.id to it.count },
        )
    }

    private fun GameTestScope.context(deps: Dependencies, random: GameRandom): NpcDropContext {
        val npc = spawnNpc(player.coords, deps.type)
        return NpcDropContext(npc, player, npc.coords, random)
    }

    private fun GameTestState.runSpiritTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(FerociousSpiritDrops::class),
            testBody = body,
        )

    private data class GemCase(val gemRoll: Int, val objId: Int?, val count: Int = 1)

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val npcTypes: NpcTypeList,
        val objTypes: ObjTypeList,
    ) {
        val type
            get() = npcTypes[npcs.brut_angry_spirit_bad]

        fun resolve(context: NpcDropContext): NpcDropRollResult =
            checkNotNull(registry[type]).resolve(context)
    }
}
