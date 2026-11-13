package org.rsmod.content.skills.mining.configs

import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.content.skills.mining.KourendMineralLocs
import org.rsmod.content.skills.mining.KourendMineralObjs
import org.rsmod.content.skills.mining.MiningGuildLocs
import org.rsmod.content.skills.mining.QuarryLocs
import org.rsmod.content.skills.mining.QuarryObjs
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType

internal object MiningRocks : LocEditor() {
    init {
        // The published75-second respawn already describes the Guild's actual amethyst timer.
        for (wall in MiningGuildLocs.amethyst) rock(
            wall,
            MiningGuildLocs.empty,
            objs.amethyst,
            92,
            240.0,
            125,
            -18,
            10,
        )
        // Lovakite has no dated extraction chart: 16/100 is an explicit playable approximation.
        // Sulphur uses its published100/220 curve; levels, XP and respawns are dated.
        for (type in KourendMineralLocs.lovakite) rock(
            type,
            MiningLocs.empty1,
            KourendMineralObjs.lovakite,
            65,
            60.0,
            58,
            16,
            100,
        )
        for (type in KourendMineralLocs.sulphur) rock(
            type,
            MiningLocs.empty1,
            KourendMineralObjs.sulphur,
            42,
            35.0,
            42,
            100,
            220,
        )
        rock(QuarryLocs.granite, MiningLocs.empty1, QuarryObjs.granite[0], 45, 50.0, 8, 16, 100)
        rock(QuarryLocs.sandstone, MiningLocs.empty1, QuarryObjs.sandstone[0], 35, 30.0, 8, 25, 200)
        for ((ore, depleted) in HauntedOreWalls.mithril.zip(HauntedOreWalls.depleted)) {
            rock(ore, depleted, objs.mithril_ore, 55, 80.0, 200, 3, 50)
        }
        for ((i, stage) in MiningLocs.barronite.withIndex()) rock(
            stage,
            MiningLocs.barroniteEmpty[i],
            MiningObjs.barroniteShards,
            14,
            16.0,
            100,
            80,
            100,
        )
        // No period success chart survives for these two deposits. Depletion and yields
        // are independent of these estimated level 1 / 99 success bounds.
        rock(
            MiningLocs.fossilAsh,
            MiningLocs.fossilEmpty,
            MiningObjs.volcanicAsh,
            22,
            10.0,
            50,
            60,
            220,
        )
        for ((i, rock) in MiningLocs.calcified.withIndex()) {
            rock(rock, MiningLocs.calcifiedEmpty[i], MiningObjs.shards, 41, 33.0, 50, 50, 200)
        }
        for ((i, stage) in MiningLocs.limestone.withIndex()) rock(
            stage,
            MiningLocs.limestone.getOrNull(i + 1) ?: MiningLocs.depletedLimestone,
            MiningObjs.limestone,
            10,
            26.5,
            listOf(39, 19, 9)[i],
            95,
            350,
        )
        rock(MiningLocs.prifClay, MiningLocs.prifEmpty, objs.soft_clay, 70, 5.0, 2, 128, 400)
        rock(MiningLocs.clay1, MiningLocs.empty1, objs.clay, 1, 5.0, 2, 128, 400)
        rock(MiningLocs.clay2, MiningLocs.empty2, objs.clay, 1, 5.0, 2, 128, 400)
        rock(MiningLocs.copper1, MiningLocs.empty1, objs.copper_ore, 1, 17.5, 4, 100, 350)
        rock(MiningLocs.copper2, MiningLocs.empty2, objs.copper_ore, 1, 17.5, 4, 100, 350)
        rock(MiningLocs.tin1, MiningLocs.empty1, objs.tin_ore, 1, 17.5, 4, 100, 350)
        rock(MiningLocs.tin2, MiningLocs.empty2, objs.tin_ore, 1, 17.5, 4, 100, 350)
        rock(MiningLocs.blurite1, MiningLocs.empty1, objs.blurite_ore, 10, 17.5, 42, 90, 350)
        rock(MiningLocs.blurite2, MiningLocs.empty2, objs.blurite_ore, 10, 17.5, 42, 90, 350)
        rock(MiningLocs.iron1, MiningLocs.empty1, objs.iron_ore, 15, 35.0, 9, 96, 350)
        rock(MiningLocs.iron2, MiningLocs.empty2, objs.iron_ore, 15, 35.0, 9, 96, 350)
        rock(MiningLocs.silver1, MiningLocs.empty1, objs.silver_ore, 20, 40.0, 100, 25, 200)
        rock(MiningLocs.silver2, MiningLocs.empty2, objs.silver_ore, 20, 40.0, 100, 25, 200)
        rock(MiningLocs.coal1, MiningLocs.empty1, objs.coal, 30, 50.0, 50, 16, 100)
        rock(MiningLocs.coal2, MiningLocs.empty2, objs.coal, 30, 50.0, 50, 16, 100)
        rock(MiningLocs.gold1, MiningLocs.empty1, objs.gold_ore, 40, 65.0, 100, 7, 75)
        rock(MiningLocs.gold2, MiningLocs.empty2, objs.gold_ore, 40, 65.0, 100, 7, 75)
        rock(MiningLocs.mithril1, MiningLocs.empty1, objs.mithril_ore, 55, 80.0, 200, 3, 50)
        rock(MiningLocs.mithril2, MiningLocs.empty2, objs.mithril_ore, 55, 80.0, 200, 3, 50)
        rock(MiningLocs.adamantite1, MiningLocs.empty1, objs.adamantite_ore, 70, 95.0, 400, 0, 25)
        rock(MiningLocs.adamantite2, MiningLocs.empty2, objs.adamantite_ore, 70, 95.0, 400, 0, 25)
        rock(MiningLocs.runite1, MiningLocs.empty1, objs.runite_ore, 85, 125.0, 1200, 10, 18)
        rock(MiningLocs.runite2, MiningLocs.empty2, objs.runite_ore, 85, 125.0, 1200, 10, 18)
        gemRock(MiningLocs.gemRock1, MiningLocs.empty1)
        gemRock(MiningLocs.gemRock2, MiningLocs.empty2)
    }

    private fun rock(
        type: LocType,
        depleted: LocType,
        product: ObjType,
        level: Int,
        xp: Double,
        respawn: Int,
        low: Int,
        high: Int,
    ) {
        edit(type) {
            contentGroup = content.ore
            param[params.levelrequire] = level
            param[params.skill_xp] = PlayerStatMap.toFineXP(xp).toInt()
            param[params.skill_productitem] = product
            param[params.next_loc_stage] = depleted
            param[params.respawn_time] = respawn
            param[MiningParams.successLow] = low
            param[MiningParams.successHigh] = high
        }
    }

    private fun gemRock(type: LocType, depleted: LocType) {
        edit(type) {
            contentGroup = content.ore
            param[params.levelrequire] = 40
            param[params.skill_xp] = PlayerStatMap.toFineXP(65.0).toInt()
            param[params.next_loc_stage] = depleted
            param[params.respawn_time] = 99
            param[MiningParams.gemRock] = true
            param[MiningParams.successLow] = 28
            param[MiningParams.successHigh] = 70
        }
    }
}

internal object HauntedOreWalls : LocReferences() {
    val mithril =
        listOf(
            find("lotr_mine_wall_mithril_l"),
            find("lotr_mine_wall_mithril_r"),
            find("lotr_mine_wall_mithril_single"),
            find("lotr_mine_wall_mithril_mid"),
        )
    val depleted =
        listOf(
            find("lotr_mine_wall_depleted_l"),
            find("lotr_mine_wall_depleted_r"),
            find("lotr_mine_wall_depleted_single"),
            find("lotr_mine_wall_depleted_mid"),
        )
}
