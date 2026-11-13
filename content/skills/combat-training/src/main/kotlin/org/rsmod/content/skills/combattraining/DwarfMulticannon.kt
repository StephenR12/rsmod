package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import java.time.Instant
import org.rsmod.api.combat.commons.npc.inKonarArea
import org.rsmod.api.combat.commons.npc.inCerberusLair
import org.rsmod.api.combat.commons.npc.inSlayerTower
import org.rsmod.api.combat.commons.npc.resolveCombatXpMultiplier
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.*
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.loc.LocLayerConstants

class DwarfMulticannon
@Inject
constructor(
    private val locs: LocRepository,
    private val world: WorldRepository,
    private val combat: CannonCombat,
    private val players: PlayerList,
    private val shops: Shops,
) : PluginScript() {
    private data class Cannon(
        val owner: Long,
        var loc: LocInfo,
        var stage: Int,
        var decay: Int,
        var direction: Int? = null,
    )

    private val cannons = mutableMapOf<CoordGrid, Cannon>()
    private var Player.tile by intVarp(CannonVars.tile)
    private var Player.setup by intVarp(CannonVars.setup)
    private var Player.ornament by intVarp(CannonVars.ornament)
    private var Player.rounds by intVarp(CannonVars.rounds)
    private var Player.granite by intVarp(CannonVars.granite)

    override fun ScriptContext.startup() {
        onOpHeld1(CannonObjs.parts.first()) { assemble(false) }
        onOpHeld1(CannonObjs.ornamented.first()) { assemble(true) }
        for (stages in listOf(CannonLocs.normal, CannonLocs.ornamented)) {
            for (part in stages.take(3)) onOpLoc1(part) {
                owned(it.loc.coords)?.let { cannon -> collect(cannon) }
            }
            val built = stages[3]
            onOpLoc1(built) { owned(it.loc.coords)?.let { cannon -> fire(cannon) } }
            onOpLoc2(built) { owned(it.loc.coords)?.let { cannon -> collect(cannon) } }
            onOpLoc3(built) { owned(it.loc.coords)?.let { cannon -> empty(cannon) } }
            onOpLoc4(built) {
                val amount = countDialog("How many cannonballs do you want to load?")
                owned(it.loc.coords)?.let { cannon -> if (amount > 0) fire(cannon, amount) }
            }
            for (ammo in listOf(CannonObjs.granite, CannonObjs.ball)) onOpLocU(built, ammo) {
                owned(it.loc.coords)?.let { cannon -> fire(cannon, requested = ammo) }
            }
            onOpLoc1(stages[4]) {
                owned(it.loc.coords)?.let { cannon ->
                    if (now() >= cannon.decay) {
                        expire(player, cannon)
                        return@let
                    }
                    player.setup = now()
                    cannon.decay = player.setup + lifetime
                    morph(cannon, stages[3], 3)
                    mes("You repair the cannon.")
                }
            }
            onOpLoc2(stages[4]) { owned(it.loc.coords)?.let { cannon -> collect(cannon) } }
        }
        onPlayerSoftTimer(CannonTimers.tick) { tick(player) }
        onPlayerLogin { if (player.tile != 0) player.softTimer(CannonTimers.tick, 1) }
        onPlayerLogout {
            cannons[CoordGrid(player.tile)]?.takeIf { it.owner == player.uuid }?.direction = null
        }
        for (ammo in listOf(CannonObjs.ball, CannonObjs.granite)) onEvent<
            NpcHitEvents.SecondaryImpact
        >(
            ammo.id
        ) {
            if (hit.isFromPlayer && hit.damage > 0) {
                val source = hit.resolvePlayerSource(players) ?: return@onEvent
                source.statAdvance(stats.ranged, 2.0 * hit.damage * npc.resolveCombatXpMultiplier())
            }
        }
        onOpNpc3(CannonNpcs.nulodion) { if (qualified()) shop() }
        onOpNpc1(CannonNpcs.nulodion) {
            if (!qualified()) return@onOpNpc1
            when (
                choice3(
                    "Can I buy a multicannon for 750,000 coins?",
                    1,
                    "Can I see your parts shop?",
                    2,
                    "I've lost my cannon.",
                    3,
                )
            ) {
                1 -> {
                    val sale =
                        player.invTransaction(inv) {
                            val bag = select(inv)
                            delete(bag, objs.coins.id, 750000)
                            for (item in
                                CannonObjs.parts + listOf(CannonObjs.manual, CannonObjs.mould)) add(
                                bag,
                                item.id,
                                1,
                            )
                        }
                    mes(
                        if (sale.success) "Nulodion hands you the cannon parts, mould and manual."
                        else "You'll need 750,000 coins and space for all six items."
                    )
                }
                2 -> shop()
                3 -> {
                    val active = cannons[CoordGrid(player.tile)]?.takeIf { it.owner == player.uuid }
                    when {
                        player.tile == 0 -> mes("You do not have a cannon for me to replace.")
                        active != null && now() < active.decay ->
                            mes("Your cannon is still set up.")
                        else -> collect(active)
                    }
                }
            }
        }
    }

    private fun ProtectedAccess.qualified(): Boolean {
        if (player.members && player.vars[varps.mcannon] >= 11) return true
        mes("You must complete Dwarf Cannon on a members' world first.")
        return false
    }

    private fun ProtectedAccess.shop() {
        shops.open(
            player,
            "Multicannon parts for sale",
            CannonInvs.shop,
            buyPercentage = 40.0,
            sellPercentage = 107.0,
            changePercentage = 1.0,
        )
    }

    private fun ProtectedAccess.assemble(ornamented: Boolean) {
        if (!qualified()) return
        if (player.tile != 0) {
            mes("You already own a cannon. Speak to Nulodion if you have lost it.")
            return
        }
        val parts = if (ornamented) CannonObjs.ornamented else CannonObjs.parts
        if (!parts.all { it in inv }) {
            mes("You need all four matching parts to assemble the cannon.")
            return
        }
        val centre = player.coords
        val anchor = centre.translate(-1, -1)
        val current = now()
        cannons.entries.removeIf {
            it.value.decay <= current && locs.findExact(it.key, it.value.loc.shape) != it.value.loc
        }
        if (
            prohibited(centre) ||
                cannons.any { (other, cannon) ->
                    cannon.decay > current &&
                        other.level == anchor.level &&
                        other.chebyshevDistance(anchor) <= 2
                } ||
                (-1..1).any { x ->
                    (-1..1).any { z ->
                        val tile = centre.translate(x, z)
                        mapBlocked(tile) ||
                            !lineOfWalk(centre, tile) ||
                            locs.findExact(tile, LocShape.CentrepieceStraight) != null
                    }
                }
        ) {
            mes("You can't set up a cannon here.")
            return
        }
        val transaction =
            player.invTransaction(inv, autoCommit = false) {
                val bag = select(inv)
                for (part in parts) delete(bag, part.id, 1)
            }
        if (!transaction.success) return
        val loc =
            locInfo(anchor, (if (ornamented) CannonLocs.ornamented else CannonLocs.normal).first())
        if (!locs.add(loc, 3500)) {
            mes("There isn't enough space to set up a cannon here.")
            return
        }
        transaction.commitAll()
        cannons[anchor] = Cannon(checkNotNull(player.uuid), loc, 0, current + lifetime)
        player.tile = anchor.packed
        player.setup = current
        player.ornament = if (ornamented) 1 else 0
        player.softTimer(CannonTimers.tick, 1)
        mes("You place the cannon base on the ground.")
    }

    private fun ProtectedAccess.owned(coords: CoordGrid): Cannon? {
        val cannon = cannons[coords]
        if (cannon != null && cannon.owner == player.uuid && coords.packed == player.tile)
            return cannon
        mes("This is not your cannon.")
        return null
    }

    private fun ProtectedAccess.fire(
        cannon: Cannon,
        limit: Int = Int.MAX_VALUE,
        requested: ObjType? = null,
    ) {
        if (cannon.stage != 3) return
        val capacity =
            when {
                player.vars[CannonRewards.elite] >= 2 -> 60
                player.vars[CannonRewards.hard] >= 2 -> 45
                player.vars[CannonRewards.medium] >= 2 -> 35
                else -> 30
            }
        val ammo =
            requested
                ?: if (player.rounds > 0) player.ammo()
                else if (CannonObjs.granite in inv) CannonObjs.granite else CannonObjs.ball
        if (player.rounds > 0 && ammo != player.ammo()) {
            mes("You can't mix different types of cannonball.")
            return
        }
        val count = minOf(capacity - player.rounds, invTotal(inv, ammo), limit)
        if (count > 0 && invDel(inv, ammo, count).success) {
            player.rounds += count
            player.granite = if (ammo == CannonObjs.granite) 1 else 0
            mes("You load the cannon with $count cannonballs.")
        }
        if (player.rounds == 0) mes("You have no cannonballs to load.")
        else if (cannon.direction == null) cannon.direction = 0
    }

    private fun ProtectedAccess.empty(cannon: Cannon) {
        if (player.rounds == 0) {
            mes("Your cannon is empty.")
            return
        }
        if (invAdd(inv, player.ammo(), player.rounds).failure) {
            mes("You need more inventory space.")
            return
        }
        player.rounds = 0
        cannon.direction = null
        mes("You unload your cannon.")
    }

    private fun ProtectedAccess.collect(cannon: Cannon?) {
        val parts = if (player.ornament == 1) CannonObjs.ornamented else CannonObjs.parts
        val returned =
            player.invTransaction(inv, autoCommit = false) {
                val bag = select(inv)
                for (part in parts) add(bag, part.id, 1)
                if (player.rounds > 0) add(bag, player.ammo().id, player.rounds)
            }
        if (!returned.success) {
            mes("You need more inventory space for the cannon and ammunition.")
            return
        }
        if (cannon != null) expire(player, cannon)
        returned.commitAll()
        player.tile = 0
        player.setup = 0
        player.rounds = 0
        player.ornament = 0
        player.granite = 0
        player.clearSoftTimer(CannonTimers.tick)
        mes("You receive your cannon parts.")
    }

    private fun Player.ammo(): ObjType = if (granite == 1) CannonObjs.granite else CannonObjs.ball

    private fun tick(player: Player) {
        val cannon = cannons[CoordGrid(player.tile)]
        if (cannon == null || cannon.owner != player.uuid) {
            player.clearSoftTimer(CannonTimers.tick)
            return
        }
        val stages = if (player.ornament == 1) CannonLocs.ornamented else CannonLocs.normal
        val elapsed = now() - player.setup
        when {
            elapsed >= lifetime -> {
                expire(player, cannon)
                player.mes("Your cannon has decayed. Speak to Nulodion for a replacement.")
            }
            elapsed >= repairAfter ->
                if (cannon.stage != 4) {
                    morph(cannon, stages[4], 4)
                    cannon.direction = null
                    player.mes("Your cannon has broken and requires repair.")
                }
            cannon.stage < 3 -> morph(cannon, stages[cannon.stage + 1], cannon.stage + 1)
            else -> {
                val direction = cannon.direction ?: return
                val shot =
                    combat.shoot(
                        player,
                        cannon.loc.coords.translate(1, 1),
                        direction,
                        player.granite == 1,
                    )
                world.locAnim(
                    cannon.loc,
                    if (shot) CannonSeqs.fire[direction] else CannonSeqs.turn[direction],
                )
                if (shot) player.rounds--
                cannon.direction = if (player.rounds > 0) (direction + 1) % 8 else null
                if (shot && player.rounds == 0) player.mes("Your cannon is out of ammo!")
            }
        }
    }

    private fun morph(cannon: Cannon, type: LocType, stage: Int) {
        val next = locInfo(cannon.loc.coords, type)
        locs.del(cannon.loc, Int.MAX_VALUE)
        val ticks = ((cannon.decay - now()).coerceAtLeast(1) * 5 + 2) / 3
        if (locs.add(next, ticks)) {
            cannon.loc = next
            cannon.stage = stage
        }
    }

    private fun expire(player: Player, cannon: Cannon) {
        locs.del(cannon.loc, Int.MAX_VALUE)
        cannons.remove(cannon.loc.coords, cannon)
        player.clearSoftTimer(CannonTimers.tick)
    }

    private fun locInfo(coords: CoordGrid, type: LocType): LocInfo {
        val shape = LocShape.CentrepieceStraight.id
        return LocInfo(
            LocLayerConstants.of(shape),
            coords,
            LocEntity(type.id, shape, LocAngle.West.id),
        )
    }

    private fun prohibited(c: CoordGrid): Boolean =
        c.inSlayerTower() || c.inCerberusLair() ||
            (c.x in 3584..3647 && c.z in 10176..10303) ||
            (c.x in 1240..1287 && c.z in 10136..10202) ||
            (c.x in 1792..1855 && c.z in 9888..9983) ||
            listOf(1, 13, 26, 30, 41).any { inKonarArea(it, c) } ||
            (c.x in 3136..3263 && c.z in 10048..10239) ||
            (c.x in 3143..3186 && c.z in 3472..3508) ||
            (c.x in 3040..3063 && c.z in 3491..3519) ||
            (c.x in 2992..3071 && c.z in 9792..9855)

    private fun now() = Instant.now().epochSecond.toInt()

    private companion object {
        const val repairAfter = 25 * 60
        const val lifetime = 35 * 60
    }
}
