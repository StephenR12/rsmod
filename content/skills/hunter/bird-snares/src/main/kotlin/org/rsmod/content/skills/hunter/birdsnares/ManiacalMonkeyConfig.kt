package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.hunt.HuntModeBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.hunt.HuntModeReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.type.hunt.HuntType
import org.rsmod.game.type.hunt.HuntVis

internal object ManiacalLocs : LocReferences() {
    val grass = find("mm2_secret_entrance_multi")
    val ladder = find("mm2_secret_lair_exit")
    val southBars = find("mm2_monkeybars_start")
    val northBars = find("mm2_monkeybars_end")
    val passage = find("mm2_secret_lab_entrance_multi")
    val exit = find("mm2_secret_lab_exit")
    val dismount = find("mm2_lab_dismount")
    val unset = find("hunting_monkeytrap_unset")
    val setting = find("hunting_monkeytrap_setting")
    val baited = find("hunting_monkeytrap_set")
    val catching = (0..1).map { find("hunting_monkeytrap_trapping_" + it) }
    val full = (0..1).map { find("hunting_monkeytrap_full_" + it) }
    val failed = find("hunting_monkeytrap_failing")
}

internal object ManiacalNpcs : NpcReferences() {
    val monkey = find("hunting_monkey")
    val growing = (1..3).map { find("hunting_monkey_sans_tail_" + it) }
    val mount = find("mm2_tortured_gorilla_small")
    val mounted = find("mm2_player_gorilla")
    val kruk = find("mm2_kruk_combat")
    val zooknock = find("mm2_zooknock_postquest")
}

internal object ManiacalQuest : VarBitReferences() {
    val progress = find("mm2_progress")
    val tracking = find("mm2_tracking_kruk")
}

internal object ManiacalObjs : ObjReferences() {
    val greegree = find("mm2_kruk_greegree")
}

internal object ManiacalTimer : TimerReferences() {
    val form = find("maniacal_monkey_form")
}

internal object ManiacalHunt : HuntModeReferences() {
    val trap = find("maniacal_monkey_baited")
}

internal object ManiacalController : ControllerBuilder() {
    val trap = build("maniacal_monkey_trap")
}

internal object ManiacalHuntBuilder : HuntModeBuilder() {
    init {
        build("maniacal_monkey_baited") {
            type = HuntType.Scenery
            checkLoc { loc = ManiacalLocs.baited }
            checkVis = HuntVis.Off
            findKeepHunting = true
            rate = 6
            findNewMode = NpcMode.OpLoc1
        }
    }
}

internal object ManiacalNpcEditor : NpcEditor() {
    init {
        edit(ManiacalNpcs.monkey) {
            defaultMode = NpcMode.Wander
            moveRestrict = MoveRestrict.PassThru
            huntMode = ManiacalHunt.trap
            huntRange = 5
        }
    }
}

internal object ManiacalSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ManiacalMonkeys>("maniacal-monkeys.toml")
    }
}
