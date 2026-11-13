package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.clue.Cache233ClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.random.GameRandom
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.magic.spell.utility.standard.PoisonProtectionRepository
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.WeaponCategory
import org.rsmod.map.CoordGrid

internal object TreasureLocs : LocReferences() {
    val bridge = find("hallowed_construction_multi")
    val poolBridge = find("hallowed_construction_pool_multi")
    val pillar = find("hallowed_ranged_pillar_multi")
    val braziers = listOf(find("hallowed_prayer_multi"), find("hallowed_prayer_multi_mirror"))
    val barrier = find("hallowed_treasure_prayer_barrier_multi")
    val portal = find("hallowed_magic_multi")
    val gate = find("hallowed_final_chest_barrier")
    val coffins =
        listOf(
                "reward_coffin_a",
                "reward_coffin_b",
                "f5_grapple_coffin",
                "f5_portal_coffin",
                "f5_bridge_coffin",
                "final_chest",
            )
            .map { find("hallowed_$it") }
}

internal object TreasureBits : VarBitReferences() {
    val ranged = find("hallowed_ranged_state")
    val prayer = find("hallowed_prayer_state")
    val building = find("hallowed_construction_state")
    val magic = find("hallowed_magic_state")
    val prayerAsh = find("hallowed_prayer_ashes_offered")
    val failures = find("hallowed_magic_spell_failed")
    val coffins =
        listOf(
                "current_floor_looted",
                "current_floor_looted_b",
                "floor5_grapple_looted",
                "floor5_portal_looted",
                "floor5_bridge_looted",
                "final_floor_looted",
            )
            .map { find("hallowed_$it") }
    val ordinary = find("hallowed_normal_chest_count")
    val grand = find("hallowed_final_chest_count")
    val pages = (1..5).map { find("hallowed_tome_page$it") }
}

internal object TreasureItems : ObjReferences() {
    val pages = (1..5).map { find("hallowed_floor${it}_page") }
    val sack = find("hallowed_sack")
}

internal enum class SepulchreChallenge {
    Bridge,
    Prayer,
    Portal,
    Grapple,
    Grand,
}

internal data class TreasureCrossing(val from: CoordGrid, val to: CoordGrid)

internal class TreasureProgress {
    val accessible = mutableSetOf<SepulchreChallenge>()
    val visited = mutableSetOf<Pair<SepulchreChallenge, Boolean>>()
}

private data class Loot(val item: Int, val min: Int = 1, val max: Int = min)

private val low =
    listOf(
        Loot(544),
        Loot(542),
        Loot(1718),
        Loot(556, 500, 750),
        Loot(554, 500, 750),
        Loot(562, 25, 50),
        Loot(9142, 50, 150),
        Loot(141),
        Loot(22932),
        Loot(995, 1500, 3000),
    )
private val mid =
    listOf(
        Loot(1317),
        Loot(1123),
        Loot(564, 60, 100),
        Loot(560, 60, 100),
        Loot(561, 60, 100),
        Loot(9143, 50, 200),
        Loot(7946, 1, 3),
        Loot(2434),
        Loot(207, 1, 2),
        Loot(995, 7500, 12500),
    )
private val high =
    listOf(
        Loot(1319),
        Loot(1127),
        Loot(563, 150, 250),
        Loot(565, 150, 250),
        Loot(566, 150, 250),
        Loot(9144, 100, 300),
        Loot(7946, 2, 6),
        Loot(10925, 1, 2),
        Loot(5295, 1, 2),
        Loot(995, 17500, 25000),
    )

/** Each entrance leads across its own map gap; several routes share the same physical coffin. */
public class SepulchreTreasure
@Inject
constructor(
    private val types: ObjTypeList,
    private val runes: MagicRuneManager,
    private val spells: MagicSpellRegistry,
    private val poison: PoisonProtectionRepository,
    private val ground: org.rsmod.api.repo.obj.ObjRepository,
    private val clues: ClueInventory,
    private val steps: Cache233ClueSteps,
) {
    internal fun reset(a: ProtectedAccess) =
        with(a) {
            for (bit in
                listOf(
                    TreasureBits.magic,
                    TreasureBits.building,
                    TreasureBits.prayer,
                    TreasureBits.ranged,
                )) vars[bit] = 1
            for (bit in
                TreasureBits.coffins + listOf(TreasureBits.failures, TreasureBits.prayerAsh)) vars[
                bit] = 0
        }

    internal suspend fun cross(
        a: ProtectedAccess,
        loc: BoundLocInfo,
        kind: SepulchreChallenge,
        floor: SepulchreFloor,
        state: TreasureProgress,
        space: SepulchreSpace,
        timeLeft: Boolean,
    ) =
        with(a) {
            val here = space.normal(coords)
            val path = path(loc, kind) ?: return@with
            val inward = here.chebyshevDistance(path.from) <= here.chebyshevDistance(path.to)
            val origin = if (inward) path.from else path.to
            if (here.chebyshevDistance(origin) > 3 || coords.level != origin.level) return@with
            if (!timeLeft && (inward || kind !in state.accessible)) return@with
            if (kind !in state.accessible) {
                val ready =
                    when (kind) {
                        SepulchreChallenge.Bridge -> repair(a)
                        SepulchreChallenge.Portal -> portal(a)
                        SepulchreChallenge.Prayer -> vars[TreasureBits.prayer] == 2
                        SepulchreChallenge.Grapple -> grapple(a)
                        SepulchreChallenge.Grand -> true
                    }
                if (!ready) return@with
                state.accessible += kind
            } else if (kind == SepulchreChallenge.Grapple && !grapple(a)) return@with
            val dest = if (inward) path.to else path.from
            crossObstacle(
                space.world(dest),
                if (kind == SepulchreChallenge.Grapple) GrappleSequences.grapple
                else ArenaMoves.step,
                2,
            )
            if (kind != SepulchreChallenge.Grand && state.visited.add(kind to inward))
                statAdvance(stats.agility, listOf(50.0, 50.0, 75.0, 75.0, 80.0)[floor.ordinal])
        }

    internal fun pray(a: ProtectedAccess) =
        with(a) {
            if (vars[TreasureBits.prayer] != 1) return@with
            if (statBase(stats.prayer) < 54) {
                mes("You need a Prayer level of 54 to use the brazier.")
                return@with
            }
            val amount = if (player.worn.any { it?.id == 24725 }) 1 else 2
            if (invDel(inv, types.getValue(3325), amount).failure) {
                mes("You need $amount vampyre dust to open this holy barrier.")
                return@with
            }
            vars[TreasureBits.prayerAsh] = 1
            vars[TreasureBits.prayer] = 2
            statAdvance(stats.prayer, 200.0)
        }

    private fun repair(a: ProtectedAccess): Boolean =
        with(a) {
            fun has(id: Int) = player.inv.any { it?.id == id } || player.worn.any { it?.id == id }
            if (
                stat(stats.construction) + (if (has(9625)) 3 else 0) < 56 ||
                    listOf(8794, 9625, 24880, 29774).none(::has) ||
                    listOf(2347, 25644, 29775, 24727).none(::has)
            ) {
                mes("You need 56 Construction, a hammer and a saw to repair this bridge.")
                return false
            }
            val recipe =
                listOf(8782 to 4824, 8780 to 4823, 8778 to 4822, 960 to 1539).firstOrNull {
                    (plank, nail) ->
                    inv.count(types.getValue(plank)) >= 2 && inv.count(types.getValue(nail)) >= 5
                }
            if (recipe == null) {
                mes("You need two planks and five matching nails.")
                return false
            }
            val (plank, nail) = recipe
            if (!has(24727) && random.of(8) == 0) {
                invDel(inv, types.getValue(nail), 5)
                mes("Your nails bend as you try to repair the bridge.")
                return false
            }
            if (
                player
                    .invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, plank, 2)
                        delete(bag, nail, 5)
                    }
                    .failure
            )
                return false
            vars[TreasureBits.building] = 2
            statAdvance(
                stats.construction,
                when (plank) {
                    960 -> 150.0
                    8778 -> 300.0
                    8780 -> 450.0
                    else -> 700.0
                },
            )
            true
        }

    private fun portal(a: ProtectedAccess): Boolean =
        with(a) {
            if (vars[TreasureBits.failures] >= 2) {
                mes("This portal frame has been damaged.")
                return false
            }
            val focus = player.worn.any { it?.id == 24723 }
            val levels =
                listOf(
                    objs.spell_enchant_lvl_1,
                    objs.spell_enchant_lvl_2,
                    objs.spell_enchant_lvl_3,
                    objs.spell_enchant_lvl_4,
                    objs.spell_enchant_lvl_5,
                    objs.spell_enchant_lvl_6,
                    objs.spell_enchant_lvl_7,
                )
            val choices = if (focus) levels else levels.reversed()
            val spell =
                choices
                    .mapNotNull { spells.getObjSpell(it) }
                    .firstOrNull { runes.canCastSpell(player, it) }
            if (spell == null) {
                mes("You need the runes and level to cast a jewellery enchantment spell.")
                return false
            }
            if (runes.attemptCast(player, spell).isFailure()) return false
            if (
                !focus &&
                    random.of(
                        3 + levels.indexOf(choices.first { spells.getObjSpell(it) == spell })
                    ) == 0
            ) {
                vars[TreasureBits.failures] = vars[TreasureBits.failures] + 1
                if (vars[TreasureBits.failures] >= 2) vars[TreasureBits.magic] = 0
                mes("The portal flickers and collapses.")
                return false
            }
            vars[TreasureBits.magic] = 2
            statAdvance(stats.magic, 200.0)
            true
        }

    private fun grapple(a: ProtectedAccess): Boolean =
        with(a) {
            val weapon = player.righthand?.let { types[it] }
            val good = player.quiver?.id == 24721
            if (
                stat(stats.ranged) < 62 ||
                    weapon?.weaponCategory != WeaponCategory.Crossbow.id ||
                    (!good && player.quiver?.id != objs.mith_grapple.id)
            ) {
                mes("You need 62 Ranged, a crossbow and an equipped grapple.")
                return false
            }
            if (!good && random.of(6) == 0) {
                mes("Your grapple slips off the pillar.")
                return false
            }
            true
        }

    private fun path(l: BoundLocInfo, kind: SepulchreChallenge): TreasureCrossing? {
        fun tiles(x: Int, z: Int, toX: Int, toZ: Int) =
            TreasureCrossing(CoordGrid(x, z, l.level), CoordGrid(toX, toZ, l.level))
        return when (kind) {
            SepulchreChallenge.Bridge ->
                when (l.x) {
                    2262 -> tiles(2262, 5981, 2262, 5975)
                    2553 -> tiles(2553, 5843, 2553, 5849)
                    2244 -> tiles(2244, 5864, 2244, 5858)
                    else -> null
                }
            SepulchreChallenge.Grapple ->
                when (l.x) {
                    2281,
                    2283 -> tiles(l.x, 5983, l.x, 5973)
                    2517,
                    2392 -> tiles(l.x - 5, l.z, l.x + 5, l.z)
                    2539,
                    2408,
                    2288 -> tiles(l.x + 5, l.z, l.x - 5, l.z)
                    else -> null
                }
            SepulchreChallenge.Portal ->
                when (l.x) {
                    2384 -> tiles(2386, 5839, 2386, 5843)
                    2549 -> tiles(2549, 5859, 2549, 5855)
                    2258 -> tiles(2255, 5834, 2261, 5834)
                    else -> null
                }
            SepulchreChallenge.Prayer ->
                if (l.x == 2528) tiles(l.x + 1, l.z, l.x - 1, l.z)
                else tiles(l.x - 1, l.z, l.x + 1, l.z)
            SepulchreChallenge.Grand ->
                if (l.z == 5857) tiles(2272, 5858, 2272, 5856) else tiles(2272, 5849, 2272, 5851)
        }
    }

    internal suspend fun loot(
        a: ProtectedAccess,
        loc: BoundLocInfo,
        index: Int,
        floor: SepulchreFloor,
        state: TreasureProgress,
    ) =
        with(a) {
            if (vars[TreasureBits.coffins[index]] != 0) return@with
            if (stat(stats.thieving) < 66) {
                mes("You need a Thieving level of 66 to loot this coffin.")
                return@with
            }
            val accessible =
                when (index) {
                    2 -> SepulchreChallenge.Grapple in state.accessible
                    3 -> SepulchreChallenge.Portal in state.accessible
                    4 -> SepulchreChallenge.Bridge in state.accessible
                    5 -> SepulchreChallenge.Grand in state.accessible
                    else ->
                        when (loc.x) {
                            2272 ->
                                SepulchreChallenge.Bridge in state.accessible ||
                                    SepulchreChallenge.Grapple in state.accessible
                            2528,
                            2400 -> SepulchreChallenge.Grapple in state.accessible
                            2524,
                            2418,
                            2542 -> SepulchreChallenge.Prayer in state.accessible
                            2386 -> SepulchreChallenge.Portal in state.accessible
                            2553 ->
                                SepulchreChallenge.Portal in state.accessible ||
                                    SepulchreChallenge.Bridge in state.accessible
                            else -> false
                        }
                }
            if (!accessible) return@with
            val old = inv.any { it?.id == 24738 || it?.id == 24740 }
            val normal = inv.any { it?.id == 1523 }
            val success =
                when {
                    old -> 12
                    normal -> 8
                    else -> 4
                }
            delay(if (old) 1 else 2)
            if (random.of(success) == 0) {
                poison.inflictPoison(player, 20)
                mes("A trap in the coffin pricks your hand.")
                return@with
            }
            val grand = index == 5
            val marks = if (grand) 8..10 else listOf(1..1, 2..3, 3..5, 3..6, 4..6)[floor.ordinal]
            val rewards = mutableListOf(24711 to random.of(marks))
            val chances = listOf(200, 120, 90, 60, 40)
            repeat(if (index == 4) 2 else 1) {
                if (grand && random.of(200) == 0) rewards += 24844 to 1
                if (!grand && random.of(chances[floor.ordinal]) == 0) rewards += 24740 to 1
                rewards += roll(if (grand) high else table(random, floor), random)
            }
            val page = TreasureItems.pages[floor.ordinal]
            if (
                !grand &&
                    vars[TreasureBits.pages[floor.ordinal]] == 0 &&
                    page !in inv &&
                    player.invMap[invs.bank]?.contains(page) != true &&
                    random.of(10) == 0
            )
                rewards += page.id to 1
            val tier =
                listOf(
                    ClueTier.Easy,
                    ClueTier.Medium,
                    ClueTier.Medium,
                    ClueTier.Hard,
                    ClueTier.Elite,
                )[floor.ordinal]
            val denominator = if (grand) 30 else listOf(50, 50, 25, 50, 60)[floor.ordinal]
            val clue =
                if (random.of(denominator) == 0)
                    clues.reward(clues.state(player, tier), tier) {
                        steps.select(tier, random.of(steps.steps(tier).size))
                    }
                else null
            clue?.obj?.let { rewards += it.id to 1 }
            for ((item, amount) in rewards) {
                val type = types.getValue(item)
                val remaining = amount - invAdd(inv, type, amount, strict = false).completed()
                if (remaining > 0)
                    ground.add(
                        type,
                        coords,
                        count = remaining,
                        duration = clue?.takeIf { it.obj?.id == item }?.groundDuration ?: 100,
                        receiver = player,
                    )
            }
            clue?.message?.let(::mes)
            vars[TreasureBits.coffins[index]] = 1
            val count = if (grand) TreasureBits.grand else TreasureBits.ordinary
            vars[count] = vars[count] + 1
            statAdvance(stats.thieving, 200.0)
        }

    internal fun sack(a: ProtectedAccess) =
        with(a) {
            val rewards =
                (List(5) { low } + listOf(mid, high, high)).map {
                    val (id, count) = roll(it, random)
                    (if (id == 7946) 7947 else id) to count
                }
            if (
                player
                    .invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, TreasureItems.sack.id, 1)
                        for ((id, count) in rewards) add(bag, id, count)
                    }
                    .failure
            )
                mes("You need more inventory space to open this sack.")
        }

    private fun table(random: GameRandom, floor: SepulchreFloor): List<Loot> {
        val chance = random.of(100)
        return when {
            chance < listOf(60, 30, 15, 0, 0)[floor.ordinal] -> low
            chance < listOf(95, 90, 80, 60, 20)[floor.ordinal] -> mid
            else -> high
        }
    }

    private fun roll(table: List<Loot>, random: GameRandom): Pair<Int, Int> {
        val roll = table[random.of(table.size)]
        return roll.item to random.of(roll.min..roll.max)
    }
}
