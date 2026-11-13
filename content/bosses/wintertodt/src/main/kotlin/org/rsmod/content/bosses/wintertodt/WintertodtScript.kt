package org.rsmod.content.bosses.wintertodt

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WintertodtScript
@Inject
constructor(
    private val round: WintertodtRound,
    private val actions: WintertodtActions,
    private val rewards: WintertodtRewards,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onGameStartup { round.startup() }
        onAiConTimer(WinterControllers.round) {
            round.tick()
            controller.aiTimer(1)
        }
        onArea(WinterAreas.prison) { round.join(player) }
        onAreaExit(WinterAreas.prison) { round.leave(player) }
        onPlayerLogin { if (WintertodtRound.inside(player.coords)) round.join(player) }
        onPlayerLogout { round.leave(player) }
        onEvent<PlayerDeathEvent.Completed> { round.leave(player) }
        onPlayerSoftTimer(WinterTimers.warmth) { round.warmthTick(player) }
        onEvent<PlayerConsumableEvent.Consumed> { if (foodHealing >= 4) round.restore(player, 350) }
        onOpLoc1(WinterLocs.door) {
            if (WintertodtRound.inside(player.coords)) {
                round.leave(player)
                telejump(CoordGrid(1629, 3963))
            } else if (!player.members || player.statBase(stats.firemaking) < 50)
                mes("You need a Firemaking level of 50 to enter the Wintertodt's prison.")
            else {
                telejump(CoordGrid(1629, 3971))
                round.join(player)
            }
        }
        onOpLoc2(WinterLocs.door) {
            mes(
                if (round.active)
                    "The Wintertodt is active, with ${round.energy}% energy remaining."
                else "The Wintertodt is subdued."
            )
        }
        onOpLoc1(WinterLocs.roots) { actions.chop(this, it.loc) }
        onOpLoc1(WinterLocs.herb) { actions.pickHerb(this, it.loc) }
        for (loc in listOf(WinterLocs.unlit, WinterLocs.broken, WinterLocs.lit)) {
            onOpLoc1(loc) { actions.brazier(this, it.loc) }
            for (item in
                listOf(
                    objs.tinderbox,
                    objs.hammer,
                    objs.imcando_hammer,
                    objs.imcando_hammer_offhand,
                    WinterObjs.torch,
                    WinterObjs.offhand,
                    WinterObjs.root,
                    WinterObjs.kindling,
                )) onOpLocU(loc, item) { actions.brazier(this, it.loc) }
        }
        onOpHeldU(objs.knife, WinterObjs.root) { actions.fletch(this) }
        onOpHeldU(WinterObjs.fletchingKnife, WinterObjs.root) { actions.fletch(this) }
        onOpHeldU(WinterObjs.herb, WinterObjs.unf) { actions.mix(this) }
        onOpNpc1(WinterNpcs.brewma) {
            if (choice2("Please combine my bruma herbs and concoctions.", true, "Leave.", false))
                actions.mix(this, brewma = true)
        }
        for (obj in listOf(WinterObjs.herb, WinterObjs.unf)) onOpNpcU(WinterNpcs.brewma, obj) {
            actions.mix(this, brewma = true)
        }
        for (potion in WinterObjs.potions) {
            onOpHeld1(potion) { actions.drink(this, it.slot, potion) }
            for (npc in listOf(WinterNpcs.pyro, WinterNpcs.down)) onOpNpcU(npc, potion) {
                actions.help(this, it.npc)
            }
        }
        for (npc in listOf(WinterNpcs.pyro, WinterNpcs.down)) onOpNpc1(npc) {
            actions.help(this, it.npc)
        }
        for ((loc, obj) in
            listOf(
                WinterLocs.hammer to objs.hammer,
                WinterLocs.knife to objs.knife,
                WinterLocs.axe to objs.bronze_axe,
                WinterLocs.tinderbox to objs.tinderbox,
                WinterLocs.concoction to WinterObjs.unf,
            )) onOpLoc1(loc) { actions.supply(this, obj) }
        onOpLoc2(WinterLocs.concoction) { actions.supply(this, WinterObjs.unf, 5) }
        onOpLoc3(WinterLocs.concoction) { actions.supply(this, WinterObjs.unf, 10) }
        for (cart in listOf(WinterLocs.cart) + WinterLocs.carts) {
            onOpLoc1(cart) { rewards.search(this) }
            onOpLoc2(cart) { mes("You have ${player.winterRewards} rewards waiting in the cart.") }
            onOpLoc3(cart) { rewards.search(this, big = true) }
        }
        onOpHeld1(WinterObjs.extra) { rewards.openExtra(this, it.slot) }
        onOpNpc1(WinterNpcs.ignisia) {
            mes(
                "Keep the braziers lit, heal the pyromancers and earn at least 500 points before the Wintertodt is subdued."
            )
        }
        onOpNpc3(WinterNpcs.ignisia) { rewards.exchange(this) }
        onOpLoc1(WinterLocs.bank) {
            ifOpenMainSidePair(main = interfaces.bank_main, side = interfaces.bank_side)
        }
    }
}

internal object WinterPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<WintertodtScript>("people.toml")
    }
}
