package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object Ectophials : ObjReferences() {
    val full = find("ectophial")
    val empty = find("ectophial_empty")
}

internal object EctoGraphics : SpotanimReferences() {
    val pour = find("ectophial_pour_spotanim")
}

public class Ectophial : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(Ectophials.full) {
            if (!player.members || it.inventory !== inv) return@onOpHeld1
            val wildernessBase = if (coords.z >= 9920) 9920 else 3520
            if (
                player.isTeleportBlocked() ||
                    vars[varbits.inside_wilderness] != 0 && (coords.z - wildernessBase) / 8 + 1 > 20
            ) {
                mes("A magical force stops you from teleporting.")
                return@onOpHeld1
            }
            val slot = it.slot
            mes("You empty the ectoplasm onto the ground around your feet...")
            anim(EctoSeqs.pour)
            spotanim(EctoGraphics.pour)
            delay(3)
            if (!replace(slot, Ectophials.full, Ectophials.empty)) return@onOpHeld1
            val arrival = CoordGrid(3660, 3522)
            teleport(arrival)
            publish(PlayerTeleportEvent.Start(player, arrival))
            player.completeDiaryTask(DiaryTask.UseEctophial)
            refill(slot)
            delay(2)
        }
        onOpLocU(EctoLocs.fountain, Ectophials.empty) { if (player.members) refill(it.invSlot) }
        onOpNpc1(EctoNpcs.velorina) {
            if (!player.members || vars[EctoBits.quest] < 8) return@onOpNpc1
            if (EctoItems.ghostspeak.none { it in worn }) {
                mes("Wooo woooo wooooo woo.")
                return@onOpNpc1
            }
            val alreadyOwns =
                listOf(Ectophials.full, Ectophials.empty).any {
                    it in inv || player.invMap[invs.bank]?.contains(it) == true
                }
            val phial = if (alreadyOwns) Ectophials.empty else Ectophials.full
            if (invAdd(inv, phial).success) mes("Velorina gives you an ectophial.")
        }
    }

    private fun ProtectedAccess.replace(slot: Int, from: ObjType, to: ObjType): Boolean {
        if (inv[slot]?.id != from.id) return false
        return player
            .invTransaction(inv) {
                val bag = select(inv)
                delete(bag, from.id, 1, slot = slot)
                add(bag, to.id, 1, slot = slot)
            }
            .success
    }

    private fun ProtectedAccess.refill(slot: Int) {
        if (replace(slot, Ectophials.empty, Ectophials.full)) {
            anim(EctoSeqs.refill)
            mes("You refill the ectophial from the Ectofuntus.")
        }
    }
}
