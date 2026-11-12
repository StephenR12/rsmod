package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropRoll
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropRolls.repeated
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcGemDropObjs
import org.rsmod.api.death.NpcGemDropRolls
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ZulrahDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val table = ZulrahDropTable.create()
        ZulrahNpcs.bosses.forEach { registry.register(it, table) }
    }
}

/** Cache-233 Zulrah table, including its nested flax, shark, gem and mega-rare tables. */
public object ZulrahDropTable {
    public const val COMMON_WEIGHT: Int = 249
    public const val SHARK_WEIGHT: Int = 8
    public const val MUTAGEN_WEIGHT: Int = 5_264
    public const val RARE_WEIGHT: Int = 128
    public const val LEGENDS_QUEST_COMPLETE: Int = 75

    public val sharkEntries: List<NpcWeightedDrop>
        get() =
            listOf(
                NpcWeightedDrop(3, item(ZulrahObjs.notedRawShark, 35..35)),
                NpcWeightedDrop(3, item(ZulrahObjs.sharkLure, 70..70)),
                NpcWeightedDrop(2, item(ZulrahObjs.notedMantaRay, 35..35)),
            )

    public val mutagenEntries: List<NpcWeightedDrop>
        get() =
            listOf(
                NpcWeightedDrop(5_244, item(ZulrahObjs.notedFlax, 1_000..1_000)),
                NpcWeightedDrop(10, item(ZulrahObjs.tanzaniteMutagen)),
                NpcWeightedDrop(10, item(ZulrahObjs.magmaMutagen)),
            )

    public val rareEntries: List<NpcWeightedDrop>
        get() =
            listOf(
                NpcWeightedDrop(21, item(objs.coins, 3_000..3_000)),
                NpcWeightedDrop(20, item(NpcGemDropObjs.loopHalfOfKey)),
                NpcWeightedDrop(20, item(NpcGemDropObjs.toothHalfOfKey)),
                NpcWeightedDrop(5, item(objs.runite_bar)),
                NpcWeightedDrop(3, item(objs.nature_rune, 67..67)),
                NpcWeightedDrop(3, item(ZulrahObjs.runeTwoHander)),
                NpcWeightedDrop(3, item(ZulrahObjs.runeBattleaxe)),
                NpcWeightedDrop(2, item(objs.adamant_javelin, 20..20)),
                NpcWeightedDrop(2, item(objs.death_rune, 45..45)),
                NpcWeightedDrop(2, item(objs.law_rune, 45..45)),
                NpcWeightedDrop(2, item(objs.rune_arrow, 42..42)),
                NpcWeightedDrop(2, item(objs.steel_arrow, 150..150)),
                NpcWeightedDrop(2, item(ZulrahObjs.runeSqShield)),
                NpcWeightedDrop(2, item(objs.dragonstone)),
                NpcWeightedDrop(2, item(ZulrahObjs.notedSilverOre, 100..100)),
                NpcWeightedDrop(20, gemTable()),
                NpcWeightedDrop(15, megaRareTable()),
                NpcWeightedDrop(1, item(objs.rune_kiteshield)),
                NpcWeightedDrop(1, item(ZulrahObjs.dragonMedHelm)),
            )

    public val commonEntries: List<NpcWeightedDrop>
        get() =
            listOf(
                NpcWeightedDrop(12, item(ZulrahObjs.deathRune, 250..250)),
                NpcWeightedDrop(12, item(ZulrahObjs.lawRune, 200..200)),
                NpcWeightedDrop(12, item(ZulrahObjs.chaosRune, 400..400)),
                NpcWeightedDrop(11, item(ZulrahObjs.notedSnakeskin, 35..35)),
                NpcWeightedDrop(11, item(ZulrahObjs.notedRuniteOre, 2..2)),
                NpcWeightedDrop(10, item(ZulrahObjs.notedBattlestaff, 10..10)),
                NpcWeightedDrop(10, weighted(mutagenEntries)),
                NpcWeightedDrop(10, item(ZulrahObjs.notedPureEssence, 1_500..1_500)),
                NpcWeightedDrop(10, item(ZulrahObjs.notedYewLogs, 35..35)),
                NpcWeightedDrop(8, item(ZulrahObjs.notedAdamantiteBars, 20..20)),
                NpcWeightedDrop(8, item(ZulrahObjs.notedCoal, 200..200)),
                NpcWeightedDrop(8, item(ZulrahObjs.notedDragonBones, 12..12)),
                NpcWeightedDrop(8, item(ZulrahObjs.notedMahoganyLogs, 50..50)),
                NpcWeightedDrop(2, item(ZulrahObjs.notedSnapdragon, 10..10)),
                NpcWeightedDrop(2, item(ZulrahObjs.notedDwarfWeed, 30..30)),
                NpcWeightedDrop(2, item(ZulrahObjs.notedToadflax, 25..25)),
                NpcWeightedDrop(2, item(ZulrahObjs.notedTorstol, 10..10)),
                NpcWeightedDrop(6, item(ZulrahObjs.palmSeed)),
                NpcWeightedDrop(6, item(ZulrahObjs.papayaSeed, 3..3)),
                NpcWeightedDrop(6, item(ZulrahObjs.calquatSeed, 2..2)),
                NpcWeightedDrop(4, item(ZulrahObjs.magicSeed)),
                NpcWeightedDrop(2, item(ZulrahObjs.toadflaxSeed, 2..2)),
                NpcWeightedDrop(2, item(ZulrahObjs.snapdragonSeed)),
                NpcWeightedDrop(2, item(ZulrahObjs.dwarfWeedSeed, 2..2)),
                NpcWeightedDrop(2, item(ZulrahObjs.torstolSeed)),
                NpcWeightedDrop(1, item(ZulrahObjs.spiritSeed)),
                NpcWeightedDrop(15, item(ZulrahObjs.zulAndraTeleport, 4..4)),
                NpcWeightedDrop(12, weighted(sharkEntries)),
                NpcWeightedDrop(9, item(ZulrahObjs.notedAntidotePlusPlus, 10..10)),
                NpcWeightedDrop(8, item(ZulrahObjs.dragonstoneBoltTips, 12..12)),
                NpcWeightedDrop(6, item(ZulrahObjs.notedGrapes, 250..250)),
                NpcWeightedDrop(6, item(ZulrahObjs.notedCoconut, 20..20)),
                NpcWeightedDrop(5, item(ZulrahObjs.swampTar, 1_000..1_000)),
                NpcWeightedDrop(5, item(ZulrahObjs.scales, 500..500)),
                NpcWeightedDrop(10, weighted(rareEntries)),
                NpcWeightedDrop(2, item(ZulrahObjs.dragonMedHelm)),
                NpcWeightedDrop(2, item(ZulrahObjs.dragonHalberd)),
            )

    public fun create(): NpcDropTable {
        check(commonEntries.sumOf(NpcWeightedDrop::weight) == COMMON_WEIGHT)
        check(sharkEntries.sumOf(NpcWeightedDrop::weight) == SHARK_WEIGHT)
        check(mutagenEntries.sumOf(NpcWeightedDrop::weight) == MUTAGEN_WEIGHT)
        check(rareEntries.sumOf(NpcWeightedDrop::weight) == RARE_WEIGHT)
        val mainRoll =
            weighted(
                NpcWeightedDrop(1, item(ZulrahObjs.tanzaniteFang)),
                NpcWeightedDrop(1, item(ZulrahObjs.magicFang)),
                NpcWeightedDrop(1, item(ZulrahObjs.serpentineVisage)),
                NpcWeightedDrop(1, item(ZulrahObjs.uncutOnyx)),
                NpcWeightedDrop(1_020, weighted(commonEntries)),
            )
        return NpcDropTable(
            sequence(
                item(ZulrahObjs.scales, 100..299),
                repeated(2, mainRoll),
                independent(denominator = 3_000, roll = item(ZulrahObjs.jarOfSwamp)),
                independent(denominator = 4_000, roll = item(ZulrahObjs.petSnakeling)),
            )
        )
    }

    private fun gemTable(): NpcDropRoll =
        NpcGemDropRolls.standard(
            talisman = ZulrahObjs.natureTalisman,
            hasRingOfWealth = ::hasRingOfWealth,
            legendsQuestComplete = { it.hero.vars[varps.legends_quest] >= LEGENDS_QUEST_COMPLETE },
        )

    private fun megaRareTable(): NpcDropRoll {
        val ordinary =
            weighted(
                NpcWeightedDrop(113, nothing()),
                NpcWeightedDrop(8, item(NpcGemDropObjs.runeSpear)),
                NpcWeightedDrop(4, item(NpcGemDropObjs.shieldLeftHalf)),
                NpcWeightedDrop(3, item(NpcGemDropObjs.dragonSpear)),
            )
        val lucky =
            weighted(
                NpcWeightedDrop(8, item(NpcGemDropObjs.runeSpear)),
                NpcWeightedDrop(4, item(NpcGemDropObjs.shieldLeftHalf)),
                NpcWeightedDrop(3, item(NpcGemDropObjs.dragonSpear)),
            )
        return NpcDropRoll { context ->
            (if (hasRingOfWealth(context)) lucky else ordinary).roll(context)
        }
    }

    private fun hasRingOfWealth(context: NpcDropContext): Boolean =
        ringsOfWealth.any(context.hero.worn::contains)

    private val ringsOfWealth: List<ObjType>
        get() =
            listOf(
                objs.ring_of_wealth,
                ZulrahObjs.ringOfWealth1,
                ZulrahObjs.ringOfWealth2,
                ZulrahObjs.ringOfWealth3,
                ZulrahObjs.ringOfWealth4,
                ZulrahObjs.ringOfWealth5,
                ZulrahObjs.ringOfWealthI,
                ZulrahObjs.ringOfWealthI1,
                ZulrahObjs.ringOfWealthI2,
                ZulrahObjs.ringOfWealthI3,
                ZulrahObjs.ringOfWealthI4,
                ZulrahObjs.ringOfWealthI5,
            )
}
