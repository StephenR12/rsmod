package org.rsmod.content.skills.maxcape

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.buttons.IfSubOp
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.*
import org.rsmod.api.net.rsprot.handlers.IfSubOpHandler
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.skillcapes.SkillcapeTrimming
import org.rsmod.game.enums.EnumTypeMapResolver
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

class MaxCapeTest {
    data class Deps
    @Inject
    constructor(
        val packet: IfSubOpHandler,
        val enums: EnumTypeMapResolver,
        val worn: WornInteractions,
    )

    @Test
    fun GameTestState
        .`held and equipped max cape entries use actual submenu packets and verified items`() =
        runInjectedGameTest(Deps::class, null, MaxCape::class) { d ->
            player.members = true
            player.inv[0] = InvObj(objs.max_cape_inv)
            val inv = components.inv_items
            val back = checkNotNull(d.enums[enums.equipment_tab_to_slots_map][Wearpos.Back.slot])
            player.ifOpenOverlay(interfaces.inventory, components.toplevel_target_side3)
            player.ifSetEvents(inv, player.inv.indices, IfEvent.Op3, IfEvent.Op4, IfEvent.Op6)
            fun packet(comp: ComponentType, slot: Int, id: Int, op: Int, sub: Int) {
                d.packet.handle(
                    player,
                    IfSubOp(CombinedId(comp.interfaceId, comp.component), slot, id, op, sub),
                )
                advance(6)
            }
            val start = CoordGrid(3200, 3200)
            val guild = CoordGrid(2931, 3286)
            player.teleport(start)
            allocZoneCollision(guild)
            packet(inv, 1, objs.max_cape_inv.id, 3, 3)
            assertEquals(start, player.coords)
            player.activityBlocksTeleport = true
            packet(inv, 0, objs.max_cape_inv.id, 3, 3)
            assertEquals(start, player.coords)
            player.activityBlocksTeleport = false
            packet(inv, 0, objs.max_cape_inv.id, 3, 3)
            assertEquals(guild, player.coords)
            player.runEnergy = 30
            packet(inv, 0, objs.max_cape_inv.id, 6, 4)
            assertEquals(10000, player.runEnergy)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(null, player.worn[Wearpos.Back.slot])
            for (def in SkillcapeTrimming.definitions) player.setBaseLevel(def.stat, 99)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(objs.max_cape_worn.id, player.worn[Wearpos.Back.slot]?.id)
            player.ifOpenOverlay(interfaces.wornitems, components.toplevel_target_side3)
            val fishing = CoordGrid(2604, 3401)
            allocZoneCollision(fishing)
            packet(back, -1, -1, 4, 3)
            assertEquals(fishing, player.coords)
            player.runEnergy = 25
            packet(back, -1, -1, 8, 4)
            assertTrue(player.runEnergy in 25..200)
            player.withProtectedAccess {
                d.worn.interact(this, player.worn, Wearpos.Back.slot, IfButtonOp.Op1)
            }
            assertEquals(1, player.count(objs.max_cape_inv))
        }

    @Test
    fun GameTestState.`ordinary and max cape searches share the three successful daily grapples`() =
        runGameTest(ProductionCapePerks::class, MaxCape::class) {
            player.members = true
            player.inv[0] = InvObj(objs.fletching_skillcape)
            for (slot in 1 until player.inv.size) player.inv[slot] = InvObj(objs.knife)
            player.withProtectedAccess { opHeld3(0) }
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(4818)])
            for (slot in 1 until player.inv.size) player.inv[slot] = null
            repeat(2) { player.withProtectedAccess { opHeld3(0) } }
            assertEquals(2, player.count(objs.bronze_crossbow))
            player.inv[10] = InvObj(objs.max_cape_inv)
            fun search() {
                player.withProtectedAccess { opHeld4(10) }
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
                advance()
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
                advance()
            }
            search()
            assertEquals(3, player.count(objs.bronze_crossbow))
            search()
            assertEquals(3, player.count(objs.mith_grapple))
            player.inv[11] = InvObj(objs.herblore_skillcape)
            player.withProtectedAccess { opHeld3(11) }
            assertEquals(1, player.count(objs.pestle_and_mortar))
        }

    @Test
    fun GameTestState.`Mac requires all base levels and pays only for matching sale sets`() =
        runGameTest(MacsIsland::class) {
            player.members = true
            val mac = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(1053))
            player.inv[0] = InvObj(objs.coins, 2_277_000)
            fun talk(i: Int) {
                player.opNpc1(mac)
                advance(2)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, i))
                advance()
            }
            fun confirm() {
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
                advance()
            }
            talk(0)
            assertEquals(2_277_000, player.count(objs.coins))
            for (def in SkillcapeTrimming.definitions) player.setBaseLevel(def.stat, 99)
            talk(0)
            confirm()
            assertEquals(1, player.count(objs.max_cape_inv))
            assertEquals(1, player.count(objs.max_hood))
            assertEquals(0, player.count(objs.coins))
            val hoodSlot = player.inv.indexOfFirst { it?.id == objs.max_hood.id }
            player.inv[hoodSlot] = null
            talk(1)
            confirm()
            assertEquals(1, player.count(objs.max_cape_inv))
            talk(2)
            assertEquals(1, player.count(objs.max_hood))
            talk(1)
            confirm()
            assertEquals(1_821_600, player.count(objs.coins))
            assertEquals(0, player.count(objs.max_cape_inv))
        }

    @Test
    fun GameTestState
        .`Mac's tree rockslide and personal raft require natural masteries and real tools`() =
        runGameTest(MacsIsland::class) {
            player.members = true
            val treeTile = CoordGrid(2828, 3543)
            val rockTile = CoordGrid(2821, 3539)
            val logTile = CoordGrid(2805, 3535)
            val boatTile = CoordGrid(2786, 3536)
            val tree = placeMapLoc(treeTile, locTypes.getValue(27060))
            val rock = placeMapLoc(rockTile, locTypes.getValue(27062))
            val log =
                placeMapLoc(
                    logTile,
                    locTypes.getValue(27067),
                    angle = org.rsmod.game.loc.LocAngle[1],
                )
            val boat = placeMapLoc(boatTile, locTypes.getValue(27066))
            for (stat in
                listOf(
                    stats.agility,
                    stats.defence,
                    stats.crafting,
                    stats.construction,
                    stats.mining,
                    stats.woodcutting,
                )) {
                player.setBaseLevel(stat, 99)
                player.setCurrentLevel(stat, 99)
            }
            player.teleport(CoordGrid(2830, 3543))
            player.opLoc1(tree)
            advance(2)
            assertNull(findLoc(treeTile, locTypes.getValue(27061)))
            player.inv[0] = InvObj(objs.bronze_axe)
            player.opLoc1(tree)
            advance(2)
            val stump = locTypes.getValue(27061)
            player.opLoc1(
                org.rsmod.game.loc.BoundLocInfo(checkNotNull(findLoc(treeTile, stump)), stump)
            )
            advance(2)
            assertEquals(CoordGrid(2827, 3543), player.coords)
            player.teleport(CoordGrid(2822, 3541))
            player.inv[1] = InvObj(objs.bronze_pickaxe)
            player.opLoc1(rock)
            advance(2)
            val mined = locTypes.getValue(27063)
            player.opLoc1(
                org.rsmod.game.loc.BoundLocInfo(checkNotNull(findLoc(rockTile, mined)), mined)
            )
            advance(2)
            assertEquals(CoordGrid(2820, 3539), player.coords)
            player.teleport(CoordGrid(2807, 3535))
            player.opLoc1(log)
            advance(2)
            val built = cacheTypes.varbits.getValue(4801)
            assertEquals(1, player.vars[built])
            player.opLoc1(log)
            advance(2)
            assertEquals(0, player.vars[built])
            assertEquals(CoordGrid(2794, 3538), player.coords)
            player.teleport(CoordGrid(2789, 3538))
            allocZoneCollision(CoordGrid(2726, 3589))
            player.opLoc1(boat)
            advance(2)
            assertEquals(CoordGrid(2726, 3589), player.coords)
        }
}
