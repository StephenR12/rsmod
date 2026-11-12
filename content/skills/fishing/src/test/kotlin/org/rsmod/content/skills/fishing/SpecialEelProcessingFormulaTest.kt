package org.rsmod.content.skills.fishing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs

class SpecialEelProcessingFormulaTest {
    @Test
    fun `infernal reward table changes only at exact thirty-five-roll boundaries`() {
        assertEquals(InfernalEelReward(objs.tokkul, 14..20), rollInfernalReward(0))
        assertEquals(InfernalEelReward(objs.tokkul, 14..20), rollInfernalReward(28))
        assertEquals(InfernalEelReward(objs.onyx_bolt_tips, 1..1), rollInfernalReward(29))
        assertEquals(InfernalEelReward(objs.onyx_bolt_tips, 1..1), rollInfernalReward(30))
        assertEquals(InfernalEelReward(objs.lava_scale_shard, 1..5), rollInfernalReward(31))
        assertEquals(InfernalEelReward(objs.lava_scale_shard, 1..5), rollInfernalReward(34))
        assertThrows(IllegalArgumentException::class.java) { rollInfernalReward(-1) }
        assertThrows(IllegalArgumentException::class.java) { rollInfernalReward(35) }
    }

    @Test
    fun `sacred scale bands use boosted level boundaries and cap at one hundred four`() {
        val cases =
            mapOf(
                1 to 3..5,
                72 to 3..5,
                79 to 3..5,
                80 to 4..6,
                87 to 4..6,
                88 to 5..7,
                95 to 5..7,
                96 to 6..8,
                103 to 6..8,
                104 to 7..9,
                126 to 7..9,
            )
        cases.forEach { (level, expected) ->
            assertEquals(expected, sacredEelScaleRange(level), "Cooking level $level")
        }
    }
}
