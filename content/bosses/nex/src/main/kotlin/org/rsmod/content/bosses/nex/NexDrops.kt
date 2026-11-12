package org.rsmod.content.bosses.nex

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRoll
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.repeated
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class NexDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        NexNpcs.forms.forEach { registry.register(it, NexDropTable.create()) }
    }
}

public object NexDropTable {
    public const val UNIQUE_DENOMINATOR: Int = 43
    public const val UNIQUE_SUBTABLE_WEIGHT: Int = 12
    public const val PET_DENOMINATOR: Int = 500
    public const val COMMON_ROLLS: Int = 2

    public val uniqueEntries: List<NpcWeightedDrop>
        get() =
            listOf(
                NpcWeightedDrop(2, item(NexObjs.torvaHelmDamaged)),
                NpcWeightedDrop(2, item(NexObjs.torvaPlatebodyDamaged)),
                NpcWeightedDrop(2, item(NexObjs.torvaPlatelegsDamaged)),
                NpcWeightedDrop(2, item(NexObjs.nihilHorn)),
                NpcWeightedDrop(3, item(NexObjs.zaryteVambraces)),
                NpcWeightedDrop(1, item(NexObjs.ancientHilt)),
            )

    public fun create(): NpcDropTable {
        check(uniqueEntries.sumOf(NpcWeightedDrop::weight) == UNIQUE_SUBTABLE_WEIGHT)
        return NpcDropTable(
            sequence(
                item(NexObjs.bigBones),
                item(NexObjs.nihilShard, 5..25),
                repeated(COMMON_ROLLS, commonTable()),
                independent(denominator = UNIQUE_DENOMINATOR, roll = weighted(uniqueEntries)),
                independent(denominator = PET_DENOMINATOR, roll = item(NexObjs.nexling)),
            )
        )
    }

    private fun commonTable(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(12, item(objs.blood_rune, 250..500)),
            NpcWeightedDrop(10, item(objs.death_rune, 250..500)),
            NpcWeightedDrop(8, item(objs.soul_rune, 250..500)),
            NpcWeightedDrop(10, item(NexObjs.dragonBoltsUnfinished, 80..120)),
            NpcWeightedDrop(8, item(NexObjs.runiteOreNoted, 12..20)),
            NpcWeightedDrop(8, item(NexObjs.coalNoted, 80..120)),
            NpcWeightedDrop(8, item(NexObjs.uncutRubyNoted, 10..20)),
            NpcWeightedDrop(8, item(NexObjs.uncutDiamondNoted, 10..20)),
            NpcWeightedDrop(8, item(NexObjs.rawSharkNoted, 25..35)),
            NpcWeightedDrop(8, item(NexObjs.rawMantaRayNoted, 25..35)),
            NpcWeightedDrop(6, item(NexObjs.superRestore4, 3..6)),
            NpcWeightedDrop(6, item(NexObjs.saradominBrew4, 3..6)),
            NpcWeightedDrop(2, item(NexObjs.bloodEssence, 1..2)),
        )
}
