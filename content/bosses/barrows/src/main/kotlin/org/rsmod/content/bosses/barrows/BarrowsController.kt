package org.rsmod.content.bosses.barrows

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.clue.Cache233ClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

@Singleton
public class BarrowsController
@Inject
constructor(
    private val random: GameRandom,
    private val ground: ObjRepository,
    private val clues: ClueInventory,
    private val steps: Cache233ClueSteps,
) {
    private fun killed(player: Player): Set<Int> =
        BarrowsBrothers.all.indices
            .filter { player.vars[BarrowsBits.kills[it]] != 0 }
            .mapTo(mutableSetOf()) { BarrowsBrothers.all[it].npc.id }

    public fun onKill(player: Player, npc: Npc) {
        if (npc.id !in 1672..1688 || player.vars[BarrowsBits.collapse] != 0) return
        val index = BarrowsBrothers.all.indexOfFirst { it.npc.id == npc.id }
        if (index >= 0) {
            val killed = BarrowsBits.kills[index]
            if (player.vars[killed] != 0) return
            VarPlayerIntMapSetter.set(player, killed, 1)
        }
        val potential = player.vars[BarrowsBits.potential] + npc.type.vislevel.coerceAtLeast(0)
        VarPlayerIntMapSetter.set(player, BarrowsBits.potential, potential.coerceAtMost(1000))
        VarPlayerIntMapSetter.set(
            player,
            BarrowsBits.count,
            (player.vars[BarrowsBits.count] + 1).coerceAtMost(32767),
        )
    }

    public fun defeatedCount(player: Player): Int = killed(player).size

    public fun claim(access: ProtectedAccess) {
        with(access) {
            val kills = killed(player)
            if (vars[BarrowsBits.collapse] != 0) return
            val potential = vars[BarrowsBits.potential] + 2 * kills.size
            if (potential == 0) {
                mes("The chest is empty.")
                return
            }
            val eligible =
                kills.flatMap { BarrowsBrothers.byId.getValue(it).equipment }.toMutableList()
            val bonus = vars[DiaryRegion.Morytania.tier(DiaryDifficulty.Hard).reward] != 0
            val rewards = mutableMapOf<ObjType, Int>()
            val clueDenominator =
                if (vars[org.rsmod.api.config.refs.varbits.ca_tier_status_elite] >= 2) 190 else 200
            var foundClue = false
            repeat(kills.size + 1) {
                val (item, amount) =
                    if (eligible.isNotEmpty() && random.of(450 - 58 * kills.size) == 0)
                        eligible.removeAt(random.of(eligible.size)) to 1
                    else ordinaryReward(random.of(potential) + 1, bonus)
                rewards[item] = (rewards[item] ?: 0) + amount
                if (!foundClue) foundClue = random.of(clueDenominator) == 0
            }
            val tier = ClueTier.Elite
            val clue =
                if (foundClue)
                    clues.reward(clues.state(player, tier), tier) {
                        steps.select(tier, random.of(steps.steps(tier).size))
                    }
                else null
            clue?.obj?.let { rewards[it] = 1 }
            for ((item, count) in rewards) {
                val remaining = count - invAdd(inv, item, count, strict = false).completed()
                if (remaining > 0)
                    ground.add(
                        item,
                        coords,
                        count = remaining,
                        duration = clue?.takeIf { it.obj?.id == item.id }?.groundDuration ?: 100,
                        receiver = player,
                    )
            }
            clue?.message?.let(::mes)
            vars[BarrowsCounters.chests] = vars[BarrowsCounters.chests] + 1
            val fullSet =
                listOf(
                        EquipmentChecks::isDharokSet,
                        EquipmentChecks::isToragSet,
                        EquipmentChecks::isGuthanSet,
                        EquipmentChecks::isKarilSet,
                        EquipmentChecks::isVeracSet,
                        EquipmentChecks::isAhrimSet,
                    )
                    .any { it(player.hat, player.torso, player.legs, player.righthand) }
            if (fullSet) player.completeDiaryTask(DiaryTask.LootBarrowsWithSet)
            for (bit in BarrowsBits.kills) vars[bit] = 0
            vars[BarrowsBits.potential] = 0
            vars[BarrowsBits.count] = 0
            vars[BarrowsBits.collapse] = 1
            mes("You search the chest and claim your Barrows reward.")
        }
    }

    private fun ordinaryReward(roll: Int, bonusRunes: Boolean): Pair<ObjType, Int> {
        val (item, count) =
            when (roll) {
                in 1..380 -> objs.coins to roll * 2
                in 381..505 -> objs.mind_rune to roll * 2 / 3
                in 506..630 -> objs.chaos_rune to roll * 2 / 9
                in 631..755 -> objs.death_rune to roll / 9
                in 756..880 -> objs.blood_rune to roll / 20
                in 881..1005 -> BarrowsObjs.racks to roll / 25
                in 1006..1008 -> BarrowsObjs.loop to 1
                in 1009..1011 -> BarrowsObjs.tooth to 1
                else -> BarrowsObjs.dragonHelm to 1
            }
        return item to (count + if (bonusRunes && roll in 381..880) count / 2 else 0)
    }
}
