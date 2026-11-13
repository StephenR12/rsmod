package org.rsmod.content.skills.maxcape

import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.minigames.skillcapeTeleport
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CapeTrainers : NpcReferences() {
    val fisher = find("master_fisher")
    val crafter = find("master_crafter")
    val hickton = find("hickton")
    val kaqemeex = find("kaqemeex")
}

internal object CapeBits : VarBitReferences() {
    val grapples = find("free_grapple")
    val raft = find("max_multi_log")
    val emote = find("emote_maxcape")
}

internal object CapeVars : VarpReferences() {
    val grappleDay = find("fletching_cape_grapple_day")
}

internal object CapeVarBuilder : VarpBuilder() {
    init {
        build("fletching_cape_grapple_day") {
            permanent = true
            transmitNever = true
        }
    }
}

public class ProductionCapePerks : PluginScript() {
    override fun ScriptContext.startup() {
        for (cape in listOf(objs.fishing_skillcape, objs.fishing_skillcape_t)) {
            onOpHeld3(cape) { fishingGuild(this) }
            onOpWorn3(cape) { fishingGuild(this) }
            onOpHeld4(cape) { grotto(this) }
            onOpWorn4(cape) { grotto(this) }
        }
        for (cape in listOf(objs.crafting_skillcape, objs.crafting_skillcape_t)) {
            onOpHeld3(cape) { craftingGuild(this) }
            onOpWorn3(cape) { craftingGuild(this) }
        }
        for (cape in listOf(objs.fletching_skillcape, objs.fletching_skillcape_t)) {
            onOpHeld3(cape) { grapple(this) }
            onOpWorn3(cape) { grapple(this) }
        }
        for (cape in listOf(objs.herblore_skillcape, objs.herblore_skillcape_t)) {
            onOpHeld3(cape) { pestle(this) }
            onOpWorn3(cape) { pestle(this) }
        }
        for ((npc, stat, hood) in
            listOf(
                Triple(CapeTrainers.fisher, stats.fishing, objs.fishing_hood),
                Triple(CapeTrainers.crafter, stats.crafting, objs.crafting_hood),
                Triple(CapeTrainers.kaqemeex, stats.herblore, objs.herblore_hood),
            )) onOpNpc1(npc) { SkillcapeSale.offer(this, stat, hood) }
    }

    internal suspend fun craftingGuild(access: ProtectedAccess) =
        with(access) { skillcapeTeleport { CoordGrid(2931, 3286) } }

    internal suspend fun fishingGuild(access: ProtectedAccess) =
        with(access) { skillcapeTeleport { CoordGrid(2604, 3401) } }

    internal suspend fun grotto(access: ProtectedAccess) =
        with(access) { skillcapeTeleport { CoordGrid(2504, 3484) } }

    internal fun pestle(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            if (invAdd(inv, objs.pestle_and_mortar).failure) mes("You need more inventory space.")
            else mes("You find a pestle and mortar inside the cape.")
        }

    internal fun grapple(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            val today = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()
            val used = if (vars[CapeVars.grappleDay] == today) vars[CapeBits.grapples] else 0
            if (used >= 3) {
                mes("You have used all three of today's grapples.")
                return
            }
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    add(bag, objs.mith_grapple.id, 1)
                    add(bag, objs.bronze_crossbow.id, 1)
                }
            if (result.failure) {
                mes("You need room for a grapple and a crossbow.")
                return
            }
            vars[CapeVars.grappleDay] = today
            vars[CapeBits.grapples] = used + 1
            mes("You find a mithril grapple and bronze crossbow inside the cape.")
        }
}
