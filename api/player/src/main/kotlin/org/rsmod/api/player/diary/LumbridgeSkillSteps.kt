package org.rsmod.api.player.diary

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varp.VarpType
import org.rsmod.map.CoordGrid

public object LumbridgeDiarySkillArea {
    public fun contains(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 3200..3263 && coords.z in 3190..3263

    public fun alKharidFishing(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 3260..3287 && coords.z in 3135..3175

    public fun alKharidMine(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 3280..3310 && coords.z in 3280..3325
}

public object LumbridgeSkillStepVars : VarpReferences() {
    public val oakCut: VarpType = find("diary_lumbridge_oak_cut")
}

internal object LumbridgeSkillStepBuilder : VarpBuilder() {
    init {
        build("diary_lumbridge_oak_cut") { transmitNever = true }
    }
}

public fun Player.recordLumbridgeOakCut(coords: CoordGrid) {
    if (
        members &&
            LumbridgeDiarySkillArea.contains(coords) &&
            !hasCompletedDiaryTask(DiaryTask.ChopAndBurnLumbridgeOak)
    ) {
        if (vars[LumbridgeSkillStepVars.oakCut] == 0) { interruptDiaryProductionSequence(DiaryTask.ChopAndBurnLumbridgeOak); interruptDiaryKillSequence() }
        VarPlayerIntMapSetter.set(this, LumbridgeSkillStepVars.oakCut, 1)
    }
}

public fun Player.recordLumbridgeOakFire(coords: CoordGrid) {
    if (
        members &&
            LumbridgeDiarySkillArea.contains(coords) &&
            vars[LumbridgeSkillStepVars.oakCut] != 0
    ) {
        completeDiaryTask(DiaryTask.ChopAndBurnLumbridgeOak)
        VarPlayerIntMapSetter.set(this, LumbridgeSkillStepVars.oakCut, 0)
    }
}
