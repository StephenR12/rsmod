package org.rsmod.content.other.gembag

import com.google.inject.Scopes
import org.rsmod.api.obj.plugin.ObjTakeDestination
import org.rsmod.plugin.module.PluginModule

public class GemBagModule : PluginModule() {
    override fun bind() {
        bind(GemBagStorage::class.java).`in`(Scopes.SINGLETON)
        addSetBinding<ObjTakeDestination>(GemBagGroundObjDestination::class.java)
    }
}
