package org.rsmod.api.shops

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object OmniButtons : ComponentReferences(){
    val list=find("omnishop_main:list")
    val items=find("omnishop_side:items")
    val quantities=(1..4).flatMap{listOf(find("omnishop_main:button_$it"),find("omnishop_side:button_$it"))}
}
internal object OmniVars:VarpReferences(){val shop=find("omnishop_lastshop")}
private class ShopButton(val input: IfModalButton, shop:Int):SuspendEvent<ProtectedAccess>{
    override val id:Long=EventBus.composeLongKey(shop,input.component.packed)
}

/** The native reward shops share the same interface. Route each button to the displayed DB row. */
public class OmniShopButtons : PluginScript(){
    override fun ScriptContext.startup(){
        for(button in listOf(OmniButtons.list,OmniButtons.items)+OmniButtons.quantities)onIfModalButton(button){
            publish(ShopButton(it,vars[OmniVars.shop]))
        }
    }
}
public fun ScriptContext.onOmniShopButton(shop:Int,button:ComponentType,action:suspend ProtectedAccess.(IfModalButton)->Unit){
    onProtectedEvent<ShopButton>(EventBus.composeLongKey(shop,button.packed)){action(it.input)}
}
