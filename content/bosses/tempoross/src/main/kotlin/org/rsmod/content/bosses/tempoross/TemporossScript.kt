package org.rsmod.content.bosses.tempoross

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.content.skills.fishing.FishingOutfits
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TemporossScript
@Inject
internal constructor(
    private val voyages: TemporossVoyages,
    private val fishing: TemporossFishing,
    private val locs: LocTypeList,
    private val npcs: NpcTypeList,
    private val objects: ObjTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onAiConTimer(SeaSpirit.lobby) { voyages.tick(controller) }
        onOpLoc1(locs.getValue(41305)) { voyages.board(player, false) }
        onOpLoc2(locs.getValue(41305)) { voyages.board(player, false) }
        onOpLoc3(locs.getValue(41305)) { voyages.board(player, true) }
        onPlayerLogout { voyages.leave(player, true) }
        onPlayerLogin { if (player.vars[SeaVars.trip] != 0) voyages.leave(player, true) }
        onEvent<PlayerTeleportEvent.Start> { voyages.leave(player, false) }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            if (voyages.trip(player) != null) {
                voyages.leave(player, false)
                safeDestination = pier
            }
        }
        for (id in listOf(10565, 10568, 10569, 10570, 10571)) onOpNpc1(npcs.getValue(id)) {
            fishing.catch(this, it.npc)
        }
        for (id in 10576..10579) {
            onOpNpc1(npcs.getValue(id)) { fishing.load(this, id - 10576) }
            onOpNpc4(npcs.getValue(id)) { checkAmmo(id - 10576) }
        }
        for (id in 40968..40979) {
            val i = (id - 40968) % 4
            onOpLoc1(locs.getValue(id)) { fishing.load(this, i) }
            onOpLoc2(locs.getValue(id)) { checkAmmo(i) }
        }
        onOpLoc1(locs.getValue(41236)) { fishing.cook(this) }
        onOpLocU(locs.getValue(41236), objects.getValue(RAW)) { fishing.cook(this) }
        for ((i, item) in
            listOf(objs.hammer, objs.rope, objs.bucket_empty, objs.harpoon).withIndex()) onOpLoc1(
            locs.getValue(40964 + i)
        ) {
            if (voyages.trip(player) != null) invAdd(inv, item)
        }
        onOpLoc2(locs.getValue(40966)) {
            if (voyages.trip(player) != null && inv.freeSpace() > 0)
                invAdd(inv, objs.bucket_empty, minOf(5, inv.freeSpace()))
        }
        onOpLoc3(locs.getValue(40966)) {
            if (voyages.trip(player) != null && inv.freeSpace() > 0)
                invAdd(inv, objs.bucket_empty, minOf(10, inv.freeSpace()))
        }
        onOpLoc4(locs.getValue(40966)) {
            if (voyages.trip(player) == null) return@onOpLoc4
            val n = countDialog().coerceIn(0, inv.freeSpace())
            if (n > 0 && voyages.trip(player) != null) invAdd(inv, objs.bucket_empty, n)
        }
        for (id in listOf(41000, 41001, 41004)) {
            onOpLoc1(locs.getValue(id)) { fillBuckets() }
            onOpLocU(locs.getValue(id), objs.bucket_empty) { fillBuckets() }
        }
        // The wall and mast bases have per-player morphs for their tether and untether options.
        for (id in listOf(40998, 40999, 41012, 41233, 41352, 41353, 41354, 41355)) {
            onOpLoc1(locs.getValue(id)) { tether(it.loc.coords) }
            onOpLocU(locs.getValue(id), objs.rope) { tether(it.loc.coords) }
        }
        for ((i, ids) in TemporossVoyages.ANCHORS.withIndex()) {
            onOpLoc1(locs.getValue(ids.second)) { repair(i) }
            for (hammer in
                listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand)) onOpLocU(
                locs.getValue(ids.second),
                hammer,
            ) {
                repair(i)
            }
        }
        onOpNpc1(npcs.getValue(8643)) { douse(it.npc.coords) }
        onOpNpcU(npcs.getValue(8643), objs.bucket_water) { douse(it.npc.coords) }
        for (id in listOf(10585, 10586, 10587, 10588)) {
            onOpNpc3(npcs.getValue(id)) { leave() }
            onOpNpc1(npcs.getValue(id)) { leave() }
        }
        for ((id, dest) in listOf(10610 to CoordGrid(3148, 2842), 10611 to CoordGrid(3270, 3144))) {
            onOpNpc3(npcs.getValue(id)) { if (player.members) telejump(dest) }
            onOpNpc1(npcs.getValue(id)) {
                if (player.members && choice2("Take the ferry.", true, "Stay here.", false))
                    telejump(dest)
            }
        }
        for (id in listOf(10583, 10584)) onOpNpc1(npcs.getValue(id)) {
            mes(
                "Climb the rope ladder to join the crew, or choose Solo-start for your own expedition."
            )
        }
    }

    private fun ProtectedAccess.checkAmmo(i: Int) {
        val g = voyages.trip(player) ?: return
        mes("There are ${g.cannons[i].size} harpoonfish in the ammunition crate.")
    }

    private suspend fun ProtectedAccess.leave() {
        val g = voyages.trip(player) ?: return
        if (g.returnIn >= 0 || choice2("Forfeit and leave the cove.", true, "Stay.", false))
            voyages.leave(player, true)
    }

    private fun ProtectedAccess.fillBuckets() {
        val n = invTotal(inv, objs.bucket_empty)
        if (n <= 0) {
            mes("You need a bucket.")
            return
        }
        player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag, objs.bucket_empty.id, n)
            add(bag, objs.bucket_water.id, n)
        }
    }

    private fun ProtectedAccess.tether(coords: CoordGrid) {
        val g = voyages.trip(player) ?: return
        val i =
            TemporossVoyages.ANCHORS.indices.firstOrNull { voyages.anchor(g, it) == coords }
                ?: return
        if (g.returnIn >= 0 || i in g.broken) return
        if (player in g.tethered) {
            voyages.untether(g, player)
            anim(SeaSeqs.untether)
            return
        }
        val outfit =
            listOf(
                FishingOutfits.spiritHat,
                FishingOutfits.spiritTop,
                FishingOutfits.spiritLegs,
                FishingOutfits.spiritBoots,
            )
        if (objs.rope !in inv && !outfit.all { it in worn }) {
            mes("You need a rope to tether yourself.")
            return
        }
        g.tethered[player] = player.coords
        vars[SeaBits.tethered] = 1
        anim(SeaSeqs.tether)
        mes("You tether yourself securely.")
    }

    private suspend fun ProtectedAccess.repair(i: Int) {
        val g = voyages.trip(player) ?: return
        fun hammer() =
            listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand).any {
                it in inv || it in worn
            }
        if (!hammer()) {
            mes("You need a hammer to repair this.")
            return
        }
        gatheringDelay(3)
        if (voyages.trip(player) !== g || !hammer() || g.returnIn >= 0 || !voyages.repair(g, i))
            return
        g.add(player, 40)
        if (vars[varbits.poh_house_location] > 0)
            statAdvance(stats.construction, 4.0 * stat(stats.construction))
    }

    private fun ProtectedAccess.douse(coords: CoordGrid) {
        val g = voyages.trip(player) ?: return
        if (coords !in g.fires || g.returnIn >= 0) return
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.bucket_water.id, 1)
                add(bag, objs.bucket_empty.id, 1)
            }
        if (!tx.success) {
            mes("You need a bucket of water.")
            return
        }
        voyages.douse(g, coords)
        g.add(player, 40)
    }
}
