package org.rsmod.content.skills.combattraining

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CombatDungeonLocs : LocReferences() {
    val bookcase = find("elemental_workshop_bookcase")
    val walls = listOf(find("elemental_workshop_oddwall_l"), find("elemental_workshop_oddwall_r"))
    val downWorkshop = find("elemental_workshop_spiralstairstop")
    val upWorkshop = find("elemental_workshop_spiralstairs")
    val crypt = find("ds2_tomb_entry")
    val cryptExit = find("ds2_tomb_exit")
    val stairs = find("ds2_tomb_stairs")
    val stairsTop = find("ds2_tomb_stairs_top")
    val ladders =
        listOf(find("ds2_tomb_ladder"), find("ds2_tomb_ladder_left"), find("ds2_tomb_ladder_right"))
    val ladderTops =
        listOf(
            find("ds2_tomb_ladder_top"),
            find("ds2_tomb_ladder_top_left"),
            find("ds2_tomb_ladder_top_right"),
        )
}

internal object WorkshopObjs : ObjReferences() {
    val book = find("elemental_workshop_shield_book")
    val slashedBook = find("elemental_workshop_shield_book_slashed")
    val key = find("elemental_workshop_key")
}

class DiaryDungeonEntrances : PluginScript() {
    override fun ScriptContext.startup() {
        with(CombatDungeonLocs) {
            onOpLoc1(bookcase) {
                if (player.members && WorkshopObjs.key !in inv && WorkshopObjs.book !in inv) {
                    if (invAdd(inv, WorkshopObjs.book).success)
                        mes("You find a small battered book.")
                }
            }
            onOpHeldU(WorkshopObjs.book) {
                val blade = it.second
                if (blade.id != objs.knife.id && blade.param(params.attack_slash) <= 0)
                    return@onOpHeldU
                val result =
                    player.invTransaction(inv) {
                        val inventory = select(inv)
                        delete(inventory, WorkshopObjs.book.id, 1, slot = it.firstSlot)
                        add(inventory, WorkshopObjs.slashedBook.id, 1)
                        add(inventory, WorkshopObjs.key.id, 1)
                    }
                if (result.success)
                    mes("You slash through the binding of the book and find a key inside.")
                else mes("You'll need room in your inventory to take the key.")
            }
            for (wall in walls) {
                onOpLoc1(wall) { crossWorkshopWall() }
                onOpLocU(wall, WorkshopObjs.key) { crossWorkshopWall() }
            }
            onOpLoc1(downWorkshop) { if (player.members) telejump(CoordGrid(2716, 9888)) }
            onOpLoc1(upWorkshop) { telejump(CoordGrid(2710, 3496)) }
            onOpLoc1(crypt) {
                if (!player.members) return@onOpLoc1
                if (hasLight()) telejump(CoordGrid(1484, 9951, 3))
                else mes("You will need a light source to enter the crypts.")
            }
            onOpLoc1(cryptExit) { telejump(CoordGrid(1483, 3550)) }
            onOpLoc1(stairs) {
                val loc = it.loc.coords
                val tile =
                    when (loc.x) {
                        1494 -> CoordGrid(1498, 9978, 2)
                        1510 -> CoordGrid(1509, 9978, 2)
                        else -> CoordGrid(1503, 9929, 3)
                    }
                telejump(tile)
            }
            onOpLoc1(stairsTop) {
                val tile =
                    when (it.loc.coords.x) {
                        1496 -> CoordGrid(1493, 9978, 1)
                        1510 -> CoordGrid(1514, 9978, 1)
                        else -> CoordGrid(1503, 9924, 2)
                    }
                telejump(tile)
            }
            for (ladder in ladders) {
                onOpLoc1(ladder) {
                    telejump(CoordGrid(player.coords.x, player.coords.z, it.loc.coords.level + 1))
                }
            }
            for (ladder in ladderTops) {
                onOpLoc1(ladder) {
                    telejump(CoordGrid(player.coords.x, player.coords.z, it.loc.coords.level - 1))
                }
            }
        }
    }

    private fun ProtectedAccess.crossWorkshopWall() {
        if (!player.members) return
        if (player.coords.z <= 3495 && WorkshopObjs.key !in inv) {
            mes("The wall has a strange crack in it. Perhaps there's a way through.")
        } else {
            telejump(
                CoordGrid(
                    player.coords.x.coerceIn(2709, 2710),
                    if (player.coords.z <= 3495) 3496 else 3495,
                )
            )
        }
    }
}
