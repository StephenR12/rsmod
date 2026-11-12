package org.rsmod.api.cache.delta

public enum class CacheUpgradePriority {
    Critical,
    High,
    Normal,
}

public data class CacheUpgradeBacklogSummary(
    public val total: Int,
    public val critical: Int,
    public val high: Int,
    public val normal: Int,
    public val added: Int,
    public val removed: Int,
    public val changed: Int,
)

public data class CacheUpgradeTask(
    public val id: String,
    public val title: String,
    public val domain: String,
    public val key: String,
    public val kind: CacheDeltaKind,
    public val priority: CacheUpgradePriority,
    public val symbolBefore: String? = null,
    public val symbolAfter: String? = null,
    public val fingerprintBefore: String? = null,
    public val fingerprintAfter: String? = null,
    public val changedFields: List<String>,
    public val riskTags: List<String>,
    public val evidenceRequirements: List<String>,
    public val acceptanceCriteria: List<String>,
)

public data class CacheUpgradeBacklog(
    public val schemaVersion: Int = SCHEMA_VERSION,
    public val baseline: CacheManifestReference,
    public val target: CacheManifestReference,
    public val summary: CacheUpgradeBacklogSummary,
    public val tasks: List<CacheUpgradeTask>,
) {
    public companion object {
        public const val SCHEMA_VERSION: Int = 1
    }
}
