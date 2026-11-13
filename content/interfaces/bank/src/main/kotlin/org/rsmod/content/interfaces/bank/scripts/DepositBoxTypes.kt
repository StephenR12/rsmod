package org.rsmod.content.interfaces.bank.scripts

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object DepositBoxInterfaces : InterfaceReferences() { val box=find("bank_depositbox") }
internal object DepositBoxComponents : ComponentReferences() {
    val inventory=find("bank_depositbox:inventory")
    val backpack=find("bank_depositbox:deposit_inv")
    val worn=find("bank_depositbox:deposit_worn")
    val looting=find("bank_depositbox:deposit_lootingbag")
    val settings=find("bank_depositbox:menu")
    val quantities=listOf("1","5","10","x","all").map { find("bank_depositbox:$it") }
}
internal object DepositBoxVars : VarpReferences() { val locked=find("bank_locked_slots") }
internal object DepositBoxBits : VarBitReferences() {
    val ignoreLocks=find("bank_side_slot_ignoreinvlocks")
    val showLocks=find("bank_side_slot_showop")
}
internal object DepositBoxInvs : InvReferences() { val looting=find("looting_bag") }
