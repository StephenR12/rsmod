package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Complete regular table for the Rat identity loaded in the cache-233.4 world. */
public class RatDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val questBones: RagAndBoneManIIDropPolicy,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val ratBone =
            RagAndBoneManIIDropPolicy.QuestBone(
                raw = objs.rat_bone,
                inVinegar = objs.rat_bone_in_vinegar,
                polished = objs.polished_rat_bone,
                handedIndex = RAT_BONE_INDEX,
            )
        registry.register(
            npcs.rat,
            NpcDropTable(
                sequence(
                    conditional(
                        {
                            it.hero.vars[varps.hetty] in
                                WITCHS_POTION_STARTED until WITCHS_POTION_COMPLETE
                        },
                        item(objs.rats_tail),
                    ),
                    questBones.independent(ratBone),
                )
            ),
        )
    }

    public companion object {
        /** Cutoff Quest Helper's 27-bone list places Rat seventh. */
        public const val RAT_BONE_INDEX: Int = 6
        public const val WITCHS_POTION_STARTED: Int = 1
        public const val WITCHS_POTION_COMPLETE: Int = 3
    }
}
