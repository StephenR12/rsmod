package org.rsmod.content.skills.fletching.ogrearrowshafts

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class OgreArrowShaftFletchingModule : PluginModule() {
    override fun bind() {
        bind(OgreArrowShaftFletching::class.java).`in`(Scopes.SINGLETON)
    }
}
