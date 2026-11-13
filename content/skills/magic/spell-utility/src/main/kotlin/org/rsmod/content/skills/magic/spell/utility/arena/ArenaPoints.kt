package org.rsmod.content.skills.magic.spell.utility.arena

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Singleton
internal class ArenaPoints @Inject constructor(private val clock:MapClock,private val random:GameRandom) {
    private var alchemyEpoch = -1
    private var cupboardShift = 0
    private val prices = mutableListOf(1,5,8,15,30)
    private var freeItem = -1
    private var bonusEpoch = -1
    private var bonusShape = 0

    private fun rotate() {
        if (clock.cycle / 100 != alchemyEpoch) {
            alchemyEpoch = clock.cycle/100
            cupboardShift = (cupboardShift + 1 + random.of(5))%6
            for(i in prices.lastIndex downTo 1){
                val j=random.of(i+1)
                val old=prices[i];prices[i]=prices[j];prices[j]=old
            }
            freeItem = if(random.randomBoolean()) random.of(5) else -1
        }
        if (clock.cycle/40 != bonusEpoch) {
            bonusEpoch=clock.cycle/40
            bonusShape = (bonusShape + 1 + random.of(3))%4
        }
    }

    fun cupboard(index:Int):ObjType? { rotate(); return ArenaItems.alchemy.getOrNull((index+cupboardShift)%6) }
    fun alchemyValue(index:Int):Int { rotate(); return prices[index] }
    fun isFree(index:Int):Boolean { rotate(); return freeItem == index }
    fun balance(player:Player,room:ArenaRoom):Int = player.vars[ArenaVars.points[room.ordinal]]
    fun change(player:Player,room:ArenaRoom,delta:Int) {
        val points = (balance(player,room).toLong()+delta).coerceIn(0L,room.cap.toLong()).toInt()
        VarPlayerIntMapSetter.set(player,ArenaVars.points[room.ordinal],points)
        if(ArenaUi.rooms[room.ordinal] in player.ui) player.ifSetText(ArenaButtons.roomPoints[room.ordinal],"Pizazz Points: $points")
        if(ArenaUi.lobby in player.ui) player.ifSetText(ArenaButtons.totals[room.ordinal],points.toString())
        if(ArenaUi.shop in player.ui) VarPlayerIntMapSetter.set(player,ArenaVars.displays[room.ordinal],points)
    }
    fun show(player:Player,room:ArenaRoom?) {
        if(room==null) {
            for(r in ArenaRoom.entries) player.ifSetText(ArenaButtons.totals[r.ordinal],balance(player,r).toString())
            return
        }
        player.ifSetText(ArenaButtons.roomPoints[room.ordinal],"Pizazz Points: ${balance(player,room)}")
        when(room){
            ArenaRoom.Alchemy -> {
                rotate()
                for(i in prices.indices){
                    player.ifSetText(ArenaButtons.prices[i],prices[i].toString())
                    player.ifSetHide(ArenaButtons.free[i],i!=freeItem)
                }
            }
            ArenaRoom.Enchantment -> {
                rotate()
                for((i,layer) in ArenaButtons.shape.withIndex()) player.ifSetHide(layer,i!=bonusShape)
            }
            ArenaRoom.Theatre -> player.ifSetText(ArenaButtons.mazes,"Mazes solved: ${player.vars[ArenaVars.streak]}")
            else -> Unit
        }
    }
    fun enchanted(player:Player,shape:ObjType,tier:Int) {
        rotate()
        val index = ArenaItems.shapes.indexOfFirst { it.id==shape.id }
        val tenth = (player.vars[ArenaBits.shapes]+1)%10
        VarPlayerIntMapSetter.set(player,ArenaBits.shapes,tenth)
        val doubled = shape.id==ArenaItems.stone.id || tenth==0
        val amount = tier*(if(doubled) 2 else 1) + if(index==bonusShape) 2 else 0
        change(player,ArenaRoom.Enchantment,amount)
    }
    fun refreshHat(player:Player) {
        val min = ArenaRoom.entries.minOf { balance(player,it) }
        val hat = ArenaItems.hats[when { min>=600 -> 2; min>=300 -> 1; else -> 0 }]
        for(bag in listOf(player.inv,player.worn)) for(slot in bag.indices){
            val obj=bag[slot] ?: continue
            if(obj.id!=hat.id && ArenaItems.hats.any {it.id==obj.id}) {
                bag[slot]=InvObj(hat,obj.count,obj.vars)
                if(bag===player.worn)player.rebuildAppearance()
            }
        }
    }
}
