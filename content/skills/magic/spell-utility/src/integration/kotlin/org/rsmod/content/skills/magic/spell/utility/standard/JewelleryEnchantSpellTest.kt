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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ui.IfOverlayButtonT
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
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.Component

@Execution(ExecutionMode.SAME_THREAD)
class JewelleryEnchantSpellTest {
    @Test
    fun GameTestState.`cache contracts define all seven enchantment spells`() =
        runEnchantTest { deps ->
            val expectations =
                listOf(
                    SpellExpectation(objs.spell_enchant_lvl_1, 7, 17.5, 16, 1, 1),
                    SpellExpectation(objs.spell_enchant_lvl_2, 27, 37.0, 27, 3, 1),
                    SpellExpectation(objs.spell_enchant_lvl_3, 49, 59.0, 40, 5, 1),
                    SpellExpectation(objs.spell_enchant_lvl_4, 57, 67.0, 49, 10, 1),
                    SpellExpectation(objs.spell_enchant_lvl_5, 68, 78.0, 64, 30, 1),
                    SpellExpectation(objs.spell_enchant_lvl_6, 87, 97.0, 77, 40, 1),
                    SpellExpectation(objs.spell_enchant_lvl_7, 93, 110.0, 80, 40, 1),
                )

            for (expected in expectations) {
                val spell = requireNotNull(deps.spells.getObjSpell(expected.spell))
                assertEquals(expected.level, spell.levelReq)
                assertEquals(expected.xp, spell.castXp)
                assertEquals(Spellbook.Standard, spell.spellbook)
                assertEquals(MagicSpellType.Utility, spell.type)
                assertEquals(expected.runeCount, spell.objReqs.sumOf { it.count })
                assertEquals(1, spell.objReqs.single { it.obj.id == objs.cosmic_rune.id }.count)
                assertEquals(expected.component, expected.spellComponent)
            }

            assertEquals(719, seqs.human_enchantamuletlvl1.id)
            assertEquals(720, seqs.human_enchantamuletlvl2.id)
            assertEquals(721, seqs.human_enchantamuletlvl3.id)
            assertEquals(114, spotanims.enchant_amulet_lvl1.id)
            assertEquals(115, spotanims.enchant_amulet_lvl2.id)
            assertEquals(116, spotanims.enchant_amulet_lvl3.id)
            assertEquals(153, spotanims.enchant_amulet_lvl4.id)
            assertEquals(154, spotanims.enchant_amulet_lvl5.id)
            assertEquals(452, spotanims.enchant_amulet_lvl6.id)
        }

    @Test
    fun GameTestState.`registry covers every cache-233 jewellery transformation`() =
        runEnchantTest { deps ->
            assertEquals(44, deps.recipes.recipes.size)
            assertEquals(
                listOf(9, 10, 9, 4, 4, 4, 4),
                spellTypes.map { spell -> deps.recipes.recipes.count { it.spell.id == spell.id } },
            )
            assertEquals(10, deps.recipes.recipes.count { it.ringSound })

            for (recipe in deps.recipes.recipes) {
                prepareBase()
                addRunes(deps, recipe.spell)
                val sourceSlot = player.inv.indexOfFirst { it == null }
                player.inv[sourceSlot] = InvObj(recipe.source)
                val startXp = player.statMap.getFineXP(stats.magic)
                val spell = requireNotNull(deps.spells.getObjSpell(recipe.spell))

                player.withProtectedAccess {
                    deps.enchant.castManual(this, recipe.spell, recipe.source.id)
                }

                assertEquals(0, player.count(recipe.source), recipe.source.toString())
                assertEquals(1, player.count(recipe.output), recipe.output.toString())
                assertEquals(
                    (spell.castXp * 10).toInt(),
                    player.statMap.getFineXP(stats.magic) - startXp,
                    recipe.toString(),
                )
                spell.objReqs.forEach { req ->
                    assertEquals(0, player.count(req.obj), recipe.toString())
                }
                assertTrue(player.pendingSpotanims.isNotEmpty(), recipe.toString())
                advance(3)
            }
        }

    @Test
    fun GameTestState.`casting transforms the first matching source in inventory`() =
        runEnchantTest { deps ->
            prepareBase()
            addRunes(deps, objs.spell_enchant_lvl_1)
            player.inv[6] = InvObj(objs.sapphire_ring)
            player.inv[12] = InvObj(objs.sapphire_ring)

            player.withProtectedAccess {
                deps.enchant.castManual(this, objs.spell_enchant_lvl_1, objs.sapphire_ring.id)
            }

            assertEquals(objs.ring_of_recoil.id, player.inv[6]?.id)
            assertEquals(objs.sapphire_ring.id, player.inv[12]?.id)
        }

    @Test
    fun GameTestState.`one manual cast queues and replaces automatic enchanting`() =
        runEnchantTest { deps ->
            prepareBase()
            addRunes(deps, objs.spell_enchant_lvl_1, casts = 3)
            player.inv[4] = InvObj(objs.sapphire_ring)
            player.inv[5] = InvObj(objs.sapphire_ring)
            player.inv[6] = InvObj(objs.sapphire_ring)

            repeat(3) {
                player.withProtectedAccess {
                    deps.enchant.castManual(this, objs.spell_enchant_lvl_1, objs.sapphire_ring.id)
                }
            }

            assertEquals(1, player.count(objs.ring_of_recoil))
            assertEquals(1, player.queueList.count(jewellery_enchant_queues.manual))
            assertEquals(0, player.queueList.count(jewellery_enchant_queues.auto))

            advance(3)

            assertEquals(2, player.count(objs.ring_of_recoil))
            assertEquals(1, player.count(objs.sapphire_ring))
            assertEquals(0, player.queueList.count(jewellery_enchant_queues.manual))
            assertEquals(1, player.queueList.count(jewellery_enchant_queues.auto))
        }

    @Test
    fun GameTestState.`automatic enchanting repeats the same source every seven ticks`() =
        runEnchantTest { deps ->
            prepareBase()
            addRunes(deps, objs.spell_enchant_lvl_1, casts = 2)
            player.inv[4] = InvObj(objs.sapphire_ring)
            player.inv[5] = InvObj(objs.sapphire_ring)
            player.inv[6] = InvObj(objs.opal_ring)

            player.withProtectedAccess {
                deps.enchant.castManual(this, objs.spell_enchant_lvl_1, objs.sapphire_ring.id)
            }
            advance(6)
            assertEquals(1, player.count(objs.ring_of_recoil))

            advance()

            assertEquals(2, player.count(objs.ring_of_recoil))
            assertEquals(1, player.count(objs.opal_ring))
            assertEquals(0, player.count(objs.ring_of_pursuit))
        }

    @Test
    fun GameTestState.`movement interrupts automatic enchanting`() = runEnchantTest { deps ->
        prepareBase()
        addRunes(deps, objs.spell_enchant_lvl_1, casts = 2)
        player.inv[4] = InvObj(objs.sapphire_ring)
        player.inv[5] = InvObj(objs.sapphire_ring)

        player.withProtectedAccess {
            deps.enchant.castManual(this, objs.spell_enchant_lvl_1, objs.sapphire_ring.id)
        }
        player.placeAt(player.coords.translateX(1))
        advance(7)

        assertEquals(1, player.count(objs.ring_of_recoil))
        assertEquals(1, player.count(objs.sapphire_ring))
        assertEquals(0, player.queueList.count(jewellery_enchant_queues.auto))
    }

    @Test
    fun GameTestState.`combat interrupts automatic enchanting`() = runEnchantTest { deps ->
        prepareBase()
        addRunes(deps, objs.spell_enchant_lvl_1, casts = 2)
        player.inv[4] = InvObj(objs.sapphire_ring)
        player.inv[5] = InvObj(objs.sapphire_ring)

        player.withProtectedAccess {
            deps.enchant.castManual(this, objs.spell_enchant_lvl_1, objs.sapphire_ring.id)
        }
        player.setVarp(varps.lastcombat, player.currentMapClock)
        advance(7)

        assertEquals(1, player.count(objs.ring_of_recoil))
        assertEquals(1, player.count(objs.sapphire_ring))
        assertEquals(0, player.queueList.count(jewellery_enchant_queues.auto))
    }

    @Test
    fun GameTestState.`inventory target event dispatches the selected enchantment spell`() =
        runEnchantTest { deps ->
            prepareBase()
            addRunes(deps, objs.spell_enchant_lvl_4)
            player.inv[5] = InvObj(objs.diamond_amulet)
            deps.events.publish(
                IfOverlayButtonT(
                    player = player,
                    selectedSlot = -1,
                    selectedObj = objTypes[objs.spell_enchant_lvl_4],
                    targetSlot = 5,
                    targetObj = objTypes[objs.diamond_amulet],
                    selectedComponent = Component(components.magic_spellbook_enchant_4.packed),
                    targetComponent = Component(components.inv_items.packed),
                )
            )

            assertEquals(1, player.count(objs.amulet_of_power))
        }

    @Test
    fun GameTestState.`free worlds permit only the four standard gem amulets`() =
        runEnchantTest { deps ->
            val freeAmulets =
                listOf(
                    objs.spell_enchant_lvl_1 to objs.sapphire_amulet,
                    objs.spell_enchant_lvl_2 to objs.emerald_amulet,
                    objs.spell_enchant_lvl_3 to objs.ruby_amulet,
                    objs.spell_enchant_lvl_4 to objs.diamond_amulet,
                )
            for ((spell, source) in freeAmulets) {
                prepareBase(members = false)
                addRunes(deps, spell)
                player.inv[5] = InvObj(source)

                player.withProtectedAccess { deps.enchant.castManual(this, spell, source.id) }

                assertEquals(0, player.count(source))
                advance(3)
            }

            prepareBase(members = false)
            addRunes(deps, objs.spell_enchant_lvl_1)
            player.inv[5] = InvObj(objs.sapphire_ring)
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess {
                deps.enchant.castManual(this, objs.spell_enchant_lvl_1, objs.sapphire_ring.id)
                assertMessageSent("You need to be on a members' world to enchant that item.")
            }
            assertEquals(1, player.count(objs.sapphire_ring))
            assertEquals(1, player.count(objs.cosmic_rune))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`Digsite pendant requires its discovery unlock`() = runEnchantTest { deps ->
        prepareBase()
        addRunes(deps, objs.spell_enchant_lvl_3)
        player.inv[5] = InvObj(objs.ruby_necklace)
        player.setVarBit(varbits.vm_digsite_talisman, 0)

        player.withProtectedAccess {
            deps.enchant.castManual(this, objs.spell_enchant_lvl_3, objs.ruby_necklace.id)
            assertMessageSent("You need to have discovered the Digsite pendant.")
        }
        assertEquals(1, player.count(objs.ruby_necklace))
        assertEquals(1, player.count(objs.cosmic_rune))

        player.setVarBit(varbits.vm_digsite_talisman, 1)
        player.withProtectedAccess {
            deps.enchant.castManual(this, objs.spell_enchant_lvl_3, objs.ruby_necklace.id)
        }
        assertEquals(1, player.count(objs.digsite_pendant_5))
    }

    @Test
    fun GameTestState.`nature amulet uniqueness checks inventory equipment and bank`() =
        runEnchantTest { deps ->
            repeat(3) { container ->
                prepareBase()
                addRunes(deps, objs.spell_enchant_lvl_2)
                player.inv[5] = InvObj(objs.pre_nature_amulet)
                player.withProtectedAccess {
                    when (container) {
                        0 -> player.inv[6] = InvObj(objs.amulet_of_nature)
                        1 -> player.worn[0] = InvObj(objs.amulet_of_nature)
                        else -> bank[0] = InvObj(objs.amulet_of_nature)
                    }
                }

                player.withProtectedAccess {
                    deps.enchant.castManual(
                        this,
                        objs.spell_enchant_lvl_2,
                        objs.pre_nature_amulet.id,
                    )
                    assertMessageSent("You can only have one amulet of nature at a time.")
                }

                assertEquals(1, player.count(objs.pre_nature_amulet))
                assertEquals(1, player.count(objs.cosmic_rune))
            }

            prepareBase()
            addRunes(deps, objs.spell_enchant_lvl_2)
            player.inv[5] = InvObj(objs.pre_nature_amulet)
            player.withProtectedAccess {
                deps.enchant.castManual(this, objs.spell_enchant_lvl_2, objs.pre_nature_amulet.id)
            }
            assertEquals(1, player.count(objs.amulet_of_nature))
        }

    @Test
    fun GameTestState.`level rune and invalid target failures are atomic`() =
        runEnchantTest { deps ->
            prepareBase(magicLevel = 86)
            addRunes(deps, objs.spell_enchant_lvl_6)
            player.inv[5] = InvObj(objs.onyx_ring)
            player.withProtectedAccess {
                deps.enchant.castManual(this, objs.spell_enchant_lvl_6, objs.onyx_ring.id)
                assertMessageSent("Your Magic level is not high enough for this spell.")
            }
            assertEquals(1, player.count(objs.onyx_ring))
            assertEquals(1, player.count(objs.cosmic_rune))

            prepareBase()
            player.inv[5] = InvObj(objs.onyx_ring)
            player.withProtectedAccess {
                deps.enchant.castManual(this, objs.spell_enchant_lvl_6, objs.onyx_ring.id)
                assertMessageSent("You do not have enough Cosmic Runes to cast this spell.")
            }
            assertEquals(1, player.count(objs.onyx_ring))

            prepareBase()
            addRunes(deps, objs.spell_enchant_lvl_1)
            player.inv[5] = InvObj(objs.bronze_axe)
            player.withProtectedAccess {
                deps.enchant.castManual(this, objs.spell_enchant_lvl_1, objs.bronze_axe.id)
                assertMessageSent(
                    "This spell can only be cast on sapphire or opal amulets, necklaces, " +
                        "rings, and bracelets, and on shapes in the Mage Training Arena."
                )
            }
            assertEquals(1, player.count(objs.cosmic_rune))
            assertEquals(1, player.count(objs.bronze_axe))
        }

    @Test
    fun GameTestState.`Fountain of Rune consumes no runes and grants no xp`() =
        runEnchantTest { deps ->
            prepareBase()
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            player.inv[5] = InvObj(objs.zenyte_amulet)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess {
                deps.enchant.castManual(this, objs.spell_enchant_lvl_7, objs.zenyte_amulet.id)
            }

            assertEquals(1, player.count(objs.amulet_of_torture))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.prepareBase(magicLevel: Int = 99, members: Boolean = true) {
        player.stats[stats.magic] = magicLevel
        player.members = members
        player.clearAllInvs()
        player.righthand = null
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.vm_digsite_talisman, 1)
        player.setVarp(varps.lastcombat, -100)
        player.setVarp(varps.lastcombat_pvp, -100)
    }

    private fun GameTestScope.addRunes(deps: Dependencies, spellType: ObjType, casts: Int = 1) {
        val spell = requireNotNull(deps.spells.getObjSpell(spellType))
        spell.objReqs.forEachIndexed { slot, req ->
            player.inv[slot] = InvObj(req.obj, req.count * casts)
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
            JewelleryEnchantSpellScript::class,
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
        val enchant: JewelleryEnchantSpell,
        val spells: MagicSpellRegistry,
        val recipes: JewelleryEnchantRecipeRegistry,
        val events: EventBus,
    )

    private data class SpellExpectation(
        val spell: ObjType,
        val level: Int,
        val xp: Double,
        val component: Int,
        val elementalRuneCount: Int,
        val cosmicRuneCount: Int,
    ) {
        val runeCount: Int = elementalRuneCount + cosmicRuneCount

        val spellComponent: Int
            get() =
                when (spell.id) {
                    objs.spell_enchant_lvl_1.id ->
                        components.magic_spellbook_enchant_1.id and 0xFFFF
                    objs.spell_enchant_lvl_2.id ->
                        components.magic_spellbook_enchant_2.id and 0xFFFF
                    objs.spell_enchant_lvl_3.id ->
                        components.magic_spellbook_enchant_3.id and 0xFFFF
                    objs.spell_enchant_lvl_4.id ->
                        components.magic_spellbook_enchant_4.id and 0xFFFF
                    objs.spell_enchant_lvl_5.id ->
                        components.magic_spellbook_enchant_5.id and 0xFFFF
                    objs.spell_enchant_lvl_6.id ->
                        components.magic_spellbook_enchant_6.id and 0xFFFF
                    objs.spell_enchant_lvl_7.id ->
                        components.magic_spellbook_enchant_7.id and 0xFFFF
                    else -> error("Unexpected enchantment spell: $spell")
                }
    }

    private companion object {
        val spellTypes =
            listOf(
                objs.spell_enchant_lvl_1,
                objs.spell_enchant_lvl_2,
                objs.spell_enchant_lvl_3,
                objs.spell_enchant_lvl_4,
                objs.spell_enchant_lvl_5,
                objs.spell_enchant_lvl_6,
                objs.spell_enchant_lvl_7,
            )
    }
}
