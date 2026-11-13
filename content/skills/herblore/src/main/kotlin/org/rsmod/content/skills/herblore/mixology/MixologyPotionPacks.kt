package org.rsmod.content.skills.herblore.mixology

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object MixPackObjs : ObjReferences() {
    val packs =
        listOf(find("mm_potion_pack_low"), find("mm_potion_pack_med"), find("mm_potion_pack_high"))
}

/** The period reward articles mark these observed loot weights as approximate. */
public object MixPackTables {
    val low =
        listOf(
            objs.attack_potion_3 to 11,
            objs.fishing_potion_3 to 11,
            objs.strength_potion_3 to 10,
            objs.restore_potion_3 to 10,
            objs.defence_potion_3 to 7,
            objs.super_attack_3 to 6,
            objs.antipoison_3 to 6,
            objs.energy_potion_3 to 5,
            objs.superantipoison_3 to 5,
            objs.agility_potion_3 to 4,
            objs.combat_potion_3 to 4,
            objs.super_strength_3 to 3,
            objs.hunter_potion_3 to 2,
            objs.prayer_potion_3 to 1,
        )
    val medium =
        listOf(
            objs.restore_potion_3 to 8,
            objs.antipoison_3 to 7,
            objs.combat_potion_3 to 7,
            objs.super_attack_3 to 6,
            objs.superantipoison_3 to 6,
            objs.antifire_potion_3 to 6,
            objs.energy_potion_3 to 4,
            objs.hunter_potion_3 to 4,
            objs.agility_potion_3 to 4,
            objs.super_energy_3 to 2,
            objs.prayer_potion_3 to 2,
            objs.super_defence_3 to 2,
            objs.super_strength_3 to 2,
            objs.prayer_regeneration_potion_3 to 2,
        )
    val high =
        listOf(
            objs.superantipoison_3 to 11,
            objs.magic_potion_3 to 11,
            objs.super_attack_3 to 10,
            objs.zamorak_brew_3 to 9,
            objs.ranging_potion_3 to 6,
            objs.super_defence_3 to 6,
            objs.hunter_potion_3 to 6,
            objs.super_strength_3 to 6,
            objs.super_energy_3 to 4,
            objs.prayer_regeneration_potion_3 to 4,
            objs.ancient_brew_3 to 3,
            objs.prayer_potion_3 to 3,
            objs.saradomin_brew_3 to 2,
            objs.super_restore_3 to 2,
        )
    val all = listOf(low, medium, high)
}

public class MixologyPotionPacks @Inject constructor(private val types: ObjTypeList) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for ((i, pack) in MixPackObjs.packs.withIndex()) onOpHeld1(pack) { open(this, i, it.slot) }
    }

    public fun open(access: ProtectedAccess, index: Int, slot: Int): Boolean =
        with(access) {
            if (
                index !in 0..2 ||
                    !player.members ||
                    player.statBase(stats.herblore) < listOf(60, 70, 85)[index] ||
                    inv[slot]?.id != MixPackObjs.packs[index].id
            )
                return false
            val table = MixPackTables.all[index]
            val loot =
                List(4) {
                    var roll = random.of(table.sumOf { it.second })
                    val item =
                        table
                            .first {
                                roll -= it.second
                                roll < 0
                            }
                            .first
                    val note = types.getValue(types[item].certlink)
                    note to random.of(2..if (index == 1) 3 else 4)
                }
            val tx =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, MixPackObjs.packs[index].id, 1, slot = slot)
                    for ((item, count) in loot) add(i, item.id, count)
                }
            if (!tx.success) mes("Make room for the potion rewards.")
            tx.success
        }
}
