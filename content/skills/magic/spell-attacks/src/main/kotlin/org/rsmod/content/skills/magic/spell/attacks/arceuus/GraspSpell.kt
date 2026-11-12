package org.rsmod.content.skills.magic.spell.attacks.arceuus

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.spot.SpotanimType

public data class GraspSpellDefinition(
    val spell: ObjType,
    val baseMaxHit: Int,
    val bindChancePercent: Int,
    val bindDuration: Int,
    val requiresKingdomDivided: Boolean,
    val castSpot: SpotanimType,
    val hitSpot: SpotanimType,
    val missSpot: SpotanimType,
) {
    public fun bindChance(markOfDarkness: Boolean): Int =
        if (markOfDarkness) minOf(100, bindChancePercent * 2) else bindChancePercent
}

public object GraspSpellDefinitions {
    public val all: List<GraspSpellDefinition> =
        listOf(
            GraspSpellDefinition(
                objs.spell_ghostly_grasp,
                baseMaxHit = 12,
                bindChancePercent = 10,
                bindDuration = 2,
                requiresKingdomDivided = false,
                castSpot = spotanims.ghostly_grasp_cast,
                hitSpot = spotanims.ghostly_grasp_hit,
                missSpot = spotanims.ghostly_grasp_miss,
            ),
            GraspSpellDefinition(
                objs.spell_skeletal_grasp,
                baseMaxHit = 17,
                bindChancePercent = 25,
                bindDuration = 3,
                requiresKingdomDivided = true,
                castSpot = spotanims.skeletal_grasp_cast,
                hitSpot = spotanims.skeletal_grasp_hit,
                missSpot = spotanims.skeletal_grasp_miss,
            ),
            GraspSpellDefinition(
                objs.spell_undead_grasp,
                baseMaxHit = 24,
                bindChancePercent = 50,
                bindDuration = 4,
                requiresKingdomDivided = true,
                castSpot = spotanims.undead_grasp_cast,
                hitSpot = spotanims.undead_grasp_hit,
                missSpot = spotanims.undead_grasp_miss,
            ),
        )
}

internal data class GraspImpact(
    val source: org.rsmod.game.entity.Player,
    val bindDuration: Int,
    val bind: Boolean,
)
