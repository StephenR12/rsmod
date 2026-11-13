package org.rsmod.content.skills.firemaking

import org.rsmod.api.player.diary.recordDiaryFire
import jakarta.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.recordLumbridgeOakFire
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseCraftingLvl
import org.rsmod.api.player.stat.baseFiremakingLvl
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.loc.LocType
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.loc.LocLayerConstants

public class Firemaking
@Inject
constructor(
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val mapClock: MapClock,
    private val placement: FiremakingPlacement,
    private val gnomishFirelighter: GnomishFirelighter,
    private val firemakingXp: FiremakingXp,
) {
    public fun colourLogs(
        access: ProtectedAccess,
        firelighterSlot: Int,
        logSlot: Int,
        recipe: ColouredLogRecipe,
    ) {
        with(access) {
            if (
                player.inv[firelighterSlot]?.id != recipe.firelighter.id ||
                    player.inv[logSlot]?.id != objs.logs.id
            ) {
                return
            }
            if (!player.members) {
                mes("You need to be on a members' world to use this item.")
                return
            }
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.firelighter.id, count = 1, slot = firelighterSlot)
                    delete(inventory, objs.logs.id, count = 1, slot = logSlot)
                    add(inventory, recipe.log.id, count = 1, slot = logSlot)
                }
            if (transaction.success) {
                mes("You coat the logs with the ${recipe.colour} chemicals.")
            }
        }
    }

    public fun lightFromInventory(access: ProtectedAccess, logSlot: Int, recipe: FiremakingRecipe) {
        with(access) {
            if (
                !validateRequirements(recipe, coords, FiremakingIgnition.Tinderbox) ||
                    player.inv[logSlot]?.id != recipe.log.id
            ) {
                return
            }
            if (!player.inv.contains(objs.tinderbox)) {
                return
            }

            val deletion = invDel(inv, recipe.log, slot = logSlot, autoCommit = false)
            if (deletion.failure) {
                return
            }
            val groundLog =
                objRepo.add(
                    InvObj(recipe.log),
                    coords,
                    duration = constants.lootdrop_duration,
                    receiver = player,
                )
            val registered = objRepo.findAll(coords).any { it === groundLog }
            if (!registered) {
                return
            }
            deletion.commitAll()
            beginAttempt(this, groundLog, recipe, FiremakingIgnition.Tinderbox)
        }
    }

    public fun lightFromBow(
        access: ProtectedAccess,
        logSlot: Int,
        bowSlot: Int,
        recipe: FiremakingRecipe,
        tool: BowFiremakingTool,
    ) {
        with(access) {
            val ignition = FiremakingIgnition.Bow(tool)
            if (
                player.inv[logSlot]?.id != recipe.log.id ||
                    player.inv[bowSlot]?.id != tool.bow.id ||
                    !validateRequirements(recipe, coords, ignition)
            ) {
                return
            }

            val deletion = invDel(inv, recipe.log, slot = logSlot, autoCommit = false)
            if (deletion.failure) {
                return
            }
            val groundLog =
                objRepo.add(
                    InvObj(recipe.log),
                    coords,
                    duration = constants.lootdrop_duration,
                    receiver = player,
                )
            val registered = objRepo.findAll(coords).any { it === groundLog }
            if (!registered) {
                return
            }
            deletion.commitAll()
            beginAttempt(this, groundLog, recipe, ignition)
        }
    }

    public fun lightFromGnomishFirelighter(
        access: ProtectedAccess,
        logSlot: Int,
        firelighterSlot: Int,
    ) {
        with(access) {
            if (player.inv[logSlot]?.id != objs.logs.id) {
                return
            }
            val state = gnomishFirelighter.state(player.inv, firelighterSlot) ?: return
            val recipe = gnomishRecipe(state.colour)
            if (
                !validateRequirements(
                    recipe,
                    coords,
                    FiremakingIgnition.Gnomish(firelighterSlot, state),
                )
            ) {
                return
            }

            val deletion = invDel(inv, objs.logs, slot = logSlot, autoCommit = false)
            if (deletion.failure) {
                return
            }
            val groundLog =
                objRepo.add(
                    InvObj(objs.logs),
                    coords,
                    duration = constants.lootdrop_duration,
                    receiver = player,
                )
            if (objRepo.findAll(coords).none { it === groundLog }) {
                return
            }
            deletion.commitAll()
            beginAttempt(
                this,
                groundLog,
                recipe,
                FiremakingIgnition.Gnomish(firelighterSlot, state),
            )
        }
    }

    public fun lightGroundLogFromGnomishFirelighter(
        access: ProtectedAccess,
        groundLog: Obj,
        firelighterSlot: Int,
    ) {
        with(access) {
            if (
                groundLog.type != objs.logs.id ||
                    !groundLog.isVisibleTo(player) ||
                    !isRegistered(groundLog)
            ) {
                return
            }
            val state = gnomishFirelighter.state(player.inv, firelighterSlot) ?: return
            val recipe = gnomishRecipe(state.colour)
            val ignition = FiremakingIgnition.Gnomish(firelighterSlot, state)
            if (!validateRequirements(recipe, groundLog.coords, ignition)) {
                return
            }
            beginAttempt(this, groundLog, recipe, ignition)
        }
    }

    public fun lightFromGround(access: ProtectedAccess, groundLog: Obj, recipe: FiremakingRecipe) {
        with(access) {
            if (!groundLog.isVisibleTo(player) || !isRegistered(groundLog)) {
                return
            }
            if (!player.inv.contains(objs.tinderbox)) {
                val slot = gnomishFirelighter.findChargedSlot(player.inv)
                val state = gnomishFirelighter.state(player.inv, slot)
                if (recipe.log != objs.logs || state == null) {
                    mes("You need a tinderbox to light these logs.")
                    return
                }
                lightGroundLogFromGnomishFirelighter(this, groundLog, slot)
                return
            }
            if (!validateRequirements(recipe, groundLog.coords, FiremakingIgnition.Tinderbox)) {
                return
            }
            beginAttempt(this, groundLog, recipe, FiremakingIgnition.Tinderbox)
        }
    }

    public fun process(access: ProtectedAccess, request: FiremakingRequest) {
        with(access) {
            val recipe = request.recipe
            val groundLog = request.groundLog
            if (!groundLog.isVisibleTo(player) || !isRegistered(groundLog)) {
                resetAnim()
                return
            }
            if (!validateIgnition(request.ignition)) {
                resetAnim()
                return
            }
            if (!validateRequirements(recipe, groundLog.coords, request.ignition)) {
                resetAnim()
                return
            }

            if (
                !recipe.guaranteed &&
                    !rollSuccess(player.firemakingLvl, random.of(ROLL_DENOMINATOR))
            ) {
                animateAttempt(this, request.ignition)
                weakQueue(firemaking_queues.attempt, ATTEMPT_TICKS, request)
                return
            }

            val duration = random.of(MIN_FIRE_TICKS, MAX_FIRE_TICKS)
            val fire = createFire(groundLog.coords, recipe.fire)
            if (!locRepo.add(fire, duration)) {
                resetAnim()
                mes(CANNOT_LIGHT_HERE)
                return
            }
            if (!objRepo.del(groundLog)) {
                locRepo.del(fire, duration = Int.MAX_VALUE)
                resetAnim()
                return
            }
            if (request.ignition is FiremakingIgnition.Gnomish) {
                check(
                    gnomishFirelighter.consume(
                        player.inv,
                        request.ignition.slot,
                        request.ignition.state,
                    )
                )
                if (request.ignition.state.charges == 1) {
                    mes("Your Gnomish firelighter has run out of charges.")
                }
            }

            // Delayed objs are processed pre-tick while temporary locs expire post-tick. This
            // offset makes the ashes appear in the same cycle in which the fire disappears.
            val ashes = Obj.fromServer(this@Firemaking.mapClock, fire.coords, objs.ashes, count = 1)
            objRepo.addDelayed(
                ashes,
                spawnDelay = duration - 1,
                duration = constants.lootdrop_duration,
            )
            if (
                request.ignition is FiremakingIgnition.Bow &&
                    vars[varbits.brut_fire] == BARBARIAN_TASK_ASSIGNED
            ) {
                vars[varbits.brut_fire] = BARBARIAN_FIRE_LIT
            }
            firemakingXp.advance(this, recipe.xp)
            player.recordDiaryFire(recipe.log.id, fire.coords, request.ignition is FiremakingIgnition.Bow)
            if (recipe.log.id == objs.oak_logs.id) player.recordLumbridgeOakFire(fire.coords)
            resetAnim()
            mes("The fire catches and the logs begin to burn.")
            walkAfterLighting(this, fire.coords)
        }
    }

    private fun beginAttempt(
        access: ProtectedAccess,
        groundLog: Obj,
        recipe: FiremakingRecipe,
        ignition: FiremakingIgnition,
    ) {
        with(access) {
            clearWeakQueue(firemaking_queues.attempt)
            clearWeakQueue(firemaking_queues.pyreLogPreparation)
            mes("You attempt to light the logs.")
            animateAttempt(this, ignition)
            weakQueue(
                firemaking_queues.attempt,
                ATTEMPT_TICKS,
                FiremakingRequest(groundLog, recipe, ignition),
            )
        }
    }

    private fun ProtectedAccess.validateRequirements(
        recipe: FiremakingRecipe,
        fireCoords: CoordGrid,
        ignition: FiremakingIgnition,
    ): Boolean {
        if (ignition is FiremakingIgnition.Bow && !validateBowRequirements(recipe)) {
            return false
        }
        if (recipe.members && !player.members) {
            mes("You need to be on a members' world to use this item.")
            return false
        }
        if (player.firemakingLvl < recipe.level) {
            mes("You need a Firemaking level of ${recipe.level} to light these logs.")
            return false
        }
        val placementRejection = placement.rejectionAt(fireCoords)
        if (placementRejection != null) {
            mes(placementRejection)
            return false
        }
        return true
    }

    private fun ProtectedAccess.validateBowRequirements(recipe: FiremakingRecipe): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to light logs with a bow.")
            return false
        }
        if (recipe !in FiremakingRecipes.barbarian) {
            mes("You can't light these logs with a bow.")
            return false
        }
        val progress = vars[varbits.brut_fire]
        if (progress == BARBARIAN_TASK_ASSIGNED) {
            if (
                player.baseFiremakingLvl < BARBARIAN_INITIAL_FIREMAKING_LEVEL ||
                    player.baseCraftingLvl < BARBARIAN_INITIAL_CRAFTING_LEVEL
            ) {
                mes(
                    "You need Firemaking level $BARBARIAN_INITIAL_FIREMAKING_LEVEL and " +
                        "Crafting level $BARBARIAN_INITIAL_CRAFTING_LEVEL to begin this training."
                )
                return false
            }
            if (recipe.log != objs.oak_logs) {
                mes("Otto asked you to light oak logs with a bow.")
                return false
            }
            return true
        }
        if (progress != BARBARIAN_TRAINING_COMPLETE) {
            mes("You need to complete Otto's barbarian Firemaking training first.")
            return false
        }
        val requiredLevel = barbarianLevel(recipe)
        if (player.firemakingLvl < requiredLevel) {
            mes("You need a Firemaking level of $requiredLevel to light these logs with a bow.")
            return false
        }
        return true
    }

    private fun animateAttempt(access: ProtectedAccess, ignition: FiremakingIgnition) {
        with(access) {
            when (ignition) {
                FiremakingIgnition.Tinderbox -> {
                    anim(seqs.human_createfire)
                    soundSynth(synths.tinder_strike)
                }
                is FiremakingIgnition.Bow -> {
                    anim(ignition.tool.sequence)
                    spotanim(spotanims.brut_bow_firemaking)
                }
                is FiremakingIgnition.Gnomish -> {
                    anim(seqs.human_createfire)
                    soundSynth(synths.tinder_strike)
                }
            }
        }
    }

    private fun ProtectedAccess.validateIgnition(ignition: FiremakingIgnition): Boolean {
        if (ignition !is FiremakingIgnition.Gnomish) {
            return true
        }
        return gnomishFirelighter.state(player.inv, ignition.slot) == ignition.state
    }

    private fun gnomishRecipe(colour: ColouredLogRecipe): FiremakingRecipe =
        FiremakingRecipe(
            log = objs.logs,
            level = 1,
            xp = 50.0,
            members = true,
            fire = colour.fire,
            guaranteed = true,
        )

    private fun walkAfterLighting(access: ProtectedAccess, fireCoords: CoordGrid) {
        with(access) {
            val destination =
                MOVE_ORDER.asSequence().map(fireCoords::translate).firstOrNull {
                    lineOfWalk(fireCoords, it)
                } ?: return
            walk(destination)
        }
    }

    private fun isRegistered(obj: Obj): Boolean = objRepo.findAll(obj.coords).any { it === obj }

    private fun createFire(coords: CoordGrid, type: LocType): LocInfo {
        val shape = LocShape.CentrepieceStraight
        val entity = LocEntity(type.id, shape.id, LocAngle.West.id)
        return LocInfo(LocLayerConstants.of(shape.id), coords, entity)
    }

    public companion object {
        public const val ATTEMPT_TICKS: Int = 4
        public const val MIN_FIRE_TICKS: Int = 100
        public const val MAX_FIRE_TICKS: Int = 199
        public const val ROLL_DENOMINATOR: Int = 256
        public const val BARBARIAN_TASK_ASSIGNED: Int = 1
        public const val BARBARIAN_FIRE_LIT: Int = 2
        public const val BARBARIAN_TRAINING_COMPLETE: Int = 3
        public const val BARBARIAN_INITIAL_FIREMAKING_LEVEL: Int = 35
        public const val BARBARIAN_INITIAL_CRAFTING_LEVEL: Int = 11
        public const val BARBARIAN_LEVEL_OFFSET: Int = 20
        public const val CANNOT_LIGHT_HERE: String = "You can't light a fire here."

        public val MOVE_ORDER: List<Direction> =
            listOf(Direction.West, Direction.East, Direction.South, Direction.North)

        public fun successfulRolls(level: Int): Int {
            val cappedLevel = level.coerceAtLeast(1)
            val interpolated = 64.0 + (cappedLevel - 1) * 448.0 / 98.0
            return min(ROLL_DENOMINATOR, interpolated.roundToInt() + 1)
        }

        public fun rollSuccess(level: Int, roll: Int): Boolean {
            require(roll in 0 until ROLL_DENOMINATOR) { "Invalid Firemaking roll: $roll" }
            return roll < successfulRolls(level)
        }

        public fun barbarianLevel(recipe: FiremakingRecipe): Int = barbarianLevel(recipe.level)

        public fun barbarianLevel(ordinaryLevel: Int): Int =
            min(99, ordinaryLevel + BARBARIAN_LEVEL_OFFSET)
    }
}

public sealed interface FiremakingIgnition {
    public data object Tinderbox : FiremakingIgnition

    public data class Bow(val tool: BowFiremakingTool) : FiremakingIgnition

    public data class Gnomish(val slot: Int, val state: GnomishFirelighterState) :
        FiremakingIgnition
}

public data class FiremakingRequest(
    val groundLog: Obj,
    val recipe: FiremakingRecipe,
    val ignition: FiremakingIgnition = FiremakingIgnition.Tinderbox,
)
