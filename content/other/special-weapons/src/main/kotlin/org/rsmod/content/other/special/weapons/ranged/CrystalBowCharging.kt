package org.rsmod.content.other.special.weapons.ranged

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CrystalBowCharging @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (bow in CrystalBowItems.finite) {
            onOpHeldU(bow, objs.crystal_shard) { addShards(this, it.firstSlot, it.secondSlot) }
            onProtectedEvent<BankedObjEvents.ConfigureCharges>(bow.id) {
                val target =
                    countDialog("How many charges should the bow hold on withdrawal?")
                        .coerceIn(0, 20000)
                if (CrystalBowItems.regular(it.obj)) player.crystalTarget = target
                else player.enhancedTarget = target
            }
            onProtectedEvent<BankedObjEvents.Autocharge>(bow.id) { autocharge(this, it) }
        }
        for (bow in listOf(objs.crystal_bow, CrystalBowItems.newBow, objs.bow_of_faerdhinen)) {
            onOpHeld3(bow) { checkBow(this, it.inventory[it.slot]) }
            onOpWorn2(bow) { checkBow(this, player.righthand) }
        }
        for (bow in CrystalBowItems.infinite) onOpHeld4(bow) { revert(this, it.slot) }
        for (bow in
            listOf(
                objs.crystal_bow,
                CrystalBowItems.newBow,
                CrystalBowItems.inactive,
                objs.bow_of_faerdhinen,
            )) {
            onOpHeld4(bow) { revert(this, it.slot) }
        }
    }

    private fun checkBow(access: ProtectedAccess, bow: InvObj?) {
        if (bow == null) return
        access.mes("Your bow has ${CrystalBowItems.charges(bow)} charges remaining.")
        if (bow.isType(objs.bow_of_faerdhinen))
            access.mes(
                "The bow is ${(CrystalBowItems.usedCharges(bow)/2000).coerceAtMost(100)}% corrupted."
            )
    }

    suspend fun addShards(access: ProtectedAccess, bowSlot: Int, shardSlot: Int) =
        with(access) {
            val bow = inv[bowSlot] ?: return
            val shards = inv[shardSlot]
            if (!shards.isType(objs.crystal_shard)) return
            val limit = minOf(shards.count, (20000 - CrystalBowItems.charges(bow)) / 100)
            if (limit <= 0) {
                mes("Your bow cannot hold another shard.")
                return
            }
            val amount =
                if (limit == 1) 1
                else countDialog("How many shards would you like to add?").coerceIn(0, limit)
            if (amount == 0 || inv[bowSlot] != bow || inv[shardSlot] != shards) return
            if (invDel(inv, objs.crystal_shard, amount, shardSlot).failure) return
            inv[bowSlot] =
                CrystalBowItems.withCharges(
                    bow,
                    CrystalBowItems.charges(bow) + 100 * amount,
                    amount,
                )
            mes("You add ${amount*100} charges to your bow.")
        }

    fun autocharge(access: ProtectedAccess, event: BankedObjEvents.Autocharge) {
        val bow = access.inv[event.slot] ?: return
        if (bow != event.obj) return
        val current = CrystalBowItems.charges(bow)
        val target =
            if (CrystalBowItems.regular(bow)) access.player.crystalTarget
            else access.player.enhancedTarget
        val needed =
            minOf((20000 - current) / 100, ((target - current + 99) / 100).coerceAtLeast(0))
        if (needed <= 0) return
        val used = event.takeMaterial(objs.crystal_shard, needed)
        if (used > 0)
            access.inv[event.slot] = CrystalBowItems.withCharges(bow, current + used * 100, used)
        if (used < needed)
            access.mes("There aren't enough crystal shards in your bank to fully charge the bow.")
    }

    suspend fun revert(access: ProtectedAccess, slot: Int) =
        with(access) {
            val bow = inv[slot] ?: return
            val regular = CrystalBowItems.regular(bow)
            if (
                !choice2(
                    "Continue.",
                    true,
                    "Cancel.",
                    false,
                    title =
                        if (regular) "Revert the bow to a seed and lose any charges?"
                        else "Uncharge the bow and lose its charges?",
                )
            )
                return
            if (inv[slot] != bow) return
            inv[slot] =
                if (regular) InvObj(CrystalBowItems.seed)
                else InvObj(CrystalBowItems.faerdhinenInactive)
        }
}

internal object CrystalBowBits : VarBitReferences() {
    val weak = find("charges_crystal_weak_quantity")
    val faerdhinen = find("charges_bow_of_faerdhinen_quantity")
    val chants = find("roving_ilfeen_chantcount")
}

private var Player.crystalTarget by intVarBit(CrystalBowBits.weak)
private var Player.enhancedTarget by intVarBit(CrystalBowBits.faerdhinen)
