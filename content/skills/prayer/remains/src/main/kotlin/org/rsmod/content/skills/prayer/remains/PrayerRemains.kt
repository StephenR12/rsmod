package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.basePrayerLvl

public class PrayerRemains @Inject constructor() {
    public suspend fun offer(access: ProtectedAccess, slot: Int, recipe: PrayerRemainsRecipe) {
        with(access) {
            val cooldown = recipe.cooldown
            if (cooldown in player.weakQueueList) {
                return
            }
            if (recipe.members && !player.members) {
                mes("You need to be on a members' world to use this item.")
                return
            }
            if (player.basePrayerLvl < recipe.prayerLevel) {
                mes("You need a Prayer level of ${recipe.prayerLevel} to use these bones.")
                return
            }
            if (recipe.confirmBurial && player.vars[varbits.giant_bone_bury_warning_disable] == 0) {
                val confirmed =
                    choice2(
                        "Yes, bury it.",
                        true,
                        "No, keep it.",
                        false,
                        title = "Bury this special bone?",
                    )
                if (!confirmed) {
                    return
                }
            }
            if (player.inv[slot]?.id != recipe.obj.id) {
                return
            }
            if (!access.zealotPreservesBone(recipe)) {
                val deleted = invDel(player.inv, recipe.obj, count = 1, slot = slot)
                if (deleted.failure) {
                    return
                }
            }
            val ancestralBonus =
                recipe.kind == PrayerRemainsKind.Bone &&
                    recipe.obj != objs.big_bones &&
                    player.vars[varbits.brut_prayer_total] > 0
            when (recipe.kind) {
                PrayerRemainsKind.Bone -> {
                    anim(seqs.human_pickupfloor)
                    soundSynth(synths.bury_bones)
                    mes("You bury the bones.")
                }
                PrayerRemainsKind.DemonicAsh -> mes("You scatter the ashes.")
            }
            if (ancestralBonus) {
                vars[varbits.brut_prayer_total] = player.vars[varbits.brut_prayer_total] - 1
                mes(
                    "You gain extra prayer experience as the ancestral barbarian spirits " +
                        "watch over you."
                )
            }
            statAdvance(stats.prayer, if (ancestralBonus) recipe.xp * 3.0 else recipe.xp)
            weakQueue(cooldown, ACTION_COOLDOWN)
        }
    }

    private val PrayerRemainsRecipe.cooldown
        get() =
            when (kind) {
                PrayerRemainsKind.Bone -> prayer_remains_queues.boneCooldown
                PrayerRemainsKind.DemonicAsh -> prayer_remains_queues.ashCooldown
            }

    private companion object {
        const val ACTION_COOLDOWN = 2
    }
}
