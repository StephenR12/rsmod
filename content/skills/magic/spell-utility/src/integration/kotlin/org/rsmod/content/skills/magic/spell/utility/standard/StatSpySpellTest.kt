package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
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
import org.rsmod.api.player.interact.PlayerTInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
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
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.game.type.stat.StatType

@Execution(ExecutionMode.SAME_THREAD)
class StatSpySpellTest {
    @Test
    fun GameTestState.`cache contracts define Stat Spy and all twenty three native stat rows`() =
        runStatSpyTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_stat_spy))

            assertEquals(11147, spell.obj.id)
            assertEquals(75, spell.levelReq)
            assertEquals(76.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(1, spell.maxHit)
            assertEquals(
                mapOf(objs.astral_rune to 2, objs.cosmic_rune to 2, objs.body_rune to 5),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(118, components.magic_spellbook_stat_spy.component)
            assertEquals(523, interfaces.dream_player_stats.id)
            assertEquals(6293, seqs.dream_player_monsterexam_statspy.id)
            assertEquals(7, deps.seqs[seqs.dream_player_monsterexam_statspy].tickDuration)
            assertEquals(1060, spotanims.dream_spell_playerexam_statspy_spotanim.id)
            assertEquals(738, spotanims.quest_lunar_spellbook_energy_heal_spot_anim.id)
            assertEquals(3620, synths.lunar_stat_spy.id)
            assertEquals(listOf(190), deps.dbRows.types.getValue(88).data[19])
            assertEquals(listOf(28), deps.dbRows.types.getValue(33).data[19])
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))

            assertEquals(23, statFields.size)
            assertEquals(
                (1..89 step 4).flatMap { listOf(it, it + 1) },
                statFields.flatMap { listOf(it.current.component, it.base.component) },
            )
            assertTrue(
                statFields
                    .flatMap { listOf(it.current, it.base) }
                    .all { deps.components[it].type == 4 }
            )
            assertEquals(94, components.dream_player_stats_name.component)
            assertEquals("Player ~ Name", deps.components[components.dream_player_stats_name].text)
            assertEquals(95, components.dream_player_stats_close.component)
        }

    @Test
    fun GameTestState.`cast consumes exact runes awards xp and populates the native side panel`() =
        runStatSpyTest { deps ->
            val target = preparePlayers(deps)
            statFields.forEachIndexed { index, field -> target.stats[field.stat] = 10 + index }
            target.statMap.setCurrentLevel(stats.attack, 19.toByte())
            target.statMap.setCurrentLevel(stats.hitpoints, 4.toByte())
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.statSpy.cast(this, target) }

            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.cosmic_rune))
            assertEquals(0, player.count(objs.body_rune))
            assertEquals(760, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.dream_player_monsterexam_statspy.id, player.pendingSequence.id)
            assertEquals(
                spotanims.dream_spell_playerexam_statspy_spotanim.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(
                spotanims.quest_lunar_spellbook_energy_heal_spot_anim.id,
                EntitySpotanim(target.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(
                synths.lunar_stat_spy.id,
                client.filterIsInstance<SynthSound>().single().id,
            )
            assertModalOpen(interfaces.dream_player_stats)
            assertMessageSent("Caster is spying on your stats!", target)

            val textByComponent =
                client.filterIsInstance<IfSetText>().associate { it.componentId to it.text }
            for (field in statFields) {
                assertEquals(
                    target.stat(field.stat).toString(),
                    textByComponent[field.current.component],
                )
                assertEquals(
                    target.statBase(field.stat).toString(),
                    textByComponent[field.base.component],
                )
            }
            assertEquals(
                "Player ~ Recipient",
                textByComponent[components.dream_player_stats_name.component],
            )
        }

    @Test
    fun GameTestState.`members quests spellbook level and rune failures are atomic`() =
        runStatSpyTest { deps ->
            val target = preparePlayers(deps)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.members = false
            assertRejected(deps, target, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            assertRejected(deps, target, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.dream_mentor, 27)
            assertRejected(deps, target, "You need to complete Dream Mentor to cast this spell.")
            player.setVarBit(varbits.dream_mentor, 28)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            assertRejected(deps, target)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 74
            assertRejected(deps, target, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[0] = InvObj(objs.astral_rune, 1)
            assertRejected(deps, target, "You do not have enough Astral Runes to cast this spell.")

            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
            assertModalNotOpen(interfaces.dream_player_stats)
            assertNoMessageSent(target)
        }

    @Test
    fun GameTestState.`Accept Aid is not an opt out while self targeting remains a no op`() =
        runStatSpyTest { deps ->
            val target = preparePlayers(deps, acceptAid = false, casts = 2)

            player.withProtectedAccess { deps.statSpy.cast(this, player) }
            assertEquals(4, player.count(objs.astral_rune))
            assertModalNotOpen(interfaces.dream_player_stats)

            player.withProtectedAccess { deps.statSpy.cast(this, target) }
            assertEquals(2, player.count(objs.astral_rune))
            assertModalOpen(interfaces.dream_player_stats)
            assertMessageSent("Caster is spying on your stats!", target)
        }

    @Test
    fun GameTestState.`recast lock lasts exactly five ticks`() = runStatSpyTest { deps ->
        val first = preparePlayers(deps, casts = 3)
        val second = registerRecipient("Second")

        player.withProtectedAccess { deps.statSpy.cast(this, first) }
        assertEquals(4, player.count(objs.astral_rune))
        player.ifButton(components.dream_player_stats_close)
        advance()
        assertModalNotOpen(interfaces.dream_player_stats)
        player.withProtectedAccess { deps.statSpy.cast(this, second) }
        assertEquals(4, player.count(objs.astral_rune))
        advance(3)
        player.withProtectedAccess { deps.statSpy.cast(this, second) }
        assertEquals(4, player.count(objs.astral_rune))
        advance()
        player.withProtectedAccess { deps.statSpy.cast(this, second) }
        assertEquals(2, player.count(objs.astral_rune))
    }

    @Test
    fun GameTestState.`Fountain supplies runes and suppresses cast xp`() = runStatSpyTest { deps ->
        val target = preparePlayers(deps, casts = 0)
        player.setVarBit(varbits.fountain_of_rune_active, 1)
        val startXp = player.statMap.getFineXP(stats.magic)

        player.withProtectedAccess { deps.statSpy.cast(this, target) }

        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        assertModalOpen(interfaces.dream_player_stats)
    }

    @Test
    fun GameTestState
        .`player target interaction casts at standard spell range and close button works`() =
        runStatSpyTest { deps ->
            val target = preparePlayers(deps, distance = 8)

            deps.interactions.interact(
                player,
                target,
                components.magic_spellbook_stat_spy,
                comsub = -1,
                objType = null,
            )
            advance()

            assertModalOpen(interfaces.dream_player_stats)
            assertEquals(0, player.count(objs.astral_rune))
            player.ifButton(components.dream_player_stats_close)
            advance()
            assertModalNotOpen(interfaces.dream_player_stats)
        }

    private fun GameTestScope.preparePlayers(
        deps: Dependencies,
        acceptAid: Boolean = true,
        casts: Int = 1,
        distance: Int = 1,
    ): Player {
        player.members = true
        player.displayName = "Caster"
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.dream_mentor, 28)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.clearInv()
        player.inv[0] = InvObj(objs.astral_rune, casts * 2)
        player.inv[1] = InvObj(objs.cosmic_rune, casts * 2)
        player.inv[2] = InvObj(objs.body_rune, casts * 5)
        deps.cooldowns.markCast(player, player.currentMapClock)
        client.clear()
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
        }

    private fun GameTestScope.assertRejected(
        deps: Dependencies,
        target: Player,
        message: String? = null,
    ) {
        val runesBefore =
            player.count(objs.astral_rune) +
                player.count(objs.cosmic_rune) +
                player.count(objs.body_rune)
        player.withProtectedAccess {
            deps.statSpy.cast(this, target)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(
            runesBefore,
            player.count(objs.astral_rune) +
                player.count(objs.cosmic_rune) +
                player.count(objs.body_rune),
        )
    }

    private fun GameTestState.runStatSpyTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            StatSpySpellScript::class,
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
        val statSpy: StatSpySpell,
        val spells: MagicSpellRegistry,
        val interactions: PlayerTInteractions,
        val components: ComponentTypeList,
        val seqs: SeqTypeList,
        val dbRows: DbRowTypeList,
        val cooldowns: UtilitySpellCooldownRepository,
    )

    private data class StatField(
        val stat: StatType,
        val current: ComponentType,
        val base: ComponentType,
    )

    private companion object {
        val statFields =
            listOf(
                StatField(
                    stats.attack,
                    components.dream_player_stats_attack,
                    components.dream_player_stats_attack_base,
                ),
                StatField(
                    stats.hitpoints,
                    components.dream_player_stats_hitpoints,
                    components.dream_player_stats_hitpoints_base,
                ),
                StatField(
                    stats.mining,
                    components.dream_player_stats_mining,
                    components.dream_player_stats_mining_base,
                ),
                StatField(
                    stats.strength,
                    components.dream_player_stats_strength,
                    components.dream_player_stats_strength_base,
                ),
                StatField(
                    stats.agility,
                    components.dream_player_stats_agility,
                    components.dream_player_stats_agility_base,
                ),
                StatField(
                    stats.smithing,
                    components.dream_player_stats_smithing,
                    components.dream_player_stats_smithing_base,
                ),
                StatField(
                    stats.defence,
                    components.dream_player_stats_defence,
                    components.dream_player_stats_defence_base,
                ),
                StatField(
                    stats.herblore,
                    components.dream_player_stats_herblore,
                    components.dream_player_stats_herblore_base,
                ),
                StatField(
                    stats.fishing,
                    components.dream_player_stats_fishing,
                    components.dream_player_stats_fishing_base,
                ),
                StatField(
                    stats.ranged,
                    components.dream_player_stats_ranged,
                    components.dream_player_stats_ranged_base,
                ),
                StatField(
                    stats.thieving,
                    components.dream_player_stats_thieving,
                    components.dream_player_stats_thieving_base,
                ),
                StatField(
                    stats.cooking,
                    components.dream_player_stats_cooking,
                    components.dream_player_stats_cooking_base,
                ),
                StatField(
                    stats.prayer,
                    components.dream_player_stats_prayer,
                    components.dream_player_stats_prayer_base,
                ),
                StatField(
                    stats.crafting,
                    components.dream_player_stats_crafting,
                    components.dream_player_stats_crafting_base,
                ),
                StatField(
                    stats.firemaking,
                    components.dream_player_stats_firemaking,
                    components.dream_player_stats_firemaking_base,
                ),
                StatField(
                    stats.magic,
                    components.dream_player_stats_magic,
                    components.dream_player_stats_magic_base,
                ),
                StatField(
                    stats.fletching,
                    components.dream_player_stats_fletching,
                    components.dream_player_stats_fletching_base,
                ),
                StatField(
                    stats.woodcutting,
                    components.dream_player_stats_woodcutting,
                    components.dream_player_stats_woodcutting_base,
                ),
                StatField(
                    stats.runecrafting,
                    components.dream_player_stats_runecrafting,
                    components.dream_player_stats_runecrafting_base,
                ),
                StatField(
                    stats.slayer,
                    components.dream_player_stats_slayer,
                    components.dream_player_stats_slayer_base,
                ),
                StatField(
                    stats.farming,
                    components.dream_player_stats_farming,
                    components.dream_player_stats_farming_base,
                ),
                StatField(
                    stats.construction,
                    components.dream_player_stats_construction,
                    components.dream_player_stats_construction_base,
                ),
                StatField(
                    stats.hunter,
                    components.dream_player_stats_hunter,
                    components.dream_player_stats_hunter_base,
                ),
            )
    }
}
