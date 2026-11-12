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
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ui.IfOverlayButtonT
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
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.ui.Component

@Execution(ExecutionMode.SAME_THREAD)
class PlankMakeSpellTest {
    @Test
    fun GameTestState.`cache 233 pins Plank Make recipes settings and presentation`() =
        runPlankMakeTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_plank_make))
            assertEquals(11149, spell.obj.id)
            assertEquals(86, spell.levelReq)
            assertEquals(90.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.earth_rune to 15, objs.astral_rune to 2, objs.nature_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(218, components.magic_spellbook_plank_make.id ushr 16)
            assertEquals(133, components.magic_spellbook_plank_make.id and 0xFFFF)
            assertEquals(
                emptyList<String>(),
                deps.components[components.magic_spellbook_plank_make].op.toList(),
            )
            assertEquals(6298, seqs.dream_player_make_plank_spell.id)
            assertEquals(1063, spotanims.dream_plank_make_spell_spotanim.id)
            assertEquals(3617, synths.lunar_make_plank.id)
            assertEquals(28628, objs.sawmill_voucher.id)
            assertEquals(95, plank_make_queues.batch.id)
            assertEquals(96, plank_make_queues.cast.id)
            assertEquals(
                listOf(
                    PlankExpectation(objs.logs.id, objs.plank.id, 70, 624),
                    PlankExpectation(objs.oak_logs.id, objs.oak_plank.id, 175, 2319),
                    PlankExpectation(objs.teak_logs.id, objs.teak_plank.id, 350, 2320),
                    PlankExpectation(objs.mahogany_logs.id, objs.mahogany_plank.id, 1_050, 3939),
                ),
                PlankMakeRecipes.all.map {
                    PlankExpectation(it.input.id, it.output.id, it.fee, it.confirmation.id)
                },
            )
            assertEquals(3618, varbits.dream_mentor.id)
        }

    @Test
    fun GameTestState.`all four logs pay exact fees and output uses first available slot`() =
        runPlankMakeTest { deps ->
            for (recipe in PlankMakeRecipes.all) {
                prepareCaster(casts = 2, coins = recipe.fee + 1)
                player.inv[4] = null
                player.inv[5] = InvObj(objs.bronze_axe)
                player.inv[6] = InvObj(objs.bronze_axe)
                player.inv[7] = InvObj(recipe.input)
                val magicXp = player.statMap.getFineXP(stats.magic)

                player.withProtectedAccess { deps.plankMake.cast(this, targetSlot = 7) }

                assertEquals(recipe.output.id, player.inv[4]?.id)
                assertEquals(null, player.inv[7])
                assertEquals(1, player.count(objs.coins))
                assertEquals(900, player.statMap.getFineXP(stats.magic) - magicXp)
                assertEquals(seqs.dream_player_make_plank_spell.id, player.pendingSequence.id)
                assertEquals(
                    spotanims.dream_plank_make_spell_spotanim.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                assertEquals(3617, client.filterIsInstance<SynthSound>().single().id)
                advance(3)
            }
        }

    @Test
    fun GameTestState.`autocast is immediate then five ticks then six ticks in inventory order`() =
        runPlankMakeTest { deps ->
            prepareCaster(casts = 3, coins = 1_575)
            player.inv[4] = InvObj(objs.oak_logs)
            player.inv[6] = InvObj(objs.teak_logs)
            player.inv[7] = InvObj(objs.mahogany_logs)

            player.withProtectedAccess { deps.plankMake.cast(this, targetSlot = 7) }
            assertEquals(1, player.count(objs.oak_plank))
            assertEquals(1, player.count(objs.mahogany_logs))
            advance(4)
            assertEquals(1, player.count(objs.teak_logs))
            advance()
            assertEquals(1, player.count(objs.teak_plank))
            assertEquals(1, player.count(objs.mahogany_logs))
            advance(5)
            assertEquals(1, player.count(objs.mahogany_logs))
            advance()
            assertEquals(1, player.count(objs.mahogany_plank))
            assertEquals(0, player.count(objs.coins))
            assertEquals(0, player.weakQueueList.count(plank_make_queues.batch))
        }

    @Test
    fun GameTestState.`voucher doubles output only when an empty inventory slot already exists`() =
        runPlankMakeTest { deps ->
            prepareCaster(casts = 1, coins = 70)
            player.inv[4] = InvObj(objs.sawmill_voucher)
            player.inv[5] = InvObj(objs.logs)
            player.withProtectedAccess { deps.plankMake.cast(this, targetSlot = 5) }
            assertEquals(2, player.count(objs.plank))
            assertEquals(0, player.count(objs.sawmill_voucher))

            advance(3)
            prepareCaster(casts = 1, coins = 71)
            player.inv[4] = InvObj(objs.sawmill_voucher)
            player.inv[5] = InvObj(objs.logs)
            for (slot in 6 until player.inv.size) {
                player.inv[slot] = InvObj(objs.bronze_axe)
            }
            assertEquals(0, player.inv.freeSpace())
            player.withProtectedAccess { deps.plankMake.cast(this, targetSlot = 5) }
            assertEquals(1, player.count(objs.plank))
            assertEquals(1, player.count(objs.sawmill_voucher))
        }

    @Test
    fun GameTestState.`members quests spellbook Magic coins runes and target gates are atomic`() =
        runPlankMakeTest { deps ->
            prepareCaster(casts = 1, coins = 70)
            player.inv[4] = InvObj(objs.logs)
            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.dream_mentor, 27)
            reject(deps, "You need to complete Dream Mentor to cast this spell.")
            player.setVarBit(varbits.dream_mentor, 28)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 85
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[3] = null
            reject(deps, "You need 70 coins to convert that log into a plank.")
            player.inv[3] = InvObj(objs.coins, 70)
            player.inv[0] = null
            reject(deps, "You do not have enough Astral Runes to cast this spell.")

            player.inv[4] = InvObj(objs.bronze_axe)
            player.withProtectedAccess {
                deps.plankMake.cast(this, targetSlot = 4)
                assertMessageSent("You can only cast this spell on logs.")
            }
            assertEquals(70, player.count(objs.coins))
            assertEquals(0, player.count(objs.plank))
        }

    @Test
    fun GameTestState.`manual recast queues at tick three and replaces automatic cadence`() =
        runPlankMakeTest { deps ->
            prepareCaster(casts = 2, coins = 140)
            player.inv[4] = InvObj(objs.logs)
            player.inv[5] = InvObj(objs.logs)

            player.withProtectedAccess { deps.plankMake.cast(this, targetSlot = 5) }
            player.withProtectedAccess { deps.plankMake.cast(this, targetSlot = 5) }
            player.withProtectedAccess { deps.plankMake.cast(this, targetSlot = 5) }
            assertEquals(1, player.count(objs.plank))
            assertEquals(1, player.queueList.count(plank_make_queues.cast))
            assertEquals(0, player.weakQueueList.count(plank_make_queues.batch))
            advance(2)
            assertEquals(1, player.count(objs.plank))
            advance()
            assertEquals(2, player.count(objs.plank))
            assertEquals(0, player.queueList.count(plank_make_queues.cast))
        }

    @Test
    fun GameTestState.`earth staff substitutes runes and Fountain suppresses Magic xp`() =
        runPlankMakeTest { deps ->
            prepareCaster(casts = 0, coins = 70)
            player.inv[0] = InvObj(objs.astral_rune, 2)
            player.inv[1] = InvObj(objs.nature_rune)
            player.inv[4] = InvObj(objs.logs)
            player.righthand = InvObj(objs.earth_battlestaff)
            val magicXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.plankMake.cast(this, targetSlot = 4) }
            assertEquals(1, player.count(objs.plank))
            assertEquals(900, player.statMap.getFineXP(stats.magic) - magicXp)

            advance(3)
            prepareCaster(casts = 0, coins = 70)
            player.inv[4] = InvObj(objs.logs)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val fountainXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.plankMake.cast(this, targetSlot = 4) }
            assertEquals(1, player.count(objs.plank))
            assertEquals(fountainXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`inventory target event dispatches Plank Make`() = runPlankMakeTest { deps ->
        prepareCaster(casts = 1, coins = 175)
        player.inv[4] = InvObj(objs.oak_logs)
        deps.events.publish(
            IfOverlayButtonT(
                player = player,
                selectedSlot = -1,
                selectedObj = objTypes[objs.spell_plank_make],
                targetSlot = 4,
                targetObj = objTypes[objs.oak_logs],
                selectedComponent = Component(components.magic_spellbook_plank_make.packed),
                targetComponent = Component(components.inv_items.packed),
            )
        )
        assertEquals(1, player.count(objs.oak_plank))
    }

    private fun GameTestScope.prepareCaster(casts: Int, coins: Int) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.dream_mentor, 28)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.clearInv()
        player.righthand = null
        client.clear()
        player.clearQueue(plank_make_queues.cast)
        player.clearWeakQueue(plank_make_queues.batch)
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 2)
            player.inv[1] = InvObj(objs.nature_rune, casts)
            player.inv[2] = InvObj(objs.earth_rune, casts * 15)
        }
        if (coins > 0) {
            player.inv[3] = InvObj(objs.coins, coins)
        }
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        player.withProtectedAccess {
            deps.plankMake.cast(this, targetSlot = 4)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(1, player.count(objs.logs))
        assertEquals(0, player.count(objs.plank))
    }

    private fun GameTestState.runPlankMakeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            PlankMakeSpellScript::class,
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

    private data class PlankExpectation(
        val input: Int,
        val output: Int,
        val fee: Int,
        val confirmation: Int,
    )

    data class Dependencies
    @Inject
    constructor(
        val plankMake: PlankMakeSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val events: EventBus,
    )
}
