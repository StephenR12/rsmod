package org.rsmod.api.combat.commons.magic

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varp.VarpType
import org.rsmod.map.CoordGrid

public object MageArenaTraining : VarpReferences() {
    public const val KOLODION_DEFEATED: Int = 6
    public const val CHOSE_CAPE: Int = 7
    public const val CLAIMED_STAFF: Int = 8
    public const val REQUIRED_CASTS: Int = 100
    public val saradomin: VarpType = find("saramage")
    public val guthix: VarpType = find("guthmage")
    public val zamorak: VarpType = find("zamomage")

    public fun counter(spell: ObjType): VarpType =
        when (spell.id) {
            objs.spell_saradomin_strike.id -> saradomin
            objs.spell_claws_of_guthix.id -> guthix
            objs.spell_flames_of_zamorak.id -> zamorak
            else -> error("Unexpected Mage Arena spell: ${spell.id}")
        }

    public fun isPractice(caster: CoordGrid, target: PathingEntity): Boolean =
        target is Npc && target.id in 1610..1612 && inside(caster) && inside(target.coords)

    public fun inside(tile: CoordGrid): Boolean =
        tile.level == 0 && tile.x in 3086..3124 && tile.z in 3916..3952
}
