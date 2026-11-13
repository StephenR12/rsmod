package org.rsmod.content.skills.fishing

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public object AerialNpcs : NpcReferences() {
    val alry = find("fishing_npc_angler")
    val pool = find("fishing_spot_aerial")
}
public object AerialObjs : ObjReferences() {
    val glove = find("aerial_fishing_gloves_no_bird")
    val bird = find("aerial_fishing_gloves_bird")
    val chunks = find("fish_chunks")
    val worm = find("king_worm")
    val pearl = find("aerial_fishing_pearl")
    val goldenTench = find("golden_tench")
}
public object AerialLocs : LocReferences() {
    val boat = find("aerial_fishing_boat")
    val worms = find("aerial_fishing_king_worm")
}
public object AerialSequences : SeqReferences() { val launch = find("human_falconer_attack") }
public object AerialTimers : TimerReferences() { val lifecycle = find("aerial_fishing_lifecycle") }
public object AerialVars : VarpReferences() { val chunkCatches = find("aerial_fishing_chunk_catches") }
internal object AerialVarpBuilder : VarpBuilder() {
    init { build("aerial_fishing_chunk_catches") { transmitNever = true } }
}
internal object AerialPoolEditor : NpcEditor() {
    init { edit(AerialNpcs.pool) { defaultMode = NpcMode.None; timer = 5 } }
}
internal object AerialSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<AerialFishingScript>("aerial-fishing-npcs.toml") }
}

public data class AerialFish(val obj: ObjType, val fishingLevel: Int, val hunterLevel: Int, val fishingXp: Double, val hunterXp: Double, val cookingXp: Double, val threshold: Int)
public object AerialFishTable {
    val all = listOf(
        AerialFish(objs.raw_bluegill, 43, 35, 11.5, 16.5, 3.5, 0),
        AerialFish(objs.raw_common_tench, 56, 51, 40.0, 45.0, 10.0, 52),
        AerialFish(objs.raw_mottled_eel, 73, 68, 65.0, 90.0, 20.0, 67),
        AerialFish(objs.raw_greater_siren, 91, 87, 100.0, 130.0, 25.0, 82),
    )
    public fun bound(fishing: Int, hunter: Int): Int = ((2 * fishing + hunter) / 3).coerceAtLeast(1)
    public fun select(fishing: Int, hunter: Int, roll: Int): AerialFish {
        require(roll in 0 until bound(fishing, hunter))
        return all.asReversed().firstOrNull { roll >= it.threshold && fishing >= it.fishingLevel && hunter >= it.hunterLevel } ?: all.first()
    }
    public fun pearlDenominator(fishing: Int, hunter: Int): Int = (100 - ((bound(fishing, hunter) - 40) * 25 / 59)).coerceAtLeast(1)
}

public object AerialPoolPositions {
    // Fishing spot (aerial fishing) oldid14992159, 2025-09-25. Six active pools is a bounded population model.
    val all = listOf(CoordGrid(1382,3627), CoordGrid(1354,3640), CoordGrid(1380,3631), CoordGrid(1353,3630), CoordGrid(1380,3627), CoordGrid(1354,3634), CoordGrid(1353,3628), CoordGrid(1353,3631), CoordGrid(1355,3638), CoordGrid(1371,3624), CoordGrid(1380,3632), CoordGrid(1382,3626), CoordGrid(1382,3633), CoordGrid(1379,3628), CoordGrid(1379,3634), CoordGrid(1381,3631), CoordGrid(1379,3622), CoordGrid(1358,3639), CoordGrid(1357,3629), CoordGrid(1379,3627))
}

public data class AerialReward(val obj: ObjType, val pearls: Int, val barbarianTraining: Boolean = false)
public object AerialRewards {
    val rods = listOf(AerialReward(objs.pearl_fishing_rod, 100), AerialReward(objs.pearl_fly_fishing_rod, 120), AerialReward(objs.pearl_barbarian_rod, 150, true))
    val outfit = listOf(AerialReward(FishingOutfits.hat, 100), AerialReward(FishingOutfits.top, 100), AerialReward(FishingOutfits.legs, 100), AerialReward(FishingOutfits.boots, 100))
    val sack = AerialReward(objs.fish_sack, 1000)
}
