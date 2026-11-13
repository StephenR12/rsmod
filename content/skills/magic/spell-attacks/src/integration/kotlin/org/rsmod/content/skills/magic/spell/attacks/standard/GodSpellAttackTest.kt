package org.rsmod.content.skills.magic.spell.attacks.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MageArenaTraining
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.manager.MagicRuneManager.CastResult
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.back
import org.rsmod.api.player.righthand
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackRegistry
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.api.spells.attack.attack
import org.rsmod.api.spells.autocast.AutocastWeapons
import org.rsmod.api.spells.autocast.scripts.AutocastScript
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
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

class GodSpellAttackTest {
    @Test
    fun GameTestState.`cache contract defines charge metadata`() = runGodSpellTest { deps ->
        val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_charge))
        assertEquals(80, spell.levelReq)
        assertEquals(180.0, spell.castXp)
        assertEquals(Spellbook.Standard, spell.spellbook)
        assertEquals(MagicSpellType.Combat, spell.type)
        assertEquals(
            mapOf(objs.fire_rune to 3, objs.blood_rune to 3, objs.air_rune to 3),
            spell.objReqs.associate { it.obj to it.count },
        )
    }

    @Test
    fun GameTestState.`charge cast consumes runes and drives active and cooldown state`() =
        runGodSpellTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 6)
            player.inv[1] = InvObj(objs.fire_rune, 6)
            player.inv[2] = InvObj(objs.blood_rune, 6)
            player.setVarp(varps.magearena, 8)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.charge.cast(this) }

            assertEquals(3, player.count(objs.air_rune))
            assertEquals(3, player.count(objs.fire_rune))
            assertEquals(3, player.count(objs.blood_rune))
            assertEquals(1_800, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.human_casting.id, player.pendingSequence.id)
            assertEquals(ChargeSpell.CHARGE_VARP_START, player.vars[varps.magearena_charge])
            assertEquals(
                1,
                player.vars[org.rsmod.api.config.refs.varbits.buff_charge_spell_disabled],
            )

            player.withProtectedAccess {
                deps.charge.cast(this)
                assertMessageSent("You can only cast this spell once every 60 seconds.")
            }
            assertEquals(3, player.count(objs.air_rune))
            assertEquals(1_800, player.statMap.getFineXP(stats.magic) - startXp)

            advance(2)
            assertEquals(349, player.vars[varps.magearena_charge])
            advance(98)
            assertEquals(
                0,
                player.vars[org.rsmod.api.config.refs.varbits.buff_charge_spell_disabled],
            )

            player.setVarp(varps.magearena_charge, 2)
            advanceUntil(predicate = { player.vars[varps.magearena_charge] == 0 })
        }

    @Test
    fun GameTestState.`charge requires a previously claimed god staff`() = runGodSpellTest { deps ->
        player.stats[stats.magic] = 99
        player.clearInv()
        player.inv[0] = InvObj(objs.air_rune, 3)
        player.inv[1] = InvObj(objs.fire_rune, 3)
        player.inv[2] = InvObj(objs.blood_rune, 3)
        val startXp = player.statMap.getFineXP(stats.magic)

        player.withProtectedAccess {
            deps.charge.cast(this)
            assertMessageSent(
                "You need to have claimed a god staff before you can cast this spell."
            )
        }

        assertRunes(3, 3, 3)
        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        assertEquals(0, player.vars[varps.magearena_charge])
    }

    @Test
    fun GameTestState.`cache contract defines all three god spells and presentation`() =
        runGodSpellTest { deps ->
            val contracts =
                listOf(
                    Contract(
                        objs.spell_saradomin_strike,
                        objs.saradomin_staff,
                        spotanims.saradomin_lightning,
                        647,
                        mapOf(objs.air_rune to 4, objs.fire_rune to 2, objs.blood_rune to 2),
                    ),
                    Contract(
                        objs.spell_claws_of_guthix,
                        objs.guthix_staff,
                        spotanims.guthix_claw,
                        648,
                        mapOf(objs.air_rune to 4, objs.fire_rune to 1, objs.blood_rune to 2),
                    ),
                    Contract(
                        objs.spell_flames_of_zamorak,
                        objs.zamorak_staff,
                        spotanims.zamorak_flame,
                        901,
                        mapOf(objs.air_rune to 1, objs.fire_rune to 4, objs.blood_rune to 2),
                    ),
                )

            for (contract in contracts) {
                val spell = requireNotNull(deps.magicSpells.getObjSpell(contract.spell))
                assertEquals(60, spell.levelReq)
                assertEquals(35.0, spell.castXp)
                assertEquals(1, spell.maxHit)
                assertEquals(Spellbook.Standard, spell.spellbook)
                assertEquals(MagicSpellType.Combat, spell.type)
                assertEquals(
                    contract.runes + (contract.staff to 1),
                    spell.objReqs.associate { it.obj to it.count },
                )
                assertNotNull(deps.registry()[contract.spell])
                assertEquals(contract.spotSequence, cacheTypes.spotanims[contract.spot.id]?.anim)
            }
            assertEquals(811, seqs.human_casting.id)
            assertEquals(76, spotanims.saradomin_lightning.id)
            assertEquals(77, spotanims.guthix_claw.id)
            assertEquals(78, spotanims.zamorak_flame.id)
        }

    @Test
    fun GameTestState.`missing god staff rejects cast atomically`() = runGodSpellTest { deps ->
        prepareCaster(objs.air_staff)
        val startXp = player.statMap.getFineXP(stats.magic)
        val target = spawnNpc(player.coords.translate(3, 0), targetType)
        val (handler, attack) = deps.attack(objs.spell_saradomin_strike)

        player.withProtectedAccess {
            handler.attack(this, target, attack)
            assertMessageSent(
                "You must be wielding the Staff of Saradomin or the Staff of Light to cast this spell."
            )
        }

        assertRunes(20, 20, 20)
        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        assertTrue(player.pendingSpotanims.isEmpty())
    }

    @Test
    fun GameTestState
        .`staff of light conserves one in seven costs after validating sufficient runes`() =
        runGodSpellTest { deps ->
            prepareCaster(objs.staff_of_light)
            val target = spawnNpc(player.coords.translate(2, 0), targetType)
            val (handler, attack) = deps.attack(objs.spell_saradomin_strike)
            player.inv[2] = null
            random.next = 0
            player.withProtectedAccess { handler.attack(this, target, attack) }
            assertEquals(20, player.count(objs.air_rune))
            player.inv[2] = InvObj(objs.blood_rune, 20)
            random.next = 0
            player.withProtectedAccess { handler.attack(this, target, attack) }
            assertRunes(20, 20, 20)
            random.next = 1
            player.withProtectedAccess { handler.attack(this, target, attack) }
            assertRunes(16, 18, 18)
        }

    @Test
    fun GameTestState.`last paid practice splash unlocks just that god spell for use outside`() =
        runGodSpellTest { deps ->
            prepareCaster(objs.saradomin_staff)
            player.setVarp(MageArenaTraining.saradomin, 99)
            player.setVarp(MageArenaTraining.guthix, 7)
            val (handler, attack) = deps.attack(objs.spell_saradomin_strike)
            val outside = spawnNpc(player.coords.translate(2, 0), targetType)
            player.withProtectedAccess { handler.attack(this, outside, attack) }
            assertRunes(20, 20, 20)
            val arena = CoordGrid(3098, 3925)
            player.placeAt(arena)
            val mage = spawnNpc(arena.translate(2, 0), npcTypes.getValue(1611))
            player.inv[2] = null
            player.withProtectedAccess { handler.attack(this, mage, attack) }
            assertEquals(99, player.vars[MageArenaTraining.saradomin])
            player.inv[2] = InvObj(objs.blood_rune, 20)
            random.next = 9999
            player.withProtectedAccess { handler.attack(this, mage, attack) }
            assertEquals(100, player.vars[MageArenaTraining.saradomin])
            assertEquals(7, player.vars[MageArenaTraining.guthix])
            assertRunes(16, 18, 18)
            player.placeAt(outside.coords.translate(2, 0))
            player.withProtectedAccess { handler.attack(this, outside, attack) }
            assertRunes(12, 16, 16)
        }

    @Test
    fun GameTestState.`saradomin strike hit drains one player prayer`() = runGodSpellTest { deps ->
        prepareCaster(objs.saradomin_staff)
        val opponent: Player = registerPlayer(player.coords.translate(3, 0))
        opponent.setBaseLevel(stats.prayer, 70)
        opponent.setCurrentLevel(stats.prayer, 70)
        val (handler, attack) = deps.attack(objs.spell_saradomin_strike)
        random.next = 0
        random.then = 0

        player.withProtectedAccess { handler.attack(this, opponent, attack) }

        assertEquals(seqs.human_casting.id, player.pendingSequence.id)
        assertEquals(
            spotanims.saradomin_lightning.id,
            EntitySpotanim(opponent.pendingSpotanims.getLong(0)).id,
        )
        assertEquals(70, opponent.stats[stats.prayer])
        advanceUntil(predicate = { opponent.stats[stats.prayer] == 69 })
    }

    @Test
    fun GameTestState.`claws drains defence by one plus five percent without stacking`() =
        runGodSpellTest { deps ->
            prepareCaster(objs.guthix_staff, casts = 2)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            target.regenClock = 10_000
            val (handler, attack) = deps.attack(objs.spell_claws_of_guthix)

            repeat(2) { castIndex ->
                random.next = 0
                random.then = 0
                player.withProtectedAccess { handler.attack(this, target, attack) }
                if (castIndex == 0) {
                    assertEquals(
                        spotanims.guthix_claw.id,
                        EntitySpotanim(target.pendingSpotanims.getLong(0)).id,
                    )
                }
                advance(5)
            }

            assertEquals(94, target.defenceLvl)
        }

    @Test
    fun GameTestState.`flames drains magic by one plus five percent without stacking`() =
        runGodSpellTest { deps ->
            prepareCaster(objs.zamorak_staff, casts = 2)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            target.regenClock = 10_000
            val (handler, attack) = deps.attack(objs.spell_flames_of_zamorak)

            repeat(2) { castIndex ->
                random.next = 0
                random.then = 0
                player.withProtectedAccess { handler.attack(this, target, attack) }
                if (castIndex == 0) {
                    assertEquals(
                        spotanims.zamorak_flame.id,
                        EntitySpotanim(target.pendingSpotanims.getLong(0)).id,
                    )
                }
                advance(5)
            }

            assertEquals(94, target.magicLvl)
        }

    @Test
    fun GameTestState.`splash consumes runes and does not apply god effect`() =
        runGodSpellTest { deps ->
            prepareCaster(objs.guthix_staff)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val startXp = player.statMap.getFineXP(stats.magic)
            val (handler, attack) = deps.attack(objs.spell_claws_of_guthix)
            random.next = 9_999

            player.withProtectedAccess { handler.attack(this, target, attack) }
            assertEquals(
                spotanims.failedspell_impact.id,
                EntitySpotanim(target.pendingSpotanims.getLong(0)).id,
            )
            advance(3)

            assertRunes(16, 19, 18)
            assertEquals(350, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(100, target.defenceLvl)
        }

    @Test
    fun GameTestState.`charge damage requires active state and matching cape`() =
        runGodSpellTest { deps ->
            prepareCaster(objs.saradomin_staff)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val attack = deps.attack(objs.spell_saradomin_strike).attack
            val cast = CastResult.Success.Consumed(usedSunfire = false)

            player.withProtectedAccess {
                assertEquals(0..20, deps.manager.calculateMaxHit(this, target, attack, cast, 20))
                player.setVarp(varps.magearena_charge, 350)
                player.back = InvObj(objs.guthix_cape)
                assertEquals(0..20, deps.manager.calculateMaxHit(this, target, attack, cast, 20))
                player.back = InvObj(objs.saradomin_cape)
                assertEquals(0..30, deps.manager.calculateMaxHit(this, target, attack, cast, 20))
                player.back = InvObj(objs.imbued_saradomin_max_cape_trouver)
                assertEquals(0..30, deps.manager.calculateMaxHit(this, target, attack, cast, 20))
                player.setVarp(varps.magearena_charge, 0)
                assertEquals(0..20, deps.manager.calculateMaxHit(this, target, attack, cast, 20))
            }
        }

    @Test
    fun GameTestState.`god spell autocast follows staff restrictions`() = runGodSpellTest { deps ->
        val saradomin = deps.autocastId(objs.spell_saradomin_strike)
        val guthix = deps.autocastId(objs.spell_claws_of_guthix)
        val zamorak = deps.autocastId(objs.spell_flames_of_zamorak)

        assertFalse(
            deps.autocastWeapons.canStaffAutocast(objTypes[objs.saradomin_staff], saradomin)
        )
        assertTrue(deps.autocastWeapons.canStaffAutocast(objTypes[objs.staff_of_light], saradomin))
        assertFalse(deps.autocastWeapons.canStaffAutocast(objTypes[objs.guthix_staff], guthix))
        assertTrue(deps.autocastWeapons.canStaffAutocast(objTypes[objs.void_knight_mace], guthix))
        assertFalse(deps.autocastWeapons.canStaffAutocast(objTypes[objs.zamorak_staff], zamorak))
        assertTrue(deps.autocastWeapons.canStaffAutocast(objTypes[objs.staff_of_the_dead], zamorak))
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.prepareCaster(
        staff: ObjType,
        casts: Int = 1,
    ) {
        player.stats[stats.magic] = 99
        player.setVarp(varps.magearena, 8)
        for (counter in
            listOf(
                MageArenaTraining.saradomin,
                MageArenaTraining.guthix,
                MageArenaTraining.zamorak,
            )) player.setVarp(counter, 100)
        player.clearInv()
        player.inv[0] = InvObj(objs.air_rune, 20 * casts)
        player.inv[1] = InvObj(objs.fire_rune, 20 * casts)
        player.inv[2] = InvObj(objs.blood_rune, 20 * casts)
        player.righthand = InvObj(staff)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.assertRunes(
        air: Int,
        fire: Int,
        blood: Int,
    ) {
        assertEquals(air, player.count(objs.air_rune))
        assertEquals(fire, player.count(objs.fire_rune))
        assertEquals(blood, player.count(objs.blood_rune))
    }

    private fun GameTestState.runGodSpellTest(
        testBody: org.rsmod.api.testing.scope.GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            AutocastScript::class,
            WeaponAttackStylesScript::class,
            StatDrainImpactScript::class,
            ChargeSpellScript::class,
            testBody = testBody,
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
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        }
    }

    private data class Contract(
        val spell: ObjType,
        val staff: ObjType,
        val spot: org.rsmod.game.type.spot.SpotanimType,
        val spotSequence: Int,
        val runes: Map<ObjType, Int>,
    )

    data class AttackFixture(val handler: SpellAttack, val attack: CombatAttack.Spell)

    data class Dependencies
    @Inject
    constructor(
        val spells: GodSpells,
        val charge: ChargeSpell,
        val manager: SpellAttackManager,
        val magicSpells: MagicSpellRegistry,
        val autocastWeapons: AutocastWeapons,
    ) {
        fun registry(): SpellAttackRegistry {
            val registry = SpellAttackRegistry()
            val repository = SpellAttackRepository(registry)
            with(spells) { with(repository) { register(manager) } }
            return registry
        }

        fun attack(spell: ObjType): AttackFixture {
            val magicSpell = requireNotNull(magicSpells.getObjSpell(spell))
            val handler = requireNotNull(registry()[spell])
            return AttackFixture(
                handler,
                CombatAttack.Spell(playerWeapon(spell), magicSpell, defensive = false),
            )
        }

        fun autocastId(spell: ObjType): Int =
            (0..100).single { magicSpells.getAutocastSpell(it)?.obj == spell }

        private fun playerWeapon(spell: ObjType): InvObj =
            when (spell) {
                objs.spell_saradomin_strike -> InvObj(objs.saradomin_staff)
                objs.spell_claws_of_guthix -> InvObj(objs.guthix_staff)
                objs.spell_flames_of_zamorak -> InvObj(objs.zamorak_staff)
                else -> error("Unsupported spell: $spell")
            }
    }

    private companion object {
        val targetType =
            npcTypeFactory.create {
                name = "God spell target"
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 100
                magic = 100
            }
    }
}
