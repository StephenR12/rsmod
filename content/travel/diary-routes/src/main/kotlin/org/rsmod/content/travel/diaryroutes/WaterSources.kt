package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WaterLocs : LocReferences() {
    val pump = find("fai_falador_waterpump")
    val well = find("rellekka_well")
    val cactus = find("desert_cactus_full")
    val cut = find("desert_cactus_empty")
}

public class WaterSources @Inject constructor(private val locs: LocRepository,private val types: ObjTypeList) : PluginScript() {
    private val skins = listOf(objs.water_skin_0,objs.water_skin_1,objs.water_skin_2,objs.water_skin_3,objs.water_skin_4)
    override fun ScriptContext.startup() {
        for ((water,task) in listOf(WaterLocs.pump to DiaryTask.FillFaladorPump, WaterLocs.well to DiaryTask.FillRellekkaWell)) {
            onOpLocU(water,objs.bucket_empty) {
                if (inv[it.invSlot]?.id != objs.bucket_empty.id || !invReplace(inv,it.invSlot,1,objs.bucket_water).success) return@onOpLocU
                val target = it.loc
                if ((water == WaterLocs.pump && target.x == 2947 && target.z == 3382) ||
                    (water == WaterLocs.well && target.x == 2668 && target.z == 3660)) player.completeDiaryTask(task)
            }
        }
        onOpLoc1(WaterLocs.cactus) { cutCactus(it.loc,null) }
        onOpLocU(WaterLocs.cactus) { cutCactus(it.loc,inv[it.invSlot]) }
    }

    private fun ProtectedAccess.cutCactus(loc: BoundLocInfo, tool:InvObj?) {
        if (!player.members) return
        fun cuts(obj:InvObj?) = obj != null && (obj.id == objs.knife.id || types[obj].param(params.attack_slash) > 0)
        val hasBlade = if (tool != null) cuts(tool) else (objs.knife in inv || cuts(player.righthand))
        if (!hasBlade) { mes("You need a knife or a slash weapon to cut the cactus."); return }
        val wc = stat(stats.woodcutting).coerceIn(1,99)
        val chance = 30 + (252 - 30) * (wc - 1) / 98
        locs.change(loc,WaterLocs.cut,100)
        if (random.of(256) > chance) {
            statAdvance(stats.woodcutting,0.4)
            mes("You fail to get any water from the cactus.")
            return
        }
        val slot = inv.indexOfFirst { item -> skins.dropLast(1).any { it.id == item?.id } }
        if (slot < 0) { mes("You have no empty waterskins to put the water in."); return }
        val previous = skins.indexOfFirst { it.id == inv[slot]?.id }
        if (invReplace(inv,slot,1,skins[previous+1]).success) {
            statAdvance(stats.woodcutting,10.0)
            player.completeDiaryTask(DiaryTask.CutDesertCactus)
            mes("You top up your waterskin.")
        }
    }
}
