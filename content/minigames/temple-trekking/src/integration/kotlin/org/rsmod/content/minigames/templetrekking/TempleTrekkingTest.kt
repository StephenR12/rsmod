package org.rsmod.content.minigames.templetrekking

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class TempleTrekkingTest {
    @Test
    fun GameTestState
        .`a hard escort survives three encounters and exchanges a scored native token`() =
        runGameTest(TempleTrekking::class, TrekRewards::class, NpcHitScript::class) {
            val burgh = CoordGrid(3480, 3237)
            player.placeAt(burgh)
            player.members = true
            player.observerUUID = player.uuid
            val hard = spawnNpc(burgh.translate(1, 0), npcTypes.getValue(1562))
            fun choose() {
                player.withProtectedAccess { opNpc3(hard) }
                advance()
            }
            val routes = cacheTypes.interfaces.getValue(329)
            choose()
            assertFalse(player.ui.containsModal(routes))
            player.setVarBit(cacheTypes.varbits.getValue(1990), 430)
            player.setVarBit(cacheTypes.varbits.getValue(3433), 1)
            choose()
            assertFalse(player.ui.containsModal(routes))
            player.setVarBit(cacheTypes.varbits.getValue(3433), 0)
            choose()
            assertTrue(player.ui.containsModal(routes))
            random.next = 1
            random.then = 5
            random.then = 1
            random.then = 0
            player.ifButton(cacheTypes.components.getValue((329 shl 16) or 25))
            advance()
            val spawn = player.coords
            assertNotEquals(burgh, spawn)
            fun nearest(id: Int) =
                npcRepo
                    .findAll(ZoneKey.from(player.coords), 7)
                    .filter { it.id == id && it.hitpoints > 0 }
                    .distinct()
                    .toList()
            val beasts = nearest(2947)
            assertEquals(5, beasts.size)
            assertEquals(65, beasts.first().hitpoints)
            fun path(id: Int, c: CoordGrid = player.coords) {
                val type = locTypes.getValue(id)
                val loc = placeMapLoc(c, type)
                player.withProtectedAccess { eventBus.publish(this, LocEvents.Op1(loc, loc, type)) }
            }
            path(13832)
            assertEquals(spawn, player.coords)
            path(13831)
            assertEquals(spawn, player.coords)
            for (foe in beasts) {
                foe.queueHit(player, 1, HitType.Melee, 200)
                advance()
            }
            path(13832)
            val bridgeStart = player.coords
            assertNotEquals(spawn, bridgeStart)
            path(13832)
            assertEquals(bridgeStart, player.coords)
            advance(5)
            val lumber = nearest(5673)
            assertEquals(5, lumber.size)
            for (foe in lumber) {
                foe.queueHit(player, 1, HitType.Melee, 200)
                advance()
            }
            val plank = objTypes.getValue(960)
            val bridge = CoordGrid(bridgeStart.x + 8, bridgeStart.z - 2, 1)
            // The client sends the ground-level effective bridge position after bridge collision
            // projection.
            for (i in 0..2) player.inv[i] = InvObj(plank)
            path(13834, bridge)
            assertEquals(3, player.inv.count(plank))
            player.inv[10] = InvObj(objs.hammer)
            for (i in 0..2) path(13834 + i, bridge)
            assertEquals(0, player.inv.count(plank))
            path(13832)
            assertEquals(1, nearest(1568).size)
            path(13832)
            assertEquals(CoordGrid(3436, 3486), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EscortHardTrekCompanion))
            val token = objTypes.getValue(10934)
            val slot = player.inv.indices.first { player.inv[it]?.id == token.id }
            assertEquals(5 * 625, player.inv[slot]?.vars)
            val obj = player.inv[slot]!!
            player.withProtectedAccess {
                eventBus.publish(this, HeldObjEvents.Op1(slot, obj, token, inv))
            }
            assertTrue(player.ui.containsModal(cacheTypes.interfaces.getValue(274)))
            val buy = cacheTypes.components.getValue((274 shl 16) or 11)
            player.ifButton(buy)
            advance()
            assertEquals(1, player.inv.count(token))
            player.ifButton(cacheTypes.components.getValue((274 shl 16) or 6), 1)
            advance()
            player.ifButton(buy)
            advance()
            assertEquals(0, player.inv.count(token))
            assertEquals(
                150 + (250 - 150) * (3125 - 325) / 7675,
                player.inv.count(objTypes.getValue(1778)),
            )
        }

    @Test
    fun GameTestState
        .`ramble checks the reverse quest and aborting a hard escort grants no token or diary`() =
        runGameTest(TempleTrekking::class) {
            val temple = CoordGrid(3436, 3486)
            player.placeAt(temple)
            player.members = true
            player.setVarBit(cacheTypes.varbits.getValue(1990), 430)
            val forester = spawnNpc(temple.translate(1, 0), npcTypes.getValue(1573))
            fun talk() {
                player.withProtectedAccess { opNpc3(forester) }
                advance()
            }
            talk()
            assertFalse(player.ui.containsModal(cacheTypes.interfaces.getValue(329)))
            player.setVarBit(cacheTypes.varbits.getValue(2573), 320)
            talk()
            random.next = 1
            repeat(3) { random.then = 5 }
            player.ifButton(cacheTypes.components.getValue((329 shl 16) or 29))
            advance()
            assertNotEquals(temple, player.coords)
            val locType = locTypes.getValue(13833)
            val escape = placeMapLoc(player.coords, locType)
            player.withProtectedAccess {
                eventBus.publish(this, LocEvents.Op1(escape, escape, locType))
            }
            assertEquals(CoordGrid(3488, 3269), player.coords)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EscortHardTrekCompanion))
            assertTrue(player.inv.isEmpty())
        }
}
