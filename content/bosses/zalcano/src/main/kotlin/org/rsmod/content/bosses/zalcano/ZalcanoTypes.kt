package org.rsmod.content.bosses.zalcano

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.map.CoordGrid

internal object PrisonNpcs : NpcReferences() {
    val demon = find("zalcano")
    val fallen = find("zalcano_weak")
    val golem = find("zalcano_golem")
    val guide = find("zalcano_instructor")
}

internal object PrisonLocs : LocReferences() {
    val platform = find("zalcano_entrance")
    val exit = find("zalcano_exit")
    val gate = find("zalcano_barrier")
    val glowing = find("zalcano_rock_active")
    val normal = find("zalcano_rock_partial")
    val depleted = find("zalcano_rock_inactive")
    val furnace = find("zalcano_furnace")
    val altar = find("zalcano_altar")
    val red = find("zalcano_symbol_damage")
    val blue = find("zalcano_symbol_empower")
}

internal object PrisonObjs : ObjReferences() {
    val essence = find("blankrune_high")
    val ore = find("zalcano_ore")
    val refined = find("zalcano_refined_ore")
    val imbued = find("zalcano_imbued_ore")
    val shard = find("zalcano_pickaxe_kit")
    val seed = find("prif_tool_seed")
    val pet = find("zalcanopet")
    val ashes = find("infernal_ashes")
    val pick = find("zalcano_pickaxe")
}

internal object PrisonVars : org.rsmod.api.type.refs.varp.VarpReferences() {
    val kills = find("total_zalcano_kills")
}

internal object PrisonBits : VarBitReferences() {
    val xp = find("zalcano_loot_customization")
}

internal object PrisonQueues : QueueReferences() {
    val work = find("zalcano_work")
}

internal object PrisonSeqs : SeqReferences() {
    val throwing = find("human_throw")
    val refine = find("human_furnace")
    val imbue = find("human_runecraft")
    val fall = find("zalcano_fall_down")
    val rise = find("zalcano_stand_up")
    val death = find("zalcano_death")
    val boulder = find("zalcano_attack_rock_fall")
    val rock = find("zalcano_attack_magic")
    val golem = find("zalcano_attack_golem")
    val signs = find("zalcano_attack_ranged")
}

internal object PrisonSpots : SpotanimReferences() {
    val fall = find("zalcano_rock_fall")
    val missile = find("zalcano_ore_throw")
    val shot = find("zalcano_ore_hit")
    val golem = find("zalcano_golem_spawn")
}

internal object PrisonSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<Zalcano>("zalcano-npcs.toml")
    }
}

internal object PrisonConfig : NpcEditor() {
    init {
        for ((type, hp) in
            listOf(
                PrisonNpcs.demon to 300,
                PrisonNpcs.fallen to 1000,
                PrisonNpcs.golem to 14,
            )) edit(type) {
            defaultMode = NpcMode.None
            hitpoints = hp
            timer = 1
        }
    }
}

internal val rocks =
    listOf(
        CoordGrid(3025, 6040),
        CoordGrid(3025, 6057),
        CoordGrid(3040, 6040),
        CoordGrid(3040, 6057),
    )

internal fun inFight(c: CoordGrid) = c.level == 0 && c.x in 3018..3049 && c.z in 6036..6061
