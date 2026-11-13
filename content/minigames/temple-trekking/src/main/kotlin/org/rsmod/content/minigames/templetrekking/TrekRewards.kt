package org.rsmod.content.minigames.templetrekking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private class Claim(val slot: Int, val token: InvObj, val route: Int, var selected: Int = -1)

public class TrekRewards
@Inject
constructor(private val types: ObjTypeList, private val random: GameRandom) : PluginScript() {
    private val pending = mutableMapOf<Player, Claim>()
    private val bounds =
        listOf(
            listOf(50..150, 120..380, 370..660),
            listOf(100..200, 150..250, 200..300),
            listOf(5..50, 50..100, 100..150),
            listOf(5..15, 10..20, 15..30),
            listOf(21..72, 66..165, 150..216),
            listOf(10..20, 15..30, 20..40),
            listOf(2..35, 30..65, 60..80),
        )
    private val tomeXp = listOf(1100..1650, 2035..3025, 4015..5005)

    override fun ScriptContext.startup() {
        for (tokens in TrekItems.tokens) for ((route, token) in tokens.withIndex()) onOpHeld1(
            token
        ) {
            if (!player.members) return@onOpHeld1
            val held = inv[it.slot] ?: return@onOpHeld1
            ifOpenMainModal(TrekUi.reward)
            pending[player] = Claim(it.slot, held, route)
            vars[TrekQuest.slot] = it.slot
            for (comp in TrekButtons.options) ifSetEvents(comp, 0..8, IfEvent.Op1)
        }
        for (comp in TrekButtons.options) onIfModalButton(comp) {
            val claim = pending[player] ?: return@onIfModalButton
            if (it.comsub in 0..8) {
                claim.selected = it.comsub
                vars[TrekQuest.selected] = it.comsub
            }
        }
        onIfModalButton(TrekButtons.buy) { pending[player]?.let { redeem(it) } }
        onIfClose(TrekUi.reward) { pending.remove(player) }
        onPlayerLogout { pending.remove(player) }
        val skills =
            listOf(
                stats.fishing,
                stats.agility,
                stats.thieving,
                stats.slayer,
                stats.mining,
                stats.firemaking,
                stats.woodcutting,
            )
        for ((skill, tomes) in TrekItems.tomes.withIndex()) for ((route, tome) in
            tomes.withIndex()) onOpHeld1(tome) {
            if (player.members && invDel(inv, tome, slot = it.slot).success) {
                val xp = random.of(tomeXp[route])
                statAdvance(skills[skill], xp.toDouble())
                mes("You gain $xp experience from the tome.")
            }
        }
    }

    private fun ProtectedAccess.redeem(claim: Claim) {
        if (!player.members || inv[claim.slot] != claim.token || claim.selected !in 0..8) return
        val reward = if (claim.selected == 8) random.of(8) else claim.selected
        val route = claim.route
        val points = claim.token.vars.coerceIn(325, 8000)
        val amount =
            bounds.getOrNull(reward)?.get(route)?.let {
                it.first + (it.last - it.first) * (points - 325) / 7675
            } ?: 1
        val give =
            when (reward) {
                0 -> listOf(7936 to amount)
                1 -> listOf(1777 to amount)
                2 -> listOf(2355 to amount)
                3 -> {
                    val low = 2 * amount / 3
                    listOf((if (random.randomBoolean()) 203 else 205) to low, 3049 to amount - low)
                }
                4 -> listOf(440 to amount / 3, 453 to amount - amount / 3)
                5 -> listOf(5321 to amount)
                6 -> listOf(377 to amount)
                else -> listOf(TrekItems.tomes[random.of(7)][route].id to 1)
            }
        val exchange =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, claim.token.id, 1, slot = claim.slot)
                for ((id, count) in give) {
                    val type = types.getValue(id)
                    add(bag, if (reward < 7 && !type.stackable) type.certlink else id, count)
                }
            }
        if (exchange.failure) {
            mes("Make room for your rewards.")
            return
        }
        pending.remove(player)
        ifClose()
    }
}
