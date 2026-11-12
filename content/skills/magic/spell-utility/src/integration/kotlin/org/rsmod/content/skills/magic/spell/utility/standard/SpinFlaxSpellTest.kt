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
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
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
import org.rsmod.content.skills.fletching.bowstringing.BowStringSpool
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class SpinFlaxSpellTest {
    @Test
    fun GameTestState.`cache 233 pins the Spin Flax spell and presentation contract`() =
        runSpinFlaxTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_spin_flax))
            assertEquals(20589, spell.obj.id)
            assertEquals(76, spell.levelReq)
            assertEquals(75.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 1, objs.nature_rune to 2, objs.air_rune to 5),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(146, components.magic_spellbook_spin_flax.id and 0xFFFF)
            assertEquals(1779, objs.flax.id)
            assertEquals(1777, objs.bow_string.id)
            assertEquals(31052, objs.bow_string_spool.id)
            assertEquals(4413, seqs.lunar_human_magic_summon2.id)
            assertEquals(728, spotanims.quest_lunar_magic_flax_strim.id)
            assertEquals(3615, synths.lunar_hunter_kit.id)
            assertEquals(17291, varbits.bow_string_spool_charges.id)
            assertEquals(17293, varbits.bow_string_spool_size.id)
            assertEquals(listOf(190), deps.dbRows.types.getValue(88).data[19])
            assertTrue(deps.objs.types.getValue(objs.flax.id).members)
            assertFalse(deps.objs.types.getValue(objs.flax.id).isStackable)
        }

    @Test
    fun GameTestState.`one cast spins at most five flax with exact xp runes and presentation`() =
        runSpinFlaxTest { deps ->
            prepareCaster(deps)
            repeat(7) { player.inv[3 + it] = InvObj(objs.flax) }
            val magicXp = player.statMap.getFineXP(stats.magic)
            val craftingXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.spinFlax.cast(this) }

            assertEquals(2, player.count(objs.flax))
            assertEquals(5, player.count(objs.bow_string))
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.nature_rune))
            assertEquals(0, player.count(objs.air_rune))
            assertEquals(750, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(750, player.statMap.getFineXP(stats.crafting) - craftingXp)
            assertEquals(seqs.lunar_human_magic_summon2.id, player.pendingSequence.id)
            assertEquals(
                spotanims.quest_lunar_magic_flax_strim.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(3615, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`partial casts award per flax Crafting xp and no flax has exact feedback`() =
        runSpinFlaxTest { deps ->
            prepareCaster(deps, casts = 2)
            player.inv[3] = InvObj(objs.flax)
            player.inv[4] = InvObj(objs.flax)
            val magicXp = player.statMap.getFineXP(stats.magic)
            val craftingXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.spinFlax.cast(this) }

            assertEquals(2, player.count(objs.bow_string))
            assertEquals(300, player.statMap.getFineXP(stats.crafting) - craftingXp)
            assertEquals(750, player.statMap.getFineXP(stats.magic) - magicXp)
            advance(5)
            player.withProtectedAccess {
                deps.spinFlax.cast(this)
                assertMessageSent("You don't have any flax to spin.")
            }
            assertEquals(1, player.count(objs.astral_rune))
        }

    @Test
    fun GameTestState.`spool stores produced strings and partial capacity spills loose output`() =
        runSpinFlaxTest { deps ->
            prepareCaster(deps, casts = 2)
            player.inv[3] = InvObj(objs.bow_string_spool)
            repeat(5) { player.inv[4 + it] = InvObj(objs.flax) }

            player.withProtectedAccess { deps.spinFlax.cast(this) }

            player.withProtectedAccess { assertEquals(5, deps.spool.charges(this)) }
            assertEquals(0, player.count(objs.bow_string))

            advance(5)
            repeat(5) { player.inv[4 + it] = InvObj(objs.flax) }
            player.setVarBit(varbits.bow_string_spool_charges, 248)
            player.withProtectedAccess { deps.spinFlax.cast(this) }

            assertEquals(250, player.vars[varbits.bow_string_spool_charges])
            assertEquals(3, player.count(objs.bow_string))
        }

    @Test
    fun GameTestState.`members quest spellbook Magic and rune failures are atomic`() =
        runSpinFlaxTest { deps ->
            prepareCaster(deps)
            repeat(5) { player.inv[3 + it] = InvObj(objs.flax) }

            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 75
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[0] = null
            reject(deps, "You do not have enough Astral Runes to cast this spell.")

            assertEquals(5, player.count(objs.flax))
            assertEquals(0, player.count(objs.bow_string))
            assertEquals(2, player.count(objs.nature_rune))
            assertEquals(5, player.count(objs.air_rune))
        }

    @Test
    fun GameTestState.`second click queues once and executes on the exact fifth tick`() =
        runSpinFlaxTest { deps ->
            prepareCaster(deps, casts = 3)
            repeat(15) { player.inv[3 + it] = InvObj(objs.flax) }

            player.withProtectedAccess { deps.spinFlax.cast(this) }
            player.withProtectedAccess { deps.spinFlax.cast(this) }
            player.withProtectedAccess { deps.spinFlax.cast(this) }
            assertTrue(spin_flax_queues.cast in player.queueList)
            assertEquals(10, player.count(objs.flax))
            advance(4)
            assertEquals(10, player.count(objs.flax))
            advance()
            assertEquals(5, player.count(objs.flax))
            assertEquals(10, player.count(objs.bow_string))
            advance(5)
            assertEquals(5, player.count(objs.flax))
            assertEquals(1, player.count(objs.astral_rune))
        }

    @Test
    fun GameTestState
        .`full inventory air staff Fountain and drained Crafting follow shared policies`() =
        runSpinFlaxTest { deps ->
            prepareCaster(deps)
            player.fillInv()
            player.inv[0] = InvObj(objs.astral_rune)
            player.inv[1] = InvObj(objs.nature_rune, 2)
            player.inv[2] = InvObj(objs.flax)
            player.righthand = InvObj(objs.air_battlestaff)
            player.stats[stats.crafting] = 1
            val initialCraftingXp = player.statMap.getFineXP(stats.crafting)
            player.withProtectedAccess { deps.spinFlax.cast(this) }
            assertEquals(1, player.count(objs.bow_string))
            assertEquals(150, player.statMap.getFineXP(stats.crafting) - initialCraftingXp)

            advance(5)
            prepareCaster(deps, casts = 0)
            repeat(5) { player.inv[it] = InvObj(objs.flax) }
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val magicXp = player.statMap.getFineXP(stats.magic)
            val craftingXp = player.statMap.getFineXP(stats.crafting)
            player.withProtectedAccess { deps.spinFlax.cast(this) }
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
            assertEquals(750, player.statMap.getFineXP(stats.crafting) - craftingXp)
            assertEquals(5, player.count(objs.bow_string))
        }

    @Test
    fun GameTestState.`overlay spell click dispatches the registered handler`() =
        runSpinFlaxTest { deps ->
            prepareCaster(deps)
            repeat(5) { player.inv[3 + it] = InvObj(objs.flax) }

            deps.events.publish(
                IfOverlayButton(
                    player,
                    components.magic_spellbook_spin_flax,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )

            assertEquals(0, player.count(objs.flax))
            assertEquals(5, player.count(objs.bow_string))
        }

    private fun GameTestScope.prepareCaster(deps: Dependencies, casts: Int = 1) {
        player.members = true
        player.stats[stats.magic] = 99
        player.stats[stats.crafting] = 1
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.bow_string_spool_charges, 0)
        player.setVarBit(varbits.bow_string_spool_size, 0)
        player.clearInv()
        player.righthand = null
        client.clear()
        player.clearQueue(spin_flax_queues.cast)
        deps.cooldowns.markCast(player, player.currentMapClock)
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts)
            player.inv[1] = InvObj(objs.nature_rune, casts * 2)
            player.inv[2] = InvObj(objs.air_rune, casts * 5)
        }
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        player.withProtectedAccess {
            deps.spinFlax.cast(this)
            if (message != null) {
                assertMessageSent(message)
            }
        }
    }

    private fun GameTestState.runSpinFlaxTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            SpinFlaxSpellScript::class,
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
            bind(BowStringSpool::class.java).`in`(Scopes.SINGLETON)
            bind(UtilitySpellCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val spinFlax: SpinFlaxSpell,
        val spool: BowStringSpool,
        val spells: MagicSpellRegistry,
        val dbRows: DbRowTypeList,
        val objs: ObjTypeList,
        val cooldowns: UtilitySpellCooldownRepository,
        val events: EventBus,
    )
}
