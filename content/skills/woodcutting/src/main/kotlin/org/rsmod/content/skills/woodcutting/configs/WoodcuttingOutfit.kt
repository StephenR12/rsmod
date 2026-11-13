package org.rsmod.content.skills.woodcutting.configs

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.stats.xpmod.StatXpMod
import org.rsmod.content.skills.woodcutting.forestry.ForestryGear
import org.rsmod.content.skills.woodcutting.forestry.GearVars
import org.rsmod.game.entity.Player

public class WoodcuttingOutfit @Inject constructor(private val gear: ForestryGear,private val items:org.rsmod.game.type.obj.ObjTypeList) : StatXpMod(stats.woodcutting) {
    private val incompatible=listOf("angler","spirit angler","ahrim","dharok","guthan","karil","torag","verac","bloodbark",
        "carpenter","crystal helm","crystal body","crystal legs","farmer","graceful","guild hunter","inquisitor","obsidian",
        "prospector","golden prospector","pyromancer","hat of the eye","robe top of the eye","robe bottoms of the eye","boots of the eye",
        "rogue","shayzien","smiths","swampbark","virtus","void","elite void","zealot")
    override fun Player.modifier():Double {
        if(!members)return 0.0
        val pouch=if(vars[GearVars.pouches] and 1!=0 && gear.hasKit(this))gear.kit(this) else null
        val slots=listOf(
            listOf(objs.lumberjack_hat,objs.forestry_hat) to 4,
            listOf(objs.lumberjack_top,objs.forestry_top) to 8,
            listOf(objs.lumberjack_legs,objs.forestry_legs) to 6,
            listOf(objs.lumberjack_boots,objs.forestry_boots) to 2)
        val wornSlots=listOf(0,4,7,10)
        val active=slots.filterIndexed{index,(forms,_)->
            val item=worn[wornSlots[index]]?.let{items[it].name.lowercase()}.orEmpty()
            forms.any{it in worn} || forms.any{pouch?.contains(it)==true} && incompatible.none(item::startsWith)
        }
        return (active.sumOf{it.second} + if(active.size==4)5 else 0)/1000.0
    }
}
