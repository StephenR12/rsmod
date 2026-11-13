package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.loc.BoundLocInfo

public class PrayerAltarRestoration {
    public fun restore(access: ProtectedAccess, altar: BoundLocInfo, bonus: Int = 0) {
        with(access) {
            faceLoc(altar)
            anim(seqs.human_pray)

            // Since 5 April 2023, praying at an altar resets accumulated Prayer drain progress.
            vars[varbits.prayer_drain_counter] = 0

            val target = (statBase(stats.prayer) + bonus).coerceAtMost(MAX_LEVEL)
            val delta = target - stat(stats.prayer)
            when {
                delta > 0 -> statAdd(stats.prayer, constant = delta, percent = 0)
                delta < 0 -> statSub(stats.prayer, constant = -delta, percent = 0)
            }
            if (altar.id == PrayerAltarLocs.natureAltar.id) {
                if (delta > 0) player.completeDiaryTask(DiaryTask.PrayAtNatureAltar)
                if (vars[varbits.piety] != 0) player.completeDiaryTask(DiaryTask.PietyAtNatureAltar)
            }
            if (delta > 0 && altar.level == 0 && altar.x in 2615..2620 && altar.z in 3307..3311) {
                player.completeDiaryTask(DiaryTask.PrayAtEastArdougne)
            }
            if (delta >= 85 && altar.id == PrayerAltarLocs.sophanemHighPriest.id) {
                player.completeDiaryTask(DiaryTask.Restore85InSophanem)
            }
            if (
                altar.id == PrayerAltarLocs.chaos.id &&
                    altar.level == 0 &&
                    altar.x in 2945..2950 &&
                    altar.z in 3818..3822
            ) {
                player.completeDiaryTask(DiaryTask.PrayAtChaosTemple)
            }
            val diary =
                when {
                    altar.level == 2 && altar.x == 1617 && altar.z == 3672 ->
                        DiaryTask.RechargeKourendCastle
                    altar.level == 1 &&
                        altar.x == 3208 &&
                        altar.z == 3494 &&
                        vars[varbits.smite] == 1 -> DiaryTask.SmiteVarrockPalace
                    altar.level == 0 &&
                        altar.x == 3376 &&
                        altar.z == 3285 &&
                        delta > 0 &&
                        vars[varbits.smite] == 1 -> DiaryTask.SmiteEmirsArena
                    altar.level == 0 &&
                        altar.x == 2925 &&
                        altar.z == 3483 &&
                        PrayerDiaryArmour.initiate.all { it in worn } ->
                        DiaryTask.InitiateTaverleyAltar
                    altar.level == 0 &&
                        altar.x == 2995 &&
                        altar.z == 3177 &&
                        delta > 0 &&
                        PrayerDiaryArmour.proselyte.all { it in worn } &&
                        PrayerDiaryArmour.proselyteLegs.any { it in worn } ->
                        DiaryTask.ProselytePortSarim
                    else -> null
                }
            diary?.let(player::completeDiaryTask)
            mes("You recharge your Prayer points.")
        }
    }

    private companion object {
        const val MAX_LEVEL = 255
    }
}

internal object PrayerDiaryArmour : ObjReferences() {
    val initiate = listOf(find("basic_tk_helm"), find("basic_tk_body"), find("basic_tk_legs"))
    val proselyte = listOf(find("basic_tk_rank2_helm"), find("basic_tk_rank2_body"))
    val proselyteLegs = listOf(find("basic_tk_rank2_legs"), find("basic_tk_rank2_plateskirt"))
}
