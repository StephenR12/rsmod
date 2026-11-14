package org.rsmod.content.skills.magic.spell.utility.arceuus

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
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
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.FreshDropOrigin
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Execution(ExecutionMode.SAME_THREAD)
class ArceuusReanimationTest {
    @Test
    fun GameTestState.`cache 238 pins every head npc spell and presentation contract`() =
        runReanimationTest { deps ->
            assertEquals(23, ReanimationRecipes.all.size)
            assertEquals(
                listOf(
                    13447,
                    13450,
                    13453,
                    13456,
                    13459,
                    13462,
                    13465,
                    13468,
                    13471,
                    13474,
                    13477,
                    13480,
                    13483,
                    13486,
                    13489,
                    13492,
                    13495,
                    13498,
                    13501,
                    26996,
                    13504,
                    13507,
                    13510,
                ),
                ReanimationRecipes.all.map { it.initialHead.id },
            )
            assertEquals(
                listOf(
                    13448,
                    13451,
                    13454,
                    13457,
                    13460,
                    13463,
                    13466,
                    13469,
                    13472,
                    13475,
                    13478,
                    13481,
                    13484,
                    13487,
                    13490,
                    13493,
                    13496,
                    13499,
                    13502,
                    26997,
                    13505,
                    13508,
                    13511,
                ),
                ReanimationRecipes.all.map { it.processedHead.id },
            )
            assertEquals(
                (7018..7036).toList() + 11463 + (7037..7039).toList(),
                ReanimationRecipes.all.map { it.npc.id },
            )
            assertEquals(
                listOf(
                    130.0,
                    182.0,
                    286.0,
                    364.0,
                    454.0,
                    480.0,
                    494.0,
                    520.0,
                    584.0,
                    650.0,
                    716.0,
                    754.0,
                    780.0,
                    832.0,
                    884.0,
                    936.0,
                    1040.0,
                    1104.0,
                    1170.0,
                    1200.0,
                    1234.0,
                    1300.0,
                    1560.0,
                ),
                ReanimationRecipes.all.map { it.prayerXp },
            )
            for (recipe in ReanimationRecipes.all) {
                assertEquals(
                    recipe.spell,
                    deps.objTypes[recipe.processedHead].param(params.spell_parent_obj),
                )
                assertEquals("Attack", deps.npcTypes[recipe.npc].op[1])
            }

            assertSpell(deps, objs.spell_basic_reanimation, 11_997, 16, 32.0, 154, 4, 2, 0)
            assertSpell(deps, objs.spell_adept_reanimation, 2_885, 41, 80.0, 156, 4, 3, 1)
            assertSpell(deps, objs.spell_expert_reanimation, 6_208, 72, 138.0, 157, 0, 3, 2, 1)
            assertSpell(deps, objs.spell_master_reanimation, 6_888, 90, 170.0, 158, 0, 4, 4, 2)
            assertEquals(2, deps.types.seqs[ReanimationSeqs.player].tickDuration)
            assertEquals(3, deps.types.seqs[ReanimationSeqs.creature].tickDuration)
            assertEquals(
                7,
                requireNotNull(
                        deps.types.seqs[deps.types.spotanims[ReanimationSpotanims.spawning].anim]
                    )
                    .tickDuration,
            )
            assertEquals(21_907, ReanimationObjs.vorkathHead.id)
            assertEquals(CoordGrid(0, 26, 60, 36, 42), ArceuusReanimation.DARK_ALTAR)
        }

    @Test
    fun GameTestState
        .`all twenty three processed heads cast and award exact xp on credited kill`() =
        runReanimationTest { deps ->
            for (recipe in ReanimationRecipes.all) {
                prepareCaster(deps, recipe, nearAltar = true)
                val startMagicXp = player.statMap.getFineXP(stats.magic)
                val spell = requireNotNull(deps.spells.getObjSpell(recipe.spell))

                val npc = requireNotNull(castInventory(deps, recipe.spell))
                assertEquals(recipe.npc.id, npc.id)
                assertEquals(0, player.count(recipe.processedHead))
                assertEquals(
                    (spell.castXp * 10).toInt(),
                    player.statMap.getFineXP(stats.magic) - startMagicXp,
                )
                assertEquals(ReanimationSeqs.player.id, player.pendingSequence.id)
                assertEquals(
                    ReanimationSpotanims.player.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                assertEquals(ReanimationSeqs.creature.id, npc.pendingSequence.id)
                assertEquals(
                    player.currentMapClock + ArceuusReanimation.SPAWN_ANIMATION_CYCLES,
                    npc.actionDelay,
                )
                assertEquals(npc, deps.states.active(player))

                assertTrue(eventBus.publish(NpcKillEvents.Credited(player, npc)))
                assertEquals((recipe.prayerXp * 10).toInt(), player.statMap.getFineXP(stats.prayer))
                assertFalse(deps.states.hasActive(player))
                deps.npcRepo.del(npc, Int.MAX_VALUE)
                advance(ArceuusReanimation.CAST_RATE)
            }
        }

    @Test
    fun GameTestState
        .`fresh ground and carried heads cast only inside their recorded thirty one square area`() =
        runReanimationTest { deps ->
            val recipe = ReanimationRecipes.all.first()
            prepareCaster(deps, recipe, nearAltar = false)
            player.inv[HEAD_SLOT] = null
            val origin = player.coords
            val fresh =
                deps.objRepo.add(
                    InvObj(recipe.initialHead, vars = FreshDropOrigin.pack(origin)),
                    origin.translateX(2),
                    100,
                )

            val npc = requireNotNull(castGround(deps, recipe.spell, fresh))
            assertTrue(deps.objRepo.findAll(fresh.coords).none { it === fresh })
            deps.states.remove(npc)
            deps.npcRepo.del(npc, Int.MAX_VALUE)
            advance(ArceuusReanimation.CAST_RATE)

            prepareCaster(deps, recipe, nearAltar = false)
            player.inv[HEAD_SLOT] =
                InvObj(recipe.initialHead, vars = FreshDropOrigin.pack(player.coords))
            val carried = requireNotNull(castInventory(deps, recipe.spell))
            deps.states.remove(carried)
            deps.npcRepo.del(carried, Int.MAX_VALUE)
            advance(ArceuusReanimation.CAST_RATE)

            prepareCaster(deps, recipe, nearAltar = false)
            player.inv[HEAD_SLOT] =
                InvObj(
                    recipe.initialHead,
                    vars = FreshDropOrigin.pack(player.coords.translateX(-16)),
                )
            val before = snapshot(recipe)
            assertNull(castInventory(deps, recipe.spell))
            assertEquals(before, snapshot(recipe))
            assertMessageSent(ArceuusReanimation.TOO_FAR_MESSAGE)

            player.inv[HEAD_SLOT] = null
            client.clear()
            val unprovenanced =
                deps.objRepo.add(recipe.initialHead, player.coords.translateX(2), 100)
            assertNull(castGround(deps, recipe.spell, unprovenanced))
            assertTrue(deps.objRepo.findAll(unprovenanced.coords).any { it === unprovenanced })
            assertMessageSent(ArceuusReanimation.TOO_FAR_MESSAGE)

            client.clear()
            val processed = deps.objRepo.add(recipe.processedHead, player.coords.translateX(2), 100)
            val ground = castGround(deps, recipe.spell, processed)
            assertNull(ground)
            assertTrue(deps.objRepo.findAll(processed.coords).any { it === processed })
            assertMessageSent(ArceuusReanimation.TOO_FAR_MESSAGE)
        }

    @Test
    fun GameTestState.`Vorkath head substitutes for dragon and Zealot robes preserve the head`() =
        runReanimationTest { deps ->
            val dragon = ReanimationRecipes.all.last()
            prepareCaster(deps, dragon, nearAltar = true)
            player.inv[HEAD_SLOT] = InvObj(ReanimationObjs.vorkathHead)
            player.hat = InvObj(objs.zealots_helm)
            player.torso = InvObj(objs.zealots_robe_top)
            player.legs = InvObj(objs.zealots_robe_bottom)
            player.feet = InvObj(objs.zealots_boots)
            player.teleport(ArceuusReanimation.DARK_ALTAR.translate(12, 0))
            assertEquals(objs.zealots_helm.id, player.hat?.id)
            assertEquals(objs.zealots_robe_top.id, player.torso?.id)
            assertEquals(objs.zealots_robe_bottom.id, player.legs?.id)
            assertEquals(objs.zealots_boots.id, player.feet?.id)

            val npc =
                requireNotNull(castInventory(deps, dragon.spell)) {
                    "Vorkath cast rejected: coords=${player.coords}, inventory=${player.inv.toList()}, " +
                        "active=${deps.states.hasActive(player)}, " +
                        "messages=${client.filterIsInstance<MessageGame>().map { it.message }}"
                }
            assertEquals(dragon.npc.id, npc.id)
            assertEquals(1, player.count(ReanimationObjs.vorkathHead))
            assertMessageSent(
                "Your Zealot's robes save the ensouled head but still grant you experience."
            )
        }

    @Test
    fun GameTestState.`active member spellbook level rune tier and room checks are atomic`() =
        runReanimationTest { deps ->
            val recipe = ReanimationRecipes.all.first()
            prepareCaster(deps, recipe, nearAltar = true, casts = 2)
            val active = requireNotNull(castInventory(deps, recipe.spell))
            player.inv[HEAD_SLOT] = InvObj(recipe.processedHead)
            val activeSnapshot = snapshot(recipe)
            assertNull(castInventory(deps, recipe.spell))
            assertEquals(activeSnapshot, snapshot(recipe))
            assertMessageSent("You should finish off your last one first.")
            deps.states.remove(active)
            deps.npcRepo.del(active, Int.MAX_VALUE)
            advance(ArceuusReanimation.CAST_RATE)

            prepareCaster(deps, recipe, nearAltar = true)
            player.members = false
            rejectInventory(deps, recipe)
            player.members = true
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            rejectInventory(deps, recipe)
            player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
            player.setCurrentLevel(stats.magic, 15)
            rejectInventory(deps, recipe)
            player.setCurrentLevel(stats.magic, 99)
            player.inv[0] = null
            rejectInventory(deps, recipe)

            prepareCaster(deps, recipe, nearAltar = true)
            rejectInventory(deps, recipe, spell = objs.spell_adept_reanimation)

            prepareCaster(deps, recipe, nearAltar = true)
            for (x in -8..8) {
                for (z in -8..8) {
                    deps.collision[
                            player.coords.x + x,
                            player.coords.z + z,
                            player.coords.level,
                        ] = CollisionFlag.LOC
                }
            }
            rejectInventory(deps, recipe)
            assertMessageSent("The creature wouldn't have room to re-animate there.")
        }

    @Test
    fun GameTestState
        .`Fountain supplies runes and suppresses magic xp without suppressing reanimation`() =
        runReanimationTest { deps ->
            val recipe = ReanimationRecipes.all.last()
            prepareCaster(deps, recipe, nearAltar = true, casts = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)

            val npc = requireNotNull(castInventory(deps, recipe.spell))
            assertEquals(0, player.statMap.getFineXP(stats.magic))
            assertEquals(0, player.count(recipe.processedHead))
        }

    private fun assertSpell(
        deps: Dependencies,
        spellObj: org.rsmod.game.type.obj.ObjType,
        id: Int,
        level: Int,
        xp: Double,
        child: Int,
        body: Int,
        nature: Int,
        soul: Int,
        blood: Int = 0,
    ) {
        val spell = requireNotNull(deps.spells.getObjSpell(spellObj))
        assertEquals(id, spell.obj.id)
        assertEquals(level, spell.levelReq)
        assertEquals(xp, spell.castXp)
        assertEquals(Spellbook.Arceuus, spell.spellbook)
        assertEquals(MagicSpellType.Utility, spell.type)
        assertEquals(218, spell.component.id ushr 16)
        assertEquals(child, spell.component.id and 0xFFFF)
        assertEquals("Reanimate", deps.components[spell.component].targetVerb)
        assertEquals(
            buildMap {
                if (body > 0) put(objs.body_rune, body)
                if (nature > 0) put(objs.nature_rune, nature)
                if (soul > 0) put(objs.soul_rune, soul)
                if (blood > 0) put(objs.blood_rune, blood)
            },
            spell.objReqs.associate { it.obj to it.count },
        )
    }

    private fun GameTestScope.prepareCaster(
        deps: Dependencies,
        recipe: ReanimationRecipe,
        nearAltar: Boolean,
        casts: Int = 1,
    ) {
        player.clearAllInvs()
        player.members = true
        player.setBaseLevel(stats.magic, 99)
        player.setCurrentLevel(stats.magic, 99)
        player.setBaseLevel(stats.prayer, 99)
        player.setCurrentLevel(stats.prayer, 99)
        player.statMap.setXP(stats.magic, 0)
        player.statMap.setXP(stats.prayer, 0)
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.righthand = null
        deps.states.active(player)?.let { deps.npcRepo.del(it, Int.MAX_VALUE) }
        deps.states.clear(player)
        player.teleport(if (nearAltar) ArceuusReanimation.DARK_ALTAR else FAR_AWAY)
        client.clear()
        val spell = requireNotNull(deps.spells.getObjSpell(recipe.spell))
        spell.objReqs.forEachIndexed { index, requirement ->
            if (casts > 0) {
                player.inv[index] = InvObj(requirement.obj, requirement.count * casts)
            }
        }
        player.inv[HEAD_SLOT] = InvObj(recipe.processedHead)
    }

    private fun GameTestScope.rejectInventory(
        deps: Dependencies,
        recipe: ReanimationRecipe,
        spell: org.rsmod.game.type.obj.ObjType = recipe.spell,
    ) {
        val before = snapshot(recipe)
        assertNull(castInventory(deps, spell))
        assertEquals(before, snapshot(recipe))
        assertFalse(deps.states.hasActive(player))
    }

    private fun GameTestScope.snapshot(recipe: ReanimationRecipe): Triple<List<InvObj?>, Int, Int> =
        Triple(
            player.inv.toList(),
            player.statMap.getFineXP(stats.magic),
            player.count(recipe.processedHead),
        )

    private fun GameTestScope.castInventory(
        deps: Dependencies,
        spell: org.rsmod.game.type.obj.ObjType,
    ): Npc? {
        var result: Npc? = null
        player.withProtectedAccess {
            result = deps.reanimation.castInventory(this, spell, HEAD_SLOT)
        }
        return result
    }

    private fun GameTestScope.castGround(
        deps: Dependencies,
        spell: org.rsmod.game.type.obj.ObjType,
        target: org.rsmod.game.obj.Obj,
    ): Npc? {
        var result: Npc? = null
        player.withProtectedAccess { result = deps.reanimation.castGround(this, spell, target) }
        return result
    }

    private fun GameTestState.runReanimationTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            ArceuusReanimationScript::class,
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
            bind(ReanimationStateRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val reanimation: ArceuusReanimation,
        val states: ReanimationStateRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val objTypes: ObjTypeList,
        val npcTypes: NpcTypeList,
        val types: TypeListMap,
        val objRepo: ObjRepository,
        val npcRepo: NpcRepository,
        val collision: CollisionFlagMap,
    )

    private companion object {
        const val HEAD_SLOT = 10
        val FAR_AWAY = CoordGrid(0, 50, 50, 32, 32)
    }
}
