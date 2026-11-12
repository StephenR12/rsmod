package org.rsmod.api.specials

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.specials.configs.energy_enums
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.specials.scripts.SpecialAttackScript
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.api.testing.GameTestExtension
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.special.attacks.SpecialAttackModule as ContentSpecialAttackModule
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.game.enums.EnumTypeMapResolver
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(GameTestExtension::class)
class RockKnockerSpecialAttackTest {
    @Test
    fun GameTestState.`cache 233 pins every Lumber Up identity and shared effect`() =
        runSpecialAttackTest { deps ->
            val variants =
                listOf(
                    objs.dragon_axe,
                    objs.dragon_axe_or,
                    objs.dragon_axe_or_reloaded,
                    objs.third_age_axe,
                    objs.infernal_axe,
                    objs.infernal_axe_uncharged,
                    objs.infernal_axe_or,
                    objs.infernal_axe_or_uncharged,
                    objs.infernal_axe_or_reloaded,
                    objs.infernal_axe_or_reloaded_uncharged,
                    objs.crystal_axe,
                    objs.crystal_axe_inactive,
                )
            assertEquals(
                listOf(
                    6739,
                    25378,
                    30352,
                    20011,
                    13241,
                    13242,
                    25066,
                    25371,
                    30347,
                    30348,
                    23673,
                    23675,
                ),
                variants.map(ObjType::id),
            )

            player.withProtectedAccess {
                for (variant in variants) {
                    val special =
                        assertInstanceOf(
                            SpecialAttack.Instant::class.java,
                            deps.registry[InvObj(variant)],
                        )
                    assertEquals(1_000, deps.weapons.getSpecialEnergy(variant))
                    assertEquals(1_000, special.energyInHundreds)

                    player.setBaseLevel(stats.woodcutting, 61)
                    player.setCurrentLevel(stats.woodcutting, 61)
                    player.pendingSay = null
                    resetAnim()

                    assertTrue(special.activate(this))
                    assertEquals(64, player.statMap.getCurrentLevel(stats.woodcutting).toInt())
                    assertEquals("Chop chop!", player.pendingSay)
                    assertEquals(special_seqs.lumber_up.id, player.pendingSequence.id)
                }
            }
        }

    @Test
    fun GameTestState.`cache 233 pins every Rock Knocker identity energy and animation`() =
        runSpecialAttackTest { deps ->
            val expected =
                listOf(
                    Variant(objs.dragon_pickaxe, special_seqs.rock_knocker_dragon_pickaxe),
                    Variant(
                        objs.dragon_pickaxe_or_zalcano,
                        special_seqs.rock_knocker_dragon_pickaxe_or_zalcano,
                    ),
                    Variant(
                        objs.dragon_pickaxe_or_trailblazer,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer,
                    ),
                    Variant(
                        objs.dragon_pickaxe_or_trailblazer_reloaded,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer_reloaded,
                    ),
                    Variant(
                        objs.dragon_pickaxe_upgraded,
                        special_seqs.rock_knocker_dragon_pickaxe_upgraded,
                    ),
                    Variant(objs.infernal_pickaxe, special_seqs.rock_knocker_infernal_pickaxe),
                    Variant(
                        objs.infernal_pickaxe_uncharged,
                        special_seqs.rock_knocker_infernal_pickaxe,
                    ),
                    Variant(
                        objs.infernal_pickaxe_or,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer,
                    ),
                    Variant(
                        objs.infernal_pickaxe_or_uncharged,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer,
                    ),
                    Variant(
                        objs.infernal_pickaxe_or_reloaded,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer_reloaded,
                    ),
                    Variant(
                        objs.infernal_pickaxe_or_reloaded_uncharged,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer_reloaded,
                    ),
                    Variant(objs.third_age_pickaxe, special_seqs.rock_knocker_3rd_age_pickaxe),
                    Variant(objs.crystal_pickaxe, special_seqs.rock_knocker_crystal_pickaxe),
                    Variant(
                        objs.crystal_pickaxe_inactive,
                        special_seqs.rock_knocker_crystal_pickaxe,
                    ),
                )

            assertEquals(
                listOf(
                    11920,
                    23677,
                    25376,
                    30351,
                    12797,
                    13243,
                    13244,
                    25063,
                    25369,
                    30345,
                    30346,
                    20014,
                    23680,
                    23682,
                ),
                expected.map { it.obj.id },
            )
            assertEquals(
                listOf(
                    7138,
                    8330,
                    11821,
                    11822,
                    334,
                    3410,
                    3410,
                    11821,
                    11821,
                    11822,
                    11822,
                    7267,
                    8329,
                    8329,
                ),
                expected.map { it.seq.id },
            )

            player.withProtectedAccess {
                for (variant in expected) {
                    val special =
                        assertInstanceOf(
                            SpecialAttack.Instant::class.java,
                            deps.registry[InvObj(variant.obj)],
                        )
                    assertEquals(1_000, deps.weapons.getSpecialEnergy(variant.obj))
                    assertEquals(1_000, special.energyInHundreds)

                    player.setBaseLevel(stats.mining, 61)
                    player.setCurrentLevel(stats.mining, 61)
                    player.pendingSay = null
                    resetAnim()
                    val activated = special.activate(this)

                    assertTrue(activated)
                    assertEquals(64, player.statMap.getCurrentLevel(stats.mining).toInt())
                    assertEquals("Smashing!", player.pendingSay)
                    assertEquals(variant.seq.id, player.pendingSequence.id)
                }
            }
        }

    @Test
    fun GameTestState.`inactive crystal pickaxe retains its cache mapped Rock Knocker`() =
        runSpecialAttackTest { deps ->
            val requirements =
                deps.enumResolver[energy_enums.energy_requirements].filterValuesNotNull()
            assertEquals(23682, objs.crystal_pickaxe_inactive.id)
            assertEquals(1_000, requirements.getOrNull(objs.crystal_pickaxe_inactive))
            assertEquals(1_000, deps.weapons.getSpecialEnergy(objs.crystal_pickaxe_inactive))
            assertInstanceOf(
                SpecialAttack.Instant::class.java,
                deps.registry[InvObj(objs.crystal_pickaxe_inactive)],
            )
        }

    @Test
    fun GameTestState.`Rock Knocker preserves a larger Mining boost and consumes a full bar`() =
        runSpecialAttackTest { deps ->
            val special =
                assertInstanceOf(
                    SpecialAttack.Instant::class.java,
                    deps.registry[InvObj(objs.dragon_pickaxe)],
                )
            player.setBaseLevel(stats.mining, 61)
            player.setCurrentLevel(stats.mining, 70)
            deps.energy.restoreSpecialEnergy(player, SpecialAttackEnergy.MAX_ENERGY)

            var activated = false
            player.withProtectedAccess { activated = special.activate(this) }
            if (activated) {
                deps.energy.takeSpecialEnergy(player, special.energyInHundreds)
            }

            assertTrue(activated)
            assertEquals(70, player.statMap.getCurrentLevel(stats.mining).toInt())
            assertEquals(false, deps.energy.hasSpecialEnergy(player, 1))
        }

    private fun GameTestState.runSpecialAttackTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            SpecialAttackScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(SpecialAttackModule())
            install(ContentSpecialAttackModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val registry: SpecialAttackRegistry,
        val weapons: SpecialAttackWeapons,
        val energy: SpecialAttackEnergy,
        val enumResolver: EnumTypeMapResolver,
    )

    private data class Variant(val obj: ObjType, val seq: SeqType)
}
