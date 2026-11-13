package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.walktriggers
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onPlayerWalkTrigger
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PickpocketScript
@Inject
constructor(
    private val pickpocketing: Pickpocketing,
    private val coinPouches: CoinPouches,
    private val stunStates: ThievingStunStateRepository,
    private val silentGloves: SilentGloves,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (definition in StandardPickpockets.all) {
            for (npc in definition.npcs) {
                onOpNpc3(npc) { pickpocketing.attempt(this, it.npc, definition) }
            }
        }
        for (definition in CoinPouchDefinitions.active) {
            onOpHeld1(definition.obj) { coinPouches.open(this, it.slot, definition, all = true) }
            onOpHeld2(definition.obj) { coinPouches.open(this, it.slot, definition, all = false) }
        }
        onPlayerWalkTrigger(walktriggers.stunned) { stunStates.blockMovement(player) }
        onOpHeld3(objs.gloves_of_silence) { silentGloves.check(this) }
        onOpWorn2(objs.gloves_of_silence) { silentGloves.check(this) }
        onOpHeldU(objs.gloves_of_silence, objs.dark_kebbit_fur) { silentGloves.repair(this) }
    }
}
