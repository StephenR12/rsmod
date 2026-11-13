package org.rsmod.content.skills.mining

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.hands
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ClayVars : VarpReferences() {
    val used = find("bracelet_of_clay_uses")
}

internal object ClaySave : VarpBuilder() {
    init {
        build("bracelet_of_clay_uses") { permanent = true }
    }
}

public class ClayBracelet : PluginScript() {
    override fun ScriptContext.startup() {
        onOpWorn2(objs.bracelet_of_clay) {
            mes("Your bracelet of clay has ${28-vars[ClayVars.used]} charges remaining.")
        }
    }

    public fun use(a: ProtectedAccess) =
        with(a) {
            if (!player.members || player.hands?.id != objs.bracelet_of_clay.id) return@with
            val used = vars[ClayVars.used] + 1
            if (used < 28) vars[ClayVars.used] = used
            else {
                vars[ClayVars.used] = 0
                player.hands = null
                rebuildAppearance()
                mes("Your bracelet of clay disintegrates.")
            }
        }
}
