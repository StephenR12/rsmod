package org.rsmod.content.minigames.sorceressgarden

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.script.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@jakarta.inject.Singleton
class SorceressGarden
@Inject
constructor(
    private val types: ObjTypeList,
    private val players: PlayerRepository,
    private val collision: CollisionFlagMap,
    private val rays: RayCastValidator,
    private val random: GameRandom,
) : PluginScript() {
    private data class Patrol(var waypoint: Int = 1, var cooldown: Int = 0)

    private val patrols = WeakHashMap<Npc, Patrol>()

    override fun ScriptContext.startup() {
        for ((index, npc) in GardenNpcs.contacts.withIndex()) {
            onOpNpc1(npc) {
                val selim = index >= 2
                if (
                    player.members &&
                        vars[GardenBits.contact] >= 130 &&
                        (vars[GardenBits.sands] >= 108) == selim &&
                        choice2(
                            if (vars[GardenBits.unclaimedKeris] != 0) "Claim the keris."
                            else "Reclaim the keris for 20,000 coins.",
                            true,
                            "Ask about sq'irks.",
                            false,
                        )
                )
                    reclaimKeris()
                else contact(this, selim)
            }
            for (juice in GardenObjs.juices) onOpNpcU(npc, juice) { contact(this, index >= 2) }
        }
        onOpNpc1(GardenNpcs.apprentice) { enter(this, true) }
        onOpNpc4(GardenNpcs.apprentice) { enter(this, false) }
        onOpLoc1(GardenLocs.shelves) { invAdd(inv, objs.beer_glass) }
        onOpLoc1(GardenLocs.fountain) {
            anim(GardenSeqs.drink)
            delay(2)
            telejump(CoordGrid(3321, 3141))
        }
        for (garden in Garden.entries) {
            val i = garden.ordinal
            onOpLoc1(GardenLocs.gates[i]) { gate(this, garden) }
            onOpLoc1(GardenLocs.trees[i]) { harvest(this, garden, false) }
            onOpLoc1(GardenLocs.patches[i]) { harvest(this, garden, true) }
            onOpHeldU(GardenObjs.fruits[i], objs.pestle_and_mortar) { juice(this, garden) }
            onOpHeld1(GardenObjs.juices[i]) { drink(this, garden, it.slot) }
        }
        for ((index, type) in GardenNpcs.guards.withIndex()) onAiTimer(type) { patrol(npc, index) }
    }

    private fun ProtectedAccess.reclaimKeris() {
        val keris = types.getValue(10581)
        val variants = 10581..10584
        if (player.invMap.values.any { bag -> bag.any { it?.id in variants } }) {
            mes("You already have a keris.")
            return
        }
        val price = if (vars[GardenBits.unclaimedKeris] != 0) 0 else 20_000
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                if (price > 0) delete(bag, objs.coins.id, price)
                add(bag, keris.id, 1)
            }
        if (result.success) vars[GardenBits.unclaimedKeris] = 0
        else mes("You need room for the keris and ${price} coins.")
    }

    internal fun contact(access: ProtectedAccess, selim: Boolean): Unit =
        with(access) {
            if (
                !player.members ||
                    vars[GardenVars.prince] < 110 ||
                    (vars[GardenBits.sands] >= 108) != selim
            ) {
                mes("The spymaster has no work for you.")
                return
            }
            if (vars[GardenBits.introduction] == 0) {
                vars[GardenBits.introduction] = 1
                mes(
                    "Speak to the apprentice near Shantay Pass. Bring me the juice of the sq'irks in her garden."
                )
                return
            }
            val bottles = Garden.entries.map { inv.count(types[GardenObjs.juices[it.ordinal]]) }
            if (bottles.all { it == 0 }) {
                mes("Bring me glasses of sq'irkjuice from the garden.")
                return
            }
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    for (garden in Garden.entries) {
                        val n = bottles[garden.ordinal]
                        if (n > 0) delete(bag, GardenObjs.juices[garden.ordinal].id, n)
                    }
                }
            if (tx.success) {
                statAdvance(
                    stats.thieving,
                    Garden.entries.sumOf { it.juiceXp * bottles[it.ordinal] }.toDouble(),
                )
                mes("The spymaster accepts your sq'irkjuice.")
            }
        }

    internal fun enter(access: ProtectedAccess, conversation: Boolean): Unit =
        with(access) {
            if (
                !player.members ||
                    vars[GardenVars.prince] < 110 ||
                    vars[GardenBits.introduction] == 0
            ) {
                mes("Speak to Al Kharid's spymaster first.")
                return
            }
            if (!conversation && vars[GardenBits.apprentice] == 0) {
                mes("Speak to the apprentice about the garden first.")
                return
            }
            if (player.isTeleportBlocked()) {
                mes("A magical force prevents you from teleporting.")
                return
            }
            vars[GardenBits.apprentice] = 1
            telejump(CoordGrid(2912, 5474))
        }

    private fun gate(access: ProtectedAccess, g: Garden): Unit =
        with(access) {
            if (g.contains(player.coords)) {
                telejump(g.gate)
                return
            }
            if (
                !player.members || vars[GardenBits.apprentice] == 0 || player.thievingLvl < g.level
            ) {
                mes("You need access to the garden and level ${g.level} Thieving to enter.")
                return
            }
            telejump(g.inside)
        }

    internal fun harvest(access: ProtectedAccess, g: Garden, herbs: Boolean): Boolean =
        with(access) {
            if (!player.members || vars[GardenBits.apprentice] == 0 || !g.contains(player.coords))
                return false
            val items =
                if (herbs) listOf(rollHerb(g), rollHerb(g))
                else listOf(GardenObjs.fruits[g.ordinal])
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    for (item in items) add(bag, item.id, 1)
                }
            if (!tx.success) {
                mes("You need room in your inventory.")
                return false
            }
            statAdvance(stats.farming, if (herbs) 50.0 else 30.0 + 10 * g.ordinal)
            mes("An elemental force returns you to the central garden.")
            telejump(g.eject)
            true
        }

    private fun rollHerb(g: Garden): ObjType {
        var roll = random.of(g.weights.sum())
        for ((i, w) in g.weights.withIndex()) {
            if (roll < w) return GardenObjs.herbs[i]
            roll -= w
        }
        error("Invalid herb roll")
    }

    internal fun juice(access: ProtectedAccess, g: Garden): Boolean =
        with(access) {
            if (objs.pestle_and_mortar !in inv) return false
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, GardenObjs.fruits[g.ordinal].id, g.fruitCount)
                    delete(bag, objs.beer_glass.id, 1)
                    add(bag, GardenObjs.juices[g.ordinal].id, 1)
                }
            if (!tx.success) {
                mes(
                    "You need ${g.fruitCount} ${g.name.lowercase()} sq'irks and an empty beer glass."
                )
                return false
            }
            statAdvance(stats.cooking, 5.0)
            true
        }

    private fun drink(access: ProtectedAccess, g: Garden, slot: Int): Unit =
        with(access) {
            if (!inv[slot].isType(GardenObjs.juices[g.ordinal])) return
            inv[slot] = InvObj(objs.beer_glass)
            if (g.ordinal > 0) player.statBoost(stats.thieving, g.ordinal, 0)
            player.runEnergy = (player.runEnergy + (g.ordinal + 1) * 500).coerceAtMost(10000)
            UpdateRun.energy(player, player.runEnergy)
        }

    internal fun patrol(npc: Npc, index: Int) {
        val path = elementalRoutes[index]
        val state = patrols.getOrPut(npc) { Patrol() }
        if (state.cooldown > 0) {
            state.cooldown--
            return
        }
        if (npc.coords == path[state.waypoint]) state.waypoint = (state.waypoint + 1) % path.size
        val next = path[state.waypoint]
        val dx = (next.x - npc.coords.x).coerceIn(-1, 1)
        val dz = (next.z - npc.coords.z).coerceIn(-1, 1)
        val garden =
            when (index) {
                in 0..5 -> Garden.Winter
                in 6..13 -> Garden.Spring
                in 14..19 -> Garden.Autumn
                else -> Garden.Summer
            }
        val caught =
            players.findAll(ZoneKey.from(npc.coords), 1).firstOrNull {
                garden.contains(it.coords) && sees(npc, it, dx, dz)
            }
        if (caught != null) {
            npc.anim(GardenSeqs.point)
            PathingEntityCommon.telejump(caught, collision, garden.eject)
            caught.mes("You've been spotted!")
            state.cooldown = 3
        } else npc.walk(next)
    }

    internal fun sees(npc: Npc, p: Player, dx: Int, dz: Int): Boolean {
        val x = p.coords.x - npc.coords.x
        val z = p.coords.z - npc.coords.z
        return npc.coords.level == p.coords.level &&
            x * dz == z * dx &&
            x * dx + z * dz in 0..2 &&
            rays.hasLineOfSight(npc.coords, p.coords)
    }
}
