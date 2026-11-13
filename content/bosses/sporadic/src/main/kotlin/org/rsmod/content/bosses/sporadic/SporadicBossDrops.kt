package org.rsmod.content.bosses.sporadic

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class SporadicBossDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            SporadicBossNpcs.obor,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 5_000..20_000),
                    independent(
                        denominator = OBOR_UNIQUE_RATE,
                        roll = item(SporadicBossObjs.hillGiantClub),
                    ),
                )
            ),
        )
        registry.register(
            SporadicBossNpcs.bryophyta,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 5_000..20_000),
                    independent(
                        denominator = BRYOPHYTA_UNIQUE_RATE,
                        roll = item(SporadicBossObjs.bryophytasEssence),
                    ),
                )
            ),
        )
        registry.register(
            SporadicBossNpcs.mimic,
            NpcDropTable(
                sequence(
                    item(objs.coins, 50_000..150_000),
                    independent(
                        denominator = MIMIC_RING_RATE,
                        roll = item(SporadicBossObjs.ringOfThirdAge),
                    ),
                )
            ),
        )
        registry.register(
            SporadicBossNpcs.hespori,
            // Cultivated Hespori is harvested with a spade after the fight.
            NpcDropTable(org.rsmod.api.death.NpcDropRolls.nothing()),
        )
        registry.register(
            SporadicBossNpcs.skotizo,
            NpcDropTable(
                sequence(
                    item(SporadicBossObjs.ancientShard, 1..5),
                    item(objs.coins, 20_000..100_000),
                    independent(
                        denominator = SKOTIZO_TOTEM_RATE,
                        roll = item(SporadicBossObjs.darkTotem),
                    ),
                    independent(
                        denominator = SKOTIZO_CLAW_RATE,
                        roll = item(SporadicBossObjs.darkClaw),
                    ),
                    independent(
                        denominator = SKOTIZO_JAR_RATE,
                        roll = item(SporadicBossObjs.jarOfDarkness),
                    ),
                    independent(
                        denominator = SKOTIZO_PET_RATE,
                        roll = item(SporadicBossObjs.skotos),
                    ),
                )
            ),
        )
    }

    public companion object {
        public const val OBOR_UNIQUE_RATE: Int = 118
        public const val BRYOPHYTA_UNIQUE_RATE: Int = 118
        public const val MIMIC_RING_RATE: Int = 40
        public const val HESPORI_BUCKET_RATE: Int = 35
        public const val SKOTIZO_TOTEM_RATE: Int = 5
        public const val SKOTIZO_CLAW_RATE: Int = 25
        public const val SKOTIZO_JAR_RATE: Int = 200
        public const val SKOTIZO_PET_RATE: Int = 65
    }
}
