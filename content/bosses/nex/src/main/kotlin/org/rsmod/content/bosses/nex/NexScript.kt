package org.rsmod.content.bosses.nex

import jakarta.inject.Inject
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class NexScript
@Inject
constructor(
    private val controller: NexController,
    private val access: NexAccessController,
    private val combat: NexCombat,
    private val deathStorage: NexDeathStorage,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        NexNpcs.combatants.forEach { type ->
            onAiOpPlayer2(type) { combat.attack(this, it.target) }
            onAiApPlayer2(type) { combat.attack(this, it.target) }
        }
        NexNpcs.forms.forEach { type ->
            onModifyNpcHit(type) { controller.modifyNexHit(npc, hit) }
            onNpcHit(type) { controller.onNexHit(npc, hit) }
        }
        NexNpcs.mages.forEach { type ->
            onModifyNpcHit(type) { controller.modifyMageHit(npc, hit) }
        }
        onEvent<NpcKillEvents.Credited> {
            access.onGodWarsKill(player, npc)
            when {
                npc.id == NexNpcs.boss.id -> {
                    controller.wrath(npc)
                    access.addAncientEssence(player, npc)
                }
                npc.id in NexNpcs.mages.map { it.id } -> controller.onMageKilled(npc)
                npc.id in NexNpcs.prisonFollowers.map { it.id } ->
                    access.addAncientEssence(player, npc)
            }
        }
        onEvent<PlayerDeathEvent.Completed> { deathStorage.onDeath(player) }
        onEvent<NpcStateEvents.Respawn> {
            when {
                npc.id == NexNpcs.boss.id -> {
                    configureNex(npc)
                    controller.reset(npc)
                    bindMages(npc)
                }
                npc.id in NexNpcs.mages.map { it.id } -> {
                    configureMage(npc)
                    findNex()?.let { controller.bindMage(npc, it) }
                }
                npc.id in NexNpcs.prisonFollowers.map { it.id } -> configurePrisonFollower(npc)
            }
        }

        listOf(NexLocs.frozenDoorOuter1, NexLocs.frozenDoorOuter2).forEach { door ->
            onOpLoc1(door) { access.enterFrozenDoor(this) }
        }
        listOf(NexLocs.frozenDoorInner1, NexLocs.frozenDoorInner2).forEach { door ->
            onOpLoc1(door) { access.leaveFrozenDoor(this) }
        }
        onOpLoc1(NexLocs.outerPrisonDoor) { access.enterSafeRoom(this) }
        onOpLoc1(NexLocs.innerPrisonDoor) { access.leaveSafeRoom(this) }
        onOpLoc1(NexLocs.fightBarrierOuter) { access.enterChamber(this) }
        listOf(NexLocs.gravestoneChest, NexLocs.gravestoneChestNoop).forEach { chest ->
            onOpLoc1(chest) { deathStorage.reclaim(this) }
        }
        listOf(
                NexObjs.armadylKeyPiece,
                NexObjs.bandosKeyPiece,
                NexObjs.zamorakKeyPiece,
                NexObjs.saradominKeyPiece,
            )
            .forEach { piece -> onOpHeld1(piece) { access.assembleFrozenKey(this) } }

        val nex = spawnPermanent(NexNpcs.boss, NexArena.bossSpawn, ::configureNex)
        controller.reset(nex)
        NexArena.mageSpawns.forEach { (type, coords) ->
            val mage = spawnPermanent(type, coords, ::configureMage)
            controller.bindMage(mage, nex)
        }
        NexAccessMap.prisonFollowers.forEach { (type, coords) ->
            spawnPermanent(type, coords, ::configurePrisonFollower)
        }
    }

    private fun spawnPermanent(type: NpcType, coords: CoordGrid, configure: (Npc) -> Unit): Npc {
        val existing = npcs.findAll(coords).firstOrNull { it.id == type.id }
        if (existing != null) {
            configure(existing)
            return existing
        }
        return Npc(npcTypes[type], coords).also { npc ->
            configure(npc)
            npcs.add(npc, Int.MAX_VALUE)
        }
    }

    private fun configureNex(npc: Npc) {
        npc.apply {
            hitpoints = HITPOINTS
            baseHitpointsLvl = HITPOINTS
            attackLvl = 315
            baseAttackLvl = 315
            strengthLvl = 200
            baseStrengthLvl = 200
            defenceLvl = 260
            baseDefenceLvl = 260
            magicLvl = 230
            baseMagicLvl = 230
            rangedLvl = 350
            baseRangedLvl = 350
            mode = NpcMode.Wander
            setHunt(HUNT_RANGE)
            setHuntMode(huntmodes.constant_ranged)
        }
    }

    private fun configureMage(npc: Npc) {
        npc.apply {
            hitpoints = MAGE_HITPOINTS
            baseHitpointsLvl = MAGE_HITPOINTS
            attackLvl = 1
            baseAttackLvl = 1
            strengthLvl = 1
            baseStrengthLvl = 1
            defenceLvl = 200
            baseDefenceLvl = 200
            magicLvl = 200
            baseMagicLvl = 200
            rangedLvl = 1
            baseRangedLvl = 1
            mode = NpcMode.Wander
            setHunt(HUNT_RANGE)
            setHuntMode(huntmodes.constant_ranged)
        }
    }

    private fun configurePrisonFollower(npc: Npc) {
        npc.apply {
            hitpoints = PRISON_HITPOINTS
            baseHitpointsLvl = PRISON_HITPOINTS
            attackLvl = 120
            baseAttackLvl = 120
            strengthLvl = 120
            baseStrengthLvl = 120
            defenceLvl = 100
            baseDefenceLvl = 100
            magicLvl = 120
            baseMagicLvl = 120
            rangedLvl = 120
            baseRangedLvl = 120
            mode = NpcMode.Wander
            setHunt(HUNT_RANGE)
            setHuntMode(huntmodes.constant_ranged)
        }
    }

    private fun findNex(): Npc? =
        npcs.findAll(NexArena.bossSpawn).firstOrNull { it.id == NexNpcs.boss.id }

    private fun bindMages(nex: Npc) {
        NexArena.mageSpawns.forEach { (type, coords) ->
            npcs
                .findAll(coords)
                .firstOrNull { it.id == type.id }
                ?.let { controller.bindMage(it, nex) }
        }
    }

    public companion object {
        public const val HITPOINTS: Int = 3_400
        public const val MAGE_HITPOINTS: Int = 500
        public const val PRISON_HITPOINTS: Int = 120
        public const val HUNT_RANGE: Int = 18
    }
}
