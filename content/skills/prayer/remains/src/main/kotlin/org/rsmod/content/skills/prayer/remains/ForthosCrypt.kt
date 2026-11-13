package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.*
import org.rsmod.api.invtx.*
import org.rsmod.api.player.feet
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CryptLocs : LocReferences() {
    val ladderDown = find("hosdun_entrance_ladder_hole")
    val ladderUp = find("hosdun_entrance_ladder")
    val stairsDown = find("hosdun_entrance_spiral_steps_hole")
    val stairsUp = find("hosdun_entrance_spiral_stairs")
    val locked = find("hosdun_stone_relief_closed")
    val opened = find("hosdun_stone_relief_open")
    val exit = find("hosdun_brick_wall_passage")
    val lever = find("hosdun_lever_closed")
    val gate = find("hosdun_burner_door")
    val west = find("hosdun_west_door")
    val east = find("hosdun_east_door")
    val burner = find("hosdun_bone_burner")
    val web = find("sarachnis_web_peek")
    val cutWeb = find("sarachnis_web_peek_slashed")
    val webIn = find("hosdun_sarachnis_entrance")
    val webOut = find("hosdun_sarachnis_exit")
}

internal object CryptPeople : NpcReferences() {
    val olbertus = find("hosdun_olbertus")
    val possessed = find("hosdun_olbertus_corrupted")
    val restored = find("hosdun_olbertus_restored")
    val brother = find("hosdun_aimeri")
    val injured = find("hosdun_aimeri_injured")
    val healed = find("hosdun_aimeri_healed")
    val beast = find("hosdun_shaeded_beast")
    val eodan = find("hosdun_eodan")
}

internal object CryptItems : ObjReferences() {
    val coin = find("hosdun_temple_coin")
    val key = find("hosdun_temple_key")
}

internal object CryptBits : VarBitReferences() {
    val olbertus = find("hosdun_olbertus_status")
    val relief = find("hosdun_relief_status")
    val faith = find("hosdun_ralos_favour")
    val lever = find("hosdun_burner_open")
    val west = find("hosdun_west_door_status")
    val east = find("hosdun_east_door_status")
}

public class ForthosCrypt
@Inject
constructor(
    private val ground: ObjRepository,
    private val fight: ForthosBeast,
    private val locs: LocRepository,
    private val items: ObjTypeList,
    private val players: PlayerRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((loc, tile) in
            listOf(
                CryptLocs.ladderDown to CoordGrid(1830, 9973),
                CryptLocs.ladderUp to CoordGrid(1702, 3573),
                CryptLocs.stairsDown to CoordGrid(1799, 9965),
                CryptLocs.stairsUp to CoordGrid(1670, 3566),
            )) {
            onOpLoc1(loc) {
                if (player.members) {
                    fight.leave(player)
                    telejump(tile)
                }
            }
        }
        for (person in listOf(CryptPeople.possessed, CryptPeople.restored)) {
            onOpNpc1(person) { talkToOlbertus() }
            onOpNpcU(person, objs.dragonstone) { payOlbertus() }
        }
        onOpLocU(CryptLocs.locked, CryptItems.coin) { unlock() }
        onOpLoc1(CryptLocs.locked) { unlock() }
        onOpLoc1(CryptLocs.opened) {
            if (player.members && vars[CryptBits.relief] != 0) telejump(CoordGrid(1810, 9954))
        }
        onOpLoc1(CryptLocs.exit) {
            if (player.members && vars[CryptBits.relief] != 0) telejump(CoordGrid(1810, 9960))
        }
        onOpLoc1(CryptLocs.lever) {
            if (player.members && vars[CryptBits.relief] != 0) {
                vars[CryptBits.lever] = 1
                mes("The southern gate is now unlocked.")
            }
        }
        onOpLoc1(CryptLocs.gate) {
            if (vars[CryptBits.lever] == 0)
                mes("The door is controlled by a lever on the other side.")
            else telejump(CoordGrid(it.loc.x, if (coords.z >= it.loc.z) it.loc.z - 1 else it.loc.z))
        }
        for ((door, bit) in
            listOf(CryptLocs.west to CryptBits.west, CryptLocs.east to CryptBits.east)) {
            onOpLoc1(door) { templeDoor(it.loc.coords, bit, door == CryptLocs.west) }
            onOpLocU(door, CryptItems.key) {
                templeDoor(it.loc.coords, bit, door == CryptLocs.west)
            }
        }
        onOpLoc1(CryptLocs.burner) { mes(faithMessage(vars[CryptBits.faith])) }
        for (recipe in PrayerRemainsRecipes.all) onOpLocU(CryptLocs.burner, recipe.obj) {
            offer(it.invSlot, recipe)
        }
        onOpLoc1(CryptLocs.web) {
            val hasTool =
                (listOfNotNull(player.righthand) + inv.filterNotNull()).any { held ->
                    held.id == objs.knife.id ||
                        items[held].wearpos1 == org.rsmod.game.type.obj.Wearpos.RightHand.slot &&
                            items[held].param(params.attack_slash) > 0
                }
            if (!hasTool && player.feet?.id != 29806)
                mes("You need a knife or a slash weapon to cut the web.")
            else {
                locs.change(it.loc, CryptLocs.cutWeb, 100)
                passWeb(it.loc)
            }
        }
        onOpLoc1(CryptLocs.cutWeb) { passWeb(it.loc) }
        for (web in listOf(CryptLocs.web, CryptLocs.cutWeb)) onOpLoc2(web) {
            val inside =
                players.findAll(ZoneKey.from(CoordGrid(1840, 9900)), 3).count { p ->
                    p.coords.level == 0 && p.coords.x in 1831..1851 && p.coords.z in 9891..9911
                }
            mes("There are $inside adventurers in the crypt.")
        }
        for ((web, inside) in listOf(CryptLocs.webIn to true, CryptLocs.webOut to false)) {
            onOpLoc1(web) {
                if (
                    !inside ||
                        coords.z < it.loc.z ||
                        choice2("Enter Sarachnis' crypt?", true, "Stay here.", false)
                )
                    crossWeb(it.loc.x, inside && coords.z >= it.loc.z)
            }
            onOpLoc2(web) { crossWeb(it.loc.x, inside && coords.z >= it.loc.z) }
        }
    }

    private fun ProtectedAccess.passWeb(web: BoundLocInfo) {
        telejump(CoordGrid(web.x, if (coords.z > web.z) web.z else web.z + 1))
    }

    private fun ProtectedAccess.crossWeb(x: Int, inside: Boolean) {
        if (!player.members) return
        telejump(CoordGrid(x, if (inside) 9911 else 9913))
    }

    private suspend fun ProtectedAccess.talkToOlbertus() {
        if (!player.members) return
        if (vars[CryptBits.olbertus] != 0) {
            if (vars[CryptBits.relief] == 0 && CryptItems.coin !in inv && CryptItems.coin !in bank)
                invAdd(inv, CryptItems.coin)
            else mes("The temple coin fits the stone relief in the crypt.")
            return
        }
        when (
            menu(
                "Olbertus has a strange coin.",
                true,
                listOf("Give him five cut dragonstones.", "Fight him for the coin.", "Leave."),
            )
        ) {
            0 -> payOlbertus()
            1 -> fight.start(this)
        }
    }

    private fun ProtectedAccess.payOlbertus() {
        if (!player.members || vars[CryptBits.olbertus] != 0) return
        if (vars[CryptBits.faith] != 0) {
            mes("You must defeat the beast.")
            return
        }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.dragonstone.id, 5)
                add(bag, CryptItems.coin.id, 1)
            }
        if (result.success) {
            vars[CryptBits.faith] = 100
            vars[CryptBits.olbertus] = 1
            mes("Olbertus gives you the temple coin.")
        } else mes("Olbertus wants five cut dragonstones.")
    }

    private fun ProtectedAccess.unlock() {
        if (!player.members || vars[CryptBits.relief] != 0) return
        if (!invDel(inv, CryptItems.coin).success) {
            mes("There is a slot where something round might fit.")
            return
        }
        vars[CryptBits.relief] = 1
        telejump(CoordGrid(1810, 9954))
    }

    private fun ProtectedAccess.templeDoor(
        tile: CoordGrid,
        bit: org.rsmod.game.type.varbit.VarBitType,
        west: Boolean,
    ) {
        if (!player.members) return
        if (vars[bit] == 0 && CryptItems.key !in inv) {
            mes("The door is locked.")
            return
        }
        vars[bit] = 1
        if (west) telejump(CoordGrid(tile.x, if (coords.z > tile.z) tile.z else tile.z + 1))
        else telejump(CoordGrid(if (coords.x >= tile.x) tile.x - 1 else tile.x, tile.z))
    }

    private fun ProtectedAccess.offer(slot: Int, r: PrayerRemainsRecipe) {
        if (!player.members || vars[CryptBits.relief] == 0 || inv[slot]?.id != r.obj.id) return
        if (statBase(stats.prayer) < r.prayerLevel) {
            mes("You need a Prayer level of ${r.prayerLevel} to offer these remains.")
            return
        }
        if (!invDel(inv, r.obj, slot = slot).success) return
        anim(seqs.human_pray)
        val faith = vars[CryptBits.faith]
        if (faith >= 100) statAdvance(stats.prayer, r.xp * 3)
        val weight =
            when (r.obj.id) {
                objs.babydragon_bones.id,
                objs.dragon_bones.id -> 1
                objs.lava_dragon_bones.id -> 2
                objs.superior_dragon_bones.id -> 3
                else -> 0
            }
        if (weight == 0) return
        val next = (faith + weight).coerceAtMost(200)
        vars[CryptBits.faith] = next
        mes(faithMessage(next))
        if (faith < 200 && next == 200)
            ground.add(CryptItems.key, CoordGrid(1810, 9950), constants.lootdrop_duration, player)
    }

    private fun faithMessage(faith: Int): String =
        when {
            faith >= 200 -> "The shrine is hot to the touch as it swells with Ralos' power."
            faith >= 150 -> "The shrine is warm to the touch. Ralos' power is growing."
            faith >= 100 -> "The shrine is smooth to the touch. You faintly sense Ralos' power."
            faith >= 50 -> "The shrine is cold to the touch, you do not sense any divine presence."
            else -> "The shrine is icy to the touch, you do not sense any divine presence."
        }
}
