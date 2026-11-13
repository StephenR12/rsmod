package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.player.events.HunterCapture
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.controller.vars.intVarCon
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.interact.AiLocEvents
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.content.skills.crafting.hunterpouches.HunterPouchStorage
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Five ordinary native deadfall catches. Exact bait/smoke bonuses; unpublished odds/times modelled.
 */
@Singleton
public class DeadfallTrap
@Inject
constructor(
    private val pouches: HunterPouchStorage,
    private val locs: LocRepository,
    private val types: LocTypeList,
    private val npcs: NpcRepository,
    private val cons: ControllerRepository,
    private val players: PlayerList,
    private val limits: PortableHunterTraps,
    private val random: GameRandom,
) : PluginScript() {
    private data class Trap(
        val con: Controller,
        val source: BoundLocInfo,
        val species: DeadfallSpecies,
        var phase: Int = 0,
        var smoked: Boolean = false,
        var baited: Boolean = false,
    )

    private val traps = mutableMapOf<CoordGrid, Trap>()

    init {
        limits.register(DeadfallControllers.trap, DeadfallVarCons.owner)
    }

    override fun ScriptContext.startup() {
        onOpLoc1(DeadfallLocs.boulder) { setup(this, it.loc) }
        onOpLoc1(DeadfallLocs.open) { dismantle(this, it.loc) }
        onOpLoc2(DeadfallLocs.open) { investigate(this, it.loc) }
        for (full in DeadfallLocs.full) onOpLoc1(full) { check(this, it.loc) }
        for (bait in
            listOf(
                objs.raw_beef,
                DeadfallObjs.rainbow,
                DeadfallObjs.barley,
                DeadfallObjs.jerboa,
            )) onOpLocU(DeadfallLocs.open, bait) { bait(this, it.loc, bait) }
        for (torch in listOf(BirdSnareObjs.litTorch, BirdSnareObjs.brumaTorch)) onOpLocU(
            DeadfallLocs.open,
            torch,
        ) {
            smoke(this, it.loc, torch)
        }
        onNpcAccessEvent<AiLocEvents.Op1>(DeadfallLocs.open.id) { approach(this, it.loc) }
        onAiConTimer(DeadfallControllers.trap) { tick(controller) }
        onPlayerLogout { clear(player) }
    }

    private fun owner(t: Trap): Player? = players[t.con.owner]?.takeIf { it.slotId == t.con.owner }

    private fun knife(p: Player): Boolean =
        objs.knife in p.inv ||
            objs.fletching_knife in p.inv ||
            p.righthand?.id == objs.fletching_knife.id

    public suspend fun setup(access: ProtectedAccess, source: BoundLocInfo): Boolean =
        with(access) {
            val species =
                DeadfallSpeciesList.sites[source.coords]?.let { DeadfallSpeciesList.all[it] }
                    ?: return false
            fun can(): Boolean =
                player.members &&
                    player.stat(stats.hunter) >= species.level &&
                    knife(player) &&
                    source.coords !in traps &&
                    traps.values.none { it.con.owner == player.slotId } &&
                    limits.count(player) < minOf(5, player.stat(stats.hunter) / 20 + 1) &&
                    isWithinDistance(source, 1) &&
                    locs.findExact(source.coords, DeadfallLocs.boulder) != null
            if (!can()) {
                mes(
                    "You need ${species.level} Hunter, a knife, suitable logs and no other deadfall to set this trap."
                )
                return false
            }
            val log =
                PitfallSpeciesList.logs.firstOrNull { it in inv }
                    ?: run {
                        mes("You need suitable logs to set a deadfall trap.")
                        return false
                    }
            anim(BirdSnareSeqs.lay)
            gatheringDelay(3)
            if (!can() || !invDel(inv, log, 1).success) return false
            val con = Controller(DeadfallControllers.trap, source.coords)
            con.owner = player.slotId
            con.aiTimer(100)
            cons.add(con, Int.MAX_VALUE)
            val trap = Trap(con, source, species)
            traps[source.coords] = trap
            render(trap, DeadfallLocs.open)
            true
        }

    public fun dismantle(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            val t = traps[loc.coords] ?: return false
            if (t.con.owner != player.slotId || t.phase != 0) return false
            anim(BirdSnareSeqs.check)
            remove(t)
            true
        }

    public fun investigate(access: ProtectedAccess, loc: BoundLocInfo): Unit =
        with(access) {
            val t = traps[loc.coords] ?: return
            if (t.con.owner != player.slotId) {
                mes("This is someone else's trap.")
                return
            }
            mes(
                "The deadfall is " +
                    (if (t.baited) "baited" else "unbaited") +
                    " and " +
                    (if (t.smoked) "smoked." else "not smoked.")
            )
        }

    public fun bait(access: ProtectedAccess, loc: BoundLocInfo, bait: ObjType): Boolean =
        with(access) {
            val t = traps[loc.coords] ?: return false
            if (
                !player.members ||
                    t.con.owner != player.slotId ||
                    t.phase != 0 ||
                    t.baited ||
                    bait.id != t.species.bait.id ||
                    bait !in inv
            )
                return false
            if (!invDel(inv, bait, 1).success) return false
            t.baited = true
            mes("You bait the deadfall trap.")
            true
        }

    public fun smoke(access: ProtectedAccess, loc: BoundLocInfo, torch: ObjType): Boolean =
        with(access) {
            val t = traps[loc.coords] ?: return false
            if (
                !player.members ||
                    player.stat(stats.hunter) < 39 ||
                    t.con.owner != player.slotId ||
                    t.phase != 0 ||
                    t.smoked ||
                    torch !in inv
            )
                return false
            if (torch.id !in listOf(BirdSnareObjs.litTorch.id, BirdSnareObjs.brumaTorch.id))
                return false
            t.smoked = true
            mes("You smoke the deadfall trap.")
            true
        }

    public fun chance(
        p: Player,
        species: DeadfallSpecies,
        baited: Boolean,
        smoked: Boolean,
    ): Double {
        val base =
            0.45 +
                0.45 * (p.stat(stats.hunter).coerceIn(species.level, 99) - species.level) /
                    (99 - species.level)
        return (base + (if (baited) 0.03 else 0.0) + (if (smoked) 0.02 else 0.0)).coerceAtMost(1.0)
    }

    public suspend fun approach(access: StandardNpcAccess, loc: BoundLocInfo): Unit =
        with(access) {
            val t = traps[loc.coords]
            val p = t?.let(::owner)
            if (
                t == null ||
                    p == null ||
                    !p.members ||
                    t.phase != 0 ||
                    npc.id != t.species.npc.id ||
                    p.stat(stats.hunter) < t.species.level
            ) {
                npc.resetMode()
                return
            }
            val caught = random.randomDouble() < chance(p, t.species, t.baited, t.smoked)
            t.phase = if (caught) 1 else 3
            render(t, if (caught) t.species.transition else DeadfallLocs.failed)
            t.con.aiTimer(2)
            delay(2)
            if (caught) npcs.despawn(npc, npc.type.respawnRate) else npc.resetMode()
        }

    public fun tick(con: Controller) {
        val t = traps[con.coords]?.takeIf { it.con === con } ?: return
        when (t.phase) {
            1 -> {
                t.phase = 2
                render(t, t.species.full)
                con.aiTimer(100)
            }
            else -> remove(t, true)
        }
    }

    public fun check(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            val t = traps[loc.coords] ?: return false
            if (!player.members || t.con.owner != player.slotId || t.phase != 2) return false
            val remains = RemainsDrop(player, org.rsmod.game.inv.InvObj(objs.bones))
            publish(remains)
            val delivered =
                pouches.deliverCaught(
                    player,
                    buildList {
                        if (remains.remaining > 0)
                            add(org.rsmod.game.inv.InvObj(objs.bones, remains.remaining))
                        for (item in t.species.loot) add(org.rsmod.game.inv.InvObj(item))
                    },
                )
            if (!delivered) {
                mes("You don't have enough inventory space to collect the deadfall.")
                return false
            }
            remains.commit()
            statAdvance(stats.hunter, t.species.xp)
            publish(HunterCapture(player, t.species.npc.internalNameValue))
            anim(BirdSnareSeqs.check)
            remove(t)
            true
        }

    public fun clear(p: Player) {
        traps.values.filter { it.con.owner == p.slotId }.toList().forEach { remove(it) }
    }

    private fun render(t: Trap, type: LocType) {
        locs.change(t.source, type, Int.MAX_VALUE)
    }

    private fun remove(t: Trap, deferred: Boolean = false) {
        traps.remove(t.source.coords)
        locs.change(t.source, DeadfallLocs.boulder, Int.MAX_VALUE)
        if (deferred) t.con.duration(0) else cons.del(t.con)
    }

    private var Controller.owner: Int by intVarCon(DeadfallVarCons.owner)
}
