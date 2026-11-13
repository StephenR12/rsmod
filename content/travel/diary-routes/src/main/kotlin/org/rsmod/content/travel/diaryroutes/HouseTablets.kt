package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.canTravelWithItem
import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.magic.spell.utility.standard.breakTeleportTablet
import org.rsmod.content.skills.magic.spell.utility.standard.HouseLocation
import org.rsmod.content.skills.magic.spell.utility.standard.PlayerOwnedHouseRepository
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HouseTabletItems : ObjReferences() {
    val scroll = find("nzone_redirection")
    val original = find("poh_tablet_teleporttohouse")
    data class Redirect(val name:String,val tablet:ObjType,val level:Int,val coords:CoordGrid)
    private fun portal(location:HouseLocation,level:Int,suffix:String = location.name.lowercase()) =
        Redirect(location.name,find("nzone_teletab_$suffix"),level,location.outside)
    val routes = listOf(
        portal(HouseLocation.Rimmington,1),portal(HouseLocation.Taverley,10),portal(HouseLocation.Pollnivneach,20),
        portal(HouseLocation.Hosidius,25,"kourend"),portal(HouseLocation.Rellekka,30),portal(HouseLocation.Aldarin,35),
        portal(HouseLocation.Brimhaven,40),portal(HouseLocation.Yanille,50),portal(HouseLocation.Prifddinas,70),
        Redirect("Trollheim", find("nzone_teletab_trollheim"),1,CoordGrid(2890,3679))
    )
}
internal object HouseTabletBits : VarBitReferences() { val sun = find("vmq1") }
internal object HouseTabletVars : VarpReferences() { val points = find("nzone_rewardpoints") }

public class HouseTablets @Inject constructor(private val houses:PlayerOwnedHouseRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        val scroll = HouseTabletItems.scroll
        val original = HouseTabletItems.original
        onOpHeldU(scroll,original) {
            if (!player.members) return@onOpHeldU
            val routes = HouseTabletItems.routes
            val choice = menu("Redirect the house tablet",true,routes.map { it.name } + "Cancel")
            val route = routes.getOrNull(choice) ?: return@onOpHeldU
            if (!eligible(route)) { mes("You do not meet the requirements to redirect the tablet there."); return@onOpHeldU }
            val amount = quantity(minOf(invTotal(inv,scroll),invTotal(inv,original)))
            if (amount == 0) return@onOpHeldU
            val result = player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag,original.id,amount)
                delete(bag,scroll.id,amount)
                add(bag,route.tablet.id,amount)
            }
            if (result.success) mes("You redirect the tablets to ${route.name}.")
        }
        onOpHeld4(scroll) {
            val amount = quantity(invTotal(inv,scroll))
            if (amount > 0 && refund(amount) && invDel(inv,scroll,amount).success) vars[HouseTabletVars.points] += amount * 775
        }
        for (route in HouseTabletItems.routes) {
            onOpHeld1(route.tablet) {
                if (!eligible(route)) { mes("You do not meet the requirements to use that tablet."); return@onOpHeld1 }
                val arrived = breakTeleportTablet(route.tablet,it.slot,route.coords)
                if (arrived && route.name == "Pollnivneach") player.completeDiaryTask(DiaryTask.RedirectPollnivneachTablet)
            }
            onOpHeld4(route.tablet) {
                val amount = quantity(invTotal(inv,route.tablet))
                if (!player.members || amount == 0) return@onOpHeld4
                val compensate = !eligible(route)
                if (compensate && !refund(amount)) return@onOpHeld4
                if (invReplace(inv,route.tablet,amount,original).success && compensate) vars[HouseTabletVars.points] += amount * 775
            }
        }
        onOpHeld1(original) { visitHouse(it.slot,true) }
        onOpHeld2(original) { visitHouse(it.slot,true) }
        onOpHeld3(original) { visitHouse(it.slot,false) }
        onOpHeld4(original) { mes("Your account needs a group house to use this destination.") }
    }
    private fun ProtectedAccess.eligible(route:HouseTabletItems.Redirect):Boolean =
        player.members && statBase(stats.construction) >= route.level && when(route.name) {
            "Aldarin" -> vars[HouseTabletBits.sun] >= 24
            "Prifddinas" -> vars[varbits.song_of_the_elves] >= 200
            "Trollheim" -> vars[varps.eadgar_quest] >= 110
            else -> true
        }
    private suspend fun ProtectedAccess.quantity(available:Int):Int =
        if (available <= 1) available else countDialog("How many?").coerceIn(0,available)

    private fun ProtectedAccess.refund(amount:Int):Boolean {
        if (!player.members) return false
        if (vars[HouseTabletVars.points].toLong() + amount.toLong()*775 > Int.MAX_VALUE) {
            mes("Spend some Nightmare Zone points before refunding these scrolls.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.visitHouse(slot:Int,inside:Boolean) {
        val site = HouseLocation.fromCacheValue(vars[varbits.poh_house_location])
        if (site == null) { mes("You don't own a house."); return }
        if (!canTravelWithItem()) return
        val dest = if (inside) houses.destination(player,vars[varbits.poh_teleport_building_mode] != 0) else site.outside
        if (dest != null) breakTeleportTablet(HouseTabletItems.original,slot,dest)
        else mes("Your house is not ready to enter.")
    }
}
