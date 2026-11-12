package org.rsmod.api.cache.delta

import java.nio.file.Paths

public object CacheUpgradePlanMain {
    public fun run(args: Array<String>): Int {
        if (args.size != 4 || args[0] != "plan-upgrade") {
            System.err.println(
                "Usage: cache-delta plan-upgrade <baseline-manifest.json> " +
                    "<target-manifest.json> <backlog.json>"
            )
            return 2
        }
        val baseline = CacheDeltaJson.readManifest(Paths.get(args[1]))
        val target = CacheDeltaJson.readManifest(Paths.get(args[2]))
        val report = CacheDeltaEngine.diff(baseline, target)
        val backlog = CacheUpgradeBacklogGenerator.generate(report)
        CacheDeltaJson.writeBacklog(Paths.get(args[3]), backlog)
        println(
            "Cache upgrade backlog ${backlog.baseline.revision} -> ${backlog.target.revision}: " +
                "${backlog.summary.total} atomic tasks (${backlog.summary.critical} critical, " +
                "${backlog.summary.high} high, ${backlog.summary.normal} normal)."
        )
        return 0
    }
}
