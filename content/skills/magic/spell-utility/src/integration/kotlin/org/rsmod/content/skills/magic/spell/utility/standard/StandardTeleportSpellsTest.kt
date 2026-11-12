package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.rsmod.api.config.refs.varps
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
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class StandardTeleportSpellsTest {
    @Test
    fun GameTestState.`spell teleport clears the casting animation and spotanim`() =
        runTeleportTest {
            prepareBase(members = false)
            player.withProtectedAccess {
                anim(seqs.teleport_casting)
                spotanim(spotanims.teleport_casting)
                spellTeleport(expectations.first().destination)

                assertEquals(0, player.pendingSequence.id)
                assertEquals(65535, EntitySpotanim(player.pendingSpotanims.getLong(0)).id)
            }
        }

    @Test
    fun GameTestState.`cache contracts define the first ten Standard teleports`() =
        runTeleportTest { deps ->
            for (expected in expectations) {
                val spell = requireNotNull(deps.spells.getObjSpell(expected.spell))
                assertEquals(expected.level, spell.levelReq, expected.spell.toString())
                assertEquals(expected.xp, spell.castXp, expected.spell.toString())
                assertEquals(Spellbook.Standard, spell.spellbook)
                assertEquals(MagicSpellType.Teleport, spell.type)
                assertEquals(expected.runes, spell.objReqs.associate { it.obj to it.count })
                if (expected.spell.id == objs.spell_apeatoll_teleport.id) {
                    assertEquals(
                        null,
                        spell.objReqs.single { it.obj.id == objs.banana.id }.wornSlot,
                    )
                }
                assertEquals(expected.component, spell.component.id and 0xFFFF)
                assertEquals(
                    expected.destination,
                    deps.objTypes[expected.spell].param(params.spell_telecoord),
                )
            }
            assertEquals(714, seqs.teleport_casting.id)
            assertEquals(111, spotanims.teleport_casting.id)
            assertEquals(200, synths.teleport_all.id)
            assertEquals(4480, varbits.varrock_diary_medium_complete.id)
            assertEquals(4477, varbits.kandarin_diary_hard_complete.id)
            assertEquals(4460, varbits.ardougne_diary_hard_complete.id)
            assertEquals(4585, varbits.varrock_ge_teleport.id)
            assertEquals(4560, varbits.seers_camelot_teleport.id)
            assertEquals(4548, varbits.yanille_teleport_location.id)
        }

    @Test
    fun GameTestState.`free-world teleports consume exact runes xp and arrive after four ticks`() =
        runTeleportTest { deps ->
            for (expected in expectations.take(3)) {
                prepareBase(members = false)
                addRunes(deps, expected.spell)
                val origin = player.coords
                val startXp = player.statMap.getFineXP(stats.magic)

                player.withProtectedAccess { deps.teleports.cast(this, expected.spell) }

                assertEquals(0, spellRuneCount(deps, expected.spell))
                assertEquals(
                    (expected.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.magic) - startXp,
                )
                assertEquals(seqs.teleport_casting.id, player.pendingSequence.id)
                assertEquals(origin, player.coords)
                advance(3)
                assertEquals(origin, player.coords)
                advance()
                assertEquals(expected.destination, player.coords)
            }
        }

    @Test
    fun GameTestState.`member teleports reject free worlds before quest checks`() =
        runTeleportTest { deps ->
            for (expected in expectations.drop(3)) {
                prepareBase(members = false)
                addRunes(deps, expected.spell)
                val origin = player.coords
                val count = spellRuneCount(deps, expected.spell)
                player.withProtectedAccess {
                    deps.teleports.cast(this, expected.spell)
                    assertMessageSent("You need to be on a members' world to cast this spell.")
                }
                assertEquals(origin, player.coords)
                assertEquals(count, spellRuneCount(deps, expected.spell))
            }

            val expected = expectations[3]
            prepareBase()
            addRunes(deps, expected.spell)
            player.members = true
            player.withProtectedAccess { deps.teleports.cast(this, expected.spell) }
            advance(4)
            assertEquals(expected.destination, player.coords)
            assertEquals(0, spellRuneCount(deps, expected.spell))
        }

    @Test
    fun GameTestState.`level rune teleblock and deep-Wilderness failures are atomic`() =
        runTeleportTest { deps ->
            val expected = expectations.first()

            prepareBase(magicLevel = expected.level - 1)
            addRunes(deps, expected.spell)
            assertRejected(deps, expected, "Your Magic level is not high enough for this spell.")

            prepareBase()
            addRunes(deps, expected.spell)
            val lawSlot = player.inv.indexOfFirst { it?.id == objs.law_rune.id }
            player.inv[lawSlot] = null
            assertRejected(deps, expected, "You do not have enough Law Runes to cast this spell.")

            prepareBase()
            addRunes(deps, expected.spell)
            player.setVarBit(varbits.teleblock_cycles, 101)
            assertRejected(deps, expected, "A magical force stops you from teleporting.")

            prepareBase()
            addRunes(deps, expected.spell)
            player.teleport(CoordGrid(0, 50, 57, 0, 32))
            player.setVarBit(varbits.inside_wilderness, 1)
            assertRejected(deps, expected, "A mysterious force prevents you from teleporting!")
        }

    @Test
    fun GameTestState.`level twenty Wilderness permits Standard teleport`() =
        runTeleportTest { deps ->
            val expected = expectations.first()
            prepareBase()
            addRunes(deps, expected.spell)
            player.teleport(CoordGrid(0, 50, 57, 0, 24))
            player.setVarBit(varbits.inside_wilderness, 1)

            player.withProtectedAccess { deps.teleports.cast(this, expected.spell) }
            advance(4)

            assertEquals(expected.destination, player.coords)
        }

    @Test
    fun GameTestState.`staff substitution and Fountain behavior use the shared rune manager`() =
        runTeleportTest { deps ->
            val expected = expectations.first()
            prepareBase(members = false)
            player.righthand = InvObj(objs.air_staff)
            player.inv[0] = InvObj(objs.fire_rune)
            player.inv[1] = InvObj(objs.law_rune)
            player.withProtectedAccess { deps.teleports.cast(this, expected.spell) }
            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(0, player.count(objs.law_rune))
            assertEquals(0, player.count(objs.air_rune))
            advance(4)

            prepareBase(members = false)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.teleports.cast(this, expected.spell) }
            advance(4)
            assertEquals(expected.destination, player.coords)
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`quest teleports reject one state early and succeed at exact unlock`() =
        runTeleportTest { deps ->
            for (expected in expectations.drop(4)) {
                prepareBase()
                setUnlock(expected.spell, unlocked = false)
                addRunes(deps, expected.spell)
                assertRejected(deps, expected, expected.unlockMessage!!)

                setUnlock(expected.spell, unlocked = true)
                val startXp = player.statMap.getFineXP(stats.magic)
                player.withProtectedAccess { deps.teleports.cast(this, expected.spell) }
                advance(4)

                assertEquals(expected.destination, player.coords)
                assertEquals(0, spellRuneCount(deps, expected.spell))
                assertEquals(
                    (expected.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.magic) - startXp,
                )
            }
        }

    @Test
    fun GameTestState.`Ape Atoll consumes its unnoted banana with the rune costs`() =
        runTeleportTest { deps ->
            val expected = expectations.last()
            prepareBase()
            setUnlock(expected.spell, unlocked = true)
            addRunes(deps, expected.spell)
            player.withProtectedAccess { deps.teleports.cast(this, expected.spell) }
            assertNoMessageSent()
            assertEquals(seqs.teleport_casting.id, player.pendingSequence.id)
            assertEquals(0, spellRuneCount(deps, expected.spell))
            advance(4)

            assertEquals(expected.destination, player.coords)

            prepareBase()
            setUnlock(expected.spell, unlocked = true)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            player.inv[0] = InvObj(objs.banana)
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.teleports.cast(this, expected.spell) }
            advance(4)

            assertEquals(expected.destination, player.coords)
            assertEquals(0, player.count(objs.banana))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`Ape Atoll missing banana leaves all runes untouched`() =
        runTeleportTest { deps ->
            val expected = expectations.last()
            prepareBase()
            setUnlock(expected.spell, unlocked = true)
            addRunes(deps, expected.spell)
            val bananaSlot = player.inv.indexOfFirst { it?.id == objs.banana.id }
            player.inv[bananaSlot] = null
            val runesBefore = spellRuneCount(deps, expected.spell)

            player.withProtectedAccess {
                deps.teleports.cast(this, expected.spell)
                assertMessageSent("You do not have enough bananas to cast this spell.")
            }

            assertEquals(runesBefore, spellRuneCount(deps, expected.spell))
            assertEquals(CoordGrid(0, 50, 50, 0, 0), player.coords)
        }

    @Test
    fun GameTestState.`quest teleport spellbook button dispatches`() = runTeleportTest { deps ->
        val expected = expectations[6]
        prepareBase()
        setUnlock(expected.spell, unlocked = true)
        addRunes(deps, expected.spell)

        deps.events.publish(
            IfOverlayButton(
                player = player,
                component = components.magic_spellbook_civitas_fortis_teleport,
                comsub = -1,
                obj = null,
                op = IfButtonOp.Op1,
            )
        )
        advance(4)

        assertEquals(expected.destination, player.coords)
    }

    @Test
    fun GameTestState.`Varrock spellbook teleport arrives visible movable and unlocked`() =
        runTeleportTest { deps ->
            val expected = expectations.first()
            prepareBase(members = false)
            addRunes(deps, expected.spell)

            deps.events.publish(
                IfOverlayButton(
                    player = player,
                    component = components.magic_spellbook_varrock_teleport,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )
            advance(4)

            assertEquals(expected.destination, player.coords)
            assertEquals(MoveSpeed.Walk, player.moveSpeed)
            assertFalse(player.hidden)
            assertFalse(player.isAccessProtected)
        }

    @Test
    fun GameTestState.`configured diary destinations require both completion and toggle`() =
        runTeleportTest { deps ->
            for ((spell, alternate) in alternateDestinations) {
                val expected = expectations.single { it.spell.id == spell.id }
                prepareBase()
                if (spell.id == objs.spell_watchtower_teleport.id) {
                    setUnlock(spell, unlocked = true)
                }
                setAlternateToggle(spell, enabled = true)
                addRunes(deps, spell)

                player.withProtectedAccess { deps.teleports.cast(this, spell) }
                advance(4)
                assertEquals(expected.destination, player.coords)

                prepareBase()
                if (spell.id == objs.spell_watchtower_teleport.id) {
                    setUnlock(spell, unlocked = true)
                }
                setDiaryComplete(spell, complete = true)
                setAlternateToggle(spell, enabled = true)
                addRunes(deps, spell)

                player.withProtectedAccess { deps.teleports.cast(this, spell) }
                advance(4)
                assertEquals(alternate, player.coords)
            }
        }

    @Test
    fun GameTestState.`completed diaries retain base destination while toggle is disabled`() =
        runTeleportTest { deps ->
            for ((spell, _) in alternateDestinations) {
                val expected = expectations.single { it.spell.id == spell.id }
                prepareBase()
                if (spell.id == objs.spell_watchtower_teleport.id) {
                    setUnlock(spell, unlocked = true)
                }
                setDiaryComplete(spell, complete = true)
                addRunes(deps, spell)

                player.withProtectedAccess { deps.teleports.cast(this, spell) }
                advance(4)
                assertEquals(expected.destination, player.coords)
            }
        }

    @Test
    fun GameTestState.`configuration operations do not cast or consume resources`() =
        runTeleportTest { deps ->
            val expected = expectations.first()
            prepareBase(members = false)
            addRunes(deps, expected.spell)
            val origin = player.coords
            val runeCount = spellRuneCount(deps, expected.spell)
            val startXp = player.statMap.getFineXP(stats.magic)

            deps.events.publish(
                IfOverlayButton(
                    player = player,
                    component = components.magic_spellbook_varrock_teleport,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op2,
                )
            )
            advance(4)

            assertEquals(origin, player.coords)
            assertEquals(runeCount, spellRuneCount(deps, expected.spell))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.assertRejected(
        deps: Dependencies,
        expected: TeleportExpectation,
        message: String,
    ) {
        val origin = player.coords
        val runeCount = spellRuneCount(deps, expected.spell)
        val startXp = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess {
            deps.teleports.cast(this, expected.spell)
            assertMessageSent(message)
        }
        assertEquals(origin, player.coords)
        assertEquals(runeCount, spellRuneCount(deps, expected.spell))
        assertEquals(startXp, player.statMap.getFineXP(stats.magic))
    }

    private fun GameTestScope.prepareBase(magicLevel: Int = 99, members: Boolean = true) {
        player.members = members
        player.stats[stats.magic] = magicLevel
        player.clearInv()
        player.righthand = null
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.teleblock_cycles, 0)
        player.setVarBit(varbits.veos_progress, 0)
        player.setVarBit(varbits.vmq2, 0)
        player.setVarBit(varbits.hundred_ilm_quest, 0)
        player.setVarBit(varbits.ardougne_diary_hard_complete, 0)
        player.setVarBit(varbits.kandarin_diary_hard_complete, 0)
        player.setVarBit(varbits.varrock_diary_medium_complete, 0)
        player.setVarBit(varbits.yanille_teleport_location, 0)
        player.setVarBit(varbits.seers_camelot_teleport, 0)
        player.setVarBit(varbits.varrock_ge_teleport, 0)
        player.setVarp(varps.elenaquest, 0)
        player.setVarp(varps.itwatchtower, 0)
        player.setVarp(varps.eadgar_quest, 0)
    }

    private fun GameTestScope.setUnlock(spell: ObjType, unlocked: Boolean) {
        when (spell.id) {
            objs.spell_kourendcastle_teleport.id ->
                player.setVarBit(varbits.veos_progress, if (unlocked) 7 else 6)
            objs.spell_ardougne_teleport.id ->
                player.setVarp(varps.elenaquest, if (unlocked) 30 else 29)
            objs.spell_civitas_fortis_teleport.id ->
                player.setVarBit(varbits.vmq2, if (unlocked) 50 else 49)
            objs.spell_watchtower_teleport.id ->
                player.setVarp(varps.itwatchtower, if (unlocked) 14 else 13)
            objs.spell_trollheim_teleport.id ->
                player.setVarp(varps.eadgar_quest, if (unlocked) 110 else 109)
            objs.spell_apeatoll_teleport.id ->
                player.setVarBit(varbits.hundred_ilm_quest, if (unlocked) 50 else 49)
            else -> error("No quest unlock for $spell")
        }
    }

    private fun GameTestScope.setDiaryComplete(spell: ObjType, complete: Boolean) {
        val value = if (complete) 1 else 0
        when (spell.id) {
            objs.spell_varrock_teleport.id ->
                player.setVarBit(varbits.varrock_diary_medium_complete, value)
            objs.spell_camelot_teleport.id ->
                player.setVarBit(varbits.kandarin_diary_hard_complete, value)
            objs.spell_watchtower_teleport.id ->
                player.setVarBit(varbits.ardougne_diary_hard_complete, value)
            else -> error("No alternate diary destination for $spell")
        }
    }

    private fun GameTestScope.setAlternateToggle(spell: ObjType, enabled: Boolean) {
        val value = if (enabled) 1 else 0
        when (spell.id) {
            objs.spell_varrock_teleport.id -> player.setVarBit(varbits.varrock_ge_teleport, value)
            objs.spell_camelot_teleport.id ->
                player.setVarBit(varbits.seers_camelot_teleport, value)
            objs.spell_watchtower_teleport.id ->
                player.setVarBit(varbits.yanille_teleport_location, value)
            else -> error("No alternate teleport toggle for $spell")
        }
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
            StandardTeleportSpellScript::class,
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
        val teleports: StandardTeleportSpells,
        val spells: MagicSpellRegistry,
        val objTypes: ObjTypeList,
        val events: EventBus,
    )

    private data class TeleportExpectation(
        val spell: ObjType,
        val level: Int,
        val xp: Double,
        val component: Int,
        val runes: Map<ObjType, Int>,
        val destination: CoordGrid,
        val unlockMessage: String? = null,
    )

    private companion object {
        val alternateDestinations =
            listOf(
                objs.spell_varrock_teleport to CoordGrid(0, 49, 54, 29, 23),
                objs.spell_camelot_teleport to CoordGrid(0, 42, 54, 36, 28),
                objs.spell_watchtower_teleport to CoordGrid(0, 40, 48, 24, 25),
            )

        val expectations =
            listOf(
                TeleportExpectation(
                    objs.spell_varrock_teleport,
                    25,
                    35.0,
                    23,
                    mapOf(objs.air_rune to 3, objs.fire_rune to 1, objs.law_rune to 1),
                    CoordGrid(0, 50, 53, 13, 32),
                ),
                TeleportExpectation(
                    objs.spell_lumbridge_teleport,
                    31,
                    41.0,
                    26,
                    mapOf(objs.air_rune to 3, objs.earth_rune to 1, objs.law_rune to 1),
                    CoordGrid(0, 50, 50, 21, 18),
                ),
                TeleportExpectation(
                    objs.spell_falador_teleport,
                    37,
                    48.0,
                    29,
                    mapOf(objs.air_rune to 3, objs.water_rune to 1, objs.law_rune to 1),
                    CoordGrid(0, 46, 52, 21, 50),
                ),
                TeleportExpectation(
                    objs.spell_camelot_teleport,
                    45,
                    55.5,
                    34,
                    mapOf(objs.air_rune to 5, objs.law_rune to 1),
                    CoordGrid(0, 43, 54, 5, 22),
                ),
                TeleportExpectation(
                    objs.spell_kourendcastle_teleport,
                    48,
                    58.0,
                    36,
                    mapOf(objs.water_rune to 1, objs.fire_rune to 1, objs.law_rune to 2),
                    CoordGrid(0, 25, 57, 41, 25),
                    "You must complete Client of Kourend to cast this spell.",
                ),
                TeleportExpectation(
                    objs.spell_ardougne_teleport,
                    51,
                    61.0,
                    41,
                    mapOf(objs.water_rune to 2, objs.law_rune to 2),
                    CoordGrid(0, 41, 51, 37, 38),
                    "You must complete Plague City and read the Ardougne teleport scroll to cast this spell.",
                ),
                TeleportExpectation(
                    objs.spell_civitas_fortis_teleport,
                    54,
                    64.0,
                    43,
                    mapOf(objs.earth_rune to 1, objs.fire_rune to 1, objs.law_rune to 2),
                    CoordGrid(0, 26, 48, 17, 61),
                    "You must complete Twilight's Promise to cast this spell.",
                ),
                TeleportExpectation(
                    objs.spell_watchtower_teleport,
                    58,
                    68.0,
                    47,
                    mapOf(objs.earth_rune to 2, objs.law_rune to 2),
                    CoordGrid(0, 39, 48, 50, 42),
                    "You must complete Watchtower and read the spell scroll to cast this spell.",
                ),
                TeleportExpectation(
                    objs.spell_trollheim_teleport,
                    61,
                    68.0,
                    54,
                    mapOf(objs.fire_rune to 2, objs.law_rune to 2),
                    CoordGrid(0, 45, 57, 10, 31),
                    "You must complete Eadgar's Ruse to cast this spell.",
                ),
                TeleportExpectation(
                    objs.spell_apeatoll_teleport,
                    64,
                    74.0,
                    57,
                    mapOf(
                        objs.fire_rune to 2,
                        objs.water_rune to 2,
                        objs.law_rune to 2,
                        objs.banana to 1,
                    ),
                    CoordGrid(0, 43, 43, 45, 46),
                    "You must complete the Freeing King Awowogei subquest to cast this spell.",
                ),
            )
    }
}
