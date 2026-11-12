package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Complete cache-233.4 no-drop table for the loaded Duck and Duckling identities. */
public class DuckDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            listOf(npcs.duck, npcs.duck_female, npcs.duckling),
            NpcDropTable(nothing()),
        )
    }
}
