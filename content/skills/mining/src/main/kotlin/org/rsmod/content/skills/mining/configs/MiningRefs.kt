package org.rsmod.content.skills.mining.configs

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences

object MiningLocs : LocReferences() {
    val copper1 = find("copperrock1")
    val copper2 = find("copperrock2")
    val tin1 = find("tinrock1")
    val tin2 = find("tinrock2")
    val clay1 = find("clayrock1")
    val clay2 = find("clayrock2")
    val iron1 = find("ironrock1")
    val iron2 = find("ironrock2")
    val coal1 = find("coalrock1")
    val coal2 = find("coalrock2")
    val silver1 = find("silverrock1")
    val silver2 = find("silverrock2")
    val gold1 = find("goldrock1")
    val gold2 = find("goldrock2")
    val mithril1 = find("mithrilrock1")
    val mithril2 = find("mithrilrock2")
    val adamantite1 = find("adamantiterock1")
    val adamantite2 = find("adamantiterock2")
    val runite1 = find("runiterock1")
    val runite2 = find("runiterock2")
    val blurite1 = find("blurite_rock_1")
    val blurite2 = find("blurite_rock_2")
    val gemRock1 = find("gemrock1")
    val gemRock2 = find("gemrock")

    val denseRunestone = find("arceuus_runestone_base_mine")
    val depletedRunestone = find("arceuus_runestone_base_depleted")
    val denseRunestoneOne = find("arceuus_runestone_base_1")
    val denseRunestoneTwo = find("arceuus_runestone_base_2")

    val empty1 = find("rocks1")
    val empty2 = find("rocks2")
    val empty3 = find("rocks3")
}

object MiningObjs : ObjReferences() {
    val bronzePickaxe = find("bronze_pickaxe")
    val ironPickaxe = find("iron_pickaxe")
    val steelPickaxe = find("steel_pickaxe")
    val blackPickaxe = find("black_pickaxe")
    val mithrilPickaxe = find("mithril_pickaxe")
    val adamantPickaxe = find("adamant_pickaxe")
    val runePickaxe = find("rune_pickaxe")
    val gildedPickaxe = find("trail_gilded_pickaxe")
    val dragonPickaxe = find("dragon_pickaxe")
    val dragonPickaxeUpgraded = find("dragon_pickaxe_pretty")
    val dragonPickaxeZalcano = find("zalcano_pickaxe")
    val dragonPickaxeTrailblazer = find("trailblazer_pickaxe_no_infernal")
    val dragonPickaxeTrailblazerReloaded = find("trailblazer_reloaded_pickaxe_no_infernal")
    val infernalPickaxe = find("infernal_pickaxe")
    val infernalPickaxeUncharged = find("infernal_pickaxe_empty")
    val infernalPickaxeTrailblazer = find("trailblazer_pickaxe")
    val infernalPickaxeTrailblazerUncharged = find("trailblazer_pickaxe_empty")
    val infernalPickaxeTrailblazerReloaded = find("trailblazer_reloaded_pickaxe")
    val infernalPickaxeTrailblazerReloadedUncharged = find("trailblazer_reloaded_pickaxe_empty")
    val thirdAgePickaxe = find("3a_pickaxe")
    val crystalPickaxe = find("crystal_pickaxe")
    val crystalPickaxeInactive = find("crystal_pickaxe_inactive")
    val chisel = find("chisel")
    val denseEssenceBlock = find("arceuus_essence_block")
}

object MiningVarbits : VarBitReferences() {
    val denseRunestoneOne = find("arceuus_runestone_1")
    val denseRunestoneTwo = find("arceuus_runestone_2")
    val denseRunestones = find("arceuus_runestones")
}

object MiningSeqs : SeqReferences() {
    val bronze = find("human_mining_bronze_pickaxe")
    val iron = find("human_mining_iron_pickaxe")
    val steel = find("human_mining_steel_pickaxe")
    val black = find("human_mining_black_pickaxe")
    val mithril = find("human_mining_mithril_pickaxe")
    val adamant = find("human_mining_adamant_pickaxe")
    val rune = find("human_mining_rune_pickaxe")
    val gilded = find("human_mining_gilded_pickaxe")
    val dragon = find("human_mining_dragon_pickaxe")
    val dragonUpgraded = find("human_mining_dragon_pickaxe_pretty")
    val dragonZalcano = find("human_mining_zalcano_pickaxe")
    val dragonTrailblazer = find("human_mining_trailblazer_pickaxe_no_infernal")
    val dragonTrailblazerReloaded = find("human_mining_trailblazer_reloaded_pickaxe_no_infernal")
    val infernal = find("human_mining_infernal_pickaxe")
    val infernalTrailblazer = find("human_mining_trailblazer_pickaxe")
    val infernalTrailblazerReloaded = find("human_mining_trailblazer_reloaded_pickaxe")
    val thirdAge = find("human_mining_3a_pickaxe")
    val crystal = find("human_mining_crystal_pickaxe")
}
