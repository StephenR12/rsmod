package org.rsmod.content.skills.mining.motherlode

import org.rsmod.api.config.refs.params
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.builders.map.loc.MapLocSpawnBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.map.CoordGrid

public object MlmLocs : LocReferences() {
    val veins = listOf("single", "left", "middle", "right").map { find("motherlode_ore_$it") }
    val depleted =
        listOf("single", "left", "middle", "right").map { find("motherlode_depleted_$it") }
    val broken = find("motherlode_wheel_strut_broken")
    val fixed = find("motherlode_wheel_strut_fixed")
    val wheelBroken = find("motherlode_wheel_broken")
    val wheelFixed = find("motherlode_wheel_fixed")
    val hopper = find("motherlode_hopper")
    val sack = find("motherlode_sack")
    val sacks = listOf(sack, find("motherlode_sack_empty"), find("motherlode_sack_full"))
    val rockfalls = listOf(find("motherlode_rockfall_1"), find("motherlode_rockfall_2"))
    val entry = find("motherlode_entrance")
    val exit = find("motherlode_exit")
    val guildEntry = find("motherlode_entrance_guild")
    val guildExit = find("motherlode_exit_guild")
    val ladders =
        listOf(
            find("motherlode_ladder_bottom"),
            find("motherlode_ladder_bottom_active"),
            find("motherlode_ladder_bottom_inactive"),
        )
    val tops =
        listOf(
            find("motherlode_ladder_top"),
            find("motherlode_ladder_top_active"),
            find("motherlode_ladder_top_inactive"),
        )
    val crate = find("crate2_old")
    val bank = find("blast_bank_chest")
    val shortcut = find("motherlode_shortcut")
    val waterTunnel = find("motherlode_water_tunnel")
}

public object MlmObjs : ObjReferences() {
    val dirt = find("paydirt")
    val nugget = find("motherlode_nugget")
    val clayPack = find("pack_softclay")
    val gemPack = find("reward_gem_bag")
}

public object MlmNpcs : NpcReferences() {
    val percy = find("motherlode_percy")
}

public object MlmBits : VarBitReferences() {
    val big = find("motherlode_biggersack")
    val count = find("motherlode_sack_transmit")
    val restricted = find("motherlode_in_restricted_area")
}

public object MlmVars : VarpReferences() {
    val unlocks = find("motherlode_unlocks")
}

public object MlmTimers : TimerReferences() {
    val processing = find("motherlode_processing")
}

public object MlmInvs : InvReferences() {
    val pending = find("motherlode_pending")
    val sack = find("motherlode_sack_storage")
}

public object MlmUi : InterfaceReferences() {
    val hud = find("motherlode_hud")
}

internal object MlmInventoryBuild : InvBuilder() {
    init {
        build("motherlode_pending") {
            scope = InvScope.Perm
            stack = InvStackType.Never
            size = 189
            protect = false
        }
        build("motherlode_sack_storage") {
            scope = InvScope.Perm
            stack = InvStackType.Always
            size = 6
            protect = false
        }
    }
}

internal object MlmVarBuild : VarpBuilder() {
    init {
        build("motherlode_unlocks") { transmitNever = true }
    }
}

internal object MlmDirtEditor : ObjEditor() {
    init {
        edit(MlmObjs.dirt) { param[params.no_bank] = 1 }
    }
}

internal object MlmVeinMap : MapLocSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<MotherlodeMine>("map/l58_88")
    }
}

internal object MlmPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<MotherlodeMine>("motherlode-people.toml")
    }
}

/**
 * Native encoded level1 veins become server plane0 through LINK_BELOW, like the upper
 * hopper/ladder.
 */
public object MlmArea {
    public fun contains(c: CoordGrid): Boolean =
        c.level == 0 && c.x in 3712..3775 && c.z in 5632..5695

    public val upper: Set<CoordGrid> =
        setOf(
            CoordGrid(3734, 5685),
            CoordGrid(3735, 5684),
            CoordGrid(3737, 5682),
            CoordGrid(3738, 5682),
            CoordGrid(3741, 5683),
            CoordGrid(3742, 5683),
            CoordGrid(3743, 5684),
            CoordGrid(3745, 5684),
            CoordGrid(3746, 5684),
            CoordGrid(3747, 5683),
            CoordGrid(3748, 5682),
            CoordGrid(3749, 5682),
            CoordGrid(3750, 5682),
            CoordGrid(3751, 5680),
            CoordGrid(3751, 5681),
            CoordGrid(3752, 5684),
            CoordGrid(3753, 5680),
            CoordGrid(3753, 5684),
            CoordGrid(3754, 5682),
            CoordGrid(3754, 5683),
            CoordGrid(3755, 5681),
            CoordGrid(3755, 5682),
            CoordGrid(3755, 5683),
            CoordGrid(3756, 5679),
            CoordGrid(3756, 5684),
            CoordGrid(3757, 5684),
            CoordGrid(3758, 5675),
            CoordGrid(3758, 5680),
            CoordGrid(3758, 5681),
            CoordGrid(3758, 5685),
            CoordGrid(3759, 5674),
            CoordGrid(3759, 5676),
            CoordGrid(3759, 5682),
            CoordGrid(3760, 5674),
            CoordGrid(3760, 5680),
            CoordGrid(3761, 5674),
            CoordGrid(3761, 5675),
            CoordGrid(3761, 5680),
            CoordGrid(3761, 5681),
            CoordGrid(3762, 5657),
            CoordGrid(3762, 5658),
            CoordGrid(3762, 5670),
            CoordGrid(3762, 5671),
            CoordGrid(3762, 5672),
            CoordGrid(3762, 5673),
            CoordGrid(3762, 5675),
            CoordGrid(3762, 5680),
            CoordGrid(3762, 5682),
            CoordGrid(3762, 5683),
            CoordGrid(3763, 5656),
            CoordGrid(3763, 5662),
            CoordGrid(3763, 5663),
            CoordGrid(3763, 5670),
            CoordGrid(3763, 5681),
            CoordGrid(3763, 5682),
            CoordGrid(3763, 5683),
            CoordGrid(3764, 5656),
            CoordGrid(3764, 5665),
            CoordGrid(3764, 5677),
            CoordGrid(3765, 5656),
            CoordGrid(3765, 5677),
        )
    public val struts: List<CoordGrid> = listOf(CoordGrid(3742, 5663), CoordGrid(3742, 5669))
}
