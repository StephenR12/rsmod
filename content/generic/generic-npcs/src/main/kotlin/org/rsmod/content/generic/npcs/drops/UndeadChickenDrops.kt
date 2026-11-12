package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Complete cache-233.4 drop table for the Animal Magnetism undead chicken. */
public class UndeadChickenDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            npcs.undead_chicken,
            NpcDropTable(
                sequence(
                    item(objs.bones),
                    item(objs.raw_chicken_undead),
                    weighted(
                        NpcWeightedDrop(weight = 64, item(objs.feather, count = 5..5)),
                        NpcWeightedDrop(weight = 32, item(objs.feather, count = 15..15)),
                        NpcWeightedDrop(weight = 32, nothing()),
                    ),
                )
            ),
        )
    }
}
