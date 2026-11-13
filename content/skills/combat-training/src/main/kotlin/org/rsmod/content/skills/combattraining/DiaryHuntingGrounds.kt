package org.rsmod.content.skills.combattraining

import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences

internal object DiaryHuntingGrounds : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<DiaryHuntingGrounds>("diary-hunting-grounds.toml")
    }
}

internal object HuntingGroundNpcs : NpcReferences() {
    val smallLizards =
        listOf(find("slayer_lizard_small1_green"), find("slayer_lizard_small2_sandy"))
    val desertLizards =
        listOf(
            find("slayer_lizard_large1_green"),
            find("slayer_lizard_large2_sandy"),
            find("slayer_lizard_large3_sandy"),
        )
    val largeLizards = listOf(find("slayer_lizard_massive"))
    val dustDevils = listOf(find("slayer_dustdevil"))
    val lancers = listOf(find("contact_locust_lance"))
    val archers = listOf(find("contact_locust_bow"))
    val mithrilDragons = listOf(find("brut_mithril_dragon"))
    val greenDragons =
        listOf(
            find("green_dragon"),
            find("green_dragon2"),
            find("green_dragon3"),
            find("green_dragon4"),
            find("green_dragon5"),
        )
    val lavaDragons = listOf(find("lava_dragon"))
    val mogres = listOf(find("mudskipper_ogre"))
}

internal object HuntingGroundStats : NpcEditor() {
    init {
        for (type in HuntingGroundNpcs.smallLizards) edit(type) {
            respawnRate = 50
            param[params.attackrate] = 4
            param[params.defence_slash] = 5
            param[params.defence_crush] = 5
            param[params.npc_attack_type] = categories.attacktype_crush
        }
        for (type in HuntingGroundNpcs.desertLizards) edit(type) {
            respawnRate = 25
            huntMode = huntmodes.aggressive_melee
            huntRange = 6
            param[params.attackrate] = 4
            param[params.defence_stab] = 5
            param[params.defence_slash] = 15
            param[params.defence_crush] = 15
            param[params.defence_light] = 5
            param[params.defence_standard] = 5
            param[params.defence_heavy] = 5
            param[params.npc_attack_type] = categories.attacktype_crush
        }
        for (type in HuntingGroundNpcs.largeLizards) edit(type) {
            respawnRate = 25
            huntMode = huntmodes.aggressive_melee
            huntRange = 6
            param[params.attackrate] = 4
            param[params.defence_stab] = 5
            param[params.defence_slash] = 15
            param[params.defence_crush] = 15
            param[params.defence_light] = 5
            param[params.defence_standard] = 5
            param[params.defence_heavy] = 5
            param[params.npc_attack_type] = categories.attacktype_crush
        }
        for (type in HuntingGroundNpcs.dustDevils) edit(type) {
            param[params.attack_anim] = DesertHuntAnims.dust
            respawnRate = 15
            param[params.attackrate] = 4
            param[params.npc_attack_type] = categories.attacktype_crush
        }
        for (type in HuntingGroundNpcs.lancers) edit(type) {
            param[params.attack_anim] = DesertHuntAnims.lance
            respawnRate = 7
            huntMode = huntmodes.aggressive_melee
            huntRange = 6
            param[params.attackrate] = 4
            param[params.melee_strength] = 30
            param[params.defence_stab] = 90
            param[params.defence_slash] = 90
            param[params.defence_crush] = 40
            param[params.npc_attack_type] = categories.attacktype_crush
        }
        for (type in HuntingGroundNpcs.archers) edit(type) {
            attackRange = 7
            respawnRate = 10
            huntMode = huntmodes.aggressive_ranged
            huntRange = 6
            param[params.attack_anim] = DesertHuntAnims.arrow
            param[params.attackrate] = 6
            param[params.attack_ranged] = 80
            param[params.defence_stab] = 50
            param[params.defence_slash] = 90
            param[params.defence_crush] = 50
            param[params.defence_magic] = 34
            param[params.defence_light] = 66
            param[params.defence_standard] = 66
            param[params.defence_heavy] = 66
            param[params.npc_attack_type] = categories.attacktype_crush
        }
        for (type in HuntingGroundNpcs.mithrilDragons) edit(type) {
            attackRange = 8
            respawnRate = 30
            huntMode = huntmodes.aggressive_ranged
            huntRange = 6
            param[params.attackrate] = 4
            param[params.defence_slash] = 100
            param[params.defence_crush] = 70
            param[params.defence_magic] = 30
            param[params.defence_light] = 90
            param[params.defence_standard] = 90
            param[params.defence_heavy] = 20
            param[params.npc_attack_type] = categories.attacktype_stab
        }
        for (type in HuntingGroundNpcs.greenDragons) edit(type) {
            respawnRate = 30
            huntMode = huntmodes.aggressive_melee
            huntRange = 6
            param[params.attackrate] = 4
            param[params.defence_slash] = 40
            param[params.defence_crush] = 40
            param[params.defence_magic] = 30
            param[params.defence_light] = 50
            param[params.defence_standard] = 50
            param[params.defence_heavy] = 10
            param[params.npc_attack_type] = categories.attacktype_slash
        }
        for (type in HuntingGroundNpcs.lavaDragons) edit(type) {
            respawnRate = 14
            huntMode = huntmodes.aggressive_melee
            huntRange = 6
            param[params.attackrate] = 4
            param[params.defence_stab] = 20
            param[params.defence_slash] = 90
            param[params.defence_crush] = 90
            param[params.defence_magic] = 80
            param[params.defence_light] = 70
            param[params.defence_standard] = 70
            param[params.defence_heavy] = 20
            param[params.npc_attack_type] = categories.attacktype_slash
        }
        for (type in HuntingGroundNpcs.mogres) edit(type) {
            respawnRate = 50
            huntMode = huntmodes.aggressive_melee
            huntRange = 6
            param[params.attackrate] = 6
            param[params.attack_melee] = 22
            param[params.melee_strength] = 20
            param[params.npc_attack_type] = categories.attacktype_crush
        }
    }
}
