package org.rsmod.content.other.gembag

import jakarta.inject.Inject
import org.rsmod.api.obj.plugin.ObjTakeDestination
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.obj.ObjTypeList

public class GemBagGroundObjDestination
@Inject
constructor(private val storage: GemBagStorage, private val objTypes: ObjTypeList) :
    ObjTakeDestination {
    override val priority: Int = 100

    override fun destination(player: Player, obj: Obj): Inventory? =
        storage.acquisitionInventory(player, objTypes[obj], obj.count)

    override fun onTake(access: ProtectedAccess, obj: Obj) {
        access.mes(GemBagStorage.AUTO_STORE_MESSAGE)
    }
}
