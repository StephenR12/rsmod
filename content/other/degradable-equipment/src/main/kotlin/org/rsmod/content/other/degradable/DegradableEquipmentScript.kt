package org.rsmod.content.other.degradable

import jakarta.inject.Inject
import org.rsmod.api.script.advanced.onPrepareDropHeld
import org.rsmod.api.script.onObjTradeQuery
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onPlayerCombatEngage
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class DegradableEquipmentScript
@Inject
constructor(private val lifecycle: DegradableEquipmentLifecycle) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerCombatEngage { lifecycle.engage(player) }
        onPlayerSoftTimer(degradation_timers.combat) { lifecycle.combatTimer(player) }
        onPlayerLogout { lifecycle.clear(player) }
        onPrepareDropHeld { groundObj = lifecycle.prepareDrop(obj) }
        onObjTradeQuery {
            val definition = DegradableEquipmentCatalog.byType[type.id] ?: return@onObjTradeQuery
            val remaining = definition.remainingPoints(obj)
            if (remaining != definition.maxPoints && remaining != 0) {
                deny()
            }
        }

        for (definition in DegradableEquipmentCatalog.definitions) {
            for (active in definition.activeStages) {
                onOpHeld3(active) { lifecycle.check(this, it.obj) }
                onOpWorn2(active) { lifecycle.check(this, player.worn[it.slot]) }
            }
        }
    }
}
