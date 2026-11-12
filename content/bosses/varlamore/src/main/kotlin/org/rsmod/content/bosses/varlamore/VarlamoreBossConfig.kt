package org.rsmod.content.bosses.varlamore

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object VarlamoreBossNpcs : NpcReferences() {
    val huey = find("huey_head")
    val hueyTail = find("huey_tail")
    val hueyBody = find("huey_body_part")
    val gemstoneCrab = find("gemstone_crab")
    val bloodMoon = find("pmoon_boss_blood_moon")
    val bloodJaguar = find("pmoon_boss_jaguar")
    val blueMoon = find("pmoon_boss_blue_moon")
    val frozenWeapons = find("pmoon_boss_icicle_uncracked")
    val eclipseMoon = find("pmoon_boss_eclipse_moon")
    val eclipseClone = find("pmoon_boss_eclipse_clone")
}

public object VarlamoreBossObjs : ObjReferences() {
    val dragonHunterWand = find("dragonhunter_wand")
    val hueyHide = find("huey_hide")
    val tomeOfEarth = find("tome_of_earth_uncharged")
    val huberte = find("hueypet")
    val uncutSapphire = find("uncut_sapphire")
    val uncutEmerald = find("uncut_emerald")
    val uncutRuby = find("uncut_ruby")
    val uncutDiamond = find("uncut_diamond")
    val dualMacuahuitl = find("dual_macuahuitl")
    val bloodHelm = find("blood_moon_helm")
    val bloodBody = find("blood_moon_chestplate")
    val bloodLegs = find("blood_moon_tassets")
    val blueSpear = find("frostmoon_spear")
    val blueHelm = find("frost_moon_helm")
    val blueBody = find("frost_moon_chestplate")
    val blueLegs = find("frost_moon_tassets")
    val eclipseAtlatl = find("eclipse_atlatl")
    val eclipseHelm = find("eclipse_moon_helm")
    val eclipseBody = find("eclipse_moon_chestplate")
    val eclipseLegs = find("eclipse_moon_tassets")
}

public enum class VarlamoreBossFamily {
    Huey,
    GemstoneCrab,
    BloodMoon,
    BlueMoon,
    EclipseMoon,
}

public data class VarlamoreBossDefinition(
    val npc: NpcType,
    val spawn: CoordGrid,
    val family: VarlamoreBossFamily,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int,
    val ranged: Int,
    val maxHit: Int,
)

public object VarlamoreBosses {
    public val definitions: List<VarlamoreBossDefinition> =
        listOf(
            VarlamoreBossDefinition(
                VarlamoreBossNpcs.huey,
                CoordGrid(1512, 3022, 0),
                VarlamoreBossFamily.Huey,
                2_500,
                260,
                260,
                250,
                260,
                260,
                45,
            ),
            VarlamoreBossDefinition(
                VarlamoreBossNpcs.gemstoneCrab,
                CoordGrid(1442, 3078, 0),
                VarlamoreBossFamily.GemstoneCrab,
                1_000,
                1,
                1,
                300,
                1,
                1,
                0,
            ),
            VarlamoreBossDefinition(
                VarlamoreBossNpcs.bloodMoon,
                CoordGrid(1437, 9634, 0),
                VarlamoreBossFamily.BloodMoon,
                500,
                220,
                220,
                150,
                1,
                1,
                30,
            ),
            VarlamoreBossDefinition(
                VarlamoreBossNpcs.blueMoon,
                CoordGrid(1410, 9665, 0),
                VarlamoreBossFamily.BlueMoon,
                500,
                180,
                180,
                150,
                220,
                1,
                30,
            ),
            VarlamoreBossDefinition(
                VarlamoreBossNpcs.eclipseMoon,
                CoordGrid(1465, 9665, 0),
                VarlamoreBossFamily.EclipseMoon,
                500,
                200,
                200,
                150,
                1,
                220,
                30,
            ),
        )

    public val byId: Map<Int, VarlamoreBossDefinition> = definitions.associateBy { it.npc.id }
}
