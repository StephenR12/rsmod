package org.rsmod.content.bosses.tempoross

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.content.skills.fishing.CrystalHarpoonState
import org.rsmod.content.skills.fishing.FishingTool
import org.rsmod.content.skills.fishing.FishingXp
import org.rsmod.content.skills.fishing.InfernalHarpoonState
import org.rsmod.content.skills.fishing.StandardFishingMethods
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList

internal class TemporossFishing
@Inject
constructor(
    private val voyages: TemporossVoyages,
    private val objects: ObjTypeList,
    private val seqs: SeqTypeList,
) {
    suspend fun catch(access: ProtectedAccess, npc: Npc): Unit =
        with(access) {
            val g = voyages.trip(player) ?: return
            if (npc !in g.fish && npc !in g.pools) return
            val pool = npc in g.pools
            while (voyages.trip(player) === g && g.returnIn < 0 && isWithinDistance(npc, 1)) {
                if (pool && g.submerged <= 0) return
                val tool =
                    StandardFishingMethods.harpoonTools.firstOrNull {
                        stat(stats.fishing) >= it.level &&
                            (it.type in inv || (it.wearable && it.type in worn))
                    }
                if (
                    tool == null &&
                        (vars[varbits.brut_fishing_s] < 1 ||
                            stat(stats.fishing) < 55 ||
                            player.statBase(stats.strength) < 35)
                ) {
                    mes("You need a harpoon. Take one from a supply crate on the ship.")
                    return
                }
                if (!pool && inv.isFull()) {
                    mes("Your inventory is full.")
                    return
                }
                anim(tool?.animation ?: seqs.getValue(6703))
                gatheringDelay(4)
                if (voyages.trip(player) !== g || g.returnIn >= 0 || !isWithinDistance(npc, 1))
                    return
                if (tool != null && tool.type !in inv && tool.type !in worn) return
                val xp = experience(player.statBase(stats.fishing))
                if (pool) {
                    if (g.submerged <= 0) return
                    FishingXp.advance(this, xp * 11)
                    mes("The glowing fish scatter, shedding their magical scales.")
                    voyages.scatter(g, player, random.of(1..12))
                } else {
                    val crystal = tool?.crystalDepletedType
                    val infernal = tool?.infernalDepletedType
                    val (low, high) =
                        when {
                            crystal != null -> 187 to 280
                            tool != null && tool.catchRateMultiplier > 1 -> 178 to 267
                            else -> 170 to 255
                        }
                    if (!statRandom(stats.fishing, low, high, 0)) continue
                    val count =
                        if (npc.transmog?.id == 10569 && inv.freeSpace() >= 2 && random.of(2) == 0)
                            2
                        else 1
                    repeat(count) {
                        val output =
                            when {
                                crystal != null && random.of(3) == 0 -> CRYSTAL
                                infernal != null && random.of(3) == 0 -> COOKED
                                else -> RAW
                            }
                        if (!invAdd(inv, objects.getValue(output)).success) return
                        if (tool != null && crystal != null) consume(tool, crystal, true)
                        if (tool != null && output == COOKED && infernal != null)
                            consume(tool, infernal, false)
                        g.add(player, 5)
                        FishingXp.advance(this, xp)
                        if (output == COOKED) statAdvance(stats.cooking, 5.0)
                        if (tool == null) {
                            statAdvance(stats.strength, 5.0)
                            if (vars[varbits.brut_fishing_s] == 1) vars[varbits.brut_fishing_s] = 2
                        }
                    }
                }
            }
        }

    private fun ProtectedAccess.consume(tool: FishingTool, depleted: ObjType, crystal: Boolean) {
        val bag = if (tool.type in worn) worn else inv
        val slot = bag.indexOfFirst { it?.id == tool.type.id }
        val item = bag[slot] ?: return
        bag[slot] =
            if (crystal) CrystalHarpoonState.fromCharged(item).consume(item, depleted)
            else InfernalHarpoonState.fromCharged(item).consume(item, depleted)
        if (bag === worn && bag[slot]?.id == depleted.id) rebuildAppearance()
    }

    suspend fun cook(access: ProtectedAccess): Unit =
        with(access) {
            val g = voyages.trip(player) ?: return
            while (g.returnIn < 0 && voyages.trip(player) === g && objects.getValue(RAW) in inv) {
                anim(seqs.getValue(896))
                gatheringDelay(3)
                if (voyages.trip(player) !== g || g.returnIn >= 0) return
                val result =
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, RAW, 1)
                        add(bag, COOKED, 1)
                    }
                if (!result.success) return
                g.add(player, 10)
                statAdvance(stats.cooking, 10.0)
            }
        }

    suspend fun load(access: ProtectedAccess, i: Int): Unit =
        with(access) {
            val g = voyages.trip(player) ?: return
            if (g.returnIn >= 0 || i !in 0..3) return
            g.loaders[player] = i
            try {
                while (voyages.trip(player) === g && g.returnIn < 0) {
                    val slot = inv.indexOfFirst { it?.id in harpoonfish }
                    val item = inv[slot] ?: return
                    gatheringDelay(2)
                    if (voyages.trip(player) !== g || g.returnIn >= 0 || inv[slot] != item) return
                    if (!invDel(inv, objects[item], 1, slot = slot).success) return
                    g.cannons[i].addLast(if (item.id == COOKED) 15 else 10)
                    voyages.crateVisual(g, i)
                    g.add(player, if (item.id == COOKED) 65 else 20)
                    FishingXp.advance(
                        this,
                        experience(player.statBase(stats.fishing)) *
                            if (item.id == CRYSTAL) 12 else 10,
                    )
                }
            } finally {
                g.loaders.remove(player)
            }
        }

    companion object {
        fun experience(level: Int): Double {
            val lv = level.coerceIn(35, 99)
            return (if (lv < 70) 450 + 550 * (lv - 35) / 64 else 890 + 110 * (lv - 70) / 29) / 100.0
        }
    }
}
