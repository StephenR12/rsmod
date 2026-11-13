package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.back
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.isOutOfCombat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

private var Player.armourSet by intVarBit(GuildTrainingBits.armour)
internal var Player.guildDefenderTier by intVarBit(GuildTrainingBits.tier)
private var Player.basementUnlocked by intVarBit(GuildTrainingBits.dungeon)
internal var Player.guildCyclopsRoom by intVarp(GuildTrainingVars.floor)
private var Player.grace by intVarp(GuildTrainingVars.grace)
internal var Player.unclaimed by intVarp(GuildTrainingVars.tokens)
private val armourTokens = listOf(5, 10, 15, 20, 25, 30, 40)
private val exits = listOf(CoordGrid(2846, 3540, 2), CoordGrid(2910, 9968))
private val entrances = listOf(CoordGrid(2848, 3540, 2), CoordGrid(2912, 9968))
private val targets =
    listOf(
        CoordGrid(2856, 3554),
        CoordGrid(2858, 3554),
        CoordGrid(2860, 3553),
        CoordGrid(2855, 3552),
        CoordGrid(2859, 3549),
        CoordGrid(2857, 3549),
        CoordGrid(2855, 3550),
    )

public class WarriorsGuildTraining
@Inject
constructor(
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val items: ObjTypeList,
    private val locs: LocRepository,
    private val ground: ObjRepository,
    private val random: GameRandom,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val interactions: AiPlayerInteractions,
    private val players: PlayerList,
    private val styles: AttackStyles,
    private val attacks: AttackTypes,
) : PluginScript() {
    private val armours = mutableMapOf<Npc, Player>()
    private val active = mutableMapOf<Player, Npc>()
    private var dummyUntil = 0
    private var currentDummy = -1
    private var currentDummyTile = targets.first()
    private var struck = false

    override fun ScriptContext.startup() {
        onOpLoc1(GuildScenery.animator) {
            val sets =
                GuildTrainingItems.sets.indices.filter { t ->
                    GuildTrainingItems.sets[t].all { piece -> piece in inv }
                }
            if (sets.isEmpty()) {
                mes("Bring a full helm, platebody and platelegs of the same metal.")
                return@onOpLoc1
            }
            val tier =
                if (sets.size == 1) sets.first()
                else
                    sets.getOrNull(
                        menu("Animate which armour?", true, sets.map { guildMetals[it] })
                    ) ?: return@onOpLoc1
            animate(tier, it.loc.coords)
        }
        for ((tier, set) in GuildTrainingItems.sets.withIndex()) for (piece in set) onOpLocU(
            GuildScenery.animator,
            piece,
        ) {
            animate(tier, it.loc.coords)
        }
        for (type in GuildEnemies.armour) {
            onEvent<NpcHitEvents.ResolveDamage>(type.id) {
                val owner = armours[npc]
                if (
                    hit.type != HitType.Melee ||
                        !hit.isFromPlayer ||
                        hit.resolvePlayerSource(players) != owner
                )
                    damage = 0
            }
        }
        for (type in GuildEnemies.cyclops) onEvent<NpcHitEvents.ResolveDamage>(type.id) {
            if (
                hit.isFromPlayer &&
                    (hit.type != HitType.Melee ||
                        hit.resolvePlayerSource(players)?.guildCyclopsRoom !=
                            if (npc.id in 2463..2468) 1 else 2)
            )
                damage = 0
        }
        onEvent<NpcKillEvents.Credited> {
            val owner = armours[npc]
            if (owner == player) {
                finishArmour(player, npc)
                return@onEvent
            }
        }
        onEvent<NpcStateEvents.Delete> { armours.remove(npc)?.let { active.remove(it) } }
        onOpNpc1(GuildEnemies.shanomi) {
            if (player.armourSet != 0 && player !in active) reclaim() else claim()
        }
        for (keeper in
            listOf(
                GuildEnemies.shanomi,
                GuildEnemies.ajjat,
                GuildEnemies.kamfreena,
                GuildEnemies.lorelai,
                GuildCoaches.gamfred,
                GuildCoaches.sloane,
                GuildCoaches.yadech,
            ) + GuildCoaches.referees) {
            onOpNpc3(keeper) { claim() }
        }
        onOpNpc1(GuildEnemies.kamfreena) {
            progress()
            claim()
        }
        onOpNpc1(GuildEnemies.lorelai) {
            if (showRune()) mes("You may fight the cyclopes in the basement.")
            claim()
        }
        onOpNpc1(GuildEnemies.ajjat) {
            mes("Match the dummy's weapon type or attack style to win tokens.")
            claim()
            if (player.statBase(stats.attack) >= 99)
                SkillcapeSale.offer(this, stats.attack, objs.attack_hood)
        }
        for ((i, dagger) in GuildTrainingItems.daggers.take(7).withIndex()) {
            onOpNpcU(GuildEnemies.kamfreena, dagger) { progress() }
            if (i == 6) onOpNpcU(GuildEnemies.lorelai, dagger) { showRune() }
        }
        for (door in GuildScenery.chamber) onOpLoc1(door) {
            val c = it.loc.coords
            if (c.level == 2 && c.x == 2847) enterRoom(1)
            else if (c.level == 0 && c.z == 3546)
                telejump(CoordGrid(c.x, if (player.coords.z >= 3546) 3545 else 3547))
        }
        onOpLoc1(GuildScenery.basement) { enterRoom(2) }
        onOpLoc1(GuildScenery.ladders[0]) { if (eligible()) telejump(CoordGrid(2907, 9968)) }
        onOpLoc1(GuildScenery.ladders[1]) { telejump(CoordGrid(2834, 3542)) }
        onOpLoc1(GuildScenery.top) { telejump(CoordGrid(2840, 3539, 1)) }
        onPlayerSoftTimer(GuildTrainingTimers.room) {
            if (player.guildCyclopsRoom !in 1..2) {
                player.clearSoftTimer(GuildTrainingTimers.room)
                return@onPlayerSoftTimer
            }
            if (player.grace == 2) {
                if (!free(player) && player.isOutOfCombat()) leave(player, true)
                return@onPlayerSoftTimer
            }
            if (!free(player)) {
                if (player.inv.count(items[GuildTrainingItems.tokens]) >= 10) {
                    player.invDel(player.inv, GuildTrainingItems.tokens, 10)
                    player.grace = 0
                } else if (player.grace != 0) {
                    leave(player, true)
                    return@onPlayerSoftTimer
                }
                if (player.inv.count(items[GuildTrainingItems.tokens]) < 10) {
                    player.grace = 1
                    player.mes("Your tokens have run out. You have one minute to leave.")
                }
            }
        }
        onPlayerLogin { if (player.guildCyclopsRoom in 1..2) leave(player, true) }
        onPlayerLogout {
            if (player.guildCyclopsRoom in 1..2) leave(player, true)
            abandonArmour(player)
        }
        onEvent<PlayerTeleportEvent.Start> {
            if (player.guildCyclopsRoom in 1..2) leave(player, false)
            abandonArmour(player)
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            if (player.guildCyclopsRoom in 1..2) leave(player, false)
            abandonArmour(player)
        }
        for ((i, dummy) in GuildScenery.activeDummies.withIndex()) onOpLoc1(dummy) {
            if (
                !eligible() ||
                    it.loc.coords != currentDummyTile ||
                    i != currentDummy ||
                    clock.cycle >= dummyUntil ||
                    struck
            )
                return@onOpLoc1
            val style = MeleeAttackStyle.from(styles.get(player))
            val type = MeleeAttackType.from(attacks.get(player))
            val matches =
                when (i) {
                    0 -> style == MeleeAttackStyle.Accurate
                    1 -> type == MeleeAttackType.Slash
                    2 -> style == MeleeAttackStyle.Aggressive
                    3 -> style == MeleeAttackStyle.Controlled
                    4 -> type == MeleeAttackType.Crush
                    5 -> type == MeleeAttackType.Stab
                    else -> style == MeleeAttackStyle.Defensive
                }
            if (!matches) {
                mes("You need a different attack style for that dummy.")
                delay(3)
                return@onOpLoc1
            }
            struck = true
            statAdvance(stats.attack, 15.0)
            player.earnGuildTokens(2)
            mes("You hit the dummy and earn guild tokens.")
        }
        onPlayerSoftTimer(GuildTrainingTimers.dummies) {
            val c = player.coords
            if (c.level != 0 || c.x !in 2851..2862 || c.z !in 3548..3556) {
                player.clearSoftTimer(GuildTrainingTimers.dummies)
                return@onPlayerSoftTimer
            }
            rotate()
        }
        onOpLoc1(GuildScenery.dummyDoor) {
            if (!eligible()) return@onOpLoc1
            val enter = player.coords.x <= 2853
            telejump(CoordGrid(if (enter) 2854 else 2852, 3554))
            if (enter) startDummy()
        }
    }

    private fun ProtectedAccess.startDummy() {
        player.softTimer(GuildTrainingTimers.dummies, 2)
        rotate()
    }

    private fun rotate() {
        if (clock.cycle < dummyUntil) return
        val slot = random.of(8)
        currentDummy = if (slot == 7) 3 else slot
        currentDummyTile = if (slot == 7) CoordGrid(2860, 3551) else targets[slot]
        struck = false
        dummyUntil = clock.cycle + 8
        val tile = currentDummyTile
        val base = locs.findAll(tile).firstOrNull { it.id == GuildScenery.dummies[currentDummy].id }
        if (base != null) locs.change(base, GuildScenery.activeDummies[currentDummy], 8)
        else
            locs.add(
                tile,
                GuildScenery.activeDummies[currentDummy],
                8,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
    }

    private fun ProtectedAccess.eligible(): Boolean = player.canTrainAtGuild()

    private fun ProtectedAccess.animate(tier: Int, c: CoordGrid) {
        if (!eligible()) return
        if (player.armourSet != 0) {
            mes("Finish your last fight or recover your equipment from Shanomi.")
            return
        }
        val set = GuildTrainingItems.sets[tier]
        if (
            player
                .invTransaction(inv) {
                    val bag = select(inv)
                    for (piece in set) delete(bag, piece.id, 1)
                }
                .failure
        ) {
            mes("You need a matching full helm, platebody and platelegs.")
            return
        }
        player.armourSet = tier + 1
        val foe = Npc(types[GuildEnemies.armour[tier]], c.translate(0, 1))
        npcs.add(foe, 500)
        armours[foe] = player
        active[player] = foe
        foe.opPlayer2(player, interactions)
    }

    private fun finishArmour(p: Player, n: Npc) {
        val tier = p.armourSet - 1
        if (tier !in 0..6) return
        armours.remove(n)
        active.remove(p)
        p.armourSet = 0
        ground.add(
            GuildTrainingItems.tokens,
            n.coords,
            constants.lootdrop_duration,
            p,
            armourTokens[tier] * p.guildTokenMultiplier(),
        )
        for ((i, piece) in GuildTrainingItems.sets[tier].withIndex()) {
            val breaks = tier == 0 || (tier == 1 && i == 2) || (tier == 2 && i == 0)
            if (!breaks || random.of(10) != 0)
                ground.add(piece, n.coords, constants.lootdrop_duration, p)
        }
    }

    private fun abandonArmour(p: Player) {
        val foe = active.remove(p) ?: return
        armours.remove(foe)
        npcs.del(foe, Int.MAX_VALUE)
    }

    private fun ProtectedAccess.reclaim() {
        val t = player.armourSet - 1
        if (t !in 0..6) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                for (piece in GuildTrainingItems.sets[t]) add(bag, piece.id, 1)
            }
        if (result.success) player.armourSet = 0
        else mes("Make room for all three pieces of armour.")
    }

    private fun ProtectedAccess.claim() {
        val amount = player.unclaimed
        if (amount <= 0) return
        if (invAdd(inv, GuildTrainingItems.tokens, amount).success) {
            player.unclaimed = 0
            mes("You claim $amount warrior guild tokens.")
        }
    }

    private fun ProtectedAccess.progress() {
        val shown = GuildTrainingItems.daggers.take(7).indexOfLast { it in inv || it in worn }
        if (shown >= 0)
            player.guildDefenderTier = maxOf(player.guildDefenderTier, (shown + 1).coerceAtMost(6))
    }

    private fun ProtectedAccess.showRune(): Boolean {
        if (GuildTrainingItems.daggers[6] in inv || GuildTrainingItems.daggers[6] in worn)
            player.basementUnlocked = 1
        return player.basementUnlocked == 1
    }

    private fun ProtectedAccess.enterRoom(floor: Int) {
        if (player.guildCyclopsRoom != 0) {
            leave(player, true)
            return
        }
        if (!eligible()) return
        if (floor == 2 && !showRune()) {
            mes("Show Lorelai a rune defender before entering.")
            return
        }
        if (!free(player)) {
            if (invTotal(inv, GuildTrainingItems.tokens) < 100) {
                mes("You need at least 100 Warrior Guild tokens to enter.")
                return
            }
            if (invDel(inv, GuildTrainingItems.tokens, 10).failure) return
        }
        if (floor == 1) progress()
        player.guildCyclopsRoom = floor
        // A cape admission lasts only while it is worn. Finish an active fight before ejecting.
        val capeAdmission = free(player)
        player.grace = if (capeAdmission) 2 else 0
        telejump(entrances[floor - 1])
        player.softTimer(GuildTrainingTimers.room, if (capeAdmission) 1 else 100)
    }

    private fun free(p: Player): Boolean {
        val cape = p.back ?: return false
        return cape.id in
            listOf(objs.attack_skillcape.id, objs.attack_skillcape_t.id, objs.max_cape_worn.id)
    }

    private fun leave(p: Player, exit: Boolean) {
        val floor = p.guildCyclopsRoom
        p.guildCyclopsRoom = 0
        p.grace = 0
        p.clearSoftTimer(GuildTrainingTimers.room)
        if (exit && floor in 1..2) PathingEntityCommon.telejump(p, collision, exits[floor - 1])
    }
}

internal fun Player.canTrainAtGuild(): Boolean {
    val attack = statBase(stats.attack)
    val strength = statBase(stats.strength)
    return members && (attack + strength >= 130 || maxOf(attack, strength) >= 99)
}

private fun Player.guildTokenMultiplier(): Int =
    GuildTrainingBits.ca.indexOfLast { vars[it] >= 2 } + 2

internal fun Player.earnGuildTokens(amount: Int) {
    unclaimed += amount * guildTokenMultiplier()
}
