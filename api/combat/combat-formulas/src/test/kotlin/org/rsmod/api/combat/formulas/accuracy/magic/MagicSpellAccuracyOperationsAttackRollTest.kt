package org.rsmod.api.combat.formulas.accuracy.magic

import java.util.EnumSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.rsmod.api.testing.params.TestArgs
import org.rsmod.api.testing.params.TestArgsProvider
import org.rsmod.api.testing.params.TestWithArgs
import org.rsmod.api.testing.params.testArgsOfSingleParam

class MagicSpellAccuracyOperationsAttackRollTest {
    @TestWithArgs(LoadoutProvider::class)
    fun `calculate hit chance based on loadout`(loadout: Loadout) {
        val baseAttackRoll = loadout.baseAttackRoll
        val finalAttackRoll = loadout.finalAttackRoll
        val weaknessPercent = loadout.weaknessPercent
        val spellAttributes = loadout.spellAttributes
        val npcAttributes = loadout.npcAttributes

        val modifiedAttackRoll =
            MagicAccuracyOperations.modifySpellAttackRoll(
                attackRoll = baseAttackRoll,
                targetWeaknessPercent = weaknessPercent,
                spellAttributes = spellAttributes,
                npcAttributes = npcAttributes,
            )
        assertEquals(finalAttackRoll, modifiedAttackRoll)
    }

    data class Loadout(
        val baseAttackRoll: Int,
        val finalAttackRoll: Int,
        val weaknessPercent: Int = 0,
        val spellAttributes: EnumSet<SpellAttr> = EnumSet.noneOf(SpellAttr::class.java),
        val npcAttributes: EnumSet<NpcAttr> = EnumSet.noneOf(NpcAttr::class.java),
    ) {
        fun withWeaknessPercent(weaknessPercent: Int) = copy(weaknessPercent = weaknessPercent)

        fun withSpellAttributes(vararg attributes: SpellAttr) =
            copy(spellAttributes = EnumSet.copyOf(attributes.toSet()))

        fun withNpcAttributes(vararg attributes: NpcAttr) =
            copy(npcAttributes = EnumSet.copyOf(attributes.toSet()))
    }

    private object LoadoutProvider : TestArgsProvider {
        override fun args(): List<TestArgs> {
            return testArgsOfSingleParam(
                Loadout(baseAttackRoll = 9072, finalAttackRoll = 10886)
                    .withSpellAttributes(SpellAttr.AmuletOfAvarice)
                    .withNpcAttributes(NpcAttr.Revenant),
                Loadout(baseAttackRoll = 8640, finalAttackRoll = 14904)
                    .withWeaknessPercent(weaknessPercent = 50)
                    .withSpellAttributes(SpellAttr.BlackMaskI, SpellAttr.DragonHunterWand)
                    .withNpcAttributes(NpcAttr.Draconic, NpcAttr.SlayerTask, NpcAttr.WaterWeakness),
                Loadout(baseAttackRoll = 8640, finalAttackRoll = 19224)
                    .withWeaknessPercent(weaknessPercent = 50)
                    .withSpellAttributes(
                        SpellAttr.BlackMaskI,
                        SpellAttr.DragonHunterWand,
                        SpellAttr.WaterSpell,
                    )
                    .withNpcAttributes(NpcAttr.Draconic, NpcAttr.SlayerTask, NpcAttr.WaterWeakness),
                Loadout(baseAttackRoll = 6912, finalAttackRoll = 9537)
                    .withSpellAttributes(SpellAttr.BlackMaskI, SpellAttr.Demonbane)
                    .withNpcAttributes(NpcAttr.SlayerTask, NpcAttr.Demon),
                Loadout(baseAttackRoll = 10908, finalAttackRoll = 17561)
                    .withSpellAttributes(
                        SpellAttr.BlackMaskI,
                        SpellAttr.PurgingStaff,
                        SpellAttr.Demonbane,
                    )
                    .withNpcAttributes(NpcAttr.SlayerTask, NpcAttr.Demon),
                Loadout(baseAttackRoll = 10908, finalAttackRoll = 16056)
                    .withSpellAttributes(
                        SpellAttr.BlackMaskI,
                        SpellAttr.PurgingStaff,
                        SpellAttr.Demonbane,
                    )
                    .withNpcAttributes(
                        NpcAttr.SlayerTask,
                        NpcAttr.Demon,
                        NpcAttr.DemonbaneResistance,
                    ),
                Loadout(baseAttackRoll = 1000, finalAttackRoll = 1200)
                    .withSpellAttributes(SpellAttr.Demonbane)
                    .withNpcAttributes(NpcAttr.Demon),
                Loadout(baseAttackRoll = 1000, finalAttackRoll = 1400)
                    .withSpellAttributes(SpellAttr.Demonbane, SpellAttr.MarkOfDarkness)
                    .withNpcAttributes(NpcAttr.Demon),
                Loadout(baseAttackRoll = 1000, finalAttackRoll = 1400)
                    .withSpellAttributes(SpellAttr.Demonbane, SpellAttr.PurgingStaff)
                    .withNpcAttributes(NpcAttr.Demon),
                Loadout(baseAttackRoll = 1000, finalAttackRoll = 1800)
                    .withSpellAttributes(
                        SpellAttr.Demonbane,
                        SpellAttr.MarkOfDarkness,
                        SpellAttr.PurgingStaff,
                    )
                    .withNpcAttributes(NpcAttr.Demon),
                Loadout(baseAttackRoll = 1000, finalAttackRoll = 1280)
                    .withSpellAttributes(SpellAttr.Demonbane, SpellAttr.MarkOfDarkness)
                    .withNpcAttributes(NpcAttr.Demon, NpcAttr.DemonbaneResistance),
                Loadout(baseAttackRoll = 1000, finalAttackRoll = 1560)
                    .withSpellAttributes(
                        SpellAttr.Demonbane,
                        SpellAttr.MarkOfDarkness,
                        SpellAttr.PurgingStaff,
                    )
                    .withNpcAttributes(NpcAttr.Demon, NpcAttr.DemonbaneResistance),
                Loadout(baseAttackRoll = 1000, finalAttackRoll = 1100)
                    .withSpellAttributes(SpellAttr.WaterTome, SpellAttr.CurseSpell),
                Loadout(baseAttackRoll = 1000, finalAttackRoll = 1100)
                    .withSpellAttributes(SpellAttr.WaterTome, SpellAttr.BindSpell),
                Loadout(baseAttackRoll = 1000, finalAttackRoll = 1000)
                    .withSpellAttributes(SpellAttr.WaterTome),
            )
        }
    }
}
