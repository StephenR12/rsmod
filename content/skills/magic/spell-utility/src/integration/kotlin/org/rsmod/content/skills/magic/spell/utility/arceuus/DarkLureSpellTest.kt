package org.rsmod.content.skills.magic.spell.utility.arceuus

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
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
import org.rsmod.api.config.refs.timers
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
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.inv.InvObj
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.comp.ComponentTypeList

@Execution(ExecutionMode.SAME_THREAD)
class DarkLureSpellTest {
    @Test
    fun GameTestState.`cache 238 pins spell presentation and timing contracts`() =
        runDarkLureTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_dark_lure))
            assertEquals(15_303, spell.obj.id)
            assertEquals(50, spell.levelReq)
            assertEquals(60.0, spell.castXp)
            assertEquals(Spellbook.Arceuus, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.death_rune to 1, objs.nature_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(218, components.magic_spellbook_dark_lure.id ushr 16)
            assertEquals(187, components.magic_spellbook_dark_lure.id and 0xFFFF)
            assertEquals("Cast", deps.components[components.magic_spellbook_dark_lure].targetVerb)
            assertEquals(1_882, spotanims.dark_lure_cast.id)
            assertEquals(1_883, spotanims.dark_lure_travel.id)
            assertEquals(1_884, spotanims.dark_lure_hit.id)
            assertEquals(
                seqs.dark_lure_cast_effect.id,
                deps.cacheTypes.spotanims[spotanims.dark_lure_cast].anim,
            )
            assertEquals(
                seqs.dark_lure_travel_effect.id,
                deps.cacheTypes.spotanims[spotanims.dark_lure_travel].anim,
            )
            assertEquals(
                seqs.dark_lure_impact_effect.id,
                deps.cacheTypes.spotanims[spotanims.dark_lure_hit].anim,
            )
            assertEquals(8_999, seqs.dark_lure_cast_effect.id)
            assertEquals(9_000, seqs.dark_lure_travel_effect.id)
            assertEquals(9_001, seqs.dark_lure_impact_effect.id)
            assertEquals(12_289, varbits.arceuus_dark_lure_cooldown.id)
            assertEquals(36, timers.magic_arceuus_dark_lure_cooldown.id)
            assertEquals(37, timers.magic_arceuus_dark_lure_follow.id)
            assertEquals(5, DarkLureSpell.CAST_RATE)
            assertEquals(10, DarkLureSpell.FOLLOW_CYCLES)
        }

    @Test
    fun GameTestState.`cast consumes exact runes awards xp and launches cache graphics`() =
        runDarkLureTest { deps ->
            prepareCaster(casts = 1)
            val target = spawnMobileNpc(60_100, attackable = false)

            player.withProtectedAccess { deps.spell.cast(this, target) }

            assertEquals(0, player.count(objs.death_rune))
            assertEquals(0, player.count(objs.nature_rune))
            assertEquals(600, player.statMap.getFineXP(stats.magic))
            assertEquals(seqs.human_spellcast_grasp.id, player.pendingSequence.id)
            assertEquals(
                spotanims.dark_lure_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(1, player.vars[varbits.arceuus_dark_lure_cooldown])

            advance(DarkLureSpell.CAST_RATE)
            assertEquals(0, player.vars[varbits.arceuus_dark_lure_cooldown])
        }

    @Test
    fun GameTestState.`mobile noncombat target follows for ten cycles then resumes default mode`() =
        runDarkLureTest { deps ->
            prepareCaster(casts = 1)
            val target = spawnMobileNpc(60_101, attackable = false, name = "Baby impling")

            player.withProtectedAccess { deps.spell.cast(this, target) }
            advance(2)

            assertEquals(NpcMode.PlayerFollow, target.mode)
            advance(DarkLureSpell.FOLLOW_CYCLES - 1)
            assertEquals(NpcMode.PlayerFollow, target.mode)
            advance()
            assertEquals(target.defaultMode, target.mode)
        }

    @Test
    fun GameTestState.`mobile attackable target attacks the caster on impact`() =
        runDarkLureTest { deps ->
            prepareCaster(casts = 1)
            val target = spawnMobileNpc(60_102, attackable = true)

            player.withProtectedAccess { deps.spell.cast(this, target) }
            advance(2)

            assertEquals(NpcMode.OpPlayer2, target.mode)
        }

    @Test
    fun GameTestState.`noncombat non impling target is not forced to follow`() =
        runDarkLureTest { deps ->
            prepareCaster(casts = 1)
            val target = spawnMobileNpc(60_105, attackable = false)

            player.withProtectedAccess { deps.spell.cast(this, target) }
            advance(2)

            assertEquals(target.defaultMode, target.mode)
        }

    @Test
    fun GameTestState.`access level rune and immobile paths are atomic`() =
        runDarkLureTest { deps ->
            prepareCaster(casts = 1)
            val mobile = spawnMobileNpc(60_103, attackable = false)
            player.members = false
            reject(deps, mobile)
            player.members = true
            player.setVarBit(varbits.a_kingdom_divided, 149)
            reject(deps, mobile)
            player.setVarBit(varbits.a_kingdom_divided, 150)
            player.stats[stats.magic] = 49
            reject(deps, mobile)
            player.stats[stats.magic] = 99
            player.inv[1] = null
            reject(deps, mobile)

            prepareCaster(casts = 1)
            val immobile =
                spawnNpc(
                    player.coords.translateX(1),
                    npcTypeFactory.create(60_104) {
                        name = "Immobile creature"
                        hitpoints = 10
                        moveRestrict = MoveRestrict.NoMove
                    },
                )
            reject(deps, immobile)

            prepareCaster(casts = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            player.withProtectedAccess { deps.spell.cast(this, mobile) }
            assertEquals(0, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.reject(deps: Dependencies, target: Npc) {
        val inventory = player.inv.toList()
        val xp = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess { deps.spell.cast(this, target) }
        assertEquals(inventory, player.inv.toList())
        assertEquals(xp, player.statMap.getFineXP(stats.magic))
        assertEquals(0, player.vars[varbits.arceuus_dark_lure_cooldown])
    }

    private fun GameTestScope.prepareCaster(casts: Int) {
        player.clearAllInvs()
        player.members = true
        player.setBaseLevel(stats.magic, 99)
        player.setCurrentLevel(stats.magic, 99)
        player.statMap.setXP(stats.magic, 0)
        player.setVarBit(varbits.a_kingdom_divided, 150)
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.arceuus_dark_lure_cooldown, 0)
        player.righthand = null
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.death_rune, casts)
            player.inv[1] = InvObj(objs.nature_rune, casts)
        }
    }

    private fun GameTestScope.spawnMobileNpc(
        id: Int,
        attackable: Boolean,
        name: String = "Mobile creature",
    ): Npc =
        spawnNpc(
            player.coords.translateX(1),
            npcTypeFactory.create(id) {
                this.name = name
                hitpoints = 10
                size = 1
                if (attackable) {
                    op[1] = "Attack"
                }
            },
        )

    private fun GameTestState.runDarkLureTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            DarkLureSpellScript::class,
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
        val spell: DarkLureSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val cacheTypes: TypeListMap,
    )
}
