package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.ranged.RangedAmmunition
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.back
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AttractionTimers : TimerReferences() {
    val gather = find("ava_metal_attraction")
}

internal object AttractionBits : VarBitReferences() {
    val disabled = find("ds2_ammo_collection")
}

internal object AttractionItems : ObjReferences() {
    val knives = listOf("iron_knife", "steel_knife", "mithril_knife").map { find(it) }
    val helms = listOf("iron_med_helm", "steel_med_helm", "mithril_med_helm").map { find(it) }
    val mouse = find("poh_toy_mouse_unwound")
    val brokenArrow = find("digsitearrow")
}

class AvaMetalAttraction
@Inject
constructor(
    private val items: ObjTypeList,
    private val random: GameRandom,
    private val equip: HeldEquipOp,
    private val ground: ObjRepository,
) : PluginScript() {
    private val lastCollection = mutableMapOf<Player, CoordGrid>()
    private val devices = listOf(AvaItems.attractor, objs.avas_accumulator, objs.avas_assembler)
    private val capes = listOf(objs.ranged_skillcape, objs.ranged_skillcape_t)
    private val wearable = devices + capes + objs.max_cape_worn

    override fun ScriptContext.startup() {
        for (device in devices) {
            onOpHeld4(device) { commune() }
            onOpWorn2(device) { commune() }
        }
        for (cape in capes) {
            onOpHeld3(cape) { commune() }
            onOpWorn3(cape) { commune() }
        }
        onEvent<HeldEquipEvents.WearposChange> { if (wearpos == Wearpos.Back) watch(player) }
        onPlayerLogin { watch(player) }
        onPlayerLogout { lastCollection.remove(player) }
        onPlayerSoftTimer(AttractionTimers.gather) {
            val tier =
                when (RangedAmmunition.recoveryRate(player, items)) {
                    60 -> 0
                    72 -> 1
                    80 -> 2
                    else -> return@onPlayerSoftTimer
                }
            val previous = lastCollection[player] ?: return@onPlayerSoftTimer
            if (
                player.vars[AttractionBits.disabled] != 0 ||
                    (previous.level == player.coords.level &&
                        previous.chebyshevDistance(player.coords) < 3)
            )
                return@onPlayerSoftTimer
            lastCollection[player] = player.coords
            val drop = item(tier, random.of(2000))
            val type = items[drop]
            val slot = Wearpos[type.wearpos1]
            val worn = slot?.let { player.worn[it.slot] }
            if (
                slot != null &&
                    (worn == null ||
                        (worn.id == type.id && type.isStackable && worn.count < Int.MAX_VALUE))
            ) {
                // Use the regular equipment checks and events, even when the backpack is full.
                val found = Inventory(player.inv.type, arrayOf(InvObj(type)))
                if (equip.equip(player, 0, found) is HeldEquipResult.Success)
                    return@onPlayerSoftTimer
            }
            player.invAddOrDrop(ground, drop)
        }
    }

    private fun watch(p: Player) {
        if (wearable.any { it.id == p.back?.id }) {
            if (lastCollection.putIfAbsent(p, p.coords) == null)
                p.softTimer(AttractionTimers.gather, 350)
        } else {
            lastCollection.remove(p)
            p.clearSoftTimer(AttractionTimers.gather)
        }
    }

    public fun communeWithDevice(access:ProtectedAccess):Unit=with(access){commune()}

    private fun ProtectedAccess.commune() {
        if (!player.members) return
        vars[AttractionBits.disabled] = 1 - vars[AttractionBits.disabled]
        mes(
            if (vars[AttractionBits.disabled] != 0)
                "Your device will stop attracting miscellaneous metal items."
            else "Your device will attract miscellaneous metal items again."
        )
    }

    private fun item(tier: Int, roll: Int): ObjType =
        when (roll) {
            in 0..1974 -> listOf(objs.iron_arrow, objs.steel_arrow, objs.mithril_arrow)[tier]
            in 1975..1979 -> listOf(objs.iron_dart, objs.steel_dart, objs.mithril_dart)[tier]
            in 1980..1984 -> AttractionItems.knives[tier]
            in 1985..1989 -> if (tier == 2) objs.mithril_ore else objs.iron_ore
            in 1990..1994 -> listOf(objs.iron_nails, objs.steel_nails, objs.mithril_nails)[tier]
            1995 -> listOf(objs.steel_arrow, AttractionItems.mouse, objs.adamant_arrow)[tier]
            1996 -> AttractionItems.brokenArrow
            1997 ->
                listOf(objs.iron_arrowheads, objs.steel_arrowheads, objs.mithril_arrowheads)[tier]
            1998 -> AttractionItems.helms[tier]
            else -> listOf(objs.iron_bar, objs.steel_bar, objs.mithril_bar)[tier]
        }
}
