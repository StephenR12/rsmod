package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.isTeleportBlocked
import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.minigames.completedBarbarianAssaultTutorial
import org.rsmod.api.player.minigames.canEnterNightmareZone
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.api.utils.time.epochMinute
import org.rsmod.content.skills.magic.spell.utility.standard.HomeTeleport
import org.rsmod.content.skills.magic.spell.utility.standard.PlayerOwnedHousePresenceRepository
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GroupInterfaces : InterfaceReferences() {
    val grouping = find("grouping")
    val tabs = listOf("chatchannel_current","clans_sidepanel","clans_guest_sidepanel","grouping").map { find(it) }
    val tithe = find("hosidius_strip_farming")
    val activeGames = listOf("pest_lander_overlay","pest_status_overlay","tzhaar_fightpit","nzone_game","trawler_overlay",
        "castlewars_status_overlay_saradomin","castlewars_status_overlay_zamorak","barbassault_over_att","barbassault_over_col",
        "barbassault_over_def","barbassault_over_heal","tob_hud","toa_hud","hosidius_strip_farming").map { find(it) }
}
internal object GroupButtons : ComponentReferences() {
    val tabs = (0..3).map { find("side_channels:tab_$it") }
    val contents = find("side_channels:contents")
    // Client script 435 creates the Teleport button as a dynamic child indexed by enum 848.
    val travel = find("grouping:teleport")
}
internal object GroupBits : VarBitReferences() {
    val selected = find("side_channels_tab_selected")
    val displayed = find("side_channels_tab")
    val giantDwarf = find("giantdwarf_quest")
    val rift = find("tote")
    val ratcatcher = find("ratcatch_var")
    val points = find("hosidius_tithe_rewardpoints")
    val saveTime = find("grouping_teleport_save_time")
}
internal object GroupVars : VarpReferences() {
    val last = find("grouping_last_teleport_minute")
    val visited = find("grouping_visited_activities")
    val shades = find("morttonquest")
    val cabin = find("fever_quest")
}
internal object GroupVarBuilds : VarpBuilder() {
    init {
        build("grouping_last_teleport_minute") { transmitNever = true }
        build("grouping_visited_activities") { transmitNever = true }
    }
}
internal object GroupNpcs : NpcReferences() { val guardian = find("magictraining_guard_entrance") }
internal object GroupItems : ObjReferences() { val hats = listOf("dull","energised","full").map { find("magictraining_proghat_$it") } }

public class GroupingTravel @Inject constructor(
    private val events: EventBus,
    private val launcher: ProtectedAccessLauncher,
    private val home: HomeTeleport,
    private val houses: PlayerOwnedHousePresenceRepository,
    private val realm: Realm
) : PluginScript() {
    // Native activity IDs in enum 848. Clan Wars, Castle Wars and LMS allow F2P travel.
    private val locations = mapOf(1 to CoordGrid(2521,3570),2 to CoordGrid(2930,10193),3 to CoordGrid(2899,3553),
        4 to CoordGrid(2442,3093),5 to CoordGrid(3150,3635),7 to CoordGrid(2658,3157),8 to CoordGrid(3361,3149),
        10 to CoordGrid(3104,9573),11 to CoordGrid(3150,3635),12 to CoordGrid(3363,3295),13 to CoordGrid(2609,3114),
        14 to CoordGrid(2658,2663),18 to CoordGrid(3506,3304),21 to CoordGrid(2018,5929),23 to CoordGrid(1792,3501),
        25 to CoordGrid(3801,3015),26 to CoordGrid(2399,5177))
    override fun ScriptContext.startup() {
        onPlayerLogin {
            val remaining = (20 - (epochMinute() - player.vars[GroupVars.last])).coerceIn(0,20)
            VarPlayerIntMapSetter.set(player,GroupBits.saveTime,remaining)
        }
        onIfOpen(interfaces.side_channels) {
            for (button in GroupButtons.tabs) player.ifSetEvents(button,-1..-1,IfEvent.Op1)
            player.openTab(player.vars[GroupBits.selected].coerceIn(0,3))
        }
        for ((index,button) in GroupButtons.tabs.withIndex()) onIfOverlayButton(button) {
            if (op == IfButtonOp.Op1) player.openTab(index)
        }
        onIfOpen(GroupInterfaces.grouping) { player.ifSetEvents(GroupButtons.travel,0..27,IfEvent.Op1) }
        onIfOverlayButton(GroupButtons.travel) {
            if (op == IfButtonOp.Op1) {
                val id = comsub
                launcher.launch(player) { depart(id) }
            }
        }
        onIfOpen(GroupInterfaces.tithe) { player.visited(TITHE) }
        onOpNpc1(GroupNpcs.guardian) {
            if (!player.members) return@onOpNpc1
            player.visited(MAGE_ARENA)
            if (GroupItems.hats.none { it in inv || it in worn || it in bank }) invAdd(inv,GroupItems.hats.first())
            mes("The Entrance Guardian introduces you to the four training arenas.")
        }
    }
    private fun Player.openTab(index:Int) {
        VarPlayerIntMapSetter.set(this,GroupBits.selected,index)
        VarPlayerIntMapSetter.set(this,GroupBits.displayed,index)
        ifOpenOverlay(GroupInterfaces.tabs[index],GroupButtons.contents,events)
    }
    private fun Player.visited(bit:Int) {
        VarPlayerIntMapSetter.set(this,GroupVars.visited,vars[GroupVars.visited] or bit)
    }
    private fun ProtectedAccess.eligible(id:Int):Boolean {
        if (!player.members && id !in listOf(4,5,11)) return false
        return when (id) {
            1 -> player.completedBarbarianAssaultTutorial()
            2 -> vars[GroupBits.giantDwarf] > 0
            7 -> statBase(stats.fishing) >= 15
            8 -> statBase(stats.smithing) >= 15
            10 -> vars[GroupBits.rift] >= 130
            12 -> vars[GroupVars.visited] and MAGE_ARENA != 0 || GroupItems.hats.any { it in worn || it in bank || it in inv }
            13 -> player.canEnterNightmareZone()
            14 -> player.combatLevel >= 40
            16 -> vars[GroupBits.ratcatcher] >= 127
            18 -> vars[GroupVars.shades] >= 85
            23 -> statBase(stats.farming) >= 34 && (vars[GroupVars.visited] and TITHE != 0 || vars[GroupBits.points] > 0)
            25 -> vars[GroupVars.cabin] >= 140 && statBase(stats.cooking) >= 40
            else -> true
        }
    }
    private suspend fun ProtectedAccess.depart(id:Int) {
        if (id !in locations && id != 16) { mes("No teleport is available for that activity."); return }
        if (!eligible(id)) { mes("You do not meet the requirements to teleport to that activity."); return }
        val destination = if (id == 16) {
            val index = menu("Which Rat Pits?",true,listOf("Ardougne","Varrock","Keldagrim","Port Sarim","Cancel"))
            listOf(CoordGrid(2561,3320),CoordGrid(3267,3400),CoordGrid(2914,10193),CoordGrid(3018,3232)).getOrNull(index) ?: return
        } else locations.getValue(id)
        val elapsed = epochMinute() - vars[GroupVars.last]
        if (elapsed < 20) { mes("You must wait ${(20-elapsed).coerceIn(1,20)} minutes before using a grouping teleport."); return }
        if (realm.config.hasWorldFlag(WorldFlag.Pvp) || vars[varbits.inside_wilderness] != 0 || player.isTeleportBlocked() ||
            player.preventLogoutUntil >= mapClock || houses.isInside(player) || GroupInterfaces.activeGames.any { it in player.ui } ||
            coords.x in 3312..3391 && coords.z in 3200..3280) {
            mes("You can't use a grouping teleport here.")
            return
        }
        home.channel(this,destination) {
            vars[GroupVars.last] = epochMinute()
            vars[GroupBits.saveTime] = 20
            if (id == 14) player.completeDiaryTask(DiaryTask.GroupTeleportPestControl)
        }
    }
    private companion object {
        const val MAGE_ARENA = 1
        const val TITHE = 2
    }
}
