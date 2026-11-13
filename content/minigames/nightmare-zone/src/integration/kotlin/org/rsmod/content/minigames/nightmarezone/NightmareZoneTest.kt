package org.rsmod.content.minigames.nightmarezone

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.minigames.canEnterNightmareZone
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class NightmareZoneTest {
    @Test
    fun GameTestState
        .`five completed quests unlock an Endurance dream and a real kill banks points on waking`() =
        runGameTest(NightmareZone::class, NpcHitScript::class) {
            player.members = true
            for ((id, end) in listOf(178 to 3, 147 to 6, 111 to 9, 317 to 50)) player.setVarp(
                cacheTypes.varps.getValue(id),
                end,
            )
            assertFalse(player.canEnterNightmareZone())
            player.setVarp(cacheTypes.varps.getValue(226), 7)
            assertTrue(player.canEnterNightmareZone())
            val lobby = CoordGrid(2606, 3115)
            player.placeAt(lobby)
            val coffer = placeMapLoc(lobby.translate(1, 0), locTypes.getValue(26272))
            player.inv[0] = InvObj(objs.coins, 5300)
            player.opLoc1(coffer)
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance()
            player.resumeCountDialog(4500)
            advance()
            assertEquals(4, player.vars[cacheTypes.varbits.getValue(3948)])
            assertEquals(1300, player.inv.count(objs.coins))
            val host = spawnNpc(lobby.translate(0, 1), npcTypes.getValue(1120))
            player.opNpc1(host)
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
            advance()
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(3946)])
            assertEquals(3, player.vars[cacheTypes.varbits.getValue(3948)])
            val vial = placeMapLoc(lobby.translate(-1, 0), locTypes.getValue(26269))
            player.opLoc1(vial)
            advance()
            assertNotEquals(lobby, player.coords)
            val count = npcRepo.findAll(ZoneKey.from(player.coords), 2).first { it.id == 6393 }
            assertEquals(35, count.hitpoints)
            count.queueHit(player, 1, HitType.Melee, 35)
            advance()
            assertEquals(224, player.vars[cacheTypes.varbits.getValue(3949)])
            assertEquals(0, player.vars[cacheTypes.varps.getValue(1060)])
            val death = PlayerDeathEvent.ResolveRespawn(player)
            eventBus.publish(death)
            assertEquals(lobby, death.safeDestination)
            assertEquals(224, player.vars[cacheTypes.varps.getValue(1060)])
            eventBus.publish(death)
            assertEquals(224, player.vars[cacheTypes.varps.getValue(1060)])
        }

    @Test
    fun GameTestState
        .`reward chest exchanges saved points for redirection and imbues an owned salve`() =
        runGameTest(DreamRewards::class) {
            player.members = true
            val chest = placeMapLoc(player.coords.translate(1, 0), locTypes.getValue(26273))
            val points = cacheTypes.varps.getValue(1060)
            player.setVarp(points, 800775)
            player.inv[6] = InvObj(objTypes.getValue(4081))
            player.opLoc1(chest)
            advance(8)
            assertTrue(player.ui.containsModal(cacheTypes.interfaces.getValue(206)))
            val resource = cacheTypes.components.getValue((206 shl 16) or 4)
            val upgrade = cacheTypes.components.getValue((206 shl 16) or 5)
            player.ifButton(resource, 24)
            advance()
            assertEquals(1, player.inv.count(objTypes.getValue(11740)))
            assertEquals(800000, player.vars[points])
            player.ifButton(upgrade, 10)
            advance()
            assertEquals(800000, player.vars[points])
            player.ifButton(upgrade, 12)
            advance()
            assertEquals(12017, player.inv[6]?.id)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ImbueNightmareSalve))
            assertEquals(0, player.vars[points])
        }
}
