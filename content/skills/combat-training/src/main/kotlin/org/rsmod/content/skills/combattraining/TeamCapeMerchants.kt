package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
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

internal object CapeMerchantNpcs : NpcReferences() {
    val merchants = (1..10).map { find("wilderness_capeseller_$it") }
}

internal object TeamCapeInvs : InvReferences() {
    val shops = (1..10).map { find("wildernesscapeshop$it") }
}

internal object TeamCapes : ObjReferences() {
    val capes = (1..50).map { find("wilderness_cape_$it") }
}

internal object TeamCapeStock : InvEditor() {
    init {
        for ((index, shop) in TeamCapeInvs.shops.withIndex()) edit(shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (colour in 0..4) stock +=
                stock(TeamCapes.capes[index + 10 * colour], count = 100, restockCycles = 100)
        }
    }
}

class TeamCapeMerchants @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        val names =
            listOf(
                "William",
                "Ian",
                "Larry",
                "Darren",
                "Edward",
                "Richard",
                "Neil",
                "Edmond",
                "Simon",
                "Sam",
            )
        for ((index, merchant) in CapeMerchantNpcs.merchants.withIndex()) {
            val inv = TeamCapeInvs.shops[index]
            val title = "${names[index]}'s Wilderness Cape Shop"
            onOpNpc1(merchant) {
                if (choice2("I'd like to see your capes.", true, "Maybe later.", false))
                    shops.open(
                        player,
                        title,
                        inv,
                        buyPercentage = 60.0,
                        sellPercentage = 100.0,
                        changePercentage = 1.0,
                    )
            }
            onOpNpc3(merchant) {
                shops.open(
                    player,
                    title,
                    inv,
                    buyPercentage = 60.0,
                    sellPercentage = 100.0,
                    changePercentage = 1.0,
                )
            }
        }
    }
}
