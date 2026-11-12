package org.rsmod.content.skills.hunter.moths

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

public object MothNpcs : NpcReferences() {
    public val sunlight = find("moth_sunlight")
    public val moonlight = find("moth_moonlight")
    public val moonlightLowWander = find("moth_moonlight_low_wander")
    public val moonlightNoClick = find("moth_moonlight_noclick")
}

public object MothObjs : ObjReferences() {
    public val butterflyNet = find("hunting_butterfly_net")
    public val magicButterflyNet = find("ii_magic_butterfly_net")
    public val emptyJar = find("butterfly_jar")
    public val sunlightJar = find("butterfly_jar_sunmoth")
    public val moonlightJar = find("butterfly_jar_moonmoth")
}

public object MothSeqs : SeqReferences() {
    public val catch = find("human_butterflynet_swing")
    public val openJar = find("hunting_opening_butterfly_jar")
    public val butterflyOut = find("hunting_butterfly_out_jar")
}

public data class MothRateCurve(val low: Int, val high: Int, val requirement: Int) {
    public fun chance(level: Int): Double =
        SkillingSuccessRate.successRate(low, high, level.coerceIn(1, MAX_LEVEL), MAX_LEVEL)
            .coerceAtMost(1.0)

    private companion object {
        const val MAX_LEVEL = 99
    }
}

public data class MothSpecies(
    val npcs: List<NpcType>,
    val level: Int,
    val barehandedLevel: Int,
    val xp: Double,
    val jar: ObjType,
    val regularRate: MothRateCurve,
    val enhancedRate: MothRateCurve,
    val effect: MothEffect,
)

public enum class MothEffect {
    Sunlight,
    Moonlight,
}

public object MothSpeciesList {
    public val sunlight: MothSpecies =
        MothSpecies(
            npcs = listOf(MothNpcs.sunlight),
            level = 65,
            barehandedLevel = 75,
            xp = 74.0,
            jar = MothObjs.sunlightJar,
            regularRate = MothRateCurve(low = 20, high = 296, requirement = 65),
            enhancedRate = MothRateCurve(low = 40, high = 316, requirement = 65),
            effect = MothEffect.Sunlight,
        )

    public val moonlight: MothSpecies =
        MothSpecies(
            npcs = listOf(MothNpcs.moonlight, MothNpcs.moonlightLowWander),
            level = 75,
            barehandedLevel = 85,
            xp = 84.0,
            jar = MothObjs.moonlightJar,
            regularRate = MothRateCurve(low = 0, high = 276, requirement = 75),
            enhancedRate = MothRateCurve(low = 20, high = 286, requirement = 75),
            effect = MothEffect.Moonlight,
        )

    public val all: List<MothSpecies> = listOf(sunlight, moonlight)
    public val byNpcId: Map<Int, MothSpecies> =
        all.flatMap { species -> species.npcs.map { it.id to species } }.toMap()
    public val byJarId: Map<Int, MothSpecies> = all.associateBy { it.jar.id }
}

public enum class MothCatchMethod {
    RegularNet,
    MagicNet,
    Barehanded,
}
