package org.rsmod.content.minigames.giantsfoundry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.righthand
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class GiantsFoundryTest {
    private fun GameTestScope.click(id: Int) {
        val type = locTypes.getValue(id)
        val loc = placeMapLoc(player.coords.translate(1, 0), type)
        player.withProtectedAccess { eventBus.publish(this, LocEvents.Op1(loc, loc, type)) }
    }

    private fun GameTestScope.prepare() {
        player.members = true
        player.observerUUID = player.uuid
        player.placeAt(CoordGrid(3359, 3149))
        player.stats[stats.smithing] = 99
    }

    @Test
    fun GameTestState
        .`commission makes a 28-bar alloy sword and spends its reward through the native shop`() =
        runInjectedGameTest(GiantsFoundry::class, null, GiantsFoundry::class, FoundryShop::class) {
            game ->
            prepare()
            player.setVarBit(cacheTypes.varbits.getValue(13902), 30)
            fun bit(id: Int) = player.vars[cacheTypes.varbits.getValue(id)]
            click(44635)
            assertEquals(CoordGrid(3363, 11482), player.coords)
            val kovac = spawnNpc(player.coords.translate(-1, 0), npcTypes.getValue(11472))
            player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op3(kovac)) }
            assertTrue(bit(13907) in 1..6)
            assertTrue(bit(13908) in 1..6)
            click(44777)
            assertTrue(player.ui.containsModal(cacheTypes.interfaces.getValue(718)))
            fun button(
                ui: Int,
                component: Int,
                child: Int? = null,
                op: IfButtonOp = IfButtonOp.Op1,
            ) {
                player.ifButton(cacheTypes.components.getValue((ui shl 16) or component), child, op)
                advance()
            }
            for (i in 0..2) {
                button(718, 8, i * 9)
                button(718, 9, 10 * 17)
                assertEquals(11, bit(13910 + i))
            }
            button(718, 6)
            assertEquals(1, bit(13914))
            repeat(14) {
                player.inv[it] = InvObj(objs.adamantite_bar)
                player.inv[it + 14] = InvObj(objs.runite_bar)
            }
            click(44776)
            assertEquals(130, bit(13913))
            click(44776)
            assertEquals(130, bit(13938))
            assertEquals(2, bit(13914))
            click(44777)
            assertNull(player.righthand)
            player.inv[0] = InvObj(objs.bucket_water)
            click(44777)
            assertEquals(27010, player.righthand?.id)
            assertEquals(objs.bucket_empty.id, player.inv[0]?.id)
            assertTrue(player.activityBlocksTeleport)
            val initial = bit(13950)
            player.withProtectedAccess {
                assertFalse(game.handIn(this))
                assertFalse(game.refine(this, 2))
            }
            assertEquals(initial - 10, bit(13939))
            click(44778)
            assertEquals(1, bit(13947))
            assertFalse(player.activityBlocksTeleport)
            click(44778)
            assertTrue(player.activityBlocksTeleport)
            var actions = 0
            while (bit(13949) < 1000 && actions++ < 150) {
                val section = bit(13949) * 7 / 1000
                val tool = bit(if (section == 0) 13946 else 13939 + section)
                val target = listOf(832, 499, 166)[tool]
                player.withProtectedAccess {
                    while (bit(13948) != target) game.adjustHeat(this, target - bit(13948))
                    assertTrue(game.refine(this, tool))
                }
            }
            assertEquals(1000, bit(13949))
            val q = bit(13939)
            player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op2(kovac)) }
            val xp = (q * q / 73 + 3 * q / 2 + 1) * 30
            assertEquals(2 * xp, player.invMap.getValue(invs.bank).count(objTypes[objs.coins]))
            assertNull(player.righthand)
            assertFalse(player.activityBlocksTeleport)
            assertEquals(q, player.vars[cacheTypes.varps.getValue(3436)])
            player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op4(kovac)) }
            assertTrue(player.ui.containsModal(cacheTypes.interfaces.getValue(753)))
            button(753, 22, 2 * 13, IfButtonOp.Op2)
            assertEquals(1, player.inv.count(objTypes.getValue(27017)))
            assertEquals(q - 15, player.vars[cacheTypes.varps.getValue(3436)])
            player.ifClose()
            advance()
            click(44636)
            assertEquals(CoordGrid(3359, 3149), player.coords)
        }

    @Test
    fun GameTestState.`access level and metal validation preserve unaccepted materials`() =
        runInjectedGameTest(GiantsFoundry::class, null, GiantsFoundry::class, FoundryShop::class) {
            game ->
            prepare()
            player.setVarBit(cacheTypes.varbits.getValue(13902), 30)
            fun bit(id: Int) = player.vars[cacheTypes.varbits.getValue(id)]
            player.setVarBit(cacheTypes.varbits.getValue(13902), 29)
            click(44635)
            assertEquals(CoordGrid(3359, 3149), player.coords)
            player.inv[0] = InvObj(objs.runite_bar)
            player.withProtectedAccess {
                assertFalse(game.commission(this))
                assertEquals(0, game.fill(this))
            }
            player.setVarBit(cacheTypes.varbits.getValue(13902), 30)
            player.stats[stats.smithing] = 15
            player.inv[1] = InvObj(objTypes.getValue(1115)) // Iron platebody, five bars to smith.
            player.inv[2] = InvObj(objTypes.getValue(1203)) // Iron dagger, one bar to smith.
            player.withProtectedAccess {
                assertEquals(4, game.fill(this))
                assertFalse(game.selectMould(this, 0, 1))
                assertFalse(game.setMould(this))
                assertFalse(game.pour(this))
            }
            assertEquals(4, bit(13932))
            assertEquals(objs.runite_bar.id, player.inv[0]?.id)
            assertEquals(1203, player.inv[2]?.id)
        }

    @Test
    fun GameTestState
        .`reward shop rejects unaffordable or full inventory purchases and unlocks mould once`() =
        runInjectedGameTest(FoundryShop::class, null, GiantsFoundry::class, FoundryShop::class) {
            shop ->
            prepare()
            player.setVarBit(cacheTypes.varbits.getValue(13902), 30)
            fun bit(id: Int) = player.vars[cacheTypes.varbits.getValue(id)]
            player.setVarp(cacheTypes.varps.getValue(3436), 400)
            player.stats[stats.smithing] = 15
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.hammer)
            player.withProtectedAccess {
                assertFalse(shop.purchase(this, 1, 1))
                assertFalse(shop.purchase(this, 3, 1))
                assertTrue(shop.purchase(this, 10, 1))
                assertFalse(shop.purchase(this, 10, 1))
            }
            assertEquals(100, player.vars[cacheTypes.varps.getValue(3436)])
            assertEquals(1, bit(13916))
        }

    @Test
    fun GameTestState
        .`hot preform cools in water works on a timed trip hammer and cannot be swapped for a weapon`() =
        runInjectedGameTest(HeldEquipOp::class, null, GiantsFoundry::class) { equipment ->
            prepare()
            player.setVarBit(cacheTypes.varbits.getValue(13902), 30)
            for ((bit, value) in
                listOf(
                    13950 to 60,
                    13939 to 50,
                    13938 to 10,
                    13948 to 1000,
                    13914 to 2,
                    13946 to 0,
                )) {
                player.setVarBit(cacheTypes.varbits.getValue(bit), value)
            }
            player.inv[0] = InvObj(objs.bucket_water)
            click(44777)
            player.inv[1] = InvObj(objTypes.getValue(1205))
            assertTrue(equipment.equip(player, 1, player.inv) is HeldEquipResult.Fail)
            assertEquals(27010, player.righthand?.id)
            click(44632)
            advance(4)
            player.clearPendingAction()
            val heat = player.vars[cacheTypes.varbits.getValue(13948)]
            assertTrue(heat in 850..980, "cooled to $heat")
            click(44619)
            advance(5)
            assertEquals(20, player.vars[cacheTypes.varbits.getValue(13949)])
            player.clearPendingAction()
            click(44778)
            val saved = player.vars[cacheTypes.varbits.getValue(13948)]
            advance(15)
            assertEquals(saved, player.vars[cacheTypes.varbits.getValue(13948)])
        }

    @Test
    fun GameTestState.`outfit gloves combine with ice gloves in the two occupied slots`() =
        runGameTest(FoundryShop::class) {
            val smiths = objTypes.getValue(27029)
            val ice = objTypes[objs.ice_gloves]
            player.inv[0] = InvObj(smiths)
            player.inv[1] = InvObj(ice)
            player.withProtectedAccess {
                eventBus.publish(this, HeldUEvents.Type(smiths, 0, ice, 1))
            }
            assertEquals(27031, player.inv[0]?.id)
            assertNull(player.inv[1])
        }

    @Test
    fun GameTestState.`ore pack holds thirty noted ores and grog applies Smithing boost`() =
        runGameTest(FoundryShop::class) {
            prepare()
            player.setVarBit(cacheTypes.varbits.getValue(13902), 30)
            fun bit(id: Int) = player.vars[cacheTypes.varbits.getValue(id)]
            for ((slot, id) in listOf(27019, 27014).withIndex()) {
                val type = objTypes.getValue(id)
                val obj = InvObj(type)
                player.inv[slot] = obj
                player.withProtectedAccess {
                    eventBus.publish(this, HeldObjEvents.Op1(slot, obj, type, inv))
                }
                if (slot == 0)
                    assertEquals(
                        30,
                        listOf(454, 441, 448, 450, 452).sumOf {
                            player.inv.count(objTypes.getValue(it))
                        },
                    )
            }
            assertEquals(1919, player.inv[1]?.id)
            assertEquals(103, player.stats[stats.smithing])
        }
}
