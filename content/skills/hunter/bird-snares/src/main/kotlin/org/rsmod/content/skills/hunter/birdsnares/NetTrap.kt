package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.player.events.HunterCapture
import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.controller.vars.intVarCon
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

/** Fixed young-tree net traps; trap assets are conserved through checking, failure, Reset and logout. */
public class NetTrap @Inject constructor(private val conRepo: ControllerRepository, private val controllers: ControllerList, private val locRepo: LocRepository, private val npcRepo: NpcRepository, private val objRepo: ObjRepository, private val players: PlayerList, private val random: GameRandom, private val limits: PortableHunterTraps) {
    private val groundAssets = mutableMapOf<Int, List<Obj>>()
    private val caughtProducts = mutableMapOf<Int, ObjType>()
    init { limits.register(NetTrapController.trap, NetTrapVars.owner) }

    public suspend fun lay(access: ProtectedAccess, tree: BoundLocInfo, species: NetTrapSpecies) = with(access) {
        if (!player.members) return
        if (player.stat(stats.hunter) < species.level) { mes("You need a Hunter level of ${species.level} to set this trap."); return }
        if (limits.count(player) >= maximumTraps(player)) { mes("You cannot lay any more traps at your current Hunter level."); return }
        if (NetTrapObjs.rope !in player.inv || NetTrapObjs.net !in player.inv) { mes("You need a rope and a small fishing net to set this trap."); return }
        if (controller(tree.coords) != null || !isTree(tree, species)) return
        val origin = player.coords
        anim(BoxTrapSeqs.lay)
        delay(LAY_TICKS)
        if (player.coords != origin || controller(tree.coords) != null || !isTree(tree, species) || limits.count(player) >= maximumTraps(player)) return
        val result = player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete(inv, NetTrapObjs.rope.id, 1)
            delete(inv, NetTrapObjs.net.id, 1)
        }
        if (!result.success) return
        locRepo.change(tree, species.open, Int.MAX_VALUE)
        val controller = Controller(NetTrapController.trap, tree.coords)
        controller.owner = player.slotId
        controller.speciesIndex = species.index
        controller.phase = OPEN
        controller.smoked = 0
        controller.baited = 0
        controller.aiTimer(IDLE_TICKS)
        conRepo.add(controller, Int.MAX_VALUE)
    }

    public fun smoke(access: ProtectedAccess, loc: BoundLocInfo) = with(access) {
        val trap = owned(player, loc) ?: return
        if (!player.members || trap.phase != OPEN || trap.speciesIndex == TECU) return
        if (player.stat(stats.hunter) < 39) { mes("You need a Hunter level of 39 to smoke a trap."); return }
        if (BoxTrapObjs.litTorch !in player.inv && BoxTrapObjs.brumaTorch !in player.inv) return
        trap.smoked = 1
        mes("You smoke the trap, removing your scent.")
    }

    public fun bait(access: ProtectedAccess, loc: BoundLocInfo, bait: ObjType) = with(access) {
        val trap = owned(player, loc) ?: return
        if (!player.members || trap.phase != OPEN || trap.speciesIndex == TECU || trap.baited != 0 || NetTrapObjs.bait[trap.speciesIndex] != bait) return
        val result = player.invTransaction(player.inv) { delete(select(player.inv), bait.id, 1) }
        if (result.success) { trap.baited = 1; mes("You add bait to the trap.") }
    }

    public fun investigate(access: ProtectedAccess, loc: BoundLocInfo) = with(access) {
        val trap = owned(player, loc) ?: return
        mes(if (trap.smoked == 1) "This trap has been smoked." else "This trap has not been smoked.")
        mes(if (trap.baited == 1) "This trap has been baited." else "This trap has not been baited.")
    }

    public fun dismantle(access: ProtectedAccess, loc: BoundLocInfo) {
        val trap = owned(access.player, loc) ?: return
        access.anim(BoxTrapSeqs.check)
        remove(trap, access.player, inventory = true)
    }

    public suspend fun check(access: ProtectedAccess, loc: BoundLocInfo, reset: Boolean = false) = with(access) {
        val trap = owned(player, loc) ?: return
        if (!player.members || trap.phase != FULL) return
        val species = NetTrapSpeciesList.all[trap.speciesIndex]
        anim(BoxTrapSeqs.check)
        delay(CHECK_TICKS + if (reset) LAY_TICKS else 0)
        if (controller(loc.coords) !== trap || trap.phase != FULL || !isWithinDistance(loc, 1)) return
        val product = caughtProducts[loc.coords.packed] ?: species.product
        val result = player.invTransaction(player.inv) {
            val inv = select(player.inv)
            add(inv, product.id, 1)
            if (!reset) { add(inv, NetTrapObjs.rope.id, 1); add(inv, NetTrapObjs.net.id, 1) }
        }
        if (!result.success) { mes("You don't have enough inventory space to collect the trap."); return }
        statAdvance(stats.hunter, species.xp)
        publish(HunterCapture(player, species.npc.internalNameValue))
        val task = when (species.index) { 0 -> DiaryTask.CatchSwampLizard; 1 -> DiaryTask.CatchOrangeSalamander; 2 -> DiaryTask.CatchRedSalamander; 3 -> DiaryTask.CatchBlackSalamander; else -> null }
        task?.let(player::completeDiaryTask)
        caughtProducts.remove(loc.coords.packed)
        if (reset) reopen(trap) else remove(trap, player, inventory = false, recover = false)
    }

    public suspend fun reset(access: ProtectedAccess, loc: BoundLocInfo) = with(access) {
        val trap = owned(player, loc) ?: return
        if (trap.phase == FULL) { check(this, loc, reset = true); return }
        if (!player.members || trap.phase != BROKEN) return
        anim(BoxTrapSeqs.lay)
        delay(LAY_TICKS)
        if (controller(loc.coords) !== trap || trap.phase != BROKEN || !isWithinDistance(loc, 1)) return
        // Failed tools are ordinary ground objects. Reset collects those still present; already
        // picked-up tools are consumed from inventory, so another player cannot create duplicates.
        val ground = groundAssets[loc.coords.packed].orEmpty().filter { candidate -> objRepo.findAll(loc.coords).any { it === candidate } }
        val fromInv = listOf(NetTrapObjs.rope, NetTrapObjs.net).filter { item -> ground.none { it.type == item.id } }
        if (fromInv.any { it !in player.inv }) { mes("You need a rope and a small fishing net to reset this trap."); return }
        if (fromInv.isNotEmpty()) {
            val result = player.invTransaction(player.inv) { val inv = select(player.inv); for (item in fromInv) delete(inv, item.id, 1) }
            if (!result.success) return
        }
        for (obj in ground) check(objRepo.del(obj, Int.MAX_VALUE))
        groundAssets.remove(loc.coords.packed)
        reopen(trap)
    }

    public suspend fun approach(access: StandardNpcAccess, loc: BoundLocInfo) {
        val trap = controller(loc.coords)
        if (trap == null || trap.phase != OPEN) { access.npc.resetMode(); return }
        val species = NetTrapSpeciesList.all[trap.speciesIndex]
        val owner = players[trap.owner]
        if (owner == null) { remove(trap, null, inventory = false); access.npc.resetMode(); return }
        if (access.npc.id != species.npc.id || owner.stat(stats.hunter) < species.level || players.any { it.coords == trap.coords }) { access.npc.resetMode(); return }
        val caught = random.randomDouble() < catchRate(owner, species, trap.smoked == 1, trap.baited == 1)
        trap.phase = if (caught) TRAPPING else FAILING
        replace(trap, if (caught) NetTrapLocs.catching[species.index] else NetTrapLocs.failing[species.index])
        if (caught) caughtProducts[trap.coords.packed] = if (species.index == TECU && random.of(1000) == 0) NetTrapObjs.grownTecu else species.product
        trap.aiTimer(TRANSITION_TICKS)
        access.delay(TRANSITION_TICKS)
        if (caught) npcRepo.despawn(access.npc, access.npc.type.respawnRate) else access.npc.resetMode()
    }

    public fun tick(trap: Controller) {
        val species = NetTrapSpeciesList.all[trap.speciesIndex]
        when (trap.phase) {
            TRAPPING -> { replace(trap, species.full); trap.phase = FULL; trap.aiTimer(IDLE_TICKS) }
            FAILING -> {
                replace(trap, species.broken)
                trap.phase = BROKEN
                trap.smoked = 0
                trap.baited = 0
                groundAssets[trap.coords.packed] = listOf(NetTrapObjs.rope, NetTrapObjs.net).map { objRepo.add(it, trap.coords, IDLE_TICKS, receiver = players[trap.owner], reveal = 0) }
                trap.aiTimer(IDLE_TICKS)
            }
            else -> remove(trap, players[trap.owner], inventory = false, deferred = true)
        }
    }

    public fun clear(player: Player) { controllers.filter { it.id == NetTrapController.trap.id && it.owner == player.slotId }.toList().forEach { remove(it, player, inventory = true) } }
    public fun activeTrapCount(player: Player): Int = limits.count(player)
    public fun maximumTraps(player: Player): Int = minOf(5, player.stat(stats.hunter) / 20 + 1) + if (player.vars[varbits.inside_wilderness] == 1) 1 else 0
    public fun catchRate(player: Player, species: NetTrapSpecies, smoked: Boolean, baited: Boolean): Double {
        val base = SkillingSuccessRate.successRate(species.low, species.high, player.stat(stats.hunter).coerceIn(1,99), 99)
        val outfit = listOf(BoxTrapObjs.guildHood, BoxTrapObjs.guildTop, BoxTrapObjs.guildLegs, BoxTrapObjs.guildBoots).all { item -> player.worn.any { it?.id == item.id } }
        return (base + (if (smoked && species.index != TECU) 0.02 else 0.0) + (if (baited && species.index != TECU) 0.03 else 0.0) + (if (outfit) 0.025 else 0.0)).coerceAtMost(1.0)
    }
    private fun owned(player: Player, loc: BoundLocInfo): Controller? = controller(loc.coords)?.takeIf { it.owner == player.slotId }
    private fun controller(coords: CoordGrid): Controller? = conRepo.findExact(coords, NetTrapController.trap)
    private fun isTree(loc: BoundLocInfo, species: NetTrapSpecies): Boolean = loc.id == species.tree.id && locRepo.findAll(loc.coords).any { it.id == loc.id && it.shape == loc.shape && it.angle == loc.angle }
    private fun current(trap: Controller): LocInfo? = locRepo.findAll(trap.coords).firstOrNull { loc -> loc.id in allLocIds }
    private fun replace(trap: Controller, into: org.rsmod.game.type.loc.LocType) { current(trap)?.let { locRepo.change(it, into, Int.MAX_VALUE) } }
    private fun reopen(trap: Controller) { replace(trap, NetTrapSpeciesList.all[trap.speciesIndex].open); trap.phase = OPEN; trap.smoked = 0; trap.baited = 0; trap.aiTimer(IDLE_TICKS) }
    private fun remove(trap: Controller, owner: Player?, inventory: Boolean, recover: Boolean = true, deferred: Boolean = false) {
        val broken = trap.phase == BROKEN
        replace(trap, NetTrapSpeciesList.all[trap.speciesIndex].tree)
        if (deferred) trap.duration(0) else conRepo.del(trap)
        caughtProducts.remove(trap.coords.packed)
        val ground = groundAssets.remove(trap.coords.packed).orEmpty()
        if (!recover) return
        if (broken) {
            if (inventory && owner != null) for (obj in ground) if (objRepo.del(obj, Int.MAX_VALUE)) owner.invAddOrDrop(objRepo, if (obj.type == NetTrapObjs.rope.id) NetTrapObjs.rope else NetTrapObjs.net, coords = trap.coords)
        } else for (item in listOf(NetTrapObjs.rope, NetTrapObjs.net)) {
            if (inventory && owner != null) owner.invAddOrDrop(objRepo, item, coords = trap.coords)
            else objRepo.add(item, trap.coords, IDLE_TICKS, receiver = owner, reveal = 0)
        }
    }
    private val allLocIds by lazy { (NetTrapLocs.trees + NetTrapLocs.open + NetTrapLocs.catching + NetTrapLocs.full + NetTrapLocs.failing + NetTrapLocs.broken).mapTo(hashSetOf()) { it.id } }
    public companion object {
        public const val LAY_TICKS: Int = 4
        public const val CHECK_TICKS: Int = 4
        public const val TRANSITION_TICKS: Int = 2
        public const val IDLE_TICKS: Int = 100
        private const val TECU: Int = 4
        private const val OPEN = 0
        private const val TRAPPING = 1
        private const val FAILING = 2
        private const val FULL = 3
        private const val BROKEN = 4
        private var Controller.owner: Int by intVarCon(NetTrapVars.owner)
        private var Controller.phase: Int by intVarCon(NetTrapVars.phase)
        private var Controller.speciesIndex: Int by intVarCon(NetTrapVars.species)
        private var Controller.smoked: Int by intVarCon(NetTrapVars.smoked)
        private var Controller.baited: Int by intVarCon(NetTrapVars.baited)
    }
}
