package org.rsmod.content.bosses.instanced

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.game.type.npc.NpcType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class InstancedBossDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            InstancedBossNpcs.vorkath,
            NpcDropTable(
                sequence(
                    item(objs.superior_dragon_bones),
                    item(objs.coins, 50_000..150_000),
                    independent(denominator = 50, roll = item(InstancedBossObjs.vorkathHead)),
                    independent(
                        denominator = 1_000,
                        roll = item(InstancedBossObjs.dragonboneNecklace),
                    ),
                    independent(denominator = 5_000, roll = item(InstancedBossObjs.skeletalVisage)),
                    independent(denominator = 3_000, roll = item(InstancedBossObjs.jarOfDecay)),
                    independent(denominator = 3_000, roll = item(InstancedBossObjs.vorki)),
                )
            ),
        )
        registry.register(
            InstancedBossNpcs.muspah,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 80_000..200_000),
                    independent(denominator = 50, roll = item(InstancedBossObjs.ancientIcon)),
                    independent(denominator = 100, roll = item(InstancedBossObjs.venatorShard)),
                    independent(denominator = 2_500, roll = item(InstancedBossObjs.muphin)),
                )
            ),
        )
        registerNightmare(InstancedBossNpcs.nightmare, 600, 4_000, false)
        registerNightmare(InstancedBossNpcs.phosani, 200, 1_400, true)
    }

    private fun registerNightmare(npc: NpcType, rate: Int, petRate: Int, phosani: Boolean) {
        val extras =
            if (phosani)
                arrayOf(
                    independent(denominator = 100, roll = item(InstancedBossObjs.slepeyTablet)),
                    independent(denominator = 200, roll = item(InstancedBossObjs.parasiticEgg)),
                )
            else emptyArray()
        registry.register(
            npc,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 80_000..250_000),
                    independent(denominator = rate, roll = item(InstancedBossObjs.inquisitorsMace)),
                    independent(denominator = rate, roll = item(InstancedBossObjs.inquisitorsHelm)),
                    independent(denominator = rate, roll = item(InstancedBossObjs.inquisitorsBody)),
                    independent(
                        denominator = rate,
                        roll = item(InstancedBossObjs.inquisitorsSkirt),
                    ),
                    independent(denominator = rate, roll = item(InstancedBossObjs.nightmareStaff)),
                    independent(denominator = rate, roll = item(InstancedBossObjs.harmonisedOrb)),
                    independent(denominator = rate, roll = item(InstancedBossObjs.volatileOrb)),
                    independent(denominator = rate, roll = item(InstancedBossObjs.eldritchOrb)),
                    independent(denominator = 2_000, roll = item(InstancedBossObjs.jarOfDreams)),
                    independent(
                        denominator = petRate,
                        roll = item(InstancedBossObjs.littleNightmare),
                    ),
                    *extras,
                )
            ),
        )
    }
}
