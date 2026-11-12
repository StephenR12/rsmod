package org.rsmod.content.skills.crafting.birdhouses

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.type.obj.ObjType

internal typealias birdhouse_objs = BirdhouseObjs

internal typealias birdhouse_seqs = BirdhouseSeqs

public object BirdhouseObjs : ObjReferences() {
    public val clockwork = find("poh_clockwork_mechanism")
    public val normal = find("birdhouse_normal")
    public val oak = find("birdhouse_oak")
    public val willow = find("birdhouse_willow")
    public val teak = find("birdhouse_teak")
    public val maple = find("birdhouse_maple")
    public val mahogany = find("birdhouse_mahogany")
    public val yew = find("birdhouse_yew")
    public val magic = find("birdhouse_magic")
    public val redwood = find("birdhouse_redwood")
}

public object BirdhouseSeqs : SeqReferences() {
    public val make = find("birdhouse_make")
    public val makeWithImcandoHammer = find("birdhouse_make_imcando_hammer")
}

public data class BirdhouseRecipe(
    public val name: String,
    public val logs: ObjType,
    public val output: ObjType,
    public val level: Int,
    public val xp: Double,
)

public object BirdhouseRecipes {
    public val entries: List<BirdhouseRecipe> =
        listOf(
            BirdhouseRecipe("bird house", objs.logs, birdhouse_objs.normal, 5, 15.0),
            BirdhouseRecipe("oak bird house", objs.oak_logs, birdhouse_objs.oak, 15, 20.0),
            BirdhouseRecipe("willow bird house", objs.willow_logs, birdhouse_objs.willow, 25, 25.0),
            BirdhouseRecipe("teak bird house", objs.teak_logs, birdhouse_objs.teak, 35, 30.0),
            BirdhouseRecipe("maple bird house", objs.maple_logs, birdhouse_objs.maple, 45, 35.0),
            BirdhouseRecipe(
                "mahogany bird house",
                objs.mahogany_logs,
                birdhouse_objs.mahogany,
                50,
                40.0,
            ),
            BirdhouseRecipe("yew bird house", objs.yew_logs, birdhouse_objs.yew, 60, 45.0),
            BirdhouseRecipe("magic bird house", objs.magic_logs, birdhouse_objs.magic, 75, 50.0),
            BirdhouseRecipe(
                "redwood bird house",
                objs.redwood_logs,
                birdhouse_objs.redwood,
                90,
                55.0,
            ),
        )

    public fun forOutput(output: ObjType): BirdhouseRecipe? =
        entries.firstOrNull { it.output == output }
}
