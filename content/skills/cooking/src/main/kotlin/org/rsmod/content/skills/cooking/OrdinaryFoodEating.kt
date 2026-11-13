package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object OrdinaryFoodObjs : ObjReferences() {
    val seaTurtle = find("seaturtle")
    val mantaRay = find("mantaray")
    val rabbit = find("cooked_rabbit")
    val ugthanki = find("cooked_ugthanki_meat")
    val bakedPotato = find("potato_baked")
    val butterPotato = find("potato_butter")
    val cheesePotato = find("potato_cheese")
    val tunaPotato = find("potato_tuna+sweetcorn")
}

/** Fixed-heal, single-bite foods. Food oldid14989962; RuneLite ItemStatChanges corroborates heals. */
public object OrdinaryFoods {
    val all: Map<ObjType,Int> by lazy { mapOf(
        objs.anchovies to 1, objs.potato to 1, objs.onion to 1, objs.cabbage to 1,
        objs.tomato to 2, objs.banana to 2, objs.cheese to 2,
        objs.shrimps to 3, objs.cooked_meat to 3, objs.cooked_chicken to 3,
        objs.sardine to 4, OrdinaryFoodObjs.bakedPotato to 4,
        objs.bread to 5, objs.herring to 5, OrdinaryFoodObjs.rabbit to 5,
        objs.mackerel to 6, objs.trout to 7, objs.cod to 7, objs.pike to 8,
        objs.salmon to 9, objs.tuna to 10, objs.lobster to 12, objs.bass to 13,
        objs.swordfish to 14, objs.cooked_oomlie to 14, OrdinaryFoodObjs.butterPotato to 14,
        objs.monkfish to 16, OrdinaryFoodObjs.cheesePotato to 16,
        objs.shark to 20, OrdinaryFoodObjs.seaTurtle to 21,
        OrdinaryFoodObjs.mantaRay to 22, objs.dark_crab to 22, OrdinaryFoodObjs.tunaPotato to 22,
    ) }
}

public class OrdinaryFoodEating @Inject constructor(private val food: FoodConsumptionRepository, private val types: ObjTypeList) {
    public fun eat(access: ProtectedAccess, slot: Int, obj: ObjType, heal: Int) = with(access) {
        if (slot !in player.inv.indices || player.inv[slot]?.id != obj.id || !food.isReady(player)) return
        if (types[obj].members && !player.members) { mes("You need to be on a members' world to eat this food."); return }
        if (!player.invDel(player.inv, obj, 1, slot = slot).success) return
        stopAction()
        anim(seqs.human_eat)
        statHeal(stats.hitpoints, constant = heal, percent = 0)
        food.consume(player, eatDelay = 3, attackDelay = 3)
        if (obj == objs.cooked_oomlie) {
            mes("You eat the oomlie meat; it tastes very gamy.")
            player.completeDiaryTask(DiaryTask.EatOomlieWrap)
        } else mes("You eat the ${types[obj].name.lowercase()}.")
        publish(PlayerConsumableEvent.Consumed(player, obj, foodHealing = heal))
    }
}
public class OrdinaryFoodScript @Inject constructor(private val eating: OrdinaryFoodEating) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((obj, heal) in OrdinaryFoods.all) onOpHeld1(obj) { eating.eat(this, it.slot, obj, heal) }
    }
}
