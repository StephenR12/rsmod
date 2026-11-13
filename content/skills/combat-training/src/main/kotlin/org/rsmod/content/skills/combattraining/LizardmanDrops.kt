package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object LizardmanLoot : ObjReferences() {
    val fang = find("lizardman_fang")
    val talisman = find("xeric_talisman_empty")
}

class LizardmanDrops
@Inject
constructor(
    private val random: GameRandom,
    private val objects: ObjRepository,
    private val players: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcKillEvents.Credited> {
            val id = npc.visType.id
            val shaman = id in 6766..6767 || id in 7744..7745 || id == 8565
            val table =
                when (id) {
                    6914,
                    6915 -> 10 to 87
                    6916,
                    6917,
                    8563 -> 14 to 78
                    6918,
                    6919,
                    8564,
                    10947 -> 14 to 67
                    else -> if (shaman) 40 to 500 else return@onEvent
                }
            val owner = npc.findHero(players) ?: player
            val easy = owner.vars[DiaryRegion.Kourend.tier(DiaryDifficulty.Easy).complete] != 0
            val talismanRate = if (easy) 125 else 250
            val (fangWeight, slots) = table
            // One main-table item per kill. On shamans, the two extra talisman slots replace fang
            // slots.
            val roll = random.of(slots * talismanRate)
            val fangSlots = if (shaman && easy) 38 else fangWeight
            when {
                roll < slots ->
                    objects.add(
                        LizardmanLoot.talisman,
                        npc.coords,
                        constants.lootdrop_duration,
                        owner,
                    )
                roll < slots + fangSlots * talismanRate ->
                    objects.add(
                        LizardmanLoot.fang,
                        npc.coords,
                        constants.lootdrop_duration,
                        owner,
                        count = if (shaman) random.of(10..14) else 1,
                    )
            }
        }
    }
}
