package org.rsmod.content.skills.fishing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp

@Execution(ExecutionMode.SAME_THREAD)
class TackleBoxTest {
    @Test fun GameTestState.storageUsesNativeCataloguePreservesToolChargesAndRejectsNotes()=runGameTest(TackleBox::class){
        player.members=true
        val box=objTypes.getValue(25580)
        val flakes=objTypes.getValue(25588)
        player.inv[0]=InvObj(box)
        player.inv[1]=InvObj(objs.crystal_harpoon,vars=273)
        player.inv[2]=InvObj(objs.crystal_harpoon,vars=197)
        player.inv[3]=InvObj(flakes,1300)
        player.inv[4]=InvObj(objTypes.cert(objTypes[objs.small_fishing_net]),3)
        player.withProtectedAccess {opHeld2(0)}
        val stored=player.invMap.getValue(cacheTypes.invs.getValue(651))
        assertEquals(1300,stored.count(flakes))
        assertEquals(273,stored[0]?.vars)
        assertEquals(197,stored[1]?.vars)
        assertEquals(3,player.inv[4]?.count)
        player.withProtectedAccess {opHeld1(0)}
        val items=cacheTypes.components.getValue((683 shl 16)+11)
        player.ifButton(items,comsub=1,obj=objs.crystal_harpoon.id);advance()
        assertEquals(197,player.inv[1]?.vars)
        player.ifButton(items,comsub=2,obj=flakes.id,op=IfButtonOp.Op3);advance()
        assertEquals(5,player.count(flakes))
        assertEquals(1295,stored.count(flakes))
        player.ifClose();advance()
        player.withProtectedAccess {opHeld3(0)}
        assertTrue(stored.isEmpty())
        assertEquals(1300,player.count(flakes))
        assertEquals(273,player.inv.filterNotNull().single { it.id==objs.crystal_harpoon.id && it.vars==273 }.vars)
    }
}
