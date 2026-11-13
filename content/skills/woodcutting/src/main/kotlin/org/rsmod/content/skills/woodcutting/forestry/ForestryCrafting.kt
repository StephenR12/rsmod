package org.rsmod.content.skills.woodcutting.forestry

import jakarta.inject.Inject
import org.rsmod.api.config.FellingAxes
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocU
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.stat.StatType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ForestryCrafting
@Inject
constructor(
    private val items: ObjTypeList,
    private val locs: LocRepository,
    private val locTypes: LocTypeList,
) : PluginScript() {
    private val rations = mapOf(6020 to 2, 6022 to 4, 6024 to 6, 6028 to 8, 6026 to 10, 6030 to 12)
    private val foods =
        listOf(
            listOf(315, 319, 325, 347, 355, 333, 351, 339, 329, 3381),
            listOf(2140, 2142, 361, 3144, 10136, 5003, 379, 365, 373, 7946),
            listOf(385, 397, 13441, 11936, 391),
        )
    private val anvils = setOf(2097, 4306, 6150, 22725, 28563, 31623, 51501, 40725, 39242, 2031)

    override fun ScriptContext.startup() {
        for ((leaf, base) in rations) for ((tier, foodGroup) in foods.withIndex()) for (food in
            foodGroup) {
            onOpHeldU(items.getValue(leaf), items.getValue(food)) {
                if (!levels(35, stats.woodcutting, 35, stats.cooking)) return@onOpHeldU
                if (make(28157, base * (tier + 1), mapOf(leaf to 1, food to 1)))
                    statAdvance(stats.cooking, 51.1)
                delay(3)
            }
        }
        val herbs = listOf(261, 263, 3000, 265, 2481, 267, 269, 211, 213, 3051, 215, 2485, 217, 219)
        for (herb in herbs) onOpHeldU(items.getValue(28154), items.getValue(herb)) {
            if (
                levels(68, stats.woodcutting, 50, stats.farming) &&
                    make(28152, 40, mapOf(28154 to 1, herb to 1))
            )
                delay(3)
        }
        onOpHeldU(items.getValue(28159), objs.iron_bar) {
            if (
                levels(35, stats.woodcutting, 35, stats.smithing) &&
                    make(28161, 50, mapOf(28159 to 1, 2351 to 1))
            ) {
                statAdvance(stats.crafting, 32.0)
                delay(3)
            }
        }
        onOpHeldU(items.getValue(28166), items.getValue(1741)) {
            if (!levels(50, stats.woodcutting, 50, stats.crafting)) return@onOpHeldU
            val mats = mutableMapOf(28166 to 1, 1741 to 1)
            if (items.getValue(29920) !in inv && items.getValue(29920) !in player.worn) {
                if (items.getValue(1733) !in inv) {
                    mes("You need a needle and thread to sew the pouch.")
                    return@onOpHeldU
                }
                mats[1734] = 1
            }
            if (make(28163, 1, mats)) statAdvance(stats.crafting, 47.0)
        }
        val harness = mapOf(28146 to 1, 1539 to 45, 954 to 2, 2361 to 3)
        for (material in harness.keys - 28146) onOpHeldU(
            items.getValue(28146),
            items.getValue(material),
        ) {
            if (levels(75, stats.woodcutting, 75, stats.smithing)) make(28149, 1, harness)
        }
        for ((i, axe) in FellingAxes.ordinary.withIndex()) {
            onOpHeldU(axe, items.getValue(28177)) { attach(i, it.firstSlot) }
        }
        for (anvil in anvils + 36552) onOpLocU(locTypes.getValue(anvil), items.getValue(28177)) {
            val slot =
                inv.indices.firstOrNull { slot ->
                    FellingAxes.ordinary.any { it.id == inv[slot]?.id }
                } ?: return@onOpLocU
            attach(FellingAxes.ordinary.indexOfFirst { it.id == inv[slot]?.id }, slot)
        }
    }

    private fun ProtectedAccess.levels(first: Int, a: StatType, second: Int, b: StatType): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to do that.")
            return false
        }
        if (stat(a) >= first && stat(b) >= second) return true
        mes("You need level $first ${a.displayName} and level $second ${b.displayName} to do that.")
        return false
    }

    private fun ProtectedAccess.make(
        output: Int,
        count: Int,
        materials: Map<Int, Int>,
        vars: Int = 0,
    ): Boolean {
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                for ((id, n) in materials) delete(bag, id, n)
                add(bag, output, count, vars = vars)
            }
        if (!tx.success) mes("You need all the materials and space for the finished item.")
        return tx.success
    }

    private suspend fun ProtectedAccess.attach(index: Int, slot: Int) {
        val axe = inv[slot] ?: return
        if (axe.id != FellingAxes.ordinary[index].id || !player.members) return
        val crystal = index in 8..9
        val level = FellingAxes.levels[index]
        if (stat(stats.smithing) < level || crystal && stat(stats.crafting) < 71) {
            mes("You need level $level Smithing" + if (crystal) " and 71 Crafting." else ".")
            return
        }
        if (
            !crystal &&
                listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand).none {
                    it in inv || it in player.worn
                }
        ) {
            mes("You'll need a hammer to attach the handle.")
            return
        }
        val nearby =
            (-1..1).any { dx ->
                (-1..1).any { dz ->
                    locs.findAll(coords.translate(dx, dz)).any {
                        if (crystal) it.id == 36552
                        else
                            it.id in anvils &&
                                (it.id != 2031 || player.vars[ForestryQuest.doric] >= 100)
                    }
                }
            }
        if (!nearby) {
            mes(
                if (crystal) "Use the handle next to a singing bowl."
                else "Use the handle next to an anvil."
            )
            return
        }
        if (crystal && vars[varbits.song_of_the_elves] < 194) return
        if (
            !choice2("Permanently attach the felling axe handle.", true, "Cancel.", false) ||
                inv[slot] != axe
        )
            return
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, axe.id, 1, slot)
                delete(bag, 28177, 1)
                add(bag, FellingAxes.tools[index].id, 1, vars = axe.vars, slot = slot)
            }
        if (tx.success) {
            statAdvance(stats.smithing, 5.0)
            if (crystal) statAdvance(stats.crafting, 5.0)
        }
    }
}

internal object ForestryQuest : org.rsmod.api.type.refs.varp.VarpReferences() {
    val doric = find("doricquest")
}
