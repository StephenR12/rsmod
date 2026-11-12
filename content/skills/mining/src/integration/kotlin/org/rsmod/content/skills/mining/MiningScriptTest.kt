package org.rsmod.content.skills.mining

import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.front
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.gemPreRollDenominator
import org.rsmod.content.skills.mining.scripts.Mining.Companion.successBounds
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class MiningScriptTest {
    @Test
    fun GameTestState.`requires a usable pickaxe`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.copper1)
            player.placeAt(rock.coords.translateX(-1))
            player.stats[stats.mining] = 1

            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessagesSent(
                "You need a pickaxe to mine this rock.",
                "You do not have a pickaxe which you have the Mining level to use.",
            )

            player.inv[0] = InvObj(MiningObjs.runePickaxe)
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessagesSent(
                "You need a pickaxe to mine this rock.",
                "You do not have a pickaxe which you have the Mining level to use.",
            )

            player.inv[1] = InvObj(MiningObjs.bronzePickaxe)
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You swing your pick at the rock.")
        }

    @Test
    fun GameTestState.`enforces rock level before starting`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.runite1)
            player.placeAt(rock.coords.translateX(-1))
            player.righthand = InvObj(MiningObjs.runePickaxe)
            player.stats[stats.mining] = 84

            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You need a Mining level of 85 to mine this rock.")
            assertDoesNotContain(player.inv, objs.runite_ore)
            assertExists(rock)
        }

    @Test
    fun GameTestState.`successful roll awards ore and xp then respawns rock`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.copper1)
            player.placeAt(rock.coords.translateX(-1))
            player.righthand = InvObj(MiningObjs.bronzePickaxe)
            player.stats[stats.mining] = 1
            val startXp = player.statMap.getFineXP(stats.mining)

            random.next = 128
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You swing your pick at the rock.")

            random.next = 0
            advance(ticks = 8)
            assertMessageSent("You manage to mine some copper ore.")
            assertContains(player.inv, objs.copper_ore)
            assertEquals(175, player.statMap.getFineXP(stats.mining) - startXp)
            assertDoesNotExist(rock)
            assertExists(rock.coords, MiningLocs.empty1)

            advance(ticks = 3)
            assertExists(rock)
            assertDoesNotExist(rock.coords, MiningLocs.empty1)
        }

    @Test
    fun GameTestState.`full inventory blocks mining`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.iron1)
            player.placeAt(rock.coords.translateX(-1))
            player.righthand = InvObj(MiningObjs.runePickaxe)
            player.stats[stats.mining] = 99
            player.fillInv()

            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("Your inventory is too full to hold any more iron ore.")
            assertDoesNotContain(player.inv, objs.iron_ore)
            assertExists(rock)
        }

    @Test
    fun GameTestState.`open coal bag receives mined coal even when inventory is full`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.coal1)
            player.placeAt(rock.coords.translateX(-1))
            player.members = true
            player.righthand = InvObj(MiningObjs.runePickaxe)
            player.stats[stats.mining] = 99
            player.fillInv()
            player.inv[0] = InvObj(objs.coal_bag_open)
            player.setVarBit(varbits.coal_bag_storage_count, 0)

            random.next = 32_767
            random.then = 0
            player.opLoc1(rock)
            advance(ticks = 4)

            assertEquals(1, player.vars[varbits.coal_bag_storage_count])
            assertDoesNotContain(player.inv, objs.coal)
            assertMessageSent("You manage to mine some coal.")
            assertDoesNotExist(rock)
        }

    @Test
    fun GameTestState.`gem rock success awards weighted gem xp sound and exact depletion`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.gemRock1)
            player.placeAt(rock.coords.translateX(-1))
            player.members = true
            player.righthand = InvObj(MiningObjs.adamantPickaxe)
            player.stats[stats.mining] = 40
            val startXp = player.statMap.getFineXP(stats.mining)

            random.next = 0
            random.then = 127
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You swing your pick at the rock.")

            advance(ticks = 4)
            assertMessageSent("You just mined a Diamond!")
            assertContains(player.inv, objs.uncut_diamond)
            assertEquals(650, player.statMap.getFineXP(stats.mining) - startXp)
            assertEquals(2655, client.filterIsInstance<SynthSound>().single().id)
            assertDoesNotExist(rock)
            assertExists(rock.coords, MiningLocs.empty1)

            // The successful mining tick is the first tick of the 99-tick loc change.
            advance(ticks = 97)
            assertDoesNotExist(rock)
            advance(ticks = 1)
            assertExists(rock)
        }

    @Test
    fun GameTestState.`gem rocks enforce level 40 before starting`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.gemRock2)
            player.placeAt(rock.coords.translateX(-1))
            player.members = true
            player.righthand = InvObj(MiningObjs.adamantPickaxe)
            player.stats[stats.mining] = 39

            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You need a Mining level of 40 to mine this rock.")
            assertExists(rock)
        }

    @Test
    fun GameTestState.`charged glory variants triple only gem rock success endpoints`() =
        runGameTest {
            val gemRock = locTypes[MiningLocs.gemRock2]
            val copperRock = locTypes[MiningLocs.copper1]
            val chargedGlories =
                listOf(
                    objs.amulet_of_glory_1,
                    objs.amulet_of_glory_2,
                    objs.amulet_of_glory_3,
                    objs.amulet_of_glory_4,
                    objs.amulet_of_glory_5,
                    objs.amulet_of_glory_6,
                    objs.amulet_of_glory_t_1,
                    objs.amulet_of_glory_t_2,
                    objs.amulet_of_glory_t_3,
                    objs.amulet_of_glory_t_4,
                    objs.amulet_of_glory_t_5,
                    objs.amulet_of_glory_t_6,
                    objs.amulet_of_eternal_glory,
                )

            assertEquals(28..70, successBounds(player, gemRock))
            assertEquals(32_768, gemPreRollDenominator(player))
            player.front = InvObj(objs.amulet_of_glory_uncharged)
            assertEquals(28..70, successBounds(player, gemRock))
            assertEquals(32_768, gemPreRollDenominator(player))
            player.front = InvObj(objs.amulet_of_glory_t_uncharged)
            assertEquals(28..70, successBounds(player, gemRock))
            assertEquals(32_768, gemPreRollDenominator(player))
            for (glory in chargedGlories) {
                player.front = InvObj(glory)
                assertEquals(84..210, successBounds(player, gemRock), glory.toString())
                assertEquals(100..350, successBounds(player, copperRock), glory.toString())
                assertEquals(8_216, gemPreRollDenominator(player), glory.toString())
            }
        }

    @Test
    fun GameTestState.`ordinary rock gem pre-roll replaces ore xp and depletion`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.copper1)
            player.placeAt(rock.coords.translateX(-1))
            player.righthand = InvObj(MiningObjs.runePickaxe)
            player.stats[stats.mining] = 99
            val startXp = player.statMap.getFineXP(stats.mining)

            random.next = 127
            random.then = 127
            player.opLoc1(rock)
            advance(ticks = 1)
            advance(ticks = 3)

            assertMessageSent("You just found a diamond!")
            assertContains(player.inv, objs.uncut_diamond)
            assertDoesNotContain(player.inv, objs.copper_ore)
            assertEquals(startXp, player.statMap.getFineXP(stats.mining))
            assertEquals(2655, client.filterIsInstance<SynthSound>().single().id)
            assertExists(rock)
        }

    @Test
    fun GameTestState.`ordinary rock gem pre-table nothing replaces ore silently`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.copper1)
            player.placeAt(rock.coords.translateX(-1))
            player.righthand = InvObj(MiningObjs.runePickaxe)
            player.stats[stats.mining] = 99
            val startXp = player.statMap.getFineXP(stats.mining)

            random.next = 0
            random.then = 69
            player.opLoc1(rock)
            advance(ticks = 1)
            advance(ticks = 3)

            assertDoesNotContain(player.inv, objs.uncut_sapphire)
            assertDoesNotContain(player.inv, objs.copper_ore)
            assertEquals(startXp, player.statMap.getFineXP(stats.mining))
            assertEquals(0, client.filterIsInstance<SynthSound>().size)
            assertExists(rock)
        }

    @Test
    fun GameTestState.`open gem bag receives ordinary and gem-rock precious rewards`() =
        runGameTest(Mining::class) {
            player.members = true
            player.inv[0] = InvObj(objs.gem_bag_open)
            player.righthand = InvObj(MiningObjs.runePickaxe)
            player.stats[stats.mining] = 99
            val storage = player.invMap.getOrPut(cacheTypes.invs[invs.gem_bag_storage])

            val copper = placeMapLoc(TEST_COORDS, MiningLocs.copper1)
            player.placeAt(copper.coords.translateX(-1))
            random.next = 127
            random.then = 127
            player.opLoc1(copper)
            advance(ticks = 4)
            assertEquals(1, storage.count(objTypes[objs.uncut_diamond]))
            assertDoesNotContain(player.inv, objs.uncut_diamond)
            assertMessageSent("You just found a diamond!")
            assertMessageSent("You put it straight into your open gem bag.")
            assertExists(copper)

            player.placeAt(TEST_COORDS.translate(0, 4))
            val gemRock = placeMapLoc(TEST_COORDS.translate(0, 5), MiningLocs.gemRock1)
            random.next = 0
            random.then = 127
            player.opLoc1(gemRock)
            advance(ticks = 4)
            assertEquals(2, storage.count(objTypes[objs.uncut_diamond]))
            assertMessageSent("You just mined a Diamond!")
            assertMessageSent("You put it straight into your open gem bag.")
            assertDoesNotExist(gemRock)
        }

    @Test
    fun GameTestState.`full inventory blocks gem rock`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.gemRock2)
            player.placeAt(rock.coords.translateX(-1))
            player.members = true
            player.righthand = InvObj(MiningObjs.runePickaxe)
            player.stats[stats.mining] = 99
            player.fillInv()

            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("Your inventory is too full to hold any more gems.")
            assertExists(rock)
        }

    @Test
    fun GameTestState.`special pickaxes enforce members and crystal quest access`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.copper1)
            player.placeAt(rock.coords.translateX(-1))
            player.stats[stats.mining] = 99

            player.members = false
            player.inv[0] = InvObj(MiningObjs.dragonPickaxe)
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessagesSent(
                "You need a pickaxe to mine this rock.",
                "You do not have a pickaxe which you have the Mining level to use.",
            )

            player.members = true
            player.inv[0] = InvObj(MiningObjs.crystalPickaxe)
            player.setVarBit(varbits.song_of_the_elves, 193)
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessagesSent(
                "You need a pickaxe to mine this rock.",
                "You do not have a pickaxe which you have the Mining level to use.",
            )

            player.setVarBit(varbits.song_of_the_elves, 194)
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You swing your pick at the rock.")
        }

    @Test
    fun GameTestState.`dragon family rolls at two or three tick intervals`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.copper1)
            player.placeAt(rock.coords.translateX(-1))
            player.members = true
            player.stats[stats.mining] = 99
            player.inv[0] = InvObj(MiningObjs.dragonPickaxe)

            random.next = 0
            random.then = 128
            random.then = 0
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You swing your pick at the rock.")
            advance(ticks = 2)
            assertContains(player.inv, objs.copper_ore)
        }

    @Test
    fun GameTestState.`crystal outranks dragon when both are carried`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.copper1)
            player.placeAt(rock.coords.translateX(-1))
            player.members = true
            player.stats[stats.mining] = 99
            player.setVarBit(varbits.song_of_the_elves, 194)
            player.inv[0] = InvObj(MiningObjs.dragonPickaxe)
            player.inv[1] = InvObj(MiningObjs.crystalPickaxe)

            assertEquals(MiningObjs.crystalPickaxe.id, Mining.findPickaxe(player, objTypes)?.id)
            random.next = 1
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You swing your pick at the rock.")
        }

    companion object {
        private val TEST_COORDS = CoordGrid(0, 50, 50, 34, 31)
    }
}
