package org.rsmod.api.cache.delta

import java.nio.file.Files
import java.nio.file.Path
import org.rsmod.api.type.symbols.name.NameLoader

public data class CoreConfigSymbols(
    public val objects: Map<Int, String>,
    public val npcs: Map<Int, String>,
    public val locations: Map<Int, String>,
) {
    public companion object {
        public fun load(directory: Path): CoreConfigSymbols {
            require(Files.isDirectory(directory)) {
                "Symbol directory does not exist: ${directory.toAbsolutePath()}"
            }
            return CoreConfigSymbols(
                objects = loadReversed(directory.resolve("obj.sym")),
                npcs = loadReversed(directory.resolve("npc.sym")),
                locations = loadReversed(directory.resolve("loc.sym")),
            )
        }

        private fun loadReversed(path: Path): Map<Int, String> {
            require(Files.isRegularFile(path)) {
                "Required symbol file does not exist: ${path.toAbsolutePath()}"
            }
            return NameLoader.read(path).entries.associate { (name, id) -> id to name }
        }
    }
}
