package org.rsmod.content.skills.runecraft

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.map.CoordGrid

/** Outside coordinates are the anchor of the three-tile-wide ruins in cache 233.4. */
internal enum class RuneTemple(
    val recipe: StandardRuneRecipe,
    val outside: CoordGrid,
    val inside: CoordGrid,
    val tiaraXp: Double,
    val elemental: Boolean = false,
) {
    Air(StandardRuneRecipes.air, CoordGrid(2984, 3291), CoordGrid(2841, 4829), 25.0, true),
    Mind(StandardRuneRecipes.mind, CoordGrid(2981, 3513), CoordGrid(2793, 4828), 27.5),
    Water(StandardRuneRecipes.water, CoordGrid(3184, 3164), CoordGrid(2726, 4832), 30.0, true),
    Earth(StandardRuneRecipes.earth, CoordGrid(3305, 3473), CoordGrid(2655, 4830), 32.5, true),
    Fire(StandardRuneRecipes.fire, CoordGrid(3312, 3254), CoordGrid(2575, 4850), 35.0, true),
    Body(StandardRuneRecipes.body, CoordGrid(3052, 3444), CoordGrid(2522, 4833), 37.5),
    Cosmic(StandardRuneRecipes.cosmic, CoordGrid(2407, 4376), CoordGrid(2162, 4833), 40.0),
    Chaos(StandardRuneRecipes.chaos, CoordGrid(3059, 3590), CoordGrid(2275, 4847, 3), 42.5),
    Nature(StandardRuneRecipes.nature, CoordGrid(2868, 3018), CoordGrid(2400, 4835), 45.0),
    Law(StandardRuneRecipes.law, CoordGrid(2857, 3380), CoordGrid(2465, 4817), 47.5),
    Death(StandardRuneRecipes.death, CoordGrid(1859, 4638), CoordGrid(2208, 4830), 50.0),
    Blood(StandardRuneRecipes.blood, CoordGrid(3560, 9780), CoordGrid(3239, 4832), 52.5),
    Wrath(StandardRuneRecipes.wrath, CoordGrid(2445, 2824), CoordGrid(2335, 4826, 1), 52.5);

    val key
        get() = name.lowercase()
}

internal object TempleLocs : LocReferences() {
    val ruins = RuneTemple.entries.associateWith { find("${it.key}temple_ruined") }
    val exits = RuneTemple.entries.associateWith { find("${it.key}temple_exit_portal") }
}

internal object TempleObjects : ObjReferences() {
    val talismans = RuneTemple.entries.associateWith { find("${it.key}_talisman") }
    val tiaras = RuneTemple.entries.associateWith { find("tiara_${it.key}") }
    val elementalTalisman = find("elemental_talisman")
    val elementalTiara = find("tiara_elemental")
    val catalyticTalisman = find("catalytic_talisman")
    val catalyticTiara = find("tiara_catalytic")
    val goldTiara = find("tiara_gold")
}

internal object TempleVars : VarBitReferences() {
    val enter = RuneTemple.entries.associateWith { find("rc_no_tally_required_${it.key}") }
    val playingGuardiansOfTheRift = find("gotr_is_playing")
    val attuned = find("rc_outfit_tiara_attuned")
}
