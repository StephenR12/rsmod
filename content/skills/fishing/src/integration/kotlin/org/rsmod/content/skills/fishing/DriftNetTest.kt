package org.rsmod.content.skills.fishing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.fishing.driftnet.DriftNetScript
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class DriftNetTest {
    @Test
    fun GameTestState.passiveShoalOnlyAwardsFishingAndIgnoresAnglerOutfit() =
        runInjectedGameTest(org.rsmod.events.EventBus::class, null, DriftNetScript::class) { bus ->
            player.members = true
            player.stats[stats.fishing] = 70
            player.stats[stats.hunter] = 70
            player.worn[0] = InvObj(objTypes.getValue(13258))
            val fishing = player.statMap.getFineXP(stats.fishing)
            val hunter = player.statMap.getFineXP(stats.hunter)
            player.setVarBit(cacheTypes.varbits.getValue(5812), 1)
            bus.publish(org.rsmod.game.entity.player.SessionStateEvent.Login(player))
            random.next = 0
            advance(2)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(5813)])
            assertEquals(770, player.statMap.getFineXP(stats.fishing) - fishing)
            assertEquals(hunter, player.statMap.getFineXP(stats.hunter))
        }

    @Test
    fun GameTestState.questSkillsAndPaymentGateTheNativePrivateCave() =
        runGameTest(DriftNetScript::class) {
            player.members = true
            val fee = objTypes.getValue(21555)
            val pos = CoordGrid(3729, 10295, 1)
            val entrance = placeMapLoc(pos, locTypes.getValue(30958))
            player.teleport(pos.translate(0, -1))
            player.inv[0] = InvObj(fee, 200)
            player.stats[stats.fishing] = 99
            player.stats[stats.hunter] = 44
            player.opLoc1(entrance)
            advance(6)
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65747)])
            player.setVarBit(cacheTypes.varbits.getValue(5795), 50)
            player.stats[stats.fishing] = 46
            player.setCurrentLevel(stats.fishing, 50)
            player.opLoc1(entrance)
            advance(6)
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65747)])
            player.stats[stats.fishing] = 47
            player.opLoc1(entrance)
            advance(6)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
            advance(2)
            assertEquals(0, player.count(fee))
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65747)])
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(242)])
            assertTrue(player.vars[cacheTypes.varps.getValue(65746)] > 0)
            assertTrue(player.vars[cacheTypes.varbits.getValue(5811)] >= 990)
        }

    @Test
    fun GameTestState.setupChaseHarvestAndBankThroughNativeObjects() =
        runInjectedGameTest(NpcRepository::class, null, DriftNetScript::class) { npcs ->
            player.members = true
            player.stats[stats.fishing] = 70
            player.stats[stats.hunter] = 70
            val initial = player.statMap.getFineXP(stats.fishing)
            player.setVarBit(cacheTypes.varbits.getValue(5795), 50)
            player.setVarBit(cacheTypes.varbits.getValue(242), 1)
            val npos = CoordGrid(3746, 10295, 1)
            val typ = locTypes.getValue(31433)
            placeMapLoc(npos, typ, angle = LocAngle.East)
            val apos = CoordGrid(3738, 10301, 1)
            val ann = locTypes.getValue(31843)
            placeMapLoc(apos, ann)
            val pos = CoordGrid(3729, 10295, 1)
            player.teleport(pos.translate(0, -1))
            player.opLoc1(placeMapLoc(pos, locTypes.getValue(30958)))
            advance(6)
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65747)])
            val inside = player.coords
            val anchor = inside.translate(9, -4)
            val loc = BoundLocInfo(requireNotNull(findLoc(anchor, typ)), typ)
            val annette = inside.translate(1, 2)
            player.inv[0] = InvObj(objTypes.cert(objTypes[objs.drift_net]), 6)
            player.teleport(annette.translate(0, -1))
            player.opLoc3(BoundLocInfo(requireNotNull(findLoc(annette, ann)), ann))
            advance(2)
            val side = cacheTypes.components.getValue(307 shl 16)
            player.ifButton(
                side,
                comsub = 0,
                obj = objTypes.cert(objTypes[objs.drift_net]).id,
                op = IfButtonOp.Op4,
            )
            advance()
            assertEquals(6, player.vars[cacheTypes.varbits.getValue(243)])
            player.ifButton(
                cacheTypes.components.getValue((309 shl 16) + 5),
                comsub = 0,
                obj = objs.drift_net.id,
                op = IfButtonOp.Op1,
            )
            advance()
            assertEquals(1, player.count(objs.drift_net))
            player.ifClose()
            advance()
            player.teleport(anchor.translate(-1, 0))
            player.opLoc1(loc)
            advance(2)
            assertEquals(0, player.count(objs.drift_net))
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(5812)])
            val shoals = npcs.findAll(ZoneKey.from(inside), 4).filter { it.id == 7782 }.toList()
            val near = shoals.minBy { it.coords.chebyshevDistance(anchor) }
            repeat(6) {
                if (player.vars[cacheTypes.varbits.getValue(5813)] == 0) {
                    player.teleport(near.coords.translate(-1, 0))
                    player.opNpc1(near)
                    advance(18)
                }
            }
            assertTrue(player.vars[cacheTypes.varbits.getValue(5813)] > 0)
            assertTrue(player.statMap.getFineXP(stats.fishing) > initial)
            player.teleport(anchor.translate(-1, 0))
            player.opLoc1(loc)
            advance(2)
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(5812)])
            val catch = player.invMap.getValue(cacheTypes.invs.getValue(947))
            assertTrue(catch.isNotEmpty())
            val claim = cacheTypes.components.getValue((607 shl 16) + 13)
            player.ifButton(claim)
            advance()
            assertTrue(catch.isNotEmpty())
            player.inv[3] = InvObj(objTypes.getValue(21555), 5)
            player.ifButton(claim)
            advance()
            assertEquals(0, player.count(objTypes.getValue(21555)))
            assertTrue(catch.isEmpty())
            assertTrue(player.invMap.getValue(cacheTypes.invs.getValue(95)).isNotEmpty())
        }
}
