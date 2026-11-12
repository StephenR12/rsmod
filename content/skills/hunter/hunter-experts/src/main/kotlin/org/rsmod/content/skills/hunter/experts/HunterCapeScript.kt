package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpWorn1
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.plugin.scripts.ScriptContext

public class HunterCapeScript
@Inject
constructor(
    private val teleport: HunterCapeTeleport,
    private val guildmaster: HunterCapeGuildmasterDialogue,
) {
    public fun startup(context: ScriptContext) {
        with(context) {
            for (cape in listOf(objs.hunter_skillcape, objs.hunter_skillcape_t)) {
                onOpHeld3(cape) { teleport.select(this) }
                onOpWorn1(cape) { boost(this) }
                onOpWorn2(cape) { teleport.teleport(this, HunterCapeDestination.HunterGuild) }
                onOpWorn3(cape) { teleport.teleport(this, HunterCapeDestination.FeldipHunterArea) }
                onOpWorn4(cape) {
                    teleport.teleport(this, HunterCapeDestination.WildernessHunterArea)
                }
            }
            onOpNpc1(npcs.hg_apatura) { guildmaster.talk(this, it.npc) }
        }
    }

    public fun boost(access: ProtectedAccess) {
        access.statBoost(stats.hunter, constant = 1, percent = 0)
    }
}
