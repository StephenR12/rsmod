package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.hat
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.proj.ProjAnimTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GuildCoaches : NpcReferences() {
    val gamfred = find("warguild_gamfred")
    val sloane = find("warguild_sloane_npc")
    val yadech = find("warguild_yadech_npc")
    val jimmy = find("war_strength_drunken_warrior")
    val referees = listOf(find("strength_referree1"), find("strength_referree2"))
}

internal object TrainingRoomLocs : LocReferences() {
    val shot = listOf(find("war_strength_shot18"), find("war_strength_shot22"))
    val doors = listOf(find("war_strength_door"), find("war_strength_door_m"))
    val keg = find("war_strength_keg_active")
    val catapultDoor = find("warguild_door_catapult")
    val notes = find("warguild_info_scroll_catapult")
}

internal object TrainingRoomItems : ObjReferences() {
    val shield = find("warguild_shield")
    val kegs = (1..5).map { find("war_strength_barrel$it") }
}

internal object TrainingRoomBits : VarBitReferences() {
    val stance = find("warguild_defence_type")
    val kegs = (1..5).map { find("war_strength_keg$it") }
}

internal object CatapultInterfaces : InterfaceReferences() {
    val stances = find("warguild_defence_mini")
    val notes = find("warguild_defence")
}

internal object CatapultButtons : ComponentReferences() {
    val stances =
        listOf("stab", "crush", "slash", "magic").map { find("warguild_defence_mini:$it") }
}

internal object CatapultSequences : SeqReferences() {
    val blocks = listOf("spike", "blunt", "blade", "mage").map { find("warguild_${it}_def") }
    val misses = listOf("spike", "blunt", "blade", "mage").map { find("warguild_${it}_fail") }
}

internal object CatapultProjectiles : SpotanimReferences() {
    val missiles = listOf("spike", "blunt", "slash", "mage").map { find("warguild_proj_$it") }
}

internal object TrainingRoomTimers : TimerReferences() {
    val catapult = find("guild_catapult")
    val kegs = find("guild_kegs")
}

private val target = CoordGrid(2842, 3545, 1)
private val catapult = CoordGrid(2842, 3552, 1)
private val distances = listOf(58, 71, 84, 97, 110, 124, 137, 150, 163, 176, 190, 203, 216)
private val kegTiles = listOf(2870 to 3542, 2867 to 3536, 2871 to 3536, 2877 to 3540, 2866 to 3539)
private val stances = listOf("stab", "blunt", "slash", "magic")
private var Player.stance by intVarBit(TrainingRoomBits.stance)

class GuildTrainingRooms
@Inject
constructor(
    private val random: GameRandom,
    private val world: WorldRepository,
    private val ground: ObjRepository,
    private val unequip: WornUnequipOp,
    private val events: EventBus,
    private val types: ObjTypeList,
    private val projectiles: ProjAnimTypeList,
) : PluginScript() {
    private val dusted = mutableSetOf<Player>()
    private val nextMissile = mutableMapOf<Player, Int>()
    private val cycles = mutableMapOf<Player, Int>()

    override fun ScriptContext.startup() {
        for ((i, door) in TrainingRoomLocs.doors.withIndex()) onOpLoc1(door) {
            if (!player.canTrainAtGuild() || player.statBase(stats.strength) < 50) {
                mes("You need a Strength level of 50 to open the heavy door.")
                return@onOpLoc1
            }
            statAdvance(stats.strength, random.of(if (i == 0) 12 else 2).toDouble())
            val tile = it.loc.coords
            telejump(CoordGrid(if (coords.x <= 2852) 2853 else 2852, tile.z, 1))
        }
        onOpHeld1(objs.ground_ashes) {
            if (invDel(inv, objs.ground_ashes, 1).success) {
                dusted += player
                mes("You dust your hands with the ground ashes.")
            }
        }
        for ((i, shot) in TrainingRoomLocs.shot.withIndex()) onOpLoc1(shot) {
            if (!readyToThrow()) return@onOpLoc1
            val style =
                menu(
                    "Throwing style",
                    true,
                    listOf("Standing throw", "Step and throw", "Spin and throw"),
                )
            if (style in 0..2 && readyToThrow()) putShot(if (i == 0) 18 else 22, style)
        }
        onOpNpc1(GuildCoaches.sloane) {
            mes(
                "Keep both hands free, pick a shot and choose your throw. Ground ashes improve your grip."
            )
            if (player.statBase(stats.strength) >= 99)
                SkillcapeSale.offer(this, stats.strength, objs.strength_hood)
        }
        onOpNpc1(GuildCoaches.yadech) {
            mes("You can claim your training tokens from me.")
            if (player.statBase(stats.strength) >= 99)
                SkillcapeSale.offer(this, stats.strength, objs.strength_hood)
        }
        onOpNpc1(GuildCoaches.jimmy) {
            mes(
                "Pick up the kegs with your hands and head free. You can claim the tokens from the training staff."
            )
        }
        onOpLoc1(TrainingRoomLocs.keg) {
            val tile = it.loc.coords
            val index = kegTiles.indexOf(tile.x to tile.z)
            if (
                index < 0 ||
                    tile.level != 1 ||
                    !player.canTrainAtGuild() ||
                    player.statBase(stats.strength) < 50
            )
                return@onOpLoc1
            val count = carried(player)
            if (
                player.righthand != null ||
                    player.lefthand != null ||
                    (player.hat != null && player.hat?.id !in 8860..8864)
            ) {
                mes("Free your head and both hands to balance the kegs.")
                return@onOpLoc1
            }
            if (count >= 5 || player.vars[TrainingRoomBits.kegs[index]] != 0) return@onOpLoc1
            VarPlayerIntMapSetter.set(player, TrainingRoomBits.kegs[index], 1)
            player.hat = InvObj(TrainingRoomItems.kegs[count])
            player.rebuildAppearance()
            if (count == 0) player.softTimer(TrainingRoomTimers.kegs, 4)
        }
        onPlayerSoftTimer(TrainingRoomTimers.kegs) {
            val count = carried(player)
            val c = player.coords
            if (
                count == 0 ||
                    player.hat?.id != 8859 + count ||
                    player.lefthand != null ||
                    player.righthand != null ||
                    c.level != 1 ||
                    c.x !in 2864..2878 ||
                    c.z !in 3534..3543
            ) {
                stopKegs(player)
                return@onPlayerSoftTimer
            }
            val energy = player.runEnergy / 100
            if (energy == 0 || random.of(1000) < 100 - energy) {
                player.queueHit(1, HitType.Typeless, random.of(5))
                stopKegs(player)
                return@onPlayerSoftTimer
            }
            spendEnergy(player, 100 * count)
            player.earnGuildTokens(count)
            player.statAdvance(stats.strength, count.toDouble())
        }
        onOpNpc1(GuildCoaches.gamfred) { giveShield() }
        onOpNpc4(GuildCoaches.gamfred) { giveShield() }
        onOpLoc1(TrainingRoomLocs.notes) { ifOpenMainModal(CatapultInterfaces.notes) }
        onOpLoc1(TrainingRoomLocs.catapultDoor) {
            if (player.canTrainAtGuild())
                telejump(CoordGrid(2842, if (coords.z <= 3542) 3543 else 3542, 1))
        }
        onEvent<HeldEquipEvents.WearposChange> {
            if (
                objType.id == TrainingRoomItems.shield.id &&
                    player.lefthand?.id == objType.id &&
                    player.coords == target
            ) {
                startCatapult(player)
            }
        }
        for ((i, button) in CatapultButtons.stances.withIndex()) onIfOverlayButton(button) {
            if (player.coords == target && player.lefthand?.id == TrainingRoomItems.shield.id)
                player.stance = i
        }
        onPlayerSoftTimer(TrainingRoomTimers.catapult) {
            if (player.coords != target || player.lefthand?.id != TrainingRoomItems.shield.id) {
                stopCatapult(player)
                return@onPlayerSoftTimer
            }
            val tick = (cycles.getValue(player) + 1) % 8
            cycles[player] = tick
            if (tick == 4) launch(player)
            if (tick == 0) block(player)
        }
        onPlayerLogin {
            stopKegs(player)
            if (player.lefthand?.id == TrainingRoomItems.shield.id) {
                if (player.coords == target) startCatapult(player) else stopCatapult(player)
            }
        }
        onPlayerLogout {
            dusted.remove(player)
            stopKegs(player)
            if (cycles.containsKey(player)) stopCatapult(player)
        }
    }

    private fun ProtectedAccess.readyToThrow(): Boolean {
        if (!player.canTrainAtGuild() || player.statBase(stats.strength) < 50) return false
        if (player.righthand != null || player.lefthand != null) {
            mes("Free both hands before putting the shot.")
            return false
        }
        if (player.runEnergy >= 500) return true
        mes("You are too tired to put a shot. Take a rest.")
        return false
    }

    private fun ProtectedAccess.putShot(weight: Int, style: Int) {
        val score =
            player.statBase(stats.strength) + player.runEnergy / 100 + 30 - 10 * style +
                (if (dusted.remove(player)) 5 else 0) - weight
        spendEnergy(player, score * 10)
        if (random.of(250) >= score) {
            player.queueHit(1, HitType.Typeless, 1)
            mes("You drop the shot on your toe.")
            return
        }
        val yards = 1 + distances.count { score >= it }
        statAdvance(stats.strength, score * 0.7)
        player.earnGuildTokens(yards + if (weight == 18) 1 else 3)
        mes("You put the shot $yards yards.")
    }

    private fun spendEnergy(p: Player, amount: Int) {
        p.runEnergy = (p.runEnergy - amount).coerceAtLeast(0)
        UpdateRun.energy(p, p.runEnergy)
    }

    private fun carried(p: Player): Int = TrainingRoomBits.kegs.count { p.vars[it] != 0 }

    private fun stopKegs(p: Player) {
        for (bit in TrainingRoomBits.kegs) if (p.vars[bit] != 0)
            VarPlayerIntMapSetter.set(p, bit, 0)
        if (p.hat?.id in 8860..8864) {
            p.hat = null
            p.rebuildAppearance()
        }
        p.clearSoftTimer(TrainingRoomTimers.kegs)
    }

    private fun ProtectedAccess.giveShield() {
        if (!player.canTrainAtGuild()) return
        if (TrainingRoomItems.shield !in inv && TrainingRoomItems.shield !in worn)
            invAdd(inv, TrainingRoomItems.shield)
        mes("Wield the shield on the target and choose the guard for each incoming shot.")
    }

    private fun startCatapult(p: Player) {
        p.stance = 0
        cycles[p] = 0
        p.ifOpenOverlay(CatapultInterfaces.stances, components.toplevel_target_side3, events)
        for (button in CatapultButtons.stances) p.ifSetEvents(button, -1..-1, IfEvent.Op1)
        p.softTimer(TrainingRoomTimers.catapult, 1)
    }

    private fun launch(p: Player) {
        val missile = random.of(4)
        nextMissile[p] = missile
        val graphic = CatapultProjectiles.missiles[missile]
        world.projAnim(
            ProjAnim.fromBoundsToPlayer(
                Bounds(catapult, 1),
                p,
                graphic.id,
                projectiles[projanims.bolt],
            )
        )
        p.mes("The catapult fires a ${stances[missile]} attack!")
    }

    private fun block(p: Player) {
        val missile = nextMissile.remove(p) ?: return
        if (p.stance == missile) {
            p.anim(CatapultSequences.blocks[missile])
            p.statAdvance(stats.defence, 10.0)
            p.earnGuildTokens(1)
        } else {
            p.anim(CatapultSequences.misses[missile])
            p.queueHit(1, HitType.Typeless, random.of(1, 4))
        }
    }

    private fun stopCatapult(p: Player) {
        nextMissile.remove(p)
        val active = cycles.remove(p) != null
        p.clearSoftTimer(TrainingRoomTimers.catapult)
        if (active) {
            p.ifCloseOverlay(CatapultInterfaces.stances, events)
            p.ifOpenOverlay(interfaces.inventory, components.toplevel_target_side3, events)
        }
        if (p.lefthand?.id != TrainingRoomItems.shield.id) return
        val result = unequip.unequip(p, Wearpos.LeftHand.slot, p.worn, p.inv)
        if (result is org.rsmod.api.player.worn.WornUnequipResult.Fail.NotEnoughInvSpace) {
            p.lefthand = null
            WornUnequipOp.notifyWornUnequip(
                p,
                Wearpos.LeftHand,
                types[TrainingRoomItems.shield],
                events,
            )
            ground.add(TrainingRoomItems.shield, p.coords, constants.lootdrop_duration, p)
        }
    }
}
