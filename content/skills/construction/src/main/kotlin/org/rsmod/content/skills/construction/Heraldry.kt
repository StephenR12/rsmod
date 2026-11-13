package org.rsmod.content.skills.construction

import org.rsmod.api.config.refs.objs
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
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ReniteeNpc : NpcReferences() {
    val herald = find("poh_herald_of_falador")
}

internal object ReniteeBits : VarBitReferences() {
    val crest = find("poh_house_logo")
}

internal object ReniteeVars : VarpReferences() {
    val elves = find("roving_elves_quest")
    val phoenix = find("phoenixgang")
    val blackArm = find("blackarmgang")
    val lostCity = find("zanaris")
}

internal object ReniteeItems : ObjReferences() {
    val painting = find("poh_unframed_painting_istafar")
    val horses = listOf("brown", "white", "black", "grey").map { find("horsey_$it") }
}

public class Heraldry : PluginScript() {
    private val crests =
        listOf(
            "Arrav",
            "Asgarnia",
            "Dorgeshuun",
            "Dragon",
            "Fairy",
            "Guthix",
            "HAM",
            "Horse",
            "Jogre",
            "Kandarin",
            "Misthalin",
            "Money",
            "Saradomin",
            "Skull",
            "Varrock",
            "Zamorak",
        )

    override fun ScriptContext.startup() {
        onOpNpc1(ReniteeNpc.herald) {
            if (!player.members) return@onOpNpc1
            when (
                choice3(
                    "Can you see if I have a family crest?",
                    1,
                    "Can I buy an Isafdar painting?",
                    2,
                    "Leave.",
                    0,
                )
            ) {
                1 -> crest()
                2 -> painting()
            }
        }
    }

    private suspend fun ProtectedAccess.crest() {
        if (stat(stats.construction) < 16) {
            mes("You need level 16 Construction for a family crest.")
            return
        }
        val free = listOf(2, 7, 9, 10, 11, 15)
        if (vars[ReniteeBits.crest] !in 1..crests.size)
            vars[ReniteeBits.crest] = free[random.of(free.size)]
        val current = vars[ReniteeBits.crest]
        mes("Your family crest is ${crests[current-1]}.")
        player.completeDiaryTask(DiaryTask.DiscoverFaladorCrest)
        if (!choice2("Change my crest.", true, "Keep my crest.", false)) return
        val name = crests.getOrNull(menu("Choose a family crest", true, crests)) ?: return
        val id = crests.indexOf(name) + 1
        if (id == current) {
            mes("You already bear this crest.")
            return
        }
        val eligible =
            when (name) {
                "Arrav" -> vars[ReniteeVars.phoenix] >= 7 || vars[ReniteeVars.blackArm] >= 7
                "Dorgeshuun" -> vars[varbits.lost_tribe_progress] >= 11
                "Dragon" -> vars[varps.dragonquest] >= 10
                "Fairy" -> vars[ReniteeVars.lostCity] >= 6
                "Guthix",
                "Saradomin",
                "Zamorak" -> statBase(stats.prayer) >= 70
                "Horse" -> ReniteeItems.horses.any { it in inv }
                "Skull" -> player.skullIcon != null
                else -> true
            }
        if (!eligible) {
            mes("You do not meet the requirements for that crest.")
            return
        }
        val cost = if (name == "Money") 500_000 else 5_000
        if (!choice2("Pay $cost coins for the $name crest.", true, "Cancel.", false)) return
        if (invDel(inv, objs.coins, cost).failure) {
            mes("You do not have enough coins.")
            return
        }
        vars[ReniteeBits.crest] = id
        if (name == "Saradomin") player.completeDiaryTask(DiaryTask.AdoptSaradominCrest)
    }

    private suspend fun ProtectedAccess.painting() {
        if (vars[ReniteeVars.elves] < 6) {
            mes("Complete Roving Elves before commissioning the Isafdar painting.")
            return
        }
        if (!choice2("Buy the Isafdar painting for 2,000 coins.", true, "Leave.", false)) return
        val result =
            player.invTransaction(inv) {
                val inventory = select(inv)
                delete(inventory, objs.coins.id, 2000)
                add(inventory, ReniteeItems.painting.id, 1)
            }
        if (result.failure) mes("You need 2,000 coins and space for the painting.")
    }
}
