package org.rsmod.content.skills.mining.motherlode

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType

public object MlmRewards {
    public val ores: List<ObjType> =
        listOf(
            objs.coal,
            objs.gold_ore,
            objs.mithril_ore,
            objs.adamantite_ore,
            objs.runite_ore,
            MlmObjs.nugget,
        )

    public fun xp(index: Int): Double =
        when (index) {
            1,
            2 -> 15.0
            3 -> 30.0
            4 -> 45.0
            5 -> 75.0
            else -> 0.0
        }

    public fun choose(access: ProtectedAccess): Int =
        with(access) {
            if (random.of(32) == 0) return 6
            val level = player.stat(stats.mining)
            val elite = vars[DiaryRegion.Falador.tier(DiaryDifficulty.Elite).complete] != 0
            // Published Elite increases are approximate; integer increments model ~1/~2/~1
            // percentage points.
            for ((index, required, low, high, bonus) in
                listOf(
                    intArrayOf(5, 85, -20, 5, 3),
                    intArrayOf(4, 70, -90, 50, 6),
                    intArrayOf(3, 55, -19, 90, 3),
                    intArrayOf(2, 40, -40, 126, 0),
                )) {
                if (
                    level >= required &&
                        statRandom(
                            stats.mining,
                            low + (if (elite) bonus else 0),
                            high + (if (elite) bonus else 0),
                            0,
                        )
                )
                    return index
            }
            1
        }
}

/** Per-item countdowns/ore selections persist in the normal account inventory format. */
public class MotherlodeStorage
@Inject
constructor(
    private val invs: InvTypeList,
    private val xp: XpModifiers,
    private val events: EventBus,
) {
    public fun pending(p: Player): Inventory = p.invMap.getOrPut(invs[MlmInvs.pending])

    public fun sack(p: Player): Inventory = p.invMap.getOrPut(invs[MlmInvs.sack])

    public fun count(p: Player): Int =
        pending(p).filterNotNull().sumOf { it.count } + sack(p).filterNotNull().sumOf { it.count }

    public fun capacity(p: Player): Int = if (p.vars[MlmBits.big] != 0) 189 else 108

    public fun deposit(access: ProtectedAccess): Int =
        with(access) {
            if (!player.members) return 0
            val items =
                inv.indices
                    .filter { inv[it]?.id == MlmObjs.dirt.id }
                    .take((capacity(player) - count(player)).coerceAtLeast(0))
            if (items.isEmpty()) {
                mes(
                    if (count(player) >= capacity(player))
                        "The sack is full. Collect your ore first."
                    else "You have no pay-dirt to deposit."
                )
                return 0
            }
            val target = pending(player)
            var nuggets = 0
            val tx =
                player.invTransaction(inv, target) {
                    val source = select(inv)
                    val into = select(target)
                    for (slot in items) {
                        val chosen = inv[slot]!!.vars.takeIf { it in 1..6 } ?: 1
                        val result = if (chosen == 6 && ++nuggets > 3) 0 else chosen
                        delete(source, MlmObjs.dirt.id, 1, slot = slot)
                        add(into, MlmObjs.dirt.id, 1, vars = (PROCESS_TICKS shl 4) or result)
                    }
                }
            if (!tx.success) return 0
            player.softTimer(MlmTimers.processing, 1)
            refresh(player)
            items.size
        }

    public fun process(player: Player, flowing: Boolean) {
        val from = pending(player)
        val to = sack(player)
        var bonus = 0.0
        if (flowing && player.members)
            for (slot in from.indices) {
                val dirt = from[slot] ?: continue
                val remaining = dirt.vars ushr 4
                val selected = dirt.vars and 15
                if (remaining > 1) {
                    from[slot] = dirt.copy(vars = dirt.vars - 16)
                    continue
                }
                val output = MlmRewards.ores.getOrNull(selected - 1)
                val tx =
                    player.invTransaction(from, to) {
                        delete(select(from), dirt.id, 1, slot = slot)
                        if (output != null) add(select(to), output.id, 1)
                    }
                if (tx.success) bonus += MlmRewards.xp(selected)
            }
        if (bonus > 0) player.statAdvance(stats.mining, bonus * xp.get(player, stats.mining))
        refresh(player)
        if (from.isEmpty() && !MlmArea.contains(player.coords))
            player.clearSoftTimer(MlmTimers.processing)
    }

    public fun collect(access: ProtectedAccess): Int =
        with(access) {
            if (!player.members) return 0
            val source = sack(player)
            var moved = 0
            // Nuggets stack and are taken together; ordinary ore quantities are limited by real
            // free slots.
            for (slot in
                source.indices.sortedBy { if (source[it]?.id == MlmObjs.nugget.id) 0 else 1 }) {
                val item = source[slot] ?: continue
                val maximum =
                    if (item.id == MlmObjs.nugget.id) {
                        if (inv.isFull() && MlmObjs.nugget !in inv) 0 else item.count
                    } else minOf(item.count, inv.freeSpace())
                if (maximum <= 0) continue
                val tx =
                    player.invTransaction(inv, source) {
                        delete(select(source), item.id, maximum, slot = slot)
                        add(select(inv), item.id, maximum)
                    }
                if (tx.success) moved += maximum
            }
            if (moved == 0)
                mes(
                    if (source.isEmpty()) "There is no cleaned ore ready to collect."
                    else "You don't have enough inventory space."
                )
            refresh(player)
            moved
        }

    public fun refresh(player: Player) {
        VarPlayerIntMapSetter.set(player, MlmBits.count, count(player))
        if (MlmArea.contains(player.coords)) {
            VarPlayerIntMapSetter.set(
                player,
                MlmBits.restricted,
                if (player.vars[MlmVars.unlocks] and 1 == 0) 1 else 0,
            )
            if (MlmUi.hud !in player.ui) player.ifOpenOverlay(MlmUi.hud, events)
        } else if (MlmUi.hud in player.ui) player.ifCloseOverlay(MlmUi.hud, events)
    }

    public fun login(p: Player) {
        if (p.invMap[invs[MlmInvs.pending]]?.isNotEmpty() == true || MlmArea.contains(p.coords)) {
            p.softTimer(MlmTimers.processing, 1)
            refresh(p)
        }
    }

    public companion object {
        public const val PROCESS_TICKS: Int = 14
    }
}
