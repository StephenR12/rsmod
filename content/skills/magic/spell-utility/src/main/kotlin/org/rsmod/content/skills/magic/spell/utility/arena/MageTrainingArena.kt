package org.rsmod.content.skills.magic.spell.utility.arena

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.*
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.events.EventBus

public class MageTrainingArena @Inject internal constructor(
    private val points:ArenaPoints, private val theatre:TelekineticTheatre,private val random:GameRandom,
    private val types:ObjTypeList,private val ground:ObjRepository,private val world:WorldRepository,
    private val launch:ProtectedAccessLauncher,private val events:EventBus,private val clock:MapClock
) : PluginScript() {
    private val active = mutableMapOf<Player,ArenaRoom>()
    private val falling = mutableMapOf<Player,CoordGrid>()
    private val lobby = CoordGrid(3363,3317)
    override fun ScriptContext.startup(){
        onOpLoc1(ArenaLocs.entrance) {
            if (player.members) telejump(CoordGrid(3363,if(coords.z<3299) 3300 else 3298))
        }
        for(stair in ArenaLocs.stairsUp) onOpLoc1(stair) { telejump(it.loc.coords.translate(1,1).copy(level=1)) }
        for(stair in ArenaLocs.stairsDown) onOpLoc1(stair) { telejump(it.loc.coords.translate(if(it.loc.x<3363) 3 else -1,0).copy(level=0)) }
        onOpLoc1(ArenaLocs.bank) { ifOpenMainSidePair(interfaces.bank_main,interfaces.bank_side) }
        onOpLoc3(ArenaLocs.bank) { ifOpenMainModal(interfaces.ge_collection_box) }
        onArea(ArenaAreas.lobby) { if(player.members && player !in active) { ifOpenOverlay(ArenaUi.lobby);points.show(player,null) } }
        onAreaExit(ArenaAreas.lobby) { player.ifCloseOverlay(ArenaUi.lobby,events) }
        for(room in ArenaRoom.entries) {
            onOpLoc1(ArenaLocs.doors[room.ordinal]) { enter(room) }
            onOpNpc1(ArenaNpcs.guides[room.ordinal]) { mes(instructions[room.ordinal]) }
        }
        onOpLoc1(ArenaLocs.exit) {
            val room=active[player] ?: ArenaRoom.entries.firstOrNull { it.contains(coords) } ?: return@onOpLoc1
            if(room==ArenaRoom.Alchemy && !depositCoins()) return@onOpLoc1
            finish(room)
            telejump(lobby)
            payRunes()
        }
        for((index,pair) in ArenaLocs.cupboards.withIndex()) for(cupboard in pair){
            onOpLoc1(cupboard) { takeItems(index,1) }
            onOpLoc2(cupboard) { takeItems(index,5) }
        }
        onOpLoc1(ArenaLocs.coinCollector) { if(ArenaRoom.Alchemy.contains(coords))depositCoins() }
        for((index,pile) in ArenaLocs.shapes.withIndex()) onOpLoc1(pile){
            if(ArenaRoom.Enchantment.contains(coords)) {
                val shape = if(random.of(32)==0) ArenaItems.stone else ArenaItems.shapes[index]
                invAdd(inv,shape)
            }
        }
        onOpLoc1(ArenaLocs.hole) {
            val count=invTotal(inv,ArenaItems.orb)
            if(count>0 && ArenaRoom.Enchantment.contains(coords) && invDel(inv,ArenaItems.orb,count).success){
                val total=count+vars[ArenaBits.orbs]
                vars[ArenaBits.orbs]=total%20
                vars[ArenaVars.enchantRunes]+=total/20
                statAdvance(stats.magic,count*20.0)
                mes("You deposit $count orbs in the hole.")
            }
        }
        for((index,pile) in ArenaLocs.bones.withIndex()) onOpLoc1(pile) {
            if(!ArenaRoom.Graveyard.contains(coords)) return@onOpLoc1
            val cycle = vars[ArenaVars.boneCycle]
            if(invAdd(inv,ArenaItems.bones[(cycle/4+index)%4]).success)vars[ArenaVars.boneCycle]=(cycle+1)%16
        }
        onOpLoc1(ArenaLocs.chute) { depositFruit() }
        onPlayerTimer(ArenaTimer.tick) {
            val room=active[player] ?: return@onPlayerTimer clearTimer(ArenaTimer.tick)
            if(room!=ArenaRoom.Theatre && !room.contains(coords) || room==ArenaRoom.Theatre && !theatre.inside(player)){
                finish(room);payRunes();return@onPlayerTimer
            }
            if(room==ArenaRoom.Graveyard) {
                if(falling.remove(player)==coords) queueHit(0,HitType.Typeless,2)
                if(clock.cycle%14==0) {
                    falling[player]=coords
                    spotanimMap(world,ArenaSpots.bones[random.of(4)],coords,height=90)
                }
            }
            if(room==ArenaRoom.Alchemy && clock.cycle%100==0 || room==ArenaRoom.Enchantment && clock.cycle%40==0)points.show(player,room)
        }
        onPlayerLogin {
            val room=ArenaRoom.entries.firstOrNull { it.contains(player.coords) }
            if(player.vars[ArenaVars.mazeActive]!=0) theatre.recover(player)
            if(room!=null) launch.launch(player) { start(room) }
        }
        onPlayerLogout {
            player.activityBlocksTeleport=false
            val room=active.remove(player)
            falling.remove(player)
            if(room==ArenaRoom.Theatre) theatre.recover(player)
            player.clearTimer(ArenaTimer.tick)
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            val room=active.remove(player) ?: return@onEvent
            player.activityBlocksTeleport=false
            if(room==ArenaRoom.Graveyard)points.change(player,room,-10)
            falling.remove(player)
            player.clearTimer(ArenaTimer.tick)
            clearGoods(player,room)
            player.ifCloseOverlay(ArenaUi.rooms[room.ordinal],events)
            if(room==ArenaRoom.Theatre)theatre.recover(player)
            safeDestination=lobby
        }
    }

    private suspend fun ProtectedAccess.enter(room:ArenaRoom){
        if(!player.members || player in active) return
        if(stat(stats.magic)<room.requiredLevel){mes("You need level ${room.requiredLevel} Magic for this room.");return}
        if(inv.filterNotNull().any {
            val type=types[it]
            val name=type.name.lowercase()
            name.startsWith("clue scroll") || name.startsWith("reward casket") || name.startsWith("clue nest") ||
            room==ArenaRoom.Alchemy && (it.id==objs.coins.id || name=="coin pouch") ||
            room==ArenaRoom.Graveyard && (it.id in forbiddenBones || it.id==objs.banana.id || it.id==objs.peach.id)
        }){mes("You cannot take those items into the training room.");return}
        if(points.balance(player,room)>=room.cap && !choice2("Enter with the maximum number of points.",true,"Stay here.",false))return
        if(room==ArenaRoom.Theatre){if(!theatre.enter(this))return} else telejump(room.landing)
        start(room)
    }
    private fun ProtectedAccess.start(room:ArenaRoom){
        active[player]=room
        player.activityBlocksTeleport=true
        ifOpenOverlay(ArenaUi.rooms[room.ordinal])
        points.show(player,room)
        timer(ArenaTimer.tick,1)
    }
    private fun ProtectedAccess.finish(room:ArenaRoom){
        player.activityBlocksTeleport=false
        if(room==ArenaRoom.Theatre)camReset()
        active.remove(player)
        falling.remove(player)
        clearTimer(ArenaTimer.tick)
        if(room==ArenaRoom.Theatre)theatre.leave(player)
        clearGoods(player,room)
        player.ifCloseOverlay(ArenaUi.rooms[room.ordinal],events)
        points.refreshHat(player)
    }
    private fun clearGoods(player:Player,room:ArenaRoom){
        val ids = when(room){
            ArenaRoom.Theatre -> emptyList()
            ArenaRoom.Alchemy -> ArenaItems.alchemy+ArenaItems.coins
            ArenaRoom.Enchantment -> ArenaItems.shapes+listOf(ArenaItems.orb,ArenaItems.stone)
            ArenaRoom.Graveyard -> ArenaItems.bones+listOf(objs.peach,objs.banana)
        }.map {it.id}.toSet()
        for(slot in player.inv.indices) if(player.inv[slot]?.id in ids)player.inv[slot]=null
    }
    private fun ProtectedAccess.takeItems(index:Int,count:Int){
        if(!ArenaRoom.Alchemy.contains(coords))return
        val item=points.cupboard(index)
        if(item==null)mes("The cupboard is empty.") else invAdd(inv,item,count,strict=false)
    }
    private fun ProtectedAccess.depositCoins():Boolean {
        val coins=invTotal(inv,ArenaItems.coins)
        val outside=invTotal(inv,objs.coins)
        if(outside>0){invDel(inv,objs.coins,outside);mes("You didn't create those coins from the items in this room!")}
        if(coins==0)return true
        val gp=coins/10
        val result=player.invTransaction(inv,bank){
            delete(select(inv),ArenaItems.coins.id,coins)
            if(gp>0)add(select(bank),objs.coins.id,gp)
        }
        if(!result.success){mes("You need room in your bank for the coin reward.");return false}
        val score=coins/100 + if(random.of(100)<coins%100)1 else 0
        points.change(player,ArenaRoom.Alchemy,score)
        statAdvance(stats.magic,coins*2.0)
        mes("You earn $score Alchemist pizazz points and $gp coins.")
        return true
    }
    private fun ProtectedAccess.depositFruit(){
        if(!ArenaRoom.Graveyard.contains(coords))return
        val bananas=invTotal(inv,objs.banana)
        val peaches=invTotal(inv,objs.peach)
        val portions=bananas+2*peaches
        if(portions==0){mes("You have no fruit to deposit.");return}
        val result=player.invTransaction(inv){
            val bag=select(inv)
            if(bananas>0)delete(bag,objs.banana.id,bananas)
            if(peaches>0)delete(bag,objs.peach.id,peaches)
        }
        if(!result.success)return
        val earned=(portions/16 + if(random.of(16)<portions%16) 1 else 0).coerceAtMost(3)
        points.change(player,ArenaRoom.Graveyard,earned)
        vars[ArenaVars.graveRunes]+=earned
        statAdvance(stats.magic,portions*5.0)
        mes("You earn $earned Graveyard pizazz points.")
    }
    private fun ProtectedAccess.payRunes(){
        for((key,runes,size) in listOf(Triple(ArenaVars.graveRunes,listOf(objs.water_rune,objs.earth_rune,objs.nature_rune,objs.death_rune,objs.blood_rune),1),
             Triple(ArenaVars.enchantRunes,listOf(objs.blood_rune,objs.death_rune,objs.cosmic_rune),3))){
            val count=vars[key]
            if(count<=0)continue
            val awards=IntArray(runes.size)
            repeat(count){ awards[random.of(runes.size)]+=size }
            for((i,qty) in awards.withIndex())if(qty>0)invAddOrDrop(ground,runes[i],qty)
            vars[key]=0
        }
    }
    private companion object {
        val forbiddenBones = setOf(526,528,530,3125,532,2859,3126,3183,3185,3179,3181,3186,3180,3182,3177)
        val instructions = listOf("Cast Telekinetic Grab on the statue from the side you want it to move towards.",
            "Search the cupboards, cast Alchemy on their contents and deposit the coins in the collector.",
            "Enchant shapes from the piles. Deposit your orbs in the hole for extra experience and runes.",
            "Collect bones, turn them into fruit and put the fruit into a food chute. Watch for falling bones.")
    }
}
