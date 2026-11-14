package org.rsmod.content.skills.runecraft

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.content.interfaces.bank.openBank
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.skills.mining.CelestialMiningRing
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp

@Execution(ExecutionMode.SAME_THREAD)
class CelestialRingChargesTest {
    @Test
    fun GameTestState.chargeCheckAndRemovalPreserveDustAndRespectFreeSpace() =
        runGameTest(CelestialMiningRing::class) {
            player.members = true
            val dust = objTypes.getValue(25527)
            player.inv[0] = InvObj(objTypes.getValue(25539))
            player.inv[1] = InvObj(dust, 30)
            player.withProtectedAccess { opHeld3(0) }
            player.resumeCountDialog(12)
            advance()
            assertEquals(25541, player.inv[0]?.id)
            assertEquals(12, player.inv[0]?.vars)
            assertEquals(18, player.count(dust))
            player.withProtectedAccess { opHeld2(0) }
            assertMessageSent("Your ring has 12 charges remaining.")
            player.inv[1] = null
            for (slot in 1 until player.inv.size) player.inv[slot] = InvObj(objs.beer)
            fun uncharge() {
                player.withProtectedAccess { opHeld5(0) }
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
                advance()
                player.ifClose()
                advance()
            }
            uncharge()
            assertEquals(12, player.inv[0]?.vars)
            player.inv[1] = null
            uncharge()
            assertEquals(25539, player.inv[0]?.id)
            assertEquals(12, player.count(dust))
        }

    @Test
    fun GameTestState.bankAutochargeUsesAvailableStardustOnlyForTheWithdrawnSignet() =
        runGameTest(CelestialMiningRing::class, BankInvScript::class) {
            player.members = true
            val charged = objTypes.getValue(25545)
            val dust = objTypes.getValue(25527)
            val buttons = cacheTypes.components.getValue((12 shl 16) + 12)
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            bank[0] = InvObj(charged, vars = 3)
            bank[1] = InvObj(dust, 14)
            player.inv[0] = InvObj(charged, vars = 9)
            player.setVarBit(BankTab.Main.sizeVarBit, 2)
            player.setVarBit(cacheTypes.varbits.getValue(16125), 1)
            player.openBank(
                this@bankAutochargeUsesAvailableStardustOnlyForTheWithdrawnSignet.eventBus
            )
            player.ifButton(buttons, comsub = 0, op = IfButtonOp.Op9)
            advance()
            player.resumeCountDialog(20)
            advance()
            assertEquals(20, player.vars[cacheTypes.varbits.getValue(16163)])
            player.ifButton(buttons, comsub = 0, op = IfButtonOp.Op2)
            advance()
            assertEquals(9, player.inv[0]?.vars)
            assertEquals(17, player.inv[1]?.vars)
            assertTrue(bank.isEmpty())
            bank[0] = InvObj(charged, vars = 25)
            bank[1] = InvObj(dust, 10)
            player.setVarBit(BankTab.Main.sizeVarBit, 2)
            player.ifButton(buttons, comsub = 0, op = IfButtonOp.Op2)
            advance()
            assertEquals(25, player.inv[2]?.vars)
            assertEquals(10, bank.count(dust))
        }
}
