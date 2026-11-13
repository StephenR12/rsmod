package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.EntranaEquipment
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.stat.StatType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AbyssBits : VarBitReferences() {
    val layout = find("rcu_abyssal_generator")
    val lastBlood = find("abyss_blood_rift_last_used")
}

internal object AbyssVars : VarpReferences() {
    val miniquest = find("abyssal_miniquest")
    val lostCity = find("zanaris")
    val trollStronghold = find("troll_quest")
    val skull = find("abyss_skull_ticks")
    val kourend = find("abyss_kourend_rifts")
}

internal object AbyssSave : VarpBuilder() {
    init {
        build("abyss_skull_ticks") { transmitNever = true }
        build("abyss_kourend_rifts") { transmitNever = true }
    }
}

internal object AbyssTimers : TimerReferences() {
    val skull = find("abyss_skull")
}

internal object AbyssPeople : NpcReferences() {
    val mage = find("rcu_zammy_mage1b")
}

internal object AbyssItems : ObjReferences() {
    val bracelets = (1..5).map { find("jewl_runerunning_bracelet_$it") }
}

internal object AbyssLocs : LocReferences() {
    val passages = listOf(find("rcu_blankmodel"), find("rcu_outer_entrance"))
    val obstacles =
        mapOf(
            find("rcu_abyssal_barrier_teeth1") to stats.mining,
            find("rcu_abyssal_barrier_tendrils1") to stats.woodcutting,
            find("rcu_abyssal_barrier_boil1") to stats.firemaking,
            find("rcu_abyssal_barrier_eyes1") to stats.thieving,
            find("rcu_abyssal_barrier_agility") to stats.agility,
        )
    val rifts =
        RuneTemple.entries
            .filter { it != RuneTemple.Wrath && it != RuneTemple.Blood }
            .associateWith { find("abyss_exit_to_${it.key}") }
    val blood =
        listOf(find("abyss_exit_to_blood_child_true"), find("abyss_exit_to_blood_child_kourend"))
    val soul = find("abyss_exit_to_soul")
}

private data class AbyssPassage(
    val obstacle: CoordGrid,
    val openWall: CoordGrid,
    val entrance: CoordGrid,
    val inside: CoordGrid,
)

private fun passage(x: Int, z: Int, wx: Int, wz: Int, ex: Int, ez: Int, ix: Int, iz: Int) =
    AbyssPassage(CoordGrid(x, z), CoordGrid(wx, wz), CoordGrid(ex, ez), CoordGrid(ix, iz))

public class AbyssTravel @Inject constructor(private val objects: ObjTypeList) : PluginScript() {
    // Cache order matches rcu_outer_multi1..12. The clear passage is opposite the blockage.
    private val passages =
        listOf(
            passage(3041, 4811, 3042, 4813, 3042, 4810, 3042, 4825),
            passage(3026, 4813, 3027, 4815, 3027, 4812, 3034, 4827),
            passage(3018, 4821, 3020, 4822, 3017, 4822, 3033, 4829),
            passage(3018, 4833, 3020, 4834, 3017, 4834, 3030, 4834),
            passage(3021, 4842, 3023, 4843, 3020, 4843, 3032, 4837),
            passage(3028, 4849, 3029, 4848, 3029, 4852, 3036, 4839),
            passage(3038, 4853, 3039, 4852, 3039, 4856, 3039, 4839),
            passage(3049, 4849, 3050, 4848, 3050, 4852, 3043, 4839),
            passage(3058, 4839, 3057, 4840, 3061, 4840, 3046, 4837),
            passage(3060, 4830, 3059, 4831, 3063, 4831, 3048, 4833),
            passage(3057, 4821, 3056, 4822, 3060, 4822, 3046, 4828),
            passage(3049, 4813, 3050, 4815, 3050, 4812, 3043, 4825),
        )

    override fun ScriptContext.startup() {
        onOpNpc4(AbyssPeople.mage) { enter() }
        onOpNpc1(AbyssPeople.mage) {
            if (choice2("Teleport me to the Abyss.", true, "Leave.", false)) enter()
        }
        for (loc in AbyssLocs.passages) onOpLoc1(loc) { cross(it.loc, null) }
        for ((loc, stat) in AbyssLocs.obstacles) onOpLoc1(loc) { cross(it.loc, stat) }
        for ((temple, rift) in AbyssLocs.rifts) onOpLoc1(rift) { through(temple) }
        for ((index, rift) in AbyssLocs.blood.withIndex()) {
            onOpLoc1(rift) { blood(kourend = index == 1) }
            onOpLoc2(rift) { blood(kourend = index == 0) }
        }
        onOpLoc1(AbyssLocs.soul) {
            if (vars[AbyssVars.kourend] and 8 != 0) arrive(CoordGrid(1814, 3852))
            else mes("The soul rift is sealed.")
        }
        for (essence in
            listOf(RunecraftObjs.darkEssenceBlock, RunecraftObjs.darkEssenceFragments)) {
            for (rift in AbyssLocs.blood) onOpLocU(rift, essence) { attune(blood = true) }
            onOpLocU(AbyssLocs.soul, essence) { attune(blood = false) }
        }
        onPlayerTimer(AbyssTimers.skull) {
            val ticks = vars[AbyssVars.skull]
            if (ticks <= 0 || player.skullIcon != constants.skullicon_default) {
                vars[AbyssVars.skull] = 0
                clearTimer(AbyssTimers.skull)
            } else if (coords.x shr 6 != 47 || coords.z shr 6 != 75) {
                vars[AbyssVars.skull] = ticks - 1
                if (ticks == 1) {
                    player.skullIcon = null
                    rebuildAppearance()
                    clearTimer(AbyssTimers.skull)
                }
            }
        }
        onPlayerLogin {
            if (player.vars[AbyssVars.skull] > 0) {
                player.skullIcon = constants.skullicon_default
                player.rebuildAppearance()
                player.timer(AbyssTimers.skull, 1)
            }
        }
        onEvent<PlayerDeathEvent.Completed> {
            VarPlayerIntMapSetter.set(player, AbyssVars.skull, 0)
        }
    }

    private fun ProtectedAccess.enter() {
        if (!player.members || vars[AbyssVars.miniquest] < 4) return
        if (player.isTeleportBlocked()) {
            mes("A magical force stops you from teleporting.")
            return
        }
        val layout = random.of(passages.size)
        vars[AbyssBits.layout] = layout
        statSub(stats.prayer, stat(stats.prayer), 0)
        val slot = Wearpos.Hands.slot
        val charges = AbyssItems.bracelets.indexOfFirst { it.id == worn[slot]?.id } + 1
        if (charges > 0) {
            if (charges == 1) {
                invDel(worn, AbyssItems.bracelets.first(), slot = slot)
                mes("Your abyssal bracelet crumbles to dust.")
            } else {
                invReplaceSlot(worn, slot, 1, AbyssItems.bracelets[charges - 2])
            }
            rebuildAppearance()
        } else if (player.skullIcon == null || vars[AbyssVars.skull] > 0) {
            player.skullIcon = constants.skullicon_default
            vars[AbyssVars.skull] = 1000
            timer(AbyssTimers.skull, 1)
            rebuildAppearance()
        }
        arrive(passages[layout].entrance)
        player.completeDiaryTask(DiaryTask.EnterAbyss)
    }

    private suspend fun ProtectedAccess.cross(loc: BoundLocInfo, stat: StatType?) {
        if (!player.members) return
        val index = passages.indexOfFirst { loc.coords == it.obstacle || loc.coords == it.openWall }
        if (index < 0) return
        if (stat == null && index != (vars[AbyssBits.layout] + 6) % passages.size) {
            mes("The entrance is blocked.")
            return
        }
        if (stat != null) {
            val equipped = worn[Wearpos.RightHand.slot]
            val tool =
                when (stat) {
                    stats.mining -> content.mining_pickaxe
                    stats.woodcutting -> content.woodcutting_axe
                    else -> null
                }
            if (
                tool != null &&
                    !ocIsContentType(equipped, tool) &&
                    inv.objs.none { ocIsContentType(it, tool) }
            ) {
                mes(if (stat == stats.mining) "You need a pickaxe." else "You need an axe.")
                return
            }
            if (stat == stats.firemaking && objs.tinderbox !in inv) {
                mes("You need a tinderbox.")
                return
            }
            delay(2)
            if (random.of(100) >= stat(stat) + 1) {
                mes("You fail to pass the obstacle.")
                return
            }
            statAdvance(stat, 25.0)
        }
        teleport(passages[index].inside)
    }

    private fun ProtectedAccess.through(temple: RuneTemple) {
        if (!player.members) return
        val requirement = temple.recipe.quest
        if (
            requirement != null && vars[requirement.varbit] < requirement.completionValue ||
                temple == RuneTemple.Cosmic && vars[AbyssVars.lostCity] < 6 ||
                temple == RuneTemple.Law && vars[AbyssVars.trollStronghold] < 50
        ) {
            mes("The rift does not respond to you.")
            return
        }
        if (
            temple == RuneTemple.Law &&
                (inv.objs + worn.objs).filterNotNull().any {
                    EntranaEquipment.prohibited(objects[it])
                }
        ) {
            mes("You cannot take that equipment to Entrana.")
            return
        }
        arrive(temple.inside)
    }

    private fun ProtectedAccess.blood(kourend: Boolean) {
        if (!player.members) return
        if (kourend && vars[AbyssVars.kourend] and 4 == 0) {
            mes("You must attune this rift with dark essence first.")
            return
        }
        if (kourend) arrive(CoordGrid(1716, 3827))
        else {
            val quest = requireNotNull(RuneTemple.Blood.recipe.quest)
            if (vars[quest.varbit] < quest.completionValue) {
                mes("The blood rift is sealed.")
                return
            }
            arrive(RuneTemple.Blood.inside)
        }
        vars[AbyssBits.lastBlood] = if (kourend) 1 else 0
    }

    private fun ProtectedAccess.attune(blood: Boolean) {
        val state = vars[AbyssVars.kourend]
        val crafted = if (blood) 1 else 2
        if (!player.members || state and crafted == 0) {
            mes("First craft runes at the corresponding altar in Great Kourend.")
            return
        }
        vars[AbyssVars.kourend] = state or (crafted shl 2)
        mes("The rift responds to the dark essence.")
    }

    private fun ProtectedAccess.arrive(tile: CoordGrid) {
        teleport(tile)
        publish(PlayerTeleportEvent.Start(player, tile))
    }
}
