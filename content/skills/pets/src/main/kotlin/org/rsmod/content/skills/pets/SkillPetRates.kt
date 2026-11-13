package org.rsmod.content.skills.pets

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.stat.StatType

public class SkillPetRates @Inject constructor(
    private val pets: SkillPets,
    private val ground: ObjRepository,
    @CoreRandom private val random: GameRandom,
) {
    /** Called once per eligible skill action after its resource transaction succeeds. */
    public fun roll(player: Player, stat: StatType, pet: Int, base: Int, yieldedSlot: Int = -1) {
        if (!player.members) return
        val denominator = base - 25 * player.statBase(stat)
        val weight = if (player.statMap.getXP(stat) >= 200_000_000) 15 else 1
        if (random.of(denominator) >= denominator - weight) receive(player,pet,yieldedSlot)
    }
    public fun trawler(player: Player, contribution: Int) {
        if (!player.members || contribution < 50) return
        val chance = 205 + (contribution.coerceAtMost(255) - 50)
        if (random.of(5000 * 205) >= 5000 * 205 - chance) pets.award(player,13320)
    }
    /** Records which slots already held this resource, including stacks that must stay in place. */
    public fun occupiedBy(player: Player, resource: Int): Int {
        if (player.vars[PetBits.space] == 0) return -1
        var occupied = 0
        for (slot in player.inv.indices) if (player.inv[slot]?.id == resource) occupied = occupied or (1 shl slot)
        return occupied
    }

    public fun newResourceSlot(player: Player, resource: Int, occupied: Int): Int {
        if (occupied == -1 || !player.inv.isFull()) return -1
        return player.inv.indices.lastOrNull { occupied and (1 shl it) == 0 && player.inv[it]?.id == resource } ?: -1
    }

    private fun receive(player: Player, item: Int, yieldedSlot: Int) {
        val form = petForms.first { it.item == item }
        // Mining, Woodcutting, Hunter and Fishing give a newly found pet the last inventory slot.
        // Only the nonstackable resource or newly created stack delivered by this action may move.
        if (player.vars[PetBits.space] != 0 && player.inv.isFull() && pets.location(player,form.species) == 0) {
            val resource: InvObj? = player.inv[yieldedSlot]
            if (resource != null && ground.add(Obj.fromOwner(player,player.coords,resource),constants.lootdrop_duration)) {
                player.inv[yieldedSlot] = null
            }
        }
        pets.award(player,item)
    }
}
