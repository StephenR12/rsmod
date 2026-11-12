package org.rsmod.content.skills.hunter.experts

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
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTypeList

@Execution(ExecutionMode.SAME_THREAD)
class HunterExpertKitGrantTest {
    @Test
    fun GameTestState.`cache 233 experts kit and talk routes are pinned`() =
        runHunterExpertTest { deps ->
            val polar = deps.npcs[npcs.hunting_polar_information]
            val jungle = deps.npcs[npcs.hunting_jungle_information]
            assertEquals(1503, polar.id)
            assertEquals(1504, jungle.id)
            assertEquals("Hunting expert", polar.name)
            assertEquals("Hunting expert", jungle.name)
            assertEquals("Talk-to", polar.op[0])
            assertEquals("Talk-to", jungle.op[0])
            assertEquals(11159, objs.hunter_kit.id)
            assertEquals(7359362605644340801L, polar.computeIdentityHash())
            assertEquals(7359362605644340802L, jungle.computeIdentityHash())
            assertTrue(
                deps.events.contains(
                    NpcEvents.Op1::class.java,
                    npcs.hunting_polar_information.id.toLong(),
                )
            )
            assertTrue(
                deps.events.contains(
                    NpcEvents.Op1::class.java,
                    npcs.hunting_jungle_information.id.toLong(),
                )
            )
            assertEquals(VarpLifetime.Perm, deps.varps[HunterExpertVarps.receivedHunterKit].scope)
        }

    @Test
    fun GameTestState.`first expert grants exactly one kit and consumes shared entitlement`() =
        runHunterExpertTest { deps ->
            var result: HunterExpertKitClaim? = null
            player.withProtectedAccess { result = deps.grant.claim(this) }

            assertEquals(HunterExpertKitClaim.Granted, result)
            assertEquals(1, player.inv.count(objs.hunter_kit))
            assertEquals(1, player.vars[HunterExpertVarps.receivedHunterKit])

            player.withProtectedAccess { result = deps.grant.claim(this) }
            assertEquals(HunterExpertKitClaim.AlreadyClaimed, result)
            assertEquals(1, player.inv.count(objs.hunter_kit))
        }

    @Test
    fun GameTestState.`claim is shared across Feldip and Rellekka and survives item loss`() =
        runHunterExpertTest { deps ->
            player.setVarp(HunterExpertVarps.receivedHunterKit, 1)
            player.inv.fillNulls()

            var result: HunterExpertKitClaim? = null
            player.withProtectedAccess { result = deps.grant.claim(this) }

            assertEquals(HunterExpertKitClaim.AlreadyClaimed, result)
            assertEquals(0, player.inv.count(objs.hunter_kit))
            assertEquals(1, player.vars[HunterExpertVarps.receivedHunterKit])
        }

    @Test
    fun GameTestState.`full inventory rejection is atomic and leaves entitlement available`() =
        runHunterExpertTest { deps ->
            player.fillInv()
            val before = player.inv.toList()
            var result: HunterExpertKitClaim? = null

            player.withProtectedAccess { result = deps.grant.claim(this) }

            assertEquals(HunterExpertKitClaim.NoInventorySpace, result)
            assertEquals(before, player.inv.toList())
            assertEquals(0, player.vars[HunterExpertVarps.receivedHunterKit])

            player.inv[0] = null
            player.withProtectedAccess { result = deps.grant.claim(this) }
            assertEquals(HunterExpertKitClaim.Granted, result)
            assertEquals(1, player.inv.count(objs.hunter_kit))
            assertEquals(1, player.vars[HunterExpertVarps.receivedHunterKit])
        }

    @Test
    fun GameTestState.`existing lunar kit does not consume the one-time expert grant`() =
        runHunterExpertTest { deps ->
            player.inv[0] = org.rsmod.game.inv.InvObj(objs.hunter_kit)
            var result: HunterExpertKitClaim? = null

            player.withProtectedAccess { result = deps.grant.claim(this) }

            assertEquals(HunterExpertKitClaim.Granted, result)
            assertEquals(2, player.inv.count(objs.hunter_kit))
            assertEquals(1, player.vars[HunterExpertVarps.receivedHunterKit])
        }

    @Test
    fun GameTestState.`cache 233 Hunter Skillcape set and sale constants are pinned`() =
        runHunterExpertTest { deps ->
            assertEquals(995, objs.coins.id)
            assertEquals(9948, objs.hunter_skillcape.id)
            assertEquals(9949, objs.hunter_skillcape_t.id)
            assertEquals(9950, objs.hunter_hood.id)
            assertEquals("Hunter cape", deps.objs[objs.hunter_skillcape].name)
            assertEquals("Hunter cape(t)", deps.objs[objs.hunter_skillcape_t].name)
            assertEquals("Hunter hood", deps.objs[objs.hunter_hood].name)
            assertEquals(99, HunterExpertSkillcapePurchase.HUNTER_LEVEL)
            assertEquals(99_000, HunterExpertSkillcapePurchase.PRICE)
            assertEquals(231, jingles.cape_of_accomplishment.id)
        }

    @Test
    fun GameTestState.`Skillcape sale rejects a player below base level 99 atomically`() =
        runHunterExpertTest { deps ->
            player.setBaseLevel(org.rsmod.api.config.refs.stats.hunter, 98)
            player.inv[0] = InvObj(objs.coins, 99_000)

            var result: HunterSkillcapePurchaseResult? = null
            player.withProtectedAccess { result = deps.skillcapes.purchase(this) }

            assertEquals(HunterSkillcapePurchaseResult.NotEligible, result)
            assertEquals(99_000, player.inv.count(objs.coins))
            assertEquals(0, player.inv.count(objs.hunter_skillcape))
            assertEquals(0, player.inv.count(objs.hunter_hood))
        }

    @Test
    fun GameTestState.`Skillcape sale requires 99000 inventory coins`() =
        runHunterExpertTest { deps ->
            player.setBaseLevel(org.rsmod.api.config.refs.stats.hunter, 99)
            player.inv[0] = InvObj(objs.coins, 98_999)

            var result: HunterSkillcapePurchaseResult? = null
            player.withProtectedAccess { result = deps.skillcapes.purchase(this) }

            assertEquals(HunterSkillcapePurchaseResult.InsufficientCoins, result)
            assertEquals(98_999, player.inv.count(objs.coins))
        }

    @Test
    fun GameTestState.`first mastery purchases untrimmed cape and free hood`() =
        runHunterExpertTest { deps ->
            player.setBaseLevel(org.rsmod.api.config.refs.stats.hunter, 99)
            player.inv[0] = InvObj(objs.coins, 100_000)

            var result: HunterSkillcapePurchaseResult? = null
            player.withProtectedAccess { result = deps.skillcapes.purchase(this) }

            assertEquals(HunterSkillcapePurchaseResult.Purchased, result)
            assertEquals(1_000, player.inv.count(objs.coins))
            assertEquals(1, player.inv.count(objs.hunter_skillcape))
            assertEquals(0, player.inv.count(objs.hunter_skillcape_t))
            assertEquals(1, player.inv.count(objs.hunter_hood))
            assertEquals(231, client.filterIsInstance<MidiJingle>().single().id)
        }

    @Test
    fun GameTestState.`second mastery purchases trimmed Hunter cape`() =
        runHunterExpertTest { deps ->
            player.setBaseLevel(org.rsmod.api.config.refs.stats.hunter, 99)
            player.setBaseLevel(org.rsmod.api.config.refs.stats.fishing, 99)
            player.inv[0] = InvObj(objs.coins, 99_000)

            var result: HunterSkillcapePurchaseResult? = null
            player.withProtectedAccess { result = deps.skillcapes.purchase(this) }

            assertEquals(HunterSkillcapePurchaseResult.Purchased, result)
            assertEquals(0, player.inv.count(objs.coins))
            assertEquals(0, player.inv.count(objs.hunter_skillcape))
            assertEquals(1, player.inv.count(objs.hunter_skillcape_t))
            assertEquals(1, player.inv.count(objs.hunter_hood))
            assertEquals(231, client.filterIsInstance<MidiJingle>().single().id)
        }

    @Test
    fun GameTestState.`Skillcape capacity rejection preserves coins and all inventory`() =
        runHunterExpertTest { deps ->
            player.setBaseLevel(org.rsmod.api.config.refs.stats.hunter, 99)
            player.fillInv()
            player.inv[0] = InvObj(objs.coins, 100_000)
            val before = player.inv.toList()

            var result: HunterSkillcapePurchaseResult? = null
            player.withProtectedAccess { result = deps.skillcapes.purchase(this) }

            assertEquals(HunterSkillcapePurchaseResult.NoInventorySpace, result)
            assertEquals(before, player.inv.toList())
            assertTrue(client.filterIsInstance<MidiJingle>().isEmpty())

            player.inv[1] = null
            player.inv[2] = null
            player.withProtectedAccess { result = deps.skillcapes.purchase(this) }
            assertEquals(HunterSkillcapePurchaseResult.Purchased, result)
            assertEquals(1_000, player.inv.count(objs.coins))
            assertEquals(1, player.inv.count(objs.hunter_skillcape))
            assertEquals(1, player.inv.count(objs.hunter_hood))
            assertEquals(231, client.filterIsInstance<MidiJingle>().single().id)
        }

    @Test
    fun GameTestState.`Feldip non-master Talk-to exposes cutoff opening and top menu`() =
        runHunterExpertTest { deps ->
            val npc =
                spawnNpc(player.coords.translate(1, 0), deps.npcs[npcs.hunting_jungle_information])

            player.opNpc1(npc)
            advance()
            assertDialogueText(
                "Whoah there, stranger, careful where you're walking. I almost clobbered you " +
                    "there; thought you were a larupia or something."
            )
            resumeNpc()
            assertMenu(
                "Select an option",
                "What are you doing here?",
                "What's a larupia?",
                "What is that cool cape you're wearing?",
                "Ahh, leave me alone you crazy killing person.",
                "Can you give me some hunting traps?",
            )
            choose(4)
            assertDialogueText("Ahh, leave me alone you crazy killing person.")
            resumePlayer()
            assertDialogueText("Heh, well I guess it's not everyone's thing.")
        }

    @Test
    fun GameTestState.`Rellekka Talk-to exposes cutoff opening and top menu`() =
        runHunterExpertTest { deps ->
            val npc =
                spawnNpc(player.coords.translate(1, 0), deps.npcs[npcs.hunting_polar_information])

            player.opNpc1(npc)
            advance()
            assertDialogueText(
                "Ah, this is a wonderful area for a spot of hunting, don't you think?"
            )
            resumeNpc()
            assertMenu(
                "Select an option",
                "I don't know, is it?",
                "Can you teach me how to hunt?",
                "Sorry, I'm not that keen on hunting.",
                "Can you give me some hunting traps?",
            )
            choose(3)
            assertDialogueText("Sorry, I'm not that keen on hunting.")
            resumePlayer()
            assertDialogueText(
                "Really? What a shame, you looked like someone who could have had some promise."
            )
        }

    @Test
    fun GameTestState.`level 99 Feldip dialogue sells cape and hood through master route`() =
        runHunterExpertTest { deps ->
            player.setBaseLevel(stats.hunter, 99)
            player.inv[0] = InvObj(objs.coins, 100_000)
            val npc =
                spawnNpc(player.coords.translate(1, 0), deps.npcs[npcs.hunting_jungle_information])

            player.opNpc1(npc)
            advance()
            assertMenu("What would you like to talk about?", "Skillcape of Hunter.", "Hunting.")
            choose(1)
            assertDialogueText("Can I buy a Hunter's Skillcape from you?")
            resumePlayer()
            resumeNpc()
            resumeNpc()
            assertMenu(
                "Select an option",
                "99000! That seems a bit expensive.",
                "Certainly, I'm happy to pay that.",
            )
            choose(2)
            resumePlayer()

            assertEquals(1_000, player.inv.count(objs.coins))
            assertEquals(1, player.inv.count(objs.hunter_skillcape))
            assertEquals(1, player.inv.count(objs.hunter_hood))
            assertEquals(231, client.filterIsInstance<MidiJingle>().single().id)
            assertDialogueText("Now you look like a true hunter!")
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

    private fun GameTestState.runHunterExpertTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, null, HunterExpertScript::class, testBody = body)

    data class Dependencies
    @Inject
    constructor(
        val grant: HunterExpertKitGrant,
        val skillcapes: HunterExpertSkillcapePurchase,
        val events: EventBus,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val varps: VarpTypeList,
    )
}
