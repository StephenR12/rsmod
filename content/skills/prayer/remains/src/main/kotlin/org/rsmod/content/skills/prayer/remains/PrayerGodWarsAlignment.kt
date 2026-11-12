package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public enum class PrayerGodWarsGod {
    Saradomin,
    Zamorak,
    Armadyl,
    Bandos,
    Zaros,
}

/** Cache-233 wearable variants represented by the Wiki's Armadyl, Bandos, and Zaros families. */
public object PrayerGodWarsObjs : ObjReferences() {
    val bandos: List<ObjType> =
        listOf(
            find("ancient_goblin_mace"),
            find("bgs"),
            find("bgsg"),
            find("bandos_chestplate"),
            find("bandos_skirt"),
            find("bandos_boots"),
            find("trail_bandos_robe_t"),
            find("trail_bandos_robe_l"),
            find("trail_bandos_scarf"),
            find("trail_bandos_mitre"),
            find("trail_bandos_cloak"),
            find("trail_bandos_staff"),
            find("rune_platebody_bandos"),
            find("rune_platelegs_bandos"),
            find("rune_plateskirt_bandos"),
            find("rune_full_helm_bandos"),
            find("rune_kiteshield_bandos"),
            find("trail_bandos_vambraces"),
            find("trail_bandos_chest"),
            find("trail_bandos_chaps"),
            find("trail_bandos_coif"),
            find("unfinished_bandosbook"),
            find("bandosbook_complete"),
            find("league_3_book_bandos"),
            find("blessed_boots_bandos"),
            find("blessing_bandos"),
            find("guardian_boots"),
            find("echo_boots"),
            find("blessed_dhide_shield_bandos"),
            find("bandos_halo"),
        )

    val armadyl: List<ObjType> =
        listOf(
            find("ikov_pendantofarmardyl"),
            find("acb"),
            find("ags"),
            find("agsg"),
            find("deadman_ags"),
            find("armadyl_helmet"),
            find("armadyl_chestplate"),
            find("armadyl_skirt"),
            find("trail_armadyl_robe_t"),
            find("trail_armadyl_robe_l"),
            find("trail_armadyl_scarf"),
            find("trail_armadyl_mitre"),
            find("trail_armadyl_cloak"),
            find("trail_armadyl_staff"),
            find("rune_platebody_armadyl"),
            find("rune_platelegs_armadyl"),
            find("rune_plateskirt_armadyl"),
            find("rune_full_helm_armadyl"),
            find("rune_kiteshield_armadyl"),
            find("trail_armadyl_vambraces"),
            find("trail_armadyl_chest"),
            find("trail_armadyl_chaps"),
            find("trail_armadyl_coif"),
            find("unfinished_armadylbook"),
            find("armadylbook_complete"),
            find("league_3_book_armadyl"),
            find("blessed_boots_armadyl"),
            find("blessing_armadyl"),
            find("wild_cave_bow_uncharged"),
            find("wild_cave_bow_charged"),
            find("wild_cave_webweaver_uncharged"),
            find("wild_cave_webweaver_charged"),
            find("blessed_dhide_shield_armadyl"),
            find("armadyl_halo"),
        )

    val zaros: List<ObjType> =
        listOf(
            find("staff_of_zaros"),
            find("trail_ancient_robe_t"),
            find("trail_ancient_robe_l"),
            find("trail_ancient_cloak"),
            find("trail_ancient_staff"),
            find("trail_ancient_scarf"),
            find("trail_ancient_mitre"),
            find("rune_platebody_ancient"),
            find("rune_platelegs_ancient"),
            find("rune_plateskirt_ancient"),
            find("rune_full_helm_ancient"),
            find("rune_kiteshield_ancient"),
            find("trail_ancient_vambraces"),
            find("trail_ancient_chest"),
            find("trail_ancient_chaps"),
            find("trail_ancient_coif"),
            find("unfinished_zarosbook"),
            find("zarosbook_complete"),
            find("league_3_book_zaros"),
            find("blessed_boots_ancient"),
            find("robe_darkness_head"),
            find("robe_darkness_top"),
            find("robe_darkness_hands"),
            find("robe_darkness_legs"),
            find("robe_darkness_feet"),
            find("blessing_zaros"),
            find("blessed_dhide_shield_ancient"),
            find("zaros_halo"),
            find("ancient_ceremonial_top"),
            find("ancient_ceremonial_legs"),
            find("ancient_ceremonial_mask"),
            find("ancient_ceremonial_gloves"),
            find("ancient_ceremonial_boots"),
            find("ancient_godsword"),
            find("zaryte_vambraces"),
            find("virtus_mask"),
            find("virtus_top"),
            find("virtus_legs"),
            find("virtus_mask_ornament"),
            find("virtus_top_ornament"),
            find("virtus_legs_ornament"),
            find("zaryte_xbow"),
            find("torva_helm"),
            find("torva_chest"),
            find("torva_legs"),
            find("torva_helm_sanguine"),
            find("torva_chest_sanguine"),
            find("torva_legs_sanguine"),
            find("venator_bow"),
            find("venator_bow_uncharged"),
            find("venator_bow_ornament"),
            find("venator_bow_ornament_uncharged"),
            find("ancient_sceptre"),
            find("ancient_sceptre_trouver"),
            find("ancient_sceptre_blood"),
            find("ancient_sceptre_ice"),
            find("ancient_sceptre_smoke"),
            find("ancient_sceptre_shadow"),
            find("ancient_sceptre_blood_trouver"),
            find("ancient_sceptre_ice_trouver"),
            find("ancient_sceptre_smoke_trouver"),
            find("ancient_sceptre_shadow_trouver"),
        )
}

public class PrayerGodWarsAlignment @Inject constructor(private val objTypes: ObjTypeList) {
    private val bandosIds by lazy { PrayerGodWarsObjs.bandos.mapTo(hashSetOf()) { it.id } }
    private val armadylIds by lazy { PrayerGodWarsObjs.armadyl.mapTo(hashSetOf()) { it.id } }
    private val zarosIds by lazy { PrayerGodWarsObjs.zaros.mapTo(hashSetOf()) { it.id } }

    public fun count(player: Player, god: PrayerGodWarsGod): Int =
        player.worn.filterNotNull { isAffiliated(it, god) }.size

    public fun isAffiliated(obj: InvObj, god: PrayerGodWarsGod): Boolean =
        when (god) {
            PrayerGodWarsGod.Saradomin -> hasCacheFlag(obj, params.saradominitem)
            PrayerGodWarsGod.Zamorak -> hasCacheFlag(obj, params.zamorakitem)
            PrayerGodWarsGod.Armadyl -> obj.id in armadylIds
            PrayerGodWarsGod.Bandos -> obj.id in bandosIds
            PrayerGodWarsGod.Zaros -> obj.id in zarosIds
        }

    private fun hasCacheFlag(
        obj: InvObj,
        param: org.rsmod.game.type.param.ParamType<Int>,
    ): Boolean {
        return objTypes[obj].paramMap?.getOrNull(param) == 1
    }
}
