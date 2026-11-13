package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isInCombat
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GangController:ControllerBuilder() { val meetings = build("shayzien_gang_meetings") }
internal object GangBoard:LocReferences() { val board = find("shayzien_crimefighting_noticeboard") }
internal object GangGoods:ObjReferences() {
    val intel = find("shayzien_gang_intelligence")
    val reward = find("shayzien_crime_reward")
    val note = find("shayzien_meeting_note")
}
internal object GangNpcs:NpcReferences() {
    val captain = find("shayzien_crime_controller")
    val gangsters = (1..4).map { find("shayzien_criminal_$it") }
    val bosses = listOf(2,1).map { find("shayzien_criminal_boss_${it}_fighting") }
}
internal object GangStats:NpcEditor() {
    init {
        val health=listOf(40,40,45,30,80,80)
        val attackLevels=listOf(40,40,50,40,1,70)
        val strengths=listOf(30,30,40,70,1,70)
        val defences=listOf(50,50,40,30,70,70)
        val speeds=listOf(4,4,7,4,3,3)
        for ((index,type) in (GangNpcs.gangsters + GangNpcs.bosses).withIndex()) edit(type) {
            hitpoints=health[index]
            attack=attackLevels[index]
            strength=strengths[index]
            defence=defences[index]
            ranged=if(index==4)80 else 1
            param[params.attackrate]=speeds[index]
        }
    }
}

public class GangMeetings @Inject constructor(
    private val random:GameRandom,
    private val clock:MapClock,
    private val controllers:ControllerRepository,
    private val npcs:NpcRepository,
    private val types:NpcTypeList,
    private val objects:ObjRepository,
    private val players:PlayerList,
    private val ai:AiPlayerInteractions,
    private val collision:CollisionFlagMap
):PluginScript() {
    private data class District(val coords:CoordGrid,val hint:String)
    private val districts=requireNotNull(javaClass.getResourceAsStream("gang-meetings.tsv")).bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() && !it.startsWith("#") }.map { row ->
            val (x,z,h,hint) = row.split('\t')
            District(CoordGrid(x.toInt(),z.toInt(),h.toInt()),hint)
        }.toList()
    }
    private val members=mutableSetOf<Npc>()
    private var current=0
    private var gathering=false
    private var nextStart=0

    override fun ScriptContext.startup() {
        onGameStartup {
            current=random.of(districts.size)
            nextStart=clock.cycle+PERIOD
            val scheduler=Controller(GangController.meetings,CoordGrid(1504,3632))
            scheduler.aiTimer(PERIOD)
            controllers.add(scheduler,Int.MAX_VALUE)
        }
        onAiConTimer(GangController.meetings) {
            if(!gathering) {
                startMeeting()
                controller.aiTimer(500)
            } else {
                for (npc in members.toList()) {
                    if (!npc.isSlotAssigned || npc.hitpoints <= 0) continue
                    val hero = npc.findHero(players)
                    if (hero != null && npc.isInCombat()) {
                        npc.hitpoints = 0
                        dropIntel(npc,hero)
                        npc.queueDeath()
                    } else npcs.del(npc,Int.MAX_VALUE)
                }
                gathering = false
                current = (current+1+random.of(districts.size-1)) % districts.size
                controller.aiTimer((nextStart-clock.cycle).coerceAtLeast(1))
            }
        }
        onEvent<NpcStateEvents.Delete> { members.remove(npc) }
        for(type in GangNpcs.gangsters+GangNpcs.bosses) onEvent<NpcHitEvents.Impact>(type.id) {
            if(npc !in members || !hit.isFromPlayer) return@onEvent
            val attacker=hit.resolvePlayerSource(players) ?: return@onEvent
            for (ally in members) if(ally !== npc && ally.isVisible && ally.hitpoints > 0 && !ally.isInCombat()) ally.opPlayer2(attacker,ai)
        }
        onEvent<NpcKillEvents.Credited> {
            if(npc !in members) return@onEvent
            dropIntel(npc,npc.findHero(players) ?: player)
        }
        onOpNpc1(GangNpcs.captain) {
            if (!player.members) return@onOpNpc1
            if (GangGoods.intel in inv) deliver() else report()
        }
        onOpNpcU(GangNpcs.captain,GangGoods.intel) { if(player.members) deliver() }
        onOpLoc1(GangBoard.board) {
            if(!player.members) return@onOpLoc1
            report()
            if(GangGoods.note !in inv && choice2("Take a note of the meeting.",true,"Leave.",false)) {
                invAdd(inv,GangGoods.note,vars=current+1)
            }
        }
        onOpHeld1(GangGoods.note) {
            val index = (inv[it.slot]?.vars ?: 0)-1
            val district=districts.getOrNull(index) ?: return@onOpHeld1
            mes("The note describes a gang meeting ${district.hint}.")
        }
        onOpHeld1(GangGoods.reward) {
            if (!player.members) return@onOpHeld1
            val choices=listOf(stats.hitpoints,stats.attack,stats.strength,stats.defence)
            val index=menu("Choose a skill to train",true,listOf("Hitpoints","Attack","Strength","Defence","Cancel"))
            val skill=choices.getOrNull(index) ?: return@onOpHeld1
            val level=statBase(skill)
            val xp=if(level<72) 5*(level*level+28*level)/16 else 125*level/4
            if(invDel(inv,GangGoods.reward,1,it.slot).success) statAdvance(skill,xp.toDouble())
        }
    }

    private fun dropIntel(npc:Npc,hero:org.rsmod.game.entity.Player) {
        if (GangNpcs.bosses.any { it.id == npc.id } || random.of(5) == 0) {
            objects.add(GangGoods.intel,npc.coords,constants.lootdrop_duration,hero)
        }
    }

    private fun startMeeting() {
        nextStart=clock.cycle+PERIOD
        gathering=true
        val centre=districts[current].coords
        val flags=CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR
        val tiles=(-3..3).flatMap { dx -> (-3..3).map { dz->centre.translate(dx,dz) } }.filter { tile ->
            collision[tile.x,tile.z,tile.level] and flags == 0
        }.toMutableList()
        if(tiles.isEmpty()) return
        val leader=random.pick(GangNpcs.bosses)
        val number=random.of(4..14)
        val crew=listOf(leader)+List(number){random.pick(GangNpcs.gangsters)}
        for (type in crew) {
            if(tiles.isEmpty()) break
            val tile=tiles.removeAt(random.of(tiles.size))
            val npc=Npc(types[type],tile)
            // The meeting controller resolves surviving NPCs together after five minutes.
            npcs.add(npc,PERIOD)
            members+=npc
        }
    }
    private fun ProtectedAccess.report() {
        val hint=districts[current].hint
        if(gathering) mes("A gang is meeting $hint right now.")
        else mes("A gang meeting $hint is expected in ${((nextStart-clock.cycle).coerceAtLeast(0)+99)/100} minutes.")
    }
    private fun ProtectedAccess.deliver() {
        if(invReplace(inv,GangGoods.intel,1,GangGoods.reward).success) {
            mes("Captain Ginea takes the intelligence and gives you a training manual.")
            player.completeDiaryTask(DiaryTask.DeliverGangIntelligence)
        }
    }
    private companion object { const val PERIOD=3000 }
}
