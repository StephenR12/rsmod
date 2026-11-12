package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class SlayerBossDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            SlayerBossNpcs.cerberus,
            NpcDropTable(
                sequence(
                    item(objs.ashes),
                    item(objs.coins, 20_000..60_000),
                    independent(denominator = 512, roll = item(SlayerBossObjs.primordialCrystal)),
                    independent(denominator = 512, roll = item(SlayerBossObjs.pegasianCrystal)),
                    independent(denominator = 512, roll = item(SlayerBossObjs.eternalCrystal)),
                    independent(denominator = 512, roll = item(SlayerBossObjs.smoulderingStone)),
                    independent(denominator = 2_000, roll = item(SlayerBossObjs.jarOfSouls)),
                    independent(denominator = 3_000, roll = item(SlayerBossObjs.hellpuppy)),
                )
            ),
        )
        registry.register(
            SlayerBossNpcs.kraken,
            NpcDropTable(
                sequence(
                    item(objs.coins, 20_000..60_000),
                    independent(denominator = 512, roll = item(SlayerBossObjs.trident)),
                    independent(denominator = 400, roll = item(SlayerBossObjs.krakenTentacle)),
                    independent(denominator = 1_000, roll = item(SlayerBossObjs.jarOfDirt)),
                    independent(denominator = 3_000, roll = item(SlayerBossObjs.krakenPet)),
                )
            ),
        )
        registry.register(
            SlayerBossNpcs.thermy,
            NpcDropTable(
                sequence(
                    item(objs.ashes),
                    item(objs.coins, 15_000..50_000),
                    independent(denominator = 350, roll = item(SlayerBossObjs.occultNecklace)),
                    independent(denominator = 512, roll = item(SlayerBossObjs.smokeBattlestaff)),
                    independent(denominator = 2_000, roll = item(SlayerBossObjs.jarOfSmoke)),
                    independent(denominator = 3_000, roll = item(SlayerBossObjs.smokeDevilPet)),
                )
            ),
        )
        registry.register(
            SlayerBossNpcs.dusk,
            NpcDropTable(
                sequence(
                    item(objs.coins, 30_000..80_000),
                    independent(denominator = 1_000, roll = item(SlayerBossObjs.tourmalineCore)),
                    independent(denominator = 500, roll = item(SlayerBossObjs.graniteGloves)),
                    independent(denominator = 500, roll = item(SlayerBossObjs.graniteRing)),
                    independent(denominator = 500, roll = item(SlayerBossObjs.graniteHammer)),
                    independent(denominator = 5_000, roll = item(SlayerBossObjs.jarOfStone)),
                    independent(denominator = 3_000, roll = item(SlayerBossObjs.noon)),
                )
            ),
        )
        registry.register(
            SlayerBossNpcs.dawn,
            NpcDropTable(sequence(item(objs.coins, 15_000..40_000))),
        )
        registry.register(
            SlayerBossNpcs.sire,
            NpcDropTable(
                sequence(
                    item(objs.ashes),
                    item(objs.coins, 30_000..80_000),
                    independent(denominator = 100, roll = item(SlayerBossObjs.unsired)),
                    independent(denominator = 492, roll = item(SlayerBossObjs.abyssalDagger)),
                    independent(denominator = 492, roll = item(SlayerBossObjs.abyssalBludgeon)),
                    independent(denominator = 984, roll = item(SlayerBossObjs.jarOfMiasma)),
                    independent(denominator = 2_560, roll = item(SlayerBossObjs.abyssalOrphan)),
                )
            ),
        )
        registry.register(
            SlayerBossNpcs.araxxor,
            NpcDropTable(
                sequence(
                    item(objs.ashes),
                    item(objs.coins, 50_000..120_000),
                    independent(denominator = 600, roll = item(SlayerBossObjs.araxyteFang)),
                    independent(denominator = 200, roll = item(SlayerBossObjs.noxiousPoint)),
                    independent(denominator = 200, roll = item(SlayerBossObjs.noxiousBlade)),
                    independent(denominator = 200, roll = item(SlayerBossObjs.noxiousPommel)),
                    independent(denominator = 1_500, roll = item(SlayerBossObjs.jarOfVenom)),
                    independent(denominator = 3_000, roll = item(SlayerBossObjs.nid)),
                )
            ),
        )
        registry.register(
            SlayerBossNpcs.hydra,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 50_000..150_000),
                    independent(denominator = 1_000, roll = item(SlayerBossObjs.hydraClaw)),
                    independent(denominator = 512, roll = item(SlayerBossObjs.hydraLeather)),
                    independent(denominator = 512, roll = item(SlayerBossObjs.hydraTail)),
                    independent(denominator = 180, roll = item(SlayerBossObjs.hydraEye)),
                    independent(denominator = 180, roll = item(SlayerBossObjs.hydraHeart)),
                    independent(denominator = 180, roll = item(SlayerBossObjs.hydraFang)),
                    independent(denominator = 2_000, roll = item(SlayerBossObjs.jarOfChemicals)),
                    independent(denominator = 3_000, roll = item(SlayerBossObjs.hydraPet)),
                )
            ),
        )
    }
}
