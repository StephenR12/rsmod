package org.rsmod.content.skills.runecraft

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class PouchServicesTest {
    @Test
    fun GameTestState
        .`stitch returns stored essence and a full inventory preserves every ingredient`() =
        runInjectedGameTest(HeldUInteractions::class, null, PouchServices::class) { use ->
            player.members = true
            player.stats[stats.runecrafting] = 75
            player.stats[stats.crafting] = 56
            player.fillInv()
            val needle = objTypes.values.single { it.internalName == "abyssal_needle" }
            player.inv[0] = InvObj(needle)
            val pouches =
                listOf(
                    RunecraftObjs.smallPouch,
                    RunecraftObjs.mediumPouch,
                    RunecraftObjs.largePouch,
                    RunecraftObjs.giantPouch,
                )
            pouches.forEachIndexed { i, item -> player.inv[i + 1] = InvObj(item) }
            player.withProtectedAccess {
                vars[RunecraftVarbits.largePouchAmount] = 9
                vars[RunecraftVarbits.largePouchType] = EssenceType.Pure.code
            }
            fun stitch() =
                player.withProtectedAccess {
                    use.interact(this, inv, objTypes[RunecraftObjs.smallPouch], 1, needle, 0)
                }
            val xp = player.statMap.getFineXP(stats.crafting)
            stitch()
            advance(4)
            assertEquals(1, player.count(needle))
            assertEquals(1, player.count(RunecraftObjs.largePouch))
            for (slot in 5..13) player.inv[slot] = null
            stitch()
            advance(4)
            assertEquals(0, player.count(needle))
            assertEquals(1, player.count(RunecraftObjs.colossalPouch))
            assertEquals(9, player.count(RunecraftObjs.pureEssence))
            assertEquals(0, player.vars[RunecraftVarbits.largePouchAmount])
            assertEquals(xp + 10000, player.statMap.getFineXP(stats.crafting))
        }

    @Test
    fun GameTestState
        .`Cordelia repairs a banked pouch once and never takes payment for a healthy pouch`() =
        runGameTest(PouchServices::class) {
            player.members = true
            player.withProtectedAccess {
                vars[cacheTypes.varbits.getValue(14672)] = 1
                vars[cacheTypes.varbits.getValue(13738)] = 150
            }
            val pearl = objTypes.values.single { it.internalName == "abyssal_pearl" }
            player.inv[0] = InvObj(pearl, 2)
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            bank[0] = InvObj(RunecraftObjs.largePouchDegraded)
            val npc =
                spawnNpc(
                    player.coords.translate(1, 0),
                    npcTypes.values.single { it.internalName == "gotr_cordelia_2ops" },
                )
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(pearl))
            assertEquals(RunecraftObjs.largePouch.id, bank[0]?.id)
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(pearl))
        }
}
