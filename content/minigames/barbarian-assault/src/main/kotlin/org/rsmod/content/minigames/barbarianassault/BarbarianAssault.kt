package org.rsmod.content.minigames.barbarianassault

import jakarta.inject.Inject
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.events.*
import org.rsmod.api.player.minigames.completedBarbarianAssaultTutorial
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.ui.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.*
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.*
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.obj.Obj
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

private val lobby = CoordGrid(2593, 5261)
private val outside = CoordGrid(2534, 3571)
private val partners = listOf(Role.Collector, Role.Attacker, Role.Healer, Role.Defender)
private val words =
    listOf(
        listOf("Controlled", "Accurate", "Aggressive", "Defensive"),
        listOf("Green", "Red", "Blue"),
        listOf("Crackers", "Tofu", "Worms"),
        listOf("Tofu", "Worms", "Meat"),
    )

private data class Seat(val wave: Int, val role: Role, val door: CoordGrid)

private data class Lure(val player: Player, val trap: CoordGrid, val bait: ObjType)

private data class Poison(val source: Player, var budget: Int)

private data class Wave(
    val number: Int,
    val region: Region,
    val team: Map<Player, Role>,
    val began: Int,
) {
    val rankIndex = if (number == 10) 6 else number - 1
    val landscape = if (number == 10) 84 else 85
    val monsters = linkedMapOf<Npc, Penance>()
    val reserve = IntArray(4)
    val deaths = IntArray(4)
    val call = IntArray(4)
    val loaded = IntArray(4)
    val lured = mutableMapOf<Npc, Lure>()
    val poisoned = mutableMapOf<Npc, Poison>()
    val ground = mutableSetOf<Obj>()
    val trapUses = mutableMapOf<CoordGrid, Int>()
    var wrongStyle = 0
    var wrongPoison = 0
    var escaped = 0
    var eggs = 0
    var healed = 0
    var queenSpawnAt = Int.MAX_VALUE
    var queenSpawned = false

    fun tile(x: Int, z: Int, level: Int = 0): CoordGrid =
        region.normal[level, 29, landscape, x - 1856, z - 5440]
}

private val initial =
    listOf(
        listOf(2, 2, 2, 2),
        listOf(2, 2, 3, 3),
        listOf(5, 2, 3, 2),
        listOf(5, 3, 3, 3),
        listOf(3, 4, 5, 4),
        listOf(5, 4, 5, 4),
        listOf(5, 5, 6, 4),
        listOf(7, 5, 5, 5),
        listOf(6, 5, 7, 6),
        listOf(5, 5, 6, 4),
    )
private val reserves =
    listOf(
        listOf(2, 0, 2, 0),
        listOf(3, 1, 1, 0),
        listOf(0, 2, 3, 1),
        listOf(1, 1, 3, 1),
        listOf(3, 1, 1, 1),
        listOf(1, 2, 2, 2),
        listOf(2, 1, 1, 3),
        listOf(0, 2, 3, 2),
        listOf(2, 4, 1, 2),
        listOf(2, 1, 1, 3),
    )

public class BarbarianAssault
@Inject
constructor(
    private val locations: org.rsmod.api.repo.loc.LocRepository,
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val ground: ObjRepository,
    private val npcTypes: NpcTypeList,
    private val items: ObjTypeList,
    private val players: PlayerList,
    private val clock: MapClock,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
    private val events: EventBus,
    private val attacks: AiPlayerInteractions,
    private val styles: AttackStyles,
    private val modifier: NpcHitModifier,
) : PluginScript() {
    private val waiting = linkedMapOf<Player, Seat>()
    private val playing = mutableMapOf<Player, Wave>()
    private val owners = mutableMapOf<Npc, Wave>()
    private var ticked = -1

    override fun ScriptContext.startup() {
        onOpLoc1(AssaultLocs.entrance) {
            if (!player.members || !player.completedBarbarianAssaultTutorial()) {
                mes("Speak to Captain Cain before entering.")
                return@onOpLoc1
            }
            telejump(lobby)
        }
        onOpLoc1(AssaultLocs.exit) {
            waiting.remove(player)
            telejump(outside)
        }
        onOpLoc1(AssaultLocs.gameExit) {
            playing[player]?.let {
                if (choice2("Leave the wave.", true, "Stay here.", false)) finish(it, false)
            }
        }
        for ((i, door) in AssaultLocs.doors.withIndex()) onOpLoc1(door) {
            join(i + 1, it.loc.coords)
        }
        onOpLoc1(AssaultLocs.quick) { join(player.nextWave(), it.loc.coords) }
        onOpLoc5(AssaultLocs.doors.first()) {
            if (choice2("Reset to wave 1.", true, "Keep my current wave.", false)) {
                waiting.remove(player)
                vars[AssaultVars.wave] = 1
                telejump(lobby)
            }
        }
        for ((kind, levels) in AssaultNpcs.monsters.withIndex()) for (type in levels) {
            bindCombat(type)
            if (kind == Penance.Healer.ordinal)
                for ((i, food) in AssaultGear.poison.withIndex()) onOpNpcU(type, food) {
                    poison(it.npc, i)
                }
        }
        bindCombat(AssaultNpcs.queen)
        onEvent<NpcKillEvents.Credited> {
            val game = owners[npc] ?: return@onEvent
            if (player in game.team) killed(game, npc)
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            val game = playing[player] ?: return@onEvent
            finish(game, false)
            safeDestination = lobby
        }
        onEvent<PlayerCombatPermission> {
            if (attacker in playing || target in playing) allowed = false
        }
        onEvent<PlayerTeleportEvent.Start> {
            waiting.remove(player)
            playing[player]?.let { finish(it, false) }
        }
        onPlayerLogout {
            waiting.remove(player)
            playing[player]?.let { finish(it, false) }
        }
        onPlayerLogin {
            if (player.vars[AssaultVars.playing] > 0) {
                clear(player)
                move(player, lobby)
            }
        }
        onPlayerSoftTimer(AssaultTimers.wave) {
            if (ticked == clock.cycle) return@onPlayerSoftTimer
            ticked = clock.cycle
            for (game in playing.values.toSet()) tick(game)
        }
        equipment()
    }

    private fun ScriptContext.bindCombat(type: NpcType) {
        onNpcQueue(type, queues.death) {
            noneMode()
            hideAllOps()
            delay(2)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        onEvent<NpcHitEvents.ResolveDamage>(type.id) {
            val game = owners[npc] ?: return@onEvent
            val player = if (hit.isFromPlayer) hit.resolvePlayerSource(players) else null
            val role = game.team[player]
            val kind = game.monsters[npc]
            if (role == null || player == null || kind == null) {
                damage = 0
                return@onEvent
            }
            // Special hits carry an actual bait, poison or cannon egg as their secondary object.
            if (hit.type == HitType.Typeless) {
                val item = hit.secondaryObj
                val valid =
                    when {
                        item == 10537 -> kind == Penance.Queen
                        item in 10531..10533 -> kind != Penance.Queen
                        item in 10513..10515 -> kind == Penance.Runner && role == Role.Defender
                        item in 10539..10541 -> kind == Penance.Healer && role == Role.Healer
                        else -> false
                    }
                if (!valid) damage = 0
                return@onEvent
            }
            if (role != Role.Attacker || kind !in listOf(Penance.Fighter, Penance.Ranger)) {
                damage = 0
                return@onEvent
            }
            val style =
                when (hit.type) {
                    HitType.Melee -> (styles.get(player)?.id ?: 0) - 1
                    HitType.Ranged -> (hit.secondaryObj ?: 0) - 22227
                    HitType.Magic ->
                        when (hit.secondaryType(items)?.internalName?.substringBefore('_')) {
                            "wind",
                            "air" -> 0
                            "water" -> 1
                            "earth" -> 2
                            "fire" -> 3
                            else -> -1
                        }
                    else -> -1
                }
            if (style != game.call[Role.Attacker.ordinal]) {
                damage = 0
                game.wrongStyle++
            } else damage += player.rank(Role.Attacker)
        }
    }

    private fun Player.nextWave() = vars[AssaultVars.wave].coerceIn(1, 10)

    private suspend fun ProtectedAccess.join(wave: Int, door: CoordGrid) {
        if (!player.members || !player.completedBarbarianAssaultTutorial() || player in playing)
            return
        if (waiting.remove(player) != null) {
            telejump(door.translate(0, -1))
            return
        }
        if (player.nextWave() != wave) {
            mes("Your next assault is wave ${player.nextWave()}.")
            return
        }
        if (
            (inv + worn).filterNotNull().any { obj ->
                val type = items.getValue(obj.id)
                type.category == 149 ||
                    type.iop.any { it == "Eat" } ||
                    type.name.endsWith("arrow", true)
            }
        ) {
            mes("Leave food, arrows and runes outside the arena.")
            return
        }
        if (!inv.hasFreeSpace()) {
            mes("You need space for your horn.")
            return
        }
        val i = menu("Choose your Barbarian Assault role", true, Role.entries.map { it.name })
        val role = Role.entries.getOrNull(i) ?: return
        waiting[player] = Seat(wave, role, door)
        telejump(door.translate(0, 1))
        val group = waiting.filterValues { it.wave == wave && it.door == door }
        val team =
            Role.entries
                .mapNotNull { r -> group.entries.firstOrNull { it.value.role == r }?.key }
                .toMutableList()
        if (team.size == 4) {
            group.keys.firstOrNull { it !in team }?.let { team += it }
        }
        if (team.size != 5) {
            mes("Waiting for a team of five covering all four roles.")
            return
        }
        val region =
            regions.add(
                RegionTemplate.create {
                    for (h in 0..1) copy(232, if (wave == 10) 672 else 680, h) {
                        zoneWidth = 8
                        zoneLength = 8
                        regionZoneX = 4
                        regionZoneZ = 4
                    }
                }
            ) ?: return
        val game = Wave(wave, region, team.associateWith { group.getValue(it).role }, clock.cycle)
        for ((p, r) in game.team) {
            waiting.remove(p)
            playing[p] = game
            p.disablePrayers()
            VarPlayerIntMapSetter.set(p, AssaultVars.playing, 1)
            move(p, game.tile(1886, 5456))
            p.invTransaction(p.inv) { add(select(p.inv), AssaultGear.horns[r.ordinal].id, 1) }
            p.ifOpenOverlay(AssaultInterfaces.roles[r.ordinal], events)
            p.ifSetText(AssaultComponents.wave[r.ordinal], "Wave $wave")
            p.softTimer(AssaultTimers.wave, 1)
        }
        call(game)
        for (kind in Penance.entries.take(4)) {
            game.reserve[kind.ordinal] = reserves[wave - 1][kind.ordinal]
            repeat(initial[wave - 1][kind.ordinal]) { spawn(game, kind, it) }
        }
    }

    private fun spawn(game: Wave, kind: Penance, offset: Int) {
        val idx = kind.ordinal
        val type =
            if (kind == Penance.Queen) AssaultNpcs.queen
            else AssaultNpcs.monsters[idx][game.rankIndex]
        val x = listOf(1880, 1898, 1874, 1892, 1886)[idx]
        val npc =
            Npc(
                npcTypes[type],
                game.tile(x + offset % 2, if (kind == Penance.Queen) 5471 else 5484 - offset / 2),
            )
        npcs.add(npc, 36000)
        owners[npc] = game
        game.monsters[npc] = kind
        if (kind in listOf(Penance.Fighter, Penance.Ranger, Penance.Queen))
            npc.opPlayer2(game.team.keys.elementAt(random.of(5)), attacks)
    }

    private fun killed(game: Wave, npc: Npc) {
        owners.remove(npc)
        val kind = game.monsters.remove(npc) ?: return
        game.poisoned.remove(npc)
        game.lured.remove(npc)
        if (kind == Penance.Queen) {
            finish(game, true)
            return
        }
        game.deaths[kind.ordinal]++
        for (egg in AssaultGear.eggs.take(3)) game.ground += ground.add(egg, npc.coords, 250)
        if (game.reserve[kind.ordinal] > 0) {
            game.reserve[kind.ordinal]--
            spawn(game, kind, 0)
        }
        if (game.monsters.isEmpty()) {
            if (game.number < 10) finish(game, true)
            else if (!game.queenSpawned)
                game.queenSpawnAt = minOf(game.queenSpawnAt, clock.cycle + 25)
        }
    }

    private fun call(game: Wave) {
        for (role in Role.entries) game.call[role.ordinal] = random.of(words[role.ordinal].size)
        for ((p, role) in game.team) {
            val partner = partners[role.ordinal].ordinal
            p.ifSetText(AssaultComponents.shout[role.ordinal], words[partner][game.call[partner]])
            p.ifSetText(AssaultComponents.hear[role.ordinal], "")
        }
    }

    private fun tick(game: Wave) {
        val time = clock.cycle - game.began
        for ((npc, lure) in game.lured.toMap()) {
            if (npc.coords.chebyshevDistance(lure.trap) > 1) {
                if (time % 5 == 0) npc.walk(lure.trap)
                continue
            }
            game.lured.remove(npc)
            val uses = game.trapUses.getOrDefault(lure.trap, 0)
            if (uses < 2) {
                setTrap(game, lure.trap, uses + 1)
                special(npc, lure.player, 5, lure.bait)
            }
        }
        if (time % 10 == 0)
            for ((item, tile) in
                listOf(objs.hammer to game.tile(1884, 5483), objs.logs to game.tile(1885, 5483))) {
                if (ground.findAll(tile).none { it.type == item.id })
                    game.ground += ground.add(item, tile, 100)
            }
        if (time % 50 == 0 && !game.queenSpawned) call(game)
        if (time % 5 == 0)
            for ((npc, poison) in game.poisoned.toMap()) {
                val amount = minOf(6, poison.budget)
                special(npc, poison.source, amount, AssaultGear.poison.first())
                poison.budget -= amount
                if (poison.budget <= 0) game.poisoned.remove(npc)
            }
        if (
            game.number == 10 &&
                !game.queenSpawned &&
                (clock.cycle >= game.queenSpawnAt || time >= 400)
        ) {
            game.queenSpawned = true
            spawn(game, Penance.Queen, 0)
        }
        if (game.queenSpawned && time % 10 == 0) {
            val queen =
                game.monsters.entries.firstOrNull { it.value == Penance.Queen }?.key ?: return
            game.ground += ground.add(AssaultGear.eggs[3], queen.coords.translate(2, 0), 150)
        }
    }

    private fun finish(game: Wave, won: Boolean) {
        for (npc in game.monsters.keys) {
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        for (egg in game.ground) ground.del(egg)
        for ((p, role) in game.team) {
            playing.remove(p)
            p.ifCloseOverlay(AssaultInterfaces.roles[role.ordinal], events)
            clear(p)
            move(p, lobby)
            if (!won) {
                p.mes("Your team failed the wave.")
                continue
            }
            val next = if (game.number == 10) 1 else game.number + 1
            VarPlayerIntMapSetter.set(p, AssaultVars.wave, next)
            val score =
                intArrayOf(
                    game.deaths[0] + game.deaths[2] - 2 - (game.wrongStyle - 1).coerceIn(0, 10),
                    (game.eggs.coerceIn(0, 60) / 4.35).toInt(),
                    (game.deaths[1] * 1.8).toInt() - minOf(10, game.escaped * 3),
                    (game.deaths[3] * 0.7).toInt() + minOf(28, game.healed / 18) -
                        game.wrongPoison / 4,
                )
            for (r in Role.entries) {
                val points =
                    if (game.number == 10) {
                        if (r == role) 80 else 5
                    } else if (r == role) score.sum().coerceAtLeast(0) + score[role.ordinal] else 0
                val bonus =
                    if (p.vars[DiaryRegion.Kandarin.tier(DiaryDifficulty.Hard).complete] != 0)
                        points / 10
                    else 0
                p.setHonour(r, p.honour(r) + (points + bonus).coerceAtLeast(0))
            }
            if (game.number == 10) p.setQueens(p.vars[AssaultVars.queens] + 1)
            p.completeDiaryTask(DiaryTask.FinishAssaultWave)
            p.mes(
                "Your team completed wave ${game.number}. You now have ${p.honour(role)} ${role.name.lowercase()} points."
            )
        }
    }

    private fun clear(p: Player) {
        VarPlayerIntMapSetter.set(p, AssaultVars.playing, 0)
        p.clearSoftTimer(AssaultTimers.wave)
        for (inv in listOf(p.inv, p.worn)) for (slot in inv.indices) {
            val id = inv[slot]?.id ?: continue
            if (id in 10513..10546 || id in 10560..10561 || id in 22227..22230) inv[slot] = null
        }
        for (bit in AssaultBits.eggs) VarPlayerIntMapSetter.set(p, bit, 0)
    }

    private fun move(p: Player, to: CoordGrid) = PathingEntityCommon.telejump(p, collision, to)

    private fun special(npc: Npc, p: Player, amount: Int, source: ObjType) {
        npc.queueHit(p, 1, HitType.Typeless, amount, modifier, sourceSecondary = source)
    }

    private fun ScriptContext.equipment() {
        for ((r, horn) in AssaultGear.horns.withIndex()) {
            val role = Role.entries[r]
            val order =
                listOf(listOf(1, 0, 2), listOf(0, 1, 2, 3), listOf(0, 1, 2), listOf(1, 0, 2))[r]
            onOpHeld1(horn) { signal(player, role, order[0]) }
            onOpHeld2(horn) { signal(player, role, order[1]) }
            onOpHeld3(horn) { signal(player, role, order[2]) }
            onOpHeld4(horn) { signal(player, role, order.getOrNull(3)) }
            if (role == Role.Collector) onOpHeld5(horn) { signal(player, role, null) }
        }
        val machines = AssaultLocs.machines
        onOpLoc1(machines[0]) {
            if (playing[player]?.team?.get(player) == Role.Attacker)
                for (id in 22227..22230) invAdd(inv, items.getValue(id), 50)
        }
        onOpLoc1(machines[1]) {
            if (playing[player]?.team?.get(player) == Role.Defender) stock(AssaultGear.bait)
        }
        onOpLoc1(machines[2]) {
            if (playing[player]?.team?.get(player) == Role.Healer) {
                if (AssaultGear.vials.none { it in inv }) invAdd(inv, AssaultGear.vials.last())
                stock(AssaultGear.poison)
            }
        }
        onOpLoc2(machines[2]) {
            if (
                playing[player]?.team?.get(player) == Role.Healer &&
                    AssaultGear.vials.none { it in inv }
            )
                invAdd(inv, AssaultGear.vials.last())
        }
        for ((i, food) in AssaultGear.bait.withIndex()) {
            onOpHeld1(food) { bait(i, player.coords) }
            for (trap in AssaultLocs.traps) onOpLocU(trap, food) { bait(i, it.loc.coords) }
        }
        for (trap in AssaultLocs.traps.drop(1)) {
            onOpLoc1(trap) { repair(it.loc.coords) }
            onOpLocU(trap, objs.hammer) { repair(it.loc.coords) }
        }
        for ((i, egg) in AssaultGear.eggs.take(4).withIndex()) onOpObj3(egg) {
            val game = playing[player] ?: return@onOpObj3
            if (game.team[player] != Role.Collector || it.obj !in game.ground) return@onOpObj3
            if (i != 3 && i != game.call[Role.Collector.ordinal]) {
                if (ground.del(it.obj)) {
                    game.ground.remove(it.obj)
                    game.eggs--
                }
                mes("That egg exploded.")
                return@onOpObj3
            }
            val take = invAdd(inv, egg, autoCommit = false)
            if (take.success && ground.del(it.obj)) {
                take.commitAll()
                game.ground.remove(it.obj)
                if (i != 3) game.eggs++
            }
        }
        for (i in 3..6) onOpPlayerU(AssaultGear.eggs[i]) {
            val game = playing[player] ?: return@onOpPlayerU
            val chain =
                listOf(Role.Collector, Role.Healer, Role.Attacker, Role.Defender, Role.Collector)
            if (game.team[player] != chain[i - 3] || game.team[it.target] != chain[i - 2])
                return@onOpPlayerU
            val egg = AssaultGear.eggs[i]
            player.invTransaction(inv, it.target.inv) {
                delete(select(inv), egg.id, 1, slot = it.invSlot)
                add(select(it.target.inv), egg.id, 1)
            }
        }
        for (pool in AssaultLocs.poison) {
            onOpLoc1(pool) { dip(Role.Healer, 3) }
            onOpLocU(pool, AssaultGear.eggs[3]) { dip(Role.Healer, 3) }
        }
        for (pool in AssaultLocs.lava) {
            onOpLoc1(pool) { dip(Role.Defender, 5) }
            onOpLocU(pool, AssaultGear.eggs[5]) { dip(Role.Defender, 5) }
        }
        onOpLoc1(AssaultLocs.mushroom) {
            if (playing[player]?.team?.get(player) == Role.Attacker) invAdd(inv, AssaultGear.spikes)
        }
        onOpHeldU(AssaultGear.spikes, AssaultGear.eggs[4]) {
            if (playing[player]?.team?.get(player) == Role.Attacker)
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, AssaultGear.spikes.id, 1)
                    delete(bag, AssaultGear.eggs[4].id, 1)
                    add(bag, AssaultGear.eggs[5].id, 1)
                }
        }
        for (vial in AssaultGear.vials) onOpLocU(AssaultLocs.spring, vial) { fillVial() }
        onOpLoc2(AssaultLocs.spring) { fillVial() }
        onOpLoc1(AssaultLocs.spring) {
            val game = playing[player] ?: return@onOpLoc1
            if (game.team[player] == Role.Healer) {
                val before = player.hitpoints
                player.statHeal(stats.hitpoints, 35, 0)
                game.healed += player.hitpoints - before
            }
        }
        for ((i, vial) in AssaultGear.vials.take(4).withIndex()) onOpPlayerU(vial) {
            val game = playing[player] ?: return@onOpPlayerU
            if (game.team[player] != Role.Healer || it.target !in game.team) return@onOpPlayerU
            if (invReplace(inv, vial, 1, AssaultGear.vials[i + 1]).failure) return@onOpPlayerU
            val p = it.target
            val before = p.hitpoints
            p.statHeal(stats.hitpoints, listOf(10, 15, 20, 25, 35)[player.rank(Role.Healer) - 1], 0)
            game.healed += p.hitpoints - before
        }
        onOpLoc1(AssaultLocs.hopper) { for (i in listOf(0, 1, 2, 6)) load(i, true) }
        for (i in listOf(0, 1, 2, 6)) onOpLocU(AssaultLocs.hopper, AssaultGear.eggs[i]) {
            load(i, false)
        }
        onOpLoc1(AssaultLocs.launcher) {
            val game = playing[player] ?: return@onOpLoc1
            val choices = listOf("Green egg", "Red egg", "Blue egg", "Omega egg")
            val i =
                menu(
                    "Fire egg launcher",
                    true,
                    choices.mapIndexed { index, name -> "$name (${game.loaded[index]})" },
                )
            if (i !in choices.indices || game.loaded[i] < 1) return@onOpLoc1
            val targets =
                game.monsters.filter { (npc, kind) ->
                    npc.hitpoints > 0 && (kind == Penance.Queen) == (i == 3)
                }
            val kinds = targets.values.distinct()
            val kind =
                if (kinds.size == 1) kinds.first()
                else kinds.getOrNull(menu("Choose a target", true, kinds.map { it.name }))
            val target = targets.entries.firstOrNull { it.value == kind }?.key ?: return@onOpLoc1
            if (i == 3 && game.monsters.values.any { it != Penance.Queen }) return@onOpLoc1
            game.loaded[i]--
            showLoad(game)
            special(target, player, listOf(6, 3, 0, 32)[i], AssaultGear.eggs[if (i == 3) 6 else i])
        }
    }

    private fun signal(player: Player, role: Role, index: Int?) {
        val game = playing[player] ?: return
        if (game.team[player] != role) return
        if (index == null) {
            game.team
                .filterValues { it == Role.Healer }
                .keys
                .forEach { it.mes("Medic! ${player.displayName} needs healing.") }
            return
        }
        val partner = partners[role.ordinal]
        for (p in game.team.filterValues { it == partner }.keys) p.ifSetText(
            AssaultComponents.hear[partner.ordinal],
            words[partner.ordinal][index],
        )
    }

    private fun ProtectedAccess.stock(types: List<ObjType>) {
        repeat(5) { for (type in types) if (inv.hasFreeSpace()) invAdd(inv, type) }
    }

    private fun ProtectedAccess.poison(npc: Npc, i: Int) {
        val game = playing[player] ?: return
        if (
            game.team[player] != Role.Healer ||
                game.monsters[npc] != Penance.Healer ||
                npc.hitpoints <= 0
        )
            return
        if (invDel(inv, AssaultGear.poison[i]).failure) return
        if (game.call[Role.Healer.ordinal] != i) {
            game.wrongPoison++
            mes("That was the wrong poison.")
            return
        }
        val poison = game.poisoned.getOrPut(npc) { Poison(player, 0) }
        poison.budget += 54
    }

    private fun ProtectedAccess.bait(i: Int, tile: CoordGrid) {
        val game = playing[player] ?: return
        if (game.team[player] != Role.Defender || invDel(inv, AssaultGear.bait[i]).failure) return
        val trap =
            listOf(game.tile(1871, 5473), game.tile(1901, 5474)).firstOrNull {
                it.chebyshevDistance(tile) <= 1
            }
        val runner =
            game.monsters
                .filter { (npc, k) ->
                    k == Penance.Runner && npc.hitpoints > 0 && npc !in game.lured
                }
                .keys
                .minByOrNull { it.coords.chebyshevDistance(tile) } ?: return
        if (game.call[Role.Defender.ordinal] != i) return
        runner.walk(tile)
        if (trap == null) return
        val uses = game.trapUses.getOrDefault(trap, 0)
        if (uses >= 2) {
            mes("The trap needs repairing with a hammer and logs.")
            return
        }
        // The runner moves to the bait before triggering the trap.
        game.lured[runner] = Lure(player, trap, AssaultGear.bait[i])
    }

    private fun ProtectedAccess.repair(tile: CoordGrid) {
        val game = playing[player] ?: return
        if (
            game.team[player] != Role.Defender ||
                game.trapUses.getOrDefault(tile, 0) == 0 ||
                objs.hammer !in inv
        )
            return
        if (invDel(inv, objs.logs).success) setTrap(game, tile, 0)
    }

    private fun setTrap(game: Wave, tile: CoordGrid, uses: Int) {
        game.trapUses[tile] = uses
        locations.add(
            tile,
            AssaultLocs.traps[uses],
            Int.MAX_VALUE,
            org.rsmod.game.loc.LocAngle.West,
            org.rsmod.game.loc.LocShape.GroundDecor,
        )
    }

    private fun ProtectedAccess.dip(role: Role, i: Int) {
        if (playing[player]?.team?.get(player) == role)
            invReplace(inv, AssaultGear.eggs[i], 1, AssaultGear.eggs[i + 1])
    }

    private fun ProtectedAccess.fillVial() {
        if (playing[player]?.team?.get(player) != Role.Healer) return
        AssaultGear.vials
            .drop(1)
            .firstOrNull { it in inv }
            ?.let { invReplace(inv, it, 1, AssaultGear.vials.first()) }
    }

    private fun ProtectedAccess.load(index: Int, all: Boolean) {
        val game = playing[player] ?: return
        if (game.team[player] != Role.Collector) return
        val i = if (index == 6) 3 else index
        val egg = AssaultGear.eggs[index]
        val count = minOf(if (all) invTotal(inv, egg) else 1, 5 - game.loaded[i])
        if (count > 0 && invDel(inv, egg, count).success) {
            game.loaded[i] += count
            showLoad(game)
        }
    }

    private fun showLoad(game: Wave) {
        for (p in game.team.keys) for ((i, bit) in
            AssaultBits.eggs.withIndex()) VarPlayerIntMapSetter.set(p, bit, game.loaded[i])
    }
}
