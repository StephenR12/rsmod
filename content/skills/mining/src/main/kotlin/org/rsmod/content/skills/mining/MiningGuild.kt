package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.hands
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object MiningGuildLocs : LocReferences() {
    val outer = find("mguild_door")
    val inner = listOf(find("mguild_door_inner1"), find("mguild_door_inner2"))
    val amethyst = listOf(find("amethystrock1"), find("amethystrock2"))
    val empty = find("amethystrock_empty")
}

public object MiningGuildObjs : ObjReferences() {
    val mineral = find("mguild_minerals")
    val basic = find("mguild_gloves")
    val superior = find("mguild_gloves_superior")
    val expert = find("mguild_gloves_expert")
    val clay = find("pack_softclay_guild")
    val gems = find("reward_gem_bag_guild")
}

public object MiningGuildNpcs : NpcReferences() {
    val belona = find("mguild_rewardseller")
}

public object MiningGuildVars : VarpReferences() {
    val disabled = find("mining_guild_minerals_disabled")
    val west = find("mining_guild_amethyst_permission")
}

internal object MiningGuildSaves : VarpBuilder() {
    init {
        build("mining_guild_minerals_disabled") {
            permanent = true
            transmitNever = true
        }
        build("mining_guild_amethyst_permission") {
            permanent = true
            transmitNever = true
        }
    }
}

internal object MiningGuildSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<MiningGuild>("mining-guild.toml")
    }
}

/**
 * Native Guild training/procurement. Stock presentation and shared vein-life ownership are models.
 */
@Singleton
public class MiningGuild
@Inject
constructor(private val random: GameRandom, private val ground: ObjRepository) : PluginScript() {
    private val remaining = mutableMapOf<CoordGrid, Int>()

    override fun ScriptContext.startup() {
        onOpLoc1(MiningGuildLocs.outer) { door(this, it.loc, false) }
        for (type in MiningGuildLocs.inner) onOpLoc1(type) { door(this, it.loc, true) }
        onOpNpc1(MiningGuildNpcs.belona) {
            when (
                choice4(
                    "Trade minerals.",
                    0,
                    "Combine mining gloves.",
                    1,
                    "Access the western amethyst cave.",
                    2,
                    "Toggle minerals.",
                    3,
                )
            ) {
                0 -> shop(this)
                1 -> combine(this)
                2 -> permitWest(this)
                3 -> toggle(this)
            }
        }
        onOpNpc3(MiningGuildNpcs.belona) { shop(this) }
        onOpNpc4(MiningGuildNpcs.belona) { toggle(this) }
    }

    public fun door(access: ProtectedAccess, loc: BoundLocInfo, membersDoor: Boolean): Boolean =
        with(access) {
            val entering = player.coords.z > loc.coords.z
            if (entering && player.stat(stats.mining) < 60) {
                mes("You need a Mining level of 60 to enter the Mining Guild.")
                return false
            }
            if (entering && membersDoor && !player.members) {
                mes("You need to be on a members' world to enter this part of the guild.")
                return false
            }
            telejump(loc.coords.translate(0, if (entering) -1 else 1))
            true
        }

    public fun invisibleBoost(player: Player, rock: CoordGrid): Int =
        if (player.members && membersArea(rock)) 7 else 0

    public fun respawn(rock: CoordGrid, product: ObjType, normal: Int): Int =
        if (membersArea(rock) && product.id != objs.amethyst.id) (normal / 2).coerceAtLeast(1)
        else normal

    public fun canMine(access: ProtectedAccess, rock: BoundLocInfo): Boolean =
        with(access) {
            if (MiningGuildLocs.amethyst.none { it.id == rock.id }) return true
            if (!player.members) {
                mes("You need to be on a members' world to mine amethyst.")
                return false
            }
            if (
                rock.coords.x < 3016 &&
                    rock.coords.x in 2992..3015 &&
                    rock.coords.z in 9696..9731 &&
                    vars[MiningGuildVars.west] == 0
            ) {
                mes(
                    "Speak to Belona after completing the Elite Falador Diary to mine in this cave."
                )
                return false
            }
            true
        }

    public fun retain(player: Player, rock: BoundLocInfo, product: ObjType): Boolean {
        val left =
            remaining[rock.coords]
                ?: ((if (product.id == objs.amethyst.id) random.of(2..3) else 1) +
                    extraGathers(player, product))
        if (left <= 1) {
            remaining.remove(rock.coords)
            return false
        }
        remaining[rock.coords] = left - 1
        return true
    }

    public fun extraGathers(player: Player, product: ObjType): Int {
        if (!player.members) return 0
        val tier =
            when (player.hands?.id) {
                MiningGuildObjs.basic.id -> 1
                MiningGuildObjs.superior.id -> 2
                MiningGuildObjs.expert.id -> 3
                else -> return 0
            }
        return when (product.id) {
            objs.copper_ore.id,
            objs.tin_ore.id,
            objs.iron_ore.id,
            objs.silver_ore.id,
            objs.coal.id,
            objs.gold_ore.id -> tier
            objs.mithril_ore.id,
            objs.adamantite_ore.id -> (tier - 1).coerceAtLeast(0)
            objs.runite_ore.id,
            objs.amethyst.id -> if (tier == 3) 1 else 0
            in QuarryObjs.sandstone.map { it.id } -> if (tier == 3) 1 else 0
            else -> 0
        }
    }

    public fun mineral(access: ProtectedAccess, rock: CoordGrid, product: ObjType): Boolean =
        with(access) {
            if (!player.members || !membersArea(rock) || vars[MiningGuildVars.disabled] != 0)
                return false
            val denominator =
                when (product.id) {
                    objs.iron_ore.id -> 100
                    objs.coal.id -> 60
                    objs.mithril_ore.id -> 40
                    objs.adamantite_ore.id -> 30
                    objs.runite_ore.id,
                    objs.amethyst.id -> 20
                    else -> return false
                }
            if (random.of(denominator) != 0) return false
            if (!player.invAdd(inv, MiningGuildObjs.mineral).success) {
                if (
                    !ground.add(
                        Obj.fromOwner(player, player.coords, InvObj(MiningGuildObjs.mineral)),
                        constants.lootdrop_duration,
                    )
                )
                    return false
            }
            mes("You find an unidentified mineral.")
            true
        }

    public fun toggle(access: ProtectedAccess) =
        with(access) {
            if (!player.members) return
            vars[MiningGuildVars.disabled] = if (vars[MiningGuildVars.disabled] == 0) 1 else 0
            mes(
                if (vars[MiningGuildVars.disabled] == 0)
                    "You will now receive unidentified minerals."
                else "You will no longer receive unidentified minerals."
            )
        }

    public fun permitWest(access: ProtectedAccess): Boolean =
        with(access) {
            if (
                !player.members ||
                    vars[DiaryRegion.Falador.tier(DiaryDifficulty.Elite).complete] == 0
            ) {
                mes("You must complete the Elite Falador Diary first.")
                return false
            }
            vars[MiningGuildVars.west] = 1
            mes("Belona gives you permission to use the western amethyst cave.")
            true
        }

    public suspend fun shop(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            val selection =
                choice5(
                    "Mining gloves: 40 minerals.",
                    0,
                    "Superior mining gloves: 100 minerals.",
                    1,
                    "Soft clay pack: 10 minerals.",
                    2,
                    "Bag full of gems: 20 minerals.",
                    3,
                    "Sell an item back.",
                    4,
                )
            if (selection in 0..3) buy(this, selection)
            else if (selection == 4) {
                val index =
                    choice5(
                        "Mining gloves.",
                        0,
                        "Superior mining gloves.",
                        1,
                        "Guild soft clay pack.",
                        2,
                        "Guild gem bag.",
                        3,
                        "Leave.",
                        -1,
                    )
                if (index >= 0) sell(this, index)
            }
        }

    public fun buy(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (!player.members || index !in goods.indices) return false
            val result =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, MiningGuildObjs.mineral.id, costs[index])
                    add(i, goods[index].id, 1)
                }
            if (!result.success)
                mes("You need ${costs[index]} unidentified minerals and room for the item.")
            result.success
        }

    public fun sell(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (!player.members || index !in goods.indices) return false
            player
                .invTransaction(inv) {
                    val i = select(inv)
                    delete(i, goods[index].id, 1)
                    add(i, MiningGuildObjs.mineral.id, costs[index] * 8 / 10)
                }
                .success
        }

    public fun combine(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members) return false
            val result =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, MiningGuildObjs.basic.id, 1)
                    delete(i, MiningGuildObjs.superior.id, 1)
                    delete(i, MiningGuildObjs.mineral.id, 40)
                    add(i, MiningGuildObjs.expert.id, 1)
                }
            if (!result.success)
                mes("Bring both pairs of mining gloves and 40 unidentified minerals.")
            result.success
        }

    public val goods: List<ObjType> by lazy {
        listOf(
            MiningGuildObjs.basic,
            MiningGuildObjs.superior,
            MiningGuildObjs.clay,
            MiningGuildObjs.gems,
        )
    }
    public val costs: List<Int> = listOf(40, 100, 10, 20)

    public companion object {
        /** Both native inner doors are north of the member resource chambers. */
        public fun membersArea(c: CoordGrid): Boolean =
            c.level == 0 && c.x in 2992..3063 && c.z in 9696..9731
    }
}

internal object MiningGuildGloveRequirements : ObjEditor() {
    init {
        for ((glove, level) in
            listOf(
                MiningGuildObjs.basic to 20,
                MiningGuildObjs.superior to 55,
                MiningGuildObjs.expert to 70,
            )) edit(glove) {
            param[params.statreq1_skill] = stats.mining
            param[params.statreq1_level] = level
        }
    }
}
