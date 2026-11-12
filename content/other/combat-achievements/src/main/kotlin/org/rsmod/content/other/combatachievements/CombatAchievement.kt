package org.rsmod.content.other.combatachievements

public data class CombatAchievement(
    public val id: Int,
    public val name: String,
    public val description: String,
    public val tier: CombatAchievementTier,
    public val type: Int,
    public val boss: Int,
)

public enum class CombatAchievementTier(public val cacheId: Int, public val points: Int) {
    Easy(1, 1),
    Medium(2, 2),
    Hard(3, 3),
    Elite(4, 4),
    Master(5, 5),
    Grandmaster(6, 6);

    public val cacheName: String
        get() = name.lowercase()

    public companion object {
        public fun fromCacheId(id: Int): CombatAchievementTier? =
            entries.firstOrNull { it.cacheId == id }
    }
}
