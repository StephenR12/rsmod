package org.rsmod.content.skills.smithing.smelting

import org.rsmod.api.type.refs.loc.LocReferences

public typealias furnace_locs = FurnaceLocs

/** Cache-233 locs whose decoded options expose an ordinary `Smelt` interaction. */
public object FurnaceLocs : LocReferences() {
    val general = find("furnace")
    val underground_pass = find("furnace_upass")
    val rellekka = find("viking_furnace")
    val keldagrim = find("dwarf_keldagrim_furnace")
    val enakhra = find("enakh_new_furnace_lit")
    val mor_ul_rek = find("tzhaar_forge")
    val zanaris = find("fairy_furnace")
    val piscatoris = find("swan_furnace")
    val burgh_de_rott = find("burgh_furnace_fired")
    val edgeville = find("varrock_diary_furnace")
    val port_phasmatys = find("ahoy_new_furnace")
    val neitiznot = find("iznot_clay_forge")
    val falador = find("fai_falador_furnace")
    val wilderness_resource_area = find("wilderness_resource_furnace")
    val shilo_village = find("zqfurnace_lit")
    val forsaken_tower = find("lovaquest_tower_furnace_lit")
    val mount_karuulm = find("brimstone_furnace")
    val prifddinas = find("prif_furnace")
    val darkmeyer = find("darkm_furnace")
    val ruins_of_ullek = find("bcs_furnace_lit")
    val lassar_shadow = find("furnace_lassar01_default01_shadow")
    val lassar = find("furnace_lassar01_default01")
    val varlamore = find("furnace3")
    val cam_torum_forge = find("cam_torum_sacred_forge")
    val cam_torum = find("cam_torum_furnace")
    val deepfin_point = find("cof_furnace")
    val lovakite_large = find("lovakengj_furnace_large_01")
    val lovakite = find("lovakengj_furnace_01")

    val standard =
        listOf(
            general,
            underground_pass,
            rellekka,
            keldagrim,
            enakhra,
            mor_ul_rek,
            zanaris,
            piscatoris,
            burgh_de_rott,
            edgeville,
            port_phasmatys,
            neitiznot,
            falador,
            wilderness_resource_area,
            forsaken_tower,
            mount_karuulm,
            prifddinas,
            darkmeyer,
            ruins_of_ullek,
            lassar_shadow,
            lassar,
            varlamore,
            cam_torum_forge,
            cam_torum,
            deepfin_point,
        )

    val lovakiteFurnaces = listOf(lovakite_large, lovakite)
    val all = standard + shilo_village + lovakiteFurnaces
}
