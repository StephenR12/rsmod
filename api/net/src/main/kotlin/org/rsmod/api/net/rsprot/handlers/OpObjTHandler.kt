package org.rsmod.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.objs.OpObjT
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.interact.ObjTInteractions
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.ctrlMoveSpeed
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.interf.InterfaceTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.ui.Component
import org.rsmod.map.CoordGrid

public class OpObjTHandler
@Inject
constructor(
    private val eventBus: EventBus,
    private val objTypes: ObjTypeList,
    private val objRegistry: ObjRegistry,
    private val componentTypes: ComponentTypeList,
    private val interfaceTypes: InterfaceTypeList,
    private val objTInteractions: ObjTInteractions,
) : MessageHandler<OpObjT> {
    private val logger = InlineLogger()

    private val OpObjT.asComponent: Component
        get() = Component(selectedInterfaceId, selectedComponentId)

    override fun handle(player: Player, message: OpObjT) {
        if (player.isDelayed) {
            return
        }

        val coords = CoordGrid(message.x, message.z, player.level)
        val obj =
            objRegistry.findAll(coords).firstOrNull {
                it.type == message.id && it.isVisibleTo(player)
            }
        if (obj == null) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

        val interfaceType = interfaceTypes[message.asComponent]
        val componentType = componentTypes[message.asComponent]
        val selectedObjType = message.selectedObj.takeIf { it != -1 }?.let(objTypes::get)
        val validInterface =
            player.ui.containsOverlay(interfaceType) || player.ui.containsModal(interfaceType)
        val targetEnabled =
            InterfaceEvents.isEnabled(player.ui, componentType, message.selectedSub, IfEvent.TgtObj)
        if (!validInterface || !targetEnabled) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

        val speed = if (message.controlKey) player.ctrlMoveSpeed() else null
        player.clearPendingAction(eventBus)
        player.resetFaceEntity()
        player.faceSquare(coords)
        objTInteractions.interact(
            player,
            obj,
            componentType,
            message.selectedSub,
            selectedObjType,
            clientRequest = true,
        )
        player.tempMoveSpeed = speed
        logger.debug {
            "OpObjT: obj=$obj, component=$componentType, comsub=${message.selectedSub}, " +
                "selectedObj=$selectedObjType"
        }
    }
}
