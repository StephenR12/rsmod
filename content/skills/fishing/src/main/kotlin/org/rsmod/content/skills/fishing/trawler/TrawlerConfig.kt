package org.rsmod.content.skills.fishing.trawler

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType

public object TrawlerLocs : LocReferences() {
    val board = find("trawler_gangplank")
    val leave = find("trawler_gangplankoff")
    val up = find("trawler_ladder")
    val down = find("trawler_laddertop")
    val reward = find("trawlernetland")
    val nets = listOf(find("trawlernet_l"), find("trawlernet_r"))
    val hulls = (0..15).map { find("trawler_hull_" + it.toString().padStart(2, '0')) }
    val leak = find("trawler_hull_leak")
    val rails = listOf(find("trawler_shiprail_broken_op"), find("trawler_shiprail_broken_floor_op"))
    val buckets = find("trawler_crate_bucket")
    val hammers = find("trawler_crate_hammer")
    val axes = find("trawler_crate_axes")
    val barrels = (1..3).map { find("sinkingbarrel$it") }
}

public object TrawlerObjs : ObjReferences() {
    val bucket = find("bucket_bailing")
    val full = find("bucket_bailingfull")
    val paste = find("swamppaste")
    val jugPack = find("pack_jug_empty")
    val bucketPack = find("pack_bucket")
}

public object TrawlerNpcs : NpcReferences() {
    val shore = find("murphy")
    val lobby = find("murphy_lobby")
    val sea = find("murphy_normal")
    val trader = find("khazard_shopkeeper")
    val tentacle = find("trawler_tentacle_op")
}

public object TrawlerBits : VarBitReferences() {
    val status = find("trawler_status")
    val points = find("trawler_contribution")
    val water = find("trawler_water_percentage")
    val net = find("trawler_netinfo")
    val time = find("trawler_timeinfo")
    val catch = find("trawler_catchinfo")
    val hulls = (0..15).map { find("trawler_hull_" + it.toString().padStart(2, '0')) }
    val rail = find("trawler_shiprail_status")
    val tentacle = find("trawler_tentacle_status")
}

public object TrawlerVars : VarpReferences() {
    val session = find("fishing_trawler_session")
    val xp = find("fishing_trawler_reward_xp")
    val manta = find("fishing_trawler_reward_manta")
    val inspected = find("fishing_trawler_reward_inspected")
}

public object TrawlerInvs : InvReferences() {
    val rewards = find("trawler_rewardinv")
    val shop = find("khazardshop")
}

public object TrawlerInterfaces : InterfaceReferences() {
    val overlay = find("trawler_overlay")
    val rewards = find("trawler_reward")
}

public object TrawlerButtons : ComponentReferences() {
    val items = find("trawler_reward:reward_inv")
    val bank = find("trawler_reward:bank_all")
    val discard = find("trawler_reward:discard_all")
}

public object TrawlerControllers : ControllerBuilder() {
    val round = build("fishing_trawler_round")
}

internal object TrawlerVarsBuild : VarpBuilder() {
    init {
        for (name in
            listOf(
                "fishing_trawler_session",
                "fishing_trawler_reward_xp",
                "fishing_trawler_reward_manta",
                "fishing_trawler_reward_inspected",
            )) build(name) { transmitNever = true }
    }
}

internal object TrawlerInventories : InvEditor() {
    init {
        edit(TrawlerInvs.rewards) {
            scope = InvScope.Perm
            stack = InvStackType.Always
            size = 28
            protect = false
        }
        edit(TrawlerInvs.shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for ((obj, count, restockTicks) in
                listOf(
                    Triple(objs.bronze_pickaxe, 5, 100),
                    Triple(objs.pot_empty, 3, 100),
                    Triple(objs.jug_empty, 2, 100),
                    Triple(TrawlerObjs.jugPack, 5, 20),
                    Triple(objs.shears, 2, 100),
                    Triple(objs.bucket_empty, 2, 100),
                    Triple(TrawlerObjs.bucketPack, 15, 10),
                    Triple(objs.tinderbox, 2, 100),
                    Triple(objs.chisel, 2, 100),
                    Triple(objs.hammer, 5, 100),
                    Triple(objs.rope, 30, 20),
                    Triple(objs.pot_of_flour, 30, 100),
                    Triple(TrawlerObjs.bucket, 30, 100),
                    Triple(TrawlerObjs.paste, 5000, 10),
                    Triple(objs.knife, 2, 100),
                    Triple(objs.bronze_axe, 5, 100),
                )) stock += stock(obj, count, restockTicks)
        }
    }
}

internal object TrawlerNpcEdits : NpcEditor() {
    init {
        for (type in
            listOf(
                TrawlerNpcs.shore,
                TrawlerNpcs.lobby,
                TrawlerNpcs.sea,
                TrawlerNpcs.tentacle,
            )) edit(type) { wanderRange = 0 }
    }
}

internal object TrawlerPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<TrawlerScript>("people.toml")
    }
}
