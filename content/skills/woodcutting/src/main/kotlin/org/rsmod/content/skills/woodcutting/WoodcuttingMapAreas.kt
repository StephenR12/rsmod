package org.rsmod.content.skills.woodcutting

import org.rsmod.api.config.refs.areas
import org.rsmod.api.type.builders.map.area.MapAreaBuilder
import org.rsmod.game.area.polygon.VertexCoord
import org.rsmod.map.CoordGrid

/** The dated Wiki map polygon for the above-ground Woodcutting Guild. */
object WoodcuttingMapAreas : MapAreaBuilder() {
    override fun onPackMapTask() {
        area(areas.woodcutting_guild) {
            polygon(levels = 0..3) {
                fun absoluteVertex(x: Int, z: Int) {
                    vertex(VertexCoord.from(CoordGrid(x = x, z = z)))
                }

                absoluteVertex(1565, 3499)
                absoluteVertex(1563, 3497)
                absoluteVertex(1563, 3478)
                absoluteVertex(1564, 3477)
                absoluteVertex(1582, 3477)
                absoluteVertex(1587, 3472)
                absoluteVertex(1595, 3472)
                absoluteVertex(1596, 3473)
                absoluteVertex(1596, 3480)
                absoluteVertex(1601, 3485)
                absoluteVertex(1601, 3497)
                absoluteVertex(1607, 3497)
                absoluteVertex(1607, 3492)
                absoluteVertex(1612, 3487)
                absoluteVertex(1617, 3487)
                absoluteVertex(1623, 3493)
                absoluteVertex(1632, 3493)
                absoluteVertex(1633, 3492)
                absoluteVertex(1633, 3490)
                absoluteVertex(1634, 3489)
                absoluteVertex(1648, 3489)
                absoluteVertex(1656, 3497)
                absoluteVertex(1656, 3502)
                absoluteVertex(1657, 3502)
                absoluteVertex(1658, 3503)
                absoluteVertex(1658, 3507)
                absoluteVertex(1655, 3510)
                absoluteVertex(1655, 3516)
                absoluteVertex(1654, 3517)
                absoluteVertex(1633, 3517)
                absoluteVertex(1631, 3519)
                absoluteVertex(1624, 3519)
                absoluteVertex(1621, 3516)
                absoluteVertex(1612, 3516)
                absoluteVertex(1610, 3514)
                absoluteVertex(1608, 3514)
                absoluteVertex(1607, 3513)
                absoluteVertex(1607, 3511)
                absoluteVertex(1604, 3511)
                absoluteVertex(1604, 3506)
                absoluteVertex(1607, 3506)
                absoluteVertex(1607, 3501)
                absoluteVertex(1601, 3501)
                absoluteVertex(1601, 3503)
                absoluteVertex(1600, 3504)
                absoluteVertex(1582, 3504)
                absoluteVertex(1577, 3499)
            }
        }
    }
}
