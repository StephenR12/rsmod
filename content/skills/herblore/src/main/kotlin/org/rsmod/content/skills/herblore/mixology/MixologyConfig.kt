package org.rsmod.content.skills.herblore.mixology

import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.type.editors.varp.VarpEditor
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.herblore.UnfinishedPotionRecipes
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public object MixObjs : ObjReferences() {
    val keys = listOf("mmm", "mma", "mml", "aaa", "aal", "aam", "lll", "lla", "llm", "mal")
    val unfinished = keys.map { find("mm_potion_${it}_unfinished") }
    val finished = keys.map { find("mm_potion_${it}_finished") }
    val paste = listOf(find("mm_mox_paste"), find("mm_aga_paste"), find("mm_lye_paste"))
    val digweed = find("mm_lab_special_herb")
    val notes = find("mm_lab_notes")
}

public object MixLocs : LocReferences() {
    val refiner = find("mm_lab_mill")
    val hopper = find("mm_lab_hopper")
    val levers =
        listOf(find("mm_lab_switch_mox"), find("mm_lab_switch_aga"), find("mm_lab_switch_lye"))
    val vessel = find("mm_lab_vessel")
    val vesselForms =
        listOf(
                "empty",
                "ready_mmm",
                "ready_mma",
                "ready_mm1",
                "ready_aaa",
                "ready_aa1",
                "ready_aam",
                "ready_lll",
                "ready_lla",
                "ready_llm",
                "ready_mal",
            )
            .map { find("mm_lab_vessel_$it") }
    val machines =
        listOf(
            find("mm_lab_machine_retort"),
            find("mm_lab_machine_agitator"),
            find("mm_lab_machine_alembic"),
        )
    val machineForms =
        listOf("retort", "agitator", "alembic").map { kind ->
            (listOf("empty") +
                    MixObjs.keys.map {
                        if (kind != "retort" && it == "mml") "mm1"
                        else if (kind != "retort" && it == "aal") "aa1" else it
                    })
                .map { find("mm_lab_machine_${kind}_$it") }
        }
    val conveyor = find("mm_lab_conveyor")
    val entry = find("mm_lab_stairs_entry")
    val exit = find("mm_lab_stairs")
    val herbs = (1..4).map { find("mm_herb_$it") }
    val herbReady = find("mm_herb_ready")
    val shelf = find("mm_lab_notes_bookshelf")
}

public object MixBits : VarBitReferences() {
    val orderType = (1..3).map { find("mm_lab_order_${it}_type") }
    val orderModifier = (1..3).map { find("mm_lab_order_${it}_modifier") }
    val mixer = (0..2).map { find("mm_lab_mixer_slot_$it") }
    val available =
        listOf(find("mm_available_mox"), find("mm_available_aga"), find("mm_available_lye"))
    val progress =
        listOf(
            find("mm_retort_progress"),
            find("mm_agitator_progress"),
            find("mm_alembic_progress"),
        )
    val machinePotion =
        listOf(
            find("mm_lab_retort_potion"),
            find("mm_lab_agitator_potion"),
            find("mm_lab_alembic_potion"),
        )
    val vessel = find("mm_lab_vessel_ready")
    val herbs = (1..4).map { find("mm_herb_ready_$it") }
    val sinceHerb = find("mm_orders_since_herb")
    val introduction = find("mm_introduction_complete")
    val met = find("mm_met_supervisor")
    val quest = find("vmq1")
    val potionStorage = find("mm_potion_storage_unlocked")
}

public object MixVars : VarpReferences() {
    val points =
        listOf(
            find("mixology_mox_points"),
            find("mixology_aga_points"),
            find("mixology_lye_points"),
        )
    val orders = find("total_mixology_orders")
    val native =
        listOf(
            find("mm_rewards_varp_1"),
            find("mm_rewards_transmit"),
            find("mm_lab_orders"),
            find("mm_lab_mixer"),
            find("mm_lab_mixer_2"),
            find("mm_lab_mixer_3"),
            find("mm_lab_mixer_4"),
        )
}

internal object MixSaves : VarpEditor() {
    init {
        for (save in MixVars.points + MixVars.orders + MixVars.native) edit(save) {
            permanent = true
        }
    }
}

public object MixTimer : TimerReferences() {
    val refresh = find("mixology_refresh")
}

public object MixUi : InterfaceReferences() {
    val overlay = find("mm_overlay")
    val formulas = find("mm_formulas")
}

public object MixNpcs : NpcReferences() {
    val supervisors =
        listOf(
            find("mm_lab_supervisor_1op"),
            find("mm_lab_supervisor_2op"),
            find("mm_lab_supervisor"),
        )
}

public object MixState {
    val state = varobjs.mixology_potion_state
}

public data class MixRecipe(
    val index: Int,
    val level: Int,
    val xp: Double,
    val weight: Int,
    val mix: List<Int>,
    val points: List<Int>,
) {
    val nativeType: Int
        get() =
            when (index) {
                4 -> 6
                5 -> 5
                7 -> 9
                8 -> 8
                else -> index + 1
            }

    val unfinished: ObjType
        get() = MixObjs.unfinished[index]

    val finished: ObjType
        get() = MixObjs.finished[index]
}

public object MixRecipes {
    val all =
        listOf(
            MixRecipe(0, 60, 190.0, 5, listOf(3, 0, 0), listOf(20, 0, 0)),
            MixRecipe(1, 63, 215.0, 4, listOf(2, 1, 0), listOf(20, 10, 0)),
            MixRecipe(2, 66, 240.0, 4, listOf(2, 0, 1), listOf(20, 0, 10)),
            MixRecipe(3, 60, 190.0, 5, listOf(0, 3, 0), listOf(0, 20, 0)),
            MixRecipe(4, 72, 290.0, 4, listOf(0, 2, 1), listOf(0, 20, 10)),
            MixRecipe(5, 69, 265.0, 4, listOf(1, 2, 0), listOf(10, 20, 0)),
            MixRecipe(6, 60, 190.0, 5, listOf(0, 0, 3), listOf(0, 0, 20)),
            MixRecipe(7, 78, 340.0, 4, listOf(0, 1, 2), listOf(0, 10, 20)),
            MixRecipe(8, 75, 315.0, 4, listOf(1, 0, 2), listOf(10, 0, 20)),
            MixRecipe(9, 81, 365.0, 3, listOf(1, 1, 1), listOf(20, 20, 20)),
        )
    val herbPaste =
        listOf(
            0 to 10,
            0 to 13,
            0 to 15,
            0 to 20,
            2 to 26,
            2 to 32,
            1 to 30,
            2 to 30,
            2 to 33,
            1 to 20,
            2 to 40,
            1 to 34,
            1 to 40,
            1 to 42,
            1 to 44,
        )
    val herbs by lazy {
        UnfinishedPotionRecipes.all
            .take(15)
            .flatMapIndexed { i, r -> listOf(r.herb to herbPaste[i], r.output to herbPaste[i]) }
            .associate { it.first.id to it.second }
    }

    fun recipe(slots: List<Int>): MixRecipe? =
        if (slots.size != 3 || slots.any { it !in 1..3 }) null
        else all.firstOrNull { r -> r.mix == (1..3).map { kind -> slots.count { it == kind } } }

    fun inBasement(c: CoordGrid) = c.level == 0 && c.x in 1384..1404 && c.z in 9308..9336

    fun inActivity(c: CoordGrid) = inBasement(c) && c.z >= 9320
}
