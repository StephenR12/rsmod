package org.rsmod.content.bosses.godwars

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

public class GodWarsDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(GodWarsNpcs.graardor, GodWarsDropTables.graardor())
        registry.register(GodWarsNpcs.kree, GodWarsDropTables.kree())
        registry.register(GodWarsNpcs.zilyana, GodWarsDropTables.zilyana())
        registry.register(GodWarsNpcs.kril, GodWarsDropTables.kril())
    }
}

/** Exact general-unique denominators on a common 1,524-slot roll. */
public object GodWarsDropTables {
    public const val UNIQUE_DENOMINATOR: Int = 1_524
    public const val PET_DENOMINATOR: Int = 5_000

    public fun graardor(): NpcDropTable =
        table(
            always = item(GodWarsObjs.bigBones),
            pet = GodWarsObjs.petGraardor,
            uniques =
                listOf(
                    unique(4, GodWarsObjs.bandosChestplate),
                    unique(4, GodWarsObjs.bandosTassets),
                    unique(4, GodWarsObjs.bandosBoots),
                    unique(3, GodWarsObjs.bandosHilt),
                    unique(2, GodWarsObjs.shard1),
                    unique(2, GodWarsObjs.shard2),
                    unique(2, GodWarsObjs.shard3),
                ),
            common = graardorCommon(),
        )

    public fun kree(): NpcDropTable =
        table(
            always = sequence(item(GodWarsObjs.bigBones), item(GodWarsObjs.feather, 1..16)),
            pet = GodWarsObjs.petKree,
            uniques =
                listOf(
                    unique(4, GodWarsObjs.armadylHelmet),
                    unique(4, GodWarsObjs.armadylChestplate),
                    unique(4, GodWarsObjs.armadylChainskirt),
                    unique(3, GodWarsObjs.armadylHilt),
                    unique(2, GodWarsObjs.shard1),
                    unique(2, GodWarsObjs.shard2),
                    unique(2, GodWarsObjs.shard3),
                ),
            common = kreeCommon(),
        )

    public fun zilyana(): NpcDropTable =
        table(
            always = item(GodWarsObjs.bigBones),
            pet = GodWarsObjs.petZilyana,
            uniques =
                listOf(
                    unique(12, GodWarsObjs.saradominSword),
                    unique(6, GodWarsObjs.saradominLight),
                    unique(3, GodWarsObjs.armadylCrossbow),
                    unique(3, GodWarsObjs.saradominHilt),
                    unique(2, GodWarsObjs.shard1),
                    unique(2, GodWarsObjs.shard2),
                    unique(2, GodWarsObjs.shard3),
                ),
            common = zilyanaCommon(),
        )

    public fun kril(): NpcDropTable =
        table(
            always = item(GodWarsObjs.infernalAshes),
            pet = GodWarsObjs.petKril,
            uniques =
                listOf(
                    unique(12, GodWarsObjs.steamBattlestaff),
                    unique(12, GodWarsObjs.zamorakianSpear),
                    unique(3, GodWarsObjs.staffOfTheDead),
                    unique(3, GodWarsObjs.zamorakHilt),
                    unique(2, GodWarsObjs.shard1),
                    unique(2, GodWarsObjs.shard2),
                    unique(2, GodWarsObjs.shard3),
                ),
            common = krilCommon(),
        )

    private fun table(
        always: NpcDropRoll,
        pet: ObjType,
        uniques: List<NpcWeightedDrop>,
        common: NpcDropRoll,
    ): NpcDropTable {
        val uniqueWeight = uniques.sumOf(NpcWeightedDrop::weight)
        check(uniqueWeight < UNIQUE_DENOMINATOR)
        val main = weighted(uniques + NpcWeightedDrop(UNIQUE_DENOMINATOR - uniqueWeight, common))
        return NpcDropTable(
            sequence(always, main, independent(denominator = PET_DENOMINATOR, roll = item(pet)))
        )
    }

    private fun unique(weight: Int, obj: ObjType): NpcWeightedDrop =
        NpcWeightedDrop(weight, item(obj))

    private fun graardorCommon(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(8, item(GodWarsObjs.runeLongsword)),
            NpcWeightedDrop(8, item(GodWarsObjs.runeTwoHander)),
            NpcWeightedDrop(8, item(GodWarsObjs.runePlatebody)),
            NpcWeightedDrop(8, item(GodWarsObjs.grimySnapdragonNoted, 3..3)),
            NpcWeightedDrop(8, item(GodWarsObjs.snapdragonSeed)),
            NpcWeightedDrop(8, item(GodWarsObjs.superRestore4, 3..3)),
            NpcWeightedDrop(8, item(GodWarsObjs.adamantiteOreNoted, 15..20)),
            NpcWeightedDrop(8, item(GodWarsObjs.coalNoted, 115..120)),
            NpcWeightedDrop(8, item(GodWarsObjs.magicLogsNoted, 15..20)),
            NpcWeightedDrop(8, item(objs.nature_rune, 65..70)),
            NpcWeightedDrop(47, item(objs.coins, 19_500..20_600)),
        )

    private fun kreeCommon(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(8, item(GodWarsObjs.blackDhideBody)),
            NpcWeightedDrop(8, item(GodWarsObjs.runeCrossbow, 8..8)),
            NpcWeightedDrop(8, item(objs.mind_rune, 586..601)),
            NpcWeightedDrop(8, item(objs.rune_arrow, 100..105)),
            NpcWeightedDrop(8, item(objs.runite_bolts, 20..25)),
            NpcWeightedDrop(8, item(GodWarsObjs.grimyDwarfWeedNoted, 8..13)),
            NpcWeightedDrop(8, item(GodWarsObjs.dwarfWeedSeed, 2..2)),
            NpcWeightedDrop(
                8,
                sequence(
                    item(GodWarsObjs.saradominBrew3, 3..3),
                    item(GodWarsObjs.superDefence3, 3..3),
                ),
            ),
            NpcWeightedDrop(63, item(objs.coins, 19_500..21_000)),
        )

    private fun zilyanaCommon(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(10, item(GodWarsObjs.runeSword)),
            NpcWeightedDrop(10, item(GodWarsObjs.runePlatebody)),
            NpcWeightedDrop(8, item(GodWarsObjs.prayerPotion4, 3..3)),
            NpcWeightedDrop(
                6,
                sequence(
                    item(GodWarsObjs.saradominBrew3, 3..3),
                    item(GodWarsObjs.superRestore4, 3..3),
                ),
            ),
            NpcWeightedDrop(8, item(GodWarsObjs.magicLogsNoted, 20..25)),
            NpcWeightedDrop(8, item(GodWarsObjs.grimySnapdragonNoted, 5..5)),
            NpcWeightedDrop(8, item(GodWarsObjs.snapdragonSeed)),
            NpcWeightedDrop(69, item(objs.coins, 19_500..21_000)),
        )

    private fun krilCommon(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(8, item(GodWarsObjs.runeSword)),
            NpcWeightedDrop(8, item(GodWarsObjs.dragonLongsword)),
            NpcWeightedDrop(8, item(GodWarsObjs.dragonDagger)),
            NpcWeightedDrop(8, item(objs.death_rune, 120..124)),
            NpcWeightedDrop(8, item(objs.blood_rune, 80..85)),
            NpcWeightedDrop(8, item(GodWarsObjs.grimySnapdragonNoted, 3..3)),
            NpcWeightedDrop(8, item(GodWarsObjs.superRestore4, 3..3)),
            NpcWeightedDrop(71, item(objs.coins, 19_500..21_000)),
        )
}
