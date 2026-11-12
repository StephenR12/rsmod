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
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.hit.queueHit
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
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList

@Execution(ExecutionMode.SAME_THREAD)
class VengeanceOtherSpellTest {
    @Test
    fun GameTestState.`cache 233 pins Vengeance Other spell and presentation contracts`() =
        runVengeanceTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_vengeance_other))

            assertEquals(9135, spell.obj.id)
            assertEquals(93, spell.levelReq)
            assertEquals(108.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 3, objs.earth_rune to 10, objs.death_rune to 2),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_vengeance_other.id ushr 16)
            assertEquals(141, components.magic_spellbook_vengeance_other.id and 0xFFFF)
            assertEquals(
                "Cast",
                deps.components[components.magic_spellbook_vengeance_other].targetVerb,
            )
            assertEquals(4411, seqs.quest_lunar_pushing_magic_animation.id)
            assertEquals(725, spotanims.quest_lunar_spellbook_vengeance_spot_anim.id)
            assertEquals(726, spotanims.quest_lunar_spellbook_vengeance_other_spot_anim.id)
            assertEquals(8316, seqs.vengeance_spell_anim_nostalling.id)
            assertEquals(8317, seqs.vengeance_spell_anim_stalling.id)
            assertEquals(2907, synths.lunar_vengence.id)
            assertEquals(2908, synths.lunar_vengence_other.id)
            assertEquals(2450, varbits.vengeance_rebound.id)
            assertEquals(2451, varbits.vengeance_timelimit.id)
            assertEquals(50, VengeanceStateRepository.COOLDOWN_CYCLES)
            assertEquals(60, VengeanceReflection.reflectedDamage(81))
            assertEquals(1, VengeanceReflection.reflectedDamage(1))
        }

    @Test
    fun GameTestState.`cast consumes exact runes awards xp and arms recipient with exact fx`() =
        runVengeanceTest { deps ->
            val target = preparePlayers()
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.spell.cast(this, target) }

            assertEquals(1080, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(listOf(0, 0, 0), runeCounts())
            assertEquals(seqs.quest_lunar_pushing_magic_animation.id, player.pendingSequence.id)
            val spot = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.quest_lunar_spellbook_vengeance_spot_anim.id, spot.id)
            assertEquals(96, spot.height)
            assertEquals(
                synths.lunar_vengence_other.id,
                client.filterIsInstance<SynthSound>().single().id,
            )
            assertMessageSent("You have the power of vengeance!", target)
            assertTrue(deps.states.isActive(target))
            assertFalse(deps.states.isCooldownReady(player))
            assertTrue(deps.states.isCooldownReady(target))
            assertEquals(1, target.vars[varbits.vengeance_rebound])
            assertEquals(1, player.vars[varbits.vengeance_timelimit])
        }

    @Test
    fun GameTestState.`self dead Accept Aid active and house failures are atomic`() =
        runVengeanceTest { deps ->
            val target = preparePlayers()

            reject(deps, player)
            target.setCurrentLevel(stats.hitpoints, 0)
            reject(deps, target)
            target.setCurrentLevel(stats.hitpoints, 99)
            target.setVarBit(varbits.option_acceptaid, 0)
            reject(deps, target, "Recipient is not accepting aid.")
            target.setVarBit(varbits.option_acceptaid, 1)
            deps.states.activate(target)
            reject(deps, target)
            deps.states.clearActive(target)
            deps.houses.enter(player)
            reject(deps, target)
            deps.houses.leave(player)
            deps.houses.enter(target)
            reject(deps, target)
        }

    @Test
    fun GameTestState.`members quest spellbook Magic and rune failures are atomic`() =
        runVengeanceTest { deps ->
            val target = preparePlayers()

            player.members = false
            reject(deps, target, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, target, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps, target)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 92
            reject(deps, target, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[2] = null
            reject(deps, target, "You do not have enough Death Runes to cast this spell.")
        }

    @Test
    fun GameTestState.`single Wilderness rejects while multiway blighted sack succeeds`() =
        runVengeanceTest { deps ->
            val target = preparePlayers(casts = 0)
            player.setVarBit(varbits.inside_wilderness, 1)
            player.setVarBit(varbits.multiway_indicator, 0)
            player.inv[0] = InvObj(objs.blighted_vengeance_sack, 2)
            player.setVarBit(varbits.blighted_items_allowed, 1)
            reject(deps, target)

            player.setVarBit(varbits.multiway_indicator, 1)
            target.setVarBit(varbits.inside_wilderness, 1)
            target.setVarBit(varbits.multiway_indicator, 0)
            reject(deps, target)

            target.setVarBit(varbits.multiway_indicator, 1)
            player.withProtectedAccess { deps.spell.cast(this, target) }
            assertEquals(1, player.count(objs.blighted_vengeance_sack))
            assertTrue(deps.states.isActive(target))
        }

    @Test
    fun GameTestState.`shared caster cooldown clears on exact fiftieth tick`() =
        runVengeanceTest { deps ->
            val first = preparePlayers(casts = 2)
            val second = registerRecipient("Second")

            player.withProtectedAccess { deps.spell.cast(this, first) }
            reject(deps, second, "You can only cast Vengeance spells every 30 seconds.")
            advance(49)
            reject(deps, second, "You can only cast Vengeance spells every 30 seconds.")
            advance()
            assertTrue(deps.states.isCooldownReady(player))
            assertEquals(0, player.vars[varbits.vengeance_timelimit])

            player.withProtectedAccess { deps.spell.cast(this, second) }
            assertEquals(listOf(0, 0, 0), runeCounts())
            assertTrue(deps.states.isActive(second))
        }

    @Test
    fun GameTestState.`player hit reflects actual damage once with floor and minimum one`() =
        runVengeanceTest { deps ->
            val target = preparePlayers(casts = 0)
            val attacker = registerRecipient("Attacker")
            deps.states.activate(target)

            target.queueHit(attacker, delay = 1, HitType.Melee, damage = 81)
            advance()

            assertEquals(18, target.hitpoints)
            assertEquals(39, attacker.hitpoints)
            assertFalse(deps.states.isActive(target))

            deps.states.activate(target)
            target.queueHit(attacker, delay = 1, HitType.Ranged, damage = 1)
            advance()
            assertEquals(17, target.hitpoints)
            assertEquals(38, attacker.hitpoints)
            assertFalse(deps.states.isActive(target))

            target.setCurrentLevel(stats.hitpoints, 10)
            attacker.setCurrentLevel(stats.hitpoints, 99)
            deps.states.activate(target)
            target.queueHit(attacker, delay = 1, HitType.Magic, damage = 81)
            advance()
            assertEquals(0, target.hitpoints)
            assertEquals(92, attacker.hitpoints)
            assertFalse(deps.states.isActive(target))

            val phraseTarget = registerRecipient("Phrase target")
            deps.states.activate(phraseTarget)
            val phraseHit = phraseTarget.queueHit(attacker, delay = 1, HitType.Melee, damage = 2)
            deps.reflection.impact(PlayerHitEvents.Impact(phraseTarget, phraseHit, damage = 2))
            assertEquals("Taste vengeance!", phraseTarget.pendingSay)
        }

    @Test
    fun GameTestState.`zero and environmental damage retain effect while npc damage rebounds`() =
        runVengeanceTest { deps ->
            val target = preparePlayers(casts = 0)
            deps.states.activate(target)

            target.queueHit(delay = 1, type = HitType.Typeless, damage = 5)
            advance()
            assertEquals(94, target.hitpoints)
            assertTrue(deps.states.isActive(target))

            val source =
                spawnNpc(
                    target.coords.translateX(1),
                    npcTypeFactory.create {
                        name = "Goblin"
                        hitpoints = 99
                        defaultMode = NpcMode.None
                    },
                )
            target.queueHit(source, delay = 1, HitType.Melee, damage = 5)
            advance(2)
            assertEquals(89, target.hitpoints)
            assertEquals(96, source.hitpoints)
            assertFalse(deps.states.isActive(target))

            deps.states.activate(target)
            target.queueHit(source, delay = 1, HitType.Melee, damage = 0)
            advance()
            assertTrue(deps.states.isActive(target))
        }

    @Test
    fun GameTestState.`recoil immune npc leaves Vengeance armed`() = runVengeanceTest { deps ->
        val target = preparePlayers(casts = 0)
        val source =
            spawnNpc(
                target.coords.translateX(1),
                npcTypeFactory.create {
                    name = "Vorkath"
                    hitpoints = 750
                    defaultMode = NpcMode.None
                },
            )
        deps.states.activate(target)

        target.queueHit(source, delay = 1, HitType.Magic, damage = 20)
        advance()

        assertEquals(79, target.hitpoints)
        assertEquals(750, source.hitpoints)
        assertTrue(deps.states.isActive(target))
    }

    @Test
    fun GameTestState.`logout clears active effect while caster cooldown survives login sync`() =
        runVengeanceTest { deps ->
            val target = preparePlayers()
            player.withProtectedAccess { deps.spell.cast(this, target) }

            deps.events.publish(SessionStateEvent.Logout(target))
            assertFalse(deps.states.isActive(target))
            deps.events.publish(SessionStateEvent.Logout(player))
            deps.events.publish(SessionStateEvent.Login(player))
            assertFalse(deps.states.isCooldownReady(player))
            assertEquals(1, player.vars[varbits.vengeance_timelimit])
        }

    @Test
    fun GameTestState.`player target interaction casts at standard spell range`() =
        runVengeanceTest { deps ->
            val target = preparePlayers(distance = 8)

            deps.interactions.interact(
                player,
                target,
                components.magic_spellbook_vengeance_other,
                comsub = -1,
                objType = null,
            )
            advance()

            assertTrue(deps.states.isActive(target))
            assertEquals(listOf(0, 0, 0), runeCounts())
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
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.multiway_indicator, 0)
        player.setVarBit(varbits.blighted_items_allowed, 0)
        player.clearInv()
        client.clear()
        if (casts > 0) {
            prepareRunes(casts)
        }
        return registerRecipient("Recipient", distance)
    }

    private fun GameTestScope.prepareRunes(casts: Int = 1) {
        player.inv[0] = InvObj(objs.astral_rune, casts * 3)
        player.inv[1] = InvObj(objs.earth_rune, casts * 10)
        player.inv[2] = InvObj(objs.death_rune, casts * 2)
    }

    private fun GameTestScope.registerRecipient(name: String, distance: Int = 1): Player =
        registerPlayer(coords = player.coords.translateX(distance)).also {
            it.members = true
            it.displayName = name
            it.setBaseLevel(stats.hitpoints, 99)
            it.setCurrentLevel(stats.hitpoints, 99)
            it.setVarBit(varbits.option_acceptaid, 1)
            it.setVarBit(varbits.inside_wilderness, 0)
            it.setVarBit(varbits.multiway_indicator, 0)
        }

    private fun GameTestScope.reject(deps: Dependencies, target: Player, message: String? = null) {
        val beforeRunes = runeCounts()
        val beforeXp = player.statMap.getFineXP(stats.magic)
        val targetActive = deps.states.isActive(target)
        player.withProtectedAccess {
            deps.spell.cast(this, target)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(beforeRunes, runeCounts())
        assertEquals(beforeXp, player.statMap.getFineXP(stats.magic))
        assertEquals(targetActive, deps.states.isActive(target))
    }

    private fun GameTestScope.runeCounts(): List<Int> =
        listOf(
            player.count(objs.astral_rune),
            player.count(objs.earth_rune),
            player.count(objs.death_rune),
        )

    private fun GameTestState.runVengeanceTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            PlayerHitScript::class,
            NpcHitScript::class,
            VengeanceEffectScript::class,
            VengeanceOtherSpellScript::class,
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
            bind(VengeanceStateRepository::class.java).`in`(Scopes.SINGLETON)
            bind(PlayerOwnedHousePresenceRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val spell: VengeanceOtherSpell,
        val states: VengeanceStateRepository,
        val houses: PlayerOwnedHousePresenceRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val interactions: PlayerTInteractions,
        val reflection: VengeanceReflection,
        val events: EventBus,
    )
}
