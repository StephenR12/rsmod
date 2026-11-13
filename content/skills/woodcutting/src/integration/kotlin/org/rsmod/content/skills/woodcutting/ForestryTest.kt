package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.buttons.IfSubOp
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.net.rsprot.handlers.IfSubOpHandler
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingOutfit
import org.rsmod.content.skills.woodcutting.forestry.*
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLogs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class ForestryTest {
    class Dependencies
    @Inject
    constructor(
        val gear: ForestryGear,
        val held: HeldUInteractions,
        val outfit: WoodcuttingOutfit,
        val nests: WoodcuttingBirdNests,
    )

    @Test
    fun GameTestState.nativeBasketSubmenusValidateSlotsAndKeepTheSelectedAction() =
        runInjectedGameTest(IfSubOpHandler::class, null, ForestryGear::class) { handler ->
            player.members = true
            val inventory = components.inv_items
            player.ifOpenOverlay(interfaces.inventory, components.toplevel_target_side3)
            player.ifSetEvents(inventory, player.inv.indices, IfEvent.Op2, IfEvent.Op4, IfEvent.Op6)
            val basket = objTypes.getValue(28143)
            player.inv[0] = InvObj(basket)
            player.inv[1] = InvObj(objs.logs)
            fun select(slot: Int, id: Int, op: Int, sub: Int) {
                handler.handle(
                    player,
                    IfSubOp(
                        CombinedId(inventory.interfaceId, inventory.component),
                        slot,
                        id,
                        op,
                        sub,
                    ),
                )
                advance()
            }
            select(1, basket.id, 2, 2)
            assertEquals(1, player.count(objs.logs))
            select(0, basket.id, 2, 2)
            assertEquals(0, player.count(objs.logs))
            select(0, basket.id, 4, 2)
            assertEquals(1, player.count(objs.logs))
            select(0, basket.id, 6, 1)
            assertEquals(28145, player.inv[0]?.id)
            select(0, 28145, 6, 2)
            for (id in listOf(28136, 28149, 28142)) assertEquals(
                1,
                player.count(objTypes.getValue(id)),
            )
        }

    @Test
    fun GameTestState.shopBuysUsingCarriedKitAndNotesAndRefundsBothCurrencies() =
        runInjectedGameTest(
            Dependencies::class,
            null,
            ForestryGear::class,
            ForestryShop::class,
            org.rsmod.api.shops.OmniShopButtons::class,
        ) { d ->
            player.members = true
            val forester = spawnNpc(CoordGrid(2010, 2010), npcTypes.getValue(12119))
            allocZoneCollision(forester.coords)
            player.telejump(forester.coords.translate(1, 0))
            player.opNpc3(forester)
            advance(3)
            assertModalOpen(cacheTypes.interfaces.getValue(819))
            fun buy(row: Int) {
                player.ifButton(
                    cacheTypes.components.values.single { it.internalName == "omnishop_main:list" },
                    row * 12,
                    IfButtonOp.Op2,
                )
                advance()
            }
            val bark = objTypes.getValue(28134)
            buy(0)
            assertEquals(1, player.count(objTypes.getValue(28136)))
            val kit = d.gear.kit(player)
            kit[0] = InvObj(bark, 4950)
            player.inv[1] = InvObj(bark, 50)
            player.inv[2] = InvObj(objTypes.getValue(1522), 300)
            player.inv[3] = InvObj(objTypes.getValue(1520), 299)
            buy(7)
            assertEquals(4950, kit.count(bark))
            assertEquals(50, player.count(bark))
            player.inv[3] = InvObj(objTypes.getValue(1520), 300)
            buy(7)
            val basket = objTypes.getValue(28140)
            assertEquals(1, player.count(basket))
            assertEquals(0, kit.count(bark))
            assertEquals(0, player.count(bark))
            player.ifButton(
                cacheTypes.components.values.single { it.internalName == "omnishop_side:items" },
                player.inv.indexOfFirst { it?.id == basket.id },
                IfButtonOp.Op2,
                basket.id,
            )
            advance()
            assertEquals(4000, player.count(bark))
            assertEquals(240, player.count(objTypes.getValue(1522)))
            assertEquals(240, player.count(objTypes.getValue(1520)))
            val egg = objTypes.getValue(28663)
            player.inv[5] = InvObj(egg)
            player.ifButton(
                cacheTypes.components.values.single { it.internalName == "omnishop_side:items" },
                5,
                IfButtonOp.Op2,
                egg.id,
            )
            advance()
            assertEquals(4250, player.count(bark))
        }

    @Test
    fun GameTestState.rationsFellingChargesAndPouchUseTheSameToolsAsOrdinaryChopping() =
        runInjectedGameTest(
            Dependencies::class,
            null,
            ForestryGear::class,
            ForestryCrafting::class,
            Woodcutting::class,
        ) { d ->
            player.members = true
            player.stats[stats.woodcutting] = 99
            player.stats[stats.cooking] = 35
            fun interact(a: Int, b: Int) {
                player.withProtectedAccess {
                    d.held.interact(this, inv, objTypes[inv[a]!!], a, objTypes[inv[b]!!], b)
                }
            }
            player.inv[0] = InvObj(objTypes.getValue(6030))
            player.inv[1] = InvObj(objTypes.getValue(391))
            val cookXp = player.statMap.getFineXP(stats.cooking)
            interact(1, 0)
            advance(4)
            assertEquals(36, player.count(objTypes.getValue(28157)))
            assertEquals(511, player.statMap.getFineXP(stats.cooking) - cookXp)
            val rationSlot = player.inv.indexOfFirst { it?.id == 28157 }
            player.inv[2] = InvObj(objTypes.getValue(28136))
            interact(2, rationSlot)
            val kit = d.gear.kit(player)
            assertEquals(36, kit.count(objTypes.getValue(28157)))
            player.inv[3] = InvObj(objs.crystal_felling_axe, vars = CrystalAxeState(1).pack())
            player.setVarBit(varbits.song_of_the_elves, 194)
            player.setVarBit(varbits.vesseled_clues_disabled, 1)
            player.runEnergy = 10000
            val type = findLocType(content.tree) { it.treeLogs.id == 1511 }
            val tree = placeMapLoc(CoordGrid(3234, 3231), type)
            player.teleport(tree.coords.translateX(-1))
            player.opLoc1(tree)
            advance(3)
            val startXp = player.statMap.getFineXP(stats.woodcutting)
            random.next = 0
            random.then = 0
            random.then = 255
            random.then = 1
            advance()
            assertEquals(1, player.count(objTypes.getValue(28223)))
            assertEquals(35, kit.count(objTypes.getValue(28157)))
            assertEquals(275, player.statMap.getFineXP(stats.woodcutting) - startXp)
            player.clearPendingAction()
            player.inv[4] = InvObj(objTypes.getValue(28163))
            interact(2, 4)
            player.inv[4] = InvObj(objs.lumberjack_hat)
            interact(4, 2)
            assertEquals(0.004, with(d.outfit) { player.modifier() })
            player.inv[4] = InvObj(objTypes.getValue(28613))
            interact(2, 4)
            player.inv[4] = InvObj(objs.woodcutting_skillcape)
            interact(4, 2)
            assertEquals(256, d.nests.chanceDenominator(player))
            player.worn[1] = player.inv[2]
            player.inv[2] = null
            assertEquals(230, d.nests.chanceDenominator(player))
        }

    @Test
    fun GameTestState.openBasketAcceptsLogsWhenInventoryIsFullAndBankReceivesTheContents() =
        runInjectedGameTest(Dependencies::class, null, ForestryGear::class, Woodcutting::class) { d
            ->
            player.members = true
            player.stats[stats.woodcutting] = 99
            player.setVarBit(varbits.vesseled_clues_disabled, 1)
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.hammer)
            player.inv[0] = InvObj(objTypes.getValue(28142))
            player.righthand = InvObj(objs.rune_axe)
            val type = findLocType(content.tree) { it.treeLogs.id == 1511 }
            val tree = placeMapLoc(CoordGrid(3234, 3231), type)
            player.teleport(tree.coords.translateX(-1))
            player.opLoc1(tree)
            advance(3)
            random.next = 0
            random.then = 0
            random.then = 255
            random.then = 1
            advance()
            assertEquals(1, d.gear.logStore(player).count(objs.logs))
            d.gear.bankLogs(player)
            assertEquals(0, d.gear.logStore(player).count(objs.logs))
            player.withProtectedAccess { assertEquals(1, bank.count(objs.logs)) }
        }

    @Test
    fun GameTestState.nearbyCuttersCanChopTheSpawnedRootsUntilTheEventEnds() =
        runInjectedGameTest(Dependencies::class, null, ForestryEvents::class) { d ->
            val pos = CoordGrid(2010, 2010)
            val tree = prepareEvent(pos, ForestEvent.Roots)
            val rootType = locTypes.getValue(47482)
            val root =
                (-5..5)
                    .flatMap { dx -> (-5..5).map { dz -> pos.translate(dx, dz) } }
                    .firstNotNullOf { findLoc(it, rootType) }
            player.telejump(root.coords.translate(1, 0))
            player.opLoc1(BoundLocInfo(root, rootType))
            advance(7)
            assertTrue(d.gear.kit(player).count(objTypes.getValue(28134)) >= 2)
            advance(155)
            assertNull(findLoc(root.coords, rootType))
            assertNotNull(findLoc(tree.coords, locTypes[tree]))
        }

    @Test
    fun GameTestState.allOtherEventTypesSpawnTheirOwnSceneAndRemoveItOnExpiry() =
        runInjectedGameTest(Dependencies::class, null, ForestryEvents::class) { _ ->
            for (kind in ForestEvent.entries.drop(1)) {
                val pos = CoordGrid(2010 + kind.ordinal * 64, 2010)
                prepareEvent(pos, kind)
                fun npcIds() = npcRepo.findAll(ZoneKey.from(pos), 1).map { it.type.id }.toSet()
                val expectedNpc =
                    mapOf(
                        ForestEvent.Entlings to 12543,
                        ForestEvent.Flowers to 12120,
                        ForestEvent.Leprechaun to 12163,
                        ForestEvent.Hives to 12514,
                        ForestEvent.Ritual to 12519,
                        ForestEvent.Poachers to 12559,
                        ForestEvent.Pheasants to 12536,
                    )[kind]
                if (expectedNpc != null) assertTrue(expectedNpc in npcIds()) { kind.toString() }
                else {
                    val sapling =
                        (-5..5)
                            .flatMap { dx -> (-5..5).map { dz -> pos.translate(dx, dz) } }
                            .any { findLoc(it, locTypes.getValue(47484)) != null }
                    assertTrue(sapling) { kind.toString() }
                }
                advance(kind.duration + 2)
                assertTrue(npcIds().isEmpty()) { kind.toString() }
            }
        }

    private fun GameTestScope.prepareEvent(pos: CoordGrid, kind: ForestEvent): BoundLocInfo {
        for (dx in -8..8 step 8) for (dz in -8..8 step 8) allocZoneCollision(pos.translate(dx, dz))
        player.members = true
        player.stats[stats.woodcutting] = 60
        player.inv[0] = InvObj(objTypes.getValue(28136))
        player.inv[1] = InvObj(objs.rune_axe)
        player.placeAt(pos.translate(1, 0))
        val type = findLocType(content.tree) { it.treeLogs.id == 1521 }
        val tree = placeMapLoc(pos, type)
        random.next = kind.ordinal
        random.then = 1
        random.then = 67
        random.then = 0
        eventBus.publish(TreeChopRoll(player, tree, type))
        advance(3)
        return tree
    }
}
