package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class HouseFixtures
@Inject
constructor(
    private val homes: StarterHouse,
    private val regions: RegionRegistry,
    private val locs: LocRepository,
    private val types: LocTypeList,
    private val objects: ObjTypeList,
    private val queue: WorldQueueList,
    private val random: GameRandom,
) : PluginScript() {
    private val objByName by lazy { objects.values.associateBy { it.internalName } }
    private val locByName by lazy { types.values.associateBy { it.internalName } }
    private val lights = mutableMapOf<CoordGrid, Any>()

    override fun ScriptContext.startup() {
        val ingredients =
            listOf(
                "poh_tea_leaves",
                "bucket_milk",
                "egg",
                "pot_flour",
                "potato",
                "garlic",
                "onion",
                "cheese",
            )
        for ((index, size) in listOf(2, 4, 8).withIndex()) {
            onOpLoc1(locByName.getValue("poh_larder_${index + 1}")) { take(ingredients.take(size)) }
        }
        val tools =
            listOf(
                listOf("poh_saw", "hammer", "chisel", "shears"),
                listOf("bucket_empty", "knife", "spade", "tinderbox"),
                listOf("brown_apron", "glassblowingpipe", "needle"),
                listOf(
                    "amulet_mould",
                    "necklace_mould",
                    "ring_mould",
                    "holy_symbol_mould",
                    "jewl_bracelet_mould",
                    "tiara_mould",
                ),
                listOf("rake", "spade", "gardening_trowel", "dibber", "watering_can_0", "secateurs"),
            )
        for ((index, supplies) in tools.withIndex()) {
            onOpLoc1(locByName.getValue("poh_tools${index + 1}")) { take(supplies) }
        }
        for (tier in 1..7) {
            val unlit = locByName.getValue("poh_torch_$tier")
            val lit = locByName.getValue("poh_torch_${tier}_lit")
            onOpLoc1(unlit) { light(it.loc, tier) }
            onOpLocU(unlit) {
                if (it.objType.id in setOf(objs.tinderbox.id, objs.marrentill.id))
                    light(it.loc, tier)
            }
            if (tier >= 5) {
                onOpLoc1(lit) { light(it.loc, tier) }
                onOpLocU(lit) { if (it.objType.id == objs.marrentill.id) light(it.loc, tier) }
            }
        }
    }

    private suspend fun ProtectedAccess.take(supplies: List<String>) {
        if (homes.publicRoom(player,coords)==null) return
        val items = supplies.map { objByName.getValue(it) }
        val item = items.getOrNull(menu("Take an item", true, items.map { it.name })) ?: return
        if (invAdd(inv, item).failure) mes("You need some free inventory space.")
    }

    private fun ProtectedAccess.light(loc: BoundLocInfo, tier: Int) {
        if (homes.publicRoom(player,coords)==null) return
        if (objs.tinderbox !in inv) {
            mes("You need a tinderbox to light this.")
            return
        }
        if (tier >= 5 && invDel(inv, objs.marrentill).failure) {
            mes("You need a clean marrentill to burn as incense.")
            return
        }
        val region = regions[loc.coords] ?: return
        val lit = locByName.getValue("poh_torch_${tier}_lit")
        val unlit = locByName.getValue("poh_torch_$tier")
        locs.change(loc, lit, Int.MAX_VALUE)
        val generation = Any()
        lights[loc.coords] = generation
        val level = stat(stats.firemaking).coerceAtLeast(1)
        queue.add(200 + level + random.of(level)) {
            if (lights[loc.coords] === generation) {
                lights.remove(loc.coords)
                if (
                    regions[loc.coords] === region &&
                        locs.findExact(loc.coords, loc.shape)?.id == lit.id
                ) {
                    locs.change(loc, unlit, Int.MAX_VALUE)
                }
            }
        }
        mes(
            if (tier >= 5) "You burn the marrentill in the incense burner."
            else "You light the lamp."
        )
    }
}
