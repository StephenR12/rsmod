package org.rsmod.content.other.gembag

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.obj.plugin.ObjTakeModule
import org.rsmod.api.obj.plugin.ObjTakePlugin
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.interact.ObjInteractions
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.FreshDropOrigin
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class GemBagStorageTest {
    @Test
    fun GameTestState.`cache 233 pins bag identities operations and permanent capacity`() =
        runGemBagTest {
            assertEquals(12020, objs.gem_bag.id)
            assertEquals(24481, objs.gem_bag_open.id)
            assertEquals(
                listOf("Fill", "Open", "Check", "Empty", "Destroy"),
                objTypes[objs.gem_bag].iop.toList(),
            )
            assertEquals(
                listOf("Fill", "Close", "Check", "Empty", "Destroy"),
                objTypes[objs.gem_bag_open].iop.toList(),
            )
            for (bag in listOf(objs.gem_bag, objs.gem_bag_open)) {
                assertTrue(objTypes[bag].members)
                assertEquals("Empty", objTypes[bag].param(params.bankside_extraop))
            }
            assertEquals(listOf(1623, 1621, 1619, 1617, 1631), GemBagStorage.accepted.map { it.id })
            assertEquals(60, GemBagStorage.PER_GEM_CAPACITY)

            val storage = cacheTypes.invs[invs.gem_bag_storage]
            assertEquals(300, storage.size)
            assertEquals(InvScope.Perm, storage.scope)
            assertEquals(InvStackType.Never, storage.stack)
            assertTrue(!storage.protect)
        }

    @Test
    fun GameTestState.`fill routes every suitable inventory gem up to independent caps`() =
        runGemBagTest { deps ->
            preparePlayer()
            add(objs.gem_bag)
            add(objs.uncut_sapphire, 4)
            add(objs.uncut_emerald, 2)
            val bagSlot = player.inv.indexOfFirst { it?.id == objs.gem_bag.id }

            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op1) }
            assertEquals(4, deps.storage.count(player, objs.uncut_sapphire))
            assertEquals(2, deps.storage.count(player, objs.uncut_emerald))
            assertMessageSent(GemBagStorage.ADD_MANY_MESSAGE)

            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op1) }
            assertMessageSent(GemBagStorage.NOTHING_SUITABLE_MESSAGE)

            preparePlayer()
            add(objs.gem_bag_open)
            for (gem in GemBagStorage.accepted) {
                assertEquals(
                    60,
                    deps.storage.storeAcquired(player, gem, count = 61),
                    gem.toString(),
                )
                assertEquals(0, deps.storage.storeAcquired(player, gem), gem.toString())
                assertEquals(60, deps.storage.count(player, gem), gem.toString())
            }
        }

    @Test
    fun GameTestState.`open close and check preserve exact per-gem contents`() =
        runGemBagTest { deps ->
            preparePlayer()
            add(objs.gem_bag)
            val bagSlot = player.inv.indexOfFirst { it?.id == objs.gem_bag.id }

            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op2) }
            assertEquals(objs.gem_bag_open.id, player.inv[bagSlot]?.id)
            assertMessageSent(GemBagStorage.OPEN_MESSAGE)
            assertEquals(2, deps.storage.storeAcquired(player, objs.uncut_sapphire, count = 2))
            assertEquals(1, deps.storage.storeAcquired(player, objs.uncut_dragonstone))

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, bagSlot, HeldOp.Op3)
                deps.held.interact(this, player.inv, bagSlot, HeldOp.Op2)
            }
            assertMessagesSent(
                "Sapphires: 2 / Emeralds: 0 / Rubies: 0",
                "Diamonds: 0 / Dragonstones: 1",
                GemBagStorage.CLOSE_MESSAGE,
            )
            assertEquals(objs.gem_bag.id, player.inv[bagSlot]?.id)
            assertEquals(2, deps.storage.count(player, objs.uncut_sapphire))
        }

    @Test
    fun GameTestState.`held use stores uncut gem in either order and rejects cut gem`() =
        runGemBagTest { deps ->
            preparePlayer()
            add(objs.gem_bag)
            add(objs.uncut_ruby)
            add(objs.ruby)
            val bagSlot = player.inv.indexOfFirst { it?.id == objs.gem_bag.id }
            val uncutSlot = player.inv.indexOfFirst { it?.id == objs.uncut_ruby.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.gem_bag],
                    bagSlot,
                    objTypes[objs.uncut_ruby],
                    uncutSlot,
                )
            }
            assertEquals(1, deps.storage.count(player, objs.uncut_ruby))
            assertEquals(0, player.count(objs.uncut_ruby))
            assertMessageSent(GemBagStorage.ADD_ONE_MESSAGE)

            val cutSlot = player.inv.indexOfFirst { it?.id == objs.ruby.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.ruby],
                    cutSlot,
                    objTypes[objs.gem_bag],
                    bagSlot,
                )
            }
            assertEquals(1, player.count(objs.ruby))
            assertMessageSent(GemBagStorage.UNCUT_ONLY_MESSAGE)
        }

    @Test
    fun GameTestState.`inventory empty is sapphire first while bankside empty moves everything`() =
        runGemBagTest { deps ->
            preparePlayer()
            add(objs.gem_bag_open)
            for (gem in GemBagStorage.accepted.asReversed()) {
                assertEquals(1, deps.storage.storeAcquired(player, gem))
            }
            add(objs.bones, 25)
            val bagSlot = player.inv.indexOfFirst { it?.id == objs.gem_bag_open.id }

            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op4) }
            assertEquals(1, player.count(objs.uncut_sapphire))
            assertEquals(1, player.count(objs.uncut_emerald))
            assertEquals(0, player.count(objs.uncut_ruby))
            assertEquals(0, deps.storage.count(player, objs.uncut_sapphire))
            assertEquals(0, deps.storage.count(player, objs.uncut_emerald))
            assertEquals(1, deps.storage.count(player, objs.uncut_ruby))

            assertTrue(
                eventBus.publish(
                    HeldBanksideEvents.Type(player, bagSlot, objTypes[objs.gem_bag_open])
                )
            )
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            assertEquals(1, bank.count(objTypes[objs.uncut_ruby]))
            assertEquals(1, bank.count(objTypes[objs.uncut_diamond]))
            assertEquals(1, bank.count(objTypes[objs.uncut_dragonstone]))
        }

    @Test
    fun GameTestState.`membership and destroy boundaries are atomic`() = runGemBagTest { deps ->
        preparePlayer()
        player.members = false
        add(objs.gem_bag)
        add(objs.uncut_diamond)
        val bagSlot = player.inv.indexOfFirst { it?.id == objs.gem_bag.id }
        player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op1) }
        assertEquals(1, player.count(objs.uncut_diamond))
        assertEquals(0, deps.storage.count(player, objs.uncut_diamond))
        assertMessageSent(GemBagStorage.MEMBERS_MESSAGE)

        player.members = true
        player.inv[bagSlot] = InvObj(objs.gem_bag_open)
        assertEquals(3, deps.storage.storeAcquired(player, objs.uncut_diamond, count = 3))
        val bag = requireNotNull(player.inv[bagSlot])
        assertTrue(
            eventBus.publish(
                HeldDropEvents.Destroy(player, bagSlot, bag, objTypes[objs.gem_bag_open])
            )
        )
        assertEquals(0, deps.storage.count(player, objs.uncut_diamond))
    }

    @Test
    fun GameTestState.`ground take routes a complete gem stack into an open bag`() =
        runGemBagTest { deps ->
            preparePlayer()
            add(objs.gem_bag_open)
            repeat(player.inv.size - 1) { add(objs.bones) }
            val target = deps.spawn(objs.uncut_dragonstone, player.coords)

            deps.objInteractions.interact(player, target, InteractionOp.Op3)
            advance(ticks = 1)

            assertEquals(1, deps.storage.count(player, objs.uncut_dragonstone))
            assertDoesNotContain(player.inv, objs.uncut_dragonstone)
            assertFalse(deps.objRepo.del(target))
            assertMessageSent(GemBagStorage.AUTO_STORE_MESSAGE)
        }

    @Test
    fun GameTestState.`ground take preserves per-item drop provenance`() = runGemBagTest { deps ->
        preparePlayer()
        val origin = player.coords
        val dropped =
            InvObj(objs.ensouled_unicorn_head_initial, vars = FreshDropOrigin.pack(origin))
        val target = deps.spawn(dropped, origin)

        deps.objInteractions.interact(player, target, InteractionOp.Op3)
        advance(ticks = 1)

        assertEquals(dropped, player.inv[0])
        assertFalse(deps.objRepo.del(target))
    }

    @Test
    fun GameTestState
        .`ground take preserves delayed pickup and closed member capacity fallbacks`() =
        runGemBagTest { deps ->
            preparePlayer()
            add(objs.gem_bag_open)
            val target = deps.spawn(objs.uncut_sapphire, player.coords.translateX(1))

            deps.objInteractions.interact(player, target, InteractionOp.Op3)
            advance(ticks = 1)
            assertEquals(0, deps.storage.count(player, objs.uncut_sapphire))
            assertTrue(deps.objRepo.findAll(target.coords).any { it === target })
            advance(ticks = 1)
            assertEquals(1, deps.storage.count(player, objs.uncut_sapphire))
            assertTrue(deps.objRepo.findAll(target.coords).none { it === target })
            assertMessageSent(GemBagStorage.AUTO_STORE_MESSAGE)

            val closed = player.inv.indexOfFirst { it?.id == objs.gem_bag_open.id }
            player.inv[closed] = InvObj(objs.gem_bag)
            val inventoryTarget = deps.spawn(objs.uncut_sapphire, player.coords)
            deps.objInteractions.interact(player, inventoryTarget, InteractionOp.Op3)
            advance(ticks = 1)
            assertContains(player.inv, objs.uncut_sapphire)
            assertEquals(1, deps.storage.count(player, objs.uncut_sapphire))

            player.inv[closed] = InvObj(objs.gem_bag_open)
            assertEquals(60, deps.storage.storeAcquired(player, objs.uncut_emerald, count = 60))
            while (player.inv.hasFreeSpace()) {
                add(objs.bones)
            }
            val fullTarget = deps.spawn(objs.uncut_emerald, player.coords)
            deps.objInteractions.interact(player, fullTarget, InteractionOp.Op3)
            advance(ticks = 1)
            assertTrue(deps.objRepo.del(fullTarget))
            assertMessageSent(Constants.dm_take_invspace)
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
    }

    private fun GameTestScope.add(type: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(type)
        }
    }

    private fun GameTestState.runGemBagTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            GemBagScript::class,
            ObjTakePlugin::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(ObjTakeModule())
            install(GemBagModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val storage: GemBagStorage,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
        val objInteractions: ObjInteractions,
        val objRepo: ObjRepository,
    ) {
        fun spawn(type: ObjType, coords: org.rsmod.map.CoordGrid, count: Int = 1): Obj =
            objRepo.add(type = type, coords = coords, duration = Int.MAX_VALUE, count = count)

        fun spawn(obj: InvObj, coords: org.rsmod.map.CoordGrid): Obj =
            objRepo.add(invObj = obj, coords = coords, duration = Int.MAX_VALUE)
    }
}
