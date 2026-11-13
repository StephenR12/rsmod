package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueVessels
import org.rsmod.content.skills.pets.SkillPetRates
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.hands
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
public class AerialFishing @Inject constructor(private val vessels: ClueVessels, private val petRates: SkillPetRates, private val objTypes: ObjTypeList, private val clock: MapClock, private val random: GameRandom, private val objRepo: ObjRepository, private val collision: CollisionFlagMap) {
    private data class Flight(val resolves: Int, val chunks: Boolean)
    private val flights = mutableMapOf<Player, Flight>()

    public fun borrow(access: ProtectedAccess): Boolean = with(access) {
        if (!player.members || !inside(player.coords)) return false
        if (player.stat(stats.fishing) < 43 || player.stat(stats.hunter) < 35) { mes("You need Fishing level 43 and Hunter level 35 to borrow a cormorant."); return false }
        if (player.lefthand != null || player.hands != null || (player.righthand != null && !isGlove(player.righthand))) { mes("Your weapon, shield and gloves slots must be empty."); return false }
        flights.remove(player)
        val result = player.invTransaction(player.inv, player.worn) {
            val inventory = select(player.inv)
            val worn = select(player.worn)
            player.inv.forEachIndexed { slot, obj -> if (isGlove(obj)) delete(inventory, requireNotNull(obj).id, 1, slot = slot) }
            player.righthand?.let { delete(worn, it.id, 1, slot = Wearpos.RightHand.slot) }
            add(worn, AerialObjs.bird.id, 1, slot = Wearpos.RightHand.slot)
        }
        if (!result.success) return false
        player.softTimer(AerialTimers.lifecycle, 1)
        mes("Alry lends you a cormorant and a cormorant's glove.")
        true
    }

    public fun launch(access: ProtectedAccess, pool: Npc) {
        with(access) {
            if (!player.members || !inside(player.coords) || pool.id != AerialNpcs.pool.id) return
            if (player.righthand?.id != AerialObjs.bird.id || player.lefthand != null || player.hands != null) { mes("You need to borrow a cormorant from Alry first."); return }
            if (player in flights) return
            if (player.inv.isFull()) { mes("You don't have enough inventory space to do that."); return }
            val chunks = AerialObjs.chunks in player.inv
            if (!chunks && AerialObjs.worm !in player.inv) { mes("You need fish chunks or a king worm to feed the cormorant."); return }
            if (!isWithinDistance(pool, LAUNCH_RANGE)) return
            faceEntitySquare(pool)
            anim(AerialSequences.launch)
            player.righthand = InvObj(AerialObjs.glove)
            val distance = maxOf(kotlin.math.abs(player.coords.x - pool.coords.x), kotlin.math.abs(player.coords.z - pool.coords.z))
            flights[player] = Flight(clock.cycle + 1 + (distance - 1).coerceAtLeast(0) / 3, chunks)
            player.softTimer(AerialTimers.lifecycle, 1)
        }
    }

    public fun tick(player: Player) {
        if (!player.members || !inside(player.coords)) { leave(player); return }
        val flight = flights[player]
        if (flight != null && clock.cycle >= flight.resolves) {
            flights.remove(player)
            val equipped = player.righthand?.id == AerialObjs.glove.id && player.lefthand == null && player.hands == null
            restoreBird(player)
            if (equipped) finish(player, flight)
        }
        if (ownsGlove(player) || player in flights) player.softTimer(AerialTimers.lifecycle, 1)
    }

    private fun finish(player: Player, flight: Flight) {
        if (player.inv.isFull()) { player.mes("The cormorant throws its catch back into the water."); return }
        val bait = if (flight.chunks) AerialObjs.chunks else AerialObjs.worm
        if (bait !in player.inv) return
        val fishing = player.stat(stats.fishing)
        val hunter = player.stat(stats.hunter)
        val fish = AerialFishTable.select(fishing, hunter, random.of(AerialFishTable.bound(fishing, hunter)))
        val golden = random.of(GOLDEN_TENCH_DENOMINATOR) == 0
        val pearl = random.of(AerialFishTable.pearlDenominator(fishing, hunter)) == 0
        val previous = player.vars[AerialVars.chunkCatches].coerceIn(0, 3)
        val next = previous + 1
        val consume = !flight.chunks || next >= 4 || (next == 3 && random.of(2) == 0)
        val output = if (golden) AerialObjs.goldenTench else fish.obj
        val occupied = petRates.occupiedBy(player,output.id)
        val result = player.invTransaction(player.inv) {
            val inventory = select(player.inv)
            if (consume) delete(inventory, bait.id, 1)
            add(inventory, output.id, 1)
        }
        if (!result.success) return
        if (flight.chunks) VarPlayerIntMapSetter.set(player, AerialVars.chunkCatches, if (consume) 0 else next)
        petRates.roll(player,stats.fishing,13320,636_833,petRates.newResourceSlot(player,output.id,occupied))
        vessels.bottles(player,fishing,636_833)
        player.statAdvance(stats.fishing, fish.fishingXp * FishingXp.multiplier(player))
        player.statAdvance(stats.hunter, fish.hunterXp)
        if (pearl) player.invAddOrDrop(objRepo, AerialObjs.pearl)
        if (golden) player.mes("<col=ff0000>The cormorant has brought you a very strange tench.</col>")
        if (output == objs.raw_bluegill) player.completeDiaryTask(DiaryTask.CatchLakeMolchBluegill)
    }

    public fun cut(access: ProtectedAccess, fish: AerialFish) = with(access) {
        if (!player.members || objs.knife !in player.inv || fish.obj !in player.inv) return
        val result = player.invTransaction(player.inv) {
            val inventory = select(player.inv)
            delete(inventory, fish.obj.id, 1)
            add(inventory, AerialObjs.chunks.id, 1)
        }
        if (result.success) statAdvance(stats.cooking, fish.cookingXp)
    }

    public fun exchangeTench(access: ProtectedAccess): Boolean = with(access) {
        if (!player.members || AerialObjs.goldenTench !in player.inv) return false
        val result = player.invTransaction(player.inv) {
            val inventory = select(player.inv)
            delete(inventory, AerialObjs.goldenTench.id, 1)
            add(inventory, AerialObjs.pearl.id, 100)
        }
        if (result.success) mes("Alry exchanges your golden tench for 100 Molch pearls.")
        result.success
    }

    public suspend fun talk(access: ProtectedAccess) = with(access) {
        when (menu("How can Alry help?", "Borrow a cormorant.", "Spend Molch pearls.", "Exchange a golden tench for 100 pearls.", "Nothing, thanks.")) {
            1 -> borrow(this)
            2 -> shop(this)
            3 -> exchangeTench(this)
        }
    }

    public suspend fun shop(access: ProtectedAccess): Unit = with(access) {
        if (!player.members) return
        val choices = when (menu("Spend Molch pearls", "Fishing rods", "Angler's outfit", "Fish sack (1,000 pearls)", "Cancel")) {
            1 -> AerialRewards.rods
            2 -> AerialRewards.outfit
            3 -> { buy(this, AerialRewards.sack); return }
            else -> return
        }
        val labels = choices.map { "${objTypes[it.obj].name} (${it.pearls} pearls)" } + "Cancel"
        val selected = menu("Choose a reward", *labels.toTypedArray())
        choices.getOrNull(selected - 1)?.let { buy(this, it) }
        Unit
    }

    public fun buy(access: ProtectedAccess, reward: AerialReward): Boolean = with(access) {
        if (!player.members || (reward !in AerialRewards.rods && reward !in AerialRewards.outfit && reward != AerialRewards.sack)) return false
        if (reward.barbarianTraining && vars[varbits.brut_fishing_r] < 1) { mes("You must begin Otto's heavy-rod fishing training before buying that rod."); return false }
        if (player.inv.count(objTypes[AerialObjs.pearl]) < reward.pearls) { mes("You don't have enough Molch pearls."); return false }
        val result = player.invTransaction(player.inv) {
            val inventory = select(player.inv)
            delete(inventory, AerialObjs.pearl.id, reward.pearls)
            add(inventory, reward.obj.id, 1)
        }
        if (!result.success) mes("You don't have enough inventory space for that reward.")
        result.success
    }

    public fun movePool(pool: Npc) {
        val candidates = AerialPoolPositions.all.filter { it != pool.coords && collision.isZoneValid(it) }
        if (candidates.isNotEmpty()) PathingEntityCommon.telejump(pool, collision, candidates[random.of(candidates.size)])
        pool.aiTimer(random.of(3..10))
    }

    public fun logout(player: Player) { flights.remove(player); if (inside(player.coords)) restoreBird(player) else leave(player) }
    public fun login(player: Player) { if (!inside(player.coords)) leave(player) else if (ownsGlove(player)) { restoreBird(player); player.softTimer(AerialTimers.lifecycle, 1) } }
    public fun leave(player: Player) {
        flights.remove(player)
        player.inv.forEachIndexed { slot, obj -> if (isGlove(obj)) player.inv[slot] = null }
        player.worn.forEachIndexed { slot, obj -> if (isGlove(obj)) player.worn[slot] = null }
        player.clearSoftTimer(AerialTimers.lifecycle)
    }
    private fun restoreBird(player: Player) {
        player.inv.forEachIndexed { slot, obj -> if (obj?.id == AerialObjs.glove.id) player.inv[slot] = InvObj(AerialObjs.bird) }
        player.worn.forEachIndexed { slot, obj -> if (obj?.id == AerialObjs.glove.id) player.worn[slot] = InvObj(AerialObjs.bird) }
    }
    private fun ownsGlove(player: Player): Boolean = player.inv.any(::isGlove) || player.worn.any(::isGlove)
    private fun isGlove(obj: InvObj?): Boolean = obj?.id == AerialObjs.glove.id || obj?.id == AerialObjs.bird.id

    public companion object {
        public const val GOLDEN_TENCH_DENOMINATOR: Int = 20_000
        public const val LAUNCH_RANGE: Int = 20
        public fun inside(c: CoordGrid): Boolean = c.level == 0 && c.x in 1350..1385 && c.z in 3620..3645
    }
}
