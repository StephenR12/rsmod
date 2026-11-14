package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.back
import org.rsmod.api.player.hands
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
import org.rsmod.content.skills.smithing.smelting.SmeltingModule
import org.rsmod.content.skills.smithing.smelting.SmeltingRecipeRegistry
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class SuperheatItemSpellTest {
    @Test
    fun GameTestState.`cache contracts define Superheat and its cutoff recipe set`() =
        runSuperheatTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_superheat))
            assertEquals(43, spell.levelReq)
            assertEquals(53.0, spell.castXp)
            assertEquals(Spellbook.Standard, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.fire_rune to 4, objs.nature_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )

            assertEquals(11, deps.recipes.recipes.size)
            assertEquals(
                setOf(
                    objs.bronze_bar,
                    objs.iron_bar,
                    objs.silver_bar,
                    objs.steel_bar,
                    objs.gold_bar,
                    objs.lovakite_bar,
                    objs.mithril_bar,
                    objs.adamantite_bar,
                    objs.runite_bar,
                    objs.blurite_bar,
                    objs.lunar_bar,
                ),
                deps.recipes.recipes.mapTo(mutableSetOf()) { it.output },
            )
            assertEquals(36, components.magic_spellbook_superheat.id and 0xFFFF)
            assertEquals(725, seqs.human_castsuperheatitem.id)
            assertEquals(687, seqs.superheatitem_casting.id)
            assertEquals(148, spotanims.superheatitem_casting.id)
            assertEquals(190, synths.superheat_all.id)
            assertEquals(191, synths.superheat_fail.id)
        }

    @Test
    fun GameTestState.`quest ores require their historical progress gates`() =
        runSuperheatTest { deps ->
            prepareBase(smithingLevel = 99)
            player.inv[1] = InvObj(objs.blurite_ore)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.blurite_ore))
            assertEquals(0, player.count(objs.blurite_bar))

            player.setVarp(varps.squire, 7)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.blurite_bar))

            advance(3)
            prepareBase(smithingLevel = 1)
            player.inv[1] = InvObj(objs.lunar_ore)
            player.setVarBit(varbits.lunar_quest_main, 154)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.lunar_ore))
            assertEquals(0, player.count(objs.lunar_bar))

            player.setVarBit(varbits.lunar_quest_main, 155)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.lunar_bar))
        }

    @Test
    fun GameTestState.`quest ores use cache-cutoff levels and exact Smithing xp`() =
        runSuperheatTest { deps ->
            prepareBase(smithingLevel = 13)
            player.setVarp(varps.squire, 7)
            player.inv[1] = InvObj(objs.blurite_ore)
            val bluriteXp = player.statMap.getFineXP(stats.smithing)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.blurite_bar))
            assertEquals(80, player.statMap.getFineXP(stats.smithing) - bluriteXp)

            advance(3)
            prepareBase(smithingLevel = 1)
            player.setVarBit(varbits.lunar_quest_main, 155)
            player.inv[1] = InvObj(objs.lunar_ore)
            val lunarXp = player.statMap.getFineXP(stats.smithing)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.lunar_bar))
            assertEquals(62, player.statMap.getFineXP(stats.smithing) - lunarXp)
        }

    @Test
    fun GameTestState.`every cache-233 metal recipe consumes its ores and grants exact xp`() =
        runSuperheatTest { deps ->
            val cases =
                listOf(
                    RecipeCase(objs.copper_ore, listOf(objs.tin_ore), objs.bronze_bar, 62),
                    RecipeCase(objs.iron_ore, emptyList(), objs.iron_bar, 125),
                    RecipeCase(objs.silver_ore, emptyList(), objs.silver_bar, 137),
                    RecipeCase(objs.iron_ore, List(2) { objs.coal }, objs.steel_bar, 175),
                    RecipeCase(objs.gold_ore, emptyList(), objs.gold_bar, 225),
                    RecipeCase(objs.lovakite_ore, List(2) { objs.coal }, objs.lovakite_bar, 200),
                    RecipeCase(objs.mithril_ore, List(4) { objs.coal }, objs.mithril_bar, 300),
                    RecipeCase(
                        objs.adamantite_ore,
                        List(6) { objs.coal },
                        objs.adamantite_bar,
                        375,
                    ),
                    RecipeCase(objs.runite_ore, List(8) { objs.coal }, objs.runite_bar, 500),
                )

            for (case in cases) {
                player.stats[stats.magic] = 99
                player.stats[stats.smithing] = 99
                player.clearInv()
                player.righthand = InvObj(objs.fire_staff)
                player.inv[0] = InvObj(objs.nature_rune)
                player.inv[1] = InvObj(case.target)
                case.materials.forEachIndexed { index, obj -> player.inv[index + 2] = InvObj(obj) }
                val magicXp = player.statMap.getFineXP(stats.magic)
                val smithingXp = player.statMap.getFineXP(stats.smithing)

                player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }

                assertEquals(1, player.count(case.output), case.output.toString())
                assertEquals(530, player.statMap.getFineXP(stats.magic) - magicXp)
                assertEquals(
                    case.smithingFineXp,
                    player.statMap.getFineXP(stats.smithing) - smithingXp,
                )
                assertEquals(seqs.human_castsuperheatitem.id, player.pendingSequence.id)
                assertTrue(player.pendingSpotanims.isNotEmpty())
                advance(3)
            }
        }

    @Test
    fun GameTestState.`either copper or tin can be selected to make bronze`() =
        runSuperheatTest { deps ->
            prepareBase()
            player.inv[1] = InvObj(objs.tin_ore)
            player.inv[2] = InvObj(objs.copper_ore)

            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }

            assertEquals(1, player.count(objs.bronze_bar))
            assertEquals(0, player.count(objs.copper_ore))
            assertEquals(0, player.count(objs.tin_ore))
        }

    @Test
    fun GameTestState.`iron becomes steel only when level and effective coal cost allow it`() =
        runSuperheatTest { deps ->
            prepareBase(smithingLevel = 29)
            player.inv[1] = InvObj(objs.iron_ore)
            player.inv[2] = InvObj(objs.coal)
            player.inv[3] = InvObj(objs.coal)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.iron_bar))
            assertEquals(2, player.count(objs.coal))

            advance(3)
            prepareBase(smithingLevel = 30)
            player.inv[1] = InvObj(objs.iron_ore)
            player.inv[2] = InvObj(objs.coal)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.iron_bar))
            assertEquals(1, player.count(objs.coal))

            advance(3)
            prepareBase(smithingLevel = 30)
            player.inv[1] = InvObj(objs.iron_ore)
            player.inv[2] = InvObj(objs.coal)
            player.inv[3] = InvObj(objs.smithing_catalyst)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.steel_bar))
            assertEquals(0, player.count(objs.coal))
            assertEquals(0, player.count(objs.smithing_catalyst))
        }

    @Test
    fun GameTestState.`catalyst halves coal and doubles xp only for coal recipes`() =
        runSuperheatTest { deps ->
            prepareBase()
            player.inv[1] = InvObj(objs.mithril_ore)
            player.inv[2] = InvObj(objs.coal)
            player.inv[3] = InvObj(objs.coal)
            player.inv[4] = InvObj(objs.smithing_catalyst)
            val smithingXp = player.statMap.getFineXP(stats.smithing)

            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }

            assertEquals(1, player.count(objs.mithril_bar))
            assertEquals(0, player.count(objs.coal))
            assertEquals(0, player.count(objs.smithing_catalyst))
            assertEquals(600, player.statMap.getFineXP(stats.smithing) - smithingXp)

            advance(3)
            prepareBase()
            player.inv[1] = InvObj(objs.gold_ore)
            player.inv[2] = InvObj(objs.smithing_catalyst)
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            assertEquals(1, player.count(objs.gold_bar))
            assertEquals(1, player.count(objs.smithing_catalyst))
        }

    @Test
    fun GameTestState.`coal bag is consumed only after loose inventory coal`() =
        runSuperheatTest { deps ->
            prepareBase()
            player.inv[1] = InvObj(objs.mithril_ore)
            player.inv[2] = InvObj(objs.coal)
            player.inv[3] = InvObj(objs.coal)
            player.inv[4] = InvObj(objs.coal)
            player.inv[5] = InvObj(objs.coal_bag)
            player.setVarBit(varbits.coal_bag_storage_count, 2)

            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }

            assertEquals(1, player.count(objs.mithril_bar))
            assertEquals(0, player.count(objs.coal))
            assertEquals(1, player.vars[varbits.coal_bag_storage_count])
        }

    @Test
    fun GameTestState.`stored coal is ignored when no coal bag is held`() =
        runSuperheatTest { deps ->
            prepareBase()
            player.inv[1] = InvObj(objs.mithril_ore)
            player.setVarBit(varbits.coal_bag_storage_count, 30)

            player.withProtectedAccess {
                deps.superheat.cast(this, targetSlot = 1)
                assertMessageSent("You do not have enough coal to smelt this ore.")
            }

            assertEquals(1, player.count(objs.mithril_ore))
            assertEquals(1, player.count(objs.nature_rune))
            assertEquals(30, player.vars[varbits.coal_bag_storage_count])
        }

    @Test
    fun GameTestState.`goldsmith gauntlets both Smithing capes and max cape grant 56 point 2 xp`() =
        runSuperheatTest { deps ->
            val equipment =
                listOf(
                    objs.goldsmith_gauntlets,
                    objs.smithing_skillcape,
                    objs.smithing_skillcape_t,
                    objs.max_cape_worn,
                )
            for (item in equipment) {
                prepareBase()
                player.inv[1] = InvObj(objs.gold_ore)
                if (item.id == objs.goldsmith_gauntlets.id) {
                    player.hands = InvObj(item)
                    player.back = null
                } else {
                    player.hands = null
                    player.back = InvObj(item)
                }
                val smithingXp = player.statMap.getFineXP(stats.smithing)

                player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }

                assertEquals(562, player.statMap.getFineXP(stats.smithing) - smithingXp)
                advance(3)
            }
        }

    @Test
    fun GameTestState.`invalid noted and elemental targets fail without consuming runes`() =
        runSuperheatTest { deps ->
            prepareBase()
            player.inv[1] = InvObj(objs.bronze_axe)
            player.withProtectedAccess {
                deps.superheat.cast(this, targetSlot = 1)
                assertMessageSent("You need to cast superheat item on ore.")
            }
            assertEquals(1, player.count(objs.nature_rune))
            assertEquals(seqs.superheatitem_casting.id, player.pendingSequence.id)

            advance(3)
            prepareBase()
            val notedGold = objTypes.cert(objTypes[objs.gold_ore])
            player.inv[1] = InvObj(notedGold)
            player.withProtectedAccess {
                deps.superheat.cast(this, targetSlot = 1)
                assertMessageSent("You need to cast superheat item on ore.")
            }
            assertEquals(1, player.count(notedGold))
            assertEquals(1, player.count(objs.nature_rune))

            advance(3)
            prepareBase()
            player.inv[1] = InvObj(objs.elemental_ore)
            player.withProtectedAccess {
                deps.superheat.cast(this, targetSlot = 1)
                assertMessageSent("Even this spell is not hot enough to heat this item.")
            }
            assertEquals(1, player.count(objs.elemental_ore))
            assertEquals(1, player.count(objs.nature_rune))
        }

    @Test
    fun GameTestState.`level and material failures are atomic`() = runSuperheatTest { deps ->
        prepareBase(smithingLevel = 49)
        player.inv[1] = InvObj(objs.mithril_ore)
        repeat(4) { player.inv[it + 2] = InvObj(objs.coal) }
        player.withProtectedAccess {
            deps.superheat.cast(this, targetSlot = 1)
            assertMessageSent("You need a Smithing level of 50 to smelt this ore.")
        }
        assertEquals(1, player.count(objs.mithril_ore))
        assertEquals(4, player.count(objs.coal))
        assertEquals(1, player.count(objs.nature_rune))

        prepareBase()
        player.inv[1] = InvObj(objs.copper_ore)
        player.withProtectedAccess {
            deps.superheat.cast(this, targetSlot = 1)
            assertMessageSent("You do not have enough ore to make a bronze bar.")
        }
        assertEquals(1, player.count(objs.copper_ore))
        assertEquals(1, player.count(objs.nature_rune))
    }

    @Test
    fun GameTestState.`jogre bones become burnt and grant Cooking and Magic xp`() =
        runSuperheatTest { deps ->
            prepareBase()
            player.stats[stats.cooking] = 30
            player.inv[1] = InvObj(objs.jogre_bones)
            val magicXp = player.statMap.getFineXP(stats.magic)
            val cookingXp = player.statMap.getFineXP(stats.cooking)

            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }

            assertEquals(0, player.count(objs.jogre_bones))
            assertEquals(1, player.count(objs.burnt_jogre_bones))
            assertEquals(530, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(250, player.statMap.getFineXP(stats.cooking) - cookingXp)
        }

    @Test
    fun GameTestState.`jogre bones require unboosted level 30 Cooking`() =
        runSuperheatTest { deps ->
            prepareBase()
            player.stats[stats.cooking] = 29
            player.statMap.setCurrentLevel(stats.cooking, 30.toByte())
            player.inv[1] = InvObj(objs.jogre_bones)

            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }

            assertEquals(1, player.count(objs.jogre_bones))
            assertEquals(0, player.count(objs.burnt_jogre_bones))
            assertEquals(1, player.count(objs.nature_rune))
        }

    @Test
    fun GameTestState.`fire runes are consumed when no staff substitutes for them`() =
        runSuperheatTest { deps ->
            prepareBase()
            player.righthand = null
            player.inv[1] = InvObj(objs.fire_rune, 4)
            player.inv[2] = InvObj(objs.iron_ore)

            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 2) }

            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(0, player.count(objs.nature_rune))
            assertEquals(1, player.count(objs.iron_bar))
        }

    @Test
    fun GameTestState.`one extra cast queues during the three tick delay`() =
        runSuperheatTest { deps ->
            prepareBase()
            player.inv[0] = InvObj(objs.nature_rune, 3)
            player.inv[1] = InvObj(objs.iron_ore)
            player.inv[2] = InvObj(objs.iron_ore)
            player.inv[3] = InvObj(objs.iron_ore)

            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 1) }
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 2) }
            player.withProtectedAccess { deps.superheat.cast(this, targetSlot = 3) }
            assertEquals(1, player.count(objs.iron_bar))
            assertEquals(2, player.count(objs.iron_ore))
            assertEquals(1, player.queueList.count(superheat_queues.cast))

            advance(3)

            assertEquals(2, player.count(objs.iron_bar))
            assertEquals(1, player.count(objs.iron_ore))
            assertEquals(0, player.queueList.count(superheat_queues.cast))
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.prepareBase(smithingLevel: Int = 99) {
        player.stats[stats.magic] = 99
        player.stats[stats.smithing] = smithingLevel
        player.clearInv()
        player.hands = null
        player.back = null
        player.righthand = InvObj(objs.fire_staff)
        player.inv[0] = InvObj(objs.nature_rune)
        player.setVarBit(varbits.coal_bag_storage_count, 0)
    }

    private fun GameTestState.runSuperheatTest(
        body: org.rsmod.api.testing.scope.GameTestScope.(Dependencies) -> Unit
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
            SuperheatItemSpellScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            install(SmeltingModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UtilitySpellCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    private data class RecipeCase(
        val target: ObjType,
        val materials: List<ObjType>,
        val output: ObjType,
        val smithingFineXp: Int,
    )

    data class Dependencies
    @Inject
    constructor(
        val superheat: SuperheatItemSpell,
        val spells: MagicSpellRegistry,
        val recipes: SmeltingRecipeRegistry,
    )
}
