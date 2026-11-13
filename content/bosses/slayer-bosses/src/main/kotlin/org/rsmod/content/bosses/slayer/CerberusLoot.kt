package org.rsmod.content.bosses.slayer

import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.*
import org.rsmod.api.type.refs.obj.ObjReferences

internal object CerberusLoot : ObjReferences() {
    val ashes = find("infernal_ashes")
    private val nature = find("nature_talisman")

    private fun entry(name: String, weight: Int, amount: IntRange = 1..1) =
        NpcWeightedDrop(weight, NpcDropRolls.item(find(name), amount))

    private val ordinary =
        listOf(
            entry("rune_platebody", 5),
            entry("rune_chainbody", 4),
            entry("rune_2h_sword", 4),
            entry("black_dragonhide_body", 3),
            entry("rune_axe", 3),
            entry("rune_pickaxe", 3),
            entry("cert_battlestaff", 3, 6..6),
            entry("rune_full_helm", 3),
            entry("lava_battlestaff", 2),
            entry("rune_halberd", 2),
            entry("firerune", 6, 300..300),
            entry("soulrune", 6, 100..100),
            entry("cert_blankrune_high", 5, 300..300),
            entry("bloodrune", 4, 60..60),
            entry("mcannonball", 4, 50..50),
            entry("xbows_crossbow_bolts_runite_unfeathered", 4, 40..40),
            entry("deathrune", 3, 100..100),
            entry("cert_coal", 6, 120..120),
            entry("4dose2restore", 6, 2..2),
            entry("summer_pie", 6, 3..3),
            entry("coins", 5, 10000..20000),
            entry("cert_dragon_bones", 5, 20..20),
            entry("blessedsnake", 5),
            entry("cert_wine_of_zamorak", 5, 15..15),
            entry("cert_ashes", 4, 50..50),
            entry("cert_fire_orb", 4, 20..20),
            entry("cert_unidentified_torstol", 4, 6..6),
            entry("cert_runite_ore", 3, 5..5),
            entry("cert_uncut_diamond", 3, 5..5),
            entry("torstol_seed", 2, 3..3),
            entry("ranarr_seed", 2, 2..2),
            entry("teleportscroll_cerberus", 2, 7..7),
        )
    private val rare =
        listOf(
            entry("naturerune", 3, 67..67),
            entry("adamant_javelin", 2, 20..20),
            entry("deathrune", 2, 45..45),
            entry("lawrune", 2, 45..45),
            entry("rune_arrow", 2, 42..42),
            entry("steel_arrow", 2, 150..150),
            entry("rune_2h_sword", 3),
            entry("rune_battleaxe", 3),
            entry("rune_sq_shield", 2),
            entry("dragon_med_helm", 1),
            entry("rune_kiteshield", 1),
            entry("coins", 21, 3000..3000),
            entry("keyhalf2", 20),
            entry("keyhalf1", 20),
            entry("runite_bar", 5),
            entry("dragonstone", 2),
            entry("cert_silver_ore", 2, 100..100),
        )

    fun table(): NpcDropRoll {
        val gems =
            NpcGemDropRolls.standard(nature, NpcGemDropRolls::wearingWealth) {
                it.hero.vars[varps.legends_quest] >= 75
            }
        val rareTable =
            NpcDropRolls.weighted(
                rare +
                    listOf(
                        NpcWeightedDrop(20, gems),
                        NpcWeightedDrop(
                            15,
                            NpcGemDropRolls.megaRare(NpcGemDropRolls::wearingWealth),
                        ),
                    )
            )
        val uniques =
            NpcDropRolls.weighted(
                listOf(
                        SlayerBossObjs.primordialCrystal,
                        SlayerBossObjs.pegasianCrystal,
                        SlayerBossObjs.eternalCrystal,
                        SlayerBossObjs.smoulderingStone,
                    )
                    .map { NpcWeightedDrop(1, NpcDropRolls.item(it)) }
            )
        return NpcDropRolls.weighted(
            ordinary + listOf(NpcWeightedDrop(1, uniques), NpcWeightedDrop(3, rareTable))
        )
    }
}
