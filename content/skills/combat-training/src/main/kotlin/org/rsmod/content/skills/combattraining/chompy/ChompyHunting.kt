package org.rsmod.content.skills.combattraining.chompy

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.ranged.OgreBows
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.script.*
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

private data class Bait(val owner: PlayerUid,val kind: Int,var rolls: Int=0,var attracted: Boolean=false)
private data class Bird(val bait: Npc,val kind: Int,var ticks: Int=0)
private data class Carcass(val owner: Long,val kind: Int)

/** Each bait has its own four independent 25-tick rolls and can lure only one flock. */
@Singleton
public class ChompyHunting @Inject constructor(
    private val pets: org.rsmod.content.skills.pets.SkillPets,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val objects: org.rsmod.game.type.obj.ObjTypeList,
    private val players: PlayerList,
    private val ground: ObjRepository,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
    private val rays: RayCastValidator,
    private val events: EventBus,
) : PluginScript() {
    private val baits = mutableMapOf<Npc,Bait>()
    private val birds = mutableMapOf<Npc,Bird>()
    private val carcasses = mutableMapOf<Npc,Carcass>()

    override fun ScriptContext.startup() {
        for (type in HuntingNpcs.baits) onAiTimer(type) { baitTick(npc) }
        for (type in HuntingNpcs.live) {
            onAiTimer(type) { birdTick(npc) }
            onNpcQueue(type,queues.com_retaliate_player) { noneMode() }
            onModifyNpcHit(type) {
                val source = hit.sourceUid?.let { PlayerUid(it).resolve(players) }
                val valid = source?.canHuntChompies() == true && hit.isFromPlayer &&
                    hit.type == HitType.Ranged && OgreBows.canFire(hit.righthandType, hit.secondaryType)
                if (!valid) hit.damage=0
            }
        }
        onEvent<NpcKillEvents.Credited> {
            val bird = birds.remove(npc) ?: return@onEvent
            if (!player.canHuntChompies() || hit?.type != HitType.Ranged ||
                !OgreBows.canFire(hit?.righthandType(objects)?.id,hit?.secondaryObj)) return@onEvent
            val id=player.uuid ?: return@onEvent
            val carcass=Npc(types[HuntingNpcs.dead[bird.kind]],npc.coords)
            npcs.add(carcass,200)
            carcasses[carcass]=Carcass(id,bird.kind)
            val previous=player.vars[HuntingVars.kills]
            VarPlayerIntMapSetter.set(player,HuntingVars.kills,(previous+1).coerceAtMost(100000))
            player.mes("You scratch a notch on your bow for the chompy bird kill.")
            if (previous==3999) {
                player.statAdvance(stats.ranged,30000.0)
                player.mes("You've been awarded ranged experience for your relentless pursuit of chompies!")
            }
            if (bird.kind==0) rollChick(player)
        }
        for (type in HuntingNpcs.dead) onOpNpc4(type) { pluck(player,it.npc) }
        onEvent<NpcStateEvents.Delete> { baits.remove(npc);birds.remove(npc);carcasses.remove(npc) }
    }

    internal fun lay(access: ProtectedAccess,index: Int,slot: Int) = with(access) {
        if (!player.canHuntChompies() || index>0 && vars[HuntingBits.skrach]<170) return
        val c=coords
        val castle = c.x in 2322..2434 && c.z in 3029..3067 || c.x in 2322..2372 && c.z in 3068..3077
        val feldip = c.x in 2496..2624 && c.z in 2944..3015
        if (c.level!=0 || !castle && !feldip || index>0 && !feldip) {
            mes("You won't attract a bird this far away from the ogre area.")
            return
        }
        if (baits.keys.any { it.coords==c }) { mes("There is a bloated toad already placed at this location."); return }
        if (!invDel(inv,HuntingObjects.bait[index],slot=slot).success) return
        val npc=Npc(types[HuntingNpcs.baits[index]],c)
        npcs.add(npc,102)
        baits[npc]=Bait(player.uid,index)
        npc.aiTimer(25)
    }

    private fun baitTick(npc: Npc) {
        val bait=baits[npc] ?: return
        val owner=bait.owner.resolve(players)
        bait.rolls++
        if (!bait.attracted && random.of(5)==0) {
            val kind=if (bait.kind==0) 0 else 1
            val count=if (kind==0 && extraBird(owner)) 2 else 1
            val legal = landingTiles(npc.coords,types[HuntingNpcs.live[kind]].size).toMutableList()
            repeat(minOf(count,legal.size)) {
                val square=legal.removeAt(random.of(legal.size))
                val bird=Npc(types[HuntingNpcs.live[kind]],square)
                npcs.add(bird,103)
                bird.anim(HuntingAnims.land[kind])
                bird.aiTimer(5)
                birds[bird]=Bird(npc,kind)
                bait.attracted=true
            }
        }
        if (bait.rolls==4) {
            if(bait.kind==0) {
                npc.anim(HuntingAnims.burst)
                if (owner!=null && owner.coords.level==npc.coords.level && owner.coords.chebyshevDistance(npc.coords)<=1 && owner.hitpoints>1)
                    owner.queueHit(0,HitType.Typeless,minOf(owner.hitpoints-1,random.of(1..2)))
            }
            baits.remove(npc)
        } else npc.aiTimer(25)
    }
    private fun extraBird(player: Player?): Boolean = when(player?.westernTier()) {
        DiaryDifficulty.Easy -> random.of(4)==0
        DiaryDifficulty.Medium,DiaryDifficulty.Hard -> random.of(2)==0
        DiaryDifficulty.Elite -> true
        null -> false
    }
    private fun landingTiles(centre: CoordGrid,size: Int): List<CoordGrid> = buildList {
        for(dx in -10..10) for(dz in -10..10) {
            if(maxOf(kotlin.math.abs(dx),kotlin.math.abs(dz))<3) continue
            val c=centre.translate(dx,dz)
            val clear=(0 until size).all { x -> (0 until size).none { z -> collision.isWalkBlocked(c.translate(x,z)) } }
            if (clear && rays.hasLineOfSight(centre,c)) add(c)
        }
    }
    private fun birdTick(npc: Npc) {
        val bird=birds[npc] ?: return
        if (npc.hitpoints<=0) return
        bird.ticks+=5
        if (bird.ticks>=100) { npc.anim(HuntingAnims.fly[bird.kind]);return }
        if (bird.bait in baits) {
            if (npc.coords.chebyshevDistance(bird.bait.coords)<=npc.size) {
                baits.remove(bird.bait)
                npcs.del(bird.bait,Int.MAX_VALUE)
            } else npc.walk(bird.bait.coords)
        }
        npc.aiTimer(5)
    }
    private fun pluck(player: Player,npc: Npc) {
        val carcass=carcasses[npc] ?: return
        if (carcass.owner!=player.uuid || !player.canHuntChompies()) return
        carcasses.remove(npc)
        val jubbly=carcass.kind!=0
        val remains=RemainsDrop(player,InvObj(if(jubbly) objs.big_bones else objs.bones))
        events.publish(remains)
        val delivered = remains.remaining == 0 || ground.add(
            org.rsmod.game.obj.Obj.fromOwner(player, npc.coords, remains.obj.copy(count = remains.remaining)),
            constants.lootdrop_duration,
        )
        if (delivered) remains.commit()
        ground.add(if(jubbly) HuntingObjects.jubbly else objs.raw_chompy,npc.coords,constants.lootdrop_duration,player)
        player.invAddOrDrop(ground,objs.feather,random.of(if(jubbly) 15..45 else 10..30),coords=npc.coords)
        npcs.del(npc,Int.MAX_VALUE)
        if(!jubbly) rollChick(player)
    }
    private fun rollChick(player: Player) {
        if(player.westernTier()!=DiaryDifficulty.Elite || random.of(500)!=0) return
        pets.award(player,HuntingObjects.chick.id)
    }
}
