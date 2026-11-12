package org.rsmod.content.minigames.toa

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
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

/** Solo Tombs of Amascut controller backed by the native cache maps. */
@Singleton
public class ToaController
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
    private val sessions = IdentityHashMap<Player, ToaSession>()
    private val owners = IdentityHashMap<Npc, ToaSession>()
    private val suspended = mutableMapOf<String, SuspendedToa>()

    private var Player.entryCompletions by intVarp(ToaVarps.entryCompletions)
    private var Player.normalCompletions by intVarp(ToaVarps.normalCompletions)
    private var Player.expertCompletions by intVarp(ToaVarps.expertCompletions)
    private var Player.raidLevelHud by intVarBit(ToaVarBits.raidLevel)
    private var Player.damageHud by intVarBit(ToaVarBits.damage)

    public suspend fun enter(access: ProtectedAccess, mode: ToaMode) {
        val player = access.player
        if (!player.members) {
            access.mes("You need to be on a members' world to enter the Tombs of Amascut.")
            return
        }
        sessions.remove(player)?.let(::destroy)
        suspended.remove(player.username)
        val session =
            create(player, mode, emptySet(), 0, 0)
                ?: run {
                    access.mes(
                        "No Tombs instance is currently available. Please try again shortly."
                    )
                    return
                }
        syncHud(session)
        access.telejump(session.nexus())
        access.mes(
            "You enter the Tombs of Amascut: ${mode.displayName}, raid level ${mode.raidLevel}."
        )
        access.mes("Complete the four paths in any order, then face the Wardens.")
    }

    public suspend fun leave(access: ProtectedAccess) {
        val session = sessions.remove(access.player) ?: return
        destroy(session)
        suspended.remove(access.player.username)
        clearHud(access.player)
        access.telejump(OUTSIDE)
        access.mes("You leave the Tombs of Amascut.")
    }

    public fun beginPath(player: Player, path: ToaPath) {
        val session = sessions[player] ?: return
        if (session.encounter != ToaEncounter.Nexus) return
        if (path in session.completed) {
            player.mes("You have already conquered the ${path.displayName}.")
            return
        }
        session.activePath = path
        session.encounter = path.challenge
        resetEncounter(session)
        protectedAccess.launch(player) {
            telejump(session.challengeEntrance(path))
            mes("${path.displayName}: ${path.challengeDescription}")
            when (path) {
                ToaPath.Scabaras ->
                    mes(
                        "Repeat this sequence: ${session.scabarasSequence.joinToString(" - ") { (it + 1).toString() }}"
                    )
                ToaPath.Apmeken -> spawnApmekenWave(session)
                else -> Unit
            }
        }
    }

    public fun beginWardens(player: Player) {
        val session = sessions[player] ?: return
        if (session.encounter != ToaEncounter.Nexus) return
        if (session.completed.size != 4) {
            player.mes("The seal remains closed. ${4 - session.completed.size} path(s) remain.")
            return
        }
        session.encounter = ToaEncounter.Wardens
        resetEncounter(session)
        protectedAccess.launch(player) {
            telejump(session.entrance(ToaArena.Wardens))
            mes("The final confrontation begins. Defeat Tumeken's Warden through all three phases.")
            delay(2)
            spawnWardenPhase(session, 1)
        }
    }

    public fun pressScabaras(player: Player, button: Int) {
        val session = sessions[player] ?: return
        if (session.encounter != ToaEncounter.ScabarasChallenge) return
        if (session.scabarasSequence[session.puzzleProgress] != button) {
            session.puzzleProgress = 0
            player.mes("The sequence resets and the floor shocks you.")
            player.queueHit(1, HitType.Typeless, 10)
            return
        }
        session.puzzleProgress++
        if (session.puzzleProgress < session.scabarasSequence.size) {
            player.mes("Correct: ${session.puzzleProgress}/${session.scabarasSequence.size}.")
        } else {
            completeChallenge(session, ToaEncounter.Kephri, ToaArena.Kephri, ToaNpcs.kephriShielded)
        }
    }

    public fun progressCrondis(player: Player) {
        progressChallenge(
            player,
            ToaEncounter.CrondisChallenge,
            "You carry water through the poison course.",
        ) { session ->
            completeChallenge(session, ToaEncounter.Zebak, ToaArena.Zebak, ToaNpcs.zebak)
        }
    }

    public fun progressHet(player: Player) {
        progressChallenge(
            player,
            ToaEncounter.HetChallenge,
            "You align another mirror and redirect the light.",
        ) { session ->
            completeChallenge(session, ToaEncounter.Akkha, ToaArena.Akkha, ToaNpcs.akkhaMelee)
        }
    }

    private fun progressChallenge(
        player: Player,
        encounter: ToaEncounter,
        message: String,
        complete: (ToaSession) -> Unit,
    ) {
        val session = sessions[player] ?: return
        if (session.encounter != encounter) return
        session.puzzleProgress++
        player.mes("$message (${session.puzzleProgress}/3)")
        if (session.puzzleProgress >= 3) complete(session)
    }

    private fun completeChallenge(
        session: ToaSession,
        encounter: ToaEncounter,
        arena: ToaArena,
        boss: NpcType,
    ) {
        session.encounter = encounter
        session.puzzleProgress = 0
        protectedAccess.launch(session.player) {
            telejump(session.entrance(arena))
            delay(2)
            session.mainBoss =
                spawn(session, boss, arena.bossX, arena.bossZ).also {
                    it.timer(timers.toa_pulse, 2)
                }
            mes("${encounter.displayName} enters the arena.")
        }
    }

    public fun onHit(npc: Npc, hit: Hit) {
        val session = owners[npc] ?: return
        if (!hit.isFromPlayer || hit.damage <= 0) return
        session.damageDone = (session.damageDone + hit.damage).coerceAtMost(16_383)
        syncHud(session)
        val required = requiredStyle(npc)
        if (required != null && hit.type != required) {
            npc.hitpoints = min(npc.baseHitpointsLvl, npc.hitpoints + hit.damage)
            session.player.mes("That creature is protected from this combat style.")
            return
        }
        if (npc !== session.mainBoss) return
        val pct = npc.hitpoints * 100 / max(1, npc.baseHitpointsLvl)
        when (session.encounter) {
            ToaEncounter.Baba ->
                thresholdSupports(
                    session,
                    pct,
                    session.babaThresholds,
                    ToaNpcs.babaBoulderWeak,
                    "Ba-Ba sends boulders crashing down!",
                )
            ToaEncounter.Akkha ->
                thresholdSupports(
                    session,
                    pct,
                    session.akkhaThresholds,
                    ToaNpcs.akkhaShadow,
                    "Akkha splits off a shadow!",
                )
            ToaEncounter.Zebak -> checkZebakThreshold(session, pct)
            else -> Unit
        }
        if (
            session.support.isNotEmpty() &&
                session.encounter in setOf(ToaEncounter.Baba, ToaEncounter.Akkha)
        ) {
            npc.hitpoints = min(npc.baseHitpointsLvl, npc.hitpoints + hit.damage)
        }
    }

    private fun thresholdSupports(
        session: ToaSession,
        pct: Int,
        thresholds: MutableSet<Int>,
        type: NpcType,
        message: String,
    ) {
        val threshold = thresholds.filter { pct <= it }.maxOrNull() ?: return
        thresholds.remove(threshold)
        session.player.mes(message)
        repeat(if (type == ToaNpcs.akkhaShadow) 1 else 3) { index ->
            spawn(session, type, 25 + index * 6, 32, support = true)
        }
    }

    private fun checkZebakThreshold(session: ToaSession, pct: Int) {
        val threshold = session.zebakThresholds.filter { pct <= it }.maxOrNull() ?: return
        session.zebakThresholds.remove(threshold)
        val roar = session.zebakThresholds.size % 2 == 1
        session.player.mes(
            if (roar) "Zebak roars—hide behind a fallen jug!"
            else "Zebak summons a tidal wave—find the opening!"
        )
        delayedTileCheck(
            session,
            session.mainBoss ?: return,
            session.player.coords,
            if (roar) 35 else 28,
            3,
        )
    }

    public fun onKill(player: Player, npc: Npc) {
        val session = owners.remove(npc) ?: return
        if (session.player !== player) return
        session.alive.remove(npc)
        session.support.remove(npc)
        if (session.encounter == ToaEncounter.ApmekenChallenge) {
            if (session.alive.isEmpty()) advanceApmeken(session)
            return
        }
        if (npc !== session.mainBoss) return
        when (session.encounter) {
            ToaEncounter.Kephri -> advanceKephri(session, npc)
            ToaEncounter.Akkha -> advanceAkkha(session, npc)
            ToaEncounter.Zebak ->
                if (npc.visType.id != ToaNpcs.zebakEnraged.id)
                    spawnReplacement(session, ToaNpcs.zebakEnraged)
                else finishPath(session)
            ToaEncounter.Wardens ->
                if (session.wardenPhase < 3) spawnNextWarden(session) else completeRaid(session)
            ToaEncounter.Baba -> finishPath(session)
            else -> Unit
        }
    }

    private fun advanceKephri(session: ToaSession, npc: Npc) {
        when (npc.visType.id) {
            ToaNpcs.kephriShielded.id -> {
                session.player.mes("Kephri's first shield breaks and scarab guardians emerge.")
                listOf(ToaNpcs.kephriMelee, ToaNpcs.kephriRanged, ToaNpcs.kephriMage)
                    .forEachIndexed { i, type ->
                        spawn(session, type, 23 + i * 5, 27, support = true)
                    }
                spawnReplacement(session, ToaNpcs.kephriWeak)
            }
            ToaNpcs.kephriWeak.id -> spawnReplacement(session, ToaNpcs.kephriEnrage)
            else -> finishPath(session)
        }
    }

    private fun advanceAkkha(session: ToaSession, npc: Npc) {
        if (npc.visType.id != ToaNpcs.akkhaEnrage.id) {
            session.player.mes("Akkha reaches 20% health and begins his final orb phase.")
            spawnReplacement(session, ToaNpcs.akkhaEnrage)
        } else finishPath(session)
    }

    private fun spawnReplacement(session: ToaSession, type: NpcType) {
        protectedAccess.launch(session.player) {
            delay(3)
            if (sessions[player] !== session) return@launch
            val arena = session.encounter.arena
            session.mainBoss =
                spawn(session, type, arena.bossX, arena.bossZ).also {
                    it.timer(timers.toa_pulse, 2)
                }
        }
    }

    private fun advanceApmeken(session: ToaSession) {
        protectedAccess.launch(session.player) {
            delay(2)
            if (sessions[player] !== session) return@launch
            if (session.apmekenWave < APMEKEN_WAVES) spawnApmekenWave(session)
            else {
                session.encounter = ToaEncounter.Baba
                telejump(session.entrance(ToaArena.Baba))
                delay(2)
                session.mainBoss =
                    spawn(session, ToaNpcs.baba, ToaArena.Baba.bossX, ToaArena.Baba.bossZ).also {
                        it.timer(timers.toa_pulse, 2)
                    }
                mes("The baboons retreat. Ba-Ba drops into the arena!")
            }
        }
    }

    private fun spawnApmekenWave(session: ToaSession) {
        session.apmekenWave++
        session.player.mes("Apmeken wave ${session.apmekenWave}/$APMEKEN_WAVES.")
        val types =
            listOf(
                ToaNpcs.apmekenMelee,
                ToaNpcs.apmekenRanged,
                ToaNpcs.apmekenMagic,
                ToaNpcs.apmekenShaman,
            )
        repeat(2 + session.apmekenWave / 3) { index ->
            spawn(session, types[(session.apmekenWave + index) % types.size], 24 + index * 4, 31)
        }
    }

    public fun pulse(access: StandardNpcAccess) {
        val session = owners[access.npc] ?: return
        if (access.npc !== session.mainBoss) return
        session.specialCounter++
        when (session.encounter) {
            ToaEncounter.Kephri ->
                if (session.specialCounter % 2 == 0) {
                    val tile = session.player.coords
                    locs.add(
                        tile,
                        locTypes[ToaLocs.kephriDung],
                        12,
                        LocAngle.North,
                        LocShape.CentrepieceStraight,
                    )
                    session.player.mes("Kephri targets your tile with a fireball—move!")
                    delayedTileCheck(session, access.npc, tile, 32, 3)
                }
            ToaEncounter.Baba ->
                if (session.specialCounter % 2 == 0) {
                    session.player.mes("Ba-Ba prepares a ground slam—move away!")
                    delayedTileCheck(session, access.npc, session.player.coords, 38, 2)
                }
            ToaEncounter.Zebak ->
                if (session.specialCounter % 2 == 0) {
                    val tile = session.player.coords
                    locs.add(
                        tile,
                        locTypes[ToaLocs.poisonTile],
                        20,
                        LocAngle.North,
                        LocShape.CentrepieceStraight,
                    )
                    session.player.mes("Zebak spits acid beneath you.")
                    delayedTileCheck(session, access.npc, tile, 22, 2)
                }
            ToaEncounter.Akkha -> {
                if (
                    session.specialCounter % 3 == 0 &&
                        access.npc.visType.id != ToaNpcs.akkhaEnrage.id
                )
                    cycleAkkha(access, session)
                if (access.npc.visType.id == ToaNpcs.akkhaEnrage.id)
                    delayedTileCheck(session, access.npc, session.player.coords, 25, 2)
            }
            ToaEncounter.Wardens -> wardenSpecial(access, session)
            else -> Unit
        }
        access.timer(timers.toa_pulse, if (session.encounter == ToaEncounter.Wardens) 3 else 4)
    }

    private fun cycleAkkha(access: StandardNpcAccess, session: ToaSession) {
        session.akkhaForm = (session.akkhaForm + 1) % 3
        val type =
            listOf(ToaNpcs.akkhaMelee, ToaNpcs.akkhaRanged, ToaNpcs.akkhaMage)[session.akkhaForm]
        access.changeType(type, Int.MAX_VALUE, npcTypes)
        access.npc.opPlayer2(session.player, interactions)
        session.player.mes("Akkha changes combat style.")
    }

    private fun wardenSpecial(access: StandardNpcAccess, session: ToaSession) {
        when (session.wardenPhase) {
            1 ->
                if (session.specialCounter % 2 == 0) {
                    session.player.mes("The obelisk discharges energy at your tile.")
                    delayedTileCheck(session, access.npc, session.player.coords, 35, 3)
                }
            2 ->
                if (session.specialCounter % 2 == 0) {
                    val next =
                        if (access.npc.visType.id == ToaNpcs.wardenP2Mage.id) ToaNpcs.wardenP2Range
                        else ToaNpcs.wardenP2Mage
                    access.changeType(next, Int.MAX_VALUE, npcTypes)
                    access.npc.opPlayer2(session.player, interactions)
                    session.player.mes("The Warden changes protection and attack style.")
                }
            3 -> {
                val tile = session.player.coords
                session.player.mes("The Warden destroys another section of floor—keep moving!")
                delayedTileCheck(
                    session,
                    access.npc,
                    tile,
                    if (access.npc.hitpoints <= access.npc.baseHitpointsLvl / 5) 48 else 34,
                    2,
                )
                if (
                    access.npc.hitpoints <= access.npc.baseHitpointsLvl / 5 &&
                        access.npc.visType.id != ToaNpcs.wardenP3Charging.id
                ) {
                    access.changeType(ToaNpcs.wardenP3Charging, Int.MAX_VALUE, npcTypes)
                    access.npc.opPlayer2(session.player, interactions)
                }
            }
        }
    }

    private fun delayedTileCheck(
        session: ToaSession,
        source: Npc,
        tile: CoordGrid,
        normal: Int,
        delayTicks: Int,
    ) {
        protectedAccess.launch(session.player) {
            delay(delayTicks)
            if (sessions[player] === session && player.coords.chebyshevDistance(tile) <= 1) {
                player.queueHit(source, 1, HitType.Typeless, scaledMaxHit(source, normal))
            }
        }
    }

    private fun spawnNextWarden(session: ToaSession) {
        protectedAccess.launch(session.player) {
            delay(4)
            if (sessions[player] === session) spawnWardenPhase(session, session.wardenPhase + 1)
        }
    }

    private fun spawnWardenPhase(session: ToaSession, phase: Int) {
        session.wardenPhase = phase
        val type =
            when (phase) {
                1 -> ToaNpcs.wardenP1
                2 -> ToaNpcs.wardenP2Mage
                else -> ToaNpcs.wardenP3
            }
        session.mainBoss =
            spawn(
                    session,
                    type,
                    ToaArena.Wardens.bossX,
                    ToaArena.Wardens.bossZ,
                    attack = phase != 1,
                )
                .also { it.timer(timers.toa_pulse, 2) }
        session.player.mes("Tumeken's Warden: phase $phase.")
    }

    private fun finishPath(session: ToaSession) {
        val path = session.activePath ?: return
        clearRoomNpcs(session)
        session.completed += path
        session.activePath = null
        session.encounter = ToaEncounter.Nexus
        if (session.completed.size == 2 || session.completed.size == 4) giveSupplies(session)
        protectedAccess.launch(session.player) {
            delay(3)
            if (sessions[player] !== session) return@launch
            telejump(session.nexus())
            mes("${path.displayName} complete (${session.completed.size}/4).")
            if (session.completed.size == 4) mes("The way to the Wardens is now open.")
        }
    }

    private fun giveSupplies(session: ToaSession) {
        val count = if (session.mode == ToaMode.Entry) 2 else 1
        session.player.invAddOrDrop(objRepo, ToaObjs.nectar, 2 * count)
        session.player.invAddOrDrop(objRepo, ToaObjs.tears, 2 * count)
        session.player.invAddOrDrop(objRepo, ToaObjs.ambrosia, count)
        session.player.invAddOrDrop(objRepo, ToaObjs.salts, count)
        session.player.mes("The Helpful Spirit provides raid supplies.")
    }

    public fun onDeath(player: Player) {
        val session = sessions[player] ?: return
        session.deaths++
        clearRoomNpcs(session)
        if (session.attemptsLeft != Int.MAX_VALUE) session.attemptsLeft--
        if (session.attemptsLeft <= 0) {
            sessions.remove(player)
            destroy(session)
            clearHud(player)
            protectedAccess.launch(player) {
                delay(2)
                telejump(OUTSIDE)
                mes("Your Tombs of Amascut attempt is over.")
            }
            return
        }
        session.encounter = ToaEncounter.Nexus
        session.activePath = null
        protectedAccess.launch(player) {
            delay(2)
            if (sessions[player] !== session) return@launch
            statHeal(stats.hitpoints, constant = 0, percent = 100)
            statHeal(stats.prayer, constant = 0, percent = 100)
            player.invAddOrDrop(objRepo, ToaObjs.honeyLocust, 3)
            telejump(session.nexus())
            mes(
                if (session.attemptsLeft == Int.MAX_VALUE) "You revive at the Nexus."
                else "You revive with ${session.attemptsLeft} attempt(s) remaining."
            )
        }
    }

    public fun suspendOnLogout(player: Player) {
        val session = sessions.remove(player) ?: return
        suspended[player.username] =
            SuspendedToa(
                session.mode,
                session.completed.toSet(),
                session.deaths,
                session.damageDone,
            )
        destroy(session)
        clearHud(player)
    }

    public fun resumeOnLogin(player: Player) {
        val saved = suspended.remove(player.username) ?: return
        protectedAccess.launch(player) {
            val session =
                create(player, saved.mode, saved.completed, saved.deaths, saved.damageDone)
                    ?: run {
                        telejump(OUTSIDE)
                        mes("Your Tombs attempt could not be restored.")
                        return@launch
                    }
            syncHud(session)
            telejump(session.nexus())
            mes(
                "Your ${session.mode.displayName} Tombs attempt resumes with ${session.completed.size}/4 paths complete."
            )
        }
    }

    public fun ownerFor(npc: Npc): Player? = owners[npc]?.player

    public fun encounterFor(npc: Npc): ToaEncounter? = owners[npc]?.encounter

    public fun requiredStyle(npc: Npc): HitType? =
        when (npc.visType.id) {
            ToaNpcs.apmekenMelee.id -> HitType.Melee
            ToaNpcs.apmekenRanged.id -> HitType.Ranged
            ToaNpcs.apmekenMagic.id -> HitType.Magic
            else -> null
        }

    public fun scaledMaxHit(npc: Npc, normal: Int): Int =
        max(
            1,
            normal * (owners[npc]?.mode?.let { ToaRules.damagePercent(it.raidLevel) } ?: 100) / 100,
        )

    private fun completeRaid(session: ToaSession) {
        when (session.mode) {
            ToaMode.Entry -> session.player.entryCompletions++
            ToaMode.Normal -> session.player.normalCompletions++
            ToaMode.Expert -> session.player.expertCompletions++
        }
        val unique = rollUnique(session)
        val reward = unique ?: commonReward(session)
        val count = if (unique != null) 1 else 10 + session.mode.raidLevel / 15
        protectedAccess.launch(session.player) {
            delay(5)
            if (sessions[player] !== session) return@launch
            sessions.remove(player)
            destroy(session)
            clearHud(player)
            telejump(OUTSIDE)
            player.invAddOrDrop(objRepo, reward, count)
            mes(
                "Tombs of Amascut complete: raid level ${session.mode.raidLevel}, ${session.deaths} death(s)."
            )
            if (unique != null)
                mes("<col=ff0000>A unique reward waits inside the ancient casket!</col>")
            val completions =
                when (session.mode) {
                    ToaMode.Entry -> player.entryCompletions
                    ToaMode.Normal -> player.normalCompletions
                    ToaMode.Expert -> player.expertCompletions
                }
            publish(
                CombatAchievementEvents.EncounterCompleted(
                    player = player,
                    encounter = "Tombs of Amascut",
                    completionCount = completions,
                    mode = session.mode.displayName,
                    deaths = session.deaths,
                    raidLevel = session.mode.raidLevel,
                )
            )
        }
    }

    private fun rollUnique(session: ToaSession): ObjType? {
        val denominator = max(12, 45 - session.mode.raidLevel / 10 + session.deaths * 5)
        if (Random.nextInt(denominator) != 0) return null
        return UNIQUES.random()
    }

    private fun commonReward(session: ToaSession): ObjType =
        when (session.damageDone % 3) {
            0 -> objs.runite_ore
            1 -> objs.dragon_dart_tip
            else -> objs.death_rune
        }

    private fun create(
        player: Player,
        mode: ToaMode,
        completed: Set<ToaPath>,
        deaths: Int,
        damageDone: Int,
    ): ToaSession? {
        val template =
            RegionTemplate.createLarge {
                ToaArena.entries.forEach { arena ->
                    copy(arena.mapX * 8, arena.mapZ * 8, arena.level) {
                        zoneWidth = 8
                        zoneLength = 8
                        regionZoneX = arena.zoneX
                        regionZoneZ = arena.zoneZ
                    }
                }
            }
        val region = regions.add(template) ?: return null
        return ToaSession(
                player,
                region,
                mode,
                completed.toMutableSet(),
                deaths,
                damageDone,
                mode.attempts,
            )
            .also { sessions[player] = it }
    }

    private fun spawn(
        session: ToaSession,
        type: NpcType,
        localX: Int,
        localZ: Int,
        support: Boolean = false,
        attack: Boolean = true,
    ): Npc {
        val arena = session.encounter.arena
        val npc = Npc(npcTypes[type], session.tile(arena, localX, localZ))
        val scale = ToaRules.statPercent(session.mode.raidLevel)
        npc.baseHitpointsLvl = max(1, npc.baseHitpointsLvl * scale / 100)
        npc.hitpoints = npc.baseHitpointsLvl
        npcs.add(npc, NPC_LIFETIME)
        owners[npc] = session
        if (support) session.support += npc else session.alive += npc
        if (attack) npc.opPlayer2(session.player, interactions)
        return npc
    }

    private fun clearRoomNpcs(session: ToaSession) {
        (session.alive + session.support).toList().forEach { npc ->
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        session.alive.clear()
        session.support.clear()
        session.mainBoss = null
    }

    private fun destroy(session: ToaSession) = clearRoomNpcs(session)

    private fun resetEncounter(session: ToaSession) {
        clearRoomNpcs(session)
        session.puzzleProgress = 0
        session.apmekenWave = 0
        session.specialCounter = 0
        session.wardenPhase = 0
        session.akkhaForm = 0
        session.babaThresholds.clear()
        session.babaThresholds += listOf(66, 33)
        session.akkhaThresholds.clear()
        session.akkhaThresholds += listOf(80, 60, 40, 20)
        session.zebakThresholds.clear()
        session.zebakThresholds += listOf(85, 70, 55, 40)
        session.scabarasSequence = (0..8).shuffled().take(4)
    }

    private fun syncHud(session: ToaSession) {
        session.player.raidLevelHud = session.mode.raidLevel
        session.player.damageHud = session.damageDone.coerceAtMost(16_383)
    }

    private fun clearHud(player: Player) {
        player.raidLevelHud = 0
        player.damageHud = 0
    }

    public companion object {
        public val OUTSIDE: CoordGrid = CoordGrid(3357, 2711, 0)
        private const val NPC_LIFETIME = 200_000
        private const val APMEKEN_WAVES = 8
        private val UNIQUES =
            listOf(
                ToaObjs.fang,
                ToaObjs.lightbearer,
                ToaObjs.ward,
                ToaObjs.masoriMask,
                ToaObjs.masoriBody,
                ToaObjs.masoriChaps,
                ToaObjs.shadow,
            )
    }
}

public data class ToaSession(
    val player: Player,
    val region: Region,
    val mode: ToaMode,
    val completed: MutableSet<ToaPath>,
    var deaths: Int,
    var damageDone: Int,
    var attemptsLeft: Int,
    var encounter: ToaEncounter = ToaEncounter.Nexus,
    var activePath: ToaPath? = null,
    val alive: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val support: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    var mainBoss: Npc? = null,
    var puzzleProgress: Int = 0,
    var apmekenWave: Int = 0,
    var specialCounter: Int = 0,
    var wardenPhase: Int = 0,
    var akkhaForm: Int = 0,
    var scabarasSequence: List<Int> = emptyList(),
    val babaThresholds: MutableSet<Int> = mutableSetOf(66, 33),
    val akkhaThresholds: MutableSet<Int> = mutableSetOf(80, 60, 40, 20),
    val zebakThresholds: MutableSet<Int> = mutableSetOf(85, 70, 55, 40),
) {
    internal fun tile(arena: ToaArena, localX: Int, localZ: Int): CoordGrid =
        region.normal[arena.level, arena.mapX, arena.mapZ, localX, localZ]

    internal fun entrance(arena: ToaArena): CoordGrid = tile(arena, arena.entryX, arena.entryZ)

    public fun nexus(): CoordGrid = entrance(ToaArena.Nexus)

    public fun challengeEntrance(path: ToaPath): CoordGrid = entrance(path.challenge.arena)
}

internal enum class ToaArena(
    val mapX: Int,
    val mapZ: Int,
    val level: Int,
    val zoneX: Int,
    val zoneZ: Int,
    val entryX: Int,
    val entryZ: Int,
    val bossX: Int,
    val bossZ: Int,
) {
    Nexus(55, 80, 0, 0, 0, 32, 31, 32, 31),
    Scabaras(55, 82, 0, 8, 0, 28, 31, 28, 31),
    Kephri(55, 84, 0, 16, 0, 16, 31, 29, 31),
    Het(57, 82, 0, 24, 0, 31, 15, 31, 31),
    Akkha(57, 84, 1, 0, 8, 48, 30, 36, 30),
    Wardens(59, 80, 1, 8, 8, 32, 18, 32, 38),
    Apmeken(59, 82, 0, 16, 8, 31, 18, 31, 31),
    Baba(59, 84, 0, 24, 8, 34, 18, 34, 33),
    Crondis(61, 82, 0, 0, 16, 15, 31, 31, 31),
    Zebak(61, 84, 0, 8, 16, 18, 31, 50, 31),
}

private val ToaPath.challenge: ToaEncounter
    get() =
        when (this) {
            ToaPath.Scabaras -> ToaEncounter.ScabarasChallenge
            ToaPath.Apmeken -> ToaEncounter.ApmekenChallenge
            ToaPath.Crondis -> ToaEncounter.CrondisChallenge
            ToaPath.Het -> ToaEncounter.HetChallenge
        }
private val ToaPath.challengeDescription: String
    get() =
        when (this) {
            ToaPath.Scabaras -> "repeat the memory sequence, then defeat Kephri."
            ToaPath.Apmeken -> "survive eight baboon waves, then defeat Ba-Ba."
            ToaPath.Crondis -> "deliver three loads of water through the poison, then defeat Zebak."
            ToaPath.Het -> "align three mirrors, then defeat Akkha."
        }
private val ToaEncounter.arena: ToaArena
    get() =
        when (this) {
            ToaEncounter.Nexus -> ToaArena.Nexus
            ToaEncounter.ScabarasChallenge -> ToaArena.Scabaras
            ToaEncounter.Kephri -> ToaArena.Kephri
            ToaEncounter.ApmekenChallenge -> ToaArena.Apmeken
            ToaEncounter.Baba -> ToaArena.Baba
            ToaEncounter.CrondisChallenge -> ToaArena.Crondis
            ToaEncounter.Zebak -> ToaArena.Zebak
            ToaEncounter.HetChallenge -> ToaArena.Het
            ToaEncounter.Akkha -> ToaArena.Akkha
            ToaEncounter.Wardens -> ToaArena.Wardens
        }
private val ToaEncounter.displayName: String
    get() = name.replace(Regex("([a-z])([A-Z])"), "$1 $2")

private data class SuspendedToa(
    val mode: ToaMode,
    val completed: Set<ToaPath>,
    val deaths: Int,
    val damageDone: Int,
)
