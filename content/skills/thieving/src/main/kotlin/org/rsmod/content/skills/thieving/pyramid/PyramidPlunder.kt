package org.rsmod.content.skills.thieving.pyramid

import jakarta.inject.Inject
import org.rsmod.content.skills.pets.SkillPetRates
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.combat.NvPCombat
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.content.skills.magic.spell.utility.standard.PoisonProtectionRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Native eight-room activity. Exact published tables coexist with explicitly approximate timing.
 */
@Singleton
public class PyramidPlunder
@Inject
constructor(private val petRates: SkillPetRates,
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val npcList: NpcList,
    private val players: PlayerList,
    private val registry: PlayerRegistry,
    private val ground: ObjRepository,
    private val combat: NvPCombat,
    private val poison: PoisonProtectionRepository,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val events: EventBus,
    private val sceptre: PharaohSceptre,
) : PluginScript() {
    private data class Run(
        val started: Int,
        var room: Int = 0,
        var attempted: Int = 0,
        var sarcLooted: Boolean = false,
    )

    private data class Hazard(val owner: Player, val room: Int)

    private val runs = WeakHashMap<Player, Run>()
    private val hazards = WeakHashMap<Npc, Hazard>()
    private val exits = IntArray(8)
    private var entrance = 0
    private var nextEntrance = -1

    override fun ScriptContext.startup() {
        sceptre.bind(this)
        for ((index, door) in PyramidLocs.entrances.withIndex()) onOpLoc1(door) {
            enter(this, index)
        }
        onOpLoc1(PyramidLocs.lobbyExit) { finish(player, true) }
        onOpLoc1(PyramidLocs.exit) { finish(player, true) }
        onOpNpc1(PyramidNpcs.guardian) {
            when (choice3("Start Pyramid Plunder.", 1, "Recharge my sceptre.", 2, "Leave.", 0)) {
                1 -> start(this)
                2 -> sceptre.recharge(this)
            }
        }
        onOpNpc3(PyramidNpcs.guardian) { start(this) }
        for ((index, urn) in PyramidLocs.urns.withIndex()) {
            onOpLoc1(urn) { searchUrn(this, it.loc, index) }
            onOpLoc2(urn) { checkOrCharm(this, it.loc, index) }
            onOpLoc3(urn) { charm(this, it.loc, index) }
            onOpLocU(urn, PyramidObjs.charm) { charm(this, it.loc, index) }
        }
        for ((index, door) in PyramidLocs.doors.withIndex()) onOpLoc1(door) {
            door(this, it.loc, index)
        }
        onOpLoc1(PyramidLocs.spear) { pass(this, it.loc) }
        onOpLoc1(PyramidLocs.chest) { chest(this, it.loc) }
        onOpLoc1(PyramidLocs.sarcophagus) { sarcophagus(this, it.loc) }
        onPlayerSoftTimer(PyramidTimers.round) { watch(player) }
        onEvent<PlayerTeleportEvent.Start> { if (player in runs) finish(player, false) }
        onEvent<PlayerDeathEvent.ResolveRespawn> { if (player in runs) finish(player, false) }
        onPlayerLogout {
            if (PyramidRooms.inside(player.coords))
                registry.relocateOnLogout(player, PyramidRooms.outside)
            finish(player, false)
        }
        onPlayerLogin {
            if (PyramidRooms.inside(player.coords))
                PathingEntityCommon.telejump(player, collision, PyramidRooms.outside)
            finish(player, false)
        }
        for (type in listOf(PyramidNpcs.swarm) + PyramidNpcs.mummies) {
            onAiOpPlayer2(type) { attack(this, it.target) }
            onAiApPlayer2(type) { attack(this, it.target) }
            onEvent<NpcHitEvents.ResolveDamage>(type.id) {
                val owner = hazards[npc]?.owner
                if (
                    owner == null || !hit.isFromPlayer || hit.resolvePlayerSource(players) !== owner
                )
                    damage = 0
            }
        }
        onEvent<PlayerHitEvents.Impact> {
            if (!hit.isFromNpc) return@onEvent
            val source = hit.resolveNpcSource(npcList)
            if (
                source?.id == PyramidNpcs.swarm.id &&
                    hazards[source]?.owner === player &&
                    damage > 0
            )
                poison.inflictPoison(player, 15)
        }
        onEvent<NpcKillEvents.Credited> {
            val owner = hazards[npc]?.owner
            if (owner === player) {
                hazards.remove(npc)
                val remains = RemainsDrop(player, InvObj(objs.bones))
                events.publish(remains)
                val delivered =
                    remains.remaining == 0 ||
                        ground.add(
                            Obj.fromOwner(player, npc.coords, objs.bones, remains.remaining),
                            constants.lootdrop_duration,
                        )
                if (delivered) remains.commit()
            }
        }
    }

    public fun currentRoom(player: Player): Int? = runs[player]?.room

    public fun enter(access: ProtectedAccess, door: Int): Boolean =
        with(access) {
            if (!player.members) return false
            if (clock.cycle >= nextEntrance) {
                entrance = random.of(4)
                nextEntrance = clock.cycle + 2500
            }
            // The period source describes a level-dependent punch, without published endpoints.
            if (door == entrance && !statRandom(stats.thieving, 130, 220, 0)) {
                queueHit(1, HitType.Typeless, random.of(1..4))
                mes("A mysterious bandaged hand punches you away from the door.")
                return false
            }
            telejump(
                if (door == entrance) PyramidRooms.lobby
                else PyramidRooms.emptyLobbies[(door - entrance + 3) % 3]
            )
            true
        }

    public fun start(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members) {
                mes("Pyramid Plunder is only available on members worlds.")
                return false
            }
            if (player.statBase(stats.thieving) < 21) {
                mes("You need an unboosted Thieving level of 21 to play Pyramid Plunder.")
                return false
            }
            finish(player, false)
            for (i in exits.indices) exits[i] = random.of(4)
            val run = Run(mapClock)
            runs[player] = run
            vars[PyramidBits.played] = 1
            resetRoom(this, run)
            telejump(PyramidRooms.all[0].entry)
            ifOpenOverlay(PyramidInterfaces.overlay)
            player.softTimer(PyramidTimers.round, 1)
            true
        }

    private fun resetRoom(access: ProtectedAccess, run: Run): Unit =
        with(access) {
            for (bit in PyramidBits.urns) vars[bit] = 0
            for (bit in PyramidBits.doors) vars[bit] = 0
            vars[PyramidBits.chest] = 0
            vars[PyramidBits.sarcophagus] = 0
            vars[PyramidBits.push] = 0
            vars[PyramidBits.trap] = 1
            vars[PyramidBits.rightDoor] = 0
            vars[PyramidBits.room] = run.room + 1
            vars[PyramidBits.level] = run.room + 1
            vars[PyramidBits.required] = 21 + run.room * 10
            vars[PyramidBits.timer] = mapClock - run.started
            run.attempted = 0
            run.sarcLooted = false
        }

    private fun run(access: ProtectedAccess, loc: BoundLocInfo, trap: Boolean = false): Run? =
        with(access) {
            val r = runs[player] ?: return null
            if (
                !player.members ||
                    mapClock - r.started >= 500 ||
                    player.coords.level != 0 ||
                    PyramidRooms.nearest(loc.coords) != r.room ||
                    !isWithinDistance(loc, 1)
            )
                return null
            if (!trap && vars[PyramidBits.trap] != 0) {
                mes("Disarm the speartrap before plundering this room.")
                return null
            }
            r
        }

    public suspend fun pass(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            val r = run(this, loc, true) ?: return false
            if (vars[PyramidBits.trap] == 0) return true
            anim(PyramidSeqs.search)
            gatheringDelay(2)
            if (run(this, loc, true) !== r) return false
            if (!statRandom(stats.thieving, 160, 250, 0)) {
                queueHit(1, HitType.Typeless, random.of(1..4))
                return false
            }
            vars[PyramidBits.trap] = 0
            statAdvance(stats.thieving, 10.0)
            telejump(PyramidRooms.all[r.room].pastTrap)
            true
        }

    public suspend fun checkOrCharm(
        access: ProtectedAccess,
        loc: BoundLocInfo,
        index: Int,
    ): Boolean =
        with(access) {
            if (vars[PyramidBits.urns[index]] == 2) return charm(this, loc, index)
            val r = run(this, loc) ?: return false
            if (vars[PyramidBits.urns[index]] != 0) return false
            anim(PyramidSeqs.search)
            gatheringDelay(2)
            if (run(this, loc) !== r || vars[PyramidBits.urns[index]] != 0) return false
            if (!statRandom(stats.thieving, 160 - r.room, 250 - r.room, 0)) {
                bite(this)
                return false
            }
            vars[PyramidBits.urns[index]] = 2
            // Split presentation is modelled; published total XP is preserved exactly.
            statAdvance(stats.thieving, PyramidRooms.strengthXp[r.room])
            mes("You find a snake. It stares at you menacingly.")
            true
        }

    public suspend fun charm(access: ProtectedAccess, loc: BoundLocInfo, index: Int): Boolean =
        with(access) {
            val r = run(this, loc) ?: return false
            if (vars[PyramidBits.urns[index]] != 2 || PyramidObjs.charm !in inv) {
                mes("Check for a snake and bring a snake charm first.")
                return false
            }
            anim(PyramidSeqs.search)
            gatheringDelay(3)
            if (
                run(this, loc) !== r ||
                    PyramidObjs.charm !in inv ||
                    vars[PyramidBits.urns[index]] != 2
            )
                return false
            vars[PyramidBits.urns[index]] = 3
            true
        }

    public suspend fun searchUrn(access: ProtectedAccess, loc: BoundLocInfo, index: Int): Boolean =
        with(access) {
            val r = run(this, loc) ?: return false
            val bit = PyramidBits.urns[index]
            val state = vars[bit]
            if (state == 1) {
                mes("The urn is empty.")
                return false
            }
            anim(PyramidSeqs.search)
            gatheringDelay(2)
            if (run(this, loc) !== r || vars[bit] != state) return false
            val successful =
                state == 3 ||
                    statRandom(
                        stats.thieving,
                        (if (state == 2) 130 else 100) - r.room,
                        (if (state == 2) 220 else 180) - r.room,
                        0,
                    )
            if (!successful) {
                bite(this)
                return false
            }
            vars[bit] = 1
            val xp =
                PyramidRooms.urnXp[r.room] -
                    if (state == 2 || state == 3) PyramidRooms.strengthXp[r.room] else 0.0
            statAdvance(stats.thieving, xp)
            val table = PyramidLoot.urn[r.room]
            if (!invAdd(inv, table[random.of(table.size)]).success)
                mes("You leave the artefact behind because your inventory is full.")
            true
        }

    private fun bite(access: ProtectedAccess): Unit =
        with(access) {
            val damage = random.of(1..4)
            queueHit(1, HitType.Typeless, damage)
            poison.inflictPoison(player, damage * 5)
            mes("The snake bites you!")
        }

    public suspend fun door(access: ProtectedAccess, loc: BoundLocInfo, index: Int): Boolean =
        with(access) {
            val r = run(this, loc) ?: return false
            if (r.room == 7) {
                finish(player, true)
                return true
            }
            val bit = PyramidBits.doors[index]
            if (vars[bit] == 0) {
                val lock = PyramidObjs.lockpick in inv || PyramidObjs.hairclip in inv
                anim(PyramidSeqs.search)
                gatheringDelay(2)
                if (run(this, loc) !== r) return false
                if (
                    !statRandom(
                        stats.thieving,
                        (if (lock) 160 else 130) - r.room,
                        (if (lock) 250 else 220) - r.room,
                        0,
                    )
                )
                    return false
                vars[bit] = 1
                statAdvance(stats.thieving, PyramidRooms.chestXp[r.room] / if (lock) 2.0 else 1.0)
            }
            val fourth = Integer.bitCount(r.attempted) == 3 && r.attempted and (1 shl index) == 0
            r.attempted = r.attempted or (1 shl index)
            if (index != exits[r.room] && !fourth) {
                mes("This door is a dead end.")
                return false
            }
            if (player.statBase(stats.thieving) < 31 + r.room * 10) {
                mes(
                    "You need an unboosted Thieving level of " +
                        (31 + r.room * 10) +
                        " to enter the next room."
                )
                return false
            }
            clearHazards(player)
            r.room++
            resetRoom(this, r)
            telejump(PyramidRooms.all[r.room].entry)
            true
        }

    public suspend fun chest(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            val r = run(this, loc) ?: return false
            if (vars[PyramidBits.chest] != 0) return false
            anim(PyramidSeqs.search)
            gatheringDelay(2)
            if (run(this, loc) !== r || vars[PyramidBits.chest] != 0) return false
            vars[PyramidBits.chest] = 1
            petRates.roll(player,stats.thieving,20663,listOf(41_355,29_540,25_847,20_678,20_678,20_678,10_339,6_893)[r.room])
            val success = statRandom(stats.thieving, 130 - r.room, 220 - r.room, 0)
            if (success) statAdvance(stats.thieving, PyramidRooms.chestXp[r.room])
            if (r.room == 7) player.completeDiaryTask(DiaryTask.LootFinalPyramidChest)
            if (rare(this, r.room)) return true
            val table = PyramidLoot.chest[r.room]
            if (!invAdd(inv, table[random.of(4)]).success)
                mes("You leave the artefact behind because your inventory is full.")
            if (!success) {
                queueHit(1, HitType.Typeless, random.of(1..4))
                spawn(player, r.room, true)
            }
            true
        }

    public suspend fun sarcophagus(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            val r = run(this, loc) ?: return false
            if (r.sarcLooted) return false
            if (vars[PyramidBits.sarcophagus] == 0) {
                anim(PyramidSeqs.push)
                gatheringDelay(6)
                if (run(this, loc) !== r) return false
                vars[PyramidBits.sarcophagus] = 1
                statAdvance(stats.strength, PyramidRooms.strengthXp[r.room])
                if (r.room == 0) player.completeDiaryTask(DiaryTask.OpenFirstPyramidSarcophagus)
                // Strength-dependent mummy odds and six-tick opening are explicit playable models.
                if (!statRandom(stats.strength, 100 - r.room, 180 - r.room, 0))
                    spawn(player, r.room, false)
            }
            r.sarcLooted = true
            if (rare(this, r.room)) return true
            val table = PyramidLoot.sarcophagus[r.room]
            if (!invAdd(inv, table[random.of(4)]).success)
                mes("You leave the artefact behind because your inventory is full.")
            true
        }

    private fun rare(access: ProtectedAccess, room: Int): Boolean =
        with(access) {
            if (random.of(PyramidRooms.sceptreDenominator[room]) != 0) return false
            finish(player, true)
            invAddOrDrop(ground, PyramidObjs.initial)
            mes("You have found the Pharaoh's sceptre!")
            true
        }

    private fun spawn(player: Player, room: Int, swarm: Boolean) {
        val type = if (swarm) PyramidNpcs.swarm else PyramidNpcs.mummies[room % 5]
        val npc = Npc(npcTypes[type], player.coords)
        npcRepo.add(npc, 500)
        hazards[npc] = Hazard(player, room)
        npc.queueCombatRetaliate(player)
    }

    private fun attack(access: StandardNpcAccess, target: Player): Unit =
        with(access) {
            val hazard = hazards[npc]
            if (hazard == null || hazard.owner !== target || runs[target]?.room != hazard.room) {
                resetMode()
                return
            }
            val style =
                if (npc.id == PyramidNpcs.swarm.id) MeleeAttackType.Slash else MeleeAttackType.Crush
            combat.attack(this, target, CombatAttack.NpcMelee(style))
        }

    private fun clearHazards(player: Player) {
        val owned = hazards.filterValues { it.owner === player }.keys.toList()
        for (npc in owned) {
            hazards.remove(npc)
            if (npc.isVisible) npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    public fun watch(player: Player) {
        val r =
            runs[player]
                ?: run {
                    player.clearSoftTimer(PyramidTimers.round)
                    return
                }
        if (
            clock.cycle - r.started >= 500 ||
                !PyramidRooms.inside(player.coords) ||
                player.coords.level != 0
        ) {
            finish(player, true)
            return
        }
        VarPlayerIntMapSetter.set(
            player,
            PyramidBits.timer,
            (clock.cycle - r.started).coerceIn(0, 500),
        )
    }

    public fun finish(player: Player, leave: Boolean) {
        val existed = runs.remove(player) != null
        clearHazards(player)
        player.clearSoftTimer(PyramidTimers.round)
        if (PyramidInterfaces.overlay in player.ui)
            player.ifCloseOverlay(PyramidInterfaces.overlay, events)
        if (leave) {
            player.cancelActiveCoroutine()
            PathingEntityCommon.telejump(player, collision, PyramidRooms.outside)
        }
        if (existed) VarPlayerIntMapSetter.set(player, PyramidBits.timer, 0)
    }
}
