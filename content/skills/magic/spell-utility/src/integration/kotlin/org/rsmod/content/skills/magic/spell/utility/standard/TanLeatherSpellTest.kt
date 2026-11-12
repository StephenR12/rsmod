package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.rsmod.content.skills.crafting.tanning.TanningModule
import org.rsmod.content.skills.crafting.tanning.TanningRecipeRegistry
import org.rsmod.events.EventBus
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.varbit.VarBitTypeList

@Execution(ExecutionMode.SAME_THREAD)
class TanLeatherSpellTest {
    @Test
    fun GameTestState.`cache 233 pins Tan Leather spell recipes unlock and presentation`() =
        runTanLeatherTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_tan_leather))
            assertEquals(1741, spell.obj.id)
            assertEquals(78, spell.levelReq)
            assertEquals(81.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.fire_rune to 5, objs.astral_rune to 2, objs.nature_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(218, components.magic_spellbook_tan_leather.id ushr 16)
            assertEquals(122, components.magic_spellbook_tan_leather.id and 0xFFFF)
            assertEquals(
                listOf("Cast", "Setup"),
                deps.components[components.magic_spellbook_tan_leather].op.toList(),
            )
            assertEquals(3014, seqs.tan_leather.id)
            assertEquals(322, spotanims.lunar_tan_leather.id)
            assertEquals(2879, synths.lunar_bake_pie.id)
            assertEquals(4493, varbits.fremennik_diary_hard_complete.id)
            assertEquals(8, deps.recipes.recipes.size)
            assertEquals(
                listOf(
                    objs.leather,
                    objs.hard_leather,
                    objs.snakeskin,
                    objs.snakeskin,
                    objs.green_dragon_leather,
                    objs.blue_dragon_leather,
                    objs.red_dragon_leather,
                    objs.black_dragon_leather,
                ),
                deps.recipes.recipes.map { it.output },
            )

            val varbit = deps.varbits[varbits.fremennik_diary_hard_complete]
            assertEquals(1189, varbit.baseVar.id)
            assertEquals(12..12, varbit.bits)
        }

    @Test
    fun GameTestState.`one cast tans first five mixed supported hides in inventory order`() =
        runTanLeatherTest { deps ->
            prepareCaster(deps)
            val inputs =
                listOf(
                    objs.cow_hide,
                    objs.snake_hide,
                    objs.swamp_snake_hide,
                    objs.green_dragonhide,
                    objs.blue_dragonhide,
                    objs.red_dragonhide,
                    objs.black_dragonhide,
                )
            inputs.forEachIndexed { index, obj -> player.inv[3 + index] = InvObj(obj) }
            val magicXp = player.statMap.getFineXP(stats.magic)
            val craftingXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.tanLeather.cast(this) }

            assertEquals(
                listOf(
                    objs.leather.id,
                    objs.snakeskin.id,
                    objs.snakeskin.id,
                    objs.green_dragon_leather.id,
                    objs.blue_dragon_leather.id,
                    objs.red_dragonhide.id,
                    objs.black_dragonhide.id,
                ),
                (3..9).map { player.inv[it]?.id },
            )
            assertEquals(810, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(craftingXp, player.statMap.getFineXP(stats.crafting))
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.nature_rune))
            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(seqs.tan_leather.id, player.pendingSequence.id)
            assertEquals(
                spotanims.lunar_tan_leather.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(2879, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`Setup selects hard cowhide output and logout restores soft default`() =
        runTanLeatherTest { deps ->
            prepareCaster(deps, casts = 2)
            publish(deps, IfButtonOp.Op2)
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.menu_list, subcomponent = 1)
            )
            assertEquals(CowhideLeather.Hard, deps.setup.get(player))
            advance()
            player.inv[3] = InvObj(objs.cow_hide)
            player.withProtectedAccess { deps.tanLeather.cast(this) }
            assertEquals(1, player.count(objs.hard_leather))

            deps.events.publish(SessionStateEvent.Logout(player))
            advance(3)
            player.inv[3] = InvObj(objs.cow_hide)
            player.withProtectedAccess { deps.tanLeather.cast(this) }
            assertEquals(1, player.count(objs.leather))
        }

    @Test
    fun GameTestState.`partial batch still costs one cast and no hide feedback is atomic`() =
        runTanLeatherTest { deps ->
            prepareCaster(deps, casts = 2)
            player.inv[3] = InvObj(objs.black_dragonhide)
            player.withProtectedAccess { deps.tanLeather.cast(this) }
            assertEquals(1, player.count(objs.black_dragon_leather))
            assertEquals(2, player.count(objs.astral_rune))
            assertEquals(1, player.count(objs.nature_rune))
            assertEquals(5, player.count(objs.fire_rune))

            advance(3)
            player.withProtectedAccess {
                deps.tanLeather.cast(this)
                assertMessageSent("You don't have any suitable hides to tan.")
            }
            assertEquals(2, player.count(objs.astral_rune))
        }

    @Test
    fun GameTestState.`members quest diary spellbook Magic and rune gates are atomic`() =
        runTanLeatherTest { deps ->
            prepareCaster(deps)
            player.inv[3] = InvObj(objs.cow_hide)

            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.fremennik_diary_hard_complete, 0)
            reject(deps, "You need to complete the hard Fremennik Diary to cast this spell.")
            player.setVarBit(varbits.fremennik_diary_hard_complete, 1)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 77
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[0] = null
            reject(deps, "You do not have enough Astral Runes to cast this spell.")

            assertEquals(1, player.count(objs.cow_hide))
            assertEquals(0, player.count(objs.leather))
        }

    @Test
    fun GameTestState.`second click queues once and executes on exact third tick`() =
        runTanLeatherTest { deps ->
            prepareCaster(deps, casts = 3)
            repeat(15) { player.inv[3 + it] = InvObj(objs.green_dragonhide) }

            player.withProtectedAccess { deps.tanLeather.cast(this) }
            player.withProtectedAccess { deps.tanLeather.cast(this) }
            player.withProtectedAccess { deps.tanLeather.cast(this) }
            assertTrue(tan_leather_queues.cast in player.queueList)
            assertEquals(10, player.count(objs.green_dragonhide))
            advance(2)
            assertEquals(10, player.count(objs.green_dragonhide))
            advance()
            assertEquals(5, player.count(objs.green_dragonhide))
            assertEquals(10, player.count(objs.green_dragon_leather))
            advance(3)
            assertEquals(5, player.count(objs.green_dragonhide))
        }

    @Test
    fun GameTestState.`fire staff substitutes runes and Fountain suppresses Magic xp`() =
        runTanLeatherTest { deps ->
            prepareCaster(deps, casts = 0)
            player.inv[0] = InvObj(objs.astral_rune, 2)
            player.inv[1] = InvObj(objs.nature_rune)
            repeat(5) { player.inv[2 + it] = InvObj(objs.blue_dragonhide) }
            player.righthand = InvObj(objs.fire_staff)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.tanLeather.cast(this) }

            assertEquals(5, player.count(objs.blue_dragon_leather))
            assertEquals(810, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.nature_rune))

            advance(3)
            prepareCaster(deps, casts = 0)
            repeat(5) { player.inv[it] = InvObj(objs.blue_dragonhide) }
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val fountainXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.tanLeather.cast(this) }

            assertEquals(5, player.count(objs.blue_dragon_leather))
            assertEquals(fountainXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`overlay Cast operation dispatches the registered spell handler`() =
        runTanLeatherTest { deps ->
            prepareCaster(deps)
            repeat(5) { player.inv[3 + it] = InvObj(objs.red_dragonhide) }

            publish(deps, IfButtonOp.Op1)

            assertEquals(0, player.count(objs.red_dragonhide))
            assertEquals(5, player.count(objs.red_dragon_leather))
        }

    private fun GameTestScope.prepareCaster(deps: Dependencies, casts: Int = 1) {
        player.members = true
        player.stats[stats.magic] = 99
        player.stats[stats.crafting] = 1
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.fremennik_diary_hard_complete, 1)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.clearInv()
        player.righthand = null
        client.clear()
        player.clearQueue(tan_leather_queues.cast)
        deps.setup.set(player, CowhideLeather.Soft)
        deps.cooldowns.markCast(player, player.currentMapClock)
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 2)
            player.inv[1] = InvObj(objs.nature_rune, casts)
            player.inv[2] = InvObj(objs.fire_rune, casts * 5)
        }
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        player.withProtectedAccess {
            deps.tanLeather.cast(this)
            if (message != null) {
                assertMessageSent(message)
            }
        }
    }

    private fun GameTestScope.publish(deps: Dependencies, op: IfButtonOp) {
        deps.events.publish(
            IfOverlayButton(
                player,
                components.magic_spellbook_tan_leather,
                comsub = -1,
                obj = null,
                op = op,
            )
        )
    }

    private fun GameTestState.runTanLeatherTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            TanLeatherSpellScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            install(TanningModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(TanLeatherSetupRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UtilitySpellCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val tanLeather: TanLeatherSpell,
        val setup: TanLeatherSetupRepository,
        val spells: MagicSpellRegistry,
        val recipes: TanningRecipeRegistry,
        val components: ComponentTypeList,
        val varbits: VarBitTypeList,
        val cooldowns: UtilitySpellCooldownRepository,
        val events: EventBus,
    )
}
