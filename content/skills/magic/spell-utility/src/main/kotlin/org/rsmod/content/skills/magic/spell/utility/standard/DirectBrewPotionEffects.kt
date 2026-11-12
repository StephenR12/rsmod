package org.rsmod.content.skills.magic.spell.utility.standard

import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType

/** Cache-233 immediate effects for brew families rejected by both Lunar potion-share spells. */
public object DirectBrewPotionEffects {
    public fun apply(player: Player, family: DirectBrewPotionFamily) {
        when (family) {
            DirectBrewPotionFamily.Saradomin -> applySaradomin(player)
            DirectBrewPotionFamily.Zamorak -> applyZamorak(player)
            DirectBrewPotionFamily.Ancient -> applyAncient(player)
            DirectBrewPotionFamily.Forgotten -> applyForgotten(player)
        }
    }

    private fun applySaradomin(player: Player) {
        player.statBoost(stats.hitpoints, constant = 2, percent = 15)
        player.statBoost(stats.defence, constant = 2, percent = 20)
        player.drainFromCurrent(stats.attack, constant = 2, percent = 10)
        player.drainFromCurrent(stats.strength, constant = 2, percent = 10)
        player.drainFromCurrent(stats.ranged, constant = 2, percent = 10)
        player.drainFromCurrent(stats.magic, constant = 2, percent = 10)
    }

    private fun applyZamorak(player: Player) {
        applyZamorakNonHitpoints(player)
        player.drainFromCurrent(stats.hitpoints, constant = 0, percent = 12, minimum = 1)
    }

    /** Applies the dated Wiki's pre-heal damage for a cache-233 Zamorak mix. */
    public fun applyZamorakMix(player: Player) {
        applyZamorakNonHitpoints(player)
        val current = player.stat(stats.hitpoints)
        val damage = (current - 1).coerceAtLeast(0) / 8
        if (damage > 0) {
            player.statSub(stats.hitpoints, constant = damage, percent = 0)
        }
    }

    private fun applyZamorakNonHitpoints(player: Player) {
        player.statBoost(stats.attack, constant = 2, percent = 20)
        player.statBoost(stats.strength, constant = 2, percent = 12)
        player.drainFromCurrent(stats.defence, constant = 2, percent = 10)

        val prayerRestore = player.stat(stats.prayer) / 10
        player.addCapped(stats.prayer, prayerRestore, player.statBase(stats.prayer))
    }

    private fun applyAncient(player: Player) {
        player.statBoost(stats.magic, constant = 2, percent = 5)
        applyAncientPrayerAndDrains(player)
    }

    private fun applyForgotten(player: Player) {
        player.statBoost(stats.magic, constant = 3, percent = 8)
        applyAncientPrayerAndDrains(player)
    }

    private fun applyAncientPrayerAndDrains(player: Player) {
        player.drainFromCurrent(stats.attack, constant = 2, percent = 10)
        player.drainFromCurrent(stats.strength, constant = 2, percent = 10)
        player.drainFromCurrent(stats.defence, constant = 2, percent = 10)

        val basePrayer = player.statBase(stats.prayer)
        val prayerRestore = 2 + basePrayer / 10
        val prayerCap = basePrayer + basePrayer / 20
        player.addCapped(stats.prayer, prayerRestore, prayerCap)
    }

    private fun Player.drainFromCurrent(
        stat: StatType,
        constant: Int,
        percent: Int,
        minimum: Int = 0,
    ) {
        val current = stat(stat)
        val drain = constant + current * percent / 100
        val applied = min(drain, (current - minimum).coerceAtLeast(0))
        if (applied > 0) {
            statSub(stat, constant = applied, percent = 0)
        }
    }

    private fun Player.addCapped(stat: StatType, amount: Int, cap: Int) {
        val current = stat(stat)
        val applied = min(amount, (cap - current).coerceAtLeast(0))
        if (applied > 0) {
            statAdd(stat, constant = applied, percent = 0)
        }
    }
}

public enum class DirectBrewPotionFamily(private val doses: List<ObjType>) {
    Saradomin(
        listOf(
            objs.saradomin_brew_1,
            objs.saradomin_brew_2,
            objs.saradomin_brew_3,
            objs.saradomin_brew_4,
        )
    ),
    Zamorak(
        listOf(objs.zamorak_brew_1, objs.zamorak_brew_2, objs.zamorak_brew_3, objs.zamorak_brew_4)
    ),
    Ancient(
        listOf(objs.ancient_brew_1, objs.ancient_brew_2, objs.ancient_brew_3, objs.ancient_brew_4)
    ),
    Forgotten(
        listOf(
            objs.forgotten_brew_1,
            objs.forgotten_brew_2,
            objs.forgotten_brew_3,
            objs.forgotten_brew_4,
        )
    );

    public companion object {
        public fun forObj(objId: Int): DirectBrewPotionFamily? =
            entries.firstOrNull { family -> family.doses.any { it.id == objId } }
    }
}
