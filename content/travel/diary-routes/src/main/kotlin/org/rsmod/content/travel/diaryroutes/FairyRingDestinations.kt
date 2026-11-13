package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid

internal object FairyRingDestinations {
    val tiles =
        mapOf(
            "AIQ" to CoordGrid(2996, 3114),
            "AIR" to CoordGrid(2700, 3247),
            "AIS" to CoordGrid(1429, 3324),
            "AJP" to CoordGrid(1651, 3010),
            "AJQ" to CoordGrid(2735, 5221),
            "AJR" to CoordGrid(2780, 3613),
            "AJS" to CoordGrid(2500, 3896),
            "AKP" to CoordGrid(3284, 2706),
            "AKQ" to CoordGrid(2319, 3619),
            "AKR" to CoordGrid(1826, 3540),
            "AKS" to CoordGrid(2571, 2956),
            "ALP" to CoordGrid(2503, 3636),
            "ALQ" to CoordGrid(3597, 3495),
            "ALR" to CoordGrid(3059, 4875),
            "ALS" to CoordGrid(2644, 3495),
            "BIP" to CoordGrid(3410, 3324),
            "BIQ" to CoordGrid(3251, 3095),
            "BIS" to CoordGrid(2635, 3266),
            "BJP" to CoordGrid(2267, 2976),
            "BJR" to CoordGrid(2650, 4730),
            "BJS" to CoordGrid(2150, 3070),
            "BKP" to CoordGrid(2385, 3035),
            "BKQ" to CoordGrid(3041, 4532),
            "BKR" to CoordGrid(3469, 3431),
            "BKS" to CoordGrid(2412, 4434),
            "BLP" to CoordGrid(2437, 5126),
            "BLQ" to CoordGrid(3573, 4371),
            "BLR" to CoordGrid(2740, 3351),
            "BLS" to CoordGrid(1295, 3493),
            "CIP" to CoordGrid(2513, 3884),
            "CIQ" to CoordGrid(2528, 3127),
            "CIR" to CoordGrid(1302, 3762),
            "CIS" to CoordGrid(1639, 3868),
            "CJR" to CoordGrid(2705, 3576),
            "CKP" to CoordGrid(2075, 4848, 1),
            "CKQ" to CoordGrid(1359, 2941),
            "CKR" to CoordGrid(2801, 3003),
            "CKS" to CoordGrid(3447, 3470),
            "CLP" to CoordGrid(3082, 3206),
            "CLR" to CoordGrid(2740, 2738),
            "CLS" to CoordGrid(2682, 3081),
            "DIP" to CoordGrid(3037, 4763),
            "DIR" to CoordGrid(3038, 5348),
            "DIS" to CoordGrid(3108, 3149),
            "DJP" to CoordGrid(2658, 3230),
            "DJR" to CoordGrid(1455, 3658),
            "DKP" to CoordGrid(2900, 3111),
            "DKR" to CoordGrid(3129, 3496),
            "DKS" to CoordGrid(2744, 3719),
            "DLQ" to CoordGrid(3423, 3016),
            "DLR" to CoordGrid(2213, 3099),
            "DLS" to CoordGrid(3447, 9824),
        )
    val dials = listOf("ADCB", "ILKJ", "PSRQ")
    val codes = dials[0].flatMap { a -> dials[1].flatMap { b -> dials[2].map { c -> "$a$b$c" } } }
}

internal object FairyRingLocs : LocReferences() {
    val ordinary =
        FairyRingDestinations.codes.map { find("fairyring_minorhub_last_${it.lowercase()}") }
    val house = FairyRingDestinations.codes.map { find("poh_fairy_ring_last_${it.lowercase()}") }
    val combined =
        FairyRingDestinations.codes.map { find("poh_spirit_ring_last_${it.lowercase()}") }
    val zanaris =
        FairyRingDestinations.codes.map { find("fairyring_homehub_last_${it.lowercase()}") }
}

internal object FairyRingBits : VarBitReferences() {
    val permission = find("fairyring_permission")
    val quest = find("fairy2_queencure_quest")
    val dials = (1..3).map { find("fairyring_$it") }
    val last = find("fairyring_lastloc")
    val visited = FairyRingDestinations.tiles.keys + "DIQ"
    val logs = visited.associateWith { find("fairyrings_log_${it.lowercase()}") }
    val zeah = find("zeah_playerhasvisited")
    val varlamore = find("varlamore_visited")
    val dorg = find("dttd_main")
    val necropolis = find("bcs")
    val goblins = find("lotg")
}

internal object FairyRingVars : VarpReferences() {
    val trials = find("viking")
    val grail = find("grail")
    val regicide = find("regicide_quest")
    val monkeys = find("mm_main")
    val myreque = find("routequest")
}

internal object FairyRingItems : ObjReferences() {
    val staffs = listOf(find("dramen_staff"), find("lunar_moonclan_liminal_staff"))
}

internal object FairyRingInterfaces : InterfaceReferences() {
    val main = find("fairyrings")
    val log = find("fairyrings_log")
}

internal object FairyRingComponents : ComponentReferences() {
    val clockwise = (1..3).map { find("fairyrings:${it}_clockwise") }
    val anticlockwise = (1..3).map { find("fairyrings:${it}_anticlockwise") }
    val confirm = find("fairyrings:confirm")
    val log = FairyRingBits.visited.associateWith { find("fairyrings_log:${it.lowercase()}") }
}
