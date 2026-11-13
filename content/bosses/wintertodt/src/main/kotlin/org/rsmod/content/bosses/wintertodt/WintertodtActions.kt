package org.rsmod.content.bosses.wintertodt

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.skills.firemaking.FiremakingXp
import org.rsmod.content.skills.woodcutting.WoodcuttingAxeEffects
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class WintertodtActions
@Inject
constructor(
    private val round: WintertodtRound,
    private val types: ObjTypeList,
    private val locTypes: LocTypeList,
    private val enumTypes: EnumTypeList,
    private val xp: XpModifiers,
    private val fireXp: FiremakingXp,
    private val axes: WoodcuttingAxeEffects,
    private val locs: LocRepository,
) {
    private val drank = WeakHashMap<Player, Int>()

    private fun ready(access: ProtectedAccess, active: Boolean = true): Boolean =
        with(access) {
            if (!WintertodtRound.inside(player.coords) || !player.members) return false
            if (player.statBase(stats.firemaking) < 50) {
                mes("You need a Firemaking level of 50 to help subdue the Wintertodt.")
                return false
            }
            round.join(player)
            if (active && !round.active) {
                mes("The Wintertodt is currently subdued.")
                return false
            }
            player.winterWarmth > 0
        }

    public suspend fun chop(access: ProtectedAccess, loc: BoundLocInfo): Unit =
        with(access) {
            if (!ready(this) || loc.id != WinterLocs.roots.id) return
            val mark = round.markAction(player, chopping = true)
            while (ready(this) && isWithinDistance(loc, 1)) {
                val axe = Woodcutting.findAxe(player, types)
                if (axe == null) {
                    mes("You need an axe you can use to chop bruma roots.")
                    return
                }
                if (player.inv.isFull()) {
                    mes("Your inventory is too full to hold any more bruma roots.")
                    resetAnim()
                    return
                }
                faceLoc(loc)
                anim(types[axe].axeWoodcuttingAnim)
                delay(3)
                if (
                    !ready(this) ||
                        !isWithinDistance(loc, 1) ||
                        !round.uninterrupted(player, mark) ||
                        Woodcutting.findAxe(player, types) != axe
                ) {
                    resetAnim()
                    return
                }
                // Existing ordinary-tree axe curve is a bounded model; exact bruma-specific
                // endpoints are unpublished.
                val (low, high) =
                    Woodcutting.cutSuccessRates(locTypes[WinterLocs.regularTree], axe, enumTypes)
                if (!statRandom(stats.woodcutting, low, high, 0)) continue
                if (!invAdd(inv, WinterObjs.root, 1).success) {
                    resetAnim()
                    return
                }
                if (
                    axe.id == objs.crystal_axe.id &&
                        SkillingSuccessRate.successRate(
                            low,
                            high,
                            player.stat(stats.woodcutting),
                            99,
                        ) < 1.0
                )
                    axes.apply(this, axe, WinterObjs.root)
                statAdvance(
                    stats.woodcutting,
                    player.statBase(stats.woodcutting) * 0.3 * xp.get(player, stats.woodcutting),
                )
            }
            resetAnim()
        }

    public suspend fun fletch(access: ProtectedAccess): Unit =
        with(access) {
            if (!ready(this)) return
            if (WintertodtRound.safe(player.coords)) {
                mes("You cannot fletch bruma roots in the safe area.")
                return
            }
            val mark = round.markAction(player, chopping = false)
            var actions = 0
            while (WinterObjs.root in player.inv) {
                val knife =
                    objs.knife in player.inv ||
                        WinterObjs.fletchingKnife in player.inv ||
                        WinterObjs.fletchingKnife in player.worn
                if (!knife) {
                    mes("You need a knife to fletch bruma roots.")
                    return
                }
                val quick =
                    actions > 0 &&
                        (WinterObjs.fletchingKnife in player.inv ||
                            WinterObjs.fletchingKnife in player.worn)
                anim(WinterSeqs.fletch)
                delay(if (quick) 3 else 4)
                if (
                    !ready(this) ||
                        !round.uninterrupted(player, mark) ||
                        WintertodtRound.safe(player.coords)
                ) {
                    resetAnim()
                    return
                }
                if (
                    objs.knife !in player.inv &&
                        WinterObjs.fletchingKnife !in player.inv &&
                        WinterObjs.fletchingKnife !in player.worn
                ) {
                    resetAnim()
                    return
                }
                val result =
                    player.invTransaction(player.inv) {
                        val inv = select(player.inv)
                        delete(inv, WinterObjs.root.id, 1)
                        add(inv, WinterObjs.kindling.id, 1)
                    }
                if (!result.success) {
                    resetAnim()
                    return
                }
                statAdvance(
                    stats.fletching,
                    player.statBase(stats.fletching) * 0.6 * xp.get(player, stats.fletching),
                )
                actions++
            }
            resetAnim()
        }

    public suspend fun brazier(access: ProtectedAccess, loc: BoundLocInfo): Unit =
        with(access) {
            if (!ready(this) || !isWithinDistance(loc, 1)) return
            val station = round.station(loc.coords) ?: return
            val mark = round.markAction(player, chopping = false)
            faceLoc(loc)
            when (station.state) {
                WinterBrazier.Unlit -> {
                    if (station.health == 0) {
                        mes("The pyromancer needs healing before you can light this brazier.")
                        return
                    }
                    if (!hasFireTool(player)) {
                        mes("You need a tinderbox or bruma torch to light the brazier.")
                        return
                    }
                    anim(WinterSeqs.light)
                    delay(4)
                    if (
                        ready(this) &&
                            round.uninterrupted(player, mark) &&
                            isWithinDistance(loc, 1) &&
                            hasFireTool(player)
                    )
                        round.light(station, player)
                }
                WinterBrazier.Broken -> {
                    if (!hasHammer(player)) {
                        mes("You need a hammer to repair the brazier.")
                        return
                    }
                    anim(WinterSeqs.smith)
                    delay(4)
                    if (
                        ready(this) &&
                            round.uninterrupted(player, mark) &&
                            isWithinDistance(loc, 1) &&
                            hasHammer(player)
                    )
                        round.repair(station, player)
                }
                WinterBrazier.Lit -> {
                    while (
                        ready(this) &&
                            station.state == WinterBrazier.Lit &&
                            isWithinDistance(loc, 1)
                    ) {
                        val obj =
                            if (WinterObjs.kindling in player.inv) WinterObjs.kindling
                            else if (WinterObjs.root in player.inv) WinterObjs.root else break
                        anim(WinterSeqs.feed)
                        delay(3)
                        if (
                            !ready(this) ||
                                !round.uninterrupted(player, mark) ||
                                station.state != WinterBrazier.Lit ||
                                !isWithinDistance(loc, 1)
                        ) {
                            resetAnim()
                            return
                        }
                        val result =
                            player.invTransaction(player.inv) {
                                delete(select(player.inv), obj.id, 1)
                            }
                        if (!result.success) {
                            resetAnim()
                            return
                        }
                        round.score(player, if (obj == WinterObjs.kindling) 25 else 10)
                        fireXp.advance(
                            this,
                            player.statBase(stats.firemaking) *
                                (if (obj == WinterObjs.kindling) 3.8 else 3.0),
                        )
                    }
                }
            }
            resetAnim()
        }

    public suspend fun pickHerb(access: ProtectedAccess, loc: BoundLocInfo): Unit =
        with(access) {
            if (!ready(this)) return
            while (ready(this) && isWithinDistance(loc, 1) && !player.inv.isFull()) {
                anim(WinterSeqs.feed)
                delay(2)
                if (
                    !ready(this) ||
                        !isWithinDistance(loc, 1) ||
                        !invAdd(inv, WinterObjs.herb, 1).success
                )
                    return
            }
            resetAnim()
        }

    public suspend fun mix(access: ProtectedAccess, brewma: Boolean = false): Unit =
        with(access) {
            if (!ready(this, active = false)) return
            if (!brewma && vars[varps.druidic_ritual] < 4) {
                mes("Ask Brew'ma to combine these ingredients for you.")
                return
            }
            while (WinterObjs.unf in player.inv && WinterObjs.herb in player.inv) {
                if (!brewma) {
                    anim(WinterSeqs.mix)
                    delay(2)
                }
                if (!ready(this, active = false)) return
                val result =
                    player.invTransaction(player.inv) {
                        val inv = select(player.inv)
                        delete(inv, WinterObjs.unf.id, 1)
                        delete(inv, WinterObjs.herb.id, 1)
                        add(inv, WinterObjs.potions[3].id, 1)
                    }
                if (!result.success) return
                if (!brewma) statAdvance(stats.herblore, player.statBase(stats.herblore) * 0.1)
            }
            resetAnim()
        }

    public fun drink(access: ProtectedAccess, slot: Int, potion: ObjType): Unit =
        with(access) {
            if (
                !ready(this, active = false) ||
                    player.inv[slot]?.id != potion.id ||
                    (drank[player] ?: Int.MIN_VALUE) > player.currentMapClock
            )
                return
            val index = WinterObjs.potions.indexOf(potion)
            if (index < 0) return
            val result =
                player.invTransaction(player.inv) {
                    val inv = select(player.inv)
                    delete(inv, potion.id, 1, slot)
                    if (index > 0) add(inv, WinterObjs.potions[index - 1].id, 1, slot = slot)
                }
            if (!result.success) return
            stopAction()
            anim(WinterSeqs.eat)
            drank[player] = player.currentMapClock + 3
            round.restore(player, 300)
            publish(PlayerConsumableEvent.Consumed(player, potion))
        }

    public fun help(access: ProtectedAccess, npc: Npc): Unit =
        with(access) {
            if (!ready(this) || !isWithinDistance(npc, 1)) return
            val station = round.pyro(npc) ?: return
            if (station.health >= 14) {
                mes("The pyromancer is already in perfect health.")
                return
            }
            val slot =
                player.inv.indices.firstOrNull {
                    player.inv[it]?.id in WinterObjs.potions.map { it.id }
                }
                    ?: run {
                        mes("You need a rejuvenation potion to heal the pyromancer.")
                        return
                    }
            val obj = player.inv.getValue(slot)
            val index = WinterObjs.potions.indexOfFirst { it.id == obj.id }
            val result =
                player.invTransaction(player.inv) {
                    val inv = select(player.inv)
                    delete(inv, obj.id, 1, slot)
                    if (index > 0) add(inv, WinterObjs.potions[index - 1].id, 1, slot = slot)
                }
            if (!result.success) return
            anim(WinterSeqs.feed)
            round.heal(station, player)
        }

    public fun supply(access: ProtectedAccess, obj: ObjType, quantity: Int = 1): Unit =
        with(access) {
            if (!ready(this, active = false)) return
            if (obj != WinterObjs.unf && obj in player.inv) {
                mes("You already have one.")
                return
            }
            val count = minOf(quantity.coerceIn(1, 28), player.inv.freeSpace())
            if (count > 0) invAdd(inv, obj, count)
        }

    private fun hasHammer(player: Player): Boolean =
        listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand).any {
            it in player.inv
        } || listOf(objs.imcando_hammer, objs.imcando_hammer_offhand).any { it in player.worn }

    private fun hasFireTool(player: Player): Boolean =
        objs.tinderbox in player.inv ||
            WinterObjs.torch in player.inv ||
            WinterObjs.torch in player.worn ||
            WinterObjs.offhand in player.inv ||
            WinterObjs.offhand in player.worn
}
