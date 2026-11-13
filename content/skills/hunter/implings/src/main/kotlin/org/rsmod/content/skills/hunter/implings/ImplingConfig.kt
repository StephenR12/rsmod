package org.rsmod.content.skills.hunter.implings

import org.rsmod.api.clue.ClueTier
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

public object ImplingNpcs : NpcReferences() {
    val wild = (1..11).map { find("ii_impling_type_$it") }
    val maze = (1..11).map { find("ii_impling_type_${it}_maze") }
    val crystal = listOf("johnny","junior","andy","joey","trouble","hingy","zolty","neil","yanny","matty","stace","ian","jamie","damo","xander","steveo","stewie").map { find("ii_impling_type_12_$it") }
    val elnock = find("ii_elnock")
    val precursors = listOf("ii_common_impling_precursor","ii_uncommon_impling_precursor","ii_rare_impling_precursor_maze","ii_rare_impling_precursor","ii_impling_type_12_precursor").map { find(it) }
}
public object ImplingObjs : ObjReferences() {
    val jars = (1..12).map { find("ii_captured_impling_$it") }
    val empty = find("ii_impling_jar")
    val butterflyJar = find("butterfly_jar")
    val net = find("hunting_butterfly_net")
    val magicNet = find("ii_magic_butterfly_net")
    val generator = find("ii_jar_generator")
    val repellent = find("ii_imp_repellent")
    val grubbyKey = find("hosdun_grubby_key")
    val signet = find("elven_signet")
}
public object ImplingBits : VarBitReferences() {
    val mm2 = find("mm2_progress")
    val freeKit = find("ii_elnock_given_freestuff")
}
public object ImplingSeqs : SeqReferences() { val catch = find("human_butterflynet_swing") }
public data class ImplingSpecies(val name: String,val number: Int,val level: Int,val mazeXp: Double,val wildXp: Double,val low: Int,val high: Int,val clues: List<Pair<ClueTier,Int>>) {
    val jar: ObjType get() = ImplingObjs.jars[number-1]
    val wild: List<NpcType> get() = if(number==12) ImplingNpcs.crystal else listOf(ImplingNpcs.wild[number-1])
    val maze: List<NpcType> get() = if(number==12) emptyList() else listOf(ImplingNpcs.maze[number-1])
}
public object ImplingSpeciesList {
    val all = listOf(
        ImplingSpecies("Baby", 1, 17, 18.0, 20.0, 79, 402, listOf(ClueTier.Beginner to 50,ClueTier.Easy to 100)),
        ImplingSpecies("Young", 2, 22, 20.0, 22.0, 69, 351, listOf(ClueTier.Beginner to 25,ClueTier.Easy to 50)),
        ImplingSpecies("Gourmet", 3, 28, 22.0, 24.0, 61, 325, listOf(ClueTier.Easy to 25)),
        ImplingSpecies("Earth", 4, 36, 25.0, 27.0, 51, 302, listOf(ClueTier.Medium to 100)),
        ImplingSpecies("Essence", 5, 42, 27.0, 29.0, 40, 275, listOf(ClueTier.Medium to 50)),
        ImplingSpecies("Eclectic", 6, 50, 30.0, 32.0, 30, 250, listOf(ClueTier.Medium to 25)),
        ImplingSpecies("Nature", 7, 58, 34.0, 36.0, 20, 200, listOf(ClueTier.Hard to 100)),
        ImplingSpecies("Magpie", 8, 65, 44.0, 216.0, 15, 177, listOf(ClueTier.Hard to 50)),
        ImplingSpecies("Ninja", 9, 74, 50.0, 240.0, 10, 151, listOf(ClueTier.Hard to 25)),
        ImplingSpecies("Crystal", 12, 80, 280.0, 280.0, 8, 141, listOf(ClueTier.Elite to 50)),
        ImplingSpecies("Dragon", 10, 83, 65.0, 300.0, 5, 125, listOf(ClueTier.Elite to 50)),
        ImplingSpecies("Lucky", 11, 89, 80.0, 380.0, 3, 100, emptyList()),
    )
    val byNpc: Map<Int,ImplingSpecies> by lazy { all.flatMap{ s -> (s.wild+s.maze).map{it.id to s} }.toMap() }
}
public data class ImplingDrop(val obj: ObjType?,val minimum: Int,val maximum: Int,val weight: Int,val mm2: Boolean)
/** Exact normal jar tables from the dated per-jar pages, resolved against cache-233.4 symbols. */
public object ImplingLoot : ObjReferences() {
    private val rows = checkNotNull(javaClass.getResourceAsStream("impling-loot.tsv")).bufferedReader().use{it.readLines()}.filter{it.isNotBlank()&&!it.startsWith("#")}.map{it.split('\t')}
    private val items = rows.map{it[1]}.filter{it!="-"}.distinct().associateWith{find(it)}
    val tables = rows.groupBy{it[0]}.mapValues{(_,rs)->rs.map{r->ImplingDrop(items[r[1]],r[2].toInt(),r[3].toInt(),r[4].toInt(),r[5]=="1")}}
    public fun select(name: String,roll: Int,mm2: Boolean): ImplingDrop {
        val table=tables.getValue(name).filter{!it.mm2||mm2};var remaining=roll
        require(roll in 0 until table.sumOf{it.weight})
        for(entry in table){if(remaining<entry.weight)return entry;remaining-=entry.weight}
        error("Impling loot table weight mismatch")
    }
}
