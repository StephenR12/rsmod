package org.rsmod.content.minigames.pestcontrol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class PestControlTest {
    @Test
    fun GameTestState.`novice boat protects shielded portals and pays participating players`() =
        lander(0, DiaryTask.NovicePestControl)

    @Test
    fun GameTestState.`intermediate boat pays four commendations and its own diary`() =
        lander(1, DiaryTask.IntermediatePestControl)

    @Test
    fun GameTestState.`veteran boat pays five commendations and its own diary`() =
        lander(2, DiaryTask.VeteranPestControl)

    private fun GameTestState.lander(tier: Int, task: DiaryTask) =
        runGameTest(PestControl::class, NpcHitScript::class) {
            val plank =
                listOf(CoordGrid(2658, 2639), CoordGrid(2643, 2644), CoordGrid(2637, 2653))[tier]
            val dock = plank.translate(if (tier == 0) -1 else 1, 0)
            val loc = placeMapLoc(plank, locTypes.getValue(listOf(14315, 25631, 25632)[tier]))
            val level = listOf(40, 70, 100)[tier]
            val team = listOf(player) + List(4) { registerPlayer(dock) }
            fun board(p: org.rsmod.game.entity.Player) {
                p.members = true
                p.placeAt(dock)
                p.opLoc1(loc)
                advance()
            }
            player.appearance.combatLevel = level - 1
            board(player)
            assertEquals(dock, player.coords)
            for (p in team.take(4)) {
                p.appearance.combatLevel = level
                board(p)
            }
            advance(205)
            assertTrue(player.coords.x in 2630..2665)
            team.last().appearance.combatLevel = level
            board(team.last())
            advance(201)
            val arrival = player.coords
            assertFalse(arrival.x in 2630..2665)
            val dead = PlayerDeathEvent.ResolveRespawn(team.last())
            eventBus.publish(dead)
            assertEquals(arrival, dead.safeDestination)
            fun portals() =
                npcRepo
                    .findAll(ZoneKey.from(arrival), 8)
                    .filter { it.visType.id in 1739..1754 && it.hitpoints > 0 }
                    .distinct()
                    .toList()
            val expected = if (tier == 0) 200 else 250
            val initial = portals()
            assertEquals(4, initial.size)
            initial.first().queueHit(player, 1, HitType.Melee, 500)
            advance()
            assertEquals(expected, initial.first().hitpoints)
            repeat(4) { round ->
                advance(if (round == 0) 25 else 50)
                val open =
                    portals().filter { it.visType.id in 1739..1742 || it.visType.id in 1747..1750 }
                assertEquals(
                    1,
                    open.size,
                    "round $round cycle ${mapClock.cycle} portals ${portals().map { listOf(it.id,it.visType.id,it.hitpoints) }}",
                )
                val vulnerable = open.first()
                vulnerable.queueHit(player, 1, HitType.Melee, 500)
                advance()
                assertEquals(0, vulnerable.hitpoints, "vulnerable portal round $round")
            }
            assertEquals(dock, player.coords)
            assertEquals(tier + 3, player.vars[cacheTypes.varps.getValue(65609)])
            assertTrue(player.hasCompletedDiaryTask(task))
            assertEquals(level * 10, player.inv.count(objs.coins))
            for (p in team.drop(1)) {
                assertEquals(dock, p.coords)
                assertFalse(p.hasCompletedDiaryTask(task))
                assertEquals(0, p.vars[cacheTypes.varps.getValue(65609)])
            }
        }

    @Test
    fun GameTestState.`Void Knight shop checks level and spends points only after item delivery`() =
        runGameTest(PestRewards::class) {
            player.members = true
            val knight = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(1755))
            val points = cacheTypes.varps.getValue(65609)
            player.setVarp(points, 250)
            player.opNpc1(knight)
            advance()
            val confirm = cacheTypes.components.getValue((243 shl 16) or 6)
            player.ifButton(confirm, 25)
            advance()
            assertEquals(250, player.vars[points])
            for (stat in
                listOf(
                    stats.attack,
                    stats.strength,
                    stats.defence,
                    stats.ranged,
                    stats.magic,
                    stats.hitpoints,
                    stats.prayer,
                )) player.stats[stat] = 42
            player.ifButton(confirm, 25)
            advance()
            assertEquals(0, player.vars[points])
            assertEquals(1, player.inv.count(objTypes.getValue(8839)))
        }
}
