package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ui.IfOverlayButton
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
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class BoltEnchantSpellTest {
    @Test
    fun GameTestState.`cache contracts define all ten bolt-enchantment spells`() =
        runEnchantTest { deps ->
            for (expected in expectations) {
                val spell = requireNotNull(deps.spells.getObjSpell(expected.recipe.spell))
                assertEquals(expected.level, spell.levelReq, expected.recipe.toString())
                assertEquals(expected.xp, spell.castXp, expected.recipe.toString())
                assertEquals(Spellbook.Standard, spell.spellbook, expected.recipe.toString())
                assertEquals(MagicSpellType.Utility, spell.type, expected.recipe.toString())
                assertEquals(expected.recipe.button, spell.component, expected.recipe.toString())
                assertEquals(expected.runes, spell.objReqs.associate { it.obj to it.count })
            }

            assertEquals(5000, objs.view_bolt_enchantment_spells.id)
            assertEquals(218, components.magic_spellbook_xbows_enchant.id ushr 16)
            assertEquals(13, components.magic_spellbook_xbows_enchant.id and 0xFFFF)
            assertEquals(4462, seqs.human_xbow_enchant_arrowtip.id)
            assertEquals(759, spotanims.xbows_enchanting_tips.id)
            assertEquals(2921, synths.enchanted_tipping.id)
        }

    @Test
    fun GameTestState
        .`spellbook opener displays the cache skill-multi contract and casts selection`() =
        runEnchantTest { deps ->
            prepareBase()
            addRunes(deps, objs.spell_enchant_bolt_sapphire, casts = 2)
            player.inv[8] = InvObj(objs.sapphire_bolts, 25)

            deps.events.publish(
                IfOverlayButton(
                    player,
                    components.magic_spellbook_xbows_enchant,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 270 }
            assertEquals(components.chatbox_chatmodal.id, open.destinationCombinedId)
            assertEquals(0, open.type)

            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    19,
                    3,
                    *BoltEnchantRecipe.productIcons.map(ObjType::id).toTypedArray(),
                    1,
                    "How many sets of bolts to enchant?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().filter {
                    it.interfaceId == 270 && it.componentId in 15..24
                }
            assertEquals(10, events.size)
            assertTrue(events.all { it.start == 1 && it.end == 3 && it.events1 == 1 })

            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.skillmulti_b, subcomponent = 2)
            )

            assertEquals(15, player.count(objs.sapphire_bolts))
            assertEquals(10, player.count(objs.sapphire_bolts_e))
            assertEquals(1, player.queueList.count(bolt_enchant_queues.batch))
        }

    @Test
    fun GameTestState.`dragon-only inventory displays the dragon-bolt menu`() =
        runEnchantTest { deps ->
            prepareBase()
            player.inv[8] = InvObj(objs.dragon_onyx_bolts, 20)

            deps.events.publish(
                IfOverlayButton(
                    player,
                    components.magic_spellbook_xbows_enchant,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )

            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    19,
                    2,
                    *BoltEnchantRecipe.dragonProductIcons.map(ObjType::id).toTypedArray(),
                    1,
                    "How many sets of bolts to enchant?",
                ),
                setup.values,
            )
        }

    @Test
    fun GameTestState.`every recipe transforms regular and dragon bolts in sets of ten`() =
        runEnchantTest { deps ->
            for (recipe in BoltEnchantRecipe.entries) {
                for ((source, output) in
                    listOf(
                        recipe.regularSource to recipe.regularOutput,
                        recipe.dragonSource to recipe.dragonOutput,
                    )) {
                    prepareBase()
                    addRunes(deps, recipe.spell)
                    player.inv[8] = InvObj(source, 10)
                    val spell = requireNotNull(deps.spells.getObjSpell(recipe.spell))
                    val startXp = player.statMap.getFineXP(stats.magic)

                    player.withProtectedAccess { deps.enchant.startBatch(this, recipe, sets = 1) }

                    assertEquals(0, player.count(source), "$recipe: $source")
                    assertEquals(10, player.count(output), "$recipe: $output")
                    assertEquals(
                        (spell.castXp * 10).toInt(),
                        player.statMap.getFineXP(stats.magic) - startXp,
                        recipe.toString(),
                    )
                    spell.objReqs.forEach {
                        assertEquals(0, player.count(it.obj), recipe.toString())
                    }
                    assertTrue(player.pendingSpotanims.isNotEmpty(), recipe.toString())
                    assertMessageSent(
                        "The magic of the runes coaxes out the true nature of the gem tips."
                    )
                    advance()
                }
            }
        }

    @Test
    fun GameTestState
        .`a partial set enchants every remaining bolt for one full rune cost and xp`() =
        runEnchantTest { deps ->
            prepareBase()
            val recipe = BoltEnchantRecipe.Opal
            addRunes(deps, recipe.spell)
            player.inv[8] = InvObj(recipe.regularSource, 7)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.enchant.startBatch(this, recipe, sets = 1) }

            assertEquals(0, player.count(recipe.regularSource))
            assertEquals(7, player.count(recipe.regularOutput))
            assertEquals(90, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(0, player.count(objs.cosmic_rune))
            assertEquals(0, player.count(objs.air_rune))
        }

    @Test
    fun GameTestState.`batch casts immediately then every three ticks and crosses bolt families`() =
        runEnchantTest { deps ->
            prepareBase()
            val recipe = BoltEnchantRecipe.Ruby
            addRunes(deps, recipe.spell, casts = 2)
            player.inv[8] = InvObj(recipe.regularSource, 5)
            player.inv[9] = InvObj(recipe.dragonSource, 10)

            player.withProtectedAccess { deps.enchant.startBatch(this, recipe, sets = 2) }
            assertEquals(5, player.count(recipe.regularOutput))
            assertEquals(0, player.count(recipe.dragonOutput))

            advance(2)
            assertEquals(0, player.count(recipe.dragonOutput))
            advance()
            assertEquals(10, player.count(recipe.dragonOutput))
            assertEquals(0, player.queueList.count(bolt_enchant_queues.batch))
        }

    @Test
    fun GameTestState.`selected set count caps the batch`() = runEnchantTest { deps ->
        prepareBase()
        addRunes(deps, BoltEnchantRecipe.Opal.spell, casts = 3)
        player.inv[8] = InvObj(BoltEnchantRecipe.Opal.regularSource, 30)

        player.withProtectedAccess {
            deps.enchant.startBatch(this, BoltEnchantRecipe.Opal, sets = 2)
        }
        assertEquals(10, player.count(BoltEnchantRecipe.Opal.regularOutput))

        advance(3)
        assertEquals(20, player.count(BoltEnchantRecipe.Opal.regularOutput))
        assertEquals(10, player.count(BoltEnchantRecipe.Opal.regularSource))
        assertEquals(0, player.queueList.count(bolt_enchant_queues.batch))

        advance(3)
        assertEquals(20, player.count(BoltEnchantRecipe.Opal.regularOutput))
        assertEquals(10, player.count(BoltEnchantRecipe.Opal.regularSource))
    }

    @Test
    fun GameTestState.`manual selection works one tick after a previous cast`() =
        runEnchantTest { deps ->
            prepareBase()
            addRunes(deps, BoltEnchantRecipe.Opal.spell)
            addRunes(deps, BoltEnchantRecipe.Emerald.spell)
            player.inv[8] = InvObj(BoltEnchantRecipe.Opal.regularSource, 10)
            player.inv[9] = InvObj(BoltEnchantRecipe.Emerald.regularSource, 10)

            player.withProtectedAccess {
                deps.enchant.startBatch(this, BoltEnchantRecipe.Opal, sets = 1)
            }
            advance()
            player.withProtectedAccess {
                deps.enchant.startBatch(this, BoltEnchantRecipe.Emerald, sets = 1)
            }
            assertEquals(10, player.count(BoltEnchantRecipe.Emerald.regularOutput))
            assertEquals(10, player.count(BoltEnchantRecipe.Opal.regularOutput))
            assertEquals(0, player.queueList.count(bolt_enchant_queues.batch))
        }

    @Test
    fun GameTestState.`enchanting is zero-time and movement does not cancel selected sets`() =
        runEnchantTest { deps ->
            prepareBase()
            val recipe = BoltEnchantRecipe.Diamond
            addRunes(deps, recipe.spell, casts = 2)
            player.inv[8] = InvObj(recipe.regularSource, 20)

            player.withProtectedAccess {
                queue(queues.generic_queue1, cycles = 10)
                deps.enchant.startBatch(this, recipe, sets = 2)
            }

            assertEquals(1, player.queueList.count(queues.generic_queue1))
            assertEquals(10, player.count(recipe.regularOutput))
            player.placeAt(player.coords.translateX(1))
            advance(3)

            assertEquals(1, player.queueList.count(queues.generic_queue1))
            assertEquals(20, player.count(recipe.regularOutput))
            assertEquals(0, player.queueList.count(bolt_enchant_queues.batch))
        }

    @Test
    fun GameTestState.`membership level rune and source failures are atomic`() =
        runEnchantTest { deps ->
            val recipe = BoltEnchantRecipe.Onyx

            prepareBase(members = false)
            addRunes(deps, recipe.spell)
            player.inv[8] = InvObj(recipe.regularSource, 10)
            player.withProtectedAccess { deps.enchant.startBatch(this, recipe, sets = 1) }
            assertMessageSent("You need to be on a members' world to cast this spell.")
            assertEquals(10, player.count(recipe.regularSource))

            prepareBase(magicLevel = 86)
            addRunes(deps, recipe.spell)
            player.inv[8] = InvObj(recipe.regularSource, 10)
            player.withProtectedAccess { deps.enchant.startBatch(this, recipe, sets = 1) }
            assertMessageSent("Your Magic level is not high enough for this spell.")
            assertEquals(10, player.count(recipe.regularSource))

            prepareBase()
            player.inv[8] = InvObj(recipe.regularSource, 10)
            player.withProtectedAccess { deps.enchant.startBatch(this, recipe, sets = 1) }
            assertMessageSent("You do not have enough Cosmic Runes to cast this spell.")
            assertEquals(10, player.count(recipe.regularSource))

            prepareBase()
            addRunes(deps, recipe.spell)
            player.withProtectedAccess { deps.enchant.startBatch(this, recipe, sets = 1) }
            assertMessageSent("You do not have any bolts of that type to enchant.")
            assertEquals(1, player.count(objs.cosmic_rune))
        }

    @Test
    fun GameTestState.`staff substitutions work and Fountain of Rune grants no xp`() =
        runEnchantTest { deps ->
            val recipe = BoltEnchantRecipe.Opal
            prepareBase()
            player.righthand = InvObj(objs.air_staff)
            player.inv[0] = InvObj(objs.cosmic_rune)
            player.inv[8] = InvObj(recipe.regularSource, 10)

            player.withProtectedAccess { deps.enchant.startBatch(this, recipe, sets = 1) }
            assertEquals(10, player.count(recipe.regularOutput))
            assertEquals(0, player.count(objs.cosmic_rune))

            advance()
            prepareBase()
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            player.inv[8] = InvObj(recipe.dragonSource, 10)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.enchant.startBatch(this, recipe, sets = 1) }
            assertEquals(10, player.count(recipe.dragonOutput))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.prepareBase(magicLevel: Int = 99, members: Boolean = true) {
        player.stats[stats.magic] = magicLevel
        player.members = members
        player.clearAllInvs()
        player.righthand = null
        player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
    }

    private fun GameTestScope.addRunes(deps: Dependencies, spellType: ObjType, casts: Int = 1) {
        val spell = requireNotNull(deps.spells.getObjSpell(spellType))
        spell.objReqs.forEach { req ->
            val slot =
                player.inv.indexOfFirst { it?.id == req.obj.id }.takeIf { it >= 0 }
                    ?: player.inv.indexOfFirst { it == null }
            val existing = player.inv[slot]
            player.inv[slot] = InvObj(req.obj, (existing?.count ?: 0) + req.count * casts)
        }
    }

    private fun GameTestState.runEnchantTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            BoltEnchantSpellScript::class,
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
        val enchant: BoltEnchantSpell,
        val spells: MagicSpellRegistry,
        val events: EventBus,
    )

    private data class SpellExpectation(
        val recipe: BoltEnchantRecipe,
        val level: Int,
        val xp: Double,
        val runes: Map<ObjType, Int>,
    )

    private companion object {
        val expectations =
            listOf(
                SpellExpectation(
                    BoltEnchantRecipe.Opal,
                    4,
                    9.0,
                    mapOf(objs.cosmic_rune to 1, objs.air_rune to 2),
                ),
                SpellExpectation(
                    BoltEnchantRecipe.Sapphire,
                    7,
                    17.5,
                    mapOf(objs.cosmic_rune to 1, objs.water_rune to 1, objs.mind_rune to 1),
                ),
                SpellExpectation(
                    BoltEnchantRecipe.Jade,
                    14,
                    19.0,
                    mapOf(objs.cosmic_rune to 1, objs.earth_rune to 2),
                ),
                SpellExpectation(
                    BoltEnchantRecipe.Pearl,
                    24,
                    29.0,
                    mapOf(objs.cosmic_rune to 1, objs.water_rune to 2),
                ),
                SpellExpectation(
                    BoltEnchantRecipe.Emerald,
                    27,
                    37.0,
                    mapOf(objs.cosmic_rune to 1, objs.air_rune to 3, objs.nature_rune to 1),
                ),
                SpellExpectation(
                    BoltEnchantRecipe.RedTopaz,
                    29,
                    33.0,
                    mapOf(objs.cosmic_rune to 1, objs.fire_rune to 2),
                ),
                SpellExpectation(
                    BoltEnchantRecipe.Ruby,
                    49,
                    59.0,
                    mapOf(objs.cosmic_rune to 1, objs.fire_rune to 5, objs.blood_rune to 1),
                ),
                SpellExpectation(
                    BoltEnchantRecipe.Diamond,
                    57,
                    67.0,
                    mapOf(objs.cosmic_rune to 1, objs.earth_rune to 10, objs.law_rune to 2),
                ),
                SpellExpectation(
                    BoltEnchantRecipe.Dragonstone,
                    68,
                    78.0,
                    mapOf(objs.cosmic_rune to 1, objs.earth_rune to 15, objs.soul_rune to 1),
                ),
                SpellExpectation(
                    BoltEnchantRecipe.Onyx,
                    87,
                    97.0,
                    mapOf(objs.cosmic_rune to 1, objs.fire_rune to 20, objs.death_rune to 1),
                ),
            )
    }
}
