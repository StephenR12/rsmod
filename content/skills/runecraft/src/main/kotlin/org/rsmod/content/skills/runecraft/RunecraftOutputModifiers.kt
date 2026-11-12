package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.torso
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** Shared pre-output modifiers for ordinary and Kourend rune altars. */
public class RunecraftOutputModifiers @Inject constructor() {
    public fun applicableExtractCount(
        access: ProtectedAccess,
        essenceCount: Int,
        extract: ObjType?,
    ): Int {
        if (extract == null || !access.player.members) {
            return 0
        }
        if (
            access.player.statBase(stats.runecrafting) < EXTRACT_LEVEL ||
                access.vars[RunecraftVarbits.desertTreasureTwo] <= 0
        ) {
            return 0
        }
        return min(essenceCount, access.invTotal(access.player.inv, extract))
    }

    public fun raimentsBonus(access: ProtectedAccess, baseRunes: Int): Int {
        if (!access.player.members) {
            return 0
        }
        val units = access.player.raimentsUnits()
        if (units == 0) {
            return 0
        }
        val guaranteed = (baseRunes / 10) * units
        val remainder = baseRunes % 10
        return guaranteed + if (remainder > 0 && access.random.of(10) < remainder) units else 0
    }

    private fun Player.raimentsUnits(): Int {
        val pieces =
            listOf(
                    RunecraftObjs.raimentsHats.any { it.id == hat?.id },
                    RunecraftObjs.raimentsTorsos.any { it.id == torso?.id },
                    RunecraftObjs.raimentsLegs.any { it.id == legs?.id },
                    feet?.id == RunecraftObjs.raimentsBoots.id,
                )
                .count { it }
        return if (pieces == 4) 6 else pieces
    }

    private companion object {
        const val EXTRACT_LEVEL = 60
    }
}
