package org.rsmod.content.skills.hunter.birdsnares

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.npc.NoopNpcInfo
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class PassiveTrapAcquisitionTest {
    @Test
    fun GameTestState.nativeBirdSeeksTheLaidSnareAndFillsItWithoutAnNpcInteraction() = game { d ->
        prepare(CoordGrid(2000, 2000))
        player.inv[0] = InvObj(BirdSnareObjs.snare)
        player.withProtectedAccess { d.birds.lay(this, 0) }
        advance(5)
        val npc =
            spawnNpc(CoordGrid(2002, 2000), d.types[BirdSnareNpcs.crimsonSwift]) { noneMode() }
        npc.infoProtocol = Observed
        random.next = 0
        random.then = 1
        advance(30)
        assertTrue(locExists(CoordGrid(2000, 2000), BirdSnareLocs.fullCrimson))
    }

    @Test
    fun GameTestState.nativeChinchompaSeeksAnOpenBoxAndFillsItWithoutAnNpcInteraction() =
        game { d ->
            prepare(CoordGrid(2016, 2000))
            VarPlayerIntMapSetter.set(player, BoxTrapVars.eaglesPeak, 40)
            player.inv[0] = InvObj(BoxTrapObjs.snare)
            player.withProtectedAccess { d.boxes.lay(this, 0) }
            advance(5)
            val npc = spawnNpc(CoordGrid(2018, 2000), d.types[BoxTrapNpcs.grey]) { noneMode() }
            npc.infoProtocol = Observed
            random.next = 0
            random.then = 1
            advance(30)
            assertTrue(locExists(CoordGrid(2016, 2000), BoxTrapSpeciesList.all[0].full))
        }

    @Test
    fun GameTestState.allFiveSalamandersSeekTheirNativeSetTreeWithoutAnNpcInteraction() =
        game { d ->
            for (species in NetTrapSpeciesList.all) {
                val c = CoordGrid(2040 + species.index * 16, 2000)
                prepare(c.translate(0, 2))
                player.inv[0] = InvObj(NetTrapObjs.rope)
                player.inv[1] = InvObj(NetTrapObjs.net)
                val tree = placeMapLoc(c, species.tree)
                player.withProtectedAccess { d.nets.lay(this, tree, species) }
                advance(5)
                val npc = spawnNpc(c.translate(2, 0), d.types[species.npc]) { noneMode() }
                npc.infoProtocol = Observed
                random.next = 0
                random.then = 1
                advance(30)
                assertTrue(
                    locExists(c, species.full),
                    "species=${species.index},npc=${npc.coords},mode=${npc.mode},interaction=${npc.interaction}",
                )
                d.nets.clear(player)
            }
        }

    private fun GameTestScope.prepare(c: CoordGrid) {
        for (dx in -8..8 step 8) for (dz in -8..8 step 8) allocZoneCollision(c.translate(dx, dz))
        player.members = true
        player.stats[stats.hunter] = 99
        player.clearInv()
        player.telejump(c)
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            Module,
            BirdSnareScript::class,
            BoxTrapScript::class,
            NetTrapScript::class,
            testBody = body,
        )

    private object Module : AbstractModule() {
        override fun configure() {
            install(BirdSnareModule())
        }
    }

    private object Observed : NpcInfoProtocol by NoopNpcInfo {
        override fun isActive() = true
    }

    data class Deps
    @Inject
    constructor(
        val birds: BirdSnareTrap,
        val boxes: BoxTrap,
        val nets: NetTrap,
        val types: NpcTypeList,
    )
}
