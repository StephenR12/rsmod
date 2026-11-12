package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.hitmark_groups
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.stat.stat
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
import org.rsmod.content.skills.cooking.TeaDrinkingScript
import org.rsmod.content.skills.herblore.GoadingPotionDrinking
import org.rsmod.content.skills.herblore.HerbloreModule
import org.rsmod.content.skills.herblore.HerbloreScript
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.Component
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class BoostPotionShareSpellTest {
    @Test
    fun GameTestState.`cache 233 pins spell interface potion and presentation contracts`() =
        runPotionShareTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_statboost_pot_share))

            assertEquals(9126, spell.obj.id)
            assertEquals(84, spell.levelReq)
            assertEquals(88.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.earth_rune to 12, objs.water_rune to 10, objs.astral_rune to 3),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_boost_potion_share.id ushr 16)
            assertEquals(130, components.magic_spellbook_boost_potion_share.id and 0xFFFF)
            assertEquals(
                emptyList<String>(),
                deps.components[components.magic_spellbook_boost_potion_share].op.toList(),
            )
            assertEquals(4413, seqs.lunar_human_magic_summon2.id)
            assertEquals(733, spotanims.quest_lunar_spellbook_cure_poison_spot_anim.id)
            assertEquals(102, varps.poison.id)
            assertEquals(0, timers.toxins.id)
            assertEquals(3981, varbits.antifire_potion.id)
            assertEquals(6101, varbits.super_antifire_potion.id)
            assertEquals(19, timers.antifire_effect.id)
            assertEquals(11294, varbits.goading_potion_timer.id)
            assertEquals(20, timers.goading_effect.id)
            assertEquals(
                listOf(1, 2, 3, 4),
                listOf(
                        objs.super_combat_potion_1,
                        objs.super_combat_potion_2,
                        objs.super_combat_potion_3,
                        objs.super_combat_potion_4,
                    )
                    .map { BoostPotionFamily.SuperCombat.doseFor(it.id) },
            )
            assertEquals(
                listOf(
                    179,
                    177,
                    175,
                    2446,
                    185,
                    183,
                    181,
                    2448,
                    5949,
                    5947,
                    5945,
                    5943,
                    5958,
                    5956,
                    5954,
                    5952,
                    12911,
                    12909,
                    12907,
                    12905,
                    12919,
                    12917,
                    12915,
                    12913,
                    29833,
                    29830,
                    29827,
                    29824,
                    10931,
                    10929,
                    10927,
                    10925,
                ),
                listOf(
                        objs.antipoison_1,
                        objs.antipoison_2,
                        objs.antipoison_3,
                        objs.antipoison_4,
                        objs.superantipoison_1,
                        objs.superantipoison_2,
                        objs.superantipoison_3,
                        objs.superantipoison_4,
                        objs.antidote_plus_1,
                        objs.antidote_plus_2,
                        objs.antidote_plus_3,
                        objs.antidote_plus_4,
                        objs.antidote_plus_plus_1,
                        objs.antidote_plus_plus_2,
                        objs.antidote_plus_plus_3,
                        objs.antidote_plus_plus_4,
                        objs.anti_venom_1,
                        objs.anti_venom_2,
                        objs.anti_venom_3,
                        objs.anti_venom_4,
                        objs.anti_venom_plus_1,
                        objs.anti_venom_plus_2,
                        objs.anti_venom_plus_3,
                        objs.anti_venom_plus_4,
                        objs.extended_anti_venom_plus_1,
                        objs.extended_anti_venom_plus_2,
                        objs.extended_anti_venom_plus_3,
                        objs.extended_anti_venom_plus_4,
                        objs.sanfew_serum_1,
                        objs.sanfew_serum_2,
                        objs.sanfew_serum_3,
                        objs.sanfew_serum_4,
                    )
                    .map { it.id },
            )
        }

    @Test
    fun GameTestState
        .`four-dose potion affects caster and first four eligible players in five by five`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.super_attack_4)
            setLevel(player, stats.attack, base = 80, current = 20)
            val recipients =
                listOf(-2 to -2, -2 to 2, 0 to 0, 2 to -2, 2 to 2).mapIndexed { index, (x, z) ->
                    registerRecipient("Recipient$index", x, z).also {
                        setLevel(it, stats.attack, base = 80, current = 20)
                    }
                }
            val disabled =
                registerRecipient("Disabled", 1, 0, acceptAid = false).also {
                    setLevel(it, stats.attack, base = 80, current = 20)
                }
            val outside =
                registerRecipient("Outside", 3, 0).also {
                    setLevel(it, stats.attack, base = 80, current = 20)
                }
            val upstairs =
                registerPlayer(coords = player.coords.translateLevel(1)).also {
                    it.setVarBit(varbits.option_acceptaid, 1)
                    setLevel(it, stats.attack, base = 80, current = 20)
                }
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

            assertEquals(37, player.stat(stats.attack))
            recipients.take(4).forEach {
                assertEquals(37, it.stat(stats.attack))
                assertPotionGraphic(it)
            }
            assertEquals(20, recipients.last().stat(stats.attack))
            assertEquals(20, disabled.stat(stats.attack))
            assertEquals(20, outside.stat(stats.attack))
            assertEquals(20, upstairs.stat(stats.attack))
            assertPotionGraphic(player)
            assertEquals(1, player.count(objs.empty_vial))
            assertEquals(880, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(seqs.lunar_human_magic_summon2.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`all thirteen ordinary stat boost families apply exact effects`() =
        runPotionShareTest { deps ->
            val recipient = registerRecipient("Recipient", 1, 0)
            val cases =
                listOf(
                    objs.attack_potion_1,
                    objs.strength_potion_1,
                    objs.defence_potion_1,
                    objs.ranging_potion_1,
                    objs.magic_potion_1,
                    objs.super_attack_1,
                    objs.super_strength_1,
                    objs.super_defence_1,
                    objs.super_combat_potion_1,
                    objs.bastion_potion_1,
                    objs.battlemage_potion_1,
                    objs.agility_potion_1,
                    objs.fishing_potion_1,
                )

            for (potion in cases) {
                prepareCaster(potion)
                for (stat in affectedStats) {
                    setLevel(player, stat, base = 80, current = if (stat == stats.magic) 99 else 20)
                    setLevel(recipient, stat, base = 80, current = 20)
                }

                player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

                when (BoostPotionFamily.forObj(potion.id)) {
                    BoostPotionFamily.Attack -> assertEquals(31, recipient.stat(stats.attack))
                    BoostPotionFamily.Strength -> assertEquals(31, recipient.stat(stats.strength))
                    BoostPotionFamily.Defence -> assertEquals(31, recipient.stat(stats.defence))
                    BoostPotionFamily.Ranging -> assertEquals(32, recipient.stat(stats.ranged))
                    BoostPotionFamily.Magic -> assertEquals(24, recipient.stat(stats.magic))
                    BoostPotionFamily.SuperAttack -> assertEquals(37, recipient.stat(stats.attack))
                    BoostPotionFamily.SuperStrength ->
                        assertEquals(37, recipient.stat(stats.strength))
                    BoostPotionFamily.SuperDefence ->
                        assertEquals(37, recipient.stat(stats.defence))
                    BoostPotionFamily.SuperCombat -> {
                        assertEquals(37, recipient.stat(stats.attack))
                        assertEquals(37, recipient.stat(stats.strength))
                        assertEquals(37, recipient.stat(stats.defence))
                    }
                    BoostPotionFamily.Bastion -> {
                        assertEquals(32, recipient.stat(stats.ranged))
                        assertEquals(37, recipient.stat(stats.defence))
                    }
                    BoostPotionFamily.Battlemage -> {
                        assertEquals(24, recipient.stat(stats.magic))
                        assertEquals(37, recipient.stat(stats.defence))
                    }
                    BoostPotionFamily.Agility -> assertEquals(23, recipient.stat(stats.agility))
                    BoostPotionFamily.Fishing -> assertEquals(23, recipient.stat(stats.fishing))
                    BoostPotionFamily.Antipoison,
                    BoostPotionFamily.Superantipoison,
                    BoostPotionFamily.AntidotePlus,
                    BoostPotionFamily.AntidotePlusPlus,
                    BoostPotionFamily.AntiVenom,
                    BoostPotionFamily.AntiVenomPlus,
                    BoostPotionFamily.ExtendedAntiVenomPlus,
                    BoostPotionFamily.SanfewSerum,
                    BoostPotionFamily.Antifire,
                    BoostPotionFamily.ExtendedAntifire,
                    BoostPotionFamily.SuperAntifire,
                    BoostPotionFamily.ExtendedSuperAntifire ->
                        error("Unexpected protection potion: $potion")
                    BoostPotionFamily.Goading -> error("Unexpected goading potion: $potion")
                    null -> error("Unexpected potion: $potion")
                }
                assertEquals(1, player.count(objs.empty_vial))
                advance(3)
            }
        }

    @Test
    fun GameTestState.`direct stat boost doses apply every exact immediate effect`() =
        runPotionShareTest { deps ->
            val cases =
                listOf(
                    objs.attack_potion_1,
                    objs.strength_potion_1,
                    objs.defence_potion_1,
                    objs.ranging_potion_1,
                    objs.magic_potion_1,
                    objs.super_attack_1,
                    objs.super_strength_1,
                    objs.super_defence_1,
                    objs.super_combat_potion_1,
                    objs.bastion_potion_1,
                    objs.battlemage_potion_1,
                    objs.agility_potion_1,
                    objs.fishing_potion_1,
                )

            for (potion in cases) {
                prepareCaster(potion, casts = 0)
                for (stat in affectedStats) {
                    setLevel(player, stat, base = 80, current = 20)
                }

                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
                }

                when (BoostPotionFamily.forObj(potion.id)) {
                    BoostPotionFamily.Attack -> assertEquals(31, player.stat(stats.attack))
                    BoostPotionFamily.Strength -> assertEquals(31, player.stat(stats.strength))
                    BoostPotionFamily.Defence -> assertEquals(31, player.stat(stats.defence))
                    BoostPotionFamily.Ranging -> assertEquals(32, player.stat(stats.ranged))
                    BoostPotionFamily.Magic -> assertEquals(24, player.stat(stats.magic))
                    BoostPotionFamily.SuperAttack -> assertEquals(37, player.stat(stats.attack))
                    BoostPotionFamily.SuperStrength -> assertEquals(37, player.stat(stats.strength))
                    BoostPotionFamily.SuperDefence -> assertEquals(37, player.stat(stats.defence))
                    BoostPotionFamily.SuperCombat -> {
                        assertEquals(37, player.stat(stats.attack))
                        assertEquals(37, player.stat(stats.strength))
                        assertEquals(37, player.stat(stats.defence))
                    }
                    BoostPotionFamily.Bastion -> {
                        assertEquals(32, player.stat(stats.ranged))
                        assertEquals(37, player.stat(stats.defence))
                    }
                    BoostPotionFamily.Battlemage -> {
                        assertEquals(24, player.stat(stats.magic))
                        assertEquals(37, player.stat(stats.defence))
                    }
                    BoostPotionFamily.Agility -> assertEquals(23, player.stat(stats.agility))
                    BoostPotionFamily.Fishing -> assertEquals(23, player.stat(stats.fishing))
                    BoostPotionFamily.Antipoison,
                    BoostPotionFamily.Superantipoison,
                    BoostPotionFamily.AntidotePlus,
                    BoostPotionFamily.AntidotePlusPlus,
                    BoostPotionFamily.AntiVenom,
                    BoostPotionFamily.AntiVenomPlus,
                    BoostPotionFamily.ExtendedAntiVenomPlus,
                    BoostPotionFamily.SanfewSerum,
                    BoostPotionFamily.Antifire,
                    BoostPotionFamily.ExtendedAntifire,
                    BoostPotionFamily.SuperAntifire,
                    BoostPotionFamily.ExtendedSuperAntifire ->
                        error("Unexpected protection potion: $potion")
                    BoostPotionFamily.Goading -> error("Unexpected goading potion: $potion")
                    null -> error("Unexpected potion: $potion")
                }
                assertEquals(1, player.count(objs.empty_vial))
                advance(3)
            }
        }

    @Test
    fun GameTestState
        .`direct only combat and hunter potions apply their exact immediate effects`() =
        runPotionShareTest { deps ->
            val cases = listOf(objs.combat_potion_1, objs.hunter_potion_1)

            for (potion in cases) {
                prepareCaster(potion, casts = 0)
                setLevel(player, stats.attack, base = 80, current = 20)
                setLevel(player, stats.strength, base = 80, current = 20)
                setLevel(player, stats.hunter, base = 80, current = 20)

                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
                }

                when (DirectOnlyBoostPotionFamily.forObj(potion.id)) {
                    DirectOnlyBoostPotionFamily.Combat -> {
                        assertEquals(31, player.stat(stats.attack))
                        assertEquals(31, player.stat(stats.strength))
                        assertEquals(20, player.stat(stats.hunter))
                    }
                    DirectOnlyBoostPotionFamily.Hunter -> {
                        assertEquals(20, player.stat(stats.attack))
                        assertEquals(20, player.stat(stats.strength))
                        assertEquals(23, player.stat(stats.hunter))
                    }
                    null -> error("Unexpected direct-only potion: $potion")
                }
                assertEquals(1, player.count(objs.empty_vial))
                advance(3)
            }
        }

    @Test
    fun GameTestState.`all eight poison remedy families use exact varp protection states`() =
        runPotionShareTest { deps ->
            val recipient = registerRecipient("Recipient", 1, 0)
            val cases =
                mapOf(
                    objs.antipoison_1 to -5,
                    objs.superantipoison_1 to -20,
                    objs.antidote_plus_1 to -30,
                    objs.antidote_plus_plus_1 to -40,
                    objs.anti_venom_1 to -41,
                    objs.anti_venom_plus_1 to -50,
                    objs.extended_anti_venom_plus_1 to -59,
                    objs.sanfew_serum_1 to -20,
                )

            for ((potion, expectedState) in cases) {
                prepareCaster(potion)
                player.setVarp(varps.poison, 0)
                recipient.setVarp(varps.poison, 0)

                player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

                assertEquals(expectedState, player.vars[varps.poison])
                assertEquals(expectedState, recipient.vars[varps.poison])
                assertTrue(deps.poison.isPoisonImmune(recipient))
                assertEquals(1, player.count(objs.empty_vial))
                advance(3)
            }
        }

    @Test
    fun GameTestState.`all four antifire families use exact cache items durations and displays`() =
        runPotionShareTest { deps ->
            val recipient = registerRecipient("Recipient", 1, 0)
            val cases =
                listOf(
                    AntifireCase(
                        objs.antifire_potion_1,
                        AntifireProtectionStrength.Antifire,
                        regularDisplay = 20,
                    ),
                    AntifireCase(
                        objs.extended_antifire_1,
                        AntifireProtectionStrength.ExtendedAntifire,
                        regularDisplay = 40,
                    ),
                    AntifireCase(
                        objs.super_antifire_potion_1,
                        AntifireProtectionStrength.SuperAntifire,
                        superiorDisplay = 15,
                    ),
                    AntifireCase(
                        objs.extended_super_antifire_1,
                        AntifireProtectionStrength.ExtendedSuperAntifire,
                        superiorDisplay = 30,
                    ),
                )

            for (case in cases) {
                prepareCaster(case.potion)

                player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

                assertEquals(case.strength, BoostPotionFamily.forObj(case.potion.id)?.antifire())
                for (affected in listOf(player, recipient)) {
                    assertEquals(case.regularDisplay, affected.vars[varbits.antifire_potion])
                    assertEquals(case.superiorDisplay, affected.vars[varbits.super_antifire_potion])
                    assertEquals(
                        case.strength.durationCycles,
                        deps.antifire.remainingCycles(affected, case.strength.superior),
                    )
                }
                assertEquals(1, player.count(objs.empty_vial))
                deps.antifire.clear(player)
                deps.antifire.clear(recipient)
                advance(3)
            }

            assertEquals(listOf(2458, 11957, 21987, 22218), cases.map { it.potion.id })
        }

    @Test
    fun GameTestState
        .`antifire lifecycle warns expires resumes and latest same tier dose resets`() =
        runPotionShareTest { deps ->
            deps.antifire.apply(player, AntifireProtectionStrength.Antifire)
            assertTrue(deps.antifire.isRegularActive(player))
            assertEquals(600, deps.antifire.remainingCycles(player, superior = false))
            client.clear()

            advance(574)
            assertEquals(1, player.vars[varbits.antifire_potion])
            advance()
            player.withProtectedAccess {
                assertMessageSent(AntifireProtectionRepository.REGULAR_WARNING_MESSAGE)
            }

            client.clear()
            advance(AntifireProtectionRepository.WARNING_CYCLES)
            assertFalse(deps.antifire.isRegularActive(player))
            assertEquals(0, player.vars[varbits.antifire_potion])
            player.withProtectedAccess {
                assertMessageSent(AntifireProtectionRepository.REGULAR_EXPIRED_MESSAGE)
            }

            deps.antifire.apply(player, AntifireProtectionStrength.ExtendedAntifire)
            advance(100)
            deps.antifire.pause(player)
            assertEquals(1_100, deps.antifire.remainingCycles(player, superior = false))
            deps.antifire.resume(player)
            deps.antifire.apply(player, AntifireProtectionStrength.Antifire)
            assertEquals(600, deps.antifire.remainingCycles(player, superior = false))
            assertEquals(20, player.vars[varbits.antifire_potion])

            deps.antifire.apply(player, AntifireProtectionStrength.ExtendedSuperAntifire)
            assertTrue(deps.antifire.isSuperiorActive(player))
            assertEquals(600, deps.antifire.remainingCycles(player, superior = true))
            assertEquals(30, player.vars[varbits.super_antifire_potion])
        }

    @Test
    fun GameTestState.`standard and metallic dragonfire consume exact protection layers`() =
        runPotionShareTest { deps ->
            val resolver = deps.dragonfire

            assertEquals(
                50,
                resolver.maximumDamage(player, DragonfireBreath.Chromatic, false, false),
            )
            assertEquals(
                30,
                resolver.maximumDamage(player, DragonfireBreath.Chromatic, true, false),
            )
            assertEquals(5, resolver.maximumDamage(player, DragonfireBreath.Chromatic, false, true))
            player.setVarBit(varbits.protect_from_magic, 1)
            assertEquals(
                10,
                resolver.maximumDamage(player, DragonfireBreath.Chromatic, false, false),
            )
            assertEquals(
                50,
                resolver.maximumDamage(player, DragonfireBreath.Metallic, false, false),
            )

            player.setVarBit(varbits.protect_from_magic, 0)
            deps.antifire.apply(player, AntifireProtectionStrength.Antifire)
            assertEquals(
                35,
                resolver.maximumDamage(player, DragonfireBreath.Chromatic, false, false),
            )
            assertEquals(
                15,
                resolver.maximumDamage(player, DragonfireBreath.Chromatic, true, false),
            )
            assertEquals(
                0,
                resolver.resolveDamage(player, DragonfireBreath.Chromatic, false, false, 14),
            )
            assertEquals(
                1,
                resolver.resolveDamage(player, DragonfireBreath.Chromatic, false, false, 16),
            )
            assertEquals(0, resolver.maximumDamage(player, DragonfireBreath.Chromatic, false, true))
            player.setVarBit(varbits.protect_from_magic, 1)
            assertEquals(
                0,
                resolver.maximumDamage(player, DragonfireBreath.Chromatic, false, false),
            )
            assertEquals(
                35,
                resolver.maximumDamage(player, DragonfireBreath.Metallic, false, false),
            )
            assertEquals(0, resolver.maximumDamage(player, DragonfireBreath.Metallic, false, true))

            deps.antifire.apply(player, AntifireProtectionStrength.SuperAntifire)
            assertEquals(
                0,
                resolver.maximumDamage(player, DragonfireBreath.Chromatic, false, false),
            )
            assertEquals(0, resolver.maximumDamage(player, DragonfireBreath.Metallic, false, false))
        }

    @Test
    fun GameTestState.`goading potion share uses exact items duration display and lifecycle`() =
        runPotionShareTest { deps ->
            val recipient = registerRecipient("Recipient", 1, 0)
            prepareCaster(objs.goading_potion_1)

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

            assertEquals(
                listOf(30146, 30143, 30140, 30137),
                listOf(
                        objs.goading_potion_1,
                        objs.goading_potion_2,
                        objs.goading_potion_3,
                        objs.goading_potion_4,
                    )
                    .map { it.id },
            )
            for (affected in listOf(player, recipient)) {
                assertTrue(deps.goading.isActive(affected))
                assertEquals(600, deps.goading.remainingCycles(affected))
                assertEquals(100, affected.vars[varbits.goading_potion_timer])
            }

            advance(599)
            assertEquals(1, player.vars[varbits.goading_potion_timer])
            advance()
            assertFalse(deps.goading.isActive(player))
            assertFalse(deps.goading.isActive(recipient))

            deps.goading.apply(player)
            advance(100)
            deps.goading.pause(player)
            assertEquals(500, deps.goading.remainingCycles(player))
            advance(100)
            assertEquals(500, deps.goading.remainingCycles(player))
            deps.goading.resume(player)
            advance(100)
            assertEquals(400, deps.goading.remainingCycles(player))
            deps.goading.apply(player)
            assertEquals(600, deps.goading.remainingCycles(player))
            assertEquals(100, player.vars[varbits.goading_potion_timer])

            val localRelocation = player.coords.translateX(8)
            allocZoneCollision(localRelocation)
            client.clear()
            player.withProtectedAccess { teleport(localRelocation) }
            assertTrue(deps.goading.isActive(player))
            assertTrue(client.filterIsInstance<MessageGame>().isEmpty())

            val spellDestination = player.coords.translateX(16)
            allocZoneCollision(spellDestination)
            client.clear()
            player.withProtectedAccess { spellTeleport(spellDestination) }
            assertFalse(deps.goading.isActive(player))
            assertEquals(0, player.vars[varbits.goading_potion_timer])
            assertMessageSent(GoadingPotionRepository.EXPIRED_MESSAGE)
        }

    @Test
    fun GameTestState.`direct goading dose applies and resets the six minute effect`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.goading_potion_4)

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
            }

            assertEquals(objs.goading_potion_3.id, player.inv[POTION_SLOT]?.id)
            assertTrue(deps.goading.isActive(player))
            assertEquals(600, deps.goading.remainingCycles(player))
            assertEquals(100, player.vars[varbits.goading_potion_timer])
            assertMessageSent(GoadingPotionDrinking.DRINK_MESSAGE)

            advance(103)
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
            }

            assertEquals(objs.goading_potion_2.id, player.inv[POTION_SLOT]?.id)
            assertEquals(600, deps.goading.remainingCycles(player))
            assertEquals(100, player.vars[varbits.goading_potion_timer])
        }

    @Test
    fun GameTestState.`direct antipoison doses downgrade venom then cure poison`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.antipoison_4)
            player.setVarp(varps.poison, 1_000_005)

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
            }

            assertEquals(objs.antipoison_3.id, player.inv[POTION_SLOT]?.id)
            assertEquals(80, player.vars[varps.poison])
            assertFalse(deps.poison.isPoisonImmune(player))

            advance(3)
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
            }

            assertEquals(objs.antipoison_2.id, player.inv[POTION_SLOT]?.id)
            assertEquals(-5, player.vars[varps.poison])
            assertTrue(deps.poison.isPoisonImmune(player))
            assertFalse(deps.poison.isVenomImmune(player))
        }

    @Test
    fun GameTestState.`direct higher remedies apply every exact poison protection state`() =
        runPotionShareTest { deps ->
            val ordinaryRemedies =
                listOf(
                    objs.superantipoison_4 to -20,
                    objs.antidote_plus_4 to -30,
                    objs.antidote_plus_plus_4 to -40,
                )
            for ((potion, expectedState) in ordinaryRemedies) {
                prepareCaster(potion)
                player.setVarp(varps.poison, 1_000_005)

                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
                }

                assertEquals(80, player.vars[varps.poison])
                advance(3)
                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
                }
                assertEquals(expectedState, player.vars[varps.poison])
                advance(3)
            }

            val venomRemedies =
                listOf(
                    objs.anti_venom_4 to -41,
                    objs.anti_venom_plus_4 to -50,
                    objs.extended_anti_venom_plus_4 to -59,
                )
            for ((potion, expectedState) in venomRemedies) {
                prepareCaster(potion)
                player.setVarp(varps.poison, 1_000_005)

                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
                }

                assertEquals(expectedState, player.vars[varps.poison])
                assertTrue(deps.poison.isVenomImmune(player))
                advance(3)
            }
        }

    @Test
    fun GameTestState.`direct Sanfew serum applies every immediate effect`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.sanfew_serum_4)
            setLevel(player, stats.attack, base = 80, current = 20)
            setLevel(player, stats.prayer, base = 99, current = 10)
            setLevel(player, stats.hitpoints, base = 99, current = 40)
            player.setVarp(varps.poison, 1_000_005)
            player.setVarp(varps.disease, 7)

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
            }

            assertEquals(objs.sanfew_serum_3.id, player.inv[POTION_SLOT]?.id)
            assertEquals(48, player.stat(stats.attack))
            assertEquals(43, player.stat(stats.prayer))
            assertEquals(40, player.stat(stats.hitpoints))
            assertEquals(80, player.vars[varps.poison])
            assertEquals(0, player.vars[varps.disease])
            assertEquals(1_500, deps.disease.remainingCycles(player))

            advance(3)
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
            }

            assertEquals(objs.sanfew_serum_2.id, player.inv[POTION_SLOT]?.id)
            assertEquals(76, player.stat(stats.attack))
            assertEquals(76, player.stat(stats.prayer))
            assertEquals(-20, player.vars[varps.poison])
            assertTrue(deps.poison.isPoisonImmune(player))
            assertEquals(1_500, deps.disease.remainingCycles(player))

            deps.events.publish(PlayerDeathEvent.Completed(player))
            assertEquals(0, deps.disease.remainingCycles(player))
        }

    @Test
    fun GameTestState.`direct antifire families apply every exact protection duration`() =
        runPotionShareTest { deps ->
            val cases =
                listOf(
                    Triple(
                        objs.antifire_potion_4,
                        objs.antifire_potion_3,
                        AntifireProtectionStrength.Antifire,
                    ),
                    Triple(
                        objs.extended_antifire_4,
                        objs.extended_antifire_3,
                        AntifireProtectionStrength.ExtendedAntifire,
                    ),
                    Triple(
                        objs.super_antifire_potion_4,
                        objs.super_antifire_potion_3,
                        AntifireProtectionStrength.SuperAntifire,
                    ),
                    Triple(
                        objs.extended_super_antifire_4,
                        objs.extended_super_antifire_3,
                        AntifireProtectionStrength.ExtendedSuperAntifire,
                    ),
                )

            for ((potion, replacement, strength) in cases) {
                prepareCaster(potion)
                player.setVarp(varps.dragonquest, 1)
                deps.antifire.clear(player)

                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
                }

                assertEquals(replacement.id, player.inv[POTION_SLOT]?.id)
                assertEquals(strength.superior, deps.antifire.isSuperiorActive(player))
                assertEquals(!strength.superior, deps.antifire.isRegularActive(player))
                assertEquals(
                    strength.durationCycles,
                    deps.antifire.remainingCycles(player, strength.superior),
                )
                deps.antifire.clear(player)
                advance(3)
            }
        }

    @Test
    fun GameTestState.`ordinary tea consumption cancels goading with the exact message`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.goading_potion_4)
            deps.goading.apply(player)
            player.inv[POTION_SLOT] = InvObj(objs.cup_of_tea)
            client.clear()

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
            }

            assertFalse(deps.goading.isActive(player))
            assertEquals(0, player.vars[varbits.goading_potion_timer])
            assertEquals(objs.empty_cup.id, player.inv[POTION_SLOT]?.id)
            assertMessageSent(GoadingPotionRepository.EXPIRED_MESSAGE)

            advance(3)
            player.inv[POTION_SLOT] = InvObj(objs.cup_of_tea)
            client.clear()
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
            }
            assertEquals(objs.empty_cup.id, player.inv[POTION_SLOT]?.id)
            assertTrue(client.filterIsInstance<MessageGame>().isEmpty())
        }

    @Test
    fun GameTestState.`ordinary remedies downgrade venom while anti venom remedies cure it`() =
        runPotionShareTest { deps ->
            val recipient = registerRecipient("Recipient", 1, 0)
            prepareCaster(objs.antidote_plus_plus_2, casts = 2)
            recipient.setVarp(varps.poison, 1_000_005)

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

            assertEquals(80, recipient.vars[varps.poison])
            advance(3)
            player.inv[POTION_SLOT] = InvObj(objs.anti_venom_1)
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

            assertEquals(-41, recipient.vars[varps.poison])
            assertTrue(deps.poison.isVenomImmune(recipient))
        }

    @Test
    fun GameTestState
        .`toxin interval preserves venom ranges and weaker remedies cannot shorten protection`() =
        runPotionShareTest { deps ->
            val recipient = registerRecipient("Recipient", 1, 0)
            prepareCaster(objs.anti_venom_1)

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

            assertEquals(-41, recipient.vars[varps.poison])
            val untilInterval =
                PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES -
                    Math.floorMod(
                        recipient.currentMapClock,
                        PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES,
                    )
            advance(untilInterval - 1)
            assertEquals(-41, recipient.vars[varps.poison])
            advance()
            assertEquals(-40, recipient.vars[varps.poison])
            advance(60)
            assertEquals(-38, recipient.vars[varps.poison])
            assertFalse(deps.poison.isVenomImmune(recipient))
            assertTrue(deps.poison.isPoisonImmune(recipient))

            deps.poison.apply(recipient, PoisonProtectionStrength.AntiVenomPlus)
            assertEquals(-50, recipient.vars[varps.poison])
            deps.poison.apply(recipient, PoisonProtectionStrength.Antipoison)
            assertEquals(-50, recipient.vars[varps.poison])
        }

    @Test
    fun GameTestState.`poison severity deals exact recurring damage and naturally expires`() =
        runPotionShareTest { deps ->
            setLevel(player, stats.hitpoints, base = 99, current = 99)

            assertTrue(deps.poison.inflictPoison(player, severity = 30))
            assertEquals(30, player.vars[varps.poison])
            deps.poison.tick(player)

            assertEquals(93, player.stat(stats.hitpoints))
            assertEquals(29, player.vars[varps.poison])
            assertEquals(
                hitmark_groups.poison_damage.lit.id,
                Hitmark(player.activeHitmarks.getLong(0)).self,
            )
            advance(PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES * 4)
            assertEquals(69, player.stat(stats.hitpoints))
            assertEquals(25, player.vars[varps.poison])
            advance(PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES)
            assertEquals(64, player.stat(stats.hitpoints))
            assertEquals(24, player.vars[varps.poison])

            setLevel(player, stats.hitpoints, base = 99, current = 99)
            player.setVarp(varps.poison, 1)
            deps.poison.resume(player)
            advance(untilNextToxinInterval(player))
            assertEquals(98, player.stat(stats.hitpoints))
            assertEquals(0, player.vars[varps.poison])
            advance(PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES)
            assertEquals(98, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState.`venom rises from six to twenty and does not naturally expire`() =
        runPotionShareTest { deps ->
            setLevel(player, stats.hitpoints, base = 99, current = 99)

            assertTrue(deps.poison.inflictVenom(player))
            deps.poison.tick(player)
            assertEquals(93, player.stat(stats.hitpoints))
            assertEquals(1_000_001, player.vars[varps.poison])
            assertEquals(
                hitmark_groups.venom.lit.id,
                Hitmark(player.activeHitmarks.getLong(0)).self,
            )
            advance(PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES)
            assertEquals(85, player.stat(stats.hitpoints))
            assertEquals(1_000_002, player.vars[varps.poison])
            advance(PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES * 5)
            assertEquals(15, player.stat(stats.hitpoints))
            assertEquals(1_000_007, player.vars[varps.poison])
            setLevel(player, stats.hitpoints, base = 99, current = 99)
            advance(PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES)
            assertEquals(79, player.stat(stats.hitpoints))
            assertEquals(1_000_007, player.vars[varps.poison])
        }

    @Test
    fun GameTestState.`interfaces pause toxins and immunity rejects matching infliction`() =
        runPotionShareTest { deps ->
            setLevel(player, stats.hitpoints, base = 99, current = 99)
            assertTrue(deps.poison.inflictPoison(player, severity = 10))
            assertFalse(deps.poison.inflictPoison(player, severity = 5))
            assertFalse(deps.poison.inflictPoison(player, severity = 15))
            player.ifOpenMain(interfaces.settings)

            advance(untilNextToxinInterval(player) + 10)
            assertEquals(99, player.stat(stats.hitpoints))
            assertEquals(10, player.vars[varps.poison])
            player.ifClose()
            advance()
            assertEquals(97, player.stat(stats.hitpoints))
            assertEquals(9, player.vars[varps.poison])
            advance(PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES - 1)
            assertEquals(97, player.stat(stats.hitpoints))
            advance()
            assertEquals(95, player.stat(stats.hitpoints))

            deps.poison.apply(player, PoisonProtectionStrength.AntiVenomPlus)
            assertFalse(deps.poison.inflictPoison(player, severity = 30))
            assertFalse(deps.poison.inflictVenom(player))
            assertEquals(-50, player.vars[varps.poison])

            deps.poison.apply(player, PoisonProtectionStrength.Antipoison)
            player.setVarp(varps.poison, -5)
            assertTrue(deps.poison.inflictVenom(player))
            assertEquals(1_000_000, player.vars[varps.poison])
        }

    @Test
    fun GameTestState.`recurring toxin damage can queue player death`() =
        runPotionShareTest { deps ->
            setLevel(player, stats.hitpoints, base = 99, current = 1)
            assertTrue(deps.poison.inflictPoison(player, severity = 5))

            deps.poison.tick(player)

            assertEquals(0, player.stat(stats.hitpoints))
            assertTrue(queues.death in player.queueList)
        }

    @Test
    fun GameTestState.`available doses cap recipients and caster dose remains free`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.bastion_potion_2)
            setLevel(player, stats.ranged, base = 80, current = 20)
            val recipients =
                (0 until 4).map { index ->
                    registerRecipient("Recipient$index", index % 3 - 1, index / 3).also {
                        setLevel(it, stats.ranged, base = 80, current = 20)
                    }
                }

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

            assertEquals(32, player.stat(stats.ranged))
            recipients.take(2).forEach { assertEquals(32, it.stat(stats.ranged)) }
            recipients.drop(2).forEach { assertEquals(20, it.stat(stats.ranged)) }
            assertEquals(1, player.count(objs.empty_vial))
        }

    @Test
    fun GameTestState
        .`no recipient incompatible potion access and Castle Wars failures are atomic`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.super_attack_4)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertAtomic(magicXp, objs.super_attack_4)

            registerRecipient("Recipient", 1, 0)
            player.inv[POTION_SLOT] = InvObj(objs.combat_potion_4)
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.combat_potion_4))

            player.inv[POTION_SLOT] = InvObj(objs.hunter_potion_4)
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.hunter_potion_4))

            for (unsupportedPotion in
                listOf(
                    objs.saradomin_brew_4,
                    objs.zamorak_brew_4,
                    objs.ancient_brew_4,
                    objs.forgotten_brew_4,
                    objs.surge_potion_4,
                    objs.menaphite_remedy_4,
                )) {
                player.inv[POTION_SLOT] = InvObj(unsupportedPotion)
                player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
                assertEquals(1, player.count(unsupportedPotion))
            }

            player.inv[POTION_SLOT] = InvObj(objs.super_attack_4)
            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.stats[stats.magic] = 83
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[2] = null
            player.withProtectedAccess {
                deps.spell.cast(this, POTION_SLOT)
                assertMessageSent("You do not have enough Astral Runes to cast this spell.")
            }
            assertEquals(1, player.count(objs.super_attack_4))

            prepareCaster(objs.super_attack_4)
            player.teleport(CoordGrid(0, 37, 48, 32, 32))
            registerRecipient("CastleWarsRecipient", 1, 0)
            player.withProtectedAccess {
                deps.spell.cast(this, POTION_SLOT)
                assertMessageSent("You don't want to help your enemies")
            }
            assertEquals(1, player.count(objs.super_attack_4))
            assertEquals(3, player.count(objs.astral_rune))
        }

    @Test
    fun GameTestState
        .`full inventory Fountain cooldown and direct interface dispatch are supported`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.super_combat_potion_4, casts = 0)
            repeat(player.inv.size) { player.inv[it] = InvObj(objs.logs) }
            player.inv[POTION_SLOT] = InvObj(objs.super_combat_potion_4)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            registerRecipient("Recipient", 1, 0)
            val magicXp = player.statMap.getFineXP(stats.magic)

            deps.events.publish(
                IfOverlayButtonT(
                    player = player,
                    selectedSlot = -1,
                    selectedObj = objTypes[objs.spell_statboost_pot_share],
                    targetSlot = POTION_SLOT,
                    targetObj = objTypes[objs.super_combat_potion_4],
                    selectedComponent =
                        Component(components.magic_spellbook_boost_potion_share.packed),
                    targetComponent = Component(components.inv_items.packed),
                )
            )

            assertEquals(1, player.count(objs.super_combat_potion_3))
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.super_combat_potion_3))
            advance(2)
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.super_combat_potion_3))
            advance()
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.super_combat_potion_2))
        }

    private fun GameTestScope.prepareCaster(potion: ObjType, casts: Int = 1) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.in_lms_game, 0)
        player.clearInv()
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.earth_rune, casts * 12)
            player.inv[1] = InvObj(objs.water_rune, casts * 10)
            player.inv[2] = InvObj(objs.astral_rune, casts * 3)
        }
        player.inv[POTION_SLOT] = InvObj(potion)
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
        }

    private fun GameTestScope.setLevel(
        player: Player,
        stat: org.rsmod.game.type.stat.StatType,
        base: Int,
        current: Int,
    ) {
        player.setBaseLevel(stat, base)
        player.setCurrentLevel(stat, current)
    }

    private fun untilNextToxinInterval(player: Player): Int =
        PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES -
            Math.floorMod(player.currentMapClock, PoisonProtectionRepository.TOXIN_INTERVAL_CYCLES)

    private fun assertPotionGraphic(recipient: Player) {
        val graphic = EntitySpotanim(recipient.pendingSpotanims.getLong(0))
        assertEquals(spotanims.quest_lunar_spellbook_cure_poison_spot_anim.id, graphic.id)
        assertEquals(96, graphic.height)
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String) {
        player.withProtectedAccess {
            deps.spell.cast(this, POTION_SLOT)
            assertMessageSent(message)
        }
        assertEquals(1, player.count(objs.super_attack_4))
        assertEquals(3, player.count(objs.astral_rune))
    }

    private fun GameTestScope.assertAtomic(magicXp: Int, potion: ObjType) {
        assertEquals(1, player.count(potion))
        assertEquals(3, player.count(objs.astral_rune))
        assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
    }

    private fun GameTestState.runPotionShareTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            BoostPotionShareSpellScript::class,
            PotionProtectionScript::class,
            TeaDrinkingScript::class,
            HerbloreScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            install(HerbloreModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UtilitySpellCooldownRepository::class.java).`in`(Scopes.SINGLETON)
            bind(PoisonProtectionRepository::class.java).`in`(Scopes.SINGLETON)
            bind(DiseaseImmunityRepository::class.java).`in`(Scopes.SINGLETON)
            bind(AntifireProtectionRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StandardDragonfireDamageResolver::class.java).`in`(Scopes.SINGLETON)
            bind(GoadingPotionRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val spell: BoostPotionShareSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val events: EventBus,
        val poison: PoisonProtectionRepository,
        val disease: DiseaseImmunityRepository,
        val antifire: AntifireProtectionRepository,
        val dragonfire: StandardDragonfireDamageResolver,
        val goading: GoadingPotionRepository,
        val held: HeldInteractions,
    )

    private companion object {
        const val POTION_SLOT = 3

        data class AntifireCase(
            val potion: ObjType,
            val strength: AntifireProtectionStrength,
            val regularDisplay: Int = 0,
            val superiorDisplay: Int = 0,
        )

        fun BoostPotionFamily.antifire(): AntifireProtectionStrength =
            when (this) {
                BoostPotionFamily.Antifire -> AntifireProtectionStrength.Antifire
                BoostPotionFamily.ExtendedAntifire -> AntifireProtectionStrength.ExtendedAntifire
                BoostPotionFamily.SuperAntifire -> AntifireProtectionStrength.SuperAntifire
                BoostPotionFamily.ExtendedSuperAntifire ->
                    AntifireProtectionStrength.ExtendedSuperAntifire
                else -> error("Not an antifire family: $this")
            }

        val affectedStats =
            listOf(
                stats.attack,
                stats.strength,
                stats.defence,
                stats.ranged,
                stats.magic,
                stats.agility,
                stats.fishing,
            )
    }
}
