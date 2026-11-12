package org.rsmod.content.skills.woodcutting.configs

import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

internal object WoodcuttingAxes : ObjEditor() {
    init {
        axe(objs.bronze_axe, level = 1, animation = seqs.human_woodcutting_bronze_axe)
        axe(objs.iron_axe, level = 1, animation = seqs.human_woodcutting_iron_axe)
        axe(objs.steel_axe, level = 6, animation = seqs.human_woodcutting_steel_axe)
        axe(objs.black_axe, level = 11, animation = seqs.human_woodcutting_black_axe)
        axe(objs.mithril_axe, level = 21, animation = seqs.human_woodcutting_mithril_axe)
        axe(objs.adamant_axe, level = 31, animation = seqs.human_woodcutting_adamant_axe)

        edit(objs.blessed_axe) {
            param[params.skill_anim] = seqs.human_woodcutting_blessed_axe
            // Blessed axe does not have its `levelrequire` param defined by default.
            param[params.levelrequire] = 35
        }

        axe(objs.rune_axe, level = 41, animation = seqs.human_woodcutting_rune_axe)
        axe(objs.gilded_axe, level = 41, animation = seqs.human_woodcutting_gilded_axe)
        axe(objs.dragon_axe, level = 61, animation = seqs.human_woodcutting_dragon_axe)
        axe(
            objs.dragon_axe_or,
            level = 61,
            animation = seqs.human_woodcutting_trailblazer_axe_no_infernal,
        )
        axe(
            objs.dragon_axe_or_reloaded,
            level = 61,
            animation = seqs.human_woodcutting_trailblazer_reloaded_axe_no_infernal,
        )
        axe(objs.third_age_axe, level = 61, animation = seqs.human_woodcutting_3a_axe)
        axe(objs.infernal_axe, level = 61, animation = seqs.human_woodcutting_infernal_axe)
        axe(
            objs.infernal_axe_uncharged,
            level = 61,
            animation = seqs.human_woodcutting_infernal_axe,
        )
        axe(objs.infernal_axe_or, level = 61, animation = seqs.human_woodcutting_trailblazer_axe)
        axe(
            objs.infernal_axe_or_uncharged,
            level = 61,
            animation = seqs.human_woodcutting_trailblazer_axe,
        )
        axe(
            objs.infernal_axe_or_reloaded,
            level = 61,
            animation = seqs.human_woodcutting_trailblazer_reloaded_axe,
        )
        axe(
            objs.infernal_axe_or_reloaded_uncharged,
            level = 61,
            animation = seqs.human_woodcutting_trailblazer_reloaded_axe,
        )
        axe(objs.crystal_axe, level = 71, animation = seqs.human_woodcutting_crystal_axe)
        axe(objs.crystal_axe_inactive, level = 71, animation = seqs.human_woodcutting_dragon_axe)
    }

    private fun axe(type: ObjType, level: Int, animation: SeqType) {
        edit(type) {
            contentGroup = content.woodcutting_axe
            param[params.levelrequire] = level
            param[params.skill_anim] = animation
        }
    }
}
