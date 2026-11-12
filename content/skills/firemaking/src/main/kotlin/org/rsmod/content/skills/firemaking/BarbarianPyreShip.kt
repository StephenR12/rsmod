package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.loc.LocLayerConstants

public class BarbarianPyreShip
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val npcTypes: NpcTypeList,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val random: GameRandom,
) {
    private val activeStations = mutableSetOf<Int>()
    private val ferociousSpiritByPlayer = mutableMapOf<PlayerUid, Npc>()

    public suspend fun construct(access: ProtectedAccess, site: BoundLocInfo): Boolean {
        with(access) {
            interruptFiremakingQueues()
            if (!isCurrent(site)) {
                return false
            }
            if (!player.members) {
                mes("You need to be on a members' world to construct a pyre ship.")
                return false
            }
            if (player.vars[varbits.brut_craft_ship] < TRAINING_STARTED) {
                mes("You must begin the relevant section of Otto's barbarian training first.")
                return false
            }
            val station = stationFor(site)
            if (!activeStations.add(station.key)) {
                mes("This pyre site is currently in use.")
                return false
            }

            var currentStage: LocInfo? = null
            try {
                val existingSpirit = ferociousSpiritByPlayer[player.uid]
                if (existingSpirit?.isSlotAssigned == true && existingSpirit.isVisible) {
                    mes("You must defeat the barbarian spirit first.")
                    return false
                }
                if (existingSpirit != null) {
                    ferociousSpiritByPlayer.remove(player.uid)
                }

                val bones = selectBones(player.inv)
                if (bones == null) {
                    mes("You need chewed bones or mangled bones to construct a pyre ship.")
                    return false
                }
                if (!hasFiremakingTool(player.inv)) {
                    mes("You need a tinderbox or a suitable bow to light the pyre ship.")
                    return false
                }
                val axe = selectAxe(player.inv)
                if (axe == null) {
                    mes("You need a woodcutting axe to carve the pyre ship.")
                    return false
                }
                val log = BarbarianPyreLogs.all.firstOrNull { player.inv.contains(it.log) }
                if (log == null) {
                    mes("You need some suitable logs to construct a pyre ship.")
                    return false
                }
                if (player.craftingLvl < log.level) {
                    mes("You need a Crafting level of ${log.level} to use these logs.")
                    return false
                }
                if (player.firemakingLvl < log.level) {
                    mes("You need a Firemaking level of ${log.level} to use these logs.")
                    return false
                }

                faceLoc(site)
                anim(axe.carveSequence)
                currentStage = setStage(station, firemaking_locs.barbarianPyreStage1)
                delay(CONSTRUCTION_STAGE_TICKS)

                anim(axe.carveSequence)
                currentStage = setStage(station, firemaking_locs.barbarianPyreStage2)
                delay(CONSTRUCTION_STAGE_TICKS)

                anim(axe.carveSequence)
                currentStage = setStage(station, firemaking_locs.barbarianPyreStage3)
                delay(CONSTRUCTION_STAGE_TICKS)

                resetAnim()
                currentStage = setStage(station, firemaking_locs.barbarianPyreBones)
                delay(CONSTRUCTION_STAGE_TICKS)

                if (!player.inv.contains(log.log) || !player.inv.contains(bones)) {
                    mes("You no longer have the materials required for this pyre ship.")
                    return false
                }
                val deletedLog = invDel(player.inv, log.log, count = 1)
                if (deletedLog.failure) {
                    return false
                }
                val deletedBones = invDel(player.inv, bones, count = 1)
                if (deletedBones.failure) {
                    player.invAdd(player.inv, log.log)
                    return false
                }

                statAdvance(stats.crafting, log.craftingXp)
                statAdvance(stats.firemaking, log.firemakingXp)
                currentStage = setStage(station, firemaking_locs.barbarianPyreFire)

                if (bones == objs.chewed_bones) {
                    completeChewedSacrifice(log, station)
                } else {
                    spawnFerociousSpirit(station)
                }

                delay(seqs.brut_ascending_spirit)
                currentStage = setStage(station, firemaking_locs.barbarianPyreLaunched)
                delay(LAUNCH_STAGE_TICKS)
                currentStage = setStage(station, firemaking_locs.barbarianPyreReady)
                delay(LAUNCH_STAGE_TICKS)
                currentStage = setStage(station, firemaking_locs.barbarianPyreSink)
                delay(seqs.brut_pyre_boat_sink)
                return true
            } finally {
                resetAnim()
                currentStage?.let { locRepo.del(it, duration = Int.MAX_VALUE) }
                activeStations.remove(station.key)
            }
        }
    }

    public fun rollChewedReward(): RolledBarbarianPyreReward {
        if (random.of(DRAGON_FULL_HELM_DENOMINATOR) == 0) {
            return RolledBarbarianPyreReward(objs.dragon_full_helm, 1)
        }
        val reward = BarbarianPyreRewards.at(random.of(BarbarianPyreRewards.MAIN_ROLL_WEIGHT))
        return RolledBarbarianPyreReward(reward.obj, random.of(reward.count))
    }

    private fun ProtectedAccess.completeChewedSacrifice(
        log: BarbarianPyreLog,
        station: BarbarianPyreStation,
    ) {
        val spirit = Npc(npcTypes[npcs.brut_ascending_spirit], station.spiritCoords)
        spirit.playerFace(player)
        spirit.anim(seqs.brut_ascending_spirit)
        npcRepo.add(spirit, duration = PEACEFUL_SPIRIT_DURATION_TICKS)

        val currentCharges = player.vars[varbits.brut_prayer_total]
        vars[varbits.brut_prayer_total] =
            (currentCharges + log.prayerCharges).coerceAtMost(MAX_PRAYER_CHARGES)
        if (player.vars[varbits.brut_craft_ship] == TRAINING_STARTED) {
            vars[varbits.brut_craft_ship] = TRAINING_SACRIFICED
        }
        if (log.log == objs.magic_logs) {
            val current = player.vars[varps.kandarin_achievement_diary2]
            vars[varps.kandarin_achievement_diary2] = current or KANDARIN_ELITE_PYRE_BIT
        }

        val reward = rollChewedReward()
        val added = player.invAdd(player.inv, reward.obj, reward.count)
        if (added.failure) {
            objRepo.add(
                reward.obj,
                player.coords,
                duration = REWARD_GROUND_DURATION_TICKS,
                receiver = player,
                count = reward.count,
                reveal = REWARD_GROUND_DURATION_TICKS,
            )
        }
        mes("The ancient barbarian is laid to rest. Your future prayer training is blessed,")
        mes("as his spirit ascends to a glorious afterlife. Spirits drop an object into your")
        mes("pack.")
    }

    private fun ProtectedAccess.spawnFerociousSpirit(station: BarbarianPyreStation) {
        val spirit = Npc(npcTypes[npcs.brut_angry_spirit_bad], station.spiritCoords)
        spirit.playerFace(player)
        spirit.anim(seqs.brut_angry_spirit_appear)
        npcRepo.add(spirit, duration = FEROCIOUS_SPIRIT_DURATION_TICKS)
        ferociousSpiritByPlayer[player.uid] = spirit
        spirit.opPlayer2(player, aiPlayerInteractions)
    }

    private fun setStage(station: BarbarianPyreStation, type: LocType): LocInfo {
        val entity = LocEntity(type.id, LocShape.CentrepieceStraight.id, station.angle.id)
        val next =
            LocInfo(LocLayerConstants.of(LocShape.CentrepieceStraight.id), station.coords, entity)
        check(locRepo.add(next, duration = Int.MAX_VALUE)) {
            "Could not set barbarian pyre stage ${type.id} at ${station.coords}."
        }
        return next
    }

    private fun stationFor(site: BoundLocInfo): BarbarianPyreStation =
        REAL_STATIONS[site.coords.packed]
            ?: BarbarianPyreStation(
                site.coords,
                site.angle,
                site.adjustedCentre,
                site.coords.packed,
            )

    private fun selectBones(inv: Inventory): ObjType? =
        when {
            inv.contains(objs.chewed_bones) -> objs.chewed_bones
            inv.contains(objs.mangled_bones) -> objs.mangled_bones
            else -> null
        }

    private fun hasFiremakingTool(inv: Inventory): Boolean =
        inv.contains(objs.tinderbox) || BowFiremakingTools.all.any { inv.contains(it.bow) }

    private fun selectAxe(inv: Inventory): BarbarianPyreAxe? =
        BarbarianPyreAxes.all.firstOrNull { inv.contains(it.obj) }

    private fun isCurrent(site: BoundLocInfo): Boolean =
        locRepo.findAll(site.coords).any { it.entity == site.entity }

    private fun ProtectedAccess.interruptFiremakingQueues() {
        clearWeakQueue(firemaking_queues.attempt)
        clearWeakQueue(firemaking_queues.pyreLogPreparation)
    }

    public companion object {
        public const val TRAINING_STARTED: Int = 1
        public const val TRAINING_SACRIFICED: Int = 2
        public const val TRAINING_COMPLETE: Int = 3
        public const val MAX_PRAYER_CHARGES: Int = 200
        public const val DRAGON_FULL_HELM_DENOMINATOR: Int = 256
        public const val CONSTRUCTION_STAGE_TICKS: Int = 4
        public const val LAUNCH_STAGE_TICKS: Int = 4
        public const val PEACEFUL_SPIRIT_DURATION_TICKS: Int = 20
        public const val FEROCIOUS_SPIRIT_DURATION_TICKS: Int = 300
        public const val REWARD_GROUND_DURATION_TICKS: Int = 500
        public const val KANDARIN_ELITE_PYRE_BIT: Int = 1 shl 10

        private val REAL_STATIONS: Map<Int, BarbarianPyreStation> = buildMap {
            fun pair(
                first: CoordGrid,
                second: CoordGrid,
                station: CoordGrid,
                angle: LocAngle,
                spirit: CoordGrid = station,
            ) {
                val key = station.packed
                put(first.packed, BarbarianPyreStation(station, angle, spirit, key))
                put(second.packed, BarbarianPyreStation(station, angle, spirit, key))
            }
            pair(
                CoordGrid(2519, 3519, 0),
                CoordGrid(2518, 3519, 0),
                CoordGrid(2518, 3521, 0),
                LocAngle.North,
            )
            pair(
                CoordGrid(2507, 3518, 0),
                CoordGrid(2506, 3518, 0),
                CoordGrid(2506, 3520, 0),
                LocAngle.North,
            )
            pair(
                CoordGrid(2503, 3499, 0),
                CoordGrid(2503, 3498, 0),
                CoordGrid(2503, 3498, 0),
                LocAngle.West,
                CoordGrid(2505, 3498, 0),
            )
        }
    }
}

public data class BarbarianPyreStation(
    val coords: CoordGrid,
    val angle: LocAngle,
    val spiritCoords: CoordGrid,
    val key: Int,
)

public data class RolledBarbarianPyreReward(val obj: ObjType, val count: Int)
