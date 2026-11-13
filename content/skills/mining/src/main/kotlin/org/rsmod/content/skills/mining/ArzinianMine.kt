package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.hat
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ring
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.mining.configs.MiningParams
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

public object ArzinianLocs : LocReferences() {
    val veins = listOf("dwarf_gold_mine", "dwarf_gold_mine_r", "dwarf_gold_mine_l").map(::find)
    val depleted = find("dwarf_gold_depleted")
}

public object ArzinianNpcs : NpcReferences() {
    val dondakan = listOf(find("dwarfrock_dondakan"), find("dwarfrock_dondakan_noaxe"))
    val hauler = find("dwarfrock_gold_boatman")
    val ferries = listOf(find("dwarfrock_ferryman1"), find("dwarfrock_ferryman2"))
}

public object ArzinianObjs : ObjReferences() {
    val helmet = find("dwarf_goldrock_helmet")
    val charos = find("ring_of_charos_unlocked")
}

public object ArzinianBits : VarBitReferences() {
    val quest = find("dwarfrock_quest")
}

public object ArzinianTimers : TimerReferences() {
    val helmet = find("arzinian_helmet_watch")
}

internal object ArzinianVeins : LocEditor() {
    init {
        for (vein in ArzinianLocs.veins) edit(vein) {
            contentGroup = content.ore
            param[params.levelrequire] = 40
            param[params.skill_xp] = PlayerStatMap.toFineXP(65.0).toInt()
            param[params.skill_productitem] = objs.gold_ore
            param[params.next_loc_stage] = ArzinianLocs.depleted
            param[params.respawn_time] = 100
            param[MiningParams.successLow] = 7
            param[MiningParams.successHigh] = 75
        }
    }
}

internal object ArzinianPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ArzinianMine>("arzinian-mine.toml")
    }
}

/** Repeatable access and native gold mining; the quest encounter/progression remains separate. */
public class ArzinianMine
@Inject
constructor(
    private val collision: CollisionFlagMap,
    private val types: org.rsmod.game.type.obj.ObjTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in ArzinianNpcs.dondakan) onOpNpc1(npc) {
            if (choice2("Fire me into the mine.", true, "Leave.", false)) enter(this)
        }
        for ((index, npc) in ArzinianNpcs.ferries.withIndex()) {
            onOpNpc1(npc) {
                if (choice2("Cross the river.", true, "Leave.", false)) ferry(this, index)
            }
            onOpNpc3(npc) { ferry(this, index) }
        }
        onOpNpc1(ArzinianNpcs.hauler) {
            if (choice2("Bank my gold ore for a share of the ore.", true, "Leave.", false))
                bank(this)
        }
        onEvent<Mining.MinedOre> {
            if (product == objs.gold_ore && inside(rock.coords) && permitted(player))
                player.completeDiaryTask(DiaryTask.MineArzinianGold)
        }
        onPlayerSoftTimer(ArzinianTimers.helmet) { watch(player) }
        onPlayerLogin { if (inside(player.coords)) leave(player) }
        onPlayerLogout { if (inside(player.coords)) leave(player) }
    }

    public fun enter(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || vars[ArzinianBits.quest] < 90) {
                mes("Dondakan is not ready to fire you into the rock yet.")
                return false
            }
            if (player.hat?.id != ArzinianObjs.helmet.id) {
                mes("You must wear your gold helmet to enter the mine.")
                return false
            }
            telejump(ENTRY)
            player.softTimer(ArzinianTimers.helmet, 1)
            true
        }

    public fun watch(player: Player) {
        if (!inside(player.coords)) player.clearSoftTimer(ArzinianTimers.helmet)
        else if (!permitted(player)) leave(player)
    }

    public fun leave(player: Player) {
        player.clearSoftTimer(ArzinianTimers.helmet)
        player.cancelActiveCoroutine()
        player.resetAnim()
        PathingEntityCommon.telejump(player, collision, EXIT)
    }

    public fun bank(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || vars[ArzinianBits.quest] < 110) {
                mes("Complete Between a Rock... before using this service.")
                return false
            }
            val count = inv.count(types[objs.gold_ore])
            if (count < 2) {
                mes("You need at least two gold ores for the boatman to bank any.")
                return false
            }
            // NPC14923820 specifies floor(n/divisor)+1; the mine overview has a conflicting ceiling
            // rule.
            val fee = count / (if (player.ring?.id == ArzinianObjs.charos.id) 10 else 5) + 1
            val destination = inv(invs.bank)
            val tx =
                player.invTransaction(inv, destination) {
                    delete(select(inv), objs.gold_ore.id, count)
                    add(select(destination), objs.gold_ore.id, count - fee)
                }
            if (!tx.success) mes("There is not enough room in your bank.")
            tx.success
        }

    public fun ferry(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (!player.members || index !in 0..1) return false
            val waived =
                player.ring?.id == ArzinianObjs.charos.id ||
                    (index == 1 && inv.count(types[objs.coins]) < 2)
            if (
                !waived &&
                    !player.invTransaction(inv) { delete(select(inv), objs.coins.id, 2) }.success
            ) {
                mes("The crossing costs two coins.")
                return false
            }
            telejump(if (index == 0) CoordGrid(2854, 10144) else CoordGrid(2838, 10128))
            true
        }

    public companion object {
        public val ENTRY: CoordGrid = CoordGrid(2368, 4960)
        public val EXIT: CoordGrid = CoordGrid(2826, 10164)

        public fun inside(c: CoordGrid): Boolean =
            c.level == 0 && c.x in 2306..2440 && c.z in 4928..4990

        public fun permitted(p: Player): Boolean =
            p.members && p.vars[ArzinianBits.quest] >= 90 && p.hat?.id == ArzinianObjs.helmet.id
    }
}
