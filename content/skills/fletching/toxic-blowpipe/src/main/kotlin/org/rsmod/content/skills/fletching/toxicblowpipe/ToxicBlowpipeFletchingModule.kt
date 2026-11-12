package org.rsmod.content.skills.fletching.toxicblowpipe

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class ToxicBlowpipeFletchingModule : PluginModule() {
    override fun bind() {
        bind(ToxicBlowpipeFletching::class.java).`in`(Scopes.SINGLETON)
        bind(ToxicBlowpipeLifecycle::class.java).`in`(Scopes.SINGLETON)
    }
}
