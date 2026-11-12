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
import org.rsmod.api.player.interact.PlayerTInteractions
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
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList

@Execution(ExecutionMode.SAME_THREAD)
class CureOtherSpellTest {
    @Test
    fun GameTestState.`cache 233 pins Cure Other spell state and presentation contracts`() =
        runCureOtherTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_cure_other))

            assertEquals(9110, spell.obj.id)
            assertEquals(68, spell.levelReq)
            assertEquals(65.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 1, objs.earth_rune to 10, objs.law_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_cure_other.id ushr 16)
            assertEquals(109, components.magic_spellbook_cure_other.id and 0xFFFF)
            assertEquals("Cast", deps.components[components.magic_spellbook_cure_other].targetVerb)
            assertEquals(
                emptyList<String>(),
                deps.components[components.magic_spellbook_cure_other].op.toList(),
            )
            assertEquals(4411, seqs.quest_lunar_pushing_magic_animation.id)
            assertEquals(738, spotanims.quest_lunar_spellbook_energy_heal_spot_anim.id)
            assertEquals(2884, synths.lunar_cure.id)
            assertEquals(4180, varbits.option_acceptaid.id)
            assertEquals(102, varps.poison.id)
        }

    @Test
    fun GameTestState.`poisoned recipient is cured without immunity using exact cost xp and fx`() =
        runCureOtherTest { deps ->
            val target = preparePlayers()
            target.setVarp(varps.poison, 25)
            target.setVarp(varps.poison_2, 123)
            target.setVarBit(varbits.poison_type, 2)
            val secondaryState = target.vars[varps.poison_2]
            val poisonType = target.vars[varbits.poison_type]
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.cureOther.cast(this, target) }

            assertEquals(0, target.vars[varps.poison])
            assertEquals(secondaryState, target.vars[varps.poison_2])
            assertEquals(poisonType, target.vars[varbits.poison_type])
            assertEquals(650, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.earth_rune))
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(seqs.quest_lunar_pushing_magic_animation.id, player.pendingSequence.id)
            val spot = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.quest_lunar_spellbook_energy_heal_spot_anim.id, spot.id)
            assertEquals(96, spot.height)
            assertEquals(synths.lunar_cure.id, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`recipient venom is downgraded to poison with the same next damage`() =
        runCureOtherTest { deps ->
            val target = preparePlayers(casts = 4)
            val cases = mapOf(1_000_000 to 30, 1_000_001 to 40, 1_000_007 to 100, 1_000_100 to 100)

            cases.forEach { (venom, expectedPoison) ->
                target.setVarp(varps.poison, venom)
                player.withProtectedAccess { deps.cureOther.cast(this, target) }
                assertEquals(expectedPoison, target.vars[varps.poison], "venom=$venom")
                assertEquals(
                    LunarPoisonCure.venomDamage(venom),
                    LunarPoisonCure.poisonDamage(expectedPoison),
                    "venom=$venom",
                )
                advance(5)
            }
        }

    @Test
    fun GameTestState.`Accept Aid self clean and immune target failures are atomic`() =
        runCureOtherTest { deps ->
            val target = preparePlayers(casts = 2)
            target.setVarp(varps.poison, 25)
            target.setVarBit(varbits.option_acceptaid, 0)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess {
                deps.cureOther.cast(this, target)
                assertMessageSent("Recipient is not accepting aid.")
            }
            player.setVarp(varps.poison, 25)
            player.withProtectedAccess { deps.cureOther.cast(this, player) }
            assertEquals(25, player.vars[varps.poison])

            target.setVarBit(varbits.option_acceptaid, 1)
            listOf(0, -1, -38, -100).forEach { state ->
                target.setVarp(varps.poison, state)
                player.withProtectedAccess { deps.cureOther.cast(this, target) }
                assertEquals(state, target.vars[varps.poison])
            }

            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(20, player.count(objs.earth_rune))
            assertEquals(2, player.count(objs.law_rune))
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`members quest spellbook Magic and rune failures are atomic`() =
        runCureOtherTest { deps ->
            val target = preparePlayers()
            target.setVarp(varps.poison, 25)

            player.members = false
            reject(deps, target, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, target, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps, target)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 67
            reject(deps, target, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[2] = null
            reject(deps, target, "You do not have enough Law Runes to cast this spell.")

            assertEquals(25, target.vars[varps.poison])
            assertEquals(1, player.count(objs.astral_rune))
            assertEquals(10, player.count(objs.earth_rune))
        }

    @Test
    fun GameTestState.`recast lock lasts exactly five ticks`() = runCureOtherTest { deps ->
        val first = preparePlayers(casts = 3)
        val second = registerRecipient("Second")
        first.setVarp(varps.poison, 25)
        second.setVarp(varps.poison, 25)

        player.withProtectedAccess { deps.cureOther.cast(this, first) }
        assertEquals(2, player.count(objs.astral_rune))
        player.withProtectedAccess { deps.cureOther.cast(this, second) }
        assertEquals(2, player.count(objs.astral_rune))
        assertEquals(25, second.vars[varps.poison])
        advance(4)
        player.withProtectedAccess { deps.cureOther.cast(this, second) }
        assertEquals(2, player.count(objs.astral_rune))
        advance()
        player.withProtectedAccess { deps.cureOther.cast(this, second) }
        assertEquals(1, player.count(objs.astral_rune))
        assertEquals(0, second.vars[varps.poison])
    }

    @Test
    fun GameTestState.`earth staff and Fountain use shared rune and xp policy`() =
        runCureOtherTest { deps ->
            val target = preparePlayers(casts = 0)
            player.inv[0] = InvObj(objs.astral_rune)
            player.inv[1] = InvObj(objs.law_rune)
            player.righthand = InvObj(objs.earth_battlestaff)
            target.setVarp(varps.poison, 25)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.cureOther.cast(this, target) }

            assertEquals(0, target.vars[varps.poison])
            assertEquals(650, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(objs.earth_battlestaff.id, player.righthand?.id)

            advance(5)
            player.clearInv()
            player.righthand = null
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            target.setVarp(varps.poison, 25)
            player.withProtectedAccess { deps.cureOther.cast(this, target) }
            assertEquals(0, target.vars[varps.poison])
            assertEquals(650, player.statMap.getFineXP(stats.magic) - magicXp)
        }

    @Test
    fun GameTestState.`player target interaction casts at standard spell range`() =
        runCureOtherTest { deps ->
            val target = preparePlayers(distance = 8)
            target.setVarp(varps.poison, 25)

            deps.interactions.interact(
                player,
                target,
                components.magic_spellbook_cure_other,
                comsub = -1,
                objType = null,
            )
            advance()

            assertEquals(0, target.vars[varps.poison])
            assertEquals(0, player.count(objs.astral_rune))
        }

    private fun GameTestScope.preparePlayers(
        acceptAid: Boolean = true,
        casts: Int = 1,
        distance: Int = 1,
    ): Player {
        player.members = true
        player.displayName = "Caster"
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.clearInv()
        player.righthand = null
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts)
            player.inv[1] = InvObj(objs.earth_rune, casts * 10)
            player.inv[2] = InvObj(objs.law_rune, casts)
        }
        return registerRecipient("Recipient", acceptAid, distance)
    }

    private fun GameTestScope.registerRecipient(
        name: String,
        acceptAid: Boolean = true,
        distance: Int = 1,
    ): Player =
        registerPlayer(coords = player.coords.translateX(distance)).also {
            it.members = true
            it.displayName = name
            it.setVarBit(varbits.option_acceptaid, if (acceptAid) 1 else 0)
            it.setVarp(varps.poison, 0)
        }

    private fun GameTestScope.reject(deps: Dependencies, target: Player, message: String? = null) {
        player.withProtectedAccess {
            deps.cureOther.cast(this, target)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(25, target.vars[varps.poison])
    }

    private fun GameTestState.runCureOtherTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            CureOtherSpellScript::class,
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
        val cureOther: CureOtherSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val interactions: PlayerTInteractions,
    )
}
