package org.rsmod.content.travel.mausoleumbridge

import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.output.privateLocChange
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.loc.LocLayerConstants

/** Poll 85 rope state at the 1 October 2025 cache-233.4 cutoff. */
public class BaxtorianFallsRopes {
    /** Installs the first cache morph; the 17 September hotfix disabled the second coil. */
    public fun sync(player: Player) {
        player.privateLocChange(ropeLoc(ROCK_ROPE, BaxtorianFallsRopeLocs.rock))
    }

    public fun take(access: ProtectedAccess, rope: BoundLocInfo) {
        with(access) {
            if (!eligible(player)) {
                sync(player)
                return
            }
            if (rope.id == BaxtorianFallsRopeLocs.rock.id) {
                teleport(CLUE_PLATEAU)
            }
        }
    }

    public fun eligible(player: Player): Boolean =
        player.members &&
            player.vars[varps.waterfall_quest] >= WATERFALL_COMPLETE &&
            player.vars[varbits.kandarin_medium_reward] != 0

    private fun ropeLoc(coords: CoordGrid, type: LocType): LocInfo =
        LocInfo(
            LocLayerConstants.of(LocShape.CentrepieceStraight.id),
            coords,
            LocEntity(type.id, LocShape.CentrepieceStraight.id, LocAngle.West.id),
        )

    public companion object {
        /** Wiki map position for the first Poll 85 coil, above the crossing rock. */
        public val ROCK_ROPE: CoordGrid = CoordGrid(2511, 3477)

        /** Wiki map position of the second coil, retained in cache but disabled at this cutoff. */
        public val TREE_ROPE: CoordGrid = CoordGrid(2513, 3465)

        /** The 17 September hotfix preserved access to the medium-clue dig tile. */
        public val CLUE_PLATEAU: CoordGrid = CoordGrid(2512, 3467)

        /** Quest Helper has active states 0..8; the journal-complete state is 10. */
        public const val WATERFALL_COMPLETE: Int = 10
    }
}
