package org.rsmod.content.skills.fletching.logcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class LogFletchingRecipe(
    val name: String,
    val level: Int,
    val log: ObjType,
    val logCount: Int,
    val output: ObjType,
    val outputCount: Int,
    val xp: Double,
)

public class LogFletchingRecipeRegistry @Inject constructor() {
    public val recipes: List<LogFletchingRecipe> =
        listOf(
            recipe("Arrow shafts", 1, objs.logs, objs.arrow_shafts, 5.0, outputCount = 15),
            recipe("Javelin shafts", 3, objs.logs, objs.javelin_shafts, 5.0, outputCount = 15),
            recipe("Shortbow (u)", 5, objs.logs, objs.unstrung_shortbow, 5.0),
            recipe("Longbow (u)", 10, objs.logs, objs.unstrung_longbow, 10.0),
            recipe("Wooden stock", 9, objs.logs, objs.wooden_stock, 6.0),
            recipe("Arrow shafts", 15, objs.oak_logs, objs.arrow_shafts, 10.0, outputCount = 30),
            recipe("Oak shortbow (u)", 20, objs.oak_logs, objs.unstrung_oak_shortbow, 16.5),
            recipe("Oak longbow (u)", 25, objs.oak_logs, objs.unstrung_oak_longbow, 25.0),
            recipe("Oak stock", 24, objs.oak_logs, objs.oak_stock, 16.0),
            recipe("Oak shield", 27, objs.oak_logs, objs.oak_shield, 50.0, logCount = 2),
            recipe("Arrow shafts", 30, objs.willow_logs, objs.arrow_shafts, 15.0, outputCount = 45),
            recipe(
                "Willow shortbow (u)",
                35,
                objs.willow_logs,
                objs.unstrung_willow_shortbow,
                33.3,
            ),
            recipe("Willow longbow (u)", 40, objs.willow_logs, objs.unstrung_willow_longbow, 41.5),
            recipe("Willow stock", 39, objs.willow_logs, objs.willow_stock, 22.0),
            recipe("Willow shield", 42, objs.willow_logs, objs.willow_shield, 83.0, logCount = 2),
            recipe("Arrow shafts", 45, objs.maple_logs, objs.arrow_shafts, 20.0, outputCount = 60),
            recipe("Maple shortbow (u)", 50, objs.maple_logs, objs.unstrung_maple_shortbow, 50.0),
            recipe("Maple longbow (u)", 55, objs.maple_logs, objs.unstrung_maple_longbow, 58.3),
            recipe("Maple stock", 54, objs.maple_logs, objs.maple_stock, 32.0),
            recipe("Maple shield", 57, objs.maple_logs, objs.maple_shield, 116.5, logCount = 2),
            recipe("Arrow shafts", 60, objs.yew_logs, objs.arrow_shafts, 25.0, outputCount = 75),
            recipe("Yew shortbow (u)", 65, objs.yew_logs, objs.unstrung_yew_shortbow, 67.5),
            recipe("Yew longbow (u)", 70, objs.yew_logs, objs.unstrung_yew_longbow, 75.0),
            recipe("Yew stock", 69, objs.yew_logs, objs.yew_stock, 50.0),
            recipe("Yew shield", 72, objs.yew_logs, objs.yew_shield, 150.0, logCount = 2),
            recipe("Arrow shafts", 75, objs.magic_logs, objs.arrow_shafts, 30.0, outputCount = 90),
            recipe("Magic shortbow (u)", 80, objs.magic_logs, objs.unstrung_magic_shortbow, 83.3),
            recipe("Magic longbow (u)", 85, objs.magic_logs, objs.unstrung_magic_longbow, 91.5),
            recipe("Magic stock", 78, objs.magic_logs, objs.magic_stock, 70.0),
            recipe("Magic shield", 87, objs.magic_logs, objs.magic_shield, 183.0, logCount = 2),
            recipe("Teak stock", 46, objs.teak_logs, objs.teak_stock, 27.0),
            recipe("Mahogany stock", 61, objs.mahogany_logs, objs.mahogany_stock, 41.0),
            recipe(
                "Arrow shafts",
                90,
                objs.redwood_logs,
                objs.arrow_shafts,
                35.0,
                outputCount = 105,
            ),
            recipe("Redwood hiking staff", 90, objs.redwood_logs, objs.redwood_hiking_staff, 10.5),
            recipe(
                "Redwood shield",
                92,
                objs.redwood_logs,
                objs.redwood_shield,
                216.0,
                logCount = 2,
            ),
            recipe("Atlatl dart shafts", 74, objs.ent_branch, objs.atlatl_dart_shaft, 30.0, outputCount = 100),
        )

    private val byLog: Map<ObjType, List<LogFletchingRecipe>> = recipes.groupBy { it.log }

    public val logs: Set<ObjType> = byLog.keys

    public fun forLog(log: ObjType): List<LogFletchingRecipe> = byLog[log].orEmpty()

    public fun forButton(log: ObjType, button: ComponentType): LogFletchingRecipe? {
        val index = buttons.indexOfFirst { it.isType(button) }
        return forLog(log).getOrNull(index)
    }

    private companion object {
        val buttons =
            listOf(
                components.skillmulti_a,
                components.skillmulti_b,
                components.skillmulti_c,
                components.skillmulti_d,
                components.skillmulti_e,
            )

        fun recipe(
            name: String,
            level: Int,
            log: ObjType,
            output: ObjType,
            xp: Double,
            logCount: Int = 1,
            outputCount: Int = 1,
        ) = LogFletchingRecipe(name, level, log, logCount, output, outputCount, xp)
    }
}
