package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import java.time.Instant
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class JuniperCharcoal
@Inject
constructor(private val locs: LocTypeList, private val npcs: NpcTypeList) : PluginScript() {
    private fun growthCycle() = (Instant.now().epochSecond / 600).toInt()

    private fun near(p: Player) =
        p.coords.level == 0 && p.coords.x in 1701..1730 && p.coords.z in 3457..3487

    private fun logs(p: Player, i: Int) = (p.vars[BlastVars.logs] ushr (i * 6)) and 63

    private fun setLogs(p: Player, i: Int, count: Int) {
        val shift = i * 6
        VarPlayerIntMapSetter.set(
            p,
            BlastVars.logs,
            (p.vars[BlastVars.logs] and (63 shl shift).inv()) or (count shl shift),
        )
    }

    private fun progress(p: Player, i: Int): Int {
        if (logs(p, i) == 0) return 0
        val start = p.vars[BlastVars.started[i]]
        if (start == 0) return 1
        val cycles = (growthCycle() - start).coerceAtLeast(0)
        return if (cycles >= 99) 102 else 2 + cycles
    }

    private fun refresh(p: Player) {
        for (i in 0..1) VarPlayerIntMapSetter.set(p, BlastBits.burners[i], progress(p, i))
    }

    override fun ScriptContext.startup() {
        onArea(BlastAreas.burners) {
            refresh(player)
            player.softTimer(BlastTimers.furnace, 20)
        }
        onAreaExit(BlastAreas.burners) { player.clearSoftTimer(BlastTimers.furnace) }
        onPlayerLogin {
            if (near(player)) {
                refresh(player)
                player.softTimer(BlastTimers.furnace, 20)
            }
        }
        onPlayerSoftTimer(BlastTimers.furnace) {
            if (near(player)) refresh(player) else player.clearSoftTimer(BlastTimers.furnace)
        }
        for (i in 0..1) {
            val furnace = locs.getValue(14837 + i)
            onOpLocU(furnace, objs.juniper_logs) { fill(i) }
            onOpLocU(furnace, objs.tinderbox) { light(i) }
            onOpLoc1(furnace) {
                if (progress(player, i) == 102) empty(i)
                else if (progress(player, i) == 1) light(i) else check(i)
            }
            onOpLoc2(furnace) { check(i) }
        }
        onOpNpc1(npcs.getValue(7069)) {
            if (!player.members) return@onOpNpc1
            if (choice2("How do I make juniper charcoal?", true, "May I have a tinderbox?", false))
                mes(
                    "Fill a furnace with up to 60 juniper logs. Bring a tinderbox and 600 coins to light it. I will watch it while the logs burn."
                )
            else if (objs.tinderbox in inv) mes("You already have a tinderbox.")
            else if (!invAdd(inv, objs.tinderbox).success) mes("You need a free inventory slot.")
        }
        val ingredients =
            listOf(
                objs.pot_empty,
                KourendMineralObjs.sulphur,
                KourendMineralObjs.saltpetre,
                BlastItems.charcoal,
            )
        for ((i, first) in ingredients.withIndex()) for (second in
            ingredients.drop(i + 1)) onOpHeldU(first, second) {
            if (!player.members) return@onOpHeldU
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    for (item in ingredients) delete(bag, item.id, 1)
                    add(bag, BlastItems.pot.id, 1)
                }
            if (tx.success) delay(3)
            else mes("You need an empty pot, volcanic sulphur, saltpetre and juniper charcoal.")
        }
        onOpHeldU(BlastItems.pot, objs.ball_of_wool) {
            if (!player.members) return@onOpHeldU
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, BlastItems.pot.id, 1, it.firstSlot)
                    delete(bag, objs.ball_of_wool.id, 1, it.secondSlot)
                    add(bag, BlastItems.dynamite.id, 1)
                }
            if (tx.success) delay(3)
        }
        onOpHeld1(BlastItems.pot) { invReplaceSlot(inv, it.slot, 1, objs.pot_empty) }
    }

    private fun ProtectedAccess.fill(i: Int) {
        if (!player.members || vars[BlastVars.started[i]] != 0) return
        val total = logs(player, i)
        val add = minOf(60 - total, invTotal(inv, objs.juniper_logs))
        if (add < 1) {
            mes("This furnace cannot hold any more logs.")
            return
        }
        if (invDel(inv, objs.juniper_logs, add).success) {
            setLogs(player, i, total + add)
            refresh(player)
            check(i)
        }
    }

    private fun ProtectedAccess.check(i: Int) {
        refresh(player)
        when (val progress = progress(player, i)) {
            0 -> mes("The charcoal furnace is empty.")
            1 -> mes("The furnace contains ${logs(player,i)} unlit juniper logs.")
            102 -> mes("Your juniper charcoal is ready to collect.")
            else -> mes("The juniper logs are ${progress-1}% charred.")
        }
    }

    private suspend fun ProtectedAccess.light(i: Int) {
        if (!player.members || progress(player, i) != 1) return
        if (objs.tinderbox !in inv) {
            mes("You need a tinderbox to light the logs.")
            return
        }
        if (
            !choice2(
                "Pay Smoggy 600 coins and light the furnace.",
                true,
                "Leave the logs unlit.",
                false,
            )
        )
            return
        if (progress(player, i) != 1 || objs.tinderbox !in inv) return
        if (!invDel(inv, objs.coins, 600).success) {
            mes("Smoggy needs 600 coins to watch the furnace.")
            return
        }
        vars[BlastVars.started[i]] = growthCycle()
        refresh(player)
    }

    private fun ProtectedAccess.empty(i: Int) {
        if (!player.members || progress(player, i) != 102) return
        val amount = logs(player, i) * 50
        if (!invAdd(inv, BlastItems.charcoal, amount).success) {
            mes("Make room for the juniper charcoal.")
            return
        }
        setLogs(player, i, 0)
        vars[BlastVars.started[i]] = 0
        refresh(player)
        mes("You collect $amount juniper charcoal.")
    }
}
