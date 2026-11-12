package org.rsmod.content.bosses.wilderness

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRoll
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WildernessBossDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registerBear(WildernessBossNpcs.callisto, lesser = false)
        registerBear(WildernessBossNpcs.artio, lesser = true)
        registerSpider(WildernessBossNpcs.venenatis, lesser = false)
        registerSpider(WildernessBossNpcs.spindel, lesser = true)
        registerSkeleton(WildernessBossNpcs.vetion, lesser = false)
        registerSkeleton(WildernessBossNpcs.calvarion, lesser = true)
        registry.register(
            WildernessBossNpcs.scorpia,
            table(
                item(WildernessBossObjs.bigBones),
                unique(256, WildernessBossObjs.odiumShard3),
                unique(256, WildernessBossObjs.maledictionShard3),
                unique(2016, WildernessBossObjs.scorpiaPet),
            ),
        )
        registry.register(
            WildernessBossNpcs.chaosFanatic,
            table(
                item(WildernessBossObjs.bigBones),
                unique(256, WildernessBossObjs.odiumShard1),
                unique(256, WildernessBossObjs.maledictionShard1),
            ),
        )
        registry.register(
            WildernessBossNpcs.crazyArchaeologist,
            table(
                item(WildernessBossObjs.bigBones),
                unique(256, WildernessBossObjs.odiumShard2),
                unique(256, WildernessBossObjs.maledictionShard2),
            ),
        )
        registry.register(
            WildernessBossNpcs.chaosElemental,
            table(
                item(WildernessBossObjs.bigBones),
                unique(128, WildernessBossObjs.dragonTwoHander),
                unique(256, WildernessBossObjs.dragonPickaxe),
                unique(300, WildernessBossObjs.chaosElementalPet),
            ),
        )
        registry.register(
            WildernessBossNpcs.kingBlackDragon,
            table(
                sequence(
                    item(WildernessBossObjs.dragonBones),
                    item(WildernessBossObjs.blackDragonhide, 2..2),
                ),
                unique(1000, WildernessBossObjs.dragonPickaxe),
                unique(5000, WildernessBossObjs.dragonfireVisage),
                unique(3000, WildernessBossObjs.kbdPet),
            ),
        )
    }

    private fun registerBear(type: NpcType, lesser: Boolean) {
        registry.register(
            type,
            wildernessReworkTable(
                lesser,
                WildernessBossObjs.callistoClaws,
                WildernessBossObjs.voidwakerHilt,
                WildernessBossObjs.tyrannicalRing,
                WildernessBossObjs.callistoPet,
            ),
        )
    }

    private fun registerSpider(type: NpcType, lesser: Boolean) {
        registry.register(
            type,
            wildernessReworkTable(
                lesser,
                WildernessBossObjs.venenatisFang,
                WildernessBossObjs.voidwakerGem,
                WildernessBossObjs.treasonousRing,
                WildernessBossObjs.venenatisPet,
            ),
        )
    }

    private fun registerSkeleton(type: NpcType, lesser: Boolean) {
        registry.register(
            type,
            wildernessReworkTable(
                lesser,
                WildernessBossObjs.vetionSkull,
                WildernessBossObjs.voidwakerBlade,
                WildernessBossObjs.ringOfGods,
                WildernessBossObjs.vetionPet,
            ),
        )
    }

    private fun wildernessReworkTable(
        lesser: Boolean,
        attachment: ObjType,
        voidwaker: ObjType,
        ring: ObjType,
        pet: ObjType,
    ): NpcDropTable {
        val equipmentDenominator = if (lesser) 912 else 196
        val voidwakerDenominator = if (lesser) 1080 else 360
        val standardDenominator = if (lesser) 358 else 256
        val ringDenominator = if (lesser) 716 else 512
        val petDenominator = if (lesser) 2800 else 1500
        return table(
            item(WildernessBossObjs.bigBones),
            unique(equipmentDenominator, attachment),
            unique(voidwakerDenominator, voidwaker),
            unique(standardDenominator, WildernessBossObjs.dragonPickaxe),
            unique(standardDenominator, WildernessBossObjs.dragonTwoHander),
            unique(ringDenominator, ring),
            unique(petDenominator, pet),
        )
    }

    private fun table(always: NpcDropRoll, vararg uniques: NpcDropRoll): NpcDropTable =
        NpcDropTable(sequence(always, item(objs.coins, 10_000..35_000), *uniques))

    private fun unique(denominator: Int, obj: ObjType): NpcDropRoll =
        independent(denominator = denominator, roll = item(obj))
}
