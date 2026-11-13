package org.rsmod.content.bosses.barrows

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.player.HallowedLockpick
import org.rsmod.api.player.events.SpadeDigDestination
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class BarrowsTunnelsTest {
    @Test
    fun GameTestState
        .`the held spade reaches the Ahrim crypt and its hidden passage leads to the native puzzle`() =
        runGameTest(
            org.rsmod.content.skills.prayer.remains.SoulBearer::class,
            BarrowsTunnels::class,
        ) {
            player.members = true
            player.teleport(CoordGrid(3566, 3289))
            player.inv[0] = InvObj(org.rsmod.api.config.refs.objs.spade)
            random.next = 0
            player.withProtectedAccess { opHeld1(0) }
            advance(3)
            assertEquals(CoordGrid(3557, 9703, 3), player.coords)
            player.setVarBit(cacheTypes.varbits.getValue(4734), 1)
            val sarcophagus =
                placeMapLoc(
                    CoordGrid(3555, 9698, 3),
                    locTypes.getValue(20770),
                    LocShape[10],
                    LocAngle[0],
                )
            player.opLoc1(sarcophagus)
            advance(8)
            player.resumeActiveCoroutine(
                org.rsmod.api.player.input.ResumePauseButtonInput(
                    org.rsmod.api.config.refs.components.chatmenu_pbutton,
                    1,
                )
            )
            advance(2)
            assertEquals(0, player.coords.level)
            assertTrue(
                player.coords.x in listOf(3534, 3568) && player.coords.z in listOf(9677, 9711)
            )
            val door =
                placeMapLoc(
                    CoordGrid(3545, 9694),
                    locTypes.getValue(20689),
                    LocShape[0],
                    LocAngle[2],
                )
            player.setVarBit(cacheTypes.varbits.getValue(477), 0)
            player.teleport(CoordGrid(3545, 9694))
            random.next = 0
            player.opLoc1(door)
            advance(2)
            player.ifButton(
                cacheTypes.components.values.single { it.internalName == "barrows_puzzle:a" }
            )
            advance(2)
            assertEquals(CoordGrid(3546, 9694), player.coords)
        }

    @Test
    fun GameTestState
        .`mounds enter crypts and a blocked tunnel door spends exactly one lockpick charge`() =
        runGameTest(BarrowsTunnels::class) {
            player.members = true
            val dig = SpadeDigDestination(player, CoordGrid(3566, 3289))
            eventBus.publish(dig)
            assertEquals(CoordGrid(3557, 9703, 3), dig.destination)
            val entrance = cacheTypes.varbits.getValue(4734)
            assertTrue(player.vars[entrance] in 1..6)
            val door =
                placeMapLoc(
                    CoordGrid(3541, 9712),
                    locTypes.getValue(20684),
                    LocShape[0],
                    LocAngle[0],
                )
            val bit = cacheTypes.varbits.getValue(472)
            player.setVarBit(bit, 1)
            val near = CoordGrid(3540, 9712)
            player.teleport(near)
            player.opLoc1(door)
            advance(3)
            assertEquals(near, player.coords)
            fun use() {
                val item = objTypes[player.inv[2]!!]
                player.withProtectedAccess {
                    check(
                        eventBus.publish(this, LocUEvents.Op(door, door, locTypes[door], item, 2))
                    )
                }
            }
            player.inv[2] = InvObj(HallowedLockpick.full)
            use()
            assertEquals(CoordGrid(3541, 9712), player.coords)
            assertEquals(49, HallowedLockpick.charges(player.inv[2]))
            player.setVarBit(bit, 0)
            use()
            assertEquals(near, player.coords)
            assertEquals(49, HallowedLockpick.charges(player.inv[2]))
            player.setVarBit(bit, 1)
            player.inv[2] = InvObj(HallowedLockpick.used, 1, 1)
            use()
            assertNull(player.inv[2])
        }
}
