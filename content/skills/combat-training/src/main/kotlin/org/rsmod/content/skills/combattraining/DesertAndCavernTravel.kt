package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.hat
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DungeonJournalBits : VarBitReferences() {
    val desert = find("deserttreasure")
    val contact = find("contact")
    val smokeOff = find("slayer_smoke_toggle")
    val follower = find("follower_space_track")
    val mogres = find("skippy_state")
}

internal object SmokeTimer : TimerReferences() {
    val exposure = find("smoke_dungeon_exposure")
}

internal object SmokeOverlay : InterfaceReferences() {
    val smoke = find("smokeoverlay")
}

internal fun dustProtection(p: Player, items: ObjTypeList): Boolean {
    val hat = items.getOrNull(p.hat) ?: return false
    return hat.id == 4164 ||
        hat.param(params.slayer_helm) != 0 ||
        hat.param(params.slayer_helm_imbued) != 0
}

public class DesertAndCavernTravel
@Inject
constructor(
    private val types: LocTypeList,
    private val items: ObjTypeList,
    private val events: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val well = types.getValue(6279)
        onOpLoc1(well) {
            if (!player.members || vars[DungeonJournalBits.desert] < 11) {
                mes("You haven't discovered the secret of this well yet.")
                return@onOpLoc1
            }
            telejump(CoordGrid(3206, 9379))
            enterSmoke(player)
        }
        onOpLoc2(well) {
            vars[DungeonJournalBits.smokeOff] = 1 - vars[DungeonJournalBits.smokeOff]
            mes(
                if (vars[DungeonJournalBits.smokeOff] == 0) "The dungeon smoke is visible."
                else "The dungeon smoke is hidden."
            )
        }
        onOpLoc1(types.getValue(6439)) {
            leaveSmoke(player)
            telejump(CoordGrid(3311, 2962))
        }
        onPlayerLogin { if (inSmoke(player)) enterSmoke(player) }
        onPlayerSoftTimer(SmokeTimer.exposure) {
            if (!inSmoke(player)) {
                leaveSmoke(player)
                return@onPlayerSoftTimer
            }
            if (!dustProtection(player, items) && player.hat?.id != 1506 && player.hitpoints > 1)
                player.queueHit(0, HitType.Typeless, minOf(20, player.hitpoints - 1))
        }
        onOpLoc1(types.getValue(20275)) {
            if (!player.members || vars[DungeonJournalBits.contact] < 40) return@onOpLoc1
            telejump(
                if (vars[DungeonJournalBits.contact] >= 130) CoordGrid(2799, 5158)
                else CoordGrid(2765, 5129)
            )
        }
        for (id in listOf(20277, 20280)) onOpLoc1(types.getValue(id)) {
            telejump(CoordGrid(3315, 2796))
        }
        for (id in listOf(20278, 20340)) onOpLoc1(types.getValue(id)) {
            if (!player.members || vars[DungeonJournalBits.contact] < 40) return@onOpLoc1
            if (!hasLight()) {
                mes("You need a light source to explore the dungeon.")
                return@onOpLoc1
            }
            telejump(CoordGrid(2166, 4409, 2))
        }
        onOpLoc1(types.getValue(20281)) {
            telejump(
                if (vars[DungeonJournalBits.contact] >= 130) CoordGrid(2800, 5158)
                else CoordGrid(2766, 5129)
            )
        }
        for (id in listOf(20284, 20285)) onOpLoc1(types.getValue(id)) {
            // After Contact! the lower map uses the neighbouring copy of the same tunnels.
            val t = it.loc.coords
            val down = id == 20285
            val dx =
                if (down && t.x < 2300 && vars[DungeonJournalBits.contact] >= 130) 256
                else if (!down && t.x >= 2300) -256 else 0
            val dz = if (dx > 0) -128 else if (dx < 0) 128 else 0
            telejump(CoordGrid(t.x + dx, t.z + dz - 1, if (down) 0 else 2))
        }
        for (id in listOf(25274, 25275)) {
            val whirlpool = types.getValue(id)
            onOpLoc1(whirlpool) { dive() }
            onApLoc1(whirlpool) { if (isWithinApRange(it.loc, 3)) dive() }
        }
        onOpLoc1(types.getValue(25338)) { telejump(it.loc.coords.translate(2, -2, -1)) }
        onOpLoc1(types.getValue(25336)) { telejump(it.loc.coords.translate(-3, 1, 1)) }
        onOpLoc1(types.getValue(25339)) { telejump(it.loc.coords.translate(0, 2, 1)) }
        onOpLoc1(types.getValue(25340)) { telejump(it.loc.coords.translate(0, 2, -1)) }
        onOpLoc1(types.getValue(25216)) { telejump(CoordGrid(2512, 3476)) }
    }

    private fun ProtectedAccess.dive() {
        if (!player.members) return
        if (vars[DungeonJournalBits.follower] != 0) {
            mes("You can't dive in with a follower.")
            return
        }
        if (vars[varbits.brut_craft_ship] != 0) {
            telejump(CoordGrid(1767, 5366, 1))
        } else {
            telejump(CoordGrid(2512, 3476))
            takeInstantHit(HitType.Typeless, minOf(6, (player.hitpoints - 1).coerceAtLeast(0)))
        }
    }

    private fun inSmoke(p: Player) = p.level == 0 && p.x in 3160..3275 && p.z in 9330..9410

    private fun enterSmoke(p: Player) {
        if (p.vars[DungeonJournalBits.smokeOff] == 0 && SmokeOverlay.smoke !in p.ui)
            p.ifOpenOverlay(SmokeOverlay.smoke, events)
        p.softTimer(SmokeTimer.exposure, 20)
    }

    private fun leaveSmoke(p: Player) {
        p.clearSoftTimer(SmokeTimer.exposure)
        if (SmokeOverlay.smoke in p.ui) p.ifCloseOverlay(SmokeOverlay.smoke, events)
    }
}
