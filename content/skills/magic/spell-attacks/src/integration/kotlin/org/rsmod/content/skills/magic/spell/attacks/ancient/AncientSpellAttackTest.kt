package org.rsmod.content.skills.magic.spell.attacks.ancient

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.torso
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
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStateRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStatus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.util.ParamMapBuilder

class AncientSpellAttackTest {
    @Test
    fun GameTestState.`cache 238 registers every ancient combat spell contract`() =
        runAncientTest { deps ->
            val registry = deps.registry()
            assertEquals(16, AncientSpellDefinitions.all.size)
            for (definition in AncientSpellDefinitions.all) {
                assertNotNull(registry[definition.spell])
            }
            assertEquals(
                listOf(13, 14, 15, 16, 17, 18, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30),
                AncientSpellDefinitions.all.map { it.baseMaxHit },
            )
            assertEquals(8, AncientSpellDefinitions.all.count { it.multiTarget })
            assertEquals(
                4,
                AncientSpellDefinitions.all.count { it.family == AncientSpellFamily.Smoke },
            )
            assertEquals(
                4,
                AncientSpellDefinitions.all.count { it.family == AncientSpellFamily.Shadow },
            )
            assertEquals(
                4,
                AncientSpellDefinitions.all.count { it.family == AncientSpellFamily.Blood },
            )
            assertEquals(
                4,
                AncientSpellDefinitions.all.count { it.family == AncientSpellFamily.Ice },
            )
        }

    @Test
    fun GameTestState
        .`ancient casts require membership and Desert Treasure before rune consumption`() =
        runAncientTest { deps ->
            player.stats[stats.magic] = 99
            player.setVarBit(varbits.spellbook, Spellbook.Ancients.varValue)
            player.inv[0] = InvObj(objs.water_rune, 2)
            player.inv[1] = InvObj(objs.chaos_rune, 2)
            player.inv[2] = InvObj(objs.death_rune, 2)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val fixture = deps.attack(objs.spell_ice_rush)

            player.members = false
            player.withProtectedAccess { fixture.handler.attack(this, target, fixture.attack) }
            assertEquals(2, player.count(objs.water_rune))
            assertMessageSent("You need to be on a members' world to cast this spell.")

            client.clear()
            player.members = true
            player.withProtectedAccess { fixture.handler.attack(this, target, fixture.attack) }
            assertEquals(2, player.count(objs.water_rune))
            assertMessageSent("You need to complete Desert Treasure to cast this spell.")
        }

    @Test
    fun GameTestState.`ice rush consumes exact runes emits cache animation and binds on impact`() =
        runAncientTest { deps ->
            prepareAncients()
            player.inv[0] = InvObj(objs.water_rune, 2)
            player.inv[1] = InvObj(objs.chaos_rune, 2)
            player.inv[2] = InvObj(objs.death_rune, 2)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val fixture = deps.attack(objs.spell_ice_rush)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { fixture.handler.attack(this, target, fixture.attack) }

            assertEquals(0, player.count(objs.water_rune))
            assertEquals(0, player.count(objs.chaos_rune))
            assertEquals(0, player.count(objs.death_rune))
            assertEquals(seqs.zaros_casting.id, player.pendingSequence.id)
            advance(4)
            assertEquals(BindingStatus.Held, deps.bindings.status(target))
            val remaining =
                requireNotNull(deps.bindings.stateFor(target)).heldUntil - target.currentMapClock
            assertTrue(remaining in 7..8)
        }

    @Test
    fun GameTestState
        .`ancient burst target square uses origins intersections and nine target cap`() =
        runAncientTest { deps ->
            val primary = spawnNpc(player.coords.translate(5, 5), largeTargetType)
            val westLarge = spawnNpc(primary.coords.translate(-3, 0), largeTargetType)
            val peaceful =
                spawnNpc(
                    primary.coords.translate(1, 0),
                    npcTypeFactory.create {
                        name = "Bystander"
                        hitpoints = 10
                    },
                )
            val east = spawnNpc(primary.coords.translate(1, 0), targetType)
            val outside = spawnNpc(primary.coords.translate(2, 0), targetType)
            repeat(10) { spawnNpc(primary.coords.translate(0, 1), targetType) }

            assertEquals(listOf(primary), deps.targets.targets(player, primary, false, true))
            assertEquals(listOf(primary), deps.targets.targets(player, primary, true, false))

            val selected =
                deps.targets.targets(player, primary, multiTarget = true, multiway = true)
            assertEquals(primary, selected.first())
            assertFalse(peaceful in selected)
            assertTrue(westLarge in selected)
            assertTrue(east in selected)
            assertFalse(outside in selected)
            assertEquals(9, selected.size)
        }

    @Test
    fun GameTestState
        .`shadow effects drain restored stats once and enhanced sceptre covers all three`() =
        runAncientTest { deps ->
            val target = spawnNpc(player.coords.translate(3, 0), highStatsTargetType)
            val impact =
                AncientImpact(
                    source = player,
                    family = AncientSpellFamily.Shadow,
                    effectStrength = 15,
                    damage = 24,
                    ancientSceptre = true,
                    familySceptre = true,
                    bloodbarkPieces = 0,
                    poisonProc = false,
                )

            deps.impacts.apply(target, impact)
            assertEquals(84, target.attackLvl)
            assertEquals(84, target.strengthLvl)
            assertEquals(84, target.defenceLvl)

            deps.impacts.apply(target, impact)
            assertEquals(84, target.attackLvl)
            assertEquals(84, target.strengthLvl)
            assertEquals(84, target.defenceLvl)
        }

    @Test
    fun GameTestState
        .`blood healing stacks multiplicatively and enhanced sceptre allows overheal`() =
        runAncientTest { deps ->
            player.setBaseLevel(stats.hitpoints, 100)
            player.setCurrentLevel(stats.hitpoints, 95)

            deps.impacts.apply(
                player,
                AncientImpact(
                    source = player,
                    family = AncientSpellFamily.Blood,
                    effectStrength = 25,
                    damage = 100,
                    ancientSceptre = true,
                    familySceptre = true,
                    bloodbarkPieces = 5,
                    poisonProc = false,
                ),
            )

            assertEquals(110, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState.`ice sceptre extends barrage freeze by ten percent`() =
        runAncientTest { deps ->
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            deps.impacts.apply(
                target,
                AncientImpact(
                    source = player,
                    family = AncientSpellFamily.Ice,
                    effectStrength = 32,
                    damage = 30,
                    ancientSceptre = true,
                    familySceptre = true,
                    bloodbarkPieces = 0,
                    poisonProc = false,
                ),
            )

            val state = requireNotNull(deps.bindings.stateFor(target))
            assertEquals(35, state.heldUntil - target.currentMapClock)
        }

    @Test
    fun GameTestState.`Muspah resistance shortens every Ancient ice duration once`() =
        runAncientTest { deps ->
            val durations = listOf(8 to 5, 16 to 10, 24 to 16, 32 to 21)
            for ((index, pair) in durations.withIndex()) {
                val (baseDuration, expectedDuration) = pair
                val target =
                    spawnNpc(player.coords.translate(3 + index, 0), freezeResistantTargetType)
                deps.impacts.apply(
                    target,
                    AncientImpact(
                        source = player,
                        family = AncientSpellFamily.Ice,
                        effectStrength = baseDuration,
                        damage = 1,
                        ancientSceptre = false,
                        familySceptre = false,
                        bloodbarkPieces = 0,
                        poisonProc = false,
                    ),
                )

                val state = requireNotNull(deps.bindings.stateFor(target))
                assertEquals(expectedDuration, state.heldUntil - target.currentMapClock)
            }
        }

    @Test
    fun GameTestState.`complete freeze resistance prevents Ancient ice binding`() =
        runAncientTest { deps ->
            val target = spawnNpc(player.coords.translate(3, 0), freezeImmuneTargetType)
            deps.impacts.apply(
                target,
                AncientImpact(
                    source = player,
                    family = AncientSpellFamily.Ice,
                    effectStrength = 32,
                    damage = 30,
                    ancientSceptre = true,
                    familySceptre = true,
                    bloodbarkPieces = 0,
                    poisonProc = false,
                ),
            )

            assertEquals(BindingStatus.Available, deps.bindings.status(target))
            assertNull(deps.bindings.stateFor(target))
        }

    @Test
    fun GameTestState.`smoke poison starts once and deals severity-scaled npc damage`() =
        runAncientTest { deps ->
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            assertTrue(deps.poison.inflict(target, 20))
            assertFalse(deps.poison.inflict(target, 10))
            assertEquals(20, deps.poison.severity(target))

            deps.poison.tick(target)
            advance(4)
            assertEquals(96, target.hitpoints)
            assertEquals(19, deps.poison.severity(target))
        }

    @Test
    fun GameTestState.`full Virtus grants fifteen percent Ancient spell damage`() =
        runAncientTest { deps ->
            prepareAncients()
            player.hat = InvObj(objs.virtus_mask)
            player.torso = InvObj(objs.virtus_robe_top)
            player.legs = InvObj(objs.virtus_robe_bottom)
            player.inv[0] = InvObj(objs.water_rune, 6)
            player.inv[1] = InvObj(objs.blood_rune, 2)
            player.inv[2] = InvObj(objs.death_rune, 4)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val fixture = deps.attack(objs.spell_ice_barrage)

            player.withProtectedAccess {
                val cast = deps.manager.attemptCast(this, fixture.attack)
                assertEquals(
                    0..34,
                    deps.manager.calculateMaxHit(
                        this,
                        target,
                        fixture.attack,
                        cast,
                        baseMaxHit = 30,
                    ),
                )
            }
        }

    @Test
    fun GameTestState.`smoke sceptre reduces poisoned player healing for ten ticks`() =
        runAncientTest { deps ->
            player.stats[stats.hitpoints] = 100
            player.setCurrentLevel(stats.hitpoints, 50)
            player.setVarp(varps.poison, 10)

            deps.impacts.apply(
                player,
                AncientImpact(
                    source = player,
                    family = AncientSpellFamily.Smoke,
                    effectStrength = 20,
                    damage = 10,
                    ancientSceptre = true,
                    familySceptre = true,
                    bloodbarkPieces = 0,
                    poisonProc = false,
                ),
            )

            player.statHeal(stats.hitpoints, constant = 10, percent = 0)
            assertEquals(58, player.stat(stats.hitpoints))
            advance(10)
            player.statHeal(stats.hitpoints, constant = 10, percent = 0)
            assertEquals(68, player.stat(stats.hitpoints))
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.prepareAncients() {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.spellbook, Spellbook.Ancients.varValue)
        player.setVarBit(varbits.desert_treasure_main, 15)
        player.clearInv()
    }

    private fun GameTestState.runAncientTest(
        body: org.rsmod.api.testing.scope.GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            AncientTestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            NpcHitScript::class,
            AncientImpactScript::class,
            testBody = body,
        )

    private object AncientTestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(BindingStateRepository::class.java).`in`(Scopes.SINGLETON)
            bind(AncientNpcPoisonRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Fixture(val handler: SpellAttack, val attack: CombatAttack.Spell)

    data class Dependencies
    @Inject
    constructor(
        val spells: AncientSpells,
        val manager: SpellAttackManager,
        val magicSpells: MagicSpellRegistry,
        val bindings: BindingStateRepository,
        val targets: AncientTargetSelector,
        val poison: AncientNpcPoisonRepository,
        val impacts: AncientImpactScript,
    ) {
        fun registry(): SpellAttackRegistry {
            val registry = SpellAttackRegistry()
            with(spells) { with(SpellAttackRepository(registry)) { register(manager) } }
            return registry
        }

        fun attack(spell: ObjType): Fixture {
            val registry = registry()
            return Fixture(
                requireNotNull(registry[spell]),
                CombatAttack.Spell(
                    weapon = null,
                    spell = requireNotNull(magicSpells.getObjSpell(spell)),
                    defensive = false,
                ),
            )
        }
    }

    private companion object {
        val targetType =
            npcTypeFactory.create {
                name = "Ancient spell target"
                op[1] = "Attack"
                hitpoints = 100
                defence = 1
                magic = 1
            }

        val largeTargetType =
            npcTypeFactory.create {
                name = "Large Ancient spell target"
                op[1] = "Attack"
                size = 3
                hitpoints = 100
                defence = 1
                magic = 1
            }

        val highStatsTargetType =
            npcTypeFactory.create {
                name = "Ancient shadow target"
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 100
                magic = 1
            }

        val freezeResistantTargetType =
            npcTypeFactory.create {
                name = "Freeze-resistant Ancient spell target"
                hitpoints = 100
                defence = 1
                magic = 1
                paramMap =
                    ParamMapBuilder().apply { this[params.freeze_resistance] = 33 }.toParamMap()
            }

        val freezeImmuneTargetType =
            npcTypeFactory.create {
                name = "Freeze-immune Ancient spell target"
                hitpoints = 100
                defence = 1
                magic = 1
                paramMap =
                    ParamMapBuilder().apply { this[params.freeze_resistance] = 100 }.toParamMap()
            }
    }
}
