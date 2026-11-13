package org.rsmod.content.skills.thieving.pyramid

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Pharaoh's sceptre14963392: period charge costs, capacities and four native teleport destinations.
 */
public class PharaohSceptre @Inject constructor(private val types: ObjTypeList) {
    public fun bind(context: ScriptContext): Unit =
        with(context) {
            onOpNpc1(PyramidNpcs.charmer) {
                mes("Put two coins into my money pot for a snake charm and basket.")
            }
            onOpLocU(PyramidLocs.money, objs.coins) { buyCharm(this) }
            onOpLoc1(PyramidLocs.obelisk) {
                if (
                    player.members &&
                        inv.any { item ->
                            item?.id in
                                listOf(
                                    PyramidObjs.sceptre.id,
                                    PyramidObjs.charged.id,
                                    PyramidObjs.initial.id,
                                )
                        }
                ) {
                    vars[PyramidBits.necropolis] = 1
                    mes("Your sceptre is now attuned to Jaltevas.")
                } else
                    mes("Bring a Pharaoh's sceptre in your inventory to commune with the obelisk.")
            }
            for (tool in listOf(PyramidObjs.charged, PyramidObjs.initial)) {
                onOpHeld2(tool) { select(this, it.inventory, it.slot) }
                onOpHeld3(tool) {
                    teleport(
                        this,
                        it.inventory,
                        it.slot,
                        vars[PyramidBits.lastTeleport].coerceIn(0, 3),
                    )
                }
                onOpHeld4(tool) { check(this, it.inventory[it.slot]) }
                onOpHeld5(tool) {
                    if (
                        choice2(
                            "Uncharge the sceptre and lose its charges.",
                            true,
                            "Keep it charged.",
                            false,
                        )
                    ) {
                        val item = inv[it.slot]
                        if (item?.id == tool.id) inv[it.slot] = InvObj(PyramidObjs.sceptre)
                    }
                }
                onOpWorn2(tool) { teleport(this, player.worn, it.slot, 0) }
                onOpWorn3(tool) { teleport(this, player.worn, it.slot, 1) }
                onOpWorn4(tool) { teleport(this, player.worn, it.slot, 2) }
                onOpWorn5(tool) { teleport(this, player.worn, it.slot, 3) }
                onOpWorn6(tool) { check(this, player.worn[it.slot]) }
            }
            onOpHeld4(PyramidObjs.sceptre) { check(this, inv[it.slot]) }
        }

    /** Snake charm14987125: two coins; existing carried/banked charm prevents another purchase. */
    public fun buyCharm(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || player.invMap.values.any { PyramidObjs.charm in it })
                return false
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, objs.coins.id, 2)
                    add(bag, PyramidObjs.charm.id, 1)
                    add(bag, PyramidObjs.basket.id, 1)
                }
            if (!tx.success) {
                mes("You need two coins and room for a charm and basket.")
                return false
            }
            mes("The snake charmer gives you a snake charm and basket.")
            true
        }

    public fun charges(item: InvObj): Int =
        when (item.id) {
            PyramidObjs.initial.id -> (item.vars and MASK).takeIf { it > 0 } ?: 3
            PyramidObjs.charged.id -> (item.vars and MASK).coerceIn(0, 100)
            else -> 0
        }

    private fun check(access: ProtectedAccess, item: InvObj?): Unit =
        with(access) {
            if (item != null) mes("The sceptre has " + charges(item) + " charges remaining.")
        }

    private suspend fun select(access: ProtectedAccess, inventory: Inventory, slot: Int): Unit =
        with(access) {
            val selection = menu("Teleport to which pyramid?", false, LABELS + "Cancel")
            if (selection in DESTINATIONS.indices) teleport(this, inventory, slot, selection)
        }

    public suspend fun teleport(
        access: ProtectedAccess,
        inventory: Inventory,
        slot: Int,
        destination: Int,
    ): Boolean =
        with(access) {
            if (!player.members || destination !in DESTINATIONS.indices) return false
            val item = inventory[slot] ?: return false
            if (charges(item) <= 0) {
                mes("The sceptre has no charges remaining.")
                return false
            }
            if (destination == 3 && vars[PyramidBits.necropolis] == 0) {
                mes("You must commune with the obelisk in the Necropolis first.")
                return false
            }
            val wilderness =
                if (coords.z >= 9920) ((coords.z - 9920) / 8) + 1 else ((coords.z - 3520) / 8) + 1
            if (
                vars[varbits.inside_wilderness] != 0 && wilderness > 30 ||
                    player.isTeleportBlocked()
            ) {
                mes("A magical force stops you from teleporting.")
                return false
            }
            anim(PyramidSeqs.teleport)
            delay(4)
            if (inventory[slot] != item) return false
            val left = charges(item) - 1
            inventory[slot] =
                if (left == 0) InvObj(PyramidObjs.sceptre)
                else InvObj(PyramidObjs.charged, vars = (item.vars and MASK.inv()) or left)
            vars[PyramidBits.lastTeleport] = destination
            val target = DESTINATIONS[destination]
            publish(PlayerTeleportEvent.Start(player, target))
            teleport(target)
            true
        }

    public suspend fun recharge(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members) return false
            val slot =
                inv.indexOfFirst { item ->
                    item?.id in
                        listOf(
                            PyramidObjs.sceptre.id,
                            PyramidObjs.charged.id,
                            PyramidObjs.initial.id,
                        )
                }
            if (slot < 0) {
                mes("Bring me a Pharaoh's sceptre to recharge.")
                return false
            }
            val tool = inv[slot] ?: return false
            val max = capacity(this)
            if (charges(tool) >= max) {
                mes("The sceptre is already fully charged.")
                return false
            }
            val material =
                choice3(
                    "Pay six gold artefacts.",
                    0,
                    "Pay twelve stone artefacts.",
                    1,
                    "Pay twenty-four pottery or ivory artefacts.",
                    2,
                )
            if (inv[slot] != tool) return false
            val needed = listOf(6, 12, 24)[material]
            val items =
                when (material) {
                    0 ->
                        listOf(PyramidObjs.goldSeal, PyramidObjs.goldScarab, PyramidObjs.goldStatue)
                    1 ->
                        listOf(
                            PyramidObjs.stoneSeal,
                            PyramidObjs.stoneScarab,
                            PyramidObjs.stoneStatue,
                        )
                    else ->
                        listOf(
                            PyramidObjs.comb,
                            PyramidObjs.potteryScarab,
                            PyramidObjs.potteryStatue,
                        )
                }
            val accepted =
                items.flatMap { base ->
                    val note = types[base].certlink
                    listOf(base) + if (note >= 0) listOf(types.getValue(note)) else emptyList()
                }
            var remaining = needed
            val payment =
                buildList<Pair<ObjType, Int>> {
                    for (type in accepted) {
                        val count = minOf(remaining, invTotal(inv, type))
                        if (count > 0) {
                            add(type to count)
                            remaining -= count
                        }
                    }
                }
            if (remaining > 0) {
                mes("You do not have enough artefacts of that material.")
                return false
            }
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    for ((type, count) in payment) delete(bag, type.id, count)
                    delete(bag, tool.id, 1, slot)
                    add(bag, PyramidObjs.charged.id, 1, vars = max, slot = slot)
                }
            if (!tx.success) return false
            mes("Your sceptre has been recharged.")
            true
        }

    public fun capacity(access: ProtectedAccess): Int =
        with(access) {
            when {
                vars[DiaryRegion.Desert.tier(DiaryDifficulty.Elite).reward] != 0 -> 100
                vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).reward] != 0 -> 50
                vars[DiaryRegion.Desert.tier(DiaryDifficulty.Medium).reward] != 0 -> 25
                vars[DiaryRegion.Desert.tier(DiaryDifficulty.Easy).reward] != 0 -> 10
                else -> 3
            }
        }

    public companion object {
        private const val MASK: Int = 0x3fff
        public val LABELS: List<String> =
            listOf("Jalsavrah", "Jaleustrophos", "Jaldraocht", "Jaltevas")
        public val DESTINATIONS: List<CoordGrid> =
            listOf(
                PyramidRooms.lobby,
                CoordGrid(3341, 2827),
                CoordGrid(3232, 2897),
                CoordGrid(3313, 2718),
            )
    }
}
