package org.rsmod.content.skills.smithing.smelting

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.fishing.BarbarianFishingNpcs
import org.rsmod.content.skills.fishing.CageHarpoonFishingNpcs
import org.rsmod.content.skills.fishing.StandardFishing
import org.rsmod.content.skills.fishing.StandardFishingMethods
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class BarbarianSmithingTest {
    @Test
    fun GameTestState.`Otto rod and harpoon lessons require actual catches before completion`() =
        test { d ->
            prepare()
            player.withProtectedAccess {
                d.otto.lesson(this, OttoLesson.HeavyRod)
                d.otto.rod(this)
            }
            assertEquals(1, player.count(OttoObjs.rod))
            assertEquals(1, player.vars[varbits.brut_fishing_r])
            player.inv[1] = InvObj(objs.feather, 2)
            random.next = 0
            val rodSpot =
                spawnNpc(player.coords.translate(1, 0), d.npcs[BarbarianFishingNpcs.ottosGrotto])
            player.withProtectedAccess {
                d.fishing.fish(this, rodSpot, StandardFishingMethods.barbarianRod)
            }
            advance(5)
            assertEquals(2, player.vars[varbits.brut_fishing_r])
            player.ifClose()
            advance()
            player.withProtectedAccess {
                d.otto.lesson(this, OttoLesson.HeavyRod)
                d.otto.lesson(this, OttoLesson.Harpoon)
            }
            assertEquals(3, player.vars[varbits.brut_fishing_r])
            val harpoonSpot =
                spawnNpc(
                    player.coords.translate(0, 1),
                    d.npcs[CageHarpoonFishingNpcs.rarefish_40_53],
                )
            player.withProtectedAccess {
                d.fishing.fish(this, harpoonSpot, StandardFishingMethods.harpoon)
            }
            advance(5)
            assertEquals(2, player.vars[varbits.brut_fishing_s])
            player.ifClose()
            advance()
            player.withProtectedAccess { d.otto.lesson(this, OttoLesson.Harpoon) }
            assertEquals(3, player.vars[varbits.brut_fishing_s])
        }

    @Test
    fun GameTestState
        .`all six metals produce spear and hasta with corresponding logs and exact XP`() =
        test { d ->
            prepare()
            trainSmithing()
            val anvil = placeMapLoc(CoordGrid(2000, 2000), BarbarianAnvilLocs.anvil)
            for (recipe in BarbarianWeapons.all) for (hasta in listOf(false, true)) {
                player.clearInv()
                player.inv[0] = InvObj(objs.hammer)
                player.inv[1] = InvObj(recipe.bar)
                player.inv[2] = InvObj(recipe.logs)
                val xp = player.statMap.getFineXP(stats.smithing)
                player.withProtectedAccess { d.smithing.make(this, anvil, recipe, hasta, 1) }
                advance(5)
                assertEquals(1, player.count(if (hasta) recipe.hasta else recipe.spear))
                assertEquals(0, player.count(recipe.bar))
                assertEquals(0, player.count(recipe.logs))
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.smithing) - xp,
                )
            }
        }

    @Test
    fun GameTestState
        .`ordinary Smithing lesson needs prerequisite then actual local weapons and reporting`() =
        test { d ->
            prepare()
            player.withProtectedAccess { d.otto.lesson(this, OttoLesson.Smithing) }
            assertEquals(0, player.vars[BarbarianSmithingBits.spear])
            player.withProtectedAccess {
                vars[varps.tbwt_main] = 6
                vars[varbits.brut_fishing_r] = 3
                vars[varbits.brut_fishing_s] = 3
                d.otto.lesson(this, OttoLesson.Smithing)
            }
            assertEquals(1, player.vars[BarbarianSmithingBits.spear])
            val anvil = placeMapLoc(CoordGrid(2502, 3485), BarbarianAnvilLocs.anvil)
            player.telejump(anvil.coords.translateZ(1))
            val recipe = BarbarianWeapons.all[0]
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(recipe.bar)
            player.inv[2] = InvObj(recipe.logs)
            player.withProtectedAccess { d.smithing.make(this, anvil, recipe, false, 1) }
            advance(5)
            assertEquals(2, player.vars[BarbarianSmithingBits.spear])
            player.withProtectedAccess { d.otto.lesson(this, OttoLesson.Smithing) }
            assertEquals(3, player.vars[BarbarianSmithingBits.spear])
            assertEquals(1, player.vars[BarbarianSmithingBits.hasta])
            player.inv[1] = InvObj(recipe.bar)
            player.inv[2] = InvObj(recipe.logs)
            player.withProtectedAccess { d.smithing.make(this, anvil, recipe, true, 1) }
            advance(5)
            assertEquals(2, player.vars[BarbarianSmithingBits.hasta])
            player.withProtectedAccess { d.otto.lesson(this, OttoLesson.Smithing) }
            assertEquals(3, player.vars[BarbarianSmithingBits.hasta])
        }

    @Test
    fun GameTestState.`Otto adamant spear and rune hasta award exact local diaries`() = test { d ->
        prepare()
        trainSmithing()
        val village = placeMapLoc(CoordGrid(3082, 3411), BarbarianAnvilLocs.anvil)
        val otto = placeMapLoc(CoordGrid(2502, 3485), BarbarianAnvilLocs.anvil)
        val adamant = BarbarianWeapons.all[4]
        val rune = BarbarianWeapons.all[5]
        for ((anvil, recipe, hasta) in
            listOf(
                Triple(village, adamant, false),
                Triple(otto, adamant, false),
                Triple(otto, rune, true),
            )) {
            player.clearInv()
            player.telejump(anvil.coords.translateZ(1))
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(recipe.bar)
            player.inv[2] = InvObj(recipe.logs)
            player.withProtectedAccess { d.smithing.make(this, anvil, recipe, hasta, 1) }
            advance(5)
            if (anvil == village)
                assertFalse(player.hasCompletedDiaryTask(DiaryTask.SmithOttoAdamantSpear))
        }
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithOttoAdamantSpear))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithOttoRuneHasta))
    }

    @Test
    fun GameTestState.`wrong log low level and lost hammer preserve all material`() = test { d ->
        prepare()
        trainSmithing()
        val anvil = placeMapLoc(CoordGrid(2000, 2000), BarbarianAnvilLocs.anvil)
        val recipe = BarbarianWeapons.all[5]
        player.inv[0] = InvObj(objs.hammer)
        player.inv[1] = InvObj(recipe.bar)
        player.inv[2] = InvObj(objs.logs)
        player.withProtectedAccess { d.smithing.make(this, anvil, recipe, true, 1) }
        advance(5)
        assertEquals(1, player.count(recipe.bar))
        assertEquals(0, player.count(recipe.hasta))
        player.inv[2] = InvObj(recipe.logs)
        player.stats[stats.smithing] = 89
        player.withProtectedAccess { d.smithing.make(this, anvil, recipe, true, 1) }
        advance(5)
        assertEquals(1, player.count(recipe.bar))
        player.stats[stats.smithing] = 90
        player.withProtectedAccess { d.smithing.make(this, anvil, recipe, true, 1) }
        player.inv[0] = null
        advance(5)
        assertEquals(1, player.count(recipe.bar))
        assertEquals(1, player.count(recipe.logs))
        assertEquals(0, player.count(recipe.hasta))
    }

    @Test
    fun GameTestState
        .`Zamorakian conversion charges exact cost discounts elite and reverses freely`() =
        test { d ->
            prepare()
            trainSmithing()
            player.inv[0] = InvObj(OttoObjs.spear)
            player.inv[1] = InvObj(objs.coins, 299999)
            player.withProtectedAccess { d.otto.convert(this, OttoObjs.spear) }
            assertEquals(1, player.count(OttoObjs.spear))
            assertEquals(299999, player.count(objs.coins))
            player.inv[1] = InvObj(objs.coins, 300000)
            player.withProtectedAccess { d.otto.convert(this, OttoObjs.spear) }
            assertEquals(1, player.count(OttoObjs.hasta))
            assertEquals(0, player.count(objs.coins))
            player.withProtectedAccess {
                d.otto.convert(this, OttoObjs.hasta)
                vars[DiaryRegion.Kandarin.tier(DiaryDifficulty.Elite).complete] = 1
            }
            player.inv[1] = InvObj(objs.coins, 150000)
            player.withProtectedAccess { d.otto.convert(this, OttoObjs.spear) }
            assertEquals(1, player.count(OttoObjs.hasta))
            assertEquals(0, player.count(objs.coins))
        }

    @Test
    fun GameTestState
        .`broken dragon hasta repairs without training and farming progresses only after actions`() =
        test { d ->
            prepare()
            player.inv[0] = InvObj(OttoObjs.broken)
            player.withProtectedAccess {
                d.otto.convert(this, OttoObjs.broken)
                d.otto.lesson(this, OttoLesson.Farming)
            }
            assertEquals(1, player.count(OttoObjs.dragon))
            assertEquals(1, player.vars[OttoBits.planting])
            player.withProtectedAccess { d.otto.lesson(this, OttoLesson.Farming) }
            assertEquals(1, player.vars[OttoBits.planting])
            assertEquals(0, player.vars[OttoBits.smashing])
            player.withProtectedAccess {
                vars[OttoBits.planting] = 2
                d.otto.lesson(this, OttoLesson.Farming)
            }
            assertEquals(3, player.vars[OttoBits.planting])
            assertEquals(1, player.vars[OttoBits.smashing])
            assertEquals(1, player.vars[OttoBits.autoSmash])
        }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.smithing] = 99
        player.stats[stats.fishing] = 99
        player.stats[stats.strength] = 99
        player.stats[stats.agility] = 99
        player.stats[stats.farming] = 15
        player.telejump(CoordGrid(2000, 2001))
        for (dx in -8..8 step 8) for (dz in -8..8 step 8) allocZoneCollision(
            CoordGrid(2000 + dx, 2000 + dz)
        )
    }

    private fun GameTestScope.trainSmithing() {
        player.withProtectedAccess {
            vars[varps.tbwt_main] = 6
            vars[BarbarianSmithingBits.spear] = 3
            vars[BarbarianSmithingBits.hasta] = 3
        }
    }

    private fun GameTestState.test(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            Module,
            OttoTrainingScript::class,
            BarbarianSmithingScript::class,
            testBody = body,
        )

    private object Module : AbstractModule() {
        override fun configure() {
            install(SmeltingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val otto: OttoTraining,
        val smithing: BarbarianSmithing,
        val fishing: StandardFishing,
        val npcs: NpcTypeList,
    )
}
