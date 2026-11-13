package org.rsmod.content.skills.combattraining.chompy

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class OgreBellows @Inject internal constructor(
    private val npcs: NpcRepository,
    private val locs: LocRepository,
    private val hunt: ChompyHunting,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(HuntingScenery.cave) { telejump(CoordGrid(2646,9378)) }
        for(exit in HuntingScenery.exits) onOpLoc1(exit) { telejump(CoordGrid(2630,2997)) }
        onOpLoc1(HuntingScenery.chest) {
            if (!player.canHuntChompies()) return@onOpLoc1
            locs.change(it.loc,HuntingScenery.openChest,100)
            takeBellows()
        }
        onOpLoc1(HuntingScenery.openChest) { if(player.canHuntChompies()) takeBellows() }
        onOpLoc1(HuntingScenery.swamp) {
            if (!player.canHuntChompies()) return@onOpLoc1
            var didFill = false
            for (slot in inv.indices) {
                if (!fill(slot)) continue
                didFill = true
                delay(2)
            }
            if (!didFill) mes("You need ogre bellows with room for swamp gas.")
        }
        for (bellows in HuntingObjects.bellows) onOpLocU(HuntingScenery.swamp,bellows) {
            if (player.canHuntChompies()) fill(it.invSlot)
        }
        onOpNpc1(HuntingNpcs.toad) { inflate(it.npc,inv.indexOfFirst { item -> HuntingObjects.bellows.drop(1).any { it.id==item?.id } }) }
        for (bellows in HuntingObjects.bellows) onOpNpcU(HuntingNpcs.toad,bellows) { inflate(it.npc,it.invSlot) }
        for (bellows in HuntingObjects.bellows.drop(1)) onOpHeldU(bellows,HuntingObjects.bait.first()) {
            balloon(bellows,it.firstSlot,it.secondSlot)
        }
        for ((index,bait) in HuntingObjects.bait.withIndex()) {
            onOpHeld1(bait) { hunt.lay(this,index,it.slot) }
            onOpHeld4(bait) { invDel(inv,bait,invTotal(inv,bait));say("Free the fatsy toady!") }
            onOpHeld5(bait) { invDel(inv,bait,slot=it.slot);say("Free the fatsy toady!") }
        }
    }
    private fun ProtectedAccess.takeBellows() {
        if (invAdd(inv,HuntingObjects.bellows.first()).success) mes("You take a pair of ogre bellows.")
        else mes("Make room for a pair of ogre bellows.")
    }
    private fun ProtectedAccess.fill(slot: Int): Boolean {
        val held = inv[slot] ?: return false
        if (HuntingObjects.bellows.dropLast(1).none {it.id == held.id}) return false
        anim(HuntingAnims.fill)
        inv[slot] = InvObj(HuntingObjects.bellows.last())
        mes("You fill the bellows with swamp gas.")
        return true
    }
    private fun ProtectedAccess.inflate(toad: Npc,slot: Int) {
        if (!player.canHuntChompies() || !toad.isVisible) return
        if (isInCombat()) { mes("You can't inflate a toad during combat.");return }
        val load = if (slot in inv.indices) HuntingObjects.bellows.indexOfFirst { it.id == inv[slot]?.id } else -1
        if (load < 1) { mes("You need ogre bellows filled with swamp gas.");return }
        val escaped = invTotal(inv,HuntingObjects.bait.first()) >= 3
        val result = player.invTransaction(inv) {
            val bag=select(inv)
            delete(bag,HuntingObjects.bellows[load].id,1,slot=slot)
            add(bag,HuntingObjects.bellows[load-1].id,1,slot=slot)
            if(!escaped) add(bag,HuntingObjects.bait.first().id,1)
        }
        if (!result.success) { mes("You need space for the bloated toad.");return }
        anim(HuntingAnims.fill)
        toad.anim(HuntingAnims.inflate)
        npcs.hide(toad,15)
        if (escaped) mes("One of your bloated toads manages to escape.")
    }
    private fun ProtectedAccess.balloon(bellows: ObjType,bellowsSlot: Int,toadSlot: Int) {
        if (!player.canHuntChompies() || isInCombat()) return
        val load=HuntingObjects.bellows.indexOf(bellows)
        val rock=HuntingObjects.rocks.indexOfFirst {it in inv}
        val tied = rock >= 0 && objs.ball_of_wool in inv && vars[HuntingBits.skrach] >= 170
        val useWool = tied && vars[HuntingVars.wool] >= 14
        val result=player.invTransaction(inv) {
            val bag=select(inv)
            delete(bag,bellows.id,1,slot=bellowsSlot)
            add(bag,HuntingObjects.bellows[load-1].id,1,slot=bellowsSlot)
            delete(bag,HuntingObjects.bait.first().id,1,slot=toadSlot)
            if (tied) {
                delete(bag,HuntingObjects.rocks[rock].id,1)
                if (useWool) delete(bag,objs.ball_of_wool.id,1)
                add(bag,HuntingObjects.bait[rock+1].id,1,slot=toadSlot)
            }
        }
        if (!result.success) return
        anim(HuntingAnims.fill)
        if (tied) {
            vars[HuntingVars.wool] = if (useWool) 0 else vars[HuntingVars.wool]+1
            mes("You tether the balloon toad to the rock.")
        } else mes("The inflated toad floats away.")
    }
}
