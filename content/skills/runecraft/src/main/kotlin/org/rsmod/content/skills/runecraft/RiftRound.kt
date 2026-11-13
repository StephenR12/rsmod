package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

private class RiftVisit(
    var altar: RuneTemple? = null,
    var returnTile: CoordGrid = RiftMapTiles.inside,
    var fullCellXp: Int = 0,
    var fullCellEnergy: Int = 0,
)

internal class RiftBarrier(
    var tier: Int = 0,
    var health: Int = 0,
    var wall: LocInfo? = null,
    var marker: Npc? = null,
) {
    val maxHealth
        get() = if (tier <= 0) 0 else 50 shl (tier - 1)
}

private data class RiftCreature(val npc: Npc, val tier: Int, var health: Int, val lane: Int)

internal enum class RiftPhase {
    Idle,
    Preparation,
    Open,
    Closing,
    Intermission,
}

@Singleton
internal class RiftRound
@Inject
constructor(
    private val controllers: ControllerRepository,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val world: org.rsmod.api.repo.world.WorldRepository,
    private val locs: LocRepository,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
    private val events: EventBus,
    private val pouches: EssencePouchStorage,
) {
    private val players = mutableMapOf<Player, RiftVisit>()
    private val barriers = RiftMapTiles.barriers.associateWith { RiftBarrier() }
    private val defenders = mutableListOf<RiftCreature>()
    private val attackers = mutableListOf<RiftCreature>()
    private var controller: Controller? = null
    private var elapsed = 0
    private var serial = 0
    private var charge = 0
    private var target = 250
    private var secondWave = false
    private var victory = false
    var locked = false
        private set

    var phase = RiftPhase.Idle
        private set

    var elemental: RuneTemple? = null
        private set

    var catalytic: RuneTemple? = null
        private set

    private var minePortal: CoordGrid? = null
    private val guardianCap
        get() = (2 + players.size / 4).coerceAtMost(10)

    val population
        get() = players.size

    fun setup() {
        if (controller != null) return
        val c = Controller(RiftGame.controller, RiftMapTiles.center)
        controllers.add(c, Int.MAX_VALUE)
        c.aiTimer(1)
        controller = c
    }

    fun join(p: Player): Boolean {
        if (!p.members || p.vars[RiftBits.quest] < 130 || p.statBase(stats.runecrafting) < 27) {
            p.mes("You need to complete Temple of the Eye and have 27 Runecraft.")
            return false
        }
        if (p in players) return false
        if (phase == RiftPhase.Open && (locked || population < 100) || phase == RiftPhase.Closing) {
            p.mes("The game is in progress. Wait for the barrier to open.")
            return false
        }
        setup()
        players[p] = RiftVisit()
        if (phase == RiftPhase.Idle) prepare() else resetPlayer(p)
        p.ifOpenOverlay(RiftInterface.hud, events)
        p.activityBlocksTeleport = true
        show(p)
        return true
    }

    fun closeToNewEntries(p: Player) {
        if (p in players && phase == RiftPhase.Preparation) {
            locked = true
            p.mes("The barrier will stay closed when the rift opens.")
        }
    }

    fun isParticipant(p: Player) = p in players

    fun inTemple(p: Player) = p in players && RiftMapTiles.inBattle(p.coords)

    fun playingAltar(p: Player, recipe: StandardRuneRecipe): Boolean =
        players[p]?.altar?.recipe == recipe

    fun portalActive(temple: RuneTemple) =
        phase == RiftPhase.Open && (temple == elemental || temple == catalytic)

    fun enterAltar(a: ProtectedAccess, temple: RuneTemple, useTalisman: Boolean): Unit =
        with(a) {
            val visit = players[player] ?: return
            if (!inTemple(player) || phase != RiftPhase.Open || !player.canUseRiftAltar(temple))
                return
            if (stat(stats.runecrafting) < temple.recipe.level) {
                mes("You need level ${temple.recipe.level} Runecraft for this altar.")
                return
            }
            if (
                !portalActive(temple) &&
                    !(useTalisman && invDel(inv, RiftItems.talismans.getValue(temple)).success)
            ) {
                mes("That portal is inactive.")
                return
            }
            visit.returnTile = coords
            visit.altar = temple
            teleport(temple.inside)
        }

    fun returnFromAltar(a: ProtectedAccess, temple: RuneTemple): Boolean {
        val visit = players[a.player] ?: return false
        if (visit.altar != temple) return false
        visit.altar = null
        a.teleport(visit.returnTile)
        return true
    }

    fun cleanup(p: Player, move: Boolean) {
        players.remove(p)
        clearItems(p)
        p.activityBlocksTeleport = false
        VarPlayerIntMapSetter.set(p, TempleVars.playingGuardiansOfTheRift, 0)
        VarPlayerIntMapSetter.set(p, RiftBits.minePortal, 0)
        VarPlayerIntMapSetter.set(p, RiftBits.finished, 0)
        p.ifCloseOverlay(RiftInterface.hud, events)
        if (move) PathingEntityCommon.telejump(p, collision, RiftMapTiles.lobby)
        if (players.isEmpty() && phase != RiftPhase.Idle) {
            clearWorld()
            phase = RiftPhase.Idle
            locked = false
        }
    }

    private fun clearItems(p: Player) {
        val items = RiftItems.roundItems.map { it.id }.toSet()
        for (i in p.inv.indices) if (p.inv[i]?.id in items) p.inv[i] = null
        for (pouch in EssencePouchDefinitions.all) if (
            pouches.essenceType(p, pouch) == EssenceType.Guardian
        )
            pouches.setState(p, pouch, 0, null)
    }

    private fun resetPlayer(p: Player) {
        clearItems(p)
        VarPlayerIntMapSetter.set(p, TempleVars.playingGuardiansOfTheRift, 1)
        VarPlayerIntMapSetter.set(p, RiftVars.gameId, serial)
        for (key in
            RiftBits.energy +
                listOf(
                    RiftBits.finished,
                    RiftBits.mineCount,
                    RiftBits.minePortal,
                )) VarPlayerIntMapSetter.set(p, key, 0)
        players[p]?.apply {
            fullCellEnergy = 0
            fullCellXp = 0
        }
    }

    private fun prepare() {
        clearWorld()
        phase = RiftPhase.Preparation
        elapsed = 0
        serial++
        locked = false
        charge = 0
        secondWave = false
        for ((p, v) in players) {
            if (v.altar != null) {
                PathingEntityCommon.telejump(p, collision, v.returnTile)
                v.altar = null
            }
            resetPlayer(p)
            show(p)
        }
    }

    fun tick() {
        if (phase == RiftPhase.Idle) return
        for ((p, v) in players.toMap()) {
            val altar = v.altar
            if (
                !RiftMapTiles.inBattle(p.coords) &&
                    (altar == null ||
                        p.coords.x / 64 != altar.inside.x / 64 ||
                        p.coords.z / 64 != altar.inside.z / 64)
            )
                cleanup(p, false)
        }
        if (phase == RiftPhase.Idle) return
        elapsed++
        when (phase) {
            RiftPhase.Preparation ->
                if (elapsed >= 200) {
                    target = 250 * minOf(population, 20) + 200 * (population - 20).coerceAtLeast(0)
                    charge = (target / 10).coerceAtLeast(1)
                    phase = RiftPhase.Open
                    elapsed = 0
                    rotate()
                    openMinePortal()
                }
            RiftPhase.Open -> {
                if (elapsed % 84 == 0) rotate()
                if (elapsed % 233 == 0) openMinePortal()
                if (elapsed % 233 == 42) removeMinePortal()
                if (elapsed % 24 == 0) spawnAttacker()
                if (elapsed % 5 == 0) fight()
                if (charge <= 0) close(false)
            }
            RiftPhase.Closing -> if (elapsed >= 10) finish()
            RiftPhase.Intermission -> if (elapsed >= 50) prepare()
            RiftPhase.Idle -> Unit
        }
        if (elapsed % 5 == 0) for (p in players.keys) show(p)
    }

    private fun rotate() {
        val eligible = riftTemples.filter { elapsed >= 168 || it.cellTier >= 2 }
        elemental = random.pick(eligible.filter { it.elemental && it != elemental })
        catalytic = random.pick(eligible.filter { !it.elemental && it != catalytic })
        for (temple in listOfNotNull(elemental, catalytic)) {
            world.spotanimMap(
                RiftEffects.beams.getValue(temple),
                RiftMapTiles.portalCentres.getValue(temple),
            )
        }
    }

    private fun openMinePortal() {
        removeMinePortal()
        minePortal = random.pick(RiftMapTiles.shortcuts)
        locs.add(
            minePortal!!,
            RiftLocs.portalTop,
            42,
            org.rsmod.game.loc.LocAngle.West,
            org.rsmod.game.loc.LocShape.CentrepieceStraight,
        )
        players.keys.forEach { VarPlayerIntMapSetter.set(it, RiftBits.minePortal, 1) }
    }

    private fun removeMinePortal() {
        minePortal?.let { c ->
            locs.findExact(c, RiftLocs.portalTop)?.let { locs.del(it, Int.MAX_VALUE) }
        }
        minePortal = null
        players.keys.forEach { VarPlayerIntMapSetter.set(it, RiftBits.minePortal, 0) }
    }

    fun enterMine(a: ProtectedAccess, tile: CoordGrid) {
        if (inTemple(a.player) && minePortal == tile) a.telejump(CoordGrid(3594, 9503))
    }

    fun restorePower(a: ProtectedAccess): Unit =
        with(a) {
            if (!inTemple(player) || phase != RiftPhase.Open && phase != RiftPhase.Closing) return
            val quantities = RiftItems.stones.map { invTotal(inv, it) }
            val sum = quantities.sum()
            if (sum == 0) {
                mes("You have no guardian stones.")
                return
            }
            for ((i, stone) in RiftItems.stones.withIndex()) if (quantities[i] > 0)
                invDel(inv, stone, quantities[i])
            energy(
                player,
                quantities[0] * 2 + quantities[2] * 3,
                quantities[1] * 2 + quantities[3] * 3,
            )
            statAdvance(stats.runecrafting, sum * 5.0)
            player.runEnergy =
                (player.runEnergy + (sum + 1 + vars[varps.sa_energy] / 100) * 100).coerceAtMost(
                    constants.run_max_energy
                )
            charge = (charge + sum).coerceAtMost(target)
            if (!secondWave && charge * 100 >= target * 60) {
                secondWave = true
                val farRight = barriers.getValue(RiftMapTiles.barriers.last())
                val blast =
                    if (farRight.tier > 0) minOf(50, (farRight.health - 1).coerceAtLeast(0)) else 50
                for ((c, b) in barriers) if (b.tier > 0) damageBarrier(c, b, blast)
            }
            if (charge == target && phase == RiftPhase.Open) close(true)
            show(player)
        }

    private fun close(success: Boolean) {
        victory = success
        phase = RiftPhase.Closing
        elapsed = 0
        elemental = null
        catalytic = null
        for (m in attackers + defenders) npcs.del(m.npc, Int.MAX_VALUE)
        attackers.clear()
        defenders.clear()
    }

    private fun finish() {
        for (p in players.keys) {
            val totals = RiftBits.energy.map { p.vars[it] }
            if (victory && totals.sum() >= 300) {
                for (i in totals.indices) {
                    val awards = totals[i] / 100 + if (random.of(100) < totals[i] % 100) 1 else 0
                    val key = RiftBits.points[i]
                    VarPlayerIntMapSetter.set(p, key, (p.vars[key] + awards).coerceAtMost(65535))
                }
                VarPlayerIntMapSetter.set(p, RiftVars.completions, p.vars[RiftVars.completions] + 1)
                p.statAdvance(stats.runecrafting, p.statBase(stats.runecrafting) * 45.0)
                p.mes(
                    "The Great Guardian seals the rift. You earn reward points for your contribution."
                )
            } else
                p.mes(
                    if (victory) "You needed 300 energy to earn a reward."
                    else "The creatures have overwhelmed the Great Guardian."
                )
            VarPlayerIntMapSetter.set(p, RiftBits.finished, 1)
        }
        removeMinePortal()
        phase = RiftPhase.Intermission
        elapsed = 0
        locked = false
    }

    private fun energy(p: Player, elemental: Int, catalytic: Int) {
        val bonus =
            when (p.lefthand?.id) {
                26842 -> 10
                26836,
                26848 -> 5
                else -> 0
            }
        val multiplier = if (bonus > 0 && random.of(100) < bonus) 2 else 1
        var remaining = 1200 + 12 * bonus - RiftBits.energy.sumOf { p.vars[it] }
        for ((i, value) in listOf(elemental, catalytic).withIndex()) {
            val key = RiftBits.energy[i]
            val delta =
                minOf(value * multiplier, remaining.coerceAtLeast(0), 1000 - p.vars[key])
                    .coerceAtLeast(0)
            VarPlayerIntMapSetter.set(p, key, p.vars[key] + delta)
            remaining -= delta
        }
    }

    private fun show(p: Player) {
        if (RiftInterface.hud !in p.ui) return
        val caption =
            when (phase) {
                RiftPhase.Preparation -> "The rift will open in ${(200-elapsed)*3/5}s"
                RiftPhase.Open -> "The rift is open"
                RiftPhase.Closing -> "The rift is closing"
                else -> "Waiting for the next rift"
            }
        p.ifSetText(RiftButtons.phase, caption)
        // Clientscript 5980 draws the native power bar, energy counters, altar icons and timers.
        val mine = minePortal
        val direction =
            when (mine) {
                null -> 0
                RiftMapTiles.shortcuts[0] -> 3
                RiftMapTiles.shortcuts[1] -> 1
                else -> 4
            }
        val portalTicks = if (phase == RiftPhase.Open) 84 - elapsed % 84 else 0
        val mineTicks = if (mine != null) maxOf(0, 42 - elapsed % 233) else 0
        val elementalIcon =
            listOf(RuneTemple.Air, RuneTemple.Water, RuneTemple.Earth, RuneTemple.Fire)
                .indexOf(elemental) + 1
        val catalyticIcon =
            listOf(
                    RuneTemple.Mind,
                    RuneTemple.Body,
                    RuneTemple.Cosmic,
                    RuneTemple.Chaos,
                    RuneTemple.Nature,
                    RuneTemple.Law,
                    RuneTemple.Death,
                    RuneTemple.Blood,
                )
                .indexOf(catalytic) + 1
        p.runClientScript(
            5980,
            p.vars[RiftBits.energy[0]],
            p.vars[RiftBits.energy[1]],
            maxOf(0, charge),
            target,
            direction,
            elementalIcon,
            catalyticIcon,
            defenders.size,
            guardianCap,
            portalTicks,
            mineTicks,
        )
    }

    private fun clearWorld() {
        for (m in attackers + defenders) npcs.del(m.npc, Int.MAX_VALUE)
        attackers.clear()
        defenders.clear()
        removeMinePortal()
        elemental = null
        catalytic = null
        for ((c, b) in barriers) {
            if (b.tier != 0) tile(c, 0)
            b.tier = 0
            b.health = 0
        }
    }

    private fun tile(c: CoordGrid, tier: Int) {
        val b = barriers.getValue(c)
        val old = locs.findAll(c).firstOrNull { l -> RiftLocs.tiles.any { it.id == l.id } }
        if (old != null && old.id != RiftLocs.tiles[tier + 1].id)
            locs.change(old, RiftLocs.tiles[tier + 1], Int.MAX_VALUE)
        val lane = RiftMapTiles.barriers.indexOf(c)
        val large = if (lane == 0 || lane == 3 || lane == 6) 1 else 0
        val wall = if (tier > 0) RiftLocs.walls[tier - 1][large] else null
        if (b.wall?.id != wall?.id) {
            b.wall?.let { locs.del(it, Int.MAX_VALUE) }
            b.marker?.let { npcs.del(it, Int.MAX_VALUE) }
            b.wall = null
            b.marker = null
            if (wall != null) {
                val corner = RiftMapTiles.wallCorners[lane]
                b.wall =
                    locs.add(
                        corner,
                        wall,
                        Int.MAX_VALUE,
                        LocAngle.West,
                        LocShape.CentrepieceStraight,
                    )
                val marker = Npc(types[RiftNpcs.walls[tier - 1][large]], corner)
                marker.mode = NpcMode.None
                npcs.add(marker, Int.MAX_VALUE)
                b.marker = marker
            }
        }
        updateHealth(b)
    }

    private fun updateHealth(b: RiftBarrier) {
        if (b.health <= 0 || b.maxHealth == 0) return
        val fill = (30 * b.health / b.maxHealth).coerceIn(1, 30)
        b.marker?.showHeadbar(Headbar.fromNoSource(0, 0, fill, fill, 0, 0))
    }

    private fun damageBarrier(c: CoordGrid, b: RiftBarrier, damage: Int) {
        b.health = (b.health - damage).coerceAtLeast(0)
        if (b.health == 0) {
            b.tier = -1
            tile(c, -1)
        } else updateHealth(b)
    }

    private fun spawnAttacker() {
        val tier = if (secondWave) random.of(1, 3) else random.pick(listOf(1, 1, 1, 2, 3))
        val lanes = barriers.keys.toList()
        val protected = lanes.indices.filter { barriers.getValue(lanes[it]).tier > 0 }
        val lane = if (protected.isEmpty()) random.of(lanes.size) else random.pick(protected)
        val npc = Npc(types[RiftNpcs.attackers[tier - 1]], CoordGrid(3614 + random.of(3), 9524))
        npcs.add(npc, Int.MAX_VALUE)
        npc.mode = NpcMode.None
        npc.walk(RiftMapTiles.wallCorners[lane].translate(0, 3))
        attackers.add(RiftCreature(npc, tier, 4 * tier, lane))
    }

    private fun fight() {
        for (m in attackers.toList()) {
            val pos = RiftMapTiles.barriers[m.lane]
            val defender = defenders.minByOrNull { it.npc.coords.chebyshevDistance(m.npc.coords) }
            if (defender != null) {
                defender.npc.walk(m.npc.coords)
                if (defender.npc.coords.chebyshevDistance(m.npc.coords) <= 2) {
                    defender.npc.faceNpc(m.npc)
                    m.npc.faceNpc(defender.npc)
                    m.npc.anim(RiftAnims.attacks[m.tier - 1])
                    m.health -= defender.tier
                    defender.health -= m.tier
                    if (defender.health <= 0) {
                        defenders.remove(defender)
                        npcs.del(defender.npc, Int.MAX_VALUE)
                    }
                    if (m.health <= 0) {
                        attackers.remove(m)
                        npcs.del(m.npc, Int.MAX_VALUE)
                    }
                    continue
                }
            }
            val endpoint =
                RiftMapTiles.wallCorners[m.lane].translate(
                    0,
                    if (m.lane in listOf(0, 3, 6)) 3 else 2,
                )
            if (m.npc.coords.chebyshevDistance(endpoint) > 2) {
                m.npc.walk(endpoint)
                continue
            }
            m.npc.faceSquare(pos)
            m.npc.anim(RiftAnims.attacks[m.tier - 1])
            val b = barriers.getValue(pos)
            if (b.tier > 0) damageBarrier(pos, b, 2 * m.tier) else charge -= m.tier
        }
    }

    fun cell(a: ProtectedAccess, pos: CoordGrid, repair: Boolean): Unit =
        with(a) {
            if (!inTemple(player) || phase !in listOf(RiftPhase.Preparation, RiftPhase.Open)) return
            val b = barriers[pos] ?: return
            if (b.tier < 0) {
                if (repair && invDel(inv, RiftItems.fragment, 12).success) {
                    b.tier = 0
                    tile(pos, 0)
                    energy(player, 25, 25)
                } else mes("You need 12 guardian fragments to repair a broken tile.")
                return
            }
            val index = usableCell(a) ?: return
            val tier = index.coerceAtLeast(1)
            val full = b.tier != 0 && b.health == b.maxHealth
            val upgrading = tier > b.tier
            val stronger = b.tier != 0 && tier > b.tier + 1
            if (stronger) {
                mes("Strengthen the barrier one tier at a time.")
                return
            }
            invDel(inv, RiftItems.cells[index])
            val score =
                when {
                    b.tier == 0 -> listOf(17, 20, 24, 30)[tier - 1]
                    upgrading -> listOf(0, 7, 13, 22)[tier - 1]
                    else -> listOf(2, 5, 9, 15)[tier - 1]
                }
            b.tier = if (b.tier == 0) 1 else maxOf(b.tier, tier)
            b.health =
                if (upgrading) b.maxHealth
                else (b.health + listOf(10, 25, 50, 100)[tier - 1]).coerceAtMost(b.maxHealth)
            tile(pos, b.tier)
            if (phase == RiftPhase.Open) {
                val v = players.getValue(player)
                val xp = listOf(30, 100, 180, 250)[tier - 1]
                val earned =
                    if (full && !upgrading)
                        minOf(score, (450 - v.fullCellEnergy).coerceAtLeast(0) / 2)
                    else score
                val xpAward =
                    if (full && !upgrading) minOf(xp, (2500 - v.fullCellXp).coerceAtLeast(0))
                    else xp
                if (full && !upgrading) {
                    v.fullCellEnergy += earned * 2
                    v.fullCellXp += xpAward
                }
                energy(player, earned, earned)
                statAdvance(stats.runecrafting, xpAward.toDouble())
            }
            show(player)
        }

    private fun usableCell(a: ProtectedAccess): Int? {
        val index =
            (1..4).firstOrNull { RiftItems.cells[it] in a.inv }
                ?: if (phase == RiftPhase.Preparation && RiftItems.cells[0] in a.inv) 0 else null
        if (index == null) a.mes("You need a charged cell.")
        return index
    }

    fun assemble(a: ProtectedAccess, element: Boolean): Unit =
        with(a) {
            if (!inTemple(player) || phase !in listOf(RiftPhase.Preparation, RiftPhase.Open)) return
            if (org.rsmod.api.config.refs.objs.chisel !in inv) {
                mes("You need a chisel to assemble a guardian.")
                return
            }
            if (defenders.size >= guardianCap) {
                mes("The temple has all the guardians it can support.")
                return
            }
            val index = usableCell(a) ?: return
            val tier = index.coerceAtLeast(1)
            val npc = Npc(types[RiftNpcs.defenders[(if (element) 0 else 4) + tier - 1]], coords)
            npcs.add(npc, Int.MAX_VALUE)
            defenders.add(RiftCreature(npc, tier, 12 * tier, 0))
            invDel(inv, RiftItems.cells[index])
            anim(RiftAnims.guardian)
            npc.walk(RiftMapTiles.center.translate(if (element) 4 else -4, 5))
            if (phase == RiftPhase.Open) {
                val score = listOf(6, 15, 27, 45)[tier - 1]
                energy(player, if (element) score else 0, if (element) 0 else score)
                statAdvance(stats.crafting, 80.0)
                statAdvance(stats.runecrafting, listOf(30.0, 100.0, 180.0, 250.0)[tier - 1])
            }
            show(player)
        }
}

internal fun Player.canUseRiftAltar(temple: RuneTemple): Boolean {
    val req = temple.recipe.quest
    val allowed =
        (req == null || vars[req.varbit] >= req.completionValue) &&
            (temple != RuneTemple.Cosmic || vars[AbyssVars.lostCity] >= 6) &&
            (temple != RuneTemple.Law || vars[AbyssVars.trollStronghold] >= 50)
    if (!allowed) mes("You must finish the quest for this altar first.")
    return allowed
}
