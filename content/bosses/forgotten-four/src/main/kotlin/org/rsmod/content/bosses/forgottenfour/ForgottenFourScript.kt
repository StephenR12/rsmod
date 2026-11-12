package org.rsmod.content.bosses.forgottenfour

import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.manager.MagicSpellEvents
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ForgottenFourScript
@Inject
constructor(
    private val controller: ForgottenFourController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val players: PlayerList,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        ForgottenFour.definitions.forEach { definition ->
            onAiOpPlayer2(definition.npc) { attack(this, it.target, definition) }
            onAiApPlayer2(definition.npc) { attack(this, it.target, definition) }
            onModifyNpcHit(definition.npc) { controller.modifyHit(npc, hit, definition) }
            onNpcHit(definition.npc) {
                if (!hit.isFromPlayer) return@onNpcHit
                val player = hit.resolvePlayerSource(players) ?: return@onNpcHit
                controller.onBossDamaged(npc, player, definition)
            }
        }
        minionTypes.forEach { type ->
            onAiOpPlayer2(type) { minionAttack(this, it.target, type) }
            onAiApPlayer2(type) { minionAttack(this, it.target, type) }
        }
        onEvent<MagicSpellEvents.Cast> {
            if (spell.obj in SHADOW_SPELLS) controller.rememberShadowCast(player)
        }
        onEvent<NpcKillEvents.Credited> { controller.onMinionKilled(npc) }
        onEvent<NpcStateEvents.Respawn> {
            ForgottenFour.byId[npc.id]?.let { definition ->
                configure(npc, definition)
                controller.reset(npc)
            }
        }
        ForgottenFour.definitions.forEach { definition ->
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

    private fun attack(
        access: StandardNpcAccess,
        target: Player,
        definition: ForgottenBossDefinition,
    ) {
        if (!controller.claim(access.npc, target)) {
            access.resetMode()
            return
        }
        when (definition.family) {
            ForgottenBossFamily.Leviathan -> leviathanAttack(access, target, definition)
            ForgottenBossFamily.Whisperer -> whispererAttack(access, target, definition)
            ForgottenBossFamily.Vardorvis -> vardorvisAttack(access, target, definition)
            ForgottenBossFamily.Duke -> dukeAttack(access, target, definition)
        }
    }

    private fun leviathanAttack(
        access: StandardNpcAccess,
        target: Player,
        definition: ForgottenBossDefinition,
    ) {
        with(access) {
            if (actionDelay > mapClock) return
            val state = controller.state(npc)
            if (state.leviathanStunned) {
                actionDelay = mapClock + 1
                return
            }
            if (npc.hitpoints <= ForgottenFourController.LEVIATHAN_ENRAGE_HP) {
                state.enraged = true
                controller.spawnLeviathanPathfinder(npc)
            }
            if (!state.enraged && state.leviathanVolleyAttack >= LEVIATHAN_VOLLEY_LENGTH) {
                state.leviathanVolleyAttack = 0
                state.leviathanVolley = (state.leviathanVolley + 1).coerceAtMost(6)
                actionDelay = mapClock + 4
                if (state.leviathanSpecials++ % 2 == 0) {
                    leviathanLightning(access, target)
                } else {
                    leviathanSmoke(access, target)
                }
                return
            }

            val style =
                if (state.enraged || state.leviathanVolley < 2) {
                    random.pick(HitType.Ranged, HitType.Magic)
                } else {
                    random.pick(HitType.Ranged, HitType.Magic, HitType.Melee)
                }
            val rate =
                if (state.enraged) 2 else LEVIATHAN_RATES[state.leviathanVolley.coerceIn(0, 6)]
            actionDelay = mapClock + rate
            state.leviathanVolleyAttack++
            anim(ForgottenFourSeqs.leviathanAttack)
            target.spotanim(
                when (style) {
                    HitType.Ranged -> ForgottenFourSpotanims.leviathanRanged
                    HitType.Magic -> ForgottenFourSpotanims.leviathanMagic
                    else -> ForgottenFourSpotanims.leviathanMelee
                }
            )
            val inPath = state.pathfinder?.let { distance(target.coords, it.coords) <= 2 } == true
            val hitType =
                if (state.enraged && !inPath && target.isProtected(style)) HitType.Typeless
                else style
            val maximum = if (state.enraged) definition.maxHit else definition.maxHit * 3 / 4
            target.queueHit(npc, delay = 2, type = hitType, damage = damage(target, style, maximum))
        }
    }

    private fun leviathanLightning(access: StandardNpcAccess, target: Player) {
        val source = access.npc
        access.anim(ForgottenFourSeqs.leviathanLightning)
        protectedAccess.launch(target) {
            repeat(8) {
                val threatened = player.coords
                delay(1)
                if (source.slotId != INVALID_SLOT && player.coords == threatened) {
                    player.spotanim(ForgottenFourSpotanims.leviathanExplosion)
                    player.queueHit(source, 1, HitType.Typeless, random.of(8..20))
                    controller.markMistake(source)
                }
            }
        }
    }

    private fun leviathanSmoke(access: StandardNpcAccess, target: Player) {
        val source = access.npc
        val start = target.coords
        access.anim(ForgottenFourSeqs.leviathanSmoke)
        protectedAccess.launch(target) {
            repeat(10) {
                val threatened = player.coords
                delay(1)
                if (source.slotId != INVALID_SLOT && player.coords == threatened) {
                    player.queueHit(source, 1, HitType.Typeless, random.of(3..8))
                    controller.markMistake(source)
                }
            }
            if (source.slotId != INVALID_SLOT && distance(player.coords, start) < 3) {
                player.queueHit(source, 1, HitType.Typeless, random.of(35..59))
                controller.markMistake(source)
            }
        }
    }

    private fun whispererAttack(
        access: StandardNpcAccess,
        target: Player,
        definition: ForgottenBossDefinition,
    ) {
        with(access) {
            if (actionDelay > mapClock) return
            val state = controller.state(npc)
            val special = state.pendingSpecial
            if (special != null && !state.enraged) {
                state.pendingSpecial = null
                actionDelay = mapClock + 8
                whispererSpecial(access, target, special)
                return
            }
            actionDelay = mapClock + if (state.enraged) 2 else 4
            val styles = whispererStyles(state)
            styles.forEachIndexed { index, style ->
                anim(
                    if (style == HitType.Magic) ForgottenFourSeqs.whispererMagic
                    else ForgottenFourSeqs.whispererRanged
                )
                target.queueHit(
                    npc,
                    delay = index + 1,
                    type = style,
                    damage = damage(target, style, if (style == HitType.Magic) 36 else 40),
                )
                sanityAfterProjectile(npc, target, style, index + 1)
            }
            controller.adjustSanity(target, 1)
            whispererTentacles(npc, target, if (state.enraged) 1 else 4)
            state.enrageAttacks++
        }
    }

    private fun whispererStyles(state: ForgottenBossState): List<HitType> {
        if (state.enraged) {
            val style = if ((state.enrageAttacks / 2) % 2 == 0) HitType.Ranged else HitType.Magic
            return listOf(style)
        }
        val first = random.pick(HitType.Ranged, HitType.Magic)
        val other = if (first == HitType.Ranged) HitType.Magic else HitType.Ranged
        return when (state.whispererSpecials) {
            0 -> listOf(first, first, first)
            1,
            2 -> listOf(first, first, other)
            else -> listOf(first, other, first)
        }
    }

    private fun sanityAfterProjectile(source: Npc, target: Player, style: HitType, delay: Int) {
        protectedAccess.launch(target) {
            delay(delay)
            if (source.slotId != INVALID_SLOT && !player.isProtected(style)) {
                loseSanity(source, player, 5)
            }
        }
    }

    private fun whispererTentacles(source: Npc, target: Player, count: Int) {
        val threatened = target.coords
        target.spotanim(ForgottenFourSpotanims.whispererTentacle)
        protectedAccess.launch(target) {
            delay(if (count == 1) 1 else 3)
            if (source.slotId != INVALID_SLOT && distance(player.coords, threatened) <= 1) {
                player.queueHit(source, 1, HitType.Typeless, 20)
                loseSanity(source, player, 20)
                controller.markMistake(source)
            }
        }
    }

    private fun whispererSpecial(
        source: StandardNpcAccess,
        target: Player,
        special: ForgottenSpecial,
    ) {
        when (special) {
            ForgottenSpecial.CorruptedSeeds -> corruptedSeeds(source.npc, target)
            ForgottenSpecial.Screech -> screech(source, target)
            ForgottenSpecial.SoulSiphon -> soulSiphon(source, target)
        }
    }

    private fun corruptedSeeds(source: Npc, target: Player) {
        val start = target.coords
        protectedAccess.launch(target) {
            delay(8)
            if (source.slotId == INVALID_SLOT) return@launch
            if (distance(player.coords, start) < 4) {
                player.queueHit(source, 1, HitType.Typeless, 75)
                loseSanity(source, player, 75)
                controller.markMistake(source)
            } else {
                controller.adjustSanity(player, 15)
            }
        }
    }

    private fun screech(source: StandardNpcAccess, target: Player) {
        val boss = source.npc
        val start = target.coords
        source.anim(ForgottenFourSeqs.whispererScreech)
        target.spotanim(ForgottenFourSpotanims.whispererScreech)
        protectedAccess.launch(target) {
            delay(6)
            if (boss.slotId != INVALID_SLOT && distance(player.coords, start) < 3) {
                player.queueHit(boss, 1, HitType.Typeless, 45)
                loseSanity(boss, player, 45)
                controller.markMistake(boss)
            }
        }
    }

    private fun soulSiphon(source: StandardNpcAccess, target: Player) {
        val boss = source.npc
        source.anim(ForgottenFourSeqs.whispererSouls)
        controller.spawnAdds(boss, List(12) { ForgottenFourNpcs.lostSoul }, hitpoints = 1)
        protectedAccess.launch(target) {
            delay(20)
            if (boss.slotId == INVALID_SLOT) return@launch
            if (!controller.finishSoulSiphon(boss)) {
                player.queueHit(boss, 1, HitType.Typeless, 50)
                loseSanity(boss, player, 50)
            } else {
                controller.adjustSanity(player, 15)
            }
        }
    }

    private fun loseSanity(source: Npc, player: Player, amount: Int) {
        if (controller.adjustSanity(player, -amount) == 0) {
            player.queueHit(source, 1, HitType.Typeless, max(99, player.hitpoints))
        }
    }

    private fun vardorvisAttack(
        access: StandardNpcAccess,
        target: Player,
        definition: ForgottenBossDefinition,
    ) {
        with(access) {
            if (actionDelay > mapClock) return
            controller.onBossDamaged(npc, target, definition)
            val state = controller.state(npc)
            actionDelay = mapClock + if (state.enraged) 3 else 4
            val attack = state.consumeAttack()
            if (attack >= 3 && random.of(3) == 0) {
                when (random.of(4)) {
                    0 -> vardorvisAxes(access, target, definition)
                    1 -> vardorvisSpikes(access, target)
                    2 -> vardorvisStrangle(access, target)
                    else -> vardorvisHead(access, target)
                }
                return
            }
            if (!isWithinDistance(target, 1)) return
            anim(ForgottenFourSeqs.vardorvisMelee)
            val scaledMax = 24 + 14 * (definition.hitpoints - npc.hitpoints) / definition.hitpoints
            target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, scaledMax))
        }
    }

    private fun vardorvisAxes(
        access: StandardNpcAccess,
        target: Player,
        definition: ForgottenBossDefinition,
    ) {
        val source = access.npc
        val count =
            when {
                source.hitpoints <= definition.hitpoints / 3 -> 3
                source.hitpoints <= definition.hitpoints - 10 -> 2 + random.of(2)
                else -> 1
            }
        val threatened = target.coords
        controller.spawnAdds(source, List(count) { ForgottenFourNpcs.vardorvisAxe }, 1, 8)
        protectedAccess.launch(target) {
            delay(3)
            if (source.slotId != INVALID_SLOT && distance(player.coords, threatened) <= 1) {
                player.queueHit(
                    source,
                    1,
                    HitType.Melee,
                    random.of(20..35),
                    modifier = ForgottenFourChipHitModifier,
                )
                controller.markMistake(source)
                repeat(5) {
                    delay(3)
                    player.queueHit(source, 1, HitType.Typeless, 3)
                }
            }
        }
    }

    private fun vardorvisSpikes(access: StandardNpcAccess, target: Player) {
        val source = access.npc
        val threatened = target.coords
        access.anim(ForgottenFourSeqs.vardorvisDash)
        target.spotanim(ForgottenFourSpotanims.vardorvisSpike)
        protectedAccess.launch(target) {
            delay(3)
            if (source.slotId != INVALID_SLOT && player.coords == threatened) {
                val damage = random.of(10..25)
                player.queueHit(source, 1, HitType.Typeless, damage)
                source.hitpoints =
                    (source.hitpoints + damage / 2).coerceAtMost(source.baseHitpointsLvl)
                controller.markMistake(source)
            }
        }
    }

    private fun vardorvisStrangle(access: StandardNpcAccess, target: Player) {
        val source = access.npc
        val start = target.coords
        access.anim(ForgottenFourSeqs.vardorvisStrangle)
        target.spotanim(ForgottenFourSpotanims.vardorvisSpores)
        protectedAccess.launch(target) {
            delay(4)
            if (source.slotId != INVALID_SLOT && distance(player.coords, start) < 2) {
                player.queueHit(source, 1, HitType.Typeless, random.of(15..25))
                controller.markMistake(source)
            }
        }
    }

    private fun vardorvisHead(access: StandardNpcAccess, target: Player) {
        val source = access.npc
        access.anim(ForgottenFourSeqs.vardorvisHead)
        val protected = target.isProtected(HitType.Ranged)
        target.queueHit(source, 2, HitType.Ranged, random.of(0..30))
        if (!protected) {
            protectedAccess.launch(target) {
                delay(2)
                vars[varbits.protect_from_melee] = 0
                vars[varbits.protect_from_missiles] = 0
                vars[varbits.protect_from_magic] = 0
                controller.markMistake(source)
            }
        }
    }

    private fun dukeAttack(
        access: StandardNpcAccess,
        target: Player,
        definition: ForgottenBossDefinition,
    ) {
        with(access) {
            if (actionDelay > mapClock) return
            val state = controller.state(npc)
            if (npc.hitpoints <= definition.hitpoints / 4) state.enraged = true
            val attack = state.consumeAttack()
            actionDelay = mapClock + if (state.enraged) 4 else 5
            if (attack % 5 == 0) {
                dukeGaze(access, target)
                return
            }
            val gasInterval = if (state.enraged) 3 else if (npc.hitpoints <= 330) 4 else 5
            if (attack > 5 && attack % gasInterval == 1) {
                dukeGas(access, target)
            }
            if (isWithinDistance(target, 2)) {
                anim(ForgottenFourSeqs.dukeMelee)
                target.spotanim(ForgottenFourSpotanims.dukeMelee)
                target.queueHit(
                    npc,
                    1,
                    HitType.Melee,
                    random.of(4..10),
                    modifier = ForgottenFourChipHitModifier,
                )
                target.queueHit(
                    npc,
                    2,
                    HitType.Melee,
                    damage(target, HitType.Melee, 50),
                    modifier = ForgottenFourChipHitModifier,
                )
            } else {
                anim(ForgottenFourSeqs.dukeMagic)
                target.spotanim(ForgottenFourSpotanims.dukeMagic)
                target.queueHit(
                    npc,
                    2,
                    HitType.Magic,
                    damage(target, HitType.Magic, 36),
                    modifier = ForgottenFourChipHitModifier,
                )
            }
        }
    }

    private fun dukeGaze(access: StandardNpcAccess, target: Player) {
        val source = access.npc
        val start = target.coords
        access.anim(ForgottenFourSeqs.dukeGaze)
        protectedAccess.launch(target) {
            delay(3)
            val movedBehindCover =
                distance(player.coords, start) >= 3 &&
                    player.coords.x != source.coords.x &&
                    player.coords.z != source.coords.z
            if (source.slotId != INVALID_SLOT && !movedBehindCover) {
                player.queueHit(source, 1, HitType.Typeless, random.of(70..99))
                controller.markMistake(source)
            }
        }
    }

    private fun dukeGas(access: StandardNpcAccess, target: Player) {
        val source = access.npc
        val threatened = target.coords
        controller.spawnAdds(source, listOf(ForgottenFourNpcs.dukeVent), 1, 12)
        protectedAccess.launch(target) {
            delay(3)
            repeat(3) {
                if (source.slotId != INVALID_SLOT && distance(player.coords, threatened) <= 1) {
                    player.queueHit(source, 1, HitType.Typeless, random.of(6..12))
                    controller.markMistake(source)
                }
                delay(2)
            }
        }
    }

    private fun minionAttack(access: StandardNpcAccess, target: Player, type: NpcType) {
        with(access) {
            if (actionDelay > mapClock) return
            if (type == ForgottenFourNpcs.leviathanPathfinder) return
            actionDelay = mapClock + 4
            val style =
                when (type.id) {
                    ForgottenFourNpcs.lostSoul.id,
                    ForgottenFourNpcs.dukeVent.id -> HitType.Magic
                    else -> HitType.Melee
                }
            if (style == HitType.Melee && !isWithinDistance(target, 1)) return
            target.queueHit(
                npc,
                if (style == HitType.Melee) 1 else 2,
                style,
                damage(target, style, 15),
            )
        }
    }

    private fun StandardNpcAccess.damage(target: Player, style: HitType, maxHit: Int): Int {
        val accurate =
            when (style) {
                HitType.Melee ->
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Slash, random)
                HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
                HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                HitType.Typeless -> true
            }
        return if (accurate) random.of(0..maxHit) else 0
    }

    private fun Player.isProtected(style: HitType): Boolean =
        when (style) {
            HitType.Melee -> vars[varbits.protect_from_melee] == 1
            HitType.Ranged -> vars[varbits.protect_from_missiles] == 1
            HitType.Magic -> vars[varbits.protect_from_magic] == 1
            HitType.Typeless -> false
        }

    private fun configure(npc: Npc, definition: ForgottenBossDefinition) {
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

    private fun distance(first: CoordGrid, second: CoordGrid): Int =
        max(abs(first.x - second.x), abs(first.z - second.z))

    public companion object {
        private const val LEVIATHAN_VOLLEY_LENGTH = 6
        private val LEVIATHAN_RATES = intArrayOf(3, 2, 2, 1, 1, 1, 1)
        private val SHADOW_SPELLS =
            setOf(
                objs.spell_shadow_rush,
                objs.spell_shadow_burst,
                objs.spell_shadow_blitz,
                objs.spell_shadow_barrage,
            )
        private val minionTypes =
            listOf(
                ForgottenFourNpcs.leviathanTailOne,
                ForgottenFourNpcs.leviathanTailTwo,
                ForgottenFourNpcs.leviathanTailThree,
                ForgottenFourNpcs.leviathanPathfinder,
                ForgottenFourNpcs.lostSoul,
                ForgottenFourNpcs.vardorvisHead,
                ForgottenFourNpcs.vardorvisAxe,
                ForgottenFourNpcs.dukeVent,
            )
    }
}

private object ForgottenFourChipHitModifier : PlayerHitModifier {
    override fun org.rsmod.game.hit.HitBuilder.modify(target: Player) {
        val protected =
            when (type) {
                HitType.Melee -> target.vars[varbits.protect_from_melee] == 1
                HitType.Ranged -> target.vars[varbits.protect_from_missiles] == 1
                HitType.Magic -> target.vars[varbits.protect_from_magic] == 1
                HitType.Typeless -> false
            }
        if (protected) damage /= 2
    }
}
