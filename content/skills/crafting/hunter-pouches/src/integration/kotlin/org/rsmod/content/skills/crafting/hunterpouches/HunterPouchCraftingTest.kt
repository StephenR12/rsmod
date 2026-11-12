package org.rsmod.content.skills.crafting.hunterpouches

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.LeatherThreadUsageRepository
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.content.skills.crafting.leatherworking.Leatherworking
import org.rsmod.content.skills.crafting.leatherworking.LeatherworkingModule
import org.rsmod.content.skills.crafting.leatherworking.LeatherworkingRecipeRegistry
import org.rsmod.content.skills.crafting.leatherworking.LeatherworkingScript
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class HunterPouchCraftingTest {
    @Test
    fun GameTestState
        .`storage registry pins cache 233 capacities resources and permanent inventories`() =
        runHunterPouchTest { deps ->
            assertEquals(
                listOf(14, 21, 28, 14, 28),
                deps.storageRegistry.descriptors.map { it.capacity },
            )
            assertEquals(
                setOf(
                    10095,
                    10099,
                    10103,
                    10115,
                    10117,
                    10119,
                    10121,
                    10123,
                    10125,
                    10127,
                    29163,
                    29174,
                    29177,
                    29218,
                ),
                HunterPouchFamily.Fur.accepted.mapTo(HashSet()) { it.id },
            )
            assertEquals(
                setOf(29104, 29122, 29101, 29119, 29125, 29110, 29107, 29116, 29113),
                HunterPouchFamily.Meat.accepted.mapTo(HashSet()) { it.id },
            )
            for (storage in listOf(invs.hunter_fur_pouch_storage, invs.hunter_meat_pouch_storage)) {
                val type = cacheTypes.invs[storage]
                assertEquals(28, type.size)
                assertEquals(InvScope.Perm, type.scope)
                assertEquals(InvStackType.Never, type.stack)
                assertTrue(!type.protect)
            }
        }

    @Test
    fun GameTestState.`held fill uses tier capacity and excludes tatty fur`() =
        runHunterPouchTest { deps ->
            preparePlayer()
            add(objs.small_fur_pouch)
            add(objs.larupia_fur, 20)
            add(objs.tatty_larupia_fur)
            val pouchSlot = player.inv.indexOfFirst { it?.id == objs.small_fur_pouch.id }

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, pouchSlot, HeldOp.Op1)
            }

            assertEquals(14, deps.storage.count(player, HunterPouchFamily.Fur))
            assertEquals(6, player.count(objs.larupia_fur))
            assertEquals(1, player.count(objs.tatty_larupia_fur))

            add(objs.small_fur_pouch)
            val secondPouch = player.inv.indexOfLast { it?.id == objs.small_fur_pouch.id }
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, secondPouch, HeldOp.Op1)
            }
            assertEquals(14, deps.storage.count(player, HunterPouchFamily.Fur))
            assertEquals(6, player.count(objs.larupia_fur))
            assertMessageSent("Your pouch is full.")
        }

    @Test
    fun GameTestState.`open close and check preserve shared contents`() =
        runHunterPouchTest { deps ->
            preparePlayer()
            add(objs.medium_fur_pouch)
            repeat(5) { deps.storage.storeAcquired(player, objs.fox_fur) }
            assertEquals(0, deps.storage.count(player, HunterPouchFamily.Fur))
            val pouchSlot = player.inv.indexOfFirst { it?.id == objs.medium_fur_pouch.id }

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, pouchSlot, HeldOp.Op4)
            }
            assertEquals(objs.medium_fur_pouch_open.id, player.inv[pouchSlot]?.id)
            assertEquals(5, deps.storage.storeAcquired(player, objs.fox_fur, count = 5))

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, pouchSlot, HeldOp.Op3)
                deps.held.interact(this, player.inv, pouchSlot, HeldOp.Op4)
            }
            assertMessageSent("Your fur pouch contains 5 furs")
            assertEquals(objs.medium_fur_pouch.id, player.inv[pouchSlot]?.id)
            assertEquals(5, deps.storage.count(player, HunterPouchFamily.Fur))
        }

    @Test
    fun GameTestState
        .`automatic acquisition selects largest open pouch and rejects raw bird meat`() =
        runHunterPouchTest { deps ->
            preparePlayer()
            add(objs.small_meat_pouch_open)
            add(objs.large_meat_pouch_open)

            assertEquals(28, deps.storage.storeAcquired(player, objs.wild_kebbit_meat, count = 40))
            assertEquals(28, deps.storage.count(player, HunterPouchFamily.Meat))
            assertEquals(0, deps.storage.storeAcquired(player, objs.raw_bird_meat, count = 1))
            assertEquals(0, deps.storage.count(player, HunterPouchFamily.Fur))
        }

    @Test
    fun GameTestState.`bankside empty transfers one family to bank without touching the other`() =
        runHunterPouchTest { deps ->
            preparePlayer()
            add(objs.large_fur_pouch_open)
            add(objs.large_meat_pouch_open)
            assertEquals(4, deps.storage.storeAcquired(player, objs.jaguar_fur, count = 4))
            assertEquals(3, deps.storage.storeAcquired(player, objs.kyatt_meat, count = 3))
            assertTrue(
                eventBus.publish(
                    HeldBanksideEvents.Type(
                        player,
                        player.inv.indexOfFirst { it?.id == objs.large_fur_pouch_open.id },
                        objTypes[objs.large_fur_pouch_open],
                    )
                )
            )

            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            assertEquals(4, bank.count(objs.jaguar_fur))
            assertEquals(0, deps.storage.count(player, HunterPouchFamily.Fur))
            assertEquals(3, deps.storage.count(player, HunterPouchFamily.Meat))
        }

    @Test
    fun GameTestState.`destroying any family pouch clears its shared contents`() =
        runHunterPouchTest { deps ->
            preparePlayer()
            add(objs.small_fur_pouch_open)
            assertEquals(7, deps.storage.storeAcquired(player, objs.dark_kebbit_fur, count = 7))
            val slot = player.inv.indexOfFirst { it?.id == objs.small_fur_pouch_open.id }
            val pouch = requireNotNull(player.inv[slot])

            assertTrue(
                eventBus.publish(
                    HeldDropEvents.Destroy(player, slot, pouch, objTypes[objs.small_fur_pouch_open])
                )
            )
            assertEquals(0, deps.storage.count(player, HunterPouchFamily.Fur))
        }

    @Test
    fun GameTestState.`cache 233 exposes all fur pouch objects and leather animation`() =
        runHunterPouchTest { deps ->
            assertEquals(
                listOf(10095, 10099, 10103, 29163, 29177),
                deps.recipes.recipes.map { it.fur.id },
            )
            assertEquals(
                listOf(29299, 29301, 29303, 29295, 29297),
                deps.recipes.recipes.map { it.output.id },
            )
            assertEquals(
                listOf(29466, 29468, 29470, 29462, 29464),
                listOf(
                    objs.small_fur_pouch_open.id,
                    objs.medium_fur_pouch_open.id,
                    objs.large_fur_pouch_open.id,
                    objs.small_meat_pouch_open.id,
                    objs.large_meat_pouch_open.id,
                ),
            )
            assertEquals(1249, seqs.human_leather_crafting.id)
            for (descriptor in deps.storageRegistry.descriptors) {
                assertEquals(
                    listOf("Fill", "Empty", "Check", "Open", "Destroy"),
                    objTypes[descriptor.closed].iop.toList(),
                )
                assertEquals(
                    listOf("Fill", "Empty", "Check", "Close", "Destroy"),
                    objTypes[descriptor.open].iop.toList(),
                )
                for (variant in descriptor.variants) {
                    assertEquals("Empty", objTypes[variant].param(params.bankside_extraop))
                }
            }
        }

    @Test
    fun GameTestState.`recipe table pins exact levels materials upgrades and xp`() =
        runHunterPouchTest { deps ->
            assertEquals(
                listOf(
                    "Small fur pouch:35:10095x4:null=29299:20.0",
                    "Medium fur pouch:50:10099x3:29299=29301:40.0",
                    "Large fur pouch:65:10103x3:29301=29303:60.0",
                    "Small meat pouch:35:29163x4:null=29295:30.0",
                    "Large meat pouch:65:29177x3:29295=29297:60.0",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.fur.id}x${it.furCount}:${it.pouch?.id}=" +
                        "${it.output.id}:${it.xp}"
                },
            )
            assertTrue(deps.recipes.recipes.all { objTypes[it.fur].members })
            assertTrue(deps.recipes.recipes.all { objTypes[it.output].members })
        }

    @Test
    fun GameTestState.`fur opens native quantity menu capped by partial thread lifecycle`() =
        runHunterPouchTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.larupia_fur, 24)

            player.withProtectedAccess {
                deps.crafting.select(this, objs.larupia_fur, costumeNeedle = false)
            }

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 270 }
            assertEquals(components.chatbox_chatmodal.id, open.destinationCombinedId)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    5,
                    objs.small_fur_pouch.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    5,
                    "What would you like to make?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().single {
                    it.interfaceId == 270 &&
                        it.componentId == (components.skillmulti_a.id and 0xFFFF)
                }
            assertEquals(1, events.start)
            assertEquals(5, events.end)
        }

    @Test
    fun GameTestState.`all five recipes transform atomically after three ticks with exact xp`() =
        runHunterPouchTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(objs.costume_needle)
                add(recipe.fur, recipe.furCount)
                recipe.pouch?.let { add(it) }
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.crafting.start(this, recipe, costumeNeedle = true, quantity = 1)
                }

                assertEquals(seqs.human_leather_crafting.id, player.pendingSequence.id)
                advance(2)
                assertEquals(0, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(0, player.count(recipe.fur), recipe.name)
                recipe.pouch?.let { assertEquals(0, player.count(it), recipe.name) }
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`one ordinary spool lasts five pouch actions and shares leather progress`() =
        runHunterPouchTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.leather)
            add(objs.fox_fur, 16)
            val leatherGloves =
                requireNotNull(
                    deps.leatherRecipes.forMaterial(objs.leather).firstOrNull {
                        it.output == objs.leather_gloves
                    }
                )
            val smallMeat = requireNotNull(deps.recipes.forOutput(objs.small_meat_pouch))

            player.withProtectedAccess {
                deps.leatherworking.start(this, leatherGloves, costumeNeedle = false, quantity = 1)
            }
            advance(3)
            assertEquals(1, player.count(objs.thread))
            assertEquals(1, deps.threadUses.uses(player))

            player.withProtectedAccess {
                deps.crafting.start(this, smallMeat, costumeNeedle = false, quantity = 4)
            }
            advance(12)

            assertEquals(4, player.count(objs.small_meat_pouch))
            assertEquals(0, player.count(objs.thread))
            assertEquals(0, deps.threadUses.uses(player))
            assertEquals(0, player.weakQueueList.count(crafting_queues.hunterPouchCrafting))
        }

    @Test
    fun GameTestState.`costume needle batches need no thread and closed pouches gate upgrades`() =
        runHunterPouchTest { deps ->
            preparePlayer()
            add(objs.costume_needle)
            add(objs.larupia_fur, 8)
            val smallFur = requireNotNull(deps.recipes.forOutput(objs.small_fur_pouch))
            player.withProtectedAccess {
                deps.crafting.start(this, smallFur, costumeNeedle = true, quantity = 2)
            }
            advance(6)
            assertEquals(2, player.count(objs.small_fur_pouch))
            assertEquals(0, player.count(objs.thread))

            preparePlayer()
            add(objs.costume_needle)
            add(objs.sunlight_antelope_fur, 3)
            add(objs.small_meat_pouch_open)
            val largeMeat = requireNotNull(deps.recipes.forOutput(objs.large_meat_pouch))
            player.withProtectedAccess {
                deps.crafting.start(this, largeMeat, costumeNeedle = true, quantity = 1)
            }
            assertMessageSent("You need the correct closed pouch to make this upgrade.")
            assertEquals(3, player.count(objs.sunlight_antelope_fur))
            assertEquals(1, player.count(objs.small_meat_pouch_open))
        }

    @Test
    fun GameTestState.`membership level tool thread fur and base failures are atomic`() =
        runHunterPouchTest { deps ->
            val medium = requireNotNull(deps.recipes.forOutput(objs.medium_fur_pouch))

            preparePlayer()
            player.members = false
            add(objs.needle)
            add(objs.thread)
            add(objs.graahk_fur, 3)
            add(objs.small_fur_pouch)
            player.withProtectedAccess {
                deps.crafting.start(this, medium, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent("You need to be on a members' world to make Hunter pouches.")

            preparePlayer()
            player.stats[stats.crafting] = 49
            add(objs.costume_needle)
            add(objs.graahk_fur, 3)
            add(objs.small_fur_pouch)
            player.withProtectedAccess {
                deps.crafting.start(this, medium, costumeNeedle = true, quantity = 1)
            }
            assertMessageSent("You need a Crafting level of 50 to make this pouch.")

            preparePlayer()
            add(objs.thread)
            add(objs.graahk_fur, 3)
            add(objs.small_fur_pouch)
            player.withProtectedAccess {
                deps.crafting.start(this, medium, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent("You need a needle to make this pouch.")

            preparePlayer()
            add(objs.needle)
            add(objs.graahk_fur, 3)
            add(objs.small_fur_pouch)
            player.withProtectedAccess {
                deps.crafting.start(this, medium, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent("You need some thread to make this pouch.")

            preparePlayer()
            add(objs.costume_needle)
            add(objs.graahk_fur, 2)
            add(objs.small_fur_pouch)
            player.withProtectedAccess {
                deps.crafting.start(this, medium, costumeNeedle = true, quantity = 1)
            }
            assertMessageSent("You do not have enough of the correct fur to make this pouch.")

            preparePlayer()
            add(objs.costume_needle)
            add(objs.graahk_fur, 3)
            player.withProtectedAccess {
                deps.crafting.start(this, medium, costumeNeedle = true, quantity = 1)
            }
            assertMessageSent("You need the correct closed pouch to make this upgrade.")
            assertEquals(3, player.count(objs.graahk_fur))
        }

    @Test
    fun GameTestState.`full inventory movement tool loss and crafting replacement remain safe`() =
        runHunterPouchTest { deps ->
            val smallFur = requireNotNull(deps.recipes.forOutput(objs.small_fur_pouch))
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.larupia_fur, 8)
            add(objs.leather, 18)
            assertEquals(28, player.inv.count { it != null })

            player.withProtectedAccess {
                weakQueue(crafting_queues.weaving, 20)
                deps.crafting.start(this, smallFur, costumeNeedle = false, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.weaving))
            player.clearPendingAction()
            advance(3)
            assertEquals(0, player.count(objs.small_fur_pouch))
            assertEquals(8, player.count(objs.larupia_fur))

            player.withProtectedAccess {
                deps.crafting.start(this, smallFur, costumeNeedle = false, quantity = 2)
            }
            advance(3)
            player.inv[player.inv.indexOfFirst { it?.id == objs.needle.id }] = null
            advance(3)
            assertEquals(1, player.count(objs.small_fur_pouch))
            assertEquals(4, player.count(objs.larupia_fur))
            assertEquals(1, player.count(objs.thread))
        }

    @Test
    fun GameTestState.`held interaction accepts reverse order and creates selected pouch`() =
        runHunterPouchTest { deps ->
            preparePlayer()
            add(objs.costume_needle)
            add(objs.fox_fur, 4)
            val needleSlot = player.inv.indexOfFirst { it?.id == objs.costume_needle.id }
            val furSlot = player.inv.indexOfFirst { it?.id == objs.fox_fur.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.fox_fur],
                    furSlot,
                    objTypes[objs.costume_needle],
                    needleSlot,
                )
            }
            resumeSkillMulti(components.skillmulti_a, quantity = 1)
            advance(3)

            assertEquals(1, player.count(objs.small_meat_pouch))
            assertEquals(1, player.count(objs.costume_needle))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].stackable) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj, count)
            return
        }
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestScope.resumeSkillMulti(component: ComponentType, quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = quantity))
    }

    private fun GameTestState.runHunterPouchTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            HunterPouchCraftingScript::class,
            LeatherworkingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HunterPouchModule())
            install(LeatherworkingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crafting: HunterPouchCrafting,
        val recipes: HunterPouchRecipeRegistry,
        val threadUses: LeatherThreadUsageRepository,
        val leatherworking: Leatherworking,
        val leatherRecipes: LeatherworkingRecipeRegistry,
        val heldU: HeldUInteractions,
        val held: HeldInteractions,
        val storage: HunterPouchStorage,
        val storageRegistry: HunterPouchStorageRegistry,
    )
}
