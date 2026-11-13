package org.rsmod.content.bosses.sporadic

import org.rsmod.api.random.GameRandom
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

internal data class SeedReward(val item: ObjType, val weight: Int, val count: IntRange)
internal object HesporiSeeds : ObjReferences() {
    val tangleroot = find("skillpetfarming")
    val main = listOf(
        SeedReward(find("watermelon_seed"), 2, 10..20),
        SeedReward(find("snape_grass_seed"), 2, 6..16),
        SeedReward(find("white_lily_seed"), 5, 8..18),
        SeedReward(find("limpwurt_seed"), 2, 6..14),
        SeedReward(find("wildblood_hop_seed"), 2, 10..20),
        SeedReward(find("whiteberry_bush_seed"), 2, 10..16),
        SeedReward(find("poisonivy_bush_seed"), 2, 8..16),
        SeedReward(find("irit_seed"), 3, 2..8),
        SeedReward(find("avantoe_seed"), 3, 2..5),
        SeedReward(find("kwuarm_seed"), 3, 2..5),
        SeedReward(find("toadflax_seed"), 3, 2..5),
        SeedReward(find("cadantine_seed"), 3, 2..5),
        SeedReward(find("lantadyme_seed"), 3, 2..5),
        SeedReward(find("dwarf_weed_seed"), 3, 2..5),
        SeedReward(find("ranarr_seed"), 2, 1..2),
        SeedReward(find("snapdragon_seed"), 2, 1..1),
        SeedReward(find("torstol_seed"), 2, 1..1),
        SeedReward(find("maple_seed"), 4, 2..4),
        SeedReward(find("willow_seed"), 3, 2..5),
        SeedReward(find("yew_seed"), 2, 1..1),
        SeedReward(find("magic_tree_seed"), 1, 1..1),
        SeedReward(find("pineapple_tree_seed"), 3, 3..6),
        SeedReward(find("papaya_tree_seed"), 3, 1..3),
        SeedReward(find("palm_tree_seed"), 3, 1..3),
        SeedReward(find("dragonfruit_tree_seed"), 2, 1..1),
        SeedReward(find("teak_seed"), 4, 2..5),
        SeedReward(find("mahogany_seed"), 3, 1..3),
        SeedReward(find("cactus_seed"), 2, 4..14),
        SeedReward(find("potato_cactus_seed"), 2, 4..14),
        SeedReward(find("celastrus_tree_seed"), 2, 1..1),
        SeedReward(find("spirit_tree_seed"), 1, 1..1),
        SeedReward(find("redwood_tree_seed"), 1, 1..1),
    )
}

/** A cultivated Hespori yields an anima seed roll and either a bucket or one normal seed roll. */
public object HesporiRewards {
    public fun harvest(random: GameRandom, farmingLevel: Int): List<InvObj> = buildList {
        val anima = listOf(SporadicBossObjs.attasSeed, SporadicBossObjs.iasorSeed, SporadicBossObjs.kronosSeed)
        add(InvObj(anima[random.of(anima.size)], random.of(1..2)))
        if (random.of(SporadicBossDrops.HESPORI_BUCKET_RATE) == 0) {
            add(InvObj(SporadicBossObjs.bottomlessCompostBucket))
        } else {
            var roll = random.of(80)
            val reward = HesporiSeeds.main.first {
                roll -= it.weight
                roll < 0
            }
            val count = if (reward.count.first == reward.count.last) reward.count.first else random.of(reward.count)
            add(InvObj(reward.item, count))
        }
        if (random.of(7000 - 25 * farmingLevel) == 0) add(InvObj(HesporiSeeds.tangleroot))
    }
}
