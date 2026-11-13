package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object PenguinBits : VarBitReferences() {
    val quest = find("peng_quest")
    val form = find("peng_transmog")
}

internal object PenguinItems : ObjReferences() {
    val suit = find("peng_suit_unwound")
    val wound = find("peng_suit_wound")
    val clock = find("poh_clockwork_mechanism")
    val silk = find("silk")
}

internal object PenguinTimers : TimerReferences() {
    val form = find("penguin_course_form")
}

internal object PenguinLocs : LocReferences() {
    val clocks = (1..4).map { find("poh_clockmaking_$it") }
    val boats =
        listOf("peng_row_boat_clickzone", "peng_boat_ice", "peng_boat_3ops").map { find(it) }
    val avalanche = listOf(find("peng_aval_l"), find("peng_aval_r"))
    val interior = find("peng_base_door_agility")
    val entry = find("peng_base_double_door_mid")
    val west = find("peng_base_double_door_mid_agility")
    val cliff = find("peng_cliffwall_door_mid")
    val steps = find("peng_agility_steps01")
    val water = find("peng_agility_crushcourse_stepstone01")
    val stones = (1..7).map { find("peng_jump_stone_clickzone_0$it") }
    val icicle = find("peng_iciclepillar_clickzone")
    val ice = (1..9).map { find("peng_agility_slippery_glitters0$it") }
    val gate = find("peng_agility_fencing_door")
}

internal object PenguinNpcs : NpcReferences() {
    val larryRell = find("peng_larry_rell")
    val larryIce = find("peng_larry_ice")
    val larryZoo = find("peng_larry_zoo")
    val instructor = find("peng_agility_instructor")
    val suit = find("peng_suit_npc")
    val form = find("peng_suit_0op")
    val crushers = (1..4).map { find("peng_agility_crushcourse_crushblock0${it}_npc") }
}

internal object PenguinSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<PenguinSpawns>("penguins.toml")
    }
}

internal object PenguinObjEdits : ObjEditor() {
    init {
        edit(PenguinItems.suit) { wearpos = Wearpos.Back }
    }
}

internal object PenguinNpcEdits : NpcEditor() {
    init {
        for (npc in PenguinNpcs.crushers) edit(npc) {
            defaultMode = NpcMode.None
            timer = 1
        }
        for (npc in
            listOf(PenguinNpcs.larryIce, PenguinNpcs.larryRell, PenguinNpcs.instructor)) edit(npc) {
            defaultMode = NpcMode.None
        }
    }
}

internal object PenguinSeqs : SeqReferences() {
    val jump = find("peng_jump")
    val fail = find("peng_jump_fail")
    val tread = find("peng_slippery_walk")
    val slide = find("peng_slide_run")
}

public class PenguinCourse
@Inject
constructor(
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val collision: CollisionFlagMap,
    private val clock: MapClock,
) : PluginScript() {
    private val steps = WeakHashMap<Player, Int>()
    private val toys = WeakHashMap<Player, Npc>()
    private val dock = CoordGrid(2707, 3733)
    private val island = CoordGrid(2660, 3989, 1)
    private val shore = CoordGrid(2635, 4054, 0)
    private val stoneTiles =
        listOf(
            CoordGrid(2631, 4057, 1),
            CoordGrid(2631, 4059, 1),
            CoordGrid(2633, 4059, 1),
            CoordGrid(2635, 4059, 1),
            CoordGrid(2635, 4061, 1),
            CoordGrid(2635, 4063, 1),
            CoordGrid(2635, 4065, 1),
        )

    override fun ScriptContext.startup() {
        for (type in PenguinNpcs.crushers) onAiTimer(type) {
            val origin = npc.spawnCoords
            val next = CoordGrid(origin.x, crusherZ(origin.x, clock.cycle))
            if (npc.coords != next) npc.telejump(collision, next)
        }
        onOpNpc1(PenguinNpcs.larryRell) { sail(true) }
        onOpNpc4(PenguinNpcs.larryRell) { sail(true) }
        onOpNpc1(PenguinNpcs.larryIce) {
            if (choice2("Climb into the clockwork suit.", true, "Return to Rellekka.", false))
                tuxedo()
            else sail(false)
        }
        onOpNpc3(PenguinNpcs.larryIce) { tuxedo() }
        onOpNpc4(PenguinNpcs.larryIce) { sail(false) }
        onOpNpc1(PenguinNpcs.larryZoo) { tuxedo() }
        onOpNpc3(PenguinNpcs.larryZoo) { tuxedo() }
        for (boat in PenguinLocs.boats) onOpLoc1(boat) { sail(coords.z < 3900) }
        onOpLoc2(PenguinLocs.boats.last()) { sail(true) }
        for ((index, table) in PenguinLocs.clocks.withIndex()) {
            onOpLoc1(table) {
                if (
                    index >= 2 &&
                        choice2(
                            "Make a clockwork penguin.",
                            true,
                            "Make a clockwork mechanism.",
                            false,
                        )
                )
                    craft(true)
                else craft(false)
            }
            onOpLocU(table, objs.steel_bar) { craft(false) }
            if (index >= 2)
                for (item in listOf(PenguinItems.clock, PenguinItems.silk, objs.plank)) onOpLocU(
                    table,
                    item,
                ) {
                    craft(true)
                }
        }
        onOpHeld1(PenguinItems.suit) { invReplace(inv, PenguinItems.suit, 1, PenguinItems.wound) }
        onOpHeld4(PenguinItems.wound) {
            if (!player.members || toys[player]?.isSlotAssigned == true) return@onOpHeld4
            if (invDel(inv, PenguinItems.wound).failure) return@onOpHeld4
            val toy = Npc(types[PenguinNpcs.suit], coords)
            npcs.add(toy, 150)
            toys[player] = toy
        }
        onOpNpc1(PenguinNpcs.suit) {
            if (toys[player] !== it.npc || invAdd(inv, PenguinItems.suit).failure) return@onOpNpc1
            npcs.del(it.npc, Int.MAX_VALUE)
            toys.remove(player)
            statAdvance(stats.agility, 3.0)
        }
        onOpWorn1(PenguinItems.suit) { leaveSuit() }
        onPlayerLogin { if (PenguinItems.suit in player.worn) player.timer(PenguinTimers.form, 1) }
        onPlayerTimer(PenguinTimers.form) { tick() }
        onPlayerLogout {
            steps.remove(player)
            toys.remove(player)?.let { if (it.isSlotAssigned) npcs.del(it, Int.MAX_VALUE) }
        }
        for (aval in PenguinLocs.avalanche) onOpLoc1(aval) {
            if (formReady(90)) telejump(CoordGrid(2658, 10371))
        }
        onOpLoc1(PenguinLocs.entry) { if (formReady(90)) telejump(CoordGrid(2638, 4010, 1)) }
        onOpLoc1(PenguinLocs.interior) {
            if (formReady(90)) telejump(CoordGrid(if (coords.x >= 2640) 2639 else 2640, 10382))
        }
        onOpLoc1(PenguinLocs.west) { if (formReady(100)) telejump(CoordGrid(2643, 4033, 1)) }
        onOpLoc1(PenguinLocs.cliff) { if (formReady(100)) telejump(CoordGrid(2633, 10403)) }
        onOpLoc1(PenguinLocs.steps) {
            if (formReady(100)) {
                telejump(CoordGrid(2636, 4054, 1))
                steps.remove(player)
            }
        }
        onOpLoc1(PenguinLocs.water) { outOfWater(it.loc) }
        onApLoc1(PenguinLocs.water) { if (isWithinApRange(it.loc, 1)) outOfWater(it.loc) }
        for ((i, loc) in PenguinLocs.stones.withIndex()) {
            onOpLoc1(loc) { stone(i, it.loc) }
            onApLoc1(loc) { if (isWithinApRange(it.loc, 2)) stone(i, it.loc) }
        }
        onOpLoc1(PenguinLocs.icicle) { icicle(it.loc) }
        onApLoc1(PenguinLocs.icicle) { if (isWithinApRange(it.loc, 1)) icicle(it.loc) }
        for (ice in PenguinLocs.ice) {
            onOpLoc1(ice) { slide() }
            onApLoc1(ice) { if (isWithinApRange(it.loc, 1)) slide() }
        }
        onOpLoc1(PenguinLocs.gate) {
            if (!formReady(100)) return@onOpLoc1
            if (coords.x >= 2652) {
                if (steps[player] == 14) {
                    recordLap(AgilityLap.Penguin)
                    statAdvance(stats.agility, 65.0)
                }
                steps.remove(player)
                telejump(CoordGrid(2651, 4039, 1))
            } else telejump(CoordGrid(2653, 4039, 1))
        }
        onOpNpc1(PenguinNpcs.instructor) {
            mes("Return through the gate when you finish the course.")
        }
    }

    private suspend fun ProtectedAccess.craft(suit: Boolean) {
        if (!player.members) return
        if (stat(stats.crafting) < if (suit) 30 else 8) {
            mes("You need a higher Crafting level.")
            return
        }
        if (suit && vars[PenguinBits.quest] < 25) {
            mes("Larry hasn't taught you how to make a clockwork penguin.")
            return
        }
        anim(GnomeSequences.climb)
        delay(3)
        val paid =
            player.invTransaction(inv) {
                val bag = select(inv)
                if (suit) {
                    delete(bag, PenguinItems.clock.id, 1)
                    delete(bag, objs.plank.id, 1)
                    delete(bag, PenguinItems.silk.id, 1)
                    add(bag, PenguinItems.suit.id, 1)
                } else {
                    delete(bag, objs.steel_bar.id, 1)
                    add(bag, PenguinItems.clock.id, 1)
                }
            }
        if (paid.success) statAdvance(stats.crafting, 15.0)
        else
            mes(
                if (suit) "You need a clockwork mechanism, a plank, and silk."
                else "You need a steel bar."
            )
    }

    private fun ProtectedAccess.sail(out: Boolean) {
        if (!player.members || vars[PenguinBits.quest] < 5) {
            mes("Speak to Larry about his expedition first.")
            return
        }
        if (PenguinItems.suit in worn) {
            leaveSuit()
            if (PenguinItems.suit in worn) return
        }
        telejump(if (out) island else dock)
    }

    private fun ProtectedAccess.tuxedo() {
        if (!player.members || vars[PenguinBits.quest] < 30) {
            mes("Larry hasn't taught you to use the suit.")
            return
        }
        if (PenguinItems.suit in worn) {
            leaveSuit()
            return
        }
        if (
            listOf(Wearpos.Hands, Wearpos.RightHand, Wearpos.LeftHand, Wearpos.Back).any {
                worn[it.slot] != null
            }
        ) {
            mes("Remove your gloves, weapons, shield and cape first.")
            return
        }
        val slot = inv.indexOfFirst { it?.id == PenguinItems.suit.id }
        if (slot < 0) {
            mes("Bring an unwound clockwork penguin suit.")
            return
        }
        invEquip(slot)
        if (PenguinItems.suit in worn) {
            vars[PenguinBits.form] = 1
            transmog(PenguinNpcs.form)
            timer(PenguinTimers.form, 1)
        }
    }

    private fun ProtectedAccess.leaveSuit() {
        if (PenguinItems.suit in worn) wornUnequip(Wearpos.Back.slot)
        if (PenguinItems.suit in worn) return
        vars[PenguinBits.form] = 0
        resetTransmog()
        player.clearTimer(PenguinTimers.form)
        steps.remove(player)
        if (coords.x in 2624..2679 && (coords.z in 4030..4095 || coords.z in 10368..10432))
            telejump(island)
    }

    private fun ProtectedAccess.formReady(stage: Int): Boolean {
        if (
            !player.members ||
                vars[PenguinBits.quest] < stage ||
                PenguinItems.suit !in worn ||
                vars[PenguinBits.form] == 0
        ) {
            mes(
                "You need Larry's disguise and clearance to enter this part of the penguin outpost."
            )
            return false
        }
        return true
    }

    private fun ProtectedAccess.tick() {
        if (PenguinItems.suit !in worn) {
            leaveSuit()
            return
        }
        if (player.transmog?.id != PenguinNpcs.form.id) transmog(PenguinNpcs.form)
        if (coords.x in 2635..2637 && coords.z == 4054 && coords.level == 1 && formReady(100)) {
            if (stat(stats.agility) < 30) {
                mes("You need level 30 Agility for this course.")
                telejump(CoordGrid(2638, 4054, 1))
                return
            }
            steps[player] = 0
            telejump(shore)
            return
        }
        if (
            steps[player] == 0 &&
                coords.level == 0 &&
                coords.x in 2629..2634 &&
                coords.z in 4053..4056
        ) {
            // The ice crushers sweep alternate lanes in opposing phases.
            if (coords.z == crusherZ(coords.x, clock.cycle)) {
                takeInstantHit(HitType.Typeless, 1)
                telejump(coords.translateX(1))
            }
        }
    }

    private fun crusherZ(x: Int, cycle: Int): Int {
        val phase = (cycle / 2 + (x - 2629) / 2) % 6
        val distance = if (phase <= 3) phase else 6 - phase
        return if ((x and 1) == 1) 4053 + distance else 4056 - distance
    }

    private suspend fun ProtectedAccess.outOfWater(stone: BoundLocInfo) {
        if (steps[player] != 0 || !formReady(100)) return
        crossObstacle(CoordGrid(stone.x, 4057, 1), PenguinSeqs.jump, 2)
        steps[player] = 1
        statAdvance(stats.agility, 55.0)
    }

    private fun ProtectedAccess.slips(): Boolean {
        val percent = 61 + (stat(stats.agility) - 1).coerceIn(0, 98) * 60 / 98
        return random.of(100) >= percent
    }

    private suspend fun ProtectedAccess.stone(i: Int, loc: BoundLocInfo) {
        if (steps[player] != i + 1 || !formReady(100) || loc.coords != stoneTiles[i]) return
        if (slips()) {
            anim(PenguinSeqs.fail)
            delay(2)
            takeInstantHit(HitType.Typeless, 1)
            telejump(CoordGrid(2630, 4056))
            steps[player] = 0
            return
        }
        crossObstacle(stoneTiles[i], PenguinSeqs.jump, 2)
        statAdvance(stats.agility, if (i == 6) 20.0 else 10.0)
        steps[player] = i + 2
    }

    private suspend fun ProtectedAccess.icicle(loc: BoundLocInfo) {
        val i = listOf(2644, 2652, 2658, 2662).indexOf(loc.x)
        if (i < 0 || steps[player] != 8 + i || coords.x > loc.x || !formReady(100)) return
        if (slips()) {
            takeInstantHit(HitType.Typeless, 1)
            return
        }
        crossObstacle(CoordGrid(loc.x + 1, loc.z, 1), PenguinSeqs.tread, 3)
        steps[player] = 9 + i
        statAdvance(stats.agility, 40.0)
    }

    private suspend fun ProtectedAccess.slide() {
        if (steps[player] != 12 || !formReady(100)) return
        if (slips()) {
            takeInstantHit(HitType.Typeless, 2)
            telejump(CoordGrid(2664, 4081, 1))
            return
        }
        crossObstacle(CoordGrid(2665, 4068, 1), PenguinSeqs.tread, 5)
        crossObstacle(CoordGrid(2656, 4040, 1), PenguinSeqs.slide, 8)
        steps[player] = 14
        statAdvance(stats.agility, 180.0)
    }
}
