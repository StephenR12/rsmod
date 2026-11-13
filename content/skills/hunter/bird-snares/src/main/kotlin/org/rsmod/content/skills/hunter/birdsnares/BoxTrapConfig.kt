package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.hunt.HuntModeBuilder
import org.rsmod.api.type.builders.varcon.VarConBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.hunt.HuntModeReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.type.hunt.HuntType
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

public object BoxTrapLocs : LocReferences() {
    val open = find("hunting_boxtrap_empty")
    val failing = find("hunting_boxtrap_failing")
    val broken = find("hunting_boxtrap_failed")
    val trappingJerboa = find("hunting_boxtrap_trapping_jerboa_n")
    val fullJerboa = find("hunting_boxtrap_full_jerboa")
    val trappingGrey = find("hunting_boxtrap_trapping_chinchompa_n")
    val trappingRed = find("hunting_boxtrap_trapping_chinchompa_big_n")
    val trappingBlack = find("hunting_boxtrap_trapping_chinchompa_black_n")
    val fullGrey = find("hunting_boxtrap_full_chinchompa")
    val fullRed = find("hunting_boxtrap_full_chinchompa_big")
    val fullBlack = find("hunting_boxtrap_full_chinchompa_black")
}

public object BoxTrapObjs : ObjReferences() {
    val snare = find("hunting_box_trap")
    val tail = find("hunting_jerboa_tail")
    val grey = find("chinchompa_captured")
    val red = find("chinchompa_big_captured")
    val black = find("chinchompa_black")
    val litTorch = find("torch_lit")
    val brumaTorch = find("wint_torch")
    val guildHood = find("hg_hunter_hood")
    val guildTop = find("hg_hunter_top")
    val guildLegs = find("hg_hunter_legs")
    val guildBoots = find("hg_hunter_boots")
    val spicyTomato = find("bowl_spicytomato")
    val spicyMeat = find("bowl_spicymeat")
}

public object BoxTrapNpcs : NpcReferences() {
    val jerboa = find("varlamore_hunterjerboa01")
    val grey = find("hunting_chinchompa")
    val red = find("hunting_chinchompa_big")
    val black = find("hunting_chinchompa_black")
}

public object BoxTrapSeqs : SeqReferences() {
    val lay = find("human_createfire")
    val check = find("human_hunting_dismantle_net")
    val birdCaught = find("hunting_chinchompa_approaching")
    val birdLeave = find("hunting_chinchompa_backoff")
}

public object BoxTrapVars : VarBitReferences() {
    val eaglesPeak = find("eaglepeak_quest")
}

public object BoxTrapHunts : HuntModeReferences() {
    val openTrap = find("box_trap_open")
}

internal object BoxTrapHuntBuilder : HuntModeBuilder() {
    init {
        build("box_trap_open") {
            type = HuntType.Scenery
            checkLoc { loc = BoxTrapLocs.open }
            checkVis = HuntVis.LineOfSight
            findKeepHunting = true
            rate = 3
            findNewMode = NpcMode.OpLoc1
        }
    }
}

internal object BoxTrapNpcEditor : NpcEditor() {
    init {
        for (npc in listOf(BoxTrapNpcs.grey, BoxTrapNpcs.red, BoxTrapNpcs.black, BoxTrapNpcs.jerboa)) {
            edit(npc) {
                defaultMode = NpcMode.Wander
                moveRestrict = MoveRestrict.Normal
                huntMode = BoxTrapHunts.openTrap
                huntRange = 2
            }
        }
    }
}

public object BoxTrapControllerBuilder : ControllerBuilder() {
    val trap = build("box_trap")
}

internal object BoxTrapVarCons : VarConBuilder() {
    val ownerSlot = build("box_trap_owner_slot")
    val phase = build("box_trap_phase")
    val birdIndex = build("box_trap_species")
    val smoked = build("box_trap_smoked")
}

public data class BoxTrapSpecies(
    val npc: NpcType,
    val level: Int,
    val xp: Double,
    val lowRate: Int,
    val highRate: Int,
    val transition: LocType,
    val full: LocType,
    val product: ObjType,
)

public object BoxTrapSpeciesList {
    val all =
        listOf(
            BoxTrapSpecies(
                BoxTrapNpcs.grey,
                53,
                198.4,
                6,
                268,
                BoxTrapLocs.trappingGrey,
                BoxTrapLocs.fullGrey,
                BoxTrapObjs.grey,
            ),
            BoxTrapSpecies(
                BoxTrapNpcs.red,
                63,
                265.0,
                -78,
                228,
                BoxTrapLocs.trappingRed,
                BoxTrapLocs.fullRed,
                BoxTrapObjs.red,
            ),
            BoxTrapSpecies(
                BoxTrapNpcs.black,
                73,
                315.0,
                -78,
                228,
                BoxTrapLocs.trappingBlack,
                BoxTrapLocs.fullBlack,
                BoxTrapObjs.black,
            ),
            BoxTrapSpecies(BoxTrapNpcs.jerboa,39,137.0,6,268,BoxTrapLocs.trappingJerboa,BoxTrapLocs.fullJerboa,BoxTrapObjs.tail),
        )
    val byNpcId = all.associateBy { it.npc.id }
}
