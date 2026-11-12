package org.rsmod.content.skills.magic.spell.attacks.ancient

import org.rsmod.game.entity.Player

public data class AncientImpact(
    val source: Player,
    val family: AncientSpellFamily,
    val effectStrength: Int,
    val damage: Int,
    val ancientSceptre: Boolean,
    val familySceptre: Boolean,
    val bloodbarkPieces: Int,
    val poisonProc: Boolean,
)
