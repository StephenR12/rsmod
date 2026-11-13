package org.rsmod.content.travel.diaryroutes

import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerStallTheftEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MerchantNpcs : NpcReferences() {
    val silk = find("silk_merchant_ardougne")
    val otherSilk = find("silk_merchant")
    val tindel = find("tindel_marchant")
    val bert = listOf("handsand_bert","handsand_bert_1op","handsand_bert_2ops").map { find(it) }
    val simon = find("agility_pyramid_simon")
}
internal object MerchantGoods : ObjReferences() {
    val silk = find("silk")
    val rusty = find("digsitesword")
    val swords = listOf("iron_sword" to 20, "iron_longsword" to 37, "bronze_sword" to 17,
        "bronze_longsword" to 7, "steel_sword" to 6, "steel_longsword" to 4, "mithril_sword" to 1,
        "mithril_longsword" to 1, "black_sword" to 3, "black_longsword" to 2).map { (key, weight) -> find(key) to weight }
    val top = find("agility_pyramid_gold_pyramid")
    val artefacts = listOf("ntk_ivory_comb" to 50, "ntk_scarab_pottery" to 75, "ntk_statuette_pottery" to 100,
        "ntk_seal_stone" to 150, "ntk_scarab_stone" to 175, "ntk_statuette_stone" to 200,
        "ntk_seal_gold" to 750, "ntk_scarab_gold" to 1000, "ntk_statuette_gold" to 1250)
    val prices = artefacts.map { (key, price) -> find(key) to price }
    val notes = artefacts.map { (key, price) -> find("cert_$key") to price }
}
internal object MerchantLocs : LocReferences() {
    val silk = find("silkthiefstall")
    val pit = find("handsand_sandpit_anim")
}
internal object MerchantVars : VarpReferences() {
    val theft = find("ardougne_silk_theft")
    val lastSand = find("bert_last_delivery_day")
    val skipSand = find("bert_auto_delivery_disabled")
}
internal object MerchantVarBuild : VarpBuilder() {
    init { for (key in listOf("ardougne_silk_theft","bert_last_delivery_day","bert_auto_delivery_disabled")) build(key) { transmitNever = true } }
}
internal object MerchantBits : VarBitReferences() {
    val sandQuest = find("handsand_quest")
    val ironman = find("ironman")
}
internal object MerchantTimers : TimerReferences() { val forgiveSilk = find("ardougne_silk_forgiveness") }
internal object MerchantQueues : QueueReferences() { val sand = find("bert_daily_sand") }
private var Player.stoleSilk by intVarp(MerchantVars.theft)

public class RegionalMerchants : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<PlayerStallTheftEvent> {
            if (stall.id == MerchantLocs.silk.id && stall.x in 2640..2680 && stall.z in 3280..3320) {
                player.stoleSilk = 1
                player.softTimer(MerchantTimers.forgiveSilk,1000)
            }
        }
        onPlayerSoftTimer(MerchantTimers.forgiveSilk) {
            player.stoleSilk = 0
            player.clearSoftTimer(MerchantTimers.forgiveSilk)
        }
        onPlayerLogin {
            if (player.stoleSilk != 0) player.softTimer(MerchantTimers.forgiveSilk,1000)
            if (player.vars[DiaryRegion.Ardougne.tier(DiaryDifficulty.Elite).reward] != 0 && player.vars[MerchantVars.skipSand] == 0)
                player.queue(MerchantQueues.sand,1)
        }
        onPlayerQueue(MerchantQueues.sand) { deliverSand(automatic=true) }
        for (merchant in listOf(MerchantNpcs.silk,MerchantNpcs.otherSilk)) onOpNpc1(merchant) {
            sellSilk(it.npc.x in 2640..2680 && it.npc.z in 3280..3320)
        }
        onOpNpc3(MerchantNpcs.tindel) { restoreSword() }
        onOpNpcU(MerchantNpcs.tindel,MerchantGoods.rusty) { restoreSword(it.invSlot) }
        onOpNpc1(MerchantNpcs.tindel) { if (choice2("Identify a rusty sword for 100 coins.",true,"Leave.",false)) restoreSword() }
        for (bert in MerchantNpcs.bert) {
            onOpNpc3(bert) { deliverSand(false) }
            onOpNpc1(bert) {
                if (vars[DiaryRegion.Ardougne.tier(DiaryDifficulty.Elite).reward] != 0) {
                    when (menu("Bert",true,listOf("Claim sand.","Toggle automatic deliveries.","Leave."))) {
                        0 -> deliverSand(false)
                        1 -> { vars[MerchantVars.skipSand] = 1 - vars[MerchantVars.skipSand]
                            mes(if (vars[MerchantVars.skipSand] == 0) "Bert will send sand automatically." else "Bert will wait for you to ask.") }
                    }
                } else deliverSand(false)
            }
        }
        onOpLocU(MerchantLocs.pit,objs.bucket_empty) {
            if (!player.members || inv[it.invSlot]?.id != objs.bucket_empty.id || !invReplace(inv,it.invSlot,1,objs.bucket_of_sand).success) return@onOpLocU
            if (vars[MerchantBits.ironman] == 2 && vars[MerchantBits.sandQuest] >= 160)
                player.completeDiaryTask(DiaryTask.ClaimBertSand)
        }
        onOpNpc1(MerchantNpcs.simon) {
            if (choice2("Sell pyramid artefacts.",true,"Leave.",false)) sellArtefacts(null)
        }
        for ((item,_) in MerchantGoods.prices + (MerchantGoods.top to 10000)) onOpNpcU(MerchantNpcs.simon,item) {
            sellArtefacts(item)
        }
    }

    private suspend fun ProtectedAccess.sellSilk(ardougne:Boolean) {
        if (!player.members || MerchantGoods.silk !in inv) { mes("Bring some unnoted silk to sell."); return }
        if (ardougne && player.stoleSilk != 0) { mes("The merchant recognises you from the silk stall."); return }
        val price = when (menu("Price per piece of silk",true,listOf("20 coins.","80 coins.","120 coins.","200 coins."))) {
            0 -> 20
            1 -> when (menu("The merchant offers 40 coins.",true,listOf("Accept 40.","Ask for 50.","Leave."))) { 0 -> 40; 1 -> 50; else -> return }
            2 -> when (menu("The merchant offers 50 coins.",true,listOf("Accept 50.","Ask for 60.","Leave."))) { 0 -> 50; 1 -> 60; else -> return }
            else -> { mes("The merchant refuses that price."); return }
        }
        val count = invTotal(inv,MerchantGoods.silk)
        if (count <= 0 || ardougne && player.stoleSilk != 0) return
        val sold = player.invTransaction(inv) { val bag=select(inv); delete(bag,MerchantGoods.silk.id,count); add(bag,objs.coins.id,count*price) }
        if (sold.success) {
            mes("You sell $count pieces of silk for ${count * price} coins.")
            if (ardougne && price == 60) player.completeDiaryTask(DiaryTask.SellArdougneSilk)
        }
    }

    private fun ProtectedAccess.restoreSword(slot:Int = inv.indexOfFirst { it?.id == MerchantGoods.rusty.id }) {
        if (!player.members || inv[slot]?.id != MerchantGoods.rusty.id || invTotal(inv,objs.coins) < 100) {
            mes("Bring a rusty sword and 100 coins."); return
        }
        var roll = random.of(100)
        val sword = MerchantGoods.swords.firstOrNull { (_, weight) -> roll -= weight; roll < 0 }?.first
        val antique = sword == null && roll == 0
        val result = player.invTransaction(inv) {
            val bag=select(inv)
            delete(bag,MerchantGoods.rusty.id,1,slot)
            delete(bag,objs.coins.id,100)
            if (sword != null) add(bag,sword.id,1,slot)
            if (antique) add(bag,objs.coins.id,random.of(1500..1999))
        }
        if (!result.success) return
        mes(when { sword != null -> "Tindel gives you a repaired sword."; antique -> "Tindel buys the antique sword."; else -> "The rusty sword falls apart." })
        if (sword != null) player.completeDiaryTask(DiaryTask.IdentifyRustySword)
    }

    private fun ProtectedAccess.deliverSand(automatic:Boolean) {
        if ((!player.members && !automatic) || vars[MerchantBits.sandQuest] < 160 || vars[MerchantBits.ironman] == 2) {
            if (!automatic) mes("Bert can't deliver sand to you.")
            return
        }
        val today = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()
        if (vars[MerchantVars.lastSand] == today) {
            if (!automatic) mes("Bert has already delivered your sand today.")
            return
        }
        if (invAdd(bank,objs.bucket_of_sand,84).success) {
            vars[MerchantVars.lastSand] = today
            mes("Bert delivers 84 buckets of sand to your bank.")
            player.completeDiaryTask(DiaryTask.ClaimBertSand)
        } else mes("Your bank has no room for the sand.")
    }

    private fun ProtectedAccess.sellArtefacts(only:ObjType?) {
        if (!player.members) return
        val acceptsNotes = only == null && vars[DiaryRegion.Desert.tier(DiaryDifficulty.Easy).reward] != 0
        val prices = MerchantGoods.prices + (MerchantGoods.top to 10000) + if (acceptsNotes) MerchantGoods.notes else emptyList()
        val sales = prices.mapNotNull { (item,price) ->
            val count=invTotal(inv,item)
            if (count > 0 && (only == null || item == only)) Triple(item,count,price) else null
        }
        val coins = sales.sumOf { (_,count,price) -> count.toLong()*price }
        if (coins !in 1..Int.MAX_VALUE.toLong()) { mes("Bring some artefacts that Simon can buy."); return }
        val result = player.invTransaction(inv) {
            val bag=select(inv)
            for ((item,count,_) in sales) delete(bag,item.id,count)
            add(bag,objs.coins.id,coins.toInt())
        }
        if (result.success) {
            mes("Simon pays you $coins coins.")
            if (sales.any { it.first != MerchantGoods.top }) player.completeDiaryTask(DiaryTask.SellDesertArtefacts)
        }
    }
}
