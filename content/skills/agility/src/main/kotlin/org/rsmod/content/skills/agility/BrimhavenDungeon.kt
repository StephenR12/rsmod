package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object BrimhavenGateVars : VarBitReferences() {
    val entryFee = find("karam_dungeon_entryfee")
    val permanentAccess = find("karam_dungeon_permanentaccess")
}

internal object BrimhavenGateNpc : NpcReferences() {
    val saniboch = find("tribesman_doorman")
}

internal object BrimhavenGateSpawn : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<BrimhavenGateSpawn>("brimhaven-dungeon.toml")
    }
}

public class BrimhavenDungeon
@Inject
constructor(
    private val types: LocTypeList,
    private val objects: ObjTypeList,
    private val locs: LocRepository,
) : PluginScript() {
    private data class Vine(val outside: CoordGrid, val level: Int)

    private val vines =
        listOf(
            Vine(CoordGrid(2691, 9564), 10),
            Vine(CoordGrid(2683, 9568), 27),
            Vine(CoordGrid(2674, 9499), 22),
            Vine(CoordGrid(2674, 9479), 34),
            Vine(CoordGrid(2693, 9482), 34),
        )

    override fun ScriptContext.startup() {
        val names = types.values.associateBy { it.internalName }
        onOpNpc3(BrimhavenGateNpc.saniboch) { pay(false) }
        onOpNpc1(BrimhavenGateNpc.saniboch) {
            if (vars[BrimhavenGateVars.permanentAccess] != 0) {
                mes("You have permanent access to the dungeon.")
                return@onOpNpc1
            }
            when (
                choice3(
                    "Pay 875 coins for one visit.",
                    1,
                    "Pay 1,000,000 coins for permanent access.",
                    2,
                    "Leave.",
                    0,
                )
            ) {
                1 -> pay(false)
                2 -> pay(true)
            }
        }
        for (name in listOf("dungeon_tree_closed", "dungeon_tree_unpaid", "dungeon_tree_open")) {
            onOpLoc1(names.getValue(name)) {
                if (!player.members) return@onOpLoc1
                if (
                    vars[BrimhavenGateVars.entryFee] == 0 &&
                        vars[BrimhavenGateVars.permanentAccess] == 0
                )
                    pay(false)
                else {
                    climb(CoordGrid(2713, 9564))
                    vars[BrimhavenGateVars.entryFee] = 0
                }
            }
        }
        onOpLoc1(names.getValue("karam_dungeon_exit")) { climb(CoordGrid(2744, 3152)) }
        for ((index, vine) in vines.withIndex()) {
            onOpLoc1(names.getValue("karam_dungeon_vineblocking${index+1}")) {
                cut(it.loc, vine, index == 0)
            }
        }
        onOpLoc1(names.getValue("karam_dungeon_cavestairs")) { climb(CoordGrid(2643, 9594, 2)) }
        onOpLoc1(names.getValue("karam_dungeon_cavestairs_lev2_down")) {
            climb(CoordGrid(2649, 9591))
        }
        onOpLoc1(names.getValue("karam_dungeon_cavestairs2")) {
            climb(CoordGrid(2636, 9510, 2))
            player.completeDiaryTask(DiaryTask.ClimbBrimhavenDungeonStairs)
        }
        onOpLoc1(names.getValue("karam_dungeon_cavestairs_lev2_down2")) {
            climb(CoordGrid(2636, 9517))
        }
    }

    private fun ProtectedAccess.pay(permanent: Boolean) {
        if (!player.members) return
        if (
            vars[BrimhavenGateVars.permanentAccess] != 0 ||
                (!permanent && vars[BrimhavenGateVars.entryFee] != 0)
        ) {
            mes("You have already paid to enter the dungeon.")
            return
        }
        val cost = if (permanent) 1_000_000 else 875
        if (!invDel(inv, objs.coins, cost).success) {
            mes("You need $cost coins to pay Saniboch.")
            return
        }
        if (permanent) vars[BrimhavenGateVars.permanentAccess] = 1
        else vars[BrimhavenGateVars.entryFee] = 1
        mes("Saniboch accepts your payment.")
    }

    private suspend fun ProtectedAccess.climb(dest: CoordGrid) {
        anim(seqs.human_reachforladder)
        delay(2)
        telejump(dest)
        resetAnim()
    }

    private suspend fun ProtectedAccess.cut(target: BoundLocInfo, vine: Vine, entrance: Boolean) {
        if (!player.members) return
        val inward = coords == vine.outside
        val axe = Woodcutting.findAxe(player, objects)
        if (inward && stat(stats.woodcutting) < vine.level) {
            mes("You need a Woodcutting level of ${vine.level} to cut these vines.")
            return
        }
        if (inward && axe == null) {
            mes("You need an axe to cut these vines.")
            return
        }
        faceLoc(target)
        if (axe != null) anim(objects[axe].axeWoodcuttingAnim)
        delay(1)
        if (!locs.del(target, 10)) return
        mes("You cut a way through the vines.")
        if (inward && entrance) player.completeDiaryTask(DiaryTask.ChopBrimhavenEntranceVines)
    }
}
