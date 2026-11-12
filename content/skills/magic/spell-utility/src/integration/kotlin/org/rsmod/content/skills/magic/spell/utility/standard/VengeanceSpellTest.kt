package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
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
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.interact.HeldInteractions
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
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class VengeanceSpellTest {
    @Test
    fun GameTestState.`cache 233 pins self Vengeance and animation-picker contracts`() =
        runVengeanceTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_vengeance))
            val spellComponent = deps.components[components.magic_spellbook_vengeance]
            val pickerRow = deps.dbRows.types.getValue(VengeanceSpell.VENGEANCE_TOGGLE_DBROW)

            assertEquals(9136, spell.obj.id)
            assertEquals(94, spell.levelReq)
            assertEquals(112.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 4, objs.earth_rune to 10, objs.death_rune to 2),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_vengeance.interfaceId)
            assertEquals(142, components.magic_spellbook_vengeance.component)
            assertEquals("Cast", spellComponent.op[0])
            assertEquals("Animation", spellComponent.op[8])
            assertEquals(525, interfaces.animation_picker.id)
            assertEquals(8, components.animation_picker_clickable.component)
            assertTrue(pickerRow.data.values.contains(listOf("Vengeance Animations")))
            assertTrue(
                pickerRow.data.values.contains(
                    listOf("Default", 956, "League IV", objs.league_4_vengeance_scroll.id)
                )
            )
            assertEquals(8316, seqs.vengeance_spell_anim_nostalling.id)
            assertEquals(8317, seqs.vengeance_spell_anim_stalling.id)
            assertEquals(10625, seqs.league_4_trailblazer_reloaded_scroll_read.id)
            assertEquals(10626, seqs.trailblazer_vengeance_spotanim_01.id)
            assertEquals(726, spotanims.quest_lunar_spellbook_vengeance_other_spot_anim.id)
            assertEquals(2605, spotanims.trailblazer_vengeance_spotanim_01.id)
            assertEquals(2907, synths.lunar_vengence.id)
            assertEquals(7569, synths.leagues4_vengence_spell_01.id)
            assertEquals(8121, varbits.pvp_area_client.id)
            assertEquals(17316, varbits.vengeance_animation_override.id)
            assertEquals(17327, varbits.league_4_vengeance_animation_unlocked.id)
            assertEquals(4559, varps.toggle_list.id)
            assertEquals(4562, varps.vengeance_anim_toggles.id)

            val scroll = deps.objTypes[objs.league_4_vengeance_scroll]
            assertEquals(28696, scroll.id)
            assertEquals("Trailblazer reloaded vengeance scroll", scroll.name)
            assertTrue(scroll.members)
            assertTrue(scroll.tradeable)
            assertFalse(scroll.stackable)
            assertEquals("Read", scroll.iop[0])
        }

    @Test
    fun GameTestState
        .`Read operation consumes one scroll and unlocks but does not enable League IV`() =
        runVengeanceTest { deps ->
            prepareCaster(casts = 0)
            player.inv[4] = InvObj(objs.league_4_vengeance_scroll)

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, invSlot = 4, HeldOp.Op1)
            }

            assertEquals(0, player.count(objs.league_4_vengeance_scroll))
            assertEquals(1, player.vars[varbits.league_4_vengeance_animation_unlocked])
            assertEquals(
                seqs.league_4_trailblazer_reloaded_scroll_read.id,
                player.pendingSequence.id,
            )
            assertEquals(
                VengeanceAnimationRepository.DEFAULT_BIT,
                deps.animations.normalizedEnabledMask(player),
            )
            assertEquals(3, deps.animations.allowedMask(player))
        }

    @Test
    fun GameTestState.`members and duplicate Read failures preserve the scroll and unlock state`() =
        runVengeanceTest { deps ->
            prepareCaster(casts = 0)
            player.inv[0] = InvObj(objs.league_4_vengeance_scroll)
            player.members = false

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, invSlot = 0, HeldOp.Op1)
            }

            assertEquals(1, player.count(objs.league_4_vengeance_scroll))
            assertEquals(0, player.vars[varbits.league_4_vengeance_animation_unlocked])

            player.members = true
            player.setVarBit(varbits.league_4_vengeance_animation_unlocked, 1)
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, invSlot = 0, HeldOp.Op1)
            }

            assertEquals(1, player.count(objs.league_4_vengeance_scroll))
            assertEquals(1, player.vars[varbits.league_4_vengeance_animation_unlocked])
        }

    @Test
    fun GameTestState
        .`default cast consumes exact runes awards xp and preserves non-PvP animation`() =
        runVengeanceTest { deps ->
            prepareCaster()
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(1120, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(listOf(0, 0, 0), runeCounts())
            assertEquals(seqs.vengeance_spell_anim_nostalling.id, player.pendingSequence.id)
            val spot = EntitySpotanim(player.pendingSpotanims.getLong(0))
            assertEquals(spotanims.quest_lunar_spellbook_vengeance_other_spot_anim.id, spot.id)
            assertEquals(VengeanceSpell.GRAPHIC_HEIGHT, spot.height)
            assertEquals(
                synths.lunar_vengence.id,
                client.filterIsInstance<SynthSound>().single().id,
            )
            assertTrue(deps.states.isActive(player))
            assertFalse(deps.states.isCooldownReady(player))
            assertEquals(1, player.vars[varbits.vengeance_rebound])
            assertEquals(1, player.vars[varbits.vengeance_timelimit])
            assertEquals(0, player.vars[varbits.vengeance_animation_override])
        }

    @Test
    fun GameTestState.`members quest spellbook Magic runes active and house failures are atomic`() =
        runVengeanceTest { deps ->
            prepareCaster()

            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 93
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[2] = null
            reject(deps, "You do not have enough Death Runes to cast this spell.")
            player.inv[2] = InvObj(objs.death_rune, 2)
            deps.states.activate(player)
            reject(deps)
            deps.states.clearActive(player)
            deps.houses.enter(player)
            reject(deps)
        }

    @Test
    fun GameTestState
        .`single Wilderness self-cast is allowed and uses stalling animation with sack`() =
        runVengeanceTest { deps ->
            prepareCaster(casts = 0)
            player.setVarBit(varbits.inside_wilderness, 1)
            player.setVarBit(varbits.multiway_indicator, 0)
            player.setVarBit(varbits.blighted_items_allowed, 1)
            player.inv[0] = InvObj(objs.blighted_vengeance_sack, 2)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(1, player.count(objs.blighted_vengeance_sack))
            assertEquals(seqs.vengeance_spell_anim_stalling.id, player.pendingSequence.id)
            assertTrue(deps.states.isActive(player))
        }

    @Test
    fun GameTestState.`PvP client flag selects stalling animation and Fountain suppresses xp`() =
        runVengeanceTest { deps ->
            prepareCaster()
            player.setVarBit(varbits.pvp_area_client, 1)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(seqs.vengeance_spell_anim_stalling.id, player.pendingSequence.id)
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`LMS fake rune pouch supplies self Vengeance without consumption`() =
        runVengeanceTest { deps ->
            prepareCaster(casts = 0)
            player.setVarBit(varbits.in_lms_game, 1)
            player.inv[0] = InvObj(objs.lms_rune_pouch)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(1, player.count(objs.lms_rune_pouch))
            assertTrue(deps.states.isActive(player))
        }

    @Test
    fun GameTestState.`both Vengeance spells share the exact caster cooldown`() =
        runVengeanceTest { deps ->
            prepareCaster(casts = 2)
            val target = registerRecipient()

            player.withProtectedAccess { deps.spell.cast(this) }
            rejectOther(deps, target, "You can only cast Vengeance spells every 30 seconds.")
            deps.states.clearCooldown(player)
            deps.states.clearActive(player)
            player.withProtectedAccess { deps.other.cast(this, target) }
            reject(deps, "You can only cast Vengeance spells every 30 seconds.")
            advance(50)
            assertTrue(deps.states.isCooldownReady(player))
        }

    @Test
    fun GameTestState.`Animation operation opens cache picker and never casts`() =
        runVengeanceTest { deps ->
            prepareCaster()
            player.setVarBit(varbits.league_4_vengeance_animation_unlocked, 1)
            client.clear()
            val beforeRunes = runeCounts()

            deps.events.publish(
                IfOverlayButton(
                    player,
                    components.magic_spellbook_vengeance,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op10,
                )
            )

            assertEquals(beforeRunes, runeCounts())
            assertFalse(deps.states.isActive(player))
            assertEquals(VengeanceSpell.VENGEANCE_TOGGLE_DBROW, player.vars[varps.toggle_list])
            assertEquals(
                VengeanceAnimationRepository.DEFAULT_BIT,
                player.vars[varps.vengeance_anim_toggles],
            )
            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 525 }
            assertEquals(components.mainmodal.id, open.destinationCombinedId)
            val setup =
                client.filterIsInstance<RunClientScript>().single {
                    it.id == VengeanceSpell.ANIMATION_PICKER_LOAD_SCRIPT
                }
            assertEquals(listOf<Any>(1, 3, VengeanceSpell.VENGEANCE_TOGGLE_DBROW), setup.values)
            val events = client.filterIsInstance<IfSetEventsV2>().single { it.interfaceId == 525 }
            assertEquals(components.animation_picker_clickable.component, events.componentId)
            assertEquals(0, events.start)
            assertEquals(1, events.end)
            assertEquals(0, events.events1)
            assertEquals(1, events.events2)
        }

    @Test
    fun GameTestState
        .`picker rejects locked and last animation but permits unlocked combinations`() =
        runVengeanceTest { deps ->
            prepareCaster(casts = 0)
            deps.animations.normalizedEnabledMask(player)

            assertFalse(deps.animations.toggle(player, 1))
            assertFalse(deps.animations.toggle(player, 0))
            player.setVarBit(varbits.league_4_vengeance_animation_unlocked, 1)
            assertTrue(deps.animations.toggle(player, 1))
            assertEquals(3, player.vars[varps.vengeance_anim_toggles])
            assertTrue(deps.animations.toggle(player, 0))
            assertEquals(2, player.vars[varps.vengeance_anim_toggles])
            assertFalse(deps.animations.toggle(player, 1))
            assertFalse(deps.animations.toggle(player, 2))
        }

    @Test
    fun GameTestState.`League IV selection uses its exact alternate presentation`() =
        runVengeanceTest { deps ->
            prepareCaster()
            player.setVarBit(varbits.league_4_vengeance_animation_unlocked, 1)
            player.setVarp(varps.vengeance_anim_toggles, VengeanceAnimationRepository.LEAGUE_IV_BIT)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(seqs.trailblazer_vengeance_spotanim_01.id, player.pendingSequence.id)
            val spot = EntitySpotanim(player.pendingSpotanims.getLong(0))
            assertEquals(spotanims.trailblazer_vengeance_spotanim_01.id, spot.id)
            assertEquals(VengeanceSpell.GRAPHIC_HEIGHT, spot.height)
            assertEquals(
                synths.leagues4_vengence_spell_01.id,
                client.filterIsInstance<SynthSound>().single().id,
            )
            assertEquals(1, player.vars[varbits.vengeance_animation_override])
        }

    private fun GameTestScope.prepareCaster(casts: Int = 1) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.pvp_area_client, 0)
        player.setVarBit(varbits.multiway_indicator, 0)
        player.setVarBit(varbits.blighted_items_allowed, 0)
        player.setVarBit(varbits.in_lms_game, 0)
        player.setVarBit(varbits.league_4_vengeance_animation_unlocked, 0)
        player.setVarp(varps.vengeance_anim_toggles, 0)
        player.clearInv()
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 4)
            player.inv[1] = InvObj(objs.earth_rune, casts * 10)
            player.inv[2] = InvObj(objs.death_rune, casts * 2)
        }
    }

    private fun GameTestScope.registerRecipient() =
        registerPlayer(coords = player.coords.translateX(1)).also {
            it.members = true
            it.displayName = "Recipient"
            it.setBaseLevel(stats.hitpoints, 99)
            it.setCurrentLevel(stats.hitpoints, 99)
            it.setVarBit(varbits.option_acceptaid, 1)
            it.setVarBit(varbits.inside_wilderness, 0)
            it.setVarBit(varbits.multiway_indicator, 0)
        }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        val beforeRunes = runeCounts()
        val beforeXp = player.statMap.getFineXP(stats.magic)
        val wasActive = deps.states.isActive(player)
        player.withProtectedAccess {
            deps.spell.cast(this)
            if (message != null) assertMessageSent(message)
        }
        assertEquals(beforeRunes, runeCounts())
        assertEquals(beforeXp, player.statMap.getFineXP(stats.magic))
        assertEquals(wasActive, deps.states.isActive(player))
    }

    private fun GameTestScope.rejectOther(
        deps: Dependencies,
        target: org.rsmod.game.entity.Player,
        message: String,
    ) {
        val beforeRunes = runeCounts()
        player.withProtectedAccess {
            deps.other.cast(this, target)
            assertMessageSent(message)
        }
        assertEquals(beforeRunes, runeCounts())
        assertFalse(deps.states.isActive(target))
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
            VengeanceEffectScript::class,
            VengeanceOtherSpellScript::class,
            VengeanceSpellScript::class,
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
        val spell: VengeanceSpell,
        val other: VengeanceOtherSpell,
        val states: VengeanceStateRepository,
        val houses: PlayerOwnedHousePresenceRepository,
        val animations: VengeanceAnimationRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val dbRows: DbRowTypeList,
        val events: EventBus,
        val held: HeldInteractions,
        val objTypes: ObjTypeList,
    )
}
