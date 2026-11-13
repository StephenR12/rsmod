package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invClear
import org.rsmod.api.invtx.invMoveAll
import org.rsmod.api.obj.plugin.ObjTakeDestination
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object LootingBagInv:InvReferences() { val storage = find("looting_bag") }
internal object LootingBagSetup:InvEditor() { init { edit(LootingBagInv.storage) { scope=InvScope.Perm } } }
internal object LootingBagBankOptions : ObjEditor() {
    init { for(bag in listOf(objs.looting_bag,objs.looting_bag_open)) edit(bag) { param[params.bankside_extraop]="Empty" } }
}
internal object LootingBagUi:InterfaceReferences() { val contents=find("wilderness_lootingbag") }
internal object LootingBagBits:VarBitReferences() {
    val all=find("lootingbag_useallitems")
    val ignoreFood=find("lootingbag_ignore_food")
}

internal class LootingBagModule : PluginModule() {
    override fun bind() { addSetBinding<ObjTakeDestination>(LootingBag::class.java) }
}

public class LootingBag @Inject constructor(private val invsById:InvTypeList, private val types:ObjTypeList, private val ground:ObjRepository):PluginScript(), ObjTakeDestination {
    override fun ScriptContext.startup() {
        for (bag in listOf(objs.looting_bag,objs.looting_bag_open)) {
            onOpHeld1(bag) {
                val other = if(bag == objs.looting_bag) objs.looting_bag_open else objs.looting_bag
                invReplaceSlot(inv,it.slot,1,other)
            }
            onOpHeld2(bag) {
                invTransmit(inv(LootingBagInv.storage))
                ifOpenMainModal(LootingBagUi.contents)
                runClientScript(495,0,"Looting bag")
            }
            onOpHeld3(bag) {
                if (!storableArea(player)) { mes("You can only store items in the Wilderness."); return@onOpHeld3 }
                val storage = inv(LootingBagInv.storage)
                for (slot in inv.indices) {
                    val obj=inv[slot] ?: continue
                    if(eligible(types[obj],player)) invMoveFromSlot(inv,storage,slot,obj.count,strict=false)
                }
            }
            onOpHeldU(bag) {
                val obj = inv[it.secondSlot] ?: return@onOpHeldU
                if (!storableArea(player)) { mes("You can only store items in the Wilderness."); return@onOpHeldU }
                if (!eligible(it.second,player)) { mes("You can't store that item."); return@onOpHeldU }
                val count = if (obj.count == 1 || player.vars[LootingBagBits.all] != 0) obj.count
                else choice3("One",1,"Five",5,"All",obj.count)
                if (inv[it.secondSlot]?.id == obj.id && bag in inv && storableArea(player)) {
                    invMoveFromSlot(inv,inv(LootingBagInv.storage),it.secondSlot,count,strict=false)
                }
            }
            onOpHeld4(bag) {
                val bit = choice2("Change the deposit quantity.",LootingBagBits.all,"Change whether to ignore food.",LootingBagBits.ignoreFood)
                val enabled = if (bit == LootingBagBits.all) choice2("Deposit all.",1,"Ask every time.",0) else choice2("Ignore food and potions.",1,"Store food and potions.",0)
                VarPlayerIntMapSetter.set(player,bit,enabled)
            }
            onOpBankside(bag) {
                if(bag in player.inv) player.invMoveAll(player.invMap.getOrPut(invsById[LootingBagInv.storage]),player.invMap.getOrPut(invsById[invs.bank]),uncert=true)
            }
        }
        onIfClose(LootingBagUi.contents) { player.invMap[LootingBagInv.storage]?.let(player::stopInvTransmit) }
        onDestroyHeld {
            if(type.id == objs.looting_bag.id || type.id == objs.looting_bag_open.id) {
                val storage=player.invMap[LootingBagInv.storage] ?: return@onDestroyHeld
                if (player.vars[varbits.inside_wilderness] == 1 && player.coords.z > 3522) {
                    for(item in storage.filterNotNull()) if(!supplies(types[item])) ground.add(item,player.coords,100)
                }
                player.invClear(storage)
            }
        }
    }

    override fun destination(player:Player,obj:Obj):Inventory? {
        if (objs.looting_bag_open !in player.inv || !storableArea(player) || !eligible(types[obj],player)) return null
        val storage=player.invMap.getOrPut(invsById[LootingBagInv.storage])
        return storage.takeIf { player.invAdd(it,obj.type,obj.count,vars=obj.vars,autoCommit=false).success }
    }

    private fun storableArea(player:Player):Boolean = player.vars[varbits.inside_wilderness] == 1 ||
        (player.coords.level == 0 && player.coords.x in 3123..3158 && player.coords.z in 3616..3645)
    private fun eligible(type:UnpackedObjType,player:Player):Boolean = type.tradeable && !type.isPlaceholder &&
        type.id != objs.looting_bag.id && type.id != objs.looting_bag_open.id &&
        (player.vars[LootingBagBits.ignoreFood] == 0 || !supplies(type))
    private fun supplies(type:UnpackedObjType):Boolean = !type.isCert &&
        (type.iop.any { it == "Eat" || it == "Drink" } || type.id == objs.vial_water.id)
}
