package org.rsmod.content.skills.skillcapes

import jakarta.inject.Inject
import org.rsmod.api.obj.plugin.ObjTakeTransform
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj

/** Trims an ordinary Skillcape acquired through the ground-object Take operation. */
public class SkillcapeGroundObjTransform
@Inject
constructor(private val trimming: SkillcapeTrimming) : ObjTakeTransform {
    override fun transform(player: Player, obj: Obj): Int? =
        trimming.groundPickupType(player, obj.type)
}
