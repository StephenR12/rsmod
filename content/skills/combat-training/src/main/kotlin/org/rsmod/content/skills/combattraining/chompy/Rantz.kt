package org.rsmod.content.skills.combattraining.chompy

import jakarta.inject.Inject
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.hasStoredChompyHat
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private val killsRequired = listOf(30,40,50,70,95,125,170,225,300,400,550,700,1000,1300,1700,2250,3000,4000)
private val buttons = listOf(components.skillmulti_a,components.skillmulti_b,components.skillmulti_c,components.skillmulti_d,
    components.skillmulti_e,components.skillmulti_f,components.skillmulti_g,components.skillmulti_h,components.skillmulti_i)

public class Rantz @Inject constructor(private val npcs: NpcTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        // Rantz's native multi-NPC changes at completion of Big Chompy Bird Hunting (varp 293).
        for (rantz in listOf(npcs.getValue(14793),npcs.getValue(14794))) {
            onOpNpc1(rantz) {
                if (!player.canHuntChompies()) { mes("Rantz is preparing to hunt chompies."); return@onOpNpc1 }
                when(menu("Rantz", false, listOf("Which hats can I claim?", "Can I have my ogre arrows?", "I need an ogre bow.", "Leave"))) {
                    0 -> hats()
                    1 -> arrows()
                    2 -> buyBow()
                }
            }
            onOpNpc3(rantz) { arrows() }
            for(bow in listOf(objs.ogre_bow,objs.comp_ogre_bow)) onOpNpcU(rantz,bow) { hats() }
        }
        onOpNpc4(npcs.getValue(14794)) { hats() }
        for(bow in listOf(objs.ogre_bow,objs.comp_ogre_bow)) {
            onOpHeld3(bow) { notches() }
            onOpWorn2(bow) { notches() }
        }
    }
    private fun ProtectedAccess.notches() {
        mes("You count " + vars[HuntingVars.kills] + " chompy bird kills marked on your bow.")
    }
    private suspend fun ProtectedAccess.buyBow() {
        if (!player.canHuntChompies()) return
        val price = random.of(500..550)
        if (!choice2("Buy an ogre bow (" + price + " coins).",true,"Leave.",false)) return
        val result = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,objs.coins.id,price)
            add(bag,objs.ogre_bow.id,1)
        }
        mes(if(result.success) "Rantz gives you an ogre bow." else "You need the coins and room for a bow.")
    }
    private fun ProtectedAccess.arrows() {
        if (!player.canHuntChompies()) return
        val tier = player.westernTier() ?: run { mes("Complete the Western Provinces Diary to claim arrows.");return }
        val today = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()
        if (vars[HuntingVars.arrowDay] == today) { mes("You have already collected today's arrows.");return }
        val amount = listOf(25,50,100,150)[tier.ordinal]
        if (invAdd(inv,objs.ogre_arrow,amount).success) {
            vars[HuntingVars.arrowDay] = today
            vars[HuntingBits.arrowsClaimed] = 1
            mes("Rantz gives you " + amount + " ogre arrows.")
        } else mes("Make room for the arrows.")
    }
    private fun ProtectedAccess.ownsHat(index: Int): Boolean {
        val hat = HuntingObjects.hats[index]
        return hat in inv || hat in worn || hat in bank || player.hasStoredChompyHat(index)
    }
    private fun ProtectedAccess.hasBow() =
        objs.ogre_bow in inv || objs.ogre_bow in worn || objs.comp_ogre_bow in inv || objs.comp_ogre_bow in worn

    private suspend fun ProtectedAccess.hats() {
        if (!player.canHuntChompies()) return
        if (!hasBow()) { mes("Bring Rantz an ogre bow so he can count the notches.");return }
        val available = killsRequired.indices.filter { killsRequired[it] <= vars[HuntingVars.kills] && !ownsHat(it) }
        if (available.isEmpty()) { mes("Rantz has no new hats for you.");return }
        val pages = available.chunked(buttons.size)
        val page = if(pages.size == 1) pages.single() else {
            val choice = menu("Which hats would you like to see?",false,pages.map { killsRequired[it.first()].toString() + "–" + killsRequired[it.last()] + " kills" })
            pages.getOrNull(choice) ?: return
        }
        val input = skillMulti("Which hat would you like?",page.map { HuntingObjects.hats[it] },1,1,0)
        val index = page.getOrNull(buttons.indexOfFirst { it.isType(input.component) }) ?: return
        if (!hasBow() || ownsHat(index) || killsRequired[index] > vars[HuntingVars.kills]) return
        if (!invAdd(inv,HuntingObjects.hats[index]).success) { mes("Make room in your inventory for the hat.");return }
        HuntingBits.collected.getOrNull(index)?.let { vars[it] = 1 }
        val count = vars[HuntingVars.kills]
        player.completeDiaryTask(DiaryTask.ClaimChompyHat)
        if (count >= 125) player.completeDiaryTask(DiaryTask.ClaimMarksmanHat)
        if (count >= 300) player.completeDiaryTask(DiaryTask.ClaimForesterHat)
        if (count >= 1000) player.completeDiaryTask(DiaryTask.ClaimOgreExpertHat)
        mes("Rantz gives you a feathered hat.")
    }
}
