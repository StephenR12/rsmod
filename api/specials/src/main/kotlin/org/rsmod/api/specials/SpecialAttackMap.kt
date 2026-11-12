package org.rsmod.api.specials

public interface SpecialAttackMap {
    /**
     * Registration order for this map. Generic, metadata-driven maps should use a higher value so
     * bespoke implementations always win.
     */
    public val priority: Int
        get() = 0

    public fun SpecialAttackRepository.register(manager: SpecialAttackManager)
}
