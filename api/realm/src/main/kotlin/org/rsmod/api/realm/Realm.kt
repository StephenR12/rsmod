package org.rsmod.api.realm

import org.rsmod.map.CoordGrid

public class Realm(public val name: String) {
    /**
     * Gets the current [RealmConfig] snapshot associated with this realm.
     *
     * **Important Note**: This value **can** and **will** be mutated through the application's
     * lifetime. It is imperative to realize this and avoid caching any values from the config, or
     * to take this fact into account if you do so.
     *
     * During the application's initial startup, this value will be inaccessible (to prevent misuse)
     * until [updateConfig] is called. Callers should only expect this value to be available _after_
     * the application services have started up.
     *
     * @throws IllegalStateException if [updateConfig] has not been called yet.
     */
    public val config: RealmConfig
        get() = getConfigOrThrow()

    @Volatile private lateinit var mutableConfig: RealmConfig

    /**
     * Updates the current [RealmConfig] associated with this realm.
     *
     * This method must only be called from the game thread to avoid race conditions with concurrent
     * readers.
     */
    public fun updateConfig(config: RealmConfig) {
        this.mutableConfig = config
    }

    private fun getConfigOrThrow(): RealmConfig {
        check(::mutableConfig.isInitialized) { "Realm config has not been initialized yet." }
        return mutableConfig
    }
}

/**
 * @param baseXpRate The base xp rate assigned to new players during registration.
 * @param globalXpRate The global xp modifier typically used for events like Double XP Weekends.
 *   This is usually compounded with each player's personal xp rate.
 * @param requireRegistration If `true`, players must have a pre-registered account in the database
 *   to log in. If `false`, new accounts will be automatically created on first login.
 * @param ignorePasswords If `true`, any password will be accepted for any account without
 *   verification. Note: This flag may only take effect in certain environments (e.g., [devMode] is
 *   `true`), depending on the login server's safeguards.
 * @param autoAssignDisplayNames If `true`, player display names will be automatically assigned
 *   based on their login name.
 * @param worldFlags Bit-packed properties of the world selected by the server configuration.
 */
public data class RealmConfig(
    val id: Int,
    val loginMessage: String?,
    val loginBroadcast: String?,
    val baseXpRate: Double,
    val globalXpRate: Double,
    val spawnCoord: CoordGrid,
    val respawnCoord: CoordGrid,
    val devMode: Boolean,
    val requireRegistration: Boolean,
    val ignorePasswords: Boolean,
    val autoAssignDisplayNames: Boolean,
    val worldFlags: Int,
) {
    public fun hasWorldFlag(flag: WorldFlag): Boolean = worldFlags and flag.mask != 0
}

/** Cache-233 world-list flags, shared by gameplay rules and the revision-matched client. */
public enum class WorldFlag(public val mask: Int) {
    Members(1),
    Pvp(1 shl 2),
    Bounty(1 shl 5),
    PvpArena(1 shl 6),
    SkillTotal(1 shl 7),
    QuestSpeedrunning(1 shl 8),
    HighRisk(1 shl 10),
    LastManStanding(1 shl 14),
    Beta(1 shl 16),
    LegacyOnly(1 shl 22),
    EocOnly(1 shl 23),
    NoSaveMode(1 shl 25),
    Tournament(1 shl 26),
    FreshStart(1 shl 27),
    Deadman(1 shl 29),
    Seasonal(1 shl 30),
}
