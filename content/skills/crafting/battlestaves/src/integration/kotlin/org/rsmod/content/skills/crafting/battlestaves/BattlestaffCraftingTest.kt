package org.rsmod.content.skills.crafting.battlestaves

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.righthand
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class BattlestaffCraftingTest {
    @Test
    fun GameTestState.`cache 233 exposes elemental battlestaff objects animation and effects`() =
        runBattlestaffTest { deps ->
            assertEquals(1391, objs.battlestaff.id)
            assertEquals(listOf(571, 575, 569, 573), deps.recipes.recipes.map { it.orb.id })
            assertEquals(listOf(1395, 1399, 1393, 1397), deps.recipes.recipes.map { it.output.id })
            assertEquals(7531, seqs.human_battlestaff_crafting.id)
            assertEquals(listOf(1370, 1371, 1372, 306), deps.recipes.recipes.map { it.spotanim.id })
        }

    @Test
    fun GameTestState.`cache 233 pins Bryophyta crafting and free world equipment contracts`() =
        runBattlestaffTest { deps ->
            assertEquals(22372, objs.bryophytas_essence.id)
            assertEquals(22368, objs.bryophytas_staff_uncharged.id)
            assertEquals(22370, objs.bryophytas_staff.id)
            assertFalse(objTypes[objs.bryophytas_essence].members)
            assertFalse(objTypes[objs.bryophytas_staff_uncharged].members)
            assertFalse(objTypes[objs.bryophytas_staff].members)
            for (staff in listOf(objs.bryophytas_staff_uncharged, objs.bryophytas_staff)) {
                val type = objTypes[staff]
                assertEquals(stats.attack, type.param(params.statreq1_skill))
                assertEquals(30, type.param(params.statreq1_level))
                assertEquals(stats.magic, type.param(params.statreq2_skill))
                assertEquals(30, type.param(params.statreq2_level))
            }
            assertEquals(62, BryophytasStaffCrafting.CRAFTING_LEVEL)
        }

    @Test
    fun GameTestState
        .`Bryophyta recipe consumes both selected inputs in a full inventory for zero xp`() =
        runBattlestaffTest { deps ->
            preparePlayer()
            repeat(player.inv.size) { player.inv[it] = InvObj(objs.beer) }
            player.inv[3] = InvObj(objs.battlestaff)
            player.inv[17] = InvObj(objs.bryophytas_essence)
            val startXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess {
                deps.bryophytasStaff.craft(this, battlestaffSlot = 3, essenceSlot = 17)
            }

            assertTrue(player.inv[3].isType(objs.bryophytas_staff_uncharged))
            assertEquals(0, player.count(objs.battlestaff))
            assertEquals(0, player.count(objs.bryophytas_essence))
            assertEquals(startXp, player.statMap.getFineXP(stats.crafting))
        }

    @Test
    fun GameTestState.`Bryophyta recipe dispatches in both use orders`() =
        runBattlestaffTest { deps ->
            for (essenceFirst in listOf(false, true)) {
                preparePlayer()
                player.inv[4] = InvObj(objs.battlestaff)
                player.inv[11] = InvObj(objs.bryophytas_essence)
                player.withProtectedAccess {
                    if (essenceFirst) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            objTypes[objs.bryophytas_essence],
                            11,
                            objTypes[objs.battlestaff],
                            4,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            objTypes[objs.battlestaff],
                            4,
                            objTypes[objs.bryophytas_essence],
                            11,
                        )
                    }
                }
                assertTrue(player.inv[4].isType(objs.bryophytas_staff_uncharged))
            }
        }

    @Test
    fun GameTestState.`Bryophyta recipe membership level and stale input failures are atomic`() =
        runBattlestaffTest { deps ->
            preparePlayer()
            player.members = false
            player.inv[2] = InvObj(objs.battlestaff)
            player.inv[8] = InvObj(objs.bryophytas_essence)
            player.withProtectedAccess {
                deps.bryophytasStaff.craft(this, battlestaffSlot = 2, essenceSlot = 8)
            }
            assertMessageSent("You need to be on a members' world to make this staff.")
            assertBryophytaInputsUnchanged()

            preparePlayer()
            player.stats[stats.crafting] = 61
            player.inv[2] = InvObj(objs.battlestaff)
            player.inv[8] = InvObj(objs.bryophytas_essence)
            player.withProtectedAccess {
                deps.bryophytasStaff.craft(this, battlestaffSlot = 2, essenceSlot = 8)
            }
            assertMessageSent("You need a Crafting level of 62 to make this item.")
            assertBryophytaInputsUnchanged()

            preparePlayer()
            player.inv[2] = InvObj(objs.battlestaff)
            player.inv[8] = InvObj(objs.beer)
            player.withProtectedAccess {
                deps.bryophytasStaff.craft(this, battlestaffSlot = 2, essenceSlot = 8)
            }
            assertEquals(1, player.count(objs.battlestaff))
            assertEquals(0, player.count(objs.bryophytas_staff_uncharged))
        }

    @Test
    fun GameTestState.`both Bryophyta variants require base Attack and Magic 30 and work in F2P`() =
        runBattlestaffTest { deps ->
            for (staff in listOf(objs.bryophytas_staff_uncharged, objs.bryophytas_staff)) {
                preparePlayer()
                player.members = false
                player.stats[stats.attack] = 29
                player.stats[stats.magic] = 30
                player.inv[4] = InvObj(staff, vars = if (staff == objs.bryophytas_staff) 17 else 0)
                assertInstanceOf<HeldEquipResult.Fail.StatRequirements>(
                    deps.equip.equip(player, invSlot = 4, inventory = player.inv)
                )
                assertTrue(player.inv[4].isType(staff))

                player.stats[stats.attack] = 30
                player.stats[stats.magic] = 29
                assertInstanceOf<HeldEquipResult.Fail.StatRequirements>(
                    deps.equip.equip(player, invSlot = 4, inventory = player.inv)
                )
                assertTrue(player.inv[4].isType(staff))

                player.stats[stats.magic] = 30
                assertInstanceOf<HeldEquipResult.Success>(
                    deps.equip.equip(player, invSlot = 4, inventory = player.inv)
                )
                assertTrue(player.righthand.isType(staff))
                assertEquals(if (staff == objs.bryophytas_staff) 17 else 0, player.righthand?.vars)
            }
        }

    @Test
    fun GameTestState.`recipe table pins exact levels materials outputs xp and membership`() =
        runBattlestaffTest { deps ->
            assertEquals(
                listOf(
                    "Water battlestaff:54:571:1395:100.0",
                    "Earth battlestaff:58:575:1399:112.5",
                    "Fire battlestaff:62:569:1393:125.0",
                    "Air battlestaff:66:573:1397:137.5",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.orb.id}:${it.output.id}:${it.xp}"
                },
            )
            assertTrue(deps.recipes.recipes.all { objTypes[it.orb].members })
            assertTrue(deps.recipes.recipes.all { objTypes[it.output].members })
            assertTrue(objTypes[objs.battlestaff].members)
        }

    @Test
    fun GameTestState.`orb and staff open the native one-product quantity menu`() =
        runBattlestaffTest { deps ->
            preparePlayer()
            add(objs.water_orb, 3)
            add(objs.battlestaff, 2)

            player.withProtectedAccess { deps.crafting.select(this, objs.water_orb) }

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 270 }
            assertEquals(components.chatbox_chatmodal.id, open.destinationCombinedId)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    objs.water_battlestaff.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    2,
                    "How many would you like to make?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().single {
                    it.interfaceId == 270 &&
                        it.componentId == (components.skillmulti_a.id and 0xFFFF)
                }
            assertEquals(1, events.start)
            assertEquals(2, events.end)
        }

    @Test
    fun GameTestState.`all four recipes consume exact materials and award exact xp and effects`() =
        runBattlestaffTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(objs.battlestaff)
                add(recipe.orb)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess { deps.crafting.start(this, recipe, quantity = 1) }

                assertEquals(seqs.human_battlestaff_crafting.id, player.pendingSequence.id)
                assertEquals(
                    recipe.spotanim.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                advance(3)
                assertEquals(0, player.count(objs.battlestaff), recipe.name)
                assertEquals(0, player.count(recipe.orb), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`make x starts at three ticks then repeats every two ticks`() =
        runBattlestaffTest { deps ->
            preparePlayer()
            add(objs.battlestaff, 3)
            add(objs.air_orb, 3)
            val recipe = requireNotNull(deps.recipes.forOrb(objs.air_orb))

            player.withProtectedAccess { deps.crafting.start(this, recipe, quantity = 3) }

            advance(2)
            assertEquals(0, player.count(objs.air_battlestaff))
            advance()
            assertEquals(1, player.count(objs.air_battlestaff))
            advance()
            assertEquals(1, player.count(objs.air_battlestaff))
            advance()
            assertEquals(2, player.count(objs.air_battlestaff))
            advance(2)
            assertEquals(3, player.count(objs.air_battlestaff))
            assertEquals(0, player.weakQueueList.count(crafting_queues.battlestaffCrafting))
        }

    @Test
    fun GameTestState.`membership level staff and orb failures are atomic`() =
        runBattlestaffTest { deps ->
            val water = requireNotNull(deps.recipes.forOrb(objs.water_orb))

            preparePlayer()
            player.members = false
            add(objs.battlestaff)
            add(objs.water_orb)
            player.withProtectedAccess { deps.crafting.start(this, water, quantity = 1) }
            assertMessageSent("You need to be on a members' world to make battlestaves.")
            assertEquals(1, player.count(objs.battlestaff))
            assertEquals(1, player.count(objs.water_orb))

            preparePlayer()
            player.stats[stats.crafting] = 53
            add(objs.battlestaff)
            add(objs.water_orb)
            player.withProtectedAccess { deps.crafting.start(this, water, quantity = 1) }
            assertMessageSent("You need a Crafting level of 54 to make this item.")
            assertEquals(1, player.count(objs.battlestaff))
            assertEquals(1, player.count(objs.water_orb))

            preparePlayer()
            add(objs.water_orb)
            player.withProtectedAccess { deps.crafting.start(this, water, quantity = 1) }
            assertMessageSent("You need a battlestaff to attach the orb to.")
            assertEquals(1, player.count(objs.water_orb))

            preparePlayer()
            add(objs.battlestaff)
            player.withProtectedAccess { deps.crafting.start(this, water, quantity = 1) }
            assertMessageSent("You need an elemental orb to make this battlestaff.")
            assertEquals(1, player.count(objs.battlestaff))
        }

    @Test
    fun GameTestState.`movement clicks material loss and other Crafting actions stop the batch`() =
        runBattlestaffTest { deps ->
            val fire = requireNotNull(deps.recipes.forOrb(objs.fire_orb))
            preparePlayer()
            add(objs.battlestaff, 2)
            add(objs.fire_orb, 2)
            player.withProtectedAccess {
                weakQueue(crafting_queues.leatherworking, 20)
                deps.crafting.start(this, fire, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.leatherworking))
            advance(3)
            assertEquals(1, player.count(objs.fire_battlestaff))
            // Movement packet handlers clear pending actions before installing their route.
            player.clearPendingAction()
            advance(2)
            assertEquals(1, player.count(objs.fire_battlestaff))
            assertEquals(1, player.count(objs.fire_orb))
            assertEquals(0, player.weakQueueList.count(crafting_queues.battlestaffCrafting))

            preparePlayer()
            add(objs.battlestaff, 2)
            add(objs.fire_orb, 2)
            player.withProtectedAccess { deps.crafting.start(this, fire, quantity = 2) }
            advance(3)
            player.inv[player.inv.indexOfFirst { it?.id == objs.fire_orb.id }] = null
            advance(2)
            assertEquals(1, player.count(objs.fire_battlestaff))
            assertEquals(1, player.count(objs.battlestaff))
            assertEquals(0, player.weakQueueList.count(crafting_queues.battlestaffCrafting))
        }

    @Test
    fun GameTestState
        .`held interaction accepts reverse order and dispatches the selected quantity`() =
        runBattlestaffTest { deps ->
            preparePlayer()
            add(objs.earth_orb, 2)
            add(objs.battlestaff, 2)
            val orbSlot = player.inv.indexOfFirst { it?.id == objs.earth_orb.id }
            val staffSlot = player.inv.indexOfFirst { it?.id == objs.battlestaff.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.battlestaff],
                    staffSlot,
                    objTypes[objs.earth_orb],
                    orbSlot,
                )
            }
            resumeSkillMulti(components.skillmulti_a, quantity = 2)
            advance(5)

            assertEquals(2, player.count(objs.earth_battlestaff))
            assertEquals(0, player.count(objs.earth_orb))
            assertEquals(0, player.count(objs.battlestaff))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
    }

    private fun GameTestScope.assertBryophytaInputsUnchanged() {
        assertEquals(1, player.count(objs.battlestaff))
        assertEquals(1, player.count(objs.bryophytas_essence))
        assertEquals(0, player.count(objs.bryophytas_staff_uncharged))
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

    private fun GameTestState.runBattlestaffTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            BattlestaffCraftingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(BattlestaffCraftingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crafting: BattlestaffCrafting,
        val bryophytasStaff: BryophytasStaffCrafting,
        val recipes: BattlestaffRecipeRegistry,
        val heldU: HeldUInteractions,
        val equip: HeldEquipOp,
    )
}
