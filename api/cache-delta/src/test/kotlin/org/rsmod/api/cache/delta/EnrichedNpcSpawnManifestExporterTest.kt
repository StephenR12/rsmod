package org.rsmod.api.cache.delta

import java.nio.ByteBuffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.cache.map.npc.MapNpcDefinition
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.map.Direction
import org.rsmod.game.movement.BlockWalk
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.type.npc.NpcTypeBuilder
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.square.MapSquareKey

class EnrichedNpcSpawnManifestExporterTest {
    @Test
    fun `emits canonical placements and stable duplicate ordinals`() {
        val square = MapSquareKey(x = 50, z = 50)
        val a = MapNpcDefinition(id = 2993, localX = 12, localZ = 9, level = 0)
        val b = MapNpcDefinition(id = 1838, localX = 4, localZ = 17, level = 1)
        val duplicate = MapNpcDefinition(id = 2993, localX = 12, localZ = 9, level = 0)
        val types =
            NpcTypeList(
                mapOf(
                    1838 to npcType(1838, "Duck", attackable = false),
                    2993 to npcType(2993, "Undead chicken", attackable = true),
                )
            )

        val forward =
            EnrichedNpcSpawnManifestExporter.entries(
                square,
                encode(listOf(a, b, duplicate)),
                mapOf(1838 to "duck", 2993 to "undead_chicken"),
                types,
            )
        val reversed =
            EnrichedNpcSpawnManifestExporter.entries(
                square,
                encode(listOf(duplicate, b, a)),
                mapOf(1838 to "duck", 2993 to "undead_chicken"),
                types,
            )

        val group = forward.single { it.domain == "map-npc-groups" }
        assertEquals("12850", group.key)
        assertEquals("3", group.attributes["placements"])
        assertEquals("2", group.attributes["distinctNpcIds"])
        assertEquals("[2, 1, 0, 0]", group.attributes["placementsByLevel"])

        val forwardSpawns = forward.filter { it.domain == "npc-spawns" }
        val reversedSpawns = reversed.filter { it.domain == "npc-spawns" }
        assertEquals(forwardSpawns, reversedSpawns)
        assertEquals(
            listOf("12850:0:12:9:2993:0", "12850:0:12:9:2993:1", "12850:1:4:17:1838:0"),
            forwardSpawns.map { it.key },
        )
        val undead = forwardSpawns.first()
        assertEquals("3212", undead.attributes["x"])
        assertEquals("3209", undead.attributes["z"])
        assertEquals("Undead chicken", undead.attributes["name"])
        assertEquals("true", undead.attributes["attackable"])
        assertEquals("undead_chicken", undead.symbol)
        assertTrue("combat-risk" in undead.riskTags)
        assertTrue("drop-table-risk" in undead.riskTags)
        val duck = forwardSpawns.last()
        assertEquals("false", duck.attributes["attackable"])
        assertTrue("combat-risk" !in duck.riskTags)
    }

    @Test
    fun `rejects malformed groups instead of silently ignoring bytes`() {
        val square = MapSquareKey(x = 50, z = 50)
        val malformed =
            encode(listOf(MapNpcDefinition(id = 1, localX = 1, localZ = 1, level = 0))) + 0
        assertThrows(IllegalArgumentException::class.java) {
            EnrichedNpcSpawnManifestExporter.entries(
                square,
                malformed,
                emptyMap(),
                NpcTypeList(emptyMap()),
            )
        }
    }

    private fun encode(placements: List<MapNpcDefinition>): ByteArray =
        ByteBuffer.allocate(2 + placements.size * 4)
            .putShort(placements.size.toShort())
            .apply { placements.forEach { putInt(it.packed) } }
            .array()

    private fun npcType(id: Int, name: String, attackable: Boolean) =
        NpcTypeBuilder(name)
            .apply {
                this.name = name
                if (attackable) {
                    op[1] = "Attack"
                    hitpoints = 5
                    attack = 1
                    attackRange = 1
                }
                moveRestrict = MoveRestrict.Normal
                defaultMode = NpcMode.None
                blockWalk = BlockWalk.Npc
                respawnDir = Direction.South
            }
            .build(id)
}
