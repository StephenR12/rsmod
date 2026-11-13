package org.rsmod.content.skills.mining.motherlode

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.MapClock
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

@Singleton
public class MotherlodeWorld
@Inject
constructor(private val locs: LocRepository, private val clock: MapClock) {
    private val depletedAt = mutableMapOf<CoordGrid, Int>()
    private val repairedUntil = IntArray(2)

    public fun flowing(): Boolean = repairedUntil.any { it > clock.cycle }

    public fun repair(loc: BoundLocInfo, rounds: Int): Boolean {
        val index = MlmArea.struts.indexOf(loc.coords)
        if (index < 0 || locs.findExact(loc.coords, MlmLocs.broken) == null) return false
        // Subject Strut14993536: 58-second rounds, break on round2/3/4. Equal weights are
        // unverified.
        val ticks = rounds.coerceIn(2, 4) * 97
        repairedUntil[index] = clock.cycle + ticks
        locs.change(loc, MlmLocs.fixed, ticks)
        locs.findExact(loc.coords.translate(1, -1), MlmLocs.wheelBroken)?.let {
            locs.change(it, MlmLocs.wheelFixed, ticks)
        }
        return true
    }

    public fun onPayDirt(loc: BoundLocInfo, duration: () -> Int) {
        val now = clock.cycle
        val upper = loc.coords in MlmArea.upper
        var expires = depletedAt[loc.coords]
        if (expires == null || upper && now >= expires + 100) {
            depletedAt[loc.coords] = now + duration()
            return
        }
        if (now < expires) return
        // A timed-out but unmined vein still yields one last pay-dirt. Lower respawns count elapsed
        // time.
        val index = MlmLocs.veins.indexOfFirst { it.id == loc.id }
        if (index < 0) return
        locs.change(
            loc,
            MlmLocs.depleted[index],
            if (upper) 100 else (100 - (now - expires)).coerceAtLeast(1),
        )
        depletedAt.remove(loc.coords)
    }
}
