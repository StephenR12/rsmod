package org.rsmod.content.skills.hunter.falconry

import org.rsmod.api.player.events.HunterCapture
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.hands
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.skills.crafting.hunterpouches.HunterPouchStorage
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

@Singleton
public class Falconry
@Inject
constructor(
    private val pouches: HunterPouchStorage,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val objTypes: ObjTypeList,
    private val clock: MapClock,
    private val random: GameRandom,
) {
    private data class Flight(val prey: Npc, val species: FalconrySpecies, val resolves: Int)

    private data class Capture(
        val owner: Player,
        val npc: Npc,
        val species: FalconrySpecies,
        val expires: Int,
    )

    private val flights = mutableMapOf<Player, Flight>()
    private val captures = mutableMapOf<Npc, Capture>()

    public suspend fun talk(access: ProtectedAccess) {
        with(access) {
            val option =
                menu(
                    "How can I help with your falconry?",
                    "Rent or recover a falcon (500 coins).",
                    "Pay 500,000 coins for permanent falconry.",
                    "Nothing, thanks.",
                )
            if (option == 1) rent(this)
            else if (
                option == 2 &&
                    menu("Cover all future falcon rentals for 500,000 coins?", "Yes.", "No.") == 1
            )
                invest(this)
        }
    }

    public fun invest(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || !inside(player.coords)) return false
            if (vars[FalconryVars.invested] != 0) {
                mes("You have already covered all future falcon rentals.")
                return true
            }
            if (player.inv.count(objTypes[objs.coins]) < INVESTMENT) {
                mes("You need 500,000 coins to cover all future falcon rentals.")
                return false
            }
            val result =
                player.invTransaction(player.inv) {
                    delete(select(player.inv), objs.coins.id, INVESTMENT)
                }
            if (!result.success) return false
            VarPlayerIntMapSetter.set(player, FalconryVars.invested, 1)
            mes("Your future falcon rentals are now free.")
            true
        }

    public fun rent(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || !inside(player.coords)) return false
            if (player.stat(stats.hunter) < 43) {
                mes("You need a Hunter level of 43 to use a falcon.")
                return false
            }
            if (
                player.lefthand != null ||
                    player.hands != null ||
                    (player.righthand != null && !isGlove(player.righthand))
            ) {
                mes("Your weapon, shield and gloves slots must be empty for falconry.")
                return false
            }
            val recover = ownsGlove(player)
            val fee = if (recover || vars[FalconryVars.invested] != 0) 0 else RENTAL
            if (player.inv.count(objTypes[objs.coins]) < fee) {
                mes("You need 500 coins to rent a falcon.")
                return false
            }
            clearPending(player)
            val result =
                player.invTransaction(player.inv, player.worn) {
                    val inventory = select(player.inv)
                    val worn = select(player.worn)
                    if (fee > 0) delete(inventory, objs.coins.id, fee)
                    player.inv.forEachIndexed { slot, obj ->
                        if (isGlove(obj)) delete(inventory, requireNotNull(obj).id, 1, slot = slot)
                    }
                    player.righthand?.let { delete(worn, it.id, 1, slot = Wearpos.RightHand.slot) }
                    add(worn, FalconryObjs.falcon.id, 1, slot = Wearpos.RightHand.slot)
                }
            if (!result.success) return false
            player.softTimer(FalconryTimers.lifecycle, 1)
            mes("Matthias lends you a falcon and a falconer's glove.")
            true
        }

    public fun launch(access: ProtectedAccess, prey: Npc) {
        val species = FalconrySpeciesList.all.firstOrNull { it.npc.id == prey.id } ?: return
        with(access) {
            if (
                !player.members ||
                    !inside(player.coords) ||
                    !inside(prey.coords) ||
                    !prey.isValidTarget()
            )
                return
            if (player.stat(stats.hunter) < species.level) {
                mes("You need a Hunter level of ${species.level} to catch this kebbit.")
                return
            }
            if (
                player.righthand?.id != FalconryObjs.falcon.id ||
                    player.lefthand != null ||
                    player.hands != null
            ) {
                mes("You need to borrow a falcon from Matthias first.")
                return
            }
            if (player in flights || captures.values.any { it.owner === player }) {
                mes("Your falcon is already attending to a kebbit.")
                return
            }
            if (!isWithinDistance(prey, LAUNCH_RANGE)) {
                mes("That kebbit is too far away.")
                return
            }
            faceEntitySquare(prey)
            anim(FalconrySeqs.launch)
            player.righthand = InvObj(FalconryObjs.glove)
            val distance =
                maxOf(
                    kotlin.math.abs(player.coords.x - prey.coords.x),
                    kotlin.math.abs(player.coords.z - prey.coords.z),
                )
            flights[player] = Flight(prey, species, clock.cycle + 1 + distance / 3)
            player.softTimer(FalconryTimers.lifecycle, 1)
        }
    }

    public fun tick(player: Player) {
        if (!player.members || !inside(player.coords)) {
            leave(player)
            return
        }
        val flight = flights[player]
        if (flight != null && clock.cycle >= flight.resolves) {
            flights.remove(player)
            if (
                !flight.prey.isValidTarget() ||
                    !inside(flight.prey.coords) ||
                    !ownsGlove(player) ||
                    player.stat(stats.hunter) < flight.species.level ||
                    random.randomDouble() >= flight.species.chance(player.stat(stats.hunter))
            ) {
                restoreFalcon(player)
                player.mes("Your falcon returns without catching the kebbit.")
            } else {
                val caught = Npc(npcTypes[flight.species.caught], flight.prey.coords)
                caught.noneMode()
                npcs.add(caught, Int.MAX_VALUE)
                npcs.despawn(flight.prey, flight.prey.type.respawnRate)
                captures[caught] =
                    Capture(player, caught, flight.species, clock.cycle + RETRIEVE_TIMEOUT_TICKS)
                player.mes("Your falcon has caught a kebbit. Retrieve it to collect the catch.")
            }
        }
        for (capture in
            captures.values
                .filter {
                    it.owner === player && (clock.cycle >= it.expires || !it.npc.isValidTarget())
                }
                .toList()) {
            captures.remove(capture.npc)
            if (capture.npc.isValidTarget()) npcs.del(capture.npc, Int.MAX_VALUE)
            player.mes(
                "Your falcon has left its prey. You see it heading back toward the falconer."
            )
        }
        if (ownsGlove(player) || player in flights || captures.values.any { it.owner === player })
            player.softTimer(FalconryTimers.lifecycle, 1)
    }

    public fun retrieve(access: ProtectedAccess, caught: Npc) {
        val capture = captures[caught] ?: return
        with(access) {
            if (capture.owner !== player) {
                mes("That isn't your falcon.")
                return
            }
            if (
                !player.members ||
                    !inside(player.coords) ||
                    !isWithinDistance(caught, 1) ||
                    !caught.isValidTarget()
            )
                return
            val remains = RemainsDrop(player, InvObj(objs.bones))
            publish(remains)
            val delivered =
                pouches.deliverCaught(
                    player,
                    buildList {
                        if (remains.remaining > 0) add(InvObj(objs.bones, remains.remaining))
                        add(InvObj(capture.species.fur))
                        capture.species.meat?.let { add(InvObj(it)) }
                    },
                )
            if (!delivered) {
                mes("You don't have enough inventory space to collect the catch.")
                return
            }
            remains.commit()
            captures.remove(caught)
            npcs.del(caught, Int.MAX_VALUE)
            restoreFalcon(player)
            statAdvance(stats.hunter, capture.species.xp)
            publish(HunterCapture(player, capture.species.npc.internalNameValue))
            if (capture.species.npc == FalconryNpcs.dashing)
                player.completeDiaryTask(DiaryTask.CatchDashingKebbit)
        }
    }

    public fun logout(player: Player) {
        clearPending(player)
        if (inside(player.coords)) restoreFalcon(player) else leave(player)
    }

    public fun login(player: Player) {
        if (!inside(player.coords)) leave(player)
        else if (ownsGlove(player)) {
            restoreFalcon(player)
            player.softTimer(FalconryTimers.lifecycle, 1)
        }
    }

    public fun leave(player: Player) {
        clearPending(player)
        player.inv.forEachIndexed { slot, obj -> if (isGlove(obj)) player.inv[slot] = null }
        player.worn.forEachIndexed { slot, obj -> if (isGlove(obj)) player.worn[slot] = null }
        player.clearSoftTimer(FalconryTimers.lifecycle)
    }

    private fun clearPending(player: Player) {
        flights.remove(player)
        for (capture in captures.values.filter { it.owner === player }.toList()) {
            captures.remove(capture.npc)
            if (capture.npc.isValidTarget()) npcs.del(capture.npc, Int.MAX_VALUE)
        }
    }

    private fun restoreFalcon(player: Player) {
        player.worn.forEachIndexed { slot, obj ->
            if (obj?.id == FalconryObjs.glove.id) player.worn[slot] = InvObj(FalconryObjs.falcon)
        }
        player.inv.forEachIndexed { slot, obj ->
            if (obj?.id == FalconryObjs.glove.id) player.inv[slot] = InvObj(FalconryObjs.falcon)
        }
    }

    private fun ownsGlove(player: Player): Boolean =
        player.inv.any(::isGlove) || player.worn.any(::isGlove)

    private fun isGlove(obj: InvObj?): Boolean =
        obj?.id == FalconryObjs.glove.id || obj?.id == FalconryObjs.falcon.id

    public companion object {
        public const val RENTAL: Int = 500
        public const val INVESTMENT: Int = 500_000
        // Travel/search radius and timeout need matching-client timing observation.
        public const val LAUNCH_RANGE: Int = 10
        public const val RETRIEVE_TIMEOUT_TICKS: Int = 100

        public fun inside(coords: CoordGrid): Boolean =
            coords.level == 0 && coords.x in 2355..2403 && coords.z in 3565..3615
    }
}
