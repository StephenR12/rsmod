package org.rsmod.content.other.special.weapons.configs

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.content.other.special.weapons.magic.OtherStaves
import org.rsmod.content.other.special.weapons.magic.TridentObjs

internal object StaffChargeRows : org.rsmod.api.type.refs.dbrow.DbRowReferences() {
    val seas = find("charges_trident_of_the_seas")
    val enhancedSeas = find("charges_trident_of_the_seas_e")
    val swamp = find("charges_trident_of_the_swamp")
    val enhancedSwamp = find("charges_trident_of_the_swamp_e")
    val warped = find("charges_warped_sceptre")
    val sang = find("charges_sanguinesti_staff")
    val shadow = find("charges_tumekens_shadow")
}

internal object SpecialWeaponObjEditor : ObjEditor() {
    init {
        for (staff in OtherStaves.all) {
            edit(staff.empty) {
                param[params.charged_variant] = staff.active
                param[params.bank_autocharge] =
                    if (staff == OtherStaves.warped) StaffChargeRows.warped
                    else StaffChargeRows.sang
            }
            edit(staff.active) { param[params.uncharged_variant] = staff.empty }
        }
        for (trident in TridentObjs.variants) {
            edit(trident.empty) {
                param[params.charged_variant] = trident.charged
                param[params.bank_autocharge] =
                    when (trident) {
                        TridentObjs.swamp -> StaffChargeRows.swamp
                        TridentObjs.enhancedSwamp -> StaffChargeRows.enhancedSwamp
                        TridentObjs.enhanced -> StaffChargeRows.enhancedSeas
                        else -> StaffChargeRows.seas
                    }
            }
            edit(trident.charged) { param[params.uncharged_variant] = trident.empty }
        }
        edit(objs.tumekens_shadow) {
            param[params.uncharged_variant] = objs.tumekens_shadow_uncharged
        }
        edit(objs.tumekens_shadow_uncharged) {
            param[params.bank_autocharge] = StaffChargeRows.shadow
            param[params.charged_variant] = objs.tumekens_shadow
        }
    }
}
