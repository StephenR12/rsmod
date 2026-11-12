package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.utils.time.epochMinute
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/** Cache-233 Home Teleports, including their shared persistent cooldown channel. */
public class HomeTeleport
@Inject
constructor(
    private val channels: HomeTeleportChannelRepository,
    private val worldRepo: WorldRepository,
) {
    public fun cast(
        access: ProtectedAccess,
        destination: HomeTeleportDestination = HomeTeleportDestination.Lumbridge,
    ) {
        with(access) {
            if (destination.membersOnly && !player.members) {
                mes("You need to be on a members' world to cast this spell.")
                return
            }
            val now = epochMinute()
            val lastCast = vars[varps.aide_tele_timer]
            val elapsedMinutes = now - lastCast
            if (lastCast > 0 && elapsedMinutes < COOLDOWN_MINUTES) {
                val remaining = (COOLDOWN_MINUTES - elapsedMinutes).coerceIn(1, COOLDOWN_MINUTES)
                val unit = if (remaining == 1) "minute" else "minutes"
                mes("You need to wait another $remaining $unit to cast this spell.")
                return
            }
            if (vars[varbits.inside_wilderness] == 1 && wildernessLevel() > MAX_WILDERNESS_LEVEL) {
                mes("A mysterious force prevents you from teleporting!")
                return
            }
            if (vars[varbits.teleblock_cycles] > TELEBLOCK_IMMUNITY_CYCLES) {
                mes("A magical force stops you from teleporting.")
                return
            }
            if (player.preventLogoutUntil >= mapClock) {
                mes("You can't use that teleport at the moment.")
                return
            }

            stopAction()
            clearQueue(home_teleport_queues.channel)
            val token = channels.begin(player)
            playStage(CHALK_STAGE)
            queue(
                home_teleport_queues.channel,
                1,
                HomeTeleportChannel(token, coords, destination, elapsedTicks = 1),
            )
        }
    }

    internal fun advance(access: ProtectedAccess, channel: HomeTeleportChannel) {
        with(access) {
            if (!channels.isActive(player, channel.token)) {
                return
            }
            if (coords != channel.origin || player.preventLogoutUntil >= mapClock) {
                abort(player, channel.token)
                return
            }

            playStage(channel.elapsedTicks)
            if (channel.elapsedTicks == CAST_TICKS) {
                check(channels.finish(player, channel.token))
                resetAnim()
                resetSpotanim()
                spellTeleport(channel.destination.coords)
                vars[varps.aide_tele_timer] = epochMinute()
                return
            }

            queue(
                home_teleport_queues.channel,
                1,
                channel.copy(elapsedTicks = channel.elapsedTicks + 1),
            )
        }
    }

    public fun interrupt(player: Player) {
        if (!channels.cancel(player)) {
            return
        }
        player.clearQueue(home_teleport_queues.channel)
        player.resetAnim()
        player.resetSpotanim()
    }

    private fun ProtectedAccess.abort(player: Player, token: Int) {
        if (!channels.finish(player, token)) {
            return
        }
        resetAnim()
        resetSpotanim()
    }

    private fun ProtectedAccess.playStage(elapsedTicks: Int) {
        when (elapsedTicks) {
            CHALK_STAGE -> {
                anim(seqs.aide_drawing_chalk_circle)
                spotanim(spotanims.aide_chalk_circle)
                soundArea(worldRepo, coords, synths.aide_teleport_chalk)
            }
            SIT_STAGE -> {
                anim(seqs.aide_sitting_down_crosslegged)
                soundArea(worldRepo, coords, synths.aide_teleport_sitdown)
            }
            BOOK_STAGE -> {
                anim(seqs.aide_player_getting_book)
                spotanim(spotanims.aide_player_book_get)
                soundArea(worldRepo, coords, synths.aide_teleport_book)
            }
            INCANTATION_STAGE -> {
                anim(seqs.aide_reciting_incantation)
                spotanim(spotanims.aide_player_book_portal, height = PORTAL_HEIGHT)
                soundArea(worldRepo, coords, synths.aide_teleport_portal)
            }
            DEPART_STAGE -> {
                anim(seqs.aide_player_teleporting)
                spotanim(spotanims.aide_player_teleport)
            }
        }
    }

    private fun ProtectedAccess.wildernessLevel(): Int {
        val baseZ =
            if (coords.z >= WILDERNESS_UNDERGROUND_Z) WILDERNESS_UNDERGROUND_Z else WILDERNESS_Z
        return max(1, ((coords.z - baseZ) / WILDERNESS_LEVEL_HEIGHT) + 1)
    }

    private companion object {
        const val COOLDOWN_MINUTES = 30
        const val CAST_TICKS = 22
        const val CHALK_STAGE = 0
        const val SIT_STAGE = 4
        const val BOOK_STAGE = 7
        const val INCANTATION_STAGE = 10
        const val DEPART_STAGE = 13
        // The update protocol carries the captured -10 offset as an unsigned 16-bit value.
        const val PORTAL_HEIGHT = 65526
        const val MAX_WILDERNESS_LEVEL = 20
        const val TELEBLOCK_IMMUNITY_CYCLES = 100
        const val WILDERNESS_LEVEL_HEIGHT = 8
        const val WILDERNESS_Z = 3520
        const val WILDERNESS_UNDERGROUND_Z = 9920
    }
}

public enum class HomeTeleportDestination(
    public val coords: CoordGrid,
    public val membersOnly: Boolean,
) {
    Lumbridge(CoordGrid(0, 50, 50, 21, 18), membersOnly = false),
    Edgeville(CoordGrid(0, 48, 54, 15, 48), membersOnly = false),
    LunarIsle(CoordGrid(0, 33, 61, 1, 11), membersOnly = true),
    Arceuus(CoordGrid(0, 26, 60, 36, 42), membersOnly = true),
}
