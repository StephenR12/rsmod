package org.rsmod.content.skills.agility

import kotlin.math.abs
import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid

internal suspend fun ProtectedAccess.crossObstacle(
    dest: CoordGrid,
    animation: SeqType,
    ticks: Int,
) {
    anim(animation)
    if (coords.level != dest.level || coords.chebyshevDistance(dest) > 20) {
        delay(ticks)
        telejump(dest)
    } else {
        val dx = dest.x - coords.x
        val dz = dest.z - coords.z
        val facing =
            if (abs(dx) > abs(dz)) {
                if (dx > 0) Constants.em_face_east else Constants.em_face_west
            } else if (dz > 0) Constants.em_face_north else Constants.em_face_south
        exactMove(coords, dest, 0, ticks * 30, facing)
        delay(ticks)
    }
    resetAnim()
}
