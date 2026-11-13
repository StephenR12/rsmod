package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropRolls
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object FlamtaerPeople : NpcReferences() {
    val razmire = find("razmire_keelgan")
    val shadows = find("shadeshadow_level1")
    val shades = find("shade_level1")
}

internal object FlamtaerPeopleSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<FlamtaerPeopleSpawns>("flamtaer.toml")
    }
}

internal object ShadeSeqs : SeqReferences() {
    val attack = find("shade_attack")
    val block = find("shade_block")
    val sink = find("shade_sink")
    val rise = find("shade_rise")
}

internal object LoarCombat : NpcEditor() {
    init {
        for (type in listOf(FlamtaerPeople.shades, FlamtaerPeople.shadows)) edit(type) {
            timer = 2
            hitpoints = 38
            attack = 45
            strength = 30
            defence = 26
            magic = 0
            ranged = 0
            respawnRate = 35
            huntMode = huntmodes.aggressive_melee
            huntRange = 5
            param[params.attackrate] = 4
            param[params.npc_attack_type] = categories.attacktype_crush
            param[params.attack_anim] = ShadeSeqs.attack
            param[params.defend_anim] = ShadeSeqs.block
            param[params.death_anim] = ShadeSeqs.sink
        }
    }
}

internal object FlamtaerShopInv : InvReferences() {
    val general = find("razmiregeneralstore")
    val building = find("razmirebuildingstore")
}

internal object FlamtaerGoods : ObjReferences() {
    val materials = listOf(find("timberbeam"), find("limestonebrick"), find("swamppaste"))
    val bag = find("flamtaer_bag")
    val pack = find("pack_olive_oil")
    val oil = find("oliveoil4")
    val general =
        listOf(
            Triple(objs.pot_empty, 3, 100),
            Triple(objs.jug_empty, 2, 100),
            Triple(objs.pack_jug_empty, 5, 15),
            Triple(objs.shears, 2, 100),
            Triple(objs.bucket_empty, 2, 100),
            Triple(find("pack_bucket"), 10, 10),
            Triple(objs.tinderbox, 2, 100),
            Triple(objs.chisel, 2, 100),
            Triple(objs.hammer, 5, 100),
            Triple(oil, 150, 10),
            Triple(pack, 20, 100),
            Triple(objs.vial_water, 100, 100),
            Triple(find("pack_vial_water"), 50, 100),
            Triple(objs.pot_of_flour, 100, 10),
            Triple(find("flamtaer_hammer"), 3, 10),
        )
    val building =
        listOf(
            Triple(find("limestone"), 1000, 10),
            Triple(materials[1], 1000, 10),
            Triple(materials[0], 1000, 10),
            Triple(materials[2], 1000, 10),
            Triple(objs.plank, 10, 1000),
        )
}

internal object FlamtaerShopStock : InvEditor() {
    init {
        for ((inv, items) in
            listOf(
                FlamtaerShopInv.general to FlamtaerGoods.general,
                FlamtaerShopInv.building to FlamtaerGoods.building,
            )) edit(inv) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            restock = true
            for ((item, amount, ticks) in items) stock += stock(item, amount, ticks)
        }
    }
}

internal object FlamtaerBag : VarpReferences() {
    val values =
        listOf(find("flamtaer_bag_beams"), find("flamtaer_bag_bricks"), find("flamtaer_bag_paste"))
    val max = intArrayOf(60, 60, 500)
}

internal object FlamtaerBagSaves : VarpBuilder() {
    init {
        for (name in listOf("beams", "bricks", "paste")) build("flamtaer_bag_$name") {
            permanent = true
        }
    }
}

public class FlamtaerSupplies
@Inject
constructor(
    private val shops: Shops,
    private val people: NpcTypeList,
    private val objects: ObjTypeList,
    private val drops: NpcDropTableRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (form in listOf(FlamtaerPeople.shadows, FlamtaerPeople.shades)) onAiTimer(form) {
            if (npc.id != FlamtaerPeople.shadows.id || npc.hitpoints <= 0) return@onAiTimer
            val attacking = npc.mode == NpcMode.OpPlayer2 || npc.mode == NpcMode.ApPlayer2
            if (attacking && npc.transmog == null) {
                npc.transmog(people[FlamtaerPeople.shades], Int.MAX_VALUE)
                npc.anim(ShadeSeqs.rise)
            } else if (!attacking && npc.transmog != null) {
                npc.resetTransmog()
            }
        }
        onOpNpc3(FlamtaerPeople.razmire) { open(true) }
        onOpNpc4(FlamtaerPeople.razmire) { open(false) }
        onOpNpc1(FlamtaerPeople.razmire) {
            if (!eligible()) return@onOpNpc1
            when (
                menu(
                    "Which shop would you like to see?",
                    false,
                    listOf("General store", "Building supplies", "Leave"),
                )
            ) {
                0 -> open(true)
                1 -> open(false)
            }
        }
        onOpHeld1(FlamtaerGoods.pack) {
            val note = objects.cert(objects[FlamtaerGoods.oil])
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, FlamtaerGoods.pack.id, 1, slot = it.slot)
                add(bag, note.id, 100)
            }
        }
        onOpHeld1(FlamtaerGoods.bag) {
            if (player.members) for (i in FlamtaerGoods.materials.indices) fill(i)
        }
        for ((i, item) in FlamtaerGoods.materials.withIndex()) onOpHeldU(item, FlamtaerGoods.bag) {
            if (player.members) fill(i)
        }
        onOpHeld2(FlamtaerGoods.bag) {
            mes(
                "The bag contains ${vars[FlamtaerBag.values[0]]} beams, ${vars[FlamtaerBag.values[1]]} bricks and ${vars[FlamtaerBag.values[2]]} swamp paste."
            )
        }
        onOpHeld3(FlamtaerGoods.bag) {
            for ((i, item) in FlamtaerGoods.materials.withIndex()) {
                val saved = FlamtaerBag.values[i]
                val n = vars[saved]
                if (n > 0) vars[saved] -= invAdd(inv, item, n, strict = false).completed()
            }
        }
        onEvent<PlayerDeathEvent.Unsafe> {
            if (FlamtaerGoods.bag in player.inv) {
                player.invDel(
                    player.inv,
                    objects[FlamtaerGoods.bag],
                    player.inv.count(objects[FlamtaerGoods.bag]),
                )
                FlamtaerBag.values.forEach { set(player, it, 0) }
            }
        }
        for (shade in listOf(FlamtaerPeople.shades, FlamtaerPeople.shadows)) drops.register(
            shade,
            NpcDropTable(NpcDropRolls.item(objs.loar_remains)),
        )
    }

    private fun ProtectedAccess.fill(i: Int) {
        val item = FlamtaerGoods.materials[i]
        val saved = FlamtaerBag.values[i]
        val moved = minOf(invTotal(inv, item), FlamtaerBag.max[i] - vars[saved])
        if (moved > 0 && invDel(inv, item, moved).success) vars[saved] += moved
    }

    private fun ProtectedAccess.eligible(): Boolean {
        if (player.members && vars[varps.morttonquest] >= PyreLogPreparation.QUEST_COMPLETE_STATE)
            return true
        mes("Razmire is too afflicted to trade.")
        return false
    }

    private fun ProtectedAccess.open(general: Boolean) {
        if (!eligible()) return
        if (general)
            shops.open(player, "Razmire General Store.", FlamtaerShopInv.general, 40.0, 130.0, 3.0)
        else
            shops.open(
                player,
                "Razmire Builders Merchants.",
                FlamtaerShopInv.building,
                95.0,
                105.0,
                4.0,
            )
    }
}
