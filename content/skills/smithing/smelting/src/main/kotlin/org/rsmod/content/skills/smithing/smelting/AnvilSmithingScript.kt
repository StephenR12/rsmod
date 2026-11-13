package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AnvilSmithingScript
@Inject
constructor(private val smithing: AnvilSmithing, private val recipes: AnvilRecipes, private val blurite: BluriteLimbSmithing) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (anvil in AnvilLocs.ordinary + AnvilLocs.doric + AnvilLocs.rusted) {
            onOpLoc1(anvil) {
                if (anvil != AnvilLocs.rusted && objs.blurite_bar in player.inv && recipes.bars.none { bar -> bar in player.inv }) blurite.select(this, it.loc)
                else smithing.open(this, it.loc)
            }
            if (anvil != AnvilLocs.rusted) onOpLocU(anvil, objs.blurite_bar) { blurite.select(this, it.loc) }
            for (bar in recipes.bars) {
                onOpLocU(anvil, bar) { smithing.open(this, it.loc, bar) }
            }
        }
        for (component in AnvilComponents.products) {
            onIfModalButton(component) { smithing.choose(this, it) }
        }
        AnvilComponents.quantities.forEachIndexed { index, component ->
            onIfModalButton(component) { smithing.quantity(this, index) }
        }
        onIfClose(AnvilInterfaces.smithing) { smithing.close(player) }
        onPlayerLogout { smithing.close(player) }
    }
}
