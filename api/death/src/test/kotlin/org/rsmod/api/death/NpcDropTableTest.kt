package org.rsmod.api.death

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.firstSelected
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropRolls.repeated
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.testing.factory.npcFactory
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.api.testing.factory.objTypeFactory
import org.rsmod.api.testing.factory.playerFactory
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.map.CoordGrid

class NpcDropTableTest {
    @Test
    fun `sequence composes guaranteed weighted tertiary conditional and repeated rolls`() {
        val random = SequenceRandom()
        random.next = 2 // Select the weighted nothing entry.
        random.then = 0 // Hit the independent 1/4 tertiary.
        random.then = 2 // First repeated quantity.
        random.then = 3 // Second repeated quantity.
        val context = context(random)
        val table =
            NpcDropTable(
                sequence(
                    item(always),
                    weighted(NpcWeightedDrop(1, item(main)), NpcWeightedDrop(2, nothing())),
                    independent(denominator = 4, roll = item(tertiary)),
                    conditional({ it.hero === context.hero }, item(conditional)),
                    repeated(times = 2, item(repeated, count = 1..3)),
                )
            )

        val drops = table.roll(context)

        assertEquals(
            listOf(always.id, tertiary.id, conditional.id, repeated.id, repeated.id),
            drops.map { it.obj.id },
        )
        assertEquals(listOf(1, 1, 1, 2, 3), drops.map { it.obj.count })
        assertTrue(drops.all { it.coords == context.coords })
        assertTrue(drops.all { it.receiver === context.hero })
    }

    @Test
    fun `ordered rolls stop after selected empty pre-roll`() {
        val random = SequenceRandom()
        random.next = 1 // Miss the first 1/2 pre-roll.
        random.then = 0 // Select the second pre-roll, which intentionally emits nothing.
        val context = context(random)
        var fallbackCalled = false
        val fallback = NpcDropRoll {
            fallbackCalled = true
            item(main).roll(it)
        }
        val ordered =
            firstSelected(
                independent(denominator = 2, roll = item(tertiary)),
                independent(denominator = 2, roll = nothing()),
                fallback,
            )

        val result = ordered.roll(context)

        assertTrue(result.selected)
        assertTrue(result.drops.isEmpty())
        assertFalse(fallbackCalled)
    }

    @Test
    fun `sequence preserves ordered feedback from selected empty rolls`() {
        val context = context(SequenceRandom())
        val feedback = NpcDropRoll {
            NpcDropRollResult(
                selected = true,
                drops = emptyList(),
                messages = listOf("first", "second"),
            )
        }
        val table = NpcDropTable(sequence(item(always), feedback, item(main)))

        val result = table.resolve(context)

        assertEquals(listOf(always.id, main.id), result.drops.map { it.obj.id })
        assertEquals(listOf("first", "second"), result.messages)
    }

    @Test
    fun `weighted boundaries select the matching nested branch`() {
        val context = context(SequenceRandom().apply { next = 3 })
        val table =
            NpcDropTable(
                weighted(
                    NpcWeightedDrop(1, item(always)),
                    NpcWeightedDrop(3, item(main)),
                    NpcWeightedDrop(2, item(tertiary)),
                )
            )

        assertEquals(main.id, table.roll(context).single().obj.id)
    }

    @Test
    fun `standard herb table has exact 128 weight boundaries`() {
        val herbs = (301..311).map { objTypeFactory.create(id = it) }
        val weights = listOf(32, 24, 18, 14, 11, 8, 6, 5, 4, 3, 3)
        val table = NpcDropTable(NpcHerbDropRolls.standard(herbs))

        var lowerBound = 0
        herbs.zip(weights).forEach { (herb, weight) ->
            val lower = SequenceRandom().apply { next = lowerBound }
            val upper = SequenceRandom().apply { next = lowerBound + weight - 1 }
            assertEquals(herb.id, table.roll(context(lower)).single().obj.id)
            assertEquals(herb.id, table.roll(context(upper)).single().obj.id)
            lowerBound += weight
        }

        assertEquals(128, lowerBound)
    }

    @Test
    fun `rare bone table is members only and rolls curved only after long misses`() {
        val random = SequenceRandom()
        val context = context(random)
        val table = NpcDropTable(NpcRareBoneDropRolls.longOrCurved(longBone, curvedBone))

        context.hero.members = false
        random.next = 0
        assertTrue(table.roll(context).isEmpty())
        random.reset()

        context.hero.members = true
        random.next = 0
        assertEquals(listOf(longBone.id), table.roll(context).map { it.obj.id })

        random.next = NpcRareBoneDropRolls.LONG_BONE_DENOMINATOR - 1
        random.then = 0
        assertEquals(listOf(curvedBone.id), table.roll(context).map { it.obj.id })

        random.next = NpcRareBoneDropRolls.LONG_BONE_DENOMINATOR - 1
        random.then = NpcRareBoneDropRolls.CURVED_BONE_DENOMINATOR - 1
        assertTrue(table.roll(context).isEmpty())
    }

    @Test
    fun `item roll can place a public drop away from the death tile`() {
        val context = context(SequenceRandom())
        val table =
            NpcDropTable(
                item(
                    always,
                    receiver = { null },
                    placement = { it.coords.translate(xOffset = -1, zOffset = 2) },
                    duration = 250,
                    reveal = 0,
                )
            )

        val drop = table.roll(context).single()

        assertEquals(context.coords.translate(xOffset = -1, zOffset = 2), drop.coords)
        assertEquals(250, drop.duration)
        assertEquals(0, drop.reveal)
        assertEquals(null, drop.receiver)
    }

    @Test
    fun `invalid chance weight quantity and repetition definitions are rejected`() {
        assertThrows<IllegalArgumentException> { independent(denominator = 0, roll = nothing()) }
        assertThrows<IllegalArgumentException> {
            independent(numerator = 3, denominator = 2, roll = nothing())
        }
        assertThrows<IllegalArgumentException> { NpcWeightedDrop(0, nothing()) }
        assertThrows<IllegalArgumentException> { weighted(emptyList()) }
        assertThrows<IllegalArgumentException> { item(always, count = 0..1) }
        assertThrows<IllegalArgumentException> { repeated(times = 0, nothing()) }
    }

    @Test
    fun `registry rejects duplicates and leaves family registration atomic`() {
        val registry = NpcDropTableRegistry()
        val first = npcTypeFactory.create(id = 101)
        val second = npcTypeFactory.create(id = 102)
        val third = npcTypeFactory.create(id = 103)
        val table = NpcDropTable(nothing())

        registry.register(first, table)

        assertThrows<IllegalStateException> { registry.register(listOf(second, first), table) }
        assertFalse(registry.contains(second))
        assertThrows<IllegalArgumentException> { registry.register(listOf(third, third), table) }
        assertFalse(registry.contains(third))
        assertSame(table, registry[first])
        assertEquals(setOf(101), registry.registeredTypeIds())
    }

    private fun context(random: SequenceRandom): NpcDropContext {
        val coords = CoordGrid(3200, 3200)
        val hero = playerFactory.create(coords)
        val npc = npcFactory.create(npcTypeFactory.create(id = 100), coords)
        return NpcDropContext(npc, hero, coords, random)
    }

    private companion object {
        private val always = objTypeFactory.create(id = 201)
        private val main = objTypeFactory.create(id = 202)
        private val tertiary = objTypeFactory.create(id = 203)
        private val conditional = objTypeFactory.create(id = 204)
        private val repeated = objTypeFactory.create(id = 205)
        private val longBone = objTypeFactory.create(id = 206)
        private val curvedBone = objTypeFactory.create(id = 207)
    }
}
