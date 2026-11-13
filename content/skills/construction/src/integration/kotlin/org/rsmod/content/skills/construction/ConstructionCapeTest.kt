package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class ConstructionCapeTest {
    @Test
    fun GameTestState
        .`direct POH and regional cape teleports require their real travel permissions`() =
        runGameTest(ConstructionCape::class) {
            player.members = true
            val from = CoordGrid(3200, 3200)
            player.teleport(from)
            player.inv[0] = InvObj(objs.construction_skillcape)
            fun home() {
                player.withProtectedAccess { opHeld4(0) }
                advance(6)
            }
            fun select(n: Int) {
                player.ifClose()
                advance(2)
                player.withProtectedAccess { opHeld3(0) }
                advance(1)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, n))
                advance(6)
            }
            home()
            assertEquals(from, player.coords)
            player.setVarBit(varbits.poh_house_location, 1)
            player.setVarBit(varbits.poh_tele_toggle, 1)
            player.activityBlocksTeleport = true
            home()
            assertEquals(from, player.coords)
            player.activityBlocksTeleport = false
            home()
            assertEquals(CoordGrid(2954,3224), player.coords)
            select(4)
            assertEquals(CoordGrid(2954,3224), player.coords)
            player.setVarBit(cacheTypes.varbits.getValue(4897), 1)
            select(4)
            assertEquals(CoordGrid(1743,3516), player.coords)
        }

    @Test fun GameTestState.`Prifddinas portal requires the completed quest`() = runGameTest(ConstructionCape::class){
        player.members=true
        val from=CoordGrid(3200,3200)
        player.teleport(from)
        player.inv[0]=InvObj(objs.construction_skillcape)
        fun select() {
            player.withProtectedAccess { opHeld3(0) }
            advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,9))
            advance(7)
        }
        select()
        assertEquals(from,player.coords)
        allocZoneCollision(CoordGrid(3239,6075))
        player.setVarBit(varbits.song_of_the_elves,200)
        select()
        assertEquals(CoordGrid(3239,6075),player.coords)
    }
}
