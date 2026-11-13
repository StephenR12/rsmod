package org.rsmod.content.skills.fletching.valetotems

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.death.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object StalkerLocs : LocReferences() {
    val north = find("soc_cave_entrance")
    val exit = find("soc_cave_exit")
    val shortcut = find("stalker_den_external_shortcut_outside")
    val shortcutExit = find("stalker_den_external_shortcut_inside")
    val stone = find("stalker_den_stepping_stone")
}

internal object StalkerProgress : VarBitReferences() {
    val quest = find("soc")
}

internal object StalkerNpcs : NpcReferences() {
    val stages = listOf("juvenile", "mature", "elder").map { find(it + "_custodian_stalker") }
}

internal object StalkerQuestVars : VarpReferences() {
    val legends = find("legendsquest")
}

public class StalkerDen
@Inject
constructor(private val registry: NpcDropTableRegistry, private val items: ObjTypeList) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(StalkerLocs.north) { enter(CoordGrid(1296, 9767), 0) }
        onOpLoc1(StalkerLocs.exit) { telejump(CoordGrid(1296, 3375)) }
        onOpLoc1(StalkerLocs.shortcut) { enter(CoordGrid(1323, 9767), 59) }
        onOpLoc1(StalkerLocs.shortcutExit) { enter(CoordGrid(1324, 3363), 59) }
        onOpLoc1(StalkerLocs.stone) {
            enter(CoordGrid(1308, if (coords.z > 9784) 9782 else 9786), 64)
        }
        for ((i, stage) in StalkerNpcs.stages.withIndex()) registry.register(stage, dropTable(i))
    }

    private fun dropTable(stage: Int): NpcDropTable {
        val entries =
            tables[stage]
                .map { row ->
                    NpcWeightedDrop(
                        row.weight,
                        NpcDropRolls.item(items.getValue(row.id), row.low..row.high),
                    )
                }
                .toMutableList()
        val factor = listOf(3, 16, 1)[stage]
        val gems =
            NpcGemDropRolls.standard(items.getValue(1462), NpcGemDropRolls::wearingWealth) {
                it.hero.vars[StalkerQuestVars.legends] >= 75
            }
        entries += NpcWeightedDrop(6 * factor, gems)
        entries += NpcWeightedDrop(factor, NpcGemDropRolls.megaRare(NpcGemDropRolls::wearingWealth))
        val remaining = denominators[stage] * factor - entries.sumOf { it.weight }
        if (remaining > 0) entries += NpcWeightedDrop(remaining, NpcDropRolls.nothing())
        val drops =
            mutableListOf(
                NpcDropRolls.item(items.getValue(532)),
                NpcDropRolls.weighted(entries),
                NpcRareBoneDropRolls.longOrCurved(),
            )
        if (stage > 0)
            drops +=
                NpcDropRolls.independent(
                    denominator = if (stage == 1) 800 else 650,
                    roll = NpcDropRolls.item(items.getValue(31081)),
                )
        return NpcDropTable(NpcDropRolls.sequence(drops))
    }

    private fun ProtectedAccess.enter(dest: CoordGrid, level: Int) {
        if (!player.members || vars[StalkerProgress.quest] < 24) {
            mes("You need to complete Shadows of Custodia before exploring the den.")
            return
        }
        if (stat(stats.agility) < level) {
            mes("You need an Agility level of $level to take this shortcut.")
            return
        }
        telejump(dest)
    }

    private data class Entry(val weight: Int, val id: Int, val low: Int, val high: Int)

    private val denominators = listOf(160, 107, 124)
    private val tables =
        listOf(
            listOf(
                Entry(60, 2, 10, 15), // Cannonball
                Entry(60, 556, 100, 100), // Air rune
                Entry(60, 554, 50, 50), // Fire rune
                Entry(36, 892, 5, 20), // Rune arrow
                Entry(30, 560, 10, 26), // Death rune
                Entry(36, 7937, 15, 15), // Pure essence
                Entry(32, 31086, 1, 1), // Broken antler
                Entry(24, 373, 2, 3), // Swordfish
                Entry(12, 2132, 1, 1), // Raw beef
                Entry(60, 995, 400, 750), // Coins
                Entry(48, 1331, 1, 1), // Adamant scimitar
            ),
            listOf(
                Entry(240, 2, 15, 30), // Cannonball
                Entry(192, 556, 150, 150), // Air rune
                Entry(192, 554, 100, 100), // Fire rune
                Entry(160, 892, 10, 25), // Rune arrow
                Entry(96, 560, 25, 40), // Death rune
                Entry(160, 7937, 20, 20), // Pure essence
                Entry(128, 7946, 2, 3), // Monkfish
                Entry(107, 31086, 1, 1), // Broken antler
                Entry(80, 2360, 5, 10), // Mithril bar
                Entry(64, 2132, 1, 1), // Raw beef
                Entry(16, 30088, 1, 1), // Huasca seed
                Entry(160, 995, 800, 1050), // Coins
            ),
            listOf(
                Entry(15, 2, 20, 30), // Cannonball
                Entry(12, 556, 200, 200), // Air rune
                Entry(12, 554, 150, 150), // Fire rune
                Entry(12, 892, 20, 35), // Rune arrow
                Entry(6, 560, 40, 50), // Death rune
                Entry(12, 385, 2, 3), // Shark
                Entry(12, 7937, 30, 30), // Pure essence
                Entry(6, 2360, 10, 15), // Mithril bar
                Entry(4, 31086, 1, 1), // Broken antler
                Entry(4, 2132, 1, 1), // Raw beef
                Entry(1, 30088, 2, 3), // Huasca seed
                Entry(19, 995, 1180, 3000), // Coins
                Entry(2, 31084, 1, 1), // Alchemist's signet
            ),
        )
}
