package org.rsmod.content.skills.smithing.smelting.workshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.smithing.smelting.AnvilSeqs
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList

public class WorkshopProduction
@Inject
constructor(
    private val rooms: WorkshopAccess,
    private val types: ObjTypeList,
    private val xp: XpModifiers,
) {
    public suspend fun smelt(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            fun valid(): Boolean =
                player.members &&
                    WorkshopLocs.furnaces.any { it.id == loc.id } &&
                    isWithinDistance(loc, 1) &&
                    player.stat(stats.smithing) >= 20 &&
                    (vars[WorkshopBits.finished] != 0 || vars[WorkshopBits.furnace] != 0) &&
                    WorkshopObjs.ore in inv
            if (!valid()) {
                mes(
                    "You need 20 Smithing and the repaired elemental furnace to smelt elemental ore."
                )
                return false
            }
            val catalyst = WorkshopObjs.catalyst in inv
            val coal = if (catalyst) 2 else 4
            if (inv.count(types[objs.coal]) < coal) {
                mes("You need $coal coal to smelt elemental ore.")
                return false
            }
            faceLoc(loc)
            delay(6)
            if (!valid() || (catalyst && WorkshopObjs.catalyst !in inv)) return false
            val tx =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, WorkshopObjs.ore.id, 1)
                    delete(i, objs.coal.id, coal)
                    if (catalyst) delete(i, WorkshopObjs.catalyst.id, 1)
                    add(i, WorkshopObjs.bar.id, 1)
                }
            if (tx.success)
                statAdvance(
                    stats.smithing,
                    (if (catalyst) 16.0 else 8.0) * xp.get(player, stats.smithing),
                )
            tx.success
        }

    public suspend fun select(access: ProtectedAccess, loc: BoundLocInfo): Unit =
        with(access) {
            val input =
                skillMulti(
                    "What would you like to smith?",
                    WorkshopObjs.outputs,
                    28,
                    28,
                    actionMode = 0,
                )
            val index =
                listOf(
                        components.skillmulti_a,
                        components.skillmulti_b,
                        components.skillmulti_c,
                        components.skillmulti_d,
                    )
                    .indexOfFirst { it.packed == input.component.packed }
            if (index >= 0) smith(this, loc, index, input.subcomponent)
        }

    public suspend fun smith(
        access: ProtectedAccess,
        loc: BoundLocInfo,
        index: Int,
        quantity: Int,
    ): Boolean =
        with(access) {
            if (index !in 0..3 || quantity <= 0) return false
            val mind = index >= 2
            val helm = index % 2 == 1
            val bar = if (mind) WorkshopObjs.mind else WorkshopObjs.bar
            val level = if (mind) 30 else 20
            fun valid(): Boolean {
                if (
                    !player.members ||
                        loc.id != WorkshopLocs.workbench.id ||
                        !isWithinDistance(loc, 1)
                )
                    return false
                if (player.stat(stats.smithing) < level) {
                    mes("You need a Smithing level of $level to make this item.")
                    return false
                }
                if (
                    mind && vars[WorkshopBits.quest] < 5 ||
                        (!mind && helm && vars[WorkshopBits.quest] < 11)
                ) {
                    mes("You haven't learned how to make this item yet.")
                    return false
                }
                if (
                    !mind &&
                        !helm &&
                        vars[WorkshopBits.learned] == 0 &&
                        vars[WorkshopBits.finished] == 0
                )
                    return false
                val book =
                    if (helm) WorkshopObjs.beaten in inv
                    else WorkshopObjs.slashed in inv || (!mind && WorkshopObjs.battered in inv)
                if (!book) {
                    mes("You need the appropriate elemental workshop book in your inventory.")
                    return false
                }
                if (bar !in inv) return false
                val hammer =
                    objs.hammer in inv ||
                        objs.imcando_hammer in inv ||
                        objs.imcando_hammer_offhand in inv ||
                        objs.imcando_hammer in player.worn ||
                        objs.imcando_hammer_offhand in player.worn
                if (!hammer) {
                    mes("You need a hammer to smith this item.")
                    return false
                }
                return true
            }
            var made = false
            repeat(minOf(quantity, 28)) {
                if (!valid()) return made
                faceLoc(loc)
                anim(AnvilSeqs.smith)
                delay(4)
                if (!valid()) {
                    resetAnim()
                    return made
                }
                val tx =
                    player.invTransaction(inv) {
                        val i = select(inv)
                        delete(i, bar.id, 1)
                        add(i, WorkshopObjs.outputs[index].id, 1)
                    }
                if (!tx.success) {
                    resetAnim()
                    return made
                }
                made = true
                statAdvance(stats.smithing, level.toDouble() * xp.get(player, stats.smithing))
                if (index == 3) player.completeDiaryTask(DiaryTask.SmithMindHelmet)
            }
            resetAnim()
            made
        }

    public fun putCart(access: ProtectedAccess, npc: Npc): Boolean =
        with(access) {
            val s = rooms.session(player) ?: return false
            if (
                !player.members ||
                    vars[WorkshopBits.quest] < 5 ||
                    s.cart !== npc ||
                    !isWithinDistance(npc, 1) ||
                    vars[WorkshopBits.position] != 0 ||
                    vars[WorkshopBits.bar] != 0 ||
                    vars[WorkshopBits.crane] in 2..3 ||
                    WorkshopObjs.bar !in inv
            )
                return false
            val tx = player.invTransaction(inv) { delete(select(inv), WorkshopObjs.bar.id, 1) }
            if (tx.success) {
                vars[WorkshopBits.bar] = 1
                rooms.sync(player)
            }
            tx.success
        }

    public fun takeCart(access: ProtectedAccess, npc: Npc): Boolean =
        with(access) {
            val s = rooms.session(player) ?: return false
            if (
                !player.members ||
                    s.cart !== npc ||
                    !isWithinDistance(npc, 1) ||
                    vars[WorkshopBits.position] != 0
            )
                return false
            val bar =
                when (vars[WorkshopBits.bar]) {
                    1 -> WorkshopObjs.bar
                    5 -> WorkshopObjs.primed
                    else -> {
                        mes("The bar cannot be removed yet.")
                        return false
                    }
                }
            val tx = player.invTransaction(inv) { add(select(inv), bar.id, 1) }
            if (tx.success) {
                vars[WorkshopBits.bar] = 0
                rooms.sync(player)
            } else mes("You don't have enough inventory space.")
            tx.success
        }

    public suspend fun operate(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            fun valid(): Boolean =
                player.members &&
                    vars[WorkshopBits.quest] >= 5 &&
                    rooms.source(player, player.coords) != null &&
                    isWithinDistance(loc, 1)
            if (!valid() || WorkshopLocs.controls.none { it.id == loc.id }) return false
            faceLoc(loc)
            delay(
                2
            ) // Exact per-machine cadence/animation offsets are unpublished; this is a playable
            // model.
            if (!valid()) return false
            val position = vars[WorkshopBits.position]
            val bar = vars[WorkshopBits.bar]
            when (loc.id) {
                WorkshopLocs.move.id -> {
                    if (
                        vars[WorkshopBits.clawPosition] % 2 != 0 ||
                            vars[WorkshopBits.crane] in 2..3 ||
                            vars[WorkshopBits.door] != 0 ||
                            vars[WorkshopBits.fan] != 0
                    ) {
                        mes("The machinery must be clear before the cart can move.")
                        return false
                    }
                    vars[WorkshopBits.position] = (position + 1) % 4
                }
                WorkshopLocs.rotate.id -> {
                    val claw = vars[WorkshopBits.clawPosition]
                    if (claw % 2 != 0) {
                        mes("Raise the crane before rotating it.")
                        return false
                    }
                    vars[WorkshopBits.clawPosition] = if (claw == 0) 2 else 0
                }
                WorkshopLocs.lift.id -> {
                    if (vars[WorkshopBits.crane] == 0) {
                        mes("The crane needs to be repaired first.")
                        return false
                    }
                    val claw = vars[WorkshopBits.clawPosition]
                    val holding = vars[WorkshopBits.crane]
                    when (claw) {
                        0 -> vars[WorkshopBits.clawPosition] = 1
                        1 -> {
                            vars[WorkshopBits.clawPosition] = 0
                            if (position == 0) {
                                if (holding == 1 && bar == 1) {
                                    vars[WorkshopBits.crane] = 2
                                    vars[WorkshopBits.bar] = 0
                                } else if (holding in 2..3 && bar == 0) {
                                    vars[WorkshopBits.bar] = if (holding == 3) 2 else 1
                                    vars[WorkshopBits.crane] = 1
                                }
                            }
                        }
                        2 -> {
                            vars[WorkshopBits.clawPosition] = 3
                            if (holding == 2) vars[WorkshopBits.crane] = 3
                        }
                        3 -> vars[WorkshopBits.clawPosition] = 2
                    }
                }
                WorkshopLocs.press.id -> {
                    if (vars[WorkshopBits.pressRepaired] == 0) {
                        mes("The press needs to be repaired first.")
                        return false
                    }
                    if (position != 1 || bar != 2) return false
                    vars[WorkshopBits.bar] = 3
                }
                WorkshopLocs.waterDoor.id -> {
                    if (vars[WorkshopBits.water] != 0) {
                        mes("Drain the water tank before opening it.")
                        return false
                    }
                    vars[WorkshopBits.door] =
                        when (vars[WorkshopBits.door]) {
                            0 -> 1
                            1 -> 0
                            3 -> 4
                            4 -> 3
                            6 -> 7
                            7 -> 6
                            else -> return false
                        }
                }
                WorkshopLocs.screw.id -> {
                    if (position != 2) return false
                    vars[WorkshopBits.door] =
                        when (vars[WorkshopBits.door]) {
                            1 -> if (bar == 3) 5 else 2
                            2 -> 1
                            5 -> 4
                            4 -> 5
                            7 -> 8
                            8 -> 1
                            else -> return false
                        }
                }
                WorkshopLocs.west.id -> {
                    if (vars[WorkshopBits.pipeRepaired] == 0) {
                        mes("The water pipes need to be repaired first.")
                        return false
                    }
                    vars[WorkshopBits.inlet] = 1 - vars[WorkshopBits.inlet]
                    if (vars[WorkshopBits.inlet] == 1 && vars[WorkshopBits.drain] == 0) {
                        if (vars[WorkshopBits.door] !in listOf(0, 3, 6)) {
                            mes("Close the tank door before filling it.")
                            vars[WorkshopBits.inlet] = 0
                            return false
                        }
                        vars[WorkshopBits.water] = 1
                        if (position == 2 && bar == 3 && vars[WorkshopBits.door] == 3) {
                            vars[WorkshopBits.bar] = 4
                            vars[WorkshopBits.door] = 6
                        }
                    }
                }
                WorkshopLocs.east.id -> {
                    vars[WorkshopBits.drain] = 1 - vars[WorkshopBits.drain]
                    if (vars[WorkshopBits.drain] == 1) vars[WorkshopBits.water] = 0
                    else if (vars[WorkshopBits.inlet] == 1) vars[WorkshopBits.water] = 1
                }
                WorkshopLocs.fan.id -> {
                    if (
                        vars[WorkshopBits.smallCog] != 1 ||
                            vars[WorkshopBits.mediumCog] != 2 ||
                            vars[WorkshopBits.largeCog] != 3
                    ) {
                        mes("The wind tunnel needs to be repaired first.")
                        return false
                    }
                    vars[WorkshopBits.fan] = 1 - vars[WorkshopBits.fan]
                    if (vars[WorkshopBits.fan] == 1 && position == 3 && bar == 4)
                        vars[WorkshopBits.bar] = 5
                }
            }
            rooms.sync(player)
            true
        }

    public fun putGun(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            if (
                !player.members ||
                    vars[WorkshopBits.quest] < 5 ||
                    rooms.source(player, player.coords) == null ||
                    !isWithinDistance(loc, 1) ||
                    vars[WorkshopBits.mind] != 0
            )
                return false
            val tx = player.invTransaction(inv) { delete(select(inv), WorkshopObjs.primed.id, 1) }
            if (tx.success) vars[WorkshopBits.mind] = 1
            tx.success
        }

    public suspend fun extract(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            fun valid(): Boolean =
                player.members &&
                    vars[WorkshopBits.quest] >= 5 &&
                    rooms.source(player, player.coords) != null &&
                    isWithinDistance(loc, 1) &&
                    vars[WorkshopBits.mind] == 1 &&
                    player.stat(stats.magic) >= 20
            if (!valid()) {
                mes("The extractor needs a primed bar and 20 current Magic.")
                return false
            }
            faceLoc(loc)
            delay(4)
            if (!valid()) return false
            statSub(stats.magic, 20, 0)
            vars[WorkshopBits.mind] = 2
            true
        }

    public fun takeGun(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            if (
                !player.members ||
                    rooms.source(player, player.coords) == null ||
                    !isWithinDistance(loc, 1)
            )
                return false
            val bar =
                when (vars[WorkshopBits.mind]) {
                    1 -> WorkshopObjs.primed
                    2 -> WorkshopObjs.mind
                    else -> return false
                }
            val tx = player.invTransaction(inv) { add(select(inv), bar.id, 1) }
            if (tx.success) vars[WorkshopBits.mind] = 0
            else mes("You don't have enough inventory space.")
            tx.success
        }
}
