package org.rsmod.content.skills.hunter.birdsnares

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class BirdSnareModule : PluginModule() {
    override fun bind() {
        bind(BirdSnarePlacement::class.java).`in`(Scopes.SINGLETON)
        bind(BirdSnareTrap::class.java).`in`(Scopes.SINGLETON)
    }
}
