package org.rsmod.api.player.interact

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.events.interact.ApEvent
import org.rsmod.api.player.events.interact.ObjTContentEvents
import org.rsmod.api.player.events.interact.ObjTDefaultEvents
import org.rsmod.api.player.events.interact.ObjTEvents
import org.rsmod.api.player.events.interact.OpEvent
import org.rsmod.events.EventBus
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionObjT
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType

public class ObjTInteractions
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val eventBus: EventBus,
    private val policies: ObjTInteractionPolicyRegistry,
) {
    public fun interact(
        player: Player,
        obj: Obj,
        component: ComponentType,
        comsub: Int,
        objType: ObjType?,
        clientRequest: Boolean = false,
    ) {
        val targetType = objTypes[obj]
        val opTrigger = hasOpTrigger(obj, targetType, component, comsub, objType)
        val apTrigger = hasApTrigger(obj, targetType, component, comsub, objType)
        val policy = policies[component]
        player.interaction =
            InteractionObjT(
                target = obj,
                comsub = comsub,
                objType = objType,
                component = component,
                hasOpTrigger = opTrigger,
                hasApTrigger = apTrigger,
                startApRange = policy.range,
                apCheckLineOfSight = policy.checkLineOfSight,
            )
        player.routeRequest = RouteRequestCoord(obj.coords, clientRequest)
    }

    public fun opTrigger(
        obj: Obj,
        targetType: UnpackedObjType,
        component: ComponentType,
        comsub: Int,
        objType: ObjType?,
    ): OpEvent? {
        val typeEvent = ObjTEvents.Op(obj, comsub, objType, targetType, component)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }
        val contentEvent =
            ObjTContentEvents.Op(obj, comsub, objType, component, targetType.contentGroup)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }
        val defaultEvent = ObjTDefaultEvents.Op(obj, comsub, objType, targetType, component)
        return defaultEvent.takeIf { eventBus.contains(it::class.java, it.id) }
    }

    public fun hasOpTrigger(
        obj: Obj,
        targetType: UnpackedObjType,
        component: ComponentType,
        comsub: Int,
        objType: ObjType?,
    ): Boolean = opTrigger(obj, targetType, component, comsub, objType) != null

    public fun apTrigger(
        obj: Obj,
        targetType: UnpackedObjType,
        component: ComponentType,
        comsub: Int,
        objType: ObjType?,
    ): ApEvent? {
        val typeEvent = ObjTEvents.Ap(obj, comsub, objType, targetType, component)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }
        val contentEvent =
            ObjTContentEvents.Ap(obj, comsub, objType, component, targetType.contentGroup)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }
        val defaultEvent = ObjTDefaultEvents.Ap(obj, comsub, objType, targetType, component)
        return defaultEvent.takeIf { eventBus.contains(it::class.java, it.id) }
    }

    public fun hasApTrigger(
        obj: Obj,
        targetType: UnpackedObjType,
        component: ComponentType,
        comsub: Int,
        objType: ObjType?,
    ): Boolean = apTrigger(obj, targetType, component, comsub, objType) != null
}

public data class ObjTInteractionPolicy(
    val range: Int = PathingEntity.DEFAULT_AP_RANGE,
    val checkLineOfSight: Boolean = true,
)

@Singleton
public class ObjTInteractionPolicyRegistry @Inject constructor() {
    private val policies = mutableMapOf<Int, ObjTInteractionPolicy>()

    public fun register(
        component: ComponentType,
        range: Int = PathingEntity.DEFAULT_AP_RANGE,
        checkLineOfSight: Boolean = true,
    ) {
        require(range >= 0) { "ObjT interaction range must be non-negative: $range" }
        val previous =
            policies.put(component.packed, ObjTInteractionPolicy(range, checkLineOfSight))
        check(previous == null) { "ObjT interaction policy already registered: $component" }
    }

    public operator fun get(component: ComponentType): ObjTInteractionPolicy =
        policies[component.packed] ?: ObjTInteractionPolicy()
}
