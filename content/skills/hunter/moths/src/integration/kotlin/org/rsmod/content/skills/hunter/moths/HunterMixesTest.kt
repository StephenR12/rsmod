package org.rsmod.content.skills.hunter.moths

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class HunterMixesTest {
    @Test
    fun GameTestState
        .`mixing consumes the selected raw meat and both doses share with consenting neighbours`() =
        runGameTest(HunterMixes::class) {
            player.members = true
            player.placeAt(CoordGrid(3200, 3200))
            val moth = objTypes.getValue(28893)
            val meat = objTypes[objs.raw_kyatt]
            player.inv[0] = InvObj(moth)
            player.inv[1] = InvObj(meat)
            fun mix() =
                player.withProtectedAccess {
                    eventBus.publish(this, HeldUEvents.Type(moth, 0, meat, 1))
                }
            mix()
            assertEquals(moth.id, player.inv[0]?.id)
            player.inv[2] = InvObj(objs.pestle_and_mortar)
            mix()
            advance(3)
            assertEquals(29195, player.inv[0]?.id)
            assertNull(player.inv[1])
            player.setVarBit(varbits.multiway_indicator, 1)
            player.setBaseLevel(stats.prayer, 99)
            player.setCurrentLevel(stats.prayer, 1)
            val neighbours =
                (0..3).map {
                    registerPlayer(coords = player.coords.translate(1, 1)).also { p ->
                        p.setBaseLevel(stats.prayer, 99)
                        p.setCurrentLevel(stats.prayer, 1)
                        p.setVarBit(varbits.multiway_indicator, 1)
                        p.setVarBit(varbits.option_acceptaid, 1)
                    }
                }
            val noAid = registerPlayer(coords = player.coords.translate(1, 0))
            noAid.setBaseLevel(stats.prayer, 99)
            noAid.setCurrentLevel(stats.prayer, 1)
            noAid.setVarBit(varbits.multiway_indicator, 1)
            fun drink() {
                val source = requireNotNull(player.inv[0])
                player.withProtectedAccess {
                    eventBus.publish(this, HeldObjEvents.Op1(0, source, objTypes[source], inv))
                }
            }
            drink()
            assertEquals(29213, player.inv[0]?.id)
            assertEquals(23, player.stat(stats.prayer))
            assertEquals(3, neighbours.count { it.stat(stats.prayer) == 23 })
            assertEquals(1, noAid.stat(stats.prayer))
            drink()
            assertEquals(29213, player.inv[0]?.id)
            advance(3)
            drink()
            assertEquals(MothObjs.emptyJar.id, player.inv[0]?.id)
            assertEquals(45, player.stat(stats.prayer))
        }
}
