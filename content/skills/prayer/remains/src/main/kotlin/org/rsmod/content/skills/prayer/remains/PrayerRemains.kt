package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
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
            when (recipe.kind) {
                PrayerRemainsKind.Bone -> {
                    player.restoreBurialPrayer(recipe.obj)
                    anim(seqs.human_pickupfloor)
                    soundSynth(synths.bury_bones)
                    mes("You bury the bones.")
                }
                PrayerRemainsKind.DemonicAsh -> mes("You scatter the ashes.")
            }
            val boosted = player.consumeAncestralBurials(recipe,1)
            statAdvance(stats.prayer, recipe.xp * (1 + 2 * boosted))
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

private var Player.ancestralBurials by intVarBit(varbits.brut_prayer_total)

/** Reserves no charges until the actual burial has committed. */
internal fun Player.consumeAncestralBurials(recipe: PrayerRemainsRecipe, count: Int): Int {
    if (recipe.kind != PrayerRemainsKind.Bone || recipe.obj == objs.big_bones) return 0
    val boosted = minOf(ancestralBurials,count)
    if (boosted > 0) {
        ancestralBurials -= boosted
        mes("You gain extra prayer experience as the ancestral barbarian spirits watch over you.")
    }
    return boosted
}
