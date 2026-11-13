package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object StoneShop : InvReferences() {
    val inventory = find("poh_stonemason_shop")
}

internal object StoneTrader : NpcReferences() {
    val stonemason = find("poh_stonemason")
}

internal object StoneMaterials : ObjReferences() {
    val goods =
        listOf(
            find("limestonebrick") to 1000,
            find("marble_block") to 20,
            find("gold_leaf") to 20,
            find("poh_magic_crystal") to 10,
            find("poh_condensed_gold") to 10,
        )
}

internal object StonemasonStock : InvEditor() {
    init {
        edit(StoneShop.inventory) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for ((item, count) in StoneMaterials.goods) stock +=
                stock(item, count = count, restockCycles = 100)
        }
    }
}

public class Stonemason @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3(StoneTrader.stonemason) { browse() }
        onApNpc3(StoneTrader.stonemason) { if (isWithinApRange(it.npc, 2)) browse() }
        onOpNpc1(StoneTrader.stonemason) {
            if (choice2("Trade construction materials.", true, "Leave.", false)) browse()
        }
    }

    private fun ProtectedAccess.browse() {
        if (!player.members) return
        shops.open(player, "Keldagrim Stonemason", StoneShop.inventory, 50.0, 130.0, 3.0)
        player.completeDiaryTask(DiaryTask.BrowseKeldagrimStonemason)
    }
}
