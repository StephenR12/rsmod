package org.rsmod.content.skills.cooking.gnome

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.script.onOpHeld1
import org.rsmod.content.skills.cooking.FoodConsumptionRepository
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Handmade gnome foods are combo foods: independent3-tick timer and2-tick attack penalty. */
public class GnomeFoodEating
@Inject
constructor(private val food: FoodConsumptionRepository, private val types: ObjTypeList) :
    PluginScript() {
    public val heals: Map<ObjType, Int> by lazy {
        val normal =
            mapOf(
                g("toad_crunchies") to 8,
                g("spicy_crunchies") to 7,
                g("worm_crunchies") to 8,
                g("chocchip_crunchies") to 7,
                g("fruit_batta") to 11,
                g("toad_batta") to 11,
                g("worm_batta") to 11,
                g("vegetable_batta") to 11,
                g("cheese+tom_batta") to 11,
                g("worm_hole") to 12,
                g("veg_ball") to 12,
                g("tangled_toads_legs") to 15,
                g("chocolate_bomb") to 15,
            )
        normal +
            normal.entries.associate { (obj, heal) ->
                g("premade_" + types[obj].internalName) to heal
            } +
            (objs.cooked_karambwan to 18)
    }

    override fun ScriptContext.startup() {
        for ((obj, heal) in heals) onOpHeld1(obj) { eat(this, it.slot, obj, heal) }
        for (drink in GnomeRecipes.cocktails) onOpHeld1(drink.result) {
            drink(this, it.slot, drink)
        }
        for (kind in listOf("stick", "shaft")) {
            val obj = g("tbw_spider_on_${kind}_cooked")
            onOpHeld1(obj) { spider(this, it.slot, obj, kind == "stick") }
        }
    }

    public fun eat(access: ProtectedAccess, slot: Int, obj: ObjType, heal: Int): Unit =
        with(access) {
            if (!ready(this, slot, obj, combo = true)) return
            if (!replace(this, slot, obj)) return
            statHeal(stats.hitpoints, heal, 0)
            consumed(this, obj, heal, 2, combo = true)
        }

    public fun spider(access: ProtectedAccess, slot: Int, obj: ObjType, stick: Boolean): Unit =
        with(access) {
            if (!ready(this, slot, obj)) return
            if (!replace(this, slot, obj, if (stick) g("tbw_skewer_stick") else null)) return
            val heal = random.of(7..10)
            statHeal(stats.hitpoints, heal, 0)
            consumed(this, obj, heal, 3)
        }

    public fun drink(access: ProtectedAccess, slot: Int, drink: GnomeCocktail): Unit =
        with(access) {
            if (!ready(this, slot, drink.result)) return
            if (!replace(this, slot, drink.result, g("cocktail_glass_empty"))) return
            val heal =
                when (drink.result) {
                    g("fruit_blast"),
                    g("pineapple_punch") -> 9
                    g("blurberry_special") -> 7
                    else -> 5
                }
            statHeal(stats.hitpoints, heal, 0)
            if (drink.result != g("fruit_blast") && drink.result != g("pineapple_punch")) {
                val constant =
                    if (drink.result == g("wizard_blizzard") || drink.result == g("sgg")) 1 else 2
                val percent = if (drink.result == g("wizard_blizzard")) 6 else 5
                statBoost(stats.strength, constant, percent)
                statSub(stats.attack, 3 + player.stat(stats.attack) * 2 / 100, 0)
            }
            consumed(this, drink.result, heal, 2)
        }

    private fun ready(
        access: ProtectedAccess,
        slot: Int,
        obj: ObjType,
        combo: Boolean = false,
    ): Boolean =
        with(access) {
            player.members &&
                slot in inv.indices &&
                inv[slot]?.id == obj.id &&
                (if (combo) food.isComboReady(player) else food.isReady(player))
        }

    private fun replace(
        access: ProtectedAccess,
        slot: Int,
        input: ObjType,
        output: ObjType? = null,
    ): Boolean =
        with(access) {
            player
                .invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, input.id, 1, slot)
                    if (output != null) add(bag, output.id, 1, slot)
                }
                .success
        }

    private fun consumed(
        access: ProtectedAccess,
        obj: ObjType,
        heal: Int,
        attack: Int,
        combo: Boolean = false,
    ): Unit =
        with(access) {
            stopAction()
            anim(seqs.human_eat)
            if (combo) food.consumeCombo(player) else food.consume(player, 3, attack)
            mes(
                "You ${if(GnomeRecipes.cocktails.any{it.result==obj})"drink"else"eat"} the ${types[obj].name.lowercase()}."
            )
            publish(PlayerConsumableEvent.Consumed(player, obj, foodHealing = heal))
        }
}
