package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.content.skills.herblore.SurgePotionCooldownRepository
import org.rsmod.content.skills.magic.spell.utility.standard.PoisonProtectionRepository
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object PoolLocs : LocReferences() {
    val tiers = listOf("restoration","revitalisation","rejuvenation","recovery","regeneration").map { find("poh_pool_$it") }
}
internal object PoolSequences : SeqReferences() { val drink = find("poh_pool_drink") }

public class HousePools @Inject constructor(
    private val homes: StarterHouse,
    private val energy: SpecialAttackEnergy,
    private val toxins: PoisonProtectionRepository,
    private val surge: SurgePotionCooldownRepository,
    private val skills: StatTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((index, pool) in PoolLocs.tiers.withIndex()) onOpLoc1(pool) { drink(it.loc.coords,index+1) }
    }

    private suspend fun ProtectedAccess.drink(tile: CoordGrid,tier:Int) {
        val room=homes.publicRoom(player,tile)?:return
        if(room.type!=HouseRoomType.SuperiorGarden || room.variant(3)!=tier) return
        if(isInCombat()){mes("You can't drink from the pool during combat.");return}
        anim(PoolSequences.drink)
        delay(2)
        if(homes.publicRoom(player,tile)!=room || isInCombat()) return
        energy.restoreSpecialEnergy(player,SpecialAttackEnergy.MAX_ENERGY)
        if(tier>=2)player.runEnergy=10000
        if(tier>=3){
            if(stat(stats.prayer)<statBase(stats.prayer))statRestore(stats.prayer)
            vars[varbits.prayer_drain_counter]=0
        }
        if(tier>=4)for(skill in skills.values) {
            if(skill.id!=stats.hitpoints.id && stat(skill)<statBase(skill))statRestore(skill)
        }
        if(tier==5){
            if(stat(stats.hitpoints)<statBase(stats.hitpoints))statRestore(stats.hitpoints)
            if(vars[varps.poison]>0){vars[varps.poison]=0;toxins.pause(player)}
            if(vars[varps.disease]>0)vars[varps.disease]=0
            surge.clear(player)
        }
        mes("You drink from the pool.")
    }
}
