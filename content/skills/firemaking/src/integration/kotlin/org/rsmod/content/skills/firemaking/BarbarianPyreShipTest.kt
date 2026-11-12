package org.rsmod.content.skills.firemaking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class BarbarianPyreShipTest {
    @Test
    fun GameTestState.`cache 233 pyre ship types recipes axes and rewards are pinned`() =
        runPyreShipTest { deps ->
            assertEquals(3764, varbits.brut_craft_ship.id)
            assertEquals(3767, varbits.brut_prayer_total.id)
            assertEquals(1179, varps.kandarin_achievement_diary2.id)
            assertEquals(5566, npcs.brut_angry_spirit_bad.id)
            assertEquals(5605, npcs.brut_ascending_spirit.id)
            val ferocious = deps.npcTypes[npcs.brut_angry_spirit_bad]
            assertEquals(166, ferocious.vislevel)
            assertEquals(150, ferocious.attack)
            assertEquals(140, ferocious.strength)
            assertEquals(100, ferocious.defence)
            assertEquals(190, ferocious.hitpoints)
            assertEquals(1, ferocious.magic)
            assertEquals(1, ferocious.ranged)
            assertEquals(4, ferocious.param(params.attackrate))
            assertEquals(9, ferocious.param(params.attack_melee))
            assertEquals(15, ferocious.param(params.melee_strength))
            assertEquals(0, ferocious.param(params.defence_stab))
            assertEquals(73, ferocious.param(params.defence_slash))
            assertEquals(72, ferocious.param(params.defence_crush))
            assertEquals(-3, ferocious.param(params.defence_magic))
            assertEquals(30, ferocious.param(params.defence_standard))
            assertEquals(72, ferocious.param(params.defence_light))
            assertEquals(72, ferocious.param(params.defence_heavy))
            assertEquals(categories.attacktype_crush, ferocious.param(params.npc_attack_type))
            assertEquals(
                constants.elemental_weakness_wind,
                ferocious.param(params.elemental_weakness_type),
            )
            assertEquals(10, ferocious.param(params.elemental_weakness_percent))
            assertEquals(6724, seqs.brut_ascending_spirit.id)
            assertEquals(6725, seqs.brut_angry_spirit_appear.id)
            assertEquals(listOf(6733, 6734, 6735, 6736), pyreSequences())
            assertEquals(
                listOf(25286, 25288, 25289, 25290, 25291, 25292, 25293, 25294, 25295),
                pyreLocs(),
            )
            assertEquals("Construct", locTypes[firemaking_locs.barbarianPyreSite].op[0])
            assertEquals(
                listOf(
                    PyreLogContract(1511, 11, 10.0, 40.0, 1),
                    PyreLogContract(2862, 11, 10.0, 40.0, 1),
                    PyreLogContract(1521, 25, 15.0, 60.0, 2),
                    PyreLogContract(1519, 40, 22.5, 90.0, 2),
                    PyreLogContract(6333, 45, 26.2, 105.0, 3),
                    PyreLogContract(10810, 52, 31.2, 125.0, 3),
                    PyreLogContract(1517, 55, 33.7, 135.0, 3),
                    PyreLogContract(6332, 60, 39.3, 157.5, 4),
                    PyreLogContract(1515, 70, 50.6, 202.5, 4),
                    PyreLogContract(1513, 85, 75.9, 303.8, 5),
                    PyreLogContract(19669, 95, 87.0, 350.0, 5),
                ),
                BarbarianPyreLogs.all.map {
                    PyreLogContract(
                        it.log.id,
                        it.level,
                        it.craftingXp,
                        it.firemakingXp,
                        it.prayerCharges,
                    )
                },
            )
            assertEquals(
                listOf(
                    23673,
                    28220,
                    20011,
                    25110,
                    25066,
                    13241,
                    25371,
                    13242,
                    25378,
                    6739,
                    28217,
                    1359,
                    28214,
                    23279,
                    1357,
                    28211,
                    1355,
                    28208,
                    1361,
                    28205,
                    1353,
                    28202,
                    1349,
                    28199,
                    1351,
                    28196,
                ),
                BarbarianPyreAxes.all.map { it.obj.id },
            )
            assertEquals(128, BarbarianPyreRewards.totalWeight)
            assertEquals(22, BarbarianPyreRewards.main.size)
            assertTrue(BarbarianPyreRewards.main.all { deps.objTypes.containsKey(it.obj.id) })
        }

    @Test
    fun GameTestState.`every compatible log consumes exact materials and awards exact xp`() =
        runPyreShipTest { deps ->
            for ((index, recipe) in BarbarianPyreLogs.all.withIndex()) {
                preparePlayer()
                add(objs.rune_axe)
                add(objs.tinderbox)
                add(objs.chewed_bones)
                add(recipe.log)
                random.next = 0
                val site = prepareSite(index)

                player.withProtectedAccess { deps.pyre.construct(this, site) }
                assertStage(site, firemaking_locs.barbarianPyreStage1)
                assertEquals(1, player.count(recipe.log))
                advance(BarbarianPyreShip.CONSTRUCTION_STAGE_TICKS * 4)

                assertEquals(0, player.count(recipe.log))
                assertEquals(0, player.count(objs.chewed_bones))
                assertEquals(1, player.count(objs.rune_axe))
                assertEquals(1, player.count(objs.tinderbox))
                assertEquals((recipe.craftingXp * 10).toInt(), fineXp(stats.crafting))
                assertEquals((recipe.firemakingXp * 10).toInt(), fineXp(stats.firemaking))
                assertEquals(recipe.prayerCharges, player.vars[varbits.brut_prayer_total])
                assertEquals(
                    BarbarianPyreShip.TRAINING_SACRIFICED,
                    player.vars[varbits.brut_craft_ship],
                )
                assertEquals(1, player.count(objs.dragon_full_helm))
                assertTrue(
                    deps.npcRepo.findAll(site.coords).any { it.id == npcs.brut_ascending_spirit.id }
                )
                val expectedDiary =
                    if (recipe.log == objs.magic_logs) {
                        BarbarianPyreShip.KANDARIN_ELITE_PYRE_BIT
                    } else {
                        0
                    }
                assertEquals(expectedDiary, player.vars[varps.kandarin_achievement_diary2])
                advance(60)
            }
        }

    @Test
    fun GameTestState.`suitable bow lights a ship while bruma torch does not`() =
        runPyreShipTest { deps ->
            preparePlayer()
            add(objs.rune_axe)
            add(objs.shortbow)
            add(objs.chewed_bones)
            add(objs.logs)
            random.next = 0
            val bowSite = prepareSite(20)

            player.withProtectedAccess { deps.pyre.construct(this, bowSite) }
            advance(BarbarianPyreShip.CONSTRUCTION_STAGE_TICKS * 4)
            assertEquals(0, player.count(objs.logs))

            advance(60)
            preparePlayer()
            add(objs.rune_axe)
            add(objs.bruma_torch)
            add(objs.chewed_bones)
            add(objs.logs)
            val brumaSite = prepareSite(21)

            player.withProtectedAccess { deps.pyre.construct(this, brumaSite) }
            assertEquals(1, player.count(objs.logs))
            assertMessageSent("You need a tinderbox or a suitable bow to light the pyre ship.")
        }

    @Test
    fun GameTestState.`prayer blessing stacks to two hundred charges`() = runPyreShipTest { deps ->
        preparePlayer()
        player.setVarBit(varbits.brut_prayer_total, 198)
        add(objs.rune_axe)
        add(objs.tinderbox)
        add(objs.chewed_bones)
        add(objs.magic_logs)
        random.next = 0
        val site = prepareSite(30)

        player.withProtectedAccess { deps.pyre.construct(this, site) }
        advance(BarbarianPyreShip.CONSTRUCTION_STAGE_TICKS * 4)

        assertEquals(200, player.vars[varbits.brut_prayer_total])
    }

    @Test
    fun GameTestState.`mangled bones summon one timed ferocious spirit and give no blessing`() =
        runPyreShipTest { deps ->
            preparePlayer()
            player.setVarBit(varbits.brut_craft_ship, BarbarianPyreShip.TRAINING_COMPLETE)
            add(objs.rune_axe)
            add(objs.tinderbox)
            add(objs.mangled_bones, 2)
            add(objs.logs, 2)
            val site = prepareSite(40)

            player.withProtectedAccess { deps.pyre.construct(this, site) }
            advance(BarbarianPyreShip.CONSTRUCTION_STAGE_TICKS * 4)

            assertEquals(1, player.count(objs.mangled_bones))
            assertEquals(1, player.count(objs.logs))
            assertEquals(0, player.vars[varbits.brut_prayer_total])
            assertEquals(BarbarianPyreShip.TRAINING_COMPLETE, player.vars[varbits.brut_craft_ship])
            val spirit =
                deps.npcRepo.findAll(site.coords).single { it.id == npcs.brut_angry_spirit_bad.id }
            assertTrue(spirit.isVisible)

            advance(60)
            player.withProtectedAccess { deps.pyre.construct(this, site) }
            assertEquals(1, player.count(objs.mangled_bones))
            assertEquals(1, player.count(objs.logs))
            assertMessageSent("You must defeat the barbarian spirit first.")

            advance(BarbarianPyreShip.FEROCIOUS_SPIRIT_DURATION_TICKS)
            assertFalse(spirit.isSlotAssigned)
        }

    @Test
    fun GameTestState.`membership training materials and boosted level gates preserve inputs`() =
        runPyreShipTest { deps ->
            val recipe = BarbarianPyreLogs.byLog.getValue(objs.redwood_logs.id)
            val site = prepareSite(50)

            preparePlayer(members = false)
            addFullKit(recipe.log)
            player.withProtectedAccess { deps.pyre.construct(this, site) }
            assertMessageSent("You need to be on a members' world to construct a pyre ship.")
            assertEquals(1, player.count(recipe.log))

            preparePlayer(training = 0)
            addFullKit(recipe.log)
            player.withProtectedAccess { deps.pyre.construct(this, site) }
            assertMessageSent(
                "You must begin the relevant section of Otto's barbarian training first."
            )
            assertEquals(1, player.count(recipe.log))

            preparePlayer(crafting = recipe.level - 1)
            addFullKit(recipe.log)
            player.withProtectedAccess { deps.pyre.construct(this, site) }
            assertMessageSent("You need a Crafting level of 95 to use these logs.")
            assertEquals(1, player.count(recipe.log))

            preparePlayer(firemaking = recipe.level - 1)
            addFullKit(recipe.log)
            player.withProtectedAccess { deps.pyre.construct(this, site) }
            assertMessageSent("You need a Firemaking level of 95 to use these logs.")
            assertEquals(1, player.count(recipe.log))
        }

    @Test
    fun GameTestState.`dragon full helm and every weighted reward boundary are exact`() =
        runPyreShipTest { deps ->
            random.next = 0
            assertEquals(
                RolledBarbarianPyreReward(objs.dragon_full_helm, 1),
                deps.pyre.rollChewedReward(),
            )

            var start = 0
            for (reward in BarbarianPyreRewards.main) {
                random.next = 1
                random.then = start
                val first = deps.pyre.rollChewedReward()
                assertEquals(reward.obj, first.obj)
                assertTrue(first.count in reward.count)

                random.next = 1
                random.then = start + reward.weight - 1
                val last = deps.pyre.rollChewedReward()
                assertEquals(reward.obj, last.obj)
                assertTrue(last.count in reward.count)
                start += reward.weight
            }
            assertEquals(BarbarianPyreRewards.MAIN_ROLL_WEIGHT, start)
        }

    private fun GameTestScope.preparePlayer(
        members: Boolean = true,
        training: Int = BarbarianPyreShip.TRAINING_STARTED,
        crafting: Int = 99,
        firemaking: Int = 99,
    ) {
        player.clearAllInvs()
        player.members = members
        player.stats[stats.crafting] = crafting
        player.stats[stats.firemaking] = firemaking
        player.statMap.setXP(stats.crafting, 0)
        player.statMap.setXP(stats.firemaking, 0)
        player.setVarBit(varbits.brut_craft_ship, training)
        player.setVarBit(varbits.brut_prayer_total, 0)
        player.setVarp(varps.kandarin_achievement_diary2, 0)
        player.weakQueueList.clear()
        player.withProtectedAccess { resetAnim() }
        client.clear()
    }

    private fun GameTestScope.prepareSite(index: Int): BoundLocInfo {
        val site =
            placeMapLoc(
                CoordGrid(1800 + index * 4, 1800, 0),
                firemaking_locs.barbarianPyreSite,
                LocShape.GroundDecor,
                LocAngle.West,
            )
        player.placeAt(site.coords.translateX(-1))
        return site
    }

    private fun GameTestScope.addFullKit(log: ObjType) {
        add(objs.rune_axe)
        add(objs.tinderbox)
        add(objs.chewed_bones)
        add(log)
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].stackable) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj, count)
            return
        }
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestScope.assertStage(
        site: BoundLocInfo,
        stage: org.rsmod.game.type.loc.LocType,
    ) {
        assertNotNull(
            findLocs(site.coords).firstOrNull { it.coords == site.coords && it.id == stage.id }
        )
    }

    private fun GameTestScope.fineXp(stat: org.rsmod.game.type.stat.StatType): Int =
        player.statMap.getFineXP(stat)

    private fun GameTestState.runPyreShipTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            FiremakingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(FiremakingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val pyre: BarbarianPyreShip,
        val locRepo: LocRepository,
        val npcRepo: NpcRepository,
        val npcTypes: NpcTypeList,
        val objTypes: ObjTypeList,
    )

    data class PyreLogContract(
        val log: Int,
        val level: Int,
        val craftingXp: Double,
        val firemakingXp: Double,
        val prayerCharges: Int,
    )

    private fun pyreSequences(): List<Int> =
        listOf(
                seqs.brut_pyre_boat_fire,
                seqs.brut_pyre_boat_launch,
                seqs.brut_pyre_boat_ready,
                seqs.brut_pyre_boat_sink,
            )
            .map { it.id }

    private fun pyreLocs(): List<Int> =
        listOf(
                firemaking_locs.barbarianPyreSite,
                firemaking_locs.barbarianPyreStage1,
                firemaking_locs.barbarianPyreStage2,
                firemaking_locs.barbarianPyreStage3,
                firemaking_locs.barbarianPyreBones,
                firemaking_locs.barbarianPyreFire,
                firemaking_locs.barbarianPyreLaunched,
                firemaking_locs.barbarianPyreReady,
                firemaking_locs.barbarianPyreSink,
            )
            .map { it.id }
}
