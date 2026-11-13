package org.rsmod.content.skills.smithing.smelting.workshop

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

public data class WorkshopSession(val region: Region, val cart: Npc)

/**
 * Personal machinery state uses the native bits; isolated copies prevent another account moving a
 * cart.
 */
@Singleton
public class WorkshopAccess
@Inject
constructor(
    private val regions: RegionRepository,
    private val registry: RegionRegistry,
    private val players: PlayerRegistry,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val collision: CollisionFlagMap,
) {
    private val sessions = WeakHashMap<Player, WorkshopSession>()
    private val template =
        RegionTemplate.create {
            for (level in listOf(0, 2, 3)) for (x in 0..7) for (z in 0..7) this[x, z, level] =
                ZoneKey(240 + x, 640 + z, level)
        }

    public fun session(player: Player): WorkshopSession? =
        sessions[player]?.takeIf { it.region.slot != Region.INVALID_SLOT }

    public fun coord(player: Player, normal: CoordGrid): CoordGrid? =
        session(player)?.region?.normal?.get(normal)

    public fun source(player: Player, c: CoordGrid): CoordGrid? =
        session(player)?.let { s ->
            if (
                c.x in s.region.southWest.x until s.region.northEast.x &&
                    c.z in s.region.southWest.z until s.region.northEast.z
            )
                CoordGrid(
                    c.x - s.region.southWest.x + 1920,
                    c.z - s.region.southWest.z + 5120,
                    c.level,
                )
            else null
        }

    public fun enter(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || vars[WorkshopBits.quest] < 5) {
                mes("You haven't unlocked the deeper workshop yet.")
                return false
            }
            var s = session(player)
            if (s == null) {
                val region =
                    regions.add(template)
                        ?: run {
                            mes("The workshop is currently full.")
                            return false
                        }
                val cart =
                    Npc(
                        types[WorkshopCarts.stages[0]],
                        region.normal[CART_TILES[vars[WorkshopBits.position].coerceIn(0, 3)]],
                    )
                npcs.add(cart, Int.MAX_VALUE)
                s = WorkshopSession(region, cart)
                sessions[player] = s
            }
            s.region.reclaimFromCycle = mapClock + 3
            teleport(s.region.normal[CoordGrid(1952, 5154, 2)])
            sync(player)
            true
        }

    public fun sync(player: Player) {
        val s = session(player) ?: return
        val stage = player.vars[WorkshopBits.bar].coerceIn(0, 5)
        val position = player.vars[WorkshopBits.position].coerceIn(0, 3)
        s.cart.transmog(types[WorkshopCarts.stages[stage]], Int.MAX_VALUE)
        s.cart.telejump(collision, s.region.normal[CART_TILES[position]])
    }

    public fun leave(access: ProtectedAccess): Unit =
        with(access) {
            cleanup(player)
            teleport(RETURN)
        }

    public fun logout(player: Player) {
        if (source(player, player.coords) != null) players.relocateOnLogout(player, RETURN)
        cleanup(player)
    }

    public fun cleanup(player: Player) {
        val s = sessions.remove(player) ?: return
        if (s.region.slot != Region.INVALID_SLOT && s.cart.isVisible)
            npcs.del(s.cart, Int.MAX_VALUE)
    }

    public fun recover(player: Player) {
        val z = registry[ZoneKey.from(player.coords)]
        if (z.normalX in 240..247 && z.normalZ in 640..647)
            org.rsmod.game.entity.util.PathingEntityCommon.telejump(player, collision, RETURN)
    }

    public companion object {
        public val RETURN: CoordGrid = CoordGrid(2718, 9888)
        public val CART_TILES: List<CoordGrid> =
            listOf(
                CoordGrid(1954, 5147, 2),
                CoordGrid(1946, 5154, 2),
                CoordGrid(1954, 5162, 2),
                CoordGrid(1961, 5154, 2),
            )
    }
}
