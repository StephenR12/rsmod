package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/** Events consumed by the cache-driven Combat Achievement system. */
public class CombatAchievementEvents {
    /**
     * Signals that game content has positively verified one exact task condition.
     *
     * Content should publish this only after every restriction in the task has been checked.
     */
    public data class TaskSatisfied(public val player: Player, public val taskId: Int) :
        UnboundEvent

    /**
     * Signals a completed encounter. The achievement plugin uses this for completion-count, mode,
     * raid-level, deathless, and speed tasks whose criteria are present in cache text.
     */
    public data class EncounterCompleted(
        public val player: Player,
        public val encounter: String,
        public val completionCount: Int = 1,
        public val mode: String? = null,
        public val deaths: Int = 0,
        public val durationTicks: Int? = null,
        public val raidLevel: Int? = null,
    ) : UnboundEvent
}
