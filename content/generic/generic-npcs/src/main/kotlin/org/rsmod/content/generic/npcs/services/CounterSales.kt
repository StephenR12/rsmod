package org.rsmod.content.generic.npcs.services

import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.feet
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onMissingNpcService
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private data class CounterItem(val symbol: String, val quantity: Int, val price: Int)

private data class Counter(
    val name: String,
    val members: Boolean,
    val currency: String,
    val owners: List<Pair<String, Int>>,
    val stock: List<CounterItem>,
)

private val counters =
    requireNotNull(Counter::class.java.getResourceAsStream("counter-sales.tsv"))
        .bufferedReader()
        .useLines { lines ->
            lines.filterNot { it.startsWith('#') }.map { line ->
                val fields = line.split('\t')
                val owners = fields[3].split(',').map {
                    val (name, slot) = it.split(':')
                    name to slot.toInt()
                }
                val stock = fields[4].split(',').map {
                    val (name, quantity, price) = it.split(':')
                    CounterItem(name, quantity.toInt(), price.toInt())
                }
                Counter(fields[0], fields[1].toBooleanStrict(), fields[2], owners, stock)
            }.toList()
        }

internal object CounterObjects : ObjReferences() {
    val vyreShoes = find("vyrelord_shoes")
    val vyreTorso = (0..12).map { find(if (it == 0) "vyrelord_torso" else "vyrelord_torso_$it") }
    val vyreLegs = (0..12).map { find(if (it == 0) "vyrelord_legs" else "vyrelord_legs_$it") }
    val byName = counters.flatMap { shop -> shop.stock.map { it.symbol } + shop.currency }
        .distinct().associateWith { find(it) }
}

internal object CounterQuests : VarBitReferences() {
    val sinsOfTheFather = find("myq5")
}

internal object CounterOwners : NpcReferences() {
    val byName = counters.flatMap { it.owners }.map { it.first }.distinct().associateWith { find(it) }
}

public class CounterSales @Inject constructor(
    private val npcs: NpcTypeList,
    private val objects: ObjTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onGameStartup {
            for (counter in counters) {
                for ((owner, slot) in counter.owners) {
                    onMissingNpcService(npcs[CounterOwners.byName.getValue(owner)], slot) {
                        buy(counter)
                    }
                }
            }
        }
    }

    private suspend fun ProtectedAccess.buy(counter: Counter) {
        if (counter.members && !player.members) {
            mes("You need to be on a members' world to use this shop.")
            return
        }
        if (counter.name == "The Crypt" && !canBuyBloodPints()) {
            mes("You need to complete Sins of the Father and wear full vyre noble clothing.")
            return
        }
        val currency = objects[CounterObjects.byName.getValue(counter.currency)]
        val options = counter.stock.map { item ->
            val name = objects[CounterObjects.byName.getValue(item.symbol)].name
            val quantity = if (item.quantity == 1) "" else " (x${item.quantity})"
            "$name$quantity - ${item.price} ${currency.name.lowercase()}"
        }
        val item = counter.stock.getOrNull(menu(counter.name, true, options)) ?: return
        val product = objects[CounterObjects.byName.getValue(item.symbol)]
        if (invTotal(inv, currency) < item.price) {
            mes("You need ${item.price} ${currency.name.lowercase()} to buy that.")
            return
        }
        val result = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag, currency.id, item.price)
            add(bag, product.id, item.quantity)
        }
        if (!result.success) mes("You need more inventory space.")
    }
    private fun ProtectedAccess.canBuyBloodPints(): Boolean =
        vars[CounterQuests.sinsOfTheFather] >= 138 &&
            player.feet?.id == CounterObjects.vyreShoes.id &&
            CounterObjects.vyreTorso.any { it.id == player.torso?.id } &&
            CounterObjects.vyreLegs.any { it.id == player.legs?.id }

}
