package org.rsmod.content.skills.magic.spell.utility.standard

import kotlin.math.min

/** Integer health-transfer rules used by cache-233 Lunar Heal Group. */
public object LunarHealGroup {
    /** Floors the caster's documented 75% current-Hitpoints sacrifice. */
    public fun sacrifice(current: Int): Int {
        require(current >= 0) { "Current Hitpoints must not be negative." }
        return (current.toLong() * SACRIFICE_PERCENT / 100).toInt()
    }

    /**
     * Divides the sacrificed health plus the five-point bonus equally between recipients.
     *
     * Integer division deliberately discards the indivisible remainder. Each share is then capped
     * to that recipient's deficit; unused healing is not redistributed because doing so would no
     * longer be an equal split.
     */
    public fun healing(sacrifice: Int, recipientDeficits: List<Int>): List<Int> {
        require(sacrifice >= 0) { "Sacrifice must not be negative." }
        require(recipientDeficits.isNotEmpty()) { "At least one recipient is required." }
        require(recipientDeficits.size <= MAX_RECIPIENTS) {
            "Heal Group supports at most $MAX_RECIPIENTS recipients."
        }
        require(recipientDeficits.all { it > 0 }) { "Every recipient must be injured." }
        val share = (sacrifice + BONUS_HEAL) / recipientDeficits.size
        return recipientDeficits.map { min(share, it) }
    }

    public const val BONUS_HEAL: Int = 5
    public const val MAX_RECIPIENTS: Int = 5
    private const val SACRIFICE_PERCENT = 75L
}
