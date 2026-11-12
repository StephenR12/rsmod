package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PrayerRemainsScript
@Inject
constructor(
    private val remains: PrayerRemains,
    private val altarOffering: PrayerAltarOffering,
    private val altarRestoration: PrayerAltarRestoration,
    private val hazardousAltarRestoration: PrayerHazardousAltarRestoration,
    private val elidinisStatuetteRestoration: PrayerElidinisStatuetteRestoration,
    private val spellbookAltar: PrayerSpellbookAltar,
    private val mageArenaStatues: PrayerMageArenaStatues,
    private val trappedChaosAltar: PrayerTrappedChaosAltar,
    private val brokenSunAltar: PrayerBrokenSunAltar,
    private val goblinTempleAltar: PrayerGoblinTempleAltar,
    private val godWarsAltarRestoration: PrayerGodWarsAltarRestoration,
    private val godWarsAltarTeleport: PrayerGodWarsAltarTeleport,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in PrayerRemainsRecipes.all) {
            onOpHeld1(recipe.obj) { remains.offer(this, it.slot, recipe) }
        }
        onPlayerQueue(prayer_remains_queues.boneCooldown) {}
        onPlayerQueue(prayer_remains_queues.ashCooldown) {}
        onPlayerQueue(prayer_remains_queues.altarManualCooldown) {}
        onPlayerQueueWithArgs<PrayerAltarOfferingRequest>(prayer_remains_queues.altarOffering) {
            altarOffering.process(this, it.args)
        }
        onPlayerLogin { godWarsAltarRestoration.syncClientTimer(player) }

        for (altar in prayer_altar_locs.chapel + prayer_altar_locs.chaos) {
            for (recipe in PrayerRemainsRecipes.all.filter { it.kind == PrayerRemainsKind.Bone }) {
                onOpLocU(altar, recipe.obj) {
                    altarOffering.start(this, it.loc, it.invSlot, recipe)
                }
            }
        }

        for (altar in prayer_altar_locs.standardRestoration) {
            onOpLoc1(altar) { altarRestoration.restore(this, it.loc) }
        }
        for (altar in prayer_altar_locs.boostedRestoration) {
            onOpLoc1(altar) { altarRestoration.restore(this, it.loc, bonus = 2) }
        }
        onOpLoc1(prayer_altar_locs.cursedAltar) { hazardousAltarRestoration.curse(this, it.loc) }
        onOpLoc1(prayer_altar_locs.darkmeyerStatue) {
            hazardousAltarRestoration.darkmeyer(this, it.loc)
        }
        onOpLoc1(prayer_altar_locs.elidinisStatuette) {
            elidinisStatuetteRestoration.restore(this, it.loc)
        }
        onOpLoc2(prayer_altar_locs.astralAltar) { spellbookAltar.astral(this, it.loc) }
        onOpLoc1(prayer_altar_locs.ancientPyramidAltar) {
            spellbookAltar.ancientPyramid(this, it.loc)
        }
        onOpLoc1(prayer_altar_locs.mageArenaSaradominStatue) {
            mageArenaStatues.pray(this, it.loc, objs.saradomin_cape)
        }
        onOpLoc1(prayer_altar_locs.mageArenaGuthixStatue) {
            mageArenaStatues.pray(this, it.loc, objs.guthix_cape)
        }
        onOpLoc1(prayer_altar_locs.mageArenaZamorakStatue) {
            mageArenaStatues.pray(this, it.loc, objs.zamorak_cape)
        }
        onOpLoc1(prayer_altar_locs.trappedChaosAltar) { trappedChaosAltar.pray(this, it.loc) }
        onOpLoc1(prayer_altar_locs.brokenSunAltar) { brokenSunAltar.pray(this, it.loc) }
        for (altar in prayer_altar_locs.goblinTempleAltars) {
            onOpLoc1(altar) { goblinTempleAltar.pray(this, it.loc) }
        }
        for ((altar, god) in prayer_altar_locs.godWarsByGod) {
            onOpLoc1(altar) { godWarsAltarRestoration.restore(this, it.loc, god) }
            onOpLoc2(altar) { godWarsAltarTeleport.teleport(this, god) }
        }
    }
}
