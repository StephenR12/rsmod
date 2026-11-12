package org.rsmod.api.combat

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.righthand
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackRegistry
import org.rsmod.api.spells.attack.SpellAttackRepository
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
import org.rsmod.api.testing.GameTestExtension
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingImpactScript
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.CrumbleUndeadSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.ElementalSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.GodSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.IbanBlastSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.IbanStaffChargeRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.MagicDartSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.StatDrainImpactScript
import org.rsmod.content.skills.magic.spell.attacks.standard.StatDrainSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.TeleBlockImpactScript
import org.rsmod.content.skills.magic.spell.attacks.standard.TeleBlockSpells
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.util.ParamMapBuilder

@ExtendWith(GameTestExtension::class)
class ElementalSpellTimingTest {
    @Test
    fun GameTestState.`standard combat spell repeats every five ticks`() =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
        ) { deps ->
            with(deps.elementalSpells) {
                with(SpellAttackRepository(deps.spellAttacks)) { register(deps.spellAttackManager) }
            }

            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 2)
            player.inv[1] = InvObj(objs.mind_rune, 2)
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_wind_strike))
            val attack = CombatAttack.Spell(weapon = null, spell = spell, defensive = false)
            val target = spawnNpc(player.coords.translate(3, 0), attackableTarget)
            val startingXp = player.statMap.getFineXP(stats.magic)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { deps.combat.attack(this, target, attack) }

            val nextAttackClock = player.actionDelay
            assertEquals(5, nextAttackClock - player.currentMapClock)
            assertEquals(1, player.count(objs.air_rune))
            assertEquals(1, player.count(objs.mind_rune))
            assertEquals(55, player.statMap.getFineXP(stats.magic) - startingXp)

            repeat(4) {
                advance()
                player.withProtectedAccess { deps.combat.attack(this, target, attack) }
                assertEquals(1, player.count(objs.air_rune))
                assertEquals(1, player.count(objs.mind_rune))
                assertEquals(55, player.statMap.getFineXP(stats.magic) - startingXp)
            }

            advance()
            random.next = 0
            random.then = 0
            player.withProtectedAccess { deps.combat.attack(this, target, attack) }
            assertEquals(0, player.count(objs.air_rune))
            assertEquals(0, player.count(objs.mind_rune))
            assertEquals(110, player.statMap.getFineXP(stats.magic) - startingXp)
        }

    @Test
    fun GameTestState.`standard stat drain spell respects five tick attack cadence`() =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            StatDrainImpactScript::class,
        ) { deps ->
            with(deps.statDrainSpells) {
                with(SpellAttackRepository(deps.spellAttacks)) { register(deps.spellAttackManager) }
            }

            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.water_rune, 6)
            player.inv[1] = InvObj(objs.earth_rune, 4)
            player.inv[2] = InvObj(objs.body_rune, 2)
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_confuse))
            val attack = CombatAttack.Spell(weapon = null, spell = spell, defensive = false)
            val firstTarget = spawnNpc(player.coords.translate(3, 0), attackableTarget)
            val secondTarget = spawnNpc(player.coords.translate(0, 3), attackableTarget)
            val startingXp = player.statMap.getFineXP(stats.magic)
            random.next = 0

            player.withProtectedAccess { deps.combat.attack(this, firstTarget, attack) }

            assertEquals(5, player.actionDelay - player.currentMapClock)
            assertEquals(3, player.count(objs.water_rune))
            assertEquals(2, player.count(objs.earth_rune))
            assertEquals(1, player.count(objs.body_rune))
            assertEquals(130, player.statMap.getFineXP(stats.magic) - startingXp)

            repeat(4) {
                advance()
                player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
                assertEquals(3, player.count(objs.water_rune))
                assertEquals(2, player.count(objs.earth_rune))
                assertEquals(1, player.count(objs.body_rune))
                assertEquals(130, player.statMap.getFineXP(stats.magic) - startingXp)
            }

            advance()
            random.next = 0
            player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
            assertEquals(0, player.count(objs.water_rune))
            assertEquals(0, player.count(objs.earth_rune))
            assertEquals(0, player.count(objs.body_rune))
            assertEquals(260, player.statMap.getFineXP(stats.magic) - startingXp)
        }

    @Test
    fun GameTestState.`standard binding spell respects five tick attack cadence`() =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            BindingImpactScript::class,
        ) { deps ->
            with(deps.bindingSpells) {
                with(SpellAttackRepository(deps.spellAttacks)) { register(deps.spellAttackManager) }
            }

            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.water_rune, 6)
            player.inv[1] = InvObj(objs.earth_rune, 6)
            player.inv[2] = InvObj(objs.nature_rune, 4)
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_bind))
            val attack = CombatAttack.Spell(weapon = null, spell = spell, defensive = false)
            val firstTarget = spawnNpc(player.coords.translate(3, 0), attackableTarget)
            val secondTarget = spawnNpc(player.coords.translate(0, 3), attackableTarget)
            val startingXp = player.statMap.getFineXP(stats.magic)
            random.next = 0

            player.withProtectedAccess { deps.combat.attack(this, firstTarget, attack) }

            assertEquals(5, player.actionDelay - player.currentMapClock)
            assertEquals(3, player.count(objs.water_rune))
            assertEquals(3, player.count(objs.earth_rune))
            assertEquals(2, player.count(objs.nature_rune))
            assertEquals(310, player.statMap.getFineXP(stats.magic) - startingXp)

            repeat(4) {
                advance()
                player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
                assertEquals(3, player.count(objs.water_rune))
                assertEquals(3, player.count(objs.earth_rune))
                assertEquals(2, player.count(objs.nature_rune))
                assertEquals(310, player.statMap.getFineXP(stats.magic) - startingXp)
            }

            advance()
            random.next = 0
            player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
            assertEquals(0, player.count(objs.water_rune))
            assertEquals(0, player.count(objs.earth_rune))
            assertEquals(0, player.count(objs.nature_rune))
            assertEquals(620, player.statMap.getFineXP(stats.magic) - startingXp)
        }

    @Test
    fun GameTestState.`tele block respects five tick attack cadence`() =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            WeaponAttackStylesScript::class,
            TeleBlockImpactScript::class,
        ) { deps ->
            with(deps.teleBlockSpells) {
                with(SpellAttackRepository(deps.spellAttacks)) { register(deps.spellAttackManager) }
            }

            player.stats[stats.magic] = 99
            player.setVarBit(varbits.inside_wilderness, 1)
            player.clearInv()
            player.inv[0] = InvObj(objs.law_rune, 2)
            player.inv[1] = InvObj(objs.chaos_rune, 2)
            player.inv[2] = InvObj(objs.death_rune, 2)
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_tele_block))
            val attack = CombatAttack.Spell(weapon = null, spell = spell, defensive = false)
            val firstTarget = registerPlayer(player.coords.translate(3, 0))
            val secondTarget = registerPlayer(player.coords.translate(0, 3))
            firstTarget.setVarBit(varbits.inside_wilderness, 1)
            secondTarget.setVarBit(varbits.inside_wilderness, 1)
            val startingXp = player.statMap.getFineXP(stats.magic)
            random.next = 0

            player.withProtectedAccess { deps.playerCombat.attack(this, firstTarget, attack) }

            assertEquals(5, player.actionDelay - player.currentMapClock)
            assertEquals(1, player.count(objs.law_rune))
            assertEquals(1, player.count(objs.chaos_rune))
            assertEquals(1, player.count(objs.death_rune))
            assertEquals(950, player.statMap.getFineXP(stats.magic) - startingXp)

            repeat(4) {
                advance()
                player.withProtectedAccess { deps.playerCombat.attack(this, secondTarget, attack) }
                assertEquals(1, player.count(objs.law_rune))
                assertEquals(1, player.count(objs.chaos_rune))
                assertEquals(1, player.count(objs.death_rune))
                assertEquals(950, player.statMap.getFineXP(stats.magic) - startingXp)
            }

            advance()
            random.next = 0
            player.withProtectedAccess { deps.playerCombat.attack(this, secondTarget, attack) }
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(0, player.count(objs.chaos_rune))
            assertEquals(0, player.count(objs.death_rune))
            assertEquals(1900, player.statMap.getFineXP(stats.magic) - startingXp)
        }

    @Test
    fun GameTestState.`crumble undead respects five tick attack cadence`() =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
        ) { deps ->
            with(deps.crumbleUndeadSpells) {
                with(SpellAttackRepository(deps.spellAttacks)) { register(deps.spellAttackManager) }
            }

            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 4)
            player.inv[1] = InvObj(objs.earth_rune, 4)
            player.inv[2] = InvObj(objs.chaos_rune, 2)
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_crumble_undead))
            val attack = CombatAttack.Spell(weapon = null, spell = spell, defensive = false)
            val firstTarget = spawnNpc(player.coords.translate(3, 0), undeadTarget)
            val secondTarget = spawnNpc(player.coords.translate(0, 3), undeadTarget)
            val startingXp = player.statMap.getFineXP(stats.magic)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { deps.combat.attack(this, firstTarget, attack) }

            assertEquals(5, player.actionDelay - player.currentMapClock)
            assertEquals(2, player.count(objs.air_rune))
            assertEquals(2, player.count(objs.earth_rune))
            assertEquals(1, player.count(objs.chaos_rune))
            assertEquals(245, player.statMap.getFineXP(stats.magic) - startingXp)

            repeat(4) {
                advance()
                player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
                assertEquals(2, player.count(objs.air_rune))
                assertEquals(2, player.count(objs.earth_rune))
                assertEquals(1, player.count(objs.chaos_rune))
                assertEquals(245, player.statMap.getFineXP(stats.magic) - startingXp)
            }

            advance()
            random.next = 0
            random.then = 0
            player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
            assertEquals(0, player.count(objs.air_rune))
            assertEquals(0, player.count(objs.earth_rune))
            assertEquals(0, player.count(objs.chaos_rune))
            assertEquals(490, player.statMap.getFineXP(stats.magic) - startingXp)
        }

    @Test
    fun GameTestState.`magic dart respects five tick attack cadence`() =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
        ) { deps ->
            with(deps.magicDartSpells) {
                with(SpellAttackRepository(deps.spellAttacks)) { register(deps.spellAttackManager) }
            }

            player.stats[stats.magic] = 99
            player.stats[stats.slayer] = 55
            player.clearInv()
            player.inv[0] = InvObj(objs.mind_rune, 8)
            player.inv[1] = InvObj(objs.death_rune, 2)
            player.righthand = InvObj(objs.slayers_staff)
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_magic_dart))
            val attack =
                CombatAttack.Spell(weapon = player.righthand, spell = spell, defensive = false)
            val firstTarget = spawnNpc(player.coords.translate(3, 0), attackableTarget)
            val secondTarget = spawnNpc(player.coords.translate(0, 3), attackableTarget)
            val startingXp = player.statMap.getFineXP(stats.magic)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { deps.combat.attack(this, firstTarget, attack) }

            assertEquals(5, player.actionDelay - player.currentMapClock)
            assertEquals(4, player.count(objs.mind_rune))
            assertEquals(1, player.count(objs.death_rune))
            assertEquals(300, player.statMap.getFineXP(stats.magic) - startingXp)

            repeat(4) {
                advance()
                player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
                assertEquals(4, player.count(objs.mind_rune))
                assertEquals(1, player.count(objs.death_rune))
                assertEquals(300, player.statMap.getFineXP(stats.magic) - startingXp)
            }

            advance()
            random.next = 0
            random.then = 0
            player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
            assertEquals(0, player.count(objs.mind_rune))
            assertEquals(0, player.count(objs.death_rune))
            assertEquals(600, player.statMap.getFineXP(stats.magic) - startingXp)
        }

    @Test
    fun GameTestState.`iban blast respects five tick attack cadence`() =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
        ) { deps ->
            with(deps.ibanBlastSpells) {
                with(SpellAttackRepository(deps.spellAttacks)) { register(deps.spellAttackManager) }
            }

            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.fire_rune, 10)
            player.inv[1] = InvObj(objs.death_rune, 2)
            player.righthand = InvObj(objs.ibans_staff)
            deps.ibanCharges.set(player, 2)
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_iban_blast))
            val attack =
                CombatAttack.Spell(weapon = player.righthand, spell = spell, defensive = false)
            val firstTarget = spawnNpc(player.coords.translate(3, 0), attackableTarget)
            val secondTarget = spawnNpc(player.coords.translate(0, 3), attackableTarget)
            val startingXp = player.statMap.getFineXP(stats.magic)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { deps.combat.attack(this, firstTarget, attack) }

            assertEquals(5, player.actionDelay - player.currentMapClock)
            assertEquals(5, player.count(objs.fire_rune))
            assertEquals(1, player.count(objs.death_rune))
            assertEquals(1, deps.ibanCharges.get(player))
            assertEquals(300, player.statMap.getFineXP(stats.magic) - startingXp)

            repeat(4) {
                advance()
                player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
                assertEquals(5, player.count(objs.fire_rune))
                assertEquals(1, player.count(objs.death_rune))
                assertEquals(1, deps.ibanCharges.get(player))
                assertEquals(300, player.statMap.getFineXP(stats.magic) - startingXp)
            }

            advance()
            random.next = 0
            random.then = 0
            player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(0, player.count(objs.death_rune))
            assertEquals(0, deps.ibanCharges.get(player))
            assertEquals(600, player.statMap.getFineXP(stats.magic) - startingXp)
        }

    @Test
    fun GameTestState.`god spell respects five tick attack cadence`() =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
        ) { deps ->
            with(deps.godSpells) {
                with(SpellAttackRepository(deps.spellAttacks)) { register(deps.spellAttackManager) }
            }

            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 8)
            player.inv[1] = InvObj(objs.fire_rune, 4)
            player.inv[2] = InvObj(objs.blood_rune, 4)
            player.righthand = InvObj(objs.saradomin_staff)
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_saradomin_strike))
            val attack =
                CombatAttack.Spell(weapon = player.righthand, spell = spell, defensive = false)
            val firstTarget = spawnNpc(player.coords.translate(3, 0), attackableTarget)
            val secondTarget = spawnNpc(player.coords.translate(0, 3), attackableTarget)
            val startingXp = player.statMap.getFineXP(stats.magic)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { deps.combat.attack(this, firstTarget, attack) }

            assertEquals(5, player.actionDelay - player.currentMapClock)
            assertEquals(4, player.count(objs.air_rune))
            assertEquals(2, player.count(objs.fire_rune))
            assertEquals(2, player.count(objs.blood_rune))
            assertEquals(350, player.statMap.getFineXP(stats.magic) - startingXp)

            repeat(4) {
                advance()
                player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
                assertEquals(4, player.count(objs.air_rune))
                assertEquals(2, player.count(objs.fire_rune))
                assertEquals(2, player.count(objs.blood_rune))
                assertEquals(350, player.statMap.getFineXP(stats.magic) - startingXp)
            }

            advance()
            random.next = 0
            random.then = 0
            player.withProtectedAccess { deps.combat.attack(this, secondTarget, attack) }
            assertEquals(0, player.count(objs.air_rune))
            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(0, player.count(objs.blood_rune))
            assertEquals(700, player.statMap.getFineXP(stats.magic) - startingXp)
        }

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            bind(SpellAttackRegistry::class.java).`in`(Scopes.SINGLETON)
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        }
    }

    internal data class Dependencies
    @Inject
    constructor(
        val combat: PvNCombat,
        val playerCombat: PvPCombat,
        val bindingSpells: BindingSpells,
        val crumbleUndeadSpells: CrumbleUndeadSpells,
        val elementalSpells: ElementalSpells,
        val godSpells: GodSpells,
        val ibanBlastSpells: IbanBlastSpells,
        val ibanCharges: IbanStaffChargeRepository,
        val magicDartSpells: MagicDartSpells,
        val statDrainSpells: StatDrainSpells,
        val teleBlockSpells: TeleBlockSpells,
        val spellAttackManager: SpellAttackManager,
        val spellAttacks: SpellAttackRegistry,
        val magicSpells: MagicSpellRegistry,
    )

    private companion object {
        val attackableTarget =
            npcTypeFactory.create {
                name = "Attackable magic test target"
                op[1] = "Attack"
                hitpoints = 100
                defence = 1
                magic = 1
            }

        val undeadTarget =
            npcTypeFactory.create {
                name = "Undead magic test target"
                op[1] = "Attack"
                hitpoints = 100
                defence = 1
                magic = 1
                paramMap = ParamMapBuilder().apply { this[params.undead] = 1 }.toParamMap()
            }
    }
}
