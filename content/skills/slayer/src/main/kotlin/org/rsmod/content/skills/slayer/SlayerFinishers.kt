package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.combat.commons.npc.requiresSlayerAssignment
import org.rsmod.api.combat.commons.npc.slayerSuperiorOwner
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private enum class Finisher(
    val monsters: List<Int>,
    val tools: List<Int>,
    val threshold: Int,
    val reward: Int,
    val automaticWeapon: Int? = null,
) {
    Gargoyle(listOf(412, 1543, 7407), listOf(4162, 21754, 21742), 8, 0, 21742),
    Rockslug(listOf(421, 422, 7392, 14423), listOf(4161), 4, 1, 11037),
    DesertLizard((458..463).toList() + listOf(12003, 12004), listOf(6696), 4, 2),
    Zygomite(listOf(537, 1024, 7797), (7421..7430).toList(), 7, 3),
}

class SlayerFinishers
@Inject
constructor(
    private val npcs: NpcTypeList,
    private val objs: ObjTypeList,
    private val players: PlayerList,
    private val modifier: NpcHitModifier,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (finisher in Finisher.entries) {
            for (id in finisher.monsters) {
                val target = npcs.getValue(id)
                onEvent<NpcHitEvents.ResolveDamage>(id) { applyFinisher(finisher) }
                for (toolId in finisher.tools) {
                    val tool = objs.getValue(toolId)
                    onOpNpcU(target, tool) { event ->
                        val npc = event.npc
                        if (!player.members || npc.hitpoints <= 0) return@onOpNpcU
                        if (
                            player.stat(stats.slayer) <
                                npc.visType.param(params.slayer_levelrequire)
                        ) {
                            mes("Your Slayer level is too low to harm this creature.")
                            return@onOpNpcU
                        }
                        val owner = npc.slayerSuperiorOwner
                        if (owner != null && owner != player.uid) return@onOpNpcU
                        if (
                            owner == null &&
                                npc.requiresSlayerAssignment(player) &&
                                !npc.isAssignedSlayerTarget(player)
                        ) {
                            mes("You must be on a Slayer assignment to finish this creature.")
                            return@onOpNpcU
                        }
                        if (inv[event.invSlot]?.id != toolId) return@onOpNpcU
                        if (npc.hitpoints > finisher.threshold) {
                            if (finisher == Finisher.Rockslug || finisher == Finisher.DesertLizard)
                                useTool(player, event.invSlot)
                            mes("The creature is too healthy for that to finish it.")
                            return@onOpNpcU
                        }
                        useTool(player, event.invSlot)
                        npc.queueHit(
                            player,
                            1,
                            HitType.Typeless,
                            npc.hitpoints,
                            modifier,
                            sourceSecondary = tool,
                        )
                    }
                }
            }
        }
        val fungicide = objs.getValue(7432)
        for (id in 7422..7431) {
            onOpHeldU(objs.getValue(id), fungicide) {
                if (
                    inv[it.firstSlot]?.id != id ||
                        invDel(inv, fungicide, slot = it.secondSlot).failure
                )
                    return@onOpHeldU
                inv[it.firstSlot] = InvObj(objs.getValue(7421))
                mes("You refill the fungicide spray.")
            }
        }
    }

    private fun NpcHitEvents.ResolveDamage.applyFinisher(finisher: Finisher) {
        if (npc.hitpoints <= 0 || damage <= 0) return
        val source = if (hit.isFromPlayer) hit.resolvePlayerSource(players) else null
        val manual =
            hit.type == HitType.Typeless &&
                finisher.tools.any { hit.isSecondaryObj(objs.getValue(it)) }
        if (source != null && manual && npc.hitpoints <= finisher.threshold) {
            damage = npc.hitpoints
            return
        }
        if (npc.hitpoints - damage <= finisher.threshold && source != null) {
            val armed =
                finisher.automaticWeapon?.let {
                    hit.type == HitType.Melee && hit.isRighthandObj(objs.getValue(it))
                } == true
            if (armed) {
                // The brine sabre bypasses the salt requirement. It still deals its rolled damage.
                if (finisher != Finisher.Rockslug) damage = npc.hitpoints
                return
            }
            if (source.vars[SlayerVarps.unlocks.first()] and (1 shl finisher.reward) != 0) {
                val slot = source.inv.indexOfFirst { it?.id in finisher.tools }
                if (slot >= 0) {
                    useTool(source, slot)
                    damage = npc.hitpoints
                    return
                }
            }
        }
        damage = damage.coerceAtMost(npc.hitpoints - 1)
    }

    private fun useTool(player: Player, slot: Int) {
        val tool = player.inv.getValue(slot)
        when (tool.id) {
            4161,
            6696,
            21754 ->
                player.inv[slot] = if (tool.count > 1) tool.copy(count = tool.count - 1) else null
            in 7421..7430 -> player.inv[slot] = InvObj(objs.getValue(tool.id + 1))
        }
    }
}
