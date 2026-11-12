package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ui.IfOverlayButton
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
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class LunarGroupTeleportSpellsTest {
    @Test
    fun GameTestState.`cache contracts define all seven Lunar group teleports`() =
        runGroupTeleportTest { deps ->
            for (expected in expectations) {
                val spell = requireNotNull(deps.spells.getObjSpell(expected.spell))
                assertEquals(expected.level, spell.levelReq, expected.spell.toString())
                assertEquals(expected.xp, spell.castXp, expected.spell.toString())
                assertEquals(Spellbook.Lunars, spell.spellbook)
                assertEquals(MagicSpellType.Teleport, spell.type)
                assertEquals(expected.runes, spell.objReqs.associate { it.obj to it.count })
                assertEquals(expected.component, spell.component.id and 0xFFFF)
                assertNull(deps.objTypes[expected.spell].paramOrNull(params.spell_telecoord))
            }
            assertEquals(326, interfaces.teleport_other.id)
            assertEquals(4423, seqs.lunar_teleport.id)
            assertEquals(747, spotanims.lunar_teleport_spotanim.id)
            assertEquals(200, synths.teleport_all.id)
            assertEquals(4180, varbits.option_acceptaid.id)
        }

    @Test
    fun GameTestState.`all seven casts consume exact costs and teleport caster after four ticks`() =
        runGroupTeleportTest { deps ->
            for (expected in expectations) {
                prepareCaster()
                addRunes(deps, expected.spell)
                val origin = player.coords
                val startXp = player.statMap.getFineXP(stats.magic)
                random.next = 0

                cast(deps, expected.definition, confirmIcePlateau = true)

                assertEquals(0, runeCount(deps, expected.spell))
                assertEquals(
                    (expected.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.magic) - startXp,
                )
                assertEquals(seqs.lunar_teleport.id, player.pendingSequence.id)
                assertEquals(
                    spotanims.lunar_teleport_spotanim.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                assertEquals(origin, player.coords)
                advance(3)
                assertEquals(origin, player.coords)
                advance()
                assertTrue(expected.definition.accepts(player.coords), player.coords.toString())
            }
        }

    @Test
    fun GameTestState.`destination areas match cache-cutoff Wiki polygons`() =
        runGroupTeleportTest {
            assertEquals(5, LunarGroupTeleportDefinition.Moonclan.destinationCount)
            assertTrue(LunarGroupTeleportDefinition.Moonclan.accepts(CoordGrid(2107, 3915)))
            assertTrue(LunarGroupTeleportDefinition.Moonclan.accepts(CoordGrid(2111, 3915)))
            assertFalse(LunarGroupTeleportDefinition.Moonclan.accepts(CoordGrid(2112, 3915)))
            assertFalse(LunarGroupTeleportDefinition.Moonclan.accepts(CoordGrid(2107, 3916)))

            assertEquals(25, LunarGroupTeleportDefinition.Waterbirth.destinationCount)
            assertTrue(LunarGroupTeleportDefinition.Waterbirth.accepts(CoordGrid(2544, 3753)))
            assertTrue(LunarGroupTeleportDefinition.Waterbirth.accepts(CoordGrid(2548, 3757)))
            assertFalse(LunarGroupTeleportDefinition.Waterbirth.accepts(CoordGrid(2549, 3757)))

            assertEquals(23, LunarGroupTeleportDefinition.Catherby.destinationCount)
            assertTrue(LunarGroupTeleportDefinition.Catherby.accepts(CoordGrid(2799, 3448)))
            assertTrue(LunarGroupTeleportDefinition.Catherby.accepts(CoordGrid(2803, 3451)))
            assertTrue(LunarGroupTeleportDefinition.Catherby.accepts(CoordGrid(2801, 3447)))
            assertFalse(LunarGroupTeleportDefinition.Catherby.accepts(CoordGrid(2800, 3447)))
            assertFalse(LunarGroupTeleportDefinition.Catherby.accepts(CoordGrid(2804, 3451)))

            assertEquals(49, LunarGroupTeleportDefinition.IcePlateau.destinationCount)
            assertTrue(LunarGroupTeleportDefinition.IcePlateau.accepts(CoordGrid(2972, 3935)))
            assertTrue(LunarGroupTeleportDefinition.IcePlateau.accepts(CoordGrid(2978, 3941)))
            assertFalse(LunarGroupTeleportDefinition.IcePlateau.accepts(CoordGrid(2979, 3941)))
        }

    @Test
    fun GameTestState.`same-plane radius-one Accept Aid recipients receive independent offers`() =
        runGroupTeleportTest { deps ->
            prepareCaster()
            player.displayName = "Caster"
            val sameTile = registerRecipient("Same tile", player.coords)
            val diagonal = registerRecipient("Diagonal", player.coords.translate(1, 1))
            val noAid = registerRecipient("No aid", player.coords.translate(-1, 0), false)
            val tooFar = registerRecipient("Too far", player.coords.translate(2, 0))
            val otherPlane =
                registerRecipient(
                    "Other plane",
                    CoordGrid(player.coords.x, player.coords.z, player.coords.level + 1),
                )
            addRunes(deps, expectations.first().spell)

            cast(deps, LunarGroupTeleportDefinition.Moonclan)

            for (recipient in listOf(sameTile, diagonal)) {
                assertEquals(
                    LunarGroupTeleportDefinition.Moonclan,
                    deps.offers.get(recipient)?.definition,
                )
                assertModalOpen(interfaces.teleport_other, recipient)
            }
            for (recipient in listOf(noAid, tooFar, otherPlane)) {
                assertNull(deps.offers.get(recipient))
                assertModalNotOpen(interfaces.teleport_other, recipient)
            }
        }

    @Test
    fun GameTestState.`accept teleports recipient independently after Lunar animation`() =
        runGroupTeleportTest { deps ->
            prepareCaster()
            val recipient = registerRecipient("Recipient", player.coords.translate(1, 0))
            val origin = recipient.coords
            addRunes(deps, LunarGroupTeleportDefinition.Catherby.spell)
            random.next = 0

            cast(deps, LunarGroupTeleportDefinition.Catherby)
            respond(recipient, components.teleport_other_accept)

            assertModalNotOpen(interfaces.teleport_other, recipient)
            assertNull(deps.offers.get(recipient))
            assertEquals(seqs.lunar_teleport.id, recipient.pendingSequence.id)
            assertEquals(
                spotanims.lunar_teleport_spotanim.id,
                EntitySpotanim(recipient.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(origin, recipient.coords)
            advance(3)
            assertEquals(origin, recipient.coords)
            advance()
            assertTrue(LunarGroupTeleportDefinition.Catherby.accepts(recipient.coords))
            assertTrue(LunarGroupTeleportDefinition.Catherby.accepts(player.coords))
        }

    @Test
    fun GameTestState.`decline close and logout clear offers without affecting caster cost`() =
        runGroupTeleportTest { deps ->
            prepareCaster()
            val recipient = registerRecipient("Recipient", player.coords.translate(1, 0))
            val definition = LunarGroupTeleportDefinition.Waterbirth
            addRunes(deps, definition.spell)

            cast(deps, definition)
            respond(recipient, components.teleport_other_decline)
            assertNull(deps.offers.get(recipient))
            assertModalNotOpen(interfaces.teleport_other, recipient)
            assertEquals(0, runeCount(deps, definition.spell))

            prepareCaster()
            addRunes(deps, definition.spell)
            cast(deps, definition)
            recipient.ifCloseSub(interfaces.teleport_other, deps.events)
            assertNull(deps.offers.get(recipient))

            prepareCaster()
            addRunes(deps, definition.spell)
            cast(deps, definition)
            deps.events.publish(SessionStateEvent.Logout(recipient))
            assertNull(deps.offers.get(recipient))
        }

    @Test
    fun GameTestState.`members quest spellbook level and rune failures are atomic`() =
        runGroupTeleportTest { deps ->
            val expected = expectations.first()
            prepareCaster()
            addRunes(deps, expected.spell)

            player.members = false
            assertRejected(deps, expected, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            assertRejected(
                deps,
                expected,
                "You need to complete Lunar Diplomacy to cast this spell.",
            )
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            assertRejected(deps, expected)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = expected.level - 1
            assertRejected(deps, expected, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.clearInv()
            assertRejected(deps, expected, "You do not have enough Law Runes to cast this spell.")
        }

    @Test
    fun GameTestState.`caster and accepted recipient obey Tele Block and Wilderness limits`() =
        runGroupTeleportTest { deps ->
            val expected = expectations.first()
            prepareCaster()
            addRunes(deps, expected.spell)
            player.setVarBit(varbits.teleblock_cycles, 101)
            assertRejected(deps, expected, "A magical force stops you from teleporting.")

            prepareCaster()
            addRunes(deps, expected.spell)
            player.teleport(CoordGrid(0, 50, 57, 0, 32))
            player.setVarBit(varbits.inside_wilderness, 1)
            assertRejected(deps, expected, "A mysterious force prevents you from teleporting!")

            prepareCaster()
            val blocked = registerRecipient("Blocked", player.coords.translate(1, 0))
            addRunes(deps, expected.spell)
            cast(deps, expected.definition)
            blocked.setVarBit(varbits.teleblock_cycles, 101)
            respond(blocked, components.teleport_other_accept)
            assertMessageSent("A magical force stops you from teleporting.", blocked)
            advance(4)

            prepareCaster()
            val wilderness = registerRecipient("Wilderness", player.coords.translate(1, 0))
            addRunes(deps, expected.spell)
            cast(deps, expected.definition)
            wilderness.teleport(CoordGrid(0, 50, 57, 0, 32))
            wilderness.setVarBit(varbits.inside_wilderness, 1)
            respond(wilderness, components.teleport_other_accept)
            assertMessageSent("A mysterious force prevents you from teleporting!", wilderness)
        }

    @Test
    fun GameTestState
        .`Ice Plateau warning cancellation is atomic and acceptance names destination`() =
        runGroupTeleportTest { deps ->
            val expected = expectations.last()
            prepareCaster()
            val recipient = registerRecipient("Recipient", player.coords.translate(1, 0))
            addRunes(deps, expected.spell)
            val startXp = player.statMap.getFineXP(stats.magic)

            cast(deps, expected.definition, confirmIcePlateau = false)

            assertEquals(expected.runes.values.sum(), runeCount(deps, expected.spell))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
            assertNull(deps.offers.get(recipient))
            player.ifCloseSub(interfaces.menu, deps.events)
            advance()

            cast(deps, expected.definition, confirmIcePlateau = true)
            assertModalOpen(interfaces.teleport_other, recipient)
            respond(recipient, components.teleport_other_accept)
            advance(4)
            assertTrue(expected.definition.accepts(player.coords))
            assertTrue(expected.definition.accepts(recipient.coords))
        }

    @Test
    fun GameTestState.`all group spellbook buttons dispatch and non-cast ops no-op`() =
        runGroupTeleportTest { deps ->
            for (expected in expectations) {
                prepareCaster()
                addRunes(deps, expected.spell)
                deps.events.publish(
                    IfOverlayButton(
                        player = player,
                        component = expected.definition.component,
                        comsub = -1,
                        obj = null,
                        op = IfButtonOp.Op1,
                    )
                )
                if (expected.definition == LunarGroupTeleportDefinition.IcePlateau) {
                    resumeMenu(0)
                }
                advance(4)
                assertTrue(expected.definition.accepts(player.coords), expected.toString())
            }

            val expected = expectations.first()
            prepareCaster()
            addRunes(deps, expected.spell)
            deps.events.publish(
                IfOverlayButton(
                    player = player,
                    component = expected.definition.component,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op2,
                )
            )
            advance(4)
            assertEquals(ORIGIN, player.coords)
            assertEquals(expected.runes.values.sum(), runeCount(deps, expected.spell))
        }

    @Test
    fun GameTestState.`staff substitution and Fountain behavior use shared rune policy`() =
        runGroupTeleportTest { deps ->
            val expected = expectations.first()
            prepareCaster()
            player.righthand = InvObj(objs.earth_battlestaff)
            player.inv[0] = InvObj(objs.astral_rune, 2)
            player.inv[1] = InvObj(objs.law_rune)
            cast(deps, expected.definition)
            advance(4)
            assertTrue(expected.definition.accepts(player.coords))

            prepareCaster()
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)
            cast(deps, expected.definition)
            advance(4)
            assertTrue(expected.definition.accepts(player.coords))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.prepareCaster() {
        player.members = true
        player.displayName = "Caster"
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.teleblock_cycles, 0)
        player.clearInv()
        player.righthand = null
        player.teleport(ORIGIN)
        client.clear()
    }

    private fun GameTestScope.registerRecipient(
        name: String,
        coords: CoordGrid,
        acceptAid: Boolean = true,
    ): Player =
        registerPlayer(coords = coords).also {
            it.members = true
            it.displayName = name
            it.setVarBit(varbits.option_acceptaid, if (acceptAid) 1 else 0)
            it.setVarBit(varbits.inside_wilderness, 0)
            it.setVarBit(varbits.teleblock_cycles, 0)
        }

    private fun GameTestScope.cast(
        deps: Dependencies,
        definition: LunarGroupTeleportDefinition,
        confirmIcePlateau: Boolean? = null,
    ) {
        player.withProtectedAccess { deps.teleports.cast(this, definition) }
        if (definition == LunarGroupTeleportDefinition.IcePlateau) {
            resumeMenu(if (confirmIcePlateau == true) 0 else 1)
        }
    }

    private fun GameTestScope.respond(
        target: Player,
        component: org.rsmod.game.type.comp.ComponentType,
    ) {
        target.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = -1))
    }

    private fun GameTestScope.resumeMenu(selection: Int) {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.menu_list, subcomponent = selection)
        )
    }

    private fun GameTestScope.addRunes(deps: Dependencies, spellType: ObjType) {
        val spell = requireNotNull(deps.spells.getObjSpell(spellType))
        spell.objReqs.forEachIndexed { slot, req -> player.inv[slot] = InvObj(req.obj, req.count) }
    }

    private fun GameTestScope.runeCount(deps: Dependencies, spellType: ObjType): Int {
        val spell = requireNotNull(deps.spells.getObjSpell(spellType))
        return spell.objReqs.sumOf { player.count(it.obj) }
    }

    private fun GameTestScope.assertRejected(
        deps: Dependencies,
        expected: Expectation,
        message: String? = null,
    ) {
        val origin = player.coords
        val runes = runeCount(deps, expected.spell)
        val startXp = player.statMap.getFineXP(stats.magic)
        cast(deps, expected.definition)
        assertEquals(origin, player.coords)
        assertEquals(runes, runeCount(deps, expected.spell))
        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        if (message != null) {
            assertMessageSent(message)
        }
        client.clear()
    }

    private fun GameTestState.runGroupTeleportTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            LunarGroupTeleportSpellScript::class,
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
        val teleports: LunarGroupTeleportSpells,
        val offers: LunarGroupTeleportOfferRepository,
        val spells: MagicSpellRegistry,
        val objTypes: ObjTypeList,
        val events: EventBus,
    )

    private data class Expectation(
        val definition: LunarGroupTeleportDefinition,
        val level: Int,
        val xp: Double,
        val component: Int,
        val runes: Map<ObjType, Int>,
    ) {
        val spell: ObjType
            get() = definition.spell
    }

    private companion object {
        val ORIGIN = CoordGrid(0, 50, 50, 0, 0)

        val expectations =
            listOf(
                Expectation(
                    LunarGroupTeleportDefinition.Moonclan,
                    70,
                    67.0,
                    112,
                    mapOf(objs.law_rune to 1, objs.astral_rune to 2, objs.earth_rune to 4),
                ),
                Expectation(
                    LunarGroupTeleportDefinition.Waterbirth,
                    73,
                    72.0,
                    116,
                    mapOf(objs.law_rune to 1, objs.astral_rune to 2, objs.water_rune to 5),
                ),
                Expectation(
                    LunarGroupTeleportDefinition.Barbarian,
                    76,
                    77.0,
                    120,
                    mapOf(objs.law_rune to 2, objs.astral_rune to 2, objs.fire_rune to 6),
                ),
                Expectation(
                    LunarGroupTeleportDefinition.Khazard,
                    79,
                    81.0,
                    124,
                    mapOf(objs.law_rune to 2, objs.astral_rune to 2, objs.water_rune to 8),
                ),
                Expectation(
                    LunarGroupTeleportDefinition.FishingGuild,
                    86,
                    90.0,
                    132,
                    mapOf(objs.law_rune to 3, objs.astral_rune to 3, objs.water_rune to 14),
                ),
                Expectation(
                    LunarGroupTeleportDefinition.Catherby,
                    88,
                    93.0,
                    135,
                    mapOf(objs.law_rune to 3, objs.astral_rune to 3, objs.water_rune to 15),
                ),
                Expectation(
                    LunarGroupTeleportDefinition.IcePlateau,
                    90,
                    99.0,
                    138,
                    mapOf(objs.law_rune to 3, objs.astral_rune to 3, objs.water_rune to 16),
                ),
            )
    }
}
