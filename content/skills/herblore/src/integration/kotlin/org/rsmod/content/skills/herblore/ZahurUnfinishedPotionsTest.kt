package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.back
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class ZahurUnfinishedPotionsTest {
    @Test
    fun GameTestState.`all sixteen water and blood recipes accept noted ingredients without XP`() =
        test { d ->
            player.members = true
            player.withProtectedAccess {
                vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).complete] = 1
            }
            for (recipe in
                UnfinishedPotionRecipes.all.filter {
                    it.base == objs.vial_of_water || it.base == objs.vial_of_blood
                }) {
                player.clearInv()
                player.inv[0] = InvObj(d.types.getValue(d.types[recipe.herb].certlink), 4)
                player.inv[1] = InvObj(d.types.getValue(d.types[recipe.base].certlink), 3)
                player.inv[2] = InvObj(objs.coins, 600)
                val xp = player.statMap.getFineXP(stats.herblore)
                player.withProtectedAccess { assertEquals(3, d.service.make(this)) }
                assertEquals(3, player.count(d.types.getValue(d.types[recipe.output].certlink)))
                assertEquals(1, player.count(d.types.getValue(d.types[recipe.herb].certlink)))
                assertEquals(0, player.count(objs.coins))
                assertEquals(xp, player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`unnoted materials become noted outputs and use only affordable pairs`() =
        test { d ->
            player.members = true
            player.back = InvObj(ZahurCapes.normal)
            player.inv[0] = InvObj(objs.torstol)
            player.inv[1] = InvObj(objs.torstol)
            player.inv[2] = InvObj(objs.vial_of_water)
            player.inv[3] = InvObj(objs.vial_of_water)
            player.inv[4] = InvObj(objs.coins, 399)
            player.withProtectedAccess { assertEquals(1, d.service.make(this)) }
            assertEquals(1, player.count(objs.torstol))
            assertEquals(1, player.count(objs.vial_of_water))
            assertEquals(199, player.count(objs.coins))
            assertEquals(
                1,
                player.count(d.types.getValue(d.types[objs.torstol_potion_unf].certlink)),
            )
        }

    @Test
    fun GameTestState.`service requires completed hard tier or actually worn cape`() = test { d ->
        player.members = true
        player.inv[0] = InvObj(objs.guam_leaf)
        player.inv[1] = InvObj(objs.vial_of_water)
        player.inv[2] = InvObj(objs.coins, 200)
        player.inv[3] = InvObj(ZahurCapes.normal)
        player.withProtectedAccess { assertEquals(0, d.service.make(this)) }
        assertEquals(200, player.count(objs.coins))
        player.back = InvObj(ZahurCapes.trimmed)
        player.withProtectedAccess { assertEquals(1, d.service.make(this)) }
        assertEquals(0, player.count(objs.coins))
    }

    @Test
    fun GameTestState.`native make operation works and ignores grimy herbs and coconut recipes`() =
        test { d ->
            player.members = true
            player.withProtectedAccess {
                vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).complete] = 1
            }
            player.inv[0] = InvObj(objs.cadantine)
            player.inv[1] = InvObj(objs.vial_of_blood)
            player.inv[2] = InvObj(objs.coins, 1000)
            player.inv[3] = InvObj(objs.grimy_guam_leaf)
            player.inv[4] = InvObj(objs.vial_of_water)
            player.inv[5] = InvObj(objs.toadflax)
            player.inv[6] = InvObj(objs.vial_coconut_milk)
            val npc =
                spawnNpc(player.coords.translate(1, 0), d.npcs[ZahurNpcs.zahur]) { noneMode() }
            assertEquals("Make unfinished potion(s)", d.npcs[ZahurNpcs.zahur].op[4])
            player.opNpc5(npc)
            advance(2)
            assertEquals(
                1,
                player.count(d.types.getValue(d.types[objs.cadantine_blood_potion_unf].certlink)),
            )
            assertEquals(1, player.count(objs.grimy_guam_leaf))
            assertEquals(1, player.count(objs.vial_coconut_milk))
            assertEquals(600, player.count(objs.coins))
        }

    private fun GameTestState.test(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            ZahurScript::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(val service: ZahurUnfinishedPotions, val types: ObjTypeList, val npcs: NpcTypeList)
}
