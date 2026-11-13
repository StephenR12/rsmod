package org.rsmod.api.combat.commons.ranged

import org.rsmod.api.config.refs.objs

/** Both ogre bows use hunting ammunition. The original bow accepts brutals up to mithril. */
public object OgreBows {
    public fun isBow(id: Int?): Boolean = id == objs.ogre_bow.id || id == objs.comp_ogre_bow.id

    public fun isHuntingArrow(id: Int?): Boolean = id == objs.ogre_arrow.id ||
        id == objs.bronze_brutal.id || id == objs.iron_brutal.id || id == objs.steel_brutal.id ||
        id == objs.black_brutal.id || id == objs.mithril_brutal.id || id == objs.adamant_brutal.id ||
        id == objs.rune_brutal.id

    public fun canFire(bow: Int?, arrow: Int?): Boolean {
        if (!isBow(bow) || !isHuntingArrow(arrow)) return false
        return bow == objs.comp_ogre_bow.id || (arrow != objs.adamant_brutal.id && arrow != objs.rune_brutal.id)
    }

    public fun isGameBird(npc: Int): Boolean = npc == 1475 || npc == 4863
}
