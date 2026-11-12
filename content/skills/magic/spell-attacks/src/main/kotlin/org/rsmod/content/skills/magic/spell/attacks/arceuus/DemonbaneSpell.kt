package org.rsmod.content.skills.magic.spell.attacks.arceuus

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.spot.SpotanimType

public data class DemonbaneSpellDefinition(
    val spell: ObjType,
    val baseMaxHit: Int,
    val requiresKingdomDivided: Boolean,
    val castSpot: SpotanimType,
    val hitSpot: SpotanimType,
)

public object DemonbaneSpellDefinitions {
    public val all: List<DemonbaneSpellDefinition> =
        listOf(
            DemonbaneSpellDefinition(
                objs.spell_inferior_demonbane,
                baseMaxHit = 16,
                requiresKingdomDivided = false,
                castSpot = spotanims.inferior_demonbane_cast,
                hitSpot = spotanims.inferior_demonbane_hit,
            ),
            DemonbaneSpellDefinition(
                objs.spell_superior_demonbane,
                baseMaxHit = 23,
                requiresKingdomDivided = true,
                castSpot = spotanims.superior_demonbane_cast,
                hitSpot = spotanims.superior_demonbane_hit,
            ),
            DemonbaneSpellDefinition(
                objs.spell_dark_demonbane,
                baseMaxHit = 30,
                requiresKingdomDivided = true,
                castSpot = spotanims.dark_demonbane_cast,
                hitSpot = spotanims.dark_demonbane_hit,
            ),
        )
}
