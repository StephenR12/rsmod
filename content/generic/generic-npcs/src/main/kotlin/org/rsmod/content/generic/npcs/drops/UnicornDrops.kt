package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcEnsouledHeadDropRolls
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Final pre-cache-233-cutoff drop table for the loaded ordinary Unicorn. */
public class UnicornDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val questBones: RagAndBoneManIDropPolicy,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val unicornBone =
            RagAndBoneManIDropPolicy.QuestBone(
                raw = objs.unicorn_bone,
                inVinegar = objs.unicorn_bone_in_vinegar,
                polished = objs.polished_unicorn_bone,
                handedIndex = UNICORN_BONE_INDEX,
            )
        registry.register(
            npcs.unicorn,
            NpcDropTable(
                sequence(
                    item(objs.bones),
                    conditional({ it.hero.members }, item(objs.unicorn_horn)),
                    questBones.guaranteed(unicornBone),
                    conditional(
                        { it.hero.members },
                        NpcEnsouledHeadDropRolls.independent(
                            objs.ensouled_unicorn_head_initial,
                            ENSOULED_HEAD_DENOMINATOR,
                        ),
                    ),
                )
            ),
        )
    }

    public companion object {
        /** Quest Helper's eight-bone cutoff list places Unicorn second. */
        public const val UNICORN_BONE_INDEX: Int = 1
        public const val ENSOULED_HEAD_DENOMINATOR: Int = 35
    }
}
