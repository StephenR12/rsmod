package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TrainingNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<CanifisWerewolves>("training-npcs.toml")
    }
}

internal object CanifisEquipment : ObjReferences() {
    val wolfbane = find("dagger_wolfbane")
}

internal object CanifisSeqs : SeqReferences() {
    val transform = find("werewolf_update_transform_werewolf")
}

class CanifisWerewolves
@Inject
constructor(
    private val npcTypes: NpcTypeList,
    private val players: PlayerList,
    private val drops: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (id in 2613..2632) {
            val human = npcTypes.getValue(id)
            val wolf = npcTypes.getValue(id - 20)
            onModifyNpcHit(human) {
                if (
                    !hit.isFromPlayer ||
                        (hit.type == HitType.Melee && hit.isRighthandObj(CanifisEquipment.wolfbane))
                )
                    return@onModifyNpcHit
                npc.transmog(wolf, Int.MAX_VALUE)
                npc.copyStats(wolf)
                npc.anim(CanifisSeqs.transform)
            }
            onNpcHit(human) {
                if (
                    npc.hitpoints != 0 ||
                        !hit.isFromPlayer ||
                        hit.type != HitType.Melee ||
                        !hit.isRighthandObj(CanifisEquipment.wolfbane)
                )
                    return@onNpcHit
                hit.resolvePlayerSource(players)?.completeDiaryTask(DiaryTask.WolfbaneWerewolf)
            }
        }
        onEvent<NpcKillEvents.Credited> {
            if (npc.id in 2613..2632)
                drops.add(objs.wolf_bones, npc.coords, constants.lootdrop_duration, player)
        }
        for (id in listOf(9636, 9805)) {
            onOpNpc1(npcTypes.getValue(id)) {
                if (vars[varps.priest_in_peril] < 60) {
                    mes("Drezel can help you after Priest in Peril.")
                } else if (player.invMap.values.any { CanifisEquipment.wolfbane in it }) {
                    mes("You already have Wolfbane.")
                } else if (invAdd(inv, CanifisEquipment.wolfbane).success) {
                    mes("Drezel gives you a replacement Wolfbane dagger.")
                }
            }
        }
    }
}
