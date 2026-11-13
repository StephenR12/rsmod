package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AquariumGoods : ObjReferences() {
    val prepared = find("fishbowl_water_weed")
    val net = find("tiny_net")
    val seaweed = find("seaweed")
    val fish = listOf("bluefish","greenfish","spinefish").map { find("fishbowl_$it") }
    val food = find("fish_food")
    val box = find("empty_fishfood_box")
    val guamBox = find("guam_fishfood_box")
    val seaweedBox = find("seaweed_fishfood_box")
    val guam = find("ground_guam")
    val groundSeaweed = find("ground_seaweed")
}
internal object AquariumSequences : SeqReferences() {
    val actions = listOf("blue","green","spine").map { colour ->
        listOf("talk_to","play_with","feed").map { find("human_${it}_pet_fish_$colour") }
    }
}
internal object AquariumTanks : LocReferences() { val tank = find("aquarium") }
internal object CatherbyCitizens : NpcReferences() { val harry = find("harry") }

public class CatherbyAquarium @Inject constructor(private val locs:LocTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        val goods = AquariumGoods
        for (loc in locs.values.filter { it.name == "Sink" || it.name == "Waterpump" || it.id == 879 }) {
            onOpLocU(loc,objs.fishbowl_empty) {
                if (player.members) invReplaceSlot(inv,it.invSlot,1,objs.fishbowl_water)
            }
        }
        onOpHeldU(objs.fishbowl_water,goods.seaweed) { combine(objs.fishbowl_water,goods.seaweed,goods.prepared) }
        for (bowl in listOf(objs.fishbowl_water,goods.prepared)) onOpHeld3(bowl) {
            invReplaceSlot(inv,it.slot,1,objs.fishbowl_empty)
        }
        onOpNpc1(CatherbyCitizens.harry) {
            if (!player.members) return@onOpNpc1
            if (!choice2("I'd like a tiny net for a pet fish.",true,"Leave.",false)) return@onOpNpc1
            if (goods.prepared !in inv) { mes("Bring me a fishbowl filled with water and seaweed."); return@onOpNpc1 }
            val result = player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag,objs.coins.id,10)
                add(bag,goods.net.id,1)
            }
            mes(if (result.success) "Harry sells you a tiny net." else "You need 10 coins and room for the net.")
        }
        onOpLoc1(AquariumTanks.tank) { catchFish() }
        onOpLocU(AquariumTanks.tank,goods.net) { catchFish() }
        for ((i,fish) in goods.fish.withIndex()) {
            onOpHeld1(fish) { anim(AquariumSequences.actions[i][0]); mes("Your fish looks at you through the glass.") }
            onOpHeld2(fish) { anim(AquariumSequences.actions[i][1]); mes("Your fish swims around excitedly.") }
            onOpHeld3(fish) { feed(i) }
            onOpHeldU(goods.food,fish) { feed(i) }
        }
        onOpHeldU(goods.box,goods.groundSeaweed) { combine(goods.box,goods.groundSeaweed,goods.seaweedBox) }
        onOpHeldU(goods.box,goods.guam) { combine(goods.box,goods.guam,goods.guamBox) }
        onOpHeldU(goods.guamBox,goods.groundSeaweed) { combine(goods.guamBox,goods.groundSeaweed,goods.food) }
        onOpHeldU(goods.seaweedBox,goods.guam) { combine(goods.seaweedBox,goods.guam,goods.food) }
    }

    private fun ProtectedAccess.feed(index:Int) {
        if (!invReplace(inv,AquariumGoods.food,1,AquariumGoods.box).success) {
            mes("You need some fish food.")
            return
        }
        anim(AquariumSequences.actions[index][2])
        mes("Your fish nibbles the food.")
    }

    private fun ProtectedAccess.combine(container:ObjType,ingredient:ObjType,result:ObjType) {
        if (!player.members) return
        player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,container.id,1)
            delete(bag,ingredient.id,1)
            add(bag,result.id,1)
        }
    }

    private fun ProtectedAccess.catchFish() {
        if (!player.members) return
        val level = stat(stats.fishing)
        if (level < 10) { mes("You need level 10 Fishing to catch a pet fish."); return }
        if (AquariumGoods.prepared !in inv || AquariumGoods.net !in inv) {
            mes("You need a tiny net and a fishbowl containing water and seaweed.")
            return
        }
        val greenChance = if (level >= 70) 128 else 25 + (87*(level-1)+49)/98
        val species = when {
            level >= 70 && random.of(256) <= 51 -> 2
            random.of(256) <= greenChance -> 1
            else -> 0
        }
        if (invReplace(inv,AquariumGoods.prepared,1,AquariumGoods.fish[species]).success) {
            statAdvance(stats.fishing,1.0)
            player.completeDiaryTask(DiaryTask.CatchHarrysPetFish)
            mes("You catch a pet fish and slip it into the bowl.")
        }
    }
}
