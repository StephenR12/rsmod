package org.rsmod.api.specials.scripts

import jakarta.inject.Inject
import org.rsmod.api.specials.SpecialAttackBootstrap
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class SpecialAttackScript
@Inject
constructor(private val bootstrap: SpecialAttackBootstrap) : PluginScript() {
    override fun ScriptContext.startup() {
        bootstrap.startup()
    }
}
