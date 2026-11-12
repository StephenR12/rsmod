package org.rsmod.api.specials

import jakarta.inject.Inject
import org.rsmod.api.specials.weapon.SpecialAttackWeapons

/** Loads special-attack cache mappings and registers every installed content definition. */
public class SpecialAttackBootstrap
@Inject
public constructor(
    private val repository: SpecialAttackRepository,
    private val manager: SpecialAttackManager,
    private val weapons: SpecialAttackWeapons,
    private val maps: Set<SpecialAttackMap>,
) {
    public fun startup() {
        weapons.startup()
        for (map in maps.sortedBy(SpecialAttackMap::priority)) {
            with(map) { repository.register(manager) }
        }
    }
}
