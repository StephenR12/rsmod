package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
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
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

/** Cache-233 Standard Teleother offer, confirmation and accepted-teleport behavior. */
public class TeleotherSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val protectedAccess: ProtectedAccessLauncher,
    private val offers: TeleotherOfferRepository,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess, target: Player, spellType: ObjType) {
        val definition =
            TeleotherDefinition.forSpell(spellType)
                ?: error("Unsupported Teleother spell: $spellType")
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to cast this spell.")
                return
            }
            if (player === target || !cooldowns.isReady(player, mapClock)) {
                return
            }
            if (target.vars[varbits.option_acceptaid] == 0) {
                mes("${target.displayName} is not accepting aid.")
                return
            }
            if (target.isAccessProtected) {
                mes("The other player is busy.")
                return
            }

            val spell = requireNotNull(spells.getObjSpell(spellType))
            if (!runes.canCastSpell(player, spell)) {
                return
            }

            val offer = TeleotherOffer(definition)
            val casterName = player.displayName
            offers.put(target, offer)
            val opened =
                protectedAccess.launch(target) {
                    ifOpenMainModal(interfaces.teleport_other)
                    ifSetText(components.teleport_other_caster_name, casterName)
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
                offers.remove(target)
                mes("The other player is busy.")
                return
            }

            if (runes.attemptCast(player, spell).isFailure()) {
                offers.remove(target)
                protectedAccess.launch(target) { ifCloseSub(interfaces.teleport_other) }
                return
            }
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            stopAction()
            anim(seqs.teleport_other_casting)
            spotanim(spotanims.teleport_other_casting)
            soundSynth(synths.tele_other_cast)
            cooldowns.markCast(player, mapClock + CAST_RATE)
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
            anim(seqs.teleport_other_impact)
            spotanim(spotanims.teleport_other_impact)
            delay(seqTicks(seqs.teleport_other_impact))
            stopAction()
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

    private fun ProtectedAccess.teleportFailure(): String? {
        if (vars[varbits.inside_wilderness] == 1 && wildernessLevel() > MAX_WILDERNESS_LEVEL) {
            return "A mysterious force prevents you from teleporting!"
        }
        if (vars[varbits.teleblock_cycles] > TELEBLOCK_IMMUNITY_CYCLES) {
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
        const val CAST_RATE = 10
        const val MAX_WILDERNESS_LEVEL = 20
        const val TELEBLOCK_IMMUNITY_CYCLES = 100
        const val WILDERNESS_LEVEL_HEIGHT = 8
        const val WILDERNESS_Z = 3520
        const val WILDERNESS_UNDERGROUND_Z = 9920
    }
}

@Singleton
public class TeleotherOfferRepository @Inject constructor() {
    private val offers = WeakHashMap<Player, TeleotherOffer>()

    public fun put(target: Player, offer: TeleotherOffer) {
        offers[target] = offer
    }

    public fun get(target: Player): TeleotherOffer? = offers[target]

    public fun take(target: Player): TeleotherOffer? = offers.remove(target)

    public fun remove(target: Player) {
        offers.remove(target)
    }
}

public data class TeleotherOffer(public val definition: TeleotherDefinition)

public enum class TeleotherDefinition(
    public val spell: ObjType,
    public val component: ComponentType,
    public val location: String,
    private val destinations: List<CoordGrid>,
) {
    Lumbridge(
        objs.spell_teleother_lumbridge,
        components.magic_spellbook_teleother_lumbridge,
        "Lumbridge",
        buildList {
            for (x in 3219 until 3224) {
                for (z in 3218 until 3220) {
                    add(CoordGrid(x, z))
                }
            }
            for (x in 3221 until 3223) {
                for (z in 3216 until 3218) {
                    add(CoordGrid(x, z))
                }
                add(CoordGrid(x, 3220))
            }
        },
    ),
    Falador(
        objs.spell_teleother_falador,
        components.magic_spellbook_teleother_falador,
        "Falador",
        teleotherSquare(CoordGrid(2964, 3378), radius = 2),
    ),
    Camelot(
        objs.spell_teleother_camelot,
        components.magic_spellbook_teleother_camelot,
        "Camelot",
        teleotherSquare(CoordGrid(2757, 3479), radius = 2),
    );

    public fun destination(random: org.rsmod.api.random.GameRandom): CoordGrid =
        random.pick(destinations)

    public fun accepts(coords: CoordGrid): Boolean = coords in destinations

    public companion object {
        public fun forSpell(spell: ObjType): TeleotherDefinition? =
            entries.firstOrNull { it.spell.id == spell.id }
    }
}

private fun teleotherSquare(center: CoordGrid, radius: Int): List<CoordGrid> = buildList {
    for (x in center.x - radius..center.x + radius) {
        for (z in center.z - radius..center.z + radius) {
            add(CoordGrid(x, z, center.level))
        }
    }
}
