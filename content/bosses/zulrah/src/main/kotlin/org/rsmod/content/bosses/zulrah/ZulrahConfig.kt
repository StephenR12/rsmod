package org.rsmod.content.bosses.zulrah

import org.rsmod.game.movement.MoveRestrict
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object ZulrahNpcs : NpcReferences() {
    val ranged = find("snakeboss_boss_ranged")
    val melee = find("snakeboss_boss_melee")
    val magic = find("snakeboss_boss_magic")
    val meleeSnakeling = find("snakeboss_minion_melee")
    val magicSnakeling = find("snakeboss_minion_magic")

    val bosses: List<NpcType> = listOf(ranged, melee, magic)
    val snakelings: List<NpcType> = listOf(meleeSnakeling, magicSnakeling)
    val all: List<NpcType> = bosses + snakelings
}

public object ZulrahLocs : LocReferences() {
    val boat = find("snakeboss_boat")
    val boatOneOp = find("snakeboss_boat_1op")
    val boatTwoOps = find("snakeboss_boat_2ops")
    val poisonCloud = find("snakeboss_poisoncloud")
    val exit = find("snakeboss_exit")

    val boats = listOf(boat, boatOneOp, boatTwoOps)
}

public object ZulrahSeqs : SeqReferences() {
    val attack = find("snakeboss_attack_acidx3")
    val alternateAttack = find("snakeboss_attack_acidx1")
    val spawn = find("snakeboss_spawn")
    val sink = find("snakeboss_sinkfast")
    val emerge = find("snakeboss_emergefast")
    val death = find("snakeboss_death")
    val tailLeft = find("snakeboss_attack_tail_left")
    val tailRight = find("snakeboss_attack_tail_right")
}

public object ZulrahSpotanims : SpotanimReferences() {
    val orb = find("snakeboss_orb")
    val doubleOrb = find("snakeboss_double_orb")
    val fireball = find("snakeboss_fireball")
    val egg = find("snakeboss_egg")
    val snakelingSpell = find("snakeboss_minion_spell")
}

public object ZulrahObjs : ObjReferences() {
    val scales = find("snakeboss_scale")
    val tanzaniteFang = find("blowpipe_fang")
    val magicFang = find("magic_fang")
    val serpentineVisage = find("serpentine_visage")
    val uncutOnyx = find("uncut_onyx")
    val jarOfSwamp = find("jar_of_swamp")
    val petSnakeling = find("snakepet")
    val tanzaniteMutagen = find("cyan_mutagen")
    val magmaMutagen = find("red_mutagen")
    val zulAndraTeleport = find("teleportscroll_zulandra")
    val dragonMedHelm = find("dragon_med_helm")
    val dragonHalberd = find("dragon_halberd")
    val deathRune = find("deathrune")
    val lawRune = find("lawrune")
    val chaosRune = find("chaosrune")
    val notedBattlestaff = find("cert_battlestaff")
    val notedFlax = find("cert_flax")
    val notedDragonBones = find("cert_dragon_bones")
    val notedRuniteOre = find("cert_runite_ore")
    val notedPureEssence = find("cert_blankrune_high")
    val notedYewLogs = find("cert_yew_logs")
    val notedRawShark = find("cert_raw_shark")
    val notedMantaRay = find("cert_mantaray")
    val sharkLure = find("shark_lure")
    val notedGrapes = find("cert_grapes")
    val notedCoconut = find("cert_coconut")
    val notedAdamantiteBars = find("cert_adamantite_bar")
    val notedCoal = find("cert_coal")
    val notedMahoganyLogs = find("cert_mahogany_logs")
    val notedSnakeskin = find("cert_village_snake_skin")
    val notedToadflax = find("cert_toadflax")
    val notedSnapdragon = find("cert_snapdragon")
    val notedDwarfWeed = find("cert_dwarf_weed")
    val notedTorstol = find("cert_torstol")
    val magicSeed = find("magic_tree_seed")
    val calquatSeed = find("calquat_tree_seed")
    val papayaSeed = find("papaya_tree_seed")
    val palmSeed = find("palm_tree_seed")
    val toadflaxSeed = find("toadflax_seed")
    val snapdragonSeed = find("snapdragon_seed")
    val dwarfWeedSeed = find("dwarf_weed_seed")
    val torstolSeed = find("torstol_seed")
    val spiritSeed = find("spirit_tree_seed")
    val notedAntidotePlusPlus = find("cert_antidote++4")
    val dragonstoneBoltTips = find("xbows_bolt_tips_dragonstone")
    val swampTar = find("swamp_tar")

    val runeTwoHander = find("rune_2h_sword")
    val runeBattleaxe = find("rune_battleaxe")
    val runeSqShield = find("rune_sq_shield")
    val notedSilverOre = find("cert_silver_ore")
    val natureTalisman = find("nature_talisman")

    val ringOfWealth1 = find("ring_of_wealth_1")
    val ringOfWealth2 = find("ring_of_wealth_2")
    val ringOfWealth3 = find("ring_of_wealth_3")
    val ringOfWealth4 = find("ring_of_wealth_4")
    val ringOfWealth5 = find("ring_of_wealth_5")
    val ringOfWealthI = find("ring_of_wealth_i")
    val ringOfWealthI1 = find("ring_of_wealth_i1")
    val ringOfWealthI2 = find("ring_of_wealth_i2")
    val ringOfWealthI3 = find("ring_of_wealth_i3")
    val ringOfWealthI4 = find("ring_of_wealth_i4")
    val ringOfWealthI5 = find("ring_of_wealth_i5")
}

public enum class ZulrahForm(public val npc: NpcType) {
    Ranged(ZulrahNpcs.ranged),
    Melee(ZulrahNpcs.melee),
    Magic(ZulrahNpcs.magic),
}

public enum class ZulrahPosition(public val normalCoords: CoordGrid) {
    North(CoordGrid(2266, 3074, 0)),
    South(CoordGrid(2266, 3062, 0)),
    West(CoordGrid(2258, 3071, 0)),
    East(CoordGrid(2276, 3071, 0)),
}

internal object ZulrahAttackRanges : NpcEditor() {
    init {
        with(ZulrahNpcs) {
            for (form in bosses) {
                edit(form) {
                    attackRange = 20
                    moveRestrict = MoveRestrict.NoMove
                }
            }
            edit(magicSnakeling) { attackRange = 8 }
        }
    }
}
