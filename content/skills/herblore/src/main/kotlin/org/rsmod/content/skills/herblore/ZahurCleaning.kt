package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object ZahurNpcs : NpcReferences() {
    val zahur = find("elid_herbalist")
}

/** Zahur oldid14994347 (2025-09-26): 200 coins per herb, noted cleaning needs Desert Hard. */
public class ZahurCleaning @Inject constructor(private val objTypes: ObjTypeList) {
    public fun clean(access: ProtectedAccess): Int =
        with(access) {
            if (!player.members) return 0
            var cleaned = 0
            var sawNoted = false
            for ((slot, obj) in player.inv.toList().withIndex()) {
                if (obj == null) continue
                val type = objTypes[obj]
                val noted = type.certtemplate > 0
                val rawId = if (noted) type.certlink else obj.id
                val recipe = HerbCleaningRecipes.byGrimy[rawId] ?: continue
                if (noted && vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).complete] == 0) {
                    sawNoted = true
                    continue
                }
                val affordable = player.inv.count(objTypes[objs.coins]) / COINS_PER_HERB
                val count = minOf(obj.count, affordable)
                if (count <= 0) break
                val output = if (noted) objTypes[recipe.clean].certlink else recipe.clean.id
                if (output <= 0) continue
                val result =
                    player.invTransaction(player.inv) {
                        val inventory = select(player.inv)
                        delete(inventory, obj.id, count, slot = slot)
                        delete(inventory, objs.coins.id, count * COINS_PER_HERB)
                        add(inventory, output, count)
                    }
                if (!result.success) break
                cleaned += count
            }
            if (cleaned > 0) {
                player.completeDiaryTask(DiaryTask.CleanWithZahur)
                mes("Zahur cleans your herbs.")
            } else if (sawNoted)
                mes("Zahur can only clean noted herbs after you complete the Hard Desert Diary.")
            else mes("You need a grimy herb and 200 coins for Zahur to clean it.")
            cleaned
        }

    public companion object {
        public const val COINS_PER_HERB: Int = 200
    }
}

public class ZahurScript
@Inject
constructor(
    private val cleaning: ZahurCleaning,
    private val unfinished: ZahurUnfinishedPotions,
    private val decanting: PotionDecanting,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3(ZahurNpcs.zahur) { decanting.select(this) }
        onOpNpc4(ZahurNpcs.zahur) { cleaning.clean(this) }
        onOpNpc5(ZahurNpcs.zahur) { unfinished.make(this) }
        onOpNpc1(ZahurNpcs.zahur) {
            when (
                choice4(
                    "Clean my herbs.",
                    0,
                    "Make unfinished potions.",
                    1,
                    "Decant my potions.",
                    2,
                    "No, thank you.",
                    3,
                )
            ) {
                0 -> cleaning.clean(this)
                1 -> unfinished.make(this)
                2 -> decanting.select(this)
            }
        }
    }
}

internal object ZahurSpawn : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ZahurScript>("zahur.toml")
    }
}
