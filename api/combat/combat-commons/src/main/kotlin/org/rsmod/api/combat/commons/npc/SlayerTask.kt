package org.rsmod.api.combat.commons.npc

import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.config.refs.varns
import org.rsmod.api.config.refs.varps
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.map.CoordGrid
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.UnpackedNpcType

/** Cache NPC parameter 50 is the task family id from the slayer_task table. */
public val UnpackedNpcType.slayerFamily: Int get() = paramMap?.primitiveMap?.get(50) as? Int ?: 0

public fun Npc.isAssignedSlayerTarget(player: Player): Boolean =
    visType.isAssignedSlayerTarget(player, if (player.vars[SlayerLocationBits.master] == 7) coords else spawnCoords)

public fun UnpackedNpcType.isAssignedSlayerTarget(player: Player, targetCoords: CoordGrid = player.coords): Boolean {
    val family = slayerFamily
    if (player.vars[varps.slayer_count] <= 0) return false
    val master = player.vars[SlayerLocationBits.master]
    val bossTask = player.vars[varps.slayer_target] == 98
    when (master) {
        7 -> if (!targetCoords.inSlayerWilderness() || !player.coords.inSlayerWilderness()) return false
        8 -> if (!bossTask && !inKonarArea(player.vars[SlayerLocationVars.area], targetCoords)) return false
    }
    if (bossTask) {
        if (master == 8 && id == 7806) return false
        return bossTaskId(id) == player.vars[SlayerLocationBits.boss] && bossTaskId(id) != 0
    }
    return when (val target = player.vars[varps.slayer_target]) {
        31 -> family == 31 || (id in 6613..6614 || id in 12107..12108)
        10, 120 -> target == family || id in 13489..13493
        127 -> family in 58..60 || family == 93 || family == 108 || family == 109
        else -> family > 0 && target == family
    }
}

public fun CoordGrid.inSlayerTowerBasement(): Boolean =
    level == 3 && x in 3392..3455 && z in 9920..9983

public fun CoordGrid.inSlayerTower(): Boolean =
    inSlayerTowerBasement() || (level in 0..2 && x in 3400..3449 && z in 3531..3579)

/** Native boss task ids from DB 116, including alternative kills and damageable phases. */
private fun bossTaskId(npc: Int): Int = when (npc) {
    3162 -> 1
    2205 -> 2
    2215 -> 3
    3129 -> 4
    in 2265..2267 -> 5
    5779 -> 6
    963, 965 -> 7
    239 -> 8
    6609, 11992 -> 9
    6610, 11998 -> 10
    6611, 6612, 11993, 11994 -> 11
    2054 -> 12
    6619 -> 13
    6618, 7806 -> 14
    6615 -> 15
    in 2042..2044 -> 16
    in 1672..1677 -> 17
    494, 496 -> 18
    499 -> 19
    5862, 5863, 5866 -> 20
    in 5886..5891, 5908 -> 21
    in 7851..7855, in 7882..7889 -> 22
    8061 -> 23
    in 8615..8622 -> 24
    8713 -> 25
    in 12077..12080, 12082 -> 26
    12223 -> 27
    12191 -> 28
    12204, 12205 -> 29
    12214 -> 30
    13668 -> 31
    else -> 0
}

public fun Npc.requiresSlayerAssignment(player: Player): Boolean =
    when (bossTaskId(visType.id)) {
        18, 20, 21, 22, 24, 31 -> true
        19 -> player.hasCompletedDiaryTask(DiaryTask.ThermonuclearSmokeDevil)
        else -> false
    } || visType.id in 492..493 ||
    spawnCoords.inSlayerTowerBasement() ||
        when (visType.id) {
            in 465..468 -> spawnCoords.level == 0 && spawnCoords.x in 3016..3087 && spawnCoords.z in 9536..9599 &&
                (spawnCoords.z >= 9558 || spawnCoords.x >= 3071)
            in 7792..7795 -> spawnCoords.x in 3584..3647 && spawnCoords.z in 10240..10303
            8610, 8611 -> spawnCoords.x in 1240..1288 && spawnCoords.z in 10136..10170
            8609 -> spawnCoords.x in 1290..1343 && spawnCoords.z in 10257..10287
            else -> false
        } ||
        (visType.slayerFamily == 45 && spawnCoords.level == 0 &&
            spawnCoords.x in 2688..2720 && spawnCoords.z in 9952..9982)

public var Npc.slayerSuperiorOwner: PlayerUid?
    get() = if (varns.slayer_superior_owner in vars) PlayerUid(vars[varns.slayer_superior_owner]) else null
    set(value) { vars[varns.slayer_superior_owner] = value?.packed }
