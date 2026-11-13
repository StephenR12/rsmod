package org.rsmod.content.skills.agility

import org.rsmod.map.CoordGrid

internal data class SepulchreRoute(val start:CoordGrid, val drop:String, val landing:CoordGrid)

internal enum class SepulchreFloor(
    val requirement:Int,
    val completionXp:Int,
    val center:CoordGrid,
    val routes:List<SepulchreRoute>,
) {
    One(52,500,CoordGrid(2272,5984),listOf(
        route(2252,6018,"1_northpath_drop",2282,5985),
        route(2309,6010,"1_eastpath_stairs",2283,5984),
        route(2293,5949,"1_southpath_stairs",2261,5984),
        route(2234,5960,"1_westpath_drop",2262,5985),
    )),
    Two(62,850,CoordGrid(2528,5984),listOf(
        route(2528,5988,"2_northpath_stairs",2548,6008),
        route(2532,5984,"2_eastpath_stairs",2555,5959),
        route(2528,5980,"2_southpath_stairs",2511,5969),
        route(2524,5984,"2_westpath_drop",2492,5977),
    )),
    Three(72,1600,CoordGrid(2400,5856),listOf(
        route(2404,5856,"3_eastpath_drop",2402,5827),
        route(2396,5856,"3_westpath_drop",2398,5827),
    )),
    Four(82,2875,CoordGrid(2528,5856),listOf(
        route(2528,5860,"4_northpath_drop",2544,5858),
        route(2528,5852,"4_southpath_stairs",2531,5842),
    )),
    Five(92,5725,CoordGrid(2272,5856),listOf(
        route(2272,5884,"5_drop_1",2246,5829),
    ));
    fun contains(c:CoordGrid):Boolean = kotlin.math.abs(c.x-center.x)<=52 && kotlin.math.abs(c.z-center.z)<=45
}

private fun route(x:Int,z:Int,name:String,toX:Int,toZ:Int)=
    SepulchreRoute(CoordGrid(x,z,2),"hallowed_floor_$name",CoordGrid(toX,toZ,1))

/** The same normal coordinates drive public floors and the player's optional independent copy. */
internal class SepulchreSpace(
    val region: org.rsmod.game.region.Region? = null,
    private val dx:Int=0,
    private val dz:Int=0,
){
    fun world(tile:CoordGrid)=tile.translate(dx,dz)
    fun normal(tile:CoordGrid)=tile.translate(-dx,-dz)

    companion object{
        fun privateFloor(floor:SepulchreFloor,regions:org.rsmod.api.repo.region.RegionRepository,clock:Int):SepulchreSpace?{
            val baseX=(floor.center.x-56)/8
            val baseZ=(floor.center.z-48)/8
            val template=org.rsmod.api.repo.region.RegionTemplate.create{
                for(level in 0..2)for(x in 0..13)for(z in 0..12)this[x,z,level]=org.rsmod.map.zone.ZoneKey(baseX+x,baseZ+z,level)
            }
            val region=regions.add(template)?:return null
            region.reclaimFromCycle=clock+5
            return SepulchreSpace(region,region.southWest.x-baseX*8,region.southWest.z-baseZ*8)
        }
    }
}
