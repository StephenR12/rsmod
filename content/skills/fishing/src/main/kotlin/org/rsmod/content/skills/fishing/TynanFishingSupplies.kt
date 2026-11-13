package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object TynanNpcs : NpcReferences() {
    val trader = find("piscarilius_fishing_supplies_trader")
}

public object TynanInvs : InvReferences() {
    val supplies = find("piscarilius_fishing_supplies")
}

public object TynanObjs : ObjReferences() {
    val baitPack = find("pack_fishing_bait")
    val sandwormPack = find("piscarilius_pack_sandworms")
}

/** Tynan stock oldid14994365, 26 September 2025; shop multipliers1000/850/delta10. */
internal object TynanStock : InvEditor() {
    init {
        edit(TynanInvs.supplies) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(objs.bucket_empty, 1000, 1)
            stock += stock(objs.small_fishing_net, 5, 100)
            stock += stock(objs.big_fishing_net, 5, 100)
            stock += stock(objs.fishing_rod, 5, 100)
            stock += stock(objs.harpoon, 2, 400)
            stock += stock(objs.lobster_pot, 2, 400)
            stock += stock(objs.fishing_bait, 1200, 1)
            stock += stock(HighLevelFishingObjs.sandworms, 500, 50)
            stock += stock(TynanObjs.baitPack, 80, 5)
            stock += stock(TynanObjs.sandwormPack, 50, 5)
        }
    }
}

public class TynanFishingSupplies @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3(TynanNpcs.trader) { trade() }
        onOpNpc1(TynanNpcs.trader) {
            if (choice2("I'd like to buy some fishing supplies.", true, "Leave.", false)) trade()
        }
        onOpHeld1(TynanObjs.baitPack) { openPack(it.slot, TynanObjs.baitPack, objs.fishing_bait) }
        onOpHeld1(TynanObjs.sandwormPack) {
            openPack(it.slot, TynanObjs.sandwormPack, HighLevelFishingObjs.sandworms)
        }
    }

    private fun ProtectedAccess.trade() {
        if (player.members)
            shops.open(player, "Tynan's Fishing Supplies", TynanInvs.supplies, 85.0, 100.0, 1.0)
    }

    private fun ProtectedAccess.openPack(slot: Int, pack: ObjType, output: ObjType) {
        if (!player.members || player.inv[slot]?.id != pack.id) return
        player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete(inv, pack.id, 1, slot)
            add(inv, output.id, 100)
        }
    }
}
