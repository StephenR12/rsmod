package org.rsmod.content.skills.thieving

import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal fun recordPickpocketDiary(player: Player, npc: Npc, definition: PickpocketDefinition) {
    val c = npc.coords
    if (definition === MasterFarmerSeeds.definition) {
        if (npc.id == MasterFarmerNpcs.martin.id) player.completeDiaryTask(DiaryTask.PickpocketMartin)
        if (c.level == 0 && c.x in 2620..2650 && c.z in 3340..3380) player.completeDiaryTask(DiaryTask.PickpocketArdougneMasterFarmer)
    }
    if (definition === StandardPickpockets.hero) player.completeDiaryTask(DiaryTask.PickpocketArdougneHero)
    if (definition === StandardPickpockets.guard && c.level == 0 && c.x in 2940..3015 && c.z in 3300..3390) player.completeDiaryTask(DiaryTask.PickpocketFaladorGuard)
    if (definition === StandardPickpockets.gnome) player.completeDiaryTask(DiaryTask.PickpocketGnome)
    if (definition === StandardPickpockets.lletyaElf) player.completeDiaryTask(DiaryTask.PickpocketElf)
}
