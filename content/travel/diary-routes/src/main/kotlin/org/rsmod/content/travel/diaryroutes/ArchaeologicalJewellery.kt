package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.travelWithItem
import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ArchaeologyItems : ObjReferences() {
    val pendants = (1..5).map { find("necklace_of_digsite_$it") }
    val camulet = find("camulet")
    val dung = find("feud_camel_pooh_bucket")
}

internal object ArchaeologyNpcs : NpcReferences() {
    val lazim = find("enakh_lazim")
    val templeLazim = find("enakh_lazim_altar_multinpc")
    val fullore = find("akd_lydia_fullore_vis")
    val castleFullore = find("akd_lydia_fullore_castle")
}

internal object ArchaeologyBits : VarBitReferences() {
    val fossilQuest = find("fossilquest_progress")
    val dragonSlayer = find("ds2")
    val lithkren = find("lithkren_ruby_necklace_redirect")
    val lament = find("enakh_quest")
    val charges = find("enakh_camulet_charge")
}
internal object ArchaeologyVars : VarpReferences() {
    val digsite = find("itexamlevel")
    val fossil = find("fossil_island_pendant_attuned")
    val camulet = find("camulet_settings")
}
internal object ArchaeologyBuilds : VarpBuilder() {
    init {
        build("camulet_settings") { transmitNever = true }
        build("fossil_island_pendant_attuned") { transmitNever = true }
    }
}
internal object ArchaeologyLocs : LocReferences() {
    val fossilMachine = find("fossil_lab_necklace_enchanter")
    val lithkrenMachine = find("lithkren_necklace_enchanter")
}

public class ArchaeologicalJewellery @Inject constructor(private val objTypes: ObjTypeList, private val events: EventBus) : PluginScript() {
    private val pendantNames = listOf("Digsite", "Fossil Island", "Lithkren Dungeon")
    private val destinations = listOf(CoordGrid(3341,3445), CoordGrid(3763,3869,1), CoordGrid(3549,10456))

    override fun ScriptContext.startup() {
        for ((index, pendant) in ArchaeologyItems.pendants.withIndex()) {
            onOpHeld4(pendant) {
                val choice = menu("Where would you like to teleport?",true,pendantNames)
                if (choice in destinations.indices) pendantTeleport(inv,it.slot,index,choice)
            }
            onOpWorn2(pendant) { pendantTeleport(worn,it.slot,index,0) }
            onOpWorn3(pendant) { pendantTeleport(worn,it.slot,index,1) }
            onOpWorn4(pendant) { pendantTeleport(worn,it.slot,index,2) }
            onOpLocU(ArchaeologyLocs.fossilMachine,pendant) { attune(pendant,it.invSlot,true) }
            onOpLocU(ArchaeologyLocs.lithkrenMachine,pendant) { attune(pendant,it.invSlot,false) }
        }
        val camulet = ArchaeologyItems.camulet
        onOpHeld3(camulet) { checkCamulet() }
        onOpWorn2(camulet) { checkCamulet() }
        onOpHeld4(camulet) {
            if (surfaceUnlocked()) {
                when (menu("Camulet",true,listOf("Temple","Surface","Change default destination","Cancel"))) {
                    0 -> camuletTeleport(inv,it.slot,false)
                    1 -> camuletTeleport(inv,it.slot,true)
                    2 -> toggleDefault()
                }
            } else camuletTeleport(inv,it.slot,false)
        }
        onOpWorn3(camulet) { camuletTeleport(worn,it.slot,setting(SURFACE)) }
        onOpWorn4(camulet) { camuletTeleport(worn,it.slot,false) }
        onOpWorn5(camulet) { camuletTeleport(worn,it.slot,true) }
        onOpHeldU(camulet,ArchaeologyItems.dung) {
            if (!player.members || vars[ArchaeologyBits.lament] < 70) return@onOpHeldU
            if (setting(UNLIMITED) || vars[ArchaeologyBits.charges] >= 4) {
                mes("Your Camulet is already fully charged.")
            } else if (invReplace(inv,it.secondSlot,1,objs.bucket_empty).success) {
                vars[ArchaeologyBits.charges] = 4
                checkCamulet()
            }
        }
        for (lazim in listOf(ArchaeologyNpcs.lazim,ArchaeologyNpcs.templeLazim)) onOpNpc1(lazim) { speakToLazim() }
        for (fullore in listOf(ArchaeologyNpcs.fullore,ArchaeologyNpcs.castleFullore)) onOpNpc1(fullore) {
            if (!player.members || vars[KourendRelicBits.divided] < KINGDOM_DIVIDED_COMPLETE) return@onOpNpc1
            if (objs.book_of_the_dead in bank || objs.book_of_the_dead in worn || objs.book_of_the_dead in inv) {
                mes("You already have your book of the dead.")
            } else if (invAdd(inv,objs.book_of_the_dead).success) {
                mes("Commander Fullore returns your book of the dead.")
            }
        }
    }

    private fun ProtectedAccess.knowsPendant() =
        player.members && vars[ArchaeologyVars.digsite] >= 9 &&
            (vars[varbits.vm_necklace] != 0 || vars[varbits.vm_digsite_talisman] != 0)

    private suspend fun ProtectedAccess.attune(pendant:ObjType,slot:Int,fossil:Boolean) {
        val eligible = if (fossil) vars[ArchaeologyBits.fossilQuest] >= 50 else vars[ArchaeologyBits.dragonSlayer] >= 215
        if (!knowsPendant() || !eligible) { mes("You cannot attune your pendant here yet."); return }
        val active = if (fossil) vars[ArchaeologyVars.fossil] != 0 else vars[ArchaeologyBits.lithkren] != 0
        if (active && !choice2("Remove this destination.",true,"Keep it.",false)) return
        if (inv[slot]?.id != pendant.id) return
        val value = if (active) 0 else 1
        if (fossil) vars[ArchaeologyVars.fossil] = value else vars[ArchaeologyBits.lithkren] = value
        mes(if (active) "You remove this destination from your pendants." else "Your digsite pendants are now attuned to this place.")
    }

    private suspend fun ProtectedAccess.pendantTeleport(bag:Inventory,slot:Int,chargeIndex:Int,site:Int) {
        if (!knowsPendant() || site == 1 && vars[ArchaeologyVars.fossil] == 0 || site == 2 && vars[ArchaeologyBits.lithkren] == 0) {
            mes("You have not yet unlocked that destination.")
            return
        }
        val pendant = ArchaeologyItems.pendants[chargeIndex]
        val travelled = travelWithItem(destinations[site]) {
            if (bag[slot]?.id != pendant.id) return@travelWithItem false
            val next = ArchaeologyItems.pendants.getOrNull(chargeIndex-1)
            bag[slot] = next?.let { InvObj(it) }
            if (bag === worn && next == null) WornUnequipOp.notifyWornUnequip(player,Wearpos.Front,objTypes[pendant],events)
            true
        }
        if (!travelled) return
        mes(if (chargeIndex == 0) "Your digsite pendant crumbles to dust." else "Your digsite pendant has $chargeIndex uses left.")
        if (site == 0) player.completeDiaryTask(DiaryTask.UseDigsitePendant)
    }

    private fun ProtectedAccess.setting(bit:Int) = vars[ArchaeologyVars.camulet] and bit != 0
    private fun ProtectedAccess.surfaceUnlocked() = vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).reward] != 0
    private fun ProtectedAccess.toggleDefault() {
        vars[ArchaeologyVars.camulet] = vars[ArchaeologyVars.camulet] xor SURFACE
        mes(if (setting(SURFACE)) "Your Camulet now takes you to the surface." else "Your Camulet now takes you inside the temple.")
    }
    private fun ProtectedAccess.checkCamulet() {
        mes(if (setting(UNLIMITED)) "Your Camulet has unlimited charges." else "Your Camulet has ${vars[ArchaeologyBits.charges]} charges left.")
    }
    private suspend fun ProtectedAccess.camuletTeleport(bag:Inventory,slot:Int,surface:Boolean) {
        if (vars[ArchaeologyBits.lament] < 70 || surface && !surfaceUnlocked()) {
            mes("You have not unlocked that destination.")
            return
        }
        if (!setting(UNLIMITED) && vars[ArchaeologyBits.charges] == 0) { checkCamulet(); return }
        val dest = if (surface) CoordGrid(3191,2924) else CoordGrid(3105,9315,2)
        val travelled = travelWithItem(dest) {
            if (bag[slot]?.id != ArchaeologyItems.camulet.id) return@travelWithItem false
            if (!setting(UNLIMITED)) {
                if (vars[ArchaeologyBits.charges] == 0) return@travelWithItem false
                vars[ArchaeologyBits.charges]--
            }
            true
        }
        if (travelled) {
            checkCamulet()
            if (!surface) player.completeDiaryTask(DiaryTask.UseTempleCamulet)
        }
    }
    private suspend fun ProtectedAccess.speakToLazim() {
        if (!player.members || vars[ArchaeologyBits.lament] < 70) return
        val camulet = ArchaeologyItems.camulet
        if (camulet !in inv && camulet !in worn) {
            if (camulet in bank) mes("You already have a Camulet in your bank.")
            else if (invAdd(inv,camulet).success) mes("Lazim gives you a replacement Camulet.")
            return
        }
        if (setting(UNLIMITED)) { checkCamulet(); return }
        if (!choice2("Pay 1,000,000 coins for unlimited Camulet charges.",true,"Keep my coins.",false)) return
        if (camulet !in inv && camulet !in worn) return
        if (invDel(inv,objs.coins,1_000_000).success) {
            vars[ArchaeologyVars.camulet] = vars[ArchaeologyVars.camulet] or UNLIMITED
            checkCamulet()
        } else mes("You need 1,000,000 coins.")
    }
    private companion object {
        const val UNLIMITED = 1
        const val SURFACE = 2
    }
}
internal const val KINGDOM_DIVIDED_COMPLETE = 150
