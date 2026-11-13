package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.hunt.HuntModeBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varcon.VarConBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.hunt.HuntModeReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.type.hunt.HuntType
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public object DeadfallLocs : LocReferences() {
    val boulder = find("hunting_deadfall_boulder")
    val open = find("hunting_deadfall_trap")
    val failed = find("hunting_deadfall_failing")
    val transitions =
        listOf("claw", "barbed", "spike", "sabre", "fennec").map {
            find("hunting_deadfall_trapping_" + it)
        }
    val full =
        listOf("claw", "barbed", "spike", "sabre", "fennec").map {
            find("hunting_deadfall_full_" + it)
        }
}

public object DeadfallNpcs : NpcReferences() {
    val all =
        listOf(
                "huntingbeast_claws",
                "huntingbeast_barbedtail",
                "huntingbeast_spiky",
                "huntingbeast_sabreteeth",
                "varlamore_hunterfox01",
            )
            .map(::find)
}

public object DeadfallObjs : ObjReferences() {
    val claws = find("huntingbeast_claws")
    val harpoon = find("hunting_barbed_harpoon")
    val spike = find("huntingbeast_spike")
    val teeth = find("huntingbeast_sabreteeth")
    val foxFur = find("hunting_fennecfox_fur")
    val wildMeat = find("huntingbeast_wild_meat")
    val barbMeat = find("huntingbeast_barbed_meat")
    val foxMeat = find("hunting_fennecfox_meat")
    val rainbow = find("hunting_raw_fish_special")
    val jerboa = find("hunting_jerboa_tail")
    val barley = find("barley")
}

public object DeadfallHunts : HuntModeReferences() {
    val trap = find("deadfall_open_trap")
}

internal object DeadfallHuntBuilder : HuntModeBuilder() {
    init {
        build("deadfall_open_trap") {
            type = HuntType.Scenery
            checkLoc { loc = DeadfallLocs.open }
            checkVis = HuntVis.Off
            findKeepHunting = true
            rate = 3
            findNewMode = NpcMode.OpLoc1
        }
    }
}

internal object DeadfallNpcEditor : NpcEditor() {
    init {
        for (npc in DeadfallNpcs.all) edit(npc) {
            defaultMode = NpcMode.Wander
            moveRestrict = MoveRestrict.PassThru
            huntMode = DeadfallHunts.trap
            huntRange = 4
            respawnRate = 50
        }
    }
}

public object DeadfallControllers : ControllerBuilder() {
    val trap = build("deadfall_trap")
}

internal object DeadfallVarCons : VarConBuilder() {
    val owner = build("deadfall_owner")
}

internal object DeadfallPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<DeadfallTrap>("deadfall-creatures.toml")
    }
}

public data class DeadfallSpecies(val index: Int, val level: Int, val xp: Double) {
    val npc: NpcType
        get() = DeadfallNpcs.all[index]

    val full: LocType
        get() = DeadfallLocs.full[index]

    val transition: LocType
        get() = DeadfallLocs.transitions[index]

    val loot: List<ObjType>
        get() =
            when (index) {
                0 -> listOf(DeadfallObjs.claws, DeadfallObjs.wildMeat)
                1 -> listOf(DeadfallObjs.harpoon, DeadfallObjs.barbMeat)
                2 -> listOf(DeadfallObjs.spike)
                3 -> listOf(DeadfallObjs.teeth)
                else -> listOf(DeadfallObjs.foxFur, DeadfallObjs.foxMeat)
            }

    val bait: ObjType
        get() =
            when (index) {
                0,
                3 -> org.rsmod.api.config.refs.objs.raw_beef
                1 -> DeadfallObjs.rainbow
                2 -> DeadfallObjs.barley
                else -> DeadfallObjs.jerboa
            }
}

public object DeadfallSpeciesList {
    val all =
        listOf(
            DeadfallSpecies(0, 23, 128.0),
            DeadfallSpecies(1, 33, 168.0),
            DeadfallSpecies(2, 37, 204.0),
            DeadfallSpecies(3, 51, 200.0),
            DeadfallSpecies(4, 57, 222.0),
        )
    // Native boulder coordinates are from Deadfall14986793 at the cache cutoff.
    val sites: Map<CoordGrid, Int> =
        mapOf(
            CoordGrid(1378, 3386) to 0,
            CoordGrid(1398, 3387) to 0,
            CoordGrid(2308, 3577) to 2,
            CoordGrid(2308, 3605) to 2,
            CoordGrid(2308, 3620) to 2,
            CoordGrid(2309, 3642) to 2,
            CoordGrid(2319, 3594) to 2,
            CoordGrid(2322, 3643) to 2,
            CoordGrid(2323, 3615) to 2,
            CoordGrid(2323, 3627) to 2,
            CoordGrid(2327, 3636) to 2,
            CoordGrid(2337, 3631) to 2,
            CoordGrid(2342, 3642) to 2,
            CoordGrid(2306, 3542) to 0,
            CoordGrid(2316, 3523) to 0,
            CoordGrid(2318, 3561) to 0,
            CoordGrid(2330, 3553) to 0,
            CoordGrid(2342, 3545) to 0,
            CoordGrid(2348, 3524) to 0,
            CoordGrid(2350, 3564) to 0,
            CoordGrid(2361, 3577) to 0,
            CoordGrid(2363, 3538) to 0,
            CoordGrid(2708, 3773) to 3,
            CoordGrid(2710, 3769) to 3,
            CoordGrid(2711, 3772) to 3,
            CoordGrid(2715, 3765) to 3,
            CoordGrid(2715, 3769) to 3,
            CoordGrid(2719, 3763) to 3,
            CoordGrid(2706, 3779) to 3,
            CoordGrid(2710, 3777) to 3,
            CoordGrid(2714, 3779) to 3,
            CoordGrid(2718, 3776) to 3,
            CoordGrid(2566, 2902) to 1,
            CoordGrid(2572, 2930) to 1,
            CoordGrid(2573, 2897) to 1,
            CoordGrid(2574, 2910) to 1,
            CoordGrid(2574, 2916) to 1,
            CoordGrid(2576, 2925) to 1,
            CoordGrid(2579, 2885) to 1,
            CoordGrid(2583, 2913) to 1,
            CoordGrid(1619, 3001) to 4,
            CoordGrid(1612, 2997) to 4,
            CoordGrid(1616, 2993) to 4,
            CoordGrid(1623, 2996) to 4,
            CoordGrid(1613, 3002) to 4,
        )
}
