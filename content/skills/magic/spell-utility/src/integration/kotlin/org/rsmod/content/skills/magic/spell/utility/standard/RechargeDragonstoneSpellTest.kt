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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.front
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
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.interf.IfButtonOp

@Execution(ExecutionMode.SAME_THREAD)
class RechargeDragonstoneSpellTest {
    @Test
    fun GameTestState.`cache 238 pins spell unlock recipes and audiovisual identities`() =
        runRechargeDragonstoneTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_recharge_dragonstone))
            assertEquals(2529, spell.obj.id)
            assertEquals(89, spell.levelReq)
            assertEquals(97.5, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.water_rune to 4, objs.astral_rune to 1, objs.soul_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(218, components.magic_spellbook_recharge_dragonstone.id ushr 16)
            assertEquals(141, components.magic_spellbook_recharge_dragonstone.id and 0xFFFF)
            assertEquals(
                listOf("Cast"),
                deps.components[components.magic_spellbook_recharge_dragonstone].op.toList(),
            )
            assertEquals(4409, seqs.quest_lunar_spell_cast_spell_on_group.id)
            assertEquals(282, spotanims.lunar_enchant_dragonstone.id)
            assertEquals(139, synths.enchant_dragon_amulet.id)
            assertEquals(139, varps.legends_quest.id)
            assertEquals(listOf(75), deps.dbRows.types.getValue(85).data[19])
            assertEquals(4493, varbits.fremennik_diary_hard_complete.id)
            assertEquals(16, RechargeDragonstoneRecipes.all.size)
            assertEquals(
                mapOf(
                    1704 to 1712,
                    1706 to 1712,
                    1708 to 1712,
                    1710 to 1712,
                    10362 to 10354,
                    10360 to 10354,
                    10358 to 10354,
                    10356 to 10354,
                    11113 to 11105,
                    11111 to 11105,
                    11109 to 11105,
                    11107 to 11105,
                    11126 to 11118,
                    11124 to 11118,
                    11122 to 11118,
                    11120 to 11118,
                ),
                RechargeDragonstoneRecipes.all.associate { it.input.id to it.output.id },
            )
        }

    @Test
    fun GameTestState.`one cast recharges every ordinary eligible state in place`() =
        runRechargeDragonstoneTest { deps ->
            prepareCaster()
            val recipes = RechargeDragonstoneRecipes.all
            recipes.forEachIndexed { index, recipe -> player.inv[index + 3] = InvObj(recipe.input) }
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.recharge.cast(this) }

            recipes.forEachIndexed { index, recipe ->
                assertEquals(recipe.output.id, player.inv[index + 3]?.id, recipe.input.toString())
            }
            assertEquals(975, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(0, player.count(objs.astral_rune))
            assertEquals(0, player.count(objs.soul_rune))
            assertEquals(0, player.count(objs.water_rune))
            assertEquals(seqs.quest_lunar_spell_cast_spell_on_group.id, player.pendingSequence.id)
            assertEquals(
                spotanims.lunar_enchant_dragonstone.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(139, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`four five six eternal wealth and equipped jewellery are not changed`() =
        runRechargeDragonstoneTest { deps ->
            prepareCaster()
            val excluded =
                listOf(
                    objs.amulet_of_glory_4,
                    objs.amulet_of_glory_5,
                    objs.amulet_of_glory_6,
                    objs.amulet_of_glory_t_4,
                    objs.amulet_of_glory_t_5,
                    objs.amulet_of_glory_t_6,
                    objs.skills_necklace_4,
                    objs.skills_necklace_5,
                    objs.skills_necklace_6,
                    objs.combat_bracelet_4,
                    objs.combat_bracelet_5,
                    objs.combat_bracelet_6,
                    objs.amulet_of_eternal_glory,
                    objs.ring_of_wealth,
                )
            excluded.forEachIndexed { index, obj -> player.inv[index + 3] = InvObj(obj) }
            player.front = InvObj(objs.amulet_of_glory_uncharged)

            player.withProtectedAccess {
                deps.recharge.cast(this)
                assertMessageSent("You don't have any dragonstone jewellery to recharge.")
            }

            excluded.forEachIndexed { index, obj ->
                assertEquals(obj.id, player.inv[index + 3]?.id)
            }
            assertEquals(objs.amulet_of_glory_uncharged.id, player.front?.id)
            assertEquals(1, player.count(objs.astral_rune))
        }

    @Test
    fun GameTestState.`Legends Quest gates necklaces and bracelets without blocking glories`() =
        runRechargeDragonstoneTest { deps ->
            prepareCaster(casts = 2, legendsState = 74)
            player.inv[3] = InvObj(objs.amulet_of_glory_1)
            player.inv[4] = InvObj(objs.skills_necklace_1)
            player.inv[5] = InvObj(objs.combat_bracelet_1)

            player.withProtectedAccess { deps.recharge.cast(this) }
            assertEquals(objs.amulet_of_glory_4.id, player.inv[3]?.id)
            assertEquals(objs.skills_necklace_1.id, player.inv[4]?.id)
            assertEquals(objs.combat_bracelet_1.id, player.inv[5]?.id)

            advance(3)
            player.setVarp(varps.legends_quest, 75)
            player.withProtectedAccess { deps.recharge.cast(this) }
            assertEquals(objs.skills_necklace_4.id, player.inv[4]?.id)
            assertEquals(objs.combat_bracelet_4.id, player.inv[5]?.id)

            advance(3)
            prepareCaster(legendsState = 74)
            player.inv[3] = InvObj(objs.skills_necklace_uncharged)
            player.withProtectedAccess {
                deps.recharge.cast(this)
                assertMessageSent("You need to complete Legends' Quest to recharge this jewellery.")
            }
            assertEquals(1, player.count(objs.astral_rune))
        }

    @Test
    fun GameTestState.`members quest diary spellbook Magic and rune failures are atomic`() =
        runRechargeDragonstoneTest { deps ->
            prepareCaster()
            player.inv[3] = InvObj(objs.amulet_of_glory_uncharged)

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
            player.stats[stats.magic] = 88
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[1] = null
            reject(deps, "You do not have enough Soul Runes to cast this spell.")

            assertEquals(1, player.count(objs.amulet_of_glory_uncharged))
            assertEquals(1, player.count(objs.astral_rune))
            assertEquals(4, player.count(objs.water_rune))
        }

    @Test
    fun GameTestState.`full inventory is atomic and repeated casts use the three tick rate`() =
        runRechargeDragonstoneTest { deps ->
            prepareCaster(casts = 2)
            player.fillInv()
            player.inv[0] = InvObj(objs.astral_rune, 2)
            player.inv[1] = InvObj(objs.soul_rune, 2)
            player.inv[2] = InvObj(objs.water_rune, 8)
            player.inv[3] = InvObj(objs.amulet_of_glory_uncharged)

            player.withProtectedAccess { deps.recharge.cast(this) }
            player.inv[4] = InvObj(objs.amulet_of_glory_uncharged)
            player.withProtectedAccess { deps.recharge.cast(this) }
            advance(2)
            player.withProtectedAccess { deps.recharge.cast(this) }
            assertEquals(objs.amulet_of_glory_uncharged.id, player.inv[4]?.id)

            advance()
            player.withProtectedAccess { deps.recharge.cast(this) }
            assertEquals(objs.amulet_of_glory_4.id, player.inv[4]?.id)
            assertEquals(23, player.count(objs.beer))
        }

    @Test
    fun GameTestState.`water staff and Fountain use shared rune and xp policy`() =
        runRechargeDragonstoneTest { deps ->
            prepareCaster()
            player.inv[2] = null
            player.inv[3] = InvObj(objs.amulet_of_glory_uncharged)
            player.righthand = InvObj(objs.water_battlestaff)
            player.withProtectedAccess { deps.recharge.cast(this) }
            assertEquals(1, player.count(objs.amulet_of_glory_4))

            advance(3)
            prepareCaster(casts = 0)
            player.inv[3] = InvObj(objs.amulet_of_glory_uncharged)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val magicXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.recharge.cast(this) }
            assertEquals(1, player.count(objs.amulet_of_glory_4))
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`spellbook button dispatches Recharge Dragonstone`() =
        runRechargeDragonstoneTest { deps ->
            prepareCaster()
            player.inv[3] = InvObj(objs.amulet_of_glory_uncharged)
            deps.events.publish(
                IfOverlayButton(
                    player,
                    components.magic_spellbook_recharge_dragonstone,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )
            assertEquals(1, player.count(objs.amulet_of_glory_4))
        }

    private fun GameTestScope.prepareCaster(casts: Int = 1, legendsState: Int = 75) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.fremennik_diary_hard_complete, 1)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarp(varps.legends_quest, legendsState)
        player.clearInv()
        player.front = null
        player.righthand = null
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts)
            player.inv[1] = InvObj(objs.soul_rune, casts)
            player.inv[2] = InvObj(objs.water_rune, casts * 4)
        }
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        player.withProtectedAccess {
            deps.recharge.cast(this)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(1, player.count(objs.amulet_of_glory_uncharged))
        assertEquals(0, player.count(objs.amulet_of_glory_4))
    }

    private fun GameTestState.runRechargeDragonstoneTest(
        body: GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            RechargeDragonstoneSpellScript::class,
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
        val recharge: RechargeDragonstoneSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val dbRows: DbRowTypeList,
        val events: EventBus,
    )
}
