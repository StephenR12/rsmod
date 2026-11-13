package org.rsmod.content.other.cluevessels

import jakarta.inject.Inject
import org.rsmod.api.clue.*
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class OpenClueVessels @Inject constructor(
    private val inventory: ClueInventory,
    private val types: ObjTypeList,
    private val easy: Cache233EasyClueSteps,
    private val steps: Cache233ClueSteps,
    private val ground: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (entry in SkillingClue.entries) {
            for (id in listOf(entry.geode, entry.bottle, entry.nest, entry.tier.scrollBox.id)) {
                onOpHeld1(types.getValue(id)) { open(it.slot, id, entry.tier, nest = id == entry.nest) }
            }
        }
        onOpHeld1(ClueTier.Master.scrollBox) { open(it.slot, ClueTier.Master.scrollBox.id, ClueTier.Master) }
    }

    private fun ProtectedAccess.open(slot: Int, id: Int, tier: ClueTier, nest: Boolean = false) {
        val box = id == tier.scrollBox.id
        if (tier.members && !player.members || inv[slot]?.id != id) return
        val convert = !box && inventory.state(player, tier).xMarksComplete
        if (!convert && inventory.ownsScroll(player, tier)) {
            mes("You already have a ${tier.key} clue scroll.")
            return
        }
        val result: ObjType = when {
            convert -> tier.scrollBox
            tier == ClueTier.Beginner -> objs.clue_scroll_beginner
            tier == ClueTier.Master -> types.getValue(19835)
            tier == ClueTier.Easy -> easy.select(random.of(easy.steps.size))
            else -> steps.select(tier, random.of(steps.steps(tier).size))
        }
        val tx = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag, id, 1, slot = slot)
            add(bag, result.id, 1)
        }
        if (tx.success && nest && player.members) player.invAddOrDrop(ground, objs.empty_bird_nest)
        if (!tx.success) mes("You need more inventory space.")
    }
}
