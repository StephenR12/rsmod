package org.rsmod.content.other.combatachievements

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.type.struct.StructTypeList

/** Reads the authoritative task catalogue from the active cache revision. */
@Singleton
public class CombatAchievementCatalogue @Inject constructor(structs: StructTypeList) {
    public val tasks: List<CombatAchievement> =
        structs.values
            .mapNotNull { struct ->
                val params = struct.paramMap?.primitiveMap ?: return@mapNotNull null
                val id = (params[TASK_ID] as? Number)?.toInt() ?: return@mapNotNull null
                val name = params[TASK_NAME] as? String ?: return@mapNotNull null
                val description = params[TASK_DESCRIPTION] as? String ?: return@mapNotNull null
                val tierId = (params[TASK_TIER] as? Number)?.toInt() ?: return@mapNotNull null
                val tier = CombatAchievementTier.fromCacheId(tierId) ?: return@mapNotNull null
                val type = (params[TASK_TYPE] as? Number)?.toInt() ?: 0
                val boss = (params[TASK_BOSS] as? Number)?.toInt() ?: 0
                CombatAchievement(id, name, description, tier, type, boss)
            }
            .sortedBy(CombatAchievement::id)

    public val byId: Map<Int, CombatAchievement> = tasks.associateBy(CombatAchievement::id)

    public fun find(query: String): CombatAchievement? {
        query.toIntOrNull()?.let {
            return byId[it]
        }
        return tasks.firstOrNull { it.name.equals(query, ignoreCase = true) }
            ?: tasks.firstOrNull { it.name.contains(query, ignoreCase = true) }
    }

    public fun threshold(tier: CombatAchievementTier): Int =
        tasks.asSequence().filter { it.tier.cacheId <= tier.cacheId }.sumOf { it.tier.points }

    private companion object {
        private const val TASK_ID = 1306
        private const val TASK_NAME = 1308
        private const val TASK_DESCRIPTION = 1309
        private const val TASK_TIER = 1310
        private const val TASK_TYPE = 1311
        private const val TASK_BOSS = 1312
    }
}
