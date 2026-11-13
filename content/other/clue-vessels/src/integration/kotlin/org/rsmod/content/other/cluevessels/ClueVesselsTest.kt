package org.rsmod.content.other.cluevessels

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.rsmod.api.clue.*
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

class ClueVesselsTest {
    class Dependencies @Inject constructor(val inventory: ClueInventory, val ground: ObjRepository, val random: GameRandom)

    @Test fun GameTestState.catchRollsUseTierOwnershipAndMinedOreIsReplacedOnlyOnAnAward() =
        runInjectedGameTest(Dependencies::class, null, OpenClueVessels::class) { deps ->
            val vessels = ClueVessels(deps.inventory, objTypes, deps.ground, deps.random)
            player.members = false
            val bottle = objTypes.getValue(SkillingClue.Beginner.bottle)
            val denominator = SkillingClue.Beginner.denominator(player, 435_165, 1)
            assertEquals(861, denominator)
            random.next = denominator - 1
            assertTrue(vessels.bottles(player, 1, 435_165))
            player.withProtectedAccess { opHeld1(inv.indexOfFirst { it?.id == bottle.id }) }
            assertEquals(1, player.count(objs.clue_scroll_beginner))
            val geodeChance = SkillingClue.Beginner.denominator(player, 741_600 * 7 / 10, 1)
            random.next = geodeChance - 1
            assertFalse(vessels.geodes(player, 1, 741_600))
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            random.next = geodeChance - 1
            assertTrue(vessels.geodes(player, 1, 741_600))
            assertEquals(1, player.count(ClueTier.Beginner.scrollBox))
            player.withProtectedAccess { opHeld1(inv.indexOfFirst { it?.id == ClueTier.Beginner.scrollBox.id }) }
            assertEquals(1, player.count(ClueTier.Beginner.scrollBox))
            player.setVarBit(varbits.vesseled_clues_disabled, 1)
            random.next = geodeChance - 1
            assertFalse(vessels.geodes(player, 1, 741_600))
        }

    @Test fun GameTestState.nativeVesselsGiveTheirTierAndPostQuestBoxesSplitWithinCapacity() =
        runGameTest(OpenClueVessels::class) {
            player.members = true
            for (entry in SkillingClue.entries) {
                val type = objTypes.getValue(entry.geode)
                player.inv[0] = InvObj(type)
                player.withProtectedAccess { opHeld1(0) }
                val result = objTypes[player.inv.getValue(0)]
                assertEquals("Clue scroll (${entry.tier.key})", result.name)
                player.inv[0] = null
            }
            player.inv[0] = InvObj(objTypes.getValue(SkillingClue.Easy.nest))
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(1, player.count(objs.empty_bird_nest))
            assertEquals(1, player.count(ClueTier.Easy.scrollBox))
            player.inv.fillNulls()
            player.fillInv(with=InvObj(objs.logs))
            player.inv[0] = InvObj(ClueTier.Easy.scrollBox, 2)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(2, player.count(ClueTier.Easy.scrollBox))
            player.inv[1] = null
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(1, player.count(ClueTier.Easy.scrollBox))
            assertEquals("Clue scroll (easy)", objTypes[player.inv.getValue(1)].name)
        }
}
