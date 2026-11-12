package org.rsmod.content.skills.magic.spell.attacks.arceuus

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.min
import org.rsmod.api.combat.commons.magic.MarkOfDarknessStateRepository
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.hitmark_groups
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.magic.WardOfArceuusStateRepository
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerHit
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.corruptionCooldown by intVarBit(varbits.arceuus_corruption_cooldown)
private var Player.corruptionArmedTier by intVarBit(varbits.arceuus_corruption)
private var Player.corruptionBaseDamage by intVarBit(varbits.corruption_base_damage)
private var Player.corruptionCurrentIterations by intVarBit(varbits.corruption_current_iterations)
private var Player.corruptionTargetIterations by intVarBit(varbits.corruption_target_iterations)

public enum class CorruptionTier(
    public val spell: ObjType,
    public val component: ComponentType,
    public val castSpot: SpotanimType,
    public val hitSpot: SpotanimType,
    public val baseDrain: Int,
    public val clientValue: Int,
) {
    Lesser(
        spell = objs.spell_lesser_corruption,
        component = components.magic_spellbook_lesser_corruption,
        castSpot = spotanims.lesser_corruption_cast,
        hitSpot = spotanims.lesser_corruption_hit,
        baseDrain = 1,
        clientValue = 1,
    ),
    Greater(
        spell = objs.spell_greater_corruption,
        component = components.magic_spellbook_greater_corruption,
        castSpot = spotanims.greater_corruption_cast,
        hitSpot = spotanims.greater_corruption_hit,
        baseDrain = 2,
        clientValue = 2,
    ),
}

@Singleton
public class CorruptionStateRepository {
    private val armed = WeakHashMap<Player, CorruptionTier>()
    private val statuses = WeakHashMap<Player, DrainState>()

    public fun arm(player: Player, tier: CorruptionTier) {
        armed[player] = tier
        player.corruptionArmedTier = tier.clientValue
    }

    public fun armedTier(player: Player): CorruptionTier? = armed[player]

    public fun clearArmed(player: Player) {
        armed.remove(player)
        player.corruptionArmedTier = 0
    }

    public fun isCorrupted(player: Player): Boolean = player in statuses

    public fun apply(player: Player, tier: CorruptionTier, interval: Int) {
        require(interval > 0) { "Corruption interval must be positive: $interval" }
        check(!isCorrupted(player)) { "Corruption cannot stack on a player." }
        statuses[player] = DrainState(tier, interval, iteration = 0)
        player.corruptionBaseDamage = tier.baseDrain
        player.corruptionCurrentIterations = 0
        player.corruptionTargetIterations = DRAIN_COUNT
        player.softTimer(timers.magic_arceuus_corruption_drain, interval)
    }

    public fun drain(player: Player): Int? {
        val state = statuses[player] ?: return null
        val amount = state.tier.baseDrain * (state.iteration + 1)
        val drained = min(player.stat(stats.prayer), amount)
        player.statSub(stats.prayer, constant = amount, percent = 0)
        state.iteration++
        player.corruptionCurrentIterations = state.iteration
        if (state.iteration >= DRAIN_COUNT) {
            clearStatus(player)
        } else {
            player.softTimer(timers.magic_arceuus_corruption_drain, state.interval)
        }
        return drained
    }

    public fun clearStatus(player: Player) {
        statuses.remove(player)
        player.corruptionBaseDamage = 0
        player.corruptionCurrentIterations = 0
        player.corruptionTargetIterations = 0
        player.clearSoftTimer(timers.magic_arceuus_corruption_drain)
    }

    public fun statusFor(player: Player): DrainState? = statuses[player]

    public data class DrainState(
        public val tier: CorruptionTier,
        public val interval: Int,
        public var iteration: Int,
    )

    private companion object {
        const val DRAIN_COUNT = 3
    }
}

public class CorruptionSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: CorruptionStateRepository,
) {
    public fun cast(access: ProtectedAccess, tier: CorruptionTier) {
        with(access) {
            if (!canAccess() || player.corruptionCooldown != 0) {
                return
            }
            val spell = requireNotNull(spells.getObjSpell(tier.spell))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.corruption_cast)
            spotanim(tier.castSpot)
            states.arm(player, tier)
            player.corruptionCooldown = 1
            player.softTimer(timers.magic_arceuus_corruption_cooldown, COOLDOWN_CYCLES)
        }
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.a_kingdom_divided] < A_KINGDOM_DIVIDED_COMPLETE) {
            mes("You need to complete A Kingdom Divided to cast this spell.")
            return false
        }
        return true
    }

    public companion object {
        public const val COOLDOWN_CYCLES = 50
        private const val A_KINGDOM_DIVIDED_COMPLETE = 150
    }
}

public class CorruptionSpellScript
@Inject
constructor(
    private val spells: CorruptionSpells,
    private val states: CorruptionStateRepository,
    private val mark: MarkOfDarknessStateRepository,
    private val ward: WardOfArceuusStateRepository,
    private val players: PlayerList,
    private val random: GameRandom,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (tier in CorruptionTier.entries) {
            onIfOverlayButton(tier.component) {
                protectedAccess.launch(player) { spells.cast(this, tier) }
            }
        }
        onPlayerSoftTimer(timers.magic_arceuus_corruption_cooldown) {
            player.corruptionCooldown = 0
        }
        onPlayerSoftTimer(timers.magic_arceuus_corruption_drain) {
            val drained = states.drain(player) ?: return@onPlayerSoftTimer
            val hitmark = hitmark_groups.corruption.lit.id
            player.showHitmark(
                Hitmark.fromNoSource(
                    self = hitmark,
                    source = hitmark,
                    public = hitmark,
                    damage = drained,
                    delay = 0,
                )
            )
        }
        onPlayerHit {
            if (!hit.successful || !hit.isFromPlayer) {
                return@onPlayerHit
            }
            val source = hit.resolvePlayerSource(players) ?: return@onPlayerHit
            val tier = states.armedTier(source) ?: return@onPlayerHit
            if (ward.isActive(player) || states.isCorrupted(player)) {
                return@onPlayerHit
            }

            states.clearArmed(source)
            val marked = mark.isActive(source)
            val chance = if (marked) MARKED_PROC_CHANCE else BASE_PROC_CHANCE
            if (random.of(100) >= chance) {
                return@onPlayerHit
            }

            val interval = if (marked) MARKED_DRAIN_INTERVAL else BASE_DRAIN_INTERVAL
            states.apply(player, tier, interval)
            player.spotanim(tier.hitSpot)
            source.mes("Your target has been corrupted.")
            player.mes("You have been corrupted.")
        }
    }

    private companion object {
        const val BASE_PROC_CHANCE = 50
        const val MARKED_PROC_CHANCE = 100
        const val BASE_DRAIN_INTERVAL = 10
        const val MARKED_DRAIN_INTERVAL = 5
    }
}
