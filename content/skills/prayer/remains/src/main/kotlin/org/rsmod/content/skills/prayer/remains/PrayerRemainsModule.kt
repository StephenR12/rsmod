package org.rsmod.content.skills.prayer.remains

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class PrayerRemainsModule : PluginModule() {
    override fun bind() {
        bind(PrayerRemains::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerAltarOffering::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerAltarRestoration::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerHazardousAltarRestoration::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerElidinisStatuetteRestoration::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerSpellbookAltar::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerMageArenaStatues::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerTrappedChaosAltar::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerBrokenSunAltar::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerGoblinTempleAltar::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerGodWarsAlignment::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerGodWarsAltarRestoration::class.java).`in`(Scopes.SINGLETON)
        bind(PrayerGodWarsAltarTeleport::class.java).`in`(Scopes.SINGLETON)
    }
}
