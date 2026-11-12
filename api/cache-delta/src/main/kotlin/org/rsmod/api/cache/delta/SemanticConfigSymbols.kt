package org.rsmod.api.cache.delta

import java.nio.file.Files
import java.nio.file.Path
import org.rsmod.api.type.symbols.name.NameLoader

public data class SemanticConfigSymbols(
    public val core: CoreConfigSymbols,
    public val inventories: Map<Int, String>,
    public val parameters: Map<Int, String>,
    public val enums: Map<Int, String>,
    public val sequences: Map<Int, String>,
    public val spotAnimations: Map<Int, String>,
    public val projectiles: Map<Int, String>,
    public val varps: Map<Int, String>,
    public val varbits: Map<Int, String>,
    public val varClients: Map<Int, String>,
    public val varObjects: Map<Int, String>,
    public val structs: Map<Int, String>,
    public val databaseTables: Map<Int, String>,
    public val databaseRows: Map<Int, String>,
) {
    public companion object {
        public fun load(directory: Path): SemanticConfigSymbols {
            require(Files.isDirectory(directory)) {
                "Symbol directory does not exist: ${directory.toAbsolutePath()}"
            }
            return SemanticConfigSymbols(
                core = CoreConfigSymbols.load(directory),
                inventories = loadReversed(directory.resolve("inv.sym")),
                parameters = loadReversed(directory.resolve("param.sym")),
                enums = loadReversed(directory.resolve("enum.sym")),
                sequences = loadReversed(directory.resolve("seq.sym")),
                spotAnimations = loadReversed(directory.resolve("spotanim.sym")),
                projectiles = loadReversed(directory.resolve("projanim.sym")),
                varps = loadReversed(directory.resolve("varp.sym")),
                varbits = loadReversed(directory.resolve("varbit.sym")),
                varClients = loadReversed(directory.resolve("varc.sym")),
                varObjects = loadReversed(directory.resolve("varobj.sym")),
                structs = loadReversed(directory.resolve("struct.sym")),
                databaseTables = loadReversed(directory.resolve("dbtable.sym")),
                databaseRows = loadReversed(directory.resolve("dbrow.sym")),
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
