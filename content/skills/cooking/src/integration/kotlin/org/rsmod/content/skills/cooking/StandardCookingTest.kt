package org.rsmod.content.skills.cooking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class StandardCookingTest {
    @Test
    fun GameTestState.`every cache 233 Cook location is classified and registered`() =
        runCookingTest { deps ->
            val cacheAppliances =
                deps.locs.values
                    .filter { type -> type.op.any { it.equals("Cook", ignoreCase = true) } }
                    .sortedBy { it.id }
            val classified = cooking_locs.allCookOperationSources.sortedBy { it.id }
            assertEquals(cacheAppliances.map { it.id }, classified.map { it.id })
            assertEquals(listOf(2), deps.dbRows.types.getValue(17).data[19])
            assertEquals(29, varps.cookquest.id)
            assertEquals(320, varps.tbwt_main.id)
            assertEquals(321, varps.tbwt_tiadeche.id)
            assertEquals(listOf(6), deps.dbRows.types.getValue(141).data[19])
            assertEquals(3142, objs.raw_karambwan.id)
            assertEquals(3144, objs.cooked_karambwan.id)
            assertEquals(3146, objs.poison_karambwan.id)
            assertEquals(3148, objs.burnt_karambwan.id)
            assertEquals(9545, varbits.poison_karambwan_warning_disabled.id)
            assertEquals(1425, deps.types.varbits.types.getValue(9545).baseVar.id)
            assertEquals(27..27, deps.types.varbits.types.getValue(9545).bits)
            assertEquals(3604, varbits.karamja_diary_hard_karambwan.id)
            assertEquals(1002, deps.types.varbits.types.getValue(3604).baseVar.id)
            assertEquals(4..4, deps.types.varbits.types.getValue(3604).bits)
            assertEquals(65534, varbits.tbwt_tinsay_reward_claimed.id)
            assertEquals(65531, varbits.tbwt_tinsay_reward_claimed.baseVar.id)
            assertEquals(31..31, varbits.tbwt_tinsay_reward_claimed.bits)
            assertEquals(VarpLifetime.Perm, deps.types.varps.getValue(65531).scope)
            assertEquals(88, cooking_queues.karambwan.id)
            assertEquals(9436, objs.sinew.id)
            assertEquals(89, cooking_queues.sinew.id)

            for (source in classified) {
                val type = deps.locs[source]
                val operation = type.op.indexOfFirst { it.equals("Cook", ignoreCase = true) } + 1
                when (operation) {
                    1 -> assertTrue(deps.events.contains(LocEvents.Op1::class.java, source.id))
                    2 -> assertTrue(deps.events.contains(LocEvents.Op2::class.java, source.id))
                    else -> error("Unsupported cache Cook operation $operation for ${source.id}")
                }
                assertTrue(
                    deps.events.contains(
                        LocUEvents.Op::class.java,
                        EventBus.composeLongKey(source.id, objs.raw_shrimps.id),
                    ),
                    "Missing raw-food use for ${source.id}",
                )
                assertTrue(
                    deps.events.contains(
                        LocUEvents.Op::class.java,
                        EventBus.composeLongKey(source.id, objs.raw_karambwan.id),
                    ),
                    "Missing raw-karambwan use for ${source.id}",
                )
                assertTrue(
                    deps.events.contains(
                        LocUEvents.Op::class.java,
                        EventBus.composeLongKey(source.id, objs.burnt_karambwan.id),
                    ),
                    "Missing burnt-karambwan reheat use for ${source.id}",
                )
            }
        }

    @Test
    fun GameTestState.`cache 233 recipes sources animations and dispatch are pinned`() =
        runCookingTest { deps ->
            assertEquals(
                listOf(
                    RecipeContract(2138, 2140, 2144, 1, 30.0, false),
                    RecipeContract(2132, 2142, 2146, 1, 30.0, false),
                    RecipeContract(2134, 2142, 2146, 1, 30.0, false),
                    RecipeContract(2136, 2142, 2146, 1, 30.0, false),
                    RecipeContract(25833, 2142, 2146, 1, 30.0, false),
                    RecipeContract(10816, 2142, 2146, 1, 30.0, true),
                    RecipeContract(24782, 24785, 2146, 1, 30.0, true),
                    RecipeContract(3226, 3228, 2146, 1, 30.0, true),
                    RecipeContract(1859, 1861, 2146, 1, 40.0, true),
                    RecipeContract(317, 315, 7954, 1, 30.0, false),
                    RecipeContract(321, 319, 323, 1, 30.0, false),
                    RecipeContract(327, 325, 369, 1, 40.0, false),
                    RecipeContract(345, 347, 357, 5, 50.0, false),
                    RecipeContract(353, 355, 357, 10, 60.0, true),
                    RecipeContract(3363, 3369, 3375, 12, 70.0, true),
                    RecipeContract(335, 333, 343, 15, 70.0, false),
                    RecipeContract(3365, 3371, 3375, 17, 80.0, true),
                    RecipeContract(341, 339, 343, 18, 75.0, true),
                    RecipeContract(349, 351, 343, 20, 80.0, false),
                    RecipeContract(3367, 3373, 3375, 22, 95.0, true),
                    RecipeContract(29104, 29128, 29155, 23, 73.0, true),
                    RecipeContract(331, 329, 343, 25, 90.0, false),
                    RecipeContract(3379, 3381, 3383, 28, 95.0, true),
                    RecipeContract(359, 361, 367, 30, 100.0, false),
                    RecipeContract(29122, 29146, 29157, 31, 92.0, true),
                    RecipeContract(29101, 29131, 29155, 32, 106.0, true),
                    RecipeContract(10138, 10136, 10140, 35, 110.0, true),
                    RecipeContract(5001, 5003, 5002, 38, 115.0, true),
                    RecipeContract(377, 379, 381, 40, 120.0, false),
                    RecipeContract(29119, 29149, 29157, 41, 124.0, true),
                    RecipeContract(363, 365, 367, 43, 130.0, true),
                    RecipeContract(371, 373, 375, 45, 140.0, false),
                    RecipeContract(29125, 29152, 29157, 51, 143.0, true),
                    RecipeContract(2148, 2149, null, 53, 30.0, true),
                    RecipeContract(29110, 29137, 29161, 59, 154.0, true),
                    RecipeContract(7944, 7946, 7948, 62, 150.0, true),
                    RecipeContract(29116, 29140, 29159, 68, 175.0, true),
                    RecipeContract(383, 385, 387, 80, 210.0, true),
                    RecipeContract(395, 397, 399, 82, 211.3, true),
                    RecipeContract(29107, 29134, 29155, 82, 215.0, true),
                    RecipeContract(13439, 13441, 13443, 84, 230.0, true),
                    RecipeContract(11934, 11936, 11938, 90, 215.0, true),
                    RecipeContract(389, 391, 393, 91, 216.3, true),
                    RecipeContract(29113, 29143, 29159, 92, 220.0, true),
                    RecipeContract(2307, 2309, 2311, 1, 40.0, false),
                    RecipeContract(1863, 1865, null, 58, 40.0, true),
                    RecipeContract(2321, 2325, 2329, 10, 78.0, false),
                    RecipeContract(2319, 2327, 2329, 20, 110.0, false),
                    RecipeContract(7168, 7170, 2329, 29, 128.0, true),
                    RecipeContract(2317, 2323, 2329, 30, 130.0, false),
                    RecipeContract(7176, 7178, 2329, 34, 138.0, true),
                    RecipeContract(7186, 7188, 2329, 47, 164.0, true),
                    RecipeContract(19656, 19662, 2329, 52, 180.0, true),
                    RecipeContract(21684, 21690, 2329, 60, 200.0, true),
                    RecipeContract(7196, 7198, 2329, 70, 210.0, true),
                    RecipeContract(22789, 22795, 2329, 73, 220.0, true),
                    RecipeContract(7206, 7208, 2329, 85, 240.0, true),
                    RecipeContract(7216, 7218, 2329, 95, 260.0, true),
                    RecipeContract(1889, 1891, 1903, 40, 180.0, false),
                    RecipeContract(2287, 2289, 2305, 35, 143.0, false),
                    RecipeContract(2001, 2003, 2005, 25, 117.0, false),
                    RecipeContract(2009, 2011, 2013, 60, 280.0, true),
                    RecipeContract(1942, 6701, 6699, 7, 15.0, true),
                    RecipeContract(7076, 7078, 7090, 13, 50.0, true),
                    RecipeContract(5986, 5988, 5990, 28, 104.0, true),
                    RecipeContract(1871, 7084, 7092, 42, 60.0, true),
                    RecipeContract(7080, 7082, 7094, 46, 60.0, true),
                    RecipeContract(1921, 4456, null, 1, 0.0, true),
                    RecipeContract(4458, 4460, null, 1, 0.0, true),
                    RecipeContract(4237, 4239, 1923, 20, 52.0, true),
                    RecipeContract(2341, 2343, 2345, 50, 30.0, true),
                    RecipeContract(2142, 2146, 2146, 1, 0.0, false),
                    RecipeContract(24785, 2146, 2146, 1, 0.0, true),
                ),
                StandardCookingRecipes.all.map {
                    RecipeContract(
                        it.raw.id,
                        it.cooked.id,
                        it.burnt?.id,
                        it.level,
                        it.xp,
                        it.members,
                    )
                },
            )
            assertEquals(26185, cooking_locs.fire.id)
            assertEquals(26181, cooking_locs.range.id)
            assertEquals(114, cooking_locs.lumbridgeRange.id)
            assertEquals(27516, cooking_locs.hosidiusRange.id)
            assertEquals(27517, cooking_locs.hosidiusRange2.id)
            assertEquals(897, seqs.human_firecooking.id)
            assertEquals(896, seqs.human_cooking.id)
            assertNotNull(deps.seqs[seqs.human_firecooking])
            assertNotNull(deps.seqs[seqs.human_cooking])
            assertEquals("Cook", deps.locs[cooking_locs.range].op[0])
            for (recipe in StandardCookingRecipes.all) {
                if (recipe.raw != objs.potato && recipe.raw != objs.bowl_water) {
                    assertEquals(
                        recipe.members,
                        deps.objs[recipe.raw].members,
                        deps.objs[recipe.raw].name,
                    )
                }
                assertTrue(
                    deps.events.contains(
                        LocUEvents.Op::class.java,
                        EventBus.composeLongKey(cooking_locs.range.id, recipe.raw.id),
                    )
                )
                assertEquals(
                    CookingSourceKind.Fire in recipe.allowedSources,
                    deps.events.contains(
                        LocUEvents.Op::class.java,
                        EventBus.composeLongKey(cooking_locs.fire.id, recipe.raw.id),
                    ),
                    deps.objs[recipe.raw].name,
                )
            }
        }

    @Test
    fun GameTestState.`range breads pin source restrictions curves timing and gates`() =
        runCookingTest { deps ->
            assertEquals(2307, objs.bread_dough.id)
            assertEquals(2309, objs.bread.id)
            assertEquals(2311, objs.burnt_bread.id)
            assertEquals(1863, objs.pitta_dough.id)
            assertEquals(1865, objs.pitta_bread.id)
            assertEquals(1867, objs.burnt_pitta_bread.id)

            val bread = StandardCookingRecipes.byRaw.getValue(objs.bread_dough.id)
            val pitta = StandardCookingRecipes.byRaw.getValue(objs.pitta_dough.id)
            val rangeSources =
                setOf(
                    CookingSourceKind.Range,
                    CookingSourceKind.Lumbridge,
                    CookingSourceKind.Hosidius,
                )
            assertEquals(rangeSources, bread.allowedSources)
            assertEquals(rangeSources, pitta.allowedSources)
            assertEquals(CookingChance(118, 492), bread.range)
            assertEquals(CookingChance(128, 512), bread.lumbridge)
            assertEquals(CookingChance(130, 504), bread.hosidius)
            assertEquals(CookingChance(143, 517), bread.hosidiusElite)
            assertTrue(pitta.alwaysSucceeds)
            assertEquals(null, pitta.burnt)

            preparePlayer(level = 1)
            player.inv[4] = InvObj(objs.bread_dough)
            player.inv[8] = InvObj(objs.bread_dough)
            val range = prepareSource(cooking_locs.range)
            random.next = 99
            random.then = 0
            player.withProtectedAccess {
                deps.cooking.start(this, range, CookingSourceKind.Range, bread, 2)
            }
            advance()
            assertEquals(1, player.count(objs.burnt_bread))
            assertEquals(1, player.count(objs.bread_dough))
            assertEquals(0, cookingXp())
            advance(3)
            assertEquals(1, player.count(objs.bread))
            assertEquals(0, player.count(objs.bread_dough))
            assertEquals(400, cookingXp())

            preparePlayer(level = 57)
            add(objs.pitta_dough)
            val lowRange = prepareSource(cooking_locs.range, offset = 20)
            useOnSource(deps, lowRange, slot = 0, objs.pitta_dough)
            assertEquals(1, player.count(objs.pitta_dough))
            assertMessageSent("You need a Cooking level of 58 to cook this food.")

            preparePlayer(level = 57)
            player.setCurrentLevel(stats.cooking, 58)
            add(objs.pitta_dough)
            val boostedRange = prepareSource(cooking_locs.range, offset = 40)
            random.next = 99
            useOnSource(deps, boostedRange, slot = 0, objs.pitta_dough)
            advance()
            assertEquals(1, player.count(objs.pitta_bread))
            assertEquals(400, cookingXp())

            preparePlayer(level = 58, members = false)
            add(objs.pitta_dough)
            val freeRange = prepareSource(cooking_locs.range, offset = 60)
            useOnSource(deps, freeRange, slot = 0, objs.pitta_dough)
            assertEquals(1, player.count(objs.pitta_dough))
            assertMessageSent("You need to be on a members' world to cook this food.")

            preparePlayer(level = 99)
            add(objs.bread_dough)
            val fire = prepareSource(cooking_locs.fire, offset = 80)
            useOnSource(deps, fire, slot = 0, objs.bread_dough)
            assertEquals(1, player.count(objs.bread_dough))
            assertMessageSent("Nothing interesting happens.")
        }

    @Test
    fun GameTestState.`standard pies pin cache curves cadence restrictions and burn boundaries`() =
        runCookingTest { deps ->
            val cases =
                listOf(
                    PieHeatCase(
                        objs.uncooked_redberry_pie,
                        objs.redberry_pie,
                        level = 10,
                        xp = 78.0,
                        members = false,
                        range = CookingChance(98, 452),
                        lumbridge = CookingChance(108, 462),
                        hosidius = CookingChance(110, 464),
                        elite = CookingChance(123, 477),
                        rangeNoBurnLevel = 45,
                        lumbridgeNoBurnLevel = 42,
                        hosidiusNoBurnLevel = 42,
                    ),
                    PieHeatCase(
                        objs.uncooked_meat_pie,
                        objs.meat_pie,
                        level = 20,
                        xp = 110.0,
                        members = false,
                        range = CookingChance(78, 412),
                        lumbridge = CookingChance(88, 432),
                        hosidius = CookingChance(90, 424),
                        elite = CookingChance(103, 437),
                        rangeNoBurnLevel = 54,
                        lumbridgeNoBurnLevel = 49,
                        hosidiusNoBurnLevel = 50,
                    ),
                    PieHeatCase(
                        objs.uncooked_mud_pie,
                        objs.mud_pie,
                        level = 29,
                        xp = 128.0,
                        members = true,
                        range = CookingChance(58, 372),
                        hosidius = CookingChance(70, 384),
                        elite = CookingChance(83, 397),
                        rangeNoBurnLevel = 63,
                        hosidiusNoBurnLevel = 59,
                    ),
                    PieHeatCase(
                        objs.uncooked_apple_pie,
                        objs.apple_pie,
                        level = 30,
                        xp = 130.0,
                        members = false,
                        range = CookingChance(58, 372),
                        hosidius = CookingChance(70, 384),
                        elite = CookingChance(83, 397),
                        rangeNoBurnLevel = 63,
                        hosidiusNoBurnLevel = 59,
                    ),
                    PieHeatCase(
                        objs.uncooked_garden_pie,
                        objs.garden_pie,
                        level = 34,
                        xp = 138.0,
                        members = true,
                        range = CookingChance(48, 352),
                        hosidius = CookingChance(60, 364),
                        elite = CookingChance(73, 377),
                        rangeNoBurnLevel = 68,
                        hosidiusNoBurnLevel = 64,
                    ),
                    PieHeatCase(
                        objs.uncooked_fish_pie,
                        objs.fish_pie,
                        level = 47,
                        xp = 164.0,
                        members = true,
                        range = CookingChance(38, 332),
                        hosidius = CookingChance(50, 344),
                        elite = CookingChance(63, 357),
                        rangeNoBurnLevel = 74,
                        hosidiusNoBurnLevel = 70,
                    ),
                    PieHeatCase(
                        objs.uncooked_botanical_pie,
                        objs.botanical_pie,
                        level = 52,
                        xp = 180.0,
                        members = true,
                        range = CookingChance(20, 300),
                        lumbridge = CookingChance(108, 462),
                        hosidius = CookingChance(32, 312),
                        elite = CookingChance(45, 325),
                        rangeNoBurnLevel = 84,
                        lumbridgeNoBurnLevel = 52,
                        hosidiusNoBurnLevel = 80,
                    ),
                    PieHeatCase(
                        objs.uncooked_mushroom_pie,
                        objs.mushroom_pie,
                        level = 60,
                        xp = 200.0,
                        members = true,
                        range = CookingChance(17, 285),
                        lumbridge = CookingChance(140, 450),
                        hosidius = CookingChance(29, 297),
                        elite = CookingChance(42, 310),
                        rangeNoBurnLevel = 89,
                        lumbridgeNoBurnLevel = 60,
                        hosidiusNoBurnLevel = 84,
                    ),
                    PieHeatCase(
                        objs.uncooked_admiral_pie,
                        objs.admiral_pie,
                        level = 70,
                        xp = 210.0,
                        members = true,
                        range = CookingChance(15, 270),
                        hosidius = CookingChance(27, 282),
                        elite = CookingChance(40, 295),
                        rangeNoBurnLevel = 94,
                        hosidiusNoBurnLevel = 89,
                    ),
                    PieHeatCase(
                        objs.uncooked_dragonfruit_pie,
                        objs.dragonfruit_pie,
                        level = 73,
                        xp = 220.0,
                        members = true,
                        range = CookingChance(8, 250),
                        hosidius = CookingChance(20, 262),
                        elite = CookingChance(33, 275),
                        hosidiusNoBurnLevel = 97,
                    ),
                    PieHeatCase(
                        objs.uncooked_wild_pie,
                        objs.wild_pie,
                        level = 85,
                        xp = 240.0,
                        members = true,
                        range = CookingChance(1, 222),
                        hosidius = CookingChance(13, 234),
                        elite = CookingChance(26, 247),
                    ),
                    PieHeatCase(
                        objs.uncooked_summer_pie,
                        objs.summer_pie,
                        level = 95,
                        xp = 260.0,
                        members = true,
                        range = CookingChance(1, 212),
                        hosidius = CookingChance(13, 224),
                        elite = CookingChance(26, 237),
                    ),
                )
            assertEquals(
                listOf(
                    2321,
                    2325,
                    2319,
                    2327,
                    7168,
                    7170,
                    2317,
                    2323,
                    7176,
                    7178,
                    7186,
                    7188,
                    19656,
                    19662,
                    21684,
                    21690,
                    7196,
                    7198,
                    22789,
                    22795,
                    7206,
                    7208,
                    7216,
                    7218,
                ),
                cases.flatMap { listOf(it.raw.id, it.cooked.id) },
            )
            assertEquals(2329, objs.burnt_pie.id)
            val rangeSources =
                setOf(
                    CookingSourceKind.Range,
                    CookingSourceKind.Lumbridge,
                    CookingSourceKind.Hosidius,
                )

            for ((index, case) in cases.withIndex()) {
                val recipe = StandardCookingRecipes.byRaw.getValue(case.raw.id)
                assertEquals(objs.burnt_pie, recipe.burnt)
                assertEquals(case.level, recipe.level)
                assertEquals(case.xp, recipe.xp)
                assertEquals(case.members, recipe.members)
                assertEquals(case.range, recipe.fire)
                assertEquals(case.range, recipe.range)
                assertEquals(case.lumbridge, recipe.lumbridge)
                assertEquals(case.hosidius, recipe.hosidius)
                assertEquals(case.elite, recipe.hosidiusElite)
                assertEquals(
                    case.lumbridge ?: case.range,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Lumbridge, false),
                )
                assertEquals(
                    case.elite,
                    deps.cooking.chanceFor(
                        recipe,
                        CookingSourceKind.Hosidius,
                        false,
                        eliteHosidius = true,
                    ),
                )
                assertEquals(listOf(1, 3, 4), recipe.resultCadence())
                assertEquals(rangeSources, recipe.allowedSources)

                val offset = index * 600
                preparePlayer(level = case.level)
                player.inv[4] = InvObj(case.raw)
                player.inv[8] = InvObj(case.raw)
                val range = prepareSource(cooking_locs.range, offset)
                random.next = 99
                random.then = 0
                player.withProtectedAccess {
                    deps.cooking.start(this, range, CookingSourceKind.Range, recipe, 2)
                }
                advance()
                assertEquals(1, player.count(objs.burnt_pie), deps.objs[case.raw].name)
                assertEquals(1, player.count(case.raw), deps.objs[case.raw].name)
                assertEquals(0, cookingXp(), deps.objs[case.raw].name)
                advance(3)
                assertEquals(1, player.count(case.cooked), deps.objs[case.raw].name)
                assertEquals(0, player.count(case.raw), deps.objs[case.raw].name)
                assertEquals((case.xp * 10).toInt(), cookingXp(), deps.objs[case.raw].name)

                preparePlayer(level = case.level - 1)
                player.setCurrentLevel(stats.cooking, case.level)
                add(case.raw)
                val boostedRange = prepareSource(cooking_locs.range, offset + 20)
                random.next = 0
                useOnSource(deps, boostedRange, slot = 0, case.raw)
                advance()
                assertEquals(1, player.count(case.cooked), deps.objs[case.raw].name)

                preparePlayer(level = case.level - 1)
                add(case.raw)
                val lowRange = prepareSource(cooking_locs.range, offset + 40)
                useOnSource(deps, lowRange, slot = 0, case.raw)
                assertEquals(1, player.count(case.raw), deps.objs[case.raw].name)
                assertMessageSent("You need a Cooking level of ${case.level} to cook this food.")

                preparePlayer(level = case.level, members = false)
                add(case.raw)
                val freeRange = prepareSource(cooking_locs.range, offset + 60)
                if (!case.members) {
                    random.next = 0
                }
                useOnSource(deps, freeRange, slot = 0, case.raw)
                if (case.members) {
                    assertEquals(1, player.count(case.raw), deps.objs[case.raw].name)
                    assertMessageSent("You need to be on a members' world to cook this food.")
                } else {
                    advance()
                    assertEquals(1, player.count(case.cooked), deps.objs[case.raw].name)
                }

                preparePlayer(level = 99)
                add(case.raw)
                val fire = prepareSource(cooking_locs.fire, offset + 80)
                useOnSource(deps, fire, slot = 0, case.raw)
                assertEquals(1, player.count(case.raw), deps.objs[case.raw].name)
                assertMessageSent("Nothing interesting happens.")

                val rangeBoundary = case.rangeNoBurnLevel
                preparePlayer(level = rangeBoundary ?: 99)
                add(case.raw)
                val boundaryRange = prepareSource(cooking_locs.range, offset + 100)
                random.next = 99
                useOnSource(deps, boundaryRange, slot = 0, case.raw)
                advance()
                if (rangeBoundary == null) {
                    assertEquals(1, player.count(objs.burnt_pie), deps.objs[case.raw].name)
                } else {
                    assertEquals(1, player.count(case.cooked), deps.objs[case.raw].name)
                }

                val hosidiusBoundary = case.hosidiusNoBurnLevel
                preparePlayer(level = hosidiusBoundary ?: 99)
                player.setVarBit(varbits.kourend_diary_easy_complete, 1)
                add(case.raw)
                val hosidiusRange = prepareSource(cooking_locs.hosidiusRange, offset + 120)
                random.next = 99
                useOnSource(deps, hosidiusRange, slot = 0, case.raw)
                advance()
                if (hosidiusBoundary == null) {
                    assertEquals(1, player.count(objs.burnt_pie), deps.objs[case.raw].name)
                } else {
                    assertEquals(1, player.count(case.cooked), deps.objs[case.raw].name)
                }

                case.lumbridgeNoBurnLevel?.let { level ->
                    preparePlayer(level)
                    player.setVarp(varps.cookquest, 2)
                    add(case.raw)
                    val lumbridgeRange = prepareSource(cooking_locs.lumbridgeRange, offset + 140)
                    random.next = 99
                    useOnSource(deps, lumbridgeRange, slot = 0, case.raw)
                    advance()
                    assertEquals(1, player.count(case.cooked), deps.objs[case.raw].name)
                }
            }

            preparePlayer(level = 92)
            player.setVarBit(varbits.kourend_diary_easy_complete, 1)
            player.setVarBit(varbits.kourend_diary_elite_complete, 1)
            add(objs.uncooked_dragonfruit_pie)
            val eliteRange = prepareSource(cooking_locs.hosidiusRange, offset = 7300)
            random.next = 99
            useOnSource(deps, eliteRange, slot = 0, objs.uncooked_dragonfruit_pie)
            advance()
            assertEquals(1, player.count(objs.dragonfruit_pie))

            for ((index, raw) in
                listOf(objs.uncooked_wild_pie, objs.uncooked_summer_pie).withIndex()) {
                preparePlayer(level = 99)
                add(raw)
                player.worn[1] = InvObj(objs.cooking_skillcape)
                val capeRange = prepareSource(cooking_locs.range, offset = 7400 + index * 20)
                useOnSource(deps, capeRange, slot = 0, raw)
                advance()
                assertEquals(0, player.count(objs.burnt_pie))
                assertEquals(0, player.count(raw))
            }

            preparePlayer(level = 30, members = false)
            player.inv[9] = InvObj(objs.uncooked_apple_pie)
            repeat(27) { add(objs.logs) }
            val fullRange = prepareSource(cooking_locs.range, offset = 7500)
            random.next = 0
            useOnSource(deps, fullRange, slot = 9, objs.uncooked_apple_pie)
            advance()
            assertEquals(1, player.count(objs.apple_pie))
            assertEquals(27, player.count(objs.logs))
            assertEquals(1300, cookingXp())
        }

    @Test
    fun GameTestState.`remaining baked goods pin tins curves cadence and inventory capacity`() =
        runCookingTest { deps ->
            assertEquals(1887, objs.cake_tin.id)
            assertEquals(1889, objs.uncooked_cake.id)
            assertEquals(1891, objs.cake.id)
            assertEquals(1903, objs.burnt_cake.id)
            assertEquals(2287, objs.uncooked_pizza.id)
            assertEquals(2289, objs.plain_pizza.id)
            assertEquals(2305, objs.burnt_pizza.id)

            val cake = StandardCookingRecipes.byRaw.getValue(objs.uncooked_cake.id)
            val pizza = StandardCookingRecipes.byRaw.getValue(objs.uncooked_pizza.id)
            val rangeSources =
                setOf(
                    CookingSourceKind.Range,
                    CookingSourceKind.Lumbridge,
                    CookingSourceKind.Hosidius,
                )
            assertEquals(40, cake.level)
            assertEquals(180.0, cake.xp)
            assertFalse(cake.members)
            assertEquals(objs.cake_tin, cake.returnedContainer)
            assertEquals(CookingChance(38, 332), cake.range)
            assertEquals(CookingChance(50, 344), cake.hosidius)
            assertEquals(CookingChance(63, 357), cake.hosidiusElite)
            assertEquals(35, pizza.level)
            assertEquals(143.0, pizza.xp)
            assertFalse(pizza.members)
            assertEquals(null, pizza.returnedContainer)
            assertEquals(CookingChance(48, 352), pizza.range)
            assertEquals(CookingChance(60, 364), pizza.hosidius)
            assertEquals(CookingChance(73, 377), pizza.hosidiusElite)
            for (recipe in listOf(cake, pizza)) {
                assertEquals(listOf(1, 3, 4), recipe.resultCadence())
                assertEquals(rangeSources, recipe.allowedSources)
                assertEquals(
                    recipe.range,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Lumbridge, false),
                )
            }

            preparePlayer(level = 40, members = false)
            player.inv[4] = InvObj(objs.uncooked_cake)
            player.inv[8] = InvObj(objs.uncooked_cake)
            val cakeRange = prepareSource(cooking_locs.range)
            random.next = 99
            random.then = 0
            player.withProtectedAccess {
                deps.cooking.start(this, cakeRange, CookingSourceKind.Range, cake, 2)
            }
            advance()
            assertEquals(1, player.count(objs.burnt_cake))
            assertEquals(1, player.count(objs.cake_tin))
            assertEquals(1, player.count(objs.uncooked_cake))
            assertEquals(0, cookingXp())
            advance(3)
            assertEquals(1, player.count(objs.cake))
            assertEquals(2, player.count(objs.cake_tin))
            assertEquals(0, player.count(objs.uncooked_cake))
            assertEquals(1800, cookingXp())

            preparePlayer(level = 35, members = false)
            player.inv[4] = InvObj(objs.uncooked_pizza)
            player.inv[8] = InvObj(objs.uncooked_pizza)
            val pizzaRange = prepareSource(cooking_locs.range, offset = 20)
            random.next = 99
            random.then = 0
            player.withProtectedAccess {
                deps.cooking.start(this, pizzaRange, CookingSourceKind.Range, pizza, 2)
            }
            advance()
            assertEquals(1, player.count(objs.burnt_pizza))
            assertEquals(1, player.count(objs.uncooked_pizza))
            assertEquals(0, cookingXp())
            advance(3)
            assertEquals(1, player.count(objs.plain_pizza))
            assertEquals(0, player.count(objs.uncooked_pizza))
            assertEquals(1430, cookingXp())

            for ((index, recipe) in listOf(pizza, cake).withIndex()) {
                preparePlayer(level = recipe.level - 1, members = false)
                player.setCurrentLevel(stats.cooking, recipe.level)
                add(recipe.raw)
                val boostedRange = prepareSource(cooking_locs.range, offset = 40 + index * 20)
                random.next = 0
                useOnSource(deps, boostedRange, slot = 0, recipe.raw)
                advance()
                assertEquals(1, player.count(recipe.cooked))

                preparePlayer(level = recipe.level - 1, members = false)
                add(recipe.raw)
                val lowRange = prepareSource(cooking_locs.range, offset = 80 + index * 20)
                useOnSource(deps, lowRange, slot = 0, recipe.raw)
                assertEquals(1, player.count(recipe.raw))
                assertMessageSent("You need a Cooking level of ${recipe.level} to cook this food.")

                preparePlayer(level = 99, members = false)
                add(recipe.raw)
                val fire = prepareSource(cooking_locs.fire, offset = 120 + index * 20)
                useOnSource(deps, fire, slot = 0, recipe.raw)
                assertEquals(1, player.count(recipe.raw))
                assertMessageSent("Nothing interesting happens.")
            }

            for ((index, case) in
                listOf(
                        NoBurnCase(pizza, rangeLevel = 68, hosidiusLevel = 64),
                        NoBurnCase(cake, rangeLevel = 74, hosidiusLevel = 70),
                    )
                    .withIndex()) {
                preparePlayer(level = case.rangeLevel, members = false)
                add(case.recipe.raw)
                val range = prepareSource(cooking_locs.range, offset = 160 + index * 80)
                random.next = 99
                useOnSource(deps, range, slot = 0, case.recipe.raw)
                advance()
                assertEquals(1, player.count(case.recipe.cooked))

                preparePlayer(level = case.rangeLevel - 1, members = false)
                add(case.recipe.raw)
                val belowRange = prepareSource(cooking_locs.range, offset = 180 + index * 80)
                random.next = 100
                useOnSource(deps, belowRange, slot = 0, case.recipe.raw)
                advance()
                assertEquals(1, player.count(requireNotNull(case.recipe.burnt)))

                preparePlayer(level = case.hosidiusLevel, members = false)
                player.setVarBit(varbits.kourend_diary_easy_complete, 1)
                add(case.recipe.raw)
                val hosidius = prepareSource(cooking_locs.hosidiusRange, offset = 200 + index * 80)
                random.next = 99
                useOnSource(deps, hosidius, slot = 0, case.recipe.raw)
                advance()
                assertEquals(1, player.count(case.recipe.cooked))

                preparePlayer(level = case.hosidiusLevel - 1, members = false)
                player.setVarBit(varbits.kourend_diary_easy_complete, 1)
                add(case.recipe.raw)
                val belowHosidius =
                    prepareSource(cooking_locs.hosidiusRange, offset = 220 + index * 80)
                random.next = 100
                useOnSource(deps, belowHosidius, slot = 0, case.recipe.raw)
                advance()
                assertEquals(1, player.count(requireNotNull(case.recipe.burnt)))
            }

            preparePlayer(level = 40, members = false)
            player.inv[9] = InvObj(objs.uncooked_cake)
            repeat(27) { add(objs.logs) }
            val fullCakeRange = prepareSource(cooking_locs.range, offset = 340)
            useOnSource(deps, fullCakeRange, slot = 9, objs.uncooked_cake)
            assertEquals(1, player.count(objs.uncooked_cake))
            assertEquals(0, player.count(objs.cake_tin))
            assertMessageSent("You don't have enough inventory space.")

            preparePlayer(level = 40, members = false)
            player.inv[4] = InvObj(objs.uncooked_cake)
            player.inv[8] = InvObj(objs.uncooked_cake)
            repeat(25) { add(objs.logs) }
            val oneSlotCakeRange = prepareSource(cooking_locs.range, offset = 360)
            random.next = 0
            player.withProtectedAccess {
                deps.cooking.start(this, oneSlotCakeRange, CookingSourceKind.Range, cake, 2)
            }
            advance()
            assertEquals(1, player.count(objs.cake))
            assertEquals(1, player.count(objs.cake_tin))
            assertEquals(1, player.count(objs.uncooked_cake))
            assertFalse(cooking_queues.standardFood in player.weakQueueList)

            preparePlayer(level = 35, members = false)
            player.inv[9] = InvObj(objs.uncooked_pizza)
            repeat(27) { add(objs.logs) }
            val fullPizzaRange = prepareSource(cooking_locs.range, offset = 380)
            random.next = 0
            useOnSource(deps, fullPizzaRange, slot = 9, objs.uncooked_pizza)
            advance()
            assertEquals(1, player.count(objs.plain_pizza))
            assertEquals(27, player.count(objs.logs))

            preparePlayer(level = 40, members = false)
            add(objs.uncooked_cake)
            player.worn[1] = InvObj(objs.cooking_skillcape)
            val capeRange = prepareSource(cooking_locs.range, offset = 400)
            random.next = 99
            useOnSource(deps, capeRange, slot = 0, objs.uncooked_cake)
            advance()
            assertEquals(1, player.count(objs.cake))
            assertEquals(1, player.count(objs.cake_tin))
        }

    @Test
    fun GameTestState.`stew cooks on fires and ranges with exact curve cadence and boost`() =
        runCookingTest { deps ->
            assertEquals(2001, objs.uncooked_stew.id)
            assertEquals(2003, objs.stew.id)
            assertEquals(2005, objs.burnt_stew.id)
            val recipe = StandardCookingRecipes.byRaw.getValue(objs.uncooked_stew.id)
            assertEquals(25, recipe.level)
            assertEquals(117.0, recipe.xp)
            assertFalse(recipe.members)
            assertEquals(CookingChance(68, 392), recipe.fire)
            assertEquals(CookingChance(68, 392), recipe.range)
            assertEquals(CookingChance(78, 412), recipe.lumbridge)
            assertEquals(CookingChance(80, 404), recipe.hosidius)
            assertEquals(CookingChance(93, 417), recipe.hosidiusElite)
            assertEquals(listOf(1, 3, 4), recipe.resultCadence())
            assertEquals(CookingSourceKind.entries.toSet(), recipe.allowedSources)

            preparePlayer(level = 25, members = false)
            player.inv[4] = InvObj(objs.uncooked_stew)
            player.inv[8] = InvObj(objs.uncooked_stew)
            val fire = prepareSource(cooking_locs.fire)
            random.next = 99
            random.then = 0
            player.withProtectedAccess {
                deps.cooking.start(this, fire, CookingSourceKind.Fire, recipe, 2)
            }
            advance()
            assertEquals(1, player.count(objs.burnt_stew))
            assertEquals(1, player.count(objs.uncooked_stew))
            assertEquals(0, cookingXp())
            advance(3)
            assertEquals(1, player.count(objs.stew))
            assertEquals(0, player.count(objs.uncooked_stew))
            assertEquals(1170, cookingXp())

            preparePlayer(level = 24, members = false)
            player.setCurrentLevel(stats.cooking, 25)
            player.inv[9] = InvObj(objs.uncooked_stew)
            repeat(27) { add(objs.logs) }
            val range = prepareSource(cooking_locs.range, offset = 20)
            random.next = 0
            useOnSource(deps, range, slot = 9, objs.uncooked_stew)
            advance()
            assertEquals(1, player.count(objs.stew))
            assertEquals(27, player.count(objs.logs))
            assertEquals(1170, cookingXp())

            preparePlayer(level = 24, members = false)
            add(objs.uncooked_stew)
            val lowRange = prepareSource(cooking_locs.range, offset = 40)
            useOnSource(deps, lowRange, slot = 0, objs.uncooked_stew)
            assertEquals(1, player.count(objs.uncooked_stew))
            assertMessageSent("You need a Cooking level of 25 to cook this food.")

            preparePlayer(level = 58, members = false)
            add(objs.uncooked_stew)
            val noBurnFire = prepareSource(cooking_locs.fire, offset = 60)
            random.next = 99
            useOnSource(deps, noBurnFire, slot = 0, objs.uncooked_stew)
            advance()
            assertEquals(1, player.count(objs.stew))
            assertEquals(1170, cookingXp())

            preparePlayer(level = 25, members = false)
            add(objs.uncooked_stew)
            player.worn[1] = InvObj(objs.max_cape_worn)
            val capeFire = prepareSource(cooking_locs.fire, offset = 80)
            random.next = 99
            useOnSource(deps, capeFire, slot = 0, objs.uncooked_stew)
            advance()
            assertEquals(1, player.count(objs.stew))
            assertEquals(1170, cookingXp())
        }

    @Test
    fun GameTestState.`curry cooks on fires and ranges with exact curve cadence and gates`() =
        runCookingTest { deps ->
            assertEquals(2009, objs.uncooked_curry.id)
            assertEquals(2011, objs.curry.id)
            assertEquals(2013, objs.burnt_curry.id)
            val recipe = StandardCookingRecipes.byRaw.getValue(objs.uncooked_curry.id)
            assertEquals(60, recipe.level)
            assertEquals(280.0, recipe.xp)
            assertTrue(recipe.members)
            assertEquals(CookingChance(38, 332), recipe.fire)
            assertEquals(CookingChance(38, 332), recipe.range)
            assertEquals(null, recipe.lumbridge)
            assertEquals(CookingChance(50, 344), recipe.hosidius)
            assertEquals(CookingChance(63, 357), recipe.hosidiusElite)
            assertEquals(listOf(1, 3, 4), recipe.resultCadence())
            assertEquals(CookingSourceKind.entries.toSet(), recipe.allowedSources)

            preparePlayer(level = 60)
            player.inv[4] = InvObj(objs.uncooked_curry)
            player.inv[8] = InvObj(objs.uncooked_curry)
            val fire = prepareSource(cooking_locs.fire)
            random.next = 99
            random.then = 0
            player.withProtectedAccess {
                deps.cooking.start(this, fire, CookingSourceKind.Fire, recipe, 2)
            }
            advance()
            assertEquals(1, player.count(objs.burnt_curry))
            assertEquals(1, player.count(objs.uncooked_curry))
            assertEquals(0, cookingXp())
            advance(3)
            assertEquals(1, player.count(objs.curry))
            assertEquals(0, player.count(objs.uncooked_curry))
            assertEquals(2800, cookingXp())

            preparePlayer(level = 59)
            player.setCurrentLevel(stats.cooking, 60)
            player.inv[9] = InvObj(objs.uncooked_curry)
            repeat(27) { add(objs.logs) }
            val range = prepareSource(cooking_locs.range, offset = 20)
            random.next = 0
            useOnSource(deps, range, slot = 9, objs.uncooked_curry)
            advance()
            assertEquals(1, player.count(objs.curry))
            assertEquals(27, player.count(objs.logs))
            assertEquals(2800, cookingXp())

            preparePlayer(level = 59)
            add(objs.uncooked_curry)
            val lowRange = prepareSource(cooking_locs.range, offset = 40)
            useOnSource(deps, lowRange, slot = 0, objs.uncooked_curry)
            assertEquals(1, player.count(objs.uncooked_curry))
            assertMessageSent("You need a Cooking level of 60 to cook this food.")

            preparePlayer(level = 60, members = false)
            add(objs.uncooked_curry)
            val freeRange = prepareSource(cooking_locs.range, offset = 60)
            useOnSource(deps, freeRange, slot = 0, objs.uncooked_curry)
            assertEquals(1, player.count(objs.uncooked_curry))
            assertMessageSent("You need to be on a members' world to cook this food.")

            preparePlayer(level = 74)
            add(objs.uncooked_curry)
            val noBurnFire = prepareSource(cooking_locs.fire, offset = 80)
            random.next = 99
            useOnSource(deps, noBurnFire, slot = 0, objs.uncooked_curry)
            advance()
            assertEquals(1, player.count(objs.curry))
            assertEquals(2800, cookingXp())
        }

    @Test
    fun GameTestState.`potatoes bake only on ranges with exact curve cadence and gates`() =
        runCookingTest { deps ->
            assertEquals(1942, objs.potato.id)
            assertEquals(6701, objs.baked_potato.id)
            assertEquals(6699, objs.burnt_potato.id)
            val recipe = StandardCookingRecipes.byRaw.getValue(objs.potato.id)
            val rangeSources =
                setOf(
                    CookingSourceKind.Range,
                    CookingSourceKind.Lumbridge,
                    CookingSourceKind.Hosidius,
                )
            assertEquals(7, recipe.level)
            assertEquals(15.0, recipe.xp)
            assertTrue(recipe.members)
            assertFalse(deps.objs[objs.potato].members)
            assertEquals(CookingChance(108, 472), recipe.range)
            assertEquals(null, recipe.lumbridge)
            assertEquals(CookingChance(120, 484), recipe.hosidius)
            assertEquals(CookingChance(133, 497), recipe.hosidiusElite)
            assertEquals(listOf(1, 3, 4), recipe.resultCadence())
            assertEquals(rangeSources, recipe.allowedSources)

            preparePlayer(level = 7)
            player.inv[4] = InvObj(objs.potato)
            player.inv[8] = InvObj(objs.potato)
            val range = prepareSource(cooking_locs.range)
            random.next = 99
            random.then = 0
            player.withProtectedAccess {
                deps.cooking.start(this, range, CookingSourceKind.Range, recipe, 2)
            }
            advance()
            assertEquals(1, player.count(objs.burnt_potato))
            assertEquals(1, player.count(objs.potato))
            assertEquals(0, cookingXp())
            advance(3)
            assertEquals(1, player.count(objs.baked_potato))
            assertEquals(0, player.count(objs.potato))
            assertEquals(150, cookingXp())

            preparePlayer(level = 6)
            player.setCurrentLevel(stats.cooking, 7)
            player.inv[9] = InvObj(objs.potato)
            repeat(27) { add(objs.logs) }
            val boostedRange = prepareSource(cooking_locs.range, offset = 20)
            random.next = 0
            useOnSource(deps, boostedRange, slot = 9, objs.potato)
            advance()
            assertEquals(1, player.count(objs.baked_potato))
            assertEquals(27, player.count(objs.logs))
            assertEquals(150, cookingXp())

            preparePlayer(level = 6)
            add(objs.potato)
            val lowRange = prepareSource(cooking_locs.range, offset = 40)
            useOnSource(deps, lowRange, slot = 0, objs.potato)
            assertEquals(1, player.count(objs.potato))
            assertMessageSent("You need a Cooking level of 7 to cook this food.")

            preparePlayer(level = 7, members = false)
            add(objs.potato)
            val freeRange = prepareSource(cooking_locs.range, offset = 60)
            useOnSource(deps, freeRange, slot = 0, objs.potato)
            assertEquals(1, player.count(objs.potato))
            assertMessageSent("You need to be on a members' world to cook this food.")

            preparePlayer(level = 41)
            add(objs.potato)
            val noBurnRange = prepareSource(cooking_locs.range, offset = 80)
            random.next = 99
            useOnSource(deps, noBurnRange, slot = 0, objs.potato)
            advance()
            assertEquals(1, player.count(objs.baked_potato))
            assertEquals(150, cookingXp())

            preparePlayer(level = 99)
            add(objs.potato)
            val fire = prepareSource(cooking_locs.fire, offset = 100)
            useOnSource(deps, fire, slot = 0, objs.potato)
            assertEquals(1, player.count(objs.potato))
            assertMessageSent("Nothing interesting happens.")
        }

    @Test
    fun GameTestState.`ordinary vegetables pin curves cadence boosts membership and burns`() =
        runCookingTest { deps ->
            val cases =
                listOf(
                    VegetableHeatCase(
                        objs.uncooked_egg,
                        objs.scrambled_egg,
                        objs.burnt_egg,
                        level = 13,
                        xp = 50.0,
                        noBurnLevel = 48,
                        ordinary = CookingChance(90, 438),
                        hosidius = CookingChance(102, 450),
                        elite = CookingChance(115, 463),
                        firstTicks = 0,
                    ),
                    VegetableHeatCase(
                        objs.sweetcorn,
                        objs.cooked_sweetcorn,
                        objs.burnt_sweetcorn,
                        level = 28,
                        xp = 104.0,
                        noBurnLevel = 54,
                        ordinary = CookingChance(78, 412),
                        hosidius = CookingChance(90, 424),
                        elite = CookingChance(103, 437),
                    ),
                    VegetableHeatCase(
                        objs.chopped_onion,
                        objs.fried_onions,
                        objs.burnt_onion,
                        level = 42,
                        xp = 60.0,
                        noBurnLevel = 77,
                        ordinary = CookingChance(36, 322),
                        hosidius = CookingChance(48, 334),
                        elite = CookingChance(61, 347),
                    ),
                    VegetableHeatCase(
                        objs.sliced_mushrooms,
                        objs.fried_mushrooms,
                        objs.burnt_mushroom,
                        level = 46,
                        xp = 60.0,
                        noBurnLevel = 90,
                        ordinary = CookingChance(16, 282),
                        hosidius = CookingChance(28, 294),
                        elite = CookingChance(41, 307),
                    ),
                )
            assertEquals(
                listOf(7076, 7078, 7090, 5986, 5988, 5990, 1871, 7084, 7092, 7080, 7082, 7094),
                cases.flatMap { listOf(it.raw.id, it.cooked.id, it.burnt.id) },
            )

            for ((index, case) in cases.withIndex()) {
                val recipe = StandardCookingRecipes.byRaw.getValue(case.raw.id)
                assertEquals(case.level, recipe.level)
                assertEquals(case.xp, recipe.xp)
                assertTrue(recipe.members)
                assertEquals(case.ordinary, recipe.fire)
                assertEquals(case.ordinary, recipe.range)
                assertEquals(null, recipe.lumbridge)
                assertEquals(case.hosidius, recipe.hosidius)
                assertEquals(case.elite, recipe.hosidiusElite)
                assertEquals(listOf(case.firstTicks, 3, 4), recipe.resultCadence())
                assertEquals(CookingSourceKind.entries.toSet(), recipe.allowedSources)

                preparePlayer(level = case.level)
                player.inv[4] = InvObj(case.raw)
                player.inv[8] = InvObj(case.raw)
                val fire = prepareSource(cooking_locs.fire, offset = index * 140)
                random.next = 99
                random.then = 0
                player.withProtectedAccess {
                    deps.cooking.start(this, fire, CookingSourceKind.Fire, recipe, 2)
                }
                advance(case.firstTicks)
                assertEquals(1, player.count(case.burnt), deps.objs[case.raw].name)
                assertEquals(1, player.count(case.raw), deps.objs[case.raw].name)
                assertEquals(0, cookingXp(), deps.objs[case.raw].name)
                advance(3)
                assertEquals(1, player.count(case.cooked), deps.objs[case.raw].name)
                assertEquals(0, player.count(case.raw), deps.objs[case.raw].name)
                assertEquals((case.xp * 10).toInt(), cookingXp(), deps.objs[case.raw].name)

                preparePlayer(level = case.level - 1)
                player.setCurrentLevel(stats.cooking, case.level)
                add(case.raw)
                val boostedRange = prepareSource(cooking_locs.range, offset = index * 140 + 20)
                random.next = 0
                useOnSource(deps, boostedRange, slot = 0, case.raw)
                advance(case.firstTicks)
                assertEquals(1, player.count(case.cooked), deps.objs[case.raw].name)

                preparePlayer(level = case.level - 1)
                add(case.raw)
                val lowRange = prepareSource(cooking_locs.range, offset = index * 140 + 40)
                useOnSource(deps, lowRange, slot = 0, case.raw)
                assertEquals(1, player.count(case.raw), deps.objs[case.raw].name)
                assertMessageSent("You need a Cooking level of ${case.level} to cook this food.")

                preparePlayer(level = case.level, members = false)
                add(case.raw)
                val freeRange = prepareSource(cooking_locs.range, offset = index * 140 + 60)
                useOnSource(deps, freeRange, slot = 0, case.raw)
                assertEquals(1, player.count(case.raw), deps.objs[case.raw].name)
                assertMessageSent("You need to be on a members' world to cook this food.")

                preparePlayer(level = case.noBurnLevel)
                add(case.raw)
                val noBurnFire = prepareSource(cooking_locs.fire, offset = index * 140 + 80)
                random.next = 99
                useOnSource(deps, noBurnFire, slot = 0, case.raw)
                advance(case.firstTicks)
                assertEquals(1, player.count(case.cooked), deps.objs[case.raw].name)
            }

            preparePlayer(level = 28)
            player.inv[9] = InvObj(objs.sweetcorn)
            repeat(27) { add(objs.logs) }
            val fullRange = prepareSource(cooking_locs.range, offset = 700)
            random.next = 0
            useOnSource(deps, fullRange, slot = 9, objs.sweetcorn)
            advance()
            assertEquals(1, player.count(objs.cooked_sweetcorn))
            assertEquals(27, player.count(objs.logs))
            assertEquals(1040, cookingXp())
        }

    @Test
    fun GameTestState.`cache 233 pins the chompy iron spit fire contract and dispatch`() =
        runCookingTest { deps ->
            assertEquals(2876, objs.raw_chompy.id)
            assertEquals(2878, objs.cooked_chompy.id)
            assertEquals(2880, objs.ruined_chompy.id)
            assertEquals(7225, objs.iron_spit.id)
            assertEquals(7230, objs.skewered_chompy.id)
            assertEquals(1016, seqs.spit_fire.id)
            assertEquals(30, ChompyFireCooking.COOKING_LEVEL)
            assertEquals(20, ChompyFireCooking.FIREMAKING_LEVEL)
            assertEquals(100.0, ChompyFireCooking.COOKING_XP)
            assertEquals(3, ChompyFireCooking.FIRST_RESULT_TICKS)
            assertEquals(CookingChance(200, 255), ChompyFireCooking.CHANCE)
            assertTrue(deps.objs[objs.raw_chompy].members)
            assertTrue(deps.objs[objs.skewered_chompy].members)
            assertNotNull(deps.seqs[seqs.spit_fire])
            assertTrue(
                deps.events.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.raw_chompy.id, objs.iron_spit.id),
                )
            )
            for (source in listOf(cooking_locs.fire, cooking_locs.permanentCookFire)) {
                assertTrue(
                    deps.events.contains(
                        LocUEvents.Op::class.java,
                        EventBus.composeLongKey(source.id, objs.skewered_chompy.id),
                    )
                )
            }
        }

    @Test
    fun GameTestState.`chompy skewering is immediate boostable symmetric and awards no xp`() =
        runCookingTest { deps ->
            repeat(2) { reverse ->
                preparePlayer(level = 1)
                player.setBaseLevel(stats.firemaking, 1)
                player.setCurrentLevel(stats.firemaking, 20)
                player.setCurrentLevel(stats.cooking, 30)
                player.inv[3] = InvObj(objs.raw_chompy)
                player.inv[8] = InvObj(objs.iron_spit)

                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.raw_chompy],
                            3,
                            deps.objs[objs.iron_spit],
                            8,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.iron_spit],
                            8,
                            deps.objs[objs.raw_chompy],
                            3,
                        )
                    }
                }

                assertEquals(0, player.count(objs.raw_chompy))
                assertEquals(0, player.count(objs.iron_spit))
                assertEquals(1, player.count(objs.skewered_chompy))
                assertEquals(0, cookingXp())
                assertEquals(0, player.statMap.getFineXP(stats.firemaking))
            }
        }

    @Test
    fun GameTestState.`skewered chompy cooks or burns after three ticks and returns the spit`() =
        runCookingTest { deps ->
            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 20)
            add(objs.skewered_chompy)
            val fire = prepareSource(cooking_locs.fire)
            random.next = 0

            useOnSource(deps, fire, slot = 0, objs.skewered_chompy)

            assertEquals(seqs.spit_fire.id, player.pendingSequence.id)
            advance(2)
            assertEquals(1, player.count(objs.skewered_chompy))
            assertEquals(0, player.count(objs.cooked_chompy))
            advance()
            assertEquals(0, player.count(objs.skewered_chompy))
            assertEquals(1, player.count(objs.cooked_chompy))
            assertEquals(1, player.count(objs.iron_spit))
            assertEquals(1000, cookingXp())
            assertMessageSent("You successfully cook the chompy.")
            assertEquals(0, player.weakQueueList.count(cooking_queues.chompyFire))

            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 20)
            add(objs.skewered_chompy)
            val burnFire = prepareSource(cooking_locs.permanentCookFire, offset = 20)
            random.next = 99
            useOnSource(deps, burnFire, slot = 0, objs.skewered_chompy)
            advance(3)

            assertEquals(0, player.count(objs.skewered_chompy))
            assertEquals(1, player.count(objs.ruined_chompy))
            assertEquals(1, player.count(objs.iron_spit))
            assertEquals(0, cookingXp())
            assertMessageSent("You accidentally burn the chompy.")
        }

    @Test
    fun GameTestState.`chompy gates full inventory cape and interruption are safe`() =
        runCookingTest { deps ->
            preparePlayer(level = 30, members = false)
            player.setCurrentLevel(stats.firemaking, 20)
            player.inv[0] = InvObj(objs.raw_chompy)
            player.inv[1] = InvObj(objs.iron_spit)
            player.withProtectedAccess { deps.chompy.skewer(this, 0, 1) }
            assertEquals(1, player.count(objs.raw_chompy))
            assertMessageSent("You need to be on a members' world to cook this food.")

            preparePlayer(level = 29)
            player.setCurrentLevel(stats.firemaking, 20)
            player.inv[0] = InvObj(objs.raw_chompy)
            player.inv[1] = InvObj(objs.iron_spit)
            player.withProtectedAccess { deps.chompy.skewer(this, 0, 1) }
            assertEquals(1, player.count(objs.raw_chompy))
            assertMessageSent("You need a Cooking level of 30 to skewer this chompy.")

            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 19)
            player.inv[0] = InvObj(objs.raw_chompy)
            player.inv[1] = InvObj(objs.iron_spit)
            player.withProtectedAccess { deps.chompy.skewer(this, 0, 1) }
            assertEquals(1, player.count(objs.raw_chompy))
            assertMessageSent("You need a Firemaking level of 20 to use an iron spit.")

            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 19)
            add(objs.skewered_chompy)
            val lowFiremakingFire = prepareSource(cooking_locs.fire, offset = 20)
            player.withProtectedAccess { deps.chompy.start(this, lowFiremakingFire, inputSlot = 0) }
            assertEquals(1, player.count(objs.skewered_chompy))
            assertFalse(cooking_queues.chompyFire in player.weakQueueList)
            assertMessageSent("You need a Firemaking level of 20 to use an iron spit.")

            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 20)
            add(objs.skewered_chompy)
            repeat(27) { add(objs.logs) }
            val fullFire = prepareSource(cooking_locs.fire, offset = 40)
            player.withProtectedAccess { deps.chompy.start(this, fullFire, inputSlot = 0) }
            assertEquals(1, player.count(objs.skewered_chompy))
            assertFalse(cooking_queues.chompyFire in player.weakQueueList)
            assertMessageSent("You don't have enough inventory space.")

            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 20)
            add(objs.skewered_chompy)
            player.worn[1] = InvObj(objs.cooking_skillcape)
            val capeFire = prepareSource(cooking_locs.fire, offset = 60)
            random.next = 99
            player.withProtectedAccess { deps.chompy.start(this, capeFire, inputSlot = 0) }
            advance(3)
            assertEquals(1, player.count(objs.cooked_chompy))
            assertEquals(1000, cookingXp())

            preparePlayer(level = 99)
            player.setCurrentLevel(stats.firemaking, 20)
            add(objs.skewered_chompy)
            val level99Fire = prepareSource(cooking_locs.fire, offset = 70)
            random.next = 99
            player.withProtectedAccess { deps.chompy.start(this, level99Fire, inputSlot = 0) }
            advance(3)
            assertEquals(1, player.count(objs.cooked_chompy))
            assertEquals(1000, cookingXp())

            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 20)
            add(objs.skewered_chompy)
            val movingFire = prepareSource(cooking_locs.fire, offset = 80)
            player.withProtectedAccess { deps.chompy.start(this, movingFire, inputSlot = 0) }
            player.placeAt(movingFire.coords.translateX(5))
            advance(3)
            assertEquals(1, player.count(objs.skewered_chompy))
            assertEquals(0, player.count(objs.cooked_chompy))
            assertFalse(cooking_queues.chompyFire in player.weakQueueList)

            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 20)
            player.inv[4] = InvObj(objs.skewered_chompy)
            player.inv[5] = InvObj(objs.skewered_chompy)
            val staleFire = prepareSource(cooking_locs.fire, offset = 100)
            player.withProtectedAccess { deps.chompy.start(this, staleFire, inputSlot = 4) }
            player.inv[4] = InvObj(objs.logs)
            advance(3)
            assertEquals(1, player.count(objs.skewered_chompy))
            assertEquals(1, player.count(objs.logs))
            assertEquals(0, player.count(objs.cooked_chompy))
            assertEquals(0, cookingXp())
        }

    @Test
    fun GameTestState.`cache 233 pins ordinary iron spit recipes and dispatch`() =
        runCookingTest { deps ->
            assertEquals(7222, objs.burnt_rabbit_spit.id)
            assertEquals(7223, objs.roast_rabbit.id)
            assertEquals(7224, objs.skewered_rabbit.id)
            assertEquals(7225, objs.iron_spit.id)
            assertEquals(9978, objs.raw_bird_meat.id)
            assertEquals(9980, objs.roast_bird_meat.id)
            assertEquals(9982, objs.burnt_bird_meat.id)
            assertEquals(9984, objs.skewered_bird_meat.id)
            assertEquals(9986, objs.raw_beast_meat.id)
            assertEquals(9988, objs.roast_beast_meat.id)
            assertEquals(9990, objs.burnt_beast_meat.id)
            assertEquals(9992, objs.skewered_beast_meat.id)
            assertEquals(1016, seqs.spit_fire.id)
            assertEquals(90, cooking_queues.ironSpit.id)
            assertEquals(20, IronSpitCooking.FIREMAKING_LEVEL)
            assertEquals(3, IronSpitCooking.RESULT_TICKS)
            assertEquals(
                listOf(
                    IronSpitRecipe(
                        objs.raw_rabbit,
                        objs.skewered_rabbit,
                        objs.roast_rabbit,
                        objs.burnt_rabbit_spit,
                        16,
                        72.5,
                        CookingChance(160, 255),
                        false,
                        "You roast a rabbit.",
                        "You accidentally burn the rabbit.",
                    ),
                    IronSpitRecipe(
                        objs.raw_bird_meat,
                        objs.skewered_bird_meat,
                        objs.roast_bird_meat,
                        objs.burnt_bird_meat,
                        11,
                        62.5,
                        CookingChance(155, 255),
                        true,
                        "You roast a piece of bird meat.",
                        "You accidentally burn the bird meat.",
                    ),
                    IronSpitRecipe(
                        objs.raw_beast_meat,
                        objs.skewered_beast_meat,
                        objs.roast_beast_meat,
                        objs.burnt_beast_meat,
                        21,
                        82.5,
                        CookingChance(180, 255),
                        false,
                        "You roast a piece of beast meat.",
                        "You accidentally burn the beast meat.",
                    ),
                ),
                IronSpitRecipes.all,
            )
            assertNotNull(deps.seqs[seqs.spit_fire])
            for (recipe in IronSpitRecipes.all) {
                assertTrue(deps.objs[recipe.raw].members)
                assertTrue(deps.objs[recipe.skewered].members)
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(recipe.raw.id, objs.iron_spit.id),
                    )
                )
                for (source in listOf(cooking_locs.fire, cooking_locs.permanentCookFire)) {
                    assertTrue(
                        deps.events.contains(
                            LocUEvents.Op::class.java,
                            EventBus.composeLongKey(source.id, recipe.skewered.id),
                        )
                    )
                }
            }
        }

    @Test
    fun GameTestState
        .`ordinary iron spit skewering is immediate symmetric boostable and zero xp`() =
        runCookingTest { deps ->
            for ((index, recipe) in IronSpitRecipes.all.withIndex()) {
                preparePlayer(level = 1)
                player.setBaseLevel(stats.firemaking, 1)
                player.setCurrentLevel(stats.firemaking, 20)
                player.setCurrentLevel(stats.cooking, recipe.level)
                player.inv[3] = InvObj(recipe.raw)
                player.inv[8] = InvObj(objs.iron_spit)

                useTogether(
                    deps,
                    first = recipe.raw,
                    firstSlot = 3,
                    second = objs.iron_spit,
                    secondSlot = 8,
                    reverse = index % 2 != 0,
                )

                assertEquals(0, player.count(recipe.raw))
                assertEquals(0, player.count(objs.iron_spit))
                assertEquals(1, player.count(recipe.skewered))
                assertEquals(0, cookingXp())
                assertEquals(0, player.statMap.getFineXP(stats.firemaking))
            }

            preparePlayer(level = 10)
            player.setCurrentLevel(stats.firemaking, 20)
            player.inv[0] = InvObj(objs.raw_bird_meat)
            player.inv[1] = InvObj(objs.iron_spit)
            player.withProtectedAccess {
                deps.ironSpit.skewer(this, IronSpitRecipes.bird, rawSlot = 0, spitSlot = 1)
            }
            assertEquals(1, player.count(objs.raw_bird_meat))
            assertMessageSent("You need a Cooking level of 11 to skewer this meat.")

            preparePlayer(level = 21)
            player.setCurrentLevel(stats.firemaking, 19)
            player.inv[0] = InvObj(objs.raw_beast_meat)
            player.inv[1] = InvObj(objs.iron_spit)
            player.withProtectedAccess {
                deps.ironSpit.skewer(this, IronSpitRecipes.beast, rawSlot = 0, spitSlot = 1)
            }
            assertEquals(1, player.count(objs.raw_beast_meat))
            assertMessageSent("You need a Firemaking level of 20 to use an iron spit.")

            preparePlayer(level = 16, members = false)
            player.setCurrentLevel(stats.firemaking, 20)
            player.inv[0] = InvObj(objs.raw_rabbit)
            player.inv[1] = InvObj(objs.iron_spit)
            player.withProtectedAccess {
                deps.ironSpit.skewer(this, IronSpitRecipes.rabbit, rawSlot = 0, spitSlot = 1)
            }
            assertEquals(1, player.count(objs.raw_rabbit))
            assertMessageSent("You need to be on a members' world to use an iron spit.")
        }

    @Test
    fun GameTestState.`ordinary skewered meats roast or burn after three ticks and return spit`() =
        runCookingTest { deps ->
            for ((index, recipe) in IronSpitRecipes.all.withIndex()) {
                preparePlayer(level = recipe.level)
                player.setCurrentLevel(stats.firemaking, 20)
                add(recipe.skewered)
                val source =
                    prepareSource(
                        if (index % 2 == 0) cooking_locs.fire else cooking_locs.permanentCookFire,
                        offset = index * 20,
                    )
                random.next = 0

                useOnSource(deps, source, slot = 0, recipe.skewered)

                assertEquals(seqs.spit_fire.id, player.pendingSequence.id)
                advance(2)
                assertEquals(1, player.count(recipe.skewered))
                advance()
                assertEquals(0, player.count(recipe.skewered))
                assertEquals(1, player.count(recipe.roasted))
                assertEquals(1, player.count(objs.iron_spit))
                assertEquals((recipe.xp * 10).toInt(), cookingXp())
                assertMessageSent(recipe.successMessage)
                assertFalse(cooking_queues.ironSpit in player.weakQueueList)
            }

            preparePlayer(level = IronSpitRecipes.bird.level)
            player.setCurrentLevel(stats.firemaking, 20)
            add(IronSpitRecipes.bird.skewered)
            val burnFire = prepareSource(cooking_locs.fire, offset = 80)
            random.next = 99
            useOnSource(deps, burnFire, slot = 0, IronSpitRecipes.bird.skewered)
            advance(3)
            assertEquals(1, player.count(IronSpitRecipes.bird.burnt))
            assertEquals(1, player.count(objs.iron_spit))
            assertEquals(0, cookingXp())
            assertMessageSent(IronSpitRecipes.bird.burnMessage)
        }

    @Test
    fun GameTestState.`ordinary iron spit roast gates capes slots and interruptions safely`() =
        runCookingTest { deps ->
            preparePlayer(level = IronSpitRecipes.bird.level)
            player.setCurrentLevel(stats.firemaking, 19)
            add(IronSpitRecipes.bird.skewered)
            val birdFire = prepareSource(cooking_locs.fire)
            player.withProtectedAccess {
                deps.ironSpit.start(this, IronSpitRecipes.bird, birdFire, inputSlot = 0)
            }
            assertEquals(1, player.count(IronSpitRecipes.bird.skewered))
            assertFalse(cooking_queues.ironSpit in player.weakQueueList)
            assertMessageSent("You need a Firemaking level of 20 to use an iron spit.")

            for ((index, recipe) in
                listOf(IronSpitRecipes.rabbit, IronSpitRecipes.beast).withIndex()) {
                preparePlayer(level = recipe.level)
                player.setCurrentLevel(stats.firemaking, 1)
                add(recipe.skewered)
                val fire = prepareSource(cooking_locs.fire, offset = 20 + index * 20)
                random.next = 0
                player.withProtectedAccess {
                    deps.ironSpit.start(this, recipe, fire, inputSlot = 0)
                }
                advance(3)
                assertEquals(1, player.count(recipe.roasted))
            }

            preparePlayer(level = IronSpitRecipes.rabbit.level)
            add(IronSpitRecipes.rabbit.skewered)
            repeat(27) { add(objs.logs) }
            val fullFire = prepareSource(cooking_locs.fire, offset = 80)
            player.withProtectedAccess {
                deps.ironSpit.start(this, IronSpitRecipes.rabbit, fullFire, inputSlot = 0)
            }
            assertEquals(1, player.count(IronSpitRecipes.rabbit.skewered))
            assertFalse(cooking_queues.ironSpit in player.weakQueueList)
            assertMessageSent("You don't have enough inventory space.")

            preparePlayer(level = IronSpitRecipes.beast.level)
            add(IronSpitRecipes.beast.skewered)
            player.worn[1] = InvObj(objs.cooking_skillcape)
            val capeFire = prepareSource(cooking_locs.fire, offset = 100)
            random.next = 99
            player.withProtectedAccess {
                deps.ironSpit.start(this, IronSpitRecipes.beast, capeFire, inputSlot = 0)
            }
            advance(3)
            assertEquals(1, player.count(IronSpitRecipes.beast.roasted))

            preparePlayer(level = 99)
            add(IronSpitRecipes.rabbit.skewered)
            val maxFire = prepareSource(cooking_locs.fire, offset = 120)
            random.next = 99
            player.withProtectedAccess {
                deps.ironSpit.start(this, IronSpitRecipes.rabbit, maxFire, inputSlot = 0)
            }
            advance(3)
            assertEquals(1, player.count(IronSpitRecipes.rabbit.roasted))

            preparePlayer(level = IronSpitRecipes.rabbit.level)
            player.inv[4] = InvObj(IronSpitRecipes.rabbit.skewered)
            val movingFire = prepareSource(cooking_locs.fire, offset = 140)
            player.withProtectedAccess {
                deps.ironSpit.start(this, IronSpitRecipes.rabbit, movingFire, inputSlot = 4)
            }
            player.placeAt(movingFire.coords.translateX(5))
            advance(3)
            assertEquals(1, player.count(IronSpitRecipes.rabbit.skewered))
            assertEquals(0, cookingXp())

            preparePlayer(level = IronSpitRecipes.beast.level)
            player.inv[4] = InvObj(IronSpitRecipes.beast.skewered)
            val staleFire = prepareSource(cooking_locs.fire, offset = 160)
            player.withProtectedAccess {
                deps.ironSpit.start(this, IronSpitRecipes.beast, staleFire, inputSlot = 4)
            }
            player.inv[4] = InvObj(objs.logs)
            advance(3)
            assertEquals(1, player.count(objs.logs))
            assertEquals(0, player.count(IronSpitRecipes.beast.roasted))
            assertEquals(0, cookingXp())

            preparePlayer(level = 30)
            player.inv[4] = InvObj(IronSpitRecipes.bird.skewered)
            player.inv[5] = InvObj(objs.raw_chompy)
            player.inv[6] = InvObj(objs.iron_spit)
            player.setCurrentLevel(stats.firemaking, 20)
            val firstFire = prepareSource(cooking_locs.fire, offset = 180)
            player.withProtectedAccess {
                deps.ironSpit.start(this, IronSpitRecipes.bird, firstFire, inputSlot = 4)
                deps.chompy.skewer(this, rawSlot = 5, spitSlot = 6)
            }
            random.next = 0
            advance(3)
            assertEquals(1, player.count(IronSpitRecipes.bird.skewered))
            assertEquals(1, player.count(objs.skewered_chompy))
            assertFalse(cooking_queues.ironSpit in player.weakQueueList)
        }

    @Test
    fun GameTestState.`cache 233 pins ogre spit roast morphs animations and dispatch`() =
        runCookingTest { deps ->
            assertEquals(6895, cooking_locs.ogreSpitRoast.id)
            assertEquals(3375, cooking_locs.ogreSpitRoastEmpty.id)
            assertEquals(3372, cooking_locs.ogreSpitRoastRawChompy.id)
            assertEquals(3373, cooking_locs.ogreSpitRoastCookedChompy.id)
            assertEquals(3374, cooking_locs.ogreSpitRoastRuinedChompy.id)
            assertEquals(3382, cooking_locs.ogreSpitRoastRawChicken.id)
            assertEquals(3383, cooking_locs.ogreSpitRoastCookedChicken.id)
            assertEquals(1770, varbits.ogre_spit_roaster.id)
            assertEquals(1017, seqs.roast.id)
            assertEquals(3181, seqs.spit_anim_player.id)
            assertEquals(3182, seqs.spit_end_anim_player.id)
            assertEquals(30, OgreSpitRoast.CHOMPY_COOKING_LEVEL)
            assertEquals(100.0, OgreSpitRoast.CHOMPY_COOKING_XP)
            assertEquals(5, OgreSpitRoast.CHOMPY_RESULT_TICKS)
            assertEquals(CookingChance(200, 255), OgreSpitRoast.CHOMPY_CHANCE)
            assertEquals(30, OgreSpitRoast.CHICKEN_COOKING_LEVEL)
            assertEquals(30.0, OgreSpitRoast.CHICKEN_COOKING_XP)
            assertEquals(6, OgreSpitRoast.CHICKEN_RESULT_TICKS)
            assertEquals(CookingChance(128, 512), OgreSpitRoast.CHICKEN_CHANCE)
            assertEquals(7566, objs.raw_jubbly.id)
            assertEquals(7568, objs.cooked_jubbly.id)
            assertEquals(7570, objs.burnt_jubbly.id)
            assertEquals(12561, cooking_locs.ogreSpitRoastRawJubbly.id)
            assertEquals(12562, cooking_locs.ogreSpitRoastCookedJubbly.id)
            assertEquals(12563, cooking_locs.ogreSpitRoastBurntJubbly.id)
            assertEquals(1904, varbits.rfd_skrach_uglogwee.id)
            assertEquals(41, OgreSpitRoast.JUBBLY_COOKING_LEVEL)
            assertEquals(160.0, OgreSpitRoast.JUBBLY_COOKING_XP)
            assertEquals(6, OgreSpitRoast.JUBBLY_RESULT_TICKS)
            assertEquals(110, OgreSpitRoast.JUBBLY_QUEST_STATE)
            assertEquals(CookingChance(191, 245), OgreSpitRoast.JUBBLY_CHANCE)
            assertEquals(listOf(170), deps.dbRows.types.getValue(2313).data[19])

            val base = deps.locs[cooking_locs.ogreSpitRoast]
            assertEquals(varbits.ogre_spit_roaster.id, base.multiVarBit)
            assertEquals(seqs.roast.id, base.anim)
            assertEquals(
                listOf(3375, 3372, 3373, 3374, 3382, 3383, 12561, 12562, 12563),
                base.multiLoc.map { it.toInt() and 0xFFFF },
            )
            assertNotNull(deps.seqs[seqs.spit_anim_player])
            assertNotNull(deps.seqs[seqs.spit_end_anim_player])
            assertTrue(
                deps.events.contains(
                    LocUEvents.Op::class.java,
                    EventBus.composeLongKey(cooking_locs.ogreSpitRoastEmpty.id, objs.raw_chompy.id),
                )
            )
            assertTrue(
                deps.events.contains(
                    LocUEvents.Op::class.java,
                    EventBus.composeLongKey(cooking_locs.ogreSpitRoastEmpty.id, objs.raw_chicken.id),
                )
            )
            assertTrue(
                deps.events.contains(
                    LocUEvents.Op::class.java,
                    EventBus.composeLongKey(cooking_locs.ogreSpitRoastEmpty.id, objs.raw_jubbly.id),
                )
            )

            preparePlayer(level = 99)
            add(objs.raw_shrimps)
            val spit = prepareSource(cooking_locs.ogreSpitRoast)
            useOnSource(deps, spit, slot = 0, objs.raw_shrimps)
            assertEquals(1, player.count(objs.raw_shrimps))
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertMessageSent("Nothing interesting happens.")
        }

    @Test
    fun GameTestState.`raw chompy ogre roasting has exact five tick player morph lifecycle`() =
        runCookingTest { deps ->
            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 1)
            player.inv[6] = InvObj(objs.raw_chompy)
            val spit = prepareSource(cooking_locs.ogreSpitRoast)
            random.next = 0

            useOnSource(deps, spit, slot = 6, objs.raw_chompy)

            assertEquals(OgreSpitRoast.RAW_CHOMPY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastRawChompy.id, visibleSpit(deps, spit).id)
            assertEquals(seqs.spit_anim_player.id, player.pendingSequence.id)
            assertEquals(1, player.count(objs.raw_chompy))
            assertMessageSent("You carefully place the chompy bird on the spit-roast.")

            advance(4)
            assertEquals(1, player.count(objs.raw_chompy))
            assertEquals(OgreSpitRoast.RAW_CHOMPY_STATE, player.vars[varbits.ogre_spit_roaster])
            advance()

            assertEquals(0, player.count(objs.raw_chompy))
            assertEquals(1, player.count(objs.cooked_chompy))
            assertEquals(1000, cookingXp())
            assertEquals(OgreSpitRoast.COOKED_CHOMPY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastCookedChompy.id, visibleSpit(deps, spit).id)
            assertMessageSent("You spit-roast the chompy bird.")

            advance()
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastEmpty.id, visibleSpit(deps, spit).id)

            preparePlayer(level = 30)
            player.inv[9] = InvObj(objs.raw_chompy)
            val burntSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 20)
            random.next = 99
            useOnSource(deps, burntSpit, slot = 9, objs.raw_chompy)
            advance(5)

            assertEquals(0, player.count(objs.raw_chompy))
            assertEquals(1, player.count(objs.ruined_chompy))
            assertEquals(0, cookingXp())
            assertEquals(OgreSpitRoast.RUINED_CHOMPY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastRuinedChompy.id, visibleSpit(deps, burntSpit).id)
            assertMessageSent("You accidentally burn the chompy.")
            advance()
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])
        }

    @Test
    fun GameTestState.`ogre chompy gates capes slots interruptions and sessions safely`() =
        runCookingTest { deps ->
            preparePlayer(level = 30, members = false)
            add(objs.raw_chompy)
            val membersSpit = prepareSource(cooking_locs.ogreSpitRoast)
            useOnSource(deps, membersSpit, slot = 0, objs.raw_chompy)
            assertEquals(1, player.count(objs.raw_chompy))
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertMessageSent("You need to be on a members' world to cook this food.")

            preparePlayer(level = 1)
            player.setCurrentLevel(stats.cooking, 29)
            add(objs.raw_chompy)
            val lowSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 20)
            useOnSource(deps, lowSpit, slot = 0, objs.raw_chompy)
            assertEquals(1, player.count(objs.raw_chompy))
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertMessageSent("You need a Cooking level of 30 to cook this food.")

            preparePlayer(level = 1)
            player.setCurrentLevel(stats.cooking, 30)
            repeat(28) { player.inv[it] = InvObj(objs.logs) }
            player.inv[11] = InvObj(objs.raw_chompy)
            player.worn[1] = InvObj(objs.max_cape_worn)
            val fullSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 40)
            random.next = 99
            useOnSource(deps, fullSpit, slot = 11, objs.raw_chompy)
            advance(5)
            assertEquals(0, player.count(objs.raw_chompy))
            assertEquals(1, player.count(objs.cooked_chompy))
            assertEquals(27, player.count(objs.logs))

            preparePlayer(level = 30)
            add(objs.raw_chompy)
            val cancelledSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 60)
            useOnSource(deps, cancelledSpit, slot = 0, objs.raw_chompy)
            player.weakQueueList.clear()
            advance()
            assertEquals(1, player.count(objs.raw_chompy))
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastEmpty.id, visibleSpit(deps, cancelledSpit).id)

            preparePlayer(level = 30)
            player.inv[3] = InvObj(objs.raw_chompy)
            player.inv[4] = InvObj(objs.raw_chompy)
            val staleSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 80)
            useOnSource(deps, staleSpit, slot = 3, objs.raw_chompy)
            player.inv[3] = InvObj(objs.logs)
            advance(5)
            assertEquals(1, player.count(objs.raw_chompy))
            assertEquals(1, player.count(objs.logs))
            assertEquals(0, player.count(objs.cooked_chompy))
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])

            preparePlayer(level = 30)
            add(objs.raw_chompy)
            val logoutSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 100)
            useOnSource(deps, logoutSpit, slot = 0, objs.raw_chompy)
            deps.events.publish(SessionStateEvent.Logout(player))
            assertEquals(1, player.count(objs.raw_chompy))
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertFalse(cooking_queues.ogreSpitRoast in player.weakQueueList)
        }

    @Test
    fun GameTestState.`jubbly roasting pins quest level six tick results and seven tick reuse`() =
        runCookingTest { deps ->
            preparePlayer(level = 41)
            player.setCurrentLevel(stats.firemaking, 1)
            player.setVarBit(varbits.rfd_skrach_uglogwee, 110)
            player.inv[5] = InvObj(objs.raw_jubbly)
            player.inv[8] = InvObj(objs.raw_jubbly)
            val spit = prepareSource(cooking_locs.ogreSpitRoast)
            random.next = 0
            random.then = 0

            useOnSource(deps, spit, slot = 5, objs.raw_jubbly)
            assertEquals(OgreSpitRoast.RAW_JUBBLY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastRawJubbly.id, visibleSpit(deps, spit).id)
            advance(5)
            assertEquals(2, player.count(objs.raw_jubbly))
            advance()

            assertEquals(1, player.count(objs.raw_jubbly))
            assertEquals(1, player.count(objs.cooked_jubbly))
            assertEquals(1600, cookingXp())
            assertEquals(OgreSpitRoast.COOKED_JUBBLY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastCookedJubbly.id, visibleSpit(deps, spit).id)
            assertMessageSent("Eventually the Jubbly is cooked.")

            advance()
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])
            useOnSource(deps, spit, slot = 8, objs.raw_jubbly)
            advance(6)
            assertEquals(0, player.count(objs.raw_jubbly))
            assertEquals(2, player.count(objs.cooked_jubbly))
            assertEquals(3200, cookingXp())
        }

    @Test
    fun GameTestState.`jubbly quest gates burns and cape immunity are enforced`() =
        runCookingTest { deps ->
            preparePlayer(level = 41)
            player.setVarBit(varbits.rfd_skrach_uglogwee, 109)
            add(objs.raw_jubbly)
            val lockedSpit = prepareSource(cooking_locs.ogreSpitRoast)
            useOnSource(deps, lockedSpit, slot = 0, objs.raw_jubbly)
            assertEquals(1, player.count(objs.raw_jubbly))
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertMessageSent(
                "You need to progress further through Recipe for Disaster to cook this food."
            )

            preparePlayer(level = 40)
            player.setVarBit(varbits.rfd_skrach_uglogwee, 110)
            add(objs.raw_jubbly)
            val lowSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 20)
            useOnSource(deps, lowSpit, slot = 0, objs.raw_jubbly)
            assertEquals(1, player.count(objs.raw_jubbly))
            assertMessageSent("You need a Cooking level of 41 to cook this food.")

            preparePlayer(level = 99)
            player.setVarBit(varbits.rfd_skrach_uglogwee, 170)
            add(objs.raw_jubbly)
            val burntSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 40)
            random.next = 99
            useOnSource(deps, burntSpit, slot = 0, objs.raw_jubbly)
            advance(6)
            assertEquals(0, player.count(objs.raw_jubbly))
            assertEquals(1, player.count(objs.burnt_jubbly))
            assertEquals(0, cookingXp())
            assertEquals(OgreSpitRoast.BURNT_JUBBLY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastBurntJubbly.id, visibleSpit(deps, burntSpit).id)
            assertMessageSent("Unfortunately the Jubbly is burnt.")

            preparePlayer(level = 41)
            player.setVarBit(varbits.rfd_skrach_uglogwee, 110)
            add(objs.raw_jubbly)
            player.worn[1] = InvObj(objs.cooking_skillcape_t)
            val capeSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 60)
            random.next = 99
            useOnSource(deps, capeSpit, slot = 0, objs.raw_jubbly)
            advance(6)
            assertEquals(1, player.count(objs.cooked_jubbly))
            assertEquals(1600, cookingXp())
        }

    @Test
    fun GameTestState.`ogre spit chicken is members level thirty six ticks and burnable`() =
        runCookingTest { deps ->
            preparePlayer(level = 30)
            player.setCurrentLevel(stats.firemaking, 1)
            player.inv[7] = InvObj(objs.raw_chicken)
            val spit = prepareSource(cooking_locs.ogreSpitRoast)
            random.next = 0

            useOnSource(deps, spit, slot = 7, objs.raw_chicken)
            assertEquals(OgreSpitRoast.RAW_CHICKEN_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastRawChicken.id, visibleSpit(deps, spit).id)
            advance(5)
            assertEquals(1, player.count(objs.raw_chicken))
            advance()
            assertEquals(0, player.count(objs.raw_chicken))
            assertEquals(1, player.count(objs.cooked_chicken))
            assertEquals(300, cookingXp())
            assertEquals(OgreSpitRoast.COOKED_CHICKEN_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastCookedChicken.id, visibleSpit(deps, spit).id)
            assertMessageSent("You roast a chicken.")
            advance()
            assertEquals(OgreSpitRoast.EMPTY_STATE, player.vars[varbits.ogre_spit_roaster])

            preparePlayer(level = 30)
            add(objs.raw_chicken)
            val burntSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 20)
            random.next = 99
            useOnSource(deps, burntSpit, slot = 0, objs.raw_chicken)
            advance(6)
            assertEquals(0, player.count(objs.raw_chicken))
            assertEquals(1, player.count(objs.burnt_chicken))
            assertEquals(0, cookingXp())
            assertEquals(OgreSpitRoast.RUINED_CHOMPY_STATE, player.vars[varbits.ogre_spit_roaster])
            assertEquals(cooking_locs.ogreSpitRoastRuinedChompy.id, visibleSpit(deps, burntSpit).id)

            preparePlayer(level = 34)
            add(objs.raw_chicken)
            val noBurnSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 40)
            random.next = 99
            useOnSource(deps, noBurnSpit, slot = 0, objs.raw_chicken)
            advance(6)
            assertEquals(1, player.count(objs.cooked_chicken))

            preparePlayer(level = 30, members = false)
            add(objs.raw_chicken)
            val freeSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 60)
            useOnSource(deps, freeSpit, slot = 0, objs.raw_chicken)
            assertEquals(1, player.count(objs.raw_chicken))
            assertMessageSent("You need to be on a members' world to cook this food.")

            preparePlayer(level = 29)
            add(objs.raw_chicken)
            val lowSpit = prepareSource(cooking_locs.ogreSpitRoast, offset = 80)
            useOnSource(deps, lowSpit, slot = 0, objs.raw_chicken)
            assertEquals(1, player.count(objs.raw_chicken))
            assertMessageSent("You need a Cooking level of 30 to cook this food.")
        }

    @Test
    fun GameTestState.`legacy rabbit ugthanki snails and lava eel pin exact cooking rules`() =
        runCookingTest { deps ->
            val burnable =
                listOf(
                    FishCurve(
                        objs.raw_rabbit,
                        CookingChance(128, 512),
                        CookingChance(128, 512),
                        CookingChance(128, 512),
                        CookingChance(140, 524),
                        CookingChance(153, 537),
                    ),
                    FishCurve(
                        objs.raw_ugthanki_meat,
                        CookingChance(40, 252),
                        CookingChance(30, 253),
                        CookingChance(30, 253),
                        CookingChance(42, 265),
                        CookingChance(55, 278),
                    ),
                    FishCurve(
                        objs.thin_snail,
                        CookingChance(93, 444),
                        CookingChance(93, 444),
                        CookingChance(103, 464),
                        CookingChance(105, 456),
                        CookingChance(118, 469),
                    ),
                    FishCurve(
                        objs.lean_snail,
                        CookingChance(93, 444),
                        CookingChance(85, 428),
                        CookingChance(95, 448),
                        CookingChance(97, 440),
                        CookingChance(110, 453),
                    ),
                    FishCurve(
                        objs.fat_snail,
                        CookingChance(73, 402),
                        CookingChance(73, 402),
                        CookingChance(83, 422),
                        CookingChance(85, 414),
                        CookingChance(98, 427),
                    ),
                )

            for ((index, curve) in burnable.withIndex()) {
                val recipe = StandardCookingRecipes.byRaw.getValue(curve.raw.id)
                assertEquals(
                    curve.fire,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Fire, false),
                )
                assertEquals(
                    curve.range,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Range, false),
                )
                assertEquals(
                    curve.lumbridge,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Lumbridge, false),
                )
                assertEquals(
                    curve.hosidius,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Hosidius, false),
                )
                assertEquals(
                    curve.hosidiusElite,
                    deps.cooking.chanceFor(
                        recipe,
                        CookingSourceKind.Hosidius,
                        false,
                        eliteHosidius = true,
                    ),
                )
                assertEquals(listOf(1, 3, 4), recipe.resultCadence())

                preparePlayer(level = recipe.level, members = true)
                add(recipe.raw)
                val range = prepareSource(cooking_locs.range, offset = 1600 + index * 10)
                random.next = 0
                player.withProtectedAccess {
                    deps.cooking.start(this, range, CookingSourceKind.Range, recipe, quantity = 1)
                }
                advance()
                assertEquals(0, player.count(recipe.raw))
                assertEquals(1, player.count(recipe.cooked))
                assertEquals((recipe.xp * 10).toInt(), cookingXp())

                preparePlayer(level = recipe.level, members = true)
                add(recipe.raw)
                val burnRange = prepareSource(cooking_locs.range, offset = 1700 + index * 10)
                random.next = 99
                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        burnRange,
                        CookingSourceKind.Range,
                        recipe,
                        quantity = 1,
                    )
                }
                advance()
                assertEquals(0, player.count(recipe.raw))
                assertEquals(1, player.count(requireNotNull(recipe.burnt)))
                assertEquals(0, cookingXp())

                preparePlayer(level = recipe.level, members = false)
                add(recipe.raw)
                val freeRange = prepareSource(cooking_locs.range, offset = 1800 + index * 10)
                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        freeRange,
                        CookingSourceKind.Range,
                        recipe,
                        quantity = 1,
                    )
                }
                assertEquals(1, player.count(recipe.raw))
                assertMessageSent("You need to be on a members' world to cook this food.")
            }

            for (raw in listOf(objs.thin_snail, objs.lean_snail, objs.fat_snail)) {
                val recipe = StandardCookingRecipes.byRaw.getValue(raw.id)
                preparePlayer(level = recipe.level - 1, members = true)
                add(recipe.raw)
                val range = prepareSource(cooking_locs.range, offset = 1900 + recipe.level)
                player.withProtectedAccess {
                    deps.cooking.start(this, range, CookingSourceKind.Range, recipe, quantity = 1)
                }
                assertEquals(1, player.count(recipe.raw))
                assertMessageSent("You need a Cooking level of ${recipe.level} to cook this food.")
            }

            val lavaEel = StandardCookingRecipes.byRaw.getValue(objs.raw_lava_eel.id)
            assertTrue(lavaEel.alwaysSucceeds)
            assertEquals(null, lavaEel.burnt)
            assertEquals(listOf(1, 3, 4), lavaEel.resultCadence())
            preparePlayer(level = lavaEel.level, members = true)
            add(lavaEel.raw)
            val range = prepareSource(cooking_locs.range, offset = 2000)
            random.next = 99
            player.withProtectedAccess {
                deps.cooking.start(this, range, CookingSourceKind.Range, lavaEel, quantity = 1)
            }
            advance()
            assertEquals(0, player.count(lavaEel.raw))
            assertEquals(1, player.count(lavaEel.cooked))
            assertEquals(300, cookingXp())

            preparePlayer(level = lavaEel.level - 1, members = true)
            add(lavaEel.raw)
            val lowRange = prepareSource(cooking_locs.range, offset = 2020)
            player.withProtectedAccess {
                deps.cooking.start(this, lowRange, CookingSourceKind.Range, lavaEel, quantity = 1)
            }
            assertEquals(1, player.count(lavaEel.raw))
            assertMessageSent("You need a Cooking level of 53 to cook this food.")

            preparePlayer(level = lavaEel.level, members = false)
            add(lavaEel.raw)
            val freeRange = prepareSource(cooking_locs.range, offset = 2040)
            player.withProtectedAccess {
                deps.cooking.start(this, freeRange, CookingSourceKind.Range, lavaEel, quantity = 1)
            }
            assertEquals(1, player.count(lavaEel.raw))
            assertMessageSent("You need to be on a members' world to cook this food.")
        }

    @Test
    fun GameTestState.`nine Hunter meats pin four tick curves requirements and outputs`() =
        runCookingTest { deps ->
            val expected =
                listOf(
                    FishCurve(
                        objs.raw_wild_kebbit,
                        CookingChance(84, 428),
                        CookingChance(84, 428),
                        CookingChance(84, 428),
                        CookingChance(96, 440),
                        CookingChance(109, 453),
                    ),
                    FishCurve(
                        objs.raw_larupia,
                        CookingChance(65, 390),
                        CookingChance(65, 390),
                        CookingChance(65, 390),
                        CookingChance(77, 402),
                        CookingChance(90, 415),
                    ),
                    FishCurve(
                        objs.raw_barb_tailed_kebbit,
                        CookingChance(56, 370),
                        CookingChance(56, 370),
                        CookingChance(56, 370),
                        CookingChance(68, 382),
                        CookingChance(81, 395),
                    ),
                    FishCurve(
                        objs.raw_graahk,
                        CookingChance(32, 328),
                        CookingChance(32, 328),
                        CookingChance(32, 328),
                        CookingChance(44, 340),
                        CookingChance(57, 353),
                    ),
                    FishCurve(
                        objs.raw_kyatt,
                        CookingChance(18, 292),
                        CookingChance(30, 310),
                        CookingChance(30, 310),
                        CookingChance(42, 322),
                        CookingChance(55, 335),
                    ),
                    FishCurve(
                        objs.raw_pyre_fox,
                        CookingChance(10, 273),
                        CookingChance(11, 276),
                        CookingChance(11, 276),
                        CookingChance(23, 288),
                        CookingChance(36, 301),
                    ),
                    FishCurve(
                        objs.raw_sunlight_antelope,
                        CookingChance(8, 254),
                        CookingChance(8, 265),
                        CookingChance(8, 265),
                        CookingChance(20, 277),
                        CookingChance(33, 290),
                    ),
                    FishCurve(
                        objs.raw_dashing_kebbit,
                        CookingChance(1, 194),
                        CookingChance(1, 224),
                        CookingChance(1, 224),
                        CookingChance(13, 236),
                        CookingChance(26, 249),
                    ),
                    FishCurve(
                        objs.raw_moonlight_antelope,
                        CookingChance(1, 185),
                        CookingChance(1, 200),
                        CookingChance(1, 200),
                        CookingChance(13, 212),
                        CookingChance(26, 225),
                    ),
                )

            for ((index, curve) in expected.withIndex()) {
                val recipe = StandardCookingRecipes.byRaw.getValue(curve.raw.id)
                assertEquals(listOf(4, 4, 4), recipe.resultCadence())
                assertEquals(
                    curve.fire,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Fire, false),
                )
                assertEquals(
                    curve.range,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Range, false),
                )
                assertEquals(
                    curve.lumbridge,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Lumbridge, false),
                )
                assertEquals(
                    curve.hosidius,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Hosidius, false),
                )
                assertEquals(
                    curve.hosidiusElite,
                    deps.cooking.chanceFor(
                        recipe,
                        CookingSourceKind.Hosidius,
                        false,
                        eliteHosidius = true,
                    ),
                )
                assertEquals(
                    curve.range,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Range, true),
                )
                assertEquals(
                    curve.hosidius,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Hosidius, true),
                )

                preparePlayer(level = recipe.level, members = true)
                add(recipe.raw)
                val range = prepareSource(cooking_locs.range, offset = 1100 + index * 10)
                random.next = 0
                player.withProtectedAccess {
                    deps.cooking.start(this, range, CookingSourceKind.Range, recipe, quantity = 1)
                }
                advance(3)
                assertEquals(1, player.count(recipe.raw))
                advance()
                assertEquals(0, player.count(recipe.raw))
                assertEquals(1, player.count(recipe.cooked))
                assertEquals((recipe.xp * 10).toInt(), cookingXp())

                preparePlayer(level = recipe.level, members = true)
                add(recipe.raw)
                val burnRange = prepareSource(cooking_locs.range, offset = 1200 + index * 10)
                random.next = 99
                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        burnRange,
                        CookingSourceKind.Range,
                        recipe,
                        quantity = 1,
                    )
                }
                advance(4)
                assertEquals(0, player.count(recipe.raw))
                assertEquals(1, player.count(requireNotNull(recipe.burnt)))
                assertEquals(0, cookingXp())

                preparePlayer(level = recipe.level - 1, members = true)
                add(recipe.raw)
                val lowRange = prepareSource(cooking_locs.range, offset = 1300 + index * 10)
                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        lowRange,
                        CookingSourceKind.Range,
                        recipe,
                        quantity = 1,
                    )
                }
                assertEquals(1, player.count(recipe.raw))
                assertMessageSent("You need a Cooking level of ${recipe.level} to cook this food.")

                preparePlayer(level = recipe.level, members = false)
                add(recipe.raw)
                val freeRange = prepareSource(cooking_locs.range, offset = 1400 + index * 10)
                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        freeRange,
                        CookingSourceKind.Range,
                        recipe,
                        quantity = 1,
                    )
                }
                assertEquals(1, player.count(recipe.raw))
                assertMessageSent("You need to be on a members' world to cook this food.")
            }

            val moonlight = StandardCookingRecipes.byRaw.getValue(objs.raw_moonlight_antelope.id)
            preparePlayer(level = moonlight.level, members = true)
            add(moonlight.raw, 3)
            val range = prepareSource(cooking_locs.range, offset = 1500)
            random.next = 0
            random.then = 0
            player.withProtectedAccess {
                deps.cooking.start(this, range, CookingSourceKind.Range, moonlight, quantity = 3)
            }
            advance(3)
            assertEquals(0, player.count(moonlight.cooked))
            advance()
            assertEquals(1, player.count(moonlight.cooked))
            advance(3)
            assertEquals(1, player.count(moonlight.cooked))
            advance()
            assertEquals(2, player.count(moonlight.cooked))
            advance(4)
            assertEquals(3, player.count(moonlight.cooked))
            assertEquals(6600, cookingXp())
        }

    @Test
    fun GameTestState.`remaining simple ordinary fish pin curves timing and membership`() =
        runCookingTest { deps ->
            val expected =
                listOf(
                    FishCurve(
                        objs.raw_rainbow_fish,
                        CookingChance(56, 370),
                        CookingChance(56, 370),
                        CookingChance(56, 370),
                        CookingChance(68, 382),
                        CookingChance(81, 395),
                    ),
                    FishCurve(
                        objs.raw_cave_eel,
                        CookingChance(38, 332),
                        CookingChance(38, 332),
                        CookingChance(38, 332),
                        CookingChance(50, 344),
                        CookingChance(63, 357),
                    ),
                    FishCurve(
                        objs.raw_dark_crab,
                        CookingChance(10, 222),
                        CookingChance(10, 222),
                        CookingChance(10, 222),
                        CookingChance(22, 234),
                        CookingChance(35, 247),
                    ),
                )

            for ((index, curve) in expected.withIndex()) {
                val recipe = StandardCookingRecipes.byRaw.getValue(curve.raw.id)
                assertEquals(
                    curve.fire,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Fire, false),
                )
                assertEquals(
                    curve.range,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Range, false),
                )
                assertEquals(
                    curve.lumbridge,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Lumbridge, false),
                )
                assertEquals(
                    curve.hosidius,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Hosidius, false),
                )
                assertEquals(
                    curve.hosidiusElite,
                    deps.cooking.chanceFor(
                        recipe,
                        CookingSourceKind.Hosidius,
                        false,
                        eliteHosidius = true,
                    ),
                )
                assertEquals(
                    curve.range,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Range, true),
                )

                preparePlayer(level = recipe.level, members = true)
                add(recipe.raw)
                val range = prepareSource(cooking_locs.range, offset = 900 + index * 10)
                random.next = 0
                player.withProtectedAccess {
                    deps.cooking.start(this, range, CookingSourceKind.Range, recipe, quantity = 1)
                }
                advance(recipe.firstResultTicks)

                assertEquals(0, player.count(recipe.raw))
                assertEquals(1, player.count(recipe.cooked))
                assertEquals((recipe.xp * 10).toInt(), cookingXp())

                preparePlayer(level = recipe.level, members = true)
                add(recipe.raw)
                val burnRange = prepareSource(cooking_locs.range, offset = 940 + index * 10)
                random.next = 99
                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        burnRange,
                        CookingSourceKind.Range,
                        recipe,
                        quantity = 1,
                    )
                }
                advance(recipe.firstResultTicks)

                assertEquals(0, player.count(recipe.raw))
                assertEquals(1, player.count(requireNotNull(recipe.burnt)))
                assertEquals(0, cookingXp())

                preparePlayer(level = recipe.level, members = false)
                add(recipe.raw)
                val freeRange = prepareSource(cooking_locs.range, offset = 980 + index * 10)
                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        freeRange,
                        CookingSourceKind.Range,
                        recipe,
                        quantity = 1,
                    )
                }
                assertEquals(1, player.count(recipe.raw))
                assertMessageSent("You need to be on a members' world to cook this food.")
            }

            val rainbow = StandardCookingRecipes.byRaw.getValue(objs.raw_rainbow_fish.id)
            val caveEel = StandardCookingRecipes.byRaw.getValue(objs.raw_cave_eel.id)
            val darkCrab = StandardCookingRecipes.byRaw.getValue(objs.raw_dark_crab.id)
            assertEquals(listOf(1, 3, 4), rainbow.resultCadence())
            assertEquals(listOf(1, 3, 4), caveEel.resultCadence())
            assertEquals(listOf(4, 4, 4), darkCrab.resultCadence())

            preparePlayer(level = darkCrab.level, members = true)
            add(darkCrab.raw, 3)
            val range = prepareSource(cooking_locs.range, offset = 1020)
            random.next = 0
            random.then = 0
            player.withProtectedAccess {
                deps.cooking.start(this, range, CookingSourceKind.Range, darkCrab, quantity = 3)
            }
            advance(3)
            assertEquals(0, player.count(darkCrab.cooked))
            advance()
            assertEquals(1, player.count(darkCrab.cooked))
            advance(3)
            assertEquals(1, player.count(darkCrab.cooked))
            advance()
            assertEquals(2, player.count(darkCrab.cooked))
            advance(4)
            assertEquals(3, player.count(darkCrab.cooked))
            assertEquals(6450, cookingXp())
        }

    @Test
    fun GameTestState.`seven further ordinary fish pin every source curve and membership`() =
        runCookingTest { deps ->
            val expected =
                listOf(
                    FishCurve(
                        objs.raw_mackerel,
                        CookingChance(98, 452),
                        CookingChance(98, 452),
                        CookingChance(108, 472),
                        CookingChance(110, 464),
                        CookingChance(123, 477),
                    ),
                    FishCurve(
                        objs.raw_cod,
                        CookingChance(83, 422),
                        CookingChance(88, 432),
                        CookingChance(93, 442),
                        CookingChance(100, 444),
                        CookingChance(113, 457),
                    ),
                    FishCurve(
                        objs.raw_pike,
                        CookingChance(78, 412),
                        CookingChance(78, 412),
                        CookingChance(88, 432),
                        CookingChance(90, 424),
                        CookingChance(103, 437),
                    ),
                    FishCurve(
                        objs.raw_slimy_eel,
                        CookingChance(63, 382),
                        CookingChance(63, 382),
                        CookingChance(63, 382),
                        CookingChance(75, 394),
                        CookingChance(88, 407),
                    ),
                    FishCurve(
                        objs.raw_bass,
                        CookingChance(33, 312),
                        CookingChance(33, 312),
                        CookingChance(33, 312),
                        CookingChance(45, 324),
                        CookingChance(58, 337),
                    ),
                    FishCurve(
                        objs.raw_sea_turtle,
                        CookingChance(1, 202),
                        CookingChance(1, 222),
                        CookingChance(1, 222),
                        CookingChance(13, 234),
                        CookingChance(26, 247),
                    ),
                    FishCurve(
                        objs.raw_manta_ray,
                        CookingChance(1, 202),
                        CookingChance(1, 222),
                        CookingChance(1, 222),
                        CookingChance(13, 234),
                        CookingChance(26, 247),
                    ),
                )

            for (curve in expected) {
                val recipe = StandardCookingRecipes.byRaw.getValue(curve.raw.id)
                assertEquals(
                    curve.fire,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Fire, false),
                )
                assertEquals(
                    curve.range,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Range, false),
                )
                assertEquals(
                    curve.lumbridge,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Lumbridge, false),
                )
                assertEquals(
                    curve.hosidius,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Hosidius, false),
                )
                assertEquals(
                    curve.hosidiusElite,
                    deps.cooking.chanceFor(
                        recipe,
                        CookingSourceKind.Hosidius,
                        false,
                        eliteHosidius = true,
                    ),
                )
                assertEquals(
                    curve.range,
                    deps.cooking.chanceFor(recipe, CookingSourceKind.Range, true),
                )

                preparePlayer(level = recipe.level, members = recipe.members)
                add(recipe.raw)
                val range = prepareSource(cooking_locs.range, offset = recipe.raw.id)
                random.next = 0
                player.withProtectedAccess {
                    deps.cooking.start(this, range, CookingSourceKind.Range, recipe, quantity = 1)
                }
                advance()

                assertEquals(0, player.count(recipe.raw))
                assertEquals(1, player.count(recipe.cooked))
                assertEquals((recipe.xp * 10).toInt(), cookingXp())

                preparePlayer(level = recipe.level, members = recipe.members)
                add(recipe.raw)
                val burnRange = prepareSource(cooking_locs.range, offset = recipe.raw.id + 5000)
                random.next = 99
                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        burnRange,
                        CookingSourceKind.Range,
                        recipe,
                        quantity = 1,
                    )
                }
                advance()

                assertEquals(0, player.count(recipe.raw))
                assertEquals(1, player.count(requireNotNull(recipe.burnt)))
                assertEquals(0, cookingXp())

                if (recipe.members) {
                    preparePlayer(level = recipe.level, members = false)
                    add(recipe.raw)
                    val freeRange = prepareSource(cooking_locs.range, offset = recipe.raw.id + 1)
                    player.withProtectedAccess {
                        deps.cooking.start(
                            this,
                            freeRange,
                            CookingSourceKind.Range,
                            recipe,
                            quantity = 1,
                        )
                    }
                    assertEquals(1, player.count(recipe.raw))
                    assertMessageSent("You need to be on a members' world to cook this food.")
                }
            }
        }

    @Test
    fun GameTestState.`ordinary raw meat aliases share exact output curves and membership`() =
        runCookingTest { deps ->
            val range = prepareSource(cooking_locs.range)
            val freeInputs = listOf(objs.raw_rat_meat, objs.raw_bear_meat, objs.raw_boar_meat)
            for (input in freeInputs) {
                preparePlayer(level = 1, members = false)
                add(input)
                random.next = 0

                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        range,
                        CookingSourceKind.Range,
                        StandardCookingRecipes.byRaw.getValue(input.id),
                        quantity = 1,
                    )
                }
                advance()

                assertEquals(0, player.count(input))
                assertEquals(1, player.count(objs.cooked_meat))
                assertEquals(300, cookingXp())
                assertMessageSent("You cook a piece of meat.")
            }

            for (input in listOf(objs.raw_yak_meat, objs.raw_mystery_meat)) {
                preparePlayer(level = 1, members = false)
                add(input)
                player.withProtectedAccess {
                    deps.cooking.start(
                        this,
                        range,
                        CookingSourceKind.Range,
                        StandardCookingRecipes.byRaw.getValue(input.id),
                        quantity = 1,
                    )
                }
                assertEquals(1, player.count(input))
                assertMessageSent("You need to be on a members' world to cook this food.")
            }
        }

    @Test
    fun GameTestState.`cooked meat deliberately burns with zero xp on the standard cadence`() =
        runCookingTest { deps ->
            preparePlayer(level = 99, members = false)
            add(objs.cooked_meat, 3)
            val range = prepareSource(cooking_locs.range)

            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    range,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(objs.cooked_meat.id),
                    quantity = 3,
                )
            }

            advance()
            assertEquals(1, player.count(objs.burnt_meat))
            advance(3)
            assertEquals(2, player.count(objs.burnt_meat))
            advance(4)
            assertEquals(3, player.count(objs.burnt_meat))
            assertEquals(0, cookingXp())
            assertMessageSent("You deliberately burn the nicely cooked meat.")

            preparePlayer(level = 1, members = true)
            repeat(28) { player.inv[it] = InvObj(objs.burnt_chicken) }
            player.inv[7] = InvObj(objs.cooked_mystery_meat)
            val fire = prepareSource(cooking_locs.fire, offset = 20)

            useOnSource(deps, fire, slot = 7, objs.cooked_mystery_meat)
            assertEquals(seqs.human_firecooking.id, player.pendingSequence.id)
            advance()

            assertEquals(0, player.count(objs.cooked_mystery_meat))
            assertEquals(1, player.count(objs.burnt_meat))
            assertEquals(27, player.count(objs.burnt_chicken))
            assertEquals(0, cookingXp())
        }

    @Test
    fun GameTestState.`range chooser deduplicates meat and respects its ten product limit`() =
        runCookingTest { deps ->
            preparePlayer(level = 99, members = true)
            val inputs =
                listOf(
                    objs.raw_chicken,
                    objs.raw_beef,
                    objs.raw_rat_meat,
                    objs.raw_bear_meat,
                    objs.raw_mystery_meat,
                    objs.raw_shrimps,
                    objs.raw_anchovies,
                    objs.raw_sardine,
                    objs.raw_herring,
                    objs.raw_trout,
                    objs.raw_salmon,
                    objs.raw_tuna,
                    objs.raw_lobster,
                )
            inputs.forEach { add(it) }
            val range = prepareSource(cooking_locs.range)

            player.withProtectedAccess { deps.cooking.select(this, range, CookingSourceKind.Range) }

            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    1,
                    1,
                    objs.cooked_chicken.id,
                    objs.cooked_meat.id,
                    objs.cooked_mystery_meat.id,
                    objs.shrimps.id,
                    objs.anchovies.id,
                    objs.sardine.id,
                    objs.herring.id,
                    objs.trout.id,
                    objs.salmon.id,
                    objs.tuna.id,
                    1,
                    "What would you like to cook?",
                ),
                setup.values,
            )
            resumeSkillMulti(components.skillmulti_a, quantity = 1)
            advance()
            assertEquals(1, player.count(objs.cooked_chicken))
        }

    @Test
    fun GameTestState.`single raw food skips the chooser and cooks after one tick`() =
        runCookingTest { deps ->
            preparePlayer(level = 1)
            player.inv[4] = InvObj(objs.raw_shrimps)
            val fire = prepareSource(cooking_locs.fire)
            random.next = 0

            useOnSource(deps, fire, slot = 4, objs.raw_shrimps)

            assertEquals(1, player.count(objs.raw_shrimps))
            assertEquals(seqs.human_firecooking.id, player.pendingSequence.id)
            advance()
            assertEquals(0, player.count(objs.raw_shrimps))
            assertEquals(1, player.count(objs.shrimps))
            assertEquals(300, cookingXp())
            assertEquals(0, player.weakQueueList.count(cooking_queues.standardFood))
        }

    @Test
    fun GameTestState.`Lumbridge range requires the completed cache quest state`() =
        runCookingTest { deps ->
            preparePlayer(level = 1)
            add(objs.raw_shrimps)
            val range = prepareSource(cooking_locs.lumbridgeRange)
            random.next = 0

            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    range,
                    CookingSourceKind.Lumbridge,
                    StandardCookingRecipes.byRaw.getValue(objs.raw_shrimps.id),
                    quantity = 1,
                )
            }
            assertMessageSent("You need to complete Cook's Assistant to use this range.")
            assertEquals(1, player.count(objs.raw_shrimps))

            player.setVarp(varps.cookquest, 2)
            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    range,
                    CookingSourceKind.Lumbridge,
                    StandardCookingRecipes.byRaw.getValue(objs.raw_shrimps.id),
                    quantity = 1,
                )
            }
            advance()
            assertEquals(0, player.count(objs.raw_shrimps))
            assertEquals(1, player.count(objs.shrimps))
            assertEquals(300, cookingXp())
        }

    @Test
    fun GameTestState.`cache 233 food preparation recipes and held interactions are pinned`() =
        runCookingTest { deps ->
            assertEquals(87, FoodPreparationRecipes.all.size)
            assertEquals(12, FoodPreparationRecipes.doughRecipes.size)
            assertEquals(3, FoodPreparationRecipes.cakeRecipes.size)
            assertEquals(9, FoodPreparationRecipes.directRecipes.size)
            assertEquals(8, FoodPreparationRecipes.stewAndCurryRecipes.size)
            assertEquals(26, FoodPreparationRecipes.pieRecipes.size)
            assertEquals(22, FoodPreparationRecipes.potatoRecipes.size)
            assertEquals(6, FoodPreparationRecipes.hotDrinkRecipes.size)
            assertEquals(1, FoodPreparationRecipes.oomlieRecipes.size)
            assertEquals(FoodPreparationRecipes.all.size, FoodPreparationRecipes.byKey.size)
            assertEquals(
                listOf(2307, 1953, 2283, 1863),
                FoodPreparationRecipes.doughOutputs.map { it.id },
            )
            assertEquals(
                listOf(1929, 1921, 1937),
                FoodPreparationRecipes.waterContainers.map { it.input.id },
            )
            assertEquals(
                listOf(1925, 1923, 1935),
                FoodPreparationRecipes.waterContainers.map { it.empty.id },
            )
            assertEquals(85, cooking_queues.foodPreparation.id)

            val identities =
                mapOf(
                    objs.pot_of_flour to 1933,
                    objs.pot_empty to 1931,
                    objs.cake_tin to 1887,
                    objs.uncooked_cake to 1889,
                    objs.chocolate_cake to 1897,
                    objs.pastry_dough to 1953,
                    objs.pizza_base to 2283,
                    objs.incomplete_pizza to 2285,
                    objs.uncooked_pizza to 2287,
                    objs.meat_pizza to 2293,
                    objs.anchovy_pizza to 2297,
                    objs.pineapple_pizza to 2301,
                    objs.incomplete_stew_potato to 1997,
                    objs.incomplete_stew_meat to 1999,
                    objs.uncooked_stew to 2001,
                    objs.spice to 2007,
                    objs.uncooked_curry to 2009,
                    objs.curry_leaf to 5970,
                    objs.pie_dish to 2313,
                    objs.pie_shell to 2315,
                    objs.redberries to 1951,
                    objs.cooking_apple to 1955,
                    objs.bucket_of_compost to 6032,
                    objs.clay to 434,
                    objs.draynor_cabbage to 1967,
                    objs.strawberry to 5504,
                    objs.watermelon to 5982,
                    objs.golovanova_fruit_top to 19653,
                    objs.sulliuscep_cap to 21626,
                    objs.dragonfruit to 22929,
                    objs.part_mud_pie_compost to 7164,
                    objs.part_mud_pie_water to 7166,
                    objs.part_garden_pie_tomato to 7172,
                    objs.part_garden_pie_onion to 7174,
                    objs.part_fish_pie_trout to 7182,
                    objs.part_fish_pie_cod to 7184,
                    objs.part_admiral_pie_salmon to 7192,
                    objs.part_admiral_pie_tuna to 7194,
                    objs.part_wild_pie_bear to 7202,
                    objs.part_wild_pie_chompy to 7204,
                    objs.part_summer_pie_strawberry to 7212,
                    objs.part_summer_pie_watermelon to 7214,
                    objs.pat_of_butter to 6697,
                    objs.pat_of_not_garlic_butter to 24788,
                    objs.potato_with_butter to 6703,
                    objs.potato_with_cheese to 6705,
                    objs.chilli_potato to 7054,
                    objs.egg_potato to 7056,
                    objs.mushroom_potato to 7058,
                    objs.tuna_potato to 7060,
                    objs.chilli_con_carne to 7062,
                    objs.egg_and_tomato to 7064,
                    objs.mushroom_and_onion to 7066,
                    objs.tuna_and_corn to 7068,
                    objs.minced_meat to 7070,
                    objs.spicy_sauce to 7072,
                    objs.chopped_garlic to 7074,
                    objs.chopped_tuna to 7086,
                    objs.bowl_of_sweetcorn to 7088,
                    objs.gnome_spice to 2169,
                    objs.bittercap_mushroom to 6004,
                    objs.bowl_hot_water to 4456,
                    objs.bowl_nettlewater to 4237,
                    objs.bowl_nettletea to 4239,
                    objs.bowl_nettletea_milky to 4240,
                    objs.nettles to 4241,
                    objs.empty_cup to 1980,
                    objs.cup_of_water to 4458,
                    objs.cup_of_hot_water to 4460,
                    objs.cup_of_nettle_tea to 4242,
                    objs.cup_of_milky_nettle_tea to 4243,
                    objs.raw_oomlie to 2337,
                    objs.palm_leaf to 2339,
                    objs.wrapped_oomlie to 2341,
                    objs.cooked_oomlie to 2343,
                    objs.burnt_oomlie to 2345,
                    objs.burnt_unwrapped_oomlie to 2426,
                )
            identities.forEach { (obj, id) -> assertEquals(id, obj.id) }

            val registered =
                FoodPreparationRecipes.waterContainers.map { objs.pot_of_flour to it.input } +
                    FoodPreparationRecipes.heldRecipes.map { it.first.obj to it.second.obj }
            assertEquals(78, registered.size)
            for ((first, second) in registered) {
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(first.id, second.id),
                    )
                )
            }
            assertFalse(
                deps.events.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.pizza_base.id, objs.cheese.id),
                )
            )
            assertFalse(
                deps.events.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.part_garden_pie_onion.id, objs.draynor_cabbage.id),
                )
            )
            assertFalse(
                deps.events.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.chopped_garlic.id, objs.spice.id),
                )
            )
        }

    @Test
    fun GameTestState.`oomlie wrapping is immediate boostable members only and awards ten xp`() =
        runCookingTest { deps ->
            val recipe = FoodPreparationRecipes.oomlieRecipes.single()
            assertEquals(objs.raw_oomlie, recipe.first.obj)
            assertEquals(objs.palm_leaf, recipe.second.obj)
            assertEquals(objs.wrapped_oomlie, recipe.output)
            assertEquals(50, recipe.level)
            assertEquals(10.0, recipe.xp)
            assertTrue(recipe.members)
            assertEquals(0, recipe.ticks)
            assertFalse(recipe.batch)

            preparePlayer(level = 49)
            player.setCurrentLevel(stats.cooking, 50)
            player.inv[3] = InvObj(objs.raw_oomlie)
            player.inv[8] = InvObj(objs.palm_leaf)
            useTogether(deps, objs.raw_oomlie, 3, objs.palm_leaf, 8, reverse = true)
            assertEquals(0, player.count(objs.raw_oomlie))
            assertEquals(0, player.count(objs.palm_leaf))
            assertEquals(1, player.count(objs.wrapped_oomlie))
            assertEquals(100, cookingXp())
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)

            preparePlayer(level = 50, members = false)
            player.inv[3] = InvObj(objs.raw_oomlie)
            player.inv[8] = InvObj(objs.palm_leaf)
            useTogether(deps, objs.raw_oomlie, 3, objs.palm_leaf, 8)
            assertEquals(1, player.count(objs.raw_oomlie))
            assertEquals(1, player.count(objs.palm_leaf))
            assertMessageSent("You need to be on a members' world to use this item.")

            preparePlayer(level = 49)
            player.inv[3] = InvObj(objs.raw_oomlie)
            player.inv[8] = InvObj(objs.palm_leaf)
            useTogether(deps, objs.raw_oomlie, 3, objs.palm_leaf, 8)
            assertEquals(1, player.count(objs.raw_oomlie))
            assertEquals(1, player.count(objs.palm_leaf))
            assertMessageSent("You need a Cooking level of 50 to make this.")
        }

    @Test
    fun GameTestState.`wrapped oomlie pins curves cadence and continued drained cooking`() =
        runCookingTest { deps ->
            val recipe = StandardCookingRecipes.byRaw.getValue(objs.wrapped_oomlie.id)
            assertEquals(objs.cooked_oomlie, recipe.cooked)
            assertEquals(objs.burnt_oomlie, recipe.burnt)
            assertEquals(50, recipe.level)
            assertEquals(30.0, recipe.xp)
            assertTrue(recipe.members)
            assertEquals(CookingChance(106, 450), recipe.fire)
            assertEquals(CookingChance(112, 476), recipe.range)
            assertEquals(null, recipe.lumbridge)
            assertEquals(CookingChance(124, 488), recipe.hosidius)
            assertEquals(CookingChance(137, 501), recipe.hosidiusElite)
            assertEquals(listOf(2, 4, 4), recipe.resultCadence())
            assertFalse(recipe.maintainLevelDuringAction)
            assertEquals(CookingSourceKind.entries.toSet(), recipe.allowedSources)
            assertTrue(
                deps.events.contains(
                    LocUEvents.Op::class.java,
                    EventBus.composeLongKey(cooking_locs.range.id, objs.wrapped_oomlie.id),
                )
            )
            assertTrue(
                deps.events.contains(
                    LocUEvents.Op::class.java,
                    EventBus.composeLongKey(cooking_locs.fire.id, objs.wrapped_oomlie.id),
                )
            )

            preparePlayer(level = 50)
            add(objs.wrapped_oomlie, 3)
            val range = prepareSource(cooking_locs.range)
            random.next = 99
            player.withProtectedAccess {
                deps.cooking.start(this, range, CookingSourceKind.Range, recipe, quantity = 3)
            }
            advance()
            assertEquals(0, player.count(objs.cooked_oomlie))
            advance()
            assertEquals(1, player.count(objs.cooked_oomlie))
            advance(4)
            assertEquals(2, player.count(objs.cooked_oomlie))
            advance(4)
            assertEquals(3, player.count(objs.cooked_oomlie))
            assertEquals(900, cookingXp())

            preparePlayer(level = 50)
            add(objs.wrapped_oomlie, 2)
            val fire = prepareSource(cooking_locs.fire, offset = 20)
            random.next = 0
            random.then = 99
            player.withProtectedAccess {
                deps.cooking.start(this, fire, CookingSourceKind.Fire, recipe, quantity = 2)
            }
            advance(2)
            assertEquals(1, player.count(objs.cooked_oomlie))
            player.setCurrentLevel(stats.cooking, 42)
            advance(4)
            assertEquals(1, player.count(objs.burnt_oomlie))
            assertEquals(300, cookingXp())

            preparePlayer(level = 49)
            add(objs.wrapped_oomlie)
            val lowRange = prepareSource(cooking_locs.range, offset = 40)
            useOnSource(deps, lowRange, slot = 0, objs.wrapped_oomlie)
            assertEquals(1, player.count(objs.wrapped_oomlie))
            assertMessageSent("You need a Cooking level of 50 to cook this food.")

            preparePlayer(level = 50, members = false)
            add(objs.wrapped_oomlie)
            val freeRange = prepareSource(cooking_locs.range, offset = 60)
            useOnSource(deps, freeRange, slot = 0, objs.wrapped_oomlie)
            assertEquals(1, player.count(objs.wrapped_oomlie))
            assertMessageSent("You need to be on a members' world to cook this food.")
        }

    @Test
    fun GameTestState.`cache 233 hot drink recipes curves and dispatch are pinned`() =
        runCookingTest { deps ->
            assertEquals(
                listOf(
                    RecipeContract(1921, 4456, null, 1, 0.0, true),
                    RecipeContract(4458, 4460, null, 1, 0.0, true),
                    RecipeContract(4237, 4239, 1923, 20, 52.0, true),
                ),
                StandardCookingRecipes.hotDrinks.map {
                    RecipeContract(
                        it.raw.id,
                        it.cooked.id,
                        it.burnt?.id,
                        it.level,
                        it.xp,
                        it.members,
                    )
                },
            )
            val tea = StandardCookingRecipes.byRaw.getValue(objs.bowl_nettlewater.id)
            assertEquals(CookingChance(78, 412), tea.fire)
            assertEquals(CookingChance(78, 412), tea.range)
            assertEquals(null, tea.lumbridge)
            assertEquals(CookingChance(90, 424), tea.hosidius)
            assertEquals(CookingChance(103, 437), tea.hosidiusElite)
            assertEquals(1, tea.firstResultTicks)
            assertEquals(3, tea.secondResultGapTicks)
            assertEquals(4, tea.repeatResultGapTicks)
            assertTrue(StandardCookingRecipes.hotDrinks.take(2).all { it.alwaysSucceeds })
            assertTrue(
                StandardCookingRecipes.hotDrinks.take(2).all { it.secondResultGapTicks == 4 }
            )

            val held = FoodPreparationRecipes.hotDrinkRecipes
            assertEquals(listOf(20, 1, 1, 1, 1, 1), held.map { it.level })
            assertEquals(listOf(2, 0, 0, 0, 0, 0), held.map { it.ticks })
            assertEquals(listOf(true, false, false, false, false, true), held.map { it.batch })
            assertEquals(2, held.first().repeatTicks)
            assertEquals(1, held.last().repeatTicks)
            for (recipe in held) {
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(recipe.first.obj.id, recipe.second.obj.id),
                    )
                )
            }
            for (recipe in StandardCookingRecipes.hotDrinks) {
                assertTrue(
                    deps.events.contains(
                        LocUEvents.Op::class.java,
                        EventBus.composeLongKey(cooking_locs.range.id, recipe.raw.id),
                    )
                )
                assertTrue(
                    deps.events.contains(
                        LocUEvents.Op::class.java,
                        EventBus.composeLongKey(cooking_locs.fire.id, recipe.raw.id),
                    )
                )
            }
        }

    @Test
    fun GameTestState.`nettle water batches at level twenty without experience`() =
        runCookingTest { deps ->
            preparePlayer(level = 19)
            player.setCurrentLevel(stats.cooking, 20)
            player.inv[3] = InvObj(objs.bowl_water)
            player.inv[4] = InvObj(objs.bowl_water)
            player.inv[8] = InvObj(objs.nettles)
            player.inv[9] = InvObj(objs.nettles)

            useTogether(deps, objs.bowl_water, 3, objs.nettles, 8, reverse = true)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(2, setup.values[1])
            resumeSkillMulti(FoodPreparation.SKILL_MULTI_BUTTONS.first(), quantity = 2)
            advance()
            assertEquals(0, player.count(objs.bowl_nettlewater))
            advance()
            assertEquals(1, player.count(objs.bowl_nettlewater))
            advance(2)
            assertEquals(2, player.count(objs.bowl_nettlewater))
            assertEquals(0, cookingXp())

            preparePlayer(level = 20, members = false)
            player.inv[3] = InvObj(objs.bowl_water)
            player.inv[8] = InvObj(objs.nettles)
            useTogether(deps, objs.bowl_water, 3, objs.nettles, 8)
            assertMessageSent("You need to be on a members' world to use this item.")
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)

            preparePlayer(level = 19)
            player.inv[3] = InvObj(objs.bowl_water)
            player.inv[8] = InvObj(objs.nettles)
            useTogether(deps, objs.bowl_water, 3, objs.nettles, 8)
            assertMessageSent("You need a Cooking level of 20 to make this.")
        }

    @Test
    fun GameTestState.`nettle tea and hot water pin success burn xp and heat cadence`() =
        runCookingTest { deps ->
            val tea = StandardCookingRecipes.byRaw.getValue(objs.bowl_nettlewater.id)

            preparePlayer(level = 20)
            add(objs.bowl_nettlewater)
            val fire = prepareSource(cooking_locs.fire)
            random.next = 99
            useOnSource(deps, fire, slot = 0, objs.bowl_nettlewater)
            assertEquals(seqs.human_firecooking.id, player.pendingSequence.id)
            advance()
            assertEquals(1, player.count(objs.bowl_empty))
            assertEquals(0, player.count(objs.bowl_nettletea))
            assertEquals(0, cookingXp())

            preparePlayer(level = 20)
            add(objs.bowl_nettlewater)
            val range = prepareSource(cooking_locs.range, offset = 20)
            random.next = 0
            useOnSource(deps, range, slot = 0, objs.bowl_nettlewater)
            assertEquals(seqs.human_cooking.id, player.pendingSequence.id)
            advance()
            assertEquals(1, player.count(objs.bowl_nettletea))
            assertEquals(520, cookingXp())

            preparePlayer(level = 54)
            add(objs.bowl_nettlewater)
            val noBurnRange = prepareSource(cooking_locs.range, offset = 40)
            random.next = 99
            player.withProtectedAccess {
                deps.cooking.start(this, noBurnRange, CookingSourceKind.Range, tea, quantity = 1)
            }
            advance()
            assertEquals(1, player.count(objs.bowl_nettletea))
            assertEquals(520, cookingXp())

            preparePlayer(level = 1)
            add(objs.bowl_water, 2)
            val waterRange = prepareSource(cooking_locs.range, offset = 60)
            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    waterRange,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(objs.bowl_water.id),
                    quantity = 2,
                )
            }
            advance()
            assertEquals(1, player.count(objs.bowl_hot_water))
            advance(3)
            assertEquals(1, player.count(objs.bowl_hot_water))
            advance()
            assertEquals(2, player.count(objs.bowl_hot_water))
            assertEquals(0, cookingXp())

            preparePlayer(level = 1)
            add(objs.cup_of_water)
            val waterFire = prepareSource(cooking_locs.fire, offset = 80)
            useOnSource(deps, waterFire, slot = 0, objs.cup_of_water)
            advance()
            assertEquals(1, player.count(objs.cup_of_hot_water))
            assertEquals(0, cookingXp())
        }

    @Test
    fun GameTestState.`tea pouring milk and hot water preserve containers in a full inventory`() =
        runCookingTest { deps ->
            val transforms =
                listOf(
                    listOf(
                        objs.empty_cup,
                        objs.bowl_nettletea,
                        objs.cup_of_nettle_tea,
                        objs.bowl_empty,
                    ),
                    listOf(
                        objs.empty_cup,
                        objs.bowl_nettletea_milky,
                        objs.cup_of_milky_nettle_tea,
                        objs.bowl_empty,
                    ),
                    listOf(
                        objs.bowl_nettletea,
                        objs.bucket_of_milk,
                        objs.bowl_nettletea_milky,
                        objs.bucket_empty,
                    ),
                    listOf(
                        objs.cup_of_nettle_tea,
                        objs.bucket_of_milk,
                        objs.cup_of_milky_nettle_tea,
                        objs.bucket_empty,
                    ),
                )
            for ((index, items) in transforms.withIndex()) {
                val (first, second, output, returned) = items
                preparePlayer(level = 1)
                player.inv[0] = InvObj(first)
                player.inv[1] = InvObj(second)
                repeat(26) { add(objs.logs) }
                useTogether(deps, first, 0, second, 1, reverse = index % 2 != 0)
                assertEquals(1, player.count(output))
                assertEquals(1, player.count(returned))
                assertEquals(0, cookingXp())
            }

            preparePlayer(level = 1)
            player.inv[0] = InvObj(objs.empty_cup)
            player.inv[1] = InvObj(objs.bowl_hot_water)
            player.inv[2] = InvObj(objs.empty_cup)
            player.inv[3] = InvObj(objs.bowl_hot_water)
            repeat(24) { add(objs.logs) }
            useTogether(deps, objs.empty_cup, 0, objs.bowl_hot_water, 1)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(2, setup.values[1])
            resumeSkillMulti(FoodPreparation.SKILL_MULTI_BUTTONS.first(), quantity = 2)
            assertEquals(1, player.count(objs.cup_of_hot_water))
            assertEquals(1, player.count(objs.bowl_empty))
            advance()
            assertEquals(2, player.count(objs.cup_of_hot_water))
            assertEquals(2, player.count(objs.bowl_empty))
            assertEquals(0, cookingXp())
        }

    @Test
    fun GameTestState.`all ordinary dough choices return both containers after one tick`() =
        runCookingTest { deps ->
            for ((waterIndex, water) in FoodPreparationRecipes.waterContainers.withIndex()) {
                for ((outputIndex, output) in FoodPreparationRecipes.doughOutputs.withIndex()) {
                    preparePlayer(level = 99)
                    player.inv[3] = InvObj(objs.pot_of_flour)
                    player.inv[8] = InvObj(water.input)

                    useTogether(
                        deps,
                        objs.pot_of_flour,
                        3,
                        water.input,
                        8,
                        reverse = (waterIndex + outputIndex) % 2 != 0,
                    )

                    val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
                    assertEquals(
                        listOf<Any>(
                            0,
                            1,
                            objs.bread_dough.id,
                            objs.pastry_dough.id,
                            objs.pizza_base.id,
                            objs.pitta_dough.id,
                            -1,
                            -1,
                            -1,
                            -1,
                            -1,
                            -1,
                            1,
                            FoodPreparation.DOUGH_MENU_TITLE,
                        ),
                        setup.values,
                    )
                    resumeSkillMulti(FoodPreparation.SKILL_MULTI_BUTTONS[outputIndex], quantity = 1)
                    assertEquals(0, player.count(output))
                    advance()
                    assertEquals(1, player.count(output))
                    assertEquals(1, player.count(objs.pot_empty))
                    assertEquals(1, player.count(water.empty))
                    assertEquals(0, cookingXp())
                }
            }
        }

    @Test
    fun GameTestState.`dough preparation is boostable member gated capacity safe and slot safe`() =
        runCookingTest { deps ->
            preparePlayer(level = 34)
            player.setCurrentLevel(stats.cooking, 35)
            player.inv[3] = InvObj(objs.pot_of_flour)
            player.inv[8] = InvObj(objs.bucket_water)
            useTogether(deps, objs.pot_of_flour, 3, objs.bucket_water, 8)
            resumeSkillMulti(components.skillmulti_c, 1)
            advance()
            assertEquals(1, player.count(objs.pizza_base))

            preparePlayer(level = 34)
            player.inv[3] = InvObj(objs.pot_of_flour)
            player.inv[8] = InvObj(objs.bucket_water)
            useTogether(deps, objs.pot_of_flour, 3, objs.bucket_water, 8)
            resumeSkillMulti(components.skillmulti_c, 1)
            assertMessageSent("You need a Cooking level of 35 to make this.")
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)
            advance()

            preparePlayer(level = 99, members = false)
            player.inv[3] = InvObj(objs.pot_of_flour)
            player.inv[8] = InvObj(objs.bucket_water)
            useTogether(deps, objs.pot_of_flour, 3, objs.bucket_water, 8)
            resumeSkillMulti(components.skillmulti_d, 1)
            assertMessageSent("You need to be on a members' world to use this item.")
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)
            advance()

            preparePlayer(level = 99)
            player.inv[3] = InvObj(objs.pot_of_flour)
            player.inv[8] = InvObj(objs.bucket_water)
            repeat(26) { add(objs.logs) }
            useTogether(deps, objs.pot_of_flour, 3, objs.bucket_water, 8)
            resumeSkillMulti(components.skillmulti_a, 1)
            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(objs.pot_of_flour))
            assertEquals(1, player.count(objs.bucket_water))
            advance()

            preparePlayer(level = 99)
            player.inv[3] = InvObj(objs.pot_of_flour)
            player.inv[8] = InvObj(objs.bucket_water)
            useTogether(deps, objs.pot_of_flour, 3, objs.bucket_water, 8)
            resumeSkillMulti(components.skillmulti_a, 1)
            player.inv[8] = InvObj(objs.logs)
            advance()
            assertEquals(1, player.count(objs.pot_of_flour))
            assertEquals(0, player.count(objs.bread_dough))
        }

    @Test
    fun GameTestState
        .`cake assembly accepts every trigger after two ticks and returns containers`() =
        runCookingTest { deps ->
            val triggers = listOf(objs.egg, objs.pot_of_flour, objs.bucket_of_milk)
            for ((index, trigger) in triggers.withIndex()) {
                preparePlayer(level = 39)
                player.setCurrentLevel(stats.cooking, 40)
                player.inv[2] = InvObj(objs.cake_tin)
                player.inv[7] = InvObj(objs.egg)
                player.inv[11] = InvObj(objs.pot_of_flour)
                player.inv[16] = InvObj(objs.bucket_of_milk)
                val triggerSlot = listOf(7, 11, 16)[index]

                useTogether(deps, objs.cake_tin, 2, trigger, triggerSlot, reverse = index % 2 != 0)

                advance()
                assertEquals(0, player.count(objs.uncooked_cake))
                advance()
                assertEquals(1, player.count(objs.uncooked_cake))
                assertEquals(1, player.count(objs.pot_empty))
                assertEquals(1, player.count(objs.bucket_empty))
                assertEquals(0, player.count(objs.cake_tin))
                assertEquals(0, cookingXp())
                assertMessageSent(
                    "You mix the milk, flour and egg together to make a raw cake mix."
                )
            }

            preparePlayer(level = 40)
            player.inv[2] = InvObj(objs.cake_tin)
            player.inv[7] = InvObj(objs.egg)
            player.inv[11] = InvObj(objs.pot_of_flour)
            player.inv[16] = InvObj(objs.bucket_of_milk)
            repeat(24) { add(objs.logs) }
            useTogether(deps, objs.cake_tin, 2, objs.egg, 7)
            advance(2)
            assertEquals(1, player.count(objs.uncooked_cake))
            assertEquals(24, player.count(objs.logs))

            preparePlayer(level = 40)
            player.inv[2] = InvObj(objs.cake_tin)
            player.inv[7] = InvObj(objs.egg)
            player.inv[11] = InvObj(objs.pot_of_flour)
            player.inv[16] = InvObj(objs.bucket_of_milk)
            useTogether(deps, objs.cake_tin, 2, objs.egg, 7)
            player.inv[11] = InvObj(objs.logs)
            advance(2)
            assertEquals(0, player.count(objs.uncooked_cake))
            assertEquals(1, player.count(objs.cake_tin))
        }

    @Test
    fun GameTestState
        .`pizza stages and standard toppings use exact levels xp timing and membership`() =
        runCookingTest { deps ->
            for ((index, recipe) in FoodPreparationRecipes.directRecipes.withIndex()) {
                preparePlayer(level = 99)
                player.inv[4] = InvObj(recipe.first.obj)
                player.inv[12] = InvObj(recipe.second.obj)
                useTogether(
                    deps,
                    recipe.first.obj,
                    4,
                    recipe.second.obj,
                    12,
                    reverse = index % 2 != 0,
                )
                advance()
                assertEquals(0, player.count(recipe.output))
                advance()
                assertEquals(1, player.count(recipe.output))
                assertEquals((recipe.xp * 10).toInt(), cookingXp(), recipe.toString())
            }

            preparePlayer(level = 64)
            player.setCurrentLevel(stats.cooking, 65)
            player.inv[4] = InvObj(objs.plain_pizza)
            player.inv[12] = InvObj(objs.pineapple_ring)
            useTogether(deps, objs.plain_pizza, 4, objs.pineapple_ring, 12)
            advance(2)
            assertEquals(1, player.count(objs.pineapple_pizza))
            assertEquals(450, cookingXp())

            preparePlayer(level = 99, members = false)
            player.inv[4] = InvObj(objs.plain_pizza)
            player.inv[12] = InvObj(objs.pineapple_chunks)
            useTogether(deps, objs.plain_pizza, 4, objs.pineapple_chunks, 12)
            assertMessageSent("You need to be on a members' world to use this item.")
            advance(2)
            assertEquals(1, player.count(objs.plain_pizza))
            assertEquals(0, player.count(objs.pineapple_pizza))

            preparePlayer(level = 49)
            player.inv[4] = InvObj(objs.cake)
            player.inv[12] = InvObj(objs.chocolate_dust)
            useTogether(deps, objs.cake, 4, objs.chocolate_dust, 12)
            assertMessageSent("You need a Cooking level of 50 to make this.")
            assertEquals(0, player.count(objs.chocolate_cake))
        }

    @Test
    fun GameTestState.`stew ingredients work in either order at boosted level twenty five`() =
        runCookingTest { deps ->
            val recipes = FoodPreparationRecipes.stewAndCurryRecipes.take(6)
            assertEquals(
                listOf(
                    "You cut up the potato and put it into the bowl.",
                    "You cut up the meat and put it into the bowl.",
                    "You cut up the chicken and put it into the bowl.",
                    "You cut up the meat and put it into the stew.",
                    "You cut up the chicken and put it into the stew.",
                    "You cut up the potato and put it into the bowl.",
                ),
                recipes.map { it.message },
            )
            for ((index, recipe) in recipes.withIndex()) {
                preparePlayer(level = 24, members = false)
                player.setCurrentLevel(stats.cooking, 25)
                player.inv[5] = InvObj(recipe.first.obj)
                player.inv[17] = InvObj(recipe.second.obj)
                useTogether(
                    deps,
                    recipe.first.obj,
                    5,
                    recipe.second.obj,
                    17,
                    reverse = index % 2 != 0,
                )
                advance()
                assertEquals(0, player.count(recipe.output))
                advance()
                assertEquals(1, player.count(recipe.output))
                assertEquals(0, cookingXp())
                assertMessageSent(requireNotNull(recipe.message))
            }

            preparePlayer(level = 25, members = false)
            player.inv[5] = InvObj(objs.bowl_water)
            player.inv[17] = InvObj(objs.potato)
            repeat(26) { add(objs.logs) }
            useTogether(deps, objs.bowl_water, 5, objs.potato, 17)
            advance(2)
            assertEquals(1, player.count(objs.incomplete_stew_potato))
            assertEquals(26, player.count(objs.logs))

            preparePlayer(level = 24, members = false)
            player.inv[5] = InvObj(objs.bowl_water)
            player.inv[17] = InvObj(objs.potato)
            useTogether(deps, objs.bowl_water, 5, objs.potato, 17)
            assertMessageSent("You need a Cooking level of 25 to make this.")
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)
        }

    @Test
    fun GameTestState.`curry accepts one spice or exactly three leaves after two ticks`() =
        runCookingTest { deps ->
            preparePlayer(level = 59)
            player.setCurrentLevel(stats.cooking, 60)
            player.inv[4] = InvObj(objs.uncooked_stew)
            player.inv[12] = InvObj(objs.spice)
            useTogether(deps, objs.uncooked_stew, 4, objs.spice, 12, reverse = true)
            advance()
            assertEquals(0, player.count(objs.uncooked_curry))
            advance()
            assertEquals(1, player.count(objs.uncooked_curry))
            assertEquals(0, cookingXp())

            preparePlayer(level = 60)
            player.inv[4] = InvObj(objs.uncooked_stew)
            player.inv[12] = InvObj(objs.curry_leaf)
            player.inv[13] = InvObj(objs.curry_leaf)
            player.inv[14] = InvObj(objs.curry_leaf)
            useTogether(deps, objs.uncooked_stew, 4, objs.curry_leaf, 13)
            advance(2)
            assertEquals(1, player.count(objs.uncooked_curry))
            assertEquals(0, player.count(objs.curry_leaf))
            assertEquals(0, cookingXp())

            preparePlayer(level = 60)
            player.inv[4] = InvObj(objs.uncooked_stew)
            player.inv[12] = InvObj(objs.curry_leaf)
            player.inv[13] = InvObj(objs.curry_leaf)
            useTogether(deps, objs.uncooked_stew, 4, objs.curry_leaf, 12)
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)
            advance(2)
            assertEquals(1, player.count(objs.uncooked_stew))
            assertEquals(0, player.count(objs.uncooked_curry))

            preparePlayer(level = 99, members = false)
            player.inv[4] = InvObj(objs.uncooked_stew)
            player.inv[12] = InvObj(objs.spice)
            useTogether(deps, objs.uncooked_stew, 4, objs.spice, 12)
            assertMessageSent("You need to be on a members' world to use this item.")
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)

            preparePlayer(level = 60)
            player.inv[4] = InvObj(objs.uncooked_stew)
            player.inv[12] = InvObj(objs.curry_leaf)
            player.inv[13] = InvObj(objs.curry_leaf)
            player.inv[14] = InvObj(objs.curry_leaf)
            useTogether(deps, objs.uncooked_stew, 4, objs.curry_leaf, 12)
            player.inv[14] = InvObj(objs.logs)
            advance(2)
            assertEquals(1, player.count(objs.uncooked_stew))
            assertEquals(0, player.count(objs.uncooked_curry))
        }

    @Test
    fun GameTestState.`cache 233 pins every standard pie preparation stage`() = runCookingTest {
        assertEquals(
            listOf(
                PiePreparationContract(1953, 2313, 2315, 1, false, 2),
                PiePreparationContract(2315, 1951, 2321, 10, false, 2),
                PiePreparationContract(2315, 2142, 2319, 20, false, 2),
                PiePreparationContract(2315, 2140, 2319, 20, false, 2),
                PiePreparationContract(2315, 1955, 2317, 30, false, 2),
                PiePreparationContract(2315, 6032, 7164, 29, true, 2, 1925),
                PiePreparationContract(7164, 1929, 7166, 29, true, 2, 1925),
                PiePreparationContract(7166, 434, 7168, 29, true, 2),
                PiePreparationContract(2315, 1982, 7172, 34, true, 2),
                PiePreparationContract(7172, 1957, 7174, 34, true, 2),
                PiePreparationContract(7174, 1965, 7176, 34, true, 2),
                PiePreparationContract(2315, 333, 7182, 47, true, 2),
                PiePreparationContract(7182, 339, 7184, 47, true, 2),
                PiePreparationContract(7184, 1942, 7186, 47, true, 2),
                PiePreparationContract(2315, 19653, 19656, 52, true, 2),
                PiePreparationContract(2315, 21626, 21684, 60, true, 2),
                PiePreparationContract(2315, 329, 7192, 70, true, 1),
                PiePreparationContract(7192, 361, 7194, 70, true, 2),
                PiePreparationContract(7194, 1942, 7196, 70, true, 2),
                PiePreparationContract(2315, 22929, 22789, 73, true, 2),
                PiePreparationContract(2315, 2136, 7202, 85, true, 2),
                PiePreparationContract(7202, 2876, 7204, 85, true, 2),
                PiePreparationContract(7204, 3226, 7206, 85, true, 2),
                PiePreparationContract(2315, 5504, 7212, 95, true, 2),
                PiePreparationContract(7212, 5982, 7214, 95, true, 2),
                PiePreparationContract(7214, 1955, 7216, 95, true, 2),
            ),
            FoodPreparationRecipes.pieRecipes.map {
                PiePreparationContract(
                    it.first.obj.id,
                    it.second.obj.id,
                    it.output.id,
                    it.level,
                    it.members,
                    it.ticks,
                    it.second.returned?.id,
                )
            },
        )
        assertTrue(FoodPreparationRecipes.pieRecipes.all { it.batch })
        assertTrue(FoodPreparationRecipes.pieRecipes.all { it.repeatTicks == 2 })
        assertTrue(FoodPreparationRecipes.pieRecipes.all { it.xp == 0.0 })
    }

    @Test
    fun GameTestState.`every pie preparation recipe dispatches symmetrically through Make-All`() =
        runCookingTest { deps ->
            for ((index, recipe) in FoodPreparationRecipes.pieRecipes.withIndex()) {
                preparePlayer(level = recipe.level, members = recipe.members)
                player.inv[4] = InvObj(recipe.first.obj)
                player.inv[12] = InvObj(recipe.second.obj)

                useTogether(
                    deps,
                    recipe.first.obj,
                    4,
                    recipe.second.obj,
                    12,
                    reverse = index % 2 != 0,
                )

                val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
                assertEquals(
                    listOf<Any>(
                        0,
                        1,
                        recipe.output.id,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        1,
                        FoodPreparation.BATCH_MENU_TITLE,
                    ),
                    setup.values,
                    recipe.toString(),
                )
                resumeSkillMulti(components.skillmulti_a, 1)
                repeat(recipe.ticks - 1) {
                    advance()
                    assertEquals(0, player.count(recipe.output), recipe.toString())
                }
                advance()
                assertEquals(1, player.count(recipe.output), recipe.toString())
                assertEquals(0, player.count(recipe.first.obj), recipe.toString())
                assertEquals(0, player.count(recipe.second.obj), recipe.toString())
                recipe.second.returned?.let { assertEquals(1, player.count(it), recipe.toString()) }
                assertEquals(0, cookingXp(), recipe.toString())
            }
        }

    @Test
    fun GameTestState.`pie Make-All uses two tick results and the admiral first tick exception`() =
        runCookingTest { deps ->
            val shell = FoodPreparationRecipes.pieRecipes.first()
            preparePlayer(level = 1, members = false)
            add(shell.first.obj, 3)
            add(shell.second.obj, 3)
            useTogether(deps, shell.first.obj, 0, shell.second.obj, 3, reverse = true)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(3, setup.values[1])
            assertEquals(3, setup.values[12])
            resumeSkillMulti(components.skillmulti_a, 3)
            advance()
            assertEquals(0, player.count(shell.output))
            repeat(3) { made ->
                advance(if (made == 0) 1 else 2)
                assertEquals(made + 1, player.count(shell.output))
            }
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)

            val admiral =
                FoodPreparationRecipes.pieRecipes.single {
                    it.output == objs.part_admiral_pie_salmon
                }
            preparePlayer(level = 70)
            add(admiral.first.obj, 3)
            add(admiral.second.obj, 3)
            useTogether(deps, admiral.first.obj, 0, admiral.second.obj, 3)
            resumeSkillMulti(components.skillmulti_a, 3)
            advance()
            assertEquals(1, player.count(admiral.output))
            advance(2)
            assertEquals(2, player.count(admiral.output))
            advance(2)
            assertEquals(3, player.count(admiral.output))
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)
        }

    @Test
    fun GameTestState.`pie levels membership containers and stale batches are atomic`() =
        runCookingTest { deps ->
            val summer = FoodPreparationRecipes.pieRecipes.last()
            preparePlayer(level = 94)
            player.setCurrentLevel(stats.cooking, 95)
            player.inv[4] = InvObj(summer.first.obj)
            player.inv[12] = InvObj(summer.second.obj)
            useTogether(deps, summer.first.obj, 4, summer.second.obj, 12)
            resumeSkillMulti(components.skillmulti_a, 1)
            advance(2)
            assertEquals(1, player.count(objs.uncooked_summer_pie))

            preparePlayer(level = 94)
            player.inv[4] = InvObj(summer.first.obj)
            player.inv[12] = InvObj(summer.second.obj)
            useTogether(deps, summer.first.obj, 4, summer.second.obj, 12)
            assertMessageSent("You need a Cooking level of 95 to make this.")
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)

            val wild =
                FoodPreparationRecipes.pieRecipes.single { it.output == objs.part_wild_pie_bear }
            preparePlayer(level = 99, members = false)
            player.inv[4] = InvObj(wild.first.obj)
            player.inv[12] = InvObj(wild.second.obj)
            useTogether(deps, wild.first.obj, 4, wild.second.obj, 12)
            assertMessageSent("You need to be on a members' world to use this item.")
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)

            val redberry =
                FoodPreparationRecipes.pieRecipes.single { it.output == objs.uncooked_redberry_pie }
            preparePlayer(level = 10, members = false)
            player.inv[4] = InvObj(redberry.first.obj)
            player.inv[12] = InvObj(redberry.second.obj)
            useTogether(deps, redberry.first.obj, 4, redberry.second.obj, 12)
            player.inv[12] = InvObj(objs.logs)
            resumeSkillMulti(components.skillmulti_a, 1)
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)
            advance(2)
            assertEquals(0, player.count(redberry.output))
            assertEquals(1, player.count(redberry.first.obj))

            val mud =
                FoodPreparationRecipes.pieRecipes.single { it.output == objs.part_mud_pie_compost }
            preparePlayer(level = 29)
            player.inv[4] = InvObj(mud.first.obj)
            player.inv[12] = InvObj(mud.second.obj)
            repeat(26) { add(objs.logs) }
            useTogether(deps, mud.first.obj, 4, mud.second.obj, 12)
            resumeSkillMulti(components.skillmulti_a, 1)
            advance(2)
            assertEquals(1, player.count(objs.part_mud_pie_compost))
            assertEquals(1, player.count(objs.bucket_empty))
            assertEquals(26, player.count(objs.logs))

            val strawberry =
                FoodPreparationRecipes.pieRecipes.single {
                    it.output == objs.part_summer_pie_strawberry
                }
            preparePlayer(level = 95)
            player.inv[4] = InvObj(strawberry.first.obj)
            player.inv[5] = InvObj(strawberry.first.obj)
            player.inv[12] = InvObj(strawberry.second.obj)
            player.inv[13] = InvObj(strawberry.second.obj)
            useTogether(deps, strawberry.first.obj, 4, strawberry.second.obj, 12)
            resumeSkillMulti(components.skillmulti_a, 2)
            advance(2)
            assertEquals(1, player.count(strawberry.output))
            player.inv[13] = InvObj(objs.logs)
            advance(2)
            assertEquals(1, player.count(strawberry.output))
            assertEquals(1, player.count(strawberry.first.obj))
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)
        }

    @Test
    fun GameTestState.`cache 233 pins the complete potato topping preparation graph`() =
        runCookingTest {
            assertEquals(
                listOf(
                    PotatoPreparationContract(6701, 6697, 6703, 39, 40.0, 2),
                    PotatoPreparationContract(6701, 24788, 6703, 39, 40.0, 2),
                    PotatoPreparationContract(6703, 1985, 6705, 47, 40.0, 2),
                    PotatoPreparationContract(6703, 7062, 7054, 41, 15.0, 2, 1923),
                    PotatoPreparationContract(6703, 7064, 7056, 51, 45.0, 2),
                    PotatoPreparationContract(6703, 7066, 7058, 64, 55.0, 2),
                    PotatoPreparationContract(6703, 7068, 7060, 68, 10.0, 2, 1923),
                    PotatoPreparationContract(1923, 1550, 7074, 1, 0.0, 2, tool = 946),
                    PotatoPreparationContract(7074, 2169, 7072, 9, 25.0, 2),
                    PotatoPreparationContract(1923, 2142, 7070, 1, 0.0, 2, tool = 946),
                    PotatoPreparationContract(7072, 2142, 7062, 11, 0.0, 1, tool = 946),
                    PotatoPreparationContract(7072, 7070, 7062, 11, 0.0, 1, 1923),
                    PotatoPreparationContract(1923, 1944, 7076, 13, 0.0, 2),
                    PotatoPreparationContract(7078, 1982, 7064, 23, 0.0, 2),
                    PotatoPreparationContract(1923, 6004, 7080, 1, 0.0, 2, tool = 946),
                    PotatoPreparationContract(1923, 1957, 1871, 1, 0.0, 2, tool = 946),
                    PotatoPreparationContract(7082, 7084, 7066, 57, 0.0, 2),
                    PotatoPreparationContract(1923, 361, 7086, 1, 0.0, 2, tool = 946),
                    PotatoPreparationContract(1923, 5988, 7088, 1, 0.0, 2),
                    PotatoPreparationContract(7088, 361, 7068, 67, 0.0, 2, tool = 946),
                    PotatoPreparationContract(7088, 7086, 7068, 67, 0.0, 2, 1923),
                    PotatoPreparationContract(7086, 5988, 7068, 67, 0.0, 2),
                ),
                FoodPreparationRecipes.potatoRecipes.map {
                    PotatoPreparationContract(
                        it.first.obj.id,
                        it.second.obj.id,
                        it.output.id,
                        it.level,
                        it.xp,
                        it.ticks,
                        it.second.returned?.id,
                        it.tool?.id,
                    )
                },
            )
            assertTrue(FoodPreparationRecipes.potatoRecipes.all { it.members })
            assertTrue(FoodPreparationRecipes.potatoRecipes.none { it.batch })
        }

    @Test
    fun GameTestState.`every potato preparation route dispatches symmetrically at exact ticks`() =
        runCookingTest { deps ->
            for ((index, recipe) in FoodPreparationRecipes.potatoRecipes.withIndex()) {
                preparePlayer(level = recipe.level)
                player.inv[4] = InvObj(recipe.first.obj)
                player.inv[12] = InvObj(recipe.second.obj)
                recipe.tool?.let { player.inv[20] = InvObj(it) }

                useTogether(
                    deps,
                    recipe.first.obj,
                    4,
                    recipe.second.obj,
                    12,
                    reverse = index % 2 != 0,
                )

                repeat(recipe.ticks - 1) {
                    advance()
                    assertEquals(0, player.count(recipe.output), recipe.toString())
                }
                advance()
                assertEquals(1, player.count(recipe.output), recipe.toString())
                assertEquals(0, player.count(recipe.first.obj), recipe.toString())
                assertEquals(0, player.count(recipe.second.obj), recipe.toString())
                recipe.second.returned?.let { assertEquals(1, player.count(it), recipe.toString()) }
                recipe.tool?.let { assertEquals(1, player.count(it), recipe.toString()) }
                assertEquals((recipe.xp * 10).toInt(), cookingXp(), recipe.toString())
            }
        }

    @Test
    fun GameTestState.`potato preparation enforces boosts members knives and cancellation`() =
        runCookingTest { deps ->
            val tunaPotato =
                FoodPreparationRecipes.potatoRecipes.single { it.output == objs.tuna_potato }
            preparePlayer(level = 67)
            player.setCurrentLevel(stats.cooking, 68)
            player.inv[4] = InvObj(tunaPotato.first.obj)
            player.inv[12] = InvObj(tunaPotato.second.obj)
            useTogether(deps, tunaPotato.first.obj, 4, tunaPotato.second.obj, 12)
            advance(2)
            assertEquals(1, player.count(objs.tuna_potato))
            assertEquals(1, player.count(objs.bowl_empty))
            assertEquals(100, cookingXp())

            preparePlayer(level = 67)
            player.inv[4] = InvObj(tunaPotato.first.obj)
            player.inv[12] = InvObj(tunaPotato.second.obj)
            useTogether(deps, tunaPotato.first.obj, 4, tunaPotato.second.obj, 12)
            assertMessageSent("You need a Cooking level of 68 to make this.")
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)

            val butter = FoodPreparationRecipes.potatoRecipes.first()
            preparePlayer(level = 99, members = false)
            player.inv[4] = InvObj(butter.first.obj)
            player.inv[12] = InvObj(butter.second.obj)
            useTogether(deps, butter.first.obj, 4, butter.second.obj, 12)
            assertMessageSent("You need to be on a members' world to use this item.")
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)

            val garlic =
                FoodPreparationRecipes.potatoRecipes.single { it.output == objs.chopped_garlic }
            preparePlayer(level = 99)
            player.inv[4] = InvObj(garlic.first.obj)
            player.inv[12] = InvObj(garlic.second.obj)
            useTogether(deps, garlic.first.obj, 4, garlic.second.obj, 12)
            assertFalse(cooking_queues.foodPreparation in player.weakQueueList)
            assertEquals(1, player.count(garlic.first.obj))
            assertEquals(1, player.count(garlic.second.obj))

            player.inv[20] = InvObj(objs.knife)
            useTogether(deps, garlic.first.obj, 4, garlic.second.obj, 12)
            player.inv[20] = InvObj(objs.logs)
            advance(2)
            assertEquals(0, player.count(objs.chopped_garlic))
            assertEquals(1, player.count(garlic.first.obj))
            assertEquals(1, player.count(garlic.second.obj))

            preparePlayer(level = 99)
            player.inv[0] = InvObj(butter.first.obj)
            player.inv[1] = InvObj(butter.second.obj)
            player.inv[4] = InvObj(objs.bowl_empty)
            player.inv[5] = InvObj(objs.egg)
            useTogether(deps, butter.first.obj, 0, butter.second.obj, 1)
            useTogether(deps, objs.bowl_empty, 4, objs.egg, 5)
            advance(2)
            assertEquals(0, player.count(objs.potato_with_butter))
            assertEquals(1, player.count(objs.uncooked_egg))
            assertEquals(1, player.count(butter.first.obj))
            assertEquals(1, player.count(butter.second.obj))
        }

    @Test
    fun GameTestState.`potato bowl returns are full inventory atomic and recipe specific`() =
        runCookingTest { deps ->
            val returned =
                FoodPreparationRecipes.potatoRecipes.filter {
                    it.second.returned == objs.bowl_empty
                }
            assertEquals(4, returned.size)
            for ((index, recipe) in returned.withIndex()) {
                preparePlayer(level = recipe.level)
                player.inv[4] = InvObj(recipe.first.obj)
                player.inv[12] = InvObj(recipe.second.obj)
                repeat(26) { add(objs.logs) }
                useTogether(
                    deps,
                    recipe.first.obj,
                    4,
                    recipe.second.obj,
                    12,
                    reverse = index % 2 != 0,
                )
                advance(recipe.ticks)
                assertEquals(1, player.count(recipe.output), recipe.toString())
                assertEquals(1, player.count(objs.bowl_empty), recipe.toString())
                assertEquals(26, player.count(objs.logs), recipe.toString())
            }

            val noReturn =
                FoodPreparationRecipes.potatoRecipes.single { it.output == objs.egg_potato }
            preparePlayer(level = noReturn.level)
            player.inv[4] = InvObj(noReturn.first.obj)
            player.inv[12] = InvObj(noReturn.second.obj)
            repeat(26) { add(objs.logs) }
            useTogether(deps, noReturn.first.obj, 4, noReturn.second.obj, 12)
            advance(2)
            assertEquals(1, player.count(objs.egg_potato))
            assertEquals(0, player.count(objs.bowl_empty))
            assertEquals(26, player.count(objs.logs))
        }

    @Test
    fun GameTestState.`cache 233 pins every dairy churn route location and animation tier`() =
        runCookingTest { deps ->
            assertEquals(listOf(11695, 11696, 25720, 35937), cooking_locs.dairyChurns.map { it.id })
            for (churn in cooking_locs.dairyChurns) {
                assertEquals(listOf("Churn", null, null, null, null), deps.locs[churn].op.toList())
                assertTrue(deps.events.contains(LocEvents.Op1::class.java, churn.id))
                for (input in DairyChurningRecipes.inputs) {
                    assertTrue(
                        deps.events.contains(
                            LocUEvents.Op::class.java,
                            EventBus.composeLongKey(churn.id, input.id),
                        )
                    )
                }
            }

            assertEquals(
                listOf(
                    DairyChurningContract(1927, 2130, 21, 18.0, 10, 2793, 459, 460, true),
                    DairyChurningContract(1927, 6697, 38, 40.5, 19, 2794, 461, 462, true),
                    DairyChurningContract(1927, 1985, 48, 64.0, 26, 2795, 463, 464, true),
                    DairyChurningContract(2130, 6697, 38, 22.5, 10, 2793, 459, 460, false),
                    DairyChurningContract(2130, 1985, 48, 46.0, 19, 2794, 461, 462, false),
                    DairyChurningContract(6697, 1985, 48, 23.5, 10, 2793, 459, 460, false),
                    DairyChurningContract(24788, 1985, 48, 23.5, 10, 2793, 459, 460, false),
                ),
                DairyChurningRecipes.all.map {
                    DairyChurningContract(
                        it.input.id,
                        it.output.id,
                        it.level,
                        it.xp,
                        it.ticks,
                        it.playerAnimation.id,
                        it.regularChurnAnimation.id,
                        it.metalChurnAnimation.id,
                        it.returnsBucket,
                    )
                },
            )
            assertEquals(
                listOf(2790, 2791, 2792),
                listOf(seqs.churn_milk_short.id, seqs.churn_milk_medium.id, seqs.churn_milk_long.id),
            )
            assertEquals(
                seqs.churn_milk_short.id,
                deps.types.spotanims[spotanims.dairy_churn_short].anim,
            )
            assertEquals(
                seqs.churn_milk_short.id,
                deps.types.spotanims[spotanims.dairy_churn_metal_short].anim,
            )
            assertEquals(
                seqs.churn_milk_medium.id,
                deps.types.spotanims[spotanims.dairy_churn_medium].anim,
            )
            assertEquals(
                seqs.churn_milk_medium.id,
                deps.types.spotanims[spotanims.dairy_churn_metal_medium].anim,
            )
            assertEquals(
                seqs.churn_milk_long.id,
                deps.types.spotanims[spotanims.dairy_churn_long].anim,
            )
            assertEquals(
                seqs.churn_milk_long.id,
                deps.types.spotanims[spotanims.dairy_churn_metal_long].anim,
            )
        }

    @Test
    fun GameTestState.`every dairy route produces its exact result at its sourced tick`() =
        runCookingTest { deps ->
            for ((index, recipe) in DairyChurningRecipes.all.withIndex()) {
                preparePlayer(level = recipe.level)
                player.inv[4] = InvObj(recipe.input)
                val churn =
                    prepareSource(
                        if (index % 2 == 0) cooking_locs.dairyChurn
                        else cooking_locs.dairyChurnMetal,
                        offset = index * 4,
                    )
                player.withProtectedAccess { deps.dairyChurning.start(this, churn, 4, recipe, 1) }

                assertEquals(recipe.playerAnimation.id, player.pendingSequence.id)
                repeat(recipe.ticks - 1) {
                    advance()
                    assertEquals(1, player.count(recipe.input), recipe.toString())
                    assertEquals(0, player.count(recipe.output), recipe.toString())
                }
                advance()
                assertEquals(0, player.count(recipe.input), recipe.toString())
                assertEquals(1, player.count(recipe.output), recipe.toString())
                assertEquals(if (recipe.returnsBucket) 1 else 0, player.count(objs.bucket_empty))
                assertEquals((recipe.xp * 10).toInt(), cookingXp(), recipe.toString())
            }
        }

    @Test
    fun GameTestState.`dairy item use exposes every source-specific route and repeats exactly`() =
        runCookingTest { deps ->
            var offset = 0
            for (input in DairyChurningRecipes.inputs) {
                for ((button, recipe) in
                    DairyChurningRecipes.forInput.getValue(input.id).withIndex()) {
                    preparePlayer(level = 99)
                    player.inv[4] = InvObj(input)
                    player.inv[12] = InvObj(input)
                    val churn = prepareSource(cooking_locs.dairyChurn, offset = offset)
                    offset += 4

                    useOnSource(deps, churn, 4, input)
                    requireNotNull(
                        player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id))
                    )
                    resumeSkillMulti(DairyChurning.SKILL_MULTI_BUTTONS[button], quantity = 2)
                    advance(recipe.ticks)
                    assertEquals(1, player.count(recipe.output), recipe.toString())
                    assertEquals(1, player.count(input), recipe.toString())
                    advance(recipe.ticks)
                    assertEquals(2, player.count(recipe.output), recipe.toString())
                    assertEquals(0, player.count(input), recipe.toString())
                    assertEquals((recipe.xp * 20).toInt(), cookingXp(), recipe.toString())
                }
            }
        }

    @Test
    fun GameTestState.`dairy churning enforces boosts membership state and shared interruption`() =
        runCookingTest { deps ->
            val cheese =
                DairyChurningRecipes.byKey.getValue(objs.bucket_of_milk.id to objs.cheese.id)
            val churn = prepareSource(cooking_locs.dairyChurn)
            preparePlayer(level = 47)
            player.setCurrentLevel(stats.cooking, 48)
            player.inv[4] = InvObj(cheese.input)
            player.withProtectedAccess { deps.dairyChurning.start(this, churn, 4, cheese, 1) }
            advance(26)
            assertEquals(1, player.count(objs.cheese))
            assertEquals(640, cookingXp())

            preparePlayer(level = 47)
            player.inv[4] = InvObj(cheese.input)
            player.withProtectedAccess { deps.dairyChurning.start(this, churn, 4, cheese, 1) }
            assertMessageSent("You need a Cooking level of 48 to churn this.")
            assertFalse(cooking_queues.dairyChurning in player.weakQueueList)

            preparePlayer(level = 99, members = false)
            player.inv[4] = InvObj(cheese.input)
            player.withProtectedAccess { deps.dairyChurning.start(this, churn, 4, cheese, 1) }
            assertMessageSent(DairyChurning.MEMBERS_MESSAGE)
            assertFalse(cooking_queues.dairyChurning in player.weakQueueList)

            val cream = DairyChurningRecipes.all.first()
            preparePlayer(level = 99)
            player.inv[4] = InvObj(cream.input)
            player.withProtectedAccess { deps.dairyChurning.start(this, churn, 4, cream, 1) }
            player.inv[4] = InvObj(objs.logs)
            advance(10)
            assertEquals(0, player.count(objs.pot_of_cream))
            assertEquals(1, player.count(objs.logs))

            preparePlayer(level = 99)
            player.inv[4] = InvObj(cream.input)
            player.withProtectedAccess { deps.dairyChurning.start(this, churn, 4, cream, 1) }
            player.placeAt(churn.coords.translateX(5))
            advance(10)
            assertEquals(1, player.count(objs.bucket_of_milk))
            assertEquals(0, player.count(objs.pot_of_cream))

            preparePlayer(level = 99)
            player.inv[0] = InvObj(cream.input)
            player.inv[4] = InvObj(objs.baked_potato)
            player.inv[12] = InvObj(objs.pat_of_butter)
            player.withProtectedAccess { deps.dairyChurning.start(this, churn, 0, cream, 1) }
            useTogether(deps, objs.baked_potato, 4, objs.pat_of_butter, 12)
            advance(2)
            assertEquals(1, player.count(objs.potato_with_butter))
            advance(10)
            assertEquals(1, player.count(objs.bucket_of_milk))
            assertEquals(0, player.count(objs.pot_of_cream))
        }

    @Test
    fun GameTestState.`milk churning returns or owner drops buckets and native Churn works`() =
        runCookingTest { deps ->
            val cream = DairyChurningRecipes.all.first()
            val churn = prepareSource(cooking_locs.dairyChurn)
            preparePlayer(level = 99)
            player.inv[4] = InvObj(cream.input)
            repeat(26) { add(objs.logs) }
            player.withProtectedAccess { deps.dairyChurning.start(this, churn, 4, cream, 1) }
            advance(10)
            assertEquals(1, player.count(objs.pot_of_cream))
            assertEquals(1, player.count(objs.bucket_empty))
            assertEquals(0, groundCount(deps, objs.bucket_empty, player.coords))

            preparePlayer(level = 99)
            player.inv[4] = InvObj(cream.input)
            repeat(27) { add(objs.logs) }
            player.withProtectedAccess { deps.dairyChurning.start(this, churn, 4, cream, 1) }
            advance(10)
            assertEquals(1, player.count(objs.pot_of_cream))
            assertEquals(0, player.count(objs.bucket_empty))
            assertEquals(1, groundCount(deps, objs.bucket_empty, player.coords))

            preparePlayer(level = 99)
            churnAt(deps, churn)
            assertMessageSent(DairyChurning.NO_INGREDIENTS_MESSAGE)

            player.inv[7] = InvObj(objs.pat_of_butter)
            churnAt(deps, churn)
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
            resumeSkillMulti(components.skillmulti_a, quantity = 1)
            advance(10)
            assertEquals(1, player.count(objs.cheese))
            assertEquals(235, cookingXp())
        }

    @Test
    fun GameTestState.`cache 233 pins grape wine recipes graphics timers and dispatch`() =
        runCookingTest { deps ->
            assertEquals(
                listOf(
                    WineContract(1987, 1995, 1993, 35, false, 48, 352, 68),
                    WineContract(20749, 20752, 245, 65, true, -125, 260, 98),
                ),
                WineRecipes.all.map {
                    WineContract(
                        grapes = it.grapes.id,
                        unfermented = it.unfermented.id,
                        fermented = it.fermented.id,
                        level = it.level,
                        members = it.members,
                        low = it.chance.low,
                        high = it.chance.high,
                        noFailureLevel = it.noFailureLevel,
                    )
                },
            )
            assertEquals(1937, objs.jug_water.id)
            assertEquals(1991, objs.jug_of_bad_wine.id)
            assertEquals(7529, seqs.human_make_wine.id)
            assertEquals(47, spotanims.cooking_make_wine.id)
            assertEquals(7530, deps.types.spotanims[spotanims.cooking_make_wine].anim)
            assertEquals(87, cooking_queues.wineMaking.id)
            assertEquals(13, cooking_timers.wineFermentation.id)
            assertEquals(3, WineMaking.FIRST_RESULT_TICKS)
            assertEquals(2, WineMaking.REPEAT_RESULT_TICKS)
            assertEquals(20, WineMaking.FERMENTATION_TICKS)
            assertEquals(15, WineMaking.FERMENTATION_BATCHES)
            assertEquals(60_000, WineMaking.MAX_UNFERMENTED)
            assertEquals(200.0, WineMaking.XP_PER_SUCCESS)
            for (recipe in WineRecipes.all) {
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(recipe.grapes.id, objs.jug_water.id),
                    )
                )
            }
        }

    @Test
    fun GameTestState
        .`wine making is three then two ticks and fermentation resets for the batch`() =
        runCookingTest { deps ->
            preparePlayer(level = 68, members = false)
            player.inv[4] = InvObj(objs.grapes)
            player.inv[5] = InvObj(objs.grapes)
            player.inv[12] = InvObj(objs.jug_water)
            player.inv[13] = InvObj(objs.jug_water)
            random.next = 99
            random.then = 99

            player.withProtectedAccess {
                deps.wineMaking.start(this, 4, 12, WineRecipes.regular, quantity = 2)
            }
            assertEquals(seqs.human_make_wine.id, player.pendingSequence.id)
            advance(2)
            assertEquals(2, player.count(objs.grapes))
            assertEquals(0, player.count(objs.unfermented_wine))
            advance()
            assertEquals(1, player.count(objs.grapes))
            assertEquals(1, player.count(objs.unfermented_wine))
            assertMessageSent(WineMaking.SQUEEZE_MESSAGE)
            advance()
            assertEquals(1, player.count(objs.unfermented_wine))
            advance()
            assertEquals(0, player.count(objs.grapes))
            assertEquals(2, player.count(objs.unfermented_wine))
            assertEquals(0, cookingXp())

            advance(19)
            assertEquals(2, player.count(objs.unfermented_wine))
            assertEquals(0, player.count(objs.jug_of_wine))
            advance()
            assertEquals(0, player.count(objs.unfermented_wine))
            assertEquals(2, player.count(objs.jug_of_wine))
            assertEquals(4000, cookingXp())
        }

    @Test
    fun GameTestState.`wine fermentation groups large stacks into fifteen rolls`() =
        runCookingTest { deps ->
            preparePlayer(level = 35)
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            bank[10] = InvObj(objs.unfermented_wine, 32)
            repeat(8) { random.next = 0 }
            repeat(7) { random.next = 99 }
            random.next = 0
            random.then = 99

            player.withProtectedAccess { deps.wineMaking.ferment(this) }

            assertEquals(0, bank.count(deps.objs[objs.unfermented_wine]))
            assertEquals(17, bank.count(deps.objs[objs.jug_of_wine]))
            assertEquals(15, bank.count(deps.objs[objs.jug_of_bad_wine]))
            assertEquals(34_000, cookingXp())
        }

    @Test
    fun GameTestState.`all inventory and bank wines ferment together with delayed xp`() =
        runCookingTest { deps ->
            preparePlayer(level = 99)
            player.inv[4] = InvObj(objs.unfermented_wine)
            player.inv[12] = InvObj(objs.zamoraks_unfermented_wine)
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            bank[20] = InvObj(objs.unfermented_wine, 30)
            bank[30] = InvObj(objs.zamoraks_unfermented_wine, 15)
            repeat(19) { random.next = 99 }

            player.withProtectedAccess { timer(cooking_timers.wineFermentation, 20) }
            advance(19)
            assertEquals(
                31,
                player.count(objs.unfermented_wine) + bank.count(deps.objs[objs.unfermented_wine]),
            )
            assertEquals(
                16,
                player.count(objs.zamoraks_unfermented_wine) +
                    bank.count(deps.objs[objs.zamoraks_unfermented_wine]),
            )
            assertEquals(0, cookingXp())
            advance()

            assertEquals(1, player.count(objs.jug_of_wine))
            assertEquals(1, player.count(objs.wine_of_zamorak))
            assertEquals(30, bank.count(deps.objs[objs.jug_of_wine]))
            assertEquals(15, bank.count(deps.objs[objs.wine_of_zamorak]))
            assertEquals(0, bank.count(deps.objs[objs.jug_of_bad_wine]))
            assertEquals(94_000, cookingXp())
        }

    @Test
    fun GameTestState.`Zamorak wine gates membership boosts and its no failure boundary`() =
        runCookingTest { deps ->
            preparePlayer(level = 65, members = false)
            player.inv[4] = InvObj(objs.zamoraks_grapes)
            player.inv[12] = InvObj(objs.jug_water)
            player.withProtectedAccess {
                deps.wineMaking.start(this, 4, 12, WineRecipes.zamorak, quantity = 1)
            }
            assertMessageSent(WineMaking.MEMBERS_MESSAGE)
            assertFalse(cooking_queues.wineMaking in player.weakQueueList)

            preparePlayer(level = 64)
            player.inv[4] = InvObj(objs.zamoraks_grapes)
            player.inv[12] = InvObj(objs.jug_water)
            player.withProtectedAccess {
                deps.wineMaking.start(this, 4, 12, WineRecipes.zamorak, quantity = 1)
            }
            assertMessageSent("You need a Cooking level of 65 to make this.")

            preparePlayer(level = 64)
            player.setCurrentLevel(stats.cooking, 65)
            player.inv[4] = InvObj(objs.zamoraks_grapes)
            player.inv[12] = InvObj(objs.jug_water)
            player.withProtectedAccess {
                deps.wineMaking.start(this, 4, 12, WineRecipes.zamorak, quantity = 1)
            }
            advance(3)
            assertEquals(1, player.count(objs.zamoraks_unfermented_wine))
            assertEquals(0, cookingXp())

            preparePlayer(level = 97)
            add(objs.zamoraks_unfermented_wine)
            random.next = 99
            player.withProtectedAccess { deps.wineMaking.ferment(this) }
            assertEquals(1, player.count(objs.jug_of_bad_wine))
            assertEquals(0, cookingXp())

            preparePlayer(level = 98)
            add(objs.zamoraks_unfermented_wine)
            random.next = 99
            player.withProtectedAccess { deps.wineMaking.ferment(this) }
            assertEquals(1, player.count(objs.wine_of_zamorak))
            assertEquals(2000, cookingXp())
        }

    @Test
    fun GameTestState.`shared wine cap and full bank fermentation remain atomic`() =
        runCookingTest { deps ->
            preparePlayer(level = 99)
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            bank[10] = InvObj(objs.unfermented_wine, 59_999)
            player.inv[0] = InvObj(objs.zamoraks_unfermented_wine)
            player.inv[4] = InvObj(objs.grapes)
            player.inv[12] = InvObj(objs.jug_water)
            var maximum = -1
            player.withProtectedAccess {
                maximum = deps.wineMaking.maximumActions(this, WineRecipes.regular)
            }
            assertEquals(0, maximum)
            player.withProtectedAccess {
                deps.wineMaking.start(this, 4, 12, WineRecipes.regular, quantity = 1)
            }
            assertFalse(cooking_queues.wineMaking in player.weakQueueList)
            assertEquals(1, player.count(objs.grapes))

            preparePlayer(level = 35)
            val fullBank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            fullBank.indices.forEach { fullBank[it] = InvObj(objs.logs) }
            fullBank[10] = InvObj(objs.unfermented_wine, 30)
            repeat(8) { random.next = 0 }
            repeat(7) { random.next = 99 }
            player.withProtectedAccess { deps.wineMaking.ferment(this) }
            assertEquals(30, fullBank.count(deps.objs[objs.unfermented_wine]))
            assertEquals(0, fullBank.count(deps.objs[objs.jug_of_wine]))
            assertEquals(0, cookingXp())

            fullBank[11] = null
            advance()
            assertEquals(0, fullBank.count(deps.objs[objs.unfermented_wine]))
            assertEquals(16, fullBank.count(deps.objs[objs.jug_of_wine]))
            assertEquals(14, fullBank.count(deps.objs[objs.jug_of_bad_wine]))
            assertEquals(32_000, cookingXp())
        }

    @Test
    fun GameTestState.`batch results follow the one three then four tick cadence`() =
        runCookingTest { deps ->
            preparePlayer(level = 99)
            add(objs.raw_tuna, 3)
            val range = prepareSource(cooking_locs.range)
            random.next = 0
            random.then = 0

            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    range,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(objs.raw_tuna.id),
                    quantity = 3,
                )
            }

            advance()
            assertEquals(1, player.count(objs.tuna))
            advance(2)
            assertEquals(1, player.count(objs.tuna))
            advance()
            assertEquals(2, player.count(objs.tuna))
            advance(3)
            assertEquals(2, player.count(objs.tuna))
            advance()
            assertEquals(3, player.count(objs.tuna))
            assertEquals(3000, cookingXp())
        }

    @Test
    fun GameTestState.`failure burns atomically and awards no experience`() =
        runCookingTest { deps ->
            preparePlayer(level = 1)
            add(objs.raw_sardine)
            val fire = prepareSource(cooking_locs.fire)
            random.next = 99

            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    fire,
                    CookingSourceKind.Fire,
                    StandardCookingRecipes.byRaw.getValue(objs.raw_sardine.id),
                    quantity = 1,
                )
            }
            advance()

            assertEquals(0, player.count(objs.raw_sardine))
            assertEquals(1, player.count(objs.burnt_sardine))
            assertEquals(0, cookingXp())
            assertMessageSent("You accidentally burn the sardine.")
        }

    @Test
    fun GameTestState.`source and equipment policies select the cutoff curves`() =
        runCookingTest { deps ->
            val lobster = StandardCookingRecipes.byRaw.getValue(objs.raw_lobster.id)
            val swordfish = StandardCookingRecipes.byRaw.getValue(objs.raw_swordfish.id)
            val shark = StandardCookingRecipes.byRaw.getValue(objs.raw_shark.id)
            assertEquals(
                CookingChance(38, 332),
                deps.cooking.chanceFor(lobster, CookingSourceKind.Fire, false),
            )
            assertEquals(
                CookingChance(55, 368),
                deps.cooking.chanceFor(lobster, CookingSourceKind.Range, true),
            )
            assertEquals(
                CookingChance(67, 380),
                deps.cooking.chanceFor(lobster, CookingSourceKind.Hosidius, true),
            )
            assertEquals(
                CookingChance(80, 393),
                deps.cooking.chanceFor(lobster, CookingSourceKind.Hosidius, true, true),
            )
            assertEquals(
                CookingChance(18, 292),
                deps.cooking.chanceFor(swordfish, CookingSourceKind.Fire, false),
            )
            assertEquals(
                CookingChance(30, 310),
                deps.cooking.chanceFor(swordfish, CookingSourceKind.Range, false),
            )
            assertEquals(
                CookingChance(42, 322),
                deps.cooking.chanceFor(swordfish, CookingSourceKind.Hosidius, true),
            )
            assertEquals(
                CookingChance(1, 202),
                deps.cooking.chanceFor(shark, CookingSourceKind.Fire, false),
            )
            assertEquals(
                CookingChance(40, 295),
                deps.cooking.chanceFor(shark, CookingSourceKind.Hosidius, true, true),
            )
        }

    @Test
    fun GameTestState.`cape diary membership level and movement rules are enforced`() =
        runCookingTest { deps ->
            preparePlayer(level = 80)
            add(objs.raw_shark)
            player.worn[1] = InvObj(objs.cooking_skillcape)
            val range = prepareSource(cooking_locs.range)
            random.next = 99
            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    range,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(objs.raw_shark.id),
                    1,
                )
            }
            advance()
            assertEquals(1, player.count(objs.shark))
            assertEquals(2100, cookingXp())

            preparePlayer(level = 99)
            add(objs.raw_lobster)
            val hosidius = prepareSource(cooking_locs.hosidiusRange, offset = 20)
            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    hosidius,
                    CookingSourceKind.Hosidius,
                    StandardCookingRecipes.byRaw.getValue(objs.raw_lobster.id),
                    1,
                )
            }
            assertMessageSent(
                "You need to complete the Easy Kourend & Kebos Diary to use this range."
            )
            assertEquals(1, player.count(objs.raw_lobster))

            preparePlayer(level = 61, members = true)
            add(objs.raw_monkfish)
            val memberRange = prepareSource(cooking_locs.range, offset = 40)
            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    memberRange,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(objs.raw_monkfish.id),
                    1,
                )
            }
            assertMessageSent("You need a Cooking level of 62 to cook this food.")

            preparePlayer(level = 99, members = false)
            add(objs.raw_monkfish)
            val freeRange = prepareSource(cooking_locs.range, offset = 60)
            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    freeRange,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(objs.raw_monkfish.id),
                    1,
                )
            }
            assertMessageSent("You need to be on a members' world to cook this food.")

            preparePlayer(level = 99)
            add(objs.raw_tuna, 2)
            val movingRange = prepareSource(cooking_locs.range, offset = 80)
            random.next = 0
            player.withProtectedAccess {
                deps.cooking.start(
                    this,
                    movingRange,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(objs.raw_tuna.id),
                    2,
                )
            }
            player.placeAt(movingRange.coords.translateX(5))
            advance()
            assertEquals(2, player.count(objs.raw_tuna))
            assertFalse(cooking_queues.standardFood in player.weakQueueList)
        }

    @Test
    fun GameTestState.`karambwan poison path requires its cache setting warning`() =
        runCookingTest { deps ->
            preparePlayer(level = 1)
            add(objs.raw_karambwan)
            val range = prepareSource(cooking_locs.range)

            useOnSource(deps, range, slot = 0, objs.raw_karambwan)
            val warning = client.filterIsInstance<RunClientScript>().single { it.id == 58 }
            assertEquals(
                listOf("This will poison the Karambwan. Do you still want to cook it?", "No.|Yes."),
                warning.values,
            )
            resumeChoice(option = 1)
            assertEquals(1, player.count(objs.raw_karambwan))
            assertEquals(0, cookingXp())
        }

    @Test
    fun GameTestState.`karambwan warning setting bypasses confirmation in a full inventory`() =
        runCookingTest { deps ->
            preparePlayer(level = 1)
            add(objs.raw_karambwan)
            repeat(27) { add(objs.logs) }
            player.setVarBit(varbits.poison_karambwan_warning_disabled, 1)
            val range = prepareSource(cooking_locs.range)
            random.next = 0
            useOnSource(deps, range, slot = 0, objs.raw_karambwan)
            advance()
            assertEquals(0, player.count(objs.raw_karambwan))
            assertEquals(1, player.count(objs.poison_karambwan))
            assertEquals(27, player.count(objs.logs))
            assertEquals(800, cookingXp())
            assertMessageSent("You cook the karambwan. It looks very dodgy.")
        }

    @Test
    fun GameTestState.`karambwan rejects free worlds`() = runCookingTest { deps ->
        preparePlayer(level = 99, members = false)
        add(objs.raw_karambwan)
        val range = prepareSource(cooking_locs.range)
        useOnSource(deps, range, slot = 0, objs.raw_karambwan)
        assertEquals(1, player.count(objs.raw_karambwan))
        assertMessageSent("You need to be on a members' world to cook this food.")
    }

    @Test
    fun GameTestState
        .`Tinsay reward unlocks cooked-first karambwan selection at visible level one`() =
        runCookingTest { deps ->
            preparePlayer(level = 1)
            player.setVarp(varps.tbwt_main, 6)
            assertEquals(listOf(KarambwanProduct.Poison), deps.karambwan.availableProducts(player))
            player.setVarp(varps.tbwt_main, 5)
            player.setVarBit(varbits.tbwt_tinsay_reward_claimed, 1)
            assertEquals(listOf(KarambwanProduct.Poison), deps.karambwan.availableProducts(player))

            unlockThoroughKarambwan()
            add(objs.raw_karambwan, 2)
            val range = prepareSource(cooking_locs.range)
            useOnSource(deps, range, slot = 0, objs.raw_karambwan)

            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    1,
                    2,
                    objs.cooked_karambwan.id,
                    objs.poison_karambwan.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    2,
                    "How would you like to cook your karambwan?",
                ),
                setup.values,
            )
            random.next = 0
            random.then = 0
            resumeSkillMulti(components.skillmulti_a, quantity = 2)
            advance()
            assertEquals(1, player.count(objs.cooked_karambwan))
            assertEquals(1900, cookingXp())
            assertEquals(1, player.vars[varbits.karamja_diary_hard_karambwan])
            advance(3)
            assertEquals(2, player.count(objs.cooked_karambwan))
            assertEquals(3800, cookingXp())
            assertMessageSent("You cook the karambwan. It looks delicious.")
        }

    @Test
    fun GameTestState.`unlocked karambwan poison choice warns and awards only poison experience`() =
        runCookingTest { deps ->
            preparePlayer(level = 30)
            unlockThoroughKarambwan()
            add(objs.raw_karambwan)
            val range = prepareSource(cooking_locs.range)

            useOnSource(deps, range, slot = 0, objs.raw_karambwan)
            resumeSkillMulti(components.skillmulti_b, quantity = 1)
            val warning = client.filterIsInstance<RunClientScript>().single { it.id == 58 }
            assertEquals("No.|Yes.", warning.values[1])
            random.next = 0
            resumeChoice(option = 2)
            advance()

            assertEquals(0, player.count(objs.raw_karambwan))
            assertEquals(0, player.count(objs.burnt_karambwan))
            assertEquals(1, player.count(objs.poison_karambwan))
            assertEquals(800, cookingXp())
            assertEquals(0, player.vars[varbits.karamja_diary_hard_karambwan])
        }

    @Test
    fun GameTestState.`karambwan pins curves burns cape immunity cadence and interruption`() =
        runCookingTest { deps ->
            assertEquals(CookingChance(70, 255), deps.karambwan.chanceFor(CookingSourceKind.Fire))
            assertEquals(CookingChance(70, 255), deps.karambwan.chanceFor(CookingSourceKind.Range))
            assertEquals(
                CookingChance(70, 255),
                deps.karambwan.chanceFor(CookingSourceKind.Lumbridge),
            )
            assertEquals(
                CookingChance(82, 267),
                deps.karambwan.chanceFor(CookingSourceKind.Hosidius),
            )
            assertEquals(
                CookingChance(95, 280),
                deps.karambwan.chanceFor(CookingSourceKind.Hosidius, eliteHosidius = true),
            )

            preparePlayer(level = 1)
            unlockThoroughKarambwan()
            add(objs.raw_karambwan, 2)
            val fire = prepareSource(cooking_locs.fire)
            random.next = 99
            random.then = 0
            player.withProtectedAccess {
                deps.karambwan.start(
                    this,
                    fire,
                    CookingSourceKind.Fire,
                    KarambwanProduct.Thorough,
                    quantity = 2,
                )
            }
            advance()
            assertEquals(1, player.count(objs.burnt_karambwan))
            assertEquals(0, cookingXp())
            assertEquals(0, player.vars[varbits.karamja_diary_hard_karambwan])
            advance(3)
            assertEquals(1, player.count(objs.cooked_karambwan))
            assertEquals(1900, cookingXp())

            preparePlayer(level = 1)
            unlockThoroughKarambwan()
            add(objs.raw_karambwan, 2)
            player.worn[1] = InvObj(objs.cooking_skillcape)
            val capeFire = prepareSource(cooking_locs.fire, offset = 20)
            random.next = 99
            player.withProtectedAccess {
                deps.karambwan.start(
                    this,
                    capeFire,
                    CookingSourceKind.Fire,
                    KarambwanProduct.Thorough,
                    quantity = 2,
                )
            }
            advance()
            assertEquals(1, player.count(objs.cooked_karambwan))
            player.withProtectedAccess {
                deps.karambwan.start(
                    this,
                    capeFire,
                    CookingSourceKind.Fire,
                    KarambwanProduct.Thorough,
                    quantity = 1,
                )
            }
            advance()
            assertEquals(2, player.count(objs.cooked_karambwan))
            assertFalse(cooking_queues.karambwan in player.weakQueueList)

            preparePlayer(level = 30)
            unlockThoroughKarambwan()
            add(objs.raw_karambwan, 2)
            val movingRange = prepareSource(cooking_locs.range, offset = 40)
            player.withProtectedAccess {
                deps.karambwan.start(
                    this,
                    movingRange,
                    CookingSourceKind.Range,
                    KarambwanProduct.Thorough,
                    quantity = 2,
                )
            }
            player.placeAt(movingRange.coords.translateX(5))
            advance()
            assertEquals(2, player.count(objs.raw_karambwan))
            assertFalse(cooking_queues.karambwan in player.weakQueueList)
        }

    @Test
    fun GameTestState
        .`karambwan participates in range menu and burnt octopus cannot be reheated`() =
        runCookingTest { deps ->
            preparePlayer(level = 30)
            unlockThoroughKarambwan()
            add(objs.raw_shrimps)
            add(objs.raw_karambwan)
            val range = prepareSource(cooking_locs.range)
            player.withProtectedAccess { deps.cooking.select(this, range, CookingSourceKind.Range) }
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertTrue(setup.values.contains(objs.shrimps.id))
            assertTrue(setup.values.contains(objs.cooked_karambwan.id))
            assertTrue(setup.values.contains(objs.poison_karambwan.id))

            resumeSkillMulti(components.skillmulti_a, quantity = 1)
            advance()
            preparePlayer(level = 30)
            add(objs.burnt_karambwan)
            val reheatRange = prepareSource(cooking_locs.range, offset = 20)
            useOnSource(deps, reheatRange, slot = 0, objs.burnt_karambwan)
            assertEquals(1, player.count(objs.burnt_karambwan))
            assertMessageSent("There is nothing to be gained from heating a burnt octopus.")
        }

    @Test
    fun GameTestState.`sinew range choice pins inputs curves experience and cadence`() =
        runCookingTest { deps ->
            assertEquals(CookingChance(128, 512), deps.sinew.chanceFor(CookingSourceKind.Range))
            assertEquals(CookingChance(128, 512), deps.sinew.chanceFor(CookingSourceKind.Lumbridge))
            assertEquals(CookingChance(140, 524), deps.sinew.chanceFor(CookingSourceKind.Hosidius))
            assertEquals(
                CookingChance(153, 537),
                deps.sinew.chanceFor(CookingSourceKind.Hosidius, eliteHosidius = true),
            )

            preparePlayer(level = 1)
            add(objs.raw_beef, 3)
            repeat(25) { add(objs.logs) }
            val range = prepareSource(cooking_locs.range)
            random.next = 0
            random.then = 0
            useOnSource(deps, range, slot = 0, objs.raw_beef)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    1,
                    3,
                    objs.cooked_meat.id,
                    objs.sinew.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    3,
                    "What would you like to cook?",
                ),
                setup.values,
            )
            resumeSkillMulti(components.skillmulti_b, quantity = 3)
            assertEquals(1, player.count(objs.sinew))
            assertEquals(30, cookingXp())
            advance(3)
            assertEquals(2, player.count(objs.sinew))
            advance(4)
            assertEquals(3, player.count(objs.sinew))
            assertEquals(90, cookingXp())
            assertEquals(25, player.count(objs.logs))
            assertMessageSent("You dry a piece of meat and extract the sinew.")
        }

    @Test
    fun GameTestState.`sinew enforces source membership burns capes and interruption`() =
        runCookingTest { deps ->
            for (raw in listOf(objs.raw_beef, objs.raw_bear_meat, objs.raw_boar_meat)) {
                preparePlayer(level = 1)
                add(raw)
                val range = prepareSource(cooking_locs.range, offset = raw.id % 100)
                random.next = 0
                player.withProtectedAccess {
                    deps.sinew.start(this, range, CookingSourceKind.Range, raw, quantity = 1)
                }
                assertEquals(1, player.count(objs.sinew), raw.toString())
            }

            preparePlayer(level = 1)
            add(objs.raw_beef)
            val fire = prepareSource(cooking_locs.fire, offset = 120)
            player.withProtectedAccess {
                deps.sinew.start(this, fire, CookingSourceKind.Fire, objs.raw_beef, quantity = 1)
            }
            assertEquals(1, player.count(objs.raw_beef))
            assertMessageSent("You need a range to extract sinew from this meat.")

            preparePlayer(level = 1, members = false)
            add(objs.raw_beef)
            val freeRange = prepareSource(cooking_locs.range, offset = 140)
            player.withProtectedAccess {
                deps.sinew.start(
                    this,
                    freeRange,
                    CookingSourceKind.Range,
                    objs.raw_beef,
                    quantity = 1,
                )
            }
            assertEquals(1, player.count(objs.raw_beef))
            assertMessageSent("You need to be on a members' world to make sinew.")

            preparePlayer(level = 1)
            add(objs.raw_bear_meat)
            val burnRange = prepareSource(cooking_locs.range, offset = 160)
            random.next = 99
            player.withProtectedAccess {
                deps.sinew.start(
                    this,
                    burnRange,
                    CookingSourceKind.Range,
                    objs.raw_bear_meat,
                    quantity = 1,
                )
            }
            assertEquals(1, player.count(objs.burnt_meat))
            assertEquals(0, cookingXp())

            preparePlayer(level = 1)
            add(objs.raw_boar_meat, 2)
            player.worn[1] = InvObj(objs.cooking_skillcape)
            val capeRange = prepareSource(cooking_locs.range, offset = 180)
            random.next = 99
            player.withProtectedAccess {
                deps.sinew.start(
                    this,
                    capeRange,
                    CookingSourceKind.Range,
                    objs.raw_boar_meat,
                    quantity = 2,
                )
            }
            assertEquals(1, player.count(objs.sinew))
            player.placeAt(capeRange.coords.translateX(5))
            advance(3)
            assertEquals(1, player.count(objs.raw_boar_meat))
            assertFalse(cooking_queues.sinew in player.weakQueueList)
        }

    private fun GameTestScope.preparePlayer(level: Int, members: Boolean = true) {
        player.clearAllInvs()
        player.observerUUID = 1L
        player.members = members
        player.setBaseLevel(stats.cooking, level)
        player.setCurrentLevel(stats.cooking, level)
        player.statMap.setXP(stats.cooking, 0)
        player.setVarBit(varbits.kourend_diary_easy_complete, 0)
        player.setVarBit(varbits.kourend_diary_elite_complete, 0)
        player.setVarBit(varbits.ogre_spit_roaster, 0)
        player.setVarBit(varbits.rfd_skrach_uglogwee, 0)
        player.setVarp(varps.tbwt_main, 0)
        player.setVarp(varps.tbwt_tiadeche, 0)
        player.setVarBit(varbits.tbwt_tinsay_reward_claimed, 0)
        player.setVarBit(varbits.poison_karambwan_warning_disabled, 0)
        player.setVarBit(varbits.karamja_diary_hard_karambwan, 0)
        player.weakQueueList.clear()
        player.clearTimer(cooking_timers.wineFermentation)
        player.clearSoftTimer(cooking_timers.ogreSpitRoastState)
        client.clear()
    }

    private fun GameTestScope.prepareSource(type: LocType, offset: Int = 0): BoundLocInfo {
        val source =
            placeMapLoc(
                CoordGrid(2000 + offset, 2000, 0),
                type,
                LocShape.CentrepieceStraight,
                LocAngle.West,
            )
        player.placeAt(source.coords.translateX(-1))
        return source
    }

    private fun GameTestScope.useOnSource(
        deps: Dependencies,
        source: BoundLocInfo,
        slot: Int,
        obj: ObjType,
    ) {
        player.withProtectedAccess {
            deps.locU.interactOp(
                this,
                source,
                source,
                locTypes.getValue(source.id),
                objTypes[obj],
                player.inv,
                slot,
            )
        }
    }

    private fun GameTestScope.churnAt(deps: Dependencies, churn: BoundLocInfo) {
        val event =
            deps.locInteractions.opTrigger(
                player,
                churn,
                InteractionOp.Op1,
                locTypes.getValue(churn.id),
            )
        assertNotNull(event)
        player.withProtectedAccess { deps.events.publish(this, requireNotNull(event)) }
    }

    private fun GameTestScope.useTogether(
        deps: Dependencies,
        first: ObjType,
        firstSlot: Int,
        second: ObjType,
        secondSlot: Int,
        reverse: Boolean = false,
    ) {
        player.withProtectedAccess {
            if (reverse) {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[second],
                    secondSlot,
                    deps.objs[first],
                    firstSlot,
                )
            } else {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[first],
                    firstSlot,
                    deps.objs[second],
                    secondSlot,
                )
            }
        }
    }

    private fun GameTestScope.visibleSpit(deps: Dependencies, source: BoundLocInfo): BoundLocInfo =
        requireNotNull(
            deps.locU.multiLoc(source, deps.locs[cooking_locs.ogreSpitRoast], player.vars)
        )

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestScope.cookingXp(): Int = player.statMap.getFineXP(stats.cooking)

    private fun GameTestScope.groundCount(
        deps: Dependencies,
        obj: ObjType,
        coords: CoordGrid,
    ): Int = deps.objRepo.findAll(coords).count { it.type == obj.id }

    private fun GameTestScope.resumeSkillMulti(component: ComponentType, quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = quantity))
    }

    private fun GameTestScope.resumeChoice(option: Int) {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = option)
        )
    }

    private fun GameTestScope.unlockThoroughKarambwan() {
        player.setVarp(varps.tbwt_main, 6)
        player.setVarBit(varbits.tbwt_tinsay_reward_claimed, 1)
    }

    private fun GameTestState.runCookingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, CookingScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(CookingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val cooking: StandardCooking,
        val karambwan: KarambwanCooking,
        val sinew: SinewCooking,
        val ironSpit: IronSpitCooking,
        val chompy: ChompyFireCooking,
        val ogreSpitRoast: OgreSpitRoast,
        val foodPreparation: FoodPreparation,
        val dairyChurning: DairyChurning,
        val wineMaking: WineMaking,
        val heldU: HeldUInteractions,
        val locInteractions: LocInteractions,
        val locU: LocUInteractions,
        val objs: ObjTypeList,
        val locs: LocTypeList,
        val seqs: SeqTypeList,
        val types: TypeListMap,
        val dbRows: DbRowTypeList,
        val objRepo: ObjRepository,
        val events: EventBus,
    )

    private data class RecipeContract(
        val raw: Int,
        val cooked: Int,
        val burnt: Int?,
        val level: Int,
        val xp: Double,
        val members: Boolean,
    )

    private data class WineContract(
        val grapes: Int,
        val unfermented: Int,
        val fermented: Int,
        val level: Int,
        val members: Boolean,
        val low: Int,
        val high: Int,
        val noFailureLevel: Int,
    )

    private data class FishCurve(
        val raw: ObjType,
        val fire: CookingChance,
        val range: CookingChance,
        val lumbridge: CookingChance,
        val hosidius: CookingChance,
        val hosidiusElite: CookingChance,
    )

    private data class VegetableHeatCase(
        val raw: ObjType,
        val cooked: ObjType,
        val burnt: ObjType,
        val level: Int,
        val xp: Double,
        val noBurnLevel: Int,
        val ordinary: CookingChance,
        val hosidius: CookingChance,
        val elite: CookingChance,
        val firstTicks: Int = 1,
    )

    private data class PieHeatCase(
        val raw: ObjType,
        val cooked: ObjType,
        val level: Int,
        val xp: Double,
        val members: Boolean,
        val range: CookingChance,
        val lumbridge: CookingChance? = null,
        val hosidius: CookingChance,
        val elite: CookingChance,
        val rangeNoBurnLevel: Int? = null,
        val lumbridgeNoBurnLevel: Int? = null,
        val hosidiusNoBurnLevel: Int? = null,
    )

    private data class PiePreparationContract(
        val first: Int,
        val second: Int,
        val output: Int,
        val level: Int,
        val members: Boolean,
        val firstTicks: Int,
        val returnedSecond: Int? = null,
    )

    private data class PotatoPreparationContract(
        val first: Int,
        val second: Int,
        val output: Int,
        val level: Int,
        val xp: Double,
        val ticks: Int,
        val returnedSecond: Int? = null,
        val tool: Int? = null,
    )

    private data class DairyChurningContract(
        val input: Int,
        val output: Int,
        val level: Int,
        val xp: Double,
        val ticks: Int,
        val playerAnimation: Int,
        val regularChurnAnimation: Int,
        val metalChurnAnimation: Int,
        val returnsBucket: Boolean,
    )

    private data class NoBurnCase(
        val recipe: StandardCookingRecipe,
        val rangeLevel: Int,
        val hosidiusLevel: Int,
    )

    private fun StandardCookingRecipe.resultCadence(): List<Int> =
        listOf(firstResultTicks, secondResultGapTicks, repeatResultGapTicks)
}
