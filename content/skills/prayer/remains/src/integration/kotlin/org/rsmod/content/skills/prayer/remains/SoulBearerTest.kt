package org.rsmod.content.skills.prayer.remains

import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.*
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.content.interfaces.bank.bankCapacity
import org.rsmod.game.inv.FreshDropOrigin
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class SoulBearerTest {
    @Test
    fun GameTestState
        .`the book author crypt and Key Master repair a bearer and restrict replacement`() =
        runGameTest(SoulBearer::class) {
            val crypt = CoordGrid(1699, 3794)
            player.members = true
            player.placeAt(crypt)
            player.inv[0] = InvObj(objTypes.getValue(19637))
            player.inv[1] = InvObj(objs.spade)
            fun dig() {
                player.withProtectedAccess { opHeld1(1) }
                advance(2)
            }
            dig()
            assertEquals(0, player.count(objTypes.getValue(19636)))
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(5078)])
            val author = spawnNpc(crypt.translate(1, 0), npcTypes.getValue(7042))
            player.opNpc1(author)
            advance()
            assertEquals(2, player.vars[cacheTypes.varbits.getValue(5078)])
            player.placeAt(CoordGrid(1699, 3794, 1))
            dig()
            assertEquals(0, player.count(objTypes.getValue(19636)))
            player.placeAt(crypt)
            dig()
            dig()
            assertEquals(1, player.count(objTypes.getValue(19636)))
            val keeper = spawnNpc(crypt.translate(0, 1), npcTypes.getValue(12599))
            player.opNpc1(keeper)
            advance()
            assertEquals(3, player.vars[cacheTypes.varbits.getValue(5078)])
            assertEquals(1, player.count(objTypes.getValue(19634)))
            player.inv[2] = null
            dig()
            assertEquals(1, player.count(objTypes.getValue(19634)))
        }

    @Test
    fun GameTestState
        .`bank capacity and item provenance are enforced per successfully moved head`() =
        runGameTest(SoulBearer::class) {
            player.members = true
            player.bankCapacity = 1
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            bank[0] = InvObj(objs.coins)
            player.setVarBit(BankTab.Main.sizeVarBit, 1)
            val head =
                InvObj(
                    objs.ensouled_unicorn_head_initial,
                    vars = FreshDropOrigin.pack(CoordGrid(3100, 3100)),
                )
            player.inv[0] = InvObj(objTypes.getValue(19634), vars = 2)
            player.inv[1] = head
            player.inv[2] = InvObj(objs.ensouled_unicorn_head)
            player.withProtectedAccess { opHeld1(0) }
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 2))
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65735)])
            assertEquals(2, player.inv[0]?.vars)
            assertEquals(head, player.inv[1])
            player.ifClose()
            advance()
            player.bankCapacity = 2
            player.withProtectedAccess { opHeld1(0) }
            assertMessageSent("You send 2 ensouled heads to your bank.")
            assertEquals(0, player.inv[0]?.vars)
            assertNull(player.inv[1])
            assertNull(player.inv[2])
            assertEquals(InvObj(objs.ensouled_unicorn_head, 2), bank[1])
        }

    @Test
    fun GameTestState
        .`runes are consumed as pairs for manual and bank charging and refunded together`() =
        runInjectedGameTest(HeldUInteractions::class, null, SoulBearer::class) { interact ->
            player.members = true
            player.inv[0] = InvObj(objTypes.getValue(19634), vars = 998)
            player.inv[1] = InvObj(objs.blood_rune, 3)
            player.inv[2] = InvObj(objs.soul_rune, 5)
            player.withProtectedAccess {
                interact.interact(
                    this,
                    inv,
                    objTypes[objs.blood_rune],
                    1,
                    objTypes[objTypes.getValue(19634)],
                    0,
                )
            }
            player.resumeActiveCoroutine(ResumePCountDialogInput(99))
            assertEquals(1000, player.inv[0]?.vars)
            assertEquals(1, player.count(objs.blood_rune))
            assertEquals(3, player.count(objs.soul_rune))
            for (i in 1..27) player.inv[i] = InvObj(objs.hammer)
            player.withProtectedAccess { opHeld4(0) }
            assertEquals(1000, player.inv[0]?.vars)
            player.inv[1] = null
            player.inv[2] = null
            player.withProtectedAccess { opHeld4(0) }
            assertEquals(0, player.inv[0]?.vars)
            assertEquals(1000, player.count(objs.soul_rune))
            assertEquals(1000, player.count(objs.blood_rune))
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            player.bankCapacity = 800
            bank[0] = InvObj(objs.blood_rune, 4)
            bank[1] = InvObj(objs.soul_rune, 2)
            player.setVarBit(BankTab.Main.sizeVarBit, 2)
            player.setVarBit(cacheTypes.varbits.getValue(16160), 5)
            fun withdraw() {
                player.withProtectedAccess {
                    check(
                        eventBus.publish(
                            this,
                            BankedObjEvents.Autocharge(0, inv.getValue(0)) { _, _ ->
                                error("Paired charging must be atomic.")
                            },
                        )
                    )
                }
            }
            withdraw()
            assertEquals(2, player.inv[0]?.vars)
            assertEquals(InvObj(objs.blood_rune, 2), bank[0])
            assertNull(bank[1])
            withdraw()
            assertEquals(2, player.inv[0]?.vars)
            assertEquals(InvObj(objs.blood_rune, 2), bank[0])
            bank[1] = InvObj(objs.soul_rune, 6)
            player.setVarBit(BankTab.Main.sizeVarBit, 2)
            player.setVarBit(cacheTypes.varbits.getValue(16160), 1)
            withdraw()
            assertEquals(2, player.inv[0]?.vars)
            assertEquals(InvObj(objs.blood_rune, 2), bank[0])
            assertEquals(InvObj(objs.soul_rune, 6), bank[1])
        }
}
