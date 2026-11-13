package org.rsmod.content.bosses.zalcano

import jakarta.inject.Inject
import kotlin.math.ceil
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.skills.pets.SkillPets
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

internal data class Contribution(var armour: Int = 0, var health: Int = 0) {
    val points
        get() = health + 2 * armour

    val uniquePoints
        get() = health.coerceAtMost(400) + 2 * armour.coerceAtMost(300)

    val qualifies
        get() = armour >= 5

    val unique
        get() = qualifies && armour + health >= 31
}

internal class PrisonRewards
@Inject
constructor(
    private val random: GameRandom,
    private val ground: ObjRepository,
    private val types: ObjTypeList,
    private val pets: SkillPets,
) {
    private data class Drop(
        val item: ObjType,
        val weight: Int,
        val min: Int,
        val max: Int,
        val noted: Boolean = false,
    )

    private val table =
        listOf(
            Drop(objs.blood_rune, 1, 64, 480),
            Drop(objs.cosmic_rune, 1, 295, 926),
            Drop(objs.death_rune, 1, 201, 832),
            Drop(objs.law_rune, 1, 166, 790),
            Drop(objs.soul_rune, 1, 47, 392),
            Drop(objs.nature_rune, 1, 1, 937),
            Drop(objs.silver_ore, 3, 86, 800, true),
            Drop(objs.coal, 1, 169, 938, true),
            Drop(objs.mithril_ore, 2, 56, 469, true),
            Drop(objs.adamantite_ore, 2, 51, 343, true),
            Drop(objs.runite_ore, 2, 3, 27, true),
            Drop(objs.gold_ore, 3, 86, 756, true),
            Drop(objs.steel_bar, 3, 16, 699, true),
            Drop(objs.mithril_bar, 3, 53, 459, true),
            Drop(objs.adamantite_bar, 2, 13, 111, true),
            Drop(objs.runite_bar, 2, 3, 27, true),
            Drop(objs.uncut_diamond, 2, 2, 21, true),
            Drop(objs.uncut_dragonstone, 2, 1, 12, true),
            Drop(objs.onyx_bolt_tips, 2, 1, 45),
            Drop(PrisonObjs.essence, 1, 784, 5500, true),
        )

    fun distribute(credits: Map<Player, Contribution>) {
        // Leaving makes a player ineligible without transferring their contribution to another
        // player's unique chance.
        val mvp =
            credits
                .filter { (p, c) -> c.qualifies && p.isSlotAssigned && inFight(p.coords) }
                .maxByOrNull { it.value.points }
                ?.key
        for ((p, c) in credits) {
            if (!c.qualifies || !inFight(p.coords) || !p.isSlotAssigned) continue
            set(p, PrisonVars.kills, p.vars[PrisonVars.kills] + 1)
            drop(
                p,
                objs.crystal_shard,
                when {
                    p === mvp -> 3
                    c.unique -> 2
                    else -> 1
                },
            )
            if (p === mvp) drop(p, PrisonObjs.ashes, 1)
            if (p.vars[PrisonBits.xp] != 0)
                p.statAdvance(stats.mining, c.points.coerceAtMost(1000) * 5.0)
            else {
                val d = tableElement(random.of(36))
                val amount = d.min + ((d.max - d.min) * c.points.coerceIn(0, 1000) / 1000)
                val count = if (p === mvp) ceil(amount * 1.1).toInt() else amount
                val item = if (d.noted) types.cert(types[d.item]) else types[d.item]
                ground.add(InvObj(item, count), p.coords, 300, p)
            }
            if (c.unique) {
                if (random.randomBoolean(1500 - 750 * c.uniquePoints / 1000))
                    drop(p, PrisonObjs.shard, 1)
                if (random.randomBoolean(2250)) pets.award(p, PrisonObjs.pet.id)
            }
        }
        val eligible = credits.filterValues { it.unique }
        if (eligible.isEmpty() || !random.randomBoolean(200)) return
        var roll = random.of(eligible.values.sumOf { it.uniquePoints })
        for ((p, c) in eligible) {
            if (roll < c.uniquePoints) {
                if (p.isSlotAssigned && inFight(p.coords))
                    drop(p, if (random.randomBoolean(40)) objs.uncut_onyx else PrisonObjs.seed, 1)
                return
            }
            roll -= c.uniquePoints
        }
    }

    private fun tableElement(value: Int): Drop {
        var roll = value
        for (d in table) {
            if (roll < d.weight) return d
            roll -= d.weight
        }
        error("Invalid Zalcano loot roll")
    }

    private fun drop(p: Player, type: ObjType, count: Int) {
        ground.add(type, p.coords, 300, p, count)
    }
}
