package org.rsmod.content.skills.magic.spell.attacks.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.lefthand
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackRegistry
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.api.spells.attack.attack
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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType

class StatDrainSpellAttackTest {
    @Test
    fun GameTestState.`cache contract defines all six standard stat drains`() =
        runStatDrainTest { deps ->
            val contracts =
                listOf(
                    contract(
                        objs.spell_confuse,
                        stats.attack,
                        5,
                        3,
                        13.0,
                        objs.water_rune to 3,
                        objs.earth_rune to 2,
                        objs.body_rune to 1,
                    ),
                    contract(
                        objs.spell_weaken,
                        stats.strength,
                        5,
                        11,
                        21.0,
                        objs.water_rune to 3,
                        objs.earth_rune to 2,
                        objs.body_rune to 1,
                    ),
                    contract(
                        objs.spell_curse,
                        stats.defence,
                        5,
                        19,
                        29.0,
                        objs.water_rune to 2,
                        objs.earth_rune to 3,
                        objs.body_rune to 1,
                    ),
                    contract(
                        objs.spell_vulnerability,
                        stats.defence,
                        10,
                        66,
                        76.0,
                        objs.water_rune to 5,
                        objs.earth_rune to 5,
                        objs.soul_rune to 1,
                    ),
                    contract(
                        objs.spell_enfeeble,
                        stats.strength,
                        10,
                        73,
                        83.0,
                        objs.water_rune to 8,
                        objs.earth_rune to 8,
                        objs.soul_rune to 1,
                    ),
                    contract(
                        objs.spell_stun,
                        stats.attack,
                        10,
                        80,
                        90.0,
                        objs.water_rune to 12,
                        objs.earth_rune to 12,
                        objs.soul_rune to 1,
                    ),
                )

            val registry = deps.registry()
            for (contract in contracts) {
                val type = objTypes[contract.spell]
                val magicSpell = requireNotNull(deps.magicSpells.getObjSpell(contract.spell))
                assertEquals(
                    contract.stat,
                    type.param(params.spell_drain_stat),
                    contract.spell.toString(),
                )
                assertEquals(
                    contract.percent,
                    type.param(params.spell_drain_stat_amount),
                    contract.spell.toString(),
                )
                assertEquals(contract.level, magicSpell.levelReq, contract.spell.toString())
                assertEquals(contract.castXp, magicSpell.castXp, contract.spell.toString())
                assertEquals(Spellbook.Standard, magicSpell.spellbook, contract.spell.toString())
                assertEquals(MagicSpellType.Combat, magicSpell.type, contract.spell.toString())
                assertEquals(
                    contract.runes,
                    magicSpell.objReqs.associate { requirement ->
                        requirement.obj to requirement.count
                    },
                    contract.spell.toString(),
                )
                assertNotNull(registry[contract.spell], contract.spell.toString())
            }
        }

    @Test
    fun GameTestState.`successful confuse drains npc attack only when projectile lands`() =
        runStatDrainTest { deps ->
            player.stats[stats.magic] = 99
            giveCurseRunes()
            val startingMagicXp = player.statMap.getFineXP(stats.magic)
            val startingHitpointsXp = player.statMap.getFineXP(stats.hitpoints)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(objs.spell_confuse)
            random.next = 0

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                assertEquals(100, target.attackLvl)
                assertEquals(130, player.statMap.getFineXP(stats.magic) - startingMagicXp)
                assertEquals(startingHitpointsXp, player.statMap.getFineXP(stats.hitpoints))
                assertEquals(seqs.human_castconfuse.id, player.pendingSequence.id)
                assertEquals(
                    spotanims.confuse_casting.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                assertEquals(
                    spotanims.confuse_impact.id,
                    EntitySpotanim(target.pendingSpotanims.getLong(0)).id,
                )
            }

            advanceUntil(predicate = { target.attackLvl == 95 })
            assertEquals(100, target.strengthLvl)
            assertEquals(100, target.defenceLvl)
        }

    @Test
    fun GameTestState.`splash consumes runes and awards cast xp without applying drain`() =
        runStatDrainTest { deps ->
            player.stats[stats.magic] = 99
            giveCurseRunes()
            val startingMagicXp = player.statMap.getFineXP(stats.magic)
            val startingHitpointsXp = player.statMap.getFineXP(stats.hitpoints)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(objs.spell_weaken)
            random.next = 9_999

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                assertEquals(210, player.statMap.getFineXP(stats.magic) - startingMagicXp)
                assertEquals(startingHitpointsXp, player.statMap.getFineXP(stats.hitpoints))
                assertEquals(
                    spotanims.failedspell_impact.id,
                    EntitySpotanim(target.pendingSpotanims.getLong(0)).id,
                )
            }

            advance(10)
            assertEquals(100, target.strengthLvl)
        }

    @Test
    fun GameTestState.`already lowered stat rejects before consuming runes or awarding xp`() =
        runStatDrainTest { deps ->
            player.stats[stats.magic] = 99
            giveCurseRunes()
            val startingWater = player.count(objs.water_rune)
            val startingEarth = player.count(objs.earth_rune)
            val startingBody = player.count(objs.body_rune)
            val startingSoul = player.count(objs.soul_rune)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            target.attackLvl = 99
            val (handler, attack) = deps.attack(objs.spell_stun)

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                assertEquals(startingWater, player.count(objs.water_rune))
                assertEquals(startingEarth, player.count(objs.earth_rune))
                assertEquals(startingBody, player.count(objs.body_rune))
                assertEquals(startingSoul, player.count(objs.soul_rune))
                assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
                assertTrue(player.pendingSpotanims.isEmpty())
                assertMessageSent("Your foe's attack has already been weakened.")
            }
        }

    @Test
    fun GameTestState.`boosted stat can be cursed until its first drain below base`() =
        runStatDrainTest { deps ->
            player.stats[stats.magic] = 99
            giveCurseRunes()
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            target.strengthLvl = 110
            target.regenClock = 10_000
            val (handler, attack) = deps.attack(objs.spell_weaken)

            for (expected in listOf(105, 100, 95)) {
                random.next = 0
                player.withProtectedAccess { handler.attack(this, target, attack) }
                advanceUntil(predicate = { target.strengthLvl == expected })
            }

            val startingWater = player.count(objs.water_rune)
            val startingEarth = player.count(objs.earth_rune)
            val startingBody = player.count(objs.body_rune)
            val startingXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { handler.attack(this, target, attack) }

            assertEquals(startingWater, player.count(objs.water_rune))
            assertEquals(startingEarth, player.count(objs.earth_rune))
            assertEquals(startingBody, player.count(objs.body_rune))
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
            assertMessageSent("Your foe's strength has already been weakened.")
        }

    @Test
    fun GameTestState.`tome of water increases five-percent curse drain to seven percent`() =
        runStatDrainTest { deps ->
            player.stats[stats.magic] = 99
            giveCurseRunes()
            player.lefthand = InvObj(objs.tome_of_water, vars = 2)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(objs.spell_curse)
            random.next = 0

            player.withProtectedAccess { handler.attack(this, target, attack) }

            advanceUntil(predicate = { target.defenceLvl != 100 })
            assertEquals(93, target.defenceLvl)
            assertEquals(1, player.lefthand?.vars)
        }

    @Test
    fun GameTestState.`successful curse drains a player target on impact`() =
        runStatDrainTest { deps ->
            player.stats[stats.magic] = 99
            giveCurseRunes()
            val target = registerPlayer(player.coords.translate(3, 0))
            target.setBaseLevel(stats.strength, 100)
            target.setCurrentLevel(stats.strength, 100)
            val (handler, attack) = deps.attack(objs.spell_enfeeble)
            random.next = 0

            player.withProtectedAccess {
                handler.attack(this, target, attack)
                assertEquals(100, target.stats[stats.strength])
            }

            advanceUntil(predicate = { target.stats[stats.strength] == 90 })
        }

    @Test
    fun GameTestState.`vulnerability uses curse motion and its own cache effects`() =
        runStatDrainTest { deps ->
            player.stats[stats.magic] = 99
            giveCurseRunes()
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(objs.spell_vulnerability)
            random.next = 0

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                assertEquals(seqs.human_castcurse.id, player.pendingSequence.id)
                assertEquals(
                    spotanims.vulnerability_casting.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                assertEquals(
                    spotanims.vulnerability_impact.id,
                    EntitySpotanim(target.pendingSpotanims.getLong(0)).id,
                )
            }
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.giveCurseRunes() {
        player.clearInv()
        player.inv[0] = InvObj(objs.water_rune, 100)
        player.inv[1] = InvObj(objs.earth_rune, 100)
        player.inv[2] = InvObj(objs.body_rune, 100)
        player.inv[3] = InvObj(objs.soul_rune, 100)
    }

    private fun GameTestState.runStatDrainTest(
        testBody: org.rsmod.api.testing.scope.GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            StatDrainTestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            WeaponAttackStylesScript::class,
            StatDrainImpactScript::class,
            testBody = testBody,
        )

    private object StatDrainTestModule : AbstractModule() {
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
        val stat: StatType,
        val percent: Int,
        val level: Int,
        val castXp: Double,
        val runes: Map<ObjType, Int>,
    )

    private fun contract(
        spell: ObjType,
        stat: StatType,
        percent: Int,
        level: Int,
        castXp: Double,
        vararg runes: Pair<ObjType, Int>,
    ): Contract = Contract(spell, stat, percent, level, castXp, mapOf(*runes))

    data class AttackFixture(val handler: SpellAttack, val attack: CombatAttack.Spell)

    data class Dependencies
    @Inject
    constructor(
        val spells: StatDrainSpells,
        val manager: SpellAttackManager,
        val magicSpells: MagicSpellRegistry,
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
                CombatAttack.Spell(weapon = null, spell = magicSpell, defensive = false),
            )
        }
    }

    private companion object {
        val targetType =
            npcTypeFactory.create {
                name = "Stat-drain test target"
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 100
                magic = 1
            }
    }
}
