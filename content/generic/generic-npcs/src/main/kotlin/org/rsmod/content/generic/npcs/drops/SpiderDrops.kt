package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcClueDropRolls
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.game.type.npc.NpcType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Complete non-Wilderness table for both Spider identities loaded in the cache-233.4 world. */
public class SpiderDrops
@Inject
constructor(private val registry: NpcDropTableRegistry, private val clueDrops: NpcClueDropRolls) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val table =
            NpcDropTable(
                clueDrops.independent(
                    tier = ClueTier.Beginner,
                    clueScroll = objs.clue_scroll_beginner,
                    denominator = BEGINNER_CLUE_DENOMINATOR,
                )
            )
        loadedTypes.forEach { registry.register(it, table) }
    }

    public companion object {
        public const val BEGINNER_CLUE_DENOMINATOR: Int = 128
        public val loadedTypes: Set<NpcType> = setOf(npcs.spider, npcs.giant_spider)
    }
}
