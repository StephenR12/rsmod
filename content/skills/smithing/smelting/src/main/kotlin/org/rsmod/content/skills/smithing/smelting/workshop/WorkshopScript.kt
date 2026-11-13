package org.rsmod.content.skills.smithing.smelting.workshop

import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WorkshopScript
@Inject
constructor(private val rooms: WorkshopAccess, private val production: WorkshopProduction) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(WorkshopLocs.beatenBook) { recoverBook(this, WorkshopObjs.beaten) }
        for (book in
            listOf(WorkshopObjs.battered, WorkshopObjs.slashed, WorkshopObjs.beaten)) onOpHeld1(
            book
        ) {
            mes("The book describes how to work elemental metal in the workshop.")
        }
        for (hatch in
            listOf(WorkshopLocs.hatch, WorkshopLocs.closedHatch, WorkshopLocs.openHatch)) onOpLoc1(
            hatch
        ) {
            if (!player.members || vars[WorkshopBits.quest] < 5) {
                mes("You haven't unlocked this hatch yet.")
                return@onOpLoc1
            }
            if (vars[WorkshopBits.hatch] == 0) {
                vars[WorkshopBits.hatch] = 1
                mes("You open the hatch.")
            } else rooms.enter(this)
        }
        onOpLoc1(WorkshopLocs.firstReturn) { rooms.leave(this) }
        onOpLoc1(WorkshopLocs.lower) {
            rooms.coord(player, CoordGrid(1949, 5160))?.let { teleport(it) }
        }
        onOpLoc1(WorkshopLocs.upper) {
            rooms.coord(player, CoordGrid(1949, 5157, 2))?.let { teleport(it) }
        }
        onOpLoc1(WorkshopLocs.gantryUp) {
            val normal = rooms.source(player, it.loc.coords) ?: return@onOpLoc1
            rooms
                .coord(player, normal.translateX(if (normal.x == 1949) -1 else 1).copy(level = 3))
                ?.let { teleport(it) }
        }
        for (stair in listOf(WorkshopLocs.gantryDown, WorkshopLocs.gantryReturn)) onOpLoc1(stair) {
            val normal = rooms.source(player, it.loc.coords) ?: return@onOpLoc1
            rooms
                .coord(
                    player,
                    if (stair == WorkshopLocs.gantryReturn) CoordGrid(1952, 5154, 2)
                    else normal.translateX(if (normal.x == 1949) -1 else 1).copy(level = 2),
                )
                ?.let { teleport(it) }
        }
        for (door in WorkshopLocs.doors) onOpLoc1(door) {
            val source = rooms.source(player, player.coords) ?: return@onOpLoc1
            rooms
                .coord(
                    player,
                    CoordGrid(if (source.x <= 1952) 1953 else 1951, source.z.coerceIn(5149, 5150)),
                )
                ?.let { teleport(it) }
        }
        for (furnace in WorkshopLocs.furnaces) {
            onOpLoc1(furnace) { production.smelt(this, it.loc) }
            onOpLocU(furnace, WorkshopObjs.ore) { production.smelt(this, it.loc) }
        }
        onOpLoc1(WorkshopLocs.workbench) { production.select(this, it.loc) }
        for (bar in listOf(WorkshopObjs.bar, WorkshopObjs.mind)) onOpLocU(
            WorkshopLocs.workbench,
            bar,
        ) {
            production.select(this, it.loc)
        }
        for (control in WorkshopLocs.controls) onOpLoc1(control) {
            production.operate(this, it.loc)
        }
        for (cart in WorkshopCarts.stages) {
            onOpNpc1(cart) { production.takeCart(this, it.npc) }
            onOpNpcU(cart, WorkshopObjs.bar) { production.putCart(this, it.npc) }
        }
        for (gun in WorkshopLocs.guns) {
            onOpLocU(gun, WorkshopObjs.primed) { production.putGun(this, it.loc) }
            onOpLoc1(gun) { production.takeGun(this, it.loc) }
        }
        onOpLoc1(WorkshopLocs.hat) { production.extract(this, it.loc) }
        onPlayerLogout { rooms.logout(player) }
        onPlayerLogin { rooms.recover(player) }
    }

    public fun recoverBook(access: ProtectedAccess, book: ObjType): Boolean =
        with(access) {
            if (!player.members || player.invMap.values.any { book in it }) return false
            player.invTransaction(inv) { add(select(inv), book.id, 1) }.success
        }
}
