package org.rsmod.content.bosses.world

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.interfaces.bank.BankProximity
import org.rsmod.content.interfaces.bank.deliverToBank
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object EggSacItems : ObjReferences() {
    val open = find("hosdun_egg_sac")
}

public class GiantEggSac
@Inject
constructor(private val banks: BankProximity, private val invTypes: InvTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        for (sac in listOf(SarachnisLoot.egg, EggSacItems.open)) {
            onOpHeld1(sac) { open(player, it.slot) }
            onOpHeld2(sac) { mes("There are ${remaining(it.obj)} eggs in the sac.") }
            onOpBankside(sac) {
                val item = player.inv[slot] ?: return@onOpBankside
                if (item.id != sac.id || !canOpen(player, item, true)) return@onOpBankside
                val bank = player.invMap.getOrPut(invTypes[invs.bank])
                if (
                    player.deliverToBank(
                        bank,
                        objs.red_spiders_eggs,
                        remaining(item),
                        sac,
                        1,
                        paymentSlot = slot,
                    )
                )
                    player.mes("You empty the sac into your bank.")
            }
        }
        onOpHeldU(SarachnisLoot.egg, objs.knife) { open(player, it.firstSlot) }
    }

    private fun canOpen(player: Player, item: InvObj, atBank: Boolean): Boolean {
        if (!item.isType(SarachnisLoot.egg))
            return item.isType(EggSacItems.open) && remaining(item) > 0
        if (objs.knife !in player.inv) {
            player.mes("You need a knife to cut open the sac.")
            return false
        }
        if (!atBank) {
            player.mes("You need to be at a bank to cut open the sac.")
            return false
        }
        return true
    }

    private fun remaining(item: InvObj): Int =
        if (item.isType(SarachnisLoot.egg)) 100 else item.vars.coerceIn(0, 100)

    private fun open(player: Player, slot: Int) {
        val item = player.inv[slot] ?: return
        if (
            !canOpen(player, item, !item.isType(SarachnisLoot.egg) || banks.nearBank(player.coords))
        )
            return
        val eggs = remaining(item)
        // The last egg can occupy the slot of the exhausted sac.
        val free = player.inv.freeSpace()
        val count = if (eggs <= free + 1) eggs else minOf(free, eggs)
        if (count == 0 && !item.isType(SarachnisLoot.egg)) {
            player.mes("You need room in your inventory to remove the eggs.")
            return
        }
        player.invTransaction(player.inv) {
            val bag = select(player.inv)
            delete(bag, item.id, 1, slot)
            if (count < eggs) add(bag, EggSacItems.open.id, 1, eggs - count, slot)
            if (count > 0) add(bag, objs.red_spiders_eggs.id, count)
        }
    }
}
