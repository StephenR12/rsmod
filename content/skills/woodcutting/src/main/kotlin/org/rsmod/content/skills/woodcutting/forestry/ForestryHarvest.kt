package org.rsmod.content.skills.woodcutting.forestry

import jakarta.inject.Inject
import org.rsmod.api.config.FellingAxes
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.content.skills.woodcutting.WoodcuttingLevelBoosts
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

internal object FellingSequences : SeqReferences() {
    val chop =
        listOf(
                "bronze",
                "iron",
                "steel",
                "black",
                "mithril",
                "adamant",
                "rune",
                "dragon",
                "crystal",
                "crystal_inactive",
                "3a",
            )
            .map { find("forestry_2h_axe_chopping_$it") }
}

public data class FellingChop(val xp: Double, val skipLog: Boolean)

public class ForestryHarvest
@Inject
constructor(
    private val gear: ForestryGear,
    private val items: ObjTypeList,
    private val ground: ObjRepository,
    private val group: WoodcuttingLevelBoosts,
    @CoreRandom private val random: GameRandom,
) {
    private val leaves = mapOf(1521 to 6022, 1519 to 6024, 1517 to 6028, 1515 to 6026, 1513 to 6030)

    public fun chop(player: Player, axe: InvObj): FellingChop {
        if (!player.members) return FellingChop(1.0, false)
        val fueled = FellingAxes.isFelling(axe.id) && gear.consume(player, 28157)
        val skip = fueled && random.of(5) == 0
        if (skip)
            player.mes(
                "You consume a Forester's ration to fuel a mighty chop. You strike a clean cut without gathering any material."
            )
        if (
            player.runEnergy < 9000 &&
                has(player, 28157) &&
                random.of(10) < 3 &&
                gear.consume(player, 28157)
        ) {
            player.runEnergy = (player.runEnergy + 2000).coerceAtMost(10000)
            UpdateRun.energy(player, player.runEnergy)
            player.mes("The Forester's ration restores some of your energy.")
        }
        return FellingChop(if (fueled) 1.1 else 1.0, skip)
    }

    private fun has(player: Player, id: Int) =
        items.getValue(id) in player.inv ||
            gear.hasKit(player) && items.getValue(id) in gear.kit(player)

    public fun afterLog(player: Player, log: ObjType, spare: Boolean) {
        if (!player.members) return
        val leaf = leaves[log.id]
        if (leaf != null && gear.hasKit(player) && random.of(4) == 0) {
            val count = if (gear.consume(player, 28161)) 2 else 1
            if (!gear.reward(player, leaf, count))
                player.invAddOrDrop(ground, items.getValue(leaf), count)
        }
        if (log.id !in gear.logs || !gear.consume(player, 28152) || !spare) return
        val cutters = with(group) { player.groupBoost() }
        val chance = 50 + (player.woodcuttingLvl.coerceIn(1, 99) - 1) * 10 / 98 + cutters * 2
        if (random.of(100) < chance && !gear.storeLog(player, log)) player.invAdd(player.inv, log)
    }
}
