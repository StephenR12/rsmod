package org.rsmod.content.minigames.fightpit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.PlayerCombatPermission
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class FightPitTest {
    @Test
    fun GameTestState
        .`two fighters enter through the lobby and only the survivor earns the champion reward`() =
        runGameTest(FightPit::class) {
            val door = placeMapLoc(CoordGrid(2399, 5176), locTypes.getValue(11846))
            val entrance = CoordGrid(2399, 5177)
            val challenger = registerPlayer(entrance)
            val observer = registerPlayer(entrance)
            fun enter(p: Player) {
                p.members = true
                p.stats[stats.hitpoints] = 10
                p.placeAt(entrance)
                p.opLoc1(door)
                advance()
                assertEquals(CoordGrid(2399, 5175), p.coords)
            }
            enter(player)
            advance(55)
            assertEquals(5175, player.coords.z)
            enter(challenger)
            advance(51)
            assertTrue(player.coords.z < 5168)
            assertTrue(challenger.coords.z < 5168)
            fun canAttack(from: Player, to: Player): Boolean? {
                val permission = PlayerCombatPermission(from, to)
                eventBus.publish(permission)
                return permission.allowed
            }
            assertEquals(true, canAttack(player, challenger))
            assertEquals(false, canAttack(observer, challenger))
            val death = PlayerDeathEvent.ResolveRespawn(challenger)
            eventBus.publish(death)
            assertEquals(CoordGrid(2399, 5169), death.safeDestination)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.FightPitChampion))
            assertFalse(challenger.hasCompletedDiaryTask(DiaryTask.FightPitChampion))
            assertEquals(challenger.combatLevel, player.inv.count(objTypes[objs.tokkul]))
            assertEquals(1, player.skullIcon)
            assertEquals(false, canAttack(player, challenger))
            eventBus.publish(death)
            assertEquals(challenger.combatLevel, player.inv.count(objTypes[objs.tokkul]))
            eventBus.publish(PlayerTeleportEvent.Start(player, entrance))
            assertNull(player.skullIcon)
        }
}
