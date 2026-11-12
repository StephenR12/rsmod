package org.rsmod.content.skills.mining.configs

import org.rsmod.api.type.builders.param.ParamBuilder
import org.rsmod.api.type.refs.param.ParamReferences

object MiningParams : ParamReferences() {
    val pickaxeInterval = find<Int>("mining_pickaxe_interval")
    val pickaxeFastIntervalChance = find<Int>("mining_pickaxe_fast_interval_chance")
    val pickaxeSoteRequired = find<Boolean>("mining_pickaxe_sote_required")
    val gemRock = find<Boolean>("mining_gem_rock")
    val successLow = find<Int>("mining_success_low")
    val successHigh = find<Int>("mining_success_high")
}

internal object MiningParamBuilder : ParamBuilder() {
    init {
        build<Int>("mining_pickaxe_interval")
        build<Int>("mining_pickaxe_fast_interval_chance") { default = 0 }
        build<Boolean>("mining_pickaxe_sote_required") { default = false }
        build<Boolean>("mining_gem_rock") { default = false }
        build<Int>("mining_success_low")
        build<Int>("mining_success_high")
    }
}
