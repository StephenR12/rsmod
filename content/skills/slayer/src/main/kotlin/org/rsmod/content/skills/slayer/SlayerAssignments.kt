package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.npc.SlayerLocationVars
import org.rsmod.api.combat.commons.npc.inSlayerTower
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.combat.commons.npc.slayerSuperiorOwner
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.back
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.dbrow.UnpackedDbRowType
import org.rsmod.game.type.stat.StatTypeList

object SlayerVarBits : VarBitReferences() {
    val master = find("slayer_master")
    val focus = find("slayer_master_in_focus")
    val boss = find("slayer_target_bossid")
    val combatRewards =
        listOf("easy", "medium", "hard", "elite", "master", "grandmaster").map {
            find("ca_tier_status_$it")
        }
    val toggleOff =
        mapOf(
            35 to find("slayer_toggleoff_superiormobs"),
            43 to find("slayer_toggleoff_fossilwyvernblock"),
            53 to find("slayer_toggleoff_longerrevenants"),
        )
    val streak = find("slayer_tasks_completed")
    val wildernessStreak = find("slayer_wilderness_tasks_completed")
    val points = find("slayer_points")
    val porcine = find("porcine")
    internal val blockLists = Master.entries.associateWith { blocks(it.blockList) }

    private fun blocks(name: String) =
        (1..6).map { find("slayer_blocked_${name}_$it") } + find("slayer_blocked_${name}_diary")
}

object SlayerVarps : VarpReferences() {
    val capeHistory = find("slayer_cape_history")
    val contacts = find("slayer_master_contacts")
    val originalCount = find("slayer_count_original")
    val unlocks = listOf(find("slayer_rewards_unlocks"), find("slayer_rewards_unlocks1"))
    val lostCity = find("zanaris")
    val shiloVillage = find("zombiequeen")
}

private var Player.capeHistory by intVarp(SlayerVarps.capeHistory)
internal var Player.slayerArea by intVarp(SlayerLocationVars.area)
internal var Player.slayerContacts by intVarp(SlayerVarps.contacts)

internal var Player.slayerBoss by intVarBit(SlayerVarBits.boss)
var Player.slayerMaster by intVarBit(SlayerVarBits.master)
var Player.wildernessSlayerStreak by intVarBit(SlayerVarBits.wildernessStreak)
var Player.slayerStreak by intVarBit(SlayerVarBits.streak)
var Player.slayerPoints by intVarBit(SlayerVarBits.points)
var Player.slayerTarget by intVarp(varps.slayer_target)
var Player.slayerCount by intVarp(varps.slayer_count)
var Player.originalSlayerCount by intVarp(SlayerVarps.originalCount)

public enum class Master(val id: Int, val combat: Int, val points: Int, val blockList: String) {
    Turael(1, 0, 0, "turael"),
    Mazchna(2, 20, 6, "mazchna"),
    Vannaka(3, 40, 8, "vannaka"),
    Chaeldar(4, 70, 10, "chaeldar"),
    Duradel(5, 100, 15, "duradel"),
    Nieve(6, 85, 12, "nieve"),
    Krystilia(7, 0, 25, "krystilia"),
    Konar(8, 75, 18, "konar"),
    Spria(9, 0, 0, "turael");

    val blockCost: Int
        get() =
            when (this) {
                Turael,
                Spria -> 40
                Mazchna -> 50
                Vannaka -> 60
                Chaeldar -> 70
                Nieve -> 90
                Konar -> 80
                Krystilia,
                Duradel -> 100
            }

    fun accessible(player: Player): Boolean =
        when (this) {
            Mazchna -> player.vars[varps.priest_in_peril] >= 60
            Chaeldar -> player.vars[SlayerVarps.lostCity] >= 6
            Duradel ->
                player.vars[SlayerVarps.shiloVillage] >= 15 && player.statBase(stats.slayer) >= 50
            Spria -> player.vars[SlayerVarBits.porcine] >= 40
            else -> true
        }
}

@Singleton
internal class SlayerAssignments
@Inject
constructor(
    rows: DbRowTypeList,
    private val statTypes: StatTypeList,
    private val random: GameRandom,
    private val questAccess: SlayerQuestAccess,
    private val superior: SuperiorSlayer,
    private val drops: RegionalSlayerDrops,
) {
    private val targets =
        rows.values.filter { it.table == 113 && it.data[9] != null }.associateBy { it.int(0) }
    private val bossRows = rows.values.filter { it.table == 116 }.associateBy { it.int(1) }
    private val areas = rows.values.filter { it.table == 115 }.associateBy { it.int(0) }
    private val offers =
        rows.values.filter { it.table == 114 }.sortedBy { it.id }.groupBy { it.int(0) }
    private val rowsById = rows

    fun canReplace(player: Player, master: Master): Boolean {
        if (!player.members || master != Master.Turael || player.slayerCount <= 0) return false
        if (player.slayerMaster == Master.Turael.id || player.slayerMaster == Master.Spria.id)
            return false
        return choices(player, master).none {
            rowsById.getValue(it.int(1)).int(0) == player.slayerTarget
        }
    }

    // Store the last assignment independently of the active task. A cancelled assignment
    // remains repeatable. Remember the roll so closing the confirmation cannot reroll it.
    private fun hasCape(player: Player): Boolean =
        player.statBase(stats.slayer) >= 99 &&
            listOf(
                    objs.slayer_skillcape,
                    objs.slayer_skillcape_t,
                    objs.max_cape_inv,
                    objs.max_cape_worn,
                )
                .any { player.back?.id == it.id || it in player.inv }

    private fun previousOffer(
        player: Player,
        master: Master,
        choices: List<UnpackedDbRowType>,
    ): UnpackedDbRowType? {
        val history = player.capeHistory
        if ((history ushr 16 and 15) != master.id || !hasCape(player)) return null
        return choices.firstOrNull { offer ->
            val id = rowsById.getValue(offer.int(1)).int(0)
            id == (history and 255) &&
                (master != Master.Konar ||
                    id == 98 ||
                    locations(player, offer).any { it.int(0) == (history ushr 8 and 255) })
        }
    }

    fun offerRepeat(player: Player, master: Master): String? {
        if (
            !player.members ||
                player.slayerCount > 0 ||
                !master.accessible(player) ||
                !hasCape(player)
        )
            return null
        val offer = previousOffer(player, master, choices(player, master)) ?: return null
        if (player.capeHistory ushr 20 == 0) {
            player.capeHistory = player.capeHistory or ((if (random.of(10) == 0) 1 else 2) shl 20)
        }
        if (player.capeHistory ushr 20 != 1) return null
        return rowsById.getValue(offer.int(1)).name()
    }

    fun request(
        player: Player,
        master: Master,
        replace: Boolean = false,
        repeatLast: Boolean = false,
    ): Boolean {
        if (!player.members) {
            player.mes("You need to be on a members' world to train Slayer.")
            return false
        }
        if (player.slayerCount > 0 && (!replace || !canReplace(player, master))) {
            checkTask(player)
            return false
        }
        if (player.combatLevel < master.combat && player.statBase(stats.slayer) < 99) {
            player.mes(
                "You need a combat level of ${master.combat} to receive an assignment from ${master.name}."
            )
            return false
        }
        if (!master.accessible(player)) {
            player.mes("You do not yet meet the requirements for this Slayer master.")
            return false
        }
        val choices = choices(player, master)
        if (choices.isEmpty()) {
            player.mes("I have no suitable assignment for you.")
            return false
        }
        val repeat =
            if (repeatLast && player.capeHistory ushr 20 == 1)
                previousOffer(player, master, choices)
            else null
        val offer =
            repeat
                ?: run {
                    var roll = random.of(choices.sumOf { it.int(2) })
                    choices.first {
                        roll -= it.int(2)
                        roll < 0
                    }
                }
        val target = rowsById.getValue(offer.int(1))
        val boss =
            if (target.int(0) == 98) {
                val candidates = bossesFor(player, master)
                candidates[random.of(candidates.size)].int(1)
            } else 0
        val extension = target.data[15]
        val range =
            if (extension != null && unlocked(player, extension[0] as Int)) {
                (extension[1] as Int)..(extension[2] as Int)
            } else {
                offer.int(3)..offer.int(4)
            }
        val count = if (boss != 0) 3 else random.of(range)
        val area =
            if (boss == 0 && master == Master.Konar) {
                val available = locations(player, offer)
                if (repeat != null) player.capeHistory ushr 8 and 255
                else available[random.of(available.size)].int(0)
            } else 0
        if (replace && player.slayerCount > 0) {
            if (player.slayerMaster == 7) player.wildernessSlayerStreak = 0
            else player.slayerStreak = 0
        }
        player.capeHistory = (master.id shl 16) or (area shl 8) or target.int(0)
        player.slayerBoss = boss
        player.slayerArea = area
        if (master == Master.Krystilia) player.slayerContacts = player.slayerContacts or 1
        player.slayerMaster = master.id
        player.slayerTarget = target.int(0)
        player.originalSlayerCount = count
        player.slayerCount = count
        if (boss == 0)
            player.mes("Your new task is to kill $count ${target.name()}${locationText(player)}.")
        else player.mes("Your new assignment is ${bossName(boss)}.")
        return true
    }

    private fun choices(player: Player, master: Master): List<UnpackedDbRowType> {
        val blocks = SlayerVarBits.blockLists.getValue(master)
        val blocked = blocks.map { player.vars[it] }
        return offers.getValue(master.id).filter { offer ->
            val target = rowsById.getValue(offer.int(1))
            val id = target.int(0)
            id !in blocked &&
                (id != 98 || bossesFor(player, master).isNotEmpty()) &&
                (if (master == Master.Krystilia) questAccess.canReceiveWild(player, id)
                else questAccess.canReceive(player, id)) &&
                eligible(player, target, master) &&
                (master != Master.Konar || id == 98 || locations(player, offer).isNotEmpty()) &&
                (offer.data[6] == null || unlocked(player, offer.int(6))) &&
                (target.data[16] == null || !unlocked(player, target.int(16)))
        }
    }

    private fun unlocked(player: Player, rowId: Int): Boolean {
        val bit = rowsById.getValue(rowId).int(0)
        if (SlayerVarBits.toggleOff[bit]?.let { player.vars[it] != 0 } == true) return false
        val page = SlayerVarps.unlocks.getOrNull(bit / 32) ?: return false
        return player.vars[page] and (1 shl (bit % 32)) != 0
    }

    private fun bossesFor(player: Player, master: Master) =
        bossRows.values.filter { row ->
            val boss = row.int(1)
            (master != Master.Krystilia || boss in 9..15) &&
                (boss != 24 || master == Master.Konar) &&
                questAccess.canReceiveBoss(player, boss) &&
                eligible(player, rowsById.getValue(row.int(4)), master)
        }

    private fun bossName(id: Int): String? =
        bossRows[id]?.let { row ->
            rowsById.getValue(row.int(4)).data.getValue(9).first() as String
        }

    internal fun maximumBossCount(player: Player): Int =
        (if (player.slayerBoss == 17) 36 else 35) +
            5 * SlayerVarBits.combatRewards.count { player.vars[it] >= 2 }

    private fun locations(player: Player, offer: UnpackedDbRowType) =
        offer.data[5]
            .orEmpty()
            .map { rowsById.getValue(it as Int) }
            .filter { questAccess.canEnterArea(player, it.int(0)) }

    private fun locationText(player: Player): String =
        when (player.slayerMaster) {
            Master.Krystilia.id -> " in the Wilderness"
            Master.Konar.id ->
                areas[player.vars[SlayerLocationVars.area]]
                    ?.data
                    ?.get(2)
                    ?.firstOrNull()
                    ?.let { " $it" }
                    .orEmpty()
            else -> ""
        }

    private fun eligible(player: Player, target: UnpackedDbRowType, master: Master): Boolean {
        if (
            master != Master.Krystilia &&
                player.combatLevel < (target.data[1]?.first() as? Int ?: 0) &&
                !hasCape(player)
        )
            return false
        fun meets(pair: List<Any>): Boolean =
            player.statBase(statTypes.getValue(pair[1] as Int)) >= pair[0] as Int
        val all = target.data[2]?.chunked(2).orEmpty()
        val any = target.data[3]?.chunked(2).orEmpty()
        return all.all(::meets) && (any.isEmpty() || any.any(::meets))
    }

    fun checkTask(player: Player) {
        val name =
            if (player.slayerTarget == 98) bossName(player.slayerBoss)
            else targets[player.slayerTarget]?.name()
        if (player.slayerCount > 0 && name != null) {
            player.mes(
                "You're assigned to kill $name${locationText(player)}; only ${player.slayerCount} more to go."
            )
        } else {
            player.mes("You need something new to hunt. Talk to a Slayer master.")
        }
    }

    fun credit(player: Player, npc: Npc, share: Double = 1.0, rollDrops: Boolean = true) {
        if (!player.members) return
        val assigned = npc.isAssignedSlayerTarget(player)
        val owner = npc.slayerSuperiorOwner
        if (owner != null) {
            if (owner != player.uid) return
            superior.reward(player, npc)
        } else {
            if (!assigned) return
            if (rollDrops) {
                superior.trySpawn(player, npc)
                drops.roll(player, npc, player.slayerMaster, superior = false)
            }
        }
        val fineXp = npc.visType.paramOrNull(params.slayer_experience)
        val baseXp = fineXp?.div(10.0) ?: npc.baseHitpointsLvl.toDouble()
        val diaryTiers =
            if (npc.spawnCoords.inSlayerTower()) {
                DiaryDifficulty.entries.count {
                    player.vars[DiaryRegion.Morytania.tier(it).complete] != 0
                }
            } else 0
        player.statAdvance(stats.slayer, baseXp * (1.0 + 0.025 * diaryTiers) * share)
        if (!assigned) return
        val master = Master.entries.firstOrNull { it.id == player.slayerMaster } ?: return
        player.slayerCount--
        if (player.slayerCount > 0) return

        if (player.slayerTarget == 98) player.statAdvance(stats.slayer, 5000.0)
        player.slayerBoss = 0
        val wilderness = master == Master.Krystilia
        val streak = (if (wilderness) player.wildernessSlayerStreak else player.slayerStreak) + 1
        val westernElite = DiaryRegion.Western.tier(DiaryDifficulty.Elite).complete
        val base =
            when {
                master == Master.Nieve && player.vars[westernElite] != 0 -> 15
                master == Master.Konar &&
                    player.vars[DiaryRegion.Kourend.tier(DiaryDifficulty.Elite).complete] != 0 -> 20
                else -> master.points
            }
        val points = base * milestone(streak)
        if (wilderness)
            player.wildernessSlayerStreak =
                streak.coerceAtMost((1 shl SlayerVarBits.wildernessStreak.bits.count()) - 1)
        else
            player.slayerStreak = streak.coerceAtMost((1 shl SlayerVarBits.streak.bits.count()) - 1)
        player.slayerPoints =
            (player.slayerPoints + points).coerceAtMost(
                (1 shl SlayerVarBits.points.bits.count()) - 1
            )
        player.slayerArea = 0
        player.slayerTarget = 0
        player.originalSlayerCount = 0
        player.mes(
            "You've completed your task! You have completed $streak tasks in a row and received $points points."
        )
    }

    private fun UnpackedDbRowType.int(column: Int) = data.getValue(column).first() as Int

    private fun UnpackedDbRowType.name() = (data.getValue(9).first() as String).lowercase()

    private companion object {
        fun milestone(streak: Int): Int =
            when {
                streak < 5 -> 0
                streak % 1000 == 0 -> 50
                streak % 250 == 0 -> 35
                streak % 100 == 0 -> 25
                streak % 50 == 0 -> 15
                streak % 10 == 0 -> 5
                else -> 1
            }
    }
}
