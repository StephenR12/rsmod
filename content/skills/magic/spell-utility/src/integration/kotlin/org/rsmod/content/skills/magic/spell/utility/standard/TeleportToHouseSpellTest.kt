package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.ui.IfOverlayButton
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
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class TeleportToHouseSpellTest {
    @Test
    fun GameTestState.`cache contract defines the spell component and POH settings`() =
        runHouseTest { deps ->
            val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_poh_teleport))
            assertEquals(40, spell.levelReq)
            assertEquals(30.0, spell.castXp)
            assertEquals(Spellbook.Standard, spell.spellbook)
            assertEquals(MagicSpellType.Teleport, spell.type)
            assertEquals(
                mapOf(objs.air_rune to 1, objs.earth_rune to 1, objs.law_rune to 1),
                spell.objReqs.associate { it.obj to it.count },
            )
            assertEquals(34, spell.component.id and 0xFFFF)
            assertEquals(8828, objs.spell_poh_teleport.id)
            assertEquals(218, spell.component.id ushr 16)
            assertEquals(
                listOf("*", "Outside", "*", "*", "*"),
                deps.components[components.magic_spellbook_teleport_to_house].op.toList(),
            )
            assertEquals(714, seqs.teleport_casting.id)
            assertEquals(111, spotanims.teleport_casting.id)
            assertEquals(200, synths.teleport_all.id)

            assertVarBit(deps, varbits.poh_building_mode, 2176, 780, 0..0)
            assertVarBit(deps, varbits.poh_house_location, 2187, 738, 0..4)
            assertVarBit(deps, varbits.poh_tele_toggle, 4744, 1047, 23..23)
            assertVarBit(deps, varbits.poh_teleport_building_mode, 14670, 2885, 31..31)
        }

    @Test
    fun GameTestState.`all cache 238 house locations teleport outside after four ticks`() =
        runHouseTest { deps ->
            for ((location, destination) in outsideDestinations) {
                prepareCaster(deps, location.cacheValue)
                allocZoneCollision(destination)
                val origin = player.coords
                val startXp = player.statMap.getFineXP(stats.magic)

                player.withProtectedAccess {
                    deps.spell.cast(this, HouseTeleportDestination.Outside)
                }

                assertEquals(0, runeCount(deps))
                assertEquals(300, player.statMap.getFineXP(stats.magic) - startXp)
                assertEquals(seqs.teleport_casting.id, player.pendingSequence.id)
                assertEquals(
                    spotanims.teleport_casting.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                )
                advance(3)
                assertEquals(origin, player.coords)
                advance()
                assertEquals(destination, player.coords, location.name)
            }
        }

    @Test
    fun GameTestState.`inside teleport uses the prepared regular or building-mode entrance`() =
        runHouseTest { deps ->
            val regular = CoordGrid(0, 30, 30, 10, 11)
            val building = CoordGrid(0, 31, 30, 12, 13)
            prepareCaster(deps)
            allocZoneCollision(regular)
            allocZoneCollision(building)
            deps.houses.register(player, regular, building)

            player.withProtectedAccess { deps.spell.cast(this, HouseTeleportDestination.Inside) }
            advance(4)
            assertEquals(regular, player.coords)

            prepareCaster(deps)
            deps.houses.register(player, regular, building)
            player.setVarBit(varbits.poh_teleport_building_mode, 1)
            player.withProtectedAccess { deps.spell.cast(this, HouseTeleportDestination.Inside) }
            advance(4)
            assertEquals(building, player.coords)
        }

    @Test
    fun GameTestState.`missing ownership instance and group routes reject atomically`() =
        runHouseTest { deps ->
            prepareCaster(deps, houseLocation = 0)
            assertRejected(
                deps,
                HouseTeleportDestination.Outside,
                "You must own a house to cast this spell.",
            )

            prepareCaster(deps, houseLocation = 7)
            assertRejected(
                deps,
                HouseTeleportDestination.Outside,
                "You must own a house to cast this spell.",
            )

            prepareCaster(deps)
            assertRejected(
                deps,
                HouseTeleportDestination.Inside,
                "Your house is not ready to enter.",
            )

            prepareCaster(deps)
            assertRejected(
                deps,
                HouseTeleportDestination.GroupChoose,
                "Group house teleporting is not available yet.",
            )
            assertRejected(
                deps,
                HouseTeleportDestination.GroupPrevious,
                "Group house teleporting is not available yet.",
            )
        }

    @Test
    fun GameTestState.`membership level runes teleblock and deep Wilderness gates are atomic`() =
        runHouseTest { deps ->
            prepareCaster(deps, members = false)
            assertRejected(
                deps,
                HouseTeleportDestination.Outside,
                "You need to be on a members' world to cast this spell.",
            )

            prepareCaster(deps, magicLevel = 39)
            assertRejected(
                deps,
                HouseTeleportDestination.Outside,
                "Your Magic level is not high enough for this spell.",
            )

            prepareCaster(deps)
            val lawSlot = player.inv.indexOfFirst { it?.id == objs.law_rune.id }
            player.inv[lawSlot] = null
            assertRejected(
                deps,
                HouseTeleportDestination.Outside,
                "You do not have enough Law Runes to cast this spell.",
            )

            prepareCaster(deps)
            player.setVarBit(varbits.teleblock_cycles, 101)
            assertRejected(
                deps,
                HouseTeleportDestination.Outside,
                "A magical force stops you from teleporting.",
            )

            prepareCaster(deps)
            player.teleport(CoordGrid(0, 50, 57, 0, 32))
            player.setVarBit(varbits.inside_wilderness, 1)
            assertRejected(
                deps,
                HouseTeleportDestination.Outside,
                "A mysterious force prevents you from teleporting!",
            )
        }

    @Test
    fun GameTestState.`spellbook operations route inside outside and group choices`() =
        runHouseTest { deps ->
            val inside = CoordGrid(0, 30, 30, 10, 11)
            prepareCaster(deps)
            allocZoneCollision(inside)
            deps.houses.register(player, inside)
            publish(deps, IfButtonOp.Op1)
            advance(4)
            assertEquals(inside, player.coords)

            for (op in listOf(IfButtonOp.Op2, IfButtonOp.Op3)) {
                prepareCaster(deps)
                allocZoneCollision(outsideDestinations.first().second)
                publish(deps, op)
                advance(4)
                assertEquals(outsideDestinations.first().second, player.coords)
            }

            for (op in listOf(IfButtonOp.Op4, IfButtonOp.Op5)) {
                prepareCaster(deps)
                publish(deps, op)
                assertMessageSent("Group house teleporting is not available yet.")
                assertEquals(3, runeCount(deps))
            }
        }

    @Test
    fun GameTestState.`logout clears the prepared house entrance`() = runHouseTest { deps ->
        prepareCaster(deps)
        val inside = CoordGrid(0, 30, 30, 10, 11)
        deps.houses.register(player, inside)
        assertEquals(inside, deps.houses.destination(player, buildingMode = false))

        deps.events.publish(SessionStateEvent.Logout(player))

        assertNull(deps.houses.destination(player, buildingMode = false))
    }

    private fun GameTestScope.prepareCaster(
        deps: Dependencies,
        houseLocation: Int = HouseLocation.Rimmington.cacheValue,
        magicLevel: Int = 99,
        members: Boolean = true,
    ) {
        player.members = members
        player.stats[stats.magic] = magicLevel
        player.clearInv()
        player.setVarBit(varbits.fountain_of_rune_active, 0)
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.teleblock_cycles, 0)
        player.setVarBit(varbits.poh_house_location, houseLocation)
        player.setVarBit(varbits.poh_teleport_building_mode, 0)
        deps.houses.clear(player)
        val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_poh_teleport))
        spell.objReqs.forEachIndexed { slot, req -> player.inv[slot] = InvObj(req.obj, req.count) }
    }

    private fun GameTestScope.assertRejected(
        deps: Dependencies,
        destination: HouseTeleportDestination,
        message: String,
    ) {
        val origin = player.coords
        val runes = runeCount(deps)
        val xp = player.statMap.getFineXP(stats.magic)
        player.withProtectedAccess {
            deps.spell.cast(this, destination)
            assertMessageSent(message)
        }
        assertEquals(origin, player.coords)
        assertEquals(runes, runeCount(deps))
        assertEquals(xp, player.statMap.getFineXP(stats.magic))
    }

    private fun GameTestScope.runeCount(deps: Dependencies): Int {
        val spell = requireNotNull(deps.spells.getObjSpell(objs.spell_poh_teleport))
        return spell.objReqs.sumOf { player.count(it.obj) }
    }

    private fun GameTestScope.publish(deps: Dependencies, op: IfButtonOp) {
        deps.events.publish(
            IfOverlayButton(
                player,
                components.magic_spellbook_teleport_to_house,
                comsub = -1,
                obj = null,
                op = op,
            )
        )
    }

    private fun assertVarBit(
        deps: Dependencies,
        type: VarBitType,
        id: Int,
        varp: Int,
        bits: IntRange,
    ) {
        val unpacked = deps.varbitTypes[type]
        assertEquals(id, unpacked.id)
        assertEquals(varp, unpacked.baseVar.id)
        assertEquals(bits, unpacked.bits)
    }

    private fun GameTestState.runHouseTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
            TeleportToHouseSpellScript::class,
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
        val spell: TeleportToHouseSpell,
        val houses: PlayerOwnedHouseRepository,
        val spells: MagicSpellRegistry,
        val components: ComponentTypeList,
        val varbitTypes: VarBitTypeList,
        val events: EventBus,
    )

    private companion object {
        val outsideDestinations =
            listOf(
                HouseLocation.Rimmington to CoordGrid(0, 46, 50, 10, 24),
                HouseLocation.Taverley to CoordGrid(0, 45, 54, 13, 9),
                HouseLocation.Pollnivneach to CoordGrid(0, 52, 46, 11, 59),
                HouseLocation.Rellekka to CoordGrid(0, 41, 56, 46, 48),
                HouseLocation.Brimhaven to CoordGrid(0, 43, 49, 6, 42),
                HouseLocation.Yanille to CoordGrid(0, 39, 48, 48, 23),
                HouseLocation.Hosidius to CoordGrid(0, 27, 54, 15, 60),
                HouseLocation.Prifddinas to CoordGrid(0, 50, 94, 39, 59),
                HouseLocation.Aldarin to CoordGrid(0, 22, 46, 14, 19),
            )
    }
}
