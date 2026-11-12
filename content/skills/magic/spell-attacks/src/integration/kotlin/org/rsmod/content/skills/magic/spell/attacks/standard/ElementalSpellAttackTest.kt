package org.rsmod.content.skills.magic.spell.attacks.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.zone.payload.MapProjAnimV2
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.righthand
import org.rsmod.api.registry.zone.ZoneUpdateMap
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
import org.rsmod.game.interact.InteractionNpc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.util.ParamMapBuilder
import org.rsmod.map.zone.ZoneKey

class ElementalSpellAttackTest {
    @Test
    fun GameTestState.`reject insufficient magic level without consuming runes`() =
        runElementalTest { deps ->
            player.stats[stats.magic] = 94
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 7)
            player.inv[1] = InvObj(objs.fire_rune, 10)
            player.inv[2] = InvObj(objs.wrath_rune, 1)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.elementalAttack(objs.spell_fire_surge)

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                assertEquals(7, player.count(objs.air_rune))
                assertEquals(10, player.count(objs.fire_rune))
                assertEquals(1, player.count(objs.wrath_rune))
                assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
                assertTrue(player.pendingSpotanims.isEmpty())
                assertMessageSent("Your Magic level is not high enough for this spell.")
            }
            advance()
            assertTrue(client.filterIsInstance<MapProjAnimV2>().isEmpty())
        }

    @Test
    fun GameTestState.`reject missing rune atomically`() = runElementalTest { deps ->
        player.stats[stats.magic] = 99
        player.clearInv()
        player.inv[0] = InvObj(objs.air_rune, 1)
        val startingXp = player.statMap.getFineXP(stats.magic)
        val target = spawnNpc(player.coords.translate(3, 0), targetType)
        val (handler, attack) = deps.elementalAttack(objs.spell_wind_strike)

        player.withProtectedAccess {
            handler.attack(this, target, attack)

            assertEquals(1, player.count(objs.air_rune))
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
            assertTrue(player.pendingSpotanims.isEmpty())
            assertMessageSent("You do not have enough Mind Runes to cast this spell.")
        }
        advance()
        assertTrue(client.filterIsInstance<MapProjAnimV2>().isEmpty())
    }

    @Test
    fun GameTestState.`consume runes award cast xp and emit wind strike effects`() =
        runElementalTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 2)
            player.inv[1] = InvObj(objs.mind_rune, 2)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.elementalAttack(objs.spell_wind_strike)
            random.next = 0 // accuracy roll: hit
            random.then = 0 // damage roll: zero, isolating cast XP

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                assertEquals(1, player.count(objs.air_rune))
                assertEquals(1, player.count(objs.mind_rune))
                assertEquals(55, player.statMap.getFineXP(stats.magic) - startingXp)
                assertEquals(seqs.human_caststrike.id, player.pendingSequence.id)
                val launch = EntitySpotanim(player.pendingSpotanims.getLong(0))
                assertEquals(spotanims.windstrike_casting.id, launch.id)
                assertEquals(92, launch.height)

                val impact = EntitySpotanim(target.pendingSpotanims.getLong(0))
                assertEquals(spotanims.windstrike_impact.id, impact.id)
                assertEquals(124, impact.height)

                val projectile = deps.projectilesAt(player.coords).single()
                assertEquals(spotanims.windstrike_travel.id, projectile.id)
                assertEquals(target.slotId + 1, projectile.targetIndex)
                assertEquals(projectile.endTime, impact.delay)
                assertNoMessageSent()
            }
        }

    @Test
    fun GameTestState.`air staff substitutes air rune and uses staff animation`() =
        runElementalTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 2)
            player.inv[1] = InvObj(objs.mind_rune, 2)
            player.righthand = InvObj(objs.air_staff)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.elementalAttack(objs.spell_wind_strike, player.righthand)
            random.next = 0
            random.then = 0

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                assertEquals(2, player.count(objs.air_rune))
                assertEquals(1, player.count(objs.mind_rune))
                assertEquals(seqs.human_caststrike_staff.id, player.pendingSequence.id)
            }
        }

    @Test
    fun GameTestState.`award standard magic and hitpoints xp for damage`() =
        runElementalTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 1)
            player.inv[1] = InvObj(objs.mind_rune, 1)
            val startingMagicXp = player.statMap.getFineXP(stats.magic)
            val startingHitpointsXp = player.statMap.getFineXP(stats.hitpoints)
            val target =
                spawnNpc(player.coords.translate(3, 0), targetType) { combatXpMultiplier = 1_000 }
            val (handler, attack) = deps.elementalAttack(objs.spell_wind_strike)
            random.next = 0 // accuracy roll: hit
            random.then = 4 // damage roll

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                // 5.5 cast XP + (4 damage * 2 Magic XP).
                assertEquals(135, player.statMap.getFineXP(stats.magic) - startingMagicXp)
                // 4 damage * 1.33 Hitpoints XP, stored at one decimal-place precision.
                assertEquals(53, player.statMap.getFineXP(stats.hitpoints) - startingHitpointsXp)
            }
        }

    @Test
    fun GameTestState.`registered elemental handlers scale max hit at every tier boundary`() =
        runElementalTest { deps ->
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 1_000)
            player.inv[1] = InvObj(objs.mind_rune, 100)
            player.inv[2] = InvObj(objs.chaos_rune, 100)
            player.inv[3] = InvObj(objs.death_rune, 100)
            player.inv[4] = InvObj(objs.blood_rune, 100)
            player.inv[5] = InvObj(objs.wrath_rune, 100)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)

            val tierCases =
                listOf(
                    objs.spell_wind_strike to
                        listOf(1 to 2, 4 to 2, 5 to 4, 8 to 4, 9 to 6, 12 to 6, 13 to 8),
                    objs.spell_wind_bolt to
                        listOf(17 to 9, 22 to 9, 23 to 10, 28 to 10, 29 to 11, 34 to 11, 35 to 12),
                    objs.spell_wind_blast to
                        listOf(
                            41 to 13,
                            46 to 13,
                            47 to 14,
                            52 to 14,
                            53 to 15,
                            58 to 15,
                            59 to 16,
                        ),
                    objs.spell_wind_wave to
                        listOf(
                            62 to 17,
                            64 to 17,
                            65 to 18,
                            69 to 18,
                            70 to 19,
                            74 to 19,
                            75 to 20,
                        ),
                    objs.spell_wind_surge to
                        listOf(81 to 21, 84 to 21, 85 to 22, 89 to 22, 90 to 23, 94 to 23, 95 to 24),
                )

            for ((spell, cases) in tierCases) {
                val (handler, attack) = deps.elementalAttack(spell)
                for ((magicLevel, expectedMaxHit) in cases) {
                    player.stats[stats.magic] = magicLevel
                    random.next = 0 // accuracy roll: hit
                    random.then = 0 // damage roll: capture its live bounds

                    player.withProtectedAccess { handler.attack(this, target, attack) }

                    assertEquals(0..expectedMaxHit, random.lastInclusiveRange)
                }
            }
        }

    @Test
    fun GameTestState.`splash consumes runes and awards cast xp without damage xp`() =
        runElementalTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 1)
            player.inv[1] = InvObj(objs.mind_rune, 1)
            val startingMagicXp = player.statMap.getFineXP(stats.magic)
            val startingHitpointsXp = player.statMap.getFineXP(stats.hitpoints)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.elementalAttack(objs.spell_wind_strike)
            random.next = 9_999 // accuracy roll: splash

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                assertEquals(0, player.count(objs.air_rune))
                assertEquals(0, player.count(objs.mind_rune))
                assertEquals(55, player.statMap.getFineXP(stats.magic) - startingMagicXp)
                assertEquals(startingHitpointsXp, player.statMap.getFineXP(stats.hitpoints))
                val splash = EntitySpotanim(target.pendingSpotanims.getLong(0))
                assertEquals(spotanims.failedspell_impact.id, splash.id)
                assertEquals(deps.projectilesAt(player.coords).single().endTime, splash.delay)
            }
        }

    @Test
    fun GameTestState.`matching elemental weakness raises only the matching spell max hit`() =
        runElementalTest { deps ->
            player.stats[stats.magic] = 99
            val target = spawnNpc(player.coords.translate(3, 0), fireWeakTargetType)
            val fire = deps.elementalAttack(objs.spell_fire_strike).attack
            val wind = deps.elementalAttack(objs.spell_wind_strike).attack
            val cast = MagicRuneManager.CastResult.Success.Consumed(usedSunfire = false)

            player.withProtectedAccess {
                assertEquals(
                    0..12,
                    deps.manager.calculateMaxHit(this, target, fire, cast, baseMaxHit = 8),
                )
                assertEquals(
                    0..8,
                    deps.manager.calculateMaxHit(this, target, wind, cast, baseMaxHit = 8),
                )
            }
        }

    @Test
    fun GameTestState.`autocast re-engages the same target after a spell`() =
        runElementalTest { deps ->
            player.stats[stats.magic] = 99
            player.setVarBit(varbits.autocast_set, 1)
            player.clearInv()
            player.inv[0] = InvObj(objs.air_rune, 1)
            player.inv[1] = InvObj(objs.mind_rune, 1)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.elementalAttack(objs.spell_wind_strike)
            random.next = 0
            random.then = 0

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                val interaction = player.interaction as InteractionNpc
                assertEquals(target, interaction.target)
            }
        }

    private fun GameTestState.runElementalTest(
        testBody: org.rsmod.api.testing.scope.GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            ElementalTestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            testBody = testBody,
        )

    private object ElementalTestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class AttackFixture(val handler: SpellAttack, val attack: CombatAttack.Spell)

    data class Dependencies
    @Inject
    constructor(
        val spells: ElementalSpells,
        val manager: SpellAttackManager,
        val magicSpells: MagicSpellRegistry,
        val zoneUpdates: ZoneUpdateMap,
    ) {
        fun elementalAttack(spell: ObjType, weapon: InvObj? = null): AttackFixture {
            val registry = SpellAttackRegistry()
            val repository = SpellAttackRepository(registry)
            with(spells) { with(repository) { register(manager) } }
            val magicSpell = requireNotNull(magicSpells.getObjSpell(spell))
            val handler = requireNotNull(registry[spell])
            return AttackFixture(handler, CombatAttack.Spell(weapon, magicSpell, defensive = false))
        }

        fun projectilesAt(coords: org.rsmod.map.CoordGrid): List<MapProjAnimV2> =
            zoneUpdates[ZoneKey.from(coords)]?.filterIsInstance<MapProjAnimV2>().orEmpty()
    }

    private companion object {
        val targetType =
            npcTypeFactory.create {
                name = "Magic test target"
                hitpoints = 100
                defence = 1
                magic = 1
            }

        val fireWeakTargetType =
            npcTypeFactory.create {
                name = "Fire-weak magic test target"
                hitpoints = 100
                defence = 1
                magic = 1
                paramMap =
                    ParamMapBuilder()
                        .apply {
                            this[params.elemental_weakness_type] = constants.elemental_weakness_fire
                            this[params.elemental_weakness_percent] = 50
                        }
                        .toParamMap()
            }
    }
}
