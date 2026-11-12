package org.rsmod.content.other.combatachievements

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.rsmod.game.type.struct.StructTypeList
import org.rsmod.game.type.struct.UnpackedStructType
import org.rsmod.game.type.util.ParamMap

class CombatAchievementCatalogueTest {
    @Test
    fun `decodes cache task metadata and derives point thresholds`() {
        val catalogue =
            CombatAchievementCatalogue(
                StructTypeList(
                    mapOf(
                        10 to taskStruct(7, "Easy does it", "Defeat the boss once.", 1),
                        11 to taskStruct(8, "Harder", "Defeat the boss 10 times.", 3),
                    )
                )
            )

        assertEquals(2, catalogue.tasks.size)
        val easy = catalogue.find("easy")
        assertNotNull(easy)
        assertEquals("Easy does it", easy?.name)
        assertEquals(1, catalogue.threshold(CombatAchievementTier.Easy))
        assertEquals(4, catalogue.threshold(CombatAchievementTier.Hard))
    }

    private fun taskStruct(id: Int, name: String, description: String, tier: Int) =
        UnpackedStructType(
            paramMap =
                ParamMap(
                    mapOf(
                        1306 to id,
                        1308 to name,
                        1309 to description,
                        1310 to tier,
                        1311 to 3,
                        1312 to 1,
                    )
                ),
            internalId = id,
            internalName = null,
        )
}
