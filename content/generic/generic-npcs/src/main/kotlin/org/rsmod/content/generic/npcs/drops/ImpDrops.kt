package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcChampionScrollDropRolls
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcEnsouledHeadDropRolls
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Final pre-cache-233-cutoff table for the loaded ordinary Imp (NPC 5007). */
public class ImpDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            npcs.imp,
            NpcDropTable(
                sequence(
                    item(objs.fiendish_ashes),
                    weighted(
                        NpcWeightedDrop(5, item(objs.black_bead)),
                        NpcWeightedDrop(5, item(objs.red_bead)),
                        NpcWeightedDrop(5, item(objs.white_bead)),
                        NpcWeightedDrop(5, item(objs.yellow_bead)),
                        NpcWeightedDrop(8, item(objs.bronze_bolts)),
                        NpcWeightedDrop(8, item(objs.blue_wizard_hat)),
                        NpcWeightedDrop(5, item(objs.egg)),
                        NpcWeightedDrop(5, item(objs.raw_chicken)),
                        NpcWeightedDrop(4, item(objs.burnt_bread)),
                        NpcWeightedDrop(4, item(objs.burnt_meat)),
                        NpcWeightedDrop(2, item(objs.cabbage)),
                        NpcWeightedDrop(2, item(objs.bread_dough)),
                        NpcWeightedDrop(1, item(objs.bread)),
                        NpcWeightedDrop(1, item(objs.cooked_meat)),
                        NpcWeightedDrop(8, item(objs.hammer)),
                        NpcWeightedDrop(5, item(objs.tinderbox)),
                        NpcWeightedDrop(4, item(objs.shears)),
                        NpcWeightedDrop(4, item(objs.bucket_empty)),
                        NpcWeightedDrop(2, item(objs.bucket_water)),
                        NpcWeightedDrop(2, item(objs.jug_empty)),
                        NpcWeightedDrop(2, item(objs.jug_water)),
                        NpcWeightedDrop(2, item(objs.pot_empty)),
                        NpcWeightedDrop(2, item(objs.pot_of_flour)),
                        NpcWeightedDrop(8, item(objs.ball_of_wool)),
                        NpcWeightedDrop(7, item(objs.mind_talisman)),
                        NpcWeightedDrop(6, item(objs.ashes)),
                        NpcWeightedDrop(4, item(objs.clay)),
                        NpcWeightedDrop(4, item(objs.cadava_berries)),
                        NpcWeightedDrop(3, item(objs.grain)),
                        NpcWeightedDrop(2, item(objs.chefs_hat)),
                        NpcWeightedDrop(2, item(objs.flyer)),
                        NpcWeightedDrop(1, item(objs.apothecary_potion)),
                    ),
                    conditional(
                        { it.hero.members },
                        NpcEnsouledHeadDropRolls.independent(
                            objs.ensouled_imp_head_initial,
                            ENSOULED_HEAD_DENOMINATOR,
                        ),
                    ),
                    NpcChampionScrollDropRolls.independent(
                        type = objs.imp_champion_scroll,
                        questPoints = varps.quest_points,
                        completed = varbits.champions_defeated_imp,
                    ),
                )
            ),
        )
    }

    public companion object {
        public const val ENSOULED_HEAD_DENOMINATOR: Int = 25
    }
}
