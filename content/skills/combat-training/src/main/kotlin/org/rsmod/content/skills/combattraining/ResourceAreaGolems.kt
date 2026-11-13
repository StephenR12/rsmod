package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.recordDiaryRuniteGolemOre
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.*
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeFastIntervalChance
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeInterval
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object GolemNpcs : NpcReferences() {
    val golem = find("wilderness_rune_golem")
    val rock = find("wilderness_golem_rune_rock")
    val empty = find("golem_rune_rock_no_ore")
}

internal object GolemLocs : LocReferences() {
    val ore = find("runiterock1")
    val gate = find("wilderness_resource_gate")
}

internal object GolemStats : NpcEditor() {
    init {
        edit(GolemNpcs.golem) {
            respawnRate = 500
            hitpoints = 120
            attack = 140
            strength = 150
            defence = 165
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
            param[params.elemental_weakness_type] = constants.elemental_weakness_earth
            param[params.elemental_weakness_percent] = 60
        }
    }
}

class ResourceAreaGolems
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val objTypes: ObjTypeList,
    private val locTypes: LocTypeList,
    private val levels: InvisibleLevels,
    private val xp: XpModifiers,
    private val players: PlayerRepository,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    private val killedBy = mutableMapOf<Npc, Long?>()

    override fun ScriptContext.startup() {
        onModifyNpcHit(GolemNpcs.golem) {
            if (hit.damage > 0) hit.damage = (hit.damage - 4).coerceAtLeast(1)
        }
        onEvent<NpcKillEvents.Credited> {
            if (npc.id == GolemNpcs.golem.id && inside(npc.spawnCoords)) {
                val head = Npc(npcTypes[GolemNpcs.rock], npc.coords)
                npcRepo.add(head, 100)
                killedBy[head] = player.uuid
            }
        }
        onEvent<NpcStateEvents.Delete> { killedBy.remove(npc) }
        onOpNpc1(GolemNpcs.rock) { mine(it.npc) }
        onOpLoc1(GolemLocs.gate) {
            if (!player.members) return@onOpLoc1
            if (player.coords.z <= 3944) {
                telejump(outside)
                return@onOpLoc1
            }
            val cost =
                when {
                    player.vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Elite).complete] != 0 ->
                        0
                    player.vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Hard).complete] != 0 ->
                        3750
                    player.vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Medium).complete] !=
                        0 -> 6000
                    else -> 7500
                }
            if (cost > 0 && !choice2("Pay $cost coins to enter.", true, "Never mind.", false))
                return@onOpLoc1
            if (cost > 0 && invDel(inv, objs.coins, cost).failure)
                mes("You do not have enough coins to enter the Resource Area.")
            else telejump(CoordGrid(3184, 3943))
        }
        onOpLoc2(GolemLocs.gate) {
            val any =
                players.findAll(ZoneKey.from(CoordGrid(3185, 3935)), 2).any {
                    inside(it.coords) && it != player
                }
            mes(
                if (any) "You see someone inside the resource area."
                else "The resource area looks empty."
            )
        }
        onPlayerLogin {
            if (inside(player.coords)) PathingEntityCommon.telejump(player, collision, outside)
        }
    }

    private suspend fun ProtectedAccess.mine(rock: Npc) {
        if (!player.members || rock !in killedBy || rock.visType.id != GolemNpcs.rock.id) return
        if (player.stat(stats.mining) < 85) {
            mes("You need a Mining level of 85 to mine runite.")
            return
        }
        val pick = Mining.findPickaxe(player, objTypes)?.let { objTypes[it] }
        if (pick == null) {
            mes("You need a pickaxe that you can use to mine this rock.")
            return
        }
        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more ore.")
            return
        }
        anim(pick.pickaxeAnim)
        val fast = pick.pickaxeFastIntervalChance
        delay(
            if (fast > 0 && random.randomBoolean(fast)) pick.pickaxeInterval - 1
            else pick.pickaxeInterval
        )
        if (rock !in killedBy || rock.visType.id != GolemNpcs.rock.id || !isWithinDistance(rock, 1))
            return
        val bounds = Mining.successBounds(player, locTypes[GolemLocs.ore])
        if (!statRandom(stats.mining, bounds.first, bounds.last, levels)) {
            opNpc1(rock)
            return
        }
        if (invAdd(inv, objs.runite_ore).failure) {
            mes("Your inventory is too full to hold any more ore.")
            return
        }
        val own = killedBy.remove(rock) == player.uuid
        rock.transmog(npcTypes[GolemNpcs.empty], Int.MAX_VALUE)
        resetAnim()
        statAdvance(stats.mining, 125.0 * xp.get(player, stats.mining))
        if (own) player.recordDiaryRuniteGolemOre(rock.coords)
        mes("You manage to mine some runite ore.")
    }

    private fun inside(c: CoordGrid): Boolean =
        c.level == 0 && c.x in 3175..3196 && c.z in 3925..3944

    private val outside = CoordGrid(3184, 3945)
}
