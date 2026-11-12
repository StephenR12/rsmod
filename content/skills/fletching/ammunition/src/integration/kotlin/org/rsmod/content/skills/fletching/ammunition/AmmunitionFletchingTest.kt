package org.rsmod.content.skills.fletching.ammunition

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.dbtable.DbTableTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class AmmunitionFletchingTest {
    @Test
    fun GameTestState.`cache 233 pins all standard ammunition recipe contracts`() =
        runAmmunitionTest { deps ->
            assertEquals(
                listOf(
                    "Headless arrow:HeadlessArrow:1:52+314=53:1.0:8481:false",
                    "Bronze arrow:Arrow:1:53+39=882:1.3:8480:false",
                    "Iron arrow:Arrow:15:53+40=884:2.5:8480:false",
                    "Steel arrow:Arrow:30:53+41=886:5.0:8480:false",
                    "Mithril arrow:Arrow:45:53+42=888:7.5:8480:false",
                    "Broad arrow:Arrow:52:53+11874=4160:10.0:8480:true",
                    "Adamant arrow:Arrow:60:53+43=890:10.0:8480:false",
                    "Rune arrow:Arrow:75:53+44=892:12.5:8480:false",
                    "Amethyst arrow:Arrow:82:53+21350=21326:13.5:8480:false",
                    "Dragon arrow:Arrow:90:53+11237=11212:15.0:8480:false",
                    "Bronze bolts:Bolt:9:9375+314=877:0.5:8463:false",
                    "Blurite bolts:Bolt:24:9376+314=9139:1.0:8466:false",
                    "Iron bolts:Bolt:39:9377+314=9140:1.5:8464:false",
                    "Silver bolts:Bolt:43:9382+314=9145:2.5:8465:false",
                    "Steel bolts:Bolt:46:9378+314=9141:3.5:8467:false",
                    "Mithril bolts:Bolt:54:9379+314=9142:5.0:8468:false",
                    "Broad bolts:Bolt:55:11876+314=11875:3.0:8463:true",
                    "Adamant bolts:Bolt:61:9380+314=9143:7.0:8469:false",
                    "Runite bolts:Bolt:69:9381+314=9144:10.0:8470:false",
                    "Dragon bolts:Bolt:84:21930+314=21905:12.0:8471:false",
                    "Opal bolts:TippedBolt:11:877+45=879:1.6:8472:false",
                    "Jade bolts:TippedBolt:26:9139+9187=9335:2.4:8474:false",
                    "Pearl bolts:TippedBolt:41:9140+46=880:3.2:8473:false",
                    "Topaz bolts:TippedBolt:48:9141+9188=9336:3.9:8475:false",
                    "Barbed bolts:TippedBolt:51:877+47=881:9.5:8472:false",
                    "Sapphire bolts:TippedBolt:56:9142+9189=9337:4.7:8476:false",
                    "Emerald bolts:TippedBolt:58:9142+9190=9338:5.5:8476:false",
                    "Ruby bolts:TippedBolt:63:9143+9191=9339:6.3:8477:false",
                    "Diamond bolts:TippedBolt:65:9143+9192=9340:7.0:8477:false",
                    "Dragonstone bolts:TippedBolt:71:9144+9193=9341:8.2:8478:false",
                    "Onyx bolts:TippedBolt:73:9144+9194=9342:9.4:8478:false",
                    "Amethyst broad bolts:TippedBolt:76:11875+21338=21316:10.6:8473:true",
                    "Opal dragon bolts:TippedBolt:84:21905+45=21955:1.6:8479:false",
                    "Jade dragon bolts:TippedBolt:84:21905+9187=21957:2.4:8479:false",
                    "Pearl dragon bolts:TippedBolt:84:21905+46=21959:3.2:8479:false",
                    "Topaz dragon bolts:TippedBolt:84:21905+9188=21961:4.0:8479:false",
                    "Sapphire dragon bolts:TippedBolt:84:21905+9189=21963:4.7:8479:false",
                    "Emerald dragon bolts:TippedBolt:84:21905+9190=21965:5.5:8479:false",
                    "Ruby dragon bolts:TippedBolt:84:21905+9191=21967:6.3:8479:false",
                    "Diamond dragon bolts:TippedBolt:84:21905+9192=21969:7.0:8479:false",
                    "Dragonstone dragon bolts:TippedBolt:84:21905+9193=21971:8.2:8479:false",
                    "Onyx dragon bolts:TippedBolt:84:21905+9194=21973:9.4:8479:false",
                    "Bronze dart:Dart:10:819+314=806:1.8:8482:false",
                    "Iron dart:Dart:22:820+314=807:3.8:8483:false",
                    "Steel dart:Dart:37:821+314=808:7.5:8484:false",
                    "Mithril dart:Dart:52:822+314=809:11.2:8485:false",
                    "Adamant dart:Dart:67:823+314=810:15.0:8486:false",
                    "Rune dart:Dart:81:824+314=811:18.8:8487:false",
                    "Amethyst dart:Dart:90:25853+314=25849:21.0:9108:false",
                    "Dragon dart:Dart:95:11232+314=11230:25.0:8488:false",
                    "Bronze javelin:Javelin:3:19570+19584=825:1.0:-1:false",
                    "Iron javelin:Javelin:17:19572+19584=826:2.0:-1:false",
                    "Steel javelin:Javelin:32:19574+19584=827:5.0:-1:false",
                    "Mithril javelin:Javelin:47:19576+19584=828:8.0:-1:false",
                    "Adamant javelin:Javelin:62:19578+19584=829:10.0:-1:false",
                    "Rune javelin:Javelin:77:19580+19584=830:12.4:-1:false",
                    "Amethyst javelin:Javelin:84:21352+19584=21318:13.5:-1:false",
                    "Dragon javelin:Javelin:92:19582+19584=19484:15.0:-1:false",
                ),
                deps.recipes.recipes
                    .filter {
                        it.family != AmmunitionFamily.FlightedOgreArrow &&
                            it.family != AmmunitionFamily.OgreArrow &&
                            it.family != AmmunitionFamily.BrutalArrow
                    }
                    .map {
                        "${it.name}:${it.family}:${it.level}:${it.primary.id}+${it.secondary.id}=" +
                            "${it.output.id}:${it.xpPerItem}:${it.sequence?.id ?: -1}:" +
                            it.requiresBroaderFletching
                    },
            )
            assertEquals(10971, varbits.option_fletch_dart_or_bolt_makex.id)
            assertEquals(3208, varbits.slayer_ammo_unlocked.id)
        }

    @Test
    fun GameTestState.`cache 233 pins the flighted ogre arrow contract`() =
        runAmmunitionTest { deps ->
            val recipe =
                deps.recipes.recipes.single { it.family == AmmunitionFamily.FlightedOgreArrow }
            assertEquals("Flighted ogre arrow", recipe.name)
            assertEquals(5, recipe.level)
            assertEquals(2864, recipe.primary.id)
            assertEquals(314, recipe.secondary.id)
            assertEquals(2865, recipe.output.id)
            assertEquals(0.9, recipe.xpPerItem)
            assertEquals(4, recipe.secondaryPerItem)
            assertEquals(6, recipe.family.batchSize)
            assertEquals(seqs.human_ogre_fletching, recipe.sequence)
            assertTrue(recipe.requiresBigChompyStarted)
            assertEquals(293, varps.big_chompy_bird_hunting.id)
        }

    @Test
    fun GameTestState.`flighted ogre arrows consume four feathers each after two ticks`() =
        runAmmunitionTest { deps ->
            preparePlayer()
            val recipe =
                deps.recipes.recipes.single { it.family == AmmunitionFamily.FlightedOgreArrow }
            add(recipe.primary, 6)
            add(recipe.secondary, 24)
            val startXp = player.statMap.getFineXP(stats.fletching)

            player.withProtectedAccess { deps.fletching.start(this, recipe, quantity = 1) }

            assertEquals(seqs.human_ogre_fletching.id, player.pendingSequence.id)
            advance()
            assertEquals(0, player.count(recipe.output))
            advance()
            assertEquals(6, player.count(recipe.output))
            assertEquals(0, player.count(recipe.primary))
            assertEquals(0, player.count(recipe.secondary))
            assertEquals(54, player.statMap.getFineXP(stats.fletching) - startXp)
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
        }

    @Test
    fun GameTestState
        .`flighted ogre arrows enforce quest membership level ratio and reverse use`() =
        runAmmunitionTest { deps ->
            val recipe =
                deps.recipes.recipes.single { it.family == AmmunitionFamily.FlightedOgreArrow }

            preparePlayer()
            player.setVarp(varps.big_chompy_bird_hunting, 0)
            add(recipe.primary, 6)
            add(recipe.secondary, 24)
            player.withProtectedAccess { deps.fletching.start(this, recipe, 1) }
            assertMessageSent("You need to start Big Chompy Bird Hunting first.")
            assertEquals(6, player.count(recipe.primary))
            assertEquals(24, player.count(recipe.secondary))

            preparePlayer()
            player.members = false
            add(recipe.primary, 6)
            add(recipe.secondary, 24)
            player.withProtectedAccess { deps.fletching.start(this, recipe, 1) }
            assertMessageSent("You need to be on a members' world to train Fletching.")
            assertEquals(6, player.count(recipe.primary))
            assertEquals(24, player.count(recipe.secondary))

            preparePlayer()
            player.stats[stats.fletching] = 4
            add(recipe.primary, 6)
            add(recipe.secondary, 24)
            player.withProtectedAccess { deps.fletching.start(this, recipe, 1) }
            assertMessageSent("You need a Fletching level of 5 to make flighted ogre arrow.")
            assertEquals(6, player.count(recipe.primary))
            assertEquals(24, player.count(recipe.secondary))

            preparePlayer()
            add(recipe.primary)
            add(recipe.secondary, 3)
            player.withProtectedAccess { deps.fletching.start(this, recipe, 1) }
            assertMessageSent("You do not have the required feather.")
            assertEquals(1, player.count(recipe.primary))
            assertEquals(3, player.count(recipe.secondary))
            assertEquals(0, player.count(recipe.output))

            preparePlayer()
            add(recipe.primary, 4)
            add(recipe.secondary, 17)
            repeat(26) { add(objs.logs) }
            val primarySlot = player.inv.indexOfFirst { it?.id == recipe.primary.id }
            val secondarySlot = player.inv.indexOfFirst { it?.id == recipe.secondary.id }
            val startXp = player.statMap.getFineXP(stats.fletching)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.secondary],
                    secondarySlot,
                    objTypes[recipe.primary],
                    primarySlot,
                )
            }
            advance(2)
            assertEquals(4, player.count(recipe.output))
            assertEquals(0, player.count(recipe.primary))
            assertEquals(1, player.count(recipe.secondary))
            assertEquals(36, player.statMap.getFineXP(stats.fletching) - startXp)
            assertEquals(28, player.inv.count { it != null })
        }

    @Test
    fun GameTestState.`cache 233 pins the final ogre arrow contract and quest boundaries`() =
        runAmmunitionTest { deps ->
            val recipe = deps.recipes.recipes.single { it.family == AmmunitionFamily.OgreArrow }
            assertEquals("Ogre arrow", recipe.name)
            assertEquals(5, recipe.level)
            assertEquals(2865, recipe.primary.id)
            assertEquals(2861, recipe.secondary.id)
            assertEquals(2866, recipe.output.id)
            assertEquals(1.0, recipe.xpPerItem)
            assertEquals(6, recipe.family.batchSize)
            assertEquals(seqs.human_ogre_fletching, recipe.sequence)
            assertTrue(recipe.requiresBigChompyStarted)
            assertEquals(293, varps.big_chompy_bird_hunting.id)
            assertEquals(listOf(0), deps.dbTables.types.getValue(0).defaults[18])
            assertEquals(listOf(65), deps.dbRows.types.getValue(8).data[19])
        }

    @Test
    fun GameTestState.`ogre arrows make six after two ticks with exact xp and animation`() =
        runAmmunitionTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.family == AmmunitionFamily.OgreArrow }
            add(recipe.primary, 6)
            add(recipe.secondary, 6)
            val startXp = player.statMap.getFineXP(stats.fletching)

            player.withProtectedAccess { deps.fletching.start(this, recipe, quantity = 1) }

            assertEquals(seqs.human_ogre_fletching.id, player.pendingSequence.id)
            advance()
            assertEquals(0, player.count(recipe.output))
            advance()
            assertEquals(6, player.count(recipe.output))
            assertEquals(0, player.count(recipe.primary))
            assertEquals(0, player.count(recipe.secondary))
            assertEquals(60, player.statMap.getFineXP(stats.fletching) - startXp)
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
        }

    @Test
    fun GameTestState.`ogre arrow quest level reverse use and partial full inventory are atomic`() =
        runAmmunitionTest { deps ->
            val recipe = deps.recipes.recipes.single { it.family == AmmunitionFamily.OgreArrow }

            preparePlayer()
            player.setVarp(varps.big_chompy_bird_hunting, 0)
            add(recipe.primary, 6)
            add(recipe.secondary, 6)
            player.withProtectedAccess { deps.fletching.start(this, recipe, 1) }
            assertMessageSent("You need to start Big Chompy Bird Hunting first.")
            assertInputsRemain(recipe, 6)

            preparePlayer()
            player.members = false
            add(recipe.primary, 6)
            add(recipe.secondary, 6)
            player.withProtectedAccess { deps.fletching.start(this, recipe, 1) }
            assertMessageSent("You need to be on a members' world to train Fletching.")
            assertInputsRemain(recipe, 6)

            preparePlayer()
            player.stats[stats.fletching] = 4
            add(recipe.primary, 6)
            add(recipe.secondary, 6)
            player.withProtectedAccess { deps.fletching.start(this, recipe, 1) }
            assertMessageSent("You need a Fletching level of 5 to make ogre arrow.")
            assertInputsRemain(recipe, 6)

            preparePlayer()
            add(recipe.primary, 4)
            add(recipe.secondary, 4)
            repeat(26) { add(objs.logs) }
            assertEquals(28, player.inv.count { it != null })
            val primarySlot = player.inv.indexOfFirst { it?.id == recipe.primary.id }
            val secondarySlot = player.inv.indexOfFirst { it?.id == recipe.secondary.id }
            val startXp = player.statMap.getFineXP(stats.fletching)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.secondary],
                    secondarySlot,
                    objTypes[recipe.primary],
                    primarySlot,
                )
            }
            advance(2)
            assertEquals(4, player.count(recipe.output))
            assertEquals(0, player.count(recipe.primary))
            assertEquals(0, player.count(recipe.secondary))
            assertEquals(40, player.statMap.getFineXP(stats.fletching) - startXp)
            assertEquals(27, player.inv.count { it != null })
        }

    @Test
    fun GameTestState.`cache 233 pins all brutal arrow contracts`() = runAmmunitionTest { deps ->
        val recipes = deps.recipes.recipes.filter { it.family == AmmunitionFamily.BrutalArrow }
        assertEquals(
            listOf(
                "Bronze brutal:7:2865+4819=4773:1.4",
                "Iron brutal:18:2865+4820=4778:2.6",
                "Steel brutal:33:2865+1539=4783:5.1",
                "Black brutal:38:2865+4821=4788:6.5",
                "Mithril brutal:49:2865+4822=4793:7.5",
                "Adamant brutal:62:2865+4823=4798:10.2",
                "Rune brutal:77:2865+4824=4803:12.5",
            ),
            recipes.map {
                "${it.name}:${it.level}:${it.primary.id}+${it.secondary.id}=${it.output.id}:" +
                    it.xpPerItem
            },
        )
        assertTrue(recipes.all { it.requiresHammer && it.requiresBrutalArrowUnlock })
        assertTrue(recipes.all { it.sequence == seqs.human_ogre_fletching })
        assertEquals(4433, seqs.human_ogre_fletching.id)
        assertEquals(499, varbits.zogre_brutal_arrow_unlocked.id)
    }

    @Test
    fun GameTestState.`all brutal arrows make six after two ticks with exact xp and animation`() =
        runAmmunitionTest { deps ->
            val recipes = deps.recipes.recipes.filter { it.family == AmmunitionFamily.BrutalArrow }
            for (recipe in recipes) {
                preparePlayer()
                player.setVarBit(varbits.zogre_brutal_arrow_unlocked, 1)
                add(objs.hammer)
                add(recipe.primary, 6)
                add(recipe.secondary, 6)
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess { deps.fletching.start(this, recipe, quantity = 1) }

                assertEquals(seqs.human_ogre_fletching.id, player.pendingSequence.id, recipe.name)
                assertEquals(0, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(0, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(6, player.count(recipe.output), recipe.name)
                assertEquals(0, player.count(recipe.primary), recipe.name)
                assertEquals(0, player.count(recipe.secondary), recipe.name)
                assertEquals(
                    (recipe.xpPerItem * 60).toInt(),
                    player.statMap.getFineXP(stats.fletching) - startXp,
                    recipe.name,
                )
                assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            }
        }

    @Test
    fun GameTestState.`brutal arrow requirements and reverse partial use are atomic`() =
        runAmmunitionTest { deps ->
            val rune = deps.recipes.recipes.single { it.output == objs.rune_brutal }

            preparePlayer()
            add(objs.hammer)
            add(rune.primary, 6)
            add(rune.secondary, 6)
            player.withProtectedAccess { deps.fletching.start(this, rune, 1) }
            assertMessageSent("You need to learn how to make brutal arrows first.")
            assertInputsRemain(rune, 6)

            preparePlayer()
            player.setVarBit(varbits.zogre_brutal_arrow_unlocked, 1)
            add(rune.primary, 6)
            add(rune.secondary, 6)
            player.withProtectedAccess { deps.fletching.start(this, rune, 1) }
            assertMessageSent("You need a hammer to make brutal arrows.")
            assertInputsRemain(rune, 6)

            preparePlayer()
            player.setVarBit(varbits.zogre_brutal_arrow_unlocked, 1)
            player.stats[stats.fletching] = 76
            add(objs.hammer)
            add(rune.primary, 6)
            add(rune.secondary, 6)
            player.withProtectedAccess { deps.fletching.start(this, rune, 1) }
            assertMessageSent("You need a Fletching level of 77 to make rune brutal.")
            assertInputsRemain(rune, 6)

            preparePlayer()
            player.setVarBit(varbits.zogre_brutal_arrow_unlocked, 1)
            val adamant = deps.recipes.recipes.single { it.output == objs.adamant_brutal }
            add(objs.hammer)
            add(adamant.primary, 4)
            add(adamant.secondary, 4)
            repeat(25) { add(objs.logs) }
            val primarySlot = player.inv.indexOfFirst { it?.id == adamant.primary.id }
            val secondarySlot = player.inv.indexOfFirst { it?.id == adamant.secondary.id }
            val startXp = player.statMap.getFineXP(stats.fletching)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[adamant.secondary],
                    secondarySlot,
                    objTypes[adamant.primary],
                    primarySlot,
                )
            }
            advance(2)
            assertEquals(4, player.count(adamant.output))
            assertEquals(0, player.count(adamant.primary))
            assertEquals(0, player.count(adamant.secondary))
            assertEquals(408, player.statMap.getFineXP(stats.fletching) - startXp)
        }

    @Test
    fun GameTestState.`all bolts and darts make ten immediately with exact xp and animation`() =
        runAmmunitionTest { deps ->
            val immediate =
                deps.recipes.recipes.filter {
                    it.family == AmmunitionFamily.Bolt || it.family == AmmunitionFamily.Dart
                }
            for (recipe in immediate) {
                preparePlayer()
                add(recipe.primary, 12)
                add(recipe.secondary, 12)
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess { deps.fletching.start(this, recipe, quantity = 1) }

                assertEquals(
                    requireNotNull(recipe.sequence).id,
                    player.pendingSequence.id,
                    recipe.name,
                )
                assertEquals(2, player.count(recipe.primary), recipe.name)
                assertEquals(2, player.count(recipe.secondary), recipe.name)
                assertEquals(10, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xpPerItem * 100).toInt(),
                    player.statMap.getFineXP(stats.fletching) - startXp,
                    recipe.name,
                )
                assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            }
        }

    @Test
    fun GameTestState.`all javelins use rapid first two sets then two tick repeats`() =
        runAmmunitionTest { deps ->
            val javelins = deps.recipes.recipes.filter { it.family == AmmunitionFamily.Javelin }
            for (recipe in javelins) {
                preparePlayer()
                add(recipe.primary, 50)
                add(recipe.secondary, 50)
                val startXp = player.statMap.getFineXP(stats.fletching)
                val startSequence = player.pendingSequence.id

                player.withProtectedAccess { deps.fletching.start(this, recipe, quantity = 4) }
                assertEquals(0, player.count(recipe.output), recipe.name)
                assertEquals(startSequence, player.pendingSequence.id, recipe.name)
                advance()
                assertEquals(15, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(30, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(30, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(45, player.count(recipe.output), recipe.name)
                advance(2)
                assertEquals(50, player.count(recipe.output), recipe.name)
                assertEquals(0, player.count(recipe.primary), recipe.name)
                assertEquals(0, player.count(recipe.secondary), recipe.name)
                assertEquals(
                    (recipe.xpPerItem * 50 * 10).toInt(),
                    player.statMap.getFineXP(stats.fletching) - startXp,
                    recipe.name,
                )
                assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            }
        }

    @Test
    fun GameTestState.`javelin quantity menu caps ten sets and supports reverse held use`() =
        runAmmunitionTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.output == objs.dragon_javelin }
            add(recipe.primary, 200)
            add(recipe.secondary, 200)

            val primarySlot = player.inv.indexOfFirst { it?.id == recipe.primary.id }
            val secondarySlot = player.inv.indexOfFirst { it?.id == recipe.secondary.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.secondary],
                    secondarySlot,
                    objTypes[recipe.primary],
                    primarySlot,
                )
            }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(10, setup.values[1])
            assertEquals(recipe.output.id, setup.values[2])
            assertEquals(10, setup.values[12])
            assertEquals("How many sets of 15 would you like to make?", setup.values[13])
        }

    @Test
    fun GameTestState.`arrows make one set every two ticks and finish a partial set`() =
        runAmmunitionTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.output == objs.amethyst_arrow }
            add(recipe.primary, 18)
            add(recipe.secondary, 18)
            val startXp = player.statMap.getFineXP(stats.fletching)

            player.withProtectedAccess { deps.fletching.start(this, recipe, quantity = 2) }
            advance()
            assertEquals(0, player.count(recipe.output))
            advance()
            assertEquals(15, player.count(recipe.output))
            advance(2)
            assertEquals(18, player.count(recipe.output))
            assertEquals(0, player.count(recipe.primary))
            assertEquals(0, player.count(recipe.secondary))
            assertEquals(2430, player.statMap.getFineXP(stats.fletching) - startXp)
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
        }

    @Test
    fun GameTestState.`headless arrows use sets of fifteen and cap make x at ten sets`() =
        runAmmunitionTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.family == AmmunitionFamily.HeadlessArrow }
            add(recipe.primary, 200)
            add(recipe.secondary, 200)

            player.withProtectedAccess { deps.fletching.select(this, recipe) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(10, setup.values[1])
            assertEquals(objs.headless_arrow.id, setup.values[2])
            assertEquals(10, setup.values[12])
            assertEquals("How many sets of 15 would you like to make?", setup.values[13])
        }

    @Test
    fun GameTestState.`dart and bolt make x setting switches from instant to two tick batching`() =
        runAmmunitionTest { deps ->
            preparePlayer()
            val dart = deps.recipes.recipes.single { it.output == objs.rune_dart }
            add(dart.primary, 20)
            add(dart.secondary, 20)
            player.setVarBit(varbits.option_fletch_dart_or_bolt_makex, 1)

            player.withProtectedAccess { deps.fletching.select(this, dart) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            resumeSkillMulti(quantity = 2)
            advance()
            assertEquals(0, player.count(dart.output))
            advance()
            assertEquals(10, player.count(dart.output))
            advance(2)
            assertEquals(20, player.count(dart.output))

            preparePlayer()
            val bolt = deps.recipes.recipes.single { it.output == objs.mithril_bolts }
            add(bolt.primary, 10)
            add(bolt.secondary, 10)
            player.withProtectedAccess { deps.fletching.select(this, bolt) }
            assertEquals(10, player.count(bolt.output))
            assertTrue(client.filterIsInstance<IfOpenSub>().isEmpty())
        }

    @Test
    fun GameTestState.`membership level broader fletching and material failures are atomic`() =
        runAmmunitionTest { deps ->
            val dragon = deps.recipes.recipes.single { it.output == objs.dragon_dart }

            preparePlayer()
            player.members = false
            add(dragon.primary, 10)
            add(dragon.secondary, 10)
            player.withProtectedAccess { deps.fletching.start(this, dragon, 1) }
            assertMessageSent("You need to be on a members' world to train Fletching.")
            assertInputsRemain(dragon, 10)

            preparePlayer()
            player.stats[stats.fletching] = 94
            add(dragon.primary, 10)
            add(dragon.secondary, 10)
            player.withProtectedAccess { deps.fletching.start(this, dragon, 1) }
            assertMessageSent("You need a Fletching level of 95 to make dragon dart.")
            assertInputsRemain(dragon, 10)

            preparePlayer()
            val broad = deps.recipes.recipes.single { it.output == objs.slayer_broad_arrow }
            player.setVarBit(varbits.slayer_ammo_unlocked, 0)
            add(broad.primary, 15)
            add(broad.secondary, 15)
            player.withProtectedAccess { deps.fletching.start(this, broad, 1) }
            assertMessageSent("You need to unlock Broader Fletching from a Slayer master first.")
            assertInputsRemain(broad, 15)

            preparePlayer()
            add(dragon.primary, 10)
            player.withProtectedAccess { deps.fletching.start(this, dragon, 1) }
            assertMessageSent("You do not have the required feather.")
            assertEquals(10, player.count(dragon.primary))
            assertEquals(0, player.count(dragon.output))

            val dragonJavelin = deps.recipes.recipes.single { it.output == objs.dragon_javelin }
            preparePlayer()
            player.stats[stats.fletching] = 91
            add(dragonJavelin.primary, 15)
            add(dragonJavelin.secondary, 15)
            player.withProtectedAccess { deps.fletching.start(this, dragonJavelin, 1) }
            assertMessageSent("You need a Fletching level of 92 to make dragon javelin.")
            assertInputsRemain(dragonJavelin, 15)

            preparePlayer()
            add(dragonJavelin.primary, 15)
            player.withProtectedAccess { deps.fletching.start(this, dragonJavelin, 1) }
            assertMessageSent("You do not have the required javelin shaft.")
            assertEquals(15, player.count(dragonJavelin.primary))
            assertEquals(0, player.count(dragonJavelin.secondary))
            assertEquals(0, player.count(dragonJavelin.output))
        }

    @Test
    fun GameTestState.`held use works in reverse order and supports a partial direct set`() =
        runAmmunitionTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.output == objs.adamant_dart }
            add(recipe.primary, 4)
            add(recipe.secondary, 7)
            val primarySlot = player.inv.indexOfFirst { it?.id == recipe.primary.id }
            val secondarySlot = player.inv.indexOfFirst { it?.id == recipe.secondary.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.secondary],
                    secondarySlot,
                    objTypes[recipe.primary],
                    primarySlot,
                )
            }

            assertEquals(4, player.count(recipe.output))
            assertEquals(0, player.count(recipe.primary))
            assertEquals(3, player.count(recipe.secondary))
        }

    @Test
    fun GameTestState
        .`another fletching action and missing materials stop a queued batch safely`() =
        runAmmunitionTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.output == objs.rune_arrow }
            add(recipe.primary, 30)
            add(recipe.secondary, 30)
            player.withProtectedAccess {
                weakQueue(fletching_queues.logCutting, 20)
                deps.fletching.start(this, recipe, 2)
            }
            assertEquals(0, player.weakQueueList.count(fletching_queues.logCutting))
            advance(2)
            assertEquals(15, player.count(recipe.output))
            player.inv[player.inv.indexOfFirst { it?.id == recipe.secondary.id }] = null
            advance(2)
            assertEquals(15, player.count(recipe.output))
            assertEquals(15, player.count(recipe.primary))
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))

            preparePlayer()
            val javelin = deps.recipes.recipes.single { it.output == objs.rune_javelin }
            add(javelin.primary, 45)
            add(javelin.secondary, 45)
            player.withProtectedAccess { deps.fletching.start(this, javelin, 3) }
            advance()
            assertEquals(15, player.count(javelin.output))
            player.inv[player.inv.indexOfFirst { it?.id == javelin.secondary.id }] = null
            advance()
            assertEquals(15, player.count(javelin.output))
            assertEquals(30, player.count(javelin.primary))
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.withProtectedAccess { resetAnim() }
        client.clear()
        player.members = true
        player.stats[stats.fletching] = 99
        player.setVarBit(varbits.option_fletch_dart_or_bolt_makex, 0)
        player.setVarBit(varbits.slayer_ammo_unlocked, 1)
        player.setVarp(varps.big_chompy_bird_hunting, 1)
    }

    @Test
    fun GameTestState.`all tipped bolts use exact cache animation and complete after two ticks`() =
        runAmmunitionTest { deps ->
            val tipped = deps.recipes.recipes.filter { it.family == AmmunitionFamily.TippedBolt }
            assertEquals(22, tipped.size)
            for (recipe in tipped) {
                preparePlayer()
                add(recipe.primary, 10)
                add(recipe.secondary, 10)
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess { deps.fletching.start(this, recipe, quantity = 1) }

                assertEquals(
                    requireNotNull(recipe.sequence).id,
                    player.pendingSequence.id,
                    recipe.name,
                )
                assertEquals(0, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(0, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(10, player.count(recipe.output), recipe.name)
                assertEquals(0, player.count(recipe.primary), recipe.name)
                assertEquals(0, player.count(recipe.secondary), recipe.name)
                assertEquals(
                    (recipe.xpPerItem * 10 * 10).toInt(),
                    player.statMap.getFineXP(stats.fletching) - startXp,
                    recipe.name,
                )
                assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            }
        }

    @Test
    fun GameTestState.`tipped bolts default to one set and make x setting queues multiple sets`() =
        runAmmunitionTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.output == objs.dragon_onyx_bolts }
            add(recipe.primary, 25)
            add(recipe.secondary, 25)

            player.withProtectedAccess { deps.fletching.select(this, recipe) }

            assertTrue(client.filterIsInstance<IfOpenSub>().isEmpty())
            advance(2)
            assertEquals(10, player.count(recipe.output))
            assertEquals(15, player.count(recipe.primary))

            preparePlayer()
            add(recipe.primary, 25)
            add(recipe.secondary, 25)
            player.setVarBit(varbits.option_fletch_dart_or_bolt_makex, 1)
            player.withProtectedAccess { deps.fletching.select(this, recipe) }
            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(3, setup.values[1])
            assertEquals(recipe.output.id, setup.values[2])
            assertEquals(3, setup.values[12])
            assertEquals("How many sets of 10 would you like to make?", setup.values[13])
            resumeSkillMulti(quantity = 3)
            advance(2)
            assertEquals(10, player.count(recipe.output))
            advance(2)
            assertEquals(20, player.count(recipe.output))
            advance(2)
            assertEquals(25, player.count(recipe.output))
        }

    @Test
    fun GameTestState
        .`tipped bolt requirements partial sets full inventory and interruption are safe`() =
        runAmmunitionTest { deps ->
            val amethyst = deps.recipes.recipes.single { it.output == objs.amethyst_broad_bolts }

            preparePlayer()
            player.setVarBit(varbits.slayer_ammo_unlocked, 0)
            add(amethyst.primary, 10)
            add(amethyst.secondary, 10)
            player.withProtectedAccess { deps.fletching.start(this, amethyst, 1) }
            assertMessageSent("You need to unlock Broader Fletching from a Slayer master first.")
            assertInputsRemain(amethyst, 10)

            preparePlayer()
            val dragon = deps.recipes.recipes.single { it.output == objs.dragon_opal_bolts }
            player.stats[stats.fletching] = 83
            add(dragon.primary, 10)
            add(dragon.secondary, 10)
            player.withProtectedAccess { deps.fletching.start(this, dragon, 1) }
            assertMessageSent("You need a Fletching level of 84 to make opal dragon bolts.")
            assertInputsRemain(dragon, 10)

            preparePlayer()
            val opal = deps.recipes.recipes.single { it.output == objs.opal_bolts }
            add(opal.primary, 7)
            add(opal.secondary, 9)
            repeat(26) { add(objs.logs) }
            player.withProtectedAccess { deps.fletching.start(this, opal, 1) }
            advance(2)
            assertEquals(7, player.count(opal.output))
            assertEquals(0, player.count(opal.primary))
            assertEquals(2, player.count(opal.secondary))

            preparePlayer()
            val ruby = deps.recipes.recipes.single { it.output == objs.ruby_bolts }
            add(ruby.primary, 20)
            add(ruby.secondary, 20)
            player.setVarBit(varbits.option_fletch_dart_or_bolt_makex, 1)
            player.withProtectedAccess { deps.fletching.start(this, ruby, 2) }
            advance(2)
            assertEquals(10, player.count(ruby.output))
            player.inv[player.inv.indexOfFirst { it?.id == ruby.secondary.id }] = null
            advance(2)
            assertEquals(10, player.count(ruby.output))
            assertEquals(10, player.count(ruby.primary))
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
        }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].isStackable) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj, count)
            return
        }
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestScope.assertInputsRemain(recipe: AmmunitionFletchingRecipe, count: Int) {
        assertEquals(count, player.count(recipe.primary))
        assertEquals(count, player.count(recipe.secondary))
        assertEquals(0, player.count(recipe.output))
    }

    private fun GameTestScope.resumeSkillMulti(quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.skillmulti_a, subcomponent = quantity)
        )
    }

    private fun GameTestState.runAmmunitionTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            AmmunitionFletchingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(AmmunitionFletchingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val fletching: AmmunitionFletching,
        val recipes: AmmunitionFletchingRecipeRegistry,
        val heldU: HeldUInteractions,
        val dbRows: DbRowTypeList,
        val dbTables: DbTableTypeList,
    )
}
