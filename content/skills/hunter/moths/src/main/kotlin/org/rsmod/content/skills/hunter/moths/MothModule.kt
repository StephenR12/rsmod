package org.rsmod.content.skills.hunter.moths

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class MothModule : PluginModule() {
    override fun bind() {
        bind(MothHunting::class.java).`in`(Scopes.SINGLETON)
    }
}
