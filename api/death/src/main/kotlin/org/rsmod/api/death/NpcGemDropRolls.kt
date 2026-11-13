package org.rsmod.api.death

import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType

/** Cache-233 object references used by the standard gem and mega-rare drop tables. */
public object NpcGemDropObjs : ObjReferences() {
    internal val wealth = listOf("ring_of_wealth","ring_of_wealth_i").flatMap { name ->
        listOf(name) + (1..5).map { if (name.endsWith("_i")) "$name$it" else "${name}_$it" }
    }.map { find(it) }
    public val runeJavelin: ObjType = find("rune_javelin")
    public val loopHalfOfKey: ObjType = find("keyhalf2")
    public val toothHalfOfKey: ObjType = find("keyhalf1")
    public val runeSpear: ObjType = find("rune_spear")
    public val shieldLeftHalf: ObjType = find("dragonshield_a")
    public val dragonSpear: ObjType = find("dragon_spear")
}

/** Shared cache-233 standard gem table, including its nested mega-rare table. */
public object NpcGemDropRolls {
    public fun wearingWealth(context: NpcDropContext): Boolean =
        NpcGemDropObjs.wealth.any(context.hero.worn::contains)

    /** Direct access to the mega-rare table does not require Legends' Quest. */
    public fun megaRare(hasRingOfWealth: (NpcDropContext) -> Boolean): NpcDropRoll {
        val ordinaryMega =
            weighted(
                NpcWeightedDrop(113, nothing()),
                NpcWeightedDrop(8, item(NpcGemDropObjs.runeSpear)),
                NpcWeightedDrop(4, item(NpcGemDropObjs.shieldLeftHalf)),
                NpcWeightedDrop(3, item(NpcGemDropObjs.dragonSpear)),
            )
        val luckyMega =
            weighted(
                NpcWeightedDrop(8, item(NpcGemDropObjs.runeSpear)),
                NpcWeightedDrop(4, item(NpcGemDropObjs.shieldLeftHalf)),
                NpcWeightedDrop(3, item(NpcGemDropObjs.dragonSpear)),
            )
        return NpcDropRoll { context ->
            if (hasRingOfWealth(context)) {
                luckyMega.roll(context)
            } else {
                ordinaryMega.roll(context)
            }
        }
    }

    /**
     * Builds the standard table for a caller-supplied map talisman.
     *
     * A Ring of Wealth removes the 63-weight main-table nothing entry and the 113-weight mega-table
     * nothing entry. Before Legends' Quest completion, selecting the mega-table slot awards the map
     * talisman instead of entering the mega-rare table.
     */
    public fun standard(
        talisman: ObjType,
        hasRingOfWealth: (NpcDropContext) -> Boolean,
        legendsQuestComplete: (NpcDropContext) -> Boolean,
    ): NpcDropRoll {
        val megaRare = megaRare(hasRingOfWealth)
        val mega = NpcDropRoll { context ->
            (if (legendsQuestComplete(context)) megaRare else item(talisman)).roll(context)
        }
        val commonEntries =
            listOf(
                NpcWeightedDrop(32, item(objs.uncut_sapphire)),
                NpcWeightedDrop(16, item(objs.uncut_emerald)),
                NpcWeightedDrop(8, item(objs.uncut_ruby)),
                NpcWeightedDrop(3, item(talisman)),
                NpcWeightedDrop(2, item(objs.uncut_diamond)),
                NpcWeightedDrop(1, item(NpcGemDropObjs.runeJavelin, 5..5)),
                NpcWeightedDrop(1, item(NpcGemDropObjs.loopHalfOfKey)),
                NpcWeightedDrop(1, item(NpcGemDropObjs.toothHalfOfKey)),
                NpcWeightedDrop(1, mega),
            )
        val ordinary = weighted(listOf(NpcWeightedDrop(63, nothing())) + commonEntries)
        val lucky = weighted(commonEntries)
        return NpcDropRoll { context ->
            (if (hasRingOfWealth(context)) lucky else ordinary).roll(context)
        }
    }
}
