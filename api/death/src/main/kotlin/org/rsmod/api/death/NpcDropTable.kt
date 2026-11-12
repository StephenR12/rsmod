package org.rsmod.api.death

import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

/** Immutable inputs available while resolving one NPC death table. */
public data class NpcDropContext(
    val npc: Npc,
    val hero: Player,
    val coords: CoordGrid,
    val random: GameRandom,
)

/** A fully resolved ground-item spawn produced by an [NpcDropTable]. */
public data class NpcDrop(
    val obj: InvObj,
    val coords: CoordGrid,
    val receiver: Player?,
    val duration: Int? = null,
    val reveal: Int? = null,
) {
    init {
        require(obj.count > 0) { "Drop count must be positive: $obj" }
        require(duration == null || duration > 0) { "Drop duration must be positive: $duration" }
        require(reveal == null || reveal >= 0) { "Drop reveal delay must be non-negative: $reveal" }
    }
}

/**
 * Result of one composable drop roll.
 *
 * [selected] is distinct from whether an item was emitted. For example, a weighted `nothing` entry
 * is selected but emits no item. This distinction lets [NpcDropRolls.firstSelected] model ordered
 * pre-rolls that suppress the main table even when the selected entry is empty.
 */
public data class NpcDropRollResult(
    val selected: Boolean,
    val drops: List<NpcDrop>,
    val messages: List<String> = emptyList(),
) {
    public companion object {
        public val Miss: NpcDropRollResult = NpcDropRollResult(selected = false, emptyList())

        public val SelectedNothing: NpcDropRollResult =
            NpcDropRollResult(selected = true, emptyList())
    }
}

/** One deterministic, composable operation in an NPC drop table. */
public fun interface NpcDropRoll {
    public fun roll(context: NpcDropContext): NpcDropRollResult
}

/** Root roll for one NPC or shared NPC-family drop table. */
public class NpcDropTable(public val root: NpcDropRoll) {
    public fun resolve(context: NpcDropContext): NpcDropRollResult = root.roll(context)

    public fun roll(context: NpcDropContext): List<NpcDrop> = resolve(context).drops
}

/** A weighted branch in [NpcDropRolls.weighted]. */
public data class NpcWeightedDrop(val weight: Int, val roll: NpcDropRoll) {
    init {
        require(weight > 0) { "Drop weight must be positive: $weight" }
    }
}

/**
 * Standard building blocks for cache-revision-specific NPC drop tables.
 *
 * These primitives deliberately separate the reusable roll engine from content data. Tables can
 * compose guaranteed items, explicit nothing entries, weighted main tables, independent tertiary
 * rolls, ordered pre-rolls, conditions, repeated rolls and nested shared tables without embedding
 * any NPC-specific approximation in the engine.
 */
public object NpcDropRolls {
    public fun item(
        type: ObjType,
        count: IntRange = 1..1,
        receiver: (NpcDropContext) -> Player? = { it.hero },
        placement: (NpcDropContext) -> CoordGrid = { it.coords },
        duration: Int? = null,
        reveal: Int? = null,
    ): NpcDropRoll {
        require(!count.isEmpty() && count.first > 0) { "Drop count range must be positive: $count" }
        require(duration == null || duration > 0) { "Drop duration must be positive: $duration" }
        require(reveal == null || reveal >= 0) { "Drop reveal delay must be non-negative: $reveal" }
        return NpcDropRoll { context ->
            val quantity = if (count.first == count.last) count.first else context.random.of(count)
            val drop =
                NpcDrop(
                    obj = InvObj(type, quantity),
                    coords = placement(context),
                    receiver = receiver(context),
                    duration = duration,
                    reveal = reveal,
                )
            NpcDropRollResult(selected = true, listOf(drop))
        }
    }

    /** A selected branch that intentionally emits no item. */
    public fun nothing(): NpcDropRoll = NpcDropRoll { NpcDropRollResult.SelectedNothing }

    /** An unselected branch, useful as a conditional or ordered-roll fallback. */
    public fun miss(): NpcDropRoll = NpcDropRoll { NpcDropRollResult.Miss }

    /** Runs every child in order and concatenates their emitted drops. */
    public fun sequence(vararg rolls: NpcDropRoll): NpcDropRoll = sequence(rolls.asList())

    /** Runs every child in order and concatenates their emitted drops. */
    public fun sequence(rolls: List<NpcDropRoll>): NpcDropRoll = NpcDropRoll { context ->
        var selected = false
        val messages = mutableListOf<String>()
        val drops = buildList {
            for (roll in rolls) {
                val result = roll.roll(context)
                selected = selected || result.selected
                addAll(result.drops)
                messages += result.messages
            }
        }
        NpcDropRollResult(selected, drops, messages)
    }

    /** Selects exactly one weighted branch, including explicit [nothing] branches. */
    public fun weighted(vararg entries: NpcWeightedDrop): NpcDropRoll = weighted(entries.asList())

    /** Selects exactly one weighted branch, including explicit [nothing] branches. */
    public fun weighted(entries: List<NpcWeightedDrop>): NpcDropRoll {
        require(entries.isNotEmpty()) { "Weighted drop table must contain at least one entry." }
        val totalWeight = entries.sumOf { it.weight.toLong() }
        require(totalWeight <= Int.MAX_VALUE) {
            "Weighted drop table exceeds the supported total weight: $totalWeight"
        }
        return NpcDropRoll { context ->
            val value = context.random.of(totalWeight.toInt())
            var upperBound = 0
            val selected =
                entries.first { entry ->
                    upperBound += entry.weight
                    value < upperBound
                }
            val result = selected.roll.roll(context)
            result.copy(selected = true)
        }
    }

    /** Performs an independent [numerator]-in-[denominator] chance roll. */
    public fun independent(numerator: Int = 1, denominator: Int, roll: NpcDropRoll): NpcDropRoll {
        require(denominator > 0) { "Drop denominator must be positive: $denominator" }
        require(numerator in 1..denominator) {
            "Drop numerator must be in 1..$denominator: $numerator"
        }
        return NpcDropRoll { context ->
            if (context.random.of(denominator) >= numerator) {
                NpcDropRollResult.Miss
            } else {
                roll.roll(context).copy(selected = true)
            }
        }
    }

    /** Runs children in order and stops after the first selected branch. */
    public fun firstSelected(vararg rolls: NpcDropRoll): NpcDropRoll = firstSelected(rolls.asList())

    /** Runs children in order and stops after the first selected branch. */
    public fun firstSelected(rolls: List<NpcDropRoll>): NpcDropRoll = NpcDropRoll { context ->
        for (roll in rolls) {
            val result = roll.roll(context)
            if (result.selected) {
                return@NpcDropRoll result
            }
        }
        NpcDropRollResult.Miss
    }

    /** Runs [roll] only when [predicate] accepts the current death context. */
    public fun conditional(predicate: (NpcDropContext) -> Boolean, roll: NpcDropRoll): NpcDropRoll =
        NpcDropRoll { context ->
            if (predicate(context)) roll.roll(context) else NpcDropRollResult.Miss
        }

    /** Runs the same child a fixed number of times, preserving roll and output order. */
    public fun repeated(times: Int, roll: NpcDropRoll): NpcDropRoll {
        require(times > 0) { "Drop roll count must be positive: $times" }
        return sequence(List(times) { roll })
    }
}
