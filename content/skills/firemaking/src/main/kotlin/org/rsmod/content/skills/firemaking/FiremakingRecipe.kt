package org.rsmod.content.skills.firemaking

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

public data class FiremakingRecipe(
    val log: ObjType,
    val level: Int,
    val xp: Double,
    val members: Boolean,
    val fire: LocType = firemaking_locs.fire,
    val guaranteed: Boolean = false,
)

public object FiremakingRecipes {
    public val ordinary: List<FiremakingRecipe> by lazy {
        listOf(
            FiremakingRecipe(objs.logs, level = 1, xp = 40.0, members = false),
            FiremakingRecipe(objs.achey_tree_logs, level = 1, xp = 40.0, members = true),
            FiremakingRecipe(objs.oak_logs, level = 15, xp = 60.0, members = false),
            FiremakingRecipe(objs.willow_logs, level = 30, xp = 90.0, members = false),
            FiremakingRecipe(objs.teak_logs, level = 35, xp = 105.0, members = true),
            FiremakingRecipe(objs.arctic_pine_logs, level = 42, xp = 125.0, members = true),
            FiremakingRecipe(objs.maple_logs, level = 45, xp = 135.0, members = false),
            FiremakingRecipe(objs.mahogany_logs, level = 50, xp = 157.5, members = true),
            FiremakingRecipe(objs.yew_logs, level = 60, xp = 202.5, members = false),
            FiremakingRecipe(objs.blisterwood_logs, level = 62, xp = 96.0, members = true),
            FiremakingRecipe(objs.magic_logs, level = 75, xp = 303.8, members = true),
            FiremakingRecipe(objs.redwood_logs, level = 90, xp = 350.0, members = true),
        )
    }

    public val coloured: List<FiremakingRecipe> by lazy {
        ColouredLogRecipes.all.map {
            FiremakingRecipe(
                log = it.log,
                level = 1,
                xp = 50.0,
                members = true,
                fire = it.fire,
                guaranteed = true,
            )
        }
    }

    public val all: List<FiremakingRecipe> by lazy { ordinary + coloured }

    public val byLog: Map<Int, FiremakingRecipe> by lazy { all.associateBy { it.log.id } }

    public val barbarian: List<FiremakingRecipe> by lazy {
        all.filterNot { it.log == objs.blisterwood_logs }
    }
}

public data class ColouredLogRecipe(
    val colour: String,
    val firelighter: ObjType,
    val log: ObjType,
    val fire: LocType,
)

public object ColouredLogRecipes {
    public val all: List<ColouredLogRecipe> by lazy {
        listOf(
            ColouredLogRecipe("red", objs.red_firelighter, objs.red_logs, firemaking_locs.redFire),
            ColouredLogRecipe(
                "green",
                objs.green_firelighter,
                objs.green_logs,
                firemaking_locs.greenFire,
            ),
            ColouredLogRecipe(
                "blue",
                objs.blue_firelighter,
                objs.blue_logs,
                firemaking_locs.blueFire,
            ),
            ColouredLogRecipe(
                "purple",
                objs.purple_firelighter,
                objs.purple_logs,
                firemaking_locs.purpleFire,
            ),
            ColouredLogRecipe(
                "white",
                objs.white_firelighter,
                objs.white_logs,
                firemaking_locs.whiteFire,
            ),
        )
    }
}

public data class BowFiremakingTool(val bow: ObjType, val sequence: SeqType)

public object BowFiremakingTools {
    public val all: List<BowFiremakingTool> by lazy {
        listOf(
            BowFiremakingTool(objs.training_bow, seqs.brut_player_firemaking_aide_shortbow),
            BowFiremakingTool(objs.shortbow, seqs.brut_player_firemaking_shortbow),
            BowFiremakingTool(objs.longbow, seqs.brut_player_firemaking_shortbow),
            BowFiremakingTool(objs.oak_shortbow, seqs.brut_player_firemaking_oak_shortbow),
            BowFiremakingTool(objs.oak_longbow, seqs.brut_player_firemaking_oak_shortbow),
            BowFiremakingTool(objs.willow_shortbow, seqs.brut_player_firemaking_willow_shortbow),
            BowFiremakingTool(objs.willow_longbow, seqs.brut_player_firemaking_willow_shortbow),
            BowFiremakingTool(objs.maple_shortbow, seqs.brut_player_firemaking_maple_shortbow),
            BowFiremakingTool(objs.maple_longbow, seqs.brut_player_firemaking_maple_shortbow),
            BowFiremakingTool(objs.yew_shortbow, seqs.brut_player_firemaking_yew_shortbow),
            BowFiremakingTool(objs.yew_longbow, seqs.brut_player_firemaking_yew_shortbow),
            BowFiremakingTool(objs.magic_shortbow, seqs.brut_player_firemaking_magic_shortbow),
            BowFiremakingTool(objs.magic_longbow, seqs.brut_player_firemaking_magic_shortbow),
            BowFiremakingTool(objs.magic_shortbow_i, seqs.brut_player_firemaking_magic_shortbow),
            BowFiremakingTool(objs.seercull, seqs.brut_player_firemaking_daganoth_bow),
        )
    }

    public val byBow: Map<Int, BowFiremakingTool> by lazy { all.associateBy { it.bow.id } }
}
