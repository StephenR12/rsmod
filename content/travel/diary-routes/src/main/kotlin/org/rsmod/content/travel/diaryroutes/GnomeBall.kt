package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.interact.OpDefaultEvent
import org.rsmod.api.player.events.interact.PlayerEvents
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.worn.WornUnequipResult
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GnomeBallVars : VarpReferences() {
    val match = find("gnomeball_match_state")
    val deposit = find("gnomeball_held_deposit")
}

internal object GnomeBallSaves : VarpBuilder() {
    init {
        for (n in listOf("gnomeball_match_state", "gnomeball_held_deposit")) build(n) {
            transmitNever = true
        }
    }
}

internal object GnomeBallBits : VarBitReferences() {
    val score = find("gnomeball_score")
    val iron = find("ironman")
}

internal object GnomeBallNpcs : NpcReferences() {
    val ref = find("gnomereferee")
    val winger = find("gnomeballer_green")
    val passing = find("gnomeballer_green_withball")
    val rivals =
        listOf("yellow", "orange", "red").map { team ->
            listOf("", "_withball", "_tackled", "_tackled2").map { find("gnomeballer_$team$it") }
        }
}

internal object GnomeBallLocs : LocReferences() {
    val gate = find("gate_gnomeball_game")
    val goal = find("goal_gnomeball_game")
}

internal object GnomeBallItem : ObjReferences() {
    val ball = find("ball_gnomeball_game")
}

internal object GnomeBallHud : InterfaceReferences() {
    val score = find("gnomeball")
}

internal object GnomeBallTimer : TimerReferences() {
    val play = find("gnomeball_pitch")
}

internal object GnomeBallSeq : SeqReferences() {
    val shoot = find("human_throw_gnomeball")
    val tackle = find("human_gnome_tackle")
    val fall = find("human_tackled")
}

internal object GnomeBallSpot : SpotanimReferences() {
    val ball = find("gnomeball_travel")
}

public class GnomeBall
@Inject
constructor(
    private val types: NpcTypeList,
    private val items: ObjTypeList,
    private val npcs: NpcRepository,
    private val world: WorldRepository,
    private val rng: GameRandom,
) : PluginScript() {
    private data class Play(
        var hasBall: Boolean = false,
        var carrier: Npc? = null,
        var returnedAt: Int = 0,
        var protectedUntil: Int = 0,
        var hud: Boolean = false,
    )

    private val plays = WeakHashMap<Player, Play>()
    private val hand = Wearpos.RightHand.slot

    override fun ScriptContext.startup() {
        onOpLoc1(GnomeBallLocs.gate) {
            if (!player.members) return@onOpLoc1
            if (vars[GnomeBallVars.match] != 0) {
                finish(true)
                telejump(CoordGrid(2382, 3488))
            } else {
                if (vars[GnomeBallVars.match] == 0 && vars[GnomeBallVars.deposit] > 0 && !refund())
                    return@onOpLoc1
                if (player.righthand != null && wornUnequip(hand) != WornUnequipResult.Success) {
                    mes("You need space to put away your weapon.")
                    return@onOpLoc1
                }
                val count = inv.count(items[GnomeBallItem.ball])
                if (count > 0 && !invDel(inv, GnomeBallItem.ball, count).success) return@onOpLoc1
                vars[GnomeBallVars.deposit] += count
                vars[GnomeBallVars.match] = 1
                vars[GnomeBallBits.score] = 0
                telejump(CoordGrid(2384, 3488))
                open()
            }
        }
        onOpNpc1(GnomeBallNpcs.ref) {
            if (!playing()) {
                if (
                    !inside(coords) &&
                        vars[GnomeBallVars.match] == 0 &&
                        vars[GnomeBallVars.deposit] > 0
                )
                    refund()
                else mes("Enter the pitch through the gate to play Gnome Ball.")
                return@onOpNpc1
            }
            val p = open()
            if (p.hasBall || p.carrier != null) {
                mes("The ball is still in play!")
                return@onOpNpc1
            }
            giveBall(player, p)
            mes("The referee gives you the ball.")
        }
        onApNpc1(GnomeBallNpcs.winger) { if (isWithinApRange(it.npc, 12)) winger(it.npc) }
        onOpNpc1(GnomeBallNpcs.winger) { winger(it.npc) }
        onOpNpc1(GnomeBallNpcs.passing) { mes("The winger is looking for a pass.") }
        for (team in GnomeBallNpcs.rivals) for (n in team.take(2)) onOpNpc1(n) {
            if (!playing()) return@onOpNpc1
            val p = open()
            if (it.npc.transmog?.id in team.drop(2).map { it.id }) return@onOpNpc1
            anim(GnomeBallSeq.tackle)
            if (rng.of(100) >= 45 + stat(stats.agility) / 2) {
                mes("The gnome dodges your tackle.")
                return@onOpNpc1
            }
            val holder = plays.entries.firstOrNull { entry -> entry.value.carrier === it.npc }
            if (holder != null && !p.hasBall) {
                holder.value.carrier = null
                giveBall(player, p)
            }
            it.npc.transmog(types[team[2]], 5)
            mes("You tackle the gnome.")
        }
        onOpLoc1(GnomeBallLocs.goal) { shoot(it.loc.coords) }
        onApLoc1(GnomeBallLocs.goal) { if (isWithinApRange(it.loc, 20)) shoot(it.loc.coords) }
        onProtectedEvent<PlayerEvents.Op5>(OpDefaultEvent.ID) { pass(it.target) }
        onOpPlayerU(GnomeBallItem.ball) { throwItem(it.target, it.invSlot) }
        onApPlayerU(GnomeBallItem.ball) {
            if (isWithinApRange(it.target, 10)) throwItem(it.target, it.invSlot)
        }
        onPlayerTimer(GnomeBallTimer.play) {
            if (!playing()) {
                finish(false)
                return@onPlayerTimer
            }
            val p = open()
            val carrier = p.carrier
            if (carrier != null && p.returnedAt > 0 && mapClock >= p.returnedAt) {
                clearCarrier(p)
                world.projAnim(carrier, player, GnomeBallSpot.ball, projanims.thrown)
                giveBall(player, p)
            }
            if (p.hasBall && mapClock >= p.protectedUntil) opponentTackle(p)
            timer(GnomeBallTimer.play, 1)
        }
        onPlayerLogin {
            if (player.vars[GnomeBallVars.match] != 0 || player.vars[GnomeBallVars.deposit] > 0)
                player.timer(GnomeBallTimer.play, 1)
        }
        onPlayerLogout {
            plays.remove(player)?.let { clearCarrier(it) }
            player.appearance.heldProp = null
        }
    }

    private fun inside(c: CoordGrid): Boolean {
        if (c.level == 0 && c.x == 2383 && c.z == 3488) return true
        if (c.level != 0 || c.x !in 2384..2407 || c.z !in 3482..3495) return false
        val dist = kotlin.math.abs(c.z - 3488)
        return dist <= 3 || c.x in (2384 + dist - 3)..(2407 - dist + 3)
    }

    private fun ProtectedAccess.playing() =
        player.members && vars[GnomeBallVars.match] != 0 && inside(coords)

    private fun ProtectedAccess.open(): Play {
        val p = plays.getOrPut(player) { Play() }
        if (!p.hud) {
            ifOpenOverlay(GnomeBallHud.score)
            MiscOutput.setPlayerOp(player, 5, "Pass-to")
            p.hud = true
        }
        timer(GnomeBallTimer.play, 1)
        return p
    }

    private fun ProtectedAccess.refund(): Boolean {
        val count = vars[GnomeBallVars.deposit]
        if (count == 0) return true
        if (!invAdd(inv, GnomeBallItem.ball, count).success) {
            mes("Make room and ask the referee for your gnomeball.")
            return false
        }
        vars[GnomeBallVars.deposit] = 0
        return true
    }

    private fun ProtectedAccess.finish(atGate: Boolean) {
        val p = plays.remove(player)
        if (p != null) {
            clearCarrier(p)
            if (p.hud) ifCloseSub(GnomeBallHud.score)
        }
        player.appearance.heldProp = null
        MiscOutput.setPlayerOp(player, 5, null)
        clearTimer(GnomeBallTimer.play)
        if (atGate && vars[GnomeBallVars.match] == 2 && vars[GnomeBallVars.deposit] == 0)
            vars[GnomeBallVars.deposit] = 1
        vars[GnomeBallVars.match] = 0
        vars[GnomeBallBits.score] = 0
        refund()
    }

    private fun clearCarrier(p: Play) {
        p.carrier?.resetTransmog()
        p.carrier = null
        p.returnedAt = 0
    }

    private fun giveBall(player: Player, p: Play) {
        p.hasBall = true
        p.protectedUntil = player.currentMapClock + 4
        player.appearance.heldProp = GnomeBallItem.ball.id
    }

    private fun ProtectedAccess.release(p: Play) {
        p.hasBall = false
        player.appearance.heldProp = null
        anim(GnomeBallSeq.shoot)
    }

    private fun ProtectedAccess.winger(npc: Npc) {
        if (!playing()) return
        val p = open()
        if (!p.hasBall || npc.transmog != null) return
        release(p)
        world.projAnim(player, npc, GnomeBallSpot.ball, projanims.thrown)
        npc.transmog(types[GnomeBallNpcs.passing], 5)
        p.carrier = npc
        p.returnedAt = mapClock + 4
    }

    private fun ProtectedAccess.opponentTackle(p: Play) {
        val rival =
            npcs.findAll(ZoneKey.from(coords), 1).firstOrNull { n ->
                n.transmog == null &&
                    n.coords.chebyshevDistance(coords) <= 1 &&
                    GnomeBallNpcs.rivals.any { team -> team[0].id == n.id }
            } ?: return
        p.protectedUntil = mapClock + 4
        if (rng.of(100) >= 65 - stat(stats.agility) / 2) return
        val team = GnomeBallNpcs.rivals.first { it[0].id == rival.id }
        p.hasBall = false
        player.appearance.heldProp = null
        p.carrier = rival
        rival.transmog(types[team[1]], Int.MAX_VALUE)
        anim(GnomeBallSeq.fall)
        takeInstantHit(HitType.Typeless, rng.of(1, 2))
        mes("The gnome tackles you and takes the ball!")
    }

    private fun ProtectedAccess.shoot(goal: CoordGrid) {
        if (!playing()) return
        val p = open()
        if (!p.hasBall) {
            mes("You need the ball to take a shot.")
            return
        }
        release(p)
        world.projAnim(player, goal, GnomeBallSpot.ball, projanims.thrown)
        val chance =
            (85 + stat(stats.ranged) / 3 - 3 * goal.chebyshevDistance(coords)).coerceIn(10, 95)
        if (rng.of(100) >= chance) {
            mes("The ball misses the goal.")
            return
        }
        val n = vars[GnomeBallBits.score] + 1
        val xp = listOf(4.0, 5.0, 6.0, 7.0, 30.0)[n.coerceIn(1, 5) - 1]
        statAdvance(stats.ranged, xp)
        statAdvance(stats.agility, xp)
        vars[GnomeBallBits.score] = if (n >= 5) 0 else n
        if (n >= 5) {
            vars[GnomeBallVars.match] = 2
            mes("Five goals! You win the game.")
        } else mes("Goal!")
        player.completeDiaryTask(DiaryTask.ScoreGnomeBall)
    }

    private fun ProtectedAccess.pass(target: Player) {
        if (
            !playing() ||
                !inside(target.coords) ||
                !isWithinDistance(target, 12) ||
                player.vars[GnomeBallBits.iron] != 0 ||
                target.vars[GnomeBallBits.iron] != 0
        )
            return
        val from = plays[player] ?: return
        val to = plays[target] ?: return
        if (!from.hasBall || to.hasBall || to.carrier != null) return
        release(from)
        world.projAnim(player, target, GnomeBallSpot.ball, projanims.thrown)
        giveBall(target, to)
        target.mes("You catch the gnomeball.")
    }

    private fun ProtectedAccess.throwItem(target: Player, slot: Int) {
        if (
            !player.members ||
                playing() ||
                inside(target.coords) ||
                target.isAccessProtected ||
                target.righthand != null ||
                vars[GnomeBallBits.iron] != 0 ||
                target.vars[GnomeBallBits.iron] != 0 ||
                inv[slot]?.id != GnomeBallItem.ball.id
        )
            return
        if (
            player
                .invTransaction(inv, target.worn) {
                    delete(select(inv), GnomeBallItem.ball.id, 1, slot = slot)
                    add(select(target.worn), GnomeBallItem.ball.id, 1, slot = hand)
                }
                .success
        ) {
            anim(GnomeBallSeq.shoot)
            world.projAnim(player, target, GnomeBallSpot.ball, projanims.thrown)
            target.mes("You catch the gnomeball.")
        }
    }
}
