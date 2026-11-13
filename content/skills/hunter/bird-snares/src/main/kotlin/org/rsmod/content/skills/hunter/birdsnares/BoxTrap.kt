package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.player.events.HunterCapture
import org.rsmod.api.player.diary.recordDiaryHunter
import jakarta.inject.Inject
import org.rsmod.content.skills.pets.SkillPetRates
import kotlin.math.min
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.controller.vars.intVarCon
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

/** Cache-233 chinchompa box-trap lifecycle. */
public class BoxTrap
@Inject
constructor(private val petRates: SkillPetRates,
    private val placement: BirdSnarePlacement,
    private val conRepo: ControllerRepository,
    private val controllers: ControllerList,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val playerList: PlayerList,
    private val npcTypes: NpcTypeList,
    private val random: GameRandom,
    private val portableTraps: PortableHunterTraps,
) {
    init {
        portableTraps.register(BoxTrapControllerBuilder.trap, BoxTrapVarCons.ownerSlot)
    }

    public suspend fun lay(access: ProtectedAccess, slot: Int) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to use this.")
                return
            }
            if (player.vars[BoxTrapVars.eaglesPeak] < 40) {
                mes("You need to complete Eagles' Peak before using box traps.")
                return
            }
            if (player.stat(stats.hunter) < MINIMUM_LEVEL) {
                mes("You need a Hunter level of $MINIMUM_LEVEL to lay a box trap.")
                return
            }
            if (activeTrapCount(player) >= maximumTraps(player)) {
                mes("You cannot lay any more traps at your current Hunter level.")
                return
            }
            val origin = player.coords
            if (!placement.canPlace(origin)) {
                mes("You can't lay a trap here.")
                return
            }
            if (slot !in player.inv.indices || player.inv[slot]?.id != BoxTrapObjs.snare.id) {
                return
            }

            anim(BoxTrapSeqs.lay)
            delay(LAY_TICKS)
            if (
                player.coords != origin ||
                    activeTrapCount(player) >= maximumTraps(player) ||
                    !placement.canPlace(origin) ||
                    slot !in player.inv.indices ||
                    player.inv[slot]?.id != BoxTrapObjs.snare.id
            ) {
                resetAnim()
                return
            }
            val removed =
                player.invTransaction(player.inv) {
                    delete(select(player.inv), BoxTrapObjs.snare.id, 1, slot)
                }
            if (!removed.success) {
                resetAnim()
                return
            }

            locRepo.add(
                origin,
                BoxTrapLocs.open,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.GroundDecor,
            )
            val controller = Controller(BoxTrapControllerBuilder.trap, origin)
            controller.ownerSlot = player.slotId
            controller.phase = Phase.Open.id
            controller.birdIndex = NO_BIRD
            controller.smoked = 0
            controller.aiTimer(IDLE_TICKS)
            conRepo.add(controller, Int.MAX_VALUE)
            walkOffTrap(this, origin)
        }
    }

    public fun smoke(access: ProtectedAccess, loc: org.rsmod.game.loc.BoundLocInfo) {
        val controller = controller(loc.coords) ?: return
        if (!controller.isOwnedBy(access.player) || controller.phase != Phase.Open.id) {
            access.mes("This isn't your trap.")
            return
        }
        if (!access.player.members) {
            access.mes("You need to be on a members' world to smoke a trap.")
            return
        }
        if (access.player.stat(stats.hunter) < SMOKING_LEVEL) {
            access.mes("You need a Hunter level of $SMOKING_LEVEL to smoke a trap.")
            return
        }
        if (
            BoxTrapObjs.litTorch !in access.player.inv &&
                BoxTrapObjs.brumaTorch !in access.player.inv
        ) {
            access.mes("You need a lit torch to smoke a trap.")
            return
        }
        if (controller.smoked == 0) {
            controller.smoked = 1
            access.mes("You smoke the trap, removing your scent.")
        } else {
            access.mes("This trap has already been smoked.")
        }
    }

    public fun dismantle(access: ProtectedAccess, loc: org.rsmod.game.loc.BoundLocInfo) {
        val controller = controller(loc.coords) ?: return
        if (!controller.isOwnedBy(access.player)) {
            access.mes("This isn't your trap.")
            return
        }
        access.anim(BoxTrapSeqs.check)
        removeTrap(controller, access.player, Recovery.Inventory)
    }

    public fun investigate(access: ProtectedAccess, loc: org.rsmod.game.loc.BoundLocInfo) {
        val controller = controller(loc.coords) ?: return
        if (!controller.isOwnedBy(access.player)) {
            access.mes("This isn't your trap.")
            return
        }
        if (controller.smoked == 1) {
            access.mes("This trap has been smoked.")
        } else {
            access.mes("This trap has not been smoked.")
        }
    }

    public suspend fun check(
        access: ProtectedAccess,
        loc: org.rsmod.game.loc.BoundLocInfo,
        reset: Boolean = false,
    ) {
        val controller = controller(loc.coords) ?: return
        if (!controller.isOwnedBy(access.player)) {
            access.mes("This isn't your trap.")
            return
        }
        if (
            controller.phase != Phase.Full.id ||
                controller.birdIndex !in BoxTrapSpeciesList.all.indices
        ) {
            return
        }
        val bird = BoxTrapSpeciesList.all[controller.birdIndex]
        access.anim(BoxTrapSeqs.check)
        access.delay(if (reset) CHECK_TICKS + LAY_TICKS else CHECK_TICKS)
        if (
            controller(loc.coords) !== controller ||
                controller.phase != Phase.Full.id ||
                !access.isWithinDistance(loc, distance = 1)
        ) {
            access.resetAnim()
            return
        }
        val occupied = petRates.occupiedBy(access.player,bird.product.id)
        val result =
            access.player.invTransaction(access.player.inv) {
                val inv = select(access.player.inv)
                if (!reset) add(inv, BoxTrapObjs.snare.id, 1)
                add(inv, bird.product.id, 1)
            }
        if (!result.success) {
            access.mes("You don't have enough inventory space to collect the trap.")
            return
        }
        val petRoll = when (bird.product) {
            BoxTrapObjs.grey -> 13324 to 131_395
            BoxTrapObjs.red -> 13323 to 98_373
            BoxTrapObjs.black -> 13325 to 82_758
            else -> null
        }
        if(petRoll!=null)petRates.roll(access.player,stats.hunter,petRoll.first,petRoll.second,petRates.newResourceSlot(access.player,bird.product.id,occupied))
        access.statAdvance(stats.hunter, bird.xp)
        access.publish(HunterCapture(access.player, bird.npc.internalNameValue))
        access.player.recordDiaryHunter(bird.npc.id, controller.coords)
        if (reset) {
            replaceLoc(controller.coords, BoxTrapLocs.open)
            controller.phase = Phase.Open.id
            controller.birdIndex = NO_BIRD
            controller.smoked = 0
            controller.aiTimer(IDLE_TICKS)
        } else {
            removeTrap(controller, access.player, Recovery.None)
        }
        access.resetAnim()
    }

    public suspend fun reset(access: ProtectedAccess, loc: org.rsmod.game.loc.BoundLocInfo) {
        val controller = controller(loc.coords) ?: return
        if (!controller.isOwnedBy(access.player)) {
            access.mes("This isn't your trap.")
            return
        }
        if (controller.phase == Phase.Full.id) {
            check(access, loc, reset = true)
            return
        }
        if (controller.phase != Phase.Broken.id) return
        access.anim(BoxTrapSeqs.lay)
        access.delay(LAY_TICKS)
        if (
            controller(loc.coords) !== controller ||
                controller.phase != Phase.Broken.id ||
                !access.isWithinDistance(loc, distance = 1)
        ) {
            access.resetAnim()
            return
        }
        replaceLoc(controller.coords, BoxTrapLocs.open)
        controller.phase = Phase.Open.id
        controller.birdIndex = NO_BIRD
        controller.smoked = 0
        controller.aiTimer(IDLE_TICKS)
        access.resetAnim()
    }

    public suspend fun approach(access: StandardNpcAccess, loc: org.rsmod.game.loc.BoundLocInfo) {
        val controller =
            controller(loc.coords)
                ?: run {
                    access.npc.resetMode()
                    return
                }
        if (controller.phase != Phase.Open.id) {
            access.npc.resetMode()
            return
        }
        val owner =
            owner(controller)
                ?: run {
                    removeTrap(controller, null, Recovery.Ground)
                    access.npc.resetMode()
                    return
                }
        val bird =
            BoxTrapSpeciesList.byNpcId[access.npc.id]
                ?: run {
                    access.npc.resetMode()
                    return
                }
        if (
            playerList.any { it.coords == controller.coords } ||
                owner.stat(stats.hunter) < bird.level
        ) {
            access.npc.resetMode()
            return
        }

        controller.birdIndex = BoxTrapSpeciesList.all.indexOf(bird)
        val caught = random.randomDouble() < catchRate(owner, bird, controller.smoked == 1)
        if (caught) {
            controller.phase = Phase.Trapping.id
            replaceLoc(controller.coords, bird.transition)
            access.anim(BoxTrapSeqs.birdCaught)
        } else {
            controller.phase = Phase.Failing.id
            replaceLoc(controller.coords, BoxTrapLocs.failing)
            access.anim(BoxTrapSeqs.birdLeave)
        }
        controller.aiTimer(TRANSITION_TICKS)
        access.delay(TRANSITION_TICKS)
        if (caught) {
            npcRepo.despawn(access.npc, npcTypes.getValue(access.npc.id).respawnRate)
        } else {
            access.npc.resetMode()
        }
    }

    public fun tick(controller: Controller) {
        when (Phase.from(controller.phase)) {
            Phase.Trapping -> {
                val bird = BoxTrapSpeciesList.all.getOrNull(controller.birdIndex)
                if (bird == null) {
                    removeTrap(controller, owner(controller), Recovery.Ground)
                    return
                }
                replaceLoc(controller.coords, bird.full)
                controller.phase = Phase.Full.id
                controller.aiTimer(IDLE_TICKS)
            }
            Phase.Failing -> {
                replaceLoc(controller.coords, BoxTrapLocs.broken)
                controller.phase = Phase.Broken.id
                controller.birdIndex = NO_BIRD
                controller.smoked = 0
                controller.aiTimer(IDLE_TICKS)
            }
            Phase.Open,
            Phase.Full,
            Phase.Broken,
            null ->
                removeTrap(
                    controller,
                    owner(controller),
                    Recovery.Ground,
                    deferControllerDeletion = true,
                )
        }
    }

    public fun clear(player: Player) {
        controllers
            .filter { it.id == BoxTrapControllerBuilder.trap.id && it.isOwnedBy(player) }
            .toList()
            .forEach { removeTrap(it, player, Recovery.Inventory) }
    }

    public fun activeTrapCount(player: Player): Int = portableTraps.count(player)

    public fun maximumTraps(player: Player): Int {
        val base = min(MAXIMUM_TRAPS, player.stat(stats.hunter) / 20 + 1)
        val wildernessBonus = if (player.vars[varbits.inside_wilderness] == 1) 1 else 0
        return base + wildernessBonus
    }

    public fun catchRate(player: Player, bird: BoxTrapSpecies, smoked: Boolean): Double {
        val level = player.stat(stats.hunter).coerceIn(1, 99)
        val base = SkillingSuccessRate.successRate(bird.lowRate, bird.highRate, level, 99)
        val smokeBonus = if (smoked) SMOKE_BONUS else 0.0
        val outfitBonus = if (player.hasGuildHunterOutfit()) GUILD_OUTFIT_BONUS else 0.0
        return (base + smokeBonus + outfitBonus).coerceAtMost(1.0)
    }

    private fun controller(coords: CoordGrid): Controller? =
        conRepo.findExact(coords, BoxTrapControllerBuilder.trap)

    private fun owner(controller: Controller): Player? = playerList[controller.ownerSlot]

    private fun Controller.isOwnedBy(player: Player): Boolean = ownerSlot == player.slotId

    private fun Player.hasGuildHunterOutfit(): Boolean {
        val wornIds = worn.filterNotNull().mapTo(hashSetOf()) { it.id }
        return GUILD_OUTFIT.all { it.id in wornIds }
    }

    private fun replaceLoc(coords: CoordGrid, into: org.rsmod.game.type.loc.LocType) {
        val current = currentTrapLoc(coords) ?: return
        locRepo.change(current, into, Int.MAX_VALUE)
    }

    private fun removeTrap(
        controller: Controller,
        owner: Player?,
        recovery: Recovery,
        deferControllerDeletion: Boolean = false,
    ) {
        currentTrapLoc(controller.coords)?.let { locRepo.del(it, Int.MAX_VALUE) }
        if (deferControllerDeletion) {
            controller.duration(0)
        } else {
            conRepo.del(controller)
        }
        when (recovery) {
            Recovery.None -> Unit
            Recovery.Inventory -> {
                if (owner != null) {
                    owner.invAddOrDrop(objRepo, BoxTrapObjs.snare, coords = controller.coords)
                } else {
                    dropSnare(controller.coords, null)
                }
            }
            Recovery.Ground -> dropSnare(controller.coords, owner)
        }
    }

    private fun dropSnare(coords: CoordGrid, owner: Player?) {
        objRepo.add(
            BoxTrapObjs.snare,
            coords,
            constants.lootdrop_duration,
            receiver = owner,
            reveal = 0,
        )
    }

    private fun currentTrapLoc(coords: CoordGrid): LocInfo? =
        locRepo.findAll(coords).firstOrNull { it.id in TRAP_LOC_IDS }

    private fun walkOffTrap(access: ProtectedAccess, origin: CoordGrid) {
        val destination =
            MOVE_ORDER.asSequence().map(origin::translate).firstOrNull {
                access.lineOfWalk(origin, it)
            } ?: return
        access.walk(destination)
    }

    private enum class Phase(val id: Int) {
        Open(0),
        Trapping(1),
        Failing(2),
        Full(3),
        Broken(4);

        companion object {
            fun from(id: Int): Phase? = entries.firstOrNull { it.id == id }
        }
    }

    private enum class Recovery {
        None,
        Inventory,
        Ground,
    }

    public companion object {
        public const val SMOKING_LEVEL: Int = 39
        public const val MINIMUM_LEVEL: Int = 27
        public const val LAY_TICKS: Int = 4
        public const val CHECK_TICKS: Int = 4
        public const val TRANSITION_TICKS: Int = 2
        public const val IDLE_TICKS: Int = 100
        public const val ATTRACTION_RADIUS: Int = 2
        public const val MAXIMUM_TRAPS: Int = 5
        public const val SMOKE_BONUS: Double = 0.02
        public const val GUILD_OUTFIT_BONUS: Double = 0.025
        private const val NO_BIRD: Int = -1

        private val MOVE_ORDER =
            listOf(Direction.West, Direction.East, Direction.South, Direction.North)
        private val GUILD_OUTFIT =
            listOf(
                BoxTrapObjs.guildHood,
                BoxTrapObjs.guildTop,
                BoxTrapObjs.guildLegs,
                BoxTrapObjs.guildBoots,
            )
        private val TRAP_LOC_IDS = buildSet {
            add(BoxTrapLocs.open.id)
            add(BoxTrapLocs.broken.id)
            add(BoxTrapLocs.failing.id)
            for (bird in BoxTrapSpeciesList.all) {
                add(bird.transition.id)
                add(bird.full.id)
            }
        }

        private var Controller.ownerSlot: Int by intVarCon(BoxTrapVarCons.ownerSlot)
        private var Controller.phase: Int by intVarCon(BoxTrapVarCons.phase)
        private var Controller.birdIndex: Int by intVarCon(BoxTrapVarCons.birdIndex)
        private var Controller.smoked: Int by intVarCon(BoxTrapVarCons.smoked)
    }
}
