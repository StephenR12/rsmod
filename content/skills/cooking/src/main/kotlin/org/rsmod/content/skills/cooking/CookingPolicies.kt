package org.rsmod.content.skills.cooking

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal var Player.ogreSpitRoastState: Int by intVarBit(varbits.ogre_spit_roaster)

internal fun ProtectedAccess.clearCookingQueues() {
    clearWeakQueue(cooking_queues.standardFood)
    clearWeakQueue(cooking_queues.chompyFire)
    clearWeakQueue(cooking_queues.ogreSpitRoast)
    clearWeakQueue(cooking_queues.foodPreparation)
    clearWeakQueue(cooking_queues.dairyChurning)
    clearWeakQueue(cooking_queues.wineMaking)
    clearWeakQueue(cooking_queues.karambwan)
    clearWeakQueue(cooking_queues.sinew)
    clearWeakQueue(cooking_queues.ironSpit)
    player.clearOgreSpitRoastState()
}

internal fun Player.clearOgreSpitRoastState() {
    ogreSpitRoastState = OgreSpitRoast.EMPTY_STATE
    clearSoftTimer(cooking_timers.ogreSpitRoastState)
}

internal fun ProtectedAccess.hasCookingBurnImmunity(): Boolean =
    player.worn.contains(objs.cooking_skillcape) ||
        player.worn.contains(objs.cooking_skillcape_t) ||
        player.worn.contains(objs.max_cape_worn)
