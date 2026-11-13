package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object EctoItems : ObjReferences() {
    val crusher = find("bonecrusher")
    val crusherNecklace = find("bonecrusher_necklace")
    val dragonNecklace = find("dragonbone_necklace")
    val hydraTail = find("hydra_tail")
    val slime = find("bucket_ectoplasm")
    val tokens = find("ectotoken")
    val ghostspeak =
        listOf(
            find("amulet_of_ghostspeak"),
            find("amulet_of_ghostspeak_enchanted"),
            objs.diary_morytania_legs_2,
            objs.diary_morytania_legs_3,
            objs.diary_morytania_legs_4,
        )
    val recipes =
        listOf(
            objs.bones to find("pot_bonemeal"),
            objs.burnt_bones to find("pot_bonemeal_burnt"),
            objs.bat_bones to find("pot_bonemeal_bat"),
            objs.big_bones to find("pot_bonemeal_big"),
            objs.long_bone to find("pot_bonemeal_big"),
            objs.curved_bone to find("pot_bonemeal_big"),
            objs.babydragon_bones to find("pot_bonemeal_babydragon"),
            objs.dragon_bones to find("pot_bonemeal_dragon"),
            objs.wolf_bones to find("pot_bonemeal_wolf"),
            objs.shaikahan_bones to find("pot_bonemeal_beast"),
            objs.jogre_bones to find("pot_bonemeal_jogre"),
            objs.burnt_jogre_bones to find("pot_bonemeal_burnt_jogre"),
            objs.small_ninja_monkey_bones to find("pot_bonemeal_small_ninja_monkey"),
            objs.medium_ninja_monkey_bones to find("pot_bonemeal_medium_ninja_monkey"),
            objs.gorilla_bones to find("pot_bonemeal_normal_gorilla_monkey"),
            objs.bearded_gorilla_bones to find("pot_bonemeal_bearded_gorilla_monkey"),
            objs.monkey_bones to find("pot_bonemeal_normal_monkey"),
            objs.small_zombie_monkey_bones to find("pot_bonemeal_small_zombie_monkey"),
            objs.large_zombie_monkey_bones to find("pot_bonemeal_large_zombie_monkey"),
            objs.monkey_skeleton_bones to find("pot_bonemeal_skeleton"),
            objs.zogre_bones to find("pot_bonemeal_zogre"),
            objs.fayrg_bones to find("pot_bonemeal_ancestral_fayg"),
            objs.raurg_bones to find("pot_bonemeal_ancestral_raurg"),
            objs.ourg_bones to find("pot_bonemeal_ancestral_ourg"),
            objs.dagannoth_bones to find("pot_bonemeal_dagannoth"),
            objs.wyvern_bones to find("pot_bonemeal_wyvern"),
            objs.lava_dragon_bones to find("pot_bonemeal_lavadragon"),
            objs.superior_dragon_bones to find("pot_bonemeal_dragon_superior"),
            objs.wyrm_bones to find("pot_bonemeal_wyrm"),
            objs.drake_bones to find("pot_bonemeal_drake"),
            objs.hydra_bones to find("pot_bonemeal_hydra"),
            objs.wyrmling_bones to find("pot_bonemeal_babywyrm"),
            objs.alans_bones to find("pot_bonemeal_alan"),
        )
    val inputs by lazy { recipes.associate { it.first.id to it.first } }
    val byBone by lazy { recipes.associate { it.first.id to it.second } }
    val byMeal by lazy { recipes.asReversed().associate { it.second.id to it.first } }
}

internal object EctoLocs : LocReferences() {
    val loader = find("ahoy_grinder_loader")
    val handle = find("ahoy_grinder_handle")
    val bin = find("ahoy_grinder_chute")
    val fountain = find("ahoy_ectofuntus")
    val pools =
        listOf(
            find("ahoy_gloopbubbles"),
            find("ahoy_gloopbubbles_large"),
            find("ahoy_gloopbubbles_larger"),
            find("ahoy_new_green_floor"),
        )
    val up = find("ahoy_tower_stairs_lv1")
    val down = find("ahoy_tower_stairs_lv1_top")
    val trapdoor = find("ahoy_trapdoor")
    val openTrapdoor = find("ahoy_trapdoor_open")
    val ladder = find("ahoy_ladder_from_cellar")
    val cavernUp = find("ahoy_cavern_stairs")
    val cavernDown = find("ahoy_cavern_stairs_top")
}

internal object EctoNpcs : NpcReferences() {
    val velorina = find("ahoy_velorina")
    val disciple = find("ahoy_disciple")
    val robin = find("ahoy_robin")
}

internal object EctoSeqs : SeqReferences() {
    val pour = find("ectophial_pour_slime_cover")
    val refill = find("ahoy_ecto_teleport")
    val load = find("ahoy_bone_dump")
    val grind = find("ahoy_bone_grind")
    val empty = find("ahoy_fillbucket_bonedust")
    val fill = find("quest_ahoy_human_filling_bucket")
    val pray = find("ahoy_prayer")
}

internal object EctoBits : VarBitReferences() {
    val crusherBankCharges = find("charges_bonecrusher_quantity")
    val sanctifierBankCharges = find("charges_ash_sanctifier_quantity")
    val quest = find("ahoy_questvar")
    val stage = find("ahoy_grinder_status")
    val tokensLow = find("ahoy_ectotokens_base")
    val tokensHigh = find("ahoy_ectotokens_more")
    val robinClaimed = find("morytania_slime_claimed")
}

internal object EctoVars : VarpReferences() {
    val devicesDisabled = find("prayer_remains_devices_disabled")
    val loaded = find("ectofuntus_loaded_bone")
    val claimDay = find("ectofuntus_robin_day")
}

internal object EctoVarBuilds : VarpBuilder() {
    init {
        build("prayer_remains_devices_disabled") { transmitNever = true }
        build("ectofuntus_loaded_bone") { transmitNever = true }
        build("ectofuntus_robin_day") { transmitNever = true }
    }
}

internal object EctoQueues : QueueReferences() {
    val grind = find("ectofuntus_grinder_cycle")
    val fill = find("ectofuntus_fill_buckets")
}

internal object EctoSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<Ectofuntus>("ectofuntus-npcs.toml")
    }
}
