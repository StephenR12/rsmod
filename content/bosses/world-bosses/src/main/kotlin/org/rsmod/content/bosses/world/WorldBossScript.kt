package org.rsmod.content.bosses.world

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.feet
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStateRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WorldBossScript
@Inject
constructor(
    private val controller: WorldBossController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val players: PlayerRepository,
    private val binding: BindingStateRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        WorldBosses.definitions.forEach { definition ->
            onAiOpPlayer2(definition.npc) { attack(this, it.target, definition) }
            onAiApPlayer2(definition.npc) { attack(this, it.target, definition) }
            onNpcHit(definition.npc) { controller.onHit(npc, hit, definition) }
        }
        listOf(
                WorldBossNpcs.sarachnisMeleeSpawn,
                WorldBossNpcs.sarachnisMagicSpawn,
                WorldBossNpcs.giantRat,
            )
            .forEach { type ->
                onAiOpPlayer2(type) { minionAttack(this, it.target) }
                onAiApPlayer2(type) { minionAttack(this, it.target) }
            }
        onEvent<org.rsmod.api.npc.events.NpcKillEvents.Credited> {
            if (npc.id == WorldBossNpcs.sarachnis.id) controller.removeSpawns(npc)
        }
        onEvent<NpcStateEvents.Respawn> {
            WorldBosses.byId[npc.id]?.let { configure(npc, it).also { controller.reset(npc) } }
        }
        WorldBosses.definitions.forEach { definition ->
            val existing = npcs.findAll(definition.spawn).firstOrNull { it.id == definition.npc.id }
            val boss =
                existing
                    ?: Npc(npcTypes[definition.npc], definition.spawn).also {
                        npcs.add(it, Int.MAX_VALUE)
                    }
            configure(boss, definition)
            controller.reset(boss)
        }
    }

    private fun attack(access: StandardNpcAccess, target: Player, definition: WorldBossDefinition) {
        with(access) {
            if (actionDelay > mapClock) return
            actionDelay = mapClock + 4
            val state = controller.state(npc)
            state.attacks++
            if (definition.family == WorldBossFamily.Sarachnis) {
                val nearby =
                    players
                        .findAll(ZoneKey.from(npc.coords), 3)
                        .filter { p ->
                            p.coords.level == 0 &&
                                p.coords.x in 1831..1851 &&
                                p.coords.z in 9891..9911 &&
                                p.hitpoints > 0
                        }
                        .toList()
                if (state.attacks % 5 == 0) {
                    for (p in nearby) if (p.feet?.id != 29806) binding.bind(p, npc, 6, 0)
                    val landings =
                        listOf(
                                CoordGrid(1837, 9897),
                                CoordGrid(1844, 9897),
                                CoordGrid(1837, 9904),
                                CoordGrid(1844, 9904),
                            )
                            .filter { npc.coords.chebyshevDistance(it) > 2 }
                    walk(landings[random.of(landings.size)])
                    return
                }
                val melee = isWithinDistance(target, 1)
                for (p in if (melee) listOf(target) else nearby) {
                    val accurate =
                        if (melee) accuracy.rollMeleeAccuracy(npc, p, MeleeAttackType.Crush, random)
                        else accuracy.rollRangedAccuracy(npc, p, random)
                    val praying =
                        p.vars[
                                if (melee) varbits.protect_from_melee
                                else varbits.protect_from_missiles] != 0
                    val damage = if (accurate && !praying) random.of(0..31) else 0
                    if (accurate && !praying)
                        npc.hitpoints =
                            (npc.hitpoints + if (melee) 5 else 10).coerceAtMost(
                                npc.baseHitpointsLvl
                            )
                    p.queueHit(
                        npc,
                        if (melee) 1 else 2,
                        if (melee) HitType.Melee else HitType.Ranged,
                        damage,
                    )
                }
                return
            }
            val style =
                when (definition.family) {
                    WorldBossFamily.Archaeologist -> HitType.Magic
                    WorldBossFamily.Sarachnis ->
                        if (isWithinDistance(target, 1)) HitType.Melee else HitType.Magic
                    WorldBossFamily.Scurrius ->
                        when (state.attacks % 3) {
                            0 -> HitType.Melee
                            1 -> HitType.Ranged
                            else -> HitType.Magic
                        }
                    WorldBossFamily.Maledictus ->
                        if (state.attacks % 2 == 0) HitType.Ranged else HitType.Magic
                }
            if (style == HitType.Melee && !isWithinDistance(target, 1)) return
            val special = state.attacks % 5 == 0
            val hitType =
                if (special && definition.family != WorldBossFamily.Sarachnis) HitType.Typeless
                else style
            val maxHit = if (special) definition.maxHit else (definition.maxHit * 3 / 4)
            target.queueHit(
                npc,
                if (style == HitType.Melee) 1 else 2,
                hitType,
                damage(target, style, maxHit),
            )
        }
    }

    private fun minionAttack(access: StandardNpcAccess, target: Player) {
        with(access) {
            val magic = npc.id == WorldBossNpcs.sarachnisMagicSpawn.id
            if (actionDelay > mapClock || !isWithinDistance(target, if (magic) 8 else 1)) return
            val meleeSpawn = npc.id == WorldBossNpcs.sarachnisMeleeSpawn.id
            actionDelay = mapClock + if (meleeSpawn) 3 else 4
            when {
                magic -> target.queueHit(npc, 2, HitType.Magic, damage(target, HitType.Magic, 11))
                meleeSpawn ->
                    target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, 13))
                else -> target.queueHit(npc, 1, HitType.Melee, random.of(0..8))
            }
        }
    }

    private fun StandardNpcAccess.damage(target: Player, style: HitType, maxHit: Int): Int {
        val accurate =
            when (style) {
                HitType.Melee ->
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
                HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
                HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                HitType.Typeless -> true
            }
        return if (accurate) random.of(0..maxHit) else 0
    }

    private fun configure(npc: Npc, definition: WorldBossDefinition) {
        npc.apply {
            hitpoints = definition.hitpoints
            baseHitpointsLvl = definition.hitpoints
            attackLvl = definition.attack
            baseAttackLvl = definition.attack
            strengthLvl = definition.strength
            baseStrengthLvl = definition.strength
            defenceLvl = definition.defence
            baseDefenceLvl = definition.defence
            magicLvl = definition.magic
            baseMagicLvl = definition.magic
            rangedLvl = definition.ranged
            baseRangedLvl = definition.ranged
            mode = NpcMode.Wander
            setHunt(18)
            setHuntMode(huntmodes.constant_ranged)
        }
    }
}
