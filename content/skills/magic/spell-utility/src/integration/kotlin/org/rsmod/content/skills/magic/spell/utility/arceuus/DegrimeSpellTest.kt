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
import org.rsmod.api.config.refs.varps
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
import org.rsmod.content.skills.herblore.HerbCleaningRecipes
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class DegrimeSpellTest {
    @Test
    fun GameTestState.`cache 238 pins spell runes component and all presentation assets`() =
        runDegrimeTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_degrime))
            assertEquals(15_345, spell.obj.id)
            assertEquals(70, spell.levelReq)
            assertEquals(83.0, spell.castXp)
            assertEquals(Spellbook.Arceuus, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.earth_rune to 4, objs.nature_rune to 2),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(218, components.magic_spellbook_degrime.id ushr 16)
            assertEquals(184, components.magic_spellbook_degrime.id and 0xFFFF)
            assertEquals("Cast", deps.components[components.magic_spellbook_degrime].targetVerb)
            assertEquals(8_980, seqs.human_spellcast_degrime.id)
            assertEquals(8_981, seqs.human_prepare_degrime.id)
            assertEquals(9_002, seqs.degrime_effect.id)
            assertEquals(1_885, spotanims.degrime_cast.id)
        }

    @Test
    fun GameTestState.`one cast cleans every standard herb and floors half xp per herb`() =
        runDegrimeTest { deps ->
            prepareCaster(casts = 1, herblore = 99)
            HerbCleaningRecipes.all.forEachIndexed { index, recipe ->
                player.inv[index + 2] = InvObj(recipe.grimy)
            }
            val expectedFineXp = HerbCleaningRecipes.all.sumOf { (it.xp * 10).toInt() / 2 }

            player.withProtectedAccess { deps.spell.cast(this) }

            assertTrue(HerbCleaningRecipes.all.all { player.count(it.grimy) == 0 })
            assertTrue(HerbCleaningRecipes.all.all { player.count(it.clean) == 1 })
            assertEquals(expectedFineXp, player.statMap.getFineXP(stats.herblore))
            assertEquals(830, player.statMap.getFineXP(stats.magic))
            assertEquals(0, player.count(objs.earth_rune))
            assertEquals(0, player.count(objs.nature_rune))
            assertEquals(seqs.human_spellcast_degrime.id, player.pendingSequence.id)
            assertEquals(
                spotanims.degrime_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
        }

    @Test
    fun GameTestState
        .`boosted Herblore cleans only eligible mixed herbs in their original slots`() =
        runDegrimeTest { deps ->
            prepareCaster(casts = 1, herblore = 70, baseHerblore = 65)
            val guam = HerbCleaningRecipes.all.first()
            val dwarf = HerbCleaningRecipes.all.first { it.grimy == objs.grimy_dwarf_weed }
            val torstol = HerbCleaningRecipes.all.last()
            player.inv[2] = InvObj(guam.grimy)
            player.inv[4] = InvObj(dwarf.grimy)
            player.inv[6] = InvObj(torstol.grimy)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(guam.clean.id, player.inv[2]?.id)
            assertEquals(dwarf.clean.id, player.inv[4]?.id)
            assertEquals(torstol.grimy.id, player.inv[6]?.id)
            assertEquals(12 + 69, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`eight tick interval blocks a second batch until the exact boundary`() =
        runDegrimeTest { deps ->
            prepareCaster(casts = 2, herblore = 99)
            player.inv[2] = InvObj(objs.grimy_guam_leaf)
            player.withProtectedAccess { deps.spell.cast(this) }
            player.inv[2] = InvObj(objs.grimy_guam_leaf)

            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(1, player.count(objs.grimy_guam_leaf))
            assertEquals(4, player.count(objs.earth_rune))
            advance(DegrimeSpell.CAST_INTERVAL_CYCLES - 1)
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(1, player.count(objs.grimy_guam_leaf))

            advance()
            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(0, player.count(objs.grimy_guam_leaf))
            assertEquals(0, player.count(objs.earth_rune))
        }

    @Test
    fun GameTestState.`access resource no herb and routed movement failures are atomic`() =
        runDegrimeTest { deps ->
            prepareCaster(casts = 1, herblore = 99)
            player.inv[2] = InvObj(objs.grimy_guam_leaf)

            player.members = false
            reject(deps)
            player.members = true
            player.setVarBit(varbits.a_kingdom_divided, 149)
            reject(deps)
            player.setVarBit(varbits.a_kingdom_divided, 150)
            player.setVarp(varps.druidic_ritual, 3)
            reject(deps)
            player.setVarp(varps.druidic_ritual, 4)
            player.stats[stats.magic] = 69
            reject(deps)
            player.stats[stats.magic] = 99
            player.inv[0] = null
            reject(deps)

            prepareCaster(casts = 1, herblore = 2)
            player.inv[2] = InvObj(objs.grimy_guam_leaf)
            reject(deps)

            prepareCaster(casts = 1, herblore = 99)
            player.inv[2] = InvObj(objs.grimy_guam_leaf)
            player.routeDestination.add(CoordGrid(3_201, 3_200, 0))
            reject(deps)
        }

    @Test
    fun GameTestState.`Fountain supplies runes and suppresses only Magic xp`() =
        runDegrimeTest { deps ->
            prepareCaster(casts = 0, herblore = 99)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            player.inv[0] = InvObj(objs.grimy_toadflax)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(0, player.count(objs.grimy_toadflax))
            assertEquals(1, player.count(objs.toadflax))
            assertEquals(40, player.statMap.getFineXP(stats.herblore))
            assertEquals(0, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.reject(deps: Dependencies) {
        val inventory = player.inv.toList()
        val magicXp = player.statMap.getFineXP(stats.magic)
        val herbloreXp = player.statMap.getFineXP(stats.herblore)
        player.withProtectedAccess { deps.spell.cast(this) }
        assertEquals(inventory, player.inv.toList())
        assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
        assertEquals(herbloreXp, player.statMap.getFineXP(stats.herblore))
        assertFalse(deps.cooldowns.isActive(player))
    }

    private fun GameTestScope.prepareCaster(
        casts: Int,
        herblore: Int,
        baseHerblore: Int = herblore,
    ) {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.magic] = 99
        player.setBaseLevel(stats.herblore, baseHerblore)
        player.setCurrentLevel(stats.herblore, herblore)
        player.statMap.setXP(stats.magic, 0)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarBit(varbits.a_kingdom_divided, 150)
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarp(varps.druidic_ritual, 4)
        player.routeDestination.clear()
        player.righthand = null
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.earth_rune, casts * 4)
            player.inv[1] = InvObj(objs.nature_rune, casts * 2)
        }
    }

    private fun GameTestState.runDegrimeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            DegrimeSpellScript::class,
            testBody = { deps ->
                deps.cooldowns.clear(player)
                body(deps)
            },
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
            bind(DegrimeCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val spell: DegrimeSpell,
        val cooldowns: DegrimeCooldownRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
    )
}
