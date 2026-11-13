package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.content.bosses.sporadic.HesporiRewards
import org.rsmod.content.bosses.sporadic.SporadicBossController
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HesporiLocs : LocReferences() {
    val entrance = find("kebos_farming_guild_hespori_entrance")
    val exit = find("hespori_cave_exit")
    val privateExit = find("hespori_cave_exit_instance")
    val patch = find("hespori_patch")
}

@Singleton
public class HesporiEncounter
@Inject
constructor(
    private val petRates:org.rsmod.content.skills.pets.SkillPetRates,
    private val regions: RegionRepository,
    private val registry: RegionRegistry,
    private val players: PlayerRegistry,
    private val bosses: SporadicBossController,
    private val locs: LocRepository,
    private val ground: ObjRepository,
    private val service: FarmingService,
) {
    private data class Fight(
        val region: Region,
        val boss: Npc,
        val patch: LocInfo,
        val crop: FarmingPatchState,
    )

    private val fights = WeakHashMap<Player, Fight>()
    private val template =
        RegionTemplate.create {
            copy(154, 1259, 0) {
                zoneWidth = 4
                zoneLength = 5
                regionZoneX = 1
                regionZoneZ = 1
            }
        }

    internal fun disturb(access: ProtectedAccess, state: FarmingPatchState) {
        if (state.status != FarmingCropStatus.Harvestable || state.cropKey != "hespori") return
        if (FarmingObjs.spade !in access.inv) {
            access.mes("You need a spade to harvest Hespori.")
            return
        }
        cleanupIfAway(access.player)
        if (access.player in fights) return
        val region =
            regions.add(template)
                ?: run {
                    access.mes("The cave is currently full.")
                    return
                }
        region.reclaimFromCycle = access.mapClock + 5
        val plot = locs.findExact(region.normal[PATCH_TILE], HesporiLocs.patch) ?: return
        val exit = locs.findExact(region.normal[EXIT_TILE], HesporiLocs.exit) ?: return
        locs.change(exit, HesporiLocs.privateExit, Int.MAX_VALUE)
        locs.del(plot, Int.MAX_VALUE)
        val boss = bosses.spawnHespori(plot.coords)
        fights[access.player] = Fight(region, boss, plot, state)
        access.telejump(region.normal[LANDING])
        access.mes("The Hespori awakens!")
    }

    internal fun defeated(player: Player, npc: Npc) {
        val fight = fights[player] ?: return
        if (fight.boss !== npc || fight.crop.status != FarmingCropStatus.Harvestable) return
        fight.crop.status = FarmingCropStatus.Dead
        fight.crop.healthChecked = true
        locs.add(fight.patch, Int.MAX_VALUE)
        service.syncHespori(player, fight.crop)
    }

    internal fun harvest(access: ProtectedAccess, plot: BoundLocInfo, state: FarmingPatchState) {
        if (state.status != FarmingCropStatus.Dead || !state.healthChecked) return
        if (FarmingObjs.spade !in access.inv) {
            access.mes("You need a spade to clear Hespori.")
            return
        }
        if (!service.clearDead(access, plot)) return
        access.anim(FarmingSeqs.harvest)
        access.farmingXp(12600.0)
        petRates.roll(access.player,stats.farming,20661,7000)
        for (item in HesporiRewards.harvest(access.random, access.statBase(stats.farming))) {
            if (access.invAdd(access.inv, item).failure)
                ground.add(item, access.coords, 300, access.player)
        }
        access.mes("You clear the Hespori and collect your harvest.")
    }

    internal fun leave(access: ProtectedAccess) {
        cleanup(access.player)
        access.telejump(OUTSIDE)
    }

    internal fun logout(player: Player) {
        val fight = fights[player] ?: return
        if (registry[player.coords] === fight.region) players.relocateOnLogout(player, OUTSIDE)
        cleanup(player)
    }

    internal fun cleanupIfAway(player: Player) {
        val fight = fights[player] ?: return
        if (registry[player.coords] !== fight.region) cleanup(player)
    }

    internal fun cleanup(player: Player) {
        fights.remove(player)?.let { bosses.removeHespori(it.boss) }
    }

    internal companion object {
        val PATCH_TILE = CoordGrid(1246, 10086)
        val EXIT_TILE = CoordGrid(1242, 10081)
        val LANDING = CoordGrid(1244, 10083)
        val OUTSIDE = CoordGrid(1234, 3727)
    }
}

public class HesporiCave @Inject constructor(private val encounter: HesporiEncounter) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(HesporiLocs.entrance) {
            if (!player.members || stat(stats.farming) < 65)
                mes("You need a Farming level of 65 to enter this cave.")
            else telejump(HesporiEncounter.LANDING)
        }
        onOpLoc1(HesporiLocs.exit) { encounter.leave(this) }
        onOpLoc1(HesporiLocs.privateExit) {
            if (choice2("Leave the Hespori cave?", true, "Stay here.", false)) encounter.leave(this)
        }
        onOpLoc2(HesporiLocs.privateExit) { encounter.leave(this) }
        onEvent<NpcKillEvents.Credited> { encounter.defeated(player, npc) }
        onEvent<PlayerDeathEvent.Completed> { encounter.cleanup(player) }
        onPlayerLogout { encounter.logout(player) }
    }
}
