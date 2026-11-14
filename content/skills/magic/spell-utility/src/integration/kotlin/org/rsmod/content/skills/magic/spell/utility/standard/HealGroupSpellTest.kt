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
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
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
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.interf.IfButtonOp

@Execution(ExecutionMode.SAME_THREAD)
class HealGroupSpellTest {
    @Test
    fun GameTestState
        .`cache 238 pins Heal Group spell formula cooldown and presentation contracts`() =
        runHealGroupTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_heal_group))

            assertEquals(9137, spell.obj.id)
            assertEquals(95, spell.levelReq)
            assertEquals(124.0, spell.castXp)
            assertEquals(Spellbook.Lunars, spell.spellbook)
            assertEquals(MagicSpellType.Combat, spell.type)
            assertEquals(
                mapOf(objs.astral_rune to 4, objs.blood_rune to 3, objs.law_rune to 6),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertFalse(deps.spells.offensiveCombatSpells().contains(spell))
            assertEquals(218, components.magic_spellbook_heal_group.id ushr 16)
            assertEquals(148, components.magic_spellbook_heal_group.id and 0xFFFF)
            assertEquals(
                listOf("Cast"),
                deps.components[components.magic_spellbook_heal_group].op.toList(),
            )
            assertEquals(4409, seqs.quest_lunar_spell_cast_spell_on_group.id)
            assertEquals(745, spotanims.quest_lunar_spellbook_heal_group_spot_anim.id)
            assertEquals(2893, synths.lunar_heal.id)
            assertEquals(2894, synths.lunar_heal_group.id)
            assertEquals(925, varbits.heal_group_timelimit.id)
            assertEquals(16, timers.magic_heal_group_cooldown.id)
            assertEquals(150, HealGroupStateRepository.COOLDOWN_CYCLES)

            assertEquals(74, LunarHealGroup.sacrifice(99))
            assertEquals(listOf(79), LunarHealGroup.healing(74, listOf(98)))
            assertEquals(listOf(39, 2), LunarHealGroup.healing(74, listOf(98, 2)))
            assertEquals(listOf(15, 15, 15, 15, 15), LunarHealGroup.healing(74, List(5) { 98 }))
        }

    @Test
    fun GameTestState
        .`single recipient receives sacrificed health plus five with exact cast state`() =
        runHealGroupTest { deps ->
            prepareCaster()
            val recipient = registerRecipient("Recipient", 1, 0, hitpoints = 1)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.healGroup.cast(this) }

            assertEquals(25, player.hitpoints)
            assertEquals(80, recipient.hitpoints)
            assertEquals(1240, player.statMap.getFineXP(stats.magic) - magicXp)
            assertEquals(listOf(0, 0, 0), runeCounts())
            assertEquals(seqs.quest_lunar_spell_cast_spell_on_group.id, player.pendingSequence.id)
            assertRecipientGraphic(recipient)
            assertEquals(
                synths.lunar_heal_group.id,
                client.filterIsInstance<SynthSound>().single().id,
            )
            val hitmark = Hitmark(player.activeHitmarks.getLong(0))
            assertEquals(74, hitmark.damage)
            assertTrue(hitmark.isNoSource)
            assertEquals(1, player.vars[varbits.heal_group_timelimit])
        }

    @Test
    fun GameTestState.`equal integer split affects at most first five eligible player slots`() =
        runHealGroupTest { deps ->
            prepareCaster()
            val recipients =
                List(6) { index -> registerRecipient("Recipient$index", 1, 0, hitpoints = 1) }

            player.withProtectedAccess { deps.healGroup.cast(this) }

            recipients.take(5).forEach {
                assertEquals(16, it.hitpoints)
                assertRecipientGraphic(it)
            }
            assertEquals(1, recipients.last().hitpoints)
            assertTrue(recipients.last().pendingSpotanims.isEmpty())
            assertEquals(25, player.hitpoints)
        }

    @Test
    fun GameTestState.`recipient caps do not redistribute an equal share`() =
        runHealGroupTest { deps ->
            prepareCaster()
            val nearlyFull = registerRecipient("NearlyFull", 1, 0, hitpoints = 98)
            val injured = registerRecipient("Injured", 0, 1, hitpoints = 1)

            player.withProtectedAccess { deps.healGroup.cast(this) }

            assertEquals(99, nearlyFull.hitpoints)
            assertEquals(40, injured.hitpoints)
            assertEquals(25, player.hitpoints)
        }

    @Test
    fun GameTestState
        .`selection requires injured aiding living players in exact three by three area`() =
        runHealGroupTest { deps ->
            prepareCaster()
            player.setCurrentLevel(stats.hitpoints, 50)
            val sameTile = registerRecipient("SameTile", 0, 0, hitpoints = 1)
            val diagonal = registerRecipient("Diagonal", 1, 1, hitpoints = 1)
            val noAid = registerRecipient("NoAid", 1, 0, hitpoints = 1, acceptAid = false)
            val full = registerRecipient("Full", 0, 1, hitpoints = 99)
            val dead = registerRecipient("Dead", -1, 0, hitpoints = 0)
            val outside = registerRecipient("Outside", 2, 0, hitpoints = 1)
            val upstairs =
                registerPlayer(coords = player.coords.translateLevel(1)).also {
                    prepareRecipient(it, "Upstairs", hitpoints = 1, acceptAid = true)
                }

            player.withProtectedAccess { deps.healGroup.cast(this) }

            // floor(50 * .75) + 5 = 42; each of the two eligible recipients receives 21.
            assertEquals(22, sameTile.hitpoints)
            assertEquals(22, diagonal.hitpoints)
            assertEquals(13, player.hitpoints)
            listOf(noAid, full, dead, outside, upstairs).forEach {
                assertTrue(it.pendingSpotanims.isEmpty())
            }
        }

    @Test
    fun GameTestState
        .`membership quest spellbook Magic health runes and no recipient failures are atomic`() =
        runHealGroupTest { deps ->
            prepareCaster()
            val recipient = registerRecipient("Recipient", 1, 0, hitpoints = 1)

            player.members = false
            reject(deps, "You need to be on a members' world to cast this spell.")
            player.members = true
            player.setVarBit(varbits.lunar_quest_main, 189)
            reject(deps, "You need to complete Lunar Diplomacy to cast this spell.")
            player.setVarBit(varbits.lunar_quest_main, 190)
            player.setVarBit(varbits.spellbook, Spellbook.Standard.varValue)
            reject(deps)
            player.setVarBit(varbits.spellbook, Spellbook.Lunars.varValue)
            player.stats[stats.magic] = 94
            reject(deps, "Your Magic level is not high enough for this spell.")
            player.stats[stats.magic] = 99
            player.setCurrentLevel(stats.hitpoints, 10)
            reject(deps)
            player.setCurrentLevel(stats.hitpoints, 99)
            player.inv[2] = null
            reject(deps, "You do not have enough Law Runes to cast this spell.")
            player.inv[2] = InvObj(objs.law_rune, 6)
            recipient.setCurrentLevel(stats.hitpoints, 99)
            reject(deps)
        }

    @Test
    fun GameTestState.`cooldown remains active through tick 149 and clears on tick 150`() =
        runHealGroupTest { deps ->
            prepareCaster(casts = 3)
            val recipient = registerRecipient("Recipient", 1, 0, hitpoints = 1)

            player.withProtectedAccess { deps.healGroup.cast(this) }
            assertEquals(8, player.count(objs.astral_rune))
            assertEquals(1, player.vars[varbits.heal_group_timelimit])
            player.setCurrentLevel(stats.hitpoints, 99)
            recipient.setCurrentLevel(stats.hitpoints, 1)
            advance(149)
            player.withProtectedAccess { deps.healGroup.cast(this) }
            assertEquals(8, player.count(objs.astral_rune))
            assertEquals(1, player.vars[varbits.heal_group_timelimit])

            advance()
            assertEquals(0, player.vars[varbits.heal_group_timelimit])
            player.withProtectedAccess { deps.healGroup.cast(this) }
            assertEquals(4, player.count(objs.astral_rune))
            assertEquals(1, player.vars[varbits.heal_group_timelimit])
        }

    @Test
    fun GameTestState.`successful cast adds three ticks only to an active attack delay`() =
        runHealGroupTest { deps ->
            prepareCaster()
            registerRecipient("Recipient", 1, 0, hitpoints = 1)
            player.actionDelay = player.currentMapClock + 4

            player.withProtectedAccess { deps.healGroup.cast(this) }

            assertEquals(player.currentMapClock + 7, player.actionDelay)
        }

    @Test
    fun GameTestState.`idle cast does not create an attack delay`() = runHealGroupTest { deps ->
        prepareCaster()
        registerRecipient("Recipient", 1, 0, hitpoints = 1)
        player.actionDelay = player.currentMapClock

        player.withProtectedAccess { deps.healGroup.cast(this) }

        assertEquals(player.currentMapClock, player.actionDelay)
    }

    @Test
    fun GameTestState
        .`single way PvP recipients are excluded while multiway PvP recipients work`() =
        runHealGroupTest { deps ->
            prepareCaster(casts = 2)
            val recipient = registerRecipient("Recipient", 1, 0, hitpoints = 1)
            player.setVarBit(varbits.inside_wilderness, 1)
            player.setVarBit(varbits.multiway_indicator, 0)

            reject(deps)
            player.setVarBit(varbits.multiway_indicator, 1)
            recipient.setVarBit(varbits.inside_wilderness, 1)
            recipient.setVarBit(varbits.multiway_indicator, 1)
            player.withProtectedAccess { deps.healGroup.cast(this) }
            assertEquals(80, recipient.hitpoints)

            deps.states.clear(player)
            player.setCurrentLevel(stats.hitpoints, 99)
            recipient.setCurrentLevel(stats.hitpoints, 1)
            advance(5)
            deps.realm.updateConfig(deps.realm.config.copy(worldFlags = WorldFlag.Pvp.mask))
            player.setVarBit(varbits.inside_wilderness, 0)
            recipient.setVarBit(varbits.inside_wilderness, 0)
            player.setVarBit(varbits.multiway_indicator, 0)
            recipient.setVarBit(varbits.multiway_indicator, 0)
            recipient.setVarp(varps.lastcombat_pvp, mapClock.cycle)
            reject(deps)
        }

    @Test
    fun GameTestState.`Fountain supplies runes and suppresses Magic xp only`() =
        runHealGroupTest { deps ->
            prepareCaster(casts = 0)
            val recipient = registerRecipient("Recipient", 1, 0, hitpoints = 1)
            player.setVarBit(varbits.fountain_of_rune_active, 1)
            val magicXp = player.statMap.getFineXP(stats.magic)

            player.withProtectedAccess { deps.healGroup.cast(this) }

            assertEquals(25, player.hitpoints)
            assertEquals(80, recipient.hitpoints)
            assertEquals(magicXp, player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`spellbook button dispatches Heal Group`() = runHealGroupTest { deps ->
        prepareCaster()
        val recipient = registerRecipient("Recipient", 1, 0, hitpoints = 1)

        deps.events.publish(
            IfOverlayButton(
                player,
                components.magic_spellbook_heal_group,
                comsub = -1,
                obj = null,
                op = IfButtonOp.Op1,
            )
        )

        assertEquals(80, recipient.hitpoints)
        assertEquals(0, player.count(objs.astral_rune))
    }

    private fun GameTestScope.prepareCaster(casts: Int = 1) {
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
        player.clearInv()
        client.clear()
        if (casts > 0) {
            player.inv[0] = InvObj(objs.astral_rune, casts * 4)
            player.inv[1] = InvObj(objs.blood_rune, casts * 3)
            player.inv[2] = InvObj(objs.law_rune, casts * 6)
        }
    }

    private fun GameTestScope.registerRecipient(
        name: String,
        x: Int,
        z: Int,
        hitpoints: Int,
        acceptAid: Boolean = true,
    ): Player =
        registerPlayer(coords = player.coords.translate(x, z)).also {
            prepareRecipient(it, name, hitpoints, acceptAid)
        }

    private fun GameTestScope.prepareRecipient(
        recipient: Player,
        name: String,
        hitpoints: Int,
        acceptAid: Boolean,
    ) {
        recipient.members = true
        recipient.displayName = name
        recipient.setBaseLevel(stats.hitpoints, 99)
        recipient.setCurrentLevel(stats.hitpoints, hitpoints)
        recipient.setVarBit(varbits.option_acceptaid, if (acceptAid) 1 else 0)
        recipient.setVarBit(varbits.inside_wilderness, 0)
        recipient.setVarBit(varbits.multiway_indicator, 0)
    }

    private fun assertRecipientGraphic(recipient: Player) {
        val graphic = EntitySpotanim(recipient.pendingSpotanims.getLong(0))
        assertEquals(spotanims.quest_lunar_spellbook_heal_group_spot_anim.id, graphic.id)
        assertEquals(96, graphic.height)
    }

    private fun GameTestScope.reject(deps: Dependencies, message: String? = null) {
        val beforeRunes = runeCounts()
        val beforeXp = player.statMap.getFineXP(stats.magic)
        val beforeCasterHp = player.hitpoints
        val recipientHp =
            players.asSequence().filter { it !== player }.associateWith { it.hitpoints }
        player.withProtectedAccess {
            deps.healGroup.cast(this)
            if (message != null) {
                assertMessageSent(message)
            }
        }
        assertEquals(beforeRunes, runeCounts())
        assertEquals(beforeXp, player.statMap.getFineXP(stats.magic))
        assertEquals(beforeCasterHp, player.hitpoints)
        recipientHp.forEach { (recipient, hitpoints) ->
            assertEquals(hitpoints, recipient.hitpoints)
        }
    }

    private fun GameTestScope.runeCounts(): List<Int> =
        listOf(
            player.count(objs.astral_rune),
            player.count(objs.blood_rune),
            player.count(objs.law_rune),
        )

    private fun GameTestState.runHealGroupTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            HealGroupSpellScript::class,
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
            bind(HealGroupStateRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val healGroup: HealGroupSpell,
        val states: HealGroupStateRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val events: EventBus,
        val realm: Realm,
    )
}
