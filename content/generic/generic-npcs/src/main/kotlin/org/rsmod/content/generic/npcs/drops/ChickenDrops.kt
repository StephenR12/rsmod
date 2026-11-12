package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcClueDropRolls
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Complete cache-233.4 drop table for every dated production Chicken identity. */
public class ChickenDrops
@Inject
constructor(private val registry: NpcDropTableRegistry, private val clueDrops: NpcClueDropRolls) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val table =
            NpcDropTable(
                sequence(
                    item(objs.bones),
                    item(objs.raw_chicken),
                    weighted(
                        NpcWeightedDrop(weight = 64, item(objs.feather, count = 5..5)),
                        NpcWeightedDrop(weight = 32, item(objs.feather, count = 15..15)),
                        NpcWeightedDrop(weight = 32, nothing()),
                    ),
                    conditional(
                        predicate = {
                            it.hero.members && it.hero.inv.contains(objs.chicken_clue_scroll_medium)
                        },
                        roll = item(objs.chicken_clue_key_medium),
                    ),
                    clueDrops.independent(
                        tier = ClueTier.Beginner,
                        clueScroll = objs.clue_scroll_beginner,
                        denominator = 300,
                    ),
                )
            )
        registry.register(CHICKENS, table)
    }

    private companion object {
        val CHICKENS =
            listOf(
                npcs.chicken,
                npcs.chicken_brown,
                npcs.farm_chicken_brown,
                npcs.farm_chicken_dark_brown,
                npcs.farm_chicken_tan,
                npcs.misc_chicken,
                npcs.misc_chicken_brown,
                npcs.tutorial_chicken,
                npcs.farm_chicken_brown_outdoors,
                npcs.farm_chicken_dark_brown_outdoors,
                npcs.farm_chicken_tan_outdoors,
                npcs.farm_chicken_brown_indoors,
            )
    }
}
