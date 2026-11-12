package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.map.CoordGrid

/** Cache-233 Teleport to House behavior and its boundary with the POH instance system. */
public class TeleportToHouseSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val houses: PlayerOwnedHouseRepository,
) {
    public suspend fun cast(access: ProtectedAccess, destination: HouseTeleportDestination) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to cast this spell.")
                return
            }
            val houseLocation = HouseLocation.fromCacheValue(vars[varbits.poh_house_location])
            if (houseLocation == null) {
                mes("You must own a house to cast this spell.")
                return
            }
            if (destination.isGroup) {
                mes("Group house teleporting is not available yet.")
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

            val target =
                when (destination) {
                    HouseTeleportDestination.Inside -> {
                        val buildingMode = vars[varbits.poh_teleport_building_mode] != 0
                        houses.destination(player, buildingMode)
                            ?: run {
                                mes("Your house is not ready to enter.")
                                return
                            }
                    }
                    HouseTeleportDestination.Outside -> houseLocation.outside
                    HouseTeleportDestination.GroupChoose,
                    HouseTeleportDestination.GroupPrevious -> error("Group route passed validation")
                }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_poh_teleport))
            check(spell.type == MagicSpellType.Teleport) { "Expected teleport spell: $spell" }
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            stopAction()
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.teleport_casting)
            spotanim(spotanims.teleport_casting)
            soundSynth(synths.teleport_all)
            delay(CAST_TICKS)
            spellTeleport(target)
        }
    }

    private fun ProtectedAccess.wildernessLevel(): Int {
        val baseZ =
            if (coords.z >= WILDERNESS_UNDERGROUND_Z) WILDERNESS_UNDERGROUND_Z else WILDERNESS_Z
        return ((coords.z - baseZ) / WILDERNESS_LEVEL_HEIGHT) + 1
    }

    private companion object {
        const val CAST_TICKS = 4
        const val MAX_WILDERNESS_LEVEL = 20
        const val TELEBLOCK_IMMUNITY_CYCLES = 100
        const val WILDERNESS_LEVEL_HEIGHT = 8
        const val WILDERNESS_Z = 3520
        const val WILDERNESS_UNDERGROUND_Z = 9920
    }
}

public enum class HouseTeleportDestination(public val isGroup: Boolean = false) {
    Inside,
    Outside,
    GroupChoose(true),
    GroupPrevious(true),
}

public enum class HouseLocation(public val cacheValue: Int, public val outside: CoordGrid) {
    Rimmington(1, CoordGrid(0, 46, 50, 10, 24)),
    Taverley(2, CoordGrid(0, 45, 54, 13, 9)),
    Pollnivneach(3, CoordGrid(0, 52, 46, 11, 59)),
    Rellekka(4, CoordGrid(0, 41, 56, 46, 48)),
    Brimhaven(5, CoordGrid(0, 43, 49, 6, 42)),
    Yanille(6, CoordGrid(0, 39, 48, 48, 23)),
    Hosidius(8, CoordGrid(0, 27, 54, 15, 60)),
    Prifddinas(9, CoordGrid(0, 50, 94, 39, 59)),
    Aldarin(10, CoordGrid(0, 22, 46, 14, 19));

    public companion object {
        public fun fromCacheValue(value: Int): HouseLocation? =
            entries.firstOrNull { it.cacheValue == value }
    }
}
