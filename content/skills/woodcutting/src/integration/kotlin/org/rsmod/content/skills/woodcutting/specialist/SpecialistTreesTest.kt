package org.rsmod.content.skills.woodcutting.specialist

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.woodcutting.CrystalAxeState
import org.rsmod.content.skills.woodcutting.forestry.ForestryGear
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SpecialistTreesTest {
    @Test
    fun GameTestState
        .`native personal grown mushroom cuts a cap with exact XP and advances only that player phase`() =
        game { _ ->
            setup()
            val tree = tree(0)
            val other = registerPlayer(tree.coords.translateZ(-1))
            other.setVarBit(SpecialistBits.cep, 0)
            rolls(1000, 0)
            player.opLoc1(tree)
            advance(5)
            assertEquals(1, player.count(SpecialistObjs.cap))
            assertEquals(1270, player.statMap.getFineXP(stats.woodcutting))
            assertEquals(1, player.vars[SpecialistBits.cep])
            assertEquals(0, other.vars[SpecialistBits.cep])
            assertEquals(1, player.vars[SpecialistBits.chopped])
        }

    @Test
    fun GameTestState
        .`all six native bases rotate in the geographical sequence without global loc changes`() =
        game { d ->
            setup()
            var index = 5
            repeat(6) {
                player.setVarBit(SpecialistBits.cep, index)
                val tree = tree(index)
                player.inv[0] = InvObj(SpecialistObjs.proboscis)
                player.inv[1] = InvObj(SpecialistObjs.sprays[0])
                player.withProtectedAccess { assertTrue(d.trees.inject(this, tree)) }
                assertEquals(Sulliusceps.next[index], player.vars[SpecialistBits.cep])
                assertEquals(SpecialistObjs.sprays[1].id, player.inv[1]?.id)
                index = Sulliusceps.next[index]
            }
            assertEquals(5, index)
            assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
            assertEquals(1, player.vars[SpecialistBits.injected])
        }

    @Test
    fun GameTestState
        .`source-backed pre-roll delivers each fossil and numulite before main mushrooms`() =
        game { _ ->
            setup()
            for ((roll, item) in
                listOf(
                    900 to SpecialistObjs.fossils[0],
                    950 to SpecialistObjs.fossils[1],
                    975 to SpecialistObjs.fossils[2],
                    995 to SpecialistObjs.fossils[3],
                    0 to SpecialistObjs.numulite,
                )) {
                player.clearInv()
                player.setVarBit(SpecialistBits.cep, 0)
                val tree = tree(0)
                rolls(roll, 12)
                player.opLoc1(tree)
                advance(5)
                assertEquals(if (roll == 0) 12 else 1, player.count(item))
                assertEquals(0, player.count(SpecialistObjs.mushroom))
            }
        }

    @Test
    fun GameTestState
        .`fossil toggles divert only their own failed pre-roll to the exact main table`() =
        game { _ ->
            setup()
            player.setVarBit(SpecialistBits.fossilsBlocked, 1)
            player.setVarBit(SpecialistBits.rareBlocked, 1)
            for ((roll, main, item) in
                listOf(
                    Triple(900, 0, SpecialistObjs.cap),
                    Triple(995, 9, SpecialistObjs.fungus),
                    Triple(1000, 10, SpecialistObjs.mushroom),
                )) {
                player.clearInv()
                player.setVarBit(SpecialistBits.cep, 0)
                val tree = tree(0)
                rolls(roll, main)
                player.opLoc1(tree)
                advance(5)
                assertEquals(1, player.count(item))
            }
        }

    @Test
    fun GameTestState
        .`wrong personal tree quest membership level and unusable axe never yield or move phase`() =
        game { _ ->
            setup()
            val tree = tree(1)
            player.opLoc1(tree)
            advance(6)
            assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
            assertEquals(0, player.vars[SpecialistBits.cep])
            val active = tree(0)
            player.setVarBit(SpecialistBits.quest, 49)
            player.opLoc1(active)
            advance(6)
            player.setVarBit(SpecialistBits.quest, 50)
            player.members = false
            player.opLoc1(active)
            advance(6)
            player.members = true
            player.stats[stats.woodcutting] = 64
            player.opLoc1(active)
            advance(6)
            player.stats[stats.woodcutting] = 65
            player.statMap.setFineXP(stats.woodcutting, 0)
            player.righthand = InvObj(objs.crystal_axe)
            player.opLoc1(active)
            advance(6)
            assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
            assertEquals(0, player.vars[SpecialistBits.cep])
        }

    @Test
    fun GameTestState.`walking or full inventory cancels before resource XP or phase progress`() =
        game { _ ->
            setup()
            val tree = tree(0)
            player.opLoc1(tree)
            advance()
            val origin = player.coords
            player.moveGameClick(origin.translateX(-1))
            advance(8)
            assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
            assertEquals(0, player.vars[SpecialistBits.cep])
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.logs)
            player.placeAt(tree.coords.translateX(-1))
            player.opLoc1(tree)
            advance(8)
            assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
        }

    @Test
    fun GameTestState
        .`charged crystal axe spends one charge and leaves inactive form on successful mushroom`() =
        game { _ ->
            setup()
            player.righthand = InvObj(objs.crystal_axe, vars = CrystalAxeState(1).pack())
            val tree = tree(0)
            rolls(1000, 10)
            player.opLoc1(tree)
            advance(5)
            assertEquals(objs.crystal_axe_inactive.id, player.righthand?.id)
            assertEquals(1, player.count(SpecialistObjs.mushroom))
        }

    @Test
    fun GameTestState
        .`injection rejects missing proboscis exhausted spray and wrong phase without spending anything`() =
        game { d ->
            setup()
            val tree = tree(0)
            player.inv[0] = InvObj(SpecialistObjs.proboscis)
            player.inv[1] = InvObj(SpecialistObjs.sprays.last())
            player.withProtectedAccess { assertFalse(d.trees.inject(this, tree)) }
            assertEquals(1, player.count(SpecialistObjs.proboscis))
            assertEquals(0, player.vars[SpecialistBits.cep])
            player.inv[0] = null
            player.inv[1] = InvObj(SpecialistObjs.sprays[0])
            player.withProtectedAccess { assertFalse(d.trees.inject(this, tree)) }
            assertEquals(SpecialistObjs.sprays[0].id, player.inv[1]?.id)
        }

    @Test
    fun GameTestState
        .`blisterwood needs native quest stage and never depletes its world object or produces nests`() =
        game { _ ->
            setup()
            val source = placeMapLoc(CoordGrid(3633, 3359), SpecialistLocs.blisterwood)
            player.placeAt(CoordGrid(3632, 3360))
            player.setVarBit(SpecialistBits.sins, 111)
            player.opLoc1(source)
            advance(5)
            assertEquals(0, player.count(objs.blisterwood_logs))
            player.setVarBit(SpecialistBits.sins, 112)
            random.next = 0
            random.then = 0
            player.opLoc1(source)
            advance(5)
            assertEquals(1, player.count(objs.blisterwood_logs))
            assertEquals(760, player.statMap.getFineXP(stats.woodcutting))
            assertNotNull(findLoc(source.coords, SpecialistLocs.blisterwood))
            advance(12)
            assertEquals(1, player.count(objs.blisterwood_logs))
            assertEquals(15 to 50, SpecialistTrees.blisterwoodRate(0))
            assertEquals(61 to 186, SpecialistTrees.blisterwoodRate(7))
        }

    @Test
    fun GameTestState.`native vine path checks rake versus axe and walking interrupts clearing`() =
        game { d ->
            setup()
            val pos = CoordGrid(3671, 3802)
            val vine = placeMapLoc(pos, TarSwampLocs.obstacles[0])
            player.placeAt(pos.translateX(-1))
            var result = false
            player.withProtectedAccess { result = d.traversal.clear(this, vine, 0) }
            assertFalse(result)
            player.inv[0] = InvObj(objs.rake)
            player.withProtectedAccess { result = d.traversal.clear(this, vine, 0) }
            advance(5)
            assertTrue(result)
            assertNotNull(d.locs.findExact(pos, TarSwampLocs.cleared[0]))
            val thick = placeMapLoc(CoordGrid(3672, 3801), TarSwampLocs.obstacles[2])
            player.placeAt(thick.coords.translateX(-1))
            player.withProtectedAccess { d.traversal.clear(this, thick, 2) }
            advance()
            player.moveGameClick(player.coords.translateX(-1))
            advance(6)
            assertNull(d.locs.findExact(thick.coords, TarSwampLocs.cleared[2]))
        }

    @Test
    fun GameTestState
        .`mud pit takes nine mushrooms atomically and rubber cap consumes cushion once`() =
        game { d ->
            setup()
            val bed = placeMapLoc(CoordGrid(3686, 3755), TarSwampLocs.bed)
            player.placeAt(CoordGrid(3685, 3755))
            repeat(8) { player.inv[it] = InvObj(SpecialistObjs.mushroom) }
            player.withProtectedAccess { assertFalse(d.traversal.fill(this, bed)) }
            assertEquals(8, player.count(SpecialistObjs.mushroom))
            player.inv[8] = InvObj(SpecialistObjs.mushroom)
            player.withProtectedAccess { assertTrue(d.traversal.fill(this, bed)) }
            assertEquals(0, player.count(SpecialistObjs.mushroom))
            assertEquals(1, player.vars[TarSwampBits.bed])
            val cap = placeMapLoc(CoordGrid(3663, 3809), TarSwampLocs.jump)
            player.placeAt(CoordGrid(3662, 3809))
            val hp = player.stats[stats.hitpoints]
            player.opLoc1(cap)
            advance(6)
            assertEquals(CoordGrid(3686, 3756), player.coords)
            assertEquals(hp, player.stats[stats.hitpoints])
            assertEquals(0, player.vars[TarSwampBits.bed])
            player.placeAt(CoordGrid(3662, 3809))
            random.next = 40
            var launched = false
            player.withProtectedAccess { launched = d.traversal.jump(this, cap) }
            advance(7)
            assertTrue(launched)
            assertEquals(CoordGrid(3686, 3756), player.coords)
            assertEquals(hp - 40, player.stats[stats.hitpoints])
        }

    @Test
    fun GameTestState
        .`Arboretum double doors permit ordinary entry and exit only after the native tree stage`() =
        game { _ ->
            setup()
            val door =
                placeMapLoc(
                    CoordGrid(3614, 3362),
                    SpecialistLocs.doors[0],
                    LocShape.WallStraight,
                    LocAngle.East,
                )
            player.placeAt(CoordGrid(3613, 3362))
            player.opLoc1(door)
            advance(2)
            assertEquals(3613, player.coords.x)
            player.setVarBit(SpecialistBits.sins, 112)
            player.opLoc1(door)
            advance(2)
            assertEquals(3615, player.coords.x)
            player.opLoc1(door)
            advance(2)
            assertEquals(3613, player.coords.x)
        }

    @Test
    fun GameTestState
        .`felling rations award boosted mushroom XP and destroy fossil yields while consuming crystal charges`() =
        game { _ ->
            setup()
            player.righthand = InvObj(objs.crystal_felling_axe, vars = CrystalAxeState(1).pack())
            player.inv[0] = InvObj(objTypes.getValue(28157), 2)
            player.runEnergy = 10000
            val tree = tree(0)
            rolls(900, 0)
            player.opLoc1(tree)
            advance(5)
            assertEquals(1397, player.statMap.getFineXP(stats.woodcutting))
            assertEquals(0, player.count(SpecialistObjs.fossils[0]))
            assertEquals(1, player.count(objTypes.getValue(28157)))
            assertEquals(28223, player.righthand?.id)
            assertEquals(1, player.vars[SpecialistBits.cep])
        }

    private fun GameTestScope.setup() {
        player.members = true
        player.stats[stats.woodcutting] = 99
        player.statMap.setFineXP(stats.woodcutting, 0)
        player.stats[stats.hitpoints] = 99
        player.righthand = InvObj(objs.dragon_axe)
        player.setVarBit(SpecialistBits.quest, 50)
        player.setVarBit(SpecialistBits.cep, 0)
        player.setVarBit(SpecialistBits.sins, 0)
        player.setVarBit(SpecialistBits.fossilsBlocked, 0)
        player.setVarBit(SpecialistBits.rareBlocked, 0)
        player.setVarBit(varbits.song_of_the_elves, 194)
        player.setVarBit(varbits.vesseled_clues_disabled, 1)
    }

    private fun GameTestScope.tree(i: Int): BoundLocInfo {
        val source = placeMapLoc(Sulliusceps.sites[i], SpecialistLocs.cep[i])
        player.placeAt(source.coords.translateX(-1))
        return source
    }

    private fun GameTestScope.rolls(pre: Int, mainOrCount: Int) {
        random.next = 0
        random.then = 0
        random.then = 1
        random.then = pre
        if (
            pre < 900 ||
                pre >= 1000 ||
                pre in 900..994 && player.vars[SpecialistBits.fossilsBlocked] != 0 ||
                pre in 995..999 && player.vars[SpecialistBits.rareBlocked] != 0
        )
            random.then = mainOrCount
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            null,
            SpecialistTrees::class,
            TarSwampTraversal::class,
            ForestryGear::class,
            testBody = body,
        )

    data class Deps
    @Inject
    constructor(
        val trees: SpecialistTrees,
        val traversal: TarSwampTraversal,
        val locs: LocRepository,
    )
}
