package org.rsmod.content.skills.hunter.birdhouses

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class BirdhouseTrapModule : PluginModule() {
    override fun bind() {
        bind(BirdhouseClock::class.java).to(SystemBirdhouseClock::class.java).`in`(Scopes.SINGLETON)
        bind(BirdhouseTrap::class.java).`in`(Scopes.SINGLETON)
    }
}
