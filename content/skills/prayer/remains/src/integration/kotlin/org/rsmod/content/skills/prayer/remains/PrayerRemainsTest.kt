package org.rsmod.content.skills.prayer.remains

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.util.UncheckedType
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@OptIn(UncheckedType::class)
class PrayerRemainsTest {
    @Test
    fun GameTestState
        .`all prayer scrolls consume once after confirmation and check their own reading level`() =
        runInjectedGameTest(Dependencies::class, TestModule, PrayerScrolls::class) { deps ->
            preparePlayer()
            val items = listOf(21047, 21034, 21079, 30626, 30627)
            val bits = listOf(5453, 5451, 5452, 16097, 16098)
            for (index in items.indices) {
                val scroll = objTypes.getValue(items[index])
                val bit = cacheTypes.varbits.getValue(bits[index])
                val level = listOf(1, 1, 1, 62, 63)[index]
                fun read() {
                    player.ifClose()
                    player.withProtectedAccess { deps.held.interact(this, inv, 0, HeldOp.Op1) }
                }
                player.inv[0] = InvObj(scroll)
                if (level > 1) {
                    player.setBaseLevel(stats.prayer, level - 1)
                    read()
                    assertEquals(0, player.vars[bit])
                    assertEquals(1, player.count(scroll))
                }
                player.setBaseLevel(stats.prayer, level)
                advance()
                read()
                declineChoice()
                assertEquals(1, player.count(scroll))
                advance()
                read()
                confirmChoice()
                assertMessageSent(
                    "You have unlocked " +
                        listOf("Preserve", "Rigour", "Augury", "Deadeye", "Mystic Vigour")[index] +
                        "."
                )
                org.junit.jupiter.api.Assertions.assertEquals(
                    1,
                    player.vars[bit],
                    "Unlock for scroll ${scroll.id}",
                )
                assertEquals(0, player.count(scroll))
                player.inv[0] = InvObj(scroll)
                read()
                assertEquals(1, player.count(scroll))
            }
        }

    @Test
    fun GameTestState.`cache 238 Goblin Temple altar pins composite loc and quest contracts`() =
        runPrayerRemainsTest { deps ->
            assertEquals(
                listOf(43089, 43097, 43098),
                prayer_altar_locs.goblinTempleAltars.map { it.id },
            )
            assertTrue(
                prayer_altar_locs.goblinTempleAltars.all { altar ->
                    locTypes.getValue(altar.id).op.toList() ==
                        listOf("Pray-at", null, null, null, null)
                }
            )
            assertEquals(13599, varbits.land_of_the_goblins.id)
            assertEquals(3355, varbits.land_of_the_goblins.baseVar.id)
            assertEquals(0..8, varbits.land_of_the_goblins.bits)
            assertEquals(36, PrayerGoblinTempleAltar.RESTORATION_UNLOCKED)
            assertEquals(56, PrayerGoblinTempleAltar.QUEST_COMPLETE)
            assertEquals(listOf(56), deps.dbRows.types.getValue(LAND_OF_THE_GOBLINS_ROW).data[19])
        }

    @Test
    fun GameTestState.`Goblin Temple altar refuses prayer before the crypt opens`() =
        runPrayerRemainsTest { deps ->
            for ((altarIndex, type) in prayer_altar_locs.goblinTempleAltars.withIndex()) {
                val altar = prepareAltar(type, xOffset = altarIndex * 3)
                for (questState in listOf(0, PrayerGoblinTempleAltar.RESTORATION_UNLOCKED - 1)) {
                    preparePlayer()
                    player.setBaseLevel(stats.prayer, 70)
                    player.setCurrentLevel(stats.prayer, 17)
                    player.setVarBit(varbits.prayer_drain_counter, 47)
                    player.setVarBit(varbits.land_of_the_goblins, questState)

                    prayAt(deps, altar)

                    assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
                    assertEquals(47, player.vars[varbits.prayer_drain_counter])
                    assertEquals(questState, player.vars[varbits.land_of_the_goblins])
                    assertEquals(seqs.human_pray.id, player.pendingSequence.id)
                    assertMessageSent(PrayerGoblinTempleAltar.NO_ANSWER)
                }
            }
        }

    @Test
    fun GameTestState.`Goblin Temple altar restores Prayer from the crypt stage onward`() =
        runPrayerRemainsTest { deps ->
            for ((altarIndex, type) in prayer_altar_locs.goblinTempleAltars.withIndex()) {
                val altar = prepareAltar(type, xOffset = altarIndex * 3)
                for (questState in
                    listOf(
                        PrayerGoblinTempleAltar.RESTORATION_UNLOCKED,
                        PrayerGoblinTempleAltar.QUEST_COMPLETE,
                    )) {
                    preparePlayer()
                    player.setBaseLevel(stats.prayer, 70)
                    player.setCurrentLevel(stats.prayer, 17)
                    player.setVarBit(varbits.prayer_drain_counter, 47)
                    player.setVarBit(varbits.land_of_the_goblins, questState)

                    prayAt(deps, altar)

                    assertEquals(70, player.statMap.getCurrentLevel(stats.prayer).toInt())
                    assertEquals(0, player.vars[varbits.prayer_drain_counter])
                    assertEquals(questState, player.vars[varbits.land_of_the_goblins])
                    assertEquals(seqs.human_pray.id, player.pendingSequence.id)
                    assertMessageSent("You recharge your Prayer points.")
                }
            }
        }

    @Test
    fun GameTestState.`cache 238 Mage Arena statues pin operations state and cape mapping`() =
        runPrayerRemainsTest {
            val statues =
                listOf(
                    prayer_altar_locs.mageArenaSaradominStatue to objs.saradomin_cape,
                    prayer_altar_locs.mageArenaGuthixStatue to objs.guthix_cape,
                    prayer_altar_locs.mageArenaZamorakStatue to objs.zamorak_cape,
                )

            assertEquals(listOf(2873, 2875, 2874), statues.map { (statue) -> statue.id })
            assertTrue(
                statues.all { (statue) ->
                    locTypes.getValue(statue.id).op.toList() ==
                        listOf("Pray-at", null, null, null, null)
                }
            )
            assertEquals(listOf(2412, 2413, 2414), statues.map { (_, cape) -> cape.id })
            assertEquals(267, varps.magearena.id)
            assertEquals(6, PrayerMageArenaStatues.MAGE_ARENA_COMPLETE)
        }

    @Test
    fun GameTestState.`cache 238 trapped Chaos altar pins operation damage and destination`() =
        runPrayerRemainsTest {
            assertEquals(412, prayer_altar_locs.trappedChaosAltar.id)
            assertEquals(
                listOf("Pray-at", null, null, null, null),
                locTypes.getValue(prayer_altar_locs.trappedChaosAltar.id).op.toList(),
            )
            assertEquals(15, PrayerTrappedChaosAltar.FALL_DAMAGE)
            assertEquals(2578, PrayerTrappedChaosAltar.SPIDER_PIT.x)
            assertEquals(9578, PrayerTrappedChaosAltar.SPIDER_PIT.z)
            assertEquals(0, PrayerTrappedChaosAltar.SPIDER_PIT.level)
        }

    @Test
    fun GameTestState.`cache 238 broken and unrestricted altars pin production classification`() =
        runPrayerRemainsTest {
            assertEquals(34872, prayer_altar_locs.brokenSunAltar.id)
            assertEquals(
                listOf("Pray-at", null, null, null, null),
                locTypes.getValue(prayer_altar_locs.brokenSunAltar.id).op.toList(),
            )
            assertEquals(645, seqs.human_pray.id)
            assertEquals(8321, seqs.human_sun_prayer.id)
            assertEquals(8409, varbits.varlamore_sun_knight.id)
            assertEquals(1, PrayerBrokenSunAltar.KNIGHT_AT_ALTAR)

            assertEquals(7814, prayer_altar_locs.unrestrictedWorldSpellbookAltar.id)
            assertEquals(
                listOf(
                    "Pray-at",
                    "Standard spellbook",
                    "Ancient spellbook",
                    "Lunar spellbook",
                    "Arceuus spellbook",
                ),
                locTypes.getValue(prayer_altar_locs.unrestrictedWorldSpellbookAltar.id).op.toList(),
            )
        }

    @Test
    fun GameTestState.`cache 238 spellbook altars pin operations quests and spellbook state`() =
        runPrayerRemainsTest { deps ->
            assertEquals(34771, prayer_altar_locs.astralAltar.id)
            assertEquals(
                listOf("Craft-rune", "Pray", null, null, null),
                locTypes.getValue(prayer_altar_locs.astralAltar.id).op.toList(),
            )
            assertEquals(6552, prayer_altar_locs.ancientPyramidAltar.id)
            assertEquals(
                listOf("Pray-at", null, null, null, null),
                locTypes.getValue(prayer_altar_locs.ancientPyramidAltar.id).op.toList(),
            )

            assertEquals(2448, varbits.lunar_quest_main.id)
            assertEquals(190, PrayerSpellbookAltar.LUNAR_DIPLOMACY_COMPLETE)
            assertEquals(listOf(190), deps.dbRows.types.getValue(LUNAR_DIPLOMACY_ROW).data[19])
            assertEquals(358, varbits.desert_treasure_main.id)
            assertEquals(440, varbits.desert_treasure_main.baseVar.id)
            assertEquals(15, PrayerSpellbookAltar.DESERT_TREASURE_COMPLETE)
            assertEquals(listOf(15), deps.dbRows.types.getValue(DESERT_TREASURE_ROW).data[19])
            assertEquals(4070, varbits.spellbook.id)
            assertEquals(listOf(0, 1, 2, 3), Spellbook.entries.map { it.varValue })
        }

    @Test
    fun GameTestState.`every cache 238 Pray location is explicitly classified`() =
        runPrayerRemainsTest {
            assertEquals(645, seqs.human_pray.id)

            val cachePrayLocs =
                locTypes.entries
                    .filter { (_, type) -> type.op.any { it == "Pray" || it == "Pray-at" } }
                    .mapTo(mutableSetOf()) { it.key }
            val categories =
                listOf(
                    prayer_altar_locs.standardRestoration,
                    prayer_altar_locs.boostedRestoration,
                    prayer_altar_locs.godWarsRestoration,
                    prayer_altar_locs.hazardousRestoration,
                    prayer_altar_locs.utilityRestoration,
                    prayer_altar_locs.spellbookAltars,
                    prayer_altar_locs.mageArenaStatues,
                    prayer_altar_locs.transportAltars,
                    prayer_altar_locs.nonRestoringAltars,
                    prayer_altar_locs.nonProductionAltars,
                    prayer_altar_locs.goblinTempleAltars,
                )
            val classified = categories.flatten().map { it.id }

            assertEquals(85, cachePrayLocs.size)
            assertEquals(listOf(64, 2, 5, 2, 1, 2, 3, 1, 1, 1, 3), categories.map { it.size })
            assertEquals(classified.size, classified.toSet().size, "altar categories overlap")
            assertEquals(cachePrayLocs, classified.toSet())
        }

    @Test
    fun GameTestState.`Astral Altar requires Lunar Diplomacy and preserves Prayer`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.astralAltar)
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 17)
            player.setVarBit(varbits.prayer_drain_counter, 47)
            player.setVarBit(varbits.spellbook, Spellbook.Ancients.varValue)
            player.setVarBit(
                varbits.lunar_quest_main,
                PrayerSpellbookAltar.LUNAR_DIPLOMACY_COMPLETE - 1,
            )

            prayAt(deps, altar, InteractionOp.Op2)

            assertEquals(Spellbook.Ancients.varValue, player.vars[varbits.spellbook])
            assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(47, player.vars[varbits.prayer_drain_counter])
        }

    @Test
    fun GameTestState.`Astral Altar activates Lunar from every other book and then Standard`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.astralAltar)
            for (current in listOf(Spellbook.Standard, Spellbook.Ancients, Spellbook.Arceuus)) {
                preparePlayer()
                player.setBaseLevel(stats.prayer, 70)
                player.setCurrentLevel(stats.prayer, 17)
                player.setVarBit(varbits.prayer_drain_counter, 47)
                player.setVarBit(
                    varbits.lunar_quest_main,
                    PrayerSpellbookAltar.LUNAR_DIPLOMACY_COMPLETE,
                )
                player.setVarBit(varbits.spellbook, current.varValue)

                prayAt(deps, altar, InteractionOp.Op2)

                assertEquals(Spellbook.Lunars.varValue, player.vars[varbits.spellbook])
                assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
                assertEquals(47, player.vars[varbits.prayer_drain_counter])
                assertEquals(seqs.human_pray.id, player.pendingSequence.id)
                assertMessageSent(PrayerSpellbookAltar.LUNAR_ACTIVATED)
            }

            preparePlayer()
            player.setVarBit(
                varbits.lunar_quest_main,
                PrayerSpellbookAltar.LUNAR_DIPLOMACY_COMPLETE,
            )
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)

            prayAt(deps, altar, InteractionOp.Op2)

            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook])
            assertMessageSent(PrayerSpellbookAltar.LUNAR_DEACTIVATED)
        }

    @Test
    fun GameTestState.`Ancient Pyramid altar requires Desert Treasure completion`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.ancientPyramidAltar)
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 17)
            player.setVarBit(varbits.prayer_drain_counter, 47)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.setVarBit(
                varbits.desert_treasure_main,
                PrayerSpellbookAltar.DESERT_TREASURE_COMPLETE - 1,
            )

            prayAt(deps, altar)

            assertEquals(Spellbook.Lunars.varValue, player.vars[varbits.spellbook])
            assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(47, player.vars[varbits.prayer_drain_counter])
        }

    @Test
    fun GameTestState.`Ancient Pyramid altar drains Prayer and toggles Ancient and Standard`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.ancientPyramidAltar)
            for (current in listOf(Spellbook.Standard, Spellbook.Lunars, Spellbook.Arceuus)) {
                preparePlayer()
                player.setBaseLevel(stats.prayer, 70)
                player.setCurrentLevel(stats.prayer, 17)
                player.setVarBit(varbits.prayer_drain_counter, 47)
                player.setVarBit(
                    varbits.desert_treasure_main,
                    PrayerSpellbookAltar.DESERT_TREASURE_COMPLETE,
                )
                player.setVarBit(varbits.spellbook, current.varValue)

                prayAt(deps, altar)

                assertEquals(Spellbook.Ancients.varValue, player.vars[varbits.spellbook])
                assertEquals(0, player.statMap.getCurrentLevel(stats.prayer).toInt())
                assertEquals(0, player.vars[varbits.prayer_drain_counter])
                assertEquals(seqs.human_pray.id, player.pendingSequence.id)
                assertMessageSent(PrayerSpellbookAltar.ANCIENT_ACTIVATED)
            }

            preparePlayer()
            player.setCurrentLevel(stats.prayer, 0)
            player.setVarBit(
                varbits.desert_treasure_main,
                PrayerSpellbookAltar.DESERT_TREASURE_COMPLETE,
            )
            player.setVarBit(varbits.spellbook, Spellbook.Ancients.varValue)

            prayAt(deps, altar)

            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook])
            assertEquals(0, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertMessageSent(PrayerSpellbookAltar.ANCIENT_DEACTIVATED)
        }

    @Test
    fun GameTestState.`spellbook altars preserve active and saved autocast state`() =
        runPrayerRemainsTest { deps ->
            val autocastState =
                mapOf(
                    varbits.autocast_set to 1,
                    varbits.autocast_spell to 1,
                    varbits.autocast_defmode to 1,
                    varbits.saved_autocast_spell_staff to 1,
                    varbits.saved_defensive_casting_staff to 1,
                    varbits.saved_autocast_spell_bladed_staff to 1,
                    varbits.saved_defensive_casting_bladed_staff to 1,
                )

            fun setAutocastState() {
                for ((varbit, value) in autocastState) {
                    player.setVarBit(varbit, value)
                }
            }

            fun assertAutocastState() {
                for ((varbit, value) in autocastState) {
                    assertEquals(value, player.vars[varbit], varbit.toString())
                }
            }

            val astral = prepareAltar(prayer_altar_locs.astralAltar)
            preparePlayer()
            player.setVarBit(
                varbits.lunar_quest_main,
                PrayerSpellbookAltar.LUNAR_DIPLOMACY_COMPLETE,
            )
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            setAutocastState()

            prayAt(deps, astral, InteractionOp.Op2)
            assertEquals(Spellbook.Lunars.varValue, player.vars[varbits.spellbook])
            assertAutocastState()
            prayAt(deps, astral, InteractionOp.Op2)
            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook])
            assertAutocastState()

            val pyramid = prepareAltar(prayer_altar_locs.ancientPyramidAltar, xOffset = 3)
            preparePlayer()
            player.setVarBit(
                varbits.desert_treasure_main,
                PrayerSpellbookAltar.DESERT_TREASURE_COMPLETE,
            )
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            setAutocastState()

            prayAt(deps, pyramid)
            assertEquals(Spellbook.Ancients.varValue, player.vars[varbits.spellbook])
            assertAutocastState()
            prayAt(deps, pyramid)
            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook])
            assertAutocastState()
        }

    @Test
    fun GameTestState.`Mage Arena statues require completion and create private god capes`() =
        runPrayerRemainsTest { deps ->
            val statues =
                listOf(
                    prayer_altar_locs.mageArenaSaradominStatue to objs.saradomin_cape,
                    prayer_altar_locs.mageArenaGuthixStatue to objs.guthix_cape,
                    prayer_altar_locs.mageArenaZamorakStatue to objs.zamorak_cape,
                )

            for ((index, pair) in statues.withIndex()) {
                val (statueType, expectedCape) = pair
                val statue = prepareAltar(statueType, xOffset = index * 4)
                preparePlayer()
                player.placeAt(statue.coords.translateX(-1))
                player.setBaseLevel(stats.prayer, 70)
                player.setCurrentLevel(stats.prayer, 17)
                player.setVarp(varps.magearena, 0)

                prayAt(deps, statue)

                assertTrue(deps.objRepo.findAll(player.coords).none())
                assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())

                player.setVarp(varps.magearena, PrayerMageArenaStatues.MAGE_ARENA_COMPLETE)
                prayAt(deps, statue)

                val cape = deps.objRepo.findAll(player.coords).single()
                assertEquals(expectedCape.id, cape.type)
                assertTrue(cape.isPrivate)
                assertEquals(player.observerUUID, cape.nullableReceiverId)
                assertEquals(player.observerUUID, cape.nullableOwnerId)
                assertEquals(seqs.human_pray.id, player.pendingSequence.id)
                assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
            }
        }

    @Test
    fun GameTestState.`Mage Arena statues allow multiple copies of the same cape`() =
        runPrayerRemainsTest { deps ->
            val statue = prepareAltar(prayer_altar_locs.mageArenaSaradominStatue)
            preparePlayer()
            player.setVarp(varps.magearena, PrayerMageArenaStatues.MAGE_ARENA_COMPLETE)

            prayAt(deps, statue)
            prayAt(deps, statue)

            assertEquals(
                listOf(objs.saradomin_cape.id, objs.saradomin_cape.id),
                deps.objRepo.findAll(player.coords).map { it.type }.toList(),
            )
        }

    @Test
    fun GameTestState.`trapped Chaos altar deals 15 damage and drops player into spider pit`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.trappedChaosAltar)
            preparePlayer()
            player.setBaseLevel(stats.hitpoints, 50)
            player.setCurrentLevel(stats.hitpoints, 50)
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 17)
            player.setVarBit(varbits.prayer_drain_counter, 47)

            prayAt(deps, altar)

            assertEquals(35, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(PrayerTrappedChaosAltar.SPIDER_PIT, player.coords)
            assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(47, player.vars[varbits.prayer_drain_counter])
            assertEquals(seqs.human_pray.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`trapped Chaos altar fall damage is lethal at 15 hitpoints`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.trappedChaosAltar)
            preparePlayer()
            player.setBaseLevel(stats.hitpoints, 50)
            player.setCurrentLevel(stats.hitpoints, PrayerTrappedChaosAltar.FALL_DAMAGE)

            prayAt(deps, altar)

            assertEquals(0, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(PrayerTrappedChaosAltar.SPIDER_PIT, player.coords)
        }

    @Test
    fun GameTestState.`broken sun altar does nothing with ordinary prayer animation`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.brokenSunAltar)
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 17)
            player.setVarBit(varbits.prayer_drain_counter, 47)
            player.setVarBit(varbits.varlamore_sun_knight, 0)

            prayAt(deps, altar)
            assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(47, player.vars[varbits.prayer_drain_counter])
            assertEquals(seqs.human_pray.id, player.pendingSequence.id)
            assertMessageSent(PrayerBrokenSunAltar.NOTHING_HAPPENS)
        }

    @Test
    fun GameTestState.`broken sun altar praises the sun after the knight arrives`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.brokenSunAltar)
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 17)
            player.setVarBit(varbits.prayer_drain_counter, 47)
            player.setVarBit(varbits.varlamore_sun_knight, PrayerBrokenSunAltar.KNIGHT_AT_ALTAR)

            prayAt(deps, altar)
            assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(47, player.vars[varbits.prayer_drain_counter])
            assertEquals(seqs.human_sun_prayer.id, player.pendingSequence.id)
            assertMessageSent(PrayerBrokenSunAltar.NOTHING_HAPPENS)
        }

    @Test
    fun GameTestState.`temporary unrestricted altar has no production Pray handler`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.unrestrictedWorldSpellbookAltar)
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 17)
            player.setVarBit(varbits.prayer_drain_counter, 47)
            player.setVarBit(varbits.spellbook, Spellbook.Ancients.varValue)

            val event =
                deps.locs.opTrigger(
                    player = player,
                    loc = altar,
                    op = InteractionOp.Op1,
                    type = locTypes.getValue(altar.id),
                )

            assertNull(event)
            assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(47, player.vars[varbits.prayer_drain_counter])
            assertEquals(Spellbook.Ancients.varValue, player.vars[varbits.spellbook])
            assertNoMessageSent()
        }

    @Test
    fun GameTestState.`ordinary altar dispatch restores base Prayer and resets drain progress`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.standardRestoration.first())
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 7)
            player.setVarBit(varbits.prayer_drain_counter, 47)

            prayAt(deps, altar)

            assertEquals(70, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(0, player.vars[varbits.prayer_drain_counter])
            assertEquals(seqs.human_pray.id, player.pendingSequence.id)
            assertMessageSent("You recharge your Prayer points.")
        }

    @Test
    fun GameTestState.`ordinary altar resets an existing Prayer overcharge to base`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.standardRestoration.first())
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 72)

            prayAt(deps, altar)

            assertEquals(70, player.statMap.getCurrentLevel(stats.prayer).toInt())
        }

    @Test
    fun GameTestState.`Entrana quest-state altars and repaired tribal statue restore Prayer`() =
        runPrayerRemainsTest { deps ->
            val cases =
                listOf(
                    prayer_altar_locs.standardRestoration.first { it.id == 10639 },
                    prayer_altar_locs.standardRestoration.first { it.id == 10640 },
                    prayer_altar_locs.standardRestoration.first { it.id == 3863 },
                )
            for ((index, type) in cases.withIndex()) {
                val altar = prepareAltar(type, xOffset = index * 3)
                preparePlayer()
                player.setBaseLevel(stats.prayer, 70)
                player.setCurrentLevel(stats.prayer, 1)

                prayAt(deps, altar)

                assertEquals(
                    70,
                    player.statMap.getCurrentLevel(stats.prayer).toInt(),
                    type.toString(),
                )
            }
        }

    @Test
    fun GameTestState.`monastery and Nature Grotto altars restore exactly two above base`() =
        runPrayerRemainsTest { deps ->
            for ((index, type) in prayer_altar_locs.boostedRestoration.withIndex()) {
                val altar = prepareAltar(type, xOffset = index * 3)
                preparePlayer()
                player.setBaseLevel(stats.prayer, 70)
                player.setCurrentLevel(stats.prayer, 1)

                prayAt(deps, altar)

                assertEquals(
                    72,
                    player.statMap.getCurrentLevel(stats.prayer).toInt(),
                    type.toString(),
                )
            }
        }

    @Test
    fun GameTestState.`Cursed Altar drains Prayer and deals bounded lethal damage`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.cursedAltar)
            val cases = listOf(0 to 5, 10 to 10, 70 to 30)
            for ((prayer, damage) in cases) {
                preparePlayer()
                player.setBaseLevel(stats.prayer, 70)
                player.setCurrentLevel(stats.prayer, prayer)
                player.setBaseLevel(stats.hitpoints, 99)
                player.setCurrentLevel(stats.hitpoints, 99)
                player.setVarBit(varbits.prayer_drain_counter, 47)

                prayAt(deps, altar)

                assertEquals(0, player.statMap.getCurrentLevel(stats.prayer).toInt())
                assertEquals(99 - damage, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
                assertEquals(0, player.vars[varbits.prayer_drain_counter])
                assertEquals(seqs.human_pray.id, player.pendingSequence.id)
            }

            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 70)
            player.setBaseLevel(stats.hitpoints, 10)
            player.setCurrentLevel(stats.hitpoints, 10)

            prayAt(deps, altar)

            assertEquals(0, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
        }

    @Test
    fun GameTestState.`Darkmeyer statue restores Prayer and charges one to three Hitpoints`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.darkmeyerStatue)
            for (damage in 1..3) {
                preparePlayer()
                player.setBaseLevel(stats.prayer, 70)
                player.setCurrentLevel(stats.prayer, 1)
                player.setBaseLevel(stats.hitpoints, 99)
                player.setCurrentLevel(stats.hitpoints, 99)
                player.setVarBit(varbits.prayer_drain_counter, 47)
                random.next = damage

                prayAt(deps, altar)

                assertEquals(70, player.statMap.getCurrentLevel(stats.prayer).toInt())
                assertEquals(99 - damage, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
                assertEquals(0, player.vars[varbits.prayer_drain_counter])
                assertEquals(seqs.human_pray.id, player.pendingSequence.id)
                assertMessageSent("You recharge your Prayer points.")
            }
        }

    @Test
    fun GameTestState.`Elidinis statuette pins cache quest poison and restoration contracts`() =
        runPrayerRemainsTest { deps ->
            assertEquals(1444, varbits.elidquest.id)
            assertEquals(616, varbits.elidquest.baseVar.id)
            assertEquals(60, PrayerElidinisStatuetteRestoration.QUEST_COMPLETE)
            assertEquals(listOf(60), deps.dbRows.types.getValue(SPIRITS_OF_THE_ELID_ROW).data[19])
            assertEquals(102, varps.poison.id)
            assertEquals(3777, varps.poison_2.id)
            assertEquals(6251, varbits.poison_type.id)
            assertEquals(1 shl 2, WorldFlag.Pvp.mask)
            assertEquals(1 shl 29, WorldFlag.Deadman.mask)
        }

    @Test
    fun GameTestState
        .`Elidinis statuette requires quest completion and fully restores the player`() =
        runPrayerRemainsTest { deps ->
            val statuette = prepareAltar(prayer_altar_locs.elidinisStatuette)
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 5)
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 10)
            player.runEnergy = 1_234
            player.setVarp(varps.sa_energy, 123)
            player.setVarp(varps.poison, 25)
            player.setVarp(varps.poison_2, 123)
            player.setVarBit(varbits.poison_type, 2)
            player.setVarBit(
                varbits.elidquest,
                PrayerElidinisStatuetteRestoration.QUEST_COMPLETE - 1,
            )

            prayAt(deps, statuette)

            assertEquals(5, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(10, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(1_234, player.runEnergy)
            assertEquals(123, player.vars[varps.sa_energy])

            player.setVarBit(varbits.elidquest, PrayerElidinisStatuetteRestoration.QUEST_COMPLETE)
            player.setVarBit(varbits.prayer_drain_counter, 47)
            prayAt(deps, statuette)

            assertEquals(70, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(106, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(10_000, player.runEnergy)
            assertEquals(1_000, player.vars[varps.sa_energy])
            assertEquals(0, player.vars[varps.poison])
            assertEquals(0, player.vars[varps.poison_2])
            assertEquals(0, player.vars[varbits.poison_type])
            assertEquals(0, player.vars[varbits.prayer_drain_counter])
            assertEquals(seqs.human_pray.id, player.pendingSequence.id)
            assertMessageSent("You recharge your Prayer points.")

            player.setCurrentLevel(stats.hitpoints, 110)
            prayAt(deps, statuette)
            assertEquals(110, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
        }

    @Test
    fun GameTestState.`Elidinis statuette applies exact PvP and Deadman Hitpoints rules`() =
        runPrayerRemainsTest { deps ->
            val statuette = prepareAltar(prayer_altar_locs.elidinisStatuette)

            deps.realm.updateConfig(deps.realm.config.copy(worldFlags = WorldFlag.Pvp.mask))
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 5)
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 10)
            player.runEnergy = 1_234
            player.setVarp(varps.sa_energy, 123)
            player.setVarp(varps.poison, 25)
            player.setVarp(varps.poison_2, 123)
            player.setVarBit(varbits.poison_type, 2)
            player.setVarBit(varbits.elidquest, PrayerElidinisStatuetteRestoration.QUEST_COMPLETE)

            prayAt(deps, statuette)

            assertEquals(70, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(10, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(10_000, player.runEnergy)
            assertEquals(1_000, player.vars[varps.sa_energy])
            assertEquals(0, player.vars[varps.poison])
            assertEquals(0, player.vars[varps.poison_2])
            assertEquals(0, player.vars[varbits.poison_type])

            deps.realm.updateConfig(deps.realm.config.copy(worldFlags = WorldFlag.Deadman.mask))
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 5)
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 10)
            player.runEnergy = 1_234
            player.setVarp(varps.sa_energy, 123)
            player.setVarp(varps.poison, 25)
            player.setVarp(varps.poison_2, 123)
            player.setVarBit(varbits.poison_type, 2)
            player.setVarBit(varbits.elidquest, PrayerElidinisStatuetteRestoration.QUEST_COMPLETE)

            prayAt(deps, statuette)

            assertEquals(70, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(99, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(10_000, player.runEnergy)
            assertEquals(1_000, player.vars[varps.sa_energy])
            assertEquals(0, player.vars[varps.poison])
            assertEquals(0, player.vars[varps.poison_2])
            assertEquals(0, player.vars[varbits.poison_type])

            deps.realm.updateConfig(deps.realm.config.copy(worldFlags = 0))
        }

    @Test
    fun GameTestState
        .`cache 238 God Wars altar configuration and affiliation catalogues are exact`() =
        runPrayerRemainsTest {
            assertEquals(4099, varbits.godwars_altar_delay.id)
            assertEquals(695, varbits.godwars_altar_delay.baseVar.id)
            assertEquals(6..9, varbits.godwars_altar_delay.bits)
            assertEquals(65502, varps.godwars_altar_available_at.id)
            assertEquals(VarpLifetime.Perm, varps.godwars_altar_available_at.scope)
            assertEquals(
                listOf(26363, 26364, 26365, 26366, 42965),
                prayer_altar_locs.godWarsByGod.map { (altar) -> altar.id },
            )
            assertTrue(
                prayer_altar_locs.godWarsRestoration.all { altar ->
                    locTypes.getValue(altar.id).op.toList() ==
                        listOf("Pray", "Teleport", null, null, null)
                }
            )
            assertEquals(
                mapOf(
                    PrayerGodWarsGod.Zamorak to CoordGrid(2925, 5333, 2),
                    PrayerGodWarsGod.Saradomin to CoordGrid(2909, 5265, 0),
                    PrayerGodWarsGod.Armadyl to CoordGrid(2839, 5294, 2),
                    PrayerGodWarsGod.Bandos to CoordGrid(2862, 5354, 2),
                    PrayerGodWarsGod.Zaros to CoordGrid(2900, 5204, 0),
                ),
                PrayerGodWarsGod.entries.associateWith(PrayerGodWarsAltarTeleport::destination),
            )

            assertEquals(
                EXPECTED_BANDOS_ITEMS,
                PrayerGodWarsObjs.bandos.mapTo(hashSetOf()) { it.id },
            )
            assertEquals(
                EXPECTED_ARMADYL_ITEMS,
                PrayerGodWarsObjs.armadyl.mapTo(hashSetOf()) { it.id },
            )
            assertEquals(EXPECTED_ZAROS_ITEMS, PrayerGodWarsObjs.zaros.mapTo(hashSetOf()) { it.id })

            val saradomin = cacheGodItems(params.saradominitem)
            val zamorak = cacheGodItems(params.zamorakitem)
            val guthix = cacheGodItems(params.guthixitem)
            assertEquals(80, saradomin.size)
            assertEquals(112, zamorak.size)
            assertEquals(90, guthix.size)
            assertTrue(saradomin.containsAll(setOf(542, 3840, 22326, 25731, 29617, 30367)))
            assertTrue(zamorak.containsAll(setOf(1033, 3842, 24288, 29566, 30750, 30777)))
        }

    @Test
    fun GameTestState.`all four original God Wars altars add one point per matching worn item`() =
        runPrayerRemainsTest { deps ->
            val cases =
                listOf(
                    prayer_altar_locs.godWarsSaradomin to 2665,
                    prayer_altar_locs.godWarsZamorak to 2657,
                    prayer_altar_locs.godWarsArmadyl to 11826,
                    prayer_altar_locs.godWarsBandos to 12486,
                )
            for ((index, case) in cases.withIndex()) {
                val (type, alignedHat) = case
                val altar = prepareAltar(type, xOffset = index * 3)
                preparePlayer()
                player.setBaseLevel(stats.prayer, 70)
                player.setCurrentLevel(stats.prayer, 1)
                player.hat = InvObj(alignedHat, 1)

                prayAt(deps, altar)

                assertEquals(
                    71,
                    player.statMap.getCurrentLevel(stats.prayer).toInt(),
                    type.toString(),
                )
                assertEquals(
                    PrayerGodWarsAltarRestoration.COOLDOWN_MINUTES,
                    player.vars[varbits.godwars_altar_delay],
                )
                assertTrue(
                    player.vars[varps.godwars_altar_available_at] > prayerGodWarsEpochSecond()
                )
            }
        }

    @Test
    fun GameTestState
        .`Nex altar only restores health run and special energy with Zaros equipment`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.godWarsZaros)
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 1)
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 10)
            player.runEnergy = 1_234
            player.setVarp(varps.sa_energy, 123)

            prayAt(deps, altar)

            assertEquals(70, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(10, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(1_234, player.runEnergy)
            assertEquals(123, player.vars[varps.sa_energy])

            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 1)
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 10)
            player.runEnergy = 1_234
            player.setVarp(varps.sa_energy, 123)
            player.hat = InvObj(26382, 1)
            player.torso = InvObj(30439, 1)
            player.legs = InvObj(26223, 1)

            prayAt(deps, altar)

            assertEquals(73, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(99, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(10_000, player.runEnergy)
            assertEquals(1_000, player.vars[varps.sa_energy])

            preparePlayer()
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 110)
            player.hat = InvObj(26382, 1)

            prayAt(deps, altar)

            assertEquals(110, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
        }

    @Test
    fun GameTestState.`God Wars altar cooldown is shared and enforced for a full ten minutes`() =
        runPrayerRemainsTest { deps ->
            val first = prepareAltar(prayer_altar_locs.godWarsBandos)
            val second = prepareAltar(prayer_altar_locs.godWarsArmadyl, xOffset = 3)
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 1)

            prayAt(deps, first)
            player.setCurrentLevel(stats.prayer, 1)
            prayAt(deps, second)

            assertEquals(1, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertMessageSent("You must wait another 10 minutes before you may use this altar.")

            player.setVarp(varps.godwars_altar_available_at, prayerGodWarsEpochSecond())
            prayAt(deps, second)
            assertEquals(70, player.statMap.getCurrentLevel(stats.prayer).toInt())
        }

    @Test
    fun GameTestState.`God Wars client timer is reconstructed from its persistent deadline`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            player.setVarp(varps.godwars_altar_available_at, prayerGodWarsEpochSecond() + 359)

            deps.godWarsAltarRestoration.syncClientTimer(player)

            assertEquals(6, player.vars[varbits.godwars_altar_delay])
            player.setVarp(varps.godwars_altar_available_at, prayerGodWarsEpochSecond())
            deps.godWarsAltarRestoration.syncClientTimer(player)
            assertEquals(0, player.vars[varbits.godwars_altar_delay])
        }

    @Test
    fun GameTestState.`God Wars altar rejects combat without starting its cooldown`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.godWarsSaradomin)
            preparePlayer()
            player.setBaseLevel(stats.prayer, 70)
            player.setCurrentLevel(stats.prayer, 1)
            player.setVarp(varps.lastcombat, player.currentMapClock)

            prayAt(deps, altar)

            assertEquals(1, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(0, player.vars[varps.godwars_altar_available_at])
            assertMessageSent("You cannot use the altar while in combat.")
        }

    @Test
    fun GameTestState.`God Wars altar Teleport exits every chamber even during combat`() =
        runPrayerRemainsTest { deps ->
            for ((index, altarAndGod) in prayer_altar_locs.godWarsByGod.withIndex()) {
                val (type, god) = altarAndGod
                val altar = prepareAltar(type, xOffset = index * 3)
                val destination = PrayerGodWarsAltarTeleport.destination(god)
                preparePlayer()
                val origin = player.coords
                player.placeAt(destination)
                player.placeAt(origin)
                player.setBaseLevel(stats.prayer, 70)
                player.setCurrentLevel(stats.prayer, 17)
                player.setBaseLevel(stats.hitpoints, 99)
                player.setCurrentLevel(stats.hitpoints, 41)
                player.runEnergy = 1_234
                player.setVarp(varps.sa_energy, 321)
                player.setVarBit(varbits.prayer_drain_counter, 47)
                player.setVarBit(varbits.godwars_altar_delay, 7)
                player.setVarp(varps.godwars_altar_available_at, 123_456)
                player.setVarp(varps.lastcombat, player.currentMapClock)

                prayAt(deps, altar, InteractionOp.Op2)

                assertEquals(destination, player.coords, god.toString())
                assertTrue(player.pendingTelejump, god.toString())
                assertEquals(17, player.statMap.getCurrentLevel(stats.prayer).toInt())
                assertEquals(41, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
                assertEquals(1_234, player.runEnergy)
                assertEquals(321, player.vars[varps.sa_energy])
                assertEquals(47, player.vars[varbits.prayer_drain_counter])
                assertEquals(7, player.vars[varbits.godwars_altar_delay])
                assertEquals(123_456, player.vars[varps.godwars_altar_available_at])
                assertNoMessageSent()
            }
        }

    @Test
    fun GameTestState.`cache 238 altar offering catalogue and visuals are exact`() =
        runPrayerRemainsTest {
            assertEquals(3705, seqs.human_bone_sacrifice.id)
            assertEquals(624, spotanims.poh_bone_sacrifice.id)
            assertEquals(411, prayer_altar_locs.chaos.id)
            assertEquals(28, prayer_altar_locs.chapel.size)
            assertEquals(
                (13179..13199).toSet() + (40872..40878).toSet(),
                prayer_altar_locs.chapel.mapTo(mutableSetOf()) { it.id },
            )
            assertEquals(7, prayer_altar_locs.chapelByTier.size)
            assertEquals(
                setOf(13209, 13211, 13213),
                prayer_altar_locs.litBurners.mapTo(mutableSetOf()) { it.id },
            )
            for ((tier, altars) in prayer_altar_locs.chapelByTier.withIndex()) {
                assertEquals(4, altars.size, "tier ${tier + 1}")
                for (altar in altars) {
                    assertEquals(true, locTypes[altar].op.any { it == "Pray" }, altar.toString())
                }
            }
            assertEquals(25434, objs.zealots_robe_top.id)
            assertEquals(25436, objs.zealots_robe_bottom.id)
            assertEquals(25438, objs.zealots_helm.id)
            assertEquals(25440, objs.zealots_boots.id)
        }

    @Test
    fun GameTestState.`chapel altar dispatch consumes top-left matching bone and sends visuals`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.chapelByTier[6].first())
            preparePlayer()
            add(objs.big_bones)
            add(objs.bones, 2)
            val selectedSlot = slotOfLast(objs.bones)
            val startXp = prayerXp()

            offerAt(deps, altar, selectedSlot, objs.bones)

            assertNull(player.inv[1])
            assertEquals(objs.bones.id, player.inv[selectedSlot]?.id)
            assertEquals(112, prayerXp() - startXp)
            assertEquals(seqs.human_bone_sacrifice.id, player.pendingSequence.id)
            assertMessageSent("The gods accept your offering.")
        }

    @Test
    fun GameTestState.`all seven chapel tiers apply exact base xp multipliers`() =
        runPrayerRemainsTest { deps ->
            val percentages = listOf(100, 110, 125, 150, 175, 200, 250)
            for ((tier, percentage) in percentages.withIndex()) {
                val altar = prepareAltar(prayer_altar_locs.chapelByTier[tier].first(), tier * 3)
                preparePlayer()
                add(objs.bones)
                val startXp = prayerXp()

                player.withProtectedAccess {
                    deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
                }

                assertEquals(
                    (4.5 * percentage / 100.0 * 10).toInt(),
                    prayerXp() - startXp,
                    "tier ${tier + 1}",
                )
            }
        }

    @Test
    fun GameTestState.`only lit incense burners add fifty percent each`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.chapelByTier[6].first())
            placeMapLoc(altar.coords.translateX(2), prayer_altar_locs.litOakBurner)
            placeMapLoc(altar.coords.translateZ(2), prayer_altar_locs.litMarbleBurner)
            preparePlayer()
            add(objs.bones)
            val startXp = prayerXp()

            player.withProtectedAccess {
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
            }

            assertEquals(157, prayerXp() - startXp)
            assertMessageSent("The gods are very pleased with your offering.")
        }

    @Test
    fun GameTestState.`chapel offering automatically repeats every three ticks`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.chapelByTier[0].first())
            preparePlayer()
            add(objs.bones, 3)

            player.withProtectedAccess {
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
            }
            assertEquals(2, player.count(objs.bones))
            advance(2)
            assertEquals(2, player.count(objs.bones))
            advance()
            assertEquals(1, player.count(objs.bones))
            advance(3)
            assertEquals(0, player.count(objs.bones))
            assertEquals(0, player.weakQueueList.count(prayer_remains_queues.altarOffering))
        }

    @Test
    fun GameTestState.`manual chapel offering restarts the automatic cadence after one tick`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.chapelByTier[0].first())
            preparePlayer()
            add(objs.bones, 3)

            player.withProtectedAccess {
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
            }
            advance()
            player.withProtectedAccess {
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
            }
            assertEquals(1, player.count(objs.bones))
            advance(2)
            assertEquals(1, player.count(objs.bones))
            advance()
            assertEquals(0, player.count(objs.bones))
        }

    @Test
    fun GameTestState.`chapel offering ignores a second manual use in the same tick`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.chapelByTier[0].first())
            preparePlayer()
            add(objs.bones, 3)

            player.withProtectedAccess {
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
            }

            assertEquals(2, player.count(objs.bones))
            assertEquals(45, prayerXp())
        }

    @Test
    fun GameTestState.`leaving the altar interrupts automatic offerings`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.chapelByTier[0].first())
            preparePlayer()
            add(objs.bones, 2)
            player.withProtectedAccess {
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
            }

            player.placeAt(altar.coords.translateX(4))
            advance(3)

            assertEquals(1, player.count(objs.bones))
            assertEquals(0, player.weakQueueList.count(prayer_remains_queues.altarOffering))
        }

    @Test
    fun GameTestState.`only the level 38 temple zone enables the cache-shared chaos altar`() =
        runPrayerRemainsTest { deps ->
            val wrongAltar = prepareAltar(prayer_altar_locs.chaos)
            preparePlayer()
            add(objs.bones)
            player.withProtectedAccess {
                deps.altarOffering.start(this, wrongAltar, slotOf(objs.bones), recipe(objs.bones))
            }
            assertEquals(1, player.count(objs.bones))
            assertEquals(0, prayerXp())
            assertMessageSent("You fear the wrath of the gods!")

            val chaosAltar = prepareChaosAltar()
            preparePlayer()
            add(objs.bones)
            random.next = 1
            val startXp = prayerXp()
            player.withProtectedAccess {
                deps.altarOffering.start(this, chaosAltar, slotOf(objs.bones), recipe(objs.bones))
            }
            assertEquals(0, player.count(objs.bones))
            assertEquals(157, prayerXp() - startXp)
        }

    @Test
    fun GameTestState.`chaos altar save roll grants xp without consuming the bone`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareChaosAltar()
            preparePlayer()
            add(objs.bones)
            random.next = 0
            val startXp = prayerXp()

            player.withProtectedAccess {
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
            }

            assertEquals(1, player.count(objs.bones))
            assertEquals(157, prayerXp() - startXp)
            assertMessageSent("Your sacrifice is spared!")
        }

    @Test
    fun GameTestState.`zealot pieces preserve bones and roll after a failed chaos save`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareChaosAltar()
            preparePlayer()
            player.hat = InvObj(objs.zealots_helm)
            player.torso = InvObj(objs.zealots_robe_top)
            add(objs.bones)
            random.next = 1
            random.then = 0

            player.withProtectedAccess {
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
            }

            assertEquals(1, player.count(objs.bones))
            assertEquals(157, prayerXp())

            preparePlayer()
            player.hat = InvObj(objs.zealots_helm)
            add(objs.bones)
            random.next = 0
            buryOrScatter(deps, objs.bones)
            assertEquals(1, player.count(objs.bones))
            assertEquals(45, prayerXp())
        }

    @Test
    fun GameTestState.`altar membership and superior bone level checks are atomic`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.chapelByTier[6].first())
            preparePlayer()
            player.members = false
            add(objs.bones)
            player.withProtectedAccess {
                deps.altarOffering.start(this, altar, slotOf(objs.bones), recipe(objs.bones))
            }
            assertEquals(1, player.count(objs.bones))
            assertEquals(0, prayerXp())
            assertMessageSent("You need to be on a members' world to offer bones at this altar.")

            preparePlayer()
            player.setBaseLevel(stats.prayer, 69)
            player.setCurrentLevel(stats.prayer, 70)
            add(objs.superior_dragon_bones)
            val level69Xp = prayerXp()
            player.withProtectedAccess {
                deps.altarOffering.start(
                    this,
                    altar,
                    slotOf(objs.superior_dragon_bones),
                    recipe(objs.superior_dragon_bones),
                )
            }
            assertEquals(1, player.count(objs.superior_dragon_bones))
            assertEquals(level69Xp, prayerXp())
            assertMessageSent("You need a Prayer level of 70 to use these bones.")
        }

    @Test
    fun GameTestState.`cache 238 ordinary remains catalogue is complete and exact`() =
        runPrayerRemainsTest {
            assertEquals(827, seqs.human_pickupfloor.id)
            assertEquals(2738, synths.bury_bones.id)
            assertEquals(16616, varbits.giant_bone_bury_warning_disable.id)
            assertEquals(EXPECTED_XP, PrayerRemainsRecipes.all.associate { it.obj.id to it.xp })
            assertEquals(
                EXPECTED_FREE_REMAINS,
                PrayerRemainsRecipes.all
                    .filterNot { it.members }
                    .mapTo(mutableSetOf()) { it.obj.id },
            )
            for (recipe in PrayerRemainsRecipes.all) {
                val type = objTypes[recipe.obj]
                val expectedOp =
                    when (recipe.kind) {
                        PrayerRemainsKind.Bone -> "Bury"
                        PrayerRemainsKind.DemonicAsh -> "Scatter"
                    }
                assertEquals(expectedOp, type.iop[0], "${type.name} (${recipe.obj.id})")
                assertEquals(recipe.members, type.members, "${type.name} (${recipe.obj.id})")
            }
        }

    @Test
    fun GameTestState.`every cache Bury or Scatter operation is explicitly classified`() =
        runPrayerRemainsTest {
            val cacheRemains =
                objTypes.entries
                    .filter { (_, type) -> type.iop.any { it == "Bury" || it == "Scatter" } }
                    .mapTo(mutableSetOf()) { it.key }
            val generic = PrayerRemainsRecipes.all.mapTo(mutableSetOf()) { it.obj.id }

            assertEquals(EXPECTED_QUEST_SCRIPTED_REMAINS, cacheRemains - generic)
            assertEquals(cacheRemains, generic + EXPECTED_QUEST_SCRIPTED_REMAINS)
        }

    @Test
    fun GameTestState.`inventory Bury dispatch consumes one bone with exact feedback and xp`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            add(objs.bones)
            val startXp = prayerXp()

            buryOrScatter(deps, objs.bones)

            assertEquals(0, player.count(objs.bones))
            assertEquals(45, prayerXp() - startXp)
            assertEquals(seqs.human_pickupfloor.id, player.pendingSequence.id)
            assertEquals(2738, client.filterIsInstance<SynthSound>().single().id)
            assertMessageSent("You bury the bones.")
            assertEquals(1, player.weakQueueList.count(prayer_remains_queues.boneCooldown))
        }

    @Test
    fun GameTestState.`barbarian ancestral charges triple burial xp and decrement once`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            player.setVarBit(varbits.brut_prayer_total, 2)
            add(objs.dragon_bones)
            val startXp = prayerXp()

            buryOrScatter(deps, objs.dragon_bones)

            assertEquals(1, player.vars[varbits.brut_prayer_total])
            assertEquals(2160, prayerXp() - startXp)
            assertMessageSent(
                "You gain extra prayer experience as the ancestral barbarian spirits watch over you."
            )
        }

    @Test
    fun GameTestState.`ancestral charge is spent when zealot preserves the buried bone`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            player.hat = InvObj(objs.zealots_helm)
            player.setVarBit(varbits.brut_prayer_total, 1)
            add(objs.bones)
            random.next = 0

            buryOrScatter(deps, objs.bones)

            assertEquals(1, player.count(objs.bones))
            assertEquals(0, player.vars[varbits.brut_prayer_total])
            assertEquals(135, prayerXp())
        }

    @Test
    fun GameTestState.`ancestral charges exclude big bones and demonic ashes`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            player.setVarBit(varbits.brut_prayer_total, 2)
            add(objs.big_bones)
            add(objs.vile_ashes)

            buryOrScatter(deps, objs.big_bones)
            buryOrScatter(deps, objs.vile_ashes)

            assertEquals(2, player.vars[varbits.brut_prayer_total])
            assertEquals(400, prayerXp())
        }

    @Test
    fun GameTestState.`ancestral charges do not affect altar offerings`() =
        runPrayerRemainsTest { deps ->
            val altar = prepareAltar(prayer_altar_locs.chapelByTier[6].first())
            preparePlayer()
            player.setVarBit(varbits.brut_prayer_total, 2)
            add(objs.bones)

            offerAt(deps, altar, slotOf(objs.bones), objs.bones)

            assertEquals(2, player.vars[varbits.brut_prayer_total])
            assertEquals(112, prayerXp())
        }

    @Test
    fun GameTestState.`same remains type is rate limited to one action per two ticks`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            add(objs.bones, 2)
            val startXp = prayerXp()

            buryOrScatter(deps, objs.bones)
            buryOrScatter(deps, objs.bones)
            assertEquals(1, player.count(objs.bones))
            assertEquals(45, prayerXp() - startXp)

            advance(2)
            buryOrScatter(deps, objs.bones)
            assertEquals(0, player.count(objs.bones))
            assertEquals(90, prayerXp() - startXp)
        }

    @Test
    fun GameTestState.`bone and ash award simultaneously through independent cooldowns`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            add(objs.big_bones)
            add(objs.vile_ashes)
            val startXp = prayerXp()

            buryOrScatter(deps, objs.big_bones)
            buryOrScatter(deps, objs.vile_ashes)

            assertEquals(0, player.count(objs.big_bones))
            assertEquals(0, player.count(objs.vile_ashes))
            assertEquals(400, prayerXp() - startXp)
            assertMessageSent("You bury the bones.")
            assertMessageSent("You scatter the ashes.")
            assertEquals(1, player.weakQueueList.count(prayer_remains_queues.boneCooldown))
            assertEquals(1, player.weakQueueList.count(prayer_remains_queues.ashCooldown))
        }

    @Test
    fun GameTestState.`every ordinary remains grants its exact fine xp`() =
        runPrayerRemainsTest { deps ->
            for (recipe in PrayerRemainsRecipes.all.filterNot { it.confirmBurial }) {
                preparePlayer()
                player.setBaseLevel(stats.prayer, 99)
                player.setCurrentLevel(stats.prayer, 99)
                add(recipe.obj)
                val startXp = prayerXp()

                player.withProtectedAccess { deps.remains.offer(this, slotOf(recipe.obj), recipe) }

                assertEquals(
                    (recipe.xp * 10).toInt(),
                    prayerXp() - startXp,
                    "${objTypes[recipe.obj].name} (${recipe.obj.id})",
                )
                assertEquals(0, player.count(recipe.obj))
            }
        }

    @Test
    fun GameTestState.`members and unboostable superior-bone boundaries are atomic`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.dragon_bones)
            val startXp = prayerXp()
            buryOrScatter(deps, objs.dragon_bones)
            assertEquals(1, player.count(objs.dragon_bones))
            assertEquals(startXp, prayerXp())
            assertMessageSent("You need to be on a members' world to use this item.")

            preparePlayer()
            player.setBaseLevel(stats.prayer, 69)
            player.setCurrentLevel(stats.prayer, 70)
            add(objs.superior_dragon_bones)
            val level69Xp = prayerXp()
            buryOrScatter(deps, objs.superior_dragon_bones)
            assertEquals(1, player.count(objs.superior_dragon_bones))
            assertEquals(level69Xp, prayerXp())
            assertMessageSent("You need a Prayer level of 70 to use these bones.")

            player.setBaseLevel(stats.prayer, 70)
            val level70Xp = prayerXp()
            buryOrScatter(deps, objs.superior_dragon_bones)
            assertEquals(0, player.count(objs.superior_dragon_bones))
            assertEquals(level70Xp + 1500, prayerXp())
        }

    @Test
    fun GameTestState.`special bone warning accepts confirmation`() = runPrayerRemainsTest { deps ->
        preparePlayer()
        add(objs.long_bone)
        player.withProtectedAccess {
            deps.remains.offer(this, slotOf(objs.long_bone), recipe(objs.long_bone))
        }
        confirmChoice()
        assertEquals(0, player.count(objs.long_bone))
        assertEquals(151, prayerXp())
    }

    @Test
    fun GameTestState.`special bone warning can be declined`() = runPrayerRemainsTest { deps ->
        preparePlayer()
        add(objs.long_bone)
        player.withProtectedAccess {
            deps.remains.offer(this, slotOf(objs.long_bone), recipe(objs.long_bone))
        }
        declineChoice()
        assertEquals(1, player.count(objs.long_bone))
        assertEquals(0, prayerXp())
    }

    @Test
    fun GameTestState.`special bone warning varbit bypasses confirmation`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            add(objs.curved_bone)
            player.setVarBit(varbits.giant_bone_bury_warning_disable, 1)
            buryOrScatter(deps, objs.curved_bone)
            assertEquals(0, player.count(objs.curved_bone))
            assertEquals(151, prayerXp())
        }

    @Test
    fun GameTestState.`special bone warning revalidates the selected slot`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            add(objs.long_bone)
            val slot = slotOf(objs.long_bone)
            player.withProtectedAccess { deps.remains.offer(this, slot, recipe(objs.long_bone)) }
            player.inv[slot] = InvObj(objs.bones)
            confirmChoice()
            assertEquals(objs.bones.id, player.inv[slot]?.id)
            assertEquals(0, prayerXp())
        }

    @Test
    fun GameTestState.`quest scripted remains are not consumed by the generic plugin`() =
        runPrayerRemainsTest { deps ->
            preparePlayer()
            player.inv[0] = InvObj(610, 1)

            player.withProtectedAccess { deps.held.interact(this, player.inv, 0, HeldOp.Op1) }

            assertEquals(610, player.inv[0]?.id)
            assertEquals(0, prayerXp())
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.observerUUID = 1L
        player.members = true
        player.setBaseLevel(stats.prayer, 1)
        player.setCurrentLevel(stats.prayer, 1)
        player.statMap.setXP(stats.prayer, 0)
        player.weakQueueList.clear()
        player.hat = null
        player.torso = null
        player.legs = null
        player.feet = null
        player.setVarp(varps.lastcombat, -100)
        player.setVarp(varps.lastcombat_pvp, -100)
        player.setVarp(varps.godwars_altar_available_at, 0)
        player.setVarBit(varbits.godwars_altar_delay, 0)
        player.setVarBit(varbits.brut_prayer_total, 0)
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestScope.buryOrScatter(deps: Dependencies, obj: ObjType) {
        val slot = slotOf(obj)
        player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
    }

    private fun GameTestScope.slotOf(obj: ObjType): Int =
        player.inv.indexOfFirst { it?.id == obj.id }.also { require(it >= 0) }

    private fun GameTestScope.slotOfLast(obj: ObjType): Int =
        player.inv.indexOfLast { it?.id == obj.id }.also { require(it >= 0) }

    private fun GameTestScope.prepareAltar(type: LocType, xOffset: Int = 0): BoundLocInfo {
        val altar = placeMapLoc(CoordGrid(0, 50, 50, 32 + xOffset, 32), type)
        player.placeAt(altar.coords.translateX(-1))
        return altar
    }

    private fun GameTestScope.prepareChaosAltar(): BoundLocInfo {
        val altar = placeMapLoc(CoordGrid(0, 46, 59, 4, 44), prayer_altar_locs.chaos)
        player.placeAt(altar.coords.translateX(-1))
        return altar
    }

    private fun GameTestScope.offerAt(
        deps: Dependencies,
        altar: BoundLocInfo,
        selectedSlot: Int,
        obj: ObjType,
    ) {
        player.withProtectedAccess {
            deps.locU.interactOp(
                this,
                altar,
                altar,
                locTypes.getValue(altar.id),
                objTypes[obj],
                player.inv,
                selectedSlot,
            )
        }
    }

    private fun GameTestScope.prayAt(
        deps: Dependencies,
        altar: BoundLocInfo,
        op: InteractionOp = InteractionOp.Op1,
    ) {
        val event =
            deps.locs.opTrigger(
                player = player,
                loc = altar,
                op = op,
                type = locTypes.getValue(altar.id),
            )
        assertNotNull(event)
        when (op) {
            InteractionOp.Op1 ->
                assertTrue(event is org.rsmod.api.player.events.interact.LocEvents.Op1)
            InteractionOp.Op2 ->
                assertTrue(event is org.rsmod.api.player.events.interact.LocEvents.Op2)
            else -> error("Unsupported Pray operation: $op")
        }
        player.withProtectedAccess { deps.events.publish(this, requireNotNull(event)) }
    }

    private fun GameTestScope.prayerXp(): Int = player.statMap.getFineXP(stats.prayer)

    private fun GameTestScope.cacheGodItems(
        param: org.rsmod.game.type.param.ParamType<Int>
    ): Set<Int> =
        objTypes.entries
            .filter { (_, type) -> type.wearpos1 != -1 && type.paramMap?.getOrNull(param) == 1 }
            .mapTo(hashSetOf()) { it.key }

    private fun recipe(obj: ObjType): PrayerRemainsRecipe =
        requireNotNull(PrayerRemainsRecipes.byObj[obj.id])

    private fun GameTestScope.confirmChoice() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
        )
    }

    private fun GameTestScope.declineChoice() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 2)
        )
    }

    private fun GameTestState.runPrayerRemainsTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            PrayerRemainsScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(PrayerRemainsModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val remains: PrayerRemains,
        val altarOffering: PrayerAltarOffering,
        val altarRestoration: PrayerAltarRestoration,
        val hazardousAltarRestoration: PrayerHazardousAltarRestoration,
        val godWarsAlignment: PrayerGodWarsAlignment,
        val godWarsAltarRestoration: PrayerGodWarsAltarRestoration,
        val held: HeldInteractions,
        val locs: LocInteractions,
        val locU: LocUInteractions,
        val events: EventBus,
        val realm: Realm,
        val dbRows: DbRowTypeList,
        val objRepo: ObjRepository,
    )

    private companion object {
        const val DESERT_TREASURE_ROW = 27
        const val LAND_OF_THE_GOBLINS_ROW = 165
        const val LUNAR_DIPLOMACY_ROW = 88
        const val SPIRITS_OF_THE_ELID_ROW = 139

        val EXPECTED_BANDOS_ITEMS =
            setOf(
                11061,
                11804,
                11832,
                11834,
                11836,
                12265,
                12267,
                12269,
                12271,
                12273,
                12275,
                12480,
                12482,
                12484,
                12486,
                12488,
                12498,
                12500,
                12502,
                12504,
                12607,
                12608,
                19924,
                20232,
                20370,
                21733,
                23203,
                24195,
                26494,
                28945,
            )

        val EXPECTED_ARMADYL_ITEMS =
            setOf(
                87,
                11785,
                11802,
                11826,
                11828,
                11830,
                12253,
                12255,
                12257,
                12259,
                12261,
                12263,
                12470,
                12472,
                12474,
                12476,
                12478,
                12506,
                12508,
                12510,
                12512,
                12609,
                12610,
                19930,
                20229,
                20368,
                22547,
                22550,
                23200,
                24192,
                26492,
                27652,
                27655,
                29605,
            )

        val EXPECTED_ZAROS_ITEMS =
            setOf(
                4675,
                12193,
                12195,
                12197,
                12199,
                12201,
                12203,
                12460,
                12462,
                12464,
                12466,
                12468,
                12490,
                12492,
                12494,
                12496,
                12611,
                12612,
                19921,
                20128,
                20131,
                20134,
                20137,
                20140,
                20235,
                23197,
                24201,
                26221,
                26223,
                26225,
                26227,
                26229,
                26233,
                26235,
                26241,
                26243,
                26245,
                26374,
                26382,
                26384,
                26386,
                26490,
                27610,
                27612,
                27624,
                27626,
                28254,
                28256,
                28258,
                28260,
                28262,
                28264,
                28266,
                28473,
                28474,
                28475,
                28476,
                30434,
                30436,
                30437,
                30439,
                30441,
            )

        val EXPECTED_XP =
            linkedMapOf(
                526 to 4.5,
                528 to 4.5,
                530 to 5.3,
                532 to 15.0,
                534 to 30.0,
                536 to 72.0,
                31726 to 60.0,
                31729 to 100.0,
                2530 to 4.5,
                2859 to 4.5,
                3123 to 25.0,
                3125 to 15.0,
                3179 to 16.0,
                3180 to 18.0,
                3181 to 18.0,
                3182 to 18.0,
                3183 to 5.0,
                3185 to 5.0,
                3186 to 5.0,
                3187 to 3.0,
                4812 to 22.5,
                4830 to 84.0,
                4832 to 96.0,
                4834 to 140.0,
                6729 to 125.0,
                6812 to 72.0,
                10976 to 15.1,
                10977 to 15.1,
                11943 to 85.0,
                22124 to 150.0,
                22780 to 50.0,
                22783 to 80.0,
                22786 to 110.0,
                24655 to 4.5,
                25422 to 4.5,
                25766 to 10.0,
                25769 to 25.0,
                25772 to 65.0,
                25775 to 85.0,
                25778 to 110.0,
                28899 to 21.0,
                30973 to 3.0,
            )

        val EXPECTED_FREE_REMAINS = setOf(526, 528, 532, 2530, 3183, 24655, 25766, 25769)

        val EXPECTED_QUEST_SCRIPTED_REMAINS =
            setOf(
                610,
                3127,
                3128,
                3129,
                3130,
                3131,
                3132,
                3133,
                4488,
                25199,
                26589,
                26590,
                26591,
                26592,
                26593,
            )
    }
}
