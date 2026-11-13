package org.rsmod.content.skills.woodcutting.specialist

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object TarSwampLocs : LocReferences() {
    val obstacles =
        listOf(
                "fossil_flora_swamp_tarpit_obstacle_01_active",
                "fossil_flora_swamp_tarpit_obstacle_02_active",
                "fossil_flora_swamp_tarpit_obstacle_03_active",
                "fossil_flora_swamp_tarpit_obstacle_03_stumps_active",
                "fossil_flora_swamp_tarpit_obstacle_04_pass_active",
                "fossil_flora_swamp_tarpit_obstacle_04_stumps_active",
                "fossil_flora_swamp_tarpit_obstacle_05_pass_active",
            )
            .map(::find)
    val cleared =
        listOf(
                "fossil_flora_swamp_tarpit_obstacle_01",
                "fossil_flora_swamp_tarpit_obstacle_02",
                "fossil_flora_swamp_tarpit_obstacle_03",
                "fossil_flora_swamp_tarpit_obstacle_03_stumps",
                "fossil_flora_swamp_tarpit_obstacle_04",
                "fossil_flora_swamp_tarpit_obstacle_04_stumps",
                "fossil_flora_swamp_tarpit_obstacle_05",
            )
            .map(::find)
    val bed = find("fossil_mushroom_bed_multi")
    val emptyBed = find("fossil_mushroom_bed_empty")
    val fullBed = find("fossil_mushroom_bed_full")
    val jump = find("fossil_jumpshroom")
}

public object TarSwampBits : VarBitReferences() {
    val bed = find("fossil_mushroom_bed")
    val activity = find("fossil_activity_mushroombed")
}

internal object TarSwampSeqs : SeqReferences() {
    val rake = find("farming_raking")
    val jump = find("human_longjump")
}

public class TarSwampTraversal
@Inject
constructor(private val types: ObjTypeList, private val locs: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((i, loc) in TarSwampLocs.obstacles.withIndex()) onOpLoc1(loc) {
            clear(this, it.loc, i)
        }
        for (loc in listOf(TarSwampLocs.bed, TarSwampLocs.emptyBed)) {
            onOpLoc1(loc) { fill(this, it.loc) }
            onOpLocU(loc, SpecialistObjs.mushroom) { fill(this, it.loc) }
        }
        onOpLoc1(TarSwampLocs.jump) { jump(this, it.loc) }
    }

    private fun ProtectedAccess.allowed(): Boolean =
        player.members && vars[SpecialistBits.quest] >= 50

    public suspend fun clear(access: ProtectedAccess, loc: BoundLocInfo, index: Int): Boolean =
        with(access) {
            if (!allowed() || index !in TarSwampLocs.obstacles.indices || !isWithinDistance(loc, 1))
                return false
            val rake = index == 0 || index == 6
            val axe = Woodcutting.findAxe(player, types)
            if (if (rake) objs.rake !in inv else axe == null) {
                mes(
                    if (rake) "You need a rake to clear these vines."
                    else "You need an axe you can use to cut these vines."
                )
                return false
            }
            if (rake) anim(TarSwampSeqs.rake)
            else anim(types[requireNotNull(axe)].axeWoodcuttingAnim)
            gatheringDelay(4)
            if (
                !allowed() ||
                    !isWithinDistance(loc, 1) ||
                    if (rake) objs.rake !in inv else Woodcutting.findAxe(player, types) != axe
            )
                return false
            locs.change(loc, TarSwampLocs.cleared[index], 100)
            resetAnim()
            mes("You clear a path through the vines.")
            true
        }

    public fun fill(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            if (
                !allowed() ||
                    loc.coords != CoordGrid(3686, 3755) ||
                    !isWithinDistance(loc, 1) ||
                    vars[TarSwampBits.bed] != 0
            )
                return false
            if (
                !player
                    .invTransaction(inv) { delete(select(inv), SpecialistObjs.mushroom.id, 9) }
                    .success
            ) {
                mes("You need nine bittercap mushrooms to cushion the mud pit.")
                return false
            }
            vars[TarSwampBits.bed] = 1
            vars[TarSwampBits.activity] = 1
            mes("You fill the mud pit with a soft bed of mushrooms.")
            true
        }

    public suspend fun jump(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            if (!allowed() || loc.coords != CoordGrid(3663, 3809)) return false
            val cushioned = vars[TarSwampBits.bed] != 0
            // Jump trajectory and four-cycle presentation are a model; target is a decoded walkable
            // bed edge.
            anim(TarSwampSeqs.jump)
            delay(4)
            telejump(CoordGrid(3686, 3756))
            if (cushioned) {
                vars[TarSwampBits.bed] = 0
                mes("The mushroom bed cushions your landing.")
            } else {
                takeInstantHit(HitType.Typeless, random.of(40..50))
                mes("You land painfully in the empty mud pit.")
            }
            resetAnim()
            true
        }
}
