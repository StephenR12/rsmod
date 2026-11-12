package org.rsmod.content.other.coalbag

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.back
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class CoalBagStorageTest {
    @Test
    fun GameTestState.`cache 233 pins bag identities operations storage and bankside contract`() =
        runCoalBagTest {
            assertEquals(12019, objs.coal_bag.id)
            assertEquals(24480, objs.coal_bag_open.id)
            assertEquals(
                listOf("Fill", "Open", "Check", "Empty", "Destroy"),
                objTypes[objs.coal_bag].iop.toList(),
            )
            assertEquals(
                listOf("Fill", "Close", "Check", "Empty", "Destroy"),
                objTypes[objs.coal_bag_open].iop.toList(),
            )
            for (bag in CoalBagStorage.variants) {
                assertTrue(objTypes[bag].members)
                assertEquals("Empty", objTypes[bag].param(params.bankside_extraop))
            }
            assertEquals(25..30, cacheTypes.varbits[varbits.coal_bag_storage_count].bits)
            assertEquals(27, CoalBagStorage.BASE_CAPACITY)
            assertEquals(36, CoalBagStorage.SMITHING_CAPE_CAPACITY)
        }

    @Test
    fun GameTestState.`inventory fill and held use store coal up to the ordinary cap`() =
        runCoalBagTest { deps ->
            preparePlayer()
            add(objs.coal_bag)
            repeat(27) { add(objs.coal) }
            val bagSlot = player.inv.indexOfFirst { it?.id == objs.coal_bag.id }

            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op1) }
            assertEquals(27, deps.storage.count(player))
            assertEquals(0, player.count(objs.coal))

            add(objs.coal)
            val coalSlot = player.inv.indexOfFirst { it?.id == objs.coal.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.coal],
                    coalSlot,
                    objTypes[objs.coal_bag],
                    bagSlot,
                )
            }
            assertEquals(27, deps.storage.count(player))
            assertEquals(1, player.count(objs.coal))
            assertMessageSent(CoalBagStorage.FULL_MESSAGE)
        }

    @Test
    fun GameTestState.`worn Smithing and max capes raise capacity to 36 without truncating`() =
        runCoalBagTest { deps ->
            preparePlayer()
            add(objs.coal_bag_open)
            val qualifying =
                listOf(objs.smithing_skillcape, objs.smithing_skillcape_t, objs.max_cape_worn)
            for (cape in qualifying) {
                player.setVarBit(varbits.coal_bag_storage_count, 0)
                player.back = InvObj(cape)
                assertEquals(36, deps.storage.capacity(player), cape.toString())
                assertEquals(36, deps.storage.storeMinedCoal(player, objs.coal, amount = 40))
                assertEquals(36, deps.storage.count(player))
            }

            player.back = null
            assertEquals(27, deps.storage.capacity(player))
            assertEquals(36, deps.storage.count(player))
            assertEquals(0, deps.storage.remainingCapacity(player))
            assertEquals(0, deps.storage.storeMinedCoal(player, objs.coal))

            player.setVarBit(varbits.coal_bag_storage_count, 0)
            add(objs.smithing_skillcape)
            assertEquals(27, deps.storage.capacity(player))
            assertEquals(27, deps.storage.storeMinedCoal(player, objs.coal, amount = 40))
        }

    @Test
    fun GameTestState.`open close check and partial inventory empty preserve storage`() =
        runCoalBagTest { deps ->
            preparePlayer()
            add(objs.coal_bag)
            val bagSlot = player.inv.indexOfFirst { it?.id == objs.coal_bag.id }

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, bagSlot, HeldOp.Op3)
                deps.held.interact(this, player.inv, bagSlot, HeldOp.Op2)
            }
            assertMessageSent(CoalBagStorage.EMPTY_MESSAGE)
            assertEquals(objs.coal_bag_open.id, player.inv[bagSlot]?.id)

            assertEquals(5, deps.storage.storeMinedCoal(player, objs.coal, amount = 5))
            repeat(25) { add(objs.bones) }
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, bagSlot, HeldOp.Op3)
                deps.held.interact(this, player.inv, bagSlot, HeldOp.Op4)
            }
            assertMessageSent("Your coal bag contains 5 pieces of coal.")
            assertEquals(2, player.count(objs.coal))
            assertEquals(3, deps.storage.count(player))

            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op2) }
            assertEquals(objs.coal_bag.id, player.inv[bagSlot]?.id)
            assertEquals(3, deps.storage.count(player))
        }

    @Test
    fun GameTestState.`bankside action fills from bank and empties back without loss`() =
        runCoalBagTest { deps ->
            preparePlayer()
            add(objs.coal_bag_open)
            player.back = InvObj(objs.smithing_skillcape)
            val bagSlot = player.inv.indexOfFirst { it?.id == objs.coal_bag_open.id }
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            bank[0] = InvObj(objs.coal, 40)

            assertTrue(
                eventBus.publish(
                    HeldBanksideEvents.Type(player, bagSlot, objTypes[objs.coal_bag_open])
                )
            )
            assertEquals(36, deps.storage.count(player))
            assertEquals(4, bank.count(objTypes[objs.coal]))

            assertTrue(
                eventBus.publish(
                    HeldBanksideEvents.Type(player, bagSlot, objTypes[objs.coal_bag_open])
                )
            )
            assertEquals(0, deps.storage.count(player))
            assertEquals(40, bank.count(objTypes[objs.coal]))
        }

    @Test
    fun GameTestState.`membership and nonempty destroy boundaries are atomic`() =
        runCoalBagTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.coal_bag)
            add(objs.coal)
            val bagSlot = player.inv.indexOfFirst { it?.id == objs.coal_bag.id }

            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op1) }
            assertEquals(0, deps.storage.count(player))
            assertEquals(1, player.count(objs.coal))
            assertMessageSent(CoalBagStorage.MEMBERS_MESSAGE)

            player.members = true
            player.inv[bagSlot] = InvObj(objs.coal_bag_open)
            assertEquals(1, deps.storage.storeMinedCoal(player, objs.coal))
            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op5) }
            assertEquals(1, deps.storage.count(player))
            assertEquals(objs.coal_bag_open.id, player.inv[bagSlot]?.id)
            assertMessageSent(CoalBagStorage.NONEMPTY_DESTROY_MESSAGE)

            player.setVarBit(varbits.coal_bag_storage_count, 0)
            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op5) }
            assertEquals(objs.coal_bag_open.id, player.inv[bagSlot]?.id)
            resumeDestroyConfirmation(confirm = false)
            advance()
            assertEquals(objs.coal_bag_open.id, player.inv[bagSlot]?.id)

            player.withProtectedAccess { deps.held.interact(this, player.inv, bagSlot, HeldOp.Op5) }
            resumeDestroyConfirmation(confirm = true)
            advance()
            assertFalse(player.inv.contains(objs.coal_bag_open))
        }

    private fun GameTestScope.resumeDestroyConfirmation(confirm: Boolean) {
        val target =
            requireNotNull(
                player.ui.modals.getComponent(UserInterface(interfaces.destroy_obj_dialogue.id))
            )
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(
                components.confirmdestroy_pbutton,
                subcomponent = if (confirm) 1 else 0,
            )
        )
    }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.back = null
        player.setVarBit(varbits.coal_bag_storage_count, 0)
    }

    private fun GameTestScope.add(type: ObjType) {
        val slot = player.inv.indexOfFirst { it == null }
        require(slot >= 0)
        player.inv[slot] = InvObj(type)
    }

    private fun GameTestState.runCoalBagTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, CoalBagScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(CoalBagModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val storage: CoalBagStorage,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
    )
}
