package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.godWarsEssenceCost
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.prayer.remains.PrayerGodWarsGod
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.WeaponCategory
import org.rsmod.game.type.stat.StatType
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal fun inMainDungeon(c: CoordGrid): Boolean =
    c.level in 0..3 && c.x in 2816..2943 && c.z in 5184..5375

internal object GwdLocs : LocReferences() {
    val hole = find("godwars_entrance_groundhole01")
    val ropedHole = find("godwars_entrance_groundhole02")
    val holeBottom = find("godwars_entrance_rope_bottom")
    val boulder = find("godwars_entrance_boulder_moverock01")
    val crack = find("godwars_little_hole")
    val corpse = find("godwars_dead_paladin")
    val bare = listOf(find("godwars_rock_no_rope1"), find("godwars_rock_no_rope2"))
    val tied = listOf(find("godwars_rock_rope1"), find("godwars_rock_rope2"))
    val bottom = listOf(find("godwars_rock_rope_bottom1"), find("godwars_rock_rope_bottom2"))
    val door = find("godwars_icecave_bandos_door")
    val bridge = find("godwars_bridge_clickzone")
    val pillar = find("godwars_grapple_pillar")
    val chest = find("godwars_crate")
    val normalDoor = find("godwars_dungeon_door_normal")
    val privateDoor = find("godwars_dungeon_door_private")
}

internal object GwdBits : VarBitReferences() {
    val talked = find("godwars_diary_found")
    val rope = find("godwars_entrance_rope")
    val ropes = listOf(find("godwars_saradomin_rope1"), find("godwars_saradomin_rope2"))
    val counters =
        PrayerGodWarsGod.entries.associateWith { find("godwars_counter_${it.name.lowercase()}") }
}

internal object GwdNpcs : NpcReferences() {
    val knight = find("godwars_dying_paladin")
}

internal object GwdObjs : ObjReferences() {
    val notes = find("godwars_knights_scroll_closed")
    val openNotes = find("godwars_knights_scroll_open")
    val crossbow = find("xbows_crossbow_bronze")
}

internal object GwdTimers : TimerReferences() {
    val essence = find("godwars_essence_watch")
}

private data class Chamber(
    val god: PrayerGodWarsGod,
    val inside: CoordGrid,
    val outside: CoordGrid,
    val dx: Int,
    val dz: Int,
) {
    fun inside(c: CoordGrid): Boolean = (c.x - outside.x) * dx + (c.z - outside.z) * dz > 0
}

class GodWarsDungeonAccess
@Inject
constructor(private val items: ObjTypeList, private val players: PlayerRepository) :
    PluginScript() {
    private val chambers =
        mapOf(
            26502 to
                Chamber(
                    PrayerGodWarsGod.Armadyl,
                    CoordGrid(2839, 5296, 2),
                    CoordGrid(2839, 5294, 2),
                    0,
                    1,
                ),
            26503 to
                Chamber(
                    PrayerGodWarsGod.Bandos,
                    CoordGrid(2864, 5354, 2),
                    CoordGrid(2862, 5354, 2),
                    1,
                    0,
                ),
            26504 to
                Chamber(
                    PrayerGodWarsGod.Saradomin,
                    CoordGrid(2907, 5265, 0),
                    CoordGrid(2909, 5265, 0),
                    -1,
                    0,
                ),
            26505 to
                Chamber(
                    PrayerGodWarsGod.Zamorak,
                    CoordGrid(2925, 5331, 2),
                    CoordGrid(2925, 5333, 2),
                    0,
                    -1,
                ),
        )

    override fun ScriptContext.startup() {
        onOpNpc1(GwdNpcs.knight) {
            if (!player.members) return@onOpNpc1
            vars[GwdBits.talked] = 1
            mes("The dying knight asks you to take his notes to Sir Tiffy in Falador.")
            notes()
        }
        onOpLoc3(GwdLocs.corpse) { if (vars[GwdBits.talked] != 0) notes() }
        onOpLoc1(GwdLocs.boulder) {
            if (required(stats.strength, 60, boosted = true))
                telejump(CoordGrid(2899, if (player.coords.z < 3716) 3719 else 3715))
        }
        onOpLoc1(GwdLocs.crack) {
            if (required(stats.agility, 60, boosted = true))
                telejump(if (it.loc.z < 3716) CoordGrid(2904, 3720) else CoordGrid(2901, 3712))
        }
        onOpLoc1(GwdLocs.hole) { tie(GwdBits.rope, true) }
        onOpLocU(GwdLocs.hole, objs.rope) { tie(GwdBits.rope, true) }
        onOpLoc1(GwdLocs.ropedHole) {
            if (!player.members || vars[GwdBits.talked] == 0)
                mes("Talk to the wounded knight before entering.")
            else {
                telejump(CoordGrid(2882, 5311, 2))
                watch(player)
            }
        }
        onOpLoc1(GwdLocs.holeBottom) {
            telejump(CoordGrid(2916, 3745))
            clear(player)
        }

        val down = listOf(CoordGrid(2916, 5300, 1), CoordGrid(2919, 5274, 0))
        val up = listOf(CoordGrid(2912, 5300, 2), CoordGrid(2922, 5274, 1))
        for (i in 0..1) {
            onOpLoc1(GwdLocs.bare[i]) {
                if (required(stats.agility, 70)) tie(GwdBits.ropes[i], false)
            }
            onOpLocU(GwdLocs.bare[i], objs.rope) {
                if (required(stats.agility, 70)) tie(GwdBits.ropes[i], false)
            }
            onOpLoc1(GwdLocs.tied[i]) {
                if (required(stats.agility, 70) && vars[GwdBits.ropes[i]] != 0) telejump(down[i])
            }
            onOpLoc1(GwdLocs.bottom[i]) { if (vars[GwdBits.ropes[i]] != 0) telejump(up[i]) }
        }
        onOpLoc1(GwdLocs.door) {
            val entering = player.coords.x >= 2851
            if (entering) {
                if (!required(stats.strength, 70)) return@onOpLoc1
                val hammers = listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand)
                if (hammers.none { it in inv || it in worn }) {
                    mes("You need a hammer to bang on the door.")
                    return@onOpLoc1
                }
            }
            telejump(CoordGrid(if (entering) 2850 else 2852, 5333, 2))
        }
        onOpLoc1(GwdLocs.bridge) {
            if (!required(stats.hitpoints, 70)) return@onOpLoc1
            val north = player.coords.z < 5340
            telejump(CoordGrid(2885, if (north) 5347 else 5331, 2))
            statSub(stats.prayer, 255, 0)
            mes("The icy water drains your Prayer.")
        }
        onApLoc1(GwdLocs.pillar) {
            val north = player.coords.z >= 5276
            val position = CoordGrid(2871, if (north) 5279 else 5269, 2)
            if (!isWithinDistance(position, 2)) {
                apRange(9)
                return@onApLoc1
            }
            grapple(north)
        }
        onOpLoc1(GwdLocs.pillar) { grapple(player.coords.z >= 5276) }
        onOpLoc1(GwdLocs.chest) {
            val crossbow =
                items.getOrNull(player.righthand)?.weaponCategory == WeaponCategory.Crossbow.id ||
                    inv.filterNotNull().any {
                        items[it].weaponCategory == WeaponCategory.Crossbow.id
                    }
            if (!crossbow) invAdd(inv, GwdObjs.crossbow)
            if (objs.mith_grapple !in inv && objs.mith_grapple !in worn)
                invAdd(inv, objs.mith_grapple)
        }
        for (door in listOf(GwdLocs.normalDoor, GwdLocs.privateDoor)) {
            onOpLoc1(door) { chambers[it.loc.id]?.let { chamber -> enter(chamber) } }
            onOpLocU(door, WildernessFollowerKeys.key) {
                chambers[it.loc.id]?.let { chamber -> enter(chamber) }
            }
            onOpLoc3(door) {
                val chamber = chambers[it.loc.id] ?: return@onOpLoc3
                val count =
                    players.findAll(ZoneKey.from(chamber.inside), 2).count {
                        it.coords.level == chamber.inside.level &&
                            it.coords.chebyshevDistance(chamber.inside) <= 12 &&
                            chamber.inside(it.coords)
                    }
                mes("You can see $count adventurers inside.")
            }
        }
        onEvent<NpcKillEvents.Credited> {
            if (!inMainDungeon(npc.spawnCoords) || !inMainDungeon(player.coords)) return@onEvent
            val god = godWarsAllegiance(npc.id) ?: return@onEvent
            val bit = GwdBits.counters.getValue(god)
            VarPlayerIntMapSetter.set(player, bit, (player.vars[bit] + 1).coerceAtMost(4000))
            watch(player)
        }
        onPlayerLogin { if (inMainDungeon(player.coords)) watch(player) else clear(player) }
        onPlayerSoftTimer(GwdTimers.essence) { if (!inMainDungeon(player.coords)) clear(player) }
    }

    private fun ProtectedAccess.required(
        stat: StatType,
        level: Int,
        boosted: Boolean = false,
    ): Boolean {
        if (!player.members) return false
        if ((if (boosted) player.stat(stat) else player.statBase(stat)) >= level) return true
        val name =
            when (stat) {
                stats.strength -> "Strength"
                stats.agility -> "Agility"
                stats.hitpoints -> "Hitpoints"
                else -> "Ranged"
            }
        mes("You need a $name level of $level to cross here.")
        return false
    }

    private fun ProtectedAccess.grapple(north: Boolean) {
        if (!required(stats.ranged, 70)) return
        if (
            items.getOrNull(player.righthand)?.weaponCategory != WeaponCategory.Crossbow.id ||
                player.quiver?.id != objs.mith_grapple.id
        ) {
            mes("Equip a crossbow and mith grapple to cross the chasm.")
            return
        }
        telejump(CoordGrid(2871, if (north) 5269 else 5279, 2))
    }

    private fun ProtectedAccess.notes() {
        if (GwdObjs.notes !in inv && GwdObjs.openNotes !in inv) invAdd(inv, GwdObjs.notes)
    }

    private fun ProtectedAccess.tie(bit: VarBitType, entrance: Boolean) {
        if (!player.members || vars[bit] != 0) return
        if (entrance && vars[GwdBits.talked] == 0) {
            mes("Talk to the wounded knight before entering.")
            return
        }
        if (invDel(inv, objs.rope).failure) {
            mes("You need a rope to tie here.")
            return
        }
        vars[bit] = 1
        mes("You tie the rope securely.")
    }

    private fun ProtectedAccess.enter(chamber: Chamber) {
        if (!player.members) return
        if (chamber.inside(player.coords)) {
            mes("Use the altar inside the chamber to leave.")
            return
        }
        val cost = player.godWarsEssenceCost()
        val counter = GwdBits.counters.getValue(chamber.god)
        if (vars[counter] >= cost) vars[counter] -= cost
        else if (invDel(inv, WildernessFollowerKeys.key).failure) {
            mes("You need $cost essence or an ecumenical key to enter.")
            return
        }
        telejump(chamber.inside)
        watch(player)
    }

    private fun watch(player: Player) {
        player.softTimer(GwdTimers.essence, 1)
    }

    private fun clear(player: Player) {
        for (bit in GwdBits.counters.values) if (player.vars[bit] != 0)
            VarPlayerIntMapSetter.set(player, bit, 0)
        player.clearSoftTimer(GwdTimers.essence)
    }
}
