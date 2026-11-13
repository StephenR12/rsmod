package org.rsmod.content.skills.combattraining

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AvaNpcs : NpcReferences() {
    val ava = find("anma_assistant")
    val multi = find("anma_assistant_multi")
}

object AvaQuests : VarBitReferences() {
    val animalMagnetism = find("anma_main")
    val dragonSlayerII = find("ds2")
}

internal object AvaItems : ObjReferences() {
    val attractor = find("anma_30_reward")
    val head = find("vorkath_head")
}

object AvaInterface : InterfaceReferences() {
    val devices = find("anma_reward")
}

object AvaButtons : ComponentReferences() {
    val attractor = find("anma_reward:30")
    val accumulator = find("anma_reward:50")
    val assembler = find("anma_reward:assembler")
}

class AvaDevices : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in listOf(AvaNpcs.ava, AvaNpcs.multi)) {
            onOpNpc4(npc) { devices() }
            onOpNpc1(npc) { devices() }
        }
        for ((button, device) in
            listOf(
                AvaButtons.attractor to AvaItems.attractor,
                AvaButtons.accumulator to objs.avas_accumulator,
                AvaButtons.assembler to objs.avas_assembler,
            )) {
            onIfModalButton(button) {
                if (!player.members || vars[AvaQuests.animalMagnetism] < 240) return@onIfModalButton
                val quantity =
                    when (it.op.slot) {
                        1 -> 1
                        2 -> 5
                        3 -> countDialog()
                        4 -> inv.size
                        else -> return@onIfModalButton
                    }
                repeat(quantity.coerceIn(0, inv.size)) {
                    if (!purchase(device)) return@onIfModalButton
                }
            }
        }
    }

    private fun ProtectedAccess.devices() {
        if (!player.members || vars[AvaQuests.animalMagnetism] < 240) {
            mes("You must complete Animal Magnetism before Ava can supply you with a device.")
            return
        }
        ifOpenMainModal(AvaInterface.devices)
        for (button in listOf(AvaButtons.attractor, AvaButtons.accumulator, AvaButtons.assembler)) {
            ifSetEvents(button, -1..-1, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4)
        }
    }

    private fun ProtectedAccess.purchase(device: ObjType): Boolean {
        val accumulator = device.id == objs.avas_accumulator.id
        val assembler = device.id == objs.avas_assembler.id
        if (accumulator && player.statBase(stats.ranged) < 50) {
            mes("You need a Ranged level of 50 for an accumulator.")
            return false
        }
        if (
            assembler &&
                (player.statBase(stats.ranged) < 70 || vars[AvaQuests.dragonSlayerII] < 215)
        ) {
            mes("You must complete Dragon Slayer II and have 70 Ranged for an assembler.")
            return false
        }
        val previous = if (assembler) objs.avas_accumulator else AvaItems.attractor
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                if ((accumulator || assembler) && previous in inv) delete(bag, previous.id, 1)
                else delete(bag, objs.coins.id, if (assembler) 4999 else 999)
                if (accumulator) delete(bag, objs.steel_arrow.id, 75)
                if (assembler) {
                    delete(bag, AvaItems.head.id, 1)
                    delete(bag, objs.mithril_arrow.id, 75)
                }
                add(bag, device.id, 1)
            }
        if (result.failure) {
            mes("You need the materials and enough inventory space for the device.")
            return false
        }
        if (accumulator) player.completeDiaryTask(DiaryTask.AvaDeviceUpgrade)
        return true
    }
}
