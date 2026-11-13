package org.rsmod.content.other.special.weapons.magic

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.hat
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.other.special.weapons.ranged.BlowpipeHelms
import org.rsmod.content.skills.magic.spell.attacks.ancient.AncientNpcPoisonRepository
import org.rsmod.content.skills.magic.spell.utility.standard.PoisonProtectionRepository
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TridentNpcs : NpcReferences() {
    val lieve = find("slayer_kraken_cove_lieve")
}

internal object TridentBits : VarBitReferences() {
    val target = find("charges_trident_of_the_seas_quantity")
    val enhancedTarget = find("charges_trident_of_the_seas_e_quantity")
    val tentacles = find("kraken_cove_tentaclesstored")
}

class TridentCharging
@Inject
constructor(
    private val banking: BankInvScript,
    private val types: ObjTypeList,
    private val random: GameRandom,
    private val npcVenom: AncientNpcPoisonRepository,
    private val playerVenom: PoisonProtectionRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(TridentNpcs.lieve) {
            mes(
                "Lieve has ${vars[TridentBits.tentacles]} of your tentacles. Give her ten and an uncharged trident to enhance it."
            )
        }
        for (tentacle in listOf(TridentObjs.tentacle, TridentObjs.notedTentacle)) onOpNpcU(
            TridentNpcs.lieve,
            tentacle,
        ) {
            val count = minOf(10 - vars[TridentBits.tentacles], inv.count(types[tentacle]))
            if (
                count > 0 &&
                    choice2("Give Lieve $count tentacles to keep.", true, "Cancel.", false) &&
                    invDel(inv, tentacle, count).success
            )
                vars[TridentBits.tentacles] += count
        }
        for (t in listOf(TridentObjs.seas, TridentObjs.swamp)) onOpNpcU(
            TridentNpcs.lieve,
            t.empty,
        ) {
            enhance(this, it.invSlot)
        }
        for (trident in TridentObjs.variants) {
            val variants =
                listOf(trident.empty, trident.charged) +
                    (if (trident == TridentObjs.seas) listOf(TridentObjs.full) else emptyList())
            for (item in variants) {
                onOpHeld3(item) {
                    val held = inv[it.slot] ?: return@onOpHeld3
                    mes("Your trident has ${trident.charges(held)} charges.")
                }
                onOpWorn2(item) {
                    val held = player.righthand ?: return@onOpWorn2
                    mes("Your trident has ${trident.charges(held)} charges.")
                }
                for ((ingredient, _) in ingredients(trident)) onOpHeldU(item, ingredient) {
                    val held = inv[it.firstSlot] ?: return@onOpHeldU
                    val limit = possible(this, trident, held, inv)
                    if (limit <= 0) {
                        mes(
                            "You need the required Magic level and the exact runes and materials in your inventory."
                        )
                        return@onOpHeldU
                    }
                    val count =
                        if (limit == 1) 1
                        else
                            countDialog("How many charges would you like to add?")
                                .coerceIn(0, limit)
                    if (inv[it.firstSlot] == held) charge(this, it.firstSlot, count)
                }
                if (item != trident.empty) {
                    onOpHeld4(item) {
                        val held = inv[it.slot]
                        if (
                            choice2("Uncharge the trident.", true, "Cancel.", false) &&
                                inv[it.slot] == held
                        )
                            uncharge(this, it.slot)
                    }
                } else if (trident.toxic) {
                    onOpHeld4(item) { dismantle(this, it.slot) }
                } else {
                    onOpHeldU(item, TridentObjs.fang) {
                        attachFang(this, it.firstSlot, it.secondSlot)
                    }
                }
                onProtectedEvent<BankedObjEvents.ConfigureCharges>(item.id) {
                    val target =
                        if (trident.extended) TridentBits.enhancedTarget else TridentBits.target
                    vars[target] =
                        countDialog("How many charges should this trident hold on withdrawal?")
                            .coerceIn(0, trident.cap)
                }
                onProtectedEvent<BankedObjEvents.Autocharge>(item.id) {
                    val target =
                        vars[
                            if (trident.extended) TridentBits.enhancedTarget
                            else TridentBits.target]
                    if (inv[it.slot] == it.obj)
                        charge(
                            this,
                            it.slot,
                            (target - trident.charges(it.obj)).coerceAtLeast(0),
                            true,
                        )
                }
            }
            if (trident.toxic)
                onEvent<NpcHitEvents.WeaponImpact>(trident.charged.id) {
                    if (
                        hit.type == HitType.Magic &&
                            (source.hat.isAnyType(
                                BlowpipeHelms.green,
                                BlowpipeHelms.cyan,
                                BlowpipeHelms.red,
                            ) || random.randomBoolean(4))
                    )
                        npcVenom.envenom(npc)
                }
        }
        onEvent<PlayerHitEvents.Impact> {
            if (
                damage > 0 &&
                    hit.type == HitType.Magic &&
                    hit.isFromPlayer &&
                    (hit.isRighthandObj(TridentObjs.swamp.charged) ||
                        hit.isRighthandObj(TridentObjs.enhancedSwamp.charged)) &&
                    !player.hat.isAnyType(
                        BlowpipeHelms.green,
                        BlowpipeHelms.cyan,
                        BlowpipeHelms.red,
                    ) &&
                    random.randomBoolean(4)
            )
                playerVenom.inflictVenom(player)
        }
    }

    private fun ingredients(trident: Trident): List<Pair<ObjType, Int>> =
        listOf(
            objs.death_rune to 1,
            objs.chaos_rune to 1,
            objs.fire_rune to 5,
            if (trident.toxic) objs.zulrahs_scales to 1 else objs.coins to 10,
        )

    private fun possible(access: ProtectedAccess, t: Trident, item: InvObj, from: Inventory): Int {
        if (access.player.stat(stats.magic) < t.level) return 0
        return minOf(
            t.cap - t.charges(item),
            ingredients(t).minOf { (material, cost) -> from.count(types[material]) / cost },
        )
    }

    fun charge(access: ProtectedAccess, slot: Int, requested: Int, banked: Boolean = false): Int =
        with(access) {
            val held = inv[slot] ?: return 0
            val t = TridentObjs.type(held.id) ?: return 0
            val from = if (banked) bank else inv
            val count = minOf(requested, possible(this, t, held, from))
            if (count <= 0) return 0
            if (banked) {
                if (banking.takeBankChargeMaterials(this, ingredients(t), count) != count) return 0
            } else {
                val tx =
                    player.invTransaction(from) {
                        val source = select(from)
                        for ((material, cost) in ingredients(t)) delete(
                            source,
                            material.id,
                            cost * count,
                        )
                    }
                if (!tx.success) return 0
            }
            inv[slot] = t.item(t.charges(held) + count)
            mes("You add $count charges to the trident.")
            count
        }

    fun uncharge(access: ProtectedAccess, slot: Int): Boolean =
        with(access) {
            val held = inv[slot] ?: return false
            val t = TridentObjs.type(held.id) ?: return false
            val charges = t.charges(held)
            if (charges == 0) return false
            val tx =
                player.invTransaction(inv) {
                    val dest = select(inv)
                    for ((material, cost) in ingredients(t)) if (material != objs.coins)
                        add(dest, material.id, cost * charges)
                }
            if (!tx.success) {
                mes("Make room for the materials.")
                return false
            }
            inv[slot] = InvObj(t.empty)
            true
        }

    fun enhance(access: ProtectedAccess, slot: Int): Boolean =
        with(access) {
            val held = inv[slot] ?: return false
            val t = TridentObjs.type(held.id) ?: return false
            if (t.extended || held.id != t.empty.id) return false
            val needed = (10 - vars[TridentBits.tentacles]).coerceIn(0, 10)
            val physical = minOf(needed, inv.count(types[TridentObjs.tentacle]))
            val notes = needed - physical
            if (needed > 0) {
                val tx =
                    player.invTransaction(inv) {
                        val source = select(inv)
                        if (physical > 0) delete(source, TridentObjs.tentacle.id, physical)
                        if (notes > 0) delete(source, TridentObjs.notedTentacle.id, notes)
                    }
                if (!tx.success) {
                    mes("Lieve needs ten kraken tentacles to enhance your trident.")
                    return false
                }
            }
            inv[slot] =
                InvObj(if (t.toxic) TridentObjs.enhancedSwamp.empty else TridentObjs.enhanced.empty)
            vars[TridentBits.tentacles] = 0
            mes("Lieve enhances your trident. It can now hold 20,000 charges.")
            true
        }

    fun attachFang(access: ProtectedAccess, slot: Int, fang: Int): Boolean =
        with(access) {
            val held = inv[slot] ?: return false
            val t = TridentObjs.type(held.id) ?: return false
            if (t.toxic || held.id != t.empty.id || !inv[fang].isType(TridentObjs.fang))
                return false
            if (player.stat(stats.crafting) < 59 || objs.chisel !in inv) {
                mes("You need a chisel and level 59 Crafting.")
                return false
            }
            inv[fang] = null
            inv[slot] =
                InvObj(if (t.extended) TridentObjs.enhancedSwamp.empty else TridentObjs.swamp.empty)
            true
        }

    fun dismantle(access: ProtectedAccess, slot: Int): Boolean =
        with(access) {
            val held = inv[slot] ?: return false
            val t = TridentObjs.type(held.id) ?: return false
            if (!t.toxic || held.id != t.empty.id || invAdd(inv, TridentObjs.fang).failure)
                return false
            inv[slot] =
                InvObj(if (t.extended) TridentObjs.enhanced.empty else TridentObjs.seas.empty)
            true
        }
}
