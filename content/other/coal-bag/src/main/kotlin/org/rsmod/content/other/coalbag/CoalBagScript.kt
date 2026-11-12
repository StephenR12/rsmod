package org.rsmod.content.other.coalbag

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class CoalBagScript @Inject constructor(private val storage: CoalBagStorage) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (bag in CoalBagStorage.variants) {
            onOpHeld1(bag) { storage.fillFromInventory(this) }
            onOpHeld2(bag) { storage.toggle(this, bag, it.slot) }
            onOpHeld3(bag) { storage.check(this) }
            onOpHeld4(bag) { storage.emptyToInventory(this) }
            onOpHeld5(bag) { storage.destroy(this, bag, it.slot) }
            onOpHeldU(objs.coal, bag) { storage.fillFromInventory(this) }
            onOpBankside(bag) { storage.bankside(player) }
        }
    }
}
