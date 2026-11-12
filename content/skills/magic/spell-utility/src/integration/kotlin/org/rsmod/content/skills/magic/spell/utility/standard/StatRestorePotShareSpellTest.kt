package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
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
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
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
import org.rsmod.content.skills.herblore.HerbloreModule
import org.rsmod.content.skills.herblore.HerbloreScript
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.Component

@Execution(ExecutionMode.SAME_THREAD)
class StatRestorePotShareSpellTest {
    @Test
    fun GameTestState.`cache 233 pins spell interface potion and presentation contracts`() =
        runPotionShareTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_res_pot_share))

            assertEquals(9123, spell.obj.id)
            assertEquals(81, spell.levelReq)
            assertEquals(84.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.earth_rune to 10, objs.water_rune to 10, objs.astral_rune to 2),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_stat_restore_pot_share.id ushr 16)
            assertEquals(127, components.magic_spellbook_stat_restore_pot_share.id and 0xFFFF)
            assertEquals(
                emptyList<String>(),
                deps.components[components.magic_spellbook_stat_restore_pot_share].op.toList(),
            )
            assertEquals(4413, seqs.lunar_human_magic_summon2.id)
            assertEquals(733, spotanims.quest_lunar_spellbook_cure_poison_spot_anim.id)
            assertEquals(456, varps.disease.id)
            assertEquals(24, varbits.stamina_duration.id)
            assertEquals(25, varbits.stamina_active.id)
            assertEquals(
                listOf(1, 2, 3, 4),
                listOf(
                        objs.restore_potion_1,
                        objs.restore_potion_2,
                        objs.restore_potion_3,
                        objs.restore_potion_4,
                    )
                    .map { StatRestorePotionFamily.Restore.doseFor(it.id) },
            )
        }

    @Test
    fun GameTestState
        .`four-dose potion affects caster and first four eligible players in five by five`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.restore_potion_4)
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

            assertEquals(54, player.stat(stats.attack))
            recipients.take(4).forEach {
                assertEquals(54, it.stat(stats.attack))
                assertPotionGraphic(it)
            }
            assertEquals(20, recipients.last().stat(stats.attack))
            assertEquals(20, disabled.stat(stats.attack))
            assertEquals(20, outside.stat(stats.attack))
            assertEquals(20, upstairs.stat(stats.attack))
            assertPotionGraphic(player)
            assertEquals(1, player.count(objs.empty_vial))
            assertEquals(840, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(seqs.lunar_human_magic_summon2.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`available doses cap recipients and caster dose remains free`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.restore_potion_2)
            setLevel(player, stats.attack, base = 80, current = 20)
            val recipients =
                (0 until 4).map { index ->
                    registerRecipient("Recipient$index", index % 3 - 1, index / 3).also {
                        setLevel(it, stats.attack, base = 80, current = 20)
                    }
                }

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

            assertEquals(54, player.stat(stats.attack))
            recipients.take(2).forEach { assertEquals(54, it.stat(stats.attack)) }
            recipients.drop(2).forEach { assertEquals(20, it.stat(stats.attack)) }
            assertEquals(1, player.count(objs.empty_vial))
        }

    @Test
    fun GameTestState.`all seven accepted families apply exact immediate effects`() =
        runPotionShareTest { deps ->
            val recipient = registerRecipient("Recipient", 1, 0)
            val cases =
                listOf(
                    objs.restore_potion_1,
                    objs.energy_potion_1,
                    objs.prayer_potion_1,
                    objs.super_energy_1,
                    objs.super_restore_1,
                    objs.sanfew_serum_1,
                    objs.stamina_potion_1,
                )

            for (potion in cases) {
                prepareCaster(potion)
                setLevel(player, stats.attack, base = 80, current = 20)
                setLevel(recipient, stats.attack, base = 80, current = 20)
                setLevel(player, stats.prayer, base = 99, current = 10)
                setLevel(recipient, stats.prayer, base = 99, current = 10)
                player.runEnergy = 1_234
                recipient.runEnergy = 1_234
                player.setVarp(varps.poison, 25)
                recipient.setVarp(varps.poison, 25)
                player.setVarp(varps.disease, 7)
                recipient.setVarp(varps.disease, 7)

                player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

                when (StatRestorePotionFamily.forObj(potion.id)) {
                    StatRestorePotionFamily.Restore -> {
                        assertEquals(54, player.stat(stats.attack))
                        assertEquals(54, recipient.stat(stats.attack))
                    }
                    StatRestorePotionFamily.Energy -> {
                        assertEquals(2_234, player.runEnergy)
                        assertEquals(2_234, recipient.runEnergy)
                    }
                    StatRestorePotionFamily.Prayer -> {
                        assertEquals(41, player.stat(stats.prayer))
                        assertEquals(41, recipient.stat(stats.prayer))
                    }
                    StatRestorePotionFamily.SuperEnergy -> {
                        assertEquals(3_234, player.runEnergy)
                        assertEquals(3_234, recipient.runEnergy)
                    }
                    StatRestorePotionFamily.SuperRestore -> {
                        assertEquals(48, player.stat(stats.attack))
                        assertEquals(48, recipient.stat(stats.attack))
                        assertEquals(42, player.stat(stats.prayer))
                        assertEquals(42, recipient.stat(stats.prayer))
                    }
                    StatRestorePotionFamily.SanfewSerum -> {
                        assertEquals(48, player.stat(stats.attack))
                        assertEquals(48, recipient.stat(stats.attack))
                        assertEquals(43, player.stat(stats.prayer))
                        assertEquals(43, recipient.stat(stats.prayer))
                        assertEquals(-20, player.vars[varps.poison])
                        assertEquals(-20, recipient.vars[varps.poison])
                        assertEquals(0, player.vars[varps.disease])
                        assertEquals(0, recipient.vars[varps.disease])
                        assertTrue(deps.poison.isPoisonImmune(player))
                        assertTrue(deps.poison.isPoisonImmune(recipient))
                        assertEquals(1_500, deps.disease.remainingCycles(player))
                        assertEquals(1_500, deps.disease.remainingCycles(recipient))
                    }
                    StatRestorePotionFamily.Stamina -> {
                        assertEquals(3_234, player.runEnergy)
                        assertEquals(3_234, recipient.runEnergy)
                        assertTrue(deps.stamina.isActive(player))
                        assertTrue(deps.stamina.isActive(recipient))
                    }
                    null -> error("Unexpected potion: $potion")
                }
                advance(3)
            }
        }

    @Test
    fun GameTestState.`direct restoration families apply every exact immediate effect`() =
        runPotionShareTest { deps ->
            val cases =
                listOf(
                    objs.restore_potion_4 to objs.restore_potion_3,
                    objs.energy_potion_4 to objs.energy_potion_3,
                    objs.prayer_potion_4 to objs.prayer_potion_3,
                    objs.super_energy_4 to objs.super_energy_3,
                    objs.super_restore_4 to objs.super_restore_3,
                    objs.stamina_potion_4 to objs.stamina_potion_3,
                )

            for ((potion, replacement) in cases) {
                prepareCaster(potion, casts = 0)
                deps.stamina.clear(player)
                setLevel(player, stats.attack, base = 80, current = 20)
                setLevel(player, stats.cooking, base = 80, current = 20)
                setLevel(player, stats.prayer, base = 99, current = 10)
                setLevel(player, stats.hitpoints, base = 99, current = 40)
                player.runEnergy = 1_234

                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
                }

                assertEquals(replacement.id, player.inv[POTION_SLOT]?.id)
                when (StatRestorePotionFamily.forObj(potion.id)) {
                    StatRestorePotionFamily.Restore -> {
                        assertEquals(54, player.stat(stats.attack))
                        assertEquals(20, player.stat(stats.cooking))
                        assertEquals(10, player.stat(stats.prayer))
                    }
                    StatRestorePotionFamily.Energy -> assertEquals(2_234, player.runEnergy)
                    StatRestorePotionFamily.Prayer -> assertEquals(41, player.stat(stats.prayer))
                    StatRestorePotionFamily.SuperEnergy -> assertEquals(3_234, player.runEnergy)
                    StatRestorePotionFamily.SuperRestore -> {
                        assertEquals(48, player.stat(stats.attack))
                        assertEquals(48, player.stat(stats.cooking))
                        assertEquals(42, player.stat(stats.prayer))
                        assertEquals(40, player.stat(stats.hitpoints))
                    }
                    StatRestorePotionFamily.Stamina -> {
                        assertEquals(3_234, player.runEnergy)
                        assertEquals(200, deps.stamina.remainingCycles(player))
                        assertEquals(1, player.vars[varbits.stamina_active])
                        assertEquals(20, player.vars[varbits.stamina_duration])
                    }
                    StatRestorePotionFamily.SanfewSerum,
                    null -> error("Unexpected potion: $potion")
                }
                advance(3)
            }
        }

    @Test
    fun GameTestState.`direct prayer restorers honor the holy wrench effect`() =
        runPotionShareTest { deps ->
            val cases = listOf(objs.prayer_potion_4 to 43, objs.super_restore_4 to 44)

            for ((potion, expectedPrayer) in cases) {
                prepareCaster(potion, casts = 0)
                player.inv[4] = InvObj(objs.holy_wrench)
                setLevel(player, stats.prayer, base = 99, current = 10)

                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, POTION_SLOT, HeldOp.Op1)
                }

                assertEquals(expectedPrayer, player.stat(stats.prayer))
                advance(3)
            }
        }

    @Test
    fun GameTestState
        .`prayer restoration modifiers are evaluated independently for each recipient`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.prayer_potion_4)
            setLevel(player, stats.prayer, base = 99, current = 10)
            player.inv[4] = InvObj(objs.holy_wrench)
            val cape = registerRecipient("Cape", 1, 0)
            val ring = registerRecipient("Ring", 0, 1)
            val plain = registerRecipient("Plain", -1, 0)
            for (recipient in listOf(cape, ring, plain)) {
                setLevel(recipient, stats.prayer, base = 99, current = 10)
            }
            cape.inv[0] = InvObj(objs.prayer_skillcape)
            ring.worn[12] = InvObj(objs.ring_of_the_gods_i_soul_wars)

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }

            assertEquals(43, player.stat(stats.prayer))
            assertEquals(43, cape.stat(stats.prayer))
            assertEquals(43, ring.stat(stats.prayer))
            assertEquals(41, plain.stat(stats.prayer))
            assertEquals(1, player.count(objs.prayer_potion_1))
        }

    @Test
    fun GameTestState.`stamina lasts exactly two hundred cycles and a new dose refreshes it`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.stamina_potion_2, casts = 2)
            registerRecipient("Recipient", 1, 0)

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.vars[varbits.stamina_active])
            assertEquals(20, player.vars[varbits.stamina_duration])
            advance(50)
            assertEquals(150, deps.stamina.remainingCycles(player))

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(200, deps.stamina.remainingCycles(player))
            assertEquals(20, player.vars[varbits.stamina_duration])
            advance(199)
            assertTrue(deps.stamina.isActive(player))
            advance()
            assertFalse(deps.stamina.isActive(player))
            assertEquals(0, player.vars[varbits.stamina_active])
            assertEquals(0, player.vars[varbits.stamina_duration])
        }

    @Test
    fun GameTestState.`no recipient invalid target and access failures are atomic`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.restore_potion_4)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.restore_potion_4))
            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))

            registerRecipient("Recipient", 1, 0)
            player.inv[POTION_SLOT] = InvObj(objs.attack_potion_4)
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.attack_potion_4))

            player.inv[POTION_SLOT] = InvObj(objs.prayer_regeneration_potion_4)
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.prayer_regeneration_potion_4))

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

            player.inv[POTION_SLOT] = InvObj(objs.restore_potion_4)
            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.stats[stats.magic] = 80
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[2] = null
            player.withProtectedAccess {
                deps.spell.cast(this, POTION_SLOT)
                assertMessageSent("You do not have enough Astral Runes to cast this spell.")
            }
            assertEquals(1, player.count(objs.restore_potion_4))
            assertEquals(0, player.count(objs.astral_rune))
        }

    @Test
    fun GameTestState
        .`full inventory Fountain policy cooldown and interface dispatch are supported`() =
        runPotionShareTest { deps ->
            prepareCaster(objs.restore_potion_4, casts = 0)
            repeat(player.inv.size) { player.inv[it] = InvObj(objs.logs) }
            player.inv[POTION_SLOT] = InvObj(objs.restore_potion_4)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            registerRecipient("Recipient", 1, 0)
            val magicXp = player.statMap.getFineXP(stats.magic)

            deps.events.publish(
                IfOverlayButtonT(
                    player = player,
                    selectedSlot = -1,
                    selectedObj = objTypes[objs.spell_res_pot_share],
                    targetSlot = POTION_SLOT,
                    targetObj = objTypes[objs.restore_potion_4],
                    selectedComponent =
                        Component(components.magic_spellbook_stat_restore_pot_share.packed),
                    targetComponent = Component(components.inv_items.packed),
                )
            )

            assertEquals(1, player.count(objs.restore_potion_3))
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.restore_potion_3))
            advance(2)
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.restore_potion_3))
            advance()
            player.withProtectedAccess { deps.spell.cast(this, POTION_SLOT) }
            assertEquals(1, player.count(objs.restore_potion_2))
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
            player.inv[0] = InvObj(objs.earth_rune, casts * 10)
            player.inv[1] = InvObj(objs.water_rune, casts * 10)
            player.inv[2] = InvObj(objs.astral_rune, casts * 2)
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
        assertEquals(1, player.count(objs.restore_potion_4))
        assertEquals(2, player.count(objs.astral_rune))
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
            StatRestorePotShareSpellScript::class,
            PotionProtectionScript::class,
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
            bind(StaminaEffectRepository::class.java).`in`(Scopes.SINGLETON)
            bind(PoisonProtectionRepository::class.java).`in`(Scopes.SINGLETON)
            bind(DiseaseImmunityRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val spell: StatRestorePotShareSpell,
        val stamina: StaminaEffectRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val events: EventBus,
        val poison: PoisonProtectionRepository,
        val disease: DiseaseImmunityRepository,
        val held: HeldInteractions,
    )

    private companion object {
        const val POTION_SLOT = 3
    }
}
