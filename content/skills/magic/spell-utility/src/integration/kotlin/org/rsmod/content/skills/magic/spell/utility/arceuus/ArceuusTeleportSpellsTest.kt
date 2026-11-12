package org.rsmod.content.skills.magic.spell.utility.arceuus

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
import org.rsmod.api.realm.Realm
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
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varp.VarpType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class ArceuusTeleportSpellsTest {
    @Test
    fun GameTestState.`cache contracts define all twelve Arceuus destination teleports`() =
        runTeleportTest { deps ->
            for (expected in expectations) {
                val spell = requireNotNull(deps.spells.getObjSpell(expected.spell))
                assertEquals(expected.level, spell.levelReq, expected.spell.toString())
                assertEquals(expected.xp, spell.castXp, expected.spell.toString())
                assertEquals(Spellbook.Arceuus, spell.spellbook)
                assertEquals(MagicSpellType.Teleport, spell.type)
                assertEquals(expected.runes, spell.objReqs.associate { it.obj to it.count })
                assertEquals(expected.component.id and 0xFFFF, spell.component.id and 0xFFFF)
                if (expected.spell.id == objs.spell_respawn_teleport.id) {
                    assertNull(deps.objTypes[expected.spell].paramOrNull(params.spell_telecoord))
                } else {
                    assertEquals(
                        expected.destination,
                        deps.objTypes[expected.spell].param(params.spell_telecoord),
                    )
                }
            }
            assertEquals(6_575, seqs.human_spellcast_arceuus_teleport.id)
            assertEquals(1_296, spotanims.arceuus_teleport.id)
            assertEquals(200, synths.teleport_all.id)
            assertEquals(CoordGrid(0, 50, 50, 0, 0), deps.realm.config.respawnCoord)
            assertEquals(68, varps.biohazard.id)
            assertEquals(302, varps.priest_in_peril.id)
            assertEquals(365, varps.monkey_madness.id)
            assertEquals(980, varps.great_brain_robbery.id)
        }

    @Test
    fun GameTestState.`all twelve teleports consume exact costs and arrive after four ticks`() =
        runTeleportTest { deps ->
            for (expected in expectations) {
                prepareCaster()
                addRunes(deps, expected.spell)
                val origin = player.coords
                val startXp = player.statMap.getFineXP(stats.magic)

                cast(deps, expected.spell)

                assertEquals(0, spellRuneCount(deps, expected.spell))
                assertEquals(
                    (expected.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.magic) - startXp,
                )
                assertEquals(seqs.human_spellcast_arceuus_teleport.id, player.pendingSequence.id)
                assertEquals(
                    spotanims.arceuus_teleport.id,
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
    fun GameTestState.`members spellbook level and rune gates are atomic without an AKD gate`() =
        runTeleportTest { deps ->
            val expected = expectations.first()
            prepareCaster()
            addRunes(deps, expected.spell)

            player.members = false
            assertRejected(deps, expected, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            assertRejected(deps, expected)
            player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
            player.stats[stats.magic] = expected.level - 1
            assertRejected(deps, expected, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.clearInv()
            assertRejected(deps, expected, "You do not have enough Law Runes to cast this spell.")

            prepareCaster()
            player.setVarBit(varbits.a_kingdom_divided, 0)
            addRunes(deps, expected.spell)
            cast(deps, expected.spell)
            advance(4)
            assertEquals(expected.destination, player.coords)
        }

    @Test
    fun GameTestState.`quest-gated destinations use cache completion boundaries`() =
        runTeleportTest { deps ->
            for (requirement in questRequirements) {
                val expected = expectations.single { it.spell.id == requirement.spell.id }
                prepareCaster()
                player.setVarp(requirement.varp, requirement.completeState - 1)
                addRunes(deps, expected.spell)
                assertRejected(
                    deps,
                    expected,
                    "You need to complete ${requirement.name} to cast this spell.",
                )

                player.setVarp(requirement.varp, requirement.completeState)
                cast(deps, expected.spell)
                advance(4)
                assertEquals(expected.destination, player.coords)
            }
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

            prepareCaster()
            addRunes(deps, expected.spell)
            player.teleport(CoordGrid(0, 50, 57, 0, 24))
            player.setVarBit(varbits.inside_wilderness, 1)
            cast(deps, expected.spell)
            advance(4)
            assertEquals(expected.destination, player.coords)
        }

    @Test
    fun GameTestState.`staff substitution and Fountain behavior use the shared rune manager`() =
        runTeleportTest { deps ->
            val expected = expectations.first()
            prepareCaster()
            player.righthand = InvObj(objs.earth_battlestaff)
            player.inv[0] = InvObj(objs.law_rune)
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

            assertEquals(ORIGIN, player.coords)
            assertEquals(expected.runes.values.sum(), spellRuneCount(deps, expected.spell))
        }

    private fun GameTestScope.prepareCaster() {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.a_kingdom_divided, 0)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.teleblock_cycles, 0)
        player.setVarp(varps.priest_in_peril, 60)
        player.setVarp(varps.biohazard, 16)
        player.setVarp(varps.great_brain_robbery, 130)
        player.setVarp(varps.monkey_madness, 9)
        player.clearInv()
        player.righthand = null
        player.teleport(ORIGIN)
        client.clear()
    }

    private fun GameTestScope.cast(deps: Dependencies, spell: ObjType) {
        player.withProtectedAccess { deps.teleports.cast(this, spell) }
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
            ArceuusTeleportSpellScript::class,
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
        val teleports: ArceuusTeleportSpells,
        val spells: MagicSpellRegistry,
        val objTypes: ObjTypeList,
        val events: EventBus,
        val realm: Realm,
    )

    private data class TeleportExpectation(
        val spell: ObjType,
        val level: Int,
        val xp: Double,
        val component: ComponentType,
        val runes: Map<ObjType, Int>,
        val destination: CoordGrid,
    )

    private data class QuestRequirement(
        val spell: ObjType,
        val varp: VarpType,
        val completeState: Int,
        val name: String,
    )

    private companion object {
        val ORIGIN = CoordGrid(0, 40, 40, 0, 0)
        val RESPAWN = CoordGrid(0, 50, 50, 0, 0)

        val expectations =
            listOf(
                TeleportExpectation(
                    objs.spell_arceuuslibrary_teleport,
                    6,
                    9.0,
                    components.magic_spellbook_arceuus_library_teleport,
                    mapOf(objs.law_rune to 1, objs.earth_rune to 2),
                    CoordGrid(0, 25, 59, 33, 62),
                ),
                TeleportExpectation(
                    objs.spell_draynormanor_teleport,
                    17,
                    16.0,
                    components.magic_spellbook_draynor_manor_teleport,
                    mapOf(objs.law_rune to 1, objs.water_rune to 1, objs.earth_rune to 1),
                    CoordGrid(0, 48, 52, 36, 23),
                ),
                TeleportExpectation(
                    objs.spell_battlefront_teleport,
                    23,
                    19.0,
                    components.magic_spellbook_battlefront_teleport,
                    mapOf(objs.law_rune to 1, objs.earth_rune to 1, objs.fire_rune to 1),
                    CoordGrid(0, 21, 58, 4, 27),
                ),
                TeleportExpectation(
                    objs.spell_mindaltar_teleport,
                    28,
                    22.0,
                    components.magic_spellbook_mind_altar_teleport,
                    mapOf(objs.law_rune to 1, objs.mind_rune to 2),
                    CoordGrid(0, 46, 54, 36, 53),
                ),
                TeleportExpectation(
                    objs.spell_respawn_teleport,
                    34,
                    27.0,
                    components.magic_spellbook_respawn_teleport,
                    mapOf(objs.law_rune to 1, objs.soul_rune to 1),
                    RESPAWN,
                ),
                TeleportExpectation(
                    objs.spell_salvegrave_teleport,
                    40,
                    30.0,
                    components.magic_spellbook_salve_graveyard_teleport,
                    mapOf(objs.law_rune to 1, objs.soul_rune to 2),
                    CoordGrid(0, 53, 54, 40, 5),
                ),
                TeleportExpectation(
                    objs.spell_fenkenstrainscastle_teleport,
                    48,
                    50.0,
                    components.magic_spellbook_fenkenstrain_castle_teleport,
                    mapOf(objs.law_rune to 1, objs.soul_rune to 1, objs.earth_rune to 1),
                    CoordGrid(0, 55, 55, 28, 9),
                ),
                TeleportExpectation(
                    objs.spell_westardougne_teleport,
                    61,
                    68.0,
                    components.magic_spellbook_west_ardougne_teleport,
                    mapOf(objs.law_rune to 2, objs.soul_rune to 2),
                    CoordGrid(0, 39, 51, 4, 27),
                ),
                TeleportExpectation(
                    objs.spell_harmonyisland_teleport,
                    65,
                    74.0,
                    components.magic_spellbook_harmony_island_teleport,
                    mapOf(objs.law_rune to 1, objs.soul_rune to 1, objs.nature_rune to 1),
                    CoordGrid(0, 59, 44, 21, 51),
                ),
                TeleportExpectation(
                    objs.spell_cemetery_teleport,
                    71,
                    82.0,
                    components.magic_spellbook_cemetery_teleport,
                    mapOf(objs.law_rune to 1, objs.soul_rune to 1, objs.blood_rune to 1),
                    CoordGrid(0, 46, 58, 36, 51),
                ),
                TeleportExpectation(
                    objs.spell_barrows_teleport,
                    83,
                    90.0,
                    components.magic_spellbook_barrows_teleport,
                    mapOf(objs.law_rune to 2, objs.soul_rune to 2, objs.blood_rune to 1),
                    CoordGrid(0, 55, 51, 45, 50),
                ),
                TeleportExpectation(
                    objs.spell_arceuusapeatoll_teleport,
                    90,
                    100.0,
                    components.magic_spellbook_arceuus_ape_atoll_teleport,
                    mapOf(objs.law_rune to 2, objs.soul_rune to 2, objs.blood_rune to 2),
                    CoordGrid(0, 43, 142, 19, 13),
                ),
            )

        val questRequirements =
            listOf(
                QuestRequirement(
                    objs.spell_salvegrave_teleport,
                    varps.priest_in_peril,
                    60,
                    "Priest in Peril",
                ),
                QuestRequirement(
                    objs.spell_fenkenstrainscastle_teleport,
                    varps.priest_in_peril,
                    60,
                    "Priest in Peril",
                ),
                QuestRequirement(
                    objs.spell_barrows_teleport,
                    varps.priest_in_peril,
                    60,
                    "Priest in Peril",
                ),
                QuestRequirement(
                    objs.spell_westardougne_teleport,
                    varps.biohazard,
                    16,
                    "Biohazard",
                ),
                QuestRequirement(
                    objs.spell_harmonyisland_teleport,
                    varps.great_brain_robbery,
                    130,
                    "The Great Brain Robbery",
                ),
                QuestRequirement(
                    objs.spell_arceuusapeatoll_teleport,
                    varps.monkey_madness,
                    9,
                    "Monkey Madness I",
                ),
            )
    }
}
