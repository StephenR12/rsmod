package org.rsmod.content.travel.fremennikseaboots

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class FremennikSeaBootsTeleportTest {
    @Test
    fun GameTestState.`cache 233 boots and inventory plus worn teleport hooks are pinned`() =
        runBootsTest { deps ->
            val expectedIds = listOf(13129, 13130, 13131, 13132)
            assertEquals(expectedIds, FremennikSeaBootsTier.entries.map { it.obj.id })
            assertEquals(listOf(1, 3, 5, null), FremennikSeaBootsTier.entries.map { it.dailyLimit })

            for (tier in FremennikSeaBootsTier.entries) {
                val type = deps.objs[tier.obj]
                assertEquals(
                    listOf(null, "Wear", "Teleport", null, null),
                    type.iop.toList(),
                    tier.name,
                )
                assertEquals("Teleport", type.param(params.wear_op1), tier.name)
                assertTrue(
                    deps.events.contains(HeldObjEvents.Op3::class.java, tier.obj.id.toLong()),
                    tier.name,
                )
                assertTrue(
                    deps.events.contains(WornObjEvents.Op2::class.java, tier.obj.id.toLong()),
                    tier.name,
                )
            }

            assertEquals(VarpLifetime.Perm, deps.varps[FremennikSeaBootsVarps.teleports_used].scope)
            assertEquals(
                VarpLifetime.Perm,
                deps.varps[FremennikSeaBootsVarps.last_teleport_day].scope,
            )
        }

    @Test
    fun GameTestState
        .`accepted teleport uses exact market destination and four tick standard effect`() =
        runBootsTest { deps ->
            prepare()
            val origin = player.coords
            player.withProtectedAccess {
                deps.teleport.teleport(this, FremennikSeaBootsTier.Easy, currentUtcDay = 100)
            }

            assertEquals(seqs.teleport_casting.id, player.pendingSequence.id)
            assertEquals(
                spotanims.teleport_casting.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(200, synths.teleport_all.id)
            advance(3)
            assertEquals(origin, player.coords)
            advance()
            assertEquals(FremennikSeaBootsTeleport.RELLEKKA_MARKET, player.coords)
            assertEquals(1, used(deps))
            assertEquals(100, lastDay(deps))
        }

    @Test
    fun GameTestState.`Poll 85 tiers enforce one three five and unlimited shared daily uses`() =
        runBootsTest { deps ->
            val limited =
                listOf(
                    FremennikSeaBootsTier.Easy to 1,
                    FremennikSeaBootsTier.Medium to 3,
                    FremennikSeaBootsTier.Hard to 5,
                )
            for ((tier, limit) in limited) {
                prepare()
                for (attempt in 1..limit) {
                    player.withProtectedAccess {
                        deps.teleport.teleport(this, tier, currentUtcDay = 200)
                    }
                    advance(FremennikSeaBootsTeleport.CAST_TICKS)
                    assertEquals(attempt, used(deps), tier.name)
                }

                val rejectedOrigin = CoordGrid(3200, 3200)
                player.teleport(rejectedOrigin)
                client.clear()
                player.withProtectedAccess {
                    deps.teleport.teleport(this, tier, currentUtcDay = 200)
                }
                assertEquals(rejectedOrigin, player.coords, tier.name)
                assertEquals(limit, used(deps), tier.name)
                assertMessageSent("The boots have no teleports remaining today.")
            }

            prepare()
            repeat(7) {
                player.withProtectedAccess {
                    deps.teleport.teleport(this, FremennikSeaBootsTier.Elite, currentUtcDay = 200)
                }
                advance(FremennikSeaBootsTeleport.CAST_TICKS)
            }
            assertEquals(0, used(deps))
            assertEquals(200, lastDay(deps))
        }

    @Test
    fun GameTestState.`daily rollover resets quota and changing boots cannot reset shared uses`() =
        runBootsTest { deps ->
            prepare()
            player.withProtectedAccess {
                deps.teleport.teleport(this, FremennikSeaBootsTier.Easy, currentUtcDay = 300)
            }
            advance(FremennikSeaBootsTeleport.CAST_TICKS)

            player.teleport(CoordGrid(3200, 3200))
            client.clear()
            player.withProtectedAccess {
                deps.teleport.teleport(this, FremennikSeaBootsTier.Easy, currentUtcDay = 300)
            }
            assertMessageSent("The boots have no teleports remaining today.")

            client.clear()
            player.withProtectedAccess {
                deps.teleport.teleport(this, FremennikSeaBootsTier.Medium, currentUtcDay = 300)
            }
            advance(FremennikSeaBootsTeleport.CAST_TICKS)
            assertEquals(2, used(deps))

            player.withProtectedAccess {
                deps.teleport.teleport(this, FremennikSeaBootsTier.Easy, currentUtcDay = 301)
            }
            advance(FremennikSeaBootsTeleport.CAST_TICKS)
            assertEquals(1, used(deps))
            assertEquals(301, lastDay(deps))
        }

    @Test
    fun GameTestState.`membership teleblock and level twenty Wilderness gates are atomic`() =
        runBootsTest { deps ->
            prepare()
            setQuota(day = 400, used = 1)
            player.members = false
            player.withProtectedAccess {
                deps.teleport.teleport(this, FremennikSeaBootsTier.Hard, currentUtcDay = 401)
            }
            assertMessageSent("You need to be on a members' world to use these boots.")
            assertQuota(deps, day = 400, used = 1)

            prepare()
            setQuota(day = 400, used = 1)
            player.setVarBit(varbits.teleblock_cycles, 101)
            player.withProtectedAccess {
                deps.teleport.teleport(this, FremennikSeaBootsTier.Hard, currentUtcDay = 401)
            }
            assertMessageSent("A magical force stops you from teleporting.")
            assertQuota(deps, day = 400, used = 1)

            prepare()
            setQuota(day = 400, used = 1)
            player.teleport(CoordGrid(3200, 3680))
            player.setVarBit(varbits.inside_wilderness, 1)
            player.withProtectedAccess {
                deps.teleport.teleport(this, FremennikSeaBootsTier.Hard, currentUtcDay = 401)
            }
            assertMessageSent("A mysterious force prevents you from teleporting!")
            assertQuota(deps, day = 400, used = 1)

            prepare()
            player.teleport(CoordGrid(3200, 3679))
            player.setVarBit(varbits.inside_wilderness, 1)
            player.withProtectedAccess {
                deps.teleport.teleport(this, FremennikSeaBootsTier.Hard, currentUtcDay = 401)
            }
            advance(FremennikSeaBootsTeleport.CAST_TICKS)
            assertEquals(FremennikSeaBootsTeleport.RELLEKKA_MARKET, player.coords)
        }

    private fun GameTestScope.prepare() {
        player.members = true
        player.teleport(CoordGrid(3200, 3200))
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.teleblock_cycles, 0)
        player.setVarp(FremennikSeaBootsVarps.teleports_used, 0)
        player.setVarp(FremennikSeaBootsVarps.last_teleport_day, 0)
        allocZoneCollision(FremennikSeaBootsTeleport.RELLEKKA_MARKET)
        client.clear()
    }

    private fun GameTestScope.used(deps: Dependencies): Int =
        player.vars[FremennikSeaBootsVarps.teleports_used]

    private fun GameTestScope.lastDay(deps: Dependencies): Int =
        player.vars[FremennikSeaBootsVarps.last_teleport_day]

    private fun GameTestScope.setQuota(day: Int, used: Int) {
        player.setVarp(FremennikSeaBootsVarps.last_teleport_day, day)
        player.setVarp(FremennikSeaBootsVarps.teleports_used, used)
    }

    private fun GameTestScope.assertQuota(deps: Dependencies, day: Int, used: Int) {
        assertEquals(day, lastDay(deps))
        assertEquals(used, player.vars[FremennikSeaBootsVarps.teleports_used])
    }

    private fun GameTestState.runBootsTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            null,
            FremennikSeaBootsScript::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val teleport: FremennikSeaBootsTeleport,
        val events: EventBus,
        val objs: ObjTypeList,
        val varps: VarpTypeList,
    )
}
