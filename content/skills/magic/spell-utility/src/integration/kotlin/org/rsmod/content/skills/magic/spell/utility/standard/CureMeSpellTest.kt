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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.interf.IfButtonOp

@Execution(ExecutionMode.SAME_THREAD)
class CureMeSpellTest {
    @Test
    fun GameTestState.`cache 238 pins Cure Me spell state and presentation contracts`() =
        runCureMeTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_cure_me))
            assertEquals(9113, spell.obj.id)
            assertEquals(71, spell.levelReq)
            assertEquals(69.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 2, objs.cosmic_rune to 2, objs.law_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_cure_me.id ushr 16)
            assertEquals(118, components.magic_spellbook_cure_me.id and 0xFFFF)
            assertEquals(
                listOf("Cast"),
                deps.components[components.magic_spellbook_cure_me].op.toList(),
            )
            assertEquals(4411, seqs.quest_lunar_pushing_magic_animation.id)
            assertEquals(742, spotanims.quest_lunar_cure_me_spot.id)
            assertEquals(2884, synths.lunar_cure.id)
            assertEquals(102, varps.poison.id)
            assertEquals(3777, varps.poison_2.id)
            assertEquals(6251, varbits.poison_type.id)
            assertEquals(3777, varbits.poison_type.baseVar.id)
        }

    @Test
    fun GameTestState
        .`poison is cured without immunity and undocumented secondary state is preserved`() =
        runCureMeTest { deps ->
            prepareCaster()
            player.setVarp(varps.poison, 25)
            player.setVarp(varps.poison_2, 123)
            player.setVarBit(varbits.poison_type, 2)
            val secondaryState = player.vars[varps.poison_2]
            val poisonType = player.vars[varbits.poison_type]
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.cureMe.cast(this) }

            assertEquals(0, player.vars[varps.poison])
            assertEquals(secondaryState, player.vars[varps.poison_2])
            assertEquals(poisonType, player.vars[varbits.poison_type])
            assertEquals(690, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.cosmic_rune))
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(seqs.quest_lunar_pushing_magic_animation.id, player.pendingSequence.id)
            val spot = EntitySpotanim(player.pendingSpotanims.getLong(0))
            assertEquals(spotanims.quest_lunar_cure_me_spot.id, spot.id)
            assertEquals(96, spot.height)
            assertEquals(2884, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`venom is downgraded to poison with the same next damage`() =
        runCureMeTest { deps ->
            prepareCaster(casts = 4)
            val cases = mapOf(1_000_000 to 30, 1_000_001 to 40, 1_000_007 to 100, 1_000_100 to 100)

            cases.forEach { (venom, expectedPoison) ->
                player.setVarp(varps.poison, venom)
                player.withProtectedAccess { deps.cureMe.cast(this) }
                assertEquals(expectedPoison, player.vars[varps.poison], "venom=$venom")
                assertEquals(
                    LunarPoisonCure.venomDamage(venom),
                    LunarPoisonCure.poisonDamage(expectedPoison),
                    "venom=$venom",
                )
                advance()
            }
        }

    @Test
    fun GameTestState.`a second same tick cast is rejected and the next tick may cure again`() =
        runCureMeTest { deps ->
            prepareCaster(casts = 2)
            player.setVarp(varps.poison, 1_000_000)

            player.withProtectedAccess { deps.cureMe.cast(this) }
            assertEquals(30, player.vars[varps.poison])
            player.withProtectedAccess {
                deps.cureMe.cast(this)
                assertMessageSent("You can't activate this just yet.")
            }
            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(30, player.vars[varps.poison])

            advance()
            player.withProtectedAccess { deps.cureMe.cast(this) }
            assertEquals(0, player.vars[varps.poison])
            assertEquals(0, player.count(objs.astral_rune))
        }

    @Test
    fun GameTestState.`clean and immune players cannot spend runes or gain experience`() =
        runCureMeTest { deps ->
            prepareCaster()
            val magicXp = player.statMap.getFineXP(stats.magic)

            listOf(0, -1, -38, -100).forEach { state ->
                player.setVarp(varps.poison, state)
                player.withProtectedAccess { deps.cureMe.cast(this) }
                assertEquals(state, player.vars[varps.poison])
            }

            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(2, player.count(objs.cosmic_rune))
            assertEquals(1, player.count(objs.law_rune))
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`members quest spellbook Magic and rune failures are atomic`() =
        runCureMeTest { deps ->
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
            player.stats[stats.magic] = 70
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[2] = null
            reject(deps, "You do not have enough Law Runes to cast this spell.")

            assertEquals(25, player.vars[varps.poison])
            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(2, player.count(objs.cosmic_rune))
        }

    @Test
    fun GameTestState.`Fountain and LMS fake runes use shared rune and xp policy`() =
        runCureMeTest { deps ->
            prepareCaster(casts = 0)
            player.setVarp(varps.poison, 25)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val magicXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.cureMe.cast(this) }
            assertEquals(0, player.vars[varps.poison])
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))

            advance()
            player.setVarBit(varbits.fountain_of_rune_active, 0)
            player.setVarBit(varbits.in_lms_game, 1)
            player.inv[0] = InvObj(objs.lms_rune_pouch)
            player.setVarp(varps.poison, 25)
            player.withProtectedAccess { deps.cureMe.cast(this) }
            assertEquals(0, player.vars[varps.poison])
            assertEquals(690, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(1, player.count(objs.lms_rune_pouch))
        }

    @Test
    fun GameTestState.`spellbook button dispatches Cure Me`() = runCureMeTest { deps ->
        prepareCaster()
        player.setVarp(varps.poison, 25)

        deps.events.publish(
            IfOverlayButton(
                player,
                components.magic_spellbook_cure_me,
                comsub = -1,
                obj = null,
                op = IfButtonOp.Op1,
            )
        )

        assertEquals(0, player.vars[varps.poison])
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
            player.inv[2] = InvObj(objs.law_rune, casts)
        }
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        player.withProtectedAccess {
            deps.cureMe.cast(this)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(25, player.vars[varps.poison])
    }

    private fun GameTestState.runCureMeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            CureMeSpellScript::class,
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
        val cureMe: CureMeSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val events: EventBus,
    )
}
