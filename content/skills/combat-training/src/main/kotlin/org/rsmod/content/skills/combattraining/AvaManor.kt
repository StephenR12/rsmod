package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ManorLocs : LocReferences() {
    val left = find("hauntedbookcasel")
    val right = find("hauntedbookcaser")
    val open = find("hauntedbookcaseopen")
    val lever = find("hauntedleverup")
}

internal object AvaShop : InvReferences() {
    val inventory = find("anma_shop")
}

internal object AvaShopItems : ObjReferences() {
    val feather = find("feather")
    val pack = find("pack_feather")
}

internal object AvaStock : InvEditor() {
    init {
        edit(AvaShop.inventory) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(AvaShopItems.feather, 1000, 100)
            stock += stock(AvaShopItems.pack, 100, 10)
            stock += stock(objs.iron_arrow, 40, 100)
            stock += stock(objs.steel_arrow, 10, 100)
            stock += stock(objs.iron_arrowheads, 30, 100)
            stock += stock(objs.steel_arrowheads, 20, 100)
        }
    }
}

class AvaManor @Inject constructor(private val locs: LocRepository, private val shops: Shops) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (type in listOf(ManorLocs.left, ManorLocs.right, ManorLocs.lever)) {
            onOpLoc1(type) {
                if (it.loc.coords.x in 3096..3097 && it.loc.coords.z in 3357..3359) openBookcase()
            }
        }
        for (npc in listOf(AvaNpcs.ava, AvaNpcs.multi)) {
            onOpNpc3(npc) {
                if (player.members && vars[AvaQuests.animalMagnetism] >= 240) {
                    shops.open(
                        player,
                        "Ava's Odds and Ends",
                        AvaShop.inventory,
                        buyPercentage = 40.0,
                        sellPercentage = 130.0,
                        changePercentage = 3.0,
                    )
                } else mes("You must complete Animal Magnetism to use Ava's shop.")
            }
        }
    }

    private fun openBookcase() {
        val left = locs.findExact(CoordGrid(3097, 3358), ManorLocs.left) ?: return
        val right = locs.findExact(CoordGrid(3097, 3359), ManorLocs.right) ?: return
        for ((shelf, dz) in listOf(left to -1, right to 1)) {
            locs.del(shelf, 10)
            locs.add(shelf.coords.translate(0, dz), ManorLocs.open, 10, shelf.angle, shelf.shape)
        }
    }
}
