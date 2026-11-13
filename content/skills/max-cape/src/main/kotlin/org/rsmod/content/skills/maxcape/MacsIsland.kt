package org.rsmod.content.skills.maxcape

import jakarta.inject.Inject
import org.rsmod.api.config.refs.jingles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.skillcapes.SkillcapeTrimming
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.stat.StatType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object MacNpcs : NpcReferences() {
    val mac = find("mac")
    val boulder = find("max_boulder")
}

internal object MacSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<MacSpawns>("masters.toml")
    }
}

internal object MacNpcEdits : NpcEditor() {
    init {
        edit(MacNpcs.boulder) { defaultMode = NpcMode.None }
    }
}

internal object MacLocs : LocReferences() {
    val tree = find("max_evergreen")
    val stump = find("max_treestump")
    val rocks = find("max_rockslide")
    val mined = find("max_rockslide1")
    val log = find("max_log")
    val boat = find("max_boat")
}

internal object MaxCapeSets : ObjReferences() {
    val sets =
        listOf(
                "",
                "_firecape",
                "_saradomin",
                "_zamorak",
                "_guthix",
                "_anma",
                "_ardy",
                "_infernalcape",
                "_saradomin2",
                "_zamorak2",
                "_guthix2",
                "_assembler",
                "_mythical",
                "_assembler_masori",
                "_dizanas",
            )
            .map { suffix -> find("skillcape_max$suffix") to find("skillcape_max_hood$suffix") }
}

public class MacsIsland
@Inject
constructor(
    private val locs: LocRepository,
    private val objTypes: ObjTypeList,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(MacLocs.tree) {
            if (!mastered(stats.woodcutting)) return@onOpLoc1
            if (Woodcutting.findAxe(player, objTypes) == null) {
                mes("You need an axe to cut the tree.")
                return@onOpLoc1
            }
            locs.change(it.loc, MacLocs.stump, 20)
        }
        onOpLoc1(MacLocs.stump) {
            if (mastered(stats.agility) && mastered(stats.woodcutting))
                telejump(if (coords.x >= 2830) CoordGrid(2827, 3543) else CoordGrid(2830, 3543))
        }
        onOpLoc1(MacLocs.rocks) {
            if (!mastered(stats.mining)) return@onOpLoc1
            if (Mining.findPickaxe(player, objTypes) == null) {
                mes("You need a pickaxe to mine the rockslide.")
                return@onOpLoc1
            }
            locs.change(it.loc, MacLocs.mined, 20)
        }
        onOpLoc1(MacLocs.mined) {
            if (mastered(stats.mining) && mastered(stats.defence))
                telejump(if (coords.z >= 3541) CoordGrid(2820, 3539) else CoordGrid(2822, 3541))
        }
        onOpNpc1(MacNpcs.boulder) {
            if (!mastered(stats.strength)) return@onOpNpc1
            val next = if (it.npc.coords.x == 2814) CoordGrid(2813, 3534) else CoordGrid(2814, 3534)
            it.npc.telejump(collision, next)
            mes("You push the boulder aside.")
        }
        onOpLoc1(MacLocs.log) {
            if (!mastered(stats.crafting) || !mastered(stats.construction)) return@onOpLoc1
            if (vars[CapeBits.raft] == 0) {
                vars[CapeBits.raft] = 1
                mes("You make a raft from the old log.")
            } else {
                vars[CapeBits.raft] = 0
                telejump(CoordGrid(2794, 3538))
                mes("The raft drifts away as you land on the island.")
            }
        }
        onOpLoc1(MacLocs.boat) { if (player.members) telejump(CoordGrid(2726, 3589)) }

        onOpNpc1(MacNpcs.mac) {
            if (!player.members) return@onOpNpc1
            val sets = MaxCapeSets.sets.filter { (cape, _) -> cape in inv }
            val options = mutableListOf("What do you have in your sack?")
            if (sets.isNotEmpty()) options += listOf("Sell a cape back.", "Claim another hood.")
            options += "Bye."
            when (val action = menu("Mac", true, options)) {
                0 -> buy()
                1,
                2 ->
                    if (sets.isNotEmpty()) {
                        val selected =
                            if (sets.size == 1) sets.first()
                            else {
                                val i =
                                    menu("Which cape?", true, sets.map { objTypes[it.first].name })
                                sets.getOrNull(i) ?: return@onOpNpc1
                            }
                        val (cape, hood) = selected
                        if (action == 1) sell(cape, hood)
                        else if (cape in inv && invAdd(inv, hood).failure)
                            mes("You need room for the hood.")
                    }
            }
        }
        for ((cape, hood) in MaxCapeSets.sets) {
            onOpNpcU(MacNpcs.mac, cape) { sell(cape, hood) }
            onOpNpcU(MacNpcs.mac, hood) { mes("Mac asks you to show him the cape.") }
        }
    }

    private fun ProtectedAccess.mastered(stat: StatType): Boolean {
        if (!player.members) return false
        if (statBase(stat) >= 99) return true
        mes("You need level 99 ${stat.displayName} to do that.")
        return false
    }

    private suspend fun ProtectedAccess.buy() {
        if (!SkillcapeTrimming.definitions.all { statBase(it.stat) >= 99 }) {
            mes("None of yer bis'ness.")
            return
        }
        if (inv.freeSpace() < 2) {
            mes("You need two free inventory spaces.")
            return
        }
        if (!choice2("Buy Mac's cape and hood for 2,277,000 coins.", true, "No.", false)) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.coins.id, 2_277_000)
                add(bag, objs.max_cape_inv.id, 1)
                add(bag, objs.max_hood.id, 1)
            }
        if (result.failure) {
            mes("You'll need 2,277,000 coins and room for the cape and hood.")
            return
        }
        vars[CapeBits.emote] = 1
        midiJingle(jingles.cape_of_accomplishment)
        mes("Mac takes your coins and gives you his cape and hood.")
    }

    private suspend fun ProtectedAccess.sell(cape: ObjType, hood: ObjType) {
        if (
            !player.members ||
                !choice2("Sell your cape and hood for 1,821,600 coins.", true, "No.", false)
        )
            return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, cape.id, 1)
                delete(bag, hood.id, 1)
                add(bag, objs.coins.id, 1_821_600)
            }
        if (result.failure) mes("Show Mac the matching cape and hood.")
        else mes("Mac buys the cape and hood back from you.")
    }
}
