package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.HunterCapture
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GuildNpcs : NpcReferences() {
    val verity = find("hg_verity")
    val hunters = listOf("gilman", "cervus", "ornus", "aco", "teco", "wolf").map { find("hg_$it") }
}

internal object GuildVars : VarpReferences() {
    val blocks = find("hunter_rumour_assignments")
    val state = find("hunter_rumour_progress")
    val rewards = find("hunter_rumour_reward_unlocks")
    val peril = find("priestperil")
}

internal object GuildBits : VarBitReferences() {
    val peak = find("eaglepeak_quest")
    val voyage = find("fossilquest_progress")
    val light = find("afl")
}

internal object GuildBuilders : VarpBuilder() {
    init {
        for (name in
            listOf(
                "hunter_rumour_assignments",
                "hunter_rumour_progress",
                "hunter_rumours_completed",
                "hunter_rumour_reward_unlocks",
            )) build(name) { transmitNever = true }
    }
}

internal object GuildSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<GuildRumours>("guild-rumours.toml")
    }
}

internal enum class GuildField(val shift: Int, val width: Int) {
    Introduced(0, 1),
    Repeats(1, 1),
    Active(2, 3),
    Last(5, 5),
    Temporary(10, 5),
    Pity(15, 8),
    GilmanReset(23, 1),
    LastHunter(24, 3);

    fun get(p: Player): Int = (p.vars[GuildVars.state] ushr shift) and ((1 shl width) - 1)

    fun set(p: Player, value: Int) {
        val mask = ((1 shl width) - 1) shl shift
        VarPlayerIntMapSetter.set(
            p,
            GuildVars.state,
            (p.vars[GuildVars.state] and mask.inv()) or ((value shl shift) and mask),
        )
    }
}

public class GuildRumours
@Inject
constructor(
    private val items: ObjTypeList,
    private val ground: ObjRepository,
    private val rng: GameRandom,
) : PluginScript() {
    private val catalogue = RumourCatalogue.rumours

    override fun ScriptContext.startup() {
        onOpNpc1(GuildNpcs.verity) {
            if (!eligible(0)) return@onOpNpc1
            if (GuildField.Introduced.get(player) == 0) {
                GuildField.Introduced.set(player, 1)
                mes(
                    "The guild hunters investigate unusual creatures. Speak to a hunter in the Burrow, hunt the creature they describe, and bring them the rare part."
                )
            } else if (
                choice2("Change whether I receive back-to-back rumours.", true, "Leave.", false)
            ) {
                val repeats = 1 - GuildField.Repeats.get(player)
                GuildField.Repeats.set(player, repeats)
                mes(
                    if (repeats == 1) "You can now receive the same rumour twice in a row."
                    else "You will receive a different rumour after each completion."
                )
            }
        }
        onOpNpc3(GuildNpcs.verity) {
            if (!eligible(0)) return@onOpNpc3
            if (GuildField.Introduced.get(player) == 0) {
                mes("Speak to Verity about the guild hunters first.")
                return@onOpNpc3
            }
            if (
                choice2("Allow back-to-back rumours.", true, "Disable back-to-back rumours.", false)
            )
                GuildField.Repeats.set(player, 1)
            else GuildField.Repeats.set(player, 0)
        }
        for ((index, npc) in GuildNpcs.hunters.withIndex()) {
            onOpNpc1(npc) { discuss(index) }
            onOpNpc3(npc) { discuss(index) }
            onOpNpcU(npc) { if (it.objType.id in 29221..29241) discuss(index) }
        }
        onEvent<HunterCapture> { record(player, species) }
    }

    private fun ProtectedAccess.eligible(hunter: Int): Boolean {
        val level = listOf(46, 57, 57, 72, 72, 91)[hunter]
        if (!player.members || statBase(stats.hunter) < level) {
            mes("You need an unboosted Hunter level of $level for these rumours.")
            return false
        }
        if (hunter == 5 && vars[GuildBits.light] < 12) {
            mes("Complete At First Light to investigate master rumours.")
            return false
        }
        return true
    }

    private fun stored(p: Player, hunter: Int): Int =
        (p.vars[GuildVars.blocks] ushr (5 * hunter)) and 31

    private fun store(p: Player, hunter: Int, task: Int) {
        val mask = 31 shl (5 * hunter)
        VarPlayerIntMapSetter.set(
            p,
            GuildVars.blocks,
            (p.vars[GuildVars.blocks] and mask.inv()) or (task shl (5 * hunter)),
        )
    }

    private fun task(p: Player): Int {
        val active = GuildField.Active.get(p) - 1
        return if (active < 0) 0
        else GuildField.Temporary.get(p).takeIf { it > 0 } ?: stored(p, active)
    }

    private fun roll(p: Player, hunter: Int, except: Int = 0): Int {
        val blocked = (0..5).filter { it != hunter }.map { stored(p, it) }.toSet()
        val choices =
            catalogue.indices
                .map { it + 1 }
                .filter { id ->
                    val r = catalogue[id - 1]
                    r.hunters and (1 shl hunter) != 0 &&
                        r.level <= p.statBase(stats.hunter) &&
                        id !in blocked &&
                        id != except &&
                        (GuildField.Repeats.get(p) != 0 || GuildField.Last.get(p) != id) &&
                        when (r.species) {
                            "salamander_green" -> p.vars[GuildVars.peril] >= 60
                            "varlamore_hunterjerboa01",
                            "hunting_chinchompa",
                            "hunting_chinchompa_big" -> p.vars[GuildBits.peak] >= 40
                            "fossil_herbiboar_visible" ->
                                p.vars[GuildBits.voyage] >= 50 && p.statBase(stats.herblore) >= 31
                            else -> true
                        }
                }
        return rng.pickOrNull(choices) ?: 0
    }

    private suspend fun ProtectedAccess.discuss(hunter: Int) {
        if (!eligible(hunter)) return
        if (GuildField.Introduced.get(player) == 0) {
            mes("Speak to Guild Scribe Verity about hunting rumours first.")
            return
        }
        if (GuildField.GilmanReset.get(player) != 0 && hunter != 0) {
            mes("Finish Huntmaster Gilman's new rumour first.")
            return
        }
        val current = GuildField.Active.get(player) - 1
        if (hunter == current) {
            val id = task(player)
            val r = catalogue.getOrNull(id - 1) ?: return
            val slot =
                inv.indices.firstOrNull {
                    inv[it]?.id == r.part && (r.part != 29223 || inv[it]?.vars == id)
                }
            if (slot == null) {
                mes(
                    "Your current rumour is about a ${r.name.lowercase()}. Bring me ${items.getValue(r.part).name.lowercase()}."
                )
                return
            }
            val tier = listOf(0, 1, 1, 2, 2, 3)[hunter]
            val result =
                player.invTransaction(inv) {
                    val source = select(inv)
                    delete(source, r.part, 1, slot = slot)
                    add(source, 29242 + tier * 2, 1)
                }
            if (!result.success) return
            GuildField.Active.set(player, 0)
            GuildField.Last.set(player, id)
            GuildField.LastHunter.set(player, hunter + 1)
            GuildField.Temporary.set(player, 0)
            GuildField.Pity.set(player, 0)
            GuildField.GilmanReset.set(player, 0)
            store(player, hunter, 0)
            vars[varps.hunter_rumours_completed] = vars[varps.hunter_rumours_completed] + 1
            statAdvance(stats.hunter, (statBase(stats.hunter) + 5.0) * listOf(50, 50, 55, 60)[tier])
            mes(
                "You hand over the rare part and receive a hunters' loot sack. You have completed ${vars[varps.hunter_rumours_completed]} rumours."
            )
            return
        }
        var assigned = stored(player, hunter)
        var temporary = 0
        if (current >= 0 && assigned != 0 && hunter == 0) {
            when (
                choice3(
                    "Investigate Gilman's previous rumour.",
                    1,
                    "Reset all assignments and take a new rumour.",
                    2,
                    "Leave.",
                    0,
                )
            ) {
                0 -> return
                2 -> {
                    vars[GuildVars.blocks] = 0
                    assigned = 0
                    GuildField.GilmanReset.set(player, 1)
                }
            }
        } else if (
            current >= 0 &&
                !choice2(
                    "Investigate this hunter's rumour instead.",
                    true,
                    "Keep my current rumour.",
                    false,
                )
        )
            return
        if (assigned == 0) {
            assigned = roll(player, hunter)
            if (assigned == 0) {
                mes("I don't have a suitable rumour for you at the moment.")
                return
            }
            store(player, hunter, assigned)
        } else if (
            current < 0 &&
                GuildField.LastHunter.get(player) != 0 &&
                GuildField.LastHunter.get(player) != hunter + 1
        ) {
            temporary = roll(player, hunter, assigned)
        }
        GuildField.Temporary.set(player, temporary)
        GuildField.Active.set(player, hunter + 1)
        GuildField.Pity.set(player, 0)
        val r = catalogue[(if (temporary != 0) temporary else assigned) - 1]
        mes(
            "Investigate the ${r.name.lowercase()} and bring me ${items.getValue(r.part).name.lowercase()}."
        )
    }

    private fun record(p: Player, species: String) {
        val id = task(p)
        val r = catalogue.getOrNull(id - 1) ?: return
        if (r.species != species) return
        val full = listOf(29263, 29265, 29267, 29269).all { items.getValue(it) in p.worn }
        val threshold =
            2 * r.rate -
                (if (full)
                    when (r.rate) {
                        75 -> 8
                        50 -> 6
                        25 -> 4
                        else -> 2
                    }
                else 0)
        val count = (GuildField.Pity.get(p) + 1).coerceAtMost(threshold)
        GuildField.Pity.set(p, count)
        if (p.inv.any { it != null && it.id in 29221..29241 }) return
        val guaranteed = count >= threshold
        if (!guaranteed && rng.randomDouble() >= (if (full) 1.05 else 1.0) / r.rate) return
        val type = items.getValue(r.part)
        val part = InvObj(type, 1, if (r.part == 29223) id else 0)
        val added = p.invAdd(p.inv, r.part, 1, vars = part.vars).success
        if (!added) {
            if (guaranteed) {
                p.mes("Make room in your inventory to keep the rare part on your next catch.")
                return
            }
            ground.add(
                Obj.fromOwner(p, p.coords, part),
                p.lootDropDuration ?: constants.lootdrop_duration,
            )
        }
        GuildField.Pity.set(p, 0)
        p.mes("You find a rare piece of the creature! You should take it back to the Hunter Guild.")
    }
}
