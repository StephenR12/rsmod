package org.rsmod.content.other.special.weapons.magic

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.*
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PoweredStaffCharging
@Inject
constructor(
    private val banking: BankInvScript,
    private val types: ObjTypeList,
    private val random: GameRandom,
    private val players: PlayerList,
    private val chargeManager: ObjChargeManager,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (staff in OtherStaves.all) {
            for (item in listOf(staff.empty, staff.active)) {
                for ((rune, _) in staff.materials) onOpHeldU(item, rune) {
                    prompt(this, it.firstSlot)
                }
                onProtectedEvent<BankedObjEvents.ConfigureCharges>(item.id) {
                    vars[staff.target] =
                        countDialog("How many charges should the staff hold on withdrawal?")
                            .coerceIn(0, 20000)
                }
                onProtectedEvent<BankedObjEvents.Autocharge>(item.id) {
                    if (inv[it.slot] == it.obj)
                        charge(this, it.slot, vars[staff.target] - staff.charges(it.obj), true)
                }
            }
            onOpHeld3(staff.empty) { prompt(this, it.slot) }
            onOpHeld3(staff.active) {
                mes("Your staff has ${staff.charges(it.inventory[it.slot]!!)} charges.")
            }
            onOpWorn2(staff.active) {
                player.righthand?.let { held ->
                    mes("Your staff has ${staff.charges(held)} charges.")
                }
            }
            onOpHeld4(staff.active) { prompt(this, it.slot) }
            onOpHeld5(staff.active) {
                val previous = inv[it.slot]
                if (
                    choice2("Uncharge the staff.", true, "Cancel.", false) &&
                        inv[it.slot] == previous
                )
                    uncharge(this, it.slot)
            }
            if (staff != OtherStaves.warped)
                onEvent<NpcHitEvents.WeaponImpact>(staff.active.id) {
                    if (hit.type == HitType.Magic && hit.secondaryObj == null)
                        leech(source, npc, hit.damage, staff)
                }
        }
        onEvent<NpcKillEvents.Credited> {
            val landed = hit ?: return@onEvent
            if (landed.type != HitType.Magic || landed.secondaryObj != null) return@onEvent
            val staff = landed.righthandObj?.let(OtherStaves::type) ?: return@onEvent
            if (staff != OtherStaves.warped) leech(player, npc, landed.damage, staff)
        }
        onEvent<PlayerHitEvents.Impact> {
            if (
                damage <= 0 ||
                    !hit.isFromPlayer ||
                    hit.type != HitType.Magic ||
                    hit.secondaryObj != null
            )
                return@onEvent
            val staff = hit.righthandObj?.let(OtherStaves::type) ?: return@onEvent
            if (staff == OtherStaves.warped) return@onEvent
            hit.resolvePlayerSource(players)?.let { leech(it, player, damage, staff) }
        }
        for (item in listOf(objs.tumekens_shadow, objs.tumekens_shadow_uncharged)) {
            onProtectedEvent<BankedObjEvents.ConfigureCharges>(item.id) {
                vars[StaffBits.shadow] =
                    countDialog("How many charges should the shadow hold on withdrawal?")
                        .coerceIn(0, 20000)
            }
            onProtectedEvent<BankedObjEvents.Autocharge>(item.id) {
                if (inv[it.slot] != it.obj) return@onProtectedEvent
                val current = chargeManager.getCharges(it.obj, varobjs.tumeken_charges)
                val materials = listOf(objs.chaos_rune to 5, objs.soul_rune to 2)
                val added =
                    banking.takeBankChargeMaterials(
                        this,
                        materials,
                        vars[StaffBits.shadow] - current,
                    )
                if (added > 0)
                    chargeManager.addCharges(inv, it.slot, added, varobjs.tumeken_charges, 20000)
            }
        }
    }

    private fun leech(source: Player, target: PathingEntity, damage: Int, staff: Staff) {
        if (damage < 2 || !random.randomBoolean(6)) return
        source.statHeal(stats.hitpoints, damage / 2, 0)
        target.spotanim(
            if (staff == OtherStaves.holy) StaffSpots.holyHeal else StaffSpots.sangHeal,
            height = 96,
        )
    }

    private suspend fun prompt(access: ProtectedAccess, slot: Int): Unit =
        with(access) {
            val item = inv[slot] ?: return
            val staff = OtherStaves.type(item.id) ?: return
            val limit = minOf(20000 - staff.charges(item), available(inv, staff.materials))
            if (limit <= 0) {
                mes("You need room for charges and the required runes in your inventory.")
                return
            }
            val count =
                if (limit == 1) 1
                else countDialog("How many charges would you like to add?").coerceIn(0, limit)
            if (inv[slot] == item) charge(this, slot, count)
        }

    private fun available(inv: Inventory, materials: List<Pair<ObjType, Int>>) =
        materials.minOf { (rune, quantity) -> inv.count(types[rune]) / quantity }

    private fun consume(
        access: ProtectedAccess,
        from: Inventory,
        materials: List<Pair<ObjType, Int>>,
        maximum: Int,
    ): Int =
        with(access) {
            val count = minOf(maximum, available(from, materials))
            if (count <= 0) return 0
            val tx =
                player.invTransaction(from) {
                    val inventory = select(from)
                    for ((rune, quantity) in materials) delete(inventory, rune.id, count * quantity)
                }
            if (tx.success) count else 0
        }

    fun charge(access: ProtectedAccess, slot: Int, quantity: Int, banked: Boolean = false): Int =
        with(access) {
            val item = inv[slot] ?: return 0
            val staff = OtherStaves.type(item.id) ?: return 0
            val current = staff.charges(item)
            val maximum = minOf(20000 - current, quantity)
            val added =
                if (banked) banking.takeBankChargeMaterials(this, staff.materials, maximum)
                else consume(this, inv, staff.materials, maximum)
            if (added > 0) inv[slot] = staff.item(current + added)
            added
        }

    fun uncharge(access: ProtectedAccess, slot: Int): Boolean =
        with(access) {
            val item = inv[slot] ?: return false
            val staff = OtherStaves.type(item.id) ?: return false
            val count = staff.charges(item)
            if (count == 0) return false
            val tx =
                player.invTransaction(inv) {
                    val inventory = select(inv)
                    for ((rune, cost) in staff.materials) add(inventory, rune.id, cost * count)
                }
            if (!tx.success) {
                mes("Make room for the recovered runes.")
                return false
            }
            inv[slot] = staff.item(0)
            true
        }
}
