package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.LocTDefaultEvents
import org.rsmod.api.player.interact.LocTInteractions
import org.rsmod.api.player.righthand
import org.rsmod.api.registry.loc.LocRegistry
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
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class OrbChargingSpellTest {
    @Test
    fun GameTestState.`cache contracts define all four orb spells and facilities`() =
        runOrbTest { deps ->
            val expectations =
                listOf(
                    SpellExpectation(objs.spell_chargeorb_water, 56, 66.0, 45, 30, 2151),
                    SpellExpectation(objs.spell_chargeorb_earth, 60, 70.0, 49, 30, 2150),
                    SpellExpectation(objs.spell_chargeorb_fire, 63, 73.0, 56, 30, 2153),
                    SpellExpectation(objs.spell_chargeorb_air, 66, 76.0, 59, 30, 2152),
                )

            assertEquals(4, deps.recipes.recipes.size)
            for ((recipe, expected) in deps.recipes.recipes.zip(expectations)) {
                val spell = requireNotNull(deps.spells.getObjSpell(expected.spell))
                assertEquals(expected.level, spell.levelReq)
                assertEquals(expected.xp, spell.castXp)
                assertEquals(Spellbook.Standard, spell.spellbook)
                assertEquals(MagicSpellType.Utility, spell.type)
                assertEquals(expected.component, recipe.component.id and 0xFFFF)
                assertEquals(expected.obelisk, recipe.obelisk.id)
                assertEquals(3, spell.objReqs.size)
                assertEquals(3, spell.objReqs.single { it.obj.id == objs.cosmic_rune.id }.count)
                assertEquals(
                    expected.elementalRunes,
                    spell.objReqs
                        .single {
                            it.obj.id != objs.cosmic_rune.id && it.obj.id != objs.unpowered_orb.id
                        }
                        .count,
                )
                assertEquals(1, spell.objReqs.single { it.obj.id == objs.unpowered_orb.id }.count)
            }

            assertEquals(726, seqs.human_castchargeorb.id)
            assertEquals(149, spotanims.chargewaterorb_casting.id)
            assertEquals(150, spotanims.chargewindorb_casting.id)
            assertEquals(151, spotanims.chargeearthorb_casting.id)
            assertEquals(152, spotanims.chargefireorb_casting.id)
            assertEquals(115, synths.charge_earth_orb.id)
            assertEquals(116, synths.charge_air_orb.id)
            assertEquals(117, synths.charge_fire_orb.id)
            assertEquals(118, synths.charge_water_orb.id)
        }

    @Test
    fun GameTestState.`each matching obelisk charges one orb with exact costs xp and effects`() =
        runOrbTest { deps ->
            for (recipe in deps.recipes.recipes) {
                prepareBase()
                val sourceSlot = addRequirements(deps, recipe.spell).single()
                val target = placeObelisk(recipe)
                val spell = requireNotNull(deps.spells.getObjSpell(recipe.spell))
                val startXp = player.statMap.getFineXP(stats.magic)

                player.withProtectedAccess { deps.charging.castManual(this, recipe.spell, target) }

                assertEquals(recipe.output.id, player.inv[sourceSlot]?.id)
                assertEquals(0, player.count(objs.unpowered_orb))
                assertEquals(
                    (spell.castXp * 10).toInt(),
                    player.statMap.getFineXP(stats.magic) - startXp,
                )
                spell.objReqs
                    .filter { it.obj.id != objs.unpowered_orb.id }
                    .forEach { assertEquals(0, player.count(it.obj), recipe.toString()) }
                assertEquals(seqs.human_castchargeorb.id, player.pendingSequence.id)
                assertTrue(player.pendingSpotanims.isNotEmpty())
                advance(3)
            }
        }

    @Test
    fun GameTestState.`full inventory succeeds by replacing the source slot`() =
        runOrbTest { deps ->
            prepareBase()
            val recipe = deps.recipes[objs.spell_chargeorb_fire]!!
            val sourceSlot = addRequirements(deps, recipe.spell).single()
            for (slot in player.inv.indices) {
                if (player.inv[slot] == null) {
                    player.inv[slot] = InvObj(objs.bronze_axe)
                }
            }
            assertEquals(0, player.inv.freeSpace())
            val target = placeObelisk(recipe)

            player.withProtectedAccess { deps.charging.castManual(this, recipe.spell, target) }

            assertEquals(recipe.output.id, player.inv[sourceSlot]?.id)
        }

    @Test
    fun GameTestState.`wrong obelisk and free world failures are atomic`() = runOrbTest { deps ->
        prepareBase()
        val water = deps.recipes[objs.spell_chargeorb_water]!!
        val earth = deps.recipes[objs.spell_chargeorb_earth]!!
        addRequirements(deps, water.spell)
        val earthTarget = placeObelisk(earth)

        player.withProtectedAccess {
            deps.charging.castManual(this, water.spell, earthTarget)
            assertMessageSent("This spell can only be cast on the Obelisk of Water.")
        }
        assertEquals(1, player.count(objs.unpowered_orb))
        assertEquals(3, player.count(objs.cosmic_rune))

        prepareBase(members = false)
        addRequirements(deps, water.spell)
        val waterTarget = placeObelisk(water)
        player.withProtectedAccess {
            deps.charging.castManual(this, water.spell, waterTarget)
            assertMessageSent("You need to be on a members' world to cast this spell.")
        }
        assertEquals(1, player.count(objs.unpowered_orb))
        assertEquals(3, player.count(objs.cosmic_rune))
    }

    @Test
    fun GameTestState.`level rune and unpowered orb failures consume nothing`() =
        runOrbTest { deps ->
            val recipe = deps.recipes[objs.spell_chargeorb_air]!!

            prepareBase(magicLevel = 65)
            addRequirements(deps, recipe.spell)
            val target = placeObelisk(recipe)
            player.withProtectedAccess {
                deps.charging.castManual(this, recipe.spell, target)
                assertMessageSent("Your Magic level is not high enough for this spell.")
            }
            assertEquals(1, player.count(objs.unpowered_orb))
            assertEquals(3, player.count(objs.cosmic_rune))

            prepareBase()
            addRequirements(deps, recipe.spell)
            val cosmicSlot = player.inv.indexOfFirst { it?.id == objs.cosmic_rune.id }
            player.inv[cosmicSlot] = null
            player.withProtectedAccess {
                deps.charging.castManual(this, recipe.spell, target)
                assertMessageSent("You do not have enough Cosmic Runes to cast this spell.")
            }
            assertEquals(1, player.count(objs.unpowered_orb))

            prepareBase()
            addRequirements(deps, recipe.spell)
            val orbSlot = player.inv.indexOfFirst { it?.id == objs.unpowered_orb.id }
            player.inv[orbSlot] = null
            player.withProtectedAccess {
                deps.charging.castManual(this, recipe.spell, target)
                assertMessageSent("You do not have enough unpowered orbs to cast this spell.")
            }
            assertEquals(3, player.count(objs.cosmic_rune))
        }

    @Test
    fun GameTestState.`elemental staff substitutes for all thirty elemental runes`() =
        runOrbTest { deps ->
            prepareBase()
            val recipe = deps.recipes[objs.spell_chargeorb_air]!!
            player.righthand = InvObj(objs.air_staff)
            player.inv[0] = InvObj(objs.cosmic_rune, 3)
            player.inv[1] = InvObj(objs.unpowered_orb)
            val target = placeObelisk(recipe)

            player.withProtectedAccess { deps.charging.castManual(this, recipe.spell, target) }

            assertEquals(1, player.count(objs.air_orb))
            assertEquals(0, player.count(objs.cosmic_rune))
            assertEquals(objs.air_staff.id, player.righthand?.id)
        }

    @Test
    fun GameTestState.`manual spam queues one cast at the three tick boundary`() =
        runOrbTest { deps ->
            prepareBase()
            val recipe = deps.recipes[objs.spell_chargeorb_water]!!
            addRequirements(deps, recipe.spell, casts = 3)
            val target = placeObelisk(recipe)

            repeat(3) {
                player.withProtectedAccess { deps.charging.castManual(this, recipe.spell, target) }
            }

            assertEquals(1, player.count(recipe.output))
            assertEquals(1, player.queueList.count(orb_charging_queues.manual))
            assertEquals(0, player.queueList.count(orb_charging_queues.auto))

            advance(2)
            assertEquals(1, player.count(recipe.output))
            advance()
            assertEquals(2, player.count(recipe.output))
            assertEquals(1, player.count(objs.unpowered_orb))
            assertEquals(0, player.queueList.count(orb_charging_queues.manual))
            assertEquals(1, player.queueList.count(orb_charging_queues.auto))
        }

    @Test
    fun GameTestState.`automatic charging repeats every six ticks and stops after the last orb`() =
        runOrbTest { deps ->
            prepareBase()
            val recipe = deps.recipes[objs.spell_chargeorb_earth]!!
            addRequirements(deps, recipe.spell, casts = 2)
            val target = placeObelisk(recipe)

            player.withProtectedAccess { deps.charging.castManual(this, recipe.spell, target) }
            advance(5)
            assertEquals(1, player.count(recipe.output))
            advance()

            assertEquals(2, player.count(recipe.output))
            assertEquals(0, player.count(objs.unpowered_orb))
            assertEquals(0, player.queueList.count(orb_charging_queues.auto))
        }

    @Test
    fun GameTestState.`movement combat and obelisk removal each interrupt automatic charging`() =
        runOrbTest { deps ->
            val recipe = deps.recipes[objs.spell_chargeorb_fire]!!

            repeat(3) { interruption ->
                prepareBase()
                addRequirements(deps, recipe.spell, casts = 2)
                val target = placeObelisk(recipe)
                player.withProtectedAccess { deps.charging.castManual(this, recipe.spell, target) }
                when (interruption) {
                    0 -> player.placeAt(player.coords.translateX(1))
                    1 -> player.setVarp(varps.lastcombat, player.currentMapClock)
                    else ->
                        deps.locRegistry.del(LocInfo(target.layer, target.coords, target.entity))
                }

                advance(6)

                assertEquals(1, player.count(recipe.output))
                assertEquals(1, player.count(objs.unpowered_orb))
                assertEquals(0, player.queueList.count(orb_charging_queues.auto))
            }
        }

    @Test
    fun GameTestState.`location target trigger dispatches the selected orb spell`() =
        runOrbTest { deps ->
            prepareBase()
            val recipe = deps.recipes[objs.spell_chargeorb_air]!!
            addRequirements(deps, recipe.spell)
            val target = placeObelisk(recipe)
            val event =
                deps.interactions.opTrigger(
                    player = player,
                    loc = target,
                    objType = null,
                    component = recipe.component,
                    comsub = -1,
                )

            assertNotNull(event)
            assertTrue(event is LocTDefaultEvents.Op)
            player.withProtectedAccess { deps.events.publish(this, requireNotNull(event)) }

            assertEquals(1, player.count(recipe.output))
        }

    @Test
    fun GameTestState.`Fountain substitutes runes but still consumes an orb and grants no xp`() =
        runOrbTest { deps ->
            prepareBase()
            val recipe = deps.recipes[objs.spell_chargeorb_water]!!
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            player.inv[0] = InvObj(objs.unpowered_orb)
            val target = placeObelisk(recipe)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.charging.castManual(this, recipe.spell, target) }

            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.count(objs.unpowered_orb))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.prepareBase(magicLevel: Int = 99, members: Boolean = true) {
        player.stats[stats.magic] = magicLevel
        player.members = members
        player.clearAllInvs()
        player.righthand = null
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarp(varps.lastcombat, -100)
        player.setVarp(varps.lastcombat_pvp, -100)
    }

    private fun GameTestScope.addRequirements(
        deps: Dependencies,
        spellType: ObjType,
        casts: Int = 1,
    ): List<Int> {
        val spell = requireNotNull(deps.spells.getObjSpell(spellType))
        val orbSlots = mutableListOf<Int>()
        var slot = 0
        for (requirement in spell.objReqs) {
            if (requirement.obj.id == objs.unpowered_orb.id) {
                repeat(casts) {
                    player.inv[slot] = InvObj(requirement.obj)
                    orbSlots += slot++
                }
            } else {
                player.inv[slot++] = InvObj(requirement.obj, requirement.count * casts)
            }
        }
        return orbSlots
    }

    private fun GameTestScope.placeObelisk(recipe: OrbChargingRecipe): BoundLocInfo {
        val coords = obeliskCoords.getValue(recipe.obelisk.id)
        player.placeAt(coords.translateX(-1))
        return placeMapLoc(coords, recipe.obelisk)
    }

    private fun GameTestState.runOrbTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            OrbChargingSpellScript::class,
            testBody = body,
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
            bind(UtilitySpellCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val charging: OrbChargingSpell,
        val spells: MagicSpellRegistry,
        val recipes: OrbChargingRecipeRegistry,
        val interactions: LocTInteractions,
        val locRegistry: LocRegistry,
        val events: EventBus,
    )

    private data class SpellExpectation(
        val spell: ObjType,
        val level: Int,
        val xp: Double,
        val component: Int,
        val elementalRunes: Int,
        val obelisk: Int,
    )

    private companion object {
        val obeliskCoords =
            mapOf(
                orb_charging_locs.water.id to CoordGrid(2844, 3422),
                orb_charging_locs.earth.id to CoordGrid(3086, 9932),
                orb_charging_locs.fire.id to CoordGrid(2819, 9828),
                orb_charging_locs.air.id to CoordGrid(3087, 3571),
            )
    }
}
