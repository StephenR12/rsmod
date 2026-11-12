package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.map.square.MapSquareKey

@Singleton
public class FarmingPatchCodec
@Inject
constructor(private val locTypes: LocTypeList, private val varbits: VarBitTypeList) {
    public fun patchType(type: UnpackedLocType): FarmingPatchType? {
        if (type.multiVarBit <= 0 || type.multiLoc.isEmpty()) return null
        val name = type.internalName.orEmpty()
        return when {
            name.startsWith("farming_veg_patch") -> FarmingPatchType.Allotment
            name.startsWith("farming_flower_patch") -> FarmingPatchType.Flower
            name.startsWith("farming_herb_patch") || name.endsWith("herbpatch") ->
                FarmingPatchType.Herb
            name.startsWith("farming_hops_patch") -> FarmingPatchType.Hops
            name.startsWith("farming_bush_patch") -> FarmingPatchType.Bush
            name.startsWith("farming_fruit_tree_patch") -> FarmingPatchType.FruitTree
            name.startsWith("farming_hardwood_tree_patch") -> FarmingPatchType.HardwoodTree
            name.startsWith("farming_spirit_tree_patch") -> FarmingPatchType.SpiritTree
            name.startsWith("farming_cactus_patch") -> FarmingPatchType.Cactus
            name.startsWith("farming_calquat_tree_patch") -> FarmingPatchType.Calquat
            name.startsWith("farming_celastrus_patch") -> FarmingPatchType.Celastrus
            name.startsWith("farming_redwood_tree_patch") -> FarmingPatchType.Redwood
            name.startsWith("farming_crystal_tree_patch") -> FarmingPatchType.CrystalTree
            name.startsWith("farming_anima_patch") -> FarmingPatchType.Anima
            name.startsWith("farming_seaweed_patch") -> FarmingPatchType.Seaweed
            name.startsWith("farming_mushroom_patch") -> FarmingPatchType.Mushroom
            name.startsWith("farming_belladonna_patch") -> FarmingPatchType.Belladonna
            name == "hespori_patch" -> FarmingPatchType.Hespori
            name.startsWith("farming_compost") -> FarmingPatchType.Compost
            name.startsWith("grapevine_patch_clickzone") -> FarmingPatchType.Grapes
            name.startsWith("farming_tree_patch") -> FarmingPatchType.Tree
            else -> null
        }
    }

    public fun patchKey(loc: BoundLocInfo, type: UnpackedLocType): String =
        patchKey(loc.coords.let(MapSquareKey::from).id, type)

    public fun patchKey(loc: LocInfo, type: UnpackedLocType): String =
        patchKey(loc.coords.let(MapSquareKey::from).id, type)

    private fun patchKey(square: Int, type: UnpackedLocType): String =
        "$square:${type.multiVarBit}:${patchType(type)?.name ?: "Unknown"}"

    public fun sync(
        player: Player,
        base: UnpackedLocType,
        state: FarmingPatchState,
        crop: FarmingCrop?,
    ) {
        val varbit = varbits[base.multiVarBit] ?: return
        val value = visualValue(base, state, crop) ?: return
        VarPlayerIntMapSetter.set(player, varbit, value)
    }

    public fun visualValue(
        base: UnpackedLocType,
        state: FarmingPatchState,
        crop: FarmingCrop?,
    ): Int? {
        val variants =
            base.multiLoc.indices.mapNotNull { index ->
                val id = base.multiLoc[index].toInt() and 0xFFFF
                locTypes[id]?.internalName?.let { Variant(index, it) }
            }
        if (state.patchType == FarmingPatchType.Compost) {
            val token =
                when (state.compostBinStatus) {
                    CompostBinStatus.Rotting ->
                        when (state.compostBinKind) {
                            CompostBinKind.Supercompost -> "supercompost_rotting"
                            CompostBinKind.RottenTomatoes -> "rottentomatoes_rotting"
                            else -> "compost_rotting"
                        }
                    CompostBinStatus.Ready ->
                        when (state.compostBinKind) {
                            CompostBinKind.Supercompost ->
                                if (
                                    state.compostBinCount ==
                                        if (base.internalName.orEmpty().contains("bigbin")) 30
                                        else 15
                                )
                                    "supercompost_ready"
                                else
                                    "supercompost_${state.compostBinCount.toString().padStart(2, '0')}"
                            CompostBinKind.RottenTomatoes ->
                                if (
                                    state.compostBinCount ==
                                        if (base.internalName.orEmpty().contains("bigbin")) 30
                                        else 15
                                )
                                    "rottentomatoes_ready"
                                else
                                    "rottentomatoes_${state.compostBinCount.toString().padStart(2, '0')}"
                            CompostBinKind.Ultracompost ->
                                "ultracompost_${state.compostBinCount.toString().padStart(2, '0')}"
                            else ->
                                if (
                                    state.compostBinCount ==
                                        if (base.internalName.orEmpty().contains("bigbin")) 30
                                        else 15
                                )
                                    "compost_ready"
                                else "compost_${state.compostBinCount.toString().padStart(2, '0')}"
                        }
                    CompostBinStatus.Open ->
                        when (state.compostBinKind) {
                            CompostBinKind.Empty -> "empty"
                            CompostBinKind.Compost ->
                                "compostable_${state.compostBinCount.toString().padStart(2, '0')}"
                            CompostBinKind.Supercompost ->
                                "supercompostable_${state.compostBinCount.toString().padStart(2, '0')}"
                            CompostBinKind.RottenTomatoes ->
                                "tomatoes_${state.compostBinCount.toString().padStart(2, '0')}"
                            CompostBinKind.Ultracompost ->
                                "ultracompost_${state.compostBinCount.toString().padStart(2, '0')}"
                        }
                }
            return variants.firstOrNull { token in it.name }?.value ?: variants.firstOrNull()?.value
        }
        if (state.isEmpty || crop == null) {
            if (state.scarecrow) {
                val token =
                    if (state.weeds == 0) "farming_scarecrow"
                    else "scarecrow_weeds_${state.weeds.coerceIn(1, 3)}"
                return variants.firstOrNull { token in it.name }?.value
                    ?: variants.firstOrNull()?.value
            }
            val weedToken =
                when (state.weeds.coerceIn(0, 3)) {
                    0 -> "weeded"
                    else -> "weeds_${state.weeds.coerceIn(1, 3)}"
                }
            return variants.firstOrNull { it.name.contains(weedToken) }?.value
                ?: variants.firstOrNull()?.value
        }

        fun Variant.matchesCrop(): Boolean =
            crop.visualPrefixes.any { prefix ->
                (name == prefix || name.startsWith("${prefix}_") || name.startsWith(prefix)) &&
                    "patch_" !in name
            }

        val matches = variants.filter(Variant::matchesCrop)
        val candidates =
            when (state.status) {
                FarmingCropStatus.Diseased -> matches.filter { "diseased" in it.name }
                FarmingCropStatus.Dead -> matches.filter { "dead" in it.name }
                FarmingCropStatus.Harvestable ->
                    matches.filter {
                        "fullygrown" in it.name ||
                            "harvest" in it.name ||
                            "grown" in it.name ||
                            "fruit_" in it.name ||
                            "spines_" in it.name
                    }
                FarmingCropStatus.Stump -> matches.filter { "stump" in it.name }
                FarmingCropStatus.Growing ->
                    matches.filter {
                        (if (state.watered) "watered" in it.name else "watered" !in it.name) &&
                            "diseased" !in it.name &&
                            "dead" !in it.name &&
                            "fullygrown" !in it.name &&
                            "harvest" !in it.name &&
                            "fruit_" !in it.name &&
                            "spines_" !in it.name &&
                            "claimxp" !in it.name
                    }
            }
        if (candidates.isEmpty()) return matches.firstOrNull()?.value
        return when (state.status) {
            FarmingCropStatus.Harvestable -> {
                val harvested = (crop.baseHarvestLives - state.harvestLives).coerceAtLeast(0)
                candidates[harvested.coerceAtMost(candidates.lastIndex)].value
            }
            FarmingCropStatus.Stump -> candidates.first().value
            else -> candidates[state.stage.coerceIn(0, candidates.lastIndex)].value
        }
    }

    public fun visualType(
        base: UnpackedLocType,
        state: FarmingPatchState,
        crop: FarmingCrop?,
    ): UnpackedLocType? {
        val index = visualValue(base, state, crop) ?: return null
        val id = base.multiLoc.getOrNull(index)?.toInt()?.and(0xFFFF) ?: return null
        return locTypes[id]
    }

    private data class Variant(val value: Int, val name: String)
}
