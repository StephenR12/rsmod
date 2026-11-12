package org.rsmod.api.obj.plugin

import org.rsmod.plugin.module.PluginModule

public class ObjTakeModule : PluginModule() {
    override fun bind() {
        newSetBinding<ObjTakeDestination>()
        newSetBinding<ObjTakeTransform>()
    }
}
