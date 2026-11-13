package org.rsmod.api.clue

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitType

public enum class SkillingClue(
    public val tier: ClueTier,
    public val geode: Int,
    public val bottle: Int,
    public val nest: Int,
    private val modifierTenths: Int,
    private val achievement: VarBitType?,
) {
    Beginner(ClueTier.Beginner, 23442, 23129, 23127, 2, null),
    Easy(ClueTier.Easy, 20358, 13648, 19712, 17, varbits.ca_tier_status_easy),
    Medium(ClueTier.Medium, 20360, 13649, 19714, 20, varbits.ca_tier_status_medium),
    Hard(ClueTier.Hard, 20362, 13650, 19716, 33, varbits.ca_tier_status_hard),
    Elite(ClueTier.Elite, 20364, 13651, 19718, 100, varbits.ca_tier_status_elite);

    public fun denominator(player: Player, base: Int, level: Int): Int {
        val ca = achievement?.let { player.vars[it] == 2 } == true
        val ring =
            this != Beginner &&
                player.vars[varbits.inside_wilderness] != 0 &&
                player.worn.any { it?.id == 12785 || it?.id in 20786..20790 }
        return denominator(base, level, modifierTenths, ca, ring)
    }

    public companion object {
        public fun denominator(
            base: Int,
            level: Int,
            modifierTenths: Int,
            ca: Boolean,
            ring: Boolean,
        ): Int {
            require(base > 0 && level >= 0 && modifierTenths > 0)
            val adjusted = if (ca) base.toLong() * 95 / 100 else base.toLong()
            val result = adjusted * modifierTenths / ((100L + level) * 10)
            return (if (ring) result / 2 else result).coerceAtLeast(1).toInt()
        }
    }
}

public class ClueVessels
@Inject
constructor(
    private val inventory: ClueInventory,
    private val types: ObjTypeList,
    private val ground: ObjRepository,
    @CoreRandom private val random: GameRandom,
) {
    public fun geodes(player: Player, level: Int, base: Int): Boolean =
        award(player, level, base * 7 / 10, true)

    public fun bottles(player: Player, level: Int, base: Int): Boolean =
        award(player, level, base, false)

    private fun award(player: Player, level: Int, base: Int, geode: Boolean): Boolean {
        if (player.vars[varbits.vesseled_clues_disabled] != 0) return false
        var found = false
        for (entry in SkillingClue.entries) {
            val tier = entry.tier
            if (tier.members && !player.members) continue
            val denominator = entry.denominator(player, base, level)
            if (random.of(denominator) != denominator - 1) continue
            val state = inventory.state(player, tier)
            if (!state.canReceive) {
                if (state.xMarksComplete)
                    player.mes(
                        "You have a sneaking suspicion that you would have received a ${tier.key} scroll box."
                    )
                continue
            }
            val vessel = types.getValue(if (geode) entry.geode else entry.bottle)
            val reward = if (state.xMarksComplete) types[tier.scrollBox] else vessel
            val duration =
                if (state.xMarksComplete) ClueInventory.SCROLL_BOX_GROUND_DURATION
                else Constants.lootdrop_duration
            player.invAddOrDrop(ground, reward, duration = duration)
            player.mes("You find a ${reward.name.lowercase()}.")
            found = true
        }
        return found
    }
}
