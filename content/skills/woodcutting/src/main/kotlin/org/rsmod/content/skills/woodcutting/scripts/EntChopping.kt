package org.rsmod.content.skills.woodcutting.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.EntTrunkOwnership
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.content.skills.woodcutting.WoodcuttingAxeEffects
import org.rsmod.content.skills.woodcutting.WoodcuttingBirdNests
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingReq
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object EntNpcs : NpcReferences() {
    val trunk = find("ent_trunk")
}

public object EntCaves : LocReferences() {
    val enter = find("wcguild_cave")
    val exit = find("wcguild_cave_exit")
}

/** Ent calculator14807078 (2024-11-24): axe/level table,16..90 and diary22..102 success curves. */
public object EntLogs {
    public val logs =
        listOf(
            objs.logs,
            objs.oak_logs,
            objs.willow_logs,
            objs.maple_logs,
            objs.yew_logs,
            objs.magic_logs,
        )
    private val table =
        arrayOf(
            intArrayOf(-1, -1, -1, -1, -1, -1),
            intArrayOf(0, 0, 0, 1, 1, 1),
            intArrayOf(0, 0, 1, 1, 1, 2),
            intArrayOf(0, 0, 1, 1, 2, 3),
            intArrayOf(0, 0, 1, 2, 3, 4),
            intArrayOf(0, 1, 2, 3, 4, 5),
        )

    public fun select(baseLevel: Int, roll: Int): ObjType? {
        require(baseLevel in 1..99)
        require(roll in 0..170)
        val column =
            when (baseLevel) {
                in 1..15 -> 0
                in 16..30 -> 1
                in 31..45 -> 2
                in 46..60 -> 3
                in 61..74 -> 4
                else -> 5
            }
        val row =
            when (roll) {
                in 0..10 -> 0
                in 11..20 -> 1
                in 21..35 -> 2
                in 36..50 -> 3
                in 51..75 -> 4
                else -> 5
            }
        return logs.getOrNull(table[row][column])
    }

    public fun wilderness(c: CoordGrid): Boolean =
        c.level == 0 && c.x in 2944..3391 && c.z in 3520..4095
}

internal object EntTrunkEditor : NpcEditor() {
    init {
        edit(EntNpcs.trunk) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

public class EntChopping
@Inject
constructor(
    private val ownership: EntTrunkOwnership,
    private val types: ObjTypeList,
    private val xp: XpModifiers,
    private val effects: WoodcuttingAxeEffects,
    private val nests: WoodcuttingBirdNests,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(EntNpcs.trunk) { chop(this, it.npc) }
        onOpLoc1(EntCaves.enter) {
            if (!player.members) return@onOpLoc1
            if (player.statBase(stats.woodcutting) < 60)
                mes("You need a Woodcutting level of 60 to enter the guild dungeon.")
            else telejump(CoordGrid(1596, 9900))
        }
        onOpLoc1(EntCaves.exit) { telejump(CoordGrid(1606, 3508)) }
    }

    public suspend fun chop(access: ProtectedAccess, trunk: Npc): Unit =
        with(access) {
            if (!player.members) return
            if (!ownership.canChop(player, trunk)) {
                mes("Only the player who defeated this Ent can chop its trunk.")
                return
            }
            while (ownership.canChop(player, trunk) && isWithinDistance(trunk, 1)) {
                val axe = Woodcutting.findAxe(player, types)
                if (axe == null) {
                    mes("You need an axe you can use to chop this trunk.")
                    return
                }
                val notes = EntLogs.logs.map { types.getValue(types[it].certlink) }
                if (inv.isFull() && notes.none { it in inv }) {
                    mes("You don't have enough inventory space.")
                    return
                }
                faceEntitySquare(trunk)
                anim(types[axe].axeWoodcuttingAnim)
                // Four-cycle action interval is a playable model; the exact Ent-specific cadence is
                // unpublished.
                gatheringDelay(4)
                if (
                    !ownership.canChop(player, trunk) ||
                        !isWithinDistance(trunk, 1) ||
                        Woodcutting.findAxe(player, types) != axe
                ) {
                    resetAnim()
                    return
                }
                val wild = EntLogs.wilderness(trunk.coords)
                val diary =
                    wild && vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Medium).complete] != 0
                if (
                    !statRandom(stats.woodcutting, if (diary) 22 else 16, if (diary) 102 else 90, 0)
                )
                    continue
                val base = player.statBase(stats.woodcutting).coerceIn(1, 99)
                val offset = types[axe].axeWoodcuttingReq.coerceIn(1, 71)
                val log = EntLogs.select(base, random.of(base + offset + 1)) ?: continue
                val noted = types.getValue(types[log].certlink)
                if (inv.isFull() && noted !in inv) {
                    mes("You don't have enough inventory space.")
                    resetAnim()
                    return
                }
                if (nests.tryDrop(this, log)) continue
                val tx =
                    player.invTransaction(inv) { add(select(inv), noted.id, if (wild) 2 else 1) }
                if (!tx.success) {
                    resetAnim()
                    return
                }
                effects.apply(this, axe, noted)
                statAdvance(stats.woodcutting, 25.0 * xp.get(player, stats.woodcutting))
                if (wild && log == objs.yew_logs)
                    player.completeDiaryTask(DiaryTask.ChopWildernessEntYew)
                mes("You chop some ${types[log].name.lowercase()} from the Ent trunk.")
            }
            resetAnim()
        }
}
