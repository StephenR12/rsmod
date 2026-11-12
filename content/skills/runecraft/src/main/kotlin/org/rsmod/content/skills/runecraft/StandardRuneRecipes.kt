package org.rsmod.content.skills.runecraft

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varbit.VarBitType

public data class RuneAltarQuestRequirement(
    val varbit: VarBitType,
    val completionValue: Int,
    val questName: String,
)

public data class StandardRuneRecipe(
    val altar: LocType,
    val output: ObjType,
    val level: Int,
    val xp: Double,
    val name: String,
    val members: Boolean,
    val runeEssence: Boolean,
    val multiplierLevels: List<Int>,
    val extract: ObjType?,
    val extractBonus: Int,
    val quest: RuneAltarQuestRequirement? = null,
) {
    public fun outputMultiplier(visibleLevel: Int): Int =
        multiplierLevels.count { visibleLevel >= it }.coerceAtLeast(1)
}

/**
 * The ordinary true-altar table at the cache-233.4 cutoff. Kourend dense essence, Ourania,
 * combination runes, and sunfire runes are separate mechanics and deliberately do not share this
 * recipe path.
 */
public object StandardRuneRecipes {
    public val air: StandardRuneRecipe =
        recipe(
            RunecraftLocs.airAltar,
            objs.air_rune,
            1,
            5.0,
            "air",
            false,
            true,
            1,
            11,
            22,
            33,
            44,
            55,
            66,
            77,
            88,
            99,
        )
    public val mind: StandardRuneRecipe =
        recipe(
            RunecraftLocs.mindAltar,
            objs.mind_rune,
            2,
            5.5,
            "mind",
            false,
            true,
            2,
            14,
            28,
            42,
            56,
            70,
            84,
            98,
        )
    public val water: StandardRuneRecipe =
        recipe(
            RunecraftLocs.waterAltar,
            objs.water_rune,
            5,
            6.0,
            "water",
            false,
            true,
            5,
            19,
            38,
            57,
            76,
            95,
        )
    public val earth: StandardRuneRecipe =
        recipe(
            RunecraftLocs.earthAltar,
            objs.earth_rune,
            9,
            6.5,
            "earth",
            false,
            true,
            9,
            26,
            52,
            78,
            104,
        )
    public val fire: StandardRuneRecipe =
        recipe(RunecraftLocs.fireAltar, objs.fire_rune, 14, 7.0, "fire", false, true, 14, 35, 70)
    public val body: StandardRuneRecipe =
        recipe(RunecraftLocs.bodyAltar, objs.body_rune, 20, 7.5, "body", false, true, 20, 46, 92)
    public val cosmic: StandardRuneRecipe =
        recipe(RunecraftLocs.cosmicAltar, objs.cosmic_rune, 27, 8.0, "cosmic", true, false, 27, 59)
    public val chaos: StandardRuneRecipe =
        recipe(RunecraftLocs.chaosAltar, objs.chaos_rune, 35, 8.5, "chaos", true, false, 35, 74)
    public val astral: StandardRuneRecipe =
        recipe(
            RunecraftLocs.astralAltar,
            objs.astral_rune,
            40,
            8.7,
            "astral",
            true,
            false,
            40,
            82,
            quest =
                RuneAltarQuestRequirement(RunecraftVarbits.lunarDiplomacy, 190, "Lunar Diplomacy"),
        )
    public val nature: StandardRuneRecipe =
        recipe(RunecraftLocs.natureAltar, objs.nature_rune, 44, 9.0, "nature", true, false, 44, 91)
    public val law: StandardRuneRecipe =
        recipe(RunecraftLocs.lawAltar, objs.law_rune, 54, 9.5, "law", true, false, 54, 95)
    public val death: StandardRuneRecipe =
        recipe(
            RunecraftLocs.deathAltar,
            objs.death_rune,
            65,
            10.0,
            "death",
            true,
            false,
            65,
            99,
            quest =
                RuneAltarQuestRequirement(
                    RunecraftVarbits.mourningEndsPartTwo,
                    60,
                    "Mourning's End Part II",
                ),
        )
    public val blood: StandardRuneRecipe =
        recipe(
            RunecraftLocs.bloodAltar,
            objs.blood_rune,
            77,
            10.5,
            "blood",
            true,
            false,
            77,
            quest =
                RuneAltarQuestRequirement(
                    RunecraftVarbits.sinsOfTheFather,
                    138,
                    "Sins of the Father",
                ),
        )
    public val wrath: StandardRuneRecipe =
        recipe(
            RunecraftLocs.wrathAltar,
            objs.wrath_rune,
            95,
            8.0,
            "wrath",
            true,
            false,
            95,
            quest =
                RuneAltarQuestRequirement(RunecraftVarbits.dragonSlayerTwo, 215, "Dragon Slayer II"),
        )

    public val all: List<StandardRuneRecipe> =
        listOf(
            air,
            mind,
            water,
            earth,
            fire,
            body,
            cosmic,
            chaos,
            astral,
            nature,
            law,
            death,
            blood,
            wrath,
        )

    private fun recipe(
        altar: LocType,
        output: ObjType,
        level: Int,
        xp: Double,
        name: String,
        members: Boolean,
        runeEssence: Boolean,
        vararg multiplierLevels: Int,
        quest: RuneAltarQuestRequirement? = null,
    ): StandardRuneRecipe {
        val (extract, bonus) =
            when (name) {
                "air",
                "mind",
                "water",
                "earth",
                "fire",
                "body" -> RunecraftObjs.warpedExtract to 250
                "cosmic",
                "chaos" -> RunecraftObjs.twistedExtract to 60
                "astral",
                "nature",
                "law",
                "death" -> RunecraftObjs.mangledExtract to 60
                "blood",
                "wrath" -> RunecraftObjs.scarredExtract to 60
                else -> null to 0
            }
        return StandardRuneRecipe(
            altar,
            output,
            level,
            xp,
            name,
            members,
            runeEssence,
            multiplierLevels.toList(),
            extract,
            bonus,
            quest,
        )
    }
}
