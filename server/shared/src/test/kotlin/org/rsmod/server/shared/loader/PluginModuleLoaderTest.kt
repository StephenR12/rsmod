package org.rsmod.server.shared.loader

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.plugin.module.PluginModule

class PluginModuleLoaderTest {
    @Test
    fun `production runtime includes hunter birdhouse module`() {
        val modules =
            PluginModuleLoader.load(
                PluginModule::class.java,
                arrayOf("org.rsmod.content.skills.hunter.birdhouses"),
            )

        assertTrue(
            modules.any {
                it::class.qualifiedName ==
                    "org.rsmod.content.skills.hunter.birdhouses.BirdhouseTrapModule"
            },
            "Hunter birdhouse module is missing from the production runtime classpath.",
        )
    }
}
