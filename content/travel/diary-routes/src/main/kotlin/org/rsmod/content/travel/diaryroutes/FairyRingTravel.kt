package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.isTeleportBlocked
import java.util.WeakHashMap
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.HouseFairyRingDestination
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class FairyRingTravel : PluginScript() {
    private val configuredAt = WeakHashMap<Player, CoordGrid>()

    override fun ScriptContext.startup() {
        (FairyRingLocs.ordinary + FairyRingLocs.house).forEach { ring ->
            onOpLoc1(ring) { travel("BKS", it.loc.coords, remember = false) }
        }
        for (ring in FairyRingLocs.ordinary + FairyRingLocs.zanaris + FairyRingLocs.house) {
            onOpLoc2(ring) { configure(it.loc) }
            onOpLoc3(ring) {
                val code = FairyRingDestinations.codes.getOrNull(vars[FairyRingBits.last])
                if (code != null) travel(code, it.loc.coords)
            }
        }
        for (ring in FairyRingLocs.combined) {
            onOpLoc2(ring) { travel("BKS", it.loc.coords, remember = false) }
            onOpLoc3(ring) { configure(it.loc) }
            onOpLoc4(ring) {
                val code = FairyRingDestinations.codes.getOrNull(vars[FairyRingBits.last])
                if (code != null) travel(code, it.loc.coords)
            }
        }
        FairyRingComponents.clockwise.forEachIndexed { index, button ->
            onIfModalButton(button) { rotate(index, 1) }
        }
        FairyRingComponents.anticlockwise.forEachIndexed { index, button ->
            onIfModalButton(button) { rotate(index, -1) }
        }
        for ((code, button) in FairyRingComponents.log) onIfModalButton(button) {
            if (vars[FairyRingBits.logs.getValue(code)] != 0 && configuredAt.containsKey(player)) {
                val number = FairyRingDestinations.codes.indexOf(code)
                FairyRingBits.dials.forEachIndexed { i, bit ->
                    vars[bit] = (number shr (4 - 2 * i)) and 3
                }
            }
        }
        onIfModalButton(FairyRingComponents.confirm) {
            val source = configuredAt[player] ?: return@onIfModalButton
            val code =
                FairyRingBits.dials
                    .mapIndexed { index, bit ->
                        FairyRingDestinations.dials[index][vars[bit] and 3]
                    }
                    .joinToString("")
            travel(code, source)
        }
        onIfClose(FairyRingInterfaces.main) { configuredAt.remove(player) }
        onPlayerLogout { configuredAt.remove(player) }
    }

    private fun ProtectedAccess.rotate(index: Int, direction: Int) {
        if (configuredAt.containsKey(player)) {
            val dial = FairyRingBits.dials[index]
            vars[dial] = (vars[dial] + direction) and 3
        }
    }

    private fun ProtectedAccess.configure(loc: BoundLocInfo) {
        if (!hasAccess()) return
        ifOpenMainSidePair(FairyRingInterfaces.main, FairyRingInterfaces.log)
        configuredAt[player] = loc.coords
        for (bit in FairyRingBits.dials) vars[bit] = 0
        for (button in
            FairyRingComponents.clockwise +
                FairyRingComponents.anticlockwise +
                FairyRingComponents.confirm) {
            ifSetEvents(button, -1..-1, IfEvent.Op1)
        }
        for ((code, button) in FairyRingComponents.log) {
            if (vars[FairyRingBits.logs.getValue(code)] != 0)
                ifSetEvents(button, -1..-1, IfEvent.Op1)
        }
    }

    private fun ProtectedAccess.hasAccess(): Boolean {
        if (!player.members) return false
        // TODO(quests): the Fairy Godfather grants permission during Fairytale II.
        if (vars[FairyRingBits.permission] == 0 && vars[FairyRingBits.quest] < 40) {
            mes("You need permission from the Fairy Godfather to use the rings.")
            return false
        }
        if (
            FairyRingItems.staffs.none { it.id == player.righthand?.id } &&
                vars[DiaryRegion.Lumbridge.tier(DiaryDifficulty.Elite).reward] == 0
        ) {
            mes("You need to wield a dramen or lunar staff.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.unlocked(code: String): Boolean =
        when (code) {
            "AJQ" -> vars[FairyRingBits.dorg] >= 13
            "AJP",
            "CKQ" -> vars[FairyRingBits.varlamore] != 0
            "AKP" -> vars[FairyRingBits.necropolis] >= 14
            "BJR" -> vars[FairyRingVars.grail] >= 4
            "BJS" -> vars[FairyRingVars.regicide] >= 15
            "BLQ" -> vars[FairyRingBits.goblins] >= 52
            "CIP" -> vars[FairyRingVars.trials] >= 9
            "CIR",
            "CIS",
            "DJR" -> vars[FairyRingBits.zeah] != 0
            "CLR" -> vars[FairyRingVars.monkeys] >= 10
            "DLS" -> vars[FairyRingVars.myreque] >= 110
            else -> true
        }

    private suspend fun ProtectedAccess.travel(
        code: String,
        source: CoordGrid,
        remember: Boolean = true,
    ) {
        if (
            coords.level != source.level ||
                kotlin.math.abs(coords.x - source.x) > 2 ||
                kotlin.math.abs(coords.z - source.z) > 2 ||
                !hasAccess()
        )
            return
        if (player.isTeleportBlocked()) {
            mes("A magical force stops you from teleporting.")
            return
        }
        val destination =
            if (code == "DIQ") {
                val house = HouseFairyRingDestination(player)
                publish(house)
                house.destination
            } else FairyRingDestinations.tiles[code]
        if (destination == null || destination == source || !unlocked(code)) {
            mes("The fairy ring doesn't take you anywhere.")
            return
        }
        ifClose()
        anim(seqs.teleport_casting)
        spotanim(spotanims.teleport_casting)
        delay(3)
        teleport(destination)
        publish(PlayerTeleportEvent.Start(player, destination))
        if (remember) vars[FairyRingBits.last] = FairyRingDestinations.codes.indexOf(code)
        vars[FairyRingBits.logs.getValue(code)] = 1
        val task =
            when (code) {
                "AIR" -> DiaryTask.RingNecromancerTower
                "BIS" -> DiaryTask.RingArdougneZoo
                "CIP" -> DiaryTask.RingMiscellania
                "ALS" -> DiaryTask.RingMcGrubors
                "CIR" -> DiaryTask.RingKaruulm
                "DIS" -> DiaryTask.RingWizardsTower
                else -> null
            }
        task?.let(player::completeDiaryTask)
    }
}
