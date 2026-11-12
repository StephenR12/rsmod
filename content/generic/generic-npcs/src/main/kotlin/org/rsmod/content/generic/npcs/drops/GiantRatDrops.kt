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
import org.rsmod.game.type.npc.NpcType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Complete regular table for every Giant rat identity loaded in the cache-233.4 world. */
public class GiantRatDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val clueDrops: NpcClueDropRolls,
    private val questBones: RagAndBoneManIDropPolicy,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val giantRatBone =
            RagAndBoneManIDropPolicy.QuestBone(
                raw = objs.giant_rat_bone,
                inVinegar = objs.giant_rat_bone_in_vinegar,
                polished = objs.polished_giant_rat_bone,
                handedIndex = GIANT_RAT_BONE_INDEX,
            )
        val table =
            NpcDropTable(
                sequence(
                    item(objs.bones),
                    item(objs.raw_rat_meat),
                    questBones.guaranteed(giantRatBone),
                    clueDrops.independent(
                        tier = ClueTier.Beginner,
                        clueScroll = objs.clue_scroll_beginner,
                        denominator = BEGINNER_CLUE_DENOMINATOR,
                    ),
                )
            )
        loadedTypes.forEach { registry.register(it, table) }
    }

    public companion object {
        /** Quest Helper's eight-bone cutoff list places Giant rat first. */
        public const val GIANT_RAT_BONE_INDEX: Int = 0
        public const val BEGINNER_CLUE_DENOMINATOR: Int = 128

        public val loadedTypes: Set<NpcType> =
            setOf(
                npcs.giant_rat,
                npcs.giant_rat_grey,
                npcs.giant_rat_grey_2,
                npcs.giant_rat_grey_3,
                npcs.giant_rat_level_6,
                npcs.giant_rat_level_6_2,
            )
    }
}
