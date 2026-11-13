package org.rsmod.content.skills.hunter.implings

import kotlin.math.abs
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType

public data class LuckyImplingItem(val obj:ObjType,val minimum:Int,val maximum:Int)
public data class LuckyImplingOutcome(val probability:Double,val items:List<LuckyImplingItem>)
/** Dated ordinary casket tables, one roll only; hard triple-potion rewards stay one bundled roll. */
public object LuckyImplingLoot:ObjReferences(){
    private val rows=checkNotNull(javaClass.getResourceAsStream("lucky-impling-loot.tsv")).bufferedReader().use{it.readLines()}.filter{it.isNotBlank()&&!it.startsWith("#")}.map{it.split('\t')}
    private val items=rows.map{it[2]}.distinct().associateWith{find(it)}
    public val tiers=listOf("easy","medium","hard","elite","master")
    public val tables=tiers.map{tier->rows.filter{it[0]==tier}.groupBy{it[1]}.values.map{rs->
        val row=rs.first();val probability=row[5].toDouble()/row[6].toDouble()
        LuckyImplingOutcome(probability,rs.map{LuckyImplingItem(items.getValue(it[2]),it[3].toInt(),it[4].toInt())})
    }}.also{tables->for(table in tables)check(abs(table.sumOf{it.probability}-1.0)<1e-12){"Lucky impling table probabilities must sum to one"}}
    public fun select(tier:Int,roll:Double):LuckyImplingOutcome{
        require(tier in tables.indices&&roll>=0.0&&roll<1.0)
        var remaining=roll
        for(outcome in tables[tier]){if(remaining<outcome.probability)return outcome;remaining-=outcome.probability}
        return tables[tier].last() // Floating-point accumulation at the final interval boundary.
    }
}
