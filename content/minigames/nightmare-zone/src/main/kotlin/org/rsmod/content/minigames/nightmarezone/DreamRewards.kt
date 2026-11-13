package org.rsmod.content.minigames.nightmarezone

import jakarta.inject.Inject
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DreamButtons : ComponentReferences() {
    val resources = find("nzone_rewards:resources")
    val upgrades = find("nzone_rewards:upgrades")
}

internal object DreamRewardBits : VarBitReferences() {
    val ironman = find("ironman")
    val boxes = find("nzone_herbboxes_purchased")
}

internal object DreamDayVar : VarpReferences() {
    val none = find("nzone_box_day")
}

internal object DreamDayConfig : VarpBuilder() {
    init {
        build("nzone_box_day") {
            permanent = true
            transmitNever = true
        }
    }
}

public class DreamRewards
@Inject
constructor(private val enums: EnumTypeList, private val types: ObjTypeList) : PluginScript() {
    private val prices = listOf(75, 200, 200, 5000, 25, 25, 25, 25, 60, 70, 9500, 145, 775)

    override fun ScriptContext.startup() {
        onOpLoc1(DreamLocs.chest) {
            if (!player.members) return@onOpLoc1
            ifOpenMainModal(DreamInterface.shop)
            ifSetEvents(
                DreamButtons.resources,
                0..25,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
            )
            ifSetEvents(DreamButtons.upgrades, 0..55, IfEvent.Op1)
        }
        onIfModalButton(DreamButtons.resources) {
            if (it.comsub !in 0..25 || it.comsub % 2 != 0 || !player.members) return@onIfModalButton
            val index = it.comsub / 2
            val quantity =
                when (it.op.slot) {
                    1 -> 1
                    2 -> 5
                    3 -> 10
                    4 -> countDialog("How many would you like to buy?")
                    else -> 0
                }
            buy(index, quantity)
        }
        onIfModalButton(DreamButtons.upgrades) {
            if (it.op.slot == 1 && it.comsub in 0..55 && it.comsub % 2 == 0 && player.members)
                imbue(it.comsub / 2)
        }
        onEvent<HeldEquipEvents.WearposChange> {
            if (objType.id in listOf(12017, 12018) && player.worn[wearpos.slot]?.id == objType.id)
                player.completeDiaryTask(DiaryTask.ImbueNightmareSalve)
        }
    }

    private fun ProtectedAccess.buy(index: Int, requested: Int) {
        if (requested <= 0 || (index != 12 && vars[DreamRewardBits.ironman] != 0)) return
        var count = minOf(requested, player.dreamPoints / prices[index])
        if (index == 10) {
            val today = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()
            if (vars[DreamDayVar.none] != today) {
                vars[DreamDayVar.none] = today
                vars[DreamRewardBits.boxes] = 0
            }
            count = minOf(count, 15 - vars[DreamRewardBits.boxes])
        }
        if (count <= 0) {
            mes("You cannot buy that many with your remaining allowance and points.")
            return
        }
        val id = enums.getValue(709).primitiveMap[index] as? Int ?: return
        val type = types.getValue(id)
        if (invAdd(inv, type, count).failure) {
            mes("You need more inventory space.")
            return
        }
        player.dreamPoints -= count * prices[index]
        if (index == 10) vars[DreamRewardBits.boxes] += count
    }

    private fun ProtectedAccess.imbue(slot: Int) {
        val old = inv[slot] ?: return
        val output = enums.getValue(815).primitiveMap[old.id] as? Int ?: return
        if (output < 0) return
        val base =
            when (old.id) {
                4081,
                10588 -> 800000
                6731,
                6733,
                6735,
                6737,
                12601,
                12603,
                12605 -> 650000
                19550,
                20655 -> 725000
                21739 -> 500000
                in 8901..8921,
                11864,
                19639,
                19643,
                19647,
                21264,
                21888,
                23073,
                24370,
                25898,
                25904,
                25910,
                29816 -> 1250000
                else -> return
            }
        if (old.id == 12601 && types.getValue(6714) !in inv) {
            mes("Bring a holy wrench to imbue this ring.")
            return
        }
        val cost = if (vars[DreamBits.hardCA] >= 2) base / 2 else base
        if (player.dreamPoints < cost) {
            mes("You need $cost Nightmare Zone points to imbue this item.")
            return
        }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, old.id, 1, slot = slot)
                add(bag, output, 1, vars = old.vars, slot = slot)
            }
        if (result.failure) return
        player.dreamPoints -= cost
        if (output in 12017..12018) player.completeDiaryTask(DiaryTask.ImbueNightmareSalve)
    }
}
