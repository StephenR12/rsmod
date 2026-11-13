package org.rsmod.content.skills.farming

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType

internal data class SeedReward(
    val seed: ObjType,
    val minimum: Int,
    val maximum: Int,
    val weight: Int,
)

internal object SeedPackRewards : ObjReferences() {
    val low =
        listOf(
            SeedReward(find("potato_seed"), 8, 12, 2),
            SeedReward(find("onion_seed"), 8, 12, 2),
            SeedReward(find("cabbage_seed"), 8, 12, 2),
            SeedReward(find("tomato_seed"), 8, 12, 2),
            SeedReward(find("sweetcorn_seed"), 8, 12, 2),
            SeedReward(find("strawberry_seed"), 8, 12, 2),
            SeedReward(find("barley_seed"), 8, 14, 2),
            SeedReward(find("hammerstone_hop_seed"), 6, 8, 2),
            SeedReward(find("asgarnian_hop_seed"), 6, 8, 2),
            SeedReward(find("jute_seed"), 8, 12, 2),
            SeedReward(find("yanillian_hop_seed"), 6, 8, 2),
            SeedReward(find("krandorian_hop_seed"), 6, 8, 2),
            SeedReward(find("acorn"), 3, 5, 2),
            SeedReward(find("apple_tree_seed"), 3, 5, 2),
            SeedReward(find("banana_tree_seed"), 3, 5, 2),
            SeedReward(find("orange_tree_seed"), 3, 5, 2),
            SeedReward(find("curry_tree_seed"), 3, 5, 2),
            SeedReward(find("redberry_bush_seed"), 6, 8, 2),
            SeedReward(find("cadavaberry_bush_seed"), 6, 8, 2),
            SeedReward(find("dwellberry_bush_seed"), 6, 8, 2),
            SeedReward(find("jangerberry_bush_seed"), 6, 8, 2),
            SeedReward(find("marigold_seed"), 8, 12, 2),
            SeedReward(find("rosemary_seed"), 8, 12, 2),
            SeedReward(find("nasturtium_seed"), 8, 12, 2),
            SeedReward(find("woad_seed"), 8, 12, 2),
            SeedReward(find("guam_seed"), 3, 5, 2),
            SeedReward(find("marrentill_seed"), 3, 5, 2),
            SeedReward(find("tarromin_seed"), 3, 5, 2),
            SeedReward(find("harralander_seed"), 3, 5, 2),
            SeedReward(find("mushroom_seed"), 4, 6, 1),
            SeedReward(find("belladonna_seed"), 4, 6, 1),
        )
    val medium =
        listOf(
            SeedReward(find("irit_seed"), 2, 6, 3),
            SeedReward(find("limpwurt_seed"), 4, 8, 3),
            SeedReward(find("watermelon_seed"), 8, 12, 2),
            SeedReward(find("snape_grass_seed"), 6, 8, 2),
            SeedReward(find("wildblood_hop_seed"), 8, 12, 2),
            SeedReward(find("whiteberry_bush_seed"), 6, 8, 2),
            SeedReward(find("poisonivy_bush_seed"), 6, 8, 2),
            SeedReward(find("cactus_seed"), 2, 6, 2),
            SeedReward(find("potato_cactus_seed"), 2, 6, 2),
            SeedReward(find("willow_seed"), 2, 4, 1),
            SeedReward(find("pineapple_tree_seed"), 3, 5, 1),
            SeedReward(find("toadflax_seed"), 1, 3, 1),
            SeedReward(find("avantoe_seed"), 1, 3, 1),
            SeedReward(find("kwuarm_seed"), 1, 3, 1),
            SeedReward(find("cadantine_seed"), 1, 3, 1),
            SeedReward(find("lantadyme_seed"), 1, 3, 1),
            SeedReward(find("dwarf_weed_seed"), 1, 3, 1),
            SeedReward(find("calquat_tree_seed"), 3, 6, 1),
            SeedReward(find("teak_seed"), 1, 3, 1),
            SeedReward(find("white_lily_seed"), 1, 1, 1),
        )
    val high =
        listOf(
            SeedReward(find("papaya_tree_seed"), 1, 3, 5),
            SeedReward(find("palm_tree_seed"), 1, 2, 5),
            SeedReward(find("hespori_seed"), 1, 1, 5),
            SeedReward(find("ranarr_seed"), 1, 2, 4),
            SeedReward(find("snapdragon_seed"), 1, 1, 4),
            SeedReward(find("maple_seed"), 1, 2, 4),
            SeedReward(find("mahogany_seed"), 1, 2, 4),
            SeedReward(find("yew_seed"), 1, 1, 3),
            SeedReward(find("dragonfruit_tree_seed"), 1, 1, 3),
            SeedReward(find("celastrus_tree_seed"), 1, 1, 2),
            SeedReward(find("torstol_seed"), 1, 1, 2),
            SeedReward(find("magic_tree_seed"), 1, 1, 1),
            SeedReward(find("spirit_tree_seed"), 1, 1, 1),
            SeedReward(find("redwood_tree_seed"), 1, 1, 1),
        )
}
