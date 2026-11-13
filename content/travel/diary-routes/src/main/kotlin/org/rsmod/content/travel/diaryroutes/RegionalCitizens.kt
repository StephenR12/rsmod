package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.EntranaEquipment
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RegionalCitizensNpcs : NpcReferences() {
    val security = find("sos_guard")
    val probita = find("pet_insurance_broker")
    val apothecary = find("apothecary")
    val emblem = find("emblem_trader")
    val sarimMonks = listOf("shipmonk","shipmonk1_b","shipmonk1_c").map { find(it) }
    val entranaMonks = listOf("shipmonk2","shipmonk2_b","shipmonk2_c").map { find(it) }
}
internal object RegionalCitizensGoods : ObjReferences() {
    val book = find("sos_security_book")
    val root = find("limpwurt_root")
    val potion = find("strength4")
}
internal object PetInsurance : InterfaceReferences() { val main = find("pet_insurance") }

public class RegionalCitizens @Inject constructor(private val types: ObjTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(RegionalCitizensNpcs.security) {
            if (RegionalCitizensGoods.book in inv) { mes("You already have the security book."); return@onOpNpc1 }
            if (invAdd(inv,RegionalCitizensGoods.book).success) {
                mes("The Security Guard gives you a security book.")
                player.completeDiaryTask(DiaryTask.ClaimSecurityBook)
            }
        }
        onOpNpc3(RegionalCitizensNpcs.probita) { insurance() }
        onOpNpc1(RegionalCitizensNpcs.probita) {
            if (choice2("Check my insured pets.",true,"Leave.",false)) insurance()
        }
        onOpNpc1(RegionalCitizensNpcs.apothecary) { strengthPotion() }
        onOpNpc3(RegionalCitizensNpcs.apothecary) { strengthPotion() }
        onOpNpc1(RegionalCitizensNpcs.emblem) {
            if (it.npc.isWithinDistance(CoordGrid(3096,3503),12) &&
                choice2("What are emblems?",true,"Leave.",false)) {
                mes("Emblems are rewards from Bounty Hunter. You can trade eligible emblems at the crater.")
                player.completeDiaryTask(DiaryTask.AskEmblemTrader)
            }
        }
        for (monk in RegionalCitizensNpcs.sarimMonks) {
            onOpNpc3(monk) { entrana(outbound = true) }
            onOpNpc1(monk) { if (choice2("Sail to Entrana.",true,"Stay here.",false)) entrana(outbound = true) }
        }
        for (monk in RegionalCitizensNpcs.entranaMonks) {
            onOpNpc3(monk) { entrana(outbound = false) }
            onOpNpc1(monk) { if (choice2("Sail to Port Sarim.",true,"Stay here.",false)) entrana(outbound = false) }
        }
    }
    private fun ProtectedAccess.insurance() {
        if (!player.members) return
        ifOpenMainModal(PetInsurance.main)
        player.completeDiaryTask(DiaryTask.CheckPetInsurance)
    }
    private suspend fun ProtectedAccess.strengthPotion() {
        if (!choice2("Make a strength potion for a limpwurt root, red spiders' eggs and 5 coins.",true,"No thanks.",false)) return
        val result = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,RegionalCitizensGoods.root.id,1)
            delete(bag,GatheringObjs.eggs.id,1)
            delete(bag,objs.coins.id,5)
            add(bag,RegionalCitizensGoods.potion.id,1)
        }
        if (result.success) {
            mes("The Apothecary gives you a strength potion.")
            player.completeDiaryTask(DiaryTask.BrewApothecaryStrength)
        } else mes("Bring a limpwurt root, red spiders' eggs and 5 coins.")
    }

    private fun ProtectedAccess.entrana(outbound: Boolean) {
        if (!player.members) return
        if (outbound && listOf(inv,worn).any { bag -> bag.filterNotNull().any { EntranaEquipment.prohibited(types[it]) } }) {
            mes("The monks will not allow that equipment on Entrana.")
            return
        }
        val landing = if (outbound) CoordGrid(2834,3335) else CoordGrid(3047,3234)
        teleport(landing)
        publish(PlayerTeleportEvent.Start(player,landing))
        if (outbound) player.completeDiaryTask(DiaryTask.SailToEntrana)
    }
}
