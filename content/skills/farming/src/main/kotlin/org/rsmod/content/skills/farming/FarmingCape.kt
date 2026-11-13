package org.rsmod.content.skills.farming

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.minigames.skillcapeTeleport
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object FarmingCapeBits : VarBitReferences() {
    val visited = find("zeah_playerhasvisited")
}

internal object FarmingCapeTrainers : NpcReferences() {
    val martin = find("martin_the_master_farmer")
}

public class FarmingCape : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(FarmingCapeTrainers.martin) {
            if (statBase(stats.farming) >= 99)
                SkillcapeSale.offer(this, stats.farming, objs.farming_hood)
            else mes("Come back when you have mastered Farming and I can sell you a cape.")
        }
        for (cape in listOf(objs.farming_skillcape, objs.farming_skillcape_t)) {
            onOpHeld3(cape) { travel() }
            onOpWorn3(cape) { travel() }
        }
    }

    public suspend fun teleportToGuild(access: ProtectedAccess): Unit = with(access) { travel() }

    private suspend fun ProtectedAccess.travel() {
        if (vars[FarmingCapeBits.visited] == 0) {
            mes("You must travel to Great Kourend before you can use this teleport.")
            return
        }
        skillcapeTeleport { CoordGrid(1248, 3725) }
    }
}
