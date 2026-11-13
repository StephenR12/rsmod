package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.content.skills.mining.VarrockMining
import org.rsmod.api.script.*
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeInterval
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class RiftScript
@Inject
internal constructor(
    private val round: RiftRound,
    private val objects: ObjTypeList,
    private val banking: org.rsmod.content.interfaces.bank.scripts.BankInvScript,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onGameStartup { round.setup() }
        onAiConTimer(RiftGame.controller) {
            round.tick()
            controller.aiTimer(1)
        }
        onPlayerLogout { if (round.isParticipant(player)) round.cleanup(player, true) }
        onPlayerLogin {
            if (player.vars[TempleVars.playingGuardiansOfTheRift] != 0) round.cleanup(player, true)
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            if (round.isParticipant(player)) {
                round.cleanup(player, false)
                safeDestination = RiftMapTiles.lobby
            }
        }
        onOpLoc1(RiftLocs.entrance) {
            if (player.members && vars[RiftBits.quest] >= 130) telejump(RiftMapTiles.entry)
            else mes("The portal leads to the Temple of the Eye.")
        }
        onOpLoc1(RiftLocs.exit) { if (coords.x in 3584..3647) telejump(RiftMapTiles.tower) }
        onOpLoc1(RiftLocs.bank) { ifOpenMainSidePair(interfaces.bank_main, interfaces.bank_side) }
        onOpLoc3(RiftLocs.bank) { ifOpenMainModal(interfaces.ge_collection_box) }
        for (barrier in RiftLocs.barrier) {
            onOpLoc1(barrier) { pass(false) }
            onOpLoc2(barrier) {
                mes(
                    "There are ${round.population} players in the temple. The rift is ${round.phase.name.lowercase()}."
                )
            }
            onOpLoc3(barrier) { pass(true) }
            onOpLoc4(barrier) { vars[RiftBits.effectsOff] = 1 - vars[RiftBits.effectsOff] }
        }
        for ((temple, portal) in RiftLocs.portals) {
            val talisman = RiftItems.talismans.getValue(temple)
            onOpLoc1(portal) { round.enterAltar(this, temple, talisman in inv) }
            onOpLocU(portal, talisman) { round.enterAltar(this, temple, true) }
            onOpLoc2(portal) {
                val bit = RiftBits.tallies.getValue(temple)
                vars[bit] = 1 - vars[bit]
                mes(
                    if (vars[bit] != 0) "You will no longer receive this portal talisman."
                    else "You can receive this portal talisman again."
                )
            }
        }
        for ((index, remains) in RiftLocs.remains.withIndex()) {
            onOpLoc1(remains) { mine(it.loc, index, start = true) }
            onOpLoc3(remains) { mine(it.loc, index, start = false) }
        }
        onOpLoc1(RiftLocs.bench) { workbench(it.loc) }
        onOpLoc3(RiftLocs.bench) { workbench(it.loc) }
        onOpLocU(RiftLocs.bench, RiftItems.fragment) { workbench(it.loc) }
        onOpLoc1(RiftLocs.cells) { takeUncharged(10) }
        onOpLoc2(RiftLocs.cells) { takeUncharged(1) }
        onOpLoc1(RiftLocs.weakCells) {
            if (
                round.inTemple(player) &&
                    round.phase != RiftPhase.Open &&
                    RiftItems.cells.drop(1).none { it in inv }
            )
                invAdd(inv, RiftItems.cells[1])
        }
        onOpLoc2(RiftLocs.weakCells) {
            if (round.inTemple(player) && objs.chisel !in inv) invAdd(inv, objs.chisel)
        }
        for ((i, place) in RiftLocs.crafting.withIndex()) onOpLoc1(place) {
            round.assemble(this, i == 0)
        }
        for ((i, tile) in RiftLocs.tiles.withIndex()) {
            onOpLoc1(tile) { round.cell(this, it.loc.coords, i == 0) }
            for (cell in RiftItems.cells) onOpLocU(tile, cell) {
                round.cell(this, it.loc.coords, false)
            }
            if (i == 0) onOpLocU(tile, RiftItems.fragment) { round.cell(this, it.loc.coords, true) }
        }
        onOpNpc1(RiftNpcs.guardian) { round.restorePower(this) }
        for (stone in RiftItems.stones) onOpNpcU(RiftNpcs.guardian, stone) {
            round.restorePower(this)
        }
        for ((i, shortcut) in RiftLocs.shortcuts.withIndex()) onOpLoc1(shortcut) {
            if (round.inTemple(player) && (i == 1 || player.statBase(stats.agility) >= 56)) {
                telejump(if (i == 0) CoordGrid(3637, 9503) else CoordGrid(3633, 9503))
            } else mes("You need 56 Agility to climb over the rubble.")
        }
        onOpLoc1(RiftLocs.portalTop) { round.enterMine(this, it.loc.coords) }
        onOpLoc1(RiftLocs.portalBottom) {
            if (round.inTemple(player)) telejump(RiftMapTiles.center.translate(-4, -1))
        }
        onOpLoc1(RiftLocs.deposit) { depositRunes() }
        onOpLoc2(RiftLocs.deposit) {
            if (round.inTemple(player)) ifOpenMainModal(RiftInterface.deposit)
        }
        for (npc in RiftNpcs.tamara) onOpNpc1(npc) {
            if (choice2("Lock the barrier when the rift opens.", true, "What is happening?", false))
                round.closeToNewEntries(player)
            else
                mes(
                    "Mine fragments, shape them at the workbench and use active altars to power the Great Guardian."
                )
        }
        onOpNpc3(RiftNpcs.tamara.last()) { round.closeToNewEntries(player) }
    }

    private suspend fun ProtectedAccess.pass(quick: Boolean) {
        if (round.inTemple(player)) {
            if (
                !quick &&
                    !choice2(
                        "Leave the game and forfeit this round's contribution.",
                        true,
                        "Stay here.",
                        false,
                    )
            )
                return
            round.cleanup(player, true)
        } else if (round.join(player)) telejump(RiftMapTiles.inside)
    }

    private fun ProtectedAccess.takeUncharged(howMany: Int) {
        if (!round.inTemple(player)) return
        val room = 10 - invTotal(inv, RiftItems.cells[0])
        if (room > 0) invAdd(inv, RiftItems.cells[0], minOf(room, howMany), strict = false)
    }

    private fun ProtectedAccess.mine(rock: BoundLocInfo, index: Int, start: Boolean) {
        if (!round.inTemple(player) || round.phase !in setOf(RiftPhase.Preparation, RiftPhase.Open))
            return
        val pick = Mining.findPickaxe(player, objects)
        if (pick == null) {
            mes("You need a pickaxe you can use.")
            return
        }
        val tool = objects[pick]
        if (start || skillAnimDelay <= mapClock) {
            anim(tool.pickaxeAnim)
            skillAnimDelay = mapClock + 4
        }
        if (start) actionDelay = mapClock + tool.pickaxeInterval.coerceAtLeast(1)
        if (actionDelay <= mapClock) {
            val output = if (index >= 5) RiftItems.essence else RiftItems.fragment
            val range =
                when (index) {
                    4,
                    6 -> 2..4
                    5 -> 4..6
                    else -> 1..3
                }
            var count = random.of(range)
            if (VarrockMining.roll(player,1,random)) count *= 2
            val got = invAdd(inv, output, count, strict = false).completed()
            if (got == 0) {
                resetAnim()
                return
            }
            val eligible =
                if (output == RiftItems.essence) got
                else minOf(got, (250 - vars[RiftBits.mineCount]).coerceAtLeast(0))
            if (eligible > 0) {
                if (output == RiftItems.fragment) vars[RiftBits.mineCount] += eligible
                statAdvance(stats.mining, 5.0 * eligible)
            }
            actionDelay = mapClock + tool.pickaxeInterval.coerceAtLeast(1)
        }
        opLoc3(rock)
    }

    private fun ProtectedAccess.workbench(bench: BoundLocInfo) {
        if (!round.inTemple(player) || RiftItems.fragment !in inv) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, RiftItems.fragment.id, 1)
                add(bag, RiftItems.essence.id, 1)
            }
        if (!result.success) {
            resetAnim()
            return
        }
        anim(RiftAnims.make)
        statAdvance(stats.crafting, (stat(stats.crafting) / 10.0).coerceIn(1.0, 5.0))
        if (RiftItems.fragment in inv) opLoc3(bench)
    }

    private fun ProtectedAccess.depositRunes() {
        if (!round.inTemple(player)) return
        val runes =
            (StandardRuneRecipes.all.map { it.output.id } +
                    CombinationRuneRecipes.all.map { it.output.id })
                .toSet()
        val moved =
            banking.depositMatching(this) {
                it.category == org.rsmod.api.config.refs.categories.rune.id || it.id in runes
            }
        if (moved > 0) mes("You deposit your runes in the bank.")
    }
}
