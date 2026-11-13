package org.rsmod.content.skills.mining

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType

public object QuarryLocs : LocReferences() {
    val granite = find("enakh_granite_rocks")
    val sandstone = find("enakh_sandstone_rocks")
}
public object QuarryObjs : ObjReferences() {
    val granite = listOf(find("enakh_granite_tiny"),find("enakh_granite_small"),find("enakh_granite_medium"))
    val sandstone = listOf(find("enakh_sandstone_tiny"),find("enakh_sandstone_small"),find("enakh_sandstone_medium"),find("enakh_sandstone_large"))
}
public data class QuarryReward(val obj: ObjType, val xp: Double, val low: Int, val high: Int)
/** Independent, largest-first cascade from Granite rocks/Sandstone rocks dated Wiki charts. */
public object QuarryRocks {
    val granite: List<QuarryReward> by lazy { listOf(
        QuarryReward(QuarryObjs.granite[2],75.0,6,64),QuarryReward(QuarryObjs.granite[1],60.0,8,75),QuarryReward(QuarryObjs.granite[0],50.0,16,100)) }
    val sandstone: List<QuarryReward> by lazy { listOf(
        QuarryReward(QuarryObjs.sandstone[3],60.0,4,50),QuarryReward(QuarryObjs.sandstone[2],50.0,8,75),QuarryReward(QuarryObjs.sandstone[1],40.0,16,100),QuarryReward(QuarryObjs.sandstone[0],30.0,25,200)) }
    public fun table(id: Int): List<QuarryReward>? = when(id) { QuarryLocs.granite.id -> granite; QuarryLocs.sandstone.id -> sandstone; else -> null }
    public inline fun roll(table: List<QuarryReward>, succeeds: (Int,Int)->Boolean): QuarryReward? = table.firstOrNull { succeeds(it.low,it.high) }
}
