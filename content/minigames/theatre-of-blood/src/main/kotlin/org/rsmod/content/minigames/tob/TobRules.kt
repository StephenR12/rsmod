package org.rsmod.content.minigames.tob

public enum class TobMode(
    public val displayName: String,
    public val damagePercent: Int,
    public val uniqueDenominatorTenths: Int?,
) {
    Entry("Entry Mode", 50, null),
    Normal("Normal Mode", 100, 91),
    Hard("Hard Mode", 115, 77),
}

public enum class TobRoom(public val displayName: String) {
    Maiden("The Maiden of Sugadinti"),
    Bloat("Pestilent Bloat"),
    Nylocas("Nylocas Vasilias"),
    Sotetseg("Sotetseg"),
    Xarpus("Xarpus"),
    Verzik("Verzik Vitur"),
}

public object TobRules {
    public val route: List<TobRoom> = TobRoom.entries

    public fun commonLootPercent(mode: TobMode): Int =
        when (mode) {
            TobMode.Entry -> 20
            TobMode.Normal -> 100
            TobMode.Hard -> 115
        }

    public fun canRollUnique(mode: TobMode): Boolean = mode.uniqueDenominatorTenths != null
}
