package org.rsmod.content.skills.cooking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class OrdinaryFoodEatingTest {
    @Test fun GameTestState.`ordinary foods expose native Eat and consume only selected slot with capped healing`() = runFoodTest { d ->
        player.members=true;player.setBaseLevel(stats.hitpoints,50);player.setCurrentLevel(stats.hitpoints,49)
        for ((obj, heal) in OrdinaryFoods.all) {
            assertEquals("Eat",d.types[obj].iop[0]);assertTrue(heal>0)
        }
        player.inv[3]=InvObj(objs.shark);player.inv[4]=InvObj(objs.shark)
        player.withProtectedAccess { d.held.interact(this,player.inv,3,HeldOp.Op1) }
        assertNull(player.inv[3]);assertNotNull(player.inv[4]);assertEquals(50,player.stat(stats.hitpoints))
        assertEquals(3,d.food.remainingCycles(player))
        player.withProtectedAccess { d.held.interact(this,player.inv,4,HeldOp.Op1) }
        assertNotNull(player.inv[4]);advance(3)
        player.withProtectedAccess { d.held.interact(this,player.inv,4,HeldOp.Op1) }
        assertNull(player.inv[4])
    }
    @Test fun GameTestState.`eating adds attack delay only to an already pending attack`() = runFoodTest { d ->
        player.members=true;player.setBaseLevel(stats.hitpoints,50);player.setCurrentLevel(stats.hitpoints,10)
        player.inv[0]=InvObj(objs.lobster);player.actionDelay=player.currentMapClock+2
        player.withProtectedAccess { d.eating.eat(this,0,objs.lobster,12) }
        assertEquals(22,player.stat(stats.hitpoints));assertEquals(player.currentMapClock+5,player.actionDelay)
    }
    @Test fun GameTestState.`oomlie credits diary only after actual member consumption`() = runFoodTest { d ->
        player.members=false;player.inv[0]=InvObj(objs.cooked_oomlie)
        player.withProtectedAccess { d.eating.eat(this,0,objs.cooked_oomlie,14) }
        assertNotNull(player.inv[0]);assertFalse(player.hasCompletedDiaryTask(DiaryTask.EatOomlieWrap))
        player.members=true
        player.withProtectedAccess { d.eating.eat(this,0,objs.cooked_oomlie,14) }
        assertNull(player.inv[0]);assertTrue(player.hasCompletedDiaryTask(DiaryTask.EatOomlieWrap))
    }
    private fun GameTestState.runFoodTest(body: GameTestScope.(Deps)->Unit)=runInjectedGameTest(Deps::class,Module,OrdinaryFoodScript::class,testBody=body)
    private object Module: AbstractModule(){override fun configure(){ install(CookingModule()) }}
    data class Deps @Inject constructor(val eating:OrdinaryFoodEating,val food:FoodConsumptionRepository,val held:HeldInteractions,val types:ObjTypeList)
}
