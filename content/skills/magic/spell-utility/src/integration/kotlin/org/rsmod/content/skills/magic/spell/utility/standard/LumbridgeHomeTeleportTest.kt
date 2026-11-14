package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.api.utils.time.epochMinute
import org.rsmod.events.EventBus
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTransmitLevel
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HomeTeleportTest {
    @Test
    fun GameTestState.`cache contract defines the default Home Teleport channel and timer`() =
        runHomeTeleportTest { deps ->
            assertEquals(6, components.magic_spellbook_home_teleport.id and 0xFFFF)
            assertEquals(108, components.magic_spellbook_ancient_home_teleport.id and 0xFFFF)
            assertEquals(109, components.magic_spellbook_lunar_home_teleport.id and 0xFFFF)
            assertEquals(153, components.magic_spellbook_arceuus_home_teleport.id and 0xFFFF)
            assertEquals(4847, seqs.aide_drawing_chalk_circle.id)
            assertEquals(4850, seqs.aide_sitting_down_crosslegged.id)
            assertEquals(4853, seqs.aide_player_getting_book.id)
            assertEquals(4855, seqs.aide_reciting_incantation.id)
            assertEquals(4857, seqs.aide_player_teleporting.id)
            assertEquals(800, spotanims.aide_chalk_circle.id)
            assertEquals(802, spotanims.aide_player_book_get.id)
            assertEquals(803, spotanims.aide_player_book_portal.id)
            assertEquals(804, spotanims.aide_player_teleport.id)
            assertEquals(193, synths.aide_teleport_chalk.id)
            assertEquals(194, synths.aide_teleport_book.id)
            assertEquals(195, synths.aide_teleport_portal.id)
            assertEquals(196, synths.aide_teleport_sitdown.id)
            assertEquals(3078, varps.date_minutes.id)
            assertEquals(892, varps.aide_tele_timer.id)
            assertEquals(VarpLifetime.Perm, deps.varpTypes[varps.aide_tele_timer].scope)
            assertEquals(
                VarpTransmitLevel.OnSetAlways,
                deps.varpTypes[varps.aide_tele_timer].transmit,
            )
            assertEquals(
                VarpTransmitLevel.OnSetDifferent,
                deps.varpTypes[varps.date_minutes].transmit,
            )
        }

    @Test
    fun GameTestState.`default channel starts immediately and arrives on tick twenty two`() =
        runHomeTeleportTest { deps ->
            prepareBase()
            val origin = player.coords
            val castMinute = epochMinute()

            player.withProtectedAccess { deps.teleport.cast(this) }
            assertEquals(seqs.aide_drawing_chalk_circle.id, player.pendingSequence.id)

            // Pending visual updates are cleared at the end of each game cycle, so the cache
            // contract above verifies every phase identity while this test verifies channel time.
            advance(21)
            assertEquals(origin, player.coords)
            assertEquals(0, player.vars[varps.aide_tele_timer])
            advance()

            assertEquals(LUMBRIDGE, player.coords)
            assertEquals(MoveSpeed.Walk, player.moveSpeed)
            assertEquals(castMinute, player.vars[varps.aide_tele_timer])
        }

    @Test
    fun GameTestState.`spellbook button dispatches the free teleport on a free world`() =
        runHomeTeleportTest { deps ->
            prepareBase()
            player.members = false

            deps.events.publish(
                IfOverlayButton(
                    player = player,
                    component = components.magic_spellbook_home_teleport,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )
            advance(22)

            assertEquals(LUMBRIDGE, player.coords)
        }

    @Test
    fun GameTestState.`Ancient home remains free-to-play and shares the cooldown`() =
        runHomeTeleportTest { deps ->
            prepareBase()
            player.members = false
            clickHome(components.magic_spellbook_ancient_home_teleport, deps)
            advance(22)
            assertEquals(EDGEVILLE, player.coords)
            val sharedStamp = player.vars[varps.aide_tele_timer]

            clickHome(components.magic_spellbook_home_teleport, deps)
            player.withProtectedAccess {
                assertMessageSent("You need to wait another 30 minutes to cast this spell.")
            }
            advance(22)
            assertEquals(EDGEVILLE, player.coords)
            assertEquals(sharedStamp, player.vars[varps.aide_tele_timer])
        }

    @Test
    fun GameTestState.`Lunar and Arceuus homes require a members world`() =
        runHomeTeleportTest { deps ->
            for (component in
                listOf(
                    components.magic_spellbook_lunar_home_teleport,
                    components.magic_spellbook_arceuus_home_teleport,
                )) {
                prepareBase()
                player.members = false
                clickHome(component, deps)
                player.withProtectedAccess {
                    assertMessageSent("You need to be on a members' world to cast this spell.")
                }
                advance(22)
                assertEquals(ORIGIN, player.coords)
                assertEquals(0, player.vars[varps.aide_tele_timer])
            }
        }

    @Test
    fun GameTestState.`members spellbook homes use their exact destinations`() =
        runHomeTeleportTest { deps ->
            for ((component, destination) in
                listOf(
                    components.magic_spellbook_lunar_home_teleport to LUNAR_ISLE,
                    components.magic_spellbook_arceuus_home_teleport to ARCEUUS,
                )) {
                prepareBase()
                player.members = true
                clickHome(component, deps)
                advance(22)
                assertEquals(destination, player.coords)
            }
        }

    @Test
    fun GameTestState.`cooldown reports rounded remaining minutes and unlocks at thirty`() =
        runHomeTeleportTest { deps ->
            prepareBase()
            val now = epochMinute()
            player.setVarp(varps.aide_tele_timer, now)
            player.withProtectedAccess {
                deps.teleport.cast(this)
                assertMessageSent("You need to wait another 30 minutes to cast this spell.")
            }
            assertEquals(ORIGIN, player.coords)

            player.setVarp(varps.aide_tele_timer, now - 29)
            player.withProtectedAccess {
                deps.teleport.cast(this)
                assertMessageSent("You need to wait another 1 minute to cast this spell.")
            }
            assertEquals(ORIGIN, player.coords)

            player.setVarp(varps.aide_tele_timer, now - 30)
            player.withProtectedAccess { deps.teleport.cast(this) }
            advance(22)
            assertEquals(LUMBRIDGE, player.coords)
        }

    @Test
    fun GameTestState.`movement and replacement actions interrupt without consuming cooldown`() =
        runHomeTeleportTest { deps ->
            prepareBase()
            player.withProtectedAccess { deps.teleport.cast(this) }
            advance(5)
            player.teleport(ORIGIN.translate(1, 0))
            advance()
            advance(30)
            assertEquals(0, player.vars[varps.aide_tele_timer])
            assertEquals(ORIGIN.translate(1, 0), player.coords)

            prepareBase()
            player.withProtectedAccess { deps.teleport.cast(this) }
            advance(5)
            player.clearPendingAction()
            advance(30)
            assertEquals(0, player.vars[varps.aide_tele_timer])
            assertEquals(ORIGIN, player.coords)
        }

    @Test
    fun GameTestState.`combat before or during the channel blocks without consuming cooldown`() =
        runHomeTeleportTest { deps ->
            prepareBase()
            player.preventLogoutUntil = player.currentMapClock + 16
            player.withProtectedAccess {
                deps.teleport.cast(this)
                assertMessageSent("You can't use that teleport at the moment.")
            }
            advance(22)
            assertEquals(ORIGIN, player.coords)
            assertEquals(0, player.vars[varps.aide_tele_timer])

            prepareBase()
            player.withProtectedAccess { deps.teleport.cast(this) }
            advance(5)
            player.preventLogoutUntil = player.currentMapClock + 16
            advance()
            advance(30)
            assertEquals(ORIGIN, player.coords)
            assertEquals(0, player.vars[varps.aide_tele_timer])
        }

    @Test
    fun GameTestState.`teleblock and deep Wilderness restrictions are atomic`() =
        runHomeTeleportTest { deps ->
            prepareBase()
            player.setVarBit(varbits.teleblock_cycles, 101)
            player.withProtectedAccess {
                deps.teleport.cast(this)
                assertMessageSent("A magical force stops you from teleporting.")
            }
            advance(22)
            assertEquals(ORIGIN, player.coords)
            assertEquals(0, player.vars[varps.aide_tele_timer])

            prepareBase()
            player.teleport(CoordGrid(0, 50, 57, 0, 32))
            val wildernessOrigin = player.coords
            player.setVarBit(varbits.inside_wilderness, 1)
            player.withProtectedAccess {
                deps.teleport.cast(this)
                assertMessageSent("A mysterious force prevents you from teleporting!")
            }
            advance(22)
            assertEquals(wildernessOrigin, player.coords)
            assertEquals(0, player.vars[varps.aide_tele_timer])
        }

    private fun GameTestScope.prepareBase() {
        player.teleport(ORIGIN)
        player.setVarp(varps.aide_tele_timer, 0)
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.teleblock_cycles, 0)
        player.preventLogoutUntil = Int.MIN_VALUE
        player.clearQueue(home_teleport_queues.channel)
    }

    private fun GameTestState.runHomeTeleportTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, null, HomeTeleportScript::class, testBody = body)

    data class Dependencies
    @Inject
    constructor(val teleport: HomeTeleport, val events: EventBus, val varpTypes: VarpTypeList)

    private companion object {
        fun GameTestScope.clickHome(component: ComponentType, deps: Dependencies) {
            deps.events.publish(
                IfOverlayButton(
                    player = player,
                    component = component,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )
        }

        val ORIGIN = CoordGrid(0, 50, 50, 0, 0)
        val LUMBRIDGE = CoordGrid(0, 50, 50, 21, 18)
        val EDGEVILLE = CoordGrid(0, 48, 54, 15, 48)
        val LUNAR_ISLE = CoordGrid(0, 33, 61, 1, 11)
        val ARCEUUS = CoordGrid(0, 26, 60, 36, 42)
    }
}
