package org.rsmod.content.bosses.zalcano

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.hitmark_groups
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.*
import org.rsmod.content.skills.mining.CelestialMiningRing
import org.rsmod.content.skills.mining.CrystalPickaxeLifecycle
import org.rsmod.content.skills.mining.VarrockMining
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeInterval
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeLevel
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.HitType
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class Zalcano
@Inject
internal constructor(
    private val types: ObjTypeList,
    private val npcTypes: NpcTypeList,
    private val locations: LocRepository,
    private val npcs: NpcRepository,
    private val ground: ObjRepository,
    private val world: WorldRepository,
    private val clock: MapClock,
    private val random: GameRandom,
    private val ring: CelestialMiningRing,
    private val crystal: CrystalPickaxeLifecycle,
    private val rewards: PrisonRewards,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    internal class Fight(
        val demon: Npc,
        val started: Int,
        val credits: MutableMap<Player, Contribution> = mutableMapOf(),
    ) {
        var armour = 300
        var health = 1000
        var weakUntil = 0
        var glowing = 3
        var nextAttack = started + 15
        var attack = 0
        var golem: Npc? = null
        val golemHits = mutableMapOf<Player, Int>()
        val falling = mutableMapOf<CoordGrid, Int>()
        var rockExplosion = 0
        var signsUntil = 0
        val signs = mutableListOf<LocInfo>()
    }

    private data class Work(val kind: Kind, val coords: CoordGrid)

    private enum class Kind {
        Rock,
        Furnace,
        Altar,
        Boss,
        Bombard,
        Golem,
    }

    private val members = mutableSetOf<Player>()
    internal var fight: Fight? = null
        private set

    private var respawn = 0

    override fun ScriptContext.startup() {
        onOpHeldU(PrisonObjs.shard, objs.dragon_pickaxe) {
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, PrisonObjs.shard.id, 1)
                    delete(bag, objs.dragon_pickaxe.id, 1)
                    add(bag, PrisonObjs.pick.id, 1)
                }
            if (tx.success) mes("You fit the Zalcano shard to your dragon pickaxe.")
        }
        onOpHeld5(PrisonObjs.pick) {
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, PrisonObjs.pick.id, 1, slot = it.slot)
                    add(bag, objs.dragon_pickaxe.id, 1)
                    add(bag, PrisonObjs.shard.id, 1)
                }
            if (!tx.success) mes("You need an extra inventory space to remove the shard.")
        }
        onOpLoc1(PrisonLocs.platform) {
            if (eligible(player)) telejump(lobby)
            else mes("You must complete Song of the Elves first.")
        }
        onOpLoc1(PrisonLocs.exit) {
            leave(player)
            telejump(surface)
        }
        onOpLoc1(PrisonLocs.gate) {
            if (inFight(player.coords)) {
                leave(player)
                telejump(lobby)
            } else enter(this)
        }
        onOpLoc2(PrisonLocs.gate) {
            mes("There are ${members.count{inFight(it.coords)}} players in Zalcano's prison.")
        }
        onOpNpc1(PrisonNpcs.guide) {
            val xp = choice2("Receive Mining experience.", true, "Receive resource drops.", false)
            vars[PrisonBits.xp] = if (xp) 1 else 0
            mes(
                if (xp) "You will now receive Mining experience."
                else "You will now receive resource drops."
            )
        }
        for (type in listOf(PrisonLocs.normal, PrisonLocs.glowing)) onOpLoc1(type) {
            work(this, Work(Kind.Rock, it.loc.coords))
        }
        for ((kind, loc, item) in
            listOf(
                Triple(Kind.Furnace, PrisonLocs.furnace, PrisonObjs.ore),
                Triple(Kind.Altar, PrisonLocs.altar, PrisonObjs.refined),
            )) {
            onOpLoc1(loc) { work(this, Work(kind, it.loc.coords)) }
            onOpLocU(loc, item) { work(this, Work(kind, it.loc.coords)) }
        }
        for (type in listOf(PrisonNpcs.demon, PrisonNpcs.fallen, PrisonNpcs.golem)) {
            onEvent<NpcHitEvents.ResolveDamage>(type.id) { damage = 0 }
            onModifyNpcHit(type) { hit.damage = 0 }
            onOpNpc1(type) { target(this, it.npc) }
            onApNpc1(type) {
                val range = if (fight?.demon === it.npc && fight?.weakUntil != 0) 1 else 10
                if (isWithinApRange(it.npc, range)) target(this, it.npc)
            }
            onOpNpcU(type, PrisonObjs.imbued) { target(this, it.npc) }
        }
        for (type in listOf(PrisonNpcs.demon, PrisonNpcs.fallen)) onAiTimer(type) { tick(npc) }
        onPlayerQueueWithArgs<Work>(PrisonQueues.work) { work(this, it.args) }
        onPlayerLogin { if (inFight(player.coords) && eligible(player)) members += player }
        onPlayerLogout { leave(player) }
    }

    private fun eligible(p: Player) = p.members && p.vars[varbits.song_of_the_elves] >= 194

    internal fun enter(access: ProtectedAccess): Boolean =
        with(access) {
            if (!eligible(player)) {
                mes("You must complete Song of the Elves first.")
                return false
            }
            telejump(inside)
            members += player
            npcs
                .findAll(ZoneKey.from(centre), 1)
                .firstOrNull { it.id == PrisonNpcs.demon.id }
                ?.let(::tick)
            true
        }

    private fun clearOre(p: Player) {
        val ids = setOf(PrisonObjs.ore.id, PrisonObjs.refined.id, PrisonObjs.imbued.id)
        for (slot in p.inv.indices) if (p.inv[slot]?.id in ids) p.inv[slot] = null
        if (p.righthand?.id == PrisonObjs.imbued.id) {
            p.righthand = null
            p.rebuildAppearance()
        }
    }

    private fun leave(p: Player) {
        members.remove(p)
        clearOre(p)
    }

    private fun rockForms(f: Fight, active: Boolean) {
        for ((index, tile) in rocks.withIndex()) {
            val loc = locations.findAll(tile).firstOrNull { it.id in 36192..36194 } ?: continue
            val target =
                when {
                    !active -> PrisonLocs.depleted
                    index == f.glowing -> PrisonLocs.glowing
                    else -> PrisonLocs.normal
                }
            if (loc.id != target.id) locations.change(loc, target, Int.MAX_VALUE)
        }
    }

    internal fun tick(demon: Npc) {
        val now = clock.cycle
        for (p in members.toList()) if (!inFight(p.coords) || !p.isSlotAssigned) leave(p)
        val f =
            fight
                ?: run {
                    if (members.isEmpty() || now < respawn) return
                    Fight(demon, now).also {
                        it.glowing = if (random.randomBoolean()) 3 else random.of(4)
                        fight = it
                        demon.hitpoints = 300
                        demon.baseHitpointsLvl = 300
                        rockForms(it, true)
                    }
                }
        if (f.demon !== demon) return
        if (members.isEmpty()) {
            end(f, false)
            return
        }
        for ((tile, due) in f.falling.toMap()) if (now >= due) {
            for (p in members) if (p.coords == tile)
                p.queueHit(1, HitType.Typeless, random.of(15..40))
            f.falling.remove(tile)
        }
        if (f.weakUntil > 0) {
            for (p in members) if (bodyDistance(demon, p.coords) == 0)
                p.queueHit(1, HitType.Typeless, random.of(3..10))
            if (now >= f.weakUntil) {
                f.weakUntil = 0
                f.armour = 300
                demon.resetTransmog()
                demon.anim(PrisonSeqs.rise)
                demon.hitpoints = 300
                demon.baseHitpointsLvl = 300
                f.nextAttack = now + 12
                for (p in members) {
                    clearOre(p)
                    if (bodyDistance(demon, p.coords) <= 1) {
                        p.queueHit(1, HitType.Typeless, random.of(1..10))
                        val dx = (p.x - (demon.x + demon.size / 2)).compareTo(0)
                        val dz = (p.z - (demon.z + demon.size / 2)).compareTo(0)
                        val offsetX = if (dx == 0 && dz == 0) 1 else dx
                        val destination =
                            (1..3)
                                .map { p.coords.translate(it * offsetX, it * dz) }
                                .takeWhile { inFight(it) && !collision.isWalkBlocked(it) }
                                .lastOrNull()
                        if (destination != null)
                            PathingEntityCommon.telejump(p, collision, destination)
                        p.actionDelay = maxOf(p.actionDelay, now + 2)
                    }
                }
            }
            return
        }
        if (f.rockExplosion > 0 && now >= f.rockExplosion) {
            for (p in members) if (rocks[f.glowing].chebyshevDistance(p.coords) <= 2)
                p.queueHit(1, HitType.Typeless, random.of(1..28))
            f.glowing = (f.glowing + random.of(1..3)) % 4
            f.rockExplosion = 0
            rockForms(f, true)
        }
        if (now >= f.signsUntil) clearSigns(f)
        else
            for (p in members) {
                if (onSign(f, p, PrisonLocs.red.id)) {
                    p.queueHit(1, HitType.Typeless, (26 - (f.signsUntil - now)).coerceIn(1, 26))
                    p.runEnergy = (p.runEnergy - 100).coerceAtLeast(0)
                    UpdateRun.energy(p, p.runEnergy)
                }
            }
        f.golem?.let { g ->
            if (bodyDistance(demon, g.coords) <= 1) {
                f.armour = (f.armour + (50 * g.hitpoints + 13) / 14).coerceAtMost(300)
                demon.hitpoints = f.armour
                removeGolem(f)
            } else g.walk(demon.coords.translate(-1, 1))
        }
        if (now < f.nextAttack) return
        f.nextAttack = now + 15
        when (f.attack++ % 5) {
            0 -> {
                demon.anim(PrisonSeqs.boulder)
                for (p in members) {
                    f.falling[p.coords] = now + 4
                    world.spotanimMap(PrisonSpots.fall, p.coords)
                }
            }
            1 -> {
                demon.anim(PrisonSeqs.signs)
                clearSigns(f)
                f.signsUntil = now + 20
                val rotation = random.of(4)
                for ((i, tile) in sigils.withIndex()) {
                    val type = if ((i + rotation) % 4 == 0) PrisonLocs.blue else PrisonLocs.red
                    f.signs +=
                        locations.add(tile, type, 25, LocAngle.West, LocShape.CentrepieceStraight)
                }
            }
            2 -> {
                demon.anim(PrisonSeqs.rock)
                f.rockExplosion = now + 4
                world.spotanimMap(PrisonSpots.fall, rocks[f.glowing])
            }
            3 ->
                if (f.golem == null) {
                    demon.anim(PrisonSeqs.golem)
                    val tile = CoordGrid(3021, 6049)
                    val g = Npc(npcTypes[PrisonNpcs.golem], tile)
                    npcs.add(g, 1000)
                    f.golem = g
                    world.spotanimMap(PrisonSpots.golem, tile)
                }
            else -> for (p in members) p.queueHit(1, HitType.Typeless, random.of(1..4))
        }
    }

    private fun bodyDistance(n: Npc, c: CoordGrid) =
        maxOf(n.x - c.x, c.x - (n.x + n.size - 1), n.z - c.z, c.z - (n.z + n.size - 1), 0)

    private fun onSign(f: Fight, p: Player, id: Int) =
        f.signs.any {
            it.id == id &&
                p.coords.x in it.coords.x..it.coords.x + 2 &&
                p.coords.z in it.coords.z..it.coords.z + 2
        }

    private fun clearSigns(f: Fight) {
        for (loc in f.signs) locations.del(loc, Int.MAX_VALUE)
        f.signs.clear()
    }

    private fun removeGolem(f: Fight) {
        f.golem?.let { if (it.isSlotAssigned) npcs.del(it, Int.MAX_VALUE) }
        f.golem = null
        f.golemHits.clear()
    }

    private fun end(f: Fight, victory: Boolean) {
        if (victory) {
            f.demon.anim(PrisonSeqs.death)
            rewards.distribute(f.credits)
        }
        for (p in members) clearOre(p)
        clearSigns(f)
        removeGolem(f)
        rockForms(f, false)
        f.demon.resetTransmog()
        f.demon.hitpoints = 300
        f.demon.baseHitpointsLvl = 300
        fight = null
        respawn = clock.cycle + if (victory) 62 else 0
        if (victory) npcs.hide(f.demon, 62)
    }

    private fun target(a: ProtectedAccess, n: Npc) {
        val f = fight ?: return
        val kind =
            when {
                n === f.golem -> Kind.Golem
                n !== f.demon -> return
                f.weakUntil > 0 -> Kind.Boss
                else -> Kind.Bombard
            }
        work(a, Work(kind, n.coords))
    }

    private fun work(a: ProtectedAccess, w: Work): Unit =
        with(a) {
            val f = fight ?: return
            if (player !in members || !inFight(player.coords) || player.actionDelay > clock.cycle)
                return
            val dist = player.coords.chebyshevDistance(w.coords)
            val cycles =
                when (w.kind) {
                    Kind.Rock -> {
                        val index = rocks.indexOf(w.coords)
                        if (index < 0 || dist > 3) return
                        val pick =
                            Mining.findPickaxe(player, types)
                                ?: run {
                                    mes("You need a pickaxe you can use.")
                                    return
                                }
                        anim(types[pick].pickaxeAnim)
                        val chance = if (index == f.glowing) 85 else 20
                        if (
                            random.of(100) <
                                (chance +
                                        (player.stat(stats.mining) + ring.invisibleBoost(player) -
                                            70) / 4)
                                    .coerceIn(5, 99)
                        ) {
                            if (invAdd(inv, PrisonObjs.ore).failure) {
                                mes("Your inventory is full.")
                                return
                            }
                            var extra = if (VarrockMining.roll(player, 1, random)) 1 else 0
                            if (ring.rollExtra(this)) extra++
                            if (
                                (objs.mining_skillcape in worn ||
                                    objs.mining_skillcape_t in worn) && random.randomBoolean(10)
                            )
                                extra++
                            val added =
                                if (extra > 0)
                                    invAdd(inv, PrisonObjs.ore, extra, strict = false).completed()
                                else 0
                            player.statAdvance(stats.mining, 35.0 * (1 + added))
                            crystal.consumeCharge(this, pick)
                            if (random.randomBoolean(65))
                                invAdd(inv, objs.crystal_shard, random.of(1..3), strict = false)
                        }
                        types[pick].pickaxeInterval.coerceIn(2, 8)
                    }
                    Kind.Furnace,
                    Kind.Altar -> {
                        val furnace = w.kind == Kind.Furnace
                        val station = if (furnace) PrisonLocs.furnace else PrisonLocs.altar
                        if (dist > 4 || locations.findExact(w.coords, station) == null) return
                        val before = if (furnace) PrisonObjs.ore else PrisonObjs.refined
                        val after = if (furnace) PrisonObjs.refined else PrisonObjs.imbued
                        if (before !in inv) return
                        val tx =
                            player.invTransaction(inv) {
                                val bag = select(inv)
                                delete(bag, before.id, 1)
                                add(bag, after.id, 1)
                            }
                        if (!tx.success) return
                        anim(if (furnace) PrisonSeqs.refine else PrisonSeqs.imbue)
                        player.statAdvance(
                            if (furnace) stats.smithing else stats.runecrafting,
                            if (furnace) 10.0 else 5.0,
                        )
                        1
                    }
                    Kind.Boss -> {
                        if (f.weakUntil == 0 || bodyDistance(f.demon, player.coords) > 1) return
                        val pick =
                            Mining.findPickaxe(player, types)
                                ?: run {
                                    mes("You need a pickaxe you can use.")
                                    return
                                }
                        anim(types[pick].pickaxeAnim)
                        val level = player.stat(stats.mining) + ring.invisibleBoost(player)
                        val max =
                            30 +
                                listOf(75, 85, 95, 105).count { level >= it } +
                                ((types[pick].pickaxeLevel - 40) / 15).coerceAtLeast(0)
                        val damage = minOf(f.health, random.of(1..max))
                        f.credits.getOrPut(player) { Contribution() }.health += damage
                        f.health -= damage
                        showHit(f.demon, player, damage, false)
                        player.statAdvance(stats.mining, 35.2)
                        if (f.health == 0) {
                            end(f, true)
                            return
                        }
                        f.demon.hitpoints = f.health
                        4
                    }
                    Kind.Bombard,
                    Kind.Golem -> {
                        val target = if (w.kind == Kind.Golem) f.golem ?: return else f.demon
                        if (f.weakUntil != 0 || bodyDistance(target, player.coords) > 10) return
                        if (PrisonObjs.imbued in inv) {
                            if (invDel(inv, PrisonObjs.imbued, 1).failure) return
                        } else {
                            val held = player.righthand
                            if (held?.id != PrisonObjs.imbued.id) {
                                mes("You need imbued tephra to break the stone armour.")
                                return
                            }
                            player.righthand =
                                if (held.count == 1) null
                                else InvObj(PrisonObjs.imbued, held.count - 1)
                            if (held.count == 1) rebuildAppearance()
                        }
                        player.actionDelay = clock.cycle + 3
                        val sum = player.stat(stats.smithing) + player.stat(stats.runecrafting)
                        var max =
                            5 + sum / 14 + if (sum % 14 > 0 && random.of(14) < sum % 14) 1 else 0
                        val blue = onSign(f, player, PrisonLocs.blue.id)
                        if (blue) max = max * 3 / 2
                        val damage = random.of(0..max)
                        anim(PrisonSeqs.throwing)
                        world.projAnim(player, target, PrisonSpots.missile, projanims.thrown)
                        target.spotanim(PrisonSpots.shot)
                        if (target === f.demon) {
                            val dealt = minOf(damage, f.armour)
                            showHit(target, player, dealt, true)
                            f.armour -= dealt
                            f.credits.getOrPut(player) { Contribution() }.armour += dealt
                            if (f.armour == 0) {
                                weaken(f)
                                return
                            }
                            target.hitpoints = f.armour
                        } else {
                            val dealt = minOf(damage, target.hitpoints)
                            showHit(target, player, dealt, false)
                            target.hitpoints -= dealt
                            f.golemHits[player] = (f.golemHits[player] ?: 0) + dealt
                            if (target.hitpoints == 0) {
                                val owner = f.golemHits.maxBy { it.value }.key
                                ground.add(
                                    PrisonObjs.imbued,
                                    target.coords,
                                    100,
                                    owner,
                                    random.of(16..24),
                                )
                                removeGolem(f)
                                return
                            }
                        }
                        3
                    }
                }
            player.actionDelay = clock.cycle + cycles
            weakQueue(PrisonQueues.work, cycles + 1, w)
        }

    private fun showHit(n: Npc, p: Player, damage: Int, armour: Boolean) {
        val marks =
            when {
                damage == 0 -> hitmark_groups.zero_damage
                armour -> hitmark_groups.zalcano_armour_damage
                else -> hitmark_groups.regular_damage
            }
        n.showHitmark(
            Hitmark.fromPlayerSource(
                marks.lit.id,
                marks.lit.id,
                marks.tint?.id,
                damage,
                0,
                p.slotId,
            )
        )
        val bar = n.visHeadbar(params.headbar)
        val fill =
            (n.hitpoints - damage).coerceAtLeast(0) * bar.segments /
                n.baseHitpointsLvl.coerceAtLeast(1)
        n.showHeadbar(Headbar.fromPlayerSource(bar.id, bar.id, fill, fill, 0, 0, p.slotId))
    }

    private fun weaken(f: Fight) {
        f.demon.anim(PrisonSeqs.fall)
        f.demon.transmog(npcTypes[PrisonNpcs.fallen], Int.MAX_VALUE)
        f.demon.baseHitpointsLvl = 1000
        f.demon.hitpoints = f.health
        f.weakUntil = clock.cycle + random.of(20..27)
        clearSigns(f)
        removeGolem(f)
        f.rockExplosion = 0
    }

    private companion object {
        val lobby = CoordGrid(3033, 6066)
        val surface = CoordGrid(3281, 6057)
        val inside = CoordGrid(3033, 6060)
        val centre = CoordGrid(3031, 6048)
        val sigils =
            listOf(
                CoordGrid(3026, 6045),
                CoordGrid(3030, 6041),
                CoordGrid(3035, 6042),
                CoordGrid(3040, 6045),
                CoordGrid(3039, 6051),
                CoordGrid(3036, 6055),
                CoordGrid(3029, 6055),
                CoordGrid(3025, 6051),
            )
    }
}
