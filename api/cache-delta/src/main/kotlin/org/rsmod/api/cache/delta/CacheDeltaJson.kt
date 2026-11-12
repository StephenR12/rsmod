package org.rsmod.api.cache.delta

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

public object CacheDeltaJson {
    private val mapper: ObjectMapper =
        JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build()

    public fun readManifest(path: Path): CacheManifest =
        Files.newInputStream(path).use(mapper::readValue)

    public fun writeManifest(manifest: CacheManifest): String =
        mapper.writeValueAsString(CacheManifestNormalizer.normalize(manifest)) + "\n"

    public fun writeManifest(path: Path, manifest: CacheManifest) {
        path.toAbsolutePath().normalize().parent?.let(Files::createDirectories)
        Files.writeString(path, writeManifest(manifest))
    }

    public fun writeReport(report: CacheDeltaReport): String =
        mapper.writeValueAsString(report) + "\n"

    public fun writeReport(path: Path, report: CacheDeltaReport) {
        path.toAbsolutePath().normalize().parent?.let(Files::createDirectories)
        Files.writeString(path, writeReport(report))
    }

    public fun readBacklog(path: Path): CacheUpgradeBacklog =
        Files.newInputStream(path).use(mapper::readValue)

    public fun writeBacklog(backlog: CacheUpgradeBacklog): String =
        mapper.writeValueAsString(backlog) + "\n"

    public fun writeBacklog(path: Path, backlog: CacheUpgradeBacklog) {
        path.toAbsolutePath().normalize().parent?.let(Files::createDirectories)
        Files.writeString(path, writeBacklog(backlog))
    }
}
