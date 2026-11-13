package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private enum class Chest {
    Brimstone,
    Small,
    Big,
}

internal object SlayerChestLocs : LocReferences() {
    val brimstone =
        listOf(find("brimstone_konar_chest_closed"), find("brimstone_konar_chest_parent"))
    val small = listOf(find("slayer_larran_chest_small_closed"), find("slayer_larran_small_chest"))
    val big = listOf(find("slayer_larran_chest_big_closed"), find("slayer_larran_big_chest"))
}

internal object SlayerChestCounters : VarpReferences() {
    val opened = listOf("brimstone", "larran_small", "larran_big").map { find("${it}_chest_count") }
}

internal object SlayerChestBuild : VarpBuilder() {
    init {
        for (name in listOf("brimstone", "larran_small", "larran_big")) build(
            "${name}_chest_count"
        ) {
            permanent = true
            transmitNever = true
        }
    }
}

private data class Entry(
    val symbol: String,
    val normalWeight: Int,
    val wildWeight: Int,
    val normal: IntRange,
    val small: IntRange,
    val big: IntRange,
)

internal object SlayerChestLoot : ObjReferences() {
    private val entries =
        listOf(
            Entry("cert_uncut_diamond", 5, 5, 25..35, 15..25, 35..45),
            Entry("cert_uncut_ruby", 5, 5, 25..35, 20..30, 35..45),
            Entry("cert_coal", 5, 5, 300..500, 282..480, 450..650),
            Entry("coins", 5, 3, 50000..150000, 40534..114792, 75000..175000),
            Entry("cert_gold_ore", 4, 4, 100..200, 81..179, 150..250),
            Entry("dragon_arrowheads", 4, 4, 50..200, 41..182, 100..250),
            Entry("cert_iron_ore", 3, 3, 350..500, 300..449, 500..650),
            Entry("cert_rune_full_helm", 3, 3, 2..4, 1..3, 3..5),
            Entry("cert_rune_platebody", 3, 3, 1..2, 1..2, 2..3),
            Entry("cert_rune_platelegs", 3, 3, 1..2, 1..2, 2..3),
            Entry("cert_runite_ore", 2, 2, 10..15, 5..10, 15..20),
            Entry("cert_steel_bar", 2, 2, 300..500, 253..450, 350..550),
            Entry("cert_magic_logs", 2, 2, 120..160, 80..120, 180..220),
            Entry("dragon_dart_tip", 2, 2, 40..160, 31..149, 80..200),
            Entry("palm_tree_seed", 1, 1, 2..4, 1..3, 3..5),
            Entry("magic_tree_seed", 1, 1, 2..3, 1..2, 3..4),
            Entry("celastrus_tree_seed", 1, 1, 2..4, 1..3, 3..5),
            Entry("dragonfruit_tree_seed", 1, 1, 2..4, 1..3, 3..5),
            Entry("redwood_tree_seed", 1, 1, 1..1, 1..1, 1..1),
            Entry("torstol_seed", 1, 1, 3..5, 2..4, 4..6),
            Entry("snapdragon_seed", 1, 1, 3..5, 2..4, 4..6),
            Entry("ranarr_seed", 1, 1, 3..5, 2..4, 4..6),
            Entry("cert_blankrune_high", 1, 3, 3000..6000, 3041..5989, 4500..7500),
        )
    private val objects = entries.associate { it.symbol to find(it.symbol) }

    fun main(random: GameRandom, index: Int, roll: Int): Pair<ObjType, Int> {
        var ticket = roll
        val entry =
            entries.first {
                ticket -= if (index == 0) it.normalWeight else it.wildWeight
                ticket < 0
            }
        val range =
            when (index) {
                0 -> entry.normal
                1 -> entry.small
                else -> entry.big
            }
        return objects.getValue(entry.symbol) to random.of(range)
    }
}

class RegionalSlayerChests
@Inject
constructor(
    private val random: GameRandom,
    private val types: ObjTypeList,
    private val players: PlayerRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val locations =
            listOf(SlayerChestLocs.brimstone, SlayerChestLocs.small, SlayerChestLocs.big)
        for (chest in Chest.entries) for (loc in locations[chest.ordinal]) {
            val key = if (chest == Chest.Brimstone) SlayerKeys.brimstone else SlayerKeys.larran
            onOpLoc1(loc) { open(chest, key) }
            onOpLocU(loc, key) { open(chest, key) }
            onOpLoc2(loc) {
                mes(
                    "You have opened this chest ${player.vars[SlayerChestCounters.opened[chest.ordinal]]} times."
                )
            }
        }
    }

    private fun ProtectedAccess.open(chest: Chest, key: ObjType) {
        if (!player.members || key !in inv) {
            mes("The chest is locked.")
            return
        }
        val (item, quantity) = roll(chest, player.statBase(stats.fishing))
        val result =
            player.invTransaction(inv) {
                val inventory = select(inv)
                delete(inventory, key.id, 1)
                add(inventory, item.id, quantity)
            }
        if (!result.success) {
            mes("You need more space in your inventory.")
            return
        }
        val counter = SlayerChestCounters.opened[chest.ordinal]
        VarPlayerIntMapSetter.set(
            player,
            counter,
            (player.vars[counter].toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        )
        val name = types.uncert(types[item]).name
        mes("You unlock the chest and find ${quantity} x $name.")
        if (chest == Chest.Big) {
            players
                .findAll(ZoneKey.from(player.coords), 2)
                .filter { it != player && it.isWithinDistance(player, 15) }
                .forEach {
                    it.mes(
                        "${player.displayName} has opened Larran's chest and received ${quantity} x $name."
                    )
                }
        }
    }

    private fun roll(chest: Chest, fishing: Int): Pair<ObjType, Int> {
        if (chest == Chest.Brimstone && random.of(100) == 0) {
            val special = random.of(10)
            return types.getValue(if (special < 5) 22963 else 23047 + (special - 5) * 3) to 1
        }
        if (chest == Chest.Big) {
            val special = random.of(256)
            if (special < 3) return types.getValue(24288 + 3 * special) to 1
        }
        val ticket = random.of(60)
        if (ticket < 57) return SlayerChestLoot.main(random, chest.ordinal, ticket)
        return fish(chest, fishing)
    }

    private fun fish(chest: Chest, level: Int): Pair<ObjType, Int> {
        fun succeeds(low: Int, high: Int): Boolean {
            val chance = (low * (99 - level) + high * (level - 1)) / 98 + 1
            return random.of(256) < chance
        }
        var id =
            when {
                succeeds(-10, 20) -> 389
                succeeds(-10, 50) -> 395
                succeeds(-60, 40) -> 383
                succeeds(0, 170) -> 7944
                succeeds(25, 200) -> 371
                succeeds(160, 0) -> 359
                else -> 377
            }
        if (id == 389 || id == 395 || id == 383) {
            when (random.of(8)) {
                in 2..4 -> id = 383
                in 5..7 -> id = 30900
            }
        }
        val range =
            when (id) {
                389 -> 80..160
                395 -> 80..200
                383,
                30900 -> 80..250
                7944,
                371 -> 100..300
                else -> 100..350
            }
        val quantity = random.of(range) * (if (id == 30900) 2 else 1)
        val type = types.getValue(id)
        return (if (id == 30900) type else types.cert(type)) to
            (if (chest == Chest.Big) quantity * 3 / 2 else quantity)
    }
}
