package org.rsmod.content.skills.magic.spell.utility.standard

import kotlin.math.min

/** Integer health-transfer rules used by cache-233 Lunar Heal Other. */
public object LunarHealOther {
    /** The spell requires current health to be at least 11% of base Hitpoints. */
    public fun hasRequiredHealth(current: Int, base: Int): Boolean {
        require(current >= 0) { "Current Hitpoints must not be negative." }
        require(base > 0) { "Base Hitpoints must be positive." }
        return current.toLong() * 100 >= base.toLong() * MINIMUM_HEALTH_PERCENT
    }

    /** Floors 75% of current health, then caps the actual transfer to the recipient's deficit. */
    public fun transfer(current: Int, recipientCurrent: Int, recipientBase: Int): Int {
        require(current >= 0) { "Current Hitpoints must not be negative." }
        require(recipientCurrent >= 0) { "Recipient Hitpoints must not be negative." }
        require(recipientBase > 0) { "Recipient base Hitpoints must be positive." }
        val available = current.toLong() * TRANSFER_PERCENT / 100
        val missing = (recipientBase - recipientCurrent).coerceAtLeast(0)
        return min(available, missing.toLong()).toInt()
    }

    private const val MINIMUM_HEALTH_PERCENT = 11L
    private const val TRANSFER_PERCENT = 75L
}
