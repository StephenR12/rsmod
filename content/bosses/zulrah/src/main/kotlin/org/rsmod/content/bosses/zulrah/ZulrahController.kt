package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.queues
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Singleton
public class ZulrahController
@Inject
constructor(
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val locs: LocRepository,
    private val locTypes: LocTypeList,
    private val interactions: AiPlayerInteractions,
    private val protectedAccess: ProtectedAccessLauncher,
    private val random: GameRandom,
) {
    private val sessions = IdentityHashMap<Player, ZulrahSession>()
    private val owners = IdentityHashMap<Npc, ZulrahSession>()
    private val returnOnLogin = mutableSetOf<String>()

    public suspend fun enter(access: ProtectedAccess) {
        val player = access.player
        if (!player.members) {
            access.mes("You need to be on a members' world to fight Zulrah.")
            return
        }
        sessions.remove(player)?.let(::destroy)
        val session =
            create(player)
                ?: run {
                    access.mes("Zulrah's shrine is currently full. Please try again shortly.")
                    return
                }
        access.telejump(session.arena(PLAYER_START))
        access.mes("The shrine is eerily quiet...")
        spawnBoss(session, hitpoints = HITPOINTS)
    }

    public suspend fun leave(access: ProtectedAccess) {
        val session = sessions.remove(access.player) ?: return
        destroy(session)
        access.telejump(OUTSIDE)
        access.mes("You return to Zul-Andra.")
    }

    public fun sessionFor(npc: Npc): ZulrahSession? = owners[npc]

    public fun sessionFor(player: Player): ZulrahSession? = sessions[player]

    public fun ownerFor(npc: Npc): Player? = owners[npc]?.player

    public fun phaseFor(npc: Npc): ZulrahPhase? = owners[npc]?.phase

    public fun isBoss(npc: Npc): Boolean = owners[npc]?.boss === npc

    public fun consumeBossAttack(npc: Npc): ZulrahPhase? {
        val session = owners[npc] ?: return null
        if (session.boss !== npc || session.completed || session.transitioning) {
            return null
        }
        session.attacksRemaining--
        if (session.attacksRemaining <= 0 && queues.generic_queue1 !in npc.queueList) {
            session.transitioning = true
            npc.queue(queues.generic_queue1, 1)
        }
        return session.phase
    }

    public fun alternateJadStyle(npc: Npc): Boolean {
        val session = owners[npc] ?: return false
        val magic = session.nextJadMagic
        session.nextJadMagic = !session.nextJadMagic
        return magic
    }

    public fun isStandingInCloud(npc: Npc, player: Player): Boolean {
        val session = owners[npc] ?: return false
        return session.player === player && player.coords in session.clouds
    }

    /** Called after the old form has finished its sink animation. */
    public fun advance(old: Npc) {
        val session = owners[old] ?: return
        if (session.boss !== old || session.completed) {
            return
        }
        val hitpoints = old.hitpoints.coerceAtLeast(1)
        owners.remove(old)
        session.npcs.remove(old)
        session.phaseIndex = (session.phaseIndex + 1) % session.rotation.size
        session.attacksRemaining = session.phase.attacks
        session.nextJadMagic = true
        session.transitioning = false
        spawnBoss(session, hitpoints)
        if (old.slotId != INVALID_SLOT) {
            npcs.del(old, Int.MAX_VALUE)
        }
    }

    public fun onKill(player: Player, npc: Npc) {
        val session = owners[npc] ?: return
        if (session.player !== player || session.boss !== npc || session.completed) {
            return
        }
        session.completed = true
        session.clouds.clear()
        session.player.mes(
            "Zulrah has been defeated. Collect your loot and use the scroll to leave."
        )
        session.npcs.filterNot { it === npc }.toList().forEach { removeNpc(it) }
    }

    public fun removeNpc(npc: Npc) {
        val session = owners.remove(npc)
        session?.npcs?.remove(npc)
        if (session?.boss === npc) {
            session.boss = null
        }
        if (npc.slotId != INVALID_SLOT) {
            npcs.del(npc, Int.MAX_VALUE)
        }
    }

    public fun failAfterDeath(player: Player) {
        val session = sessions.remove(player) ?: return
        destroy(session)
        protectedAccess.launch(player) {
            telejump(OUTSIDE)
            mes("Zulrah has bested you.")
        }
    }

    public fun onLogout(player: Player) {
        val session = sessions.remove(player) ?: return
        destroy(session)
        returnOnLogin += player.username
    }

    public fun onLogin(player: Player) {
        if (!returnOnLogin.remove(player.username)) {
            return
        }
        protectedAccess.launch(player) {
            telejump(OUTSIDE)
            mes("Your Zulrah encounter ended when you logged out.")
        }
    }

    private fun create(player: Player): ZulrahSession? {
        val template =
            RegionTemplate.create {
                copy(ARENA_ZONE_X, ARENA_ZONE_Z, 0) {
                    zoneWidth = ARENA_ZONE_WIDTH
                    zoneLength = ARENA_ZONE_LENGTH
                    regionZoneX = 4
                    regionZoneZ = 4
                }
            }
        val region = regions.add(template) ?: return null
        val rotation = ZulrahRotations.all[random.of(ZulrahRotations.all.size)]
        return ZulrahSession(player, region, rotation).also { sessions[player] = it }
    }

    private fun spawnBoss(session: ZulrahSession, hitpoints: Int) {
        if (sessions[session.player] !== session) {
            return
        }
        session.clouds.clear()
        val phase = session.phase
        val boss = spawn(session, phase.form.npc, session.arena(phase.position.normalCoords))
        boss.apply {
            attackLvl = COMBAT_LEVEL
            baseAttackLvl = COMBAT_LEVEL
            strengthLvl = COMBAT_LEVEL
            baseStrengthLvl = COMBAT_LEVEL
            defenceLvl = DEFENCE_LEVEL
            baseDefenceLvl = DEFENCE_LEVEL
            rangedLvl = COMBAT_LEVEL
            baseRangedLvl = COMBAT_LEVEL
            magicLvl = COMBAT_LEVEL
            baseMagicLvl = COMBAT_LEVEL
            this.hitpoints = hitpoints
            baseHitpointsLvl = HITPOINTS
            setHunt(HUNT_RANGE)
            setHuntMode(huntmodes.constant_ranged)
            anim(ZulrahSeqs.emerge)
        }
        session.boss = boss
        if (phase.clouds) {
            spawnClouds(session, phase.position)
        }
        if (phase.snakelings) {
            spawnSnakelings(session)
        }
        boss.opPlayer2(session.player, interactions)
    }

    private fun spawnClouds(session: ZulrahSession, position: ZulrahPosition) {
        val normalCoords = CLOUD_PATTERNS.getValue(position)
        for (normal in normalCoords) {
            val coords = session.arena(normal)
            session.clouds += coords
            locs.add(
                coords,
                locTypes[ZulrahLocs.poisonCloud],
                CLOUD_DURATION,
                LocAngle.North,
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun spawnSnakelings(session: ZulrahSession) {
        val types = listOf(ZulrahNpcs.meleeSnakeling, ZulrahNpcs.magicSnakeling)
        SNAKELING_SPAWNS.zip(types).forEach { (normal, type) ->
            val snakeling = spawn(session, type, session.arena(normal))
            snakeling.apply {
                attackLvl = 100
                baseAttackLvl = 100
                strengthLvl = 100
                baseStrengthLvl = 100
                rangedLvl = 100
                baseRangedLvl = 100
                magicLvl = 100
                baseMagicLvl = 100
                defenceLvl = 1
                baseDefenceLvl = 1
                hitpoints = 1
                baseHitpointsLvl = 1
                setHunt(HUNT_RANGE)
                setHuntMode(huntmodes.constant_ranged)
            }
            snakeling.opPlayer2(session.player, interactions)
        }
    }

    private fun spawn(session: ZulrahSession, type: NpcType, coords: CoordGrid): Npc {
        val npc = Npc(npcTypes[type], coords)
        npcs.add(npc, NPC_LIFETIME)
        session.npcs += npc
        owners[npc] = session
        return npc
    }

    private fun destroy(session: ZulrahSession) {
        session.npcs.toList().forEach(::removeNpc)
        session.npcs.clear()
        session.clouds.clear()
    }

    public companion object {
        public val OUTSIDE: CoordGrid = CoordGrid(2200, 3055, 0)
        public val PLAYER_START: CoordGrid = CoordGrid(2268, 3069, 0)
        public const val HITPOINTS: Int = 500
        public const val COMBAT_LEVEL: Int = 300
        public const val DEFENCE_LEVEL: Int = 300
        public const val HUNT_RANGE: Int = 20

        private const val ARENA_ZONE_X = 282
        private const val ARENA_ZONE_Z = 382
        private const val ARENA_ZONE_WIDTH = 4
        private const val ARENA_ZONE_LENGTH = 4
        private const val NPC_LIFETIME = 100_000
        private const val CLOUD_DURATION = 40

        private val SNAKELING_SPAWNS = listOf(CoordGrid(2262, 3068, 0), CoordGrid(2273, 3068, 0))

        private val CLOUD_PATTERNS =
            mapOf(
                ZulrahPosition.North to
                    listOf(
                        CoordGrid(2261, 3070, 0),
                        CoordGrid(2262, 3070, 0),
                        CoordGrid(2273, 3070, 0),
                        CoordGrid(2274, 3070, 0),
                    ),
                ZulrahPosition.South to
                    listOf(
                        CoordGrid(2261, 3067, 0),
                        CoordGrid(2262, 3067, 0),
                        CoordGrid(2273, 3067, 0),
                        CoordGrid(2274, 3067, 0),
                    ),
                ZulrahPosition.West to
                    listOf(
                        CoordGrid(2264, 3066, 0),
                        CoordGrid(2264, 3067, 0),
                        CoordGrid(2271, 3070, 0),
                        CoordGrid(2271, 3071, 0),
                    ),
                ZulrahPosition.East to
                    listOf(
                        CoordGrid(2264, 3070, 0),
                        CoordGrid(2264, 3071, 0),
                        CoordGrid(2271, 3066, 0),
                        CoordGrid(2271, 3067, 0),
                    ),
            )
    }
}

public data class ZulrahSession(
    val player: Player,
    val region: Region,
    val rotation: List<ZulrahPhase>,
    var phaseIndex: Int = 0,
    var attacksRemaining: Int = rotation.first().attacks,
    var nextJadMagic: Boolean = true,
    var transitioning: Boolean = false,
    var completed: Boolean = false,
    var boss: Npc? = null,
    val npcs: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val clouds: MutableSet<CoordGrid> = mutableSetOf(),
) {
    public val phase: ZulrahPhase
        get() = rotation[phaseIndex]

    public fun arena(normalCoords: CoordGrid): CoordGrid = region.normal[normalCoords]
}
