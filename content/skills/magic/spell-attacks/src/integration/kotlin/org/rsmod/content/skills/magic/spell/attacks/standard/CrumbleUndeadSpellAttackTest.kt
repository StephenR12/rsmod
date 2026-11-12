package org.rsmod.content.skills.magic.spell.attacks.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.zone.payload.MapProjAnimV2
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.front
import org.rsmod.api.player.hands
import org.rsmod.api.player.righthand
import org.rsmod.api.registry.zone.ZoneUpdateMap
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
import org.rsmod.game.type.util.ParamMapBuilder
import org.rsmod.map.zone.ZoneKey

class CrumbleUndeadSpellAttackTest {
    @Test
    fun GameTestState.`cache contract defines crumble undead metadata and presentation`() =
        runCrumbleTest { deps ->
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_crumble_undead))
            assertEquals(39, spell.levelReq)
            assertEquals(24.5, spell.castXp)
            // Cache metadata uses the generic restricted-spell sentinel; live damage is 0..15.
            assertEquals(1, spell.maxHit)
            assertEquals(Spellbook.Standard, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.air_rune to 2, objs.earth_rune to 2, objs.chaos_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertNotNull(deps.attack().handler)
            assertEquals(724, seqs.human_castcrumbleundead.id)
            assertEquals(1166, seqs.human_castcrumbleundead_staff.id)
            assertEquals(145, spotanims.crumbleundead_casting.id)
            assertEquals(146, spotanims.crumbleundead_travel.id)
            assertEquals(147, spotanims.crumbleundead_impact.id)
            assertEquals(11, projanims.crumble_undead.id)
            assertEquals(684, cacheTypes.spotanims[145]?.anim)
            assertEquals(685, cacheTypes.spotanims[146]?.anim)
            assertEquals(686, cacheTypes.spotanims[147]?.anim)
        }

    @Test
    fun GameTestState.`reject non-undead npc before consuming resources or xp`() =
        runCrumbleTest { deps ->
            prepareCaster()
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), livingTarget)
            val (handler, attack) = deps.attack()

            player.withProtectedAccess {
                handler.attack(this, target, attack)
                assertMessageSent("This spell only affects skeletons, zombies, ghosts and shades")
            }

            assertCrumbleRunes(2, 2, 1)
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
            assertTrue(player.pendingSpotanims.isEmpty())
            assertTrue(deps.projectilesAt(player.coords).isEmpty())
        }

    @Test
    fun GameTestState.`reject player target before consuming resources or xp`() =
        runCrumbleTest { deps ->
            prepareCaster()
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target: Player = registerPlayer(player.coords.translate(3, 0))
            val (handler, attack) = deps.attack()

            player.withProtectedAccess {
                handler.attack(this, target, attack)
                assertMessageSent("This spell only affects skeletons, zombies, ghosts and shades")
            }

            assertCrumbleRunes(2, 2, 1)
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`successful cast consumes runes awards xp and emits dedicated effects`() =
        runCrumbleTest { deps ->
            prepareCaster()
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), undeadTarget)
            val (handler, attack) = deps.attack()
            random.next = 0
            random.then = 0

            player.withProtectedAccess { handler.attack(this, target, attack) }

            assertCrumbleRunes(0, 0, 0)
            assertEquals(245, player.statMap.getFineXP(stats.magic) - startingXp)
            assertEquals(seqs.human_castcrumbleundead.id, player.pendingSequence.id)
            val launch = EntitySpotanim(player.pendingSpotanims.getLong(0))
            assertEquals(spotanims.crumbleundead_casting.id, launch.id)
            assertEquals(92, launch.height)
            val impact = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.crumbleundead_impact.id, impact.id)
            assertEquals(124, impact.height)
            val projectile = deps.projectilesAt(player.coords).single()
            assertEquals(spotanims.crumbleundead_travel.id, projectile.id)
            assertEquals(target.slotId + 1, projectile.targetIndex)
            assertEquals(projectile.endTime, impact.delay)
        }

    @Test
    fun GameTestState.`splash consumes runes and awards only base cast xp`() =
        runCrumbleTest { deps ->
            prepareCaster()
            val startingMagicXp = player.statMap.getFineXP(stats.magic)
            val startingHitpointsXp = player.statMap.getFineXP(stats.hitpoints)
            val target = spawnNpc(player.coords.translate(3, 0), undeadTarget)
            val (handler, attack) = deps.attack()
            random.next = 9_999

            player.withProtectedAccess { handler.attack(this, target, attack) }

            assertCrumbleRunes(0, 0, 0)
            assertEquals(245, player.statMap.getFineXP(stats.magic) - startingMagicXp)
            assertEquals(startingHitpointsXp, player.statMap.getFineXP(stats.hitpoints))
            val splash = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.failedspell_impact.id, splash.id)
            assertEquals(deps.projectilesAt(player.coords).single().endTime, splash.delay)
        }

    @Test
    fun GameTestState
        .`max hit is fifteen salve ei raises it to eighteen and chaos gauntlets do not`() =
        runCrumbleTest { deps ->
            player.stats[stats.magic] = 99
            val target = spawnNpc(player.coords.translate(3, 0), undeadTarget)
            val attack = deps.attack().attack
            val cast = MagicRuneManager.CastResult.Success.Consumed(usedSunfire = false)

            player.withProtectedAccess {
                assertEquals(0..15, deps.manager.calculateMaxHit(this, target, attack, cast, 15))
                player.hands = InvObj(objs.chaos_gauntlets)
                assertEquals(0..15, deps.manager.calculateMaxHit(this, target, attack, cast, 15))
                player.hands = null
                player.front = InvObj(objs.salve_amulet_ei)
                assertEquals(0..18, deps.manager.calculateMaxHit(this, target, attack, cast, 15))
            }
        }

    @Test
    fun GameTestState.`air staff substitutes air runes and uses staff animation`() =
        runCrumbleTest { deps ->
            prepareCaster()
            player.righthand = InvObj(objs.air_staff)
            val target = spawnNpc(player.coords.translate(3, 0), undeadTarget)
            val (handler, attack) = deps.attack(player.righthand)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { handler.attack(this, target, attack) }

            assertCrumbleRunes(2, 0, 0)
            assertEquals(seqs.human_castcrumbleundead_staff.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`only cache era restricted weapons can autocast crumble undead`() =
        runCrumbleTest { deps ->
            val autocastId =
                (0..100).single {
                    deps.magicSpells.getAutocastSpell(it)?.obj == objs.spell_crumble_undead
                }
            val allowedWeapons =
                listOf(
                    objs.skull_sceptre_i,
                    objs.slayers_staff,
                    objs.slayers_staff_e,
                    objs.staff_of_the_dead,
                    objs.toxic_staff_of_the_dead,
                    objs.staff_of_light,
                    objs.staff_of_balance,
                    objs.void_knight_mace,
                    objs.void_knight_mace_l,
                )

            allowedWeapons.forEach {
                assertTrue(deps.autocastWeapons.canStaffAutocast(objTypes[it], autocastId))
            }
            assertFalse(deps.autocastWeapons.canStaffAutocast(objTypes[objs.air_staff], autocastId))
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.prepareCaster() {
        player.stats[stats.magic] = 99
        player.clearInv()
        player.inv[0] = InvObj(objs.air_rune, 2)
        player.inv[1] = InvObj(objs.earth_rune, 2)
        player.inv[2] = InvObj(objs.chaos_rune, 1)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.assertCrumbleRunes(
        air: Int,
        earth: Int,
        chaos: Int,
    ) {
        assertEquals(air, player.count(objs.air_rune))
        assertEquals(earth, player.count(objs.earth_rune))
        assertEquals(chaos, player.count(objs.chaos_rune))
    }

    private fun GameTestState.runCrumbleTest(
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
        }
    }

    data class AttackFixture(val handler: SpellAttack, val attack: CombatAttack.Spell)

    data class Dependencies
    @Inject
    constructor(
        val spells: CrumbleUndeadSpells,
        val manager: SpellAttackManager,
        val magicSpells: MagicSpellRegistry,
        val autocastWeapons: AutocastWeapons,
        val zoneUpdates: ZoneUpdateMap,
    ) {
        fun attack(weapon: InvObj? = null): AttackFixture {
            val registry = SpellAttackRegistry()
            val repository = SpellAttackRepository(registry)
            with(spells) { with(repository) { register(manager) } }
            val magicSpell = requireNotNull(magicSpells.getObjSpell(objs.spell_crumble_undead))
            val handler = requireNotNull(registry[objs.spell_crumble_undead])
            return AttackFixture(handler, CombatAttack.Spell(weapon, magicSpell, defensive = false))
        }

        fun projectilesAt(coords: org.rsmod.map.CoordGrid): List<MapProjAnimV2> =
            zoneUpdates[ZoneKey.from(coords)]?.filterIsInstance<MapProjAnimV2>().orEmpty()
    }

    private companion object {
        val livingTarget =
            npcTypeFactory.create {
                name = "Living target"
                hitpoints = 100
                defence = 1
                magic = 1
            }

        val undeadTarget =
            npcTypeFactory.create {
                name = "Undead target"
                hitpoints = 100
                defence = 1
                magic = 1
                paramMap = ParamMapBuilder().apply { this[params.undead] = 1 }.toParamMap()
            }
    }
}
