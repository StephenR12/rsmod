package org.rsmod.content.skills.fletching.ammunition

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public enum class PoisonedAmmunitionFamily {
    Arrow,
    Bolt,
    Dart,
    Javelin,
    ThrowingKnife,
}

public enum class WeaponPoisonStrength(public val poison: ObjType) {
    Standard(objs.weapon_poison),
    Plus(objs.weapon_poison_plus),
    PlusPlus(objs.weapon_poison_plus_plus),
}

public data class PoisonedAmmunitionRecipe(
    val name: String,
    val family: PoisonedAmmunitionFamily,
    val ammunition: ObjType,
    val poison: ObjType,
    val output: ObjType,
    val strength: WeaponPoisonStrength,
)

public class PoisonedAmmunitionRecipeRegistry @Inject constructor() {
    public val recipes: List<PoisonedAmmunitionRecipe> = buildList {
        addPoisonable(
            "Bronze arrow",
            PoisonedAmmunitionFamily.Arrow,
            objs.bronze_arrow,
            objs.bronze_arrow_p,
            objs.bronze_arrow_p_plus,
            objs.bronze_arrow_p_plus_plus,
        )
        addPoisonable(
            "Iron arrow",
            PoisonedAmmunitionFamily.Arrow,
            objs.iron_arrow,
            objs.iron_arrow_p,
            objs.iron_arrow_p_plus,
            objs.iron_arrow_p_plus_plus,
        )
        addPoisonable(
            "Steel arrow",
            PoisonedAmmunitionFamily.Arrow,
            objs.steel_arrow,
            objs.steel_arrow_p,
            objs.steel_arrow_p_plus,
            objs.steel_arrow_p_plus_plus,
        )
        addPoisonable(
            "Mithril arrow",
            PoisonedAmmunitionFamily.Arrow,
            objs.mithril_arrow,
            objs.mithril_arrow_p,
            objs.mithril_arrow_p_plus,
            objs.mithril_arrow_p_plus_plus,
        )
        addPoisonable(
            "Adamant arrow",
            PoisonedAmmunitionFamily.Arrow,
            objs.adamant_arrow,
            objs.adamant_arrow_p,
            objs.adamant_arrow_p_plus,
            objs.adamant_arrow_p_plus_plus,
        )
        addPoisonable(
            "Rune arrow",
            PoisonedAmmunitionFamily.Arrow,
            objs.rune_arrow,
            objs.rune_arrow_p,
            objs.rune_arrow_p_plus,
            objs.rune_arrow_p_plus_plus,
        )
        addPoisonable(
            "Amethyst arrow",
            PoisonedAmmunitionFamily.Arrow,
            objs.amethyst_arrow,
            objs.amethyst_arrow_p,
            objs.amethyst_arrow_p_plus,
            objs.amethyst_arrow_p_plus_plus,
        )
        addPoisonable(
            "Dragon arrow",
            PoisonedAmmunitionFamily.Arrow,
            objs.dragon_arrow,
            objs.dragon_arrow_p,
            objs.dragon_arrow_p_plus,
            objs.dragon_arrow_p_plus_plus,
        )

        addPoisonable(
            "Bronze bolts",
            PoisonedAmmunitionFamily.Bolt,
            objs.bronze_bolts,
            objs.bronze_bolts_p,
            objs.bronze_bolts_p_plus,
            objs.bronze_bolts_p_plus_plus,
        )
        addPoisonable(
            "Blurite bolts",
            PoisonedAmmunitionFamily.Bolt,
            objs.blurite_bolts,
            objs.blurite_bolts_p,
            objs.blurite_bolts_p_plus,
            objs.blurite_bolts_p_plus_plus,
        )
        addPoisonable(
            "Iron bolts",
            PoisonedAmmunitionFamily.Bolt,
            objs.iron_bolts,
            objs.iron_bolts_p,
            objs.iron_bolts_p_plus,
            objs.iron_bolts_p_plus_plus,
        )
        addPoisonable(
            "Silver bolts",
            PoisonedAmmunitionFamily.Bolt,
            objs.silver_bolts,
            objs.silver_bolts_p,
            objs.silver_bolts_p_plus,
            objs.silver_bolts_p_plus_plus,
        )
        addPoisonable(
            "Steel bolts",
            PoisonedAmmunitionFamily.Bolt,
            objs.steel_bolts,
            objs.steel_bolts_p,
            objs.steel_bolts_p_plus,
            objs.steel_bolts_p_plus_plus,
        )
        addPoisonable(
            "Mithril bolts",
            PoisonedAmmunitionFamily.Bolt,
            objs.mithril_bolts,
            objs.mithril_bolts_p,
            objs.mithril_bolts_p_plus,
            objs.mithril_bolts_p_plus_plus,
        )
        addPoisonable(
            "Adamant bolts",
            PoisonedAmmunitionFamily.Bolt,
            objs.adamant_bolts,
            objs.adamant_bolts_p,
            objs.adamant_bolts_p_plus,
            objs.adamant_bolts_p_plus_plus,
        )
        addPoisonable(
            "Runite bolts",
            PoisonedAmmunitionFamily.Bolt,
            objs.runite_bolts,
            objs.runite_bolts_p,
            objs.runite_bolts_p_plus,
            objs.runite_bolts_p_plus_plus,
        )

        addPoisonable(
            "Bronze dart",
            PoisonedAmmunitionFamily.Dart,
            objs.bronze_dart,
            objs.bronze_dart_p,
            objs.bronze_dart_p_plus,
            objs.bronze_dart_p_plus_plus,
        )
        addPoisonable(
            "Iron dart",
            PoisonedAmmunitionFamily.Dart,
            objs.iron_dart,
            objs.iron_dart_p,
            objs.iron_dart_p_plus,
            objs.iron_dart_p_plus_plus,
        )
        addPoisonable(
            "Steel dart",
            PoisonedAmmunitionFamily.Dart,
            objs.steel_dart,
            objs.steel_dart_p,
            objs.steel_dart_p_plus,
            objs.steel_dart_p_plus_plus,
        )
        addPoisonable(
            "Black dart",
            PoisonedAmmunitionFamily.Dart,
            objs.black_dart,
            objs.black_dart_p,
            objs.black_dart_p_plus,
            objs.black_dart_p_plus_plus,
        )
        addPoisonable(
            "Mithril dart",
            PoisonedAmmunitionFamily.Dart,
            objs.mithril_dart,
            objs.mithril_dart_p,
            objs.mithril_dart_p_plus,
            objs.mithril_dart_p_plus_plus,
        )
        addPoisonable(
            "Adamant dart",
            PoisonedAmmunitionFamily.Dart,
            objs.adamant_dart,
            objs.adamant_dart_p,
            objs.adamant_dart_p_plus,
            objs.adamant_dart_p_plus_plus,
        )
        addPoisonable(
            "Rune dart",
            PoisonedAmmunitionFamily.Dart,
            objs.rune_dart,
            objs.rune_dart_p,
            objs.rune_dart_p_plus,
            objs.rune_dart_p_plus_plus,
        )
        addPoisonable(
            "Amethyst dart",
            PoisonedAmmunitionFamily.Dart,
            objs.amethyst_dart,
            objs.amethyst_dart_p,
            objs.amethyst_dart_p_plus,
            objs.amethyst_dart_p_plus_plus,
        )
        addPoisonable(
            "Dragon dart",
            PoisonedAmmunitionFamily.Dart,
            objs.dragon_dart,
            objs.dragon_dart_p,
            objs.dragon_dart_p_plus,
            objs.dragon_dart_p_plus_plus,
        )

        addPoisonable(
            "Bronze javelin",
            PoisonedAmmunitionFamily.Javelin,
            objs.bronze_javelin,
            objs.bronze_javelin_p,
            objs.bronze_javelin_p_plus,
            objs.bronze_javelin_p_plus_plus,
        )
        addPoisonable(
            "Iron javelin",
            PoisonedAmmunitionFamily.Javelin,
            objs.iron_javelin,
            objs.iron_javelin_p,
            objs.iron_javelin_p_plus,
            objs.iron_javelin_p_plus_plus,
        )
        addPoisonable(
            "Steel javelin",
            PoisonedAmmunitionFamily.Javelin,
            objs.steel_javelin,
            objs.steel_javelin_p,
            objs.steel_javelin_p_plus,
            objs.steel_javelin_p_plus_plus,
        )
        addPoisonable(
            "Mithril javelin",
            PoisonedAmmunitionFamily.Javelin,
            objs.mithril_javelin,
            objs.mithril_javelin_p,
            objs.mithril_javelin_p_plus,
            objs.mithril_javelin_p_plus_plus,
        )
        addPoisonable(
            "Adamant javelin",
            PoisonedAmmunitionFamily.Javelin,
            objs.adamant_javelin,
            objs.adamant_javelin_p,
            objs.adamant_javelin_p_plus,
            objs.adamant_javelin_p_plus_plus,
        )
        addPoisonable(
            "Rune javelin",
            PoisonedAmmunitionFamily.Javelin,
            objs.rune_javelin,
            objs.rune_javelin_p,
            objs.rune_javelin_p_plus,
            objs.rune_javelin_p_plus_plus,
        )
        addPoisonable(
            "Amethyst javelin",
            PoisonedAmmunitionFamily.Javelin,
            objs.amethyst_javelin,
            objs.amethyst_javelin_p,
            objs.amethyst_javelin_p_plus,
            objs.amethyst_javelin_p_plus_plus,
        )
        addPoisonable(
            "Dragon javelin",
            PoisonedAmmunitionFamily.Javelin,
            objs.dragon_javelin,
            objs.dragon_javelin_p,
            objs.dragon_javelin_p_plus,
            objs.dragon_javelin_p_plus_plus,
        )

        addPoisonable(
            "Bronze knife",
            PoisonedAmmunitionFamily.ThrowingKnife,
            objs.bronze_throwing_knife,
            objs.bronze_throwing_knife_p,
            objs.bronze_throwing_knife_p_plus,
            objs.bronze_throwing_knife_p_plus_plus,
        )
        addPoisonable(
            "Iron knife",
            PoisonedAmmunitionFamily.ThrowingKnife,
            objs.iron_throwing_knife,
            objs.iron_throwing_knife_p,
            objs.iron_throwing_knife_p_plus,
            objs.iron_throwing_knife_p_plus_plus,
        )
        addPoisonable(
            "Steel knife",
            PoisonedAmmunitionFamily.ThrowingKnife,
            objs.steel_throwing_knife,
            objs.steel_throwing_knife_p,
            objs.steel_throwing_knife_p_plus,
            objs.steel_throwing_knife_p_plus_plus,
        )
        addPoisonable(
            "Black knife",
            PoisonedAmmunitionFamily.ThrowingKnife,
            objs.black_throwing_knife,
            objs.black_throwing_knife_p,
            objs.black_throwing_knife_p_plus,
            objs.black_throwing_knife_p_plus_plus,
        )
        addPoisonable(
            "Mithril knife",
            PoisonedAmmunitionFamily.ThrowingKnife,
            objs.mithril_throwing_knife,
            objs.mithril_throwing_knife_p,
            objs.mithril_throwing_knife_p_plus,
            objs.mithril_throwing_knife_p_plus_plus,
        )
        addPoisonable(
            "Adamant knife",
            PoisonedAmmunitionFamily.ThrowingKnife,
            objs.adamant_throwing_knife,
            objs.adamant_throwing_knife_p,
            objs.adamant_throwing_knife_p_plus,
            objs.adamant_throwing_knife_p_plus_plus,
        )
        addPoisonable(
            "Rune knife",
            PoisonedAmmunitionFamily.ThrowingKnife,
            objs.rune_throwing_knife,
            objs.rune_throwing_knife_p,
            objs.rune_throwing_knife_p_plus,
            objs.rune_throwing_knife_p_plus_plus,
        )
        addPoisonable(
            "Dragon knife",
            PoisonedAmmunitionFamily.ThrowingKnife,
            objs.dragon_throwing_knife,
            objs.dragon_throwing_knife_p,
            objs.dragon_throwing_knife_p_plus,
            objs.dragon_throwing_knife_p_plus_plus,
        )
    }

    private fun MutableList<PoisonedAmmunitionRecipe>.addPoisonable(
        name: String,
        family: PoisonedAmmunitionFamily,
        ammunition: ObjType,
        poisoned: ObjType,
        poisonedPlus: ObjType,
        poisonedPlusPlus: ObjType,
    ) {
        add(
            PoisonedAmmunitionRecipe(
                name,
                family,
                ammunition,
                objs.weapon_poison,
                poisoned,
                WeaponPoisonStrength.Standard,
            )
        )
        add(
            PoisonedAmmunitionRecipe(
                name,
                family,
                ammunition,
                objs.weapon_poison_plus,
                poisonedPlus,
                WeaponPoisonStrength.Plus,
            )
        )
        add(
            PoisonedAmmunitionRecipe(
                name,
                family,
                ammunition,
                objs.weapon_poison_plus_plus,
                poisonedPlusPlus,
                WeaponPoisonStrength.PlusPlus,
            )
        )
    }
}
