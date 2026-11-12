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
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class BakePieSpellTest {
    @Test
    fun GameTestState.`cache contracts define Bake Pie and all twelve products`() =
        runBakePieTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_bake_pie))
            assertEquals(9107, spell.obj.id)
            assertEquals(65, spell.levelReq)
            assertEquals(60.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 1, objs.fire_rune to 5, objs.water_rune to 4),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(105, components.magic_spellbook_bake_pie.id and 0xFFFF)
            assertEquals(4413, seqs.lunar_human_magic_summon2.id)
            assertEquals(746, spotanims.quest_lunar_bakepie.id)
            assertEquals(91, bake_pie_queues.batch.id)
            assertEquals(
                listOf(
                    PieContract(2321, 2325, 10, 78.0),
                    PieContract(2319, 2327, 20, 110.0),
                    PieContract(7168, 7170, 29, 128.0),
                    PieContract(2317, 2323, 30, 130.0),
                    PieContract(7176, 7178, 34, 138.0),
                    PieContract(7186, 7188, 47, 164.0),
                    PieContract(19656, 19662, 52, 180.0),
                    PieContract(21684, 21690, 60, 200.0),
                    PieContract(7196, 7198, 70, 210.0),
                    PieContract(22789, 22795, 73, 220.0),
                    PieContract(7206, 7208, 85, 240.0),
                    PieContract(7216, 7218, 95, 260.0),
                ),
                BakePieRecipes.all.map { PieContract(it.raw.id, it.cooked.id, it.level, it.xp) },
            )
        }

    @Test
    fun GameTestState.`batch bakes in inventory order every three ticks without burning`() =
        runBakePieTest { deps ->
            prepareCaster(cookingLevel = 99, casts = 3)
            player.inv[6] = InvObj(objs.uncooked_summer_pie)
            player.inv[4] = InvObj(objs.uncooked_redberry_pie)
            player.inv[5] = InvObj(objs.uncooked_mud_pie)
            val magicXp = player.statMap.getFineXP(stats.magic)
            val cookingXp = player.statMap.getFineXP(stats.cooking)

            player.withProtectedAccess { deps.bakePie.cast(this) }
            advance(2)
            assertEquals(0, player.count(objs.redberry_pie))
            advance()
            assertEquals(1, player.count(objs.redberry_pie))
            assertEquals(0, player.count(objs.mud_pie))

            advance(2)
            assertEquals(0, player.count(objs.mud_pie))
            advance()
            assertEquals(1, player.count(objs.mud_pie))
            advance(3)
            assertEquals(1, player.count(objs.summer_pie))
            assertEquals(0, player.count(objs.burnt_pie))
            assertEquals(1800, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(4660, player.statMap.getFineXP(stats.cooking) - cookingXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(0, player.count(objs.water_rune))
        }

    @Test
    fun GameTestState.`members quest spellbook magic and boosted Cooking gates are atomic`() =
        runBakePieTest { deps ->
            prepareCaster(cookingLevel = 94)
            player.inv[4] = InvObj(objs.uncooked_summer_pie)

            player.members = false
            player.withProtectedAccess {
                deps.bakePie.cast(this)
                assertMessageSent("You need to be on a members' world to cast this spell.")
            }
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            player.withProtectedAccess {
                deps.bakePie.cast(this)
                assertMessageSent("You need to complete Lunar Diplomacy to cast this spell.")
            }
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            player.withProtectedAccess { deps.bakePie.cast(this) }
            assertEquals(0, player.weakQueueList.count(bake_pie_queues.batch))
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 64
            player.withProtectedAccess {
                deps.bakePie.cast(this)
                assertMessageSent("Your Magic level is not high enough for this spell.")
            }
            player.stats[stats.magic] = 99
            player.withProtectedAccess {
                deps.bakePie.cast(this)
                assertMessageSent("You need a Cooking level of 95 to bake this pie.")
            }

            player.statMap.setCurrentLevel(stats.cooking, 95.toByte())
            player.withProtectedAccess { deps.bakePie.cast(this) }
            advance(3)
            assertEquals(1, player.count(objs.summer_pie))
            assertEquals(0, player.count(objs.burnt_pie))
        }

    @Test
    fun GameTestState.`no pie missing runes and depleted boost stop without partial changes`() =
        runBakePieTest { deps ->
            prepareCaster(cookingLevel = 99)
            player.withProtectedAccess {
                deps.bakePie.cast(this)
                assertMessageSent("You do not have any uncooked pies to bake.")
            }
            assertEquals(1, player.count(objs.astral_rune))

            player.inv[4] = InvObj(objs.uncooked_redberry_pie)
            player.inv[0] = null
            player.withProtectedAccess {
                deps.bakePie.cast(this)
                assertMessageSent("You do not have enough Astral Runes to cast this spell.")
            }
            assertEquals(1, player.count(objs.uncooked_redberry_pie))

            prepareCaster(cookingLevel = 94, casts = 2)
            player.statMap.setCurrentLevel(stats.cooking, 95.toByte())
            player.inv[4] = InvObj(objs.uncooked_summer_pie)
            player.inv[5] = InvObj(objs.uncooked_summer_pie)
            player.withProtectedAccess { deps.bakePie.cast(this) }
            advance(3)
            assertEquals(1, player.count(objs.summer_pie))
            player.statMap.setCurrentLevel(stats.cooking, 94.toByte())
            advance(3)
            assertEquals(1, player.count(objs.uncooked_summer_pie))
            assertEquals(1, player.count(objs.astral_rune))
            assertEquals(0, player.weakQueueList.count(bake_pie_queues.batch))
        }

    @Test
    fun GameTestState.`a completed pie applies the Lunar cast presentation`() =
        runBakePieTest { deps ->
            prepareCaster(cookingLevel = 99)
            player.inv[4] = InvObj(objs.uncooked_redberry_pie)

            player.withProtectedAccess { deps.bakePie.process(this) }

            assertEquals(1, player.count(objs.redberry_pie))
            assertEquals(4413, player.pendingSequence.id)
            assertTrue(player.pendingSpotanims.isNotEmpty())
        }

    @Test
    fun GameTestState.`full inventory stale slots rune exhaustion and interruption are safe`() =
        runBakePieTest { deps ->
            prepareCaster(cookingLevel = 99)
            player.fillInv()
            player.inv[0] = InvObj(objs.astral_rune)
            player.inv[1] = InvObj(objs.fire_rune, 5)
            player.inv[2] = InvObj(objs.water_rune, 4)
            player.inv[3] = InvObj(objs.uncooked_redberry_pie)
            player.withProtectedAccess { deps.bakePie.cast(this) }
            advance(3)
            assertEquals(1, player.count(objs.redberry_pie))
            assertEquals(24, player.count(objs.beer))

            prepareCaster(cookingLevel = 99)
            player.inv[4] = InvObj(objs.uncooked_redberry_pie)
            player.withProtectedAccess { deps.bakePie.cast(this) }
            player.inv[4] = InvObj(objs.beer)
            advance(3)
            assertEquals(1, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.redberry_pie))

            prepareCaster(cookingLevel = 99)
            player.inv[4] = InvObj(objs.uncooked_redberry_pie)
            player.inv[5] = InvObj(objs.uncooked_redberry_pie)
            player.withProtectedAccess { deps.bakePie.cast(this) }
            advance(3)
            assertEquals(1, player.count(objs.redberry_pie))
            advance(3)
            assertEquals(1, player.count(objs.uncooked_redberry_pie))
            assertEquals(1, player.count(objs.redberry_pie))

            prepareCaster(cookingLevel = 99, casts = 2)
            player.inv[4] = InvObj(objs.uncooked_redberry_pie)
            player.inv[5] = InvObj(objs.uncooked_redberry_pie)
            player.withProtectedAccess { deps.bakePie.cast(this) }
            player.clearPendingAction()
            advance(3)
            assertEquals(2, player.count(objs.uncooked_redberry_pie))
            assertEquals(0, player.weakQueueList.count(bake_pie_queues.batch))
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.prepareCaster(
        cookingLevel: Int,
        casts: Int = 1,
    ) {
        player.members = true
        player.stats[stats.magic] = 99
        player.stats[stats.cooking] = cookingLevel
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.clearInv()
        player.inv[0] = InvObj(objs.astral_rune, casts)
        player.inv[1] = InvObj(objs.fire_rune, casts * 5)
        player.inv[2] = InvObj(objs.water_rune, casts * 4)
    }

    private fun GameTestState.runBakePieTest(
        body: org.rsmod.api.testing.scope.GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            BakePieSpellScript::class,
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

    private data class PieContract(val raw: Int, val cooked: Int, val level: Int, val xp: Double)

    data class Dependencies
    @Inject
    constructor(val bakePie: BakePieSpell, val spells: MagicSpellRegistry)
}
