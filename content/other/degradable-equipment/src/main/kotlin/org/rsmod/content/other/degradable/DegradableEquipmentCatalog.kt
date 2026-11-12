package org.rsmod.content.other.degradable

import org.rsmod.api.config.refs.objs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

/** Cache-233 Barrows and Moon armour state graph, shared by degradation and repair services. */
public object DegradableEquipmentCatalog {
    public const val BARROWS_MAX_POINTS: Int = 1_000
    public const val MOON_MAX_POINTS: Int = 3_000
    public const val MOON_COINS_PER_POINT: Int = 500

    public val definitions: List<DegradableEquipmentDefinition> = buildList {
        addBarrowsSet(
            barrows(
                objs.ahrims_hood,
                objs.ahrims_hood_100,
                objs.ahrims_hood_75,
                objs.ahrims_hood_50,
                objs.ahrims_hood_25,
                objs.ahrims_hood_0,
                60,
            ),
            barrows(
                objs.ahrims_robetop,
                objs.ahrims_robetop_100,
                objs.ahrims_robetop_75,
                objs.ahrims_robetop_50,
                objs.ahrims_robetop_25,
                objs.ahrims_robetop_0,
                90,
            ),
            barrows(
                objs.ahrims_robeskirt,
                objs.ahrims_robeskirt_100,
                objs.ahrims_robeskirt_75,
                objs.ahrims_robeskirt_50,
                objs.ahrims_robeskirt_25,
                objs.ahrims_robeskirt_0,
                80,
            ),
            barrows(
                objs.ahrims_staff,
                objs.ahrims_staff_100,
                objs.ahrims_staff_75,
                objs.ahrims_staff_50,
                objs.ahrims_staff_25,
                objs.ahrims_staff_0,
                100,
            ),
        )
        addBarrowsSet(
            barrows(
                objs.dharoks_helm,
                objs.dharoks_helm_100,
                objs.dharoks_helm_75,
                objs.dharoks_helm_50,
                objs.dharoks_helm_25,
                objs.dharoks_helm_0,
                60,
            ),
            barrows(
                objs.dharoks_platebody,
                objs.dharoks_platebody_100,
                objs.dharoks_platebody_75,
                objs.dharoks_platebody_50,
                objs.dharoks_platebody_25,
                objs.dharoks_platebody_0,
                90,
            ),
            barrows(
                objs.dharoks_platelegs,
                objs.dharoks_platelegs_100,
                objs.dharoks_platelegs_75,
                objs.dharoks_platelegs_50,
                objs.dharoks_platelegs_25,
                objs.dharoks_platelegs_0,
                80,
            ),
            barrows(
                objs.dharoks_greataxe,
                objs.dharoks_greataxe_100,
                objs.dharoks_greataxe_75,
                objs.dharoks_greataxe_50,
                objs.dharoks_greataxe_25,
                objs.dharoks_greataxe_0,
                100,
            ),
        )
        addBarrowsSet(
            barrows(
                objs.guthans_helm,
                objs.guthans_helm_100,
                objs.guthans_helm_75,
                objs.guthans_helm_50,
                objs.guthans_helm_25,
                objs.guthans_helm_0,
                60,
            ),
            barrows(
                objs.guthans_platebody,
                objs.guthans_platebody_100,
                objs.guthans_platebody_75,
                objs.guthans_platebody_50,
                objs.guthans_platebody_25,
                objs.guthans_platebody_0,
                90,
            ),
            barrows(
                objs.guthans_chainskirt,
                objs.guthans_chainskirt_100,
                objs.guthans_chainskirt_75,
                objs.guthans_chainskirt_50,
                objs.guthans_chainskirt_25,
                objs.guthans_chainskirt_0,
                80,
            ),
            barrows(
                objs.guthans_warspear,
                objs.guthans_warspear_100,
                objs.guthans_warspear_75,
                objs.guthans_warspear_50,
                objs.guthans_warspear_25,
                objs.guthans_warspear_0,
                100,
            ),
        )
        addBarrowsSet(
            barrows(
                objs.karils_coif,
                objs.karils_coif_100,
                objs.karils_coif_75,
                objs.karils_coif_50,
                objs.karils_coif_25,
                objs.karils_coif_0,
                60,
            ),
            barrows(
                objs.karils_leathertop,
                objs.karils_leathertop_100,
                objs.karils_leathertop_75,
                objs.karils_leathertop_50,
                objs.karils_leathertop_25,
                objs.karils_leathertop_0,
                90,
            ),
            barrows(
                objs.karils_leatherskirt,
                objs.karils_leatherskirt_100,
                objs.karils_leatherskirt_75,
                objs.karils_leatherskirt_50,
                objs.karils_leatherskirt_25,
                objs.karils_leatherskirt_0,
                80,
            ),
            barrows(
                objs.karils_crossbow,
                objs.karils_crossbow_100,
                objs.karils_crossbow_75,
                objs.karils_crossbow_50,
                objs.karils_crossbow_25,
                objs.karils_crossbow_0,
                100,
            ),
        )
        addBarrowsSet(
            barrows(
                objs.torags_helm,
                objs.torags_helm_100,
                objs.torags_helm_75,
                objs.torags_helm_50,
                objs.torags_helm_25,
                objs.torags_helm_0,
                60,
            ),
            barrows(
                objs.torags_platebody,
                objs.torags_platebody_100,
                objs.torags_platebody_75,
                objs.torags_platebody_50,
                objs.torags_platebody_25,
                objs.torags_platebody_0,
                90,
            ),
            barrows(
                objs.torags_platelegs,
                objs.torags_platelegs_100,
                objs.torags_platelegs_75,
                objs.torags_platelegs_50,
                objs.torags_platelegs_25,
                objs.torags_platelegs_0,
                80,
            ),
            barrows(
                objs.torags_hammers,
                objs.torags_hammers_100,
                objs.torags_hammers_75,
                objs.torags_hammers_50,
                objs.torags_hammers_25,
                objs.torags_hammers_0,
                100,
            ),
        )
        addBarrowsSet(
            barrows(
                objs.veracs_helm,
                objs.veracs_helm_100,
                objs.veracs_helm_75,
                objs.veracs_helm_50,
                objs.veracs_helm_25,
                objs.veracs_helm_0,
                60,
            ),
            barrows(
                objs.veracs_brassard,
                objs.veracs_brassard_100,
                objs.veracs_brassard_75,
                objs.veracs_brassard_50,
                objs.veracs_brassard_25,
                objs.veracs_brassard_0,
                90,
            ),
            barrows(
                objs.veracs_plateskirt,
                objs.veracs_plateskirt_100,
                objs.veracs_plateskirt_75,
                objs.veracs_plateskirt_50,
                objs.veracs_plateskirt_25,
                objs.veracs_plateskirt_0,
                80,
            ),
            barrows(
                objs.veracs_flail,
                objs.veracs_flail_100,
                objs.veracs_flail_75,
                objs.veracs_flail_50,
                objs.veracs_flail_25,
                objs.veracs_flail_0,
                100,
            ),
        )
        addBarrowsSet(
            barrows(
                objs.ahrims_hood_or,
                objs.ahrims_hood_or_100,
                objs.ahrims_hood_or_75,
                objs.ahrims_hood_or_50,
                objs.ahrims_hood_or_25,
                objs.ahrims_hood_or_0,
                60,
            ),
            barrows(
                objs.ahrims_robetop_or,
                objs.ahrims_robetop_or_100,
                objs.ahrims_robetop_or_75,
                objs.ahrims_robetop_or_50,
                objs.ahrims_robetop_or_25,
                objs.ahrims_robetop_or_0,
                90,
            ),
            barrows(
                objs.ahrims_robeskirt_or,
                objs.ahrims_robeskirt_or_100,
                objs.ahrims_robeskirt_or_75,
                objs.ahrims_robeskirt_or_50,
                objs.ahrims_robeskirt_or_25,
                objs.ahrims_robeskirt_or_0,
                80,
            ),
            barrows(
                objs.ahrims_staff_or,
                objs.ahrims_staff_or_100,
                objs.ahrims_staff_or_75,
                objs.ahrims_staff_or_50,
                objs.ahrims_staff_or_25,
                objs.ahrims_staff_or_0,
                100,
            ),
        )

        addMoon(objs.eclipse_moon_helm, objs.eclipse_moon_helm_used, objs.eclipse_moon_helm_broken)
        addMoon(
            objs.eclipse_moon_chestplate,
            objs.eclipse_moon_chestplate_used,
            objs.eclipse_moon_chestplate_broken,
        )
        addMoon(
            objs.eclipse_moon_tassets,
            objs.eclipse_moon_tassets_used,
            objs.eclipse_moon_tassets_broken,
        )
        addMoon(objs.blue_moon_helm, objs.blue_moon_helm_used, objs.blue_moon_helm_broken)
        addMoon(
            objs.blue_moon_chestplate,
            objs.blue_moon_chestplate_used,
            objs.blue_moon_chestplate_broken,
        )
        addMoon(objs.blue_moon_tassets, objs.blue_moon_tassets_used, objs.blue_moon_tassets_broken)
        addMoon(objs.blood_moon_helm, objs.blood_moon_helm_used, objs.blood_moon_helm_broken)
        addMoon(
            objs.blood_moon_chestplate,
            objs.blood_moon_chestplate_used,
            objs.blood_moon_chestplate_broken,
        )
        addMoon(
            objs.blood_moon_tassets,
            objs.blood_moon_tassets_used,
            objs.blood_moon_tassets_broken,
        )
    }

    public val byType: Map<Int, DegradableEquipmentDefinition> =
        definitions
            .flatMap { definition -> definition.allTypes.map { it.id to definition } }
            .toMap()

    private fun MutableList<DegradableEquipmentDefinition>.addBarrowsSet(
        head: DegradableEquipmentDefinition,
        body: DegradableEquipmentDefinition,
        legs: DegradableEquipmentDefinition,
        weapon: DegradableEquipmentDefinition,
    ) {
        add(head)
        add(body)
        add(legs)
        add(weapon)
    }

    private fun barrows(
        repaired: ObjType,
        hundred: ObjType,
        seventyFive: ObjType,
        fifty: ObjType,
        twentyFive: ObjType,
        broken: ObjType,
        coinsPerMissingPoint: Int,
    ): DegradableEquipmentDefinition =
        DegradableEquipmentDefinition(
            family = DegradableEquipmentFamily.Barrows,
            repaired = repaired,
            activeStages = listOf(hundred, seventyFive, fifty, twentyFive),
            broken = broken,
            maxPoints = BARROWS_MAX_POINTS,
            coinsPerMissingPoint = coinsPerMissingPoint,
            canonicalStagePoints = listOf(1_000, 750, 500, 250),
        )

    private fun MutableList<DegradableEquipmentDefinition>.addMoon(
        repaired: ObjType,
        used: ObjType,
        broken: ObjType,
    ) {
        add(
            DegradableEquipmentDefinition(
                family = DegradableEquipmentFamily.Moon,
                repaired = repaired,
                activeStages = listOf(used),
                broken = broken,
                maxPoints = MOON_MAX_POINTS,
                coinsPerMissingPoint = MOON_COINS_PER_POINT,
                canonicalStagePoints = listOf(2_999),
            )
        )
    }
}

public enum class DegradableEquipmentFamily {
    Barrows,
    Moon,
}

public data class DegradableEquipmentDefinition(
    public val family: DegradableEquipmentFamily,
    public val repaired: ObjType,
    public val activeStages: List<ObjType>,
    public val broken: ObjType,
    public val maxPoints: Int,
    public val coinsPerMissingPoint: Int,
    private val canonicalStagePoints: List<Int>,
) {
    init {
        require(activeStages.size == canonicalStagePoints.size)
    }

    public val allTypes: List<ObjType> = listOf(repaired) + activeStages + broken

    public fun remainingPoints(obj: InvObj): Int =
        when (obj.id) {
            repaired.id -> maxPoints
            broken.id -> 0
            else -> {
                val stageIndex = activeStages.indexOfFirst { it.id == obj.id }
                require(stageIndex >= 0) {
                    "Obj does not belong to this degradation definition: $obj"
                }
                obj.vars.takeIf { it in 1..maxPoints } ?: canonicalStagePoints[stageIndex]
            }
        }

    public fun degrade(obj: InvObj): InvObj {
        val remaining = (remainingPoints(obj) - 1).coerceAtLeast(0)
        return InvObj(typeForRemaining(remaining), obj.count, vars = remaining)
    }

    public fun breakForDrop(obj: InvObj): InvObj = InvObj(broken, obj.count)

    public fun typeForRemaining(remaining: Int): ObjType {
        require(remaining in 0..maxPoints)
        if (remaining == maxPoints) return repaired
        if (remaining == 0) return broken
        return when (family) {
            DegradableEquipmentFamily.Moon -> activeStages.single()
            DegradableEquipmentFamily.Barrows ->
                when {
                    remaining > 750 -> activeStages[0]
                    remaining > 500 -> activeStages[1]
                    remaining > 250 -> activeStages[2]
                    else -> activeStages[3]
                }
        }
    }
}
