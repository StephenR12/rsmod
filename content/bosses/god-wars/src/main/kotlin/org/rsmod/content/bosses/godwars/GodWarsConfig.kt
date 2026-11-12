package org.rsmod.content.bosses.godwars

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid

public object GodWarsNpcs : NpcReferences() {
    val zilyana = find("godwars_saradomin_avatar")
    val starlight = find("godwars_saradomin_unicorn")
    val growler = find("godwars_saradomin_lion")
    val bree = find("godwars_saradomin_centaur")

    val graardor = find("godwars_bandos_avatar")
    val strongstack = find("godwars_sergeant_goblin1")
    val steelwill = find("godwars_sergeant_goblin2")
    val grimspike = find("godwars_sergeant_goblin3")

    val kril = find("godwars_zamorak_avatar")
    val tstanon = find("godwars_ancient_greater_demon")
    val balfrug = find("godwars_ancient_lesser_demon")
    val zakln = find("godwars_ancient_black_demon")

    val kree = find("godwars_armadyl_avatar")
    val skree = find("godwars_armadyl_bodyguard_skree")
    val geerin = find("godwars_armadyl_bodyguard_geerin")
    val kilisa = find("godwars_armadyl_bodyguard_kilisa")
}

public object GodWarsSeqs : SeqReferences() {
    val graardorAttack = find("godwars_bandos_attack")
    val zilyanaAttack = find("godwars_saradomin_attack")
    val zilyanaMagic = find("godwars_saradomin_magic_attack")
    val krilAttack = find("godwars_zamorak_attack")
    val krilMagic = find("godwars_zamorak_magic_attack")
    val kreeRanged = find("godwars_armadyl_avatar_wind_attack")
    val kreeMelee = find("godwars_armadyl_avatar_claw_attack")
    val armadylRanged = find("godwars_armadyl_cannon_attack")
    val armadylMelee = find("godwars_armadyl_sword_attack")
    val minionMelee = find("godwars_zamorak_attack")
    val minionMagic = find("godwars_zamorak_magic_attack")
}

public object GodWarsSpotanims : SpotanimReferences() {
    val graardorRanged = find("godwars_bandos_spot")
    val zilyanaMagic = find("godwars_saradomin_magic_attack_spotanim")
    val krilMagic = find("godwars_zamorak_magic_attack_spotanim")
    val kreeRanged = find("godwars_armadyl_avatar_wind_attack_spotanim")
    val kreeMagic = find("godwars_armadyl_avatar_magic_attack_spotanim")
}

public object GodWarsObjs : ObjReferences() {
    val bigBones = find("big_bones")
    val feather = find("feather")
    val infernalAshes = find("infernal_ashes")

    val armadylHelmet = find("armadyl_helmet")
    val armadylChestplate = find("armadyl_chestplate")
    val armadylChainskirt = find("armadyl_skirt")
    val armadylHilt = find("godwars_godsword_hilt_armadyl")
    val bandosChestplate = find("bandos_chestplate")
    val bandosTassets = find("bandos_skirt")
    val bandosBoots = find("bandos_boots")
    val bandosHilt = find("godwars_godsword_hilt_bandos")
    val saradominSword = find("saradomin_sword")
    val saradominLight = find("saradomin_light")
    val armadylCrossbow = find("acb")
    val saradominHilt = find("godwars_godsword_hilt_saradomin")
    val steamBattlestaff = find("steam_battlestaff")
    val zamorakianSpear = find("zamorak_spear")
    val staffOfTheDead = find("sotd")
    val zamorakHilt = find("godwars_godsword_hilt_zamorak")
    val shard1 = find("godwars_godsword_blade1")
    val shard2 = find("godwars_godsword_blade2")
    val shard3 = find("godwars_godsword_blade3")

    val petKree = find("armadylpet")
    val petGraardor = find("bandospet")
    val petZilyana = find("saradominpet")
    val petKril = find("zamorakpet")

    val runeLongsword = find("rune_longsword")
    val runeTwoHander = find("rune_2h_sword")
    val runePlatebody = find("rune_platebody")
    val runeCrossbow = find("xbows_crossbow_runite")
    val blackDhideBody = find("black_dragonhide_body")
    val runeSword = find("rune_sword")
    val dragonLongsword = find("dragon_longsword")
    val dragonDagger = find("dragon_dagger")
    val magicLogsNoted = find("cert_magic_logs")
    val coalNoted = find("cert_coal")
    val adamantiteOreNoted = find("cert_adamantite_ore")
    val grimySnapdragonNoted = find("cert_unidentified_snapdragon")
    val grimyDwarfWeedNoted = find("cert_unidentified_dwarf_weed")
    val snapdragonSeed = find("snapdragon_seed")
    val dwarfWeedSeed = find("dwarf_weed_seed")
    val prayerPotion4 = find("4doseprayerrestore")
    val superRestore4 = find("4dose2restore")
    val saradominBrew3 = find("3dosepotionofsaradomin")
    val superDefence3 = find("3dose2defense")
}

public enum class GodWarsStyle {
    Melee,
    Ranged,
    Magic,
}

public enum class GodWarsBoss {
    Graardor,
    Kree,
    Zilyana,
    Kril,
}

public data class GodWarsStats(
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val ranged: Int,
    val magic: Int,
)

public data class GodWarsCombatant(
    val npc: NpcType,
    val spawn: CoordGrid,
    val style: GodWarsStyle,
    val maxHit: Int,
    val attackRate: Int,
    val animation: SeqType,
    val stats: GodWarsStats? = null,
    val boss: GodWarsBoss? = null,
)

public data class GodWarsRoom(
    val boss: GodWarsBoss,
    val minX: Int,
    val maxX: Int,
    val minZ: Int,
    val maxZ: Int,
    val level: Int,
) {
    fun contains(player: Player): Boolean =
        player.coords.level == level &&
            player.coords.x in minX..maxX &&
            player.coords.z in minZ..maxZ
}

public object GodWarsDungeon {
    val rooms =
        listOf(
            GodWarsRoom(GodWarsBoss.Graardor, 2854, 2876, 5349, 5369, 2),
            GodWarsRoom(GodWarsBoss.Kree, 2818, 2844, 5294, 5315, 2),
            GodWarsRoom(GodWarsBoss.Zilyana, 2889, 2912, 5256, 5278, 0),
            GodWarsRoom(GodWarsBoss.Kril, 2912, 2938, 5318, 5344, 2),
        )

    val combatants =
        listOf(
            GodWarsCombatant(
                GodWarsNpcs.graardor,
                CoordGrid(2864, 5354, 2),
                GodWarsStyle.Melee,
                60,
                6,
                GodWarsSeqs.graardorAttack,
                GodWarsStats(255, 280, 350, 250, 350, 80),
                GodWarsBoss.Graardor,
            ),
            GodWarsCombatant(
                GodWarsNpcs.strongstack,
                CoordGrid(2867, 5357, 2),
                GodWarsStyle.Melee,
                15,
                5,
                GodWarsSeqs.minionMelee,
            ),
            GodWarsCombatant(
                GodWarsNpcs.steelwill,
                CoordGrid(2861, 5357, 2),
                GodWarsStyle.Magic,
                15,
                5,
                GodWarsSeqs.minionMagic,
            ),
            GodWarsCombatant(
                GodWarsNpcs.grimspike,
                CoordGrid(2861, 5352, 2),
                GodWarsStyle.Ranged,
                21,
                5,
                GodWarsSeqs.armadylRanged,
            ),
            GodWarsCombatant(
                GodWarsNpcs.kree,
                CoordGrid(2830, 5302, 2),
                GodWarsStyle.Ranged,
                69,
                3,
                GodWarsSeqs.kreeRanged,
                GodWarsStats(255, 260, 350, 260, 380, 200),
                GodWarsBoss.Kree,
            ),
            GodWarsCombatant(
                GodWarsNpcs.skree,
                CoordGrid(2826, 5302, 2),
                GodWarsStyle.Magic,
                16,
                5,
                GodWarsSeqs.armadylRanged,
            ),
            GodWarsCombatant(
                GodWarsNpcs.geerin,
                CoordGrid(2835, 5302, 2),
                GodWarsStyle.Ranged,
                25,
                5,
                GodWarsSeqs.armadylRanged,
            ),
            GodWarsCombatant(
                GodWarsNpcs.kilisa,
                CoordGrid(2830, 5298, 2),
                GodWarsStyle.Melee,
                15,
                5,
                GodWarsSeqs.armadylMelee,
            ),
            GodWarsCombatant(
                GodWarsNpcs.zilyana,
                CoordGrid(2902, 5265, 0),
                GodWarsStyle.Melee,
                27,
                2,
                GodWarsSeqs.zilyanaAttack,
                GodWarsStats(255, 280, 196, 300, 250, 300),
                GodWarsBoss.Zilyana,
            ),
            GodWarsCombatant(
                GodWarsNpcs.starlight,
                CoordGrid(2898, 5265, 0),
                GodWarsStyle.Melee,
                15,
                5,
                GodWarsSeqs.minionMelee,
            ),
            GodWarsCombatant(
                GodWarsNpcs.growler,
                CoordGrid(2905, 5261, 0),
                GodWarsStyle.Magic,
                16,
                5,
                GodWarsSeqs.minionMagic,
            ),
            GodWarsCombatant(
                GodWarsNpcs.bree,
                CoordGrid(2905, 5269, 0),
                GodWarsStyle.Ranged,
                21,
                5,
                GodWarsSeqs.armadylRanged,
            ),
            GodWarsCombatant(
                GodWarsNpcs.kril,
                CoordGrid(2925, 5331, 2),
                GodWarsStyle.Melee,
                46,
                4,
                GodWarsSeqs.krilAttack,
                GodWarsStats(255, 340, 300, 270, 150, 300),
                GodWarsBoss.Kril,
            ),
            GodWarsCombatant(
                GodWarsNpcs.tstanon,
                CoordGrid(2921, 5331, 2),
                GodWarsStyle.Melee,
                15,
                5,
                GodWarsSeqs.minionMelee,
            ),
            GodWarsCombatant(
                GodWarsNpcs.balfrug,
                CoordGrid(2928, 5327, 2),
                GodWarsStyle.Magic,
                16,
                5,
                GodWarsSeqs.minionMagic,
            ),
            GodWarsCombatant(
                GodWarsNpcs.zakln,
                CoordGrid(2928, 5335, 2),
                GodWarsStyle.Ranged,
                21,
                5,
                GodWarsSeqs.armadylRanged,
            ),
        )

    val byNpcId: Map<Int, GodWarsCombatant> = combatants.associateBy { it.npc.id }
    val roomByBoss: Map<GodWarsBoss, GodWarsRoom> = rooms.associateBy { it.boss }
}
