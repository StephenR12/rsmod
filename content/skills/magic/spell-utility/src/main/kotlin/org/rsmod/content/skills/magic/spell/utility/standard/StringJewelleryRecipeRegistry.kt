package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.content.skills.crafting.jewellery.AmuletStringingRecipe
import org.rsmod.content.skills.crafting.jewellery.AmuletStringingRecipeRegistry

public class StringJewelleryRecipeRegistry
@Inject
constructor(base: AmuletStringingRecipeRegistry) {
    public val recipes: List<AmuletStringingRecipe> =
        base.recipes.filterNot { it.unstrung == objs.unstrung_m_speak_amulet }

    private val byUnstrungId: Map<Int, AmuletStringingRecipe> =
        recipes.associateBy { it.unstrung.id }

    public fun forUnstrung(unstrungId: Int): AmuletStringingRecipe? = byUnstrungId[unstrungId]
}
