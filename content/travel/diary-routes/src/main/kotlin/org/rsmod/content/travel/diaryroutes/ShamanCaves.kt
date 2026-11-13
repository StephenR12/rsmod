package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ShamanLocs : LocReferences() {
    val vegetation =
        listOf(
                "kharazi_jungle_tree1",
                "kharazi_jungle_tree2",
                "kharazi_jungle_plant1",
                "kharazi_jungle_plant2",
            )
            .map(::find)
    val cleared =
        listOf(
                "junglestump_kharazi",
                "junglestump_kharazi",
                "kharazi_jungle_plant1_slashed",
                "kharazi_jungle_plant2_slashed",
            )
            .map(::find)
    val rocks = (1..3).map { find("lgshamancaverock$it") }
    val exits = listOf(find("shaman_entrance_caver"), find("shaman_entrance_cavel"))
    val book = find("shaman_bookcase")
    val crevice = find("shaman_cave_crevice")
    val locks = listOf(find("lglockpickgatebottoml"), find("lglockpickgatebottomr"))
    val boulders = (1..3).map { find("mine_test_boulder$it") }
    val gates = listOf(find("lgstrengthtrialgatel"), find("lgstrengthtrialgater"))
}

internal object ShamanObjs : ObjReferences() {
    val notes = listOf(find("thkaramjamap"), find("thkaramjamapcomp"))
    val blades =
        listOf("machette", "machette_opal", "machette_jade", "machette_redtopaz").map(::find)
    val lockpick = find("lockpick")
    val rock = find("rock")
}

public class ShamanCaves
@Inject
constructor(private val items: ObjTypeList, private val locs: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        with(ShamanLocs) {
            for ((index, plant) in vegetation.withIndex()) onOpLoc1(plant) { chop(it.loc, index) }
            for (rock in rocks) onOpLoc1(rock) {
                if (!player.members || vars[varps.legends_quest] < 1)
                    mes("You haven't learned how to enter these caves.")
                else if (agile()) telejump(CoordGrid(2773, 9341))
            }
            for (exit in exits) onOpLoc1(exit) { telejump(CoordGrid(2782, 2934)) }
            onOpLoc1(book) {
                if (agile()) {
                    mes("You find a narrow crevice behind the bookcase and squeeze through.")
                    telejump(CoordGrid(2800, 9341))
                }
            }
            onOpLoc1(crevice) { mes("The crevice leads through the rock to a bookcase.") }
            onOpLoc2(crevice) { if (agile()) telejump(CoordGrid(2794, 9339)) }
            for (gate in locks) {
                onOpLoc1(gate) { unlock(it.loc) }
                onOpLocU(gate, ShamanObjs.lockpick) { unlock(it.loc) }
                onOpLoc2(gate) {
                    mes("The ancient gate has a complex lock. A lockpick might open it.")
                }
            }
            for (rock in boulders) onOpLoc1(rock) { smash(it.loc) }
            for (gate in gates) onOpLoc1(gate) {
                if (coords.z >= it.loc.z && stat(stats.strength) < 50)
                    mes("You need level 50 Strength to push the ancient gate open.")
                else passGate(it.loc)
            }
        }
    }

    private suspend fun ProtectedAccess.chop(loc: BoundLocInfo, index: Int) {
        if (
            !player.members ||
                vars[varps.legends_quest] < 1 ||
                (vars[varps.legends_quest] < 75 && ShamanObjs.notes.none { it in inv })
        ) {
            mes("You'll need Radimus's instructions to navigate the dense jungle.")
            return
        }
        val axe = Woodcutting.findAxe(player, items)
        if (axe == null || ShamanObjs.blades.none { it in inv || it in worn }) {
            mes("You need a usable axe and a machete to cut through the dense jungle.")
            return
        }
        anim(items[axe].axeWoodcuttingAnim)
        gatheringDelay(3)
        if (
            !isWithinDistance(loc, 1) ||
                locs.findExact(loc.coords, ShamanLocs.vegetation[index]) == null
        )
            return
        locs.change(loc, ShamanLocs.cleared[index], 40)
        resetAnim()
        if (random.of(2) == 0 && invAdd(inv, objs.logs).success)
            statAdvance(stats.woodcutting, 100.0)
    }

    private fun ProtectedAccess.agile(): Boolean {
        if (stat(stats.agility) >= 50) return true
        mes("You need level 50 Agility to squeeze through the crevice.")
        return false
    }

    private fun ProtectedAccess.unlock(loc: BoundLocInfo) {
        if (coords.z < loc.z || stat(stats.thieving) >= 50 && ShamanObjs.lockpick in inv)
            passGate(loc)
        else mes("You need a lockpick and level 50 Thieving to open the ancient gate.")
    }

    private fun ProtectedAccess.passGate(loc: BoundLocInfo) {
        telejump(CoordGrid(loc.x, if (coords.z < loc.z) loc.z else loc.z - 1))
    }

    private suspend fun ProtectedAccess.smash(loc: BoundLocInfo) {
        val tool = Mining.findPickaxe(player, items)
        if (tool == null || stat(stats.mining) < 52) {
            mes("You need a usable pickaxe and level 52 Mining to smash the boulder.")
            return
        }
        anim(items[tool].pickaxeAnim)
        delay(2)
        if (invAdd(inv, ShamanObjs.rock).success) statAdvance(stats.mining, 35.0)
        // Each boulder blocks two north/south tiles. A new one falls behind the player.
        telejump(CoordGrid(loc.x, if (coords.z > loc.z) loc.z - 1 else loc.z + 2))
        resetAnim()
    }
}
