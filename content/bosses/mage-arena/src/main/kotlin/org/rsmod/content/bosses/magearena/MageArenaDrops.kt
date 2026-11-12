package org.rsmod.content.bosses.magearena

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MageArenaDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(MageArenaNpcs.zachariah, NpcDropTable(item(MageArenaObjs.justiciarsHand)))
        registry.register(MageArenaNpcs.derwen, NpcDropTable(item(MageArenaObjs.entsRoots)))
        registry.register(MageArenaNpcs.porazdir, NpcDropTable(item(MageArenaObjs.demonsHeart)))
    }
}
