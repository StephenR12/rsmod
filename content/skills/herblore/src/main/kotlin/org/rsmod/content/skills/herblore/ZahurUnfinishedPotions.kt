package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.back
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public object ZahurCapes : ObjReferences() {
    val normal = find("skillcape_herblore")
    val trimmed = find("skillcape_herblore_trimmed")
}

/** Zahur oldid14994347, September2025: all water/blood recipes,200gp each, no production XP. */
public class ZahurUnfinishedPotions @Inject constructor(private val types: ObjTypeList) {
    public fun make(access: ProtectedAccess): Int = with(access) {
        if (!player.members) return 0
        if (vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).complete] == 0 && player.back?.id !in listOf(ZahurCapes.normal.id, ZahurCapes.trimmed.id)) {
            mes("Complete the Hard Desert Diary or wear a Herblore cape to use this service.")
            return 0
        }
        var made = 0
        for (recipe in UnfinishedPotionRecipes.all.filter { it.base == objs.vial_of_water || it.base == objs.vial_of_blood }) {
            val herbs = forms(recipe.herb)
            val vials = forms(recipe.base)
            val herbCount = herbs.sumOf { player.inv.count(types.getValue(it)).toLong() }
            val vialCount = vials.sumOf { player.inv.count(types.getValue(it)).toLong() }
            val count = minOf(herbCount, vialCount, (player.inv.count(types[objs.coins]) / 200).toLong()).toInt()
            if (count == 0) continue
            val output = types[recipe.output].certlink
            if (output <= 0) continue
            val result = player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                var remaining = count
                for (id in herbs) {
                    val take = minOf(remaining, player.inv.count(types.getValue(id)))
                    if (take > 0) delete(inventory, id, take)
                    remaining -= take
                }
                remaining = count
                for (id in vials) {
                    val take = minOf(remaining, player.inv.count(types.getValue(id)))
                    if (take > 0) delete(inventory, id, take)
                    remaining -= take
                }
                delete(inventory, objs.coins.id, count * 200)
                add(inventory, output, count)
            }
            if (!result.success) continue
            made += count
        }
        if (made > 0) mes("Zahur combines your clean herbs and vials into noted unfinished potions.")
        else mes("Bring clean herbs, vials of water or blood, and 200 coins per potion.")
        made
    }

    private fun forms(type: ObjType): List<Int> = listOf(type.id) + listOf(types[type].certlink).filter { it > 0 }
}
