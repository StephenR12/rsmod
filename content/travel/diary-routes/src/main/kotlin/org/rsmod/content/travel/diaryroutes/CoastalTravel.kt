package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object FerryNpcs : NpcReferences() {
    val musaCrew = listOf("captain_tobias", "seaman_lorris", "seaman_thresnor").map { find(it) }
    val customs = find("customs_officer")
    val barnabyArdougne = find("captain_barnaby_ardougne_model")
    val barnabyBrimhaven = find("captain_barnaby_karamja")
    val barnabyRimmington = find("captain_barnaby_rimmington")
    val hajedy = find("brimhavencartdriver")
    val vigroy = find("shilocartdriver")
    val shanks = find("captain_shanks")
    val seravel = find("shiloshiptickets")
    val sarim =
        listOf("veos_visible_travel", "veos_visible_travel_amulet", "cabin_boy_herbert_sarim_vis")
            .map { find(it) }
    val piscarilius =
        listOf("veos_vis", "veos_vis_amulet", "cabin_boy_herbert_pisc_vis").map { find(it) }
    val veosTalk = listOf(find("veos_visible"), find("veos_visible_amulet"))
    val magoro = find("zeah_magoro")
}

internal object FerryItems : ObjReferences() {
    val rum = find("karamja_rum")
    val ticket = find("shiloshipticket")
    val gloves = listOf("easy", "med", "hard", "elite").map { find("atjun_gloves_$it") }
}

internal object FerryVars : VarpReferences() {
    val shilo = find("zombiequeen")
}

internal object FerryBits : VarBitReferences() {
    val visited = find("zeah_playerhasvisited")
}

private enum class Port(val label: String, val shore: CoordGrid) {
    Sarim("Port Sarim", CoordGrid(3027, 3218)),
    Musa("Musa Point", CoordGrid(2952, 3146)),
    Ardougne("Ardougne", CoordGrid(2679, 3274)),
    Brimhaven("Brimhaven", CoordGrid(2772, 3224)),
    Rimmington("Rimmington", CoordGrid(2915, 3225)),
    SarimWest("Port Sarim", CoordGrid(3054, 3245)),
    Piscarilius("Port Piscarilius", CoordGrid(1824, 3691)),
    LandsEnd("Land's End", CoordGrid(1505, 3402)),
    Khazard("Port Khazard", CoordGrid(2663, 3161)),
}

public class CoastalTravel : PluginScript() {
    override fun ScriptContext.startup() {
        for (sailor in FerryNpcs.musaCrew) {
            onOpNpc1(sailor) { ferry(Port.Sarim, Port.Musa, talk = true) }
            onOpNpc3(sailor) { ferry(Port.Sarim, Port.Musa, talk = false) }
        }
        onOpNpc1(FerryNpcs.customs) { ferry(Port.Musa, Port.Sarim, talk = true) }
        onOpNpc3(FerryNpcs.customs) { ferry(Port.Musa, Port.Sarim, talk = false) }
        for ((sailor, ports) in
            mapOf(
                FerryNpcs.barnabyArdougne to listOf(Port.Ardougne, Port.Brimhaven, Port.Rimmington),
                FerryNpcs.barnabyBrimhaven to
                    listOf(Port.Brimhaven, Port.Ardougne, Port.Rimmington),
                FerryNpcs.barnabyRimmington to
                    listOf(Port.Rimmington, Port.Ardougne, Port.Brimhaven),
            )) {
            val (from, first, second) = ports
            onOpNpc1(sailor) { choose(first, second)?.let { ferry(from, it, talk = true) } }
            onOpNpc3(sailor) { ferry(from, first, talk = false) }
            onOpNpc4(sailor) { ferry(from, second, talk = false) }
        }
        for ((driver, landing) in
            mapOf(
                FerryNpcs.hajedy to CoordGrid(2835, 2954),
                FerryNpcs.vigroy to CoordGrid(2782, 3214),
            )) {
            onOpNpc1(driver) { cart(landing, true) }
            onOpNpc3(driver) { cart(landing, false) }
        }
        onOpNpc1(FerryNpcs.seravel) { if (player.members) buyTicket(25) }
        onOpNpc1(FerryNpcs.shanks) {
            if (!player.members) return@onOpNpc1
            if (FerryItems.ticket !in inv && !buyTicket(random.of(20..50))) return@onOpNpc1
            val port = choose(Port.Khazard, Port.Sarim) ?: return@onOpNpc1
            if (!invDel(inv, FerryItems.ticket).success) return@onOpNpc1
            loseRum("You lose your rum to a sailor in a game of dice.")
            arrive(port.shore)
            if (port == Port.Khazard) player.completeDiaryTask(DiaryTask.SailLadyToKhazard)
        }
        for ((sailors, mainland) in
            listOf(FerryNpcs.sarim to true, FerryNpcs.piscarilius to false)) {
            val other = if (mainland) Port.Piscarilius else Port.SarimWest
            for (sailor in sailors) {
                onOpNpc1(sailor) {
                    if (!mainland && vars[KourendRelicBits.client] >= 7 && vars[KourendRelicBits.divided] < KINGDOM_DIVIDED_COMPLETE) {
                        when (menu("Port Piscarilius",true,listOf(other.label,Port.LandsEnd.label,"Ask about my memoirs.","Stay here."))) {
                            0 -> kourend(other)
                            1 -> kourend(Port.LandsEnd)
                            2 -> {
                                val book = KourendRelicItems.memoirs
                                if (book in inv || book in worn || book in bank) mes("You already have your memoirs.")
                                else if (invAdd(inv,book).success) mes("You receive a replacement copy of Kharedst's memoirs.")
                            }
                        }
                    } else choose(other, Port.LandsEnd)?.let { kourend(it) }
                }
                onOpNpc3(sailor) { kourend(other) }
                onOpNpc4(sailor) { kourend(Port.LandsEnd) }
            }
        }
        for (sailor in FerryNpcs.veosTalk) onOpNpc1(sailor) {
            if (it.npc.isWithinDistance(Port.SarimWest.shore, 12))
                choose(Port.Piscarilius, Port.LandsEnd)?.let { port -> kourend(port) }
        }
        onOpNpc1(FerryNpcs.magoro) { choose(Port.SarimWest, Port.Piscarilius)?.let { kourend(it) } }
        onOpNpc3(FerryNpcs.magoro) { kourend(Port.SarimWest) }
        onOpNpc4(FerryNpcs.magoro) { kourend(Port.Piscarilius) }
    }

    private suspend fun ProtectedAccess.choose(first: Port, second: Port): Port? =
        listOf(first, second)
            .getOrNull(
                menu(
                    "Where would you like to sail?",
                    true,
                    listOf(first.label, second.label, "Stay here."),
                )
            )

    private fun ProtectedAccess.kourend(port: Port) {
        if (!player.members) {
            mes("You need to be on a members' world to sail to Great Kourend.")
            return
        }
        arrive(port.shore)
        if (port != Port.SarimWest) vars[FerryBits.visited] = 1
        if (port == Port.LandsEnd) player.completeDiaryTask(DiaryTask.SailToLandsEnd)
    }

    private suspend fun ProtectedAccess.ferry(from: Port, to: Port, talk: Boolean) {
        val customsRoute = from == Port.Musa || from == Port.Sarim
        if (!customsRoute && !player.members) return
        val discount =
            player.members &&
                (FerryItems.gloves.any { it in worn } ||
                    customsRoute &&
                        vars[DiaryRegion.Karamja.tier(DiaryDifficulty.Easy).reward] != 0)
        val fare =
            when {
                talk && player.members && worn[Wearpos.Ring.slot]?.id == CarpetItems.charos.id -> 0
                discount -> 15
                else -> 30
            }
        if (talk && !choice2("Sail to ${to.label} for $fare coins.", true, "Stay here.", false))
            return
        if (!pay(fare)) return
        if (from == Port.Musa) loseRum("The customs officer confiscates your rum.")
        arrive(to.shore)
        if (from == Port.Musa && to == Port.Sarim)
            player.completeDiaryTask(DiaryTask.SailMusaToSarim)
        if (from == Port.Brimhaven && to == Port.Ardougne)
            player.completeDiaryTask(DiaryTask.SailBrimhavenToArdougne)
    }

    private suspend fun ProtectedAccess.cart(landing: CoordGrid, talk: Boolean) {
        if (!player.members || vars[FerryVars.shilo] < 15) {
            mes("You must complete Shilo Village first.")
            return
        }
        val price =
            when {
                vars[DiaryRegion.Karamja.tier(DiaryDifficulty.Elite).reward] != 0 -> 0
                talk -> (invTotal(inv, objs.coins) / 20).coerceIn(10, 200)
                else -> 200
            }
        if (talk && !choice2("Pay $price coins for a ride.", true, "Stay here.", false)) return
        if (!pay(price)) return
        arrive(landing)
        player.completeDiaryTask(DiaryTask.RideShiloCart)
    }

    private suspend fun ProtectedAccess.buyTicket(price: Int): Boolean {
        if (!choice2("Buy a ship ticket for $price coins.", true, "No thanks.", false)) return false
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.coins.id, price)
                add(bag, FerryItems.ticket.id, 1)
            }
        if (!tx.success) mes("You need $price coins and room for the ticket.")
        return tx.success
    }

    private fun ProtectedAccess.pay(fare: Int): Boolean {
        if (fare == 0 || invDel(inv, objs.coins, count = fare).success) return true
        mes("You need $fare coins to make this journey.")
        return false
    }

    private fun ProtectedAccess.loseRum(message: String) {
        val bottles = invTotal(inv, FerryItems.rum)
        if (bottles > 0 && invDel(inv, FerryItems.rum, count = bottles).success) mes(message)
    }

    private fun ProtectedAccess.arrive(tile: CoordGrid) {
        teleport(tile)
        publish(PlayerTeleportEvent.Start(player, tile))
    }
}
