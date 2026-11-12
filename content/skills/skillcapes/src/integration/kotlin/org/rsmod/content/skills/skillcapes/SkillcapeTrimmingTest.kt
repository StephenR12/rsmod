package org.rsmod.content.skills.skillcapes

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.obj.plugin.ObjTakeModule
import org.rsmod.api.obj.plugin.ObjTakePlugin
import org.rsmod.api.player.interact.ObjInteractions
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTransmitLevel
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

class SkillcapeTrimmingTest {
    @Test
    fun GameTestState.`cache 233 ordinary skillcape pairs are complete and pinned`() =
        runTrimmingTest { deps ->
            assertEquals(
                listOf(
                    9747 to 9748,
                    9753 to 9754,
                    9750 to 9751,
                    9768 to 9769,
                    9756 to 9757,
                    9759 to 9760,
                    9762 to 9763,
                    9801 to 9802,
                    9807 to 9808,
                    9783 to 9784,
                    9798 to 9799,
                    9804 to 9805,
                    9780 to 9781,
                    9795 to 9796,
                    9792 to 9793,
                    9774 to 9775,
                    9771 to 9772,
                    9777 to 9778,
                    9786 to 9787,
                    9810 to 9811,
                    9765 to 9766,
                    9948 to 9949,
                    9789 to 9790,
                ),
                SkillcapeTrimming.capePairs.map { (plain, trimmed) ->
                    deps.objs[plain].id to deps.objs[trimmed].id
                },
            )
            val unexpectedBoostOps =
                SkillcapeTrimming.definitions
                    .flatMap { listOf(it.untrimmed, it.trimmed) }
                    .map { it.id to deps.objs[it].param(params.wear_op1) }
                    .filter { (_, op) -> op != "Boost" }
            assertEquals(listOf(9771 to "", 9772 to ""), unexpectedBoostOps)
            assertEquals(
                VarpTransmitLevel.Never,
                deps.varps[varps.skillcape_hitpoints_boost_ready_at].transmit,
            )
            assertEquals(65495, varps.skillcape_hitpoints_boost_ready_at.id)
            assertEquals(
                VarpLifetime.Perm,
                deps.varps[varps.skillcape_hitpoints_boost_ready_at].scope,
            )
            assertEquals(
                VarpTransmitLevel.Never,
                deps.varps[varps.skillcape_prayer_boost_ready_at].transmit,
            )
            assertEquals(65496, varps.skillcape_prayer_boost_ready_at.id)
            assertEquals(VarpLifetime.Perm, deps.varps[varps.skillcape_prayer_boost_ready_at].scope)
        }

    @Test
    fun GameTestState.`worn Boost raises every cache exposed non Hunter cape once`() =
        runTrimmingTest { deps ->
            for (definition in SkillcapeTrimming.definitions) {
                if (definition.stat == stats.hunter || definition.stat == stats.agility) {
                    continue
                }
                for (cape in listOf(definition.untrimmed, definition.trimmed)) {
                    clearSkillcapeCooldown(definition.stat)
                    player.stats[definition.stat] = definition.stat.maxLevel
                    player.worn[Wearpos.Back.slot] = InvObj(cape)

                    player.withProtectedAccess {
                        deps.worn.interact(this, player.worn, Wearpos.Back.slot, IfButtonOp.Op1)
                    }

                    assertEquals(
                        definition.stat.maxLevel + 1,
                        player.stat(definition.stat),
                        "${cape.id} did not boost ${definition.stat.id}",
                    )
                    player.stats[definition.stat] = definition.stat.maxLevel
                }
            }
        }

    @Test
    fun GameTestState.`equipping every ordinary cape applies its visible boost`() =
        runTrimmingTest { deps ->
            for (definition in SkillcapeTrimming.definitions) {
                for (cape in listOf(definition.untrimmed, definition.trimmed)) {
                    player.clearInv()
                    player.worn[Wearpos.Back.slot] = null
                    clearSkillcapeCooldown(definition.stat)
                    player.stats[definition.stat] = definition.stat.maxLevel
                    player.inv[0] = InvObj(cape)

                    player.withProtectedAccess { deps.equip.equip(player, 0, player.inv) }

                    assertEquals(
                        definition.stat.maxLevel + 1,
                        player.stat(definition.stat),
                        "${cape.id} did not boost ${definition.stat.id} on equip",
                    )
                    player.stats[definition.stat] = definition.stat.maxLevel
                }
            }
        }

    @Test
    fun GameTestState.`equipping the max cape inherits every ordinary visible boost`() =
        runTrimmingTest { deps ->
            for (definition in SkillcapeTrimming.definitions) {
                clearSkillcapeCooldown(definition.stat)
                player.stats[definition.stat] = definition.stat.maxLevel
            }
            player.inv[0] = InvObj(objs.max_cape_inv)

            player.withProtectedAccess { deps.equip.equip(player, 0, player.inv) }

            for (definition in SkillcapeTrimming.definitions) {
                assertEquals(
                    definition.stat.maxLevel + 1,
                    player.stat(definition.stat),
                    "Max cape did not inherit ${definition.stat.id} boost",
                )
            }
        }

    @Test
    fun GameTestState.`visible boosts do not stack and reactivate after returning to 99`() =
        runTrimmingTest { deps ->
            player.stats[stats.attack] = 99

            assertTrue(deps.boost.activate(player, stats.attack, nowSeconds = 1_000))
            assertEquals(100, player.stat(stats.attack))
            assertFalse(deps.boost.activate(player, stats.attack, nowSeconds = 1_001))
            assertEquals(100, player.stat(stats.attack))

            player.stats[stats.attack] = 99
            assertTrue(deps.boost.activate(player, stats.attack, nowSeconds = 1_001))
            assertEquals(100, player.stat(stats.attack))
        }

    @Test
    fun GameTestState.`Hitpoints and Prayer have independent exact sixty second reuse delays`() =
        runTrimmingTest { deps ->
            player.stats[stats.hitpoints] = 99
            player.stats[stats.prayer] = 99

            assertTrue(deps.boost.activate(player, stats.hitpoints, nowSeconds = 1_000))
            assertTrue(deps.boost.activate(player, stats.prayer, nowSeconds = 1_000))
            assertEquals(100, player.stat(stats.hitpoints))
            assertEquals(100, player.stat(stats.prayer))

            player.stats[stats.hitpoints] = 99
            player.stats[stats.prayer] = 99
            assertFalse(deps.boost.activate(player, stats.hitpoints, nowSeconds = 1_059))
            assertFalse(deps.boost.activate(player, stats.prayer, nowSeconds = 1_059))
            assertEquals(99, player.stat(stats.hitpoints))
            assertEquals(99, player.stat(stats.prayer))

            assertTrue(deps.boost.activate(player, stats.hitpoints, nowSeconds = 1_060))
            assertTrue(deps.boost.activate(player, stats.prayer, nowSeconds = 1_060))
            assertEquals(100, player.stat(stats.hitpoints))
            assertEquals(100, player.stat(stats.prayer))
        }

    @Test
    fun GameTestState.`second level 99 trims every ordinary cape in inventory`() =
        runTrimmingTest { deps ->
            SkillcapeTrimming.capePairs.forEachIndexed { slot, (plain, _) ->
                player.inv[slot] = InvObj(plain, vars = slot + 1)
            }
            player.stats[stats.attack] = 99
            player.stats[stats.hunter] = 98

            advanceHunterTo99()
            advance()

            SkillcapeTrimming.capePairs.forEachIndexed { slot, (_, trimmed) ->
                assertEquals(trimmed.id, player.inv[slot]?.id)
                assertEquals(slot + 1, player.inv[slot]?.vars)
            }
            assertEquals(99, player.stats[stats.hunter])
            assertTrue(deps.objs[objs.hunter_skillcape_t].id == player.inv[21]?.id)
        }

    @Test
    fun GameTestState.`second level 99 trims carried worn and banked Hunter capes`() =
        runTrimmingTest { deps ->
            val bank = player.invMap.getOrPut(deps.invs[invs.bank])
            player.inv[0] = InvObj(objs.hunter_skillcape, vars = 11)
            player.worn[Wearpos.Back.slot] = InvObj(objs.hunter_skillcape, vars = 22)
            bank[7] = InvObj(objs.hunter_skillcape, vars = 33)
            player.stats[stats.attack] = 99
            player.stats[stats.hunter] = 98

            advanceHunterTo99()
            advance()

            assertEquals(InvObj(objs.hunter_skillcape_t, vars = 11), player.inv[0])
            assertEquals(InvObj(objs.hunter_skillcape_t, vars = 22), player.worn[Wearpos.Back.slot])
            assertEquals(InvObj(objs.hunter_skillcape_t, vars = 33), bank[7])
        }

    @Test
    fun GameTestState.`first level 99 and sub 99 advances do not trim`() = runTrimmingTest {
        player.inv[0] = InvObj(objs.hunter_skillcape)
        player.stats[stats.hunter] = 98
        advanceHunterTo99()
        advance()
        assertEquals(objs.hunter_skillcape.id, player.inv[0]?.id)

        player.stats[stats.attack] = 97
        player.statAdvance(stats.attack, xpToNextLevel(97, 98), rate = 1.0, globalRate = 1.0)
        advance()
        assertEquals(objs.hunter_skillcape.id, player.inv[0]?.id)
    }

    @Test
    fun GameTestState.`ordinary ground Take trims a cape for a multi mastery player`() =
        runTrimmingTest { deps ->
            player.clearInv()
            player.stats[stats.attack] = 99
            player.stats[stats.hunter] = 99
            val target = deps.spawn(objs.hunter_skillcape, player.coords)

            deps.objInteractions.interact(player, target, InteractionOp.Op3)
            advance()

            assertEquals(objs.hunter_skillcape_t.id, player.inv[0]?.id)
            assertTrue(deps.objRepo.findAll(target.coords).none { it === target })
        }

    @Test
    fun GameTestState.`ordinary ground Take preserves a cape before second mastery`() =
        runTrimmingTest { deps ->
            player.clearInv()
            player.stats[stats.hunter] = 99
            val target = deps.spawn(objs.hunter_skillcape, player.coords)

            deps.objInteractions.interact(player, target, InteractionOp.Op3)
            advance()

            assertEquals(objs.hunter_skillcape.id, player.inv[0]?.id)
        }

    private fun GameTestScope.advanceHunterTo99() {
        player.statAdvance(stats.hunter, xpToNextLevel(98, 99), rate = 1.0, globalRate = 1.0)
    }

    private fun GameTestScope.clearSkillcapeCooldown(stat: org.rsmod.game.type.stat.StatType) {
        when (stat) {
            stats.hitpoints -> player.setVarp(varps.skillcape_hitpoints_boost_ready_at, 0)
            stats.prayer -> player.setVarp(varps.skillcape_prayer_boost_ready_at, 0)
            else -> Unit
        }
    }

    private fun xpToNextLevel(from: Int, to: Int): Double =
        (PlayerSkillXPTable.getXPFromLevel(to) - PlayerSkillXPTable.getXPFromLevel(from)).toDouble()

    private fun GameTestState.runTrimmingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            SkillcapeScript::class,
            ObjTakePlugin::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(ObjTakeModule())
            install(SkillcapeModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val objs: ObjTypeList,
        val invs: InvTypeList,
        val varps: VarpTypeList,
        val boost: SkillcapeBoost,
        val equip: HeldEquipOp,
        val worn: WornInteractions,
        val objInteractions: ObjInteractions,
        val objRepo: ObjRepository,
    ) {
        fun spawn(type: ObjType, coords: CoordGrid): Obj =
            objRepo.add(type = type, coords = coords, duration = Int.MAX_VALUE)
    }
}
