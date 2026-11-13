package org.rsmod.api.player.events.interact

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.events.KeyedEvent
import org.rsmod.events.SuspendEvent
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.droptrig.DropTriggerType
import org.rsmod.game.type.inv.InvType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.obj.Wearpos

public class HeldBanksideEvents {
    public class Type(
        public val player: Player,
        public val slot: Int,
        public val type: UnpackedObjType,
        override val id: Long = type.id.toLong(),
    ) : KeyedEvent
}

public class BankedObjEvents {
    /** Fired for each newly withdrawn equipment item with the cache bank-autocharge option. */
    public class Autocharge(
        public val slot: Int,
        public val obj: InvObj,
        public val takeMaterial: (ObjType, Int) -> Int,
    ) : SuspendEvent<ProtectedAccess> {
        override val id: Long = obj.id.toLong()
    }

    public class ConfigureCharges(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
    ) : SuspendEvent<ProtectedAccess> {
        override val id: Long = type.id.toLong()
    }
}

public class HeldDropEvents {
    /**
     * Published only when a drop is about to be committed, after any valuable-item warning has been
     * accepted. Scripts may replace the obj placed on the ground while the inventory verification
     * and post-drop event continue to refer to the originally selected obj.
     */
    public class Prepare(
        public val player: Player,
        public val dropSlot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public var groundObj: InvObj = obj,
    ) : UnboundEvent

    public class Trigger(
        public val player: Player,
        public val dropSlot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        triggerType: DropTriggerType,
        override val id: Long = triggerType.id.toLong(),
    ) : KeyedEvent

    public data class Drop(
        public val player: Player,
        public val dropSlot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
    ) : UnboundEvent

    public data class Destroy(
        public val player: Player,
        public val invSlot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
    ) : UnboundEvent

    public data class Release(
        public val player: Player,
        public val invSlot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
    ) : UnboundEvent

    public data class Dispose(
        public val player: Player,
        public val invType: InvType,
        public val invSlot: Int,
        public val obj: InvObj,
    ) : UnboundEvent
}

public class HeldEquipEvents {
    public data class Equip(
        public val player: Player,
        public val invSlot: Int,
        public val wearpos: Wearpos,
        public val type: UnpackedObjType,
        override val id: Long = type.contentGroup.toLong(),
    ) : KeyedEvent

    public data class Unequip(
        public val player: Player,
        public val wearpos: Wearpos,
        public val type: UnpackedObjType,
        override val id: Long = type.contentGroup.toLong(),
    ) : KeyedEvent

    public data class WearposChange(
        public val player: Player,
        public val wearpos: Wearpos,
        public val objType: UnpackedObjType,
    ) : UnboundEvent
}

public sealed class HeldObjEvents(id: Number) : OpEvent(id.toLong()) {
    public class Op1(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldObjEvents(obj.id)

    public class Op2(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldObjEvents(obj.id)

    public class Op3(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldObjEvents(obj.id)

    public class Op4(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldObjEvents(obj.id)

    public class Op5(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldObjEvents(obj.id)
}

public sealed class HeldContentEvents(id: Number) : OpEvent(id.toLong()) {
    public class Op1(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldContentEvents(type.contentGroup)

    public class Op2(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldContentEvents(type.contentGroup)

    public class Op3(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldContentEvents(type.contentGroup)

    public class Op4(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldContentEvents(type.contentGroup)

    public class Op5(
        public val slot: Int,
        public val obj: InvObj,
        public val type: UnpackedObjType,
        public val inventory: Inventory,
    ) : HeldContentEvents(type.contentGroup)
}

public class HeldUEvents {
    public class Type(
        public val first: UnpackedObjType,
        public val firstSlot: Int,
        public val second: UnpackedObjType,
        public val secondSlot: Int,
    ) : SuspendEvent<ProtectedAccess> {
        override val id: Long = EventBus.composeLongKey(first.id, second.id)
    }
}

public class HeldUContentEvents {
    public class Type(
        public val first: UnpackedObjType,
        public val firstSlot: Int,
        public val second: UnpackedObjType,
        public val secondSlot: Int,
    ) : SuspendEvent<ProtectedAccess> {
        override val id: Long = EventBus.composeLongKey(first.contentGroup, second.id)
    }

    public class Content(
        public val first: UnpackedObjType,
        public val firstSlot: Int,
        public val second: UnpackedObjType,
        public val secondSlot: Int,
    ) : SuspendEvent<ProtectedAccess> {
        override val id: Long = EventBus.composeLongKey(first.contentGroup, second.contentGroup)
    }
}

public class HeldUDefaultEvents {
    public class Type(
        public val first: UnpackedObjType,
        public val firstSlot: Int,
        public val second: UnpackedObjType,
        public val secondSlot: Int,
    ) : SuspendEvent<ProtectedAccess> {
        override val id: Long = first.id.toLong()
    }

    public class Content(
        public val first: UnpackedObjType,
        public val firstSlot: Int,
        public val second: UnpackedObjType,
        public val secondSlot: Int,
    ) : SuspendEvent<ProtectedAccess> {
        override val id: Long = first.contentGroup.toLong()
    }
}
