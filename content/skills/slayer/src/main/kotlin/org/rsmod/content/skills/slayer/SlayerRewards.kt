package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.dbrow.UnpackedDbRowType
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

object RewardInterface : InterfaceReferences() {
    val main = find("slayer_rewards")
}

object RewardButtons : ComponentReferences() {
    val confirm = find("slayer_rewards:confirm_button")
    val items = find("slayer_rewards:buy_items")
}

private val taskFields = listOf("master", "target", "remaining", "original", "area", "boss")

internal object StoredTaskVars : VarpReferences() {
    val stored = taskFields.map { find("slayer_stored_$it") }
    val display = (1..6).map { find("if$it") }
    val area = find("slayer_area")
}

internal object StoredTaskBuild : VarpBuilder() {
    init {
        build("slayer_cape_history") {
            transmitNever = true
            permanent = true
        }
        build("slayer_master_contacts") {
            transmitNever = true
            permanent = true
        }
        for (field in taskFields) build("slayer_stored_$field") { permanent = true }
    }
}

class SlayerRewards
@Inject
constructor(rows: DbRowTypeList, enums: EnumTypeList, private val objs: ObjTypeList) :
    PluginScript() {
    private val rewards =
        rows.values
            .filter { it.table == 117 && it.data[6]?.first() != -1 }
            .associateBy { it.data.getValue(0).first() as Int }

    // Cache enums 840-842 drive the items, bundle quantities, and prices in the Buy tab.
    private val items = enums.getValue(840).primitiveMap
    private val quantities = enums.getValue(841).primitiveMap
    private val prices = enums.getValue(842).primitiveMap

    override fun ScriptContext.startup() {
        onIfModalButton(RewardButtons.confirm) {
            if (!player.members || it.op.slot != 1) return@onIfModalButton
            val master =
                Master.entries.firstOrNull { m -> m.id == player.vars[SlayerVarBits.focus] }
                    ?: return@onIfModalButton
            when (it.comsub) {
                in 0..57 -> toggle(it.comsub)
                58 -> cancelOrBlock(master, false)
                59 -> cancelOrBlock(master, true)
                in 60..64 -> unblock(master, it.comsub - 60)
                65 -> unblock(master, 6)
                66 -> extendAll()
                67 -> store()
                68 -> if (player.slayerCount == 0) store()
                69 -> unblock(master, 5)
            }
            refresh(player)
        }
        onIfModalButton(RewardButtons.items) { buy(it.comsub, it.op.slot) }
    }

    internal fun open(access: ProtectedAccess, master: Master) =
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to use Slayer Rewards.")
                return@with
            }
            VarPlayerIntMapSetter.set(player, SlayerVarBits.focus, master.id)
            refresh(player)
            ifOpenMainModal(RewardInterface.main)
            ifSetEvents(RewardButtons.confirm, 0..69, IfEvent.Op1)
            ifSetEvents(
                RewardButtons.items,
                0 until items.size,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
            )
        }

    private fun refresh(player: Player) {
        val stored = StoredTaskVars.stored.map { player.vars[it] }
        val display =
            listOf(
                player.slayerCount,
                player.slayerTarget,
                player.vars[SlayerVarBits.boss],
                stored[2],
                stored[1],
                stored[5],
            )
        StoredTaskVars.display.zip(display).forEach { (varp, value) ->
            VarPlayerIntMapSetter.set(player, varp, value)
        }
    }

    private fun ProtectedAccess.toggle(bit: Int) {
        val row = rewards[bit] ?: return
        val owned = owns(player, bit)
        val name = row.data.getValue(3).first() as String
        if (owned) {
            val toggle = SlayerVarBits.toggleOff[bit]
            if (toggle != null) {
                VarPlayerIntMapSetter.set(player, toggle, 1 - player.vars[toggle])
            } else {
                if (row.data[5]?.first() != 1) return
                setReward(player, bit, false)
            }
        } else {
            if (!pay(row.cost())) return
            setReward(player, bit, true)
        }
        mes("You update your Slayer reward: $name.")
    }

    private fun ProtectedAccess.extendAll() {
        val missing =
            rewards.filter { (bit, row) -> row.data[6]?.first() == 1 && !owns(player, bit) }
        if (!pay(missing.values.sumOf { it.cost() })) return
        missing.keys.forEach { setReward(player, it, true) }
    }

    private fun ProtectedAccess.pay(amount: Int): Boolean {
        if (player.slayerPoints < amount) {
            mes("You need $amount Slayer points for that.")
            return false
        }
        player.slayerPoints -= amount
        return true
    }

    private fun ProtectedAccess.cancelOrBlock(master: Master, block: Boolean) {
        if (player.slayerCount <= 0) return
        if (block) {
            if (
                master.id != player.slayerMaster &&
                    !(master in listOf(Master.Turael, Master.Spria) &&
                        player.slayerMaster in listOf(1, 9))
            ) {
                mes("Use the master who assigned this task to block it.")
                return
            }
            val blocks = SlayerVarBits.blockLists.getValue(master)
            val unlocked = (player.vars[varps.quest_points] / 50).coerceIn(0, 6)
            val diary = DiaryRegion.Lumbridge.tier(DiaryDifficulty.Elite).reward
            val available =
                blocks.take(unlocked) +
                    if (player.vars[diary] != 0) listOf(blocks.last()) else emptyList()
            val slot = available.firstOrNull { player.vars[it] == 0 }
            if (slot == null) {
                mes("You don't have a free block slot.")
                return
            }
            if (blocks.any { player.vars[it] == player.slayerTarget } || !pay(master.blockCost))
                return
            VarPlayerIntMapSetter.set(player, slot, player.slayerTarget)
        } else if (!pay(30)) return
        clearTask(player)
        mes("Your Slayer task has been cancelled.")
    }

    private fun ProtectedAccess.unblock(master: Master, index: Int) {
        VarPlayerIntMapSetter.set(player, SlayerVarBits.blockLists.getValue(master)[index], 0)
    }

    private fun ProtectedAccess.store() {
        if (!owns(player, 51)) return
        val incoming = StoredTaskVars.stored.map { player.vars[it] }
        if (player.slayerCount <= 0 && incoming[2] <= 0) return
        val outgoing =
            if (player.slayerCount > 0)
                listOf(
                    player.slayerMaster,
                    player.slayerTarget,
                    player.slayerCount,
                    player.originalSlayerCount,
                    player.vars[StoredTaskVars.area],
                    player.vars[SlayerVarBits.boss],
                )
            else List(6) { 0 }
        StoredTaskVars.stored.zip(outgoing).forEach { (varp, value) ->
            VarPlayerIntMapSetter.set(player, varp, value)
        }
        player.slayerMaster = incoming[0]
        player.slayerTarget = incoming[1]
        player.slayerCount = incoming[2]
        player.originalSlayerCount = incoming[3]
        VarPlayerIntMapSetter.set(player, StoredTaskVars.area, incoming[4])
        VarPlayerIntMapSetter.set(player, SlayerVarBits.boss, incoming[5])
        mes("Your stored Slayer task has been updated.")
    }

    private fun ProtectedAccess.buy(slot: Int, op: Int) {
        if (!player.members || op !in 2..4) return
        val id = items[slot] as? Int ?: return
        val type = objs.getValue(id)
        val unitCost = prices[id] as? Int ?: return
        val bundle = quantities[id] as? Int ?: 1
        val limit =
            when (op) {
                2 -> 1
                3 -> 5
                else -> 10
            }
        val variants = uniqueVariants[id]
        if (
            variants != null &&
                player.invMap.values.any { inventory ->
                    variants.any { objs.getValue(it) in inventory }
                }
        ) {
            mes("You already have one of those.")
            return
        }
        repeat(if (variants != null) 1 else limit) {
            if (player.slayerPoints < unitCost) {
                mes("You need $unitCost Slayer points for that.")
                return
            }
            if (invAdd(inv, type, bundle).failure) return
            player.slayerPoints -= unitCost
        }
    }

    private companion object {
        val uniqueVariants =
            mapOf(
                11941 to listOf(11941, 22586), // Looting bag, closed or open.
                12791 to
                    listOf(
                        12791,
                        24416,
                        27281,
                        27509,
                    ), // Rune pouch, including its upgraded versions.
                13226 to listOf(13226, 24478), // Herb sack, closed or open.
            )
    }

    private fun owns(player: Player, bit: Int): Boolean =
        player.vars[SlayerVarps.unlocks[bit / 32]] and (1 shl (bit % 32)) != 0

    private fun setReward(player: Player, bit: Int, enable: Boolean) {
        val page = SlayerVarps.unlocks[bit / 32]
        val mask = 1 shl (bit % 32)
        val value = if (enable) player.vars[page] or mask else player.vars[page] and mask.inv()
        VarPlayerIntMapSetter.set(player, page, value)
    }

    private fun UnpackedDbRowType.cost() = data.getValue(1).single() as Int

    private fun clearTask(player: Player) {
        player.slayerCount = 0
        player.originalSlayerCount = 0
        player.slayerTarget = 0
        VarPlayerIntMapSetter.set(player, StoredTaskVars.area, 0)
        VarPlayerIntMapSetter.set(player, SlayerVarBits.boss, 0)
    }
}
