package org.rsmod.content.skills.hunter.tracking

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.map.CoordGrid

public object TrackingLocs : LocReferences() {
    val all =
        listOf(
                "hunting_trail_clue1_0",
                "hunting_trail_clue1_1",
                "hunting_trail_clue1_2",
                "hunting_trail_clue1_3",
                "hunting_trail_clue1_4",
                "hunting_trail_clue1_5",
                "hunting_trail_clue1_6",
                "hunting_trail_clue1_7",
                "hunting_trail_clue1_8",
                "hunting_trail_clue1_9",
                "hunting_trail_clue2_0",
                "hunting_trail_clue2_1",
                "hunting_trail_clue2_2",
                "hunting_trail_clue2_3",
                "hunting_trail_clue2_4",
                "hunting_trail_clue2_5",
                "hunting_trail_clue2_6",
                "hunting_trail_clue2_7",
                "hunting_trail_clue2_8",
                "hunting_trail_clue3_0",
                "hunting_trail_clue3_1",
                "hunting_trail_clue3_2",
                "hunting_trail_clue3_3",
                "hunting_trail_clue3_4",
                "hunting_trail_clue3_5",
                "hunting_trail_clue3_6",
                "hunting_trail_clue3_7",
                "hunting_trail_clue3_8",
                "hunting_trail_clue3_9",
                "hunting_trail_clue4_0",
                "hunting_trail_clue4_1",
                "hunting_trail_clue4_2",
                "hunting_trail_clue4_3",
                "hunting_trail_clue4_4",
                "hunting_trail_clue4_5",
                "hunting_trail_clue4_6",
                "hunting_trail_clue4_7",
                "hunting_trail_clue4_8",
                "hunting_trail_clue5_0",
                "hunting_trail_clue5_1",
                "hunting_trail_clue5_2",
                "hunting_trail_clue5_3",
                "hunting_trail_clue5_4",
                "hunting_trail_clue5_5",
                "hunting_trail_clue5_6",
                "hunting_trail_clue5_7",
                "hunting_trail_clue5_8",
                "hunting_trail_clue5_9",
                "hunting_trail_clue6_0",
                "hunting_trail_clue6_1",
                "hunting_trail_clue6_2",
                "hunting_trail_clue6_3",
                "hunting_trail_clue6_4",
                "hunting_trail_clue6_5",
                "hunting_trail_clue6_6",
                "hunting_trail_clue6_7",
                "hunting_trail_clue6_8",
                "hunting_trail_clue7_0",
                "hunting_trail_clue7_1",
                "hunting_trail_clue7_2",
                "hunting_trail_clue7_3",
                "hunting_trail_clue7_4",
                "hunting_trail_clue7_5",
                "hunting_trail_clue7_6",
                "hunting_trail_clue7_7",
                "hunting_trail_clue7_8",
                "hunting_trail_clue8_0",
                "hunting_trail_clue8_1",
                "hunting_trail_clue8_2",
                "hunting_trail_clue8_3",
                "hunting_trail_clue8_4",
                "hunting_trail_clue8_5",
                "hunting_trail_clue8_6",
                "hunting_trail_clue8_7",
                "hunting_trail_clue8_8",
                "hunting_trail_end_bush29",
                "hunting_trail_end_bush35_55",
                "hunting_trail_end_desert",
                "hunting_trail_end_jungle",
                "hunting_trail_end_polar",
                "hunting_trail_spawn1",
                "hunting_trail_spawn2",
                "hunting_trail_spawn3",
                "hunting_trail_spawn4",
                "hunting_trail_spawn5",
                "hunting_trail_spawn6",
                "hunting_trail_spawn_desert1",
                "hunting_trail_spawn_desert2",
                "hunting_trail_spawn_jungle1",
                "hunting_trail_spawn_jungle2",
                "hunting_trail_spawn_polar1",
                "hunting_trail_spawn_polar2",
            )
            .associateWith(::find)
    val polarUp = find("hunting_polar_steps_upwards")
    val polarDown = find("hunting_polar_steps_down")
}

public object TrackingBits : VarBitReferences() {
    val all =
        listOf(
                "hunting_trail_state1_0",
                "hunting_trail_state1_1",
                "hunting_trail_state1_2",
                "hunting_trail_state1_3",
                "hunting_trail_state1_4",
                "hunting_trail_state1_5",
                "hunting_trail_state1_6",
                "hunting_trail_state1_7",
                "hunting_trail_state1_8",
                "hunting_trail_state1_9",
                "hunting_trail_state2_0",
                "hunting_trail_state2_1",
                "hunting_trail_state2_2",
                "hunting_trail_state2_3",
                "hunting_trail_state2_4",
                "hunting_trail_state2_5",
                "hunting_trail_state2_6",
                "hunting_trail_state2_7",
                "hunting_trail_state2_8",
                "hunting_trail_state3_0",
                "hunting_trail_state3_1",
                "hunting_trail_state3_2",
                "hunting_trail_state3_3",
                "hunting_trail_state3_4",
                "hunting_trail_state3_5",
                "hunting_trail_state3_6",
                "hunting_trail_state3_7",
                "hunting_trail_state3_8",
                "hunting_trail_state3_9",
                "hunting_trail_state4_0",
                "hunting_trail_state4_1",
                "hunting_trail_state4_2",
                "hunting_trail_state4_3",
                "hunting_trail_state4_4",
                "hunting_trail_state4_5",
                "hunting_trail_state4_6",
                "hunting_trail_state4_7",
                "hunting_trail_state4_8",
                "hunting_trail_state5_0",
                "hunting_trail_state5_1",
                "hunting_trail_state5_2",
                "hunting_trail_state5_3",
                "hunting_trail_state5_4",
                "hunting_trail_state5_5",
                "hunting_trail_state5_6",
                "hunting_trail_state5_7",
                "hunting_trail_state5_8",
                "hunting_trail_state5_9",
                "hunting_trail_state6_0",
                "hunting_trail_state6_1",
                "hunting_trail_state6_2",
                "hunting_trail_state6_3",
                "hunting_trail_state6_4",
                "hunting_trail_state6_5",
                "hunting_trail_state6_6",
                "hunting_trail_state6_7",
                "hunting_trail_state6_8",
                "hunting_trail_state7_0",
                "hunting_trail_state7_1",
                "hunting_trail_state7_2",
                "hunting_trail_state7_3",
                "hunting_trail_state7_4",
                "hunting_trail_state7_5",
                "hunting_trail_state7_6",
                "hunting_trail_state7_7",
                "hunting_trail_state7_8",
                "hunting_trail_state8_0",
                "hunting_trail_state8_1",
                "hunting_trail_state8_2",
                "hunting_trail_state8_3",
                "hunting_trail_state8_4",
                "hunting_trail_state8_5",
                "hunting_trail_state8_6",
                "hunting_trail_state8_7",
                "hunting_trail_state8_8",
            )
            .associateWith(::find)
}

public object TrackingObjs : ObjReferences() {
    val wand = find("noose_wand")
    val pursuit = find("ring_of_pursuit")
    val polarFur = find("huntingbeast_polar_fur")
    val woodlandFur = find("huntingbeast_woodland_fur")
    val jungleFur = find("huntingbeast_jungle_fur")
    val desertFur = find("huntingbeast_desert_fur")
    val longSpike = find("huntingbeast_bigspike")
}

public object TrackingSeqs : SeqReferences() {
    val catch = find("hunting_noose_catch")
}

public data class TrackingSite(
    val type: LocType,
    val coords: CoordGrid,
    val shape: Int,
    val angle: Int,
    val node: Int,
)

public data class TrackingEdge(
    val a: Int,
    val b: Int,
    val bit: VarBitType,
    val clues: List<TrackingSite>,
)

public data class TrackingNetwork(
    val name: String,
    val level: Int,
    val xp: Double,
    val reward: ObjType,
    val starts: List<TrackingSite>,
    val ends: List<TrackingSite>,
    val edges: List<TrackingEdge>,
)

/**
 * Native233.4 map objects/footprint varbits. Polar underground connections are explicitly modelled.
 */
public object TrackingNetworks {
    val all: List<TrackingNetwork> by lazy {
        listOf(
            TrackingNetwork(
                "polar",
                1,
                30.0,
                TrackingObjs.polarFur,
                starts =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn_polar1"),
                            CoordGrid(x = 2711, z = 3830, level = 1),
                            11,
                            2,
                            3,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn_polar2"),
                            CoordGrid(x = 2717, z = 3819, level = 1),
                            11,
                            3,
                            1,
                        ),
                    ),
                ends =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_polar"),
                            CoordGrid(x = 2708, z = 3819, level = 1),
                            22,
                            3,
                            0,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_polar"),
                            CoordGrid(x = 2712, z = 3831, level = 1),
                            22,
                            1,
                            3,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_polar"),
                            CoordGrid(x = 2716, z = 3827, level = 1),
                            22,
                            1,
                            2,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_polar"),
                            CoordGrid(x = 2718, z = 3820, level = 1),
                            22,
                            1,
                            1,
                        ),
                    ),
                edges =
                    listOf(
                        TrackingEdge(
                            0,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state8_0"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue8_0"),
                                    CoordGrid(x = 2712, z = 3815, level = 1),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            0,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state8_1"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue8_1"),
                                    CoordGrid(x = 2711, z = 3819, level = 1),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            1,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state8_2"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue8_2"),
                                    CoordGrid(x = 2715, z = 3820, level = 1),
                                    10,
                                    2,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            1,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state8_3"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue8_3"),
                                    CoordGrid(x = 2721, z = 3827, level = 1),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            0,
                            3,
                            TrackingBits.all.getValue("hunting_trail_state8_4"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue8_4"),
                                    CoordGrid(x = 2708, z = 3825, level = 1),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            1,
                            2,
                            TrackingBits.all.getValue("hunting_trail_state8_5"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue8_5"),
                                    CoordGrid(x = 2714, z = 3821, level = 1),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            2,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state8_6"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue8_6"),
                                    CoordGrid(x = 2718, z = 3829, level = 1),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            2,
                            3,
                            TrackingBits.all.getValue("hunting_trail_state8_7"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue8_7"),
                                    CoordGrid(x = 2713, z = 3827, level = 1),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            3,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state8_8"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue8_8"),
                                    CoordGrid(x = 2718, z = 3832, level = 1),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                    ),
            ),
            TrackingNetwork(
                "razor",
                49,
                348.5,
                TrackingObjs.longSpike,
                starts =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn1"),
                            CoordGrid(x = 2357, z = 3624, level = 0),
                            11,
                            1,
                            8,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn2"),
                            CoordGrid(x = 2353, z = 3595, level = 0),
                            11,
                            3,
                            1,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn3"),
                            CoordGrid(x = 2360, z = 3611, level = 0),
                            11,
                            2,
                            5,
                        ),
                    ),
                ends =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush29"),
                            CoordGrid(x = 2349, z = 3604, level = 0),
                            22,
                            2,
                            3,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush29"),
                            CoordGrid(x = 2351, z = 3619, level = 0),
                            22,
                            2,
                            9,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush29"),
                            CoordGrid(x = 2354, z = 3609, level = 0),
                            22,
                            2,
                            6,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush29"),
                            CoordGrid(x = 2355, z = 3601, level = 0),
                            22,
                            2,
                            2,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush29"),
                            CoordGrid(x = 2357, z = 3607, level = 0),
                            22,
                            2,
                            4,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush29"),
                            CoordGrid(x = 2358, z = 3620, level = 0),
                            22,
                            2,
                            10,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush29"),
                            CoordGrid(x = 2360, z = 3602, level = 0),
                            22,
                            0,
                            0,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush29"),
                            CoordGrid(x = 2362, z = 3615, level = 0),
                            22,
                            2,
                            7,
                        ),
                    ),
                edges =
                    listOf(
                        TrackingEdge(
                            0,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state1_0"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_0"),
                                    CoordGrid(x = 2362, z = 3598, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            2,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state1_1"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_1"),
                                    CoordGrid(x = 2355, z = 3598, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            1,
                            3,
                            TrackingBits.all.getValue("hunting_trail_state1_2"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_2"),
                                    CoordGrid(x = 2347, z = 3603, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            0,
                            2,
                            TrackingBits.all.getValue("hunting_trail_state1_3"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_3"),
                                    CoordGrid(x = 2358, z = 3599, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            2,
                            3,
                            TrackingBits.all.getValue("hunting_trail_state1_4"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_4"),
                                    CoordGrid(x = 2352, z = 3603, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            0,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state1_5"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_5"),
                                    CoordGrid(x = 2358, z = 3603, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            5,
                            0,
                            TrackingBits.all.getValue("hunting_trail_state1_6"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_6"),
                                    CoordGrid(x = 2362, z = 3610, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_6"),
                                    CoordGrid(x = 2363, z = 3602, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            5,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state1_7"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_7"),
                                    CoordGrid(x = 2358, z = 3607, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_7"),
                                    CoordGrid(x = 2359, z = 3613, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            4,
                            6,
                            TrackingBits.all.getValue("hunting_trail_state1_8"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_8"),
                                    CoordGrid(x = 2355, z = 3608, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            6,
                            3,
                            TrackingBits.all.getValue("hunting_trail_state1_9"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue1_9"),
                                    CoordGrid(x = 2351, z = 3608, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            7,
                            8,
                            TrackingBits.all.getValue("hunting_trail_state2_0"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_0"),
                                    CoordGrid(x = 2363, z = 3617, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            8,
                            9,
                            TrackingBits.all.getValue("hunting_trail_state2_1"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_1"),
                                    CoordGrid(x = 2349, z = 3620, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            8,
                            10,
                            TrackingBits.all.getValue("hunting_trail_state2_2"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_2"),
                                    CoordGrid(x = 2356, z = 3620, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            11,
                            3,
                            TrackingBits.all.getValue("hunting_trail_state2_3"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_3"),
                                    CoordGrid(x = 2344, z = 3612, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_3"),
                                    CoordGrid(x = 2347, z = 3607, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            6,
                            11,
                            TrackingBits.all.getValue("hunting_trail_state2_4"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_4"),
                                    CoordGrid(x = 2348, z = 3612, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_4"),
                                    CoordGrid(x = 2352, z = 3612, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            9,
                            11,
                            TrackingBits.all.getValue("hunting_trail_state2_5"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_5"),
                                    CoordGrid(x = 2345, z = 3614, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_5"),
                                    CoordGrid(x = 2349, z = 3617, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            10,
                            9,
                            TrackingBits.all.getValue("hunting_trail_state2_6"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_6"),
                                    CoordGrid(x = 2352, z = 3618, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_6"),
                                    CoordGrid(x = 2356, z = 3618, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            7,
                            5,
                            TrackingBits.all.getValue("hunting_trail_state2_7"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_7"),
                                    CoordGrid(x = 2362, z = 3614, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            7,
                            10,
                            TrackingBits.all.getValue("hunting_trail_state2_8"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue2_8"),
                                    CoordGrid(x = 2360, z = 3618, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                    ),
            ),
            TrackingNetwork(
                "common",
                3,
                36.0,
                TrackingObjs.woodlandFur,
                starts =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn4"),
                            CoordGrid(x = 2331, z = 3562, level = 0),
                            11,
                            2,
                            0,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn5"),
                            CoordGrid(x = 2322, z = 3576, level = 0),
                            11,
                            3,
                            8,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn6"),
                            CoordGrid(x = 2341, z = 3577, level = 0),
                            11,
                            1,
                            7,
                        ),
                    ),
                ends =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush35_55"),
                            CoordGrid(x = 2322, z = 3570, level = 0),
                            22,
                            0,
                            5,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush35_55"),
                            CoordGrid(x = 2323, z = 3563, level = 0),
                            22,
                            0,
                            1,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush35_55"),
                            CoordGrid(x = 2327, z = 3573, level = 0),
                            22,
                            0,
                            9,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush35_55"),
                            CoordGrid(x = 2332, z = 3568, level = 0),
                            22,
                            0,
                            2,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush35_55"),
                            CoordGrid(x = 2332, z = 3578, level = 0),
                            22,
                            0,
                            10,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush35_55"),
                            CoordGrid(x = 2336, z = 3571, level = 0),
                            22,
                            0,
                            6,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush35_55"),
                            CoordGrid(x = 2337, z = 3565, level = 0),
                            22,
                            0,
                            3,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_bush35_55"),
                            CoordGrid(x = 2343, z = 3568, level = 0),
                            22,
                            0,
                            4,
                        ),
                    ),
                edges =
                    listOf(
                        TrackingEdge(
                            0,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state3_0"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_0"),
                                    CoordGrid(x = 2323, z = 3562, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_0"),
                                    CoordGrid(x = 2330, z = 3562, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            2,
                            0,
                            TrackingBits.all.getValue("hunting_trail_state3_1"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_1"),
                                    CoordGrid(x = 2333, z = 3565, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            3,
                            0,
                            TrackingBits.all.getValue("hunting_trail_state3_2"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_2"),
                                    CoordGrid(x = 2335, z = 3561, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            4,
                            3,
                            TrackingBits.all.getValue("hunting_trail_state3_3"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_3"),
                                    CoordGrid(x = 2338, z = 3563, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_3"),
                                    CoordGrid(x = 2346, z = 3566, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            1,
                            5,
                            TrackingBits.all.getValue("hunting_trail_state3_4"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_4"),
                                    CoordGrid(x = 2321, z = 3565, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            2,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state3_5"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_5"),
                                    CoordGrid(x = 2325, z = 3565, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_5"),
                                    CoordGrid(x = 2330, z = 3568, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            3,
                            6,
                            TrackingBits.all.getValue("hunting_trail_state3_6"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_6"),
                                    CoordGrid(x = 2335, z = 3564, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_6"),
                                    CoordGrid(x = 2337, z = 3573, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            4,
                            6,
                            TrackingBits.all.getValue("hunting_trail_state3_7"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_7"),
                                    CoordGrid(x = 2338, z = 3570, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_7"),
                                    CoordGrid(x = 2343, z = 3566, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            4,
                            7,
                            TrackingBits.all.getValue("hunting_trail_state3_8"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_8"),
                                    CoordGrid(x = 2341, z = 3579, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_8"),
                                    CoordGrid(x = 2345, z = 3567, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            8,
                            5,
                            TrackingBits.all.getValue("hunting_trail_state3_9"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue3_9"),
                                    CoordGrid(x = 2320, z = 3573, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            9,
                            5,
                            TrackingBits.all.getValue("hunting_trail_state4_0"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_0"),
                                    CoordGrid(x = 2324, z = 3571, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            2,
                            9,
                            TrackingBits.all.getValue("hunting_trail_state4_1"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_1"),
                                    CoordGrid(x = 2331, z = 3572, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            9,
                            8,
                            TrackingBits.all.getValue("hunting_trail_state4_2"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_2"),
                                    CoordGrid(x = 2326, z = 3575, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            10,
                            8,
                            TrackingBits.all.getValue("hunting_trail_state4_3"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_3"),
                                    CoordGrid(x = 2323, z = 3577, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_3"),
                                    CoordGrid(x = 2331, z = 3576, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            6,
                            10,
                            TrackingBits.all.getValue("hunting_trail_state4_4"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_4"),
                                    CoordGrid(x = 2335, z = 3574, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            7,
                            6,
                            TrackingBits.all.getValue("hunting_trail_state4_5"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_5"),
                                    CoordGrid(x = 2340, z = 3574, level = 0),
                                    10,
                                    2,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            7,
                            10,
                            TrackingBits.all.getValue("hunting_trail_state4_6"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_6"),
                                    CoordGrid(x = 2336, z = 3579, level = 0),
                                    10,
                                    2,
                                    -1,
                                )
                            ),
                        ),
                    ),
            ),
            TrackingNetwork(
                "desert",
                13,
                66.0,
                TrackingObjs.desertFur,
                starts =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn_desert2"),
                            CoordGrid(x = 3402, z = 3131, level = 0),
                            11,
                            2,
                            0,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn_desert1"),
                            CoordGrid(x = 3396, z = 3106, level = 0),
                            11,
                            1,
                            6,
                        ),
                    ),
                ends =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_desert"),
                            CoordGrid(x = 3393, z = 3122, level = 0),
                            22,
                            0,
                            1,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_desert"),
                            CoordGrid(x = 3400, z = 3114, level = 0),
                            22,
                            0,
                            7,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_desert"),
                            CoordGrid(x = 3405, z = 3124, level = 0),
                            22,
                            0,
                            2,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_desert"),
                            CoordGrid(x = 3407, z = 3121, level = 0),
                            22,
                            0,
                            5,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_desert"),
                            CoordGrid(x = 3411, z = 3108, level = 0),
                            22,
                            0,
                            9,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_desert"),
                            CoordGrid(x = 3414, z = 3121, level = 0),
                            22,
                            0,
                            3,
                        ),
                    ),
                edges =
                    listOf(
                        TrackingEdge(
                            0,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state4_7"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_7"),
                                    CoordGrid(x = 3393, z = 3129, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            0,
                            2,
                            TrackingBits.all.getValue("hunting_trail_state4_8"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue4_8"),
                                    CoordGrid(x = 3403, z = 3126, level = 0),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            3,
                            0,
                            TrackingBits.all.getValue("hunting_trail_state5_0"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_0"),
                                    CoordGrid(x = 3408, z = 3128, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            4,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state5_1"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_1"),
                                    CoordGrid(x = 3396, z = 3121, level = 0),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            2,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state5_2"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_2"),
                                    CoordGrid(x = 3401, z = 3123, level = 0),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            5,
                            2,
                            TrackingBits.all.getValue("hunting_trail_state5_3"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_3"),
                                    CoordGrid(x = 3405, z = 3122, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            3,
                            5,
                            TrackingBits.all.getValue("hunting_trail_state5_4"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_4"),
                                    CoordGrid(x = 3409, z = 3121, level = 0),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            6,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state5_5"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_5"),
                                    CoordGrid(x = 3392, z = 3112, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            7,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state5_6"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_6"),
                                    CoordGrid(x = 3402, z = 3116, level = 0),
                                    10,
                                    0,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            5,
                            8,
                            TrackingBits.all.getValue("hunting_trail_state5_7"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_7"),
                                    CoordGrid(x = 3406, z = 3118, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            3,
                            9,
                            TrackingBits.all.getValue("hunting_trail_state5_8"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_8"),
                                    CoordGrid(x = 3411, z = 3114, level = 0),
                                    10,
                                    2,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            7,
                            6,
                            TrackingBits.all.getValue("hunting_trail_state5_9"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue5_9"),
                                    CoordGrid(x = 3399, z = 3111, level = 0),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            8,
                            7,
                            TrackingBits.all.getValue("hunting_trail_state6_0"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_0"),
                                    CoordGrid(x = 3403, z = 3110, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            8,
                            9,
                            TrackingBits.all.getValue("hunting_trail_state6_1"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_1"),
                                    CoordGrid(x = 3406, z = 3111, level = 0),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            9,
                            6,
                            TrackingBits.all.getValue("hunting_trail_state6_2"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_2"),
                                    CoordGrid(x = 3402, z = 3101, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                    ),
            ),
            TrackingNetwork(
                "jungle",
                7,
                48.0,
                TrackingObjs.jungleFur,
                starts =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn_jungle2"),
                            CoordGrid(x = 2525, z = 2889, level = 0),
                            11,
                            1,
                            0,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_spawn_jungle1"),
                            CoordGrid(x = 2554, z = 2882, level = 0),
                            11,
                            0,
                            9,
                        ),
                    ),
                ends =
                    listOf(
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_jungle"),
                            CoordGrid(x = 2525, z = 2882, level = 0),
                            22,
                            1,
                            1,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_jungle"),
                            CoordGrid(x = 2531, z = 2890, level = 0),
                            22,
                            0,
                            2,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_jungle"),
                            CoordGrid(x = 2533, z = 2885, level = 0),
                            22,
                            1,
                            3,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_jungle"),
                            CoordGrid(x = 2540, z = 2886, level = 0),
                            22,
                            0,
                            6,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_jungle"),
                            CoordGrid(x = 2542, z = 2881, level = 0),
                            22,
                            0,
                            7,
                        ),
                        TrackingSite(
                            TrackingLocs.all.getValue("hunting_trail_end_jungle"),
                            CoordGrid(x = 2553, z = 2888, level = 0),
                            22,
                            0,
                            8,
                        ),
                    ),
                edges =
                    listOf(
                        TrackingEdge(
                            0,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state6_3"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_3"),
                                    CoordGrid(x = 2522, z = 2881, level = 0),
                                    10,
                                    2,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_3"),
                                    CoordGrid(x = 2524, z = 2891, level = 0),
                                    10,
                                    2,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            0,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state6_4"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_4"),
                                    CoordGrid(x = 2524, z = 2886, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            2,
                            0,
                            TrackingBits.all.getValue("hunting_trail_state6_5"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_5"),
                                    CoordGrid(x = 2527, z = 2890, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            2,
                            3,
                            TrackingBits.all.getValue("hunting_trail_state6_6"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_6"),
                                    CoordGrid(x = 2530, z = 2887, level = 0),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            4,
                            1,
                            TrackingBits.all.getValue("hunting_trail_state6_7"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_7"),
                                    CoordGrid(x = 2526, z = 2882, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_7"),
                                    CoordGrid(x = 2531, z = 2881, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            3,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state6_8"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue6_8"),
                                    CoordGrid(x = 2533, z = 2882, level = 0),
                                    10,
                                    3,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            5,
                            2,
                            TrackingBits.all.getValue("hunting_trail_state7_0"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_0"),
                                    CoordGrid(x = 2533, z = 2889, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_0"),
                                    CoordGrid(x = 2539, z = 2890, level = 0),
                                    10,
                                    3,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            6,
                            3,
                            TrackingBits.all.getValue("hunting_trail_state7_1"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_1"),
                                    CoordGrid(x = 2534, z = 2886, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_1"),
                                    CoordGrid(x = 2540, z = 2884, level = 0),
                                    10,
                                    3,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            7,
                            4,
                            TrackingBits.all.getValue("hunting_trail_state7_2"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_2"),
                                    CoordGrid(x = 2537, z = 2880, level = 0),
                                    10,
                                    2,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            7,
                            6,
                            TrackingBits.all.getValue("hunting_trail_state7_3"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_3"),
                                    CoordGrid(x = 2541, z = 2883, level = 0),
                                    10,
                                    2,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            5,
                            6,
                            TrackingBits.all.getValue("hunting_trail_state7_4"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_4"),
                                    CoordGrid(x = 2540, z = 2887, level = 0),
                                    10,
                                    1,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            8,
                            5,
                            TrackingBits.all.getValue("hunting_trail_state7_5"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_5"),
                                    CoordGrid(x = 2543, z = 2890, level = 0),
                                    10,
                                    1,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_5"),
                                    CoordGrid(x = 2551, z = 2888, level = 0),
                                    10,
                                    2,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            9,
                            7,
                            TrackingBits.all.getValue("hunting_trail_state7_6"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_6"),
                                    CoordGrid(x = 2544, z = 2881, level = 0),
                                    10,
                                    2,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_6"),
                                    CoordGrid(x = 2551, z = 2881, level = 0),
                                    10,
                                    0,
                                    -1,
                                ),
                            ),
                        ),
                        TrackingEdge(
                            9,
                            8,
                            TrackingBits.all.getValue("hunting_trail_state7_7"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_7"),
                                    CoordGrid(x = 2552, z = 2885, level = 0),
                                    10,
                                    2,
                                    -1,
                                )
                            ),
                        ),
                        TrackingEdge(
                            8,
                            9,
                            TrackingBits.all.getValue("hunting_trail_state7_8"),
                            listOf(
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_8"),
                                    CoordGrid(x = 2554, z = 2888, level = 0),
                                    10,
                                    3,
                                    -1,
                                ),
                                TrackingSite(
                                    TrackingLocs.all.getValue("hunting_trail_clue7_8"),
                                    CoordGrid(x = 2555, z = 2881, level = 0),
                                    10,
                                    2,
                                    -1,
                                ),
                            ),
                        ),
                    ),
            ),
        )
    }
}
