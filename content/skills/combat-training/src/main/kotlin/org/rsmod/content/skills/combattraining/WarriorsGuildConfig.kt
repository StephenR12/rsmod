package org.rsmod.content.skills.combattraining

import org.rsmod.api.config.refs.params
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal val guildMetals = listOf("bronze", "iron", "steel", "black", "mithril", "adamant", "rune")

internal object GuildEnemies : NpcReferences() {
    val armour = guildMetals.map { find("warguild_${it}_armour") }
    val cyclops =
        (1..6).map { find("warguild_cyclops$it") } +
            (1..6).map { find("warguild_cyclops${it}_high") }
    val shanomi = find("warguild_shanomi")
    val kamfreena = find("warguild_kamfreena")
    val lorelai = find("warguild_lorelai")
    val ajjat = find("warguild_ajjat")
}

internal object GuildEnemyStats : NpcEditor() {
    init {
        val metalLevels = listOf(10, 20, 40, 60, 80, 99, 120)
        for ((i, armour) in GuildEnemies.armour.withIndex()) edit(armour) {
            hitpoints = metalLevels[i]
            attack = metalLevels[i]
            strength = metalLevels[i]
            defence = metalLevels[i]
            magic = 1
            ranged = 1
            respawnRate = 30
            param[params.attackrate] = 4
            param[params.attack_melee] = 4
            param[params.melee_strength] = 5
            param[params.defence_stab] = 50
            param[params.defence_slash] = 25
            param[params.defence_crush] = 19
            param[params.defence_magic] = 400
            param[params.defence_ranged] = 400
        }
        data class CyclopsStats(
            val hp: Int,
            val attack: Int,
            val strength: Int,
            val defence: Int,
            val respawn: Int,
        )
        val low = CyclopsStats(75, 47, 50, 26, 30)
        val high = CyclopsStats(100, 65, 65, 35, 30)
        val basement = CyclopsStats(150, 95, 75, 55, 15)
        for ((i, cyclops) in GuildEnemies.cyclops.withIndex()) edit(cyclops) {
            val stats = if (i >= 6) basement else if (i % 2 == 0) low else high
            hitpoints = stats.hp
            attack = stats.attack
            strength = stats.strength
            defence = stats.defence
            respawnRate = stats.respawn
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
        }
    }
}

internal object GuildTrainingItems : ObjReferences() {
    val sets =
        guildMetals.map { m ->
            listOf("full_helm", "platebody", "platelegs").map { find("${m}_$it") }
        }
    val daggers = (guildMetals + "dragon").map { find("${it}_parryingdagger") }
    val tokens = find("warguild_tokens")
}

internal object GuildScenery : LocReferences() {
    val animator = find("warguild_animator_cent")
    val chamber = listOf(find("warguild_door_armour_l"), find("warguild_door_armour_r"))
    val basement = find("warguild_basement_door")
    val ladders = listOf(find("warguild_ladder_down"), find("warguild_ladder_up"))
    val top = find("spiralstairstop_wg")
    val dummyDoor = find("warguild_door_dummy")
    val dummies =
        listOf("acc", "slash", "agg", "control", "crush", "stab", "defend").map {
            find("warguild_dummy_$it")
        }
    val activeDummies =
        listOf("acc", "slash", "agg", "control", "crush", "stab", "defend").map {
            find("warguild_dummy_${it}_up")
        }
}

internal object GuildTrainingBits : VarBitReferences() {
    val dungeon = find("warguild_basement_access")
    val armour = find("warguild_armour_type")
    val tier = find("warguild_dagger")
    val ca = listOf("easy", "medium").map { find("ca_tier_status_$it") }
}

internal object GuildTrainingVars : VarpReferences() {
    val tokens = find("guild_training_unclaimed")
    val floor = find("guild_cyclops_room")
    val grace = find("guild_cyclops_grace")
}

internal object GuildTrainingSave : VarpBuilder() {
    init {
        for (n in
            listOf("guild_training_unclaimed", "guild_cyclops_room", "guild_cyclops_grace")) build(
            n
        ) {
            permanent = true
            transmitNever = true
        }
    }
}

internal object GuildTrainingTimers : TimerReferences() {
    val room = find("guild_cyclops_token_timer")
    val dummies = find("guild_dummy_interval")
}

internal object GuildTrainingPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<WarriorsGuildTraining>("warriors-guild.toml")
    }
}
