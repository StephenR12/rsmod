package org.rsmod.content.minigames.inferno

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.timers
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.events.CombatAchievementEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
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
public class InfernoController
@Inject
constructor(
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val interactions: AiPlayerInteractions,
    private val protectedAccess: ProtectedAccessLauncher,
    private val objRepo: ObjRepository,
) {
    private val sessions = IdentityHashMap<Player, InfernoSession>()
    private val owners = IdentityHashMap<Npc, InfernoSession>()
    private val suspended = mutableMapOf<String, Int>()
    private var Player.totalZukKills by intVarp(InfernoVarps.totalZukKills)
    private var Player.sacrificedFireCape by boolVarBit(InfernoVarBits.sacrificedFireCape)
    private var Player.playerInInferno by boolVarBit(InfernoVarBits.playerInInferno)
    private var Player.pillar1Health by intVarBit(InfernoVarBits.pillar1Health)
    private var Player.pillar2Health by intVarBit(InfernoVarBits.pillar2Health)
    private var Player.pillar3Health by intVarBit(InfernoVarBits.pillar3Health)
    private var Player.zukHealth by intVarBit(InfernoVarBits.zukHealth)
    private var Player.zukBaseHealth by intVarBit(InfernoVarBits.zukBaseHealth)

    public fun hasUnlocked(player: Player): Boolean = player.sacrificedFireCape

    public fun sacrificeCape(access: ProtectedAccess): Boolean {
        if (access.invDel(access.inv, objs.fire_cape, count = 1).completed() != 1) {
            access.mes("You need a fire cape to sacrifice before entering the Inferno.")
            return false
        }
        access.player.sacrificedFireCape = true
        access.mes("You sacrifice your fire cape. The Inferno is now permanently unlocked.")
        return true
    }

    public suspend fun enter(access: ProtectedAccess) {
        val player = access.player
        if (!player.members) {
            access.mes("You need to be on a members' world to enter the Inferno.")
            return
        }
        if (!hasUnlocked(player)) {
            access.mes("You must sacrifice a fire cape before entering the Inferno.")
            return
        }
        sessions.remove(player)?.let(::destroy)
        suspended.remove(player.username)
        val session =
            create(player, 1)
                ?: run {
                    access.mes("The Inferno is currently full. Please try again shortly.")
                    return
                }
        initialiseHud(session)
        access.telejump(session.arena(29, 17))
        access.mes("The heat of the Inferno closes in around you.")
        access.delay(2)
        spawnWave(session)
    }

    public suspend fun leave(access: ProtectedAccess) {
        val session = sessions.remove(access.player) ?: return
        destroy(session)
        suspended.remove(access.player.username)
        clearHud(access.player)
        access.telejump(OUTSIDE)
        access.mes("You leave the Inferno.")
    }

    public fun suspendOnLogout(player: Player) {
        val session = sessions.remove(player) ?: return
        suspended[player.username] = session.wave
        destroy(session)
        clearHud(player)
    }

    public fun resumeOnLogin(player: Player) {
        val wave = suspended.remove(player.username) ?: return
        protectedAccess.launch(player) {
            val session =
                create(player, wave)
                    ?: run {
                        telejump(OUTSIDE)
                        mes("Your Inferno attempt could not be restored.")
                        return@launch
                    }
            initialiseHud(session)
            telejump(session.arena(29, 17))
            mes("Your Inferno attempt resumes from wave $wave.")
            delay(2)
            spawnWave(session)
        }
    }

    public fun failAfterDeath(player: Player) {
        val session = sessions.remove(player) ?: return
        val reward = consolationTokkul(session.wave)
        destroy(session)
        suspended.remove(player.username)
        clearHud(player)
        protectedAccess.launch(player) {
            telejump(OUTSIDE)
            if (reward > 0) player.invAddOrDrop(objRepo, objs.tokkul, reward)
            mes("You have been defeated on wave ${session.wave} of the Inferno.")
            if (reward > 0) mes("You receive $reward Tokkul for your attempt.")
        }
    }

    public fun ownerFor(npc: Npc): Player? = owners[npc]?.player

    public fun onKill(player: Player, npc: Npc) {
        val session = owners.remove(npc) ?: return
        if (session.player !== player || !session.alive.remove(npc)) return
        when (npc.id) {
            InfernoNpcs.blob.id -> splitBlob(session, npc.coords)
            InfernoNpcs.jad.id,
            InfernoNpcs.finalJad.id -> removeJadHealers(session, npc)
            InfernoNpcs.zuk.id -> complete(session)
            else -> rememberDeath(session, npc)
        }
        if (npc.id != InfernoNpcs.zuk.id && session.alive.isEmpty()) advance(session)
    }

    public fun onNpcHit(npc: Npc) {
        val session = owners[npc] ?: return
        when (npc.id) {
            InfernoNpcs.jad.id,
            InfernoNpcs.finalJad.id -> maybeSpawnJadHealers(session, npc)
            InfernoNpcs.zuk.id -> updateZukPhases(session, npc)
            InfernoNpcs.zukHealer.id -> {
                session.tagged += npc
                npc.opPlayer2(session.player, interactions)
            }
            InfernoNpcs.finalRanger.id,
            InfernoNpcs.finalMager.id -> session.tagged += npc
        }
    }

    public fun healerPulse(access: StandardNpcAccess) {
        with(access) {
            val session = owners[npc] ?: return
            val jad = session.healerTargets[npc]
            if (jad == null || jad.hitpoints <= 0 || jad.slotId == INVALID_SLOT) {
                npc.clearTimer(timers.inferno_healer_heal)
                return
            }
            if (isWithinDistance(jad, 4)) {
                jad.hitpoints = min(jad.baseHitpointsLvl, jad.hitpoints + 5)
            } else {
                walk(jad.coords)
            }
            timer(timers.inferno_healer_heal, 4)
        }
    }

    public fun pulse(access: StandardNpcAccess) {
        val npc = access.npc
        val session = owners[npc] ?: return
        when (npc.id) {
            InfernoNpcs.nibbler.id -> pulseNibbler(access, session)
            InfernoNpcs.mager.id -> pulseMager(access, session)
            InfernoNpcs.shield.id -> pulseShield(access, session)
            InfernoNpcs.zuk.id -> pulseZukState(access, session)
            InfernoNpcs.zukHealer.id -> pulseZukHealer(access, session)
            InfernoNpcs.finalRanger.id,
            InfernoNpcs.finalMager.id -> pulseFinalAdd(access, session)
        }
    }

    public fun isShielded(sessionNpc: Npc, player: Player): Boolean {
        val session = owners[sessionNpc] ?: return false
        val shield = session.shield ?: return false
        return player.coords.x in (shield.coords.x - 1)..(shield.coords.x + 3) &&
            player.coords.z in (shield.coords.z - 4)..(shield.coords.z - 2)
    }

    public fun aggroHealer(npc: Npc, player: Player) {
        if (ownerFor(npc) === player) npc.opPlayer2(player, interactions)
    }

    private fun create(player: Player, wave: Int): InfernoSession? {
        val template =
            RegionTemplate.create {
                copy(ARENA_ZONE_X, ARENA_ZONE_Z, 0) {
                    zoneWidth = 8
                    zoneLength = 8
                    regionZoneX = 4
                    regionZoneZ = 4
                }
            }
        val region = regions.add(template) ?: return null
        return InfernoSession(player, region, wave).also { sessions[player] = it }
    }

    private fun spawnWave(session: InfernoSession) {
        if (sessions[session.player] !== session) return
        session.dead.clear()
        session.healerTargets.clear()
        session.player.mes("Wave: ${session.wave}")
        if (session.wave == 67) collapsePillars(session)
        InfernoWaves.composition(session.wave).forEachIndexed { index, monster ->
            if (monster == InfernoMonster.Zuk) {
                spawnZuk(session)
            } else {
                val point = spawnPoint(session.wave, index, monster)
                val npc =
                    spawn(
                        session,
                        typeFor(monster),
                        point.first,
                        point.second,
                        attackPlayer = monster != InfernoMonster.Nibbler,
                    )
                when (monster) {
                    InfernoMonster.Nibbler -> npc.timer(timers.inferno_pulse, 1)
                    InfernoMonster.Mager -> npc.timer(timers.inferno_pulse, 4)
                    else -> Unit
                }
                if (monster == InfernoMonster.Jad) npc.actionDelay = npc.currentMapClock + index * 3
            }
        }
    }

    private fun spawn(
        session: InfernoSession,
        type: NpcType,
        localX: Int,
        localZ: Int,
        alive: Boolean = true,
        attackPlayer: Boolean = true,
    ): Npc {
        val npc = Npc(npcTypes[type], session.arena(localX, localZ))
        npcs.add(npc, NPC_LIFETIME)
        owners[npc] = session
        if (alive) session.alive += npc else session.support += npc
        if (attackPlayer) npc.opPlayer2(session.player, interactions)
        return npc
    }

    private fun typeFor(monster: InfernoMonster): NpcType =
        when (monster) {
            InfernoMonster.Nibbler -> InfernoNpcs.nibbler
            InfernoMonster.Bat -> InfernoNpcs.bat
            InfernoMonster.Blob -> InfernoNpcs.blob
            InfernoMonster.Meleer -> InfernoNpcs.melee
            InfernoMonster.Ranger -> InfernoNpcs.ranger
            InfernoMonster.Mager -> InfernoNpcs.mager
            InfernoMonster.Jad -> InfernoNpcs.jad
            InfernoMonster.Zuk -> InfernoNpcs.zuk
        }

    private fun splitBlob(session: InfernoSession, coords: CoordGrid) {
        val x = coords.x - session.arena(0, 0).x
        val z = coords.z - session.arena(0, 0).z
        listOf(InfernoNpcs.blobMelee, InfernoNpcs.blobRange, InfernoNpcs.blobMage).forEachIndexed {
            i,
            type ->
            spawn(session, type, x + i - 1, z)
        }
    }

    private fun rememberDeath(session: InfernoSession, npc: Npc) {
        val eligible = npc.id in RESURRECTABLE_IDS && npc.id != InfernoNpcs.mager.id
        if (eligible) {
            val type =
                when (npc.id) {
                    InfernoNpcs.bat.id -> InfernoNpcs.bat
                    InfernoNpcs.blob.id -> InfernoNpcs.blob
                    InfernoNpcs.blobMage.id -> InfernoNpcs.blobMage
                    InfernoNpcs.blobRange.id -> InfernoNpcs.blobRange
                    InfernoNpcs.blobMelee.id -> InfernoNpcs.blobMelee
                    InfernoNpcs.melee.id -> InfernoNpcs.melee
                    else -> InfernoNpcs.ranger
                }
            session.dead += DeadInfernoNpc(type, npc.coords)
        }
    }

    private fun pulseNibbler(access: StandardNpcAccess, session: InfernoSession) {
        val pillar =
            session.pillars
                .withIndex()
                .filter { it.value > 0 }
                .minByOrNull {
                    val p = PILLARS[it.index]
                    max(
                        kotlin.math.abs(access.coords.x - session.arena(p.first, p.second).x),
                        kotlin.math.abs(access.coords.z - session.arena(p.first, p.second).z),
                    )
                }
        if (pillar == null) {
            access.npc.opPlayer2(session.player, interactions)
            return
        }
        val target = PILLARS[pillar.index]
        val coords = session.arena(target.first, target.second)
        val distance =
            max(
                kotlin.math.abs(access.coords.x - coords.x),
                kotlin.math.abs(access.coords.z - coords.z),
            )
        if (distance <= 1) {
            session.pillars[pillar.index] = (pillar.value - access.random.of(1..2)).coerceAtLeast(0)
            syncPillars(session)
        } else {
            access.walk(coords)
        }
        access.timer(timers.inferno_pulse, 4)
    }

    private fun pulseMager(access: StandardNpcAccess, session: InfernoSession) {
        if (session.dead.isNotEmpty() && access.mapClock >= session.nextResurrection) {
            val dead = session.dead.removeAt(0)
            access.anim(InfernoSeqs.magerResurrect)
            val x = dead.coords.x - session.arena(0, 0).x
            val z = dead.coords.z - session.arena(0, 0).z
            spawn(session, dead.type, x, z).also { it.hitpoints = max(1, it.baseHitpointsLvl / 2) }
            session.nextResurrection = access.mapClock + 16
        }
        access.timer(timers.inferno_pulse, 4)
    }

    private fun spawnZuk(session: InfernoSession) {
        val zuk = spawn(session, InfernoNpcs.zuk, 27, 51, attackPlayer = false)
        val shield = spawn(session, InfernoNpcs.shield, 28, 46, alive = false, attackPlayer = false)
        session.zuk = zuk
        session.shield = shield
        session.shieldDirection = 1
        session.nextSetClock = zuk.currentMapClock + 42
        zuk.timer(timers.inferno_pulse, 1)
        shield.timer(timers.inferno_pulse, 1)
        session.player.zukBaseHealth = zuk.baseHitpointsLvl
        session.player.zukHealth = zuk.hitpoints
    }

    private fun pulseShield(access: StandardNpcAccess, session: InfernoSession) {
        if (session.shieldPause > 0) {
            session.shieldPause--
        } else {
            val localX = access.coords.x - session.arena(0, 0).x
            if (localX <= 18) {
                session.shieldDirection = 1
                session.shieldPause = 5
            }
            if (localX >= 40) {
                session.shieldDirection = -1
                session.shieldPause = 5
            }
            access.walk(session.arena(localX + session.shieldDirection, 46))
        }
        access.timer(timers.inferno_pulse, 1)
    }

    private fun pulseZukState(access: StandardNpcAccess, session: InfernoSession) {
        session.player.zukHealth = access.npc.hitpoints
        if (access.mapClock >= session.nextSetClock) {
            spawnSet(session)
            session.nextSetClock = access.mapClock + 350
        }
        access.timer(timers.inferno_pulse, 1)
    }

    private fun pulseZukHealer(access: StandardNpcAccess, session: InfernoSession) {
        val zuk = session.zuk ?: return
        if (access.npc !in session.tagged && zuk.hitpoints > 0) {
            zuk.hitpoints = min(zuk.baseHitpointsLvl, zuk.hitpoints + access.random.of(15..25))
        }
        access.timer(timers.inferno_pulse, 3)
    }

    private fun pulseFinalAdd(access: StandardNpcAccess, session: InfernoSession) {
        if (access.npc in session.tagged) {
            access.npc.opPlayer2(session.player, interactions)
        } else {
            val shield = session.shield
            if (shield != null && shield.hitpoints > 0) {
                access.anim(
                    if (access.npc.id == InfernoNpcs.finalRanger.id) InfernoSeqs.rangerRanged
                    else InfernoSeqs.magerMagic
                )
                shield.hitpoints = (shield.hitpoints - access.random.of(0..10)).coerceAtLeast(0)
                if (shield.hitpoints == 0) {
                    session.player.queueHit(
                        1,
                        org.rsmod.game.hit.HitType.Typeless,
                        session.player.hitpoints,
                    )
                }
            }
        }
        access.timer(timers.inferno_pulse, 4)
    }

    private fun updateZukPhases(session: InfernoSession, zuk: Npc) {
        session.player.zukHealth = zuk.hitpoints
        if (zuk.hitpoints <= 600 && !session.setPauseApplied) {
            session.setPauseApplied = true
            session.nextSetClock += 175
        }
        if (zuk.hitpoints <= 480 && !session.zukJadSpawned) {
            session.zukJadSpawned = true
            spawn(session, InfernoNpcs.finalJad, 30, 37)
        }
        if (zuk.hitpoints <= 240 && !session.zukHealersSpawned) {
            session.zukHealersSpawned = true
            listOf(18 to 43, 23 to 44, 37 to 44, 42 to 43).forEach { (x, z) ->
                spawn(session, InfernoNpcs.zukHealer, x, z, attackPlayer = false)
                    .timer(timers.inferno_pulse, 3)
            }
        }
    }

    private fun spawnSet(session: InfernoSession) {
        spawn(session, InfernoNpcs.finalRanger, 18, 39, attackPlayer = false)
            .timer(timers.inferno_pulse, 4)
        spawn(session, InfernoNpcs.finalMager, 42, 39, attackPlayer = false)
            .timer(timers.inferno_pulse, 4)
    }

    private fun maybeSpawnJadHealers(session: InfernoSession, jad: Npc) {
        if (jad.hitpoints > jad.baseHitpointsLvl / 2 || session.healedJads.contains(jad)) return
        session.healedJads += jad
        val final = jad.id == InfernoNpcs.finalJad.id
        val type = if (final) InfernoNpcs.finalJadHealer else InfernoNpcs.jadHealer
        val count = if (session.wave == 67) 5 else 3
        repeat(count) { i ->
            val point = SPAWNS[(i * 2 + session.wave) % SPAWNS.size]
            val healer = spawn(session, type, point.first, point.second, attackPlayer = false)
            session.healerTargets[healer] = jad
            healer.timer(timers.inferno_healer_heal, 4)
        }
    }

    private fun removeJadHealers(session: InfernoSession, jad: Npc) {
        val healers = session.healerTargets.filterValues { it === jad }.keys.toList()
        healers.forEach { healer ->
            session.healerTargets.remove(healer)
            session.alive.remove(healer)
            owners.remove(healer)
            if (healer.slotId != INVALID_SLOT) npcs.del(healer, Int.MAX_VALUE)
        }
    }

    private fun collapsePillars(session: InfernoSession) {
        session.pillars.fill(0)
        syncPillars(session)
        session.player.queueHit(
            1,
            org.rsmod.game.hit.HitType.Typeless,
            session.player.hitpoints / 2,
        )
    }

    private fun advance(session: InfernoSession) {
        if (session.wave >= InfernoWaves.FINAL_WAVE) return
        protectedAccess.launch(session.player) {
            delay(4)
            if (sessions[player] !== session || session.alive.isNotEmpty()) return@launch
            session.wave++
            spawnWave(session)
        }
    }

    private fun complete(session: InfernoSession) {
        session.player.totalZukKills++
        val kills = session.player.totalZukKills
        protectedAccess.launch(session.player) {
            delay(4)
            if (sessions[player] !== session) return@launch
            sessions.remove(player)
            suspended.remove(player.username)
            destroy(session)
            clearHud(player)
            telejump(OUTSIDE)
            player.invAddOrDrop(objRepo, objs.tokkul, TOKKUL_REWARD)
            player.invAddOrDrop(objRepo, objs.infernal_cape)
            mes("You have defeated TzKal-Zuk and completed the Inferno!")
            mes("Your TzKal-Zuk kill count is: <col=ff0000>$kills</col>.")
            publish(
                CombatAchievementEvents.EncounterCompleted(
                    player = player,
                    encounter = "Inferno",
                    completionCount = kills,
                )
            )
        }
    }

    private fun initialiseHud(session: InfernoSession) {
        session.player.playerInInferno = true
        syncPillars(session)
    }

    private fun syncPillars(session: InfernoSession) {
        session.player.pillar1Health = session.pillars[0]
        session.player.pillar2Health = session.pillars[1]
        session.player.pillar3Health = session.pillars[2]
    }

    private fun clearHud(player: Player) {
        player.playerInInferno = false
        player.zukHealth = 0
        player.zukBaseHealth = 0
    }

    private fun destroy(session: InfernoSession) {
        (session.alive + session.support).toList().forEach { npc ->
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        session.alive.clear()
        session.support.clear()
    }

    private fun spawnPoint(wave: Int, ordinal: Int, monster: InfernoMonster): Pair<Int, Int> {
        if (monster == InfernoMonster.Nibbler) return NIBBLER_SPAWNS[ordinal % NIBBLER_SPAWNS.size]
        return SPAWNS[Math.floorMod(wave * 3 + ordinal * 5, SPAWNS.size)]
    }

    private fun consolationTokkul(wave: Int): Int =
        ((TOKKUL_REWARD * (wave - 1)) / 68).coerceAtLeast(0)

    public companion object {
        public val OUTSIDE: CoordGrid = CoordGrid(2494, 5124, 0)
        private const val ARENA_ZONE_X = 280
        private const val ARENA_ZONE_Z = 664
        private const val NPC_LIFETIME = 150_000
        private const val TOKKUL_REWARD = 16_440
        private val PILLARS = listOf(23 to 36, 37 to 36, 30 to 26)
        private val NIBBLER_SPAWNS =
            listOf(29 to 21, 30 to 21, 31 to 21, 29 to 22, 30 to 22, 31 to 22)
        private val SPAWNS =
            listOf(19 to 24, 22 to 42, 30 to 34, 38 to 42, 42 to 24, 20 to 32, 40 to 32)
        private val RESURRECTABLE_IDS by lazy {
            setOf(
                InfernoNpcs.bat.id,
                InfernoNpcs.blob.id,
                InfernoNpcs.blobMage.id,
                InfernoNpcs.blobRange.id,
                InfernoNpcs.blobMelee.id,
                InfernoNpcs.melee.id,
                InfernoNpcs.ranger.id,
            )
        }
    }
}

public data class InfernoSession(
    val player: Player,
    val region: Region,
    var wave: Int,
    val alive: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val support: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val dead: MutableList<DeadInfernoNpc> = mutableListOf(),
    val healerTargets: MutableMap<Npc, Npc> = IdentityHashMap(),
    val healedJads: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val tagged: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val pillars: IntArray = intArrayOf(255, 255, 255),
    var nextResurrection: Int = 0,
    var zuk: Npc? = null,
    var shield: Npc? = null,
    var shieldDirection: Int = 1,
    var shieldPause: Int = 0,
    var nextSetClock: Int = Int.MAX_VALUE,
    var setPauseApplied: Boolean = false,
    var zukJadSpawned: Boolean = false,
    var zukHealersSpawned: Boolean = false,
) {
    public fun arena(localX: Int, localZ: Int): CoordGrid = region.normal[0, 35, 83, localX, localZ]
}

public data class DeadInfernoNpc(val type: NpcType, val coords: CoordGrid)
