package org.rsmod.content.skills.hunter.birdhouses

import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.synth.SynthReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varp.VarpType
import org.rsmod.map.CoordGrid

public object BirdhouseTrapLocs : LocReferences() {
    public val notBuilt = find("birdhouse_not_built")
    public val siteA = find("birdhouse_1")
    public val siteB = find("birdhouse_2")
    public val siteC = find("birdhouse_3")
    public val siteD = find("birdhouse_4")

    public val normalBuilt = find("birdhouse_normal_built")
    public val normalSeeded = find("birdhouse_normal_full")
    public val normalActive = find("birdhouse_normal_bird")
    public val oakBuilt = find("birdhouse_oak_built")
    public val oakSeeded = find("birdhouse_oak_full")
    public val oakActive = find("birdhouse_oak_bird")
    public val willowBuilt = find("birdhouse_willow_built")
    public val willowSeeded = find("birdhouse_willow_full")
    public val willowActive = find("birdhouse_willow_bird")
    public val teakBuilt = find("birdhouse_teak_built")
    public val teakSeeded = find("birdhouse_teak_full")
    public val teakActive = find("birdhouse_teak_bird")
    public val mapleBuilt = find("birdhouse_maple_built")
    public val mapleSeeded = find("birdhouse_maple_full")
    public val mapleActive = find("birdhouse_maple_bird")
    public val mahoganyBuilt = find("birdhouse_mahogany_built")
    public val mahoganySeeded = find("birdhouse_mahogany_full")
    public val mahoganyActive = find("birdhouse_mahogany_bird")
    public val yewBuilt = find("birdhouse_yew_built")
    public val yewSeeded = find("birdhouse_yew_full")
    public val yewActive = find("birdhouse_yew_bird")
    public val magicBuilt = find("birdhouse_magic_built")
    public val magicSeeded = find("birdhouse_magic_full")
    public val magicActive = find("birdhouse_magic_bird")
    public val redwoodBuilt = find("birdhouse_redwood_built")
    public val redwoodSeeded = find("birdhouse_redwood_full")
    public val redwoodActive = find("birdhouse_redwood_bird")
}

public object BirdhouseTrapObjs : ObjReferences() {
    public val normal = find("birdhouse_normal")
    public val oak = find("birdhouse_oak")
    public val willow = find("birdhouse_willow")
    public val teak = find("birdhouse_teak")
    public val maple = find("birdhouse_maple")
    public val mahogany = find("birdhouse_mahogany")
    public val yew = find("birdhouse_yew")
    public val magic = find("birdhouse_magic")
    public val redwood = find("birdhouse_redwood")

    public val clockwork = find("poh_clockwork_mechanism")
    public val feather = find("feather")
    public val rawBirdMeat = find("spit_raw_bird_meat")
    public val seedNest = find("bird_nest_seeds_jan2019")

    public val marigoldSeed = find("marigold_seed")
    public val rosemarySeed = find("rosemary_seed")
    public val nasturtiumSeed = find("nasturtium_seed")
    public val woadSeed = find("woad_seed")
    public val limpwurtSeed = find("limpwurt_seed")
    public val redberrySeed = find("redberry_bush_seed")
    public val cadavaberrySeed = find("cadavaberry_bush_seed")
    public val dwellberrySeed = find("dwellberry_bush_seed")
    public val jangerberrySeed = find("jangerberry_bush_seed")
    public val whiteberrySeed = find("whiteberry_bush_seed")
    public val poisonIvySeed = find("poisonivy_bush_seed")
    public val guamSeed = find("guam_seed")
    public val marrentillSeed = find("marrentill_seed")
    public val tarrominSeed = find("tarromin_seed")
    public val harralanderSeed = find("harralander_seed")
    public val ranarrSeed = find("ranarr_seed")
    public val toadflaxSeed = find("toadflax_seed")
    public val iritSeed = find("irit_seed")
    public val avantoeSeed = find("avantoe_seed")
    public val kwuarmSeed = find("kwuarm_seed")
    public val snapdragonSeed = find("snapdragon_seed")
    public val cadantineSeed = find("cadantine_seed")
    public val lantadymeSeed = find("lantadyme_seed")
    public val dwarfWeedSeed = find("dwarf_weed_seed")
    public val torstolSeed = find("torstol_seed")
    public val barleySeed = find("barley_seed")
    public val juteSeed = find("jute_seed")
    public val hammerstoneSeed = find("hammerstone_hop_seed")
    public val asgarnianSeed = find("asgarnian_hop_seed")
    public val yanillianSeed = find("yanillian_hop_seed")
    public val krandorianSeed = find("krandorian_hop_seed")
    public val wildbloodSeed = find("wildblood_hop_seed")
    public val potatoSeed = find("potato_seed")
    public val onionSeed = find("onion_seed")
    public val sweetcornSeed = find("sweetcorn_seed")
    public val watermelonSeed = find("watermelon_seed")
    public val tomatoSeed = find("tomato_seed")
    public val strawberrySeed = find("strawberry_seed")
    public val cabbageSeed = find("cabbage_seed")
    public val whiteLilySeed = find("white_lily_seed")
    public val snapeGrassSeed = find("snape_grass_seed")
}

public object BirdhouseTrapVarps : VarpReferences() {
    public val displayA = find("birdhouse_transmit_a")
    public val displayB = find("birdhouse_transmit_b")
    public val displayC = find("birdhouse_transmit_c")
    public val displayD = find("birdhouse_transmit_d")
    public val readyAtA = find("birdhouse_ready_at_a")
    public val readyAtB = find("birdhouse_ready_at_b")
    public val readyAtC = find("birdhouse_ready_at_c")
    public val readyAtD = find("birdhouse_ready_at_d")
}

internal object BirdhouseTrapVarpBuilder : VarpBuilder() {
    init {
        build("birdhouse_ready_at_a") { transmitNever = true }
        build("birdhouse_ready_at_b") { transmitNever = true }
        build("birdhouse_ready_at_c") { transmitNever = true }
        build("birdhouse_ready_at_d") { transmitNever = true }
    }
}

public object BirdhouseTrapSynths : SynthReferences() {
    public val buildFill = find("birdhouse_build_fill")
}

public data class BirdhouseTier(
    val obj: ObjType,
    val hunterLevel: Int,
    val hunterXp: Double,
    val builtState: Int,
    val builtLoc: LocType,
    val seededLoc: LocType,
    val activeLoc: LocType,
)

public object BirdhouseTiers {
    public val all: List<BirdhouseTier> =
        listOf(
            BirdhouseTier(
                BirdhouseTrapObjs.normal,
                5,
                280.0,
                1,
                BirdhouseTrapLocs.normalBuilt,
                BirdhouseTrapLocs.normalSeeded,
                BirdhouseTrapLocs.normalActive,
            ),
            BirdhouseTier(
                BirdhouseTrapObjs.oak,
                14,
                420.0,
                4,
                BirdhouseTrapLocs.oakBuilt,
                BirdhouseTrapLocs.oakSeeded,
                BirdhouseTrapLocs.oakActive,
            ),
            BirdhouseTier(
                BirdhouseTrapObjs.willow,
                24,
                560.0,
                7,
                BirdhouseTrapLocs.willowBuilt,
                BirdhouseTrapLocs.willowSeeded,
                BirdhouseTrapLocs.willowActive,
            ),
            BirdhouseTier(
                BirdhouseTrapObjs.teak,
                34,
                700.0,
                10,
                BirdhouseTrapLocs.teakBuilt,
                BirdhouseTrapLocs.teakSeeded,
                BirdhouseTrapLocs.teakActive,
            ),
            BirdhouseTier(
                BirdhouseTrapObjs.maple,
                44,
                820.0,
                13,
                BirdhouseTrapLocs.mapleBuilt,
                BirdhouseTrapLocs.mapleSeeded,
                BirdhouseTrapLocs.mapleActive,
            ),
            BirdhouseTier(
                BirdhouseTrapObjs.mahogany,
                49,
                960.0,
                16,
                BirdhouseTrapLocs.mahoganyBuilt,
                BirdhouseTrapLocs.mahoganySeeded,
                BirdhouseTrapLocs.mahoganyActive,
            ),
            BirdhouseTier(
                BirdhouseTrapObjs.yew,
                59,
                1020.0,
                19,
                BirdhouseTrapLocs.yewBuilt,
                BirdhouseTrapLocs.yewSeeded,
                BirdhouseTrapLocs.yewActive,
            ),
            BirdhouseTier(
                BirdhouseTrapObjs.magic,
                74,
                1140.0,
                22,
                BirdhouseTrapLocs.magicBuilt,
                BirdhouseTrapLocs.magicSeeded,
                BirdhouseTrapLocs.magicActive,
            ),
            BirdhouseTier(
                BirdhouseTrapObjs.redwood,
                89,
                1200.0,
                25,
                BirdhouseTrapLocs.redwoodBuilt,
                BirdhouseTrapLocs.redwoodSeeded,
                BirdhouseTrapLocs.redwoodActive,
            ),
        )

    public val byObjId: Map<Int, BirdhouseTier> = all.associateBy { it.obj.id }
    public val byStageLocId: Map<Int, BirdhouseTier> =
        all.flatMap { tier ->
                listOf(
                    tier.builtLoc.id to tier,
                    tier.seededLoc.id to tier,
                    tier.activeLoc.id to tier,
                )
            }
            .toMap()
}

public data class BirdhouseSeed(val obj: ObjType, val units: Int)

public object BirdhouseSeeds {
    private val high =
        listOf(
            BirdhouseTrapObjs.wildbloodSeed,
            BirdhouseTrapObjs.ranarrSeed,
            BirdhouseTrapObjs.toadflaxSeed,
            BirdhouseTrapObjs.iritSeed,
            BirdhouseTrapObjs.avantoeSeed,
            BirdhouseTrapObjs.kwuarmSeed,
            BirdhouseTrapObjs.snapdragonSeed,
            BirdhouseTrapObjs.cadantineSeed,
            BirdhouseTrapObjs.lantadymeSeed,
            BirdhouseTrapObjs.dwarfWeedSeed,
            BirdhouseTrapObjs.torstolSeed,
        )
    private val low =
        listOf(
            BirdhouseTrapObjs.barleySeed,
            BirdhouseTrapObjs.juteSeed,
            BirdhouseTrapObjs.hammerstoneSeed,
            BirdhouseTrapObjs.asgarnianSeed,
            BirdhouseTrapObjs.yanillianSeed,
            BirdhouseTrapObjs.krandorianSeed,
            BirdhouseTrapObjs.guamSeed,
            BirdhouseTrapObjs.marrentillSeed,
            BirdhouseTrapObjs.tarrominSeed,
            BirdhouseTrapObjs.harralanderSeed,
            BirdhouseTrapObjs.marigoldSeed,
            BirdhouseTrapObjs.rosemarySeed,
            BirdhouseTrapObjs.nasturtiumSeed,
            BirdhouseTrapObjs.woadSeed,
            BirdhouseTrapObjs.limpwurtSeed,
            BirdhouseTrapObjs.whiteLilySeed,
            BirdhouseTrapObjs.potatoSeed,
            BirdhouseTrapObjs.onionSeed,
            BirdhouseTrapObjs.cabbageSeed,
            BirdhouseTrapObjs.tomatoSeed,
            BirdhouseTrapObjs.sweetcornSeed,
            BirdhouseTrapObjs.strawberrySeed,
            BirdhouseTrapObjs.watermelonSeed,
            BirdhouseTrapObjs.snapeGrassSeed,
            BirdhouseTrapObjs.redberrySeed,
            BirdhouseTrapObjs.cadavaberrySeed,
            BirdhouseTrapObjs.dwellberrySeed,
            BirdhouseTrapObjs.jangerberrySeed,
            BirdhouseTrapObjs.whiteberrySeed,
            BirdhouseTrapObjs.poisonIvySeed,
        )

    public val all: List<BirdhouseSeed> =
        low.map { BirdhouseSeed(it, 1) } + high.map { BirdhouseSeed(it, 2) }
    public val byObjId: Map<Int, BirdhouseSeed> = all.associateBy { it.obj.id }
}

public data class BirdhouseSite(
    val baseLoc: LocType,
    val coords: CoordGrid,
    val displayVarp: VarpType,
    val readyAtVarp: VarpType,
)

public object BirdhouseSites {
    public val all: List<BirdhouseSite> =
        listOf(
            BirdhouseSite(
                BirdhouseTrapLocs.siteA,
                CoordGrid(3677, 3882),
                BirdhouseTrapVarps.displayA,
                BirdhouseTrapVarps.readyAtA,
            ),
            BirdhouseSite(
                BirdhouseTrapLocs.siteB,
                CoordGrid(3679, 3815),
                BirdhouseTrapVarps.displayB,
                BirdhouseTrapVarps.readyAtB,
            ),
            BirdhouseSite(
                BirdhouseTrapLocs.siteC,
                CoordGrid(3768, 3761),
                BirdhouseTrapVarps.displayC,
                BirdhouseTrapVarps.readyAtC,
            ),
            BirdhouseSite(
                BirdhouseTrapLocs.siteD,
                CoordGrid(3763, 3755),
                BirdhouseTrapVarps.displayD,
                BirdhouseTrapVarps.readyAtD,
            ),
        )

    public val byCoords: Map<CoordGrid, BirdhouseSite> = all.associateBy { it.coords }
}
