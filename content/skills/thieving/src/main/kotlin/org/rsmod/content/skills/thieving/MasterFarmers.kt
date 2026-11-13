package org.rsmod.content.skills.thieving

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.player.stat.baseFarmingLvl
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.obj.ObjType
import kotlin.math.roundToInt

public object MasterFarmerObjs : ObjReferences() {
    val asgarnian_hop_seed = find("asgarnian_hop_seed")
    val avantoe_seed = find("avantoe_seed")
    val barley_seed = find("barley_seed")
    val belladonna_seed = find("belladonna_seed")
    val cabbage_seed = find("cabbage_seed")
    val cactus_seed = find("cactus_seed")
    val cadantine_seed = find("cadantine_seed")
    val cadavaberry_bush_seed = find("cadavaberry_bush_seed")
    val dwarf_weed_seed = find("dwarf_weed_seed")
    val dwellberry_bush_seed = find("dwellberry_bush_seed")
    val guam_seed = find("guam_seed")
    val hammerstone_hop_seed = find("hammerstone_hop_seed")
    val harralander_seed = find("harralander_seed")
    val irit_seed = find("irit_seed")
    val jangerberry_bush_seed = find("jangerberry_bush_seed")
    val jute_seed = find("jute_seed")
    val krandorian_hop_seed = find("krandorian_hop_seed")
    val kwuarm_seed = find("kwuarm_seed")
    val lantadyme_seed = find("lantadyme_seed")
    val limpwurt_seed = find("limpwurt_seed")
    val marigold_seed = find("marigold_seed")
    val marrentill_seed = find("marrentill_seed")
    val mushroom_seed = find("mushroom_seed")
    val nasturtium_seed = find("nasturtium_seed")
    val onion_seed = find("onion_seed")
    val poisonivy_bush_seed = find("poisonivy_bush_seed")
    val potato_cactus_seed = find("potato_cactus_seed")
    val potato_seed = find("potato_seed")
    val ranarr_seed = find("ranarr_seed")
    val redberry_bush_seed = find("redberry_bush_seed")
    val rosemary_seed = find("rosemary_seed")
    val seaweed_seed = find("seaweed_seed")
    val snapdragon_seed = find("snapdragon_seed")
    val snape_grass_seed = find("snape_grass_seed")
    val strawberry_seed = find("strawberry_seed")
    val sweetcorn_seed = find("sweetcorn_seed")
    val tarromin_seed = find("tarromin_seed")
    val toadflax_seed = find("toadflax_seed")
    val tomato_seed = find("tomato_seed")
    val torstol_seed = find("torstol_seed")
    val watermelon_seed = find("watermelon_seed")
    val whiteberry_bush_seed = find("whiteberry_bush_seed")
    val wildblood_hop_seed = find("wildblood_hop_seed")
    val woad_seed = find("woad_seed")
    val yanillian_hop_seed = find("yanillian_hop_seed")
}

public object MasterFarmerNpcs : NpcReferences() {
    val martin = find("martin_the_master_farmer")
    val ordinary = listOf("master_farmer_1", "master_farmer_2", "master_farmer_1_f", "master_farmer_2_f") .map(::find)
    val varlamore = (1..4).flatMap { listOf(find("varlamore_master_farmer_m_$it"), find("varlamore_master_farmer_f_$it")) }
    val kastori = (1..2).flatMap { listOf(find("kastori_master_farmer_m_$it"), find("kastori_master_farmer_f_$it")) }
}

/** Master Farmer oldid14966702 and seed calculator oldid14718052 (before 2025-10-01).
 * The non-herb subtable odds reproduce the published three-significant-figure rates;
 * exact unrounded server weights are unavailable. Herb redistribution follows the dated formula.
 */
public object MasterFarmerSeeds {
    private fun seed(obj: ObjType, denominator: Double, maximum: Int = 1): PickpocketReward =
        PickpocketReward(obj, weight = (1_000_000 / denominator).roundToInt(), countRange = 1..maximum)
    val allotments = listOf(seed(MasterFarmerObjs.potato_seed, 5.65, 4), seed(MasterFarmerObjs.onion_seed, 7.53, 3), seed(MasterFarmerObjs.cabbage_seed, 14.4, 3), seed(MasterFarmerObjs.tomato_seed, 15.7, 2), seed(MasterFarmerObjs.sweetcorn_seed, 45.2, 2), seed(MasterFarmerObjs.strawberry_seed, 90.4), seed(MasterFarmerObjs.watermelon_seed, 189.0), seed(MasterFarmerObjs.snape_grass_seed, 260.0))
    val hops = listOf(seed(MasterFarmerObjs.barley_seed, 18.0, 12), seed(MasterFarmerObjs.hammerstone_hop_seed, 18.0, 9), seed(MasterFarmerObjs.asgarnian_hop_seed, 23.9, 6), seed(MasterFarmerObjs.jute_seed, 24.1, 9), seed(MasterFarmerObjs.yanillian_hop_seed, 36.1, 6), seed(MasterFarmerObjs.krandorian_hop_seed, 72.2, 6), seed(MasterFarmerObjs.wildblood_hop_seed, 142.0, 3))
    val flowers = listOf(seed(MasterFarmerObjs.marigold_seed, 21.8), seed(MasterFarmerObjs.nasturtium_seed, 32.9), seed(MasterFarmerObjs.rosemary_seed, 50.9), seed(MasterFarmerObjs.woad_seed, 68.9), seed(MasterFarmerObjs.limpwurt_seed, 86.3))
    val bushes = listOf(seed(MasterFarmerObjs.redberry_bush_seed, 25.8), seed(MasterFarmerObjs.cadavaberry_bush_seed, 36.8), seed(MasterFarmerObjs.dwellberry_bush_seed, 51.5), seed(MasterFarmerObjs.jangerberry_bush_seed, 129.0), seed(MasterFarmerObjs.whiteberry_bush_seed, 355.0), seed(MasterFarmerObjs.poisonivy_bush_seed, 937.0))
    val special = listOf(seed(MasterFarmerObjs.mushroom_seed, 492.0), seed(MasterFarmerObjs.belladonna_seed, 820.0), seed(MasterFarmerObjs.cactus_seed, 1230.0), seed(MasterFarmerObjs.seaweed_seed, 1892.0), seed(MasterFarmerObjs.potato_cactus_seed, 2460.0))
    val valuableHerbs = listOf(PickpocketReward(MasterFarmerObjs.ranarr_seed, weight = 69), PickpocketReward(MasterFarmerObjs.snapdragon_seed, weight = 10), PickpocketReward(MasterFarmerObjs.torstol_seed, weight = 2))
    private val otherHerbs = listOf(PickpocketReward(MasterFarmerObjs.marrentill_seed, weight = 218), PickpocketReward(MasterFarmerObjs.tarromin_seed, weight = 149), PickpocketReward(MasterFarmerObjs.harralander_seed, weight = 101), PickpocketReward(MasterFarmerObjs.toadflax_seed, weight = 47), PickpocketReward(MasterFarmerObjs.irit_seed, weight = 32), PickpocketReward(MasterFarmerObjs.avantoe_seed, weight = 22), PickpocketReward(MasterFarmerObjs.kwuarm_seed, weight = 15), PickpocketReward(MasterFarmerObjs.cadantine_seed, weight = 7), PickpocketReward(MasterFarmerObjs.lantadyme_seed, weight = 5), PickpocketReward(MasterFarmerObjs.dwarf_weed_seed, weight = 3))
    val all = allotments + hops + flowers + bushes + special + otherHerbs + valuableHerbs + PickpocketReward(MasterFarmerObjs.guam_seed)
    public fun valuableSlots(farming: Int): Int = 6 + farming.coerceIn(1, 85)
    public fun herb(access: ProtectedAccess): PickpocketReward = with(access) {
        val specialSlots = valuableSlots(player.baseFarmingLvl)
        val roll = random.of(1000)
        if (roll < specialSlots) return@with weighted(valuableHerbs, random.of(81))
        if (roll < 401) return@with PickpocketReward(MasterFarmerObjs.guam_seed)
        weighted(otherHerbs, roll - 401)
    }
    public fun select(access: ProtectedAccess): PickpocketReward = with(access) {
        val table = when (random.of(1000)) {
            in 0..484 -> allotments
            in 485..727 -> hops
            in 728..849 -> flowers
            in 850..946 -> bushes
            in 947..994 -> return@with herb(this)
            else -> special
        }
        weighted(table, random.of(table.sumOf { it.weight }))
    }
    public fun weighted(table: List<PickpocketReward>, roll: Int): PickpocketReward {
        require(roll in 0 until table.sumOf { it.weight })
        var cursor = roll
        for (reward in table) { cursor -= reward.weight; if (cursor < 0) return reward }
        error("Unreachable master farmer seed roll")
    }
    val definition = PickpocketDefinition("master farmer", MasterFarmerNpcs.ordinary + MasterFarmerNpcs.varlamore + MasterFarmerNpcs.kastori + MasterFarmerNpcs.martin, 38, 43.0, 90, 240, all, 3, 8, 9, requiresFreeInventorySlot = true, rewardSelector = ::select)
}
