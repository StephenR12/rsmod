package org.rsmod.content.skills.skillcapes

import com.google.inject.Scopes
import org.rsmod.api.obj.plugin.ObjTakeTransform
import org.rsmod.plugin.module.PluginModule

public class SkillcapeModule : PluginModule() {
    override fun bind() {
        bind(SkillcapeBoost::class.java).`in`(Scopes.SINGLETON)
        bind(SkillcapeTrimming::class.java).`in`(Scopes.SINGLETON)
        addSetBinding<ObjTakeTransform>(SkillcapeGroundObjTransform::class.java)
    }
}
