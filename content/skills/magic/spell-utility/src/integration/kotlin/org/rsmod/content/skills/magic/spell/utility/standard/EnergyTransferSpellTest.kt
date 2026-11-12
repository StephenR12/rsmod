package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
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
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.interact.PlayerTInteractions
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.api.specials.energy.SpecialAttackEnergy
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
import org.rsmod.api.testing.capture.CaptureClient
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.obj.Wearpos

@Execution(ExecutionMode.SAME_THREAD)
class EnergyTransferSpellTest {
    @Test
    fun GameTestState.`cache 233 pins Energy Transfer spell and presentation contracts`() =
        runEnergyTransferTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_energy_transfer))

            assertEquals(9133, spell.obj.id)
            assertEquals(91, spell.levelReq)
            assertEquals(100.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 3, objs.law_rune to 2, objs.nature_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_energy_transfer.id ushr 16)
            assertEquals(139, components.magic_spellbook_energy_transfer.id and 0xFFFF)
            assertEquals(
                "Cast",
                deps.components[components.magic_spellbook_energy_transfer].targetVerb,
            )
            assertEquals(
                emptyList<String>(),
                deps.components[components.magic_spellbook_energy_transfer].op.toList(),
            )
            assertEquals(4414, seqs.quest_lunar_spell_energy_exchange.id)
            assertEquals(734, spotanims.quest_lunar_spellbook_energy_exchange_spot_anim.id)
            assertEquals(2885, synths.lunar_energy_transfer.id)
            assertEquals(2906, synths.lunar_transfer_other.id)
            assertEquals(1_000, SpecialAttackEnergy.MAX_ENERGY)
            assertEquals(10_000, constants.run_max_energy)
        }

    @Test
    fun GameTestState.`cast transfers full run and special and costs exact runes xp hp and fx`() =
        runEnergyTransferTest { deps ->
            val target = preparePlayers()
            target.runEnergy = 1_234
            target.setVarp(varps.sa_energy, 123)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.energyTransfer.cast(this, target) }

            assertEquals(89, player.hitpoints)
            assertEquals(0, player.vars[varps.sa_energy])
            assertEquals(10_000, target.runEnergy)
            assertEquals(1_000, target.vars[varps.sa_energy])
            assertEquals(1_000, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(listOf(0, 0, 0), runeCounts())
            assertEquals(seqs.quest_lunar_spell_energy_exchange.id, player.pendingSequence.id)
            val spot = EntitySpotanim(target.pendingSpotanims.getLong(0))
            assertEquals(spotanims.quest_lunar_spellbook_energy_exchange_spot_anim.id, spot.id)
            assertEquals(96, spot.height)
            assertEquals(
                synths.lunar_energy_transfer.id,
                client.filterIsInstance<SynthSound>().single().id,
            )
            val targetClient = target.client as CaptureClient
            assertEquals(
                synths.lunar_transfer_other.id,
                targetClient.filterIsInstance<SynthSound>().single().id,
            )
            val hitmark = Hitmark(player.activeHitmarks.getLong(0))
            assertEquals(10, hitmark.damage)
            assertTrue(hitmark.isNoSource)
            assertEquals(1, player.activeHeadbars.size)
        }

    @Test
    fun GameTestState.`eleven hp succeeds to one while ten and lower reject with exact message`() =
        runEnergyTransferTest { deps ->
            val target = preparePlayers(casts = 2)
            player.setCurrentLevel(stats.hitpoints, 10)
            reject(deps, target, "You need more hitpoints to cast this spell.")

            player.setCurrentLevel(stats.hitpoints, 11)
            player.withProtectedAccess { deps.energyTransfer.cast(this, target) }

            assertEquals(1, player.hitpoints)
            assertEquals(0, player.vars[varps.sa_energy])
            assertEquals(1_000, target.vars[varps.sa_energy])
            assertEquals(listOf(3, 2, 1), runeCounts())
        }

    @Test
    fun GameTestState.`self dead Accept Aid and incomplete special failures are atomic`() =
        runEnergyTransferTest { deps ->
            val target = preparePlayers()
            player.withProtectedAccess { deps.energyTransfer.cast(this, player) }
            target.setCurrentLevel(stats.hitpoints, 0)
            reject(deps, target)
            target.setCurrentLevel(stats.hitpoints, 99)
            target.setVarBit(varbits.option_acceptaid, 0)
            reject(deps, target, "Recipient is not accepting aid.")
            target.setVarBit(varbits.option_acceptaid, 1)
            player.setVarp(varps.sa_energy, 999)
            reject(deps, target)
        }

    @Test
    fun GameTestState.`members quest spellbook Magic and rune failures are atomic`() =
        runEnergyTransferTest { deps ->
            val target = preparePlayers()

            player.members = false
            reject(deps, target, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, target, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps, target)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 90
            reject(deps, target, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.inv[2] = null
            reject(deps, target, "You do not have enough Nature Runes to cast this spell.")
            player.worn[Wearpos.RightHand.slot] = InvObj(objs.bryophytas_staff)
            reject(deps, target, "You do not have enough Nature Runes to cast this spell.")
        }

    @Test
    fun GameTestState.`single Wilderness and recent PvP-world combat block atomically`() =
        runEnergyTransferTest { deps ->
            val target = preparePlayers()
            player.setVarBit(varbits.inside_wilderness, 1)
            player.setVarBit(varbits.multiway_indicator, 1)
            target.setVarBit(varbits.inside_wilderness, 1)
            target.setVarBit(varbits.multiway_indicator, 0)
            reject(deps, target)

            target.setVarBit(varbits.multiway_indicator, 1)
            player.withProtectedAccess { deps.energyTransfer.cast(this, target) }
            assertEquals(listOf(0, 0, 0), runeCounts())

            advance(5)
            prepareRunes()
            player.setCurrentLevel(stats.hitpoints, 99)
            player.setVarp(varps.sa_energy, 1_000)
            player.setVarBit(varbits.inside_wilderness, 0)
            target.setVarBit(varbits.inside_wilderness, 0)
            deps.realm.updateConfig(deps.realm.config.copy(worldFlags = WorldFlag.Pvp.mask))
            target.setVarp(varps.lastcombat_pvp, 1)
            reject(deps, target)

            target.setVarp(varps.lastcombat_pvp, -100)
            player.withProtectedAccess { deps.energyTransfer.cast(this, target) }
            assertEquals(listOf(0, 0, 0), runeCounts())
        }

    @Test
    fun GameTestState.`ordinary five tick lock and Fountain behavior are enforced`() =
        runEnergyTransferTest { deps ->
            val first = preparePlayers(casts = 2)
            val second = registerRecipient("Second")

            player.withProtectedAccess { deps.energyTransfer.cast(this, first) }
            player.setCurrentLevel(stats.hitpoints, 99)
            player.setVarp(varps.sa_energy, 1_000)
            player.withProtectedAccess { deps.energyTransfer.cast(this, second) }
            assertEquals(listOf(3, 2, 1), runeCounts())
            advance(4)
            player.withProtectedAccess { deps.energyTransfer.cast(this, second) }
            assertEquals(listOf(3, 2, 1), runeCounts())
            advance()
            player.withProtectedAccess { deps.energyTransfer.cast(this, second) }
            assertEquals(listOf(0, 0, 0), runeCounts())

            advance(5)
            player.setCurrentLevel(stats.hitpoints, 99)
            player.setVarp(varps.sa_energy, 1_000)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val magicXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.energyTransfer.cast(this, first) }
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
            assertEquals(89, player.hitpoints)
            assertEquals(0, player.vars[varps.sa_energy])
        }

    @Test
    fun GameTestState.`player target interaction casts at standard spell range`() =
        runEnergyTransferTest { deps ->
            val target = preparePlayers(distance = 8)
            target.runEnergy = 1_234

            deps.interactions.interact(
                player,
                target,
                components.magic_spellbook_energy_transfer,
                comsub = -1,
                objType = null,
            )
            advance()

            assertEquals(10_000, target.runEnergy)
            assertEquals(0, player.vars[varps.sa_energy])
            assertEquals(listOf(0, 0, 0), runeCounts())
        }

    private fun GameTestScope.preparePlayers(casts: Int = 1, distance: Int = 1): Player {
        player.members = true
        player.displayName = "Caster"
        player.stats[stats.magic] = 99
        player.setBaseLevel(stats.hitpoints, 99)
        player.setCurrentLevel(stats.hitpoints, 99)
        player.setVarBit(varbits.lunar_quest_main, 190)
        player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.multiway_indicator, 0)
        player.setVarp(varps.sa_energy, 1_000)
        player.clearInv()
        client.clear()
        if (casts > 0) {
            prepareRunes(casts)
        }
        return registerRecipient("Recipient", distance)
    }

    private fun GameTestScope.prepareRunes(casts: Int = 1) {
        player.inv[0] = InvObj(objs.astral_rune, casts * 3)
        player.inv[1] = InvObj(objs.law_rune, casts * 2)
        player.inv[2] = InvObj(objs.nature_rune, casts)
    }

    private fun GameTestScope.registerRecipient(name: String, distance: Int = 1): Player =
        registerPlayer(coords = player.coords.translateX(distance)).also {
            it.members = true
            it.displayName = name
            it.setBaseLevel(stats.hitpoints, 99)
            it.setCurrentLevel(stats.hitpoints, 99)
            it.setVarBit(varbits.option_acceptaid, 1)
            it.setVarBit(varbits.inside_wilderness, 0)
            it.setVarBit(varbits.multiway_indicator, 0)
            it.setVarp(varps.lastcombat_pvp, -100)
            it.setVarp(varps.sa_energy, 0)
            it.runEnergy = 0
        }

    private fun GameTestScope.reject(deps: Dependencies, target: Player, message: String? = null) {
        val beforeRunes = runeCounts()
        val beforeXp = player.statMap.getFineXP(stats.magic)
        val beforeCasterHp = player.hitpoints
        val beforeCasterSpec = player.vars[varps.sa_energy]
        val beforeTargetSpec = target.vars[varps.sa_energy]
        val beforeTargetRun = target.runEnergy
        player.withProtectedAccess {
            deps.energyTransfer.cast(this, target)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(beforeRunes, runeCounts())
        assertEquals(beforeXp, player.statMap.getFineXP(stats.magic))
        assertEquals(beforeCasterHp, player.hitpoints)
        assertEquals(beforeCasterSpec, player.vars[varps.sa_energy])
        assertEquals(beforeTargetSpec, target.vars[varps.sa_energy])
        assertEquals(beforeTargetRun, target.runEnergy)
    }

    private fun GameTestScope.runeCounts(): List<Int> =
        listOf(
            player.count(objs.astral_rune),
            player.count(objs.law_rune),
            player.count(objs.nature_rune),
        )

    private fun GameTestState.runEnergyTransferTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            EnergyTransferSpellScript::class,
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
            bind(SpecialAttackEnergy::class.java).`in`(Scopes.SINGLETON)
            bind(UtilitySpellCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val energyTransfer: EnergyTransferSpell,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val interactions: PlayerTInteractions,
        val realm: Realm,
    )
}
