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
import org.rsmod.api.death.NpcRareBoneDropRolls
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Complete cache-233.4 drop table for the loaded level-thirteen Giant frog. */
public class GiantFrogDrops
@Inject
constructor(private val registry: NpcDropTableRegistry, private val clueDrops: NpcClueDropRolls) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            npcs.giant_frog,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    clueDrops.independent(
                        tier = ClueTier.Beginner,
                        clueScroll = objs.clue_scroll_beginner,
                        denominator = BEGINNER_CLUE_DENOMINATOR,
                    ),
                    NpcRareBoneDropRolls.longOrCurved(),
                )
            ),
        )
    }

    public companion object {
        public const val BEGINNER_CLUE_DENOMINATOR: Int = 64
    }
}
