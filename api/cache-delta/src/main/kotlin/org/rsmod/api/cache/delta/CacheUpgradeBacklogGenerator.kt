package org.rsmod.api.cache.delta

import java.security.MessageDigest

public object CacheUpgradeBacklogGenerator {
    private val criticalRiskTags =
        setOf("client-contract-risk", "map-collision-risk", "protocol-risk")

    public fun generate(report: CacheDeltaReport): CacheUpgradeBacklog {
        validate(report)
        val tasks =
            report.changes
                .sortedWith(
                    compareBy(CacheDeltaEntry::domain, CacheDeltaEntry::key, CacheDeltaEntry::kind)
                )
                .map { it.toTask(report.target.revision) }
        return CacheUpgradeBacklog(
            baseline = report.baseline,
            target = report.target,
            summary =
                CacheUpgradeBacklogSummary(
                    total = tasks.size,
                    critical = tasks.count { it.priority == CacheUpgradePriority.Critical },
                    high = tasks.count { it.priority == CacheUpgradePriority.High },
                    normal = tasks.count { it.priority == CacheUpgradePriority.Normal },
                    added = tasks.count { it.kind == CacheDeltaKind.Added },
                    removed = tasks.count { it.kind == CacheDeltaKind.Removed },
                    changed = tasks.count { it.kind == CacheDeltaKind.Changed },
                ),
            tasks = tasks,
        )
    }

    private fun validate(report: CacheDeltaReport) {
        require(report.schemaVersion == CacheDeltaReport.SCHEMA_VERSION) {
            "Cache delta has unsupported schema version ${report.schemaVersion}."
        }
        require(report.baseline.revision.isNotBlank()) {
            "Cache delta is missing baseline revision."
        }
        require(report.target.revision.isNotBlank()) { "Cache delta is missing target revision." }
        require(report.baseline.archive >= 0 && report.target.archive >= 0) {
            "Cache delta has an invalid archive id."
        }
        require(report.baseline.capturedAt.isNotBlank() && report.target.capturedAt.isNotBlank()) {
            "Cache delta is missing capture time."
        }
        require(report.summary.added == report.changes.count { it.kind == CacheDeltaKind.Added }) {
            "Cache delta added summary does not match its entries."
        }
        require(
            report.summary.removed == report.changes.count { it.kind == CacheDeltaKind.Removed }
        ) {
            "Cache delta removed summary does not match its entries."
        }
        require(
            report.summary.changed == report.changes.count { it.kind == CacheDeltaKind.Changed }
        ) {
            "Cache delta changed summary does not match its entries."
        }
        val identities = mutableSetOf<CacheManifestKey>()
        for (change in report.changes) {
            require(change.domain.isNotBlank() && change.key.isNotBlank()) {
                "Cache delta contains a blank identity."
            }
            require(identities.add(CacheManifestKey(change.domain, change.key))) {
                "Cache delta contains duplicate key ${change.domain}:${change.key}."
            }
            require(
                change.before?.manifestKey in
                    setOf(null, CacheManifestKey(change.domain, change.key))
            ) {
                "Cache delta before entry does not match ${change.domain}:${change.key}."
            }
            require(
                change.after?.manifestKey in
                    setOf(null, CacheManifestKey(change.domain, change.key))
            ) {
                "Cache delta after entry does not match ${change.domain}:${change.key}."
            }
            when (change.kind) {
                CacheDeltaKind.Added ->
                    require(change.before == null && change.after != null) {
                        "Added cache delta ${change.domain}:${change.key} has invalid endpoints."
                    }
                CacheDeltaKind.Removed ->
                    require(change.before != null && change.after == null) {
                        "Removed cache delta ${change.domain}:${change.key} has invalid endpoints."
                    }
                CacheDeltaKind.Changed ->
                    require(
                        change.before != null &&
                            change.after != null &&
                            change.before != change.after
                    ) {
                        "Changed cache delta ${change.domain}:${change.key} has invalid endpoints."
                    }
            }
        }
    }

    private fun CacheDeltaEntry.toTask(targetRevision: String): CacheUpgradeTask {
        val normalizedRisks = riskTags.distinct().sorted()
        val priority =
            when {
                normalizedRisks.any(criticalRiskTags::contains) -> CacheUpgradePriority.Critical
                kind != CacheDeltaKind.Added || normalizedRisks.isNotEmpty() ->
                    CacheUpgradePriority.High
                else -> CacheUpgradePriority.Normal
            }
        val evidence = sortedSetOf<String>()
        if (before != null) {
            evidence += "baseline-cache-definition"
        }
        if (after != null) {
            evidence += "target-cache-definition"
        }
        evidence += "revision-matched-symbols"
        evidence += "official-wiki-or-update-evidence"
        normalizedRisks.forEach { evidence += "risk-review:$it" }

        val acceptance = sortedSetOf<String>()
        acceptance += "cache-fingerprint-accounted-for"
        acceptance += "feature-manifest-row-updated"
        acceptance += "full-unit-and-integration-gates-pass"
        acceptance += "implementation-or-evidence-backed-noop-recorded"
        acceptance += "revision-233.4-regression-covered"
        acceptance += "target-$targetRevision-contract-covered"
        normalizedRisks.forEach { acceptance += "$it-verified" }

        val displaySymbol = after?.symbol ?: before?.symbol
        return CacheUpgradeTask(
            id = stableId(this),
            title =
                buildString {
                    append(kind.name)
                    append(' ')
                    append(domain)
                    append(':')
                    append(key)
                    displaySymbol?.let {
                        append(" (")
                        append(it)
                        append(')')
                    }
                },
            domain = domain,
            key = key,
            kind = kind,
            priority = priority,
            symbolBefore = before?.symbol,
            symbolAfter = after?.symbol,
            fingerprintBefore = before?.fingerprint,
            fingerprintAfter = after?.fingerprint,
            changedFields = changedFields(),
            riskTags = normalizedRisks,
            evidenceRequirements = evidence.toList(),
            acceptanceCriteria = acceptance.toList(),
        )
    }

    private fun CacheDeltaEntry.changedFields(): List<String> {
        if (kind != CacheDeltaKind.Changed) {
            return listOf("definition")
        }
        val fields = sortedSetOf<String>()
        if (before?.fingerprint != after?.fingerprint) {
            fields += "fingerprint"
        }
        if (before?.symbol != after?.symbol) {
            fields += "symbol"
        }
        if (
            before?.riskTags.orEmpty().distinct().sorted() !=
                after?.riskTags.orEmpty().distinct().sorted()
        ) {
            fields += "riskTags"
        }
        val attributeKeys = before?.attributes.orEmpty().keys + after?.attributes.orEmpty().keys
        attributeKeys
            .filter { before?.attributes?.get(it) != after?.attributes?.get(it) }
            .forEach { fields += "attributes.$it" }
        return fields.toList()
    }

    private fun stableId(change: CacheDeltaEntry): String {
        val identity = "${change.domain}\u0000${change.key}\u0000${change.kind.name}"
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
        val suffix = digest.take(16).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "cache-upgrade-${change.domain.lowercase().replace(Regex("[^a-z0-9]+"), "-")}-$suffix"
    }
}
