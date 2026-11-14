package org.rsmod.content.interfaces.bank

import org.rsmod.map.CoordGrid

public data class BankDestination(val name: String, val coords: CoordGrid, val region: String)

public object BankDestinations {
    public val all: List<BankDestination> =
        requireNotNull(javaClass.getResourceAsStream("bank-heist.tsv")).bufferedReader().useLines {
            lines ->
            lines
                .filter { it.isNotBlank() && !it.startsWith('#') }
                .map { line ->
                    val (name, x, z, plane, region) = line.split('\t')
                    BankDestination(name, CoordGrid(x.toInt(), z.toInt(), plane.toInt()), region)
                }
                .toList()
        }
}
