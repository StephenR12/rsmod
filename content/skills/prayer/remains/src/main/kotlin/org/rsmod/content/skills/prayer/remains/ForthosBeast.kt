package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.*
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ForthosCreatureStats : NpcEditor() {
    init {
        edit(CryptPeople.beast) {
            attackRange = 8
            hitpoints = 210
            attack = 50
            strength = 80
            defence = 50
            magic = 250
            ranged = 1
            param[params.attackrate] = 4
            param[params.defence_stab] = 50
            param[params.defence_slash] = 30
            param[params.defence_crush] = 100
            param[params.defence_magic] = 150
            param[params.defence_ranged] = 50
        }
    }
}

@Singleton
public class ForthosBeast
@Inject
constructor(
    private val regions: RegionRepository,
    private val registry: RegionRegistry,
    private val players: PlayerRegistry,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val ground: ObjRepository,
    private val accessLauncher: ProtectedAccessLauncher,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
) {
    private data class Fight(val region: Region, val npc: Npc, var drain: Int)

    private val fights = mutableMapOf<Player, Fight>()
    private val temple =
        RegionTemplate.create {
            copy(224, 1243, 0) {
                zoneWidth = 2
                zoneLength = 2
                regionZoneX = 1
                regionZoneZ = 1
            }
        }
    private val outside = CoordGrid(1798, 9953)

    internal fun start(access: ProtectedAccess) {
        val player = access.player
        if (!player.members || player.vars[CryptBits.olbertus] != 0) return
        if (fights[player]?.region === registry[player.coords] && player in fights) return
        leave(player)
        val region =
            regions.add(temple)
                ?: run {
                    access.mes("The temple is currently occupied.")
                    return
                }
        region.reclaimFromCycle = access.mapClock + 5
        val beast = Npc(types[CryptPeople.beast], region.normal[CoordGrid(1796, 9950)])
        npcs.add(beast, 6000)
        fights[player] = Fight(region, beast, access.mapClock + 10)
        VarPlayerIntMapSetter.set(player, CryptBits.faith, 1)
        access.telejump(region.normal[outside])
        beast.queueCombatRetaliate(player)
    }

    internal fun attack(access: StandardNpcAccess, target: Player) {
        val fight = fights[target] ?: return
        with(access) {
            if (fight.npc !== npc || registry[target.coords] !== fight.region) return
            if (mapClock >= fight.drain) {
                target.statSub(stats.prayer, 5, 0)
                fight.drain = mapClock + 10
            }
            if (actionDelay > mapClock || !isWithinDistance(target, 8)) return
            actionDelay = mapClock + 4
            val damage =
                if (accuracy.rollMagicAccuracy(npc, target, random)) random.of(0..30) else 0
            target.queueHit(npc, 1, HitType.Magic, damage)
        }
    }

    internal fun defeated(player: Player, npc: Npc) {
        val fight = fights[player] ?: return
        if (fight.npc !== npc) return
        VarPlayerIntMapSetter.set(player, CryptBits.olbertus, 1)
        ground.add(CryptItems.coin, outside, constants.lootdrop_duration, player)
        accessLauncher.launch(player) { telejump(outside) }
        leave(player)
    }

    internal fun logout(player: Player) {
        val fight = fights[player] ?: return
        if (registry[player.coords] === fight.region) players.relocateOnLogout(player, outside)
        leave(player)
    }

    internal fun leave(player: Player) {
        val fight = fights.remove(player) ?: return
        if (fight.npc.isSlotAssigned) npcs.del(fight.npc, Int.MAX_VALUE)
    }
}

public class ForthosBeastScript @Inject constructor(private val encounter: ForthosBeast) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onAiOpPlayer2(CryptPeople.beast) { encounter.attack(this, it.target) }
        onAiApPlayer2(CryptPeople.beast) { encounter.attack(this, it.target) }
        onEvent<NpcKillEvents.Credited> { encounter.defeated(player, npc) }
        onPlayerLogout { encounter.logout(player) }
        onEvent<PlayerDeathEvent.Completed> { encounter.leave(player) }
    }
}
