package org.rsmod.content.skills.runecraft

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

public object RunecraftLocs : LocReferences() {
    val airAltar = find("air_altar")
    val mindAltar = find("mind_altar")
    val waterAltar = find("water_altar")
    val earthAltar = find("earth_altar")
    val fireAltar = find("fire_altar")
    val bodyAltar = find("body_altar")
    val cosmicAltar = find("cosmic_altar")
    val lawAltar = find("law_altar")
    val natureAltar = find("nature_altar")
    val chaosAltar = find("chaos_altar")
    val deathAltar = find("death_altar")
    val astralAltar = find("astral_altar")
    val wrathAltar = find("wrath_altar")
    val bloodAltar = find("blood_altar")
    val kourendBloodAltar = find("archeus_altar_blood")
    val darkAltar = find("archeus_altar_dark")
    val kourendSoulAltar = find("archeus_altar_soul")
}

public object RunecraftObjs : ObjReferences() {
    val airTalisman = find("air_talisman")
    val waterTalisman = find("water_talisman")
    val earthTalisman = find("earth_talisman")
    val fireTalisman = find("fire_talisman")
    val runeEssence = find("blankrune")
    val pureEssence = find("blankrune_high")
    val daeyaltEssence = find("blankrune_daeyalt")
    val guardianEssence = find("gotr_guardian_essence")
    val smallPouch = find("rcu_pouch_small")
    val mediumPouch = find("rcu_pouch_medium")
    val mediumPouchDegraded = find("rcu_pouch_medium_degrade")
    val largePouch = find("rcu_pouch_large")
    val largePouchDegraded = find("rcu_pouch_large_degrade")
    val giantPouch = find("rcu_pouch_giant")
    val giantPouchDegraded = find("rcu_pouch_giant_degrade")
    val colossalPouch = find("rcu_pouch_colossal")
    val colossalPouchDegraded = find("rcu_pouch_colossal_degrade")
    val warpedExtract = find("scar_extract_warped")
    val twistedExtract = find("scar_extract_twisted")
    val mangledExtract = find("scar_extract_mangled")
    val scarredExtract = find("scar_extract_scarred")
    val bloodEssenceInactive = find("blood_essence_inactive")
    val bloodEssenceActive = find("blood_essence_active")
    val denseEssenceBlock = find("arceuus_essence_block")
    val darkEssenceBlock = find("arceuus_essence_block_dark")
    val darkEssenceFragments = find("bigblankrune")
    val raimentsBoots = find("boots_of_the_eye")
    val raimentsHat = find("hat_of_the_eye")
    private val raimentsHatRed = find("hat_of_the_eye_red")
    private val raimentsHatGreen = find("hat_of_the_eye_green")
    private val raimentsHatBlue = find("hat_of_the_eye_blue")
    val raimentsTorso = find("robe_top_of_the_eye")
    private val raimentsTorsoRed = find("robe_top_of_the_eye_red")
    private val raimentsTorsoGreen = find("robe_top_of_the_eye_green")
    private val raimentsTorsoBlue = find("robe_top_of_the_eye_blue")
    val raimentsLegsBase = find("robe_bottom_of_the_eye")
    private val raimentsLegsRed = find("robe_bottom_of_the_eye_red")
    private val raimentsLegsGreen = find("robe_bottom_of_the_eye_green")
    private val raimentsLegsBlue = find("robe_bottom_of_the_eye_blue")
    val raimentsHats = listOf(raimentsHat, raimentsHatRed, raimentsHatGreen, raimentsHatBlue)
    val raimentsTorsos =
        listOf(raimentsTorso, raimentsTorsoRed, raimentsTorsoGreen, raimentsTorsoBlue)
    val raimentsLegs =
        listOf(raimentsLegsBase, raimentsLegsRed, raimentsLegsGreen, raimentsLegsBlue)
    val aetherCatalyst = find("cosmic_soul_catalyst")
    val aetherRune = find("aetherrune")
}

public object RunecraftSeqs : SeqReferences() {
    val craft = find("human_runecraft")
    val chiselDarkEssence = find("arceuus_chisel_essenceblock")
}

public object RunecraftQueues : QueueReferences() {
    val darkAltarVenerate = find("runecraft_dark_altar_venerate")
}

public object RunecraftSpotanims : SpotanimReferences() {
    val craft = find("runecrafting")
}

public object RunecraftVarps : VarpReferences() {
    val bindingNecklace = find("necklace_of_binding")
    val mediumPouchDegradation = find("rcu_pouch_degradation_med")
    val largePouchDegradation = find("rcu_pouch_degradation_large")
    val giantPouchDegradation = find("rcu_pouch_degradation_giant")
}

public object RunecraftVarbits : VarBitReferences() {
    val smallPouchAmount = find("small_essence_pouch")
    val mediumPouchAmount = find("medium_essence_pouch")
    val largePouchAmount = find("large_essence_pouch")
    val giantPouchAmount = find("giant_essence_pouch")
    val colossalPouchAmount = find("colossal_essence_pouch")
    val smallPouchType = find("small_essence_pouch_type")
    val mediumPouchType = find("medium_essence_pouch_type")
    val largePouchType = find("large_essence_pouch_type")
    val giantPouchType = find("giant_essence_pouch_type")
    val colossalPouchType = find("colossal_essence_pouch_type")
    val colossalPouchDegradation = find("rcu_pouch_degradation_colossal")
    val lunarDiplomacy = find("lunar_quest_main")
    val mourningEndsPartTwo = find("mourning_quest_main")
    val sinsOfTheFather = find("myq5")
    val dragonSlayerTwo = find("ds2")
    val desertTreasureTwo = find("dt2")
}
