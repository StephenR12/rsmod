package org.rsmod.content.minigames.tob

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.events.CombatAchievementEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

@Singleton
public class TobController
@Inject
constructor(
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val interactions: AiPlayerInteractions,
    private val protectedAccess: ProtectedAccessLauncher,
    private val objRepo: ObjRepository,
    private val locs: LocRepository,
    private val locTypes: LocTypeList,
) {
    private val sessions = IdentityHashMap<Player, TobSession>()
    private val owners = IdentityHashMap<Npc, TobSession>()
    private val suspended = mutableMapOf<String, SuspendedTob>()

    private var Player.normalCompletions by intVarp(TobVarps.normalCompletions)
    private var Player.hardCompletions by intVarp(TobVarps.hardCompletions)
    private var Player.entryCompletions by intVarp(TobVarps.entryCompletions)
    private var Player.progressHud by intVarBit(TobVarBits.progress)
    private var Player.waveTypeHud by intVarBit(TobVarBits.waveType)
    private var Player.waveValueHud by intVarBit(TobVarBits.waveValue)
    private var Player.waveMaxHud by intVarBit(TobVarBits.waveMax)
    private var Player.damageTakenHud by intVarBit(TobVarBits.damageTaken)

    public suspend fun enter(access: ProtectedAccess, mode: TobMode) {
        val player = access.player
        if (!player.members) {
            access.mes("You need to be on a members' world to enter the Theatre of Blood.")
            return
        }
        if (mode == TobMode.Hard && player.normalCompletions < 1) {
            access.mes("You must complete Normal Mode before attempting Hard Mode.")
            return
        }
        sessions.remove(player)?.let(::destroy)
        suspended.remove(player.username)
        val session =
            create(player, mode, roomIndex = 0, deaths = 0, damageTaken = 0)
                ?: run {
                    access.mes(
                        "No Theatre instance is currently available. Please try again shortly."
                    )
                    return
                }
        syncHud(session)
        access.telejump(session.entrance())
        access.mes("You enter the Theatre of Blood: ${mode.displayName}.")
        access.delay(2)
        spawnRoom(session)
    }

    public suspend fun leave(access: ProtectedAccess) {
        val session = sessions.remove(access.player) ?: return
        destroy(session)
        suspended.remove(access.player.username)
        clearHud(access.player)
        access.telejump(OUTSIDE)
        access.mes("You leave the Theatre of Blood.")
    }

    public suspend fun claimSupplies(access: ProtectedAccess) {
        val session = sessions[access.player] ?: return
        if (!session.awaitingSupplies) {
            access.mes("This supply chest has already been claimed.")
            return
        }
        val tile = session.supplyChestTile ?: return
        if (access.player.coords.chebyshevDistance(tile) > 3) return
        session.awaitingSupplies = false
        locs.add(
            tile,
            locTypes[TobLocs.midwayChestOpen],
            50,
            LocAngle.North,
            LocShape.CentrepieceStraight,
        )
        val count = if (session.mode == TobMode.Entry) 10 else 5
        access.player.invAddOrDrop(objRepo, TobObjs.bandage, count)
        access.mes("You spend your raid supply points on $count bandages.")
        access.delay(2)
        if (sessions[access.player] !== session) return
        session.roomIndex++
        access.telejump(session.entrance())
        spawnRoom(session)
    }

    public fun suspendOnLogout(player: Player) {
        val session = sessions.remove(player) ?: return
        suspended[player.username] =
            SuspendedTob(session.mode, session.roomIndex, session.deaths, session.damageTaken)
        destroy(session)
        clearHud(player)
    }

    public fun resumeOnLogin(player: Player) {
        val saved = suspended.remove(player.username) ?: return
        protectedAccess.launch(player) {
            val session =
                create(player, saved.mode, saved.roomIndex, saved.deaths, saved.damageTaken)
                    ?: run {
                        telejump(OUTSIDE)
                        mes("Your Theatre attempt could not be restored.")
                        return@launch
                    }
            syncHud(session)
            telejump(session.entrance())
            mes(
                "Your ${session.mode.displayName} Theatre attempt resumes at ${session.room.displayName}."
            )
            delay(2)
            spawnRoom(session)
        }
    }

    public fun onDeath(player: Player) {
        val session = sessions[player] ?: return
        session.deaths++
        if (session.mode != TobMode.Entry) {
            sessions.remove(player)
            destroy(session)
            clearHud(player)
            protectedAccess.launch(player) {
                delay(2)
                telejump(OUTSIDE)
                mes("With no surviving party members, your Theatre attempt ends.")
            }
            return
        }
        clearRoomNpcs(session)
        protectedAccess.launch(player) {
            delay(2)
            if (sessions[player] !== session) return@launch
            player.invAddOrDrop(objRepo, TobObjs.bandage, 3)
            statHeal(stats.hitpoints, constant = 0, percent = 100)
            statHeal(stats.prayer, constant = 0, percent = 100)
            telejump(session.entrance())
            mes("Entry Mode allows another attempt. Three bandages have been supplied.")
            delay(2)
            spawnRoom(session)
        }
    }

    public fun ownerFor(npc: Npc): Player? = owners[npc]?.player

    public fun scaledMaxHit(npc: Npc, normal: Int): Int =
        max(1, normal * (owners[npc]?.mode?.damagePercent ?: 100) / 100)

    public fun roomFor(npc: Npc): TobRoom? = owners[npc]?.room

    public fun verzikPhaseFor(npc: Npc): Int = owners[npc]?.verzikPhase ?: 0

    public fun onVerzikAttack(npc: Npc) {
        val session = owners[npc] ?: return
        if (session.verzikPhase != 3) return
        session.verzikAutoAttacks++
        if (session.verzikAutoAttacks >= 4) {
            session.verzikAutoAttacks = 0
            session.verzikSpecialReady = true
        }
    }

    public fun useVerzikPillar(npc: Npc): Boolean {
        val session = owners[npc] ?: return false
        if (session.verzikPhase != 1) return false
        val active = max(0, 4 - session.verzikPillarHits / 7)
        val protected =
            VERZIK_PILLARS.take(active).any { (x, z) ->
                session.player.coords.chebyshevDistance(session.arena(x, z)) <= 2
            }
        if (protected) {
            session.verzikPillarHits++
            if (session.verzikPillarHits % 7 == 0)
                session.player.mes("A supporting pillar crumbles under Verzik's magic!")
        }
        return protected
    }

    public fun isCombatPaused(npc: Npc): Boolean = owners[npc]?.inShadowRealm == true

    public fun onXarpusAttack(npc: Npc) {
        val session = owners[npc] ?: return
        if (session.room != TobRoom.Xarpus || session.xarpusFinal) return
        val tile = session.player.coords
        session.xarpusAcid += tile
        locs.add(
            tile,
            locTypes[TobLocs.xarpusAcid],
            XARPUS_ACID_DURATION,
            LocAngle.North,
            LocShape.CentrepieceStraight,
        )
        session.player.mes("Xarpus spits acid beneath your feet!")
    }

    public fun onSotetsegAttack(npc: Npc) {
        val session = owners[npc] ?: return
        if (session.inShadowRealm) return
        session.sotetsegAttacks++
        if (session.sotetsegAttacks % 10 != 0) return
        session.player.mes("Sotetseg launches a large red projectile!")
        val amount =
            if (session.mode == TobMode.Entry) 15 else max(15, session.player.hitpoints * 9 / 10)
        val hit = session.player.queueHit(npc, 3, HitType.Typeless, amount)
        session.damageTaken += hit.damage
        syncHud(session)
    }

    public fun requiredStyle(npc: Npc): HitType? {
        val id = npc.visType.id
        return when {
            id in MELEE_NYLOS -> HitType.Melee
            id in RANGED_NYLOS -> HitType.Ranged
            id in MAGIC_NYLOS -> HitType.Magic
            else -> null
        }
    }

    public fun recordDamage(npc: Npc, amount: Int) {
        if (amount <= 0) return
        val session = owners[npc] ?: return
        session.damageTaken += amount
        syncHud(session)
    }

    public fun onHit(npc: Npc, hit: Hit) {
        val session = owners[npc] ?: return
        if (!hit.isFromPlayer || hit.damage <= 0) return
        val required = requiredStyle(npc)
        if (required != null && hit.type != required) {
            npc.hitpoints = min(npc.baseHitpointsLvl, npc.hitpoints + hit.damage)
            session.player.mes("The Nylocas is immune to that combat style.")
            return
        }
        when (session.room) {
            TobRoom.Maiden -> if (npc === session.mainBoss) checkMaidenThresholds(session, npc)
            TobRoom.Bloat ->
                if (npc === session.mainBoss && !session.bloatResting) {
                    npc.hitpoints = min(npc.baseHitpointsLvl, npc.hitpoints + hit.damage)
                }
            TobRoom.Sotetseg -> if (npc === session.mainBoss) checkSotetsegThresholds(session, npc)
            TobRoom.Xarpus -> if (npc === session.mainBoss) checkXarpusFinal(session, npc, hit)
            TobRoom.Verzik -> if (npc === session.mainBoss) checkVerzikDamage(session, npc, hit)
            else -> Unit
        }
    }

    public fun onKill(player: Player, npc: Npc) {
        val session = owners.remove(npc) ?: return
        if (session.player !== player) return
        session.support.remove(npc)
        session.nyloAges.remove(npc)
        if (!session.alive.remove(npc)) return
        if (session.room == TobRoom.Nylocas && !session.nyloBossSpawned) {
            if (session.alive.isEmpty()) advanceNyloWave(session)
            return
        }
        if (session.room == TobRoom.Verzik && npc === session.mainBoss && session.verzikPhase < 3) {
            spawnNextVerzikPhase(session)
            return
        }
        if (npc === session.mainBoss) {
            clearSupports(session)
            finishRoom(session)
        }
    }

    public fun pulse(access: StandardNpcAccess) {
        val session = owners[access.npc] ?: return
        when (session.room) {
            TobRoom.Maiden -> if (access.npc in session.support) pulseMaidenHealer(access, session)
            TobRoom.Bloat -> pulseBloat(access, session)
            TobRoom.Nylocas ->
                if (access.npc === session.mainBoss) {
                    pulseNyloBoss(access, session)
                } else {
                    pulseNyloSmall(access, session)
                }
            TobRoom.Sotetseg -> if (access.npc === session.mainBoss) pulseSotetseg(access, session)
            TobRoom.Xarpus -> if (access.npc === session.mainBoss) pulseXarpus(access, session)
            TobRoom.Verzik ->
                if (access.npc === session.mainBoss) {
                    pulseVerzikSpecial(access, session)
                } else {
                    pulseVerzikNylo(access, session)
                }
            else -> Unit
        }
    }

    private fun create(
        player: Player,
        mode: TobMode,
        roomIndex: Int,
        deaths: Int,
        damageTaken: Int,
    ): TobSession? {
        val template =
            RegionTemplate.createLarge {
                TobArena.entries.forEach { arena ->
                    copy(arena.mapX * 8, arena.mapZ * 8, arena.level) {
                        zoneWidth = 8
                        zoneLength = 8
                        regionZoneX = arena.regionZoneX
                        regionZoneZ = arena.regionZoneZ
                    }
                }
            }
        val region = regions.add(template) ?: return null
        return TobSession(player, region, mode, roomIndex, deaths, damageTaken).also {
            sessions[player] = it
        }
    }

    private fun spawnRoom(session: TobSession) {
        if (sessions[session.player] !== session) return
        clearRoomNpcs(session)
        resetEncounterState(session)
        syncHud(session)
        session.player.mes(
            "Theatre encounter ${session.roomIndex + 1}/6: ${session.room.displayName}"
        )
        when (session.room) {
            TobRoom.Maiden ->
                session.mainBoss =
                    spawn(
                        session,
                        TobNpcs.maiden(session.mode),
                        session.arenaData.bossX,
                        session.arenaData.bossZ,
                    )
            TobRoom.Bloat ->
                session.mainBoss =
                    spawn(
                            session,
                            TobNpcs.bloat(session.mode),
                            session.arenaData.bossX,
                            session.arenaData.bossZ,
                            attack = false,
                        )
                        .also { it.timer(timers.tob_pulse, 2) }
            TobRoom.Nylocas -> spawnNyloWave(session)
            TobRoom.Sotetseg ->
                session.mainBoss =
                    spawn(
                            session,
                            TobNpcs.sotetseg(session.mode),
                            session.arenaData.bossX,
                            session.arenaData.bossZ,
                        )
                        .also { it.timer(timers.tob_pulse, 1) }
            TobRoom.Xarpus ->
                session.mainBoss =
                    spawn(
                            session,
                            TobNpcs.xarpus(session.mode),
                            session.arenaData.bossX,
                            session.arenaData.bossZ,
                        )
                        .also { it.timer(timers.tob_pulse, 4) }
            TobRoom.Verzik -> spawnVerzikPhase(session, 1)
        }
    }

    private fun spawn(
        session: TobSession,
        type: NpcType,
        localX: Int,
        localZ: Int,
        support: Boolean = false,
        attack: Boolean = true,
    ): Npc {
        val npc = Npc(npcTypes[type], session.arena(localX, localZ))
        npcs.add(npc, NPC_LIFETIME)
        owners[npc] = session
        if (support) session.support += npc else session.alive += npc
        if (attack) npc.opPlayer2(session.player, interactions)
        return npc
    }

    private fun checkMaidenThresholds(session: TobSession, maiden: Npc) {
        val pct = maiden.hitpoints * 100 / max(1, maiden.baseHitpointsLvl)
        val crossed = session.maidenThresholds.filter { pct <= it }.sortedDescending()
        crossed.forEach { threshold ->
            session.maidenThresholds.remove(threshold)
            session.player.mes("The Maiden summons Nylocas Matomenos at $threshold% health!")
            val healerType = TobNpcs.maidenHealer(session.mode)
            spawn(session, healerType, 24, 37, support = true).timer(timers.tob_pulse, 4)
            spawn(session, healerType, 38, 37, support = true).timer(timers.tob_pulse, 4)
        }
    }

    private fun pulseMaidenHealer(access: StandardNpcAccess, session: TobSession) {
        val maiden = session.mainBoss ?: return
        if (maiden.hitpoints > 0) {
            access.walk(maiden.coords)
            if (access.isWithinDistance(maiden, 2)) {
                maiden.hitpoints =
                    min(
                        maiden.baseHitpointsLvl,
                        maiden.hitpoints + max(1, access.npc.hitpoints * 2),
                    )
                if (access.npc.slotId != INVALID_SLOT) npcs.del(access.npc, Int.MAX_VALUE)
                owners.remove(access.npc)
                session.support.remove(access.npc)
                return
            }
        }
        access.timer(timers.tob_pulse, 2)
    }

    private fun pulseBloat(access: StandardNpcAccess, session: TobSession) {
        session.specialCounter = (session.specialCounter + 1) % 16
        when (session.specialCounter) {
            0 -> {
                session.bloatResting = false
                access.anim(TobSeqs.bloatStomp)
                if (access.isWithinDistance(session.player, 2)) damage(session, access.npc, 45, 1)
                session.player.mes("Bloat lurches upright—get behind the central pillar!")
            }
            in 1..9 -> {
                access.anim(TobSeqs.bloatWalk)
                if (session.specialCounter % 2 == 1) {
                    session.bloatWaypoint = (session.bloatWaypoint + 1) % BLOAT_ROUTE.size
                    val (x, z) = BLOAT_ROUTE[session.bloatWaypoint]
                    access.walk(session.arena(x, z))
                }
                if (session.specialCounter % 3 == 0 && !isBloatPillarSafe(session, access.npc)) {
                    damage(session, access.npc, 24, 1, HitType.Ranged)
                }
                if (session.specialCounter == 7) fallingBloatLimb(session, access.npc)
            }
            10 -> {
                session.bloatResting = true
                access.anim(TobSeqs.bloatSleep)
                session.player.mes("Pestilent Bloat falls to its knees. Now is the time to attack!")
            }
        }
        access.timer(timers.tob_pulse, 1)
    }

    private fun isBloatPillarSafe(session: TobSession, bloat: Npc): Boolean {
        val centre = session.arena(31, 33)
        fun side(coords: CoordGrid): Int {
            val dx = coords.x - centre.x
            val dz = coords.z - centre.z
            return if (abs(dx) > abs(dz)) {
                if (dx < 0) 0 else 2
            } else {
                if (dz < 0) 1 else 3
            }
        }
        return abs(side(session.player.coords) - side(bloat.coords)) == 2
    }

    private fun fallingBloatLimb(session: TobSession, bloat: Npc) {
        val target = session.player.coords
        session.player.mes("A rotten limb begins to fall above you—move!")
        protectedAccess.launch(session.player) {
            delay(2)
            if (sessions[player] !== session || session.room != TobRoom.Bloat) return@launch
            if (player.coords.chebyshevDistance(target) <= 1) {
                val hit = player.queueHit(bloat, 1, HitType.Typeless, scaledMaxHit(bloat, 35))
                session.damageTaken += hit.damage
                syncHud(session)
            }
        }
    }

    private fun spawnNyloWave(session: TobSession) {
        session.nyloWave++
        session.player.waveTypeHud = 1
        session.player.waveValueHud = session.nyloWave
        session.player.waveMaxHud = NYLO_WAVES
        val types =
            listOf(
                TobNpcs.nyloMelee(session.mode),
                TobNpcs.nyloRanged(session.mode),
                TobNpcs.nyloMagic(session.mode),
            )
        val count = min(6, 3 + session.nyloWave / 8)
        repeat(count) { index ->
            val (x, z) = NYLO_SPAWNS[(session.nyloWave + index) % NYLO_SPAWNS.size]
            spawn(session, types[(session.nyloWave + index) % 3], x, z, attack = false).also {
                session.nyloAges[it] = 0
                it.timer(timers.tob_pulse, 1)
            }
        }
    }

    private fun pulseNyloSmall(access: StandardNpcAccess, session: TobSession) {
        if (access.npc !in session.alive) return
        val age = (session.nyloAges[access.npc] ?: 0) + 1
        session.nyloAges[access.npc] = age
        val target = NYLO_PILLARS[Math.floorMod(access.npc.slotId, NYLO_PILLARS.size)]
        val pillar = session.arena(target.first, target.second)
        access.walk(pillar)
        if (access.distanceTo(pillar) <= 1 && age % 2 == 0) {
            session.nyloPillarHealth = max(0, session.nyloPillarHealth - 8)
            if (session.nyloPillarHealth % 40 == 0) {
                session.player.mes(
                    "The Nylocas pillars have ${session.nyloPillarHealth}/400 integrity remaining."
                )
            }
            if (session.nyloPillarHealth == 0) {
                session.player.mes("The supporting pillars collapse!")
                session.player.queueHit(access.npc, 1, HitType.Typeless, session.player.hitpoints)
                return
            }
        }
        if (age >= 24) {
            if (access.isWithinDistance(session.player, 2)) damage(session, access.npc, 32, 1)
            owners.remove(access.npc)
            session.nyloAges.remove(access.npc)
            session.alive.remove(access.npc)
            if (access.npc.slotId != INVALID_SLOT) npcs.del(access.npc, Int.MAX_VALUE)
            if (session.alive.isEmpty()) advanceNyloWave(session)
            return
        }
        access.timer(timers.tob_pulse, 1)
    }

    private fun advanceNyloWave(session: TobSession) {
        protectedAccess.launch(session.player) {
            delay(2)
            if (sessions[player] !== session) return@launch
            if (session.nyloWave < NYLO_WAVES) {
                spawnNyloWave(session)
            } else {
                session.nyloBossSpawned = true
                session.player.waveTypeHud = 2
                session.mainBoss =
                    spawn(session, TobNpcs.nyloBossMelee(session.mode), 31, 39).also {
                        it.timer(timers.tob_pulse, 10)
                    }
                mes("Nylocas Vasilias descends into the arena!")
            }
        }
    }

    private fun pulseNyloBoss(access: StandardNpcAccess, session: TobSession) {
        session.nyloForm = (session.nyloForm + 1 + access.random.of(0..1)) % 3
        val type =
            when (session.nyloForm) {
                0 -> TobNpcs.nyloBossMelee(session.mode)
                1 -> TobNpcs.nyloBossRanged(session.mode)
                else -> TobNpcs.nyloBossMagic(session.mode)
            }
        access.changeType(type, Int.MAX_VALUE, npcTypes)
        access.npc.opPlayer2(session.player, interactions)
        session.player.mes("Nylocas Vasilias changes combat form.")
        access.timer(timers.tob_pulse, 10)
    }

    private fun checkSotetsegThresholds(session: TobSession, sotetseg: Npc) {
        val pct = sotetseg.hitpoints * 100 / max(1, sotetseg.baseHitpointsLvl)
        val threshold = session.sotetsegThresholds.filter { pct <= it }.maxOrNull() ?: return
        if (session.inShadowRealm) return
        session.sotetsegThresholds.remove(threshold)
        session.inShadowRealm = true
        session.shadowPath = createShadowPath(session)
        session.shadowProgress = 0
        session.shadowLastTile = session.shadowPath.first()
        session.shadowPath.forEach { tile ->
            locs.add(
                tile,
                locTypes[TobLocs.sotetsegPath],
                SHADOW_PATH_DURATION,
                LocAngle.North,
                LocShape.CentrepieceStraight,
            )
        }
        protectedAccess.launch(session.player) {
            mes("Sotetseg drags you into the shadow realm. Follow every glowing red tile!")
            telejump(session.shadowPath.first())
        }
    }

    private fun createShadowPath(session: TobSession): List<CoordGrid> {
        val local = mutableListOf<Pair<Int, Int>>()
        var x = 11
        var z = 24
        local += x to z
        repeat(7) {
            z++
            local += x to z
        }
        repeat(5) {
            x++
            local += x to z
        }
        repeat(5) {
            z++
            local += x to z
        }
        repeat(5) {
            x++
            local += x to z
        }
        return local.map { (lx, lz) -> session.arena(lx, lz) }
    }

    private fun pulseSotetseg(access: StandardNpcAccess, session: TobSession) {
        if (session.inShadowRealm) {
            val tile = session.player.coords
            if (tile != session.shadowLastTile) {
                session.shadowLastTile = tile
                val nextIndex = session.shadowPath.indexOf(tile)
                if (nextIndex == session.shadowProgress + 1) {
                    session.shadowProgress = nextIndex
                } else if (
                    nextIndex < session.shadowProgress || nextIndex > session.shadowProgress + 1
                ) {
                    val amount = max(1, session.player.hitpoints / 15 + 15)
                    val hit = session.player.queueHit(access.npc, 1, HitType.Typeless, amount)
                    session.damageTaken += hit.damage
                    syncHud(session)
                    session.player.mes("You step away from the revealed path!")
                }
            }
            if (session.shadowProgress == session.shadowPath.lastIndex) {
                session.inShadowRealm = false
                session.shadowPath = emptyList()
                protectedAccess.launch(session.player) {
                    telejump(session.entrance())
                    mes("You escape the shadow realm through the portal.")
                }
                access.npc.opPlayer2(session.player, interactions)
            }
        }
        access.timer(timers.tob_pulse, 1)
    }

    private fun pulseXarpus(access: StandardNpcAccess, session: TobSession) {
        session.specialCounter++
        if (session.player.coords in session.xarpusAcid) {
            damage(session, access.npc, 8, 1)
        }
        if (!session.xarpusFinal && session.xarpusExhumes < 4 && session.specialCounter % 4 == 0) {
            session.xarpusExhumes++
            access.anim(TobSeqs.xarpusAbsorb)
            access.npc.hitpoints =
                min(
                    access.npc.baseHitpointsLvl,
                    access.npc.hitpoints + max(1, access.npc.baseHitpointsLvl / 20),
                )
            session.player.mes("Xarpus absorbs an exhumed skeleton and recovers health.")
        }
        if (session.xarpusFinal && session.specialCounter % 8 == 0) {
            session.xarpusGaze = (session.xarpusGaze + 1) % 4
            session.player.mes(
                "Xarpus turns to watch the ${QUADRANT_NAMES[session.xarpusGaze]} quadrant."
            )
        }
        access.timer(timers.tob_pulse, 1)
    }

    private fun checkXarpusFinal(session: TobSession, xarpus: Npc, hit: Hit) {
        val pct = xarpus.hitpoints * 100 / max(1, xarpus.baseHitpointsLvl)
        if (pct <= 22) {
            if (!session.xarpusFinal) {
                session.player.mes("Xarpus screeches and begins watching the arena quadrants!")
                session.xarpusFinal = true
                session.xarpusGaze = quadrant(session, session.player.coords)
            }
            if (quadrant(session, session.player.coords) == session.xarpusGaze) {
                val reflected =
                    session.player.queueHit(xarpus, 1, HitType.Typeless, max(1, hit.damage))
                session.damageTaken += reflected.damage
                syncHud(session)
            }
        }
    }

    private fun checkVerzikDamage(session: TobSession, verzik: Npc, hit: Hit) {
        if (session.verzikPhase != 1 || hit.isRighthandObj(TobObjs.dawnbringer)) return
        val cap = if (hit.type == HitType.Melee) 10 else 3
        if (hit.damage > cap) {
            verzik.hitpoints = min(verzik.baseHitpointsLvl, verzik.hitpoints + hit.damage - cap)
            session.player.mes("Only the Dawnbringer can pierce Verzik's phase-one protection.")
        }
    }

    private fun quadrant(session: TobSession, coords: CoordGrid): Int {
        val centre = session.arena(session.arenaData.bossX, session.arenaData.bossZ)
        return when {
            coords.x < centre.x && coords.z >= centre.z -> 0
            coords.x >= centre.x && coords.z >= centre.z -> 1
            coords.x >= centre.x -> 2
            else -> 3
        }
    }

    private fun spawnVerzikPhase(session: TobSession, phase: Int) {
        session.verzikPhase = phase
        session.specialCounter = 0
        session.verzikAutoAttacks = 0
        session.verzikSpecialReady = false
        val type =
            when (phase) {
                1 -> TobNpcs.verzik1(session.mode)
                2 -> TobNpcs.verzik2(session.mode)
                else -> TobNpcs.verzik3(session.mode)
            }
        session.mainBoss =
            spawn(session, type, 31, 42, attack = false).also { it.timer(timers.tob_pulse, 3) }
        session.player.mes("Verzik Vitur: phase $phase.")
    }

    private fun spawnNextVerzikPhase(session: TobSession) {
        protectedAccess.launch(session.player) {
            delay(4)
            if (sessions[player] === session) spawnVerzikPhase(session, session.verzikPhase + 1)
        }
    }

    private fun pulseVerzikSpecial(access: StandardNpcAccess, session: TobSession) {
        session.specialCounter++
        when (session.verzikPhase) {
            2 -> {
                if (session.specialCounter % 4 == 0) verzikUrnBomb(session, access.npc)
                if (session.specialCounter % 6 == 0) spawnVerzikPhaseTwoNylos(session)
                if (
                    access.npc.hitpoints <= access.npc.baseHitpointsLvl * 35 / 100 &&
                        session.specialCounter % 5 == 0
                ) {
                    access.npc.hitpoints =
                        min(
                            access.npc.baseHitpointsLvl,
                            access.npc.hitpoints + access.npc.baseHitpointsLvl / 50,
                        )
                    session.player.mes("Verzik draws blood from the arena and restores health.")
                }
            }
            3 -> {
                if (session.verzikSpecialReady) {
                    session.verzikSpecialReady = false
                    performVerzikPhaseThreeSpecial(access, session)
                }
                if (access.npc.hitpoints <= access.npc.baseHitpointsLvl / 5)
                    pulseVerzikTornado(access, session)
            }
        }
        access.timer(
            timers.tob_pulse,
            if (session.verzikPhase == 3 && access.npc.hitpoints <= access.npc.baseHitpointsLvl / 5)
                3
            else 4,
        )
    }

    private fun verzikUrnBomb(session: TobSession, verzik: Npc) {
        val target = session.player.coords
        session.player.mes("Verzik hurls an explosive urn at your tile—move!")
        protectedAccess.launch(session.player) {
            delay(3)
            if (sessions[player] !== session || session.verzikPhase != 2) return@launch
            if (player.coords.chebyshevDistance(target) <= 1) {
                val hit = player.queueHit(verzik, 1, HitType.Magic, scaledMaxHit(verzik, 44))
                session.damageTaken += hit.damage
                syncHud(session)
            }
        }
    }

    private fun spawnVerzikPhaseTwoNylos(session: TobSession) {
        spawn(
                session,
                TobNpcs.verzikArmouredNylo(session.mode),
                23,
                32,
                support = true,
                attack = false,
            )
            .timer(timers.tob_pulse, 1)
        spawn(
                session,
                TobNpcs.verzikBloodNylo(session.mode),
                39,
                32,
                support = true,
                attack = false,
            )
            .timer(timers.tob_pulse, 1)
        session.player.mes("Armoured and blood Nylocas crawl into the arena.")
    }

    private fun pulseVerzikNylo(access: StandardNpcAccess, session: TobSession) {
        val blood = access.npc.visType.id in VERZIK_BLOOD_NYLOS
        val target = if (blood) session.mainBoss?.coords else session.player.coords
        if (target == null) return
        access.walk(target)
        if (access.distanceTo(target) <= 1) {
            if (blood) {
                session.mainBoss?.let {
                    it.hitpoints = min(it.baseHitpointsLvl, it.hitpoints + it.baseHitpointsLvl / 12)
                }
                session.player.mes("A blood Nylocas reaches Verzik and heals her!")
            } else {
                damage(session, access.npc, 34, 1)
            }
            owners.remove(access.npc)
            session.support.remove(access.npc)
            if (access.npc.slotId != INVALID_SLOT) npcs.del(access.npc, Int.MAX_VALUE)
            return
        }
        access.timer(timers.tob_pulse, 1)
    }

    private fun performVerzikPhaseThreeSpecial(access: StandardNpcAccess, session: TobSession) {
        when (session.verzikSpecialIndex++ % 4) {
            0 -> {
                val types =
                    listOf(
                        TobNpcs.verzikNyloMelee(session.mode),
                        TobNpcs.verzikNyloRanged(session.mode),
                        TobNpcs.verzikNyloMagic(session.mode),
                    )
                types.forEachIndexed { index, type ->
                    spawn(session, type, 25 + index * 6, 29, support = true)
                }
                session.player.mes("Verzik summons a trio of Nylocas!")
            }
            1 -> {
                access.anim(TobSeqs.verzik3Web)
                val origin = session.player.coords
                session.player.mes("Webs close around you—keep moving!")
                protectedAccess.launch(session.player) {
                    delay(3)
                    if (
                        sessions[player] === session && player.coords.chebyshevDistance(origin) < 3
                    ) {
                        val hit =
                            player.queueHit(
                                access.npc,
                                1,
                                HitType.Typeless,
                                scaledMaxHit(access.npc, 48),
                            )
                        session.damageTaken += hit.damage
                        syncHud(session)
                    }
                }
            }
            2 -> {
                access.anim(TobSeqs.verzik3Power)
                val safe =
                    session.arena(
                        VERZIK_SAFE_TILES[session.specialCounter % VERZIK_SAFE_TILES.size].first,
                        VERZIK_SAFE_TILES[session.specialCounter % VERZIK_SAFE_TILES.size].second,
                    )
                locs.add(
                    safe,
                    locTypes[TobLocs.sotetsegPath],
                    6,
                    LocAngle.North,
                    LocShape.CentrepieceStraight,
                )
                session.player.mes("A glowing safe tile appears—stand on it!")
                protectedAccess.launch(session.player) {
                    delay(3)
                    if (sessions[player] === session && player.coords.chebyshevDistance(safe) > 1) {
                        val hit =
                            player.queueHit(
                                access.npc,
                                1,
                                HitType.Typeless,
                                scaledMaxHit(access.npc, 64),
                            )
                        session.damageTaken += hit.damage
                        syncHud(session)
                    }
                }
            }
            else -> {
                session.player.mes("A green projectile crashes into you.")
                damage(session, access.npc, if (session.mode == TobMode.Entry) 45 else 74, 3)
            }
        }
    }

    private fun pulseVerzikTornado(access: StandardNpcAccess, session: TobSession) {
        val previous = session.verzikLastTile
        if (previous != null && previous == session.player.coords) {
            val amount = scaledMaxHit(access.npc, 18)
            val hit = session.player.queueHit(access.npc, 1, HitType.Typeless, amount)
            session.damageTaken += hit.damage
            access.npc.hitpoints =
                min(access.npc.baseHitpointsLvl, access.npc.hitpoints + hit.damage)
            syncHud(session)
            session.player.mes("The tornado catches you and restores Verzik's health!")
        }
        session.verzikLastTile = session.player.coords
    }

    private fun finishRoom(session: TobSession) {
        if (session.room == TobRoom.Verzik) {
            complete(session)
            return
        }
        if (session.room == TobRoom.Bloat || session.room == TobRoom.Sotetseg) {
            val (x, z) = if (session.room == TobRoom.Bloat) 31 to 21 else 16 to 16
            val tile = session.arena(x, z)
            session.supplyChestTile = tile
            session.awaitingSupplies = true
            locs.add(
                tile,
                locTypes[TobLocs.midwayChest],
                SUPPLY_CHEST_DURATION,
                LocAngle.North,
                LocShape.CentrepieceStraight,
            )
            session.player.mes(
                "A midway supply chest appears near the entrance. Claim supplies to continue."
            )
            return
        }
        if (session.room == TobRoom.Xarpus)
            session.player.invAddOrDrop(objRepo, TobObjs.dawnbringer, 1)
        protectedAccess.launch(session.player) {
            delay(4)
            if (sessions[player] !== session) return@launch
            session.roomIndex++
            telejump(session.entrance())
            spawnRoom(session)
        }
    }

    private fun complete(session: TobSession) {
        when (session.mode) {
            TobMode.Entry -> session.player.entryCompletions++
            TobMode.Normal -> session.player.normalCompletions++
            TobMode.Hard -> session.player.hardCompletions++
        }
        val unique = rollUnique(session.mode)
        val reward = unique ?: commonReward(session)
        val commonScale = TobRules.commonLootPercent(session.mode)
        val count =
            if (unique != null) 1 else max(1, (10 - min(8, session.deaths)) * commonScale / 20)
        protectedAccess.launch(session.player) {
            delay(5)
            if (sessions[player] !== session) return@launch
            sessions.remove(player)
            destroy(session)
            clearHud(player)
            telejump(OUTSIDE)
            player.invAddOrDrop(objRepo, reward, count)
            mes(
                "Theatre of Blood complete: ${session.mode.displayName}, ${session.deaths} death(s)."
            )
            if (unique != null)
                mes("<col=ff0000>You find a unique reward in the Monumental chest!</col>")
            val completions =
                when (session.mode) {
                    TobMode.Entry -> player.entryCompletions
                    TobMode.Normal -> player.normalCompletions
                    TobMode.Hard -> player.hardCompletions
                }
            publish(
                CombatAchievementEvents.EncounterCompleted(
                    player = player,
                    encounter = "Theatre of Blood",
                    completionCount = completions,
                    mode = session.mode.displayName,
                    deaths = session.deaths,
                )
            )
        }
    }

    private fun rollUnique(mode: TobMode): ObjType? {
        val tenths = mode.uniqueDenominatorTenths ?: return null
        if (Random.nextInt(tenths) >= 10) return null
        return UNIQUES[Random.nextInt(UNIQUES.size)]
    }

    private fun commonReward(session: TobSession): ObjType =
        when (Math.floorMod(session.damageTaken + session.deaths, 3)) {
            0 -> objs.vial_of_blood
            1 -> objs.runite_ore
            else -> objs.coal
        }

    private fun damage(
        session: TobSession,
        source: Npc,
        maxHit: Int,
        delay: Int,
        type: HitType = HitType.Typeless,
    ) {
        val amount = Random.nextInt(scaledMaxHit(source, maxHit) + 1)
        session.player.queueHit(source, delay, type, amount)
        session.damageTaken += amount
        syncHud(session)
    }

    private fun clearSupports(session: TobSession) {
        session.support.toList().forEach { npc ->
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        session.support.clear()
    }

    private fun clearRoomNpcs(session: TobSession) {
        (session.alive + session.support).toList().forEach { npc ->
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        session.alive.clear()
        session.support.clear()
        session.mainBoss = null
    }

    private fun destroy(session: TobSession) = clearRoomNpcs(session)

    private fun resetEncounterState(session: TobSession) {
        session.specialCounter = 0
        session.bloatResting = false
        session.maidenThresholds.clear()
        session.maidenThresholds += listOf(70, 50, 30)
        session.sotetsegThresholds.clear()
        session.sotetsegThresholds += listOf(66, 33)
        session.inShadowRealm = false
        session.shadowPath = emptyList()
        session.shadowProgress = 0
        session.shadowLastTile = null
        session.sotetsegAttacks = 0
        session.nyloWave = 0
        session.nyloBossSpawned = false
        session.nyloForm = 0
        session.nyloAges.clear()
        session.nyloPillarHealth = 400
        session.xarpusExhumes = 0
        session.xarpusFinal = false
        session.xarpusAcid.clear()
        session.xarpusGaze = 0
        session.verzikPhase = 0
        session.verzikPillarHits = 0
        session.verzikAutoAttacks = 0
        session.verzikSpecialReady = false
        session.verzikSpecialIndex = 0
        session.verzikLastTile = null
        session.awaitingSupplies = false
        session.supplyChestTile = null
    }

    private fun syncHud(session: TobSession) {
        session.player.progressHud = session.roomIndex
        session.player.damageTakenHud = session.damageTaken.coerceAtMost(16_383)
        if (session.room != TobRoom.Nylocas) {
            session.player.waveTypeHud = 0
            session.player.waveValueHud = 0
            session.player.waveMaxHud = 0
        }
    }

    private fun clearHud(player: Player) {
        player.progressHud = 0
        player.waveTypeHud = 0
        player.waveValueHud = 0
        player.waveMaxHud = 0
        player.damageTakenHud = 0
    }

    public companion object {
        public val OUTSIDE: CoordGrid = CoordGrid(3678, 3216, 0)
        private const val NPC_LIFETIME = 200_000
        private const val SHADOW_PATH_DURATION = 500
        private const val XARPUS_ACID_DURATION = 500
        private const val SUPPLY_CHEST_DURATION = 1_000
        private const val NYLO_WAVES = 31
        private val BLOAT_ROUTE = listOf(24 to 25, 38 to 25, 38 to 41, 24 to 41)
        private val NYLO_SPAWNS = listOf(19 to 40, 31 to 51, 43 to 40, 31 to 34)
        private val NYLO_PILLARS = listOf(27 to 38, 35 to 38, 27 to 44, 35 to 44)
        private val VERZIK_PILLARS = listOf(23 to 26, 39 to 26, 23 to 37, 39 to 37)
        private val VERZIK_SAFE_TILES = listOf(26 to 31, 31 to 31, 36 to 31)
        private val QUADRANT_NAMES = listOf("north-west", "north-east", "south-east", "south-west")
        private val UNIQUES =
            listOf(
                TobObjs.avernicHilt,
                TobObjs.rapier,
                TobObjs.sanguinesti,
                TobObjs.scythe,
                TobObjs.justiciarHelm,
                TobObjs.justiciarBody,
                TobObjs.justiciarLegs,
            )
        private val MELEE_NYLOS =
            setOf(
                TobNpcs.nyloMelee.id,
                TobNpcs.nyloMeleeEntry.id,
                TobNpcs.nyloMeleeHard.id,
                TobNpcs.nyloBossMelee.id,
                TobNpcs.nyloBossMeleeEntry.id,
                TobNpcs.nyloBossMeleeHard.id,
                TobNpcs.verzikNyloMelee.id,
                TobNpcs.verzikNyloMeleeEntry.id,
                TobNpcs.verzikNyloMeleeHard.id,
            )
        private val RANGED_NYLOS =
            setOf(
                TobNpcs.nyloRanged.id,
                TobNpcs.nyloRangedEntry.id,
                TobNpcs.nyloRangedHard.id,
                TobNpcs.nyloBossRanged.id,
                TobNpcs.nyloBossRangedEntry.id,
                TobNpcs.nyloBossRangedHard.id,
                TobNpcs.verzikNyloRanged.id,
                TobNpcs.verzikNyloRangedEntry.id,
                TobNpcs.verzikNyloRangedHard.id,
            )
        private val MAGIC_NYLOS =
            setOf(
                TobNpcs.nyloMagic.id,
                TobNpcs.nyloMagicEntry.id,
                TobNpcs.nyloMagicHard.id,
                TobNpcs.nyloBossMagic.id,
                TobNpcs.nyloBossMagicEntry.id,
                TobNpcs.nyloBossMagicHard.id,
                TobNpcs.verzikNyloMagic.id,
                TobNpcs.verzikNyloMagicEntry.id,
                TobNpcs.verzikNyloMagicHard.id,
            )
        private val VERZIK_BLOOD_NYLOS =
            setOf(
                TobNpcs.verzikBloodNylo.id,
                TobNpcs.verzikBloodNyloEntry.id,
                TobNpcs.verzikBloodNyloHard.id,
            )
    }
}

public data class TobSession(
    val player: Player,
    val region: Region,
    val mode: TobMode,
    var roomIndex: Int,
    var deaths: Int,
    var damageTaken: Int,
    val alive: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val support: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val maidenThresholds: MutableSet<Int> = mutableSetOf(70, 50, 30),
    val sotetsegThresholds: MutableSet<Int> = mutableSetOf(66, 33),
    var mainBoss: Npc? = null,
    var specialCounter: Int = 0,
    var bloatResting: Boolean = false,
    var nyloWave: Int = 0,
    var nyloBossSpawned: Boolean = false,
    var nyloForm: Int = 0,
    val nyloAges: MutableMap<Npc, Int> = IdentityHashMap(),
    var nyloPillarHealth: Int = 400,
    var xarpusExhumes: Int = 0,
    var xarpusFinal: Boolean = false,
    val xarpusAcid: MutableSet<CoordGrid> = mutableSetOf(),
    var xarpusGaze: Int = 0,
    var verzikPhase: Int = 0,
    var verzikPillarHits: Int = 0,
    var verzikAutoAttacks: Int = 0,
    var verzikSpecialReady: Boolean = false,
    var verzikSpecialIndex: Int = 0,
    var bloatWaypoint: Int = 0,
    var inShadowRealm: Boolean = false,
    var shadowPath: List<CoordGrid> = emptyList(),
    var shadowProgress: Int = 0,
    var shadowLastTile: CoordGrid? = null,
    var sotetsegAttacks: Int = 0,
    var verzikLastTile: CoordGrid? = null,
    var awaitingSupplies: Boolean = false,
    var supplyChestTile: CoordGrid? = null,
) {
    public val room: TobRoom
        get() = TobRules.route[roomIndex]

    internal val arenaData: TobArena
        get() = TobArena.forRoom(room)

    public fun arena(localX: Int, localZ: Int): CoordGrid =
        region.normal[arenaData.level, arenaData.mapX, arenaData.mapZ, localX, localZ]

    public fun entrance(): CoordGrid = arena(arenaData.entryX, arenaData.entryZ)
}

internal enum class TobArena(
    val room: TobRoom,
    val mapX: Int,
    val mapZ: Int,
    val level: Int,
    val regionZoneX: Int,
    val regionZoneZ: Int,
    val entryX: Int,
    val entryZ: Int,
    val bossX: Int,
    val bossZ: Int,
) {
    Maiden(TobRoom.Maiden, 49, 69, 0, 0, 0, 54, 31, 18, 31),
    Bloat(TobRoom.Bloat, 51, 69, 0, 8, 0, 31, 20, 31, 38),
    Nylocas(TobRoom.Nylocas, 51, 66, 0, 16, 0, 31, 24, 31, 40),
    Sotetseg(TobRoom.Sotetseg, 51, 67, 0, 24, 0, 16, 15, 16, 39),
    Xarpus(TobRoom.Xarpus, 49, 68, 1, 0, 8, 34, 24, 34, 37),
    Verzik(TobRoom.Verzik, 49, 67, 0, 8, 8, 31, 13, 31, 42);

    companion object {
        fun forRoom(room: TobRoom): TobArena = entries.first { it.room == room }
    }
}

private data class SuspendedTob(
    val mode: TobMode,
    val roomIndex: Int,
    val deaths: Int,
    val damageTaken: Int,
)
