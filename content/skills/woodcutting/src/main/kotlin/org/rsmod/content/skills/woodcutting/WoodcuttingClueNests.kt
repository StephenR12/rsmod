package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.Constants
import org.rsmod.api.config.locParam
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingObjs
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingParams
import org.rsmod.game.entity.Player
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varbit.VarBitType

public class WoodcuttingClueNests
@Inject
constructor(
    private val random: GameRandom,
    private val objRepo: ObjRepository,
    private val clueInventory: ClueInventory,
) {
    /** Rolls every eligible clue tier independently after a successful Woodcutting action. */
    public fun tryDrops(access: ProtectedAccess, tree: UnpackedLocType): List<WoodcuttingClueTier> {
        with(access) {
            if (vars[varbits.vesseled_clues_disabled] != 0) {
                return emptyList()
            }

            val drops = mutableListOf<WoodcuttingClueTier>()
            for (tier in WoodcuttingClueTier.entries) {
                if (tier.clue.members && !player.members) {
                    continue
                }
                val inventoryState = clueInventory.state(player, inv, bank, tier.clue)
                if (!inventoryState.xMarksComplete && !inventoryState.canReceive) continue
                val denominator = denominator(player, tree, tier)
                if (random.of(denominator) != 0) {
                    continue
                }
                if (!inventoryState.canReceive) {
                    mes(
                        "You have a sneaking suspicion that you would have received a " +
                            "${tier.clue.key} scroll box."
                    )
                    continue
                }
                val coords =
                    WoodcuttingBirdNests.adjacentDropCoords(
                        player.coords,
                        random.of(WoodcuttingBirdNests.ADJACENT_TILE_COUNT),
                    )
                if (inventoryState.xMarksComplete) {
                    objRepo.add(
                        type = tier.clue.scrollBox,
                        coords = coords,
                        duration = ClueInventory.SCROLL_BOX_GROUND_DURATION,
                        receiver = player,
                    )
                    objRepo.add(
                        type = objs.empty_bird_nest,
                        coords = coords,
                        duration = Constants.lootdrop_duration,
                        receiver = player,
                    )
                } else {
                    objRepo.add(
                        type = tier.nest,
                        coords = coords,
                        duration = Constants.lootdrop_duration,
                        receiver = player,
                    )
                }
                mes("<col=ff0000>A bird's nest falls out of the tree.</col>")
                drops += tier
            }
            return drops
        }
    }

    public fun denominator(player: Player, tree: UnpackedLocType, tier: WoodcuttingClueTier): Int {
        val combatTierComplete = tier.caStatus?.let { player.vars[it] == COMPLETE_STATUS } ?: false
        val wildernessRing =
            tier != WoodcuttingClueTier.Beginner && player.vars[varbits.inside_wilderness] != 0 &&
                RING_OF_WEALTH_I_TYPES.any { player.worn.contains(it) }
        val chance=denominator(
            baseChance = tree.clueNestBaseChance,
            visibleLevel = player.woodcuttingLvl,
            tierModifierTenths = tier.modifierTenths,
            combatTierComplete = combatTierComplete,
            wildernessRing = wildernessRing,
        )
        return if(org.rsmod.content.skills.woodcutting.forestry.TwitchersGloves.preference(player)==4)(chance*5/6).coerceAtLeast(1) else chance
    }

    public companion object {
        public const val COMPLETE_STATUS: Int = 2

        public val UnpackedLocType.clueNestBaseChance: Int by
            locParam(WoodcuttingParams.clue_nest_base_chance)

        public fun denominator(
            baseChance: Int,
            visibleLevel: Int,
            tierModifierTenths: Int,
            combatTierComplete: Boolean,
            wildernessRing: Boolean,
        ): Int {
            return org.rsmod.api.clue.SkillingClue.denominator(
                baseChance, visibleLevel, tierModifierTenths, combatTierComplete, wildernessRing
            )
        }

        private val RING_OF_WEALTH_I_TYPES =
            listOf(
                WoodcuttingObjs.ringOfWealthI,
                WoodcuttingObjs.ringOfWealthI1,
                WoodcuttingObjs.ringOfWealthI2,
                WoodcuttingObjs.ringOfWealthI3,
                WoodcuttingObjs.ringOfWealthI4,
                WoodcuttingObjs.ringOfWealthI5,
            )
    }
}

public enum class WoodcuttingClueTier(
    public val clue: ClueTier,
    public val nest: ObjType,
    public val modifierTenths: Int,
    public val caStatus: VarBitType?,
) {
    Beginner(ClueTier.Beginner, WoodcuttingObjs.clueNestBeginner, 2, null),
    Easy(ClueTier.Easy, WoodcuttingObjs.clueNestEasy, 17, varbits.ca_tier_status_easy),
    Medium(ClueTier.Medium, WoodcuttingObjs.clueNestMedium, 20, varbits.ca_tier_status_medium),
    Hard(ClueTier.Hard, WoodcuttingObjs.clueNestHard, 33, varbits.ca_tier_status_hard),
    Elite(ClueTier.Elite, WoodcuttingObjs.clueNestElite, 100, varbits.ca_tier_status_elite),
}
