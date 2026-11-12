package org.rsmod.content.travel.canoe.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.travel.canoe.scripts.Canoe
import org.rsmod.content.travel.canoe.scripts.CanoeDestination
import org.rsmod.content.travel.canoe.scripts.Station
import org.rsmod.content.travel.canoe.scripts.findAxe
import org.rsmod.content.travel.canoe.scripts.resolveValidDestinations
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

class CanoeConfigTest {
    @Test
    fun GameTestState.`cache 233 canoe tiers retain exact level xp and sinking stages`() =
        runBasicGameTest {
            val expected =
                listOf(
                    canoe_locs.ready_log to Triple(12, 30, canoe_locs.sinking_log.id),
                    canoe_locs.ready_dugout to Triple(27, 60, canoe_locs.sinking_dugout.id),
                    canoe_locs.ready_stable_dugout to
                        Triple(42, 90, canoe_locs.sinking_stable_dugout.id),
                    canoe_locs.ready_waka to Triple(57, 150, canoe_locs.sinking_waka.id),
                )

            for ((type, contract) in expected) {
                val loc = cacheTypes.locs[type]
                assertEquals(contract.first, loc.param(params.levelrequire))
                assertEquals(contract.second, loc.param(params.skill_xp))
                assertEquals(contract.third, loc.param(params.next_loc_stage).id)
            }
        }

    @Test
    fun `canoe tiers expose the exact cache 233 destination graph`() {
        val expected =
            mapOf(
                Station.Lumbridge to
                    mapOf(
                        Canoe.Log to listOf(CanoeDestination.ChampionsGuild),
                        Canoe.Dugout to
                            listOf(
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.BarbarianVillage,
                            ),
                        Canoe.StableDugout to
                            listOf(
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.BarbarianVillage,
                                CanoeDestination.Edgeville,
                            ),
                        Canoe.Waka to
                            listOf(
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.BarbarianVillage,
                                CanoeDestination.Edgeville,
                                CanoeDestination.FeroxEnclave,
                                CanoeDestination.WildernessPond,
                            ),
                    ),
                Station.ChampionsGuild to
                    mapOf(
                        Canoe.Log to
                            listOf(CanoeDestination.Lumbridge, CanoeDestination.BarbarianVillage),
                        Canoe.Dugout to
                            listOf(
                                CanoeDestination.Lumbridge,
                                CanoeDestination.BarbarianVillage,
                                CanoeDestination.Edgeville,
                            ),
                        Canoe.StableDugout to
                            listOf(
                                CanoeDestination.Lumbridge,
                                CanoeDestination.BarbarianVillage,
                                CanoeDestination.Edgeville,
                            ),
                        Canoe.Waka to
                            listOf(
                                CanoeDestination.Lumbridge,
                                CanoeDestination.BarbarianVillage,
                                CanoeDestination.Edgeville,
                                CanoeDestination.FeroxEnclave,
                                CanoeDestination.WildernessPond,
                            ),
                    ),
                Station.BarbarianVillage to
                    mapOf(
                        Canoe.Log to
                            listOf(CanoeDestination.ChampionsGuild, CanoeDestination.Edgeville),
                        Canoe.Dugout to
                            listOf(
                                CanoeDestination.Lumbridge,
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.Edgeville,
                            ),
                        Canoe.StableDugout to
                            listOf(
                                CanoeDestination.Lumbridge,
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.Edgeville,
                            ),
                        Canoe.Waka to
                            listOf(
                                CanoeDestination.Lumbridge,
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.Edgeville,
                                CanoeDestination.FeroxEnclave,
                                CanoeDestination.WildernessPond,
                            ),
                    ),
                Station.Edgeville to
                    mapOf(
                        Canoe.Log to listOf(CanoeDestination.BarbarianVillage),
                        Canoe.Dugout to
                            listOf(
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.BarbarianVillage,
                            ),
                        Canoe.StableDugout to
                            listOf(
                                CanoeDestination.Lumbridge,
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.BarbarianVillage,
                            ),
                        Canoe.Waka to
                            listOf(
                                CanoeDestination.Lumbridge,
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.BarbarianVillage,
                                CanoeDestination.FeroxEnclave,
                                CanoeDestination.WildernessPond,
                            ),
                    ),
                Station.FeroxEnclave to
                    mapOf(
                        Canoe.Log to listOf(CanoeDestination.Edgeville),
                        Canoe.Dugout to
                            listOf(CanoeDestination.BarbarianVillage, CanoeDestination.Edgeville),
                        Canoe.StableDugout to
                            listOf(
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.BarbarianVillage,
                                CanoeDestination.Edgeville,
                            ),
                        Canoe.Waka to
                            listOf(
                                CanoeDestination.Lumbridge,
                                CanoeDestination.ChampionsGuild,
                                CanoeDestination.BarbarianVillage,
                                CanoeDestination.Edgeville,
                                CanoeDestination.WildernessPond,
                            ),
                    ),
            )

        for ((station, tiers) in expected) {
            for ((canoe, destinations) in tiers) {
                assertEquals(destinations, resolveValidDestinations(station, canoe))
            }
        }
    }

    @Test
    fun GameTestState.`canoe axe selection respects free worlds quest access and true tier`() =
        runGameTest {
            player.setCurrentLevel(stats.woodcutting, 99)
            player.inv[0] = InvObj(objs.rune_axe)
            player.inv[1] = InvObj(objs.dragon_axe)
            player.inv[2] = InvObj(objs.crystal_axe)

            player.members = false
            assertEquals(objs.rune_axe.id, findAxe(player, objTypes)?.id)

            player.members = true
            player.setVarBit(varbits.song_of_the_elves, 193)
            assertEquals(objs.dragon_axe.id, findAxe(player, objTypes)?.id)

            player.setVarBit(varbits.song_of_the_elves, 194)
            assertEquals(objs.crystal_axe.id, findAxe(player, objTypes)?.id)

            player.inv[0] = null
            player.inv[1] = null
            player.inv[2] = null
            player.righthand = InvObj(objs.bronze_axe)
            player.inv[0] = InvObj(objs.iron_axe)
            player.setCurrentLevel(stats.woodcutting, 1)
            assertEquals(objs.iron_axe.id, findAxe(player, objTypes)?.id)
        }

    @Test
    fun GameTestState.`ensure all axe objs have canoe shaping anim`() = runBasicGameTest {
        val axes = cacheTypes.objs.values.filter { it.isContentType(content.woodcutting_axe) }
        val required = cacheTypes.enums[canoe_enums.shaping_axe_anims].keys.map(ObjType::id)
        val missing = axes.filter { it.id !in required }.map { it as ObjType }
        assertEquals(emptyList<ObjType>(), missing) {
            "Found missing axe objs in `shaping_axe_anims` enum:"
        }
    }

    @Test
    fun GameTestState.`ensure all axe objs have station axe rates`() = runBasicGameTest {
        val axes = cacheTypes.objs.values.filter { it.isContentType(content.woodcutting_axe) }
        val required = cacheTypes.enums[canoe_enums.station_axe_rates].keys.map(ObjType::id)
        val missing = axes.filter { it.id !in required }.map { it as ObjType }
        assertEquals(emptyList<ObjType>(), missing) {
            "Found missing axe objs in `station_axe_rates` enum:"
        }
    }

    @Test
    fun GameTestState.`ensure all axe objs have shaping axe rates`() = runBasicGameTest {
        val axes = cacheTypes.objs.values.filter { it.isContentType(content.woodcutting_axe) }
        val required = cacheTypes.enums[canoe_enums.shaping_axe_rates].keys.map(ObjType::id)
        val missing = axes.filter { it.id !in required }.map { it as ObjType }
        assertEquals(emptyList<ObjType>(), missing) {
            "Found missing axe objs in `shaping_axe_rates` enum:"
        }
    }
}
