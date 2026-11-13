package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.api.type.builders.area.AreaBuilder
import org.rsmod.api.type.builders.map.area.MapAreaBuilder
import org.rsmod.api.type.refs.area.AreaReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.game.area.polygon.VertexCoord
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WildMagicAreas : AreaReferences() {
    val fountain = find("fountain_of_rune")
    val ruins = find("demonic_ruins_prayer")
    val lumberYard = find("varrock_lumber_yard")
}

internal object WildMagicAreaTypes : AreaBuilder() {
    init {
        build("fountain_of_rune")
        build("demonic_ruins_prayer")
        build("varrock_lumber_yard")
    }
}

internal object WildMagicTimer : TimerReferences() {
    val restore = find("demonic_ruins_restore")
}

internal object WildMagicMap : MapAreaBuilder() {
    override fun onPackMapTask() {
        area(WildMagicAreas.fountain) {
            polygon {
                fun tile(x: Int, z: Int) = vertex(VertexCoord.from(CoordGrid(x, z)))
                tile(3366, 3899)
                tile(3367, 3899)
                tile(3367, 3898)
                tile(3368, 3898)
                tile(3368, 3897)
                tile(3369, 3897)
                tile(3369, 3896)
                tile(3368, 3896)
                tile(3368, 3893)
                tile(3367, 3893)
                tile(3367, 3890)
                tile(3377, 3890)
                tile(3377, 3889)
                tile(3378, 3889)
                tile(3378, 3888)
                tile(3379, 3888)
                tile(3379, 3887)
                tile(3380, 3887)
                tile(3380, 3886)
                tile(3381, 3886)
                tile(3381, 3885)
                tile(3388, 3885)
                tile(3388, 3895)
                tile(3387, 3896)
                tile(3386, 3897)
                tile(3385, 3898)
                tile(3384, 3899)
                tile(3383, 3900)
                tile(3382, 3901)
                tile(3381, 3902)
                tile(3380, 3903)
                tile(3366, 3903)
            }
        }
        area(WildMagicAreas.lumberYard) {
            polygon {
                vertex(VertexCoord.from(CoordGrid(3298, 3518)))
                vertex(VertexCoord.from(CoordGrid(3293, 3513)))
                vertex(VertexCoord.from(CoordGrid(3293, 3504)))
                vertex(VertexCoord.from(CoordGrid(3296, 3501)))
                vertex(VertexCoord.from(CoordGrid(3296, 3497)))
                vertex(VertexCoord.from(CoordGrid(3300, 3493)))
                vertex(VertexCoord.from(CoordGrid(3300, 3492)))
                vertex(VertexCoord.from(CoordGrid(3306, 3492)))
                vertex(VertexCoord.from(CoordGrid(3307, 3493)))
                vertex(VertexCoord.from(CoordGrid(3314, 3493)))
                vertex(VertexCoord.from(CoordGrid(3326, 3505)))
                vertex(VertexCoord.from(CoordGrid(3326, 3514)))
                vertex(VertexCoord.from(CoordGrid(3322, 3518)))
            }
        }
        area(WildMagicAreas.ruins) {
            polygon {
                vertex(VertexCoord.from(CoordGrid(3272, 3872)))
                vertex(VertexCoord.from(CoordGrid(3303, 3872)))
                vertex(VertexCoord.from(CoordGrid(3303, 3895)))
                vertex(VertexCoord.from(CoordGrid(3272, 3895)))
            }
        }
    }
}

public class WildernessMagicAreas : PluginScript() {
    override fun ScriptContext.startup() {
        onArea(WildMagicAreas.fountain) {
            vars[varbits.fountain_of_rune_active] = if (player.members) 1 else 0
        }
        onAreaExit(WildMagicAreas.fountain) { vars[varbits.fountain_of_rune_active] = 0 }
        onArea(WildMagicAreas.ruins) {
            mes(
                "A dark and ancient energy is emitted from the ruins granting you faster prayer restoration."
            )
            timer(WildMagicTimer.restore, 5)
        }
        onAreaExit(WildMagicAreas.ruins) { clearTimer(WildMagicTimer.restore) }
        onPlayerTimer(WildMagicTimer.restore) {
            if (coords.level != 0 || coords.x !in 3272..3303 || coords.z !in 3872..3895) {
                clearTimer(WildMagicTimer.restore)
            } else if (stat(stats.prayer) < statBase(stats.prayer)) {
                statAdd(stats.prayer, 1, 0)
                player.completeDiaryTask(DiaryTask.RestoreAtDemonicRuins)
            }
        }
    }
}
