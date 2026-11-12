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
import org.rsmod.api.death.NpcHerbDropRolls
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Exact full-table contract for the cache-233.4 Mugger loaded behind Lumbridge Castle. */
public class MuggerDrops
@Inject
constructor(private val registry: NpcDropTableRegistry, private val clueDrops: NpcClueDropRolls) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            npcs.mugger,
            NpcDropTable(
                sequence(
                    item(objs.bones),
                    weighted(
                        NpcWeightedDrop(
                            27,
                            conditional({ it.hero.members }, item(objs.bronze_bolts, 2..12)),
                        ),
                        NpcWeightedDrop(3, item(objs.mind_rune, 9..9)),
                        NpcWeightedDrop(2, item(objs.water_rune, 6..6)),
                        NpcWeightedDrop(2, item(objs.earth_rune, 5..5)),
                        NpcWeightedDrop(
                            13,
                            sequence(
                                conditional({ it.hero.members }, NpcHerbDropRolls.standard()),
                                conditional({ !it.hero.members }, item(objs.coins, 10..10)),
                            ),
                        ),
                        NpcWeightedDrop(12, item(objs.coins, 5..5)),
                        NpcWeightedDrop(3, item(objs.coins, 15..15)),
                        NpcWeightedDrop(1, item(objs.coins, 25..25)),
                        NpcWeightedDrop(40, item(objs.rope)),
                        NpcWeightedDrop(13, nothing()),
                        NpcWeightedDrop(1, item(objs.knife)),
                        NpcWeightedDrop(6, item(objs.fishing_bait)),
                        NpcWeightedDrop(2, item(objs.copper_ore)),
                        NpcWeightedDrop(2, item(objs.bronze_med_helm)),
                        NpcWeightedDrop(1, item(objs.cabbage)),
                    ),
                    clueDrops.independent(
                        tier = ClueTier.Beginner,
                        clueScroll = objs.clue_scroll_beginner,
                        denominator = 80,
                    ),
                    clueDrops.independentEasy(denominator = 128),
                )
            ),
        )
    }
}
