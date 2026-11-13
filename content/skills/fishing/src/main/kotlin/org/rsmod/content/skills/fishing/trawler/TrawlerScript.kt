package org.rsmod.content.skills.fishing.trawler

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object TrawlerChopSeqs : SeqReferences() {
    val hacks =
        listOf(
                "bronze",
                "iron",
                "steel",
                "black",
                "mithril",
                "adamant",
                "rune",
                "gilded",
                "dragon",
                "infernal",
                "3a",
                "crystal",
            )
            .associateWith { find("human_trawler_hack_axe_$it") }
}

internal object TrawlerTrees : LocReferences() {
    val normal = find("tree")
}

public class TrawlerScript
@Inject
constructor(
    private val round: TrawlerRound,
    private val rewards: TrawlerRewards,
    private val shops: Shops,
    private val objects: ObjTypeList,
    private val locTypes: LocTypeList,
    private val enums: EnumTypeList,
    private val events: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onGameStartup { round.startup() }
        onAiConTimer(TrawlerControllers.round) {
            round.tick()
            controller.aiTimer(1)
        }
        onPlayerLogin { round.login(player) }
        onPlayerLogout { round.leave(player) }
        onEvent<PlayerDeathEvent.Completed> { round.leave(player) }
        onOpLoc1(TrawlerLocs.board) {
            if (!player.members) return@onOpLoc1
            if (
                !rewards.rewards(player).isEmpty() &&
                    !choice2(
                        "Board and discard your uncollected catch?",
                        true,
                        "Stay ashore.",
                        false,
                    )
            )
                return@onOpLoc1
            rewards.clear(player)
            telejump(CoordGrid(2672, 3170))
            round.board(player)
        }
        onOpLoc1(TrawlerLocs.leave) {
            round.leave(player)
            telejump(CoordGrid(2676, 3170))
        }
        onOpLoc1(TrawlerLocs.up) {
            if (TrawlerRound.atSea(player.coords))
                telejump(CoordGrid(if (it.loc.coords.x < 2016) 2013 else 2020, 4825, 1))
            else if (TrawlerRound.lobby(player.coords))
                telejump(CoordGrid(it.loc.coords.x, it.loc.coords.z))
        }
        onOpLoc1(TrawlerLocs.down) {
            if (TrawlerRound.atSea(player.coords))
                telejump(CoordGrid(if (it.loc.coords.x < 2016) 2013 else 2020, 4825))
            else if (TrawlerRound.lobby(player.coords))
                telejump(CoordGrid(it.loc.coords.x, it.loc.coords.z))
        }
        onOpLoc1(TrawlerLocs.reward) { rewards.open(this) }
        onIfClose(TrawlerInterfaces.rewards) { rewards.close(player) }
        onIfModalButton(TrawlerButtons.items) {
            rewards.collect(
                this,
                bank = it.op.slot == 2,
                discard = it.op.slot == 3,
                slot = it.comsub,
            )
        }
        onIfModalButton(TrawlerButtons.bank) { rewards.collect(this, bank = true) }
        onIfModalButton(TrawlerButtons.discard) { rewards.collect(this, discard = true) }
        for (loc in TrawlerLocs.hulls + TrawlerLocs.leak) {
            onOpLoc1(loc) { patch(this, it.loc) }
            onOpLocU(loc, TrawlerObjs.paste) { patch(this, it.loc) }
        }
        for (loc in TrawlerLocs.nets) {
            onOpLoc1(loc) { mend() }
            onOpLocU(loc, objs.rope) { mend() }
        }
        for (loc in TrawlerLocs.rails) {
            onOpLoc1(loc) { repair() }
            onOpLocU(loc, objs.hammer) { repair() }
        }
        onOpNpc1(TrawlerNpcs.tentacle) { chop(this, it.npc) }
        for ((loc, obj) in
            listOf(
                TrawlerLocs.buckets to TrawlerObjs.bucket,
                TrawlerLocs.hammers to objs.hammer,
                TrawlerLocs.axes to objs.bronze_axe,
            )) onOpLoc1(loc) { supply(obj) }
        onOpLoc2(TrawlerLocs.buckets) { supply(TrawlerObjs.bucket, 5) }
        onOpLoc3(TrawlerLocs.buckets) { supply(TrawlerObjs.bucket, 10) }
        onOpLoc4(TrawlerLocs.buckets) { supply(TrawlerObjs.bucket, countDialog().coerceIn(0, 28)) }
        onOpHeld1(TrawlerObjs.bucket) { bucket(it.slot) }
        onOpHeld1(TrawlerObjs.full) { empty(it.slot) }
        for (barrel in TrawlerLocs.barrels) onOpLoc1(barrel) {
            if (TrawlerRound.wreck(player.coords)) {
                telejump(CoordGrid(2653, 3174))
                statSub(stats.hitpoints, 1.coerceAtMost((player.hitpoints - 1).coerceAtLeast(0)), 0)
            }
        }
        for (npc in listOf(TrawlerNpcs.shore, TrawlerNpcs.lobby, TrawlerNpcs.sea)) onOpNpc1(npc) {
            mes(
                "Keep the ship afloat, repair the net, and earn at least 50 contribution to share the catch. The voyage lasts five minutes."
            )
        }
        onOpNpc3(TrawlerNpcs.trader) { trade() }
        onOpNpc1(TrawlerNpcs.trader) {
            if (choice2("I'd like to buy some supplies.", true, "Leave.", false)) trade()
        }
    }

    private fun ProtectedAccess.trade() {
        if (player.members)
            shops.open(player, "Khazard General Store", TrawlerInvs.shop, 40.0, 140.0, 1.0)
    }

    private fun ProtectedAccess.supply(obj: ObjType, count: Int = 1) {
        if (!round.playing(player)) return
        val n = minOf(count, inv.freeSpace())
        if (n > 0) invAdd(inv, obj, n)
    }

    public suspend fun patch(access: ProtectedAccess, loc: BoundLocInfo): Unit =
        with(access) {
            if (!round.playing(player)) return
            val index =
                when {
                    loc.coords.x in 2013..2020 && loc.coords.z == 4826 -> loc.coords.x - 2013
                    loc.coords.x in 2013..2020 && loc.coords.z == 4823 -> loc.coords.x - 2013 + 8
                    else -> return
                }
            if (!round.leaks[index]) return
            if (TrawlerObjs.paste !in inv) {
                mes("You need swamp paste to fill the leak.")
                return
            }
            gatheringDelay(2)
            if (!round.playing(player) || !round.leaks[index] || TrawlerObjs.paste !in inv) return
            if (invDel(inv, TrawlerObjs.paste, 1).success && round.patch(player, index)) {
                constructionXp()
                mes("You fill the leak with swamp paste.")
            }
        }

    private suspend fun ProtectedAccess.mend() {
        if (!round.playing(player) || !round.torn) return
        if (objs.rope !in inv) {
            mes("You need a rope to mend the net.")
            return
        }
        gatheringDelay(3)
        if (!round.playing(player) || !round.torn || objs.rope !in inv) return
        if (invDel(inv, objs.rope, 1).success && round.mend(player)) {
            statAdvance(stats.crafting, 5.0)
            mes("You repair the fishing net.")
        }
    }

    private suspend fun ProtectedAccess.repair() {
        if (!round.playing(player) || !round.railBroken) return
        if (
            objs.hammer !in inv &&
                objs.imcando_hammer !in inv &&
                objs.imcando_hammer !in worn &&
                objs.imcando_hammer_offhand !in worn
        ) {
            mes("You need a hammer to repair the rail.")
            return
        }
        gatheringDelay(3)
        if (round.repair(player)) {
            constructionXp()
            mes("You repair the broken rail.")
        }
    }

    private fun ProtectedAccess.constructionXp() {
        if (vars[varbits.poh_house_location] != 0) statAdvance(stats.construction, 5.0)
    }

    private suspend fun ProtectedAccess.bucket(slot: Int) {
        if (!round.playing(player) || inv[slot]?.id != TrawlerObjs.bucket.id || round.water == 0)
            return
        gatheringDelay(2)
        if (!round.playing(player) || round.water == 0 || inv[slot]?.id != TrawlerObjs.bucket.id)
            return
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, TrawlerObjs.bucket.id, 1, slot)
                add(bag, TrawlerObjs.full.id, 1, slot)
            }
        if (tx.success) round.bail(player)
    }

    private fun ProtectedAccess.empty(slot: Int) {
        if (!round.playing(player) || inv[slot]?.id != TrawlerObjs.full.id) return
        player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag, TrawlerObjs.full.id, 1, slot)
            add(bag, TrawlerObjs.bucket.id, 1, slot)
        }
    }

    public suspend fun chop(access: ProtectedAccess, npc: Npc): Unit =
        with(access) {
            while (round.playing(player) && round.tentacle === npc) {
                val axe = Woodcutting.findAxe(player, objects)
                if (axe == null || axe.id == objs.blessed_axe.id) {
                    mes("You need an axe you can use to chop the tentacle.")
                    return
                }
                val name = objects[axe].internalName ?: "bronze"
                val kind =
                    when {
                        name.contains("third_age") -> "3a"
                        name.contains("infernal") -> "infernal"
                        name.contains("crystal") -> "crystal"
                        name.contains("dragon") -> "dragon"
                        else ->
                            TrawlerChopSeqs.hacks.keys.firstOrNull { name.contains(it) } ?: "bronze"
                    }
                anim(TrawlerChopSeqs.hacks.getValue(kind))
                gatheringDelay(4)
                if (
                    !round.playing(player) ||
                        round.tentacle !== npc ||
                        Woodcutting.findAxe(player, objects) != axe
                )
                    return
                val (low, high) =
                    Woodcutting.cutSuccessRates(locTypes[TrawlerTrees.normal], axe, enums)
                if (statRandom(stats.woodcutting, low, high, 0) && round.chop(player))
                    statAdvance(stats.woodcutting, 5.0)
            }
            resetAnim()
        }
}
