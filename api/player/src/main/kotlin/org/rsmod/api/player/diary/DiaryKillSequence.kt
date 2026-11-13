package org.rsmod.api.player.diary

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varp.VarpType

internal object CombatDiaryVars : VarpReferences() {
    val steps: VarpType = find("diary_combat_steps")
}

internal object CombatDiaryVarBuilder : VarpBuilder() {
    init {
        build("diary_combat_steps") { transmitNever = true }
    }
}

public enum class DiaryKillSequence(
    internal val task: DiaryTask,
    internal val shift: Int,
    internal val size: Int,
) {
    RockCrabs(DiaryTask.FremennikRockCrabs, 0, 5),
    Kings(DiaryTask.DagannothKings, 3, 3),
    Generals(DiaryTask.GodWarsGenerals, 6, 4),
    Elementals(DiaryTask.WorkshopElementals, 10, 4),
    WildernessHard(DiaryTask.WildernessHardBosses, 14, 3),
    WildernessElite(DiaryTask.WildernessEliteBosses, 17, 3);

    internal val mask: Int = if (this.task == DiaryTask.FremennikRockCrabs) 7 else (1 shl size) - 1
}

/** A different diary entry interrupts both Wilderness boss sequences, including partial updates. */
public fun Player.interruptDiaryKillSequence() {
    val steps = vars[CombatDiaryVars.steps]
    val updated = steps and WILDERNESS_BITS.inv()
    if (steps != updated) VarPlayerIntMapSetter.set(this, CombatDiaryVars.steps, updated)
}

public fun Player.recordDiaryKill(sequence: DiaryKillSequence, part: Int = 0) {
    if (!members || hasCompletedDiaryTask(sequence.task)) return
    require(part in 0 until sequence.size)
    val steps = vars[CombatDiaryVars.steps]
    val previous = (steps ushr sequence.shift) and sequence.mask
    val countKills = sequence == DiaryKillSequence.RockCrabs
    val next = if (countKills) previous + 1 else previous or (1 shl part)
    if (previous == next) return

    // Preserve the current sequence's own progress when removing interrupted Wilderness steps.
    val cleared = steps and WILDERNESS_BITS.inv() and (sequence.mask shl sequence.shift).inv()
    val count = if (countKills) next else next.countOneBits()
    if (count >= sequence.size) {
        VarPlayerIntMapSetter.set(this, CombatDiaryVars.steps, cleared)
        completeDiaryTask(sequence.task)
    } else {
        VarPlayerIntMapSetter.set(this, CombatDiaryVars.steps, cleared or (next shl sequence.shift))
        val entry = sequence.task.region.tasks(sequence.task.difficulty)[sequence.task.number - 1]
        mes("Achievement Diary task updated: ${entry.text} ($count/${sequence.size})")
    }
}

private val WILDERNESS_BITS = (7 shl DiaryKillSequence.WildernessHard.shift) or
    (7 shl DiaryKillSequence.WildernessElite.shift)
