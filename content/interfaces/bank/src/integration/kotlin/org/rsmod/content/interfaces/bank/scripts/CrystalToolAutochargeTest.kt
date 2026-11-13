package org.rsmod.content.interfaces.bank.scripts

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class CrystalToolAutochargeTest {
    @Test fun GameTestState.withdrawalChargesOnlySelectedInactiveToolWithWholeAvailableShards()=runGameTest(BankInvScript::class,CrystalToolAutocharge::class){
        player.members=true
        val bank=openBank(player)
        val other=InvObj(objs.crystal_harpoon,vars=160)
        player.inv[0]=other
        bank[0]=InvObj(objs.crystal_harpoon_inactive)
        bank[1]=InvObj(objs.crystal_shard,3)
        player.setVarBit(BankTab.Main.sizeVarBit,2)
        player.setVarBit(cacheTypes.varbits.getValue(16125),1)
        player.setVarBit(varbits.charges_crystal_tools_quantity,125)
        player.ifButton(bank_components.main_inventory,comsub=0,op=MAIN_INV_WITHDRAW_1)
        advance()
        assertEquals(other,player.inv[0])
        assertEquals(objs.crystal_harpoon.id,player.inv[1]?.id)
        assertEquals(200,player.inv[1]?.vars)
        assertEquals(1,bank.count(objTypes[objs.crystal_shard]))
    }
}
