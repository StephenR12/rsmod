package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
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
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.interact.NpcTInteractions
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
import org.rsmod.api.type.script.dsl.NpcPluginBuilder
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.npc.UnpackedNpcType

@Execution(ExecutionMode.SAME_THREAD)
class MonsterInspectSpellTest {
    @Test
    fun GameTestState.`cache contracts define Monster Inspect and its presentation`() =
        runMonsterInspectTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_monster_inspect))
            val populate = deps.cacheTypes.clientscripts.getValue(DREAM_MONSTER_POPULATE)

            assertEquals(28180, objs.spell_monster_inspect.id)
            assertEquals(42, spell.levelReq)
            assertEquals(30.5, spell.castXp)
            assertEquals(Spellbook.Standard, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.body_rune to 2, objs.mind_rune to 2),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(198, components.magic_spellbook_monster_inspect.id and 0xFFFF)
            assertEquals(522, interfaces.dream_monster_stat.id)
            assertEquals(0, populate.intArgumentCount)
            assertEquals(1, populate.stringArgumentCount)
            assertEquals("[clientscript,dream_monster_populate]", populate.internalName)
            assertEquals(6293, seqs.dream_player_monsterexam_statspy.id)
            assertEquals(1059, spotanims.dream_spell_monsterexam_statspy_spotanim.id)
            assertEquals(3620, synths.lunar_stat_spy.id)
        }

    @Test
    fun GameTestState.`free world cast consumes exact runes and opens current monster stats`() =
        runMonsterInspectTest { deps ->
            prepareBase(members = false)
            val type = testMonster()
            val npc =
                Npc(type).apply {
                    hitpoints = 73
                    attackLvl = 67
                    defenceLvl = 61
                    strengthLvl = 59
                    magicLvl = 53
                    rangedLvl = 47
                }
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.inspect.cast(this, npc, type) }

            assertEquals(0, player.count(objs.body_rune))
            assertEquals(0, player.count(objs.mind_rune))
            assertEquals(305, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.dream_player_monsterexam_statspy.id, player.pendingSequence.id)
            assertTrue(player.pendingSpotanims.isNotEmpty())
            assertModalOpen(interfaces.dream_monster_stat)
            val payload = client.populatePayload()
            assertTrue("Hitpoints: 73" in payload)
            assertTrue("Attack: 67" in payload)
            assertFalse("Hitpoints: 190" in payload)
        }

    @Test
    fun GameTestState
        .`formatter fills four tabs from combat params and omits zero attack bonuses`() =
        runMonsterInspectTest {
            val type = testMonster()
            val npc = Npc(type)
            val sections = MonsterInspectFormatter.format(npc, type).split('|')

            assertEquals(5, sections.size)
            assertEquals("Pipe safe monster", sections[0])
            assertTrue("Combat level: 321" in sections[1])
            assertTrue("Attack speed: 3" in sections[2])
            assertTrue("Attack bonus: +118" in sections[2])
            assertTrue("Magic bonus: -10" in sections[2])
            assertFalse("Ranged bonus" in sections[2])
            assertFalse("Stab bonus" in sections[2])
            assertTrue("Stab bonus: +42" in sections[3])
            assertTrue("Light ranged bonus: +11" in sections[3])
            assertTrue("Standard ranged bonus: +22" in sections[3])
            assertTrue("Heavy ranged bonus: +33" in sections[3])
            assertTrue("Slayer monster: Yes" in sections[4])
            assertTrue("Poisonable: No" in sections[4])
            assertTrue("Venomable: Poisons instead" in sections[4])
            assertTrue("Freeze resistance: 33%" in sections[4])
            assertTrue("Elemental weakness: Fire (50%)" in sections[4])
        }

    @Test
    fun GameTestState.`level and rune failures do not grant xp or open the interface`() =
        runMonsterInspectTest { deps ->
            val target = Npc(testMonster())

            prepareBase(magicLevel = 41)
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess {
                deps.inspect.cast(this, target)
                assertMessageSent("Your Magic level is not high enough for this spell.")
            }
            assertEquals(2, player.count(objs.body_rune))
            assertEquals(2, player.count(objs.mind_rune))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
            assertModalNotOpen(interfaces.dream_monster_stat)

            prepareBase(bodyRunes = 1)
            player.withProtectedAccess {
                deps.inspect.cast(this, target)
                assertMessageSent("You do not have enough Body Runes to cast this spell.")
            }
            assertEquals(1, player.count(objs.body_rune))
            assertEquals(2, player.count(objs.mind_rune))
            assertModalNotOpen(interfaces.dream_monster_stat)
        }

    @Test
    fun GameTestState.`Fountain of Rune opens inspect without consuming runes or granting xp`() =
        runMonsterInspectTest { deps ->
            prepareBase(bodyRunes = 0, mindRunes = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.inspect.cast(this, Npc(testMonster())) }

            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
            assertModalOpen(interfaces.dream_monster_stat)
            assertEquals(DREAM_MONSTER_POPULATE, client.populateScript().id)
        }

    @Test
    fun GameTestState.`npc target script casts at standard spell range`() =
        runMonsterInspectTest { deps ->
            prepareBase()
            val type = testMonster(id = TEST_NPC_ID)
            val npc = spawnNpc(player.coords.translateX(8), type)

            deps.interactions.interact(
                player = player,
                npc = npc,
                component = components.magic_spellbook_monster_inspect,
                comsub = -1,
                objType = null,
            )
            advance()

            assertModalOpen(interfaces.dream_monster_stat)
            assertEquals(0, player.count(objs.body_rune))
            assertEquals(TEST_NPC_ID, npc.id)
        }

    private fun GameTestScope.prepareBase(
        magicLevel: Int = 99,
        bodyRunes: Int = 2,
        mindRunes: Int = 2,
        members: Boolean = true,
    ) {
        player.members = members
        player.stats[stats.magic] = magicLevel
        player.clearInv()
        player.inv[0] = InvObj(objs.body_rune, bodyRunes)
        player.inv[1] = InvObj(objs.mind_rune, mindRunes)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
    }

    private fun testMonster(id: Int = TEST_NPC_ID): UnpackedNpcType =
        NpcPluginBuilder("test_monster")
            .apply {
                name = "Pipe|safe monster"
                vislevel = 321
                hitpoints = 190
                attack = 101
                defence = 102
                strength = 103
                magic = 104
                ranged = 105
                param[params.attackrate] = 3
                param[params.attack_melee] = 118
                param[params.attack_magic] = -10
                param[params.attack_ranged] = 0
                param[params.melee_strength] = 129
                param[params.ranged_strength] = 35
                param[params.npc_magic_damage_bonus] = 12
                param[params.attack_stab] = 999
                param[params.defence_stab] = 42
                param[params.defence_slash] = 0
                param[params.defence_crush] = -4
                param[params.defence_magic] = 90
                param[params.defence_light] = 11
                param[params.defence_standard] = 22
                param[params.defence_heavy] = 33
                param[params.slayer_experience] = 1900
                param[params.poison_immunity] = 1
                param[params.venom_immunity] = constants.npc_venom_partial_immunity
                param[params.thrall_immunity] = 1
                param[params.burn_immunity] = 1
                param[params.freeze_resistance] = 33
                param[params.demon] = 1
                param[params.draconic] = 1
                param[params.rat] = 0
                param[params.golem] = 0
                param[params.elemental_weakness_type] = constants.elemental_weakness_fire
                param[params.elemental_weakness_percent] = 50
            }
            .build(id)

    private fun org.rsmod.api.testing.capture.CaptureClient.populateScript(): RunClientScript =
        filterIsInstance<RunClientScript>().single { it.id == DREAM_MONSTER_POPULATE }

    private fun org.rsmod.api.testing.capture.CaptureClient.populatePayload(): String =
        populateScript().values.single() as String

    private fun GameTestState.runMonsterInspectTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            MonsterInspectSpellScript::class,
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
        val inspect: MonsterInspectSpell,
        val spells: MagicSpellRegistry,
        val interactions: NpcTInteractions,
        val cacheTypes: TypeListMap,
    )

    private companion object {
        const val DREAM_MONSTER_POPULATE = 1179
        const val TEST_NPC_ID = 65000
    }
}
