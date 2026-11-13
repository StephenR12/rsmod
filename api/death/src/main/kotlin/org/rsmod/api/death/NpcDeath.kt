package org.rsmod.api.death

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varns
import org.rsmod.api.config.refs.varps
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.vars.typePlayerUidVarn
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.vars.typeNpcUidVarp
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.map.CoordGrid

@Singleton
public class NpcDeath
@Inject
constructor(
    private val events: EventBus,
    private val npcRepo: NpcRepository,
    private val seqTypes: SeqTypeList,
    private val players: PlayerList,
    private val objRepo: ObjRepository,
    private val dropTables: NpcDropTableRegistry,
    @CoreRandom private val random: GameRandom,
) {
    public suspend fun deathNoDrops(access: StandardNpcAccess) {
        access.death(npcRepo, seqTypes, players)
    }

    public suspend fun deathWithDrops(
        access: StandardNpcAccess,
        dropCoords: CoordGrid = access.coords,
    ) {
        access.death(npcRepo, seqTypes, players) { access.npc.spawnDeathDrops(dropCoords) }
    }

    private fun Npc.spawnDeathDrops(dropCoords: CoordGrid) {
        val hero = findHero(players) ?: return
        val table = dropTables[type] ?: return
        val context = NpcDropContext(this, hero, dropCoords, random)
        val resolution = table.resolve(context)
        resolution.messages.forEach(hero::mes)
        resolution.drops.forEach { drop ->
            val duration =
                drop.duration
                    ?: drop.receiver?.lootDropDuration
                    ?: hero.lootDropDuration
                    ?: constants.lootdrop_duration
            val reveal = drop.reveal ?: ObjRepository.DEFAULT_REVEAL_DELAY
            val remains = drop.receiver?.takeIf { it === hero }?.let { RemainsDrop(it, drop.obj) }
            if (remains == null) {
                objRepo.add(drop.obj, drop.coords, duration, drop.receiver, reveal)
            } else {
                events.publish(remains)
                val delivered = remains.remaining == 0 || objRepo.add(
                    org.rsmod.game.obj.Obj.fromOwner(hero, drop.coords, drop.obj.copy(count = remains.remaining)),
                    duration,
                    reveal,
                )
                if (delivered) remains.commit()
            }
        }
    }

    public fun spawnDrops(npc: Npc, dropCoords: CoordGrid = npc.coords) {
        npc.spawnDeathDrops(dropCoords)
    }

    public fun spawnDrops(access: StandardNpcAccess, dropCoords: CoordGrid = access.coords) {
        access.npc.spawnDeathDrops(dropCoords)
    }
}

private var Player.lastCombat: Int by intVarp(varps.lastcombat)
private var Player.aggressiveNpc: NpcUid? by typeNpcUidVarp(varps.aggressive_npc)
private var Npc.aggressivePlayer by typePlayerUidVarn(varns.aggressive_player)

/**
 * Handles the death sequence of this [StandardNpcAccess.npc], including clearing interactions and
 * removing (or hiding, if it respawns) the npc from the world.
 *
 * **Notes:**
 * - This is **not** the way to "kill" a npc. This "death sequence" occurs after the npc has already
 *   been deemed dead and its death queue is being processed.
 * - To queue a npc's death, use [StandardNpcAccess.queueDeath] or [org.rsmod.api.npc.queueDeath]
 *   instead.
 * - This function **does not** spawn any drop table objs for the npc.
 * - Drop table spawns are handled via [NpcDeath.deathWithDrops], which is **automatically called**
 *   for queued deaths by default. However, if you override death queues for specific npc types
 *   (`onNpcQueue(npc_type, queues.death)`), you must explicitly handle drop spawns in the script by
 *   injecting `NpcDeath` and calling either [NpcDeath.deathWithDrops] or [NpcDeath.spawnDrops].
 */
public suspend fun StandardNpcAccess.death(
    npcRepo: NpcRepository,
    seqTypes: SeqTypeList,
    players: PlayerList,
    beforeRemoval: () -> Unit = {},
) {
    walk(coords)
    noneMode()
    hideAllOps()
    arriveDelay()

    val aggressivePlayer = npc.aggressivePlayer
    if (aggressivePlayer != null) {
        val player = aggressivePlayer.resolve(players)

        val deathSound = paramOrNull(params.death_sound)
        if (deathSound != null && player != null) {
            player.soundSynth(deathSound)
        }

        // TODO(combat): Should we assert that npc.uid will always match player.aggressiveNpc at
        // this point?

        if (player != null && player.aggressiveNpc == npc.uid) {
            player.lastCombat = 0
        }
    }

    val deathAnim = param(params.death_anim)
    anim(deathAnim)
    delay(seqTypes[deathAnim])

    beforeRemoval()
    if (npc.respawns) {
        npcRepo.despawn(npc, npc.type.respawnRate)
        return
    }

    npcRepo.del(npc, Int.MAX_VALUE)
}
