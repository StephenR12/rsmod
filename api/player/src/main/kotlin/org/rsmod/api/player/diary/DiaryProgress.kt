package org.rsmod.api.player.diary

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.type.varp.VarpType

public enum class DiaryDifficulty {
    Easy,
    Medium,
    Hard,
    Elite,
}

/** Client enum 595 indexes the clickable area_task rows independently of their display order. */
public enum class DiaryRegion(public val journalIndex: Int, public val title: String) {
    Karamja(0, "Karamja Area Tasks"),
    Ardougne(1, "Ardougne Area Tasks"),
    Falador(2, "Falador Area Tasks"),
    Fremennik(3, "Fremennik Tasks"),
    Kandarin(4, "Kandarin Tasks"),
    Desert(5, "Desert Tasks"),
    Lumbridge(6, "Lumbridge & Draynor Tasks"),
    Morytania(7, "Morytania Tasks"),
    Varrock(8, "Varrock Tasks"),
    Wilderness(9, "Wilderness Area Tasks"),
    Western(10, "Western Area Tasks"),
    Kourend(11, "Kourend & Kebos Tasks");

    internal val key: String
        get() = name.lowercase()

    public fun tasks(difficulty: DiaryDifficulty): List<DiaryEntry> =
        when (this) {
            Karamja -> karamjaTasks
            Ardougne -> ardougneTasks
            Falador -> faladorTasks
            Fremennik -> fremennikTasks
            Kandarin -> kandarinTasks
            Desert -> desertTasks
            Lumbridge -> lumbridgeTasks
            Morytania -> morytaniaTasks
            Varrock -> varrockTasks
            Wilderness -> wildernessTasks
            Western -> westernTasks
            Kourend -> kourendTasks
        }.getValue(difficulty)

    public fun tier(difficulty: DiaryDifficulty): DiaryTierVars =
        DiaryBits.tiers.getValue(this to difficulty)
}

public data class DiaryTierVars(
    val count: VarBitType,
    val complete: VarBitType,
    val reward: VarBitType,
)

public sealed class DiaryEntry(public val text: String) {
    public abstract fun isComplete(player: Player): Boolean

    internal abstract fun markComplete(player: Player)

    internal class Flag(val page: VarpType, bit: Int, text: String) : DiaryEntry(text) {
        private val mask = 1 shl bit

        override fun isComplete(player: Player): Boolean = player.vars[page] and mask != 0

        override fun markComplete(player: Player) {
            VarPlayerIntMapSetter.set(player, page, player.vars[page] or mask)
        }
    }

    internal class Counter(val varbit: VarBitType, val target: Int, text: String) :
        DiaryEntry(text) {
        override fun isComplete(player: Player): Boolean = player.vars[varbit] >= target

        override fun markComplete(player: Player) {
            VarPlayerIntMapSetter.set(player, varbit, target)
        }
    }
}

/** Rebuild derived state for saves that predate task counts, preserving existing unlocks. */
public fun Player.syncDiaryTier(region: DiaryRegion, difficulty: DiaryDifficulty) {
    val tasks = region.tasks(difficulty)
    val tier = region.tier(difficulty)
    val completed = tasks.count { it.isComplete(this) }
    if (completed != vars[tier.count]) VarPlayerIntMapSetter.set(this, tier.count, completed)
    if (completed > 0 && vars[DiaryBits.started.getValue(region)] == 0) {
        VarPlayerIntMapSetter.set(this, DiaryBits.started.getValue(region), 1)
    }
    if (completed == tasks.size) {
        if (vars[tier.complete] == 0) VarPlayerIntMapSetter.set(this, tier.complete, 1)
        if (region == DiaryRegion.Wilderness && vars[DiaryBits.wildernessAnyComplete] == 0) {
            VarPlayerIntMapSetter.set(this, DiaryBits.wildernessAnyComplete, 1)
        }
    }
}

public fun Player.completeDiaryTask(task: DiaryTask) {
    if (!members) return
    val entry = task.region.tasks(task.difficulty)[task.number - 1]
    if (entry.isComplete(this)) return
    interruptDiaryKillSequence()
    interruptDiaryProductionSequence(task)
    entry.markComplete(this)
    syncDiaryTier(task.region, task.difficulty)
    mes("Achievement Diary task complete: ${entry.text}")
}

public fun Player.hasCompletedDiaryTask(task: DiaryTask): Boolean =
    task.region.tasks(task.difficulty)[task.number - 1].isComplete(this)

/** Advances one of Karamja's original saved quantities, including progress on imported saves. */
public fun Player.advanceKaramjaDiaryCounter(task: DiaryTask, amount: Int = 1) {
    require(task.region == DiaryRegion.Karamja && amount > 0)
    val entry = task.region.tasks(task.difficulty)[task.number - 1] as DiaryEntry.Counter
    if (!members || entry.isComplete(this)) return
    val remaining = entry.target - vars[entry.varbit]
    if (amount >= remaining) completeDiaryTask(task)
    else {
        interruptDiaryProductionSequence(task)
        VarPlayerIntMapSetter.set(this,entry.varbit,vars[entry.varbit] + amount)
    }
}
