package org.rsmod.content.bosses.magearena

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class MageArenaTest {
    @Test
    fun GameTestState.`mage arena one progresses through all five Kolodion forms`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val kolodion = spawnNpc(player.coords, deps.types[MageArenaNpcs.kolodionHuman])
            val state = deps.controller.resetKolodion(kolodion)

            MageArenaBosses.kolodionForms.forEachIndexed { phase, form ->
                state.kolodionPhase = phase
                deps.controller.configureKolodion(kolodion, phase)
                assertEquals(form.npc.id, kolodion.visType.id)
                assertEquals(form.hitpoints, kolodion.hitpoints)
            }
        }

    @Test
    fun GameTestState
        .`Kolodion checks Magic then advances only the challenger through five magic kills`() =
        runInjectedGameTest(
            Dependencies::class,
            WeaponTestModule,
            MageArenaAccess::class,
            MageArenaScript::class,
            NpcHitScript::class,
            org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript::class,
        ) { deps ->
            player.members = true
            val lobby = CoordGrid(2541, 4715)
            player.placeAt(lobby.translate(-1, 0))
            val guide = deps.npcs.findAll(lobby).single { it.id == 1603 }
            player.setBaseLevel(stats.magic, 59)
            player.opNpc1(guide)
            advance(2)
            assertEquals(0, player.vars[varps.magearena])
            player.setBaseLevel(stats.magic, 60)
            player.opNpc1(guide)
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            val opponent =
                deps.npcs.findAll(MageArenaBosses.mageArena).single {
                    it.visType.id == MageArenaNpcs.kolodionHuman.id
                }
            val outsider = registerPlayer(player.coords.translate(0, 1))
            opponent.queueHit(outsider, 1, HitType.Magic, 12, deps.modifier)
            opponent.queueHit(player, 1, HitType.Melee, 12, deps.modifier)
            advance(2)
            assertEquals(3, opponent.hitpoints)
            for ((idx, form) in MageArenaBosses.kolodionForms.withIndex()) {
                assertEquals(form.npc.id, opponent.visType.id)
                assertEquals(idx + 1, player.vars[varps.magearena])
                opponent.queueHit(player, 1, HitType.Magic, form.hitpoints, deps.modifier)
                advance(2)
            }
            assertEquals(6, player.vars[varps.magearena])
            assertEquals(CoordGrid(2539, 4715), player.coords)
            assertEquals(0, outsider.vars[varps.magearena])
        }

    @Test
    fun GameTestState.`the chamber guardian grants one matching staff after the statue cape`() =
        runInjectedGameTest(Dependencies::class, null, MageArenaAccess::class) { d ->
            player.members = true
            val room = CoordGrid(2508, 4696)
            player.placeAt(room.translate(1, 0))
            val guardian = d.npcs.findAll(room).single { it.visType.name == "Chamber guardian" }
            player.setVarp(varps.magearena, 6)
            player.opNpc1(guardian)
            advance(2)
            assertEquals(0, player.count(org.rsmod.api.config.refs.objs.guthix_staff))
            player.setVarp(varps.magearena, 7)
            player.inv[0] = InvObj(org.rsmod.api.config.refs.objs.guthix_cape)
            player.opNpc1(guardian)
            advance(2)
            assertEquals(1, player.count(org.rsmod.api.config.refs.objs.guthix_staff))
            assertEquals(8, player.vars[varps.magearena])
            player.opNpc1(guardian)
            advance(2)
            assertEquals(1, player.count(org.rsmod.api.config.refs.objs.guthix_staff))
        }

    @Test
    fun GameTestState.`enchanted symbol summons the next mage arena two follower`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val point =
                MageArenaController.followerLocation(
                    player.accountHash,
                    (System.currentTimeMillis() / 2_700_000).toInt(),
                    0,
                )
            player.placeAt(point)
            player.members = true
            player.withProtectedAccess { vars[varbits.inside_wilderness] = 1 }
            player.withProtectedAccess { deps.controller.activateSymbol(this) }
            assertTrue(deps.npcs.findAll(point).none { it.id in MageArenaBosses.followerIds })
            player.setVarBit(cacheTypes.varbits.getValue(6067), 2)
            player.withProtectedAccess { deps.controller.activateSymbol(this) }
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 0))
            val followers =
                deps.npcs
                    .findAll(ZoneKey.from(player.coords), 1)
                    .filter { it.id in MageArenaBosses.followerIds }
                    .toList()
            assertEquals(1, followers.size)
            assertEquals(MageArenaNpcs.zachariah.id, followers.single().id)
            assertEquals(MageArenaController.FOLLOWER_HITPOINTS, followers.single().hitpoints)
        }

    @Test
    fun GameTestState.`Derwen energy heals five hitpoints while alive`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val derwen = spawnNpc(player.coords, deps.types[MageArenaNpcs.derwen])
            derwen.hitpoints = 200
            derwen.baseHitpointsLvl = MageArenaController.FOLLOWER_HITPOINTS

            deps.controller.summonDerwenEnergy(derwen)
            val energy =
                deps.npcs.findAll(ZoneKey.from(player.coords), 1).single {
                    it.id == MageArenaNpcs.derwenEnergy.id
                }
            deps.controller.healDerwen(energy)

            assertEquals(MageArenaController.DERWEN_ENERGY_HITPOINTS, energy.hitpoints)
            assertEquals(205, derwen.hitpoints)
        }

    @Test
    fun GameTestState.`mage arena two followers always drop their quest artefacts`() =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(MageArenaDrops::class)) { deps ->
            val expected =
                mapOf(
                    MageArenaNpcs.zachariah to MageArenaObjs.justiciarsHand,
                    MageArenaNpcs.derwen to MageArenaObjs.entsRoots,
                    MageArenaNpcs.porazdir to MageArenaObjs.demonsHeart,
                )
            expected.forEach { (type, obj) ->
                val npc = spawnNpc(player.coords, deps.types[type])
                val drops =
                    checkNotNull(deps.drops[type])
                        .roll(NpcDropContext(npc, player, player.coords, deps.random))
                assertTrue(drops.any { it.obj.id == obj.id })
            }
        }

    private object WeaponTestModule : com.google.inject.AbstractModule() {
        override fun configure() {
            bind(org.rsmod.api.combat.weapon.styles.AttackStyles::class.java)
                .`in`(com.google.inject.Scopes.SINGLETON)
        }
    }

    private data class Dependencies
    @Inject
    constructor(
        val modifier: StandardNpcHitModifier,
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val random: org.rsmod.api.random.GameRandom,
        val controller: MageArenaController,
    )
}
