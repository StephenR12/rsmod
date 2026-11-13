package org.rsmod.content.skills.magic.spell.attacks.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.zone.payload.MapProjAnimV2
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.manager.MagicRuneManager.CastResult
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.lefthand
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
import org.rsmod.map.zone.ZoneKey

class IbanBlastSpellAttackTest {
    @Test
    fun GameTestState
        .`Koftik replaces one lost staff and the Dark Mage repairs and upgrades after Underground Pass`() =
        runInjectedGameTest(Dependencies::class, TestModule, IbanStaffServices::class) { d ->
            player.members = true
            fun talk(id: Int, x: Int, z: Int) {
                player.ifClose()
                val c = org.rsmod.map.CoordGrid(x, z)
                player.placeAt(c.translate(1, 0))
                player.opNpc1(npcRepo.findAll(c).single { it.id == id })
                advance()
            }
            fun choose(i: Int) {
                player.resumeActiveCoroutine(
                    org.rsmod.api.player.input.ResumePauseButtonInput(
                        org.rsmod.api.config.refs.components.chatmenu_pbutton,
                        i,
                    )
                )
            }
            val broken = objTypes.getValue(1410)
            talk(8976, 2436, 3315)
            assertEquals(0, player.count(broken))
            player.setVarp(IbanQuest.progress, 11)
            talk(8976, 2436, 3315)
            talk(8976, 2436, 3315)
            assertEquals(1, player.count(broken))
            talk(7753, 2458, 3311)
            choose(1)
            choose(1)
            assertEquals(1, player.count(broken))
            player.inv[5] = InvObj(objs.coins, 400000)
            talk(7753, 2458, 3311)
            choose(1)
            choose(1)
            assertEquals(1, player.count(objs.ibans_staff_u))
            assertEquals(0, player.count(objs.coins))
            assertEquals(2500, d.charges.get(player))
        }

    @Test
    fun GameTestState.`cache contract defines iban blast metadata and presentation`() =
        runIbanBlastTest { deps ->
            val spell = requireNotNull(deps.magicSpells.getObjSpell(objs.spell_iban_blast))
            assertEquals(50, spell.levelReq)
            assertEquals(30.0, spell.castXp)
            // Cache metadata uses the generic restricted-spell sentinel; live damage is 0..25.
            assertEquals(1, spell.maxHit)
            assertEquals(Spellbook.Standard, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.fire_rune to 5, objs.death_rune to 1, objs.ibans_staff to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertNotNull(deps.attack().handler)
            assertEquals(708, seqs.human_castibanblast.id)
            assertEquals(87, spotanims.ibanblast_casting.id)
            assertEquals(88, spotanims.ibanblast_travel.id)
            assertEquals(89, spotanims.ibanblast_impact.id)
            assertEquals(8, projanims.iban_blast.id)
            assertEquals(655, cacheTypes.spotanims[87]?.anim)
            assertEquals(656, cacheTypes.spotanims[88]?.anim)
            assertEquals(657, cacheTypes.spotanims[89]?.anim)
        }

    @Test
    fun GameTestState.`reject missing staff before consuming runes charges or xp`() =
        runIbanBlastTest { deps ->
            prepareCaster(objs.air_staff)
            deps.charges.set(player, 10)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(player.righthand)

            player.withProtectedAccess {
                handler.attack(this, target, attack)
                assertMessageSent("You must wield Iban's Staff to cast this spell.")
            }

            assertIbanRunes(5, 1)
            assertEquals(10, deps.charges.get(player))
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
            assertTrue(deps.projectilesAt(player.coords).isEmpty())
        }

    @Test
    fun GameTestState.`reject exhausted npc and insufficient pvp charges atomically`() =
        runIbanBlastTest { deps ->
            prepareCaster(objs.ibans_staff, casts = 2)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val npc = spawnNpc(player.coords.translate(3, 0), targetType)
            val opponent: Player = registerPlayer(player.coords.translate(0, 3))
            val (handler, attack) = deps.attack(player.righthand)

            deps.charges.set(player, 0)
            player.withProtectedAccess {
                handler.attack(this, npc, attack)
                assertMessageSent("There are no charges left on this staff.")
            }

            deps.charges.set(player, 1)
            player.withProtectedAccess {
                handler.attack(this, opponent, attack)
                assertMessageSent("There are not enough charges left on this staff.")
            }

            assertIbanRunes(10, 2)
            assertEquals(1, deps.charges.get(player))
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
            assertTrue(deps.projectilesAt(player.coords).isEmpty())
        }

    @Test
    fun GameTestState.`npc hit consumes one shared charge and emits dedicated effects`() =
        runIbanBlastTest { deps ->
            prepareCaster(objs.ibans_staff)
            deps.charges.set(player, 2)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(player.righthand)
            val cast = CastResult.Success.Consumed(usedSunfire = false)
            random.next = 0
            random.then = 0

            player.withProtectedAccess {
                assertEquals(0..25, deps.manager.calculateMaxHit(this, target, attack, cast, 25))
                handler.attack(this, target, attack)
            }

            assertIbanRunes(0, 0)
            assertEquals(1, deps.charges.get(player))
            assertEquals(300, player.statMap.getFineXP(stats.magic) - startingXp)
            assertEquals(seqs.human_castibanblast.id, player.pendingSequence.id)
            val launch = EntitySpotanim(player.pendingSpotanims.getLong(0))
            assertEquals(spotanims.ibanblast_casting.id, launch.id)
            assertEquals(92, launch.height)
            val projectile = deps.projectilesAt(player.coords).single()
            assertEquals(spotanims.ibanblast_travel.id, projectile.id)
            val impact = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.ibanblast_impact.id, impact.id)
            assertEquals(124, impact.height)
            assertEquals(projectile.endTime, impact.delay)
        }

    @Test
    fun GameTestState.`pvp cast consumes two shared charges`() = runIbanBlastTest { deps ->
        prepareCaster(objs.ibans_staff_u)
        deps.charges.set(player, 3)
        val target: Player = registerPlayer(player.coords.translate(3, 0))
        val (handler, attack) = deps.attack(player.righthand)
        random.next = 0
        random.then = 0

        player.withProtectedAccess { handler.attack(this, target, attack) }

        assertIbanRunes(0, 0)
        assertEquals(1, deps.charges.get(player))
    }

    @Test
    fun GameTestState.`splash consumes one charge and awards only base cast xp`() =
        runIbanBlastTest { deps ->
            prepareCaster(objs.ibans_staff)
            deps.charges.set(player, 1)
            val startingMagicXp = player.statMap.getFineXP(stats.magic)
            val startingHitpointsXp = player.statMap.getFineXP(stats.hitpoints)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(player.righthand)
            random.next = 9_999

            player.withProtectedAccess { handler.attack(this, target, attack) }

            assertIbanRunes(0, 0)
            assertEquals(0, deps.charges.get(player))
            assertEquals(300, player.statMap.getFineXP(stats.magic) - startingMagicXp)
            assertEquals(0, player.statMap.getFineXP(stats.hitpoints) - startingHitpointsXp)
            val impact = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.failedspell_impact.id, impact.id)
        }

    @Test
    fun GameTestState.`charges are player shared across regular and upgraded staves`() =
        runIbanBlastTest { deps ->
            prepareCaster(objs.ibans_staff, casts = 2)
            deps.charges.set(player, 3)
            val firstTarget = spawnNpc(player.coords.translate(3, 0), targetType)
            val secondTarget = spawnNpc(player.coords.translate(0, 3), targetType)
            var (handler, attack) = deps.attack(player.righthand)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { handler.attack(this, firstTarget, attack) }
            assertEquals(2, deps.charges.get(player))

            advance(ticks = 5)
            player.righthand = InvObj(objs.ibans_staff_u)
            val upgraded = deps.attack(player.righthand)
            handler = upgraded.handler
            attack = upgraded.attack
            random.next = 0
            random.then = 0
            player.withProtectedAccess { handler.attack(this, secondTarget, attack) }

            assertIbanRunes(0, 0)
            assertEquals(1, deps.charges.get(player))
        }

    @Test
    fun GameTestState.`tome of fire substitutes runes without boosting iban blast`() =
        runIbanBlastTest { deps ->
            prepareCaster(objs.ibans_staff, includeFireRunes = false)
            player.lefthand = InvObj(objs.tome_of_fire, vars = 1)
            deps.charges.set(player, 1)
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val (handler, attack) = deps.attack(player.righthand)
            val cast = CastResult.Success.AllUnlimited
            random.next = 0
            random.then = 0

            player.withProtectedAccess {
                assertEquals(0..25, deps.manager.calculateMaxHit(this, target, attack, cast, 25))
                handler.attack(this, target, attack)
            }

            assertIbanRunes(0, 0)
            assertEquals(0, deps.charges.get(player))
            assertEquals(InvObj(objs.tome_of_fire, vars = 1), player.lefthand)
        }

    @Test
    fun GameTestState.`only iban staves can select restricted iban blast autocast`() =
        runIbanBlastTest { deps ->
            val autocastId =
                (0..100).single {
                    deps.magicSpells.getAutocastSpell(it)?.obj == objs.spell_iban_blast
                }

            assertTrue(
                deps.autocastWeapons.canStaffAutocast(objTypes[objs.ibans_staff], autocastId)
            )
            assertTrue(
                deps.autocastWeapons.canStaffAutocast(objTypes[objs.ibans_staff_u], autocastId)
            )
            assertFalse(deps.autocastWeapons.canStaffAutocast(objTypes[objs.air_staff], autocastId))
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.prepareCaster(
        staff: org.rsmod.game.type.obj.ObjType,
        casts: Int = 1,
        includeFireRunes: Boolean = true,
    ) {
        player.stats[stats.magic] = 99
        player.setVarp(IbanQuest.progress, 11)
        player.clearInv()
        if (includeFireRunes) {
            player.inv[0] = InvObj(objs.fire_rune, 5 * casts)
        }
        player.inv[1] = InvObj(objs.death_rune, casts)
        player.righthand = InvObj(staff)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.assertIbanRunes(fire: Int, death: Int) {
        assertEquals(fire, player.count(objs.fire_rune))
        assertEquals(death, player.count(objs.death_rune))
    }

    private fun GameTestState.runIbanBlastTest(
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

    data class AttackFixture(val handler: SpellAttack, val attack: CombatAttack.Spell)

    data class Dependencies
    @Inject
    constructor(
        val spells: IbanBlastSpells,
        val charges: IbanStaffChargeRepository,
        val manager: SpellAttackManager,
        val magicSpells: MagicSpellRegistry,
        val autocastWeapons: AutocastWeapons,
        val zoneUpdates: ZoneUpdateMap,
    ) {
        fun attack(weapon: InvObj? = null): AttackFixture {
            val registry = SpellAttackRegistry()
            val repository = SpellAttackRepository(registry)
            with(spells) { with(repository) { register(manager) } }
            val magicSpell = requireNotNull(magicSpells.getObjSpell(objs.spell_iban_blast))
            val handler = requireNotNull(registry[objs.spell_iban_blast])
            return AttackFixture(handler, CombatAttack.Spell(weapon, magicSpell, defensive = false))
        }

        fun projectilesAt(coords: org.rsmod.map.CoordGrid): List<MapProjAnimV2> =
            zoneUpdates[ZoneKey.from(coords)]?.filterIsInstance<MapProjAnimV2>().orEmpty()
    }

    private companion object {
        val targetType =
            npcTypeFactory.create {
                name = "Iban Blast target"
                hitpoints = 100
                defence = 1
                magic = 1
            }
    }
}
