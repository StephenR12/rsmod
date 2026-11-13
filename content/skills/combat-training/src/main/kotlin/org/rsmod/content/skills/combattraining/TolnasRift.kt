package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SoulRiftLocs : LocReferences() {
    val rift =
        listOf(
                "falloff1",
                "falloff2",
                "falloff2_rope",
                "falloff3",
                "falloff4",
                "falloff5",
                "falloff6",
                "falloff7",
                "falloff8",
                "falloff9",
                "falloff_10",
                "falloff_11",
                "falloff_12",
            )
            .map { find("soulbane_$it") }
    val ropes = listOf(find("soulbane_rope_up"), find("soulbane_rope_down"))
}

internal object SoulRiftBits : VarBitReferences() {
    val quest = find("soulbane_prog")
}

public class TolnasRift @Inject constructor(private val ground: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (rift in SoulRiftLocs.rift) onOpLoc1(rift) {
            if (!player.members || vars[SoulRiftBits.quest] < 13) {
                mes("Complete A Soul's Bane before entering the training dungeon.")
                return@onOpLoc1
            }
            telejump(CoordGrid(3105, 5278))
            player.completeDiaryTask(DiaryTask.EnterTolnasRift)
        }
        for (rope in SoulRiftLocs.ropes) onOpLoc1(rope) {
            if (it.loc.coords.x == 3105 && it.loc.coords.z == 5279) telejump(CoordGrid(3310, 3453))
        }
        onEvent<NpcKillEvents.Credited> {
            if (npc.id == 4689 && npc.spawnCoords.mx == 48 && npc.spawnCoords.mz == 82)
                ground.add(objs.bones, npc.coords, constants.lootdrop_duration, player)
        }
    }
}
