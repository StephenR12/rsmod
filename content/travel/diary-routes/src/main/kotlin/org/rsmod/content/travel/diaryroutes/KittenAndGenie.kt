package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.firemaking.LightSourceRecipes
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object KittenVars:VarpReferences() { val quest=find("fluffs"); val follower=find("follower_npc") }
internal object KittenNpcs:NpcReferences() { val gertrude=find("gertrude_post"); val genie=find("elid_genie") }
internal object KittenItems:ObjReferences() {
    val ring=find("ring_of_charos_unlocked")
    private val colours=listOf("","_light","_brown","_black","_browngrey","_bluegrey","_hell")
    val kittens=colours.map { find("kittenobject$it") }
    val household=kittens + colours.map { find("growncatobject$it") }
}
internal object GenieEntrance:LocReferences() {
    val crevice=find("elid_crevice_clickzone")
    val rope=find("elid_climbing_rope")
    val door=find("elid_genie_door")
}
internal object KittenBits:VarBitReferences() { val occupied=find("follower_space_track") }
internal object GenieQuest:VarBitReferences() { val progress=find("elidquest") }

public class KittenAndGenie @Inject constructor(private val objects:ObjTypeList, private val npcs: org.rsmod.game.entity.NpcList):PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(KittenNpcs.gertrude) { if (choice2("Do you have any more kittens?",true,"Goodbye.",false)) kitten() }
        onOpNpc3(KittenNpcs.gertrude) { kitten() }
        onOpLoc1(GenieEntrance.crevice) { descend() }
        onOpLocU(GenieEntrance.crevice,objs.rope) { descend() }
        onOpLoc1(GenieEntrance.rope) { telejump(CoordGrid(3372,2905)) }
        onOpLoc1(GenieEntrance.door) {
            if (coords.z > 9312) telejump(CoordGrid(3371,9312))
            else if(player.members && player.vars[GenieQuest.progress] >= 40) telejump(CoordGrid(3371,9313))
            else mes("A magical force holds the door shut.")
        }
        onOpNpc1(KittenNpcs.genie) {
            if (player.members && player.vars[GenieQuest.progress] >= 40 && coords.x in 3366..3377 && coords.z in 9313..9325) {
                player.completeDiaryTask(DiaryTask.VisitElidGenie)
                startDialogue(it.npc) { chatNpc(neutral,"Who dares disturb the genie of Nardah?") }
            }
        }
    }

    private suspend fun ProtectedAccess.kitten() {
        if (!player.members || player.vars[KittenVars.quest] < 6) return
        fun ownsCat(): Boolean {
            val follower = npcs[player.vars[KittenVars.follower]]?.id
            val feline = follower != null && (follower in 5591..5597 || follower in 1619..1625)
            return (player.vars[KittenBits.occupied] != 0 && feline) ||
                player.invMap.values.any { bag -> KittenItems.household.any { it in bag } }
        }
        if (ownsCat()) { mes("Gertrude asks you to look after the cat you already have."); return }
        if (inv.count(objects[objs.coins]) < 100) { mes("Gertrude charges 100 coins for a kitten."); return }
        val selectColour = KittenItems.ring in worn &&
            choice2("Charm Gertrude and choose a colour.",true,"Choose a kitten for me.",false)
        val index = if(selectColour) menu("Choose a kitten",true,listOf("Grey and black","White","Brown","Black","Grey and brown","Grey and blue")) else random.of(6)
        val item=KittenItems.kittens.take(6).getOrNull(index) ?: return
        if(ownsCat() || (selectColour && KittenItems.ring !in worn)) return
        val result=player.invTransaction(inv) {
            val bag=select(inv)
            delete(bag,objs.coins.id,100)
            add(bag,item.id,1)
        }
        if(!result.success) { mes("You need 100 coins and space for a kitten."); return }
        mes("Gertrude gives you a kitten.")
        if(selectColour) player.completeDiaryTask(DiaryTask.SelectKittenColour)
    }

    private fun ProtectedAccess.descend() {
        if(!player.members) return
        if(objs.rope !in inv) { mes("You need a rope to climb down the crevice."); return }
        if(!LightSourceRecipes.carriesLight(player)) { mes("It is too dark down there without a light source."); return }
        if(invDel(inv,objs.rope,1).success) telejump(CoordGrid(3372,9305))
    }
}
