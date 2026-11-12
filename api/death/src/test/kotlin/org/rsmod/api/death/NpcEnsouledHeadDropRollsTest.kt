package org.rsmod.api.death

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.factory.npcFactory
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.api.testing.factory.objTypeFactory
import org.rsmod.api.testing.factory.playerFactory
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.game.inv.FreshDropOrigin
import org.rsmod.map.CoordGrid

class NpcEnsouledHeadDropRollsTest {
    @Test
    fun `selected head records the immutable death coordinates`() {
        val coords = CoordGrid(3_140, 3_209)
        val hero = playerFactory.create(coords)
        val npc = npcFactory.create(npcTypeFactory.create(id = 100), coords)
        val head = objTypeFactory.create(id = 13_465)
        val random = SequenceRandom().apply { next = 0 }
        val result =
            NpcEnsouledHeadDropRolls.independent(head, denominator = 35)
                .roll(NpcDropContext(npc, hero, coords, random))

        assertTrue(result.selected)
        assertEquals(head.id, result.drops.single().obj.id)
        assertEquals(coords, FreshDropOrigin.unpack(result.drops.single().obj.vars))
    }
}
