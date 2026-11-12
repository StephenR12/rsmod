package org.rsmod.content.interfaces.emotes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.params
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.stat.UnpackedStatType

class EmotesConfigTest {
    @Test
    fun GameTestState.`ensure all stats have required skillcape emotes`() {
        val anims = cacheTypes.enums[emote_enums.skill_cape_anims]
        val spots = cacheTypes.enums[emote_enums.skill_cape_spots]
        val valid = cacheTypes.stats.filterValues { !it.unreleased }
        val stats = valid.values.map(UnpackedStatType::toHashedType)
        for (stat in stats) {
            assertTrue(stat in anims) { "Seq for skill cape emote is missing: stat=$stat" }
            assertTrue(stat in spots) { "Spotanim for skill cape emote is missing: stat=$stat" }
        }
    }

    @Test
    fun GameTestState.`cache 233 ordinary skillcapes have complete emote metadata`() {
        val capes = cacheTypes.objs.values.filter { it.isContentType(content.skill_cape) }
        assertEquals(
            listOf(
                9747,
                9748,
                9750,
                9751,
                9753,
                9754,
                9756,
                9757,
                9759,
                9760,
                9762,
                9763,
                9765,
                9766,
                9768,
                9769,
                9771,
                9772,
                9774,
                9775,
                9777,
                9778,
                9780,
                9781,
                9783,
                9784,
                9786,
                9787,
                9789,
                9790,
                9792,
                9793,
                9795,
                9796,
                9798,
                9799,
                9801,
                9802,
                9804,
                9805,
                9807,
                9808,
                9810,
                9811,
                9948,
                9949,
            ),
            capes.map { it.id }.sorted(),
        )

        val capeStats = capes.groupingBy { it.param(params.statreq1_skill) }.eachCount()
        val releasedStats = cacheTypes.stats.values.filterNot { it.unreleased }
        assertEquals(releasedStats.size, capeStats.size)
        for (stat in releasedStats) {
            assertEquals(2, capeStats[stat.toHashedType()], "Cape variants for ${stat.displayName}")
        }
    }
}
