package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcAllotmentSeedDropRolls
import org.rsmod.api.death.NpcClueDropRolls
import org.rsmod.api.death.NpcDropRoll
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcHerbDropRolls
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.game.type.npc.NpcType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Exact final pre-cache-233-cutoff table for every ordinary Farmer loaded in the world. */
public class FarmerDrops
@Inject
constructor(private val registry: NpcDropTableRegistry, private val clueDrops: NpcClueDropRolls) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val table =
            NpcDropTable(
                sequence(
                    item(objs.bones),
                    conditional({ it.hero.members }, membersTable()),
                    conditional({ !it.hero.members }, freeTable()),
                    clueDrops.independent(
                        tier = ClueTier.Beginner,
                        clueScroll = objs.clue_scroll_beginner,
                        denominator = BEGINNER_CLUE_DENOMINATOR,
                    ),
                    clueDrops.independentEasy(denominator = EASY_CLUE_DENOMINATOR),
                )
            )
        registry.register(farmerTypes, table)
    }

    private fun membersTable(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(2, item(objs.earth_rune, 4..4)),
            NpcWeightedDrop(2, item(objs.fire_rune, 6..6)),
            NpcWeightedDrop(2, item(objs.mind_rune, 9..9)),
            NpcWeightedDrop(1, item(objs.chaos_rune, 2..2)),
            NpcWeightedDrop(11, NpcHerbDropRolls.standard()),
            NpcWeightedDrop(27, NpcAllotmentSeedDropRolls.standard()),
            NpcWeightedDrop(38, item(objs.coins, 3..3)),
            NpcWeightedDrop(1, item(objs.coins, 25..25)),
            NpcWeightedDrop(28, nothing()),
            NpcWeightedDrop(5, item(objs.fishing_bait)),
            NpcWeightedDrop(3, item(objs.rake)),
            NpcWeightedDrop(2, item(objs.earth_talisman)),
            NpcWeightedDrop(2, item(objs.gardening_boots)),
            NpcWeightedDrop(2, item(objs.seed_dibber)),
            NpcWeightedDrop(1, item(objs.secateurs)),
            NpcWeightedDrop(1, item(objs.watering_can_8)),
        )

    private fun freeTable(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(2, item(objs.bronze_med_helm)),
            NpcWeightedDrop(1, item(objs.iron_dagger)),
            NpcWeightedDrop(3, item(objs.bronze_arrow, 7..7)),
            NpcWeightedDrop(2, item(objs.earth_rune, 4..4)),
            NpcWeightedDrop(2, item(objs.fire_rune, 6..6)),
            NpcWeightedDrop(2, item(objs.mind_rune, 9..9)),
            NpcWeightedDrop(1, item(objs.chaos_rune, 2..2)),
            NpcWeightedDrop(38, item(objs.coins, 3..3)),
            NpcWeightedDrop(23, item(objs.coins, 10..10)),
            NpcWeightedDrop(9, item(objs.coins, 5..5)),
            NpcWeightedDrop(4, item(objs.coins, 15..15)),
            NpcWeightedDrop(1, item(objs.coins, 25..25)),
            NpcWeightedDrop(30, nothing()),
            NpcWeightedDrop(5, item(objs.fishing_bait)),
            NpcWeightedDrop(2, item(objs.copper_ore)),
            NpcWeightedDrop(2, item(objs.earth_talisman)),
            NpcWeightedDrop(1, item(objs.cabbage)),
        )

    public companion object {
        public const val BEGINNER_CLUE_DENOMINATOR: Int = 90
        public const val EASY_CLUE_DENOMINATOR: Int = 128

        public val farmerTypes: Set<NpcType> =
            setOf(npcs.farmer, npcs.farmer_3, npcs.farmer_2_f, npcs.farmer_3_f, npcs.farmer_4)
    }
}
