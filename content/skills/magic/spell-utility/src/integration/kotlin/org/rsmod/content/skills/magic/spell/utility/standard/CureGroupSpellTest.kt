package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
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
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
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
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.interf.IfButtonOp

@Execution(ExecutionMode.SAME_THREAD)
class CureGroupSpellTest {
    @Test
    fun GameTestState.`cache 238 pins Cure Group state and presentation contracts`() =
        runCureGroupTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_cure_group))

            assertEquals(9116, spell.obj.id)
            assertEquals(74, spell.levelReq)
            assertEquals(74.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 2, objs.cosmic_rune to 2, objs.law_rune to 2),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_cure_group.id ushr 16)
            assertEquals(122, components.magic_spellbook_cure_group.id and 0xFFFF)
            assertEquals(
                listOf("Cast"),
                deps.components[components.magic_spellbook_cure_group].op.toList(),
            )
            assertEquals(4409, seqs.quest_lunar_spell_cast_spell_on_group.id)
            assertEquals(744, spotanims.quest_lunar_spellbook_cure_group_spot_anim.id)
            assertEquals(2884, synths.lunar_cure.id)
            assertEquals(4180, varbits.option_acceptaid.id)
        }

    @Test
    fun GameTestState.`cast cures caster and all aiding players in exact three by three area`() =
        runCureGroupTest { deps ->
            prepareCaster()
            player.setVarp(varps.poison, 25)
            player.setVarBit(varbits.option_acceptaid, 0)
            val recipients =
                listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
                    .mapIndexed { index, (x, z) ->
                        registerRecipient("Recipient$index", x, z).also {
                            it.setVarp(varps.poison, 25)
                        }
                    }
            val outside = registerRecipient("Outside", 2, 0).also { it.setVarp(varps.poison, 25) }
            val upstairs =
                registerPlayer(coords = player.coords.translateLevel(1)).also {
                    it.setVarBit(varbits.option_acceptaid, 1)
                    it.setVarp(varps.poison, 25)
                }
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.cureGroup.cast(this) }

            assertEquals(0, player.vars[varps.poison])
            recipients.forEach {
                assertEquals(0, it.vars[varps.poison])
                assertGroupGraphic(it)
            }
            assertEquals(25, outside.vars[varps.poison])
            assertEquals(25, upstairs.vars[varps.poison])
            assertTrue(outside.pendingSpotanims.isEmpty())
            assertTrue(upstairs.pendingSpotanims.isEmpty())
            assertGroupGraphic(player)
            assertEquals(740, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.cosmic_rune))
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(seqs.quest_lunar_spell_cast_spell_on_group.id, player.pendingSequence.id)
            assertEquals(synths.lunar_cure.id, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`Accept Aid filters recipients while caster and clean players receive fx`() =
        runCureGroupTest { deps ->
            prepareCaster()
            player.setVarp(varps.poison, -38)
            player.setVarBit(varbits.option_acceptaid, 0)
            val enabled = registerRecipient("Enabled", 1, 0).also { it.setVarp(varps.poison, 0) }
            val disabled =
                registerRecipient("Disabled", 0, 1, acceptAid = false).also {
                    it.setVarp(varps.poison, 25)
                }

            player.withProtectedAccess { deps.cureGroup.cast(this) }

            assertEquals(-38, player.vars[varps.poison])
            assertEquals(0, enabled.vars[varps.poison])
            assertEquals(25, disabled.vars[varps.poison])
            assertGroupGraphic(player)
            assertGroupGraphic(enabled)
            assertTrue(disabled.pendingSpotanims.isEmpty())
        }

    @Test
    fun GameTestState.`venom recipients retain the same next damage after group cure`() =
        runCureGroupTest { deps ->
            prepareCaster()
            val cases = mapOf(1_000_000 to 30, 1_000_001 to 40, 1_000_007 to 100, 1_000_100 to 100)
            val recipients =
                cases.entries.mapIndexed { index, (venom, expected) ->
                    val recipient = registerRecipient("Venom$index", index % 3 - 1, index / 3)
                    recipient.setVarp(varps.poison, venom)
                    Triple(venom, recipient, expected)
                }

            player.withProtectedAccess { deps.cureGroup.cast(this) }

            recipients.forEach { (venom, recipient, expected) ->
                assertEquals(expected, recipient.vars[varps.poison])
                assertEquals(
                    LunarPoisonCure.venomDamage(venom),
                    LunarPoisonCure.poisonDamage(expected),
                )
            }
        }

    @Test
    fun GameTestState.`members quest spellbook Magic and rune failures are atomic`() =
        runCureGroupTest { deps ->
            prepareCaster()
            player.setVarp(varps.poison, 25)

            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 73
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[2] = null
            reject(deps, "You do not have enough Law Runes to cast this spell.")

            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(2, player.count(objs.cosmic_rune))
            assertTrue(player.pendingSpotanims.isEmpty())
        }

    @Test
    fun GameTestState.`clean cast succeeds and recast lock lasts exactly five ticks`() =
        runCureGroupTest { deps ->
            prepareCaster(casts = 3)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.cureGroup.cast(this) }
            assertEquals(4, player.count(objs.astral_rune))
            assertEquals(740, player.statMap.getFineXP(stats.magic) - magicXp)
            repeat(2) { player.withProtectedAccess { deps.cureGroup.cast(this) } }
            assertEquals(4, player.count(objs.astral_rune))
            advance(4)
            player.withProtectedAccess { deps.cureGroup.cast(this) }
            assertEquals(4, player.count(objs.astral_rune))
            advance()
            player.withProtectedAccess { deps.cureGroup.cast(this) }
            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(1480, player.statMap.getFineXP(stats.magic) - magicXp)
        }

    @Test
    fun GameTestState.`Fountain uses shared zero-rune and zero-xp policy`() =
        runCureGroupTest { deps ->
            prepareCaster(casts = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.cureGroup.cast(this) }
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
            assertGroupGraphic(player)
        }

    @Test
    fun GameTestState.`spellbook button dispatches Cure Group`() = runCureGroupTest { deps ->
        prepareCaster()
        player.setVarp(varps.poison, 25)

        deps.events.publish(
            IfOverlayButton(
                player,
                components.magic_spellbook_cure_group,
                comsub = -1,
                obj = null,
                op = IfButtonOp.Op1,
            )
        )

        assertEquals(0, player.vars[varps.poison])
        assertEquals(0, player.count(objs.astral_rune))
    }

    private fun GameTestScope.prepareCaster(casts: Int = 1) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.in_lms_game, 0)
        player.setVarp(varps.poison, 0)
        player.clearInv()
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 2)
            player.inv[1] = InvObj(objs.cosmic_rune, casts * 2)
            player.inv[2] = InvObj(objs.law_rune, casts * 2)
        }
    }

    private fun GameTestScope.registerRecipient(
        name: String,
        x: Int,
        z: Int,
        acceptAid: Boolean = true,
    ): Player =
        registerPlayer(coords = player.coords.translate(x, z)).also {
            it.displayName = name
            it.setVarBit(varbits.option_acceptaid, if (acceptAid) 1 else 0)
            it.setVarp(varps.poison, 0)
        }

    private fun assertGroupGraphic(recipient: Player) {
        val graphic = EntitySpotanim(recipient.pendingSpotanims.getLong(0))
        assertEquals(spotanims.quest_lunar_spellbook_cure_group_spot_anim.id, graphic.id)
        assertEquals(96, graphic.height)
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        player.withProtectedAccess {
            deps.cureGroup.cast(this)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(25, player.vars[varps.poison])
    }

    private fun GameTestState.runCureGroupTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            CureGroupSpellScript::class,
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
        val cureGroup: CureGroupSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val events: EventBus,
    )
}
