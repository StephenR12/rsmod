package org.rsmod.content.minigames.cox

import kotlin.random.Random

/** Cache-233.4 Chambers room selection. Challenge mode uses every encounter in fixed order. */
public object CoxLayout {
    public val challengeRoute: List<CoxRoom> =
        listOf(
            CoxRoom.Tekton,
            CoxRoom.Crabs,
            CoxRoom.IceDemon,
            CoxRoom.Shamans,
            CoxRoom.Vanguards,
            CoxRoom.Thieving,
            CoxRoom.Vasa,
            CoxRoom.Guardians,
            CoxRoom.Vespula,
            CoxRoom.Tightrope,
            CoxRoom.Muttadiles,
            CoxRoom.Mystics,
            CoxRoom.Prep,
            CoxRoom.Olm,
        )

    private val normalPool = challengeRoute.filter { it != CoxRoom.Prep && it != CoxRoom.Olm }

    public fun generate(seed: Long, challenge: Boolean): List<CoxRoom> {
        if (challenge) return challengeRoute
        val shuffled = normalPool.shuffled(Random(seed))
        val combat = shuffled.filter(CoxRoom::combat).take(3)
        val puzzle = shuffled.filterNot(CoxRoom::combat).take(2)
        return (combat + puzzle).shuffled(Random(seed xor -7046029254386353131L)) +
            listOf(CoxRoom.Prep, CoxRoom.Olm)
    }
}

public enum class CoxRoom(public val combat: Boolean) {
    Tekton(true),
    Vasa(true),
    Vanguards(true),
    Vespula(true),
    Muttadiles(true),
    Shamans(true),
    Mystics(true),
    Guardians(true),
    IceDemon(true),
    Crabs(false),
    Tightrope(false),
    Thieving(false),
    Prep(false),
    Olm(true),
}
