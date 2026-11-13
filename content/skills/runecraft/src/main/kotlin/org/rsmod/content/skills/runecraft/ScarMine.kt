package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.back
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.mining.VarrockMining
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.mining.CrystalPickaxeLifecycle
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeFastIntervalChance
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeInterval
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ScarLocs : LocReferences() {
    val entryStones = find("dt2_scar_entry_stepping_stone")
    val mineStone = find("scar_ventriculus_stepping_stone_with_op")
    val passage = find("scar_ventriculus_entry_with_op")
    val exitMine = find("scar_exit")
    val rift = find("dt2_scar_rift")
    val rocks = listOf("", "_2x2", "_3x3").map { find("scar_amalgamation$it") }
    val empty = listOf("", "_2x2", "_3x3").map { find("scar_amalgamation${it}_empty") }
    val opening = find("scar_ventriculus_input")
    val essence = find("scar_ventriculus_output_essence")
    val extracts = find("scar_ventriculus_output_extracts")
    val coffer = find("scar_coffer")
}

internal object ScarNpcs : NpcReferences() {
    val guardian = listOf(find("dt2_catalytic_guardian_1op"), find("dt2_catalytic_guardian_2ops"))
    val hagus = find("hagus")
}

internal object ScarBits : VarBitReferences() {
    val quest = find("dt2_scar")
    val essence = find("scar_essencemine_chunks_deposited")
}

internal object ScarVars : VarpReferences() {
    val coins = find("scar_essencemine_coffer")
    val wrath = find("hagus_wrath_runes_given")
}

internal object ScarSaves : VarpBuilder() {
    init {
        build("hagus_wrath_runes_given") { transmitNever = true }
    }
}

internal object ScarItems : ObjReferences() {
    val chunks = find("scar_tainted_essence")
}

internal object ScarAnims : SeqReferences() {
    val jump = find("human_jump_stones")
}

private enum class Extract(val item: ObjType, val price: Int) {
    Warped(RunecraftObjs.warpedExtract, 1250),
    Twisted(RunecraftObjs.twistedExtract, 6000),
    Mangled(RunecraftObjs.mangledExtract, 12000),
    Scarred(RunecraftObjs.scarredExtract, 24000),
}

public class ScarMine
@Inject
constructor(
    private val locs: LocRepository,
    private val types: ObjTypeList,
    private val crystal: CrystalPickaxeLifecycle,
    private val celestial: org.rsmod.content.skills.mining.CelestialMiningRing,
    private val xp: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in ScarNpcs.guardian) onOpNpc1(npc) {
            if (choice2("Enter the Scar.", true, "Stay here.", false)) travel()
        }
        onOpNpc3(ScarNpcs.guardian.last()) { travel() }
        onOpLoc1(ScarLocs.rift) { telejump(CoordGrid(3611, 9471)) }
        // The two stepping stones form a single crossing. Both banks are separated from
        // the clicked floor decoration by unwalkable water.
        onApLoc1(ScarLocs.entryStones) { if (isWithinApRange(it.loc, 2)) cross(false) }
        onOpLoc1(ScarLocs.entryStones) { cross(false) }
        onApLoc1(ScarLocs.mineStone) { if (isWithinApRange(it.loc, 2)) cross(true) }
        onOpLoc1(ScarLocs.mineStone) { cross(true) }
        onOpLoc1(ScarLocs.passage) { if (permitted()) telejump(CoordGrid(1952, 6349)) }
        onOpLoc1(ScarLocs.exitMine) { telejump(CoordGrid(2041, 6424)) }
        for ((size, rock) in ScarLocs.rocks.withIndex()) {
            onOpLoc1(rock) { mine(it.loc, size, true) }
            onOpLoc3(rock) { mine(it.loc, size, false) }
        }
        onOpLoc1(ScarLocs.opening) { deposit() }
        onOpLocU(ScarLocs.opening, ScarItems.chunks) { deposit() }
        onOpLoc2(ScarLocs.opening) {
            mes("The Ventriculus holds ${vars[ScarBits.essence]} tainted essence.")
        }
        onOpLoc1(ScarLocs.essence) { withdraw(null) }
        onOpLoc1(ScarLocs.extracts) {
            if (permitted()) {
                val choice =
                    menu(
                        "Choose a runic extract",
                        true,
                        Extract.entries.map { "${it.name} extract (${it.price} coins each)" },
                    )
                Extract.entries.getOrNull(choice)?.let { withdraw(it) }
            }
        }
        onOpLoc2(ScarLocs.extracts) {
            mes(
                "Hagus lists ${Extract.entries.joinToString { "${it.name.lowercase()} at ${it.price} coins" }}."
            )
        }
        onOpLoc1(ScarLocs.coffer) {
            if (permitted())
                when (
                    menu(
                        "Coffer (${vars[ScarVars.coins]} coins)",
                        "Deposit coins",
                        "Withdraw coins",
                        "Leave",
                    )
                ) {
                    0 -> transferCoins(true)
                    1 -> transferCoins(false)
                }
        }
        onOpLocU(ScarLocs.coffer, objs.coins) { transferCoins(true) }
        onOpNpc1(ScarNpcs.hagus) {
            if (!permitted()) return@onOpNpc1
            val items = Extract.entries.filter { it.item in inv }
            val options =
                listOf("How does the Ventriculus work?") +
                    items.map { "Refund ${it.name.lowercase()} extracts" } +
                    "Leave"
            val choice = menu("Hagus", true, options)
            if (choice == 0)
                mes(
                    "Deposit chunks into the openings. The essence orifice produces pure essence. Extracts cost coins from my coffer."
                )
            else items.getOrNull(choice - 1)?.let { refund(it) }
        }
        for (extract in Extract.entries) onOpNpcU(ScarNpcs.hagus, extract.item) { refund(extract) }
        onOpNpcU(ScarNpcs.hagus, objs.wrath_rune) {
            if (!permitted()) return@onOpNpcU
            if (vars[ScarVars.wrath] >= 1000) mes("Hagus cannot hold any more wrath runes.")
            else if (
                choice2("Give Hagus a wrath rune.", true, "Keep it.", false) &&
                    invDel(inv, objs.wrath_rune).success
            ) {
                vars[ScarVars.wrath] += 1
                mes("Hagus adds the rune to his collection.")
            }
        }
    }

    private fun ProtectedAccess.permitted(): Boolean {
        // The passage and the third stone first transform at native DT2_SCAR stage 38.
        // TODO(quests): Desert Treasure II progresses these states.
        if (player.members && vars[ScarBits.quest] >= 38) return true
        mes("You need to progress further into the Scar during Desert Treasure II.")
        return false
    }

    private fun ProtectedAccess.travel() {
        if (player.members && vars[ScarBits.quest] >= 4) telejump(CoordGrid(2013, 6432))
        else mes("The guardian has no reason to send you into the Scar yet.")
    }

    private suspend fun ProtectedAccess.cross(mine: Boolean) {
        if (!player.members || (mine && !permitted())) return
        val destination =
            if (mine) CoordGrid(2041, if (coords.z >= 6427) 6425 else 6429)
            else CoordGrid(if (coords.x <= 2031) 2035 else 2029, 6430)
        anim(ScarAnims.jump)
        delay(1)
        telejump(destination)
    }

    private fun ProtectedAccess.mine(rock: BoundLocInfo, size: Int, start: Boolean) {
        if (!permitted() || locs.findExact(rock.coords, ScarLocs.rocks[size]) == null) return
        val level = stat(stats.mining)
        if (level < 64) {
            mes("You need level 64 Mining to mine an amalgamation.")
            return
        }
        val pick = Mining.findPickaxe(player, types)
        if (pick == null) {
            mes("You need a pickaxe you can use.")
            return
        }
        if (inv.isFull()) {
            mes("Your inventory is full.")
            resetAnim()
            return
        }
        val tool = types[pick]
        if (start || skillAnimDelay <= mapClock) {
            anim(tool.pickaxeAnim)
            skillAnimDelay = mapClock + 4
        }
        if (start || actionDelay <= mapClock) {
            if (!start) {
                // Yield rises with visible Mining up to 99. The original distribution and
                // depletion chance are unknown; size determines the observed respawn time.
                val bonusChance = (level.coerceAtMost(99) - 64)
                var chunks = 1 + List(2) { random.of(50) < bonusChance }.count { it }
                if (VarrockMining.roll(player,3,random)) chunks++
                if (
                    player.back?.id in
                        listOf(
                            objs.mining_skillcape.id,
                            objs.mining_skillcape_t.id,
                            objs.max_cape_worn.id,
                        ) && random.of(20) == 0
                )
                    chunks++
                val produced = invAdd(inv, ScarItems.chunks, chunks, strict = false).completed()
                if (produced <= 0) {
                    resetAnim()
                    return
                }
                crystal.consumeCharge(this, pick)
                val extra =
                    if (celestial.rollExtra(this) && invAdd(inv, ScarItems.chunks).success) 1 else 0
                statAdvance(stats.mining, 6.0 * (produced + extra) * xp.get(player, stats.mining))
                if (random.of(4) == 0) {
                    locs.change(rock, ScarLocs.empty[size], listOf(25, 59, 70)[size])
                    resetAnim()
                    return
                }
            }
            val fast = tool.pickaxeFastIntervalChance
            actionDelay =
                mapClock + tool.pickaxeInterval -
                    if (fast > 0 && random.randomBoolean(fast)) 1 else 0
        }
        opLoc3(rock)
    }

    private fun ProtectedAccess.deposit() {
        if (!permitted()) return
        val count = minOf(invTotal(inv, ScarItems.chunks), (500_000 - vars[ScarBits.essence]) / 5)
        if (count <= 0) {
            mes("You need tainted essence chunks and room in the Ventriculus.")
            return
        }
        if (invDel(inv, ScarItems.chunks, count).success) {
            vars[ScarBits.essence] += count * 5
            mes(
                "You deposit $count chunks. The Ventriculus holds ${vars[ScarBits.essence]} tainted essence."
            )
        }
    }

    private suspend fun ProtectedAccess.quantity(limit: Int, title: String): Int =
        if (limit <= 1) limit else countDialog("$title (up to $limit)").coerceIn(0, limit)

    private suspend fun ProtectedAccess.transferCoins(deposit: Boolean) {
        if (!permitted()) return
        val stored = vars[ScarVars.coins]
        val limit =
            if (deposit) minOf(invTotal(inv, objs.coins), Int.MAX_VALUE - stored) else stored
        val count =
            quantity(
                limit,
                if (deposit) "How many coins would you like to deposit?"
                else "How many coins would you like to withdraw?",
            )
        if (count <= 0 || vars[ScarVars.coins] != stored) return
        val result = if (deposit) invDel(inv, objs.coins, count) else invAdd(inv, objs.coins, count)
        if (result.success) {
            vars[ScarVars.coins] = stored + if (deposit) count else -count
            mes("Your coffer now holds ${vars[ScarVars.coins]} coins.")
        } else mes("There isn't enough room for the coins.")
    }

    private suspend fun ProtectedAccess.withdraw(extract: Extract?) {
        if (!permitted()) return
        val cost = extract?.price ?: 0
        val units = if (extract == null) 2 else 1
        val limit =
            minOf(
                vars[ScarBits.essence] / units,
                if (cost == 0) Int.MAX_VALUE else vars[ScarVars.coins] / cost,
            )
        if (limit == 0) {
            mes("You need enough tainted essence and payment for the requested output.")
            return
        }
        val count = quantity(limit, "How many would you like to withdraw?")
        if (
            count <= 0 ||
                vars[ScarBits.essence] < count * units ||
                vars[ScarVars.coins] < count * cost
        )
            return
        val output = extract?.item ?: RunecraftObjs.pureEssence
        val tx = invAdd(inv, output, count, cert = extract == null)
        if (!tx.success) {
            mes("You don't have enough room to collect the output.")
            return
        }
        vars[ScarBits.essence] -= count * units
        vars[ScarVars.coins] -= count * cost
        mes("You receive $count ${ocName(output).lowercase()}.")
    }

    private suspend fun ProtectedAccess.refund(extract: Extract) {
        if (!permitted()) return
        val price = extract.price * 9 / 10
        val available =
            minOf(invTotal(inv, extract.item), (Int.MAX_VALUE - invTotal(inv, objs.coins)) / price)
        val count =
            quantity(available, "How many extracts would you like to return at $price coins each?")
        if (count <= 0) return
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, extract.item.id, count)
                add(bag, objs.coins.id, count * price)
            }
        if (tx.success) mes("Hagus refunds ${count*price} coins.")
        else mes("You don't have room for the refund.")
    }
}
