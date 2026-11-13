package org.rsmod.content.skills.maxcape

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.events.ObjTradeEvents
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HicktonItems : ObjReferences() {
    val supplies =
        listOf(
            Triple(find("bolt"), 200, 10),
            Triple(find("bronze_arrow"), 1000, 10),
            Triple(find("iron_arrow"), 800, 20),
            Triple(find("steel_arrow"), 0, 1000),
            Triple(find("mithril_arrow"), 0, 2000),
            Triple(find("adamant_arrow"), 0, 3000),
            Triple(find("rune_arrow"), 0, 4000),
            Triple(find("zogre_brutal_bronze"), 0, 1000),
            Triple(find("zogre_brutal_iron"), 0, 1000),
            Triple(find("zogre_brutal_steel"), 0, 1000),
            Triple(find("zogre_brutal_black"), 0, 1500),
            Triple(find("zogre_brutal_mithril"), 0, 2000),
            Triple(find("zogre_brutal_adamant"), 0, 3000),
            Triple(find("zogre_brutal_rune"), 0, 4000),
            Triple(find("bronze_arrowheads"), 1000, 10),
            Triple(find("iron_arrowheads"), 800, 20),
            Triple(find("steel_arrowheads"), 600, 40),
            Triple(find("mithril_arrowheads"), 400, 40),
            Triple(find("adamant_arrowheads"), 200, 40),
            Triple(find("rune_arrowheads"), 100, 40),
            Triple(find("shortbow"), 4, 100),
            Triple(find("longbow"), 2, 200),
            Triple(find("crossbow"), 2, 400),
            Triple(find("oak_shortbow"), 4, 800),
            Triple(find("oak_longbow"), 4, 1600),
            Triple(find("zogre_bow"), 0, 2000),
            Triple(find("studded_body"), 2, 1000),
            Triple(find("studded_chaps"), 2, 1000),
        )
}

internal object HicktonInvs : InvReferences() {
    val shops =
        listOf("archeryshop2", "archeryshop2_skillcape", "archeryshop2_skillcape_trimmed").map {
            find(it)
        }
}

internal object HicktonStock : InvEditor() {
    init {
        for ((i, shop) in HicktonInvs.shops.withIndex()) edit(shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for ((obj, amount, interval) in HicktonItems.supplies) stock +=
                stock(obj, amount, interval)
            if (i > 0)
                stock +=
                    stock(
                        if (i == 1) objs.fletching_skillcape else objs.fletching_skillcape_t,
                        1,
                        1,
                    )
        }
    }
}

public class HicktonShop
@Inject
constructor(private val shops: Shops, private val ground: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CapeTrainers.hickton) {
            mes("You can buy archery supplies from me. Master fletchers can purchase a cape.")
        }
        onOpNpc3(CapeTrainers.hickton) {
            if (!player.members) return@onOpNpc3
            val index =
                if (statBase(stats.fletching) < 99) 0
                else if (SkillcapeSale.isTrimmed(player)) 2 else 1
            shops.open(
                player,
                "Hickton's Archery Emporium",
                HicktonInvs.shops[index],
                50.0,
                100.0,
                1.0,
            )
        }
        onEvent<ObjTradeEvents.Purchased> {
            val index = HicktonInvs.shops.indexOfFirst { it.id == shop.id }
            if (
                (index == 1 && obj.id == objs.fletching_skillcape.id) ||
                    (index == 2 && obj.id == objs.fletching_skillcape_t.id)
            )
                player.invAddOrDrop(ground, objs.fletching_hood, count)
        }
    }
}
