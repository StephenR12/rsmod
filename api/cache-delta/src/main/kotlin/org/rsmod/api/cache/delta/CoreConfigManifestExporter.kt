package org.rsmod.api.cache.delta

import io.netty.buffer.Unpooled
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.TreeMap
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.Js5Configs
import org.rsmod.api.cache.types.loc.LocTypeDecoder
import org.rsmod.api.cache.types.npc.NpcTypeDecoder
import org.rsmod.api.cache.types.obj.ObjTypeDecoder
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.util.ParamMap

public object CoreConfigManifestExporter {
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
        val symbols = CoreConfigSymbols.load(symbolDirectory)
        return Cache.open(cacheDirectory).use { cache ->
            export(cache, symbols, revision, archive, capturedAt)
        }
    }

    public fun export(
        cache: Cache,
        symbols: CoreConfigSymbols,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        val entries = ArrayList<CacheManifestEntry>()
        entries += exportObjects(cache, symbols.objects)
        entries += exportNpcs(cache, symbols.npcs)
        entries += exportLocations(cache, symbols.locations)
        return CacheManifestNormalizer.normalize(
            CacheManifest(
                revision = revision,
                archive = archive,
                capturedAt = capturedAt,
                entries = entries,
            )
        )
    }

    private fun exportObjects(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> =
        configFileIds(cache, Js5Configs.OBJ).map { id ->
            val bytes = readConfigBytes(cache, Js5Configs.OBJ, id)
            val data = Unpooled.wrappedBuffer(bytes)
            val type =
                try {
                    ObjTypeDecoder.decode(data).build(id)
                } finally {
                    data.release()
                }
            objectEntry(id, type, symbols[id], bytes)
        }

    private fun exportNpcs(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> =
        configFileIds(cache, Js5Configs.NPC).map { id ->
            val bytes = readConfigBytes(cache, Js5Configs.NPC, id)
            val data = Unpooled.wrappedBuffer(bytes)
            val type =
                try {
                    NpcTypeDecoder.decode(data).build(id)
                } finally {
                    data.release()
                }
            npcEntry(id, type, symbols[id], bytes)
        }

    private fun exportLocations(cache: Cache, symbols: Map<Int, String>): List<CacheManifestEntry> =
        configFileIds(cache, Js5Configs.LOC).map { id ->
            val bytes = readConfigBytes(cache, Js5Configs.LOC, id)
            val data = Unpooled.wrappedBuffer(bytes)
            val type =
                try {
                    LocTypeDecoder.decode(data).build(id)
                } finally {
                    data.release()
                }
            locationEntry(id, type, symbols[id], bytes)
        }

    internal fun objectEntry(
        id: Int,
        type: UnpackedObjType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry {
        val attributes = TreeMap<String, String>()
        attributes["name"] = type.name
        attributes["description"] = type.desc
        attributes["members"] = type.members.toString()
        attributes["stackable"] = type.stackable.toString()
        attributes["cost"] = type.cost.toString()
        attributes["tradeable"] = type.tradeable.toString()
        attributes["wearSlots"] =
            listOf(type.wearpos1, type.wearpos2, type.wearpos3).joinToString(",")
        attributes["shiftClickOp"] = type.shiftclickiop.toString()
        attributes["certificate"] = "${type.certlink}:${type.certtemplate}"
        attributes["bought"] = "${type.boughtlink}:${type.boughttemplate}"
        attributes["placeholder"] = "${type.placeholderlink}:${type.placeholdertemplate}"
        attributes["transform"] = "${type.transformlink}:${type.transformtemplate}"
        attributes["category"] = type.category.toString()
        attributes["contentGroup"] = type.contentGroup.toString()
        attributes["weaponCategory"] = type.weaponCategory.toString()
        attributes["respawnRate"] = type.respawnRate.toString()
        attributes["dummyItem"] = type.dummyitem.toString()
        attributes.addOperations("groundOp", type.op)
        attributes.addOperations("inventoryOp", type.iop)
        listOf(type.isubop1, type.isubop2, type.isubop3, type.isubop4, type.isubop5)
            .forEachIndexed { slot, operations ->
                operations?.forEachIndexed { subSlot, operation ->
                    attributes["inventorySubOp.${slot + 1}.${subSlot + 1}"] = operation
                }
            }
        attributes.addParams(type.paramMap)
        val risks = sortedSetOf("item-contract-risk")
        if (type.op.any { it.isMeaningful() } || type.iop.any { it.isMeaningful() }) {
            risks += "interaction-risk"
        }
        if (type.isEquipable) risks += "equipment-risk"
        if (
            type.objvar.isNotEmpty() ||
                type.certlink > 0 ||
                type.placeholderlink > 0 ||
                type.transformlink > 0
        ) {
            risks += "state-transition-risk"
        }
        if (type.paramMap?.primitiveMap?.isNotEmpty() == true) risks += "parameter-risk"
        return CacheManifestEntry(
            domain = "objects",
            key = id.toString(),
            symbol = symbol,
            fingerprint = fingerprint(encoded),
            attributes = attributes,
            riskTags = risks.toList(),
        )
    }

    internal fun npcEntry(
        id: Int,
        type: UnpackedNpcType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry {
        val attributes = TreeMap<String, String>()
        attributes["name"] = type.name
        attributes["description"] = type.desc
        attributes["size"] = type.size.toString()
        attributes["category"] = type.category.toString()
        attributes["contentGroup"] = type.contentGroup.toString()
        attributes["combatStats"] =
            listOf(
                    type.attack,
                    type.strength,
                    type.defence,
                    type.hitpoints,
                    type.ranged,
                    type.magic,
                )
                .joinToString(",")
        attributes["ranges"] =
            listOf(type.maxRange, type.wanderRange, type.attackRange, type.huntRange)
                .joinToString(",")
        attributes["movement"] = "${type.moveRestrict}:${type.blockWalk}"
        attributes["mode"] = type.defaultMode.toString()
        attributes["huntMode"] = type.huntMode?.toString().orEmpty()
        attributes["giveChase"] = type.giveChase.toString()
        attributes["respawnRate"] = type.respawnRate.toString()
        attributes["regenRate"] = type.regenRate.toString()
        attributes["morphVarbit"] = type.multiVarBit.toString()
        attributes["morphVarp"] = type.multiVarp.toString()
        attributes["morphs"] =
            (type.multiNpc.map(Short::toInt) + type.multiNpcDefault).joinToString(",")
        attributes.addOperations("operation", type.op)
        attributes.addParams(type.paramMap)
        val risks = sortedSetOf("npc-contract-risk")
        if (type.op.any { it.isMeaningful() }) risks += "interaction-risk"
        if (type.hitpoints > 0 || type.attackRange > 0) risks += "combat-risk"
        if (type.multiNpc.isNotEmpty() || type.multiNpcDefault >= 0) {
            risks += "state-transition-risk"
        }
        if (type.paramMap?.primitiveMap?.isNotEmpty() == true) risks += "parameter-risk"
        return CacheManifestEntry(
            domain = "npcs",
            key = id.toString(),
            symbol = symbol,
            fingerprint = fingerprint(encoded),
            attributes = attributes,
            riskTags = risks.toList(),
        )
    }

    internal fun locationEntry(
        id: Int,
        type: UnpackedLocType,
        symbol: String?,
        encoded: ByteArray,
    ): CacheManifestEntry {
        val attributes = TreeMap<String, String>()
        attributes["name"] = type.name
        attributes["description"] = type.desc
        attributes["dimensions"] = "${type.width}:${type.length}"
        attributes["collision"] =
            "${type.blockWalk}:${type.blockRange}:${type.forceApproachFlags}:${type.breakRouteFinding}"
        attributes["active"] = type.active.toString()
        attributes["animation"] = type.anim.toString()
        attributes["category"] = type.category.toString()
        attributes["contentGroup"] = type.contentGroup.toString()
        attributes["morphVarbit"] = type.multiVarBit.toString()
        attributes["morphVarp"] = type.multiVarp.toString()
        attributes["morphs"] =
            (type.multiLoc.map(Short::toInt) + type.multiLocDefault).joinToString(",")
        attributes["ambientSound"] =
            listOf(
                    type.bgsoundSound,
                    type.bgsoundRange,
                    type.bgsoundSize,
                    type.bgsoundMinDelay,
                    type.bgsoundMaxDelay,
                )
                .joinToString(",")
        attributes["randomSounds"] = type.bgsoundRandomSounds.joinToString(",")
        attributes.addOperations("operation", type.op)
        attributes.addParams(type.paramMap)
        val risks = sortedSetOf("location-contract-risk", "map-collision-risk")
        if (type.op.any { it.isMeaningful() }) risks += "interaction-risk"
        if (type.multiLoc.isNotEmpty() || type.multiLocDefault >= 0) {
            risks += "state-transition-risk"
        }
        if (type.paramMap?.primitiveMap?.isNotEmpty() == true) risks += "parameter-risk"
        return CacheManifestEntry(
            domain = "locations",
            key = id.toString(),
            symbol = symbol,
            fingerprint = fingerprint(encoded),
            attributes = attributes,
            riskTags = risks.toList(),
        )
    }

    internal fun fingerprint(bytes: ByteArray): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        return "sha256:" + hash.joinToString("") { "%02x".format(it) }
    }

    internal fun configFileIds(cache: Cache, group: Int): List<Int> =
        cache.list(Js5Archives.CONFIG, group).asSequence().map { it.id }.sorted().toList()

    internal fun readConfigBytes(cache: Cache, group: Int, file: Int): ByteArray {
        val data = cache.read(Js5Archives.CONFIG, group, file)
        return try {
            ByteArray(data.readableBytes()).also { data.getBytes(data.readerIndex(), it) }
        } finally {
            data.release()
        }
    }

    private fun MutableMap<String, String>.addOperations(
        prefix: String,
        operations: Array<String?>,
    ) {
        operations.forEachIndexed { slot, operation ->
            if (operation != null) this["$prefix.${slot + 1}"] = operation
        }
    }

    private fun MutableMap<String, String>.addParams(params: ParamMap?) {
        params?.primitiveMap?.toSortedMap()?.forEach { (id, value) ->
            this["param.$id"] = value.toString()
        }
    }

    private fun String?.isMeaningful(): Boolean =
        this != null && isNotBlank() && !equals("hidden", ignoreCase = true)
}
