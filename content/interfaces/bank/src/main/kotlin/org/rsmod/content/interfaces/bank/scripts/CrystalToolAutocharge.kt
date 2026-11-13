package org.rsmod.content.interfaces.bank.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

internal object FellingAxe : ObjReferences() {
    val charged = find("crystal_axe_2h")
    val empty = find("crystal_axe_2h_inactive")
}

public class CrystalToolAutocharge @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        val tools = listOf(objs.crystal_axe to objs.crystal_axe_inactive,
            objs.crystal_pickaxe to objs.crystal_pickaxe_inactive, objs.crystal_harpoon to objs.crystal_harpoon_inactive,
            FellingAxe.charged to FellingAxe.empty)
        for ((charged,empty) in tools) {
            for (form in listOf(charged,empty)) onProtectedEvent<BankedObjEvents.Autocharge>(form.id) { e ->
                if (!player.members || inv[e.slot] != e.obj) return@onProtectedEvent
                val chargeBits = varobjs.crystal_tool_charges.bits
                val current = if (form == empty) 0 else e.obj.vars.getBits(chargeBits).let { if (it == 0) 10_000 else it }
                val needed = vars[varbits.charges_crystal_tools_quantity].coerceAtMost(MAX) - current
                if (needed <= 0) return@onProtectedEvent
                val shards = (needed + 99) / 100
                val paid = e.takeMaterial(objs.crystal_shard,shards)
                if (paid > 0) inv[e.slot] = InvObj(charged,e.obj.count,
                    e.obj.vars.withBits(chargeBits,(current + paid * 100).coerceAtMost(MAX)))
                if (paid < shards) mes("Your bank has insufficient crystal shards to fully charge this item.")
            }
        }
    }
    private companion object { const val MAX = 20_000 }
}
