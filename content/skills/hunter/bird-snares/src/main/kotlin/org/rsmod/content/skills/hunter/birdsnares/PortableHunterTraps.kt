package org.rsmod.content.skills.hunter.birdsnares

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Player
import org.rsmod.game.type.controller.ControllerType
import org.rsmod.game.type.varcon.VarConType

/** All portable trap families share the same per-player simultaneous-trap allowance. */
@Singleton
public class PortableHunterTraps @Inject constructor(private val controllers: ControllerList) {
    private val ownerVars = mutableMapOf<Int, VarConType>()

    public fun register(type: ControllerType, owner: VarConType) {
        ownerVars[type.id] = owner
    }

    public fun count(player: Player): Int =
        controllers.count { controller ->
            val owner = ownerVars[controller.id]
            owner != null && controller.vars[owner] == player.slotId
        }
}
