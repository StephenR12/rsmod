package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.magic.WardOfArceuusStateRepository
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueueWithArgs
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CerberusQueue : QueueReferences() {
    val step = find("cerberus_special_step")
}

internal object CerberusSeqs : SeqReferences() {
    val bite = find("cerberus_bite")
    val ranged = find("cerberus_attack_range")
    val magic = find("cerberus_fire_breath")
    val howl = find("cerberus_howl")
    val combo = find("cerberus_special_attack_spray")
    val lava = find("cerberus_special_attack_flame")
}

internal object CerberusSpots : SpotanimReferences() {
    val pool = find("cerberus_special_attack_flame")
}

private class Fight(val target: Player) {
    var attacks = 0
    var lastAttack = 0
    val spirits = mutableListOf<Npc>()
}

private sealed interface CerberusStep {
    val fight: Fight

    data class Strike(override val fight: Fight, val style: HitType) : CerberusStep

    data class Soul(override val fight: Fight, val spirit: Npc, val style: HitType) : CerberusStep

    data class Lava(override val fight: Fight, val tiles: Set<CoordGrid>, val pulse: Int) :
        CerberusStep
}

@Singleton
public class CerberusCombat
@Inject
constructor(
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val world: WorldRepository,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val ward: WardOfArceuusStateRepository,
) : PluginScript() {
    private val fights = IdentityHashMap<Npc, Fight>()

    override fun ScriptContext.startup() {
        onNpcQueueWithArgs<CerberusStep>(CerberusQueue.step) {
            val step = it.args
            if (fights[npc] !== step.fight || npc.hitpoints <= 0) return@onNpcQueueWithArgs
            if (!inArena(npc, step.fight.target)) {
                end(npc)
                return@onNpcQueueWithArgs
            }
            when (step) {
                is CerberusStep.Strike -> strike(npc, step.fight.target, step.style, false)
                is CerberusStep.Soul -> {
                    soulStrike(step.spirit, step.fight.target, step.style)
                    if (step.spirit.isSlotAssigned) npcs.del(step.spirit, Int.MAX_VALUE)
                    step.fight.spirits.remove(step.spirit)
                }
                is CerberusStep.Lava -> lavaPulse(npc, step)
            }
        }
        onEvent<NpcKillEvents.Credited> { if (npc.id == SlayerBossNpcs.cerberus.id) end(npc) }
        onEvent<NpcStateEvents.Respawn> { if (npc.id == SlayerBossNpcs.cerberus.id) end(npc) }
    }

    public fun attack(boss: Npc, target: Player) {
        if (boss.actionDelay > boss.currentMapClock || !inArena(boss, target)) return
        if (boss.distanceTo(target) > 14 || boss.hitpoints <= 0) return
        val previous = fights[boss]
        val fight =
            if (previous?.target === target && boss.currentMapClock - previous.lastAttack <= 15)
                previous
            else {
                end(boss)
                Fight(target).also { fights[boss] = it }
            }
        fight.attacks++
        fight.lastAttack = boss.currentMapClock
        boss.actionDelay = boss.currentMapClock + 6
        when {
            fight.attacks % 10 == 1 -> {
                boss.anim(CerberusSeqs.combo)
                strike(boss, target, HitType.Magic, false)
                boss.queue(CerberusQueue.step, 2, CerberusStep.Strike(fight, HitType.Ranged))
                boss.queue(CerberusQueue.step, 4, CerberusStep.Strike(fight, HitType.Melee))
            }
            fight.attacks % 7 == 0 && boss.hitpoints < 400 && random.of(10) != 0 ->
                souls(boss, fight)
            fight.attacks % 5 == 0 && boss.hitpoints < 200 && random.of(10) != 0 ->
                lava(boss, fight)
            else -> {
                val styles =
                    if (boss.distanceTo(target) <= 1)
                        listOf(HitType.Magic, HitType.Ranged, HitType.Melee)
                    else listOf(HitType.Magic, HitType.Ranged)
                strike(boss, target, styles[random.of(styles.size)], true)
            }
        }
    }

    private fun strike(boss: Npc, target: Player, style: HitType, animate: Boolean) {
        if (animate)
            boss.anim(
                when (style) {
                    HitType.Melee -> CerberusSeqs.bite
                    HitType.Ranged -> CerberusSeqs.ranged
                    else -> CerberusSeqs.magic
                }
            )
        val accurate =
            !protected(target, style) &&
                when (style) {
                    HitType.Melee ->
                        accuracy.rollMeleeAccuracy(boss, target, MeleeAttackType.Crush, random)
                    HitType.Ranged -> accuracy.rollRangedAccuracy(boss, target, random)
                    else -> accuracy.rollMagicAccuracy(boss, target, random)
                }
        target.queueHit(
            boss,
            if (style == HitType.Melee) 1 else 2,
            style,
            if (accurate) random.of(0..23) else 0,
        )
    }

    private fun souls(boss: Npc, fight: Fight) {
        boss.say("Aaarrrooooooo")
        boss.anim(CerberusSeqs.howl)
        val spirits =
            mutableListOf(
                SlayerBossNpcs.meleeSoul to HitType.Melee,
                SlayerBossNpcs.rangedSoul to HitType.Ranged,
                SlayerBossNpcs.magicSoul to HitType.Magic,
            )
        val origin = boss.spawnCoords
        for (index in 0..2) {
            val (type, style) = spirits.removeAt(random.of(spirits.size))
            val soul = Npc(types[type], origin.translate(-3 + index * 3, 7))
            soul.mode = NpcMode.None
            npcs.add(soul, Int.MAX_VALUE)
            fight.spirits += soul
            boss.queue(CerberusQueue.step, 7 + index * 2, CerberusStep.Soul(fight, soul, style))
        }
    }

    private fun soulStrike(soul: Npc, target: Player, style: HitType) {
        if (protected(target, style)) {
            val spectral = target.lefthand?.id == 12821
            val base = if (ward.isActive(target)) 20 else 30
            target.statSub(stats.prayer, if (spectral) base / 2 else base, 0)
            target.queueHit(soul, 1, HitType.Typeless, 0)
        } else {
            val damage =
                if (target.lefthand.isType(objs.elysian_spirit_shield) && random.of(100) < 70) 22
                else 30
            target.queueHit(soul, 1, HitType.Typeless, damage)
        }
    }

    private fun protected(target: Player, style: HitType): Boolean {
        val prayer =
            when (style) {
                HitType.Melee -> varbits.protect_from_melee
                HitType.Ranged -> varbits.protect_from_missiles
                else -> varbits.protect_from_magic
            }
        return target.stat(stats.prayer) > 0 && target.vars[prayer] != 0
    }

    private fun lava(boss: Npc, fight: Fight) {
        boss.anim(CerberusSeqs.lava)
        boss.say("Grrrrrrrrrrrrrr")
        val target = fight.target
        val tiles = linkedSetOf(target.coords)
        val choices =
            buildList {
                    for (dx in -4..4) for (dz in -4..4) if (dx != 0 || dz != 0)
                        add(target.coords.translate(dx, dz))
                }
                .toMutableList()
        repeat(2) { tiles += choices.removeAt(random.of(choices.size)) }
        val delay = if (boss.distanceTo(target) <= 1) 1 else 3
        for (tile in tiles) world.spotanimMap(CerberusSpots.pool, tile)
        boss.queue(CerberusQueue.step, delay, CerberusStep.Lava(fight, tiles, 0))
    }

    private fun lavaPulse(boss: Npc, step: CerberusStep.Lava) {
        val target = step.fight.target
        if (target.coords in step.tiles) {
            val shield = target.lefthand?.id in dragonShields
            val damage =
                when {
                    step.pulse == 0 -> 10
                    target.vars[varbits.super_antifire_potion] > 0 -> 11
                    shield ||
                        target.vars[varbits.antifire_potion] > 0 &&
                            target.vars[varbits.protect_from_magic] != 0 -> 12
                    else -> 15
                }
            target.queueHit(boss, 1, HitType.Typeless, damage)
        }
        if (step.pulse < 3) boss.queue(CerberusQueue.step, 2, step.copy(pulse = step.pulse + 1))
    }

    private fun inArena(boss: Npc, target: Player): Boolean {
        if (!target.isSlotAssigned || target.hitpoints <= 0) return false
        val room = CerberusRoom.entries.firstOrNull { it.spawn == boss.spawnCoords } ?: return false
        return target.coords.level == room.spawn.level &&
            target.coords.x in room.spawn.x - 17..room.spawn.x + 19 &&
            target.coords.z in room.portcullis.z + 1..room.spawn.z + 15
    }

    private fun end(boss: Npc) {
        val fight = fights.remove(boss) ?: return
        for (soul in fight.spirits) if (soul.isSlotAssigned) npcs.del(soul, Int.MAX_VALUE)
        boss.clearQueue(CerberusQueue.step)
    }

    private companion object {
        val dragonShields = setOf(1540, 11283, 11284, 21633, 21634, 22002, 22003)
    }
}
