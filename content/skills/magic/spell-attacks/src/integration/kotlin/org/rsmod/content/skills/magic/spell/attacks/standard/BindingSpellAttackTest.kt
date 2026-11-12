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
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.obj.ObjType

class BindingSpellAttackTest {
    @Test
    fun GameTestState.`cache contract defines all three standard binding spells`() =
        runBindingTest { deps ->
            val contracts =
                listOf(
                    contract(
                        objs.spell_bind,
                        level = 20,
                        splashXp = 30.0,
                        maxHit = 1,
                        objs.water_rune to 3,
                        objs.earth_rune to 3,
                        objs.nature_rune to 2,
                    ),
                    contract(
                        objs.spell_snare,
                        level = 50,
                        splashXp = 60.0,
                        maxHit = 1,
                        objs.water_rune to 4,
                        objs.earth_rune to 4,
                        objs.nature_rune to 3,
                    ),
                    contract(
                        objs.spell_entangle,
                        level = 79,
                        splashXp = 70.0,
                        maxHit = 1,
                        objs.water_rune to 5,
                        objs.earth_rune to 5,
                        objs.nature_rune to 4,
                    ),
                )

            val registry = deps.registry()
            for (contract in contracts) {
                val spell = requireNotNull(deps.magicSpells.getObjSpell(contract.spell))
                assertEquals(contract.level, spell.levelReq, contract.spell.toString())
                assertEquals(contract.splashXp, spell.castXp, contract.spell.toString())
                assertEquals(contract.maxHit, spell.maxHit, contract.spell.toString())
                assertEquals(Spellbook.Standard, spell.spellbook, contract.spell.toString())
                assertEquals(MagicSpellType.Combat, spell.type, contract.spell.toString())
                assertEquals(
                    contract.runes,
                    spell.objReqs.associate { requirement -> requirement.obj to requirement.count },
                    contract.spell.toString(),
                )
                assertNotNull(registry[contract.spell], contract.spell.toString())
            }
        }

    @Test
    fun GameTestState.`successful bind applies effects xp and eight-cycle movement lock`() =
        runBindingTest { deps ->
            player.stats[stats.magic] = 99
            giveBindingRunes()
            val startingMagicXp = player.statMap.getFineXP(stats.magic)
            val startingHitpointsXp = player.statMap.getFineXP(stats.hitpoints)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val startingTargetHitpoints = target.hitpoints
            val start = target.coords
            val (handler, attack) = deps.attack(objs.spell_bind)
            random.next = 0

            player.withProtectedAccess {
                handler.attack(this, target, attack)

                assertEquals(310, player.statMap.getFineXP(stats.magic) - startingMagicXp)
                assertEquals(startingHitpointsXp, player.statMap.getFineXP(stats.hitpoints))
                assertEquals(seqs.human_castentangle.id, player.pendingSequence.id)
                assertEquals(
                    spotanims.entangle_casting.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                assertEquals(
                    spotanims.bind_impact.id,
                    EntitySpotanim(target.pendingSpotanims.getLong(0)).id,
                )
            }

            advanceUntilBindingStatus(deps, target, BindingStatus.Held)
            val state = requireNotNull(deps.states.stateFor(target))
            assertEquals(8, state.heldUntil - target.currentMapClock)
            assertEquals(startingTargetHitpoints, target.hitpoints)

            target.clearInteraction()
            target.routeRequest = RouteRequestCoord(start.translate(1, 0))
            deps.states.blockMovement(target)
            assertEquals(start, target.coords)
            assertEquals(null, target.routeRequest)

            advanceUntilBindingStatus(deps, target, BindingStatus.Available)
            assertEquals(null, target.walkTrigger)
        }

    @Test
    fun GameTestState.`splash uses lower xp and does not bind or damage`() =
        runBindingTest { deps ->
            player.stats[stats.magic] = 99
            giveBindingRunes()
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val startingHitpoints = target.hitpoints
            val (handler, attack) = deps.attack(objs.spell_entangle)
            random.next = 9_999

            player.withProtectedAccess { handler.attack(this, target, attack) }

            assertEquals(700, player.statMap.getFineXP(stats.magic) - startingXp)
            advance(10)
            assertEquals(startingHitpoints, target.hitpoints)
            assertEquals(BindingStatus.Available, deps.states.status(target))
        }

    @Test
    fun GameTestState.`snare hit deals up to two and awards successful-cast bonus`() =
        runBindingTest { deps ->
            player.stats[stats.magic] = 99
            giveBindingRunes()
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val startingHitpoints = target.hitpoints
            val (handler, attack) = deps.attack(objs.spell_snare)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { handler.attack(this, target, attack) }

            assertEquals(610, player.statMap.getFineXP(stats.magic) - startingXp)
            advanceUntilBindingStatus(deps, target, BindingStatus.Held)
            assertTrue(target.hitpoints in (startingHitpoints - 2)..startingHitpoints)
            assertEquals(
                16,
                requireNotNull(deps.states.stateFor(target)).heldUntil - target.currentMapClock,
            )
        }

    @Test
    fun GameTestState.`held and immune targets reject atomically with exact messages`() =
        runBindingTest { deps ->
            player.stats[stats.magic] = 99
            giveBindingRunes()
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (bindHandler, bindAttack) = deps.attack(objs.spell_bind)
            random.next = 0
            player.withProtectedAccess { bindHandler.attack(this, target, bindAttack) }
            advanceUntilBindingStatus(deps, target, BindingStatus.Held)

            val (snareHandler, snareAttack) = deps.attack(objs.spell_snare)
            val heldWater = player.count(objs.water_rune)
            val heldEarth = player.count(objs.earth_rune)
            val heldNature = player.count(objs.nature_rune)
            val heldXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess {
                snareHandler.attack(this, target, snareAttack)
                assertMessageSent("Your target is already held by a magical force.")
            }
            assertEquals(heldWater, player.count(objs.water_rune))
            assertEquals(heldEarth, player.count(objs.earth_rune))
            assertEquals(heldNature, player.count(objs.nature_rune))
            assertEquals(heldXp, player.statMap.getFineXP(stats.magic))

            advanceUntilBindingStatus(deps, target, BindingStatus.Immune)
            val immuneWater = player.count(objs.water_rune)
            val immuneXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess {
                snareHandler.attack(this, target, snareAttack)
                assertMessageSent("The target is currently immune to that spell.")
            }
            assertEquals(immuneWater, player.count(objs.water_rune))
            assertEquals(immuneXp, player.statMap.getFineXP(stats.magic))

            val immuneStart = target.currentMapClock
            advanceUntilBindingStatus(deps, target, BindingStatus.Available)
            assertEquals(5, target.currentMapClock - immuneStart)
        }

    @Test
    fun GameTestState.`swampbark helm body and legs each add two binding cycles`() =
        runBindingTest { deps ->
            player.stats[stats.magic] = 99
            giveBindingRunes()
            player.hat = InvObj(objs.swampbark_helm)
            player.torso = InvObj(objs.swampbark_body)
            player.legs = InvObj(objs.swampbark_legs)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(objs.spell_entangle)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { handler.attack(this, target, attack) }
            advanceUntilBindingStatus(deps, target, BindingStatus.Held)

            assertEquals(
                30,
                requireNotNull(deps.states.stateFor(target)).heldUntil - target.currentMapClock,
            )
        }

    @Test
    fun GameTestState.`player bind breaks when caster leaves ten-tile range`() =
        runBindingTest { deps ->
            player.stats[stats.magic] = 99
            giveBindingRunes()
            val target = registerPlayer(player.coords.translate(3, 0))
            val start = target.coords
            val (handler, attack) = deps.attack(objs.spell_bind)
            random.next = 0

            player.withProtectedAccess { handler.attack(this, target, attack) }
            advanceUntilBindingStatus(deps, target, BindingStatus.Held)

            target.routeRequest = RouteRequestCoord(start.translate(1, 0))
            advance()
            assertEquals(start, target.coords)

            player.coords = start.translate(20, 0)
            assertEquals(BindingStatus.Immune, deps.states.status(target))
            target.routeRequest = RouteRequestCoord(start.translate(1, 0))
            advance()
            assertEquals(start.translate(1, 0), target.coords)
        }

    @Test
    fun GameTestState.`player receives fade warning four cycles before release`() =
        runBindingTest { deps ->
            player.stats[stats.magic] = 99
            giveBindingRunes()
            val target = registerPlayer(player.coords.translate(3, 0))
            val (handler, attack) = deps.attack(objs.spell_bind)
            random.next = 0

            player.withProtectedAccess { handler.attack(this, target, attack) }
            advanceUntilBindingStatus(deps, target, BindingStatus.Held)

            advance(4)
            assertMessageSent("<col=ef1020>The holding effect is beginning to fade.</col>", target)
            assertEquals(
                4,
                requireNotNull(deps.states.stateFor(target)).heldUntil - target.currentMapClock,
            )
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.giveBindingRunes() {
        player.clearInv()
        player.inv[0] = InvObj(objs.water_rune, 100)
        player.inv[1] = InvObj(objs.earth_rune, 100)
        player.inv[2] = InvObj(objs.nature_rune, 100)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.advanceUntilBindingStatus(
        deps: Dependencies,
        target: org.rsmod.game.entity.PathingEntity,
        expected: BindingStatus,
    ) {
        advanceUntil(
            predicate = { deps.states.status(target) == expected },
            timeoutTicks = 40,
            timeoutMessage = {
                "Binding status did not become $expected; clock=${target.currentMapClock}, " +
                    "state=${deps.states.stateFor(target)}"
            },
        )
    }

    private fun GameTestState.runBindingTest(
        testBody: org.rsmod.api.testing.scope.GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            BindingTestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            WeaponAttackStylesScript::class,
            BindingImpactScript::class,
            testBody = testBody,
        )

    private object BindingTestModule : AbstractModule() {
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
        val level: Int,
        val splashXp: Double,
        val maxHit: Int,
        val runes: Map<ObjType, Int>,
    )

    private fun contract(
        spell: ObjType,
        level: Int,
        splashXp: Double,
        maxHit: Int,
        vararg runes: Pair<ObjType, Int>,
    ): Contract = Contract(spell, level, splashXp, maxHit, mapOf(*runes))

    data class AttackFixture(val handler: SpellAttack, val attack: CombatAttack.Spell)

    data class Dependencies
    @Inject
    constructor(
        val spells: BindingSpells,
        val states: BindingStateRepository,
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
                name = "Binding test target"
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 100
                magic = 1
            }
    }
}
