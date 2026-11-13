package org.rsmod.api.combat.commons.npc

import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.varp.VarpType
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.map.CoordGrid

public object SlayerLocationBits : VarBitReferences() {
    public val boss: VarBitType = find("slayer_target_bossid")
    public val master: VarBitType = find("slayer_master")
}

public object SlayerLocationVars : VarpReferences() {
    public val area: VarpType = find("slayer_area")
}

public fun CoordGrid.inWildernessGodWars(): Boolean = level == 0 && x in 3008..3071 && z in 10112..10167

public fun CoordGrid.inWildernessSlayerCave(): Boolean = level == 0 && x in 3328..3455 && z in 10048..10175

/** Wilderness Slayer includes its caves and excludes the Enclave, Mage Arena bank and KBD lair. */
public fun CoordGrid.inSlayerWilderness(): Boolean {
    if (x in 2944..3391 && z in 3523..3967) {
        if (x in 2992..3063 && z <= 3527) return false // Black Knights' Fortress, south of the rocks.
        return x !in (feroxRows.getOrNull(z - 3617) ?: IntRange.EMPTY)
    }
    if (inWildernessGodWars() || inWildernessSlayerCave()) return true
    if (level == 3 && x in 3040..3071 && z in 10136..10175) return true
    if (level != 0) return false
    return when {
        x in 3072..3135 && z in 9918..10015 -> true // Wilderness half of Edgeville Dungeon.
        x in 3136..3263 && z in 10048..10239 -> true // Revenant Caves.
        x in 2944..3071 && z in 10240..10367 -> true // Lava Maze and Deep Wilderness dungeons.
        x in 3136..3327 && z in 10304..10367 -> true // Scorpia, resource and agility area dungeons.
        x in 3328..3391 && z in 10240..10367 -> true
        x in 1792..1919 && z in 11520..11647 -> true // Wilderness boss lairs.
        else -> false
    }
}

// Walkable safe tiles inside the Enclave's barriers, indexed by world Z.
private val feroxRows = listOf(
    3131..3138,3128..3141,3128..3141,3128..3141,3127..3139,3127..3146,3127..3145,
    3128..3145,3128..3146,3132..3151,3124..3152,3123..3154,3123..3154,3127..3154,
    3128..3154,3129..3154,3126..3151,3125..3154,3125..3154,3127..3155,3127..3155,
    3126..3155,3126..3155,3147..3155,3150..3151,3150..3152,3150..3152,3150..3152,3152..3152,
)
