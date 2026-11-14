package org.rsmod.content.skills.magic.spell.utility.arceuus

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
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
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList

@Execution(ExecutionMode.SAME_THREAD)
class VileVigourSpellTest {
    @Test
    fun GameTestState.`cache 238 pins the spell and presentation contracts`() =
        runVileVigourTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_vile_vigour))

            assertEquals(15304, spell.obj.id)
            assertEquals(66, spell.levelReq)
            assertEquals(76.0, spell.castXp)
            assertEquals(Spellbook.Arceuus, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.air_rune to 3, objs.soul_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(218, components.magic_spellbook_vile_vigour.id ushr 16)
            assertEquals(186, components.magic_spellbook_vile_vigour.id and 0xFFFF)
            assertEquals("Cast", deps.components[components.magic_spellbook_vile_vigour].targetVerb)
            assertEquals(8978, seqs.human_cast_vile_vigour.id)
            assertEquals(1876, spotanims.vile_vigour_cast.id)
            assertEquals(12292, varbits.arceuus_vile_vigour_cooldown.id)
            assertEquals(10_000, constants.run_max_energy)
        }

    @Test
    fun GameTestState.`cast consumes only enough prayer to fill run and starts exact cooldown`() =
        runVileVigourTest { deps ->
            prepareCaster(casts = 2)
            player.stats[stats.prayer] = 70
            player.runEnergy = 4_200
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(12, player.stats[stats.prayer])
            assertEquals(10_000, player.runEnergy)
            assertEquals(3, player.count(objs.air_rune))
            assertEquals(1, player.count(objs.soul_rune))
            assertEquals(760, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.human_cast_vile_vigour.id, player.pendingSequence.id)
            assertEquals(
                spotanims.vile_vigour_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(1, player.vars[varbits.arceuus_vile_vigour_cooldown])

            player.runEnergy = 9_000
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(3, player.count(objs.air_rune))
            assertEquals(12, player.stats[stats.prayer])

            advance(VileVigourSpell.COOLDOWN_CYCLES)
            assertEquals(0, player.vars[varbits.arceuus_vile_vigour_cooldown])
        }

    @Test
    fun GameTestState.`insufficient prayer converts completely and no effect casts are atomic`() =
        runVileVigourTest { deps ->
            prepareCaster(casts = 2)
            player.stats[stats.prayer] = 7
            player.runEnergy = 1_200

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(0, player.stats[stats.prayer])
            assertEquals(1_900, player.runEnergy)
            advance(VileVigourSpell.COOLDOWN_CYCLES)

            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(3, player.count(objs.air_rune))
            assertEquals(1, player.count(objs.soul_rune))
            assertEquals(0, player.vars[varbits.arceuus_vile_vigour_cooldown])

            player.stats[stats.prayer] = 10
            player.runEnergy = constants.run_max_energy
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(3, player.count(objs.air_rune))
            assertEquals(10, player.stats[stats.prayer])
            assertEquals(0, player.vars[varbits.arceuus_vile_vigour_cooldown])
        }

    @Test
    fun GameTestState.`access level runes and Fountain behavior are atomic`() =
        runVileVigourTest { deps ->
            prepareCaster(casts = 1)
            player.stats[stats.prayer] = 10
            player.runEnergy = 5_000

            player.members = false
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(3, player.count(objs.air_rune))
            player.members = true
            player.setVarBit(varbits.a_kingdom_divided, 149)
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(3, player.count(objs.air_rune))
            player.setVarBit(varbits.a_kingdom_divided, 150)
            player.stats[stats.magic] = 65
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(3, player.count(objs.air_rune))
            player.stats[stats.magic] = 99
            player.inv[1] = null
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(3, player.count(objs.air_rune))
            assertEquals(10, player.stats[stats.prayer])

            prepareCaster(casts = 0)
            player.stats[stats.prayer] = 10
            player.runEnergy = 5_000
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(0, player.count(objs.air_rune))
            assertEquals(0, player.count(objs.soul_rune))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
            assertEquals(6_000, player.runEnergy)
        }

    private fun GameTestScope.prepareCaster(casts: Int) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.a_kingdom_divided, 150)
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.arceuus_vile_vigour_cooldown, 0)
        player.clearInv()
        player.righthand = null
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.air_rune, casts * 3)
            player.inv[1] = InvObj(objs.soul_rune, casts)
        }
    }

    private fun GameTestState.runVileVigourTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            VileVigourSpellScript::class,
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
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val spell: VileVigourSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
    )
}
