package org.rsmod.content.areas.city.lumbridge.npcs

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import net.rsprot.protocol.game.outgoing.sound.MidiJingle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.jingles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.areas.city.lumbridge.configs.lumbridge_npcs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class WoodsmanTutorTest {
    @Test
    fun GameTestState.`cache 233 Woodsman tutor and Skillcape assets are pinned`() =
        runWoodsmanTest { deps ->
            val tutor = deps.npcs[lumbridge_npcs.woodsman_tutor]
            assertEquals(3226, tutor.id)
            assertEquals("Woodsman tutor", tutor.name)
            assertEquals("Talk-to", tutor.op[0])
            assertEquals(9807, objs.woodcutting_skillcape.id)
            assertEquals(9808, objs.woodcutting_skillcape_t.id)
            assertEquals(9809, objs.woodcutting_hood.id)
            assertEquals(231, jingles.cape_of_accomplishment.id)
            assertEquals(99, WoodcuttingSkillcapePurchase.REQUIRED_LEVEL)
            assertEquals(99_000, WoodcuttingSkillcapePurchase.PRICE)
        }

    @Test
    fun GameTestState.`first mastery purchases untrimmed cape and hood atomically`() =
        runWoodsmanTest { deps ->
            prepareMaster()
            player.inv[0] = InvObj(objs.coins, 100_000)

            var result: WoodcuttingSkillcapePurchaseResult? = null
            player.withProtectedAccess { result = deps.skillcapes.purchase(this) }

            assertEquals(WoodcuttingSkillcapePurchaseResult.Purchased, result)
            assertEquals(1_000, player.inv.count(objs.coins))
            assertEquals(1, player.inv.count(objs.woodcutting_skillcape))
            assertEquals(0, player.inv.count(objs.woodcutting_skillcape_t))
            assertEquals(1, player.inv.count(objs.woodcutting_hood))
            assertEquals(231, client.filterIsInstance<MidiJingle>().single().id)
        }

    @Test
    fun GameTestState.`second mastery purchases trimmed cape`() = runWoodsmanTest { deps ->
        prepareMaster()
        player.setBaseLevel(stats.attack, 99)
        player.inv[0] = InvObj(objs.coins, 99_000)

        var result: WoodcuttingSkillcapePurchaseResult? = null
        player.withProtectedAccess { result = deps.skillcapes.purchase(this) }

        assertEquals(WoodcuttingSkillcapePurchaseResult.Purchased, result)
        assertEquals(0, player.inv.count(objs.woodcutting_skillcape))
        assertEquals(1, player.inv.count(objs.woodcutting_skillcape_t))
        assertEquals(1, player.inv.count(objs.woodcutting_hood))
    }

    @Test
    fun GameTestState.`sale gates preserve coins and inventory`() = runWoodsmanTest { deps ->
        player.clearAllInvs()
        player.members = false
        player.setBaseLevel(stats.woodcutting, 99)
        player.inv[0] = InvObj(objs.coins, 100_000)
        var result: WoodcuttingSkillcapePurchaseResult? = null
        player.withProtectedAccess { result = deps.skillcapes.purchase(this) }
        assertEquals(WoodcuttingSkillcapePurchaseResult.MembersOnly, result)
        assertEquals(100_000, player.inv.count(objs.coins))

        player.members = true
        player.setBaseLevel(stats.woodcutting, 98)
        player.withProtectedAccess { result = deps.skillcapes.purchase(this) }
        assertEquals(WoodcuttingSkillcapePurchaseResult.NotEligible, result)
        assertEquals(100_000, player.inv.count(objs.coins))

        player.setBaseLevel(stats.woodcutting, 99)
        player.inv[0] = InvObj(objs.coins, 98_999)
        player.withProtectedAccess { result = deps.skillcapes.purchase(this) }
        assertEquals(WoodcuttingSkillcapePurchaseResult.InsufficientCoins, result)
        assertEquals(98_999, player.inv.count(objs.coins))

        player.fillInv()
        player.inv[0] = InvObj(objs.coins, 100_000)
        player.withProtectedAccess { result = deps.skillcapes.purchase(this) }
        assertEquals(WoodcuttingSkillcapePurchaseResult.NoInventorySpace, result)
        assertEquals(100_000, player.inv.count(objs.coins))
        assertEquals(0, player.inv.count(objs.woodcutting_skillcape))
        assertEquals(0, player.inv.count(objs.woodcutting_hood))
        assertTrue(client.filterIsInstance<MidiJingle>().isEmpty())
    }

    @Test
    fun GameTestState.`free world mastery dialogue reaches the exact refusal`() =
        runWoodsmanTest { deps ->
            player.members = false
            player.setBaseLevel(stats.woodcutting, 99)
            val tutor =
                spawnNpc(player.coords.translate(1, 0), deps.npcs[lumbridge_npcs.woodsman_tutor])

            player.opNpc1(tutor)
            advance()
            assertDialogueText(
                "Wow! It's not often I meet somebody as accomplished as you in Woodcutting! " +
                    "Perhaps you should become the tutor, instead of me! Seeing as you're so " +
                    "skilled, maybe you are interested in buying a Skillcape of"
            )
            resumeNpc()
            assertDialogueText("Woodcutting?")
            resumeNpc()
            assertMenu("Select an option", "No, thanks.", "Yes, please")
            choose(2)
            assertDialogueText("Yes, please.")
            resumePlayer()
            assertDialogueText("I can't sell you one here. Try talking to me later.")
        }

    @Test
    fun GameTestState.`members mastery dialogue sells the correct pair`() =
        runWoodsmanTest { deps ->
            prepareMaster()
            player.inv[0] = InvObj(objs.coins, 100_000)
            val tutor =
                spawnNpc(player.coords.translate(1, 0), deps.npcs[lumbridge_npcs.woodsman_tutor])

            player.opNpc1(tutor)
            advance()
            resumeNpc()
            resumeNpc()
            choose(2)
            resumePlayer()
            assertDialogueText(
                "Anybody who has spent as much time cutting trees as you deserves the right to " +
                    "own one. Wearing this cape will increase your chance of finding bird's " +
                    "nests. That'll be 99000 coins, please."
            )
            resumeNpc()
            assertMenu("Select an option", "99000! That's too rich for me.", "No problem.")
            choose(2)
            assertDialogueText("No problem.")
            resumePlayer()

            assertEquals(1_000, player.inv.count(objs.coins))
            assertEquals(1, player.inv.count(objs.woodcutting_skillcape))
            assertEquals(1, player.inv.count(objs.woodcutting_hood))
            assertDialogueText("Excellent! Wear that cape with pride my friend.")
        }

    @Test
    fun GameTestState.`level cutoffs expose the dated tree menus`() = runWoodsmanTest { deps ->
        player.setBaseLevel(stats.woodcutting, 19)
        var tutor =
            spawnNpc(player.coords.translate(1, 0), deps.npcs[lumbridge_npcs.woodsman_tutor])
        player.opNpc1(tutor)
        advance()
        assertMenu(
            "Select an option",
            "Tell me about different trees and axes.",
            "What is that cape you're wearing?",
            "Goodbye.",
        )

        choose(3)
        assertDialogueText("Goodbye.")
        resumePlayer()
        player.setBaseLevel(stats.woodcutting, 29)
        tutor = spawnNpc(player.coords.translate(0, 1), deps.npcs[lumbridge_npcs.woodsman_tutor])
        player.opNpc1(tutor)
        advance()
        assertMenu(
            "Select an option",
            "Any advice for an advanced woodcutter?",
            "Tell me about different trees and axes.",
            "What is that cape you're wearing?",
            "Goodbye.",
        )
        choose(2)
        resumePlayer()
        assertMenu(
            "Trees",
            "Oak and Willow",
            "Maple and Yew",
            "Magic and other trees",
            "Axes",
            "Go back to teaching",
        )
    }

    private fun GameTestScope.prepareMaster() {
        player.clearAllInvs()
        player.members = true
        player.setBaseLevel(stats.woodcutting, 99)
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

    private fun GameTestScope.assertMenu(title: String, vararg options: String) {
        val expected = listOf(title, options.joinToString("|"))
        val menus = client.filterIsInstance<RunClientScript>().filter { it.id == 58 }
        assertTrue(menus.any { it.values == expected }) {
            "Dialogue menu not found: $expected | actual=${menus.map { it.values }}"
        }
    }

    private fun GameTestState.runWoodsmanTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, null, WoodsmanTutor::class, testBody = body)

    data class Dependencies
    @Inject
    constructor(
        val skillcapes: WoodcuttingSkillcapePurchase,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
    )
}
