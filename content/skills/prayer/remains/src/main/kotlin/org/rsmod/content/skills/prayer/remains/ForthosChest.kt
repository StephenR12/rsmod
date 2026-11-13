package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GrubbyLocs : LocReferences() {
    val door = find("hosdun_grubby_door_locked")
    val chest = find("hosdun_grubby_chest_closed")
}

internal object GrubbyBits : VarBitReferences() {
    val open = find("hosdun_chest_status")
    val warning = find("hosdun_chest_warning")
}

internal object GrubbyLoot : ObjReferences() {
    data class Reward(val item: ObjType, val count: Int, val weight: Int)

    val main =
        listOf(
            Reward(objs.law_rune, 200, 10),
            Reward(objs.death_rune, 200, 10),
            Reward(objs.astral_rune, 200, 10),
            Reward(objs.dragon_dart_tip, 50, 2),
            Reward(objs.dragon_arrowheads, 100, 2),
            Reward(objs.blood_rune, 200, 10),
            Reward(find("crystal_key"), 1, 6),
            Reward(find("cert_dragon_bones"), 10, 6),
            Reward(find("cert_dragonhide_red"), 10, 6),
            Reward(find("cert_unidentified_toadflax"), 10, 8),
            Reward(find("cert_unidentified_ranarr"), 10, 8),
            Reward(find("cert_unidentified_snapdragon"), 10, 7),
            Reward(find("cert_unidentified_torstol"), 5, 7),
            Reward(objs.coins, 10000, 8),
        )
    val orange = find("hosdun_orange_egg_sac")
    val blue = find("hosdun_blue_egg_sac")
}

public class ForthosChest @Inject constructor(private val ground: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            org.rsmod.api.player.vars.VarPlayerIntMapSetter.set(player, GrubbyBits.open, 0)
        }
        onOpLoc1(GrubbyLocs.door) {
            if (!player.members) return@onOpLoc1
            if (stat(stats.thieving) < 57) {
                mes("You need a Thieving level of 57 to pick the lock.")
                return@onOpLoc1
            }
            statAdvance(stats.thieving, 10.0)
            telejump(CoordGrid(if (coords.x >= it.loc.x) it.loc.x - 1 else it.loc.x, it.loc.z))
        }
        onOpLoc1(GrubbyLocs.chest) { openChest() }
        onOpLocU(GrubbyLocs.chest, CryptDrops.key) { openChest() }
        onOpLoc2(GrubbyLocs.chest) {
            if (
                choice2(
                    "Warn me when I have fewer than eleven empty slots.",
                    true,
                    "Disable the inventory warning.",
                    false,
                )
            )
                vars[GrubbyBits.warning] = 0
            else vars[GrubbyBits.warning] = 1
        }
    }

    private suspend fun ProtectedAccess.openChest() {
        if (!player.members || vars[GrubbyBits.open] != 0) return
        if (CryptDrops.key !in inv) {
            mes("The chest is locked.")
            return
        }
        if (
            inv.freeSpace() < 11 &&
                vars[GrubbyBits.warning] == 0 &&
                !choice2("Open the chest and drop excess loot?", true, "Leave it closed.", false)
        )
            return
        if (!invDel(inv, CryptDrops.key).success) return
        vars[GrubbyBits.open] = 1
        fun give(item: ObjType, count: Int = 1) {
            player.invAddOrDrop(ground, item, count)
        }
        repeat(2) {
            when (random.of(20)) {
                in 0..6 -> give(objs.shark, 4)
                in 7..18 -> give(objs.egg_potato, 4)
                else -> {
                    give(objs.saradomin_brew_2, 3)
                    give(objs.super_restore_2)
                }
            }
        }
        when (random.of(20)) {
            in 0..7 -> {
                give(objs.super_attack_2)
                give(objs.super_strength_2)
                give(objs.super_defence_2)
            }
            in 8..15 -> {
                give(objs.ranging_potion_2)
                give(objs.super_defence_2)
            }
            16 -> give(objs.super_restore_3, 2)
            else -> give(objs.prayer_potion_3, 2)
        }
        var roll = random.of(100)
        val main =
            GrubbyLoot.main.first {
                roll -= it.weight
                roll < 0
            }
        give(main.item, main.count)
        if (random.of(25) == 0) give(GrubbyLoot.orange)
        if (random.of(25) == 0) give(GrubbyLoot.blue)
        mes("You unlock the grubby chest.")
        try {
            delay(2)
        } finally {
            vars[GrubbyBits.open] = 0
        }
    }
}
