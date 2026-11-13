package org.rsmod.content.skills.herblore.mixology

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.*
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.varp.VarpEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.interfaces.bank.BankProximity
import org.rsmod.content.skills.cooking.FoodConsumptionRepository
import org.rsmod.content.skills.herblore.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object BarrelObjs : ObjReferences() {
    val assembled = find("mm_prepot_device")
    val kit = find("mm_prepot_device_disassembled")
}

public object BarrelInv : InvReferences() {
    val contents = find("prepot_device_inv")
}

internal object BarrelSlots : InvEditor() {
    init {
        edit(BarrelInv.contents) {
            scope = InvScope.Perm
            size = 5
            stack = InvStackType.Always
            protect = false
        }
    }
}

public object BarrelVars : VarpReferences() {
    val layout =
        (0..3).map { i -> (0..4).map { find("prepot_device_loadout_${i}_slot_${it}_potion") } }
    val caps = (1..5).map { find("prepot_device_loadouts_$it") }
}

public object BarrelBits : VarBitReferences() {
    val caps = (0..3).map { i -> (0..4).map { find("prepot_device_loadout_${i}_slot_${it}_cap") } }
}

internal object BarrelSaves : VarpEditor() {
    init {
        for (save in BarrelVars.layout.flatten() + BarrelVars.caps) edit(save) { permanent = true }
    }
}

public object BarrelUi : InterfaceReferences() {
    val main = find("prepot_device")
}

public object BarrelComps : ComponentReferences() {
    val contents = find("prepot_device:device_contents")
    val inventory = find("prepot_device:inventory_contents")
    val bankContents = find("bankside:prepot_device_contents")
    val save = (0..3).map { find("bankside:prepot_device_loadout_${it}_save") }
    val load = (0..3).map { find("bankside:prepot_device_loadout_${it}_load") }
    val fill = find("bankside:prepot_device_fill")
    val bank = find("bankside:prepot_device_bankall")
}

internal object BarrelSeq : SeqReferences() {
    val drink = find("prepot_device_drink_full")
}

public class ChuggingBarrel
@Inject
constructor(
    private val invTypes: InvTypeList,
    private val types: ObjTypeList,
    private val banks: BankProximity,
    private val store: PotionStorage,
    private val potions: PotionConsumptionRepository,
    private val food: FoodConsumptionRepository,
    private val surge: SurgePotionDrinking,
) : PluginScript() {
    private val standard by lazy {
        (AntipoisonPotionDoses.all +
                HigherPoisonRemedyPotionDoses.all +
                SanfewSerumPotionDoses.all +
                AntifirePotionDoses.all +
                StatRestorePotionDoses.all +
                PrayerRegenerationPotionDoses.all +
                MenaphiteRemedyPotionDoses.all +
                StatBoostPotionDoses.all +
                DirectOnlyStatBoostPotionDoses.all +
                DirectBrewPotionDoses.all)
            .associateBy { it.potion.id }
    }
    public val fluids: List<DecantFluid> by lazy {
        DecantFluids.all.filter { fluid ->
            fluid.empty == objs.empty_vial &&
                fluid.doses.any {
                    it.id in standard ||
                        GoadingPotionDoses.all.any { d -> d.potion.id == it.id } ||
                        SurgePotionDoses.all.any { d -> d.potion.id == it.id } ||
                        BarbarianMixRecipes.all.any { r -> r.oneDose.id == it.id }
                }
        }
    }
    private val byItem by lazy { fluids.flatMap { f -> f.doses.map { it.id to f } }.toMap() }

    public fun contents(player: Player): Inventory =
        player.invMap.getOrPut(invTypes[BarrelInv.contents])

    private fun allowed(player: Player) =
        player.members && BarrelObjs.assembled in player.inv && banks.nearBank(player.coords)

    override fun ScriptContext.startup() {
        onOpHeld1(BarrelObjs.kit) { assemble(this, it.slot) }
        onOpHeld1(BarrelObjs.assembled) { drink(this) }
        onOpHeld2(BarrelObjs.assembled) { open(this) }
        onOpHeld3(BarrelObjs.assembled) {
            if (
                menu(
                    "Dismantling loses every potion stored inside.",
                    "Dismantle the barrel.",
                    "Keep the barrel.",
                ) == 1
            )
                dismantle(this, it.slot)
        }
        for (fluid in fluids) for (potion in fluid.doses) onOpHeldU(BarrelObjs.assembled, potion) {
            load(this, it.secondSlot)
        }
        onIfModalButton(BarrelComps.inventory) { e ->
            val slot = e.comsub
            if (inv[slot]?.id != e.obj?.id) return@onIfModalButton
            val count =
                when (e.op.slot) {
                    1,
                    5 -> 1
                    2,
                    6 -> 5
                    3,
                    7 -> Int.MAX_VALUE
                    4,
                    8 -> countDialog()
                    else -> 0
                }
            if (count > 0) {
                if (e.op.slot < 5) loadMany(this, slot, count)
                else fill(this, byItem[inv[slot]?.id] ?: return@onIfModalButton, count)
            }
        }
        for (component in listOf(BarrelComps.contents, BarrelComps.bankContents)) {
            onIfModalButton(component) { e ->
                val slot = e.comsub
                val bag = contents(player)
                val fluid = bag[slot]?.id?.let(byItem::get) ?: return@onIfModalButton
                when (e.op.slot) {
                    1 -> unload(this, slot, fluid.maximum)
                    2 -> unload(this, slot, 1)
                    3 -> unload(this, slot, Int.MAX_VALUE)
                    4 -> {
                        val count = countDialog()
                        if (count > 0) unload(this, slot, count)
                    }
                    5 -> fill(this, fluid, 1)
                    6 -> fill(this, fluid, 5)
                    7 -> fill(this, fluid, Int.MAX_VALUE)
                    8 -> {
                        val count = countDialog()
                        if (count > 0) fill(this, fluid, count)
                    }
                }
            }
            onIfModalDrag(component) { e ->
                reorder(
                    this,
                    e.selectedSlot ?: return@onIfModalDrag,
                    e.targetSlot ?: return@onIfModalDrag,
                )
            }
        }
        onEvent<org.rsmod.api.player.events.BankOpened> {
            if (BarrelObjs.assembled in player.inv) {
                player.startInvTransmit(contents(player))
                for (c in
                    BarrelComps.save +
                        BarrelComps.load +
                        BarrelComps.fill +
                        BarrelComps.bank) player.ifSetEvents(c, -1..0, IfEvent.Op1)
                player.ifSetEvents(
                    BarrelComps.bankContents,
                    0..4,
                    IfEvent.Op1,
                    IfEvent.Op2,
                    IfEvent.Op3,
                    IfEvent.Op4,
                    IfEvent.Op5,
                    IfEvent.Op6,
                    IfEvent.Op7,
                    IfEvent.Op8,
                    IfEvent.Depth1,
                    IfEvent.DragTarget,
                )
            }
        }
        for (i in 0..3) {
            onIfModalButton(BarrelComps.save[i]) { savePreset(this, i) }
            onIfModalButton(BarrelComps.load[i]) { loadPreset(this, i) }
        }
        onIfModalButton(BarrelComps.fill) { loadPreset(this, 0) }
        onIfModalButton(BarrelComps.bank) { emptyToBank(this) }
    }

    public fun assemble(access: ProtectedAccess, slot: Int): Boolean =
        with(access) {
            if (!player.members || inv[slot]?.id != BarrelObjs.kit.id) return false
            player
                .invTransaction(inv) {
                    val i = select(inv)
                    delete(i, BarrelObjs.kit.id, 1, slot = slot)
                    add(i, BarrelObjs.assembled.id, 1, slot = slot)
                }
                .success
        }

    public fun dismantle(access: ProtectedAccess, slot: Int): Boolean =
        with(access) {
            if (!player.members || inv[slot]?.id != BarrelObjs.assembled.id) return false
            val tx =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, BarrelObjs.assembled.id, 1, slot = slot)
                    add(i, BarrelObjs.kit.id, 1, slot = slot)
                }
            if (!tx.success) return false
            val bag = contents(player)
            for (i in bag.indices) bag[i] = null
            true
        }

    public fun open(access: ProtectedAccess): Boolean =
        with(access) {
            if (!allowed(player)) {
                mes("You can only use the chugging barrel near a bank.")
                return false
            }
            invTransmit(contents(player))
            invTransmit(inv)
            ifOpenMain(BarrelUi.main)
            ifSetEvents(
                BarrelComps.inventory,
                0..27,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
                IfEvent.Op6,
                IfEvent.Op7,
                IfEvent.Op8,
            )
            ifSetEvents(
                BarrelComps.contents,
                0..4,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
                IfEvent.Op6,
                IfEvent.Op7,
                IfEvent.Op8,
                IfEvent.Depth1,
                IfEvent.DragTarget,
            )
            true
        }

    public fun load(access: ProtectedAccess, slot: Int, maximum: Int = Int.MAX_VALUE): Int =
        with(access) {
            if (!allowed(player)) return 0
            val obj = inv[slot] ?: return 0
            val fluid = byItem[obj.id] ?: return 0
            val bag = contents(player)
            val key = fluid.doses.last().id
            val held = bag.indexOfFirst { it?.id == key }
            val dest = if (held >= 0) held else bag.indexOfFirst { it == null }
            if (dest < 0) {
                mes("The barrel can hold five different potions.")
                return 0
            }
            val stored = bag[dest]?.count ?: 0
            val input = fluid.amount(obj.id)
            val accepted = minOf(input, 100 - stored, maximum)
            if (accepted <= 0) return 0
            val remaining = input - accepted
            val replacement = if (remaining == 0) objs.empty_vial else fluid.doses[remaining - 1]
            val tx =
                player.invTransaction(inv, bag) {
                    val from = select(inv)
                    val into = select(bag)
                    delete(from, obj.id, 1, slot = slot)
                    add(from, replacement.id, 1, slot = slot)
                    add(into, key, accepted, slot = dest)
                }
            if (!tx.success) return 0
            saveCurrent(player)
            accepted
        }

    private fun loadMany(access: ProtectedAccess, slot: Int, request: Int): Int =
        with(access) {
            val id = inv[slot]?.id ?: return 0
            var loaded = 0
            repeat(minOf(request, 28)) {
                val source = inv.indexOfFirst { it?.id == id }
                if (source < 0) return loaded
                val count = load(this, source)
                if (count == 0) return loaded
                loaded += count
            }
            loaded
        }

    private fun fill(access: ProtectedAccess, fluid: DecantFluid, request: Int): Int =
        with(access) {
            var loaded = 0
            while (loaded < request) {
                val slot = inv.indexOfFirst { it?.id in fluid.doses.map { d -> d.id } }
                if (slot < 0) break
                val count = load(this, slot, request - loaded)
                if (count == 0) break
                loaded += count
            }
            loaded
        }

    public fun reorder(access: ProtectedAccess, from: Int, into: Int): Boolean =
        with(access) {
            if (!allowed(player)) return false
            val bag = contents(player)
            if (from !in bag.indices || into !in bag.indices) return false
            val old = bag[from]
            bag[from] = bag[into]
            bag[into] = old
            val first = vars[BarrelVars.layout[0][from]]
            val cap = vars[BarrelBits.caps[0][from]]
            vars[BarrelVars.layout[0][from]] = vars[BarrelVars.layout[0][into]]
            vars[BarrelBits.caps[0][from]] = vars[BarrelBits.caps[0][into]]
            vars[BarrelVars.layout[0][into]] = first
            vars[BarrelBits.caps[0][into]] = cap
            true
        }

    public fun unload(access: ProtectedAccess, slot: Int, request: Int): Int =
        with(access) {
            if (!allowed(player) || request <= 0) return 0
            val bag = contents(player)
            val obj = bag[slot] ?: return 0
            val fluid = byItem[obj.id] ?: return 0
            val total = minOf(obj.count, request)
            val full = minOf(total / fluid.maximum, inv.freeSpace())
            val partial = if (full < inv.freeSpace()) total - full * fluid.maximum else 0
            val consumed = full * fluid.maximum + partial
            if (consumed <= 0) return 0
            val tx =
                player.invTransaction(inv, bag) {
                    val from = select(bag)
                    val into = select(inv)
                    delete(from, obj.id, consumed, slot = slot)
                    if (full > 0) add(into, fluid.doses.last().id, full)
                    if (partial > 0) add(into, fluid.doses[partial - 1].id, 1)
                }
            if (!tx.success) return 0
            saveCurrent(player)
            consumed
        }

    public fun move(access: ProtectedAccess, slot: Int, direction: Int): Boolean =
        with(access) {
            if (!allowed(player) || direction !in listOf(-1, 1)) return false
            val bag = contents(player)
            val other = slot + direction
            if (slot !in bag.indices || other !in bag.indices) return false
            val previous = bag[slot]
            bag[slot] = bag[other]
            bag[other] = previous
            saveCurrent(player)
            true
        }

    public fun drink(access: ProtectedAccess): Int =
        with(access) {
            if (!allowed(player) || !potions.isReady(player)) return 0
            val bag = contents(player)
            val drinkable =
                bag.indices.mapNotNull { slot ->
                    val obj = bag[slot] ?: return@mapNotNull null
                    val fluid = byItem[obj.id] ?: return@mapNotNull null
                    val one = fluid.doses.first()
                    val requirement = standard[one.id]?.questRequirement
                    if (requirement != null && vars[requirement.varp] < requirement.minimumState)
                        return@mapNotNull null
                    if (
                        SurgePotionDoses.all.any { it.potion.id == one.id } &&
                            !surge.canConsumeStored(player)
                    )
                        return@mapNotNull null
                    if (
                        BarbarianMixRecipes.all.any { it.oneDose.id == one.id } &&
                            !food.isReady(player)
                    )
                        return@mapNotNull null
                    Triple(slot, obj, one)
                }
            if (drinkable.isEmpty()) return 0
            val tx =
                player.invTransaction(bag) {
                    val i = select(bag)
                    for ((slot, obj, _) in drinkable) delete(i, obj.id, 1, slot = slot)
                }
            if (!tx.success) return 0
            stopAction()
            anim(BarrelSeq.drink)
            for ((_, _, one) in drinkable) {
                val mix = BarbarianMixRecipes.all.firstOrNull { it.oneDose.id == one.id }
                if (SurgePotionDoses.all.any { it.potion.id == one.id })
                    surge.applyStoredEffect(this, one)
                else {
                    publish(
                        PlayerConsumableEvent.Consumed(
                            player,
                            one,
                            mix?.potion ?: one,
                            foodHealing = mix?.heal ?: 0,
                        )
                    )
                    if (mix != null) statHeal(stats.hitpoints, mix.heal, 0)
                }
            }
            if (
                drinkable.any { (_, _, one) ->
                    BarbarianMixRecipes.all.any { it.oneDose.id == one.id }
                }
            )
                food.consume(player, 3, 3)
            potions.consume(player, 3)
            mes("You drink one dose of each selected potion.")
            drinkable.size
        }

    private fun saveCurrent(player: Player) {
        for (i in 0..4) {
            val item = contents(player)[i]
            val kind = item?.id?.let(store.byItem::get)?.kind
            org.rsmod.api.player.vars.VarPlayerIntMapSetter.set(
                player,
                BarrelVars.layout[0][i],
                kind?.doseEnum ?: 0,
            )
            org.rsmod.api.player.vars.VarPlayerIntMapSetter.set(
                player,
                BarrelBits.caps[0][i],
                item?.count ?: 0,
            )
        }
    }

    public fun savePreset(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (!allowed(player) || interfaces.bank_main !in player.ui || index !in 0..3)
                return false
            saveCurrent(player)
            for (i in 0..4) {
                vars[BarrelVars.layout[index][i]] = vars[BarrelVars.layout[0][i]]
                vars[BarrelBits.caps[index][i]] = vars[BarrelBits.caps[0][i]]
            }
            true
        }

    public fun emptyToBank(access: ProtectedAccess): Int =
        with(access) {
            if (!allowed(player) || interfaces.bank_main !in player.ui) return 0
            val bag = contents(player)
            var total = 0
            for (slot in bag.indices) {
                val obj = bag[slot] ?: continue
                val fluid = byItem[obj.id] ?: continue
                val full = obj.count / fluid.maximum
                val partial = obj.count % fluid.maximum
                val additions =
                    listOfNotNull(
                        if (full > 0) fluid.doses.last().id else null,
                        if (partial > 0) fluid.doses[partial - 1].id else null,
                    )
                if (!bankFits(player, bank, additions)) break
                val tx =
                    player.invTransaction(bag, bank) {
                        val from = select(bag)
                        val into = select(bank)
                        delete(from, obj.id, obj.count, slot = slot)
                        if (full > 0) add(into, fluid.doses.last().id, full)
                        if (partial > 0) add(into, fluid.doses[partial - 1].id, 1)
                    }
                if (!tx.success) break
                total += obj.count
            }
            saveCurrent(player)
            total
        }

    private fun bankFits(
        player: Player,
        bank: Inventory,
        additions: List<Int>,
        removed: Int? = null,
    ): Boolean {
        val existing = bank.filterNotNull().map { it.id }.toMutableSet()
        if (removed != null && bank.count { it?.id == removed } == 1) existing.remove(removed)
        val occupied = bank.occupiedSpace() - if (removed != null) 1 else 0
        val needed = additions.distinct().count { it !in existing }
        return occupied + needed <= player.vars[varbits.bank_capacity]
    }

    public fun loadPreset(access: ProtectedAccess, index: Int): Int =
        with(access) {
            if (!allowed(player) || interfaces.bank_main !in player.ui || index !in 0..3) return 0
            val profile =
                (0..4).map {
                    vars[BarrelVars.layout[index][it]] to
                        vars[BarrelBits.caps[index][it]].coerceIn(0, 100)
                }
            if (index != 0 && contents(player).isNotEmpty()) {
                emptyToBank(this)
                if (contents(player).isNotEmpty()) return 0
            }
            var loaded = 0
            val bag = contents(player)
            for ((target, cap) in profile.withIndex()) {
                val (enum, limit) = cap
                if (enum == 0 || limit == 0) continue
                val kind = store.kinds.firstOrNull { it.doseEnum == enum } ?: continue
                val key = kind.raw.getValue(kind.maximum)
                if (key !in byItem) continue
                val current = bag[target]?.takeIf { it.id == key }?.count ?: 0
                var need = (limit - current).coerceAtLeast(0)
                if (need == 0) continue
                if (vars[PotionStoreBits.currentTab] == 15) {
                    if (vars[MixBits.potionStorage] == 0) continue
                    val count = minOf(need, vars[kind.counter])
                    if (
                        count > 0 &&
                            player.invAdd(bag, types.getValue(key), count, slot = target).success
                    ) {
                        vars[kind.counter] -= count
                        loaded += count
                    }
                    continue
                }
                for (slot in bank.indices) {
                    while (need > 0) {
                        val bottle = bank[slot] ?: break
                        val input = store.byItem[bottle.id] ?: break
                        if (input.kind != kind || types[bottle].isCert) break
                        val taken = minOf(input.doses, need)
                        val remainder = input.doses - taken
                        val replacement =
                            if (remainder == 0) objs.empty_vial.id else kind.raw.getValue(remainder)
                        if (
                            !bankFits(
                                player,
                                bank,
                                listOf(replacement),
                                if (bottle.count == 1) bottle.id else null,
                            )
                        )
                            break
                        val tx =
                            player.invTransaction(bank, bag) {
                                val from = select(bank)
                                val into = select(bag)
                                delete(from, bottle.id, 1, slot = slot)
                                add(from, replacement, 1)
                                add(into, key, taken, slot = target)
                            }
                        if (tx.success) {
                            need -= taken
                            loaded += taken
                        } else break
                    }
                }
            }
            for (i in 0..4) {
                vars[BarrelVars.layout[0][i]] = profile[i].first
                vars[BarrelBits.caps[0][i]] = profile[i].second
            }
            loaded
        }
}
