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
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
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
import org.rsmod.content.other.coalbag.CoalBagModule
import org.rsmod.content.other.coalbag.CoalBagStorage
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class AlchemySpellsTest {
    @Test
    fun GameTestState.`cache contracts define both alchemy spells and presentation`() =
        runAlchemyTest { deps ->
            val low = requireNotNull(deps.spells.getObjSpell(objs.spell_low_alchemy))
            assertEquals(21, low.levelReq)
            assertEquals(31.0, low.castXp)
            assertEquals(Spellbook.Standard, low.spellbook)
            assertEquals(MagicSpellType.Utility, low.type)
            assertEquals(
                mapOf(objs.fire_rune to 3, objs.nature_rune to 1),
                low.objReqs.associate { it.obj to it.count },
            )

            val high = requireNotNull(deps.spells.getObjSpell(objs.spell_high_alchemy))
            assertEquals(55, high.levelReq)
            assertEquals(65.0, high.castXp)
            assertEquals(Spellbook.Standard, high.spellbook)
            assertEquals(MagicSpellType.Utility, high.type)
            assertEquals(
                mapOf(objs.fire_rune to 5, objs.nature_rune to 1),
                high.objReqs.associate { it.obj to it.count },
            )

            assertEquals(712, seqs.human_castlowlvlalchemy.id)
            assertEquals(713, seqs.human_casthighlvlalchemy.id)
            assertEquals(112, spotanims.lowlvlalchemy_casting.id)
            assertEquals(113, spotanims.highlvlalchemy_casting.id)
            assertEquals(98, synths.low_alchemy.id)
            assertEquals(97, synths.high_alchemy.id)
        }

    @Test
    fun GameTestState.`cache contracts define both bone conversion spells and presentation`() =
        runAlchemyTest { deps ->
            val bananas = requireNotNull(deps.spells.getObjSpell(objs.spell_bones_to_bananas))
            assertEquals(15, bananas.levelReq)
            assertEquals(25.0, bananas.castXp)
            assertEquals(
                mapOf(objs.water_rune to 2, objs.earth_rune to 2, objs.nature_rune to 1),
                bananas.objReqs.associate { it.obj to it.count },
            )

            val peaches = requireNotNull(deps.spells.getObjSpell(objs.spell_bones_to_peaches))
            assertEquals(60, peaches.levelReq)
            assertEquals(35.5, peaches.castXp)
            assertEquals(
                mapOf(objs.water_rune to 4, objs.earth_rune to 4, objs.nature_rune to 2),
                peaches.objReqs.associate { it.obj to it.count },
            )

            assertEquals(722, seqs.human_castbonestobananas.id)
            assertEquals(141, spotanims.bonestobananas_casting.id)
            assertEquals(114, synths.bones_to_bananas_all.id)
        }

    @Test
    fun GameTestState
        .`bones to bananas converts every eligible held bone and leaves higher bones`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.water_rune, 2)
            player.inv[1] = InvObj(objs.earth_rune, 2)
            player.inv[2] = InvObj(objs.nature_rune)
            player.inv[3] = InvObj(objs.bones)
            player.inv[4] = InvObj(objs.burnt_bones)
            player.inv[5] = InvObj(objs.monkey_bones)
            player.inv[6] = InvObj(objs.alans_bones)
            player.inv[7] = InvObj(objs.dragon_bones)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess {
                deps.boneConversion.cast(this, objs.spell_bones_to_bananas)
            }

            assertEquals(4, player.count(objs.banana))
            assertEquals(1, player.count(objs.dragon_bones))
            assertEquals(250, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.human_castbonestobananas.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`bones to peaches requires the Mage Training Arena unlock atomically`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.water_rune, 4)
            player.inv[1] = InvObj(objs.earth_rune, 4)
            player.inv[2] = InvObj(objs.nature_rune, 2)
            player.inv[3] = InvObj(objs.bones)
            player.setVarBit(varbits.magictraining_bonespeaches, 0)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess {
                deps.boneConversion.cast(this, objs.spell_bones_to_peaches)
                assertMessageSent("You have not unlocked this spell.")
            }

            assertEquals(1, player.count(objs.bones))
            assertEquals(4, player.count(objs.water_rune))
            assertEquals(2, player.count(objs.nature_rune))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`bones to peaches applies Creature Graveyard bone yields`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.water_rune, 4)
            player.inv[1] = InvObj(objs.earth_rune, 4)
            player.inv[2] = InvObj(objs.nature_rune, 2)
            player.inv[3] = InvObj(objs.animals_bones_1)
            player.inv[4] = InvObj(objs.animals_bones_4)
            player.setVarBit(varbits.magictraining_bonespeaches, 1)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess {
                deps.boneConversion.cast(this, objs.spell_bones_to_peaches)
            }

            assertEquals(5, player.count(objs.peach))
            assertEquals(355, player.statMap.getFineXP(stats.magic) - startXp)
        }

    @Test
    fun GameTestState.`bone conversion without eligible bones rejects before rune use`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.water_rune, 2)
            player.inv[1] = InvObj(objs.earth_rune, 2)
            player.inv[2] = InvObj(objs.nature_rune)
            player.inv[3] = InvObj(objs.dragon_bones)

            player.withProtectedAccess {
                deps.boneConversion.cast(this, objs.spell_bones_to_bananas)
                assertMessageSent("You aren't holding any bones!")
            }

            assertEquals(2, player.count(objs.water_rune))
            assertEquals(1, player.count(objs.nature_rune))
            assertEquals(1, player.count(objs.dragon_bones))
        }

    @Test
    fun GameTestState.`bone conversion permits one cast each tick`() = runAlchemyTest { deps ->
        player.stats[stats.magic] = 99
        player.clearInv()
        player.inv[0] = InvObj(objs.water_rune, 4)
        player.inv[1] = InvObj(objs.earth_rune, 4)
        player.inv[2] = InvObj(objs.nature_rune, 2)
        player.inv[3] = InvObj(objs.bones)

        player.withProtectedAccess { deps.boneConversion.cast(this, objs.spell_bones_to_bananas) }
        player.inv[4] = InvObj(objs.bones)
        player.withProtectedAccess { deps.boneConversion.cast(this, objs.spell_bones_to_bananas) }
        assertEquals(1, player.count(objs.bones))

        advance()
        player.withProtectedAccess { deps.boneConversion.cast(this, objs.spell_bones_to_bananas) }
        assertEquals(0, player.count(objs.bones))
        assertEquals(2, player.count(objs.banana))
    }

    @Test
    fun GameTestState.`low alchemy converts one item to forty percent value`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.fire_rune, 3)
            player.inv[1] = InvObj(objs.nature_rune)
            player.inv[2] = InvObj(objs.bronze_axe)
            val value = objTypes[objs.bronze_axe].lowAlch
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_low_alchemy, 2) }

            assertEquals(0, player.count(objs.bronze_axe))
            assertEquals(value, player.count(objs.coins))
            assertEquals(0, player.count(objs.fire_rune))
            assertEquals(0, player.count(objs.nature_rune))
            assertEquals(310, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.human_castlowlvlalchemy.id, player.pendingSequence.id)
            assertTrue(player.pendingSpotanims.isNotEmpty())
        }

    @Test
    fun GameTestState.`high alchemy uses staff substitution and sixty percent value`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.nature_rune)
            player.inv[1] = InvObj(objs.bronze_axe)
            player.righthand = InvObj(objs.fire_staff)
            val value = objTypes[objs.bronze_axe].highAlch
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_high_alchemy, 1) }

            assertEquals(0, player.count(objs.bronze_axe))
            assertEquals(value, player.count(objs.coins))
            assertEquals(0, player.count(objs.nature_rune))
            assertEquals(650, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.human_casthighlvlalchemy.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`alchemy destroys either coal bag variant and clears stored coal`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.nature_rune, 2)
            player.inv[1] = InvObj(objs.coal_bag)
            player.righthand = InvObj(objs.fire_staff)
            player.setVarBit(varbits.coal_bag_storage_count, 27)
            assertTrue(objTypes[objs.coal_bag].param(params.no_alchemy) != 0)
            assertTrue(objTypes[objs.coal_bag_open].param(params.no_alchemy) != 0)

            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_high_alchemy, 1) }

            assertEquals(0, player.count(objs.coal_bag))
            assertEquals(0, deps.coalBag.count(player))
            assertEquals(objTypes[objs.coal_bag].highAlch, player.count(objs.coins))

            advance(5)
            player.inv[2] = InvObj(objs.coal_bag_open)
            player.setVarBit(varbits.coal_bag_storage_count, 36)
            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_low_alchemy, 2) }

            assertEquals(0, player.count(objs.coal_bag_open))
            assertEquals(0, deps.coalBag.count(player))
            assertEquals(
                objTypes[objs.coal_bag].highAlch + objTypes[objs.coal_bag_open].lowAlch,
                player.count(objs.coins),
            )
        }

    @Test
    fun GameTestState.`stackable targets alchemise one item per cast`() = runAlchemyTest { deps ->
        player.stats[stats.magic] = 99
        player.clearInv()
        player.inv[0] = InvObj(objs.nature_rune, 3)
        player.righthand = InvObj(objs.fire_staff)
        val value = objTypes[objs.nature_rune].highAlch

        player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_high_alchemy, 0) }

        assertEquals(1, player.count(objs.nature_rune))
        assertEquals(value, player.count(objs.coins))
    }

    @Test
    fun GameTestState.`target rune is reserved separately from the spell cost`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.nature_rune)
            player.righthand = InvObj(objs.fire_staff)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess {
                deps.alchemy.cast(this, objs.spell_high_alchemy, 0)
                assertMessageSent("You do not have enough runes to cast this spell.")
            }

            assertEquals(1, player.count(objs.nature_rune))
            assertEquals(0, player.count(objs.coins))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`non alchable target rejects before rune or xp use`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.fire_rune, 5)
            player.inv[1] = InvObj(objs.nature_rune)
            val unalchable =
                objTypes.values.firstOrNull { it.param(params.no_alchemy) != 0 }
                    ?: error("Cache has no ObjType with the `no_alchemy` param.")
            player.inv[2] = InvObj(unalchable)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess {
                deps.alchemy.cast(this, objs.spell_high_alchemy, 2)
                assertMessageSent("You can't alchemise that!")
            }

            assertEquals(5, player.count(objs.fire_rune))
            assertEquals(1, player.count(objs.nature_rune))
            assertEquals(1, player.count(unalchable))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`fountain of rune cast consumes no runes and grants no xp`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.bronze_axe)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_high_alchemy, 0) }

            assertEquals(0, player.count(objs.bronze_axe))
            assertEquals(objTypes[objs.bronze_axe].highAlch, player.count(objs.coins))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`low and high alchemy enforce their independent cast rates`() =
        runAlchemyTest { deps ->
            player.stats[stats.magic] = 99
            player.clearInv()
            player.inv[0] = InvObj(objs.fire_rune, 16)
            player.inv[1] = InvObj(objs.nature_rune, 4)
            player.inv[2] = InvObj(objs.bronze_axe)
            player.inv[3] = InvObj(objs.bronze_axe)

            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_low_alchemy, 2) }
            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_low_alchemy, 3) }
            assertEquals(1, player.count(objs.bronze_axe))

            advance(3)
            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_low_alchemy, 3) }
            assertEquals(0, player.count(objs.bronze_axe))

            player.inv[2] = InvObj(objs.bronze_axe)
            player.inv[3] = InvObj(objs.bronze_axe)
            advance(3)
            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_high_alchemy, 2) }
            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_high_alchemy, 3) }
            assertEquals(1, player.count(objs.bronze_axe))

            advance(5)
            player.withProtectedAccess { deps.alchemy.cast(this, objs.spell_high_alchemy, 3) }
            assertEquals(0, player.count(objs.bronze_axe))
        }

    private fun GameTestState.runAlchemyTest(
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
            install(CoalBagModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val alchemy: AlchemySpells,
        val boneConversion: BoneConversionSpells,
        val coalBag: CoalBagStorage,
        val spells: MagicSpellRegistry,
    )
}
