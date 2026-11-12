package org.rsmod.content.bosses.forgottenfour

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDrop
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropRoll
import org.rsmod.api.death.NpcDropRollResult
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ForgottenFourDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val controller: ForgottenFourController,
) : PluginScript() {
    override fun ScriptContext.startup() {
        register(
            ForgottenFour.definitions.single { it.family == ForgottenBossFamily.Leviathan },
            ForgottenFourObjs.venatorVestige,
            ForgottenFourObjs.leviathansLure,
            ForgottenFourObjs.smokeQuartz,
            ForgottenFourObjs.lilviathan,
            ForgottenFourObjs.leviathanTablet,
            LEVIATHAN_PET_RATE,
            41,
        )
        register(
            ForgottenFour.definitions.single { it.family == ForgottenBossFamily.Whisperer },
            ForgottenFourObjs.bellatorVestige,
            ForgottenFourObjs.sirensStaff,
            ForgottenFourObjs.shadowQuartz,
            ForgottenFourObjs.wisp,
            ForgottenFourObjs.whispererTablet,
            WHISPERER_PET_RATE,
            34,
        )
        register(
            ForgottenFour.definitions.single { it.family == ForgottenBossFamily.Vardorvis },
            ForgottenFourObjs.ultorVestige,
            ForgottenFourObjs.executionersAxeHead,
            ForgottenFourObjs.bloodQuartz,
            ForgottenFourObjs.butch,
            ForgottenFourObjs.vardorvisTablet,
            VARDORVIS_PET_RATE,
            48,
        )
        register(
            ForgottenFour.definitions.single { it.family == ForgottenBossFamily.Duke },
            ForgottenFourObjs.magusVestige,
            ForgottenFourObjs.eyeOfTheDuke,
            ForgottenFourObjs.iceQuartz,
            ForgottenFourObjs.baron,
            ForgottenFourObjs.dukeTablet,
            DUKE_PET_RATE,
            36,
        )
    }

    private fun register(
        definition: ForgottenBossDefinition,
        vestige: ObjType,
        axePiece: ObjType,
        quartz: ObjType,
        pet: ObjType,
        tablet: ObjType,
        petRate: Int,
        orbRate: Int,
    ) {
        val uniqueTable =
            weighted(
                NpcWeightedDrop(1, virtusPiece()),
                NpcWeightedDrop(3, vestigeProgress(definition.family, vestige)),
                NpcWeightedDrop(3, item(ForgottenFourObjs.chromiumIngot)),
                NpcWeightedDrop(1, item(axePiece)),
            )
        registry.register(
            definition.npc,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    commonTable(),
                    independent(
                        denominator = definition.uniqueTableDenominator,
                        roll = uniqueTable,
                    ),
                    independent(denominator = QUARTZ_RATE, roll = item(quartz)),
                    independent(denominator = TABLET_RATE, roll = item(tablet)),
                    independent(denominator = orbRate, roll = item(ForgottenFourObjs.awakenersOrb)),
                    independent(denominator = petRate, roll = item(pet)),
                )
            ),
        )
    }

    private fun commonTable(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(18, scaledItem(objs.coins, 80_000..180_000)),
            NpcWeightedDrop(12, scaledItem(objs.runite_ore, 20..30)),
            NpcWeightedDrop(12, scaledItem(objs.adamantite_ore, 40..60)),
            NpcWeightedDrop(12, scaledItem(objs.coal, 160..240)),
            NpcWeightedDrop(10, scaledItem(objs.soul_rune, 400..600)),
            NpcWeightedDrop(10, scaledItem(objs.death_rune, 400..600)),
            NpcWeightedDrop(10, scaledItem(objs.dragon_javelin_tips, 40..60)),
            NpcWeightedDrop(8, scaledItem(objs.onyx_bolt_tips, 60..90)),
            NpcWeightedDrop(8, scaledItem(objs.manta_ray, 20..30)),
        )

    private fun scaledItem(type: ObjType, count: IntRange): NpcDropRoll = NpcDropRoll { context ->
        val base = context.random.of(count)
        val amount = if (controller.state(context.npc).perfect) base * 3 / 2 else base
        NpcDropRollResult(
            selected = true,
            drops = listOf(NpcDrop(InvObj(type, amount), context.coords, context.hero)),
        )
    }

    private fun virtusPiece(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(1, item(ForgottenFourObjs.virtusMask)),
            NpcWeightedDrop(1, item(ForgottenFourObjs.virtusTop)),
            NpcWeightedDrop(1, item(ForgottenFourObjs.virtusLegs)),
        )

    private fun vestigeProgress(family: ForgottenBossFamily, vestige: ObjType): NpcDropRoll =
        NpcDropRoll { context ->
            resolveVestigeProgress(context, family, vestige)
        }

    private fun resolveVestigeProgress(
        context: NpcDropContext,
        family: ForgottenBossFamily,
        vestige: ObjType,
    ): NpcDropRollResult {
        val packed = context.hero.vars[varps.dt2_vestige_progress]
        val advance = advanceVestigeProgress(packed, family)
        VarPlayerIntMapSetter.set(context.hero, varps.dt2_vestige_progress, advance.packed)
        val type = if (advance.completed) vestige else objs.gold_ring
        val count = if (advance.completed) 1 else advance.stage
        val message =
            if (advance.completed) {
                "You feel an ancient vestige take shape."
            } else {
                "The gold rings hint at hidden vestige progress (${advance.stage}/3)."
            }
        return NpcDropRollResult(
            selected = true,
            drops = listOf(NpcDrop(InvObj(type, count), context.coords, context.hero)),
            messages = listOf(message),
        )
    }

    public companion object {
        public const val LEVIATHAN_UNIQUE_RATE: Int = 768
        public const val WHISPERER_UNIQUE_RATE: Int = 512
        public const val VARDORVIS_UNIQUE_RATE: Int = 1_088
        public const val DUKE_UNIQUE_RATE: Int = 720
        public const val QUARTZ_RATE: Int = 200
        public const val TABLET_RATE: Int = 25
        public const val LEVIATHAN_PET_RATE: Int = 2_500
        public const val WHISPERER_PET_RATE: Int = 2_000
        public const val VARDORVIS_PET_RATE: Int = 3_000
        public const val DUKE_PET_RATE: Int = 2_500

        public fun advanceVestigeProgress(
            packed: Int,
            family: ForgottenBossFamily,
        ): VestigeProgressAdvance {
            val shift = family.ordinal * VESTIGE_BITS
            val progress = (packed ushr shift) and VESTIGE_MASK
            val completed = progress >= 2
            val next = if (completed) 0 else progress + 1
            val updated = (packed and (VESTIGE_MASK shl shift).inv()) or (next shl shift)
            return VestigeProgressAdvance(updated, next, completed)
        }

        private const val VESTIGE_BITS = 2
        private const val VESTIGE_MASK = 0b11
    }

    public data class VestigeProgressAdvance(
        val packed: Int,
        val stage: Int,
        val completed: Boolean,
    )
}
