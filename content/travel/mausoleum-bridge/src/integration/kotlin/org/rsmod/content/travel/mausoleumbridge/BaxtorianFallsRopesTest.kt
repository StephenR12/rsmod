package org.rsmod.content.travel.mausoleumbridge

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList

@Execution(ExecutionMode.SAME_THREAD)
class BaxtorianFallsRopesTest {
    @Test
    fun GameTestState.`cache 233 point 4 rope morph contracts are pinned`() = runRopeTest { deps ->
        val rock = deps.locTypes[BaxtorianFallsRopeLocs.rock]
        val tree = deps.locTypes[BaxtorianFallsRopeLocs.tree]
        assertEquals(57703, rock.id)
        assertEquals(4516, rock.multiVarBit)
        assertEquals(listOf(57704, 57705), rock.multiLoc.map { it.toInt() and 0xFFFF })
        assertEquals(57704, rock.multiLocDefault)
        assertEquals(8643432917486768767L, rock.computeIdentityHash())
        assertEquals(57706, tree.id)
        assertEquals(4516, tree.multiVarBit)
        assertEquals(listOf(57707, 57708), tree.multiLoc.map { it.toInt() and 0xFFFF })
        assertEquals(57707, tree.multiLocDefault)
        assertEquals(8643432917486785789L, tree.computeIdentityHash())

        assertRopeVariant(deps, BaxtorianFallsRopeLocs.rockNoOp, 57704, null, 7053790155170537924L)
        assertRopeVariant(deps, BaxtorianFallsRopeLocs.rockOp, 57705, "Take", 2063956480298860504L)
        assertRopeVariant(deps, BaxtorianFallsRopeLocs.treeNoOp, 57707, null, 7053790155170537927L)
        assertRopeVariant(deps, BaxtorianFallsRopeLocs.treeOp, 57708, "Take", 2063956480298860507L)
        assertEquals(65, varps.waterfall_quest.id)
        assertEquals(4516, varbits.kandarin_medium_reward.id)
    }

    @Test
    fun GameTestState.`cutoff sync installs first anchor and leaves hotfixed second absent`() =
        runRopeTest { deps ->
            deps.ropes.sync(player)
            assertNotNull(player.privateLocs.findExactType(BaxtorianFallsRopes.ROCK_ROPE, 57703))
            assertEquals(
                null,
                player.privateLocs.findExactType(BaxtorianFallsRopes.TREE_ROPE, 57706),
            )
        }

    @Test
    fun GameTestState
        .`rope gate requires members waterfall completion and kandarin medium reward`() =
        runRopeTest { deps ->
            val rock = prepare(deps)
            val start = player.coords
            player.members = false
            player.withProtectedAccess { deps.ropes.take(this, rock) }
            assertEquals(start, player.coords)

            player.members = true
            player.setVarp(varps.waterfall_quest, BaxtorianFallsRopes.WATERFALL_COMPLETE - 1)
            player.withProtectedAccess { deps.ropes.take(this, rock) }
            assertEquals(start, player.coords)

            player.setVarp(varps.waterfall_quest, BaxtorianFallsRopes.WATERFALL_COMPLETE)
            player.setVarBit(varbits.kandarin_medium_reward, 0)
            player.withProtectedAccess { deps.ropes.take(this, rock) }
            assertEquals(start, player.coords)

            player.setVarBit(varbits.kandarin_medium_reward, 1)
            player.withProtectedAccess { deps.ropes.take(this, rock) }
            assertEquals(BaxtorianFallsRopes.CLUE_PLATEAU, player.coords)
        }

    @Test
    fun GameTestState.`take preserves clue plateau and never adds a rope item`() =
        runRopeTest { deps ->
            val rock = prepare(deps)
            val before = player.inv.toList()
            player.withProtectedAccess { deps.ropes.take(this, rock) }
            assertEquals(BaxtorianFallsRopes.CLUE_PLATEAU, player.coords)
            assertEquals(before, player.inv.toList())
        }

    @Test
    fun GameTestState.`normal loc packet resolves diary morph and takes first rope`() =
        runRopeTest { deps ->
            val rock = prepare(deps)
            player.opLoc1(rock)
            advance()
            assertEquals(BaxtorianFallsRopes.CLUE_PLATEAU, player.coords)
        }

    private fun GameTestScope.prepare(deps: Dependencies): BoundLocInfo {
        player.members = true
        player.setVarp(varps.waterfall_quest, BaxtorianFallsRopes.WATERFALL_COMPLETE)
        player.setVarBit(varbits.kandarin_medium_reward, 1)
        player.teleport(BaxtorianFallsRopes.ROCK_ROPE.translateZ(1))
        allocZoneCollision(BaxtorianFallsRopes.ROCK_ROPE)
        allocZoneCollision(BaxtorianFallsRopes.TREE_ROPE)
        allocZoneCollision(BaxtorianFallsRopes.CLUE_PLATEAU)
        deps.ropes.sync(player)
        client.clear()
        return privateRope(BaxtorianFallsRopeLocs.rock)
    }

    private fun GameTestScope.privateRope(type: LocType): BoundLocInfo {
        val coords =
            if (type == BaxtorianFallsRopeLocs.rock) {
                BaxtorianFallsRopes.ROCK_ROPE
            } else {
                BaxtorianFallsRopes.TREE_ROPE
            }
        val loc = requireNotNull(player.privateLocs.findExactType(coords, type.id))
        return BoundLocInfo(loc, locTypes[type])
    }

    private fun assertRopeVariant(
        deps: Dependencies,
        type: LocType,
        id: Int,
        op: String?,
        hash: Long,
    ) {
        val cache = deps.locTypes[type]
        assertEquals(id, cache.id)
        assertEquals("Rope", cache.name)
        assertEquals(op, cache.op[0])
        assertEquals(listOf(1842), cache.models.map { it.toInt() and 0xFFFF })
        assertEquals(hash, cache.computeIdentityHash())
    }

    private fun GameTestState.runRopeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            null,
            MausoleumBridgeScript::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(val ropes: BaxtorianFallsRopes, val locTypes: LocTypeList)
}
