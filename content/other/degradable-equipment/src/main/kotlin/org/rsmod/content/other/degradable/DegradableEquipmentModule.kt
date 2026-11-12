package org.rsmod.content.other.degradable

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class DegradableEquipmentModule : PluginModule() {
    override fun bind() {
        bind(DegradableEquipmentLifecycle::class.java).`in`(Scopes.SINGLETON)
    }
}
