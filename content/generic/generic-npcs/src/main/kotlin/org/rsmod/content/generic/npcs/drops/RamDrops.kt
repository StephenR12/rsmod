package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.content.generic.npcs.ram.RAM_TYPES
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Complete cache-233.4 table for all five final pre-cutoff Ram identities. */
public class RamDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val questBones: RagAndBoneManIDropPolicy,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val ramSkull =
            RagAndBoneManIDropPolicy.QuestBone(
                raw = objs.ram_skull,
                inVinegar = objs.ram_skull_in_vinegar,
                polished = objs.polished_ram_skull,
                handedIndex = RAM_SKULL_INDEX,
            )
        registry.register(
            RAM_TYPES,
            NpcDropTable(sequence(item(objs.bones), questBones.guaranteed(ramSkull))),
        )
    }

    public companion object {
        /** Quest Helper's eight-bone cutoff list places Ram fourth, at zero-based bit three. */
        public const val RAM_SKULL_INDEX: Int = 3
    }
}
