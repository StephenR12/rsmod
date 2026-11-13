package org.rsmod.api.death

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.type.npc.NpcType

/** Runtime mapping from resolved cache NPC identities to their revision-specific drop tables. */
@Singleton
public class NpcDropTableRegistry @Inject constructor() {
    private val tables = mutableMapOf<Int, NpcDropTable>()

    private val tertiaryTables = mutableMapOf<Int, NpcDropRoll>()

    /**
     * Adds a tertiary roll independently of the NPC's primary table and script registration order.
     */
    public fun registerTertiary(type: NpcType, roll: NpcDropRoll) {
        val previous = tertiaryTables[type.id]
        tertiaryTables[type.id] =
            if (previous == null) roll else NpcDropRolls.sequence(previous, roll)
    }

    public fun register(type: NpcType, table: NpcDropTable) {
        register(listOf(type), table)
    }

    /** Registers one shared table for an NPC family as a single atomic operation. */
    public fun register(types: Iterable<NpcType>, table: NpcDropTable) {
        val ids = types.map { it.id }
        require(ids.isNotEmpty()) { "At least one NPC type must be registered." }
        require(ids.distinct().size == ids.size) { "Duplicate NPC types in registration: $ids" }
        val conflicts = ids.filter(tables::containsKey)
        check(conflicts.isEmpty()) { "NPC drop tables already registered: $conflicts" }
        ids.forEach { tables[it] = table }
    }

    public operator fun get(type: NpcType): NpcDropTable? {
        val main = tables[type.id]
        val tertiary = tertiaryTables[type.id] ?: return main
        return if (main == null) NpcDropTable(tertiary)
        else NpcDropTable(NpcDropRolls.sequence(NpcDropRoll(main::resolve), tertiary))
    }

    public fun contains(type: NpcType): Boolean = type.id in tables || type.id in tertiaryTables

    /** Stable cache IDs exposed for parity inventory and cache-contract tests. */
    public fun registeredTypeIds(): Set<Int> = (tables.keys + tertiaryTables.keys).toSortedSet()
}
