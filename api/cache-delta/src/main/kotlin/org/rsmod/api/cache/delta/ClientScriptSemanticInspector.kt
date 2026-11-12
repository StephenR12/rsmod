package org.rsmod.api.cache.delta

import java.nio.file.Files
import java.nio.file.Path
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.types.clientscript.ClientScriptTypeDecoder

/** Decodes one client script for exact, revision-specific contract inspection. */
public object ClientScriptSemanticInspector {
    public data class Snapshot(
        public val id: Int,
        public val intLocalCount: Int,
        public val stringLocalCount: Int,
        public val intArgumentCount: Int,
        public val stringArgumentCount: Int,
        public val instructions: List<Instruction>,
        public val switches: List<Map<Int, Int>>,
    )

    public data class Instruction(
        public val index: Int,
        public val opcode: Int,
        public val intOperand: Int,
        public val stringOperand: String?,
    )

    public fun inspect(cacheDirectory: Path, id: Int): Snapshot {
        require(Files.isDirectory(cacheDirectory)) {
            "Cache directory does not exist: ${cacheDirectory.toAbsolutePath()}"
        }
        return Cache.open(cacheDirectory).use { cache ->
            require(cache.exists(Js5Archives.CLIENTSCRIPTS, id)) {
                "Client script $id does not exist in ${cacheDirectory.toAbsolutePath()}."
            }
            val data = cache.read(Js5Archives.CLIENTSCRIPTS, id, file = 0)
            val type =
                try {
                    ClientScriptTypeDecoder.decode(data).build(id)
                } finally {
                    data.release()
                }
            Snapshot(
                id = id,
                intLocalCount = type.intLocalCount,
                stringLocalCount = type.stringLocalCount,
                intArgumentCount = type.intArgumentCount,
                stringArgumentCount = type.stringArgumentCount,
                instructions =
                    type.commands.indices.map { index ->
                        Instruction(
                            index = index,
                            opcode = type.commands[index],
                            intOperand = type.intOperands[index],
                            stringOperand = type.stringOperands[index],
                        )
                    },
                switches = type.switches.map(Map<Int, Int>::toSortedMap),
            )
        }
    }
}
