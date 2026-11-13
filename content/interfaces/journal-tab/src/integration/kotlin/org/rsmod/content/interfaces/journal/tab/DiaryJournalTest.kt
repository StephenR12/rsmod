package org.rsmod.content.interfaces.journal.tab

import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.journal.tab.configs.journal_components
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class DiaryJournalTest {
    @Test
    fun GameTestState
        .`all twelve distinct Lumbridge easy tasks unlock one ring and one selectable lamp`() =
        runInjectedGameTest(HeldInteractions::class, null, DiaryLamps::class) { held ->
            player.members = true
            val tier = DiaryRegion.Lumbridge.tier(DiaryDifficulty.Easy)
            val tasks =
                DiaryTask.entries.filter {
                    it.region == DiaryRegion.Lumbridge && it.difficulty == DiaryDifficulty.Easy
                }
            assertEquals((1..12).toList(), tasks.map { it.number }.sorted())
            tasks.dropLast(1).forEach(player::completeDiaryTask)
            assertEquals(0, player.vars[tier.complete])
            player.completeDiaryTask(tasks.last())
            assertEquals(12, player.vars[tier.count])
            assertEquals(1, player.vars[tier.complete])
            player.withProtectedAccess {
                assertTrue(DiaryRewards.claim(this, DiaryRegion.Lumbridge))
            }
            val lamp = DiaryRewardItems.ordinary.first()
            val slot = player.inv.indexOfFirst { it?.id == lamp.item.id }
            assertTrue(slot >= 0)
            player.stats[stats.attack] = 29
            player.withProtectedAccess { held.interact(this, player.inv, slot, HeldOp.Op1) }
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(DiaryComponents.skillChoices, subcomponent = 0)
            )
            assertMessageSent("You need level 30 in that skill to use this lamp.")
            assertEquals(1, player.count(lamp.item))
            player.stats[stats.attack] = 30
            val before = player.statMap.getFineXP(stats.attack)
            player.withProtectedAccess { held.interact(this, player.inv, slot, HeldOp.Op1) }
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(DiaryComponents.skillChoices, subcomponent = 0)
            )
            assertEquals(0, player.count(lamp.item))
            assertEquals(before + 25000, player.statMap.getFineXP(stats.attack))
        }

    @Test
    fun GameTestState.`member progress shares native flags with the journal and is idempotent`() =
        runGameTest(DiaryJournal::class) {
            val region = DiaryRegion.Lumbridge
            val tier = region.tier(DiaryDifficulty.Easy)
            player.members = false
            player.completeDiaryTask(DiaryTask.LearnAgeFromHans)
            assertEquals(0, player.vars[tier.count])
            player.members = true
            player.completeDiaryTask(DiaryTask.LearnAgeFromHans)
            player.completeDiaryTask(DiaryTask.LearnAgeFromHans)
            assertEquals(1, player.vars[tier.count])
            assertEquals(1 shl 5, player.vars[cacheTypes.varps.getValue(1194)])
            assertEquals(0, player.vars[tier.reward])
            assertEquals(
                492,
                DiaryRegion.entries.sumOf { area ->
                    DiaryDifficulty.entries.sumOf { area.tasks(it).size }
                },
            )
            player.ifOpenOverlay(interfaces.area_task, journal_components.tab_container)
            player.ifButton(DiaryComponents.taskbox, region.journalIndex)
            advance()
            val text = client.filterIsInstance<IfSetText>().map { it.text }
            assertTrue(region.title in text)
            assertTrue(text.any { it.startsWith("<str>") && "Hans" in it })
        }

    @Test
    fun GameTestState.`reward claims respect tier order and commit equipment and lamp together`() =
        runGameTest {
            player.members = true
            val region = DiaryRegion.Lumbridge
            val easy = region.tier(DiaryDifficulty.Easy)
            val medium = region.tier(DiaryDifficulty.Medium)
            VarPlayerIntMapSetter.set(player, medium.complete, 1)
            player.withProtectedAccess { assertFalse(DiaryRewards.claim(this, region)) }
            VarPlayerIntMapSetter.set(player, easy.complete, 1)
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.chisel)
            player.withProtectedAccess { assertFalse(DiaryRewards.claim(this, region)) }
            assertEquals(0, player.vars[easy.reward])
            player.inv[0] = null
            player.inv[1] = null
            player.withProtectedAccess { assertTrue(DiaryRewards.claim(this, region)) }
            assertEquals(1, player.vars[easy.reward])
            assertEquals(13125, player.inv[0]?.id)
            assertEquals(13145, player.inv[1]?.id)
            player.inv[1] = null
            player.withProtectedAccess { assertTrue(DiaryRewards.claim(this, region)) }
            assertEquals(1, player.vars[medium.reward])
            assertEquals(13126, player.inv[0]?.id)
            assertEquals(13146, player.inv[1]?.id)
            player.withProtectedAccess { assertFalse(DiaryRewards.claim(this, region)) }
        }
}
