package org.rsmod.content.skills.magic.spell.utility.arceuus

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.magic.ShadowVeilStateRepository
import org.rsmod.api.player.righthand
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.MagicSpellRegistry
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
import org.rsmod.api.testing.random.FixedRandom
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList

@Execution(ExecutionMode.SAME_THREAD)
class ShadowVeilSpellTest {
    @Test
    fun GameTestState.`cache 238 pins spell state and presentation contracts`() =
        runShadowVeilTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_shadow_veil))
            assertEquals(15_344, spell.obj.id)
            assertEquals(47, spell.levelReq)
            assertEquals(58.0, spell.castXp)
            assertEquals(Spellbook.Arceuus, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.earth_rune to 5, objs.fire_rune to 5, objs.cosmic_rune to 5),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(218, components.magic_spellbook_shadow_veil.id ushr 16)
            assertEquals(185, components.magic_spellbook_shadow_veil.id and 0xFFFF)
            assertEquals("Cast", deps.components[components.magic_spellbook_shadow_veil].targetVerb)
            assertEquals(8_979, seqs.human_spellcast_shadow_veil.id)
            assertEquals(8_998, seqs.shadow_veil_effect.id)
            assertEquals(1_881, spotanims.shadow_veil_cast.id)
            assertEquals(12_291, varbits.arceuus_shadow_veil_cooldown.id)
            assertEquals(12_414, varbits.arceuus_shadow_veil_active.id)
            assertEquals(15, ShadowVeilStateRepository.PROTECTION_PERCENT)
        }

    @Test
    fun GameTestState.`cast snapshots base Magic and clears cooldown and active at exact ticks`() =
        runShadowVeilTest { deps ->
            prepareCaster(deps, casts = 1, baseMagic = 57, currentMagic = 99)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(580, player.statMap.getFineXP(stats.magic))
            assertEquals(0, player.count(objs.earth_rune))
            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(0, player.count(objs.cosmic_rune))
            assertEquals(57, deps.states.remainingCycles(player))
            assertEquals(1, player.vars[varbits.arceuus_shadow_veil_active])
            assertEquals(1, player.vars[varbits.arceuus_shadow_veil_cooldown])
            assertEquals(seqs.human_spellcast_shadow_veil.id, player.pendingSequence.id)
            assertEquals(
                spotanims.shadow_veil_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )

            advance(ShadowVeilSpell.COOLDOWN_CYCLES)
            assertEquals(0, player.vars[varbits.arceuus_shadow_veil_cooldown])
            assertEquals(7, deps.states.remainingCycles(player))
            advance(7)
            assertFalse(deps.states.isActive(player))
            assertEquals(0, player.vars[varbits.arceuus_shadow_veil_active])
        }

    @Test
    fun GameTestState.`cooldown blocks then permits an active buff refresh`() =
        runShadowVeilTest { deps ->
            prepareCaster(deps, casts = 2, baseMagic = 99, currentMagic = 99)
            player.withProtectedAccess { deps.spell.cast(this) }
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(5, player.count(objs.earth_rune))
            assertEquals(99, deps.states.remainingCycles(player))

            advance(ShadowVeilSpell.COOLDOWN_CYCLES)
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(0, player.count(objs.earth_rune))
            assertEquals(99, deps.states.remainingCycles(player))
        }

    @Test
    fun GameTestState.`protection roll has an exact fifteen percent boundary only while active`() =
        runShadowVeilTest { deps ->
            prepareCaster(deps, casts = 0, baseMagic = 99, currentMagic = 99)
            deps.states.activate(player, 10)
            val roll = FixedRandom(14)
            assertTrue(deps.states.protects(player, roll))
            roll.set(15)
            assertFalse(deps.states.protects(player, roll))
            deps.states.clear(player)
            roll.set(0)
            assertFalse(deps.states.protects(player, roll))
        }

    @Test
    fun GameTestState.`access level rune and Fountain paths are atomic`() =
        runShadowVeilTest { deps ->
            prepareCaster(deps, casts = 1, baseMagic = 99, currentMagic = 99)
            player.members = false
            reject(deps)
            player.members = true
            player.setVarBit(varbits.a_kingdom_divided, 149)
            reject(deps)
            player.setVarBit(varbits.a_kingdom_divided, 150)
            player.stats[stats.magic] = 46
            reject(deps)
            player.stats[stats.magic] = 99
            player.inv[2] = null
            reject(deps)

            prepareCaster(deps, casts = 0, baseMagic = 99, currentMagic = 99)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            player.withProtectedAccess { deps.spell.cast(this) }
            assertTrue(deps.states.isActive(player))
            assertEquals(0, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.reject(deps: Dependencies) {
        val inventory = player.inv.toList()
        val xp = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess { deps.spell.cast(this) }
        assertEquals(inventory, player.inv.toList())
        assertEquals(xp, player.statMap.getFineXP(stats.magic))
        assertFalse(deps.states.isActive(player))
        assertEquals(0, player.vars[varbits.arceuus_shadow_veil_cooldown])
    }

    private fun GameTestScope.prepareCaster(
        deps: Dependencies,
        casts: Int,
        baseMagic: Int,
        currentMagic: Int,
    ) {
        player.clearAllInvs()
        player.members = true
        player.setBaseLevel(stats.magic, baseMagic)
        player.setCurrentLevel(stats.magic, currentMagic)
        player.statMap.setXP(stats.magic, 0)
        player.setVarBit(varbits.a_kingdom_divided, 150)
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.arceuus_shadow_veil_cooldown, 0)
        player.setVarBit(varbits.arceuus_shadow_veil_active, 0)
        player.righthand = null
        deps.states.clear(player)
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.earth_rune, casts * 5)
            player.inv[1] = InvObj(objs.fire_rune, casts * 5)
            player.inv[2] = InvObj(objs.cosmic_rune, casts * 5)
        }
    }

    private fun GameTestState.runShadowVeilTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            ShadowVeilSpellScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(ShadowVeilStateRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val spell: ShadowVeilSpell,
        val states: ShadowVeilStateRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
    )
}
