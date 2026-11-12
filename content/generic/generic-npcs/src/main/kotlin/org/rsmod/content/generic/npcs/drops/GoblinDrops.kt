package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcChampionScrollDropRolls
import org.rsmod.api.death.NpcClueDropRolls
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcEnsouledHeadDropRolls
import org.rsmod.api.death.NpcHerbDropRolls
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.game.type.npc.NpcType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Exact final pre-cache-233-cutoff tables for every ordinary Goblin loaded in the world. */
public class GoblinDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val clueDrops: NpcClueDropRolls,
    private val questBones: RagAndBoneManIDropPolicy,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val goblinSkull =
            RagAndBoneManIDropPolicy.QuestBone(
                raw = objs.goblin_skull,
                inVinegar = objs.goblin_skull_in_vinegar,
                polished = objs.polished_goblin_skull,
                handedIndex = GOBLIN_SKULL_INDEX,
            )
        val levelTwoTable = tableOne(goblinSkull)
        tableOneTypes.forEach { registry.register(it, levelTwoTable) }
        registry.register(npcs.goblin_level_5, tableTwo(goblinSkull))
    }

    private fun tableOne(goblinSkull: RagAndBoneManIDropPolicy.QuestBone): NpcDropTable =
        NpcDropTable(
            sequence(
                item(objs.bones),
                weighted(
                    NpcWeightedDrop(3, item(objs.bronze_sq_shield)),
                    NpcWeightedDrop(4, members(item(objs.bronze_spear))),
                    NpcWeightedDrop(5, item(objs.body_rune, 7..7)),
                    NpcWeightedDrop(6, item(objs.water_rune, 6..6)),
                    NpcWeightedDrop(3, item(objs.earth_rune, 4..4)),
                    NpcWeightedDrop(3, item(objs.bronze_bolts, 8..8)),
                    NpcWeightedDrop(28, item(objs.coins, 5..5)),
                    NpcWeightedDrop(3, item(objs.coins, 9..9)),
                    NpcWeightedDrop(3, item(objs.coins, 15..15)),
                    NpcWeightedDrop(2, item(objs.coins, 20..20)),
                    NpcWeightedDrop(1, item(objs.coins)),
                    NpcWeightedDrop(38, nothing()),
                    NpcWeightedDrop(15, item(objs.hammer)),
                    NpcWeightedDrop(2, members(item(objs.goblin_book))),
                    NpcWeightedDrop(5, item(objs.goblin_mail)),
                    NpcWeightedDrop(3, item(objs.chefs_hat)),
                    NpcWeightedDrop(2, item(objs.beer)),
                    NpcWeightedDrop(1, item(objs.brass_necklace)),
                    NpcWeightedDrop(1, item(objs.air_talisman)),
                ),
                tertiaries(
                    goblinSkull,
                    ENSOULED_HEAD_TABLE_ONE_DENOMINATOR,
                    BEGINNER_TABLE_ONE_DENOMINATOR,
                ),
            )
        )

    private fun tableTwo(goblinSkull: RagAndBoneManIDropPolicy.QuestBone): NpcDropTable =
        NpcDropTable(
            sequence(
                item(objs.bones),
                weighted(
                    NpcWeightedDrop(3, item(objs.bronze_axe)),
                    NpcWeightedDrop(1, item(objs.bronze_scimitar)),
                    NpcWeightedDrop(9, members(item(objs.bronze_spear))),
                    NpcWeightedDrop(3, item(objs.bronze_arrow, 7..7)),
                    NpcWeightedDrop(3, item(objs.mind_rune, 2..2)),
                    NpcWeightedDrop(3, item(objs.earth_rune, 4..4)),
                    NpcWeightedDrop(3, item(objs.body_rune, 2..2)),
                    NpcWeightedDrop(2, members(item(objs.bronze_javelin, 5..5))),
                    NpcWeightedDrop(1, item(objs.chaos_rune)),
                    NpcWeightedDrop(1, item(objs.nature_rune)),
                    NpcWeightedDrop(
                        2,
                        sequence(
                            conditional({ it.hero.members }, NpcHerbDropRolls.standard()),
                            conditional({ !it.hero.members }, item(objs.coins, 10..10)),
                        ),
                    ),
                    NpcWeightedDrop(34, item(objs.coins)),
                    NpcWeightedDrop(13, item(objs.coins, 3..3)),
                    NpcWeightedDrop(8, item(objs.coins, 5..5)),
                    NpcWeightedDrop(7, item(objs.coins, 16..16)),
                    NpcWeightedDrop(3, item(objs.coins, 24..24)),
                    NpcWeightedDrop(8, nothing()),
                    NpcWeightedDrop(9, item(objs.hammer)),
                    NpcWeightedDrop(2, members(item(objs.goblin_book))),
                    NpcWeightedDrop(10, item(objs.green_goblin_mail)),
                    NpcWeightedDrop(1, item(objs.grapes)),
                    NpcWeightedDrop(1, item(objs.red_cape)),
                    NpcWeightedDrop(1, item(objs.tin_ore)),
                ),
                tertiaries(
                    goblinSkull,
                    ENSOULED_HEAD_TABLE_TWO_DENOMINATOR,
                    BEGINNER_TABLE_TWO_DENOMINATOR,
                ),
            )
        )

    private fun tertiaries(
        goblinSkull: RagAndBoneManIDropPolicy.QuestBone,
        ensouledHeadDenominator: Int,
        beginnerDenominator: Int,
    ) =
        sequence(
            conditional(
                { questBones.isNeeded(it.hero, goblinSkull) },
                org.rsmod.api.death.NpcDropRolls.independent(
                    denominator = QUEST_SKULL_DENOMINATOR,
                    roll = item(goblinSkull.raw),
                ),
            ),
            conditional(
                { it.hero.members },
                NpcEnsouledHeadDropRolls.independent(
                    objs.ensouled_goblin_head_initial,
                    ensouledHeadDenominator,
                ),
            ),
            clueDrops.independent(
                tier = ClueTier.Beginner,
                clueScroll = objs.clue_scroll_beginner,
                denominator = beginnerDenominator,
            ),
            clueDrops.independentEasy(denominator = EASY_CLUE_DENOMINATOR),
            NpcChampionScrollDropRolls.independent(
                type = objs.goblin_champion_scroll,
                questPoints = varps.quest_points,
                completed = varbits.champions_defeated_goblin,
            ),
        )

    private fun members(roll: org.rsmod.api.death.NpcDropRoll) =
        conditional({ it.hero.members }, roll)

    public companion object {
        /** Quest Helper's cutoff eight-bone list places Goblin fifth, at zero-based bit four. */
        public const val GOBLIN_SKULL_INDEX: Int = 4
        public const val QUEST_SKULL_DENOMINATOR: Int = 4
        public const val ENSOULED_HEAD_TABLE_ONE_DENOMINATOR: Int = 35
        public const val ENSOULED_HEAD_TABLE_TWO_DENOMINATOR: Int = 30
        public const val BEGINNER_TABLE_ONE_DENOMINATOR: Int = 64
        public const val BEGINNER_TABLE_TWO_DENOMINATOR: Int = 80
        public const val EASY_CLUE_DENOMINATOR: Int = 128

        public val tableOneTypes: Set<NpcType> =
            setOf(
                npcs.goblin_unarmed_1,
                npcs.goblin_unarmed_2,
                npcs.goblin_unarmed_3,
                npcs.goblin_unarmed_4,
                npcs.goblin_unarmed_5,
                npcs.goblin_unarmed_6,
                npcs.goblin_unarmed_7,
                npcs.goblin_unarmed_8,
            )
    }
}
