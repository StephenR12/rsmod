package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.walktriggers
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

public enum class BindingStatus {
    Available,
    Held,
    Immune,
}

public data class BindingState(val source: PathingEntity, val heldUntil: Int, val immuneUntil: Int)

internal data class BindingImpact(val source: Player, val duration: Int)

internal data class BindingWarning(val heldUntil: Int)

/** Shared binding state for Standard, Ancient, and Arceuus movement-restricting spells. */
@Singleton
public class BindingStateRepository {
    private val states = WeakHashMap<PathingEntity, BindingState>()

    fun status(target: PathingEntity): BindingStatus {
        val clock = target.currentMapClock
        val state = states[target] ?: return BindingStatus.Available

        if (clock < state.heldUntil && remainsLinked(target, state)) {
            return BindingStatus.Held
        }

        if (clock < state.heldUntil) {
            val released =
                state.copy(heldUntil = clock, immuneUntil = clock + REFREEZE_IMMUNITY_CYCLES)
            states[target] = released
            target.clearBindingWalkTrigger()
            return BindingStatus.Immune
        }

        if (clock < state.immuneUntil) {
            target.clearBindingWalkTrigger()
            return BindingStatus.Immune
        }

        states.remove(target)
        target.clearBindingWalkTrigger()
        return BindingStatus.Available
    }

    fun bind(
        target: PathingEntity,
        source: PathingEntity,
        duration: Int,
        immunityDuration: Int = REFREEZE_IMMUNITY_CYCLES,
    ): BindingState {
        require(duration > 0) { "Binding duration must be positive: $duration" }
        require(immunityDuration >= 0) {
            "Binding immunity duration must not be negative: $immunityDuration"
        }
        val state =
            BindingState(
                source = source,
                heldUntil = target.currentMapClock + duration,
                immuneUntil = target.currentMapClock + duration + immunityDuration,
            )
        states[target] = state
        target.abortRoute()
        if (target is Player) {
            target.clearMapFlag()
        }
        target.walkTrigger(if (target is Player) walktriggers.pvp_frozen else walktriggers.frozen)
        return state
    }

    fun blockMovement(target: PathingEntity) {
        if (status(target) != BindingStatus.Held) {
            return
        }
        target.abortRoute()
        if (target is Player) {
            target.clearMapFlag()
        }
        target.walkTrigger(if (target is Player) walktriggers.pvp_frozen else walktriggers.frozen)
    }

    internal fun isCurrent(target: PathingEntity, warning: BindingWarning): Boolean =
        status(target) == BindingStatus.Held && states[target]?.heldUntil == warning.heldUntil

    public fun stateFor(target: PathingEntity): BindingState? = states[target]

    private fun remainsLinked(target: PathingEntity, state: BindingState): Boolean {
        val source = state.source
        if (source !is Player || target !is Player) {
            return true
        }
        return source.isSlotAssigned && source.isVisible && source.isWithinDistance(target, 10)
    }

    private fun PathingEntity.clearBindingWalkTrigger() {
        if (walkTrigger == walktriggers.frozen || walkTrigger == walktriggers.pvp_frozen) {
            clearWalkTrigger()
        }
    }

    companion object {
        const val REFREEZE_IMMUNITY_CYCLES: Int = 5
    }
}
