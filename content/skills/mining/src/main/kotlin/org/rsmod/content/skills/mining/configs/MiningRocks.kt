package org.rsmod.content.skills.mining.configs

import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType

internal object MiningRocks : LocEditor() {
    init {
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
