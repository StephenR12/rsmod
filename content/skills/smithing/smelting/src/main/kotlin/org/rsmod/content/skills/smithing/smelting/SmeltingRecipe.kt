package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.type.varp.VarpType

public sealed interface SmeltingUnlock {
    public val questName: String
    public val minimumProgress: Int

    public data class Varp(
        val type: VarpType,
        override val minimumProgress: Int,
        override val questName: String,
    ) : SmeltingUnlock

    public data class VarBit(
        val type: VarBitType,
        override val minimumProgress: Int,
        override val questName: String,
    ) : SmeltingUnlock
}

/** A cache-233 metal-bar recipe shared by furnaces and alternative smelting methods. */
public data class SmeltingRecipe(
    val name: String,
    val targetOres: Set<ObjType>,
    val oreCosts: Map<ObjType, Int>,
    val coalCount: Int,
    val output: ObjType,
    val level: Int,
    val xp: Double,
    val members: Boolean = false,
    val standardMenu: Boolean = true,
    val furnaceInitialTicks: Int = 4,
    val furnaceRepeatTicks: Int = 5,
    val unlock: SmeltingUnlock? = null,
) {
    public val catalystEligible: Boolean
        get() = coalCount > 0

    public fun coalCount(catalysed: Boolean): Int =
        if (catalysed && catalystEligible) coalCount / 2 else coalCount
}

public class SmeltingRecipeRegistry @Inject constructor() {
    public val recipes: List<SmeltingRecipe> =
        listOf(
            SmeltingRecipe(
                name = "Bronze bar",
                targetOres = setOf(objs.copper_ore, objs.tin_ore),
                oreCosts = mapOf(objs.copper_ore to 1, objs.tin_ore to 1),
                coalCount = 0,
                output = objs.bronze_bar,
                level = 1,
                xp = 6.2,
            ),
            SmeltingRecipe(
                name = "Iron bar",
                targetOres = setOf(objs.iron_ore),
                oreCosts = mapOf(objs.iron_ore to 1),
                coalCount = 0,
                output = objs.iron_bar,
                level = 15,
                xp = 12.5,
            ),
            SmeltingRecipe(
                name = "Silver bar",
                targetOres = setOf(objs.silver_ore),
                oreCosts = mapOf(objs.silver_ore to 1),
                coalCount = 0,
                output = objs.silver_bar,
                level = 20,
                xp = 13.7,
            ),
            SmeltingRecipe(
                name = "Steel bar",
                targetOres = setOf(objs.iron_ore),
                oreCosts = mapOf(objs.iron_ore to 1),
                coalCount = 2,
                output = objs.steel_bar,
                level = 30,
                xp = 17.5,
            ),
            SmeltingRecipe(
                name = "Gold bar",
                targetOres = setOf(objs.gold_ore),
                oreCosts = mapOf(objs.gold_ore to 1),
                coalCount = 0,
                output = objs.gold_bar,
                level = 40,
                xp = 22.5,
                furnaceInitialTicks = 5,
            ),
            SmeltingRecipe(
                name = "Lovakite bar",
                targetOres = setOf(objs.lovakite_ore),
                oreCosts = mapOf(objs.lovakite_ore to 1),
                coalCount = 2,
                output = objs.lovakite_bar,
                level = 45,
                xp = 20.0,
                members = true,
                standardMenu = false,
            ),
            SmeltingRecipe(
                name = "Blurite bar",
                targetOres = setOf(objs.blurite_ore),
                oreCosts = mapOf(objs.blurite_ore to 1),
                coalCount = 0,
                output = objs.blurite_bar,
                level = 13,
                xp = 8.0,
                members = true,
                standardMenu = false,
                unlock =
                    SmeltingUnlock.Varp(
                        type = varps.squire,
                        minimumProgress = 7,
                        questName = "The Knight's Sword",
                    ),
            ),
            SmeltingRecipe(
                name = "Lunar bar",
                targetOres = setOf(objs.lunar_ore),
                oreCosts = mapOf(objs.lunar_ore to 1),
                coalCount = 0,
                output = objs.lunar_bar,
                level = 1,
                xp = 6.2,
                members = true,
                standardMenu = false,
                furnaceInitialTicks = 6,
                furnaceRepeatTicks = 6,
                unlock =
                    SmeltingUnlock.VarBit(
                        type = varbits.lunar_quest_main,
                        minimumProgress = 155,
                        questName = "Lunar Diplomacy",
                    ),
            ),
            SmeltingRecipe(
                name = "Mithril bar",
                targetOres = setOf(objs.mithril_ore),
                oreCosts = mapOf(objs.mithril_ore to 1),
                coalCount = 4,
                output = objs.mithril_bar,
                level = 50,
                xp = 30.0,
            ),
            SmeltingRecipe(
                name = "Adamantite bar",
                targetOres = setOf(objs.adamantite_ore),
                oreCosts = mapOf(objs.adamantite_ore to 1),
                coalCount = 6,
                output = objs.adamantite_bar,
                level = 70,
                xp = 37.5,
            ),
            SmeltingRecipe(
                name = "Runite bar",
                targetOres = setOf(objs.runite_ore),
                oreCosts = mapOf(objs.runite_ore to 1),
                coalCount = 8,
                output = objs.runite_bar,
                level = 85,
                xp = 50.0,
            ),
        )

    public val standardMenu: List<SmeltingRecipe> = recipes.filter(SmeltingRecipe::standardMenu)

    public fun forTarget(target: ObjType): List<SmeltingRecipe> =
        recipes.filter { recipe -> recipe.targetOres.any { it.id == target.id } }

    public fun forOutput(output: ObjType): SmeltingRecipe? =
        recipes.firstOrNull { recipe -> recipe.output.id == output.id }
}
