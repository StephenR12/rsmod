package org.rsmod.api.cache.delta

public object CacheDeltaEngine {
    public fun diff(baseline: CacheManifest, target: CacheManifest): CacheDeltaReport {
        val baselineEntries = CacheManifestNormalizer.index(baseline, "baseline")
        val targetEntries = CacheManifestNormalizer.index(target, "target")
        val keys = (baselineEntries.keys + targetEntries.keys).toSortedSet()
        val changes =
            keys.mapNotNull { key ->
                val before = baselineEntries[key]
                val after = targetEntries[key]
                val kind =
                    when {
                        before == null -> CacheDeltaKind.Added
                        after == null -> CacheDeltaKind.Removed
                        before != after -> CacheDeltaKind.Changed
                        else -> null
                    }
                kind?.let {
                    CacheDeltaEntry(
                        domain = key.domain,
                        key = key.key,
                        kind = it,
                        before = before,
                        after = after,
                        riskTags = classifyRisk(it, before, after),
                    )
                }
            }
        val added = changes.count { it.kind == CacheDeltaKind.Added }
        val removed = changes.count { it.kind == CacheDeltaKind.Removed }
        val changed = changes.count { it.kind == CacheDeltaKind.Changed }
        val unchanged = keys.size - added - removed - changed
        return CacheDeltaReport(
            baseline = baseline.reference(),
            target = target.reference(),
            summary = CacheDeltaSummary(added, removed, changed, unchanged),
            changes = changes,
        )
    }

    private fun classifyRisk(
        kind: CacheDeltaKind,
        before: CacheManifestEntry?,
        after: CacheManifestEntry?,
    ): List<String> {
        val current = after ?: before ?: error("A cache delta must have a before or after entry.")
        val tags = sortedSetOf<String>()
        tags += before?.riskTags.orEmpty()
        tags += after?.riskTags.orEmpty()
        if (kind != CacheDeltaKind.Added) {
            tags += "behavioral-risk"
        }
        if (before?.symbol != after?.symbol) {
            tags += "typed-symbol-migration"
        }
        if (before?.attributes?.get("morphs") != after?.attributes?.get("morphs")) {
            tags += "morph-change"
        }
        when (current.domain) {
            "locations",
            "maps" -> tags += "map-collision-risk"
            "interfaces",
            "client-scripts" -> tags += "client-contract-risk"
            "protocol",
            "vars" -> tags += "protocol-risk"
        }
        return tags.toList()
    }

    private fun CacheManifest.reference(): CacheManifestReference =
        CacheManifestReference(revision, archive, capturedAt)
}
