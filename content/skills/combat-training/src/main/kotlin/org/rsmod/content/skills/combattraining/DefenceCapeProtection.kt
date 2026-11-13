package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.back
import org.rsmod.api.player.combatClearQueue
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.front
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ring
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object SurvivalSpots : SpotanimReferences() {
    val redemption = find("redemption_spotanim")
}

internal object DefenceCapeBits : VarBitReferences() {
    val enabled = find("rol_toggle")
    val ardougne = find("ardougne_rol_respawn_point")
}

public class DefenceCapeProtection
@Inject
constructor(
    private val realm: Realm,
    private val events: EventBus,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (cape in listOf(objs.defence_skillcape, objs.defence_skillcape_t)) {
            onOpHeld3(cape) { toggleProtection() }
            onOpWorn3(cape) { toggleProtection() }
            onOpHeld4(cape) { toggleDestination() }
            onOpWorn4(cape) { toggleDestination() }
        }
        onOpHeld3(objs.ring_of_life) { toggleDestination() }
        onPlayerHit { if (player.hitpoints > 0) protect(player, damage) }
    }

    public fun toggleLifeProtection(access: ProtectedAccess): Unit =
        with(access) { toggleProtection() }

    public fun toggleRespawnDestination(access: ProtectedAccess): Unit =
        with(access) { toggleDestination() }

    private fun ProtectedAccess.toggleProtection() {
        if (!player.members) return
        vars[DefenceCapeBits.enabled] = 1 - vars[DefenceCapeBits.enabled]
        mes(
            if (vars[DefenceCapeBits.enabled] == 1) "Your cape will protect you."
            else "Your cape's protection is disabled."
        )
    }

    private fun ProtectedAccess.toggleDestination() {
        if (
            !player.members || vars[DiaryRegion.Ardougne.tier(DiaryDifficulty.Medium).reward] == 0
        ) {
            mes("You must claim the medium Ardougne Diary reward first.")
            return
        }
        vars[DefenceCapeBits.ardougne] = 1 - vars[DefenceCapeBits.ardougne]
        mes(
            if (vars[DefenceCapeBits.ardougne] == 1)
                "Your ring of life effect will take you to Ardougne."
            else "Your ring of life effect will take you to your respawn point."
        )
    }

    private fun protect(player: Player, damage: Int) {
        val hp = player.hitpoints
        val max = player.baseHitpointsLvl
        if (
            damage > 0 &&
                player.members &&
                hp * 5 <= max &&
                player.front?.id == objs.phoenix_necklace.id &&
                !realm.config.hasWorldFlag(WorldFlag.Bounty)
        ) {
            player.front = null
            player.rebuildAppearance()
            player.combatClearQueue()
            player.statHeal(stats.hitpoints, max * 30 / 100, 0)
            player.mes("Your phoenix necklace restores your health.")
            return
        }
        if (
            hp * 10 < max && player.vars[varbits.redemption] != 0 && player.stat(stats.prayer) > 0
        ) {
            player.statSub(stats.prayer, 255, 0)
            player.disablePrayers()
            player.statHeal(stats.hitpoints, player.statBase(stats.prayer) / 4, 0)
            player.spotanim(SurvivalSpots.redemption)
            player.mes("You have run out of Prayer points.")
            return
        }
        if (
            damage <= 0 ||
                !player.members ||
                hp * 10 > max ||
                player.isTeleportBlocked() ||
                realm.config.hasWorldFlag(WorldFlag.Deadman)
        )
            return
        val wildBase = if (player.coords.z >= 9920) 9920 else 3520
        if (
            player.vars[varbits.inside_wilderness] != 0 && (player.coords.z - wildBase) / 8 + 1 > 30
        )
            return
        val cape =
            player.vars[DefenceCapeBits.enabled] != 0 &&
                player.back?.id in
                    listOf(
                        objs.defence_skillcape.id,
                        objs.defence_skillcape_t.id,
                        objs.max_cape_worn.id,
                    )
        if (!cape && player.ring?.id != objs.ring_of_life.id) return
        val destination =
            if (player.vars[DefenceCapeBits.ardougne] != 0) CoordGrid(2662, 3306)
            else realm.config.respawnCoord
        if (!collision.isZoneValid(destination)) return
        if (cape) player.mes("Your cape saves you.")
        else {
            player.ring = null
            player.rebuildAppearance()
            player.mes("Your Ring of Life saves you and is destroyed in the process.")
        }
        player.combatClearQueue()
        PathingEntityCommon.teleport(player, collision, destination)
        events.publish(PlayerTeleportEvent.Start(player, destination))
    }
}
