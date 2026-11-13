package org.rsmod.api.player.diary

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varp.VarpType
import org.rsmod.map.CoordGrid

/** Skill result hooks for the dated cache-233.4 task descriptions. Coordinates refer to the source. */
public object SkillDiaryAreas {
    public fun catherby(c: CoordGrid): Boolean = c.inside(2790, 2875, 3410, 3470)
    public fun karamja(c: CoordGrid): Boolean = c.inside(2688, 3007, 2880, 3327)
    public fun piscatoris(c: CoordGrid): Boolean = c.inside(2290, 2375, 3630, 3730)
    public fun kourend(c: CoordGrid): Boolean = c.inside(1216, 1855, 3392, 3967)
    public fun resourceArea(c: CoordGrid): Boolean = c.inside(3174, 3199, 3920, 3944)
    public fun apeAtoll(c: CoordGrid): Boolean = c.inside(2690, 2825, 2690, 2825)
    public fun mosLeHarmless(c: CoordGrid): Boolean = c.inside(3648, 3839, 2880, 3135)
    public fun rellekka(c: CoordGrid): Boolean = c.inside(2600, 2720, 3600, 3730)
    public fun varrock(c: CoordGrid): Boolean = c.inside(3168, 3327, 3328, 3519)
    public fun tirannwn(c: CoordGrid): Boolean = c.inside(2112, 2431, 3072, 3391)
    public fun wilderness(c: CoordGrid): Boolean = c.inside(2944, 3391, 3520, 3967)
    public fun openDesert(c: CoordGrid): Boolean =
        c.inside(3136, 3583, 2688, 3135) && !c.inside(3150, 3200, 2950, 2990) &&
            !c.inside(3315, 3380, 2950, 3015) && !c.inside(3392, 3475, 2875, 2945) &&
            !c.inside(3255, 3345, 2740, 2825)

    public fun morytania(c: CoordGrid): Boolean = c.x in 3392..3839 && (c.z in 3180..3711 || c.z in 9580..10111)
}

private fun CoordGrid.inside(x1: Int, x2: Int, z1: Int, z2: Int, plane: Int = 0): Boolean = level == plane && x in x1..x2 && z in z1..z2
private fun CoordGrid.at(x: Int, z: Int, plane: Int = 0): Boolean = level == plane && this.x == x && this.z == z

public object ProductionDiaryVars : VarpReferences() {
    public val steps: VarpType = find("diary_skill_production_steps")
}
internal object ProductionDiaryBuilder : VarpBuilder() {
    init { build("diary_skill_production_steps") { transmitNever = true } }
}

private fun stepTask(bit: Int): DiaryTask = when (bit) {
    0 -> DiaryTask.CatchCookCatherbyBass
    1 -> DiaryTask.CatchCookPiscatorisMonkfish
    2 -> DiaryTask.CatchCookKourendAnglerfish
    3 -> DiaryTask.CatchCookResourceDarkCrab
    4 -> DiaryTask.ChopBurnTaverleyWillow
    5 -> DiaryTask.ChopBurnApeAtollTeak
    6 -> DiaryTask.ChopBurnApeAtollMahogany
    7 -> DiaryTask.ChopBurnMosMahogany
    8 -> DiaryTask.ChopBurnResourceMagic
    9 -> DiaryTask.ChopBurnFremennikOak
    10 -> DiaryTask.ChopBurnVarrockYew
    11 -> DiaryTask.SmithFletchVarrockRuneDarts
    12 -> DiaryTask.MixSeersSuperantipoison
    13, 14 -> DiaryTask.CraftRellekkaTiara
    15 -> DiaryTask.CraftBarbarianBowl
    22, 23 -> DiaryTask.CreateSeersYewLongbow
    30, 31 -> DiaryTask.CraftLumbridgePowerAmulet
    else -> error("Unknown production diary step: $bit")
}
private fun Player.markStep(bit: Int) {
    if (!members || hasCompletedDiaryTask(stepTask(bit))) return
    val previous = vars[ProductionDiaryVars.steps]
    if (previous and (1 shl bit) != 0) return
    interruptDiaryProductionSequence(stepTask(bit))
    interruptDiaryKillSequence()
    VarPlayerIntMapSetter.set(this, ProductionDiaryVars.steps, previous or (1 shl bit))
}
private fun Player.countStep(shift: Int, task: DiaryTask) {
    if (!members || hasCompletedDiaryTask(task)) return
    val previous = vars[ProductionDiaryVars.steps]
    val count = ((previous ushr shift) and 7) + 1
    interruptDiaryProductionSequence(task)
    interruptDiaryKillSequence()
    VarPlayerIntMapSetter.set(this, ProductionDiaryVars.steps, (previous and (7 shl shift).inv()) or (minOf(5, count) shl shift))
    if (count >= 5) completeDiaryTask(task)
}
private fun Player.finishStep(bit: Int, task: DiaryTask) {
    if (members && vars[ProductionDiaryVars.steps] and (1 shl bit) != 0) {
        completeDiaryTask(task)
        VarPlayerIntMapSetter.set(this, ProductionDiaryVars.steps, vars[ProductionDiaryVars.steps] and (1 shl bit).inv())
    }
}

public fun Player.recordDiaryStall(output: Int, fullLoc: Int, coords: CoordGrid) {
    if (!members) return
    if (fullLoc == 4277 && SkillDiaryAreas.rellekka(coords)) completeDiaryTask(DiaryTask.StealRellekkaFish)
    if (fullLoc == 28823 && coords.inside(1760, 1810, 3590, 3615)) completeDiaryTask(DiaryTask.StealHosidiusFruit)
    if (output == objs.cake.id && fullLoc == 11730 && coords.inside(2645, 2685, 3290, 3330)) completeDiaryTask(DiaryTask.StealArdougneCake)
    if (fullLoc == 635 && SkillDiaryAreas.varrock(coords)) completeDiaryTask(DiaryTask.StealVarrockTea)
    if (fullLoc == 6163 && coords.x in 2816..2943 && coords.z in 10112..10239) completeDiaryTask(DiaryTask.StealKeldagrimStall)
    if (fullLoc == 6162 && coords.x in 2816..2943 && coords.z in 10112..10239) completeDiaryTask(DiaryTask.StealKeldagrimGem)
}

public fun Player.recordDiaryAnvil(output: Int, count: Int, coords: CoordGrid) {
    recordDiaryAdvancedAnvil(output,coords)
    if (!members) return
    if (output == 1121 && (coords.inside(2496, 2591, 3264, 3327) || coords.inside(2540, 2625, 3070, 3120) || coords.inside(2640, 2675, 3140, 3190))) completeDiaryTask(DiaryTask.SmithArdougneMithrilPlatebody)
    if (output == 1121 && coords.at(3409, 2921)) completeDiaryTask(DiaryTask.SmithNardahMithrilPlatebody)
    if (output == 1123 && coords.at(3112, 9689)) completeDiaryTask(DiaryTask.SmithDraynorAdamantPlatebody)
    if (output == 1331 && coords.at(3190, 3938)) completeDiaryTask(DiaryTask.SmithResourceAdamantScimitar)
    if (output == objs.rune_dart_tip.id && count >= 10 && SkillDiaryAreas.varrock(coords)) markStep(11)
}

public fun Player.recordDiaryOre(output: Int, coords: CoordGrid) {
    if (!members) return
    if (output == 13421 && coords.inside(1675, 1725, 3495, 3555)) completeDiaryTask(DiaryTask.DigHosidiusSaltpetre)
    if (output == 13571 && coords.inside(1410, 1455, 3855, 3885)) completeDiaryTask(DiaryTask.MineVolcanicSulphur)
    if (output == 13356 && coords.inside(1410, 1460, 3795, 3860)) completeDiaryTask(DiaryTask.MineLovakiteOre)
    if (output == objs.gold_ore.id && coords.inside(2731, 2735, 3223, 3227)) completeDiaryTask(DiaryTask.MineKaramjaPeninsulaGold)
    if (output == objs.uncut_red_topaz.id && SkillDiaryAreas.karamja(coords)) completeDiaryTask(DiaryTask.MineKaramjaTopaz)
    if (output == objs.mithril_ore.id && (coords.inside(2798, 2807, 4438, 4450) || coords.inside(2783, 2788, 4596, 4599))) completeDiaryTask(DiaryTask.MineAbandonedMithril)
    if (output in setOf(6979, 6981, 6983) && coords.inside(3155, 3185, 2890, 2930)) completeDiaryTask(DiaryTask.MineDesertGranite)
    if (output == objs.clay.id && coords.inside(3398, 3425, 3150, 3169)) countStep(24, DiaryTask.MineFiveDesertClay)
    if (output == objs.gold_ore.id && coords.inside(2937, 2943, 3276, 3280)) completeDiaryTask(DiaryTask.MineCraftingGuildGold)
    if (output == objs.gold_ore.id && coords.inside(2487, 2491, 9907, 9910)) completeDiaryTask(DiaryTask.MineGrandTreeGold)
    if (output == objs.adamantite_ore.id && SkillDiaryAreas.tirannwn(coords)) completeDiaryTask(DiaryTask.MineTirannwnAdamantite)
    if (output == objs.adamantite_ore.id && coords.inside(2375, 2425, 10180, 10235)) countStep(27, DiaryTask.MineFiveJatizsoAdamantite)
    if (output == objs.coal.id && coords.inside(2574, 2590, 3474, 3485)) completeDiaryTask(DiaryTask.MineCoalTrucksCoal)
    if (output == objs.silver_ore.id && coords.inside(2685, 2687, 3702, 3703)) markStep(13)
    if (output == objs.coal.id && coords.inside(2678, 2689, 3700, 3710)) completeDiaryTask(DiaryTask.MineRellekkaCoal)
    if (output == objs.iron_ore.id) {
        if (coords.inside(1275, 1279, 3813, 3816)) completeDiaryTask(DiaryTask.MineKaruulmIron)
        if (coords.inside(3280, 3294, 3363, 3375)) completeDiaryTask(DiaryTask.MineVarrockIron)
        if (coords.inside(2337, 2339, 3639, 3640)) completeDiaryTask(DiaryTask.MinePiscatorisIron)
        if (SkillDiaryAreas.wilderness(coords)) completeDiaryTask(DiaryTask.MineWildIron)
    }
    if (output == objs.mithril_ore.id && SkillDiaryAreas.wilderness(coords)) completeDiaryTask(DiaryTask.MineWildMithril)
}

public fun Player.recordDiaryFish(output: Int, coords: CoordGrid, barehanded: Boolean = false) {
    if (!members) return
    if (coords.inside(2921, 2926, 3175, 3182)) completeDiaryTask(DiaryTask.FishKaramjaPlantation)
    if (output == objs.raw_trout.id && coords.inside(1260, 1320, 3680, 3730)) completeDiaryTask(DiaryTask.FishMolchTrout)
    if (output == objs.raw_mackerel.id && SkillDiaryAreas.catherby(coords)) completeDiaryTask(DiaryTask.FishCatherbyMackerel)
    if (output == objs.raw_salmon.id && coords.inside(3230, 3255, 3230, 3268)) completeDiaryTask(DiaryTask.FishLumbridgeSalmon)
    if (output == objs.raw_trout.id && coords.inside(3095, 3120, 3410, 3440)) completeDiaryTask(DiaryTask.FishBarbarianTrout)
    if (output in setOf(317, 321, 327, 331, 335, 341, 345, 349, 353, 359, 363, 371, 377, 383, 389, 395) && coords.inside(2750, 2795, 3270, 3300)) completeDiaryTask(DiaryTask.FishPlatform)
    if (output == objs.raw_bass.id && SkillDiaryAreas.apeAtoll(coords)) completeDiaryTask(DiaryTask.FishApeAtollBass)
    if (output == objs.raw_lava_eel.id && SkillDiaryAreas.wilderness(coords)) completeDiaryTask(DiaryTask.FishWildLavaEel)
    if (output == objs.leaping_sturgeon.id && coords.inside(2490, 2525, 3480, 3525)) completeDiaryTask(DiaryTask.FishBaxtorianSturgeon)
    if (output == objs.raw_shark.id && barehanded && coords.inside(3470, 3530, 3170, 3245)) completeDiaryTask(DiaryTask.FishBurghBarehandShark)
    if (output == objs.raw_bass.id && SkillDiaryAreas.catherby(coords)) markStep(0)
    if (output == objs.raw_monkfish.id && SkillDiaryAreas.piscatoris(coords)) markStep(1)
    if (output == objs.raw_anglerfish.id && SkillDiaryAreas.kourend(coords)) markStep(2)
    if (output == objs.raw_dark_crab.id && SkillDiaryAreas.resourceArea(coords)) markStep(3)
    if (output == objs.raw_shark.id && SkillDiaryAreas.catherby(coords) && !hasCompletedDiaryTask(DiaryTask.FishCookFiveCatherbySharks)) {
        val state = vars[ProductionDiaryVars.steps]
        val caught = (state ushr 16) and 7
        if (caught < 5) { interruptDiaryProductionSequence(DiaryTask.FishCookFiveCatherbySharks); interruptDiaryKillSequence() }
        VarPlayerIntMapSetter.set(this, ProductionDiaryVars.steps, (state and (7 shl 16).inv()) or (minOf(5, caught + 1) shl 16))
    }
}

public fun Player.recordDiaryCook(output: Int, coords: CoordGrid, range: Boolean, cookingGauntlets: Boolean = false) {
    recordDiaryTrawlerCook(output, coords)
    if (!members) return
    if (output == objs.wild_pie.id && range && (coords.at(3410, 2937) || coords.at(3433, 2885))) completeDiaryTask(DiaryTask.BakeNardahWildPie)
    if (output == objs.summer_pie.id && range && (coords.at(3145, 3453) || coords.at(3143, 3453, 1) || coords.at(3145, 3453, 1))) completeDiaryTask(DiaryTask.BakeCookingGuildSummerPie)
    if (output == objs.thin_snail_meat.id && range && coords.at(3676, 3467)) completeDiaryTask(DiaryTask.MORYTANIA_EASY_COOK_THIN_SNAIL)
    if (output == objs.bass.id && SkillDiaryAreas.catherby(coords)) finishStep(0, DiaryTask.CatchCookCatherbyBass)
    if (output == objs.monkfish.id && SkillDiaryAreas.piscatoris(coords)) finishStep(1, DiaryTask.CatchCookPiscatorisMonkfish)
    if (output == objs.anglerfish.id && SkillDiaryAreas.kourend(coords)) finishStep(2, DiaryTask.CatchCookKourendAnglerfish)
    if (output == objs.dark_crab.id && SkillDiaryAreas.resourceArea(coords)) finishStep(3, DiaryTask.CatchCookResourceDarkCrab)
    if (output == objs.shark.id && range && cookingGauntlets && coords.at(2817, 3444)) {
        val state = vars[ProductionDiaryVars.steps]
        val caught = (state ushr 16) and 7
        val cooked = (state ushr 19) and 7
        if (caught > 0 && cooked < 5 && !hasCompletedDiaryTask(DiaryTask.FishCookFiveCatherbySharks)) {
            interruptDiaryProductionSequence(DiaryTask.FishCookFiveCatherbySharks)
            interruptDiaryKillSequence()
            VarPlayerIntMapSetter.set(this, ProductionDiaryVars.steps, (state and ((7 shl 16) or (7 shl 19)).inv()) or ((caught - 1) shl 16) or ((cooked + 1) shl 19))
            if (cooked + 1 == 5) completeDiaryTask(DiaryTask.FishCookFiveCatherbySharks)
        }
    }
}

public fun Player.recordDiaryWood(output: Int, tree: Int, coords: CoordGrid) {
    if (!members) return
    if (output == objs.teak_logs.id && SkillDiaryAreas.karamja(coords)) completeDiaryTask(DiaryTask.ChopKaramjaTeak)
    if (output == objs.mahogany_logs.id && SkillDiaryAreas.karamja(coords)) completeDiaryTask(DiaryTask.ChopKaramjaMahogany)
    if (output == objs.mahogany_logs.id && coords.inside(1230, 1245, 3760, 3780)) completeDiaryTask(DiaryTask.ChopFarmingGuildMahogany)
    if (output == objs.bark.id && SkillDiaryAreas.morytania(coords)) completeDiaryTask(DiaryTask.ChopHollowBark)
    if (coords.inside(3290, 3320, 3485, 3520) && tree == 3648) completeDiaryTask(DiaryTask.ChopLumberYardDyingTree)
    if (output == objs.magic_logs.id && coords.inside(3340, 3375, 3290, 3335)) completeDiaryTask(DiaryTask.ChopMageTrainingMagic)
    if (output == objs.redwood_logs.id && SkillDiaryAreas.kourend(coords)) completeDiaryTask(DiaryTask.ChopKourendRedwood)
    if (output == objs.teak_logs.id && coords.at(3510, 3073)) completeDiaryTask(DiaryTask.ChopUzerTeak)
    if (output == objs.willow_logs.id && coords.inside(2870, 2950, 3380, 3470)) markStep(4)
    if (output == objs.teak_logs.id && SkillDiaryAreas.apeAtoll(coords)) markStep(5)
    if (output == objs.mahogany_logs.id && SkillDiaryAreas.apeAtoll(coords)) markStep(6)
    if (output == objs.mahogany_logs.id && SkillDiaryAreas.mosLeHarmless(coords)) markStep(7)
    if (output == objs.magic_logs.id && SkillDiaryAreas.resourceArea(coords)) markStep(8)
    if (output == objs.oak_logs.id && SkillDiaryAreas.rellekka(coords)) markStep(9)
    if (output == objs.yew_logs.id && SkillDiaryAreas.varrock(coords)) markStep(10)
    if (output == objs.yew_logs.id && coords.inside(2705, 2716, 3459, 3466)) markStep(22)
}

public fun Player.recordDiaryFire(log: Int, coords: CoordGrid, bow: Boolean = false) {
    if (!members) return
    if (log == objs.yew_logs.id && coords.inside(3439, 3441, 2911, 2920, 1)) completeDiaryTask(DiaryTask.BurnNardahYew)
    if (log == objs.maple_logs.id && bow && coords.inside(2688, 2751, 3456, 3520)) completeDiaryTask(DiaryTask.BurnSeersMapleWithBow)
    if (log == objs.willow_logs.id && coords.inside(2870, 2950, 3380, 3470)) finishStep(4, DiaryTask.ChopBurnTaverleyWillow)
    if (log == objs.teak_logs.id && SkillDiaryAreas.apeAtoll(coords)) finishStep(5, DiaryTask.ChopBurnApeAtollTeak)
    if (log == objs.mahogany_logs.id && SkillDiaryAreas.apeAtoll(coords)) finishStep(6, DiaryTask.ChopBurnApeAtollMahogany)
    if (log == objs.mahogany_logs.id && SkillDiaryAreas.mosLeHarmless(coords)) finishStep(7, DiaryTask.ChopBurnMosMahogany)
    if (log == objs.magic_logs.id && SkillDiaryAreas.resourceArea(coords)) finishStep(8, DiaryTask.ChopBurnResourceMagic)
    if (log == objs.oak_logs.id && SkillDiaryAreas.rellekka(coords)) finishStep(9, DiaryTask.ChopBurnFremennikOak)
    if (log == objs.yew_logs.id && coords.inside(3253, 3260, 3471, 3488, plane = 3)) finishStep(10, DiaryTask.ChopBurnVarrockYew)
}

public fun Player.recordDiaryCraft(output: Int, coords: CoordGrid) {
    if (!members) return
    if (output == objs.unstrung_dragonstone_amulet.id && coords.inside(2338, 2350, 3807, 3815)) completeDiaryTask(DiaryTask.CraftNeitiznotDragonstoneAmulet)
    if (output == objs.unstrung_diamond_amulet.id && LumbridgeDiarySkillArea.contains(coords)) markStep(30)
    if (output == objs.diamond_amulet.id && LumbridgeDiarySkillArea.contains(coords) && vars[ProductionDiaryVars.steps] and (1 shl 30) != 0) {
        VarPlayerIntMapSetter.set(this, ProductionDiaryVars.steps, vars[ProductionDiaryVars.steps] and (1 shl 30).inv())
        markStep(31)
    }
    if (output == objs.tiara.id && SkillDiaryAreas.rellekka(coords)) finishStep(14, DiaryTask.CraftRellekkaTiara)
    if (output == objs.leather_coif.id && coords.inside(3250, 3265, 3255, 3297)) completeDiaryTask(DiaryTask.CraftLumbridgeCoif)
    if (output == objs.black_dhide_body.id && coords.inside(3508, 3515, 3474, 3483)) completeDiaryTask(DiaryTask.CraftCanifisBlackBody)
    if (output in 3327..3343 && output % 2 == 1 && SkillDiaryAreas.morytania(coords) && !coords.inside(3392, 3540, 3270, 3485)) completeDiaryTask(DiaryTask.CraftMorytaniaSnelm)
}

public fun Player.recordDiaryFletch(output: Int, count: Int, coords: CoordGrid) {
    recordDiaryAdvancedFletch(output,coords)
    if (!members) return
    if (output == objs.maple_shortbow.id && coords.inside(2721, 2729, 3490, 3498)) completeDiaryTask(DiaryTask.StringSeersMapleShortbow)
    if (output == objs.dragon_dart.id && count >= 10 && coords.inside(3150, 3190, 3010, 3050)) completeDiaryTask(DiaryTask.FletchBedabinDragonDarts)
    if (output == objs.unstrung_yew_longbow.id && coords.inside(2688, 2751, 3430, 3520) && vars[ProductionDiaryVars.steps] and (1 shl 22) != 0) {
        VarPlayerIntMapSetter.set(this, ProductionDiaryVars.steps, vars[ProductionDiaryVars.steps] and (1 shl 22).inv())
        markStep(23)
    }
    if (output == objs.yew_longbow.id && coords.inside(2688, 2751, 3430, 3520)) finishStep(23, DiaryTask.CreateSeersYewLongbow)
    if (output == objs.oak_shortbow.id && coords.inside(2380, 2495, 3380, 3525)) completeDiaryTask(DiaryTask.StringGnomeOakShortbow)
    if (output == objs.magic_longbow.id && SkillDiaryAreas.tirannwn(coords)) completeDiaryTask(DiaryTask.StringTirannwnMagicLongbow)
    if (output == objs.rune_dart.id && count >= 10 && SkillDiaryAreas.varrock(coords)) finishStep(11, DiaryTask.SmithFletchVarrockRuneDarts)
}

public fun Player.recordDiaryPotion(output: Int, coords: CoordGrid) {
    if (!members) return
    if (output == objs.strength_potion_3.id && (coords.inside(1561, 1571, 3756, 3767) || coords.inside(1564, 1571, 3751, 3755) || coords.inside(1572, 1576, 3756, 3760))) completeDiaryTask(DiaryTask.MixLovakengjStrength)
    if (output == objs.combat_potion_3.id && SkillDiaryAreas.openDesert(coords)) completeDiaryTask(DiaryTask.MixDesertCombatPotion)
    if (output in setOf(objs.anti_venom_1.id, objs.anti_venom_2.id, objs.anti_venom_3.id, objs.anti_venom_4.id) && coords.inside(2730, 2768, 3138, 3172)) completeDiaryTask(DiaryTask.MixHorseShoeAntivenom)
    if (output == objs.saradomin_brew_3.id && coords.inside(3009, 3021, 3352, 3359)) completeDiaryTask(DiaryTask.MixFaladorSaradominBrew)
    if (output == objs.super_combat_potion_3.id && coords.inside(3180, 3189, 3433, 3447)) completeDiaryTask(DiaryTask.MixVarrockSuperCombat)
    if (output == objs.super_defence_3.id && SkillDiaryAreas.rellekka(coords)) completeDiaryTask(DiaryTask.MixFremennikSuperDefence)
    if (output in setOf(7660, 7662, 7664, 7666) && SkillDiaryAreas.morytania(coords)) completeDiaryTask(DiaryTask.MixMorytaniaGuthixBalance)
    if (output == objs.stamina_mix_2.id && coords.inside(2721, 2730, 3490, 3498, plane = 3)) completeDiaryTask(DiaryTask.MixSeersStamina)
    if (output == objs.irit_potion_unf.id && coords.inside(2688, 2879, 3392, 3550)) markStep(12)
    if (output == 181 && coords.inside(2688, 2879, 3392, 3550)) finishStep(12, DiaryTask.MixSeersSuperantipoison)
}

public fun Player.recordDiaryHunter(npc: Int, coords: CoordGrid) {
    if (!members) return
    if (npc == 5551 && coords.inside(3370, 3430, 3020, 3175)) completeDiaryTask(DiaryTask.CatchGoldenWarbler)
    if (npc == 5550 && coords.inside(2680, 2760, 3750, 3845)) completeDiaryTask(DiaryTask.CatchCeruleanTwitch)
    if (npc == 5552 && coords.inside(2280, 2390, 3500, 3650)) completeDiaryTask(DiaryTask.CatchCopperLongtail)
    if (npc == 2910 && coords.inside(1465, 1510, 3485, 3525)) completeDiaryTask(DiaryTask.CatchKourendChinchompa)
}

public fun Player.recordDiaryCannonballs(coords: CoordGrid) {
    if (members && coords.at(3688, 3478)) completeDiaryTask(DiaryTask.SmithPhasmatysCannonballs)
}

public fun Player.recordDiarySmelt(output: Int, coords: CoordGrid) {
    recordDiaryAdvancedSmelt(output,coords)
    if (members && output == objs.adamantite_bar.id && coords.at(1384, 3828)) completeDiaryTask(DiaryTask.SmeltForsakenAdamant)
    if (members && output == objs.silver_bar.id && SkillDiaryAreas.rellekka(coords) && vars[ProductionDiaryVars.steps] and (1 shl 13) != 0) {
        if (!hasCompletedDiaryTask(DiaryTask.CraftRellekkaTiara)) { interruptDiaryProductionSequence(DiaryTask.CraftRellekkaTiara); interruptDiaryKillSequence() }
        VarPlayerIntMapSetter.set(this, ProductionDiaryVars.steps, (vars[ProductionDiaryVars.steps] and (1 shl 13).inv()) or (1 shl 14))
    }
}

public fun Player.recordDiaryPottery(output: Int, coords: CoordGrid) {
    if (!members) return
    if (output == objs.unfired_bowl.id && (coords.at(3087, 3409) || coords.at(3087, 3410))) markStep(15)
    if (output == objs.bowl_empty.id && coords.at(3084, 3407)) finishStep(15, DiaryTask.CraftBarbarianBowl)
}

/** Called only after an actual level-4 jewellery enchant consumes its runes and changes the amulet. */
public fun Player.recordDiaryAmuletEnchant(output: Int, coords: CoordGrid) {
    if (output == objs.amulet_of_power.id && LumbridgeDiarySkillArea.contains(coords)) finishStep(31, DiaryTask.CraftLumbridgePowerAmulet)
}
