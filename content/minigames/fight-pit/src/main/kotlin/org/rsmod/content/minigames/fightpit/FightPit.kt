package org.rsmod.content.minigames.fightpit

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerCombatPermission
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object PitLocs : LocReferences() {
    val outer = find("tzhaar_fightpit_door_entrance")
    val inner = find("tzhaar_fightpit_door_closed")
}

internal object PitTimers : TimerReferences() {
    val round = find("fight_pit_round")
}

internal object PitInterfaces : InterfaceReferences() {
    val hud = find("tzhaar_fightpit")
}

internal object PitComponents : ComponentReferences() {
    val champion = find("tzhaar_fightpit:tzhaar_fightpit_champion")
}

internal object PitVarps : VarpReferences() {
    val remaining = find("tzhaar_fightpit_remaining")
}

internal object PitNpcs : NpcReferences() {
    val swarms =
        (1..3).map {
            listOf(find("tzhaar_fightpit_swarm_${it}a"), find("tzhaar_fightpit_swarm_${it}b"))
        }
}

public class FightPit
@Inject
constructor(
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val ground: ObjRepository,
    private val events: EventBus,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val interactions: AiPlayerInteractions,
) : PluginScript() {
    private val waiting = linkedSetOf<Player>()
    private val fighters = linkedSetOf<Player>()
    private val monsters = mutableListOf<Npc>()
    private val crowns = mutableMapOf<Player, Int>()
    private var Player.foes by intVarp(PitVarps.remaining)
    private var champion: Player? = null
    private var championName = "None"
    private var nextStart: Int? = null
    private var roundStart = 0
    private var defeatedLevels = 0
    private var lastTick = -1

    override fun ScriptContext.startup() {
        onOpLoc1(PitLocs.outer) {
            if (!player.members) return@onOpLoc1
            if (player.coords.z >= 5176) {
                telejump(WAITING_DOOR)
                enqueue(player)
            } else {
                remove(player)
                telejump(OUTSIDE)
            }
        }
        onOpLoc1(PitLocs.inner) {
            if (player in fighters || champion === player) {
                remove(player)
                telejump(WAITING)
                enqueue(player)
            } else {
                mes("Wait here. When the next fight starts you will be taken into the pit.")
            }
        }
        onEvent<PlayerCombatPermission> {
            if (inArena(attacker.coords) || inArena(target.coords)) {
                allowed =
                    attacker !== target &&
                        attacker in fighters &&
                        target in fighters &&
                        inArena(attacker.coords) &&
                        inArena(target.coords)
            }
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            if (player in fighters || champion === player) {
                if (player in fighters) defeatedLevels += player.combatLevel
                remove(player)
                safeDestination = WAITING
                enqueue(player)
            }
        }
        onEvent<PlayerTeleportEvent.Start> {
            clearCrown(player)
            if (player in fighters || player in waiting || player === champion) remove(player)
        }
        onEvent<PlayerHitEvents.Impact> { if (hit.isFromNpc && damage > 0) clearCrown(player) }
        onEvent<HeldEquipEvents.WearposChange> {
            if (!inArena(player.coords) && player !in waiting) clearCrown(player)
        }
        onPlayerSoftTimer(PitTimers.round) {
            if (lastTick != clock.cycle) {
                lastTick = clock.cycle
                tick()
            }
            if (
                player in crowns &&
                    (clock.cycle >= crowns.getValue(player) || player.overheadIcon != null)
            )
                clearCrown(player)
            if (
                player !in crowns &&
                    player !in waiting &&
                    player !in fighters &&
                    player !== champion
            )
                player.clearSoftTimer(PitTimers.round)
        }
        onPlayerLogout {
            remove(player)
            clearCrown(player)
        }
        onPlayerLogin {
            if (inArena(player.coords) || inLobby(player.coords)) move(player, OUTSIDE)
        }
    }

    private fun enqueue(player: Player) {
        waiting += player
        player.softTimer(PitTimers.round, 1)
        player.ifOpenOverlay(PitInterfaces.hud, events)
        updateHud(player)
        schedule()
    }

    private fun schedule() {
        if (fighters.isEmpty() && waiting.size + (if (champion == null) 0 else 1) >= 2) {
            if (nextStart == null) nextStart = clock + 50
        } else nextStart = null
    }

    private fun tick() {
        if (nextStart?.let { clock.cycle >= it } == true) {
            nextStart = null
            val entrants = waiting.toList() + listOfNotNull(champion)
            if (entrants.size < 2) return
            waiting.clear()
            champion = null
            roundStart = clock.cycle
            defeatedLevels = 0
            fighters.addAll(entrants)
            entrants.forEachIndexed { index, player ->
                move(player, SPAWNS[index % SPAWNS.size])
                MiscOutput.setPlayerOp(player, 2, "Attack", priority = true)
                player.mes("Fight!")
                updateHud(player)
            }
        }
        if (fighters.isEmpty()) return
        val elapsed = clock.cycle - roundStart
        if (elapsed in listOf(550, 650, 750)) {
            val stage = (elapsed - 550) / 100
            fighters.forEachIndexed { index, player ->
                val type = PitNpcs.swarms[stage][index % 2]
                val npc = Npc(types[type], SPAWNS[index % SPAWNS.size])
                npcs.add(npc, 1500)
                npc.opPlayer2(player, interactions)
                monsters += npc
            }
        } else if (elapsed >= 850) {
            fighters.forEach { if (it.hitpoints > 0) it.queueHit(0, HitType.Typeless, 1) }
        }
    }

    private fun remove(player: Player) {
        val wasFighting = fighters.remove(player)
        val wasWaiting = waiting.remove(player)
        val wasChampion = champion === player
        if (!wasFighting && !wasWaiting && !wasChampion) return
        if (wasChampion) champion = null
        MiscOutput.setPlayerOp(player, 2, null)
        player.ifCloseOverlay(PitInterfaces.hud, events)
        player.foes = 0
        if (wasFighting && fighters.size == 1) {
            val survivor = fighters.single()
            if (survivor.hitpoints > 0) {
                fighters.clear()
                champion = survivor
                championName = survivor.displayName
                if (defeatedLevels > 0) survivor.invAddOrDrop(ground, objs.tokkul, defeatedLevels)
                survivor.completeDiaryTask(DiaryTask.FightPitChampion)
                survivor.skullIcon = 1
                survivor.rebuildAppearance()
                crowns[survivor] = clock + 6000
                MiscOutput.setPlayerOp(survivor, 2, null)
                survivor.mes("You are the Fight Pit champion!")
                updateHud(survivor)
            }
        }
        if (fighters.isEmpty()) {
            monsters.filter { it.slotId != INVALID_SLOT }.forEach { npcs.del(it, Int.MAX_VALUE) }
            monsters.clear()
        }
        (waiting + fighters).forEach(::updateHud)
        schedule()
    }

    private fun updateHud(player: Player) {
        player.foes = (fighters.size - if (player in fighters) 1 else 0).coerceAtLeast(0)
        player.ifSetText(PitComponents.champion, "Current Champion: $championName")
    }

    private fun clearCrown(player: Player) {
        if (crowns.remove(player) != null && player.skullIcon == 1) {
            player.skullIcon = null
            player.rebuildAppearance()
        }
    }

    private fun move(player: Player, dest: CoordGrid) =
        PathingEntityCommon.telejump(player, collision, dest)

    private fun inArena(c: CoordGrid): Boolean =
        c.level == 0 && c.x in 2371..2424 && c.z in 5126..5167

    private fun inLobby(c: CoordGrid): Boolean =
        c.level == 0 && c.x in 2394..2403 && c.z in 5168..5175

    private companion object {
        val OUTSIDE = CoordGrid(2399, 5177)
        val WAITING_DOOR = CoordGrid(2399, 5175)
        val WAITING = CoordGrid(2399, 5169)
        val SPAWNS =
            listOf(
                CoordGrid(2399, 5165),
                CoordGrid(2399, 5163),
                CoordGrid(2398, 5164),
                CoordGrid(2400, 5164),
            )
    }
}
