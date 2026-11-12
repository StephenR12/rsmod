package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.obj.ObjType

public data class LampOilContainer(public val empty: ObjType, public val filled: ObjType)

public object LampOilContainers {
    public val all: List<LampOilContainer> =
        listOf(
            LampOilContainer(objs.empty_oil_lamp, objs.unlit_oil_lamp),
            LampOilContainer(objs.empty_oil_lantern, objs.unlit_oil_lantern),
            LampOilContainer(objs.empty_bullseye_lantern, objs.unlit_bullseye_lantern),
            LampOilContainer(objs.empty_sapphire_lantern, objs.unlit_sapphire_lantern),
            LampOilContainer(objs.empty_emerald_lantern, objs.unlit_emerald_lantern),
        )
}

public class LampOilStill @Inject constructor(private val locRepo: LocRepository) {
    private val loadedDorgeshStills = mutableSetOf<Int>()

    public fun addTar(access: ProtectedAccess, still: BoundLocInfo, tarSlot: Int): Boolean {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to make lamp oil.")
                return false
            }
            if (player.inv[tarSlot]?.id != objs.swamp_tar.id || !isCurrent(still)) {
                return false
            }
            val isRimmington = still.id == firemaking_locs.lampOilStillEmpty.id
            val isDorgesh = still.id == firemaking_locs.dorgeshLampStill.id
            if (!isRimmington && !isDorgesh) {
                return false
            }
            if (isDorgesh && still.coords.packed in loadedDorgeshStills) {
                mes("The still already contains lamp oil.")
                return false
            }
            val deletion = invDel(player.inv, objs.swamp_tar, slot = tarSlot, autoCommit = false)
            if (deletion.failure) {
                return false
            }
            if (isRimmington && !replace(still, firemaking_locs.lampOilStillFull.id)) {
                return false
            }
            deletion.commitAll()
            if (isDorgesh) {
                loadedDorgeshStills += still.coords.packed
            }
            return true
        }
    }

    public fun fill(
        access: ProtectedAccess,
        still: BoundLocInfo,
        containerSlot: Int,
        recipe: LampOilContainer,
    ): Boolean {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to make lamp oil.")
                return false
            }
            if (player.inv[containerSlot]?.id != recipe.empty.id || !isCurrent(still)) {
                return false
            }
            val isRimmington = still.id == firemaking_locs.lampOilStillFull.id
            val isDorgesh =
                still.id == firemaking_locs.dorgeshLampStill.id &&
                    still.coords.packed in loadedDorgeshStills
            if (!isRimmington && !isDorgesh) {
                mes("You need to put swamp tar in the still first.")
                return false
            }
            val transformation =
                player.invTransaction(player.inv, autoCommit = false) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.empty.id, 1, slot = containerSlot)
                    add(inventory, recipe.filled.id, 1, slot = containerSlot)
                }
            if (transformation.failure) {
                return false
            }
            if (isRimmington && !replace(still, firemaking_locs.lampOilStillEmpty.id)) {
                return false
            }
            if (isDorgesh) {
                loadedDorgeshStills -= still.coords.packed
            }
            transformation.commitAll()
            return true
        }
    }

    private fun isCurrent(still: BoundLocInfo): Boolean =
        locRepo.findAll(still.coords).any { it.entity == still.entity }

    private fun replace(still: BoundLocInfo, into: Int): Boolean {
        val replacement =
            LocInfo(still.layer, still.coords, LocEntity(into, still.shapeId, still.angleId))
        return locRepo.add(replacement, Int.MAX_VALUE)
    }
}
