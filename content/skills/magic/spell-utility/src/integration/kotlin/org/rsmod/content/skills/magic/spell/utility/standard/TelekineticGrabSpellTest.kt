package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.zone.payload.MapProjAnimV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.obj.plugin.ObjTakeModule
import org.rsmod.api.player.interact.ObjTInteractionPolicyRegistry
import org.rsmod.api.player.interact.ObjTInteractions
import org.rsmod.api.player.righthand
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.MagicSpellRegistry
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
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.skillcapes.SkillcapeModule
import org.rsmod.game.interact.InteractionObjT
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class TelekineticGrabSpellTest {
    @Test
    fun GameTestState.`cache contracts define Telekinetic Grab and its presentation`() =
        runTelegrabTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_tele_grab))

            assertEquals(33, spell.levelReq)
            assertEquals(43.0, spell.castXp)
            assertEquals(Spellbook.Standard, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.air_rune to 1, objs.law_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(27, components.magic_spellbook_telegrab.id and 0xFFFF)
            assertEquals(723, seqs.human_casttelegrab.id)
            assertEquals(681, seqs.telegrab_casting.id)
            assertEquals(682, seqs.telegrab_travel.id)
            assertEquals(683, seqs.telegrab_impact.id)
            assertEquals(142, spotanims.telegrab_casting.id)
            assertEquals(143, spotanims.telegrab_travel.id)
            assertEquals(144, spotanims.telegrab_impact.id)
            assertEquals(192, synths.telegrab_all.id)
        }

    @Test
    fun GameTestState.`whole stack arrives two ticks after a successful cast`() =
        runTelegrabTest { deps ->
            prepareBase()
            val target = deps.spawn(objs.coins, count = 42)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.telegrab.cast(this, target) }

            assertEquals(0, player.count(objs.coins))
            assertEquals(0, player.count(objs.air_rune))
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(430, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.human_casttelegrab.id, player.pendingSequence.id)
            assertTrue(player.pendingSpotanims.isNotEmpty())
            assertTrue(target in deps.objRegistry.findAll(target.coords))

            advance()
            assertEquals(0, player.count(objs.coins))
            advance()

            assertEquals(42, player.count(objs.coins))
            assertFalse(target in deps.objRegistry.findAll(target.coords))
        }

    @Test
    fun GameTestState.`Telekinetic Grab preserves an untrimmed cape after second mastery`() =
        runTelegrabTest { deps ->
            prepareBase()
            player.stats[stats.attack] = 99
            player.stats[stats.hunter] = 99
            val target = deps.spawn(objs.hunter_skillcape)

            player.withProtectedAccess { deps.telegrab.cast(this, target) }
            advance(2)

            assertEquals(1, player.count(objs.hunter_skillcape))
            assertEquals(0, player.count(objs.hunter_skillcape_t))
        }

    @Test
    fun GameTestState.`air staff substitutes without changing the law rune cost`() =
        runTelegrabTest { deps ->
            prepareBase(useAirStaff = true)
            val target = deps.spawn(objs.bronze_axe)

            player.withProtectedAccess { deps.telegrab.cast(this, target) }
            advance(2)

            assertEquals(1, player.count(objs.bronze_axe))
            assertEquals(0, player.count(objs.air_rune))
            assertEquals(0, player.count(objs.law_rune))
        }

    @Test
    fun GameTestState.`full inventory failure is atomic`() = runTelegrabTest { deps ->
        player.stats[stats.magic] = 99
        player.clearInv()
        player.inv[0] = InvObj(objs.air_rune)
        player.inv[1] = InvObj(objs.law_rune)
        for (slot in 2 until player.inv.size) {
            player.inv[slot] = InvObj(objs.bronze_axe)
        }
        val target = deps.spawn(objs.bronze_axe)
        val startXp = player.statMap.getFineXP(stats.magic)

        player.withProtectedAccess {
            deps.telegrab.cast(this, target)
            assertMessageSent(Constants.dm_take_invspace)
        }

        assertEquals(1, player.count(objs.air_rune))
        assertEquals(1, player.count(objs.law_rune))
        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        assertTrue(target in deps.objRegistry.findAll(target.coords))
    }

    @Test
    fun GameTestState.`a target taken during travel reports too late without duplicating it`() =
        runTelegrabTest { deps ->
            prepareBase()
            val target = deps.spawn(objs.bronze_axe)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.telegrab.cast(this, target) }
            assertTrue(deps.objRepo.del(target))
            advance(2)

            player.withProtectedAccess { assertMessageSent(Constants.dm_take_taken) }
            assertEquals(0, player.count(objs.bronze_axe))
            assertEquals(430, player.statMap.getFineXP(stats.magic) - startXp)
        }

    @Test
    fun GameTestState.`changing planes during travel fizzles the grab`() = runTelegrabTest { deps ->
        prepareBase()
        val target = deps.spawn(objs.bronze_axe)

        player.withProtectedAccess { deps.telegrab.cast(this, target) }
        player.placeAt(player.coords.translateLevel(1))
        advance(2)

        player.withProtectedAccess {
            assertMessageSent("Your telegrab fizzles as you move too far away.")
        }
        assertEquals(0, player.count(objs.bronze_axe))
        assertTrue(target in deps.objRegistry.findAll(target.coords))
    }

    @Test
    fun GameTestState.`documented targets are rejected before rune or xp use`() =
        runTelegrabTest { deps ->
            assertEquals(55, excludedTargets.size)
            for (excluded in excludedTargets) {
                prepareBase()
                val target = deps.spawn(excluded)
                val startXp = player.statMap.getFineXP(stats.magic)

                player.withProtectedAccess {
                    deps.telegrab.cast(this, target)
                    assertMessageSent("I can't use Telekinetic Grab on this object.")
                }

                assertEquals(1, player.count(objs.air_rune), excluded.toString())
                assertEquals(1, player.count(objs.law_rune), excluded.toString())
                assertEquals(startXp, player.statMap.getFineXP(stats.magic), excluded.toString())
                assertTrue(deps.objRepo.del(target))
            }
        }

    @Test
    fun GameTestState.`Fountain of Rune consumes no runes and grants no xp`() =
        runTelegrabTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val target = deps.spawn(objs.bronze_axe)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.telegrab.cast(this, target) }
            advance(2)

            assertEquals(1, player.count(objs.bronze_axe))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`one extra grab queues during the three tick cast rate`() =
        runTelegrabTest { deps ->
            prepareBase(airRunes = 3, lawRunes = 3)
            val first = deps.spawn(objs.bronze_axe, xOffset = 1)
            val second = deps.spawn(objs.bronze_axe, xOffset = 2)
            val ignored = deps.spawn(objs.bronze_axe, xOffset = 3)

            player.withProtectedAccess { deps.telegrab.cast(this, first) }
            player.withProtectedAccess { deps.telegrab.cast(this, second) }
            player.withProtectedAccess { deps.telegrab.cast(this, ignored) }

            assertEquals(1, player.queueList.count(telekinetic_grab_queues.cast))
            advance(5)

            assertEquals(2, player.count(objs.bronze_axe))
            assertTrue(ignored in deps.objRegistry.findAll(ignored.coords))
            assertEquals(0, player.queueList.count(telekinetic_grab_queues.cast))
        }

    @Test
    fun GameTestState.`targeting policy permits ten tiles without line of sight`() =
        runTelegrabTest { deps ->
            val policy = deps.policies[components.magic_spellbook_telegrab]
            assertEquals(10, policy.range)
            assertFalse(policy.checkLineOfSight)
        }

    @Test
    fun GameTestState.`ground item target event dispatches the spell handler`() =
        runTelegrabTest { deps ->
            prepareBase()
            val target = deps.spawn(objs.bronze_axe, xOffset = 10)

            deps.interactions.interact(
                player,
                target,
                components.magic_spellbook_telegrab,
                comsub = -1,
                objType = null,
            )
            val interaction = assertInstanceOf(InteractionObjT::class.java, player.interaction)
            assertEquals(10, interaction.apRange)
            assertFalse(interaction.apCheckLineOfSight)

            advance()

            assertEquals(0, player.count(objs.air_rune))
            assertEquals(0, player.count(objs.law_rune))
            advance(2)
            assertEquals(1, player.count(objs.bronze_axe))
        }

    @Test
    fun GameTestState.`projectile timing remains fixed at near and far range`() =
        runTelegrabTest { deps ->
            prepareBase(airRunes = 2, lawRunes = 2)
            val near = deps.spawn(objs.coins, xOffset = 1)
            deps.zoneUpdates.clear()

            player.withProtectedAccess { deps.telegrab.cast(this, near) }
            val nearProjectile = deps.singleProjectile(ZoneKey.from(player.coords))
            assertEquals(35, nearProjectile.startTime)
            assertEquals(45, nearProjectile.endTime)

            advance(3)
            val far = deps.spawn(objs.coins, xOffset = 10)
            deps.zoneUpdates.clear()
            player.withProtectedAccess { deps.telegrab.cast(this, far) }
            val farProjectile = deps.singleProjectile(ZoneKey.from(player.coords))

            assertEquals(35, farProjectile.startTime)
            assertEquals(45, farProjectile.endTime)
        }

    private fun GameTestScope.prepareBase(
        airRunes: Int = 1,
        lawRunes: Int = 1,
        useAirStaff: Boolean = false,
    ) {
        player.stats[stats.magic] = 99
        player.clearInv()
        player.righthand = if (useAirStaff) InvObj(objs.air_staff) else null
        if (!useAirStaff) {
            player.inv[0] = InvObj(objs.air_rune, airRunes)
        }
        player.inv[1] = InvObj(objs.law_rune, lawRunes)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
    }

    private fun GameTestState.runTelegrabTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            TelekineticGrabSpellScript::class,
            testBody = { deps ->
                deps.testPlayerCoords = player.coords
                body(deps)
            },
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(ObjTakeModule())
            install(SkillcapeModule())
            install(MagicSpellModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UtilitySpellCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val telegrab: TelekineticGrabSpell,
        val spells: MagicSpellRegistry,
        val objRepo: ObjRepository,
        val objRegistry: ObjRegistry,
        val interactions: ObjTInteractions,
        val policies: ObjTInteractionPolicyRegistry,
        val zoneUpdates: ZoneUpdateMap,
    ) {
        fun spawn(type: ObjType, count: Int = 1, xOffset: Int = 1): Obj =
            objRepo.add(
                type = type,
                coords = checkNotNull(testPlayerCoords).translateX(xOffset),
                duration = Int.MAX_VALUE,
                count = count,
            )

        var testPlayerCoords: org.rsmod.map.CoordGrid? = null

        fun singleProjectile(zone: ZoneKey): MapProjAnimV2 =
            requireNotNull(zoneUpdates[zone]).filterIsInstance<MapProjAnimV2>().single()
    }

    private companion object {
        val excludedTargets =
            listOf(
                objs.telegrab_ahabs_beer,
                objs.telegrab_raw_pheasant,
                objs.telegrab_blue_flowers,
                objs.telegrab_red_flowers,
                objs.telegrab_dragon_token,
                objs.looting_bag,
                objs.looting_bag_open,
                objs.diary_karamja_gloves_1,
                objs.diary_karamja_gloves_2,
                objs.diary_karamja_gloves_3,
                objs.diary_karamja_gloves_4,
                objs.diary_varrock_armour_1,
                objs.diary_varrock_armour_2,
                objs.diary_varrock_armour_3,
                objs.diary_varrock_armour_4,
                objs.diary_wilderness_sword_1,
                objs.diary_wilderness_sword_2,
                objs.diary_wilderness_sword_3,
                objs.diary_wilderness_sword_4,
                objs.diary_morytania_legs_1,
                objs.diary_morytania_legs_2,
                objs.diary_morytania_legs_3,
                objs.diary_morytania_legs_4,
                objs.diary_falador_shield_1,
                objs.diary_falador_shield_2,
                objs.diary_falador_shield_3,
                objs.diary_falador_shield_4,
                objs.diary_ardougne_cloak_1,
                objs.diary_ardougne_cloak_2,
                objs.diary_ardougne_cloak_3,
                objs.diary_ardougne_cloak_4,
                objs.diary_explorers_ring_1,
                objs.diary_explorers_ring_2,
                objs.diary_explorers_ring_3,
                objs.diary_explorers_ring_4,
                objs.diary_fremennik_boots_1,
                objs.diary_fremennik_boots_2,
                objs.diary_fremennik_boots_3,
                objs.diary_fremennik_boots_4,
                objs.diary_desert_amulet_1,
                objs.diary_desert_amulet_2,
                objs.diary_desert_amulet_3,
                objs.diary_desert_amulet_4,
                objs.diary_kandarin_headgear_1,
                objs.diary_kandarin_headgear_2,
                objs.diary_kandarin_headgear_3,
                objs.diary_kandarin_headgear_4,
                objs.diary_western_banner_1,
                objs.diary_western_banner_2,
                objs.diary_western_banner_3,
                objs.diary_western_banner_4,
                objs.diary_radas_blessing_1,
                objs.diary_radas_blessing_2,
                objs.diary_radas_blessing_3,
                objs.diary_radas_blessing_4,
            )
    }
}
