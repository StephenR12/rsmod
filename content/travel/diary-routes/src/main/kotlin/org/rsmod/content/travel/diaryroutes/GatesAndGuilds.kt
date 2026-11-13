package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GuildGates : LocReferences() {
    val champions = find("championdoor")
    val wizards = listOf(find("magicguild_door_l"),find("magicguild_door_r"))
    val courthouse = listOf(find("kr_courthouse_double_door_l"),find("kr_courthouse_double_door_r"))
    val legends = listOf(find("legendsguildgatel"),find("legendsguildgater"),find("legendsguilddoorl"),find("legendsguilddoorr"))
    val shantay = find("shantay_pass_henge_doorway")
    val unkah = find("tempoross_lobby_pass")
    val trollEntrance = find("troll_stronghold_entrance")
    val trollExit = find("troll_stronghold_exit")
}
internal object GateItems : ObjReferences() {
    val pass = find("shantay_pass")
    val desertSet = listOf("desert_shirt","desert_robe","desert_boots").map { find(it) }
}
internal object GateNpcs : NpcReferences() { val guard = find("tempoross_pass_guard") }
internal object GateVars : VarpReferences() { val trolls = find("troll_quest") }

public class GatesAndGuilds : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(GuildGates.champions) {
            val entering = coords.z >= 3363
            if (entering && vars[varps.quest_points] < 32) {
                mes("You need 32 quest points to enter the Champions' Guild.")
                return@onOpLoc1
            }
            teleport(CoordGrid(3191,if (entering) 3362 else 3363))
            if (entering) player.completeDiaryTask(DiaryTask.EnterChampionsGuild)
        }
        for (door in GuildGates.wizards) onOpLoc1(door) {
            val west = it.loc.x == 2584
            val entering = if (west) coords.x <= 2584 else coords.x >= 2597
            if (entering && (!player.members || stat(stats.magic) < 66)) {
                mes("You need level 66 Magic to enter the Wizards' Guild.")
                return@onOpLoc1
            }
            val inside = if (west) 2585 else 2596
            teleport(CoordGrid(if (entering) inside else it.loc.x,it.loc.z))
            if (entering) player.completeDiaryTask(DiaryTask.EnterWizardsGuild)
        }
        for (door in GuildGates.courthouse) onOpLoc1(door) {
            if (!player.members) return@onOpLoc1
            val entering = coords.z >= it.loc.z
            teleport(it.loc.coords.translate(0,if (entering) -1 else 0))
            if (entering && vars[varbits.piety] != 0) player.completeDiaryTask(DiaryTask.EnterCourtWithPiety)
        }
        for (gate in GuildGates.legends) onOpLoc1(gate) {
            val border = it.loc.z
            val entering = coords.z <= border
            val minimum = if (border == 3349) 1 else 75
            if (entering && (!player.members || vars[varps.legends_quest] < minimum)) {
                mes("The guards won't let you enter the Legends' Guild.")
                return@onOpLoc1
            }
            teleport(it.loc.coords.translate(0,if (entering) 1 else 0))
        }
        onOpNpc4(DiaryShopNpcs.shantay) { sellPass() }
        onOpNpc1(DiaryShopNpcs.shantay) {
            if (choice2("Buy a Shantay pass for 5 coins.",true,"Leave.",false)) sellPass()
        }
        onOpNpc1(GateNpcs.guard) {
            if (choice2("Buy a Shantay pass for 5 coins.",true,"Leave.",false)) sellPass()
        }
        onOpNpc4(GateNpcs.guard) { sellPass() }
        onOpLoc1(GuildGates.shantay) { crossPass(CoordGrid(3303,3117),CoordGrid(3303,3115),coords.z >= 3117) }
        onOpLoc1(GuildGates.unkah) {
            when (it.loc.x) {
                3194 -> crossPass(CoordGrid(3193,2842),CoordGrid(3196,2842),coords.x <= 3193)
                3166 -> crossPass(CoordGrid(3167,2819),CoordGrid(3167,2816),coords.z >= 2819)
            }
        }
        onOpLoc1(GuildGates.trollEntrance) {
            if (!player.members) return@onOpLoc1
            teleport(CoordGrid(2825,10050))
            if (vars[GateVars.trolls] >= 50) player.completeDiaryTask(DiaryTask.EnterTrollStronghold)
        }
        onOpLoc1(GuildGates.trollExit) {
            teleport(CoordGrid(2828,3645))
        }
    }

    private fun ProtectedAccess.sellPass() {
        if (!player.members) { mes("You need to be on a members' world to enter the desert."); return }
        val result = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,objs.coins.id,5)
            add(bag,GateItems.pass.id,1)
        }
        if (result.success) mes("You buy a Shantay pass for 5 coins.")
        else mes("You need 5 coins and room for the pass.")
    }
    private fun ProtectedAccess.crossPass(town:CoordGrid,desert:CoordGrid,entering:Boolean) {
        if (!entering) { teleport(town); return }
        if (!player.members) { mes("You need to be on a members' world to enter the desert."); return }
        if (vars[DiaryRegion.Desert.tier(DiaryDifficulty.Elite).reward] == 0 && !invDel(inv,GateItems.pass).success) {
            mes("You need a Shantay pass to enter the desert.")
            return
        }
        teleport(desert)
        if (GateItems.desertSet.all { it in worn }) player.completeDiaryTask(DiaryTask.EnterDesertInRobes)
    }
}
