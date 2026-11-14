package org.rsmod.content.interfaces.combat.tab

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.righthand
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.autocast.scripts.AutocastScript
import org.rsmod.api.spells.runes.combo.ComboRuneRepository
import org.rsmod.api.spells.runes.combo.ComboRuneScript
import org.rsmod.api.spells.runes.compact.CompactRuneRepository
import org.rsmod.api.spells.runes.compact.CompactRuneScript
import org.rsmod.api.spells.runes.fake.FakeRuneRepository
import org.rsmod.api.spells.runes.fake.FakeRuneScript
import org.rsmod.api.spells.runes.staves.StaffSubstituteRepository
import org.rsmod.api.spells.runes.staves.StaffSubstituteScript
import org.rsmod.api.spells.runes.subs.RuneSubstituteRepository
import org.rsmod.api.spells.runes.subs.RuneSubstituteScript
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneRepository
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneScript
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.combat.tab.configs.combat_components
import org.rsmod.game.inv.InvObj

class AutocastSelectionTest {
    @Test
    fun GameTestState.`kodai wand selects and saves normal and defensive autocast`() =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            CombatTabScript::class,
            AutocastScript::class,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
        ) { deps ->
            val spellId = (1..127).single {
                deps.spells.getAutocastSpell(it)?.obj == objs.spell_wind_strike
            }
            player.righthand = InvObj(objs.kodai_wand)
            player.inv[0] = InvObj(objs.air_rune, 2)
            player.inv[1] = InvObj(objs.mind_rune, 2)
            player.ifOpenOverlay(interfaces.combat_interface, components.toplevel_target_side12)

            player.ifButton(combat_components.autocast_normal)
            advance()
            player.ifButton(combat_components.autocast_spells, spellId)
            advance()
            assertEquals(1, player.vars[varbits.autocast_set])
            assertEquals(spellId, player.vars[varbits.autocast_spell])
            assertEquals(spellId, player.vars[varbits.saved_autocast_spell_staff])
            assertEquals(0, player.vars[varbits.autocast_defmode])

            player.ifButton(combat_components.autocast_defensive)
            advance()
            player.ifButton(combat_components.autocast_spells, spellId)
            advance()
            assertEquals(1, player.vars[varbits.autocast_defmode])
            assertEquals(1, player.vars[varbits.saved_defensive_casting_staff])
        }

    data class Dependencies @Inject constructor(val spells: MagicSpellRegistry)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }
}
