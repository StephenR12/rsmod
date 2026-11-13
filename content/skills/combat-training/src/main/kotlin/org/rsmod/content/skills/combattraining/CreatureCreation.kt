package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.*
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object LifeBits : VarBitReferences() {
    val quest = find("tol_prog")
    val trapdoor = find("tol_trapdoor_open")
    val offerings = (1..12).map { find("tol_symbol_obj$it") }
}

// Each altar keeps its two offerings independently in the player's saved state.
private enum class Altar(
    val x: Int,
    val z: Int,
    val monster: Int,
    val first: Int,
    val second: Int,
    val yield: Int,
    val amount: IntRange,
    val satchel: Int,
    val note: DiaryDifficulty?,
) {
    Newtroost(3058, 4410, 3605, 221, 314, 221, 4..10, 10882, DiaryDifficulty.Medium),
    Unicow(3018, 4410, 3601, 1739, 237, 237, 2..4, 10878, DiaryDifficulty.Medium),
    Spidine(3043, 4361, 3602, 223, 327, 223, 3..6, 10879, DiaryDifficulty.Medium),
    Swordchick(3034, 4361, 3603, 371, 2138, 314, 10..40, 10880, null),
    Jubster(3066, 4380, 3604, 7566, 377, 7566, 3..7, 10881, DiaryDifficulty.Easy),
    Frogeel(3012, 4380, 3600, 5001, 4517, 5001, 5..10, 10877, DiaryDifficulty.Easy);

    val tile = CoordGrid(x, z)
}

public class CreatureCreation
@Inject
constructor(
    private val locs: LocTypeList,
    private val npcs: NpcTypeList,
    private val objects: ObjTypeList,
    private val repository: NpcRepository,
    private val players: org.rsmod.game.entity.PlayerList,
    private val drops: NpcDropTableRegistry,
) : PluginScript() {
    private val byTile = Altar.entries.associateBy { it.tile }
    private val active = mutableMapOf<Player, Npc>()

    override fun ScriptContext.startup() {
        onOpLoc1(locs.getValue(21814)) {
            if (allowed()) telejump(CoordGrid(2649, if (coords.z >= 3225) 3224 else 3226))
        }
        val trap = locs.getValue(21944)
        onOpLoc1(trap) {
            if (!allowed()) return@onOpLoc1
            if (vars[LifeBits.trapdoor] == 0) vars[LifeBits.trapdoor] = 1
            else telejump(CoordGrid(3038, 4376))
        }
        onOpLoc2(trap) { if (allowed()) vars[LifeBits.trapdoor] = 0 }
        onOpLoc1(locs.getValue(17974), CoordGrid(3038, 4375)) { telejump(CoordGrid(2649, 3213)) }
        val symbol = locs.getValue(21893)
        for (altar in Altar.entries) {
            val ingredients = listOf(altar.first, altar.second)
            for ((i, id) in ingredients.withIndex()) onOpLocU(symbol, objects.getValue(id)) {
                if (!allowed() || it.loc.coords != altar.tile) return@onOpLocU
                offer(altar, i, it.invSlot)
            }
            val product = objects.getValue(altar.yield)
            drops.register(
                npcs.getValue(altar.monster),
                NpcDropTable(
                    NpcDropRolls.sequence(
                        NpcDropRolls.item(objs.bones),
                        NpcDropRoll { c ->
                            val note = altar.note
                            val type =
                                if (
                                    note != null &&
                                        c.hero.vars[DiaryRegion.Ardougne.tier(note).complete] != 0
                                )
                                    objects.getValue(product.certlink)
                                else product
                            NpcDropRolls.item(type, altar.amount).roll(c)
                        },
                        NpcDropRolls.independent(
                            3,
                            20,
                            NpcDropRolls.item(objects.getValue(altar.satchel)),
                        ),
                        NpcDropRolls.independent(1, 10, NpcDropRolls.item(objects.getValue(10859))),
                    )
                ),
            )
            onEvent<NpcHitEvents.ResolveDamage>(altar.monster.toLong()) {
                if (hit.isFromPlayer && active[hit.resolvePlayerSource(players)] !== npc) damage = 0
            }
        }
        onOpLoc1(symbol) {
            val altar = byTile[it.loc.coords] ?: return@onOpLoc1
            if (!allowed()) return@onOpLoc1
            if (active[player]?.isSlotAssigned == true) {
                mes("Finish fighting the creature you have already made.")
                return@onOpLoc1
            }
            for ((i, id) in listOf(altar.first, altar.second).withIndex()) {
                if (vars[LifeBits.offerings[2 * altar.ordinal + i]] != 0) continue
                val slot = inv.indexOfFirst { item -> item?.id == id }
                if (slot >= 0) offer(altar, i, slot)
            }
            val bits = LifeBits.offerings.subList(2 * altar.ordinal, 2 * altar.ordinal + 2)
            if (bits.any { vars[it] == 0 }) {
                inspect(altar)
                return@onOpLoc1
            }
            val creature =
                Npc(
                    npcs.getValue(altar.monster),
                    when (altar) {
                        Altar.Swordchick,
                        Altar.Jubster -> altar.tile.translate(-3, 0)
                        Altar.Spidine,
                        Altar.Frogeel -> altar.tile.translate(1, 0)
                        else -> altar.tile.translate(0, -2)
                    },
                )
            repository.add(creature, 500)
            active[player] = creature
            for (bit in bits) vars[bit] = 0
            creature.queueCombatRetaliate(player)
        }
        onOpLoc2(symbol) { byTile[it.loc.coords]?.let { altar -> inspect(altar) } }
        onEvent<NpcStateEvents.Delete> { active.values.remove(npc) }
        onPlayerLogout {
            active.remove(player)?.let { if (it.isSlotAssigned) repository.del(it, Int.MAX_VALUE) }
        }
    }

    private fun ProtectedAccess.allowed() = player.members && vars[LifeBits.quest] >= 18

    private fun ProtectedAccess.offer(altar: Altar, index: Int, slot: Int) {
        val bit = LifeBits.offerings[2 * altar.ordinal + index]
        if (vars[bit] != 0) {
            mes("That ingredient is already on the altar.")
            return
        }
        val item = objects.getValue(if (index == 0) altar.first else altar.second)
        if (invDel(inv, item, slot = slot).success) vars[bit] = 1
    }

    private fun ProtectedAccess.inspect(altar: Altar) {
        val needs =
            listOf(altar.first, altar.second).filterIndexed { i, _ ->
                vars[LifeBits.offerings[2 * altar.ordinal + i]] == 0
            }
        mes(
            if (needs.isEmpty()) "Both ingredients are in place."
            else
                "The altar needs " +
                    needs.joinToString(" and ") { objects.getValue(it).name.lowercase() } +
                    "."
        )
    }
}
