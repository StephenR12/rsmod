package org.rsmod.content.skills.farming

import org.rsmod.api.config.refs.stats
import org.rsmod.content.skills.pets.SkillPetRates
import org.rsmod.game.entity.Player

internal fun SkillPetRates.rollTangleroot(player: Player, crop: FarmingCrop) {
    val rate =
        when (crop.patchType) {
            FarmingPatchType.FruitTree -> 9000
            FarmingPatchType.Herb -> 98364
            else ->
                when (crop.key) {
                    "potato",
                    "onion",
                    "cabbage",
                    "tomato",
                    "marigold",
                    "rosemary",
                    "nasturtium",
                    "woad",
                    "white_lily" -> 281040
                    "sweetcorn",
                    "limpwurt" -> 224832
                    "strawberry" -> 187360
                    "snape_grass" -> 173977
                    "watermelon",
                    "potato_cactus" -> 160594
                    "barley",
                    "hammerstone" -> 112416
                    "asgarnian",
                    "jute" -> 89933
                    "yanillian" -> 74944
                    "krandorian" -> 64238
                    "wildblood" -> 56208
                    "redberry" -> 44966
                    "cadavaberry" -> 37472
                    "dwellberry" -> 32119
                    "jangerberry",
                    "whiteberry",
                    "poisonivy" -> 28104
                    "oak" -> 22483
                    "willow" -> 16059
                    "maple" -> 14052
                    "yew" -> 11242
                    "magic" -> 9368
                    "celastrus",
                    "crystal" -> 9000
                    "belladonna" -> 8000
                    "seaweed",
                    "bittercap" -> 7500
                    "cactus" -> 7000
                    "calquat" -> 6000
                    "spirit",
                    "teak",
                    "mahogany",
                    "redwood" -> 5000
                    "grape" -> 385426
                    else -> return
                }
        }
    roll(player, stats.farming, 20661, rate)
}
