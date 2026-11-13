package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object SpecialTheftNpcs : NpcReferences() {
    val phoenix = find("golem_phoenix")
    val martin = find("roguesden_trader")
    val banker = find("roguesden_banker")
}

public object SpecialTheftLocs : LocReferences() {
    val safe = find("roguesden_walldecor_safe")
    val open = find("roguesden_walldecor_safe_open")
    val spikes = find("roguesden_trap_floor_spikes")
    val entrance = find("roguesden_trapdoor_entrance")
    val exit = find("roguesden_passage_to_pub")
    val door = find("roguesden_door_to_pub")
    val doorOpen = find("roguesden_door_to_pub_open")
}

public object SpecialTheftObjs : ObjReferences() {
    val feather = find("golem_phoenixfeather")
    val pen = find("golem_pen")
    val stethoscope = find("roguesden_stethoscope")
    val lockpick = find("lockpick")
    val claws = listOf("bronze", "iron", "steel").map { find(it + "_claws") }
    val knives = listOf("bronze", "iron", "steel").map { find(it + "_knife") }
}

public object SpecialTheftInvs : InvReferences() {
    val shop = find("roguesden_shop")
}

public object SpecialTheftSeqs : SeqReferences() {
    val start = find("rogue_player_crack_safe_start")
    val mid = find("rogue_player_crack_safe_mid")
    val end = find("rogue_player_crack_safe_end")
}

internal object SpecialTheftStock : InvEditor() {
    init {
        edit(SpecialTheftInvs.shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for ((obj, count) in
                listOf(
                    objs.rope to 50,
                    SpecialTheftObjs.lockpick to 25,
                    objs.chisel to 30,
                    objs.knife to 20,
                    SpecialTheftObjs.stethoscope to 25,
                ) +
                    SpecialTheftObjs.knives.zip(listOf(15, 10, 5)) +
                    SpecialTheftObjs.claws.zip(listOf(3, 2, 1))) stock += stock(obj, count, 100)
        }
    }
}

internal object SpecialTheftPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<SpecialThefts>("special-thefts.toml")
    }
}

/** Wall safe14997332; Desert Phoenix14768269 and feather14788009, all pre-cache cutoff. */
public class SpecialThefts
@Inject
constructor(
    private val locs: LocRepository,
    private val shops: Shops,
    private val stun: ThievingStunStateRepository,
    private val objects: ObjTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3(SpecialTheftNpcs.phoenix) { phoenix(this, it.npc) }
        onOpLoc1(SpecialTheftLocs.safe) { safe(this, it.loc) }
        onOpLoc1(SpecialTheftLocs.spikes) {
            if (
                player.coords.level == 1 &&
                    player.coords.x in 3035..3065 &&
                    player.coords.z in 4965..4990
            )
                statSub(stats.thieving, 1, 0)
        }
        onOpLoc1(SpecialTheftLocs.entrance) {
            if (player.members) telejump(CoordGrid(3061, 4985, 1))
        }
        onOpLoc1(SpecialTheftLocs.exit) { telejump(CoordGrid(2906, 3537)) }
        onOpLoc1(SpecialTheftLocs.door) {
            locs.change(it.loc, SpecialTheftLocs.doorOpen, 4)
            telejump(CoordGrid(3061, if (player.coords.z >= 4985) 4983 else 4985, 1))
        }
        onOpNpc3(SpecialTheftNpcs.martin) { trade() }
        onOpNpc1(SpecialTheftNpcs.martin) {
            if (choice2("Show me your shop.", true, "Leave.", false)) trade()
        }
        onOpNpc3(SpecialTheftNpcs.banker) {
            ifOpenMainSidePair(interfaces.bank_main, interfaces.bank_side)
        }
    }

    private fun ProtectedAccess.trade() {
        if (!player.members) return
        if (
            player.statBase(stats.thieving) < 99 &&
                (player.statBase(stats.thieving) < 50 || player.statBase(stats.agility) < 50)
        ) {
            mes("You need level 50 Thieving and Agility to use Martin's shop.")
            return
        }
        shops.open(
            player,
            "Martin Thwait's Lost and Found",
            SpecialTheftInvs.shop,
            60.0,
            100.0,
            2.0,
        )
    }

    public fun phoenix(access: ProtectedAccess, npc: Npc): Unit =
        with(access) {
            if (!player.members || actionDelay > player.currentMapClock) return
            if (player.thievingLvl < 25) {
                mes("You need a Thieving level of 25 to grab a phoenix feather.")
                return
            }
            if (
                player.invMap.values.any {
                    SpecialTheftObjs.feather in it || SpecialTheftObjs.pen in it
                }
            ) {
                mes("You already have a phoenix feather or quill pen.")
                return
            }
            if (inv.isFull()) {
                mes("You don't have enough inventory space.")
                return
            }
            faceEntitySquare(npc)
            anim(seqs.human_pickpocket)
            actionDelay = player.currentMapClock + 3
            // Exact Phoenix endpoints are unpublished: bounded playable curve pending historical
            // verification.
            if (statRandom(stats.thieving, 80, 240, 0)) {
                if (invAdd(inv, SpecialTheftObjs.feather).success) {
                    statAdvance(stats.thieving, 26.0)
                    player.completeDiaryTask(DiaryTask.StealDesertPhoenixFeather)
                    mes("You pluck a feather from the phoenix.")
                }
            } else {
                actionDelay = player.currentMapClock + 6
                stun.stun(player, 6)
                anim(seqs.stunned_thieving)
                spotanim(spotanims.stunned_thieving)
                queueHit(npc, 1, HitType.Typeless, 1)
                mes("The phoenix catches you trying to pluck a feather.")
            }
        }

    public suspend fun safe(access: ProtectedAccess, loc: BoundLocInfo): Unit =
        with(access) {
            if (!player.members || actionDelay > player.currentMapClock) return
            if (player.thievingLvl < 50) {
                mes("You need a Thieving level of 50 to crack this safe.")
                return
            }
            if (inv.isFull() && objs.coins !in inv) {
                mes("You don't have enough inventory space.")
                return
            }
            anim(SpecialTheftSeqs.start)
            while (true) {
                gatheringDelay(4)
                if (!isWithinDistance(loc, 1) || player.thievingLvl < 50) return
                val scope = SpecialTheftObjs.stethoscope in inv
                if (statRandom(stats.thieving, if (scope) 16 else 8, if (scope) 192 else 160, 0)) {
                    val roll = random.of(128)
                    val (id, count) =
                        when (roll) {
                            in 0..24 -> objs.coins.id to 10
                            in 25..80 -> objs.coins.id to 20
                            in 81..101 -> objs.coins.id to 40
                            in 102..113 -> objs.uncut_sapphire.id to 1
                            in 114..122 -> objs.uncut_emerald.id to 1
                            in 123..126 -> objs.uncut_ruby.id to 1
                            else -> objs.uncut_diamond.id to 1
                        }
                    val tx = player.invTransaction(inv) { add(select(inv), id, count) }
                    if (!tx.success) {
                        mes("You don't have enough inventory space.")
                        resetAnim()
                        return
                    }
                    statAdvance(stats.thieving, 70.0)
                    player.completeDiaryTask(DiaryTask.CrackRoguesDenSafe)
                    anim(SpecialTheftSeqs.end)
                    locs.change(loc, SpecialTheftLocs.open, 4)
                    actionDelay = player.currentMapClock + 4
                    gatheringDelay(4)
                    resetAnim()
                    return
                }
                // Period article documents an estimated half of unsuccessful attempts triggering
                // the trap.
                if (random.of(2) == 0) {
                    queueHit(1, HitType.Typeless, random.of(2..6))
                    locs.add(
                        player.coords,
                        SpecialTheftLocs.spikes,
                        8,
                        LocAngle.West,
                        LocShape.CentrepieceStraight,
                    )
                    actionDelay = player.currentMapClock + 4
                    anim(seqs.stunned_thieving)
                    mes("You trigger a trap!")
                    gatheringDelay(4)
                    resetAnim()
                    return
                }
                anim(SpecialTheftSeqs.mid)
            }
        }
}
