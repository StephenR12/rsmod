package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.components
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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class LunarTeleportSpellsTest {
    @Test
    fun GameTestState.`cache contracts define all eight Lunar self teleports`() =
        runTeleportTest { deps ->
            for (expected in expectations) {
                val spell = requireNotNull(deps.spells.getObjSpell(expected.spell))
                assertEquals(expected.level, spell.levelReq, expected.spell.toString())
                assertEquals(expected.xp, spell.castXp, expected.spell.toString())
                assertEquals(Spellbook.Lunars, spell.spellbook)
                assertEquals(MagicSpellType.Teleport, spell.type)
                assertEquals(expected.runes, spell.objReqs.associate { it.obj to it.count })
                assertEquals(expected.component.id and 0xFFFF, spell.component.id and 0xFFFF)
                assertEquals(
                    expected.destination,
                    deps.objTypes[expected.spell].param(params.spell_telecoord),
                )
            }
            assertEquals(4423, seqs.lunar_teleport.id)
            assertEquals(747, spotanims.lunar_teleport_spotanim.id)
            assertEquals(200, synths.teleport_all.id)
            assertEquals(5376, varbits.rc_zmi_lunar_spell.id)
        }

    @Test
    fun GameTestState.`all eight teleports consume exact costs and use four-tick cast delay`() =
        runTeleportTest { deps ->
            for (expected in expectations) {
                prepareCaster()
                addRunes(deps, expected.spell)
                val origin = player.coords
                val startXp = player.statMap.getFineXP(stats.magic)

                cast(deps, expected.spell, confirmIcePlateau = true)

                assertEquals(0, spellRuneCount(deps, expected.spell))
                assertEquals(
                    (expected.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.magic) - startXp,
                )
                assertEquals(seqs.lunar_teleport.id, player.pendingSequence.id)
                assertEquals(
                    spotanims.lunar_teleport_spotanim.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                assertEquals(200, client.filterIsInstance<SynthSound>().single().id)
                assertEquals(origin, player.coords)
                advance(3)
                assertEquals(origin, player.coords)
                advance()
                assertEquals(expected.destination, player.coords)
            }
        }

    @Test
    fun GameTestState.`members quest spellbook level and rune gates are atomic`() =
        runTeleportTest { deps ->
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
    fun GameTestState.`Ourania requires the separate Baba Yaga unlock varbit`() =
        runTeleportTest { deps ->
            val expected = expectations[1]
            prepareCaster()
            player.setVarBit(varbits.rc_zmi_lunar_spell, 0)
            addRunes(deps, expected.spell)
            assertRejected(deps, expected)

            player.setVarBit(varbits.rc_zmi_lunar_spell, 1)
            cast(deps, expected.spell)
            advance(4)
            assertEquals(expected.destination, player.coords)
        }

    @Test
    fun GameTestState.`teleblock and level twenty-one Wilderness reject atomically`() =
        runTeleportTest { deps ->
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
        }

    @Test
    fun GameTestState.`level twenty Wilderness permits Lunar teleport`() = runTeleportTest { deps ->
        val expected = expectations.first()
        prepareCaster()
        addRunes(deps, expected.spell)
        player.teleport(CoordGrid(0, 50, 57, 0, 24))
        player.setVarBit(varbits.inside_wilderness, 1)

        cast(deps, expected.spell)
        advance(4)

        assertEquals(expected.destination, player.coords)
    }

    @Test
    fun GameTestState.`Ice Plateau warning cancellation is atomic`() = runTeleportTest { deps ->
        val expected = expectations.last()
        prepareCaster()
        addRunes(deps, expected.spell)
        val startXp = player.statMap.getFineXP(stats.magic)

        cast(deps, expected.spell, confirmIcePlateau = false)

        assertEquals(CoordGrid(0, 50, 50, 0, 0), player.coords)
        assertEquals(expected.runes.values.sum(), spellRuneCount(deps, expected.spell))
        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
    }

    @Test
    fun GameTestState.`staff substitution and Fountain behavior use the shared rune manager`() =
        runTeleportTest { deps ->
            val expected = expectations.first()
            prepareCaster()
            player.righthand = InvObj(objs.earth_battlestaff)
            player.inv[0] = InvObj(objs.astral_rune, 2)
            player.inv[1] = InvObj(objs.law_rune)
            cast(deps, expected.spell)
            advance(4)
            assertEquals(expected.destination, player.coords)
            assertEquals(0, player.count(objs.earth_rune))

            prepareCaster()
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)
            cast(deps, expected.spell)
            advance(4)
            assertEquals(expected.destination, player.coords)
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`all spellbook buttons dispatch their matching teleport`() =
        runTeleportTest { deps ->
            for (expected in expectations) {
                prepareCaster()
                addRunes(deps, expected.spell)
                deps.events.publish(
                    IfOverlayButton(
                        player = player,
                        component = expected.component,
                        comsub = -1,
                        obj = null,
                        op = IfButtonOp.Op1,
                    )
                )
                if (expected.spell.id == objs.spell_lunarghorrock_teleport.id) {
                    resumeMenu(selection = 0)
                }
                advance(4)
                assertEquals(expected.destination, player.coords)
            }
        }

    @Test
    fun GameTestState.`non-cast interface operations leave resources untouched`() =
        runTeleportTest { deps ->
            val expected = expectations.first()
            prepareCaster()
            addRunes(deps, expected.spell)

            deps.events.publish(
                IfOverlayButton(
                    player = player,
                    component = expected.component,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op2,
                )
            )
            advance(4)

            assertEquals(CoordGrid(0, 50, 50, 0, 0), player.coords)
            assertEquals(expected.runes.values.sum(), spellRuneCount(deps, expected.spell))
        }

    private fun GameTestScope.prepareCaster() {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.rc_zmi_lunar_spell, 1)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.teleblock_cycles, 0)
        player.clearInv()
        player.righthand = null
        player.teleport(CoordGrid(0, 50, 50, 0, 0))
        client.clear()
    }

    private fun GameTestScope.cast(
        deps: Dependencies,
        spell: ObjType,
        confirmIcePlateau: Boolean? = null,
    ) {
        player.withProtectedAccess { deps.teleports.cast(this, spell) }
        if (spell.id == objs.spell_lunarghorrock_teleport.id) {
            resumeMenu(if (confirmIcePlateau == true) 0 else 1)
        }
    }

    private fun GameTestScope.resumeMenu(selection: Int) {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.menu_list, subcomponent = selection)
        )
    }

    private fun GameTestScope.assertRejected(
        deps: Dependencies,
        expected: TeleportExpectation,
        message: String? = null,
    ) {
        val origin = player.coords
        val runeCount = spellRuneCount(deps, expected.spell)
        val startXp = player.statMap.getFineXP(stats.magic)
        cast(deps, expected.spell)
        assertEquals(origin, player.coords)
        assertEquals(runeCount, spellRuneCount(deps, expected.spell))
        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        if (message != null) {
            assertMessageSent(message)
        }
        client.clear()
    }

    private fun GameTestScope.addRunes(deps: Dependencies, spellType: ObjType) {
        val spell = requireNotNull(deps.spells.getObjSpell(spellType))
        spell.objReqs.forEachIndexed { slot, req -> player.inv[slot] = InvObj(req.obj, req.count) }
    }

    private fun GameTestScope.spellRuneCount(deps: Dependencies, spellType: ObjType): Int {
        val spell = requireNotNull(deps.spells.getObjSpell(spellType))
        return spell.objReqs.sumOf { player.count(it.obj) }
    }

    private fun GameTestState.runTeleportTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            LunarTeleportSpellScript::class,
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
        val teleports: LunarTeleportSpells,
        val spells: MagicSpellRegistry,
        val objTypes: ObjTypeList,
        val events: EventBus,
    )

    private data class TeleportExpectation(
        val spell: ObjType,
        val level: Int,
        val xp: Double,
        val component: org.rsmod.game.type.comp.ComponentType,
        val runes: Map<ObjType, Int>,
        val destination: CoordGrid,
    )

    private companion object {
        val expectations =
            listOf(
                TeleportExpectation(
                    objs.spell_moonclan_teleport,
                    69,
                    66.0,
                    components.magic_spellbook_tele_moonclan,
                    mapOf(objs.law_rune to 1, objs.astral_rune to 2, objs.earth_rune to 2),
                    CoordGrid(0, 33, 61, 2, 11),
                ),
                TeleportExpectation(
                    objs.spell_zmi_teleport,
                    71,
                    69.0,
                    components.magic_spellbook_ourania_teleport,
                    mapOf(objs.law_rune to 1, objs.astral_rune to 2, objs.earth_rune to 6),
                    CoordGrid(0, 38, 50, 36, 46),
                ),
                TeleportExpectation(
                    objs.spell_waterbirth_teleport,
                    72,
                    71.0,
                    components.magic_spellbook_tele_waterbirth,
                    mapOf(objs.law_rune to 1, objs.astral_rune to 2, objs.water_rune to 1),
                    CoordGrid(0, 39, 58, 50, 44),
                ),
                TeleportExpectation(
                    objs.spell_barboutpost_teleport,
                    75,
                    76.0,
                    components.magic_spellbook_tele_barb_out,
                    mapOf(objs.law_rune to 2, objs.astral_rune to 2, objs.fire_rune to 3),
                    CoordGrid(0, 39, 55, 47, 49),
                ),
                TeleportExpectation(
                    objs.spell_portkhazard_teleport,
                    78,
                    80.0,
                    components.magic_spellbook_tele_khazard,
                    mapOf(objs.law_rune to 2, objs.astral_rune to 2, objs.water_rune to 4),
                    CoordGrid(0, 41, 49, 12, 31),
                ),
                TeleportExpectation(
                    objs.spell_fishguild_teleport,
                    85,
                    89.0,
                    components.magic_spellbook_tele_fish,
                    mapOf(objs.law_rune to 3, objs.astral_rune to 3, objs.water_rune to 10),
                    CoordGrid(0, 40, 52, 51, 63),
                ),
                TeleportExpectation(
                    objs.spell_catherby_teleport,
                    87,
                    92.0,
                    components.magic_spellbook_tele_cather,
                    mapOf(objs.law_rune to 3, objs.astral_rune to 3, objs.water_rune to 10),
                    CoordGrid(0, 43, 53, 49, 57),
                ),
                TeleportExpectation(
                    objs.spell_lunarghorrock_teleport,
                    89,
                    96.0,
                    components.magic_spellbook_tele_ghorrock,
                    mapOf(objs.law_rune to 3, objs.astral_rune to 3, objs.water_rune to 8),
                    CoordGrid(0, 46, 61, 30, 34),
                ),
            )
    }
}
