package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import java.time.Instant
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.events.interact.ObjEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

class BlastMineTest {
    class Deps @Inject constructor(val ground: ObjRepository, val held: HeldUInteractions)

    @Test
    fun GameTestState.chiselFuseAndDepositAwardThePreselectedOresOnlyOnCollection() =
        runInjectedGameTest(Deps::class, null, BlastMine::class) { d ->
            player.members = true
            player.observerUUID = 1
            player.stats[stats.mining] = 75
            val face = placeMapLoc(CoordGrid(3202, 3202), locTypes.getValue(28579))
            val approach = face.coords.translateX(-1)
            player.telejump(approach)
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(objTypes.getValue(13573))
            player.inv[2] = InvObj(objs.tinderbox)
            fun stage(id: Int) =
                BoundLocInfo(
                    checkNotNull(findLoc(face.coords, locTypes.getValue(id))) {
                        "Missing rock stage $id"
                    },
                    locTypes.getValue(id),
                )
            val start = player.statMap.getFineXP(stats.mining)
            player.opLoc1(face)
            advance(6)
            assertEquals(200, player.statMap.getFineXP(stats.mining) - start)
            player.opLoc1(stage(28581))
            advance(2)
            assertEquals(0, player.count(objTypes.getValue(13573)))
            val fire = player.statMap.getFineXP(stats.firemaking)
            player.opLoc1(stage(28583))
            advance(2)
            assertEquals(500, player.statMap.getFineXP(stats.firemaking) - fire)
            player.telejump(approach.translateX(-3))
            random.next = 0
            random.then = 0
            random.then = 999
            advance(6)
            stage(28587)
            val blasted = d.ground.findAll(approach).single { it.type == 13575 }
            player.telejump(approach)
            player.withProtectedAccess { publish(ObjEvents.Op3(blasted)) }
            assertEquals(1, player.count(objTypes.getValue(13575)))
            val sack = placeMapLoc(approach.translateZ(-1), locTypes.getValue(28592))
            player.opLoc1(sack)
            advance(3)
            assertEquals(0, player.count(objTypes.getValue(13575)))
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(10702)])
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(10698)])
            assertEquals(200, player.statMap.getFineXP(stats.mining) - start)
            val op = spawnNpc(player.coords.translateX(-1), npcTypes.getValue(7072))
            player.opNpc3(op)
            advance(3)
            assertEquals(1, player.count(objTypes.getValue(452)))
            assertEquals(1, player.count(objTypes.getValue(454)))
            assertEquals(3130, player.statMap.getFineXP(stats.mining) - start)
            player.opNpc3(op)
            advance(2)
            assertEquals(1, player.count(objTypes.getValue(452)))
        }

    @Test
    fun GameTestState.expiredOreAndFullSacksDoNotBuryOrDuplicateAReward() =
        runGameTest(BlastMine::class) {
            player.members = true
            val seconds = Instant.now().epochSecond.toInt()
            val fresh = ((seconds and 0x1ffffff) shl 6) or 5
            val expired = (((seconds - 181) and 0x1ffffff) shl 6) or 1
            player.inv[0] = InvObj(objTypes.getValue(13575), vars = fresh)
            player.inv[1] = InvObj(objTypes.getValue(13575), vars = expired)
            player.setVarBit(cacheTypes.varbits.getValue(10702), 900)
            val sack = placeMapLoc(CoordGrid(3202, 3202), locTypes.getValue(28592))
            player.telejump(sack.coords.translateX(-1))
            player.opLoc1(sack)
            advance(3)
            assertEquals(fresh, player.inv[0]?.vars)
            assertNull(player.inv[1])
            val operator = spawnNpc(player.coords.translateZ(-1), npcTypes.getValue(7072))
            for (i in 1 until player.inv.size) player.inv[i] = InvObj(objs.knife)
            player.opNpc3(operator)
            advance(3)
            assertEquals(900, player.vars[cacheTypes.varbits.getValue(10702)])
            player.inv[1] = null
            player.opNpc3(operator)
            advance(3)
            assertEquals(900, player.count(objTypes.getValue(452)))
            player.opLoc1(sack)
            advance(3)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(10702)])
            assertNull(player.inv[0])
        }

    @Test
    fun GameTestState.thirusOnlyConsumesTheDailyClaimWhenTheNotedDynamiteFits() =
        runGameTest(BlastMine::class) {
            player.members = true
            val npc = spawnNpc(CoordGrid(1517, 3834), npcTypes.getValue(8627))
            player.telejump(npc.coords.translateX(1))
            player.setVarBit(varbits.kourend_diary_medium_complete, 1)
            player.setVarBit(cacheTypes.varbits.getValue(7930), 1)
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            fun claim() {
                player.opNpc4(npc)
                advance(3)
            }
            claim()
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(7939)])
            player.inv[5] = null
            claim()
            claim()
            assertEquals(20, player.count(objTypes.getValue(13574)))
            player.setVarp(cacheTypes.varps.getValue(65820), 1)
            player.setVarBit(varbits.kourend_diary_elite_complete, 1)
            claim()
            assertEquals(100, player.count(objTypes.getValue(13574)))
        }

    @Test
    fun GameTestState.burnerPersistsLogsAndTimeThenFusesTheResultIntoDynamite() =
        runInjectedGameTest(Deps::class, null, JuniperCharcoal::class) { d ->
            player.members = true
            val west =
                placeMapLoc(
                    CoordGrid(1714, 3470),
                    locTypes.getValue(14837),
                    angle = org.rsmod.game.loc.LocAngle.North,
                )
            player.telejump(west.coords.translate(1, -1))
            player.inv[0] = InvObj(objs.juniper_logs)
            player.inv[1] = InvObj(objs.juniper_logs)
            player.withProtectedAccess {
                publish(LocUEvents.Op(west, west, locTypes[west], objTypes[objs.juniper_logs], 0))
            }
            assertEquals(2, player.vars[cacheTypes.varps.getValue(65821)])
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(4771)])
            player.inv[0] = InvObj(objs.coins, 600)
            player.inv[1] = InvObj(objs.tinderbox)
            player.opLoc1(west)
            advance(7)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance()
            assertEquals(0, player.count(objs.coins))
            assertEquals(2, player.vars[cacheTypes.varbits.getValue(4771)])
            player.setVarp(
                cacheTypes.varps.getValue(65822),
                (Instant.now().epochSecond / 600).toInt() - 99,
            )
            player.opLoc2(west)
            advance(2)
            assertEquals(102, player.vars[cacheTypes.varbits.getValue(4771)])
            player.opLoc1(west)
            advance(2)
            val charcoal = objTypes.getValue(13570)
            assertEquals(100, player.count(charcoal))
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(4771)])
            player.inv[4] = InvObj(objs.pot_empty)
            player.inv[5] = InvObj(objTypes.getValue(13571))
            player.inv[6] = InvObj(objTypes.getValue(13421))
            fun use(a: Int, b: Int) {
                player.withProtectedAccess {
                    d.held.interact(this, inv, objTypes[inv[a]!!], a, objTypes[inv[b]!!], b)
                }
                advance(3)
            }
            use(4, 5)
            val slot = player.inv.indexOfFirst { it?.id == 13572 }
            assertTrue(slot >= 0)
            player.inv[6] = InvObj(objs.ball_of_wool)
            use(6, slot)
            assertEquals(1, player.count(objTypes.getValue(13573)))
            assertEquals(99, player.count(charcoal))
        }
}
