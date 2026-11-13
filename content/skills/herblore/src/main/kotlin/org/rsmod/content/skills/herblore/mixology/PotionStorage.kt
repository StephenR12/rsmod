package org.rsmod.content.skills.herblore.mixology

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.BankPotionStoreDeposit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.content.interfaces.bank.QuantityMode
import org.rsmod.content.interfaces.bank.lastQtyInput
import org.rsmod.content.interfaces.bank.leftClickQtyMode
import org.rsmod.content.interfaces.bank.withdrawCert
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.type.varp.VarpType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public data class StoredPotionKind(
    val doseEnum: Int,
    val counter: VarpType,
    val preferred: VarBitType?,
    val raw: Map<Int, Int>,
    val noted: Map<Int, Int>,
) {
    val maximum: Int
        get() = raw.keys.max()
}

public data class StoredPotionInput(val kind: StoredPotionKind, val doses: Int)

public class PotionStorage
@Inject
constructor(private val enums: EnumTypeList, private val types: ObjTypeList) : PluginScript() {
    public val kinds: List<StoredPotionKind> by lazy {
        PotionStoreSpecs.all.mapIndexed { i, spec ->
            val values = enums.getValue(spec.doseEnum).primitiveMap
            val raw =
                values.entries
                    .filter { (it.key as Int) in 1..6 && (it.value as Int) > 0 }
                    .associate { it.key as Int to it.value as Int }
            val notes =
                values.entries
                    .filter { (it.key as Int) in -16..-11 && (it.value as Int) > 0 }
                    .associate { -(it.key as Int) - 10 to it.value as Int }
            StoredPotionKind(
                spec.doseEnum,
                PotionStoreVars.counters[i],
                PotionStoreBits.preferred[i],
                raw,
                notes,
            )
        } +
            StoredPotionKind(
                0,
                PotionStoreVars.vials,
                null,
                mapOf(1 to objs.empty_vial.id),
                mapOf(1 to types[objs.empty_vial].certlink),
            )
    }
    public val byItem: Map<Int, StoredPotionInput> by lazy {
        kinds
            .flatMap { kind ->
                (kind.raw + kind.noted.mapKeys { it.key + 10 }).map { (dose, id) ->
                    id to StoredPotionInput(kind, if (dose > 10) dose - 10 else dose)
                }
            }
            .toMap()
    }
    private val nativeGroups: List<List<StoredPotionKind?>> by lazy {
        listOf(4826, 4829).map { id ->
            enums
                .getValue(id)
                .primitiveMap
                .entries
                .sortedBy { it.key as Int }
                .map { entry -> kinds.firstOrNull { it.doseEnum == entry.value as Int } }
        }
    }
    public val nativeRows: List<StoredPotionKind?>
        get() = nativeGroups.flatten()

    /** CS6080 reserves five children per family, then five per visible section header. */
    public fun vialControls(player: Player): Set<Int> {
        val favouriteIds =
            PotionStoreVars.favourites.map { player.vars[it] }.filter { it >= 0 }.toSet()
        val favourites = nativeRows.count { it != null && it.raw[1] in favouriteIds }
        val counts =
            listOf(favourites) +
                nativeGroups.map { group ->
                    group.count {
                        it != null && player.vars[it.counter] > 0 && it.raw[1] !in favouriteIds
                    }
                }
        var cursor = nativeRows.size * 5
        val controls = mutableSetOf<Int>()
        for (count in counts) if (count > 0) {
            if (count % 2 == 1) cursor++
            controls += cursor + 4
            cursor += 5
        }
        return controls
    }

    override fun ScriptContext.startup() {
        onEvent<BankPotionStoreDeposit> {
            if (
                player.vars[MixBits.potionStorage] == 0 ||
                    player.vars[PotionStoreBits.currentTab] != 15 &&
                        player.vars[PotionStoreBits.always] == 0
            )
                return@onEvent
            val item = inventory[slot] ?: return@onEvent
            if (item.id !in byItem) return@onEvent
            handled = true
            deposited = deposit(player, inventory, slot, count)
        }
        onIfModalButton(PotionStoreComps.toggle) {
            if (vars[MixBits.potionStorage] == 0) return@onIfModalButton
            if (vars[PotionStoreBits.currentTab] == 15)
                vars[PotionStoreBits.currentTab] = vars[PotionStoreBits.previousTab].coerceIn(0, 9)
            else {
                vars[PotionStoreBits.previousTab] = vars[PotionStoreBits.currentTab].coerceIn(0, 9)
                vars[PotionStoreBits.currentTab] = 15
            }
            events(this)
        }
        onEvent<org.rsmod.api.player.events.BankOpened> {
            if (player.vars[MixBits.potionStorage] != 0) {
                for (favourite in PotionStoreVars.favourites) if (favourite !in player.vars)
                    VarPlayerIntMapSetter.set(player, favourite, -1)
                player.ifSetEvents(PotionStoreComps.toggle, -1..0, IfEvent.Op1)
                player.ifSetEvents(
                    PotionStoreComps.items,
                    0..32767,
                    IfEvent.Op1,
                    IfEvent.Op2,
                    IfEvent.Op3,
                    IfEvent.Op4,
                    IfEvent.Op5,
                    IfEvent.Op6,
                    IfEvent.Op7,
                    IfEvent.Op8,
                    IfEvent.Op9,
                    IfEvent.Op10,
                )
            }
        }
        onIfModalButton(PotionStoreComps.items) { event ->
            if (vars[MixBits.potionStorage] == 0 || vars[PotionStoreBits.currentTab] != 15)
                return@onIfModalButton
            val vial = event.comsub in vialControls(player)
            val kind =
                if (vial) kinds.last()
                else nativeRows.getOrNull(event.comsub / 5) ?: return@onIfModalButton
            val part = if (vial) 0 else event.comsub % 5
            if (part == 1 && event.op.slot in 6..9) {
                setDose(player, kind, event.op.slot - 5)
                return@onIfModalButton
            }
            if (part == 4 && event.op.slot == 1) {
                favourite(player, kind)
                return@onIfModalButton
            }
            if (part != 0) return@onIfModalButton
            when (event.op.slot) {
                else -> {
                    val request =
                        when (event.op.slot) {
                            1 ->
                                when (leftClickQtyMode) {
                                    QuantityMode.One -> 1
                                    QuantityMode.Five -> 5
                                    QuantityMode.Ten -> 10
                                    QuantityMode.X -> lastQtyInput
                                    QuantityMode.All -> Int.MAX_VALUE
                                }
                            2 -> 1
                            3 -> 5
                            4 -> 10
                            5 -> lastQtyInput
                            6 -> {
                                val count = countDialog()
                                if (count > 0) lastQtyInput = count
                                count
                            }
                            7 -> Int.MAX_VALUE
                            8 ->
                                (player.vars[kind.counter] / preferred(player, kind) - 1)
                                    .coerceAtLeast(0)
                            else -> 0
                        }
                    if (request > 0) withdraw(this, kind, request, withdrawCert)
                }
            }
        }
    }

    private fun events(access: ProtectedAccess) {
        with(access) {
            ifSetEvents(
                PotionStoreComps.items,
                0..32767,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
                IfEvent.Op6,
                IfEvent.Op7,
                IfEvent.Op8,
                IfEvent.Op9,
                IfEvent.Op10,
            )
        }
    }

    public fun preferred(player: Player, kind: StoredPotionKind): Int =
        kind.preferred?.let { (player.vars[it] + 1).coerceIn(1, kind.maximum) } ?: 1

    public fun setDose(player: Player, kind: StoredPotionKind, doses: Int) {
        if (doses in 1..kind.maximum)
            kind.preferred?.let { VarPlayerIntMapSetter.set(player, it, doses - 1) }
    }

    public fun deposit(player: Player, from: Inventory, slot: Int, request: Int): Int {
        if (!player.members || player.vars[MixBits.potionStorage] == 0 || request <= 0) return 0
        val item = from[slot] ?: return 0
        val input = byItem[item.id] ?: return 0
        val current = player.vars[input.kind.counter]
        val bank = player.invMap[invs.bank]
        if (
            current == 0 &&
                bank != null &&
                bank.occupiedSpace() >= player.vars[varbits.bank_capacity]
        ) {
            player.mes("Your bank is full. Make room before storing a new potion type.")
            return 0
        }
        val count =
            minOf(request, item.count, (Int.MAX_VALUE.toLong() - current).div(input.doses).toInt())
        if (count <= 0) return 0
        val tx = player.invTransaction(from) { delete(select(from), item.id, count, slot = slot) }
        if (!tx.success) return 0
        VarPlayerIntMapSetter.set(player, input.kind.counter, current + count * input.doses)
        if (current == 0) setDose(player, input.kind, input.doses)
        return count
    }

    public fun withdraw(
        access: ProtectedAccess,
        kind: StoredPotionKind,
        request: Int,
        noted: Boolean = false,
    ): Int =
        with(access) {
            if (
                vars[MixBits.potionStorage] == 0 ||
                    request <= 0 ||
                    interfaces.bank_main !in player.ui
            )
                return 0
            val total = vars[kind.counter]
            if (total <= 0) return 0
            val dose = preferred(player, kind)
            val raw = types.getValue(kind.raw.getValue(dose))
            if (raw.members && !player.members) return 0
            val chosen = if (noted && kind.noted.containsKey(dose)) kind.noted else kind.raw
            val capacity = if (noted || raw.stackable) request else minOf(request, inv.freeSpace())
            if (capacity <= 0) return 0
            val full = minOf(capacity, total / dose)
            val remainder = if (full < capacity) total - full * dose else 0
            val partial = if (remainder in 1 until dose) remainder else 0
            val bottles = full + if (partial > 0) 1 else 0
            if (bottles <= 0) return 0
            val consumed = full * dose + partial
            val tx =
                player.invTransaction(inv) {
                    val i = select(inv)
                    if (full > 0) add(i, chosen.getValue(dose), full)
                    if (partial > 0) add(i, chosen.getValue(partial), 1)
                }
            if (!tx.success) return 0
            vars[kind.counter] = total - consumed
            bottles
        }

    private fun favourite(player: Player, kind: StoredPotionKind) {
        val existing =
            PotionStoreVars.favourites.firstOrNull { player.vars[it] == kind.raw.getValue(1) }
        if (existing != null) VarPlayerIntMapSetter.set(player, existing, -1)
        else
            PotionStoreVars.favourites
                .firstOrNull { player.vars[it] == -1 }
                ?.let { VarPlayerIntMapSetter.set(player, it, kind.raw.getValue(1)) }
    }
}
