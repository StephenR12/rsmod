package org.rsmod.content.interfaces.journal.tab

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.content.skills.magic.spell.utility.standard.AlchemySpells
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RingAlchemyInterface : InterfaceReferences() {
    val side = find("lumbridge_alchemy")
}

internal object RingAlchemyComponents : ComponentReferences() {
    val low = find("lumbridge_alchemy:low")
    val high = find("lumbridge_alchemy:high")
    val items = find("lumbridge_alchemy:items")
    val close = find("lumbridge_alchemy:close")
}

public class ExplorersAlchemy @Inject constructor(private val alchemy: AlchemySpells) :
    PluginScript() {
    private val rings
        get() = DiaryRewardItems.equipment.getValue(DiaryRegion.Lumbridge)

    override fun ScriptContext.startup() {
        onIfModalButton(RingAlchemyComponents.close) { ifClose() }
        onIfModalButton(RingAlchemyComponents.low) { vars[DiaryEquipmentBits.highAlchemy] = 0 }
        onIfModalButton(RingAlchemyComponents.high) {
            if (availableTier() == 3) vars[DiaryEquipmentBits.highAlchemy] = 1
            else mes("You need the elite explorer ring to do that.")
        }
        onIfModalButton(RingAlchemyComponents.items) { cast(it.comsub) }
    }

    internal fun open(access: ProtectedAccess, tier: Int): Unit =
        with(access) {
            if (!canUseDiaryEquipment(DiaryRegion.Lumbridge, tier) || availableTier() < tier) return
            refreshDiaryEquipment()
            if (tier < 3) vars[DiaryEquipmentBits.highAlchemy] = 0
            ifOpenSide(RingAlchemyInterface.side)
            ifSetEvents(RingAlchemyComponents.items, inv.indices, IfEvent.Op1)
            ifSetEvents(RingAlchemyComponents.low, -1..-1, IfEvent.Op1)
            ifSetEvents(RingAlchemyComponents.high, -1..-1, IfEvent.Op1)
        }

    private fun ProtectedAccess.availableTier(): Int = rings.indexOfLast { it in worn || it in inv }

    private fun ProtectedAccess.cast(slot: Int) {
        val tier = availableTier()
        if (tier < 0 || !canUseDiaryEquipment(DiaryRegion.Lumbridge, tier) || slot !in inv.indices)
            return
        val target = inv[slot] ?: return
        if (rings.any { it.id == target.id }) {
            mes("It feels wrong to use the ring's power on this item.")
            return
        }
        refreshDiaryEquipment()
        if (vars[DiaryEquipmentBits.alchs] >= 30) {
            mes("You have no alchemy charges left today.")
            return
        }
        val high = vars[DiaryEquipmentBits.highAlchemy] != 0
        if (high && tier < 3) return
        val spell = if (high) objs.spell_high_alchemy else objs.spell_low_alchemy
        if (alchemy.castWithRing(this, spell, slot)) vars[DiaryEquipmentBits.alchs]++
    }
}
