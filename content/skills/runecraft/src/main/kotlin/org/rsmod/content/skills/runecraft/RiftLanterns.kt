package org.rsmod.content.skills.runecraft

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object LanternLogs : ObjReferences() {
    val all =
        listOf(
                "logs",
                "red_logs",
                "green_logs",
                "blue_logs",
                "trail_logs_white",
                "trail_logs_purple",
                "oak_logs",
                "willow_logs",
                "maple_logs",
                "yew_logs",
                "blisterwood_logs",
                "magic_logs",
                "redwood_logs",
            )
            .map { find(it) }
}

public class RiftLanterns : PluginScript() {
    override fun ScriptContext.startup() {
        for ((i, log) in LanternLogs.all.withIndex()) {
            val lit = RiftItems.lanterns.values.elementAt(i)
            onOpHeldU(RiftItems.lantern, log) { light(log, lit, i) }
            onOpHeld3(lit) {
                if (invReplace(inv, it.slot, 1, RiftItems.lantern).success)
                    mes("You extinguish the abyssal lantern.")
            }
            onOpHeld4(lit) { mes(effects[i]) }
        }
    }

    private fun ProtectedAccess.light(log: ObjType, lit: ObjType, i: Int) {
        if (!player.members) return
        if (statBase(stats.firemaking) < levels[i] || objs.tinderbox !in inv) {
            mes("You need a tinderbox and level ${levels[i]} Firemaking.")
            return
        }
        if (RiftItems.lanterns.values.any { it in inv || it in bank || it in player.worn }) {
            mes("You already have a lit abyssal lantern.")
            return
        }
        val result =
            player.invTransaction(inv) {
                val b = select(inv)
                delete(b, log.id, 6)
                delete(b, RiftItems.lantern.id, 1)
                add(b, lit.id, 1)
            }
        if (result.success) {
            anim(RiftAnims.lantern)
            mes(effects[i])
        } else mes("You need six matching logs to light the lantern.")
    }

    private companion object {
        val levels = listOf(1, 1, 1, 1, 1, 1, 15, 30, 45, 60, 62, 75, 90)
        val effects =
            List(6) { "You have a better chance of finding portal talismans." } +
                listOf(
                    "You can earn extra contribution energy.",
                    "You craft extra runes in the rift.",
                    "Your chance of finding portal talismans increases.",
                    "You can earn extra contribution energy.",
                    "You craft extra blood runes in the rift.",
                    "You craft extra runes in the rift.",
                    "Your pouches are protected and you can earn extra energy and runes.",
                )
    }
}
