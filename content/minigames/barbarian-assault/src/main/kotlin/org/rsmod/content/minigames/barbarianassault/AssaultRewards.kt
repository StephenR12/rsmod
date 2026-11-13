package org.rsmod.content.minigames.barbarianassault

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.minigames.BarbarianAssaultBits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal fun Player.rank(role: Role): Int = vars[AssaultBits.level[role.ordinal]].coerceIn(1, 5)

internal fun Player.honour(role: Role): Int =
    vars[AssaultBits.base[role.ordinal]] + 512 * vars[AssaultBits.extra[role.ordinal]]

internal fun Player.setHonour(role: Role, total: Int) {
    val points = total.coerceIn(0, 5000)
    VarPlayerIntMapSetter.set(this, AssaultBits.base[role.ordinal], points % 512)
    VarPlayerIntMapSetter.set(this, AssaultBits.extra[role.ordinal], points / 512)
}

internal fun Player.setQueens(total: Int) {
    val count = total.coerceIn(0, 40)
    VarPlayerIntMapSetter.set(this, AssaultVars.queens, count)
    VarPlayerIntMapSetter.set(this, AssaultBits.queenExtra, count / 2 % 8)
    VarPlayerIntMapSetter.set(this, AssaultBits.queenHigh, count / 16)
}

public class AssaultRewards @Inject constructor(private val types: ObjTypeList) : PluginScript() {
    private val stock =
        listOf(
            10548 to 275,
            10550 to 275,
            10547 to 275,
            10549 to 275,
            10551 to 375,
            10555 to 375,
            10552 to 100,
            10553 to 150,
            10564 to 95000,
        )
    private val lessons =
        listOf(
            "Five players fight the Penance together in ten waves.",
            "Go down the ladder and choose a role in the recruitment rooms.",
            "All four roles must be represented. If anyone falls, the team must retry the wave.",
            "Attackers fight the rangers and fighters using the style called by the collector.",
            "Defenders use the called bait to lead runners into traps.",
            "Collectors gather the called eggs and load the hopper for the egg launcher.",
            "Healers use called poisoned food on Penance healers and the healing vial on teammates.",
            "Use your horn to tell your partner what to do. The calls change every thirty seconds.",
            "Pass yellow eggs from collector to healer, attacker, defender, then collector. Fire eight omega eggs.",
            "Commander Connad exchanges honour points for role levels and equipment.",
        )

    override fun ScriptContext.startup() {
        onOpNpc1(AssaultNpcs.captain) { tutorial() }
        onOpNpc3(AssaultNpcs.captain) { tutorial() }
        for ((i, chapter) in AssaultComponents.chapters.withIndex()) onIfModalButton(chapter) {
            if (vars[BarbarianAssaultBits.tutorial] < i + 1) return@onIfModalButton
            mes(lessons[i])
            if (vars[BarbarianAssaultBits.tutorial] == i + 1)
                vars[BarbarianAssaultBits.tutorial] = i + 2
        }
        onOpNpc1(AssaultNpcs.commander) { shop() }
        onOpNpc3(AssaultNpcs.commander) { shop() }
        onOpLoc1(AssaultLocs.board) {
            mes(Role.entries.joinToString(", ") { "${it.name}: level ${player.rank(it)}" })
            if (Role.entries.all { player.rank(it) == 5 })
                player.completeDiaryTask(DiaryTask.ReadAssaultBlackboard)
        }
        onEvent<HeldEquipEvents.WearposChange> {
            if (
                objType.id == 10564 &&
                    player.vars[AssaultVars.granite] != 0 &&
                    player.worn[wearpos.slot]?.id == 10564
            )
                player.completeDiaryTask(DiaryTask.EquipAssaultGraniteBody)
        }
    }

    private suspend fun ProtectedAccess.tutorial() {
        if (!player.members) return
        if (choice2("Show me the tutorial.", true, "I want to skip the tutorial.", false)) {
            if (vars[BarbarianAssaultBits.tutorial] == 0) vars[BarbarianAssaultBits.tutorial] = 1
            ifOpenMainModal(AssaultInterfaces.tutorial)
            for (chapter in AssaultComponents.chapters) ifSetEvents(
                chapter,
                -1..-1,
                org.rsmod.game.type.interf.IfEvent.Op1,
            )
        } else {
            vars[BarbarianAssaultBits.tutorial] = 11
            mes("You may now enter the Barbarian Assault recruitment rooms.")
        }
    }

    private suspend fun ProtectedAccess.shop() {
        if (!player.members) return
        val options =
            Role.entries.map {
                "${it.name} level ${player.rank(it)} (${player.honour(it)} honour points)"
            } +
                stock.map { (id, cost) ->
                    "${types.getValue(id).name} ($cost ${if(id==10564)"coins" else "points in each role"})"
                }
        val index = menu("Commander Connad's rewards", true, options)
        if (index in Role.entries.indices) {
            val role = Role.entries[index]
            val next = player.rank(role) + 1
            val cost = next * 100
            if (next > 5 || player.honour(role) < cost) {
                mes("You cannot purchase that role level.")
                return
            }
            player.setHonour(role, player.honour(role) - cost)
            vars[AssaultBits.level[index]] = next
            mes("You are now a level $next ${role.name.lowercase()}.")
            return
        }
        val (id, cost) = stock.getOrNull(index - Role.entries.size) ?: return
        val queenNeeded = id !in listOf(10552, 10553)
        val queens = vars[AssaultVars.queens]
        if (queenNeeded && queens < 1) {
            mes("You must have a Penance Queen kill to purchase that reward.")
            return
        }
        if (id != 10564 && Role.entries.any { player.honour(it) < cost }) {
            mes("You need $cost points in every role.")
            return
        }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                if (id == 10564) delete(bag, objs.coins.id, cost)
                add(bag, id, 1)
            }
        if (result.failure) {
            mes("You need enough coins and inventory space to claim that reward.")
            return
        }
        if (id == 10564) vars[AssaultVars.granite] = 1
        else for (role in Role.entries) player.setHonour(role, player.honour(role) - cost)
        if (queenNeeded) player.setQueens(queens - 1)
    }
}
