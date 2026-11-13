package org.rsmod.content.minigames.giantsfoundry

import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.stat
import org.rsmod.game.entity.Player

internal data class FoundryMould(
    val part: Int,
    val index: Int,
    val struct: Int,
    val name: String,
    val level: Int,
    val cost: Int,
    val axes: List<Int>,
) {
    fun unlocked(player: Player): Boolean =
        cost == 0 || player.vars[FoundryBits.unlocked[part][5 - index]] != 0

    fun usable(player: Player): Boolean = unlocked(player) && player.stat(stats.smithing) >= level

    fun score(word: Int): Int {
        if (word !in 1..6) return 0
        val axis = axes[(word - 1) % 3]
        return (if (word <= 3) -axis else axis).coerceAtLeast(0)
    }
}

internal object FoundryMoulds {
    val all: List<FoundryMould> =
        requireNotNull(FoundryMoulds::class.java.getResourceAsStream("moulds.tsv"))
            .bufferedReader()
            .useLines { lines ->
                lines
                    .drop(1)
                    .map { line ->
                        val a = line.split('\t')
                        FoundryMould(
                            a[0].toInt(),
                            a[1].toInt(),
                            a[2].toInt(),
                            a[3],
                            a[4].toInt(),
                            a[5].toInt(),
                            a.drop(6).map(String::toInt),
                        )
                    }
                    .toList()
            }
    val words = listOf("", "Narrow", "Light", "Flat", "Broad", "Heavy", "Spiked")

    fun find(part: Int, index: Int) = all.firstOrNull { it.part == part && it.index == index }

    fun metalScore(counts: List<Int>): Int {
        val metals =
            counts
                .withIndex()
                .filter { it.value > 0 }
                .sortedWith(
                    compareByDescending<IndexedValue<Int>> { it.value }
                        .thenByDescending { it.index }
                )
                .take(2)
        val amounts = metals.map { 10 * (it.index + 1) * it.value }
        val a = amounts.getOrElse(0) { 0 }
        val b = amounts.getOrElse(1) { 0 }
        return (10 * a / 28 + 10 * b / 28 + a * b / (28 * 28)) / 10
    }

    fun sections(metal: Int) =
        when (metal) {
            in 0..19 -> 3
            in 20..59 -> 4
            in 60..89 -> 5
            in 90..119 -> 6
            else -> 7
        }

    fun handInXp(quality: Int) = (quality * quality / 73 + quality * 3 / 2 + 1) * 30
}
