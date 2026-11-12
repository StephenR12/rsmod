package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class DagannothKingsDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        DagannothKings.definitions.forEach { definition ->
            registry.register(
                definition.npc,
                NpcDropTable(
                    sequence(
                        item(DagannothKingObjs.bones),
                        independent(denominator = 128, roll = item(DagannothKingObjs.dragonAxe)),
                        *definition.uniques
                            .map { independent(denominator = 128, roll = item(it)) }
                            .toTypedArray(),
                        independent(denominator = 5_000, roll = item(definition.pet)),
                    )
                ),
            )
        }
    }
}
