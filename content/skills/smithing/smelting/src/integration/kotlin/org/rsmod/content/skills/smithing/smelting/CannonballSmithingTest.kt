package org.rsmod.content.skills.smithing.smelting

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class CannonballSmithingTest {
    @Test
    fun GameTestState.`cache 233 cannonball contract opens the one-product skill-multi menu`() =
        runCannonballTest { deps ->
            assertEquals(2, objs.cannonball.id)
            assertEquals(4, objs.ammo_mould.id)
            assertEquals(27012, objs.double_ammo_mould.id)
            assertEquals(827, seqs.human_smith_cannonball.id)
            assertEquals(0, varps.mcannon.id)
            assertEquals(25.6, deps.recipes.regular.xp)
            assertEquals(51.2, deps.recipes.double.xp)

            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.ammo_mould)
            add(objs.steel_bar, 2)

            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.steel_bar)
            }

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 270 }
            assertEquals(components.chatbox_chatmodal.id, open.destinationCombinedId)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    13,
                    2,
                    objs.cannonball.id,
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
        }

    @Test
    fun GameTestState.`regular mould makes four balls after eight ticks then repeats after ten`() =
        runCannonballTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.ammo_mould)
            add(objs.steel_bar, 2)
            val startXp = player.statMap.getFineXP(stats.smithing)

            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.steel_bar)
            }
            resumeSkillMulti(quantity = 2)
            assertMessageSent("The molten metal cools slowly to form 4 cannonballs.")

            advance(7)
            assertEquals(0, player.count(objs.cannonball))
            advance()
            assertEquals(4, player.count(objs.cannonball))
            assertEquals(256, player.statMap.getFineXP(stats.smithing) - startXp)
            assertMessageSent("You remove the cannonballs from the mould")

            advance(9)
            assertEquals(4, player.count(objs.cannonball))
            advance()
            assertEquals(8, player.count(objs.cannonball))
            assertEquals(512, player.statMap.getFineXP(stats.smithing) - startXp)
            assertEquals(0, player.weakQueueList.count(furnace_queues.cannonballs))
        }

    @Test
    fun GameTestState.`double mould has steel-bar priority and makes eight from two bars`() =
        runCannonballTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.ammo_mould)
            add(objs.double_ammo_mould)
            add(objs.steel_bar, 4)
            val startXp = player.statMap.getFineXP(stats.smithing)

            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.steel_bar)
            }
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(2, setup.values[1])
            resumeSkillMulti(quantity = 1)
            assertMessageSent("The molten metal cools slowly to form 8 cannonballs.")
            advance(8)

            assertEquals(8, player.count(objs.cannonball))
            assertEquals(2, player.count(objs.steel_bar))
            assertEquals(512, player.statMap.getFineXP(stats.smithing) - startXp)
        }

    @Test
    fun GameTestState.`membership quest level mould and material failures are atomic`() =
        runCannonballTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.ammo_mould)
            add(objs.steel_bar)
            player.members = false
            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.steel_bar)
            }
            assertMessageSent("You need to be on a members' world to make cannonballs.")
            assertEquals(1, player.count(objs.steel_bar))

            preparePlayer()
            add(objs.ammo_mould)
            add(objs.steel_bar)
            player.setVarp(varps.mcannon, 9)
            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.steel_bar)
            }
            assertMessageSent("You need to progress further in Dwarf Cannon to make cannonballs.")

            preparePlayer()
            add(objs.ammo_mould)
            add(objs.steel_bar)
            player.stats[stats.smithing] = 34
            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.steel_bar)
            }
            assertMessageSent("You need a Smithing level of 35 to make cannonballs.")

            preparePlayer()
            add(objs.steel_bar)
            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.steel_bar)
            }
            assertMessageSent("You need an ammo mould to make cannonballs.")

            preparePlayer()
            add(objs.double_ammo_mould)
            add(objs.steel_bar)
            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.double_ammo_mould)
            }
            assertMessageSent("You do not have enough steel bars to make cannonballs.")
            assertEquals(1, player.count(objs.steel_bar))
        }

    @Test
    fun GameTestState.`Shilo charges once per action and Lovakite furnace rejects cannonballs`() =
        runCannonballTest { deps ->
            val shilo = prepareFurnace(furnace_locs.shilo_village)
            preparePlayer()
            add(objs.double_ammo_mould)
            add(objs.steel_bar, 2)
            add(objs.coins, 20)

            player.withProtectedAccess {
                deps.cannonballs.process(
                    this,
                    CannonballSmithingRequest(
                        shilo,
                        FurnaceKind.ShiloVillage,
                        objs.double_ammo_mould,
                        remaining = 1,
                    ),
                )
            }
            assertEquals(8, player.count(objs.cannonball))
            assertEquals(0, player.count(objs.coins))

            val lovakite = prepareFurnace(furnace_locs.lovakite, offset = 4)
            preparePlayer()
            add(objs.ammo_mould)
            add(objs.steel_bar)
            player.withProtectedAccess {
                deps.cannonballs.select(this, lovakite, FurnaceKind.Lovakite, objs.steel_bar)
            }
            assertMessageSent("This furnace is not at the right temperature to make cannonballs.")
            assertEquals(1, player.count(objs.steel_bar))
        }

    @Test
    fun GameTestState
        .`movement and material changes stop a cannonball batch without duplication`() =
        runCannonballTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.ammo_mould)
            add(objs.steel_bar, 2)

            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.steel_bar)
            }
            resumeSkillMulti(quantity = 2)
            advance(8)
            assertEquals(4, player.count(objs.cannonball))

            player.placeAt(furnace.coords.translateX(4))
            advance(10)
            assertEquals(4, player.count(objs.cannonball))
            assertEquals(1, player.count(objs.steel_bar))
            assertEquals(0, player.weakQueueList.count(furnace_queues.cannonballs))

            player.placeAt(furnace.coords.translateX(-1))
            player.withProtectedAccess {
                deps.cannonballs.select(this, furnace, FurnaceKind.Standard, objs.steel_bar)
            }
            resumeSkillMulti(quantity = 1)
            player.inv[player.inv.indexOfFirst { it?.id == objs.steel_bar.id }] = null
            advance(8)
            assertEquals(4, player.count(objs.cannonball))
        }

    private fun GameTestScope.prepareFurnace(
        type: org.rsmod.game.type.loc.LocType,
        offset: Int = 0,
    ): BoundLocInfo {
        val furnace = placeMapLoc(CoordGrid(0, 50, 50, 34 + offset, 31), type)
        player.placeAt(furnace.coords.translateX(-1))
        return furnace
    }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.smithing] = 99
        player.setVarp(varps.mcannon, 10)
        player.setVarBit(varbits.karamja_diary_elite_complete, 0)
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

    private fun GameTestScope.resumeSkillMulti(quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.skillmulti_a, subcomponent = quantity)
        )
    }

    private fun GameTestState.runCannonballTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            FurnaceSmeltingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(SmeltingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(val cannonballs: CannonballSmithing, val recipes: CannonballRecipeRegistry)
}
