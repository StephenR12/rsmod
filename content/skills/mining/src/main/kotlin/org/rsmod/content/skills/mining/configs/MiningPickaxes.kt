package org.rsmod.content.skills.mining.configs

import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

internal object MiningPickaxes : ObjEditor() {
    init {
        pickaxe(MiningObjs.bronzePickaxe, 1, 8, MiningSeqs.bronze)
        pickaxe(MiningObjs.ironPickaxe, 1, 7, MiningSeqs.iron)
        pickaxe(MiningObjs.steelPickaxe, 6, 6, MiningSeqs.steel)
        pickaxe(MiningObjs.blackPickaxe, 11, 5, MiningSeqs.black)
        pickaxe(MiningObjs.mithrilPickaxe, 21, 5, MiningSeqs.mithril)
        pickaxe(MiningObjs.adamantPickaxe, 31, 4, MiningSeqs.adamant)
        pickaxe(MiningObjs.runePickaxe, 41, 3, MiningSeqs.rune)
        pickaxe(MiningObjs.gildedPickaxe, 41, 3, MiningSeqs.gilded)
        pickaxe(MiningObjs.dragonPickaxe, 61, 3, MiningSeqs.dragon, fastChance = 6)
        pickaxe(MiningObjs.dragonPickaxeUpgraded, 61, 3, MiningSeqs.dragonUpgraded, fastChance = 6)
        pickaxe(MiningObjs.dragonPickaxeZalcano, 61, 3, MiningSeqs.dragonZalcano, fastChance = 6)
        pickaxe(
            MiningObjs.dragonPickaxeTrailblazer,
            61,
            3,
            MiningSeqs.dragonTrailblazer,
            fastChance = 6,
        )
        pickaxe(
            MiningObjs.dragonPickaxeTrailblazerReloaded,
            61,
            3,
            MiningSeqs.dragonTrailblazerReloaded,
            fastChance = 6,
        )
        pickaxe(MiningObjs.infernalPickaxe, 61, 3, MiningSeqs.infernal, fastChance = 6)
        pickaxe(MiningObjs.infernalPickaxeUncharged, 61, 3, MiningSeqs.infernal, fastChance = 6)
        pickaxe(
            MiningObjs.infernalPickaxeTrailblazer,
            61,
            3,
            MiningSeqs.infernalTrailblazer,
            fastChance = 6,
        )
        pickaxe(
            MiningObjs.infernalPickaxeTrailblazerUncharged,
            61,
            3,
            MiningSeqs.infernalTrailblazer,
            fastChance = 6,
        )
        pickaxe(
            MiningObjs.infernalPickaxeTrailblazerReloaded,
            61,
            3,
            MiningSeqs.infernalTrailblazerReloaded,
            fastChance = 6,
        )
        pickaxe(
            MiningObjs.infernalPickaxeTrailblazerReloadedUncharged,
            61,
            3,
            MiningSeqs.infernalTrailblazerReloaded,
            fastChance = 6,
        )
        pickaxe(MiningObjs.thirdAgePickaxe, 61, 3, MiningSeqs.thirdAge, fastChance = 6)
        pickaxe(
            MiningObjs.crystalPickaxe,
            71,
            3,
            MiningSeqs.crystal,
            fastChance = 4,
            soteRequired = true,
        )
        pickaxe(
            MiningObjs.crystalPickaxeInactive,
            71,
            3,
            MiningSeqs.dragon,
            fastChance = 6,
            soteRequired = true,
        )
    }

    private fun pickaxe(
        type: ObjType,
        level: Int,
        interval: Int,
        anim: SeqType,
        fastChance: Int = 0,
        soteRequired: Boolean = false,
    ) {
        edit(type) {
            contentGroup = content.mining_pickaxe
            param[params.levelrequire] = level
            param[params.skill_anim] = anim
            param[MiningParams.pickaxeInterval] = interval
            param[MiningParams.pickaxeFastIntervalChance] = fastChance
            param[MiningParams.pickaxeSoteRequired] = soteRequired
        }
    }
}
