package org.rsmod.content.minigames.fightcaves

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Instant
import java.util.IdentityHashMap
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.timers
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.events.CombatAchievementEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Singleton
public class FightCaveController
@Inject
constructor(
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val interactions: AiPlayerInteractions,
    private val protectedAccess: ProtectedAccessLauncher,
    private val objRepo: ObjRepository,
) {
    private val sessions = IdentityHashMap<Player, FightCaveSession>()
    private val owners = IdentityHashMap<Npc, FightCaveSession>()
    private val suspended = mutableMapOf<String, SuspendedFightCave>()
    private val reentryBlockedUntil = mutableMapOf<String, Int>()

    private var Player.totalJadKills by intVarp(FightCaveVarps.totalJadKills)

    public suspend fun enter(access: ProtectedAccess) {
        val player = access.player
        if (!player.members) {
            access.mes("You need to be on a members' world to enter the Fight Cave.")
            return
        }
        val blockedUntil = reentryBlockedUntil[player.username] ?: Int.MIN_VALUE
        if (player.currentMapClock < blockedUntil) {
            val seconds = ((blockedUntil - player.currentMapClock) * 3 + 4) / 5
            access.mes("You must wait another $seconds seconds before entering the Fight Cave.")
            return
        }
        sessions.remove(player)?.let(::destroy)
        suspended.remove(player.username)
        val start = FightCaveWaves.rotationStart(Instant.now().epochSecond / 60)
        val session =
            create(player, wave = 1, rotationStart = start)
                ?: run {
                    access.mes("The Fight Cave is currently full. Please try again shortly.")
                    return
                }
        access.telejump(session.arena(FightCaveSpawn.C.localX, FightCaveSpawn.C.localZ))
        access.mes("You're on your own now, JalYt. Prepare to fight for your life!")
        access.delay(2)
        spawnWave(session)
    }

    public suspend fun leave(access: ProtectedAccess, applyCooldown: Boolean = true) {
        val session = sessions.remove(access.player) ?: return
        destroy(session)
        suspended.remove(access.player.username)
        if (applyCooldown) {
            reentryBlockedUntil[access.player.username] =
                access.player.currentMapClock + EXIT_COOLDOWN
        }
        access.telejump(OUTSIDE)
        access.mes("You leave the Fight Cave.")
    }

    public fun suspendOnLogout(player: Player) {
        val session = sessions.remove(player) ?: return
        suspended[player.username] = SuspendedFightCave(session.wave, session.rotationStart)
        destroy(session)
    }

    public fun resumeOnLogin(player: Player) {
        val saved = suspended.remove(player.username) ?: return
        protectedAccess.launch(player) {
            val session =
                create(player, saved.wave, saved.rotationStart)
                    ?: run {
                        telejump(OUTSIDE)
                        mes(
                            "The Fight Cave could not restore your arena, so your attempt has ended."
                        )
                        return@launch
                    }
            telejump(session.arena(FightCaveSpawn.C.localX, FightCaveSpawn.C.localZ))
            mes("Your Fight Cave attempt resumes from wave ${saved.wave}.")
            delay(2)
            spawnWave(session)
        }
    }

    public fun failAfterDeath(player: Player) {
        val session = sessions.remove(player) ?: return
        destroy(session)
        suspended.remove(player.username)
        protectedAccess.launch(player) {
            telejump(OUTSIDE)
            mes("You have been defeated in the Fight Cave.")
        }
    }

    public fun sessionFor(npc: Npc): FightCaveSession? = owners[npc]

    public fun ownerFor(npc: Npc): Player? = owners[npc]?.player

    public fun onKill(player: Player, npc: Npc) {
        val session = owners.remove(npc) ?: return
        if (session.player !== player || !session.alive.remove(npc)) {
            return
        }
        when (npc.id) {
            FightCaveNpcs.tzKekA.id,
            FightCaveNpcs.tzKekB.id -> splitTzKek(session, npc.coords)
            FightCaveNpcs.jad.id -> complete(session)
        }
        if (npc.id != FightCaveNpcs.jad.id && session.alive.isEmpty()) {
            advance(session)
        }
    }

    public fun onJadHit(npc: Npc) {
        val session = owners[npc] ?: return
        if (npc.id != FightCaveNpcs.jad.id || session.healersSpawned) {
            return
        }
        if (npc.hitpoints > npc.baseHitpointsLvl / 2) {
            return
        }
        session.healersSpawned = true
        session.jad = npc
        val points =
            listOf(FightCaveSpawn.NW, FightCaveSpawn.C, FightCaveSpawn.SE, FightCaveSpawn.SW)
        points.forEach { point ->
            spawn(session, FightCaveNpcs.healer, point.localX, point.localZ)
                .timer(timers.fight_cave_healer_heal, HEAL_RATE)
        }
    }

    public fun jadFor(healer: Npc): Npc? {
        val session = owners[healer] ?: return null
        if (healer.id != FightCaveNpcs.healer.id || sessions[session.player] !== session) {
            return null
        }
        return session.jad?.takeIf { it.hitpoints > 0 && it.slotId != INVALID_SLOT }
    }

    public fun healJad(healer: Npc) {
        val jad = jadFor(healer) ?: return
        jad.hitpoints = min(jad.baseHitpointsLvl, jad.hitpoints + HEAL_PER_HEALER)
    }

    public fun aggroHealer(healer: Npc, player: Player) {
        if (ownerFor(healer) === player) {
            healer.opPlayer2(player, interactions)
        }
    }

    private fun create(player: Player, wave: Int, rotationStart: Int): FightCaveSession? {
        val template =
            RegionTemplate.create {
                copy(ARENA_ZONE_X, ARENA_ZONE_Z, 0) {
                    zoneWidth = ARENA_ZONE_LENGTH
                    zoneLength = ARENA_ZONE_LENGTH
                    regionZoneX = 4
                    regionZoneZ = 4
                }
            }
        val region = regions.add(template) ?: return null
        return FightCaveSession(player, region, wave, rotationStart).also { sessions[player] = it }
    }

    private fun spawnWave(session: FightCaveSession) {
        if (sessions[session.player] !== session) {
            return
        }
        session.healersSpawned = false
        session.jad = null
        session.player.mes("Wave: ${session.wave}")
        for (entry in FightCaveWaves.spawns(session.wave, session.rotationStart)) {
            val type = typeFor(entry.monster, entry.ordinal)
            val npc = spawn(session, type, entry.spawn.localX, entry.spawn.localZ)
            if (entry.monster == FightCaveMonster.TzTokJad) {
                session.jad = npc
            }
        }
    }

    private fun spawn(session: FightCaveSession, type: NpcType, localX: Int, localZ: Int): Npc {
        val npc = Npc(npcTypes[type], session.arena(localX, localZ))
        npcs.add(npc, NPC_LIFETIME)
        session.alive += npc
        owners[npc] = session
        if (type != FightCaveNpcs.healer) {
            npc.opPlayer2(session.player, interactions)
        }
        return npc
    }

    private fun typeFor(monster: FightCaveMonster, ordinal: Int): NpcType =
        when (monster) {
            FightCaveMonster.TzKih ->
                if (ordinal % 2 == 0) FightCaveNpcs.tzKihA else FightCaveNpcs.tzKihB
            FightCaveMonster.TzKek ->
                if (ordinal % 2 == 0) FightCaveNpcs.tzKekA else FightCaveNpcs.tzKekB
            FightCaveMonster.TokXil ->
                if (ordinal % 2 == 0) FightCaveNpcs.tokXilA else FightCaveNpcs.tokXilB
            FightCaveMonster.YtMejKot ->
                if (ordinal % 2 == 0) FightCaveNpcs.ytMejKotA else FightCaveNpcs.ytMejKotB
            FightCaveMonster.KetZek ->
                if (ordinal % 2 == 0) FightCaveNpcs.ketZekA else FightCaveNpcs.ketZekB
            FightCaveMonster.TzTokJad -> FightCaveNpcs.jad
        }

    private fun splitTzKek(session: FightCaveSession, coords: CoordGrid) {
        val localX = coords.x - session.arena(0, 0).x
        val localZ = coords.z - session.arena(0, 0).z
        spawn(session, FightCaveNpcs.tzKekSplit, localX - 1, localZ)
        spawn(session, FightCaveNpcs.tzKekSplit, localX + 1, localZ)
    }

    private fun advance(session: FightCaveSession) {
        protectedAccess.launch(session.player) {
            delay(4)
            if (sessions[player] !== session || session.alive.isNotEmpty()) {
                return@launch
            }
            session.wave++
            spawnWave(session)
        }
    }

    private fun complete(session: FightCaveSession) {
        session.completing = true
        session.player.totalJadKills++
        val kills = session.player.totalJadKills
        protectedAccess.launch(session.player) {
            delay(3)
            if (sessions[player] !== session) {
                return@launch
            }
            sessions.remove(player)
            suspended.remove(player.username)
            destroy(session)
            telejump(OUTSIDE)
            player.invAddOrDrop(objRepo, objs.tokkul, TOKKUL_REWARD)
            player.invAddOrDrop(objRepo, objs.fire_cape)
            mes("You have completed the Fight Cave!")
            mes("Your TzTok-Jad kill count is: <col=ff0000>$kills</col>.")
            publish(
                CombatAchievementEvents.EncounterCompleted(
                    player = player,
                    encounter = "Fight Caves",
                    completionCount = kills,
                )
            )
        }
    }

    private fun destroy(session: FightCaveSession) {
        session.alive.toList().forEach { npc ->
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) {
                npcs.del(npc, Int.MAX_VALUE)
            }
        }
        session.alive.clear()
    }

    private data class SuspendedFightCave(val wave: Int, val rotationStart: Int)

    public companion object {
        public val OUTSIDE: CoordGrid = CoordGrid(2439, 5171, 0)
        private const val ARENA_ZONE_X = 296
        private const val ARENA_ZONE_Z = 632
        private const val ARENA_ZONE_LENGTH = 8
        private const val EXIT_COOLDOWN = 300
        private const val NPC_LIFETIME = 100_000
        private const val TOKKUL_REWARD = 8_032
        private const val HEAL_PER_HEALER = 5
        private const val HEAL_RATE = 4
    }
}

public data class FightCaveSession(
    val player: Player,
    val region: Region,
    var wave: Int,
    val rotationStart: Int,
    val alive: MutableSet<Npc> = java.util.Collections.newSetFromMap(IdentityHashMap()),
    var healersSpawned: Boolean = false,
    var jad: Npc? = null,
    var completing: Boolean = false,
) {
    public fun arena(localX: Int, localZ: Int): CoordGrid = region.normal[0, 37, 79, localX, localZ]
}
