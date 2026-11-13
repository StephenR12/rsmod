package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.combat.commons.npc.inSlayerWilderness
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.config.refs.invs
import org.rsmod.api.death.*
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object HuntingDropBits : VarBitReferences() {
    val lavaNotes = find("cert_lava_d_bones")
    val mithrilNotes = find("slayer_unlock_notedmithrilbars")
    val monkeyMadness = find("mm2_progress")
}

public object HuntingDropVars : VarpReferences() {
    val legends = find("legendsquest")
}

public data class HuntingDropRow(
    val family: String,
    val weight: Int,
    val branch: String,
    val obj: Int,
    val minimum: Int,
    val maximum: Int,
)

public object HuntingGroundDropData {
    val rows: List<HuntingDropRow> =
        requireNotNull(javaClass.getResourceAsStream("hunting-ground-drops.tsv"))
            .bufferedReader()
            .useLines { lines ->
                lines
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { line ->
                        val p = line.split('\t')
                        HuntingDropRow(
                            p[0],
                            p[1].toInt(),
                            p[2],
                            p[3].toInt(),
                            p[4].toInt(),
                            p[5].toInt(),
                        )
                    }
                    .toList()
            }
}

/**
 * Ordinary main tables; generic ensouled green-dragon heads remain a separate tertiary provider.
 */
public class HuntingGroundDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val objects: ObjTypeList,
    private val rows: DbRowTypeList,
    private val clues: NpcClueDropRolls,
) : PluginScript() {
    private val grouped = HuntingGroundDropData.rows.groupBy { it.family }

    override fun ScriptContext.startup() {
        for (family in
            listOf(
                "lizard",
                "dust",
                "locust",
                "mithril",
                "green",
                "lava",
                "mogre",
                "rare",
            )) require(grouped.getValue(family).sumOf { it.weight } == 128) {
            "Invalid $family main table."
        }
        val lizardMain = NpcDropRolls.firstSelected(rare(512, 4115), table("lizard"))
        registry.register(
            HuntingGroundNpcs.smallLizards + HuntingGroundNpcs.desertLizards,
            NpcDropTable(NpcDropRolls.sequence(item(526), lizardMain)),
        )
        registry.register(
            HuntingGroundNpcs.largeLizards,
            NpcDropTable(
                NpcDropRolls.sequence(item(532), lizardMain, NpcRareBoneDropRolls.longOrCurved())
            ),
        )
        registry.register(
            HuntingGroundNpcs.dustDevils,
            NpcDropTable(
                NpcDropRolls.sequence(
                    item(526),
                    NpcDropRolls.firstSelected(rare(4000, 20736), table("dust")),
                )
            ),
        )
        registry.register(
            HuntingGroundNpcs.lancers + HuntingGroundNpcs.archers,
            NpcDropTable(NpcDropRolls.sequence(item(526), table("locust"))),
        )
        registry.register(
            HuntingGroundNpcs.mithrilDragons,
            NpcDropTable(
                NpcDropRolls.sequence(
                    item(536),
                    NpcDropRoll { c ->
                        val noted =
                            c.hero.vars[HuntingDropBits.mithrilNotes] != 0 &&
                                c.npc.isAssignedSlayerTarget(c.hero)
                        item(if (noted) objects.getValue(2359).certlink else 2359, 3..3).roll(c)
                    },
                    NpcDropRolls.firstSelected(rare(32768, 11335), table("mithril")),
                    rare(10000, 11286),
                    clue(ClueTier.Elite, 12073, 350),
                )
            ),
        )
        registry.register(
            HuntingGroundNpcs.greenDragons,
            NpcDropTable(
                NpcDropRolls.sequence(
                    bones(536, false),
                    item(1753),
                    table("green"),
                    clue(ClueTier.Hard, 2722, 128, true),
                    bag(),
                )
            ),
        )
        registry.register(
            HuntingGroundNpcs.lavaDragons,
            NpcDropTable(
                NpcDropRolls.sequence(
                    bones(11943, true),
                    item(1747),
                    item(11992),
                    table("lava"),
                    rare(10000, 11286),
                    clue(ClueTier.Elite, 12073, 250, true),
                    bag(),
                )
            ),
        )
        // Lava dragons are absent from the shared generic ensouled-head list.
        for (npc in HuntingGroundNpcs.lavaDragons) registry.registerTertiary(
            npc,
            NpcEnsouledHeadDropRolls.independent(objects.getValue(13510), 18),
        )
        registry.register(
            HuntingGroundNpcs.mogres,
            NpcDropTable(
                NpcDropRolls.sequence(
                    item(532),
                    table("mogre"),
                    NpcRareBoneDropRolls.longOrCurved(),
                )
            ),
        )
    }

    private fun item(id: Int, count: IntRange = 1..1) =
        NpcDropRolls.item(objects.getValue(id), count)

    private fun rare(rate: Int, id: Int) =
        NpcDropRolls.independent(denominator = rate, roll = item(id))

    private fun complete(player: Player, row: Int, state: Int) =
        state >= rows.getValue(row).data.getValue(19).single() as Int

    private fun monkey(player: Player) =
        complete(player, 96, player.vars[HuntingDropBits.monkeyMadness])

    private fun table(family: String): NpcDropRoll =
        NpcDropRolls.weighted(
            grouped.getValue(family).map { r -> NpcWeightedDrop(r.weight, branch(r)) }
        )

    private fun branch(r: HuntingDropRow): NpcDropRoll =
        when (r.branch) {
            "item" -> item(r.obj, r.minimum..r.maximum)
            "nothing" -> NpcDropRolls.nothing()
            "herb" -> NpcHerbDropRolls.standard()
            "doubleherb" ->
                NpcDropRolls.sequence(NpcHerbDropRolls.standard(), NpcHerbDropRolls.standard())
            "herb_fire" -> NpcDropRolls.sequence(NpcHerbDropRolls.standard(), item(554, 42..42))
            "seed_water" -> NpcDropRolls.sequence(table("general_seed"), item(1831, 2..2))
            "rareseed" -> table("rare_seed")
            "uncommonseed" -> table("uncommon_seed")
            "gem" ->
                NpcGemDropRolls.standard(objects.getValue(1462), NpcGemDropRolls::wearingWealth) {
                    complete(it.hero, 85, it.hero.vars[HuntingDropVars.legends])
                }
            "doublegem" ->
                NpcDropRolls.sequence(
                    branch(r.copy(branch = "gem")),
                    branch(r.copy(branch = "gem")),
                )
            "rare" -> table("rare")
            "mega" -> NpcGemDropRolls.megaRare(NpcGemDropRolls::wearingWealth)
            "dust_chain" -> NpcDropRolls.firstSelected(rare(256, 3140), NpcDropRolls.nothing())
            "mithril_pair" -> NpcDropRolls.sequence(item(1373), item(385))
            "mithril_mm2" ->
                NpcDropRoll { c ->
                    (if (monkey(c.hero)) item(19582, 15..15) else item(995, 876..876)).roll(c)
                }
            "lava_mm2" ->
                NpcDropRoll { c ->
                    (if (monkey(c.hero)) item(19582, 15..15) else item(995, 2960..2960)).roll(c)
                }
            "ancient_page" -> NpcDropRoll { c -> item(11341 + c.random.of(26)).roll(c) }
            else -> error("Unknown hunting drop branch: $r")
        }

    private fun bones(id: Int, lava: Boolean) = NpcDropRoll { c ->
        val elite = c.hero.vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Elite).complete] != 0
        val note =
            elite &&
                if (lava) c.hero.vars[HuntingDropBits.lavaNotes] != 0
                else c.npc.coords.inSlayerWilderness()
        item(if (note) objects.getValue(id).certlink else id).roll(c)
    }

    private fun imbuedWealth(player: Player) =
        player.worn.any { it?.id == 12785 || it?.id in 20786..20790 }

    private fun clue(tier: ClueTier, id: Int, denominator: Int, wealth: Boolean = false) =
        NpcDropRoll { c ->
            val boosted = wealth && c.npc.coords.inSlayerWilderness() && imbuedWealth(c.hero)
            clues
                .independent(
                    tier,
                    objects.getValue(id),
                    denominator = if (boosted) denominator / 2 else denominator,
                )
                .roll(c)
        }

    private fun bag() =
        NpcDropRolls.conditional(
            { c ->
                c.npc.coords.inSlayerWilderness() &&
                    c.hero.inv.none { it?.id in setOf(11941, 22586) } &&
                    c.hero.invMap[invs.bank]?.none { it?.id in setOf(11941, 22586) } != false
            },
            rare(3, 11941),
        )
}
