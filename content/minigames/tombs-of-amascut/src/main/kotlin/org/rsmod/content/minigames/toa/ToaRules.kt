package org.rsmod.content.minigames.toa

public enum class ToaMode(
    public val displayName: String,
    public val raidLevel: Int,
    public val attempts: Int,
) {
    Entry("Entry Mode", 0, Int.MAX_VALUE),
    Normal("Normal Mode", 150, 3),
    Expert("Expert Mode", 300, 1),
}

public enum class ToaPath(public val displayName: String) {
    Scabaras("Path of Scabaras"),
    Apmeken("Path of Apmeken"),
    Crondis("Path of Crondis"),
    Het("Path of Het"),
}

public enum class ToaEncounter {
    Nexus,
    ScabarasChallenge,
    Kephri,
    ApmekenChallenge,
    Baba,
    CrondisChallenge,
    Zebak,
    HetChallenge,
    Akkha,
    Wardens,
}

public object ToaRules {
    public fun statPercent(raidLevel: Int): Int = 100 + raidLevel * 100 / 250

    public fun damagePercent(raidLevel: Int): Int = statPercent(raidLevel).coerceAtMost(250)

    public fun modeFor(raidLevel: Int): ToaMode =
        when {
            raidLevel >= 300 -> ToaMode.Expert
            raidLevel >= 150 -> ToaMode.Normal
            else -> ToaMode.Entry
        }
}
