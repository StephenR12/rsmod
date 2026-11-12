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
import org.rsmod.api.combat.manager.MagicRuneManager.CastResult
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.map.zone.ZoneKey

class MagicDartSpellAttackTest {
    @Test
    fun GameTestState.`cache contract defines magic dart metadata and presentation`() =
        runMagicDartTest { deps ->
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_magic_dart))
            assertEquals(50, spell.levelReq)
            assertEquals(30.0, spell.castXp)
            assertEquals(1, spell.maxHit)
            assertEquals(Spellbook.Standard, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.mind_rune to 4, objs.death_rune to 1, objs.slayers_staff to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertNotNull(deps.attack().handler)
            assertEquals(1576, seqs.slayer_magicdart_cast.id)
            assertEquals(328, spotanims.slayer_magicdart_travel.id)
            assertEquals(329, spotanims.slayer_magicdart_impact.id)
            assertEquals(1373, spotanims.slayer_magicdart_enchanted_travel.id)
            assertEquals(1374, spotanims.slayer_magicdart_enchanted_impact.id)
            assertEquals(1577, cacheTypes.spotanims[328]?.anim)
            assertEquals(6, projanims.magic_spell.id)
        }

    @Test
    fun GameTestState.`reject insufficient slayer before consuming runes or xp`() =
        runMagicDartTest { deps ->
            prepareCaster(objs.slayers_staff, slayerLevel = 54)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(player.righthand)

            player.withProtectedAccess {
                handler.attack(this, target, attack)
                assertMessageSent("You need a Slayer level of 55 to cast this spell.")
            }

            assertDartRunes(4, 1)
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
            assertTrue(deps.projectilesAt(player.coords).isEmpty())
        }

    @Test
    fun GameTestState.`reject unsuitable staff after rune validation without consumption`() =
        runMagicDartTest { deps ->
            prepareCaster(objs.air_staff)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(player.righthand)

            player.withProtectedAccess {
                handler.attack(this, target, attack)
                assertMessageSent("You need to be wielding a suitable staff to cast this spell.")
            }

            assertDartRunes(4, 1)
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`all cache era suitable staves satisfy the worn requirement`() =
        runMagicDartTest { deps ->
            player.stats[stats.magic] = 99
            player.stats[stats.slayer] = 55
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_magic_dart))
            val suitable =
                listOf(
                    objs.slayers_staff,
                    objs.slayers_staff_e,
                    objs.staff_of_the_dead,
                    objs.toxic_staff_uncharged,
                    objs.toxic_staff_of_the_dead,
                    objs.staff_of_light,
                    objs.staff_of_balance,
                )

            suitable.forEach { staff ->
                player.clearInv()
                player.inv[0] = InvObj(objs.mind_rune, 4)
                player.inv[1] = InvObj(objs.death_rune, 1)
                player.righthand = InvObj(staff)
                assertTrue(deps.runes.attemptCast(player, spell) is CastResult.Success)
                assertDartRunes(0, 0)
            }
        }

    @Test
    fun GameTestState.`successful ordinary cast uses level scaled damage and green effects`() =
        runMagicDartTest { deps ->
            prepareCaster(objs.slayers_staff)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(player.righthand)
            val cast = CastResult.Success.Consumed(usedSunfire = false)
            random.next = 0
            random.then = 0

            player.withProtectedAccess {
                assertEquals(0..19, deps.manager.calculateMaxHit(this, target, attack, cast, 0))
                handler.attack(this, target, attack)
            }

            assertDartRunes(0, 0)
            assertEquals(300, player.statMap.getFineXP(stats.magic) - startingXp)
            assertEquals(seqs.slayer_magicdart_cast.id, player.pendingSequence.id)
            val projectile = deps.projectilesAt(player.coords).single()
            assertEquals(spotanims.slayer_magicdart_travel.id, projectile.id)
            val impact = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.slayer_magicdart_impact.id, impact.id)
            assertEquals(projectile.endTime, impact.delay)
        }

    @Test
    fun GameTestState.`enchanted staff uses white effects on hit but green travel on splash`() =
        runMagicDartTest { deps ->
            prepareCaster(objs.slayers_staff_e, casts = 2)
            val firstTarget = spawnNpc(player.coords.translate(3, 0), targetType)
            val secondTarget = spawnNpc(player.coords.translate(0, 3), targetType)
            val (handler, attack) = deps.attack(player.righthand)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { handler.attack(this, firstTarget, attack) }

            var projectile = deps.projectilesAt(player.coords).single()
            assertEquals(spotanims.slayer_magicdart_enchanted_travel.id, projectile.id)
            var impact = EntitySpotanim(firstTarget.pendingSpotanims.getLong(0))
            assertEquals(spotanims.slayer_magicdart_enchanted_impact.id, impact.id)

            advance(ticks = 5)
            random.next = 9_999
            player.withProtectedAccess { handler.attack(this, secondTarget, attack) }

            projectile = deps.projectilesAt(player.coords).last()
            assertEquals(spotanims.slayer_magicdart_travel.id, projectile.id)
            impact = EntitySpotanim(secondTarget.pendingSpotanims.getLong(0))
            assertEquals(spotanims.failedspell_impact.id, impact.id)
        }

    @Test
    fun GameTestState.`only documented weapons can select restricted magic dart autocast`() =
        runMagicDartTest { deps ->
            val autocastId =
                (0..100).single {
                    deps.magicSpells.getAutocastSpell(it)?.obj == objs.spell_magic_dart
                }
            val allowed =
                listOf(
                    objs.slayers_staff,
                    objs.slayers_staff_e,
                    objs.staff_of_the_dead,
                    objs.toxic_staff_of_the_dead,
                    objs.staff_of_light,
                    objs.staff_of_balance,
                )

            allowed.forEach {
                assertTrue(deps.autocastWeapons.canStaffAutocast(objTypes[it], autocastId))
            }
            assertFalse(deps.autocastWeapons.canStaffAutocast(objTypes[objs.air_staff], autocastId))
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.prepareCaster(
        staff: org.rsmod.game.type.obj.ObjType,
        slayerLevel: Int = 55,
        casts: Int = 1,
    ) {
        player.stats[stats.magic] = 99
        player.stats[stats.slayer] = slayerLevel
        player.clearInv()
        player.inv[0] = InvObj(objs.mind_rune, 4 * casts)
        player.inv[1] = InvObj(objs.death_rune, casts)
        player.righthand = InvObj(staff)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.assertDartRunes(mind: Int, death: Int) {
        assertEquals(mind, player.count(objs.mind_rune))
        assertEquals(death, player.count(objs.death_rune))
    }

    private fun GameTestState.runMagicDartTest(
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
        val spells: MagicDartSpells,
        val manager: SpellAttackManager,
        val runes: MagicRuneManager,
        val magicSpells: MagicSpellRegistry,
        val autocastWeapons: AutocastWeapons,
        val zoneUpdates: ZoneUpdateMap,
    ) {
        fun attack(weapon: InvObj? = null): AttackFixture {
            val registry = SpellAttackRegistry()
            val repository = SpellAttackRepository(registry)
            with(spells) { with(repository) { register(manager) } }
            val magicSpell = requireNotNull(magicSpells.getObjSpell(objs.spell_magic_dart))
            val handler = requireNotNull(registry[objs.spell_magic_dart])
            return AttackFixture(handler, CombatAttack.Spell(weapon, magicSpell, defensive = false))
        }

        fun projectilesAt(coords: org.rsmod.map.CoordGrid): List<MapProjAnimV2> =
            zoneUpdates[ZoneKey.from(coords)]?.filterIsInstance<MapProjAnimV2>().orEmpty()
    }

    private companion object {
        val targetType =
            npcTypeFactory.create {
                name = "Magic Dart target"
                hitpoints = 100
                defence = 1
                magic = 1
            }
    }
}
