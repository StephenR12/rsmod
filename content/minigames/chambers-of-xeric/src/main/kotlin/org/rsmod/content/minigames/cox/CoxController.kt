package org.rsmod.content.minigames.cox

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.random.GameRandom
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.timers
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.combat.commons.npc.engagePlayer
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.CombatAchievementEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
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
import org.rsmod.game.hit.Hit
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

@Singleton
public class CoxController
@Inject
constructor(
    private val lootRandom:GameRandom,
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val interactions: AiPlayerInteractions,
    private val protectedAccess: ProtectedAccessLauncher,
    private val objRepo: ObjRepository,
) {
    private val sessions = IdentityHashMap<Player, CoxSession>()
    private val owners = IdentityHashMap<Npc, CoxSession>()
    private val suspended = mutableMapOf<String, SuspendedCox>()

    private var Player.playerScore by intVarp(CoxVarps.playerScore)
    private var Player.completions by intVarp(CoxVarps.completions)
    private var Player.challengeCompletions by intVarp(CoxVarps.challengeCompletions)
    private var Player.inRaid by boolVarBit(CoxVarBits.inDungeon)
    private var Player.challengeHud by boolVarBit(CoxVarBits.challengeMode)
    private var Player.diedInRaid by boolVarBit(CoxVarBits.died)
    private var Player.partySizeHud by intVarBit(CoxVarBits.partySize)
    private var Player.progressHud by intVarBit(CoxVarBits.progress)
    private var Player.partyScoreHud by intVarBit(CoxVarBits.partyScore)

    public suspend fun enter(access: ProtectedAccess, challenge: Boolean) {
        val player = access.player
        if (!player.members) {
            access.mes("You need to be on a members' world to enter the Chambers of Xeric.")
            return
        }
        sessions.remove(player)?.let(::destroy)
        suspended.remove(player.username)
        val seed = player.username.hashCode().toLong() * 31L + player.currentMapClock
        val route = CoxLayout.generate(seed, challenge)
        val session =
            create(player, route, challenge, roomIndex = 0, points = 0)
                ?: run {
                    access.mes("No raid instance is currently available. Please try again shortly.")
                    return
                }
        player.diedInRaid = false
        syncHud(session)
        access.telejump(session.arena(31, 57))
        access.mes(
            if (challenge) "You begin a Challenge Mode Chambers of Xeric raid."
            else "You enter a newly scouted Chambers of Xeric raid."
        )
        access.mes("Layout: ${route.joinToString(" → ") { it.name }}")
        access.delay(2)
        spawnRoom(session)
    }

    public suspend fun leave(access: ProtectedAccess) {
        val session = sessions.remove(access.player) ?: return
        destroy(session)
        suspended.remove(access.player.username)
        clearHud(access.player)
        access.telejump(OUTSIDE)
        access.mes("You leave the Chambers of Xeric.")
    }

    public fun suspendOnLogout(player: Player) {
        val session = sessions.remove(player) ?: return
        suspended[player.username] =
            SuspendedCox(session.route, session.challenge, session.roomIndex, session.points)
        destroy(session)
        clearHud(player)
    }

    public fun resumeOnLogin(player: Player) {
        val saved = suspended.remove(player.username) ?: return
        protectedAccess.launch(player) {
            val session =
                create(player, saved.route, saved.challenge, saved.roomIndex, saved.points)
                    ?: run {
                        telejump(OUTSIDE)
                        mes("Your raid instance could not be restored.")
                        return@launch
                    }
            syncHud(session)
            telejump(session.arena(31, 48))
            mes("Your Chambers of Xeric raid resumes in ${session.room.name}.")
            delay(2)
            spawnRoom(session)
        }
    }

    public fun onDeath(player: Player) {
        val session = sessions[player] ?: return
        session.points = (session.points * 60) / 100
        player.diedInRaid = true
        syncHud(session)
        protectedAccess.launch(player) {
            delay(2)
            if (sessions[player] === session) {
                telejump(session.arena(31, 48))
                mes("You re-enter the raid. Your personal points were reduced by 40%.")
            }
        }
    }

    public fun ownerFor(npc: Npc): Player? = owners[npc]?.player

    public fun scaledMaxHit(npc: Npc, normal: Int): Int =
        if (owners[npc]?.challenge == true) normal * 3 / 2 else normal

    public fun onHit(npc: Npc, hit: Hit) {
        val session = owners[npc] ?: return
        if (hit.isFromPlayer && npc.id == CoxNpcs.vespulaPortal.id) {
            npc.engagePlayer(session.player, interactions)
        }
        if (hit.isFromPlayer && hit.damage > 0) {
            session.points += hit.damage
            syncHud(session)
        }
        when (npc.id) {
            CoxNpcs.olmHead.id -> if (!session.olmFinal) npc.hitpoints = npc.baseHitpointsLvl
            CoxNpcs.vanguardMelee.id,
            CoxNpcs.vanguardRanged.id,
            CoxNpcs.vanguardMagic.id -> enforceVanguardBalance(session)
        }
    }

    public fun onKill(player: Player, npc: Npc) {
        val session = owners.remove(npc) ?: return
        if (session.player !== player || !session.alive.remove(npc)) return
        when (npc.id) {
            CoxNpcs.babyMuttadile.id -> spawnMuttadile(session)
            CoxNpcs.vespulaPortal.id -> removeSupport(session, CoxNpcs.vespula)
            CoxNpcs.olmLeft.id -> session.olmLeftDead = true
            CoxNpcs.olmRight.id -> session.olmRightDead = true
            CoxNpcs.olmHead.id -> complete(session)
        }
        if (session.room == CoxRoom.Olm) {
            if (session.olmLeftDead && session.olmRightDead) advanceOlm(session)
            return
        }
        if (session.alive.isEmpty()) finishRoom(session)
    }

    public fun pulse(access: StandardNpcAccess) {
        val session = owners[access.npc] ?: return
        when (access.npc.id) {
            CoxNpcs.vasa.id -> pulseVasa(access, session)
            CoxNpcs.muttadile.id -> pulseMuttadile(access, session)
            CoxNpcs.olmLeft.id -> pulseOlmLeft(access, session)
        }
    }

    private fun create(
        player: Player,
        route: List<CoxRoom>,
        challenge: Boolean,
        roomIndex: Int,
        points: Int,
    ): CoxSession? {
        val template =
            RegionTemplate.create {
                copy(400, 712, 0) {
                    zoneWidth = 8
                    zoneLength = 8
                    regionZoneX = 4
                    regionZoneZ = 4
                }
            }
        val region = regions.add(template) ?: return null
        return CoxSession(player, region, route, challenge, roomIndex, points).also {
            sessions[player] = it
        }
    }

    private fun spawnRoom(session: CoxSession) {
        if (sessions[session.player] !== session) return
        clearRoomNpcs(session)
        session.player.progressHud = (session.roomIndex * 100) / max(1, session.route.lastIndex)
        session.player.mes(
            "Chambers room ${session.roomIndex + 1}/${session.route.size}: ${session.room.name}"
        )
        when (session.room) {
            CoxRoom.Tekton -> spawn(session, CoxNpcs.tekton, 31, 45)
            CoxRoom.Vasa -> {
                spawn(session, CoxNpcs.vasa, 31, 45).timer(timers.cox_pulse, 8)
                session.vasaCrystal =
                    spawn(session, CoxNpcs.vasaCrystal, 39, 43, support = true, attack = false)
            }
            CoxRoom.Vanguards -> {
                session.vanguards += spawn(session, CoxNpcs.vanguardMelee, 25, 44)
                session.vanguards += spawn(session, CoxNpcs.vanguardRanged, 31, 47)
                session.vanguards += spawn(session, CoxNpcs.vanguardMagic, 37, 44)
            }
            CoxRoom.Vespula -> {
                spawn(session, CoxNpcs.vespulaPortal, 31, 44, attack = false)
                spawn(session, CoxNpcs.vespula, 31, 48, support = true)
            }
            CoxRoom.Muttadiles -> {
                session.meatTree =
                    spawn(session, CoxNpcs.meatTree, 40, 44, support = true, attack = false)
                spawn(session, CoxNpcs.babyMuttadile, 30, 45)
            }
            CoxRoom.Shamans -> {
                spawn(session, CoxNpcs.shamanA, 27, 45)
                spawn(session, CoxNpcs.shamanB, 36, 45)
            }
            CoxRoom.Mystics -> {
                spawn(session, CoxNpcs.mysticA, 25, 45)
                spawn(session, CoxNpcs.mysticB, 31, 47)
                spawn(session, CoxNpcs.mysticC, 37, 45)
            }
            CoxRoom.Guardians -> {
                spawn(session, CoxNpcs.guardianLeft, 27, 45)
                spawn(session, CoxNpcs.guardianRight, 36, 45)
            }
            CoxRoom.IceDemon -> spawn(session, CoxNpcs.iceDemon, 31, 45)
            CoxRoom.Crabs ->
                resolvePuzzle(
                    session,
                    "You redirect the coloured crystal through the jewelled crabs.",
                    1_200,
                )
            CoxRoom.Tightrope ->
                resolvePuzzle(session, "You cross the tightrope and recover the keystone.", 1_200)
            CoxRoom.Thieving ->
                resolvePuzzle(session, "You feed enough cavern grubs into the trough.", 1_200)
            CoxRoom.Prep -> prepare(session)
            CoxRoom.Olm -> spawnOlm(session)
        }
    }

    private fun spawn(
        session: CoxSession,
        type: NpcType,
        localX: Int,
        localZ: Int,
        support: Boolean = false,
        attack: Boolean = true,
    ): Npc {
        val npc = Npc(npcTypes[type], session.arena(localX, localZ))
        val scale = if (session.challenge) 1.5 else 1.0
        npc.baseHitpointsLvl = max(1, (npc.baseHitpointsLvl * scale).toInt())
        npc.hitpoints = npc.baseHitpointsLvl
        npcs.add(npc, NPC_LIFETIME)
        owners[npc] = session
        if (support) session.support += npc else session.alive += npc
        if (attack) npc.engagePlayer(session.player, interactions)
        return npc
    }

    private fun resolvePuzzle(session: CoxSession, message: String, points: Int) {
        session.points += points
        syncHud(session)
        protectedAccess.launch(session.player) {
            mes(message)
            delay(3)
            if (sessions[player] === session) finishRoom(session)
        }
    }

    private fun prepare(session: CoxSession) {
        session.player.invAddOrDrop(objRepo, CoxObjs.xericsAid, 2)
        session.player.invAddOrDrop(objRepo, CoxObjs.prayerEnhance, 2)
        session.player.invAddOrDrop(objRepo, CoxObjs.overload, 2)
        protectedAccess.launch(session.player) {
            mes("The resource room supplies raid potions for the battle ahead.")
            delay(3)
            if (sessions[player] === session) finishRoom(session)
        }
    }

    private fun finishRoom(session: CoxSession) {
        if (session.room == CoxRoom.Olm || session.roomIndex >= session.route.lastIndex) return
        session.points += 1_000
        syncHud(session)
        protectedAccess.launch(session.player) {
            delay(3)
            if (sessions[player] !== session || session.alive.isNotEmpty()) return@launch
            session.roomIndex++
            telejump(session.arena(31, if (session.room == CoxRoom.Olm) 39 else 48))
            spawnRoom(session)
        }
    }

    private fun spawnMuttadile(session: CoxSession) {
        spawn(session, CoxNpcs.muttadile, 31, 45).timer(timers.cox_pulse, 5)
    }

    private fun pulseVasa(access: StandardNpcAccess, session: CoxSession) {
        val crystal = session.vasaCrystal
        if (
            crystal != null &&
                crystal.hitpoints > 0 &&
                access.npc.hitpoints < access.npc.baseHitpointsLvl / 2
        ) {
            access.anim(CoxSeqs.vasaAttack)
            access.npc.hitpoints =
                min(
                    access.npc.baseHitpointsLvl,
                    access.npc.hitpoints + max(1, access.npc.baseHitpointsLvl / 100),
                )
        }
        access.timer(timers.cox_pulse, 2)
    }

    private fun pulseMuttadile(access: StandardNpcAccess, session: CoxSession) {
        val tree = session.meatTree
        if (
            tree != null &&
                tree.hitpoints > 0 &&
                access.npc.hitpoints < access.npc.baseHitpointsLvl * 55 / 100
        ) {
            access.walk(tree.coords)
            access.npc.hitpoints =
                min(
                    access.npc.baseHitpointsLvl,
                    access.npc.hitpoints + max(1, access.npc.baseHitpointsLvl / 10),
                )
        }
        access.timer(timers.cox_pulse, 5)
    }

    private fun enforceVanguardBalance(session: CoxSession) {
        val living = session.vanguards.filter { it.hitpoints > 0 && it.slotId != INVALID_SLOT }
        if (living.size != 3) return
        val high = living.maxOf(Npc::hitpoints)
        val low = living.minOf(Npc::hitpoints)
        if (high - low > living.first().baseHitpointsLvl / 3) {
            living.forEach { it.hitpoints = it.baseHitpointsLvl }
            session.player.mes("The Vanguards reset after their health becomes unbalanced.")
        }
    }

    private fun spawnOlm(session: CoxSession) {
        session.player.mes("The Great Olm rises from the depths.")
        session.olmPhase = 1
        session.olmFinal = false
        spawnOlmParts(session, includeHead = true)
    }

    private fun spawnOlmParts(session: CoxSession, includeHead: Boolean) {
        session.olmLeftDead = false
        session.olmRightDead = false
        session.olmRight = spawn(session, CoxNpcs.olmRight, 36, 26, attack = false)
        session.olmLeft =
            spawn(session, CoxNpcs.olmLeft, 25, 26, attack = false).also {
                it.timer(timers.cox_pulse, 8)
            }
        if (includeHead) {
            session.olmHead =
                spawn(session, CoxNpcs.olmHead, 30, 27, support = true, attack = false).also {
                    it.timer(timers.cox_pulse, 4)
                }
        }
    }

    private fun advanceOlm(session: CoxSession) {
        if (session.olmTransitioning) return
        session.olmTransitioning = true
        if (session.olmPhase >= OLM_HAND_PHASES) {
            session.olmFinal = true
            session.olmHead?.let { head ->
                session.support.remove(head)
                session.alive += head
            }
            session.points += 5_000
            syncHud(session)
            session.player.mes("The Great Olm enters its final stand!")
            session.olmTransitioning = false
            return
        }
        protectedAccess.launch(session.player) {
            mes("The Great Olm retreats and resurfaces with another power.")
            delay(5)
            if (sessions[player] !== session) return@launch
            session.olmPhase++
            session.olmTransitioning = false
            spawnOlmParts(session, includeHead = false)
        }
    }

    private fun pulseOlmLeft(access: StandardNpcAccess, session: CoxSession) {
        val right = session.olmRight
        if (
            session.olmPhase >= OLM_HAND_PHASES &&
                right != null &&
                right.hitpoints > 0 &&
                access.npc.hitpoints < access.npc.baseHitpointsLvl / 2
        ) {
            access.anim(CoxSeqs.olmLeftHeal)
            access.npc.hitpoints = min(access.npc.baseHitpointsLvl, access.npc.hitpoints + 10)
            right.hitpoints = min(right.baseHitpointsLvl, right.hitpoints + 10)
        }
        access.timer(timers.cox_pulse, 8)
    }

    private fun complete(session: CoxSession) {
        session.points += 10_000
        session.player.completeDiaryTask(DiaryTask.CompleteChambers)
        session.player.completions++
        if (session.challenge) session.player.challengeCompletions++
        val unique = rollUnique(session)
        protectedAccess.launch(session.player) {
            delay(5)
            if (sessions[player] !== session) return@launch
            sessions.remove(player)
            destroy(session)
            clearHud(player)
            telejump(OUTSIDE)
            if(unique!=null)player.invAddOrDrop(objRepo,unique,1)
            else {
                val tablet = vars[CoxVarps.honour]==0 && CoxObjs.ancientTablet !in inv && CoxObjs.ancientTablet !in bank && random.of(10)==0
                val available=CoxObjs.common.toMutableList()
                repeat(2){ roll ->
                    if(roll==0 && tablet)player.invAddOrDrop(objRepo,CoxObjs.ancientTablet,1)
                    else {
                        val drop=available.removeAt(random.of(available.size))
                        val count=if(drop.divisor==0)1 else (session.points.coerceAtMost(131071)/drop.divisor).coerceAtLeast(1)
                        val seed=drop.seed
                        if(seed!=null && random.of(3)==0)player.invAddOrDrop(objRepo,seed,max(1,count/drop.herbsPerSeed))
                        else player.invAddOrDrop(objRepo,drop.item,count)
                    }
                }
            }
            mes("Chambers of Xeric complete! Personal points: ${session.points}.")
            if (unique != null) mes("<col=ff00ff>You receive a unique reward!</col>")
            publish(
                CombatAchievementEvents.EncounterCompleted(
                    player = player,
                    encounter = "Chambers of Xeric",
                    completionCount =
                        if (session.challenge) player.challengeCompletions else player.completions,
                    mode = if (session.challenge) "Challenge Mode" else "Normal Mode",
                )
            )
        }
    }

    private fun rollUnique(session: CoxSession): ObjType? {
        val capped = min(session.points, 570_000)
        if (lootRandom.of(867_600) >= capped) return null
        var roll=lootRandom.of(69)
        for((reward,weight) in UNIQUES){if(roll<weight)return reward;roll-=weight}
        error("Invalid Chambers unique roll")
    }

    private fun removeSupport(session: CoxSession, type: NpcType) {
        session.support
            .filter { it.id == type.id }
            .toList()
            .forEach { npc ->
                session.support.remove(npc)
                owners.remove(npc)
                if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
            }
    }

    private fun clearRoomNpcs(session: CoxSession) {
        (session.alive + session.support).toList().forEach { npc ->
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        session.alive.clear()
        session.support.clear()
        session.vanguards.clear()
    }

    private fun destroy(session: CoxSession) = clearRoomNpcs(session)

    private fun syncHud(session: CoxSession) {
        session.player.inRaid = true
        session.player.challengeHud = session.challenge
        session.player.partySizeHud = 1
        session.player.playerScore = session.points
        session.player.partyScoreHud = session.points.coerceAtMost(131_071)
        session.player.progressHud = (session.roomIndex * 100) / max(1, session.route.lastIndex)
    }

    private fun clearHud(player: Player) {
        player.inRaid = false
        player.challengeHud = false
        player.partySizeHud = 0
        player.progressHud = 0
        player.partyScoreHud = 0
        player.playerScore = 0
        player.diedInRaid = false
    }

    public companion object {
        public val OUTSIDE: CoordGrid = CoordGrid(1232, 3573, 0)
        private const val NPC_LIFETIME = 200_000
        private const val OLM_HAND_PHASES = 3
        private val UNIQUES =
            listOf(
                CoxObjs.dexterousScroll to 20,
                CoxObjs.arcaneScroll to 20,
                CoxObjs.buckler to 4,
                CoxObjs.hunterCrossbow to 4,
                CoxObjs.dragonClaws to 3,
                CoxObjs.ancestralHat to 3,
                CoxObjs.ancestralTop to 3,
                CoxObjs.ancestralBottom to 3,
                CoxObjs.bulwark to 3,
                CoxObjs.kodaiInsignia to 2,
                CoxObjs.elderMaul to 2,
                CoxObjs.twistedBow to 2,
            )
    }
}

public data class CoxSession(
    val player: Player,
    val region: Region,
    val route: List<CoxRoom>,
    val challenge: Boolean,
    var roomIndex: Int,
    var points: Int,
    val alive: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val support: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val vanguards: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    var vasaCrystal: Npc? = null,
    var meatTree: Npc? = null,
    var olmPhase: Int = 0,
    var olmFinal: Boolean = false,
    var olmTransitioning: Boolean = false,
    var olmLeftDead: Boolean = false,
    var olmRightDead: Boolean = false,
    var olmLeft: Npc? = null,
    var olmRight: Npc? = null,
    var olmHead: Npc? = null,
) {
    public val room: CoxRoom
        get() = route[roomIndex]

    public fun arena(localX: Int, localZ: Int): CoordGrid = region.normal[0, 50, 89, localX, localZ]
}

private data class SuspendedCox(
    val route: List<CoxRoom>,
    val challenge: Boolean,
    val roomIndex: Int,
    val points: Int,
)
