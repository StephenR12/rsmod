package org.rsmod.content.bosses.world

import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.*
import org.rsmod.api.type.refs.obj.ObjReferences

internal object SarachnisLoot : ObjReferences() {
    val egg = find("hosdun_egg_sac_full")
    val hardClue = find("trail_clue_hard_map001")
    val eliteClue = find("trail_clue_elite_music001")
    private val nature = find("nature_talisman")
    private val dragonHelm = find("dragon_med_helm")

    private fun entry(name: String, weight: Int, amount: IntRange = 1..1): NpcWeightedDrop =
        NpcWeightedDrop(weight, NpcDropRolls.item(find(name), amount))

    private val ordinary =
        listOf(
            entry("cert_battlestaff", 2, 8..10),
            entry("rune_platebody", 2),
            entry("rune_med_helm", 2),
            entry("rune_2h_sword", 2),
            entry("bloodrune", 5, 80..100),
            entry("chaosrune", 5, 175..200),
            entry("cosmicrune", 5, 125..150),
            entry("deathrune", 5, 80..100),
            entry("soulrune", 5, 80..100),
            entry("mithril_arrow", 2, 450..600),
            entry("xbows_crossbow_bolts_mithril", 2, 175..225),
            entry("maple_seed", 1, 2..2),
            entry("papaya_tree_seed", 1, 2..2),
            entry("yew_seed", 1),
            entry("cert_mithril_ore", 6, 60..90),
            entry("cert_dragonhide_red", 5, 15..25),
            entry("cert_uncut_sapphire", 4, 20..30),
            entry("cert_adamantite_ore", 3, 30..40),
            entry("cert_uncut_emerald", 3, 20..30),
            entry("xbows_bolt_tips_onyx", 2, 8..10),
            entry("cert_uncut_ruby", 2, 20..30),
            entry("cert_runite_ore", 1, 4..6),
            entry("cert_uncut_diamond", 1, 20..30),
            entry("coins", 6, 17000..25000),
            entry("cert_dragon_bones", 5, 10..15),
            entry("potato_egg+tomato", 5, 5..8),
            entry("cert_weapon_poison++", 2, 4..6),
            entry("crystal_key", 1),
            entry("cert_village_spider_carcass", 1, 10..10),
        )
    private val seeds =
        listOf(
            entry("toadflax_seed", 216, 3..3),
            entry("irit_seed", 148, 3..3),
            entry("belladonna_seed", 143, 3..3),
            entry("avantoe_seed", 103, 3..3),
            entry("poisonivy_bush_seed", 101, 3..3),
            entry("cactus_seed", 96, 3..3),
            entry("potato_cactus_seed", 70, 3..3),
            entry("kwuarm_seed", 69, 3..3),
            entry("snapdragon_seed", 46, 3..3),
            entry("cadantine_seed", 32, 3..3),
            entry("lantadyme_seed", 23, 3..3),
            entry("snape_grass_seed", 20, 9..9),
            entry("dwarf_weed_seed", 14, 3..3),
            entry("torstol_seed", 9, 3..3),
        )
    private val combatHerbs =
        listOf(
            entry("cert_unidentified_kwuarm", 5, 10..15),
            entry("cert_unidentified_dwarf_weed", 4, 10..15),
            entry("cert_unidentified_cadantine", 4, 10..15),
            entry("cert_unidentified_lantadyme", 3, 10..15),
        )
    private val usefulHerbs =
        listOf(
            entry("cert_unidentified_avantoe", 5, 5..10),
            entry("cert_unidentified_snapdragon", 4, 5..10),
            entry("cert_unidentified_ranarr", 4, 5..10),
            entry("cert_unidentified_torstol", 3, 5..10),
        )

    fun main(): NpcDropRoll {
        val gems =
            NpcGemDropRolls.standard(nature, NpcGemDropRolls::wearingWealth) {
                it.hero.vars[varps.legends_quest] >= 75
            }
        val normal =
            NpcDropRolls.weighted(
                ordinary +
                    listOf(
                        NpcWeightedDrop(2, NpcDropRolls.weighted(seeds)),
                        NpcWeightedDrop(6, NpcDropRolls.weighted(combatHerbs)),
                        NpcWeightedDrop(4, NpcDropRolls.weighted(usefulHerbs)),
                        NpcWeightedDrop(1, gems),
                    )
            )
        return NpcDropRolls.weighted(
            NpcWeightedDrop(2, NpcDropRolls.item(dragonHelm)),
            NpcWeightedDrop(1, NpcDropRolls.item(WorldBossObjs.sarachnisCudgel)),
            NpcWeightedDrop(381, normal),
        )
    }
}
