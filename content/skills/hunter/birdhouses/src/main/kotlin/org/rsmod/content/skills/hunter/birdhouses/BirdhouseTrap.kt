package org.rsmod.content.skills.hunter.birdhouses

import jakarta.inject.Inject
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.floor
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.skills.crafting.birdhouses.BirdhouseCrafting
import org.rsmod.content.skills.crafting.birdhouses.BirdhouseRecipe
import org.rsmod.content.skills.crafting.birdhouses.BirdhouseRecipes
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjTypeList

public fun interface BirdhouseClock {
    public fun epochSecond(): Int
}

public class SystemBirdhouseClock @Inject constructor() : BirdhouseClock {
    override fun epochSecond(): Int = Instant.now().epochSecond.toInt()
}

public class BirdhouseTrap
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val clock: BirdhouseClock,
    private val random: GameRandom,
    private val objRepo: ObjRepository,
    private val crafting: BirdhouseCrafting,
) {
    private var ProtectedAccess.displayA by intVarp(BirdhouseTrapVarps.displayA)
    private var ProtectedAccess.displayB by intVarp(BirdhouseTrapVarps.displayB)
    private var ProtectedAccess.displayC by intVarp(BirdhouseTrapVarps.displayC)
    private var ProtectedAccess.displayD by intVarp(BirdhouseTrapVarps.displayD)
    private var ProtectedAccess.readyAtA by intVarp(BirdhouseTrapVarps.readyAtA)
    private var ProtectedAccess.readyAtB by intVarp(BirdhouseTrapVarps.readyAtB)
    private var ProtectedAccess.readyAtC by intVarp(BirdhouseTrapVarps.readyAtC)
    private var ProtectedAccess.readyAtD by intVarp(BirdhouseTrapVarps.readyAtD)

    public suspend fun buildBest(access: ProtectedAccess, loc: BoundLocInfo) {
        val site = BirdhouseSites.byCoords[loc.coords] ?: return
        if (access.state(site) != 0) {
            return
        }
        val carried = bestCarriedTier(access)
        if (carried != null) {
            place(access, loc, carried, access.player.inv.indexOfFirst { it?.id == carried.obj.id })
            return
        }

        val recipe = selectLogRecipe(access) ?: return
        val tier = BirdhouseTiers.byObjId.getValue(recipe.output.id)
        if (
            !canPlace(access, tier, sendFailure = true) ||
                !crafting.canMake(access, recipe, sendFailure = true)
        ) {
            return
        }
        crafting.beginSingle(access)
        access.delay(DIRECT_BUILD_TICKS)
        if (
            !access.isWithinDistance(loc, 1) ||
                access.state(site) != 0 ||
                !canPlace(access, tier, sendFailure = false) ||
                !crafting.makeOne(access, recipe)
        ) {
            access.resetAnim()
            return
        }
        val slot = access.player.inv.indexOfFirst { it?.id == tier.obj.id }
        place(access, loc, tier, slot)
    }

    public fun place(access: ProtectedAccess, loc: BoundLocInfo, tier: BirdhouseTier, slot: Int) {
        with(access) {
            val site = BirdhouseSites.byCoords[loc.coords] ?: return
            if (!canPlace(this, tier, sendFailure = true)) {
                return
            }
            if (
                slot !in player.inv.indices ||
                    state(site) != 0 ||
                    player.inv[slot]?.id != tier.obj.id
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) { delete(select(player.inv), tier.obj.id, 1) }
            if (!result.success) {
                return
            }
            setReadyAt(site, 0)
            setState(site, tier.builtState)
            soundSynth(BirdhouseTrapSynths.buildFill)
        }
    }

    /**
     * Dismantles a partially seeded house after an explicit loss warning. The constructed shell and
     * inserted seeds are lost, while the clockwork is returned as established by the cutoff
     * lifecycle rules.
     */
    public suspend fun dismantlePartial(access: ProtectedAccess, loc: BoundLocInfo) {
        with(access) {
            val site = BirdhouseSites.byCoords[loc.coords] ?: return
            val display = state(site)
            val tier = tier(display) ?: return
            if (display != tier.builtState + SEEDED_OFFSET) {
                return
            }
            val units = player.vars[site.readyAtVarp]
            val confirmed =
                choice2(
                    "Yes, dismantle it.",
                    true,
                    "No, leave it.",
                    false,
                    title = PARTIAL_DISMANTLE_WARNING,
                )
            if (!confirmed || state(site) != display || player.vars[site.readyAtVarp] != units) {
                return
            }
            discard(site)
        }
    }

    /** Emptying a mature house harvests it; doing so early first presents the required warning. */
    public suspend fun empty(access: ProtectedAccess, loc: BoundLocInfo) {
        with(access) {
            val site = BirdhouseSites.byCoords[loc.coords] ?: return
            val display = state(site)
            val tier = tier(display) ?: return
            if (display != tier.builtState + ACTIVE_OFFSET) {
                return
            }
            if (isReady(player.vars, site)) {
                harvest(this, loc)
                return
            }
            val readyAt = player.vars[site.readyAtVarp]
            val confirmed =
                choice2("Yes, empty it.", true, "No, leave it.", false, title = EARLY_EMPTY_WARNING)
            if (!confirmed || state(site) != display || player.vars[site.readyAtVarp] != readyAt) {
                return
            }
            discard(site)
        }
    }

    /**
     * Performs cache-233's one-click mature-house replacement. A carried pre-made house takes
     * priority. Otherwise the old mechanism is retained in-place while one selected log is crafted
     * around it, so a full inventory cannot incorrectly drop the mechanism mid-reset.
     */
    public suspend fun reset(access: ProtectedAccess, loc: BoundLocInfo) {
        with(access) {
            val site = BirdhouseSites.byCoords[loc.coords] ?: return
            val oldDisplay = state(site)
            val oldTier = tier(oldDisplay) ?: return
            if (oldDisplay != oldTier.builtState + ACTIVE_OFFSET || !isReady(player.vars, site)) {
                mes("This bird house is not ready to be reset.")
                return
            }

            val carried = bestCarriedTier(this)
            if (carried != null) {
                val slot = player.inv.indexOfFirst { it?.id == carried.obj.id }
                delay(PREMADE_RESET_DELAY_TICKS)
                if (
                    !isUnchangedReady(site, oldDisplay) ||
                        !canPlace(this, carried, sendFailure = false) ||
                        slot !in player.inv.indices ||
                        player.inv[slot]?.id != carried.obj.id
                ) {
                    return
                }
                if (harvest(this, loc)) {
                    place(this, loc, carried, slot)
                }
                return
            }

            val recipe = selectLogRecipe(this, requireClockwork = false) ?: return
            val replacement = BirdhouseTiers.byObjId.getValue(recipe.output.id)
            if (
                !canPlace(this, replacement, sendFailure = true) ||
                    !crafting.canMake(this, recipe, sendFailure = true, requireClockwork = false)
            ) {
                return
            }
            crafting.beginSingle(this)
            delay(RAW_RESET_TICKS)
            if (
                !isWithinDistance(loc, 1) ||
                    !isUnchangedReady(site, oldDisplay) ||
                    !canPlace(this, replacement, sendFailure = false) ||
                    !crafting.canMake(this, recipe, sendFailure = false, requireClockwork = false)
            ) {
                resetAnim()
                return
            }
            if (!harvest(this, loc, returnClockwork = false)) {
                return
            }
            check(crafting.makeFromReturnedClockwork(this, recipe)) {
                "Birdhouse reset materials changed after protected revalidation: $recipe"
            }
            setReadyAt(site, 0)
            setState(site, replacement.builtState)
            soundSynth(BirdhouseTrapSynths.buildFill)
        }
    }

    public fun fillFirstSeed(access: ProtectedAccess, loc: BoundLocInfo) {
        val slot =
            access.player.inv.indexOfFirst { obj ->
                obj != null && BirdhouseSeeds.byObjId.containsKey(obj.id)
            }
        if (slot < 0) {
            access.mes("You don't have any suitable seeds to fill this bird house.")
            return
        }
        fill(access, loc, slot)
    }

    public fun fill(access: ProtectedAccess, loc: BoundLocInfo, slot: Int) {
        with(access) {
            val site = BirdhouseSites.byCoords[loc.coords] ?: return
            val display = state(site)
            val tier = tier(display) ?: return
            if (display == tier.builtState + ACTIVE_OFFSET) {
                return
            }
            if (slot !in player.inv.indices) {
                return
            }
            val invObj = player.inv[slot] ?: return
            val seed = BirdhouseSeeds.byObjId[invObj.id] ?: return
            val currentUnits = seedUnits(site, tier)
            val remainingUnits = REQUIRED_SEED_UNITS - currentUnits
            if (remainingUnits <= 0) {
                return
            }
            val count = minOf(invObj.count, ceil(remainingUnits.toDouble() / seed.units).toInt())
            val result =
                player.invTransaction(player.inv) { delete(select(player.inv), seed.obj.id, count) }
            if (!result.success) {
                return
            }
            val units = (currentUnits + count * seed.units).coerceAtMost(REQUIRED_SEED_UNITS)
            if (units == REQUIRED_SEED_UNITS) {
                setState(site, tier.builtState + ACTIVE_OFFSET)
                setReadyAt(site, clock.epochSecond() + MATURATION_SECONDS)
            } else {
                setState(site, tier.builtState + SEEDED_OFFSET)
                setReadyAt(site, units)
            }
            soundSynth(BirdhouseTrapSynths.buildFill)
        }
    }

    public fun isReady(
        playerVars: org.rsmod.game.vars.VarPlayerIntMap,
        site: BirdhouseSite,
    ): Boolean {
        val readyAt = playerVars[site.readyAtVarp]
        return readyAt > REQUIRED_SEED_UNITS && readyAt <= clock.epochSecond()
    }

    /**
     * Harvests a fully matured bird house. The cache-233 cutoff evidence independently establishes
     * the tier XP, guaranteed clockwork, feather quantity, raw bird meat placement and seed-nest
     * roll. The separate ten-roll other-nest table is intentionally not approximated here while its
     * clue-nest selection remains explicitly unconfirmed by the source page.
     */
    public fun harvest(
        access: ProtectedAccess,
        loc: BoundLocInfo,
        returnClockwork: Boolean = true,
    ): Boolean {
        with(access) {
            val site = BirdhouseSites.byCoords[loc.coords] ?: return false
            val tier = tier(state(site)) ?: return false
            if (state(site) != tier.builtState + ACTIVE_OFFSET || !isReady(player.vars, site)) {
                return false
            }

            val visibleHunterLevel = stat(stats.hunter)
            setState(site, 0)
            setReadyAt(site, 0)
            player.statAdvance(stats.hunter, tier.hunterXp)

            if (returnClockwork) {
                invAddOrDrop(objRepo, BirdhouseTrapObjs.clockwork)
            }
            invAddOrDrop(
                objRepo,
                BirdhouseTrapObjs.feather,
                FEATHER_MIN + random.of(FEATHER_QUANTITY_COUNT) * FEATHER_STEP,
            )
            if (random.of(SEED_NEST_DENOMINATOR) < seedNestChanceNumerator(visibleHunterLevel)) {
                invAddOrDrop(objRepo, BirdhouseTrapObjs.seedNest)
            }

            // Unlike the other rewards, all ten pieces always appear on the floor.
            objRepo.add(
                type = BirdhouseTrapObjs.rawBirdMeat,
                coords = player.coords,
                duration = Constants.lootdrop_duration,
                receiver = player,
                count = RAW_BIRD_MEAT_COUNT,
            )
            return true
        }
    }

    private fun ProtectedAccess.state(site: BirdhouseSite): Int = player.vars[site.displayVarp]

    private fun ProtectedAccess.discard(site: BirdhouseSite) {
        setState(site, 0)
        setReadyAt(site, 0)
        invAddOrDrop(objRepo, BirdhouseTrapObjs.clockwork)
    }

    private fun bestCarriedTier(access: ProtectedAccess): BirdhouseTier? =
        BirdhouseTiers.all.asReversed().firstOrNull {
            access.player.inv.contains(it.obj) && access.stat(stats.hunter) >= it.hunterLevel
        }

    private suspend fun selectLogRecipe(
        access: ProtectedAccess,
        requireClockwork: Boolean = true,
    ): BirdhouseRecipe? {
        with(access) {
            val recipes =
                BirdhouseRecipes.entries.filter { player.inv.count(objTypes[it.logs]) > 0 }
            if (
                recipes.isEmpty() ||
                    (requireClockwork && !player.inv.contains(BirdhouseTrapObjs.clockwork))
            ) {
                mes("You don't have a bird house that you can build here.")
                return null
            }
            if (recipes.size == 1) {
                return recipes.single()
            }
            val input =
                skillMulti(
                    title = BUILD_MENU_TITLE,
                    choices = recipes.map(BirdhouseRecipe::output),
                    maxQuantity = 1,
                    initialQuantity = 1,
                    actionMode = BUILD_ACTION_MODE,
                )
            val index = SKILL_MULTI_BUTTONS.indexOfFirst { it.isType(input.component) }
            return recipes.getOrNull(index)
        }
    }

    private fun canPlace(
        access: ProtectedAccess,
        tier: BirdhouseTier,
        sendFailure: Boolean,
    ): Boolean {
        with(access) {
            if (!player.members) {
                if (sendFailure) {
                    mes("You need to be on a members' world to place a bird house.")
                }
                return false
            }
            if (stat(stats.hunter) < tier.hunterLevel) {
                if (sendFailure) {
                    mes("You need a Hunter level of ${tier.hunterLevel} to place this bird house.")
                }
                return false
            }
            return true
        }
    }

    private fun ProtectedAccess.isUnchangedReady(site: BirdhouseSite, display: Int): Boolean =
        state(site) == display && isReady(player.vars, site)

    private fun ProtectedAccess.seedUnits(site: BirdhouseSite, tier: BirdhouseTier): Int =
        when (state(site)) {
            tier.builtState -> 0
            tier.builtState + SEEDED_OFFSET -> player.vars[site.readyAtVarp]
            tier.builtState + ACTIVE_OFFSET -> REQUIRED_SEED_UNITS
            else -> 0
        }

    private fun tier(display: Int): BirdhouseTier? =
        BirdhouseTiers.all.firstOrNull { display in it.builtState..(it.builtState + ACTIVE_OFFSET) }

    private fun ProtectedAccess.setState(site: BirdhouseSite, value: Int) {
        when (site.displayVarp.id) {
            BirdhouseTrapVarps.displayA.id -> displayA = value
            BirdhouseTrapVarps.displayB.id -> displayB = value
            BirdhouseTrapVarps.displayC.id -> displayC = value
            BirdhouseTrapVarps.displayD.id -> displayD = value
        }
    }

    private fun ProtectedAccess.setReadyAt(site: BirdhouseSite, value: Int) {
        when (site.readyAtVarp.id) {
            BirdhouseTrapVarps.readyAtA.id -> readyAtA = value
            BirdhouseTrapVarps.readyAtB.id -> readyAtB = value
            BirdhouseTrapVarps.readyAtC.id -> readyAtC = value
            BirdhouseTrapVarps.readyAtD.id -> readyAtD = value
        }
    }

    public companion object {
        public const val REQUIRED_SEED_UNITS: Int = 10
        public const val MATURATION_SECONDS: Int = 50 * 60
        public const val RAW_BIRD_MEAT_COUNT: Int = 10
        public const val FEATHER_MIN: Int = 30
        public const val FEATHER_STEP: Int = 10
        public const val FEATHER_QUANTITY_COUNT: Int = 4
        public const val SEED_NEST_DENOMINATOR: Int = 256
        public const val DIRECT_BUILD_TICKS: Int = 2
        public const val PREMADE_RESET_DELAY_TICKS: Int = 1
        public const val RAW_RESET_TICKS: Int = DIRECT_BUILD_TICKS + 1
        private const val SEEDED_OFFSET = 1
        private const val ACTIVE_OFFSET = 2
        private const val BUILD_ACTION_MODE = 1
        private const val BUILD_MENU_TITLE = "Which bird house would you like to build?"
        private const val PARTIAL_DISMANTLE_WARNING =
            "Dismantling this bird house will lose the bird house and all seeds inside it."
        private const val EARLY_EMPTY_WARNING =
            "This bird house is not full. Emptying it now will lose the bird house, seeds and loot."
        private val SKILL_MULTI_BUTTONS: List<ComponentType> =
            listOf(
                components.skillmulti_a,
                components.skillmulti_b,
                components.skillmulti_c,
                components.skillmulti_d,
                components.skillmulti_e,
                components.skillmulti_f,
                components.skillmulti_g,
                components.skillmulti_h,
                components.skillmulti_i,
                components.skillmulti_j,
            )

        /** Cache-233 Wiki success interpolation: low=0, high=200, requirement=5. */
        public fun seedNestChanceNumerator(visibleHunterLevel: Int): Int {
            val interpolated = floor(200.0 * (visibleHunterLevel - 1) / 98.0 + 0.5).toInt() + 1
            return interpolated.coerceIn(0, SEED_NEST_DENOMINATOR)
        }
    }
}
