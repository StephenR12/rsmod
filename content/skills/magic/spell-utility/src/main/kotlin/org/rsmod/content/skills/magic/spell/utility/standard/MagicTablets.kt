package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.player.isTeleportBlocked
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.categories
import org.rsmod.api.player.righthand
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TabletLecterns : LocReferences() {
    val house = (1..8).map { find("poh_lectern_$it") }
    val regional = mapOf(TabletLectern.Arceuus to find("arceuus_lectern"),TabletLectern.Ancient to find("dt_ancient_lectern"),TabletLectern.Lunar to find("lunar_moonclan_lectern"))
}
internal object TabletSequences: SeqReferences() {
    val create = find("poh_create_magic_tablet")
    val createWithStaff = find("poh_create_magic_tablet_withstaff")
}
internal object TabletWarnings:VarBitReferences() {
    val wilderness = mapOf(
        objs.spell_cemetery_teleport to find("teletab_cemetery_warning"),
        objs.spell_lunarghorrock_teleport to find("teletab_ice_plateau_warning"),
        objs.spell_annakarl_teleport to find("teletab_annakarl_warning"),
        objs.spell_carrallagar_teleport to find("teletab_carrallangar_warning"),
        objs.spell_dareeyak_teleport to find("teletab_dareeyak_warning"),
        objs.spell_ghorrock_teleport to find("teletab_ghorrock_warning")
    )
}

public class MagicTablets @Inject constructor(
    private val types:ObjTypeList,
    private val spells:MagicSpellRegistry,
    private val runes:MagicRuneManager,
    private val teleports:StandardTeleportSpells,
    private val enchantment:JewelleryEnchantSpell,
    private val jewellery:JewelleryEnchantRecipeRegistry,
    private val bones:BoneConversionSpells
):PluginScript() {
    override fun ScriptContext.startup() {
        for ((index,loc) in TabletLecterns.house.withIndex()) {
            onOpLoc1(loc) { study(TabletLectern.House, index+1) }
        }
        for ((kind,loc) in TabletLecterns.regional) onOpLoc1(loc) { study(kind) }

        for (recipe in MagicTabletRecipes.all) {
            val spell = types[recipe.tablet].param(params.spell_parent_obj)
            val targets = jewellery.recipes.filter { it.spell.id == spell.id }
            when {
                targets.isNotEmpty() -> {
                    onOpHeld4(recipe.tablet) { mes("Use this tablet on matching unenchanted jewellery.") }
                    for (target in targets.filter { it.source.id != objs.silvthrill_rod.id && it.source.id != objs.ruby_sickle_b.id }) {
                        onOpHeldU(recipe.tablet,target.source) { enchantment.useTablet(this,target,it.secondSlot,recipe.tablet,it.firstSlot) }
                    }
                }
                spell.id == objs.spell_bones_to_bananas.id || spell.id == objs.spell_bones_to_peaches.id ->
                    onOpHeld1(recipe.tablet) { if (player.members) bones.breakTablet(this,spell,recipe.tablet,it.slot) }
                spell.id != objs.spell_poh_teleport.id -> registerTeleport(recipe,spell)
            }
        }
    }

    private suspend fun ProtectedAccess.study(kind:TabletLectern, fixture:Int = 0) {
        if (!player.members) return
        val book = kind.spellbook
        if (book != null && vars[varbits.spellbook] != book) {
            mes("You need to switch to the ${kind.name} spellbook to use this lectern.")
            return
        }
        val recipes = MagicTabletRecipes.all.filter { it.lectern == kind && (kind != TabletLectern.House || fixture in it.fixtures) }
        val index = menu("Which tablet would you like to make?", false, recipes.map { types[it.tablet].name } + "Cancel")
        val recipe = recipes.getOrNull(index) ?: return
        val material = if (kind == TabletLectern.Arceuus) MagicTabletRecipes.darkEssence else objs.soft_clay
        val available = invTotal(inv,material)
        if (available == 0) {
            mes("You need ${types[material].name.lowercase()} to make this tablet.")
            return
        }
        val spell = types[recipe.tablet].param(params.spell_parent_obj)
        val requirements = requireNotNull(spells.getObjSpell(spell)).copy(objReqs=recipe.runes)
        if (!canMake(recipe,spell) || !runes.hasRunes(player,requirements)) return
        val count = if (available == 1) 1 else countDialog("How many tablets would you like to make?").coerceIn(0,available)
        for (i in 0 until count) {
            if (!canMake(recipe,spell) || material !in inv || !runes.hasRunes(player,requirements)) return
            val staff = player.righthand?.let { types.getValue(it.id).isCategoryType(categories.staff) } == true
            anim(if (staff) TabletSequences.createWithStaff else TabletSequences.create)
            delay(if (i == 0) 2 else 4)
            if (!canMake(recipe,spell) || !invReplace(inv,material,1,recipe.tablet,autoCommit=false).success) return
            if (runes.delReqs(player,requirements).isFailure()) return
            check(invReplace(inv,material,1,recipe.tablet).success)
            statAdvance(stats.magic,recipe.xp)
            if (spell.id == objs.spell_arceuusapeatoll_teleport.id) player.completeDiaryTask(DiaryTask.MakeApeAtollTablet)
        }
    }

    private fun ProtectedAccess.canMake(recipe:TabletRecipe,spell:ObjType):Boolean {
        if (stat(stats.magic) < recipe.level) {
            mes("You need level ${recipe.level} Magic to make this tablet.")
            return false
        }
        if (recipe.lectern.spellbook?.let { vars[varbits.spellbook] != it } == true) return false
        val gate = when (spell.id) {
            objs.spell_bones_to_peaches.id -> vars[varbits.magictraining_bonespeaches] > 0
            objs.spell_ardougne_teleport.id -> vars[varps.elenaquest] >= 29
            objs.spell_watchtower_teleport.id -> vars[varps.itwatchtower] >= 13
            objs.spell_civitas_fortis_teleport.id -> vars[varbits.vmq2] >= 50
            else -> true
        }
        if (!gate) mes("You haven't unlocked the ability to make that tablet.")
        return gate
    }

    private fun ScriptContext.registerTeleport(recipe:TabletRecipe,spell:ObjType) {
        val tablet = recipe.tablet
        val warning = TabletWarnings.wilderness.entries.firstOrNull { it.key.id == spell.id }?.value
        onOpHeld1(tablet) { useTeleport(recipe,spell,it.slot,teleports.defaultDestination(this,spell),warning) }
        val alternate = teleports.alternate(spell)
        if (alternate != null) {
            onOpHeld2(tablet) { useTeleport(recipe,spell,it.slot,types[spell].param(params.spell_telecoord),null) }
            onOpHeld3(tablet) {
                if (vars[alternate.unlock] != 0) useTeleport(recipe,spell,it.slot,alternate.destination,null)
                else mes("Complete the required diary to use that destination.")
            }
            onOpHeld4(tablet) {
                if (vars[alternate.unlock] != 0) {
                    vars[alternate.preference] = 1 - vars[alternate.preference]
                    mes("Future tablets and spells will take you to ${if(vars[alternate.preference] == 1) alternate.name else alternate.normalName}.")
                }
            }
        } else if (warning != null) {
            onOpHeld4(tablet) {
                val disabled = choice2("Warn me before teleporting.",false,"Disable this warning.",true)
                vars[warning] = if (disabled) 1 else 0
            }
        }
    }

    private suspend fun ProtectedAccess.useTeleport(recipe:TabletRecipe,spell:ObjType,slot:Int,destination:CoordGrid,warning:VarBitType?) {
        if (!eligibleToUse(recipe,spell)) {
            mes("You haven't unlocked this destination.")
            return
        }
        if (warning != null && vars[warning] == 0 && !choice2("Teleport into the Wilderness.",true,"Cancel.",false)) return
        breakTeleportTablet(recipe.tablet,slot,destination)
    }

    private fun ProtectedAccess.eligibleToUse(recipe:TabletRecipe,spell:ObjType):Boolean {
        if (recipe.lectern == TabletLectern.Ancient) return vars[varbits.desert_treasure_main] >= 15
        return when (spell.id) {
            objs.spell_ardougne_teleport.id -> vars[varps.elenaquest] >= 29
            objs.spell_watchtower_teleport.id -> vars[varps.itwatchtower] >= 13
            objs.spell_civitas_fortis_teleport.id -> vars[varbits.vmq2] >= 50
            objs.spell_kourendcastle_teleport.id -> vars[varbits.veos_progress] >= 7
            objs.spell_salvegrave_teleport.id, objs.spell_fenkenstrainscastle_teleport.id, objs.spell_barrows_teleport.id -> vars[varps.priest_in_peril] >= 60
            objs.spell_westardougne_teleport.id -> vars[varps.biohazard] >= 16
            objs.spell_harmonyisland_teleport.id -> vars[varps.great_brain_robbery] >= 130
            objs.spell_arceuusapeatoll_teleport.id -> vars[varps.monkey_madness] >= 9
            objs.spell_moonclan_teleport.id -> vars[varbits.lunar_quest_main] >= 190
            objs.spell_zmi_teleport.id -> vars[varbits.lunar_quest_main] >= 190 && vars[varbits.rc_zmi_lunar_spell] != 0
            else -> true
        }
    }
}

public suspend fun ProtectedAccess.breakTeleportTablet(tablet:ObjType,slot:Int,destination:CoordGrid):Boolean {
    val wildernessBase = if (coords.z >= 9920) 9920 else 3520
    if (!player.members || player.isTeleportBlocked() ||
        (vars[varbits.inside_wilderness] != 0 && (coords.z-wildernessBase)/8+1 > 20)) {
        mes("A magical force prevents the teleport.")
        return false
    }
    stopAction()
    anim(seqs.poh_absorb_tablet_teleport)
    spotanim(spotanims.poh_absorb_tablet_magic)
    soundSynth(synths.teleport_all)
    delay(3)
    if (!invDel(inv,tablet,1,slot).success) return false
    spellTeleport(destination)
    return true
}
