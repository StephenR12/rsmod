package org.rsmod.content.travel.mausoleumbridge

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.privateLocChange
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.loc.LocLayerConstants

/** Poll 85 repair and Agility-shortcut behavior for Fenkenstrain's Mausoleum bridge. */
public class MausoleumBridge
@Inject
constructor(private val locTypes: LocTypeList, private val objTypes: ObjTypeList) {
    private var ProtectedAccess.repairState by intVarp(MausoleumBridgeVarps.repairState)
    private val previousUnrepairedTile = WeakHashMap<Player, CoordGrid>()

    public fun sync(player: Player) {
        val state = player.vars[MausoleumBridgeVarps.repairState]
        val caveReached = player.vars[varbits.fenk_unlocked_cavern] != 0
        val mainland: LocType
        val island: LocType
        when (state) {
            REPAIRED -> {
                mainland = MausoleumBridgeLocs.mainlandRepaired
                island = MausoleumBridgeLocs.islandRepaired
            }
            ISLAND_REINFORCED -> {
                mainland = MausoleumBridgeLocs.mainlandBuild
                island = MausoleumBridgeLocs.islandUnrepaired
            }
            else -> {
                mainland = MausoleumBridgeLocs.mainlandUnrepaired
                island =
                    if (caveReached) {
                        MausoleumBridgeLocs.islandBuild
                    } else {
                        MausoleumBridgeLocs.islandUnrepaired
                    }
            }
        }
        player.privateLocChange(bridgeLoc(MAINLAND_SIDE, mainland))
        player.privateLocChange(bridgeLoc(ISLAND_SIDE, island))
    }

    public suspend fun build(access: ProtectedAccess, bridge: BoundLocInfo) {
        with(access) {
            val side = buildSide(bridge) ?: return
            if (!canBuildSide(side)) {
                sync(player)
                return
            }
            if (!player.members) {
                mes("You need to be on a members' world to build this bridge.")
                return
            }
            if (stat(stats.construction) < CONSTRUCTION_LEVEL) {
                mes("You need a Construction level of 59 to build this bridge.")
                return
            }
            if (!hasHammer(player)) {
                mes("You need a hammer to build this bridge.")
                return
            }
            val nails = nailPlan(player)
            if (player.inv.count(objTypes[objs.teak_plank]) < PLANKS_PER_SIDE || nails == null) {
                mes(MISSING_MATERIALS_MESSAGE)
                return
            }

            anim(hammerAnimation(player))
            delay(BUILD_TICKS)
            if (!isWithinDistance(bridge, 1) || !canBuildSide(side)) {
                resetAnim()
                return
            }
            val refreshedNails = nailPlan(player)
            if (
                !hasHammer(player) ||
                    player.inv.count(objTypes[objs.teak_plank]) < PLANKS_PER_SIDE ||
                    refreshedNails == null
            ) {
                resetAnim()
                return
            }
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.teak_plank.id, PLANKS_PER_SIDE)
                    for ((nail, count) in refreshedNails) {
                        delete(inventory, nail.id, count)
                    }
                }
            check(transaction.success) { "Mausoleum bridge material transaction failed." }

            statAdvance(stats.construction, CONSTRUCTION_XP_PER_SIDE)
            if (side == Side.Island) {
                anim(seqs.emote_cheer)
                mes(COLLAPSE_MESSAGE)
                repairState = ISLAND_REINFORCED
                sync(player)
                mes(FIRST_SIDE_MESSAGE)
                delay(1)
            } else {
                repairState = REPAIRED
                sync(player)
                mes(FINAL_SIDE_MESSAGE)
            }
        }
    }

    public suspend fun cross(access: ProtectedAccess, bridge: BoundLocInfo) {
        with(access) {
            if (repairState != REPAIRED) {
                unrepairedCross(bridge)
                return
            }
            if (stat(stats.agility) < AGILITY_LEVEL) {
                mes("You need an Agility level of 69 to use this shortcut.")
                return
            }
            previousUnrepairedTile.remove(player)
            val northbound = bridge.coords.z <= BRIDGE_CENTRE_Z
            val destination = if (northbound) ISLAND_ARRIVAL else MAINLAND_ARRIVAL
            anim(seqs.human_longjump)
            exactMove(
                start = coords,
                end = destination,
                delay1 = 0,
                delay2 = JUMP_CLIENT_CYCLES,
                dir = if (northbound) Constants.em_face_north else Constants.em_face_south,
            )
            delay(JUMP_TICKS)
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.unrepairedCross(bridge: BoundLocInfo) {
        val previous = previousUnrepairedTile.put(player, bridge.coords)
        if (previous == null || previous == bridge.coords) {
            return
        }
        camModeFixed()
        camMoveTo(CINEMATIC_CAMERA, height = 650, rate = 8, rate2 = 20)
        camLookAt(MAUSOLEUM, height = 120, rate = 8, rate2 = 20)
        delay(CINEMATIC_TICKS)
        camReset()
        camModeReset()
        previousUnrepairedTile.remove(player)
    }

    private fun ProtectedAccess.canBuildSide(side: Side): Boolean =
        when (side) {
            Side.Island ->
                repairState == UNREPAIRED && player.vars[varbits.fenk_unlocked_cavern] != 0
            Side.Mainland -> repairState == ISLAND_REINFORCED
        }

    private fun buildSide(bridge: BoundLocInfo): Side? =
        when (bridge.id) {
            MausoleumBridgeLocs.islandBuild.id -> Side.Island
            MausoleumBridgeLocs.mainlandBuild.id -> Side.Mainland
            else -> null
        }

    private fun bridgeLoc(coords: CoordGrid, type: LocType): LocInfo =
        LocInfo(
            LocLayerConstants.of(LocShape.GroundDecor.id),
            coords,
            LocEntity(type.id, LocShape.GroundDecor.id, LocAngle.East.id),
        )

    private fun hasHammer(player: Player): Boolean =
        player.inv.any { it?.id in HAMMERS } || player.worn.any { it?.id in HAMMERS }

    private fun hammerAnimation(player: Player) =
        if (
            player.inv.contains(objs.imcando_hammer) ||
                player.worn.contains(objs.imcando_hammer) ||
                player.inv.contains(objs.imcando_hammer_offhand) ||
                player.worn.contains(objs.imcando_hammer_offhand)
        ) {
            seqs.human_hammer_hit_imcando
        } else {
            seqs.human_hammer_hit
        }

    private fun nailPlan(player: Player): List<Pair<ObjType, Int>>? {
        var remaining = NAILS_PER_SIDE
        val plan = mutableListOf<Pair<ObjType, Int>>()
        for (nail in NAILS) {
            val count = player.inv.count(objTypes[nail]).coerceAtMost(remaining)
            if (count > 0) {
                plan += nail to count
                remaining -= count
            }
            if (remaining == 0) {
                return plan
            }
        }
        return null
    }

    private enum class Side {
        Island,
        Mainland,
    }

    public companion object {
        public val MAINLAND_SIDE: CoordGrid = CoordGrid(3504, 3556)
        public val ISLAND_SIDE: CoordGrid = CoordGrid(3504, 3560)
        public val MAINLAND_ARRIVAL: CoordGrid = CoordGrid(3504, 3555)
        public val ISLAND_ARRIVAL: CoordGrid = CoordGrid(3504, 3561)

        public const val CONSTRUCTION_LEVEL: Int = 59
        public const val AGILITY_LEVEL: Int = 69
        public const val PLANKS_PER_SIDE: Int = 6
        public const val NAILS_PER_SIDE: Int = 28
        public const val CONSTRUCTION_XP_PER_SIDE: Double = 550.0
        public const val BUILD_TICKS: Int = 4
        public const val JUMP_TICKS: Int = 3

        public const val MISSING_MATERIALS_MESSAGE: String =
            "You need 6x Teak plank, 28 nails to build that."
        public const val COLLAPSE_MESSAGE: String =
            "Despite your best attempt, the bridge span collapses into the water."
        public const val FIRST_SIDE_MESSAGE: String = "You reinforce this side of the bridge."
        public const val FINAL_SIDE_MESSAGE: String =
            "You reinforce this side of the bridge. It is now sturdy enough that you could jump " +
                "over the gap!"

        private const val UNREPAIRED = 0
        private const val ISLAND_REINFORCED = 1
        private const val REPAIRED = 2
        private const val BRIDGE_CENTRE_Z = 3558
        private const val JUMP_CLIENT_CYCLES = 90
        private const val CINEMATIC_TICKS = 6

        private val CINEMATIC_CAMERA = CoordGrid(3498, 3555)
        private val MAUSOLEUM = CoordGrid(3504, 3572)
        private val HAMMERS =
            setOf(objs.hammer.id, objs.imcando_hammer.id, objs.imcando_hammer_offhand.id)
        private val NAILS =
            listOf(
                objs.bronze_nails,
                objs.iron_nails,
                objs.steel_nails,
                objs.black_nails,
                objs.mithril_nails,
                objs.adamant_nails,
                objs.rune_nails,
            )
    }
}
