package org.rsmod.api.death

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.player.vars.VarPlayerIntMapDelegate
import org.rsmod.api.testing.factory.npcFactory
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.api.testing.factory.objTypeFactory
import org.rsmod.api.testing.factory.playerFactory
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.type.varbit.VarBitTypeBuilder
import org.rsmod.game.type.varp.VarpType
import org.rsmod.game.type.varp.VarpTypeBuilder
import org.rsmod.map.CoordGrid

class NpcChampionScrollDropRollsTest {
    private val coords = CoordGrid(3_217, 3_226)
    private val questPoints = VarpTypeBuilder("qp").build(101)
    private val championState = VarpTypeBuilder("champions_defeated").build(617)
    private val completed =
        VarBitTypeBuilder("champions_defeated_imp")
            .apply {
                baseVar = 617
                varp = championState
                lsb = 5
                msb = 5
            }
            .build(1_457)
    private val scroll = objTypeFactory.create(id = 6_803)
    private val npcType = npcTypeFactory.create(id = 5_007)

    @Test
    fun `membership and 32 quest points gate the chance roll`() {
        val hero = playerFactory.create(coords)
        val random = SequenceRandom()
        val roll = roll()

        hero.members = false
        hero.setVarp(questPoints, 32)
        assertFalse(roll.roll(context(hero, random)).selected)

        hero.members = true
        hero.setVarp(questPoints, 31)
        assertFalse(roll.roll(context(hero, random)).selected)
    }

    @Test
    fun `successful roll drops repeat scrolls before champion completion`() {
        val hero = playerFactory.create(coords)
        hero.members = true
        hero.setVarp(questPoints, 32)
        val random = SequenceRandom().apply { next = 0 }

        val result = roll().roll(context(hero, random))

        assertTrue(result.selected)
        assertEquals(listOf(scroll.id), result.drops.map { it.obj.id })
        assertEquals(hero, result.drops.single().receiver)
        assertEquals(listOf(NpcChampionScrollDropRolls.DROP_MESSAGE), result.messages)
    }

    @Test
    fun `successful roll after completion emits exact feedback without a drop`() {
        val hero = playerFactory.create(coords)
        hero.members = true
        hero.setVarp(questPoints, 32)
        hero.setVarBit(completed, 1)
        val random = SequenceRandom().apply { next = 0 }

        val result = roll().roll(context(hero, random))

        assertTrue(result.selected)
        assertTrue(result.drops.isEmpty())
        assertEquals(listOf(NpcChampionScrollDropRolls.DUPLICATE_MESSAGE), result.messages)
    }

    private fun roll() = NpcChampionScrollDropRolls.independent(scroll, questPoints, completed)

    private fun context(hero: Player, random: SequenceRandom): NpcDropContext {
        val npc = npcFactory.create(npcType, coords)
        return NpcDropContext(npc, hero, coords, random)
    }

    private fun Player.setVarp(type: VarpType, value: Int) {
        VarPlayerIntMapDelegate(client, vars, engineLoggedIn = false)[type] = value
    }

    private fun Player.setVarBit(type: VarBitType, value: Int) {
        VarPlayerIntMapDelegate(client, vars, engineLoggedIn = false)[type] = value
    }
}
