package org.rsmod.api.player.worn

import org.rsmod.api.config.refs.objs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType

public object EquipmentChecks {
    public fun isToxicBlowpipe(obj: InvObj?): Boolean =
        obj.isAnyType(objs.toxic_blowpipe, objs.blazing_blowpipe)

    public fun isDizanasQuiver(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.dizanas_quiver_uncharged,
            objs.dizanas_quiver_uncharged_trouver,
            objs.dizanas_quiver_charged,
            objs.dizanas_quiver_charged_trouver,
            objs.dizanas_quiver_infinite,
            objs.dizanas_quiver_infinite_trouver,
            objs.dizanas_max_cape,
            objs.dizanas_max_cape_trouver,
        )

    public fun isSmokeStaff(obj: InvObj?): Boolean =
        obj.isAnyType(objs.smoke_battlestaff, objs.mystic_smoke_staff, objs.twinflame_staff)

    public fun isSoulreaperAxe(obj: InvObj?): Boolean = obj.isType(objs.soulreaper_axe)

    public fun isTumekensShadow(obj: InvObj?): Boolean = obj.isType(objs.tumekens_shadow)

    public fun isTwistedBow(obj: InvObj?): Boolean = obj.isType(objs.twisted_bow)

    public fun isDragonHunterCrossbow(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.dragon_hunter_crossbow,
            objs.dragon_hunter_crossbow_t,
            objs.dragon_hunter_crossbow_b,
        )

    public fun isCrystalBow(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.crystal_bow,
            objs.bow_of_faerdhinen,
            objs.bow_of_faerdhinen_c_hefin,
            objs.bow_of_faerdhinen_c_ithell,
            objs.bow_of_faerdhinen_c_iorwerth,
            objs.bow_of_faerdhinen_c_trahaearn,
            objs.bow_of_faerdhinen_c_cadarn,
            objs.bow_of_faerdhinen_c_crwys,
            objs.bow_of_faerdhinen_c_meilyr,
            objs.bow_of_faerdhinen_c_amlodd,
        )

    public fun isCrystalHelm(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.crystal_helm_hefin,
            objs.crystal_helm_ithell,
            objs.crystal_helm_iorwerth,
            objs.crystal_helm_trahaearn,
            objs.crystal_helm_cadarn,
            objs.crystal_helm_crwys,
            objs.crystal_helm_meilyr,
            objs.crystal_helm_amlodd,
        )

    public fun isCrystalBody(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.crystal_body_hefin,
            objs.crystal_body_ithell,
            objs.crystal_body_iorwerth,
            objs.crystal_body_trahaearn,
            objs.crystal_body_cadarn,
            objs.crystal_body_crwys,
            objs.crystal_body_meilyr,
            objs.crystal_body_amlodd,
        )

    public fun isCrystalLegs(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.crystal_legs_hefin,
            objs.crystal_legs_ithell,
            objs.crystal_legs_iorwerth,
            objs.crystal_legs_trahaearn,
            objs.crystal_legs_cadarn,
            objs.crystal_legs_crwys,
            objs.crystal_legs_meilyr,
            objs.crystal_legs_amlodd,
        )

    public fun isObsidianSet(helm: InvObj?, top: InvObj?, legs: InvObj?): Boolean =
        helm.isType(objs.obsidian_helmet) &&
            top.isType(objs.obsidian_platebody) &&
            legs.isType(objs.obsidian_platelegs)

    public fun isVirtusMask(obj: InvObj?): Boolean =
        obj.isAnyType(objs.virtus_mask, objs.echo_virtus_mask)

    public fun isVirtusRobeTop(obj: InvObj?): Boolean =
        obj.isAnyType(objs.virtus_robe_top, objs.echo_virtus_robe_top)

    public fun isVirtusRobeBottom(obj: InvObj?): Boolean =
        obj.isAnyType(objs.virtus_robe_bottom, objs.echo_virtus_robe_bottom)

    public fun isVoidMeleeHelm(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.void_melee_helm,
            objs.void_melee_helm_l,
            objs.void_melee_helm_or,
            objs.void_melee_helm_l_or,
        )

    public fun isVoidRangerHelm(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.void_ranger_helm,
            objs.void_ranger_helm_l,
            objs.void_ranger_helm_or,
            objs.void_ranger_helm_l_or,
        )

    public fun isVoidMageHelm(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.void_mage_helm,
            objs.void_mage_helm_l,
            objs.void_mage_helm_or,
            objs.void_mage_helm_l_or,
        )

    public fun isVoidTop(obj: InvObj?): Boolean = isRegularVoidTop(obj) || isEliteVoidTop(obj)

    public fun isRegularVoidTop(obj: InvObj?): Boolean =
        obj.isAnyType(objs.void_top, objs.void_top_l, objs.void_top_or, objs.void_top_l_or)

    public fun isEliteVoidTop(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.elite_void_top,
            objs.elite_void_top_l,
            objs.elite_void_top_or,
            objs.elite_void_top_l_or,
        )

    public fun isVoidRobe(obj: InvObj?): Boolean = isRegularVoidRobe(obj) || isEliteVoidRobe(obj)

    public fun isRegularVoidRobe(obj: InvObj?): Boolean =
        obj.isAnyType(objs.void_robe, objs.void_robe_l, objs.void_robe_or, objs.void_robe_l_or)

    public fun isEliteVoidRobe(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.elite_void_robe,
            objs.elite_void_robe_l,
            objs.elite_void_robe_or,
            objs.elite_void_robe_l_or,
        )

    public fun isVoidGloves(obj: InvObj?): Boolean =
        obj.isAnyType(
            objs.void_gloves,
            objs.void_gloves_l,
            objs.void_gloves_or,
            objs.void_gloves_l_or,
        )

    public fun isDharokSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(
            objs.dharoks_helm_100,
            objs.dharoks_helm_75,
            objs.dharoks_helm_50,
            objs.dharoks_helm_25,
        ) &&
            top.isAnyType(
                objs.dharoks_platebody_100,
                objs.dharoks_platebody_75,
                objs.dharoks_platebody_50,
                objs.dharoks_platebody_25,
            ) &&
            legs.isAnyType(
                objs.dharoks_platelegs_100,
                objs.dharoks_platelegs_75,
                objs.dharoks_platelegs_50,
                objs.dharoks_platelegs_25,
            ) &&
            weapon.isAnyType(
                objs.dharoks_greataxe_100,
                objs.dharoks_greataxe_75,
                objs.dharoks_greataxe_50,
                objs.dharoks_greataxe_25,
            )

    public fun isToragSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(
            objs.torags_helm_100,
            objs.torags_helm_75,
            objs.torags_helm_50,
            objs.torags_helm_25,
        ) &&
            top.isAnyType(
                objs.torags_platebody_100,
                objs.torags_platebody_75,
                objs.torags_platebody_50,
                objs.torags_platebody_25,
            ) &&
            legs.isAnyType(
                objs.torags_platelegs_100,
                objs.torags_platelegs_75,
                objs.torags_platelegs_50,
                objs.torags_platelegs_25,
            ) &&
            weapon.isAnyType(
                objs.torags_hammers_100,
                objs.torags_hammers_75,
                objs.torags_hammers_50,
                objs.torags_hammers_25,
            )

    public fun isGuthanSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(
            objs.guthans_helm_100,
            objs.guthans_helm_75,
            objs.guthans_helm_50,
            objs.guthans_helm_25,
        ) &&
            top.isAnyType(
                objs.guthans_platebody_100,
                objs.guthans_platebody_75,
                objs.guthans_platebody_50,
                objs.guthans_platebody_25,
            ) &&
            legs.isAnyType(
                objs.guthans_chainskirt_100,
                objs.guthans_chainskirt_75,
                objs.guthans_chainskirt_50,
                objs.guthans_chainskirt_25,
            ) &&
            weapon.isAnyType(
                objs.guthans_warspear_100,
                objs.guthans_warspear_75,
                objs.guthans_warspear_50,
                objs.guthans_warspear_25,
            )

    public fun isKarilSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(
            objs.karils_coif_100,
            objs.karils_coif_75,
            objs.karils_coif_50,
            objs.karils_coif_25,
        ) &&
            top.isAnyType(
                objs.karils_leathertop_100,
                objs.karils_leathertop_75,
                objs.karils_leathertop_50,
                objs.karils_leathertop_25,
            ) &&
            legs.isAnyType(
                objs.karils_leatherskirt_100,
                objs.karils_leatherskirt_75,
                objs.karils_leatherskirt_50,
                objs.karils_leatherskirt_25,
            ) &&
            weapon.isAnyType(
                objs.karils_crossbow_100,
                objs.karils_crossbow_75,
                objs.karils_crossbow_50,
                objs.karils_crossbow_25,
            )

    public fun isVeracSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(
            objs.veracs_helm_100,
            objs.veracs_helm_75,
            objs.veracs_helm_50,
            objs.veracs_helm_25,
        ) &&
            top.isAnyType(
                objs.veracs_brassard_100,
                objs.veracs_brassard_75,
                objs.veracs_brassard_50,
                objs.veracs_brassard_25,
            ) &&
            legs.isAnyType(
                objs.veracs_plateskirt_100,
                objs.veracs_plateskirt_75,
                objs.veracs_plateskirt_50,
                objs.veracs_plateskirt_25,
            ) &&
            weapon.isAnyType(
                objs.veracs_flail_100,
                objs.veracs_flail_75,
                objs.veracs_flail_50,
                objs.veracs_flail_25,
            )

    public fun isBloodMoonSet(
        helm: InvObj?,
        top: InvObj?,
        legs: InvObj?,
        weapon: InvObj?,
    ): Boolean =
        helm.isAnyType(objs.blood_moon_helm, objs.blood_moon_helm_used) &&
            top.isAnyType(objs.blood_moon_chestplate, objs.blood_moon_chestplate_used) &&
            legs.isAnyType(objs.blood_moon_tassets, objs.blood_moon_tassets_used) &&
            weapon.isType(objs.dual_macuahuitl)

    public fun isBlueMoonSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(objs.blue_moon_helm, objs.blue_moon_helm_used) &&
            top.isAnyType(objs.blue_moon_chestplate, objs.blue_moon_chestplate_used) &&
            legs.isAnyType(objs.blue_moon_tassets, objs.blue_moon_tassets_used) &&
            weapon.isType(objs.blue_moon_spear)

    public fun isEclipseMoonSet(
        helm: InvObj?,
        top: InvObj?,
        legs: InvObj?,
        weapon: InvObj?,
    ): Boolean =
        helm.isAnyType(objs.eclipse_moon_helm, objs.eclipse_moon_helm_used) &&
            top.isAnyType(objs.eclipse_moon_chestplate, objs.eclipse_moon_chestplate_used) &&
            legs.isAnyType(objs.eclipse_moon_tassets, objs.eclipse_moon_tassets_used) &&
            weapon.isType(objs.eclipse_atlatl)

    public fun isAhrimSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(
            objs.ahrims_hood_100,
            objs.ahrims_hood_75,
            objs.ahrims_hood_50,
            objs.ahrims_hood_25,
        ) &&
            top.isAnyType(
                objs.ahrims_robetop_100,
                objs.ahrims_robetop_75,
                objs.ahrims_robetop_50,
                objs.ahrims_robetop_25,
            ) &&
            legs.isAnyType(
                objs.ahrims_robeskirt_100,
                objs.ahrims_robeskirt_75,
                objs.ahrims_robeskirt_50,
                objs.ahrims_robeskirt_25,
            ) &&
            weapon.isAnyType(
                objs.ahrims_staff_100,
                objs.ahrims_staff_75,
                objs.ahrims_staff_50,
                objs.ahrims_staff_25,
            )

    public fun isJusticiarSet(helm: InvObj?, top: InvObj?, legs: InvObj?): Boolean =
        helm.isType(objs.justiciar_faceguard) &&
            top.isType(objs.justiciar_chestguard) &&
            legs.isType(objs.justiciar_legguards)
}
