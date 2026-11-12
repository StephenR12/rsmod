package org.rsmod.api.cache.delta

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.Js5Configs
import org.rsmod.api.cache.types.dbrow.DbRowTypeDecoder
import org.rsmod.api.cache.types.dbtable.DbTableTypeDecoder
import org.rsmod.api.cache.types.enums.EnumTypeDecoder
import org.rsmod.api.cache.types.inv.InvTypeDecoder
import org.rsmod.api.cache.types.param.ParamTypeDecoder
import org.rsmod.api.cache.types.proj.ProjAnimTypeDecoder
import org.rsmod.api.cache.types.seq.SeqTypeDecoder
import org.rsmod.api.cache.types.spot.SpotanimTypeDecoder
import org.rsmod.api.cache.types.struct.StructTypeDecoder
import org.rsmod.api.cache.types.varbit.VarBitTypeDecoder
import org.rsmod.api.cache.types.varp.VarpTypeDecoder
import org.rsmod.game.type.dbrow.UnpackedDbRowType
import org.rsmod.game.type.dbtable.UnpackedDbTableType
import org.rsmod.game.type.enums.UnpackedEnumType
import org.rsmod.game.type.inv.UnpackedInvType
import org.rsmod.game.type.param.UnpackedParamType
import org.rsmod.game.type.proj.UnpackedProjAnimType
import org.rsmod.game.type.seq.UnpackedSeqType
import org.rsmod.game.type.spot.UnpackedSpotanimType
import org.rsmod.game.type.struct.UnpackedStructType
import org.rsmod.game.type.varbit.UnpackedVarBitType
import org.rsmod.game.type.varp.UnpackedVarpType

/** Exports behavior-defining JS5 config families beyond objects, NPCs and locations. */
public object SemanticConfigManifestExporter {
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
        val symbols = SemanticConfigSymbols.load(symbolDirectory)
        return Cache.open(cacheDirectory).use { cache ->
            export(cache, symbols, revision, archive, capturedAt)
        }
    }

    public fun export(
        cache: Cache,
        symbols: SemanticConfigSymbols,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        val entries = ArrayList<CacheManifestEntry>()
        entries +=
            CoreConfigManifestExporter.export(
                    cache = cache,
                    symbols = symbols.core,
                    revision = revision,
                    archive = archive,
                    capturedAt = capturedAt,
                )
                .entries
        entries += exportVarps(cache, symbols.varps)
        entries += exportVarbits(cache, symbols.varbits)
        entries += exportVarClients(cache, symbols.varClients)
        entries += exportVarObjects(cache, symbols.varObjects)
        entries += exportInventories(cache, symbols.inventories)
        entries += exportParameters(cache, symbols.parameters)
        entries += exportEnums(cache, symbols.enums)
        entries += exportStructs(cache, symbols.structs)
        entries += exportDatabaseTables(cache, symbols.databaseTables)
        entries += exportDatabaseRows(cache, symbols.databaseRows)
        entries += exportSequences(cache, symbols.sequences)
        entries += exportSpotAnimations(cache, symbols.spotAnimations)
        entries += exportProjectiles(cache, symbols.projectiles)
        return CacheManifestNormalizer.normalize(
            CacheManifest(
                revision = revision,
                archive = archive,
                capturedAt = capturedAt,
                entries = entries,
            )
        )
    }

    private fun exportVarps(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.VARPLAYER) { id, data, bytes ->
            varpEntry(id, VarpTypeDecoder.decode(data).build(id), symbols[id], bytes)
        }

    private fun exportVarbits(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.VARBIT) { id, data, bytes ->
            varbitEntry(id, VarBitTypeDecoder.decode(data).build(id), symbols[id], bytes)
        }

    private fun exportVarClients(
        cache: Cache,
        symbols: Map<Int, String>,
    ): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.VARCLIENT) { id, data, bytes ->
            varClientEntry(id, symbols[id], bytes, decodeVarClientPersistence(id, data))
        }

    private fun exportVarObjects(
        cache: Cache,
        symbols: Map<Int, String>,
    ): List<CacheManifestEntry> =
        exportOptionalConfig(cache, Js5Configs.VAROBJECT) { id, _, bytes ->
            opaqueConfigEntry(
                domain = "varobjects",
                id = id,
                symbol = symbols[id],
                encoded = bytes,
                reason = "semantic-decoder-not-implemented",
                risks = listOf("item-state-risk", "state-transition-risk"),
            )
        }

    private fun exportInventories(
        cache: Cache,
        symbols: Map<Int, String>,
    ): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.INV) { id, data, bytes ->
            inventoryEntry(id, InvTypeDecoder.decode(data).build(id), symbols[id], bytes)
        }

    private fun exportParameters(
        cache: Cache,
        symbols: Map<Int, String>,
    ): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.PARAM) { id, data, bytes ->
            parameterEntry(id, ParamTypeDecoder.decode(data).build(id), symbols[id], bytes)
        }

    private fun exportEnums(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.ENUM) { id, data, bytes ->
            val type = EnumTypeDecoder.decode(data).build(id)
            if (type == null) {
                opaqueConfigEntry(
                    domain = "enums",
                    id = id,
                    symbol = symbols[id],
                    encoded = bytes,
                    reason = "missing-key-or-value-literal",
                    risks = listOf("client-contract-risk", "data-contract-risk"),
                )
            } else {
                enumEntry(id, type, symbols[id], bytes)
            }
        }

    private fun exportStructs(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.STRUCT) { id, data, bytes ->
            structEntry(id, StructTypeDecoder.decode(data).build(id), symbols[id], bytes)
        }

    private fun exportDatabaseTables(
        cache: Cache,
        symbols: Map<Int, String>,
    ): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.DBTABLE) { id, data, bytes ->
            databaseTableEntry(id, DbTableTypeDecoder.decode(data).build(id), symbols[id], bytes)
        }

    private fun exportDatabaseRows(
        cache: Cache,
        symbols: Map<Int, String>,
    ): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.DBROW) { id, data, bytes ->
            databaseRowEntry(id, DbRowTypeDecoder.decode(data).build(id), symbols[id], bytes)
        }

    private fun exportSequences(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> =
        exportConfig(cache, Js5Configs.SEQ) { id, data, bytes ->
            sequenceEntry(id, SeqTypeDecoder.decode(data).build(id), symbols[id], bytes)
        }

    private fun exportSpotAnimations(
        cache: Cache,
        symbols: Map<Int, String>,
    ): List<CacheManifestEntry> {
        val decoded = SpotanimTypeDecoder.decodeAll(cache)
        return CoreConfigManifestExporter.configFileIds(cache, Js5Configs.SPOTANIM).map { id ->
            val bytes = CoreConfigManifestExporter.readConfigBytes(cache, Js5Configs.SPOTANIM, id)
            val type = checkNotNull(decoded[id]) { "Spot animation $id was not decoded." }
            spotAnimationEntry(id, type, symbols[id], bytes)
        }
    }

    internal fun exportProjectiles(
        cache: Cache,
        symbols: Map<Int, String>,
    ): List<CacheManifestEntry> {
        if (!cache.exists(Js5Archives.CONFIG, Js5Configs.PROJANIM)) {
            return emptyList()
        }
        return exportConfig(cache, Js5Configs.PROJANIM) { id, data, bytes ->
            projectileEntry(id, ProjAnimTypeDecoder.decode(data).build(id), symbols[id], bytes)
        }
    }

    internal fun varpEntry(
        id: Int,
        type: UnpackedVarpType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "varps",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "bitProtect" to type.bitProtect,
                    "clientCode" to type.clientCode,
                    "scope" to type.scope,
                    "transmit" to type.transmit,
                ),
            risks = listOf("client-contract-risk", "state-transition-risk"),
        )

    internal fun varbitEntry(
        id: Int,
        type: UnpackedVarBitType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "varbits",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "baseVar" to type.varpId,
                    "leastSignificantBit" to type.lsb,
                    "mostSignificantBit" to type.msb,
                    "width" to type.bits.count(),
                ),
            risks = listOf("client-contract-risk", "state-transition-risk"),
        )

    internal fun varClientEntry(
        id: Int,
        symbol: String?,
        encoded: ByteArray,
        persistent: Boolean,
    ): CacheManifestEntry =
        entry(
            domain = "varclients",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes = mapOf("persistent" to persistent),
            risks = listOf("client-contract-risk", "state-transition-risk"),
        )

    internal fun inventoryEntry(
        id: Int,
        type: UnpackedInvType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "inventories",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "size" to type.size,
                    "scope" to type.scope,
                    "stack" to type.stack,
                    "flags" to type.flags,
                    "restock" to type.restock,
                    "allStock" to type.allStock,
                    "protect" to type.protect,
                    "runWeight" to type.runWeight,
                    "dummy" to type.dummyInv,
                    "placeholders" to type.placeholders,
                    "stock" to type.stock,
                ),
            risks = listOf("client-contract-risk", "inventory-contract-risk"),
        )

    internal fun parameterEntry(
        id: Int,
        type: UnpackedParamType<*>,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "parameters",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "typeLiteral" to type.typeLiteral?.char,
                    "defaultInt" to type.defaultInt,
                    "defaultString" to type.defaultStr,
                    "autoDisable" to type.autoDisable,
                    "transmit" to type.transmit,
                ),
            risks = listOf("data-contract-risk", "parameter-risk"),
        )

    internal fun enumEntry(
        id: Int,
        type: UnpackedEnumType<*, *>,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "enums",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "keyLiteral" to type.keyLiteral.char,
                    "valueLiteral" to type.valLiteral.char,
                    "defaultInt" to type.defaultInt,
                    "defaultString" to type.defaultStr,
                    "transmit" to type.transmit,
                    "entryCount" to type.primitiveMap.size,
                    "entries" to type.primitiveMap,
                ),
            risks = listOf("client-contract-risk", "data-contract-risk"),
        )

    internal fun structEntry(
        id: Int,
        type: UnpackedStructType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "structs",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes = mapOf("parameters" to type.paramMap?.primitiveMap),
            risks = listOf("data-contract-risk", "parameter-risk"),
        )

    internal fun databaseTableEntry(
        id: Int,
        type: UnpackedDbTableType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "database-tables",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "columnCount" to type.columnCount,
                    "types" to type.types,
                    "defaults" to type.defaults,
                    "attributes" to type.attributes,
                    "columnTables" to type.columnTables,
                    "clientSide" to type.clientSide(),
                ),
            risks = listOf("client-contract-risk", "data-contract-risk"),
        )

    internal fun databaseRowEntry(
        id: Int,
        type: UnpackedDbRowType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "database-rows",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "table" to type.table,
                    "columnCount" to type.columnCount,
                    "types" to type.types,
                    "data" to type.data,
                ),
            risks = listOf("client-contract-risk", "data-contract-risk"),
        )

    internal fun sequenceEntry(
        id: Int,
        type: UnpackedSeqType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "sequences",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "frameGroups" to type.frameGroup,
                    "frameIndices" to type.frameIndex,
                    "delays" to type.delay,
                    "totalDelay" to type.totalDelay,
                    "tickDuration" to type.tickDuration,
                    "loops" to type.loops,
                    "walkMerge" to type.walkMerge,
                    "stretches" to type.stretches,
                    "priority" to type.priority,
                    "replaceHeldRight" to type.replaceHeldRight,
                    "replaceHeldLeft" to type.replaceHeldLeft,
                    "maxLoops" to type.maxLoops,
                    "preAnimationMove" to type.preanimMove,
                    "postAnimationMove" to type.postanimMove,
                    "duplicateBehaviour" to type.duplicateBehaviour,
                    "interleaveFrameGroups" to type.iframeGroup,
                    "interleaveFrameIndices" to type.iframeIndex,
                    "frameSounds" to type.sounds,
                    "keyframeSet" to type.keyframeSet,
                    "mayaAnimationSounds" to type.mayaAnimationSounds,
                    "keyframeRange" to "${type.keyframeRangeStart}:${type.keyframeRangeEnd}",
                    "verticalOffset" to type.verticalOffset,
                    "keyframeWalkMerge" to type.keyframeWalkMerge,
                    "debugName" to type.debugName,
                    "crossWorldSound" to type.crossWorldSound,
                ),
            risks =
                buildList {
                    add("animation-risk")
                    add("client-contract-risk")
                    if (type.sounds.isNotEmpty() || !type.mayaAnimationSounds.isNullOrEmpty()) {
                        add("audio-risk")
                    }
                },
        )

    internal fun spotAnimationEntry(
        id: Int,
        type: UnpackedSpotanimType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "spot-animations",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "model" to type.model,
                    "animation" to type.anim,
                    "ambient" to type.ambient,
                    "contrast" to type.contrast,
                    "rotation" to type.rotation,
                    "resizeHorizontal" to type.resizeH,
                    "resizeVertical" to type.resizeV,
                    "debugName" to type.debugName,
                    "recolorSource" to type.recolS,
                    "recolorDestination" to type.recolD,
                    "retextureSource" to type.retexS,
                    "retextureDestination" to type.retexD,
                ),
            risks = listOf("animation-risk", "client-contract-risk", "graphics-risk"),
        )

    internal fun projectileEntry(
        id: Int,
        type: UnpackedProjAnimType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry =
        entry(
            domain = "projectiles",
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes =
                mapOf(
                    "startHeight" to type.startHeight,
                    "endHeight" to type.endHeight,
                    "delay" to type.delay,
                    "angle" to type.angle,
                    "lengthAdjustment" to type.lengthAdjustment,
                    "progress" to type.progress,
                    "stepMultiplier" to type.stepMultiplier,
                ),
            risks = listOf("client-contract-risk", "projectile-timing-risk"),
        )

    internal fun opaqueConfigEntry(
        domain: String,
        id: Int,
        symbol: String?,
        encoded: ByteArray,
        reason: String,
        risks: List<String>,
    ): CacheManifestEntry =
        entry(
            domain = domain,
            id = id,
            symbol = symbol,
            encoded = encoded,
            attributes = mapOf("decoded" to false, "reason" to reason),
            risks = risks,
        )

    private inline fun exportConfig(
        cache: Cache,
        group: Int,
        create: (id: Int, data: ByteBuf, bytes: ByteArray) -> CacheManifestEntry,
    ): List<CacheManifestEntry> =
        CoreConfigManifestExporter.configFileIds(cache, group).map { id ->
            val bytes = CoreConfigManifestExporter.readConfigBytes(cache, group, id)
            val data = Unpooled.wrappedBuffer(bytes)
            try {
                create(id, data, bytes)
            } finally {
                data.release()
            }
        }

    private inline fun exportOptionalConfig(
        cache: Cache,
        group: Int,
        create: (id: Int, data: ByteBuf, bytes: ByteArray) -> CacheManifestEntry,
    ): List<CacheManifestEntry> =
        if (cache.exists(Js5Archives.CONFIG, group)) exportConfig(cache, group, create)
        else emptyList()

    internal fun decodeVarClientPersistence(id: Int, data: ByteBuf): Boolean {
        var persistent = false
        while (data.isReadable) {
            when (val opcode = data.readUnsignedByte().toInt()) {
                0 -> {
                    require(!data.isReadable) { "Varclient $id has bytes after its terminator." }
                    return persistent
                }
                2 -> persistent = true
                else -> error("Varclient $id has unsupported opcode $opcode.")
            }
        }
        error("Varclient $id has no terminator.")
    }

    private fun entry(
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
            attributes = TreeMap(attributes.mapValues { canonical(it.value) }),
            riskTags = risks.sorted(),
        )

    internal fun canonical(value: Any?): String =
        when (value) {
            null -> "null"
            is Map<*, *> ->
                value.entries
                    .sortedBy { canonical(it.key) }
                    .joinToString(prefix = "{", postfix = "}") {
                        "${canonical(it.key)}=${canonical(it.value)}"
                    }
            is Set<*> -> value.map(::canonical).sorted().joinToString(prefix = "[", postfix = "]")
            is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { canonical(it) }
            is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { canonical(it) }
            is BooleanArray -> value.joinToString(prefix = "[", postfix = "]")
            is ByteArray -> value.joinToString(prefix = "[", postfix = "]")
            is ShortArray -> value.joinToString(prefix = "[", postfix = "]")
            is IntArray -> value.joinToString(prefix = "[", postfix = "]")
            is LongArray -> value.joinToString(prefix = "[", postfix = "]")
            is FloatArray -> value.joinToString(prefix = "[", postfix = "]")
            is DoubleArray -> value.joinToString(prefix = "[", postfix = "]")
            else -> value.toString()
        }
}
