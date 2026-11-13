package org.rsmod.content.skills.hunter.tracking

import org.rsmod.api.player.events.HunterCapture
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ring
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.skills.crafting.hunterpouches.HunterPouchStorage
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public data class TrackingStep(val edge: TrackingEdge, val from: Int, val to: Int)

public data class KebbitTrail(
    val network: TrackingNetwork,
    val steps: List<TrackingStep>,
    val destination: TrackingSite,
    var revealed: Int,
)

/**
 * Individual native trails. Unknown nonpolar catch odds and polar tunnel links are explicit models.
 */
@Singleton
public class KebbitTracking
@Inject
constructor(
    private val random: GameRandom,
    private val pouches: HunterPouchStorage,
    private val locs: LocRepository,
) : PluginScript() {
    private val trails = WeakHashMap<Player, KebbitTrail>()

    override fun ScriptContext.startup() {
        val starts = TrackingNetworks.all.flatMap { it.starts }.map { it.type }.distinctBy { it.id }
        val clues =
            TrackingNetworks.all
                .flatMap { it.edges }
                .flatMap { it.clues }
                .map { it.type }
                .distinctBy { it.id }
        val ends = TrackingNetworks.all.flatMap { it.ends }.map { it.type }.distinctBy { it.id }
        for (type in starts) onOpLoc1(type) { start(this, it.loc) }
        for (type in clues) onOpLoc1(type) { inspect(this, it.loc) }
        for (type in ends) {
            onOpLoc1(type) { search(this, it.loc) }
            onOpLoc2(type) { catch(this, it.loc) }
        }
        onOpHeld3(TrackingObjs.pursuit) { checkRing(player, player.inv[it.slot]) }
        onOpWorn2(TrackingObjs.pursuit) { checkRing(player, player.ring) }
        onOpHeld4(TrackingObjs.pursuit) {
            if (player.inv[it.slot]?.id == TrackingObjs.pursuit.id) {
                player.inv[it.slot] = null
                mes("You break the ring of pursuit.")
            }
        }
        onOpLoc1(TrackingLocs.polarUp) {
            if (player.members) telejump(it.loc.coords.translate(0, 3, 1))
        }
        onOpLoc1(TrackingLocs.polarDown) {
            if (player.members) telejump(it.loc.coords.translate(0, -2, -1))
        }
        onPlayerLogout { clear(player) }
        onEvent<PlayerDeathEvent.Completed> { clear(player) }
    }

    public fun trail(player: Player): KebbitTrail? = trails[player]

    public fun start(access: ProtectedAccess, source: BoundLocInfo): Boolean =
        with(access) {
            val network =
                TrackingNetworks.all.firstOrNull { n ->
                    n.starts.any { it.coords == source.coords && it.type.id == source.id }
                } ?: return false
            if (!player.members || player.stat(stats.hunter) < network.level) {
                mes("You need ${network.level} Hunter to track this creature.")
                return false
            }
            if (!near(this, source)) return false
            val site = network.starts.first { it.coords == source.coords }
            val steps = plan(network, site.node) ?: return false
            clear(player)
            val destination = network.ends.first { it.node == steps.last().to }
            val ring = player.ring
            val revealed =
                if (ring?.id == TrackingObjs.pursuit.id) {
                    val remaining = ringCharges(ring) - 1
                    player.ring = if (remaining == 0) null else ring.copy(vars = remaining)
                    if (remaining == 0) mes("Your ring of pursuit crumbles to dust.")
                    else mes("Your ring of pursuit reveals the creature's entire trail.")
                    steps.size
                } else 1
            val trail = KebbitTrail(network, steps, destination, revealed)
            trails[player] = trail
            for (step in steps.take(revealed)) show(player, step)
            mes("You find some tracks leading away from the burrow.")
            true
        }

    public fun inspect(access: ProtectedAccess, source: BoundLocInfo): Boolean =
        with(access) {
            val trail =
                trails[player]
                    ?: run {
                        mes("You find no tracks. Try inspecting a burrow first.")
                        return false
                    }
            if (!player.members || !near(this, source)) return false
            val next = trail.steps.getOrNull(trail.revealed)
            if (
                next == null ||
                    next.edge.clues.none { it.coords == source.coords && it.type.id == source.id }
            ) {
                mes("You find no further tracks here.")
                return false
            }
            show(player, next)
            trail.revealed++
            mes("You discover more tracks. You can follow them to find the creature.")
            true
        }

    public fun search(access: ProtectedAccess, source: BoundLocInfo): Boolean =
        with(access) {
            val trail = trails[player]
            val found =
                trail != null &&
                    trail.revealed == trail.steps.size &&
                    trail.destination.coords == source.coords &&
                    trail.destination.type.id == source.id
            mes(
                if (found) "It looks like there might be a creature hiding here."
                else "You find no creature hiding here."
            )
            found
        }

    public suspend fun catch(access: ProtectedAccess, source: BoundLocInfo): Boolean =
        with(access) {
            val trail = trails[player] ?: return false
            if (
                !player.members ||
                    trail.revealed != trail.steps.size ||
                    trail.destination.coords != source.coords ||
                    trail.destination.type.id != source.id ||
                    !near(this, source)
            )
                return false
            if (player.righthand?.id != TrackingObjs.wand.id) {
                mes("You need to wield a noose wand. The creature escapes!")
                clear(player)
                return false
            }
            if (player.stat(stats.hunter) < trail.network.level) return false
            anim(TrackingSeqs.catch)
            gatheringDelay(1)
            if (
                trails[player] !== trail ||
                    !near(this, source) ||
                    player.righthand?.id != TrackingObjs.wand.id ||
                    player.stat(stats.hunter) < trail.network.level ||
                    !player.members
            )
                return false
            val chance =
                if (trail.network.name == "polar")
                    SkillingSuccessRate.successRate(238, 1080, player.stat(stats.hunter), 99)
                        .coerceAtMost(1.0)
                else
                    (0.93 + 0.035 * (player.stat(stats.hunter) - trail.network.level)).coerceAtMost(
                        1.0
                    )
            if (random.randomDouble() >= chance) {
                clear(player)
                mes("The creature escapes your noose!")
                return false
            }
            val remains = RemainsDrop(player, InvObj(objs.bones))
            publish(remains)
            val loot = buildList {
                if (remains.remaining > 0) add(InvObj(objs.bones, remains.remaining))
                add(InvObj(objs.raw_beast_meat))
                add(InvObj(trail.network.reward))
            }
            if (!pouches.deliverCaught(player, loot)) {
                mes("You don't have enough inventory space to collect the creature.")
                return false
            }
            remains.commit()
            statAdvance(stats.hunter, trail.network.xp)
            publish(HunterCapture(player, "tracking_"+trail.network.name))
            clear(player)
            mes("You catch the creature.")
            true
        }

    public fun clear(player: Player) {
        trails.remove(player)
        for (bit in TrackingBits.all.values) if (player.vars[bit] != 0)
            VarPlayerIntMapSetter.set(player, bit, 0)
    }

    private fun near(access: ProtectedAccess, source: BoundLocInfo): Boolean =
        access.isWithinDistance(source, 1) && locs.findAll(source.coords).any { it.id == source.id }

    private fun show(player: Player, step: TrackingStep) {
        VarPlayerIntMapSetter.set(player, step.edge.bit, if (step.from == step.edge.a) 3 else 4)
    }

    private fun plan(network: TrackingNetwork, start: Int): List<TrackingStep>? {
        fun walk(node: Int, steps: List<TrackingStep>, visits: Map<Int, Int>): List<TrackingStep>? {
            if (steps.size >= 3 && network.ends.any { it.node == node }) return steps
            val choices =
                network.edges.filter { edge ->
                    (edge.a == node || edge.b == node) &&
                        steps.none { it.edge === edge } &&
                        (visits[if (edge.a == node) edge.b else edge.a] ?: 0) < 2
                }
            if (choices.isEmpty()) return null
            val offset = random.of(0..choices.lastIndex)
            for (i in choices.indices) {
                val edge = choices[(i + offset) % choices.size]
                val next = if (edge.a == node) edge.b else edge.a
                val found =
                    walk(
                        next,
                        steps + TrackingStep(edge, node, next),
                        visits + (next to ((visits[next] ?: 0) + 1)),
                    )
                if (found != null) return found
            }
            return null
        }
        return walk(start, emptyList(), mapOf(start to 1))
    }

    public fun ringCharges(obj: InvObj): Int = (obj.vars and 15).takeIf { it in 1..10 } ?: 10

    private fun checkRing(player: Player, ring: InvObj?) {
        if (ring != null)
            player.mes("Your ring of pursuit has ${ringCharges(ring)} charges remaining.")
    }
}
