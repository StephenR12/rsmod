package org.rsmod.content.bosses.corporealbeast

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRoll
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class CorporealBeastDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            CorporealBeastNpcs.boss,
            NpcDropTable(
                sequence(
                    item(objs.coins, 20_000..50_000),
                    unique(CorporealBeastDropRates.SPIRIT_SHIELD, CorporealBeastObjs.spiritShield),
                    unique(CorporealBeastDropRates.HOLY_ELIXIR, CorporealBeastObjs.holyElixir),
                    sigilRoll(),
                    unique(CorporealBeastDropRates.JAR, CorporealBeastObjs.jarOfSpirits),
                    unique(CorporealBeastDropRates.PET, CorporealBeastObjs.pet),
                )
            ),
        )
    }

    private fun unique(denominator: Int, obj: ObjType): NpcDropRoll =
        independent(denominator = denominator, roll = item(obj))

    private fun sigilRoll(): NpcDropRoll =
        independent(
            denominator = CorporealBeastDropRates.SIGIL_TABLE,
            roll =
                weighted(
                    NpcWeightedDrop(3, item(CorporealBeastObjs.spectralSigil)),
                    NpcWeightedDrop(3, item(CorporealBeastObjs.arcaneSigil)),
                    NpcWeightedDrop(1, item(CorporealBeastObjs.elysianSigil)),
                ),
        )
}

public object CorporealBeastDropRates {
    public const val SPIRIT_SHIELD: Int = 64
    public const val HOLY_ELIXIR: Int = 171
    public const val SIGIL_TABLE: Int = 585
    public const val COMMON_SIGIL: Int = 1_365
    public const val ELYSIAN_SIGIL: Int = 4_095
    public const val JAR: Int = 1_000
    public const val PET: Int = 5_000
}
