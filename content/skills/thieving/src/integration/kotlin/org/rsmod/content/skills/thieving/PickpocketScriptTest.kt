package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.clue.Cache233ClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.back
import org.rsmod.api.player.feet
import org.rsmod.api.player.front
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.magic.ShadowVeilStateRepository
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.torso
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

class PickpocketScriptTest {
    @Test
    fun GameTestState.`success awards one citizen pouch and eight xp on a two-tick cadence`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnCitizen(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0

            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(ThievingObjs.citizenPouch))
            assertEquals(80, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the man's pocket.")
            assertEquals(4398, client.filterIsInstance<SynthSound>().single().id)

            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(ThievingObjs.citizenPouch))

            player.opNpc3(npc)
            advance()
            assertEquals(2, player.count(ThievingObjs.citizenPouch))
        }

    @Test
    fun GameTestState.`ordinary failure deals one damage and blocks pickpocketing and movement`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnCitizen(deps)
            val origin = player.coords
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.man)
            }
            assertMessageSent("You fail to pick the man's pocket.")
            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(8, player.actionDelay - player.currentMapClock)
            assertEquals(9, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(9, player.stat(stats.hitpoints))
            assertEquals(startXp, player.statMap.getFineXP(stats.thieving))
            assertEquals(0, player.count(ThievingObjs.citizenPouch))

            player.moveGameClick(origin.translateX(2))
            advance()
            assertEquals(origin, player.coords)
        }

    @Test
    fun GameTestState.`Shadow Veil protects before shout damage and stun`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnCitizen(deps)
            deps.shadowVeil.activate(player, duration = 10)
            random.next = 99
            random.then = 0

            player.opNpc3(npc)
            advance()

            assertMessagesSent(
                "You fail to pick the man's pocket.",
                "Your attempt to steal goes unnoticed.",
            )
            assertEquals(10, player.stat(stats.hitpoints))
            assertEquals(0, deps.stuns.remainingCycles(player))
            assertTrue(player.pendingSpotanims.isEmpty)
            assertEquals(0, player.count(ThievingObjs.citizenPouch))
        }

    @Test
    fun GameTestState.`dodgy necklace rolls only after Shadow Veil and consumes a charge`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnCitizen(deps)
            player.front = InvObj(objs.dodgy_necklace)
            random.next = 99
            random.then = 0

            player.opNpc3(npc)
            advance()

            assertMessageSent("Your dodgy necklace protects you.")
            assertEquals(10, player.stat(stats.hitpoints))
            assertEquals(9, player.front?.vars)
            assertEquals(0, deps.stuns.remainingCycles(player))
        }

    @Test
    fun GameTestState.`pouch cap follows Ardougne diary tiers and blocks the next attempt`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(28, pouchLimit(deps))
            player.setVarBit(varbits.ardougne_diary_medium_complete, 1)
            assertEquals(56, pouchLimit(deps))
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(84, pouchLimit(deps))
            player.setVarBit(varbits.ardougne_diary_elite_complete, 1)
            assertEquals(140, pouchLimit(deps))

            player.setVarBit(varbits.ardougne_diary_medium_complete, 0)
            player.setVarBit(varbits.ardougne_diary_hard_complete, 0)
            player.setVarBit(varbits.ardougne_diary_elite_complete, 0)
            player.inv[0] = InvObj(ThievingObjs.citizenPouch, 28)
            val npc = spawnCitizen(deps)
            random.next = 0
            player.opNpc3(npc)
            advance()
            assertMessageSent(
                "You need to open your coin pouches before you can continue pickpocketing."
            )
            assertEquals(28, player.count(ThievingObjs.citizenPouch))
        }

    @Test
    fun GameTestState.`open and open-all exchange pouches atomically for three coins each`() =
        runThievingTest { deps ->
            prepare()
            player.inv[4] = InvObj(ThievingObjs.citizenPouch, 4)

            player.withProtectedAccess { deps.pouches.openCitizen(this, 4, all = false) }
            assertEquals(3, player.count(ThievingObjs.citizenPouch))
            assertEquals(3, player.count(objs.coins))

            player.withProtectedAccess { deps.pouches.openCitizen(this, 4, all = true) }
            assertEquals(0, player.count(ThievingObjs.citizenPouch))
            assertEquals(12, player.count(objs.coins))
            assertEquals(listOf(2114, 2114), client.filterIsInstance<SynthSound>().map { it.id })
        }

    @Test
    fun GameTestState.`chance modifiers match the cutoff success chart`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(180..240, successBounds(deps))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(189..252, successBounds(deps))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(198..264, successBounds(deps))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(217..290, successBounds(deps))
        }

    @Test
    fun GameTestState.`farmer chance modifiers match the cutoff success chart`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(150..240, successBounds(deps, StandardPickpockets.farmer))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(157..252, successBounds(deps, StandardPickpockets.farmer))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(165..264, successBounds(deps, StandardPickpockets.farmer))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(181..290, successBounds(deps, StandardPickpockets.farmer))
        }

    @Test
    fun GameTestState.`non-members cannot train Thieving`() = runThievingTest { deps ->
        prepare()
        player.members = false
        val npc = spawnCitizen(deps)
        random.next = 0
        player.opNpc3(npc)
        advance()
        assertMessageSent("You need to be on a members' world to train Thieving.")
        assertEquals(0, player.count(ThievingObjs.citizenPouch))
    }

    @Test
    fun GameTestState.`farmer enforces level and awards weighted pouch or potato seed`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnFarmer(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 10 to pick this pocket.")

            player.stats[stats.thieving] = 10
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(ThievingObjs.farmerPouch))
            assertEquals(145, player.statMap.getFineXP(stats.thieving) - startXp)

            advance()
            random.next = 0
            random.then = 123
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(ThievingObjs.potatoSeed))
            assertEquals(290, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`farmer pouch opens for nine coins per pouch`() = runThievingTest { deps ->
        prepare()
        player.inv[4] = InvObj(ThievingObjs.farmerPouch, 4)

        player.withProtectedAccess {
            deps.pouches.open(this, 4, CoinPouchDefinitions.farmer, all = false)
        }
        assertEquals(3, player.count(ThievingObjs.farmerPouch))
        assertEquals(9, player.count(objs.coins))

        player.withProtectedAccess {
            deps.pouches.open(this, 4, CoinPouchDefinitions.farmer, all = true)
        }
        assertEquals(0, player.count(ThievingObjs.farmerPouch))
        assertEquals(36, player.count(objs.coins))
    }

    @Test
    fun GameTestState.`warrior enforces level and awards pouch with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnWarrior(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 25 to pick this pocket.")

            player.stats[stats.thieving] = 25
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.warriorPouch))
            assertEquals(260, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the warrior's pocket.")
        }

    @Test
    fun GameTestState.`warrior failure deals two damage with shared stun boundaries`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 25
            val npc = spawnWarrior(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.warrior)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(8, player.actionDelay - player.currentMapClock)
            assertEquals(9, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(8, player.stat(stats.hitpoints))
            assertEquals(0, player.count(ThievingObjs.warriorPouch))
        }

    @Test
    fun GameTestState.`warrior modifiers and eighteen-coin pouch match cutoff`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(100..240, successBounds(deps, StandardPickpockets.warrior))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(105..252, successBounds(deps, StandardPickpockets.warrior))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(110..264, successBounds(deps, StandardPickpockets.warrior))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(121..290, successBounds(deps, StandardPickpockets.warrior))

            player.inv[4] = InvObj(ThievingObjs.warriorPouch, 4)
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.warrior, all = false)
            }
            assertEquals(3, player.count(ThievingObjs.warriorPouch))
            assertEquals(18, player.count(objs.coins))
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.warrior, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.warriorPouch))
            assertEquals(72, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`rogue enforces level and awards weighted loot with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnRogue(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 32 to pick this pocket.")

            player.stats[stats.thieving] = 32
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 123
            player.opNpc3(npc)
            advance()

            assertEquals(8, player.count(ThievingObjs.airRune))
            assertEquals(365, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the rogue's pocket.")
        }

    @Test
    fun GameTestState.`rogue failure deals two damage with shared stun boundaries`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 32
            val npc = spawnRogue(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.rogue)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(8, player.actionDelay - player.currentMapClock)
            assertEquals(9, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(8, player.stat(stats.hitpoints))
            assertEquals(0, player.count(ThievingObjs.roguePouch))
        }

    @Test
    fun GameTestState.`rogue modifiers and variable coin pouch match cutoff`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(75..240, successBounds(deps, StandardPickpockets.rogue))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(78..252, successBounds(deps, StandardPickpockets.rogue))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(82..264, successBounds(deps, StandardPickpockets.rogue))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(90..290, successBounds(deps, StandardPickpockets.rogue))

            player.inv[4] = InvObj(ThievingObjs.roguePouch, 2)
            random.next = 25
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.rogue, all = false)
            }
            assertEquals(1, player.count(ThievingObjs.roguePouch))
            assertEquals(25, player.count(objs.coins))
            random.next = 40
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.rogue, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.roguePouch))
            assertEquals(65, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`full rogue set doubles variable pouch into one pouch and rolled coins`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 32
            val npc = spawnRogue(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0
            random.then = 25

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.roguePouch))
            assertEquals(25, player.count(objs.coins))
            assertEquals(365, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`guard enforces level and awards pouch with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnGuard(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 40 to pick this pocket.")

            player.stats[stats.thieving] = 40
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.guardPouch))
            assertEquals(468, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the guard's pocket.")
        }

    @Test
    fun GameTestState.`guard failure deals two damage with shared stun boundaries`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 40
            val npc = spawnGuard(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.guard)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(8, player.actionDelay - player.currentMapClock)
            assertEquals(9, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(8, player.stat(stats.hitpoints))
            assertEquals(0, player.count(ThievingObjs.guardPouch))
        }

    @Test
    fun GameTestState.`guard modifiers and thirty-coin pouch match cutoff`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(50..240, successBounds(deps, StandardPickpockets.guard))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(52..252, successBounds(deps, StandardPickpockets.guard))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(55..264, successBounds(deps, StandardPickpockets.guard))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(60..290, successBounds(deps, StandardPickpockets.guard))

            player.inv[4] = InvObj(ThievingObjs.guardPouch, 4)
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.guard, all = false)
            }
            assertEquals(3, player.count(ThievingObjs.guardPouch))
            assertEquals(30, player.count(objs.coins))
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.guard, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.guardPouch))
            assertEquals(120, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`Ardougne knight enforces level and awards pouch with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnArdougneKnight(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 55 to pick this pocket.")

            player.stats[stats.thieving] = 55
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.knightPouch))
            assertEquals(843, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the knight's pocket.")
        }

    @Test
    fun GameTestState.`knight failure deals three damage with six second stun boundaries`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 55
            val npc = spawnVarlamoreKnight(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.knightOfVarlamore)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(10, player.actionDelay - player.currentMapClock)
            assertEquals(11, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(7, player.stat(stats.hitpoints))
            assertEquals(0, player.count(ThievingObjs.knightPouch))
        }

    @Test
    fun GameTestState.`medium diary applies to Ardougne knight only and pouch opens for fifty`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(50..240, successBounds(deps, StandardPickpockets.knightOfArdougne))
            assertEquals(50..240, successBounds(deps, StandardPickpockets.knightOfVarlamore))

            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(52..252, successBounds(deps, StandardPickpockets.knightOfArdougne))
            player.setVarBit(varbits.ardougne_diary_medium_complete, 1)
            assertEquals(55..264, successBounds(deps, StandardPickpockets.knightOfArdougne))
            assertEquals(52..252, successBounds(deps, StandardPickpockets.knightOfVarlamore))

            player.hands = null
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(55..264, successBounds(deps, StandardPickpockets.knightOfVarlamore))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(60..290, successBounds(deps, StandardPickpockets.knightOfVarlamore))

            player.inv[4] = InvObj(ThievingObjs.knightPouch, 4)
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.knight, all = false)
            }
            assertEquals(3, player.count(ThievingObjs.knightPouch))
            assertEquals(50, player.count(objs.coins))
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.knight, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.knightPouch))
            assertEquals(200, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`watchman enforces level and awards pouch bread and exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnWatchman(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 65 to pick this pocket.")

            player.stats[stats.thieving] = 65
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.watchmanPouch))
            assertEquals(1, player.count(ThievingObjs.bread))
            assertEquals(1375, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the watchman's pocket.")
        }

    @Test
    fun GameTestState.`watchman failure modifiers and sixty coin pouch match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 65
            val npc = spawnWatchman(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.watchman)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(8, player.actionDelay - player.currentMapClock)
            assertEquals(9, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(7, player.stat(stats.hitpoints))

            assertEquals(15..160, successBounds(deps, StandardPickpockets.watchman))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(15..168, successBounds(deps, StandardPickpockets.watchman))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(16..176, successBounds(deps, StandardPickpockets.watchman))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(17..193, successBounds(deps, StandardPickpockets.watchman))

            player.inv[4] = InvObj(ThievingObjs.watchmanPouch, 2)
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.watchman, all = false)
            }
            assertEquals(1, player.count(ThievingObjs.watchmanPouch))
            assertEquals(60, player.count(objs.coins))
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.watchman, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.watchmanPouch))
            assertEquals(120, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`rogue set doubles the complete watchman bundle without doubling xp`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 65
            val npc = spawnWatchman(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.watchmanPouch))
            assertEquals(60, player.count(objs.coins))
            assertEquals(2, player.count(ThievingObjs.bread))
            assertEquals(1375, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`watchman bundle rolls back atomically when bread cannot fit`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 65
            player.inv[0] = InvObj(ThievingObjs.watchmanPouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            val npc = spawnWatchman(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0

            player.opNpc3(npc)
            advance()

            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(ThievingObjs.watchmanPouch))
            assertEquals(0, player.count(ThievingObjs.bread))
            assertEquals(startXp, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState.`post quest menaphite enforces level and awards pouch with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnMenaphiteThug(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 65 to pick this pocket.")

            player.stats[stats.thieving] = 65
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.menaphitePouch))
            assertEquals(1375, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the Menaphite's pocket.")
        }

    @Test
    fun GameTestState.`menaphite failure modifiers and sixty coin pouch match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 65
            val npc = spawnMenaphiteThug(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.menaphiteThug)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(8, player.actionDelay - player.currentMapClock)
            assertEquals(9, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(5, player.stat(stats.hitpoints))

            assertEquals(50..160, successBounds(deps, StandardPickpockets.menaphiteThug))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(52..168, successBounds(deps, StandardPickpockets.menaphiteThug))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(55..176, successBounds(deps, StandardPickpockets.menaphiteThug))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(60..193, successBounds(deps, StandardPickpockets.menaphiteThug))

            player.inv[4] = InvObj(ThievingObjs.menaphitePouch, 2)
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.menaphite, all = false)
            }
            assertEquals(1, player.count(ThievingObjs.menaphitePouch))
            assertEquals(60, player.count(objs.coins))
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.menaphite, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.menaphitePouch))
            assertEquals(120, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`rogue set doubles menaphite coins without doubling xp`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 65
            val npc = spawnMenaphiteThug(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.menaphitePouch))
            assertEquals(60, player.count(objs.coins))
            assertEquals(1375, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`menaphite doubled pouch transaction rolls back without coin space`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 65
            player.inv[0] = InvObj(ThievingObjs.menaphitePouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            val npc = spawnMenaphiteThug(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0

            player.opNpc3(npc)
            advance()

            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(ThievingObjs.menaphitePouch))
            assertEquals(0, player.count(objs.coins))
            assertEquals(startXp, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState.`paladin enforces level and awards pouch runes and exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnPaladin(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 70 to pick this pocket.")

            player.stats[stats.thieving] = 70
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.paladinPouch))
            assertEquals(2, player.count(ThievingObjs.chaosRune))
            assertEquals(1318, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the paladin's pocket.")
        }

    @Test
    fun GameTestState.`paladin failure modifiers and eighty coin pouch match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 70
            val npc = spawnPaladin(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.paladin)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(8, player.actionDelay - player.currentMapClock)
            assertEquals(9, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(7, player.stat(stats.hitpoints))

            assertEquals(40..170, successBounds(deps, StandardPickpockets.paladin))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(42..178, successBounds(deps, StandardPickpockets.paladin))
            player.setVarBit(varbits.ardougne_diary_medium_complete, 1)
            assertEquals(44..187, successBounds(deps, StandardPickpockets.paladin))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(48..205, successBounds(deps, StandardPickpockets.paladin))

            player.inv[4] = InvObj(ThievingObjs.paladinPouch, 2)
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.paladin, all = false)
            }
            assertEquals(1, player.count(ThievingObjs.paladinPouch))
            assertEquals(80, player.count(objs.coins))
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.paladin, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.paladinPouch))
            assertEquals(160, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`rogue set doubles the guaranteed paladin bundle without doubling xp`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 70
            val npc = spawnPaladin(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.paladinPouch))
            assertEquals(80, player.count(objs.coins))
            assertEquals(4, player.count(ThievingObjs.chaosRune))
            assertEquals(1318, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`paladin bundle rolls back atomically when runes cannot fit`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 70
            player.inv[0] = InvObj(ThievingObjs.paladinPouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            val npc = spawnPaladin(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0

            player.opNpc3(npc)
            advance()

            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(ThievingObjs.paladinPouch))
            assertEquals(0, player.count(ThievingObjs.chaosRune))
            assertEquals(startXp, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState.`desert bandit enforces level and awards pouch with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnDesertBandit(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 53 to pick this pocket.")

            player.stats[stats.thieving] = 53
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.desertBanditPouch))
            assertEquals(794, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the bandit's pocket.")
        }

    @Test
    fun GameTestState.`desert bandit weighted antipoison and lockpick rewards match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 53
            val npc = spawnDesertBandit(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)

            random.next = 0
            random.then = 5
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(ThievingObjs.antipoison1))
            assertEquals(0, player.count(ThievingObjs.desertBanditPouch))

            advance()
            random.next = 0
            random.then = 6
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(ThievingObjs.lockpick))
            assertEquals(1588, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`desert bandit failure modifiers and thirty coin pouch match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 53
            val npc = spawnDesertBandit(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.desertBandit)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(8, player.actionDelay - player.currentMapClock)
            assertEquals(9, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(7, player.stat(stats.hitpoints))

            assertEquals(50..240, successBounds(deps, StandardPickpockets.desertBandit))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(52..252, successBounds(deps, StandardPickpockets.desertBandit))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(55..264, successBounds(deps, StandardPickpockets.desertBandit))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(60..290, successBounds(deps, StandardPickpockets.desertBandit))

            player.inv[4] = InvObj(ThievingObjs.desertBanditPouch, 2)
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.desertBandit, all = false)
            }
            assertEquals(1, player.count(ThievingObjs.desertBanditPouch))
            assertEquals(30, player.count(objs.coins))
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.desertBandit, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.desertBanditPouch))
            assertEquals(60, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`rogue set doubles both desert bandit reward forms without doubling xp`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 53
            val npc = spawnDesertBandit(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)

            random.next = 0
            random.then = 0
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(ThievingObjs.desertBanditPouch))
            assertEquals(30, player.count(objs.coins))

            advance()
            random.next = 0
            random.then = 5
            player.opNpc3(npc)
            advance()
            assertEquals(2, player.count(ThievingObjs.antipoison1))
            assertEquals(1588, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`desert bandit weighted reward rolls back when selected item cannot fit`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 53
            player.inv[0] = InvObj(ThievingObjs.desertBanditPouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            val npc = spawnDesertBandit(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 5

            player.opNpc3(npc)
            advance()

            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(ThievingObjs.desertBanditPouch))
            assertEquals(0, player.count(ThievingObjs.antipoison1))
            assertEquals(startXp, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState.`hero enforces level and awards pouch with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnHero(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 80 to pick this pocket.")

            player.stats[stats.thieving] = 80
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.heroPouch))
            assertEquals(1633, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the hero's pocket.")
        }

    @Test
    fun GameTestState.`hero weighted non-pouch reward branches match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 80
            val npc = spawnHero(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            val rewards =
                listOf(
                    Triple(105, ThievingObjs.deathRune, 2),
                    Triple(113, ThievingObjs.jugWine, 1),
                    Triple(119, ThievingObjs.bloodRune, 1),
                    Triple(124, ThievingObjs.diamond, 1),
                    Triple(125, ThievingObjs.fireOrb, 1),
                    Triple(127, ThievingObjs.goldOre, 1),
                )

            for ((index, reward) in rewards.withIndex()) {
                random.next = 0
                random.then = reward.first
                player.opNpc3(npc)
                advance()
                assertEquals(
                    reward.third,
                    player.count(reward.second),
                    "reward=${reward.second.id}",
                )
                if (index != rewards.lastIndex) {
                    advance()
                }
            }
            assertEquals(9798, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`hero failure and six-second stun contract match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 80
            val npc = spawnHero(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.hero)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(10, player.actionDelay - player.currentMapClock)
            assertEquals(11, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(7, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState.`hero modifiers and variable pouch opening match cutoff`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(39..160, successBounds(deps, StandardPickpockets.hero))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(40..168, successBounds(deps, StandardPickpockets.hero))
            player.setVarBit(varbits.ardougne_diary_medium_complete, 1)
            assertEquals(42..176, successBounds(deps, StandardPickpockets.hero))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(46..193, successBounds(deps, StandardPickpockets.hero))

            player.inv[4] = InvObj(ThievingObjs.heroPouch, 2)
            random.next = 200
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.hero, all = false)
            }
            assertEquals(1, player.count(ThievingObjs.heroPouch))
            assertEquals(200, player.count(objs.coins))
            random.next = 300
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.hero, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.heroPouch))
            assertEquals(500, player.count(objs.coins))
        }

    @Test
    fun GameTestState
        .`rogue set gives hero pouch plus fixed two hundred coins without doubling xp`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 80
            val npc = spawnHero(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.heroPouch))
            assertEquals(200, player.count(objs.coins))
            assertEquals(1633, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`rogue set doubles hero non-pouch rewards and capacity rollback is atomic`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 80
            val npc = spawnHero(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 105

            player.opNpc3(npc)
            advance()
            assertEquals(4, player.count(ThievingObjs.deathRune))
            assertEquals(1633, player.statMap.getFineXP(stats.thieving) - startXp)

            for (slot in 0 until player.inv.size) {
                player.inv[slot] = null
            }
            player.inv[0] = InvObj(ThievingObjs.heroPouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            advance()
            random.next = 0
            random.then = 124
            player.opNpc3(npc)
            advance()

            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(ThievingObjs.heroPouch))
            assertEquals(0, player.count(ThievingObjs.diamond))
            assertEquals(1633, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`gnome enforces level and awards three hundred coin pouch with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnGnome(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 75 to pick this pocket.")

            player.stats[stats.thieving] = 75
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 56
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.gnomePouch))
            assertEquals(1335, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the gnome's pocket.")
        }

    @Test
    fun GameTestState.`gnome arrow shafts roll inclusive two through four`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 75
            val npc = spawnGnome(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)

            random.next = 0
            random.then = 0
            random.then = 2
            player.opNpc3(npc)
            advance()
            assertEquals(2, player.count(ThievingObjs.arrowShaft))

            advance()
            random.next = 0
            random.then = 55
            random.then = 4
            player.opNpc3(npc)
            advance()
            assertEquals(6, player.count(ThievingObjs.arrowShaft))
            assertEquals(2670, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`gnome weighted non-pouch reward branches match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 75
            val npc = spawnGnome(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            val rewards =
                listOf(
                    86 to ThievingObjs.swampToad,
                    110 to ThievingObjs.goldOre,
                    118 to ThievingObjs.earthRune,
                    123 to ThievingObjs.kingWorm,
                    126 to ThievingObjs.fireOrb,
                )

            for ((index, reward) in rewards.withIndex()) {
                random.next = 0
                random.then = reward.first
                player.opNpc3(npc)
                advance()
                assertEquals(1, player.count(reward.second), "reward=${reward.second.id}")
                if (index != rewards.lastIndex) {
                    advance()
                }
            }
            assertEquals(6675, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`gnome failure modifiers and pouch operations match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 75
            val npc = spawnGnome(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.gnome)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(8, player.actionDelay - player.currentMapClock)
            assertEquals(9, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(9, player.stat(stats.hitpoints))

            assertEquals(43..175, successBounds(deps, StandardPickpockets.gnome))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(45..183, successBounds(deps, StandardPickpockets.gnome))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_medium_complete, 1)
            assertEquals(43..175, successBounds(deps, StandardPickpockets.gnome))
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(47..192, successBounds(deps, StandardPickpockets.gnome))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(51..211, successBounds(deps, StandardPickpockets.gnome))

            player.inv[4] = InvObj(ThievingObjs.gnomePouch, 2)
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.gnome, all = false)
            }
            assertEquals(1, player.count(ThievingObjs.gnomePouch))
            assertEquals(300, player.count(objs.coins))
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.gnome, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.gnomePouch))
            assertEquals(600, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`rogue set gives one gnome pouch and three hundred direct coins once`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 75
            val npc = spawnGnome(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 56

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.gnomePouch))
            assertEquals(300, player.count(objs.coins))
            assertEquals(1335, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`rogue set doubles rolled gnome shafts and capacity rollback is atomic`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 75
            val npc = spawnGnome(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0
            random.then = 4

            player.opNpc3(npc)
            advance()
            assertEquals(8, player.count(ThievingObjs.arrowShaft))
            assertEquals(1335, player.statMap.getFineXP(stats.thieving) - startXp)

            for (slot in 0 until player.inv.size) {
                player.inv[slot] = null
            }
            player.inv[0] = InvObj(ThievingObjs.gnomePouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            advance()
            random.next = 0
            random.then = 86
            player.opNpc3(npc)
            advance()

            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(ThievingObjs.gnomePouch))
            assertEquals(0, player.count(ThievingObjs.swampToad))
            assertEquals(1335, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`Lletya elf enforces level and awards variable coin pouch with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnLletyaElf(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 85 to pick this pocket.")

            player.stats[stats.thieving] = 85
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.elfPouch))
            assertEquals(3533, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the elf's pocket.")
        }

    @Test
    fun GameTestState.`Lletya elf weighted non-pouch reward branches match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 85
            val npc = spawnLletyaElf(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            val rewards =
                listOf(
                    Triple(105, ThievingObjs.deathRune, 2),
                    Triple(113, ThievingObjs.natureRune, 3),
                    Triple(118, ThievingObjs.jugWine, 1),
                    Triple(124, ThievingObjs.diamond, 1),
                    Triple(125, ThievingObjs.fireOrb, 1),
                    Triple(127, ThievingObjs.goldOre, 1),
                )

            for ((index, reward) in rewards.withIndex()) {
                random.next = 0
                random.then = reward.first
                player.opNpc3(npc)
                advance()
                assertEquals(
                    reward.third,
                    player.count(reward.second),
                    "reward=${reward.second.id}",
                )
                if (index != rewards.lastIndex) {
                    advance()
                }
            }
            assertEquals(21198, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`Lletya elf failure modifiers and pouch operations match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 85
            val npc = spawnLletyaElf(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.lletyaElf)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(10, player.actionDelay - player.currentMapClock)
            assertEquals(11, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(5, player.stat(stats.hitpoints))

            assertEquals(6..100, successBounds(deps, StandardPickpockets.lletyaElf))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(6..105, successBounds(deps, StandardPickpockets.lletyaElf))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_medium_complete, 1)
            assertEquals(6..100, successBounds(deps, StandardPickpockets.lletyaElf))
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(6..110, successBounds(deps, StandardPickpockets.lletyaElf))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(6..121, successBounds(deps, StandardPickpockets.lletyaElf))

            player.inv[4] = InvObj(ThievingObjs.elfPouch, 2)
            random.next = 280
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.elf, all = false)
            }
            assertEquals(1, player.count(ThievingObjs.elfPouch))
            assertEquals(280, player.count(objs.coins))
            random.next = 350
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.elf, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.elfPouch))
            assertEquals(630, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`rogue set gives one elf pouch and one direct variable coin payout`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 85
            val npc = spawnLletyaElf(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0
            random.then = 280

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.elfPouch))
            assertEquals(280, player.count(objs.coins))
            assertEquals(3533, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`rogue set doubles elf runes and capacity rollback is atomic`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 85
            val npc = spawnLletyaElf(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 113

            player.opNpc3(npc)
            advance()
            assertEquals(6, player.count(ThievingObjs.natureRune))
            assertEquals(3533, player.statMap.getFineXP(stats.thieving) - startXp)

            for (slot in 0 until player.inv.size) {
                player.inv[slot] = null
            }
            player.inv[0] = InvObj(ThievingObjs.elfPouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            advance()
            random.next = 0
            random.then = 124
            player.opNpc3(npc)
            advance()

            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(ThievingObjs.elfPouch))
            assertEquals(0, player.count(ThievingObjs.diamond))
            assertEquals(3533, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`Vyre enforces level and awards variable coin pouch with exact xp`() =
        runThievingTest { deps ->
            prepare()
            val npc = spawnVyre(deps)

            player.opNpc3(npc)
            advance()
            assertMessageSent("You need a Thieving level of 82 to pick this pocket.")

            player.stats[stats.thieving] = 82
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0
            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.vyrePouch))
            assertEquals(3069, player.statMap.getFineXP(stats.thieving) - startXp)
            assertMessageSent("You pick the vyre's pocket.")
        }

    @Test
    fun GameTestState.`Vyre weighted non-pouch reward branches match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 82
            val npc = spawnVyre(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            val rewards =
                listOf(
                    Triple(109, ThievingObjs.deathRune, 2),
                    Triple(117, ThievingObjs.bloodRune, 4),
                    Triple(119, ThievingObjs.bloodPint, 1),
                    Triple(125, ThievingObjs.uncutRuby, 1),
                    Triple(130, ThievingObjs.diamond, 1),
                    Triple(131, ThievingObjs.cookedMysteryMeat, 1),
                )

            for ((index, reward) in rewards.withIndex()) {
                random.next = 0
                random.then = reward.first
                player.opNpc3(npc)
                advance()
                assertEquals(
                    reward.third,
                    player.count(reward.second),
                    "reward=${reward.second.id}",
                )
                if (index != rewards.lastIndex) {
                    advance()
                }
            }
            assertEquals(18414, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`Vyre failure modifiers and pouch operations match cutoff`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 82
            val npc = spawnVyre(deps)
            random.next = 99

            player.withProtectedAccess {
                deps.pickpocketing.attempt(this, npc, StandardPickpockets.vyre)
            }

            assertEquals(seqs.stunned_thieving.id, player.pendingSequence.id)
            assertEquals(spotanims.stunned_thieving.id, pendingSpotanim())
            assertEquals(10, player.actionDelay - player.currentMapClock)
            assertEquals(11, deps.stuns.remainingCycles(player))
            advance()
            assertEquals(5, player.stat(stats.hitpoints))

            assertEquals(8..128, successBounds(deps, StandardPickpockets.vyre))
            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(8..134, successBounds(deps, StandardPickpockets.vyre))
            player.hands = null
            player.setVarBit(varbits.ardougne_diary_medium_complete, 1)
            assertEquals(8..128, successBounds(deps, StandardPickpockets.vyre))
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            assertEquals(8..140, successBounds(deps, StandardPickpockets.vyre))
            player.back = InvObj(objs.thieving_skillcape)
            assertEquals(8..154, successBounds(deps, StandardPickpockets.vyre))

            player.inv[4] = InvObj(ThievingObjs.vyrePouch, 2)
            random.next = 230
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.vyre, all = false)
            }
            assertEquals(1, player.count(ThievingObjs.vyrePouch))
            assertEquals(230, player.count(objs.coins))
            random.next = 315
            player.withProtectedAccess {
                deps.pouches.open(this, 4, CoinPouchDefinitions.vyre, all = true)
            }
            assertEquals(0, player.count(ThievingObjs.vyrePouch))
            assertEquals(545, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`rogue set gives one Vyre pouch and fixed two hundred thirty coins`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 82
            val npc = spawnVyre(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.vyrePouch))
            assertEquals(230, player.count(objs.coins))
            assertEquals(3069, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`rogue set doubles Vyre blood runes and capacity rollback is atomic`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 82
            val npc = spawnVyre(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 117

            player.opNpc3(npc)
            advance()
            assertEquals(8, player.count(ThievingObjs.bloodRune))
            assertEquals(3069, player.statMap.getFineXP(stats.thieving) - startXp)

            for (slot in 0 until player.inv.size) {
                player.inv[slot] = null
            }
            player.inv[0] = InvObj(ThievingObjs.vyrePouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            advance()
            random.next = 0
            random.then = 130
            player.opNpc3(npc)
            advance()

            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(ThievingObjs.vyrePouch))
            assertEquals(0, player.count(ThievingObjs.diamond))
            assertEquals(3069, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`Vyre blood shard is an independent one in five thousand tertiary`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 82
            val npc = spawnVyre(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)

            random.next = 0
            random.then = 0
            random.then = 1
            player.opNpc3(npc)
            advance()
            assertEquals(0, player.count(ThievingObjs.bloodShard))
            assertEquals(1, player.count(ThievingObjs.vyrePouch))

            advance()
            random.next = 0
            random.then = 0
            random.then = 0
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(ThievingObjs.bloodShard))
            assertEquals(2, player.count(ThievingObjs.vyrePouch))
            assertEquals(6138, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`rogue double result applies to the Vyre blood shard`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 82
            val npc = spawnVyre(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)

            random.next = 0
            random.then = 0
            random.then = 0
            player.opNpc3(npc)
            advance()

            assertEquals(2, player.count(ThievingObjs.bloodShard))
            assertEquals(1, player.count(ThievingObjs.vyrePouch))
            assertEquals(230, player.count(objs.coins))
            assertEquals(3069, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`doubled Vyre shards owner drop together when only one slot is free`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 82
            player.observerUUID = 1L
            val startXp = player.statMap.getFineXP(stats.thieving)
            player.inv[0] = InvObj(ThievingObjs.vyrePouch)
            player.inv[1] = InvObj(objs.coins)
            for (slot in 2 until (player.inv.size - 1)) {
                player.inv[slot] = InvObj(objs.bones)
            }
            val npc = spawnVyre(deps)

            random.next = 0
            random.then = 0
            random.then = 0
            player.opNpc3(npc)
            advance()

            assertEquals(0, player.count(ThievingObjs.bloodShard))
            val floor =
                deps.objRepo
                    .findAll(player.coords)
                    .filter { it.type == ThievingObjs.bloodShard.id }
                    .toList()
            assertEquals(2, floor.size)
            assertEquals(2, floor.sumOf { it.count })
            assertTrue(floor.all { it.isPrivate })
            assertTrue(floor.all { it.isOriginalOwner(player) })
            assertEquals(2, player.count(ThievingObjs.vyrePouch))
            assertEquals(231, player.count(objs.coins))
            assertEquals(3069, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`rogue pieces provide fifteen percent each and full set guarantees double`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(0, rogueChance(deps))
            player.hat = InvObj(ThievingObjs.rogueMask)
            assertEquals(15, rogueChance(deps))
            player.torso = InvObj(ThievingObjs.rogueTop)
            assertEquals(30, rogueChance(deps))
            player.legs = InvObj(ThievingObjs.rogueTrousers)
            assertEquals(45, rogueChance(deps))
            player.feet = InvObj(ThievingObjs.rogueBoots)
            assertEquals(60, rogueChance(deps))
            player.hands = InvObj(ThievingObjs.rogueGloves)
            assertEquals(100, rogueChance(deps))

            player.hands = InvObj(objs.gloves_of_silence)
            assertEquals(60, rogueChance(deps))
        }

    @Test
    fun GameTestState.`cache 233 pickpocket clue pools pin every active initial step`() =
        runThievingTest { deps ->
            val medium = deps.clueSteps.steps(ClueTier.Medium)
            val hard = deps.clueSteps.steps(ClueTier.Hard)
            val elite = deps.clueSteps.steps(ClueTier.Elite)

            assertEquals(Cache233ClueSteps.MEDIUM_SIZE, medium.size)
            assertEquals(2_801, medium.first().id)
            assertEquals(30_935, medium.last().id)
            assertEquals(Cache233ClueSteps.HARD_SIZE, hard.size)
            assertEquals(2_722, hard.first().id)
            assertEquals(30_931, hard.last().id)
            assertEquals(Cache233ClueSteps.ELITE_SIZE, elite.size)
            assertEquals(12_073, elite.first().id)
            assertEquals(30_932, elite.last().id)
            for ((tier, steps) in
                listOf(ClueTier.Medium to medium, ClueTier.Hard to hard, ClueTier.Elite to elite)) {
                assertTrue(steps.all { it.name == "Clue scroll (${tier.key})" })
                assertTrue(steps.all { it.iop[0] == "Read" && it.iop[1] == "Check steps" })
            }
        }

    @Test
    fun GameTestState.`pickpocket clue rates use exact combat achievement reductions`() =
        runThievingTest { deps ->
            prepare()
            assertEquals(1_000, clueDenominator(deps, StandardPickpockets.paladin))
            assertEquals(150, clueDenominator(deps, StandardPickpockets.gnome))
            assertEquals(1_400, clueDenominator(deps, StandardPickpockets.hero))

            player.setVarBit(varbits.ca_tier_status_medium, 2)
            player.setVarBit(varbits.ca_tier_status_hard, 2)
            player.setVarBit(varbits.ca_tier_status_elite, 2)
            assertEquals(950, clueDenominator(deps, StandardPickpockets.paladin))
            assertEquals(142, clueDenominator(deps, StandardPickpockets.gnome))
            assertEquals(1_330, clueDenominator(deps, StandardPickpockets.hero))
        }

    @Test
    fun GameTestState.`paladin clue roll awards one hard step beside guaranteed loot`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 70
            val npc = spawnPaladin(deps)
            random.next = 0
            random.then = 0
            random.then = 0

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.paladinPouch))
            assertEquals(2, player.count(ThievingObjs.chaosRune))
            assertEquals(1, player.count(deps.clueSteps.steps(ClueTier.Hard).first()))
        }

    @Test
    fun GameTestState.`gnome clue roll awards one medium step beside weighted loot`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 75
            val npc = spawnGnome(deps)
            random.next = 0
            random.then = 56
            random.then = 0
            random.then = Cache233ClueSteps.MEDIUM_SIZE - 1

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.gnomePouch))
            assertEquals(1, player.count(deps.clueSteps.steps(ClueTier.Medium).last()))
        }

    @Test
    fun GameTestState.`hero clue roll awards one elite step beside weighted loot`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 80
            val npc = spawnHero(deps)
            random.next = 0
            random.then = 0
            random.then = 0
            random.then = Cache233ClueSteps.ELITE_SIZE - 1

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.heroPouch))
            assertEquals(1, player.count(deps.clueSteps.steps(ClueTier.Elite).last()))
        }

    @Test
    fun GameTestState.`owned clues skip rolls while X Marks awards boxes and reports capacity`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 70
            val npc = spawnPaladin(deps)
            val hardStep = deps.clueSteps.steps(ClueTier.Hard).first()
            player.inv[0] = InvObj(hardStep)
            random.next = 0
            random.then = 999

            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(hardStep))
            assertEquals(999, deps.random.of(1_000))

            for (slot in 0 until player.inv.size) {
                player.inv[slot] = null
            }
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            advance()
            random.next = 0
            random.then = 0
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(objs.scroll_box_hard))

            val boxSlot =
                (0 until player.inv.size).first { player.inv[it]?.id == objs.scroll_box_hard.id }
            player.inv[boxSlot] = InvObj(objs.scroll_box_hard, 2)
            advance()
            random.next = 0
            random.then = 0
            player.opNpc3(npc)
            advance()
            assertEquals(2, player.count(objs.scroll_box_hard))
            assertMessageSent(
                "You have a sneaking suspicion that you would have received a hard scroll box."
            )
        }

    @Test
    fun GameTestState
        .`full inventory blocks loose clue but permits an existing scroll box stack`() =
        runThievingTest { deps ->
            prepare()
            player.stats[stats.thieving] = 75
            val npc = spawnGnome(deps)
            player.inv[0] = InvObj(ThievingObjs.gnomePouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            random.next = 0
            random.then = 56
            random.then = 0
            random.then = 0

            player.opNpc3(npc)
            advance()
            assertEquals(2, player.count(ThievingObjs.gnomePouch))
            assertEquals(0, player.count(deps.clueSteps.steps(ClueTier.Medium).first()))

            player.inv[1] = InvObj(objs.scroll_box_medium)
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            advance()
            random.next = 0
            random.then = 56
            random.then = 0
            player.opNpc3(npc)
            advance()
            assertEquals(3, player.count(ThievingObjs.gnomePouch))
            assertEquals(2, player.count(objs.scroll_box_medium))
        }

    @Test
    fun GameTestState.`rogue equipment never doubles a pickpocket clue`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.stats[stats.thieving] = 80
            val npc = spawnHero(deps)
            random.next = 0
            random.then = 0
            random.then = 0
            random.then = 0

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.heroPouch))
            assertEquals(200, player.count(objs.coins))
            assertEquals(1, player.count(deps.clueSteps.steps(ClueTier.Elite).first()))
        }

    @Test
    fun GameTestState.`full rogue set gives one pouch plus direct coins without doubling xp`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            val npc = spawnFarmer(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0

            player.opNpc3(npc)
            advance()

            assertEquals(1, player.count(ThievingObjs.farmerPouch))
            assertEquals(9, player.count(objs.coins))
            assertEquals(145, player.statMap.getFineXP(stats.thieving) - startXp)
        }

    @Test
    fun GameTestState.`one rogue piece doubles below fifteen and not at the boundary`() =
        runThievingTest { deps ->
            prepare()
            player.torso = InvObj(ThievingObjs.rogueTop)
            val npc = spawnCitizen(deps)

            random.next = 0
            random.then = 14
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(ThievingObjs.citizenPouch))
            assertEquals(3, player.count(objs.coins))

            advance()
            random.next = 0
            random.then = 15
            player.opNpc3(npc)
            advance()
            assertEquals(2, player.count(ThievingObjs.citizenPouch))
            assertEquals(3, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`full rogue set doubles non-pouch farmer reward`() = runThievingTest { deps ->
        prepareFarmerWithRogueSet()
        val npc = spawnFarmer(deps)
        random.next = 0
        random.then = 123

        player.opNpc3(npc)
        advance()

        assertEquals(2, player.count(ThievingObjs.potatoSeed))
        assertEquals(0, player.count(ThievingObjs.farmerPouch))
    }

    @Test
    fun GameTestState.`rogue pouch bonus transaction rolls back when direct coins cannot fit`() =
        runThievingTest { deps ->
            prepareFarmerWithRogueSet()
            player.inv[0] = InvObj(ThievingObjs.farmerPouch)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bones)
            }
            val npc = spawnFarmer(deps)
            val startXp = player.statMap.getFineXP(stats.thieving)
            random.next = 0
            random.then = 0

            player.opNpc3(npc)
            advance()

            assertMessageSent("You don't have enough inventory space.")
            assertEquals(1, player.count(ThievingObjs.farmerPouch))
            assertEquals(0, player.count(objs.coins))
            assertEquals(startXp, player.statMap.getFineXP(stats.thieving))
        }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.thieving] = 1
        player.stats[stats.hitpoints] = 10
        player.placeAt(TEST_COORDS)
    }

    private fun GameTestScope.prepareFarmerWithRogueSet() {
        prepare()
        player.stats[stats.thieving] = 10
        player.hat = InvObj(ThievingObjs.rogueMask)
        player.torso = InvObj(ThievingObjs.rogueTop)
        player.legs = InvObj(ThievingObjs.rogueTrousers)
        player.hands = InvObj(ThievingObjs.rogueGloves)
        player.feet = InvObj(ThievingObjs.rogueBoots)
    }

    private fun GameTestScope.spawnCitizen(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.man])

    private fun GameTestScope.spawnFarmer(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.farmer1])

    private fun GameTestScope.spawnWarrior(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.warrior])

    private fun GameTestScope.spawnRogue(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.rogue])

    private fun GameTestScope.spawnGuard(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.guards.first()])

    private fun GameTestScope.spawnArdougneKnight(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.knightOfArdougne])

    private fun GameTestScope.spawnVarlamoreKnight(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.varlamoreKnightM1])

    private fun GameTestScope.spawnWatchman(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.watchman])

    private fun GameTestScope.spawnMenaphiteThug(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.menaphiteThugAfterQuest])

    private fun GameTestScope.spawnPaladin(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.paladin])

    private fun GameTestScope.spawnDesertBandit(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.desertBandit])

    private fun GameTestScope.spawnHero(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.hero])

    private fun GameTestScope.spawnGnome(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.gnome])

    private fun GameTestScope.spawnLletyaElf(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.goreu])

    private fun GameTestScope.spawnVyre(deps: Dependencies): Npc =
        spawnNpc(player.coords.translateX(1), deps.npcs[ThievingNpcs.vyres.first()])

    private fun GameTestScope.pendingSpotanim(): Int =
        org.rsmod.game.spot.EntitySpotanim(player.pendingSpotanims.getLong(0)).id

    private fun GameTestScope.pouchLimit(deps: Dependencies): Int {
        var limit = -1
        player.withProtectedAccess { limit = deps.pouches.limit(this) }
        return limit
    }

    private fun GameTestScope.successBounds(
        deps: Dependencies,
        definition: PickpocketDefinition = StandardPickpockets.man,
    ): IntRange {
        var bounds = IntRange.EMPTY
        player.withProtectedAccess { bounds = deps.pickpocketing.successBounds(this, definition) }
        return bounds
    }

    private fun GameTestScope.rogueChance(deps: Dependencies): Int {
        var chance = -1
        player.withProtectedAccess { chance = deps.pickpocketing.rogueDoubleChance(this) }
        return chance
    }

    private fun GameTestScope.clueDenominator(
        deps: Dependencies,
        definition: PickpocketDefinition,
    ): Int {
        var denominator = -1
        player.withProtectedAccess {
            denominator =
                deps.clueDrops.effectiveDenominator(this, requireNotNull(definition.clueDrop))
        }
        return denominator
    }

    private fun GameTestState.runThievingTest(testBody: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(PickpocketScript::class, PlayerHitScript::class),
            testBody = testBody,
        )

    data class Dependencies
    @Inject
    constructor(
        val npcs: NpcTypeList,
        val pickpocketing: Pickpocketing,
        val pouches: CoinPouches,
        val stuns: ThievingStunStateRepository,
        val shadowVeil: ShadowVeilStateRepository,
        val clueDrops: PickpocketClueDrops,
        val clueSteps: Cache233ClueSteps,
        val random: GameRandom,
        val objRepo: ObjRepository,
    )

    private companion object {
        val TEST_COORDS: CoordGrid = CoordGrid(3200, 3200, 0)
    }
}
