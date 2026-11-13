package org.rsmod.content.skills.woodcutting.forestry

import org.rsmod.api.player.hands
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TwitcherSave : VarpBuilder() {
    init {
        build("twitchers_gloves_nest") { permanent = true }
    }
}

internal object TwitcherVars : VarpReferences() {
    val nest = find("twitchers_gloves_nest")
}

internal object TwitcherItems : ObjReferences() {
    val gloves = find("forestry_gloves")
}

public class TwitchersGloves : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(TwitcherItems.gloves) { choose() }
        onOpWorn2(TwitcherItems.gloves) { choose() }
    }

    private suspend fun ProtectedAccess.choose() {
        if (!player.members) return
        vars[TwitcherVars.nest] =
            choice4("Ring nests.", 1, "Egg nests.", 2, "Seed nests.", 3, "Clue nests.", 4)
    }

    public companion object {
        public fun preference(player: Player): Int =
            if (player.members && player.hands?.id == TwitcherItems.gloves.id)
                player.vars[TwitcherVars.nest]
            else 0
    }
}
