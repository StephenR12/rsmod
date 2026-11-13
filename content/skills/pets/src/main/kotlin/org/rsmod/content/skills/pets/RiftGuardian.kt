package org.rsmod.content.skills.pets

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player

public class RiftGuardian @Inject constructor(private val pets: SkillPets,@CoreRandom private val random: GameRandom) {
    /** Each essence supplies one roll irrespective of rune yield, extract or outfit bonuses. */
    public fun craft(player: Player, altar: String, essence: Int) {
        if (!player.members || essence <= 0) return
        val colour = when (altar) {
            "ourania" -> random.of(14)
            "kourend blood" -> 13
            else -> COLOURS.indexOf(altar).takeIf { it >= 0 } ?: return
        }
        val form = guardianForms[colour]
        if (player.vars[PetBits.locked] == 0) pets.changeGuardian(player,form)
        val base = when (altar) {
            "ourania" -> 1_487_213
            "kourend blood" -> 804_984
            "soul" -> 782_999
            else -> 1_795_758
        }
        val rate = base - 25 * player.statBase(stats.runecrafting)
        val bonus = if (player.statMap.getXP(stats.runecrafting) >= 200_000_000) 15 else 1
        if ((0 until essence).any { random.of(rate) >= rate - bonus }) pets.award(player,form.item)
    }
    private companion object {
        val COLOURS = listOf("fire","air","mind","water","earth","body","cosmic","chaos","nature","law","death","soul","astral","blood","wrath")
    }
}
