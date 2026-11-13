package org.rsmod.content.skills.hunter.experts

/**
 * Six columns of the October 2025 assignment table in Gilman, Cervus, Ornus, Aco, Teco, Wolf order.
 */
internal data class Rumour(
    val name: String,
    val species: String,
    val level: Int,
    val hunters: Int,
    val part: Int,
    val rate: Int,
)

internal object RumourCatalogue {
    val rumours =
        listOf(
            Rumour("Tropical wagtail", "multicoloured_bird", 19, 1, 29222, 20),
            Rumour("Wild kebbit", "huntingbeast_claws", 23, 1, 29223, 15),
            Rumour("Sapphire glacialis", "butterfly_glacialis", 25, 1, 29224, 75),
            Rumour("Swamp lizard", "salamander_green", 29, 3, 29225, 25),
            Rumour("Spined larupia", "hunting_jaguar", 31, 5, 29226, 15),
            Rumour("Barb-tailed kebbit", "huntingbeast_barbedtail", 33, 1, 29223, 15),
            Rumour("Snowy knight", "butterfly_snowy", 35, 5, 29227, 75),
            Rumour("Prickly kebbit", "huntingbeast_spiky", 37, 1, 29223, 15),
            Rumour("Embertailed jerboa", "varlamore_hunterjerboa01", 39, 5, 29228, 50),
            Rumour("Horned graahk", "hunting_leopard", 41, 3, 29229, 15),
            Rumour("Spotted kebbit", "huntingbeast_speedy", 43, 7, 29223, 10),
            Rumour("Black warlock", "butterfly_warlock", 45, 3, 29230, 75),
            Rumour("Orange salamander", "salamander_orange", 47, 15, 29231, 25),
            Rumour("Razor-backed kebbit", "tracking_razor", 49, 3, 29223, 15),
            Rumour("Sabre-toothed kebbit", "huntingbeast_sabreteeth", 51, 31, 29223, 15),
            Rumour("Grey chinchompa", "hunting_chinchompa", 53, 27, 29221, 50),
            Rumour("Sabre-toothed kyatt", "hunting_snow_tiger", 55, 29, 29232, 15),
            Rumour("Dark kebbit", "huntingbeast_silent", 57, 27, 29223, 10),
            Rumour("Pyre fox", "varlamore_hunterfox01", 57, 7, 29233, 15),
            Rumour("Red salamander", "salamander_red", 59, 61, 29234, 25),
            Rumour("Red chinchompa", "hunting_chinchompa_big", 63, 63, 29235, 50),
            Rumour("Dashing kebbit", "huntingbeast_speedy2", 69, 57, 29223, 10),
            Rumour("Sunlight antelope", "sunlight_antelope", 72, 57, 29236, 15),
            Rumour("Sunlight Moth", "moth_sunlight", 75, 19, 29237, 75),
            Rumour("Tecu salamander", "salamander_mountain", 79, 41, 29238, 25),
            Rumour("Herbiboar", "fossil_herbiboar_visible", 80, 49, 29239, 7),
            Rumour("Moonlight moth", "moth_moonlight", 85, 41, 29240, 75),
            Rumour("Moonlight antelope", "moonlight_antelope", 91, 33, 29241, 15),
        )
}
