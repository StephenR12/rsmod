package org.rsmod.content.skills.crafting.penguinbongos

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseCraftingLvl
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PenguinBongos @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(penguin_bongo_objs.leather, penguin_bongo_objs.mahoganyPlank) { craft() }
        onOpHeld1(penguin_bongo_objs.bongos) { play() }
    }

    /**
     * Makes one set of bongos. Cold War permits this during the instrument step and after the
     * quest; until the exact stage values are production-traced, non-zero progress is the bounded
     * common gate that includes both states without making the item available before starting.
     */
    public fun ProtectedAccess.craft() {
        if (!player.members) {
            mes("You need to be on a members' world to make penguin bongos.")
            return
        }
        if (player.vars[penguin_bongo_varbits.coldWarProgress] == 0) {
            mes("You need to have started the Cold War quest to make penguin bongos.")
            return
        }
        if (player.baseCraftingLvl < CRAFTING_LEVEL) {
            mes("You need an unboosted Crafting level of $CRAFTING_LEVEL to make penguin bongos.")
            return
        }
        if (
            penguin_bongo_objs.leather !in player.inv ||
                penguin_bongo_objs.mahoganyPlank !in player.inv
        ) {
            return
        }

        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, penguin_bongo_objs.leather.id, 1)
                delete(inventory, penguin_bongo_objs.mahoganyPlank.id, 1)
                add(inventory, penguin_bongo_objs.bongos.id, 1)
            }
        check(result.success) { "Penguin-bongo transaction failed after validation." }
        statAdvance(stats.crafting, CRAFTING_XP)
        mes("You use the leather and plank to create penguin bongos.")
    }

    public fun ProtectedAccess.play() {
        if (player.vars[penguin_bongo_varbits.penguinTransmog] == 0) {
            mes("You must be wearing the penguin suit to play these.")
            return
        }
        stopAction()
        anim(penguin_bongo_seqs.play)
    }

    public companion object {
        public const val CRAFTING_LEVEL: Int = 30
        public const val CRAFTING_XP: Double = 25.0
    }
}
