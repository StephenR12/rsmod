package org.rsmod.content.skills.magic.spell.utility.arena

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.*
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.content.skills.magic.spell.utility.standard.UtilitySpellCooldownRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

private data class Maze(val west:Int,val south:Int,val floor:Int,val startX:Int,val startZ:Int,val goalX:Int,val goalZ:Int,val guideX:Int,val guideZ:Int)
private data class TheatreVisit(val maze:Maze,val region:Region,var statue:Npc,val guide:Npc,var solved:Boolean=false) {
    fun tile(x:Int,z:Int):CoordGrid = region.normal[maze.floor,x/64,z/64,x%64,z%64]
    val corner get()=tile(maze.west,maze.south)
    val goal get()=tile(maze.goalX,maze.goalZ)
    val start get()=tile(maze.startX,maze.startZ)
}

@Singleton
internal class TelekineticTheatre @Inject constructor(
    private val points:ArenaPoints,private val npcs:NpcRepository,private val types:NpcTypeList,
    private val regions:RegionRepository,private val collision:CollisionFlagMap, private val random:GameRandom,
    private val cooldowns:UtilitySpellCooldownRepository,private val spells:MagicSpellRegistry,
    private val runes:MagicRuneManager,private val ground:ObjRepository
){
    private val visits=mutableMapOf<Player,TheatreVisit>()
    fun inside(p:Player):Boolean {
        val v=visits[p] ?: return false
        return p.level==v.corner.level && p.x in v.region.southWest.x until v.region.southWest.x+128 && p.z in v.region.southWest.z until v.region.southWest.z+128
    }
    fun enter(a:ProtectedAccess):Boolean=with(a) {
        val maze=mazes[random.of(mazes.size)]
        val region=regions.add(RegionTemplate.create { copy(416,1208,maze.floor) {zoneWidth=8;zoneLength=8;regionZoneX=4;regionZoneZ=4} })
            ?: return false
        leave(player)
        val start=region.normal[maze.floor,52,151,maze.startX%64,maze.startZ%64]
        val statue=Npc(types[ArenaNpcs.statue],start)
        npcs.add(statue,Int.MAX_VALUE)
        val guide=Npc(types[ArenaNpcs.guides[ArenaRoom.Theatre.ordinal]],region.normal[maze.floor,52,151,maze.guideX%64,maze.guideZ%64])
        npcs.add(guide,Int.MAX_VALUE)
        val v=TheatreVisit(maze,region,statue,guide)
        visits[player]=v
        vars[ArenaVars.mazeActive]=1
        vars[ArenaBits.solved]=0
        telejump(v.tile(maze.west-1,maze.south+4))
        true
    }
    fun leave(p:Player){
        visits.remove(p)?.let { npcs.del(it.statue,Int.MAX_VALUE);npcs.del(it.guide,Int.MAX_VALUE) }
        VarPlayerIntMapSetter.set(p,ArenaVars.mazeActive,0)
        VarPlayerIntMapSetter.set(p,ArenaBits.observing,0)
        VarPlayerIntMapSetter.set(p,ArenaBits.solved,0)
    }
    fun recover(p:Player){ leave(p);PathingEntityCommon.telejump(p,collision,CoordGrid(3363,3317)) }

    fun cast(a:ProtectedAccess,npc:Npc):Unit=with(a){
        val v=visits[player] ?: return
        if(npc!==v.statue || v.solved || !inside(player) || !cooldowns.isReady(player,mapClock))return
        val spell=spells.getObjSpell(objs.spell_tele_grab) ?: return
        if(!runes.canCastSpell(player,spell) || runes.attemptCast(player,spell).isFailure())return
        anim(seqs.human_casttelegrab);spotanim(spotanims.telegrab_casting);soundSynth(synths.telegrab_all)
        statAdvance(stats.magic,spell.castXp);cooldowns.markCast(player,mapClock+4)
        val c=v.corner
        val (dx,dz,mask)=when {
            coords.x in c.x..c.x+9 && coords.z>c.z+9 -> Triple(0,1,CollisionFlag.BLOCK_NORTH)
            coords.x in c.x..c.x+9 && coords.z<c.z -> Triple(0,-1,CollisionFlag.BLOCK_SOUTH)
            coords.z in c.z..c.z+9 && coords.x<c.x -> Triple(-1,0,CollisionFlag.BLOCK_WEST)
            coords.z in c.z..c.z+9 && coords.x>c.x+9 -> Triple(1,0,CollisionFlag.BLOCK_EAST)
            else -> return
        }
        var tile=npc.coords
        while(true){
            val next=tile.translate(dx,dz)
            if(next.x !in c.x..c.x+9 || next.z !in c.z..c.z+9) break
            val flags = collision[v.maze.west + next.x - c.x, v.maze.south + next.z - c.z, v.maze.floor]
            if (flags and mask != 0) break
            tile=next
        }
        if(tile==npc.coords)return
        npc.spawnCoords=tile
        npc.telejump(collision,tile)
        if(tile!=v.goal)return
        v.solved=true
        npcs.del(npc,Int.MAX_VALUE)
        v.statue=Npc(types[ArenaNpcs.solved],tile).also { npcs.add(it,Int.MAX_VALUE) }
        vars[ArenaBits.solved]=1
        val streak=(vars[ArenaVars.streak]+1)%5
        vars[ArenaVars.streak]=streak
        val bonus=streak==0
        points.change(player,ArenaRoom.Theatre,if(bonus)10 else 2)
        statAdvance(stats.magic,if(bonus)1250.0 else 250.0)
        if(bonus){player.runEnergy=constants.run_max_energy;invAddOrDrop(ground,objs.law_rune,10)}
        points.show(player,ArenaRoom.Theatre)
        mes("You have solved the maze! Speak to the guardian for a new one.")
    }
    fun observe(a:ProtectedAccess,npc:Npc):Unit=with(a) {
        val v=visits[player] ?: return
        if(npc!==v.statue || v.solved)return
        if(vars[ArenaBits.observing]!=0){camReset();vars[ArenaBits.observing]=0;return}
        camMoveTo(v.corner.translate(4,-4),900,100,100)
        camLookAt(v.corner.translate(4,4),0,100,100)
        vars[ArenaBits.observing]=1
    }
    fun reset(a:ProtectedAccess,npc:Npc):Unit=with(a) {
        val v=visits[player] ?: return
        if(npc===v.statue && !v.solved){npc.spawnCoords=v.start;npc.telejump(collision,v.start)}
    }
    fun next(a:ProtectedAccess,npc:Npc):Unit=with(a){
        val v=visits[player] ?: return
        if(npc===v.statue && v.solved){camReset();enter(a)}
    }
    private companion object {
        // Cache walls and pitfalls identify each layout. Its statue start matches the historical solution map.
        val mazes=listOf(
Maze(3339,9674,2,3348,9674,3339,9683,3358,9679), // 9 casts
Maze(3341,9708,1,3350,9717,3341,9708,3357,9713), // 8 casts
Maze(3338,9675,0,3343,9680,3342,9684,3349,9687), // 7 casts
Maze(3343,9709,2,3346,9718,3345,9718,3355,9701), // 9 casts
Maze(3338,9705,0,3343,9705,3347,9714,3335,9703), // 10 casts
Maze(3346,9680,1,3351,9684,3353,9680,3358,9691), // 8 casts
Maze(3366,9711,0,3368,9712,3367,9720,3380,9713), // 8 casts
Maze(3369,9673,0,3373,9678,3375,9682,3373,9690), // 10 casts
Maze(3374,9713,1,3374,9713,3383,9713,3379,9707), // 7 casts
Maze(3376,9677,1,3376,9686,3385,9686,3373,9679), // 7 casts
        )
    }
}
public class TelekineticTheatreScript @Inject internal constructor(private val theatre:TelekineticTheatre):PluginScript(){
    override fun ScriptContext.startup(){
        onApNpcT(ArenaNpcs.statue,components.magic_spellbook_telegrab) { theatre.cast(this,it.npc) }
        onApNpc1(ArenaNpcs.statue) { theatre.observe(this,it.npc) }
        onApNpc3(ArenaNpcs.statue) { theatre.reset(this,it.npc) }
        onApNpc1(ArenaNpcs.solved) { theatre.next(this,it.npc) }
    }
}
