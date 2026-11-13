package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ChestLocs : LocReferences() {
    val crystal = find("crystal_chestclosed")
    val crystalOpen = find("crystal_chestopen")
    val muddy = find("muddy_chestclosed")
    val muddyOpen = find("muddy_chestopen")
}
internal object ChestItems : ObjReferences() {
    val crystalKey = find("crystal_key")
    val muddyKey = find("muddy_key")
    val loop = find("keyhalf1")
    val tooth = find("keyhalf2")
    val spinach = find("spinach_roll")
    val iron = find("cert_iron_ore")
    val coal = find("cert_coal")
    val swordfish = find("raw_swordfish")
    val shield = find("adamant_sq_shield")
    val legs = find("rune_platelegs")
    val skirt = find("rune_plateskirt")
    val bars = find("cert_mithril_bar")
    val wildExtras = listOf("cert_blighted_mantaray" to 25,"cert_blighted_karambwan" to 25,
        "blighted_sack_icebarrage" to 25,"cert_blighted_anglerfish" to 15,"cert_blighted_4dose2restore" to 3,
        "slayer_wilderness_key" to 1,"slayer_wilderness_key" to 1).map { (key,count) -> find(key) to count }
}
internal object ChestSeqs : SeqReferences() { val open = find("human_openchest") }

public class KeyChests @Inject constructor(private val locs:LocRepository,private val ground:ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(ChestItems.loop,ChestItems.tooth) {
            if (!player.members) return@onOpHeldU
            val result = player.invTransaction(inv) {
                val bag=select(inv)
                delete(bag,ChestItems.loop.id,1,it.firstSlot)
                delete(bag,ChestItems.tooth.id,1,it.secondSlot)
                add(bag,ChestItems.crystalKey.id,1,slot=it.firstSlot)
            }
            if (result.success) mes("You join the halves of the key.")
        }
        for ((chest,key) in listOf(ChestLocs.crystal to ChestItems.crystalKey,ChestLocs.muddy to ChestItems.muddyKey)) {
            onOpLoc1(chest) { open(it.loc,key) }
            onOpLocU(chest,key) { if (inv[it.invSlot]?.id == key.id) open(it.loc,key) }
        }
    }

    private fun ProtectedAccess.open(loc:BoundLocInfo,key:ObjType) {
        val crystal = key == ChestItems.crystalKey
        if (crystal && !player.members) return
        if (!invDel(inv,key).success) { mes("The chest is locked."); return }
        locs.change(loc,if (crystal) ChestLocs.crystalOpen else ChestLocs.muddyOpen,4)
        anim(ChestSeqs.open)
        val rewards = if (crystal) crystalLoot() else muddyLoot()
        for ((type,count) in rewards) invAddOrDrop(ground,type,count)
        player.completeDiaryTask(if (crystal) DiaryTask.OpenTaverleyCrystalChest else DiaryTask.OpenMuddyChest)
    }

    private fun ProtectedAccess.crystalLoot() = buildList {
        add(objs.uncut_dragonstone to 1)
        when (random.of(128)) {
            in 0..33 -> { add(ChestItems.spinach to 1); add(objs.coins to 2000) }
            in 34..45 -> {
                for (rune in listOf(objs.air_rune,objs.earth_rune,objs.water_rune,objs.fire_rune,objs.body_rune,objs.mind_rune)) add(rune to 50)
                for (rune in listOf(objs.chaos_rune,objs.death_rune,objs.nature_rune,objs.law_rune,objs.cosmic_rune)) add(rune to 10)
            }
            in 46..57 -> { add(objs.ruby to 2); add(objs.diamond to 2) }
            in 58..69 -> add(objs.runite_bar to 3)
            in 70..79 -> { add((if (random.of(2) == 0) ChestItems.loop else ChestItems.tooth) to 1); add(objs.coins to 750) }
            in 80..89 -> add(ChestItems.iron to 150)
            in 90..99 -> add(ChestItems.coal to 100)
            in 100..107 -> { add(ChestItems.swordfish to 5); add(objs.coins to 1000) }
            in 108..109 -> add(ChestItems.shield to 1)
            110 -> add((if (random.of(2) == 0) ChestItems.legs else ChestItems.skirt) to 1)
        }
    }

    private fun ProtectedAccess.muddyLoot() = buildList {
        add(objs.uncut_ruby to 1)
        add(ChestItems.bars to 2)
        add(objs.law_rune to 5)
        add(objs.death_rune to 5)
        add(objs.chaos_rune to 15)
        if (player.members && random.of(40) < 3) add(ChestItems.wildExtras[random.of(7)])
    }
}
