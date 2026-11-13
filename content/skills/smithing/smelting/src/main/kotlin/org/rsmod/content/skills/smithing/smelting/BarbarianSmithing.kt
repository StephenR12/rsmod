package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object BarbarianAnvilLocs : LocReferences() {
    val anvil = find("brut_anvil")
    val bed = find("brut_barbarian_bed")
}

public object BarbarianSmithingBits : VarBitReferences() {
    val spear = find("brut_smith_spear")
    val hasta = find("brut_smith_hasta")
}

public data class BarbarianWeaponRecipe(
    val bar: ObjType,
    val logs: ObjType,
    val spear: ObjType,
    val hasta: ObjType,
    val level: Int,
    val xp: Double,
)

public object BarbarianWeapons : ObjReferences() {
    val all =
        listOf(
            BarbarianWeaponRecipe(
                objs.bronze_bar,
                objs.logs,
                find("bronze_spear"),
                find("brut_bronze_spear"),
                5,
                25.0,
            ),
            BarbarianWeaponRecipe(
                objs.iron_bar,
                objs.oak_logs,
                find("iron_spear"),
                find("brut_iron_spear"),
                20,
                50.0,
            ),
            BarbarianWeaponRecipe(
                objs.steel_bar,
                objs.willow_logs,
                find("steel_spear"),
                find("brut_steel_spear"),
                35,
                75.0,
            ),
            BarbarianWeaponRecipe(
                objs.mithril_bar,
                objs.maple_logs,
                find("mithril_spear"),
                find("brut_mithril_spear"),
                55,
                100.0,
            ),
            BarbarianWeaponRecipe(
                objs.adamantite_bar,
                objs.yew_logs,
                find("adamant_spear"),
                find("brut_adamant_spear"),
                75,
                125.0,
            ),
            BarbarianWeaponRecipe(
                objs.runite_bar,
                objs.magic_logs,
                find("rune_spear"),
                find("brut_rune_spear"),
                90,
                150.0,
            ),
        )
}

/** Barbarian Training14988720; native anvil25349 and ordinary metal recipes. */
public class BarbarianSmithing
@Inject
constructor(private val smithing: AnvilSmithing, private val types: ObjTypeList) {
    public suspend fun select(
        access: ProtectedAccess,
        anvil: BoundLocInfo,
        bar: ObjType? = null,
    ): Unit =
        with(access) {
            val recipe =
                BarbarianWeapons.all.firstOrNull {
                    (bar == null || it.bar == bar) && it.bar in player.inv
                }
            if (recipe == null) {
                mes("You need a metal bar to smith a spear or hasta.")
                return
            }
            val hasta =
                if (vars[BarbarianSmithingBits.hasta] >= 1)
                    choice2("Make a spear.", false, "Make a hasta.", true)
                else false
            if (!canMake(this, anvil, recipe, hasta)) return
            val output = if (hasta) recipe.hasta else recipe.spear
            val max =
                minOf(player.inv.count(types[recipe.bar]), player.inv.count(types[recipe.logs]))
            val input =
                skillMulti(
                    "What would you like to smith?",
                    listOf(output),
                    max,
                    max,
                    actionMode = 0,
                )
            if (input.component.packed == components.skillmulti_a.packed)
                make(this, anvil, recipe, hasta, input.subcomponent)
        }

    public suspend fun make(
        access: ProtectedAccess,
        anvil: BoundLocInfo,
        recipe: BarbarianWeaponRecipe,
        hasta: Boolean,
        quantity: Int,
    ): Unit =
        with(access) {
            if (quantity <= 0 || !canMake(this, anvil, recipe, hasta)) return
            clearFurnaceQueues()
            repeat(minOf(quantity, 28)) {
                if (!canMake(this, anvil, recipe, hasta)) return
                faceLoc(anvil)
                anim(AnvilSeqs.smith)
                delay(smithing.actionTicks(player))
                if (!canMake(this, anvil, recipe, hasta)) {
                    resetAnim()
                    return
                }
                val output = if (hasta) recipe.hasta else recipe.spear
                val result =
                    player.invTransaction(player.inv) {
                        val inv = select(player.inv)
                        delete(inv, recipe.bar.id, 1)
                        delete(inv, recipe.logs.id, 1)
                        add(inv, output.id, 1)
                    }
                if (!result.success) {
                    resetAnim()
                    return
                }
                statAdvance(stats.smithing, recipe.xp)
                mes("You make a ${types[output].name.lowercase()}.")
                val atOtto = anvil.x == 2502 && anvil.z == 3485 && anvil.level == 0
                val progress =
                    if (hasta) BarbarianSmithingBits.hasta else BarbarianSmithingBits.spear
                if (atOtto && vars[progress] == 1) {
                    vars[progress] = 2
                    mes(
                        "You feel you have learned more of barbarian ways. Otto might wish to talk to you more."
                    )
                }
                if (atOtto && output == BarbarianWeapons.all[4].spear)
                    player.completeDiaryTask(DiaryTask.SmithOttoAdamantSpear)
                if (atOtto && output == BarbarianWeapons.all[5].hasta)
                    player.completeDiaryTask(DiaryTask.SmithOttoRuneHasta)
            }
            resetAnim()
        }

    private fun canMake(
        access: ProtectedAccess,
        anvil: BoundLocInfo,
        recipe: BarbarianWeaponRecipe,
        hasta: Boolean,
    ): Boolean =
        with(access) {
            if (
                !player.members ||
                    anvil.id != BarbarianAnvilLocs.anvil.id ||
                    !isWithinDistance(anvil, 1)
            )
                return false
            if (vars[varps.tbwt_main] < 6) {
                mes("You need to complete Tai Bwo Wannai Trio before smithing barbarian weapons.")
                return false
            }
            val progress = if (hasta) BarbarianSmithingBits.hasta else BarbarianSmithingBits.spear
            if (vars[progress] < 1) {
                mes("You need to ask Otto about ${if(hasta)"hastae" else "spears"} first.")
                return false
            }
            if (player.stat(stats.smithing) < recipe.level) {
                mes("You need a Smithing level of ${recipe.level} to smith this item.")
                return false
            }
            val hammers = listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand)
            if (hammers.none { it in player.inv } && hammers.drop(1).none { it in player.worn }) {
                mes("You need a hammer to smith items.")
                return false
            }
            if (recipe.bar !in player.inv || recipe.logs !in player.inv) {
                mes("You need a metal bar and the corresponding logs.")
                return false
            }
            true
        }
}

public class BarbarianSmithingScript @Inject constructor(private val smithing: BarbarianSmithing) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(BarbarianAnvilLocs.anvil) { smithing.select(this, it.loc) }
        for (recipe in BarbarianWeapons.all) {
            onOpLocU(BarbarianAnvilLocs.anvil, recipe.bar) {
                smithing.select(this, it.loc, recipe.bar)
            }
        }
    }
}
