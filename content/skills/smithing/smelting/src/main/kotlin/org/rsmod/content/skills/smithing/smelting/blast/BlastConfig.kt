package org.rsmod.content.skills.smithing.smelting.blast

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.map.CoordGrid

public object BlastLocs : LocReferences() {
    val down = find("dwarf_keldagrim_factory_stairs")
    val up = find("blast_furnace_stairs_up")
    val belt = find("blast_furnace_conveyer_belt_clickable")
    val dispenser = find("blast_furnace_dispenser")
    val dispensers =
        listOf(
            dispenser,
            find("blast_furnace_ore_dispenser_empty"),
            find("blast_furnace_ore_dispenser_foranim"),
            find("blast_furnace_ore_dispenser_full"),
            find("blast_furnace_ore_dispenser_cooled"),
        )
    val coffer = find("blast_furnace_automata_coffer")
    val coffers =
        listOf(
            coffer,
            find("blast_furnace_automata_coffer_empty"),
            find("blast_furnace_automata_coffer_full"),
        )
    val sink = find("blast_furnace_sink")
    val coke = find("blast_furnace_coke")
    val stoves = listOf("low", "medium", "full").map { find("blast_furnace_stove_" + it) }
    val normalParts =
        listOf("conveyer_cogs", "conveyer_cogs2", "pipes2", "pipes6").map {
            find("blast_furnace_" + it)
        }
    val brokenParts =
        listOf("conveyer_cogs_broken", "conveyer_cogs2_broken", "pipes2_broken", "pipes6_broken")
            .map { find("blast_furnace_" + it) }
    val pedals = find("blast_furnace_pedals")
    val pump = find("blast_furnace_pump")
    val gauge = find("blast_furnace_gauge")
}

public object BlastNpcs : NpcReferences() {
    val foreman = find("blast_furnace_instructor")
    val ordan = find("smithing_guild_ore_seller")
}

public object BlastObjs : ObjReferences() {
    val coke = find("blast_furnace_coke_spade")
    val smithIce = find("smithing_uniform_gloves_ice")
    val charos = find("ring_of_charos_unlocked")
}

public object BlastBits : VarBitReferences() {
    val ores =
        listOf(
                "copper_ore",
                "tin_ore",
                "iron_ore",
                "silver_ore",
                "gold_ore",
                "mithril_ore",
                "adamantite_ore",
                "runite_ore",
                "coal",
            )
            .map { find("blast_furnace_" + it) }
    val bars =
        listOf("bronze", "iron", "steel", "silver", "gold", "mithril", "adamantite", "runite").map {
            find("blast_furnace_" + it + "_bars")
        }
    val hot = find("blast_furnace_bars_hot")
    val coins = find("blast_furnace_coffer")
    val hasCoins = find("blast_furnace_coinsincoffer")
    val catalysts = find("blast_furnace_catalyst")
    val visited = find("blast_furnace_visited")
    val temp = find("blast_furnace_temperature_reading")
}

public object BlastVars : VarpReferences() {
    val permission = find("blast_furnace_free_permission")
    val paid = find("blast_furnace_paid_cycles")
    val delay = find("blast_furnace_ore_delay")
}

public object BlastTimers : TimerReferences() {
    val tick = find("blast_furnace_processing")
}

public object BlastInterfaces : InterfaceReferences() {
    val hud = find("blast_furnace_hud")
    val gauge = find("blast_furnace_temp_gauge")
}

public object BlastInvs : InvReferences() {
    val shop = find("smithing_guild_ore_seller")
}

public object BlastSeqs : SeqReferences() {
    val pedal = find("blast_furnace_human_conveyer_cycle")
    val pump = find("blast_furnace_human_air_pump")
    val coke = find("human_shovel_coke_up")
    val refuel = find("human_shovel_coke_down")
}

internal object BlastVarsBuild : VarpBuilder() {
    init {
        build("blast_furnace_free_permission") { transmitNever = true }
        build("blast_furnace_paid_cycles") { transmitNever = true }
        build("blast_furnace_ore_delay") { transmitNever = true }
    }
}

internal object BlastStock : InvEditor() {
    init {
        edit(BlastInvs.shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (ore in
                listOf(
                    objs.copper_ore,
                    objs.tin_ore,
                    objs.iron_ore,
                    objs.mithril_ore,
                    objs.silver_ore,
                    objs.gold_ore,
                    objs.coal,
                )) stock += stock(ore, 100, 200)
        }
    }
}

internal object BlastPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<BlastFurnace>("blast-furnace.toml")
    }
}

public object BlastArea {
    public fun contains(c: CoordGrid): Boolean =
        c.level == 0 && c.x in 1932..1961 && c.z in 4953..4978
}
