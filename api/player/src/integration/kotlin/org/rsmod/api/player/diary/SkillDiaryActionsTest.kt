package org.rsmod.api.player.diary

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SkillDiaryActionsTest {
    @Test
    fun GameTestState.`stall diaries check successful reward family and city`() = runGameTest {
        player.members = true
        val ardy = CoordGrid(2667, 3310)
        player.recordDiaryStall(objs.bread.id, 11730, ardy)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.StealArdougneCake))
        player.recordDiaryStall(objs.cake.id, 11730, CoordGrid(3200, 3200))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.StealArdougneCake))
        player.recordDiaryStall(objs.cake.id, 11730, ardy)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.StealArdougneCake))
        player.recordDiaryStall(objs.cup_of_tea.id, 635, CoordGrid(3268, 3410))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.StealVarrockTea))
        player.recordDiaryStall(objs.bread.id, 6163, CoordGrid(2889, 10139))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.StealKeldagrimStall))
    }

    @Test
    fun GameTestState.`thin snail requires exact Port Phasmatys range rather than a nearby fire`() = runGameTest {
        player.members = true
        player.recordDiaryCook(objs.thin_snail_meat.id, CoordGrid(3676, 3467), range = false)
        player.recordDiaryCook(objs.thin_snail_meat.id, CoordGrid(3675, 3467), range = true)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MORYTANIA_EASY_COOK_THIN_SNAIL))
        player.recordDiaryCook(objs.thin_snail_meat.id, CoordGrid(3676, 3467), range = true)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.MORYTANIA_EASY_COOK_THIN_SNAIL))
    }

    @Test
    fun GameTestState.`bass task requires local catching before local successful cooking`() = runGameTest {
        player.members = true
        val catch = CoordGrid(2840, 3430)
        val range = CoordGrid(2817, 3444)
        player.recordDiaryCook(objs.bass.id, range, range = true)
        player.recordDiaryFish(objs.raw_bass.id, CoordGrid(3200, 3200))
        player.recordDiaryCook(objs.bass.id, range, range = true)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchCookCatherbyBass))
        player.recordDiaryFish(objs.raw_bass.id, catch)
        player.recordDiaryCook(objs.bass.id, CoordGrid(3200, 3200), range = true)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchCookCatherbyBass))
        player.recordDiaryCook(objs.bass.id, range, range = true)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchCookCatherbyBass))
    }

    @Test
    fun GameTestState.`five sharks need five catches gauntlets and the east bank range`() = runGameTest {
        player.members = true
        repeat(5) { player.recordDiaryFish(objs.raw_shark.id, CoordGrid(2845, 3430)) }
        repeat(5) { player.recordDiaryCook(objs.shark.id, CoordGrid(2818, 3455), range = true, cookingGauntlets = true) }
        repeat(5) { player.recordDiaryCook(objs.shark.id, CoordGrid(2817, 3444), range = true, cookingGauntlets = false) }
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.FishCookFiveCatherbySharks))
        repeat(4) { player.recordDiaryCook(objs.shark.id, CoordGrid(2817, 3444), range = true, cookingGauntlets = true) }
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.FishCookFiveCatherbySharks))
        player.recordDiaryCook(objs.shark.id, CoordGrid(2817, 3444), range = true, cookingGauntlets = true)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.FishCookFiveCatherbySharks))
    }

    @Test
    fun GameTestState.`smithing ten dart tips must precede fletching ten darts within Varrock`() = runGameTest {
        player.members = true
        val city = CoordGrid(3189, 3425)
        player.recordDiaryFletch(objs.rune_dart.id, 10, city)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.SmithFletchVarrockRuneDarts))
        player.recordDiaryAnvil(objs.rune_dart_tip.id, 10, city)
        player.recordDiaryFletch(objs.rune_dart.id, 9, city)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.SmithFletchVarrockRuneDarts))
        player.recordDiaryFletch(objs.rune_dart.id, 10, city)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithFletchVarrockRuneDarts))
    }

    @Test
    fun GameTestState.`local wood must precede matching fire and bow-only task rejects tinderbox`() = runGameTest {
        player.members = true
        val ape = CoordGrid(2765, 2725)
        player.recordDiaryFire(objs.teak_logs.id, ape)
        player.recordDiaryWood(objs.teak_logs.id, 9036, ape)
        player.recordDiaryFire(objs.mahogany_logs.id, ape)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopBurnApeAtollTeak))
        player.recordDiaryFire(objs.teak_logs.id, ape)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.ChopBurnApeAtollTeak))
        player.recordDiaryFire(objs.maple_logs.id, CoordGrid(2720, 3500))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.BurnSeersMapleWithBow))
        player.recordDiaryFire(objs.maple_logs.id, CoordGrid(2720, 3500), bow = true)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.BurnSeersMapleWithBow))
    }

    @Test
    fun GameTestState.`superantipoison needs a locally mixed unfinished potion and stamina needs bank roof`() = runGameTest {
        assertTrue(DiaryRegion.Kandarin.tasks(DiaryDifficulty.Medium)[1].text.contains("Superantipoison"))
        assertTrue(DiaryTask.MixSeersSuperantipoison.difficulty == DiaryDifficulty.Medium)
        player.members = true
        val seers = CoordGrid(2725, 3493)
        player.recordDiaryPotion(objs.superantipoison_3.id, seers)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MixSeersSuperantipoison))
        player.recordDiaryPotion(objs.irit_potion_unf.id, seers)
        player.recordDiaryPotion(objs.superantipoison_3.id, seers)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.MixSeersSuperantipoison))
        player.recordDiaryPotion(objs.stamina_mix_2.id, seers)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MixSeersStamina))
        player.recordDiaryPotion(objs.stamina_mix_2.id, CoordGrid(2725, 3493, 3))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.MixSeersStamina))
    }

    @Test
    fun GameTestState.`coif body and strung bows require their named production areas`() = runGameTest {
        player.members = true
        player.recordDiaryCraft(objs.leather_coif.id, CoordGrid(3255, 3275))
        player.recordDiaryCraft(objs.black_dhide_body.id, CoordGrid(3511, 3479))
        player.recordDiaryFletch(objs.oak_shortbow.id, 1, CoordGrid(2435, 3430))
        player.recordDiaryFletch(objs.magic_longbow.id, 1, CoordGrid(2290, 3180))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CraftLumbridgeCoif))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CraftCanifisBlackBody))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.StringGnomeOakShortbow))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.StringTirannwnMagicLongbow))
    }

    @Test
    fun GameTestState.`free-world and upstairs mining cannot complete surface ore tasks`() = runGameTest {
        player.members = false
        player.recordDiaryOre(objs.iron_ore.id, CoordGrid(3285, 3369))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MineVarrockIron))
        player.members = true
        player.recordDiaryOre(objs.iron_ore.id, CoordGrid(3285, 3369, 1))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MineVarrockIron))
        player.recordDiaryOre(objs.iron_ore.id, CoordGrid(3285, 3369))
        player.recordDiaryOre(objs.mithril_ore.id, CoordGrid(3180, 3930))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.MineVarrockIron))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.MineWildMithril))
    }
    @Test
    fun GameTestState.`Rellekka tiara requires local silver mining then local smelting before crafting`() = runGameTest {
        player.members = true
        val furnace = CoordGrid(2644, 3675)
        player.recordDiaryCraft(objs.tiara.id, furnace)
        player.recordDiarySmelt(objs.silver_bar.id, furnace)
        player.recordDiaryCraft(objs.tiara.id, furnace)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CraftRellekkaTiara))
        player.recordDiaryOre(objs.silver_ore.id, CoordGrid(2686, 3703))
        player.recordDiaryCraft(objs.tiara.id, furnace)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CraftRellekkaTiara))
        player.recordDiarySmelt(objs.silver_bar.id, CoordGrid(3200, 3200))
        player.recordDiaryCraft(objs.tiara.id, furnace)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CraftRellekkaTiara))
        player.recordDiarySmelt(objs.silver_bar.id, furnace)
        player.recordDiaryCraft(objs.tiara.id, furnace)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CraftRellekkaTiara))
    }

    @Test
    fun GameTestState.`Barbarian Village bowl needs local shaping before successful firing at its oven`() = runGameTest {
        player.members = true
        val oven = CoordGrid(3084, 3407)
        player.recordDiaryPottery(objs.bowl_empty.id, oven)
        player.recordDiaryPottery(objs.unfired_bowl.id, CoordGrid(3200, 3200))
        player.recordDiaryPottery(objs.bowl_empty.id, oven)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CraftBarbarianBowl))
        player.recordDiaryPottery(objs.unfired_bowl.id, CoordGrid(3087, 3409))
        player.recordDiaryPottery(objs.bowl_empty.id, CoordGrid(3200, 3200))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CraftBarbarianBowl))
        player.recordDiaryPottery(objs.bowl_empty.id, oven)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CraftBarbarianBowl))
    }

    @Test
    fun GameTestState.`five-clay and Jatizso counters reject nearby rocks and require all five successful rewards`() = runGameTest {
        player.members = true
        repeat(5) { player.recordDiaryOre(objs.clay.id, CoordGrid(3400, 3100)) }
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MineFiveDesertClay))
        repeat(4) { player.recordDiaryOre(objs.clay.id, CoordGrid(3420, 3163)) }
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MineFiveDesertClay))
        player.recordDiaryOre(objs.clay.id, CoordGrid(3400, 3155))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.MineFiveDesertClay))
        repeat(5) { player.recordDiaryOre(objs.adamantite_ore.id, CoordGrid(2493, 9910)) }
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MineFiveJatizsoAdamantite))
        repeat(4) { player.recordDiaryOre(objs.adamantite_ore.id, CoordGrid(2402, 10189)) }
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MineFiveJatizsoAdamantite))
        player.recordDiaryOre(objs.adamantite_ore.id, CoordGrid(2380, 10214))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.MineFiveJatizsoAdamantite))
    }

    @Test
    fun GameTestState.`Seers yew longbow needs local tree cut carving and stringing in order`() = runGameTest {
        player.members = true
        val bank = CoordGrid(2725, 3492)
        player.recordDiaryFletch(objs.unstrung_yew_longbow.id, 1, bank)
        player.recordDiaryFletch(objs.yew_longbow.id, 1, bank)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CreateSeersYewLongbow))
        player.recordDiaryWood(objs.yew_logs.id, 10822, CoordGrid(2705, 3459))
        player.recordDiaryFletch(objs.yew_longbow.id, 1, bank)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CreateSeersYewLongbow))
        player.recordDiaryFletch(objs.unstrung_yew_longbow.id, 1, bank)
        player.recordDiaryFletch(objs.yew_longbow.id, 1, CoordGrid(3200, 3200))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CreateSeersYewLongbow))
        player.recordDiaryFletch(objs.yew_longbow.id, 1, bank)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CreateSeersYewLongbow))
        player.recordDiaryFletch(objs.maple_shortbow.id, 1, bank)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.StringSeersMapleShortbow))
    }

    @Test
    fun GameTestState.`power amulet requires all three Lumbridge stages and rejects upstairs enchantment`() = runGameTest {
        player.members = true
        val ground = CoordGrid(3226, 3255)
        player.recordDiaryAmuletEnchant(objs.amulet_of_power.id, ground)
        player.recordDiaryCraft(objs.diamond_amulet.id, ground)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CraftLumbridgePowerAmulet))
        player.recordDiaryCraft(objs.unstrung_diamond_amulet.id, ground)
        player.recordDiaryAmuletEnchant(objs.amulet_of_power.id, ground)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CraftLumbridgePowerAmulet))
        player.recordDiaryCraft(objs.diamond_amulet.id, ground)
        player.recordDiaryAmuletEnchant(objs.amulet_of_power.id, CoordGrid(3208, 3218, 1))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CraftLumbridgePowerAmulet))
        player.recordDiaryAmuletEnchant(objs.amulet_of_power.id, ground)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CraftLumbridgePowerAmulet))
    }

    @Test
    fun GameTestState.`oven and anvil product tasks require their actual regional source`() = runGameTest {
        player.members = true
        player.recordDiaryAnvil(1121, 1, CoordGrid(3200, 3200))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.SmithNardahMithrilPlatebody))
        player.recordDiaryAnvil(1121, 1, CoordGrid(3409, 2921))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithNardahMithrilPlatebody))
        player.recordDiaryCook(objs.wild_pie.id, CoordGrid(3433, 2885), range = false)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.BakeNardahWildPie))
        player.recordDiaryCook(objs.wild_pie.id, CoordGrid(3433, 2885), range = true)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.BakeNardahWildPie))
        player.recordDiaryCook(objs.summer_pie.id, CoordGrid(3143, 3453, 1), range = true)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.BakeCookingGuildSummerPie))
        player.recordDiaryCraft(objs.unstrung_dragonstone_amulet.id, CoordGrid(2345, 3812))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CraftNeitiznotDragonstoneAmulet))
    }

    @Test
    fun GameTestState.`partial production interrupts a Wilderness kill streak while repeated completed steps do not`() = runGameTest {
        player.members = true
        player.recordDiaryKill(DiaryKillSequence.WildernessHard, 0)
        player.recordDiaryOre(objs.clay.id, CoordGrid(3400, 3155))
        player.recordDiaryKill(DiaryKillSequence.WildernessHard, 1)
        player.recordDiaryKill(DiaryKillSequence.WildernessHard, 2)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.WildernessHardBosses))
        player.recordDiaryKill(DiaryKillSequence.WildernessHard, 0)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.WildernessHardBosses))
    }

    @Test fun GameTestState.`remaining successful skill rows require their actual region or floor`() = runGameTest {
        player.members=true
        player.recordDiaryOre(objs.gold_ore.id,CoordGrid(2733,3227))
        player.recordDiaryOre(objs.uncut_red_topaz.id,CoordGrid(2825,2998))
        player.recordDiaryWood(objs.teak_logs.id,9036,CoordGrid(2828,3082))
        player.recordDiaryWood(objs.mahogany_logs.id,9034,CoordGrid(1235,3772))
        player.recordDiaryWood(objs.bark.id,1322,CoordGrid(3514,3290))
        player.recordDiaryPotion(objs.strength_potion_3.id,CoordGrid(1566,3760))
        player.recordDiarySmelt(objs.adamantite_bar.id,CoordGrid(1384,3828))
        player.recordDiaryFire(objs.yew_logs.id,CoordGrid(3440,2915))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.BurnNardahYew))
        player.recordDiaryFire(objs.yew_logs.id,CoordGrid(3440,2915,1))
        for(task in listOf(DiaryTask.MineKaramjaPeninsulaGold,DiaryTask.MineKaramjaTopaz,DiaryTask.ChopKaramjaTeak,DiaryTask.ChopFarmingGuildMahogany,DiaryTask.ChopHollowBark,DiaryTask.MixLovakengjStrength,DiaryTask.SmeltForsakenAdamant,DiaryTask.BurnNardahYew)) assertTrue(player.hasCompletedDiaryTask(task),task.name)
    }

}
