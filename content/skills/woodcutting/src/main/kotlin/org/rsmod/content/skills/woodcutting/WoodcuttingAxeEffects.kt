package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.content.skills.firemaking.FiremakingRecipes
import org.rsmod.content.skills.firemaking.FiremakingXp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

public class WoodcuttingAxeEffects @Inject constructor(private val firemakingXp: FiremakingXp) {
    public fun apply(access: ProtectedAccess, axe: InvObj, product: ObjType): Result {
        val infernalDepleted =
            infernalDepletedTypes.entries.firstOrNull { it.key.id == axe.id }?.value
        if (infernalDepleted != null) {
            val recipe = FiremakingRecipes.byLog[product.id]
            if (recipe != null && access.random.randomBoolean(INFERNAL_BURN_CHANCE)) {
                check(consumeInfernalCharge(access, axe, infernalDepleted)) {
                    "Selected infernal axe was absent while applying its log effect."
                }
                firemakingXp.advance(access, recipe.xp / 2.0)
                return Result(logConsumed = true)
            }
            return Result(logConsumed = false)
        }

        if (axe.id == objs.crystal_axe.id) {
            check(consumeCrystalCharge(access, axe)) {
                "Selected crystal axe was absent while applying its log effect."
            }
        }
        return Result(logConsumed = false)
    }

    private fun consumeInfernalCharge(
        access: ProtectedAccess,
        axe: InvObj,
        depletedType: ObjType,
    ): Boolean {
        if (access.isChargeSaved(allowCrystalSignet = false)) {
            return true
        }
        val updated = InfernalAxeState.fromCharged(axe).consume(axe, depletedType)
        return access.replaceSelectedAxe(axe, updated)
    }

    private fun consumeCrystalCharge(access: ProtectedAccess, axe: InvObj): Boolean {
        if (access.isChargeSaved(allowCrystalSignet = true)) {
            return true
        }
        val updated = CrystalAxeState.fromCharged(axe).consume(axe, objs.crystal_axe_inactive)
        return access.replaceSelectedAxe(axe, updated)
    }

    private fun ProtectedAccess.replaceSelectedAxe(expected: InvObj, replacement: InvObj): Boolean {
        if (player.righthand == expected) {
            player.righthand = replacement
            return true
        }
        val slot = player.inv.indexOfFirst { it == expected }
        if (slot == -1) {
            return false
        }
        player.inv[slot] = replacement
        return true
    }

    private fun ProtectedAccess.isChargeSaved(allowCrystalSignet: Boolean): Boolean {
        val hasCrystalSignet =
            allowCrystalSignet && CRYSTAL_CHARGE_SAVING_SIGNETS.any(player.worn::contains)
        val hasLuckyPenny = player.worn.contains(objs.ghommals_lucky_penny)
        val signetSaved = hasCrystalSignet && random.randomBoolean(SIGNET_SAVE_CHANCE)
        val pennySaved = hasLuckyPenny && random.randomBoolean(LUCKY_PENNY_SAVE_CHANCE)
        return signetSaved || pennySaved
    }

    public data class Result(val logConsumed: Boolean)

    public companion object {
        public const val INFERNAL_BURN_CHANCE: Int = 3
        public const val SIGNET_SAVE_CHANCE: Int = 10
        public const val LUCKY_PENNY_SAVE_CHANCE: Int = 20

        public val infernalDepletedTypes: Map<ObjType, ObjType> =
            mapOf(
                objs.infernal_axe to objs.infernal_axe_uncharged,
                objs.infernal_axe_or to objs.infernal_axe_or_uncharged,
                objs.infernal_axe_or_reloaded to objs.infernal_axe_or_reloaded_uncharged,
            )

        public val CRYSTAL_CHARGE_SAVING_SIGNETS: Set<ObjType> =
            setOf(objs.elven_signet, objs.celestial_signet, objs.celestial_signet_charged)
    }
}
