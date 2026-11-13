package org.rsmod.content.skills.mining

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.builders.area.AreaBuilder
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.map.area.MapAreaBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.editors.varp.VarpEditor
import org.rsmod.api.type.refs.area.AreaReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.area.polygon.VertexCoord
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.map.CoordGrid

internal object BlastItems : ObjReferences() {
    val ore = find("lovakengj_blasted_ore")
    val dynamite = find("lovakengj_dynamite_fused")
    val pot = find("lovakengj_dynamite_pot")
    val charcoal = find("juniper_charcoal")
}

internal object BlastUi : InterfaceReferences() {
    val hud = find("lovakengj_blast_mining_hud")
}

internal object BlastInv : InvReferences() {
    val shop = find("lovakengj_dynamite_shop")
}

internal object BlastStore : InvEditor() {
    init {
        edit(BlastInv.shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(BlastItems.dynamite, 2000, 2500)
            stock += stock(objs.tinderbox, 2, 3)
        }
    }
}

internal object BlastOreEdit : ObjEditor() {
    init {
        edit(BlastItems.ore) { param[params.no_bank] = 1 }
    }
}

internal object BlastVars : VarpReferences() {
    val sacks = (1..2).map { find("blast_mining_ores_$it") }
    val claimDay = find("blast_mine_dynamite_day")
    val logs = find("juniper_burner_logs")
    val started = listOf("west", "east").map { find("juniper_burner_${it}_start") }
}

internal object BlastBits : VarBitReferences() {
    val ore =
        listOf("coal", "gold", "mithril", "adamantite", "runite").map {
            find("lovakengj_ore_${it}_bigger")
        }
    val claimed = find("kourend_free_dynamite")
    val burners = listOf(find("farming_transmit_a"), find("farming_transmit_b"))
}

internal object BlastSave : VarpBuilder() {
    init {
        for (name in
            listOf(
                "blast_mine_dynamite_day",
                "juniper_burner_logs",
                "juniper_burner_west_start",
                "juniper_burner_east_start",
            )) build(name) { permanent = true }
    }
}

internal object BlastSackSave : VarpEditor() {
    init {
        for (varp in BlastVars.sacks) edit(varp) {
            permanent = true
            transmitOnDiff = true
        }
    }
}

internal object BlastTimers : TimerReferences() {
    val ore = find("blast_mine_ore_expiry")
    val furnace = find("juniper_burner_display")
}

internal object BlastFuse : ControllerBuilder() {
    val fuse = build("blast_mine_fuse")
}

internal object BlastAreas : AreaReferences() {
    val mine = find("lovakengj_blast_mine_activity")
    val burners = find("hosidius_charcoal_burners")
}

internal object BlastAreaDefinitions : AreaBuilder() {
    init {
        build("lovakengj_blast_mine_activity")
        build("hosidius_charcoal_burners")
    }
}

internal object BlastMapAreas : MapAreaBuilder() {
    override fun onPackMapTask() {
        for ((type, corners) in
            listOf(
                BlastAreas.mine to listOf(1464, 3848, 1512, 3895),
                BlastAreas.burners to listOf(1701, 3457, 1730, 3487),
            )) {
            area(type) {
                for (plane in 0..(if (type == BlastAreas.mine) 1 else 0)) polygon(level = plane) {
                    val (west, south, east, north) = corners
                    for ((x, z) in
                        listOf(west to south, west to north, east to north, east to south)) vertex(
                        VertexCoord.from(CoordGrid(x, z))
                    )
                }
            }
        }
    }
}

internal object BlastPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<BlastMine>("blast-mine.toml")
    }
}
