package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.ObjTradeEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ArmouryInvs : InvReferences() {
    val shops = (1..6).map { find("whiteknight_armoury$it") }
}

internal object ArmouryStock : ObjReferences() {
    val tier =
        listOf(
                listOf("chainbody", "platelegs"),
                listOf("dagger", "sword"),
                listOf("med_helm", "plateskirt", "sq_shield"),
                listOf("mace", "magic_staff", "longsword"),
                listOf("full_helm", "platebody", "kiteshield", "armoured_boots", "gloves"),
                listOf("2h_sword", "halberd", "scimitar", "warhammer", "battleaxe", "claws"),
            )
            .map { items -> items.map { find("white_$it") } }
    val sword = tier.last().first()
}

internal object ArmouryShops : InvEditor() {
    init {
        for ((rank, shop) in ArmouryInvs.shops.withIndex()) edit(shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            val items = ArmouryStock.tier.take(rank + 1).flatten()
            // References resolve to their numeric IDs at load time. Preserve the stock order by
            // name.
            val sorted =
                listOf(
                    "chainbody",
                    "platelegs",
                    "plateskirt",
                    "med_helm",
                    "sq_shield",
                    "full_helm",
                    "kiteshield",
                    "platebody",
                    "armoured_boots",
                    "gloves",
                    "dagger",
                    "sword",
                    "mace",
                    "magic_staff",
                    "longsword",
                    "warhammer",
                    "battleaxe",
                    "claws",
                    "halberd",
                    "scimitar",
                    "2h_sword",
                )
            for (name in sorted) items
                .firstOrNull { it.internalNameValue == "white_$name" }
                ?.let { stock += stock(it, count = 20, restockCycles = 100) }
        }
    }
}

internal object ArmouryNpc : NpcReferences() {
    val vyvin = find("sir_vyvin")
}

internal object KnightBits : VarBitReferences() {
    val wanted = find("wanted_main")
    val promoted =
        listOf("novice", "peon", "page", "noble", "adept", "master").map {
            find("wanted_${it}_spam")
        }
}

internal object KnightVars : VarpReferences() {
    val points = find("white_knight_score")
    val bonus = find("white_knight_completion_bonus")
}

internal object KnightSaved : VarpBuilder() {
    init {
        for (name in listOf("white_knight_score", "white_knight_completion_bonus")) build(name) {
            permanent = true
            transmitNever = true
        }
    }
}

private var Player.knightScore by intVarp(KnightVars.points)
private var Player.completionBonus by intVarp(KnightVars.bonus)
private val rankAt = listOf(100, 200, 300, 500, 800, 1300)
private val titles = listOf("Novice", "Peon", "Page", "Noble", "Adept", "Master")

public class WhiteKnightArmoury @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcKillEvents.Credited> {
            if (player.vars[KnightBits.wanted] < 6) return@onEvent
            val points =
                when (npc.id) {
                    516,
                    517,
                    4331,
                    11952,
                    11953 -> 1
                    13463,
                    13464 -> 12
                    in 13465..13470 -> 6
                    1798,
                    1799,
                    1800,
                    1829,
                    in 11948..11951 -> -1
                    else -> return@onEvent
                }
            updateBonus(player)
            if (player.knightScore >= 1300) return@onEvent
            val old = player.knightScore
            player.knightScore = (old + points).coerceIn(0, 1300)
            for ((i, milestone) in rankAt.withIndex()) if (
                old < milestone &&
                    player.knightScore >= milestone &&
                    player.vars[KnightBits.promoted[i]] == 0
            ) {
                VarPlayerIntMapSetter.set(player, KnightBits.promoted[i], 1)
                player.mes("Congratulations! You are now a White Knight ${titles[i]}!")
            }
        }
        onPlayerLogin { updateBonus(player) }
        onOpNpc1(ArmouryNpc.vyvin) {
            if (!player.members || vars[KnightBits.wanted] < 11) {
                mes("You need to complete Wanted! to trade at the White Knight Armoury.")
                return@onOpNpc1
            }
            updateBonus(player)
            val rank = rankAt.indexOfLast { player.knightScore >= it }
            if (rank < 0) {
                mes("You need at least 100 Black Knight kills to regain your rank.")
                return@onOpNpc1
            }
            mes(
                "You are a ${titles[rank]} White Knight with a kill score of ${player.knightScore}."
            )
            shops.open(player, "White Knight Armoury", ArmouryInvs.shops[rank], 100.0, 100.0, 0.0)
        }
        onEvent<ObjTradeEvents.Purchased> {
            if (
                shop.id == ArmouryInvs.shops.last().id &&
                    obj.id == ArmouryStock.sword.id &&
                    player.vars[KnightBits.wanted] >= 11 &&
                    player.knightScore >= 1300
            )
                player.completeDiaryTask(DiaryTask.BuyWhiteTwoHandedSword)
        }
    }

    private fun updateBonus(p: Player) {
        if (p.vars[KnightBits.wanted] >= 11 && p.completionBonus == 0) {
            p.knightScore = (p.knightScore + 100).coerceAtMost(1300)
            p.completionBonus = 1
        }
    }
}
