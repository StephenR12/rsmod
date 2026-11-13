package org.rsmod.content.skills.smithing.smelting

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.back
import org.rsmod.api.player.hands
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.player.ring
import org.rsmod.api.player.torso
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class FurnaceSmeltingTest {
    @Test
    fun GameTestState.`native worn forging ring Check preserves equipment and Remove works`() =
        runSmeltingTest { d ->
            player.members = true
            player.ring = InvObj(objs.ring_of_forging)
            assertEquals("Check", d.objs[objs.ring_of_forging].param(params.wear_op1))
            player.withProtectedAccess { d.worn.interact(this, player.worn, 12, IfButtonOp.Op2) }
            assertMessageSent("Your ring of forging has 140 charges remaining.")
            assertEquals(objs.ring_of_forging.id, player.ring?.id)
            player.withProtectedAccess { d.worn.interact(this, player.worn, 12, IfButtonOp.Op1) }
            assertEquals(null, player.ring)
            assertEquals(1, player.count(objs.ring_of_forging))
        }

    @Test
    fun GameTestState.`cache 233 exposes every ordinary smelt furnace on its exact operation`() =
        runSmeltingTest { _ ->
            assertEquals(
                setOf(
                    2030,
                    3294,
                    4304,
                    6189,
                    11010,
                    11978,
                    12100,
                    12655,
                    12809,
                    16469,
                    16657,
                    21303,
                    24009,
                    26300,
                    28562,
                    28565,
                    30158,
                    33504,
                    34342,
                    36555,
                    39241,
                    43895,
                    47927,
                    48171,
                    50698,
                    51496,
                    51509,
                    56366,
                ),
                furnace_locs.all.mapTo(mutableSetOf()) { it.id },
            )
            assertEquals("Smelt", locTypes[furnace_locs.lovakite_large].op[0])
            for (furnace in furnace_locs.all - furnace_locs.lovakite_large) {
                assertEquals("Smelt", locTypes[furnace].op[1], furnace.toString())
            }
        }

    @Test
    fun GameTestState.`standard furnace uses the cache skill-multi contract and exact timing`() =
        runSmeltingTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.copper_ore, 2)
            add(objs.tin_ore, 2)

            player.withProtectedAccess { deps.smelting.select(this, furnace, FurnaceKind.Standard) }

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 270 }
            assertEquals(components.chatbox_chatmodal.id, open.destinationCombinedId)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    13,
                    2,
                    objs.bronze_bar.id,
                    objs.iron_bar.id,
                    objs.silver_bar.id,
                    objs.steel_bar.id,
                    objs.gold_bar.id,
                    objs.mithril_bar.id,
                    objs.adamantite_bar.id,
                    objs.runite_bar.id,
                    -1,
                    -1,
                    2,
                    "What would you like to smelt?",
                ),
                setup.values,
            )

            resumeSkillMulti(components.skillmulti_a, quantity = 2)
            advance(3)
            assertEquals(0, player.count(objs.bronze_bar))
            advance()
            assertEquals(1, player.count(objs.bronze_bar))
            advance(4)
            assertEquals(1, player.count(objs.bronze_bar))
            advance()
            assertEquals(2, player.count(objs.bronze_bar))
            assertEquals(0, player.weakQueueList.count(furnace_queues.smelting))
        }

    @Test
    fun GameTestState.`every metal-bar recipe consumes exact materials and grants exact xp`() =
        runSmeltingTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            val lovakiteFurnace = prepareFurnace(furnace_locs.lovakite, offset = 4)
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                player.setVarp(varps.squire, 7)
                player.setVarBit(varbits.lunar_quest_main, 155)
                recipe.oreCosts.forEach { (ore, count) -> add(ore, count) }
                add(objs.coal, recipe.coalCount)
                val startXp = player.statMap.getFineXP(stats.smithing)
                random.next = 1
                val special = recipe.output.id == objs.lovakite_bar.id
                val request =
                    FurnaceSmeltingRequest(
                        furnace = if (special) lovakiteFurnace else furnace,
                        kind = if (special) FurnaceKind.Lovakite else FurnaceKind.Standard,
                        output = recipe.output,
                        remaining = 1,
                    )

                player.placeAt(request.furnace.coords.translateX(-1))
                player.withProtectedAccess { deps.smelting.process(this, request) }

                assertEquals(1, player.count(recipe.output), recipe.name)
                recipe.oreCosts.forEach { (ore, _) ->
                    assertEquals(0, player.count(ore), recipe.name)
                }
                assertEquals(0, player.count(objs.coal), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.smithing) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`iron can fail atomically and a ring guarantees bars for 140 ores`() =
        runSmeltingTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.iron_ore)
            random.next = 0
            val request =
                FurnaceSmeltingRequest(furnace, FurnaceKind.Standard, objs.iron_bar, remaining = 1)

            player.withProtectedAccess { deps.smelting.process(this, request) }
            assertEquals(0, player.count(objs.iron_ore))
            assertEquals(0, player.count(objs.iron_bar))
            assertMessageSent("The ore is too impure and you fail to refine it.")

            preparePlayer()
            deps.ring.consume(player, 139)
            player.ring = InvObj(objs.ring_of_forging)
            add(objs.iron_ore)
            random.next = 0
            player.withProtectedAccess { deps.smelting.process(this, request) }

            assertEquals(1, player.count(objs.iron_bar))
            assertNull(player.ring)
            assertEquals(140, deps.ring.remaining(player))
            assertMessageSent("Your ring of forging has melted.")
        }

    @Test
    fun GameTestState.`ring check reports shared charges and break resets the next ring`() =
        runSmeltingTest { deps ->
            preparePlayer()
            deps.ring.consume(player, 37)

            player.withProtectedAccess { deps.smelting.checkRing(this) }
            assertMessageSent("Your ring of forging has 103 charges remaining.")

            add(objs.ring_of_forging)
            val slot = player.inv.indexOfFirst { it?.id == objs.ring_of_forging.id }
            player.withProtectedAccess { deps.smelting.breakRing(this, slot) }

            assertNull(player.inv[slot])
            assertEquals(140, deps.ring.remaining(player))
            assertMessageSent("You break the ring and reset your next ring of forging's charges.")
        }

    @Test
    fun GameTestState.`smithing catalyst draws loose coal before the bag and doubles xp`() =
        runSmeltingTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.mithril_ore)
            add(objs.coal)
            add(objs.coal_bag)
            add(objs.smithing_catalyst)
            player.setVarBit(varbits.coal_bag_storage_count, 3)
            val startXp = player.statMap.getFineXP(stats.smithing)

            player.withProtectedAccess {
                deps.smelting.process(
                    this,
                    FurnaceSmeltingRequest(
                        furnace,
                        FurnaceKind.Standard,
                        objs.mithril_bar,
                        remaining = 1,
                    ),
                )
            }

            assertEquals(1, player.count(objs.mithril_bar))
            assertEquals(0, player.count(objs.coal))
            assertEquals(2, player.vars[varbits.coal_bag_storage_count])
            assertEquals(0, player.count(objs.smithing_catalyst))
            assertEquals(600, player.statMap.getFineXP(stats.smithing) - startXp)
        }

    @Test
    fun GameTestState.`goldsmith gauntlets Smithing capes and max cape grant 56 point 2 xp`() =
        runSmeltingTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            val equipment =
                listOf(
                    objs.goldsmith_gauntlets,
                    objs.smithing_skillcape,
                    objs.smithing_skillcape_t,
                    objs.max_cape_worn,
                )
            for (item in equipment) {
                preparePlayer()
                if (item.id == objs.goldsmith_gauntlets.id) {
                    player.hands = InvObj(item)
                } else {
                    player.back = InvObj(item)
                }
                add(objs.gold_ore)
                val startXp = player.statMap.getFineXP(stats.smithing)

                player.withProtectedAccess {
                    deps.smelting.process(
                        this,
                        FurnaceSmeltingRequest(
                            furnace,
                            FurnaceKind.Standard,
                            objs.gold_bar,
                            remaining = 1,
                        ),
                    )
                }

                assertEquals(1, player.count(objs.gold_bar), item.toString())
                assertEquals(
                    562,
                    player.statMap.getFineXP(stats.smithing) - startXp,
                    item.toString(),
                )
            }
        }

    @Test
    fun GameTestState.`Edgeville armour double respects tier and consumes two sets of materials`() =
        runSmeltingTest { deps ->
            val furnace = prepareFurnace(furnace_locs.edgeville)
            preparePlayer()
            player.torso = InvObj(objs.diary_varrock_armour_2)
            add(objs.mithril_ore, 2)
            add(objs.coal, 8)
            random.next = 1

            player.withProtectedAccess {
                deps.smelting.process(
                    this,
                    FurnaceSmeltingRequest(
                        furnace,
                        FurnaceKind.Edgeville,
                        objs.mithril_bar,
                        remaining = 2,
                    ),
                )
            }
            assertEquals(2, player.count(objs.mithril_bar))
            assertEquals(0, player.count(objs.mithril_ore))
            assertEquals(0, player.count(objs.coal))

            preparePlayer()
            player.torso = InvObj(objs.diary_varrock_armour_1)
            add(objs.mithril_ore, 2)
            add(objs.coal, 8)
            random.next = 1
            player.withProtectedAccess {
                deps.smelting.process(
                    this,
                    FurnaceSmeltingRequest(
                        furnace,
                        FurnaceKind.Edgeville,
                        objs.mithril_bar,
                        remaining = 2,
                    ),
                )
            }
            assertEquals(1, player.count(objs.mithril_bar))
        }

    @Test
    fun GameTestState.`Shilo furnace charges 20 coins unless elite diary is complete`() =
        runSmeltingTest { deps ->
            val furnace = prepareFurnace(furnace_locs.shilo_village)
            preparePlayer()
            add(objs.copper_ore)
            add(objs.tin_ore)
            add(objs.coins, 20)
            val request =
                FurnaceSmeltingRequest(furnace, FurnaceKind.ShiloVillage, objs.bronze_bar, 1)

            player.withProtectedAccess { deps.smelting.process(this, request) }
            assertEquals(1, player.count(objs.bronze_bar))
            assertEquals(0, player.count(objs.coins))

            preparePlayer()
            player.setVarBit(varbits.karamja_diary_elite_complete, 1)
            add(objs.copper_ore)
            add(objs.tin_ore)
            player.withProtectedAccess { deps.smelting.process(this, request) }
            assertEquals(1, player.count(objs.bronze_bar))
        }

    @Test
    fun GameTestState.`lovakite requires its special furnace and quest bars require unlocks`() =
        runSmeltingTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.lovakite_ore)
            add(objs.coal, 2)
            player.withProtectedAccess {
                deps.smelting.select(this, furnace, FurnaceKind.Standard, objs.lovakite_ore)
            }
            assertMessageSent("This furnace is not at the right temperature to smelt lovakite ore.")
            assertEquals(1, player.count(objs.lovakite_ore))

            preparePlayer()
            add(objs.blurite_ore)
            player.withProtectedAccess {
                deps.smelting.process(
                    this,
                    FurnaceSmeltingRequest(
                        furnace,
                        FurnaceKind.Standard,
                        objs.blurite_bar,
                        remaining = 1,
                    ),
                )
            }
            assertEquals(1, player.count(objs.blurite_ore))
            assertEquals(0, player.count(objs.blurite_bar))

            preparePlayer()
            add(objs.lunar_ore)
            player.withProtectedAccess {
                deps.smelting.process(
                    this,
                    FurnaceSmeltingRequest(
                        furnace,
                        FurnaceKind.Standard,
                        objs.lunar_bar,
                        remaining = 1,
                    ),
                )
            }
            assertEquals(1, player.count(objs.lunar_ore))
            assertEquals(0, player.count(objs.lunar_bar))
        }

    @Test
    fun GameTestState.`leaving the furnace interrupts the remaining batch`() =
        runSmeltingTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.copper_ore, 2)
            add(objs.tin_ore, 2)
            player.withProtectedAccess { deps.smelting.select(this, furnace, FurnaceKind.Standard) }
            resumeSkillMulti(components.skillmulti_a, quantity = 2)
            advance(4)
            assertEquals(1, player.count(objs.bronze_bar))

            player.placeAt(furnace.coords.translateX(4))
            advance(5)
            assertEquals(1, player.count(objs.bronze_bar))
            assertEquals(1, player.count(objs.copper_ore))
            assertEquals(0, player.weakQueueList.count(furnace_queues.smelting))
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
        player.ring = null
        player.back = null
        player.hands = null
        player.torso = null
        player.setVarp(varps.squire, 0)
        player.setVarBit(varbits.lunar_quest_main, 0)
        player.setVarBit(varbits.coal_bag_storage_count, 0)
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

    private fun GameTestScope.resumeSkillMulti(
        component: org.rsmod.game.type.comp.ComponentType,
        quantity: Int,
    ) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = quantity))
    }

    private fun GameTestState.runSmeltingTest(body: GameTestScope.(Dependencies) -> Unit) =
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
    constructor(
        val smelting: FurnaceSmelting,
        val worn: WornInteractions,
        val objs: ObjTypeList,
        val recipes: SmeltingRecipeRegistry,
        val ring: RingOfForgingRepository,
    )
}
