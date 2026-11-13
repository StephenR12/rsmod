package org.rsmod.api.cache.delta

import io.netty.buffer.Unpooled
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.Js5Configs
import org.rsmod.api.cache.types.varn.VarnTypeDecoder
import org.rsmod.api.cache.types.varnbit.VarnBitTypeDecoder
import org.rsmod.api.config.refs.BaseVarObjBits
import org.rsmod.api.type.symbols.name.NameLoader
import org.rsmod.game.type.varn.UnpackedVarnType
import org.rsmod.game.type.varnbit.UnpackedVarnBitType

/** Inventories server-owned variable namespaces without presenting them as vanilla cache data. */
public object EnrichedVariableManifestExporter {
    public fun export(
        cacheDirectory: Path,
        symbolDirectory: Path,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        require(Files.isDirectory(cacheDirectory)) {
            "Cache directory does not exist: ${cacheDirectory.toAbsolutePath()}"
        }
        val symbols = EnrichedVariableSymbols.load(symbolDirectory)
        return Cache.open(cacheDirectory).use { cache ->
            export(cache, symbols, revision, archive, capturedAt)
        }
    }

    public fun export(
        cache: Cache,
        symbols: EnrichedVariableSymbols,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        val entries = ArrayList<CacheManifestEntry>()
        entries += exportVarns(cache, symbols.varns)
        entries += exportVarnBits(cache, symbols.varnBits)
        entries +=
            BaseVarObjBits.definitions().map { type ->
                val id = symbols.varObjects.idFor(type.internalName)
                codeEntry(
                    domain = "server-varobjects",
                    id = id,
                    symbol = type.internalName,
                    attributes =
                        mapOf(
                            "startBit" to type.startBit,
                            "endBit" to type.endBit,
                            "width" to type.bits.count(),
                        ),
                    risks = listOf("item-state-risk", "state-transition-risk"),
                )
            }
        entries +=
            symbols.varCons.map { (id,name) ->
                codeEntry(
                    domain = "server-varcons",
                    id = id,
                    symbol = name,
                    attributes = emptyMap(),
                    risks = listOf("controller-state-risk", "state-transition-risk"),
                )
            }
        entries +=
            symbols.controllers.map { (id,name) ->
                codeEntry(
                    domain = "server-controllers",
                    id = id,
                    symbol = name,
                    attributes = emptyMap(),
                    risks = listOf("controller-state-risk"),
                )
            }
        require(symbols.varConBits.isEmpty()) {
            "Symbol-backed varconbits require build-definition inventory support."
        }
        return CacheManifestNormalizer.normalize(
            CacheManifest(
                revision = revision,
                archive = archive,
                capturedAt = capturedAt,
                entries = entries,
            )
        )
    }

    private fun exportVarns(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> {
        require(cache.exists(Js5Archives.CONFIG, Js5Configs.VARNPC)) {
            "The enriched cache has no server varn config group."
        }
        return CoreConfigManifestExporter.configFileIds(cache, Js5Configs.VARNPC).map { id ->
            val bytes = CoreConfigManifestExporter.readConfigBytes(cache, Js5Configs.VARNPC, id)
            val data = Unpooled.wrappedBuffer(bytes)
            val type =
                try {
                    VarnTypeDecoder.decode(data).build(id)
                } finally {
                    data.release()
                }
            varnEntry(id, type, symbols[id], bytes)
        }
    }

    private fun exportVarnBits(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> {
        if (!cache.exists(Js5Archives.CONFIG, Js5Configs.VARNBIT)) return emptyList()
        return CoreConfigManifestExporter.configFileIds(cache, Js5Configs.VARNBIT).map { id ->
            val bytes = CoreConfigManifestExporter.readConfigBytes(cache, Js5Configs.VARNBIT, id)
            val data = Unpooled.wrappedBuffer(bytes)
            val type =
                try {
                    VarnBitTypeDecoder.decode(data).build(id)
                } finally {
                    data.release()
                }
            varnBitEntry(id, type, symbols[id], bytes)
        }
    }

    internal fun varnEntry(
        id: Int,
        type: UnpackedVarnType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        cacheEntry(
            domain = "server-varns",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes = mapOf("bitProtect" to type.bitProtect),
            risks = listOf("npc-state-risk", "state-transition-risk"),
        )

    internal fun varnBitEntry(
        id: Int,
        type: UnpackedVarnBitType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        cacheEntry(
            domain = "server-varnbits",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "baseVar" to type.varnId,
                    "leastSignificantBit" to type.lsb,
                    "mostSignificantBit" to type.msb,
                    "width" to type.bits.count(),
                ),
            risks = listOf("npc-state-risk", "state-transition-risk"),
        )

    private fun cacheEntry(
        domain: String,
        id: Int,
        symbol: String?,
        encoded: ByteArray,
        attributes: Map<String, Any?>,
        risks: List<String>,
    ): CacheManifestEntry =
        CacheManifestEntry(
            domain = domain,
            key = id.toString(),
            symbol = symbol,
            fingerprint = CoreConfigManifestExporter.fingerprint(encoded),
            attributes =
                TreeMap(
                    attributes.mapValues { SemanticConfigManifestExporter.canonical(it.value) }
                ),
            riskTags = risks.sorted(),
        )

    private fun codeEntry(
        domain: String,
        id: Int,
        symbol: String?,
        attributes: Map<String, Any?>,
        risks: List<String>,
    ): CacheManifestEntry {
        val canonicalAttributes =
            TreeMap(attributes.mapValues { SemanticConfigManifestExporter.canonical(it.value) })
        canonicalAttributes["source"] = "rsmod-build-definition"
        val canonical = buildString {
            append("rsmod-build-definition-v1|")
            append(domain)
            append('|')
            append(id)
            append('|')
            append(symbol)
            canonicalAttributes.forEach { (key, value) -> append("|$key=$value") }
        }
        return CacheManifestEntry(
            domain = domain,
            key = id.toString(),
            symbol = symbol,
            fingerprint =
                CoreConfigManifestExporter.fingerprint(
                    canonical.toByteArray(StandardCharsets.UTF_8)
                ),
            attributes = canonicalAttributes,
            riskTags = risks.sorted(),
        )
    }

    private fun Map<Int, String>.idFor(name: String?): Int {
        val requiredName = requireNotNull(name) { "A build definition has no internal name." }
        return entries.singleOrNull { it.value == requiredName }?.key
            ?: error("No symbol exists for build definition '$requiredName'.")
    }
}

public data class EnrichedVariableSymbols(
    public val varns: Map<Int, String>,
    public val varnBits: Map<Int, String>,
    public val varObjects: Map<Int, String>,
    public val varCons: Map<Int, String>,
    public val varConBits: Map<Int, String>,
    public val controllers: Map<Int, String>,
) {
    public companion object {
        public fun load(directory: Path): EnrichedVariableSymbols {
            require(Files.isDirectory(directory)) {
                "Symbol directory does not exist: ${directory.toAbsolutePath()}"
            }
            return EnrichedVariableSymbols(
                varns = loadSymbols(directory.resolve("varn.sym")),
                varnBits = loadSymbols(directory.resolve("varnbit.sym")),
                varObjects = loadSymbols(directory.resolve("varobj.sym")),
                varCons = loadSymbols(directory.resolve("varcon.sym")),
                varConBits = loadSymbols(directory.resolve("varconbit.sym")),
                controllers = loadSymbols(directory.resolve("controller.sym")),
            )
        }

        private fun loadSymbols(path: Path): Map<Int, String> {
            require(Files.isRegularFile(path)) {
                "Required symbol file does not exist: ${path.toAbsolutePath()}"
            }
            return NameLoader.read(path).entries.associate { (name, id) -> id to name }
        }
    }
}
