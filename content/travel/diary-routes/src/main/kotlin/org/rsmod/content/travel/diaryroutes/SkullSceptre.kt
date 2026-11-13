package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.travelWithItem
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SceptreItems : ObjReferences() {
    val right = find("sos_half_skull1")
    val left = find("sos_half_skull2")
    val top = find("sos_half_sceptre1")
    val bottom = find("sos_half_sceptre2")
    val skull = find("sos_skull")
    val rod = find("sos_sceptre")
    val regular = find("sos_skull_sceptre")
    val imbued = find("sos_skull_sceptre_imbued")
    val fragments = find("sos_skull_sceptre_fragments")
}
internal object StrongholdNpcs : NpcReferences() {
    val spirit = find("sos_barb_spirit")
}
internal object SceptreVars : VarpReferences() {
    val charges = find("stronghold_sceptre_charges")
}
internal object SceptreVarBuilds : VarpBuilder() {
    init { build("stronghold_sceptre_charges") { transmitNever = true } }
}

public class SkullSceptre @Inject constructor(
    private val objects:ObjRepository,
    private val random:GameRandom,
    private val players:PlayerList,
    private val types:ObjTypeList,
    private val events:EventBus
) : PluginScript() {
    override fun ScriptContext.startup() {
        val items = SceptreItems
        onEvent<NpcKillEvents.Credited> {
            val piece = when (npc.id) {
                in 2481..2483 -> items.right
                in 2498..2500 -> items.bottom
                in 2474..2476 -> items.top
                in 2514..2516 -> items.left
                else -> return@onEvent
            }
            val denominator = if (piece == items.right || piece == items.top) 101 else 100
            if (random.of(denominator) < 3) objects.add(piece,npc.coords,constants.lootdrop_duration,npc.findHero(players) ?: player)
        }
        onOpHeldU(items.right,items.left) { combine(items.right,items.left,items.skull) }
        onOpHeldU(items.top,items.bottom) { combine(items.top,items.bottom,items.rod) }
        onOpHeldU(items.skull,items.rod) {
            val makeImbued = vars[StrongholdUnlocks.imbued] != 0 && player.twoFactorEnabled
            val result = if (makeImbued) items.imbued else items.regular
            val first = result !in inv && result !in bank && result !in worn
            if (combine(items.skull,items.rod,result) && first) setCharges(capacity())
        }
        val pieces = listOf(items.right,items.left,items.top,items.bottom)
        for (piece in pieces) onOpHeldU(objs.chisel,piece) {
            val quantity = valueOf(piece)
            val result = player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag,piece.id,1)
                add(bag,items.fragments.id,quantity)
            }
            if (result.success) mes("You chip the piece into $quantity bone fragments.")
        }
        for (fuel in pieces + listOf(items.fragments,items.regular)) {
            onOpHeldU(fuel,items.imbued) { recharge(fuel) }
        }
        onOpHeldU(items.imbued,items.imbued) { if (it.firstSlot != it.secondSlot) recharge(items.imbued) }
        for (sceptre in listOf(items.regular,items.imbued)) {
            onOpHeld3(sceptre) { invoke(inv,it.slot,sceptre) }
            onOpWorn2(sceptre) { invoke(worn,it.slot,sceptre) }
            onOpHeld4(sceptre) { divine(sceptre) }
            onOpWorn3(sceptre) { divine(sceptre) }
        }
        onOpNpc1(StrongholdNpcs.spirit) { imbue() }
        onOpNpcU(StrongholdNpcs.spirit,items.regular) { imbue() }
    }

    private fun ProtectedAccess.combine(first:ObjType,second:ObjType,result:ObjType):Boolean {
        val transaction = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,first.id,1)
            delete(bag,second.id,1)
            add(bag,result.id,1)
        }
        if (transaction.success) mes("You fit the pieces together to make a ${types[result].name.lowercase()}.")
        return transaction.success
    }
    private fun ProtectedAccess.diaryTiers() = DiaryDifficulty.entries.count {
        val tier = DiaryRegion.Varrock.tier(it)
        vars[tier.complete] != 0 || vars[tier.reward] != 0
    }
    private fun ProtectedAccess.capacity() = 10 + 4*diaryTiers()
    // Zero denotes an unopened pool on an existing save. One denotes an empty imbued sceptre.
    private fun ProtectedAccess.charges() = if (vars[SceptreVars.charges] == 0) capacity() else vars[SceptreVars.charges]-1
    private fun ProtectedAccess.setCharges(value:Int) { vars[SceptreVars.charges] = value+1 }
    private fun ProtectedAccess.valueOf(piece:ObjType) = diaryTiers() + if (piece == SceptreItems.left) 5 else 3

    private suspend fun ProtectedAccess.invoke(bag:Inventory,slot:Int,sceptre:ObjType) {
        if (lostImbue(sceptre)) return
        if (charges() == 0) { mes("Your sceptre has no charges left."); return }
        val arrived = travelWithItem(StrongholdOfSecurity.surface,membersOnly=false) {
            if (bag[slot]?.id != sceptre.id || charges() == 0) return@travelWithItem false
            val remaining = charges()-1
            if (remaining == 0 && sceptre == SceptreItems.regular) {
                bag[slot] = null
                if (bag === worn) WornUnequipOp.notifyWornUnequip(player,Wearpos.RightHand,types[sceptre],events)
                setCharges(capacity())
                mes("Your skull sceptre shatters.")
            } else setCharges(remaining)
            true
        }
        if (arrived) player.completeDiaryTask(DiaryTask.UseSkullSceptre)
    }
    private fun ProtectedAccess.divine(type:ObjType) {
        if (!lostImbue(type)) mes("Your skull sceptre has ${charges()} charges remaining.")
    }
    private fun ProtectedAccess.recharge(fuel:ObjType) {
        if (lostImbue(SceptreItems.imbued)) return
        val missing = capacity()-charges()
        if (missing <= 0) { mes("The sceptre is already fully charged."); return }
        val fragments = fuel == SceptreItems.fragments
        val count = if (fragments) minOf(invTotal(inv,fuel),missing) else 1
        val amount = when (fuel) {
            SceptreItems.fragments -> count
            SceptreItems.imbued,SceptreItems.regular -> capacity()
            else -> valueOf(fuel)
        }
        if (count > 0 && invDel(inv,fuel,count).success) {
            setCharges(charges()+minOf(missing,amount))
            divine(SceptreItems.imbued)
        }
    }

    private fun ProtectedAccess.imbue() {
        if (!player.twoFactorEnabled) { mes("Enable two-factor authentication to imbue a skull sceptre."); return }
        if (SceptreItems.regular !in inv && SceptreItems.regular !in worn) {
            mes("Bring me a skull sceptre.")
            return
        }
        if (listOf(StrongholdBoots.fancy,StrongholdBoots.fighting,StrongholdBoots.fancier).none { it in inv || it in worn }) {
            mes("Show me a pair of boots from the Cradle of Life.")
            return
        }
        if (!convertAll(SceptreItems.regular,SceptreItems.imbued)) return
        vars[StrongholdUnlocks.imbued] = 1
        mes("Solztun imbues your skull sceptre.")
    }

    private fun ProtectedAccess.lostImbue(type:ObjType):Boolean {
        if (type != SceptreItems.imbued || player.twoFactorEnabled) return false
        if (!convertAll(SceptreItems.imbued,SceptreItems.regular)) return true
        vars[StrongholdUnlocks.imbued] = 0
        if (charges() == 0) setCharges(1)
        mes("Your skull sceptre loses its imbue because two-factor authentication is disabled.")
        return true
    }

    private fun ProtectedAccess.convertAll(from:ObjType,to:ObjType):Boolean {
        val banked = bank.indices.filter { bank[it]?.id == from.id }
        if (banked.isNotEmpty()) {
            val converted = player.invTransaction(bank) {
                val storage = select(bank)
                for (slot in banked) {
                    val obj = bank[slot]!!
                    delete(storage,from.id,obj.count,slot)
                    add(storage,to.id,obj.count,vars=obj.vars)
                }
            }
            if (!converted.success) {
                mes("There is not enough room to convert the skull sceptres in your bank.")
                return false
            }
        }
        for (bag in listOf(inv,worn)) for (slot in bag.indices) {
            val obj = bag[slot] ?: continue
            if (obj.id != from.id) continue
            bag[slot] = InvObj(to,obj.count,obj.vars)
            if (bag === worn) {
                events.publish(HeldEquipEvents.WearposChange(player,Wearpos.RightHand,types[to]))
                rebuildAppearance()
            }
        }
        return true
    }
}
