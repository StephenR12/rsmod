package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ChargingLocs : LocReferences() {
    val wilderness =
        listOf(find("fountain_of_rune_obelisk_desktop"), find("fountain_of_rune_obelisk_osm"))
    val heroes = listOf(find("fountain_of_heroes_desktop"), find("fountain_of_heroes_osm"))
    val legends = listOf(find("lg_totem_pole_legends_desktop"), find("lg_totem_pole_legends_osm"))
}

internal object WealthRings : ObjReferences() {
    val normal = listOf(find("ring_of_wealth")) + (1..5).map { find("ring_of_wealth_$it") }
    val imbued = listOf(find("ring_of_wealth_i")) + (1..5).map { find("ring_of_wealth_i$it") }
}

public class JewelleryCharging @Inject constructor(private val objTypes: ObjTypeList) :
    PluginScript() {
    private enum class Site {
        Wilderness,
        Heroes,
        Legends,
    }

    private data class Kind(val tiers: List<ObjType>, val site: Site)

    private data class Change(
        val bag: Inventory,
        val slot: Int,
        val input: Int,
        val count: Int,
        val output: Int,
    )

    private val kinds by lazy {
        fun family(cap: ObjType, fifth: ObjType, sixth: ObjType, site: Site) =
            Kind(
                RechargeDragonstoneRecipes.all.filter { it.output.id == cap.id }.map { it.input } +
                    listOf(cap, fifth, sixth),
                site,
            )
        listOf(
            family(
                objs.amulet_of_glory_4,
                objs.amulet_of_glory_5,
                objs.amulet_of_glory_6,
                Site.Heroes,
            ),
            family(
                objs.amulet_of_glory_t_4,
                objs.amulet_of_glory_t_5,
                objs.amulet_of_glory_t_6,
                Site.Heroes,
            ),
            family(
                objs.combat_bracelet_4,
                objs.combat_bracelet_5,
                objs.combat_bracelet_6,
                Site.Legends,
            ),
            family(
                objs.skills_necklace_4,
                objs.skills_necklace_5,
                objs.skills_necklace_6,
                Site.Legends,
            ),
            Kind(WealthRings.normal, Site.Wilderness),
            Kind(WealthRings.imbued, Site.Wilderness),
        )
    }

    override fun ScriptContext.startup() {
        for ((site, locs) in
            mapOf(
                Site.Heroes to ChargingLocs.heroes,
                Site.Legends to ChargingLocs.legends,
                Site.Wilderness to ChargingLocs.wilderness,
            )) {
            for (loc in locs) {
                onOpLoc1(loc) { charge(site) }
                for (kind in
                    kinds.filter { site == Site.Wilderness || site == it.site }) for (tier in
                    kind.tiers) {
                    onOpLocU(loc, tier) { charge(site) }
                    if (kind.site == Site.Legends) {
                        val note = objTypes.cert(objTypes[tier])
                        if (note.id != tier.id) onOpLocU(loc, note) { charge(site) }
                    }
                }
            }
        }
    }

    private fun ProtectedAccess.charge(site: Site) {
        if (!player.members) return
        val changes = mutableListOf<Change>()
        for (bag in listOf(inv, worn)) for (slot in bag.indices) {
            val item = bag[slot] ?: continue
            val original = objTypes[item]
            val type = objTypes.uncert(original)
            val kind =
                kinds.firstOrNull { family -> family.tiers.any { it.id == type.id } } ?: continue
            if (
                site != Site.Wilderness && site != kind.site ||
                    original.isCert && kind.site != Site.Legends
            )
                continue
            if (
                kind.site == Site.Heroes && vars[varps.heroes_quest] < 14 ||
                    kind.site == Site.Legends && vars[varps.legends_quest] < 75
            )
                continue
            val cap = if (site == Site.Wilderness) kind.tiers.lastIndex else 4
            val current = kind.tiers.indexOfFirst { it.id == type.id }
            if (current >= cap) continue
            val eternal =
                site == Site.Wilderness && kind.site == Site.Heroes && random.of(25000) == 0
            val next = if (eternal) objs.amulet_of_eternal_glory else kind.tiers[cap]
            val output = if (original.isCert) objTypes.cert(objTypes[next]).id else next.id
            changes += Change(bag, slot, item.id, item.count, output)
        }
        if (changes.isEmpty()) {
            mes("You have no jewellery that can be recharged here.")
            return
        }
        val result =
            player.invTransaction(inv, worn) {
                for (change in changes) {
                    val bag = select(change.bag)
                    delete(bag, change.input, change.count, slot = change.slot)
                    add(bag, change.output, change.count, slot = change.slot)
                }
            }
        if (!result.success) return
        if (changes.any { it.bag === worn }) rebuildAppearance()
        if (changes.any { it.output == objs.amulet_of_eternal_glory.id })
            mes("The fountain makes your amulet glow with eternal glory.")
        else mes("Your jewellery is recharged.")
        if (site == Site.Legends) player.completeDiaryTask(DiaryTask.RechargeLegendsTotem)
    }
}
