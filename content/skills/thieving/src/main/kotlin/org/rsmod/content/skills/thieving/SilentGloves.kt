package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.hands
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat

/** Player-wide durability, as documented by Wiki revision 14937449 (14 July 2025). */
public class SilentGloves @Inject constructor() {
    public fun diaryOverrides(access: ProtectedAccess, definition: PickpocketDefinition): Boolean =
        access.player.vars[varbits.ardougne_diary_hard_complete] != 0 ||
            (definition.mediumArdougneDiaryApplies &&
                access.player.vars[varbits.ardougne_diary_medium_complete] != 0)

    public fun failedPickpocket(access: ProtectedAccess, definition: PickpocketDefinition) {
        with(access) {
            if (player.hands?.id != objs.gloves_of_silence.id || diaryOverrides(this, definition)) {
                return
            }
            val damage = player.vars[ThievingVarbits.silentGlovesDamage] + 1
            if (damage >= MAX_FAILURES) {
                player.hands = null
                vars[ThievingVarbits.silentGlovesDamage] = 0
                mes("Your gloves of silence have worn out.")
            } else {
                vars[ThievingVarbits.silentGlovesDamage] = damage
                if (damage == MAX_FAILURES - 1) {
                    mes("Your gloves of silence are going to fall apart!")
                }
            }
        }
    }

    public fun check(access: ProtectedAccess) {
        val damage = access.player.vars[ThievingVarbits.silentGlovesDamage]
        val message =
            when {
                damage == 0 -> "Your gloves are new."
                damage < 29 -> "Your gloves are in good condition."
                damage < 43 -> "Your gloves are starting to look quite shabby."
                damage < 53 -> "Your gloves are starting to need repair."
                damage < 61 -> "Your gloves are in need of repair!"
                else -> "Your gloves are about to fall apart!"
            }
        access.mes(message)
    }

    public fun repair(access: ProtectedAccess) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to repair gloves of silence.")
                return
            }
            if (player.stat(stats.crafting) < REPAIR_LEVEL) {
                mes("You need a Crafting level of $REPAIR_LEVEL to repair gloves of silence.")
                return
            }
            if (objs.gloves_of_silence !in player.inv || objs.dark_kebbit_fur !in player.inv) {
                return
            }
            if (
                objs.knife !in player.inv ||
                    objs.needle !in player.inv ||
                    objs.thread !in player.inv
            ) {
                mes("You need a knife, needle and thread to repair gloves of silence.")
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.dark_kebbit_fur.id, 1)
                    delete(inventory, objs.thread.id, 1)
                }
            if (!result.success) {
                return
            }
            vars[ThievingVarbits.silentGlovesDamage] = 0
            mes("You repair your gloves of silence.")
        }
    }

    public companion object {
        public const val MAX_FAILURES: Int = 62
        public const val REPAIR_LEVEL: Int = 64
    }
}
