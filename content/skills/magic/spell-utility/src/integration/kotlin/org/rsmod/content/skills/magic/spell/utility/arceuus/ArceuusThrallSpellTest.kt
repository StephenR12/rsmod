package org.rsmod.content.skills.magic.spell.utility.arceuus

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varns
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.repo.npc.NpcRepository
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
import org.rsmod.game.interact.InteractionNpcOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.util.ParamMapBuilder

@Execution(ExecutionMode.SAME_THREAD)
class ArceuusThrallSpellTest {
    @Test
    fun GameTestState.`cache 233 pins all nine thrall spell npc and presentation contracts`() =
        runThrallTest { deps ->
            assertEquals(9, ThrallRecipes.all.size)
            assertEquals(
                listOf(25_511, 25_509, 25_508, 25_506, 25_512, 25_513, 25_507, 25_510, 25_514),
                ThrallRecipes.all.map { it.spell.id },
            )
            assertEquals(
                listOf(10_878, 10_881, 10_884, 10_879, 10_882, 10_885, 10_880, 10_883, 10_886),
                ThrallRecipes.all.map { it.npc.id },
            )
            assertEquals(listOf(1, 1, 1, 2, 2, 2, 3, 3, 3), ThrallRecipes.all.map { it.maxHit })
            assertEquals(listOf(2, 2, 2, 4, 4, 4, 6, 6, 6), ThrallRecipes.all.map { it.prayerCost })
            assertEquals((0..8).toList(), ThrallRecipes.all.map { it.usedValue })

            for ((index, recipe) in ThrallRecipes.all.withIndex()) {
                val spell = requireNotNull(deps.spells.getObjSpell(recipe.spell))
                val tier = index / 3
                assertEquals(listOf(38, 57, 76)[tier], spell.levelReq)
                assertEquals(listOf(55.0, 70.0, 88.0)[tier], spell.castXp)
                assertEquals(Spellbook.Arceuus, spell.spellbook)
                assertEquals(MagicSpellType.Combat, spell.type)
                assertEquals(218, recipe.component.id ushr 16)
                assertEquals(184 + index, recipe.component.id and 0xFFFF)
                assertEquals("Cast", requireNotNull(deps.components[recipe.component]).targetVerb)
                assertEquals(4, deps.npcTypes[recipe.npc].param(params.attackrate))
                assertEquals(0, deps.npcTypes[recipe.npc].op.count { it != null })
            }

            assertEquals(7_198, ThrallSeqs.playerCast.id)
            assertEquals(1_288, ThrallSpotanims.playerCast.id)
            assertEquals(1_906, ThrallSpotanims.rangedTravel.id)
            assertEquals(1_907, ThrallSpotanims.magicTravel.id)
            assertEquals(1_908, ThrallSpotanims.magicImpact.id)
            assertEquals(12_290, varbits.arceuus_resurrection_cooldown.id)
            assertEquals(12_412, varbits.arceuus_resurrection_used.id)
            assertEquals(12_413, varbits.arceuus_resurrection_active.id)
            assertEquals(12_867, varbits.ca_tier_status_master.id)
            assertEquals(39, timers.magic_arceuus_thrall_attack.id)
            assertEquals(40, timers.magic_arceuus_thrall_cooldown.id)
            val offensive = deps.spells.offensiveCombatSpells().mapTo(hashSetOf()) { it.obj }
            assertTrue(ThrallRecipes.all.none { it.spell in offensive })
        }

    @Test
    fun GameTestState.`all tiers consume cache runes prayer and grant exact cast xp`() =
        runThrallTest { deps ->
            for (recipe in ThrallRecipes.all) {
                prepareCaster(deps, recipe)
                val spell = requireNotNull(deps.spells.getObjSpell(recipe.spell))
                val beforePrayer = player.stats[stats.prayer]
                val beforeRunes = spell.objReqs.associate { it.obj to player.count(it.obj) }

                val npc = requireNotNull(cast(deps, recipe))

                assertEquals(recipe.npc.id, npc.id)
                assertEquals(beforePrayer - recipe.prayerCost, player.stats[stats.prayer])
                for (requirement in spell.objReqs) {
                    assertEquals(
                        beforeRunes.getValue(requirement.obj) - requirement.count,
                        player.count(requirement.obj),
                    )
                }
                assertEquals((spell.castXp * 10).toInt(), player.statMap.getFineXP(stats.magic))
                assertEquals(ThrallSeqs.playerCast.id, player.pendingSequence.id)
                assertEquals(
                    ThrallSpotanims.playerCast.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                assertEquals(recipe.style.spawnSeq.id, npc.pendingSequence.id)
                assertEquals(npc, deps.states.active(player)?.npc)
                assertEquals(1, player.vars[varbits.arceuus_resurrection_active])
                assertEquals(recipe.usedValue, player.vars[varbits.arceuus_resurrection_used])
                // The repository clock is one cycle ahead of the player test clock at insertion.
                assertEquals(100, npc.lifecycleDelCycle - player.currentMapClock)
                cleanup(deps)
            }
        }

    @Test
    fun GameTestState
        .`master combat achievement doubles duration and natural expiry clears active state`() =
        runThrallTest { deps ->
            val recipe = ThrallRecipes.all.first()
            prepareCaster(deps, recipe)
            player.setVarBit(varbits.ca_tier_status_master, 2)

            val npc = requireNotNull(cast(deps, recipe))
            assertEquals(199, npc.lifecycleDelCycle - player.currentMapClock)

            advance(198)
            assertNull(deps.states.active(player))
            assertEquals(0, player.vars[varbits.arceuus_resurrection_active])
        }

    @Test
    fun GameTestState
        .`recast cooldown is exact and replacing a thrall preserves one active owner`() =
        runThrallTest { deps ->
            val firstRecipe = ThrallRecipes.all.first()
            val secondRecipe = ThrallRecipes.all.last()
            prepareCaster(deps, firstRecipe, casts = 2)
            giveRunes(deps, secondRecipe, casts = 1, startSlot = 10)

            val first = requireNotNull(cast(deps, firstRecipe))
            assertEquals(1, player.vars[varbits.arceuus_resurrection_cooldown])
            assertNull(cast(deps, secondRecipe))
            assertEquals(first, deps.states.active(player)?.npc)

            advance(ArceuusThrallSpell.RECAST_COOLDOWN_CYCLES)
            assertEquals(0, player.vars[varbits.arceuus_resurrection_cooldown])
            val second = requireNotNull(cast(deps, secondRecipe))
            assertNotEquals(first, second)
            assertFalse(first.isSlotAssigned)
            assertEquals(second, deps.states.active(player)?.npc)
            assertEquals(1, player.vars[varbits.arceuus_resurrection_active])
        }

    @Test
    fun GameTestState.`member quest book prayer level rune and room rejections are atomic`() =
        runThrallTest { deps ->
            val recipe = ThrallRecipes.all.first()
            prepareCaster(deps, recipe)
            player.members = false
            reject(deps, recipe)
            player.members = true
            player.setVarBit(varbits.a_kingdom_divided, 149)
            reject(deps, recipe)
            player.setVarBit(varbits.a_kingdom_divided, 150)
            player.inv[BOOK_SLOT] = null
            reject(deps, recipe)
            player.inv[BOOK_SLOT] = InvObj(objs.book_of_the_dead)
            player.setCurrentLevel(stats.prayer, recipe.prayerCost - 1)
            reject(deps, recipe)
            player.setCurrentLevel(stats.prayer, 99)
            player.setCurrentLevel(stats.magic, 37)
            reject(deps, recipe)
            player.setCurrentLevel(stats.magic, 99)
            player.inv[0] = null
            reject(deps, recipe)

            prepareCaster(deps, recipe)
            for (x in -3..3) {
                for (z in -3..3) {
                    deps.collision[player.coords.x + x, player.coords.z + z, player.coords.level] =
                        org.rsmod.routefinder.flag.CollisionFlag.LOC
                }
            }
            reject(deps, recipe)
        }

    @Test
    fun GameTestState.`thrall attacks are always accurate can roll zero and bypass flat armour`() =
        runThrallTest { deps ->
            val recipe = ThrallRecipes.all.last()
            prepareCaster(deps, recipe)
            requireNotNull(cast(deps, recipe))
            val target =
                spawnNpc(
                    player.coords.translateX(1),
                    deps.npcTypes[org.rsmod.api.config.refs.npcs.man],
                )
            target.hitpoints = 7
            target.vars[varns.flat_armour] = 2
            player.interaction = InteractionNpcOp(InteractionOp.Op2, target, true, true)
            random.next = 3

            advance(ArceuusThrallSpell.FIRST_ACTION_DELAY)
            advance(ArceuusThrallScript.MELEE_IMPACT_DELAY)
            assertEquals(4, target.hitpoints)
            assertEquals(2, target.vars[varns.flat_armour])
            assertEquals(0, player.statMap.getFineXP(stats.attack))

            random.next = 0
            advance(ArceuusThrallScript.ATTACK_RATE_CYCLES - ArceuusThrallScript.MELEE_IMPACT_DELAY)
            advance(ArceuusThrallScript.MELEE_IMPACT_DELAY)
            assertEquals(4, target.hitpoints)
        }

    @Test
    fun GameTestState.`cache immunity flag suppresses thrall damage`() = runThrallTest { deps ->
        val recipe = ThrallRecipes.all.last()
        prepareCaster(deps, recipe)
        requireNotNull(cast(deps, recipe))
        val target = spawnNpc(player.coords.translateX(1), immuneTarget)
        val before = target.hitpoints
        player.interaction = InteractionNpcOp(InteractionOp.Op2, target, true, true)
        random.next = 3

        advance(ArceuusThrallSpell.FIRST_ACTION_DELAY + ArceuusThrallScript.MELEE_IMPACT_DELAY)

        assertEquals(before, target.hitpoints)
    }

    @Test
    fun GameTestState.`thralls are invulnerable to incoming hits`() = runThrallTest { deps ->
        val recipe = ThrallRecipes.all.first()
        prepareCaster(deps, recipe)
        val thrall = requireNotNull(cast(deps, recipe))
        val before = thrall.hitpoints

        thrall.queueHit(player, delay = 1, type = org.rsmod.game.hit.HitType.Melee, damage = 10)
        advance(1)

        assertEquals(before, thrall.hitpoints)
    }

    @Test
    fun GameTestState.`fountain supplies runes and suppresses only magic xp`() =
        runThrallTest { deps ->
            val recipe = ThrallRecipes.all.first()
            prepareCaster(deps, recipe, casts = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)

            assertTrue(cast(deps, recipe) != null)
            assertEquals(0, player.statMap.getFineXP(stats.magic))
            assertEquals(97, player.stats[stats.prayer])
        }

    private fun GameTestScope.prepareCaster(
        deps: Dependencies,
        recipe: ThrallRecipe,
        casts: Int = 1,
    ) {
        cleanup(deps)
        player.clearAllInvs()
        player.members = true
        player.setBaseLevel(stats.magic, 99)
        player.setCurrentLevel(stats.magic, 99)
        player.setBaseLevel(stats.prayer, 99)
        player.setCurrentLevel(stats.prayer, 99)
        player.statMap.setXP(stats.magic, 0)
        player.statMap.setXP(stats.attack, 0)
        player.setVarBit(varbits.a_kingdom_divided, 150)
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.ca_tier_status_master, 0)
        player.setVarBit(varbits.arceuus_resurrection_cooldown, 0)
        player.setVarBit(varbits.arceuus_resurrection_active, 0)
        player.clearSoftTimer(timers.magic_arceuus_thrall_cooldown)
        player.interaction = null
        player.teleport(START)
        player.inv[BOOK_SLOT] = InvObj(objs.book_of_the_dead)
        giveRunes(deps, recipe, casts, startSlot = 0)
        client.clear()
    }

    private fun GameTestScope.giveRunes(
        deps: Dependencies,
        recipe: ThrallRecipe,
        casts: Int,
        startSlot: Int,
    ) {
        if (casts == 0) {
            return
        }
        val spell = requireNotNull(deps.spells.getObjSpell(recipe.spell))
        spell.objReqs.forEachIndexed { index, requirement ->
            player.inv[startSlot + index] = InvObj(requirement.obj, requirement.count * casts)
        }
    }

    private fun GameTestScope.cast(deps: Dependencies, recipe: ThrallRecipe): Npc? {
        var result: Npc? = null
        player.withProtectedAccess { result = deps.thralls.cast(this, recipe) }
        return result
    }

    private fun GameTestScope.reject(deps: Dependencies, recipe: ThrallRecipe) {
        val inventory = player.inv.toList()
        val prayer = player.stats[stats.prayer]
        val magicXp = player.statMap.getFineXP(stats.magic)
        assertNull(cast(deps, recipe))
        assertEquals(inventory, player.inv.toList())
        assertEquals(prayer, player.stats[stats.prayer])
        assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
        assertNull(deps.states.active(player))
    }

    private fun GameTestScope.cleanup(deps: Dependencies) {
        val active = deps.states.remove(player) ?: return
        if (active.npc.isSlotAssigned) {
            deps.npcRepo.del(active.npc, Int.MAX_VALUE)
        }
    }

    private fun GameTestState.runThrallTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            NpcHitScript::class,
            ArceuusThrallScript::class,
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
            bind(ThrallStateRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val thralls: ArceuusThrallSpell,
        val states: ThrallStateRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val npcTypes: NpcTypeList,
        val types: TypeListMap,
        val npcRepo: NpcRepository,
        val collision: org.rsmod.routefinder.collision.CollisionFlagMap,
    )

    private companion object {
        const val BOOK_SLOT = 27
        val START = org.rsmod.map.CoordGrid(0, 50, 50, 32, 32)
        val immuneTarget =
            npcTypeFactory.create {
                name = "Thrall-immune target"
                hitpoints = 100
                paramMap = ParamMapBuilder().apply { this[params.thrall_immunity] = 1 }.toParamMap()
            }
    }
}
