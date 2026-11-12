package org.rsmod.content.skills.crafting.lunarclothing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LunarClothingScript @Inject constructor(private val crafting: LunarClothingCrafting) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(objs.needle, objs.suqah_leather) { crafting.select(this, costumeNeedle = false) }
        onOpHeldU(objs.costume_needle, objs.suqah_leather) {
            crafting.select(this, costumeNeedle = true)
        }
    }
}
