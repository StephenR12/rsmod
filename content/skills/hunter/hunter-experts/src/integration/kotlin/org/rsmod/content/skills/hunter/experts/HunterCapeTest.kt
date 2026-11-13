package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HunterCapeTest {
    @Test
    fun GameTestState.`cache 233 cape operations Apatura and persistent state are pinned`() =
        runCapeTest { deps ->
            for (cape in listOf(objs.hunter_skillcape, objs.hunter_skillcape_t)) {
                val type = deps.objs[cape]
                assertEquals(listOf(null, "Wear", "Teleport", null, null), type.iop.toList())
                assertEquals(
                    listOf(
                        "Boost",
                        "Hunter Guild",
                        "Carnivorous Chinchompas",
                        "Black Chinchompas",
                        "",
                        "",
                        "",
                        "",
                    ),
                    listOf(
                        type.param(params.wear_op1),
                        type.param(params.wear_op2),
                        type.param(params.wear_op3),
                        type.param(params.wear_op4),
                        type.param(params.wear_op5),
                        type.param(params.wear_op6),
                        type.param(params.wear_op7),
                        type.param(params.wear_op8),
                    ),
                )
                assertTrue(deps.events.contains(HeldObjEvents.Op3::class.java, cape.id.toLong()))
                assertFalse(deps.events.contains(WornObjEvents.Op1::class.java, cape.id.toLong()))
                assertTrue(deps.events.contains(WornObjEvents.Op5::class.java, cape.id.toLong()))
                assertTrue(deps.events.contains(WornObjEvents.Op2::class.java, cape.id.toLong()))
                assertTrue(deps.events.contains(WornObjEvents.Op3::class.java, cape.id.toLong()))
                assertTrue(deps.events.contains(WornObjEvents.Op4::class.java, cape.id.toLong()))
            }

            val apatura = deps.npcs[npcs.hg_apatura]
            assertEquals(13120, apatura.id)
            assertEquals("Guildmaster Apatura", apatura.name)
            assertEquals("Talk-to", apatura.op[0])
            assertTrue(deps.events.contains(NpcEvents.Op1::class.java, apatura.id.toLong()))
            assertEquals(1312, varps.skillcape_perks.id)
            assertEquals(VarpLifetime.Perm, deps.varps[varps.skillcape_perks].scope)

            assertEquals(
                listOf(
                    "Hunter Guild" to CoordGrid(1558, 3046),
                    "Carnivorous Chinchompas" to CoordGrid(2556, 2917),
                    "Black Chinchompas" to CoordGrid(3144, 3772),
                ),
                HunterCapeDestination.entries.map { it.label to it.coords },
            )
        }

    @Test
    fun GameTestState.`worn Boost raises Hunter once to base plus one`() = runCapeTest { deps ->
        player.stats[stats.hunter] = 99

        player.worn[1] = InvObj(objs.hunter_skillcape)
        player.withProtectedAccess { deps.worn.interact(this, player.worn, 1, IfButtonOp.Op2) }
        assertEquals(100, player.stat(stats.hunter))

        player.withProtectedAccess { deps.worn.interact(this, player.worn, 1, IfButtonOp.Op2) }
        assertEquals(100, player.stat(stats.hunter))
        player.withProtectedAccess { deps.worn.interact(this, player.worn, 1, IfButtonOp.Op1) }
        assertEquals(null, player.worn[1])
        assertEquals(1, player.count(objs.hunter_skillcape))
    }

    @Test
    fun GameTestState.`menu exposes two areas then permanent unlocked guild destination`() =
        runCapeTest { deps ->
            prepare(deps)
            player.withProtectedAccess { deps.teleport.select(this, currentUtcDay = 100) }
            assertListMenu(
                HunterCapeTeleport.MENU_TITLE,
                "Carnivorous Chinchompas",
                "Black Chinchompas",
                "Cancel",
            )
            cancelListMenu(2)

            player.setVarp(varps.skillcape_perks, player.vars[varps.skillcape_perks] or (1 shl 3))
            assertEquals(
                listOf(
                    HunterCapeDestination.HunterGuild,
                    HunterCapeDestination.FeldipHunterArea,
                    HunterCapeDestination.WildernessHunterArea,
                ),
                deps.teleport.availableDestinations(player),
            )
        }

    @Test
    fun GameTestState
        .`five uses are shared by both chinchompa destinations and reset at UTC day`() =
        runCapeTest { deps ->
            prepare(deps)
            repeat(5) { index ->
                val destination =
                    if (index % 2 == 0) {
                        HunterCapeDestination.FeldipHunterArea
                    } else {
                        HunterCapeDestination.WildernessHunterArea
                    }
                player.withProtectedAccess {
                    deps.teleport.teleport(this, destination, currentUtcDay = 200)
                }
                advance(HunterCapeTeleport.CAST_TICKS)
                assertEquals(index + 1, deps.state.usedToday(player, currentUtcDay = 200))
            }

            val rejectedOrigin = CoordGrid(3200, 3200)
            player.teleport(rejectedOrigin)
            client.clear()
            player.withProtectedAccess {
                deps.teleport.teleport(
                    this,
                    HunterCapeDestination.FeldipHunterArea,
                    currentUtcDay = 200,
                )
            }
            assertEquals(rejectedOrigin, player.coords)
            assertMessageSent(
                "You have used all of your available teleports today. Try again tomorrow."
            )
            assertEquals(5, deps.state.usedToday(player, currentUtcDay = 200))

            player.withProtectedAccess {
                deps.teleport.teleport(
                    this,
                    HunterCapeDestination.FeldipHunterArea,
                    currentUtcDay = 201,
                )
            }
            advance(HunterCapeTeleport.CAST_TICKS)
            assertEquals(HunterCapeDestination.FeldipHunterArea.coords, player.coords)
            assertEquals(1, deps.state.usedToday(player, currentUtcDay = 201))
        }

    @Test
    fun GameTestState.`accepted teleport uses pinned coordinates and standard four tick effect`() =
        runCapeTest { deps ->
            prepare(deps)
            val origin = player.coords
            player.withProtectedAccess {
                deps.teleport.teleport(
                    this,
                    HunterCapeDestination.WildernessHunterArea,
                    currentUtcDay = 300,
                )
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
            assertEquals(HunterCapeDestination.WildernessHunterArea.coords, player.coords)
        }

    @Test
    fun GameTestState.`guild teleport is locked until enhanced and remains unlimited`() =
        runCapeTest { deps ->
            prepare(deps)
            player.withProtectedAccess {
                deps.teleport.teleport(this, HunterCapeDestination.HunterGuild, currentUtcDay = 400)
            }
            assertFalse(deps.state.hasGuildTeleport(player))
            assertEquals(CoordGrid(3200, 3200), player.coords)

            player.withProtectedAccess {
                deps.state.setForTest(
                    this,
                    currentUtcDay = 400,
                    teleportsUsed = 5,
                    guildTeleport = true,
                )
            }
            repeat(3) {
                player.teleport(CoordGrid(3200, 3200))
                player.withProtectedAccess {
                    deps.teleport.teleport(
                        this,
                        HunterCapeDestination.HunterGuild,
                        currentUtcDay = 400,
                    )
                }
                advance(HunterCapeTeleport.CAST_TICKS)
                assertEquals(HunterCapeDestination.HunterGuild.coords, player.coords)
                assertEquals(5, deps.state.usedToday(player, currentUtcDay = 400))
            }
        }

    @Test
    fun GameTestState.`membership teleblock and deep Wilderness failures preserve quota`() =
        runCapeTest { deps ->
            prepare(deps)
            player.withProtectedAccess {
                deps.state.setForTest(this, currentUtcDay = 500, teleportsUsed = 2)
            }
            player.members = false
            player.withProtectedAccess {
                deps.teleport.teleport(
                    this,
                    HunterCapeDestination.FeldipHunterArea,
                    currentUtcDay = 500,
                )
            }
            assertMessageSent("You need to be on a members' world to use this cape.")
            assertEquals(2, deps.state.usedToday(player, currentUtcDay = 500))

            prepareAccessOnly()
            player.setVarBit(varbits.teleblock_cycles, 101)
            player.withProtectedAccess {
                deps.teleport.teleport(
                    this,
                    HunterCapeDestination.FeldipHunterArea,
                    currentUtcDay = 500,
                )
            }
            assertMessageSent("A magical force stops you from teleporting.")
            assertEquals(2, deps.state.usedToday(player, currentUtcDay = 500))

            prepareAccessOnly()
            player.teleport(CoordGrid(3200, 3680))
            player.setVarBit(varbits.inside_wilderness, 1)
            player.withProtectedAccess {
                deps.teleport.teleport(
                    this,
                    HunterCapeDestination.FeldipHunterArea,
                    currentUtcDay = 500,
                )
            }
            assertMessageSent("A mysterious force prevents you from teleporting!")
            assertEquals(2, deps.state.usedToday(player, currentUtcDay = 500))
        }

    @Test
    fun GameTestState
        .`state preserves unrelated skillcape bits and Apatura unlocks both variants`() =
        runCapeTest { deps ->
            for (cape in listOf(objs.hunter_skillcape, objs.hunter_skillcape_t)) {
                player.clearInv()
                player.inv[0] = InvObj(cape)
                player.setVarp(varps.skillcape_perks, 1 shl 30)
                val npc = spawnNpc(player.coords.translate(1, 0), deps.npcs[npcs.hg_apatura])

                player.opNpc1(npc)
                advance()
                assertDialogueText("Nilsal, hunter.")
                resumeNpc()
                assertDialogueText("Nilsal, Apatura.")
                resumePlayer()
                assertDialogueText("Is that a skillcape you have with you?")
                resumeNpc()
                assertDialogueText("It is.")
                resumePlayer()
                assertDialogueText(
                    "Would you like me to enhance it with the ability to teleport directly to the guild?"
                )
                resumeNpc()
                choose(1)
                assertDialogueText("Yes please.")
                resumePlayer()

                assertTrue(deps.state.hasGuildTeleport(player), cape.toString())
                assertTrue(player.vars[varps.skillcape_perks] and (1 shl 30) != 0)
                assertDialogueText("Kuani! That should be done for you now.")
                player.cancelActiveCoroutine()
            }
        }

    private fun GameTestScope.prepare(deps: Dependencies) {
        prepareAccessOnly()
        player.setVarp(varps.skillcape_perks, 0)
        for (destination in HunterCapeDestination.entries) {
            allocZoneCollision(destination.coords)
        }
        assertEquals(0, deps.state.usedToday(player, currentUtcDay = 1))
    }

    private fun GameTestScope.prepareAccessOnly() {
        player.members = true
        player.teleport(CoordGrid(3200, 3200))
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.teleblock_cycles, 0)
        client.clear()
    }

    private fun GameTestScope.assertListMenu(title: String, vararg options: String) {
        val expected = listOf(title, options.joinToString("|"), 0)
        val menus = client.filterIsInstance<RunClientScript>().filter { it.id == 217 }
        assertTrue(menus.any { it.values == expected }) {
            "List menu not found: $expected | actual=${menus.map { it.values }}"
        }
    }

    private fun GameTestScope.cancelListMenu(option: Int) {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.menu_list, subcomponent = option)
        )
    }

    private fun GameTestScope.resumeNpc() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chat_left_pbutton, subcomponent = -1)
        )
    }

    private fun GameTestScope.resumePlayer() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chat_right_pbutton, subcomponent = -1)
        )
    }

    private fun GameTestScope.choose(option: Int) {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = option)
        )
    }

    private fun GameTestScope.assertDialogueText(text: String) {
        val texts = client.filterIsInstance<IfSetText>().map { it.text.replace("<br>", " ") }
        assertTrue(texts.contains(text)) { "Dialogue text not found: $text | actual=$texts" }
    }

    private fun GameTestState.runCapeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, null, HunterExpertScript::class, testBody = body)

    data class Dependencies
    @Inject
    constructor(
        val cape: HunterCapeScript,
        val worn: WornInteractions,
        val teleport: HunterCapeTeleport,
        val state: HunterCapeState,
        val events: EventBus,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val varps: VarpTypeList,
    )
}
