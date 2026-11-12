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
import org.rsmod.api.player.interact.PlayerTInteractions
import org.rsmod.api.player.stat.hitpoints
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
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList

@Execution(ExecutionMode.SAME_THREAD)
class HealOtherSpellTest {
    @Test
    fun GameTestState.`cache 233 pins Heal Other spell formula and presentation contracts`() =
        runHealOtherTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_heal_other))

            assertEquals(9134, spell.obj.id)
            assertEquals(92, spell.levelReq)
            assertEquals(101.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 3, objs.blood_rune to 1, objs.law_rune to 3),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_heal_other.id ushr 16)
            assertEquals(140, components.magic_spellbook_heal_other.id and 0xFFFF)
            assertEquals("Cast", deps.components[components.magic_spellbook_heal_other].targetVerb)
            assertEquals(
                emptyList<String>(),
                deps.components[components.magic_spellbook_heal_other].op.toList(),
            )
            assertEquals(4411, seqs.quest_lunar_pushing_magic_animation.id)
            assertEquals(738, spotanims.quest_lunar_spellbook_energy_heal_spot_anim.id)
            assertEquals(2895, synths.lunar_heal_other.id)
            assertEquals(4180, varbits.option_acceptaid.id)

            assertFalse(LunarHealOther.hasRequiredHealth(current = 10, base = 99))
            assertTrue(LunarHealOther.hasRequiredHealth(current = 11, base = 99))
            assertEquals(74, LunarHealOther.transfer(99, 1, 99))
            assertEquals(8, LunarHealOther.transfer(11, 1, 99))
            assertEquals(3, LunarHealOther.transfer(4, 1, 99))
        }

    @Test
    fun GameTestState.`cast floors seventy five percent and transfers exact runes xp hit and fx`() =
        runHealOtherTest { deps ->
            val target = preparePlayers()
            player.setCurrentLevel(stats.hitpoints, 99)
            target.setCurrentLevel(stats.hitpoints, 25)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.healOther.cast(this, target) }

            assertEquals(25, player.hitpoints)
            assertEquals(99, target.hitpoints)
            assertEquals(1010, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.blood_rune))
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(seqs.quest_lunar_pushing_magic_animation.id, player.pendingSequence.id)
            val spot = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.quest_lunar_spellbook_energy_heal_spot_anim.id, spot.id)
            assertEquals(96, spot.height)
            assertEquals(
                synths.lunar_heal_other.id,
                client.filterIsInstance<SynthSound>().single().id,
            )
            val hitmark = Hitmark(player.activeHitmarks.getLong(0))
            assertEquals(74, hitmark.damage)
            assertTrue(hitmark.isNoSource)
            assertEquals(1, player.activeHeadbars.size)
        }

    @Test
    fun GameTestState.`recipient deficit caps both healing and caster self damage`() =
        runHealOtherTest { deps ->
            val target = preparePlayers()
            player.setCurrentLevel(stats.hitpoints, 99)
            target.setCurrentLevel(stats.hitpoints, 98)

            player.withProtectedAccess { deps.healOther.cast(this, target) }

            assertEquals(98, player.hitpoints)
            assertEquals(99, target.hitpoints)
            assertEquals(1, Hitmark(player.activeHitmarks.getLong(0)).damage)
        }

    @Test
    fun GameTestState.`health threshold full dead self and Accept Aid failures are atomic`() =
        runHealOtherTest { deps ->
            val target = preparePlayers(casts = 2)
            val magicXp = player.statMap.getFineXP(stats.magic)
            target.setCurrentLevel(stats.hitpoints, 1)
            player.setCurrentLevel(stats.hitpoints, 10)
            reject(deps, target)

            player.setCurrentLevel(stats.hitpoints, 11)
            player.withProtectedAccess { deps.healOther.cast(this, target) }
            assertEquals(3, player.hitpoints)
            assertEquals(9, target.hitpoints)
            advance(5)

            val runesAfterSuccess = runeCounts()
            val xpAfterSuccess = player.statMap.getFineXP(stats.magic)
            player.setCurrentLevel(stats.hitpoints, 99)
            target.setCurrentLevel(stats.hitpoints, 99)
            reject(deps, target)
            target.setCurrentLevel(stats.hitpoints, 0)
            reject(deps, target)
            player.withProtectedAccess { deps.healOther.cast(this, player) }
            target.setCurrentLevel(stats.hitpoints, 1)
            target.setVarBit(varbits.option_acceptaid, 0)
            player.withProtectedAccess {
                deps.healOther.cast(this, target)
                assertMessageSent("Recipient is not accepting aid.")
            }

            assertEquals(runesAfterSuccess, runeCounts())
            assertEquals(xpAfterSuccess, player.statMap.getFineXP(stats.magic))
            assertEquals(magicXp + 1010, xpAfterSuccess)
        }

    @Test
    fun GameTestState.`members quest spellbook Magic and rune failures are atomic`() =
        runHealOtherTest { deps ->
            val target = preparePlayers()
            target.setCurrentLevel(stats.hitpoints, 1)

            player.members = false
            reject(deps, target, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, target, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps, target)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 91
            reject(deps, target, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[2] = null
            reject(deps, target, "You do not have enough Law Runes to cast this spell.")

            assertEquals(99, player.hitpoints)
            assertEquals(1, target.hitpoints)
            assertEquals(3, player.count(objs.astral_rune))
            assertEquals(1, player.count(objs.blood_rune))
        }

    @Test
    fun GameTestState.`ordinary five tick lock has no ninety second Heal Other cooldown`() =
        runHealOtherTest { deps ->
            val first = preparePlayers(casts = 3)
            val second = registerRecipient("Second")
            first.setCurrentLevel(stats.hitpoints, 1)
            second.setCurrentLevel(stats.hitpoints, 1)

            player.withProtectedAccess { deps.healOther.cast(this, first) }
            assertEquals(6, player.count(objs.astral_rune))
            player.setCurrentLevel(stats.hitpoints, 99)
            player.withProtectedAccess { deps.healOther.cast(this, second) }
            assertEquals(6, player.count(objs.astral_rune))
            assertEquals(1, second.hitpoints)
            advance(4)
            player.withProtectedAccess { deps.healOther.cast(this, second) }
            assertEquals(6, player.count(objs.astral_rune))
            advance()
            player.withProtectedAccess { deps.healOther.cast(this, second) }
            assertEquals(3, player.count(objs.astral_rune))
            assertEquals(75, second.hitpoints)
        }

    @Test
    fun GameTestState.`Fountain supplies runes and suppresses Magic xp only`() =
        runHealOtherTest { deps ->
            val target = preparePlayers(casts = 0)
            target.setCurrentLevel(stats.hitpoints, 1)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.healOther.cast(this, target) }

            assertEquals(25, player.hitpoints)
            assertEquals(75, target.hitpoints)
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(
                synths.lunar_heal_other.id,
                client.filterIsInstance<SynthSound>().single().id,
            )
        }

    @Test
    fun GameTestState.`player target interaction casts at standard spell range`() =
        runHealOtherTest { deps ->
            val target = preparePlayers(distance = 8)
            target.setCurrentLevel(stats.hitpoints, 1)

            deps.interactions.interact(
                player,
                target,
                components.magic_spellbook_heal_other,
                comsub = -1,
                objType = null,
            )
            advance()

            assertEquals(75, target.hitpoints)
            assertEquals(0, player.count(objs.astral_rune))
        }

    private fun GameTestScope.preparePlayers(casts: Int = 1, distance: Int = 1): Player {
        player.members = true
        player.displayName = "Caster"
        player.stats[stats.magic] = 99
        player.setBaseLevel(stats.hitpoints, 99)
        player.setCurrentLevel(stats.hitpoints, 99)
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.clearInv()
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 3)
            player.inv[1] = InvObj(objs.blood_rune, casts)
            player.inv[2] = InvObj(objs.law_rune, casts * 3)
        }
        return registerRecipient("Recipient", distance)
    }

    private fun GameTestScope.registerRecipient(name: String, distance: Int = 1): Player =
        registerPlayer(coords = player.coords.translateX(distance)).also {
            it.members = true
            it.displayName = name
            it.setBaseLevel(stats.hitpoints, 99)
            it.setCurrentLevel(stats.hitpoints, 99)
            it.setVarBit(varbits.option_acceptaid, 1)
        }

    private fun GameTestScope.reject(deps: Dependencies, target: Player, message: String? = null) {
        val beforeRunes = runeCounts()
        val beforeXp = player.statMap.getFineXP(stats.magic)
        val beforeCasterHp = player.hitpoints
        val beforeTargetHp = target.hitpoints
        player.withProtectedAccess {
            deps.healOther.cast(this, target)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(beforeRunes, runeCounts())
        assertEquals(beforeXp, player.statMap.getFineXP(stats.magic))
        assertEquals(beforeCasterHp, player.hitpoints)
        assertEquals(beforeTargetHp, target.hitpoints)
    }

    private fun GameTestScope.runeCounts(): List<Int> =
        listOf(
            player.count(objs.astral_rune),
            player.count(objs.blood_rune),
            player.count(objs.law_rune),
        )

    private fun GameTestState.runHealOtherTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            HealOtherSpellScript::class,
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
        val healOther: HealOtherSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val interactions: PlayerTInteractions,
    )
}
