package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.smithing.smelting.workshop.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class WorkshopTest {
    @Test
    fun GameTestState
        .`native elemental furnace consumes four coal or catalyst and two coal individually`() =
        game { d ->
            prepare()
            val loc = placeMapLoc(CoordGrid(2725, 9874), WorkshopLocs.furnaces.last())
            player.telejump(loc.coords.translateX(-1))
            player.inv[0] = InvObj(WorkshopObjs.ore)
            repeat(4) { player.inv[1 + it] = InvObj(objs.coal) }
            player.withProtectedAccess {
                check(
                    eventBus.publish(
                        this,
                        LocUEvents.Op(
                            loc,
                            loc,
                            locTypes[WorkshopLocs.furnaces.last()],
                            objTypes[WorkshopObjs.ore],
                            0,
                        ),
                    )
                )
            }
            advance(7)
            assertEquals(1, player.count(WorkshopObjs.bar))
            assertEquals(0, player.count(objs.coal))
            assertEquals(80, player.statMap.getFineXP(stats.smithing))
            player.inv[1] = InvObj(WorkshopObjs.ore)
            player.inv[2] = InvObj(objs.coal)
            player.inv[3] = InvObj(objs.coal)
            player.inv[4] = InvObj(WorkshopObjs.catalyst)
            player.withProtectedAccess { assertTrue(d.production.smelt(this, loc)) }
            advance(7)
            assertEquals(2, player.count(WorkshopObjs.bar))
            assertEquals(0, player.count(WorkshopObjs.catalyst))
            assertEquals(240, player.statMap.getFineXP(stats.smithing))
        }

    @Test
    fun GameTestState.`furnace rejects insufficient coal level and interrupted ingredient loss`() =
        game { d ->
            prepare()
            val loc = placeMapLoc(CoordGrid(2725, 9874), WorkshopLocs.furnaces.last())
            player.telejump(loc.coords.translateX(-1))
            player.inv[0] = InvObj(WorkshopObjs.ore)
            player.withProtectedAccess { assertFalse(d.production.smelt(this, loc)) }
            assertEquals(0, player.count(WorkshopObjs.bar))
            repeat(4) { player.inv[1 + it] = InvObj(objs.coal) }
            player.setCurrentLevel(stats.smithing, 19)
            player.withProtectedAccess { assertFalse(d.production.smelt(this, loc)) }
            player.setCurrentLevel(stats.smithing, 20)
            player.withProtectedAccess { d.production.smelt(this, loc) }
            player.inv[0] = null
            advance(7)
            assertEquals(4, player.count(objs.coal))
            assertEquals(0, player.count(WorkshopObjs.bar))
            assertEquals(0, player.statMap.getFineXP(stats.smithing))
        }

    @Test
    fun GameTestState
        .`four normal workbench products require the matching books and grant exact XP`() =
        game { d ->
            prepare()
            val loc = placeMapLoc(CoordGrid(2717, 9887), WorkshopLocs.workbench)
            player.telejump(loc.coords.translateZ(-1))
            player.inv[0] = InvObj(objs.hammer)
            for (index in 0..3) {
                player.inv[1] = InvObj(if (index < 2) WorkshopObjs.bar else WorkshopObjs.mind)
                player.inv[2] =
                    InvObj(if (index % 2 == 0) WorkshopObjs.slashed else WorkshopObjs.beaten)
                player.withProtectedAccess { assertTrue(d.production.smith(this, loc, index, 1)) }
                advance(5)
                assertEquals(1, player.count(WorkshopObjs.outputs[index]))
            }
            assertEquals(1000, player.statMap.getFineXP(stats.smithing))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithMindHelmet))
            assertEquals(1, player.count(objs.hammer))
        }

    @Test
    fun GameTestState.`mind helmet rejects quest tool level and book loss during smithing`() =
        game { d ->
            prepare()
            val loc = placeMapLoc(CoordGrid(2717, 9887), WorkshopLocs.workbench)
            player.telejump(loc.coords.translateZ(-1))
            player.inv[0] = InvObj(WorkshopObjs.mind)
            player.inv[1] = InvObj(WorkshopObjs.beaten)
            player.withProtectedAccess { assertFalse(d.production.smith(this, loc, 3, 1)) }
            player.inv[2] = InvObj(objs.hammer)
            VarPlayerIntMapSetter.set(player, WorkshopBits.quest, 4)
            player.withProtectedAccess { assertFalse(d.production.smith(this, loc, 3, 1)) }
            VarPlayerIntMapSetter.set(player, WorkshopBits.quest, 5)
            player.setCurrentLevel(stats.smithing, 29)
            player.withProtectedAccess { assertFalse(d.production.smith(this, loc, 3, 1)) }
            player.setCurrentLevel(stats.smithing, 30)
            player.withProtectedAccess { d.production.smith(this, loc, 3, 1) }
            player.inv[1] = null
            advance(5)
            assertEquals(1, player.count(WorkshopObjs.mind))
            assertEquals(0, player.count(WorkshopObjs.mindHelmet))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.SmithMindHelmet))
        }

    @Test
    fun GameTestState
        .`full native four station cycle produces one primed bar without free inputs or XP`() =
        game { d ->
            prepare()
            enter(d)
            val cart = d.rooms.session(player)!!.cart
            player.inv[0] = InvObj(WorkshopObjs.bar)
            player.telejump(cart.coords.translateZ(1))
            player.withProtectedAccess { assertTrue(d.production.putCart(this, cart)) }
            assertEquals(0, player.count(WorkshopObjs.bar))
            assertEquals(1, player.vars[WorkshopBits.bar])
            control(d, WorkshopLocs.lift)
            control(d, WorkshopLocs.lift)
            control(d, WorkshopLocs.rotate)
            control(d, WorkshopLocs.lift)
            control(d, WorkshopLocs.lift)
            control(d, WorkshopLocs.rotate)
            control(d, WorkshopLocs.lift)
            control(d, WorkshopLocs.lift)
            assertEquals(2, player.vars[WorkshopBits.bar])
            assertEquals(1, player.vars[WorkshopBits.crane])
            control(d, WorkshopLocs.move)
            control(d, WorkshopLocs.press)
            assertEquals(3, player.vars[WorkshopBits.bar])
            control(d, WorkshopLocs.move)
            control(d, WorkshopLocs.waterDoor)
            control(d, WorkshopLocs.screw)
            control(d, WorkshopLocs.screw)
            control(d, WorkshopLocs.waterDoor)
            control(d, WorkshopLocs.west)
            assertEquals(4, player.vars[WorkshopBits.bar])
            assertEquals(1, player.vars[WorkshopBits.water])
            control(d, WorkshopLocs.east)
            control(d, WorkshopLocs.west)
            control(d, WorkshopLocs.east)
            control(d, WorkshopLocs.waterDoor)
            control(d, WorkshopLocs.screw)
            control(d, WorkshopLocs.screw)
            control(d, WorkshopLocs.waterDoor)
            control(d, WorkshopLocs.move)
            control(d, WorkshopLocs.fan)
            control(d, WorkshopLocs.fan)
            control(d, WorkshopLocs.move)
            assertEquals(5, player.vars[WorkshopBits.bar])
            assertEquals(0, player.vars[WorkshopBits.position])
            assertEquals(WorkshopCarts.stages[5].id, cart.visType.id)
            player.telejump(cart.coords.translateZ(1))
            player.withProtectedAccess { assertTrue(d.production.takeCart(this, cart)) }
            assertEquals(1, player.count(WorkshopObjs.primed))
            assertEquals(0, player.vars[WorkshopBits.bar])
            assertEquals(0, player.statMap.getFineXP(stats.smithing))
        }

    @Test
    fun GameTestState.`unrepaired and out of order machinery cannot produce a primed bar`() =
        game { d ->
            prepare()
            enter(d)
            control(d, WorkshopLocs.press, false)
            VarPlayerIntMapSetter.set(player, WorkshopBits.position, 1)
            VarPlayerIntMapSetter.set(player, WorkshopBits.bar, 2)
            VarPlayerIntMapSetter.set(player, WorkshopBits.pressRepaired, 0)
            control(d, WorkshopLocs.press, false)
            assertEquals(2, player.vars[WorkshopBits.bar])
            VarPlayerIntMapSetter.set(player, WorkshopBits.mediumCog, 0)
            control(d, WorkshopLocs.fan, false)
            assertEquals(0, player.count(WorkshopObjs.primed))
            assertEquals(0, player.statMap.getFineXP(stats.smithing))
        }

    @Test
    fun GameTestState
        .`mind extractor drains twenty current Magic and preserves bar on a full bag`() =
        game { d ->
            prepare()
            enter(d)
            val gun = instanceLoc(d, WorkshopLocs.gun, CoordGrid(1962, 5148))
            val hat = instanceLoc(d, WorkshopLocs.hat, CoordGrid(1962, 5150))
            player.inv[0] = InvObj(WorkshopObjs.primed)
            player.telejump(gun.coords.translateX(-1))
            player.withProtectedAccess { assertTrue(d.production.putGun(this, gun)) }
            player.telejump(hat.coords.translateX(-1))
            player.setCurrentLevel(stats.magic, 19)
            player.withProtectedAccess { assertFalse(d.production.extract(this, hat)) }
            player.setCurrentLevel(stats.magic, 20)
            player.withProtectedAccess { assertTrue(d.production.extract(this, hat)) }
            advance(5)
            assertEquals(0, player.stats[stats.magic])
            assertEquals(2, player.vars[WorkshopBits.mind])
            assertEquals(0, player.statMap.getFineXP(stats.magic))
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            player.telejump(gun.coords.translateX(-1))
            player.withProtectedAccess { assertFalse(d.production.takeGun(this, gun)) }
            assertEquals(2, player.vars[WorkshopBits.mind])
            player.inv[0] = null
            player.withProtectedAccess { assertTrue(d.production.takeGun(this, gun)) }
            assertEquals(1, player.count(WorkshopObjs.mind))
            assertEquals(0, player.vars[WorkshopBits.mind])
        }

    @Test
    fun GameTestState
        .`private machinery isolates two accounts and rejects another player's cart`() = game { d ->
        prepare()
        enter(d)
        val first = d.rooms.session(player)!!
        val second = registerPlayer(WorkshopAccess.RETURN)
        prepare(second)
        second.withProtectedAccess { assertTrue(d.rooms.enter(this)) }
        val other = d.rooms.session(second)!!
        assertNotEquals(first.region.uid, other.region.uid)
        second.inv[0] = InvObj(WorkshopObjs.bar)
        second.withProtectedAccess { assertFalse(d.production.putCart(this, first.cart)) }
        assertEquals(1, second.count(WorkshopObjs.bar))
        assertEquals(0, player.vars[WorkshopBits.bar])
        second.withProtectedAccess { d.rooms.leave(this) }
        assertNull(d.rooms.session(second))
        assertNotNull(d.rooms.session(player))
    }

    @Test
    fun GameTestState
        .`native beaten book reclaim and workshop hatch preserve native progression gate`() =
        game { d ->
            prepare()
            val shelf =
                placeMapLoc(
                    CoordGrid(3366, 3335),
                    WorkshopLocs.beatenBook,
                    LocShape.CentrepieceStraight,
                    LocAngle.South,
                )
            player.telejump(shelf.coords.translateZ(1))
            player.opLoc1(shelf)
            advance()
            assertEquals(1, player.count(WorkshopObjs.beaten))
            player.opLoc1(shelf)
            advance()
            assertEquals(1, player.count(WorkshopObjs.beaten))
            VarPlayerIntMapSetter.set(player, WorkshopBits.quest, 4)
            player.withProtectedAccess { assertFalse(d.rooms.enter(this)) }
            assertNull(d.rooms.session(player))
            VarPlayerIntMapSetter.set(player, WorkshopBits.quest, 5)
            player.withProtectedAccess { assertTrue(d.rooms.enter(this)) }
            assertNotNull(d.rooms.session(player))
        }

    @Test
    fun GameTestState.`native Smith menu fourth choice creates the diary mind helmet`() =
        game { _ ->
            prepare()
            val loc =
                placeMapLoc(
                    CoordGrid(2717, 9887),
                    WorkshopLocs.workbench,
                    LocShape.CentrepieceStraight,
                    LocAngle.South,
                )
            player.telejump(loc.coords.translateZ(-1))
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(WorkshopObjs.mind)
            player.inv[2] = InvObj(WorkshopObjs.beaten)
            player.opLoc1(loc)
            advance()
            val modal = player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id))!!
            player.ui.queueClose(modal)
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.skillmulti_d, subcomponent = 1)
            )
            advance(5)
            assertEquals(1, player.count(WorkshopObjs.mindHelmet))
            assertEquals(300, player.statMap.getFineXP(stats.smithing))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithMindHelmet))
        }

    @Test
    fun GameTestState
        .`logout preserves the single cart material and reconstructs it without duplication`() =
        game { d ->
            prepare()
            enter(d)
            val cart = d.rooms.session(player)!!.cart
            player.inv[0] = InvObj(WorkshopObjs.bar)
            player.telejump(cart.coords.translateZ(1))
            player.withProtectedAccess { assertTrue(d.production.putCart(this, cart)) }
            advance()
            d.rooms.logout(player)
            assertEquals(WorkshopAccess.RETURN, player.coords)
            assertNull(d.rooms.session(player))
            assertEquals(1, player.vars[WorkshopBits.bar])
            player.withProtectedAccess { assertTrue(d.rooms.enter(this)) }
            advance()
            val restored = d.rooms.session(player)!!.cart
            assertEquals(WorkshopCarts.stages[1].id, restored.visType.id)
            player.telejump(restored.coords.translateZ(1))
            player.withProtectedAccess {
                assertTrue(d.production.takeCart(this, restored))
                assertFalse(d.production.takeCart(this, restored))
            }
            assertEquals(1, player.count(WorkshopObjs.bar))
            assertEquals(0, player.vars[WorkshopBits.bar])
        }

    @Test
    fun GameTestState
        .`full cart pickup and interrupted machine operation preserve native progress`() =
        game { d ->
            prepare()
            enter(d)
            val cart = d.rooms.session(player)!!.cart
            VarPlayerIntMapSetter.set(player, WorkshopBits.bar, 5)
            d.rooms.sync(player)
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            player.telejump(cart.coords.translateZ(1))
            player.withProtectedAccess { assertFalse(d.production.takeCart(this, cart)) }
            assertEquals(5, player.vars[WorkshopBits.bar])
            player.inv[0] = null
            player.withProtectedAccess { assertTrue(d.production.takeCart(this, cart)) }
            assertEquals(1, player.count(WorkshopObjs.primed))
            val control =
                instanceLoc(d, WorkshopLocs.move, CONTROL_TILES.getValue(WorkshopLocs.move))
            player.telejump(control.coords.translateZ(-1))
            player.withProtectedAccess { d.production.operate(this, control) }
            player.telejump(control.coords.translateX(12))
            advance(3)
            assertEquals(0, player.vars[WorkshopBits.position])
        }

    private fun GameTestScope.prepare(p: Player = player) {
        p.members = true
        p.stats[stats.smithing] = 99
        p.statMap.setFineXP(stats.smithing, 0)
        p.stats[stats.magic] = 99
        p.statMap.setFineXP(stats.magic, 0)
        for ((bit, value) in
            listOf(
                WorkshopBits.quest to 11,
                WorkshopBits.learned to 1,
                WorkshopBits.finished to 1,
                WorkshopBits.furnace to 1,
                WorkshopBits.hatch to 1,
                WorkshopBits.crane to 1,
                WorkshopBits.pressRepaired to 1,
                WorkshopBits.pipeRepaired to 1,
                WorkshopBits.smallCog to 1,
                WorkshopBits.mediumCog to 2,
                WorkshopBits.largeCog to 3,
            )) VarPlayerIntMapSetter.set(p, bit, value)
    }

    private fun GameTestScope.enter(d: Dependencies) {
        player.withProtectedAccess { assertTrue(d.rooms.enter(this)) }
        advance()
    }

    private fun GameTestScope.instanceLoc(
        d: Dependencies,
        type: LocType,
        normal: CoordGrid,
    ): BoundLocInfo {
        val c = d.rooms.coord(player, normal)!!
        val loc = findLoc(c, locTypes[type])
        return if (loc != null) BoundLocInfo(loc, locTypes[type]) else placeMapLoc(c, type)
    }

    private fun GameTestScope.control(d: Dependencies, type: LocType, success: Boolean = true) {
        val normal = CONTROL_TILES.getValue(type)
        val loc = instanceLoc(d, type, normal)
        player.telejump(loc.coords.translateZ(-1))
        player.withProtectedAccess {
            assertEquals(success, d.production.operate(this, loc), "control=${type.id}")
        }
        advance(3)
    }

    private fun GameTestState.game(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, null, WorkshopScript::class) { d ->
            for (l in listOf(0, 2, 3)) for (x in 0..7) for (z in 0..7) allocZoneCollision(
                CoordGrid(1920 + x * 8, 5120 + z * 8, l)
            )
            for ((type, c) in CONTROL_TILES) placeMapLoc(c, type)
            for (c in
                listOf(
                    CoordGrid(2717, 9887),
                    CoordGrid(2725, 9874),
                    CoordGrid(3367, 3335),
                    WorkshopAccess.RETURN,
                )) for (x in -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(
                c.translate(x, z)
            )
            body(d)
        }

    data class Dependencies
    @Inject
    constructor(
        val production: WorkshopProduction,
        val rooms: WorkshopAccess,
        val script: WorkshopScript,
    )

    companion object {
        val CONTROL_TILES by lazy {
            mapOf(
                WorkshopLocs.move to CoordGrid(1953, 5151, 2),
                WorkshopLocs.rotate to CoordGrid(1955, 5148, 2),
                WorkshopLocs.lift to CoordGrid(1953, 5148, 2),
                WorkshopLocs.press to CoordGrid(1950, 5153, 2),
                WorkshopLocs.west to CoordGrid(1949, 5161, 2),
                WorkshopLocs.east to CoordGrid(1957, 5161, 2),
                WorkshopLocs.waterDoor to CoordGrid(1953, 5161, 2),
                WorkshopLocs.screw to CoordGrid(1955, 5161, 2),
                WorkshopLocs.fan to CoordGrid(1959, 5154, 2),
            )
        }
    }
}
