package org.rsmod.content.skills.magic.spell.attacks.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.zone.payload.MapProjAnimV2
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
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.PlayerDeath
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
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.map.zone.ZoneKey

class TeleBlockSpellAttackTest {
    @Test
    fun GameTestState.`cache contract defines tele block metadata and presentation`() =
        runTeleBlockTest { deps ->
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_tele_block))
            assertEquals(85, spell.levelReq)
            assertEquals(83.6, spell.castXp)
            assertEquals(1, spell.maxHit)
            assertEquals(Spellbook.Standard, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.law_rune to 1, objs.chaos_rune to 1, objs.death_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertNotNull(deps.registry()[objs.spell_tele_block])
            assertEquals(1819, seqs.human_casting_tele_block.id)
            assertEquals(1820, seqs.human_casting_tele_block_staff.id)
            assertEquals(345, spotanims.tele_block_impact.id)
            assertEquals(1300, spotanims.tele_block_travel_forfail.id)
            assertEquals(1822, cacheTypes.spotanims[345]?.anim)
            assertEquals(1821, cacheTypes.spotanims[1300]?.anim)
        }

    @Test
    fun GameTestState.`reject target outside wilderness before resources or xp`() =
        runTeleBlockTest { deps ->
            player.stats[stats.magic] = 99
            giveTeleBlockRunes()
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = registerPlayer(player.coords.translate(3, 0))
            val (handler, attack) = deps.attack()

            player.withProtectedAccess {
                handler.attack(this, target, attack)
                assertMessageSent("You can't attack players who aren't in the wilderness.")
            }

            assertEquals(100, player.count(objs.law_rune))
            assertEquals(100, player.count(objs.chaos_rune))
            assertEquals(100, player.count(objs.death_rune))
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
            assertEquals(TeleBlockStatus.Available, deps.states.status(target))
        }

    @Test
    fun GameTestState.`successful cast awards 95 xp and applies full target-only block`() =
        runTeleBlockTest { deps ->
            prepareCasterAndTarget()
            player.displayName = "Caster"
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = registerWildernessPlayer(player.coords.translate(3, 0))
            val (handler, attack) = deps.attack()
            random.next = 0

            player.withProtectedAccess {
                handler.attack(this, target, attack)
                assertEquals(950, player.statMap.getFineXP(stats.magic) - startingXp)
                assertEquals(seqs.human_casting_tele_block.id, player.pendingSequence.id)
                val impact = EntitySpotanim(target.pendingSpotanims.getLong(0))
                assertEquals(spotanims.tele_block_impact.id, impact.id)
                assertTrue(deps.projectilesAt(player.coords).isEmpty())
            }

            advanceUntilStatus(deps, target, TeleBlockStatus.Blocked)
            val state = requireNotNull(deps.states.stateFor(target))
            assertEquals(
                TeleBlockStateRepository.FULL_DURATION_CYCLES,
                state.blockedUntil - target.currentMapClock,
            )
            assertEquals(600, target.vars[varbits.teleblock_cycles])
            assertTrue(target.preventLogoutUntil > target.currentMapClock)
            assertEquals(Int.MIN_VALUE, player.preventLogoutUntil)
            assertMessageSent(
                "A Tele Block has been cast on you by Caster. It will expire in 5 minutes.",
                target,
            )
        }

    @Test
    fun GameTestState.`protect from magic at cast halves duration without changing hit path`() =
        runTeleBlockTest { deps ->
            prepareCasterAndTarget()
            val target = registerWildernessPlayer(player.coords.translate(3, 0))
            target.setVarBit(varbits.protect_from_magic, 1)
            val (handler, attack) = deps.attack()
            random.next = 0

            player.withProtectedAccess { handler.attack(this, target, attack) }
            advanceUntilStatus(deps, target, TeleBlockStatus.Blocked)

            val state = requireNotNull(deps.states.stateFor(target))
            assertEquals(
                TeleBlockStateRepository.PROTECTED_DURATION_CYCLES,
                state.blockedUntil - target.currentMapClock,
            )
            assertEquals(350, target.vars[varbits.teleblock_cycles])
            assertMessageSent(
                "A Tele Block has been cast on you by . It will expire in 2 minutes and 30 seconds.",
                target,
            )
        }

    @Test
    fun GameTestState.`splash uses fail travel graphic and applies no block`() =
        runTeleBlockTest { deps ->
            prepareCasterAndTarget()
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = registerWildernessPlayer(player.coords.translate(3, 0))
            val (handler, attack) = deps.attack()
            random.next = 9_999

            player.withProtectedAccess { handler.attack(this, target, attack) }

            assertEquals(836, player.statMap.getFineXP(stats.magic) - startingXp)
            val projectile = deps.projectilesAt(player.coords).single()
            assertEquals(spotanims.tele_block_travel_forfail.id, projectile.id)
            advance(10)
            assertEquals(TeleBlockStatus.Available, deps.states.status(target))
            assertEquals(0, target.vars[varbits.teleblock_cycles])
        }

    @Test
    fun GameTestState.`active and immune recasts reject atomically`() = runTeleBlockTest { deps ->
        prepareCasterAndTarget()
        val target = registerWildernessPlayer(player.coords.translate(3, 0))
        val (handler, attack) = deps.attack()
        random.next = 0
        player.withProtectedAccess { handler.attack(this, target, attack) }
        advanceUntilStatus(deps, target, TeleBlockStatus.Blocked)

        val blockedLaw = player.count(objs.law_rune)
        val blockedXp = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess {
            handler.attack(this, target, attack)
            assertMessageSent("That player is already being affected by this spell.")
        }
        assertEquals(blockedLaw, player.count(objs.law_rune))
        assertEquals(blockedXp, player.statMap.getFineXP(stats.magic))

        val state = requireNotNull(deps.states.stateFor(target))
        target.currentMapClock = state.blockedUntil
        deps.states.tick(target)
        assertEquals(TeleBlockStatus.Immune, deps.states.status(target))
        assertEquals(100, target.vars[varbits.teleblock_cycles])

        val immuneLaw = player.count(objs.law_rune)
        player.withProtectedAccess {
            handler.attack(this, target, attack)
            assertMessageSent("That player is already being affected by this spell.")
        }
        assertEquals(immuneLaw, player.count(objs.law_rune))

        target.currentMapClock = state.immuneUntil
        deps.states.tick(target)
        assertEquals(TeleBlockStatus.Available, deps.states.status(target))
        assertEquals(0, target.vars[varbits.teleblock_cycles])
    }

    @Test
    fun GameTestState.`leaving wilderness logout and death clear effect and immunity`() =
        runTeleBlockTest { deps ->
            prepareCasterAndTarget()
            val target = registerWildernessPlayer(player.coords.translate(3, 0))
            applyDirect(deps, target)
            target.setVarBit(varbits.inside_wilderness, 0)
            deps.states.tick(target)
            assertEquals(TeleBlockStatus.Available, deps.states.status(target))
            assertEquals(0, target.vars[varbits.teleblock_cycles])

            target.setVarBit(varbits.inside_wilderness, 1)
            applyDirect(deps, target)
            eventBus.publish(SessionStateEvent.Logout(target))
            assertEquals(TeleBlockStatus.Available, deps.states.status(target))
            assertEquals(0, target.vars[varbits.teleblock_cycles])

            applyDirect(deps, target)
            target.withProtectedAccess { deps.death.death(this) }
            advanceUntil(
                predicate = { deps.states.status(target) == TeleBlockStatus.Available },
                timeoutTicks = 10,
            )
            assertEquals(TeleBlockStatus.Available, deps.states.status(target))
            assertEquals(0, target.vars[varbits.teleblock_cycles])
        }

    @Test
    fun GameTestState.`blighted teleport sack substitutes all three runes in wilderness`() =
        runTeleBlockTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.blighted_teleport_spell_sack, 2)
            player.setVarBit(varbits.blighted_items_allowed, 1)
            val target = registerWildernessPlayer(player.coords.translate(3, 0))
            val (handler, attack) = deps.attack()
            random.next = 9_999

            player.withProtectedAccess { handler.attack(this, target, attack) }

            assertEquals(1, player.count(objs.blighted_teleport_spell_sack))
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(0, player.count(objs.chaos_rune))
            assertEquals(0, player.count(objs.death_rune))
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.prepareCasterAndTarget() {
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.inside_wilderness, 1)
        giveTeleBlockRunes()
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.giveTeleBlockRunes() {
        player.clearInv()
        player.inv[0] = InvObj(objs.law_rune, 100)
        player.inv[1] = InvObj(objs.chaos_rune, 100)
        player.inv[2] = InvObj(objs.death_rune, 100)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.registerWildernessPlayer(
        coords: org.rsmod.map.CoordGrid
    ): Player = registerPlayer(coords).also { it.setVarBit(varbits.inside_wilderness, 1) }

    private fun org.rsmod.api.testing.scope.GameTestScope.applyDirect(
        deps: Dependencies,
        target: Player,
    ) {
        val state = deps.states.apply(target, player, TeleBlockStateRepository.FULL_DURATION_CYCLES)
        assertNotNull(state)
        assertEquals(TeleBlockStatus.Blocked, deps.states.status(target))
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.advanceUntilStatus(
        deps: Dependencies,
        target: Player,
        expected: TeleBlockStatus,
    ) {
        advanceUntil(
            predicate = { deps.states.status(target) == expected },
            timeoutTicks = 10,
            timeoutMessage = {
                "Tele Block did not become $expected: ${deps.states.stateFor(target)}"
            },
        )
    }

    private fun GameTestState.runTeleBlockTest(
        testBody: org.rsmod.api.testing.scope.GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            TeleBlockTestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            WeaponAttackStylesScript::class,
            TeleBlockImpactScript::class,
            testBody = testBody,
        )

    private object TeleBlockTestModule : AbstractModule() {
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

    data class AttackFixture(val handler: SpellAttack, val attack: CombatAttack.Spell)

    data class Dependencies
    @Inject
    constructor(
        val spells: TeleBlockSpells,
        val states: TeleBlockStateRepository,
        val manager: SpellAttackManager,
        val magicSpells: MagicSpellRegistry,
        val zoneUpdates: ZoneUpdateMap,
        val death: PlayerDeath,
    ) {
        fun registry(): SpellAttackRegistry {
            val registry = SpellAttackRegistry()
            val repository = SpellAttackRepository(registry)
            with(spells) { with(repository) { register(manager) } }
            return registry
        }

        fun attack(): AttackFixture {
            val magicSpell = requireNotNull(magicSpells.getObjSpell(objs.spell_tele_block))
            val handler = requireNotNull(registry()[objs.spell_tele_block])
            return AttackFixture(
                handler,
                CombatAttack.Spell(weapon = null, spell = magicSpell, defensive = false),
            )
        }

        fun projectilesAt(coords: org.rsmod.map.CoordGrid): List<MapProjAnimV2> =
            zoneUpdates[ZoneKey.from(coords)]?.filterIsInstance<MapProjAnimV2>().orEmpty()
    }
}
