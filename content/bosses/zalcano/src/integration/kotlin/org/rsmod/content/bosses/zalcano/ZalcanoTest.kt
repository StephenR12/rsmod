package org.rsmod.content.bosses.zalcano

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.*
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.loc.LocType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class ZalcanoTest {
    internal class Deps
    @Inject
    constructor(val game: Zalcano, val drops: ObjRepository, val rewards: PrisonRewards)

    private val centre = CoordGrid(3031, 6048)

    private fun GameTestScope.enrol(g: Zalcano, p: Player) {
        p.members = true
        p.observerUUID = p.uuid
        p.stats[stats.hitpoints] = 99
        p.stats[stats.mining] = 85
        p.stats[stats.smithing] = 70
        p.stats[stats.runecrafting] = 70
        p.setVarBit(varbits.song_of_the_elves, 194)
        p.placeAt(CoordGrid(3033, 6065))
        p.withProtectedAccess { assertTrue(g.enter(this)) }
    }

    private fun GameTestScope.start(g: Zalcano): Npc {
        val npc = spawnNpc(centre, npcTypes[PrisonNpcs.demon])
        for (tile in rocks) placeMapLoc(tile, PrisonLocs.depleted)
        enrol(g, player)
        g.tick(npc)
        assertNotNull(g.fight)
        return npc
    }

    private fun GameTestScope.atStation(type: LocType, tile: CoordGrid) {
        val station = placeMapLoc(tile, type)
        player.placeAt(tile.translate(1, 0))
        player.withProtectedAccess {
            eventBus.publish(this, LocEvents.Op1(station, station, locTypes[type]))
        }
    }

    private fun GameTestScope.click(p: Player, n: Npc) {
        p.withProtectedAccess { eventBus.publish(this, NpcEvents.Op1(n)) }
    }

    @Test
    fun GameTestState.`the prison checks the quest at the map platform and again at the barrier`() =
        runInjectedGameTest(Deps::class, null, Zalcano::class) { d ->
            player.members = true
            val surface = CoordGrid(3281, 6058)
            atStation(PrisonLocs.platform, surface)
            assertEquals(surface.translate(1, 0), player.coords)
            player.setVarBit(varbits.song_of_the_elves, 194)
            atStation(PrisonLocs.platform, surface)
            assertEquals(CoordGrid(3033, 6066), player.coords)
            player.setVarBit(varbits.song_of_the_elves, 193)
            player.withProtectedAccess { assertFalse(d.game.enter(this)) }
            player.setVarBit(varbits.song_of_the_elves, 194)
            atStation(PrisonLocs.gate, CoordGrid(3033, 6062))
            assertEquals(CoordGrid(3033, 6060), player.coords)
            player.inv[0] = InvObj(PrisonObjs.imbued, 8)
            player.placeAt(CoordGrid(3033, 6060))
            val gate = placeMapLoc(CoordGrid(3033, 6062), PrisonLocs.gate)
            player.withProtectedAccess {
                eventBus.publish(this, LocEvents.Op1(gate, gate, locTypes[PrisonLocs.gate]))
            }
            assertNull(player.inv[0])
            assertEquals(CoordGrid(3033, 6066), player.coords)
            val guide = spawnNpc(player.coords.translate(1, 0), npcTypes[PrisonNpcs.guide])
            click(player, guide)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            assertEquals(1, player.vars[PrisonBits.xp])
            advance(2)
        }

    @Test
    fun GameTestState
        .`the glowing formation refinery and altar yield usable ammunition and the three skill awards`() =
        runInjectedGameTest(Deps::class, null, Zalcano::class) { d ->
            val demon = start(d.game)
            val f = d.game.fight!!
            val tile = rocks[f.glowing]
            val mining = player.statMap.getFineXP(stats.mining)
            val smith = player.statMap.getFineXP(stats.smithing)
            val rc = player.statMap.getFineXP(stats.runecrafting)
            atStation(PrisonLocs.glowing, tile)
            assertFalse(PrisonObjs.ore in player.inv)
            player.righthand = InvObj(objTypes.getValue(1275))
            random.next = 0
            random.then = 64
            atStation(PrisonLocs.glowing, tile)
            assertTrue(PrisonObjs.ore in player.inv)
            assertEquals(mining + 350, player.statMap.getFineXP(stats.mining))
            atStation(PrisonLocs.furnace, CoordGrid(3046, 6048))
            assertFalse(PrisonObjs.refined in player.inv)
            mapClock.cycle += 3
            atStation(PrisonLocs.furnace, CoordGrid(3046, 6048))
            assertTrue(PrisonObjs.refined in player.inv)
            assertEquals(smith + 100, player.statMap.getFineXP(stats.smithing))
            mapClock.cycle++
            atStation(PrisonLocs.altar, CoordGrid(3018, 6048))
            assertTrue(PrisonObjs.imbued in player.inv)
            assertEquals(rc + 50, player.statMap.getFineXP(stats.runecrafting))
            mapClock.cycle++
            player.placeAt(centre.translate(-3, 0))
            random.next = 15
            click(player, demon)
            assertFalse(PrisonObjs.imbued in player.inv)
            assertEquals(285, f.armour)
            assertEquals(15, f.credits[player]?.armour)
            player.inv[0] = InvObj(PrisonObjs.imbued)
            click(player, demon)
            assertTrue(PrisonObjs.imbued in player.inv)
            assertEquals(285, f.armour)
        }

    @Test
    fun GameTestState
        .`armour break exposes the demon for mining and recovery destroys the remaining ore`() =
        runInjectedGameTest(Deps::class, null, Zalcano::class) { d ->
            val demon = start(d.game)
            val f = d.game.fight!!
            f.armour = 8
            demon.hitpoints = 8
            player.placeAt(centre.translate(-1, 0))
            player.righthand = InvObj(PrisonObjs.imbued, 2)
            random.next = 15
            random.then = 20
            click(player, demon)
            assertEquals(0, f.armour)
            assertEquals(PrisonNpcs.fallen.id, demon.visType.id)
            assertEquals(8, f.credits[player]?.armour)
            assertEquals(1, player.righthand?.count)
            val hp = f.health
            mapClock.cycle += 3
            click(player, demon)
            assertEquals(hp, f.health)
            player.inv[0] = InvObj(objTypes.getValue(1275))
            val xp = player.statMap.getFineXP(stats.mining)
            random.next = 31
            click(player, demon)
            assertEquals(hp - 31, f.health)
            assertEquals(31, f.credits[player]?.health)
            assertEquals(xp + 352, player.statMap.getFineXP(stats.mining))
            player.inv[1] = InvObj(PrisonObjs.ore)
            player.inv[2] = InvObj(PrisonObjs.refined)
            mapClock.cycle = f.weakUntil
            d.game.tick(demon)
            assertEquals(300, f.armour)
            assertEquals(PrisonNpcs.demon.id, demon.visType.id)
            assertNull(player.righthand)
            assertNull(player.inv[1])
            assertNull(player.inv[2])
            assertNotNull(player.inv[0])
        }

    @Test
    fun GameTestState
        .`symbol bonuses and final mining hit award both qualifying participants at their own positions`() =
        runInjectedGameTest(Deps::class, null, Zalcano::class) { d ->
            val demon = start(d.game)
            val f = d.game.fight!!
            val second = registerPlayer()
            enrol(d.game, second)
            f.attack = 1
            f.nextAttack = 0
            random.next = 0
            d.game.tick(demon)
            val blue = f.signs.first { it.id == PrisonLocs.blue.id }.coords
            player.placeAt(blue)
            player.inv[0] = InvObj(PrisonObjs.imbued)
            random.next = 22
            click(player, demon)
            assertEquals(0..22, random.lastInclusiveRange)
            assertEquals(22, f.credits[player]?.armour)
            second.placeAt(centre.translate(-1, 1))
            second.inv[0] = InvObj(PrisonObjs.imbued)
            f.armour = 5
            demon.hitpoints = 5
            random.next = 10
            random.then = 20
            click(second, demon)
            assertEquals(5, f.credits[second]?.armour)
            second.setVarBit(PrisonBits.xp, 1)
            player.placeAt(centre.translate(-1, 0))
            player.righthand = InvObj(objTypes.getValue(1275))
            mapClock.cycle += 3
            f.health = 20
            demon.hitpoints = 20
            random.next = 25
            random.then = 0
            random.then = 7
            random.then = 7
            random.then = 7
            click(player, demon)
            assertNull(d.game.fight)
            assertTrue(
                d.drops.findAll(player.coords).any {
                    it.type == objs.crystal_shard.id && it.count == 3
                }
            )
            assertTrue(d.drops.findAll(player.coords).any { it.type == PrisonObjs.ashes.id })
            assertTrue(
                d.drops.findAll(second.coords).any {
                    it.type == objs.crystal_shard.id && it.count == 1
                }
            )
            assertTrue(second.statMap.getFineXP(stats.mining) > 0)
        }

    @Test
    fun GameTestState
        .`ordinary hits cannot bypass the shield and a player can dodge the telegraphed rockfall`() =
        runInjectedGameTest(
            Deps::class,
            null,
            Zalcano::class,
            NpcHitScript::class,
            PlayerHitScript::class,
        ) { d ->
            val demon = start(d.game)
            val f = d.game.fight!!
            player.placeAt(centre.translate(-3, -1))
            demon.queueHit(player, 1, HitType.Melee, 99)
            advance(2)
            assertEquals(300, demon.hitpoints)
            assertEquals(300, f.armour)
            f.attack = 0
            f.nextAttack = 0
            d.game.tick(demon)
            assertTrue(player.coords in f.falling)
            val hp = player.hitpoints
            player.placeAt(player.coords.translate(-1, 0))
            advance(5)
            assertEquals(hp, player.hitpoints)
            assertTrue(f.falling.isEmpty())
        }

    @Test
    fun GameTestState
        .`contribution thresholds exclude passengers and the weighted unique table awards exactly one seed`() =
        runInjectedGameTest(Deps::class) { d ->
            val second = registerPlayer()
            val third = registerPlayer()
            for ((i, p) in listOf(player, second, third).withIndex()) {
                p.placeAt(centre.translate(-i - 1, 0))
                p.observerUUID = p.uuid
                p.setVarBit(PrisonBits.xp, 1)
            }
            val credits =
                linkedMapOf(
                    player to Contribution(300, 399),
                    second to Contribution(5, 26),
                    third to Contribution(4, 800),
                )
            random.next = 3
            random.then = 3
            random.then = 3
            random.then = 3
            random.then = 0
            random.then = 999
            random.then = 1
            d.rewards.distribute(credits)
            assertTrue(d.drops.findAll(second.coords).any { it.type == PrisonObjs.seed.id })
            assertFalse(d.drops.findAll(player.coords).any { it.type == PrisonObjs.seed.id })
            assertTrue(d.drops.findAll(third.coords).none())
        }

    @Test
    fun GameTestState
        .`Smolcano is insured and follows while the cosmetic shard can be removed without loss`() =
        runInjectedGameTest(Deps::class, null, Zalcano::class) { d ->
            player.members = true
            player.observerUUID = player.uuid
            player.placeAt(centre.translate(-1, 0))
            player.setVarBit(PrisonBits.xp, 1)
            random.next = 0
            random.then = 0
            random.then = 50
            d.rewards.distribute(mapOf(player to Contribution(30, 1)))
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(8440)])
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(3433)])
            assertTrue(
                npcRepo.findAll(org.rsmod.map.zone.ZoneKey.from(player.coords)).any {
                    it.visType.id == 8739
                }
            )
            assertEquals(1, player.vars[PrisonVars.kills])
            assertTrue(d.drops.findAll(player.coords).any { it.type == PrisonObjs.shard.id })
            player.inv[0] = InvObj(PrisonObjs.shard)
            player.inv[1] = InvObj(objs.dragon_pickaxe)
            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    org.rsmod.api.player.events.interact.HeldUEvents.Type(
                        objTypes[PrisonObjs.shard],
                        0,
                        objTypes[objs.dragon_pickaxe],
                        1,
                    ),
                )
            }
            val slot = player.inv.indexOfFirst { it?.id == PrisonObjs.pick.id }
            assertTrue(slot >= 0)
            for (i in player.inv.indices) if (player.inv[i] == null)
                player.inv[i] = InvObj(objs.hammer)
            player.withProtectedAccess { opHeld5(slot) }
            assertTrue(PrisonObjs.pick in player.inv)
            player.inv[27] = null
            player.withProtectedAccess { opHeld5(slot) }
            assertTrue(objs.dragon_pickaxe in player.inv)
            assertTrue(PrisonObjs.shard in player.inv)
        }
}
