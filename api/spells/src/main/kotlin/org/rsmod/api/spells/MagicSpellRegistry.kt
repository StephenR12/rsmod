package org.rsmod.api.spells

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.aliases.ParamInt
import org.rsmod.api.config.aliases.ParamObj
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.spells.configs.spell_enums
import org.rsmod.game.enums.EnumTypeMapResolver
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.isAnyType
import org.rsmod.game.type.obj.isType

public class MagicSpellRegistry
@Inject
constructor(private val objTypes: ObjTypeList, private val enumResolver: EnumTypeMapResolver) {
    private lateinit var objSpells: Map<Int, MagicSpell>
    private lateinit var autocastSpells: Map<Int, MagicSpell>

    public fun getObjSpell(obj: ObjType): MagicSpell? = objSpells[obj.id]

    public fun getAutocastSpell(autocastId: Int): MagicSpell? = autocastSpells[autocastId]

    /**
     * Spells that should be routed into the offensive player/NPC combat pipelines.
     *
     * Cache 233 classifies several Lunar support and sharing spells as `Combat`, but none attacks
     * or damages a target. Their dedicated scripts own support-spell routing.
     */
    public fun offensiveCombatSpells(): List<MagicSpell> =
        objSpells.values.filter {
            it.type == MagicSpellType.Combat &&
                !it.obj.isAnyType(
                    objs.spell_stat_spy,
                    objs.spell_monster_examine,
                    objs.spell_cure_other,
                    objs.spell_cure_me,
                    objs.spell_cure_group,
                    objs.spell_dream,
                    objs.spell_res_pot_share,
                    objs.spell_statboost_pot_share,
                    objs.spell_energy_transfer,
                    objs.spell_heal_other,
                    objs.spell_vengeance_other,
                    objs.spell_vengeance,
                    objs.spell_heal_group,
                    objs.spell_spellbook_swap,
                    objs.spell_mark_of_darkness,
                    objs.spell_ward_of_arceuus,
                    objs.spell_lesser_corruption,
                    objs.spell_greater_corruption,
                    objs.spell_dark_lure,
                    objs.spell_resurrect_lesser_ghost,
                    objs.spell_resurrect_lesser_skeleton,
                    objs.spell_resurrect_lesser_zombie,
                    objs.spell_resurrect_superior_ghost,
                    objs.spell_resurrect_superior_skeleton,
                    objs.spell_resurrect_superior_zombie,
                    objs.spell_resurrect_greater_ghost,
                    objs.spell_resurrect_greater_skeleton,
                    objs.spell_resurrect_greater_zombie,
                )
        }

    internal fun init() {
        check(!::objSpells.isInitialized) { "`init` already called for this repository." }

        val objSpells = loadObjSpells()
        this.objSpells = objSpells

        val autocastSpells = loadAutocastSpells(objSpells)
        this.autocastSpells = autocastSpells
    }

    private fun loadObjSpells(): Map<Int, MagicSpell> {
        val spells = hashMapOf<Int, MagicSpell>()

        val spellbookList = enumResolver[spell_enums.spellbooks].filterValuesNotNull()
        for (spellbookEnum in spellbookList.values) {
            val spellList = enumResolver[spellbookEnum].filterValuesNotNull()
            for (spellObj in spellList.values) {
                spells[spellObj.id] = spellObj.toMagicSpell()
            }
        }

        // Cache 233.4 exposes jewellery enchantments through the Standard spellbook's submenu
        // opener rather than listing each child spell in `normal_spellbook`.
        for (spellObj in jewelleryEnchantmentSpells) {
            spells[spellObj.id] = spellObj.toMagicSpell()
        }
        for (spellObj in boltEnchantmentSpells) {
            spells[spellObj.id] = spellObj.toMagicSpell()
        }

        return spells
    }

    private fun loadAutocastSpells(objSpells: Map<Int, MagicSpell>): Map<Int, MagicSpell> {
        val spells = hashMapOf<Int, MagicSpell>()

        val autocastSpells = enumResolver[spell_enums.autocast_spells].filterValuesNotNull()
        for ((autocastId, spellObj) in autocastSpells) {
            val spell = objSpells[spellObj.id]
            checkNotNull(spell) { "Unexpected null spell for obj: $spellObj" }
            spells[autocastId] = spell
        }

        return spells
    }

    private fun ObjType.toMagicSpell(): MagicSpell {
        val unpacked = objTypes[this]

        // Some spells can have a default (-1) spellbook, such as `teleport_to_target_spell`.
        val spellbookId = unpacked.param(params.spell_spellbook)
        val spellbook = Spellbook[spellbookId]

        val spellTypeId = unpacked.param(params.spell_type)
        val spellType =
            MagicSpellType[spellTypeId]
                ?: error("Invalid MagicSpellType: $spellTypeId (spell=$unpacked)")

        val name = unpacked.param(params.spell_name)
        val button = unpacked.param(params.spell_button)
        val maxHit = unpacked.param(params.spell_maxhit)
        val levelReq = unpacked.param(params.spell_levelreq)
        val experience = unpacked.paramOrNull(params.spell_castxp)

        checkNotNull(experience) { "Cast xp not defined for spell obj: '$internalName' ($id)" }

        val objReqs = buildList {
            fun addRequirement(objParam: ParamObj, countParam: ParamInt) {
                val paramObj = unpacked.paramOrNull(objParam) ?: return
                val obj = paramObj.toRequirementObj()
                val cacheWorn = objTypes[obj].wearpos1.takeIf { it != -1 }
                // The cache's Ape Atoll spell uses the normal banana obj as its fourth
                // requirement. That obj has a legacy wear position, but the spell consumes an
                // unnoted banana from inventory rather than requiring one to be equipped.
                val worn =
                    cacheWorn.takeUnless {
                        isType(objs.spell_apeatoll_teleport) && obj.isType(objs.banana)
                    }
                val count = unpacked.param(countParam)
                check(worn == null || count == 1) {
                    "Count for worn objs expected to be 1: spell=$this, obj=$obj, count=$count"
                }
                this += MagicSpell.ObjRequirement(obj, count, worn)
            }
            addRequirement(params.spell_runetype_1, params.spell_runecount_1)
            addRequirement(params.spell_runetype_2, params.spell_runecount_2)
            addRequirement(params.spell_runetype_3, params.spell_runecount_3)
            addRequirement(params.spell_runetype_4, params.spell_runecount_4)
        }

        // For emulation purposes: Magic Dart has a quirk where obj validation order differs.
        // Although the staff appears first in the requirement list, its count is validated last.
        // Runes are always checked before the staff. This behavior appears to be unique to Magic
        // Dart, which is why we manually shift the staff to the end of the requirement list.
        val shiftFirstRequirementToTail = isType(objs.spell_magic_dart) && objReqs.isNotEmpty()

        val sortedObjReqs =
            if (shiftFirstRequirementToTail) {
                objReqs.drop(1) + objReqs.first()
            } else {
                objReqs
            }

        return MagicSpell(
            obj = this,
            name = name,
            component = button,
            spellbook = spellbook,
            type = spellType,
            maxHit = maxHit,
            levelReq = levelReq,
            castXp = experience / 10.0,
            objReqs = sortedObjReqs,
        )
    }

    // Claws of Guthix spell lists a special, non-usable staff obj (likely for visual purposes).
    // Since we use these objs for server-side validation, we replace it with the usable staff obj.
    private fun ObjType.toRequirementObj(): ObjType =
        if (isType(objs.guthix_staff_rune)) {
            objs.guthix_staff
        } else {
            this
        }

    private companion object {
        val jewelleryEnchantmentSpells =
            listOf(
                objs.spell_enchant_lvl_1,
                objs.spell_enchant_lvl_2,
                objs.spell_enchant_lvl_3,
                objs.spell_enchant_lvl_4,
                objs.spell_enchant_lvl_5,
                objs.spell_enchant_lvl_6,
                objs.spell_enchant_lvl_7,
            )

        val boltEnchantmentSpells =
            listOf(
                objs.spell_enchant_bolt_opal,
                objs.spell_enchant_bolt_sapphire,
                objs.spell_enchant_bolt_jade,
                objs.spell_enchant_bolt_pearl,
                objs.spell_enchant_bolt_emerald,
                objs.spell_enchant_bolt_red_topaz,
                objs.spell_enchant_bolt_ruby,
                objs.spell_enchant_bolt_diamond,
                objs.spell_enchant_bolt_dragonstone,
                objs.spell_enchant_bolt_onyx,
            )
    }
}
