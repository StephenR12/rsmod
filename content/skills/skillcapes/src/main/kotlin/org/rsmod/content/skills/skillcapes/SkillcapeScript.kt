package org.rsmod.content.skills.skillcapes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.script.advanced.onAdvanceStat
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.api.script.onOpWorn1
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class SkillcapeScript
@Inject
constructor(private val trimming: SkillcapeTrimming, private val boosts: SkillcapeBoost) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onAdvanceStat { trimming.onLevelUp(this, it.args) }

        for (definition in SkillcapeTrimming.definitions) {
            if (definition.stat != stats.hunter && definition.stat != stats.agility) {
                for (cape in listOf(definition.untrimmed, definition.trimmed)) {
                    onOpWorn1(cape) { boosts.activate(player, definition.stat) }
                }
            }
        }
        onOpWorn1(objs.max_cape_worn) {
            for (definition in SkillcapeTrimming.definitions) {
                boosts.activate(player, definition.stat)
            }
        }

        val statByCape =
            SkillcapeTrimming.definitions
                .flatMap { definition ->
                    listOf(
                        definition.untrimmed.id to definition.stat,
                        definition.trimmed.id to definition.stat,
                    )
                }
                .toMap()
        onWearposChange {
            if (wearpos != Wearpos.Back) {
                return@onWearposChange
            }
            val wornCape = player.worn[Wearpos.Back.slot] ?: return@onWearposChange
            if (wornCape.id == objs.max_cape_worn.id) {
                for (definition in SkillcapeTrimming.definitions) {
                    boosts.activate(player, definition.stat)
                }
                return@onWearposChange
            }
            statByCape[wornCape.id]?.let { boosts.activate(player, it) }
        }
    }
}
