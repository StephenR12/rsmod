package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.righthand
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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class HunterKitSpellTest {
    @Test
    fun GameTestState.`cache contracts define Hunter Kit and its eight contents`() =
        runHunterKitTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_hunter_kit))
            assertEquals(11146, spell.obj.id)
            assertEquals(71, spell.levelReq)
            assertEquals(70.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 2, objs.earth_rune to 2),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(119, components.magic_spellbook_hunter_kit.id and 0xFFFF)
            assertEquals(6303, seqs.dream_player_hunter_box_spell.id)
            assertEquals(1074, spotanims.dream_hunter_spell.id)
            assertEquals(3618, varbits.dream_mentor.id)
            assertEquals(listOf(28), deps.dbRows.types.getValue(33).data[19])

            val kit = deps.objs.types.getValue(objs.hunter_kit.id)
            assertEquals(11159, objs.hunter_kit.id)
            assertEquals("Hunter kit", kit.name)
            assertTrue(kit.members)
            assertFalse(kit.isStackable)
            assertFalse(kit.tradeable)
            assertEquals(listOf("Open", null, null, null, "Destroy"), kit.iop.toList())
            assertEquals(
                listOf(10150, 10010, 10006, 10031, 10029, 596, 11260, 10008),
                HunterKitContents.all.map { it.id },
            )
        }

    @Test
    fun GameTestState.`successful cast creates one kit and applies exact effects`() =
        runHunterKitTest { deps ->
            prepareCaster(deps)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.hunterKit.cast(this) }

            assertEquals(1, player.count(objs.hunter_kit))
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.earth_rune))
            assertEquals(700, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.dream_player_hunter_box_spell.id, player.pendingSequence.id)
            assertTrue(player.pendingSpotanims.isNotEmpty())
            assertEquals(3615, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`existing kit or complete contents reject but an incomplete set permits`() =
        runHunterKitTest { deps ->
            prepareCaster(deps)
            player.inv[3] = InvObj(objs.hunter_kit)
            player.withProtectedAccess {
                deps.hunterKit.cast(this)
                assertMessageSent(ALREADY_HAS_EQUIPMENT_MESSAGE)
            }
            assertEquals(2, player.count(objs.astral_rune))

            prepareCaster(deps)
            HunterKitContents.all.forEachIndexed { index, obj ->
                player.inv[index + 3] = InvObj(obj)
            }
            player.withProtectedAccess {
                deps.hunterKit.cast(this)
                assertMessageSent(ALREADY_HAS_EQUIPMENT_MESSAGE)
            }
            assertEquals(2, player.count(objs.astral_rune))

            prepareCaster(deps)
            HunterKitContents.all.dropLast(1).forEachIndexed { index, obj ->
                player.inv[index + 3] = InvObj(obj)
            }
            player.withProtectedAccess { deps.hunterKit.cast(this) }
            assertEquals(1, player.count(objs.hunter_kit))
        }

    @Test
    fun GameTestState.`members quests spellbook and Magic gates reject atomically`() =
        runHunterKitTest { deps ->
            prepareCaster(deps)

            player.members = false
            player.withProtectedAccess {
                deps.hunterKit.cast(this)
                assertMessageSent("You need to be on a members' world to cast this spell.")
            }
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            player.withProtectedAccess {
                deps.hunterKit.cast(this)
                assertMessageSent("You need to complete Lunar Diplomacy to cast this spell.")
            }
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.dream_mentor, 27)
            player.withProtectedAccess {
                deps.hunterKit.cast(this)
                assertMessageSent("You need to complete Dream Mentor to cast this spell.")
            }
            player.setVarBit(varbits.dream_mentor, 28)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            player.withProtectedAccess { deps.hunterKit.cast(this) }
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 70
            player.withProtectedAccess {
                deps.hunterKit.cast(this)
                assertMessageSent("Your Magic level is not high enough for this spell.")
            }

            assertEquals(0, player.count(objs.hunter_kit))
            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(2, player.count(objs.earth_rune))
        }

    @Test
    fun GameTestState.`full inventory and missing runes make no partial changes`() =
        runHunterKitTest { deps ->
            prepareCaster(deps)
            player.fillInv()
            player.inv[0] = InvObj(objs.astral_rune, 2)
            player.inv[1] = InvObj(objs.earth_rune, 2)
            player.withProtectedAccess {
                deps.hunterKit.cast(this)
                assertMessageSent(constants.dm_invspace)
            }
            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(2, player.count(objs.earth_rune))
            assertEquals(0, player.count(objs.hunter_kit))

            prepareCaster(deps)
            player.inv[0] = null
            player.withProtectedAccess {
                deps.hunterKit.cast(this)
                assertMessageSent("You do not have enough Astral Runes to cast this spell.")
            }
            assertEquals(2, player.count(objs.earth_rune))
            assertEquals(0, player.count(objs.hunter_kit))
        }

    @Test
    fun GameTestState.`three tick cooldown staff substitution and Fountain policy are shared`() =
        runHunterKitTest { deps ->
            prepareCaster(deps, casts = 2)
            player.inv[1] = null
            player.righthand = InvObj(objs.earth_battlestaff)

            player.withProtectedAccess { deps.hunterKit.cast(this) }
            player.inv[player.inv.indexOfFirst { it?.id == objs.hunter_kit.id }] = null
            player.withProtectedAccess { deps.hunterKit.cast(this) }
            advance(2)
            player.withProtectedAccess { deps.hunterKit.cast(this) }
            assertEquals(2, player.count(objs.astral_rune))

            advance()
            player.withProtectedAccess { deps.hunterKit.cast(this) }
            assertEquals(0, player.count(objs.astral_rune))

            advance(3)
            prepareCaster(deps, casts = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.hunterKit.cast(this) }
            assertEquals(1, player.count(objs.hunter_kit))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState
        .`opening atomically replaces the kit with all eight contents and plays sound`() =
        runHunterKitTest { deps ->
            prepareCaster(deps, casts = 0)
            player.inv[4] = InvObj(objs.hunter_kit)

            player.withProtectedAccess { deps.hunterKit.open(this, 4) }

            assertEquals(0, player.count(objs.hunter_kit))
            for (content in HunterKitContents.all) {
                assertEquals(1, player.count(content), content.toString())
            }
            assertEquals(3616, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`opening needs seven free slots and stale requests do nothing`() =
        runHunterKitTest { deps ->
            prepareCaster(deps, casts = 0)
            player.fillInv()
            player.inv[4] = InvObj(objs.hunter_kit)
            repeat(6) { player.inv[22 + it] = null }
            player.withProtectedAccess {
                deps.hunterKit.open(this, 4)
                assertMessageSent(constants.dm_invspace)
            }
            assertEquals(1, player.count(objs.hunter_kit))
            assertEquals(0, HunterKitContents.all.sumOf { player.count(it) })

            player.inv[21] = null
            player.withProtectedAccess { deps.hunterKit.open(this, 4) }
            assertEquals(0, player.count(objs.hunter_kit))
            assertEquals(8, HunterKitContents.all.sumOf { player.count(it) })

            client.clear()
            player.withProtectedAccess { deps.hunterKit.open(this, 4) }
            assertTrue(client.filterIsInstance<SynthSound>().isEmpty())
            assertEquals(8, HunterKitContents.all.sumOf { player.count(it) })
        }

    private fun GameTestScope.prepareCaster(deps: Dependencies, casts: Int = 1) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.dream_mentor, 28)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.clearInv()
        player.righthand = null
        client.clear()
        deps.cooldowns.markCast(player, player.currentMapClock)
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 2)
            player.inv[1] = InvObj(objs.earth_rune, casts * 2)
        }
    }

    private fun GameTestState.runHunterKitTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            HunterKitSpellScript::class,
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
            bind(UtilitySpellCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val hunterKit: HunterKitSpell,
        val spells: MagicSpellRegistry,
        val dbRows: DbRowTypeList,
        val objs: ObjTypeList,
        val cooldowns: UtilitySpellCooldownRepository,
    )

    private companion object {
        const val ALREADY_HAS_EQUIPMENT_MESSAGE =
            "Why do you want to create a kit? You already have all the Hunter equipment it contains."
    }
}
