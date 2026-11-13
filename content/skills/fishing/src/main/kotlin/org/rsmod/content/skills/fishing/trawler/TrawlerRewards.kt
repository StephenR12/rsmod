package org.rsmod.content.skills.fishing.trawler

import jakarta.inject.Inject
import org.rsmod.content.skills.pets.SkillPetRates
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invClear
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.recordDiaryTrawlerManta
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.content.skills.fishing.FishingOutfits
import org.rsmod.content.skills.fishing.FishingXp
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvTypeList

public class TrawlerRewards
@Inject
constructor(private val petRates: SkillPetRates,
    private val types: InvTypeList,
    private val random: GameRandom,
    private val objects: org.rsmod.game.type.obj.ObjTypeList,
    private val ground: org.rsmod.api.repo.obj.ObjRepository,
) {
    public fun rewards(player: Player): Inventory =
        player.invMap.getOrPut(types[TrawlerInvs.rewards])

    public fun clear(player: Player) {
        player.invClear(rewards(player))
        player.trawlerRewardXp = 0
        player.trawlerRewardManta = 0
        player.trawlerInspected = 0
    }

    /** Exact native level cascade150..200/256 and XP. Gross catch depends on our hazard model. */
    public fun generate(player: Player, points: Int, participants: Int, healthyTicks: Int) {
        clear(player)
        petRates.trawler(player,points)
        val level = player.statBase(stats.fishing)
        var count = (if (participants == 1) 14 else 10) * healthyTicks / 1000
        if (points == 255) count = count * 3 / 2
        if (player.vars[DiaryRegion.Ardougne.tier(DiaryDifficulty.Elite).complete] != 0)
            count = count * 3 / 2
        var fineXp = 0
        val items = linkedMapOf<Int, Int>()
        repeat(count) {
            val chance = (150 + 50 * (level - 1) / 98 + 1).coerceAtMost(256)
            val caught =
                fish.firstOrNull { level >= it.level && random.of(256) < chance } ?: fish.last()
            items[caught.id] = (items[caught.id] ?: 0) + 1
            fineXp += caught.xp
        }
        // Published probabilities are approximately37%onejunk and16%twojunk.
        val junk =
            when (random.of(100)) {
                in 0..15 -> 2
                in 16..52 -> 1
                else -> 0
            }
        repeat(junk) {
            val id = junkIds[random.of(junkIds.size)]
            items[id] = (items[id] ?: 0) + 1
            fineXp += 5
        }
        if (random.of(12) == 0) {
            val sets =
                listOf(
                    listOf(FishingOutfits.hat, FishingOutfits.spiritHat),
                    listOf(FishingOutfits.top, FishingOutfits.spiritTop),
                    listOf(FishingOutfits.legs, FishingOutfits.spiritLegs),
                    listOf(FishingOutfits.boots, FishingOutfits.spiritBoots),
                )
            fun owned(ids: List<org.rsmod.game.type.obj.ObjType>): Int =
                player.invMap.values.sumOf { inv -> ids.sumOf { inv.count(objects[it]) } }
            val counts = sets.map { owned(it) }
            val minimum = counts.min()
            if (minimum < 2) {
                val candidates = sets.indices.filter { counts[it] == minimum }
                val id = sets[candidates[random.of(candidates.size)]][0].id
                items[id] = (items[id] ?: 0) + 1
            }
        }
        val pending = rewards(player)
        for ((index, entry) in items.entries.withIndex()) pending[index] =
            InvObj(objects.getValue(entry.key), entry.value)
        player.trawlerRewardXp = fineXp
        player.trawlerRewardManta = items[389] ?: 0
        player.trawlerInspected = 0
    }

    public fun open(access: ProtectedAccess): Unit =
        with(access) {
            val pending = rewards(player)
            if (pending.isEmpty()) {
                mes("There is no catch waiting for you.")
                return
            }
            if (player.trawlerInspected == 0) {
                FishingXp.advance(this, player.trawlerRewardXp / 10.0)
                player.trawlerRewardXp = 0
                player.trawlerInspected = 1
            }
            ifOpenMainModal(TrawlerInterfaces.rewards)
            invTransmit(pending)
            interfaceInvInit(
                pending,
                TrawlerButtons.items,
                4,
                7,
                op1 = "Take",
                op2 = "Bank",
                op3 = "Discard",
            )
            ifSetEvents(
                TrawlerButtons.items,
                pending.indices,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
            )
        }

    public fun collect(
        access: ProtectedAccess,
        bank: Boolean = false,
        discard: Boolean = false,
        slot: Int? = null,
    ): Unit =
        with(access) {
            if (!player.members || player.trawlerInspected == 0) return
            if (discard) {
                drop(player, slot)
                return
            }
            val pending = rewards(player)
            val dest = if (bank) inv(invs.bank) else inv
            for (index in if (slot == null) pending.indices else listOf(slot)) {
                val item = pending[index] ?: continue
                val count = if (bank || discard) item.count else minOf(item.count, inv.freeSpace())
                if (count <= 0) {
                    mes("Make some inventory space for your catch.")
                    return
                }
                val tx =
                    player.invTransaction(pending, dest) {
                        val source = select(pending)
                        delete(source, item.id, count, index)
                        if (!discard) add(select(dest), item.id, count)
                    }
                if (!tx.success) {
                    mes("There is not enough room for your catch.")
                    return
                }
                if (item.id == 389 && !discard && player.trawlerRewardManta > 0) {
                    player.recordDiaryTrawlerManta()
                    player.trawlerRewardManta = (player.trawlerRewardManta - count).coerceAtLeast(0)
                }
            }
            invTransmit(pending)
        }

    /** Manual close drops the remaining catch; logout keeps the persistent reward inventory. */
    public fun close(player: Player) {
        if (!player.loggingOut) drop(player)
    }

    private fun drop(player: Player, slot: Int? = null) {
        val pending = rewards(player)
        for (index in if (slot == null) pending.indices else listOf(slot)) {
            val item = pending[index] ?: continue
            if (!ground.add(org.rsmod.game.obj.Obj.fromOwner(player, player.coords, item), 200))
                continue
            pending[index] = null
            if (item.id == 389 && player.trawlerRewardManta > 0) {
                player.recordDiaryTrawlerManta()
                player.trawlerRewardManta =
                    (player.trawlerRewardManta - item.count).coerceAtLeast(0)
            }
        }
    }

    private data class Fish(val id: Int, val level: Int, val xp: Int)

    private val fish =
        listOf(
            Fish(389, 81, 690),
            Fish(395, 79, 570),
            Fish(383, 76, 660),
            Fish(371, 50, 600),
            Fish(377, 40, 540),
            Fish(359, 35, 480),
            Fish(407, 16, 510),
            Fish(321, 15, 7),
            Fish(327, 15, 7),
            Fish(317, 15, 7),
        )
    private val junkIds = intArrayOf(687, 1469, 689, 688, 697, 685, 1931, 686)
}
