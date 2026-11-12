package org.rsmod.content.other.commands

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.mod.ModLevelTypeList
import org.rsmod.game.type.spot.SpotanimTypeList
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.map.CoordGrid

class AdminCommandsTest {
    @Test
    fun GameTestState.`convenience cheats set skills give items and change xp rate`() {
        val module =
            object : AbstractModule() {
                override fun configure() {
                    bind(SpotanimTypeList::class.java).toInstance(cacheTypes.spotanims)
                }
            }
        runInjectedGameTest(Dependencies::class, module, AdminCommands::class) { deps ->
            player.modLevel = deps.modLevels[modlevels.admin]

            assertFalse(player.isAccessProtected) {
                "Unexpected protected state: busy=${player.isBusy}, modals=${player.ui.modals}"
            }
            assertTrue(deps.commands.execute(player, "give", listOf("twisted", "bow")))
            assertMessageSent("Spawned inv obj `twisted_bow` x 1")
            assertTrue(player.inv.any { it?.id == objs.twisted_bow.id })

            val bank = player.invMap.getOrPut(deps.invTypes[invs.bank])
            fun hasObj(id: Int) = player.inv.any { it?.id == id } || bank.any { it?.id == id }

            assertTrue(deps.commands.execute(player, "giverunes", emptyList()))
            assertTrue(hasObj(objs.air_rune.id))

            assertTrue(deps.commands.execute(player, "giveammo", emptyList()))
            assertTrue(hasObj(objs.rune_arrow.id))

            assertTrue(deps.commands.execute(player, "bismelee", emptyList()))
            assertTrue(hasObj(objs.torva_full_helm.id))

            assertTrue(deps.commands.execute(player, "bisrange", emptyList()))
            assertTrue(hasObj(objs.twisted_bow.id))

            assertTrue(deps.commands.execute(player, "bismage", emptyList()))
            assertTrue(hasObj(objs.ancestral_hat.id))

            assertTrue(deps.commands.execute(player, "giveskilling", emptyList()))
            assertTrue(hasObj(objs.crystal_pickaxe.id))
            val bankMainTab = deps.varBits.values.first { it.internalName == "bank_tab_main" }
            assertEquals(bank.occupiedSpace(), player.vars[bankMainTab])

            val destination = CoordGrid(3208, 3220, 2)
            val homeDestination = player.coords
            assertTrue(deps.commands.execute(player, "tele", listOf("3208", "3220", "2")))
            advance()
            assertEquals(destination, player.coords)
            assertEquals(MoveSpeed.Walk, player.moveSpeed)

            assertTrue(deps.commands.execute(player, "home", emptyList()))
            advance()
            assertEquals(homeDestination, player.coords)

            assertTrue(deps.commands.execute(player, "setlevel", listOf("attack", "42")))
            assertEquals(42, player.statBase(stats.attack))

            assertTrue(deps.commands.execute(player, "setall", listOf("30")))
            assertEquals(30, player.statBase(stats.attack))

            assertTrue(deps.commands.execute(player, "xprate", listOf("2.5")))
            assertEquals(2.5, player.xpRate)
        }
    }

    private data class Dependencies
    @Inject
    constructor(
        val commands: CheatCommandMap,
        val modLevels: ModLevelTypeList,
        val invTypes: InvTypeList,
        val varBits: VarBitTypeList,
    )
}
