package org.rsmod.content.skills.hunter.birdhouses

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.birdhouses.BirdhouseObjs
import org.rsmod.content.skills.crafting.birdhouses.BirdhouseRecipes
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTransmitLevel
import org.rsmod.game.type.varp.VarpTypeList

@Execution(ExecutionMode.SAME_THREAD)
class BirdhouseTrapTest {
    @Test
    fun GameTestState.`cache 233 sites tiers states seeds and handlers are pinned`() =
        runBirdhouseTest { deps ->
            assertEquals(
                listOf(30565, 30566, 30567, 30568),
                BirdhouseSites.all.map { it.baseLoc.id },
            )
            assertEquals(
                listOf(1626, 1627, 1628, 1629),
                BirdhouseSites.all.map { it.displayVarp.id },
            )
            assertEquals(
                listOf(65536, 65537, 65538, 65539),
                BirdhouseSites.all.map { it.readyAtVarp.id },
            )
            assertEquals(
                listOf(5, 14, 24, 34, 44, 49, 59, 74, 89),
                BirdhouseTiers.all.map { it.hunterLevel },
            )
            assertEquals(
                listOf(280.0, 420.0, 560.0, 700.0, 820.0, 960.0, 1020.0, 1140.0, 1200.0),
                BirdhouseTiers.all.map { it.hunterXp },
            )
            assertEquals(
                listOf(21512, 21515, 21518, 21521, 22192, 22195, 22198, 22201, 22204),
                BirdhouseTiers.all.map { it.obj.id },
            )
            assertEquals(
                listOf(1, 4, 7, 10, 13, 16, 19, 22, 25),
                BirdhouseTiers.all.map { it.builtState },
            )
            assertEquals(
                (30553..30564).toList() + (31827..31841).toList(),
                BirdhouseTiers.all.flatMap {
                    listOf(it.builtLoc.id, it.seededLoc.id, it.activeLoc.id)
                },
            )
            assertEquals(41, BirdhouseSeeds.all.size)
            assertEquals(30, BirdhouseSeeds.all.count { it.units == 1 })
            assertEquals(11, BirdhouseSeeds.all.count { it.units == 2 })
            assertEquals(2433, BirdhouseTrapSynths.buildFill.id)
            assertEquals(8792, BirdhouseTrapObjs.clockwork.id)
            assertEquals(314, BirdhouseTrapObjs.feather.id)
            assertEquals(9978, BirdhouseTrapObjs.rawBirdMeat.id)
            assertEquals(22798, BirdhouseTrapObjs.seedNest.id)
            assertEquals(10, BirdhouseTrap.REQUIRED_SEED_UNITS)
            assertEquals(3000, BirdhouseTrap.MATURATION_SECONDS)

            for (site in BirdhouseSites.all) {
                assertEquals(VarpLifetime.Perm, deps.varps[site.readyAtVarp].scope)
                assertEquals(VarpTransmitLevel.Never, deps.varps[site.readyAtVarp].transmit)
                assertTrue(
                    deps.events.contains(
                        LocEvents.Op1::class.java,
                        BirdhouseTrapLocs.notBuilt.id.toLong(),
                    )
                )
                assertTrue(
                    deps.events.contains(
                        LocUDefaultEvents.OpType::class.java,
                        BirdhouseTrapLocs.notBuilt.id.toLong(),
                    )
                )
            }
            for (tier in BirdhouseTiers.all) {
                for (loc in listOf(tier.builtLoc, tier.seededLoc)) {
                    assertTrue(deps.events.contains(LocEvents.Op2::class.java, loc.id.toLong()))
                    assertTrue(
                        deps.events.contains(LocUDefaultEvents.OpType::class.java, loc.id.toLong())
                    )
                }
                assertTrue(
                    deps.events.contains(LocEvents.Op1::class.java, tier.activeLoc.id.toLong())
                )
                assertTrue(
                    deps.events.contains(LocEvents.Op3::class.java, tier.seededLoc.id.toLong())
                )
                assertTrue(
                    deps.events.contains(LocEvents.Op3::class.java, tier.activeLoc.id.toLong())
                )
                assertTrue(
                    deps.events.contains(LocEvents.Op4::class.java, tier.activeLoc.id.toLong())
                )
            }
        }

    @Test
    fun `seed nest chance uses cache 233 rounded visible-level interpolation`() {
        assertEquals(1, BirdhouseTrap.seedNestChanceNumerator(1))
        assertEquals(9, BirdhouseTrap.seedNestChanceNumerator(5))
        assertEquals(101, BirdhouseTrap.seedNestChanceNumerator(50))
        assertEquals(201, BirdhouseTrap.seedNestChanceNumerator(99))
        assertEquals(244, BirdhouseTrap.seedNestChanceNumerator(120))
        assertEquals(256, BirdhouseTrap.seedNestChanceNumerator(126))
        assertEquals(256, BirdhouseTrap.seedNestChanceNumerator(255))
    }

    @Test
    fun GameTestState.`all nine tiers use boosted Hunter level and exact display states`() =
        runBirdhouseTest { deps ->
            player.members = true
            val locs = BirdhouseSites.all.map { siteLoc(it) }
            for ((index, tier) in BirdhouseTiers.all.withIndex()) {
                val site = BirdhouseSites.all[index % BirdhouseSites.all.size]
                val loc = locs[index % locs.size]
                player.clearInv()
                player.setVarp(site.displayVarp, 0)
                player.setVarp(site.readyAtVarp, 999)
                player.setBaseLevel(stats.hunter, 1)
                player.setCurrentLevel(stats.hunter, tier.hunterLevel)
                player.inv[0] = InvObj(tier.obj)

                player.withProtectedAccess { deps.trap.place(this, loc, tier, 0) }

                assertEquals(0, player.count(tier.obj))
                assertEquals(tier.builtState, player.vars[site.displayVarp])
                assertEquals(0, player.vars[site.readyAtVarp])
            }
            assertEquals(
                BirdhouseTiers.all.size,
                client.filterIsInstance<SynthSound>().count { it.id == 2433 },
            )
        }

    @Test
    fun GameTestState.`level membership stale state and slot failures are atomic`() =
        runBirdhouseTest { deps ->
            val tier = BirdhouseTiers.all.last()
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            player.members = true
            player.inv[0] = InvObj(tier.obj)
            player.setBaseLevel(stats.hunter, tier.hunterLevel - 1)
            player.setCurrentLevel(stats.hunter, tier.hunterLevel - 1)

            player.withProtectedAccess { deps.trap.place(this, loc, tier, 0) }
            assertEquals(1, player.count(tier.obj))
            assertEquals(0, player.vars[site.displayVarp])
            assertMessageSent("You need a Hunter level of 89 to place this bird house.")

            player.setCurrentLevel(stats.hunter, tier.hunterLevel)
            player.members = false
            player.withProtectedAccess { deps.trap.place(this, loc, tier, 0) }
            assertEquals(1, player.count(tier.obj))
            assertEquals(0, player.vars[site.displayVarp])
            assertMessageSent("You need to be on a members' world to place a bird house.")

            player.members = true
            player.setVarp(site.displayVarp, 1)
            player.withProtectedAccess { deps.trap.place(this, loc, tier, 0) }
            player.withProtectedAccess { deps.trap.place(this, loc, tier, -1) }
            player.withProtectedAccess { deps.trap.place(this, loc, tier, player.inv.size) }
            assertEquals(1, player.count(tier.obj))
            assertEquals(1, player.vars[site.displayVarp])
        }

    @Test
    fun GameTestState.`every suitable seed fills with its five or ten seed rule`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all.first()
            clock.now = 10_000
            for (seed in BirdhouseSeeds.all) {
                player.clearInv()
                player.setVarp(site.displayVarp, tier.builtState)
                player.setVarp(site.readyAtVarp, 0)
                player.inv[0] = InvObj(seed.obj, 10)
                client.clear()

                player.withProtectedAccess { deps.trap.fill(this, loc, 0) }

                val consumed = BirdhouseTrap.REQUIRED_SEED_UNITS / seed.units
                assertEquals(10 - consumed, player.count(seed.obj), seed.obj.toString())
                assertEquals(
                    tier.builtState + 2,
                    player.vars[site.displayVarp],
                    seed.obj.toString(),
                )
                assertEquals(13_000, player.vars[site.readyAtVarp], seed.obj.toString())
                assertEquals(2433, client.filterIsInstance<SynthSound>().single().id)
            }
        }

    @Test
    fun GameTestState.`mixed partial loading preserves units then starts durable timer`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all[1]
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all[4]
            val low = BirdhouseSeeds.all.first { it.units == 1 }
            val high = BirdhouseSeeds.all.first { it.units == 2 }
            player.setVarp(site.displayVarp, tier.builtState)
            player.inv[0] = InvObj(low.obj, 4)
            player.inv[1] = InvObj(high.obj, 4)
            clock.now = 20_000

            player.withProtectedAccess { deps.trap.fill(this, loc, 0) }
            assertEquals(tier.builtState + 1, player.vars[site.displayVarp])
            assertEquals(4, player.vars[site.readyAtVarp])
            assertEquals(0, player.count(low.obj))

            player.withProtectedAccess { deps.trap.fill(this, loc, 1) }
            assertEquals(tier.builtState + 2, player.vars[site.displayVarp])
            assertEquals(23_000, player.vars[site.readyAtVarp])
            assertEquals(1, player.count(high.obj))
            clock.now = 22_999
            assertFalse(deps.trap.isReady(player.vars, site))
            clock.now = 23_000
            assertTrue(deps.trap.isReady(player.vars, site))
        }

    @Test
    fun GameTestState.`registered item on location route builds and fills through base multiloc`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all[2]
            val base = siteLoc(site)
            val tier = BirdhouseTiers.all[2]
            val seed = BirdhouseSeeds.all.first { it.units == 2 }
            player.members = true
            player.clearInv()
            player.setVarp(site.displayVarp, 0)
            player.setVarp(site.readyAtVarp, 0)
            player.setBaseLevel(stats.hunter, 1)
            player.setCurrentLevel(stats.hunter, tier.hunterLevel)
            player.inv[0] = InvObj(tier.obj)
            player.inv[1] = InvObj(seed.obj, 6)
            clock.now = 30_000

            useOnSite(deps, base, tier.obj, 0)
            assertEquals(tier.builtState, player.vars[site.displayVarp])

            useOnSite(deps, base, seed.obj, 1)
            assertEquals(tier.builtState + 2, player.vars[site.displayVarp])
            assertEquals(33_000, player.vars[site.readyAtVarp])
            assertEquals(1, player.count(seed.obj))
        }

    @Test
    fun GameTestState.`all nine matured tiers award exact xp and guaranteed rewards`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            clock.now = 100_000
            var expectedGroundMeat = 0
            for (tier in BirdhouseTiers.all) {
                player.clearInv()
                player.setVarp(site.displayVarp, tier.builtState + 2)
                player.setVarp(site.readyAtVarp, clock.now)
                val startXp = player.statMap.getFineXP(stats.hunter)
                random.next = 0 // 30 feathers.
                random.then = 255 // Miss the independently rolled seed nest.

                val harvested = harvest(deps, loc)

                assertTrue(harvested, tier.toString())
                assertEquals(0, player.vars[site.displayVarp], tier.toString())
                assertEquals(0, player.vars[site.readyAtVarp], tier.toString())
                assertEquals(
                    (tier.hunterXp * 10).toInt(),
                    player.statMap.getFineXP(stats.hunter) - startXp,
                    tier.toString(),
                )
                assertEquals(1, player.count(BirdhouseTrapObjs.clockwork), tier.toString())
                assertEquals(30, player.count(BirdhouseTrapObjs.feather), tier.toString())
                assertEquals(0, player.count(BirdhouseTrapObjs.rawBirdMeat), tier.toString())
                expectedGroundMeat += BirdhouseTrap.RAW_BIRD_MEAT_COUNT
                assertEquals(
                    expectedGroundMeat,
                    deps.objRepo
                        .findAll(player.coords)
                        .filter { it.type == BirdhouseTrapObjs.rawBirdMeat.id }
                        .sumOf { it.count },
                    tier.toString(),
                )
            }
        }

    @Test
    fun GameTestState.`seed nest boundary uses visible level captured before harvest xp`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all.last()
            clock.now = 200_000
            player.setBaseLevel(stats.hunter, 98)
            player.setCurrentLevel(stats.hunter, 99)
            player.setVarp(site.displayVarp, tier.builtState + 2)
            player.setVarp(site.readyAtVarp, clock.now)
            random.next = 3 // 60 feathers.
            random.then = 200 // Level 99 succeeds for rolls 0..200.

            assertTrue(harvest(deps, loc))
            assertEquals(60, player.count(BirdhouseTrapObjs.feather))
            assertEquals(1, player.count(BirdhouseTrapObjs.seedNest))

            player.clearInv()
            player.setCurrentLevel(stats.hunter, 99)
            player.setVarp(site.displayVarp, tier.builtState + 2)
            player.setVarp(site.readyAtVarp, clock.now)
            random.next = 0
            random.then = 201 // First failing roll at level 99.

            assertTrue(harvest(deps, loc))
            assertEquals(0, player.count(BirdhouseTrapObjs.seedNest))
        }

    @Test
    fun GameTestState.`harvest always grounds meat and overflow rewards remain private`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all.first()
            clock.now = 300_000
            player.setCurrentLevel(stats.hunter, 99)
            player.setVarp(site.displayVarp, tier.builtState + 2)
            player.setVarp(site.readyAtVarp, clock.now)
            for (slot in player.inv.indices) {
                player.inv[slot] = InvObj(BirdhouseTrapObjs.normal)
            }
            random.next = 2 // 50 feathers.
            random.then = 0 // Seed nest success.

            assertTrue(harvest(deps, loc))

            val floor = deps.objRepo.findAll(player.coords).toList()
            assertEquals(
                10,
                floor.filter { it.type == BirdhouseTrapObjs.rawBirdMeat.id }.sumOf { it.count },
            )
            assertEquals(
                1,
                floor.filter { it.type == BirdhouseTrapObjs.clockwork.id }.sumOf { it.count },
            )
            assertEquals(
                50,
                floor.filter { it.type == BirdhouseTrapObjs.feather.id }.sumOf { it.count },
            )
            assertEquals(
                1,
                floor.filter { it.type == BirdhouseTrapObjs.seedNest.id }.sumOf { it.count },
            )
            assertTrue(floor.all { it.isPrivate && it.isOriginalOwner(player) })
        }

    @Test
    fun GameTestState.`immature or stale states cannot consume readiness or randomness`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all.first()
            clock.now = 400_000
            player.setVarp(site.displayVarp, tier.builtState + 2)
            player.setVarp(site.readyAtVarp, clock.now + 1)
            random.next = 42

            assertFalse(harvest(deps, loc))
            assertEquals(tier.builtState + 2, player.vars[site.displayVarp])
            assertEquals(clock.now + 1, player.vars[site.readyAtVarp])
            assertEquals(42, random.next)
            assertEquals(0, player.statMap.getFineXP(stats.hunter))
            assertTrue(deps.objRepo.findAll(player.coords).none())

            player.setVarp(site.displayVarp, tier.builtState + 1)
            player.setVarp(site.readyAtVarp, clock.now)
            assertFalse(harvest(deps, loc))
            assertEquals(tier.builtState + 1, player.vars[site.displayVarp])
            assertEquals(clock.now, player.vars[site.readyAtVarp])
            assertEquals(42, random.next)
        }

    @Test
    fun GameTestState.`one click build prefers a carried house before raw materials`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all[3]
            val recipe = BirdhouseRecipes.forOutput(tier.obj)!!
            player.placeAt(loc.coords.translateX(-1))
            player.members = true
            player.setCurrentLevel(stats.hunter, 99)
            player.stats[stats.crafting] = 99
            player.inv[0] = InvObj(tier.obj)
            player.inv[1] = InvObj(recipe.logs)
            player.inv[2] = InvObj(BirdhouseObjs.clockwork)
            player.inv[3] = InvObj(objs.chisel)
            player.inv[4] = InvObj(objs.hammer)
            val startCraftingXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.trap.buildBest(this, loc) }

            assertEquals(tier.builtState, player.vars[site.displayVarp])
            assertEquals(0, player.count(tier.obj))
            assertEquals(1, player.count(recipe.logs))
            assertEquals(1, player.count(BirdhouseObjs.clockwork))
            assertEquals(startCraftingXp, player.statMap.getFineXP(stats.crafting))
        }

    @Test
    fun GameTestState.`one click raw build crafts for exact xp then consumes output into patch`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all[4]
            val recipe = BirdhouseRecipes.forOutput(tier.obj)!!
            player.placeAt(loc.coords.translateX(-1))
            player.members = true
            player.setCurrentLevel(stats.hunter, tier.hunterLevel)
            player.stats[stats.crafting] = recipe.level
            player.inv[0] = InvObj(recipe.logs)
            player.inv[1] = InvObj(BirdhouseObjs.clockwork)
            player.inv[2] = InvObj(objs.chisel)
            player.inv[3] = InvObj(objs.hammer)
            val startCraftingXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.trap.buildBest(this, loc) }
            assertEquals(0, player.vars[site.displayVarp])
            advance(BirdhouseTrap.DIRECT_BUILD_TICKS - 1)
            assertEquals(0, player.vars[site.displayVarp])
            advance()

            assertEquals(tier.builtState, player.vars[site.displayVarp])
            assertEquals(0, player.count(recipe.logs))
            assertEquals(0, player.count(BirdhouseObjs.clockwork))
            assertEquals(0, player.count(tier.obj))
            assertEquals(
                (recipe.xp * 10).toInt(),
                player.statMap.getFineXP(stats.crafting) - startCraftingXp,
            )
        }

    @Test
    fun GameTestState.`one click raw build revalidates an unchanged empty site after its delay`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all.first()
            val recipe = BirdhouseRecipes.forOutput(tier.obj)!!
            player.placeAt(loc.coords.translateX(-1))
            player.members = true
            player.setCurrentLevel(stats.hunter, 99)
            player.stats[stats.crafting] = 99
            player.inv[0] = InvObj(recipe.logs)
            player.inv[1] = InvObj(BirdhouseObjs.clockwork)
            player.inv[2] = InvObj(objs.chisel)
            player.inv[3] = InvObj(objs.hammer)
            val startCraftingXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.trap.buildBest(this, loc) }
            advance(BirdhouseTrap.DIRECT_BUILD_TICKS - 1)
            player.setVarp(site.displayVarp, tier.builtState)
            advance()

            assertEquals(tier.builtState, player.vars[site.displayVarp])
            assertEquals(1, player.count(recipe.logs))
            assertEquals(1, player.count(BirdhouseObjs.clockwork))
            assertEquals(startCraftingXp, player.statMap.getFineXP(stats.crafting))
        }

    @Test
    fun GameTestState.`early empty decline preserves the active house`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all.first()
            clock.now = 500_000
            player.setVarp(site.displayVarp, tier.builtState + 2)
            player.setVarp(site.readyAtVarp, clock.now + 1_000)

            player.withProtectedAccess { deps.trap.empty(this, loc) }
            resumeChoice(2)
            assertEquals(tier.builtState + 2, player.vars[site.displayVarp])
            assertEquals(0, player.count(BirdhouseTrapObjs.clockwork))
        }

    @Test
    fun GameTestState.`early empty accept discards the house and returns only clockwork`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all.first()
            clock.now = 500_000
            player.setVarp(site.displayVarp, tier.builtState + 2)
            player.setVarp(site.readyAtVarp, clock.now + 1_000)

            player.withProtectedAccess { deps.trap.empty(this, loc) }
            resumeChoice(1)
            assertEquals(0, player.vars[site.displayVarp])
            assertEquals(0, player.vars[site.readyAtVarp])
            assertEquals(1, player.count(BirdhouseTrapObjs.clockwork))
            assertEquals(0, player.statMap.getFineXP(stats.hunter))
            assertTrue(deps.objRepo.findAll(player.coords).none())
        }

    @Test
    fun GameTestState.`partial dismantle discards shell and seeds but returns clockwork`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val tier = BirdhouseTiers.all[2]
            player.setVarp(site.displayVarp, tier.builtState + 1)
            player.setVarp(site.readyAtVarp, 7)

            player.withProtectedAccess { deps.trap.dismantlePartial(this, loc) }
            resumeChoice(1)

            assertEquals(0, player.vars[site.displayVarp])
            assertEquals(0, player.vars[site.readyAtVarp])
            assertEquals(1, player.count(BirdhouseTrapObjs.clockwork))
            assertEquals(0, player.statMap.getFineXP(stats.hunter))
            assertTrue(deps.objRepo.findAll(player.coords).none())
        }

    @Test
    fun GameTestState.`reset prefers a carried house and returns the harvested clockwork`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val oldTier = BirdhouseTiers.all.first()
            val replacement = BirdhouseTiers.all[3]
            clock.now = 550_000
            player.observerUUID = 1L
            player.placeAt(loc.coords.translateX(-1))
            player.members = true
            player.setCurrentLevel(stats.hunter, 99)
            player.stats[stats.crafting] = 99
            player.setVarp(site.displayVarp, oldTier.builtState + 2)
            player.setVarp(site.readyAtVarp, clock.now)
            player.inv[0] = InvObj(replacement.obj)
            random.next = 0
            random.then = 255
            val startCraftingXp = player.statMap.getFineXP(stats.crafting)
            val startHunterXp = player.statMap.getFineXP(stats.hunter)

            player.withProtectedAccess { deps.trap.reset(this, loc) }
            assertEquals(oldTier.builtState + 2, player.vars[site.displayVarp])
            advance(BirdhouseTrap.PREMADE_RESET_DELAY_TICKS)

            assertEquals(replacement.builtState, player.vars[site.displayVarp])
            assertEquals(0, player.vars[site.readyAtVarp])
            assertEquals(0, player.count(replacement.obj))
            assertEquals(1, player.count(BirdhouseTrapObjs.clockwork))
            assertEquals(startCraftingXp, player.statMap.getFineXP(stats.crafting))
            assertEquals(
                (oldTier.hunterXp * 10).toInt(),
                player.statMap.getFineXP(stats.hunter) - startHunterXp,
            )
        }

    @Test
    fun GameTestState.`raw reset reuses clockwork and is one tick slower than direct craft`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val oldTier = BirdhouseTiers.all.first()
            val replacement = BirdhouseTiers.all[1]
            val recipe = BirdhouseRecipes.forOutput(replacement.obj)!!
            clock.now = 600_000
            player.observerUUID = 1L
            player.placeAt(loc.coords.translateX(-1))
            player.members = true
            player.setCurrentLevel(stats.hunter, 99)
            player.stats[stats.crafting] = 99
            player.setVarp(site.displayVarp, oldTier.builtState + 2)
            player.setVarp(site.readyAtVarp, clock.now)
            player.inv[0] = InvObj(recipe.logs)
            player.inv[1] = InvObj(objs.chisel)
            player.inv[2] = InvObj(objs.hammer)
            random.next = 0
            random.then = 255
            val startCraftingXp = player.statMap.getFineXP(stats.crafting)
            val startHunterXp = player.statMap.getFineXP(stats.hunter)

            player.withProtectedAccess { deps.trap.reset(this, loc) }
            advance(BirdhouseTrap.RAW_RESET_TICKS - 1)
            assertEquals(oldTier.builtState + 2, player.vars[site.displayVarp])
            advance()

            assertEquals(replacement.builtState, player.vars[site.displayVarp])
            assertEquals(0, player.vars[site.readyAtVarp])
            assertEquals(0, player.count(recipe.logs))
            assertEquals(0, player.count(BirdhouseTrapObjs.clockwork))
            assertEquals(
                (recipe.xp * 10).toInt(),
                player.statMap.getFineXP(stats.crafting) - startCraftingXp,
            )
            assertEquals(
                (oldTier.hunterXp * 10).toInt(),
                player.statMap.getFineXP(stats.hunter) - startHunterXp,
            )
        }

    @Test
    fun GameTestState.`raw reset retains site clockwork when the inventory starts full`() =
        runBirdhouseTest { deps ->
            val site = BirdhouseSites.all.first()
            val loc = siteLoc(site)
            val oldTier = BirdhouseTiers.all.first()
            val replacement = BirdhouseTiers.all[1]
            val recipe = BirdhouseRecipes.forOutput(replacement.obj)!!
            clock.now = 650_000
            player.observerUUID = 1L
            player.placeAt(loc.coords.translateX(-1))
            player.members = true
            player.setCurrentLevel(stats.hunter, 99)
            player.stats[stats.crafting] = 99
            player.setVarp(site.displayVarp, oldTier.builtState + 2)
            player.setVarp(site.readyAtVarp, clock.now)
            player.inv[0] = InvObj(recipe.logs)
            player.inv[1] = InvObj(objs.chisel)
            player.inv[2] = InvObj(objs.hammer)
            for (slot in 3 until player.inv.size) {
                player.inv[slot] = InvObj(objs.chisel)
            }
            random.next = 0
            random.then = 255

            player.withProtectedAccess { deps.trap.reset(this, loc) }
            advance(BirdhouseTrap.RAW_RESET_TICKS)

            assertEquals(replacement.builtState, player.vars[site.displayVarp])
            assertEquals(0, player.count(recipe.logs))
            assertEquals(0, player.count(BirdhouseTrapObjs.clockwork))
            assertEquals(
                30,
                deps.objRepo
                    .findAll(player.coords)
                    .filter { it.type == BirdhouseTrapObjs.feather.id }
                    .sumOf { it.count },
            )
        }

    private fun GameTestScope.siteLoc(site: BirdhouseSite): BoundLocInfo =
        placeMapLoc(site.coords, site.baseLoc)

    private fun GameTestScope.harvest(deps: Dependencies, loc: BoundLocInfo): Boolean {
        var result = false
        player.observerUUID = 1L
        player.withProtectedAccess { result = deps.trap.harvest(this, loc) }
        return result
    }

    private fun GameTestScope.useOnSite(
        deps: Dependencies,
        base: BoundLocInfo,
        obj: ObjType,
        slot: Int,
    ) {
        player.withProtectedAccess {
            deps.locU.interactOp(
                this,
                base,
                base,
                requireNotNull(locTypes[base.id]),
                objTypes[obj],
                player.inv,
                slot,
            )
        }
    }

    private fun GameTestScope.resumeChoice(option: Int) {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = option)
        )
    }

    private fun GameTestState.runBirdhouseTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            BirdhouseTrapScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            bind(BirdhouseClock::class.java).toInstance(clock)
            bind(BirdhouseTrap::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val trap: BirdhouseTrap,
        val locU: LocUInteractions,
        val events: EventBus,
        val locs: LocTypeList,
        val objs: ObjTypeList,
        val varps: VarpTypeList,
        val objRepo: ObjRepository,
    )

    private class MutableClock(var now: Int = 0) : BirdhouseClock {
        override fun epochSecond(): Int = now
    }

    private companion object {
        val clock = MutableClock()
    }
}
