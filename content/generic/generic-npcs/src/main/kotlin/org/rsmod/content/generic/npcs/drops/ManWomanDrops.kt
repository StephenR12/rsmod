package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
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

/** Exact final pre-cache-233-cutoff table for every ordinary Man and Woman loaded in the world. */
public class ManWomanDrops
@Inject
constructor(private val registry: NpcDropTableRegistry, private val clueDrops: NpcClueDropRolls) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val main = mainTable()
        val beginner =
            clueDrops.independent(
                tier = ClueTier.Beginner,
                clueScroll = objs.clue_scroll_beginner,
                denominator = BEGINNER_CLUE_DENOMINATOR,
            )
        val easy = clueDrops.independentEasy(denominator = EASY_CLUE_DENOMINATOR)
        val manTable =
            NpcDropTable(
                sequence(
                    main,
                    conditional(
                        {
                            it.hero.members && it.hero.inv.contains(objs.chicken_clue_scroll_medium)
                        },
                        item(objs.chicken_clue_key_medium),
                    ),
                    beginner,
                    easy,
                )
            )
        val womanTable = NpcDropTable(sequence(main, beginner, easy))
        registry.register(manTypes, manTable)
        registry.register(womanTypes, womanTable)
    }

    private fun mainTable(): NpcDropRoll =
        sequence(
            item(objs.bones),
            weighted(
                NpcWeightedDrop(2, item(objs.bronze_med_helm)),
                NpcWeightedDrop(1, item(objs.iron_dagger)),
                NpcWeightedDrop(22, members(item(objs.bronze_bolts, 2..12))),
                NpcWeightedDrop(3, item(objs.bronze_arrow, 7..7)),
                NpcWeightedDrop(2, item(objs.earth_rune, 4..4)),
                NpcWeightedDrop(2, item(objs.fire_rune, 6..6)),
                NpcWeightedDrop(2, item(objs.mind_rune, 9..9)),
                NpcWeightedDrop(1, item(objs.chaos_rune, 2..2)),
                NpcWeightedDrop(
                    23,
                    sequence(
                        conditional({ it.hero.members }, NpcHerbDropRolls.standard()),
                        conditional({ !it.hero.members }, item(objs.coins, 10..10)),
                    ),
                ),
                NpcWeightedDrop(38, item(objs.coins, 3..3)),
                NpcWeightedDrop(9, item(objs.coins, 5..5)),
                NpcWeightedDrop(4, item(objs.coins, 15..15)),
                NpcWeightedDrop(1, item(objs.coins, 25..25)),
                NpcWeightedDrop(8, nothing()),
                NpcWeightedDrop(5, item(objs.fishing_bait)),
                NpcWeightedDrop(2, item(objs.copper_ore)),
                NpcWeightedDrop(2, item(objs.earth_talisman)),
                NpcWeightedDrop(1, item(objs.cabbage)),
            ),
        )

    private fun members(roll: NpcDropRoll): NpcDropRoll = conditional({ it.hero.members }, roll)

    public companion object {
        public const val BEGINNER_CLUE_DENOMINATOR: Int = 90
        public const val EASY_CLUE_DENOMINATOR: Int = 128

        public val manTypes: Set<NpcType> = setOf(npcs.man, npcs.man2, npcs.man3, npcs.man_indoor)

        public val womanTypes: Set<NpcType> = setOf(npcs.woman, npcs.woman2, npcs.woman3)
    }
}
