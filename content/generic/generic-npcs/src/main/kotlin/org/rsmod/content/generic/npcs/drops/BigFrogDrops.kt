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

/** Complete cache-233.4 table for the loaded level-ten Big frog. */
public class BigFrogDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val clueDrops: NpcClueDropRolls,
    private val questBones: RagAndBoneManIDropPolicy,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val bigFrogLeg =
            RagAndBoneManIDropPolicy.QuestBone(
                raw = objs.big_frog_leg,
                inVinegar = objs.big_frog_leg_in_vinegar,
                polished = objs.polished_big_frog_leg,
                handedIndex = BIG_FROG_LEG_INDEX,
            )
        registry.register(
            npcs.big_frog,
            NpcDropTable(
                sequence(
                    item(objs.bones),
                    questBones.guaranteed(bigFrogLeg),
                    clueDrops.independent(
                        tier = ClueTier.Beginner,
                        clueScroll = objs.clue_scroll_beginner,
                        denominator = BEGINNER_CLUE_DENOMINATOR,
                    ),
                )
            ),
        )
    }

    public companion object {
        /** Quest Helper's eight-bone cutoff list places Big frog sixth. */
        public const val BIG_FROG_LEG_INDEX: Int = 5
        public const val BEGINNER_CLUE_DENOMINATOR: Int = 70
    }
}
