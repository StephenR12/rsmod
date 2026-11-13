package org.rsmod.content.skills.mining.volcanic

import org.rsmod.api.config.refs.params
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.map.CoordGrid

internal object VolcanicVars : VarpReferences() {
    val points = find("volcanic_mine_points")
    val admission = find("volcanic_mine_admission")
    val paid = find("volcanic_mine_retrieval_paid")
}

internal object VolcanicSaves : VarpBuilder() {
    init {
        for (name in
            listOf(
                "volcanic_mine_points",
                "volcanic_mine_admission",
                "volcanic_mine_retrieval_paid",
            )) build(name) { permanent = true }
    }
}

internal object VolcanicBits : VarBitReferences() {
    val current = find("fossil_mine_temp_pts")
    val players = find("fossil_mine_numberplayers")
    val stability = find("fossil_mine_current_stability")
    val status = find("fossil_mine_progress_status")
    val chamber = find("fossil_mine_chamber_status")
    val vent = listOf("a", "b", "c").map { find("fossil_mine_vent${it}_status") }
    val seconds = find("fossil_mine_time_remaining")
    val quest = find("fossilquest_progress")
    val large = find("fossil_mine_unlocked_morewater")
    val soil = find("fossil_mine_unlocked_ultrafertilesoil")
    val fossilsOff = find("fossil_drop_blocked")
    val rareOff = find("fossil_drop_rare_blocked")
    val done = find("fossil_activity_volcanicmine")
}

internal object VolcanicCourse : ControllerBuilder() {
    val round = build("volcanic_mine_round")
}

internal object VolcanicQueue : QueueReferences() {
    val mine = find("volcanic_mine_boulder_swing")
}

internal object VolcanicItems : ObjReferences() {
    val vessel = find("fossil_mine_water_container")
    val rock = find("fossil_mine_large_rock")
    val fragments =
        listOf("iron", "silver", "coal", "gold", "mithril", "adamantite", "runite").map {
            find("fossil_mine_fragment_$it")
        }
    val numulite = find("fossil_numulite")
    val fossil = listOf("small", "medium", "large", "rare").map { find("fossil_${it}_unid") }
}

internal object VolcanicEdits : ObjEditor() {
    init {
        for (item in
            VolcanicItems.fragments + listOf(VolcanicItems.vessel, VolcanicItems.rock)) edit(item) {
            param[params.no_bank] = 1
        }
    }
}

internal object VolcanicInv : InvReferences() {
    val lost = find("volcanic_mine_retrieval")
    val picks = find("pickaxeshop")
}

internal object VolcanicStorage : InvBuilder() {
    init {
        build("volcanic_mine_retrieval") {
            scope = InvScope.Perm
            stack = InvStackType.Normal
            size = 48
            protect = false
        }
    }
}

internal object VolcanicUi : InterfaceReferences() {
    val shop = find("fossil_volcanic_shop")
    val hud = find("fossil_volcanic_mine")
}

internal object VolcanicButtons : ComponentReferences() {
    val stock = find("fossil_volcanic_shop:items")
}

internal object PeteSpawn : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<PeteSpawn>("volcanic-mine.toml")
    }
}

internal val entrance = CoordGrid(3815, 3808)
internal val lobby = CoordGrid(3808, 10193, 3)

internal fun inMine(tile: CoordGrid) =
    tile.x in 3776..3839 && tile.z in 10112..10239 && tile.level in 0..3

internal object NurmofInventory : InvEditor() {
    init {
        edit(VolcanicInv.picks) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            restock = true
            autoSize = true
            val picks =
                listOf(
                    MiningObjs.bronzePickaxe,
                    MiningObjs.ironPickaxe,
                    MiningObjs.steelPickaxe,
                    MiningObjs.mithrilPickaxe,
                    MiningObjs.adamantPickaxe,
                    MiningObjs.runePickaxe,
                )
            val ticks = listOf(50, 100, 200, 300, 500, 700)
            for ((i, pick) in picks.withIndex()) stock += stock(pick, 6 - i, ticks[i])
        }
    }
}
