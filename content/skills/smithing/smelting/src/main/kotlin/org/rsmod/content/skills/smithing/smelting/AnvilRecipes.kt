package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public object AnvilInterfaces : InterfaceReferences() {
    val smithing = find("smithing")
}

public object AnvilComponents : ComponentReferences() {
    val button3 = find("smithing:make_1")
    val button4 = find("smithing:make_5")
    val button5 = find("smithing:make_10")
    val button6 = find("smithing:make_x")
    val button7 = find("smithing:make_all")
    val button8 = find("smithing:make_some")
    val button9 = find("smithing:dagger")
    val button10 = find("smithing:sword")
    val button11 = find("smithing:scimitar")
    val button12 = find("smithing:longsword")
    val button13 = find("smithing:2h")
    val button14 = find("smithing:axe")
    val button15 = find("smithing:mace")
    val button16 = find("smithing:warhammer")
    val button17 = find("smithing:battleaxe")
    val button18 = find("smithing:claws")
    val button19 = find("smithing:chainbody")
    val button20 = find("smithing:platelegs")
    val button21 = find("smithing:plateskirt")
    val button22 = find("smithing:platebody")
    val button23 = find("smithing:nails")
    val button24 = find("smithing:medhelm")
    val button25 = find("smithing:fullhelm")
    val button26 = find("smithing:squareshield")
    val button27 = find("smithing:kiteshield")
    val button28 = find("smithing:other_2")
    val button29 = find("smithing:darttips")
    val button30 = find("smithing:arrowheads")
    val button31 = find("smithing:knives")
    val button32 = find("smithing:other_1")
    val button33 = find("smithing:other_3")
    val button34 = find("smithing:bolts")
    val button35 = find("smithing:limbs")
    val quantities = listOf(button3, button4, button5, button6, button7, button8)
    val products =
        listOf(
            button9,
            button10,
            button11,
            button12,
            button13,
            button14,
            button15,
            button16,
            button17,
            button18,
            button19,
            button20,
            button21,
            button22,
            button23,
            button24,
            button25,
            button26,
            button27,
            button28,
            button29,
            button30,
            button31,
            button32,
            button33,
            button34,
            button35,
        )
}

public object AnvilLocs : LocReferences() {
    val ordinary =
        listOf(
                "anvil",
                "darkm_anvil",
                "viking_anvil",
                "dwarf_keldagrim_anvil",
                "dorgesh_blacksmith_anvil",
                "lovakengj_anvil",
                "ds2_guild_blacksmith_anvil",
                "cam_torum_anvil",
                "sw_anvil",
            )
            .map(::find)
    val doric = find("dorics_anvil")
    val rusted = find("lumbridge_anvil")
}

public object AnvilVars : VarBitReferences() {
    val barType = find("smithing_bar_type")
    val lastBarType = find("smithing_bar_lasttype")
    val lastItemType = find("smithing_item_lasttype")
}

public object AnvilQuestVars : VarpReferences() {
    val touristTrap = find("desertrescue")
    val deathPlateau = find("death_equiproom")
    val doric = find("doricquest")
}

public object AnvilSeqs : SeqReferences() {
    val smith = find("human_smithing")
    val imcando = find("human_smithing_imcando_hammer")
}

public data class AnvilRecipe(
    val metal: Int,
    val bar: ObjType,
    val component: ComponentType,
    val output: ObjType,
    val bars: Int,
    val count: Int,
    val level: Int,
    val xp: Double,
)

/**
 * Outputs/components are decoded from cache-233.4 client script 430; contracts use enums 844-846.
 */
public class AnvilRecipes
@Inject
constructor(private val enums: EnumTypeList, private val objTypes: ObjTypeList) {
    public val bars: List<ObjType> =
        listOf(
            objs.bronze_bar,
            objs.iron_bar,
            objs.steel_bar,
            objs.mithril_bar,
            objs.adamantite_bar,
            objs.runite_bar,
        )
    private val productIds: List<Map<Int, Int>> =
        listOf(
            mapOf(
                9 to 1205,
                10 to 1277,
                11 to 1321,
                12 to 1291,
                13 to 1307,
                14 to 1351,
                15 to 1422,
                16 to 1337,
                17 to 1375,
                18 to 3095,
                19 to 1103,
                20 to 1075,
                21 to 1087,
                22 to 1117,
                23 to 4819,
                24 to 1139,
                25 to 1155,
                26 to 1173,
                27 to 1189,
                29 to 819,
                30 to 39,
                31 to 864,
                32 to 1794,
                33 to 19570,
                34 to 9375,
                35 to 9420,
            ),
            mapOf(
                9 to 1203,
                10 to 1279,
                11 to 1323,
                12 to 1293,
                13 to 1309,
                14 to 1349,
                15 to 1420,
                16 to 1335,
                17 to 1363,
                18 to 3096,
                19 to 1101,
                20 to 1067,
                21 to 1081,
                22 to 1115,
                23 to 4820,
                24 to 1137,
                25 to 1153,
                26 to 1175,
                27 to 1191,
                28 to 4540,
                29 to 820,
                30 to 40,
                31 to 863,
                32 to 7225,
                33 to 19572,
                34 to 9377,
                35 to 9423,
            ),
            mapOf(
                9 to 1207,
                10 to 1281,
                11 to 1325,
                12 to 1295,
                13 to 1311,
                14 to 1353,
                15 to 1424,
                16 to 1339,
                17 to 1365,
                18 to 3097,
                19 to 1105,
                20 to 1069,
                21 to 1083,
                22 to 1119,
                23 to 1539,
                24 to 1141,
                25 to 1157,
                26 to 1177,
                27 to 1193,
                28 to 4544,
                29 to 821,
                30 to 41,
                31 to 865,
                32 to 2370,
                33 to 19574,
                34 to 9378,
                35 to 9425,
            ),
            mapOf(
                9 to 1209,
                10 to 1285,
                11 to 1329,
                12 to 1299,
                13 to 1315,
                14 to 1355,
                15 to 1428,
                16 to 1343,
                17 to 1369,
                18 to 3099,
                19 to 1109,
                20 to 1071,
                21 to 1085,
                22 to 1121,
                23 to 4822,
                24 to 1143,
                25 to 1159,
                26 to 1181,
                27 to 1197,
                29 to 822,
                30 to 42,
                31 to 866,
                32 to 9416,
                33 to 19576,
                34 to 9379,
                35 to 9427,
            ),
            mapOf(
                9 to 1211,
                10 to 1287,
                11 to 1331,
                12 to 1301,
                13 to 1317,
                14 to 1357,
                15 to 1430,
                16 to 1345,
                17 to 1371,
                18 to 3100,
                19 to 1111,
                20 to 1073,
                21 to 1091,
                22 to 1123,
                23 to 4823,
                24 to 1145,
                25 to 1161,
                26 to 1183,
                27 to 1199,
                29 to 823,
                30 to 43,
                31 to 867,
                32 to 19578,
                34 to 9380,
                35 to 9429,
            ),
            mapOf(
                9 to 1213,
                10 to 1289,
                11 to 1333,
                12 to 1303,
                13 to 1319,
                14 to 1359,
                15 to 1432,
                16 to 1347,
                17 to 1373,
                18 to 3101,
                19 to 1113,
                20 to 1079,
                21 to 1093,
                22 to 1127,
                23 to 4824,
                24 to 1147,
                25 to 1163,
                26 to 1185,
                27 to 1201,
                29 to 824,
                30 to 44,
                31 to 868,
                32 to 19580,
                34 to 9381,
                35 to 9431,
            ),
        )
    public val all: List<AnvilRecipe> by lazy {
        productIds.flatMapIndexed { metal, products ->
            products.map { (component, output) ->
                val cost = enums.getValue(845).primitiveMap.getValue(output) as Int
                AnvilRecipe(
                    metal + 1,
                    bars[metal],
                    AnvilComponents.products[component - 9],
                    objTypes.getValue(output),
                    cost,
                    enums.getValue(844).primitiveMap.getValue(output) as Int,
                    (enums.getValue(846).primitiveMap.getValue(output) as Int).coerceAtLeast(1),
                    (metal + 1) * 12.5 * cost,
                )
            }
        }
    }

    public fun find(metal: Int, component: ComponentType): AnvilRecipe? =
        all.firstOrNull { it.metal == metal && it.component.packed == component.packed }
}
