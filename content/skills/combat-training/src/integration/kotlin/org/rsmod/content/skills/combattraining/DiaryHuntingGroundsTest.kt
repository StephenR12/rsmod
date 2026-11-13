package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.plugin.NpcDeathScript
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.npc.events.NpcMeleeAlternative
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.hat
import org.rsmod.api.player.stat.stat
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.NpcList
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class DiaryHuntingGroundsTest {
    private class World
    @Inject
    constructor(
        val npcs: NpcList,
        val ground: ObjRepository,
        @org.rsmod.api.random.CoreRandom val deathRandom: org.rsmod.api.random.GameRandom,
    )

    @Test
    fun GameTestState
        .`the Swordchick altar preserves offerings until both are supplied and credits its owner's kill`() =
        runInjectedGameTest(
            World::class,
            null,
            CreatureCreation::class,
            CombatDiaryKills::class,
            NpcDeathScript::class,
            NpcHitScript::class,
        ) { world ->
            player.members = true
            player.observerUUID = player.uuid
            val tile = CoordGrid(3034, 4361)
            player.placeAt(tile.translate(-1, 0))
            val type = locTypes.getValue(21893)
            val loc = placeMapLoc(tile, type)
            val fish = objTypes.getValue(371)
            val chicken = objTypes.getValue(2138)
            player.inv[0] = InvObj(fish)
            fun activate() =
                player.withProtectedAccess { eventBus.publish(this, LocEvents.Op1(loc, loc, type)) }
            activate()
            assertEquals(fish.id, player.inv[0]?.id)
            player.setVarBit(cacheTypes.varbits.getValue(3337), 18)
            player.withProtectedAccess {
                eventBus.publish(this, LocUEvents.Op(loc, loc, type, fish, 0))
            }
            assertNull(player.inv[0])
            activate()
            assertFalse(world.npcs.any { it.id == 3603 })
            player.inv[1] = InvObj(chicken)
            activate()
            advance()
            val creature = world.npcs.single { it.id == 3603 }
            assertNull(player.inv[1])
            player.inv[0] = InvObj(fish)
            player.inv[1] = InvObj(chicken)
            activate()
            assertEquals(chicken.id, player.inv[1]?.id)
            val deathTile = creature.coords
            (world.deathRandom as org.rsmod.api.testing.random.FixedRandom).set(15)
            creature.queueHit(player, 1, HitType.Melee, 200)
            advance(9)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.Swordchick))
            assertTrue(world.ground.findAll(deathTile).any { it.type == 314 && it.count in 10..40 })
        }

    @Test
    fun GameTestState.`throwing a fishing explosive from shore lures a diary eligible mogre`() =
        runInjectedGameTest(
            World::class,
            null,
            MudskipperPoint::class,
            CombatDiaryKills::class,
            NpcDeathScript::class,
            NpcHitScript::class,
        ) { world ->
            player.members = true
            player.observerUUID = player.uuid
            player.stats[stats.slayer] = 32
            val type = locTypes.getValue(10087)
            val loc = placeMapLoc(CoordGrid(2982, 3113), type)
            val bomb = objTypes.getValue(6664)
            player.inv[0] = InvObj(bomb)
            fun lure() =
                player.withProtectedAccess {
                    eventBus.publish(this, LocUEvents.Ap(loc, loc, type, bomb, 0))
                }
            player.placeAt(CoordGrid(2987, 3115))
            lure()
            assertEquals(bomb.id, player.inv[0]?.id)
            player.setVarBit(cacheTypes.varbits.getValue(1344), 3)
            player.placeAt(CoordGrid(2983, 3113))
            lure()
            assertEquals(bomb.id, player.inv[0]?.id)
            player.placeAt(CoordGrid(2987, 3115))
            lure()
            advance()
            assertNull(player.inv[0])
            val mogre = world.npcs.single { it.id == 2592 }
            assertEquals(CoordGrid(2984, 3112), mogre.spawnCoords)
            mogre.queueHit(player, 1, HitType.Melee, 200)
            advance(9)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.MudskipperMogre))
        }

    @Test
    fun GameTestState
        .`desert access and dust protection read the player's saved quest and worn mask`() =
        runGameTest(DesertAndCavernTravel::class, DesertHuntingCombat::class) {
            player.members = true
            val well = locTypes.getValue(6279)
            val entrance = placeMapLoc(CoordGrid(3310, 2962), well)
            val approach = CoordGrid(3311, 2962)
            player.placeAt(approach)
            fun enter() =
                player.withProtectedAccess {
                    eventBus.publish(this, LocEvents.Op1(entrance, entrance, well))
                }
            enter()
            assertEquals(approach, player.coords)
            player.setVarBit(cacheTypes.varbits.getValue(358), 11)
            enter()
            assertEquals(CoordGrid(3206, 9379), player.coords)
            val monster = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(423))
            player.stats[stats.ranged] = 70
            player.stats[stats.defence] = 80
            player.hat = InvObj(objTypes.getValue(4164))
            val masked = NpcMeleeAlternative(monster, player)
            eventBus.publish(masked)
            assertNull(masked.attack)
            assertEquals(70, player.stat(stats.ranged))
            player.hat = null
            val exposed = NpcMeleeAlternative(monster, player)
            eventBus.publish(exposed)
            assertEquals(16, exposed.attack?.damage)
            assertEquals(0, player.stat(stats.ranged))
            assertEquals(40, player.stat(stats.defence))
        }

    @Test
    fun GameTestState
        .`pyre training changes the whirlpool landing and the cavern stairs reach mithril dragons`() =
        runGameTest(DesertAndCavernTravel::class) {
            player.members = true
            fun click(id: Int, tile: CoordGrid) {
                val type = locTypes.getValue(id)
                val loc = placeMapLoc(tile, type)
                player.withProtectedAccess { eventBus.publish(this, LocEvents.Op1(loc, loc, type)) }
            }
            val whirl = CoordGrid(2510, 3506)
            click(25274, whirl)
            assertEquals(CoordGrid(2512, 3476), player.coords)
            player.setVarBit(varbits.brut_craft_ship, 1)
            click(25274, whirl)
            assertEquals(CoordGrid(1767, 5366, 1), player.coords)
            click(25338, CoordGrid(1769, 5365, 1))
            assertEquals(CoordGrid(1771, 5363), player.coords)
            click(25339, CoordGrid(1778, 5344))
            assertEquals(CoordGrid(1778, 5346, 1), player.coords)
        }
}
