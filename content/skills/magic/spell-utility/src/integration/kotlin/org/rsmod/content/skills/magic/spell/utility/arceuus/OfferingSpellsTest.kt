package org.rsmod.content.skills.magic.spell.utility.arceuus

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.prayer.remains.PrayerRemainsKind
import org.rsmod.content.skills.prayer.remains.PrayerRemainsRecipes
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class OfferingSpellsTest {
    @Test
    fun GameTestState.`cache 233 pins both offering spell and presentation contracts`() =
        runOfferingTest { deps ->
            val demonic = requireNotNull(deps.registry.getObjSpell(objs.spell_demonic_offering))
            assertEquals(15_346, demonic.obj.id)
            assertEquals(84, demonic.levelReq)
            assertEquals(175.0, demonic.castXp)
            assertEquals(Spellbook.Arceuus, demonic.spellbook)
            assertEquals(MagicSpellType.Utility, demonic.type)
            assertEquals(
                mapOf(objs.soul_rune to 1, objs.wrath_rune to 1),
                demonic.objReqs.associate { it.obj to it.count },
            )

            val sinister = requireNotNull(deps.registry.getObjSpell(objs.spell_sinister_offering))
            assertEquals(8_796, sinister.obj.id)
            assertEquals(92, sinister.levelReq)
            assertEquals(180.0, sinister.castXp)
            assertEquals(Spellbook.Arceuus, sinister.spellbook)
            assertEquals(MagicSpellType.Utility, sinister.type)
            assertEquals(
                mapOf(objs.blood_rune to 1, objs.wrath_rune to 1),
                sinister.objReqs.associate { it.obj to it.count },
            )

            assertComponent(deps, components.magic_spellbook_demonic_offering.id, 177)
            assertComponent(deps, components.magic_spellbook_sinister_offering.id, 178)
            assertEquals(1_871, spotanims.demonic_offering_cast.id)
            assertEquals(1_872, spotanims.sinister_offering_cast.id)
            assertEquals(
                seqs.demonic_offering_effect.id,
                deps.cacheTypes.spotanims[spotanims.demonic_offering_cast].anim,
            )
            assertEquals(
                seqs.sinister_offering_effect.id,
                deps.cacheTypes.spotanims[spotanims.sinister_offering_cast].anim,
            )
            assertEquals(8_975, seqs.human_cast_offering.id)
            assertEquals(8_976, seqs.human_prepare_offering.id)
            assertEquals(8_992, seqs.demonic_offering_effect.id)
            assertEquals(8_991, seqs.sinister_offering_effect.id)
            assertEquals(3, deps.cacheTypes.seqs[seqs.human_cast_offering].tickDuration)
            assertEquals(5, deps.cacheTypes.seqs[seqs.human_prepare_offering].tickDuration)
            assertEquals(12_423, varbits.arceuus_offering_cooldown.id)
            assertEquals(38, timers.magic_arceuus_offering_cooldown.id)
            assertEquals(3, OfferingSpells.MAX_REMAINS)
            assertEquals(3, OfferingSpells.XP_MULTIPLIER)
            assertEquals(8, OfferingSpells.CAST_INTERVAL_CYCLES)
        }

    @Test
    fun GameTestState
        .`demonic offering consumes first three ashes and applies xp restoration and graphics`() =
        runOfferingTest { deps ->
            prepareCaster()
            player.inv[2] = InvObj(objs.fiendish_ashes)
            player.inv[3] = InvObj(objs.bones)
            player.inv[4] = InvObj(objs.infernal_ashes)
            player.inv[5] = InvObj(objs.vile_ashes)
            player.inv[6] = InvObj(objs.abyssal_ashes)

            player.withProtectedAccess { deps.spells.castDemonic(this) }

            assertEquals(null, player.inv[2])
            assertEquals(objs.bones.id, player.inv[3]?.id)
            assertEquals(null, player.inv[4])
            assertEquals(null, player.inv[5])
            assertEquals(objs.abyssal_ashes.id, player.inv[6]?.id)
            assertEquals(4_350, player.statMap.getFineXP(stats.prayer))
            assertEquals(1_750, player.statMap.getFineXP(stats.magic))
            assertEquals(4, player.stats[stats.prayer])
            assertEquals(seqs.human_cast_offering.id, player.pendingSequence.id)
            assertEquals(
                spotanims.demonic_offering_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
        }

    @Test
    fun GameTestState
        .`sinister offering consumes first three bones without recipe prayer level gates`() =
        runOfferingTest { deps ->
            prepareCaster()
            player.setBaseLevel(stats.prayer, 1)
            player.setCurrentLevel(stats.prayer, 0)
            player.inv[2] = InvObj(objs.superior_dragon_bones)
            player.inv[3] = InvObj(objs.ourg_bones)
            player.inv[4] = InvObj(objs.dagannoth_bones)
            player.inv[5] = InvObj(objs.hydra_bones)

            player.withProtectedAccess { deps.spells.castSinister(this) }

            assertEquals(null, player.inv[2])
            assertEquals(null, player.inv[3])
            assertEquals(null, player.inv[4])
            assertEquals(objs.hydra_bones.id, player.inv[5]?.id)
            assertEquals(12_450, player.statMap.getFineXP(stats.prayer))
            assertEquals(1_800, player.statMap.getFineXP(stats.magic))
            // The XP gain raises the base level first; the six restored points then cap to it.
            assertEquals(6, player.stats[stats.prayer])
            assertEquals(
                spotanims.sinister_offering_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
        }

    @Test
    fun GameTestState
        .`every canonical remains recipe is accepted by its matching spell at triple xp`() =
        runOfferingTest { deps ->
            for (recipe in PrayerRemainsRecipes.all) {
                prepareCaster()
                player.inv[2] = InvObj(recipe.obj)

                player.withProtectedAccess {
                    if (recipe.kind == PrayerRemainsKind.Bone) {
                        deps.spells.castSinister(this)
                    } else {
                        deps.spells.castDemonic(this)
                    }
                }

                assertEquals(null, player.inv[2], recipe.obj.toString())
                assertEquals(
                    (recipe.xp * OfferingSpells.XP_MULTIPLIER * 10).toInt(),
                    player.statMap.getFineXP(stats.prayer),
                    recipe.obj.toString(),
                )
            }
        }

    @Test
    fun GameTestState.`offering cooldown is shared exact and does not interrupt movement`() =
        runOfferingTest { deps ->
            prepareCaster(casts = 2)
            player.inv[2] = InvObj(objs.fiendish_ashes)
            player.inv[3] = InvObj(objs.bones)
            player.routeDestination.add(CoordGrid(3_201, 3_200, 0))

            player.withProtectedAccess { deps.spells.castDemonic(this) }
            assertFalse(player.routeDestination.isEmpty())
            assertEquals(1, player.vars[varbits.arceuus_offering_cooldown])
            player.withProtectedAccess { deps.spells.castSinister(this) }
            assertEquals(objs.bones.id, player.inv[3]?.id)

            advance(OfferingSpells.CAST_INTERVAL_CYCLES - 1)
            assertEquals(1, player.vars[varbits.arceuus_offering_cooldown])
            advance()
            assertEquals(0, player.vars[varbits.arceuus_offering_cooldown])
            player.withProtectedAccess { deps.spells.castSinister(this) }
            assertEquals(null, player.inv[3])
        }

    @Test
    fun GameTestState.`members quest level rune and empty inventory rejections are atomic`() =
        runOfferingTest { deps ->
            prepareCaster()
            player.inv[2] = InvObj(objs.fiendish_ashes)
            player.members = false
            reject(deps, demonic = true)
            player.members = true
            player.setVarBit(varbits.a_kingdom_divided, 149)
            reject(deps, demonic = true)
            player.setVarBit(varbits.a_kingdom_divided, 150)
            player.stats[stats.magic] = 83
            reject(deps, demonic = true)
            player.stats[stats.magic] = 99
            player.inv[0] = null
            reject(deps, demonic = true)

            prepareCaster()
            player.inv[2] = InvObj(objs.bones)
            player.stats[stats.magic] = 91
            reject(deps, demonic = false)

            prepareCaster()
            player.inv[2] = null
            reject(deps, demonic = true)
            reject(deps, demonic = false)
        }

    @Test
    fun GameTestState.`fountain of rune supplies runes and suppresses only magic xp`() =
        runOfferingTest { deps ->
            prepareCaster(casts = 0)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            player.inv[0] = InvObj(objs.infernal_ashes)

            player.withProtectedAccess { deps.spells.castDemonic(this) }

            assertEquals(null, player.inv[0])
            assertEquals(3_300, player.statMap.getFineXP(stats.prayer))
            assertEquals(0, player.statMap.getFineXP(stats.magic))
            assertEquals(2, player.stats[stats.prayer])
        }

    private fun assertComponent(deps: Dependencies, id: Int, child: Int) {
        assertEquals(218, id ushr 16)
        assertEquals(child, id and 0xFFFF)
        assertEquals("Cast", requireNotNull(deps.components[id]).targetVerb)
    }

    private fun GameTestScope.reject(deps: Dependencies, demonic: Boolean) {
        val inventory = player.inv.toList()
        val magicXp = player.statMap.getFineXP(stats.magic)
        val prayerXp = player.statMap.getFineXP(stats.prayer)
        player.withProtectedAccess {
            if (demonic) deps.spells.castDemonic(this) else deps.spells.castSinister(this)
        }
        assertEquals(inventory, player.inv.toList())
        assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
        assertEquals(prayerXp, player.statMap.getFineXP(stats.prayer))
        assertEquals(0, player.vars[varbits.arceuus_offering_cooldown])
    }

    private fun GameTestScope.prepareCaster(casts: Int = 1) {
        player.clearAllInvs()
        player.members = true
        player.setBaseLevel(stats.magic, 99)
        player.setCurrentLevel(stats.magic, 99)
        player.setBaseLevel(stats.prayer, 99)
        player.setCurrentLevel(stats.prayer, 0)
        player.statMap.setXP(stats.magic, 0)
        player.statMap.setXP(stats.prayer, 0)
        player.setVarBit(varbits.a_kingdom_divided, 150)
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.arceuus_offering_cooldown, 0)
        player.righthand = null
        player.routeDestination.clear()
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.wrath_rune, casts * 2)
            player.inv[1] = InvObj(objs.soul_rune, casts)
            player.inv[27] = InvObj(objs.blood_rune, casts)
        }
    }

    private fun GameTestState.runOfferingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            OfferingSpellsScript::class,
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
        val spells: OfferingSpells,
        val registry: MagicSpellRegistry,
        val components: ComponentTypeList,
        val cacheTypes: TypeListMap,
    )
}
