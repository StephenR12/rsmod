package org.rsmod.content.skills.hunter.herbiboar

import org.rsmod.map.CoordGrid

/**
 * Endpoints are the 11 search areas (positive) or nine tunnels (negative). Directional clues are
 * the actual inspectable locs at the start of that leg in map squares 57–58/59–60.
 */
internal data class BoarTrack(val a: Int, val b: Int, val clueA: Int, val clueB: Int) {
    fun leaves(node: Int): Boolean = (a == node && clueA != 0) || (b == node && clueB != 0)
}

internal object HerbiboarMap {
    val tracks =
        listOf(
            BoarTrack(-8, 3, 0, 30545),
            BoarTrack(-8, 1, 0, 30546),
            BoarTrack(-8, 6, 0, 30544),
            BoarTrack(3, 4, 30533, 30551),
            BoarTrack(3, 2, 30533, 30537),
            BoarTrack(1, 2, 30534, 30537),
            BoarTrack(1, 5, 30534, 30549),
            BoarTrack(6, 9, 30535, 30547),
            BoarTrack(6, 7, 30535, 30543),
            BoarTrack(4, 8, 30536, 30550),
            BoarTrack(4, 7, 30536, 30541),
            BoarTrack(2, -5, 30537, 0),
            BoarTrack(2, 4, 30537, 30551),
            BoarTrack(5, 6, 30539, 30535),
            BoarTrack(5, 9, 30539, 30547),
            BoarTrack(9, 11, 30540, 31479),
            BoarTrack(9, -1, 30540, 0),
            BoarTrack(7, -3, 30541, 0),
            BoarTrack(8, 10, 30542, 31480),
            BoarTrack(8, -7, 30542, 0),
            BoarTrack(8, 7, 30550, 0),
            BoarTrack(11, -1, 31479, 0),
            BoarTrack(11, 10, 31479, 0),
            BoarTrack(10, 4, 31480, 0),
        )
    val tunnels =
        listOf(
            CoordGrid(3693, 3798),
            CoordGrid(3702, 3808),
            CoordGrid(3703, 3826),
            CoordGrid(3710, 3881),
            CoordGrid(3700, 3877),
            CoordGrid(3715, 3840),
            CoordGrid(3751, 3849),
            CoordGrid(3685, 3869),
            CoordGrid(3681, 3863),
        )
    val boars =
        listOf(
            CoordGrid(3696, 3796),
            CoordGrid(3704, 3808),
            CoordGrid(3705, 3827),
            CoordGrid(3710, 3883),
            CoordGrid(3697, 3876),
            CoordGrid(3717, 3840),
            CoordGrid(3753, 3851),
            CoordGrid(3683, 3870),
            CoordGrid(3681, 3865),
        )
    val startTiles =
        listOf(
            CoordGrid(3686, 3870),
            CoordGrid(3695, 3800),
            CoordGrid(3704, 3810),
            CoordGrid(3705, 3830),
            CoordGrid(3751, 3850),
        )
    val clues =
        mapOf(
            30549 to CoordGrid(3667, 3862),
            30539 to CoordGrid(3668, 3865),
            30534 to CoordGrid(3670, 3889),
            30546 to CoordGrid(3672, 3890),
            30547 to CoordGrid(3680, 3836),
            30540 to CoordGrid(3680, 3838),
            30544 to CoordGrid(3681, 3859),
            30535 to CoordGrid(3681, 3860),
            30541 to CoordGrid(3694, 3847),
            30545 to CoordGrid(3697, 3875),
            30543 to CoordGrid(3698, 3847),
            30533 to CoordGrid(3699, 3875),
            31479 to CoordGrid(3706, 3811),
            30536 to CoordGrid(3708, 3876),
            30551 to CoordGrid(3710, 3877),
            31480 to CoordGrid(3713, 3840),
            30550 to CoordGrid(3713, 3850),
            30542 to CoordGrid(3715, 3851),
            30537 to CoordGrid(3728, 3893),
        )
    // Each signed value is a one-based track number; negative traverses it in reverse.
    val starts =
        listOf(listOf(1, 2, 3), listOf(-17, -22), listOf(-16, 23), listOf(-18), listOf(-20))
    val nearbyTunnel = mapOf(3 to 5, 4 to 4, 6 to 9, 10 to 6, 11 to 2)
}
