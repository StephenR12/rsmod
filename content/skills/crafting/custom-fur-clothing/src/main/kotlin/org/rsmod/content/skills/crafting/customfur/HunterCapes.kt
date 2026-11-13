package org.rsmod.content.skills.crafting.customfur

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.api.type.refs.obj.ObjReferences

internal object FurDiaryVars : VarpReferences() { val made = find("varrock_spottier_cape_made") }
internal object FurDiaryState : VarpBuilder() {
    init { build("varrock_spottier_cape_made") { transmitNever = true; permanent = true } }
}
internal object FurWorn : ObjReferences() {
    val spotted = find("hunting_light_cape_worn")
    val spottier = find("hunting_lighter_cape_worn")
}
internal object HunterCapes : ObjEditor() {
    init {
        for ((inventory,worn,level) in listOf(Triple(objs.spotted_cape,FurWorn.spotted,40),Triple(objs.spottier_cape,FurWorn.spottier,66))) {
            edit(inventory) {
                param[params.statreq1_skill] = stats.hunter
                param[params.statreq1_level] = level
                weight = 453.grams
                transformlink = worn
            }
            edit(worn) {
                transformlink = inventory
                transformtemplate = objs.template_for_transform
                weight = (if(level == 40) -2267 else -4535).grams
            }
        }
    }
}
