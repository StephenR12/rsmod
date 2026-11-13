package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varp.VarpType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object IbanQuest : VarpReferences() {
    public val progress: VarpType = find("upass")
}

internal object IbanPeople : NpcReferences() {
    val mage = find("upassmage_2ops")
    val koftik = find("caveguide_vis")
}

internal object IbanItems : ObjReferences() {
    val broken = find("brokenibanstaff")
}

internal object IbanWell : LocReferences() {
    val well = find("bloodwell_upass")
}

public class IbanStaffServices
@Inject
constructor(
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val charges: IbanStaffChargeRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((t, c) in
            listOf(
                IbanPeople.mage to CoordGrid(2458, 3311),
                IbanPeople.koftik to CoordGrid(2436, 3315),
            )) if (npcs.findAll(c).none { it.id == t.id }) npcs.add(Npc(types[t], c), Int.MAX_VALUE)
        val staves = listOf(IbanItems.broken, objs.ibans_staff, objs.ibans_staff_u)
        onOpNpc1(IbanPeople.koftik) {
            if (!eligible()) return@onOpNpc1
            if (staves.any { it in inv || it in worn || it in bank }) {
                mes("You already have Iban's staff.")
                return@onOpNpc1
            }
            if (invAdd(inv, IbanItems.broken).success) mes("Koftik gives you a broken staff.")
        }
        onOpNpc1(IbanPeople.mage) { service(null) }
        onOpNpc3(IbanPeople.mage) { service(null) }
        for (staff in staves) onOpNpcU(IbanPeople.mage, staff) { service(staff) }
        for (staff in staves.drop(1)) {
            onOpHeld4(staff) { checkCharges() }
            onOpWorn2(staff) { checkCharges() }
            onOpLocU(IbanWell.well, staff) {
                if (eligible()) {
                    charges.set(player, capacity(staff))
                    mes("You recharge Iban's staff in the well.")
                }
            }
        }
    }

    private fun ProtectedAccess.checkCharges() {
        mes("Your staff has ${charges.get(player)} charges remaining.")
    }

    private fun ProtectedAccess.eligible(): Boolean {
        if (player.members && vars[IbanQuest.progress] >= 11) return true
        mes("You must complete Underground Pass first.")
        return false
    }

    private fun capacity(staff: ObjType): Int = if (staff.id == objs.ibans_staff_u.id) 2500 else 120

    private suspend fun ProtectedAccess.service(used: ObjType?) {
        if (!eligible()) return
        val source =
            used
                ?: listOf(IbanItems.broken, objs.ibans_staff, objs.ibans_staff_u).firstOrNull {
                    it in inv
                }
        if (source == null || source !in inv) {
            mes("Bring me Iban's staff.")
            return
        }
        val broken = source.id == IbanItems.broken.id
        val upgrade =
            source.id != objs.ibans_staff_u.id &&
                choice2(
                    "Upgrade the staff.",
                    true,
                    if (broken) "Repair the staff." else "Recharge the staff.",
                    false,
                )
        val target =
            if (upgrade || source.id == objs.ibans_staff_u.id) objs.ibans_staff_u
            else objs.ibans_staff
        val cost =
            when {
                broken && upgrade -> 400000
                broken || upgrade -> 200000
                target.id == objs.ibans_staff_u.id -> 250000
                else -> 100000
            }
        if (!broken && !upgrade && charges.get(player) >= capacity(target)) {
            mes("Your staff is already fully charged.")
            return
        }
        if (!choice2("Pay $cost coins.", true, "Keep my coins.", false)) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.coins.id, cost)
                delete(bag, source.id, 1)
                add(bag, target.id, 1)
            }
        if (result.success) {
            charges.set(player, capacity(target))
            mes("The Dark Mage gives you a fully charged staff.")
        } else mes("You need $cost coins and the staff.")
    }
}
