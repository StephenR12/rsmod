package org.rsmod.content.other.special.weapons.magic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.content.other.special.weapons.scripts.charge.TumekensShadowCharging
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class TumekensShadowChargingTest {
    @Test
    fun GameTestState.`charge option accepts either zero charge shadow variant`() =
        runGameTest(TumekensShadowCharging::class) {
            for (type in listOf(objs.tumekens_shadow_uncharged, objs.tumekens_shadow)) {
                val otherVars = 1 shl 23
                player.inv[0] = InvObj(type, vars = otherVars)
                player.inv[1] = InvObj(objs.chaos_rune, 24)
                player.inv[2] = InvObj(objs.soul_rune, 12)
                player.withProtectedAccess {
                    if (type == objs.tumekens_shadow) opHeld4(0) else opHeld3(0)
                }
                player.resumeCountDialog(Int.MAX_VALUE)
                advance()
                assertEquals(InvObj(objs.tumekens_shadow, vars = otherVars or 4), player.inv[0])
                assertEquals(4, player.inv[1]?.count)
                assertEquals(4, player.inv[2]?.count)
                player.ifClose()
                advance()
            }
        }

    @Test
    fun GameTestState.`changed target or missing runes after the quantity prompt costs nothing`() =
        runGameTest(TumekensShadowCharging::class) {
            val staff = InvObj(objs.tumekens_shadow)
            player.inv[0] = staff
            player.inv[1] = InvObj(objs.chaos_rune, 10)
            player.inv[2] = InvObj(objs.soul_rune, 4)
            player.withProtectedAccess { opHeld4(0) }
            player.inv[0] = staff.copy(vars = 100)
            player.resumeCountDialog(2)
            advance()
            assertEquals(staff.copy(vars = 100), player.inv[0])
            assertEquals(10, player.inv[1]?.count)
            assertEquals(4, player.inv[2]?.count)
            player.withProtectedAccess { opHeld4(0) }
            player.inv[2] = null
            player.resumeCountDialog(2)
            advance()
            assertEquals(staff.copy(vars = 100), player.inv[0])
            assertEquals(10, player.inv[1]?.count)
        }

    @Test
    fun GameTestState.`bank autocharge pays for missing charges on a zero charge active shadow once`() =
        runGameTest(PoweredStaffCharging::class) {
            player.withProtectedAccess {
                player.setVarBit(cacheTypes.varbits.getValue(16156), 2)
                BankTab.Main.increaseSize(this, 2)
                bank[0] = InvObj(objs.chaos_rune, 15)
                bank[1] = InvObj(objs.soul_rune, 6)
                inv[0] = InvObj(objs.tumekens_shadow)
                repeat(2) {
                    publish(BankedObjEvents.Autocharge(0, inv.getValue(0)) { _, _ -> error("Unexpected single material withdrawal") })
                }
                assertEquals(InvObj(objs.tumekens_shadow, vars = 2), inv[0])
                assertEquals(5, bank[0]?.count)
                assertEquals(2, bank[1]?.count)
            }
        }

    @Test
    fun GameTestState.`manager caps large additions and never charges for an invalid target`() =
        runInjectedGameTest(ObjChargeManager::class) { manager ->
            player.inv[0] = InvObj(objs.tumekens_shadow, vars = 19999)
            fun add() =
                manager.addCharges(player.inv, 0, Int.MAX_VALUE, varobjs.tumeken_charges, 20000) {
                    assertEquals(1, it)
                    it
                }
            assertEquals(ObjChargeManager.Charge.Success.AddSameObj(1, 20000), add())
            assertEquals(ObjChargeManager.Charge.Failure.AlreadyFullCharges, add())
            player.inv[0] = InvObj(objs.coins, 500)
            val result = manager.addCharges(player.inv, 0, 1, varobjs.tumeken_charges, 20000) {
                error("Unsupported items cannot consume materials")
            }
            assertEquals(ObjChargeManager.Charge.Failure.UnsupportedObj, result)
            assertEquals(InvObj(objs.coins, 500), player.inv[0])
        }
}
