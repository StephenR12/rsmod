package org.rsmod.content.bosses.giantmole

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class GiantMoleDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            GiantMoleNpcs.boss,
            NpcDropTable(
                sequence(
                    item(GiantMoleObjs.bigBones),
                    item(GiantMoleObjs.claw),
                    item(GiantMoleObjs.skin, count = 1..3),
                )
            ),
        )
    }
}
