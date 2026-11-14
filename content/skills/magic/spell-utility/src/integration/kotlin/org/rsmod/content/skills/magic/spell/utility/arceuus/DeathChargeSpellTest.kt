package org.rsmod.content.skills.magic.spell.utility.arceuus

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.intVarp
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
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.npc.UnpackedNpcType

@Execution(ExecutionMode.SAME_THREAD)
class DeathChargeSpellTest {
    @Test
    fun GameTestState.`cache 238 pins normal upgraded and client state contracts`() =
        runDeathChargeTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_death_charge))

            assertEquals(15309, spell.obj.id)
            assertEquals(80, spell.levelReq)
            assertEquals(90.0, spell.castXp)
            assertEquals(Spellbook.Arceuus, spell.spellbook)
            assertEquals(MagicSpellType.Utility, spell.type)
            assertEquals(
                mapOf(objs.death_rune to 1, objs.blood_rune to 1, objs.soul_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(218, components.magic_spellbook_death_charge.id ushr 16)
            assertEquals(188, components.magic_spellbook_death_charge.id and 0xFFFF)
            assertEquals(
                "Cast",
                deps.components[components.magic_spellbook_death_charge].targetVerb,
            )
            assertEquals(8983, seqs.death_charge.id)
            assertEquals(8984, seqs.death_charge_npc_hit.id)
            assertEquals(1854, spotanims.death_charge_cast.id)
            assertEquals(1855, spotanims.death_charge_end.id)
            assertEquals(3288, spotanims.death_charge_upgrade_cast.id)
            assertEquals(3289, spotanims.death_charge_upgrade_end.id)
            assertEquals(12138, varbits.arceuus_death_charge_cooldown.id)
            assertEquals(12411, varbits.arceuus_death_charge_active.id)
            assertEquals(16269, varbits.death_charge_scroll_used.id)
            val scroll = deps.objTypes[objs.death_charge_scroll]
            assertEquals(30_806, scroll.id)
            assertEquals("Rite of vile transference", scroll.name)
            assertTrue(scroll.members)
            assertEquals("Read", scroll.iop[0])
            assertEquals(32, timers.magic_arceuus_death_charge_cooldown.id)
            assertEquals(33, timers.magic_arceuus_death_charge_expiry.id)
            assertEquals(1_000, SpecialAttackEnergy.MAX_ENERGY)
        }

    @Test
    fun GameTestState.`rite confirmation safely consumes the scroll and permanently upgrades`() =
        runDeathChargeTest { deps ->
            prepareCaster(casts = 0, upgraded = false)
            player.inv[0] = InvObj(objs.death_charge_scroll)

            player.withProtectedAccess {
                with(deps.script) { readDeathChargeScroll(player.inv, slot = 0) }
            }
            assertDialogueTextSent(
                "You can make out some faded words on the ancient parchment. It appears to be a " +
                    "demonic learning on spell efficiency. Would you like to absorb its knowledge?"
            )
            continueDialogue()
            confirmUpgrade()

            assertEquals(0, player.count(objs.death_charge_scroll))
            assertEquals(1, player.vars[varbits.death_charge_scroll_used])
            assertEquals(seqs.death_charge.id, player.pendingSequence.id)
            assertEquals(
                spotanims.death_charge_upgrade_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )
            assertMessageSent("You study the scroll and upgrade an Arceuus Spell: Death Charge")

            player.inv[0] = InvObj(objs.death_charge_scroll)
            player.withProtectedAccess {
                with(deps.script) { readDeathChargeScroll(player.inv, slot = 0) }
            }
            assertEquals(1, player.count(objs.death_charge_scroll))
        }

    @Test
    fun GameTestState.`decline and stale slot leave the rite and unlock untouched`() =
        runDeathChargeTest { deps ->
            prepareCaster(casts = 0, upgraded = false)
            player.inv[0] = InvObj(objs.death_charge_scroll)

            player.withProtectedAccess {
                with(deps.script) { readDeathChargeScroll(player.inv, slot = 0) }
            }
            continueDialogue()
            declineUpgrade()
            assertEquals(1, player.count(objs.death_charge_scroll))
            assertEquals(0, player.vars[varbits.death_charge_scroll_used])

            player.withProtectedAccess {
                with(deps.script) { readDeathChargeScroll(player.inv, slot = 0) }
            }
            continueDialogue()
            player.inv[0] = null
            confirmUpgrade()
            assertEquals(0, player.vars[varbits.death_charge_scroll_used])
        }

    @Test
    fun GameTestState.`ordinary cast restores one kill and keeps its independent cooldown`() =
        runDeathChargeTest { deps ->
            prepareCaster(casts = 2, upgraded = false)
            val startXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(1, deps.states.remainingCharges(player))
            assertEquals(1, player.vars[varbits.arceuus_death_charge_active])
            assertEquals(1, player.vars[varbits.arceuus_death_charge_cooldown])
            assertEquals(1, player.count(objs.death_rune))
            assertEquals(1, player.count(objs.blood_rune))
            assertEquals(1, player.count(objs.soul_rune))
            assertEquals(900, player.statMap.getFineXP(stats.magic) - startXp)
            assertEquals(seqs.death_charge.id, player.pendingSequence.id)
            assertEquals(
                spotanims.death_charge_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )

            val target = spawnTarget(playerDistance = 1)
            target.queueHit(player, delay = 1, type = HitType.Melee, damage = target.hitpoints)
            advance(1)

            assertEquals(150, player.specialEnergy)
            assertEquals(0, deps.states.remainingCharges(player))
            assertEquals(0, player.vars[varbits.arceuus_death_charge_active])
            assertEquals(1, player.vars[varbits.arceuus_death_charge_cooldown])

            player.withProtectedAccess { deps.spell.cast(this) }
            assertEquals(1, player.count(objs.death_rune))
            advance(DeathChargeSpell.DURATION_CYCLES - 1)
            assertEquals(0, player.vars[varbits.arceuus_death_charge_cooldown])
            assertFalse(deps.states.isActive(player))
        }

    @Test
    fun GameTestState
        .`upgraded charge ignores ineligible kills then consumes two qualifying kills`() =
        runDeathChargeTest { deps ->
            prepareCaster(casts = 1, upgraded = true)
            player.withProtectedAccess { deps.spell.cast(this) }

            assertEquals(2, deps.states.remainingCharges(player))
            assertEquals(2, player.vars[varbits.arceuus_death_charge_active])
            assertEquals(
                spotanims.death_charge_upgrade_cast.id,
                EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
            )

            val excluded = spawnTarget(playerDistance = 1, type = excludedIceBlock)
            excluded.queueHit(player, delay = 1, type = HitType.Melee, damage = excluded.hitpoints)
            advance(1)
            assertEquals(2, deps.states.remainingCharges(player))

            val distant = spawnTarget(playerDistance = 17)
            distant.queueHit(player, delay = 1, type = HitType.Melee, damage = distant.hitpoints)
            advance(1)
            assertEquals(2, deps.states.remainingCharges(player))

            player.setVarp(varps.lastcombat_pvp, player.currentMapClock)
            val pvpSuppressed = spawnTarget(playerDistance = 1)
            pvpSuppressed.queueHit(
                player,
                delay = 1,
                type = HitType.Melee,
                damage = pvpSuppressed.hitpoints,
            )
            advance(1)
            assertEquals(2, deps.states.remainingCharges(player))

            advance(10)
            val first = spawnTarget(playerDistance = 1)
            first.queueHit(player, delay = 1, type = HitType.Melee, damage = first.hitpoints)
            advance(1)
            assertEquals(150, player.specialEnergy)
            assertEquals(1, deps.states.remainingCharges(player))
            assertEquals(1, player.vars[varbits.arceuus_death_charge_active])

            player.specialEnergy = SpecialAttackEnergy.MAX_ENERGY
            val second = spawnTarget(playerDistance = 1)
            second.queueHit(player, delay = 1, type = HitType.Melee, damage = second.hitpoints)
            advance(1)
            assertEquals(SpecialAttackEnergy.MAX_ENERGY, player.specialEnergy)
            assertEquals(0, deps.states.remainingCharges(player))
            assertEquals(0, player.vars[varbits.arceuus_death_charge_active])
        }

    @Test
    fun GameTestState.`active charge and cooldown expire together without a kill`() =
        runDeathChargeTest { deps ->
            prepareCaster(casts = 1, upgraded = false)
            player.withProtectedAccess { deps.spell.cast(this) }

            advance(DeathChargeSpell.DURATION_CYCLES - 1)
            assertTrue(deps.states.isActive(player))
            assertEquals(1, player.vars[varbits.arceuus_death_charge_cooldown])

            advance(1)
            assertFalse(deps.states.isActive(player))
            assertEquals(0, player.vars[varbits.arceuus_death_charge_active])
            assertEquals(0, player.vars[varbits.arceuus_death_charge_cooldown])
        }

    @Test
    fun GameTestState.`members quest level runes and Fountain behavior are atomic`() =
        runDeathChargeTest { deps ->
            prepareCaster(casts = 1, upgraded = false)

            player.members = false
            reject(deps)
            player.members = true
            player.setVarBit(varbits.a_kingdom_divided, 149)
            reject(deps)
            player.setVarBit(varbits.a_kingdom_divided, 150)
            player.stats[stats.magic] = 79
            reject(deps)
            player.stats[stats.magic] = 99
            player.inv[2] = null
            reject(deps)

            prepareCaster(casts = 0, upgraded = false)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val startXp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { deps.spell.cast(this) }
            assertTrue(deps.states.isActive(player))
            assertEquals(startXp, player.statMap.getFineXP(stats.magic))
        }

    private fun GameTestScope.reject(deps: Dependencies) {
        val beforeRunes = runeCounts()
        val beforeXp = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess { deps.spell.cast(this) }
        assertEquals(beforeRunes, runeCounts())
        assertEquals(beforeXp, player.statMap.getFineXP(stats.magic))
        assertFalse(deps.states.isActive(player))
        assertEquals(0, player.vars[varbits.arceuus_death_charge_cooldown])
    }

    private fun GameTestScope.prepareCaster(casts: Int, upgraded: Boolean) {
        player.members = true
        player.stats[stats.magic] = 99
        player.setVarBit(varbits.a_kingdom_divided, 150)
        player.setVarBit(varbits.spellbook, Spellbook.Arceuus.varValue)
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.arceuus_death_charge_cooldown, 0)
        player.setVarBit(varbits.arceuus_death_charge_active, 0)
        player.setVarBit(varbits.death_charge_scroll_used, if (upgraded) 1 else 0)
        player.setVarp(varps.lastcombat_pvp, -100)
        player.specialEnergy = 0
        player.clearInv()
        player.righthand = null
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.death_rune, casts)
            player.inv[1] = InvObj(objs.blood_rune, casts)
            player.inv[2] = InvObj(objs.soul_rune, casts)
        }
    }

    private fun GameTestScope.runeCounts(): List<Int> =
        listOf(
            player.count(objs.death_rune),
            player.count(objs.blood_rune),
            player.count(objs.soul_rune),
        )

    private fun GameTestScope.continueDialogue() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.messagebox_pbutton, subcomponent = -1)
        )
    }

    private fun GameTestScope.confirmUpgrade() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
        )
    }

    private fun GameTestScope.declineUpgrade() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 2)
        )
    }

    private fun GameTestScope.assertDialogueTextSent(text: String) {
        val messages =
            client.filterIsInstance<MessageGame>().map { it.message.replace("<br>", " ") }
        assertTrue(messages.contains(text)) {
            "Dialogue not found. (search=$text) | (messages=$messages)"
        }
    }

    private fun GameTestScope.spawnTarget(
        playerDistance: Int,
        type: UnpackedNpcType = ordinaryTarget,
    ) = spawnNpc(player.coords.translate(playerDistance, 0), type)

    private fun GameTestState.runDeathChargeTest(body: GameTestScope.(Dependencies) -> Unit) =
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
            DeathChargeSpellScript::class,
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
            bind(DeathChargeStateRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val spell: DeathChargeSpell,
        val states: DeathChargeStateRepository,
        val spells: MagicSpellRegistry,
        val script: DeathChargeSpellScript,
        val components: ComponentTypeList,
        val objTypes: org.rsmod.game.type.obj.ObjTypeList,
    )

    private companion object {
        val ordinaryTarget =
            npcTypeFactory.create(id = 65_000) {
                name = "Ordinary target"
                hitpoints = 7
            }
        val excludedIceBlock =
            npcTypeFactory.create(id = 13_688) {
                name = "Unstable ice block"
                hitpoints = 7
            }
    }
}

private var Player.specialEnergy by intVarp(varps.sa_energy)
