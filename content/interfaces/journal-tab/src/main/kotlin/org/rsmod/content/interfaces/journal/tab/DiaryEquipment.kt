package org.rsmod.content.interfaces.journal.tab

import org.rsmod.api.player.isTeleportBlocked
import jakarta.inject.Inject
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DiaryEquipmentBits : VarBitReferences() {
    val morytania = find("morytania_legs_last_teleport")
    val wild = find("wilderness_sword_last_teleport")
    val prayer = find("falador_shield_recharge")
    val cabbage = find("lumbridge_cabbage_teleport")
    val energy = find("lumbridge_energy_restore")
    val alchs = find("lumbridge_free_alchs")
    val highAlchemy = find("lumbridge_alchemy_high")
    val nardah = find("desert_nardah_teleport")
    val sherlock = find("seers_sherlock_teleport")
    val piscatoris = find("western_pisc_teleport")
    val woodland = find("zeah_blessing_woodland_teleport")
    val karuulm = find("zeah_blessing_brimstone_teleport")
    val daily =
        listOf(
            morytania,
            wild,
            prayer,
            cabbage,
            energy,
            alchs,
            nardah,
            sherlock,
            piscatoris,
            woodland,
            karuulm,
        )
}

internal object DiaryEquipmentVars : VarpReferences() {
    val day = find("diary_equipment_day")
    val farm = find("diary_ardougne_farm_uses")
}

internal object DiaryEquipmentVarBuilds : VarpBuilder() {
    init {
        build("diary_equipment_day") { transmitNever = true }
        build("diary_ardougne_farm_uses") { transmitNever = true }
    }
}

internal fun ProtectedAccess.refreshDiaryEquipment(
    day: Int = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()
) {
    val previous = vars[DiaryEquipmentVars.day]
    if (previous == day) return
    if (previous != 0) {
        DiaryEquipmentBits.daily.forEach { vars[it] = 0 }
        vars[DiaryEquipmentVars.farm] = 0
    }
    vars[DiaryEquipmentVars.day] = day
}

private enum class DiaryDestination(
    val tile: CoordGrid,
    val usage: VarBitType?,
    vararg val limits: Int,
) {
    Monastery(CoordGrid(2606, 3222), null, -1, -1, -1, -1),
    ArdougneFarm(CoordGrid(2673, 3375), null, 0, 3, 5, -1),
    SlimePit(CoordGrid(3683, 9888), DiaryEquipmentBits.morytania, 2, 5, 5, -1),
    Burgh(CoordGrid(3482, 3230), null, 0, 0, -1, -1),
    Nardah(CoordGrid(3425, 2928), DiaryEquipmentBits.nardah, 0, 1, 1, -1),
    Kalphites(CoordGrid(3322, 3122), null, 0, 0, 0, -1),
    Sherlock(CoordGrid(2729, 3411), DiaryEquipmentBits.sherlock, 0, 0, 1, -1),
    Piscatoris(CoordGrid(2329, 3685), DiaryEquipmentBits.piscatoris, 0, 0, 1, -1),
    GemMine(CoordGrid(2840, 9387), null, 0, 0, -1, -1),
    Duradel(CoordGrid(2869, 2982, 1), null, 0, 0, 0, -1),
    Woodland(CoordGrid(1553, 3454), DiaryEquipmentBits.woodland, 3, 5, -1, -1),
    Karuulm(CoordGrid(1311, 3800), DiaryEquipmentBits.karuulm, 0, 0, 3, -1),
    Fountain(CoordGrid(3377, 3891), DiaryEquipmentBits.wild, 0, 0, 1, -1),
    Cabbage(CoordGrid(3052, 3292), DiaryEquipmentBits.cabbage, 0, 3, -1, -1),
}

public class DiaryEquipment
@Inject
constructor(private val held: HeldInteractions, private val ringAlchemy: ExplorersAlchemy) :
    PluginScript() {
    override fun ScriptContext.startup() {
        fun route(region: DiaryRegion, first: DiaryDestination, second: DiaryDestination? = null) {
            for ((index, item) in DiaryRewardItems.equipment.getValue(region).withIndex()) {
                if (first.limits[index] != 0) {
                    onOpHeld3(item) { travel(region, index, first) }
                    onOpWorn2(item) { travel(region, index, first) }
                }
                if (second != null && second.limits[index] != 0) {
                    onOpHeld4(item) { travel(region, index, second) }
                    onOpWorn3(item) { travel(region, index, second) }
                }
            }
        }
        route(DiaryRegion.Ardougne, DiaryDestination.Monastery, DiaryDestination.ArdougneFarm)
        route(DiaryRegion.Morytania, DiaryDestination.SlimePit, DiaryDestination.Burgh)
        route(DiaryRegion.Desert, DiaryDestination.Nardah, DiaryDestination.Kalphites)
        route(DiaryRegion.Kandarin, DiaryDestination.Sherlock)
        route(DiaryRegion.Western, DiaryDestination.Piscatoris)
        route(DiaryRegion.Karamja, DiaryDestination.GemMine, DiaryDestination.Duradel)
        route(DiaryRegion.Kourend, DiaryDestination.Woodland, DiaryDestination.Karuulm)
        route(DiaryRegion.Wilderness, DiaryDestination.Fountain)
        for ((index, shield) in
            DiaryRewardItems.equipment.getValue(DiaryRegion.Falador).withIndex()) {
            onOpHeld3(shield) { rechargePrayer(index) }
            onOpWorn2(shield) { rechargePrayer(index) }
            onOpHeld4(shield) { checkPrayer(index) }
            onOpWorn3(shield) { checkPrayer(index) }
        }
        for ((index, ring) in
            DiaryRewardItems.equipment.getValue(DiaryRegion.Lumbridge).withIndex()) {
            onOpHeld1(ring) { held.equip(this, inv, it.slot) }
            onOpHeld2(ring) { checkRing(index) }
            onOpWorn3(ring) { restoreEnergy(index) }
            onOpWorn4(ring) { ringAlchemy.open(this, index) }
            onOpWorn5(ring) { checkRing(index) }
            if (index == 0) {
                onOpHeld3(ring) { restoreEnergy(index) }
                onOpHeld4(ring) { ringAlchemy.open(this, index) }
            } else {
                onOpHeld3(ring) { travel(DiaryRegion.Lumbridge, index, DiaryDestination.Cabbage) }
                onOpWorn2(ring) { travel(DiaryRegion.Lumbridge, index, DiaryDestination.Cabbage) }
                onOpHeld4(ring) {
                    when (
                        menu(
                            "Explorer's ring",
                            true,
                            listOf("Replenish run energy", "Alchemy", "Check charges", "Cancel"),
                        )
                    ) {
                        0 -> restoreEnergy(index)
                        1 -> ringAlchemy.open(this, index)
                        2 -> checkRing(index)
                    }
                }
            }
        }
    }

    private suspend fun ProtectedAccess.travel(
        region: DiaryRegion,
        index: Int,
        dest: DiaryDestination,
    ) {
        if (!canUseDiaryEquipment(region, index)) return
        val level = if (coords.z >= 9920) (coords.z - 9920) / 8 + 1 else (coords.z - 3520) / 8 + 1
        if (
            player.isTeleportBlocked() ||
                (vars[varbits.inside_wilderness] != 0 && level > 20)
        ) {
            mes("A magical force stops you from teleporting.")
            return
        }
        val limit = dest.limits[index]
        if (limit == 0) return
        refreshDiaryEquipment()
        val used =
            dest.usage?.let { vars[it] }
                ?: if (dest == DiaryDestination.ArdougneFarm) vars[DiaryEquipmentVars.farm] else 0
        if (limit > 0 && used >= limit) {
            mes("You have used all of these teleports today.")
            return
        }
        stopAction()
        anim(seqs.teleport_casting)
        spotanim(spotanims.teleport_casting)
        soundSynth(synths.teleport_all)
        delay(4)
        if (limit > 0) {
            if (dest.usage != null) vars[dest.usage] = used + 1
            else vars[DiaryEquipmentVars.farm] = used + 1
        }
        teleport(dest.tile)
        publish(PlayerTeleportEvent.Start(player, dest.tile))
    }

    private fun ProtectedAccess.rechargePrayer(tier: Int) {
        if (!canUseDiaryEquipment(DiaryRegion.Falador, tier)) return
        refreshDiaryEquipment()
        if (stat(stats.prayer) >= statBase(stats.prayer)) {
            mes("Your Prayer points are already full.")
            return
        }
        if (vars[DiaryEquipmentBits.prayer] >= prayerLimit(tier)) {
            checkPrayer(tier)
            return
        }
        val percent = listOf(25, 50, 100, 100)[tier]
        val missing = statBase(stats.prayer) - stat(stats.prayer)
        statAdd(
            stats.prayer,
            minOf(missing, (statBase(stats.prayer) * percent / 100).coerceAtLeast(1)),
            0,
        )
        vars[DiaryEquipmentBits.prayer]++
        mes("Your shield restores some Prayer points.")
    }

    private fun ProtectedAccess.checkPrayer(tier: Int) {
        refreshDiaryEquipment()
        mes(
            "You have ${(prayerLimit(tier) - vars[DiaryEquipmentBits.prayer]).coerceAtLeast(0)} Prayer recharges left today."
        )
    }

    private fun ProtectedAccess.restoreEnergy(tier: Int) {
        if (!canUseDiaryEquipment(DiaryRegion.Lumbridge, tier)) return
        refreshDiaryEquipment()
        if (player.runEnergy >= constants.run_max_energy) {
            mes("Your run energy is already full.")
            return
        }
        if (vars[DiaryEquipmentBits.energy] >= energyLimit(tier)) {
            checkRing(tier)
            return
        }
        vars[DiaryEquipmentBits.energy]++
        val restored = if (tier == 3) constants.run_max_energy else constants.run_max_energy / 2
        player.runEnergy = (player.runEnergy + restored).coerceAtMost(constants.run_max_energy)
        UpdateRun.energy(player, player.runEnergy)
        mes("Your ring replenishes your run energy.")
    }

    private fun ProtectedAccess.checkRing(tier: Int) {
        refreshDiaryEquipment()
        val energy = (energyLimit(tier) - vars[DiaryEquipmentBits.energy]).coerceAtLeast(0)
        val alchs = (30 - vars[DiaryEquipmentBits.alchs]).coerceAtLeast(0)
        val teleports =
            when (tier) {
                0 -> "0"
                1 -> (3 - vars[DiaryEquipmentBits.cabbage]).coerceAtLeast(0).toString()
                else -> "unlimited"
            }
        mes(
            "Your ring has ${energy} energy restores, ${teleports} teleports and ${alchs} alchemy casts remaining today."
        )
    }

    private fun prayerLimit(tier: Int) = if (tier == 3) 2 else 1

    private fun energyLimit(tier: Int) = listOf(2, 3, 4, 3)[tier]
}

internal fun ProtectedAccess.canUseDiaryEquipment(region: DiaryRegion, index: Int): Boolean =
    player.members && vars[region.tier(DiaryDifficulty.entries[index]).reward] != 0
