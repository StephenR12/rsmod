package org.rsmod.content.bosses.world

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WorldBossDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            WorldBossNpcs.derangedArchaeologist,
            NpcDropTable(sequence(item(objs.big_bones), item(objs.coins, 8_000..20_000))),
        )
        registry.register(
            WorldBossNpcs.sarachnis,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 10_000..25_000),
                    independent(denominator = 384, roll = item(WorldBossObjs.sarachnisCudgel)),
                    independent(denominator = 2_000, roll = item(WorldBossObjs.jarOfEyes)),
                    independent(denominator = 3_000, roll = item(WorldBossObjs.sarachnisPet)),
                )
            ),
        )
        registry.register(
            WorldBossNpcs.scurrius,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 4_000..12_000),
                    independent(denominator = 33, roll = item(WorldBossObjs.scurriusSpine)),
                    independent(denominator = 3_000, roll = item(WorldBossObjs.scurriusPet)),
                )
            ),
        )
        registry.register(
            WorldBossNpcs.revenantMaledictus,
            NpcDropTable(
                sequence(
                    item(objs.coins, 50_000..150_000),
                    independent(denominator = 240, roll = item(WorldBossObjs.webweaverBow)),
                    independent(denominator = 240, roll = item(WorldBossObjs.ursineChainmace)),
                    independent(denominator = 240, roll = item(WorldBossObjs.accursedSceptre)),
                )
            ),
        )
    }
}
