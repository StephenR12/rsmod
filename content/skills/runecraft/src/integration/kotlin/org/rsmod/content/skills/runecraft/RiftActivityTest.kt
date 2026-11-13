package org.rsmod.content.skills.runecraft

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.config.ShopComponents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RiftActivityTest {
    private fun GameTestScope.gate() {
        val gate = placeMapLoc(CoordGrid(3613, 9483), locTypes.getValue(43700))
        player.teleport(CoordGrid(3615, 9482))
        player.opLoc1(gate)
        advance(2)
    }

    private fun GameTestScope.ready(quest: org.rsmod.game.type.varbit.VarBitType) {
        player.members = true
        player.setBaseLevel(stats.runecrafting, 77)
        player.setCurrentLevel(stats.runecrafting, 77)
        player.setVarBit(quest, 130)
        gate()
        assertEquals(CoordGrid(3615, 9485), player.coords)
    }

    @Test
    fun GameTestState
        .`the barrier checks the saved quest and Runecraft before admitting a participant`() =
        runGameTest(RiftScript::class) {
            player.members = true
            player.setBaseLevel(stats.runecrafting, 27)
            gate()
            assertFalse(player.activityBlocksTeleport)
            player.setVarBit(cacheTypes.varbits.getValue(13738), 130)
            player.setBaseLevel(stats.runecrafting, 26)
            gate()
            assertFalse(player.activityBlocksTeleport)
            player.setBaseLevel(stats.runecrafting, 27)
            gate()
            assertTrue(player.activityBlocksTeleport)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(13691)])
            assertTrue(player.ui.containsOverlay(cacheTypes.interfaces.getValue(746)))
        }

    @Test
    fun GameTestState
        .`mining and workbench use a pickaxe and deliver essence without losing fragments`() =
        runGameTest(RiftScript::class) {
            ready(cacheTypes.varbits.getValue(13738))
            val rock = CoordGrid(3600, 9511)
            player.teleport(rock.translate(-1, 0))
            val remains = placeMapLoc(rock, locTypes.getValue(43715))
            player.opLoc1(remains)
            advance(8)
            assertEquals(0, player.count(objTypes.getValue(26878)))
            player.inv[0] = InvObj(objs.bronze_pickaxe)
            player.opLoc1(remains)
            advance(17)
            val fragments = player.count(objTypes.getValue(26878))
            assertTrue(fragments > 0)
            val bench = CoordGrid(3611, 9486)
            player.teleport(bench.translate(2, 0))
            advance()
            val before = player.count(objTypes.getValue(26878))
            player.opLoc1(placeMapLoc(bench, locTypes.getValue(43754), angle = LocAngle.South))
            advance(4)
            val n = player.count(objTypes.getValue(26879))
            assertTrue(n > 0)
            assertEquals(before, n + player.count(objTypes.getValue(26878)))
        }

    @Test
    fun GameTestState
        .`rotating portal leads to a real altar and its exit returns to the same battle`() =
        runGameTest(RiftScript::class, RunecraftScript::class, RuneTempleScript::class) {
            ready(cacheTypes.varbits.getValue(13738))
            val portal = CoordGrid(3623, 9500)
            player.teleport(portal.translate(-1, 1))
            val door = placeMapLoc(portal, locTypes.getValue(43702), angle = LocAngle.North)
            player.opLoc1(door)
            advance(2)
            assertTrue(player.coords.z > 9000, "An altar is inactive during preparation")
            random.next = 0
            random.then = 0
            advance(198)
            player.opLoc1(door)
            advance(3)
            assertEquals(CoordGrid(2726, 4832), player.coords)
            for (i in 0..9) player.inv[i] = InvObj(RunecraftObjs.guardianEssence)
            player.inv[10] = InvObj(objTypes.getValue(26882), 2)
            val altar = placeMapLoc(player.coords.translate(1, 0), RunecraftLocs.waterAltar)
            player.opLoc1(altar)
            advance(2)
            assertEquals(10, player.count(objTypes.getValue(26881)))
            assertEquals(1, player.count(objTypes.getValue(26884)))
            assertEquals(1, player.count(objTypes.getValue(26882)))
            assertEquals(50, player.count(objs.water_rune))
            val returnPortal =
                locTypes.values.single { it.internalName == "watertemple_exit_portal" }
            player.opLoc1(placeMapLoc(player.coords.translate(-1, 0), returnPortal))
            advance(2)
            assertTrue(player.coords.x in 3620..3627 && player.coords.z in 9497..9505)
            val guardian = spawnNpc(CoordGrid(3614, 9501), npcTypes.getValue(11403))
            player.teleport(CoordGrid(3613, 9501))
            player.opNpc1(guardian)
            advance(2)
            assertEquals(20, player.vars[cacheTypes.varbits.getValue(13686)])
            assertEquals(0, player.count(objTypes.getValue(26881)))
        }

    @Test
    fun GameTestState
        .`closing a rift pays both energies once and a native search spends one of each`() =
        runGameTest(RiftScript::class, RiftRewards::class) {
            ready(cacheTypes.varbits.getValue(13738))
            advance(202)
            player.inv[0] = InvObj(objTypes.getValue(26881), 150)
            player.inv[1] = InvObj(objTypes.getValue(26880), 100)
            val guardian = spawnNpc(CoordGrid(3614, 9501), npcTypes.getValue(11403))
            player.teleport(CoordGrid(3613, 9501))
            player.opNpc1(guardian)
            advance(14)
            assertEquals(1, player.vars[cacheTypes.varps.getValue(3397)])
            assertEquals(3, player.vars[cacheTypes.varbits.getValue(13706)])
            assertEquals(2, player.vars[cacheTypes.varbits.getValue(13705)])
            val gate = placeMapLoc(CoordGrid(3613, 9483), locTypes.getValue(43700))
            player.teleport(CoordGrid(3615, 9484))
            player.opLoc3(gate)
            advance(2)
            assertFalse(player.activityBlocksTeleport)
            val reward = CoordGrid(3610, 9479)
            player.teleport(reward.translate(3, 1))
            player.opLoc1(placeMapLoc(reward, locTypes.getValue(43695), angle = LocAngle.South))
            advance(4)
            assertEquals(2, player.vars[cacheTypes.varbits.getValue(13706)])
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(13705)])
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65720)])
            assertTrue(player.inv.any { it != null })
        }

    @Test
    fun GameTestState
        .`attackers break a weak barrier and an unattended round ends without rewards`() =
        runGameTest(RiftScript::class) {
            ready(cacheTypes.varbits.getValue(13738))
            val cells =
                placeMapLoc(CoordGrid(3618, 9486), locTypes.getValue(43733), angle = LocAngle.North)
            player.teleport(CoordGrid(3618, 9485))
            player.opLoc1(cells)
            advance(3)
            assertEquals(1, player.count(objTypes.getValue(26883)))
            val square = CoordGrid(3608, 9503)
            val tile =
                placeMapLoc(
                    square,
                    locTypes.getValue(43739),
                    org.rsmod.game.loc.LocShape.GroundDecor,
                    LocAngle.South,
                )
            player.teleport(square.translate(0, -1))
            player.opLoc1(tile)
            advance(3)
            assertEquals(0, player.count(objTypes.getValue(26883)))
            assertTrue(locExists(square, locTypes.getValue(43740)))
            advanceUntil({ locExists(square, locTypes.getValue(43736)) }, 600)
            advanceUntil({ player.vars[cacheTypes.varbits.getValue(13688)] == 1 }, 300)
            assertMessageSent("The creatures have overwhelmed the Great Guardian.")
            assertEquals(0, player.vars[cacheTypes.varps.getValue(3397)])
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(13706)])
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(13705)])
            advance(51)
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(13688)])
            assertTrue(locExists(square, locTypes.getValue(43739)))
        }

    @Test
    fun GameTestState
        .`a full inventory still spends the search and Felix restores an earned bag in its saved colour`() =
        runGameTest(RiftRewards::class) {
            player.members = true
            player.observerUUID = 1L
            val points = listOf(13705, 13706).map { cacheTypes.varbits.getValue(it) }
            points.forEach { player.setVarBit(it, 1) }
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.beer)
            val reward = CoordGrid(3610, 9479)
            player.teleport(reward.translate(3, 1))
            player.opLoc1(placeMapLoc(reward, locTypes.getValue(43695), angle = LocAngle.South))
            advance()
            assertTrue(points.all { player.vars[it] == 0 })
            assertMessageSent("Some of your rewards fell to the ground.")
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65720)])
            advance(4)
            player.setVarBit(cacheTypes.varbits.getValue(931), 1)
            player.setVarBit(cacheTypes.varbits.getValue(4603), 2)
            val bag = objTypes.getValue(26986)
            val felix = spawnNpc(CoordGrid(3619, 9475), npcTypes.getValue(11404))
            player.teleport(CoordGrid(3619, 9474))
            fun reclaim() {
                player.opNpc1(felix)
                advance(2)
                player.resumeActiveCoroutine(
                    org.rsmod.api.player.input.ResumePauseButtonInput(
                        org.rsmod.api.config.refs.components.menu_list,
                        4,
                    )
                )
            }
            reclaim()
            assertEquals(0, player.count(bag))
            player.inv[0] = null
            reclaim()
            assertEquals(1, player.count(bag))
        }

    @Test
    fun GameTestState
        .`Felix takes pearls through the native shop and respects the death talisman gate`() =
        runInjectedGameTest(
            org.rsmod.api.shops.operation.ShopOperationMap::class,
            org.rsmod.api.shops.ShopsModule(),
            RiftRewards::class,
            ShopScript::class,
        ) { _ ->
            player.members = true
            player.teleport(CoordGrid(3619, 9474))
            val felix = spawnNpc(CoordGrid(3619, 9475), npcTypes.getValue(11404))
            player.inv[0] = InvObj(objTypes.getValue(26792), 399)
            player.opNpc3(felix)
            advance(2)
            val stock = player.openedShop!!.inv
            assertFalse(stock.any { it?.id == 1456 })
            val hat = RunecraftObjs.raimentsHat
            val slot = stock.indexOfFirst { it?.id == hat.id }
            fun buy() {
                player.ifButton(
                    ShopComponents.shop_inv,
                    comsub = slot + 1,
                    obj = hat.id,
                    op = IfButtonOp.Op2,
                )
                advance()
            }
            buy()
            assertEquals(0, player.count(hat))
            player.inv[0] = InvObj(objTypes.getValue(26792), 400)
            buy()
            assertEquals(1, player.count(hat))
            assertEquals(0, player.count(objTypes.getValue(26792)))
        }

    @Test
    fun GameTestState.`the deposit pool respects locked slots and cannot bank guardian supplies`() =
        runGameTest(
            RiftScript::class,
            org.rsmod.content.interfaces.bank.scripts.BankInvScript::class,
        ) {
            ready(cacheTypes.varbits.getValue(13738))
            val pool =
                placeMapLoc(CoordGrid(3609, 9487), locTypes.getValue(43696), angle = LocAngle.East)
            player.teleport(CoordGrid(3610, 9486))
            player.inv[0] = InvObj(objs.air_rune, 20)
            player.inv[1] = InvObj(objs.fire_rune, 12)
            player.inv[2] = InvObj(RunecraftObjs.guardianEssence)
            player.inv[3] = InvObj(objs.chisel)
            player.setVarBit(org.rsmod.api.config.refs.varbits.bank_capacity, 800)
            player.setVarBit(cacheTypes.varbits.getValue(15915), 1)
            player.opLoc2(pool)
            advance(3)
            assertModalOpen(cacheTypes.interfaces.getValue(192))
            val inventory = cacheTypes.components.getValue((192 shl 16) + 23)
            player.ifButton(inventory, comsub = 0, obj = objs.air_rune.id, op = IfButtonOp.Op10)
            advance()
            assertEquals(1, player.vars[cacheTypes.varps.getValue(4611)] and 1)
            player.ifClose()
            advance()
            player.opLoc1(pool)
            advance(3)
            assertEquals(20, player.count(objs.air_rune))
            assertEquals(0, player.count(objs.fire_rune))
            player.opLoc2(pool)
            advance(3)
            player.ifButton(cacheTypes.components.getValue((192 shl 16) + 29))
            advance()
            assertEquals(0, player.count(objs.chisel))
            assertEquals(1, player.count(RunecraftObjs.guardianEssence))
            player.withProtectedAccess {
                assertEquals(12, invTotal(bank, objs.fire_rune))
                assertEquals(1, invTotal(bank, objs.chisel))
            }
        }

    @Test
    fun GameTestState
        .`ring charges share runes across held worn and bank copies and respect activity restrictions`() =
        runInjectedGameTest(
            org.rsmod.api.player.interact.HeldUInteractions::class,
            null,
            RiftJewellery::class,
        ) { held ->
            val empty = objTypes.getValue(26815)
            val charged = objTypes.getValue(26818)
            val fuels =
                listOf(
                    objs.air_rune,
                    objs.earth_rune,
                    objs.water_rune,
                    objs.fire_rune,
                    objs.law_rune,
                )
            player.members = true
            player.inv[0] = InvObj(empty)
            player.worn[12] = InvObj(empty)
            player.withProtectedAccess { bank[0] = InvObj(empty, 2) }
            for ((i, r) in fuels.withIndex()) player.inv[i + 1] = InvObj(r)
            player.withProtectedAccess {
                held.interact(this, inv, empty, 0, objTypes[objs.air_rune], 1)
            }
            assertEquals(1, player.count(charged))
            assertEquals(charged.id, player.worn[12]?.id)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(13707)])
            player.setVarBit(cacheTypes.varbits.getValue(13708), 2)
            player.activityBlocksTeleport = true
            player.withProtectedAccess { opHeld4(0) }
            advance(5)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(13707)])
            player.activityBlocksTeleport = false
            player.withProtectedAccess { opHeld4(0) }
            advance(5)
            assertEquals(CoordGrid(3170, 3155), player.coords)
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(13707)])
            assertEquals(empty.id, player.inv[0]?.id)
            assertEquals(empty.id, player.worn[12]?.id)
            player.withProtectedAccess {
                assertEquals(2, invTotal(bank, empty))
                assertEquals(0, invTotal(bank, charged))
            }
        }

    @Test
    fun GameTestState
        .`dyeing raiments and returning the inspected locket deliver usable reward items`() =
        runInjectedGameTest(
            org.rsmod.api.player.interact.HeldUInteractions::class,
            null,
            RiftJewellery::class,
        ) { held ->
            val hood = objTypes[RunecraftObjs.raimentsHat]
            val dye = objTypes.getValue(26811)
            player.members = true
            player.inv[0] = InvObj(hood)
            player.inv[1] = InvObj(dye)
            player.withProtectedAccess { held.interact(this, inv, dye, 1, hood, 0) }
            assertEquals(1, player.count(objTypes.getValue(26858)))
            assertEquals(0, player.count(dye))
            val guide = spawnNpc(CoordGrid(3221, 3221), npcTypes.getValue(306))
            player.teleport(CoordGrid(3221, 3220))
            player.inv[1] = InvObj(objTypes.getValue(26910))
            player.opNpc1(guide)
            advance(2)
            assertEquals(0, player.count(objTypes.getValue(26914)))
            player.withProtectedAccess { opHeld1(1) }
            player.opNpc1(guide)
            advance(2)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(13809)])
            val slot = player.inv.indexOfFirst { it?.id == 26914 }
            player.withProtectedAccess { opHeld3(slot) }
            advance(5)
            assertEquals(CoordGrid(3615, 9460), player.coords)
        }
}
