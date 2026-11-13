package org.rsmod.api.player.hunter

import java.util.WeakHashMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

private data class ImplingClaim(val player:Player,val until:Int)
private val implingClaims=WeakHashMap<Npc,ImplingClaim>()

/** Standard successful Bind/Snare/Entangle catch grace; five cycles approximates the dated 'few seconds'. */
public fun Npc.reserveImplingCatch(player:Player,cycles:Int=5) {
    require(cycles>0)
    if(!isImplingCreature())return
    implingClaims[this]=ImplingClaim(player,player.currentMapClock+cycles)
}

public fun Npc.canCatchImpling(player:Player):Boolean {
    val claim=implingClaims[this]?:return true
    if(player.currentMapClock>=claim.until){implingClaims.remove(this);return true}
    return claim.player===player
}

/** Native catchable impling identities, including all Crystal presentation variants. */
public fun Npc.isImplingCreature():Boolean=id in 1635..1654||id==7233||id==7302||id in 8741..8757
