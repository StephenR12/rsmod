package org.rsmod.content.other.coalbag

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class CoalBagModule : PluginModule() {
    override fun bind() {
        bind(CoalBagStorage::class.java).`in`(Scopes.SINGLETON)
    }
}
