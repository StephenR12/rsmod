package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

public class PotionProductionModifiers
@Inject
constructor(
    private val random: GameRandom,
    private val chemistryAmulet: ChemistryAmuletRepository,
) {
    public fun roll(access: ProtectedAccess, recipe: FinishedPotionRecipe): PotionProductionRoll {
        return roll(
            access,
            recipe.output,
            FinishedPotionRecipes.extraDoseByOutput[recipe.output.id],
        )
    }

    public fun roll(
        access: ProtectedAccess,
        output: ObjType,
        extraDoseOutput: ObjType?,
    ): PotionProductionRoll {
        val extraDoseSource =
            if (extraDoseOutput == null) {
                ExtraDoseSource.None
            } else {
                extraDoseSource(access)
            }
        val preserveSecondary = rollSecondaryPreservation(access)
        return PotionProductionRoll(
            output = if (extraDoseSource == ExtraDoseSource.None) output else extraDoseOutput!!,
            preserveSecondary = preserveSecondary,
            extraDoseSource = extraDoseSource,
        )
    }

    public fun commit(access: ProtectedAccess, roll: PotionProductionRoll): PotionProductionCommit {
        return when (roll.extraDoseSource) {
            ExtraDoseSource.None -> PotionProductionCommit()
            ExtraDoseSource.ChemistryAmulet -> {
                val broke = consumeChemistryAmulet(access)
                PotionProductionCommit(
                    chemistryChargesRemaining =
                        if (broke) 0 else chemistryAmulet.remaining(access.player)
                )
            }
            ExtraDoseSource.AlchemistsAmulet -> {
                consumeAlchemistsAmulet(access)
                PotionProductionCommit()
            }
        }
    }

    public fun alchemistsAmuletCharges(access: ProtectedAccess): Int =
        access.alchemistsAmuletStoredCharges

    public fun setAlchemistsAmuletCharges(access: ProtectedAccess, charges: Int) {
        require(charges in 0..ALCHEMISTS_AMULET_MAX_CHARGES)
        access.alchemistsAmuletStoredCharges = charges
    }

    public fun rollSecondaryPreservation(access: ProtectedAccess): Boolean =
        hasPrescriptionGoggles(access) && random.of(ROLL_DENOMINATOR) < GOGGLES_SUCCESS_COUNT

    private fun extraDoseSource(access: ProtectedAccess): ExtraDoseSource {
        val worn = access.player.worn
        if (
            worn.contains(objs.alchemists_amulet_charged) &&
                access.alchemistsAmuletStoredCharges > 0 &&
                random.of(ROLL_DENOMINATOR) < ALCHEMISTS_AMULET_SUCCESS_COUNT
        ) {
            return ExtraDoseSource.AlchemistsAmulet
        }
        if (
            worn.contains(objs.amulet_of_chemistry) &&
                random.of(ROLL_DENOMINATOR) < CHEMISTRY_AMULET_SUCCESS_COUNT
        ) {
            return ExtraDoseSource.ChemistryAmulet
        }
        return ExtraDoseSource.None
    }

    private fun consumeChemistryAmulet(access: ProtectedAccess): Boolean {
        val slot = access.player.worn.indexOfFirst { it?.id == objs.amulet_of_chemistry.id }
        check(slot >= 0) { "Rolled chemistry amulet without an equipped amulet." }
        val broke = chemistryAmulet.consume(access.player)
        if (broke) {
            access.player.worn[slot] = null
        }
        return broke
    }

    private fun consumeAlchemistsAmulet(access: ProtectedAccess) {
        val slot = access.player.worn.indexOfFirst { it?.id == objs.alchemists_amulet_charged.id }
        check(slot >= 0) { "Rolled alchemist's amulet without an equipped amulet." }
        access.alchemistsAmuletStoredCharges--
        if (access.alchemistsAmuletStoredCharges == 0) {
            access.player.worn[slot] = InvObj(objs.alchemists_amulet_uncharged)
        }
    }

    private fun hasPrescriptionGoggles(access: ProtectedAccess): Boolean =
        access.player.worn.contains(objs.prescription_goggles) ||
            access.player.worn.contains(objs.prescription_goggles_focused)

    private var ProtectedAccess.alchemistsAmuletStoredCharges by
        intVarBit(varbits.alchemists_amulet_charges)

    private companion object {
        const val ROLL_DENOMINATOR = 100
        const val CHEMISTRY_AMULET_SUCCESS_COUNT = 5
        const val ALCHEMISTS_AMULET_SUCCESS_COUNT = 15
        const val GOGGLES_SUCCESS_COUNT = 10
        const val ALCHEMISTS_AMULET_MAX_CHARGES = 5_000
    }
}

public data class PotionProductionRoll(
    public val output: ObjType,
    public val preserveSecondary: Boolean,
    public val extraDoseSource: ExtraDoseSource,
)

public data class PotionProductionCommit(public val chemistryChargesRemaining: Int? = null)

public enum class ExtraDoseSource {
    None,
    ChemistryAmulet,
    AlchemistsAmulet,
}
