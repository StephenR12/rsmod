package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HelmetUnlock : VarBitReferences() {
    val unlocked = find("slayer_helm_unlocked")
}

class SlayerHelmets
@Inject
internal constructor(
    private val objs: ObjTypeList,
    private val assignments: SlayerAssignments,
    private val random: GameRandom,
    private val ground: ObjRepository,
) : PluginScript() {
    private val components = listOf(4168, 4164, 4166, 4551, 4155)
    private val goggles = 24942
    // Every imbue source is retained when the helmet is disassembled.
    private val masks = buildMap {
        for (id in 8901..8921 step 2) put(id, 11864)
        for (id in 11774..11784) put(id, 11865)
        for (id in 25266..25276) put(id, 25177)
        for (id in 26771..26781) put(id, 26674)
    }
    private val uncharged = mapOf(11864 to 8921, 11865 to 11784, 25177 to 25276, 26674 to 26781)

    override fun ScriptContext.startup() {
        val parts = components + goggles
        for ((index, first) in parts.withIndex()) {
            for (second in parts.drop(index + 1) + masks.keys) {
                onOpHeldU(objs.getValue(first), objs.getValue(second)) {
                    val mask =
                        if (second in masks) second else inv.firstOrNull { it?.id in masks }?.id
                    if (mask != null) assemble(mask)
                    else mes("You'll need a black mask to assemble a Slayer helmet.")
                }
            }
        }
        for ((helmet, mask) in uncharged) {
            val type = objs.getValue(helmet)
            onOpHeld3(type) { assignments.checkTask(player) }
            onOpWorn4(type) { assignments.checkTask(player) }
            onOpHeld4(type) {
                val result =
                    player.invTransaction(inv) {
                        val inventory = select(inv)
                        delete(inventory, helmet, 1, slot = it.slot)
                        add(inventory, mask, 1)
                        for (component in requiredComponents()) add(inventory, component, 1)
                    }
                if (result.success) mes("You disassemble the Slayer helmet.")
                else mes("You need more inventory space to disassemble your helmet.")
            }
        }
        onEvent<NpcKillEvents.Credited> {
            if (npc.visType.id !in 1047..1051 && npc.visType.id != 7401) return@onEvent
            repeat(if (npc.visType.id == 7401) 3 else 1) {
                if (random.of(512) == 0)
                    ground.add(objs.getValue(8901), npc.coords, constants.lootdrop_duration, player)
            }
        }
    }

    private fun ProtectedAccess.requiredComponents(): List<Int> =
        if (player.vars[SlayerVarBits.porcine] >= 40) components + goggles else components

    private fun ProtectedAccess.assemble(mask: Int) {
        if (!player.members) return
        if (player.vars[HelmetUnlock.unlocked] == 0) {
            mes("Learn the Malevolent masquerade ability from a Slayer master first.")
            return
        }
        if (player.stat(stats.crafting) < 55) {
            mes("You need a Crafting level of 55 to assemble a Slayer helmet.")
            return
        }
        val required = requiredComponents()
        if (required.any { objs.getValue(it) !in inv }) {
            mes("You need a nose peg, facemask, earmuffs, spiny helmet and enchanted gem.")
            if (goggles in required) mes("You also need a pair of reinforced goggles.")
            return
        }
        val result =
            player.invTransaction(inv) {
                val inventory = select(inv)
                delete(inventory, mask, 1)
                for (component in required) delete(inventory, component, 1)
                add(inventory, masks.getValue(mask), 1)
            }
        if (result.success) mes("You assemble a Slayer helmet.")
    }
}
