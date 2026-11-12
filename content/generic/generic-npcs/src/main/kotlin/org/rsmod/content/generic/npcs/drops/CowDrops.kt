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

/** Complete cache-233.4 drop table for the dated production Cow and Cow calf identities. */
public class CowDrops
@Inject
constructor(private val registry: NpcDropTableRegistry, private val clueDrops: NpcClueDropRolls) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val table =
            NpcDropTable(
                sequence(
                    item(objs.cow_hide),
                    item(objs.raw_beef),
                    item(objs.bones),
                    clueDrops.independent(
                        tier = ClueTier.Beginner,
                        clueScroll = objs.clue_scroll_beginner,
                        denominator = 128,
                    ),
                )
            )
        registry.register(COWS_AND_CALVES, table)
    }

    private companion object {
        val COWS_AND_CALVES =
            listOf(
                npcs.cow,
                npcs.cow2,
                npcs.cow3,
                npcs.cow_isle_of_souls,
                npcs.cow_zanaris,
                npcs.cow_calf2,
                npcs.cow_calf3,
                npcs.cow_calf_farmland,
            )
    }
}
