package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
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
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.interf.IfButtonOp

@Execution(ExecutionMode.SAME_THREAD)
class StringJewellerySpellTest {
    @Test
    fun GameTestState.`cache 233 pins String Jewellery and exactly fourteen outputs`() =
        runStringJewelleryTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_string_jewelry))
            assertEquals(9122, spell.obj.id)
            assertEquals(80, spell.levelReq)
            assertEquals(83.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 2, objs.water_rune to 5, objs.earth_rune to 10),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(126, components.magic_spellbook_string_jewel.id and 0xFFFF)
            assertEquals(4412, seqs.lunar_human_magic_summon1.id)
            assertEquals(730, spotanims.quest_lunar_spellbook_string_jewelery_spot_anim.id)
            assertEquals(2905, synths.lunar_string_amulet2.id)
            assertEquals(93, string_jewellery_queues.batch.id)
            assertEquals(
                listOf(
                    objs.unstrung_gold_amulet to objs.gold_amulet,
                    objs.unstrung_sapphire_amulet to objs.sapphire_amulet,
                    objs.unstrung_emerald_amulet to objs.emerald_amulet,
                    objs.unstrung_ruby_amulet to objs.ruby_amulet,
                    objs.unstrung_diamond_amulet to objs.diamond_amulet,
                    objs.unstrung_holy_symbol to objs.unblessed_symbol,
                    objs.unstrung_opal_amulet to objs.opal_amulet,
                    objs.unstrung_jade_amulet to objs.jade_amulet,
                    objs.unstrung_topaz_amulet to objs.topaz_amulet,
                    objs.unstrung_dragonstone_amulet to objs.dragonstone_amulet,
                    objs.unstrung_onyx_amulet to objs.onyx_amulet,
                    objs.unstrung_zenyte_amulet to objs.zenyte_amulet,
                    objs.unstrung_unholy_symbol to objs.unpowered_symbol,
                    objs.salve_shard to objs.salve_amulet,
                ),
                deps.recipes.recipes.map { it.unstrung to it.output },
            )
            assertFalse(deps.recipes.recipes.any { it.unstrung == objs.unstrung_m_speak_amulet })
        }

    @Test
    fun GameTestState.`batch strings inventory order every three ticks with per item costs`() =
        runStringJewelleryTest { deps ->
            prepareCaster(casts = 3)
            player.inv[6] = InvObj(objs.unstrung_ruby_amulet)
            player.inv[4] = InvObj(objs.salve_shard)
            player.inv[5] = InvObj(objs.unstrung_holy_symbol)
            val magicXp = player.statMap.getFineXP(stats.magic)
            val craftingXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.stringJewellery.cast(this) }
            advance(2)
            assertEquals(1, player.count(objs.salve_shard))
            advance()
            assertEquals(1, player.count(objs.salve_amulet))
            assertEquals(1, player.count(objs.unstrung_holy_symbol))

            advance(2)
            assertEquals(0, player.count(objs.unblessed_symbol))
            advance()
            assertEquals(1, player.count(objs.unblessed_symbol))
            advance(3)
            assertEquals(1, player.count(objs.ruby_amulet))
            assertEquals(2490, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(120, player.statMap.getFineXP(stats.crafting) - craftingXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.water_rune))
            assertEquals(0, player.count(objs.earth_rune))
            assertEquals(2905, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`members quest spellbook Magic rune and item gates are atomic`() =
        runStringJewelleryTest { deps ->
            prepareCaster()
            player.inv[3] = InvObj(objs.unstrung_gold_amulet)

            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 79
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[0] = null
            reject(deps, "You do not have enough Astral Runes to cast this spell.")

            assertEquals(1, player.count(objs.unstrung_gold_amulet))
            assertEquals(0, player.count(objs.gold_amulet))
            assertEquals(5, player.count(objs.water_rune))
            assertEquals(10, player.count(objs.earth_rune))

            player.clearInv()
            player.inv[0] = InvObj(objs.unstrung_m_speak_amulet)
            player.withProtectedAccess {
                deps.stringJewellery.cast(this)
                assertMessageSent("You don't have any jewellery to string.")
            }
            assertEquals(1, player.count(objs.unstrung_m_speak_amulet))
        }

    @Test
    fun GameTestState.`each result consumes runes and exhaustion leaves later items unchanged`() =
        runStringJewelleryTest { deps ->
            prepareCaster(casts = 1)
            player.inv[3] = InvObj(objs.unstrung_gold_amulet)
            player.inv[4] = InvObj(objs.unstrung_sapphire_amulet)

            player.withProtectedAccess { deps.stringJewellery.cast(this) }
            advance(3)
            assertEquals(1, player.count(objs.gold_amulet))
            advance(3)
            assertEquals(1, player.count(objs.unstrung_sapphire_amulet))
            assertEquals(0, player.count(objs.sapphire_amulet))
            assertEquals(0, player.weakQueueList.count(string_jewellery_queues.batch))
        }

    @Test
    fun GameTestState.`salve awards fixed Crafting xp and applies the cache presentation`() =
        runStringJewelleryTest { deps ->
            prepareCaster()
            player.stats[stats.crafting] = 1
            player.inv[3] = InvObj(objs.salve_shard)
            val craftingXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.stringJewellery.process(this) }

            assertEquals(1, player.count(objs.salve_amulet))
            assertEquals(40, player.statMap.getFineXP(stats.crafting) - craftingXp)
            assertEquals(seqs.lunar_human_magic_summon1.id, player.pendingSequence.id)
            assertEquals(
                spotanims.quest_lunar_spellbook_string_jewelery_spot_anim.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(2905, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`full inventory stale slots and interruption are safe`() =
        runStringJewelleryTest { deps ->
            prepareCaster()
            player.fillInv()
            player.inv[0] = InvObj(objs.astral_rune, 2)
            player.inv[1] = InvObj(objs.water_rune, 5)
            player.inv[2] = InvObj(objs.earth_rune, 10)
            player.inv[3] = InvObj(objs.unstrung_gold_amulet)
            player.withProtectedAccess { deps.stringJewellery.cast(this) }
            advance(3)
            assertEquals(1, player.count(objs.gold_amulet))
            assertEquals(24, player.count(objs.beer))

            prepareCaster()
            player.inv[3] = InvObj(objs.unstrung_gold_amulet)
            player.withProtectedAccess { deps.stringJewellery.cast(this) }
            player.inv[3] = InvObj(objs.beer)
            advance(3)
            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.gold_amulet))

            prepareCaster()
            player.inv[3] = InvObj(objs.unstrung_gold_amulet)
            player.withProtectedAccess { deps.stringJewellery.cast(this) }
            player.clearPendingAction()
            advance(3)
            assertEquals(1, player.count(objs.unstrung_gold_amulet))
            assertEquals(0, player.weakQueueList.count(string_jewellery_queues.batch))
        }

    @Test
    fun GameTestState.`earth staff and Fountain follow shared rune and xp policies`() =
        runStringJewelleryTest { deps ->
            prepareCaster()
            player.inv[2] = null
            player.righthand = InvObj(objs.earth_battlestaff)
            player.inv[3] = InvObj(objs.unstrung_gold_amulet)
            player.withProtectedAccess { deps.stringJewellery.cast(this) }
            advance(3)
            assertEquals(1, player.count(objs.gold_amulet))

            prepareCaster(casts = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            player.inv[0] = InvObj(objs.unstrung_sapphire_amulet)
            val magicXp = player.statMap.getFineXP(stats.magic)
            val craftingXp = player.statMap.getFineXP(stats.crafting)
            player.withProtectedAccess { deps.stringJewellery.cast(this) }
            advance(3)
            assertEquals(1, player.count(objs.sapphire_amulet))
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
            assertEquals(40, player.statMap.getFineXP(stats.crafting) - craftingXp)
        }

    @Test
    fun GameTestState.`overlay spell click dispatches the registered batch`() =
        runStringJewelleryTest { deps ->
            prepareCaster()
            player.inv[3] = InvObj(objs.unstrung_gold_amulet)

            deps.events.publish(
                IfOverlayButton(
                    player,
                    components.magic_spellbook_string_jewel,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )

            assertEquals(1, player.weakQueueList.count(string_jewellery_queues.batch))
            advance(3)
            assertEquals(1, player.count(objs.gold_amulet))
        }

    private fun GameTestScope.prepareCaster(casts: Int = 1) {
        player.members = true
        player.stats[stats.magic] = 99
        player.stats[stats.crafting] = 1
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.clearInv()
        player.righthand = null
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 2)
            player.inv[1] = InvObj(objs.water_rune, casts * 5)
            player.inv[2] = InvObj(objs.earth_rune, casts * 10)
        }
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        player.withProtectedAccess {
            deps.stringJewellery.cast(this)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(0, player.weakQueueList.count(string_jewellery_queues.batch))
    }

    private fun GameTestState.runStringJewelleryTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            StringJewellerySpellScript::class,
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
            bind(StringJewelleryRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val stringJewellery: StringJewellerySpell,
        val recipes: StringJewelleryRecipeRegistry,
        val spells: MagicSpellRegistry,
        val events: EventBus,
    )
}
