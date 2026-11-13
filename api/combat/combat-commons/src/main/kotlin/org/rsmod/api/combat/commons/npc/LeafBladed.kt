package org.rsmod.api.combat.commons.npc

import org.rsmod.api.config.refs.objs
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjType

public object LeafBladed {
    private val weapons: List<ObjType> get() = listOf(objs.leafbladed_spear, objs.leafbladed_sword, objs.leafbladed_battleaxe)
    private val ammunition: List<ObjType> get() = listOf(objs.slayer_broad_arrow, objs.slayer_broad_bolts, objs.amethyst_broad_bolts)

    public fun required(type: UnpackedNpcType): Boolean = when (type.slayerFamily) {
        36, 45 -> true // Turoth and kurask, including superior variants.
        else -> false
    }

    public fun melee(weaponId: Int?): Boolean = weapons.any { it.id == weaponId }
    public fun ranged(ammoId: Int?): Boolean = ammunition.any { it.id == ammoId }
    public fun canDamage(hit: HitBuilder): Boolean = when (hit.type) {
        HitType.Melee -> weapons.any(hit::isRighthandObj)
        HitType.Ranged -> ammunition.any(hit::isSecondaryObj)
        HitType.Magic -> hit.isSecondaryObj(objs.spell_magic_dart)
        HitType.Typeless -> true
    }
}
