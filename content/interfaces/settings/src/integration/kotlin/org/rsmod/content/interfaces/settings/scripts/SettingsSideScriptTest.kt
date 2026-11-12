package org.rsmod.content.interfaces.settings.scripts

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.settings.configs.setting_components

class SettingsSideScriptTest {
    @Test
    fun GameTestState.`close button closes the settings overlay`() =
        runGameTest(SettingsSideScript::class) {
            player.ifOpenOverlay(interfaces.settings, components.toplevel_target_floater)
            assertTrue(player.ui.containsOverlay(interfaces.settings))

            player.ifButton(setting_components.settings_close)
            advance()

            assertFalse(player.ui.containsOverlay(interfaces.settings))
        }
}
