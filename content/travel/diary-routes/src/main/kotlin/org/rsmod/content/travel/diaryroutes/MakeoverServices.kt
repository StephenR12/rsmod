package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MakeoverNpcs : NpcReferences() {
    val barber = find("hairdresser")
    val yrsa = find("viking_clothing_shopkeeper")
}
internal object MakeoverUi : InterfaceReferences() { val main = find("makeover") }
internal object MakeoverButtons : ComponentReferences() {
    val styles = find("makeover:item_area")
    val colours = find("makeover:colours")
    val apply = find("makeover:select")
    val confirm = find("makeover:select_and_close")
    val pages = find("makeover:burger_menu_frame")
}
internal object MakeoverBits : VarBitReferences() {
    val page = find("makeover_interface_type")
    val body = find("makeover_bodytype")
}
internal object MakeoverVars : VarpReferences() {
    val trials = find("viking")
    val style = find("makeover_hair")
    val colour = find("makeover_colour")
    val kits = (0..6).map { find("salon_kit_$it") }
    val colours = (0..3).map { find("salon_colour_$it") }
}
internal object MakeoverVarBuilds : VarpBuilder() {
    init {
        for (kit in 0..6) build("salon_kit_$kit") { transmitNever = true }
        for (colour in 0..3) build("salon_colour_$colour") { transmitNever = true }
    }
}

/** Layouts follow the enum and DB-row selections in makeover_init and makeover_draw_items. */
private enum class MakeoverPage(val clientId:Int,val kit:Int,val colour:Int?,val styles:Int,val palette:Int?) {
    Hair(0,0,0,496,890), Beard(1,1,0,2630,890),
    Torso(3,2,1,5495,789), Sleeves(4,3,1,5496,789),
    Legs(5,5,2,5497,791), Shoes(6,6,3,5498,5290), Hands(7,4,null,5499,null)
}

public class MakeoverServices @Inject constructor(private val enums:EnumTypeList,private val rows:DbRowTypeList):PluginScript() {
    private data class Selection(val barber:Boolean,val page:MakeoverPage,var style:Int,var colour:Int)
    private val sessions = mutableMapOf<Player,Selection>()

    override fun ScriptContext.startup() {
        onOpNpc1(MakeoverNpcs.barber) { open(true,MakeoverPage.Hair) }
        onOpNpc4(MakeoverNpcs.barber) { open(true,MakeoverPage.Hair) }
        onOpNpc5(MakeoverNpcs.barber) { open(true,MakeoverPage.Beard) }
        onOpNpc1(MakeoverNpcs.yrsa) { open(false,MakeoverPage.Shoes) }
        onOpNpc4(MakeoverNpcs.yrsa) { open(false,MakeoverPage.Shoes) }
        onIfModalButton(MakeoverButtons.styles) {
            val selection = sessions[player] ?: return@onIfModalButton
            if (it.comsub >= 0 && it.comsub % STYLE_CHILDREN == 0) {
                val index = it.comsub / STYLE_CHILDREN
                if (kit(selection.page,index,player.appearance.bodyType) != null) {
                    selection.style = index
                    vars[MakeoverVars.style] = it.comsub
                }
            }
        }
        onIfModalButton(MakeoverButtons.colours) {
            val selection = sessions[player] ?: return@onIfModalButton
            val palette = selection.page.palette ?: return@onIfModalButton
            if (it.comsub >= 0 && it.comsub % 2 == 0 && it.comsub/2 in enums.getValue(palette).primitiveMap) {
                selection.colour = it.comsub/2
                vars[MakeoverVars.colour] = selection.colour
            }
        }
        onIfModalButton(MakeoverButtons.pages) {
            val selection = sessions[player] ?: return@onIfModalButton
            val requested = when (it.comsub) { 11 -> 0; 13 -> 1; 17 -> 3; 19 -> 4; 21 -> 5; 23 -> 6; 25 -> 7; else -> return@onIfModalButton }
            val page = MakeoverPage.entries.first { it.clientId == requested }
            open(selection.barber,page)
        }
        onIfModalButton(MakeoverButtons.apply) { if (it.comsub == 9) applySelection() }
        onIfModalButton(MakeoverButtons.confirm) {
            if (it.comsub == 9 && sessions.containsKey(player)) {
                applySelection()
                ifCloseSub(MakeoverUi.main)
            }
        }
        onIfClose(MakeoverUi.main) { sessions.remove(player) }
        onPlayerLogout { sessions.remove(player) }
        onPlayerLogin {
            for ((index,key) in MakeoverVars.kits.withIndex()) if (player.vars[key] > 0) player.appearance.setIdentKit(index,player.vars[key]-1)
            for ((index,key) in MakeoverVars.colours.withIndex()) if (player.vars[key] > 0) player.appearance.setColour(index,player.vars[key]-1)
        }
    }

    private fun kit(page:MakeoverPage,index:Int,body:Int):Int? {
        val row = enums.getValue(page.styles).primitiveMap[index] as? Int ?: return null
        return rows.getValue(row).data[1+body]?.firstOrNull() as? Int
    }

    private fun ProtectedAccess.open(barber:Boolean,page:MakeoverPage) {
        if (!barber && (!player.members || vars[MakeoverVars.trials] < 10)) {
            mes("Yrsa only offers makeovers to members of the Fremennik.")
            return
        }
        val appearance = player.appearance
        val count = enums.getValue(page.styles).primitiveMap.size
        val current = appearance.identKitSnapshot()[page.kit].toInt()
        val style = (0 until count).firstOrNull { kit(page,it,appearance.bodyType) == current } ?: 0
        val colour = page.colour?.let { appearance.coloursSnapshot()[it].toInt() } ?: 0
        vars[MakeoverBits.page] = page.clientId
        vars[MakeoverBits.body] = appearance.bodyType
        vars[MakeoverVars.style] = style*STYLE_CHILDREN
        vars[MakeoverVars.colour] = colour
        ifOpenMainModal(MakeoverUi.main)
        sessions[player] = Selection(barber,page,style,colour)
        for (i in 0 until count) ifSetEvents(MakeoverButtons.styles,i*STYLE_CHILDREN..i*STYLE_CHILDREN,IfEvent.Op1)
        page.palette?.let { palette ->
            for (i in 0 until enums.getValue(palette).primitiveMap.size) ifSetEvents(MakeoverButtons.colours,i*2..i*2,IfEvent.Op1)
        }
        ifSetEvents(MakeoverButtons.apply,9..9,IfEvent.Op1)
        ifSetEvents(MakeoverButtons.confirm,9..9,IfEvent.Op1)
        ifSetEvents(MakeoverButtons.pages,11..25,IfEvent.Op1)
    }

    private fun ProtectedAccess.applySelection() {
        val selection = sessions[player] ?: return
        val page = selection.page
        val appearance = player.appearance
        val chosen = kit(page,selection.style,appearance.bodyType) ?: return
        val styleChanged = appearance.identKitSnapshot()[page.kit].toInt() != chosen
        val colourChanged = page.colour?.let { appearance.coloursSnapshot()[it].toInt() != selection.colour } == true
        if (!styleChanged && !colourChanged) return
        appearance.setIdentKit(page.kit,chosen)
        vars[MakeoverVars.kits[page.kit]] = chosen+1
        page.colour?.let {
            appearance.setColour(it,selection.colour)
            vars[MakeoverVars.colours[it]] = selection.colour+1
        }
        if (selection.barber && page == MakeoverPage.Hair) player.completeDiaryTask(DiaryTask.GetFaladorHaircut)
        if (!selection.barber && page == MakeoverPage.Shoes) player.completeDiaryTask(DiaryTask.ChangeRellekkaBoots)
    }

    private companion object {
        // Each style has a button, a border, two models and a caption (clientscript 7073).
        const val STYLE_CHILDREN = 5
    }
}
