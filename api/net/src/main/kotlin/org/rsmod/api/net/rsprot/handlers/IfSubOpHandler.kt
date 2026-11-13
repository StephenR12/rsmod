package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.buttons.IfSubOp
import org.rsmod.annotations.InternalApi
import org.rsmod.api.config.refs.components as baseComponents
import org.rsmod.api.config.refs.enums
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.ui.IfItemSubOp
import org.rsmod.api.player.ui.IfOverlaySubOp
import org.rsmod.api.player.ui.ifClose
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.enums.EnumTypeMapResolver
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.InterfaceTypeList
import org.rsmod.game.ui.Component

class IfSubOpHandler
@Inject
constructor(
    private val eventBus: EventBus,
    private val interfaces: InterfaceTypeList,
    private val components: ComponentTypeList,
    private val enumTypes: EnumTypeMapResolver,
    private val protectedAccess: ProtectedAccessLauncher,
) : MessageHandler<IfSubOp> {
    private val wornSlots by lazy {
        enumTypes[enums.equipment_tab_to_slots_map].associate { it.value?.packed to it.key }
    }

    @OptIn(InternalApi::class)
    override fun handle(player: Player, message: IfSubOp) {
        val op = IfButtonOp.entries.getOrNull(message.op - 1) ?: return
        val key = Component(message.interfaceId, message.componentId)
        val component = components[key.packed] ?: return
        val interf = interfaces[key.parent] ?: return
        if (!player.ui.containsOverlay(interf) && !player.ui.containsTopLevel(interf)) return
        if (!InterfaceEvents.isEnabled(player.ui, component, message.sub, op.toIfEvent())) return
        if (message.subop < 1) return
        val inventory = component.packed == baseComponents.inv_items.packed
        val slot = if (inventory) message.sub else wornSlots[component.packed]
        if (slot != null) {
            val source = if (inventory) player.inv else player.worn
            if (slot !in source.indices) return
            val item = source[slot] ?: return
            if (message.obj != item.id && (inventory || message.obj != -1)) return
            if (eventBus.contains(IfItemSubOp::class.java, item.id)) {
                player.ifClose(eventBus)
                if (player.isAccessProtected) return
                protectedAccess.launch(player) {
                    if (source[slot]?.id != item.id) return@launch
                    clearPendingAction()
                    eventBus.publish(
                        this,
                        IfItemSubOp(slot, !inventory, item.id, op, message.subop),
                    )
                }
                return
            }
        }
        eventBus.publish(
            IfOverlaySubOp(player, component, message.sub, message.obj, op, message.subop)
        )
    }
}
