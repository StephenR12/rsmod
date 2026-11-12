package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.PlayerTEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.runes.combo.ComboRuneRepository
import org.rsmod.api.spells.runes.combo.ComboRuneScript
import org.rsmod.api.spells.runes.compact.CompactRuneRepository
import org.rsmod.api.spells.runes.compact.CompactRuneScript
import org.rsmod.api.spells.runes.fake.FakeRuneRepository
import org.rsmod.api.spells.runes.fake.FakeRuneScript
import org.rsmod.api.spells.runes.staves.StaffSubstituteRepository
import org.rsmod.api.spells.runes.staves.StaffSubstituteScript
import org.rsmod.api.spells.runes.subs.RuneSubstituteRepository
import org.rsmod.api.spells.runes.subs.RuneSubstituteScript
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneRepository
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneScript
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class TeleotherSpellsTest {
    @Test
    fun GameTestState.`cache contracts define all Teleother spells and native assets`() =
        runTeleotherTest { deps ->
            for (expected in expectations) {
                val spell = requireNotNull(deps.spells.getObjSpell(expected.spell))
                assertEquals(expected.level, spell.levelReq)
                assertEquals(expected.xp, spell.castXp)
                assertEquals(Spellbook.Standard, spell.spellbook)
                assertEquals(MagicSpellType.Teleport, spell.type)
                assertEquals(expected.runes, spell.objReqs.associate { it.obj to it.count })
                assertEquals(expected.component, spell.component.id and 0xFFFF)
            }
            assertEquals(326, interfaces.teleport_other.id)
            assertEquals(89, components.teleport_other_caster_name.component)
            assertEquals(91, components.teleport_other_location.component)
            assertEquals(97, components.teleport_other_accept.component)
            assertEquals(99, components.teleport_other_decline.component)
            assertEquals(1816, seqs.teleport_other_impact.id)
            assertEquals(1818, seqs.teleport_other_casting.id)
            assertEquals(342, spotanims.teleport_other_impact.id)
            assertEquals(343, spotanims.teleport_other_casting.id)
            assertEquals(199, synths.tele_other_cast.id)
            assertTrue(deps.seqTypes[seqs.teleport_other_impact].tickDuration > 0)
            for (component in
                listOf(components.teleport_other_accept, components.teleport_other_decline)) {
                val type = deps.componentTypes[component]
                assertTrue(
                    type.hasEvent(IfEvent.PauseButton),
                    "component=$component events=${type.events} buttonType=${type.buttonType} " +
                        "buttonText=${type.buttonText} ops=${type.op.contentToString()} " +
                        "onClick=${type.onClick?.contentToString()}",
                )
            }
        }

    @Test
    fun GameTestState.`cast consumes exact runes and xp while opening recipient offer`() =
        runTeleotherTest { deps ->
            for (expected in expectations) {
                val target = preparePlayers()
                addRunes(deps, expected.spell)
                val startXp = player.statMap.getFineXP(stats.magic)

                player.withProtectedAccess { deps.teleother.cast(this, target, expected.spell) }

                assertEquals(0, runeCount(deps, expected.spell))
                assertEquals(
                    (expected.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.magic) - startXp,
                )
                assertEquals(seqs.teleport_other_casting.id, player.pendingSequence.id)
                assertEquals(
                    spotanims.teleport_other_casting.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                assertModalOpen(interfaces.teleport_other, target)
                assertEquals(expected.definition, deps.offers.get(target)?.definition)

                deps.teleother.clear(target)
                target.ifCloseSub(interfaces.teleport_other, deps.events)
                advance(10)
            }
        }

    @Test
    fun GameTestState.`Lumbridge landing tiles match the Wiki polygon boundaries`() =
        runTeleotherTest {
            val definition = TeleotherDefinition.Lumbridge
            val expected = buildSet {
                for (x in 3219 until 3224) {
                    for (z in 3218 until 3220) {
                        add(CoordGrid(x, z))
                    }
                }
                for (x in 3221 until 3223) {
                    for (z in 3216 until 3218) {
                        add(CoordGrid(x, z))
                    }
                    add(CoordGrid(x, 3220))
                }
            }
            assertEquals(16, expected.size)
            assertTrue(expected.all(definition::accepts))
            assertFalse(definition.accepts(CoordGrid(3224, 3218)))
            assertFalse(definition.accepts(CoordGrid(3219, 3220)))
            assertFalse(definition.accepts(CoordGrid(3223, 3220)))
            assertFalse(definition.accepts(CoordGrid(3222, 3221)))
        }

    @Test
    fun GameTestState.`accept uses cache impact duration and Wiki destination area`() =
        runTeleotherTest { deps ->
            val expected = expectations.first()
            val target = preparePlayers()
            val origin = target.coords
            addRunes(deps, expected.spell)
            random.next = 0

            player.withProtectedAccess { deps.teleother.cast(this, target, expected.spell) }
            deps.respond(target, components.teleport_other_accept)

            assertModalNotOpen(interfaces.teleport_other, target)
            assertNull(deps.offers.get(target))
            assertEquals(seqs.teleport_other_impact.id, target.pendingSequence.id)
            assertEquals(
                spotanims.teleport_other_impact.id,
                EntitySpotanim(target.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(origin, target.coords)

            val impactTicks = deps.seqTypes[seqs.teleport_other_impact].tickDuration
            advance(impactTicks - 1)
            assertEquals(origin, target.coords)
            advance()
            assertTrue(expected.definition.accepts(target.coords), target.coords.toString())
        }

    @Test
    fun GameTestState.`decline close and logout clear offers without refunding the cast`() =
        runTeleotherTest { deps ->
            val expected = expectations[1]
            val target = preparePlayers()
            val origin = target.coords
            addRunes(deps, expected.spell)

            player.withProtectedAccess { deps.teleother.cast(this, target, expected.spell) }
            deps.respond(target, components.teleport_other_decline)
            advance()

            assertModalNotOpen(interfaces.teleport_other, target)
            assertNull(deps.offers.get(target))
            assertEquals(origin, target.coords)
            assertEquals(0, runeCount(deps, expected.spell))

            advance(10)
            addRunes(deps, expected.spell)
            player.withProtectedAccess { deps.teleother.cast(this, target, expected.spell) }
            target.ifCloseSub(interfaces.teleport_other, deps.events)
            assertNull(deps.offers.get(target))
            assertEquals(origin, target.coords)

            advance(10)
            addRunes(deps, expected.spell)
            player.withProtectedAccess { deps.teleother.cast(this, target, expected.spell) }
            deps.events.publish(SessionStateEvent.Logout(target))
            assertNull(deps.offers.get(target))
            assertEquals(origin, target.coords)
        }

    @Test
    fun GameTestState.`Accept Aid off rejects atomically`() = runTeleotherTest { deps ->
        val expected = expectations.first()
        val target = preparePlayers(acceptAid = false)
        addRunes(deps, expected.spell)
        val runesBefore = runeCount(deps, expected.spell)
        val startXp = player.statMap.getFineXP(stats.magic)

        player.withProtectedAccess {
            deps.teleother.cast(this, target, expected.spell)
            assertMessageSent("Recipient is not accepting aid.")
        }

        assertEquals(runesBefore, runeCount(deps, expected.spell))
        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        assertModalNotOpen(interfaces.teleport_other, target)
        assertNull(deps.offers.get(target))
    }

    @Test
    fun GameTestState.`member level rune and busy-recipient failures are atomic`() =
        runTeleotherTest { deps ->
            val expected = expectations.last()
            val target = preparePlayers()
            addRunes(deps, expected.spell)

            player.members = false
            assertRejected(
                deps,
                target,
                expected,
                "You need to be on a members' world to cast this spell.",
            )

            player.members = true
            player.stats[stats.magic] = expected.level - 1
            assertRejected(
                deps,
                target,
                expected,
                "Your Magic level is not high enough for this spell.",
            )

            player.stats[stats.magic] = expected.level
            val lawSlot = player.inv.indexOfFirst { it?.id == objs.law_rune.id }
            player.inv[lawSlot] = null
            assertRejected(
                deps,
                target,
                expected,
                "You do not have enough Law Runes to cast this spell.",
            )

            player.inv[lawSlot] = InvObj(objs.law_rune)
            target.withProtectedAccess { ifOpenMainModal(interfaces.settings) }
            assertRejected(deps, target, expected, "The other player is busy.")
        }

    @Test
    fun GameTestState.`accepted target still obeys Tele Block and deep Wilderness limits`() =
        runTeleotherTest { deps ->
            val expected = expectations.first()
            val target = preparePlayers()
            val origin = target.coords
            addRunes(deps, expected.spell)

            player.withProtectedAccess { deps.teleother.cast(this, target, expected.spell) }
            target.setVarBit(varbits.teleblock_cycles, 101)
            deps.respond(target, components.teleport_other_accept)
            assertMessageSent("A magical force stops you from teleporting.", target)
            assertEquals(origin, target.coords)

            advance(10)
            target.setVarBit(varbits.teleblock_cycles, 0)
            target.placeAt(CoordGrid(0, 50, 57, 0, 32))
            target.setVarBit(varbits.inside_wilderness, 1)
            addRunes(deps, expected.spell)
            player.withProtectedAccess { deps.teleother.cast(this, target, expected.spell) }
            deps.respond(target, components.teleport_other_accept)
            assertMessageSent("A mysterious force prevents you from teleporting!", target)
            assertEquals(CoordGrid(0, 50, 57, 0, 32), target.coords)
        }

    @Test
    fun GameTestState.`ten-tick cast rate prevents duplicate offers and costs`() =
        runTeleotherTest { deps ->
            val expected = expectations.first()
            val firstTarget = preparePlayers()
            val secondTarget = registerRecipient("Second recipient")
            addRunes(deps, expected.spell, multiplier = 2)

            player.withProtectedAccess { deps.teleother.cast(this, firstTarget, expected.spell) }
            val afterFirst = runeCount(deps, expected.spell)
            player.withProtectedAccess { deps.teleother.cast(this, secondTarget, expected.spell) }

            assertEquals(afterFirst, runeCount(deps, expected.spell))
            assertNull(deps.offers.get(secondTarget))
            advance(9)
            player.withProtectedAccess { deps.teleother.cast(this, secondTarget, expected.spell) }
            assertEquals(afterFirst, runeCount(deps, expected.spell))
            advance()
            player.withProtectedAccess { deps.teleother.cast(this, secondTarget, expected.spell) }
            assertNotNull(deps.offers.get(secondTarget))
            assertEquals(0, runeCount(deps, expected.spell))
        }

    @Test
    fun GameTestState.`player-target spell event dispatches through the plugin`() =
        runTeleotherTest { deps ->
            val expected = expectations[2]
            val target = preparePlayers()
            addRunes(deps, expected.spell)

            player.withProtectedAccess {
                deps.events.publish(
                    this,
                    PlayerTEvents.Ap(
                        target,
                        comsub = -1,
                        objType = null,
                        expected.definition.component,
                    ),
                )
            }

            assertModalOpen(interfaces.teleport_other, target)
            assertEquals(expected.definition, deps.offers.get(target)?.definition)
            assertEquals(0, runeCount(deps, expected.spell))
        }

    private fun GameTestScope.preparePlayers(acceptAid: Boolean = true): Player {
        player.members = true
        player.displayName = "Caster"
        player.stats[stats.magic] = 99
        player.clearInv()
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        return registerRecipient("Recipient", acceptAid)
    }

    private fun GameTestScope.registerRecipient(name: String, acceptAid: Boolean = true): Player =
        registerPlayer(coords = player.coords.translate(1, 0)).also {
            it.members = true
            it.displayName = name
            it.setVarBit(varbits.option_acceptaid, if (acceptAid) 1 else 0)
            it.setVarBit(varbits.teleblock_cycles, 0)
            it.setVarBit(varbits.inside_wilderness, 0)
        }

    private fun GameTestScope.addRunes(
        deps: Dependencies,
        spellType: ObjType,
        multiplier: Int = 1,
    ) {
        val spell = requireNotNull(deps.spells.getObjSpell(spellType))
        spell.objReqs.forEachIndexed { slot, req ->
            player.inv[slot] = InvObj(req.obj, req.count * multiplier)
        }
    }

    private fun GameTestScope.runeCount(deps: Dependencies, spellType: ObjType): Int {
        val spell = requireNotNull(deps.spells.getObjSpell(spellType))
        return spell.objReqs.sumOf { player.count(it.obj) }
    }

    private fun GameTestScope.assertRejected(
        deps: Dependencies,
        target: Player,
        expected: Expectation,
        message: String,
    ) {
        val runesBefore = runeCount(deps, expected.spell)
        val startXp = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess {
            deps.teleother.cast(this, target, expected.spell)
            assertMessageSent(message)
        }
        assertEquals(runesBefore, runeCount(deps, expected.spell))
        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        assertNull(deps.offers.get(target))
    }

    private fun GameTestState.runTeleotherTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            TeleotherSpellScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val teleother: TeleotherSpells,
        val offers: TeleotherOfferRepository,
        val spells: MagicSpellRegistry,
        val events: EventBus,
        val seqTypes: SeqTypeList,
        val componentTypes: ComponentTypeList,
    )

    private fun Dependencies.respond(
        target: Player,
        component: org.rsmod.game.type.comp.ComponentType,
    ) {
        target.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = -1))
    }

    private data class Expectation(
        val definition: TeleotherDefinition,
        val level: Int,
        val xp: Double,
        val component: Int,
        val runes: Map<ObjType, Int>,
    ) {
        val spell: ObjType
            get() = definition.spell
    }

    private companion object {
        val expectations =
            listOf(
                Expectation(
                    TeleotherDefinition.Lumbridge,
                    74,
                    84.0,
                    64,
                    mapOf(objs.soul_rune to 1, objs.earth_rune to 1, objs.law_rune to 1),
                ),
                Expectation(
                    TeleotherDefinition.Falador,
                    82,
                    92.0,
                    70,
                    mapOf(objs.soul_rune to 1, objs.water_rune to 1, objs.law_rune to 1),
                ),
                Expectation(
                    TeleotherDefinition.Camelot,
                    90,
                    100.0,
                    75,
                    mapOf(objs.soul_rune to 2, objs.law_rune to 1),
                ),
            )
    }
}
