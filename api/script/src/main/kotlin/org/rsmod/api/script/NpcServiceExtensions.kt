package org.rsmod.api.script

import org.rsmod.api.player.events.interact.NpcContentEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Install a service after content scripts have registered. A bespoke interaction (including an
 * approach or content-group handler) owns the entire option, including its access checks.
 */
public fun ScriptContext.onMissingNpcService(
    type: UnpackedNpcType,
    slot: Int,
    action: suspend ProtectedAccess.(Npc) -> Unit,
) {
    val index = slot - 1
    if (
        eventBus.contains(ops[index], type.id) ||
            eventBus.contains(approaches[index], type.id) ||
            eventBus.contains(groupOps[index], type.contentGroup) ||
            eventBus.contains(groupApproaches[index], type.contentGroup)
    )
        return

    suspend fun ProtectedAccess.approach(npc: Npc) {
        if (isWithinApRange(npc, 2)) action(npc)
    }

    when (slot) {
        1 -> {
            onOpNpc1(type) { action(it.npc) }
            onApNpc1(type) { approach(it.npc) }
        }
        2 -> {
            onOpNpc2(type) { action(it.npc) }
            onApNpc2(type) { approach(it.npc) }
        }
        3 -> {
            onOpNpc3(type) { action(it.npc) }
            onApNpc3(type) { approach(it.npc) }
        }
        4 -> {
            onOpNpc4(type) { action(it.npc) }
            onApNpc4(type) { approach(it.npc) }
        }
        5 -> {
            onOpNpc5(type) { action(it.npc) }
            onApNpc5(type) { approach(it.npc) }
        }
    }
}

private val ops =
    listOf(
        NpcEvents.Op1::class.java,
        NpcEvents.Op2::class.java,
        NpcEvents.Op3::class.java,
        NpcEvents.Op4::class.java,
        NpcEvents.Op5::class.java,
    )
private val approaches =
    listOf(
        NpcEvents.Ap1::class.java,
        NpcEvents.Ap2::class.java,
        NpcEvents.Ap3::class.java,
        NpcEvents.Ap4::class.java,
        NpcEvents.Ap5::class.java,
    )
private val groupOps =
    listOf(
        NpcContentEvents.Op1::class.java,
        NpcContentEvents.Op2::class.java,
        NpcContentEvents.Op3::class.java,
        NpcContentEvents.Op4::class.java,
        NpcContentEvents.Op5::class.java,
    )
private val groupApproaches =
    listOf(
        NpcContentEvents.Ap1::class.java,
        NpcContentEvents.Ap2::class.java,
        NpcContentEvents.Ap3::class.java,
        NpcContentEvents.Ap4::class.java,
        NpcContentEvents.Ap5::class.java,
    )
