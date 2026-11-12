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
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
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
import org.rsmod.events.EventBus
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpLifetime

@Execution(ExecutionMode.SAME_THREAD)
class MagicImbueSpellTest {
    @Test
    fun GameTestState.`cache contracts define Magic Imbue and its client state`() =
        runMagicImbueTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_magic_imbue))
            assertEquals(9124, spell.obj.id)
            assertEquals(82, spell.levelReq)
            assertEquals(86.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 2, objs.fire_rune to 7, objs.water_rune to 7),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(128, components.magic_spellbook_magic_imbue.id and 0xFFFF)
            assertEquals(8970, seqs.human_cast_selfimbue.id)
            assertEquals(2888, synths.lunar_embue_runes.id)

            val active = deps.varbitTypes[varbits.magic_imbue_active]
            assertEquals(5438, active.id)
            assertEquals(439, active.baseVar.id)
            assertEquals(2..3, active.bits)
            assertEquals(VarpLifetime.Perm, active.baseVar.scope)
            assertEquals(listOf(190), deps.dbRows.types.getValue(88).data[19])
        }

    @Test
    fun GameTestState.`successful cast consumes runes awards xp and activates presentation`() =
        runMagicImbueTest { deps ->
            prepareCaster(deps)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.magicImbue.cast(this) }

            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(0, player.count(objs.water_rune))
            assertEquals(860, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.human_cast_selfimbue.id, player.pendingSequence.id)
            assertEquals(2888, client.filterIsInstance<SynthSound>().single().id)
            assertTrue(deps.states.isActive(player))
            assertEquals(21, deps.states.remainingCycles(player))
            assertEquals(2, player.vars[varbits.magic_imbue_active])
            assertTrue(deps.cooldowns.isReady(player, player.currentMapClock))
        }

    @Test
    fun GameTestState.`effect and recast lock use the exact twenty one tick boundary`() =
        runMagicImbueTest { deps ->
            prepareCaster(deps, casts = 2)
            player.withProtectedAccess { deps.magicImbue.cast(this) }

            advance(10)
            assertEquals(11, deps.states.remainingCycles(player))
            assertEquals(2, player.vars[varbits.magic_imbue_active])
            player.withProtectedAccess { deps.magicImbue.cast(this) }
            assertEquals(2, player.count(objs.astral_rune))

            advance()
            assertEquals(10, deps.states.remainingCycles(player))
            assertEquals(1, player.vars[varbits.magic_imbue_active])
            advance(9)
            assertEquals(1, deps.states.remainingCycles(player))
            player.withProtectedAccess { deps.magicImbue.cast(this) }
            assertEquals(2, player.count(objs.astral_rune))

            advance()
            assertFalse(deps.states.isActive(player))
            assertEquals(0, player.vars[varbits.magic_imbue_active])
            player.withProtectedAccess { deps.magicImbue.cast(this) }
            assertEquals(0, player.count(objs.astral_rune))
            assertTrue(deps.states.isActive(player))
        }

    @Test
    fun GameTestState.`members quest spellbook Magic and rune failures are atomic`() =
        runMagicImbueTest { deps ->
            prepareCaster(deps)

            player.members = false
            player.withProtectedAccess {
                deps.magicImbue.cast(this)
                assertMessageSent("You need to be on a members' world to cast this spell.")
            }
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            player.withProtectedAccess {
                deps.magicImbue.cast(this)
                assertMessageSent("You need to complete Lunar Diplomacy to cast this spell.")
            }
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            player.withProtectedAccess { deps.magicImbue.cast(this) }
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 81
            player.withProtectedAccess {
                deps.magicImbue.cast(this)
                assertMessageSent("Your Magic level is not high enough for this spell.")
            }
            player.stats[stats.magic] = 99
            player.inv[0] = null
            player.withProtectedAccess {
                deps.magicImbue.cast(this)
                assertMessageSent("You do not have enough Astral Runes to cast this spell.")
            }

            assertEquals(7, player.count(objs.fire_rune))
            assertEquals(7, player.count(objs.water_rune))
            assertFalse(deps.states.isActive(player))
        }

    @Test
    fun GameTestState.`all cache era steam staves substitute both elemental rune costs`() =
        runMagicImbueTest { deps ->
            val staves =
                listOf(objs.steam_battlestaff, objs.mystic_steam_staff, objs.twinflame_staff)
            for (staff in staves) {
                prepareCaster(deps)
                player.inv[1] = null
                player.inv[2] = null
                player.righthand = InvObj(staff)

                player.withProtectedAccess { deps.magicImbue.cast(this) }

                assertEquals(0, player.count(objs.astral_rune), staff.toString())
                assertTrue(deps.states.isActive(player), staff.toString())
                deps.states.clear(player)
            }
        }

    @Test
    fun GameTestState.`Fountain supplies all runes and suppresses xp`() =
        runMagicImbueTest { deps ->
            prepareCaster(deps, casts = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.magicImbue.cast(this) }

            assertTrue(deps.states.isActive(player))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`login and logout clear volatile state from the persistent client varp`() =
        runMagicImbueTest { deps ->
            prepareCaster(deps)
            player.withProtectedAccess { deps.magicImbue.cast(this) }
            deps.events.publish(SessionStateEvent.Logout(player))
            assertFalse(deps.states.isActive(player))
            assertEquals(0, player.vars[varbits.magic_imbue_active])

            player.setVarBit(varbits.magic_imbue_active, 2)
            deps.events.publish(SessionStateEvent.Login(player))
            assertFalse(deps.states.isActive(player))
            assertEquals(0, player.vars[varbits.magic_imbue_active])
        }

    private fun GameTestScope.prepareCaster(deps: Dependencies, casts: Int = 1) {
        deps.states.clear(player)
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.clearInv()
        player.righthand = null
        client.clear()
        deps.cooldowns.markCast(player, player.currentMapClock)
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 2)
            player.inv[1] = InvObj(objs.fire_rune, casts * 7)
            player.inv[2] = InvObj(objs.water_rune, casts * 7)
        }
    }

    private fun GameTestState.runMagicImbueTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            MagicImbueSpellScript::class,
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
        val magicImbue: MagicImbueSpell,
        val states: MagicImbueStateRepository,
        val spells: MagicSpellRegistry,
        val varbitTypes: VarBitTypeList,
        val dbRows: DbRowTypeList,
        val cooldowns: UtilitySpellCooldownRepository,
        val events: EventBus,
    )
}
