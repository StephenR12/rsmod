package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcClueDropRolls
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Complete cache-233.4 drop table for the production level-five Frog. */
public class FrogDrops
@Inject
constructor(private val registry: NpcDropTableRegistry, private val clueDrops: NpcClueDropRolls) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            npcs.frog,
            NpcDropTable(
                sequence(
                    item(objs.bones),
                    clueDrops.independent(
                        tier = ClueTier.Beginner,
                        clueScroll = objs.clue_scroll_beginner,
                        denominator = 90,
                    ),
                )
            ),
        )
    }
}
