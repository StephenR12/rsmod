package org.rsmod.api.cache.delta

import java.nio.file.Paths

internal object CacheManifestExportMain {
    fun run(args: Array<String>): Int {
        if (args.firstOrNull() == "export-client-contracts") {
            return exportClientContracts(args)
        }
        if (args.firstOrNull() == "export-archive-indexes") {
            return exportArchiveIndexes(args)
        }
        if (args.firstOrNull() == "export-maps") {
            return exportMaps(args)
        }
        if (args.firstOrNull() == "export-npc-spawns") {
            return exportNpcSpawns(args)
        }
        if (args.size != 7) {
            System.err.println(
                "Usage: cache-delta <export-core|export-configs|export-projectiles|" +
                    "export-server-variables> " +
                    "<cache-dir> <symbols-dir> " +
                    "<revision> " +
                    "<archive> <captured-at> <manifest.json>"
            )
            return 2
        }
        val archive =
            args[4].toIntOrNull()
                ?: throw IllegalArgumentException(
                    "Archive must be a non-negative integer: ${args[4]}"
                )
        val manifest =
            when (args[0]) {
                "export-core" ->
                    CoreConfigManifestExporter.export(
                        cacheDirectory = Paths.get(args[1]),
                        symbolDirectory = Paths.get(args[2]),
                        revision = args[3],
                        archive = archive,
                        capturedAt = args[5],
                    )
                "export-configs" ->
                    SemanticConfigManifestExporter.export(
                        cacheDirectory = Paths.get(args[1]),
                        symbolDirectory = Paths.get(args[2]),
                        revision = args[3],
                        archive = archive,
                        capturedAt = args[5],
                    )
                "export-projectiles" ->
                    ProjectileProfileManifestExporter.export(
                        cacheDirectory = Paths.get(args[1]),
                        symbolDirectory = Paths.get(args[2]),
                        revision = args[3],
                        archive = archive,
                        capturedAt = args[5],
                    )
                "export-server-variables" ->
                    EnrichedVariableManifestExporter.export(
                        cacheDirectory = Paths.get(args[1]),
                        symbolDirectory = Paths.get(args[2]),
                        revision = args[3],
                        archive = archive,
                        capturedAt = args[5],
                    )
                else -> error("Unsupported export mode: ${args[0]}")
            }
        CacheDeltaJson.writeManifest(Paths.get(args[6]), manifest)
        val counts =
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap()
        println("Exported ${manifest.entries.size} cache entries for ${manifest.revision}: $counts")
        return 0
    }

    private fun exportClientContracts(args: Array<String>): Int {
        if (args.size != 6) {
            System.err.println(
                "Usage: cache-delta export-client-contracts <cache-dir> <revision> " +
                    "<archive> <captured-at> <manifest.json>"
            )
            return 2
        }
        val archive =
            args[3].toIntOrNull()
                ?: throw IllegalArgumentException(
                    "Archive must be a non-negative integer: ${args[3]}"
                )
        val manifest =
            ClientContractArchiveManifestExporter.export(
                cacheDirectory = Paths.get(args[1]),
                revision = args[2],
                archive = archive,
                capturedAt = args[4],
            )
        CacheDeltaJson.writeManifest(Paths.get(args[5]), manifest)
        val counts =
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap()
        println("Exported ${manifest.entries.size} cache entries for ${manifest.revision}: $counts")
        return 0
    }

    private fun exportArchiveIndexes(args: Array<String>): Int {
        if (args.size != 6) {
            System.err.println(
                "Usage: cache-delta export-archive-indexes <cache-dir> <revision> " +
                    "<archive> <captured-at> <manifest.json>"
            )
            return 2
        }
        val archive =
            args[3].toIntOrNull()
                ?: throw IllegalArgumentException(
                    "Archive must be a non-negative integer: ${args[3]}"
                )
        val manifest =
            CacheArchiveIndexManifestExporter.export(
                cacheDirectory = Paths.get(args[1]),
                revision = args[2],
                archive = archive,
                capturedAt = args[4],
            )
        CacheDeltaJson.writeManifest(Paths.get(args[5]), manifest)
        val counts =
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap()
        println("Exported ${manifest.entries.size} cache entries for ${manifest.revision}: $counts")
        return 0
    }

    private fun exportMaps(args: Array<String>): Int {
        if (args.size != 7) {
            System.err.println(
                "Usage: cache-delta export-maps <cache-dir> <xtea-file> <revision> " +
                    "<archive> <captured-at> <manifest.json>"
            )
            return 2
        }
        val archive =
            args[4].toIntOrNull()
                ?: throw IllegalArgumentException(
                    "Archive must be a non-negative integer: ${args[4]}"
                )
        val manifest =
            MapSquareManifestExporter.export(
                cacheDirectory = Paths.get(args[1]),
                xteaFile = Paths.get(args[2]),
                revision = args[3],
                archive = archive,
                capturedAt = args[5],
            )
        CacheDeltaJson.writeManifest(Paths.get(args[6]), manifest)
        val counts =
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap()
        println("Exported ${manifest.entries.size} cache entries for ${manifest.revision}: $counts")
        return 0
    }

    private fun exportNpcSpawns(args: Array<String>): Int {
        if (args.size != 8) {
            System.err.println(
                "Usage: cache-delta export-npc-spawns <enriched-cache-dir> <xtea-file> " +
                    "<symbols-dir> <revision> <archive> <captured-at> <manifest.json>"
            )
            return 2
        }
        val archive =
            args[5].toIntOrNull()
                ?: throw IllegalArgumentException(
                    "Archive must be a non-negative integer: ${args[5]}"
                )
        val manifest =
            EnrichedNpcSpawnManifestExporter.export(
                cacheDirectory = Paths.get(args[1]),
                xteaFile = Paths.get(args[2]),
                symbolDirectory = Paths.get(args[3]),
                revision = args[4],
                archive = archive,
                capturedAt = args[6],
            )
        CacheDeltaJson.writeManifest(Paths.get(args[7]), manifest)
        val counts =
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap()
        println("Exported ${manifest.entries.size} cache entries for ${manifest.revision}: $counts")
        return 0
    }
}
