package org.rsmod.content.skills.mining.camdozaal

import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object VaultItems : ObjReferences() {
    val temporary =
        listOf("low", "mid", "high").map { find("camdozaal_vault_chest_${it}_temporary") }
}

internal object VaultEdits : ObjEditor() {
    init {
        for (chest in VaultItems.temporary) edit(chest) { param[params.no_bank] = 1 }
    }
}

private data class Reward(
    val id: Int,
    val weight: Int,
    val low: Int,
    val high: Int,
    val noted: Boolean,
)

/** Native per-player morphs select one traversable barrier per group. */
public class CamdozaalVault
@Inject
constructor(
    private val types: LocTypeList,
    private val items: ObjTypeList,
    private val clock: MapClock,
    private val flags: CollisionFlagMap,
    private val bus: EventBus,
) : PluginScript() {
    private val deadline = mutableMapOf<Player, Int>()
    private val door = CoordGrid(2969, 5824)
    private val outside = door.translate(1, -1)
    private val inside = door.translate(1, 1)
    private val widths = listOf(2, 2, 3, 3, 2, 3, 2)
    private val tables =
        CamdozaalVault::class
            .java
            .getResourceAsStream("lockboxes.tsv")!!
            .bufferedReader()
            .useLines { lines ->
                lines
                    .drop(1)
                    .map { it.split('\t') }
                    .groupBy(
                        { it[0].toInt() to (it[1] == "gear") },
                        {
                            Reward(
                                it[2].toInt(),
                                it[3].toInt(),
                                it[4].toInt(),
                                it[5].toInt(),
                                it[6] == "1",
                            )
                        },
                    )
            }

    private fun inVault(p: Player) = p.level == 0 && p.x in 2949..2990 && p.z in 5825..5860

    override fun ScriptContext.startup() {
        onOpLoc1(types.getValue(41537)) {
            if (player in deadline) {
                finish(player, true)
                return@onOpLoc1
            }
            if (!Camdozaal.allowed(player) || vars[CamVars.stored] < 750) {
                mes("You need 750 barronite shards in the Sacred Forge to enter.")
                return@onOpLoc1
            }
            if (
                !choice2(
                    "Spend 750 stored shards to enter the vault.",
                    true,
                    "Stay outside.",
                    false,
                ) || vars[CamVars.stored] < 750
            )
                return@onOpLoc1
            vars[CamVars.stored] -= 750
            for ((i, options) in widths.withIndex()) vars[CamBits.barriers[i]] = random.of(options)
            for (bit in CamBits.boxes) vars[bit] = 0
            for ((start, size, count) in
                listOf(
                    Triple(0, 6, random.of(1..5)),
                    Triple(6, 4, random.of(2..3)),
                    Triple(10, 3, 3),
                )) {
                val choices = (start until start + size).toMutableList()
                repeat(count) { vars[CamBits.boxes[choices.removeAt(random.of(choices.size))]] = 1 }
            }
            deadline[player] = clock + 100
            player.activityBlocksTeleport = true
            telejump(inside)
            vars[CamBits.remaining] = 100
            player.softTimer(CamTime.vault, 1)
            player.ifOpenOverlay(CamUi.vault, bus)
        }
        var id = 41362
        for ((group, count) in widths.withIndex()) for (option in 0 until count) {
            onOpLoc1(types.getValue(id++)) {
                if (player in deadline && vars[CamBits.barriers[group]] == option) cross(it.loc)
            }
        }
        for ((index, bit) in CamBits.boxes.withIndex()) onOpLoc1(types.getValue(41379 + index)) {
            if (player !in deadline || vars[bit] != 1) return@onOpLoc1
            val item = VaultItems.temporary[if (index < 6) 0 else if (index < 10) 1 else 2]
            if (invAdd(inv, item).success) vars[bit] = 0
        }
        onPlayerSoftTimer(CamTime.vault) {
            val end = deadline[player]
            if (end == null || !inVault(player) || clock.cycle >= end) finish(player, false)
            else VarPlayerIntMapSetter.set(player, CamBits.remaining, end - clock.cycle)
        }
        onPlayerLogout { if (player in deadline) finish(player, false) }
        onPlayerLogin {
            if (inVault(player) || player.inv.any { it?.id in listOf(25646, 25648, 25650) })
                finish(player, false)
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> { if (player in deadline) finish(player, false) }
        for (tier in 0..2) onOpHeld1(items.getValue(25647 + tier * 2)) {
            if (player !in deadline && !inVault(player)) open(tier, it.slot)
        }
    }

    private suspend fun ProtectedAccess.cross(barrier: BoundLocInfo) {
        val x = barrier.x
        val z = barrier.z
        if (
            coords.x in x until x + barrier.adjustedWidth &&
                coords.z in z until z + barrier.adjustedLength
        )
            return
        if (barrier.adjustedWidth > barrier.adjustedLength)
            telejump(
                CoordGrid(
                    coords.x.coerceIn(x, x + barrier.adjustedWidth - 1),
                    if (coords.z < z) z + barrier.adjustedLength else z - 1,
                )
            )
        else
            telejump(
                CoordGrid(
                    if (coords.x < x) x + barrier.adjustedWidth else x - 1,
                    coords.z.coerceIn(z, z + barrier.adjustedLength - 1),
                )
            )
        delay(1)
    }

    private fun finish(p: Player, success: Boolean) {
        deadline.remove(p)
        p.clearSoftTimer(CamTime.vault)
        p.activityBlocksTeleport = false
        for (slot in p.inv.indices) {
            val item = p.inv[slot] ?: continue
            if (item.id in listOf(25646, 25648, 25650))
                p.inv[slot] = if (success) InvObj(items.getValue(item.id + 1)) else null
        }
        if (success) p.runEnergy = 10000
        VarPlayerIntMapSetter.set(p, CamBits.remaining, 0)
        if (CamUi.vault in p.ui) p.ifCloseOverlay(CamUi.vault, bus)
        if (inVault(p) || success) PathingEntityCommon.telejump(p, flags, outside)
        if (!success) p.mes("The vault closes. Any unsecured lockboxes are lost.")
    }

    private fun ProtectedAccess.open(tier: Int, slot: Int) {
        var gotGear = false
        val loot = mutableMapOf<Int, Int>()
        repeat(3 - tier) {
            val gear = !gotGear && random.of(50) < (tier + 1) * 2
            gotGear = gotGear || gear
            val table = tables.getValue(tier to gear)
            var roll = random.of(table.sumOf { it.weight })
            val reward =
                table.first {
                    roll -= it.weight
                    roll < 0
                }
            val obj =
                if (reward.noted) items.getValue(items.getValue(reward.id).certlink)
                else items.getValue(reward.id)
            val qty =
                if (reward.low == reward.high) reward.low else random.of(reward.low..reward.high)
            loot[obj.id] = loot.getOrDefault(obj.id, 0) + qty
        }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, 25647 + tier * 2, 1, slot)
                for ((id, amount) in loot) add(bag, id, amount)
            }
        if (!result.success) mes("Make room for the contents of the lockbox.")
    }
}
