package org.rsmod.content.skills.combattraining

import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType

internal object CannonObjs : ObjReferences() {
    val parts = (1..4).map { find("twpart$it") }
    val ornamented =
        listOf("base", "stand", "barrels", "furnace").map { find("league_3_multicannon_$it") }
    val ball = find("mcannonball")
    val granite = find("granite_cannonball")
    val manual = find("mcannonbook")
    val mould = find("ammo_mould")
}

internal object CannonLocs : LocReferences() {
    val normal =
        listOf(
                "multicannon_base",
                "multicannon_stand",
                "multicannon_barrels",
                "dwarf_multicannon1",
                "dwarf_multicannon1_broken",
            )
            .map { find(it) }
    val ornamented =
        listOf("base", "stand", "barrels", "active", "broken").map {
            find("league03_multicannon_$it")
        }
}

internal object CannonSpots : SpotanimReferences() {
    val ball = find("cannonball_travel")
    val granite = find("cannonball_travel_granite")
}

internal object CannonSeqs : SeqReferences() {
    private val points = listOf("n", "ne", "e", "se", "s", "sw", "w", "nw")
    val fire = points.map { find("mcannon_${it}_fire") }
    val turn = points.map { find("mcannon_${it}_turn") }
}

internal object CannonNpcs : NpcReferences() {
    val nulodion = find("nulodion")
}

internal object CannonInvs : InvReferences() {
    val shop = find("mcannonshop")
}

internal object CannonTimers : TimerReferences() {
    val tick = find("dwarf_multicannon_tick")
}

internal object CannonRewards : VarBitReferences() {
    val medium = find("ca_tier_status_medium")
    val hard = find("ca_tier_status_hard")
    val elite = find("ca_tier_status_elite")
}

internal object CannonVars : VarpReferences() {
    val tile = find("dwarf_cannon_tile")
    val setup = find("dwarf_cannon_setup_epoch")
    val ornament = find("dwarf_cannon_ornament")
    val rounds = find("dwarf_cannon_rounds")
    val granite = find("dwarf_cannon_granite")
}

internal object CannonBuild : VarpBuilder() {
    init {
        for (field in listOf("tile", "setup_epoch", "ornament", "rounds", "granite")) build(
            "dwarf_cannon_$field"
        ) {
            permanent = true
            transmitNever = true
        }
    }
}

internal object CannonStock : InvEditor() {
    init {
        edit(CannonInvs.shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (item in
                listOf(
                    CannonObjs.parts[1],
                    CannonObjs.parts[0],
                    CannonObjs.parts[3],
                    CannonObjs.parts[2],
                    CannonObjs.manual,
                    CannonObjs.mould,
                )) {
                stock += stock(item, count = 5, restockCycles = 100)
            }
        }
    }
}
