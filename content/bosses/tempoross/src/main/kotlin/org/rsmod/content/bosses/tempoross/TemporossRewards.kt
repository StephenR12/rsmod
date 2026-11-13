package org.rsmod.content.bosses.tempoross

import jakarta.inject.Inject
import org.rsmod.api.clue.Cache233ClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.content.skills.fishing.FishBarrelStorage
import org.rsmod.content.skills.fishing.FishingXp
import org.rsmod.content.skills.pets.SkillPets
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SeaStorage : InvReferences() {
    val tackle = find("tackle_box")
}

internal object SeaBoard : ComponentReferences() {
    val best = find("tempoross_stats:text_mybestpoints_data")
    val claimed = find("tempoross_stats:text_myrewardsused_used")
    val wins = find("tempoross_stats:text_mytotal_data")
    val time = find("tempoross_stats:text_myfastest_data")
    val worldWins = find("tempoross_stats:text_globaltotal_data")
    val worldTime = find("tempoross_stats:text_globalfastest_data")
}

public class TemporossRewards
@Inject
internal constructor(
    private val voyages: TemporossVoyages,
    private val locs: LocTypeList,
    private val npcs: NpcTypeList,
    private val objects: ObjTypeList,
    private val pets: SkillPets,
    private val barrel: FishBarrelStorage,
    private val clues: ClueInventory,
    private val steps: Cache233ClueSteps,
    private val ground: org.rsmod.api.repo.obj.ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (id in (41296..41304) + 41356) {
            val loc = locs.getValue(id)
            onOpLoc1(loc) { search(false) }
            onOpLoc2(loc) { report() }
            onOpLoc3(loc) { search(true) }
        }
        onOpNpc3(npcs.getValue(10605)) { takeNet() }
        onOpNpc1(npcs.getValue(10605)) {
            report()
            if (
                objs.small_fishing_net !in inv &&
                    choice2("Take a fishing net.", true, "Leave.", false)
            )
                takeNet()
        }
        onOpHeld1(objects.getValue(25590)) { openCasket() }
        val vendor = npcs.getValue(10613)
        val sailor = npcs.getValue(10612)
        val outfit = OUTFIT.flatMap { listOf(it.first, it.second) }
        for (id in outfit) onOpNpcU(vendor, objects.getValue(id)) { changeOutfit(id) }
        onOpNpc1(vendor) {
            choose(outfit, "Change which piece of angler clothing?")?.let { changeOutfit(it) }
        }
        for (id in EXCHANGEABLE) onOpNpcU(sailor, objects.getValue(id)) { exchange(id) }
        onOpNpc3(sailor) {
            choose(EXCHANGEABLE, "Trade which item for 25 soaked pages?")?.let { exchange(it) }
        }
        onOpNpc1(sailor) {
            mes(
                "I trade 25 soaked pages for an empty tome of water, an empty fish barrel, a tackle box or a big harpoonfish."
            )
        }
        onOpLoc1(locs.getValue(41314)) {
            ifOpenMain(SeaUi.stats)
            ifSetText(SeaBoard.best, vars[SeaVars.best].toString())
            ifSetText(SeaBoard.claimed, vars[SeaVars.searched].toString())
            ifSetText(SeaBoard.wins, vars[SeaVars.kills].toString())
            ifSetText(SeaBoard.time, time(vars[SeaVars.fastest]))
            ifSetText(SeaBoard.worldWins, voyages.subdued.toString())
            ifSetText(SeaBoard.worldTime, time(voyages.fastest))
        }
    }

    private fun ProtectedAccess.takeNet() {
        if (!player.members) return
        if (objs.small_fishing_net in inv) mes("You already have a small fishing net.")
        else if (!invAdd(inv, objs.small_fishing_net).success) mes("You need more inventory space.")
    }

    private fun ProtectedAccess.report() {
        mes("You have ${vars[SeaBits.permits]} reward permits.")
    }

    private suspend fun ProtectedAccess.search(batch: Boolean) {
        if (!player.members) return
        if (vars[SeaBits.permits] <= 0) {
            report()
            return
        }
        if (objs.small_fishing_net !in inv && objs.big_fishing_net !in inv) {
            mes("You need a fishing net. The nearby Spirit Angler can give you one.")
            return
        }
        do {
            anim(if (objs.small_fishing_net in inv) seqs.human_smallnet else seqs.human_bignet)
            gatheringDelay(3)
            if (objs.small_fishing_net !in inv && objs.big_fishing_net !in inv) return
            repeat(minOf(if (batch) 5 else 1, vars[SeaBits.permits])) {
                val roll = TemporossTreasure.pool(random, objects, player.statBase(stats.fishing))
                val owned =
                    when (roll.id) {
                        25582 -> listOf(25582, 25584, 25585, 25587)
                        25580 -> listOf(25580)
                        else -> emptyList()
                    }.any { id -> player.invMap.values.any { inv -> objects.getValue(id) in inv } }
                val prize = if (owned) InvObj(objects.getValue(25578), 25) else roll
                if (prize.id == 25602) pets.award(player, prize.id)
                else if (!invAddOrDrop(ground, objects[prize], prize.count))
                    mes("Your reward falls to the ground.")
                vars[SeaBits.permits]--
                if (vars[SeaVars.searched] < Int.MAX_VALUE) vars[SeaVars.searched]++
                FishingXp.advance(this, 10.0)
            }
        } while (!batch && vars[SeaBits.permits] > 0)
    }

    private fun ProtectedAccess.openCasket() {
        if (!player.members || objects.getValue(25590) !in inv) return
        val prize = TemporossTreasure.casket(random, objects)
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, 25590, 1)
                add(bag, prize.id, prize.count)
            }
        if (!result.success) {
            mes("You need space to open the casket.")
            return
        }
        val tier =
            when (random.of(100)) {
                in 0..4 -> ClueTier.Easy
                in 5..7 -> ClueTier.Medium
                in 8..9 -> ClueTier.Hard
                else -> return
            }
        val clue =
            clues.reward(clues.state(player, tier), tier) {
                steps.select(tier, random.of(steps.steps(tier).size))
            }
        clue.obj?.let { invAddOrDrop(ground, it) }
    }

    private suspend fun ProtectedAccess.choose(allowed: List<Int>, title: String): Int? {
        val items = allowed.filter { objects.getValue(it) in inv }
        if (items.isEmpty()) {
            mes("You aren't carrying anything to exchange.")
            return null
        }
        return items.getOrNull(
            menu(title, false, items.map { objects.getValue(it).name } + "Cancel")
        )
    }

    private suspend fun ProtectedAccess.exchange(id: Int) {
        if (!player.members || objects.getValue(id) !in inv) return
        if (
            id in listOf(25582, 25584) && barrel.count(player) > 0 ||
                id == 25580 && player.invMap[SeaStorage.tackle]?.isNotEmpty() == true
        ) {
            mes("Empty it before exchanging it.")
            return
        }
        if (!choice2("Trade for 25 soaked pages.", true, "Keep it.", false)) return
        val result =
            player.invTransaction(inv) {
                val b = select(inv)
                delete(b, id, 1)
                add(b, 25578, 25)
            }
        if (result.success) mes("You receive 25 soaked pages.")
    }

    private suspend fun ProtectedAccess.changeOutfit(id: Int) {
        if (!player.members || objects.getValue(id) !in inv) return
        val index = OUTFIT.indexOfFirst { it.first == id || it.second == id }
        val (base, upgraded) = OUTFIT[index]
        val bit = 1 shl index
        val price = if (id == base && (vars[SeaVars.upgrades] and bit) == 0) 1200 else 0
        val dest = if (id == base) upgraded else base
        if (price > 0 && invTotal(inv, objects.getValue(25588)) < price) {
            mes("You need 1,200 spirit flakes for this piece.")
            return
        }
        val cost = if (price > 0) " for 1,200 spirit flakes" else " for free"
        if (
            !choice2(
                "Change to ${objects.getValue(dest).name}${cost}.",
                true,
                "Keep this piece.",
                false,
            )
        )
            return
        val result =
            player.invTransaction(inv) {
                val b = select(inv)
                delete(b, id, 1)
                if (price > 0) delete(b, 25588, price)
                add(b, dest, 1)
            }
        if (result.success) vars[SeaVars.upgrades] = vars[SeaVars.upgrades] or bit
    }

    private companion object {
        val OUTFIT = listOf(13258 to 25592, 13259 to 25594, 13260 to 25596, 13261 to 25598)
        val EXCHANGEABLE = listOf(25582, 25584, 25580, 25576, 25559)

        fun time(cycles: Int): String {
            if (cycles <= 0) return "-"
            val seconds = cycles * 6L / 10
            return "${seconds/60}:${(seconds%60).toString().padStart(2,'0')}"
        }
    }
}
