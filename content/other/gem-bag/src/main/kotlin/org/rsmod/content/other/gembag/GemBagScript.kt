package org.rsmod.content.other.gembag

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class GemBagScript @Inject constructor(private val storage: GemBagStorage) : PluginScript() {
    override fun ScriptContext.startup() {
        for (bag in listOf(objs.gem_bag, objs.gem_bag_open)) {
            onOpHeld1(bag) { storage.fill(this) }
            onOpHeld2(bag) { storage.toggle(this, bag, it.slot) }
            onOpHeld3(bag) { storage.check(this) }
            onOpHeld4(bag) { storage.emptyToInventory(this) }
            onOpBankside(bag) { storage.emptyToBank(player) }
            for (gem in GemBagStorage.accepted) {
                onOpHeldU(gem, bag) { storage.storeHeld(this, gem, it.firstSlot) }
            }
            for (gem in GemBagStorage.cut) {
                onOpHeldU(gem, bag) { storage.rejectCutGem(this) }
            }
        }
        onDestroyHeld {
            if (type.id == objs.gem_bag.id || type.id == objs.gem_bag_open.id) {
                storage.clear(player)
            }
        }
    }
}
