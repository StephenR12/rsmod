package org.rsmod.content.skills.crafting.glassmaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.content.skills.smithing.smelting.furnace_locs
import org.rsmod.content.skills.smithing.smelting.furnace_queues
import org.rsmod.game.type.loc.LocType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class GlassblowingScript
@Inject
constructor(
    private val glassblowing: Glassblowing,
    private val moltenGlassmaking: MoltenGlassmaking,
    private val assembly: GlassAssembly,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(objs.glassblowing_pipe, objs.molten_glass) { glassblowing.select(this) }
        onOpHeldU(objs.empty_oil_lamp, objs.oil_lantern_frame) { assembly.makeOilLantern(this) }
        onOpHeldU(objs.empty_light_orb, objs.cave_goblin_wire) { assembly.wireLightOrb(this) }
        for (furnace in furnace_locs.all) {
            registerGlassmaking(furnace)
        }
        onPlayerQueueWithArgs<GlassblowingRequest>(crafting_queues.glassblowing) {
            glassblowing.process(this, it.args)
        }
        onPlayerQueueWithArgs<MoltenGlassmakingRequest>(furnace_queues.moltenGlassmaking) {
            moltenGlassmaking.process(this, it.args)
        }
    }

    private fun ScriptContext.registerGlassmaking(furnace: LocType) {
        onOpLocU(furnace, objs.bucket_of_sand) { moltenGlassmaking.select(this, it.loc) }
        onOpLocU(furnace, objs.soda_ash) { moltenGlassmaking.select(this, it.loc) }
    }
}
