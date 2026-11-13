package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.*
import org.rsmod.api.player.output.privateLocChange
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object LoreLocs : LocReferences() {
    val chest = find("hg_lorechest_closed")
    val opened = find("hg_lorechest_open")
    val shelf = find("hg_loreshelf")
}

public class GuildLore @Inject constructor(private val items: ObjTypeList) : PluginScript() {
    private val milestones = listOf(50 to 29317, 100 to 29319, 150 to 29323)

    override fun ScriptContext.startup() {
        onOpLoc1(LoreLocs.shelf) { searchShelf() }
        onApLoc1(LoreLocs.shelf) { if (isWithinApRange(it.loc, 1)) searchShelf() }
        onOpLoc1(LoreLocs.chest) { openChest(it.loc) }
        onOpLocU(LoreLocs.chest, items.getValue(29323)) { openChest(it.loc) }
        val notes =
            mapOf(
                29317 to
                    "An expedition requests tools to establish an outpost by a cenote. Some of the explorers stayed to investigate beneath the tree.",
                29319 to
                    "The outpost grew into a refuge for hunters. Some adventurers ventured past its guards into the caverns and disappeared.",
                29321 to
                    "Apatura's predecessor entrusted her with the Guild and its cavern. She must decide who may enter and whether to authorise her partner's ocean expedition.",
            )
        for ((id, passage) in notes) onOpHeld1(items.getValue(id)) { mes(passage) }
    }

    private fun ProtectedAccess.searchShelf() {
        val missing =
            milestones.mapIndexedNotNull { index, (_, id) ->
                val received = vars[GuildVars.rewards] and (128 shl index) != 0
                id.takeIf { received && !owned(id) && (id != 29323 || !owned(29321)) }
            }
        if (missing.isEmpty()) mes("You find nothing else on the bookshelf.")
        else
            for (id in missing) {
                if (invAdd(inv, items.getValue(id)).failure) {
                    mes("Make room to retrieve the remaining documents.")
                    break
                }
                mes("You retrieve ${items.getValue(id).name.lowercase()} from the bookshelf.")
            }
    }

    internal fun award(access: ProtectedAccess): Boolean =
        with(access) {
            val next =
                milestones.indices.firstOrNull { index ->
                    vars[varps.hunter_rumours_completed] >= milestones[index].first &&
                        vars[GuildVars.rewards] and (128 shl index) == 0
                } ?: return false
            val item = items.getValue(milestones[next].second)
            if (invAdd(inv, item).success) {
                vars[GuildVars.rewards] = vars[GuildVars.rewards] or (128 shl next)
                mes("Apatura gives you ${item.name.lowercase()} for your work on hunters' rumours.")
            } else mes("Make room for Apatura's reward.")
            true
        }

    private fun ProtectedAccess.owned(id: Int): Boolean {
        val item = items.getValue(id)
        return item in inv || player.invMap[invs.bank]?.contains(item) == true
    }

    private suspend fun ProtectedAccess.openChest(loc: BoundLocInfo) {
        if (owned(29321)) {
            mes("You have already taken Apatura's note.")
            return
        }
        val key = items.getValue(29323)
        if (key !in inv) {
            mes("The chest is locked.")
            return
        }
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, key.id, 1)
                add(bag, 29321, 1)
            }
        if (tx.failure) return
        val base = LocInfo(loc.layer, loc.coords, loc.entity)
        player.privateLocChange(
            base.copy(entity = LocEntity(LoreLocs.opened.id, loc.shapeId, loc.angleId))
        )
        mes("Inside the chest you find a note addressed to Apatura.")
        delay(2)
        player.privateLocChange(base)
    }
}
