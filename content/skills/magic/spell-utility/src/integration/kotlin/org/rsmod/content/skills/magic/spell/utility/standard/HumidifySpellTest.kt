package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.rsmod.game.type.dbrow.DbRowTypeList

@Execution(ExecutionMode.SAME_THREAD)
class HumidifySpellTest {
    @Test
    fun GameTestState.`cache contracts define Humidify and the cache 233 conversion set`() =
        runHumidifyTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_humidify))
            assertEquals(11145, spell.obj.id)
            assertEquals(68, spell.levelReq)
            assertEquals(65.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 1, objs.water_rune to 3, objs.fire_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(110, components.magic_spellbook_humidify.id and 0xFFFF)
            assertEquals(6294, seqs.dream_player_humidify_spell.id)
            assertEquals(1061, spotanims.dream_humidify_spell.id)
            assertEquals(3618, varbits.dream_mentor.id)
            assertEquals(listOf(28), deps.dbRows.types.getValue(33).data[19])
            assertEquals(
                listOf(
                    229 to 227,
                    1831 to 1823,
                    1829 to 1823,
                    1827 to 1823,
                    1825 to 1823,
                    1923 to 1921,
                    1925 to 1929,
                    1935 to 1937,
                    1980 to 4458,
                    5331 to 5340,
                    5333 to 5340,
                    5334 to 5340,
                    5335 to 5340,
                    5336 to 5340,
                    5337 to 5340,
                    5338 to 5340,
                    5339 to 5340,
                    6667 to 6668,
                    434 to 1761,
                    20800 to 20801,
                    5358 to 5364,
                    5359 to 5365,
                    5360 to 5366,
                    5361 to 5367,
                    5362 to 5368,
                    5363 to 5369,
                    5480 to 5488,
                    5481 to 5489,
                    5482 to 5490,
                    5483 to 5491,
                    5484 to 5492,
                    5485 to 5493,
                    5486 to 5494,
                    5487 to 5495,
                    21469 to 21473,
                    21471 to 21475,
                    22848 to 22852,
                    22850 to 22854,
                    22862 to 22864,
                    23655 to 23657,
                ),
                HumidifyRecipes.all.map { it.input.id to it.output.id },
            )
        }

    @Test
    fun GameTestState.`one cast humidifies every eligible category with one xp award`() =
        runHumidifyTest { deps ->
            prepareCaster(deps)
            player.inv[3] = InvObj(objs.vial_empty)
            player.inv[4] = InvObj(objs.water_skin_2)
            player.inv[5] = InvObj(objs.bowl_empty)
            player.inv[6] = InvObj(objs.bucket_empty)
            player.inv[7] = InvObj(objs.jug_empty)
            player.inv[8] = InvObj(objs.empty_cup)
            player.inv[9] = InvObj(objs.watering_can_3)
            player.inv[10] = InvObj(objs.fishbowl_empty)
            player.inv[11] = InvObj(objs.clay)
            player.inv[12] = InvObj(objs.raids_vial_empty)
            player.inv[13] = InvObj(objs.redwood_seedling)
            player.inv[14] = InvObj(objs.beer)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.humidify.cast(this) }

            assertEquals(objs.vial_water.id, player.inv[3]?.id)
            assertEquals(objs.water_skin_4.id, player.inv[4]?.id)
            assertEquals(objs.bowl_water.id, player.inv[5]?.id)
            assertEquals(objs.bucket_water.id, player.inv[6]?.id)
            assertEquals(objs.jug_water.id, player.inv[7]?.id)
            assertEquals(objs.cup_of_water.id, player.inv[8]?.id)
            assertEquals(objs.watering_can_8.id, player.inv[9]?.id)
            assertEquals(objs.fishbowl_water.id, player.inv[10]?.id)
            assertEquals(objs.soft_clay.id, player.inv[11]?.id)
            assertEquals(objs.raids_vial_water.id, player.inv[12]?.id)
            assertEquals(objs.redwood_seedling_watered.id, player.inv[13]?.id)
            assertEquals(objs.beer.id, player.inv[14]?.id)
            assertEquals(650, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.water_rune))
            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(seqs.dream_player_humidify_spell.id, player.pendingSequence.id)
            assertTrue(player.pendingSpotanims.isNotEmpty())
        }

    @Test
    fun GameTestState.`every cache 233 item transform keeps its slot`() = runHumidifyTest { deps ->
        for (chunk in HumidifyRecipes.all.chunked(25)) {
            prepareCaster(deps)
            chunk.forEachIndexed { index, recipe -> player.inv[index + 3] = InvObj(recipe.input) }

            player.withProtectedAccess { deps.humidify.cast(this) }

            chunk.forEachIndexed { index, recipe ->
                assertEquals(recipe.output.id, player.inv[index + 3]?.id, recipe.input.toString())
            }
            advance(3)
        }
    }

    @Test
    fun GameTestState.`members quests spellbook and Magic gates reject atomically`() =
        runHumidifyTest { deps ->
            prepareCaster(deps)
            player.inv[3] = InvObj(objs.vial_empty)

            player.members = false
            player.withProtectedAccess {
                deps.humidify.cast(this)
                assertMessageSent("You need to be on a members' world to cast this spell.")
            }
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            player.withProtectedAccess {
                deps.humidify.cast(this)
                assertMessageSent("You need to complete Lunar Diplomacy to cast this spell.")
            }
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.dream_mentor, 27)
            player.withProtectedAccess {
                deps.humidify.cast(this)
                assertMessageSent("You need to complete Dream Mentor to cast this spell.")
            }
            player.setVarBit(varbits.dream_mentor, 28)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            player.withProtectedAccess { deps.humidify.cast(this) }
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 67
            player.withProtectedAccess {
                deps.humidify.cast(this)
                assertMessageSent("Your Magic level is not high enough for this spell.")
            }

            assertEquals(1, player.count(objs.vial_empty))
            assertEquals(1, player.count(objs.astral_rune))
            assertEquals(3, player.count(objs.water_rune))
            assertEquals(1, player.count(objs.fire_rune))
        }

    @Test
    fun GameTestState.`Gricoller's can refills by itself and a full can spends no additional runes`() =
        runHumidifyTest { deps ->
            prepareCaster(deps, casts = 2)
            val water = cacheTypes.varps.getValue(65632)
            player.inv[6] = InvObj(objTypes.getValue(13353))
            player.withProtectedAccess { vars[water] = 997 }
            val xp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.humidify.cast(this) }
            assertEquals(1000,player.vars[water])
            assertEquals(xp+650,player.statMap.getFineXP(stats.magic))
            advance(4)
            val remaining = player.count(objs.astral_rune)
            player.withProtectedAccess { deps.humidify.cast(this) }
            assertEquals(remaining,player.count(objs.astral_rune))
        }

    @Test
    fun GameTestState.`no eligible item and missing runes make no partial changes`() =
        runHumidifyTest { deps ->
            prepareCaster(deps)
            player.inv[3] = InvObj(objs.beer)
            player.withProtectedAccess {
                deps.humidify.cast(this)
                assertMessageSent("You do not have anything that can be humidified.")
            }
            assertEquals(1, player.count(objs.astral_rune))

            player.inv[3] = InvObj(objs.vial_empty)
            player.inv[0] = null
            player.withProtectedAccess {
                deps.humidify.cast(this)
                assertMessageSent("You do not have enough Astral Runes to cast this spell.")
            }
            assertEquals(1, player.count(objs.vial_empty))
            assertEquals(3, player.count(objs.water_rune))
            assertEquals(1, player.count(objs.fire_rune))
        }

    @Test
    fun GameTestState.`full inventory works and repeated casts use the three tick rate`() =
        runHumidifyTest { deps ->
            prepareCaster(deps, casts = 2)
            player.fillInv()
            player.inv[0] = InvObj(objs.astral_rune, 2)
            player.inv[1] = InvObj(objs.water_rune, 6)
            player.inv[2] = InvObj(objs.fire_rune, 2)
            player.inv[3] = InvObj(objs.vial_empty)

            player.withProtectedAccess { deps.humidify.cast(this) }
            player.inv[4] = InvObj(objs.vial_empty)
            player.withProtectedAccess { deps.humidify.cast(this) }
            advance(2)
            player.withProtectedAccess { deps.humidify.cast(this) }
            assertEquals(1, player.count(objs.vial_empty))

            advance()
            player.withProtectedAccess { deps.humidify.cast(this) }
            assertEquals(0, player.count(objs.vial_empty))
            assertEquals(2, player.count(objs.vial_water))
            assertEquals(23, player.count(objs.beer))
        }

    @Test
    fun GameTestState.`staff substitution and Fountain behavior use shared rune policy`() =
        runHumidifyTest { deps ->
            prepareCaster(deps)
            player.inv[1] = null
            player.inv[3] = InvObj(objs.vial_empty)
            player.righthand = InvObj(objs.water_battlestaff)
            player.withProtectedAccess { deps.humidify.cast(this) }
            assertEquals(1, player.count(objs.vial_water))

            advance(3)
            prepareCaster(deps, casts = 0)
            player.inv[3] = InvObj(objs.vial_empty)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.humidify.cast(this) }
            assertEquals(1, player.count(objs.vial_water))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.prepareCaster(deps: Dependencies, casts: Int = 1) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.dream_mentor, 28)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.clearInv()
        player.righthand = null
        deps.cooldowns.markCast(player, player.currentMapClock)
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts)
            player.inv[1] = InvObj(objs.water_rune, casts * 3)
            player.inv[2] = InvObj(objs.fire_rune, casts)
        }
    }

    private fun GameTestState.runHumidifyTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            HumidifySpellScript::class,
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
            bind(UtilitySpellCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val humidify: HumidifySpell,
        val spells: MagicSpellRegistry,
        val dbRows: DbRowTypeList,
        val cooldowns: UtilitySpellCooldownRepository,
    )
}
