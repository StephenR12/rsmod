package org.rsmod.content.skills.farming

import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.plugin.module.PluginModule

public class FarmingModule : PluginModule() {
    override fun bind() {
        bindInstance<FarmingCropCatalogue>()
        bindInstance<FarmingPatchRepository>()
        bindInstance<FarmingPatchApplier>()
        addSetBinding<CharacterDataStage.Pipeline>(FarmingPatchPipeline::class.java)
    }
}
