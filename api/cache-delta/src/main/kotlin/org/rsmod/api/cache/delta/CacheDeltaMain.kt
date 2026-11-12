package org.rsmod.api.cache.delta

import java.nio.file.Paths
import kotlin.system.exitProcess

public fun main(args: Array<String>) {
    if (
        args.firstOrNull() in
            setOf(
                "export-core",
                "export-configs",
                "export-projectiles",
                "export-server-variables",
                "export-maps",
                "export-npc-spawns",
                "export-client-contracts",
                "export-archive-indexes",
            )
    ) {
        exitProcess(CacheManifestExportMain.run(args))
    }
    if (args.firstOrNull() == "plan-upgrade") {
        exitProcess(CacheUpgradePlanMain.run(args))
    }
    if (args.size != 3) {
        System.err.println(
            "Usage: cache-delta <baseline-manifest.json> <target-manifest.json> <delta.json>\n" +
                "   or: cache-delta export-core <cache-dir> <symbols-dir> <revision> " +
                "<archive> <captured-at> <manifest.json>\n" +
                "   or: cache-delta export-configs <cache-dir> <symbols-dir> <revision> " +
                "<archive> <captured-at> <manifest.json>\n" +
                "   or: cache-delta export-projectiles <enriched-cache-dir> <symbols-dir> " +
                "<revision> <archive> <captured-at> <manifest.json>\n" +
                "   or: cache-delta export-server-variables <enriched-cache-dir> <symbols-dir> " +
                "<revision> <archive> <captured-at> <manifest.json>\n" +
                "   or: cache-delta export-maps <cache-dir> <xtea-file> <revision> " +
                "<archive> <captured-at> <manifest.json>\n" +
                "   or: cache-delta export-npc-spawns <enriched-cache-dir> <xtea-file> " +
                "<symbols-dir> <revision> <archive> <captured-at> <manifest.json>\n" +
                "   or: cache-delta export-client-contracts <cache-dir> <revision> " +
                "<archive> <captured-at> <manifest.json>\n" +
                "   or: cache-delta export-archive-indexes <cache-dir> <revision> " +
                "<archive> <captured-at> <manifest.json>\n" +
                "   or: cache-delta plan-upgrade <baseline-manifest.json> " +
                "<target-manifest.json> <backlog.json>"
        )
        exitProcess(2)
    }
    val baseline = CacheDeltaJson.readManifest(Paths.get(args[0]))
    val target = CacheDeltaJson.readManifest(Paths.get(args[1]))
    val report = CacheDeltaEngine.diff(baseline, target)
    CacheDeltaJson.writeReport(Paths.get(args[2]), report)
    println(
        "Cache delta ${report.baseline.revision} -> ${report.target.revision}: " +
            "${report.summary.added} added, ${report.summary.removed} removed, " +
            "${report.summary.changed} changed, ${report.summary.unchanged} unchanged."
    )
}
