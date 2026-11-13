package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object StrongholdLocations : LocReferences() {
    val entrance = find("sos_dung_ent_open")
    private val floors = listOf("war","fam","pest","death")
    val up = floors.map { find("sos_${it}_ladd_up") }
    val down = floors.take(3).map { find("sos_${it}_ladd_down") }
    val ropes = floors.drop(1).map { find("sos_${it}_rope_up") }
    val portals = floors.map { find("sos_${it}_portal") }
    val gates = floors.map { listOf(find("sos_${it}_door_face"),find("sos_${it}_door_face_mirr")) }
    val rewards = listOf("sos_war_chest","sos_fam_sack","sos_pest_chest","sos_death_pram").map { find(it) }
}
internal object StrongholdUnlocks : VarBitReferences() {
    val emotes = listOf("flap","doh","idea","stamp").map { find("sos_emote_$it") }
    val imbued = find("sos_sceptre_imbued")
}
internal object StrongholdBoots : ObjReferences() {
    val fancy = find("sos_boots")
    val fighting = find("sos_boots2")
    val fancier = find("sos_boots3")
}

public class StrongholdOfSecurity @Inject constructor(
    private val locs:LocRepository,
    private val skills:StatTypeList
) : PluginScript() {
    private val starts = listOf(CoordGrid(1859,5243),CoordGrid(2042,5245),CoordGrid(2123,5252),CoordGrid(2358,5215))
    private val treasures = listOf(CoordGrid(1907,5223),CoordGrid(2020,5215),CoordGrid(2144,5281),CoordGrid(2344,5215))
    private val previous = listOf(surface,CoordGrid(1903,5222),CoordGrid(2025,5218),CoordGrid(2148,5283))
    private val combatShortcuts = listOf(26,51,76,Int.MAX_VALUE)
    private val questions = listOf(
        Question("A player asks for your password. What should you do?",listOf("Tell them privately.","Keep it secret and report them.","Give them a hint."),1),
        Question("Where can you set a bank PIN?",listOf("Speak to a banker.","Ask another player.","At any general store."),0),
        Question("Someone offers to double your coins. What should you do?",listOf("Trade them a few coins.","Share your PIN.","Decline the offer."),2),
        Question("How can you better protect your account?",listOf("Reuse a password.","Enable two-factor authentication.","Share it with a friend."),1),
    )
    private data class Question(val text:String,val answers:List<String>,val correct:Int)

    override fun ScriptContext.startup() {
        onOpLoc1(StrongholdLocations.entrance) { teleport(starts.first()) }
        for ((floor,ladder) in StrongholdLocations.up.withIndex()) onOpLoc1(ladder) { teleport(previous[floor]) }
        for ((floor,ladder) in StrongholdLocations.down.withIndex()) onOpLoc1(ladder) {
            teleport(starts[floor+1])
            if (floor == 0) player.completeDiaryTask(DiaryTask.EnterStrongholdSecondFloor)
        }
        for ((index,rope) in StrongholdLocations.ropes.withIndex()) onOpLoc1(rope) {
            if (index == 2 && it.loc.x == 2350) teleport(surface) else teleport(starts[index+1])
        }
        for ((floor,portal) in StrongholdLocations.portals.withIndex()) onOpLoc1(portal) {
            if (vars[StrongholdUnlocks.emotes[floor]] != 0 || player.combatLevel >= combatShortcuts[floor]) {
                teleport(treasures[floor])
            } else mes("You must first reach this floor's treasure room on foot.")
        }
        for ((floor,gates) in StrongholdLocations.gates.withIndex()) for (gate in gates) onOpLoc1(gate) {
            passGate(it.loc,floor)
        }
        for ((floor,reward) in StrongholdLocations.rewards.withIndex()) onOpLoc1(reward) { claim(floor) }
    }

    private suspend fun ProtectedAccess.passGate(gate:BoundLocInfo,floor:Int) {
        val origin = gate.coords
        val outside = when (gate.angleId) {
            0 -> origin.translate(-1,0)
            1 -> origin.translate(0,1)
            2 -> origin.translate(1,0)
            else -> origin.translate(0,-1)
        }
        val horizontal = origin.x != outside.x
        val offset = if (horizontal) outside.x-origin.x else outside.z-origin.z
        val along = if (horizontal) coords.x-origin.x else coords.z-origin.z
        val fromOuterTile = along * offset > 0
        val direction = if (fromOuterTile) offset else -offset
        // The paired gates enclose a safe corridor. Walking into it always permits immediate escape.
        val insideCorridor = (2..4).any { distance ->
            val tile = origin.translate(if (horizontal) distance*direction else 0,if (horizontal) 0 else distance*direction)
            StrongholdLocations.gates[floor].any { locs.findExact(tile,it) != null }
        }
        if (insideCorridor && vars[StrongholdUnlocks.emotes[floor]] == 0) {
            val question = random.pick(questions)
            if (menu(question.text,true,question.answers) != question.correct) {
                mes("The gate remains closed.")
                return
            }
        }
        teleport(if (fromOuterTile) origin else outside)
    }

    private suspend fun ProtectedAccess.claim(floor:Int) {
        if (!player.twoFactorEnabled) {
            mes("Enable two-factor authentication on your account to claim this reward.")
            return
        }
        val unlock = StrongholdUnlocks.emotes[floor]
        if (floor == 3) {
            val choice = menu("Which boots would you like?",true,listOf("Fancy boots","Fighting boots","Leave"))
            val boots = when (choice) { 0 -> StrongholdBoots.fancy; 1 -> StrongholdBoots.fighting; else -> return }
            if (!invAdd(inv,boots).success) return
            if (vars[unlock] != 0) return
        } else {
            if (vars[unlock] != 0) { mes("You have already claimed this reward."); return }
            if (!invAdd(inv,objs.coins,listOf(2000,3000,5000)[floor]).success) return
        }
        vars[unlock] = 1
        val restore = if (floor == 2) skills.values else listOf(stats.hitpoints,stats.prayer)
        for (skill in restore) if (stat(skill) < statBase(skill)) statRestore(skill)
        mes("You unlock the ${listOf("Flap","Slap Head","Idea","Stamp")[floor]} emote.")
    }

    internal companion object { val surface = CoordGrid(3081,3421) }
}
