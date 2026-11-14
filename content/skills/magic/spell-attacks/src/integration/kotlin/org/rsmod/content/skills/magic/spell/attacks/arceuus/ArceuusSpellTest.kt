package org.rsmod.content.skills.magic.spell.attacks.arceuus

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.MarkOfDarknessStateRepository
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.magic.WardOfArceuusStateRepository
import org.rsmod.api.player.righthand
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackRegistry
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.api.spells.attack.attack
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
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingImpactScript
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStateRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStatus
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.util.ParamMapBuilder

class ArceuusSpellTest {
    @Test
    fun GameTestState.`cache 238 defines mark grasp and demonbane contracts`() =
        runArceuusTest { deps ->
            val contracts =
                listOf(
                    Contract(
                        objs.spell_ghostly_grasp,
                        level = 35,
                        xp = 22.5,
                        runes = mapOf(objs.air_rune to 4, objs.chaos_rune to 1),
                    ),
                    Contract(
                        objs.spell_skeletal_grasp,
                        level = 56,
                        xp = 33.0,
                        runes = mapOf(objs.earth_rune to 8, objs.death_rune to 1),
                    ),
                    Contract(
                        objs.spell_undead_grasp,
                        level = 79,
                        xp = 46.5,
                        runes = mapOf(objs.fire_rune to 12, objs.blood_rune to 1),
                    ),
                    Contract(
                        objs.spell_mark_of_darkness,
                        level = 59,
                        xp = 70.0,
                        runes = mapOf(objs.soul_rune to 1, objs.cosmic_rune to 1),
                    ),
                    Contract(
                        objs.spell_ward_of_arceuus,
                        level = 73,
                        xp = 83.0,
                        runes =
                            mapOf(objs.cosmic_rune to 1, objs.nature_rune to 2, objs.soul_rune to 4),
                    ),
                    Contract(
                        objs.spell_lesser_corruption,
                        level = 64,
                        xp = 75.0,
                        runes = mapOf(objs.death_rune to 1, objs.soul_rune to 2),
                    ),
                    Contract(
                        objs.spell_greater_corruption,
                        level = 85,
                        xp = 95.0,
                        runes = mapOf(objs.blood_rune to 1, objs.soul_rune to 3),
                    ),
                    Contract(
                        objs.spell_inferior_demonbane,
                        level = 44,
                        xp = 27.0,
                        runes = mapOf(objs.fire_rune to 3, objs.chaos_rune to 1),
                    ),
                    Contract(
                        objs.spell_superior_demonbane,
                        level = 62,
                        xp = 36.0,
                        runes = mapOf(objs.fire_rune to 5, objs.soul_rune to 1),
                    ),
                    Contract(
                        objs.spell_dark_demonbane,
                        level = 82,
                        xp = 43.5,
                        runes = mapOf(objs.fire_rune to 7, objs.soul_rune to 2),
                    ),
                )
            val attacks = deps.registry()
            for (contract in contracts) {
                val spell = requireNotNull(deps.magicSpells.getObjSpell(contract.spell))
                assertEquals(contract.level, spell.levelReq)
                assertEquals(contract.xp, spell.castXp)
                assertEquals(Spellbook.Arceuus, spell.spellbook)
                assertEquals(MagicSpellType.Combat, spell.type)
                assertEquals(contract.runes, spell.objReqs.associate { it.obj to it.count })
            }
            assertNotNull(attacks[objs.spell_ghostly_grasp])
            assertNotNull(attacks[objs.spell_skeletal_grasp])
            assertNotNull(attacks[objs.spell_undead_grasp])
            assertNotNull(attacks[objs.spell_inferior_demonbane])
            assertNotNull(attacks[objs.spell_superior_demonbane])
            assertNotNull(attacks[objs.spell_dark_demonbane])
            assertTrue(attacks[objs.spell_mark_of_darkness] == null)
            assertTrue(attacks[objs.spell_ward_of_arceuus] == null)
            assertTrue(attacks[objs.spell_lesser_corruption] == null)
            assertTrue(attacks[objs.spell_greater_corruption] == null)
            assertFalse(
                deps.magicSpells.offensiveCombatSpells().any {
                    it.obj.id in
                        setOf(
                            objs.spell_mark_of_darkness.id,
                            objs.spell_ward_of_arceuus.id,
                            objs.spell_lesser_corruption.id,
                            objs.spell_greater_corruption.id,
                        )
                }
            )
            assertEquals(listOf(150), deps.dbRows.types.getValue(A_KINGDOM_DIVIDED_ROW).data[19])
        }

    @Test
    fun GameTestState.`cache presentation refs resolve to the cutoff assets`() = runArceuusTest {
        assertEquals(175, components.magic_spellbook_mark_of_darkness.id and 0xFFFF)
        assertEquals(172, components.magic_spellbook_inferior_demonbane.id and 0xFFFF)
        assertEquals(173, components.magic_spellbook_superior_demonbane.id and 0xFFFF)
        assertEquals(174, components.magic_spellbook_dark_demonbane.id and 0xFFFF)
        assertEquals(176, components.magic_spellbook_ghostly_grasp.id and 0xFFFF)
        assertEquals(177, components.magic_spellbook_skeletal_grasp.id and 0xFFFF)
        assertEquals(178, components.magic_spellbook_undead_grasp.id and 0xFFFF)
        assertEquals(179, components.magic_spellbook_ward_of_arceuus.id and 0xFFFF)
        assertEquals(180, components.magic_spellbook_lesser_corruption.id and 0xFFFF)
        assertEquals(181, components.magic_spellbook_greater_corruption.id and 0xFFFF)
        assertEquals(8972, seqs.human_spellcast_grasp.id)
        assertEquals(8977, seqs.human_spellcast_demonbane.id)
        assertEquals(8982, seqs.self_imbue.id)
        assertEquals(8996, seqs.corruption_cast.id)
        assertEquals(8985, seqs.mark_of_darkness.id)
        assertEquals(1851, spotanims.ward_of_arceuus_cast.id)
        assertEquals(1877, spotanims.lesser_corruption_cast.id)
        assertEquals(1878, spotanims.greater_corruption_cast.id)
        assertEquals(1879, spotanims.lesser_corruption_hit.id)
        assertEquals(1880, spotanims.greater_corruption_hit.id)
        assertEquals(1852, spotanims.mark_of_darkness_cast.id)
        assertEquals(1886, spotanims.mark_of_darkness_end.id)
        assertEquals(listOf(1856, 1859, 1862), GraspSpellDefinitions.all.map { it.castSpot.id })
        assertEquals(listOf(1857, 1860, 1863), GraspSpellDefinitions.all.map { it.hitSpot.id })
        assertEquals(listOf(1858, 1861, 1864), GraspSpellDefinitions.all.map { it.missSpot.id })
        assertEquals(listOf(1865, 1867, 1869), DemonbaneSpellDefinitions.all.map { it.castSpot.id })
        assertEquals(listOf(1866, 1868, 1870), DemonbaneSpellDefinitions.all.map { it.hitSpot.id })
    }

    @Test
    fun GameTestState.`demonbane maxima and quest exceptions match the cutoff`() = runArceuusTest {
        assertEquals(listOf(16, 23, 30), DemonbaneSpellDefinitions.all.map { it.baseMaxHit })
        assertEquals(
            listOf(false, true, true),
            DemonbaneSpellDefinitions.all.map { it.requiresKingdomDivided },
        )
    }

    @Test
    fun GameTestState.`grasp chances durations and mark doubling match the cutoff`() =
        runArceuusTest {
            assertEquals(listOf(10, 25, 50), GraspSpellDefinitions.all.map { it.bindChance(false) })
            assertEquals(listOf(20, 50, 100), GraspSpellDefinitions.all.map { it.bindChance(true) })
            assertEquals(listOf(2, 3, 4), GraspSpellDefinitions.all.map { it.bindDuration })
            assertEquals(listOf(12, 17, 24), GraspSpellDefinitions.all.map { it.baseMaxHit })
            assertEquals(
                listOf(false, true, true),
                GraspSpellDefinitions.all.map { it.requiresKingdomDivided },
            )
        }

    @Test
    fun GameTestState.`ghostly grasp is the pre kingdom divided exception`() =
        runArceuusTest { deps ->
            player.members = true
            player.stats[stats.magic] = 99
            player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
            giveGraspRunes()
            repeat(6) { random.next = 0 }
            val target = spawnNpc(player.coords.translate(3, 0), targetType)

            val ghostly = deps.attack(objs.spell_ghostly_grasp)
            player.withProtectedAccess { ghostly.handler.attack(this, target, ghostly.attack) }
            assertEquals(99, player.count(objs.chaos_rune))

            val skeletal = deps.attack(objs.spell_skeletal_grasp)
            player.withProtectedAccess { skeletal.handler.attack(this, target, skeletal.attack) }
            assertEquals(100, player.count(objs.death_rune))
            assertMessageSent("You need to complete A Kingdom Divided to cast this spell.")
        }

    @Test
    fun GameTestState.`inferior demonbane is the pre kingdom divided exception`() =
        runArceuusTest { deps ->
            player.members = true
            player.stats[stats.magic] = 99
            player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
            giveDemonbaneRunes()
            random.next = 0
            random.then = 0
            val target = spawnNpc(player.coords.translate(3, 0), demonTarget)

            val inferior = deps.attack(objs.spell_inferior_demonbane)
            player.withProtectedAccess { inferior.handler.attack(this, target, inferior.attack) }
            assertEquals(99, player.count(objs.chaos_rune))

            val superior = deps.attack(objs.spell_superior_demonbane)
            player.withProtectedAccess { superior.handler.attack(this, target, superior.attack) }
            assertEquals(100, player.count(objs.soul_rune))
            assertMessageSent("You need to complete A Kingdom Divided to cast this spell.")
        }

    @Test
    fun GameTestState.`demonbane rejects non demons and players atomically`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveDemonbaneRunes()
            val startingXp = player.statMap.getFineXP(stats.magic)
            val living = spawnNpc(player.coords.translate(3, 0), targetType)
            val fixture = deps.attack(objs.spell_dark_demonbane)

            player.withProtectedAccess { fixture.handler.attack(this, living, fixture.attack) }
            assertEquals(100, player.count(objs.fire_rune))
            assertEquals(100, player.count(objs.soul_rune))
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
            assertMessageSent("This spell can only be cast on demonic creatures.")

            client.clear()
            val otherPlayer = registerPlayer(player.coords.translate(3, 0))
            player.withProtectedAccess { fixture.handler.attack(this, otherPlayer, fixture.attack) }
            assertEquals(100, player.count(objs.fire_rune))
            assertEquals(100, player.count(objs.soul_rune))
            assertEquals(startingXp, player.statMap.getFineXP(stats.magic))
            assertMessageSent("This spell can only be cast on demonic creatures.")
        }

    @Test
    fun GameTestState.`dark demonbane consumes cutoff runes and lands after two ticks`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveDemonbaneRunes()
            val target = spawnNpc(player.coords.translate(3, 0), demonTarget)
            val fixture = deps.attack(objs.spell_dark_demonbane)
            val startingXp = player.statMap.getFineXP(stats.magic)
            val startingHp = target.hitpoints
            random.next = 0
            random.then = 10

            player.withProtectedAccess { fixture.handler.attack(this, target, fixture.attack) }

            assertEquals(93, player.count(objs.fire_rune))
            assertEquals(98, player.count(objs.soul_rune))
            assertEquals(435, player.statMap.getFineXP(stats.magic) - startingXp)
            assertEquals(seqs.human_spellcast_demonbane.id, player.pendingSequence.id)
            assertEquals(
                spotanims.dark_demonbane_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            val impact = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.dark_demonbane_hit.id, impact.id)
            assertEquals(30, impact.delay)
            assertEquals(startingHp, target.hitpoints)
            advance()
            assertEquals(startingHp, target.hitpoints)
            advance()
            assertEquals(startingHp - 10, target.hitpoints)
        }

    @Test
    fun GameTestState.`mark and purging staff flow through production demonbane max hit`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveMarkRunes(1)
            val target = spawnNpc(player.coords.translate(3, 0), demonTarget)
            val fixture = deps.attack(objs.spell_dark_demonbane)
            val cast = MagicRuneManager.CastResult.Success.Consumed(usedSunfire = false)

            player.withProtectedAccess {
                assertEquals(
                    0..30,
                    deps.manager.calculateMaxHit(this, target, fixture.attack, cast, 30),
                )
                deps.mark.cast(this)
                assertEquals(
                    0..37,
                    deps.manager.calculateMaxHit(this, target, fixture.attack, cast, 30),
                )
                player.righthand = InvObj(objs.purging_staff)
                assertEquals(
                    0..49,
                    deps.manager.calculateMaxHit(this, target, fixture.attack, cast, 30),
                )
            }
        }

    @Test
    fun GameTestState.`mark gates are atomic and cache quest state 150 unlocks casting`() =
        runArceuusTest { deps ->
            player.stats[stats.magic] = 99
            giveMarkRunes(2)

            player.members = false
            player.withProtectedAccess { deps.mark.cast(this) }
            assertEquals(2, player.count(objs.soul_rune))
            assertMessageSent("You need to be on a members' world to cast this spell.")

            client.clear()
            player.members = true
            player.withProtectedAccess { deps.mark.cast(this) }
            assertEquals(2, player.count(objs.soul_rune))
            assertMessageSent("You need to complete A Kingdom Divided to cast this spell.")

            prepareArceuus()
            giveMarkRunes(1)
            player.withProtectedAccess { deps.mark.cast(this) }
            assertEquals(0, player.count(objs.soul_rune))
            assertTrue(deps.markStates.isActive(player))
        }

    @Test
    fun GameTestState
        .`mark lasts 300 ticks warns expires and has an independent ten tick recast lock`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveMarkRunes(2)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.mark.cast(this) }

            assertEquals(1, player.count(objs.soul_rune))
            assertEquals(700, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.mark_of_darkness.id, player.pendingSequence.id)
            assertEquals(
                spotanims.mark_of_darkness_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(
                MarkOfDarknessSpell.DURATION_CYCLES,
                deps.markStates.remainingCycles(player),
            )
            assertEquals(1, player.vars[varbits.buff_mark_of_darkness_disabled])
            assertMessageSent("<col=ef1020>You have placed a Mark of Darkness upon yourself.</col>")

            player.withProtectedAccess { deps.mark.cast(this) }
            assertEquals(1, player.count(objs.soul_rune))
            advance(MarkOfDarknessSpell.COOLDOWN_CYCLES)
            assertEquals(0, player.vars[varbits.buff_mark_of_darkness_disabled])
            assertTrue(deps.markStates.isActive(player))

            player.withProtectedAccess { deps.mark.cast(this) }
            assertEquals(0, player.count(objs.soul_rune))
            assertEquals(
                MarkOfDarknessSpell.DURATION_CYCLES,
                deps.markStates.remainingCycles(player),
            )

            advance(MarkOfDarknessSpell.DURATION_CYCLES - MarkOfDarknessSpell.WARNING_LEAD_CYCLES)
            assertMessageSent("Your Mark of Darkness is about to run out.")
            assertTrue(deps.markStates.isActive(player))
            advance(MarkOfDarknessSpell.WARNING_LEAD_CYCLES)
            assertFalse(deps.markStates.isActive(player))
        }

    @Test
    fun GameTestState.`purging staff fixes mark duration at fifteen hundred ticks at cast time`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveMarkRunes(1)
            player.righthand = InvObj(objs.purging_staff)

            player.withProtectedAccess { deps.mark.cast(this) }

            assertEquals(
                MarkOfDarknessSpell.PURGING_STAFF_DURATION_CYCLES,
                deps.markStates.remainingCycles(player),
            )
            player.righthand = null
            assertEquals(
                MarkOfDarknessSpell.PURGING_STAFF_DURATION_CYCLES,
                deps.markStates.remainingCycles(player),
            )
        }

    @Test
    fun GameTestState
        .`ward gates atomically and uses cache runes presentation xp and base level duration`() =
        runArceuusTest { deps ->
            player.stats[stats.magic] = 99
            player.setBaseLevel(stats.magic, 73)
            giveWardRunes(casts = 2)

            player.members = false
            player.withProtectedAccess { deps.ward.cast(this) }
            assertEquals(8, player.count(objs.soul_rune))
            assertMessageSent("You need to be on a members' world to cast this spell.")

            client.clear()
            player.members = true
            player.withProtectedAccess { deps.ward.cast(this) }
            assertEquals(8, player.count(objs.soul_rune))
            assertMessageSent("You need to complete A Kingdom Divided to cast this spell.")

            client.clear()
            player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
            player.setVarBit(varbits.a_kingdom_divided, 150)
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.ward.cast(this) }

            assertEquals(4, player.count(objs.soul_rune))
            assertEquals(2, player.count(objs.nature_rune))
            assertEquals(1, player.count(objs.cosmic_rune))
            assertEquals(830, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.self_imbue.id, player.pendingSequence.id)
            assertEquals(
                spotanims.ward_of_arceuus_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(73, deps.wardStates.remainingCycles(player))
            assertEquals(1, player.vars[varbits.arceuus_ward_cooldown])
            assertMessageSent(
                "<col=0000b2>Your defence against Arceuus magic has been strengthened.</col>"
            )

            player.withProtectedAccess { deps.ward.cast(this) }
            assertEquals(4, player.count(objs.soul_rune))
            advance(WardOfArceuusSpell.COOLDOWN_CYCLES)
            assertEquals(0, player.vars[varbits.arceuus_ward_cooldown])
            assertEquals(23, deps.wardStates.remainingCycles(player))
            advance(23)
            assertFalse(deps.wardStates.isActive(player))
        }

    @Test
    fun GameTestState.`ward floors ten percent reduction on damage from demon npc sources`() =
        runArceuusTest { deps ->
            player.stats[stats.hitpoints] = 99
            deps.wardStates.activate(player, duration = 100)
            val demon = spawnNpc(player.coords.translate(1, 0), demonTarget)
            val ordinary = spawnNpc(player.coords.translate(2, 0), targetType)

            assertEquals(18, player.queueHit(demon, 1, HitType.Melee, damage = 19).damage)
            assertEquals(9, player.queueHit(demon, 1, HitType.Magic, damage = 9).damage)
            assertEquals(19, player.queueHit(ordinary, 1, HitType.Ranged, damage = 19).damage)

            deps.wardStates.clear(player)
            assertEquals(19, player.queueHit(demon, 1, HitType.Melee, damage = 19).damage)
        }

    @Test
    fun GameTestState.`ward limits a successful grasp against a player to one tick`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveGraspRunes()
            val target = registerPlayer(player.coords.translate(1, 0))
            deps.wardStates.activate(target, duration = 100)
            repeat(6) { random.next = 0 }
            val fixture = deps.attack(objs.spell_undead_grasp)

            player.withProtectedAccess { fixture.handler.attack(this, target, fixture.attack) }
            advance(2)

            assertEquals(BindingStatus.Held, deps.bindings.status(target))
            assertEquals(
                1,
                requireNotNull(deps.bindings.stateFor(target)).heldUntil - target.currentMapClock,
            )
        }

    @Test
    fun GameTestState.`corruption gates atomically and shares a fifty tick cooldown`() =
        runArceuusTest { deps ->
            player.stats[stats.magic] = 99
            giveCorruptionRunes()

            player.members = false
            player.withProtectedAccess { deps.corruptionSpells.cast(this, CorruptionTier.Lesser) }
            assertEquals(10, player.count(objs.death_rune))
            assertMessageSent("You need to be on a members' world to cast this spell.")

            client.clear()
            player.members = true
            player.withProtectedAccess { deps.corruptionSpells.cast(this, CorruptionTier.Lesser) }
            assertEquals(10, player.count(objs.death_rune))
            assertMessageSent("You need to complete A Kingdom Divided to cast this spell.")

            prepareArceuus()
            giveCorruptionRunes()
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.corruptionSpells.cast(this, CorruptionTier.Lesser) }

            assertEquals(9, player.count(objs.death_rune))
            assertEquals(8, player.count(objs.soul_rune))
            assertEquals(750, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.corruption_cast.id, player.pendingSequence.id)
            assertEquals(
                spotanims.lesser_corruption_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(CorruptionTier.Lesser, deps.corruptionStates.armedTier(player))
            assertEquals(1, player.vars[varbits.arceuus_corruption_cooldown])

            player.withProtectedAccess { deps.corruptionSpells.cast(this, CorruptionTier.Greater) }
            assertEquals(10, player.count(objs.blood_rune))
            advance(CorruptionSpells.COOLDOWN_CYCLES)
            assertEquals(0, player.vars[varbits.arceuus_corruption_cooldown])
        }

    @Test
    fun GameTestState
        .`greater corruption procs on a successful player hit and drains two four six`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveCorruptionRunes()
            player.withProtectedAccess { deps.corruptionSpells.cast(this, CorruptionTier.Greater) }
            val target = registerPlayer(player.coords.translate(1, 0))
            target.stats[stats.hitpoints] = 99
            target.stats[stats.prayer] = 99
            random.next = 0

            target.queueHit(player, 1, HitType.Melee, damage = 1, successful = true)
            advance()

            assertEquals(null, deps.corruptionStates.armedTier(player))
            assertTrue(deps.corruptionStates.isCorrupted(target))
            assertEquals(10, requireNotNull(deps.corruptionStates.statusFor(target)).interval)
            assertMessageSent("Your target has been corrupted.")

            advance(10)
            assertEquals(97, target.stats[stats.prayer])
            advance(10)
            assertEquals(93, target.stats[stats.prayer])
            advance(10)
            assertEquals(87, target.stats[stats.prayer])
            assertFalse(deps.corruptionStates.isCorrupted(target))
        }

    @Test
    fun GameTestState.`mark guarantees corruption and halves its drain interval`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveMarkRunes(1)
            player.withProtectedAccess { deps.mark.cast(this) }
            giveCorruptionRunes()
            player.withProtectedAccess { deps.corruptionSpells.cast(this, CorruptionTier.Lesser) }
            val target = registerPlayer(player.coords.translate(1, 0))
            target.stats[stats.hitpoints] = 99
            target.stats[stats.prayer] = 99
            // The queued combat hit resolves a defend sound before corruption handles the impact.
            random.next = 0

            target.queueHit(player, 1, HitType.Ranged, damage = 1, successful = true)
            advance()

            assertTrue(deps.corruptionStates.isCorrupted(target))
            assertEquals(5, requireNotNull(deps.corruptionStates.statusFor(target)).interval)
            advance(5)
            assertEquals(98, target.stats[stats.prayer])
            advance(5)
            assertEquals(96, target.stats[stats.prayer])
            advance(5)
            assertEquals(93, target.stats[stats.prayer])
        }

    @Test
    fun GameTestState.`misses do not consume corruption but a failed proc does`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveCorruptionRunes()
            player.withProtectedAccess { deps.corruptionSpells.cast(this, CorruptionTier.Lesser) }
            val target = registerPlayer(player.coords.translate(1, 0))
            target.stats[stats.hitpoints] = 99

            target.queueHit(player, 1, HitType.Magic, damage = 0, successful = false)
            advance()
            assertEquals(CorruptionTier.Lesser, deps.corruptionStates.armedTier(player))

            random.next = 99
            target.queueHit(player, 1, HitType.Magic, damage = 0, successful = true)
            advance()
            assertEquals(null, deps.corruptionStates.armedTier(player))
            assertFalse(deps.corruptionStates.isCorrupted(target))
        }

    @Test
    fun GameTestState.`ward blocks corruption without consuming it and clears an active status`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveCorruptionRunes()
            player.withProtectedAccess { deps.corruptionSpells.cast(this, CorruptionTier.Greater) }
            val target = registerPlayer(player.coords.translate(1, 0))
            target.stats[stats.hitpoints] = 99
            deps.wardStates.activate(target, duration = 100)
            random.next = 0

            target.queueHit(player, 1, HitType.Melee, damage = 1, successful = true)
            advance()
            assertEquals(CorruptionTier.Greater, deps.corruptionStates.armedTier(player))
            assertFalse(deps.corruptionStates.isCorrupted(target))

            deps.wardStates.clear(target)
            target.queueHit(player, 1, HitType.Melee, damage = 1, successful = true)
            advance()
            assertEquals(null, deps.corruptionStates.armedTier(player))
            assertTrue(deps.corruptionStates.isCorrupted(target))

            deps.corruptionStates.apply(player, CorruptionTier.Lesser, interval = 10)
            prepareArceuus()
            giveWardRunes(casts = 1)
            player.withProtectedAccess { deps.ward.cast(this) }
            assertFalse(deps.corruptionStates.isCorrupted(player))
            assertTrue(deps.wardStates.isActive(player))
        }

    @Test
    fun GameTestState.`ghostly grasp has fixed two tick hit delay bind and immunity`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveGraspRunes()
            repeat(6) { random.next = 0 }
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val startHp = target.hitpoints
            val fixture = deps.attack(objs.spell_ghostly_grasp)

            player.withProtectedAccess { fixture.handler.attack(this, target, fixture.attack) }

            assertEquals(seqs.human_spellcast_grasp.id, player.pendingSequence.id)
            assertEquals(
                spotanims.ghostly_grasp_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertEquals(startHp, target.hitpoints)
            assertEquals(BindingStatus.Available, deps.bindings.status(target))
            advance(2)
            assertEquals(BindingStatus.Held, deps.bindings.status(target))
            assertEquals(
                2,
                requireNotNull(deps.bindings.stateFor(target)).heldUntil - target.currentMapClock,
            )

            advance(2)
            assertEquals(BindingStatus.Immune, deps.bindings.status(target))
            advance(2)
            assertEquals(BindingStatus.Available, deps.bindings.status(target))
        }

    @Test
    fun GameTestState.`mark makes undead grasp bind guaranteed after a successful accuracy roll`() =
        runArceuusTest { deps ->
            prepareArceuus()
            giveMarkRunes(1)
            giveGraspRunes()
            player.withProtectedAccess { deps.mark.cast(this) }
            repeat(6) { random.next = 99 }
            random.next = 0
            val target = spawnNpc(player.coords.translate(3, 0), targetType)
            val fixture = deps.attack(objs.spell_undead_grasp)

            player.withProtectedAccess { fixture.handler.attack(this, target, fixture.attack) }
            advance(2)

            assertEquals(BindingStatus.Held, deps.bindings.status(target))
            assertEquals(
                4,
                requireNotNull(deps.bindings.stateFor(target)).heldUntil - target.currentMapClock,
            )
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.prepareArceuus() {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.a_kingdom_divided, 150)
        player.clearInv()
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.giveMarkRunes(casts: Int) {
        player.inv[0] = InvObj(objs.soul_rune, casts)
        player.inv[1] = InvObj(objs.cosmic_rune, casts)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.giveGraspRunes() {
        player.inv[2] = InvObj(objs.air_rune, 100)
        player.inv[3] = InvObj(objs.chaos_rune, 100)
        player.inv[4] = InvObj(objs.earth_rune, 100)
        player.inv[5] = InvObj(objs.death_rune, 100)
        player.inv[6] = InvObj(objs.fire_rune, 100)
        player.inv[7] = InvObj(objs.blood_rune, 100)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.giveWardRunes(casts: Int) {
        player.inv[0] = InvObj(objs.cosmic_rune, casts)
        player.inv[1] = InvObj(objs.nature_rune, casts * 2)
        player.inv[2] = InvObj(objs.soul_rune, casts * 4)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.giveDemonbaneRunes() {
        player.inv[2] = InvObj(objs.fire_rune, 100)
        player.inv[3] = InvObj(objs.chaos_rune, 100)
        player.inv[4] = InvObj(objs.soul_rune, 100)
    }

    private fun org.rsmod.api.testing.scope.GameTestScope.giveCorruptionRunes() {
        player.inv[3] = InvObj(objs.death_rune, 10)
        player.inv[4] = InvObj(objs.blood_rune, 10)
        player.inv[5] = InvObj(objs.soul_rune, 10)
    }

    private fun GameTestState.runArceuusTest(
        body: org.rsmod.api.testing.scope.GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            ArceuusTestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            WeaponAttackStylesScript::class,
            NpcHitScript::class,
            PlayerHitScript::class,
            BindingImpactScript::class,
            GraspImpactScript::class,
            MarkOfDarknessSpellScript::class,
            WardOfArceuusSpellScript::class,
            CorruptionSpellScript::class,
            testBody = body,
        )

    private object ArceuusTestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
            bind(BindingStateRepository::class.java).`in`(Scopes.SINGLETON)
            bind(MarkOfDarknessStateRepository::class.java).`in`(Scopes.SINGLETON)
            bind(WardOfArceuusStateRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CorruptionStateRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    private data class Contract(
        val spell: ObjType,
        val level: Int,
        val xp: Double,
        val runes: Map<ObjType, Int>,
    )

    data class Fixture(val handler: SpellAttack, val attack: CombatAttack.Spell)

    data class Dependencies
    @Inject
    constructor(
        val grasps: GraspSpells,
        val demonbanes: DemonbaneSpells,
        val mark: MarkOfDarknessSpell,
        val markStates: MarkOfDarknessStateRepository,
        val ward: WardOfArceuusSpell,
        val wardStates: WardOfArceuusStateRepository,
        val corruptionSpells: CorruptionSpells,
        val corruptionStates: CorruptionStateRepository,
        val bindings: BindingStateRepository,
        val manager: SpellAttackManager,
        val magicSpells: MagicSpellRegistry,
        val dbRows: DbRowTypeList,
    ) {
        fun registry(): SpellAttackRegistry {
            val registry = SpellAttackRegistry()
            with(grasps) { with(SpellAttackRepository(registry)) { register(manager) } }
            with(demonbanes) { with(SpellAttackRepository(registry)) { register(manager) } }
            return registry
        }

        fun attack(spell: ObjType): Fixture =
            Fixture(
                requireNotNull(registry()[spell]),
                CombatAttack.Spell(
                    weapon = null,
                    spell = requireNotNull(magicSpells.getObjSpell(spell)),
                    defensive = false,
                ),
            )
    }

    private companion object {
        const val A_KINGDOM_DIVIDED_ROW = 81
        val targetType =
            npcTypeFactory.create {
                name = "Arceuus spell target"
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 100
                magic = 1
            }
        val demonTarget =
            npcTypeFactory.create {
                name = "Demonic Arceuus spell target"
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 100
                magic = 1
                paramMap = ParamMapBuilder().apply { this[params.demon] = 1 }.toParamMap()
            }
    }
}
