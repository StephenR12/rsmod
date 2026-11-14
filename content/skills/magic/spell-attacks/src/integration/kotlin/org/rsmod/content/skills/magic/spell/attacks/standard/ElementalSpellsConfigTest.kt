package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.objs
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackRegistry
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.obj.ObjType

class ElementalSpellsConfigTest {
    @Test
    fun GameTestState.`register every standard elemental combat spell`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val registry = SpellAttackRegistry()
            val repository = SpellAttackRepository(registry)
            with(deps.spells) { with(repository) { register(deps.manager) } }

            val elementalSpells =
                listOf(
                    objs.spell_wind_strike,
                    objs.spell_water_strike,
                    objs.spell_earth_strike,
                    objs.spell_fire_strike,
                    objs.spell_wind_bolt,
                    objs.spell_water_bolt,
                    objs.spell_earth_bolt,
                    objs.spell_fire_bolt,
                    objs.spell_wind_blast,
                    objs.spell_water_blast,
                    objs.spell_earth_blast,
                    objs.spell_fire_blast,
                    objs.spell_wind_wave,
                    objs.spell_water_wave,
                    objs.spell_earth_wave,
                    objs.spell_fire_wave,
                    objs.spell_wind_surge,
                    objs.spell_water_surge,
                    objs.spell_earth_surge,
                    objs.spell_fire_surge,
                )

            for (spell in elementalSpells) {
                assertNotNull(registry[spell], "No elemental attack registered for $spell")
            }
        }

    @Test
    fun GameTestState.`match cache 238 elemental spell requirements`() =
        runInjectedGameTest(Dependencies::class, childModule = MagicSpellModule()) { deps ->
            for (expected in elementalSpellContracts()) {
                val actual = deps.magicSpells.getObjSpell(expected.spell)
                assertNotNull(actual, "No cache spell definition for ${expected.spell}")
                requireNotNull(actual)

                assertEquals(Spellbook.Standard, actual.spellbook, actual.name)
                assertEquals(MagicSpellType.Combat, actual.type, actual.name)
                assertEquals(expected.level, actual.levelReq, actual.name)
                assertEquals(expected.castXp, actual.castXp, actual.name)
                assertEquals(expected.cacheMaxHit, actual.maxHit, actual.name)
                assertEquals(
                    expected.runes,
                    actual.objReqs.associate { requirement ->
                        requirement.obj to requirement.count
                    },
                    actual.name,
                )
            }
        }

    private fun elementalSpellContracts(): List<ElementalSpellContract> =
        listOf(
            contract(objs.spell_wind_strike, 1, 5.5, 2, objs.air_rune to 1, objs.mind_rune to 1),
            contract(
                objs.spell_water_strike,
                5,
                7.5,
                4,
                objs.air_rune to 1,
                objs.water_rune to 1,
                objs.mind_rune to 1,
            ),
            contract(
                objs.spell_earth_strike,
                9,
                9.5,
                6,
                objs.air_rune to 1,
                objs.earth_rune to 2,
                objs.mind_rune to 1,
            ),
            contract(
                objs.spell_fire_strike,
                13,
                11.5,
                8,
                objs.air_rune to 2,
                objs.fire_rune to 3,
                objs.mind_rune to 1,
            ),
            contract(objs.spell_wind_bolt, 17, 13.5, 9, objs.air_rune to 2, objs.chaos_rune to 1),
            contract(
                objs.spell_water_bolt,
                23,
                16.5,
                10,
                objs.air_rune to 2,
                objs.water_rune to 2,
                objs.chaos_rune to 1,
            ),
            contract(
                objs.spell_earth_bolt,
                29,
                19.5,
                11,
                objs.air_rune to 2,
                objs.earth_rune to 3,
                objs.chaos_rune to 1,
            ),
            contract(
                objs.spell_fire_bolt,
                35,
                22.5,
                12,
                objs.air_rune to 3,
                objs.fire_rune to 4,
                objs.chaos_rune to 1,
            ),
            contract(objs.spell_wind_blast, 41, 25.5, 13, objs.air_rune to 3, objs.death_rune to 1),
            contract(
                objs.spell_water_blast,
                47,
                28.5,
                14,
                objs.air_rune to 3,
                objs.water_rune to 3,
                objs.death_rune to 1,
            ),
            contract(
                objs.spell_earth_blast,
                53,
                31.5,
                15,
                objs.air_rune to 3,
                objs.earth_rune to 4,
                objs.death_rune to 1,
            ),
            contract(
                objs.spell_fire_blast,
                59,
                34.5,
                16,
                objs.air_rune to 4,
                objs.fire_rune to 5,
                objs.death_rune to 1,
            ),
            contract(objs.spell_wind_wave, 62, 36.0, 17, objs.air_rune to 5, objs.blood_rune to 1),
            contract(
                objs.spell_water_wave,
                65,
                37.5,
                18,
                objs.air_rune to 5,
                objs.water_rune to 7,
                objs.blood_rune to 1,
            ),
            contract(
                objs.spell_earth_wave,
                70,
                40.0,
                19,
                objs.air_rune to 5,
                objs.earth_rune to 7,
                objs.blood_rune to 1,
            ),
            contract(
                objs.spell_fire_wave,
                75,
                42.5,
                20,
                objs.air_rune to 5,
                objs.fire_rune to 7,
                objs.blood_rune to 1,
            ),
            contract(objs.spell_wind_surge, 81, 44.5, 21, objs.air_rune to 7, objs.wrath_rune to 1),
            contract(
                objs.spell_water_surge,
                85,
                46.5,
                22,
                objs.air_rune to 7,
                objs.water_rune to 10,
                objs.wrath_rune to 1,
            ),
            contract(
                objs.spell_earth_surge,
                90,
                48.5,
                23,
                objs.air_rune to 7,
                objs.earth_rune to 10,
                objs.wrath_rune to 1,
            ),
            contract(
                objs.spell_fire_surge,
                95,
                50.5,
                24,
                objs.air_rune to 7,
                objs.fire_rune to 10,
                objs.wrath_rune to 1,
            ),
        )

    private fun contract(
        spell: ObjType,
        level: Int,
        castXp: Double,
        cacheMaxHit: Int,
        vararg runes: Pair<ObjType, Int>,
    ) = ElementalSpellContract(spell, level, castXp, cacheMaxHit, mapOf(*runes))

    private data class ElementalSpellContract(
        val spell: ObjType,
        val level: Int,
        val castXp: Double,
        val cacheMaxHit: Int,
        val runes: Map<ObjType, Int>,
    )

    data class Dependencies
    @Inject
    constructor(
        val spells: ElementalSpells,
        val manager: SpellAttackManager,
        val magicSpells: MagicSpellRegistry,
    )
}
