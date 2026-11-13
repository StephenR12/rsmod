package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

class TreeSeedlingsTest {
    @Test
    fun GameTestState
        .`tree seeds require a trowel and the resulting watered seedlings grow in the bank`() =
        runInjectedGameTest(TreeSeedlings::class, null, TreeSeedlings::class) { seedlings ->
            player.setCurrentLevel(stats.farming, 15)
            fun obj(name: String) = objTypes.values.single { it.internalName == name }
            fun use(first: Int, second: Int) {
                player.withProtectedAccess {
                    check(
                        eventBus.publish(
                            this,
                            HeldUEvents.Type(
                                objTypes[inv.getValue(first)],
                                first,
                                objTypes[inv.getValue(second)],
                                second,
                            ),
                        )
                    )
                }
            }
            player.inv[0] = InvObj(obj("acorn"), 2)
            player.inv[1] = InvObj(obj("plantpot_compost"))
            use(0, 1)
            assertEquals(2, player.inv.getValue(0).count)
            player.inv[2] = InvObj(obj("gardening_trowel"))
            use(0, 1)
            assertEquals(obj("plantpot_acorn").id, player.inv.getValue(1).id)
            assertEquals(1, player.inv.getValue(0).count)
            player.inv[3] = InvObj(obj("watering_can_2"))
            use(3, 1)
            assertEquals(obj("watering_can_1").id, player.inv.getValue(3).id)
            assertEquals(obj("plantpot_acorn_watered").id, player.inv.getValue(1).id)
            player.withProtectedAccess {
                bank[0] = InvObj(obj("plantpot_acorn_watered"), 2)
                bank[1] = InvObj(obj("plantpot_oak_sapling"), 3)
            }
            seedlings.mature(player, System.currentTimeMillis() + 301_000L)
            assertEquals(obj("plantpot_oak_sapling").id, player.inv.getValue(1).id)
            assertEquals(5, player.invMap.getValue(invs.bank).getValue(1).count)
            seedlings.mature(player, System.currentTimeMillis() + 601_000L)
            assertEquals(5, player.invMap.getValue(invs.bank).getValue(1).count)
        }
}
