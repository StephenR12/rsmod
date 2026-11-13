package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object QuetzalNpcs : NpcReferences() {
    val pitri = find("hunter_guild_pitri")
}

internal object QuetzalSeqs : SeqReferences() {
    val signal = find("human_quetzal_whistle")
}

public class QuetzalWhistles @Inject constructor(private val items: ObjTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(QuetzalNpcs.pitri) {
            when (
                choice3(
                    "Ask about a whistle blueprint.",
                    1,
                    "Recharge my whistles.",
                    2,
                    "Leave.",
                    0,
                )
            ) {
                1 -> blueprint()
                2 -> charge()
            }
        }
        onOpNpc3(QuetzalNpcs.pitri) { charge() }
        for ((id, tier) in plans) {
            onOpHeld1(items.getValue(id)) {
                mes(
                    "You'll need a knife, " +
                        items.getValue(logs[tier]).name.lowercase() +
                        (if (tier > 0) " and a ${items.getValue(whistles[tier-1]).name.lowercase()}"
                        else "") +
                        " to craft this whistle."
                )
            }
            onOpHeld2(items.getValue(id)) { craft(id, tier, it.slot) }
        }
        for ((tier, id) in whistles.withIndex()) {
            onOpHeld1(items.getValue(id)) { signal(id, it.slot) }
            onOpHeld2(items.getValue(id)) {
                mes("Your quetzal whistle has ${inv[it.slot]?.vars?:0} charges remaining.")
            }
            onOpHeld3(items.getValue(id)) {
                val active = GuildField.Active.get(player) - 1
                val task =
                    if (active < 0) 0
                    else
                        GuildField.Temporary.get(player).takeIf { it != 0 }
                            ?: ((vars[GuildVars.blocks] ushr (5 * active)) and 31)
                val target = RumourCatalogue.rumours.getOrNull(task - 1)
                mes(
                    if (target == null) "You aren't investigating a rumour."
                    else
                        "Your rumour is about a ${target.name.lowercase()}. ${names[active]} is looking for ${items.getValue(target.part).name.lowercase()}."
                )
            }
            onOpNpcU(QuetzalNpcs.pitri, items.getValue(id)) { charge() }
        }
        for (meat in feeds.keys) onOpNpcU(QuetzalNpcs.pitri, items.getValue(meat)) { charge() }
    }

    private fun ProtectedAccess.blueprint() {
        if (vars[varps.hunter_rumours_completed] < 10) {
            mes("Complete ten hunters' rumours and I'll give you a whistle blueprint.")
            return
        }
        val fee = if (vars[GuildVars.rewards] and 64 == 0) 0 else 500
        if (fee > invCoinTotal()) {
            mes("A replacement blueprint costs 500 coins.")
            return
        }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                if (fee > 0) delete(bag, objs.coins.id, fee)
                add(bag, 29251, 1)
            }
        if (result.success) {
            vars[GuildVars.rewards] = vars[GuildVars.rewards] or 64
            mes("Pitri hands you a basic quetzal whistle blueprint.")
        } else mes("Make room for the blueprint.")
    }

    private fun ProtectedAccess.craft(plan: Int, tier: Int, slot: Int) {
        val lower =
            if (tier > 0) inv.indices.firstOrNull { inv[it]?.id == whistles[tier - 1] } else null
        if (
            objs.knife !in inv || items.getValue(logs[tier]) !in inv || (tier > 0 && lower == null)
        ) {
            mes("You need a knife, the correct logs and any previous-tier whistle.")
            return
        }
        val charges = lower?.let { inv[it]?.vars } ?: 0
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, plan, 1, slot)
                delete(bag, logs[tier], 1)
                if (lower != null) delete(bag, whistles[tier - 1], 1, lower)
                add(bag, whistles[tier], 1, vars = charges)
            }
        if (result.success) mes("You craft a ${items.getValue(whistles[tier]).name.lowercase()}.")
    }

    private fun ProtectedAccess.charge() {
        val slots = inv.indices.filter { inv[it]?.id in whistles }
        if (slots.isEmpty()) {
            mes("Bring me a quetzal whistle to charge.")
            return
        }
        var total = 0
        for (slot in slots) {
            val whistle = inv[slot] ?: continue
            val cap = capacities[whistles.indexOf(whistle.id)]
            var charges = whistle.vars
            for (i in inv.indices) {
                if (charges >= cap) break
                val food = inv[i] ?: continue
                val value = feeds[food.id] ?: continue
                val quantity = minOf(food.count, (cap - charges + value - 1) / value)
                if (invDel(inv, items.getValue(food.id), quantity, slot = i).success)
                    charges = minOf(cap, charges + quantity * value)
            }
            total += charges - whistle.vars
            inv[slot] = whistle.copy(vars = charges)
        }
        mes(
            if (total == 0)
                "Bring raw hunter meat or quetzal feed and a whistle with room for more charges."
            else "Pitri recharges your whistles by $total charges."
        )
    }

    private suspend fun ProtectedAccess.signal(id: Int, slot: Int) {
        val whistle = inv[slot] ?: return
        if (!player.members || whistle.vars <= 0) {
            mes("Your whistle has no charges.")
            return
        }
        if (
            player.isTeleportBlocked() ||
                vars[varbits.inside_wilderness] != 0 && wildernessLevel() > 20
        ) {
            mes("A magical force stops you from teleporting.")
            return
        }
        anim(QuetzalSeqs.signal)
        delay(3)
        if (inv[slot] != whistle || player.isTeleportBlocked()) return
        inv[slot] = whistle.copy(vars = whistle.vars - 1)
        val landing = CoordGrid(1585, 3053)
        teleport(landing)
        publish(PlayerTeleportEvent.Start(player, landing))
    }

    private companion object {
        val names = listOf("Gilman", "Cervus", "Ornus", "Aco", "Teco", "Wolf")
        val plans = mapOf(29251 to 0, 29253 to 1, 29259 to 1, 29256 to 2, 29261 to 2)
        val whistles = listOf(29271, 29273, 29275)
        val capacities = listOf(5, 20, 50)
        val logs = listOf(1519, 1515, 19669)
        val feeds =
            mapOf(
                29307 to 1,
                29104 to 1,
                29101 to 1,
                29122 to 1,
                29119 to 2,
                29125 to 2,
                29110 to 2,
                29107 to 3,
                29116 to 3,
                29113 to 3,
            )
    }
}
