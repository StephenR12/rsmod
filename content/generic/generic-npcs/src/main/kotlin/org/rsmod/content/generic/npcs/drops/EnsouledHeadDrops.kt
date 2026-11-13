package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcEnsouledHeadDropRolls
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object EnsouledHeadTypes : ObjReferences() {
    val heads =
        listOf(
                "monkey",
                "minotaur",
                "scorpion",
                "bear",
                "dog",
                "chaosdruid",
                "giant",
                "ogre",
                "elf",
                "troll",
                "horror",
                "kalphite",
                "dagannoth",
                "bloodveld",
                "tzhaar",
                "demon",
                "aviansie",
                "abyssal",
                "dragon",
                "hellhound",
            )
            .associateWith { find("arceuus_corpse_${it}_initial") }
}

public class EnsouledHeadDrops
@Inject
constructor(private val tables: NpcDropTableRegistry, private val types: NpcTypeList) :
    PluginScript() {
    override fun ScriptContext.startup() {
        fun head(variant: String, denominator: Int, vararg npcIds: Int) {
            val roll =
                org.rsmod.api.death.NpcDropRolls.conditional(
                    { it.hero.members },
                    NpcEnsouledHeadDropRolls.independent(
                        EnsouledHeadTypes.heads.getValue(variant),
                        denominator,
                    ),
                )
            for (id in npcIds) tables.registerTertiary(types.getValue(id), roll)
        }
        head("monkey", 35, 2848, 1038) // Monkey (monster)
        head("monkey", 35, 5275, 5276) // Monkey guard
        head("minotaur", 50, 2481, 2483) // Minotaur
        head("scorpion", 25, 3024, 13001, 2480, 5242, 2479) // Scorpion
        head("bear", 25, 2839) // Black bear
        head("bear", 25, 2838, 3423) // Grizzly bear
        head("dog", 25, 114, 7209) // Guard dog
        head("dog", 25, 112, 113) // Wild dog
        head("chaosdruid", 35, 520) // Chaos druid
        head("giant", 25, 2098, 2099, 2100, 2101, 2102, 2103, 7261) // Hill Giant
        head("giant", 20, 2075, 2076, 2077, 2078, 2079, 2080, 2081, 2082, 2083, 2084) // Fire giant
        head("giant", 21, 2085, 2086, 2087, 2088, 2089) // Ice giant
        head("giant", 24, 2090, 2091, 2092, 2093, 7262) // Moss giant
        head("ogre", 30, 136, 2095, 2096, 2233, 1153) // Ogre
        head("elf", 40, 5293, 5294) // Elf warrior
        head("troll", 45, 4123, 4124, 4125, 4126, 4127, 4128, 4129) // Troll spectator
        head("troll", 45, 936, 937, 938, 939, 940, 941, 942, 4143) // Mountain troll
        head("horror", 30, 1047, 1048, 1049, 1050, 1051) // Cave horror
        head("kalphite", 250, 955, 956, 961) // Kalphite Worker
        head("kalphite", 90, 957, 958) // Kalphite Soldier
        head("kalphite", 35, 959, 960, 962) // Kalphite Guardian
        head("dagannoth", 40, 970, 971, 972, 973, 974, 975, 7259, 7260) // Dagannoth
        head("bloodveld", 35, 484, 485, 486, 487, 3138) // Bloodveld
        head("tzhaar", 35, 2173, 2174, 2175, 2176, 2177, 2178, 2179, 2187) // TzHaar-Ket
        head("demon", 50, 2005, 2006, 2007, 2008, 2018, 7247, 7248) // Lesser demon
        head(
            "demon",
            40,
            2025,
            2026,
            2027,
            2028,
            2029,
            2030,
            2031,
            2032,
            7245,
            7244,
            7246,
        ) // Greater demon
        head("demon", 35, 240, 2048, 2049, 2050, 2051, 2052, 5874, 5875, 5876, 5877) // Black demon
        head(
            "aviansie",
            35,
            3169,
            3170,
            3171,
            3172,
            3173,
            3174,
            3175,
            3176,
            3177,
            3178,
            3179,
            3180,
            3181,
            3182,
            3183,
        ) // Aviansie
        head("abyssal", 25, 415, 416, 7241) // Abyssal demon
        head("dragon", 35, 260, 261, 262, 263, 264, 8073, 8076, 8082) // Green dragon
        head("dragon", 50, 265, 266, 267, 268, 269, 5878, 5879, 5880, 5881, 5882) // Blue dragon
        head("dragon", 40, 247, 248, 249, 250, 251, 8075, 8078, 8079) // Red dragon
        head("dragon", 35, 252, 253, 254, 255, 256, 257, 258, 259, 8084, 8085) // Black dragon
        head("hellhound", 40, 104, 105, 7256, 3133) // Hellhound
        head("hellhound", 15, 5862) // Cerberus
        head("bloodveld", 20, 7276) // Mutated Bloodveld
        head("dragon", 28, 2918) // Brutal green dragon
        head("dragon", 20, 7273, 13795) // Brutal blue dragon
        head("dragon", 20, 7274, 8087) // Brutal red dragon
        head("dragon", 20, 7275, 8092, 8093) // Brutal black dragon
        head("monkey", 35, 5272, 5273, 5274, 6813) // Monkey Archer
        head("tzhaar", 25, 7679) // Inner-city TzHaar-Ket
    }
}
