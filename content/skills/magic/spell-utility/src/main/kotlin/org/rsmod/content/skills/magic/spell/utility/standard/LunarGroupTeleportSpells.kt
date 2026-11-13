package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.player.isTeleportBlocked
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

/** Cache-233 Lunar group-teleport casting, consent and destination behavior. */
public class LunarGroupTeleportSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val players: PlayerList,
    private val protectedAccess: ProtectedAccessLauncher,
    private val offers: LunarGroupTeleportOfferRepository,
) {
    public suspend fun cast(access: ProtectedAccess, definition: LunarGroupTeleportDefinition) {
        with(access) {
            val spell = requireNotNull(spells.getObjSpell(definition.spell))
            check(spell.type == MagicSpellType.Teleport) { "Expected teleport spell: $spell" }

            if (!canAccess() || !runes.canCastSpell(player, spell)) {
                return
            }
            stopAction()
            if (definition == LunarGroupTeleportDefinition.IcePlateau && !confirmDeepWilderness()) {
                return
            }
            if (!canAccess() || runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            offerNearbyPlayers(player, definition)
            anim(seqs.lunar_teleport)
            spotanim(spotanims.lunar_teleport_spotanim)
            soundSynth(synths.teleport_all)
            delay(CAST_TICKS)
            spellTeleport(definition.destination(random))
        }
    }

    public suspend fun accept(access: ProtectedAccess) {
        val offer = offers.take(access.player) ?: return
        with(access) {
            ifCloseSub(interfaces.teleport_other)
            val failure = teleportFailure()
            if (failure != null) {
                mes(failure)
                return
            }
            anim(seqs.lunar_teleport)
            spotanim(spotanims.lunar_teleport_spotanim)
            soundSynth(synths.teleport_all)
            delay(CAST_TICKS)
            spellTeleport(offer.definition.destination(random))
        }
    }

    public fun decline(access: ProtectedAccess) {
        offers.remove(access.player)
        access.ifCloseSub(interfaces.teleport_other)
    }

    public fun clear(player: Player) {
        offers.remove(player)
    }

    private fun offerNearbyPlayers(caster: Player, definition: LunarGroupTeleportDefinition) {
        val recipients =
            players
                .asSequence()
                .filter { it !== caster }
                .filter { it.coords.level == caster.coords.level }
                .filter { it.coords.chebyshevDistance(caster.coords) <= AREA_RADIUS }
                .filter { it.vars[varbits.option_acceptaid] != 0 }
                .toList()

        for (recipient in recipients) {
            val offer = LunarGroupTeleportOffer(definition)
            offers.put(recipient, offer)
            val opened =
                protectedAccess.launch(recipient) {
                    ifOpenMainModal(interfaces.teleport_other)
                    ifSetText(components.teleport_other_caster_name, caster.displayName)
                    ifSetText(components.teleport_other_location, definition.location)
                    ifSetEvents(components.teleport_other_accept, -1..-1, IfEvent.PauseButton)
                    ifSetEvents(components.teleport_other_decline, -1..-1, IfEvent.PauseButton)
                    val response = pauseButton()
                    when {
                        response.component.isType(components.teleport_other_accept) -> accept(this)
                        response.component.isType(components.teleport_other_decline) ->
                            decline(this)
                        else -> clear(player)
                    }
                }
            if (!opened) {
                offers.remove(recipient)
            }
        }
    }

    private suspend fun ProtectedAccess.confirmDeepWilderness(): Boolean =
        menu(
            title = "Deep Wilderness",
            hotkeys = false,
            choices = listOf("Teleport to the Ice Plateau.", "Cancel."),
        ) == 0

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            mes("You need to complete Lunar Diplomacy to cast this spell.")
            return false
        }
        val failure = teleportFailure()
        if (failure != null) {
            mes(failure)
            return false
        }
        return true
    }

    private fun ProtectedAccess.teleportFailure(): String? {
        if (vars[varbits.inside_wilderness] == 1 && wildernessLevel() > MAX_WILDERNESS_LEVEL) {
            return "A mysterious force prevents you from teleporting!"
        }
        if (player.isTeleportBlocked()) {
            return "A magical force stops you from teleporting."
        }
        return null
    }

    private fun ProtectedAccess.wildernessLevel(): Int {
        val baseZ =
            if (coords.z >= WILDERNESS_UNDERGROUND_Z) WILDERNESS_UNDERGROUND_Z else WILDERNESS_Z
        return ((coords.z - baseZ) / WILDERNESS_LEVEL_HEIGHT) + 1
    }

    private companion object {
        const val AREA_RADIUS = 1
        const val CAST_TICKS = 4
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val MAX_WILDERNESS_LEVEL = 20

        const val WILDERNESS_LEVEL_HEIGHT = 8
        const val WILDERNESS_Z = 3520
        const val WILDERNESS_UNDERGROUND_Z = 9920
    }
}

@Singleton
public class LunarGroupTeleportOfferRepository @Inject constructor() {
    private val offers = WeakHashMap<Player, LunarGroupTeleportOffer>()

    public fun put(target: Player, offer: LunarGroupTeleportOffer) {
        offers[target] = offer
    }

    public fun get(target: Player): LunarGroupTeleportOffer? = offers[target]

    public fun take(target: Player): LunarGroupTeleportOffer? = offers.remove(target)

    public fun remove(target: Player) {
        offers.remove(target)
    }
}

public data class LunarGroupTeleportOffer(public val definition: LunarGroupTeleportDefinition)

public enum class LunarGroupTeleportDefinition(
    public val spell: ObjType,
    public val component: ComponentType,
    public val location: String,
    private val destinations: List<CoordGrid>,
) {
    Moonclan(
        objs.spell_moonclan_telegroup,
        components.magic_spellbook_tele_group_moonclan,
        "Moonclan Island",
        buildList {
            for (x in 2107 until 2112) {
                add(CoordGrid(x, 3915))
            }
        },
    ),
    Waterbirth(
        objs.spell_waterbirth_telegroup,
        components.magic_spellbook_tele_group_waterbirth,
        "Waterbirth Island",
        destinationSquare(CoordGrid(2546, 3755), radius = 2),
    ),
    Barbarian(
        objs.spell_barboutpost_telegroup,
        components.magic_spellbook_tele_group_barbarian,
        "Barbarian Outpost",
        destinationSquare(CoordGrid(2543, 3569), radius = 2),
    ),
    Khazard(
        objs.spell_portkhazard_telegroup,
        components.magic_spellbook_tele_group_khazard,
        "Port Khazard",
        destinationSquare(CoordGrid(2636, 3167), radius = 2),
    ),
    FishingGuild(
        objs.spell_fishguard_telegroup,
        components.magic_spellbook_tele_group_fishing_guild,
        "Fishing Guild",
        destinationSquare(CoordGrid(2613, 3391), radius = 2),
    ),
    Catherby(
        objs.spell_catherby_telegroup,
        components.magic_spellbook_tele_group_catherby,
        "Catherby",
        buildList {
            for (x in 2799 until 2804) {
                for (z in 3448 until 3452) {
                    add(CoordGrid(x, z))
                }
            }
            for (x in 2801 until 2804) {
                add(CoordGrid(x, 3447))
            }
        },
    ),
    IcePlateau(
        objs.spell_lunarghorrock_telegroup,
        components.magic_spellbook_tele_group_ghorrock,
        "Ice Plateau",
        destinationSquare(CoordGrid(2975, 3938), radius = 3),
    );

    public fun destination(random: GameRandom): CoordGrid = random.pick(destinations)

    public fun accepts(coords: CoordGrid): Boolean = coords in destinations

    public val destinationCount: Int
        get() = destinations.size
}

private fun destinationSquare(center: CoordGrid, radius: Int): List<CoordGrid> = buildList {
    for (x in center.x - radius..center.x + radius) {
        for (z in center.z - radius..center.z + radius) {
            add(CoordGrid(x, z, center.level))
        }
    }
}
