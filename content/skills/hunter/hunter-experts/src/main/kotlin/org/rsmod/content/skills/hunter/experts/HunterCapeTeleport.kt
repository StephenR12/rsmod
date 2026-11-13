package org.rsmod.content.skills.hunter.experts

import org.rsmod.api.player.isTeleportBlocked
import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/** Cache-233.4 Hunter-cape destination, quota, and standard teleport behavior. */
public class HunterCapeTeleport @Inject constructor(private val state: HunterCapeState) {
    public suspend fun select(
        access: ProtectedAccess,
        currentUtcDay: Int = HunterCapeState.currentUtcDay(),
    ) {
        with(access) {
            val destinations = buildList {
                if (state.hasGuildTeleport(this@with)) {
                    add(HunterCapeDestination.HunterGuild)
                }
                add(HunterCapeDestination.FeldipHunterArea)
                add(HunterCapeDestination.WildernessHunterArea)
            }
            val selection =
                menu(
                    title = MENU_TITLE,
                    hotkeys = false,
                    choices = destinations.map(HunterCapeDestination::label) + "Cancel",
                )
            val destination = destinations.getOrNull(selection) ?: return
            teleport(this, destination, currentUtcDay)
        }
    }

    public suspend fun teleport(
        access: ProtectedAccess,
        destination: HunterCapeDestination,
        currentUtcDay: Int = HunterCapeState.currentUtcDay(),
    ) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to use this cape.")
                return
            }
            if (vars[varbits.inside_wilderness] == 1 && wildernessLevel() > MAX_WILDERNESS_LEVEL) {
                mes("A mysterious force prevents you from teleporting!")
                return
            }
            if (player.isTeleportBlocked()) {
                mes("A magical force stops you from teleporting.")
                return
            }
            if (
                !destination.unlimited &&
                    state.usedToday(this, currentUtcDay) >= HunterCapeState.DAILY_TELEPORT_LIMIT
            ) {
                mes("You have used all of your available teleports today. Try again tomorrow.")
                return
            }
            if (destination == HunterCapeDestination.HunterGuild && !state.hasGuildTeleport(this)) {
                return
            }

            stopAction()
            anim(seqs.teleport_casting)
            spotanim(spotanims.teleport_casting)
            soundSynth(synths.teleport_all)
            delay(CAST_TICKS)

            if (!destination.unlimited) {
                state.consumeTeleport(this, currentUtcDay)
            }
            teleport(destination.coords)
            publish(PlayerTeleportEvent.Start(player, destination.coords))
        }
    }

    public fun availableDestinations(access: ProtectedAccess): List<HunterCapeDestination> =
        availableDestinations(access.player)

    public fun availableDestinations(player: Player): List<HunterCapeDestination> = buildList {
        if (state.hasGuildTeleport(player)) add(HunterCapeDestination.HunterGuild)
        add(HunterCapeDestination.FeldipHunterArea)
        add(HunterCapeDestination.WildernessHunterArea)
    }

    public companion object {
        public const val MENU_TITLE: String = "Hunter Areas"
        public const val CAST_TICKS: Int = 4

        private const val MAX_WILDERNESS_LEVEL = 20

    }
}

internal fun ProtectedAccess.wildernessLevel():Int {
    val baseZ=if(coords.z>=9920)9920 else 3520
    return (coords.z-baseZ)/8+1
}

public enum class HunterCapeDestination(
    public val label: String,
    public val coords: CoordGrid,
    public val unlimited: Boolean,
) {
    HunterGuild("Hunter Guild", CoordGrid(1558, 3046), true),
    FeldipHunterArea("Carnivorous Chinchompas", CoordGrid(2556, 2917), false),
    WildernessHunterArea("Black Chinchompas", CoordGrid(3144, 3772), false),
}
