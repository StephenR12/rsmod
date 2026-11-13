package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RugMerchants : NpcReferences() {
    val types = listOf(1, 2, 3, 4, 8).map { find("magic_carpet_seller$it") }
}

internal object CarpetItems : ObjReferences() {
    val charos = find("ring_of_charos_unlocked")
}

internal object CarpetBits : VarBitReferences() {
    val golem = find("golem_a")
    val helper = find("ics_little_var")
    val clothes = find("roguetrader_clothes")
    val blackjacks = find("roguetrader_blackjacks")
}

private enum class CarpetStop(val title: String, val arrival: CoordGrid) {
    Shantay("Shantay Pass", CoordGrid(3311, 3108)),
    Bedabin("Bedabin Camp", CoordGrid(3182, 3042)),
    PollNorth("Pollnivneach (north)", CoordGrid(3350, 3000)),
    Uzer("Uzer", CoordGrid(3467, 3109)),
    PollSouth("Pollnivneach (south)", CoordGrid(3348, 2942)),
    Nardah("Nardah", CoordGrid(3401, 2917)),
    Sophanem("Sophanem", CoordGrid(3287, 2812)),
    Menaphos("Menaphos", CoordGrid(3243, 2812));

    val links: List<CarpetStop>
        get() =
            when (this) {
                Shantay -> listOf(Bedabin, PollNorth, Uzer)
                PollSouth -> listOf(Nardah, Sophanem, Menaphos)
                Bedabin,
                PollNorth,
                Uzer -> listOf(Shantay)
                else -> listOf(PollSouth)
            }
}

public class DesertCarpets : PluginScript() {
    override fun ScriptContext.startup() {
        for (merchant in RugMerchants.types) {
            onOpNpc3(merchant) { choose(it.npc.coords) }
            onOpNpc1(merchant) { choose(it.npc.coords) }
        }
    }

    private suspend fun ProtectedAccess.choose(merchant: CoordGrid) {
        if (!player.members) return
        val from =
            CarpetStop.entries.minBy {
                kotlin.math.abs(it.arrival.x - merchant.x) +
                    kotlin.math.abs(it.arrival.z - merchant.z)
            }
        val choices =
            from.links
                .filter { it != CarpetStop.Uzer || vars[CarpetBits.golem] >= 8 }
                .filter {
                    it != CarpetStop.Sophanem && it != CarpetStop.Menaphos ||
                        vars[CarpetBits.helper] >= 26
                }
        val ring = worn[Wearpos.Ring.slot]?.id == CarpetItems.charos.id
        val discount = vars[CarpetBits.clothes] >= 2 || vars[CarpetBits.blackjacks] >= 2
        val fare =
            when {
                vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).reward] != 0 -> 0
                ring && discount -> 75
                ring || discount -> 100
                else -> 200
            }
        val index =
            menu(
                "Where would you like to fly? ($fare coins)",
                true,
                choices.map { it.title } + "Cancel",
            )
        val destination = choices.getOrNull(index) ?: return
        delay(2)
        if (fare != 0 && !invDel(inv, objs.coins, count = fare).success) {
            mes("You need $fare coins to fly.")
            return
        }
        teleport(destination.arrival)
        publish(PlayerTeleportEvent.Start(player, destination.arrival))
        if (from == CarpetStop.Shantay && destination == CarpetStop.PollNorth)
            player.completeDiaryTask(DiaryTask.FlyPollnivneachCarpet)
        if (destination == CarpetStop.Uzer) player.completeDiaryTask(DiaryTask.FlyUzerCarpet)
    }
}
