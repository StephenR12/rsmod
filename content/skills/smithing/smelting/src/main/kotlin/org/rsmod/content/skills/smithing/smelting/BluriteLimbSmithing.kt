package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList

/** Blurite limbs oldid=14986463, 2025-09-15: one bar, level 13, 17.5 XP, five ticks. */
public class BluriteLimbSmithing @Inject constructor(private val objTypes: ObjTypeList, private val anvilSmithing: AnvilSmithing) {
    public suspend fun select(access: ProtectedAccess, anvil: BoundLocInfo) {
        with(access) {
            if (!canSmith(anvil)) return
            val maximum = player.inv.count(objTypes[objs.blurite_bar])
            val input = skillMulti(title = "What would you like to smith?", choices = listOf(objs.blurite_limbs), maxQuantity = maximum, initialQuantity = maximum, actionMode = 0)
            if (input.component.packed == components.skillmulti_a.packed) smith(this, anvil, input.subcomponent)
        }
    }

    public suspend fun smith(access: ProtectedAccess, anvil: BoundLocInfo, quantity: Int) {
        with(access) {
            if (quantity <= 0 || !canSmith(anvil)) return
            clearFurnaceQueues()
            repeat(minOf(quantity, player.inv.count(objTypes[objs.blurite_bar]))) {
                if (!canSmith(anvil)) { resetAnim(); return }
                faceLoc(anvil)
                anim(AnvilSeqs.smith)
                delay(anvilSmithing.actionTicks(player))
                if (!canSmith(anvil)) { resetAnim(); return }
                val result = player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.blurite_bar.id, 1)
                    add(inventory, objs.blurite_limbs.id, 1)
                }
                if (!result.success) { resetAnim(); return }
                statAdvance(stats.smithing, 17.5)
                if (anvil.id == AnvilLocs.doric.id) player.completeDiaryTask(DiaryTask.SmithDoricBluriteLimbs)
            }
            resetAnim()
        }
    }

    private fun ProtectedAccess.canSmith(anvil: BoundLocInfo): Boolean {
        if (!player.members || !isWithinDistance(anvil, 1)) return false
        if ((AnvilLocs.ordinary + AnvilLocs.doric).none { it.id == anvil.id }) return false
        if (player.stat(stats.smithing) < 13) { mes("You need a Smithing level of 13 to smith blurite limbs."); return false }
        if (vars[varps.squire] < 7) { mes("You need to complete The Knight's Sword to smith blurite limbs."); return false }
        if (anvil.id == AnvilLocs.doric.id && vars[AnvilQuestVars.doric] < 100) { mes("You need to complete Doric's Quest to use this anvil."); return false }
        val hammers = listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand)
        if (hammers.none { it in player.inv } && listOf(objs.imcando_hammer, objs.imcando_hammer_offhand).none { it in player.worn }) { mes("You need a hammer to smith items."); return false }
        return objs.blurite_bar in player.inv
    }
}
