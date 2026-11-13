package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.torso

/** Cache-233 Zealot's robes preserve bones at 1.25% per worn piece, up to 5%. */
internal fun ProtectedAccess.zealotPreservesBone(recipe: PrayerRemainsRecipe): Boolean {
    if (recipe.kind != PrayerRemainsKind.Bone) {
        return false
    }
    return zealotPreservesOffering()
}

public fun ProtectedAccess.zealotPreservesOffering(): Boolean {
    val pieces =
        listOf(
                player.hat?.id == objs.zealots_helm.id,
                player.torso?.id == objs.zealots_robe_top.id,
                player.legs?.id == objs.zealots_robe_bottom.id,
                player.feet?.id == objs.zealots_boots.id,
            )
            .count { it }
    return pieces > 0 && random.of(maxExclusive = ZEALOT_ROLL_DENOMINATOR) < pieces
}

private const val ZEALOT_ROLL_DENOMINATOR = 80
