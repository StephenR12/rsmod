package org.rsmod.content.skills.woodcutting

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** One selectable product at a Prifddinas singing bowl or its elven singers. */
public interface CrystalSingingRecipe {
    public val item: ObjType
    public fun available(player: Player, assisted: Boolean): Boolean = true
    public suspend fun sing(access: ProtectedAccess, assisted: Boolean)
}
